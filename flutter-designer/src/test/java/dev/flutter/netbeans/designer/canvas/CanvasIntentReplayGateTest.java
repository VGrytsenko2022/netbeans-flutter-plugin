package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.StableId;
import org.junit.jupiter.api.Test;

class CanvasIntentReplayGateTest {
    private static final CanvasSessionId SESSION_A = CanvasSessionId.parse(
            "80ef60ed-b108-4674-99a6-c1f3102f01ab");
    private static final CanvasSessionId SESSION_B = CanvasSessionId.parse(
            "0d236d13-7048-4bc6-a11f-bf03a85d1724");
    private static final StableId DOCUMENT = StableId.parse(
            "83ed3c05-88e7-4220-8377-29fa1f21a99e");

    @Test
    void layoutAndIntentIdentitiesRetainTheExactSessionFrameAndLayout() {
        CanvasFrameKey frame = frame(SESSION_A, 4, 7);
        CanvasLayoutKey layout = new CanvasLayoutKey(frame, 3);
        CanvasIntentId intentId = new CanvasIntentId(SESSION_A, 12);
        CanvasIntentKey intent = new CanvasIntentKey(intentId, layout);

        assertEquals(frame, layout.frameKey());
        assertEquals(3, layout.layoutSequence());
        assertEquals(SESSION_A, layout.sessionId());
        assertEquals(SESSION_A, intent.intentId().sessionId());
        assertEquals(12, intent.intentId().intentSequence());
        assertEquals(layout, intent.layoutKey());
    }

    @Test
    void exactSelectionReplayIsAdmittedAsIdempotentButNotAsFirstDelivery() {
        CanvasLayoutKey layout = layout(SESSION_A, 0, 0, 0);
        CanvasIntentKey selection = intent(SESSION_A, 0, layout);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        CanvasIntentAdmission first = gate.admit(
                layout, selection, CanvasIntentReplayPolicy.IDEMPOTENT);
        CanvasIntentAdmission replay = gate.admit(
                layout, selection, CanvasIntentReplayPolicy.IDEMPOTENT);

        assertEquals(CanvasIntentAdmission.ACCEPTED, first);
        assertTrue(first.accepted());
        assertTrue(first.firstDelivery());
        assertEquals(CanvasIntentAdmission.ACCEPTED_IDEMPOTENT_REPLAY, replay);
        assertTrue(replay.accepted());
        assertFalse(replay.firstDelivery());
    }

    @Test
    void exactReplayIsRejectedAfterTheAcceptedLayoutAdvances() {
        CanvasLayoutKey original = layout(SESSION_A, 0, 0, 0);
        CanvasLayoutKey current = layout(SESSION_A, 1, 0, 0);
        CanvasIntentKey selection = intent(SESSION_A, 0, original);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        assertEquals(CanvasIntentAdmission.ACCEPTED, gate.admit(
                original, selection, CanvasIntentReplayPolicy.IDEMPOTENT));
        assertEquals(CanvasIntentAdmission.STALE_LAYOUT, gate.admit(
                current, selection, CanvasIntentReplayPolicy.IDEMPOTENT));
    }

    @Test
    void exactMutatingIntentReplayIsRejectedOneShot() {
        CanvasLayoutKey layout = layout(SESSION_A, 0, 0, 0);
        CanvasIntentKey drop = intent(SESSION_A, 0, layout);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        assertEquals(CanvasIntentAdmission.ACCEPTED, gate.admit(
                layout, drop, CanvasIntentReplayPolicy.ONE_SHOT));
        CanvasIntentAdmission replay = gate.admit(
                layout, drop, CanvasIntentReplayPolicy.ONE_SHOT);

        assertEquals(CanvasIntentAdmission.REPLAYED_ONE_SHOT, replay);
        assertFalse(replay.accepted());
        assertFalse(replay.firstDelivery());
    }

    @Test
    void rejectsStaleLayoutForeignSessionOlderSequenceAndChangedPolicy() {
        CanvasLayoutKey current = layout(SESSION_A, 2, 4, 1);
        CanvasLayoutKey stale = layout(SESSION_A, 2, 4, 0);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        assertEquals(CanvasIntentAdmission.STALE_LAYOUT, gate.admit(
                current,
                intent(SESSION_A, 0, stale),
                CanvasIntentReplayPolicy.IDEMPOTENT));
        assertEquals(CanvasIntentAdmission.STALE_SESSION, gate.admit(
                current,
                intent(SESSION_B, 0, layout(SESSION_B, 2, 4, 1)),
                CanvasIntentReplayPolicy.IDEMPOTENT));

        CanvasIntentKey accepted = intent(SESSION_A, 1, current);
        assertEquals(CanvasIntentAdmission.ACCEPTED, gate.admit(
                current, accepted, CanvasIntentReplayPolicy.IDEMPOTENT));
        assertEquals(CanvasIntentAdmission.STALE_INTENT, gate.admit(
                current,
                intent(SESSION_A, 0, current),
                CanvasIntentReplayPolicy.IDEMPOTENT));
        assertEquals(CanvasIntentAdmission.CONFLICTING_INTENT_ID, gate.admit(
                current, accepted, CanvasIntentReplayPolicy.ONE_SHOT));
    }

