package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverflowBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsEveryReviewedArgumentInExactFlutterConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("fit"), enumValue("OverflowBoxFit", "deferToChild"));
        properties.put(name("maxHeight"), doubleValue("420.5"));
        properties.put(name("minHeight"), doubleValue("24"));
        properties.put(name("maxWidth"), doubleValue("640.25"));
        properties.put(name("minWidth"), doubleValue("32"));
        properties.put(name("alignment"), new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                new BigDecimal("0.75"), new BigDecimal("-0.25")));
        WidgetNode box = overflowBox(properties, WidgetSlot.SingleSlot.of(text("Inside")));

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(box), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const OverflowBox("), build);
        assertTrue(build.contains(
                "alignment: const AlignmentDirectional(0.75, -0.25)"), build);
        assertTrue(build.contains("minWidth: 32.0"), build);
        assertTrue(build.contains("maxWidth: 640.25"), build);
        assertTrue(build.contains("minHeight: 24.0"), build);
        assertTrue(build.contains("maxHeight: 420.5"), build);
        assertTrue(build.contains(
                "fit: _nbfd_17010c97c53e.OverflowBoxFit.deferToChild"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
        assertOrdered(build,
                "alignment:", "minWidth:", "maxWidth:", "minHeight:",
                "maxHeight:", "fit:", "child:");
    }

    @Test
    void omittedArgumentsStayOmittedSoFlutterOwnsEveryDefault() {
        WidgetNode box = overflowBox(Map.of(), WidgetSlot.SingleSlot.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(box), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("return const OverflowBox("), build);
        assertTrue(build.contains("child: null"), build);
        for (String argument : List.of(
                "alignment:", "minWidth:", "maxWidth:", "minHeight:",
                "maxHeight:", "fit:")) {
            assertFalse(build.contains(argument), build);
        }
    }

    @Test
    void physicalAndDirectionalAlignmentsAndBothFitValuesGenerateExactly() {
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            for (String fit : List.of("max", "deferToChild")) {
                WidgetNode box = overflowBox(
                        Map.of(
                                name("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        basis, BigDecimal.ONE.negate(), BigDecimal.ONE),
                                name("fit"), enumValue("OverflowBoxFit", fit)),
                        WidgetSlot.SingleSlot.empty());

                DartGenerationResult result = new DartRegionGenerator().generate(
                        document(box), BuiltInWidgetCatalog.getDefault());

                assertTrue(result.successful(), () -> basis + "/" + fit + ": "
                        + result.diagnostics());
                String build = result.generated().orElseThrow().build().payload();
                String alignmentType = basis
                        == PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL
                                ? "Alignment" : "AlignmentDirectional";
                assertTrue(build.contains(
                        "alignment: const " + alignmentType + "(-1.0, 1.0)"), build);
                assertTrue(build.contains(
                        "fit: _nbfd_17010c97c53e.OverflowBoxFit." + fit), build);
            }
        }
    }

    @Test
    void mapInsertionOrderCannotChangeGeneratedDart() {
        LinkedHashMap<PropertyName, PropertyValue> forward = new LinkedHashMap<>();
        forward.put(name("minWidth"), doubleValue("10"));
        forward.put(name("maxWidth"), doubleValue("20"));
        forward.put(name("fit"), enumValue("OverflowBoxFit", "max"));
        LinkedHashMap<PropertyName, PropertyValue> reverse = new LinkedHashMap<>();
        reverse.put(name("fit"), enumValue("OverflowBoxFit", "max"));
        reverse.put(name("maxWidth"), doubleValue("20"));
        reverse.put(name("minWidth"), doubleValue("10"));

        DartGenerationResult first = new DartRegionGenerator().generate(
                document(overflowBox(forward, WidgetSlot.SingleSlot.empty())),
                BuiltInWidgetCatalog.getDefault());
        DartGenerationResult second = new DartRegionGenerator().generate(
                document(overflowBox(reverse, WidgetSlot.SingleSlot.empty())),
                BuiltInWidgetCatalog.getDefault());

        assertTrue(first.successful(), () -> first.diagnostics().toString());
        assertTrue(second.successful(), () -> second.diagnostics().toString());
        assertEquals(first.generated().orElseThrow().imports().payload(),
                second.generated().orElseThrow().imports().payload());
        assertEquals(first.generated().orElseThrow().build().payload(),
                second.generated().orElseThrow().build().payload());
    }

    @Test
    void invalidKindsRangesEnumsAndCrossAxisBoundsNeverReachDartSource() {
        List<Map<PropertyName, PropertyValue>> invalidProperties = List.of(
                Map.of(name("minWidth"),
                        new PropertyValue.IntegerValue(BigInteger.ONE)),
                Map.of(name("maxHeight"), doubleValue("-0.5")),
                Map.of(name("minHeight"), doubleValue("1E+309")),
                Map.of(name("fit"), enumValue("OverflowBoxFit", "invalid")),
                Map.of(name("minWidth"), doubleValue("80"),
                        name("maxWidth"), doubleValue("40")),
                Map.of(name("minHeight"), doubleValue("48"),
                        name("maxHeight"), doubleValue("24")));

        for (Map<PropertyName, PropertyValue> invalid : invalidProperties) {
            DartGenerationResult result = new DartRegionGenerator().generate(
                    document(overflowBox(invalid, WidgetSlot.SingleSlot.empty())),
                    BuiltInWidgetCatalog.getDefault());

            assertFalse(result.successful(), invalid.toString());
            assertTrue(result.generated().isEmpty(), invalid.toString());
        }
    }

    private static void assertOrdered(String text, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int current = text.indexOf(fragment);
            assertTrue(current > previous, () -> fragment + " is out of order in " + text);
            previous = current;
        }
    }

    private static WidgetNode overflowBox(
            Map<PropertyName, PropertyValue> properties,
            WidgetSlot.SingleSlot child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.OverflowBox"),
                properties,
                Map.of(new SlotName("child"), child),
                Extensions.empty());
    }

    private static PropertyValue.DoubleValue doubleValue(String value) {
        return new PropertyValue.DoubleValue(new BigDecimal(value));
    }

    private static PropertyValue.EnumValue enumValue(String type, String value) {
        return new PropertyValue.EnumValue(type, value);
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
}
