package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterDevice;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.junit.jupiter.api.Test;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class FlutterProjectConfigurationProviderTest {
    private static final FlutterDevice WINDOWS =
            new FlutterDevice("windows", "Windows", "windows-x64", false);
    private static final FlutterDevice PIXEL =
            new FlutterDevice("emulator-5554", "Pixel 9", "android-arm64", true);
    private static final FlutterDevice CHROME =
            new FlutterDevice("chrome", "Chrome", "web-javascript", false);

    @Test
    void successfulPlatformAdditionRefreshesTargetsExactlyOnce() {
        List<Boolean> successResults = new ArrayList<>();
        AtomicInteger refreshes = new AtomicInteger();
        ActionProgress success = FlutterProjectActionProvider
                .refreshTargetsAfterSuccessfulPlatformAdd(
                        recordingProgress(successResults),
                        refreshes::incrementAndGet);

        success.finished(true);
        success.finished(true);

        assertEquals(List.of(true), successResults);
        assertEquals(1, refreshes.get());

        List<Boolean> failureResults = new ArrayList<>();
        ActionProgress failure = FlutterProjectActionProvider
                .refreshTargetsAfterSuccessfulPlatformAdd(
                        recordingProgress(failureResults),
                        refreshes::incrementAndGet);

        failure.finished(false);

        assertEquals(List.of(false), failureResults);
        assertEquals(1, refreshes.get(), "failed platform creation must not refresh targets");
    }

    @Test
    void publishesSortedTargetsAndRestoresRememberedSelection() {
        FakeBackend backend = new FakeBackend(List.of(CHROME, PIXEL, WINDOWS), CHROME.id());
        FlutterProjectConfigurationProvider provider = provider(backend);

        provider.start();

        List<FlutterTargetConfiguration> configurations =
                List.copyOf(provider.getConfigurations());
        assertEquals(List.of("windows", "emulator-5554", "chrome"),
                configurations.stream().map(FlutterTargetConfiguration::id).toList());
        assertEquals("chrome", provider.getActiveConfiguration().id());
        assertEquals("[Web] Chrome",
                provider.getActiveConfiguration().getDisplayName());
        assertEquals(1, backend.discoveryCount);

        provider.getConfigurations();
        provider.getActiveConfiguration();
        assertEquals(1, backend.discoveryCount, "cached SPI getters must not run Flutter CLI");
    }

    @Test
    void missingRememberedTargetFallsBackToFirstAndPersistsIt() {
        FakeBackend backend = new FakeBackend(List.of(CHROME, WINDOWS), "disconnected");
        FlutterProjectConfigurationProvider provider = provider(backend);

        provider.start();

        assertEquals("windows", backend.selectedId);
        assertEquals("windows", provider.getActiveConfiguration().id());
    }

    @Test
    void toolbarSelectionPersistsAndFiresOnlyActiveConfigurationChange() throws Exception {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS, CHROME), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        List<String> events = new ArrayList<>();
        provider.addPropertyChangeListener(event -> events.add(event.getPropertyName()));

        FlutterTargetConfiguration chrome = provider.getConfigurations().stream()
                .filter(configuration -> configuration.id().equals(CHROME.id()))
                .findFirst()
                .orElseThrow();
        provider.setActiveConfiguration(chrome);

        assertEquals(CHROME.id(), backend.selectedId);
        assertEquals(CHROME.id(), provider.getActiveConfiguration().id());
        assertEquals(List.of(ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE), events);
    }

    @Test
    void userSelectionWinsAgainstRefreshSnapshotAlreadyComputed() throws Exception {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS, CHROME), WINDOWS.id());
        QueuedExecutor executor = new QueuedExecutor();
        FlutterProjectConfigurationProvider provider =
                new FlutterProjectConfigurationProvider(backend, executor);
        provider.start();
        executor.runNext();

        FlutterTargetConfiguration chrome = provider.getConfigurations().stream()
                .filter(configuration -> configuration.id().equals(CHROME.id()))
                .findFirst()
                .orElseThrow();
        CountDownLatch refreshReadSelection = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        backend.selectedTargetReadHook = () -> {
            refreshReadSelection.countDown();
            await(releaseRefresh, "release stale refresh snapshot");
        };

        provider.refreshTargets();
        AtomicReference<Throwable> refreshFailure = new AtomicReference<>();
        Thread refresh = Thread.ofVirtual().start(
                () -> runAndCapture(executor::runNext, refreshFailure));
        assertTrue(refreshReadSelection.await(5, TimeUnit.SECONDS),
                "refresh did not read the old target");

        AtomicReference<Throwable> selectionFailure = new AtomicReference<>();
        CountDownLatch selectionFinished = new CountDownLatch(1);
        Thread selection = Thread.ofVirtual().start(() -> {
            try {
                provider.setActiveConfiguration(chrome);
            } catch (Throwable failure) {
                selectionFailure.set(failure);
            } finally {
                selectionFinished.countDown();
            }
        });
        boolean selectionCompletedBeforeRefresh =
                selectionFinished.await(5, TimeUnit.SECONDS);
        releaseRefresh.countDown();
        selection.join(5_000);
        refresh.join(5_000);

        assertTrue(selectionCompletedBeforeRefresh,
                "toolbar selection must not wait for an older discovery snapshot");
        assertFalse(selection.isAlive(), "toolbar selection did not finish");
        assertFalse(refresh.isAlive(), "stale refresh did not finish");
        assertNull(selectionFailure.get());
        assertNull(refreshFailure.get());
        assertEquals(CHROME.id(), backend.selectedId);
        assertEquals(CHROME.id(), provider.getActiveConfiguration().id());
    }

    @Test
    void rejectsConfigurationThatIsNotInCurrentSnapshot() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> provider.setActiveConfiguration(new FlutterTargetConfiguration(CHROME)));

        assertTrue(failure.getMessage().contains("chrome"));
        assertEquals(WINDOWS.id(), backend.selectedId);
    }

    @Test
    void emptyDiscoveryProducesNoActiveConfiguration() {
        FakeBackend backend = new FakeBackend(List.of(), "missing");
        FlutterProjectConfigurationProvider provider = provider(backend);

        provider.start();

        assertTrue(provider.getConfigurations().isEmpty());
        assertNull(provider.getActiveConfiguration());
        assertEquals("missing", backend.selectedId);
    }

    @Test
    void refreshFailurePreservesLastGoodSnapshot() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        RuntimeException failure = new RuntimeException("flutter devices failed");
        backend.failure = failure;

        provider.refreshTargets();

        assertEquals(List.of("windows"), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertSame(failure, provider.lastRefreshFailure());
    }

    @Test
    void automaticRefreshPublishesConnectedAndDisconnectedDevices() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        ManualScheduler scheduler = new ManualScheduler();
        FlutterProjectConfigurationProvider provider = provider(backend, scheduler);

        provider.start();

        assertEquals(FlutterProjectConfigurationProvider.SUCCESS_REFRESH_DELAY_MILLIS,
                scheduler.nextDelayMillis());
        backend.devices = List.of(WINDOWS, PIXEL);
        scheduler.runNext();
        assertEquals(List.of(WINDOWS.id(), PIXEL.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());

        backend.devices = List.of(PIXEL);
        scheduler.runNext();
        assertEquals(List.of(PIXEL.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(3, backend.discoveryCount);
    }

    @Test
    void manualRefreshInvalidatesOlderScheduledPoll() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        ManualScheduler scheduler = new ManualScheduler();
        FlutterProjectConfigurationProvider provider = provider(backend, scheduler);
        provider.start();

        provider.refreshTargets();

        assertEquals(2, backend.discoveryCount);
        assertEquals(2, scheduler.size());
        scheduler.runNext();
        assertEquals(2, backend.discoveryCount, "stale timer must not run Flutter CLI");
        scheduler.runNext();
        assertEquals(3, backend.discoveryCount);
    }

    @Test
    void refreshRequestsAreCoalescedWhileDiscoveryIsRunning() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        QueuedExecutor executor = new QueuedExecutor();
        ManualScheduler scheduler = new ManualScheduler();
        FlutterProjectConfigurationProvider provider =
                new FlutterProjectConfigurationProvider(backend, executor, scheduler);
        provider.start();

        provider.refreshTargets();
        provider.refreshTargets();

        assertEquals(1, executor.size());
        executor.runNext();
        assertEquals(1, backend.discoveryCount);
        assertEquals(1, executor.size(), "only one follow-up refresh must be queued");
        executor.runNext();
        assertEquals(2, backend.discoveryCount);
        assertEquals(0, executor.size());
        assertEquals(1, scheduler.size());
    }

    @Test
    void failedAutomaticRefreshBacksOffIndefinitelyAndRecovers() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        RuntimeException failure = new RuntimeException("flutter devices failed");
        backend.failure = failure;
        ManualScheduler scheduler = new ManualScheduler();
        FlutterProjectConfigurationProvider provider = provider(backend, scheduler);

        provider.start();

        assertEquals(2_000, scheduler.nextDelayMillis());
        scheduler.runNext();
        assertEquals(5_000, scheduler.nextDelayMillis());
        scheduler.runNext();
        assertEquals(15_000, scheduler.nextDelayMillis());
        scheduler.runNext();
        assertEquals(30_000, scheduler.nextDelayMillis());
        scheduler.runNext();
        assertEquals(30_000, scheduler.nextDelayMillis(), "failure backoff must remain capped");

        backend.failure = null;
        scheduler.runNext();

        assertNull(provider.lastRefreshFailure());
        assertEquals(List.of(WINDOWS.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(FlutterProjectConfigurationProvider.SUCCESS_REFRESH_DELAY_MILLIS,
                scheduler.nextDelayMillis());
    }

    @Test
    void closingProjectInvalidatesScheduledAutomaticRefresh() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        ManualScheduler scheduler = new ManualScheduler();
        FlutterProjectConfigurationProvider provider = provider(backend, scheduler);
        provider.start();

        provider.close();
        scheduler.runNext();

        assertEquals(1, backend.discoveryCount);
    }

    @Test
    void closeAndReopenDuringDiscoveryQueuesOneCurrentLifecycleRefresh() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        QueuedExecutor executor = new QueuedExecutor();
        ManualScheduler scheduler = new ManualScheduler();
        FlutterProjectConfigurationProvider provider =
                new FlutterProjectConfigurationProvider(backend, executor, scheduler);
        provider.start();

        provider.close();
        backend.devices = List.of(CHROME);
        backend.selectedId = CHROME.id();
        provider.start();

        assertEquals(1, executor.size());
        executor.runNext();
        assertTrue(provider.getConfigurations().isEmpty(),
                "closed-lifecycle discovery must not publish after reopen");
        assertEquals(1, executor.size());
        executor.runNext();

        assertEquals(2, backend.discoveryCount);
        assertEquals(List.of(CHROME.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(1, scheduler.size());
    }

    @Test
    void lateRefreshIsIgnoredAfterProjectCloses() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        QueuedExecutor executor = new QueuedExecutor();
        FlutterProjectConfigurationProvider provider =
                new FlutterProjectConfigurationProvider(backend, executor);
        provider.start();

        provider.close();
        executor.runAll();

        assertTrue(provider.getConfigurations().isEmpty());
        assertNull(provider.getActiveConfiguration());
    }

    @Test
    void providerCanRefreshAgainWhenTheSameProjectReopens() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        provider.close();
        backend.devices = List.of(CHROME);
        backend.selectedId = CHROME.id();

        provider.start();

        assertEquals(List.of(CHROME.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(CHROME.id(), provider.getActiveConfiguration().id());
        assertEquals(2, backend.discoveryCount);
    }

    @Test
    void closeDuringListenerAttachmentDoesNotLeakListener() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        backend.addListenerHook = provider::close;

        provider.start();

        assertTrue(backend.listeners.isEmpty());
        assertEquals(0, backend.discoveryCount);
    }

    @Test
    void queuedControllerChangeAfterCloseCannotPersistFallback() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        provider.close();
        backend.devices = List.of(CHROME);
        backend.selectedId = "disconnected";

        provider.stateChanged(new ChangeEvent(backend));

        assertEquals("disconnected", backend.selectedId);
        assertEquals(List.of(WINDOWS.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
    }

    @Test
    void selectionRemovedDuringBackendUpdateNeverBecomesActive() throws Exception {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS, CHROME), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        backend.removeSelectedTargetOnSelection = true;
        FlutterTargetConfiguration chrome = provider.getConfigurations().stream()
                .filter(configuration -> configuration.id().equals(CHROME.id()))
                .findFirst()
                .orElseThrow();

        provider.setActiveConfiguration(chrome);

        assertEquals(List.of(WINDOWS.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(WINDOWS.id(), provider.getActiveConfiguration().id());
        assertEquals(WINDOWS.id(), backend.selectedId);
    }

    @Test
    void targetIdentityRemainsStableWhenMetadataChanges() {
        FlutterTargetConfiguration oldTarget = new FlutterTargetConfiguration(WINDOWS);
        FlutterTargetConfiguration renamedTarget = new FlutterTargetConfiguration(
                new FlutterDevice("windows", "Windows Desktop", "windows-arm64", false));

        assertEquals(oldTarget, renamedTarget);
        assertEquals(oldTarget.hashCode(), renamedTarget.hashCode());
        assertFalse(oldTarget.getDisplayName().equals(renamedTarget.getDisplayName()));
    }

    @Test
    void metadataChangePublishesConfigurationsWithoutChangingActiveIdentity() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        List<String> events = new ArrayList<>();
        provider.addPropertyChangeListener(event -> events.add(event.getPropertyName()));
        backend.devices = List.of(
                new FlutterDevice("windows", "Windows Desktop", "windows-arm64", false));

        backend.fireChange();

        assertEquals("[Desktop] Windows Desktop",
                provider.getActiveConfiguration().getDisplayName());
        assertEquals(List.of(ProjectConfigurationProvider.PROP_CONFIGURATIONS), events);
    }

    @Test
    void deviceAddAndSelectedDeviceRemovalAreAppliedFromBackendChanges() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        backend.devices = List.of(WINDOWS, PIXEL);
        backend.selectedId = PIXEL.id();

        backend.fireChange();

        assertEquals(List.of(WINDOWS.id(), PIXEL.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(PIXEL.id(), provider.getActiveConfiguration().id());

        backend.devices = List.of(WINDOWS);
        backend.fireChange();

        assertEquals(List.of(WINDOWS.id()), provider.getConfigurations().stream()
                .map(FlutterTargetConfiguration::id).toList());
        assertEquals(WINDOWS.id(), provider.getActiveConfiguration().id());
        assertEquals(WINDOWS.id(), backend.selectedId);
    }

    @Test
    void declaresThatTargetSelectionAffectsRunDebugAndBuildArtifacts() {
        FlutterProjectConfigurationProvider provider = provider(
                new FakeBackend(List.of(), ""));

        assertTrue(provider.configurationsAffectAction(ActionProvider.COMMAND_RUN));
        assertTrue(provider.configurationsAffectAction(ActionProvider.COMMAND_DEBUG));
        assertTrue(provider.configurationsAffectAction(ActionProvider.COMMAND_BUILD));
        assertTrue(provider.configurationsAffectAction(ActionProvider.COMMAND_REBUILD));
        assertFalse(provider.configurationsAffectAction(ActionProvider.COMMAND_CLEAN));
    }

    @Test
    void buildTargetResolutionUsesContextSnapshotBeforeCachedToolbarSelection() {
        FakeBackend backend = new FakeBackend(List.of(WINDOWS, CHROME), WINDOWS.id());
        FlutterProjectConfigurationProvider provider = provider(backend);
        provider.start();
        FlutterTargetConfiguration contextual = new FlutterTargetConfiguration(CHROME);

        assertSame(CHROME, FlutterProjectActionProvider.resolveBuildTarget(
                Lookups.singleton(contextual), provider));
        assertSame(WINDOWS, FlutterProjectActionProvider.resolveBuildTarget(
                Lookup.EMPTY, provider));
    }

    private static FlutterProjectConfigurationProvider provider(FakeBackend backend) {
        return new FlutterProjectConfigurationProvider(backend, Runnable::run);
    }

    private static ActionProgress recordingProgress(List<Boolean> results) {
        return new ActionProgress() {
            @Override
            protected void started() {
            }

            @Override
            public void finished(boolean success) {
                results.add(success);
            }
        };
    }

    private static FlutterProjectConfigurationProvider provider(
            FakeBackend backend,
            ManualScheduler scheduler) {
        return new FlutterProjectConfigurationProvider(backend, Runnable::run, scheduler);
    }

    private static final class FakeBackend
            implements FlutterProjectConfigurationProvider.Backend {
        private final List<ChangeListener> listeners = new ArrayList<>();
        private List<FlutterDevice> devices;
        private String selectedId;
        private RuntimeException failure;
        private int discoveryCount;
        private Runnable addListenerHook;
        private Runnable selectedTargetReadHook;
        private boolean removeSelectedTargetOnSelection;

        FakeBackend(List<FlutterDevice> devices, String selectedId) {
            this.devices = List.copyOf(devices);
            this.selectedId = selectedId;
        }

        @Override
        public List<FlutterDevice> discoverTargets() {
            discoveryCount++;
            if (failure != null) {
                throw failure;
            }
            return devices;
        }

        @Override
        public List<FlutterDevice> availableTargets() {
            return devices;
        }

        @Override
        public String selectedTargetId() {
            String captured = selectedId;
            Runnable hook = selectedTargetReadHook;
            selectedTargetReadHook = null;
            if (hook != null) {
                hook.run();
            }
            return captured;
        }

        @Override
        public void selectTarget(FlutterDevice device) {
            selectedId = device.id();
            if (removeSelectedTargetOnSelection) {
                devices = devices.stream()
                        .filter(candidate -> !candidate.id().equals(device.id()))
                        .toList();
                removeSelectedTargetOnSelection = false;
            }
            ChangeEvent event = new ChangeEvent(this);
            List.copyOf(listeners).forEach(listener -> listener.stateChanged(event));
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            listeners.add(listener);
            if (addListenerHook != null) {
                Runnable hook = addListenerHook;
                addListenerHook = null;
                hook.run();
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            listeners.remove(listener);
        }

        void fireChange() {
            ChangeEvent event = new ChangeEvent(this);
            List.copyOf(listeners).forEach(listener -> listener.stateChanged(event));
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runNext() {
            tasks.removeFirst().run();
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.removeFirst().run();
            }
        }

        int size() {
            return tasks.size();
        }
    }

    private static final class ManualScheduler
            implements FlutterProjectConfigurationProvider.RetryScheduler {
        private final ArrayDeque<ScheduledTask> tasks = new ArrayDeque<>();

        @Override
        public void schedule(Runnable task, int delayMillis) {
            tasks.add(new ScheduledTask(task, delayMillis));
        }

        int nextDelayMillis() {
            return tasks.getFirst().delayMillis();
        }

        void runNext() {
            tasks.removeFirst().task().run();
        }

        int size() {
            return tasks.size();
        }

        private record ScheduledTask(Runnable task, int delayMillis) {
        }
    }

    private static void await(CountDownLatch latch, String operation) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to " + operation);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting to " + operation, ex);
        }
    }

    private static void runAndCapture(Runnable task, AtomicReference<Throwable> failure) {
        try {
            task.run();
        } catch (Throwable ex) {
            failure.set(ex);
        }
    }
}
