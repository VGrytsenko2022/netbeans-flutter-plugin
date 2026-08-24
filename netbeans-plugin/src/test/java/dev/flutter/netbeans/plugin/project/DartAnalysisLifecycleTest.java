package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DartAnalysisLifecycleTest {
    @Test
    void rejectsStartupThatCrossedCloseAndReopen() {
        DartAnalysisLifecycle lifecycle = synchronousLifecycle();
        AtomicBoolean closed = new AtomicBoolean();

        lifecycle.open();
        DartAnalysisLifecycle.Ticket stale = lifecycle.acquire();
        assertNotNull(stale);
        lifecycle.close();
        lifecycle.open();

        assertFalse(lifecycle.attach(stale, () -> closed.set(true), null));
        assertTrue(closed.get());
    }

    @Test
    void replacesServerAndClosesItWithProject() {
        DartAnalysisLifecycle lifecycle = synchronousLifecycle();
        AtomicBoolean firstClosed = new AtomicBoolean();
        AtomicBoolean secondClosed = new AtomicBoolean();
        AtomicBoolean cacheRemoved = new AtomicBoolean();

        lifecycle.open();
        DartAnalysisLifecycle.Ticket ticket = lifecycle.acquire();
        assertNotNull(ticket);
        assertTrue(lifecycle.attach(ticket, () -> firstClosed.set(true), null));
        assertTrue(lifecycle.attach(ticket, () -> secondClosed.set(true),
                () -> cacheRemoved.set(true)));
        assertTrue(firstClosed.get());
        assertFalse(secondClosed.get());

        lifecycle.close();

        assertTrue(cacheRemoved.get());
        assertTrue(secondClosed.get());
        assertFalse(lifecycle.isOpen());
    }

    @Test
    void reopenAcceptsAReplacementGeneration() {
        DartAnalysisLifecycle lifecycle = synchronousLifecycle();
        AtomicBoolean reopenedClosed = new AtomicBoolean();

        lifecycle.open();
        lifecycle.close();
        lifecycle.open();

        DartAnalysisLifecycle.Ticket ticket = lifecycle.acquire();
        assertNotNull(ticket);
        assertTrue(lifecycle.attach(ticket, () -> reopenedClosed.set(true), null));
        assertFalse(reopenedClosed.get());
    }

    @Test
    void waitsForOldBindingCleanupBeforeIssuingNewTicket() {
        AtomicReference<Runnable> pendingCleanup = new AtomicReference<>();
        AtomicBoolean cacheRemoved = new AtomicBoolean();
        AtomicBoolean serverClosed = new AtomicBoolean();
        DartAnalysisLifecycle lifecycle = new DartAnalysisLifecycle(
                (owner, restarter, completion) -> pendingCleanup.set(() -> {
                    try {
                        if (restarter != null) {
                            restarter.run();
                        }
                        owner.close();
                    } catch (Exception ex) {
                        throw new AssertionError(ex);
                    } finally {
                        completion.run();
                    }
                }));

        lifecycle.open();
        DartAnalysisLifecycle.Ticket ticket = lifecycle.acquire();
        assertNotNull(ticket);
        assertTrue(lifecycle.attach(
                ticket,
                () -> serverClosed.set(true),
                () -> cacheRemoved.set(true)));
        lifecycle.close();
        lifecycle.open();

        assertNull(lifecycle.acquire());
        assertNotNull(pendingCleanup.get());
        pendingCleanup.get().run();

        assertTrue(cacheRemoved.get());
        assertTrue(serverClosed.get());
        assertNotNull(lifecycle.acquire());
    }

    @Test
    void reportsInitialStartAndReplacementAsRestart() {
        RecordingStatusReporter reporter = new RecordingStatusReporter();
        DartAnalysisLifecycle lifecycle = synchronousLifecycle("counter_app", reporter);

        lifecycle.open();
        DartAnalysisLifecycle.Ticket first = lifecycle.acquire();
        assertNotNull(first);
        assertTrue(lifecycle.beginStart(first));
        assertTrue(lifecycle.attach(first, () -> { }, null));

        DartAnalysisLifecycle.Ticket replacement = lifecycle.acquire();
        assertNotNull(replacement);
        assertTrue(lifecycle.beginStart(replacement));
        assertTrue(lifecycle.attach(replacement, () -> { }, null));

        assertEquals(List.of(
                "clear",
                "starting:counter_app:false",
                "running:counter_app:false",
                "starting:counter_app:true",
                "running:counter_app:true"), reporter.events);
    }

    @Test
    void deduplicatesFailureByGenerationAndReason() {
        RecordingStatusReporter reporter = new RecordingStatusReporter();
        DartAnalysisLifecycle lifecycle = synchronousLifecycle("broken_app", reporter);

        lifecycle.open();
        DartAnalysisLifecycle.Ticket ticket = lifecycle.acquire();
        assertNotNull(ticket);
        lifecycle.fail(ticket, "Dart SDK was not found.");
        lifecycle.fail(ticket, "Dart SDK was not found.");
        lifecycle.fail(ticket, "Dart executable is invalid.");

        assertEquals(List.of(
                "clear",
                "failed:broken_app:Dart SDK was not found.:true",
                "failed:broken_app:Dart executable is invalid.:true"), reporter.events);
    }

    @Test
    void retryUpdatesFailureStatusWithoutRepeatingNotification() {
        RecordingStatusReporter reporter = new RecordingStatusReporter();
        DartAnalysisLifecycle lifecycle = synchronousLifecycle("retry_app", reporter);

        lifecycle.open();
        DartAnalysisLifecycle.Ticket first = lifecycle.acquire();
        assertNotNull(first);
        lifecycle.fail(first, "Dart SDK is unavailable.");

        DartAnalysisLifecycle.Ticket retry = lifecycle.acquire();
        assertNotNull(retry);
        assertTrue(lifecycle.beginStart(retry));
        lifecycle.fail(retry, "Dart SDK is unavailable.");

        assertEquals(List.of(
                "clear",
                "failed:retry_app:Dart SDK is unavailable.:true",
                "starting:retry_app:false",
                "failed:retry_app:Dart SDK is unavailable.:false"), reporter.events);
    }

    @Test
    void ignoresStaleFailureAndDuplicateConcurrentStart() {
        RecordingStatusReporter reporter = new RecordingStatusReporter();
        DartAnalysisLifecycle lifecycle = synchronousLifecycle("sample", reporter);

        lifecycle.open();
        DartAnalysisLifecycle.Ticket stale = lifecycle.acquire();
        assertNotNull(stale);
        assertTrue(lifecycle.beginStart(stale));
        assertFalse(lifecycle.beginStart(stale));
        lifecycle.close();
        lifecycle.open();
        lifecycle.fail(stale, "Late failure from the old generation.");

        assertEquals(List.of(
                "clear",
                "starting:sample:false",
                "clear",
                "clear"), reporter.events);
    }

    @Test
    void olderAttemptCannotOverrideNewerRunningState() {
        RecordingStatusReporter reporter = new RecordingStatusReporter();
        DartAnalysisLifecycle lifecycle = synchronousLifecycle("race_safe", reporter);

        lifecycle.open();
        DartAnalysisLifecycle.Ticket older = lifecycle.acquire();
        assertNotNull(older);
        assertTrue(lifecycle.beginStart(older));
        DartAnalysisLifecycle.Ticket newer = lifecycle.acquire();
        assertNotNull(newer);

        assertFalse(lifecycle.beginStart(older));
        assertTrue(lifecycle.beginStart(newer));
        assertTrue(lifecycle.attach(newer, () -> { }, null));
        lifecycle.fail(older, "A late failure must be ignored.");

        assertEquals(List.of(
                "clear",
                "starting:race_safe:false",
                "starting:race_safe:false",
                "running:race_safe:false"), reporter.events);
    }

    private static DartAnalysisLifecycle synchronousLifecycle() {
        return new DartAnalysisLifecycle((owner, restarter, completion) -> {
            try {
                if (restarter != null) {
                    restarter.run();
                }
                owner.close();
            } catch (Exception ex) {
                throw new AssertionError(ex);
            } finally {
                completion.run();
            }
        });
    }

    private static DartAnalysisLifecycle synchronousLifecycle(
            String projectName,
            DartAnalysisStatusReporter reporter) {
        return new DartAnalysisLifecycle(projectName, reporter,
                (owner, restarter, completion) -> {
                    try {
                        if (restarter != null) {
                            restarter.run();
                        }
                        owner.close();
                    } catch (Exception ex) {
                        throw new AssertionError(ex);
                    } finally {
                        completion.run();
                    }
                });
    }

    private static final class RecordingStatusReporter
            implements DartAnalysisStatusReporter {
        private final List<String> events = new ArrayList<>();

        @Override
        public void starting(String projectName, boolean restarting) {
            events.add("starting:" + projectName + ":" + restarting);
        }

        @Override
        public void running(String projectName, boolean restarted) {
            events.add("running:" + projectName + ":" + restarted);
        }

        @Override
        public void failed(String projectName, String reason, boolean notifyUser) {
            events.add("failed:" + projectName + ":" + reason + ":" + notifyUser);
        }

        @Override
        public void clear() {
            events.add("clear");
        }
    }
}
