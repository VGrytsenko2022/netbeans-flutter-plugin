package dev.flutter.netbeans.designer.copy;

import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdEncodeException;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Pure, bounded planner for an independent copy of one Designer pair. */
public final class DesignerPairCopyPlanner {
    private static final Pattern DART_FILE =
            Pattern.compile("_?[a-z][a-z0-9_]*\\.dart");

    private final FdDocumentCodec codec;
    private final Function<OriginalFdBytes, FdDecodeResult> decoder;

    public DesignerPairCopyPlanner() {
        this(new FdDocumentCodec());
    }

    DesignerPairCopyPlanner(FdDocumentCodec codec) {
        this(codec, codec::decode);
    }

    DesignerPairCopyPlanner(
            FdDocumentCodec codec,
            Function<OriginalFdBytes, FdDecodeResult> decoder) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
    }

    /**
     * Prepares canonical copied metadata while leaving Dart source bytes to
     * the NetBeans transaction boundary.
     */
    public DesignerPairCopyResult prepare(
            OriginalFdBytes originalFd,
            String originalDartFile,
            String targetDartFile,
            StableId targetDocumentId) {
        Objects.requireNonNull(originalFd, "originalFd");
        Objects.requireNonNull(targetDocumentId, "targetDocumentId");
        if (!validDartFile(originalDartFile)) {
            return rejected(
                    DesignerPairCopyResult.Code.INVALID_ORIGINAL_FILENAME,
                    "/source/dartFile",
                    "The current Dart filename is outside the schema-v1 contract.");
        }
        if (!validDartFile(targetDartFile)) {
            return rejected(
                    DesignerPairCopyResult.Code.INVALID_TARGET_FILENAME,
                    "/source/dartFile",
                    "The target Dart filename must be canonical lower_snake_case.dart.");
        }

        FdDecodeResult decoded = decoder.apply(originalFd);
        if (!(decoded instanceof FdDecodeResult.Current current)) {
            return rejected(
                    DesignerPairCopyResult.Code.MODEL_NOT_CURRENT,
                    "",
                    "Only a valid current-version Flutter Designer model can be copied.");
        }
        if (current.migrated()) {
            return rejected(
                    DesignerPairCopyResult.Code.MIGRATION_REQUIRED,
                    "",
                    "Copy cannot silently persist a schema migration.");
        }

        DesignerDocument originalDocument = current.document();
        DartSourceDescriptor originalSource = originalDocument.source();
        if (!originalSource.dartFile().equals(originalDartFile)) {
            return rejected(
                    DesignerPairCopyResult.Code.SOURCE_REFERENCE_MISMATCH,
                    "/source/dartFile",
                    "The model does not name the exact current Dart file.");
        }
        if (targetDocumentId.equals(originalDocument.documentId())) {
            return rejected(
                    DesignerPairCopyResult.Code.SAME_DOCUMENT_ID,
                    "/documentId",
                    "A copied form requires a distinct target documentId.");
        }

        DartSourceDescriptor targetSource = new DartSourceDescriptor(
                targetDartFile,
                originalSource.className(),
                originalSource.widgetKind(),
                originalSource.generatorVersion(),
                originalSource.managedRegions());
        DesignerDocument targetDocument = new DesignerDocument(
                originalDocument.schemaReference(),
                targetDocumentId,
                targetSource,
                originalDocument.canvas(),
                originalDocument.root(),
                originalDocument.extensions());

        OriginalFdBytes targetFd;
        try {
            targetFd = codec.encode(targetDocument);
        } catch (FdEncodeException failure) {
            return rejected(
                    DesignerPairCopyResult.Code.ENCODE_FAILED,
                    failure.diagnostic().pointer(),
                    "The copied Flutter Designer model could not be encoded: "
                    + failure.diagnostic().message());
        }

        FdDecodeResult roundTrip = codec.decode(targetFd);
        if (!(roundTrip instanceof FdDecodeResult.Current verified)
                || verified.migrated()
                || !verified.document().equals(targetDocument)) {
            return rejected(
                    DesignerPairCopyResult.Code.ROUND_TRIP_MISMATCH,
                    "",
                    "The copied model did not pass an exact canonical round trip.");
        }
        return new DesignerPairCopyResult.Ready(new DesignerPairCopyPlan(
                originalDartFile,
                targetDartFile,
                targetDocumentId,
                originalDocument,
                targetDocument,
                originalFd,
                targetFd));
    }

    private static boolean validDartFile(String value) {
        return value != null && DART_FILE.matcher(value).matches();
    }

    private static DesignerPairCopyResult.Rejected rejected(
            DesignerPairCopyResult.Code code,
            String pointer,
            String message) {
        return new DesignerPairCopyResult.Rejected(code, pointer, message);
    }
}
