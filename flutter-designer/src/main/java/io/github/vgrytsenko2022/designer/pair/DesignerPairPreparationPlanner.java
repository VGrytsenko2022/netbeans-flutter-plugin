package io.github.vgrytsenko2022.designer.pair;

import io.github.vgrytsenko2022.designer.codec.FdCodecDiagnostic;
import io.github.vgrytsenko2022.designer.codec.FdCodecDiagnosticCode;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.FdEncodeException;
import io.github.vgrytsenko2022.designer.codec.OriginalFdBytes;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import io.github.vgrytsenko2022.designer.transition.DartSourceTransitionPlan;
import io.github.vgrytsenko2022.designer.transition.DartSourceTransitionResult;
import io.github.vgrytsenko2022.designer.transition.DartSourceTransitionStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure preparation of exact old and canonical prospective Flutter Designer
 * pair bytes. No method in this type performs I/O or authorizes persistence.
 */
public final class DesignerPairPreparationPlanner {
    private static final String SOURCE_PATH = "/source";
    private final FdDocumentCodec codec;

    public DesignerPairPreparationPlanner() {
        this(new FdDocumentCodec());
    }

    public DesignerPairPreparationPlanner(FdDocumentCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * Prepares one two-file candidate from a successful managed-source
     * transition.
     */
    public DesignerPairPreparationResult prepare(
            OriginalFdBytes baselineFd,
            DesignerDocument prospectiveDocument,
            DartSourceTransitionPlan dartTransition) {
        Objects.requireNonNull(dartTransition, "dartTransition");
        return prepareReadyTransition(
                Objects.requireNonNull(baselineFd, "baselineFd"),
                Objects.requireNonNull(prospectiveDocument, "prospectiveDocument"),
                dartTransition);
    }

    /**
     * Fail-closed convenience entry point for callers that still hold the
     * aggregate transition result. Only {@code READY} can prepare a pair.
     */
    public DesignerPairPreparationResult prepare(
            OriginalFdBytes baselineFd,
            DesignerDocument prospectiveDocument,
            DartSourceTransitionResult transitionResult) {
        Objects.requireNonNull(baselineFd, "baselineFd");
        Objects.requireNonNull(prospectiveDocument, "prospectiveDocument");
        Objects.requireNonNull(transitionResult, "transitionResult");
        if (!transitionResult.ready()) {
            return failure(
                    statusForTransition(transitionResult.status()),
                    DesignerPairPreparationDiagnosticCode.SOURCE_TRANSITION_REQUIRED,
                    SOURCE_PATH,
                    "A prepared two-file pair requires a READY Dart source "
                    + "transition; received " + transitionResult.status() + ".");
        }
        return prepareReadyTransition(
                baselineFd,
                prospectiveDocument,
                transitionResult.plan().orElseThrow());
    }

    private DesignerPairPreparationResult prepareReadyTransition(
            OriginalFdBytes baselineFd,
            DesignerDocument prospectiveDocument,
            DartSourceTransitionPlan dartTransition) {
        if (baselineFd.size() > codec.limits().maxDocumentBytes()) {
            return failure(
                    DesignerPairPreparationStatus.UNAVAILABLE,
                    DesignerPairPreparationDiagnosticCode.BASELINE_FD_TOO_LARGE,
                    "",
                    "The exact baseline .fd snapshot contains " + baselineFd.size()
                    + " bytes, exceeding this planner's "
                    + codec.limits().maxDocumentBytes() + " byte limit.");
        }

        FdDecodeResult decoded = codec.decode(baselineFd);
        if (decoded instanceof FdDecodeResult.UnsupportedNewer unsupported) {
            return failure(
                    DesignerPairPreparationStatus.UNSUPPORTED,
                    DesignerPairPreparationDiagnosticCode.BASELINE_FD_UNSUPPORTED,
                    "/schemaVersion",
                    "The exact baseline .fd uses unsupported schema version "
                    + unsupported.declaredSchemaVersion() + ".");
        }
        if (decoded instanceof FdDecodeResult.Invalid invalid) {
            FdCodecDiagnostic primary = invalid.diagnostics().getFirst();
            return failure(
                    statusForCodecDiagnostic(primary.code()),
                    DesignerPairPreparationDiagnosticCode.BASELINE_FD_INVALID,
                    primary.pointer(),
                    "The exact baseline .fd failed " + primary.code() + ": "
                    + primary.message());
        }

        FdDecodeResult.Current current = (FdDecodeResult.Current) decoded;
        DesignerDocument baselineDocument = current.document();
        DartSourceDescriptor expectedBaseline = expectedBaselineDescriptor(dartTransition);
        if (!baselineDocument.source().equals(expectedBaseline)) {
            return failure(
                    DesignerPairPreparationStatus.CONFLICT,
                    DesignerPairPreparationDiagnosticCode.BASELINE_DESCRIPTOR_MISMATCH,
                    SOURCE_PATH,
                    "The exact baseline .fd source descriptor does not match "
                    + "the descriptor proven by the Dart transition baseline.");
        }
        if (!baselineDocument.documentId().equals(prospectiveDocument.documentId())) {
            return failure(
                    DesignerPairPreparationStatus.CONFLICT,
                    DesignerPairPreparationDiagnosticCode.DOCUMENT_ID_MISMATCH,
                    "/documentId",
                    "The prospective document does not retain the exact "
                    + "baseline documentId.");
        }
        if (!prospectiveDocument.source().equals(
                dartTransition.prospectiveDescriptor())) {
            return failure(
                    DesignerPairPreparationStatus.CONFLICT,
                    DesignerPairPreparationDiagnosticCode
                            .PROSPECTIVE_DESCRIPTOR_MISMATCH,
                    SOURCE_PATH,
                    "The prospective document source descriptor does not equal "
                    + "the descriptor produced by the Dart transition.");
        }

        OriginalFdBytes prospectiveFd;
        try {
            prospectiveFd = codec.encode(prospectiveDocument);
        } catch (FdEncodeException failure) {
            return failure(
                    statusForCodecDiagnostic(failure.diagnostic().code()),
                    DesignerPairPreparationDiagnosticCode
                            .PROSPECTIVE_FD_ENCODING_FAILED,
                    failure.diagnostic().pointer(),
                    "Canonical prospective .fd encoding failed "
                    + failure.diagnostic().code() + ": "
                    + failure.diagnostic().message());
        }
        if (prospectiveFd.equals(baselineFd)
                && dartTransition.userSourceProjection().isIdentity()) {
            return failure(
                    DesignerPairPreparationStatus.NO_TRANSITION,
                    DesignerPairPreparationDiagnosticCode.PROSPECTIVE_FD_UNCHANGED,
                    "",
                    "The prospective .fd bytes equal the exact baseline; no "
                    + "two-file transition can be prepared.");
        }

        Optional<DesignerPairPreparationDiagnostic> candidateFailure =
                verifyCanonicalCandidate(prospectiveFd, prospectiveDocument);
        if (candidateFailure.isPresent()) {
            return new DesignerPairPreparationResult(
                    DesignerPairPreparationStatus.CONFLICT,
                    Optional.empty(),
                    List.of(candidateFailure.orElseThrow()));
        }

        PreparedDesignerPair prepared = new PreparedDesignerPair(
                baselineFd,
                baselineDocument,
                prospectiveDocument,
                prospectiveFd,
                dartTransition);
        return new DesignerPairPreparationResult(
                DesignerPairPreparationStatus.READY,
                Optional.of(prepared),
                List.of());
    }

    private Optional<DesignerPairPreparationDiagnostic> verifyCanonicalCandidate(
            OriginalFdBytes prospectiveFd,
            DesignerDocument prospectiveDocument) {
        FdDecodeResult decoded = codec.decode(prospectiveFd);
        if (!(decoded instanceof FdDecodeResult.Current current)
                || current.migrated()
                || !current.original().equals(prospectiveFd)
                || !current.document().equals(prospectiveDocument)) {
            return Optional.of(diagnostic(
                    DesignerPairPreparationDiagnosticCode.PROSPECTIVE_FD_INVALID,
                    "",
                    "The canonical prospective .fd did not decode back to the "
                    + "exact prospective document."));
        }
        try {
            if (!codec.encode(current.document()).equals(prospectiveFd)) {
                return Optional.of(diagnostic(
                        DesignerPairPreparationDiagnosticCode.PROSPECTIVE_FD_INVALID,
                        "",
                        "The canonical prospective .fd was not a deterministic "
                        + "decode/encode fixed point."));
            }
        } catch (FdEncodeException failure) {
            return Optional.of(diagnostic(
                    DesignerPairPreparationDiagnosticCode.PROSPECTIVE_FD_INVALID,
                    failure.diagnostic().pointer(),
                    "The decoded prospective .fd could not be encoded again: "
                    + failure.diagnostic().message()));
        }
        return Optional.empty();
    }

    private static DartSourceDescriptor expectedBaselineDescriptor(
            DartSourceTransitionPlan transition) {
        DartSourceDescriptor prospective = transition.prospectiveDescriptor();
        String importsHash = transition.baseline()
                .comparison(DartSourceIntegrityScanner.IMPORTS_REGION)
                .orElseThrow()
                .declaredNormalizedSha256();
        String buildHash = transition.baseline()
                .comparison(DartSourceIntegrityScanner.BUILD_REGION)
                .orElseThrow()
                .declaredNormalizedSha256();
        return new DartSourceDescriptor(
                prospective.dartFile(),
                prospective.className(),
                prospective.widgetKind(),
                prospective.generatorVersion(),
                new ManagedRegions(
                        new ManagedRegion(importsHash),
                        new ManagedRegion(buildHash)));
    }

    private static DesignerPairPreparationStatus statusForTransition(
            DartSourceTransitionStatus status) {
        return switch (status) {
            case READY -> throw new IllegalArgumentException(
                    "READY transition must publish a plan");
            case NO_CHANGES -> DesignerPairPreparationStatus.NO_TRANSITION;
            case CONFLICT -> DesignerPairPreparationStatus.CONFLICT;
            case UNSUPPORTED -> DesignerPairPreparationStatus.UNSUPPORTED;
            case UNAVAILABLE -> DesignerPairPreparationStatus.UNAVAILABLE;
        };
    }

    private static DesignerPairPreparationStatus statusForCodecDiagnostic(
            FdCodecDiagnosticCode code) {
        return switch (code) {
            case RESOURCE_LIMIT -> DesignerPairPreparationStatus.UNAVAILABLE;
            case UNSUPPORTED_FORMAT, UNSUPPORTED_OLDER_VERSION ->
                DesignerPairPreparationStatus.UNSUPPORTED;
            case MALFORMED_UTF8,
                    MALFORMED_JSON,
                    DUPLICATE_FIELD,
                    TRAILING_CONTENT,
                    ROOT_MUST_BE_OBJECT,
                    MISSING_REQUIRED_FIELD,
                    UNKNOWN_FIELD,
                    WRONG_VALUE_TYPE,
                    INVALID_VALUE,
                    NUMBER_RANGE -> DesignerPairPreparationStatus.CONFLICT;
        };
    }

    private static DesignerPairPreparationResult failure(
            DesignerPairPreparationStatus status,
            DesignerPairPreparationDiagnosticCode code,
            String path,
            String message) {
        return new DesignerPairPreparationResult(
                status,
                Optional.empty(),
                List.of(diagnostic(code, path, message)));
    }

    private static DesignerPairPreparationDiagnostic diagnostic(
            DesignerPairPreparationDiagnosticCode code,
            String path,
            String message) {
        return new DesignerPairPreparationDiagnostic(code, path, message);
    }
}
