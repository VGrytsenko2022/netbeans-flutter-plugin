package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;

/**
 * Thread-safe, bounded replay gate for exact-layout Canvas interactions.
 *
 * <p>The caller supplies the latest layout already accepted by its presentation
 * controller. This class deliberately does not own renderer or presentation
 * lifecycle state. It retains only the greatest accepted session-wide intent
 * sequence, so its memory use is constant. Admission is evidence only and does
 * not authorize a Designer command, file write, Save, Undo or Redo. Every
 * observed same-session sequence is consumed exactly once, including rejected
 * stale-layout evidence, so an untrusted runtime cannot later rebind that id
 * to a different payload.</p>
 */
public final class CanvasIntentReplayGate implements AutoCloseable {
    private final CanvasSessionId sessionId;
    private long nextIntentSequence;
    private CanvasIntentKey lastObservedKey;
    private CanvasIntentReplayPolicy lastObservedPolicy;
    private CanvasIntentAdmission lastObservedAdmission;
    private boolean closed;

    public CanvasIntentReplayGate(CanvasSessionId sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    }

    /**
     * Consumes one observed intent identity before presentation-dependent
     * semantic checks are performed by the caller.
     *
     * <p>This entry point is for controller races where the current accepted
     * layout may temporarily be absent (for example while a newer model is
     * being published). Every contiguous same-session sequence still advances
     * the replay fence. Callers must authorize the intent separately and must
     * act only on a first delivery.</p>
     */
    public synchronized CanvasIntentAdmission consume(
            CanvasIntentKey candidate,
            CanvasIntentReplayPolicy policy) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(policy, "policy");
        if (closed) {
            return CanvasIntentAdmission.CLOSED;
        }
        if (!sessionId.equals(candidate.intentId().sessionId())) {
            return CanvasIntentAdmission.STALE_SESSION;
        }
        long candidateSequence = candidate.intentId().intentSequence();
        if (candidateSequence < nextIntentSequence) {
            if (lastObservedKey != null
                    && candidateSequence
                    == lastObservedKey.intentId().intentSequence()) {
                if (!lastObservedKey.equals(candidate)
                        || lastObservedPolicy != policy) {
                    return CanvasIntentAdmission.CONFLICTING_INTENT_ID;
                }
                if (lastObservedAdmission != CanvasIntentAdmission.ACCEPTED) {
                    return lastObservedAdmission;
                }
                return policy == CanvasIntentReplayPolicy.IDEMPOTENT
                        ? CanvasIntentAdmission.ACCEPTED_IDEMPOTENT_REPLAY
                        : CanvasIntentAdmission.REPLAYED_ONE_SHOT;
            }
            return CanvasIntentAdmission.STALE_INTENT;
        }
        if (candidateSequence > nextIntentSequence) {
            return CanvasIntentAdmission.OUT_OF_ORDER_INTENT;
        }
        remember(candidate, policy, CanvasIntentAdmission.ACCEPTED);
        nextIntentSequence++;
        return CanvasIntentAdmission.ACCEPTED;
    }

    /**
     * Checks one candidate against the exact latest accepted layout.
     *
     * <p>{@code policy} must be derived by trusted Java code from the intent
     * kind, never accepted from an untrusted runtime message.</p>
     */
    public synchronized CanvasIntentAdmission admit(
            CanvasLayoutKey currentAcceptedLayout,
            CanvasIntentKey candidate,
            CanvasIntentReplayPolicy policy) {
        Objects.requireNonNull(currentAcceptedLayout, "currentAcceptedLayout");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(policy, "policy");
        if (closed) {
            return CanvasIntentAdmission.CLOSED;
        }
        if (!sessionId.equals(currentAcceptedLayout.sessionId())) {
            throw new IllegalArgumentException(
                    "Current accepted layout must belong to this replay gate");
        }
        if (!sessionId.equals(candidate.intentId().sessionId())) {
            return CanvasIntentAdmission.STALE_SESSION;
        }
        long candidateSequence = candidate.intentId().intentSequence();
        if (candidateSequence < nextIntentSequence) {
            if (lastObservedKey != null
                    && candidateSequence
                    == lastObservedKey.intentId().intentSequence()) {
                if (!lastObservedKey.equals(candidate)
                        || lastObservedPolicy != policy) {
                    return CanvasIntentAdmission.CONFLICTING_INTENT_ID;
                }
                if (!currentAcceptedLayout.equals(candidate.layoutKey())) {
                    return CanvasIntentAdmission.STALE_LAYOUT;
                }
                if (lastObservedAdmission != CanvasIntentAdmission.ACCEPTED) {
                    return lastObservedAdmission;
                }
                return policy == CanvasIntentReplayPolicy.IDEMPOTENT
                        ? CanvasIntentAdmission.ACCEPTED_IDEMPOTENT_REPLAY
                        : CanvasIntentAdmission.REPLAYED_ONE_SHOT;
            }
            return CanvasIntentAdmission.STALE_INTENT;
        }
        if (candidateSequence > nextIntentSequence) {
            return CanvasIntentAdmission.OUT_OF_ORDER_INTENT;
        }

        CanvasIntentAdmission admission = currentAcceptedLayout.equals(
                candidate.layoutKey())
                ? CanvasIntentAdmission.ACCEPTED
                : CanvasIntentAdmission.STALE_LAYOUT;
        remember(candidate, policy, admission);
        nextIntentSequence++;
        return admission;
    }

    public synchronized boolean closed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        closed = true;
        lastObservedKey = null;
        lastObservedPolicy = null;
        lastObservedAdmission = null;
    }

    private void remember(
            CanvasIntentKey candidate,
            CanvasIntentReplayPolicy policy,
            CanvasIntentAdmission admission) {
        lastObservedKey = candidate;
        lastObservedPolicy = policy;
        lastObservedAdmission = admission;
    }
}
