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
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListViewDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsTheCurrentNonConstConstructorAndSynthesizedStaticValues() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("scrollDirection"),
                new PropertyValue.EnumValue("Axis", "horizontal"));
        properties.put(name("reverse"), new PropertyValue.BooleanValue(true));
        properties.put(name("primary"), new PropertyValue.BooleanValue(false));
        properties.put(name("physics"), new PropertyValue.StringValue("bouncing"));
        properties.put(name("shrinkWrap"), new PropertyValue.BooleanValue(true));
        properties.put(name("padding"), new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        properties.put(name("itemExtent"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(48)));
        properties.put(name("addAutomaticKeepAlives"),
                new PropertyValue.BooleanValue(false));
        properties.put(name("addRepaintBoundaries"),
                new PropertyValue.BooleanValue(false));
        properties.put(name("addSemanticIndexes"),
                new PropertyValue.BooleanValue(false));
        properties.put(name("scrollCacheExtent"),
                new PropertyValue.IntegerValue(BigInteger.valueOf(200)));
        properties.put(name("semanticChildCount"),
                new PropertyValue.IntegerValue(BigInteger.valueOf(2)));
        properties.put(name("dragStartBehavior"),
                new PropertyValue.EnumValue("DragStartBehavior", "down"));
        properties.put(name("keyboardDismissBehavior"),
                new PropertyValue.EnumValue(
                        "ScrollViewKeyboardDismissBehavior", "onDrag"));
        properties.put(name("restorationId"),
                new PropertyValue.StringValue("main-list"));
        properties.put(name("clipBehavior"),
                new PropertyValue.EnumValue("Clip", "antiAlias"));
        properties.put(name("hitTestBehavior"),
                new PropertyValue.EnumValue("HitTestBehavior", "translucent"));
        WidgetNode listView = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ListView"),
                properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        text("First"), text("Second")))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(listView), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String gesturesPrefix = prefix(generated, GESTURES_IMPORT);
        String renderingPrefix = prefix(generated, RENDERING_IMPORT);
        String build = generated.build().payload();
        assertTrue(build.contains("return LayoutBuilder("), build);
        assertTrue(build.contains(
                "height: constraints.hasBoundedHeight ? null : 120"), build);
        assertFalse(build.contains(
                "width: constraints.hasBoundedWidth ? null : 240"), build);
        assertTrue(build.contains("child: ListView("), build);
        assertFalse(build.contains("child: const ListView("), build);
        assertTrue(build.contains("scrollDirection: Axis.horizontal"), build);
        assertTrue(build.contains(
                "physics: const BouncingScrollPhysics()"), build);
        assertTrue(build.contains(
                "padding: const EdgeInsets.fromLTRB(1.0, 2.0, 3.0, 4.0)"), build);
        assertTrue(build.contains("itemExtent: 48.0"), build);
        assertTrue(build.contains("scrollCacheExtent: const " + renderingPrefix
                + ".ScrollCacheExtent.pixels(200)"), build);
        assertTrue(build.contains("semanticChildCount: 2"), build);
        assertTrue(build.contains("dragStartBehavior: " + gesturesPrefix
                + ".DragStartBehavior.down"), build);
        assertTrue(build.contains(
                "keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag"), build);
        assertTrue(build.contains("hitTestBehavior: " + renderingPrefix
                + ".HitTestBehavior.translucent"), build);
        assertTrue(build.contains("children: ["), build);
        assertTrue(build.contains("const Text('First')"), build);
        assertTrue(build.contains("const Text('Second')"), build);
        int scrollCacheExtentIndex = build.indexOf("scrollCacheExtent:");
        int childrenIndex = build.indexOf("children:");
        int semanticChildCountIndex = build.indexOf("semanticChildCount:");
        assertTrue(scrollCacheExtentIndex < childrenIndex,
                "scrollCacheExtent must precede children:\n" + build);
        assertTrue(childrenIndex < semanticChildCountIndex,
                "children must precede semanticChildCount:\n" + build);
    }

    @Test
    void omittingAllPropertiesKeepsFrameworkDefaultsAndExtraImportsAbsent() {
        WidgetNode listView = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ListView"),
                Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(listView), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        GeneratedDartRegions generated = result.generated().orElseThrow();
        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return LayoutBuilder("), build);
        assertTrue(build.contains(
                "width: constraints.hasBoundedWidth ? null : 240"), build);
        assertTrue(build.contains(
                "height: constraints.hasBoundedHeight ? null : 120"), build);
        assertTrue(build.contains("child: ListView("), build);
        assertFalse(build.contains("physics:"));
        assertFalse(build.contains("scrollCacheExtent:"));
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

    private static String prefix(GeneratedDartRegions generated, String uri) {
        return generated.importPlan().directives().stream()
                .filter(directive -> directive.uri().equals(uri))
                .findFirst().orElseThrow().prefix().orElseThrow();
    }
}
