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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsReviewedVisiblePrototypeDefaultsBeforeTheOptionalChild() {
        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>();
        properties.put(name("baseline"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(24)));
        properties.put(name("baselineType"),
                new PropertyValue.EnumValue("TextBaseline", "alphabetic"));

        String build = generate(baseline(properties, Optional.empty()));

        assertTrue(build.contains("return const Baseline("), build);
        assertTrue(build.contains("baseline: 24.0"), build);
        assertTrue(build.contains(
                "baselineType: TextBaseline.alphabetic"), build);
        assertTrue(build.contains("child: null"), build);
        assertTrue(build.indexOf("baseline:")
                < build.indexOf("baselineType:"), build);
        assertTrue(build.indexOf("baselineType:")
                < build.indexOf("child:"), build);
    }

    @Test
    void emitsSignedFiniteDistanceIdeographicTypeAndChild() {
        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>();
        properties.put(name("baseline"),
                new PropertyValue.DoubleValue(new BigDecimal("-12.5")));
        properties.put(name("baselineType"),
                new PropertyValue.EnumValue("TextBaseline", "ideographic"));

        String build = generate(baseline(
                properties, Optional.of(text("Inside"))));

        assertTrue(build.contains("return const Baseline("), build);
        assertTrue(build.contains("baseline: -12.5"), build);
        assertTrue(build.contains(
                "baselineType: TextBaseline.ideographic"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
    }

    @Test
    void refusesMissingRequiredBaselineArgumentsBeforeDartEmission() {
        WidgetNode missingDistance = baseline(
                Map.of(name("baselineType"),
                        new PropertyValue.EnumValue(
                                "TextBaseline", "alphabetic")),
                Optional.empty());
        WidgetNode missingType = baseline(
                Map.of(name("baseline"),
                        new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                Optional.empty());

        DartGenerationResult distanceResult = new DartRegionGenerator().generate(
                document(missingDistance), BuiltInWidgetCatalog.getDefault());
        DartGenerationResult typeResult = new DartRegionGenerator().generate(
                document(missingType), BuiltInWidgetCatalog.getDefault());

        assertFalse(distanceResult.successful());
        assertTrue(distanceResult.generated().isEmpty());
        assertTrue(distanceResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("baseline")),
                () -> distanceResult.diagnostics().toString());
        assertFalse(typeResult.successful());
        assertTrue(typeResult.generated().isEmpty());
        assertTrue(typeResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("baselineType")),
                () -> typeResult.diagnostics().toString());
    }

    private static WidgetNode baseline(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Baseline"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
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

    private static String generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow().build().payload();
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
}
