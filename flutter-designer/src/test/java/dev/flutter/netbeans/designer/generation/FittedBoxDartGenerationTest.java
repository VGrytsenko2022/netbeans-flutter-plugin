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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FittedBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsEveryReviewedArgumentInExactConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("fit"), enumValue("BoxFit", "cover"));
        properties.put(name("alignment"), new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                new BigDecimal("1.5"), new BigDecimal("-0.5")));
        properties.put(name("clipBehavior"), enumValue("Clip", "hardEdge"));
        WidgetNode fittedBox = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.FittedBox"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(
                        Optional.of(text("Inside")))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(fittedBox), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const FittedBox("), build);
        assertTrue(build.contains("fit: BoxFit.cover"), build);
        assertTrue(build.contains(
                "alignment: const AlignmentDirectional(1.5, -0.5)"), build);
        assertTrue(build.contains("clipBehavior: Clip.hardEdge"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
        assertTrue(build.indexOf("fit:") < build.indexOf("alignment:"), build);
        assertTrue(build.indexOf("alignment:") < build.indexOf("clipBehavior:"), build);
        assertTrue(build.indexOf("clipBehavior:") < build.indexOf("child:"), build);
    }

    @Test
    void everyReviewedBoxFitValueGeneratesWithoutAnEscapeHatch() {
        for (String fit : List.of(
                "fill", "contain", "cover", "fitWidth", "fitHeight", "none",
                "scaleDown")) {
            WidgetNode fittedBox = new WidgetNode(
                    StableId.random(),
                    new WidgetTypeId("flutter.widgets.FittedBox"),
                    Map.of(name("fit"), enumValue("BoxFit", fit)),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                    Extensions.empty());

            DartGenerationResult result = new DartRegionGenerator().generate(
                    document(fittedBox), BuiltInWidgetCatalog.getDefault());

            assertTrue(result.successful(),
                    () -> fit + ": " + result.diagnostics());
            assertTrue(result.generated().orElseThrow().build().payload()
                    .contains("fit: BoxFit." + fit), fit);
        }
    }

    @Test
    void omittedArgumentsRemainOmittedSoFlutterOwnsEveryDefault() {
        WidgetNode fittedBox = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.FittedBox"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(fittedBox), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("return const FittedBox("), build);
        assertTrue(build.contains("child: null"), build);
        for (String argument : List.of("fit:", "alignment:", "clipBehavior:")) {
            assertFalse(build.contains(argument), build);
        }
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
}
