package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.openide.awt.UndoRedo;
import org.openide.text.CloneableEditor;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponent;

/**
 * Plugin-owned CES pane that cannot enter NetBeans' implicit Splitable paths.
 *
 * <p>This is a dormant phase-one shell. It intentionally uses
 * {@link TopComponent#PERSISTENCE_NEVER} until restart reconstruction and the
 * exact clone-safe Canvas close permit are covered by runtime tests.</p>
 */
final class FlutterDesignerCloneableEditor extends CloneableEditor {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerCloneableEditor.class.getName());

    private final FlutterDesignerEditorSupport editorSupport;
    private final FlutterDesignerDataObject dataObject;
    private final ActiveLookup activeLookup;
    private final FlutterDesignerEditorClosePermitCoordinator closePermits;
    private FlutterDesignerEditorPaneContent content;
    private FlutterDesignerEditorClosePermitCoordinator.Permit closePermit;
    private FlutterDesignerEditorClosePermitCoordinator.Permit closeInvocationPermit;
    private boolean componentOpening;
    private boolean componentOpen;
    private boolean componentShowing;
    private boolean componentActive;
    private boolean sourceRequested;

    FlutterDesignerCloneableEditor(FlutterDesignerEditorSupport editorSupport) {
        super(Objects.requireNonNull(editorSupport, "editorSupport"), false);
        this.editorSupport = editorSupport;
        dataObject = (FlutterDesignerDataObject) editorSupport.getDataObject();
        closePermits = editorSupport.editorClosePermits();
        activeLookup = new ActiveLookup(
                dataObject.getLookup(), Lookups.singleton(getActionMap()));
        associateLookup(activeLookup);
        // These stock window-system operations may reparent the heavyweight
        // AWT subtree without consulting canClose(). Keep every such UI path
        // unavailable until physical runtime tests prove a safe transition.
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DND_COPY_DISABLED, Boolean.TRUE);
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    protected CloneableTopComponent createClonedObject() {
        return editorSupport.createDedicatedCloneComponent();
    }

    @Override
    protected void addImpl(Component component, Object constraints, int index) {
        if (content == null && containsSourceEditor(component)) {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(dataObject.getLookup());
            FlutterDesignerEditorPaneContent created =
                    new FlutterDesignerEditorPaneContent(
                            component,
                            new FlutterDesignerEditorPerspective.Design(design),
                            this::requestCloseRetry,
                            this::canvasCloseFailed,
                            ignored -> perspectiveChanged());
            content = created;
            super.addImpl(created, constraints, index);
            if (sourceRequested) {
                created.selectSource();
            }
            perspectiveChanged();
            synchronizeContentLifecycle();
            return;
        }
        if (content != null
                && component instanceof JToolBar toolbar
                && BorderLayout.NORTH.equals(constraints)) {
            content.setSourceToolbar(toolbar);
            return;
        }
        super.addImpl(component, constraints, index);
    }

    @Override
    protected void componentOpened() {
        componentOpening = true;
        closePermits.editorShellOpenedOrClosing(this);
        // CloneableEditor fires PROP_OPENED_PANES synchronously from this call.
        // Register the support-level gate first so a reentrant CloseCookie
        // listener cannot enter the stock synchronous batch before the
        // dedicated shell is known to the coordinator.  Keep the gate if an
        // opened-panes listener throws: WindowManager catches that failure and
        // may still publish the physically open component. componentClosed()
        // is the only safe point that removes this fail-closed registration.
        try {
            super.componentOpened();
        } catch (RuntimeException | Error openingFailure) {
            // WindowManager logs listener failures and can still publish this
            // TopComponent as open. Complete our own Canvas lifecycle before
            // propagating; if that completion also fails, componentOpening
            // deliberately remains true and every ordinary close stays vetoed.
            try {
                completeComponentOpened();
            } catch (RuntimeException | Error completionFailure) {
                openingFailure.addSuppressed(completionFailure);
            }
            throw openingFailure;
        }
        completeComponentOpened();
    }

    private void completeComponentOpened() {
        componentOpen = true;
        synchronizeContentLifecycle();
        componentOpening = false;
    }

    @Override
    protected void componentShowing() {
        super.componentShowing();
        componentShowing = true;
        synchronizeContentLifecycle();
    }

    @Override
    protected void componentActivated() {
        super.componentActivated();
        componentActive = true;
        synchronizeContentLifecycle();
    }

    @Override
    protected void componentDeactivated() {
        FlutterDesignerEditorPaneContent current = content;
        if (current != null) {
            current.deactivated();
        }
        componentActive = false;
        super.componentDeactivated();
    }

    @Override
    protected void componentHidden() {
        FlutterDesignerEditorPaneContent current = content;
        if (current != null) {
            current.hidden();
        }
        componentActive = false;
        componentShowing = false;
        super.componentHidden();
    }

    @Override
    protected void componentClosed() {
        // Keep the support-level close gate present even for an unexpected
        // direct componentClosed callback that bypassed canClose().
        closePermits.editorShellOpenedOrClosing(this);
        FlutterDesignerEditorClosePermitCoordinator.Permit admittedPermit =
                closePermit;
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope
                closeScope = admittedPermit == null
                        ? null : closePermits.enterComponentClosed(
                                admittedPermit, this);
        try {
            FlutterDesignerEditorPaneContent current = content;
            try {
                if (current != null) {
                    current.closed();
                }
            } finally {
                try {
                    if (current != null) {
                        remove(current);
                        content = null;
                        activeLookup.setPerspective(Lookup.EMPTY);
                    }
                } finally {
                    componentActive = false;
                    componentShowing = false;
                    componentOpening = false;
                    componentOpen = false;
                    sourceRequested = false;
                    // CES clone-registry teardown must run even if a
                    // perspective lifecycle callback fails during shutdown.
                    super.componentClosed();
                }
            }
        } finally {
            try {
                if (closeScope != null) {
                    closeScope.close();
                }
            } finally {
                closePermits.editorShellClosed(this);
                if (closeScope == null
                        || !closePermits.completeAdmitted(admittedPermit)) {
                    closePermits.releaseOwner(this);
                    LOGGER.log(Level.SEVERE,
                            "Operation: finalize Flutter Designer editor close. "
                            + "Target: {0}. Reason: componentClosed was reached "
                            + "without the exact admitted close permit.",
                            dataObject.getPrimaryFile().getPath());
                }
                closePermit = null;
                closeInvocationPermit = null;
            }
        }
    }

    @Override
    public boolean canClose() {
        if (!isOpened() || componentOpening) {
            return false;
        }
        FlutterDesignerEditorClosePermitCoordinator.Permit previousPermit =
                closePermit;
        FlutterDesignerEditorClosePermitCoordinator.Admission admission =
                closePermits.acquire(
                        this,
                        this::cloneTopology,
                        editorSupport::editorShellDocumentRevision,
                        editorSupport::confirmEditorShellDocumentClose);
        if (!admission.authorized()) {
            if (previousPermit != null
                    && admission.status()
                            == FlutterDesignerEditorClosePermitCoordinator
                                    .AdmissionStatus.STALE) {
                abandonClosePermit(previousPermit);
            }
            return false;
        }
        FlutterDesignerEditorClosePermitCoordinator.Permit permit =
                admission.permit();
        closePermit = permit;
        FlutterDesignerEditorPaneContent current = content;
        FlutterDesignerEditorPerspective.CloseBarrierState barrierState =
                FlutterDesignerEditorPerspective.CloseBarrierState.NOT_REQUIRED;
        try {
            if (current != null) {
                barrierState = current.closeBarrierState(permit.sequence());
            }
        } catch (RuntimeException | Error failure) {
            abandonClosePermit(permit);
            throw failure;
        }

        switch (barrierState) {
            case OPEN -> {
                try {
                    current.beginCloseBarrier(permit.sequence());
                } catch (RuntimeException | Error failure) {
                    abandonClosePermit(permit);
                    throw failure;
                }
                return false;
            }
            case PENDING -> {
                return false;
            }
            case STALE -> {
                abandonClosePermit(permit);
                return false;
            }
            case NOT_REQUIRED, READY -> {
                return commitClose(permit);
            }
        }
        throw new AssertionError("Unhandled Canvas close-barrier state");
    }

    @Override
    protected boolean closeLast() {
        FlutterDesignerEditorClosePermitCoordinator.Permit permit =
                closeInvocationPermit;
        if (permit == null
                || !closePermits.consumeLastClose(
                        permit,
                        cloneTopology(),
                        editorSupport.editorShellDocumentRevision())) {
            // Never fall back to CloneableEditor's ordinary closeLast(). It
            // would ask the dirty-document question after Canvas retirement.
            LOGGER.log(Level.SEVERE,
                    "Operation: close the final Flutter Designer editor clone. "
                    + "Target: {0}. Reason: the one-shot document close "
                    + "permit was absent or stale.",
                    dataObject.getPrimaryFile().getPath());
            return false;
        }
        return closeLast(false);
    }

    private boolean commitClose(
            FlutterDesignerEditorClosePermitCoordinator.Permit permit) {
        final FlutterDesignerEditorClosePermitCoordinator.CloneTopology topology;
        final FlutterDesignerEditorClosePermitCoordinator.DocumentRevision revision;
        try {
            topology = cloneTopology();
            revision = editorSupport.editorShellDocumentRevision();
        } catch (RuntimeException | Error failure) {
            abandonClosePermit(permit);
            throw failure;
        }
        if (!closePermits.beginCommit(permit, topology, revision)) {
            abandonClosePermit(permit);
            return false;
        }

        boolean admitted = false;
        closeInvocationPermit = permit;
        try {
            if (!super.canClose()) {
                return false;
            }
            if (!closePermits.markAdmitted(permit)) {
                throw new IllegalStateException(
                        "The Flutter Designer close permit was lost after "
                        + "NetBeans admitted the editor close");
            }
            admitted = true;
            return true;
        } finally {
            closeInvocationPermit = null;
            if (!admitted) {
                closePermits.abortCommit(permit);
                abandonClosePermit(permit);
            }
        }
    }

    private void abandonClosePermit(
            FlutterDesignerEditorClosePermitCoordinator.Permit permit) {
        closePermits.revoke(permit);
        FlutterDesignerEditorPaneContent current = content;
        try {
            if (current != null) {
                current.abandonCloseBarrier(permit.sequence());
            }
        } finally {
            if (closePermit == permit) {
                closePermit = null;
            }
        }
    }

    private FlutterDesignerEditorClosePermitCoordinator.CloneTopology
            cloneTopology() {
        List<Object> members = new ArrayList<>();
        var components = getReference().getComponents();
        while (components.hasMoreElements()) {
            members.add(components.nextElement());
        }
        return new FlutterDesignerEditorClosePermitCoordinator.CloneTopology(
                members);
    }

    @Override
    public UndoRedo getUndoRedo() {
        return dataObject.getCombinedUndoRedo();
    }

    @Override
    public Action[] getActions() {
        Action[] inherited = super.getActions();
        FlutterDesignerEditorPaneContent current = content;
        if (current == null
                || current.perspective()
                        != FlutterDesignerEditorPaneContent.Perspective.DESIGN) {
            return inherited;
        }
        List<Action> merged = new ArrayList<>(Arrays.asList(inherited));
        Action[] designActions = current.activeActions();
        if (designActions.length > 0) {
            merged.add(null);
            merged.addAll(Arrays.asList(designActions));
        }
        return merged.toArray(Action[]::new);
    }

    @Override
    public void ensureVisible() {
        sourceRequested = true;
        FlutterDesignerEditorPaneContent current = content;
        if (current != null) {
            current.selectSource();
        }
        super.ensureVisible();
    }

    @Override
    public boolean requestFocusInWindow() {
        FlutterDesignerEditorPaneContent current = content;
        if (current != null && current.requestPerspectiveFocus()) {
            return true;
        }
        return super.requestFocusInWindow();
    }

    FlutterDesignerEditorPaneContent editorContent() {
        return content;
    }

    private boolean containsSourceEditor(Component component) {
        return pane != null
                && component != pane
                && SwingUtilities.isDescendingFrom(pane, component);
    }

    private void synchronizeContentLifecycle() {
        FlutterDesignerEditorPaneContent current = content;
        if (current == null) {
            return;
        }
        if (componentOpen) {
            current.opened();
        }
        if (componentShowing) {
            current.showing();
        }
        if (componentActive) {
            current.activated();
        }
    }

    private void perspectiveChanged() {
        FlutterDesignerEditorPaneContent current = content;
        activeLookup.setPerspective(current == null
                ? Lookup.EMPTY : current.activeLookup());
    }

    private void requestCloseRetry(long attemptId) {
        dispatchCloseCallback(() -> retryClose(attemptId));
    }

    private void retryClose(long attemptId) {
        FlutterDesignerEditorClosePermitCoordinator.Permit permit = closePermit;
        if (permit == null || permit.sequence() != attemptId) {
            return;
        }
        try {
            if (!closePermits.validate(
                    permit,
                    cloneTopology(),
                    editorSupport.editorShellDocumentRevision())) {
                abandonClosePermit(permit);
                return;
            }
            close();
        } catch (RuntimeException | Error failure) {
            abandonClosePermit(permit);
            LOGGER.log(Level.WARNING,
                    "Operation: retry Flutter Designer editor close. Target: "
                    + dataObject.getPrimaryFile().getPath()
                    + ". Reason: the tokenized close retry failed.",
                    failure);
        }
    }

    private void canvasCloseFailed(long attemptId, Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        dispatchCloseCallback(() -> {
            FlutterDesignerEditorClosePermitCoordinator.Permit permit =
                    closePermit;
            if (permit == null || permit.sequence() != attemptId) {
                return;
            }
            abandonClosePermit(permit);
            LOGGER.log(Level.WARNING,
                    "Operation: retire Flutter Designer Canvas before editor "
                    + "close. Target: "
                    + dataObject.getPrimaryFile().getPath()
                    + ". Reason: Canvas retirement failed; the editor remains open.",
                    failure);
        });
    }

    private static void dispatchCloseCallback(Runnable callback) {
        if (SwingUtilities.isEventDispatchThread()) {
            callback.run();
        } else {
            SwingUtilities.invokeLater(callback);
        }
    }

    private static final class ActiveLookup extends ProxyLookup {
        private final Lookup editorLookup;
        private final Lookup actionMapLookup;

        ActiveLookup(Lookup editorLookup, Lookup actionMapLookup) {
            super(
                    Objects.requireNonNull(editorLookup, "editorLookup"),
                    Objects.requireNonNull(actionMapLookup, "actionMapLookup"));
            this.editorLookup = editorLookup;
            this.actionMapLookup = actionMapLookup;
        }

        void setPerspective(Lookup perspectiveLookup) {
            Lookup admitted = Objects.requireNonNull(
                    perspectiveLookup, "perspectiveLookup");
            if (admitted == Lookup.EMPTY) {
                setLookups(editorLookup, actionMapLookup);
            } else {
                setLookups(editorLookup, actionMapLookup, admitted);
            }
        }
    }
}
