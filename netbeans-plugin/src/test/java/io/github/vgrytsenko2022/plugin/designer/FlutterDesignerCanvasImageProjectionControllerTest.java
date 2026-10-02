package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.canvas.CanvasImageResourceBundle;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.designer.assets.FlutterAssetInventory;
import io.github.vgrytsenko2022.plugin.designer.assets.FlutterAssetResolver;
import io.github.vgrytsenko2022.plugin.designer.assets.FlutterDesignerCanvasImageProjector;
import io.github.vgrytsenko2022.plugin.designer.properties.FlutterImageAssetChoices;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterDesignerCanvasImageProjectionControllerTest {
    private static final String SHA_A = "a".repeat(64);
    private static final String SHA_B = "b".repeat(64);

    @TempDir
    Path temporaryDirectory;

    @Test
    void exactKeyRequestsCoalesceAndReturnTheCachedProjection() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger operations = new AtomicInteger();
        AtomicInteger publications = new AtomicInteger();
        FlutterAssetInventory inventory = inventory();
        DesignerDocument document = document();
        FlutterDesignerCanvasImageProjectionController controller = controller(
                scheduler,
                (requestedDocument, requestedInventory, requestedDpr) -> {
                    operations.incrementAndGet();
                    assertEquals(document, requestedDocument);
                    assertEquals(inventory, requestedInventory);
                    assertEquals(2.0d, requestedDpr);
                    return projection(inventory, SHA_A);
                },
                publications);

        assertTrue(controller.request(document, inventory, 2.0d).isEmpty());
        assertTrue(controller.request(document, inventory, 2.0d).isEmpty());
        assertEquals(1, scheduler.tasks.size());

        scheduler.run(0);

        assertEquals(1, operations.get());
        assertEquals(1, publications.get());
        var cached = controller.request(document, inventory, 2.0d).orElseThrow();
        assertTrue(cached.available());
        assertEquals(SHA_A,
                cached.projection().orElseThrow().fingerprintSha256());
        assertEquals(1, scheduler.tasks.size());
        controller.close();
    }

    @Test
    void supersededAndClosedTasksCannotPublishTheirBytes() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger publications = new AtomicInteger();
        FlutterAssetInventory inventory = inventory();
        DesignerDocument first = document();
        DesignerDocument second = document();
        FlutterDesignerCanvasImageProjectionController controller = controller(
                scheduler,
                (document, ignored, dpr) -> projection(
                        inventory,
                        document.equals(first) ? SHA_A : SHA_B),
                publications);

        assertTrue(controller.request(first, inventory, 1.0d).isEmpty());
        assertTrue(controller.request(second, inventory, 1.0d).isEmpty());
        assertTrue(scheduler.tasks.get(0).cancelled);

        scheduler.run(0);
        assertEquals(0, publications.get());
        scheduler.run(1);
        assertEquals(1, publications.get());
        assertEquals(SHA_B, controller.request(second, inventory, 1.0d)
                .orElseThrow().projection().orElseThrow().fingerprintSha256());

        controller.invalidate();
        assertTrue(controller.request(first, inventory, 3.0d).isEmpty());
        int pending = scheduler.tasks.size() - 1;
        controller.close();
        assertTrue(scheduler.tasks.get(pending).cancelled);
        scheduler.run(pending);
        assertEquals(1, publications.get());
        assertTrue(controller.request(first, inventory, 3.0d).isEmpty());
    }

    @Test
    void projectionFailurePublishesAConcreteUnavailableResult() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger publications = new AtomicInteger();
        FlutterAssetInventory inventory = inventory();
        DesignerDocument document = document();
        FlutterDesignerCanvasImageProjectionController controller = controller(
                scheduler,
                (ignoredDocument, ignoredInventory, ignoredDpr) -> {
                    throw new IllegalStateException("decode budget exhausted");
                },
                publications);

        assertTrue(controller.request(document, inventory, 1.0d).isEmpty());
        scheduler.run(0);

        var resolution = controller.request(document, inventory, 1.0d)
                .orElseThrow();
        assertFalse(resolution.available());
        assertTrue(resolution.detail().contains("decode budget exhausted"));
        assertTrue(resolution.detail().contains(document.documentId().toString()));
        assertEquals(1, publications.get());
        controller.close();
    }

    private FlutterDesignerCanvasImageProjectionController controller(
            ManualScheduler scheduler,
            FlutterDesignerCanvasImageProjectionController.ProjectionOperation
                    operation,
            AtomicInteger publications) {
        return new FlutterDesignerCanvasImageProjectionController(
                operation,
                scheduler,
                Runnable::run,
                publications::incrementAndGet);
    }

    private FlutterAssetInventory inventory() {
        return new FlutterAssetResolver().resolve(temporaryDirectory);
    }

    private static FlutterDesignerCanvasImageProjector.ProjectionResult projection(
            FlutterAssetInventory inventory,
            String fingerprint) {
        return new FlutterDesignerCanvasImageProjector.ProjectionResult(
                inventory.fingerprintSha256(),
                fingerprint,
                FlutterImageAssetChoices.empty(),
                CanvasImageResourceBundle.empty());
    }

    private static DesignerDocument document() {
        return new DesignerDocument(
                StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart",
                        "Sample",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(
                                new ManagedRegion("A".repeat(64)),
                                new ManagedRegion("B".repeat(64)))),
                WidgetNode.empty(
                        StableId.random(),
                        new WidgetTypeId("flutter.widgets.Container")));
    }

    private static final class ManualScheduler
            implements FlutterDesignerCanvasImageProjectionController.Scheduler {
        private final List<Scheduled> tasks = new ArrayList<>();

        @Override
        public FlutterDesignerCanvasImageProjectionController.Cancellable schedule(
                Runnable task) {
            Scheduled scheduled = new Scheduled(task);
            tasks.add(scheduled);
            return () -> scheduled.cancelled = true;
        }

        private void run(int index) {
            tasks.get(index).task.run();
        }
    }

    private static final class Scheduled {
        private final Runnable task;
        private boolean cancelled;

        private Scheduled(Runnable task) {
            this.task = task;
        }
    }
}
