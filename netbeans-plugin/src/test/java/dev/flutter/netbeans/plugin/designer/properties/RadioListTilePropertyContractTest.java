package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class RadioListTilePropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE).orElseThrow();
    private static final List<String> WHOLE = List.of("shape", "fillColor", "overlayColor", "radioBackgroundColor", "radioSide", "radioInnerRadius", "visualDensity", "mouseCursor");

    @Test void all153TypedRowsKeepIdentityAndResetOnlyExplicitOptionalValues() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(153, DEF.properties().size()); assertEquals(3, DEF.slots().size());
        assertEquals(Map.of(p("value"), new PropertyValue.StringValue("option"), p("valueType"), new PropertyValue.StringValue("String"),
                p("variant"), new PropertyValue.StringValue("standard"), p("onChanged"), new PropertyValue.StringValue("noop")), prototype.properties());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var properties = new LinkedHashMap<>(prototype.properties()); properties.putAll(sparsePrerequisites(name));
            var before = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots()); assertValid(before);
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var row = cell(node, name); var editor = row.getPropertyEditor(); var next = value(name);
            assertTrue(row.canWrite()); assertEquals(!field.parameter().required(), row.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(FlutterPropertyCellValue.explicit(next), editor.getValue(), name);
            commands.clear(); row.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(1, commands.size(), name);
            var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(next, after.properties().get(field.name())); assertEquals(before.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, cell(node, name)); assertEquals(editor.getClass(), row.getPropertyEditor().getClass()); assertArrayEquals(sets, node.getPropertySets());
            if (field.parameter().required()) {
                assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset())); row.restoreDefaultValue(); assertTrue(commands.isEmpty());
            } else { row.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); }
        }
        assertTrue(DEF.property(p("groupRegistry")).isEmpty()); assertTrue(DEF.property(p("focusColor")).isEmpty());
        assertEquals(2, WidgetEventCatalog.eventsFor(DEF).size()); assertTrue(WidgetEventCatalog.eventsFor(DEF).stream().allMatch(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT));
    }

    @Test void fiveFieldTypeTransactionIncludesCallbackButRejectsStaleIdentityAndRegistrySmuggling() throws Exception {
        var before = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var baseline = FlutterPropertyCellValue.RadioTypeEdit.snapshot(before);
        assertEquals(Set.of("valueType", "nullableValueType", "value", "groupValue", "onChanged"), baseline.keySet());
        assertThrows(IllegalArgumentException.class, () -> cell(node, "valueType").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("int")))); assertTrue(commands.isEmpty());
        var requested = new LinkedHashMap<>(baseline); requested.put("valueType", Optional.of(new PropertyValue.StringValue("int")));
        requested.put("value", Optional.of(new PropertyValue.IntegerValue(BigInteger.TWO))); requested.put("groupValue", Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE)));
        requested.put("nullableValueType", Optional.of(new PropertyValue.BooleanValue(false))); requested.put("onChanged", Optional.of(reference("_integerChanged")));
        var draft = new FlutterPropertyCellValue.RadioTypeEdit(before.type(), before.id(), baseline, requested);
        var accepted = new FlutterPropertyCellValue(requested.get("valueType"), Optional.of(draft)); cell(node, "valueType").setValue(accepted);
        assertEquals(1, commands.size()); var patch = assertInstanceOf(PatchProperties.class, commands.removeFirst()); assertEquals(5, patch.patches().size());
        var changed = apply(before, patch); assertValid(changed); assertEquals(before.slots(), changed.slots());
        assertEquals(requested, FlutterPropertyCellValue.RadioTypeEdit.snapshot(changed));
        node.refreshPresentation(changed, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(node, "valueType").setValue(accepted)).getMessage().contains("changed after the dialog")); assertTrue(commands.isEmpty());
        var smuggled = new LinkedHashMap<>(requested); smuggled.put("groupRegistry", Optional.of(reference("_registry")));
        assertThrows(IllegalArgumentException.class, () -> new FlutterPropertyCellValue.RadioTypeEdit(before.type(), before.id(), baseline, smuggled));
        assertEquals(4, FlutterPropertyCellValue.RadioTypeEdit.fields(RadioWidgetPropertySchema.RADIO_TYPE).size());
        assertEquals(3, FlutterPropertyCellValue.RadioTypeEdit.fields(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE).size());
    }

    @Test void bothConstructorsRetainInactiveCheckmarkAndAllShapeBranchesAndCallbacks() throws Exception {
        for (String variant : RadioListTileWidgetPropertySchema.variants()) for (String shape : RadioListTileWidgetPropertySchema.shapeKinds()) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(variant, shape), prototype.slots()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var flag = cell(node, "useCupertinoCheckmarkStyle");
            assertEquals(variant.equals("standard"), flag.getDisplayName().contains("inactive; retained"));
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(variant.equals("standard") ? "adaptive" : "standard")));
            var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(before.slots(), after.slots());
            var expected = new LinkedHashMap<>(before.properties()); expected.put(p("variant"), after.properties().get(p("variant"))); assertEquals(expected, after.properties());
            flag.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false))); var edited = apply(before, commands.removeFirst()); assertValid(edited);
            assertEquals(before.properties().get(p("variant")), edited.properties().get(p("variant")));
            flag.restoreDefaultValue(); var reset = apply(before, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(p("useCupertinoCheckmarkStyle")));
        }
    }

    @Test void mappedRadioAndTileCompoundFamiliesSwitchAtomicallyWithoutChangingPeersOrSlots() throws Exception {
        for (String family : WHOLE) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(), prototype.slots()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var locals = locals(family);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(value(family))); assertEquals(1, commands.size()); var whole = apply(before, commands.removeFirst()); assertValid(whole);
            locals.forEach(name -> assertFalse(whole.properties().containsKey(p(name)), name)); assertEquals(before.slots(), whole.slots());
            before.properties().forEach((name, value) -> { if (!locals.contains(name.value())) assertEquals(value, whole.properties().get(name)); });
            for (String leaf : locals) {
                var properties = new LinkedHashMap<>(whole.properties()); properties.putAll(sparsePrerequisites(leaf)); if (!sparsePrerequisites(leaf).isEmpty()) properties.remove(p(family));
                var sparse = new WidgetNode(before.id(), DEF.typeId(), properties, before.slots());
                node.refreshPresentation(sparse, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, leaf).setValue(FlutterPropertyCellValue.explicit(value(leaf))); assertEquals(1, commands.size(), leaf);
                var local = apply(sparse, commands.removeFirst()); assertValid(local); assertFalse(local.properties().containsKey(p(family))); assertEquals(before.slots(), local.slots());
            }
        }
    }

    @Test void optionalCallbackNullAndResetRemainIndependentOfEnabledAndSubtitleAndReadOnlyAuthority() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands);
        for (PropertyValue callback : List.of(new PropertyValue.NullValue(), reference("_changed"))) {
            cell(node, "onChanged").setValue(FlutterPropertyCellValue.explicit(callback)); var edited = apply(base, commands.removeFirst()); assertValid(edited);
            assertEquals(base.slots(), edited.slots()); assertEquals(base.properties().get(p("value")), edited.properties().get(p("value")));
        }
        cell(node, "onChanged").restoreDefaultValue(); var omitted = apply(base, commands.removeFirst()); assertValid(omitted); assertFalse(omitted.properties().containsKey(p("onChanged")));
        var empty = WidgetNodePrototypeFactory.create(DEF, base.id()); node.refreshPresentation(empty, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(node, "isThreeLine").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)))).getMessage().contains("Subtitle"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(node, "mouseCursorPressed").setValue(FlutterPropertyCellValue.explicit(value("mouseCursorPressed")))).getMessage().contains("Default"));
        var row = cell(node, "onChanged"); node.refreshPresentation(base, DEF, null, null, null, FlutterImageAssetChoices.empty()); assertSame(row, cell(node, "onChanged"));
        assertFalse(row.canWrite()); assertThrows(IllegalAccessException.class, row::restoreDefaultValue); assertTrue(commands.isEmpty());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full("standard", "roundedRectangle"); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shapeKind) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (WHOLE.contains(name)) continue;
            if (RadioListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name) && !RadioListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, shapeKind)) continue;
            result.put(field.name(), switch (name) {
                case "variant" -> new PropertyValue.StringValue(variant); case "valueType" -> new PropertyValue.StringValue("String");
                case "nullableValueType" -> new PropertyValue.BooleanValue(false); case "enabled" -> new PropertyValue.NullValue();
                case "shapeKind" -> new PropertyValue.StringValue(shapeKind); default -> value(name);
            });
        }
        return result;
    }
    public static PropertyValue value(String name) {
        if (name.equals("controlAffinity")) return new PropertyValue.EnumValue("ListTileControlAffinity", "leading");
        if (RadioListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) return CardPropertyContractTest.value(name);
        if (RadioListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) return new PropertyValue.StringValue("click");
        String source = RadioListTileWidgetPropertySchema.radioSourceName(name);
        if (RadioWidgetPropertySchema.find(source).isPresent()) return RadioPropertyContractTest.value(source);
        if (name.equals("radioScaleFactor")) return new PropertyValue.DoubleValue(java.math.BigDecimal.ONE);
        return CheckboxListTilePropertyContractTest.value(name);
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        return RadioListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name) && !name.equals("mouseCursorDefault")
                ? Map.of(p("mouseCursorDefault"), value("mouseCursorDefault")) : Map.of();
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return RadioPropertyContractTest.reference(name); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("title", "subtitle", "secondary")) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue(name)), Map.of())));
        return new WidgetNode(base.id(), DEF.typeId(), base.properties(), slots);
    }
    private static List<String> locals(String family) { return switch (family) {
        case "shape" -> RadioListTileWidgetPropertySchema.builtInShapePropertyNames(); case "radioSide" -> RadioListTileWidgetPropertySchema.sideLocalProperties();
        case "radioInnerRadius" -> RadioListTileWidgetPropertySchema.innerRadiusStateProperties(); case "visualDensity" -> List.of("visualDensityHorizontal", "visualDensityVertical");
        case "mouseCursor" -> RadioListTileWidgetPropertySchema.mouseCursorStateProperties(); default -> RadioListTileWidgetPropertySchema.colorStateProperties(family);
    }; }
    static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(property -> property.getName().equals(name)).findFirst().orElseThrow(); }
    static void assertValid(WidgetNode widget) { ListTilePropertyContractTest.assertValid(widget); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
