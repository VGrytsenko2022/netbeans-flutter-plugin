package dev.flutter.netbeans.designer.command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of apply, Undo or Redo; failures retain the exact input session. */
public final class DesignerCommandSessionResult {
    private final DesignerCommandStatus status;
    private final DesignerCommandSession session;
    private final Optional<DesignerCommandEdit> edit;
    private final List<DesignerCommandDiagnostic> diagnostics;

    DesignerCommandSessionResult(
            DesignerCommandStatus status,
            DesignerCommandSession session,
            Optional<DesignerCommandEdit> edit,
            List<DesignerCommandDiagnostic> diagnostics) {
        this.status = Objects.requireNonNull(status, "status");
        this.session = Objects.requireNonNull(session, "session");
        this.edit = Objects.requireNonNull(edit, "edit");
        this.diagnostics = List.copyOf(Objects.requireNonNull(
                diagnostics, "diagnostics"));
        if (this.diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("diagnostics contains null");
        }
        boolean changed = status == DesignerCommandStatus.APPLIED
                || status == DesignerCommandStatus.UNDONE
                || status == DesignerCommandStatus.REDONE;
        if (changed != this.edit.isPresent()) {
            throw new IllegalArgumentException(
                    "only a changed result must publish its exact edit");
        }
        if (changed && !this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "a successful changed result cannot contain diagnostics");
        }
        if (!changed && this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "an unchanged result must explain why it was unchanged");
        }
    }

    public DesignerCommandStatus status() {
        return status;
    }

    public DesignerCommandSession session() {
        return session;
    }

    public Optional<DesignerCommandEdit> edit() {
        return edit;
    }

    public List<DesignerCommandDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean changed() {
        return edit.isPresent();
    }
}
