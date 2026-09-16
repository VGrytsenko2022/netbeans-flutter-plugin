package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
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

class ElevatedButtonLayerBuilderPropertyTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE).orElseThrow();
    private static final List<String> BUILDERS = List.of("styleBackgroundBuilder", "styleForegroundBuilder");
    private static final PropertyName COLOR = new PropertyName("styleBackgroundColor");
    private static final String FUNCTION_TYPE = "Widget Function(BuildContext, Set<WidgetState>, Widget? child)";

    @Test
    void bothNonnullBuildersAreOrdinaryCommonStyleRowsWithExactTypesAndNoAdditionalEvents() {
        var node = node(widget(Map.of()), new ArrayList<>());
        assertEquals(288, DEFINITION.properties().size()); assertEquals(11, ElevatedButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT);
        assertEquals(BUILDERS, ElevatedButtonWidgetPropertySchema.layerBuilderProperties());
        assertEquals("ButtonLayerBuilder", ElevatedButtonWidgetPropertySchema.BUTTON_LAYER_BUILDER_TYPE);
        var common = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals("elevatedButtonCommonStyle")).findFirst().orElseThrow();
        assertEquals("Properties", common.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        for (String name : BUILDERS) {
            var row = row(node, name);
            assertTrue(Arrays.stream(common.getProperties()).anyMatch(value -> value == row)); assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            assertEquals(Boolean.TRUE, row.getValue(FlutterDartObjectReferenceEditorComponent.ELEVATED_BUTTON_LAYER_ATTRIBUTE));
            assertFalse(row.getPropertyEditor() instanceof FlutterWidgetEventPropertyEditor);
            assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE, binding(name).editorKind());
            assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE), DEFINITION.property(new PropertyName(name)).orElseThrow().acceptedKinds());
            var descriptor = WidgetEventCatalog.eventsFor(DEFINITION).stream().filter(value -> value.propertyName().value().equals(name)).findFirst().orElseThrow();
            assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor.kind()); assertEquals("Widget", descriptor.signature().returnType());
            assertEquals(List.of("BuildContext", "Set<WidgetState>", "Widget?"), descriptor.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
            assertDoesNotThrow(() -> binding(name).validate(FlutterPropertyCellValue.unset()));
            for (var invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_builder"), new PropertyValue.StringValue("(context, states, child) => child")))
                assertThrows(IllegalArgumentException.class, () -> binding(name).validate(FlutterPropertyCellValue.explicit(invalid)));
        }
        var events = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(Set.of("onPressed", "onLongPress", "onHover", "onFocusChange"), Arrays.stream(events.getProperties()).map(Node.Property::getName).collect(java.util.stream.Collectors.toSet()));
        var catalog = BuiltInWidgetCatalog.getDefault().definitions(); assertEquals(247, catalog.size());
        assertEquals(8260, catalog.stream().mapToInt(definition -> definition.properties().size()).sum());
        assertEquals(273, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).count());
        assertEquals(51, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(value -> value.kind() == WidgetEventDescriptor.Kind.BUILDER).count());
        assertEquals(202, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(value -> value.kind() == WidgetEventDescriptor.Kind.EVENT).count());
    }

    @Test
    void bothEditorsCommitAndReopenExactLocalPackageMemberGetterAndFactoryReferences() throws Exception {
        for (String name : BUILDERS) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean invoke : List.of(false, true)) {
            var expected = reference(name, imported, member, invoke); var row = row(node(widget(Map.of()), new ArrayList<>()), name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.unset()); var env = environment(editor, row); var panel = editor.getCustomEditor();
                var omit = find(panel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class); assertTrue(omit.isSelected());
                omit.doClick(); var root = find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class);
                for (String invalid : List.of("", "() => child", "builder(context)", "a + b")) {
                    root.setText(invalid); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState());
                    assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                }
                find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(imported ? 1 : 0);
                if (imported) find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/layers.dart");
                root.setText(expected.rootSymbol()); find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText(expected.member().orElse(""));
                find(panel, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(invoke ? 1 : 0);
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue(), "Cancel retains the original value without publishing nested drafts.");
                var preview = find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class).getText();
                assertTrue(preview.contains(expected.rootSymbol())); assertEquals(invoke, preview.contains("()"));
                env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = row.getPropertyEditor(); reopened.setValue(editor.getValue()); var reopenedEnv = environment(reopened, row); var reopenedPanel = reopened.getCustomEditor();
                assertFalse(find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class).isSelected());
                assertEquals(expected.rootSymbol(), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).getText());
                assertEquals(expected.member().orElse(""), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).getText());
                reopenedEnv.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), reopened.getValue());
            });
        }
    }

    @Test
    void omissionThemeLayerPlacementClipAndCanvasGuidanceAreExplicitAndScopedToElevatedButton() throws Exception {
        for (String name : BUILDERS) {
            var row = row(node(widget(Map.of()), new ArrayList<>()), name);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.unset()); environment(editor, row); var panel = editor.getCustomEditor();
                String help = panel.getAccessibleContext().getAccessibleDescription();
                for (String expected : List.of(FUNCTION_TYPE, "child may be null", "returned Widget must not be null", "theme builder may remain active", "Clip.antiAlias", "Clip.none",
                        "explicit clipBehavior wins", "never executes", "placeholder keeps the editable child", "geometry and clipping are approximate", "Cancel publishes nothing"))
                    assertTrue(help.contains(expected), help);
                assertTrue(help.contains(name.equals(BUILDERS.getFirst()) ? "wraps the whole button content, including padding, inside its Material"
                        : "wraps the nullable child inside the button's padding and alignment"));
                assertEquals(name + ": <theme/framework fallback; local builder omitted>", find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class).getText());
                assertTrue(find(panel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class).getAccessibleContext().getAccessibleDescription().contains("reset does not force no layer"));
            });
        }
        for (String type : List.of("TextButton", "OutlinedButton", "FilledButton", "IconButton")) {
            var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material." + type)).orElseThrow();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, WidgetNodePrototypeFactory.create(definition, StableId.random()), definition, ignored -> { });
            for (String name : BUILDERS) {
                assertNull(rawRow(node, name).getValue(FlutterDartObjectReferenceEditorComponent.ELEVATED_BUTTON_LAYER_ATTRIBUTE));
                assertTrue(rawRow(node, name).getPropertyEditor().supportsCustomEditor());
            }
        }
    }

    @Test
    void resetEachLayerPreservesOtherLayerStyleChildAndStableRowsAndOnlyNotifiesThatLayer() throws Exception {
        for (String name : BUILDERS) {
            var background = reference(BUILDERS.getFirst(), true, true, true); var foreground = reference(BUILDERS.getLast(), false, false, false);
            var initial = widget(Map.of(new PropertyName(BUILDERS.getFirst()), background, new PropertyName(BUILDERS.getLast()), foreground));
            var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var row = row(node, name);
            String other = name.equals(BUILDERS.getFirst()) ? BUILDERS.getLast() : BUILDERS.getFirst(); var otherRow = row(node, other); var colorRow = rawRow(node, COLOR.value());
            var sets = node.getPropertySets(); var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); var currentValue = row.getValue();
            SwingUtilities.invokeAndWait(() -> {
                var env = environment(editor, row); var panel = editor.getCustomEditor();
                find(panel, FlutterDartObjectReferenceEditorComponent.DEFAULT_NAME, JCheckBox.class).doClick();
                assertEquals(currentValue, editor.getValue()); assertTrue(commands.isEmpty(), "Cancel cannot clear either builder.");
                env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            });
            row.setValue((FlutterPropertyCellValue) editor.getValue()); assertEquals(List.of(new ResetProperty(initial.id(), new PropertyName(name))), commands);
            var properties = new LinkedHashMap<>(initial.properties()); properties.remove(new PropertyName(name));
            var next = new WidgetNode(initial.id(), initial.type(), properties, initial.slots());
            var changes = new ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
            node.refreshPresentation(next, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertEquals(List.of(name), changes); assertSame(row, row(node, name)); assertSame(otherRow, row(node, other)); assertSame(colorRow, rawRow(node, COLOR.value()));
            assertArrayEquals(sets, node.getPropertySets()); assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
            assertEquals(initial.properties().get(new PropertyName(other)), otherRow.getValue().explicitValue().orElseThrow());
            assertEquals(initial.properties().get(COLOR), next.properties().get(COLOR)); assertEquals(initial.slots(), next.slots());
            var restored = reference(name, true, false, true); row.setValue(FlutterPropertyCellValue.explicit(restored));
            assertEquals(new SetProperty(initial.id(), new PropertyName(name), restored), commands.getLast());
            var reopened = node(next, new ArrayList<>()); assertEquals(otherRow.getValue(), row(reopened, other).getValue()); assertEquals(row.getValue(), row(reopened, name).getValue());
        }
    }

    @Test
    void openLayerDraftAndResetCannotConsumeWithdrawnMutationAuthorityAndReadyKeepsSameRows() throws Exception {
        for (String name : BUILDERS) {
            var initial = widget(Map.of(new PropertyName(name), reference(name, false, false, false))); var commands = new ArrayList<DesignerCommand>();
            var node = node(initial, commands); var row = row(node, name); var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); PropertyEnv[] env = new PropertyEnv[1];
            SwingUtilities.invokeAndWait(() -> {
                env[0] = environment(editor, row); var panel = editor.getCustomEditor(); find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).setText("_changedWhileOpen");
            });
            node.refreshPresentation(initial, DEFINITION, null, null, null, FlutterImageAssetChoices.empty()); assertSame(row, row(node, name)); assertFalse(row.canWrite());
            SwingUtilities.invokeAndWait(() -> env[0].setState(PropertyEnv.STATE_VALID));
            assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue()));
            assertThrows(IllegalAccessException.class, row::restoreDefaultValue); assertTrue(commands.isEmpty());
            node.refreshPresentation(initial, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, row(node, name)); assertTrue(row.canWrite());
            row.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(initial.id(), new PropertyName(name))), commands);
            var readOnly = new FlutterWidgetPropertiesNode(Children.LEAF, initial, DEFINITION); assertFalse(rawRow(readOnly, name).canWrite()); assertFalse(rawRow(readOnly, name).supportsDefaultValue());
        }
    }

    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(DEFINITION.property(new PropertyName(name)).orElseThrow()).orElseThrow(); }
    private static PropertyValue.DartObjectReferenceValue reference(String name, boolean imported, boolean member, boolean invoke) {
        String symbol = name.equals(BUILDERS.getFirst()) ? "background" : "foreground";
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/layers.dart") : Optional.empty(),
                member ? "Layers" : imported ? symbol : "_" + symbol, member ? Optional.of(symbol) : Optional.empty(),
                invoke ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                invoke ? Optional.of(false) : Optional.empty());
    }
    private static WidgetNode widget(Map<PropertyName, PropertyValue> builders) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random()); var properties = new LinkedHashMap<>(prototype.properties());
        properties.put(COLOR, new PropertyValue.ColorValue(0xFF336699L)); properties.putAll(builders);
        var text = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        var slots = new LinkedHashMap<>(prototype.slots()); slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(text));
        return new WidgetNode(prototype.id(), prototype.type(), properties, slots);
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION, commands::add); }
    private static Node.Property<?> rawRow(FlutterWidgetPropertiesNode node, String name) {
        return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(row -> row.getName().equals(name)).findFirst().orElseThrow();
    }
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
