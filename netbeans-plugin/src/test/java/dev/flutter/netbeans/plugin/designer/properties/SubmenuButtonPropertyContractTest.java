package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class SubmenuButtonPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE).orElseThrow();

    @Test void all721RowsKeepExactEditorsSparseAtomicResetsAndStablePropertyGroups() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands); var groups = node.getPropertySets();
        assertEquals(721, DEF.properties().size()); assertEquals(498, SubmenuButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(203, SubmenuButtonWidgetPropertySchema.menuStyleProperties().size()); assertEquals(4, SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties().size());
        assertTrue(base.properties().isEmpty()); assertTrue(DEF.constConstructor()); assertEquals(4, DEF.slots().size()); assertTrue(DEF.slots().stream().allMatch(slot -> slot.minChildren() == 0));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).isEmpty()); assertValid(base);
        for (var field : DEF.properties()) {
            String name = field.name().value(); var properties = new LinkedHashMap<>(base.properties()); properties.putAll(TextButtonPropertyContractTest.sparsePrerequisites(name));
            var before = with(base, properties); node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var row = cell(node, name); var editor = row.getPropertyEditor(); var accepted = FlutterPropertyCellValue.explicit(value(name));
            assertTrue(row.canWrite(), name + " must remain writable"); assertTrue(row.supportsDefaultValue(), name + " must permit omission");
            editor.setValue(accepted); assertEquals(accepted, editor.getValue(), name); row.setValue(accepted); assertEquals(1, commands.size(), name);
            var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(value(name), after.properties().get(field.name())); assertEquals(before.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, cell(node, name)); assertArrayEquals(groups, node.getPropertySets());
            row.restoreDefaultValue(); assertEquals(1, commands.size(), name); var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name()), name); assertEquals(base.slots(), reset.slots());
        }
    }

    @Test void buttonMenuAndIconWholeLocalEditsNeverClearAnotherCompound() throws Exception {
        var base = with(prototype(), full()); assertTrue(base.properties().size() > 512); assertValid(base);
        var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands);
        for (String whole : List.of("style", "menuStyle", "submenuIcon")) {
            var local = localFields(whole);
            for (PropertyValue bound : List.of(value(whole), new PropertyValue.NullValue())) {
                node.refreshPresentation(base, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, whole).setValue(FlutterPropertyCellValue.explicit(bound)); assertEquals(1, commands.size());
                var after = apply(base, commands.removeFirst()); assertValid(after); assertEquals(bound, after.properties().get(p(whole)));
                local.forEach(name -> assertFalse(after.properties().containsKey(p(name)), name));
                base.properties().forEach((key, value) -> { if (!local.contains(key.value())) assertEquals(value, after.properties().get(key), key.value()); });
                assertEquals(base.slots(), after.slots());
                for (String leaf : local) {
                    node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); var start = after;
                    for (var prerequisite : TextButtonPropertyContractTest.sparsePrerequisites(leaf).entrySet()) {
                        cell(node, prerequisite.getKey().value()).setValue(FlutterPropertyCellValue.explicit(prerequisite.getValue())); start = apply(start, commands.removeFirst());
                        node.refreshPresentation(start, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
                    }
                    cell(node, leaf).setValue(FlutterPropertyCellValue.explicit(value(leaf))); var changed = apply(start, commands.removeFirst()); assertValid(changed);
                    assertFalse(changed.properties().containsKey(p(whole))); assertEquals(value(leaf), changed.properties().get(p(leaf)));
                    after.properties().forEach((key, value) -> { if (!key.value().equals(whole)) assertEquals(value, changed.properties().get(key), key.value()); });
                    assertEquals(base.slots(), changed.slots());
                }
            }
        }
    }

    @Test void menuAlignmentAndDensityDependencyEditsAreScopedAndReadonlyResetsCannotPublish() throws Exception {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY", "styleVisualDensityHorizontal", "styleVisualDensityVertical")) values.put(p(name), value(name));
        var before = with(prototype(), values); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "menuStyleAlignmentX").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(java.math.BigDecimal.ONE)));
        var aligned = apply(before, commands.removeFirst()); assertValid(aligned); assertEquals(new PropertyValue.StringValue("physical"), aligned.properties().get(p("menuStyleAlignmentKind")));
        assertEquals(new PropertyValue.DoubleValue(java.math.BigDecimal.ZERO), aligned.properties().get(p("menuStyleAlignmentY")));
        values.forEach((key, value) -> assertEquals(value, aligned.properties().get(key)));
        node.refreshPresentation(aligned, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "menuStyleAlignmentY").restoreDefaultValue();
        var reset = apply(aligned, commands.removeFirst()); assertValid(reset); assertEquals(values, reset.properties());
        node.refreshPresentation(reset, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "menuStyleVisualDensityHorizontal").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(java.math.BigDecimal.ONE)));
        var density = apply(reset, commands.removeFirst()); assertValid(density); assertFalse(density.properties().containsKey(p("menuStyleVisualDensityVertical")));
        node.refreshPresentation(density, DEF, null, null, null, FlutterImageAssetChoices.empty()); assertThrows(IllegalAccessException.class, () -> cell(node, "menuStyleVisualDensityHorizontal").restoreDefaultValue()); assertTrue(commands.isEmpty());
    }

    @Test void nativeFiveEventsAndTwoConsumersDoNotInventActivationOrBuilderActions() {
        var callbacks = WidgetEventCatalog.eventsFor(DEF);
        assertEquals(Set.of("onHover", "onFocusChange", "onOpen", "onClose", "onAnimationStatusChanged"), Set.copyOf(callbacks.stream().filter(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT).map(event -> event.propertyName().value()).toList()));
        assertEquals(Set.of("styleBackgroundBuilder", "styleForegroundBuilder"), Set.copyOf(callbacks.stream().filter(event -> event.kind() == WidgetEventDescriptor.Kind.BUILDER).map(event -> event.propertyName().value()).toList()));
        assertEquals(Set.of("useRootOverlay", "animated"), Set.copyOf(WidgetStatePropertyBindingCatalog.descriptors(prototype()).stream().map(value -> value.propertyName().value()).toList()));
        assertTrue(WidgetStateBindingCatalog.find(prototype()).isEmpty());
        for (String name : List.of("onPressed", "onLongPress", "enabled", "autofocus", "variant", "builder", "shortcut")) assertTrue(DEF.property(p(name)).isEmpty());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full(true, false); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean directional, boolean paints) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var property : DEF.properties()) {
            String name = property.name().value(); if (List.of("style", "menuStyle", "submenuIcon").contains(name)) continue;
            if (paints ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (name.endsWith("ShapeCircleEccentricity")) continue;
            result.put(property.name(), value(name));
        }
        for (String name : List.of("styleAlignmentKind", "menuStyleAlignmentKind")) result.put(p(name), new PropertyValue.StringValue(directional ? "directional" : "physical"));
        return result;
    }
    public static PropertyValue value(String name) {
        if (SubmenuButtonWidgetPropertySchema.menuStyleProperties().contains(name)) return MenuAnchorPropertyContractTest.value(SubmenuButtonWidgetPropertySchema.menuStyleSourceName(name));
        if (SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties().contains(name)) return PropertyValue.IconDataValue.none();
        if (name.equals("clipBehavior")) return new PropertyValue.EnumValue("Clip", "hardEdge");
        if (name.equals("alignmentOffset")) return new PropertyValue.OffsetValue(java.math.BigDecimal.ONE, java.math.BigDecimal.TWO);
        return TextButtonPropertyContractTest.value(DEF.property(p(name)).orElseThrow());
    }
    public static WidgetNode prototype() { return WidgetNodePrototypeFactory.create(DEF, StableId.random()); }
    static List<String> localFields(String whole) { return whole.equals("style") ? SubmenuButtonWidgetPropertySchema.localStyleProperties() : whole.equals("menuStyle") ? SubmenuButtonWidgetPropertySchema.menuStyleProperties() : SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties(); }
    static PropertyName p(String name) { return new PropertyName(name); }
    static WidgetNode with(WidgetNode base, Map<PropertyName, PropertyValue> values) { return new WidgetNode(base.id(), base.type(), values, base.slots(), base.extensions(), base.stateBinding(), base.propertyBindings()); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return TooltipPropertyContractTest.cell(node, name); }
    static WidgetNode apply(WidgetNode before, DesignerCommand command) { return with(before, TextButtonPropertyContractTest.apply(before.properties(), command)); }
    static void assertValid(WidgetNode widget) { TooltipPropertyContractTest.assertValid(widget); }
}
