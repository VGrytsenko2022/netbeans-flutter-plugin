package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.plugin.designer.assets.FlutterAssetResolver;
import java.awt.EventQueue;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FlutterDesignerProjectAssetResolutionControllerTest {
    private static final Path PROJECT = Path.of("project").toAbsolutePath();

    @Test
    void delayedInventoryNeitherRunsOnNorBlocksTheEventDispatchThread()
            throws Exception {
        CountDownLatch operationStarted = new CountDownLatch(1);
        CountDownLatch releaseOperation = new CountDownLatch(1);
        CountDownLatch requestReturned = new CountDownLatch(1);
        CountDownLatch published = new CountDownLatch(1);
        AtomicBoolean operationRanOnEdt = new AtomicBoolean(true);
        AtomicReference<Boolean> immediateEmpty = new AtomicReference<>();
        FlutterDesignerProjectAssetResolutionController.Scheduler scheduler = task -> {
            Thread worker = new Thread(task, "project-assets-test-worker");
            worker.setDaemon(true);
            worker.start();
            return worker::interrupt;
        };
        FlutterDesignerProjectAssetResolutionController controller =
                new FlutterDesignerProjectAssetResolutionController(
                        PROJECT,
                        root -> {
                            operationRanOnEdt.set(EventQueue.isDispatchThread());
                            operationStarted.countDown();
                            try {
                                assertTrue(releaseOperation.await(
                                        5, TimeUnit.SECONDS));
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                            return new FlutterAssetResolver().resolve(root);
                        },
                        scheduler,
                        EventQueue::invokeLater,
                        published::countDown);

        EventQueue.invokeLater(() -> {
            immediateEmpty.set(controller.request().isEmpty());
            requestReturned.countDown();
        });
        boolean returnedQuickly;
        try {
            returnedQuickly = requestReturned.await(
                    Duration.ofMillis(500).toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            releaseOperation.countDown();
        }

        assertTrue(returnedQuickly, "EDT request waited for asset I/O");
        assertTrue(operationStarted.await(5, TimeUnit.SECONDS));
        assertFalse(operationRanOnEdt.get());
        assertTrue(immediateEmpty.get());
        assertTrue(published.await(5, TimeUnit.SECONDS));
        EventQueue.invokeAndWait(() -> assertTrue(
                controller.request().orElseThrow().available()));
        controller.close();
    }

    @Test
    void duplicateRequestsCoalesceAndSupersededWorkersCannotPublish() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger publications = new AtomicInteger();
        FlutterDesignerProjectAssetResolutionController controller =
                new FlutterDesignerProjectAssetResolutionController(
                        PROJECT,
                        root -> new FlutterAssetResolver().resolve(root),
                        scheduler,
                        Runnable::run,
                        publications::incrementAndGet);

        assertTrue(controller.request().isEmpty());
        assertTrue(controller.request().isEmpty());
        assertEquals(1, scheduler.tasks.size());

        controller.invalidate();
        assertTrue(scheduler.tasks.get(0).cancelled);
        assertEquals(2, scheduler.tasks.size());
        scheduler.run(0);
        assertEquals(0, publications.get(), "stale inventory was published");
        scheduler.run(1);
        assertEquals(1, publications.get());
        assertTrue(controller.request().orElseThrow().available());

        controller.invalidate();
        int pending = scheduler.tasks.size() - 1;
        controller.close();
        scheduler.run(pending);
        assertEquals(1, publications.get(), "closed controller published bytes");
    }

    @Test
    void resolverFailurePublishesConcreteUnavailableResult() {
        ManualScheduler scheduler = new ManualScheduler();
        FlutterDesignerProjectAssetResolutionController controller =
                new FlutterDesignerProjectAssetResolutionController(
                        PROJECT,
                        root -> {
                            throw new IllegalStateException("pubspec read failed");
                        },
                        scheduler,
                        Runnable::run,
                        () -> { });

        assertTrue(controller.request().isEmpty());
        scheduler.run(0);

        var result = controller.request().orElseThrow();
        assertFalse(result.available());
        assertTrue(result.detail().contains("pubspec read failed"));
        controller.close();
    }

    private static final class ManualScheduler
            implements FlutterDesignerProjectAssetResolutionController.Scheduler {
        private final List<Scheduled> tasks = new ArrayList<>();

        @Override
        public FlutterDesignerProjectAssetResolutionController.Cancellable schedule(
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
