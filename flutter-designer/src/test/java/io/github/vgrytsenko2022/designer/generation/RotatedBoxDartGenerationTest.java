package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.DartNumericLiterals;
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
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotatedBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void deterministicallyEmitsOneQuarterTurnAndExplicitEmptyOptionalChild() {
        GeneratedDartRegions first = generate(rotatedBox(
                BigInteger.ONE, Optional.empty()));
        GeneratedDartRegions second = generate(rotatedBox(
                BigInteger.ONE, Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const RotatedBox("), build);
        assertTrue(build.contains("quarterTurns: 1"), build);
        assertTrue(build.contains("child: null"), build);
        assertTrue(build.indexOf("quarterTurns: 1")
                < build.indexOf("child: null"), build);
    }

    @Test
    void preservesSignedPortableTurnsAndOrdinaryChildInConstructorOrder() {
        GeneratedDartRegions negative = generate(rotatedBox(
                BigInteger.valueOf(-3), Optional.of(text("Inside"))));
        String negativeBuild = negative.build().payload();
        assertTrue(negativeBuild.contains("return const RotatedBox("),
                negativeBuild);
        assertTrue(negativeBuild.contains("quarterTurns: -3"), negativeBuild);
        assertTrue(negativeBuild.contains("child: const Text('Inside')"),
                negativeBuild);
        assertTrue(negativeBuild.indexOf("quarterTurns: -3")
                < negativeBuild.indexOf("child: const Text('Inside')"),
                negativeBuild);

        String maximumBuild = generate(rotatedBox(
                DartNumericLiterals.MAX_PORTABLE_INTEGER,
                Optional.empty())).build().payload();
        assertTrue(maximumBuild.contains(
                "quarterTurns: " + DartNumericLiterals.MAX_PORTABLE_INTEGER),
                maximumBuild);
    }

    private static WidgetNode rotatedBox(
            BigInteger quarterTurns,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.RotatedBox"),
                Map.of(new PropertyName("quarterTurns"),
                        new PropertyValue.IntegerValue(quarterTurns)),
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
