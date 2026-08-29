package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class FlutterDesignerPaletteDragRegistryTest {
    private static final UUID CONTEXT_A = uuid(1);
    private static final UUID CONTEXT_B = uuid(2);
    private static final UUID DRAG_A = uuid(101);
    private static final UUID DRAG_B = uuid(102);
    private static final UUID DRAG_C = uuid(103);
    private static final WidgetTypeId TEXT = new WidgetTypeId("flutter.widgets.Text");
    private static final WidgetTypeId CENTER = new WidgetTypeId("flutter.widgets.Center");

    @Test
    void issuesOpaqueContextBoundTokenAndConsumesItExactlyOnce() {
        MutableClock clock = clock();
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A, clock, Duration.ofSeconds(10), 4, ids(DRAG_A));

        String token = registry.issue(TEXT).orElseThrow();

        assertEquals("nbfdnd:v1:" + CONTEXT_A + ':' + DRAG_A, token);
        assertFalse(token.contains(TEXT.value()));
        assertEquals(1, registry.outstandingCount());
        assertEquals(TEXT, registry.consume(token).orElseThrow());
        assertTrue(registry.consume(token).isEmpty(), "a drag token is one-shot");
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void resolvesLiveAuthorityRepeatedlyWithoutConsumingIt() {
        MutableClock clock = clock();
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A, clock, Duration.ofSeconds(5), 1, ids(DRAG_A));
        String token = registry.issue(TEXT).orElseThrow();

        assertEquals(TEXT, registry.resolve(token).orElseThrow());
        assertEquals(TEXT, registry.resolve(token).orElseThrow());
        assertEquals(1, registry.outstandingCount());

        clock.advance(Duration.ofSeconds(5));
        assertTrue(registry.resolve(token).isEmpty());
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void malformedAndWrongTokensDoNotConsumeTheValidEntry() {
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A, clock(), Duration.ofSeconds(10), 4, ids(DRAG_A));
        String valid = registry.issue(TEXT).orElseThrow();

        assertTrue(registry.consume(null).isEmpty());
        assertTrue(registry.consume("").isEmpty());
        assertTrue(registry.consume("nbfdnd:v2:" + CONTEXT_A + ':' + DRAG_A).isEmpty());
        assertTrue(registry.consume("nbfdnd:v1:" + CONTEXT_A + ":not-a-uuid").isEmpty());
        assertTrue(registry.consume(valid.toUpperCase()).isEmpty());
        assertEquals(1, registry.outstandingCount());
        assertEquals(TEXT, registry.consume(valid).orElseThrow());
    }

    @Test
    void tokenExpiresAtItsExactDeadline() {
        MutableClock clock = clock();
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A, clock, Duration.ofSeconds(5), 1, ids(DRAG_A));
        String token = registry.issue(TEXT).orElseThrow();

        clock.advance(Duration.ofSeconds(5));

        assertTrue(registry.consume(token).isEmpty());
        assertEquals(0, registry.outstandingCount());
    }

    @Test
    void foreignViewCannotConsumeOrInvalidateTheOwningViewsToken() {
        FlutterDesignerPaletteDragRegistry first = registry(
                CONTEXT_A, clock(), Duration.ofSeconds(10), 2, ids(DRAG_A));
        FlutterDesignerPaletteDragRegistry second = registry(
                CONTEXT_B, clock(), Duration.ofSeconds(10), 2, ids(DRAG_B));
        String token = first.issue(TEXT).orElseThrow();

        assertTrue(second.consume(token).isEmpty());
        assertEquals(TEXT, first.consume(token).orElseThrow());
    }

    @Test
    void revokeAllInvalidatesEveryOutstandingToken() {
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A,
                clock(),
                Duration.ofSeconds(10),
                3,
                ids(DRAG_A, DRAG_B));
        String text = registry.issue(TEXT).orElseThrow();
        String center = registry.issue(CENTER).orElseThrow();

        registry.revokeAll();

        assertEquals(0, registry.outstandingCount());
        assertTrue(registry.consume(text).isEmpty());
        assertTrue(registry.consume(center).isEmpty());
    }

    @Test
    void replacementIssueRevokesCanceledDragAndKeepsOneLiveLease() {
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A,
                clock(),
                Duration.ofSeconds(10),
                4,
                ids(DRAG_A, DRAG_B));
        String canceled = registry.issueReplacingOutstanding(TEXT).orElseThrow();

        String replacement = registry.issueReplacingOutstanding(TEXT).orElseThrow();

        assertEquals(1, registry.outstandingCount());
        assertTrue(registry.consume(canceled).isEmpty());
        assertEquals(TEXT, registry.consume(replacement).orElseThrow());
    }

    @Test
    void maximumIsFailClosedAndCapacityReturnsAfterConsumption() {
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A,
                clock(),
                Duration.ofSeconds(10),
                2,
                ids(DRAG_A, DRAG_B, DRAG_C));
        String first = registry.issue(TEXT).orElseThrow();
        String second = registry.issue(CENTER).orElseThrow();

        assertTrue(registry.issue(TEXT).isEmpty());
        assertEquals(2, registry.outstandingCount());
        assertEquals(TEXT, registry.consume(first).orElseThrow());
        String third = registry.issue(TEXT).orElseThrow();
        assertEquals("nbfdnd:v1:" + CONTEXT_A + ':' + DRAG_C, third);
        assertEquals(CENTER, registry.consume(second).orElseThrow());
        assertEquals(TEXT, registry.consume(third).orElseThrow());
    }

    @Test
    void expiredEntriesArePurgedBeforeTheMaximumIsChecked() {
        MutableClock clock = clock();
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A,
                clock,
                Duration.ofSeconds(2),
                1,
                ids(DRAG_A, DRAG_B));
        String expired = registry.issue(TEXT).orElseThrow();

        clock.advance(Duration.ofSeconds(2));
        String replacement = registry.issue(CENTER).orElseThrow();

        assertTrue(registry.consume(expired).isEmpty());
        assertEquals(CENTER, registry.consume(replacement).orElseThrow());
    }

    @Test
    void duplicateDragIdentifiersCannotOverwriteLiveAuthority() {
        FlutterDesignerPaletteDragRegistry registry = registry(
                CONTEXT_A,
                clock(),
                Duration.ofSeconds(10),
                2,
                () -> DRAG_A);
        String first = registry.issue(TEXT).orElseThrow();

        assertTrue(registry.issue(CENTER).isEmpty());
        assertEquals(TEXT, registry.consume(first).orElseThrow());
    }

    @Test
    void constructorRejectsUnboundedLifetimeAndEntryCounts() {
        MutableClock clock = clock();
        assertThrows(IllegalArgumentException.class, () -> registry(
                CONTEXT_A, clock, Duration.ZERO, 1, ids(DRAG_A)));
        assertThrows(IllegalArgumentException.class, () -> registry(
                CONTEXT_A, clock, Duration.ofMinutes(6), 1, ids(DRAG_A)));
        assertThrows(IllegalArgumentException.class, () -> registry(
                CONTEXT_A, clock, Duration.ofSeconds(1), 0, ids(DRAG_A)));
        assertThrows(IllegalArgumentException.class, () -> registry(
                CONTEXT_A, clock, Duration.ofSeconds(1), 1_025, ids(DRAG_A)));
    }

    private static FlutterDesignerPaletteDragRegistry registry(
            UUID context,
            Clock clock,
            Duration lifetime,
            int maximum,
            Supplier<UUID> ids) {
        return new FlutterDesignerPaletteDragRegistry(
                context, clock, lifetime, maximum, ids);
    }

    private static MutableClock clock() {
        return new MutableClock(Instant.parse("2026-08-28T10:00:00Z"));
    }

    private static Supplier<UUID> ids(UUID... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[index.getAndIncrement()];
    }

    private static UUID uuid(long suffix) {
        return new UUID(0, suffix);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
