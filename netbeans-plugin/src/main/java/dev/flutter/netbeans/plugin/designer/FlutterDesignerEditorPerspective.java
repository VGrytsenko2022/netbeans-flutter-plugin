package dev.flutter.netbeans.plugin.designer;

import javax.swing.Action;
import javax.swing.JComponent;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.openide.util.Lookup;

/** Lifecycle surface consumed by the plugin-owned editor shell. */
interface FlutterDesignerEditorPerspective {
    JComponent visual();

    JComponent toolbar();

    Action[] actions();

    Lookup lookup();

    CloseOperationState closeState();

    void opened();

    void closed();

    void showing();

    void hidden();

    void activated();

    void deactivated();

    void bindCloseRetry(Runnable retry);

    /** Production adapter retaining the existing Design implementation. */
    final class Design implements FlutterDesignerEditorPerspective {
        private final FlutterDesignerMultiViewDesign delegate;

        Design(FlutterDesignerMultiViewDesign delegate) {
            this.delegate = java.util.Objects.requireNonNull(
                    delegate, "delegate");
        }

        @Override
        public JComponent visual() {
            return delegate.getVisualRepresentation();
        }

        @Override
        public JComponent toolbar() {
            return delegate.getToolbarRepresentation();
        }

        @Override
        public Action[] actions() {
            return delegate.getActions();
        }

        @Override
        public Lookup lookup() {
            return delegate.getLookup();
        }

        @Override
        public CloseOperationState closeState() {
            return delegate.canCloseElement();
        }

        @Override
        public void opened() {
            delegate.componentOpened();
        }

        @Override
        public void closed() {
            delegate.componentClosed();
        }

        @Override
        public void showing() {
            delegate.componentShowing();
        }

        @Override
        public void hidden() {
            delegate.componentHidden();
        }

        @Override
        public void activated() {
            delegate.componentActivated();
        }

        @Override
        public void deactivated() {
            delegate.componentDeactivated();
        }

        @Override
        public void bindCloseRetry(Runnable retry) {
            delegate.setEditorShellCloseRetryRequest(retry);
        }
    }
}
