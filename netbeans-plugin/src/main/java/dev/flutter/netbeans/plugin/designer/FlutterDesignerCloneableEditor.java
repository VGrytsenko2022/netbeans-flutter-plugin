package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.netbeans.core.spi.multiview.CloseOperationState;
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
    private final FlutterDesignerEditorSupport editorSupport;
    private final FlutterDesignerDataObject dataObject;
    private final ActiveLookup activeLookup;
    private final FlutterDesignerAsyncCloseOperationHandler closeHandler =
            new FlutterDesignerAsyncCloseOperationHandler();
    private FlutterDesignerEditorPaneContent content;
    private boolean componentOpen;
    private boolean componentShowing;
    private boolean componentActive;
    private boolean sourceRequested;

    FlutterDesignerCloneableEditor(FlutterDesignerEditorSupport editorSupport) {
        super(Objects.requireNonNull(editorSupport, "editorSupport"), false);
        this.editorSupport = editorSupport;
        dataObject = (FlutterDesignerDataObject) editorSupport.getDataObject();
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
        super.componentOpened();
        componentOpen = true;
        synchronizeContentLifecycle();
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
        FlutterDesignerEditorPaneContent current = content;
        if (current != null) {
            current.closed();
            remove(current);
            content = null;
            activeLookup.setPerspective(Lookup.EMPTY);
        }
        componentActive = false;
        componentShowing = false;
        componentOpen = false;
        sourceRequested = false;
        super.componentClosed();
    }

    @Override
    public boolean canClose() {
        FlutterDesignerEditorPaneContent current = content;
        if (current != null) {
            CloseOperationState state = current.closeState();
            // Production remains hard-disabled: on a dirty last clone this
            // provisional flow can retire Canvas before super.canClose() asks
            // the Source Save/Discard/Cancel question. The final shell must
            // acquire one document/topology-bound combined permit first.
            if (!state.canClose()
                    && !closeHandler.resolveCloseOperation(
                            new CloseOperationState[] {state})) {
                return false;
            }
        }
        return super.canClose();
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

    private void requestCloseRetry() {
        if (SwingUtilities.isEventDispatchThread()) {
            close();
        } else {
            SwingUtilities.invokeLater(this::close);
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
