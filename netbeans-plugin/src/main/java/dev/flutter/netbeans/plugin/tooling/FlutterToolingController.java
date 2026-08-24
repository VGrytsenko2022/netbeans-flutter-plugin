package dev.flutter.netbeans.plugin.tooling;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainStatus;
import dev.flutter.netbeans.run.FlutterToolCommand;
import dev.flutter.netbeans.run.FlutterToolCommandType;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/** Project-scoped orchestration of cancellable one-shot Flutter tooling commands. */
public final class FlutterToolingController implements AutoCloseable {
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterToolingController.class.getName(), 3, true);

    private final Project project;
    private final Path projectRoot;
    private final FlutterExecutionBackend backend;
    private final FlutterTestSessionFactory testSessions;
    private final FlutterSdkResolver sdkResolver;
    private final ChangeSupport changes = new ChangeSupport(this);
    private final Object lock = new Object();

    private volatile boolean closed;
    private volatile ActiveOperation active;
    private long lifecycleGeneration;

    public FlutterToolingController(
            Project project,
            FlutterProjectInfo projectInfo,
            FlutterTestSessionFactory testSessions) {
        this(
                project,
                projectInfo,
                new NetBeansFlutterExecutionBackend(),
                testSessions,
                FlutterToolingController::resolveFlutterSdk);
    }

    FlutterToolingController(
            Project project,
            FlutterProjectInfo projectInfo,
            FlutterExecutionBackend backend,
            FlutterTestSessionFactory testSessions,
            FlutterSdkResolver sdkResolver) {
        this.project = Objects.requireNonNull(project, "project");
        this.projectRoot = Objects.requireNonNull(projectInfo, "projectInfo")
                .root().toAbsolutePath().normalize();
        this.backend = Objects.requireNonNull(backend, "backend");
        this.testSessions = Objects.requireNonNull(testSessions, "testSessions");
        this.sdkResolver = Objects.requireNonNull(sdkResolver, "sdkResolver");
    }

    public boolean isCommandEnabled(String command) {
        if (!isToolingCommand(command)) {
            return false;
        }
        return !closed
                && project.getProjectDirectory().isValid()
                && active == null;
    }

    public void invoke(String command, Lookup context, ActionProgress progress) {
        Lookup safeContext = context == null ? Lookup.EMPTY : context;
        if (!isCommandEnabled(command)) {
            finishProgress(progress, false);
            showMessage(
                    "Flutter action unavailable",
                    "Cannot perform " + displayName(command) + " for " + projectRoot + ": "
                    + unavailableReason() + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }

        final FlutterToolCommand toolCommand;
        try {
            toolCommand = commandFor(command, safeContext);
        } catch (IllegalArgumentException ex) {
            finishProgress(progress, false);
            showMessage(
                    "Flutter test selection unavailable",
                    "Cannot perform " + displayName(command) + " for " + projectRoot + ": "
                    + ex.getMessage() + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }
        start(toolCommand, progress);
    }

    public void open() {
        synchronized (lock) {
            if (!closed) {
                return;
            }
            closed = false;
            lifecycleGeneration++;
        }
        fireChange();
    }

    @Override
    public void close() {
        ActiveOperation operation;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            lifecycleGeneration++;
            operation = active;
            active = null;
        }
        if (operation != null) {
            Future<Integer> future = operation.future;
            if (future != null) {
                future.cancel(true);
            }
            WORKER.post(() -> operation.complete(-1, true, null, false));
        }
        fireChange();
    }

    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    private FlutterToolCommand commandFor(String command, Lookup context) {
        return switch (command) {
            case FlutterProjectActionProvider.COMMAND_PUB_GET -> FlutterToolCommand.pubGet();
            case FlutterProjectActionProvider.COMMAND_ANALYZE -> FlutterToolCommand.analyze();
            case ActionProvider.COMMAND_TEST -> FlutterToolCommand.test();
            case ActionProvider.COMMAND_TEST_SINGLE -> {
                FlutterTestSelection selection = FlutterTestSelection.fromContext(
                                projectRoot,
                                context,
                                false)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "select an existing Dart test file inside the project"));
                yield selection.plainName()
                        .map(name -> FlutterToolCommand.test(selection.relativePath(), name))
                        .orElseGet(() -> FlutterToolCommand.test(selection.relativePath()));
            }
            case FlutterProjectActionProvider.COMMAND_TEST_FILE -> {
                FlutterTestSelection selection = FlutterTestSelection.fromContext(
                                projectRoot,
                                context,
                                false)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "select an existing Dart test file inside the project"));
                yield FlutterToolCommand.test(selection.relativePath());
            }
            case SingleMethod.COMMAND_RUN_SINGLE_METHOD,
                 FlutterProjectActionProvider.COMMAND_TEST_AT_CARET -> {
                FlutterTestSelection selection = FlutterTestSelection.fromContext(
                                projectRoot,
                                context,
                                command.equals(FlutterProjectActionProvider.COMMAND_TEST_AT_CARET))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "place the caret in a test(...) or testWidgets(...) declaration"));
                yield FlutterToolCommand.test(
                        selection.relativePath(),
                        selection.plainName().orElseThrow());
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported Flutter tooling command: " + command);
        };
    }

    private void start(FlutterToolCommand command, ActionProgress progress) {
        ActiveOperation operation;
        synchronized (lock) {
            if (closed || active != null || !project.getProjectDirectory().isValid()) {
                finishProgress(progress, false);
                return;
            }
            operation = new ActiveOperation(command, lifecycleGeneration, progress);
            active = operation;
        }
        fireChange();
        WORKER.post(() -> launch(operation));
    }

    private void launch(ActiveOperation operation) {
        try {
            if (!isCurrent(operation)) {
                operation.complete(-1, true, null, false);
                return;
            }
            FlutterSdk sdk = sdkResolver.resolve(operation.command.type().displayName());
            FlutterExecutionRequest request = request(operation, sdk);
            Future<Integer> future = backend.start(request);
            operation.future = future;
            if (!isCurrent(operation)) {
                future.cancel(true);
                operation.complete(-1, true, null, false);
                return;
            }

            int exitCode = future.get();
            boolean success = exitCode == 0;
            complete(operation, exitCode, false, null, success);
        } catch (CancellationException ex) {
            complete(operation, -1, true, null, false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Future<Integer> future = operation.future;
            if (future != null) {
                future.cancel(true);
            }
            complete(operation, -1, true, null, false);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            complete(operation, -1, false, cause, false);
        } catch (Exception ex) {
            complete(operation, -1, false, ex, false);
        }
    }

    private FlutterExecutionRequest request(ActiveOperation operation, FlutterSdk sdk) {
        FlutterToolCommand command = operation.command;
        String displayName = "Flutter " + command.type().displayName() + ": "
                + projectRoot.getFileName();
        if (command.type() == FlutterToolCommandType.TEST) {
            FlutterTestSessionBridge bridge = testSessions.create(
                    project,
                    projectRoot,
                    displayName,
                    command,
                    this::rerun);
            operation.testSession = bridge;
            return new FlutterExecutionRequest(
                    displayName,
                    sdk.flutterExecutable(),
                    projectRoot,
                    command.arguments(),
                    false,
                    bridge::standardOutput,
                    bridge::standardError,
                    null);
        }
        if (command.type() == FlutterToolCommandType.ANALYZE) {
            return new FlutterExecutionRequest(
                    displayName,
                    sdk.flutterExecutable(),
                    projectRoot,
                    command.arguments(),
                    true,
                    null,
                    null,
                    new FlutterAnalyzeLineConvertor(projectRoot));
        }
        return new FlutterExecutionRequest(
                displayName,
                sdk.flutterExecutable(),
                projectRoot,
                command.arguments(),
                true,
                null,
                null,
                null);
    }

    private void rerun(FlutterToolCommand command) {
        if (!isCommandEnabled(ActionProvider.COMMAND_TEST)) {
            showMessage(
                    "Flutter test rerun unavailable",
                    "Cannot rerun Flutter tests for " + projectRoot + ": "
                    + unavailableReason() + ".",
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }
        start(command, null);
    }

    private void complete(
            ActiveOperation operation,
            int exitCode,
            boolean cancelled,
            Throwable failure,
            boolean success) {
        boolean current;
        synchronized (lock) {
            current = active == operation
                    && lifecycleGeneration == operation.generation
                    && !closed;
            if (active == operation) {
                active = null;
            }
        }
        if (!operation.complete(exitCode, cancelled, failure, success)) {
            return;
        }
        fireChange();
        if (current && failure != null) {
            showMessage(
                    "Flutter " + operation.command.type().displayName() + " failed to start",
                    "Cannot run Flutter " + operation.command.type().displayName()
                    + " for " + projectRoot + " using " + executableDescription()
                    + ": " + failureMessage(failure) + ".",
                    NotifyDescriptor.ERROR_MESSAGE);
        } else if (current
                && !cancelled
                && failure == null
                && exitCode != 0
                && operation.command.type() == FlutterToolCommandType.PUB_GET) {
            showMessage(
                    "Flutter Pub Get failed",
                    "Flutter Pub Get for " + projectRoot + " finished with exit code "
                    + exitCode + ". See the Flutter Pub Get Output tab for details.",
                    NotifyDescriptor.ERROR_MESSAGE);
        }
    }

    private static FlutterSdk resolveFlutterSdk(String operation) throws IOException {
        FlutterToolchainStatus status = new FlutterToolchainService().resolve();
        return status.flutterSdk().orElseThrow(() -> new IOException(
                "cannot " + operation + " because " + status.flutterMessage()));
    }

    private boolean isCurrent(ActiveOperation operation) {
        synchronized (lock) {
            return !closed
                    && active == operation
                    && lifecycleGeneration == operation.generation;
        }
    }

    private String unavailableReason() {
        if (closed) {
            return "the Flutter project is closed";
        }
        if (!project.getProjectDirectory().isValid()) {
            return "the project directory no longer exists";
        }
        ActiveOperation operation = active;
        if (operation != null) {
            return "Flutter " + operation.command.type().displayName() + " is already running";
        }
        return "the tooling service is unavailable";
    }

    private String executableDescription() {
        return new FlutterToolchainService().resolve().flutterSdk()
                .map(sdk -> sdk.flutterExecutable().toString())
                .orElse("the configured Flutter SDK");
    }

    private static boolean isToolingCommand(String command) {
        if (command == null) {
            return false;
        }
        return switch (command) {
            case FlutterProjectActionProvider.COMMAND_PUB_GET,
                 FlutterProjectActionProvider.COMMAND_ANALYZE,
                 FlutterProjectActionProvider.COMMAND_TEST_FILE,
                 FlutterProjectActionProvider.COMMAND_TEST_AT_CARET,
                 ActionProvider.COMMAND_TEST,
                 ActionProvider.COMMAND_TEST_SINGLE,
                 SingleMethod.COMMAND_RUN_SINGLE_METHOD -> true;
            default -> false;
        };
    }

    private static String displayName(String command) {
        return switch (command) {
            case FlutterProjectActionProvider.COMMAND_PUB_GET -> "Flutter Pub Get";
            case FlutterProjectActionProvider.COMMAND_ANALYZE -> "Flutter Analyze";
            case FlutterProjectActionProvider.COMMAND_TEST_FILE,
                 ActionProvider.COMMAND_TEST_SINGLE -> "Flutter Test Current File";
            case FlutterProjectActionProvider.COMMAND_TEST_AT_CARET -> "Flutter Test at Caret";
            case SingleMethod.COMMAND_RUN_SINGLE_METHOD -> "Flutter Test Method";
            case ActionProvider.COMMAND_TEST -> "Flutter Test";
            default -> command;
        };
    }

    private void fireChange() {
        if (EventQueue.isDispatchThread()) {
            changes.fireChange();
        } else {
            EventQueue.invokeLater(changes::fireChange);
        }
    }

    private static void showMessage(String title, String message, int type) {
        Runnable display = () -> {
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(message, type);
            descriptor.setTitle(title);
            DialogDisplayer.getDefault().notify(descriptor);
        };
        if (EventQueue.isDispatchThread()) {
            display.run();
        } else {
            EventQueue.invokeLater(display);
        }
    }

    private static String failureMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    private static void finishProgress(ActionProgress progress, boolean success) {
        if (progress != null) {
            progress.finished(success);
        }
    }

    private static final class ActiveOperation {
        private final FlutterToolCommand command;
        private final long generation;
        private final ActionProgress progress;
        private final AtomicBoolean completed = new AtomicBoolean();
        private volatile Future<Integer> future;
        private volatile FlutterTestSessionBridge testSession = FlutterTestSessionBridge.NONE;

        ActiveOperation(
                FlutterToolCommand command,
                long generation,
                ActionProgress progress) {
            this.command = command;
            this.generation = generation;
            this.progress = progress;
        }

        boolean complete(
                int exitCode,
                boolean cancelled,
                Throwable failure,
                boolean success) {
            if (!completed.compareAndSet(false, true)) {
                return false;
            }
            try {
                testSession.finish(exitCode, cancelled, failure);
            } finally {
                finishProgress(progress, success && !cancelled && failure == null);
            }
            return true;
        }
    }
}
