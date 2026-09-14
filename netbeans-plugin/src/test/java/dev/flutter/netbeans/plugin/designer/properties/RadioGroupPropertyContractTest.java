package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class RadioGroupPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("7b3c163a-7eb6-45ae-aef6-495fa3a7c489");

    @Test void fourStableCellsAndRequiredChildKeepExactTypedEditsAndResets() throws Exception {
        var initial = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(4, DEF.properties().size()); assertTrue(DEF.constConstructor());
        assertEquals(Set.of(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME, RadioGroupWidgetPropertySchema.Group.BEHAVIOR.setName(),
                FlutterWidgetPropertiesNode.SLOTS_SET_NAME, FlutterWidgetPropertiesNode.EVENTS_SET_NAME),
                Arrays.stream(sets).map(Node.PropertySet::getName).collect(java.util.stream.Collectors.toSet()));
        assertEquals(4, sets.length);
        var eventRows = Arrays.stream(sets).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(List.of("onChanged"), Arrays.stream(eventRows.getProperties()).map(Node.Property::getName).toList());
        assertEquals(Map.of(p("valueType"), new PropertyValue.StringValue("String"), p("onChanged"), new PropertyValue.StringValue("noop")), initial.properties());
        assertEquals("child", WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).orElseThrow().name().value());
        assertEquals(1, DEF.slots().getFirst().minChildren());
        for (var field : DEF.properties()) {
            node.refreshPresentation(initial, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var cell = cell(node, field.name().value()); var editor = cell.getPropertyEditor(); var next = value(field);
            assertEquals(!field.parameter().required(), cell.supportsDefaultValue()); editor.setValue(FlutterPropertyCellValue.explicit(next));
            assertEquals(FlutterPropertyCellValue.explicit(next), editor.getValue()); cell.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(1, commands.size());
            var after = apply(initial, commands.getFirst()); assertValid(after); assertEquals(next, after.properties().get(field.name())); assertEquals(initial.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            assertSame(cell, cell(node, field.name().value())); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            if (cell.supportsDefaultValue()) { cell.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(after, commands.getFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); }
            else { assertThrows(IllegalArgumentException.class, () -> cell.setValue(FlutterPropertyCellValue.unset())); cell.restoreDefaultValue(); assertTrue(commands.isEmpty()); }
        }
        assertTrue(cell(node, "onChanged").getShortDescription().contains("Null and omission are not accepted"));
    }

    @Test void threeFieldDraftIsAtomicAndPreservesCallbackDescendantsAndSameTypePeerEdits() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), prototype().slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        assertThrows(IllegalArgumentException.class, () -> cell(node, "valueType").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("int")))); assertTrue(commands.isEmpty());
        var baseline = FlutterPropertyCellValue.RadioTypeEdit.snapshot(before); assertEquals(Set.of("valueType", "nullableValueType", "groupValue"), baseline.keySet());
        var requested = new LinkedHashMap<>(baseline); requested.put("valueType", Optional.of(new PropertyValue.StringValue("int"))); requested.put("groupValue", Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.TWO)));
        var draft = draft(before, requested); cell(node, "valueType").setValue(draft); assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst());
        var after = apply(before, commands.getFirst()); assertValid(after); assertEquals(requested, FlutterPropertyCellValue.RadioTypeEdit.snapshot(after));
        assertEquals(before.properties().get(p("onChanged")), after.properties().get(p("onChanged"))); assertEquals(before.slots(), after.slots());
        node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
        assertThrows(IllegalArgumentException.class, () -> cell(node, "valueType").setValue(draft)); assertTrue(commands.isEmpty());
        var same = new LinkedHashMap<>(FlutterPropertyCellValue.RadioTypeEdit.snapshot(after)); same.put("groupValue", Optional.empty());
        cell(node, "valueType").setValue(draft(after, same)); assertEquals(1, commands.size()); assertFalse(apply(after, commands.getFirst()).properties().containsKey(p("groupValue")));
    }

    @Test void payloadCannotCrossFamiliesIdsPropertiesOrCarryExtraFields() throws Exception {
        var group = new WidgetNode(ID, DEF.typeId(), full(), prototype().slots()); var commands = new ArrayList<DesignerCommand>(); var groupNode = node(group, commands);
        var radio = new WidgetNode(ID, RadioPropertyContractTest.DEF.typeId(), RadioPropertyContractTest.full(), Map.of());
        var radioRequested = new LinkedHashMap<>(FlutterPropertyCellValue.RadioTypeEdit.snapshot(radio)); radioRequested.put("valueType", Optional.of(new PropertyValue.StringValue("Object")));
        var radioDraft = new FlutterPropertyCellValue(radioRequested.get("valueType"), Optional.of(new FlutterPropertyCellValue.RadioTypeEdit(ID, FlutterPropertyCellValue.RadioTypeEdit.snapshot(radio), radioRequested)));
        assertThrows(IllegalArgumentException.class, () -> cell(groupNode, "valueType").setValue(radioDraft));
        var requested = new LinkedHashMap<>(FlutterPropertyCellValue.RadioTypeEdit.snapshot(group)); requested.put("valueType", Optional.of(new PropertyValue.StringValue("Object"))); var groupDraft = draft(group, requested);
        var radioNode = new FlutterWidgetPropertiesNode(Children.LEAF, radio, RadioPropertyContractTest.DEF, commands::add);
        assertThrows(IllegalArgumentException.class, () -> cell(radioNode, "valueType").setValue(groupDraft));
        assertThrows(IllegalArgumentException.class, () -> cell(groupNode, "groupValue").setValue(groupDraft));
        assertThrows(IllegalArgumentException.class, () -> cell(groupNode, "onChanged").setValue(groupDraft));
        var otherId = new WidgetNode(StableId.random(), group.type(), group.properties(), group.slots());
        assertThrows(IllegalArgumentException.class, () -> cell(groupNode, "valueType").setValue(draft(otherId, requested)));
        var illegal = new LinkedHashMap<>(requested); illegal.put("onChanged", Optional.of(new PropertyValue.StringValue("noop")));
        assertThrows(IllegalArgumentException.class, () -> draft(group, illegal));
        assertThrows(IllegalArgumentException.class, () -> new FlutterPropertyCellValue.RadioTypeEdit(ID, FlutterPropertyCellValue.RadioTypeEdit.snapshot(group), requested));
        assertTrue(commands.isEmpty());
        var ordinary = FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("Object")); cell(groupNode, "valueType").setValue(ordinary); assertEquals(1, commands.size());
    }

    @Test void nullableIdentityAndRequiredCallbackRemainDistinctWithoutRawCode() throws Exception {
        var commands = new ArrayList<DesignerCommand>(); var initial = prototype(); var node = node(initial, commands);
        for (var empty : List.of(FlutterPropertyCellValue.unset(), FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()))) {
            assertThrows(IllegalArgumentException.class, () -> cell(node, "onChanged").setValue(empty)); assertTrue(commands.isEmpty());
        }
        for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            var properties = new LinkedHashMap<>(initial.properties()); properties.put(p("valueType"), new PropertyValue.StringValue("double"));
            var before = new WidgetNode(ID, DEF.typeId(), properties, initial.slots()); node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var identity = new PropertyValue.EnumValue("double", member); cell(node, "groupValue").setValue(FlutterPropertyCellValue.explicit(identity)); assertValid(apply(before, commands.getFirst()));
        }
        assertEquals(FlutterTypedPropertyEditors.EditorKind.RADIO_TYPE, binding("valueType").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.RADIO_VALUE, binding("groupValue").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BOOLEAN, binding("nullableValueType").editorKind());
        for (String name : List.of("valueType", "groupValue")) assertThrows(IllegalArgumentException.class, () -> binding(name).createEditor().setAsText("arbitrary()"));
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { var result = new LinkedHashMap<PropertyName, PropertyValue>(); for (var field : DEF.properties()) result.put(field.name(), value(field)); result.put(p("valueType"), new PropertyValue.StringValue("String")); return result; }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) { return switch (name) { case "groupValue" -> new PropertyValue.StringValue("first"); case "onChanged" -> reference("_onChanged"); case "valueType" -> new PropertyValue.StringValue("Object"); case "nullableValueType" -> new PropertyValue.BooleanValue(true); default -> throw new IllegalArgumentException(name); }; }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) { return Map.of(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return RadioPropertyContractTest.reference(name); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return RadioPropertyContractTest.apply(widget, command); }
    private static FlutterPropertyCellValue draft(WidgetNode before, Map<String, Optional<PropertyValue>> requested) { return new FlutterPropertyCellValue(requested.get("valueType"), Optional.of(new FlutterPropertyCellValue.RadioTypeEdit(before.type(), before.id(), FlutterPropertyCellValue.RadioTypeEdit.snapshot(before), requested))); }
    private static WidgetNode prototype() { var initial = WidgetNodePrototypeFactory.create(DEF, ID); var child = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Column")).orElseThrow(), StableId.random()); return new WidgetNode(ID, DEF.typeId(), initial.properties(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))); }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(DEF.property(p(name)).orElseThrow()).orElseThrow(); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("radio_group.dart", "GroupScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(result.valid(), result.toString()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
