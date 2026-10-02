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

public class TooltipPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(TooltipWidgetPropertySchema.TOOLTIP_TYPE).orElseThrow();

    @Test void all53RowsHaveExactEditorsStableIdentityAndReviewedResetsWithoutInventedContent() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var groups = node.getPropertySets();
        assertEquals(53, DEF.properties().size()); assertEquals(31, TooltipWidgetPropertySchema.textStyleProperties().size()); assertEquals(1, DEF.slots().size());
        assertEquals(Map.of(p("message"), new PropertyValue.StringValue("Tooltip")), prototype.properties()); assertTrue(DEF.constConstructor());
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).isEmpty());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var properties = new LinkedHashMap<>(prototype.properties());
            if (name.equals("textStylePackage")) properties.put(p("textStyleFontFamily"), value("textStyleFontFamily"));
            var before = new WidgetNode(prototype.id(), prototype.type(), properties, prototype.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var row = cell(node, name); var editor = row.getPropertyEditor(); var value = value(name); assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue());
            row.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), name); var after = apply(before, commands.removeFirst()); assertValid(after);
            assertEquals(value, after.properties().get(field.name())); assertEquals(before.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, cell(node, name)); assertArrayEquals(groups, node.getPropertySets());
            if (name.equals("message") || name.equals("richMessage")) {
                assertThrows(IllegalArgumentException.class, row::restoreDefaultValue, "Reset cannot remove the final non-null content."); assertTrue(commands.isEmpty());
            } else {
                row.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); assertEquals(before.slots(), reset.slots());
            }
        }
    }

    @Test void messageAndRichMessageSwitchAtomicallyAndNullDoesNotCreateFallbackText() throws Exception {
        var plain = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(plain, commands); var plainRow = cell(node, "message"); var richRow = cell(node, "richMessage");
        richRow.setValue(FlutterPropertyCellValue.explicit(value("richMessage"))); assertInstanceOf(PatchProperties.class, commands.getFirst()); var rich = apply(plain, commands.removeFirst()); assertValid(rich);
        assertFalse(rich.properties().containsKey(p("message"))); assertEquals(plain.slots(), rich.slots());
        node.refreshPresentation(rich, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(plainRow, cell(node, "message")); assertSame(richRow, cell(node, "richMessage"));
        assertThrows(IllegalArgumentException.class, richRow::restoreDefaultValue); assertThrows(IllegalArgumentException.class, () -> richRow.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()))); assertTrue(commands.isEmpty());
        plainRow.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); var inactiveNull = apply(rich, commands.removeFirst()); assertValid(inactiveNull); assertEquals(new PropertyValue.NullValue(), inactiveNull.properties().get(p("message")));
        plainRow.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(""))); var empty = apply(rich, commands.removeFirst()); assertValid(empty);
        assertEquals(new PropertyValue.StringValue(""), empty.properties().get(p("message"))); assertFalse(empty.properties().containsKey(p("richMessage"))); assertEquals(plain.slots(), empty.slots());
        node.refreshPresentation(empty, DEF, null, null, null, FlutterImageAssetChoices.empty()); assertFalse(plainRow.canWrite()); assertThrows(IllegalAccessException.class, plainRow::restoreDefaultValue); assertTrue(commands.isEmpty());
    }

    @Test void heightConstraintsAndWholeLocalStyleSwitchIndependentlyAndPreserveEveryOtherValue() throws Exception {
        var base = prototype(); var before = new WidgetNode(base.id(), base.type(), full(), base.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "height").setValue(FlutterPropertyCellValue.explicit(value("height"))); var height = apply(before, commands.removeFirst()); assertValid(height);
        assertFalse(height.properties().containsKey(p("constraints"))); assertEquals(before.slots(), height.slots());
        node.refreshPresentation(height, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        cell(node, "constraints").setValue(FlutterPropertyCellValue.explicit(value("constraints"))); var constrained = apply(height, commands.removeFirst()); assertValid(constrained); assertFalse(constrained.properties().containsKey(p("height")));
        for (PropertyValue wholeValue : List.of(value("textStyle"), new PropertyValue.NullValue())) {
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "textStyle").setValue(FlutterPropertyCellValue.explicit(wholeValue));
            var whole = apply(before, commands.removeFirst()); assertValid(whole); TooltipWidgetPropertySchema.textStyleProperties().forEach(name -> assertFalse(whole.properties().containsKey(p(name))));
            before.properties().forEach((name, value) -> { if (!TooltipWidgetPropertySchema.isTextStyleProperty(name)) assertEquals(value, whole.properties().get(name)); });
            node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "textStyleColor").setValue(FlutterPropertyCellValue.explicit(value("textStyleColor")));
            var local = apply(whole, commands.removeFirst()); assertValid(local); assertFalse(local.properties().containsKey(p("textStyle"))); assertEquals(before.slots(), local.slots());
        }
        node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "textStyleForeground").setValue(FlutterPropertyCellValue.explicit(value("textStyleForeground")));
        var paint = apply(before, commands.removeFirst()); assertValid(paint); assertFalse(paint.properties().containsKey(p("textStyleColor"))); assertEquals(before.properties().get(p("decoration")), paint.properties().get(p("decoration")));
    }

    @Test void onlyTriggeredIsAnEventWhilePositionDelegateRemainsAPropertyAndNoStateProducerExists() {
        var node = node(prototype(), new ArrayList<>()); var eventSet = Arrays.stream(node.getPropertySets()).filter(group -> group.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)).findFirst().orElseThrow();
        assertEquals(List.of("onTriggered"), Arrays.stream(eventSet.getProperties()).map(Node.Property::getName).toList());
        var callables = WidgetEventCatalog.eventsFor(DEF); assertEquals(2, callables.size());
        assertEquals(WidgetEventDescriptor.Kind.DELEGATE, callables.stream().filter(event -> event.propertyName().value().equals("positionDelegate")).findFirst().orElseThrow().kind());
        assertNotNull(cell(node, "positionDelegate").getPropertyEditor()); assertTrue(WidgetStateBindingCatalog.find(prototype()).isEmpty());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (List.of("richMessage", "height", "textStyle", "textStyleForeground", "textStyleBackground").contains(name)) continue;
            values.put(field.name(), value(name));
        }
        return values;
    }
    public static PropertyValue value(String name) {
        if (TooltipWidgetPropertySchema.isTextStyleProperty(p(name))) return BadgePropertyContractTest.value(name);
        return switch (name) {
            case "message" -> new PropertyValue.StringValue("Tooltip content");
            case "richMessage", "textStyle", "onTriggered", "positionDelegate" -> reference("_" + name);
            case "height" -> new PropertyValue.DoubleValue(BigDecimal.valueOf(32));
            case "verticalOffset" -> new PropertyValue.DoubleValue(BigDecimal.valueOf(24));
            case "constraints" -> new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.of(BigDecimal.valueOf(300)), BigDecimal.ZERO, Optional.of(BigDecimal.valueOf(200)));
            case "padding", "margin" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            case "preferBelow", "excludeFromSemantics", "enableTapToDismiss", "enableFeedback", "ignorePointer" -> new PropertyValue.BooleanValue(true);
            case "decoration" -> new PropertyValue.BoxDecorationValue(Optional.of(new ColorSource.Literal(0xff223344L)), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
            case "textAlign" -> new PropertyValue.EnumValue("TextAlign", "center");
            case "triggerMode" -> new PropertyValue.EnumValue("TooltipTriggerMode", "longPress");
            case "mouseCursor" -> new PropertyValue.StringValue("click");
            case "waitDurationUs" -> new PropertyValue.IntegerValue(BigInteger.valueOf(250000));
            case "showDurationUs" -> new PropertyValue.IntegerValue(BigInteger.valueOf(1500000));
            case "exitDurationUs" -> new PropertyValue.IntegerValue(BigInteger.valueOf(100000));
            default -> throw new IllegalArgumentException("Unreviewed Tooltip fixture field: " + name);
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return RadioPropertyContractTest.reference(name); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue("Keep my anchor")), Map.of());
        return new WidgetNode(base.id(), base.type(), base.properties(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(property -> property.getName().equals(name)).findFirst().orElseThrow(); }
    static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return RadioPropertyContractTest.apply(widget, command); }
    static void assertValid(WidgetNode widget) { ListTilePropertyContractTest.assertValid(widget); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
