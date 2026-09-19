package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class ExpansionTilePropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE).orElseThrow();
    private static final List<String> WHOLE = List.of("shape", "collapsedShape", "visualDensity", "expansionAnimationStyle");

    @Test void all76RowsHaveExactTypedEditorsStableIdentityAndIndependentOptionalResets() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var groups = node.getPropertySets();
        assertEquals(76, DEF.properties().size()); assertEquals(5, DEF.slots().size()); assertTrue(prototype.properties().isEmpty());
        assertEquals("title", WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).orElseThrow().name().value()); assertTrue(DEF.constConstructor());
        var eventRows = Arrays.stream(groups).filter(group -> group.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(List.of("onExpansionChanged"), Arrays.stream(eventRows.getProperties()).map(Node.Property::getName).toList());
        assertTrue(WidgetStateBindingCatalog.find(prototype).isEmpty(), "Initial seed is not a controlled State producer.");
        for (var field : DEF.properties()) {
            node.refreshPresentation(prototype, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var row = cell(node, field.name().value()); var editor = row.getPropertyEditor(); var value = value(field.name().value());
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue()); editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue());
            row.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), field.name().value());
            var after = apply(prototype, commands.removeFirst()); assertValid(after); assertEquals(value, after.properties().get(field.name())); assertEquals(prototype.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, cell(node, field.name().value())); assertArrayEquals(groups, node.getPropertySets());
            assertEquals(editor.getClass(), row.getPropertyEditor().getClass()); row.restoreDefaultValue(); assertEquals(1, commands.size());
            var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); assertEquals(prototype.slots(), reset.slots());
        }
        assertEquals(1, WidgetEventCatalog.eventsFor(DEF).size()); assertEquals(WidgetEventDescriptor.Kind.EVENT, WidgetEventCatalog.eventsFor(DEF).getFirst().kind());
    }

    @Test void bothIndependentShapeFamiliesSupportAllTenKindsAndWholeLocalAtomicTransitions() throws Exception {
        for (String family : ExpansionTileWidgetPropertySchema.shapeFamilies()) for (String kind : ExpansionTileWidgetPropertySchema.shapeKinds()) {
            var base = prototype(); var before = new WidgetNode(base.id(), base.type(), full(), base.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, family + "Kind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(kind)));
            var after = commands.isEmpty() ? before : apply(before, commands.removeFirst()); assertValid(after);
            assertEquals(base.slots(), after.slots()); before.properties().forEach((name, value) -> { if (!ExpansionTileWidgetPropertySchema.shapeLocalProperties(family).contains(name.value())) assertEquals(value, after.properties().get(name)); });
            for (PropertyValue whole : List.of(reference("_" + family), new PropertyValue.NullValue())) {
                cell(node, family).setValue(FlutterPropertyCellValue.explicit(whole)); var replaced = apply(before, commands.removeFirst()); assertValid(replaced);
                ExpansionTileWidgetPropertySchema.shapeLocalProperties(family).forEach(name -> assertFalse(replaced.properties().containsKey(p(name))));
                assertEquals(whole, replaced.properties().get(p(family))); assertEquals(base.slots(), replaced.slots());
            }
        }
    }

    @Test void fourWholeLocalFamiliesPreserveNullLeavesAndOtherFamiliesAndAllSlots() throws Exception {
        for (String family : WHOLE) {
            var base = prototype(); var before = new WidgetNode(base.id(), base.type(), full(), base.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            var locals = locals(family);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); var whole = apply(before, commands.removeFirst()); assertValid(whole);
            locals.forEach(name -> assertFalse(whole.properties().containsKey(p(name))));
            before.properties().forEach((name, value) -> { if (!locals.contains(name.value())) assertEquals(value, whole.properties().get(name)); });
            for (String name : locals) {
                node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name))); var local = apply(whole, commands.removeFirst()); assertValid(local);
                assertFalse(local.properties().containsKey(p(family))); assertEquals(before.slots(), local.slots());
                if (DEF.property(p(name)).orElseThrow().acceptedKinds().contains(PropertyValueKind.NULL)) {
                    node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
                    cell(node, name).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); var nullLeaf = apply(whole, commands.removeFirst()); assertValid(nullLeaf);
                    assertFalse(nullLeaf.properties().containsKey(p(family))); assertEquals(new PropertyValue.NullValue(), nullLeaf.properties().get(p(name)));
                }
            }
        }
    }

    @Test void callbackAndInitialSeedDoNotInventControlledStateOrClearChildrenAndStaleRowsAreReadOnly() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands);
        for (PropertyValue callback : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("noop"), reference("_onExpansionChanged"))) {
            cell(node, "onExpansionChanged").setValue(FlutterPropertyCellValue.explicit(callback)); var changed = apply(base, commands.removeFirst()); assertValid(changed);
            assertEquals(base.slots(), changed.slots()); assertTrue(changed.stateBinding().isEmpty());
        }
        var seed = cell(node, "initiallyExpanded"); seed.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true))); var seeded = apply(base, commands.removeFirst());
        assertTrue(seed.getShortDescription().contains("seed")); assertTrue(seeded.stateBinding().isEmpty()); assertEquals(base.slots(), seeded.slots());
        assertThrows(IllegalArgumentException.class, () -> cell(node, "expandedCrossAxisAlignment").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("CrossAxisAlignment", "baseline"))));
        assertTrue(commands.isEmpty()); var row = cell(node, "onExpansionChanged");
        var callback = reference("_onExpansionChanged"); row.setValue(FlutterPropertyCellValue.explicit(callback)); var configured = apply(base, commands.removeFirst());
        node.refreshPresentation(configured, DEF, null, null, null, FlutterImageAssetChoices.empty());
        assertSame(row, cell(node, "onExpansionChanged")); assertEquals(FlutterPropertyCellValue.explicit(callback), row.getValue());
        assertFalse(row.canWrite()); assertThrows(IllegalAccessException.class, row::restoreDefaultValue); assertTrue(commands.isEmpty());
        assertEquals(FlutterPropertyCellValue.explicit(callback), row.getValue()); assertEquals(base.slots(), configured.slots());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (WHOLE.contains(name)) continue;
            if (ExpansionTileWidgetPropertySchema.shapeFamily(name).isPresent() && !ExpansionTileWidgetPropertySchema.shapePropertyAppliesToKind(name, "roundedRectangle")) continue;
            result.put(field.name(), value(name));
        }
        return result;
    }
    public static PropertyValue value(String name) {
        var shape = ExpansionTileWidgetPropertySchema.shapeSourceName(name); if (shape.isPresent()) return CardPropertyContractTest.value(shape.orElseThrow());
        if (ExpansionTileWidgetPropertySchema.colorProperties().contains(name)) return new PropertyValue.ColorValue(0xff336699L);
        return switch (name) {
            case "onExpansionChanged", "controller", "statesController", "shape", "collapsedShape", "visualDensity", "expansionAnimationStyle" -> reference("_" + name);
            case "showTrailingIcon", "initiallyExpanded", "maintainState", "dense", "enableFeedback", "enabled", "internalAddSemanticForOnTap" -> new PropertyValue.BooleanValue(true);
            case "tilePadding", "childrenPadding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            case "expandedAlignment" -> new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL, new BigDecimal("0.25"), new BigDecimal("-0.5"));
            case "expandedCrossAxisAlignment" -> new PropertyValue.EnumValue("CrossAxisAlignment", "center");
            case "controlAffinity" -> new PropertyValue.EnumValue("ListTileControlAffinity", "leading");
            case "clipBehavior" -> new PropertyValue.EnumValue("Clip", "antiAlias");
            case "minTileHeight" -> new PropertyValue.DoubleValue(BigDecimal.valueOf(48));
            case "visualDensityHorizontal", "visualDensityVertical" -> new PropertyValue.DoubleValue(BigDecimal.ONE);
            case "expansionAnimationStyleDurationUs" -> new PropertyValue.IntegerValue(BigInteger.valueOf(250000));
            case "expansionAnimationStyleReverseDurationUs" -> new PropertyValue.IntegerValue(BigInteger.valueOf(125000));
            case "expansionAnimationStyleCurve" -> new PropertyValue.StringValue("easeIn");
            case "expansionAnimationStyleReverseCurve" -> new PropertyValue.StringValue("easeOut");
            default -> throw new IllegalArgumentException("Unreviewed fixture property " + name);
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return RadioPropertyContractTest.reference(name); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("title", "leading", "subtitle", "trailing")) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(text(name)));
        slots.put(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text("first"), text("second"))));
        return new WidgetNode(base.id(), base.type(), base.properties(), slots);
    }
    private static WidgetNode text(String value) { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of()); }
    private static List<String> locals(String family) { return switch (family) { case "shape", "collapsedShape" -> ExpansionTileWidgetPropertySchema.shapeLocalProperties(family); case "visualDensity" -> List.of("visualDensityHorizontal", "visualDensityVertical"); default -> ExpansionTileWidgetPropertySchema.animationStyleLocalProperties(); }; }
    static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return RadioPropertyContractTest.apply(widget, command); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(property -> property.getName().equals(name)).findFirst().orElseThrow(); }
    static void assertValid(WidgetNode widget) { ListTilePropertyContractTest.assertValid(widget); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
