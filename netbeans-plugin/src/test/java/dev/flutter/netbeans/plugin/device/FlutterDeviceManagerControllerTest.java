package dev.flutter.netbeans.plugin.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.run.AndroidAvdCreateRequest;
import dev.flutter.netbeans.run.AndroidDeviceDefinition;
import dev.flutter.netbeans.run.AndroidSystemImage;
import java.awt.Component;
import java.awt.EventQueue;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class FlutterDeviceManagerControllerTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void openRefreshesInventoryAndFinishesProgress() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        RecordingView view = new RecordingView();
        RecordingProgressFactory progress = new RecordingProgressFactory();
        RecordingFeedback feedback = new RecordingFeedback();
        FlutterDeviceManagerController controller = controller(
                view, backend, options -> Optional.empty(), progress, feedback);

        controller.open();
        DeviceManagerSnapshot ready = view.await(snapshot ->
                !snapshot.refreshing()
                        && snapshot.toolchain().state() == AndroidToolchainStatus.State.READY);

        assertEquals(1, backend.refreshCalls.get());
        assertEquals(1, ready.avds().size());
        assertEquals("Found one AVD.", ready.message());
        assertTrue(progress.await(0).started.get());
        assertTrue(progress.await(0).finished.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertTrue(feedback.failures.isEmpty());
        closeAndAwait(controller, backend);
    }

    @Test
    void refreshFailureRestoresStableToolchainStateInsteadOfLeavingDiscovering() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        backend.refreshFailure = new IOException("adb inventory failed");
        RecordingView view = new RecordingView();
        RecordingFeedback feedback = new RecordingFeedback();
        FlutterDeviceManagerController controller = controller(
                view,
                backend,
                options -> Optional.empty(),
                new RecordingProgressFactory(),
                feedback);

        controller.open();
        DeviceManagerSnapshot failed = view.await(snapshot ->
                !snapshot.refreshing() && snapshot.message().contains("adb inventory failed"));

        assertEquals(AndroidToolchainStatus.State.UNAVAILABLE, failed.toolchain().state());
        assertFalse(failed.refreshing());
        assertNotNull(feedback.awaitFailure());
        closeAndAwait(controller, backend);
    }

    @Test
    void successfulStartIsFollowedByRefreshAndPublishesBackendMessage() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        RecordingView view = new RecordingView();
        RecordingProgressFactory progress = new RecordingProgressFactory();
        FlutterDeviceManagerController controller = controller(
                view, backend, options -> Optional.empty(), progress, new RecordingFeedback());
        controller.open();
        view.await(snapshot -> !snapshot.refreshing());

        controller.perform(
                DeviceManagerAction.START,
                DeviceManagerSelection.avd("Pixel_API_35"));
        DeviceManagerSnapshot result = view.await(snapshot ->
                !snapshot.refreshing() && snapshot.message().startsWith("Started Pixel_API_35"));

        assertEquals(
                List.of("refresh", "start:Pixel_API_35", "refresh"),
                backend.calls);
        assertEquals(AndroidVirtualDevice.State.RUNNING, result.avds().getFirst().state());
        assertTrue(progress.await(1).finished.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        closeAndAwait(controller, backend);
    }

    @Test
    void createLoadsOptionsOnWorkerAndOpensProviderOnEdt() throws Exception {
        FakeBackend backend = new FakeBackend(emptyInventory());
        RecordingView view = new RecordingView();
        RecordingProgressFactory progress = new RecordingProgressFactory();
        AtomicBoolean providerOnEdt = new AtomicBoolean();
        AndroidAvdCreateRequest request = new AndroidAvdCreateRequest(
                "Tablet_API_35",
                "system-images;android-35;google_apis;x86_64");
        FlutterDeviceManagerController controller = controller(
                view,
                backend,
                options -> {
                    providerOnEdt.set(EventQueue.isDispatchThread());
                    assertEquals(1, options.systemImages().size());
                    return Optional.of(request);
                },
                progress,
                new RecordingFeedback());
        controller.open();
        view.await(snapshot -> !snapshot.refreshing());

        controller.perform(DeviceManagerAction.CREATE, DeviceManagerSelection.none());
        view.await(snapshot ->
                !snapshot.refreshing() && snapshot.message().contains("Tablet_API_35"));

        assertTrue(providerOnEdt.get());
        assertEquals(request, backend.created);
        assertEquals(
                List.of("refresh", "creationOptions", "create:Tablet_API_35", "refresh"),
                backend.calls);
        closeAndAwait(controller, backend);
    }

    @Test
    void backendFailureIsWrittenToFeedbackAndInventoryIsRefreshed() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        backend.startFailure = new IOException("emulator executable is missing");
        RecordingView view = new RecordingView();
        RecordingFeedback feedback = new RecordingFeedback();
        FlutterDeviceManagerController controller = controller(
                view,
                backend,
                options -> Optional.empty(),
                new RecordingProgressFactory(),
                feedback);
        controller.open();
        view.await(snapshot -> !snapshot.refreshing());

        controller.perform(
                DeviceManagerAction.START,
                DeviceManagerSelection.avd("Pixel_API_35"));
        Failure failure = feedback.awaitFailure();
        DeviceManagerSnapshot recovered = view.await(snapshot ->
                !snapshot.refreshing() && snapshot.message().contains("emulator executable is missing"));

        assertTrue(failure.title().contains("start the Android Virtual Device failed"));
        assertTrue(failure.message().contains("Pixel 8 (Pixel_API_35)"));
        assertTrue(failure.message().contains("emulator executable is missing"));
        assertNotNull(failure.cause());
        assertEquals(2, backend.refreshCalls.get());
        assertFalse(recovered.refreshing());
        closeAndAwait(controller, backend);
    }

    @Test
    void progressCancellationInterruptsBootWaitThenRefreshesInventory() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        backend.blockStart = true;
        RecordingView view = new RecordingView();
        RecordingProgressFactory progress = new RecordingProgressFactory();
        FlutterDeviceManagerController controller = controller(
                view,
                backend,
                options -> Optional.empty(),
                progress,
                new RecordingFeedback());
        controller.open();
        view.await(snapshot -> !snapshot.refreshing());

        controller.perform(
                DeviceManagerAction.START,
                DeviceManagerSelection.avd("Pixel_API_35"));
        assertTrue(backend.startEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        RecordingProgress operationProgress = progress.await(1);
        assertTrue(operationProgress.cancel.getAsBoolean());

        assertTrue(backend.startInterrupted.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        DeviceManagerSnapshot cancelled = view.await(snapshot ->
                !snapshot.refreshing() && snapshot.message().startsWith("Cancelled"));
        assertEquals(2, backend.refreshCalls.get());
        assertFalse(cancelled.refreshing());
        assertTrue(cancelled.message().contains("emulator already started"));
        assertTrue(cancelled.message().contains("continues running"));
        assertTrue(operationProgress.finished.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertFalse(backend.calls.stream().anyMatch(call -> call.startsWith("stop:")));
        closeAndAwait(controller, backend);
    }

    @Test
    void componentCloseInterruptsWaitAndClosesBackendWithoutStoppingEmulator() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        backend.blockStart = true;
        RecordingView view = new RecordingView();
        RecordingProgressFactory progress = new RecordingProgressFactory();
        FlutterDeviceManagerController controller = controller(
                view,
                backend,
                options -> Optional.empty(),
                progress,
                new RecordingFeedback());
        controller.open();
        view.await(snapshot -> !snapshot.refreshing());
        controller.perform(
                DeviceManagerAction.START,
                DeviceManagerSelection.avd("Pixel_API_35"));
        assertTrue(backend.startEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

        controller.close();

        assertTrue(backend.startInterrupted.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertTrue(backend.closed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertTrue(progress.await(1).finished.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertFalse(backend.calls.stream().anyMatch(call -> call.startsWith("stop:")));
        assertEquals(1, backend.closeCalls.get());
    }

    @Test
    void secondOperationIsRejectedWhileSerializedWorkerIsBusy() throws Exception {
        FakeBackend backend = new FakeBackend(stoppedInventory());
        backend.blockStart = true;
        RecordingView view = new RecordingView();
        RecordingProgressFactory progress = new RecordingProgressFactory();
        RecordingFeedback feedback = new RecordingFeedback();
        FlutterDeviceManagerController controller = controller(
                view, backend, options -> Optional.empty(), progress, feedback);
        controller.open();
        view.await(snapshot -> !snapshot.refreshing());
        controller.perform(
                DeviceManagerAction.START,
                DeviceManagerSelection.avd("Pixel_API_35"));
        assertTrue(backend.startEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

        controller.perform(DeviceManagerAction.REFRESH, DeviceManagerSelection.none());
        Failure busy = feedback.awaitFailure();
        assertTrue(busy.title().contains("busy"));
        assertTrue(busy.message().contains("still running"));
        assertEquals(1, backend.refreshCalls.get());

        assertTrue(progress.await(1).cancel.getAsBoolean());
        view.await(snapshot -> !snapshot.refreshing());
        closeAndAwait(controller, backend);
    }

    private static FlutterDeviceManagerController controller(
            RecordingView view,
            FakeBackend backend,
            AndroidAvdCreationProvider creationProvider,
            RecordingProgressFactory progress,
            RecordingFeedback feedback) {
        return new FlutterDeviceManagerController(
                view,
                backend,
                creationProvider,
                progress,
                feedback);
    }

    private static void closeAndAwait(
            FlutterDeviceManagerController controller,
            FakeBackend backend) throws Exception {
        controller.close();
        assertTrue(backend.closed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    }

    private static DeviceManagerInventory stoppedInventory() {
        return inventory(
                List.of(),
                List.of(avd(AndroidVirtualDevice.State.STOPPED, "")),
                "Found one AVD.");
    }

    private static DeviceManagerInventory emptyInventory() {
        return inventory(List.of(), List.of(), "No Android devices or AVDs were found.");
    }

    private static DeviceManagerInventory runningInventory() {
        ConnectedAndroidDevice connected = new ConnectedAndroidDevice(
                "emulator-5554",
                "Pixel 8",
                "15",
                "35",
                true,
                ConnectedAndroidDevice.State.ONLINE,
                "");
        return inventory(
                List.of(connected),
                List.of(avd(AndroidVirtualDevice.State.RUNNING, connected.id())),
                "Found one running AVD.");
    }

    private static DeviceManagerInventory inventory(
            List<ConnectedAndroidDevice> devices,
            List<AndroidVirtualDevice> avds,
            String message) {
        return new DeviceManagerInventory(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                devices,
                avds,
                "",
                message);
    }

    private static AndroidVirtualDevice avd(
            AndroidVirtualDevice.State state,
            String connectedDeviceId) {
        return new AndroidVirtualDevice(
                "Pixel_API_35",
                "Pixel 8",
                "pixel_8",
                "35",
                "x86_64",
                state,
                connectedDeviceId,
                "");
    }

    private static final class FakeBackend implements AndroidDeviceManagerBackend {
        final List<String> calls = new CopyOnWriteArrayList<>();
        final AtomicInteger refreshCalls = new AtomicInteger();
        final AtomicInteger closeCalls = new AtomicInteger();
        final CountDownLatch startEntered = new CountDownLatch(1);
        final CountDownLatch startInterrupted = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        volatile DeviceManagerInventory inventory;
        volatile IOException refreshFailure;
        volatile IOException startFailure;
        volatile boolean blockStart;
        volatile AndroidAvdCreateRequest created;

        FakeBackend(DeviceManagerInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public DeviceManagerInventory refresh() throws IOException {
            calls.add("refresh");
            refreshCalls.incrementAndGet();
            if (refreshFailure != null) {
                throw refreshFailure;
            }
            return inventory;
        }

        @Override
        public CreationOptions creationOptions() {
            calls.add("creationOptions");
            return new CreationOptions(
                    List.of(new AndroidSystemImage(
                            "system-images;android-35;google_apis;x86_64",
                            "35",
                            "google_apis",
                            "x86_64",
                            "Google APIs Intel x86_64",
                            "1",
                            true)),
                    List.of(new AndroidDeviceDefinition(
                            "pixel_8",
                            "Pixel 8",
                            "Google",
                            "")),
                    Set.of());
        }

        @Override
        public void create(AndroidAvdCreateRequest request) {
            calls.add("create:" + request.name());
            created = request;
            inventory = new DeviceManagerInventory(
                    AndroidToolchainStatus.ready("C:/Android/sdk"),
                    List.of(),
                    List.of(new AndroidVirtualDevice(
                            request.name(),
                            request.name(),
                            request.deviceDefinitionId().orElse(""),
                            "35",
                            "x86_64",
                            AndroidVirtualDevice.State.STOPPED,
                            "",
                            "")),
                    "",
                    "Found one AVD.");
        }

        @Override
        public String start(String avdId) throws IOException, InterruptedException {
            calls.add("start:" + avdId);
            startEntered.countDown();
            if (startFailure != null) {
                throw startFailure;
            }
            inventory = runningInventory();
            if (blockStart) {
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException interrupted) {
                    startInterrupted.countDown();
                    throw interrupted;
                }
            }
            return "Started " + avdId + ".";
        }

        @Override
        public String stop(String avdId) {
            calls.add("stop:" + avdId);
            inventory = stoppedInventory();
            return "Stopped " + avdId + ".";
        }

        @Override
        public String restart(String avdId) {
            calls.add("restart:" + avdId);
            inventory = runningInventory();
            return "Restarted " + avdId + ".";
        }

        @Override
        public String wipe(String avdId) {
            calls.add("wipe:" + avdId);
            inventory = runningInventory();
            return "Wiped " + avdId + ".";
        }

        @Override
        public String delete(String avdId) {
            calls.add("delete:" + avdId);
            inventory = emptyInventory();
            return "Deleted " + avdId + ".";
        }

        @Override
        public String selectTarget(DeviceManagerSelection selection) {
            calls.add("select:" + selection.id());
            return "Selected " + selection.id() + ".";
        }

        @Override
        public void close() {
            calls.add("close");
            closeCalls.incrementAndGet();
            closed.countDown();
        }
    }

    private static final class RecordingView implements FlutterDeviceManagerController.View {
        private final Object monitor = new Object();
        private final List<DeviceManagerSnapshot> snapshots = new ArrayList<>();
        private final JPanel component = new JPanel();

        @Override
        public void publish(DeviceManagerSnapshot snapshot) {
            synchronized (monitor) {
                snapshots.add(snapshot);
                monitor.notifyAll();
            }
        }

        @Override
        public Component parentComponent() {
            return component;
        }

        DeviceManagerSnapshot await(Predicate<DeviceManagerSnapshot> predicate)
                throws InterruptedException {
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            synchronized (monitor) {
                while (true) {
                    for (int index = snapshots.size() - 1; index >= 0; index--) {
                        DeviceManagerSnapshot snapshot = snapshots.get(index);
                        if (predicate.test(snapshot)) {
                            return snapshot;
                        }
                    }
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        throw new AssertionError("Timed out waiting for Device Manager snapshot: "
                                + snapshots);
                    }
                    TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                }
            }
        }
    }

    private static final class RecordingProgressFactory
            implements FlutterDeviceManagerController.ProgressFactory {
        private final Object monitor = new Object();
        private final List<RecordingProgress> progresses = new ArrayList<>();

        @Override
        public FlutterDeviceManagerController.ControllerProgress create(
                String displayName,
                BooleanSupplier cancel) {
            RecordingProgress progress = new RecordingProgress(displayName, cancel);
            synchronized (monitor) {
                progresses.add(progress);
                monitor.notifyAll();
            }
            return progress;
        }

        RecordingProgress await(int index) throws InterruptedException {
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            synchronized (monitor) {
                while (progresses.size() <= index) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        throw new AssertionError("Timed out waiting for progress " + index);
                    }
                    TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                }
                return progresses.get(index);
            }
        }
    }

    private static final class RecordingProgress
            implements FlutterDeviceManagerController.ControllerProgress {
        final String displayName;
        final BooleanSupplier cancel;
        final AtomicBoolean started = new AtomicBoolean();
        final CountDownLatch finished = new CountDownLatch(1);

        RecordingProgress(String displayName, BooleanSupplier cancel) {
            this.displayName = displayName;
            this.cancel = cancel;
        }

        @Override
        public void start(String detail) {
            started.set(true);
        }

        @Override
        public void finish() {
            finished.countDown();
        }
    }

    private static final class RecordingFeedback implements FlutterDeviceManagerController.Feedback {
        private final Object monitor = new Object();
        private final List<Failure> failures = new ArrayList<>();

        @Override
        public void failure(String title, String message, Throwable cause) {
            synchronized (monitor) {
                failures.add(new Failure(title, message, cause));
                monitor.notifyAll();
            }
        }

        Failure awaitFailure() throws InterruptedException {
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            synchronized (monitor) {
                while (failures.isEmpty()) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        throw new AssertionError("Timed out waiting for Device Manager failure");
                    }
                    TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                }
                return failures.getLast();
            }
        }
    }

    private record Failure(String title, String message, Throwable cause) {
    }
}
