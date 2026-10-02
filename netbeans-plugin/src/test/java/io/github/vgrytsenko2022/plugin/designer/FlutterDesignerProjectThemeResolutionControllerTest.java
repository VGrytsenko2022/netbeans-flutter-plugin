package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.canvas.CanvasResolvedTheme;
import io.github.vgrytsenko2022.designer.canvas.CanvasThemeBrightness;
import io.github.vgrytsenko2022.designer.model.DesignerThemeMode;
import java.awt.EventQueue;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FlutterDesignerProjectThemeResolutionControllerTest {
    private static final Path PROJECT = Path.of("project").toAbsolutePath();

    @Test
    void delayedVerificationNeitherRunsOnNorBlocksTheEventDispatchThread()
            throws Exception {
        CountDownLatch operationStarted = new CountDownLatch(1);
        CountDownLatch releaseOperation = new CountDownLatch(1);
        CountDownLatch requestReturned = new CountDownLatch(1);
        CountDownLatch published = new CountDownLatch(1);
        AtomicBoolean operationRanOnEdt = new AtomicBoolean(true);
        AtomicReference<Optional<FlutterDesignerProjectThemeResolver.Resolution>>
                immediate = new AtomicReference<>();
        FlutterDesignerProjectThemeResolutionController.Scheduler scheduler = task -> {
            Thread worker = new Thread(task, "project-theme-test-worker");
            worker.setDaemon(true);
            worker.start();
            return worker::interrupt;
        };
        FlutterDesignerProjectThemeResolutionController controller =
                new FlutterDesignerProjectThemeResolutionController(
                        PROJECT,
                        (root, override, systemBrightness) -> {
                            operationRanOnEdt.set(EventQueue.isDispatchThread());
                            operationStarted.countDown();
                            try {
                                assertTrue(releaseOperation.await(5, TimeUnit.SECONDS));
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                            return available("A".repeat(64));
                        },
                        scheduler,
                        EventQueue::invokeLater,
                        published::countDown);

        EventQueue.invokeLater(() -> {
            immediate.set(controller.request(
                    Optional.of(DesignerThemeMode.LIGHT),
                    CanvasThemeBrightness.LIGHT));
            requestReturned.countDown();
        });
        boolean returnedQuickly;
        try {
            returnedQuickly = requestReturned.await(
                    Duration.ofMillis(500).toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            releaseOperation.countDown();
        }

        assertTrue(returnedQuickly, "EDT request waited for project-theme I/O");
        assertTrue(operationStarted.await(5, TimeUnit.SECONDS));
        assertFalse(operationRanOnEdt.get());
        assertTrue(immediate.get().isEmpty());
        assertTrue(published.await(5, TimeUnit.SECONDS));
        EventQueue.invokeAndWait(() -> assertTrue(controller.request(
                Optional.of(DesignerThemeMode.LIGHT),
                CanvasThemeBrightness.LIGHT).orElseThrow().available()));
        controller.close();
    }

    @Test
    void supersededAndClosedWorkersCannotPublishStaleAuthority() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger publications = new AtomicInteger();
        FlutterDesignerProjectThemeResolutionController controller =
                new FlutterDesignerProjectThemeResolutionController(
                        PROJECT,
                        (root, override, systemBrightness) -> available(
                                override.orElseThrow() == DesignerThemeMode.DARK
                                        ? "D".repeat(64)
                                        : "A".repeat(64)),
                        scheduler,
                        Runnable::run,
                        publications::incrementAndGet);

        assertTrue(controller.request(
                Optional.of(DesignerThemeMode.LIGHT),
                CanvasThemeBrightness.LIGHT).isEmpty());
        assertTrue(controller.request(
                Optional.of(DesignerThemeMode.DARK),
                CanvasThemeBrightness.LIGHT).isEmpty());
        assertTrue(scheduler.tasks.get(0).cancelled);

        scheduler.run(1);
        assertEquals(1, publications.get());
        assertEquals("D".repeat(64), controller.request(
                Optional.of(DesignerThemeMode.DARK),
                CanvasThemeBrightness.LIGHT).orElseThrow()
                .theme().orElseThrow().digestIdentity());

        scheduler.run(0);
        assertEquals(1, publications.get(), "stale generation was published");
        assertEquals("D".repeat(64), controller.request(
                Optional.of(DesignerThemeMode.DARK),
                CanvasThemeBrightness.LIGHT).orElseThrow()
                .theme().orElseThrow().digestIdentity());

        controller.invalidate();
        int queuedBeforeClose = scheduler.tasks.size();
        controller.close();
        scheduler.run(queuedBeforeClose - 1);
        assertEquals(1, publications.get(), "closed controller published a result");
    }

    @Test
    void validInvalidAndRestoredThemeSequenceIsRepublishedExactly() {
        ManualScheduler scheduler = new ManualScheduler();
        AtomicInteger publications = new AtomicInteger();
        AtomicReference<FlutterDesignerProjectThemeResolver.Resolution> source =
                new AtomicReference<>(available("A".repeat(64)));
        FlutterDesignerProjectThemeResolutionController controller =
                new FlutterDesignerProjectThemeResolutionController(
                        PROJECT,
                        (root, override, systemBrightness) -> source.get(),
                        scheduler,
                        Runnable::run,
                        publications::incrementAndGet);

        Optional<DesignerThemeMode> override = Optional.of(DesignerThemeMode.LIGHT);
        assertTrue(controller.request(
                override, CanvasThemeBrightness.LIGHT).isEmpty());
        scheduler.run(0);
        assertTrue(controller.request(
                override, CanvasThemeBrightness.LIGHT).orElseThrow().available());

        source.set(FlutterDesignerProjectThemeResolver.Resolution.unavailable(
                "Generated Dart hash mismatch."));
        controller.invalidate();
        assertTrue(controller.request(
                override, CanvasThemeBrightness.LIGHT).isEmpty());
        scheduler.run(1);
        var invalid = controller.request(
                override, CanvasThemeBrightness.LIGHT).orElseThrow();
        assertFalse(invalid.available());
        assertEquals("Generated Dart hash mismatch.", invalid.detail());

        source.set(available("B".repeat(64)));
        controller.invalidate();
        scheduler.run(2);
        var restored = controller.request(
                override, CanvasThemeBrightness.LIGHT).orElseThrow();
        assertTrue(restored.available());
        assertEquals("B".repeat(64),
                restored.theme().orElseThrow().digestIdentity());
        assertEquals(3, publications.get());
        controller.close();
    }

    private static FlutterDesignerProjectThemeResolver.Resolution available(
            String digest) {
        return FlutterDesignerProjectThemeResolver.Resolution.available(
                new CanvasResolvedTheme(
                        "light",
                        0xFF6750A4,
                        CanvasThemeBrightness.LIGHT,
                        digest),
                false,
                "Verified project theme.");
    }

    private static final class ManualScheduler
            implements FlutterDesignerProjectThemeResolutionController.Scheduler {
        private final List<Scheduled> tasks = new ArrayList<>();

        @Override
        public FlutterDesignerProjectThemeResolutionController.Cancellable schedule(
                Runnable task) {
            Scheduled scheduled = new Scheduled(task);
            tasks.add(scheduled);
            return () -> scheduled.cancelled = true;
        }

        private void run(int index) {
            // Deliberately execute cancelled work to prove generation fencing.
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
