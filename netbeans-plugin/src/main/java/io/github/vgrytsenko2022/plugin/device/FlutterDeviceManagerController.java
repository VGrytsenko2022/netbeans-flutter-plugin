package io.github.vgrytsenko2022.plugin.device;

import io.github.vgrytsenko2022.run.AndroidAvdCreateRequest;
import java.awt.Component;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/** Serializes Flutter Device Manager operations and owns their NetBeans lifecycle. */
final class FlutterDeviceManagerController implements DeviceManagerActionHandler, AutoCloseable {
    private static final String OUTPUT_NAME = "Flutter Device Manager";

    private final Object lock = new Object();
    private final AndroidDeviceManagerBackend backend;
    private final AndroidAvdCreationProvider creationProvider;
    private final View view;
    private final ProgressFactory progressFactory;
    private final Feedback feedback;
    private final RequestProcessor worker;

    private DeviceManagerInventory inventory;
    private DeviceManagerSnapshot lastSnapshot = DeviceManagerSnapshot.initial();
    private ActiveOperation active;
    private boolean opened;
    private boolean refreshWhenIdle;
    private long lifecycle;

    FlutterDeviceManagerController(FlutterDeviceManagerPanel panel) {
        this(
                new PanelView(panel),
                new NetBeansAndroidDeviceManagerBackend(),
                CreateAndroidAvdPanel::showDialog,
                new NetBeansProgressFactory(),
                new NetBeansFeedback());
        panel.setActionHandler(this);
    }

    FlutterDeviceManagerController(
            View view,
            AndroidDeviceManagerBackend backend,
            AndroidAvdCreationProvider creationProvider,
            ProgressFactory progressFactory,
            Feedback feedback) {
        this.view = Objects.requireNonNull(view, "view");
        this.backend = Objects.requireNonNull(backend, "backend");
        this.creationProvider = Objects.requireNonNull(creationProvider, "creationProvider");
        this.progressFactory = Objects.requireNonNull(progressFactory, "progressFactory");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.worker = new RequestProcessor(
                FlutterDeviceManagerController.class.getName()
                        + "-" + Integer.toHexString(System.identityHashCode(this)),
                1,
                true);
    }

    /** Activates the controller for a newly opened TopComponent and starts discovery. */
    void open() {
        boolean refreshNow;
        synchronized (lock) {
            if (opened) {
                return;
            }
            opened = true;
            lifecycle++;
            refreshNow = active == null;
            refreshWhenIdle = !refreshNow;
        }
        if (refreshNow) {
            perform(DeviceManagerAction.REFRESH, DeviceManagerSelection.none());
        }
    }

    /**
     * Cancels discovery/boot waits and closes the backend. Backend close only detaches emulator
     * handles; it does not stop a running emulator.
     */
    @Override
    public void close() {
        ActiveOperation operation;
        synchronized (lock) {
            if (!opened && active == null) {
                return;
            }
            opened = false;
            lifecycle++;
            refreshWhenIdle = false;
            operation = active;
            active = null;
        }
        if (operation != null) {
            operation.cancelled.set(true);
            operation.task.cancel();
            operation.progress.finish();
        }
        worker.post(() -> {
            try {
                backend.close();
            } catch (RuntimeException ignored) {
                // A closed Device Manager has no visible target for a close-time failure.
            }
        });
    }

    @Override
    public void perform(DeviceManagerAction action, DeviceManagerSelection selection) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(selection, "selection");

