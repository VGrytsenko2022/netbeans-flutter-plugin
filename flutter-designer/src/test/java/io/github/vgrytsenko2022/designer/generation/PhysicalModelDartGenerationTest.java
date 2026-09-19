package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.PhysicalModelTestSupport.*;

class PhysicalModelDartGenerationTest {
    @Test
    void defaultPrototypeIsDeterministicConstWithRequiredColorOnly() {
        var node = physicalModel(defaults(), false);
        var first = generate(node);
        var second = generate(node);
        assertEquals(first, second);
        assertEquals("import 'package:flutter/widgets.dart';\n", first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const PhysicalModel("), build);
        assertTrue(build.contains("color: const Color(0xFF2196F3)"), build);
        assertTrue(build.contains("child: null"), build);
        for (String omitted : List.of("shape", "clipBehavior", "borderRadius", "elevation", "shadowColor")) {
            assertFalse(build.contains(omitted + ":"), build);
        }
    }

    @Test
    void allShapesClipsAndEllipticalCornersGenerateInExactConstructorOrderWithoutDiscardingRadius() {
        for (String shape : List.of("rectangle", "circle")) {
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                var generated = generate(physicalModel(fullProperties(shape, clip, false), true));
                String build = generated.build().payload();
                assertTrue(build.contains("return const PhysicalModel("), build);
                assertTrue(build.contains("shape: BoxShape." + shape), build);
                assertTrue(build.contains("clipBehavior: Clip." + clip), build);
                assertTrue(build.contains("borderRadius: const BorderRadius.only("), build);
                assertTrue(build.contains("topLeft: const Radius.elliptical(1.5, 2.5)"), build);
                assertTrue(build.contains("topRight: const Radius.elliptical(3.5, 4.5)"), build);
                assertTrue(build.contains("bottomRight: const Radius.elliptical(5.5, 6.5)"), build);
                assertTrue(build.contains("bottomLeft: const Radius.elliptical(7.5, 8.5)"), build);
                assertTrue(build.contains("elevation: 12.5"), build);
                assertTrue(build.contains("color: const Color(0xFF102030)"), build);
                assertTrue(build.contains("shadowColor: const Color(0x80204060)"), build);
                assertTrue(build.contains("child: const Text('Preserved child')"), build);
                int previous = -1;
                for (String property : List.of("shape", "clipBehavior", "borderRadius", "elevation", "color", "shadowColor", "child")) {
                    int next = build.indexOf(property + ":");
                    assertTrue(next > previous, build);
                    previous = next;
                }
            }
        }
    }

    @Test
    void eachThemeColorIndependentlyUsesMaterialThemeAndRemovesOnlyRequiredConstness() {
        for (String color : List.of("color", "shadowColor")) {
            var properties = new LinkedHashMap<>(fullProperties("circle", "antiAlias", false));
            properties.put(name(color), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
            var generated = generate(physicalModel(properties, true));
            assertEquals("import 'package:flutter/material.dart';\n", generated.imports().payload());
            String build = generated.build().payload();
            assertTrue(build.contains("return PhysicalModel("), build);
            assertFalse(build.contains("return const PhysicalModel("), build);
            assertTrue(build.contains(color + ": Theme.of(context).colorScheme.primary"), build);
            assertTrue(build.contains("child: const Text("), build);
            assertTrue(build.contains("borderRadius: const BorderRadius.only("), build);
        }
    }

    private static GeneratedDartRegions generate(WidgetNode node) {
        var result = new DartRegionGenerator().generate(document(node), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}
