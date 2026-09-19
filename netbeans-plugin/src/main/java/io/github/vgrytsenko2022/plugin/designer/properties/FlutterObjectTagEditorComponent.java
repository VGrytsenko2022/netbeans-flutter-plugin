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

/** Closed typed Hero/LayoutId identity union. Each mode owns a cancel-safe local draft. */
final class FlutterObjectTagEditorComponent {
    static final String MODE_NAME = "flutter.objectTag.mode";
    static final String STRING_NAME = "flutter.objectTag.string";
    static final String INTEGER_NAME = "flutter.objectTag.integer";
    static final String DOUBLE_NAME = "flutter.objectTag.double";
    static final String BOOLEAN_NAME = "flutter.objectTag.boolean";
    static final String NOTE_NAME = "flutter.objectTag.note";
    static final String OMIT = "Use SDK Hero tag (omit argument)";
    static final String NONE = "No Hero (explicit null)";
    static final String STRING = "String";
    static final String INTEGER = "Integer";
    static final String DOUBLE = "Double";
    static final String BOOLEAN = "Boolean";
    static final String PROJECT = "Project Object reference";

    private FlutterObjectTagEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new ObjectTagPanel(editor, binding, environment);
    }

    private static final class ObjectTagPanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final JPanel cards = new JPanel(new CardLayout());
        private final Map<String, JTextField> fields = new LinkedHashMap<>();
        private final JTextArea stringValue = new JTextArea(6, 38);
        private final JCheckBox booleanValue = new JCheckBox();
        private final JTextArea note = new JTextArea(3, 55);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;
        private final boolean requiredIdentity;

        ObjectTagPanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            requiredIdentity = !binding.definition().acceptedKinds().contains(io.github.vgrytsenko2022.designer.model.PropertyValueKind.NULL);
            if (binding.editorKind() != FlutterTypedPropertyEditors.EditorKind.OBJECT_TAG)
                throw new IllegalArgumentException("Object tag editor requires the exact typed literal/Object-reference union.");
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(710, 490)); setName("flutter.objectTag.editor");
            getAccessibleContext().setAccessibleName(requiredIdentity ? "Layout ID typed value editor" : "Hero tag typed value editor");
            getAccessibleContext().setAccessibleDescription(requiredIdentity ? "Required non-null layout identity: primitive literal or analyzer-verified Object reference. Cancel never publishes a draft." : "Omission, explicit null, primitive literals and analyzer-verified non-null Object references remain distinct. Cancel never publishes a draft.");
            mode = new JComboBox<>(requiredIdentity ? new String[]{STRING, INTEGER, DOUBLE, BOOLEAN, PROJECT} : binding.optional() ? new String[]{OMIT, NONE, STRING, INTEGER, DOUBLE, BOOLEAN, PROJECT}
                    : new String[]{NONE, STRING, INTEGER, DOUBLE, BOOLEAN, PROJECT});
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName(requiredIdentity ? "Layout ID value type" : "Hero tag value type");
            var heading = new JPanel(new BorderLayout(8, 0)); var source = new JLabel("Type:"); source.setLabelFor(mode);
            heading.add(source, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            stringValue.setName(STRING_NAME); stringValue.setText(initial instanceof PropertyValue.StringValue value ? value.value() : "");
            stringValue.getAccessibleContext().setAccessibleName(requiredIdentity ? "String layout ID literal" : "String Hero tag literal");
            stringValue.getAccessibleContext().setAccessibleDescription("Exact string including empty text, whitespace and line breaks; never parsed as Dart code.");
            var stringRow = new JPanel(new BorderLayout(8, 0)); var stringLabel = new JLabel("String:"); stringLabel.setLabelFor(stringValue);
            stringRow.add(stringLabel, BorderLayout.NORTH); stringRow.add(new JScrollPane(stringValue), BorderLayout.CENTER); cards.add(stringRow, STRING);
            addField(INTEGER, INTEGER_NAME, initial instanceof PropertyValue.IntegerValue value ? value.value().toString() : "0");
            addField(DOUBLE, DOUBLE_NAME, initial instanceof PropertyValue.DoubleValue value ? value.value().toPlainString() : "0.0");
            booleanValue.setName(BOOLEAN_NAME); booleanValue.setHorizontalAlignment(SwingConstants.CENTER);
            booleanValue.getAccessibleContext().setAccessibleName(requiredIdentity ? "Boolean layout ID" : "Boolean Hero tag");
            booleanValue.setSelected(initial instanceof PropertyValue.BooleanValue value && value.value());
            cards.add(booleanValue, BOOLEAN); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), NONE);
            var constraint = binding.definition().constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true), List.of(constraint), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow(); var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_heroTag", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel) FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearReferenceRoot(referencePanel);
            cards.add(referencePanel, PROJECT); add(cards, BorderLayout.CENTER);
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName(requiredIdentity ? "Layout identity and preview behavior" : "Hero identity and preview behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? requiredIdentity ? STRING : binding.optional() ? OMIT : NONE : switch (initial) {
                case PropertyValue.NullValue ignored -> NONE;
                case PropertyValue.StringValue ignored -> STRING;
                case PropertyValue.IntegerValue ignored -> INTEGER;
                case PropertyValue.DoubleValue ignored -> DOUBLE;
                case PropertyValue.BooleanValue ignored -> BOOLEAN;
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
            var field = new JTextField(value, 30); field.setName(name); field.getAccessibleContext().setAccessibleName(type + (requiredIdentity ? " layout ID literal" : " Hero tag literal"));
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
                String description = NONE.equals(selected) ? "Stores null and disables Hero wrapping. This is not the SDK default tag."
                        : OMIT.equals(selected) ? "Omits heroTag, retaining the SDK shared default tag. Multiple buttons in one route need distinct explicit tags or null."
                        : PROJECT.equals(selected) ? "The analyzer verifies a non-null Object. Project code is never executed in isolated Canvas; use a stable getter or factory for project identity."
                        : "Stores an explicit " + selected.toLowerCase(java.util.Locale.ROOT) + " tag. Equal tags in one route conflict, including numerically equal integer/double values.";
                if (requiredIdentity) description = "Required unique LayoutId: primitive literal or strict Object source. "
                        + "Equal IDs in the same CustomMultiChildLayout are invalid, including equal integer/double values. "
                        + "Source equality is a runtime obligation; Canvas never executes project ID code.";
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                FlutterPropertyCellValue candidate = switch (selected) {
                    case OMIT -> FlutterPropertyCellValue.unset();
                    case NONE -> FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                    case STRING -> FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(stringValue.getText()));
                    case INTEGER -> FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger(fields.get(INTEGER).getText().strip())));
                    case DOUBLE -> FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new BigDecimal(fields.get(DOUBLE).getText().strip())));
                    case BOOLEAN -> FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(booleanValue.isSelected()));
                    default -> requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue();
                };
                clearInvalid(note, description);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) {
                markInvalid((requiredIdentity ? "Invalid layout ID: " : "Invalid typed Hero tag: ") + failure.getMessage(), note); return false;
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
