package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.state.WidgetStatePropertyBindingCatalog;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Keeps the established literal/in-place editor intact; binding actions own their atomic mutation. */
final class FlutterWidgetStatePropertyEditor extends PropertyEditorSupport implements ExPropertyEditor {
    static final String FIELDS_NAME = "flutter.property.state.fields";
    static final String TRANSFORM_NAME = "flutter.property.state.transform";
    static final String COMPARISON_NAME = "flutter.property.state.comparison";
    static final String APPLY_NAME = "flutter.property.state.apply";
    static final String REMOVE_NAME = "flutter.property.state.remove";
    static final String STATUS_NAME = "flutter.property.state.status";
    static final String LITERAL_NAME = "flutter.property.state.literal";
    static final String BOOLEAN_NAME = "flutter.property.state.boolean";
    static final String BOUND_FIELD_NAME = "flutter.property.state.boundField";
    static final String RENAME_FIELD_NAME = "flutter.property.state.renameFieldName";
    static final String RENAME_NAME = "flutter.property.state.rename";
    static final String NAVIGATE_NAME = "flutter.property.state.navigate";
    private final WidgetNode widget;
    private final WidgetStatePropertyBindingCatalog.Descriptor descriptor;
    private final FlutterWidgetEventsContext context;
    private final Supplier<FlutterWidgetEventsContext> currentContext;
    private final Supplier<WidgetNode> currentWidget;
    private final PropertyEditor literal;
    private PropertyEnv environment;
    private Component panel;
    private boolean operationCompleted;

    FlutterWidgetStatePropertyEditor(WidgetNode widget, WidgetStatePropertyBindingCatalog.Descriptor descriptor,
            FlutterWidgetEventsContext context, Supplier<FlutterWidgetEventsContext> currentContext,
            Supplier<WidgetNode> currentWidget, PropertyEditor literal) {
        this.widget = Objects.requireNonNull(widget);
        this.descriptor = Objects.requireNonNull(descriptor);
        this.context = Objects.requireNonNull(context);
        this.currentContext = Objects.requireNonNull(currentContext);
        this.currentWidget = Objects.requireNonNull(currentWidget);
        this.literal = Objects.requireNonNull(literal);
        PropertyValue preview = widget.properties().get(descriptor.propertyName());
        literal.setValue(preview == null ? FlutterPropertyCellValue.unset() : FlutterPropertyCellValue.explicit(preview));
        literal.addPropertyChangeListener(ignored -> firePropertyChange());
    }

    @Override public void setValue(Object value) { literal.setValue(value); }
    @Override public Object getValue() {
        if (!operationCompleted) return literal.getValue();
        PropertyValue value = currentWidget.get().properties().get(descriptor.propertyName());
        return value == null ? FlutterPropertyCellValue.unset() : FlutterPropertyCellValue.explicit(value);
    }
    @Override public String getAsText() { return literal.getAsText(); }
    @Override public void setAsText(String text) { literal.setAsText(text); }
    @Override public String[] getTags() { return literal.getTags(); }
    @Override public String getJavaInitializationString() { return literal.getJavaInitializationString(); }
    @Override public boolean isPaintable() { return literal.isPaintable(); }
    @Override public void paintValue(Graphics graphics, Rectangle box) { literal.paintValue(graphics, box); }
    @Override public boolean supportsCustomEditor() { return true; }
    @Override public void attachEnv(PropertyEnv value) {
        environment = Objects.requireNonNull(value);
        if (literal instanceof ExPropertyEditor extended) extended.attachEnv(value);
        panel = null;
    }
    @Override public Component getCustomEditor() {
        if (panel == null) panel = new BindingPanel();
        return panel;
    }

    private String staleReason() {
        if (currentContext.get() != context) return "Cannot edit State binding for " + descriptor.propertyName().value()
                + " on widget " + widget.id() + ": the document changed. Close and reopen this property editor.";
        return context.propertyStateBindingUnavailableReason(descriptor.propertyName());
    }

