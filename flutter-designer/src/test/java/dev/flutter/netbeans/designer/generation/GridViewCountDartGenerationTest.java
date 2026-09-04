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

class GridViewCountDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsNamedCountConstructorFullSurfaceAndOrderedChildren() {
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
        properties.put(name("crossAxisCount"),
                new PropertyValue.IntegerValue(BigInteger.valueOf(3)));
        properties.put(name("mainAxisSpacing"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(8)));
        properties.put(name("crossAxisSpacing"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(6)));
        properties.put(name("childAspectRatio"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(1.5)));
        properties.put(name("mainAxisExtent"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(72)));
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
                new PropertyValue.StringValue("main-grid"));
        properties.put(name("clipBehavior"),
                new PropertyValue.EnumValue("Clip", "antiAlias"));
        properties.put(name("hitTestBehavior"),
                new PropertyValue.EnumValue("HitTestBehavior", "translucent"));
        WidgetNode grid = grid(properties, List.of(text("First"), text("Second")));

        GeneratedDartRegions generated = generate(grid);

        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String gesturesPrefix = prefix(generated, GESTURES_IMPORT);
        String renderingPrefix = prefix(generated, RENDERING_IMPORT);
        String build = generated.build().payload();
        assertTrue(build.contains("child: GridView.count("), build);
        assertFalse(build.contains("child: const GridView.count("), build);
        assertTrue(build.contains("scrollDirection: Axis.horizontal"), build);
        assertTrue(build.contains("reverse: true"), build);
        assertTrue(build.contains("primary: false"), build);
        assertTrue(build.contains("shrinkWrap: true"), build);
        assertTrue(build.contains(
                "padding: const EdgeInsets.fromLTRB(1.0, 2.0, 3.0, 4.0)"), build);
        assertTrue(build.contains("crossAxisCount: 3"), build);
        assertTrue(build.contains("mainAxisSpacing: 8.0"), build);
        assertTrue(build.contains("crossAxisSpacing: 6.0"), build);
        assertTrue(build.contains("childAspectRatio: 1.5"), build);
        assertTrue(build.contains("mainAxisExtent: 72.0"), build);
        assertTrue(build.contains("addAutomaticKeepAlives: false"), build);
        assertTrue(build.contains("addRepaintBoundaries: false"), build);
        assertTrue(build.contains("addSemanticIndexes: false"), build);
        assertTrue(build.contains("physics: const BouncingScrollPhysics()"), build);
        assertTrue(build.contains("scrollCacheExtent: const " + renderingPrefix
                + ".ScrollCacheExtent.pixels(200)"), build);
        assertTrue(build.contains("semanticChildCount: 2"), build);
        assertTrue(build.contains("dragStartBehavior: " + gesturesPrefix
                + ".DragStartBehavior.down"), build);
        assertTrue(build.contains(
                "keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag"), build);
        assertTrue(build.contains("restorationId: 'main-grid'"), build);
        assertTrue(build.contains("clipBehavior: Clip.antiAlias"), build);
        assertTrue(build.contains("hitTestBehavior: " + renderingPrefix
                + ".HitTestBehavior.translucent"), build);
        assertTrue(build.contains("children: ["), build);
        assertTrue(build.contains("const Text('First')"), build);
        assertTrue(build.contains("const Text('Second')"), build);
        int cache = build.indexOf("scrollCacheExtent:");
        int children = build.indexOf("children:");
        int semantics = build.indexOf("semanticChildCount:");
        assertTrue(cache < children && children < semantics, build);
    }

    @Test
    void prototypeAtRootKeepsRequiredDefaultAndBoundedViewportGuard() {
        WidgetNode grid = grid(
                Map.of(name("crossAxisCount"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                List.of());

        GeneratedDartRegions generated = generate(grid);

        assertEquals(List.of(WIDGETS_IMPORT), generated.importPlan().directives().stream()
                .map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("return LayoutBuilder("), build);
        assertTrue(build.contains(
                "width: constraints.hasBoundedWidth ? null : 240"), build);
        assertTrue(build.contains(
                "height: constraints.hasBoundedHeight ? null : 120"), build);
        assertTrue(build.contains("child: GridView.count("), build);
        assertTrue(build.contains("crossAxisCount: 2"), build);
        assertFalse(build.contains("physics:"), build);
        assertFalse(build.contains("scrollCacheExtent:"), build);
    }

    @Test
    void keepsConstraintGuardInsideColumnAndForHorizontalGridInsideRow() {
        WidgetNode vertical = grid(
                Map.of(name("crossAxisCount"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                List.of(text("Vertical")));
        WidgetNode horizontal = grid(
                Map.of(
                        name("crossAxisCount"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                        name("scrollDirection"),
                        new PropertyValue.EnumValue("Axis", "horizontal")),
                List.of(text("Horizontal")));
        WidgetNode column = multi("flutter.widgets.Column", List.of(vertical));
        WidgetNode row = multi("flutter.widgets.Row", List.of(horizontal));
        WidgetNode root = multi("flutter.widgets.Column", List.of(column, row));

        String build = generate(root).build().payload();

        assertEquals(2, occurrences(build, "GridView.count("), build);
        assertEquals(2, occurrences(build, "LayoutBuilder("), build);
        assertTrue(build.contains(
                "width: constraints.hasBoundedWidth ? null : 240"), build);
        assertTrue(build.contains(
                "height: constraints.hasBoundedHeight ? null : 120"), build);
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode grid(
            Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> children) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.GridView"),
                properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)),
                Extensions.empty());
    }

    private static WidgetNode multi(String type, List<WidgetNode> children) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId(type),
                Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)),
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

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0;
                index = value.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }
}
