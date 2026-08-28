package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class FlutterDesignerPaletteDragLifecycleTest {
    private static final WidgetTypeId TEXT = new WidgetTypeId("flutter.widgets.Text");

    @Test
    void canceledAndFailedDragsRevokeTheExactTokenImmediately() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.lifecycle.install());
        String canceled = fixture.registry.issue(TEXT).orElseThrow();

        fixture.lifecycle.dragEnded(
                new StringSelection(canceled), DnDConstants.ACTION_NONE, false);

        assertTrue(fixture.registry.consume(canceled).isEmpty());
        assertEquals(0, fixture.scheduler.pendingCount());

        String failed = fixture.registry.issue(TEXT).orElseThrow();
        fixture.lifecycle.dragEnded(
                new StringSelection(failed), DnDConstants.ACTION_MOVE, false);
        assertTrue(fixture.registry.consume(failed).isEmpty());

        String copied = fixture.registry.issue(TEXT).orElseThrow();
        fixture.lifecycle.dragEnded(
                new StringSelection(copied), DnDConstants.ACTION_COPY, true);
        assertTrue(fixture.registry.consume(copied).isEmpty(),
                "only a successful native MOVE receives the grace interval");
        fixture.lifecycle.close();
    }

    @Test
    void foreignAndUnrelatedStringsNeverAffectLocalOrForeignAuthority() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.lifecycle.install());
        String local = fixture.registry.issue(TEXT).orElseThrow();
        FlutterDesignerPaletteDragRegistry foreignRegistry = registry(
                new UUID(0, 999), new AtomicLong(2_000));
        String foreign = foreignRegistry.issue(TEXT).orElseThrow();

        fixture.lifecycle.dragEnded(
                new StringSelection(foreign), DnDConstants.ACTION_NONE, false);
        fixture.lifecycle.dragEnded(
                new StringSelection("unrelated clipboard text"),
                DnDConstants.ACTION_NONE,
                false);
        fixture.lifecycle.dragEnded(
                new StringSelection("x".repeat(4_096)),
                DnDConstants.ACTION_NONE,
                false);

        assertEquals(TEXT, fixture.registry.consume(local).orElseThrow());
        assertEquals(TEXT, foreignRegistry.consume(foreign).orElseThrow());
        fixture.lifecycle.close();
    }

    @Test
    void successfulMoveIsConsumableExactlyOnceDuringGrace() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.lifecycle.install());
        String token = fixture.registry.issue(TEXT).orElseThrow();

        fixture.lifecycle.dragEnded(
                new StringSelection(token), DnDConstants.ACTION_MOVE, true);

        assertEquals(1, fixture.scheduler.pendingCount());
        assertEquals(TEXT, fixture.lifecycle.consume(token).orElseThrow());
        assertEquals(0, fixture.scheduler.pendingCount());
        assertTrue(fixture.lifecycle.consume(token).isEmpty());
        fixture.scheduler.runPending();
        assertTrue(fixture.registry.consume(token).isEmpty());
        fixture.lifecycle.close();
    }

    @Test
    void successfulMoveExpiresAtTheBoundedGraceDeadline() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.lifecycle.install());
        String token = fixture.registry.issue(TEXT).orElseThrow();

        fixture.lifecycle.dragEnded(
                new StringSelection(token), DnDConstants.ACTION_MOVE, true);
        fixture.scheduler.runPending();

        assertTrue(fixture.registry.consume(token).isEmpty());
        assertEquals(0, fixture.registry.outstandingCount());
        fixture.lifecycle.close();
    }

    @Test
    void atLeastSixtyFiveCanceledCyclesCannotExhaustTheRegistry() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.lifecycle.install());

        for (int cycle = 0; cycle < 65; cycle++) {
            int iteration = cycle;
            String token = fixture.registry.issue(TEXT).orElseThrow(
                    () -> new AssertionError(
                            "cycle " + iteration + " exhausted capacity"));
            fixture.lifecycle.dragEnded(
                    new StringSelection(token), DnDConstants.ACTION_NONE, false);
            assertEquals(0, fixture.registry.outstandingCount());
        }

        fixture.lifecycle.close();
    }

    @Test
    void listenerInstallUninstallAndCloseAreIdempotentAndLeakFree() {
        Fixture fixture = fixture(true);
        assertTrue(fixture.lifecycle.install());
        assertTrue(fixture.lifecycle.install());
        assertEquals(1, fixture.listenerAccess.installCount);

        String token = fixture.registry.issue(TEXT).orElseThrow();
        fixture.lifecycle.dragEnded(
                new StringSelection(token), DnDConstants.ACTION_MOVE, true);
        fixture.lifecycle.uninstall();
        fixture.lifecycle.uninstall();

        assertEquals(1, fixture.listenerAccess.uninstallCount);
        assertFalse(fixture.lifecycle.isInstalled());
        assertTrue(fixture.registry.consume(token).isEmpty());
        assertEquals(0, fixture.scheduler.pendingCount());

        assertTrue(fixture.lifecycle.install(), "an uninstalled open view may reinstall");
        assertEquals(2, fixture.listenerAccess.installCount);
        fixture.lifecycle.close();
        fixture.lifecycle.close();
        assertEquals(2, fixture.listenerAccess.uninstallCount);
        assertTrue(fixture.scheduler.closed);
        assertFalse(fixture.lifecycle.install(), "a closed view cannot leak a new listener");
    }

    @Test
    void unavailableHeadlessAccessFailsClosedWithoutInstallingAListener() {
        Fixture fixture = fixture(false);
        String token = fixture.registry.issue(TEXT).orElseThrow();

        assertFalse(fixture.lifecycle.install());

        assertFalse(fixture.lifecycle.isInstalled());
        assertEquals(0, fixture.listenerAccess.installCount);
        assertTrue(fixture.registry.consume(token).isEmpty());
        fixture.lifecycle.close();
        assertEquals(0, fixture.listenerAccess.uninstallCount);
    }

    @Test
    void awtAccessDoesNotResolveTheGlobalDragSourceWhenHeadless() {
        AtomicBoolean dragSourceResolved = new AtomicBoolean();
        FlutterDesignerPaletteDragLifecycle.AwtGlobalListenerAccess access =
                new FlutterDesignerPaletteDragLifecycle.AwtGlobalListenerAccess(
                        () -> true,
                        () -> {
                            dragSourceResolved.set(true);
                            throw new AssertionError(
                                    "headless install must not resolve DragSource");
                        });

        boolean installed = access.install(new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent event) {
            }
        });

        assertFalse(installed);
        assertFalse(dragSourceResolved.get());
    }

    private static Fixture fixture(boolean listenerAvailable) {
        FlutterDesignerPaletteDragRegistry registry = registry(
                new UUID(0, 1), new AtomicLong(1_000));
        FakeGlobalListenerAccess access = new FakeGlobalListenerAccess(
                listenerAvailable);
        ManualGraceScheduler scheduler = new ManualGraceScheduler();
        FlutterDesignerPaletteDragLifecycle lifecycle =
                new FlutterDesignerPaletteDragLifecycle(
                        registry, Duration.ofSeconds(3), access, scheduler);
        return new Fixture(registry, access, scheduler, lifecycle);
    }

    private static FlutterDesignerPaletteDragRegistry registry(
            UUID context,
            AtomicLong dragIds) {
        return new FlutterDesignerPaletteDragRegistry(
                context,
                Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC),
                Duration.ofSeconds(30),
                64,
                () -> new UUID(0, dragIds.getAndIncrement()));
    }

    private record Fixture(
            FlutterDesignerPaletteDragRegistry registry,
            FakeGlobalListenerAccess listenerAccess,
            ManualGraceScheduler scheduler,
            FlutterDesignerPaletteDragLifecycle lifecycle) {
    }

    private static final class FakeGlobalListenerAccess
            implements FlutterDesignerPaletteDragLifecycle.GlobalListenerAccess {
        private final boolean available;
        private DragSourceListener installed;
        private int installCount;
        private int uninstallCount;

        private FakeGlobalListenerAccess(boolean available) {
            this.available = available;
        }

        @Override
        public boolean install(DragSourceListener listener) {
            if (!available) {
                return false;
            }
            assertTrue(installed == null, "listener already installed");
            installed = listener;
            installCount++;
            return true;
        }

        @Override
        public void uninstall(DragSourceListener listener) {
            assertTrue(installed == listener, "only the owning listener may uninstall");
            installed = null;
            uninstallCount++;
        }
    }

    private static final class ManualGraceScheduler
            implements FlutterDesignerPaletteDragLifecycle.GraceScheduler {
        private final List<PendingTask> tasks = new ArrayList<>();
        private boolean closed;

        @Override
        public FlutterDesignerPaletteDragLifecycle.Cancellable schedule(
                Runnable task,
                Duration delay) {
            assertFalse(closed, "closed scheduler");
            assertEquals(Duration.ofSeconds(3), delay);
            PendingTask pending = new PendingTask(task);
            tasks.add(pending);
            return () -> pending.canceled = true;
        }

        private int pendingCount() {
            return (int) tasks.stream().filter(task -> !task.canceled).count();
        }

        private void runPending() {
            List.copyOf(tasks).forEach(task -> {
                if (!task.canceled) {
                    task.canceled = true;
                    task.runnable.run();
                }
            });
        }

        @Override
        public void close() {
            closed = true;
            tasks.forEach(task -> task.canceled = true);
        }
    }

    private static final class PendingTask {
        private final Runnable runnable;
        private boolean canceled;

        private PendingTask(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
