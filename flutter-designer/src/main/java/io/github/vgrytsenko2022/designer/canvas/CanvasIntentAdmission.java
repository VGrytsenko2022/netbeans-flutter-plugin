package io.github.vgrytsenko2022.designer.canvas;

/** Result of exact-layout and replay admission for a Canvas interaction. */
public enum CanvasIntentAdmission {
    /** First accepted delivery of this session-scoped intent sequence. */
    ACCEPTED,
    /** Exact replay that the trusted host classified as idempotent. */
    ACCEPTED_IDEMPOTENT_REPLAY,
    /** Candidate belongs to another open Canvas session. */
    STALE_SESSION,
    /** Candidate did not observe the exact latest accepted layout. */
    STALE_LAYOUT,
    /** Candidate sequence precedes an already accepted session intent. */
    STALE_INTENT,
    /** Candidate skipped one or more required session intent sequences. */
    OUT_OF_ORDER_INTENT,
    /** An accepted sequence was reused with different identity or policy. */
    CONFLICTING_INTENT_ID,
    /** Exact replay of an already accepted one-shot intent. */
    REPLAYED_ONE_SHOT,
    /** The owning open Canvas session has already closed. */
    CLOSED;

    public boolean accepted() {
        return this == ACCEPTED || this == ACCEPTED_IDEMPOTENT_REPLAY;
    }

    public boolean firstDelivery() {
        return this == ACCEPTED;
    }
}
