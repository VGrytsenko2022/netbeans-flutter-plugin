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

class OverflowBarDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void deterministicallyPreservesFlutterDefaultsAndExplicitEmptyChildren() {
        GeneratedDartRegions first = generate(overflowBar(Map.of(), List.of()));
        GeneratedDartRegions second = generate(overflowBar(Map.of(), List.of()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const OverflowBar("), build);
        for (String property : List.of(
                "spacing:", "alignment:", "overflowSpacing:",
                "overflowAlignment:", "overflowDirection:", "textDirection:")) {
            assertFalse(build.contains(property), property + " in " + build);
        }
        assertTrue(build.contains("children: []"), build);
        assertFalse(build.contains("LayoutBuilder("), build);
        assertFalse(build.contains("SingleChildScrollView("), build);
    }

    @Test
    void emitsFullConfiguredSurfaceAndOrderedChildrenInConstructorOrder() {
        WidgetNode root = overflowBar(Map.of(
                name("spacing"), doubleValue("-3.5"),
                name("alignment"),
                enumValue("MainAxisAlignment", "spaceEvenly"),
                name("overflowSpacing"), doubleValue("8.25"),
                name("overflowAlignment"),
                enumValue("OverflowBarAlignment", "end"),
                name("overflowDirection"),
                enumValue("VerticalDirection", "up"),
                name("textDirection"), enumValue("TextDirection", "rtl")),
                List.of(text("First"), text("Second")));

        String build = generate(root).build().payload();
        assertTrue(build.contains("return const OverflowBar("), build);
        List<String> ordered = List.of(
                "spacing: -3.5",
                "alignment: MainAxisAlignment.spaceEvenly",
                "overflowSpacing: 8.25",
                "overflowAlignment: OverflowBarAlignment.end",
                "overflowDirection: VerticalDirection.up",
                "textDirection: TextDirection.rtl",
                "children: [",
                "const Text('First')",
                "const Text('Second')");
        int previous = -1;
        for (String token : ordered) {
            int current = build.indexOf(token);
            assertTrue(current > previous,
                    () -> token + " is out of constructor order in " + build);
            previous = current;
        }
        assertFalse(build.contains("LayoutBuilder("), build);
        assertFalse(build.contains("SingleChildScrollView("), build);
    }

    private static WidgetNode overflowBar(
            Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> children) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.OverflowBar"),
                properties,
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(children)),
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

    private static PropertyValue.DoubleValue doubleValue(String value) {
        return new PropertyValue.DoubleValue(new BigDecimal(value));
    }

    private static PropertyValue.EnumValue enumValue(String type, String value) {
        return new PropertyValue.EnumValue(type, value);
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
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
