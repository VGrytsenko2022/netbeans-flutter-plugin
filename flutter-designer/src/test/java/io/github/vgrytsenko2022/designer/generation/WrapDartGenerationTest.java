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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrapDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsEveryReviewedArgumentInExactConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("direction"), enumValue("Axis", "vertical"));
        properties.put(name("alignment"), enumValue("WrapAlignment", "spaceEvenly"));
        properties.put(name("spacing"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(-8)));
        properties.put(name("runAlignment"), enumValue("WrapAlignment", "center"));
        properties.put(name("runSpacing"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(-4)));
        properties.put(name("crossAxisAlignment"),
                enumValue("WrapCrossAlignment", "end"));
        properties.put(name("textDirection"), enumValue("TextDirection", "rtl"));
        properties.put(name("verticalDirection"),
                enumValue("VerticalDirection", "up"));
        properties.put(name("clipBehavior"), enumValue("Clip", "antiAlias"));
        WidgetNode wrap = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Wrap"),
                properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        text("First"), text("Second")))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(wrap), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const Wrap("), build);
        assertTrue(build.contains("direction: Axis.vertical"), build);
        assertTrue(build.contains("alignment: WrapAlignment.spaceEvenly"), build);
        assertTrue(build.contains("spacing: -8.0"), build);
        assertTrue(build.contains("runAlignment: WrapAlignment.center"), build);
        assertTrue(build.contains("runSpacing: -4.0"), build);
        assertTrue(build.contains(
                "crossAxisAlignment: WrapCrossAlignment.end"), build);
        assertTrue(build.contains("textDirection: TextDirection.rtl"), build);
        assertTrue(build.contains("verticalDirection: VerticalDirection.up"), build);
        assertTrue(build.contains("clipBehavior: Clip.antiAlias"), build);
        assertTrue(build.contains("children: ["), build);
        assertTrue(build.contains("const Text('First')"), build);
        assertTrue(build.contains("const Text('Second')"), build);

        int direction = build.indexOf("direction:");
        int alignment = build.indexOf("alignment:");
        int spacing = build.indexOf("spacing:");
        int runAlignment = build.indexOf("runAlignment:");
        int runSpacing = build.indexOf("runSpacing:");
        int crossAxisAlignment = build.indexOf("crossAxisAlignment:");
        int textDirection = build.indexOf("textDirection:");
        int verticalDirection = build.indexOf("verticalDirection:");
        int clipBehavior = build.indexOf("clipBehavior:");
        int children = build.indexOf("children:");
        assertTrue(direction < alignment && alignment < spacing
                && spacing < runAlignment && runAlignment < runSpacing
                && runSpacing < crossAxisAlignment
                && crossAxisAlignment < textDirection
                && textDirection < verticalDirection
                && verticalDirection < clipBehavior
                && clipBehavior < children, build);
    }

    @Test
    void omittedArgumentsRemainOmittedSoFlutterOwnsEveryDefault() {
        WidgetNode wrap = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Wrap"),
                Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(wrap), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("return const Wrap("), build);
        for (String argument : List.of(
                "direction:", "alignment:", "spacing:", "runAlignment:",
                "runSpacing:", "crossAxisAlignment:", "textDirection:",
                "verticalDirection:", "clipBehavior:")) {
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
