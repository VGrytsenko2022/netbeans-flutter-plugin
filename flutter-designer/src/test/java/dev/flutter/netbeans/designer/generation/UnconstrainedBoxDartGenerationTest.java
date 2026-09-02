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

class UnconstrainedBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsEveryReviewedArgumentInExactCatalogOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("textDirection"), enumValue("TextDirection", "rtl"));
        properties.put(name("alignment"), new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                new BigDecimal("1.5"), new BigDecimal("-0.5")));
        properties.put(name("constrainedAxis"), enumValue("Axis", "horizontal"));
        properties.put(name("clipBehavior"),
                enumValue("Clip", "antiAliasWithSaveLayer"));
        WidgetNode box = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.UnconstrainedBox"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(
                        Optional.of(text("Inside")))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(box), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const UnconstrainedBox("), build);
        assertTrue(build.contains("textDirection: TextDirection.rtl"), build);
        assertTrue(build.contains(
                "alignment: const AlignmentDirectional(1.5, -0.5)"), build);
        assertTrue(build.contains("constrainedAxis: Axis.horizontal"), build);
        assertTrue(build.contains(
                "clipBehavior: Clip.antiAliasWithSaveLayer"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
        assertTrue(build.indexOf("child:") < build.indexOf("textDirection:"), build);
        assertTrue(build.indexOf("textDirection:") < build.indexOf("alignment:"), build);
        assertTrue(build.indexOf("alignment:") < build.indexOf("constrainedAxis:"), build);
        assertTrue(build.indexOf("constrainedAxis:") < build.indexOf("clipBehavior:"), build);
    }

    @Test
    void everyReviewedEnumValueGeneratesWithoutAnEscapeHatch() {
        Map<String, List<String>> values = Map.of(
                "textDirection", List.of("rtl", "ltr"),
                "constrainedAxis", List.of("horizontal", "vertical"),
                "clipBehavior", List.of(
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        Map<String, String> types = Map.of(
                "textDirection", "TextDirection",
                "constrainedAxis", "Axis",
                "clipBehavior", "Clip");
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            for (String value : entry.getValue()) {
                WidgetNode box = new WidgetNode(
                        StableId.random(),
                        new WidgetTypeId("flutter.widgets.UnconstrainedBox"),
                        Map.of(name(entry.getKey()),
                                enumValue(types.get(entry.getKey()), value)),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                        Extensions.empty());

                DartGenerationResult result = new DartRegionGenerator().generate(
                        document(box), BuiltInWidgetCatalog.getDefault());

                assertTrue(result.successful(),
                        () -> entry.getKey() + '=' + value + ": "
                        + result.diagnostics());
                assertTrue(result.generated().orElseThrow().build().payload()
                        .contains(entry.getKey() + ": "
                                + types.get(entry.getKey()) + '.' + value),
                        entry.getKey() + '=' + value);
            }
        }
    }

    @Test
    void omittedArgumentsRemainOmittedSoFlutterOwnsEveryDefault() {
        WidgetNode box = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.UnconstrainedBox"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(box), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("return const UnconstrainedBox("), build);
        assertTrue(build.contains("child: null"), build);
        for (String argument : List.of(
                "textDirection:", "alignment:", "constrainedAxis:",
                "clipBehavior:")) {
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