        final ActiveOperation operation;
        final DeviceManagerSnapshot busySnapshot;
        synchronized (lock) {
            if (!opened) {
                feedback.failure(
                        "Flutter Device Manager unavailable",
                        "Cannot " + actionLabel(action)
                                + ": the Flutter Device Manager window is closed.",
                        null);
                return;
            }
            if (active != null) {
                feedback.failure(
                        "Flutter Device Manager busy",
                        "Cannot " + actionLabel(action) + " while "
                                + actionLabel(active.action) + " is still running.",
                        null);
                return;
            }
            DeviceManagerActionState state = DeviceManagerPresentation.actionState(
                    lastSnapshot,
                    selection,
                    action);
            if (!state.enabled()) {
                feedback.failure(
                        "Flutter Device Manager action unavailable",
                        "Cannot " + actionLabel(action) + ": " + state.reason(),
                        null);
                return;
            }

            long operationLifecycle = lifecycle;
            String target = targetLabel(lastSnapshot, action, selection);
            DeviceManagerSnapshot baselineSnapshot = lastSnapshot;
            ControllerProgress progress = progressFactory.create(
                    progressTitle(action, target),
                    () -> cancel(operationLifecycle));
            operation = new ActiveOperation(
                    action,
                    selection,
                    target,
                    operationLifecycle,
                    baselineSnapshot,
                    progress);
            RequestProcessor.Task task = worker.create(() -> execute(operation));
            operation.task = task;
            active = operation;
            busySnapshot = busySnapshot(action, selection, target);
            lastSnapshot = busySnapshot;
        }
        view.publish(busySnapshot);
        operation.progress.start(operationMessage(action, operation.target));
        operation.task.schedule(0);
    }

    private boolean cancel(long expectedLifecycle) {
        ActiveOperation operation;
        DeviceManagerSnapshot snapshot;
        synchronized (lock) {
            operation = active;
            if (!opened || operation == null || lifecycle != expectedLifecycle) {
                return false;
            }
            operation.cancelled.set(true);
            operation.task.cancel();
            snapshot = copySnapshot(
                    lastSnapshot,
                    true,
                    operation.selection,
                    "Cancelling " + actionLabel(operation.action) + " for "
                            + operation.target + ".");
            lastSnapshot = snapshot;
        }
        view.publish(snapshot);
        worker.post(() -> completeCancelledIfTaskDidNotRun(operation));
        return true;
    }

    private void execute(ActiveOperation operation) {
        OperationResult result;
        try {
            result = executeAction(operation);
        } catch (InterruptedException interrupted) {
            Thread.interrupted();
            result = operation.cancelled.get()
                    ? OperationResult.cancelled(cancellationMessage(
                            operation.action,
                            operation.target))
                    : OperationResult.failure(interrupted);
        } catch (Exception failure) {
            result = OperationResult.failure(failure);
        }
        complete(operation, result);
    }

    private OperationResult executeAction(ActiveOperation operation) throws Exception {
        if (operation.cancelled.get()) {
            return OperationResult.cancelled(cancellationMessage(
                    operation.action,
                    operation.target));
        }
        if (operation.action == DeviceManagerAction.REFRESH) {
            DeviceManagerInventory refreshed = backend.refresh();
            return OperationResult.success(refreshed, refreshed.message());
        }

        String message = switch (operation.action) {
            case CREATE -> createAvd();
            case START -> backend.start(operation.selection.id());
            case STOP -> backend.stop(operation.selection.id());
            case RESTART -> backend.restart(operation.selection.id());
            case WIPE -> backend.wipe(operation.selection.id());
            case DELETE -> backend.delete(operation.selection.id());
            case SELECT_TARGET -> backend.selectTarget(operation.selection);
            case REFRESH -> throw new IllegalStateException("Refresh was handled above");
        };
        if (message == null) {
            return OperationResult.cancelled("Android Virtual Device creation was cancelled.");
        }
        DeviceManagerInventory refreshed = backend.refresh();
        return OperationResult.success(refreshed, message);
    }

    private String createAvd() throws Exception {
        AndroidDeviceManagerBackend.CreationOptions options = backend.creationOptions();
        Optional<AndroidAvdCreateRequest> request = callOnEdt(
                () -> creationProvider.request(options));
        if (request.isEmpty()) {
            return null;
        }
        backend.create(request.orElseThrow());
        return "Created Android Virtual Device '" + request.orElseThrow().name() + "'.";
    }

    private void complete(ActiveOperation operation, OperationResult result) {
        if (!operation.completed.compareAndSet(false, true)) {
            return;
        }

        DeviceManagerInventory recoveredInventory = null;
        boolean recover;
        synchronized (lock) {
            recover = opened
                    && lifecycle == operation.lifecycle
                    && result.inventory == null
                    && operation.action != DeviceManagerAction.REFRESH;
        }
        if (recover) {
            recoveredInventory = refreshAfterIncompleteOperation(result.failure);
        }

        DeviceManagerSnapshot snapshot = null;
        boolean notifyFailure = false;
        String failureMessage = null;
        synchronized (lock) {
            boolean currentLifecycle = opened && lifecycle == operation.lifecycle;
            if (currentLifecycle) {
                if (result.inventory != null) {
                    inventory = result.inventory;
                    snapshot = result.inventory.snapshot(false, result.message);
                } else {
                    if (recoveredInventory != null) {
                        inventory = recoveredInventory;
                    }
                    String message = result.message;
                    if (result.failure != null) {
                        failureMessage = failureMessage(operation, result.failure);
                        message = failureMessage;
                        notifyFailure = true;
                    }
                    snapshot = inventory == null
                            ? copySnapshot(
                                    operation.baselineSnapshot,
                                    false,
                                    DeviceManagerSelection.none(),
                                    message)
                            : inventory.snapshot(false, message);
                }
                lastSnapshot = snapshot;
            }
            if (active == operation) {
                active = null;
            }
        }

        operation.progress.finish();
        if (snapshot != null) {
            view.publish(snapshot);
        }
        if (notifyFailure) {
            feedback.failure(
                    "Flutter Device Manager: " + actionLabel(operation.action) + " failed",
                    failureMessage,
                    result.failure);
        }
        scheduleDeferredRefresh();
    }

    private DeviceManagerInventory refreshAfterIncompleteOperation(Exception failure) {
        try {
            return backend.refresh();
        } catch (InterruptedException interrupted) {
            Thread.interrupted();
            if (failure != null) {
                failure.addSuppressed(interrupted);
            }
            return null;
        } catch (IOException | RuntimeException refreshFailure) {
            if (failure != null) {
                failure.addSuppressed(refreshFailure);
            }
            return null;
        }
    }

    private void completeCancelledIfTaskDidNotRun(ActiveOperation operation) {
        complete(
                operation,
                OperationResult.cancelled(cancellationMessage(
                        operation.action,
                        operation.target)));
    }

    private void scheduleDeferredRefresh() {
        boolean refresh;
        synchronized (lock) {
            refresh = opened && active == null && refreshWhenIdle;
            if (refresh) {
                refreshWhenIdle = false;
            }
        }
        if (refresh) {
            perform(DeviceManagerAction.REFRESH, DeviceManagerSelection.none());
        }
    }

    private DeviceManagerSnapshot busySnapshot(
            DeviceManagerAction action,
            DeviceManagerSelection selection,
            String target) {
        AndroidToolchainStatus toolchain = action == DeviceManagerAction.REFRESH
                ? AndroidToolchainStatus.discovering()
                : lastSnapshot.toolchain();
        Set<DeviceManagerSelection> busy = selection.isNone()
                ? Set.of()
                : Set.of(selection);
        return new DeviceManagerSnapshot(
                toolchain,
                lastSnapshot.connectedDevices(),
                lastSnapshot.avds(),
                lastSnapshot.selectedTargetId(),
                true,
                busy,
                operationMessage(action, target));
    }

    private static DeviceManagerSnapshot copySnapshot(
            DeviceManagerSnapshot source,
            boolean busy,
            DeviceManagerSelection selection,
            String message) {
        return new DeviceManagerSnapshot(
                source.toolchain(),
                source.connectedDevices(),
                source.avds(),
                source.selectedTargetId(),
                busy,
                selection.isNone() ? Set.of() : Set.of(selection),
                message);
    }

    private String failureMessage(ActiveOperation operation, Exception failure) {
        return "Could not " + actionLabel(operation.action) + " for " + operation.target
                + ": " + messageOf(failure) + ".";
    }

    private static String targetLabel(
            DeviceManagerSnapshot snapshot,
            DeviceManagerAction action,
            DeviceManagerSelection selection) {
        return switch (action) {
            case REFRESH -> "Android devices and Android Virtual Devices";
            case CREATE -> "a new Android Virtual Device";
            default -> DeviceManagerPresentation.selectionLabel(snapshot, selection);
        };
    }

    private static String operationMessage(DeviceManagerAction action, String target) {
        return switch (action) {
            case CREATE -> "Preparing Android Virtual Device creation.";
            case REFRESH -> "Refreshing connected Android devices and Android Virtual Devices.";
            case START -> "Starting " + target + ".";
            case STOP -> "Stopping " + target + ".";
            case RESTART -> "Restarting " + target + ".";
            case WIPE -> "Wiping user data for " + target + ".";
            case DELETE -> "Deleting " + target + ".";
            case SELECT_TARGET -> "Selecting " + target + " as the Flutter run target.";
        };
    }

    private static String progressTitle(DeviceManagerAction action, String target) {
        return "Flutter Device Manager — " + actionLabel(action) + ": " + target;
    }

    private static String cancellationMessage(DeviceManagerAction action, String target) {
        return switch (action) {
            case START, RESTART, WIPE -> "Cancelled the wait for " + target
                    + ". If the Android emulator already started, it continues running. "
                    + "Use Refresh to update its state.";
            default -> "Cancelled " + actionLabel(action) + " for " + target + ".";
        };
    }

    private static String actionLabel(DeviceManagerAction action) {
        return switch (action) {
            case CREATE -> "create an Android Virtual Device";
            case REFRESH -> "refresh Android devices";
            case START -> "start the Android Virtual Device";
            case STOP -> "stop the Android Virtual Device";
            case RESTART -> "restart the Android Virtual Device";
            case WIPE -> "wipe Android Virtual Device user data";
            case DELETE -> "delete the Android Virtual Device";
            case SELECT_TARGET -> "select the Flutter run target";
        };
    }

    private static String messageOf(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.strip();
    }

    private static <T> T callOnEdt(Callable<T> task) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return task.call();
        }
        FutureTask<T> future = new FutureTask<>(task);
        try {
            EventQueue.invokeAndWait(future);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("Could not open Android Virtual Device creation", cause);
        }
        try {
            return future.get();
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("Android Virtual Device creation failed", cause);
        }
    }

    interface View {
        void publish(DeviceManagerSnapshot snapshot);

        Component parentComponent();
    }

    @FunctionalInterface
    interface ProgressFactory {
        ControllerProgress create(String displayName, BooleanSupplier cancel);
    }

    interface ControllerProgress {
        void start(String detail);

        void finish();
    }

    interface Feedback {
        void failure(String title, String message, Throwable cause);
    }

    private static final class PanelView implements View {
        private final FlutterDeviceManagerPanel panel;

        PanelView(FlutterDeviceManagerPanel panel) {
            this.panel = Objects.requireNonNull(panel, "panel");
        }

        @Override
        public void publish(DeviceManagerSnapshot snapshot) {
            panel.setSnapshot(snapshot);
        }

        @Override
        public Component parentComponent() {
            return panel;
        }
    }

    private static final class NetBeansProgressFactory implements ProgressFactory {
        @Override
        public ControllerProgress create(String displayName, BooleanSupplier cancel) {
            ProgressHandle handle = ProgressHandle.createHandle(displayName, cancel::getAsBoolean);
            handle.setInitialDelay(0);
            return new ControllerProgress() {
                private final AtomicBoolean finished = new AtomicBoolean();

                @Override
                public void start(String detail) {
                    if (finished.get()) {
                        return;
                    }
                    handle.start();
                    handle.switchToIndeterminate();
                    handle.progress(detail);
                }

                @Override
                public void finish() {
                    if (finished.compareAndSet(false, true)) {
                        handle.finish();
                    }
                }
            };
        }
    }

    private static final class NetBeansFeedback implements Feedback {
        private final Object outputLock = new Object();
        private InputOutput output;

        @Override
        public void failure(String title, String message, Throwable cause) {
            synchronized (outputLock) {
                if (output == null) {
                    output = IOProvider.getDefault().getIO(OUTPUT_NAME, false);
                }
                PrintWriter error = output.getErr();
                error.println(title);
                error.println(message);
                if (cause != null) {
                    cause.printStackTrace(error);
                }
                error.flush();
                output.select();
            }
            EventQueue.invokeLater(() -> {
                NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                        message,
                        NotifyDescriptor.ERROR_MESSAGE);
                descriptor.setTitle(title);
                DialogDisplayer.getDefault().notify(descriptor);
            });
        }
    }

    private static final class ActiveOperation {
        final DeviceManagerAction action;
        final DeviceManagerSelection selection;
        final String target;
        final long lifecycle;
        final DeviceManagerSnapshot baselineSnapshot;
        final ControllerProgress progress;
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean completed = new AtomicBoolean();
        RequestProcessor.Task task;

        ActiveOperation(
                DeviceManagerAction action,
                DeviceManagerSelection selection,
                String target,
                long lifecycle,
                DeviceManagerSnapshot baselineSnapshot,
                ControllerProgress progress) {
            this.action = action;
            this.selection = selection;
            this.target = target;
            this.lifecycle = lifecycle;
            this.baselineSnapshot = baselineSnapshot;
            this.progress = progress;
        }
    }

    private static final class OperationResult {
        final DeviceManagerInventory inventory;
        final String message;
        final Exception failure;

        private OperationResult(
                DeviceManagerInventory inventory,
                String message,
                Exception failure) {
            this.inventory = inventory;
            this.message = message;
            this.failure = failure;
        }

        static OperationResult success(DeviceManagerInventory inventory, String message) {
            return new OperationResult(
                    Objects.requireNonNull(inventory, "inventory"),
                    message == null ? "" : message,
                    null);
        }

        static OperationResult cancelled(String message) {
            return new OperationResult(null, message, null);
        }

        static OperationResult failure(Exception failure) {
            return new OperationResult(null, "", Objects.requireNonNull(failure, "failure"));
        }
    }
}
