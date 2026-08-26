package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.DesignerDocument;
import java.util.Objects;

/**
 * Exact snapshot inverse for one successfully applied semantic command.
 *
 * <p>The inverse is deliberately conditional: it restores the exact prior
 * document only when applied to the exact document produced by the command.
 * It cannot be used as a lossy best-effort edit against another revision.</p>
 */
public final class DesignerCommandInverse {
    private final DesignerDocument expectedCurrent;
    private final DesignerDocument restored;

    DesignerCommandInverse(
            DesignerDocument expectedCurrent,
            DesignerDocument restored) {
        this.expectedCurrent = Objects.requireNonNull(
                expectedCurrent, "expectedCurrent");
        this.restored = Objects.requireNonNull(restored, "restored");
    }

    public DesignerDocument expectedCurrent() {
        return expectedCurrent;
    }

    public DesignerDocument restored() {
        return restored;
    }

    public boolean canApplyTo(DesignerDocument current) {
        return expectedCurrent.equals(Objects.requireNonNull(current, "current"));
    }

    public DesignerDocument applyTo(DesignerDocument current) {
        if (!canApplyTo(current)) {
            throw new IllegalStateException(
                    "Exact designer command inverse does not match the current revision");
        }
        return restored;
    }
}
