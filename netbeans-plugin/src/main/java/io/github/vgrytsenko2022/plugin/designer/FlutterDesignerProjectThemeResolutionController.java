package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.canvas.CanvasThemeBrightness;
import io.github.vgrytsenko2022.designer.model.DesignerThemeMode;
import java.awt.EventQueue;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.openide.util.RequestProcessor;

/**
 * Off-EDT, coalesced project-theme verification for one open Design view.
 *
 * <p>The store reads and hashes bounded project files, but even bounded I/O is
 * forbidden on Swing's event thread. This controller owns one cached result,
 * cancels superseded work where possible, and fences every worker and UI
 * publication by a monotonically increasing generation. A non-cooperative
 * cancelled task therefore still cannot publish stale theme authority.</p>
 */
final class FlutterDesignerProjectThemeResolutionController
        implements AutoCloseable {
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterDesignerProjectThemeResolutionController.class.getName(),
            1,
            true);

    private final Path projectRoot;
    private final ResolutionOperation operation;
    private final Scheduler scheduler;
    private final Executor uiExecutor;
    private final Runnable resolutionChanged;

    private boolean closed;
    private long generation;
    private RequestKey desiredKey;
    private RequestKey cachedKey;
    private FlutterDesignerProjectThemeResolver.Resolution cachedResolution;
    private RequestKey inFlightKey;
    private Cancellable inFlight;

    FlutterDesignerProjectThemeResolutionController(
            Path projectRoot,
            FlutterDesignerProjectThemeResolver resolver,
            Runnable resolutionChanged) {
        this(
                projectRoot,
                resolver::resolve,
                task -> {
                    RequestProcessor.Task scheduled = WORKER.create(task);
                    scheduled.schedule(0);
                    return scheduled::cancel;
                },
                EventQueue::invokeLater,
                resolutionChanged);
    }

    FlutterDesignerProjectThemeResolutionController(
            Path projectRoot,
            ResolutionOperation operation,
            Scheduler scheduler,
            Executor uiExecutor,
            Runnable resolutionChanged) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize();
        this.operation = Objects.requireNonNull(operation, "operation");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        this.resolutionChanged = Objects.requireNonNull(
                resolutionChanged, "resolutionChanged");
    }

    /**
     * Returns the current cached result or starts a background verification.
     * This method performs no filesystem access and is safe to call on EDT.
     */
    Optional<FlutterDesignerProjectThemeResolver.Resolution> request(
            Optional<DesignerThemeMode> previewOverride,
            CanvasThemeBrightness systemBrightness) {
        RequestKey key = new RequestKey(previewOverride, systemBrightness);
        Pending pending;
        Cancellable superseded;
        synchronized (this) {
            if (closed) {
                return Optional.empty();
            }
            desiredKey = key;
            if (key.equals(cachedKey) && cachedResolution != null) {
                return Optional.of(cachedResolution);
            }
            if (key.equals(inFlightKey)) {
                return Optional.empty();
            }
            superseded = inFlight;
            pending = beginLocked(key);
        }
        cancel(superseded);
        schedule(pending);
        return Optional.empty();
    }

    /** Invalidates the cached project pair and reloads the last requested key. */
    void invalidate() {
        Pending pending;
        Cancellable superseded;
        synchronized (this) {
            if (closed) {
                return;
            }
            superseded = inFlight;
            cachedKey = null;
            cachedResolution = null;
            if (desiredKey == null) {
                generation++;
                inFlightKey = null;
                inFlight = null;
                pending = null;
            } else {
                pending = beginLocked(desiredKey);
            }
        }
        cancel(superseded);
        if (pending != null) {
            schedule(pending);
        }
    }

    private Pending beginLocked(RequestKey key) {
        generation++;
        cachedKey = null;
        cachedResolution = null;
        inFlightKey = key;
        inFlight = null;
        return new Pending(generation, key);
    }

    private void schedule(Pending pending) {
        final Cancellable scheduled;
        try {
            scheduled = Objects.requireNonNull(
                    scheduler.schedule(() -> resolve(pending)),
                    "scheduler returned null");
        } catch (RuntimeException failure) {
            publishAsync(pending, unavailable(
                    "Could not schedule project theme verification: "
                    + failureReason(failure) + "."));
            return;
        }
        boolean retain;
        synchronized (this) {
            retain = !closed
                    && generation == pending.generation()
                    && pending.key().equals(inFlightKey);
            if (retain) {
                inFlight = scheduled;
            }
        }
        if (!retain) {
            cancel(scheduled);
        }
    }

    private void resolve(Pending pending) {
        final FlutterDesignerProjectThemeResolver.Resolution resolution;
        try {
            resolution = Objects.requireNonNull(
                    operation.resolve(
                            projectRoot,
                            pending.key().previewOverride(),
                            pending.key().systemBrightness()),
                    "project theme resolver returned null");
        } catch (RuntimeException failure) {
            publishAsync(pending, unavailable(
                    "Project theme verification failed: "
                    + failureReason(failure) + "."));
            return;
        }
        publishAsync(pending, resolution);
    }

    private void publishAsync(
            Pending pending,
            FlutterDesignerProjectThemeResolver.Resolution resolution) {
        try {
            uiExecutor.execute(() -> publish(pending, resolution));
        } catch (RuntimeException ignored) {
            // A disposed UI dispatcher cannot acquire Canvas authority.
        }
    }

    private void publish(
            Pending pending,
            FlutterDesignerProjectThemeResolver.Resolution resolution) {
        synchronized (this) {
            if (closed
                    || generation != pending.generation()
                    || !pending.key().equals(desiredKey)
                    || !pending.key().equals(inFlightKey)) {
                return;
            }
            cachedKey = pending.key();
            cachedResolution = resolution;
            inFlightKey = null;
            inFlight = null;
        }
        resolutionChanged.run();
    }

    @Override
    public void close() {
        Cancellable cancelled;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            generation++;
            desiredKey = null;
            cachedKey = null;
            cachedResolution = null;
            inFlightKey = null;
            cancelled = inFlight;
            inFlight = null;
        }
        cancel(cancelled);
    }

    private static FlutterDesignerProjectThemeResolver.Resolution unavailable(
            String detail) {
        return FlutterDesignerProjectThemeResolver.Resolution.unavailable(detail);
    }

    private static void cancel(Cancellable task) {
        if (task != null) {
            task.cancel();
        }
    }

    private static String failureReason(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        String compact = message.trim().replaceAll("\\s+", " ");
        return compact.length() <= 400
                ? compact
                : compact.substring(0, 400) + "…";
    }

    @FunctionalInterface
    interface ResolutionOperation {
        FlutterDesignerProjectThemeResolver.Resolution resolve(
                Path projectRoot,
                Optional<DesignerThemeMode> previewOverride,
                CanvasThemeBrightness systemBrightness);
    }

    @FunctionalInterface
    interface Scheduler {
        Cancellable schedule(Runnable task);
    }

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }

    private record RequestKey(
            Optional<DesignerThemeMode> previewOverride,
            CanvasThemeBrightness systemBrightness) {
        private RequestKey {
            previewOverride = Objects.requireNonNull(
                    previewOverride, "previewOverride");
            Objects.requireNonNull(systemBrightness, "systemBrightness");
        }
    }

    private record Pending(long generation, RequestKey key) {
        private Pending {
            if (generation <= 0) {
                throw new IllegalArgumentException("generation must be positive");
            }
            Objects.requireNonNull(key, "key");
        }
    }
}
