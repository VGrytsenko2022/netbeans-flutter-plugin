package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyEditor;
import javax.swing.*;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Nullable literal text; omission, null and the empty string remain different values. */
final class FlutterNullableStringEditorComponent {
    static final String MODE_NAME = "flutter.nullableString.mode";
    static final String TEXT_NAME = "flutter.nullableString.text";
    static final String PREVIEW_NAME = "flutter.nullableString.preview";
    static final String OMIT = "Use constructor default (omit argument)";
    static final String NULL = "Explicit null";
    static final String STRING = "String value (including empty text)";
    private FlutterNullableStringEditorComponent() { }
    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new Panel(editor, binding, environment);
    }
    private static final class Panel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JTextArea text = new JTextArea(5, 42);
        private final JTextArea preview = new JTextArea(3, 42);
        Panel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            setLayout(new BorderLayout(8, 8)); setPreferredSize(new Dimension(580, 310));
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable String editor");
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, NULL, STRING} : new String[]{NULL, STRING});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("String value source");
            var row = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode);
            row.add(label, BorderLayout.WEST); row.add(mode, BorderLayout.CENTER); add(row, BorderLayout.NORTH);
            text.setName(TEXT_NAME); text.getAccessibleContext().setAccessibleName("Literal String value");
            text.getAccessibleContext().setAccessibleDescription("Plain text, not Dart code. Empty text is retained as an explicit empty String.");
            add(new JScrollPane(text), BorderLayout.CENTER);
            preview.setName(PREVIEW_NAME); preview.setEditable(false); preview.setOpaque(false); preview.setLineWrap(true); preview.setWrapStyleWord(true);
            add(preview, BorderLayout.SOUTH);
            var initial = initialValue().explicitValue().orElse(null);
            text.setText(initial instanceof PropertyValue.StringValue value ? value.value() : "");
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : NULL : initial instanceof PropertyValue.NullValue ? NULL : STRING);
            mode.addActionListener(ignored -> refresh(true));
            text.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { refresh(true); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { refresh(true); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { refresh(true); }
            });
            activate(); refresh(true);
        }
        @Override boolean prepareCommit() { return refresh(false); }
        private boolean refresh(boolean requestValidation) {
            text.setEnabled(STRING.equals(mode.getSelectedItem()));
            try {
                var value = OMIT.equals(mode.getSelectedItem()) ? FlutterPropertyCellValue.unset()
                        : NULL.equals(mode.getSelectedItem()) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())
                        : FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(text.getText()));
                String summary = value.explicitValue().map(PropertyValueFormatter::format).orElse(FlutterPropertyCellValue.NOT_SET_TEXT)
                        + "\nOmission, null and empty text are distinct. Text is stored literally; only OK applies this draft.";
                preview.setText(summary); clearInvalid(preview, summary);
                if (requestValidation) markValid(value); else stageValid(value);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), preview); return false; }
        }
    }
}
