package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.CatalogDiagnostic;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityResult;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityResult;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable UI-facing result of loading one bounded Flutter Designer model. */
public sealed interface FlutterDesignerDocumentState permits
        FlutterDesignerDocumentState.Idle,
        FlutterDesignerDocumentState.Loading,
        FlutterDesignerDocumentState.Current,
        FlutterDesignerDocumentState.UnsupportedNewer,
        FlutterDesignerDocumentState.Invalid,
        FlutterDesignerDocumentState.InputTooLarge,
        FlutterDesignerDocumentState.Failure {

    /** The design view has not requested its first model load yet. */
    record Idle() implements FlutterDesignerDocumentState {
    }

    /** A background load is in progress for the named model file. */
    record Loading(String modelFileName) implements FlutterDesignerDocumentState {
        public Loading {
            modelFileName = requireText(modelFileName, "modelFileName");
        }
    }

    /**
     * A current schema document decoded successfully. Catalog, pair, source
     * and read-only three-way facts remain separate so the UI can name the
     * exact reason editing is blocked.
     */
    record Current(
            FdDecodeResult.Current decoded,
            ValidationResult validation,
            WidgetCatalog catalog,
            List<CatalogDiagnostic> catalogDiagnostics,
            List<ContextIssue> contextIssues,
            Optional<DartSourceIntegrityResult> sourceIntegrity,
            Optional<DartThreeWayIntegrityResult> threeWayIntegrity)
            implements FlutterDesignerDocumentState {
        public Current {
            Objects.requireNonNull(decoded, "decoded");
            Objects.requireNonNull(validation, "validation");
            Objects.requireNonNull(catalog, "catalog");
            catalogDiagnostics = List.copyOf(Objects.requireNonNull(
                    catalogDiagnostics, "catalogDiagnostics"));
            contextIssues = List.copyOf(Objects.requireNonNull(
                    contextIssues, "contextIssues"));
            sourceIntegrity = Objects.requireNonNull(sourceIntegrity, "sourceIntegrity");
            threeWayIntegrity = Objects.requireNonNull(
                    threeWayIntegrity, "threeWayIntegrity");
            if (threeWayIntegrity.isPresent() && sourceIntegrity.isEmpty()) {
                throw new IllegalArgumentException(
                        "three-way integrity requires a Dart source-integrity result");
            }
            if (threeWayIntegrity.isPresent()
                    && threeWayIntegrity.orElseThrow().source()
                    != sourceIntegrity.orElseThrow()) {
                throw new IllegalArgumentException(
                        "three-way integrity must retain the same source-integrity snapshot");
            }
        }

    }

    /** A valid future document is exposed read-only with its exact byte snapshot. */
    record UnsupportedNewer(
            FdDecodeResult.UnsupportedNewer decoded)
            implements FlutterDesignerDocumentState {
        public UnsupportedNewer {
            Objects.requireNonNull(decoded, "decoded");
        }
    }

    /** Strict JSON, envelope or current-schema decoding failed. */
    record Invalid(FdDecodeResult.Invalid decoded)
            implements FlutterDesignerDocumentState {
        public Invalid {
            Objects.requireNonNull(decoded, "decoded");
        }
    }

    /** The edge rejected the file before allocating an unbounded byte array. */
    record InputTooLarge(
            String modelFileName,
            long observedBytes,
            int maximumBytes) implements FlutterDesignerDocumentState {
        public InputTooLarge {
            modelFileName = requireText(modelFileName, "modelFileName");
            if (maximumBytes <= 0 || observedBytes <= maximumBytes) {
                throw new IllegalArgumentException(
                        "observedBytes must exceed the positive maximumBytes");
            }
        }
    }

    /** The model could not be read or decoded because of an infrastructure failure. */
    record Failure(
            String operation,
            String target,
            String reason) implements FlutterDesignerDocumentState {
        public Failure {
            operation = requireText(operation, "operation");
            target = requireText(target, "target");
            reason = requireText(reason, "reason");
        }
    }

    /** One exact pairing/context problem outside the JSON wire contract. */
    record ContextIssue(String code, String path, String message) {
        public ContextIssue {
            code = requireText(code, "code");
            path = requireText(path, "path");
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("path must be an absolute model path");
            }
            message = requireText(message, "message");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
