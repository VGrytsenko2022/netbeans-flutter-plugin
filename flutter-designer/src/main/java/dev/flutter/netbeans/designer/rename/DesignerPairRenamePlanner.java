package dev.flutter.netbeans.designer.rename;

import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdEncodeException;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Pure, bounded planner for the metadata part of a pair file rename.
 *
 * <p>It changes only {@code source.dartFile}. A Dart class rename is a
 * separate analyzer-backed refactoring because the declaration and
 * constructors are user-owned source outside generated regions.</p>
 */
public final class DesignerPairRenamePlanner {
    private static final Pattern DART_FILE =
            Pattern.compile("_?[a-z][a-z0-9_]*\\.dart");

    private final FdDocumentCodec codec;
    private final Function<OriginalFdBytes, FdDecodeResult> decoder;

    public DesignerPairRenamePlanner() {
        this(new FdDocumentCodec());
    }

    DesignerPairRenamePlanner(FdDocumentCodec codec) {
        this(codec, codec::decode);
    }

    DesignerPairRenamePlanner(
            FdDocumentCodec codec,
            Function<OriginalFdBytes, FdDecodeResult> decoder) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
    }

    public DesignerPairRenameResult prepare(
            OriginalFdBytes originalFd,
            String originalDartFile,
            String targetDartFile) {
        Objects.requireNonNull(originalFd, "originalFd");
        if (!validDartFile(originalDartFile)) {
            return rejected(
                    DesignerPairRenameResult.Code.INVALID_ORIGINAL_FILENAME,
                    "/source/dartFile",
                    "The current Dart filename is outside the schema-v1 contract.");
        }
        if (!validDartFile(targetDartFile)) {
            return rejected(
                    DesignerPairRenameResult.Code.INVALID_TARGET_FILENAME,
                    "/source/dartFile",
                    "The target Dart filename must be canonical lower_snake_case.dart.");
        }
        if (originalDartFile.equals(targetDartFile)) {
            return rejected(
                    DesignerPairRenameResult.Code.SAME_FILENAME,
                    "/source/dartFile",
                    "The target Dart filename is unchanged.");
        }

        FdDecodeResult decoded = decoder.apply(originalFd);
        if (!(decoded instanceof FdDecodeResult.Current current)) {
            return rejected(
                    DesignerPairRenameResult.Code.MODEL_NOT_CURRENT,
                    "",
                    "Only a valid current-version Flutter Designer model can be renamed.");
        }
        if (current.migrated()) {
            return rejected(
                    DesignerPairRenameResult.Code.MIGRATION_REQUIRED,
                    "",
                    "Rename cannot silently persist a schema migration.");
        }

        DesignerDocument originalDocument = current.document();
        DartSourceDescriptor originalSource = originalDocument.source();
        if (!originalSource.dartFile().equals(originalDartFile)) {
            return rejected(
                    DesignerPairRenameResult.Code.SOURCE_REFERENCE_MISMATCH,
                    "/source/dartFile",
                    "The model does not name the exact current Dart file.");
        }

        DartSourceDescriptor targetSource = new DartSourceDescriptor(
                targetDartFile,
                originalSource.className(),
                originalSource.widgetKind(),
                originalSource.generatorVersion(),
                originalSource.managedRegions());
        DesignerDocument targetDocument = new DesignerDocument(
                originalDocument.schemaReference(),
                originalDocument.documentId(),
                targetSource,
                originalDocument.canvas(),
                originalDocument.root(),
                originalDocument.extensions());

        OriginalFdBytes targetFd;
        try {
            targetFd = codec.encode(targetDocument);
        } catch (FdEncodeException failure) {
            return rejected(
                    DesignerPairRenameResult.Code.ENCODE_FAILED,
                    failure.diagnostic().pointer(),
                    "The renamed Flutter Designer model could not be encoded: "
                    + failure.diagnostic().message());
        }

        FdDecodeResult roundTrip = codec.decode(targetFd);
        if (!(roundTrip instanceof FdDecodeResult.Current verified)
                || verified.migrated()
                || !verified.document().equals(targetDocument)) {
            return rejected(
                    DesignerPairRenameResult.Code.ROUND_TRIP_MISMATCH,
                    "",
                    "The renamed model did not pass an exact canonical round trip.");
        }
        return new DesignerPairRenameResult.Ready(new DesignerPairRenamePlan(
                originalDartFile,
                targetDartFile,
                originalDocument,
                targetDocument,
                originalFd,
                targetFd));
    }

    private static boolean validDartFile(String value) {
        return value != null && DART_FILE.matcher(value).matches();
    }

    private static DesignerPairRenameResult.Rejected rejected(
            DesignerPairRenameResult.Code code,
            String pointer,
            String message) {
        return new DesignerPairRenameResult.Rejected(code, pointer, message);
    }
}
