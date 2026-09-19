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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlexibleDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final String RENDERING_PREFIX = "_nbfd_17010c97c53e.";

    @Test
    void omittedPropertiesRemainOmittedAndFlutterOwnsBothDefaults() {
        WidgetNode flexible = flexible(
                Map.of(), text("Default child"));

        DartGenerationResult result = generateInRow(flexible);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("const Flexible("), build);
        assertTrue(build.contains("child: const Text('Default child')"), build);
        assertFalse(build.contains("flex:"), build);
        assertFalse(build.contains("fit:"), build);
    }

    @Test
    void emitsZeroAndPortableMaximumFlexWithBothReviewedFitsInConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> looseProperties =
                new LinkedHashMap<>();
        looseProperties.put(name("flex"),
                new PropertyValue.IntegerValue(BigInteger.ZERO));
        looseProperties.put(name("fit"), enumValue("FlexFit", "loose"));
        WidgetNode loose = flexible(looseProperties, text("Loose"));

        LinkedHashMap<PropertyName, PropertyValue> tightProperties =
                new LinkedHashMap<>();
        tightProperties.put(name("flex"), new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER));
        tightProperties.put(name("fit"), enumValue("FlexFit", "tight"));
        WidgetNode tight = flexible(tightProperties, text("Tight"));

        DartGenerationResult result = generateInRow(loose, tight);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertEquals(2, occurrences(build, "const Flexible("), build);
        assertTrue(build.contains("flex: 0"), build);
        assertTrue(build.contains("fit: " + RENDERING_PREFIX + "FlexFit.loose"), build);
        assertTrue(build.contains(
                "flex: " + DartNumericLiterals.MAX_PORTABLE_INTEGER), build);
        assertTrue(build.contains("fit: " + RENDERING_PREFIX + "FlexFit.tight"), build);
        int firstFlexible = build.indexOf("const Flexible(");
        int firstFlex = build.indexOf("flex:", firstFlexible);
        int firstFit = build.indexOf("fit:", firstFlexible);
        int firstChild = build.indexOf("child:", firstFlexible);
        assertTrue(firstFlex < firstFit, build);
        assertTrue(firstFit < firstChild, build);
    }

    @Test
    void requiredChildCannotBeOmittedFromGeneratedDart() {
        WidgetNode incomplete = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Flexible"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        DartGenerationResult result = generateInRow(incomplete);

        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("Flexible")
                && diagnostic.message().contains("child")),
                () -> result.diagnostics().toString());
    }

    private static DartGenerationResult generateInRow(WidgetNode... children) {
        WidgetNode row = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Row"),
                Map.of(),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(children))),
                Extensions.empty());
        return new DartRegionGenerator().generate(
                document(row), BuiltInWidgetCatalog.getDefault());
    }

    private static WidgetNode flexible(
            Map<PropertyName, PropertyValue> properties,
            WidgetNode child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Flexible"),
                properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)),
                Extensions.empty());
    }

    private static PropertyValue.EnumValue enumValue(String type, String value) {
        return new PropertyValue.EnumValue(type, value);
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
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

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
