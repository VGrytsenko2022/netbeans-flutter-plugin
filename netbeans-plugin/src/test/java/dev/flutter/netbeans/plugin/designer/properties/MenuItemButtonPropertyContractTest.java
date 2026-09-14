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

public class MenuItemButtonPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE).orElseThrow();

    @Test void all520RowsKeepTypedEditorsIdentitySparseResetsAndOptionalChildren() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands); var groups = node.getPropertySets();
        assertEquals(520, DEF.properties().size()); assertEquals(498, MenuItemButtonWidgetPropertySchema.localStyleProperties().size()); assertEquals(8, MenuItemButtonWidgetPropertySchema.shortcutLocalProperties().size());
        assertEquals(Map.of(p("enabled"), new PropertyValue.BooleanValue(true)), base.properties());
        assertTrue(DEF.constConstructor()); assertEquals(3, DEF.slots().size()); assertTrue(DEF.slots().stream().allMatch(slot -> slot.minChildren() == 0)); assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(DEF).isEmpty());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var values = new LinkedHashMap<>(base.properties()); values.putAll(TextButtonPropertyContractTest.sparsePrerequisites(name));
            if (name.startsWith("shortcut") && !List.of("shortcut", "shortcutTrigger", "shortcutCharacter").contains(name)) values.put(p("shortcutTrigger"), value("shortcutTrigger"));
            var before = with(base, values); node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var row = cell(node, name); var editor = row.getPropertyEditor(); var value = value(name);
            assertTrue(row.canWrite()); assertEquals(!field.parameter().required(), row.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue());
            row.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), name); var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(value, after.properties().get(field.name())); assertEquals(before.slots(), after.slots());
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); assertSame(row, cell(node, name)); assertArrayEquals(groups, node.getPropertySets());
            if (row.supportsDefaultValue()) { row.restoreDefaultValue(); assertEquals(1, commands.size(), name); var reset = apply(after, commands.removeFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); assertEquals(base.slots(), reset.slots()); }
        }
    }

    @Test void wholeStyleAndEveryLocalStyleLeafSwitchAtomicallyWithoutAConstructorVariant() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var before = with(base, full()); var node = node(before, commands);
        cell(node, "style").setValue(FlutterPropertyCellValue.explicit(value("style"))); var whole = apply(before, commands.removeFirst()); assertValid(whole);
        assertEquals(value("style"), whole.properties().get(p("style"))); MenuItemButtonWidgetPropertySchema.localStyleProperties().forEach(name -> assertFalse(whole.properties().containsKey(p(name))));
        assertEquals(before.slots(), whole.slots()); assertEquals(before.properties().get(p("shortcutTrigger")), whole.properties().get(p("shortcutTrigger")));
        for (String name : MenuItemButtonWidgetPropertySchema.localStyleProperties()) {
            node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var starting = whole;
            for (var prerequisite : TextButtonPropertyContractTest.sparsePrerequisites(name).entrySet()) {
                cell(node, prerequisite.getKey().value()).setValue(FlutterPropertyCellValue.explicit(prerequisite.getValue())); starting = apply(starting, commands.removeFirst()); assertValid(starting);
                node.refreshPresentation(starting, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            }
            cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name))); var local = apply(starting, commands.removeFirst()); assertValid(local);
            assertFalse(local.properties().containsKey(p("style"))); assertEquals(value(name), local.properties().get(p(name))); assertEquals(before.slots(), local.slots());
            assertFalse(local.properties().containsKey(p("variant"))); assertFalse(local.properties().containsKey(p("iconAlignment")));
        }
    }

    @Test void allShortcutBranchesAndAnchorResetAreAtomicAndNeverInventAKey() throws Exception {
        var base = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(base, commands);
        for (String name : MenuItemButtonWidgetPropertySchema.shortcutLocalProperties()) if (!List.of("shortcutTrigger", "shortcutCharacter").contains(name)) {
            assertThrows(IllegalArgumentException.class, () -> cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)))); assertTrue(commands.isEmpty());
        }
        var values = new LinkedHashMap<>(base.properties()); MenuItemButtonWidgetPropertySchema.shortcutLocalProperties().stream().filter(name -> !name.equals("shortcutCharacter")).forEach(name -> values.put(p(name), value(name)));
        var single = with(base, values); assertValid(single); node.refreshPresentation(single, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        cell(node, "shortcutCharacter").setValue(FlutterPropertyCellValue.explicit(value("shortcutCharacter"))); var character = apply(single, commands.removeFirst()); assertValid(character);
        for (String name : List.of("shortcutTrigger", "shortcutShift", "shortcutNumLock")) assertFalse(character.properties().containsKey(p(name)));
        for (String name : List.of("shortcutControl", "shortcutAlt", "shortcutMeta", "shortcutIncludeRepeats")) assertEquals(single.properties().get(p(name)), character.properties().get(p(name)));
        node.refreshPresentation(character, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        for (String name : List.of("shortcutShift", "shortcutNumLock")) assertThrows(IllegalArgumentException.class, () -> cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)))); assertTrue(commands.isEmpty());
        cell(node, "shortcutCharacter").restoreDefaultValue(); var reset = apply(character, commands.removeFirst()); assertEquals(base.properties(), reset.properties()); assertEquals(base.slots(), reset.slots());
        for (PropertyValue wholeValue : List.of(value("shortcut"), new PropertyValue.NullValue())) {
            node.refreshPresentation(single, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "shortcut").setValue(FlutterPropertyCellValue.explicit(wholeValue));
            var whole = apply(single, commands.removeFirst()); assertValid(whole); MenuItemButtonWidgetPropertySchema.shortcutLocalProperties().forEach(name -> assertFalse(whole.properties().containsKey(p(name))));
            node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "shortcutTrigger").setValue(FlutterPropertyCellValue.explicit(value("shortcutTrigger")));
            var local = apply(whole, commands.removeFirst()); assertValid(local); assertFalse(local.properties().containsKey(p("shortcut"))); assertEquals(value("shortcutTrigger"), local.properties().get(p("shortcutTrigger")));
        }
        node.refreshPresentation(single, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "shortcutAlt").restoreDefaultValue(); var one = apply(single, commands.removeFirst()); assertValid(one);
        assertFalse(one.properties().containsKey(p("shortcutAlt"))); assertEquals(single.properties().get(p("shortcutTrigger")), one.properties().get(p("shortcutTrigger")));
        node.refreshPresentation(single, DEF, null, null, null, FlutterImageAssetChoices.empty()); assertThrows(IllegalAccessException.class, () -> cell(node, "shortcutTrigger").restoreDefaultValue()); assertTrue(commands.isEmpty());
    }

    @Test void all432KeysAreExactClosedEnumValuesAndNoForbiddenModifierTriggerIsAdmitted() {
        var binding = FlutterTypedPropertyEditors.binding(DEF.property(p("shortcutTrigger")).orElseThrow()).orElseThrow(); assertEquals(432, MenuShortcutKeyCatalog.names().size());
        for (String key : MenuShortcutKeyCatalog.names()) assertDoesNotThrow(() -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("LogicalKeyboardKey", key))), key);
        for (String key : List.of("shift", "shiftLeft", "shiftRight", "control", "controlLeft", "controlRight", "alt", "altLeft", "altRight", "meta", "metaLeft", "metaRight", "inventedKey"))
            assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("LogicalKeyboardKey", key))), key);
        assertThrows(IllegalArgumentException.class, () -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("LogicalKeyboardKey.keyK"))));
    }

    @Test void onlyThreeNativeEventsTwoLayerBuildersAndFourBooleanConsumersExist() {
        var descriptors = WidgetEventCatalog.eventsFor(DEF); assertEquals(5, descriptors.size());
        assertEquals(Set.of("onPressed", "onHover", "onFocusChange"), Set.copyOf(descriptors.stream().filter(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT).map(event -> event.propertyName().value()).toList()));
        assertEquals(Set.of("styleBackgroundBuilder", "styleForegroundBuilder"), Set.copyOf(descriptors.stream().filter(event -> event.kind() == WidgetEventDescriptor.Kind.BUILDER).map(event -> event.propertyName().value()).toList()));
        assertEquals(Set.of("enabled", "autofocus", "requestFocusOnHover", "closeOnActivate"), Set.copyOf(WidgetStatePropertyBindingCatalog.descriptors(prototype()).stream().map(value -> value.propertyName().value()).toList()));
        assertTrue(WidgetStateBindingCatalog.find(prototype()).isEmpty());
        for (String name : List.of("variant", "onLongPress", "iconAlignment")) assertTrue(DEF.property(p(name)).isEmpty());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full(true, false); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean directional, boolean paints) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (List.of("style", "shortcut", "shortcutCharacter").contains(name)) continue;
            if (paints ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (name.endsWith("ShapeCircleEccentricity")) continue;
            result.put(field.name(), value(name));
        }
        result.put(p("enabled"), new PropertyValue.BooleanValue(true)); result.put(p("styleAlignmentKind"), new PropertyValue.StringValue(directional ? "directional" : "physical"));
        return result;
    }
    public static PropertyValue value(String name) {
        return switch (name) {
            case "shortcutTrigger" -> new PropertyValue.EnumValue("LogicalKeyboardKey", "keyK");
            case "shortcutCharacter" -> new PropertyValue.StringValue("k");
            case "shortcutNumLock" -> new PropertyValue.EnumValue("LockState", "ignored");
            case "clipBehavior" -> new PropertyValue.EnumValue("Clip", "none");
            case "semanticsLabel" -> new PropertyValue.StringValue("Open menu item");
            default -> TextButtonPropertyContractTest.value(DEF.property(p(name)).orElseThrow());
        };
    }
    public static WidgetNode prototype() { return WidgetNodePrototypeFactory.create(DEF, StableId.random()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
    static WidgetNode with(WidgetNode base, Map<PropertyName, PropertyValue> values) { return new WidgetNode(base.id(), base.type(), values, base.slots(), base.extensions(), base.stateBinding(), base.propertyBindings()); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return TooltipPropertyContractTest.cell(node, name); }
    static WidgetNode apply(WidgetNode before, DesignerCommand command) { return with(before, TextButtonPropertyContractTest.apply(before.properties(), command)); }
    static void assertValid(WidgetNode widget) { TooltipPropertyContractTest.assertValid(widget); }
}
