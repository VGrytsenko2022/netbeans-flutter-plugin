package dev.flutter.netbeans.designer.pair;

import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import java.util.Objects;

/**
 * Immutable exact-baseline and prospective-byte bundle for one two-file
 * transition.
 *
 * <p>This object is deliberately not write authorization. It contains no live
 * editor revision, analyzer proof, filesystem lock or save-time recheck.</p>
 */
public final class PreparedDesignerPair {
    private final OriginalFdBytes baselineFd;
    private final DesignerDocument baselineDocument;
    private final DesignerDocument prospectiveDocument;
    private final OriginalFdBytes prospectiveFd;
    private final DartSourceTransitionPlan dartTransition;

    PreparedDesignerPair(
            OriginalFdBytes baselineFd,
            DesignerDocument baselineDocument,
            DesignerDocument prospectiveDocument,
            OriginalFdBytes prospectiveFd,
            DartSourceTransitionPlan dartTransition) {
        this.baselineFd = Objects.requireNonNull(baselineFd, "baselineFd");
        this.baselineDocument = Objects.requireNonNull(
                baselineDocument, "baselineDocument");
        this.prospectiveDocument = Objects.requireNonNull(
                prospectiveDocument, "prospectiveDocument");
        this.prospectiveFd = Objects.requireNonNull(prospectiveFd, "prospectiveFd");
        this.dartTransition = Objects.requireNonNull(
                dartTransition, "dartTransition");

        if (!baselineDocument.documentId().equals(prospectiveDocument.documentId())) {
            throw new IllegalArgumentException(
                    "baseline and prospective documents must retain documentId");
        }
        if (!prospectiveDocument.source().equals(
                dartTransition.prospectiveDescriptor())) {
            throw new IllegalArgumentException(
                    "prospective document must retain the transition descriptor");
        }
        if (baselineFd.equals(prospectiveFd)) {
            throw new IllegalArgumentException(
                    "a prepared two-file transition must change the .fd bytes");
        }
    }

    public OriginalFdBytes baselineFd() {
        return baselineFd;
    }

    public DesignerDocument baselineDocument() {
        return baselineDocument;
    }

    public DesignerDocument prospectiveDocument() {
        return prospectiveDocument;
    }

    public OriginalFdBytes prospectiveFd() {
        return prospectiveFd;
    }

    public DartSourceTransitionPlan dartTransition() {
        return dartTransition;
    }

    public byte[] baselineFdBytes() {
        return baselineFd.copyBytes();
    }

    public byte[] prospectiveFdBytes() {
        return prospectiveFd.copyBytes();
    }

    public byte[] baselineDartBytes() {
        return dartTransition.baseline().original().orElseThrow().copyBytes();
    }

    /** Marker-bearing live source used to reconstruct the prospective Dart bytes. */
    public byte[] liveDartBytes() {
        return dartTransition.liveSource().original().orElseThrow().copyBytes();
    }

    public byte[] prospectiveDartBytes() {
        return dartTransition.candidateBytes();
    }
}
