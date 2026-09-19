package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.CircleAvatarWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.command.DesignerCommand;
import io.github.vgrytsenko2022.designer.command.PatchProperties;
import io.github.vgrytsenko2022.designer.command.ResetProperty;
import io.github.vgrytsenko2022.designer.command.SetProperty;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class CircleAvatarPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3900001-e530-4b9b-92fa-49e3c491f091");

    @Test void allNineOptionalRowsEditResetWithoutReplacingCellsOrGroups() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        assertTrue(initial.properties().isEmpty());
        assertEquals(Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()), initial.slots());
        var commands = new ArrayList<DesignerCommand>();
        var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(9, DEF.properties().size());
        assertEquals(6, sets.length);
        for (var definition : DEF.properties()) {
            String name = definition.name().value();
            var seed = new LinkedHashMap<PropertyName, PropertyValue>();
            if (name.equals("onBackgroundImageError")) seed.put(p("backgroundImage"), value("backgroundImage"));
            if (name.equals("onForegroundImageError")) seed.put(p("foregroundImage"), value("foregroundImage"));
            var widget = new WidgetNode(ID, DEF.typeId(), seed, initial.slots());
            node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name);
            assertTrue(cell.canWrite()); assertTrue(cell.supportsDefaultValue());
            assertEquals(FlutterPropertyCellValue.unset(), cell.getValue());
            var editor = cell.getPropertyEditor(); assertNotNull(editor);
            editor.setValue(FlutterPropertyCellValue.explicit(value(name)));
            if (!name.endsWith("Image")) {
                editor.setAsText(editor.getAsText());
                assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue());
            }
            if (name.endsWith("Image") || name.endsWith("Error") || name.endsWith("Color")) {
                assertTrue(editor.supportsCustomEditor(), name);
            }
            if (name.endsWith("Color")) assertTrue(editor.isPaintable());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new SetProperty(ID, p(name), value(name))), commands);
            var edited = apply(widget, commands.getFirst());
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            assertEquals(value(name), cell.getValue().explicitValue().orElseThrow());
            commands.clear(); cell.restoreDefaultValue(); assertEquals(1, commands.size());
            assertEquals(widget, apply(edited, commands.getFirst()));
        }
    }

    @Test void radiiSwitchAtomicallyAndDoNotRemoveOtherValuesOrChild() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("minRadius"), value("minRadius")); values.put(p("maxRadius"), value("maxRadius"));
        values.put(p("foregroundColor"), value("foregroundColor"));
        var before = new WidgetNode(ID, DEF.typeId(), values, initial.slots());
        var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "radius").setValue(FlutterPropertyCellValue.explicit(value("radius")));
        var patch = assertInstanceOf(PatchProperties.class, commands.getFirst());
        assertEquals(3, patch.patches().size());
        var fixed = apply(before, patch);
        assertEquals(Map.of(p("radius"), value("radius"), p("foregroundColor"), value("foregroundColor")), fixed.properties());
        for (String bound : List.of("minRadius", "maxRadius")) {
            node.refreshPresentation(fixed, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            commands.clear(); cell(node, bound).setValue(FlutterPropertyCellValue.explicit(value(bound)));
            assertEquals(2, assertInstanceOf(PatchProperties.class, commands.getFirst()).patches().size());
            var bounded = apply(fixed, commands.getFirst()); assertFalse(bounded.properties().containsKey(p("radius")));
            assertEquals(value(bound), bounded.properties().get(p(bound))); assertEquals(before.slots(), bounded.slots());
        }
    }

    @Test void imageCallbacksRequireTheirOwnProviderAndProviderResetIsAtomic() throws Exception {
        for (String prefix : List.of("Background", "Foreground")) {
            String image = prefix.toLowerCase(Locale.ROOT) + "Image";
            String callback = "on" + prefix + "ImageError";
            var initial = WidgetNodePrototypeFactory.create(DEF, ID);
            var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands);
            var error = assertThrows(IllegalArgumentException.class,
                    () -> cell(node, callback).setValue(FlutterPropertyCellValue.explicit(value(callback))));
            assertTrue(error.getMessage().contains(ID.toString())); assertTrue(error.getMessage().contains(image));
            assertTrue(commands.isEmpty());
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            for (String name : List.of("backgroundImage", "foregroundImage", "onBackgroundImageError", "onForegroundImageError")) values.put(p(name), value(name));
            var before = new WidgetNode(ID, DEF.typeId(), values, initial.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            cell(node, image).restoreDefaultValue();
            assertEquals(2, assertInstanceOf(PatchProperties.class, commands.getFirst()).patches().size());
            var after = apply(before, commands.getFirst()); values.remove(p(image)); values.remove(p(callback));
            assertEquals(values, after.properties()); assertEquals(before.slots(), after.slots());
        }
    }

    @Test void allThreeRadiusEditorsUseClosedInfinityAndRejectExpressionsWithoutChangingDraft() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            var editor = cell(node, name).getPropertyEditor();
            for (String valid : List.of("0", "4.5", "Infinity", "infinity", "<not set>")) {
                editor.setAsText(valid);
                if (valid.equalsIgnoreCase("Infinity")) {
                    assertEquals(new PropertyValue.EnumValue("double", "infinity"), ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
                    assertEquals("Infinity", editor.getAsText());
                }
            }
            editor.setAsText("12.5"); var previous = editor.getValue();
            for (String invalid : List.of("-1", "NaN", "-Infinity", "double.infinity", "1 + 2", "1e999")) {
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid), name + ": " + invalid);
                assertEquals(previous, editor.getValue());
            }
        }
    }

    public static PropertyValue value(String name) {
        return switch (name) {
            case "backgroundColor" -> new PropertyValue.ColorValue(0xff123456L);
            case "foregroundColor" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            case "backgroundImage" -> PropertyValue.ImageProviderValue.asset("assets/avatar.png");
            case "foregroundImage" -> PropertyValue.ImageProviderValue.exactAsset("assets/avatar.png", BigDecimal.valueOf(2));
            case "onBackgroundImageError" -> new PropertyValue.CallbackValue("handleBackgroundError");
            case "onForegroundImageError" -> new PropertyValue.CallbackValue("handleForegroundError");
            case "radius" -> new PropertyValue.DoubleValue(BigDecimal.valueOf(24));
            case "minRadius" -> new PropertyValue.IntegerValue(BigInteger.valueOf(8));
            case "maxRadius" -> new PropertyValue.IntegerValue(BigInteger.valueOf(32));
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) {
        return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add);
    }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) {
        return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets())
                .flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow();
    }
    private static WidgetNode apply(WidgetNode widget, DesignerCommand command) {
        var values = new LinkedHashMap<>(widget.properties());
        var patches = command instanceof PatchProperties patch ? patch.patches()
                : command instanceof SetProperty set ? List.<PatchProperties.Patch>of(new PatchProperties.SetPatch(set.propertyName(), set.value()))
                : List.<PatchProperties.Patch>of(new PatchProperties.ResetPatch(((ResetProperty) command).propertyName()));
        for (var patch : patches) {
            if (patch instanceof PatchProperties.SetPatch set) values.put(set.propertyName(), set.value());
            else values.remove(patch.propertyName());
        }
        return new WidgetNode(widget.id(), widget.type(), values, widget.slots());
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
