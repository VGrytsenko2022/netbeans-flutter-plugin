package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.awt.*;
import java.beans.*;
import io.github.vgrytsenko2022.designer.command.DesignerCommand;
import java.util.*;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class MenuItemButtonEditorComponentsTest {
    @Test void allNullableObjectAndCallbackEditorsKeepExactLocalImportedGetterMemberFactoryNullAndOmission() throws Exception {
        for (String name : List.of("shortcut", "style", "focusNode", "statesController", "onHover", "onFocusChange"))
            for (String branch : List.of("omit", "null", "local", "getter", "member", "factory")) onEdt(() -> {
                var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.explicit(MenuItemButtonPropertyContractTest.value(name)); editor.setValue(initial);
                var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
                var mode = find(panel, JComboBox.class, FlutterNullableDartReferenceEditorComponent.MODE_NAME); FlutterPropertyCellValue expected;
                if (branch.equals("omit")) { mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.OMIT); expected = FlutterPropertyCellValue.unset(); }
                else if (branch.equals("null")) { mode.setSelectedItem("Explicit null"); expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()); }
                else { mode.setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT); expected = setReference(panel, branch); }
                assertEquals(initial, editor.getValue()); assertEquals(0, commits.get(), "Cancel publishes nothing."); env.setState(PropertyEnv.STATE_VALID);
                assertEquals(expected, editor.getValue(), name + " " + branch); assertEquals(1, commits.get()); reopen(name, expected);
                if (name.equals("shortcut")) { String help = text(panel); assertTrue(help.contains("432")); assertTrue(help.contains("not automatic global key registration")); assertTrue(help.contains("never executes")); }
                return null;
            });
    }

    @Test void ActivationAndBothLayerBuildersKeepStrictReferenceModesAndIndependentReset() throws Exception {
        for (String name : List.of("onPressed", "styleBackgroundBuilder", "styleForegroundBuilder"))
            for (String branch : List.of("omit", "local", "getter", "member", "factory")) onEdt(() -> {
                var editor = binding(name).createEditor(); var initial = FlutterPropertyCellValue.explicit(MenuItemButtonPropertyContractTest.value(name)); editor.setValue(initial);
                var env = environment(editor, new FeatureDescriptor()); var panel = editor.getCustomEditor(); var commits = new AtomicInteger(); editor.addPropertyChangeListener(ignored -> commits.incrementAndGet());
                var omitted = find(panel, JCheckBox.class, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME); FlutterPropertyCellValue expected;
                if (branch.equals("omit")) { assertFalse(omitted.isSelected()); omitted.doClick(); expected = FlutterPropertyCellValue.unset(); } else expected = setReference(panel, branch);
                assertEquals(initial, editor.getValue()); assertEquals(0, commits.get()); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue()); reopen(name, expected);
                assertThrows(IllegalArgumentException.class, () -> binding(name).validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
                return null;
            });
    }

    @Test void ShortcutMatchStringsAndAll432KeyTagsRemainExactWithoutRawExpressions() throws Exception {
        for (String value : List.of("", "k", "K", "ж", "é", "🎹", "two characters", " ")) onEdt(() -> { reopen("shortcutCharacter", FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(value))); return null; });
        var key = binding("shortcutTrigger").createEditor(); assertEquals(433, key.getTags().length);
        for (String value : MenuShortcutKeyCatalog.names()) {
            var expected = FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("LogicalKeyboardKey", value));
            key.setValue(expected); String encoded = key.getAsText(); assertTrue(List.of(key.getTags()).contains(encoded)); key.setAsText(encoded); assertEquals(expected, key.getValue());
        }
        key.setAsText("<not set>"); assertEquals(FlutterPropertyCellValue.unset(), key.getValue());
        assertThrows(IllegalArgumentException.class, () -> key.setAsText("LogicalKeyboardKey.keyK ?? arbitrary()"));
        for (String name : List.of("clipBehavior", "overflowAxis")) assertThrows(IllegalArgumentException.class, () -> binding(name).validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }

    @Test void CommittedDraftCannotPublishAfterReadOnlyAuthorityWithdrawalAndRowsStayStable() throws Exception {
        for (String name : List.of("shortcut", "style", "onPressed", "styleBackgroundBuilder")) {
            var commands = new java.util.ArrayList<DesignerCommand>(); var widget = MenuItemButtonPropertyContractTest.prototype();
            var node = MenuItemButtonPropertyContractTest.node(widget, commands); var row = MenuItemButtonPropertyContractTest.cell(node, name);
            var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(MenuItemButtonPropertyContractTest.value(name)));
            node.refreshPresentation(widget, MenuItemButtonPropertyContractTest.DEF, null, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, MenuItemButtonPropertyContractTest.cell(node, name)); assertFalse(row.canWrite());
            assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue())); assertTrue(commands.isEmpty());
            assertEquals(FlutterPropertyCellValue.unset(), row.getValue()); assertEquals(widget.slots(), MenuItemButtonPropertyContractTest.prototype().slots());
        }
    }

    private static FlutterPropertyCellValue setReference(Component panel, String branch) {
        String root = branch.equals("getter") ? "_valueGetter" : branch.equals("member") ? "styles" : branch.equals("factory") ? "buildValue" : "_value";
        Optional<String> library = branch.equals("member") || branch.equals("factory") ? Optional.of("package:app/menu_values.dart") : Optional.empty();
        if (library.isPresent()) { find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1); find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText(library.orElseThrow()); }
        find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText(root);
        if (branch.equals("member")) find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME).setText("value");
        if (branch.equals("factory")) find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
        return FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(library, root, branch.equals("member") ? Optional.of("value") : Optional.empty(), branch.equals("factory") ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, branch.equals("factory") ? Optional.of(false) : Optional.empty()));
    }
    private static void reopen(String name, FlutterPropertyCellValue value) { var editor = binding(name).createEditor(); editor.setValue(value); var env = environment(editor, new FeatureDescriptor()); editor.getCustomEditor(); env.setState(PropertyEnv.STATE_VALID); assertEquals(value, editor.getValue(), name); }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(MenuItemButtonPropertyContractTest.DEF.property(new PropertyName(name)).orElseThrow(), Optional.empty(), false, List.of()).orElseThrow(); }
    private static PropertyEnv environment(PropertyEditor editor, FeatureDescriptor descriptor) { var env = PropertyEnv.create(descriptor); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static String text(Component component) { String result = component instanceof JTextArea area ? area.getText() : ""; if (component instanceof Container container) for (Component child : container.getComponents()) result += text(child); return result; }
    private static <T extends Component> T find(Component component, Class<T> type, String name) { T result = maybe(component, type, name); if (result == null) throw new AssertionError("Missing " + name); return result; }
    private static <T extends Component> T maybe(Component component, Class<T> type, String name) { if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component); if (component instanceof Container container) for (Component child : container.getComponents()) { T result = maybe(child, type, name); if (result != null) return result; } return null; }
    private static <T> T onEdt(java.util.concurrent.Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}



