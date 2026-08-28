package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 * NetBeans-native UI controls for the catalog-driven property editors.
 *
 * <p>The controls keep an edit draft separate from the immutable designer
 * snapshot. In-place controls return one typed value to PropertySheet, while
 * custom panels publish their draft only when NetBeans accepts the dialog.
 * This prevents preview events from consuming a designer mutation token.</p>
 */
final class FlutterPropertyEditorComponents {
    static final String BOOLEAN_COMPONENT_NAME = "flutter.boolean.inplace";
    static final String NUMERIC_COMPONENT_NAME = "flutter.numeric.inplace";
    static final String COLOR_CHOOSER_NAME = "flutter.color.chooser";
    static final String COLOR_ARGB_NAME = "flutter.color.argb";
    static final String COLOR_ALPHA_NAME = "flutter.color.alpha";
    static final String EDGE_ALL_NAME = "flutter.edgeInsets.all";

    private FlutterPropertyEditorComponents() {
    }

    static Optional<InplaceEditor.Factory> inplaceFactory(
            FlutterTypedPropertyEditors.Binding binding) {
        return switch (binding.editorKind()) {
            case BOOLEAN -> Optional.of(() -> new BooleanInplaceEditor(binding));
            case INTEGER, DOUBLE, NUMBER -> Optional.of(
                    () -> new NumericInplaceEditor(binding));
            default -> Optional.empty();
        };
    }

    static boolean supportsCustomEditor(
            FlutterTypedPropertyEditors.Binding binding) {
        return switch (binding.editorKind()) {
            case STRING, EDGE_INSETS, COLOR -> true;
            default -> false;
        };
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv attachedEnvironment) {
        PropertyEnv environment = attachedEnvironment != null
                ? attachedEnvironment
                : PropertyEnv.create(new FeatureDescriptor());
        return switch (binding.editorKind()) {
            case STRING -> new StringCustomEditor(editor, binding, environment);
            case EDGE_INSETS -> new EdgeInsetsCustomEditor(
                    editor, binding, environment);
            case COLOR -> new ColorCustomEditor(editor, binding, environment);
            default -> throw new IllegalStateException(
                    "No custom editor for " + binding.editorKind());
        };
    }

