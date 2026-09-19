package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.plugin.designer.assets.FlutterAssetInventory;
import io.github.vgrytsenko2022.plugin.designer.assets.FlutterAssetResolver;
import java.awt.EventQueue;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.openide.util.RequestProcessor;

/**
 * Off-EDT, generation-fenced inventory of one Flutter project's declared images.
 *
 * <p>The resolver performs bounded filesystem reads. This controller keeps
 * those reads away from Swing, coalesces duplicate requests, and prevents a
 * cancelled or superseded worker from publishing stale bytes into a newer
 * Canvas presentation.</p>
 */
final class FlutterDesignerProjectAssetResolutionController
        implements AutoCloseable {
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterDesignerProjectAssetResolutionController.class.getName(),
            1,
            true);

    private final Path projectRoot;
    private final ResolutionOperation operation;
    private final Scheduler scheduler;
    private final Executor uiExecutor;
    private final Runnable resolutionChanged;

    private boolean closed;
    private boolean requested;
    private long generation;
    private Resolution cached;
    private long inFlightGeneration;
    private Cancellable inFlight;

    FlutterDesignerProjectAssetResolutionController(
            Path projectRoot,
            FlutterAssetResolver resolver,
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

    FlutterDesignerProjectAssetResolutionController(
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

    /** Returns cached authority or starts a bounded worker without doing I/O. */
    Optional<Resolution> request() {
        Pending pending;
        synchronized (this) {
            if (closed) {
                return Optional.empty();
            }
            requested = true;
            if (cached != null) {
                return Optional.of(cached);
            }
            if (inFlightGeneration != 0) {
                return Optional.empty();
            }
            pending = beginLocked();
        }
        schedule(pending);
        return Optional.empty();
    }

    /** Invalidates cached bytes and reloads when this view already requested them. */
    void invalidate() {
        Pending pending;
        Cancellable superseded;
        synchronized (this) {
            if (closed) {
                return;
            }
            superseded = inFlight;
            cached = null;
            pending = requested ? beginLocked() : null;
            if (pending == null) {
                generation++;
                inFlightGeneration = 0;
                inFlight = null;
            }
        }
        cancel(superseded);
        if (pending != null) {
            schedule(pending);
        }
    }

    private Pending beginLocked() {
        generation++;
        cached = null;
        inFlightGeneration = generation;
        inFlight = null;
        return new Pending(generation);
    }

    private void schedule(Pending pending) {
        final Cancellable scheduled;
        try {
            scheduled = Objects.requireNonNull(
                    scheduler.schedule(() -> resolve(pending)),
                    "scheduler returned null");
        } catch (RuntimeException failure) {
            publishAsync(pending, Resolution.unavailable(
                    "Could not schedule declared Flutter image inventory: "
                    + failureReason(failure) + "."));
            return;
        }
        boolean retain;
        synchronized (this) {
            retain = !closed
                    && generation == pending.generation()
                    && inFlightGeneration == pending.generation();
            if (retain) {
                inFlight = scheduled;
            }
        }
        if (!retain) {
            cancel(scheduled);
        }
    }

    private void resolve(Pending pending) {
        Resolution resolution;
        try {
            FlutterAssetInventory inventory = Objects.requireNonNull(
                    operation.resolve(projectRoot),
                    "Flutter asset resolver returned null");
            resolution = Resolution.available(inventory);
        } catch (RuntimeException failure) {
            resolution = Resolution.unavailable(
                    "Declared Flutter image inventory failed: "
                    + failureReason(failure) + ".");
        }
        publishAsync(pending, resolution);
    }

    private void publishAsync(Pending pending, Resolution resolution) {
        try {
            uiExecutor.execute(() -> publish(pending, resolution));
        } catch (RuntimeException ignored) {
            // A disposed UI dispatcher cannot acquire Canvas authority.
        }
    }

    private void publish(Pending pending, Resolution resolution) {
        synchronized (this) {
            if (closed
                    || generation != pending.generation()
                    || inFlightGeneration != pending.generation()) {
                return;
            }
            cached = Objects.requireNonNull(resolution, "resolution");
            inFlightGeneration = 0;
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
            requested = false;
            generation++;
            cached = null;
            inFlightGeneration = 0;
            cancelled = inFlight;
            inFlight = null;
        }
        cancel(cancelled);
    }

    private static void cancel(Cancellable task) {
        if (task != null) {
            task.cancel();
        }
    }

    private static String failureReason(Throwable failure) {
        String message = failure.getMessage();
        String compact = message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.strip().replaceAll("\\s+", " ");
        return compact.length() <= 400
                ? compact
                : compact.substring(0, 400) + "…";
    }

    record Resolution(
            Optional<FlutterAssetInventory> inventory,
            String detail) {
        Resolution {
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(detail, "detail");
            detail = detail.strip().replaceAll("\\s+", " ");
            if (detail.isEmpty() || detail.length() > 1024) {
                throw new IllegalArgumentException(
                        "Asset inventory detail must contain 1..1024 characters");
            }
        }

        static Resolution available(FlutterAssetInventory inventory) {
            return new Resolution(
                    Optional.of(Objects.requireNonNull(inventory, "inventory")),
                    inventory.assets().isEmpty()
                            ? "No supported image assets are declared by the Flutter project."
                            : "Resolved " + inventory.assets().size()
                            + " declared Flutter image asset"
                            + (inventory.assets().size() == 1 ? "." : "s."));
        }

        static Resolution unavailable(String detail) {
            return new Resolution(Optional.empty(), detail);
        }

        boolean available() {
            return inventory.isPresent();
        }
    }

    @FunctionalInterface
    interface ResolutionOperation {
        FlutterAssetInventory resolve(Path projectRoot);
    }

    @FunctionalInterface
    interface Scheduler {
        Cancellable schedule(Runnable task);
    }

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }

    private record Pending(long generation) {
        private Pending {
            if (generation <= 0) {
                throw new IllegalArgumentException("generation must be positive");
            }
        }
    }
}
