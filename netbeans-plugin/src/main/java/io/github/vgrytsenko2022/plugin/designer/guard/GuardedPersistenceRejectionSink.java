package io.github.vgrytsenko2022.plugin.designer.guard;

/**
 * Pre-commit rejection edge for an exact persistence output.
 *
 * <p>The guarded writer calls this before closing its delegate whenever it
 * must emit a known-good fallback instead of the current live document. Exact
 * transaction outputs can therefore refuse the fallback before any filesystem
 * write, rather than trying to infer the cause from an exception raised after
 * delegate close.</p>
 */
public interface GuardedPersistenceRejectionSink {

    void rejectGuardedPersistence(String reason);
}
