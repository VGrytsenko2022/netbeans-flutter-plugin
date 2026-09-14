package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/** A scoped builder action around the unchanged nullable reference editor. */
final class FlutterMenuAnchorBuilderPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor {
    static final String METHOD_NAME = "flutter.menuAnchor.builder.method";
    static final String CONFIRM_NAME = "flutter.menuAnchor.builder.confirm";
    static final String CREATE_NAME = "flutter.menuAnchor.builder.create";
    static final String NAVIGATE_NAME = "flutter.menuAnchor.builder.navigate";
    static final String STATUS_NAME = "flutter.menuAnchor.builder.status";
    static final String GUIDANCE_NAME = "flutter.menuAnchor.builder.guidance";
    static final String TABS_NAME = "flutter.menuAnchor.builder.tabs";
    private static final String REFERENCE_DRAFT_MESSAGE = "Apply or cancel the Reference draft before creating a builder template.";
    private static final PropertyName BUILDER = new PropertyName("builder");
    private final WidgetNode widget;
    private final FlutterWidgetEventsContext context;
    private final Supplier<FlutterWidgetEventsContext> currentContext;
    private final Supplier<WidgetNode> currentWidget;
    private final PropertyEditor literal;
    private PropertyEnv environment;
    private Component panel;
    private boolean completed;

    FlutterMenuAnchorBuilderPropertyEditor(WidgetNode widget, FlutterWidgetEventsContext context,
            Supplier<FlutterWidgetEventsContext> currentContext, Supplier<WidgetNode> currentWidget,
            PropertyEditor literal) {
        this.widget = Objects.requireNonNull(widget);
        this.context = Objects.requireNonNull(context);
        this.currentContext = Objects.requireNonNull(currentContext);
        this.currentWidget = Objects.requireNonNull(currentWidget);
        this.literal = Objects.requireNonNull(literal);
        literal.setValue(cell(widget));
        literal.addPropertyChangeListener(ignored -> firePropertyChange());
    }

    private static FlutterPropertyCellValue cell(WidgetNode widget) {
        PropertyValue value = widget.properties().get(BUILDER);
        return value == null ? FlutterPropertyCellValue.unset() : FlutterPropertyCellValue.explicit(value);
    }

    @Override public void setValue(Object value) { literal.setValue(value); }
    @Override public Object getValue() { return completed ? cell(currentWidget.get()) : literal.getValue(); }
    @Override public String getAsText() { return literal.getAsText(); }
    @Override public void setAsText(String value) { literal.setAsText(value); }
    @Override public String[] getTags() { return literal.getTags(); }
    @Override public boolean supportsCustomEditor() { return true; }
    @Override public void attachEnv(PropertyEnv value) {
        environment = Objects.requireNonNull(value);
        if (literal instanceof ExPropertyEditor editor) editor.attachEnv(value);
        panel = null;
    }
    @Override public Component getCustomEditor() {
        if (panel == null) panel = new BuilderPanel();
        return panel;
    }

    private String unavailableReason() {
        if (currentContext.get() != context || !currentWidget.get().equals(widget)) {
            return "Cannot edit Menu Builder: the selected document revision changed. Close and reopen this editor.";
        }
        if (environment != null && environment.getFeatureDescriptor() instanceof org.openide.nodes.Node.Property<?> property
                && !property.canWrite()) return "Cannot edit Menu Builder: this property is read-only.";
        return context.unavailableReason();
    }

    private final class BuilderPanel extends JPanel {
        private final JTextField name = new JTextField("_buildMenuAnchor", 24);
        private final JCheckBox confirm = new JCheckBox("Use Child as the new TextButton's label/content");
        private final JButton create = new JButton("Create Menu Builder…");
        private final JButton navigate = new JButton("Go to Menu Builder");
        private final JTextArea status = text("", STATUS_NAME);
        private final Component referenceEditor;
        private boolean busy;

