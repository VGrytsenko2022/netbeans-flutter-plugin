package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.*;

class PhysicalShapeDartGenerationTest {
    @Test
    void prototypeIsUsableDeterministicAndConstWithExactSdkSymbolEvidence() {
        var node = physicalShape(defaults(), false);
        var generated = generate(node);
        assertEquals(generated, generate(node));
        assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
        String build = generated.build().payload();
        assertTrue(build.contains("return const PhysicalShape("), build);
        assertTrue(build.contains("clipper: const ShapeBorderClipper("), build);
        assertTrue(build.contains("shape: const RoundedRectangleBorder("), build);
        assertTrue(build.contains("color: const Color(0xFF2196F3)"), build);
        assertTrue(build.contains("child: null"), build);
        for (String omitted : List.of("clipBehavior", "elevation", "shadowColor", "textDirection")) {
            assertFalse(build.contains(omitted + ":"), build);
        }
        var names = generated.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::symbolName).toList();
        for (String symbol : List.of("PhysicalShape", "ShapeBorderClipper", "RoundedRectangleBorder", "BorderRadius", "Radius", "Color")) {
            assertTrue(names.contains(symbol), names.toString());
        }
    }

    @Test
    void allSixShapesClipsAndDirectionalEllipticalCornersGenerateExactConstructors() {
        var classes = List.of("RoundedRectangleBorder", "BeveledRectangleBorder", "ContinuousRectangleBorder",
                "RoundedSuperellipseBorder", "CircleBorder", "StadiumBorder");
        for (var shape : PropertyValue.ShapeBorderClipperValue.Shape.values()) {
            for (boolean directional : List.of(false, true)) {
                for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                    var properties = new LinkedHashMap<>(fullProperties(shape, clip, false));
                    properties.put(name("clipper"), clipper(shape, directional));
                    String build = generate(physicalShape(properties, true)).build().payload();
                    assertTrue(build.contains("return const PhysicalShape("), build);
                    assertTrue(build.contains("shape: const " + classes.get(shape.ordinal()) + "("), build);
                    assertEquals(shape.supportsRadius(), build.contains("borderRadius:"), build);
                    assertEquals(shape.supportsRadius() && directional, build.contains("textDirection: TextDirection.rtl"), build);
                    if (shape.supportsRadius()) {
                        assertTrue(build.contains(directional ? "BorderRadiusDirectional.only(" : "BorderRadius.only("), build);
                        assertTrue(build.contains("Radius.elliptical(1.5, 2.5)"), build);
                    }
                    assertTrue(build.contains("clipBehavior: Clip." + clip), build);
                    assertTrue(build.contains("elevation: 12.5"), build);
                    assertTrue(build.contains("child: const Text('Preserved child')"), build);
                    int prior = -1;
                    for (String property : List.of("clipper", "clipBehavior", "elevation", "color", "shadowColor", "child")) {
                        int at = build.indexOf(property + ":");
                        assertTrue(at > prior, build);
                        prior = at;
                    }
                }
            }
        }
    }

    @Test
    void themeColorsRemainIndependentAndPreserveNestedClipperConstness() {
        for (String color : List.of("color", "shadowColor")) {
            var values = new LinkedHashMap<>(defaults());
            values.put(name(color), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
            var generated = generate(physicalShape(values, true));
            String build = generated.build().payload();
            assertEquals("import 'package:flutter/material.dart';\n", generated.imports().payload());
            assertTrue(build.contains("return PhysicalShape("), build);
            assertTrue(build.contains(color + ": Theme.of(context).colorScheme.primary"), build);
            assertTrue(build.contains("clipper: const ShapeBorderClipper("), build);
            assertTrue(build.contains("child: const Text("), build);
        }
    }

    @Test
    void exactPathProjectReferenceRetainsProbesAndConstnessForBothLibraries() {
        for (boolean imported : List.of(false, true)) {
            for (boolean constant : List.of(false, true)) {
                var values = new LinkedHashMap<>(defaults());
                values.put(name("clipper"), new PropertyValue.DartObjectReferenceValue(
                        imported ? Optional.of("package:app/clippers.dart") : Optional.empty(),
                        "PathClipper", Optional.of("compact"),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                        Optional.of(constant)));
                var generated = generate(physicalShape(values, true));
                String build = generated.build().payload();
                assertEquals(constant, build.contains("return const PhysicalShape("), build);
                assertTrue(build.contains("PathClipper.compact()"), build);
                assertFalse(build.contains("ShapeBorderClipper"), build);
                assertTrue(generated.symbolOccurrences().stream()
                        .anyMatch(o -> o.symbolName().equals("PhysicalShape")));
                assertTrue(generated.symbolOccurrences().stream()
                        .filter(o -> o.staticTypeRequirement().isPresent())
                        .anyMatch(o -> o.staticTypeRequirement().orElseThrow().expectedDartType().equals("CustomClipper<Path>")));
            }
        }
    }

    private static GeneratedDartRegions generate(WidgetNode node) {
        var result = new DartRegionGenerator().generate(document(node), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}
