package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class RadioPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(RadioWidgetPropertySchema.RADIO_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3d489fe-1a15-43be-8eb8-de129a15b8e1");
    private static final List<String> WHOLE = List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius", "side", "visualDensity");

    @Test void all107CellsRetainStableSetsTypedEditorsAndSparseEditResetCommands() throws Exception {
        var prototype = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(107, DEF.properties().size()); assertTrue(DEF.slots().isEmpty()); assertTrue(DEF.constConstructor());
        var expectedGroups = new LinkedHashSet<String>(); expectedGroups.add(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME);
        Arrays.stream(RadioWidgetPropertySchema.Group.values()).map(RadioWidgetPropertySchema.Group::setName).forEach(expectedGroups::add);
        expectedGroups.add(FlutterWidgetPropertiesNode.EVENTS_SET_NAME);
        assertEquals(expectedGroups, Arrays.stream(sets).map(Node.PropertySet::getName).collect(java.util.stream.Collectors.toSet()));
        assertEquals(expectedGroups.size(), sets.length);
        var eventRows = Arrays.stream(sets).filter(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(List.of("onChanged"), Arrays.stream(eventRows.getProperties()).map(Node.Property::getName).toList());
        assertEquals(Map.of(p("value"), new PropertyValue.StringValue("option"), p("valueType"), new PropertyValue.StringValue("String"),
                p("variant"), new PropertyValue.StringValue("standard"), p("onChanged"), new PropertyValue.StringValue("noop")), prototype.properties());
        for (var field : DEF.properties()) {
            var properties = new LinkedHashMap<>(prototype.properties()); properties.putAll(sparsePrerequisites(field.name().value()));
            var before = new WidgetNode(ID, DEF.typeId(), properties, Map.of()); assertValid(before);
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, field.name().value()); var editor = cell.getPropertyEditor(); var next = value(field);
            assertTrue(cell.canWrite()); assertEquals(!field.parameter().required(), cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(FlutterPropertyCellValue.explicit(next), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(1, commands.size(), field.name().value());
            var after = apply(before, commands.getFirst()); assertValid(after); assertEquals(next, after.properties().get(field.name()));
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, field.name().value())); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear();
            if (!field.parameter().required()) { cell.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(after, commands.getFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); }
            else { assertThrows(IllegalArgumentException.class, () -> cell.setValue(FlutterPropertyCellValue.unset())); cell.restoreDefaultValue(); assertTrue(commands.isEmpty()); }
        }
    }

    @Test void genericTypeTransactionsPreservePeersRejectStaleOrSmuggledDraftsAndAllowSameTypePeerEdits() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        assertThrows(IllegalArgumentException.class, () -> cell(node, "valueType").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("int"))));
        assertTrue(commands.isEmpty());
        var requested = new LinkedHashMap<>(FlutterPropertyCellValue.RadioTypeEdit.snapshot(before));
        requested.put("valueType", Optional.of(new PropertyValue.StringValue("int")));
        requested.put("value", Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(2))));
        requested.put("groupValue", Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(1))));
        var draft = new FlutterPropertyCellValue(requested.get("valueType"), Optional.of(new FlutterPropertyCellValue.RadioTypeEdit(ID, FlutterPropertyCellValue.RadioTypeEdit.snapshot(before), requested)));
        cell(node, "valueType").setValue(draft); assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst());
        var after = apply(before, commands.getFirst()); assertValid(after);
        for (var entry : before.properties().entrySet()) if (!FlutterPropertyCellValue.RadioTypeEdit.FIELDS.contains(entry.getKey().value())) assertEquals(entry.getValue(), after.properties().get(entry.getKey()));
        commands.clear(); node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertThrows(IllegalArgumentException.class, () -> cell(node, "valueType").setValue(draft)); assertTrue(commands.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> cell(node, "groupValue").setValue(draft)); assertTrue(commands.isEmpty());
        var sameType = new LinkedHashMap<>(FlutterPropertyCellValue.RadioTypeEdit.snapshot(after)); sameType.put("value", Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3))));
        cell(node, "valueType").setValue(new FlutterPropertyCellValue(sameType.get("valueType"), Optional.of(new FlutterPropertyCellValue.RadioTypeEdit(ID, FlutterPropertyCellValue.RadioTypeEdit.snapshot(after), sameType))));
        assertEquals(1, commands.size()); assertEquals(new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3)), apply(after, commands.getFirst()).properties().get(p("value")));
        var illegal = new LinkedHashMap<>(requested); illegal.put("enabled", Optional.of(new PropertyValue.BooleanValue(false)));
        assertThrows(IllegalArgumentException.class, () -> new FlutterPropertyCellValue.RadioTypeEdit(ID, FlutterPropertyCellValue.RadioTypeEdit.snapshot(before), illegal));
    }

    @Test void adaptiveFlagSwitchesOnlyItsConstructorBranchAndNullableEnabledDoesNotEraseCallbacks() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("standard")));
        var standard = apply(before, commands.getFirst()); assertValid(standard); assertFalse(standard.properties().containsKey(p("useCupertinoCheckmarkStyle")));
        for (var entry : before.properties().entrySet()) if (!List.of("variant", "useCupertinoCheckmarkStyle").contains(entry.getKey().value())) assertEquals(entry.getValue(), standard.properties().get(entry.getKey()));
        node.refreshPresentation(standard, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
        cell(node, "useCupertinoCheckmarkStyle").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
        var adaptive = apply(standard, commands.getFirst()); assertValid(adaptive); assertEquals(new PropertyValue.StringValue("adaptive"), adaptive.properties().get(p("variant")));
        var withoutEnabled = new LinkedHashMap<>(standard.properties()); withoutEnabled.remove(p("enabled"));
        standard = new WidgetNode(ID, DEF.typeId(), withoutEnabled, Map.of());
        for (PropertyValue enabled : List.of(new PropertyValue.BooleanValue(true), new PropertyValue.BooleanValue(false), new PropertyValue.NullValue())) {
            node.refreshPresentation(standard, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, "enabled").setValue(FlutterPropertyCellValue.explicit(enabled)); var edited = apply(standard, commands.getFirst()); assertValid(edited);
            assertEquals(before.properties().get(p("onChanged")), edited.properties().get(p("onChanged"))); assertEquals(before.properties().get(p("groupRegistry")), edited.properties().get(p("groupRegistry")));
        }
    }

    @Test void allSixWholeLocalFamiliesSwitchAtomicallyWithoutChangingPeers() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of());
        for (String family : WHOLE) {
            List<String> locals = family.equals("innerRadius") ? RadioWidgetPropertySchema.innerRadiusStateProperties()
                    : family.equals("side") ? RadioWidgetPropertySchema.sideLocalProperties()
                    : family.equals("visualDensity") ? List.of("visualDensityHorizontal", "visualDensityVertical") : RadioWidgetPropertySchema.colorStateProperties(family);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(reference("_" + family))); var whole = apply(before, commands.getFirst()); assertValid(whole);
            for (String local : locals) assertFalse(whole.properties().containsKey(p(local)));
            for (var entry : before.properties().entrySet()) if (!locals.contains(entry.getKey().value())) assertEquals(entry.getValue(), whole.properties().get(entry.getKey()));
            for (String local : locals) {
                node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, local).setValue(FlutterPropertyCellValue.explicit(value(local))); var edited = apply(whole, commands.getFirst()); assertValid(edited);
                assertFalse(edited.properties().containsKey(p(family))); assertEquals(value(local), edited.properties().get(p(local)));
            }
        }
    }

    @Test void sideInheritanceAndPlainResetsKeepBaseAndOtherStateBuckets() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of());
        for (String state : RadioWidgetPropertySchema.sideStates()) {
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); String mode = "side" + state + "Mode";
            cell(node, mode).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("inherit")));
            var inherited = apply(before, commands.getFirst()); assertValid(inherited);
            for (String detail : RadioWidgetPropertySchema.sideBucketProperties(state).subList(1, 5)) {
                assertFalse(inherited.properties().containsKey(p(detail))); node.refreshPresentation(inherited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, detail).setValue(FlutterPropertyCellValue.explicit(value(detail))); var restored = apply(inherited, commands.getFirst()); assertValid(restored);
                assertEquals(new PropertyValue.StringValue("border"), restored.properties().get(p(mode)));
                for (String base : RadioWidgetPropertySchema.sideBaseProperties()) assertEquals(before.properties().get(p(base)), restored.properties().get(p(base)));
            }
        }
        for (boolean reset : List.of(false, true)) {
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            if (reset) cell(node, "sideStateful").restoreDefaultValue(); else cell(node, "sideStateful").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
            var plain = apply(before, commands.getFirst()); assertValid(plain);
            RadioWidgetPropertySchema.sideStateProperties().forEach(field -> assertFalse(plain.properties().containsKey(p(field))));
            RadioWidgetPropertySchema.sideBaseProperties().forEach(field -> assertEquals(before.properties().get(p(field)), plain.properties().get(p(field))));
        }
    }

    @Test void specialDoubleIdentitiesRespectSelectedTypeWithoutCoercionOrPeerDeletion() throws Exception {
        for (String type : List.of("double", "num", "Object", "String", "int", "bool")) for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            var values = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(DEF, ID).properties());
            values.put(p("valueType"), new PropertyValue.StringValue(type));
            values.put(p("value"), type.equals("String") ? new PropertyValue.StringValue("saved") : type.equals("bool") ? new PropertyValue.BooleanValue(false) : new PropertyValue.IntegerValue(java.math.BigInteger.ZERO));
            var before = new WidgetNode(ID, DEF.typeId(), values, Map.of()); assertValid(before);
            for (String field : List.of("value", "groupValue")) {
                var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
                var special = new PropertyValue.EnumValue("double", member);
                if (List.of("String", "int", "bool").contains(type)) { assertThrows(IllegalArgumentException.class, () -> cell(node, field).setValue(FlutterPropertyCellValue.explicit(special))); assertTrue(commands.isEmpty()); }
                else { cell(node, field).setValue(FlutterPropertyCellValue.explicit(special)); assertEquals(1, commands.size()); var after = apply(before, commands.getFirst()); assertValid(after); assertEquals(special, after.properties().get(p(field))); }
            }
        }
    }

    @Test void nullableAndInfinityKindsRemainExactAndNoRawTypeExpressionsAreAccepted() {
        assertEquals(FlutterTypedPropertyEditors.EditorKind.RADIO_TYPE, binding("valueType").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.RADIO_VALUE, binding("value").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN, binding("enabled").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE, binding("groupRegistry").editorKind());
        for (String name : List.of("splashRadius", "innerRadiusDefault", "innerRadiusPressed")) for (String member : List.of("infinity", "negativeInfinity"))
            binding(name).validate(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", member)));
        for (String name : RadioWidgetPropertySchema.innerRadiusStateProperties()) binding(name).validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        for (String name : List.of("valueType", "value", "groupValue", "groupRegistry", "focusNode")) assertThrows(IllegalArgumentException.class, () -> binding(name).createEditor().setAsText("arbitrary()"));
        assertEquals(FlutterTypedPropertyEditors.EditorKind.OBJECT_TAG, FlutterTypedPropertyEditors.binding(FloatingActionButtonPropertyContractTest.DEF.property(p("heroTag")).orElseThrow()).orElseThrow().editorKind());
        assertThrows(IllegalArgumentException.class, () -> binding("sideWidth").createEditor().setAsText("-1"));
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) if (!WHOLE.contains(field.name().value())) result.put(field.name(), value(field));
        result.put(p("valueType"), new PropertyValue.StringValue("String"));
        result.put(p("enabled"), new PropertyValue.NullValue());
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) { return Map.of(); }
    public static PropertyDefinition field(String name) { return DEF.property(p(name)).orElseThrow(); }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        if (name.equals("variant")) return new PropertyValue.StringValue("adaptive");
        if (name.equals("valueType")) return new PropertyValue.StringValue("Object");
        if (name.equals("value")) return new PropertyValue.StringValue("second");
        if (name.equals("groupValue")) return new PropertyValue.StringValue("first");
        if (name.equals("enabled")) return new PropertyValue.BooleanValue(false);
        if (List.of("toggleable", "autofocus", "useCupertinoCheckmarkStyle", "nullableValueType", "sideStateful").contains(name)) return new PropertyValue.BooleanValue(true);
        if (List.of("onChanged", "mouseCursor", "focusNode", "groupRegistry").contains(name) || WHOLE.contains(name)) return reference("_" + name);
        if (name.endsWith("Mode")) return new PropertyValue.StringValue("border");
        if (name.endsWith("Style")) return new PropertyValue.EnumValue("BorderStyle", "solid");
        if (name.endsWith("Color") || List.of("fillColor", "overlayColor", "backgroundColor").stream().anyMatch(name::startsWith)) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        if (name.equals("materialTapTargetSize")) return new PropertyValue.EnumValue("MaterialTapTargetSize", "shrinkWrap");
        return new PropertyValue.DoubleValue(new BigDecimal(name.endsWith("StrokeAlign") ? "-2.5" : "2.5"));
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(field(name)).orElseThrow(); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("radio.dart", "RadioScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(result.valid(), result.toString()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
