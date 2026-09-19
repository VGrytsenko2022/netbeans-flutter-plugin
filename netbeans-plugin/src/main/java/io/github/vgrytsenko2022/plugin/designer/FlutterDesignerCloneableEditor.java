package io.github.vgrytsenko2022.plugin.designer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
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
    private static final String SAFE_CLOSE_ACTION_KEY =
            "flutter-designer.safe-close";
    private static final String SAFE_CLOSE_ALL_ACTION_KEY =
            "flutter-designer.safe-close-all";
    private static final int MAX_ADMITTED_PHYSICAL_CLOSE_RETRIES = 3;

    private final FlutterDesignerEditorSupport editorSupport;
    private final FlutterDesignerDataObject dataObject;
    private final ActiveLookup activeLookup;
    private final FlutterDesignerEditorClosePermitCoordinator closePermits;
    private final Action safeCloseAction = new AbstractAction(
            "Close Flutter Designer") {
        @Override
        public void actionPerformed(ActionEvent event) {
            close();
        }
    };
    private final Action safeCloseAllAction = new AbstractAction(
            "Close All Flutter Designer Views") {
        @Override
        public void actionPerformed(ActionEvent event) {
            editorSupport.close();
        }
    };
    private FlutterDesignerEditorPaneContent content;
    private FlutterDesignerEditorClosePermitCoordinator.Permit closePermit;
    private FlutterDesignerEditorClosePermitCoordinator.Permit closeInvocationPermit;
    private FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch
            supportCloseBatch;
    private FlutterDesignerEditorClosePermitCoordinator.LifecycleStamp
            lifecycleStamp;
    private int admittedPhysicalCloseRetries;
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
        // RELEASE300 Close Mode bypasses TopComponent.canClose() and checks
        // only this supported per-component latch. Keep it permanent and
        // expose the permit-aware action below for intentional single-shell
        // close. Programmatic TopComponent.close() still consults canClose().
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        int menuMask = System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT).startsWith("mac")
                        ? InputEvent.META_DOWN_MASK
                        : InputEvent.CTRL_DOWN_MASK;
        KeyStroke closeKey = KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask);
        safeCloseAction.putValue(Action.ACCELERATOR_KEY, closeKey);
        safeCloseAction.putValue(Action.SHORT_DESCRIPTION,
                "Close this Flutter Designer after its Canvas peer is safe");
        safeCloseAllAction.putValue(Action.SHORT_DESCRIPTION,
                "Close every clone of this Flutter Designer after each "
                + "Canvas peer is safe");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(closeKey, SAFE_CLOSE_ACTION_KEY);
        getActionMap().put(SAFE_CLOSE_ACTION_KEY, safeCloseAction);
        getActionMap().put(SAFE_CLOSE_ALL_ACTION_KEY, safeCloseAllAction);
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
        lifecycleStamp = closePermits.editorShellOpenedOrClosing(this);
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
        lifecycleStamp = closePermits.editorShellOpenedOrClosing(this);
        FlutterDesignerEditorClosePermitCoordinator.Permit admittedPermit =
                closePermit;
        FlutterDesignerEditorClosePermitCoordinator.LifecycleStamp
                closingLifecycleStamp = admittedPermit != null
                        && admittedPermit.ownerLifecycleStamp() != null
                                ? admittedPermit.ownerLifecycleStamp()
                                : lifecycleStamp;
        FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch =
                supportCloseBatch;
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
                    if (closeScope != null
                            && batch != null
                            && admittedPermit.lastClone()) {
                        // The production WindowManager normally reaches the
                        // support's internal close(false) through
                        // CloneableOpenSupportRedirector. The headless module
                        // WindowManager can notify componentClosed while its
                        // registry still reports this TopComponent as open and
                        // skips that callback. Drive the same idempotent path
                        // explicitly while the exact componentClosed scope is
                        // still active. A production duplicate is rejected by
                        // the coordinator after its one-shot completion.
                        editorSupport.close(false);
                    }
                }
            }
        } finally {
            try {
                if (closeScope != null) {
                    closeScope.close();
                }
            } finally {
                boolean retiredLifecycle = closePermits.editorShellClosed(
                        this, closingLifecycleStamp);
                if (retiredLifecycle
                        && lifecycleStamp == closingLifecycleStamp) {
                    lifecycleStamp = null;
                }
                boolean finalized = closeScope != null
                        && admittedPermit != null
                        && (batch == null
                            ? closePermits.completeAdmitted(admittedPermit)
                            : closePermits.completeSupportCloseOwner(
                                    batch, this, admittedPermit));
                String completionFailureReason = finalized
                        || batch == null
                        || admittedPermit == null
                                ? "componentClosed did not carry the exact "
                                        + "admitted close permit"
                                : closePermits
                                        .supportCloseOwnerCompletionFailureReason(
                                                batch, this, admittedPermit);
                if (!finalized) {
                    if (batch == null) {
                        closePermits.releaseOwner(this);
                    } else {
                        closePermits.abortSupportClose(batch);
                    }
                    LOGGER.log(Level.SEVERE,
                            "Operation: finalize Flutter Designer editor close. "
                            + "Target: {0}. Reason: {1}.",
                            new Object[] {
                                dataObject.getPrimaryFile().getPath(),
                                completionFailureReason
                            });
                }
                closePermit = null;
                closeInvocationPermit = null;
                supportCloseBatch = null;
                admittedPhysicalCloseRetries = 0;
                if (batch != null) {
                    if (finalized) {
                        editorSupport.supportCloseOwnerCompleted(
                                batch, admittedPermit.lastClone());
                    } else {
                        editorSupport.supportCloseOwnerFailed(
                                batch,
                                completionFailureReason);
                    }
                }
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
        FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch =
                supportCloseBatch;
        if (batch != null
                && previousPermit != null
                && closePermits.supportCloseOwnerState(
                        batch, this, previousPermit)
                        == FlutterDesignerEditorClosePermitCoordinator
                                .SupportCloseOwnerState.ADMITTED) {
            return closePermits.validateAdmittedSupportCloseOwner(
                    batch,
                    this,
                    previousPermit,
                    cloneTopology(),
                    editorSupport.editorShellDocumentRevision());
        }
        FlutterDesignerEditorClosePermitCoordinator.Admission admission;
        if (batch == null) {
            admission = closePermits.acquire(
                    this,
                    this::cloneTopology,
                    editorSupport::editorShellDocumentRevision,
                    editorSupport::confirmEditorShellDocumentClose);
        } else {
            admission = closePermits.acquireSupportCloseOwner(
                    batch,
                    this,
                    cloneTopology(),
                    editorSupport.editorShellDocumentRevision());
        }
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

    /**
     * Drives the Designer close gate explicitly before asking NetBeans to
     * retire the TopComponent.
     *
     * <p>The production window system normally calls {@link #canClose()} from
     * {@link #close()}, but the headless window manager used by module tests
     * closes a TopComponent without that callback.  Calling the gate here is
     * also safe in production: a second callback sees the already-admitted
     * one-shot permit and only validates its exact shell, topology and document
     * revision.</p>
     */
    private boolean closeThroughExplicitGate() {
        if (!canClose()) {
            return false;
        }
        return close();
    }

    /** Starts this exact owner's phase of an already admitted support batch. */
    boolean requestSupportClose(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch) {
        Objects.requireNonNull(batch, "batch");
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Support-wide Designer close must dispatch owners on the EDT");
        }
        if (!isOpened() || componentOpening || supportCloseBatch != null) {
            return false;
        }
        supportCloseBatch = batch;
        admittedPhysicalCloseRetries = 0;
        try {
            boolean closed = closeThroughExplicitGate();
            if (closed) {
                return true;
            }
            FlutterDesignerEditorClosePermitCoordinator.Permit permit =
                    closePermit;
            if (permit != null) {
                FlutterDesignerEditorClosePermitCoordinator.SupportCloseOwnerState
                        state = closePermits.supportCloseOwnerState(
                                batch, this, permit);
                if (state == FlutterDesignerEditorClosePermitCoordinator
                        .SupportCloseOwnerState.READY) {
                    return true;
                }
                if (state == FlutterDesignerEditorClosePermitCoordinator
                        .SupportCloseOwnerState.ADMITTED) {
                    scheduleAdmittedPhysicalCloseRetry(batch, permit);
                    return true;
                }
            }
            supportCloseBatch = null;
            return false;
        } catch (RuntimeException | Error failure) {
            FlutterDesignerEditorClosePermitCoordinator.Permit permit =
                    closePermit;
            if (permit != null
                    && closePermits.isAdmittedOwner(permit, this)) {
                scheduleAdmittedPhysicalCloseRetry(batch, permit);
            } else if (permit != null) {
                abandonClosePermit(permit);
            } else {
                closePermits.abortSupportClose(batch);
            }
            if (!closePermits.supportCloseActive(batch)) {
                supportCloseBatch = null;
            }
            throw failure;
        }
    }

    private void scheduleAdmittedPhysicalCloseRetry(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            FlutterDesignerEditorClosePermitCoordinator.Permit permit) {
        if (supportCloseBatch != batch || closePermit != permit) {
            return;
        }
        if (admittedPhysicalCloseRetries
                >= MAX_ADMITTED_PHYSICAL_CLOSE_RETRIES) {
            editorSupport.supportCloseOwnerFailed(
                    batch,
                    "NetBeans admitted the editor close but kept the shell "
                    + "physically open after "
                    + MAX_ADMITTED_PHYSICAL_CLOSE_RETRIES + " retries");
            return;
        }
        admittedPhysicalCloseRetries++;
        SwingUtilities.invokeLater(() ->
                retryAdmittedPhysicalClose(batch, permit));
    }

    private void retryAdmittedPhysicalClose(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            FlutterDesignerEditorClosePermitCoordinator.Permit permit) {
        if (supportCloseBatch != batch || closePermit != permit) {
            return;
        }
        if (!isOpened()
                || !closePermits.validateAdmittedSupportCloseOwner(
                        batch,
                        this,
                        permit,
                        cloneTopology(),
                        editorSupport.editorShellDocumentRevision())) {
            failSupportCloseIfPresent(
                    "the admitted physical-close retry lost its exact shell, "
                    + "topology or document revision");
            return;
        }
        final boolean closed;
        try {
            closed = closeThroughExplicitGate();
        } catch (RuntimeException | Error failure) {
            if (closePermits.isAdmittedOwner(permit, this)) {
                scheduleAdmittedPhysicalCloseRetry(batch, permit);
            } else {
                failSupportCloseIfPresent(
                        "the admitted physical-close retry failed");
            }
            LOGGER.log(Level.WARNING,
                    "Operation: finish an admitted Flutter Designer editor "
                    + "close. Target: "
                    + dataObject.getPrimaryFile().getPath()
                    + ". Reason: NetBeans failed the physical-close retry.",
                    failure);
            return;
        }
        if (closed) {
            return;
        }
        FlutterDesignerEditorClosePermitCoordinator.SupportCloseOwnerState state =
                closePermits.supportCloseOwnerState(batch, this, permit);
        if (state == FlutterDesignerEditorClosePermitCoordinator
                .SupportCloseOwnerState.ADMITTED) {
            scheduleAdmittedPhysicalCloseRetry(batch, permit);
        } else {
            failSupportCloseIfPresent(
                    "the admitted physical-close retry lost its close authority");
        }
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
            boolean lifecycleCurrent = closePermits.markAdmitted(permit);
            if (!closePermits.isAdmittedOwner(permit, this)) {
                throw new IllegalStateException(
                        "The Flutter Designer close permit was lost after "
                        + "NetBeans admitted the editor close");
            }
            if (!closePermits.admittedPhysicalOwnerCurrent(permit, this)) {
                // The same TopComponent identity was closed and reopened while
                // NetBeans was inside super.canClose(). The old admission must
                // not physically close that new lifecycle incarnation.
                if (!closePermits.releaseStaleAdmittedOwner(permit, this)) {
                    throw new IllegalStateException(
                            "The stale Flutter Designer lifecycle admission "
                            + "could not be released");
                }
                return false;
            }
            admitted = true;
            boolean admittedStateBound = closePermits.bindAdmittedState(
                    permit,
                    cloneTopology(),
                    editorSupport.editorShellDocumentRevision());
            if (!lifecycleCurrent || !admittedStateBound) {
                // CloneableTopComponent.Ref already removed this owner. It is
                // no longer safe to revoke the permit or veto physical close;
                // componentClosed will finish or reject the batch using the
                // retained irreversible authority.
                LOGGER.log(Level.SEVERE,
                        "Operation: bind admitted Flutter Designer editor close. "
                        + "Target: {0}. Reason: the post-unregister topology "
                        + "or document revision could not be bound exactly.",
                        dataObject.getPrimaryFile().getPath());
            }
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
        if (closePermits.isAdmittedOwner(permit, this)) {
            // NetBeans already removed this owner from its clone Ref. Preserve
            // the sole token that can complete or retry the physical close.
            return;
        }
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
        List<Action> merged = new ArrayList<>(Arrays.asList(inherited));
        merged.add(null);
        merged.add(safeCloseAction);
        merged.add(safeCloseAllAction);
        FlutterDesignerEditorPaneContent current = content;
        if (current == null
                || current.perspective()
                        != FlutterDesignerEditorPaneContent.Perspective.DESIGN) {
            return merged.toArray(Action[]::new);
        }
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
                failSupportCloseIfPresent(
                        "the document or clone topology changed before the "
                        + "Canvas close retry");
                return;
            }
            boolean closed = closeThroughExplicitGate();
            if (!closed) {
                FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch
                        batch = supportCloseBatch;
                if (batch != null
                        && closePermit == permit
                        && closePermits.isAdmittedOwner(permit, this)) {
                    scheduleAdmittedPhysicalCloseRetry(batch, permit);
                } else if (closePermit == null) {
                    failSupportCloseIfPresent(
                            "the Canvas close retry lost its exact close permit");
                }
            }
        } catch (RuntimeException | Error failure) {
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch =
                    supportCloseBatch;
            if (batch != null
                    && closePermit == permit
                    && closePermits.isAdmittedOwner(permit, this)) {
                scheduleAdmittedPhysicalCloseRetry(batch, permit);
            } else {
                abandonClosePermit(permit);
                failSupportCloseIfPresent(
                        "the tokenized Canvas close retry failed");
            }
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
            failSupportCloseIfPresent("Canvas retirement failed");
            LOGGER.log(Level.WARNING,
                    "Operation: retire Flutter Designer Canvas before editor "
                    + "close. Target: "
                    + dataObject.getPrimaryFile().getPath()
                    + ". Reason: Canvas retirement failed; the editor remains open.",
                    failure);
        });
    }

    private void failSupportCloseIfPresent(String reason) {
        FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch =
                supportCloseBatch;
        if (batch == null) {
            return;
        }
        closePermits.abortSupportClose(batch);
        if (!closePermits.supportCloseActive(batch)) {
            supportCloseBatch = null;
        }
        editorSupport.supportCloseOwnerFailed(batch, reason);
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
