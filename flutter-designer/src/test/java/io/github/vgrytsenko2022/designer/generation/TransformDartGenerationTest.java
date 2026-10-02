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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransformDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void deterministicallyEmitsIdentityTransformNewAndOmitsFrameworkDefaults() {
        WidgetNode root = transform(
                identityMatrix(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        GeneratedDartRegions first = generate(root);
        GeneratedDartRegions second = generate(root);

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return Transform("), build);
        assertFalse(build.contains("return const Transform("), build);
        assertTrue(build.contains(
                "transform: Matrix4.fromList(<double>["
                + "1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, "
                + "0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0])"), build);
        assertFalse(build.contains("origin:"), build);
        assertFalse(build.contains("alignment:"), build);
        assertFalse(build.contains("transformHitTests:"), build);
        assertFalse(build.contains("filterQuality:"), build);
        assertFalse(build.contains("child:"), build);
        assertFalse(build.contains("Transform.rotate"), build);
        assertFalse(build.contains("Transform.translate"), build);
        assertFalse(build.contains("Transform.scale"), build);
        assertFalse(build.contains("Transform.flip"), build);
        GeneratedDartSymbolOccurrence matrix4 = first.symbolOccurrences().stream()
                .filter(occurrence -> occurrence.symbolName().equals("Matrix4"))
                .findFirst()
                .orElseThrow();
        assertEquals("package:vector_math/vector_math_64.dart",
                matrix4.libraryUri(),
                "widgets.dart re-exports Matrix4 but analyzer navigation owns it in vector_math");
        assertEquals("""
                  @override
                  Widget build(BuildContext context) {
                    return Transform(
                      transform: Matrix4.fromList(<double>[1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0]),
                    );
                  }
                """, build);
    }

    @Test
    void deterministicallyEmitsCompleteSurfaceInTransformNewConstructorOrder() {
        PropertyValue.Matrix4Value matrix = new PropertyValue.Matrix4Value(List.of(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                new BigDecimal("12.5"), new BigDecimal("-8.25"),
                BigDecimal.ZERO, BigDecimal.ONE));
        WidgetNode root = transform(
                matrix,
                Optional.of(new PropertyValue.OffsetValue(
                        new BigDecimal("-4.5"), new BigDecimal("2.25"))),
                Optional.of(new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE, BigDecimal.ONE.negate())),
                Optional.of(new PropertyValue.BooleanValue(false)),
                Optional.of(new PropertyValue.EnumValue("FilterQuality", "high")),
                Optional.of(text("Transformed child")));

        GeneratedDartRegions first = generate(root);
        GeneratedDartRegions second = generate(root);

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        String build = first.build().payload();
        assertTrue(build.contains("return Transform("), build);
        assertTrue(build.contains("transform: Matrix4.fromList(<double>["), build);
        assertTrue(build.contains("const Offset(-4.5, 2.25)"), build);
        assertTrue(build.contains(
                "alignment: const AlignmentDirectional(1.0, -1.0)"), build);
        assertTrue(build.contains("transformHitTests: false"), build);
        assertTrue(build.contains("filterQuality: FilterQuality.high"), build);
        assertTrue(build.contains("child: const Text('Transformed child')"), build);

        int transformIndex = build.indexOf("transform: Matrix4.fromList");
        int originIndex = build.indexOf("origin: const Offset");
        int alignmentIndex = build.indexOf("alignment: const AlignmentDirectional");
        int hitTestsIndex = build.indexOf("transformHitTests: false");
        int qualityIndex = build.indexOf("filterQuality: FilterQuality.high");
        int childIndex = build.indexOf("child: const Text('Transformed child')");
        assertTrue(transformIndex < originIndex, build);
        assertTrue(originIndex < alignmentIndex, build);
        assertTrue(alignmentIndex < hitTestsIndex, build);
        assertTrue(hitTestsIndex < qualityIndex, build);
        assertTrue(qualityIndex < childIndex, build);
    }

    private static WidgetNode transform(
            PropertyValue.Matrix4Value matrix,
            Optional<PropertyValue.OffsetValue> origin,
            Optional<PropertyValue.AlignmentGeometryValue> alignment,
            Optional<PropertyValue.BooleanValue> transformHitTests,
            Optional<PropertyValue.EnumValue> filterQuality,
            Optional<WidgetNode> child) {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyName("transform"), matrix);
        origin.ifPresent(value -> properties.put(new PropertyName("origin"), value));
        alignment.ifPresent(value -> properties.put(new PropertyName("alignment"), value));
        transformHitTests.ifPresent(value -> properties.put(
                new PropertyName("transformHitTests"), value));
        filterQuality.ifPresent(value -> properties.put(
                new PropertyName("filterQuality"), value));
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Transform"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    private static PropertyValue.Matrix4Value identityMatrix() {
        return new PropertyValue.Matrix4Value(List.of(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
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
