package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.SafeAreaWidgetPropertySchema;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeAreaDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void omittedFrameworkDefaultsEmitOnlyRequiredChildAndRemainConst() {
        WidgetNode safeArea = safeArea(Map.of(), Optional.of(text("Content")));

        GeneratedDartRegions generated = generateSuccessful(safeArea);

        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const SafeArea("), build);
        assertFalse(build.contains("left:"), build);
        assertFalse(build.contains("top:"), build);
        assertFalse(build.contains("right:"), build);
        assertFalse(build.contains("bottom:"), build);
        assertFalse(build.contains("minimum:"), build);
        assertFalse(build.contains("maintainBottomViewPadding:"), build);
        assertTrue(build.contains("child: const Text('Content')"), build);
    }

    @Test
    void completeSignedPhysicalInsetsSurfaceEmitsInConstructorOrder() {
        WidgetNode safeArea = safeArea(
                Map.ofEntries(
                        Map.entry(name("left"), new PropertyValue.BooleanValue(false)),
                        Map.entry(name("top"), new PropertyValue.BooleanValue(false)),
                        Map.entry(name("right"), new PropertyValue.BooleanValue(true)),
                        Map.entry(name("bottom"), new PropertyValue.BooleanValue(false)),
                        Map.entry(name("minimum"), new PropertyValue.EdgeInsetsValue(
                                BigDecimal.valueOf(-1), BigDecimal.valueOf(2),
                                BigDecimal.valueOf(-3), BigDecimal.valueOf(4))),
                        Map.entry(name("maintainBottomViewPadding"),
                                new PropertyValue.BooleanValue(true))),
                Optional.of(text("Protected")));

        String build = generateSuccessful(safeArea).build().payload();

        assertTrue(build.contains("return const SafeArea("), build);
        assertTrue(build.contains("left: false"), build);
        assertTrue(build.contains("top: false"), build);
        assertTrue(build.contains("right: true"), build);
        assertTrue(build.contains("bottom: false"), build);
        assertTrue(build.contains(
                "minimum: const EdgeInsets.fromLTRB(-1.0, 2.0, -3.0, 4.0)"), build);
        assertTrue(build.contains("maintainBottomViewPadding: true"), build);
        assertTrue(build.contains("child: const Text('Protected')"), build);
        assertOrdered(build,
                "left:", "top:", "right:", "bottom:", "minimum:",
                "maintainBottomViewPadding:", "child:");
    }

    @Test
    void incompleteChildAndDirectionalInsetsBothFailClosedBeforeDartEmission() {
        WidgetNode missingChild = safeArea(Map.of(), Optional.empty());
        WidgetNode directional = safeArea(
                Map.of(name("minimum"),
                        new PropertyValue.EdgeInsetsDirectionalValue(
                                BigDecimal.ONE, BigDecimal.TWO,
                                BigDecimal.valueOf(3), BigDecimal.valueOf(4))),
                Optional.of(text("Content")));

        DartGenerationResult missingResult = generate(missingChild);
        DartGenerationResult directionalResult = generate(directional);

        assertFalse(missingResult.successful());
        assertTrue(missingResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/slots/child")),
                () -> missingResult.diagnostics().toString());
        assertFalse(directionalResult.successful());
        assertTrue(directionalResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/properties/minimum")),
                () -> directionalResult.diagnostics().toString());
    }

    private static GeneratedDartRegions generateSuccessful(WidgetNode root) {
        DartGenerationResult result = generate(root);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static DartGenerationResult generate(WidgetNode root) {
        return new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
    }

    private static WidgetNode safeArea(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE,
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

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(
                Optional.empty(), StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(
                                new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))),
                Optional.empty(), root, Extensions.empty());
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static void assertOrdered(String value, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int current = value.indexOf(needle);
            assertTrue(current > previous, needle + " is out of order:\n" + value);
            previous = current;
        }
    }
}
