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
