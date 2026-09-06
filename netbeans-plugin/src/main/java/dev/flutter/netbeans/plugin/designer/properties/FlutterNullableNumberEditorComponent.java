package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Closed, cancel-safe omission/null/finite-number union; never a Dart expression editor. */
final class FlutterNullableNumberEditorComponent {
    static final String MODE_NAME = "flutter.nullableNumber.mode";
    static final String VALUE_NAME = "flutter.nullableNumber.value";
    static final String OMIT = "Use constructor default (omit argument)";
    static final String INHERITED = "Use inherited value (null)";
    static final String NUMBER = "Explicit number";
    static final String INHERITED_TEXT = "Inherited (null)";

    private FlutterNullableNumberEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new NullableNumberPanel(editor, binding, environment);
    }

    private static final class NullableNumberPanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JTextField number = new JTextField(20);
        private final JTextArea note = new JTextArea(3, 48);
        private final FlutterTypedPropertyEditors.Binding numberBinding;
        private final FlutterPropertyCellValue numericInitial;

        NullableNumberPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment); numberBinding = binding;
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(530, 175));
            setName("flutter.nullableNumber.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable number editor");
            getAccessibleContext().setAccessibleDescription("Choose constructor omission, explicit inherited null, or a finite number. Drafts remain local until OK; Cancel preserves the original typed state.");
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, INHERITED, NUMBER} : new String[]{INHERITED, NUMBER});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Numeric value source");
            mode.getAccessibleContext().setAccessibleDescription("Omission and inherited null are distinct saved states.");
            var sourceLabel = new JLabel("Source:"); sourceLabel.setLabelFor(mode);
            var source = new JPanel(new BorderLayout(8, 0)); source.add(sourceLabel, BorderLayout.WEST); source.add(mode, BorderLayout.CENTER); add(source, BorderLayout.NORTH);
            number.setName(VALUE_NAME); number.getAccessibleContext().setAccessibleName("Explicit finite number");
            number.getAccessibleContext().setAccessibleDescription("A signed finite integer or decimal; no null, blank value, Infinity, NaN, or Dart expression in this mode.");
            var valueLabel = new JLabel("Number:"); valueLabel.setLabelFor(number);
            var value = new JPanel(new BorderLayout(8, 0)); value.add(valueLabel, BorderLayout.WEST); value.add(number, BorderLayout.CENTER); add(value, BorderLayout.CENTER);
            note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Nullable number behavior"); add(note, BorderLayout.SOUTH);
            var initial = initialValue().explicitValue().orElse(null);
            numericInitial = initial instanceof PropertyValue.IntegerValue || initial instanceof PropertyValue.DoubleValue
                    ? initialValue() : FlutterPropertyCellValue.unset();
            number.setText(initial instanceof PropertyValue.IntegerValue integer ? integer.value().toString()
                    : initial instanceof PropertyValue.DoubleValue decimal ? decimal.value().toPlainString() : "0");
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : INHERITED : initial instanceof PropertyValue.NullValue ? INHERITED : NUMBER);
            mode.addActionListener(ignored -> refresh(true));
            number.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { refresh(true); }
                @Override public void removeUpdate(DocumentEvent event) { refresh(true); }
                @Override public void changedUpdate(DocumentEvent event) { refresh(true); }
            });
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            String selected = (String) mode.getSelectedItem(); number.setEnabled(NUMBER.equals(selected));
            String description = NUMBER.equals(selected) ? "Stores the exact finite number. It does not select the constructor default or inherited null."
                    : INHERITED.equals(selected) ? "Stores explicit null. Inherited/theme behavior may differ from omitting this constructor argument."
                    : "Omits the argument and preserves the constructor's default. This is not explicit null.";
            note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
            try {
                FlutterPropertyCellValue candidate;
                if (OMIT.equals(selected)) candidate = FlutterPropertyCellValue.unset();
                else if (INHERITED.equals(selected)) candidate = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                else {
                    var parser = numberBinding.createEditor();
                    if (numericInitial.explicitValue().isPresent()) parser.setValue(numericInitial);
                    parser.setAsText(number.getText()); candidate = (FlutterPropertyCellValue) parser.getValue();
                    var parsed = candidate.explicitValue().orElse(null);
                    if (!(parsed instanceof PropertyValue.IntegerValue || parsed instanceof PropertyValue.DoubleValue))
                        throw new IllegalArgumentException("Enter a finite number; choose an explicit source mode for inherited null or omission.");
                }
                clearInvalid(number, description);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), number); return false; }
        }
    }
}
