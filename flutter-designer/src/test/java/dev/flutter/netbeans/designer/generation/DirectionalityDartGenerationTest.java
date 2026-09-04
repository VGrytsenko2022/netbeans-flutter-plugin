package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DirectionalityWidgetPropertySchema;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionalityDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void reviewedLtrCreationValueAndRequiredChildEmitConstConstructor() {
        WidgetNode directionality = directionality(
                Map.of(name("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "ltr")),
                Optional.of(text("Content")));

        GeneratedDartRegions generated = generateSuccessful(directionality);

        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const Directionality("), build);
        assertTrue(build.contains("textDirection: TextDirection.ltr"), build);
        assertTrue(build.contains("child: const Text('Content')"), build);
        assertOrdered(build, "textDirection:", "child:");
    }

    @Test
    void rtlUsesTheExactPinnedEnumSymbolAndRemainsConst() {
        WidgetNode directionality = directionality(
                Map.of(name("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl")),
                Optional.of(text("RTL")));

        String build = generateSuccessful(directionality).build().payload();

        assertTrue(build.contains("return const Directionality("), build);
        assertTrue(build.contains("textDirection: TextDirection.rtl"), build);
        assertFalse(build.contains("TextDirection.ltr"), build);
    }

    @Test
    void missingDirectionMissingChildAndWrongEnumFailBeforeDartEmission() {
        DartGenerationResult missingDirection = generate(directionality(
                Map.of(), Optional.of(text("Content"))));
        DartGenerationResult missingChild = generate(directionality(
                Map.of(name("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "ltr")),
                Optional.empty()));
        DartGenerationResult wrongEnum = generate(directionality(
                Map.of(name("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "up")),
                Optional.of(text("Content"))));

        assertFalse(missingDirection.successful());
        assertTrue(missingDirection.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/properties/textDirection")),
                () -> missingDirection.diagnostics().toString());
        assertFalse(missingChild.successful());
        assertTrue(missingChild.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/slots/child")),
                () -> missingChild.diagnostics().toString());
        assertFalse(wrongEnum.successful());
        assertTrue(wrongEnum.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.path().contains("/properties/textDirection")),
                () -> wrongEnum.diagnostics().toString());
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

    private static WidgetNode directionality(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE,
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
