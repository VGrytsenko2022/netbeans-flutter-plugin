package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.*;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class ScaffoldBottomSheetScrimBuilderPropertyTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE).orElseThrow();
    private static final PropertyName BUILDER = new PropertyName("bottomSheetScrimBuilder");
    private static final String FUNCTION_TYPE = "Widget? Function(BuildContext, Animation<double>)";

    @Test
    void optionalNonnullBuilderAppearsInAppearanceAndNeverAddsANativeEvent() {
        var node = node(widget(Optional.empty()), new ArrayList<>());
        assertEquals(18, DEFINITION.properties().size());
        var appearance = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals("scaffoldAppearance")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(appearance.getProperties()).anyMatch(row -> row.getName().equals(BUILDER.value())));
        assertEquals("Properties", appearance.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(Set.of("onDrawerChanged", "onEndDrawerChanged"), Arrays.stream(events.getProperties()).map(Node.Property::getName).collect(java.util.stream.Collectors.toSet()));
        var descriptor = WidgetEventCatalog.eventsFor(DEFINITION).stream().filter(event -> event.propertyName().equals(BUILDER)).findFirst().orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor.kind()); assertEquals("Widget?", descriptor.signature().returnType());
        assertEquals(List.of("BuildContext", "Animation<double>"), descriptor.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
        assertEquals(253, BuiltInWidgetCatalog.getDefault().definitions().stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).count());
        assertEquals(50, BuiltInWidgetCatalog.getDefault().definitions().stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream())
                .filter(event -> event.kind() == WidgetEventDescriptor.Kind.BUILDER).count());
        var row = row(node); assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE, binding().editorKind());
        assertFalse(row.getPropertyEditor() instanceof FlutterWidgetEventPropertyEditor);
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE), DEFINITION.property(BUILDER).orElseThrow().acceptedKinds());
        assertEquals(FUNCTION_TYPE, ScaffoldWidgetPropertySchema.BOTTOM_SHEET_SCRIM_BUILDER_TYPE);
        assertThrows(IllegalArgumentException.class, () -> binding().validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalArgumentException.class, () -> binding().validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("null"))));
        assertDoesNotThrow(() -> binding().validate(FlutterPropertyCellValue.unset()));
    }

    @Test
    void referenceGetterMemberAndZeroArgumentFactoryModesCommitExactValuesOnlyOnOk() throws Exception {
        for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean invoke : List.of(false, true)) {
            var expected = reference(imported, member, invoke);
            SwingUtilities.invokeAndWait(() -> {
                var editor = binding().createEditor(); editor.setValue(FlutterPropertyCellValue.unset());
                var env = environment(editor); var panel = editor.getCustomEditor();
                var omit = find(panel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class);
                assertTrue(omit.isSelected());
                find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(imported ? 1 : 0);
                if (imported) find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/scrims.dart");
                find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText(expected.rootSymbol());
                find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText(expected.member().orElse(""));
                find(panel, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(invoke ? 1 : 0);
                omit.doClick(); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), "Drafts / Cancel cannot change the model or user body.");
                var preview = find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class).getText();
                assertTrue(preview.contains(expected.rootSymbol())); assertEquals(invoke, preview.contains("()"));
                env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = binding().createEditor(); reopened.setValue(FlutterPropertyCellValue.explicit(expected)); environment(reopened);
                var reopenedPanel = reopened.getCustomEditor();
                assertFalse(find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class).isSelected());
                assertEquals(expected.rootSymbol(), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).getText());
                assertEquals(expected.member().orElse(""), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).getText());
            });
        }
    }

    @Test
    void omissionPreviewExplainsSdkDefaultAndNullableReturnWithoutOfferingNullCallback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var editor = binding().createEditor(); editor.setValue(FlutterPropertyCellValue.unset()); environment(editor);
            var panel = editor.getCustomEditor(); String help = panel.getAccessibleContext().getAccessibleDescription();
            for (String expected : List.of(FUNCTION_TYPE, "70%", "100%", "return null", "callback itself to null is invalid", "Canvas previews the SDK default"))
                assertTrue(help.contains(expected), help);
            var preview = find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class);
            assertEquals("bottomSheetScrimBuilder: <Flutter default; argument omitted>", preview.getText());
            assertFalse(preview.getText().contains("default null"));
            assertTrue(find(panel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class).getAccessibleContext()
                    .getAccessibleDescription().contains("never sets the callback to null"));
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("(context, animation) => null"));
        });
    }

    @Test
    void cancelResetAndPresentationRefreshKeepExactRowsReferencesAndOnlyNotifyTheChangedField() throws Exception {
        var initialReference = reference(true, true, true); var initial = widget(Optional.of(initialReference));
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var row = row(node);
        Node.PropertySet[] sets = node.getPropertySets(); var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
        SwingUtilities.invokeAndWait(() -> {
            var env = environment(editor); var panel = editor.getCustomEditor();
            find(panel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class).doClick();
            assertEquals(FlutterPropertyCellValue.explicit(initialReference), editor.getValue(), "Cancelling keeps the exact package/member/factory reference.");
            assertTrue(commands.isEmpty()); env.setState(PropertyEnv.STATE_VALID);
            assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        });
        row.setValue((FlutterPropertyCellValue) editor.getValue());
        assertEquals(List.of(new ResetProperty(initial.id(), BUILDER)), commands);
        var changes = new ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
        var next = new WidgetNode(initial.id(), initial.type(), Map.of(), initial.slots());
        node.refreshPresentation(next, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertEquals(List.of(BUILDER.value()), changes); assertSame(row, row(node));
        assertArrayEquals(sets, node.getPropertySets()); assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
        var restored = reference(false, false, false); row.setValue(FlutterPropertyCellValue.explicit(restored));
        assertEquals(new SetProperty(initial.id(), BUILDER, restored), commands.getLast());
        assertEquals(initialReference, initial.properties().get(BUILDER));
    }

    @Test
    void staleDialogCannotCommitOrResetAfterMutationAuthorityIsWithdrawnAndReadyRestoresSameRow() throws Exception {
        var reference = reference(false, false, false); var initial = widget(Optional.of(reference));
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var row = row(node);
        var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); PropertyEnv[] env = new PropertyEnv[1];
        SwingUtilities.invokeAndWait(() -> {
            env[0] = environment(editor); var panel = editor.getCustomEditor();
            find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("_changedWhileOpen");
        });
        node.refreshPresentation(initial, DEFINITION, null, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, row(node)); assertFalse(row.canWrite());
        SwingUtilities.invokeAndWait(() -> env[0].setState(PropertyEnv.STATE_VALID));
        assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue()));
        assertThrows(IllegalAccessException.class, row::restoreDefaultValue); assertTrue(commands.isEmpty());
        node.refreshPresentation(initial, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, row(node)); assertTrue(row.canWrite()); row.restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(initial.id(), BUILDER)), commands);
        var readOnly = new FlutterWidgetPropertiesNode(Children.LEAF, initial, DEFINITION);
        assertFalse(rawRow(readOnly).canWrite()); assertFalse(rawRow(readOnly).supportsDefaultValue());
    }

    private static FlutterTypedPropertyEditors.Binding binding() { return FlutterTypedPropertyEditors.binding(DEFINITION.property(BUILDER).orElseThrow()).orElseThrow(); }
    private static PropertyValue.DartObjectReferenceValue reference(boolean imported, boolean member, boolean invoke) {
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/scrims.dart") : Optional.empty(),
                member ? "ScrimBuilders" : imported ? "scrim" : "_scrim", member ? Optional.of("scrim") : Optional.empty(),
                invoke ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                invoke ? Optional.of(false) : Optional.empty());
    }
    private static WidgetNode widget(Optional<PropertyValue.DartObjectReferenceValue> reference) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random());
        return new WidgetNode(prototype.id(), prototype.type(), reference.<Map<PropertyName, PropertyValue>>map(value -> Map.of(BUILDER, value)).orElse(Map.of()), prototype.slots());
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION, commands::add); }
    private static Node.Property<?> rawRow(FlutterWidgetPropertiesNode node) {
        return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(row -> row.getName().equals(BUILDER.value())).findFirst().orElseThrow();
    }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> row(FlutterWidgetPropertiesNode node) { return (Node.Property<FlutterPropertyCellValue>) rawRow(node); }
    private static PropertyEnv environment(PropertyEditor editor) {
        var env = PropertyEnv.create(new java.beans.FeatureDescriptor()); ((ExPropertyEditor) editor).attachEnv(env); return env;
    }
    private static <T extends Component> T find(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) return type.cast(root);
        if (root instanceof Container container) for (Component child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
}
