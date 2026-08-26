package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;

/** Immutable in-process request to present one validated Designer revision. */
public record CanvasRenderRequest(
        CanvasRevisionKey revisionKey,
        CanvasRenderProfile renderProfile,
        ValidatedCanvasRevisionSnapshot snapshot) {

    public CanvasRenderRequest {
        Objects.requireNonNull(revisionKey, "revisionKey");
        Objects.requireNonNull(renderProfile, "renderProfile");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!revisionKey.documentId().equals(snapshot.document().documentId())) {
            throw new IllegalArgumentException(
                    "Canvas revision and Designer document ids must match");
        }
        if (revisionKey.logicalRevisionId() != snapshot.logicalRevisionId()) {
            throw new IllegalArgumentException(
                    "Canvas key and validated snapshot revision ids must match");
        }
    }
}
