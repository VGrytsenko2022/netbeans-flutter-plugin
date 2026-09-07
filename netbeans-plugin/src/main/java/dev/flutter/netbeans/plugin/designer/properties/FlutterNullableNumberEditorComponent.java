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

/** Closed omission/null/number union with schema-approved Infinity members; never raw Dart. */
final class FlutterNullableNumberEditorComponent {
    static final String MODE_NAME = "flutter.nullableNumber.mode";
    static final String VALUE_NAME = "flutter.nullableNumber.value";
    static final String OMIT = "Use constructor default (omit argument)";
    static final String INHERITED = "Use inherited value (null)";
    static final String NUMBER = "Explicit number";
    static final String INHERITED_TEXT = "Inherited (null)";
    static final String SECONDARY_NULL_TEXT = "No secondary track (null)";
    static String nullText(FlutterTypedPropertyEditors.Binding binding) {
        return binding.definition().name().value().equals("secondaryTrackValue") ? SECONDARY_NULL_TEXT : INHERITED_TEXT;
    }

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
        private final boolean stateEntry;
        private final String omitLabel;
        private final boolean supportsInfinity;
        private final boolean secondaryTrack;
        private final boolean negativeInfinity;
        private final boolean supportsNaN;
        private final String nullLabel;

        NullableNumberPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment); numberBinding = binding;
            supportsInfinity = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.NULLABLE_NUMBER_WITH_INFINITY;
            secondaryTrack = binding.definition().name().value().equals("secondaryTrackValue");
            negativeInfinity = supportsInfinity && binding.definition().constraints().stream().anyMatch(constraint -> constraint instanceof dev.flutter.netbeans.designer.catalog.PropertyValueConstraint.EnumValues values && values.values().contains("negativeInfinity"));
            supportsNaN = supportsInfinity && binding.definition().constraints().stream().anyMatch(constraint -> constraint instanceof dev.flutter.netbeans.designer.catalog.PropertyValueConstraint.EnumValues values && values.values().contains("nan"));
            nullLabel = secondaryTrack ? SECONDARY_NULL_TEXT : INHERITED;
            stateEntry = dev.flutter.netbeans.designer.catalog.SwitchWidgetPropertySchema.outlineWidthStateProperties().contains(binding.definition().name().value());
            omitLabel = stateEntry ? "Not set (omit state entry)" : OMIT;
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(530, 175));
            setName("flutter.nullableNumber.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " nullable number editor");
            getAccessibleContext().setAccessibleDescription(stateEntry
                    ? "Choose an omitted state entry, explicit inherited null, or a finite number" + (supportsInfinity ? " or positive Infinity" : "") + ". Explicit null stops lower-priority local states. Drafts remain local until OK."
                    : "Choose constructor omission, explicit inherited null, or a finite number. Drafts remain local until OK; Cancel preserves the original typed state.");
            mode = new JComboBox<>(binding.optional() ? new String[]{omitLabel, nullLabel, NUMBER} : new String[]{nullLabel, NUMBER});
            if (secondaryTrack) getAccessibleContext().setAccessibleDescription("Choose omitted Secondary track value, explicit null with no secondary track, or an exact number including signed Infinity. Values must remain in range; drafts stay local until OK.");
            if (supportsNaN) getAccessibleContext().setAccessibleDescription("Choose omitted ListTile geometry, explicit inherited null, or an exact signed number including Infinity, -Infinity and NaN. Unsafe mounted geometry is reported by Canvas; drafts stay local until OK.");
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Numeric value source");
            mode.getAccessibleContext().setAccessibleDescription(secondaryTrack
                    ? "Omission and explicit null both disable the secondary track but remain distinct saved states."
                    : "Omission and inherited null are distinct saved states.");
            var sourceLabel = new JLabel("Source:"); sourceLabel.setLabelFor(mode);
            var source = new JPanel(new BorderLayout(8, 0)); source.add(sourceLabel, BorderLayout.WEST); source.add(mode, BorderLayout.CENTER); add(source, BorderLayout.NORTH);
            number.setName(VALUE_NAME); number.getAccessibleContext().setAccessibleName(supportsNaN ? "Explicit number, signed Infinity or NaN" : negativeInfinity ? "Explicit number or signed Infinity" : supportsInfinity ? "Explicit number or positive Infinity" : "Explicit finite number");
            number.getAccessibleContext().setAccessibleDescription(supportsInfinity
                    ? supportsNaN ? "A signed finite integer or decimal, Infinity, -Infinity or NaN. Only the closed double constants are accepted; no Dart expressions."
                            : negativeInfinity ? "A signed finite integer or decimal, Infinity or -Infinity. NaN and Dart expressions are not accepted."
                            : "A signed finite integer or decimal, or exactly Infinity. Negative Infinity, NaN and Dart expressions are not accepted."
                    : "A signed finite integer or decimal; no null, blank value, Infinity, NaN, or Dart expression in this mode.");
            var valueLabel = new JLabel("Number:"); valueLabel.setLabelFor(number);
            var value = new JPanel(new BorderLayout(8, 0)); value.add(valueLabel, BorderLayout.WEST); value.add(number, BorderLayout.CENTER); add(value, BorderLayout.CENTER);
            note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Nullable number behavior"); add(note, BorderLayout.SOUTH);
            var initial = initialValue().explicitValue().orElse(null);
            numericInitial = initial instanceof PropertyValue.IntegerValue || initial instanceof PropertyValue.DoubleValue
                    || supportsInfinity && initial instanceof PropertyValue.EnumValue
                    ? initialValue() : FlutterPropertyCellValue.unset();
            number.setText(initial instanceof PropertyValue.IntegerValue integer ? integer.value().toString()
                    : initial instanceof PropertyValue.DoubleValue decimal ? decimal.value().toPlainString()
                    : supportsInfinity && initial instanceof PropertyValue.EnumValue enumValue ? enumValue.value().equals("nan") ? "NaN" : enumValue.value().equals("negativeInfinity") ? "-Infinity" : "Infinity" : "0");
            mode.setSelectedItem(initial == null ? binding.optional() ? omitLabel : nullLabel : initial instanceof PropertyValue.NullValue ? nullLabel : NUMBER);
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
                    : nullLabel.equals(selected) ? "Stores explicit null. Inherited/theme behavior may differ from omitting this constructor argument."
                    : "Omits the argument and preserves the constructor's default. This is not explicit null.";
            if (stateEntry && !NUMBER.equals(selected)) description = nullLabel.equals(selected)
                    ? "Stores explicit null. Matching this entry stops lower-priority local states and delegates to SDK/theme fallback."
                    : "Omits this state entry; lower-priority local states remain eligible. This is not explicit null.";
            if (supportsInfinity && NUMBER.equals(selected)) description = supportsNaN
                    ? "Stores the exact finite number or closed Infinity, -Infinity or NaN constant. Unsafe mounted ListTile geometry is reported by Canvas without rewriting the saved value."
                    : negativeInfinity
                    ? "Stores the exact finite number, Infinity or -Infinity; it must be within the current slider range. Values are never clamped."
                    : "Stores the exact finite number or positive Infinity. This is distinct from omitted state entry and inherited null.";
            if (secondaryTrack && !NUMBER.equals(selected)) description = nullLabel.equals(selected)
                    ? "Stores explicit null and disables the secondary track. The main Value is unchanged."
                    : "Omits Secondary track value; the constructor default disables this additional track.";
            note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
            try {
                FlutterPropertyCellValue candidate;
                if (omitLabel.equals(selected)) candidate = FlutterPropertyCellValue.unset();
                else if (nullLabel.equals(selected)) candidate = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                else {
                    var parser = numberBinding.createEditor();
                    if (numericInitial.explicitValue().isPresent()) parser.setValue(numericInitial);
                    parser.setAsText(number.getText()); candidate = (FlutterPropertyCellValue) parser.getValue();
                    var parsed = candidate.explicitValue().orElse(null);
                    if (!(parsed instanceof PropertyValue.IntegerValue || parsed instanceof PropertyValue.DoubleValue
                            || supportsInfinity && (new PropertyValue.EnumValue("double", "infinity").equals(parsed)
                                    || negativeInfinity && new PropertyValue.EnumValue("double", "negativeInfinity").equals(parsed)
                                    || supportsNaN && new PropertyValue.EnumValue("double", "nan").equals(parsed))))
                        throw new IllegalArgumentException(supportsInfinity
                                ? supportsNaN ? "Enter a number, Infinity, -Infinity or NaN; choose a source mode for null or omission."
                                        : negativeInfinity ? "Enter a finite number, Infinity or -Infinity; choose a source mode for null or omission."
                                        : "Enter a finite number or Infinity; choose a source mode for inherited null or omission."
                                : "Enter a finite number; choose an explicit source mode for inherited null or omission.");
                }
                clearInvalid(number, description);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), number); return false; }
        }
    }
}
