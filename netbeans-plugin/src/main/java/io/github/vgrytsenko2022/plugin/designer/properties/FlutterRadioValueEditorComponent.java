package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.DartParameter;
import io.github.vgrytsenko2022.designer.catalog.PropertyDefinition;
import io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Closed typed Radio value union. Each mode owns a cancel-safe local draft. */
final class FlutterRadioValueEditorComponent {
    static final String MODE_NAME = "flutter.radioValue.mode";
    static final String STRING_NAME = "flutter.radioValue.string";
    static final String INTEGER_NAME = "flutter.radioValue.integer";
    static final String DOUBLE_NAME = "flutter.radioValue.double";
    static final String BOOLEAN_NAME = "flutter.radioValue.boolean";
    static final String NOTE_NAME = "flutter.radioValue.note";
    static final String OMIT = "Use SDK Radio value (omit argument)";
    static final String NONE = "Explicit null";
    static final String STRING = "String";
    static final String INTEGER = "Integer";
    static final String DOUBLE = "Double";
    static final String BOOLEAN = "Boolean";
    static final String POSITIVE_INFINITY = "Positive Infinity";
    static final String NEGATIVE_INFINITY = "Negative Infinity";
    static final String NAN = "NaN";
    static final String PROJECT = "Project value reference";

    private FlutterRadioValueEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new RadioValuePanel(editor, binding, environment);
    }

    private static final class RadioValuePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JPanel cards = new JPanel(new CardLayout());
        private final Map<String, JTextField> fields = new LinkedHashMap<>();
        private final JTextArea stringValue = new JTextArea(6, 38);
        private final JCheckBox booleanValue = new JCheckBox();
        private final JTextArea note = new JTextArea(3, 55);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;

        RadioValuePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.RADIO_VALUE)
                throw new IllegalArgumentException("Radio value editor requires the exact nullable literal/Object-reference union.");
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(710, 490)); setName("flutter.radioValue.editor");
            getAccessibleContext().setAccessibleName("Radio value typed value editor");
            getAccessibleContext().setAccessibleDescription("Omission, explicit null, primitive literals and analyzer-verified selected-type references remain distinct. Cancel never publishes a draft.");
            mode = new JComboBox<>(binding.optional() ? new String[]{OMIT, NONE, STRING, INTEGER, DOUBLE, BOOLEAN, POSITIVE_INFINITY, NEGATIVE_INFINITY, NAN, PROJECT}
                    : new String[]{NONE, STRING, INTEGER, DOUBLE, BOOLEAN, POSITIVE_INFINITY, NEGATIVE_INFINITY, NAN, PROJECT});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Radio value value type");
            var heading = new JPanel(new BorderLayout(8, 0)); var source = new JLabel("Type:"); source.setLabelFor(mode);
            heading.add(source, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            stringValue.setName(STRING_NAME); stringValue.setText(initial instanceof PropertyValue.StringValue value ? value.value() : "");
            stringValue.getAccessibleContext().setAccessibleName("String Radio value literal");
            stringValue.getAccessibleContext().setAccessibleDescription("Exact string including empty text, whitespace and line breaks; never parsed as Dart code.");
            var stringRow = new JPanel(new BorderLayout(8, 0)); var stringLabel = new JLabel("String:"); stringLabel.setLabelFor(stringValue);
            stringRow.add(stringLabel, BorderLayout.NORTH); stringRow.add(new JScrollPane(stringValue), BorderLayout.CENTER); cards.add(stringRow, STRING);
            addField(INTEGER, INTEGER_NAME, initial instanceof PropertyValue.IntegerValue value ? value.value().toString() : "0");
            addField(DOUBLE, DOUBLE_NAME, initial instanceof PropertyValue.DoubleValue value ? value.value().toPlainString() : "0.0");
            booleanValue.setName(BOOLEAN_NAME); booleanValue.setHorizontalAlignment(SwingConstants.CENTER);
            booleanValue.getAccessibleContext().setAccessibleName("Boolean Radio value");
            booleanValue.setSelected(initial instanceof PropertyValue.BooleanValue value && value.value());
            cards.add(booleanValue, BOOLEAN); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), NONE);
            cards.add(new JPanel(), POSITIVE_INFINITY); cards.add(new JPanel(), NEGATIVE_INFINITY); cards.add(new JPanel(), NAN);
            var constraint = binding.definition().constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true), List.of(constraint), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow(); var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_radioValue", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel) FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearReferenceRoot(referencePanel);
            cards.add(referencePanel, PROJECT); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Radio value and preview behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : NONE : switch (initial) {
                case PropertyValue.NullValue ignored -> NONE;
                case PropertyValue.StringValue ignored -> STRING;
                case PropertyValue.IntegerValue ignored -> INTEGER;
                case PropertyValue.DoubleValue ignored -> DOUBLE;
                case PropertyValue.BooleanValue ignored -> BOOLEAN;
                case PropertyValue.EnumValue number -> switch (number.value()) {
                    case "infinity" -> POSITIVE_INFINITY;
                    case "negativeInfinity" -> NEGATIVE_INFINITY;
                    case "nan" -> NAN;
                    default -> throw new IllegalArgumentException("Radio special doubles accept only Infinity, -Infinity or NaN.");
                };
                default -> PROJECT;
            });
            mode.addActionListener(ignored -> refresh(true)); booleanValue.addActionListener(ignored -> refresh(true));
            referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            var documents = new java.util.ArrayList<javax.swing.text.Document>();
            documents.add(stringValue.getDocument()); fields.values().forEach(field -> documents.add(field.getDocument()));
            documents.forEach(document -> document.addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { refresh(true); }
                @Override public void removeUpdate(DocumentEvent event) { refresh(true); }
                @Override public void changedUpdate(DocumentEvent event) { refresh(true); }
            }));
            activate(); refresh(true);
        }

        private void addField(String type, String name, String value) {
            var field = new JTextField(value, 30); field.setName(name); field.getAccessibleContext().setAccessibleName(type + " Radio value literal");
            field.getAccessibleContext().setAccessibleDescription(STRING.equals(type) ? "Exact string including empty text and whitespace; never parsed as Dart code."
                    : "A finite " + type.toLowerCase(java.util.Locale.ROOT) + " literal, not an expression.");
            var row = new JPanel(new BorderLayout(8, 0)); var label = new JLabel(type + ":"); label.setLabelFor(field);
            row.add(label, BorderLayout.WEST); row.add(field, BorderLayout.CENTER); fields.put(type, field); cards.add(row, type);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true; refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); ((CardLayout) cards.getLayout()).show(cards, selected);
                String description = NONE.equals(selected) ? "Stores explicit null. Value requires Nullable value type; Group value always permits null."
                        : OMIT.equals(selected) ? "Omits groupValue and preserves inherited RadioGroup or registry selection."
                        : PROJECT.equals(selected) ? "The analyzer verifies this reference against the selected Radio type. Nullable references are accepted only where the selected type allows null; dynamic is rejected. Canvas never executes project code."
                        : List.of(POSITIVE_INFINITY, NEGATIVE_INFINITY, NAN).contains(selected) ? "Stores the exact closed double identity. Select double, num, Object or a compatible project type; String, int and bool reject it. NaN is unequal to itself, so matching NaN values are not selected."
                        : "Stores an explicit " + selected.toLowerCase(java.util.Locale.ROOT) + " value checked against the selected Radio type. Strings are literal data, never Dart code.";
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                FlutterPropertyCellValue candidate = switch (selected) {
                    case OMIT -> FlutterPropertyCellValue.unset();
                    case NONE -> FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                    case STRING -> FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(stringValue.getText()));
                    case INTEGER -> FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger(fields.get(INTEGER).getText().strip())));
                    case DOUBLE -> FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new BigDecimal(fields.get(DOUBLE).getText().strip())));
                    case BOOLEAN -> FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(booleanValue.isSelected()));
                    case POSITIVE_INFINITY -> FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "infinity"));
                    case NEGATIVE_INFINITY -> FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "negativeInfinity"));
                    case NAN -> FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "nan"));
                    default -> requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue();
                };
                clearInvalid(note, description);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid("Invalid typed Radio value: " + failure.getMessage(), note); return false;
            } finally { refreshing = false; }
        }

        private static void clearReferenceRoot(Container parent) {
            for (var component : parent.getComponents()) {
                if (component instanceof JTextField field && FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME.equals(field.getName())) field.setText("");
                else if (component instanceof Container child) clearReferenceRoot(child);
            }
        }
    }
}
