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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitedBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsEveryReviewedArgumentInExactFlutterConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("maxHeight"),
                new PropertyValue.DoubleValue(new BigDecimal("180.25")));
        properties.put(name("maxWidth"),
                new PropertyValue.DoubleValue(new BigDecimal("320.5")));
        WidgetNode box = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.LimitedBox"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(
                        Optional.of(text("Inside")))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(box), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const LimitedBox("), build);
        assertTrue(build.contains("maxWidth: 320.5"), build);
        assertTrue(build.contains("maxHeight: 180.25"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
        assertTrue(build.indexOf("maxWidth:") < build.indexOf("maxHeight:"), build);
        assertTrue(build.indexOf("maxHeight:") < build.indexOf("child:"), build);
    }

    @Test
    void omittedDimensionsStayOmittedSoFlutterOwnsInfinityDefaults() {
        WidgetNode box = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.LimitedBox"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(box), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("return const LimitedBox("), build);
        assertFalse(build.contains("maxWidth:"), build);
        assertFalse(build.contains("maxHeight:"), build);
        assertFalse(build.contains("double.infinity"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void invalidDimensionKindsAndRangesNeverReachDartSource() {
        for (Map.Entry<String, PropertyValue> invalid : Map.<String, PropertyValue>of(
                "integer", new PropertyValue.IntegerValue(java.math.BigInteger.ONE),
                "negative", new PropertyValue.DoubleValue(
                        new BigDecimal("-0.5")),
                "unrepresentable", new PropertyValue.DoubleValue(
                        new BigDecimal("1E+309"))).entrySet()) {
            WidgetNode box = new WidgetNode(
                    StableId.random(),
                    new WidgetTypeId("flutter.widgets.LimitedBox"),
                    Map.of(name("maxWidth"), invalid.getValue()),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                    Extensions.empty());

            DartGenerationResult result = new DartRegionGenerator().generate(
                    document(box), BuiltInWidgetCatalog.getDefault());

            assertFalse(result.successful(), invalid.getKey());
            assertTrue(result.generated().isEmpty(), invalid.getKey());
        }
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
