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

class SingleChildScrollViewDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsConstConstructorFullDeclarativeSurfaceAndOptionalChild() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("scrollDirection"),
                new PropertyValue.EnumValue("Axis", "horizontal"));
        properties.put(name("reverse"), new PropertyValue.BooleanValue(true));
        properties.put(name("padding"), new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        properties.put(name("primary"), new PropertyValue.BooleanValue(false));
        properties.put(name("physics"), new PropertyValue.StringValue("bouncing"));
        properties.put(name("dragStartBehavior"),
                new PropertyValue.EnumValue("DragStartBehavior", "down"));
        properties.put(name("clipBehavior"),
                new PropertyValue.EnumValue("Clip", "antiAlias"));
        properties.put(name("hitTestBehavior"),
                new PropertyValue.EnumValue("HitTestBehavior", "translucent"));
        properties.put(name("restorationId"),
                new PropertyValue.StringValue("details-scroll"));
        properties.put(name("keyboardDismissBehavior"),
                new PropertyValue.EnumValue(
                        "ScrollViewKeyboardDismissBehavior", "onDrag"));
        WidgetNode scrollView = scrollView(properties, Optional.of(text("Details")));

        GeneratedDartRegions generated = generate(scrollView);

        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String gesturesPrefix = prefix(generated, GESTURES_IMPORT);
        String renderingPrefix = prefix(generated, RENDERING_IMPORT);
        String build = generated.build().payload();
        assertTrue(build.contains("return const SingleChildScrollView("), build);
        assertFalse(build.contains("LayoutBuilder("), build);
        assertFalse(build.contains("SizedBox("), build);
        assertTrue(build.contains("scrollDirection: Axis.horizontal"), build);
        assertTrue(build.contains("reverse: true"), build);
        assertTrue(build.contains(
                "padding: const EdgeInsets.fromLTRB(1.0, 2.0, 3.0, 4.0)"), build);
        assertTrue(build.contains("primary: false"), build);
        assertTrue(build.contains("physics: const BouncingScrollPhysics()"), build);
        assertTrue(build.contains("child: const Text('Details')"), build);
        assertTrue(build.contains("dragStartBehavior: " + gesturesPrefix
                + ".DragStartBehavior.down"), build);
        assertTrue(build.contains("clipBehavior: Clip.antiAlias"), build);
        assertTrue(build.contains("hitTestBehavior: " + renderingPrefix
                + ".HitTestBehavior.translucent"), build);
        assertTrue(build.contains("restorationId: 'details-scroll'"), build);
        assertTrue(build.contains(
                "keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag"), build);
        assertOrdered(build,
                "physics:", "child:", "dragStartBehavior:", "clipBehavior:",
                "hitTestBehavior:", "restorationId:", "keyboardDismissBehavior:");
    }

    @Test
    void omittedPropertiesAndChildPreserveDefaultsWithoutConstraintGuard() {
        GeneratedDartRegions generated = generate(scrollView(Map.of(), Optional.empty()));

        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return const SingleChildScrollView("), build);
        assertTrue(build.contains("child: null"), build);
        assertFalse(build.contains("physics:"), build);
        assertFalse(build.contains("LayoutBuilder("), build);
        assertFalse(build.contains("constraints.hasBounded"), build);
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode scrollView(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.SingleChildScrollView"),
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

    private static String prefix(GeneratedDartRegions generated, String uri) {
        return generated.importPlan().directives().stream()
                .filter(directive -> directive.uri().equals(uri))
                .findFirst().orElseThrow().prefix().orElseThrow();
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
