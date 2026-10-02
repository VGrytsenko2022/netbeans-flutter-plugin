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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Transactional editor for the closed, typed Flutter {@code Size} value. */
final class FlutterSizePropertyEditorComponents {
    static final String CUSTOM_EDITOR_NAME = "flutter.size.custom";
    static final String WIDTH_COMPONENT_NAME = "flutter.size.width";
    static final String HEIGHT_COMPONENT_NAME = "flutter.size.height";

    private FlutterSizePropertyEditorComponents() {
    }

    static Component customEditor(
            PropertyEditor editor,
            FlutterTypedPropertyEditors.Binding binding,
            PropertyEnv environment) {
        if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.SIZE) {
            throw new IllegalStateException(
                    "No Size editor for " + binding.editorKind());
        }
        return new SizePanel(editor, binding, environment);
    }

    private static final class SizePanel
            extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JTextField width = new JTextField(14);
        private final JTextField height = new JTextField(14);
        private boolean updating;

        SizePanel(
                PropertyEditor editor,
                FlutterTypedPropertyEditors.Binding binding,
                PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new GridBagLayout());
            setPreferredSize(new Dimension(420, 150));
            setName(CUSTOM_EDITOR_NAME);
            getAccessibleContext().setAccessibleName("Flutter Size editor");
            getAccessibleContext().setAccessibleDescription(
                    "Edits required finite non-negative width and height dimensions "
                    + "without changing the selected property until OK is accepted.");

            width.setName(WIDTH_COMPONENT_NAME);
            height.setName(HEIGHT_COMPONENT_NAME);
            width.getAccessibleContext().setAccessibleName("Size width");
            height.getAccessibleContext().setAccessibleName("Size height");
            addRow(this, 0, "Width:", width);
            addRow(this, 1, "Height:", height);

            PropertyValue.SizeValue value = initialValue().explicitValue()
                    .map(PropertyValue.SizeValue.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Required Size property cannot be unset."));
            updating = true;
            try {
                width.setText(value.width().toPlainString());
                height.setText(value.height().toPlainString());
            } finally {
                updating = false;
            }
            width.getDocument().addDocumentListener(listener(this::refresh));
            height.getDocument().addDocumentListener(listener(this::refresh));
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
            clear(width, "Finite non-negative logical-pixel width.");
            clear(height, "Finite non-negative logical-pixel height.");
            try {
                BigDecimal parsedWidth = dimension(width, "Width");
                BigDecimal parsedHeight = dimension(height, "Height");
                FlutterPropertyCellValue candidate = FlutterPropertyCellValue.explicit(
                        new PropertyValue.SizeValue(parsedWidth, parsedHeight));
                if (requestValidation) {
                    markValid(candidate);
                } else {
                    stageValid(candidate);
                }
                return true;
            } catch (InvalidDimension failure) {
                markInvalid(failure.getMessage(), failure.component());
                return false;
            } catch (IllegalArgumentException failure) {
                markInvalid(failure.getMessage(), this);
                return false;
            }
        }

        private void clear(JTextField field, String description) {
            field.putClientProperty("JComponent.outline", null);
            field.setToolTipText(description);
            field.getAccessibleContext().setAccessibleDescription(description);
        }

        private static BigDecimal dimension(JTextField field, String label) {
            String text = field.getText().strip();
            final BigDecimal value;
            try {
                value = new BigDecimal(text);
            } catch (NumberFormatException failure) {
                throw new InvalidDimension(field,
                        label + " must be a finite decimal number.", failure);
            }
            if (value.signum() < 0) {
                throw new InvalidDimension(field,
                        label + " must be non-negative.");
            }
            if (!DartNumericLiterals.isRepresentableDouble(value)) {
                throw new InvalidDimension(field,
                        label + " must be exactly representable as a finite Dart double.");
            }
            return value;
        }
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

    private static final class InvalidDimension extends IllegalArgumentException {
        private final JComponent component;

        InvalidDimension(JComponent component, String message) {
            super(message);
            this.component = Objects.requireNonNull(component, "component");
        }

        InvalidDimension(JComponent component, String message, Throwable cause) {
            super(message, cause);
            this.component = Objects.requireNonNull(component, "component");
        }

        JComponent component() {
            return component;
        }
    }
}
