package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipOvalDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsDeterministicConstConstructorWithFrameworkDefaultsAndEmptyChild() {
        GeneratedDartRegions first = generate(clipOval(Map.of(), Optional.empty()));
        GeneratedDartRegions second = generate(clipOval(Map.of(), Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const ClipOval("), build);
        assertFalse(build.contains("clipBehavior:"), build);
        assertFalse(build.contains("clipper:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void emitsEveryClosedClipBehaviorBeforeTheOptionalConstChild() {
        for (String value : new String[] {
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"}) {
            GeneratedDartRegions generated = generate(clipOval(
                    Map.of(new PropertyName("clipBehavior"),
                            new PropertyValue.EnumValue("Clip", value)),
                    Optional.of(text("Visible"))));

            String build = generated.build().payload();
            String behavior = "clipBehavior: Clip." + value + ',';
            assertTrue(build.contains("return const ClipOval("), build);
            assertTrue(build.contains(behavior), build);
            assertTrue(build.contains("child: const Text('Visible')"), build);
            assertTrue(build.indexOf(behavior)
                    < build.indexOf("child: const Text('Visible')"), build);
            assertFalse(build.contains("clipper:"), build);
        }
    }

    @Test
    void removesOnlyTheOuterConstWhenTheChildIsNotConstCapable() {
        WidgetNode container = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Container"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        String build = generate(clipOval(Map.of(), Optional.of(container)))
                .build().payload();

        assertTrue(build.contains("return ClipOval("), build);
        assertFalse(build.contains("return const ClipOval("), build);
        assertTrue(build.contains("child: Container("), build);
    }

    private static WidgetNode clipOval(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ClipOval"),
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