    static void paintColorValue(
            Graphics graphics,
            Rectangle box,
            FlutterPropertyCellValue cell) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(cell, "cell");
        Color original = graphics.getColor();
        int swatch = Math.max(10, Math.min(18, box.height - 4));
        int x = box.x + 2;
        int y = box.y + Math.max(1, (box.height - swatch) / 2);
        if (cell.explicitValue().orElse(null)
                instanceof PropertyValue.ColorValue colorValue) {
            int square = Math.max(2, swatch / 4);
            for (int row = 0; row < swatch; row += square) {
                for (int column = 0; column < swatch; column += square) {
                    graphics.setColor(((row / square) + (column / square)) % 2 == 0
                            ? new Color(230, 230, 230)
                            : new Color(180, 180, 180));
                    graphics.fillRect(x + column, y + row,
                            Math.min(square, swatch - column),
                            Math.min(square, swatch - row));
                }
            }
            graphics.setColor(toAwtColor(colorValue));
            graphics.fillRect(x, y, swatch, swatch);
            graphics.setColor(UIManager.getColor("Component.borderColor") != null
                    ? UIManager.getColor("Component.borderColor") : Color.DARK_GRAY);
            graphics.drawRect(x, y, swatch, swatch);
            graphics.setColor(original);
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(colorValue.wireArgb(), x + swatch + 6,
                    box.y + (box.height + metrics.getAscent()
                    - metrics.getDescent()) / 2);
        } else {
            graphics.setColor(original);
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(FlutterPropertyCellValue.NOT_SET_TEXT, x,
                    box.y + (box.height + metrics.getAscent()
                    - metrics.getDescent()) / 2);
        }
        graphics.setColor(original);
    }

    private static FlutterPropertyCellValue parse(
            FlutterTypedPropertyEditors.Binding binding,
            String text) {
        PropertyEditor parser = binding.createEditor();
        parser.setAsText(text);
        return cell(parser.getValue());
    }

    private static String format(
            FlutterTypedPropertyEditors.Binding binding,
            FlutterPropertyCellValue value) {
        PropertyEditor formatter = binding.createEditor();
        formatter.setValue(value);
        return formatter.getAsText();
    }

    private static FlutterPropertyCellValue cell(Object value) {
        if (!(value instanceof FlutterPropertyCellValue cell)) {
            throw new IllegalArgumentException(
                    "Flutter property UI requires FlutterPropertyCellValue.");
        }
        return cell;
    }

    private abstract static class AbstractInplaceEditor implements InplaceEditor {
        final FlutterTypedPropertyEditors.Binding binding;
        private EventListenerList listeners = new EventListenerList();
        private PropertyEditor propertyEditor;
        private PropertyEnv environment;
        private PropertyModel propertyModel;

        AbstractInplaceEditor(FlutterTypedPropertyEditors.Binding binding) {
            this.binding = Objects.requireNonNull(binding, "binding");
        }

        @Override
        public final void connect(PropertyEditor editor, PropertyEnv environment) {
            this.propertyEditor = Objects.requireNonNull(editor, "editor");
            this.environment = environment;
            configureAccessibility(environment);
            reset();
        }

        @Override
        public void clear() {
            propertyEditor = null;
            environment = null;
            propertyModel = null;
            listeners = new EventListenerList();
            clearEditorState();
        }

        @Override
        public final PropertyEditor getPropertyEditor() {
            return propertyEditor;
        }

        @Override
        public final PropertyModel getPropertyModel() {
            return propertyModel;
        }

        @Override
        public final void setPropertyModel(PropertyModel propertyModel) {
            this.propertyModel = propertyModel;
        }

        @Override
        public final void addActionListener(ActionListener listener) {
            listeners.add(ActionListener.class, listener);
        }

        @Override
        public final void removeActionListener(ActionListener listener) {
            listeners.remove(ActionListener.class, listener);
        }

        @Override
        public KeyStroke[] getKeyStrokes() {
            return null;
        }

        @Override
        public final boolean isKnownComponent(Component component) {
            return component == getComponent()
                    || SwingUtilities.isDescendingFrom(component, getComponent());
        }

        final FlutterPropertyCellValue editorValue() {
            if (propertyEditor == null) {
                throw new IllegalStateException("In-place editor is not connected.");
            }
            return cell(propertyEditor.getValue());
        }

        final PropertyEnv environment() {
            return environment;
        }

        final void fireSuccess() {
            ActionEvent event = new ActionEvent(
                    this, ActionEvent.ACTION_PERFORMED, COMMAND_SUCCESS);
            for (ActionListener listener
                    : listeners.getListeners(ActionListener.class)) {
                listener.actionPerformed(event);
            }
        }

        final void configureAccessibility(PropertyEnv environment) {
            FeatureDescriptor descriptor = environment == null
                    ? null : environment.getFeatureDescriptor();
            String displayName = descriptor == null
                    ? binding.definition().name().value()
                    : descriptor.getDisplayName();
            String description = descriptor == null
                    ? acceptedDescription(binding)
                    : descriptor.getShortDescription();
            JComponent component = getComponent();
            component.setToolTipText(description);
            component.getAccessibleContext().setAccessibleName(displayName);
            component.getAccessibleContext().setAccessibleDescription(description);
        }

        /** Return the reusable editor to its post-constructor UI state. */
        abstract void clearEditorState();
    }

    private static final class BooleanInplaceEditor
            extends AbstractInplaceEditor {
        private final JCheckBox checkBox = new JCheckBox();
        private FlutterPropertyCellValue value = FlutterPropertyCellValue.unset();

        BooleanInplaceEditor(FlutterTypedPropertyEditors.Binding binding) {
            super(binding);
            checkBox.setName(BOOLEAN_COMPONENT_NAME);
            checkBox.setOpaque(false);
            checkBox.addActionListener(ignored -> {
                value = nextValue(value, binding.optional());
                showValue();
                fireSuccess();
            });
        }

        @Override
        public JComponent getComponent() {
            return checkBox;
        }

        @Override
        public FlutterPropertyCellValue getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = binding.validate(cell(value));
            showValue();
        }

        @Override
        public boolean supportsTextEntry() {
            return false;
        }

        @Override
        public void reset() {
            value = binding.validate(editorValue());
            showValue();
        }

        @Override
        void clearEditorState() {
            value = FlutterPropertyCellValue.unset();
            checkBox.setSelected(false);
            checkBox.putClientProperty("JButton.selectedState", null);
            checkBox.setText("");
            checkBox.setToolTipText(null);
            checkBox.getAccessibleContext().setAccessibleName(null);
            checkBox.getAccessibleContext().setAccessibleDescription(null);
        }

        @Override
        public KeyStroke[] getKeyStrokes() {
            return new KeyStroke[]{KeyStroke.getKeyStroke("SPACE")};
        }

        private void showValue() {
            Object indeterminate = null;
            String text;
            if (value.explicitValue().orElse(null)
                    instanceof PropertyValue.BooleanValue explicit) {
                checkBox.setSelected(explicit.value());
                text = Boolean.toString(explicit.value());
            } else {
                // FlatLaf renders the native indeterminate mark; the explicit
                // text keeps the third state unambiguous on every NetBeans LAF.
                checkBox.setSelected(true);
                indeterminate = "indeterminate";
                text = FlutterPropertyCellValue.NOT_SET_TEXT;
            }
            checkBox.putClientProperty("JButton.selectedState", indeterminate);
            checkBox.setText(text);
            checkBox.getAccessibleContext().setAccessibleDescription(
                    "Boolean value " + text
                    + (binding.optional()
                            ? "; cycles through default, true, and false."
                            : "; toggles true or false."));
        }

        private static FlutterPropertyCellValue nextValue(
                FlutterPropertyCellValue current,
                boolean optional) {
            PropertyValue explicit = current.explicitValue().orElse(null);
            if (explicit == null) {
                return FlutterPropertyCellValue.explicit(
                        new PropertyValue.BooleanValue(true));
            }
            boolean selected = ((PropertyValue.BooleanValue) explicit).value();
            if (selected) {
                return FlutterPropertyCellValue.explicit(
                        new PropertyValue.BooleanValue(false));
            }
            return optional
                    ? FlutterPropertyCellValue.unset()
                    : FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(true));
        }
    }

    private static final class NumericInplaceEditor
            extends AbstractInplaceEditor {
        private final JTextField field = new JTextField();
        private boolean updating;

        NumericInplaceEditor(FlutterTypedPropertyEditors.Binding binding) {
            super(binding);
            field.setName(NUMERIC_COMPONENT_NAME);
            field.setColumns(12);
            // Catalog parsing remains exact BigInteger/BigDecimal. A standard
            // SpinnerNumberModel or NumberFormatter would narrow portable Dart
            // integers/decimals through primitive Number subclasses, and the
            // catalog intentionally declares no artificial increment step.
            field.addActionListener(ignored -> commitAndFire());
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    validateDraft();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    validateDraft();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    validateDraft();
                }
            });
        }

        @Override
        public JComponent getComponent() {
            return field;
        }

        @Override
        public String getValue() {
            validateDraft();
            return field.getText();
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof String text) {
                updating = true;
                try {
                    field.setText(text);
                } finally {
                    updating = false;
                }
                validateDraft();
                return;
            }
            show(binding.validate(cell(value)));
        }

        @Override
        public boolean supportsTextEntry() {
            return true;
        }

        @Override
        public void reset() {
            show(binding.validate(editorValue()));
        }

        @Override
        void clearEditorState() {
            updating = true;
            try {
                field.setText("");
            } finally {
                updating = false;
            }
            field.putClientProperty("JComponent.outline", null);
            field.setToolTipText(null);
            field.getAccessibleContext().setAccessibleName(null);
            field.getAccessibleContext().setAccessibleDescription(null);
        }

        @Override
        public KeyStroke[] getKeyStrokes() {
            return new KeyStroke[]{KeyStroke.getKeyStroke("ENTER")};
        }

        private void show(FlutterPropertyCellValue value) {
            updating = true;
            try {
                field.setText(format(binding, value));
                setValid(true, acceptedDescription(binding));
            } finally {
                updating = false;
            }
        }

        private void validateDraft() {
            if (updating) {
                return;
            }
            try {
                // Document replacement is remove+insert. Validate the
                // transient text for feedback, but publish it to the draft
                // only on Enter/focus commit so a temporary empty optional
                // field cannot erase the last valid numeric value.
                parse(binding, field.getText());
                setValid(true, acceptedDescription(binding));
            } catch (IllegalArgumentException failure) {
                setValid(false, failure.getMessage());
            }
        }

        private void commitAndFire() {
            try {
                parse(binding, field.getText());
                setValid(true, acceptedDescription(binding));
                fireSuccess();
            } catch (IllegalArgumentException failure) {
                setValid(false, failure.getMessage());
                Toolkit.getDefaultToolkit().beep();
                field.selectAll();
            }
        }

        private void setValid(boolean valid, String message) {
            field.putClientProperty("JComponent.outline", valid ? null : "error");
            field.setToolTipText(message);
            PropertyEnv environment = environment();
            if (environment != null) {
                environment.setState(valid
                        ? PropertyEnv.STATE_VALID : PropertyEnv.STATE_INVALID);
            }
            field.getAccessibleContext().setAccessibleDescription(
                    (valid ? "Numeric property. " : "Invalid numeric property. ")
                    + message);
        }
    }

    private abstract static class CommitOnValidPanel extends JPanel
            implements PropertyChangeListener {
        final PropertyEditor editor;
        final FlutterTypedPropertyEditors.Binding binding;
        final PropertyEnv environment;
        private FlutterPropertyCellValue draft;
        private boolean draftValid;
        private boolean committed;

        CommitOnValidPanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            this.editor = Objects.requireNonNull(editor, "editor");
            this.binding = Objects.requireNonNull(binding, "binding");
            this.environment = Objects.requireNonNull(environment, "environment");
            this.draft = binding.validate(cell(editor.getValue()));
            setBorder(new EmptyBorder(8, 8, 8, 8));
        }

        final FlutterPropertyCellValue initialValue() {
            return draft;
        }

        final void activate() {
            environment.addPropertyChangeListener(this);
            markValid(draft);
        }

        final void markValid(FlutterPropertyCellValue candidate) {
            draft = binding.validate(candidate);
            draftValid = true;
            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        }

        final void markInvalid(String message, JComponent component) {
            draftValid = false;
            environment.setState(PropertyEnv.STATE_INVALID);
            component.putClientProperty("JComponent.outline", "error");
            component.setToolTipText(message);
            component.getAccessibleContext().setAccessibleDescription(
                    "Invalid value. " + message);
        }

        final void clearInvalid(JComponent component, String description) {
            component.putClientProperty("JComponent.outline", null);
            component.setToolTipText(description);
            component.getAccessibleContext().setAccessibleDescription(description);
        }

        @Override
        public final void propertyChange(PropertyChangeEvent event) {
            if (PropertyEnv.PROP_STATE.equals(event.getPropertyName())
                    && event.getNewValue() == PropertyEnv.STATE_VALID
                    && draftValid
                    && !committed) {
                committed = true;
                editor.setValue(binding.validate(draft));
            }
        }
    }

    private static final class StringCustomEditor extends CommitOnValidPanel {
        private final JTextArea textArea = new JTextArea(10, 48);
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private boolean updating;

        StringCustomEditor(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setName("flutter.string.custom");
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " text editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits the exact Flutter String constructor argument.");

            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(false);
            textArea.setName("flutter.string.text");
            textArea.getAccessibleContext().setAccessibleName(
                    binding.definition().name().value());
            textArea.getAccessibleContext().setAccessibleDescription(
                    "Exact Flutter String value; whitespace and empty text are preserved.");
            add(new JScrollPane(textArea), BorderLayout.CENTER);

            if (binding.optional()) {
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "When selected, removes this constructor argument.");
                add(useDefault, BorderLayout.NORTH);
                useDefault.addActionListener(ignored -> updateDraft());
            }
            FlutterPropertyCellValue initial = initialValue();
            updating = true;
            try {
                boolean unset = initial.explicitValue().isEmpty();
                useDefault.setSelected(unset);
                textArea.setEnabled(!unset);
                textArea.setText(initial.explicitValue()
                        .map(PropertyValue.StringValue.class::cast)
                        .map(PropertyValue.StringValue::value)
                        .orElse(""));
                textArea.setCaretPosition(0);
            } finally {
                updating = false;
            }
            textArea.getDocument().addDocumentListener(
                    documentListener(this::updateDraft));
            activate();
        }

        private void updateDraft() {
            if (updating) {
                return;
            }
            boolean unset = binding.optional() && useDefault.isSelected();
            textArea.setEnabled(!unset);
            markValid(unset
                    ? FlutterPropertyCellValue.unset()
                    : FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue(textArea.getText())));
        }
    }

    private static final class EdgeInsetsCustomEditor
            extends CommitOnValidPanel {
        private static final String[] NAMES = {"left", "top", "right", "bottom"};
        private final JTextField[] fields = new JTextField[4];
        private final JTextField all = new JTextField(12);
        private final JButton applyAll = new JButton();
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private boolean updating;

        EdgeInsetsCustomEditor(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setName("flutter.edgeInsets.custom");
            setPreferredSize(new Dimension(430, 250));
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " edge insets editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits physical left, top, right, and bottom Flutter EdgeInsets.");

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 0;

            for (int index = 0; index < fields.length; index++) {
                JLabel label = new JLabel(capitalize(NAMES[index]) + ':');
                JTextField field = new JTextField(14);
                field.setName("flutter.edgeInsets." + NAMES[index]);
                label.setLabelFor(field);
                field.getAccessibleContext().setAccessibleName(
                        capitalize(NAMES[index]) + " inset");
                field.getAccessibleContext().setAccessibleDescription(
                        "Non-negative physical " + NAMES[index]
                        + " padding in logical pixels.");
                constraints.gridx = 0;
                constraints.gridy = index;
                constraints.weightx = 0;
                form.add(label, constraints);
                constraints.gridx = 1;
                constraints.weightx = 1;
                form.add(field, constraints);
                fields[index] = field;
            }

            JLabel allLabel = new JLabel("All sides:");
            all.setName(EDGE_ALL_NAME);
            allLabel.setLabelFor(all);
            all.getAccessibleContext().setAccessibleName("All edge insets");
            all.getAccessibleContext().setAccessibleDescription(
                    "A non-negative value to copy to all four physical sides.");
            applyAll.setText("Apply to all");
            applyAll.getAccessibleContext().setAccessibleDescription(
                    "Copies the All sides value to left, top, right, and bottom.");
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.weightx = 0;
            form.add(allLabel, constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            form.add(all, constraints);
            constraints.gridx = 2;
            constraints.weightx = 0;
            form.add(applyAll, constraints);
            add(form, BorderLayout.CENTER);

            if (binding.optional()) {
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "When selected, removes this constructor argument.");
                add(useDefault, BorderLayout.NORTH);
                useDefault.addActionListener(ignored -> validateSides());
            }

            PropertyValue.EdgeInsetsValue initial = initialValue().explicitValue()
                    .filter(PropertyValue.EdgeInsetsValue.class::isInstance)
                    .map(PropertyValue.EdgeInsetsValue.class::cast)
                    .orElseGet(() -> new PropertyValue.EdgeInsetsValue(
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO));
            updating = true;
            try {
                useDefault.setSelected(initialValue().explicitValue().isEmpty());
                fields[0].setText(initial.left().toPlainString());
                fields[1].setText(initial.top().toPlainString());
                fields[2].setText(initial.right().toPlainString());
                fields[3].setText(initial.bottom().toPlainString());
                all.setText(allEqual(initial)
                        ? initial.left().toPlainString() : "");
                setFieldsEnabled(!useDefault.isSelected());
            } finally {
                updating = false;
            }

            for (JTextField field : fields) {
                field.getDocument().addDocumentListener(
                        documentListener(this::validateSides));
            }
            applyAll.addActionListener(ignored -> applyAll());
            all.addActionListener(ignored -> applyAll());
            activate();
        }

        private void applyAll() {
            if (binding.optional() && useDefault.isSelected()) {
                return;
            }
            try {
                BigDecimal value = new BigDecimal(all.getText().strip());
                FlutterPropertyCellValue candidate = FlutterPropertyCellValue.explicit(
                        new PropertyValue.EdgeInsetsValue(
                                value, value, value, value));
                binding.validate(candidate);
                updating = true;
                try {
                    for (JTextField field : fields) {
                        field.setText(value.toPlainString());
                    }
                } finally {
                    updating = false;
                }
                for (int index = 0; index < fields.length; index++) {
                    clearInvalid(fields[index],
                            "Non-negative physical " + NAMES[index]
                            + " padding in logical pixels.");
                }
                clearInvalid(all, "Copies one non-negative value to all sides.");
                markValid(candidate);
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), all);
            }
        }

        private void validateSides() {
            if (updating) {
                return;
            }
            if (binding.optional() && useDefault.isSelected()) {
                setFieldsEnabled(false);
                for (int index = 0; index < fields.length; index++) {
                    clearInvalid(fields[index],
                            "Non-negative physical " + NAMES[index]
                            + " padding in logical pixels.");
                }
                clearInvalid(all, "Copies one non-negative value to all sides.");
                markValid(FlutterPropertyCellValue.unset());
                return;
            }
            setFieldsEnabled(true);
            BigDecimal[] values = new BigDecimal[fields.length];
            boolean valid = true;
            for (int index = 0; index < fields.length; index++) {
                try {
                    values[index] = new BigDecimal(fields[index].getText().strip());
                    if (values[index].signum() < 0) {
                        throw new IllegalArgumentException(
                                capitalize(NAMES[index])
                                + " inset must be non-negative.");
                    }
                    clearInvalid(fields[index],
                            "Non-negative physical " + NAMES[index]
                            + " padding in logical pixels.");
                } catch (NumberFormatException failure) {
                    valid = false;
                    markInvalid(capitalize(NAMES[index])
                            + " inset must be a decimal number.", fields[index]);
                } catch (IllegalArgumentException failure) {
                    valid = false;
                    markInvalid(failure.getMessage(), fields[index]);
                }
            }
            if (!valid) {
                return;
            }
            try {
                FlutterPropertyCellValue candidate = FlutterPropertyCellValue.explicit(
                        new PropertyValue.EdgeInsetsValue(
                                values[0], values[1], values[2], values[3]));
                binding.validate(candidate);
                for (JTextField field : fields) {
                    clearInvalid(field,
                            "Non-negative physical padding in logical pixels.");
                }
                clearInvalid(all, "Copies one non-negative value to all sides.");
                markValid(candidate);
            } catch (IllegalArgumentException failure) {
                for (JTextField field : fields) {
                    markInvalid(failure.getMessage(), field);
                }
            }
        }

        private void setFieldsEnabled(boolean enabled) {
            for (JTextField field : fields) {
                field.setEnabled(enabled);
            }
            all.setEnabled(enabled);
            applyAll.setEnabled(enabled);
        }

        private static boolean allEqual(PropertyValue.EdgeInsetsValue value) {
            return value.left().compareTo(value.top()) == 0
                    && value.left().compareTo(value.right()) == 0
                    && value.left().compareTo(value.bottom()) == 0;
        }
    }

    private static final class ColorCustomEditor extends CommitOnValidPanel {
        private final JColorChooser chooser = new JColorChooser();
        private final JTextField argb = new JTextField(12);
        private final JSpinner alpha = new JSpinner(
                new SpinnerNumberModel(255, 0, 255, 1));
        private final JTextField alphaText;
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private boolean updating;

        ColorCustomEditor(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setName("flutter.color.custom");
            setPreferredSize(new Dimension(670, 480));
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " ARGB color editor");
            getAccessibleContext().setAccessibleDescription(
                    "Chooses an exact Flutter ARGB color, including alpha transparency.");

            chooser.setName(COLOR_CHOOSER_NAME);
            chooser.getAccessibleContext().setAccessibleName("RGB color chooser");
            chooser.getAccessibleContext().setAccessibleDescription(
                    "Chooses the red, green, and blue color channels.");
            chooser.setPreviewPanel(new JPanel());
            add(chooser, BorderLayout.CENTER);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
            JLabel argbLabel = new JLabel("ARGB:");
            argb.setName(COLOR_ARGB_NAME);
            argbLabel.setLabelFor(argb);
            argb.getAccessibleContext().setAccessibleName("ARGB hexadecimal value");
            argb.getAccessibleContext().setAccessibleDescription(
                    "Exact eight-digit Flutter color in 0xAARRGGBB form.");
            JLabel alphaLabel = new JLabel("Alpha (0–255):");
            alpha.setName(COLOR_ALPHA_NAME);
            alphaText = ((JSpinner.DefaultEditor) alpha.getEditor()).getTextField();
            alphaLabel.setLabelFor(alpha);
            alpha.getAccessibleContext().setAccessibleName("Alpha channel");
            alpha.getAccessibleContext().setAccessibleDescription(
                    "Transparency from 0 fully transparent to 255 fully opaque.");
            controls.add(argbLabel);
            controls.add(argb);
            controls.add(alphaLabel);
            controls.add(alpha);
            if (binding.optional()) {
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "When selected, removes this constructor argument.");
                controls.add(useDefault);
            }
            add(controls, BorderLayout.NORTH);

            PropertyValue.ColorValue initial = initialValue().explicitValue()
                    .filter(PropertyValue.ColorValue.class::isInstance)
                    .map(PropertyValue.ColorValue.class::cast)
                    .orElseGet(() -> PropertyValue.ColorValue.fromWireArgb(
                            "0xFF000000"));
            updating = true;
            try {
                useDefault.setSelected(initialValue().explicitValue().isEmpty());
                showColor(initial);
                setColorControlsEnabled(!useDefault.isSelected());
            } finally {
                updating = false;
            }

            argb.getDocument().addDocumentListener(
                    documentListener(this::argbChanged));
            chooser.getSelectionModel().addChangeListener(
                    ignored -> chooserChanged());
            alpha.addChangeListener(ignored -> alphaModelChanged());
            alphaText.getDocument().addDocumentListener(
                    documentListener(this::alphaTextChanged));
            useDefault.addActionListener(ignored -> defaultChanged());
            activate();
        }

        private void argbChanged() {
            if (updating) {
                return;
            }
            try {
                PropertyValue.ColorValue value = PropertyValue.ColorValue
                        .fromWireArgb(normalizeArgb(argb.getText()));
                updating = true;
                try {
                    chooser.setColor(toAwtColor(value));
                    alpha.setValue((int) ((value.argb() >>> 24) & 0xFF));
                } finally {
                    updating = false;
                }
                clearInvalid(argb,
                        "Exact eight-digit Flutter color in 0xAARRGGBB form.");
                clearInvalid(alphaText, alphaDescription());
                markValid(FlutterPropertyCellValue.explicit(value));
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), argb);
            }
        }

        private void chooserChanged() {
            if (updating || (binding.optional() && useDefault.isSelected())) {
                return;
            }
            try {
                updateFromChannels(parseAlphaText());
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), alphaText);
            }
        }

        private void alphaModelChanged() {
            if (updating || (binding.optional() && useDefault.isSelected())) {
                return;
            }
            updateFromChannels(((Number) alpha.getValue()).intValue());
        }

        private void alphaTextChanged() {
            if (updating || (binding.optional() && useDefault.isSelected())) {
                return;
            }
            try {
                updateFromChannels(parseAlphaText());
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), alphaText);
            }
        }

        private void updateFromChannels(int alphaValue) {
            Color rgb = chooser.getColor();
            long value = ((long) alphaValue << 24)
                    | ((long) rgb.getRed() << 16)
                    | ((long) rgb.getGreen() << 8)
                    | rgb.getBlue();
            PropertyValue.ColorValue color = new PropertyValue.ColorValue(value);
            updating = true;
            try {
                argb.setText(color.wireArgb());
            } finally {
                updating = false;
            }
            clearInvalid(argb,
                    "Exact eight-digit Flutter color in 0xAARRGGBB form.");
            clearInvalid(alphaText, alphaDescription());
            markValid(FlutterPropertyCellValue.explicit(color));
        }

        private void defaultChanged() {
            if (updating) {
                return;
            }
            boolean useInherited = binding.optional() && useDefault.isSelected();
            setColorControlsEnabled(!useInherited);
            if (useInherited) {
                clearInvalid(argb,
                        "Exact eight-digit Flutter color in 0xAARRGGBB form.");
                clearInvalid(alphaText, alphaDescription());
                markValid(FlutterPropertyCellValue.unset());
            } else {
                alphaTextChanged();
            }
        }

        private void showColor(PropertyValue.ColorValue value) {
            argb.setText(value.wireArgb());
            chooser.setColor(toAwtColor(value));
            alpha.setValue((int) ((value.argb() >>> 24) & 0xFF));
        }

        private void setColorControlsEnabled(boolean enabled) {
            setEnabledRecursively(chooser, enabled);
            argb.setEnabled(enabled);
            alpha.setEnabled(enabled);
        }

        private int parseAlphaText() {
            String text = alphaText.getText().strip();
            final int value;
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException(
                        "Alpha must be an integer from 0 through 255.", failure);
            }
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException(
                        "Alpha must be an integer from 0 through 255.");
            }
            return value;
        }

        private static String alphaDescription() {
            return "Integer alpha from 0 (fully transparent) through 255 "
                    + "(fully opaque).";
        }
    }

    private static DocumentListener documentListener(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
    }

    private static void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    private static String normalizeArgb(String text) {
        String normalized = Objects.requireNonNull(text, "text").strip();
        if (normalized.length() >= 2
                && normalized.substring(0, 2).equalsIgnoreCase("0x")) {
            return "0x" + normalized.substring(2).toUpperCase(Locale.ROOT);
        }
        return normalized;
    }

    private static Color toAwtColor(PropertyValue.ColorValue value) {
        int alpha = (int) ((value.argb() >>> 24) & 0xFF);
        int red = (int) ((value.argb() >>> 16) & 0xFF);
        int green = (int) ((value.argb() >>> 8) & 0xFF);
        int blue = (int) (value.argb() & 0xFF);
        return new Color(red, green, blue, alpha);
    }

    private static String acceptedDescription(
            FlutterTypedPropertyEditors.Binding binding) {
        return binding.definition().constraints().stream()
                .map(PropertyValueConstraint::description)
                .reduce((left, right) -> left + "; " + right)
                .map(value -> "Accepted: " + value + '.')
                .orElse("Enter a catalog-approved Flutter value.");
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
