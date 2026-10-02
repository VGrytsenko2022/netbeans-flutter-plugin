package io.github.vgrytsenko2022.designer.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SizedOverflowBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void deterministicallyEmitsRequiredCreationSizeAndOmitsFrameworkDefaults() {
        WidgetNode root = sizedOverflowBox(
                new PropertyValue.SizeValue(
                        BigDecimal.valueOf(100), BigDecimal.valueOf(100)),
                Optional.empty(), Optional.empty());

        GeneratedDartRegions first = generate(root);
        GeneratedDartRegions second = generate(root);

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const SizedOverflowBox("), build);
        assertTrue(build.contains("size: Size(100.0, 100.0)"), build);
        assertFalse(build.contains("alignment:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void deterministicallyEmitsCustomSizeAlignmentAndChildInConstructorOrder() {
        PropertyValue.AlignmentGeometryValue alignment =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE, BigDecimal.ONE.negate());
        WidgetNode root = sizedOverflowBox(
                new PropertyValue.SizeValue(
                        new BigDecimal("120.5"), BigDecimal.ZERO),
                Optional.of(alignment),
                Optional.of(text("Overflow child")));

        GeneratedDartRegions first = generate(root);
        GeneratedDartRegions second = generate(root);

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        String build = first.build().payload();
        assertTrue(build.contains("size: Size(120.5, 0.0)"), build);
        assertTrue(build.contains(
                "alignment: const AlignmentDirectional(1.0, -1.0)"), build);
        assertTrue(build.contains("child: const Text('Overflow child')"), build);
        assertTrue(build.indexOf("size: Size(120.5, 0.0)")
                < build.indexOf("alignment: const AlignmentDirectional"), build);
        assertTrue(build.indexOf("alignment: const AlignmentDirectional")
                < build.indexOf("child: const Text('Overflow child')"), build);
    }

    private static WidgetNode sizedOverflowBox(
            PropertyValue.SizeValue size,
            Optional<PropertyValue.AlignmentGeometryValue> alignment,
            Optional<WidgetNode> child) {
        java.util.LinkedHashMap<PropertyName, PropertyValue> properties =
                new java.util.LinkedHashMap<>();
        properties.put(new PropertyName("size"), size);
        alignment.ifPresent(value -> properties.put(
                new PropertyName("alignment"), value));
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.SizedOverflowBox"),
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