    private final class BindingPanel extends JPanel {
        private final JComboBox<StatePropertyBinding> fields = new JComboBox<>();
        private final JComboBox<StatePropertyBinding.Transform> transforms = new JComboBox<>();
        private final JTextField comparison = new JTextField(24);
        private final JTextArea status = new JTextArea(3, 48);
        private final JButton apply = new JButton("Bind State Field");
        private final JButton remove = new JButton("Remove Binding");
        private final JButton rename = new JButton("Rename State Field");
        private final JButton navigate = new JButton("Go to Field");
        private final StatePropertyBinding existingBinding = widget.propertyBindings().get(descriptor.propertyName());
        private final JTextField renamedField = new JTextField(existingBinding == null ? "" : existingBinding.fieldName(), 24);
        private boolean busy;

        BindingPanel() {
            super(new BorderLayout(8, 8));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setPreferredSize(new Dimension(740, 540));
            var pages = new JTabbedPane();
            pages.addTab("Literal / Canvas preview", literalPanel());
            JPanel state = new JPanel(new BorderLayout(8, 8));
            JPanel controls = new JPanel(new GridLayout(0, 2, 8, 8));
            controls.add(new JLabel("Runtime property:"));
            controls.add(new JLabel(descriptor.propertyName().value() + " — " + descriptor.dartType()));
            controls.add(new JLabel("Currently bound State field:"));
            JLabel boundField = new JLabel(existingBinding == null ? "<none> — bind a field first" : fieldLabel(existingBinding));
            boundField.setName(BOUND_FIELD_NAME);
            controls.add(boundField);
            JLabel renameLabel = new JLabel("New private State field name:");
            renameLabel.setLabelFor(renamedField);
            renamedField.setName(RENAME_FIELD_NAME);
            controls.add(renameLabel);
            controls.add(renamedField);
            controls.add(new JLabel("Existing State field:"));
            fields.setName(FIELDS_NAME);
            fields.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean selected, boolean focus) {
                    return super.getListCellRendererComponent(list, value instanceof StatePropertyBinding field
                            ? fieldLabel(field) : "Choose a direct initialized private State field", index, selected, focus);
                }
            });
            controls.add(fields);
            controls.add(new JLabel("Value transform:"));
            transforms.setName(TRANSFORM_NAME);
            transforms.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean selected, boolean focus) {
                    String label = value instanceof StatePropertyBinding.Transform transform ? switch (transform) {
                        case DIRECT -> "Direct field value";
                        case TO_STRING -> "Convert value to text";
                        case TEXT -> "Controller text";
                        case EQUALS -> "Equals the comparison value";
                        case NOT -> "Invert boolean";
                        case CLAMP -> "Clamp to valid range";
                    } : "Choose a transform";
                    return super.getListCellRendererComponent(list, label, index, selected, focus);
                }
            });
            descriptor.allowedTransforms().stream().sorted().forEach(transforms::addItem);
            controls.add(transforms);
            controls.add(new JLabel("Comparison value (Equals only):"));
            comparison.setName(COMPARISON_NAME);
            controls.add(comparison);
            state.add(controls, BorderLayout.NORTH);
            JTextArea help = new JTextArea("Choose an existing private State field. Direct uses its value; Convert to text calls toString; "
                    + "Controller text reads TextEditingController.text; Equals compares a typed literal; Invert boolean applies !; "
                    + "Clamp keeps a numeric value within the widget's valid range. "
                    + "For comparisons use true/false, a number, null, or plain text matching the source field type. "
                    + "The literal remains an independent Canvas preview; it does not change the runtime field. "
                    + "This is a read binding: runtime updates must call setState (or use the generated controller listener). "
                    + "Binding actions apply immediately and close the editor. Remove preserves your fields and code. "
                    + "Rename State Field changes the currently bound field throughout this form, including its supported source references, all bindings and controller listener name; event handler names stay unchanged. "
                    + "It does not rename an unbound dropdown candidate, save, or navigate automatically. Go to Field only opens the verified declaration.");
            help.setEditable(false);
            help.setLineWrap(true);
            help.setWrapStyleWord(true);
            help.setOpaque(false);
            state.add(help, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
            apply.setName(APPLY_NAME);
            remove.setName(REMOVE_NAME);
            rename.setName(RENAME_NAME);
            navigate.setName(NAVIGATE_NAME);
            actions.add(apply);
            actions.add(remove);
            actions.add(navigate);
            actions.add(rename);
            state.add(actions, BorderLayout.SOUTH);
            pages.addTab("State binding", state);
            add(pages, BorderLayout.CENTER);
            status.setName(STATUS_NAME);
            status.setEditable(false);
            status.setLineWrap(true);
            status.setWrapStyleWord(true);
            status.setOpaque(false);
            add(status, BorderLayout.SOUTH);
            StatePropertyBinding existing = widget.propertyBindings().get(descriptor.propertyName());
            if (existing != null) {
                transforms.setSelectedItem(existing.transform());
                comparison.setText(existing.comparisonValue().map(FlutterWidgetStatePropertyEditor::comparisonText).orElse(""));
                pages.setSelectedIndex(1);
            }
            fields.addActionListener(ignored -> { updateTransforms(); refresh(); });
            transforms.addActionListener(ignored -> refresh());
            apply.addActionListener(ignored -> run(() -> {
                StatePropertyBinding field = Objects.requireNonNull((StatePropertyBinding) fields.getSelectedItem(), "Choose a State field first.");
                StatePropertyBinding.Transform transform = Objects.requireNonNull((StatePropertyBinding.Transform) transforms.getSelectedItem());
                Optional<PropertyValue> value = transform == StatePropertyBinding.Transform.EQUALS
                        ? Optional.of(parseComparison(field.type(), comparison.getText())) : Optional.empty();
                return context.bindPropertyToState(descriptor.propertyName(), new StatePropertyBinding(field.fieldName(),
                        field.type(), field.referenceType(), transform, value));
            }));
            remove.addActionListener(ignored -> run(() -> context.removePropertyStateBinding(descriptor.propertyName())));
            rename.addActionListener(ignored -> run(() -> context.renameStateField(boundFieldName(), acceptedRename())));
            navigate.addActionListener(ignored -> run(() -> context.navigateToStateField(boundFieldName()), false));
            String reason = staleReason();
            if (!reason.isEmpty()) {
                status.setText(reason);
                refresh();
            } else load(existing);
        }

        private Component literalPanel() {
            if (literal.supportsCustomEditor()) return literal.getCustomEditor();
            if (descriptor.dartType().equals("bool")) {
                JPanel result = new JPanel(new FlowLayout(FlowLayout.LEADING));
                var current = (FlutterPropertyCellValue) literal.getValue();
                JCheckBox unset = new JCheckBox("Use default (<not set>)", !current.isExplicit());
                JCheckBox checkbox = new JCheckBox("Value", current.explicitValue()
                        .filter(value -> value instanceof PropertyValue.BooleanValue bool && bool.value()).isPresent());
                checkbox.setName(BOOLEAN_NAME);
                checkbox.setEnabled(!unset.isSelected());
                java.awt.event.ActionListener update = ignored -> {
                    checkbox.setEnabled(!unset.isSelected());
                    try {
                        literal.setAsText(unset.isSelected() ? FlutterPropertyCellValue.NOT_SET_TEXT : Boolean.toString(checkbox.isSelected()));
                        if (environment != null) environment.setState(PropertyEnv.STATE_VALID);
                    } catch (IllegalArgumentException error) {
                        if (environment != null) environment.setState(PropertyEnv.STATE_INVALID);
                        status.setText(error.getMessage());
                    }
                };
                unset.addActionListener(update);
                checkbox.addActionListener(update);
                result.add(unset);
                result.add(checkbox);
                return result;
            }
            JPanel result = new JPanel(new BorderLayout(8, 8));
            JTextField text = new JTextField(literal.getAsText());
            text.setName(LITERAL_NAME);
            text.getDocument().addDocumentListener(new DocumentListener() {
                private void changed() {
                    try {
                        literal.setAsText(text.getText());
                        if (environment != null) environment.setState(PropertyEnv.STATE_VALID);
                    } catch (IllegalArgumentException error) {
                        if (environment != null) environment.setState(PropertyEnv.STATE_INVALID);
                        status.setText(error.getMessage());
                    }
                }
                @Override public void insertUpdate(DocumentEvent event) { changed(); }
                @Override public void removeUpdate(DocumentEvent event) { changed(); }
                @Override public void changedUpdate(DocumentEvent event) { changed(); }
            });
            result.add(new JLabel("Literal value / independent Canvas preview:"), BorderLayout.NORTH);
            result.add(text, BorderLayout.CENTER);
            return result;
        }

        private void load(StatePropertyBinding existing) {
            busy = true;
            status.setText("Reading direct initialized State fields…");
            refresh();
            try {
                context.discoverStateFields().whenComplete((result, error) -> onEdt(() -> {
                    busy = false;
                    String reason = staleReason();
                    if (!reason.isEmpty()) status.setText(reason);
                    else if (error != null) status.setText(errorText(error));
                    else {
                        fields.removeAllItems();
                        result.stream().filter(field -> descriptor.allowedTransforms().stream().anyMatch(transform -> accepts(field, transform)))
                                .forEach(fields::addItem);
                        if (existing != null) result.stream().filter(field -> field.fieldName().equals(existing.fieldName())
                                && field.type() == existing.type() && field.referenceType().equals(existing.referenceType()))
                                .findFirst().ifPresent(fields::setSelectedItem);
                        status.setText(fields.getItemCount() == 0 ? "No compatible fields. Create a State binding on a control, or declare a compatible initialized private field in Source."
                                : "Choose a field and transform. Dart compatibility is checked before applying the binding.");
                        if (existing != null) transforms.setSelectedItem(existing.transform());
                    }
                    refresh();
                }));
            } catch (RuntimeException error) {
                busy = false;
                status.setText(errorText(error));
                refresh();
            }
        }

        private void updateTransforms() {
            Object previous = transforms.getSelectedItem();
            transforms.removeAllItems();
            if (fields.getSelectedItem() instanceof StatePropertyBinding field) {
                descriptor.allowedTransforms().stream().sorted().filter(transform -> accepts(field, transform)).forEach(transforms::addItem);
                for (int index = 0; index < transforms.getItemCount(); index++) {
                    if (transforms.getItemAt(index).equals(previous)) { transforms.setSelectedIndex(index); break; }
                }
            }
        }

        private boolean accepts(StatePropertyBinding field, StatePropertyBinding.Transform transform) {
            try {
                Optional<PropertyValue> sample = transform == StatePropertyBinding.Transform.EQUALS
                        ? Optional.of(sampleComparison(field.type())) : Optional.empty();
                var binding = new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), transform, sample);
                var bindings = new java.util.LinkedHashMap<>(widget.propertyBindings());
                bindings.put(descriptor.propertyName(), binding);
                var candidate = new WidgetNode(widget.id(), widget.type(), widget.properties(), widget.slots(),
                        widget.extensions(), widget.stateBinding(), bindings);
                return WidgetStatePropertyBindingCatalog.validationError(candidate).isEmpty();
            } catch (IllegalArgumentException error) { return false; }
        }

        private void refresh() {
            boolean ready = !busy && !operationCompleted && staleReason().isEmpty();
            fields.setEnabled(ready);
            transforms.setEnabled(ready);
            comparison.setEnabled(ready && transforms.getSelectedItem() == StatePropertyBinding.Transform.EQUALS);
            apply.setEnabled(ready && fields.getSelectedItem() != null && transforms.getSelectedItem() != null);
            remove.setEnabled(ready && currentWidget.get().propertyBindings().containsKey(descriptor.propertyName()));
            boolean bound = ready && currentWidget.get().propertyBindings().containsKey(descriptor.propertyName());
            rename.setEnabled(bound);
            navigate.setEnabled(bound);
            renamedField.setEnabled(bound);
        }

        private void run(Supplier<CompletionStage<Void>> operation) {
            run(operation, true);
        }

        private void run(Supplier<CompletionStage<Void>> operation, boolean mutation) {
            String reason = staleReason();
            if (!reason.isEmpty()) { status.setText(reason); refresh(); return; }
            if (busy || operationCompleted) return;
            busy = true;
            status.setText(mutation ? "Applying property State binding…" : "Opening the State field declaration…");
            refresh();
            try {
                operation.get().whenComplete((ignored, error) -> onEdt(() -> {
                    busy = false;
                    if (error != null) { status.setText(errorText(error)); refresh(); return; }
                    // Navigation also closes the dialog without applying a
                    // pending literal draft through the host PropertySheet.
                    operationCompleted = true;
                    status.setText("Done.");
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window instanceof Dialog) window.dispose();
                    refresh();
                }));
            } catch (RuntimeException error) {
                busy = false;
                status.setText(errorText(error));
                refresh();
            }
        }

        private String boundFieldName() {
            if (existingBinding == null) throw new IllegalArgumentException("Bind this property to a State field before managing its declaration.");
            return existingBinding.fieldName();
        }

        private String acceptedRename() {
            String name = renamedField.getText().trim();
            if (!name.matches("_[A-Za-z][A-Za-z0-9_]*") || name.length() > 128) {
                throw new IllegalArgumentException("Enter a private Dart State field name starting with an underscore (at most 128 characters).");
            }
            return name;
        }
    }

    static String fieldLabel(StatePropertyBinding field) {
        return field.fieldName() + " — " + switch (field.type()) {
            case BOOL -> "bool";
            case NULLABLE_BOOL -> "bool?";
            case DOUBLE -> "double";
            case RANGE_VALUES -> "RangeValues";
            case STRING -> "String";
            case INT -> "int";
            case NUM -> "num";
            case TEXT_CONTROLLER -> "TextEditingController";
            case NULLABLE_STRING -> "String?";
            case NULLABLE_INT -> "int?";
            case NULLABLE_DOUBLE -> "double?";
            case NULLABLE_NUM -> "num?";
            case NULLABLE_OBJECT -> "Object?";
            case NULLABLE_REFERENCE -> field.referenceType().orElseThrow().rootSymbol() + "?";
        };
    }

    static PropertyValue parseComparison(StateBinding.Type type, String text) {
        String trimmed = text.trim();
        if (trimmed.equals("null") && type.name().startsWith("NULLABLE_")) return new PropertyValue.NullValue();
        return switch (type) {
            case BOOL, NULLABLE_BOOL -> {
                if (!trimmed.equals("true") && !trimmed.equals("false")) throw new IllegalArgumentException("Enter true or false for a boolean comparison.");
                yield new PropertyValue.BooleanValue(Boolean.parseBoolean(trimmed));
            }
            case INT, NULLABLE_INT -> new PropertyValue.IntegerValue(new BigInteger(trimmed));
            case DOUBLE, NUM, NULLABLE_DOUBLE, NULLABLE_NUM -> new PropertyValue.DoubleValue(new BigDecimal(trimmed));
            case STRING, NULLABLE_STRING -> new PropertyValue.StringValue(text);
            case NULLABLE_OBJECT -> {
                if (trimmed.equals("true") || trimmed.equals("false")) yield new PropertyValue.BooleanValue(Boolean.parseBoolean(trimmed));
                if (trimmed.matches("-?[0-9]+")) yield new PropertyValue.IntegerValue(new BigInteger(trimmed));
                if (trimmed.matches("-?[0-9]+\\.[0-9]+")) yield new PropertyValue.DoubleValue(new BigDecimal(trimmed));
                yield new PropertyValue.StringValue(text);
            }
            default -> throw new IllegalArgumentException("The comparison editor supports bool, number, string, and null literals. Choose a supported source field.");
        };
    }

    private static PropertyValue sampleComparison(StateBinding.Type type) {
        if (type.name().startsWith("NULLABLE_")) return new PropertyValue.NullValue();
        return switch (type) {
            case BOOL -> new PropertyValue.BooleanValue(false);
            case STRING -> new PropertyValue.StringValue("");
            case INT -> new PropertyValue.IntegerValue(BigInteger.ZERO);
            case NUM, DOUBLE -> new PropertyValue.DoubleValue(BigDecimal.ZERO);
            default -> throw new IllegalArgumentException("No supported comparison literal for this field type.");
        };
    }

    static String comparisonText(PropertyValue value) {
        return switch (value) {
            case PropertyValue.StringValue string -> string.value();
            case PropertyValue.BooleanValue bool -> Boolean.toString(bool.value());
            case PropertyValue.IntegerValue integer -> integer.value().toString();
            case PropertyValue.DoubleValue decimal -> decimal.value().toPlainString();
            case PropertyValue.NullValue ignored -> "null";
            default -> PropertyValueFormatter.format(value);
        };
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run(); else SwingUtilities.invokeLater(action);
    }
    private static String errorText(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
