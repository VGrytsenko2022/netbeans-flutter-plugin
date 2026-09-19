package io.github.vgrytsenko2022.designer.copy;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.OriginalFdBytes;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/** Immutable metadata transformation for one independent copied form. */
public record DesignerPairCopyPlan(
        String originalDartFile,
        String targetDartFile,
        StableId targetDocumentId,
        DesignerDocument originalDocument,
        DesignerDocument targetDocument,
        OriginalFdBytes originalFdBytes,
        OriginalFdBytes targetFdBytes) {

    public DesignerPairCopyPlan {
        originalDartFile = requireName(originalDartFile, "originalDartFile");
        targetDartFile = requireName(targetDartFile, "targetDartFile");
        Objects.requireNonNull(targetDocumentId, "targetDocumentId");
        Objects.requireNonNull(originalDocument, "originalDocument");
        Objects.requireNonNull(targetDocument, "targetDocument");
        Objects.requireNonNull(originalFdBytes, "originalFdBytes");
        Objects.requireNonNull(targetFdBytes, "targetFdBytes");
        if (!originalDocument.source().dartFile().equals(originalDartFile)) {
            throw new IllegalArgumentException(
                    "originalDocument does not name originalDartFile");
        }
        if (!targetDocument.source().dartFile().equals(targetDartFile)) {
            throw new IllegalArgumentException(
                    "targetDocument does not name targetDartFile");
        }
        if (targetDocumentId.equals(originalDocument.documentId())) {
            throw new IllegalArgumentException(
                    "A copied form requires a distinct targetDocumentId");
        }
        if (!targetDocument.documentId().equals(targetDocumentId)) {
            throw new IllegalArgumentException(
                    "targetDocument does not name targetDocumentId");
        }

        DartSourceDescriptor originalSource = originalDocument.source();
        DartSourceDescriptor expectedTargetSource = new DartSourceDescriptor(
                targetDartFile,
                originalSource.className(),
                originalSource.widgetKind(),
                originalSource.generatorVersion(),
                originalSource.managedRegions());
        DesignerDocument expectedTargetDocument = new DesignerDocument(
                originalDocument.schemaReference(),
                targetDocumentId,
                expectedTargetSource,
                originalDocument.canvas(),
                originalDocument.root(),
                originalDocument.extensions());
        if (!targetDocument.equals(expectedTargetDocument)) {
            throw new IllegalArgumentException(
                    "targetDocument may differ from originalDocument only in "
                    + "documentId and source.dartFile");
        }

        FdDocumentCodec codec = new FdDocumentCodec();
        requireExactCurrentDocument(
                codec.decode(originalFdBytes),
                originalDocument,
                "originalFdBytes");
        requireExactCurrentDocument(
                codec.decode(targetFdBytes),
                targetDocument,
                "targetFdBytes");
    }

    private static String requireName(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    private static void requireExactCurrentDocument(
            FdDecodeResult decoded,
            DesignerDocument expected,
            String label) {
        if (!(decoded instanceof FdDecodeResult.Current current)
                || current.migrated()
                || !current.document().equals(expected)) {
            throw new IllegalArgumentException(
                    label + " does not encode its exact non-migrated plan document");
        }
    }
}
