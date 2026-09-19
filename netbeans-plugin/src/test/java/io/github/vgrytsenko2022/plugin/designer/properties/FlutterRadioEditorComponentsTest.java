package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.awt.*;
import java.beans.*;
import java.math.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class FlutterRadioEditorComponentsTest {
    @Test void valueAndGroupAllLiteralModesPreserveExactTypedDataAndOneCommit() throws Exception {
        for (String field : List.of("value", "groupValue")) for (String branch : List.of("String", "Integer", "Double", "Boolean", "Explicit null", "Project value reference")) onEdt(() -> {
            var binding = binding(field); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("old"));
            var editor = binding.createEditor(); editor.setValue(initial); var env = environment(editor, new FeatureDescriptor()); var commits = new AtomicInteger();
            editor.addPropertyChangeListener(ignored -> commits.incrementAndGet()); var panel = editor.getCustomEditor();
            assertFalse(panel.getAccessibleContext().getAccessibleName().contains("Hero"));
            var mode = find(panel, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME); mode.setSelectedItem(branch);
            FlutterPropertyCellValue expected;
            switch (branch) {
                case "String" -> { String exact = " ' \\ $ literal\nline\r\nend\r"; find(panel, JTextArea.class, FlutterRadioValueEditorComponent.STRING_NAME).setText(exact); expected = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(exact)); }
                case "Integer" -> { find(panel, JTextField.class, FlutterRadioValueEditorComponent.INTEGER_NAME).setText("-12"); expected = FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.valueOf(-12))); }
                case "Double" -> { find(panel, JTextField.class, FlutterRadioValueEditorComponent.DOUBLE_NAME).setText("-1.25"); expected = FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new BigDecimal("-1.25"))); }
                case "Boolean" -> { var bool = find(panel, JCheckBox.class, FlutterRadioValueEditorComponent.BOOLEAN_NAME); assertEquals(SwingConstants.CENTER, bool.getHorizontalAlignment()); bool.doClick(); expected = FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)); }
                case "Explicit null" -> expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
                default -> { find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_radioValue"); expected = FlutterPropertyCellValue.explicit(RadioPropertyContractTest.reference("_radioValue")); }
            }
            assertEquals(initial, editor.getValue()); assertEquals(0, commits.get()); env.setState(PropertyEnv.STATE_VALID);
            assertEquals(expected, editor.getValue()); assertEquals(1, commits.get()); env.setState(PropertyEnv.STATE_NEEDS_VALIDATION); env.setState(PropertyEnv.STATE_VALID); assertEquals(1, commits.get());
            return null;
        });
    }

    @Test void literalLineBreaksSurviveUntouchedOpenAndInvalidInactiveDraftsNeverPublish() throws Exception {
        for (String text : List.of("", "line\nnext", "line\r\nnext\r", "  quote' $value  ")) onEdt(() -> {
            var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(text)); var editor = binding("value").createEditor(); editor.setValue(initial);
            var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); assertEquals(text, find(panel, JTextArea.class, FlutterRadioValueEditorComponent.STRING_NAME).getText());
            env.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); return null;
        });
        onEdt(() -> {
            var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("saved")); var editor = binding("groupValue").createEditor(); editor.setValue(initial);
            var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var mode = find(panel, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME);
            mode.setSelectedItem(FlutterRadioValueEditorComponent.PROJECT); var symbol = find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
            symbol.setText("bad(1)"); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            mode.setSelectedItem(FlutterRadioValueEditorComponent.DOUBLE); find(panel, JTextField.class, FlutterRadioValueEditorComponent.DOUBLE_NAME).setText("1e400");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            mode.setSelectedItem(FlutterRadioValueEditorComponent.OMIT); assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            return null;
        });
    }

    @Test void radioSpecialDoubleIdentityModesRoundTripExactlyWithoutWideningHeroOrGeometry() throws Exception {
        var modes = Map.of(FlutterRadioValueEditorComponent.POSITIVE_INFINITY, "infinity",
                FlutterRadioValueEditorComponent.NEGATIVE_INFINITY, "negativeInfinity", FlutterRadioValueEditorComponent.NAN, "nan");
        for (String field : List.of("value", "groupValue")) for (var entry : modes.entrySet()) onEdt(() -> {
            var expected = FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", entry.getValue()));
            for (var initial : List.of(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("saved")), expected)) {
                var editor = binding(field).createEditor(); editor.setValue(initial); var env = environment(editor, new FeatureDescriptor());
                var panel = editor.getCustomEditor(); find(panel, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(entry.getKey());
                assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
            }
            return null;
        });
        var hero = FlutterTypedPropertyEditors.binding(FloatingActionButtonPropertyContractTest.DEF.property(new PropertyName("heroTag")).orElseThrow()).orElseThrow();
        for (String member : modes.values()) assertThrows(IllegalArgumentException.class, () -> hero.validate(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", member))));
        assertThrows(IllegalArgumentException.class, () -> binding("splashRadius").validate(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "nan"))));
        assertThrows(IllegalArgumentException.class, () -> binding("innerRadiusDefault").validate(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "nan"))));
    }

    @Test void typeSelectorRestrictsSimpleSymbolsAndPreservesInactiveDrafts() throws Exception {
        for (String branch : List.of("builtin", "current", "package")) onEdt(() -> {
            var editor = binding("valueType").createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("String")); editor.setValue(initial);
            var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var mode = find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.MODE_NAME);
            var symbol = find(panel, JTextField.class, FlutterRadioTypeEditorComponent.SYMBOL_NAME); mode.setSelectedItem(FlutterRadioTypeEditorComponent.CURRENT);
            for (String invalid : List.of("", "List<String>", "Choice.first", "make()", "dynamic", "void")) {
                symbol.setText(invalid); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            }
            PropertyValue expected;
            if (branch.equals("builtin")) { mode.setSelectedItem(FlutterRadioTypeEditorComponent.BUILTIN); find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.PRESET_NAME).setSelectedItem("int"); expected = new PropertyValue.StringValue("int"); }
            else {
                symbol.setText("_Choice"); expected = RadioPropertyContractTest.reference("_Choice");
                if (branch.equals("package")) { mode.setSelectedItem(FlutterRadioTypeEditorComponent.PACKAGE); find(panel, JTextField.class, FlutterRadioTypeEditorComponent.LIBRARY_NAME).setText("package:demo/types.dart"); symbol.setText("Choice");
                    expected = new PropertyValue.DartObjectReferenceValue(Optional.of("package:demo/types.dart"), "Choice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()); }
            }
            assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue()); return null;
        });
    }

    @Test void atomicTypeDialogValidatesAllLocalDraftsBeforePublishingOneFourFieldPayload() throws Exception {
        onEdt(() -> {
            var widget = WidgetNodePrototypeFactory.create(RadioPropertyContractTest.DEF, StableId.random());
            var descriptor = new FeatureDescriptor(); descriptor.setValue(FlutterRadioTypeEditorComponent.CONTEXT_ATTRIBUTE, (java.util.function.Supplier<WidgetNode>) () -> widget);
            var editor = binding("valueType").createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("String")); editor.setValue(initial);
            var env = environment(editor, descriptor); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
            var panel = editor.getCustomEditor(); find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.PRESET_NAME).setSelectedItem("int");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue()); assertEquals(0, commits.get());
            var valuePanel = find(panel, JPanel.class, "flutter.radioType.value");
            find(valuePanel, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(FlutterRadioValueEditorComponent.INTEGER);
            find(valuePanel, JTextField.class, FlutterRadioValueEditorComponent.INTEGER_NAME).setText("2");
            var groupPanel = find(panel, JPanel.class, "flutter.radioType.groupValue");
            find(groupPanel, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(FlutterRadioValueEditorComponent.PROJECT);
            find(groupPanel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("invalid()");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            find(groupPanel, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(FlutterRadioValueEditorComponent.NONE);
            find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.NULLABILITY_NAME).setSelectedIndex(1);
            assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID);
            var committed = (FlutterPropertyCellValue) editor.getValue(); var draft = committed.radioTypeEdit().orElseThrow(); assertEquals(1, commits.get());
            assertEquals(widget.id(), draft.widgetId()); assertEquals(FlutterPropertyCellValue.RadioTypeEdit.snapshot(widget), draft.baseline());
            assertEquals(Optional.of(new PropertyValue.IntegerValue(BigInteger.TWO)), draft.requested().get("value"));
            assertEquals(Optional.of(new PropertyValue.NullValue()), draft.requested().get("groupValue"));
            assertEquals(Optional.of(new PropertyValue.BooleanValue(false)), draft.requested().get("nullableValueType"));
            assertEquals(Optional.of(new PropertyValue.StringValue("int")), draft.requested().get("valueType"));
            assertEquals(4, draft.requested().size()); assertEquals(4, widget.properties().size());
            return null;
        });
    }

    @Test void radioCallbackNoopNullOmissionAndNullableEnabledRemainDifferent() throws Exception {
        var callback = FlutterTypedPropertyEditors.binding(RadioPropertyContractTest.field("onChanged"), Optional.empty(), false, List.of("noop")).orElseThrow();
        for (String modeValue : List.of(FlutterPresetDartReferenceEditorComponent.OMIT, "Explicit null", FlutterPresetDartReferenceEditorComponent.PRESET)) onEdt(() -> {
            var editor = callback.createEditor(); editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("noop"))); var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor();
            find(panel, JComboBox.class, FlutterPresetDartReferenceEditorComponent.MODE_NAME).setSelectedItem(modeValue); env.setState(PropertyEnv.STATE_VALID);
            var expected = modeValue.equals(FlutterPresetDartReferenceEditorComponent.OMIT) ? FlutterPropertyCellValue.unset()
                    : FlutterPropertyCellValue.explicit(modeValue.equals("Explicit null") ? new PropertyValue.NullValue() : new PropertyValue.StringValue("noop"));
            assertEquals(expected, editor.getValue()); return null;
        });
        for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))) onEdt(() -> {
            var editor = binding("enabled").createEditor(); editor.setValue(initial); var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor();
            assertEquals(SwingConstants.CENTER, find(panel, JCheckBox.class, FlutterNullableChoiceEditorComponent.VALUE_NAME).getHorizontalAlignment());
            env.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); return null;
        });
    }

    @Test void radioReferenceHelpDescribesSelectedTypeAndInvariantRegistryInsteadOfPlaceholderTypes() throws Exception {
        for (String field : List.of("value", "groupValue", "groupRegistry", "onChanged")) onEdt(() -> {
            var constraint = RadioPropertyContractTest.field(field).constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow();
            assertEquals(field.equals("groupRegistry") ? "RadioGroupRegistry<Object>" : field.equals("onChanged") ? "ValueChanged<Object?>" : "Object?", constraint.expectedDartType());
            var localBinding = field.equals("onChanged") ? FlutterTypedPropertyEditors.binding(RadioPropertyContractTest.field(field), Optional.empty(), false, List.of("noop")).orElseThrow() : binding(field);
            var editor = localBinding.createEditor(); editor.setValue(FlutterPropertyCellValue.explicit(RadioPropertyContractTest.reference("_reference")));
            environment(editor, new FeatureDescriptor()); String help = text(editor.getCustomEditor());
            if (field.equals("groupRegistry")) { assertTrue(help.contains("selected Radio type")); assertTrue(help.contains("registerClient contract")); }
            else if (field.equals("onChanged")) { assertTrue(help.contains("ValueChanged<T?>")); assertTrue(help.contains("Enabled false preserves")); assertTrue(help.contains("retains the actual SDK Radio")); assertTrue(help.contains("benign controlled preview callback")); assertFalse(help.contains("explicit preview-unavailable state")); }
            else { assertTrue(help.contains("selected type")); assertTrue(help.contains("Dynamic references are rejected")); }
            return null;
        });
    }

    @Test void radioGroupTypeDraftHasOnlyThreeFieldsAndCancelInvalidAndSingleCommitStayIsolated() throws Exception {
        onEdt(() -> {
            var widget = new WidgetNode(StableId.random(), RadioGroupPropertyContractTest.DEF.typeId(), RadioGroupPropertyContractTest.full(), Map.of());
            var descriptor = new FeatureDescriptor(); descriptor.setValue(FlutterRadioTypeEditorComponent.CONTEXT_ATTRIBUTE, (java.util.function.Supplier<WidgetNode>) () -> widget);
            var binding = FlutterTypedPropertyEditors.binding(RadioGroupPropertyContractTest.DEF.property(new PropertyName("valueType")).orElseThrow()).orElseThrow();
            var editor = binding.createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("String")); editor.setValue(initial);
            var env = environment(editor, descriptor); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
            var panel = editor.getCustomEditor(); assertTrue(panel.getAccessibleContext().getAccessibleName().contains("RadioGroup"));
            var tabs = find(panel, JTabbedPane.class, "flutter.radioType.tabs"); assertEquals(2, tabs.getTabCount()); assertEquals("Group value", tabs.getTitleAt(1));
            assertTrue(text(panel).contains("all three")); assertTrue(text(panel).contains("Descendant Radio types are never changed"));
            find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.PRESET_NAME).setSelectedItem("int");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue()); assertEquals(0, commits.get());
            var group = find(panel, JPanel.class, "flutter.radioType.groupValue");
            find(group, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(FlutterRadioValueEditorComponent.PROJECT);
            find(group, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("invalid()");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            find(group, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(FlutterRadioValueEditorComponent.INTEGER);
            find(group, JTextField.class, FlutterRadioValueEditorComponent.INTEGER_NAME).setText("2");
            find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.NULLABILITY_NAME).setSelectedIndex(1);
            assertEquals(initial, editor.getValue()); assertEquals(0, commits.get());
            env.setState(PropertyEnv.STATE_VALID); var result = (FlutterPropertyCellValue) editor.getValue(); var draft = result.radioTypeEdit().orElseThrow();
            assertEquals(widget.type(), draft.widgetType()); assertEquals(widget.id(), draft.widgetId()); assertEquals(FlutterPropertyCellValue.RadioTypeEdit.snapshot(widget), draft.baseline());
            assertEquals(Map.of("valueType", Optional.of(new PropertyValue.StringValue("int")), "nullableValueType", Optional.of(new PropertyValue.BooleanValue(false)),
                    "groupValue", Optional.of(new PropertyValue.IntegerValue(BigInteger.TWO))), draft.requested());
            assertEquals(1, commits.get()); env.setState(PropertyEnv.STATE_NEEDS_VALIDATION); env.setState(PropertyEnv.STATE_VALID); assertEquals(1, commits.get());
            assertEquals(RadioGroupPropertyContractTest.full(), widget.properties());
            var canceledEditor = binding.createEditor(); canceledEditor.setValue(initial); environment(canceledEditor, descriptor);
            var canceledPanel = canceledEditor.getCustomEditor(); find(canceledPanel, JComboBox.class, FlutterRadioTypeEditorComponent.PRESET_NAME).setSelectedItem("Object");
            assertEquals(initial, canceledEditor.getValue()); assertEquals(RadioGroupPropertyContractTest.full(), widget.properties());
            return null;
        });
    }

    @Test void radioGroupRequiredCallbackOffersOnlyNoopOrValidatedReferenceWithAccurateHelp() throws Exception {
        onEdt(() -> {
            var binding = FlutterTypedPropertyEditors.binding(RadioGroupPropertyContractTest.DEF.property(new PropertyName("onChanged")).orElseThrow(),
                    Optional.empty(), false, List.of("noop")).orElseThrow();
            var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("noop")); var editor = binding.createEditor(); editor.setValue(initial);
            var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var mode = find(panel, JComboBox.class, FlutterPresetDartReferenceEditorComponent.MODE_NAME);
            assertEquals(List.of(FlutterPresetDartReferenceEditorComponent.PRESET, FlutterPresetDartReferenceEditorComponent.PROJECT),
                    java.util.stream.IntStream.range(0, mode.getItemCount()).mapToObj(mode::getItemAt).toList());
            mode.setSelectedItem(FlutterPresetDartReferenceEditorComponent.PROJECT);
            assertTrue(text(panel).contains("required RadioGroup callback")); assertTrue(text(panel).contains("null and omission are not accepted"));
            assertFalse(text(panel).contains("Enabled false preserves"));
            find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("bad()");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_onChanged");
            assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.explicit(RadioGroupPropertyContractTest.reference("_onChanged")), editor.getValue());
            return null;
        });
    }

    private static String text(Component component) {
        String result = component instanceof JTextArea area ? area.getText() : "";
        if (component instanceof Container container) for (Component child : container.getComponents()) result += text(child);
        return result;
    }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(RadioPropertyContractTest.field(name)).orElseThrow(); }
    private static PropertyEnv environment(PropertyEditor editor, FeatureDescriptor descriptor) { var env = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static <T extends Component> T find(Component component, Class<T> type, String name) {
        if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component);
        if (component instanceof Container container) for (Component child : container.getComponents()) { T result = maybe(child, type, name); if (result != null) return result; }
        throw new AssertionError("Missing " + name);
    }
    private static <T extends Component> T maybe(Component component, Class<T> type, String name) {
        if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component);
        if (component instanceof Container container) for (Component child : container.getComponents()) { T result = maybe(child, type, name); if (result != null) return result; }
        return null;
    }
    private static <T> T onEdt(java.util.concurrent.Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}
