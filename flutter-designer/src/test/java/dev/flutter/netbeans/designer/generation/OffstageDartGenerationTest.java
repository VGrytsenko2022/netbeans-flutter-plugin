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

class OffstageDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void deterministicallyOmitsFrameworkDefaultAndKeepsEmptyOptionalChild() {
        GeneratedDartRegions first = generate(offstage(Map.of(), Optional.empty()));
        GeneratedDartRegions second = generate(offstage(Map.of(), Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const Offstage("), build);
        assertFalse(build.contains("offstage:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void deterministicallyPreservesExplicitTrueFalseAndConstructorOrder() {
        GeneratedDartRegions trueFirst = generate(offstage(
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(true)),
                Optional.empty()));
        GeneratedDartRegions trueSecond = generate(offstage(
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(true)),
                Optional.empty()));
        assertEquals(trueFirst.build(), trueSecond.build());
        assertTrue(trueFirst.build().payload().contains("offstage: true"),
                trueFirst.build().payload());
        assertTrue(trueFirst.build().payload().contains("child: null"),
                trueFirst.build().payload());

        GeneratedDartRegions falseFirst = generate(offstage(
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(false)),
                Optional.of(text("Visible child"))));
        GeneratedDartRegions falseSecond = generate(offstage(
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(false)),
                Optional.of(text("Visible child"))));
        assertEquals(falseFirst.imports(), falseSecond.imports());
        assertEquals(falseFirst.build(), falseSecond.build());
        assertEquals(falseFirst.importPlan(), falseSecond.importPlan());
        String build = falseFirst.build().payload();
        assertTrue(build.contains("return const Offstage("), build);
        assertTrue(build.contains("offstage: false"), build);
        assertTrue(build.contains("child: const Text('Visible child')"), build);
        assertTrue(build.indexOf("offstage: false")
                < build.indexOf("child: const Text('Visible child')"), build);
    }

    private static WidgetNode offstage(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Offstage"),
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
