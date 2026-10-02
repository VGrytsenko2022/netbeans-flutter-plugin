package io.github.vgrytsenko2022.designer.canvas;

import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.DesignerCommandRevision;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSession;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.util.Objects;

/**
 * Exact immutable Designer revision admitted for read-only Canvas rendering.
 *
 * <p>The public factory captures the current revision, catalog identity and
 * validation limits from one command session. Callers therefore cannot pair a
 * logical revision id with a detached document or a substitute catalog. The
 * snapshot carries validation evidence only; it grants no command, file,
 * persistence or Undo/Redo authority.</p>
 */
public final class ValidatedCanvasRevisionSnapshot {
    private final long logicalRevisionId;
    private final DesignerDocument document;
    private final WidgetCatalog catalog;
    private final ValidationResult validation;

    private ValidatedCanvasRevisionSnapshot(
            long logicalRevisionId,
            DesignerDocument document,
            WidgetCatalog catalog,
            ValidationResult validation) {
        this.logicalRevisionId = logicalRevisionId;
        this.document = document;
        this.catalog = catalog;
        this.validation = validation;
    }

    /** Captures and revalidates the exact current revision of {@code session}. */
    public static ValidatedCanvasRevisionSnapshot capture(
            DesignerCommandSession session) {
        Objects.requireNonNull(session, "session");
        DesignerCommandRevision revision = session.current();
        return validate(
                revision.revisionId(),
                revision.document(),
                session.catalog(),
                session.limits().validationLimits());
    }

    /**
     * Revalidates one immutable read-only document snapshot at an integration
     * edge that does not yet own a {@link DesignerCommandSession}.
     *
     * <p>This factory is intended for the initial disk-backed Design view. It
     * deliberately grants no mutation, persistence or Undo/Redo authority;
     * callers must still issue a fresh Canvas presentation identity for every
     * publication. Once the editable command session owns the document, use
     * {@link #capture(DesignerCommandSession)} instead.</p>
     */
    public static ValidatedCanvasRevisionSnapshot captureReadOnly(
            long logicalRevisionId,
            DesignerDocument document,
            WidgetCatalog catalog,
            ValidationLimits limits) {
        return validate(logicalRevisionId, document, catalog, limits);
    }

    /* Package-private for pure contract fixtures in this package. */
    static ValidatedCanvasRevisionSnapshot validate(
            long logicalRevisionId,
            DesignerDocument document,
            WidgetCatalog catalog,
            ValidationLimits limits) {
        if (logicalRevisionId < 0) {
            throw new IllegalArgumentException(
                    "logicalRevisionId must not be negative");
        }
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(limits, "limits");
        ValidationResult validation = new WidgetTreeValidator(limits)
                .validate(document, catalog);
        if (!validation.valid()) {
            throw new IllegalArgumentException(
                    "Canvas revision must be valid for its exact widget catalog: "
                    + validation.errors().getFirst().code());
        }
        return new ValidatedCanvasRevisionSnapshot(
                logicalRevisionId,
                document,
                catalog,
                validation);
    }

    public long logicalRevisionId() {
        return logicalRevisionId;
    }

    public DesignerDocument document() {
        return document;
    }

    /** Exact immutable catalog instance used to validate this snapshot. */
    public WidgetCatalog catalog() {
        return catalog;
    }

    public ValidationResult validation() {
        return validation;
    }
}
