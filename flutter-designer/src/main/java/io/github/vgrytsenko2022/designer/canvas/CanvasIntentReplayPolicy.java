package io.github.vgrytsenko2022.designer.canvas;

/**
 * Trusted host-side replay policy for a classified Canvas interaction.
 *
 * <p>The runtime does not choose this value. Selection may be classified as
 * {@link #IDEMPOTENT}; every intent that can create or apply a Designer command
 * must be classified as {@link #ONE_SHOT}.</p>
 */
public enum CanvasIntentReplayPolicy {
    IDEMPOTENT,
    ONE_SHOT
}
