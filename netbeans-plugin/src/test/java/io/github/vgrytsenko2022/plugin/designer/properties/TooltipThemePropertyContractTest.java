package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.WidgetStateBindingCatalog;
import io.github.vgrytsenko2022.designer.state.WidgetStatePropertyBindingCatalog;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class TooltipThemePropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE).orElseThrow();
    private static final PropertyName DATA = new PropertyName("data");
    private static final SlotName CHILD = new SlotName("child");

    @Test void all47RowsHaveTypedEditorsStableIdentityAndIndependentResets() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands); var groups = node.getPropertySets();
        assertEquals(47, DEF.properties().size()); assertEquals(46, TooltipThemeWidgetPropertySchema.localProperties().size());
        assertEquals(31, TooltipThemeWidgetPropertySchema.textStyleProperties().size()); assertTrue(base.properties().isEmpty()); assertTrue(DEF.constConstructor());
        assertEquals(CHILD, WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).orElseThrow().name());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var values = new LinkedHashMap<PropertyName, PropertyValue>();
            if (name.equals("textStylePackage")) values.put(p("textStyleFontFamily"), value("textStyleFontFamily"));
            var before = new WidgetNode(base.id(), base.type(), values, base.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var row = cell(node, name); var editor = row.getPropertyEditor(); var value = value(name);
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue());
            row.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), name);
            var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(value, after.properties().get(field.name())); assertEquals(before.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, cell(node, name)); assertArrayEquals(groups, node.getPropertySets());
            row.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(after, commands.removeFirst()); assertValid(reset);
            assertFalse(reset.properties().containsKey(field.name())); assertEquals(before.slots(), reset.slots());
        }
    }

    @Test void wholeDataConflictsNeverEraseLocalNullsOrStateBindingsAndLocalEditsNeverEraseData() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands);
        var dataRow = cell(node, "data"); var below = cell(node, "preferBelow");
        for (PropertyValue local : List.of(new PropertyValue.BooleanValue(false), new PropertyValue.NullValue())) {
            var before = new WidgetNode(base.id(), base.type(), Map.of(p("preferBelow"), local), base.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertThrows(IllegalArgumentException.class, () -> dataRow.setValue(FlutterPropertyCellValue.explicit(value("data")))); assertTrue(commands.isEmpty());
            assertEquals(FlutterPropertyCellValue.explicit(local), below.getValue());
        }
        var state = new StatePropertyBinding("_below", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        var stateOnly = new WidgetNode(base.id(), base.type(), Map.of(), base.slots(), base.extensions(), Optional.empty(), Map.of(p("preferBelow"), state));
        node.refreshPresentation(stateOnly, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertThrows(IllegalArgumentException.class, () -> dataRow.setValue(FlutterPropertyCellValue.explicit(value("data")))); assertTrue(commands.isEmpty());
        var whole = new WidgetNode(base.id(), base.type(), Map.of(DATA, value("data")), base.slots());
        node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        for (var local : List.of(new PropertyValue.BooleanValue(false), new PropertyValue.NullValue())) {
            assertThrows(IllegalArgumentException.class, () -> below.setValue(FlutterPropertyCellValue.explicit(local))); assertTrue(commands.isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> dataRow.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        dataRow.restoreDefaultValue(); var empty = apply(whole, commands.removeFirst()); assertValid(empty); assertTrue(empty.properties().isEmpty());
        node.refreshPresentation(empty, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        below.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false))); assertValid(apply(empty, commands.removeFirst()));
        node.refreshPresentation(whole, DEF, null, null, null, FlutterImageAssetChoices.empty()); assertFalse(dataRow.canWrite());
        assertThrows(IllegalAccessException.class, dataRow::restoreDefaultValue); assertTrue(commands.isEmpty());
    }

    @Test void innerHeightConstraintsAndStyleCompoundsRemainAtomicWithoutDroppingOtherData() throws Exception {
        var base = prototype(); var before = new WidgetNode(base.id(), base.type(), full(), base.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "height").setValue(FlutterPropertyCellValue.explicit(value("height"))); var height = apply(before, commands.removeFirst()); assertValid(height);
        assertFalse(height.properties().containsKey(p("constraints"))); assertEquals(before.slots(), height.slots());
        node.refreshPresentation(height, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        cell(node, "constraints").setValue(FlutterPropertyCellValue.explicit(value("constraints"))); var constrained = apply(height, commands.removeFirst()); assertValid(constrained); assertFalse(constrained.properties().containsKey(p("height")));
        for (PropertyValue wholeValue : List.of(value("textStyle"), new PropertyValue.NullValue())) {
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "textStyle").setValue(FlutterPropertyCellValue.explicit(wholeValue));
            var whole = apply(before, commands.removeFirst()); assertValid(whole); TooltipThemeWidgetPropertySchema.textStyleProperties().forEach(name -> assertFalse(whole.properties().containsKey(p(name))));
            assertEquals(before.properties().get(p("exitDurationUs")), whole.properties().get(p("exitDurationUs")));
            node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "textStyleColor").setValue(FlutterPropertyCellValue.explicit(value("textStyleColor")));
            var local = apply(whole, commands.removeFirst()); assertValid(local); assertFalse(local.properties().containsKey(p("textStyle"))); assertEquals(before.slots(), local.slots());
        }
        node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "textStyleForeground").setValue(FlutterPropertyCellValue.explicit(value("textStyleForeground")));
        var paint = apply(before, commands.removeFirst()); assertValid(paint); assertFalse(paint.properties().containsKey(p("textStyleColor"))); assertEquals(before.properties().get(p("decoration")), paint.properties().get(p("decoration")));
    }

    @Test void onlyThreeNullableStateConsumersExistWithoutNativeEventsOrAStateProducer() {
        var widget = prototype(); assertTrue(WidgetEventCatalog.eventsFor(DEF).isEmpty()); assertTrue(WidgetStateBindingCatalog.find(widget).isEmpty());
        assertEquals(Set.of("preferBelow", "excludeFromSemantics", "enableFeedback"), Set.copyOf(WidgetStatePropertyBindingCatalog.descriptors(widget).stream().map(value -> value.propertyName().value()).toList()));
        assertTrue(Arrays.stream(node(widget, new ArrayList<>()).getPropertySets()).noneMatch(set -> set.getName().equals(FlutterWidgetPropertiesNode.EVENTS_SET_NAME)));
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (List.of("data", "height", "textStyle", "textStyleForeground", "textStyleBackground").contains(name)) continue;
            values.put(field.name(), value(name));
        }
        return values;
    }
    public static PropertyValue value(String name) { return name.equals("data") ? TooltipPropertyContractTest.reference("_tooltipThemeData") : TooltipPropertyContractTest.value(name); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue("Retained themed anchor")), Map.of());
        return new WidgetNode(base.id(), base.type(), base.properties(), Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return TooltipPropertyContractTest.cell(node, name); }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return TooltipPropertyContractTest.apply(widget, command); }
    private static void assertValid(WidgetNode widget) { TooltipPropertyContractTest.assertValid(widget); }
}
