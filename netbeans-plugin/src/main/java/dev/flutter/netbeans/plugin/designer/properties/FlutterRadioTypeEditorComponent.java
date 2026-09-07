package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyEditor;
import java.util.List;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Closed Radio type identity; never accepts a Dart type-expression escape hatch. */
final class FlutterRadioTypeEditorComponent {
    static final String CONTEXT_ATTRIBUTE = "flutter.radioType.context";
    static final String NULLABILITY_NAME = "flutter.radioType.nullability";
    static final String MODE_NAME = "flutter.radioType.mode";
    static final String PRESET_NAME = "flutter.radioType.preset";
    static final String LIBRARY_NAME = "flutter.radioType.library";
    static final String SYMBOL_NAME = "flutter.radioType.symbol";
    static final String NOTE_NAME = "flutter.radioType.note";
    static final String BUILTIN = "Built-in type";
    static final String CURRENT = "Current-file type";
    static final String PACKAGE = "Package type";
    static final List<String> BUILTIN_TYPES = List.of("String", "int", "double", "num", "bool", "Object");

    private FlutterRadioTypeEditorComponent() { }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new TypePanel(editor, binding, environment);
    }

    private static final class TypePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode = new JComboBox<>(new String[]{BUILTIN, CURRENT, PACKAGE});
        private final JComboBox<String> preset = new JComboBox<>(BUILTIN_TYPES.toArray(String[]::new));
        private final JTextField library = new JTextField(40);
        private final JTextField symbol = new JTextField(32);
        private final JTextArea note = new JTextArea(4, 58);
        private boolean refreshing;
        private final dev.flutter.netbeans.designer.model.WidgetNode captured;
        private final JComboBox<String> nullability = new JComboBox<>(new String[]{"Non-nullable (omit flag)", "Non-nullable (explicit false)", "Nullable (explicit true)"});
        private final java.util.Map<String, FlutterPropertyEditorComponents.CommitOnValidPanel> dependentPanels = new java.util.LinkedHashMap<>();

        TypePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            Object context = environment.getFeatureDescriptor().getValue(CONTEXT_ATTRIBUTE);
            captured = context instanceof java.util.function.Supplier<?> supplier && supplier.get() instanceof dev.flutter.netbeans.designer.model.WidgetNode node
                    && FlutterPropertyCellValue.RadioTypeEdit.supports(node.type()) ? node : null;
            if (!binding.definition().name().value().equals("valueType") || binding.optional())
                throw new IllegalArgumentException("Radio/RadioGroup type editor requires the required valueType property.");
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(690, 255)); setName("flutter.radioType.editor");
            getAccessibleContext().setAccessibleName(familyName() + " value type editor");
            getAccessibleContext().setAccessibleDescription("Select a built-in type or a simple current-file/package type symbol. Nullability is a separate Radio property. Cancel preserves every property.");
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName(familyName() + " type source");
            preset.setName(PRESET_NAME); preset.getAccessibleContext().setAccessibleName("Built-in " + familyName() + " type");
            library.setName(LIBRARY_NAME); library.getAccessibleContext().setAccessibleName("Type package library");
            library.getAccessibleContext().setAccessibleDescription("A package: URI inside an admitted project package; no filesystem paths or network URLs.");
            symbol.setName(SYMBOL_NAME); symbol.getAccessibleContext().setAccessibleName(familyName() + " type symbol");
            symbol.getAccessibleContext().setAccessibleDescription("A simple class, enum, or typedef identifier. Use a project typedef for a complex generic type; expressions, members and invocations are not accepted.");
            var form = new JPanel(new GridBagLayout());
            addRow(form, 0, "Source:", mode); addRow(form, 1, "Built-in:", preset);
            addRow(form, 2, "Package library:", library); addRow(form, 3, "Type symbol:", symbol);
            if (captured == null) add(form, BorderLayout.CENTER);
            else {
                setPreferredSize(new Dimension(770, 710));
                nullability.setName(NULLABILITY_NAME); nullability.getAccessibleContext().setAccessibleName(familyName() + " nullable value type");
                var flag = captured.properties().get(new dev.flutter.netbeans.designer.model.PropertyName("nullableValueType"));
                nullability.setSelectedIndex(flag instanceof PropertyValue.BooleanValue bool ? bool.value() ? 2 : 1 : 0);
                addRow(form, 4, "Nullability:", nullability);
                var tabs = new javax.swing.JTabbedPane(); tabs.setName("flutter.radioType.tabs");
                tabs.getAccessibleContext().setAccessibleName("Atomic " + familyName() + " type and dependent values");
                tabs.addTab("Type", form);
                var definition = dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog.getDefault()
                        .find(captured.type()).orElseThrow();
                for (String field : dependentFields()) {
                    var dependentBinding = FlutterTypedPropertyEditors.binding(definition.property(new dev.flutter.netbeans.designer.model.PropertyName(field)).orElseThrow()).orElseThrow();
                    var dependentEditor = dependentBinding.createEditor();
                    dependentEditor.setValue(new FlutterPropertyCellValue(Optional.ofNullable(captured.properties().get(new dev.flutter.netbeans.designer.model.PropertyName(field)))));
                    var nestedEnvironment = PropertyEnv.create(new java.beans.FeatureDescriptor());
                    var panel = (FlutterPropertyEditorComponents.CommitOnValidPanel) FlutterRadioValueEditorComponent.customEditor(dependentEditor, dependentBinding, nestedEnvironment);
                    panel.setName("flutter.radioType." + field); dependentPanels.put(field, panel);
                    tabs.addTab(field.equals("value") ? "Value" : "Group value", panel);
                    nestedEnvironment.addPropertyChangeListener(ignored -> refresh(true));
                }
                nullability.addActionListener(ignored -> refresh(true)); add(tabs, BorderLayout.CENTER);
            }
            note.setName(NOTE_NAME); note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName(familyName() + " type safety and dependent values"); add(note, BorderLayout.SOUTH);
            var initial = initialValue().explicitValue().orElse(null);
            if (initial instanceof PropertyValue.StringValue value) {
                if (!BUILTIN_TYPES.contains(value.value())) throw new IllegalArgumentException("Stored Radio built-in type is not reviewed.");
                preset.setSelectedItem(value.value()); mode.setSelectedItem(BUILTIN);
            } else if (initial instanceof PropertyValue.DartObjectReferenceValue reference) {
                requireSimpleType(reference); library.setText(reference.libraryUri().orElse("")); symbol.setText(reference.rootSymbol());
                mode.setSelectedItem(reference.libraryUri().isPresent() ? PACKAGE : CURRENT);
            } else throw new IllegalArgumentException("Radio valueType must contain a built-in type or a simple type reference.");
            mode.addActionListener(ignored -> refresh(true)); preset.addActionListener(ignored -> refresh(true));
            var listener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { refresh(true); }
                @Override public void removeUpdate(DocumentEvent event) { refresh(true); }
                @Override public void changedUpdate(DocumentEvent event) { refresh(true); }
            };
            library.getDocument().addDocumentListener(listener); symbol.getDocument().addDocumentListener(listener);
            activate(); refresh(true);
        }

        private static void addRow(JPanel form, int row, String text, Component input) {
            var constraints = new GridBagConstraints(); constraints.gridy = row; constraints.insets = new Insets(3, 0, 3, 8);
            constraints.anchor = GridBagConstraints.WEST; var label = new JLabel(text); label.setLabelFor(input);
            form.add(label, constraints); constraints.gridx = 1; constraints.weightx = 1; constraints.fill = GridBagConstraints.HORIZONTAL; form.add(input, constraints);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); boolean builtin = BUILTIN.equals(selected); boolean imported = PACKAGE.equals(selected);
                preset.setEnabled(builtin); library.setEnabled(imported); symbol.setEnabled(!builtin);
                String description = "The selected type is verified by the Dart analyzer. Draft type, nullability, "
                        + (isGroup() ? "and Group value together using the tabs; OK applies one atomic edit and Undo restores all three. Descendant Radio types are never changed. "
                                : "Value and Group value together using the tabs; OK applies one atomic edit and Undo restores all four. ")
                        + "Callbacks and registry references remain unchanged and must be compatible. Cancel changes nothing. Complex generics can be named by a project typedef.";
                note.setText(description); note.getAccessibleContext().setAccessibleDescription(description);
                PropertyValue candidate;
                if (builtin) candidate = new PropertyValue.StringValue((String) preset.getSelectedItem());
                else {
                    var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of(library.getText().strip()) : Optional.empty(),
                            symbol.getText().strip(), Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
                    requireSimpleType(reference); candidate = reference;
                }
                clearInvalid(note, description);
                FlutterPropertyCellValue result = FlutterPropertyCellValue.explicit(candidate);
                if (captured != null && dependentPanels.size() == dependentFields().size()) {
                    var baseline = FlutterPropertyCellValue.RadioTypeEdit.snapshot(captured);
                    var requested = new java.util.LinkedHashMap<>(baseline);
                    requested.put("valueType", Optional.of(candidate));
                    requested.put("nullableValueType", nullability.getSelectedIndex() == 0 ? Optional.empty() : Optional.of(new PropertyValue.BooleanValue(nullability.getSelectedIndex() == 2)));
                    for (var entry : dependentPanels.entrySet()) requested.put(entry.getKey(), (requestValidation ? entry.getValue().stagedDraftValue() : entry.getValue().validatedDraftValue()).explicitValue());
                    var properties = new java.util.LinkedHashMap<>(captured.properties());
                    requested.forEach((field, value) -> { var key = new dev.flutter.netbeans.designer.model.PropertyName(field); if (value.isPresent()) properties.put(key, value.orElseThrow()); else properties.remove(key); });
                    var prospective = new dev.flutter.netbeans.designer.model.WidgetNode(captured.id(), captured.type(), properties, captured.slots());
                    (isGroup() ? dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema.valueTypeError(prospective)
                            : dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema.valueTypeError(prospective))
                            .ifPresent(reason -> { throw new IllegalArgumentException(reason); });
                    if (!requested.equals(baseline)) result = new FlutterPropertyCellValue(Optional.of(candidate), Optional.of(new FlutterPropertyCellValue.RadioTypeEdit(captured.type(), captured.id(), baseline, requested)));
                }
                if (requestValidation) markValid(result); else stageValid(result);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid("Invalid " + familyName() + " type: " + failure.getMessage(), note); return false; }
            finally { refreshing = false; }
        }

        private boolean isGroup() {
            return captured != null && dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(captured.type());
        }
        private String familyName() { return isGroup() ? "RadioGroup" : "Radio"; }
        private List<String> dependentFields() { return isGroup() ? List.of("groupValue") : List.of("value", "groupValue"); }

        private static void requireSimpleType(PropertyValue.DartObjectReferenceValue reference) {
            if (reference.member().isPresent() || reference.access() != PropertyValue.DartObjectReferenceValue.Access.REFERENCE || reference.constant().isPresent())
                throw new IllegalArgumentException("Radio type identities cannot contain a member or invocation.");
            if (List.of("dynamic", "void", "Never", "Null").contains(reference.rootSymbol()))
                throw new IllegalArgumentException("Select one of the reviewed built-in types or a concrete project type/typedef, not " + reference.rootSymbol() + ".");
        }
    }
}
