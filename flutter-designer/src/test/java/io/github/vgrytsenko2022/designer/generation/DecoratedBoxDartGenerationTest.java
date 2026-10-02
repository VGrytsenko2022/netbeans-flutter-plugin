package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.DecoratedBoxWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.model.ColorSource;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.ThemeToken;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoratedBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void prototypeEmitsExplicitEmptyDecorationAndOmitsFlutterPositionDefault() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                BuiltInWidgetCatalog.getDefault()
                        .find(DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE)
                        .orElseThrow(),
                StableId.random());

        GeneratedDartRegions generated = generateSuccessful(prototype);

        assertEquals(List.of(WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const DecoratedBox("), build);
        assertTrue(build.contains("decoration: const BoxDecoration("), build);
        assertFalse(build.contains("position:"), build);
        assertTrue(build.contains("child: null"), build);
        assertOrdered(build, "decoration:", "child:");
    }

    @Test
    void literalDecorationForegroundAndChildRemainFullyConstAndOrdered() {
        WidgetNode decoratedBox = decoratedBox(
                Map.of(
                        name("decoration"), decoration(
                                new ColorSource.Literal(0xFF102030L)),
                        name("position"), new PropertyValue.EnumValue(
                                "DecorationPosition", "foreground")),
                Optional.of(text("Foreground")));

        GeneratedDartRegions generated = generateSuccessful(decoratedBox);

        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();

        assertTrue(build.contains("return const DecoratedBox("), build);
        assertTrue(build.contains("decoration: const BoxDecoration("), build);
        assertTrue(build.contains("color: const Color(0xFF102030)"), build);
        assertTrue(build.contains(
                ".DecorationPosition.foreground"), build);
        assertTrue(build.contains("child: const Text('Foreground')"), build);
        assertOrdered(build, "decoration:", "position:", "child:");
    }

    @Test
    void nestedThemeTokenUsesMaterialThemeAndRemovesConstTransitively() {
        WidgetNode decoratedBox = decoratedBox(
                Map.of(name("decoration"), decoration(
                        new ColorSource.Theme(new ThemeToken(
                                "material.colorScheme.primary")))),
                Optional.empty());

        GeneratedDartRegions generated = generateSuccessful(decoratedBox);

        assertEquals(List.of(MATERIAL_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return DecoratedBox("), build);
        assertFalse(build.contains("return const DecoratedBox("), build);
        assertTrue(build.contains("decoration: BoxDecoration("), build);
        assertFalse(build.contains("decoration: const BoxDecoration("), build);
        assertTrue(build.contains(
                "color: Theme.of(context).colorScheme.primary"), build);
    }

    @Test
    void missingDecorationAndWrongPositionFailBeforeDartEmission() {
        DartGenerationResult missing = generate(decoratedBox(
                Map.of(), Optional.empty()));
        DartGenerationResult wrongPosition = generate(decoratedBox(
                Map.of(
                        name("decoration"), emptyDecoration(),
                        name("position"), new PropertyValue.EnumValue(
                                "DecorationPosition", "middle")),
                Optional.empty()));

        assertFalse(missing.successful());
        assertTrue(missing.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/properties/decoration")),
                () -> missing.diagnostics().toString());
        assertFalse(wrongPosition.successful());
        assertTrue(wrongPosition.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/properties/position")),
                () -> wrongPosition.diagnostics().toString());
    }

    private static GeneratedDartRegions generateSuccessful(WidgetNode root) {
        DartGenerationResult result = generate(root);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static DartGenerationResult generate(WidgetNode root) {
        return new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
    }

    private static WidgetNode decoratedBox(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE,
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    private static PropertyValue.BoxDecorationValue emptyDecoration() {
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static PropertyValue.BoxDecorationValue decoration(ColorSource color) {
        return new PropertyValue.BoxDecorationValue(
                Optional.of(color), Optional.empty(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
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
