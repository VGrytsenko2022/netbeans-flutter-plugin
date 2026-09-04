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

class ExcludeSemanticsDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsDeterministicConstConstructorWithOmittedTrueDefaultAndEmptyChild() {
        GeneratedDartRegions first = generate(excludeSemantics(
                Map.of(), Optional.empty()));
        GeneratedDartRegions second = generate(excludeSemantics(
                Map.of(), Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const ExcludeSemantics("), build);
        assertFalse(build.contains("excluding:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void emitsExplicitFalseBeforeConstOrdinaryChild() {
        GeneratedDartRegions generated = generate(excludeSemantics(
                Map.of(new PropertyName("excluding"),
                        new PropertyValue.BooleanValue(false)),
                Optional.of(text("Announced"))));

        String build = generated.build().payload();
        assertTrue(build.contains("return const ExcludeSemantics("), build);
        assertTrue(build.contains("excluding: false,"), build);
        assertTrue(build.contains(
                "child: const Text('Announced')"), build);
        assertTrue(build.indexOf("excluding: false,")
                < build.indexOf("child: const Text('Announced')"), build);
    }

    private static WidgetNode excludeSemantics(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ExcludeSemantics"),
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
