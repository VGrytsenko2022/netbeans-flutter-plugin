package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartNumericLiterals;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacerDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void omittedFlexRemainsOmittedAndFlutterOwnsTheDefault() {
        DartGenerationResult result = generateInRow(spacer(Map.of()));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("const Spacer()"), build);
        assertFalse(build.contains("flex:"), build);
    }

    @Test
    void emitsPositiveMinimumAndPortableMaximumFlexInConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> minimumProperties =
                new LinkedHashMap<>();
        minimumProperties.put(name("flex"),
                new PropertyValue.IntegerValue(BigInteger.ONE));
        LinkedHashMap<PropertyName, PropertyValue> maximumProperties =
                new LinkedHashMap<>();
        maximumProperties.put(name("flex"), new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER));

        DartGenerationResult result = generateInRow(
                spacer(minimumProperties), spacer(maximumProperties));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertEquals(2, occurrences(build, "const Spacer("), build);
        assertTrue(build.contains("flex: 1"), build);
        assertTrue(build.contains(
                "flex: " + DartNumericLiterals.MAX_PORTABLE_INTEGER), build);
    }

    @Test
    void rejectsZeroFlexAndRootPlacementBeforeDartEmission() {
        WidgetNode zero = spacer(Map.of(name("flex"),
                new PropertyValue.IntegerValue(BigInteger.ZERO)));

        DartGenerationResult zeroResult = generateInRow(zero);
        DartGenerationResult rootResult = new DartRegionGenerator().generate(
                document(spacer(Map.of())), BuiltInWidgetCatalog.getDefault());

        assertFalse(zeroResult.successful());
        assertTrue(zeroResult.generated().isEmpty());
        assertTrue(zeroResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("flex")),
                () -> zeroResult.diagnostics().toString());
        assertFalse(rootResult.successful());
        assertTrue(rootResult.generated().isEmpty());
        assertTrue(rootResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("direct child")),
                () -> rootResult.diagnostics().toString());
    }

    private static DartGenerationResult generateInRow(WidgetNode... children) {
        WidgetNode row = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Row"),
                Map.of(),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(children))),
                Extensions.empty());
        return new DartRegionGenerator().generate(
                document(row), BuiltInWidgetCatalog.getDefault());
    }

    private static WidgetNode spacer(Map<PropertyName, PropertyValue> properties) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Spacer"),
                properties,
                Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(
                Optional.empty(),
                StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart",
                        "Sample",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(
                                new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))),
                Optional.empty(),
                root,
                Extensions.empty());
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
