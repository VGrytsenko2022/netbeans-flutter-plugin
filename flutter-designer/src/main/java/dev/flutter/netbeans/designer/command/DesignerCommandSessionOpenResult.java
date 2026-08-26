package dev.flutter.netbeans.designer.command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Fail-closed result of binding one exact durable pair to a command session. */
public final class DesignerCommandSessionOpenResult {
    private final DesignerCommandStatus status;
    private final Optional<DesignerCommandSession> session;
    private final List<DesignerCommandDiagnostic> diagnostics;

    DesignerCommandSessionOpenResult(
            DesignerCommandStatus status,
            Optional<DesignerCommandSession> session,
            List<DesignerCommandDiagnostic> diagnostics) {
        this.status = Objects.requireNonNull(status, "status");
        this.session = Objects.requireNonNull(session, "session");
        this.diagnostics = List.copyOf(Objects.requireNonNull(
                diagnostics, "diagnostics"));
        if (this.diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("diagnostics contains null");
        }
        if ((status == DesignerCommandStatus.READY) != this.session.isPresent()) {
            throw new IllegalArgumentException("only READY may publish a session");
        }
        if (status == DesignerCommandStatus.READY && !this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException("READY cannot contain diagnostics");
        }
        if (status != DesignerCommandStatus.READY && this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "a failed open result must contain a diagnostic");
        }
    }

    public DesignerCommandStatus status() {
        return status;
    }

    public Optional<DesignerCommandSession> session() {
        return session;
    }

    public List<DesignerCommandDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean ready() {
        return status == DesignerCommandStatus.READY;
    }
}
