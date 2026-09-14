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
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageViewDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void emitsStaticPageViewValuesAndAClosedNoopPageCallback() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("scrollDirection"), new PropertyValue.EnumValue("Axis", "vertical"));
        properties.put(name("reverse"), new PropertyValue.BooleanValue(true));
        properties.put(name("physics"), new PropertyValue.StringValue("page"));
        properties.put(name("pageSnapping"), new PropertyValue.BooleanValue(false));
        properties.put(name("onPageChanged"), new PropertyValue.StringValue("noop"));
        properties.put(name("dragStartBehavior"), new PropertyValue.EnumValue("DragStartBehavior", "down"));
        properties.put(name("allowImplicitScrolling"), new PropertyValue.BooleanValue(true));
        properties.put(name("scrollCacheExtent"), new PropertyValue.IntegerValue(BigInteger.valueOf(160)));
        properties.put(name("restorationId"), new PropertyValue.StringValue("pages"));
        properties.put(name("clipBehavior"), new PropertyValue.EnumValue("Clip", "antiAlias"));
        properties.put(name("hitTestBehavior"), new PropertyValue.EnumValue("HitTestBehavior", "translucent"));
        properties.put(name("padEnds"), new PropertyValue.BooleanValue(false));
        WidgetNode pageView = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.PageView"),
                properties, Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text("First"), text("Second")))),
                Extensions.empty());

        GeneratedDartRegions generated = generate(pageView);
        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                generated.importPlan().directives().stream().map(DartImportDirective::uri).toList());
        String gesturesPrefix = prefix(generated, GESTURES_IMPORT);
        String renderingPrefix = prefix(generated, RENDERING_IMPORT);
        String build = generated.build().payload();
        assertTrue(build.contains("child: PageView("), build);
        assertTrue(build.contains("scrollDirection: Axis.vertical"), build);
        assertTrue(build.contains("reverse: true"), build);
        assertTrue(build.contains("physics: const PageScrollPhysics()"), build);
        assertTrue(build.contains("pageSnapping: false"), build);
        assertTrue(build.contains("onPageChanged: (_) {}"), build);
        assertTrue(build.contains("dragStartBehavior: " + gesturesPrefix + ".DragStartBehavior.down"), build);
        assertTrue(build.contains("scrollCacheExtent: const " + renderingPrefix + ".ScrollCacheExtent.pixels(160)"), build);
        assertTrue(build.contains("hitTestBehavior: " + renderingPrefix + ".HitTestBehavior.translucent"), build);
        assertTrue(build.contains("children: ["), build);
        assertTrue(build.contains("const Text('First')"), build);
        assertTrue(build.contains("const Text('Second')"), build);
    }

    @Test
    void emitsTypedProjectCallbackAndPreservesOmissionDefaults() {
        WidgetNode callbackPage = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.PageView"),
                Map.of(name("onPageChanged"), new PropertyValue.CallbackValue("handlePageChanged")),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())), Extensions.empty());
        GeneratedDartRegions callback = generate(callbackPage);
        String callbackBuild = callback.build().payload();
        assertTrue(callbackBuild.contains("onPageChanged: handlePageChanged"), callbackBuild);
        assertFalse(callbackBuild.contains("onPageChanged: 'noop'"), callbackBuild);

        WidgetNode defaultsPage = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.PageView"),
                Map.of(), Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())), Extensions.empty());
        String defaultsBuild = generate(defaultsPage).build().payload();
        assertTrue(defaultsBuild.contains("child: PageView("), defaultsBuild);
        assertFalse(defaultsBuild.contains("onPageChanged:"), defaultsBuild);
        assertFalse(defaultsBuild.contains("physics:"), defaultsBuild);
        assertFalse(defaultsBuild.contains("scrollCacheExtent:"), defaultsBuild);
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)), Map.of(), Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(Optional.empty(), StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(),
                        new ManagedRegions(new ManagedRegion(ZERO_HASH), new ManagedRegion(ZERO_HASH))),
                Optional.empty(), root, Extensions.empty());
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static String prefix(GeneratedDartRegions generated, String uri) {
        return generated.importPlan().directives().stream().filter(directive -> directive.uri().equals(uri))
                .findFirst().orElseThrow().prefix().orElseThrow();
    }
}
