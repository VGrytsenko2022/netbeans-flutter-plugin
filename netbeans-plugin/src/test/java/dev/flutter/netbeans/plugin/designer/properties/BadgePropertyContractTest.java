package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class BadgePropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(BadgeWidgetPropertySchema.BADGE_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("b4930001-e530-4b9b-92fa-49e3c491f091");

    @Test void all41OptionalRowsReuseRichEditorsAndPreservePropertySetAndCellIdentity() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        assertTrue(initial.properties().isEmpty()); assertEquals(2, initial.slots().size());
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(41, DEF.properties().size());
        assertEquals(31, DEF.properties().stream().filter(v -> BadgeWidgetPropertySchema.isTextStyleProperty(v.name())).count());
        for (var definition : DEF.properties()) {
            String name = definition.name().value(); var seed = new LinkedHashMap<PropertyName, PropertyValue>();
            if (name.equals("maxCount")) seed.put(p("count"), value("count"));
            if (name.equals("textStylePackage")) seed.put(p("textStyleFontFamily"), value("textStyleFontFamily"));
            var widget = new WidgetNode(ID, DEF.typeId(), seed, initial.slots());
            node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); assertTrue(cell.canWrite()); assertTrue(cell.supportsDefaultValue());
            assertEquals(FlutterPropertyCellValue.unset(), cell.getValue()); assertNotNull(cell.getPropertyEditor());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(1, commands.size(), name); var edited = apply(widget, commands.getFirst()); assertValid(edited);
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            assertEquals(value(name), cell.getValue().explicitValue().orElseThrow());
            commands.clear(); cell.restoreDefaultValue(); assertEquals(1, commands.size());
            assertEquals(widget, apply(edited, commands.getFirst()));
        }
        var fallback = cell(node, "textStyleFontFamilyFallback").getPropertyEditor();
        fallback.setAsText("Inter, Noto Sans");
        assertEquals(new PropertyValue.StringValue("Inter\nNoto Sans"), ((FlutterPropertyCellValue) fallback.getValue()).explicitValue().orElseThrow());
        for (String name : List.of("isLabelVisible", "textStyleInherit", "textStyleDecorationUnderline", "textStyleDecorationOverline", "textStyleDecorationLineThrough")) {
            var editor = cell(node, name).getPropertyEditor(); assertNull(editor.getTags()); assertTrue(editor.isPaintable());
            for (boolean flag : List.of(false, true)) { editor.setAsText(Boolean.toString(flag)); assertEquals(new PropertyValue.BooleanValue(flag), ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow()); }
            editor.setValue(FlutterPropertyCellValue.unset()); assertEquals("<not set>", editor.getAsText());
        }
    }

    @Test void countAndLabelConflictsFailBeforeCommandAndCountResetIsOneAtomicPatch() throws Exception {
        var label = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        var child = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow(), StableId.random());
        var widget = new WidgetNode(ID, DEF.typeId(), Map.of(p("textColor"), value("textColor")), Map.of(new SlotName("label"), WidgetSlot.SingleSlot.of(label), new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
        var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands); var count = cell(node, "count"); var sets = node.getPropertySets();
        var conflict = assertThrows(IllegalArgumentException.class, () -> count.setValue(FlutterPropertyCellValue.explicit(value("count"))));
        assertTrue(conflict.getMessage().contains("Clear or move Label first")); assertTrue(conflict.getMessage().contains(ID.toString()));
        var max = cell(node, "maxCount"); var missing = assertThrows(IllegalArgumentException.class, () -> max.setValue(FlutterPropertyCellValue.explicit(value("maxCount"))));
        assertTrue(missing.getMessage().contains("set Count first")); assertTrue(commands.isEmpty());
        assertSame(count, cell(node, "count")); assertEquals(List.of(sets), List.of(node.getPropertySets()));
        widget = new WidgetNode(ID, DEF.typeId(), Map.of(p("count"), value("count"), p("maxCount"), value("maxCount"), p("textColor"), value("textColor")), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
        node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); count.restoreDefaultValue();
        assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst());
        var result = apply(widget, commands.getFirst()); assertValid(result);
        assertEquals(Map.of(p("textColor"), value("textColor")), result.properties()); assertEquals(widget.slots(), result.slots());
    }

    @Test void foregroundAndBackgroundPaintSwitchAtomicallyWithoutTouchingBadgeColors() throws Exception {
        for (var pair : List.of(List.of("textStyleColor", "textStyleForeground"), List.of("textStyleBackgroundColor", "textStyleBackground"))) {
            for (var names : List.of(pair, pair.reversed())) {
                var widget = new WidgetNode(ID, DEF.typeId(), Map.of(p(names.getFirst()), value(names.getFirst()), p("textColor"), value("textColor"), p("backgroundColor"), value("backgroundColor")), Map.of());
                var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
                cell(node, names.getLast()).setValue(FlutterPropertyCellValue.explicit(value(names.getLast())));
                assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst());
                var result = apply(widget, commands.getFirst()); assertValid(result);
                assertFalse(result.properties().containsKey(p(names.getFirst()))); assertEquals(value(names.getLast()), result.properties().get(p(names.getLast())));
                assertEquals(value("textColor"), result.properties().get(p("textColor"))); assertEquals(value("backgroundColor"), result.properties().get(p("backgroundColor")));
            }
        }
    }

    @Test void invalidNumericAndLocaleInputsKeepExactPreviousDraft() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("count", "maxCount", "smallSize", "textStyleFontSize")) {
            var editor = cell(node, name).getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(value(name)));
            var before = editor.getValue();
            for (String invalid : List.of("-1", "1e999", "NaN")) { assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid), name); assertEquals(before, editor.getValue()); }
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "maxCount").getPropertyEditor().setAsText("0"));
        for (String name : List.of("largeSize", "textStyleHeight", "textStyleLetterSpacing", "textStyleWordSpacing", "textStyleDecorationThickness")) {
            var editor = cell(node, name).getPropertyEditor(); editor.setAsText("-2.5");
            assertEquals(new PropertyValue.DoubleValue(new BigDecimal("-2.5")), ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("1e999"));
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "count").getPropertyEditor().setAsText("1.5"));
        assertThrows(IllegalArgumentException.class, () -> cell(node, "textStyleLocaleLanguageCode").getPropertyEditor().setAsText("en_US"));
    }

    public static PropertyValue value(String name) {
        if (name.equals("count")) return new PropertyValue.IntegerValue(BigInteger.valueOf(1001));
        if (name.equals("maxCount")) return new PropertyValue.IntegerValue(BigInteger.valueOf(999));
        if (name.equals("padding")) return new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        if (name.equals("alignment")) return new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL, BigDecimal.ONE, BigDecimal.ONE.negate());
        if (name.equals("offset")) return new PropertyValue.OffsetValue(BigDecimal.valueOf(-2), BigDecimal.valueOf(3));
        if (name.endsWith("Color")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.onError"));
        if (name.equals("textStyleThemeTextStyle")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.labelSmall"));
        if (name.startsWith("textStyleDecoration") && !name.endsWith("Color") && !name.endsWith("Style") && !name.endsWith("Thickness")) return new PropertyValue.BooleanValue(true);
        if (List.of("isLabelVisible", "textStyleInherit").contains(name)) return new PropertyValue.BooleanValue(false);
        if (name.equals("textStyleForeground") || name.equals("textStyleBackground")) return PropertyValue.PaintValue.defaults(new ColorSource.Theme(new ThemeToken("material.colorScheme.primary")));
        if (name.equals("textStyleShadows")) return new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(ID, new ColorSource.Literal(0xff123456L), BigDecimal.ONE, BigDecimal.valueOf(-2), BigDecimal.valueOf(3))));
        if (name.equals("textStyleFontFeatures")) return new PropertyValue.FontFeatureListValue(List.of(new PropertyValue.FontFeatureListValue.FontFeature(ID, "smcp", 1)));
        if (name.equals("textStyleFontVariations")) return new PropertyValue.FontVariationListValue(List.of(new PropertyValue.FontVariationListValue.FontVariation(ID, "wght", BigDecimal.valueOf(600))));
        return switch (name) {
            case "textStyleFontWeight" -> new PropertyValue.EnumValue("FontWeight", "w600");
            case "textStyleFontStyle" -> new PropertyValue.EnumValue("FontStyle", "italic");
            case "textStyleTextBaseline" -> new PropertyValue.EnumValue("TextBaseline", "ideographic");
            case "textStyleLeadingDistribution" -> new PropertyValue.EnumValue("TextLeadingDistribution", "even");
            case "textStyleDecorationStyle" -> new PropertyValue.EnumValue("TextDecorationStyle", "dashed");
            case "textStyleOverflow" -> new PropertyValue.EnumValue("TextOverflow", "ellipsis");
            case "textStyleLocaleLanguageCode" -> new PropertyValue.StringValue("uk");
            case "textStyleLocaleScriptCode" -> new PropertyValue.StringValue("Cyrl");
            case "textStyleLocaleCountryCode" -> new PropertyValue.StringValue("UA");
            case "textStyleDebugLabel" -> new PropertyValue.StringValue("Badge style");
            case "textStyleFontFamily" -> new PropertyValue.StringValue("Inter");
            case "textStyleFontFamilyFallback" -> new PropertyValue.StringValue("Noto Sans\nNoto Color Emoji");
            case "textStylePackage" -> new PropertyValue.StringValue("badge_fonts");
            default -> new PropertyValue.DoubleValue(new BigDecimal("2.5"));
        };
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static WidgetNode apply(WidgetNode widget, DesignerCommand command) {
        var values = new LinkedHashMap<>(widget.properties());
        var patches = command instanceof PatchProperties patch ? patch.patches() : command instanceof SetProperty set ? List.<PatchProperties.Patch>of(new PatchProperties.SetPatch(set.propertyName(), set.value())) : List.<PatchProperties.Patch>of(new PatchProperties.ResetPatch(((ResetProperty) command).propertyName()));
        for (var patch : patches) { if (patch instanceof PatchProperties.SetPatch set) values.put(set.propertyName(), set.value()); else values.remove(patch.propertyName()); }
        return new WidgetNode(widget.id(), widget.type(), values, widget.slots());
    }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("badge.dart", "BadgeScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var validation = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(validation.valid(), validation.toString()); }
    private static PropertyName p(String value) { return new PropertyName(value); }
}
