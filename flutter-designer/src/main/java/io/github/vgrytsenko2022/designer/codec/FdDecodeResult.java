package io.github.vgrytsenko2022.designer.codec;

import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Closed result of decoding an exact, bounded {@code .fd} byte snapshot. */
public sealed interface FdDecodeResult permits
        FdDecodeResult.Current,
        FdDecodeResult.UnsupportedNewer,
        FdDecodeResult.Invalid {

    OriginalFdBytes original();

    /**
     * A structurally valid current document. Later catalog, tree and source
     * integrity gates still decide whether an editor may save it.
     */
    record Current(
            DesignerDocument document,
            int sourceSchemaVersion,
            boolean migrated,
            OriginalFdBytes original) implements FdDecodeResult {
        public Current {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(original, "original");
            if (sourceSchemaVersion <= 0 || sourceSchemaVersion > DesignerDocument.SCHEMA_VERSION) {
                throw new IllegalArgumentException("sourceSchemaVersion is outside the supported range");
            }
            if (!migrated && sourceSchemaVersion != DesignerDocument.SCHEMA_VERSION) {
                throw new IllegalArgumentException(
                        "A non-migrated result must use the current schema version");
            }
        }
    }

    /** A fully parsed but unsupported future version; only raw read-only access is safe. */
    record UnsupportedNewer(
            BigInteger declaredSchemaVersion,
            OriginalFdBytes original) implements FdDecodeResult {
        public UnsupportedNewer {
            Objects.requireNonNull(declaredSchemaVersion, "declaredSchemaVersion");
            Objects.requireNonNull(original, "original");
            if (declaredSchemaVersion.compareTo(
                    BigInteger.valueOf(DesignerDocument.SCHEMA_VERSION)) <= 0) {
                throw new IllegalArgumentException("declaredSchemaVersion must be newer than supported");
            }
        }
    }

    /** Invalid syntax, envelope, current-version structure or resource usage. */
    record Invalid(
            Optional<BigInteger> declaredSchemaVersion,
            List<FdCodecDiagnostic> diagnostics,
            OriginalFdBytes original) implements FdDecodeResult {
        public Invalid {
            Objects.requireNonNull(declaredSchemaVersion, "declaredSchemaVersion");
            Objects.requireNonNull(diagnostics, "diagnostics");
            Objects.requireNonNull(original, "original");
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.isEmpty()) {
                throw new IllegalArgumentException("diagnostics must not be empty");
            }
        }
    }
}
