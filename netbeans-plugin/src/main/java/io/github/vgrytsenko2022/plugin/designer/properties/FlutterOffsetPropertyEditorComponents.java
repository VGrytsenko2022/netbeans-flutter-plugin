package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.DartNumericLiterals;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Transactional editor for the closed, typed Flutter {@code Offset} value. */
final class FlutterOffsetPropertyEditorComponents {
    static final String FRACTIONAL_COORDINATES_ATTRIBUTE = "flutter.offset.fractionalCoordinates";
    static final String CUSTOM_EDITOR_NAME = "flutter.offset.custom";
    static final String USE_DEFAULT_COMPONENT_NAME = "flutter.offset.useDefault";
    static final String DX_COMPONENT_NAME = "flutter.offset.dx";
    static final String DY_COMPONENT_NAME = "flutter.offset.dy";

    private FlutterOffsetPropertyEditorComponents() {
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.OFFSET) {
            throw new IllegalStateException(
                    "No Offset editor for " + binding.editorKind());
        }
        return new OffsetPanel(editor, binding, environment);
    }

    private static final class OffsetPanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JCheckBox useDefault = new JCheckBox(
                "Use inherited/default value (omit argument)");
        private final JTextField dx = new JTextField(14);
        private final JTextField dy = new JTextField(14);
        private boolean updating;
        private final boolean fractional;

        OffsetPanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            fractional = environment.getFeatureDescriptor() != null && Boolean.TRUE.equals(
                    environment.getFeatureDescriptor().getValue(FRACTIONAL_COORDINATES_ATTRIBUTE));
            setLayout(new GridBagLayout());
            setPreferredSize(new Dimension(430, 180));
            setName(CUSTOM_EDITOR_NAME);
            getAccessibleContext().setAccessibleName("Flutter Offset editor");
            getAccessibleContext().setAccessibleDescription(
                    (fractional ? "Edits finite signed fractions of child width and height " : "Edits finite signed horizontal and vertical logical-pixel offsets ")
                    + "without changing the selected property until OK is accepted.");

            useDefault.setName(USE_DEFAULT_COMPONENT_NAME);
            useDefault.getAccessibleContext().setAccessibleName(
                    "Use default Offset value");
            useDefault.getAccessibleContext().setAccessibleDescription(
                    "When selected, removes this optional constructor argument.");
            dx.setName(DX_COMPONENT_NAME);
            dy.setName(DY_COMPONENT_NAME);
            dx.getAccessibleContext().setAccessibleName("Offset horizontal delta");
            dy.getAccessibleContext().setAccessibleName("Offset vertical delta");

            int row = 0;
            if (binding.optional()) {
                addWideRow(this, row++, useDefault);
            }
            addRow(this, row++, fractional ? "Width fraction (dx):" : "Horizontal (dx):", dx);
            addRow(this, row, fractional ? "Height fraction (dy):" : "Vertical (dy):", dy);

            PropertyValue.OffsetValue value = initialValue().explicitValue()
                    .map(PropertyValue.OffsetValue.class::cast)
                    .orElseGet(() -> new PropertyValue.OffsetValue(
                    BigDecimal.ZERO, BigDecimal.ZERO));
            updating = true;
            try {
                useDefault.setSelected(initialValue().explicitValue().isEmpty());
                dx.setText(value.dx().toPlainString());
                dy.setText(value.dy().toPlainString());
            } finally {
                updating = false;
            }
            useDefault.addActionListener(ignored -> refresh());
            dx.getDocument().addDocumentListener(listener(this::refresh));
            dy.getDocument().addDocumentListener(listener(this::refresh));
            refresh();
            activate();
        }

        @Override
        boolean prepareCommit() {
            return refreshDraft(false);
        }

        private void refresh() {
            if (!updating) {
                refreshDraft(true);
            }
        }

        private boolean refreshDraft(boolean requestValidation) {
            boolean unset = binding.optional() && useDefault.isSelected();
            dx.setEnabled(!unset);
            dy.setEnabled(!unset);
            clear(dx, fractional ? "Finite signed fraction of child width, not pixels." : "Finite signed horizontal logical-pixel offset.");
            clear(dy, fractional ? "Finite signed fraction of child height, not pixels." : "Finite signed vertical logical-pixel offset.");
            try {
                FlutterPropertyCellValue candidate = unset
                        ? FlutterPropertyCellValue.unset()
                        : FlutterPropertyCellValue.explicit(
                                new PropertyValue.OffsetValue(
                                        coordinate(dx, "Horizontal offset"),
                                        coordinate(dy, "Vertical offset")));
                if (requestValidation) {
                    markValid(candidate);
                } else {
                    stageValid(candidate);
                }
                return true;
            } catch (InvalidCoordinate failure) {
                markInvalid(failure.getMessage(), failure.component());
                return false;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
                return false;
            }
        }

        private static void clear(JTextField field, String description) {
            field.putClientProperty("JComponent.outline", null);
            field.setToolTipText(description);
            field.getAccessibleContext().setAccessibleDescription(description);
        }

        private static BigDecimal coordinate(JTextField field, String label) {
            String text = field.getText().strip();
            final BigDecimal value;
            try {
                value = new BigDecimal(text);
            } catch (NumberFormatException failure) {
                throw new InvalidCoordinate(field,
                        label + " must be a finite decimal number.", failure);
            }
            if (!DartNumericLiterals.isRepresentableDouble(value)) {
                throw new InvalidCoordinate(field,
                        label + " must be exactly representable as a finite Dart double.");
            }
            return value;
        }
    }

    private static void addWideRow(
            JPanel panel, int row, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 8, 5);
        panel.add(component, constraints);
    }

    private static void addRow(
            JPanel panel, int row, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setLabelFor(field);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(5, 5, 5, 8);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 5, 5, 5);
        panel.add(field, fieldConstraints);
    }

    private static DocumentListener listener(Runnable action) {
        Objects.requireNonNull(action, "action");
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

    private static final class InvalidCoordinate extends IllegalArgumentException {
        private final JComponent component;

        InvalidCoordinate(JComponent component, String message) {
            super(message);
            this.component = Objects.requireNonNull(component, "component");
        }

        InvalidCoordinate(JComponent component, String message, Throwable cause) {
            super(message, cause);
            this.component = Objects.requireNonNull(component, "component");
        }

        JComponent component() {
            return component;
        }
    }
}
