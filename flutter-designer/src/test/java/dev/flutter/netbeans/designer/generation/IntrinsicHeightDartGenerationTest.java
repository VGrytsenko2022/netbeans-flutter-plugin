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
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntrinsicHeightDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsDeterministicConstConstructorWithExplicitEmptyOptionalChild() {
        GeneratedDartRegions first = generate(intrinsicHeight(Optional.empty()));
        GeneratedDartRegions second = generate(intrinsicHeight(Optional.empty()));

        assertEquals(first.imports(), second.imports(),
                "Stable ids cannot affect deterministic import bytes");
        assertEquals(first.build(), second.build(),
                "Stable ids cannot affect deterministic build bytes");
        assertEquals(first.importPlan(), second.importPlan(),
                "Stable ids cannot affect the deterministic import plan");
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        assertTrue(first.build().payload().contains(
                "return const IntrinsicHeight("), first.build().payload());
        assertTrue(first.build().payload().contains(
                "child: null"), first.build().payload());
    }

    @Test
    void emitsConstIntrinsicHeightWithOrdinaryChild() {
        GeneratedDartRegions generated = generate(intrinsicHeight(
                Optional.of(text("Inside"))));

        assertTrue(generated.build().payload().contains(
                "return const IntrinsicHeight("), generated.build().payload());
        assertTrue(generated.build().payload().contains(
                "child: const Text('Inside')"), generated.build().payload());
    }

    private static WidgetNode intrinsicHeight(Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.IntrinsicHeight"),
                Map.of(),
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
