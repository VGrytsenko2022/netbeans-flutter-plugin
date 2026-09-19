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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntrinsicWidthDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsDeterministicConstConstructorWithNullableStepsAndEmptyChild() {
        GeneratedDartRegions first = generate(intrinsicWidth(
                Map.of(), Optional.empty()));
        GeneratedDartRegions second = generate(intrinsicWidth(
                Map.of(), Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const IntrinsicWidth("), build);
        assertFalse(build.contains("stepWidth:"), build);
        assertFalse(build.contains("stepHeight:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void emitsExactNamedArgumentOrderAndPreservesZeroWithOrdinaryChild() {
        LinkedHashMap<PropertyName, PropertyValue> reverseInsertion =
                new LinkedHashMap<>();
        reverseInsertion.put(
                new PropertyName("stepHeight"),
                new PropertyValue.DoubleValue(new BigDecimal("12.5")));
        reverseInsertion.put(
                new PropertyName("stepWidth"),
                new PropertyValue.DoubleValue(BigDecimal.ZERO));

        GeneratedDartRegions generated = generate(intrinsicWidth(
                reverseInsertion, Optional.of(text("Inside"))));
        String build = generated.build().payload();

        assertTrue(build.contains("return const IntrinsicWidth("), build);
        assertTrue(build.contains("stepWidth: 0.0"), build);
        assertTrue(build.contains("stepHeight: 12.5"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
        assertTrue(build.indexOf("stepWidth: 0.0")
                < build.indexOf("stepHeight: 12.5"), build);
        assertTrue(build.indexOf("stepHeight: 12.5")
                < build.indexOf("child: const Text('Inside')"), build);
    }

    private static WidgetNode intrinsicWidth(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.IntrinsicWidth"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
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
}
