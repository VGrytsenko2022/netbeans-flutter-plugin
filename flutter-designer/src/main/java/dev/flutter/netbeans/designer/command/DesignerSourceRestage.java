package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import java.util.Objects;

/**
 * Pure, exact Source-edit candidate above one retained logical revision. This is
 * not analyzer evidence or write authority; the native owner must analyze and
 * accept the exact observed live snapshot before adopting {@link #acceptedSession()}.
 */
public final class DesignerSourceRestage {
    private final DesignerCommandSession originalSession;
    private final DesignerCommandSession acceptedSession;
    private final DesignerCommandRevision physicalRevision;
    private final PreparedDesignerPair predecessorPair;

    DesignerSourceRestage(DesignerCommandSession originalSession, DesignerCommandSession acceptedSession,
            DesignerCommandRevision physicalRevision, PreparedDesignerPair predecessorPair) {
        this.originalSession = Objects.requireNonNull(originalSession);
        this.acceptedSession = Objects.requireNonNull(acceptedSession);
        this.physicalRevision = Objects.requireNonNull(physicalRevision);
        this.predecessorPair = Objects.requireNonNull(predecessorPair);
    }

    public DesignerCommandRevision revision() { return originalSession.current(); }
    public DesignerCommandRevision physicalRevision() { return physicalRevision; }
    public PreparedDesignerPair predecessorPair() { return predecessorPair; }
    public PreparedDesignerPair preparedPair() { return physicalRevision.preparedPair().orElseThrow(); }
    public DesignerCommandSession acceptedSession() { return acceptedSession; }
    boolean belongsTo(DesignerCommandSession session) { return originalSession == session; }
}