    @Test
    void rejectedEvidenceConsumesItsIdAndSequenceMustRemainContiguous() {
        CanvasLayoutKey current = layout(SESSION_A, 3, 0, 0);
        CanvasLayoutKey stale = layout(SESSION_A, 2, 9, 9);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        assertEquals(CanvasIntentAdmission.STALE_LAYOUT, gate.admit(
                current,
                intent(SESSION_A, 0, stale),
                CanvasIntentReplayPolicy.ONE_SHOT));
        assertEquals(CanvasIntentAdmission.STALE_LAYOUT, gate.admit(
                current,
                intent(SESSION_A, 0, stale),
                CanvasIntentReplayPolicy.ONE_SHOT));
        assertEquals(CanvasIntentAdmission.CONFLICTING_INTENT_ID, gate.admit(
                current,
                intent(SESSION_A, 0, current),
                CanvasIntentReplayPolicy.ONE_SHOT));
        assertEquals(CanvasIntentAdmission.OUT_OF_ORDER_INTENT, gate.admit(
                current,
                intent(SESSION_A, Long.MAX_VALUE, current),
                CanvasIntentReplayPolicy.IDEMPOTENT));
        assertEquals(CanvasIntentAdmission.ACCEPTED, gate.admit(
                current,
                intent(SESSION_A, 1, current),
                CanvasIntentReplayPolicy.IDEMPOTENT));
        assertEquals(CanvasIntentAdmission.OUT_OF_ORDER_INTENT, gate.admit(
                current,
                intent(SESSION_A, 3, current),
                CanvasIntentReplayPolicy.ONE_SHOT));
        assertEquals(CanvasIntentAdmission.ACCEPTED, gate.admit(
                current,
                intent(SESSION_A, 2, current),
                CanvasIntentReplayPolicy.IDEMPOTENT));
    }

    @Test
    void rejectsInvalidProtocolValuesAndForeignCurrentLayout() {
        CanvasFrameKey frame = frame(SESSION_A, 0, 0);
        CanvasLayoutKey layout = new CanvasLayoutKey(frame, 0);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        assertThrows(IllegalArgumentException.class,
                () -> new CanvasLayoutKey(frame, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasIntentId(SESSION_A, -1));
        assertThrows(IllegalArgumentException.class, () -> new CanvasIntentKey(
                new CanvasIntentId(SESSION_B, 0), layout));
        assertThrows(IllegalArgumentException.class, () -> gate.admit(
                layout(SESSION_B, 0, 0, 0),
                intent(SESSION_A, 0, layout),
                CanvasIntentReplayPolicy.IDEMPOTENT));
    }

    @Test
    void closeFencesLateIntentAndIsIdempotent() {
        CanvasLayoutKey layout = layout(SESSION_A, 0, 0, 0);
        CanvasIntentKey intent = intent(SESSION_A, 0, layout);
        CanvasIntentReplayGate gate = new CanvasIntentReplayGate(SESSION_A);

        gate.close();
        gate.close();

        assertTrue(gate.closed());
        assertEquals(CanvasIntentAdmission.CLOSED, gate.admit(
                layout, intent, CanvasIntentReplayPolicy.IDEMPOTENT));
    }

    private static CanvasIntentKey intent(
            CanvasSessionId session,
            long intentSequence,
            CanvasLayoutKey layout) {
        return new CanvasIntentKey(
                new CanvasIntentId(session, intentSequence),
                layout);
    }

    private static CanvasLayoutKey layout(
            CanvasSessionId session,
            long presentationSequence,
            long frameSequence,
            long layoutSequence) {
        return new CanvasLayoutKey(
                frame(session, presentationSequence, frameSequence),
                layoutSequence);
    }

    private static CanvasFrameKey frame(
            CanvasSessionId session,
            long presentationSequence,
            long frameSequence) {
        return new CanvasFrameKey(
                new CanvasRevisionKey(
                        session,
                        presentationSequence,
                        DOCUMENT,
                        presentationSequence),
                frameSequence);
    }
}
