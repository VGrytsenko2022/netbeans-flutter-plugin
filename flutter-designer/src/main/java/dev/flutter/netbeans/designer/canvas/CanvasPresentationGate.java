package dev.flutter.netbeans.designer.canvas;

import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe, transport-neutral owner of Canvas presentation identities.
 *
 * <p>The gate issues a new presentation sequence for every render request,
 * including Undo/Redo publications that revisit a logical revision. A renderer
 * frame is current only when it echoes the exact session and revision key and
 * advances the current frame sequence. Layout-dependent interaction intents
 * must bind the exact latest admitted layout. Admission is evidence only and
 * never authorizes a Designer command or persistence operation.</p>
 */
public final class CanvasPresentationGate implements AutoCloseable {
    private final CanvasSessionId sessionId;
    private final StableId documentId;
    private long nextPresentationSequence;
    private CanvasRevisionKey currentRevision;
    private CanvasFrameKey currentFrame;
    private CanvasLayoutKey currentLayout;
    private boolean closed;

    public CanvasPresentationGate(
            CanvasSessionId sessionId,
            StableId documentId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.documentId = Objects.requireNonNull(documentId, "documentId");
    }

    /**
     * Publishes the next immutable in-process render request.
     *
     * <p>The snapshot already binds the logical revision, immutable document,
     * exact catalog identity and validation evidence. This method performs no
     * serialization, process work or I/O.</p>
     */
    public synchronized CanvasRenderRequest present(
            CanvasRenderProfile renderProfile,
            ValidatedCanvasRevisionSnapshot snapshot) {
        requireOpen();
        Objects.requireNonNull(renderProfile, "renderProfile");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!documentId.equals(snapshot.document().documentId())) {
            throw new IllegalArgumentException(
                    "Cannot present a Designer document owned by another Canvas gate");
        }
        if (nextPresentationSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Canvas presentation sequence is exhausted");
        }
        CanvasRevisionKey next = new CanvasRevisionKey(
                sessionId,
                nextPresentationSequence,
                documentId,
                snapshot.logicalRevisionId());
        CanvasRenderRequest request = new CanvasRenderRequest(
                next, renderProfile, snapshot);
        nextPresentationSequence++;
        currentRevision = next;
        currentFrame = null;
        currentLayout = null;
        return request;
    }

    /** Admits a newer renderer frame for the exact current publication. */
    public synchronized CanvasAdmission admitFrame(CanvasFrameKey frameKey) {
        Objects.requireNonNull(frameKey, "frameKey");
        CanvasAdmission revisionAdmission = revisionAdmission(frameKey.revisionKey());
        if (revisionAdmission != CanvasAdmission.ACCEPTED) {
            return revisionAdmission;
        }
        CanvasAdmission sequenceAdmission = frameSequenceAdmission(frameKey);
        if (sequenceAdmission != CanvasAdmission.ACCEPTED) {
            return sequenceAdmission;
        }
        currentFrame = frameKey;
        currentLayout = null;
        return CanvasAdmission.ACCEPTED;
    }

    /** Admits a newer exact layout for the latest accepted renderer frame. */
    public synchronized CanvasAdmission admitLayout(CanvasLayoutKey layoutKey) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        CanvasAdmission frameAdmission = exactFrameAdmission(layoutKey.frameKey());
        if (frameAdmission != CanvasAdmission.ACCEPTED) {
            return frameAdmission;
        }
        CanvasAdmission sequenceAdmission = layoutSequenceAdmission(layoutKey);
        if (sequenceAdmission != CanvasAdmission.ACCEPTED) {
            return sequenceAdmission;
        }
        currentLayout = layoutKey;
        return CanvasAdmission.ACCEPTED;
    }

    /**
     * Atomically admits one exact frame and its initial layout projection.
     *
     * <p>No frame state changes when either identity is stale, out of order or
     * inconsistent. Runtime controllers should prefer this method after
     * buffering independently arriving frame/layout evidence.</p>
     */
    public synchronized CanvasAdmission admitPresentation(
            CanvasFrameKey frameKey,
            CanvasLayoutKey layoutKey) {
        Objects.requireNonNull(frameKey, "frameKey");
        Objects.requireNonNull(layoutKey, "layoutKey");
        if (!frameKey.equals(layoutKey.frameKey())) {
            throw new IllegalArgumentException(
                    "Canvas layout must belong to the exact presented frame");
        }
        CanvasAdmission revisionAdmission = revisionAdmission(frameKey.revisionKey());
        if (revisionAdmission != CanvasAdmission.ACCEPTED) {
            return revisionAdmission;
        }
        CanvasAdmission frameAdmission = frameSequenceAdmission(frameKey);
        if (frameAdmission != CanvasAdmission.ACCEPTED) {
            return frameAdmission;
        }
        if (layoutKey.layoutSequence() != 0) {
            return CanvasAdmission.OUT_OF_ORDER_LAYOUT;
        }
        currentFrame = frameKey;
        currentLayout = layoutKey;
        return CanvasAdmission.ACCEPTED;
    }

    /** Checks a read-only selection/hit-test intent against the exact layout. */
    public synchronized CanvasAdmission admitSelection(
            CanvasLayoutKey layoutKey) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        CanvasAdmission frameAdmission = exactFrameAdmission(layoutKey.frameKey());
        if (frameAdmission != CanvasAdmission.ACCEPTED) {
            return frameAdmission;
        }
        if (currentLayout == null) {
            return CanvasAdmission.NOT_READY;
        }
        return currentLayout.equals(layoutKey)
                ? CanvasAdmission.ACCEPTED
                : CanvasAdmission.STALE_LAYOUT;
    }

    public synchronized Optional<CanvasRevisionKey> currentRevision() {
        return Optional.ofNullable(currentRevision);
    }

    public synchronized Optional<CanvasFrameKey> currentFrame() {
        return Optional.ofNullable(currentFrame);
    }

    public synchronized Optional<CanvasLayoutKey> currentLayout() {
        return Optional.ofNullable(currentLayout);
    }

    public synchronized boolean closed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        currentRevision = null;
        currentFrame = null;
        currentLayout = null;
    }

    private CanvasAdmission revisionAdmission(CanvasRevisionKey candidate) {
        if (closed) {
            return CanvasAdmission.CLOSED;
        }
        if (!sessionId.equals(candidate.sessionId())) {
            return CanvasAdmission.STALE_SESSION;
        }
        if (!documentId.equals(candidate.documentId())) {
            return CanvasAdmission.STALE_DOCUMENT;
        }
        if (currentRevision == null) {
            return CanvasAdmission.NOT_READY;
        }
        return currentRevision.equals(candidate)
                ? CanvasAdmission.ACCEPTED
                : CanvasAdmission.STALE_REVISION;
    }

    private CanvasAdmission exactFrameAdmission(CanvasFrameKey candidate) {
        CanvasAdmission revisionAdmission = revisionAdmission(candidate.revisionKey());
        if (revisionAdmission != CanvasAdmission.ACCEPTED) {
            return revisionAdmission;
        }
        if (currentFrame == null) {
            return CanvasAdmission.NOT_READY;
        }
        return currentFrame.equals(candidate)
                ? CanvasAdmission.ACCEPTED
                : CanvasAdmission.STALE_FRAME;
    }

    private CanvasAdmission frameSequenceAdmission(CanvasFrameKey candidate) {
        long expected = currentFrame == null
                ? 0
                : incrementSequence(
                        currentFrame.frameSequence(), "Canvas frame sequence");
        if (candidate.frameSequence() < expected) {
            return CanvasAdmission.STALE_FRAME;
        }
        return candidate.frameSequence() == expected
                ? CanvasAdmission.ACCEPTED
                : CanvasAdmission.OUT_OF_ORDER_FRAME;
    }

    private CanvasAdmission layoutSequenceAdmission(CanvasLayoutKey candidate) {
        long expected = currentLayout == null
                ? 0
                : incrementSequence(
                        currentLayout.layoutSequence(), "Canvas layout sequence");
        if (candidate.layoutSequence() < expected) {
            return CanvasAdmission.STALE_LAYOUT;
        }
        return candidate.layoutSequence() == expected
                ? CanvasAdmission.ACCEPTED
                : CanvasAdmission.OUT_OF_ORDER_LAYOUT;
    }

    private static long incrementSequence(long current, String label) {
        if (current == Long.MAX_VALUE) {
            throw new IllegalStateException(label + " is exhausted");
        }
        return current + 1;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Canvas presentation gate is closed");
        }
    }
}