        BuilderPanel() {
            super(new BorderLayout(8, 8));
            var tabs = new JTabbedPane();
            tabs.setName(TABS_NAME);
            referenceEditor = literal.getCustomEditor();
            tabs.addTab("Reference", referenceEditor);
            var actions = new JPanel(new BorderLayout(8, 8));
            actions.add(text("Creates a user-owned Widget method(BuildContext context, MenuController controller, Widget? child). "
                    + "The reviewed template returns a TextButton that toggles the supplied controller and uses Child, or Text('Menu') when Child is empty. "
                    + "An existing interactive Child is not rewired; using another button as content can create nested controls. "
                    + "The method is editable in Source. Existing callbacks, controller references and menu children are preserved. "
                    + "Create is an explicit atomic action; opening or cancelling this editor creates nothing. No automatic Save or navigation occurs. "
                    + "A bound builder must be reset before creating another; reset retains its method. "
                    + "Canvas Preview menu uses its own controller and never executes this method.", GUIDANCE_NAME), BorderLayout.NORTH);
            var controls = new JPanel(new BorderLayout(4, 4));
            var method = new JPanel(new FlowLayout(FlowLayout.LEADING));
            name.setName(METHOD_NAME); method.add(new JLabel("Method name:")); method.add(name);
            controls.add(method, BorderLayout.NORTH);
            confirm.setName(CONFIRM_NAME); controls.add(confirm, BorderLayout.CENTER);
            var buttons = new JPanel(new FlowLayout(FlowLayout.LEADING));
            create.setName(CREATE_NAME); navigate.setName(NAVIGATE_NAME);
            buttons.add(create); buttons.add(navigate); controls.add(buttons, BorderLayout.SOUTH);
            actions.add(controls, BorderLayout.CENTER); actions.add(status, BorderLayout.SOUTH);
            tabs.addTab("Menu builder", actions); add(tabs, BorderLayout.CENTER);
            tabs.addChangeListener(ignored -> refresh());
            confirm.addActionListener(ignored -> refresh());
            literal.addPropertyChangeListener(ignored -> refresh());
            create.addActionListener(ignored -> run(() -> context.createMenuAnchorBuilder(name.getText().trim()), true));
            navigate.addActionListener(ignored -> run(context::navigateToMenuAnchorBuilder, false));
            refresh();
        }

        private void refresh() {
            String reason = unavailableReason();
            boolean ready = !busy && !completed && reason.isEmpty();
            PropertyValue bound = widget.properties().get(BUILDER);
            boolean empty = bound == null || bound instanceof PropertyValue.NullValue;
            boolean unchanged = referenceDraftUnchanged();
            create.setEnabled(ready && empty && unchanged && confirm.isSelected());
            navigate.setEnabled(ready && bound instanceof PropertyValue.DartObjectReferenceValue reference
                    && reference.libraryUri().isEmpty() && reference.member().isEmpty()
                    && reference.access() == PropertyValue.DartObjectReferenceValue.Access.REFERENCE);
            name.setEnabled(ready && empty); confirm.setEnabled(ready && empty);
            if (!reason.isEmpty()) status.setText(reason);
            else if (!empty) status.setText("A builder is already bound. Reset Builder and apply that edit before creating another method.");
            else if (!unchanged) status.setText(REFERENCE_DRAFT_MESSAGE);
            else if (REFERENCE_DRAFT_MESSAGE.equals(status.getText())) status.setText("");
        }

        private boolean referenceDraftUnchanged() {
            // The reference editor publishes only on outer OK. Inspect its live
            // local draft without committing it or changing Cancel semantics.
            if (!(referenceEditor instanceof FlutterPropertyEditorComponents.CommitOnValidPanel draft)) return false;
            try {
                return Objects.equals(draft.validatedDraftValue(), cell(widget));
            } catch (IllegalArgumentException invalidDraft) {
                return false;
            }
        }

        private void run(Supplier<CompletionStage<Void>> operation, boolean mutation) {
            String reason = unavailableReason();
            if (!reason.isEmpty()) { status.setText(reason); refresh(); return; }
            if (busy || completed) return;
            // Confirmation may precede a later Reference edit; never trust the
            // previously enabled button as authority to discard that draft.
            refresh();
            if (mutation && !create.isEnabled() || !mutation && !navigate.isEnabled()) return;
            busy = true; status.setText(mutation ? "Creating and analyzing Menu Builder…" : "Opening Menu Builder…"); refresh();
            try {
                operation.get().whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> finish(error, mutation)));
            } catch (RuntimeException error) { finish(error, mutation); }
        }

        private void finish(Throwable error, boolean mutation) {
            busy = false;
            if (error != null) {
                while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
                status.setText(error.getMessage() == null ? error.toString() : error.getMessage()); refresh(); return;
            }
            completed = mutation;
            status.setText("Done."); refresh();
            var window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof Dialog) window.dispose();
        }
    }

    private static JTextArea text(String value, String name) {
        var area = new JTextArea(value, 4, 58); area.setName(name); area.setEditable(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false);
        area.getAccessibleContext().setAccessibleName(name); return area;
    }
}
