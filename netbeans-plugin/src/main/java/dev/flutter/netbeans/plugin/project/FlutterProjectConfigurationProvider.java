package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.run.FlutterTargetKind;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.util.RequestProcessor;

/**
 * Cached bridge from Flutter devices to NetBeans' native project-configuration combo.
 * The SPI getters are intentionally non-blocking because NetBeans calls them while
 * holding the project read mutex.
 */
final class FlutterProjectConfigurationProvider
        implements ProjectConfigurationProvider<FlutterTargetConfiguration>,
        ChangeListener,
        AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterProjectConfigurationProvider.class.getName());
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterProjectConfigurationProvider.class.getName(), 2, true);
    static final int SUCCESS_REFRESH_DELAY_MILLIS = 5_000;
    private static final int[] FAILURE_REFRESH_DELAYS_MILLIS = {
        2_000, 5_000, 15_000, 30_000
    };

    private final Backend backend;
    private final Executor executor;
    private final RetryScheduler retryScheduler;
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final Object lock = new Object();

    private List<FlutterTargetConfiguration> configurations = List.of();
    private FlutterTargetConfiguration activeConfiguration;
    private boolean started;
    private boolean listenerAttached;
    private boolean refreshInProgress;
    private boolean refreshQueued;
    private long lifecycleGeneration;
    private long refreshGeneration;
    private long activeRefreshGeneration;
    private long scheduledRefreshGeneration;
    private long targetRevision;
    private int consecutiveRefreshFailures;
    private Throwable lastRefreshFailure;

    FlutterProjectConfigurationProvider(FlutterRunController controller) {
        this(
                new ControllerBackend(controller),
                task -> WORKER.post(task),
                (task, delayMillis) -> WORKER.post(task, delayMillis));
    }

    FlutterProjectConfigurationProvider(Backend backend, Executor executor) {
        this(backend, executor, (task, delayMillis) -> {
        });
    }

    FlutterProjectConfigurationProvider(
            Backend backend,
            Executor executor,
            RetryScheduler retryScheduler) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.retryScheduler = Objects.requireNonNull(retryScheduler, "retryScheduler");
    }

    void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            started = true;
            lifecycleGeneration++;
            scheduledRefreshGeneration++;
            refreshQueued = false;
            targetRevision++;
            consecutiveRefreshFailures = 0;
            lastRefreshFailure = null;
        }
        backend.addChangeListener(this);
        boolean detach;
        synchronized (lock) {
            detach = !started;
            listenerAttached = !detach;
        }
        if (detach) {
            backend.removeChangeListener(this);
            return;
        }
        refreshTargets();
    }

    void refreshTargets() {
        RefreshAttempt attempt;
        synchronized (lock) {
            if (!started) {
                return;
            }
            scheduledRefreshGeneration++;
            if (refreshInProgress) {
                refreshQueued = true;
                return;
            }
            attempt = beginRefreshLocked();
        }
        executeRefresh(attempt);
    }

    @Override
    public Collection<FlutterTargetConfiguration> getConfigurations() {
        synchronized (lock) {
            return configurations;
        }
    }

    @Override
    public FlutterTargetConfiguration getActiveConfiguration() {
        synchronized (lock) {
            return activeConfiguration;
        }
    }

    @Override
    public void setActiveConfiguration(FlutterTargetConfiguration configuration)
            throws IllegalArgumentException, IOException {
        Objects.requireNonNull(configuration, "configuration");
        FlutterTargetConfiguration canonical;
        synchronized (lock) {
            if (!started) {
                throw new IOException("Flutter project is not open");
            }
            canonical = configurations.stream()
                    .filter(configuration::equals)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown Flutter run target: " + configuration.id()));
            targetRevision++;
            backend.selectTarget(canonical.device());
        }
        synchronizeSnapshot();
    }

    @Override
    public boolean hasCustomizer() {
        return false;
    }

    @Override
    public void customize() {
        // No customizer entry is exposed by hasCustomizer().
    }

    @Override
    public boolean configurationsAffectAction(String command) {
        return ActionProvider.COMMAND_RUN.equals(command)
                || ActionProvider.COMMAND_DEBUG.equals(command)
                || ActionProvider.COMMAND_BUILD.equals(command)
                || ActionProvider.COMMAND_REBUILD.equals(command);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        synchronized (lock) {
            if (!started) {
                return;
            }
            targetRevision++;
        }
        synchronizeSnapshot();
    }

    Throwable lastRefreshFailure() {
        synchronized (lock) {
            return lastRefreshFailure;
        }
    }

    private void synchronizeSnapshot() {
        synchronizeSnapshot(-1);
    }

    private void synchronizeSnapshot(long expectedLifecycleGeneration) {
        final long observedTargetRevision;
        synchronized (lock) {
            if (!started || (expectedLifecycleGeneration >= 0
                    && expectedLifecycleGeneration != lifecycleGeneration)) {
                return;
            }
            observedTargetRevision = targetRevision;
        }
        List<FlutterTargetConfiguration> next = configurationsOf(backend.availableTargets());
        String selectedId = backend.selectedTargetId();
        FlutterTargetConfiguration nextActive = next.stream()
                .filter(configuration -> configuration.id().equals(selectedId))
                .findFirst()
                .orElse(next.isEmpty() ? null : next.getFirst());
        boolean persistFallback = nextActive != null && !nextActive.id().equals(selectedId);

        List<FlutterTargetConfiguration> previousConfigurations;
        FlutterTargetConfiguration previousActive;
        boolean configurationsChanged;
        boolean activeChanged;
        synchronized (lock) {
            if (!started || (expectedLifecycleGeneration >= 0
                    && expectedLifecycleGeneration != lifecycleGeneration)
                    || observedTargetRevision != targetRevision) {
                return;
            }
            previousConfigurations = configurations;
            previousActive = activeConfiguration;
            configurationsChanged = !sameSnapshot(previousConfigurations, next);
            activeChanged = !Objects.equals(previousActive, nextActive);
            configurations = next;
            activeConfiguration = nextActive;
        }
        if (configurationsChanged) {
            changes.firePropertyChange(PROP_CONFIGURATIONS, null, next);
        }
        if (activeChanged) {
            changes.firePropertyChange(PROP_CONFIGURATION_ACTIVE, previousActive, nextActive);
        }
        if (persistFallback) {
            synchronized (lock) {
                if (started
                        && (expectedLifecycleGeneration < 0
                        || expectedLifecycleGeneration == lifecycleGeneration)
                        && observedTargetRevision == targetRevision
                        && Objects.equals(activeConfiguration, nextActive)) {
                    backend.selectTarget(nextActive.device());
                }
            }
        }
    }

    private RefreshAttempt beginRefreshLocked() {
        refreshInProgress = true;
        activeRefreshGeneration = ++refreshGeneration;
        return new RefreshAttempt(lifecycleGeneration, activeRefreshGeneration);
    }

    private void executeRefresh(RefreshAttempt attempt) {
        try {
            executor.execute(() -> runRefresh(attempt));
        } catch (RuntimeException ex) {
            completeRefresh(attempt, ex);
        }
    }

    private void runRefresh(RefreshAttempt attempt) {
        Throwable failure = null;
        try {
            backend.discoverTargets();
            if (isCurrent(attempt)) {
                synchronizeSnapshot(attempt.lifecycleGeneration());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failure = ex;
        } catch (Exception ex) {
            failure = ex;
        }
        completeRefresh(attempt, failure);
    }

    private boolean isCurrent(RefreshAttempt attempt) {
        synchronized (lock) {
            return started
                    && attempt.lifecycleGeneration() == lifecycleGeneration
                    && attempt.refreshGeneration() == activeRefreshGeneration;
        }
    }

    private void completeRefresh(RefreshAttempt attempt, Throwable failure) {
        RefreshAttempt followUp = null;
        ScheduledRefresh scheduled = null;
        boolean reportFailure = false;
        synchronized (lock) {
            if (!refreshInProgress
                    || attempt.refreshGeneration() != activeRefreshGeneration) {
                return;
            }
            boolean currentLifecycle = started
                    && attempt.lifecycleGeneration() == lifecycleGeneration;
            if (currentLifecycle) {
                if (failure == null) {
                    lastRefreshFailure = null;
                    consecutiveRefreshFailures = 0;
                } else {
                    lastRefreshFailure = failure;
                    consecutiveRefreshFailures++;
                    reportFailure = true;
                }
            }
            refreshInProgress = false;

            if (started && refreshQueued) {
                refreshQueued = false;
                scheduledRefreshGeneration++;
                followUp = beginRefreshLocked();
            } else if (currentLifecycle) {
                int delayMillis = failure == null
                        ? SUCCESS_REFRESH_DELAY_MILLIS
                        : failureDelayMillis(consecutiveRefreshFailures);
                long scheduleGeneration = ++scheduledRefreshGeneration;
                scheduled = new ScheduledRefresh(
                        lifecycleGeneration,
                        scheduleGeneration,
                        delayMillis);
            }
        }
        if (reportFailure) {
            LOGGER.log(Level.FINE, "Could not refresh Flutter run targets", failure);
        }
        if (followUp != null) {
            executeRefresh(followUp);
        } else if (scheduled != null) {
            scheduleRefresh(scheduled);
        }
    }

    private void scheduleRefresh(ScheduledRefresh scheduled) {
        retryScheduler.schedule(
                () -> runScheduledRefresh(scheduled),
                scheduled.delayMillis());
    }

    private void runScheduledRefresh(ScheduledRefresh scheduled) {
        RefreshAttempt attempt;
        synchronized (lock) {
            if (!started
                    || scheduled.lifecycleGeneration() != lifecycleGeneration
                    || scheduled.scheduleGeneration() != scheduledRefreshGeneration) {
                return;
            }
            scheduledRefreshGeneration++;
            if (refreshInProgress) {
                refreshQueued = true;
                return;
            }
            attempt = beginRefreshLocked();
        }
        executeRefresh(attempt);
    }

    private static int failureDelayMillis(int failureCount) {
        int index = Math.max(0, Math.min(
                failureCount - 1,
                FAILURE_REFRESH_DELAYS_MILLIS.length - 1));
        return FAILURE_REFRESH_DELAYS_MILLIS[index];
    }

    private static List<FlutterTargetConfiguration> configurationsOf(
            List<FlutterDevice> devices) {
        Map<String, FlutterDevice> unique = new LinkedHashMap<>();
        for (FlutterDevice device : devices) {
            if (device != null && device.id() != null && !device.id().isBlank()) {
                unique.putIfAbsent(device.id(), device);
            }
        }
        List<FlutterDevice> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator
                .comparing((FlutterDevice device) -> FlutterTargetKind.from(device).ordinal())
                .thenComparing(FlutterDevice::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FlutterDevice::id, String.CASE_INSENSITIVE_ORDER));
        return sorted.stream().map(FlutterTargetConfiguration::new).toList();
    }

    private static boolean sameSnapshot(
            List<FlutterTargetConfiguration> first,
            List<FlutterTargetConfiguration> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (!first.get(index).device().equals(second.get(index).device())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        boolean removeListener;
        synchronized (lock) {
            if (!started && !listenerAttached) {
                return;
            }
            started = false;
            lifecycleGeneration++;
            scheduledRefreshGeneration++;
            refreshQueued = false;
            refreshGeneration++;
            targetRevision++;
            removeListener = listenerAttached;
            listenerAttached = false;
        }
        if (removeListener) {
            backend.removeChangeListener(this);
        }
    }

    private record RefreshAttempt(long lifecycleGeneration, long refreshGeneration) {
    }

    private record ScheduledRefresh(
            long lifecycleGeneration,
            long scheduleGeneration,
            int delayMillis) {
    }

    interface Backend {
        List<FlutterDevice> discoverTargets() throws Exception;

        List<FlutterDevice> availableTargets();

        String selectedTargetId();

        void selectTarget(FlutterDevice device);

        void addChangeListener(ChangeListener listener);

        void removeChangeListener(ChangeListener listener);
    }

    @FunctionalInterface
    interface RetryScheduler {
        void schedule(Runnable task, int delayMillis);
    }

    private static final class ControllerBackend implements Backend {
        private final FlutterRunController controller;

        ControllerBackend(FlutterRunController controller) {
            this.controller = Objects.requireNonNull(controller, "controller");
        }

        @Override
        public List<FlutterDevice> discoverTargets() throws Exception {
            return controller.discoverTargets();
        }

        @Override
        public List<FlutterDevice> availableTargets() {
            return controller.availableTargets();
        }

        @Override
        public String selectedTargetId() {
            return controller.selectedTargetId();
        }

        @Override
        public void selectTarget(FlutterDevice device) {
            controller.selectTarget(device);
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            controller.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            controller.removeChangeListener(listener);
        }
    }
}
