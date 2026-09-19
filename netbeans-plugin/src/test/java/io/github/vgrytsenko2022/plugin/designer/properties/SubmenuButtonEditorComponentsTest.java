package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.SubmenuButtonWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.model.*;
import java.awt.Component;
import java.awt.Container;
import java.beans.FeatureDescriptor;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import static org.junit.jupiter.api.Assertions.*;

class SubmenuButtonEditorComponentsTest {
    @Test void fourIconBucketsDistinguishOmissionNullEmptyIconAndTypedWidgetFactoriesUntilOk() throws Exception {
        for (String name : SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties())
        for (String modeName : List.of(FlutterLocalDartReferenceEditorComponent.OMIT, FlutterLocalDartReferenceEditorComponent.NULL, FlutterLocalDartReferenceEditorComponent.ICON, FlutterLocalDartReferenceEditorComponent.PROJECT)) onEdt(() -> {
            var binding = binding(name); assertEquals(FlutterTypedPropertyEditors.EditorKind.ICON_WIDGET_REFERENCE, binding.editorKind());
            var initial = FlutterPropertyCellValue.explicit(reference("_existingWidget", false)); var editor = binding.createEditor(); editor.setValue(initial);
            var env = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
            find(panel, JComboBox.class, FlutterLocalDartReferenceEditorComponent.MODE_NAME).setSelectedItem(modeName);
            FlutterPropertyCellValue expected;
            if (modeName.equals(FlutterLocalDartReferenceEditorComponent.OMIT)) expected = FlutterPropertyCellValue.unset();
            else if (modeName.equals(FlutterLocalDartReferenceEditorComponent.NULL)) expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
            else if (modeName.equals(FlutterLocalDartReferenceEditorComponent.ICON)) {
                var none = find(panel, JCheckBox.class, FlutterPropertyEditorComponents.MATERIAL_ICON_NONE_NAME); if (!none.isSelected()) none.doClick();
                expected = FlutterPropertyCellValue.explicit(PropertyValue.IconDataValue.none());
            } else {
                find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_customArrow");
                find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                expected = FlutterPropertyCellValue.explicit(reference("_customArrow", true));
            }
            assertEquals(initial, editor.getValue(), "Cancel must not publish an inactive local/reference draft.");
            assertTrue(text(panel).contains("Icon(null)")); assertTrue(text(panel).contains("Explicit null is terminal"));
            env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
            var reopened = binding.createEditor(); reopened.setValue(expected); var next = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) reopened).attachEnv(next); reopened.getCustomEditor(); assertEquals(expected, reopened.getValue());
            assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("Icon(Icons.menu)")))); return null;
        });
    }

    @Test void hoverDelayRetainsSignedMicrosecondsAndDurationFactoryWithoutOfferingNull() throws Exception {
        var binding = binding("hoverOpenDelayUs"); assertEquals(FlutterTypedPropertyEditors.EditorKind.DURATION_REFERENCE, binding.editorKind());
        assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        for (String modeName : List.of(FlutterDurationReferenceEditorComponent.OMIT, FlutterDurationReferenceEditorComponent.LITERAL, FlutterDurationReferenceEditorComponent.PROJECT)) onEdt(() -> {
            var initial = FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.valueOf(1234567))); var editor = binding.createEditor(); editor.setValue(initial);
            var env = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
            var mode = find(panel, JComboBox.class, FlutterDurationReferenceEditorComponent.MODE_NAME);
            for (int index = 0; index < mode.getItemCount(); index++) assertNotEquals(FlutterDurationReferenceEditorComponent.NULL, mode.getItemAt(index));
            mode.setSelectedItem(modeName); FlutterPropertyCellValue expected;
            if (modeName.equals(FlutterDurationReferenceEditorComponent.OMIT)) expected = FlutterPropertyCellValue.unset();
            else if (modeName.equals(FlutterDurationReferenceEditorComponent.LITERAL)) {
                find(panel, JTextField.class, FlutterDurationReferenceEditorComponent.VALUE_NAME).setText("-1234567"); expected = FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.valueOf(-1234567)));
            } else {
                find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("_hoverDelay"); find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                expected = FlutterPropertyCellValue.explicit(reference("_hoverDelay", true));
            }
            assertEquals(initial, editor.getValue()); String help = find(panel, JTextArea.class, FlutterDurationReferenceEditorComponent.NOTE_NAME).getText();
            assertTrue(help.contains("SubmenuButton")); assertTrue(help.contains("explicit null is not allowed")); assertTrue(help.contains("direct MenuBar child"));
            env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
            var reopened = binding.createEditor(); reopened.setValue(expected); ((ExPropertyEditor) reopened).attachEnv(PropertyEnv.create(new FeatureDescriptor())); reopened.getCustomEditor(); assertEquals(expected, reopened.getValue()); return null;
        });
    }

    @Test void everyNullableControllerStyleIconAndEventReferenceKeepsFactoryDraftAndResetIndependent() throws Exception {
        for (String name : List.of("controller", "focusNode", "statesController", "style", "menuStyle", "submenuIcon", "onHover", "onFocusChange", "onOpen", "onClose", "onAnimationStatusChanged"))
        for (String modeName : List.of(FlutterNullableDartReferenceEditorComponent.OMIT, "Explicit null", FlutterNullableDartReferenceEditorComponent.PROJECT)) onEdt(() -> {
            var binding = binding(name); var initial = FlutterPropertyCellValue.explicit(SubmenuButtonPropertyContractTest.value(name)); var editor = binding.createEditor(); editor.setValue(initial);
            var env = PropertyEnv.create(new FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); var panel = editor.getCustomEditor();
            find(panel, JComboBox.class, FlutterNullableDartReferenceEditorComponent.MODE_NAME).setSelectedItem(modeName); FlutterPropertyCellValue expected;
            if (modeName.equals(FlutterNullableDartReferenceEditorComponent.OMIT)) expected = FlutterPropertyCellValue.unset();
            else if (modeName.equals("Explicit null")) expected = FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
            else {
                find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME).setSelectedIndex(1);
                find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME).setText("package:app/submenus.dart");
                find(panel, JTextField.class, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME).setText("makeValue");
                find(panel, JComboBox.class, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME).setSelectedIndex(1);
                expected = FlutterPropertyCellValue.explicit(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/submenus.dart"), "makeValue", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)));
            }
            assertEquals(initial, editor.getValue()); env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
            var reopened = binding.createEditor(); reopened.setValue(expected); ((ExPropertyEditor) reopened).attachEnv(PropertyEnv.create(new FeatureDescriptor())); reopened.getCustomEditor(); assertEquals(expected, reopened.getValue()); return null;
        });
    }

    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(SubmenuButtonPropertyContractTest.DEF.property(new PropertyName(name)).orElseThrow()).orElseThrow(); }
    private static PropertyValue.DartObjectReferenceValue reference(String root, boolean factory) { return new PropertyValue.DartObjectReferenceValue(Optional.empty(), root, Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty()); }
    private static String text(Component component) { var result = new StringBuilder(); if (component instanceof JTextArea area) result.append(area.getText()); if (component instanceof Container container) for (var child : container.getComponents()) result.append(text(child)); return result.toString(); }
    private static <T extends Component> T find(Component component, Class<T> type, String name) { if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component); if (component instanceof Container container) for (var child : container.getComponents()) { try { return find(child, type, name); } catch (NoSuchElementException ignored) { } } throw new NoSuchElementException(name); }
    private static <T> T onEdt(Callable<T> operation) throws Exception { var task = new FutureTask<T>(operation); SwingUtilities.invokeAndWait(task); return task.get(); }
}
