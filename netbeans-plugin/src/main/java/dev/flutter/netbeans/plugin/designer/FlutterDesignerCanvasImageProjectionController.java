package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.plugin.designer.assets.FlutterAssetInventory;
import dev.flutter.netbeans.plugin.designer.assets.FlutterDesignerCanvasImageProjector;
import java.awt.EventQueue;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.openide.util.RequestProcessor;

/**
 * Off-EDT, generation-fenced image-byte projection for one Canvas view.
 *
 * <p>The inventory already owns bounded immutable bytes, but constructing the
 * strict presentation bundle clones, hashes, deduplicates and indexes them.
 * Keeping that bounded CPU and memory work off Swing avoids a large declared
 * image stalling the NetBeans UI.</p>
 */
final class FlutterDesignerCanvasImageProjectionController
        implements AutoCloseable {
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterDesignerCanvasImageProjectionController.class.getName(),
            1,
            true);

    private final ProjectionOperation operation;
    private final Scheduler scheduler;
    private final Executor uiExecutor;
    private final Runnable projectionChanged;

    private boolean closed;
    private long generation;
    private RequestKey desiredKey;
    private RequestKey cachedKey;
    private Resolution cached;
    private RequestKey inFlightKey;
    private Cancellable inFlight;

    FlutterDesignerCanvasImageProjectionController(
            FlutterDesignerCanvasImageProjector projector,
            Runnable projectionChanged) {
        this(
                projector::project,
                task -> {
                    RequestProcessor.Task scheduled = WORKER.create(task);
                    scheduled.schedule(0);
                    return scheduled::cancel;
                },
                EventQueue::invokeLater,
                projectionChanged);
    }

    FlutterDesignerCanvasImageProjectionController(
            ProjectionOperation operation,
            Scheduler scheduler,
            Executor uiExecutor,
            Runnable projectionChanged) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        this.projectionChanged = Objects.requireNonNull(
                projectionChanged, "projectionChanged");
    }

    /** Returns a cached exact-key projection or starts it without blocking EDT. */
    Optional<Resolution> request(
            DesignerDocument document,
            FlutterAssetInventory inventory,
            double devicePixelRatio) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(inventory, "inventory");
        if (!Double.isFinite(devicePixelRatio) || devicePixelRatio <= 0.0d) {
            throw new IllegalArgumentException(
                    "devicePixelRatio must be finite and positive");
        }
        RequestKey key = new RequestKey(
                document,
                inventory.fingerprintSha256(),
                Double.doubleToLongBits(devicePixelRatio));
        Pending pending;
        Cancellable superseded;
        synchronized (this) {
            if (closed) {
                return Optional.empty();
            }
            desiredKey = key;
            if (key.equals(cachedKey) && cached != null) {
                return Optional.of(cached);
            }
            if (key.equals(inFlightKey)) {
                return Optional.empty();
            }
            superseded = inFlight;
            pending = beginLocked(key, inventory, devicePixelRatio);
        }
        cancel(superseded);
        schedule(pending);
        return Optional.empty();
    }

    /** Drops all projected byte snapshots and cancels any older key. */
    void invalidate() {
        Cancellable cancelled;
        synchronized (this) {
            if (closed) {
                return;
            }
            generation++;
            desiredKey = null;
            cachedKey = null;
            cached = null;
            inFlightKey = null;
            cancelled = inFlight;
            inFlight = null;
        }
        cancel(cancelled);
    }

    private Pending beginLocked(
            RequestKey key,
            FlutterAssetInventory inventory,
            double devicePixelRatio) {
        generation++;
        cachedKey = null;
        cached = null;
        inFlightKey = key;
        inFlight = null;
        return new Pending(generation, key, inventory, devicePixelRatio);
    }

    private void schedule(Pending pending) {
        final Cancellable scheduled;
        try {
            scheduled = Objects.requireNonNull(
                    scheduler.schedule(() -> project(pending)),
                    "scheduler returned null");
        } catch (RuntimeException failure) {
            publishAsync(pending, Resolution.unavailable(
                    "Could not schedule Canvas image projection: "
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

    private void project(Pending pending) {
        Resolution resolution;
        try {
            resolution = Resolution.available(Objects.requireNonNull(
                    operation.project(
                            pending.key().document(),
                            pending.inventory(),
                            pending.devicePixelRatio()),
                    "Canvas image projector returned null"));
        } catch (RuntimeException failure) {
            resolution = Resolution.unavailable(
                    "Project Canvas image bytes for document "
                    + pending.key().document().documentId() + " failed: "
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
                    || !pending.key().equals(desiredKey)
                    || !pending.key().equals(inFlightKey)) {
                return;
            }
            cachedKey = pending.key();
            cached = Objects.requireNonNull(resolution, "resolution");
            inFlightKey = null;
            inFlight = null;
        }
        projectionChanged.run();
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
            cached = null;
            inFlightKey = null;
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
            Optional<FlutterDesignerCanvasImageProjector.ProjectionResult>
                    projection,
            String detail) {
        Resolution {
            Objects.requireNonNull(projection, "projection");
            Objects.requireNonNull(detail, "detail");
            detail = detail.strip().replaceAll("\\s+", " ");
            if (detail.isEmpty() || detail.length() > 1024) {
                throw new IllegalArgumentException(
                        "Canvas image projection detail must contain 1..1024 characters");
            }
        }

        static Resolution available(
                FlutterDesignerCanvasImageProjector.ProjectionResult result) {
            return new Resolution(
                    Optional.of(Objects.requireNonNull(result, "result")),
                    "Projected " + result.bundle().assets().size()
                    + " resolved and " + result.bundle().issues().size()
                    + " unavailable Canvas image asset(s).");
        }

        static Resolution unavailable(String detail) {
            return new Resolution(Optional.empty(), detail);
        }

        boolean available() {
            return projection.isPresent();
        }
    }

    @FunctionalInterface
    interface ProjectionOperation {
        FlutterDesignerCanvasImageProjector.ProjectionResult project(
                DesignerDocument document,
                FlutterAssetInventory inventory,
                double devicePixelRatio);
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
            DesignerDocument document,
            String inventoryFingerprintSha256,
            long devicePixelRatioBits) {
        private RequestKey {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(
                    inventoryFingerprintSha256,
                    "inventoryFingerprintSha256");
        }
    }

    private record Pending(
            long generation,
            RequestKey key,
            FlutterAssetInventory inventory,
            double devicePixelRatio) {
        private Pending {
            if (generation <= 0) {
                throw new IllegalArgumentException("generation must be positive");
            }
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(inventory, "inventory");
        }
    }
}
