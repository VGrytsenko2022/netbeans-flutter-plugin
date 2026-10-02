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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexedStackDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsConstPrototypeWithoutMaterializingAnyFlutterDefaults() {
        GeneratedDartRegions first = generate(indexedStack(Map.of(), List.of()));
        GeneratedDartRegions second = generate(indexedStack(Map.of(), List.of()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const IndexedStack("), build);
        assertFalse(build.contains("alignment:"), build);
        assertFalse(build.contains("textDirection:"), build);
        assertFalse(build.contains("clipBehavior:"), build);
        assertFalse(build.contains("sizing:"), build);
        assertFalse(build.contains("index:"), build);
        assertTrue(build.contains("children: []"), build);
    }

    @Test
    void emitsAllArgumentsInPinnedConstructorOrderAndPreservesExplicitNull() {
        WidgetNode root = indexedStack(Map.of(
                name("alignment"), new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("0.25"), new BigDecimal("-0.5")),
                name("textDirection"), enumValue("TextDirection", "rtl"),
                name("clipBehavior"), enumValue("Clip", "antiAlias"),
                name("sizing"), enumValue("StackFit", "expand"),
                name("index"), new PropertyValue.NullValue()),
                List.of(text("First"), text("Second")));

        String build = generate(root).build().payload();
        assertTrue(build.contains("return const IndexedStack("), build);
        assertTrue(build.contains("alignment: const AlignmentDirectional(0.25, -0.5)"), build);
        assertTrue(build.contains("textDirection: TextDirection.rtl"), build);
        assertTrue(build.contains("clipBehavior: Clip.antiAlias"), build);
        assertTrue(build.contains("sizing: StackFit.expand"), build);
        assertTrue(build.contains("index: null"), build);
        assertTrue(build.contains("const Text('First')"), build);
        assertTrue(build.contains("const Text('Second')"), build);
        assertOrdered(build,
                "alignment:", "textDirection:", "clipBehavior:",
                "sizing:", "index:", "children:");
        assertTrue(build.indexOf("const Text('First')")
                < build.indexOf("const Text('Second')"), build);
    }

    private static void assertOrdered(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token);
            assertTrue(current > previous, text);
            previous = current;
        }
    }

    private static WidgetNode indexedStack(
            Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> children) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.IndexedStack"),
                properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)),
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

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static PropertyValue.EnumValue enumValue(String type, String value) {
        return new PropertyValue.EnumValue(type, value);
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
