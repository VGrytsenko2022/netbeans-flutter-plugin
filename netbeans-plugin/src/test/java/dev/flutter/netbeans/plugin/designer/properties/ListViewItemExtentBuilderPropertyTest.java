package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class ListViewItemExtentBuilderPropertyTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetDefinition DEFINITION = BuiltInWidgetCatalog.getDefault()
            .find(ListViewWidgetPropertySchema.LIST_VIEW_TYPE).orElseThrow();
    private static final PropertyName BUILDER = new PropertyName("itemExtentBuilder");
    private static final PropertyName FIXED = new PropertyName("itemExtent");
    private static final PropertyName REVERSE = new PropertyName("reverse");

    @Test
    void nullableBuilderIsAnOrdinaryLayoutRowWithRenderingSignatureAndNoNativeEvents() {
        var node = node(widget(Map.of()), new ArrayList<>()); assertEquals(18, DEFINITION.properties().size());
        var layout = Arrays.stream(node.getPropertySets()).filter(set -> set.getName().equals("listViewLayout")).findFirst().orElseThrow();
        assertEquals("Properties", layout.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        assertTrue(Arrays.stream(layout.getProperties()).anyMatch(value -> value.getName().equals(BUILDER.value())));
        assertTrue(Arrays.stream(node.getPropertySets()).noneMatch(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)));
        assertEquals("ItemExtentBuilder?", ListViewWidgetPropertySchema.ITEM_EXTENT_BUILDER_TYPE);
        var row = row(node, BUILDER); assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
        assertEquals(Boolean.TRUE, row.getValue(FlutterDartObjectReferenceEditorComponent.LIST_VIEW_EXTENT_BUILDER_ATTRIBUTE));
        assertFalse(row.getPropertyEditor() instanceof FlutterWidgetEventPropertyEditor);
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE, binding().editorKind());
        assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.DART_OBJECT_REFERENCE), DEFINITION.property(BUILDER).orElseThrow().acceptedKinds());
        var descriptor = WidgetEventCatalog.eventsFor(DEFINITION).stream().filter(value -> value.propertyName().equals(BUILDER)).findFirst().orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor.kind()); assertTrue(descriptor.nullableCallback()); assertTrue(descriptor.allowsExplicitNull());
        assertEquals("double?", descriptor.signature().returnType());
        assertEquals(List.of("int", "SliverLayoutDimensions"), descriptor.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
        assertEquals(List.of("index", "dimensions"), descriptor.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::name).toList());
        assertTrue(descriptor.signature().parameters().stream().noneMatch(WidgetEventDescriptor.Parameter::named));
        assertTrue(descriptor.signature().importUris().contains("package:flutter/rendering.dart"));
        assertDoesNotThrow(() -> binding().validate(FlutterPropertyCellValue.unset()));
        assertDoesNotThrow(() -> binding().validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        for (var invalid : List.of(new PropertyValue.CallbackValue("_extent"), new PropertyValue.StringValue("(index, dimensions) => null")))
            assertThrows(IllegalArgumentException.class, () -> binding().validate(FlutterPropertyCellValue.explicit(invalid)));
        var catalog = BuiltInWidgetCatalog.getDefault().definitions(); assertEquals(219, catalog.size());
        assertEquals(7442, catalog.stream().mapToInt(definition -> definition.properties().size()).sum());
        assertEquals(240, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).count());
        // Current audited callable inventory includes all admitted sliver builders.
        assertEquals(50, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(value -> value.kind() == WidgetEventDescriptor.Kind.BUILDER).count());
        assertEquals(174, catalog.stream().flatMap(definition -> WidgetEventCatalog.eventsFor(definition).stream()).filter(value -> value.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        assertEquals(88, catalog.stream().filter(definition -> !WidgetEventCatalog.eventsFor(definition).isEmpty()).count());
    }

    @Test
    void omissionAndExplicitNullCommitAndReopenSeparatelyWhileCancelPreservesReference() throws Exception {
        for (boolean explicitNull : List.of(false, true)) {
            var initial = reference(true, true, true); var commands = new ArrayList<DesignerCommand>(); var row = row(node(widget(Map.of(BUILDER, initial)), commands), BUILDER);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(initial)); var env = environment(editor, row); var panel = editor.getCustomEditor();
                var mode = find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class);
                assertEquals(List.of(FlutterNullableDartReferenceEditorComponent.OMIT, "Use existing sizing (explicit null)", FlutterNullableDartReferenceEditorComponent.PROJECT),
                        java.util.stream.IntStream.range(0, mode.getItemCount()).mapToObj(mode::getItemAt).toList());
                mode.setSelectedItem(explicitNull ? FlutterNullableDartReferenceEditorComponent.nullText(binding()) : FlutterNullableDartReferenceEditorComponent.OMIT);
                assertEquals(FlutterPropertyCellValue.explicit(initial), editor.getValue()); assertTrue(commands.isEmpty());
                var expected = explicitNull ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()) : FlutterPropertyCellValue.unset();
                env.setState(PropertyEnv.STATE_VALID); assertEquals(expected, editor.getValue());
                assertEquals(explicitNull ? "Use existing sizing (explicit null)" : FlutterPropertyCellValue.NOT_SET_TEXT, editor.getAsText());
                var reopened = row.getPropertyEditor(); reopened.setValue(expected); environment(reopened, row);
                assertEquals(mode.getSelectedItem(), find(reopened.getCustomEditor(), FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).getSelectedItem());
            });
        }
    }

    @Test
    void currentPackageGetterMemberAndNullableFactoriesCommitExactReferencesOnlyOnOk() throws Exception {
        for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean invoke : List.of(false, true)) {
            var expected = reference(imported, member, invoke); var row = row(node(widget(Map.of()), new ArrayList<>()), BUILDER);
            SwingUtilities.invokeAndWait(() -> {
                var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.unset()); var env = environment(editor, row); var panel = editor.getCustomEditor();
                find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.PROJECT);
                var root = find(panel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class);
                for (String invalid : List.of("", "() => null", "extent(index)", "a + b")) {
                    root.setText(invalid); env.setState(PropertyEnv.STATE_VALID); assertEquals(PropertyEnv.STATE_INVALID, env.getState()); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                }
                find(panel, FlutterDartObjectReferenceEditorComponent.SCOPE_NAME, JComboBox.class).setSelectedIndex(imported ? 1 : 0);
                if (imported) find(panel, FlutterDartObjectReferenceEditorComponent.LIBRARY_URI_NAME, JTextField.class).setText("package:app/extents.dart");
                root.setText(expected.rootSymbol()); find(panel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).setText(expected.member().orElse(""));
                find(panel, FlutterDartObjectReferenceEditorComponent.ACCESS_NAME, JComboBox.class).setSelectedIndex(invoke ? 1 : 0);
                assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
                var preview = find(panel, FlutterDartObjectReferenceEditorComponent.PREVIEW_NAME, JLabel.class).getText(); assertTrue(preview.contains(expected.rootSymbol())); assertEquals(invoke, preview.contains("()"));
                env.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue());
                var reopened = row.getPropertyEditor(); reopened.setValue(editor.getValue()); var reopenedEnv = environment(reopened, row); var reopenedPanel = reopened.getCustomEditor();
                assertEquals(expected.rootSymbol(), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME, JTextField.class).getText());
                assertEquals(expected.member().orElse(""), find(reopenedPanel, FlutterDartObjectReferenceEditorComponent.MEMBER_NAME, JTextField.class).getText());
                reopenedEnv.setState(PropertyEnv.STATE_VALID); assertEquals(FlutterPropertyCellValue.explicit(expected), reopened.getValue());
            });
        }
    }

    @Test
    void guidanceExplainsDimensionsNullableCallbackOutOfRangeResultConflictAndNaturalCanvasApproximation() throws Exception {
        var row = row(node(widget(Map.of(BUILDER, reference(false, false, false))), new ArrayList<>()), BUILDER);
        SwingUtilities.invokeAndWait(() -> {
            var editor = row.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(reference(false, false, false))); environment(editor, row); var panel = editor.getCustomEditor();
            String help = panel.getAccessibleContext().getAccessibleDescription();
            for (String expected : List.of("double? Function(int index, SliverLayoutDimensions dimensions)", "scrollOffset", "precedingScrollExtent", "viewportMainAxisExtent", "crossAxisExtent",
                    "nullable callback", "valid extent for every actual child", "out-of-range marker", "not a default size", "truncate existing children", "Reset fixed itemExtent",
                    "reset itemExtentBuilder or set explicit null", "Even a nullable reference cannot coexist", "no property is silently cleared", "never executes", "natural-size approximation", "Cancel publishes nothing"))
                assertTrue(help.contains(expected), help);
            assertEquals(help, find(panel, FlutterDartObjectReferenceEditorComponent.PANEL_NAME, JPanel.class).getAccessibleContext().getAccessibleDescription());
            var note = find(panel, FlutterNullableDartReferenceEditorComponent.NOTE_NAME, JTextArea.class);
            find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.nullText(binding()));
            assertTrue(note.getText().startsWith("Stores explicit null: existing natural or fixed sizing remains active"));
            find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.OMIT);
            assertTrue(note.getText().startsWith("Omits itemExtentBuilder and restores existing natural or fixed sizing"));
        });
    }

    @Test
    void realValidationConflictPropagatesBothWaysWithoutClearingAndExplicitNullWithFixedExtentRemainsValid() throws Exception {
        var initial = widget(Map.of(FIXED, new PropertyValue.DoubleValue(BigDecimal.valueOf(48))));
        var current = new AtomicReference<>(initial); var attempted = new ArrayList<DesignerCommand>(); var applied = new ArrayList<DesignerCommand>();
        FlutterWidgetPropertiesNode.PropertyMutationHandler handler = command -> {
            attempted.add(command); var properties = new LinkedHashMap<>(current.get().properties());
            if (command instanceof SetProperty set) properties.put(set.propertyName(), set.value());
            else if (command instanceof ResetProperty reset) properties.remove(reset.propertyName());
            else fail("An independent extent row must never silently clear/patch another field: " + command);
            var next = new WidgetNode(current.get().id(), current.get().type(), properties, current.get().slots(), current.get().extensions(), current.get().stateBinding(), current.get().propertyBindings());
            var validation = new WidgetTreeValidator().validate(document(next), BuiltInWidgetCatalog.getDefault());
            if (!validation.valid()) throw new IllegalArgumentException(validation.toString());
            applied.add(command); current.set(next);
        };
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, initial, DEFINITION, handler); var row = row(node, BUILDER); var fixed = row(node, FIXED); var reverse = rawRow(node, REVERSE);
        var sets = node.getPropertySets(); var value = reference(true, true, true);
        var failure = assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(value)));
        assertTrue(failure.getMessage().contains("No value was cleared")); assertTrue(failure.getMessage().contains("itemExtentBuilder"));
        assertEquals(List.of(new SetProperty(initial.id(), BUILDER, value)), attempted); assertTrue(applied.isEmpty()); assertSame(initial, current.get());
        row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        var changes = new ArrayList<String>(); node.addPropertyChangeListener(event -> changes.add(event.getPropertyName()));
        node.refreshPresentation(current.get(), DEFINITION, handler, null, null, FlutterImageAssetChoices.empty());
        assertEquals(List.of(BUILDER.value()), changes); assertSame(row, row(node, BUILDER)); assertSame(fixed, row(node, FIXED)); assertSame(reverse, rawRow(node, REVERSE)); assertArrayEquals(sets, node.getPropertySets());
        assertEquals(initial.properties().get(FIXED), current.get().properties().get(FIXED));
        row.restoreDefaultValue(); node.refreshPresentation(current.get(), DEFINITION, handler, null, null, FlutterImageAssetChoices.empty());
        assertFalse(current.get().properties().containsKey(BUILDER)); assertTrue(current.get().properties().containsKey(FIXED));
        fixed.restoreDefaultValue(); node.refreshPresentation(current.get(), DEFINITION, handler, null, null, FlutterImageAssetChoices.empty());
        row.setValue(FlutterPropertyCellValue.explicit(value)); node.refreshPresentation(current.get(), DEFINITION, handler, null, null, FlutterImageAssetChoices.empty());
        var bound = current.get(); int appliedBefore = applied.size(); attempted.clear();
        assertThrows(IllegalArgumentException.class, () -> fixed.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(BigDecimal.valueOf(36)))));
        assertEquals(List.of(new SetProperty(initial.id(), FIXED, new PropertyValue.DoubleValue(BigDecimal.valueOf(36)))), attempted);
        assertEquals(appliedBefore, applied.size()); assertSame(bound, current.get()); assertSame(row, row(node, BUILDER)); assertSame(reverse, rawRow(node, REVERSE));
        assertEquals(initial.slots(), current.get().slots()); assertEquals(initial.properties().get(REVERSE), current.get().properties().get(REVERSE));
        var reopened = node(current.get(), new ArrayList<>()); assertEquals(row.getValue(), row(reopened, BUILDER).getValue());
        assertEquals(initial.slots(), current.get().slots(), "Child IDs, order and the child's existing onChanged callback remain intact.");
    }

    @Test
    void nullableDraftAndResetCannotCommitAfterAuthorityWithdrawalAndReadyRestoresSameRow() throws Exception {
        var initial = widget(Map.of(BUILDER, reference(false, false, false))); var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var row = row(node, BUILDER);
        var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); PropertyEnv[] env = new PropertyEnv[1];
        SwingUtilities.invokeAndWait(() -> {
            env[0] = environment(editor, row); var panel = editor.getCustomEditor();
            find(panel, FlutterNullableDartReferenceEditorComponent.MODE_NAME, JComboBox.class).setSelectedItem(FlutterNullableDartReferenceEditorComponent.nullText(binding()));
        });
        node.refreshPresentation(initial, DEFINITION, null, null, null, FlutterImageAssetChoices.empty()); assertSame(row, row(node, BUILDER)); assertFalse(row.canWrite());
        SwingUtilities.invokeAndWait(() -> env[0].setState(PropertyEnv.STATE_VALID));
        assertThrows(IllegalAccessException.class, () -> row.setValue((FlutterPropertyCellValue) editor.getValue())); assertThrows(IllegalAccessException.class, row::restoreDefaultValue); assertTrue(commands.isEmpty());
        node.refreshPresentation(initial, DEFINITION, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, row(node, BUILDER)); assertTrue(row.canWrite());
        row.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(initial.id(), BUILDER)), commands);
        var readOnly = new FlutterWidgetPropertiesNode(Children.LEAF, initial, DEFINITION); assertFalse(rawRow(readOnly, BUILDER).canWrite()); assertFalse(rawRow(readOnly, BUILDER).supportsDefaultValue());
    }

    private static FlutterTypedPropertyEditors.Binding binding() { return FlutterTypedPropertyEditors.binding(DEFINITION.property(BUILDER).orElseThrow()).orElseThrow(); }
    private static PropertyValue.DartObjectReferenceValue reference(boolean imported, boolean member, boolean invoke) {
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/extents.dart") : Optional.empty(),
                member ? "Extents" : imported ? "extent" : "_extent", member ? Optional.of("extent") : Optional.empty(),
                invoke ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                invoke ? Optional.of(false) : Optional.empty());
    }
    private static WidgetNode widget(Map<PropertyName, PropertyValue> values) {
        var prototype = WidgetNodePrototypeFactory.create(DEFINITION, StableId.random()); var properties = new LinkedHashMap<>(prototype.properties());
        properties.put(REVERSE, new PropertyValue.BooleanValue(true)); properties.putAll(values);
        var field = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.TextField"), Map.of(new PropertyName("onChanged"), new PropertyValue.CallbackValue("_onChanged")), Map.of());
        var text = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        return new WidgetNode(prototype.id(), prototype.type(), properties, Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(field, text))));
    }
    private static DesignerDocument document(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.random(), new DartSourceDescriptor("list.dart", "ListScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEFINITION, commands::add); }
    private static Node.Property<?> rawRow(FlutterWidgetPropertiesNode node, PropertyName name) { return Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(row -> row.getName().equals(name.value())).findFirst().orElseThrow(); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> row(FlutterWidgetPropertiesNode node, PropertyName name) { return (Node.Property<FlutterPropertyCellValue>) rawRow(node, name); }
    private static PropertyEnv environment(PropertyEditor editor, Node.Property<?> row) { var env = PropertyEnv.create(row); ((ExPropertyEditor) editor).attachEnv(env); return env; }
    private static <T extends Component> T find(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) return type.cast(root);
        if (root instanceof Container container) for (Component child : container.getComponents()) {
            try { return find(child, name, type); } catch (IllegalArgumentException ignored) { }
        }
        throw new IllegalArgumentException("Missing component " + name);
    }
}
