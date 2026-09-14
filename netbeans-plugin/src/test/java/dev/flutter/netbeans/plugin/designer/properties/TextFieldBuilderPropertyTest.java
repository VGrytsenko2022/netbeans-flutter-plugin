package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
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
import java.util.LinkedHashMap;
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

class TextFieldBuilderPropertyTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE).orElseThrow();
    private static final List<String> BUILDERS = List.of("buildCounter", "contextMenuBuilder");
    private static final PropertyName ON_CHANGED = new PropertyName("onChanged");
    private static final PropertyName ENABLED = new PropertyName("enabled");
    private static final String COUNTER_SIGNATURE = "Widget? Function(BuildContext, {required int currentLength, required int? maxLength, required bool isFocused})";

    @Test
    void nullableBuildersHaveAccurateNamedSignaturesAndStayInPropertiesWithoutNewEvents() {
        var node = node(widget(Map.of()), new ArrayList<>()); assertEquals(56, DEFINITION.properties().size());
        var builders = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals("textFieldBuilders")).findFirst().orElseThrow();
        assertEquals("Properties", builders.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        assertEquals(BUILDERS, Arrays.stream(builders.getProperties()).map(Node.Property::getName).toList());
        assertEquals("InputCounterWidgetBuilder?", TextFieldWidgetPropertySchema.INPUT_COUNTER_BUILDER_TYPE);
        assertEquals("EditableTextContextMenuBuilder?", TextFieldWidgetPropertySchema.CONTEXT_MENU_BUILDER_TYPE);
        for (String name : BUILDERS) {
            var row = row(node, name); assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            assertEquals(Boolean.TRUE, row.getValue(FlutterDartObjectReferenceEditorComponent.TEXT_FIELD_BUILDER_ATTRIBUTE));
            assertFalse(row.getPropertyEditor() instanceof FlutterWidgetEventPropertyEditor);
            assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE, binding(name).editorKind());
            assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.DART_OBJECT_REFERENCE), DEFINITION.property(new PropertyName(name)).orElseThrow().acceptedKinds());
            var descriptor = descriptor(name); assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor.kind()); assertTrue(descriptor.nullableCallback());
            assertTrue(descriptor.allowsExplicitNull()); assertFalse(descriptor.required());
            assertDoesNotThrow(() -> binding(name).validate(FlutterPropertyCellValue.unset()));
            assertDoesNotThrow(() -> binding(name).validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
            for (var invalid : List.of(new PropertyValue.CallbackValue("_builder"), new PropertyValue.StringValue("null"), new PropertyValue.StringValue("(context) => null")))
                assertThrows(IllegalArgumentException.class, () -> binding(name).validate(FlutterPropertyCellValue.explicit(invalid)));
        }
        var counter = descriptor("buildCounter").signature(); assertEquals("Widget?", counter.returnType());
        assertEquals(List.of("BuildContext", "int", "int?", "bool"), counter.parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
        assertEquals(List.of("context", "currentLength", "maxLength", "isFocused"), counter.parameters().stream().map(WidgetEventDescriptor.Parameter::name).toList());
        assertFalse(counter.parameters().getFirst().named());
        assertTrue(counter.parameters().subList(1, 4).stream().allMatch(parameter -> parameter.named() && parameter.required()));
        assertEquals(COUNTER_SIGNATURE, counter.dartFunctionType());
        var menu = descriptor("contextMenuBuilder").signature(); assertEquals("Widget", menu.returnType());
        assertEquals(List.of("BuildContext", "EditableTextState"), menu.parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
        assertTrue(menu.parameters().stream().noneMatch(WidgetEventDescriptor.Parameter::named));
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(Set.of("onChanged", "onEditingComplete", "onSubmitted", "onAppPrivateCommand", "onTap", "onTapOutside", "onTapUpOutside"),
                Arrays.stream(events.getProperties()).map(Node.Property::getName).collect(java.util.stream.Collectors.toSet()));
        var catalog = BuiltInWidgetCatalog.getDefault().definitions(); assertEquals(211, catalog.size());
        assertEquals(7341, catalog.stream().mapToInt(definition -> definition.properties().size()).sum());
        assertEquals(239, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).count());
        assertEquals(50, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(value -> value.kind() == WidgetEventDescriptor.Kind.BUILDER).count());
        assertEquals(174, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(value -> value.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        assertTrue(rawRow(node, ENABLED.value()).getPropertyEditor().isPaintable()); assertNull(rawRow(node, ENABLED.value()).getPropertyEditor().getTags());
    }

    @Test
    void omissionAndExplicitNullAreSeparateModesThatCommitAndReopenWithoutPublishingOnCancel() throws Exception {
        for (String name : BUILDERS) for (boolean explicitNull : List.of(false, true)) {
            var initial = reference(name, true, true, true); var commands = new ArrayList<DesignerCommand>(); var row = row(node(widget(Map.of(new PropertyName(name), initial)), commands), name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(initial)); var env = environment(editor, row); var panel = editor.getCustomEditor();
                var mode = find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                assertEquals(3, mode.getItemCount()); assertEquals(List.of(FlutterNullableDartReferenceEditorComponent.OMIT,
                        FlutterNullableDartReferenceEditorComponent.nullText(binding(name)), FlutterNullableDartReferenceEditorComponent.PROJECT),
                        java.util.stream.IntStream.range(0, mode.getItemCount()).mapToObj(mode::getItemAt).toList());
                mode.setSelectedItem(explicitNull ? FlutterNullableDartReferenceEditorComponent.nullText(binding(name)) : FlutterNullableDartReferenceEditorComponent.OMIT);
                assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue()); assertTrue(commands.isEmpty());
                var expected = explicitNull ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
                env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
                assertEquals(explicitNull ? FlutterNullableDartReferenceEditorComponent.nullText(binding(name)) : FlutterPropertyCellValue.NOT_SET_TEXT, editor.getAsText());
                var reopened = row.getPropertyEditor(); reopened.setValue(expected); environment(reopened, row);
                assertEquals(mode.getSelectedItem(), find(reopened.getCustomEditor(), FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
            });
        }
    }

    @Test
    void bothEditorsRetainExactCurrentPackageGetterMemberAndNullableCallbackFactoryReferences() throws Exception {
        for (String name : BUILDERS) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean invoke : List.of(false, true)) {
            var expected = reference(name, imported, member, invoke); var row = row(node(widget(Map.of()), new ArrayList<>()), name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.unset()); var env = environment(editor, row); var panel = editor.getCustomEditor();
                find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT);
                var root = find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class);
                for (String invalid : List.of("", "() => null", "builder(context)", "a + b")) {
                    root.setText(invalid); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState());
                    assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                }
                find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(imported ? 1 : 0);
                if (imported) find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/field_builders.dart");
                root.setText(expected.rootSymbol()); find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText(expected.member().orElse(""));
                find(panel, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(invoke ? 1 : 0);
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), "Nested drafts remain local until OK.");
                var preview = find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class).getText();
                assertTrue(preview.contains(expected.rootSymbol())); assertEquals(invoke, preview.contains("()"));
                env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = row.getPropertyEditor(); reopened.setValue(editor.getValue()); var reopenedEnv = environment(reopened, row); var reopenedPanel = reopened.getCustomEditor();
                assertEquals(FlutterNullableDartReferenceEditorComponent.PROJECT, find(reopenedPanel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
                assertEquals(expected.rootSymbol(), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).getText());
                assertEquals(expected.member().orElse(""), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).getText());
                reopenedEnv.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), reopened.getValue());
            });
        }
    }

    @Test
    void scopedHelpDistinguishesNullableCallbackFromNullableReturnAndDefaultCanvasApproximation() throws Exception {
        for (String name : BUILDERS) {
            var row = row(node(widget(Map.of(new PropertyName(name), reference(name, false, false, false))), new ArrayList<>()), name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(reference(name, false, false, false))); environment(editor, row); var panel = editor.getCustomEditor();
                var note = find(panel, FlutterNullableDartReferenceEditorComponent.NOTE_NAME, JTextArea.class);
                String help = panel.getAccessibleContext().getAccessibleDescription();
                for (String expected : List.of("nullable callback", "getter/member", "zero-argument factory", "Canvas never executes", "SDK-default approximation",
                        "cannot reproduce the callback result or null", "Flutter widget test", "onChanged", "user Dart bodies unchanged"))
                    assertTrue(help.contains(expected), help);
                assertEquals(help, find(panel, FlutterDartObjectReferenceEditorComponent.PANEL_NAME, JPanel.class).getAccessibleContext().getAccessibleDescription());
                if (name.equals("buildCounter")) {
                    assertTrue(help.contains(COUNTER_SIGNATURE)); assertTrue(help.contains("three named parameters are required"));
                    assertTrue(help.contains("callback returning null hides both its counter and generated Semantics"));
                } else {
                    assertTrue(help.contains("Widget Function(BuildContext, EditableTextState)")); assertTrue(help.contains("returned Widget must not be null"));
                    assertTrue(help.contains("nullable reference resolving to null disables"));
                }
                find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.nullText(binding(name)));
                assertTrue(note.getText().startsWith(name.equals("buildCounter") ? "Stores explicit null: SDK counter logic remains active" : "Stores explicit null: the context menu is disabled"));
                find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.OMIT);
                assertTrue(note.getText().startsWith(name.equals("buildCounter") ? "Omits buildCounter: SDK counter logic remains active" : "Omits contextMenuBuilder: restores the SDK platform default menu"));
            });
        }
        var focus = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Focus")).orElseThrow().property(new PropertyName("focusNode")).orElseThrow();
        assertEquals("Explicit null", FlutterNullableDartReferenceEditorComponent.nullText(FlutterTypedPropertyEditors.binding(focus).orElseThrow()));
    }

    @Test
    void eachBuilderNullAndResetOnlyChangeThatRowAndPreserveOtherBuilderOnChangedAndBooleanRow() throws Exception {
        for (String name : BUILDERS) {
            var initial = widget(Map.of(new PropertyName(BUILDERS.getFirst()), reference(BUILDERS.getFirst(), true, true, true),
                    new PropertyName(BUILDERS.getLast()), reference(BUILDERS.getLast(), false, false, false)));
            var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var row = row(node, name); var sets = node.getPropertySets();
            String other = name.equals(BUILDERS.getFirst()) ? BUILDERS.getLast() : BUILDERS.getFirst(); var otherRow = row(node, other);
            var onChanged = rawRow(node, ON_CHANGED.value()); var enabled = rawRow(node, ENABLED.value()); var key = new PropertyName(name);
            row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); assertEquals(List.of(new SetProperty(initial.id(), key, new PropertyValue.NullValue())), commands);
            var properties = new LinkedHashMap<>(initial.properties()); properties.put(key, new PropertyValue.NullValue());
            var changes = new ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
            node.refreshPresentation(new WidgetNode(initial.id(), initial.type(), properties, initial.slots()), DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertEquals(List.of(name), changes); assertSame(row, row(node, name)); assertSame(otherRow, row(node, other));
            assertSame(onChanged, rawRow(node, ON_CHANGED.value())); assertSame(enabled, rawRow(node, ENABLED.value())); assertArrayEquals(sets, node.getPropertySets());
            assertEquals(initial.properties().get(new PropertyName(other)), otherRow.getValue().explicitValue().orElseThrow());
            row.restoreDefaultValue(); assertEquals(new ResetProperty(initial.id(), key), commands.getLast()); properties.remove(key); changes.clear();
            var next = new WidgetNode(initial.id(), initial.type(), properties, initial.slots());
            node.refreshPresentation(next, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty()); assertEquals(List.of(name), changes);
            assertSame(row, row(node, name)); assertSame(enabled, rawRow(node, ENABLED.value())); assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
            assertEquals(initial.properties().get(ON_CHANGED), next.properties().get(ON_CHANGED)); assertEquals(initial.properties().get(ENABLED), next.properties().get(ENABLED));
            var reopened = node(next, new ArrayList<>()); assertEquals(row.getValue(), row(reopened, name).getValue()); assertEquals(otherRow.getValue(), row(reopened, other).getValue());
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.CallbackValue("_onChanged")), rawRow(reopened, ON_CHANGED.value()).getValue());
        }
    }

    @Test
    void openNullableDraftAndResetRejectWithdrawnAuthorityAndReadyRestoresSameRows() throws Exception {
        for (String name : BUILDERS) {
            var initial = widget(Map.of(new PropertyName(name), reference(name, false, false, false))); var commands = new ArrayList<DesignerCommand>();
            var node = node(initial, commands); var row = row(node, name); var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); PropertyEnv[] env = new PropertyEnv[1];
            SwingUtilities.invokeAndWait(() -> {
                env[0] = environment(editor, row); var panel = editor.getCustomEditor();
                find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.nullText(binding(name)));
            });
            node.refreshPresentation(initial, DEFINITION, null, null, null, FlutterImageAssetChoices.empty()); assertSame(row, row(node, name)); assertFalse(row.canWrite());
            SwingUtilities.invokeAndWait(() -> env[0].setState(PropertyEnv.STATE_VALID)); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue());
            assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue())); assertThrows(IllegalAccessException.class, row::restoreDefaultValue);
            assertTrue(commands.isEmpty()); node.refreshPresentation(initial, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, row(node, name)); assertTrue(row.canWrite()); row.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(initial.id(), new PropertyName(name))), commands);
            var readOnly = new FlutterWidgetPropertiesNode(Children.LEAF, initial, DEFINITION); assertFalse(rawRow(readOnly, name).canWrite()); assertFalse(rawRow(readOnly, name).supportsDefaultValue());
        }
    }

    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName(name)).orElseThrow()).orElseThrow(); }
    private static WidgetEventDescriptor descriptor(String name) { return WidgetEventCatalog.eventsFor(DEFINITION).stream().filter(value -> value.propertyName().value().equals(name)).findFirst().orElseThrow(); }
    private static PropertyValue.DartObjectReferenceValue reference(String name, boolean imported, boolean member, boolean invoke) {
        String symbol = name.equals("buildCounter") ? "counter" : "menu";
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/field_builders.dart") : Optional.empty(),
                member ? "FieldBuilders" : imported ? symbol : "_" + symbol, member ? Optional.of(symbol) : Optional.empty(),
                invoke ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                invoke ? Optional.of(false) : Optional.empty());
    }
    private static WidgetNode widget(Map<PropertyName, PropertyValue> builders) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random()); var properties = new LinkedHashMap<>(prototype.properties());
        properties.put(ON_CHANGED, new PropertyValue.CallbackValue("_onChanged")); properties.put(ENABLED, new PropertyValue.BooleanValue(true)); properties.putAll(builders);
        return new WidgetNode(prototype.id(), prototype.type(), properties, prototype.slots());
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION, commands::add); }
    private static Node.Property<?> rawRow(FlutterWidgetPropertiesNode node, String name) { return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(row -> row.getName().equals(name)).findFirst().orElseThrow(); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> row(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) rawRow(node, name); }
    private static PropertyEnv environment(PropertyEditor editor, Node.Property<?> row) { var env = PropertyEnv.create(row); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static <T extends Component> T find(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) return type.cast(root);
        if (root instanceof Container container) for (Component child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
}
