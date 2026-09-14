package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class MenuAnchorPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE).orElseThrow();

    @Test void all219RowsKeepTypedValuesStableIdentityIndependentResetsAndBothSlots() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands); var groups = node.getPropertySets();
        assertEquals(219, DEF.properties().size()); assertEquals(203, MenuAnchorWidgetPropertySchema.localStyleProperties().size());
        assertEquals(16, MenuAnchorWidgetPropertySchema.DIRECT_PROPERTY_COUNT); assertTrue(base.properties().isEmpty()); assertTrue(DEF.constConstructor());
        assertEquals(2, DEF.slots().size()); assertTrue(DEF.slots().stream().allMatch(slot -> slot.minChildren() == 0));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).isEmpty());
        for (var field : DEF.properties()) {
            String name = field.name().value(); node.refreshPresentation(base, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var row = cell(node, name); var editor = row.getPropertyEditor(); var value = value(name);
            assertTrue(row.canWrite(), name + " must be writable"); assertTrue(row.supportsDefaultValue(), name + " must support reset");
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue());
            row.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), name);
            var after = apply(base, commands.removeFirst()); assertValid(after); assertEquals(value, after.properties().get(field.name())); assertEquals(base.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, cell(node, name)); assertArrayEquals(groups, node.getPropertySets());
            row.restoreDefaultValue(); assertEquals(1, commands.size(), name); var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name()));
        }
    }

    @Test void wholeMenuStyleAndAll203LocalLeavesSwitchAtomicallyWithoutButtonOnlyFields() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var dense = with(base, full()); assertValid(dense); var node = node(dense, commands);
        for (PropertyValue whole : List.of(value("style"), new PropertyValue.NullValue())) {
            node.refreshPresentation(dense, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "style").setValue(FlutterPropertyCellValue.explicit(whole));
            var referenced = apply(dense, commands.removeFirst()); assertValid(referenced); assertEquals(whole, referenced.properties().get(p("style")));
            MenuAnchorWidgetPropertySchema.localStyleProperties().forEach(name -> assertFalse(referenced.properties().containsKey(p(name))));
            for (String name : MenuAnchorWidgetPropertySchema.localStyleProperties()) {
                node.refreshPresentation(referenced, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
                var local = apply(referenced, commands.removeFirst()); assertValid(local); assertEquals(value(name), local.properties().get(p(name))); assertFalse(local.properties().containsKey(p("style"))); assertEquals(base.slots(), local.slots());
            }
        }
        for (String invalid : List.of("enabled", "onPressed", "styleTextColor", "styleForegroundColor", "styleOverlayColor", "styleEnableFeedback", "styleBackgroundBuilder", "styleForegroundBuilder")) assertTrue(DEF.property(p(invalid)).isEmpty(), invalid);
    }

    @Test void threeNativeEventsOnePropertiesBuilderAndOnlyFourEffectiveBooleanConsumers() {
        var descriptors = WidgetEventCatalog.eventsFor(DEF); assertEquals(4, descriptors.size());
        assertEquals(Set.of("onOpen", "onClose", "onAnimationStatusChanged"), Set.copyOf(descriptors.stream().filter(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT).map(event -> event.propertyName().value()).toList()));
        var builder = descriptors.stream().filter(event -> event.propertyName().value().equals("builder")).findFirst().orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, builder.kind()); assertEquals("Widget", builder.signature().returnType());
        assertEquals(List.of("BuildContext", "MenuController", "Widget?"), builder.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::type).toList());
        assertEquals(Set.of("consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated"), Set.copyOf(WidgetStatePropertyBindingCatalog.descriptors(prototype()).stream().map(value -> value.propertyName().value()).toList()));
        assertTrue(WidgetStateBindingCatalog.find(prototype()).isEmpty());
        assertTrue(MenuAnchorWidgetPropertySchema.find("anchorTapClosesMenu").orElseThrow().description().contains("does not use it"));
        assertTrue(MenuAnchorWidgetPropertySchema.find("styleHoveredMouseCursor").orElseThrow().description().contains("empty states"));
        var node = node(prototype(), new ArrayList<>());
        var builderGroup = Arrays.stream(node.getPropertySets()).filter(group -> Arrays.stream(group.getProperties()).anyMatch(row -> row.getName().equals("builder"))).findFirst().orElseThrow();
        assertEquals("Properties", builderGroup.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
    }

    @Test void readOnlyRowsRejectMeaningfulResetsWithoutChangingTheChildOrMenuList() throws Exception {
        var base = with(prototype(), full()); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands); var rows = new HashMap<String, Node.Property<FlutterPropertyCellValue>>();
        for (String name : List.of("controller", "builder", "animated", "styleBackgroundColor")) rows.put(name, cell(node, name));
        node.refreshPresentation(base, DEF, null, null, null, FlutterImageAssetChoices.empty());
        for (var entry : rows.entrySet()) { assertSame(entry.getValue(), cell(node, entry.getKey())); assertFalse(entry.getValue().canWrite()); assertThrows(IllegalAccessException.class, entry.getValue()::restoreDefaultValue); }
        assertTrue(commands.isEmpty());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full(true, false); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean directional, boolean circles) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (name.equals("style")) continue;
            if (circles ? name.contains("ShapeRadius") : name.endsWith("ShapeCircleEccentricity")) continue;
            values.put(field.name(), circles && name.endsWith("ShapeKind") ? new PropertyValue.StringValue("circle") : value(name));
        }
        values.put(p("styleAlignmentKind"), new PropertyValue.StringValue(directional ? "directional" : "physical"));
        return values;
    }
    public static PropertyValue value(String name) {
        return switch (name) {
            case "alignmentOffset" -> new PropertyValue.OffsetValue(BigDecimal.ONE, BigDecimal.TWO);
            case "reservedPadding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO);
            case "clipBehavior" -> new PropertyValue.EnumValue("Clip", "hardEdge");
            default -> TextButtonPropertyContractTest.value(DEF.property(p(name)).orElseThrow());
        };
    }
    public static WidgetNode prototype() { return WidgetNodePrototypeFactory.create(DEF, StableId.random()); }
    static PropertyName p(String name) { return new PropertyName(name); }
    static WidgetNode with(WidgetNode base, Map<PropertyName, PropertyValue> values) { return new WidgetNode(base.id(), base.type(), values, base.slots(), base.extensions(), base.stateBinding(), base.propertyBindings()); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return TooltipPropertyContractTest.cell(node, name); }
    static WidgetNode apply(WidgetNode before, DesignerCommand command) { return with(before, TextButtonPropertyContractTest.apply(before.properties(), command)); }
    static void assertValid(WidgetNode widget) { TooltipPropertyContractTest.assertValid(widget); }
}
