package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import javax.swing.JComboBox;
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
    static final String NEWLINE_LIST_TEXT_NAME = "flutter.newlineList.text";

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
            case STRING, NEWLINE_STRING_LIST, EDGE_INSETS, COLOR,
                    THEME_COLOR, PAINT, SHADOW_LIST, FONT_FEATURE_LIST,
                    FONT_VARIATION_LIST -> true;
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
            case NEWLINE_STRING_LIST -> new NewlineListCustomEditor(
                    editor, binding, environment);
            case EDGE_INSETS -> new EdgeInsetsCustomEditor(
                    editor, binding, environment);
            case COLOR -> new ColorCustomEditor(editor, binding, environment);
            case THEME_COLOR, PAINT, SHADOW_LIST, FONT_FEATURE_LIST,
                    FONT_VARIATION_LIST -> FlutterComplexPropertyEditorComponents
                    .customEditor(editor, binding, environment);
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
        } else if (cell.explicitValue().orElse(null)
                instanceof PropertyValue.ThemeTokenValue theme) {
            graphics.setColor(original);
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString("Theme: "
                    + FlutterThemePropertyRoles.displayRole(theme.token()), x,
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
                // "<not set>" is presentation text for the inactive property
                // cell, not an editable numeric literal. Start an optional
                // unset value with an empty draft so the user can type the
                // number immediately without first deleting the placeholder.
                field.setText(value.isExplicit() ? format(binding, value) : "");
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

    abstract static class CommitOnValidPanel extends JPanel
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
            stageValid(candidate);
            environment.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        }

        /** Stores a validated draft while an existing OK validation is in progress. */
        final void stageValid(FlutterPropertyCellValue candidate) {
            draft = binding.validate(candidate);
            draftValid = true;
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

        /** Gives compound controls a last chance to finish their local cell edit. */
        boolean prepareCommit() {
            return true;
        }

        @Override
        public final void propertyChange(PropertyChangeEvent event) {
            if (PropertyEnv.PROP_STATE.equals(event.getPropertyName())
                    && event.getNewValue() == PropertyEnv.STATE_VALID
                    && !committed
                    && prepareCommit()
                    && draftValid) {
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

    private static final class NewlineListCustomEditor
            extends CommitOnValidPanel {
        private final JTextArea textArea = new JTextArea(12, 48);
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private boolean updating;

        NewlineListCustomEditor(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setName("flutter.newlineList.custom");
            setPreferredSize(new Dimension(520, 320));
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " font fallback editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits an ordered list of Flutter font families, one family per line.");

            JLabel instruction = new JLabel("One font family per line");
            instruction.setLabelFor(textArea);
            textArea.setLineWrap(false);
            textArea.setName(NEWLINE_LIST_TEXT_NAME);
            textArea.getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " font families");
            textArea.getAccessibleContext().setAccessibleDescription(
                    "Ordered Flutter font fallback families; blank lines are ignored.");
            JPanel content = new JPanel(new BorderLayout(0, 4));
            content.add(instruction, BorderLayout.NORTH);
            content.add(new JScrollPane(textArea), BorderLayout.CENTER);
            add(content, BorderLayout.CENTER);

            if (binding.optional()) {
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "When selected, removes this font fallback list.");
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
                            new PropertyValue.StringValue(
                                    FlutterTypedPropertyEditors.normalizeNewlineList(
                                            textArea.getText()))));
        }
    }

    private static final class EdgeInsetsCustomEditor
            extends CommitOnValidPanel {
        private enum Mode {
            ALL("All sides"),
            SYMMETRIC("Symmetric"),
            PHYSICAL("Physical (left/right)"),
            DIRECTIONAL("Directional (start/end)");

            private final String displayName;

            Mode(String displayName) {
                this.displayName = displayName;
            }

            @Override
            public String toString() {
                return displayName;
            }
        }

        private static final String[] PHYSICAL_NAMES = {
            "left", "top", "right", "bottom"
        };
        private static final String[] DIRECTIONAL_NAMES = {
            "start", "top", "end", "bottom"
        };
        private final JComboBox<Mode> mode = new JComboBox<>(Mode.values());
        private final CardLayout cardLayout = new CardLayout();
        private final JPanel cards = new JPanel(cardLayout);
        private final JTextField all = new JTextField(12);
        private final JTextField horizontal = new JTextField(12);
        private final JTextField vertical = new JTextField(12);
        private final JTextField[] physical = fields("", PHYSICAL_NAMES);
        private final JTextField[] directional = fields("directional", DIRECTIONAL_NAMES);
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private Mode activeMode;
        private boolean updating;

        EdgeInsetsCustomEditor(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(0, 8));
            setName("flutter.edgeInsets.custom");
            setPreferredSize(new Dimension(470, 290));
            getAccessibleContext().setAccessibleName(
                    binding.definition().name().value() + " edge insets editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits all, symmetric, physical, or text-direction-aware "
                    + "Flutter edge insets.");

            JPanel header = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = constraints();
            JLabel modeLabel = new JLabel("Mode:");
            mode.setName("flutter.edgeInsets.mode");
            modeLabel.setLabelFor(mode);
            mode.getAccessibleContext().setAccessibleName("Edge insets mode");
            mode.getAccessibleContext().setAccessibleDescription(
                    "Select all, symmetric, physical left/right, or "
                    + "directional start/end padding.");
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;
            header.add(modeLabel, constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            header.add(mode, constraints);
            if (binding.optional()) {
                constraints.gridx = 0;
                constraints.gridy = 1;
                constraints.gridwidth = 2;
                header.add(useDefault, constraints);
                useDefault.getAccessibleContext().setAccessibleDescription(
                        "When selected, removes this constructor argument.");
            }
            add(header, BorderLayout.NORTH);

            all.setName(EDGE_ALL_NAME);
            all.getAccessibleContext().setAccessibleName("All edge insets");
            all.getAccessibleContext().setAccessibleDescription(
                    "One non-negative padding value for every side.");
            horizontal.setName("flutter.edgeInsets.horizontal");
            vertical.setName("flutter.edgeInsets.vertical");
            cards.add(singleFieldPanel("All sides:", all), Mode.ALL.name());
            cards.add(twoFieldPanel(), Mode.SYMMETRIC.name());
            cards.add(sidePanel(PHYSICAL_NAMES, physical, false), Mode.PHYSICAL.name());
            cards.add(sidePanel(DIRECTIONAL_NAMES, directional, true),
                    Mode.DIRECTIONAL.name());
            add(cards, BorderLayout.CENTER);

            PropertyValue initial = initialValue().explicitValue().orElseGet(
                    () -> new PropertyValue.EdgeInsetsValue(
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO));
            activeMode = initialMode(initial);
            updating = true;
            try {
                useDefault.setSelected(initialValue().explicitValue().isEmpty());
                populate(initial);
                mode.setSelectedItem(activeMode);
                cardLayout.show(cards, activeMode.name());
                setEditorEnabled(!useDefault.isSelected());
            } finally {
                updating = false;
            }

            for (JTextField field : allFields()) {
                field.getDocument().addDocumentListener(
                        documentListener(this::validateActive));
            }
            mode.addActionListener(ignored -> modeChanged());
            useDefault.addActionListener(ignored -> validateActive());
            activate();
        }

        private void modeChanged() {
            if (updating) {
                return;
            }
            Mode selected = (Mode) mode.getSelectedItem();
            if (selected == null || selected == activeMode) {
                return;
            }
            candidate(activeMode).ifPresent(value -> {
                updating = true;
                try {
                    populate(value);
                } finally {
                    updating = false;
                }
            });
            activeMode = selected;
            cardLayout.show(cards, activeMode.name());
            validateActive();
        }

        private void validateActive() {
            if (updating) {
                return;
            }
            boolean unset = binding.optional() && useDefault.isSelected();
            setEditorEnabled(!unset);
            if (unset) {
                clearAllInvalid();
                markValid(FlutterPropertyCellValue.unset());
                return;
            }
            Optional<PropertyValue> parsed = candidate(activeMode);
            if (parsed.isEmpty()) {
                return;
            }
            try {
                FlutterPropertyCellValue candidate = FlutterPropertyCellValue.explicit(
                        parsed.orElseThrow());
                binding.validate(candidate);
                clearAllInvalid();
                markValid(candidate);
            } catch (IllegalArgumentException failure) {
                for (JTextField field : activeFields(activeMode)) {
                    markInvalid(failure.getMessage(), field);
                }
            }
        }

        private Optional<PropertyValue> candidate(Mode selected) {
            JTextField[] selectedFields = activeFields(selected);
            BigDecimal[] values = new BigDecimal[selectedFields.length];
            boolean valid = true;
            for (int index = 0; index < selectedFields.length; index++) {
                JTextField field = selectedFields[index];
                String label = activeLabels(selected)[index];
                try {
                    values[index] = new BigDecimal(field.getText().strip());
                    if (values[index].signum() < 0) {
                        throw new IllegalArgumentException(
                                label + " inset must be non-negative.");
                    }
                    clearInvalid(field, label
                            + " padding in non-negative logical pixels.");
                } catch (NumberFormatException failure) {
                    valid = false;
                    markInvalid(label + " inset must be a decimal number.", field);
                } catch (IllegalArgumentException failure) {
                    valid = false;
                    markInvalid(failure.getMessage(), field);
                }
            }
            if (!valid) {
                return Optional.empty();
            }
            return Optional.of(switch (selected) {
                case ALL -> new PropertyValue.EdgeInsetsValue(
                        values[0], values[0], values[0], values[0]);
                case SYMMETRIC -> new PropertyValue.EdgeInsetsValue(
                        values[0], values[1], values[0], values[1]);
                case PHYSICAL -> new PropertyValue.EdgeInsetsValue(
                        values[0], values[1], values[2], values[3]);
                case DIRECTIONAL -> new PropertyValue.EdgeInsetsDirectionalValue(
                        values[0], values[1], values[2], values[3]);
            });
        }

        private void populate(PropertyValue value) {
            BigDecimal first;
            BigDecimal top;
            BigDecimal third;
            BigDecimal bottom;
            if (value instanceof PropertyValue.EdgeInsetsValue insets) {
                first = insets.left();
                top = insets.top();
                third = insets.right();
                bottom = insets.bottom();
            } else if (value instanceof PropertyValue.EdgeInsetsDirectionalValue insets) {
                first = insets.start();
                top = insets.top();
                third = insets.end();
                bottom = insets.bottom();
            } else {
                throw new IllegalArgumentException("Expected Flutter edge insets.");
            }
            String[] values = {
                first.toPlainString(), top.toPlainString(),
                third.toPlainString(), bottom.toPlainString()
            };
            for (int index = 0; index < values.length; index++) {
                physical[index].setText(values[index]);
                directional[index].setText(values[index]);
            }
            all.setText(first.toPlainString());
            horizontal.setText(first.toPlainString());
            vertical.setText(top.toPlainString());
        }

        private void setEditorEnabled(boolean enabled) {
            mode.setEnabled(enabled);
            for (JTextField field : allFields()) {
                field.setEnabled(enabled);
            }
        }

        private void clearAllInvalid() {
            for (JTextField field : allFields()) {
                clearInvalid(field, "Non-negative padding in logical pixels.");
            }
        }

        private JTextField[] activeFields(Mode selected) {
            return switch (selected) {
                case ALL -> new JTextField[]{all};
                case SYMMETRIC -> new JTextField[]{horizontal, vertical};
                case PHYSICAL -> physical;
                case DIRECTIONAL -> directional;
            };
        }

        private String[] activeLabels(Mode selected) {
            return switch (selected) {
                case ALL -> new String[]{"All sides"};
                case SYMMETRIC -> new String[]{"Horizontal", "Vertical"};
                case PHYSICAL -> new String[]{"Left", "Top", "Right", "Bottom"};
                case DIRECTIONAL -> new String[]{"Start", "Top", "End", "Bottom"};
            };
        }

        private JTextField[] allFields() {
            JTextField[] result = new JTextField[11];
            result[0] = all;
            result[1] = horizontal;
            result[2] = vertical;
            System.arraycopy(physical, 0, result, 3, 4);
            System.arraycopy(directional, 0, result, 7, 4);
            return result;
        }

        private static Mode initialMode(PropertyValue value) {
            if (value instanceof PropertyValue.EdgeInsetsDirectionalValue) {
                return Mode.DIRECTIONAL;
            }
            PropertyValue.EdgeInsetsValue physical =
                    (PropertyValue.EdgeInsetsValue) value;
            if (physical.left().compareTo(physical.top()) == 0
                    && physical.left().compareTo(physical.right()) == 0
                    && physical.left().compareTo(physical.bottom()) == 0) {
                return Mode.ALL;
            }
            if (physical.left().compareTo(physical.right()) == 0
                    && physical.top().compareTo(physical.bottom()) == 0) {
                return Mode.SYMMETRIC;
            }
            return Mode.PHYSICAL;
        }

        private JPanel singleFieldPanel(String labelText, JTextField field) {
            return fieldPanel(new String[]{labelText}, new JTextField[]{field});
        }

        private JPanel twoFieldPanel() {
            horizontal.getAccessibleContext().setAccessibleDescription(
                    "Non-negative horizontal padding in logical pixels.");
            vertical.getAccessibleContext().setAccessibleDescription(
                    "Non-negative vertical padding in logical pixels.");
            return fieldPanel(
                    new String[]{"Horizontal:", "Vertical:"},
                    new JTextField[]{horizontal, vertical});
        }

        private JPanel sidePanel(
                String[] names,
                JTextField[] fields,
                boolean textDirectionAware) {
            String[] labels = new String[names.length];
            for (int index = 0; index < names.length; index++) {
                labels[index] = capitalize(names[index]) + ':';
                fields[index].getAccessibleContext().setAccessibleDescription(
                        "Non-negative "
                        + (textDirectionAware ? "directional " : "physical ")
                        + names[index] + " padding in logical pixels.");
            }
            return fieldPanel(labels, fields);
        }

        private JPanel fieldPanel(String[] labels, JTextField[] fields) {
            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = constraints();
            for (int index = 0; index < fields.length; index++) {
                JLabel label = new JLabel(labels[index]);
                label.setLabelFor(fields[index]);
                constraints.gridx = 0;
                constraints.gridy = index;
                constraints.weightx = 0;
                form.add(label, constraints);
                constraints.gridx = 1;
                constraints.weightx = 1;
                form.add(fields[index], constraints);
            }
            return form;
        }

        private static GridBagConstraints constraints() {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            return constraints;
        }

        private static JTextField[] fields(String prefix, String[] names) {
            JTextField[] result = new JTextField[names.length];
            for (int index = 0; index < names.length; index++) {
                JTextField field = new JTextField(14);
                field.setName("flutter.edgeInsets."
                        + (prefix.isEmpty() ? "" : prefix + '.') + names[index]);
                field.getAccessibleContext().setAccessibleName(
                        capitalize(names[index]) + " inset");
                result[index] = field;
            }
            return result;
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
