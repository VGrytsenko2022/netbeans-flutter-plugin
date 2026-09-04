package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ColoredBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColoredBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void prototypeAlwaysEmitsRequiredLiteralColorAndRemainsConst() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                BuiltInWidgetCatalog.getDefault()
                        .find(ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE)
                        .orElseThrow(),
                StableId.random());

        GeneratedDartRegions generated = generate(prototype);

        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const ColoredBox("), build);
        assertTrue(build.contains("color: const Color(0xFF2196F3)"), build);
        assertTrue(build.contains("child: null"), build);
        assertFalse(build.contains("isAntiAlias:"), build);
        assertOrdered(build, "color:", "child:");
    }

    @Test
    void explicitLiteralPolicyAndChildUseCompleteConstConstructor() {
        WidgetNode coloredBox = coloredBox(
                Map.of(
                        name("color"), new PropertyValue.ColorValue(0xFF102030L),
                        name("isAntiAlias"), new PropertyValue.BooleanValue(false)),
                Optional.of(text("Foreground")));

        String build = generate(coloredBox).build().payload();

        assertTrue(build.contains("return const ColoredBox("), build);
        assertTrue(build.contains("color: const Color(0xFF102030)"), build);
        assertTrue(build.contains("isAntiAlias: false"), build);
        assertTrue(build.contains("child: const Text('Foreground')"), build);
        assertOrdered(build, "color:", "isAntiAlias:", "child:");
    }

    @Test
    void themeTokenUsesMaterialThemeAndCorrectlyRemovesConst() {
        WidgetNode coloredBox = coloredBox(
                Map.of(name("color"), new PropertyValue.ThemeTokenValue(
                        new ThemeToken("material.colorScheme.primary"))),
                Optional.empty());

        GeneratedDartRegions generated = generate(coloredBox);

        assertEquals(List.of(MATERIAL_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return ColoredBox("), build);
        assertFalse(build.contains("return const ColoredBox("), build);
        assertTrue(build.contains(
                "color: Theme.of(context).colorScheme.primary"), build);
        assertTrue(build.contains("child: null"), build);
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode coloredBox(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE,
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(
                Optional.empty(), StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(
                                new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))),
                Optional.empty(), root, Extensions.empty());
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static void assertOrdered(String value, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int current = value.indexOf(needle);
            assertTrue(current > previous, needle + " is out of order:\n" + value);
            previous = current;
        }
    }
}
