package io.github.vgrytsenko2022.designer.command;

import java.util.Objects;

/** One semantic action and its exact before/after revisions and inverse. */
public final class DesignerCommandEdit {
    private final DesignerCommand forward;
    private final DesignerCommandInverse inverse;
    private final DesignerCommandRevision beforeRevision;
    private final DesignerCommandRevision afterRevision;

    DesignerCommandEdit(
            DesignerCommand forward,
            DesignerCommandRevision beforeRevision,
            DesignerCommandRevision afterRevision) {
        this.forward = Objects.requireNonNull(forward, "forward");
        this.beforeRevision = Objects.requireNonNull(
                beforeRevision, "beforeRevision");
        this.afterRevision = Objects.requireNonNull(
                afterRevision, "afterRevision");
        if (beforeRevision.revisionId() == afterRevision.revisionId()) {
            throw new IllegalArgumentException(
                    "an applied edit must advance the revision identity");
        }
        this.inverse = new DesignerCommandInverse(
                afterRevision.document(), beforeRevision.document());
    }

    public DesignerCommand forward() {
        return forward;
    }

    public DesignerCommandInverse inverse() {
        return inverse;
    }

    public DesignerCommandRevision beforeRevision() {
        return beforeRevision;
    }

    public DesignerCommandRevision afterRevision() {
        return afterRevision;
    }
}
