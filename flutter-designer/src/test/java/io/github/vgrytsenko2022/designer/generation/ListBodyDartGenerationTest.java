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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListBodyDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void deterministicallyPreservesFlutterDefaultsAndExplicitEmptyChildren() {
        GeneratedDartRegions first = generate(listBody(Map.of(), List.of()));
        GeneratedDartRegions second = generate(listBody(Map.of(), List.of()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const ListBody("), build);
        assertFalse(build.contains("mainAxis:"), build);
        assertFalse(build.contains("reverse:"), build);
        assertTrue(build.contains("children: []"), build);
    }

    @Test
    void emitsConfiguredAxisReverseAndOrderedChildrenInConstructorOrder() {
        WidgetNode root = listBody(Map.of(
                name("mainAxis"), enumValue("Axis", "horizontal"),
                name("reverse"), new PropertyValue.BooleanValue(true)),
                List.of(text("First"), text("Second")));

        String build = generate(root).build().payload();
        assertTrue(build.contains("return const ListBody("), build);
        assertTrue(build.contains("mainAxis: Axis.horizontal"), build);
        assertTrue(build.contains("reverse: true"), build);
        assertTrue(build.contains("children: ["), build);
        assertTrue(build.contains("const Text('First')"), build);
        assertTrue(build.contains("const Text('Second')"), build);
        assertTrue(build.indexOf("mainAxis: Axis.horizontal")
                < build.indexOf("reverse: true"), build);
        assertTrue(build.indexOf("reverse: true")
                < build.indexOf("children: ["), build);
        assertTrue(build.indexOf("const Text('First')")
                < build.indexOf("const Text('Second')"), build);
    }

    private static WidgetNode listBody(
            Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> children) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ListBody"),
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
