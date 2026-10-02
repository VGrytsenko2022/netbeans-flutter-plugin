package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.model.*;
import java.awt.*;
import java.beans.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class RadioListTileEditorComponentsTest {
    @Test void fiveFieldDialogValidatesValueGroupAndCallbackAndPublishesExactlyOneDraft() throws Exception {
        onEdt(() -> {
            var widget = RadioListTilePropertyContractTest.prototype(); var descriptor = descriptor();
            descriptor.setValue(FlutterRadioTypeEditorComponent.CONTEXT_ATTRIBUTE, (java.util.function.Supplier<WidgetNode>) () -> widget);
            var editor = binding("valueType").createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("String")); editor.setValue(initial);
            var env = environment(editor, descriptor); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
            var panel = editor.getCustomEditor(); assertTrue(panel.getAccessibleContext().getAccessibleName().contains("RadioListTile"));
            var tabs = find(panel, JTabbedPane.class, "flutter.radioType.tabs");
            assertEquals(List.of("Type", "Value", "Group value", "On changed"), java.util.stream.IntStream.range(0, tabs.getTabCount()).mapToObj(tabs::getTitleAt).toList());
            assertTrue(text(panel).contains("five")); assertTrue(text(panel).contains("RadioGroup"));
            find(panel, JComboBox.class, FlutterRadioTypeEditorComponent.PRESET_NAME).setSelectedItem("int");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue());
            var value = find(panel, JPanel.class, "flutter.radioType.value"); var group = find(panel, JPanel.class, "flutter.radioType.groupValue");
            for (var field : List.of(value, group)) { find(field, JComboBox.class, FlutterRadioValueEditorComponent.MODE_NAME).setSelectedItem(FlutterRadioValueEditorComponent.INTEGER); find(field, JTextField.class, FlutterRadioValueEditorComponent.INTEGER_NAME).setText(field == value ? "2" : "1"); }
            var callback = find(panel, JPanel.class, "flutter.radioType.onChanged");
            find(callback, JComboBox.class, FlutterPresetDartReferenceEditorComponent.MODE_NAME).setSelectedItem(FlutterPresetDartReferenceEditorComponent.PROJECT);
            find(callback, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("bad()");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(initial, editor.getValue()); assertEquals(0, commits.get());
            find(callback, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_integerChanged");
            assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID);
            var draft = ((FlutterPropertyCellValue) editor.getValue()).radioTypeEdit().orElseThrow();
            assertEquals(5, draft.requested().size()); assertEquals(FlutterPropertyCellValue.RadioTypeEdit.snapshot(widget), draft.baseline());
            assertEquals(Optional.of(new PropertyValue.IntegerValue(BigInteger.TWO)), draft.requested().get("value"));
            assertEquals(Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE)), draft.requested().get("groupValue"));
            assertEquals(Optional.of(RadioListTilePropertyContractTest.reference("_integerChanged")), draft.requested().get("onChanged"));
            assertFalse(draft.requested().containsKey("groupRegistry")); assertEquals(1, commits.get());
            env.setState(PropertyEnv.STATE_NEEDS_VALIDATION); env.setState(PropertyEnv.STATE_VALID); assertEquals(1, commits.get());
            assertEquals(new PropertyValue.StringValue("option"), widget.properties().get(new PropertyName("value"))); assertEquals(3, widget.slots().size());
            return null;
        });
    }

    @Test void callbackOmitNullNoopReferencesAndFactoriesStayDraftOnlyUntilOkAndReopenExactly() throws Exception {
        for (String branch : List.of(FlutterPresetDartReferenceEditorComponent.OMIT, "Explicit null", FlutterPresetDartReferenceEditorComponent.PRESET, "local", "getter", "member", "factory")) onEdt(() -> {
            var editor = binding("onChanged").createEditor(); var initial = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("noop")); editor.setValue(initial);
            var env = environment(editor, descriptor()); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
            var panel = editor.getCustomEditor(); var mode = find(panel, JComboBox.class, FlutterPresetDartReferenceEditorComponent.MODE_NAME); FlutterPropertyCellValue expected;
            if (List.of("local", "getter", "member", "factory").contains(branch)) {
                mode.setSelectedItem(FlutterPresetDartReferenceEditorComponent.PROJECT);
                String root = branch.equals("getter") ? "_callbackGetter" : branch.equals("member") ? "handlers" : branch.equals("factory") ? "buildCallback" : "_onChanged";
                Optional<String> library = branch.equals("member") || branch.equals("factory") ? Optional.of("package:app/handlers.dart") : Optional.empty();
                if (library.isPresent()) { find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1); find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText(library.orElseThrow()); }
                find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText(root);
                if (branch.equals("member")) find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME).setText("onChanged");
                if (branch.equals("factory")) find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                expected = FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(library, root, branch.equals("member") ? Optional.of("onChanged") : Optional.empty(), branch.equals("factory") ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, branch.equals("factory") ? Optional.of(false) : Optional.empty()));
            } else { mode.setSelectedItem(branch); expected = branch.equals(FlutterPresetDartReferenceEditorComponent.OMIT) ? FlutterPropertyCellValue.unset() : FlutterPropertyCellValue.explicit(branch.equals("Explicit null") ? new PropertyValue.NullValue() : new PropertyValue.StringValue("noop")); }
            assertTrue(text(panel).contains("RadioListTile")); assertTrue(text(panel).contains("RadioGroup"));
            assertEquals(initial, editor.getValue()); assertEquals(0, commits.get(), "Cancel leaves the unpublished draft local.");
            env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); assertEquals(1, commits.get());
            var reopened = binding("onChanged").createEditor(); reopened.setValue(expected); var reopenedEnv = environment(reopened, descriptor()); reopened.getCustomEditor(); reopenedEnv.setState(PropertyEnv.STATE_VALID); assertEquals(expected, reopened.getValue());
            return null;
        });
    }

    @Test void nullableBooleansKeepCenteredCheckboxAndAllFourLiteralStates() throws Exception {
        for (String name : List.of("enabled", "dense", "isThreeLine", "enableFeedback")) for (var initial : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)), FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))) onEdt(() -> {
            var editor = binding(name).createEditor(); editor.setValue(initial); var env = environment(editor, descriptor()); var panel = editor.getCustomEditor();
            assertEquals(SwingConstants.CENTER, find(panel, JCheckBox.class, FlutterNullableChoiceEditorComponent.VALUE_NAME).getHorizontalAlignment());
            env.setState(PropertyEnv.STATE_VALID); assertEquals(initial, editor.getValue()); return null;
        });
    }

    private static FeatureDescriptor descriptor() { var result = new FeatureDescriptor(); result.setValue(FlutterDartObjectReferenceEditorComponent.RADIO_TILE_CALLBACK_ATTRIBUTE, Boolean.TRUE); return result; }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(RadioListTilePropertyContractTest.DEF.property(new PropertyName(name)).orElseThrow(), Optional.empty(), false, name.equals("onChanged") ? List.of("noop") : List.of()).orElseThrow(); }
    private static PropertyEnv environment(PropertyEditor editor, FeatureDescriptor descriptor) { var env = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static String text(Component component) { String result = component instanceof JTextArea area ? area.getText() : ""; if (component instanceof Container container) for (Component child : container.getComponents()) result += text(child); return result; }
    private static <T extends Component> T find(Component component, Class<T> type, String name) { T result = maybe(component, type, name); if (result == null) throw new AssertionError("Missing " + name); return result; }
    private static <T extends Component> T maybe(Component component, Class<T> type, String name) { if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component); if (component instanceof Container container) for (Component child : container.getComponents()) { T result = maybe(child, type, name); if (result != null) return result; } return null; }
    private static <T> T onEdt(java.util.concurrent.Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}
