package dev.flutter.netbeans.designer.canvas.payload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasDevicePixelRatio;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasImageAsset;
import dev.flutter.netbeans.designer.canvas.CanvasImageAssetId;
import dev.flutter.netbeans.designer.canvas.CanvasImageFormat;
import dev.flutter.netbeans.designer.canvas.CanvasImageResource;
import dev.flutter.netbeans.designer.canvas.CanvasImageResourceBundle;
import dev.flutter.netbeans.designer.canvas.CanvasImageVariant;
import dev.flutter.netbeans.designer.canvas.CanvasLocale;
import dev.flutter.netbeans.designer.canvas.CanvasPresentationGate;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasTextScaleFactor;
import dev.flutter.netbeans.designer.canvas.CanvasThemeBrightness;
import dev.flutter.netbeans.designer.canvas.CanvasThemeColorValue;
import dev.flutter.netbeans.designer.canvas.CanvasThemeComponentColorRole;
import dev.flutter.netbeans.designer.canvas.CanvasThemeTextStyleOverride;
import dev.flutter.netbeans.designer.canvas.CanvasViewport;
import dev.flutter.netbeans.designer.canvas.ValidatedCanvasRevisionSnapshot;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CanvasModelPayloadCodecTest {
    @Test void fadeInImageProvidersResolveIndependentlyAndSourceIdentitiesStayPrivate() throws Exception {
        var codec=new CanvasModelPayloadCodec();
        var one=CanvasImageResource.create(CanvasImageFormat.PNG,8,8,new byte[]{1,2,3});
        var two=CanvasImageResource.create(CanvasImageFormat.PNG,4,4,new byte[]{4,5,6});
        var a=CanvasImageAssetId.application("assets/placeholder.png");
        var b=CanvasImageAssetId.application("assets/target.png");
        var bundle=new CanvasImageResourceBundle(List.of(
            new CanvasImageAsset(a,one.resourceId(),List.of(new CanvasImageVariant(BigDecimal.ONE,one.resourceId()))),
            new CanvasImageAsset(b,two.resourceId(),List.of(new CanvasImageVariant(BigDecimal.ONE,two.resourceId())))),List.of(one,two));
        var definition=BuiltInWidgetCatalog.getDefault().find(dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.TYPE).orElseThrow();
        var prototype=dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(definition,StableId.random());
        var properties=new LinkedHashMap<PropertyName,PropertyValue>();
        properties.put(new PropertyName("placeholder"),PropertyValue.ImageProviderValue.asset("assets/placeholder.png"));
        properties.put(new PropertyName("image"),PropertyValue.ImageProviderValue.exactAsset("assets/target.png",BigDecimal.valueOf(2)));
        var node=new WidgetNode(prototype.id(),prototype.type(),properties,Map.of());
        String json=new String(codec.encode(request(PROFILE,new DesignerDocument(DOCUMENT_ID,source(),node),bundle)),StandardCharsets.UTF_8);
        assertTrue(json.contains(one.resourceId()));assertTrue(json.contains(two.resourceId()));
        for(String name:List.of("placeholder","image")){
            properties.put(new PropertyName(name),new PropertyValue.DartObjectReferenceValue(Optional.of("package:secret/assets.dart"),"privateImages",Optional.of("image"),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()));
        }
        node=new WidgetNode(prototype.id(),prototype.type(),properties,Map.of());
        json=new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID,source(),node))),StandardCharsets.UTF_8);
        assertFalse(json.contains("secret"));assertFalse(json.contains("privateImages"));
        assertTrue(json.contains("\"placeholder\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertTrue(json.contains("\"image\":{\"kind\":\"dartObjectReferencePresence\"}"));
    }

    @org.junit.jupiter.api.Test
    void rawImageOpaqueHandlesAndAnimationSourcesNeverLeakIntoCanvasPayload() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var properties = new LinkedHashMap<PropertyName,PropertyValue>();
        for (String name : List.of("image", "opacity", "centerSlice", "width", "height", "scale", "color", "alignment"))
            properties.put(new PropertyName(name), new PropertyValue.DartObjectReferenceValue(
                    Optional.of("package:private_app/images.dart"), "PrivateImages", Optional.of("decoded"),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        var node = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.RawImage"), properties, Map.of());
        String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID,source(),node))), StandardCharsets.UTF_8);
        assertFalse(json.contains("private_app")); assertFalse(json.contains("PrivateImages")); assertFalse(json.contains("decoded"));
        for (String name : List.of("image", "opacity", "centerSlice"))
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
    }

    @Test
    void colorFilteredSourcesStayPrivateIncludingInactiveColorDraft() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        var source = new PropertyValue.DartObjectReferenceValue(
                Optional.of("package:private_app/filters.dart"), "PrivateFilters", Optional.of("privateValue"),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
        properties.put(new PropertyName("color"), source);
        for (PropertyValue selector : List.of(new PropertyValue.StringValue("matrix"), source)) {
            properties.put(new PropertyName("colorFilter"), selector);
            var node = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.ColorFiltered"),
                    properties, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            for (String secret : List.of("private_app", "PrivateFilters", "privateValue", "filters.dart")) {
                assertFalse(json.contains(secret), secret);
            }
            assertTrue(json.contains("\"color\":{\"kind\":\"dartObjectReferencePresence\"}"));
            if (selector == source) {
                assertTrue(json.contains("\"colorFilter\":{\"kind\":\"dartObjectReferencePresence\"}"));
            }
        }
    }

    private static final StableId DOCUMENT_ID = id(
            "4efb0eb1-b0f9-4809-bd3e-73f2486dd7bd");
    private static final CanvasRenderProfile PROFILE = new CanvasRenderProfile(
            CanvasPreviewMode.MOBILE,
            CanvasTargetPlatform.ANDROID,
            new CanvasViewport(390, 844),
            new CanvasDevicePixelRatio(1),
            new CanvasResolvedTheme(
                    "light", 0xFF6750A4, CanvasThemeBrightness.LIGHT,
                    "A".repeat(64)),
            new CanvasLocale("en-US"),
            new CanvasTextScaleFactor(1),
            new CanvasEngineIdentity("3.44.8", "framework", "engine", "3.12.0"));

    @Test
    void exposesTheExactReviewedCanvasCapabilitySetInPaletteOrder() {
        assertEquals(List.of(
                "flutter.material.Scaffold",
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.TextField",
                "flutter.material.Divider",
                "flutter.material.VerticalDivider",
                "flutter.material.Card",
                "flutter.material.Badge",
                "flutter.material.CircleAvatar",
                "flutter.material.LinearProgressIndicator",
                "flutter.material.CircularProgressIndicator",
                "flutter.material.RefreshProgressIndicator",
            "flutter.material.RefreshIndicator",
            "flutter.material.TextButton",
            "flutter.material.OutlinedButton",
            "flutter.material.FilledButton",
            "flutter.material.FloatingActionButton",
            "flutter.material.IconButton",
            "flutter.material.Checkbox",
            "flutter.material.Switch",
            "flutter.material.Slider",
            "flutter.material.RangeSlider",
            "flutter.material.Radio",
            "flutter.widgets.RadioGroup",
            "flutter.material.ListTile",
            "flutter.material.CheckboxListTile",
            "flutter.material.SwitchListTile",
            "flutter.material.RadioListTile",
            "flutter.material.ExpansionTile",
            "flutter.material.Tooltip",
            "flutter.material.TooltipVisibility",
            "flutter.material.TooltipTheme",
                "flutter.material.MenuItemButton",
                "flutter.material.MenuAnchor",
                "flutter.material.SubmenuButton",
                "flutter.material.MenuBar",
                "flutter.material.NavigationBar",
                "flutter.material.NavigationRail",
                "flutter.material.NavigationDrawer",
                "flutter.material.Drawer",
                "flutter.material.BottomAppBar",
                "flutter.material.BottomNavigationBar",
                "flutter.material.Material",
                "flutter.material.Scrollbar",
            "flutter.material.SliverAppBar", "flutter.material.SliverAppBar.medium", "flutter.material.SliverAppBar.large", "flutter.material.FlexibleSpaceBar", "flutter.material.FlexibleSpaceBarSettings", "flutter.material.AnimatedTheme", "flutter.material.Theme", "flutter.material.AnimatedIcon",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Wrap",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Container",
                "flutter.widgets.Opacity",
                "flutter.widgets.Align",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.FittedBox",
                "flutter.widgets.ConstrainedBox",
                "flutter.widgets.UnconstrainedBox",
                "flutter.widgets.LimitedBox",
                "flutter.widgets.OverflowBox",
                "flutter.widgets.Stack",
                "flutter.widgets.IndexedStack",
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer",
                "flutter.widgets.Baseline",
                "flutter.widgets.IntrinsicHeight",
                "flutter.widgets.IntrinsicWidth",
                "flutter.widgets.Offstage",
                "flutter.widgets.SizedOverflowBox",
                "flutter.widgets.Transform",
                "flutter.widgets.RotatedBox",
                "flutter.widgets.PreferredSize",
                "flutter.widgets.ListBody",
                "flutter.widgets.OverflowBar",
                "flutter.widgets.SafeArea", "flutter.widgets.LayoutBuilder", "flutter.widgets.OrientationBuilder", "flutter.widgets.DeviceOrientationBuilder", "flutter.widgets.ListenableBuilder", "flutter.widgets.AnimatedBuilder", "flutter.widgets.ValueListenableBuilder", "flutter.widgets.TweenAnimationBuilder", "flutter.widgets.AnimatedOpacity", "flutter.widgets.AnimatedAlign", "flutter.widgets.AnimatedPadding", "flutter.widgets.AnimatedSlide", "flutter.widgets.AnimatedScale", "flutter.widgets.AnimatedRotation", "flutter.widgets.AnimatedContainer", "flutter.widgets.AnimatedSize", "flutter.widgets.AnimatedPositioned", "flutter.widgets.AnimatedPositioned.fromRect", "flutter.widgets.AnimatedPositionedDirectional", "flutter.widgets.AnimatedDefaultTextStyle", "flutter.widgets.AnimatedPhysicalModel", "flutter.widgets.AnimatedFractionallySizedBox", "flutter.widgets.AnimatedCrossFade", "flutter.widgets.AnimatedSwitcher", "flutter.widgets.DefaultTextStyleTransition", "flutter.widgets.FadeTransition", "flutter.widgets.SlideTransition", "flutter.widgets.ScaleTransition", "flutter.widgets.RotationTransition", "flutter.widgets.SizeTransition", "flutter.widgets.PositionedTransition", "flutter.widgets.RelativePositionedTransition", "flutter.widgets.DecoratedBoxTransition", "flutter.widgets.AlignTransition", "flutter.widgets.MatrixTransition",
                "flutter.widgets.ListView",
                "flutter.widgets.GridView",
                "flutter.widgets.GridView.extent",
                "flutter.widgets.SingleChildScrollView",
                "flutter.widgets.PageView",
                "flutter.widgets.ListWheelScrollView",
                "flutter.widgets.CustomScrollView",
                "flutter.widgets.SliverToBoxAdapter",
            "flutter.widgets.SliverList",
            "flutter.widgets.SliverGrid",
            "flutter.widgets.SliverGrid.extent",
                "flutter.widgets.SliverList.builder",
                "flutter.widgets.SliverList.separated",
                "flutter.widgets.SliverList.delegate",
                "flutter.widgets.SliverGrid.builder",
                "flutter.widgets.SliverGrid.list",
                "flutter.widgets.SliverGrid.delegate",
                "flutter.widgets.SliverPadding",
                "flutter.widgets.SliverFillRemaining",
                "flutter.widgets.SliverFillViewport", "flutter.widgets.SliverFillViewport.delegate", "flutter.widgets.SliverFixedExtentList", "flutter.widgets.SliverFixedExtentList.builder", "flutter.widgets.SliverFixedExtentList.delegate", "flutter.widgets.SliverPrototypeExtentList", "flutter.widgets.SliverPrototypeExtentList.builder", "flutter.widgets.SliverPrototypeExtentList.delegate",
                "flutter.widgets.SliverVariedExtentList", "flutter.widgets.SliverVariedExtentList.builder", "flutter.widgets.SliverVariedExtentList.delegate", "flutter.widgets.SliverMainAxisGroup", "flutter.widgets.SliverCrossAxisGroup", "flutter.widgets.SliverCrossAxisExpanded", "flutter.widgets.SliverConstrainedCrossAxis", "flutter.widgets.SliverOpacity", "flutter.widgets.SliverIgnorePointer", "flutter.widgets.SliverOffstage", "flutter.widgets.SliverVisibility", "flutter.widgets.SliverVisibility.maintain", "flutter.widgets.SliverSafeArea", "flutter.widgets.SliverAnimatedOpacity", "flutter.widgets.SliverLayoutBuilder", "flutter.widgets.SliverPersistentHeader", "flutter.widgets.SliverResizingHeader", "flutter.widgets.PinnedHeaderSliver", "flutter.widgets.SliverFloatingHeader", "flutter.widgets.DeviceOrientationBuilder.sliver", "flutter.widgets.ListenableBuilder.sliver", "flutter.widgets.AnimatedBuilder.sliver", "flutter.widgets.ValueListenableBuilder.sliver", "flutter.widgets.TweenAnimationBuilder.sliver", "flutter.widgets.SliverFadeTransition",
                "flutter.widgets.Text",
                "flutter.widgets.Icon",
                "flutter.widgets.Image",
                "flutter.widgets.ColoredBox",
                 "flutter.widgets.Placeholder",
                 "flutter.widgets.Directionality",
                 "flutter.widgets.DecoratedBox",
                 "flutter.widgets.Builder",
                 "flutter.widgets.ClipRect",
                "flutter.widgets.ClipOval",
                "flutter.widgets.ClipRRect",
                "flutter.widgets.ClipPath",
                "flutter.widgets.ClipRSuperellipse",
                "flutter.widgets.PhysicalModel",
                "flutter.widgets.PhysicalShape",
                "flutter.widgets.RepaintBoundary",
                "flutter.widgets.IgnorePointer",
                "flutter.widgets.AbsorbPointer",
                "flutter.widgets.Visibility",
                "flutter.widgets.TickerMode",
                "flutter.widgets.DefaultTextHeightBehavior",
                "flutter.widgets.DefaultSelectionStyle",
                "flutter.widgets.IconTheme",
                "flutter.widgets.ImageIcon",
                "flutter.widgets.DefaultTextStyle",
                "flutter.widgets.DefaultTextStyle.merge", "flutter.widgets.ModalBarrier", "flutter.widgets.AnimatedModalBarrier", "flutter.widgets.FadeInImage", "flutter.widgets.RawImage", "flutter.widgets.ColorFiltered",
                "flutter.widgets.ExcludeSemantics",
                "flutter.widgets.BlockSemantics",
                "flutter.widgets.MergeSemantics",
                "flutter.widgets.IndexedSemantics",
                "flutter.widgets.ExcludeFocus",
                "flutter.widgets.ExcludeFocusTraversal",
                "flutter.widgets.GestureDetector",
                "flutter.widgets.Listener",
                "flutter.widgets.MouseRegion",
                "flutter.widgets.Focus",
                "flutter.widgets.NotificationListener"),
                BuiltInWidgetCatalog.getDefault().paletteDefinitions().stream()
                        .filter(CanvasModelPayloadCodec::supports)
                        .map(definition -> definition.typeId().value())
                        .toList());
    }

    @Test
    void projectsAllPhysicalModelValuesAndRetainsIgnoredCircleRadiusWithoutPrivateMetadata()
            throws Exception {
        for (boolean themed : List.of(false, true)) {
            var physical = dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.physicalModel(
                    dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.fullProperties(
                            "circle", "antiAliasWithSaveLayer", themed), true);
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), physical))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.PhysicalModel\""), json);
            assertTrue(json.contains("\"shape\":{\"kind\":\"enum\",\"type\":\"BoxShape\",\"value\":\"circle\"}"), json);
            assertTrue(json.contains("\"geometry\":{\"kind\":\"physical\""), json);
            assertTrue(json.contains("\"topLeft\":{\"x\":1.5,\"y\":2.5}"), json);
            assertTrue(json.contains("\"bottomLeft\":{\"x\":7.5,\"y\":8.5}"), json);
            assertTrue(json.contains("\"elevation\":{\"kind\":\"double\",\"value\":12.5}"), json);
            assertTrue(json.contains("\"shadowColor\""), json);
            assertTrue(json.contains("Preserved child"), json);
            for (String privateField : List.of("sample.dart", "libraryUri", "dartClassName", "creationDefault")) {
                assertFalse(json.contains(privateField), json);
            }
        }
    }

    @Test
    void projectsAllPhysicalShapePresetsAndRedactsProjectClipperNames() throws Exception {
        for (var shape : PropertyValue.ShapeBorderClipperValue.Shape.values()) {
            var values = dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.fullProperties(
                    shape, "antiAliasWithSaveLayer", true);
            var node = dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.physicalShape(values, true);
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"kind\":\"shapeBorderClipper\",\"shape\":\"" + shape.wireName() + "\""), json);
            assertTrue(json.contains("\"topLeft\":{\"x\":1.5,\"y\":2.5}"), json);
            assertTrue(json.contains("Preserved child"), json);
        }
        var values = new java.util.LinkedHashMap<>(dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.defaults());
        values.put(new PropertyName("clipper"), new PropertyValue.DartObjectReferenceValue(
                Optional.of("package:private_app/private_clippers.dart"), "PrivateClipper", Optional.of("hidden"),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true)));
        var node = dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.physicalShape(values, true);
        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"clipper\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
        for (String privateName : List.of("PrivateClipper", "private_clippers", "hidden", "libraryUri", "rootSymbol")) {
            assertFalse(json.contains(privateName), json);
        }
    }

    @Test
    void rejectsPhysicalModelDirectionalRadiusAndMissingRequiredColorBeforePayloadEncoding() {
        var values = new java.util.LinkedHashMap<>(
                dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.defaults());
        values.put(new PropertyName("borderRadius"),
                dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.radius(true));
        for (var invalid : List.of(
                dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.physicalModel(values, false),
                dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.physicalModel(Map.of(), false))) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
    }

    @Test
    void projectsColoredBoxColorPolicyAndOptionalChildWithoutProtocolChange()
            throws Exception {
        WidgetNode coloredBox = new WidgetNode(
                id("09432278-340e-48b7-9604-766fc5cad4a5"),
                type("flutter.widgets.ColoredBox"),
                Map.of(
                        new PropertyName("color"),
                        new PropertyValue.ThemeTokenValue(
                                new ThemeToken("material.colorScheme.primary")),
                        new PropertyName("isAntiAlias"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "780ed564-3d39-4a50-bb75-e22892d7ef45",
                        "Colored child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), coloredBox))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.ColoredBox\""), json);
        assertTrue(json.contains("\"color\":{\"kind\":\"themeToken\","
                + "\"token\":\"material.colorScheme.primary\"}"), json);
        assertTrue(json.contains(
                "\"isAntiAlias\":{\"kind\":\"boolean\",\"value\":false}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"780ed564-3d39-4a50-bb75-e22892d7ef45\""), json);
    }

    @Test
    void projectsPlaceholderOmissionExplicitKindsAndOptionalChildWithoutProtocolChange()
            throws Exception {
        WidgetNode omitted = new WidgetNode(
                id("7843aa63-0bf8-4788-b18d-a470de935891"),
                type("flutter.widgets.Placeholder"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        WidgetNode explicit = new WidgetNode(
                id("c3138487-f329-4cc5-bda9-fba8a8b61b0e"),
                type("flutter.widgets.Placeholder"),
                Map.of(
                        new PropertyName("color"),
                        new PropertyValue.ThemeTokenValue(
                                new ThemeToken("material.colorScheme.outline")),
                        new PropertyName("strokeWidth"),
                        new PropertyValue.DoubleValue(new BigDecimal("0.5")),
                        new PropertyName("fallbackWidth"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(320)),
                        new PropertyName("fallbackHeight"),
                        new PropertyValue.DoubleValue(new BigDecimal("480.25"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "3925066f-4426-4726-a05e-f7fdc02f0bba",
                        "Placeholder child"))));

        CanvasModelPayloadCodec codec = new CanvasModelPayloadCodec();
        String omittedJson = new String(codec.encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), omitted))),
                StandardCharsets.UTF_8);
        String explicitJson = new String(codec.encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), explicit))),
                StandardCharsets.UTF_8);

        assertTrue(omittedJson.contains("\"protocolVersion\":19"), omittedJson);
        assertTrue(omittedJson.contains(
                "\"type\":\"flutter.widgets.Placeholder\""), omittedJson);
        assertTrue(omittedJson.contains(
                "\"properties\":{},\"slots\":{\"child\":{"
                + "\"kind\":\"single\",\"child\":null}"), omittedJson);
        assertFalse(omittedJson.contains("\"color\":"), omittedJson);
        assertFalse(omittedJson.contains("\"strokeWidth\":"), omittedJson);
        assertFalse(omittedJson.contains("\"fallbackWidth\":"), omittedJson);
        assertFalse(omittedJson.contains("\"fallbackHeight\":"), omittedJson);

        assertTrue(explicitJson.contains("\"protocolVersion\":19"), explicitJson);
        assertTrue(explicitJson.contains(
                "\"type\":\"flutter.widgets.Placeholder\""), explicitJson);
        assertTrue(explicitJson.contains("\"color\":{\"kind\":\"themeToken\","
                + "\"token\":\"material.colorScheme.outline\"}"), explicitJson);
        assertTrue(explicitJson.contains(
                "\"strokeWidth\":{\"kind\":\"double\",\"value\":0.5}"),
                explicitJson);
        assertTrue(explicitJson.contains(
                "\"fallbackWidth\":{\"kind\":\"integer\",\"value\":320}"),
                explicitJson);
        assertTrue(explicitJson.contains(
                "\"fallbackHeight\":{\"kind\":\"double\",\"value\":480.25}"),
                explicitJson);
        assertTrue(explicitJson.contains(
                "\"child\":{\"id\":\"3925066f-4426-4726-a05e-f7fdc02f0bba\""),
                explicitJson);
    }

    @Test
    void projectsSafeAreaRequiredChildAndSignedPhysicalMinimumAsEdgeInsets()
            throws Exception {
        WidgetNode safeArea = new WidgetNode(
                id("177b55b5-7831-4d43-8d26-0219f743087f"),
                type("flutter.widgets.SafeArea"),
                Map.of(new PropertyName("minimum"),
                        new PropertyValue.EdgeInsetsValue(
                                new BigDecimal("-12.5"),
                                new BigDecimal("2.25"),
                                new BigDecimal("-3.5"),
                                BigDecimal.ZERO)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "1877e358-ac21-4c35-9ac4-a0e62402fc19",
                        "Safe child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), safeArea))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.SafeArea\""), json);
        assertTrue(json.contains("\"minimum\":{\"kind\":\"edgeInsets\","
                + "\"left\":-12.5,\"top\":2.25,"
                + "\"right\":-3.5,\"bottom\":0}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"1877e358-ac21-4c35-9ac4-a0e62402fc19\""),
                json);
        assertFalse(json.contains("\"kind\":\"edgeInsetsDirectional\""), json);
    }

    @Test
    void rejectsDirectionalSafeAreaMinimumAtJavaBoundaryBeforePayloadEncoding() {
        WidgetNode safeArea = new WidgetNode(
                id("4621d370-f7c7-4a66-8dc1-f8c0b500c0d0"),
                type("flutter.widgets.SafeArea"),
                Map.of(new PropertyName("minimum"),
                        new PropertyValue.EdgeInsetsDirectionalValue(
                                BigDecimal.ONE,
                                BigDecimal.TWO,
                                BigDecimal.valueOf(3),
                                BigDecimal.valueOf(4))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "9321e2c3-cbbc-4a7a-a627-35a86e35e715",
                        "Directional child"))));
        DesignerDocument invalid = new DesignerDocument(
                DOCUMENT_ID, source(), safeArea);

        // A CanvasRenderRequest cannot be created, so encode/write is never reached.
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class, () -> request(invalid));
        assertTrue(failure.getMessage().contains("designer.property.constraint"),
                failure::getMessage);
    }

    @Test
    void projectsDirectionalityRequiredEnumAndChildWithoutProtocolChange()
            throws Exception {
        WidgetNode directionality = new WidgetNode(
                id("de590ed8-e30e-4fbd-8b20-c5acbdd820a1"),
                type("flutter.widgets.Directionality"),
                Map.of(new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "274e325d-afd5-428a-9e0a-427e13e8990b",
                        "RTL child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), directionality))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.Directionality\""), json);
        assertTrue(json.contains("\"textDirection\":{\"kind\":\"enum\","
                + "\"type\":\"TextDirection\",\"value\":\"rtl\"}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"274e325d-afd5-428a-9e0a-427e13e8990b\""),
                json);
    }

    @Test
    void rejectsDirectionalityWithoutRequiredDirectionBeforePayloadEncoding() {
        WidgetNode directionality = new WidgetNode(
                id("c4137bbd-4448-4adc-b8a4-99f468423098"),
                type("flutter.widgets.Directionality"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "ae1d9358-7041-4143-9dd0-77e40931e72e",
                        "Missing direction"))));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> request(new DesignerDocument(
                        DOCUMENT_ID, source(), directionality)));

        assertTrue(failure.getMessage().contains("designer.property.missing"),
                failure::getMessage);
    }

    @Test
    void projectsDecoratedBoxTypedDecorationPositionAndOptionalChildWithoutProtocolChange()
            throws Exception {
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(new ColorSource.Literal(0xFF102030L)),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        WidgetNode decoratedBox = new WidgetNode(
                id("914da39f-6ac7-4210-a6e0-9b86c8da306a"),
                type("flutter.widgets.DecoratedBox"),
                Map.of(
                        new PropertyName("decoration"), decoration,
                        new PropertyName("position"), new PropertyValue.EnumValue(
                                "DecorationPosition", "foreground")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "789cf2e4-c77d-45a3-b015-8d1080516b78",
                        "Decorated child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), decoratedBox))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.DecoratedBox\""), json);
        assertTrue(json.contains(
                "\"decoration\":{\"kind\":\"boxDecoration\""), json);
        assertTrue(json.contains(
                "\"color\":{\"kind\":\"literal\",\"argb\":\"0xFF102030\"}"),
                json);
        assertTrue(json.contains("\"shape\":\"rectangle\""), json);
        assertTrue(json.contains("\"position\":{\"kind\":\"enum\","
                + "\"type\":\"DecorationPosition\",\"value\":\"foreground\"}"),
                json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"789cf2e4-c77d-45a3-b015-8d1080516b78\""),
                json);
    }

    @Test
    void rejectsDecoratedBoxWithoutRequiredDecorationBeforePayloadEncoding() {
        WidgetNode decoratedBox = new WidgetNode(
                id("62cc1ed5-786b-47e8-9b40-c88e87e21db1"),
                type("flutter.widgets.DecoratedBox"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> request(new DesignerDocument(
                        DOCUMENT_ID, source(), decoratedBox)));

        assertTrue(failure.getMessage().contains("designer.property.missing"),
                failure::getMessage);
    }

    @Test
    void projectsClipRRectPhysicalAndDirectionalRadiusGeometryInProtocolV17()
            throws Exception {
        PropertyValue.BoxDecorationValue.Radius first =
                new PropertyValue.BoxDecorationValue.Radius(
                        new BigDecimal("1.5"), new BigDecimal("2.5"));
        PropertyValue.BoxDecorationValue.Radius second =
                new PropertyValue.BoxDecorationValue.Radius(
                        new BigDecimal("3.5"), new BigDecimal("4.5"));
        WidgetNode physical = new WidgetNode(
                id("b42d740f-4989-4149-9639-5668cab9fc60"),
                type("flutter.widgets.ClipRRect"),
                Map.of(
                        new PropertyName("borderRadius"),
                        new PropertyValue.BorderRadiusValue(
                                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                        first, second, first, second)),
                        new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "6bc88608-afde-4717-be9e-6d7414eb9d12",
                        "Rounded child"))));

        String physicalJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), physical))),
                StandardCharsets.UTF_8);

        assertTrue(physicalJson.contains("\"protocolVersion\":19"), physicalJson);
        assertTrue(physicalJson.contains(
                "\"type\":\"flutter.widgets.ClipRRect\""), physicalJson);
        assertTrue(physicalJson.contains(
                "\"borderRadius\":{\"kind\":\"borderRadius\","
                + "\"geometry\":{\"kind\":\"physical\","
                + "\"topLeft\":{\"x\":1.5,\"y\":2.5},"
                + "\"topRight\":{\"x\":3.5,\"y\":4.5},"
                + "\"bottomRight\":{\"x\":1.5,\"y\":2.5},"
                + "\"bottomLeft\":{\"x\":3.5,\"y\":4.5}}}"), physicalJson);
        assertTrue(physicalJson.contains(
                "\"clipBehavior\":{\"kind\":\"enum\","
                + "\"type\":\"Clip\",\"value\":\"hardEdge\"}"), physicalJson);
        assertTrue(physicalJson.contains(
                "\"child\":{\"id\":\"6bc88608-afde-4717-be9e-6d7414eb9d12\""),
                physicalJson);

        WidgetNode directional = new WidgetNode(
                id("08efef8e-982a-4a09-8dc8-34bd1c52cdab"),
                type("flutter.widgets.ClipRRect"),
                Map.of(new PropertyName("borderRadius"),
                        new PropertyValue.BorderRadiusValue(
                                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                        first, second, first, second))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        String directionalJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), directional))),
                StandardCharsets.UTF_8);

        assertTrue(directionalJson.contains(
                "\"geometry\":{\"kind\":\"directional\","
                + "\"topStart\":{\"x\":1.5,\"y\":2.5},"
                + "\"topEnd\":{\"x\":3.5,\"y\":4.5},"
                + "\"bottomEnd\":{\"x\":1.5,\"y\":2.5},"
                + "\"bottomStart\":{\"x\":3.5,\"y\":4.5}}}"),
                directionalJson);
        assertFalse(directionalJson.contains("\"clipBehavior\""), directionalJson);
        assertTrue(directionalJson.contains("\"child\":null"), directionalJson);
        assertFalse(directionalJson.contains("clipper"), directionalJson);
    }

    @Test
    void projectsClipRRectClipperAsPresenceOnlyWithoutProjectIdentity()
            throws Exception {
        WidgetNode clipRRect = new WidgetNode(
                id("3cc07d19-0f82-49b4-ae54-61e9ac82c38e"),
                type("flutter.widgets.ClipRRect"),
                Map.of(new PropertyName("clipper"),
                        new PropertyValue.DartObjectReferenceValue(
                                Optional.of(
                                        "package:private_app/secret/rrect_clipper.dart"),
                                "SecretRoundedClipper", Optional.of("hiddenFactory"),
                                PropertyValue.DartObjectReferenceValue.Access
                                        .ZERO_ARGUMENT_INVOCATION,
                                Optional.of(true))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), clipRRect))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"clipper\":{\"kind\":\"dartObjectReferencePresence\"}"),
                json);
        for (String secret : List.of(
                "package:private_app", "rrect_clipper.dart",
                "SecretRoundedClipper", "hiddenFactory",
                "zeroArgumentInvocation", "\"constant\"", "\"libraryUri\"",
                "\"rootSymbol\"", "\"member\"", "\"access\"")) {
            assertFalse(json.contains(secret), () -> secret + " leaked in " + json);
        }
    }

    @Test
    void projectsClipRSuperellipsePhysicalAndDirectionalRadiusGeometryInProtocolV17()
            throws Exception {
        PropertyValue.BoxDecorationValue.Radius first =
                new PropertyValue.BoxDecorationValue.Radius(
                        new BigDecimal("1.5"), new BigDecimal("2.5"));
        PropertyValue.BoxDecorationValue.Radius second =
                new PropertyValue.BoxDecorationValue.Radius(
                        new BigDecimal("3.5"), new BigDecimal("4.5"));
        WidgetNode physical = new WidgetNode(
                id("b42d740f-4989-4149-9639-5668cab9fc60"),
                type("flutter.widgets.ClipRSuperellipse"),
                Map.of(
                        new PropertyName("borderRadius"),
                        new PropertyValue.BorderRadiusValue(
                                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                        first, second, first, second)),
                        new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "6bc88608-afde-4717-be9e-6d7414eb9d12",
                        "Rounded child"))));

        String physicalJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), physical))),
                StandardCharsets.UTF_8);

        assertTrue(physicalJson.contains("\"protocolVersion\":19"), physicalJson);
        assertTrue(physicalJson.contains(
                "\"type\":\"flutter.widgets.ClipRSuperellipse\""), physicalJson);
        assertTrue(physicalJson.contains(
                "\"borderRadius\":{\"kind\":\"borderRadius\","
                + "\"geometry\":{\"kind\":\"physical\","
                + "\"topLeft\":{\"x\":1.5,\"y\":2.5},"
                + "\"topRight\":{\"x\":3.5,\"y\":4.5},"
                + "\"bottomRight\":{\"x\":1.5,\"y\":2.5},"
                + "\"bottomLeft\":{\"x\":3.5,\"y\":4.5}}}"), physicalJson);
        assertTrue(physicalJson.contains(
                "\"clipBehavior\":{\"kind\":\"enum\","
                + "\"type\":\"Clip\",\"value\":\"hardEdge\"}"), physicalJson);
        assertTrue(physicalJson.contains(
                "\"child\":{\"id\":\"6bc88608-afde-4717-be9e-6d7414eb9d12\""),
                physicalJson);

        WidgetNode directional = new WidgetNode(
                id("08efef8e-982a-4a09-8dc8-34bd1c52cdab"),
                type("flutter.widgets.ClipRSuperellipse"),
                Map.of(new PropertyName("borderRadius"),
                        new PropertyValue.BorderRadiusValue(
                                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                        first, second, first, second))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        String directionalJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), directional))),
                StandardCharsets.UTF_8);

        assertTrue(directionalJson.contains(
                "\"geometry\":{\"kind\":\"directional\","
                + "\"topStart\":{\"x\":1.5,\"y\":2.5},"
                + "\"topEnd\":{\"x\":3.5,\"y\":4.5},"
                + "\"bottomEnd\":{\"x\":1.5,\"y\":2.5},"
                + "\"bottomStart\":{\"x\":3.5,\"y\":4.5}}}"),
                directionalJson);
        assertFalse(directionalJson.contains("\"clipBehavior\""), directionalJson);
        assertTrue(directionalJson.contains("\"child\":null"), directionalJson);
        assertFalse(directionalJson.contains("clipper"), directionalJson);
    }

    @Test
    void projectsClipRSuperellipseClipperAsPresenceOnlyWithoutProjectIdentity()
            throws Exception {
        WidgetNode clipRSuperellipse = new WidgetNode(
                id("3cc07d19-0f82-49b4-ae54-61e9ac82c38e"),
                type("flutter.widgets.ClipRSuperellipse"),
                Map.of(new PropertyName("clipper"),
                        new PropertyValue.DartObjectReferenceValue(
                                Optional.of(
                                        "package:private_app/secret/rsuperellipse_clipper.dart"),
                                "SecretRoundedClipper", Optional.of("hiddenFactory"),
                                PropertyValue.DartObjectReferenceValue.Access
                                        .ZERO_ARGUMENT_INVOCATION,
                                Optional.of(true))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), clipRSuperellipse))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"clipper\":{\"kind\":\"dartObjectReferencePresence\"}"),
                json);
        for (String secret : List.of(
                "package:private_app", "rsuperellipse_clipper.dart",
                "SecretRoundedClipper", "hiddenFactory",
                "zeroArgumentInvocation", "\"constant\"", "\"libraryUri\"",
                "\"rootSymbol\"", "\"member\"", "\"access\"")) {
            assertFalse(json.contains(secret), () -> secret + " leaked in " + json);
        }
    }

    @Test
    void projectsBothClipPathBranchesAsPresenceOnlyWithoutProjectIdentity()
            throws Exception {
        for (String property : List.of("clipper", "shape")) {
            WidgetNode clipPath = new WidgetNode(
                    id("3cc07d19-0f82-49b4-ae54-61e9ac82c38e"),
                    type("flutter.widgets.ClipPath"),
                    Map.of(new PropertyName(property),
                            new PropertyValue.DartObjectReferenceValue(
                                    Optional.of("package:private_app/secret/geometry.dart"),
                                    "SecretGeometry", Optional.of("configuredFactory"),
                                    PropertyValue.DartObjectReferenceValue.Access
                                            .ZERO_ARGUMENT_INVOCATION,
                                    Optional.of(false))),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), clipPath))),
                    StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"" + property
                    + "\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"child\":null"), json);
            for (String secret : List.of("package:private_app", "geometry.dart",
                    "SecretGeometry", "configuredFactory", "zeroArgumentInvocation",
                    "\"constant\"", "\"libraryUri\"", "\"rootSymbol\"",
                    "\"member\"", "\"access\"")) {
                assertFalse(json.contains(secret), () -> secret + " leaked in " + json);
            }
        }
    }

    @Test
    void projectsExcludeSemanticsOmittedDefaultExplicitFalseAndOptionalChild()
            throws Exception {
        WidgetNode omitted = new WidgetNode(
                id("0275932d-bcc6-4bba-9154-7e86cb9489a8"),
                type("flutter.widgets.ExcludeSemantics"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        String omittedJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), omitted))),
                StandardCharsets.UTF_8);

        assertTrue(omittedJson.contains("\"protocolVersion\":19"), omittedJson);
        assertTrue(omittedJson.contains(
                "\"type\":\"flutter.widgets.ExcludeSemantics\""), omittedJson);
        assertFalse(omittedJson.contains("\"excluding\""), omittedJson);
        assertTrue(omittedJson.contains("\"child\":null"), omittedJson);

        WidgetNode included = new WidgetNode(
                id("2729cf34-eae9-42b5-b280-05842420b5cd"),
                type("flutter.widgets.ExcludeSemantics"),
                Map.of(new PropertyName("excluding"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "c9f148a4-6488-4d45-8092-3afdb02451b7",
                        "Announced child"))));
        String includedJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), included))),
                StandardCharsets.UTF_8);

        assertTrue(includedJson.contains(
                "\"excluding\":{\"kind\":\"boolean\",\"value\":false}"),
                includedJson);
        assertTrue(includedJson.contains(
                "\"child\":{\"id\":\"c9f148a4-6488-4d45-8092-3afdb02451b7\""),
                includedJson);
    }

    @Test
    void projectsTheReviewedWidgetProfileWithoutSourceOrExecutableCode()
            throws Exception {
        CanvasRenderRequest request = request(document(false));

        String json = new String(
                new CanvasModelPayloadCodec().encode(request),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"format\":\"netbeans-flutter-canvas-model\""));
        assertTrue(json.contains("\"previewMode\":\"mobile\""));
        assertTrue(json.contains("\"targetPlatform\":\"android\""));
        assertTrue(json.contains("\"protocolVersion\":19"));
        assertTrue(json.contains("\"theme\":{\"definitionId\":\"light\","));
        assertTrue(json.contains("\"seedArgb\":\"0xFF6750A4\""));
        assertTrue(json.contains("\"brightness\":\"light\""));
        assertTrue(json.contains("\"digestIdentity\":\"" + "A".repeat(64) + "\""));
        assertTrue(json.contains("\"colorScheme\":{}"));
        assertTrue(json.contains("\"textTheme\":{}"));
        assertTrue(json.contains("\"components\":{}"));
        assertTrue(json.contains("\"type\":\"flutter.material.Scaffold\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Column\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Row\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Padding\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Center\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.SizedBox\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.AspectRatio\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Opacity\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Align\""));
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.FractionallySizedBox\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.FittedBox\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.ConstrainedBox\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.UnconstrainedBox\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.LimitedBox\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Stack\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Text\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Icon\""));
        assertTrue(json.contains("\"width\":{\"kind\":\"integer\",\"value\":120}"));
        assertTrue(json.contains("\"height\":{\"kind\":\"double\",\"value\":48.5}"));
        assertTrue(json.contains("\"aspectRatio\":{\"kind\":\"double\",\"value\":1.7777777777777777}"));
        assertTrue(json.contains("\"opacity\":{\"kind\":\"double\",\"value\":0.5}"));
        assertTrue(json.contains("\"alwaysIncludeSemantics\":{\"kind\":\"boolean\",\"value\":true}"));
        assertTrue(json.contains("\"alignment\":{\"kind\":\"alignmentGeometry\","));
        assertTrue(json.contains("\"basis\":\"directional\",\"horizontal\":0.25,"));
        assertTrue(json.contains("\"widthFactor\":{\"kind\":\"integer\",\"value\":0}"));
        assertTrue(json.contains("\"heightFactor\":{\"kind\":\"double\",\"value\":1.5}"));
        assertTrue(json.contains("\"basis\":\"physical\",\"horizontal\":-0.5,"));
        assertTrue(json.contains("\"widthFactor\":{\"kind\":\"double\",\"value\":0.625}"));
        assertTrue(json.contains("\"heightFactor\":{\"kind\":\"integer\",\"value\":2}"));
        assertTrue(json.contains("\"basis\":\"physical\",\"horizontal\":0.125,"));
        assertTrue(json.contains("\"fit\":{\"kind\":\"enum\","
                + "\"type\":\"BoxFit\",\"value\":\"cover\"}"));
        assertTrue(json.contains("\"fit\":{\"kind\":\"enum\","
                + "\"type\":\"StackFit\",\"value\":\"passthrough\"}"));
        assertTrue(json.contains("\"clipBehavior\":{\"kind\":\"enum\","
                + "\"type\":\"Clip\",\"value\":\"antiAliasWithSaveLayer\"}"));
        assertTrue(json.contains("\"kind\":\"edgeInsets\",\"left\":16"));
        assertTrue(json.contains("\"crossAxisAlignment\":{\"kind\":\"enum\","
                + "\"type\":\"CrossAxisAlignment\",\"value\":\"baseline\"}"));
        assertTrue(json.contains("\"spacing\":{\"kind\":\"double\",\"value\":12.5}"));
        assertTrue(json.contains("\"semanticsLabel\":{\"kind\":\"string\","
                + "\"value\":\"Primary greeting\"}"));
        assertTrue(json.contains("\"semanticsIdentifier\":{\"kind\":\"string\","
                + "\"value\":\"primary-greeting\"}"));
        assertTrue(json.contains("\"textWidthBasis\":{\"kind\":\"enum\","
                + "\"type\":\"TextWidthBasis\",\"value\":\"longestLine\"}"));
        assertTrue(json.contains("\"selectionColor\":{\"kind\":\"color\","
                + "\"argb\":\"0xFF336699\"}"));
        assertTrue(json.contains("\"styleFontSize\":{\"kind\":\"double\",\"value\":18"));
        assertTrue(json.contains("\"styleFontWeight\":{\"kind\":\"enum\","
                + "\"type\":\"FontWeight\",\"value\":\"w700\"}"));
        assertTrue(json.contains("\"styleColor\":{\"kind\":\"color\","
                + "\"argb\":\"0xFF112233\"}"));
        assertTrue(json.contains("\"textScalerFactor\":{\"kind\":\"double\",\"value\":1.25}"));
        assertTrue(json.contains("\"icon\":{\"kind\":\"iconData\","
                + "\"codePoint\":57490,\"fontFamily\":\"MaterialIcons\","
                + "\"fontPackage\":null,"
                + "\"matchTextDirection\":true,"
                + "\"fontFamilyFallback\":[]}"), json);
        assertFalse(json.contains("home_page.dart"));
        assertFalse(json.contains("HomePage"));
        assertFalse(json.contains("managedRegions"));
        assertFalse(json.contains("sha256"));
        assertFalse(json.contains("generatorVersion"));
        assertFalse(json.contains("DartExpression"));
    }

    @Test
    void projectsConstrainedBoxRequiredBoundsAndOptionalChildInProtocolV17()
            throws Exception {
        PropertyValue.BoxConstraintsValue constraints =
                new PropertyValue.BoxConstraintsValue(
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.finite(
                                BigDecimal.valueOf(24)),
                        PropertyValue.BoxConstraintBound.finite(
                                BigDecimal.valueOf(96)));
        WidgetNode constrained = new WidgetNode(
                id("12f4f4b7-c842-49b0-b670-802f78ae4985"),
                type("flutter.widgets.ConstrainedBox"),
                Map.of(new PropertyName("constraints"), constraints),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "730b8499-781d-43b6-a38e-69720fe10047", "Bounded"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), constrained))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.ConstrainedBox\""), json);
        assertTrue(json.contains(
                "\"constraints\":{\"kind\":\"boxConstraints\""), json);
        assertTrue(json.contains("\"minWidth\":null,\"maxWidth\":null"), json);
        assertTrue(json.contains("\"minHeight\":24,\"maxHeight\":96"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"730b8499-781d-43b6-a38e-69720fe10047\""), json);
    }

    @Test
    void projectsUnconstrainedBoxTypedArgumentsAndOptionalChildInProtocolV17()
            throws Exception {
        WidgetNode unconstrained = new WidgetNode(
                id("882f9b5e-b65f-424f-95a4-b76dd6d4d66e"),
                type("flutter.widgets.UnconstrainedBox"),
                Map.of(
                        new PropertyName("textDirection"),
                                new PropertyValue.EnumValue(
                                        "TextDirection", "rtl"),
                        new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.DIRECTIONAL,
                                        new BigDecimal("0.75"),
                                        new BigDecimal("-0.25")),
                        new PropertyName("constrainedAxis"),
                                new PropertyValue.EnumValue("Axis", "vertical"),
                        new PropertyName("clipBehavior"),
                                new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "38d53ad7-dc28-4a50-803e-c8723eb82098", "Natural"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), unconstrained))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.UnconstrainedBox\""), json);
        assertTrue(json.contains("\"textDirection\":{\"kind\":\"enum\","
                + "\"type\":\"TextDirection\",\"value\":\"rtl\"}"), json);
        assertTrue(json.contains("\"alignment\":{\"kind\":\"alignmentGeometry\","
                + "\"basis\":\"directional\",\"horizontal\":0.75,"
                + "\"vertical\":-0.25}"), json);
        assertTrue(json.contains("\"constrainedAxis\":{\"kind\":\"enum\","
                + "\"type\":\"Axis\",\"value\":\"vertical\"}"), json);
        assertTrue(json.contains("\"clipBehavior\":{\"kind\":\"enum\","
                + "\"type\":\"Clip\",\"value\":\"hardEdge\"}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"38d53ad7-dc28-4a50-803e-c8723eb82098\""), json);
    }

    @Test
    void projectsLimitedBoxFiniteDoubleArgumentsAndOptionalChildInProtocolV17()
            throws Exception {
        WidgetNode limited = new WidgetNode(
                id("6602f016-01cb-44f0-b1ac-bf965968170c"),
                type("flutter.widgets.LimitedBox"),
                Map.of(
                        new PropertyName("maxWidth"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("320.5")),
                        new PropertyName("maxHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("180.25"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "48ee7298-8eed-47f5-8915-9231c42afadb", "Limited"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), limited))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.LimitedBox\""), json);
        assertTrue(json.contains(
                "\"maxWidth\":{\"kind\":\"double\",\"value\":320.5}"), json);
        assertTrue(json.contains(
                "\"maxHeight\":{\"kind\":\"double\",\"value\":180.25}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"48ee7298-8eed-47f5-8915-9231c42afadb\""), json);
    }

    @Test
    void projectsOverflowBoxTypedArgumentsAndOptionalChildInProtocolV17()
            throws Exception {
        WidgetNode overflow = new WidgetNode(
                id("3c6cb681-ff4b-47bb-860e-a73f409b56df"),
                type("flutter.widgets.OverflowBox"),
                Map.of(
                        new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.DIRECTIONAL,
                                        new BigDecimal("0.75"),
                                        new BigDecimal("-0.25")),
                        new PropertyName("minWidth"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("32.5")),
                        new PropertyName("maxWidth"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("640.25")),
                        new PropertyName("minHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("24")),
                        new PropertyName("maxHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("420.5")),
                        new PropertyName("fit"),
                                new PropertyValue.EnumValue(
                                        "OverflowBoxFit", "deferToChild")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "59fea0be-4f87-4ed3-b70a-0a4975af3189", "Overflow"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), overflow))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.OverflowBox\""), json);
        assertTrue(json.contains("\"alignment\":{\"kind\":\"alignmentGeometry\","
                + "\"basis\":\"directional\",\"horizontal\":0.75,"
                + "\"vertical\":-0.25}"), json);
        assertTrue(json.contains(
                "\"minWidth\":{\"kind\":\"double\",\"value\":32.5}"), json);
        assertTrue(json.contains(
                "\"maxWidth\":{\"kind\":\"double\",\"value\":640.25}"), json);
        assertTrue(json.contains(
                "\"minHeight\":{\"kind\":\"double\",\"value\":24}"), json);
        assertTrue(json.contains(
                "\"maxHeight\":{\"kind\":\"double\",\"value\":420.5}"), json);
        assertTrue(json.contains("\"fit\":{\"kind\":\"enum\","
                + "\"type\":\"OverflowBoxFit\",\"value\":\"deferToChild\"}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"59fea0be-4f87-4ed3-b70a-0a4975af3189\""), json);
    }

    @Test
    void projectsFlexibleOnProtocolV17() throws Exception {
        WidgetNode flexible = new WidgetNode(
                id("e201552d-d13d-4507-8aa1-35ea3ccad664"),
                type("flutter.widgets.Flexible"),
                Map.of(
                        new PropertyName("flex"),
                                new PropertyValue.IntegerValue(BigInteger.ZERO),
                        new PropertyName("fit"),
                                new PropertyValue.EnumValue("FlexFit", "tight")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "4ac0119f-f11b-4012-a08e-0aa2fe260dca", "Flexible"))));
        WidgetNode row = new WidgetNode(
                id("67cfc44e-39bc-4886-b849-6e7774563eb0"),
                type("flutter.widgets.Row"),
                Map.of(),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(flexible))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), row))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.Flexible\""), json);
        assertTrue(json.contains(
                "\"flex\":{\"kind\":\"integer\",\"value\":0}"), json);
        assertTrue(json.contains("\"fit\":{\"kind\":\"enum\","
                + "\"type\":\"FlexFit\",\"value\":\"tight\"}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"4ac0119f-f11b-4012-a08e-0aa2fe260dca\""), json);
    }

    @Test
    void projectsSpacerOnProtocolV17() throws Exception {
        WidgetNode spacer = new WidgetNode(
                id("39d10c48-9652-45d3-965a-a61002dd4a0c"),
                type("flutter.widgets.Spacer"),
                Map.of(new PropertyName("flex"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(3))),
                Map.of());
        WidgetNode row = new WidgetNode(
                id("d05cbeae-b3ca-4c59-844c-f5f4f318ef53"),
                type("flutter.widgets.Row"),
                Map.of(),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(spacer))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), row))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.Spacer\""), json);
        assertTrue(json.contains(
                "\"flex\":{\"kind\":\"integer\",\"value\":3}"), json);
    }

    @Test
    void projectsBaselineOnProtocolV17() throws Exception {
        WidgetNode baseline = new WidgetNode(
                id("82bf740b-1a96-4d8f-b0d3-345f4169776c"),
                type("flutter.widgets.Baseline"),
                Map.of(
                        new PropertyName("baseline"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("-12.5")),
                        new PropertyName("baselineType"),
                                new PropertyValue.EnumValue(
                                        "TextBaseline", "ideographic")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "66d958ff-0f49-43fe-ae5b-59ea9a55252f", "Baseline"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), baseline))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.Baseline\""), json);
        assertTrue(json.contains(
                "\"baseline\":{\"kind\":\"double\",\"value\":-12.5}"), json);
        assertTrue(json.contains("\"baselineType\":{\"kind\":\"enum\","
                + "\"type\":\"TextBaseline\",\"value\":\"ideographic\"}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"66d958ff-0f49-43fe-ae5b-59ea9a55252f\""), json);
    }

    @Test
    void projectsIntrinsicHeightOnProtocolV17() throws Exception {
        WidgetNode intrinsicHeight = new WidgetNode(
                id("aa623160-3214-4724-8550-64719ea659aa"),
                type("flutter.widgets.IntrinsicHeight"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "2d797cbd-2ddc-4532-84e6-7c735cb0e9cb",
                        "Intrinsic"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), intrinsicHeight))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.IntrinsicHeight\""), json);
        assertTrue(json.contains("\"properties\":{}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"2d797cbd-2ddc-4532-84e6-7c735cb0e9cb\""), json);
    }

    @Test
    void repaintBoundaryPayloadCarriesOnlyIdentityEmptyPropertiesAndOptionalChild() throws Exception {
        for (var child : List.of(Optional.<WidgetNode>empty(), Optional.of(text(
                "2d797cbd-2ddc-4532-84e6-7c735cb0e9cb", "Boundary child")))) {
            var boundary = new WidgetNode(id("aa623160-3214-4724-8550-64719ea659aa"),
                    type("flutter.widgets.RepaintBoundary"), Map.of(),
                    Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), boundary))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.RepaintBoundary\""), json);
            assertTrue(json.contains("\"properties\":{}"), json);
            assertTrue(json.contains(child.isEmpty() ? "\"child\":null" : "Boundary child"), json);
            for (String absent : List.of("toImage", "wrapAll", "isRepaintBoundary", "sample.dart", "creationDefault")) {
                assertFalse(json.contains(absent), json);
            }
        }
        var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.RepaintBoundary"),
                Map.of(new PropertyName("toImage"), new PropertyValue.BooleanValue(true)), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), invalid))));
    }

    @Test
    void ignorePointerPayloadRetainsBothExplicitFalseFlagsAndExactOmissionWithoutProtocolBump() throws Exception {
        var states = List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true));
        for (var ignoring : states) {
            for (var semantics : states) {
                for (boolean child : List.of(false, true)) {
                    var values = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                    ignoring.ifPresent(value -> values.put(new PropertyName("ignoring"), new PropertyValue.BooleanValue(value)));
                    semantics.ifPresent(value -> values.put(new PropertyName("ignoringSemantics"), new PropertyValue.BooleanValue(value)));
                    var node = new WidgetNode(StableId.random(), type("flutter.widgets.IgnorePointer"), values,
                            Map.of(new SlotName("child"), child ? WidgetSlot.SingleSlot.of(text(
                                    "2d797cbd-2ddc-4532-84e6-7c735cb0e9cb", "IgnorePointer child")) : WidgetSlot.SingleSlot.empty()));
                    String json = new String(new CanvasModelPayloadCodec().encode(request(
                            new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                    assertTrue(json.contains("\"protocolVersion\":19"), json);
                    assertTrue(json.contains("\"type\":\"flutter.widgets.IgnorePointer\""), json);
                    assertEquals(ignoring.isPresent(), json.contains("\"ignoring\":"), json);
                    assertEquals(semantics.isPresent(), json.contains("\"ignoringSemantics\":"), json);
                    ignoring.ifPresent(value -> assertTrue(json.contains(
                            "\"ignoring\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                    semantics.ifPresent(value -> assertTrue(json.contains(
                            "\"ignoringSemantics\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                    assertEquals(child, json.contains("IgnorePointer child"), json);
                }
            }
        }
    }

    @Test
    void ignorePointerPayloadRejectsRawOrNullBooleanSubstitutionBeforeSerialization() {
        for (String property : List.of("ignoring", "ignoringSemantics")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.DartExpressionValue("secretProjectCode()"))) {
                var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.IgnorePointer"),
                        Map.of(new PropertyName(property), value), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), invalid))));
            }
        }
    }

    @Test
    void absorbPointerPayloadRetainsBothExplicitFalseFlagsAndExactOmissionWithoutProtocolBump() throws Exception {
        var states = List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true));
        for (var absorbing : states) {
            for (var semantics : states) {
                for (boolean child : List.of(false, true)) {
                    var values = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                    absorbing.ifPresent(value -> values.put(new PropertyName("absorbing"), new PropertyValue.BooleanValue(value)));
                    semantics.ifPresent(value -> values.put(new PropertyName("ignoringSemantics"), new PropertyValue.BooleanValue(value)));
                    var node = new WidgetNode(StableId.random(), type("flutter.widgets.AbsorbPointer"), values,
                            Map.of(new SlotName("child"), child ? WidgetSlot.SingleSlot.of(text(
                                    "2d797cbd-2ddc-4532-84e6-7c735cb0e9cb", "AbsorbPointer child")) : WidgetSlot.SingleSlot.empty()));
                    String json = new String(new CanvasModelPayloadCodec().encode(request(
                            new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                    assertTrue(json.contains("\"protocolVersion\":19"), json);
                    assertTrue(json.contains("\"type\":\"flutter.widgets.AbsorbPointer\""), json);
                    assertEquals(absorbing.isPresent(), json.contains("\"absorbing\":"), json);
                    assertEquals(semantics.isPresent(), json.contains("\"ignoringSemantics\":"), json);
                    absorbing.ifPresent(value -> assertTrue(json.contains(
                            "\"absorbing\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                    semantics.ifPresent(value -> assertTrue(json.contains(
                            "\"ignoringSemantics\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                    assertEquals(child, json.contains("AbsorbPointer child"), json);
                }
            }
        }
    }

    @Test
    void absorbPointerPayloadRejectsRawOrNullBooleanSubstitutionBeforeSerialization() {
        for (String property : List.of("absorbing", "ignoringSemantics")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.DartExpressionValue("secretProjectCode()"))) {
                var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.AbsorbPointer"),
                        Map.of(new PropertyName(property), value), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), invalid))));
            }
        }
    }

    @Test
    void blockSemanticsPayloadPreservesOmissionExplicitFalseAndTrueWithOptionalChild() throws Exception {
        for (var blocking : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
            for (boolean child : List.of(false, true)) {
                Map<PropertyName, PropertyValue> values = blocking.<Map<PropertyName, PropertyValue>>map(value ->
                        Map.of(new PropertyName("blocking"), new PropertyValue.BooleanValue(value))).orElseGet(Map::of);
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.BlockSemantics"), values,
                        Map.of(new SlotName("child"), child ? WidgetSlot.SingleSlot.of(text(
                                "a763c527-e067-45f4-b348-06e8b70249ce", "BlockSemantics child")) : WidgetSlot.SingleSlot.empty()));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"protocolVersion\":19"), json);
                assertTrue(json.contains("\"type\":\"flutter.widgets.BlockSemantics\""), json);
                assertEquals(blocking.isPresent(), json.contains("\"blocking\":"), json);
                blocking.ifPresent(value -> assertTrue(json.contains(
                        "\"blocking\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                assertEquals(child, json.contains("BlockSemantics child"), json);
                assertFalse(json.contains("\"excluding\":"), json);
                assertFalse(json.contains("\"ignoringSemantics\":"), json);
            }
        }
    }

    @Test
    void blockSemanticsPayloadRejectsRawNullAndInventedOverridesBeforeSerialization() {
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.DartExpressionValue("secretProjectCode()"))) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.BlockSemantics"),
                    Map.of(new PropertyName("blocking"), value), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
        for (String invented : List.of("key", "excluding", "ignoringSemantics", "absorbing", "blockUserActions")) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.BlockSemantics"),
                    Map.of(new PropertyName(invented), new PropertyValue.BooleanValue(false)), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
    }

    @Test
    void mergeSemanticsPayloadPreservesZeroPropertiesAndOptionalChildWithoutInventedFlags() throws Exception {
        var child = text("03b89f77-f150-461f-bdce-5ba43e87e264", "MergeSemantics child");
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(
                Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)))) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.MergeSemantics"), Map.of(), slots);
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.MergeSemantics\""), json);
            assertTrue(json.contains("\"properties\":{}"), json);
            assertEquals(slots.containsKey(new SlotName("child")), json.contains("\"child\":"), json);
            assertEquals(slots.values().stream().anyMatch(slot -> ((WidgetSlot.SingleSlot) slot).child().isPresent()),
                    json.contains("MergeSemantics child"), json);
            assertFalse(json.contains("\"blocking\":"), json);
            assertFalse(json.contains("\"excluding\":"), json);
            assertFalse(json.contains("\"mergeAllDescendantsIntoThisNode\":"), json);
        }
    }

    @Test
    void mergeSemanticsPayloadRejectsEveryInventedScalarAndRawValueBeforeSerialization() {
        for (String invented : List.of("key", "blocking", "excluding", "mergeAllDescendantsIntoThisNode", "label")) {
            for (PropertyValue value : List.of(new PropertyValue.BooleanValue(true), new PropertyValue.NullValue(),
                    new PropertyValue.DartExpressionValue("secretProjectCode()"))) {
                var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.MergeSemantics"),
                        Map.of(new PropertyName(invented), value), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), invalid))));
            }
        }
    }

    @Test
    void indexedSemanticsPayloadPreservesSignedSafeIntegerEdgesAndOptionalChild() throws Exception {
        var child = text("c657fb5d-2a97-4d3b-84bc-efbe1f8ed4d9", "IndexedSemantics child");
        for (var index : List.of(dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MIN_PORTABLE_INTEGER,
                java.math.BigInteger.valueOf(-1), java.math.BigInteger.ZERO,
                dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MAX_PORTABLE_INTEGER)) {
            for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)))) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.IndexedSemantics"),
                        Map.of(new PropertyName("index"), new PropertyValue.IntegerValue(index)), slots);
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"protocolVersion\":19"), json);
                assertTrue(json.contains("\"type\":\"flutter.widgets.IndexedSemantics\""), json);
                assertTrue(json.contains("\"index\":{\"kind\":\"integer\",\"value\":" + index + "}"), json);
                assertEquals(slots.containsKey(new SlotName("child")), json.contains("\"child\":"), json);
                assertEquals(slots.values().stream().anyMatch(slot -> ((WidgetSlot.SingleSlot) slot).child().isPresent()),
                        json.contains("IndexedSemantics child"), json);
            }
        }
    }

    @Test
    void indexedSemanticsPayloadRejectsMissingNullFractionalRawAndOverflowIndexesBeforeSerialization() {
        var missing = new WidgetNode(StableId.random(), type("flutter.widgets.IndexedSemantics"), Map.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), missing))));
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true),
                new PropertyValue.StringValue("0"), new PropertyValue.DoubleValue(java.math.BigDecimal.ONE),
                new PropertyValue.DartExpressionValue("secretProjectIndex()"),
                new PropertyValue.IntegerValue(dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MIN_PORTABLE_INTEGER.subtract(java.math.BigInteger.ONE)),
                new PropertyValue.IntegerValue(dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MAX_PORTABLE_INTEGER.add(java.math.BigInteger.ONE)))) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.IndexedSemantics"),
                    Map.of(new PropertyName("index"), value), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
    }

    @Test
    void excludeFocusPayloadPreservesAllBooleanStatesAndRealRequiredChildWithoutInternalFlags() throws Exception {
        var child = text("dc52facb-cb38-41f5-ae2e-af97424c8ea4", "ExcludeFocus child");
        for (Optional<Boolean> excluding : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
            Map<PropertyName, PropertyValue> properties = excluding.<Map<PropertyName, PropertyValue>>map(value ->
                    Map.of(new PropertyName("excluding"), new PropertyValue.BooleanValue(value))).orElseGet(Map::of);
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.ExcludeFocus"), properties,
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.ExcludeFocus\""), json);
            assertTrue(json.contains("ExcludeFocus child"), json);
            assertEquals(excluding.isPresent(), json.contains("\"excluding\":"), json);
            excluding.ifPresent(value -> assertTrue(json.contains(
                    "\"excluding\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
            for (String internal : List.of("canRequestFocus", "descendantsAreFocusable", "skipTraversal", "includeSemantics", "key")) {
                assertFalse(json.contains("\"" + internal + "\":"), json);
            }
        }
    }

    @Test
    void excludeFocusPayloadRejectsMissingRequiredChildAndMalformedBooleansBeforeSerialization() {
        var child = text("df33eade-ae49-4a34-bfd2-4e0f391d9f82", "Required child");
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Map.of(new SlotName("child"), new WidgetSlot.ListSlot(List.of(child))))) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.ExcludeFocus"), Map.of(), slots);
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
                new PropertyValue.DartExpressionValue("privateProjectFunction()"))) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.ExcludeFocus"),
                    Map.of(new PropertyName("excluding"), value), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
    }

    @Test
    void iconThemePayloadPreservesAll1536FieldPresenceAndBooleanCombinationsWithRawOpacityAndShadows() throws Exception {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "Icon theme child");
        var fields = List.of("size", "fill", "weight", "grade", "opticalSize", "color", "opacity", "shadows");
        var values = List.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.valueOf(32)),
                new PropertyValue.DoubleValue(new BigDecimal("0.5")), new PropertyValue.DoubleValue(new BigDecimal("700")),
                new PropertyValue.DoubleValue(new BigDecimal("-25")), new PropertyValue.DoubleValue(new BigDecimal("48")),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")),
                new PropertyValue.DoubleValue(new BigDecimal("-0.5")),
                new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(
                        id("c4e1fdab-0c06-4dd6-9d3f-6be0f34a1aed"), new ColorSource.Literal(0x80123456L),
                        new BigDecimal("-1.25"), new BigDecimal("2.5"), new BigDecimal("4")))));
        int count = 0;
        for (boolean merge : List.of(false, true)) for (int mask = 0; mask < 256; mask++) {
            for (var scale : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
                count++;
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(new PropertyName("merge"), new PropertyValue.BooleanValue(merge));
                for (int index = 0; index < fields.size(); index++) {
                    if ((mask & (1 << index)) != 0) properties.put(new PropertyName(fields.get(index)), values.get(index));
                }
                scale.ifPresent(value -> properties.put(new PropertyName("applyTextScaling"), new PropertyValue.BooleanValue(value)));
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), properties,
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
                var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
                var codec = new CanvasModelPayloadCodec();
                byte[] bytes = codec.encode(request);
                assertArrayEquals(bytes, codec.encode(request));
                String json = new String(bytes, StandardCharsets.UTF_8);
                assertTrue(json.contains("\"protocolVersion\":19"), json);
                assertTrue(json.contains("\"type\":\"flutter.widgets.IconTheme\""), json);
                assertTrue(json.contains("Icon theme child"), json);
                assertTrue(json.contains("\"merge\":{\"kind\":\"boolean\",\"value\":" + merge + "}"), json);
                assertEquals(scale.isPresent(), json.contains("\"applyTextScaling\":"), json);
                scale.ifPresent(value -> assertTrue(json.contains("\"applyTextScaling\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                for (String field : fields) {
                    if (!field.equals("color")) assertEquals(properties.containsKey(new PropertyName(field)), json.contains("\"" + field + "\":"), json);
                }
                assertEquals(properties.containsKey(new PropertyName("color")), json.contains("material.colorScheme.primary"), json);
                if (properties.containsKey(new PropertyName("opacity"))) assertTrue(json.contains("\"opacity\":{\"kind\":\"double\",\"value\":-0.5}"), json);
                if (properties.containsKey(new PropertyName("shadows"))) {
                    assertTrue(json.contains("\"kind\":\"shadowList\""), json);
                    assertTrue(json.contains("c4e1fdab-0c06-4dd6-9d3f-6be0f34a1aed"), json);
                    assertTrue(json.contains("0x80123456"), json);
                    assertTrue(json.contains("\"offsetX\":-1.25"), json);
                    assertTrue(json.contains("\"blurRadius\":4"), json);
                }
                for (String name : List.of("fallback", "copyWith", "lerp", "resolve", "fontWeight", "blendMode")) {
                    assertFalse(json.contains("\"" + name + "\":"), json);
                }
            }
        }
        assertEquals(1536, count);
    }

    @Test
    void iconThemePayloadRejectsMissingModeOrChildInvalidDomainsKindsAndForeignTokens() {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "Icon theme child");
        for (String name : List.of("size", "fill", "weight", "grade", "opticalSize", "color", "opacity", "shadows", "applyTextScaling", "merge")) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("invalid"),
                    new PropertyValue.DartExpressionValue("IconThemeData.fallback()"))) {
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(new PropertyName("merge"), new PropertyValue.BooleanValue(false));
                properties.put(new PropertyName(name), invalid);
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), properties,
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        for (var node : List.of(
                new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))),
                new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), Map.of()),
                new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())),
                new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false),
                        new PropertyName("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))))) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        for (var entry : Map.of("size", "-1", "fill", "1.001", "weight", "0", "grade", "32768", "opticalSize", "32768", "opacity", "1e999").entrySet()) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.IconTheme"), Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false),
                    new PropertyName(entry.getKey()), new PropertyValue.DoubleValue(new BigDecimal(entry.getValue()))), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void defaultSelectionStylePayloadPreservesAllModesAnd41CursorValuesWithOptionalColors() throws Exception {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "Default selection child");
        var modes = List.of(false, true);
        var cursors = new java.util.ArrayList<Optional<String>>();
        cursors.add(Optional.empty());
        dev.flutter.netbeans.designer.catalog.DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets()
                .forEach(value -> cursors.add(Optional.of(value)));
        int combinations = 0;
        for (var mode : modes) for (var mouse : cursors) for (boolean colors : List.of(false, true)) {
            combinations++;
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("merge"), new PropertyValue.BooleanValue(mode));
            mouse.ifPresent(value -> properties.put(new PropertyName("mouseCursor"), new PropertyValue.StringValue(value)));
            if (colors) {
                properties.put(new PropertyName("cursorColor"), new PropertyValue.ColorValue(0x80123456L));
                properties.put(new PropertyName("selectionColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
            }
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.DefaultSelectionStyle"), properties,
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            var codec = new CanvasModelPayloadCodec();
            byte[] bytes = codec.encode(request);
            assertArrayEquals(bytes, codec.encode(request));
            String json = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.DefaultSelectionStyle\""), json);
            assertTrue(json.contains("Default selection child"), json);
            assertTrue(json.contains("\"merge\":"), json);
            assertEquals(mouse.isPresent(), json.contains("\"mouseCursor\":"), json);
            assertEquals(colors, json.contains("\"cursorColor\":"), json);
            assertEquals(colors, json.contains("\"selectionColor\":"), json);
            assertTrue(json.contains("\"merge\":{\"kind\":\"boolean\",\"value\":" + mode + "}"), json);
            mouse.ifPresent(value -> assertTrue(json.contains("\"mouseCursor\":{\"kind\":\"string\",\"value\":\"" + value + "\"}"), json));
            if (colors) {
                assertTrue(json.contains("0x80123456"), json);
                assertTrue(json.contains("material.colorScheme.primary"), json);
            }
            for (String name : List.of("key", "fallback", "defaultColor", "inherit")) {
                assertFalse(json.contains("\"" + name + "\":"), json);
            }
        }
        assertEquals(168, combinations);
    }

    @Test
    void defaultSelectionStylePayloadRejectsWrongKindsForeignTokensAndMissingRequiredModeOrChildBeforeEncoding() {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "Default selection child");
        for (String name : List.of("cursorColor", "selectionColor", "mouseCursor", "merge")) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("unknown"),
                    new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), new PropertyValue.DartExpressionValue("MouseCursor.defer"))) {
                var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(new PropertyName("merge"), new PropertyValue.BooleanValue(false));
                properties.put(new PropertyName(name), invalid);
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.DefaultSelectionStyle"),
                        properties, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        for (var node : List.of(
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultSelectionStyle"), Map.of(),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))),
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultSelectionStyle"),
                        Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), Map.of()),
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultSelectionStyle"),
                        Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())),
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultSelectionStyle"),
                        Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false),
                                new PropertyName("selectionColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))))) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void defaultTextHeightBehaviorPayloadPreservesAll27FlattenedCombinationsAndRealChild() throws Exception {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "Default height child");
        var states = List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true));
        var leadingStates = List.of(Optional.<String>empty(), Optional.of("even"), Optional.of("proportional"));
        int combinations = 0;
        for (var first : states) for (var last : states) for (var leading : leadingStates) {
            combinations++;
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            first.ifPresent(value -> properties.put(new PropertyName("textHeightApplyFirstAscent"), new PropertyValue.BooleanValue(value)));
            last.ifPresent(value -> properties.put(new PropertyName("textHeightApplyLastDescent"), new PropertyValue.BooleanValue(value)));
            leading.ifPresent(value -> properties.put(new PropertyName("textHeightLeadingDistribution"), new PropertyValue.EnumValue("TextLeadingDistribution", value)));
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.DefaultTextHeightBehavior"), properties,
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.DefaultTextHeightBehavior\""), json);
            assertTrue(json.contains("Default height child"), json);
            assertEquals(first.isPresent(), json.contains("\"textHeightApplyFirstAscent\":"), json);
            assertEquals(last.isPresent(), json.contains("\"textHeightApplyLastDescent\":"), json);
            assertEquals(leading.isPresent(), json.contains("\"textHeightLeadingDistribution\":"), json);
            first.ifPresent(value -> assertTrue(json.contains("\"textHeightApplyFirstAscent\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
            last.ifPresent(value -> assertTrue(json.contains("\"textHeightApplyLastDescent\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
            leading.ifPresent(value -> assertTrue(json.contains("\"textHeightLeadingDistribution\":{\"kind\":\"enum\",\"type\":\"TextLeadingDistribution\",\"value\":\"" + value + "\"}"), json));
            for (String name : List.of("key", "textHeightBehavior", "applyHeightToFirstAscent", "applyHeightToLastDescent", "leadingDistribution")) {
                assertFalse(json.contains("\"" + name + "\":"), json);
            }
        }
        assertEquals(27, combinations);
    }

    @Test
    void defaultTextHeightBehaviorPayloadRejectsInvalidKindsEnumsAndMissingChildBeforeSerialization() {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "Default height child");
        for (String name : List.of("textHeightApplyFirstAscent", "textHeightApplyLastDescent", "textHeightLeadingDistribution")) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), new PropertyValue.DartExpressionValue("TextHeightBehavior()"))) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.DefaultTextHeightBehavior"),
                        Map.of(new PropertyName(name), invalid), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        for (var node : List.of(
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultTextHeightBehavior"), Map.of(), Map.of()),
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultTextHeightBehavior"), Map.of(),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())),
                new WidgetNode(StableId.random(), type("flutter.widgets.DefaultTextHeightBehavior"),
                        Map.of(new PropertyName("textHeightLeadingDistribution"), new PropertyValue.EnumValue("TextLeadingDistribution", "invalid")),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))))) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void tickerModePayloadPreservesAllSixBooleanCombinationsAndRequiredChildWithoutTickerRuntimeData() throws Exception {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "TickerMode child");
        for (boolean enabled : List.of(false, true)) {
            for (Optional<Boolean> force : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
                var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(enabled));
                force.ifPresent(value -> properties.put(new PropertyName("forceFrames"), new PropertyValue.BooleanValue(value)));
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.TickerMode"), properties,
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"protocolVersion\":19"), json);
                assertTrue(json.contains("\"type\":\"flutter.widgets.TickerMode\""), json);
                assertTrue(json.contains("TickerMode child"), json);
                assertTrue(json.contains("\"enabled\":{\"kind\":\"boolean\",\"value\":" + enabled + "}"), json);
                assertEquals(force.isPresent(), json.contains("\"forceFrames\":"), json);
                force.ifPresent(value -> assertTrue(json.contains("\"forceFrames\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
                for (String name : List.of("key", "merge", "muted", "ticker", "vsync", "elapsed", "visible")) {
                    assertFalse(json.contains("\"" + name + "\":"), json);
                }
            }
        }
    }

    @Test
    void tickerModePayloadRejectsMissingEnabledRequiredChildAndMalformedBooleanValues() {
        var child = text("a0c5b9b2-30b8-4ad4-995c-2e630be728ba", "TickerMode child");
        for (String name : List.of("enabled", "forceFrames")) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), new PropertyValue.DartExpressionValue("false"))) {
                var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(true));
                properties.put(new PropertyName(name), invalid);
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.TickerMode"), properties,
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        for (var node : List.of(
                new WidgetNode(StableId.random(), type("flutter.widgets.TickerMode"), Map.of(),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))),
                new WidgetNode(StableId.random(), type("flutter.widgets.TickerMode"),
                        Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true)), Map.of()),
                new WidgetNode(StableId.random(), type("flutter.widgets.TickerMode"),
                        Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true)),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())))) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void visibilityPayloadPreservesBothBranchesAndAllBooleanStatesWithoutRuntimeOrSourceData() throws Exception {
        var child = text("dc52facb-cb38-41f5-ae2e-af97424c8ea4", "Visibility child");
        var replacement = text("df33eade-ae49-4a34-bfd2-4e0f391d9f82", "Visibility replacement");
        for (Optional<Boolean> state : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            state.ifPresent(value -> List.of("visible", "maintainState", "maintainAnimation", "maintainSize",
                    "maintainSemantics", "maintainInteractivity", "maintainFocusability").forEach(name ->
                    properties.put(new PropertyName(name), new PropertyValue.BooleanValue(value))));
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.Visibility"), properties,
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child),
                            new SlotName("replacement"), WidgetSlot.SingleSlot.of(replacement)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.Visibility\""), json);
            assertTrue(json.contains("Visibility child"), json);
            assertTrue(json.contains("Visibility replacement"), json);
            for (String name : List.of("visible", "maintainState", "maintainAnimation", "maintainSize",
                    "maintainSemantics", "maintainInteractivity", "maintainFocusability")) {
                assertEquals(state.isPresent(), json.contains("\"" + name + "\":"), json);
                state.ifPresent(value -> assertTrue(json.contains("\"" + name + "\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
            }
            for (String name : List.of("maintain", "canRequestFocus", "descendantsAreFocusable", "includeSemantics", "key")) {
                assertFalse(json.contains("\"" + name + "\":"), json);
            }
        }
    }

    @Test
    void visibilityPayloadRejectsInvalidDependenciesMalformedBooleansAndMissingChild() {
        var child = text("dc52facb-cb38-41f5-ae2e-af97424c8ea4", "Visibility child");
        for (String name : List.of("maintainAnimation", "maintainSize", "maintainSemantics", "maintainInteractivity", "maintainFocusability")) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.Visibility"),
                    Map.of(new PropertyName(name), new PropertyValue.BooleanValue(true)),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        for (var node : List.of(
                new WidgetNode(StableId.random(), type("flutter.widgets.Visibility"), Map.of(), Map.of()),
                new WidgetNode(StableId.random(), type("flutter.widgets.Visibility"), Map.of(new PropertyName("visible"), new PropertyValue.NullValue()),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))))) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void excludeFocusTraversalPayloadPreservesAllBooleanStatesAndRealRequiredChildWithoutInternalFlags() throws Exception {
        var child = text("dc52facb-cb38-41f5-ae2e-af97424c8ea4", "ExcludeFocusTraversal child");
        for (Optional<Boolean> excluding : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
            Map<PropertyName, PropertyValue> properties = excluding.<Map<PropertyName, PropertyValue>>map(value ->
                    Map.of(new PropertyName("excluding"), new PropertyValue.BooleanValue(value))).orElseGet(Map::of);
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.ExcludeFocusTraversal"), properties,
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
            assertTrue(json.contains("\"type\":\"flutter.widgets.ExcludeFocusTraversal\""), json);
            assertTrue(json.contains("ExcludeFocusTraversal child"), json);
            assertEquals(excluding.isPresent(), json.contains("\"excluding\":"), json);
            excluding.ifPresent(value -> assertTrue(json.contains(
                    "\"excluding\":{\"kind\":\"boolean\",\"value\":" + value + "}"), json));
            for (String internal : List.of("canRequestFocus", "descendantsAreFocusable", "descendantsAreTraversable", "skipTraversal", "includeSemantics", "key")) {
                assertFalse(json.contains("\"" + internal + "\":"), json);
            }
        }
    }

    @Test
    void excludeFocusTraversalPayloadRejectsMissingRequiredChildAndMalformedBooleansBeforeSerialization() {
        var child = text("df33eade-ae49-4a34-bfd2-4e0f391d9f82", "Required child");
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Map.of(new SlotName("child"), new WidgetSlot.ListSlot(List.of(child))))) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.ExcludeFocusTraversal"), Map.of(), slots);
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
                new PropertyValue.DartExpressionValue("privateProjectFunction()"))) {
            var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.ExcludeFocusTraversal"),
                    Map.of(new PropertyName("excluding"), value), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
    }

    @Test
    void projectsIntrinsicWidthPropertiesAndChildOnProtocolV17()
            throws Exception {
        WidgetNode intrinsicWidth = new WidgetNode(
                id("e589812d-a08a-4476-9443-0a70af6c708f"),
                type("flutter.widgets.IntrinsicWidth"),
                Map.of(
                        new PropertyName("stepWidth"),
                                new PropertyValue.DoubleValue(BigDecimal.ZERO),
                        new PropertyName("stepHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("12.5"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "65c34b52-ff29-40a4-895a-6bddfb757483",
                        "Intrinsic width"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), intrinsicWidth))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.IntrinsicWidth\""), json);
        assertTrue(json.contains(
                "\"stepWidth\":{\"kind\":\"double\",\"value\":0}"), json);
        assertTrue(json.contains(
                "\"stepHeight\":{\"kind\":\"double\",\"value\":12.5}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"65c34b52-ff29-40a4-895a-6bddfb757483\""), json);
    }

    @Test
    void projectsExplicitOffstageBooleanAndChildInProtocolV17()
            throws Exception {
        WidgetNode offstage = new WidgetNode(
                id("c70084d4-2ccf-423e-b5ce-8108bbf22f3c"),
                type("flutter.widgets.Offstage"),
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "592e6585-79bd-4da2-a6f5-3c0b8d528258",
                        "Visible child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), offstage))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.Offstage\""), json);
        assertTrue(json.contains(
                "\"offstage\":{\"kind\":\"boolean\",\"value\":false}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"592e6585-79bd-4da2-a6f5-3c0b8d528258\""), json);
    }

    @Test
    void projectsTypedSizedOverflowBoxSurfaceOnProtocolV17() throws Exception {
        WidgetNode box = new WidgetNode(
                id("544dad42-0ae5-42a1-8d36-1035b0215c68"),
                type("flutter.widgets.SizedOverflowBox"),
                Map.of(
                        new PropertyName("size"),
                        new PropertyValue.SizeValue(
                                new BigDecimal("120.5"), BigDecimal.ZERO),
                        new PropertyName("alignment"),
                        new PropertyValue.AlignmentGeometryValue(
                                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                                BigDecimal.ONE, BigDecimal.ONE.negate())),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "3d020e1c-a143-43f9-9cb7-54005dd2ff9b",
                        "Overflow child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), box))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.SizedOverflowBox\""), json);
        assertTrue(json.contains(
                "\"size\":{\"kind\":\"size\",\"width\":120.5,"
                + "\"height\":0}"), json);
        assertTrue(json.contains(
                "\"alignment\":{\"kind\":\"alignmentGeometry\","
                + "\"basis\":\"directional\",\"horizontal\":1,"
                + "\"vertical\":-1}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"3d020e1c-a143-43f9-9cb7-54005dd2ff9b\""), json);
    }

    @Test
    void projectsTypedTransformNewSurfaceOnProtocolV17() throws Exception {
        WidgetNode transform = new WidgetNode(
                id("ce5a8755-4984-4d25-9a3b-0f3dfa286d72"),
                type("flutter.widgets.Transform"),
                Map.of(
                        new PropertyName("transform"),
                        new PropertyValue.Matrix4Value(List.of(
                                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                                new BigDecimal("12.5"), new BigDecimal("-8.25"),
                                BigDecimal.ZERO, BigDecimal.ONE)),
                        new PropertyName("origin"),
                        new PropertyValue.OffsetValue(
                                new BigDecimal("-4.5"), new BigDecimal("2.25")),
                        new PropertyName("alignment"),
                        new PropertyValue.AlignmentGeometryValue(
                                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                                BigDecimal.ONE, BigDecimal.ONE.negate()),
                        new PropertyName("transformHitTests"),
                        new PropertyValue.BooleanValue(false),
                        new PropertyName("filterQuality"),
                        new PropertyValue.EnumValue("FilterQuality", "high")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "78f43c7c-af2c-4c5e-a944-0cfb5bf1a20f",
                        "Transformed child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), transform))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.Transform\""), json);
        assertTrue(json.contains(
                "\"transform\":{\"kind\":\"matrix4\",\"storage\":["
                + "1,0,0,0,0,1,0,0,0,0,1,0,12.5,-8.25,0,1]}"), json);
        assertTrue(json.contains(
                "\"origin\":{\"kind\":\"offset\",\"dx\":-4.5,\"dy\":2.25}"), json);
        assertTrue(json.contains(
                "\"alignment\":{\"kind\":\"alignmentGeometry\","
                + "\"basis\":\"directional\",\"horizontal\":1,\"vertical\":-1}"), json);
        assertTrue(json.contains(
                "\"transformHitTests\":{\"kind\":\"boolean\",\"value\":false}"), json);
        assertTrue(json.contains(
                "\"filterQuality\":{\"kind\":\"enum\","
                + "\"type\":\"FilterQuality\",\"value\":\"high\"}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"78f43c7c-af2c-4c5e-a944-0cfb5bf1a20f\""), json);
    }

    @Test
    void projectsRotatedBoxSignedTurnsAndChildInProtocolV17()
            throws Exception {
        WidgetNode rotated = new WidgetNode(
                id("72c25af9-7cb4-4bad-b149-bcc4639626d8"),
                type("flutter.widgets.RotatedBox"),
                Map.of(new PropertyName("quarterTurns"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(-3))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "e6a3ba4c-b00a-4f73-9355-40c26a4c03c8",
                        "Rotated child"))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), rotated))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains(
                "\"type\":\"flutter.widgets.RotatedBox\""), json);
        assertTrue(json.contains(
                "\"quarterTurns\":{\"kind\":\"integer\",\"value\":-3}"), json);
        assertTrue(json.contains(
                "\"child\":{\"id\":\"e6a3ba4c-b00a-4f73-9355-40c26a4c03c8\""), json);
    }

    @Test
    void projectsListBodyAxisReverseAndOrderedChildrenInProtocolV17()
            throws Exception {
        WidgetNode listBody = new WidgetNode(
                id("499db53a-46af-478b-822a-64b53af792e3"),
                type("flutter.widgets.ListBody"),
                Map.of(
                        new PropertyName("mainAxis"),
                        new PropertyValue.EnumValue("Axis", "horizontal"),
                        new PropertyName("reverse"),
                        new PropertyValue.BooleanValue(true)),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        text("eb28269c-0422-4376-8b55-c022e8dba1df", "First"),
                        text("22aa2516-fab4-4294-a3cd-7ec8513f438b", "Second")))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), listBody))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.ListBody\""), json);
        assertTrue(json.contains(
                "\"mainAxis\":{\"kind\":\"enum\","
                + "\"type\":\"Axis\",\"value\":\"horizontal\"}"), json);
        assertTrue(json.contains(
                "\"reverse\":{\"kind\":\"boolean\",\"value\":true}"), json);
        int first = json.indexOf("eb28269c-0422-4376-8b55-c022e8dba1df");
        int second = json.indexOf("22aa2516-fab4-4294-a3cd-7ec8513f438b");
        assertTrue(first >= 0 && first < second, json);
    }

    @Test
    void projectsIndexedStackExplicitNullAndOrderedChildrenInProtocolV17()
            throws Exception {
        WidgetNode indexedStack = new WidgetNode(
                id("8518b429-751c-4e46-9624-312f6491c6bd"),
                type("flutter.widgets.IndexedStack"),
                Map.ofEntries(
                        Map.entry(new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.DIRECTIONAL,
                                        new BigDecimal("0.25"),
                                        new BigDecimal("-0.5"))),
                        Map.entry(new PropertyName("textDirection"),
                                new PropertyValue.EnumValue("TextDirection", "rtl")),
                        Map.entry(new PropertyName("clipBehavior"),
                                new PropertyValue.EnumValue("Clip", "antiAlias")),
                        Map.entry(new PropertyName("sizing"),
                                new PropertyValue.EnumValue("StackFit", "expand")),
                        Map.entry(new PropertyName("index"),
                                new PropertyValue.NullValue())),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        text("da550e63-b66a-4727-a81d-1fb72823042d", "First"),
                        text("ae2e975e-d73f-4aa5-a798-aa247af8604c", "Second")))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), indexedStack))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.IndexedStack\""), json);
        assertTrue(json.contains("\"index\":{\"kind\":\"null\"}"), json);
        assertFalse(json.contains("\"index\":{\"kind\":\"null\",\"value\""), json);
        assertTrue(json.contains("\"sizing\":{\"kind\":\"enum\","), json);
        int first = json.indexOf("da550e63-b66a-4727-a81d-1fb72823042d");
        int second = json.indexOf("ae2e975e-d73f-4aa5-a798-aa247af8604c");
        assertTrue(first >= 0 && first < second, json);

        WidgetNode omitted = new WidgetNode(
                id("3a0eacb9-4ba7-4a49-97c4-0cca0d97b53c"),
                type("flutter.widgets.IndexedStack"),
                Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())));
        String omittedJson = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), omitted))),
                StandardCharsets.UTF_8);
        assertFalse(omittedJson.contains("\"index\""), omittedJson);
        assertTrue(omittedJson.contains(
                "\"children\":{\"kind\":\"list\",\"children\":[]}"), omittedJson);
    }

    @Test
    void projectsOverflowBarFullSurfaceAndOrderedChildrenInProtocolV17()
            throws Exception {
        WidgetNode overflowBar = new WidgetNode(
                id("6f2b8f33-99c6-49db-aede-b12defe4fe82"),
                type("flutter.widgets.OverflowBar"),
                Map.of(
                        new PropertyName("spacing"),
                        new PropertyValue.DoubleValue(new BigDecimal("-3.5")),
                        new PropertyName("alignment"),
                        new PropertyValue.EnumValue(
                                "MainAxisAlignment", "spaceEvenly"),
                        new PropertyName("overflowSpacing"),
                        new PropertyValue.DoubleValue(new BigDecimal("8.25")),
                        new PropertyName("overflowAlignment"),
                        new PropertyValue.EnumValue(
                                "OverflowBarAlignment", "end"),
                        new PropertyName("overflowDirection"),
                        new PropertyValue.EnumValue("VerticalDirection", "up"),
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl")),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        text("8494d57f-c4b2-4884-a6f6-389f410a7b0e", "First"),
                        text("ea94db4d-3b30-49f4-a779-89ec95325a85", "Second")))));

        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), overflowBar))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"type\":\"flutter.widgets.OverflowBar\""), json);
        assertTrue(json.contains(
                "\"spacing\":{\"kind\":\"double\",\"value\":-3.5}"), json);
        assertTrue(json.contains(
                "\"alignment\":{\"kind\":\"enum\","
                + "\"type\":\"MainAxisAlignment\",\"value\":\"spaceEvenly\"}"), json);
        assertTrue(json.contains(
                "\"overflowSpacing\":{\"kind\":\"double\",\"value\":8.25}"), json);
        assertTrue(json.contains(
                "\"overflowAlignment\":{\"kind\":\"enum\","
                + "\"type\":\"OverflowBarAlignment\",\"value\":\"end\"}"), json);
        assertTrue(json.contains(
                "\"overflowDirection\":{\"kind\":\"enum\","
                + "\"type\":\"VerticalDirection\",\"value\":\"up\"}"), json);
        assertTrue(json.contains(
                "\"textDirection\":{\"kind\":\"enum\","
                + "\"type\":\"TextDirection\",\"value\":\"rtl\"}"), json);
        int first = json.indexOf("8494d57f-c4b2-4884-a6f6-389f410a7b0e");
        int second = json.indexOf("ea94db4d-3b30-49f4-a779-89ec95325a85");
        assertTrue(first >= 0 && first < second, json);
    }

    @Test
    void mapInsertionOrderCannotChangeCanonicalBytes() throws Exception {
        byte[] first = new CanvasModelPayloadCodec().encode(request(document(false)));
        byte[] second = new CanvasModelPayloadCodec().encode(request(document(true)));

        assertArrayEquals(first, second);
    }

    @Test
    void elevatedButtonLayerPayloadStripsAllReferenceFormsAndKeepsOmissionAndNullChild() throws Exception {
        var refs = List.of(
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "privateLayer", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_layers/style.dart"),
                        "privateOwner", Optional.of("privateGetter"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_layers/style.dart"),
                        "privateOwner", Optional.of("privateFactory"),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)));
        for (var reference : refs) {
            for (int mask = 0; mask < 4; mask++) {
                var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                if ((mask & 1) != 0) properties.put(new PropertyName("styleBackgroundBuilder"), reference);
                if ((mask & 2) != 0) properties.put(new PropertyName("styleForegroundBuilder"), reference);
                var button = new WidgetNode(DOCUMENT_ID, type("flutter.material.ElevatedButton"), properties,
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), button))), StandardCharsets.UTF_8);
                assertEquals((mask & 1) != 0, json.contains("\"styleBackgroundBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
                assertEquals((mask & 2) != 0, json.contains("\"styleForegroundBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
                assertTrue(json.contains("\"child\":{\"kind\":\"single\",\"child\":null}"), json);
                assertFalse(json.contains("\"clipBehavior\":"), json);
                for (String forbidden : List.of("privateLayer", "privateOwner", "privateGetter", "privateFactory",
                        "private_layers", "rootSymbol", "zeroArgumentInvocation", "ButtonLayerBuilder")) {
                    assertFalse(json.contains(forbidden), json);
                }
            }
        }
    }

    @Test
    void elevatedButtonLayerPayloadRejectsNullCallbackAndExecutableSourceKinds() {
        for (String name : List.of("styleBackgroundBuilder", "styleForegroundBuilder")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("buildLayer"),
                    new PropertyValue.StringValue("layer"), new PropertyValue.DartExpressionValue("(_, _, child) => child!"))) {
                var button = new WidgetNode(DOCUMENT_ID, type("flutter.material.ElevatedButton"),
                        Map.of(new PropertyName(name), value), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), button))), name + ": " + value);
            }
        }
    }

    @Test
    void projectsElevatedButtonCallbackPresenceWithoutExecutableIdentifier() throws Exception {
        WidgetNode button = new WidgetNode(
                id("d5659c06-8da0-45d1-91c8-75eef08d9442"),
                type("flutter.material.ElevatedButton"),
                Map.of(
                        new PropertyName("enabled"),
                        new PropertyValue.BooleanValue(true),
                        new PropertyName("onPressed"),
                        new PropertyValue.CallbackValue("handlePress")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID, source(), button);
        String json = new String(
                new CanvasModelPayloadCodec().encode(request(document)),
                StandardCharsets.UTF_8);

        assertTrue(json.contains(
                "\"onPressed\":{\"kind\":\"callbackPresence\"}"), json);
        assertFalse(json.contains("handlePress"), json);
        assertFalse(json.contains("configured"), json);
    }

    @Test
    void projectsThemeBoundComplexTextValuesInProtocolV17() throws Exception {
        WidgetNode text = new WidgetNode(
                id("5ab6c203-3d32-489c-9d7a-7c14f29637cb"),
                type("flutter.widgets.Text"),
                Map.ofEntries(
                        Map.entry(new PropertyName("data"),
                                new PropertyValue.StringValue("Styled")),
                        Map.entry(new PropertyName("selectionColor"),
                                new PropertyValue.ThemeTokenValue(
                                        new ThemeToken("material.colorScheme.primary"))),
                        Map.entry(new PropertyName("styleThemeTextStyle"),
                                new PropertyValue.ThemeTokenValue(
                                        new ThemeToken("material.textTheme.bodyLarge"))),
                        Map.entry(new PropertyName("styleForeground"),
                                PropertyValue.PaintValue.defaults(new ColorSource.Theme(
                                        new ThemeToken("material.colorScheme.secondary")))),
                        Map.entry(new PropertyName("styleShadows"),
                                new PropertyValue.ShadowListValue(List.of(
                                        new PropertyValue.ShadowListValue.Shadow(
                                                id("192489fb-3bbb-46c5-9bac-c988f412218c"),
                                                new ColorSource.Literal(0x80445566L),
                                                BigDecimal.ZERO, BigDecimal.ONE,
                                                BigDecimal.valueOf(2))))),
                        Map.entry(new PropertyName("styleFontFeatures"),
                                new PropertyValue.FontFeatureListValue(List.of(
                                        new PropertyValue.FontFeatureListValue.FontFeature(
                                                id("d8ca6ff9-1aa5-4bb1-944b-fdd475b5359d"),
                                                "liga", 1)))),
                        Map.entry(new PropertyName("styleFontVariations"),
                                new PropertyValue.FontVariationListValue(List.of(
                                        new PropertyValue.FontVariationListValue.FontVariation(
                                                id("2115406c-c05d-4323-81a2-7cfe7ea35dc4"),
                                                "wght", BigDecimal.valueOf(700)))))),
                Map.of());
        DesignerDocument document = new DesignerDocument(DOCUMENT_ID, source(), text);

        String json = new String(
                new CanvasModelPayloadCodec().encode(request(document)),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"selectionColor\":{\"kind\":\"themeToken\","
                + "\"token\":\"material.colorScheme.primary\"}"), json);
        assertTrue(json.contains("\"styleThemeTextStyle\":{\"kind\":\"themeToken\","
                + "\"token\":\"material.textTheme.bodyLarge\"}"), json);
        assertTrue(json.contains("\"styleForeground\":{\"kind\":\"paint\","
                + "\"color\":{\"kind\":\"theme\","
                + "\"token\":\"material.colorScheme.secondary\"}"), json);
        assertTrue(json.contains("\"blendMode\":\"srcOver\""), json);
        assertTrue(json.contains("\"styleShadows\":{\"kind\":\"shadowList\","), json);
        assertTrue(json.contains("\"color\":{\"kind\":\"literal\","
                + "\"argb\":\"0x80445566\"}"), json);
        assertTrue(json.contains("\"styleFontFeatures\":{\"kind\":\"fontFeatureList\","), json);
        assertTrue(json.contains("\"tag\":\"liga\",\"value\":1"), json);
        assertTrue(json.contains("\"styleFontVariations\":{\"kind\":\"fontVariationList\","), json);
        Matcher variation = Pattern.compile(
                "\\\"axis\\\":\\\"wght\\\",\\\"value\\\":([^,}]+)")
                .matcher(json);
        assertTrue(variation.find(), json);
        assertEquals(0, BigDecimal.valueOf(700).compareTo(
                new BigDecimal(variation.group(1))));
    }

    @Test
    void projectsResolvedThemeOverridesAndComponentsInCanonicalProtocolV17Order()
            throws Exception {
        CanvasThemeTextStyleOverride body = new CanvasThemeTextStyleOverride(
                Optional.of(new CanvasThemeColorValue.ColorRole("onSurface")),
                Optional.empty(), Optional.of(16.0d), Optional.of("w600"),
                Optional.of("italic"), Optional.empty(), Optional.empty(),
                Optional.of(1.4d), Optional.of("Noto Sans"),
                Optional.of(java.util.Set.of("lineThrough", "underline")),
                Optional.of(new CanvasThemeColorValue.Literal(0xFF123456)),
                Optional.of("dashed"), Optional.of(2.0d));
        CanvasResolvedTheme resolved = new CanvasResolvedTheme(
                "light", 0xFF6750A4, CanvasThemeBrightness.LIGHT,
                "B".repeat(64),
                Map.of("surface", 0xFF020202, "primary", 0xFF010101),
                Map.of("bodyMedium", body),
                Map.of(
                        CanvasThemeComponentColorRole.APP_BAR_FOREGROUND,
                        new CanvasThemeColorValue.ColorRole("onPrimary"),
                        CanvasThemeComponentColorRole.SCAFFOLD_BACKGROUND,
                        new CanvasThemeColorValue.Literal(0xFF030303),
                        CanvasThemeComponentColorRole.ELEVATED_BUTTON_OVERLAY_PRESSED,
                        new CanvasThemeColorValue.Literal(0x44010101)));
        CanvasRenderProfile profile = new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID,
                new CanvasViewport(390, 844), new CanvasDevicePixelRatio(1),
                resolved, new CanvasLocale("en-US"), new CanvasTextScaleFactor(1),
                new CanvasEngineIdentity("3.44.8", "framework", "engine", "3.12.0"));

        String json = new String(new CanvasModelPayloadCodec().encode(
                request(profile, document(false))), StandardCharsets.UTF_8);

        assertTrue(json.indexOf("\"primary\":\"0xFF010101\"")
                < json.indexOf("\"surface\":\"0xFF020202\""), json);
        assertTrue(json.contains("\"color\":{\"kind\":\"colorScheme\","
                + "\"role\":\"onSurface\"}"), json);
        assertTrue(json.contains("\"decoration\":[\"underline\",\"lineThrough\"]"), json);
        assertTrue(json.contains("\"decorationColor\":{\"kind\":\"argb\","
                + "\"argb\":\"0xFF123456\"}"), json);
        assertTrue(json.indexOf("\"scaffold.backgroundColor\"")
                < json.indexOf("\"appBar.foregroundColor\""), json);
        assertTrue(json.indexOf("\"appBar.foregroundColor\"")
                < json.indexOf("\"elevatedButton.overlayColor.pressed\""), json);
        assertTrue(json.contains("\"appBar.foregroundColor\":{"
                + "\"kind\":\"colorScheme\",\"role\":\"onPrimary\"}"), json);
    }

    @Test
    void rejectsNonRepresentableComplexNumbersBeforeCanvasProjection() {
        BigDecimal overflow = new BigDecimal("1E+400");
        BigDecimal underflow = new BigDecimal("1E-400");
        BigDecimal excessPrecision = new BigDecimal("1.234567890123456789");

        assertCanvasAdmissionRejects(new PropertyValue.PaintValue(
                new ColorSource.Literal(0xFF112233L),
                PropertyValue.PaintValue.BlendMode.SRC_OVER,
                PropertyValue.PaintValue.Style.FILL,
                overflow,
                PropertyValue.PaintValue.StrokeCap.BUTT,
                PropertyValue.PaintValue.StrokeJoin.MITER,
                BigDecimal.valueOf(4),
                true,
                PropertyValue.PaintValue.FilterQuality.NONE,
                false,
                Optional.empty()), "styleForeground");
        assertCanvasAdmissionRejects(new PropertyValue.ShadowListValue(List.of(
                new PropertyValue.ShadowListValue.Shadow(
                        id("c53f7722-6a78-4d72-b87f-7279370fc2c7"),
                        new ColorSource.Literal(0xFF112233L),
                        underflow,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO))), "styleShadows");
        assertCanvasAdmissionRejects(new PropertyValue.FontVariationListValue(List.of(
                new PropertyValue.FontVariationListValue.FontVariation(
                        id("188bc869-8f94-444a-a4f9-e83a8248042b"),
                        "GRAD",
                        excessPrecision))), "styleFontVariations");
    }

    @Test
    void projectsDirectionalPaddingWithoutResolvingStartAndEnd() throws Exception {
        WidgetNode padding = new WidgetNode(
                id("66ad67f4-e69e-457c-8a08-958149072507"),
                type("flutter.widgets.Padding"),
                Map.of(new PropertyName("padding"),
                        new PropertyValue.EdgeInsetsDirectionalValue(
                                BigDecimal.ONE, BigDecimal.valueOf(2),
                                BigDecimal.valueOf(3), BigDecimal.valueOf(4))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        DesignerDocument document = new DesignerDocument(DOCUMENT_ID, source(), padding);

        String json = new String(new CanvasModelPayloadCodec().encode(
                request(document)), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"kind\":\"edgeInsetsDirectional\""), json);
        assertTrue(json.contains("\"start\":1"), json);
        assertTrue(json.contains("\"end\":3"), json);
        assertFalse(json.contains("\"left\":"), json);
        assertFalse(json.contains("\"right\":"), json);
    }

    @Test
    void projectsStructuredContainerValuesInProtocolV17() throws Exception {
        PropertyValue.AlignmentGeometryValue alignment =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("0.25"), new BigDecimal("-0.5"));
        ColorSource primary = new ColorSource.Theme(
                new ThemeToken("material.colorScheme.primary"));
        PropertyValue.BoxDecorationValue.GradientStop first =
                new PropertyValue.BoxDecorationValue.GradientStop(
                        id("2e147e17-a407-40b0-a43d-ff630bf29bee"),
                        primary, BigDecimal.ZERO);
        PropertyValue.BoxDecorationValue.GradientStop second =
                new PropertyValue.BoxDecorationValue.GradientStop(
                        id("9636df3c-b0f5-4c4a-bb13-6990dc476ccd"),
                        new ColorSource.Literal(0xFFFFFFFFL), BigDecimal.ONE);
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(primary), Optional.empty(), Optional.empty(),
                        List.of(),
                        Optional.of(new PropertyValue.BoxDecorationValue.LinearGradient(
                                alignment,
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                                        BigDecimal.ONE, BigDecimal.ONE),
                                List.of(first, second),
                                PropertyValue.BoxDecorationValue.TileMode.MIRROR,
                                Optional.of(new BigDecimal("0.125")))),
                        Optional.of(PropertyValue.PaintValue.BlendMode.MULTIPLY),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        WidgetNode container = new WidgetNode(
                id("6130c00c-8568-48f5-a835-e88a01fa0e6f"),
                type("flutter.widgets.Container"),
                Map.ofEntries(
                        Map.entry(new PropertyName("alignment"), alignment),
                        Map.entry(new PropertyName("constraints"),
                                new PropertyValue.BoxConstraintsValue(
                                        BigDecimal.TEN, Optional.empty(),
                                        BigDecimal.valueOf(20),
                                        Optional.of(BigDecimal.valueOf(500)))),
                        Map.entry(new PropertyName("transform"),
                                new PropertyValue.Matrix4Value(
                                        java.util.stream.IntStream.range(0, 16)
                                                .mapToObj(BigDecimal::valueOf)
                                                .toList())),
                        Map.entry(new PropertyName("decoration"), decoration),
                        Map.entry(new PropertyName("clipBehavior"),
                                new PropertyValue.EnumValue("Clip", "hardEdge"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));

        String json = new String(new CanvasModelPayloadCodec().encode(
                request(new DesignerDocument(DOCUMENT_ID, source(), container))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"alignment\":{\"kind\":\"alignmentGeometry\","), json);
        assertTrue(json.contains("\"basis\":\"directional\",\"horizontal\":0.25,"), json);
        assertTrue(json.contains("\"constraints\":{\"kind\":\"boxConstraints\","), json);
        assertTrue(json.contains("\"maxWidth\":null"), json);
        assertTrue(json.contains("\"transform\":{\"kind\":\"matrix4\","), json);
        assertTrue(json.contains("\"storage\":[0,1,2,3,4,5,6,7,8,9,1E+1,11,12,13,14,15]"), json);
        assertTrue(json.contains("\"decoration\":{\"kind\":\"boxDecoration\","), json);
        assertTrue(json.contains("\"token\":\"material.colorScheme.primary\""), json);
        assertTrue(json.contains("\"gradient\":{\"kind\":\"linear\""), json);
        assertTrue(json.contains("\"backgroundBlendMode\":\"multiply\""), json);
        assertTrue(json.contains("\"shape\":\"rectangle\""), json);
    }

    @Test
    void projectsResolvedDecorationImageWithoutPathsOrCallbackCode()
            throws Exception {
        PropertyValue.ImageProviderValue provider =
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                        "assets/background.png",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new PropertyValue.ImageProviderValue
                                .ResizeImageConfig(
                                        Optional.of(320),
                                        Optional.empty(),
                                        PropertyValue.ImageProviderValue
                                                .ResizePolicy.FIT,
                                        false)));
        PropertyValue.DecorationImageValue image =
                new PropertyValue.DecorationImageValue(
                        provider,
                        Optional.of(new PropertyValue.CallbackValue(
                                "handleImageError")),
                        Optional.of(new PropertyValue.DecorationImageValue.Mode(
                                new ColorSource.Theme(new ThemeToken(
                                        "material.colorScheme.primary")),
                                PropertyValue.PaintValue.BlendMode.SRC_OVER)),
                        Optional.of(PropertyValue.DecorationImageValue.BoxFit.COVER),
                        new PropertyValue.AlignmentGeometryValue(
                                PropertyValue.AlignmentGeometryValue
                                        .HorizontalBasis.DIRECTIONAL,
                                new BigDecimal("0.25"),
                                BigDecimal.ZERO),
                        Optional.empty(),
                        PropertyValue.DecorationImageValue.ImageRepeat.REPEAT_X,
                        true,
                        new BigDecimal("1.5"),
                        new BigDecimal("0.75"),
                        PropertyValue.PaintValue.FilterQuality.HIGH,
                        true,
                        true);
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.empty(),
                        Optional.of(image),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        WidgetNode container = new WidgetNode(
                id("6130c00c-8568-48f5-a835-e88a01fa0e6f"),
                type("flutter.widgets.Container"),
                Map.of(new PropertyName("decoration"), decoration),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID, source(), container);
        CanvasImageResource resource = CanvasImageResource.create(
                CanvasImageFormat.PNG, 64, 32, new byte[]{1, 2, 3, 4});
        CanvasImageAssetId assetId = CanvasImageAssetId.application(
                "assets/background.png");
        CanvasImageResourceBundle bundle = new CanvasImageResourceBundle(
                List.of(new CanvasImageAsset(
                        assetId,
                        resource.resourceId(),
                        List.of(new CanvasImageVariant(
                                BigDecimal.ONE, resource.resourceId())))),
                List.of(resource));

        String json = new String(
                new CanvasModelPayloadCodec().encode(request(
                        PROFILE, document, bundle)),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"protocolVersion\":19"), json);
        assertTrue(json.contains("\"image\":{\"image\":{"), json);
        assertTrue(json.contains("\"kind\":\"asset\""), json);
        assertTrue(json.contains("\"assetName\":\"assets/background.png\""), json);
        assertTrue(json.contains("\"resize\":{\"width\":320,\"height\":null,"), json);
        assertTrue(json.contains("\"resolution\":{\"kind\":\"resolved\","), json);
        assertTrue(json.contains("\"resourceId\":\""
                + resource.resourceId() + "\""), json);
        assertTrue(json.contains("\"resolvedScale\":1"), json);
        assertTrue(json.contains("\"onError\":true"), json);
        assertFalse(json.contains("handleImageError"), json);
        assertTrue(json.contains("\"colorFilter\":{\"kind\":\"mode\""), json);
        assertTrue(json.contains("\"fit\":\"cover\""), json);
        assertTrue(json.contains("\"repeat\":\"repeatX\""), json);
        assertTrue(json.contains("\"opacity\":0.75"), json);
    }

    @Test
    void verticalDividerPayloadPreservesAll1215NumericColorAndRadiusStatesWithoutSynthesizingThemeDefaults() throws Exception {
        int count = 0;
        for (int numbers = 0; numbers < 81; numbers++) for (int color = 0; color < 3; color++) for (int radius = 0; radius < 5; radius++) {
            count++;
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            int state = numbers;
            for (String name : List.of("width", "thickness", "indent", "endIndent")) {
                int value = state % 3; state /= 3;
                if (value > 0) properties.put(new PropertyName(name), value == 1 ? new PropertyValue.IntegerValue(BigInteger.ZERO) : new PropertyValue.DoubleValue(new BigDecimal("24.5")));
            }
            if (color > 0) properties.put(new PropertyName("color"), color == 1 ? new PropertyValue.ColorValue(0L) : new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant")));
            if (radius > 0) {
                boolean zero = radius % 2 == 1;
                var a = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("1.5"), zero ? BigDecimal.ZERO : new BigDecimal("2.5"));
                var b = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("3.5"), zero ? BigDecimal.ZERO : new BigDecimal("4.5"));
                var c = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("5.5"), zero ? BigDecimal.ZERO : new BigDecimal("6.5"));
                var d = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("7.5"), zero ? BigDecimal.ZERO : new BigDecimal("8.5"));
                properties.put(new PropertyName("radius"), new PropertyValue.BorderRadiusValue(radius >= 3 ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,b,c,d) : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a,b,c,d)));
            }
            var node = new WidgetNode(StableId.random(), type("flutter.material.VerticalDivider"), properties, Map.of());
            var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            var codec = new CanvasModelPayloadCodec();
            String json = new String(codec.encode(request), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(request), codec.encode(request));
            assertTrue(json.contains("\"type\":\"flutter.material.VerticalDivider\""), json);
            for (String name : List.of("width", "thickness", "indent", "endIndent", "color", "radius")) assertEquals(properties.containsKey(new PropertyName(name)), json.contains("\"" + name + "\":"), json);
            if (radius > 0) {
                assertTrue(json.contains("\"radius\":{\"kind\":\"borderRadius\",\"geometry\":{\"kind\":\"" + (radius >= 3 ? "directional" : "physical") + "\""), json);
                var corners = radius >= 3 ? List.of("topStart", "topEnd", "bottomEnd", "bottomStart") : List.of("topLeft", "topRight", "bottomRight", "bottomLeft");
                for (int index = 0; index < 4; index++) {
                    String x = radius % 2 == 1 ? "0" : (index * 2 + 1) + ".5";
                    String y = radius % 2 == 1 ? "0" : (index * 2 + 2) + ".5";
                    assertTrue(json.contains("\"" + corners.get(index) + "\":{\"x\":" + x + ",\"y\":" + y + "}"), json);
                }
            }
            assertFalse(json.contains("createBorderSide"), json);
            assertFalse(json.contains("DividerThemeData"), json);
            assertFalse(json.contains("\"resolution\":"), json);
        }
        assertEquals(1215, count);
    }

    @Test
    void verticalDividerPayloadRejectsWrongKindsNegativeOrNonfiniteDimensionsForeignTokensAndInventedFields() {
        for (String name : List.of("width", "thickness", "indent", "endIndent", "color", "radius")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true), new PropertyValue.StringValue("invalid"), new PropertyValue.DartExpressionValue("Divider.createBorderSide(context)"))) {
                var node = new WidgetNode(StableId.random(), type("flutter.material.VerticalDivider"), Map.of(new PropertyName(name), value), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        for (String name : List.of("width", "thickness", "indent", "endIndent", "height", "space", "key", "borderRadius", "semanticLabel")) {
            for (String value : List.of("-1", "1e999")) {
                var node = new WidgetNode(StableId.random(), type("flutter.material.VerticalDivider"), Map.of(new PropertyName(name), new PropertyValue.DoubleValue(new BigDecimal(value))), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        var foreign = new WidgetNode(StableId.random(), type("flutter.material.VerticalDivider"), Map.of(new PropertyName("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), foreign))));
        var slot = new WidgetNode(StableId.random(), type("flutter.material.VerticalDivider"), Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), slot))));
    }

    @Test
    void dividerPayloadPreservesAll1215NumericColorAndRadiusStatesWithoutSynthesizingThemeDefaults() throws Exception {
        int count = 0;
        for (int numbers = 0; numbers < 81; numbers++) for (int color = 0; color < 3; color++) for (int radius = 0; radius < 5; radius++) {
            count++;
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            int state = numbers;
            for (String name : List.of("height", "thickness", "indent", "endIndent")) {
                int value = state % 3; state /= 3;
                if (value > 0) properties.put(new PropertyName(name), value == 1 ? new PropertyValue.IntegerValue(BigInteger.ZERO) : new PropertyValue.DoubleValue(new BigDecimal("24.5")));
            }
            if (color > 0) properties.put(new PropertyName("color"), color == 1 ? new PropertyValue.ColorValue(0L) : new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant")));
            if (radius > 0) {
                boolean zero = radius % 2 == 1;
                var a = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("1.5"), zero ? BigDecimal.ZERO : new BigDecimal("2.5"));
                var b = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("3.5"), zero ? BigDecimal.ZERO : new BigDecimal("4.5"));
                var c = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("5.5"), zero ? BigDecimal.ZERO : new BigDecimal("6.5"));
                var d = new PropertyValue.BoxDecorationValue.Radius(zero ? BigDecimal.ZERO : new BigDecimal("7.5"), zero ? BigDecimal.ZERO : new BigDecimal("8.5"));
                properties.put(new PropertyName("radius"), new PropertyValue.BorderRadiusValue(radius >= 3 ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,b,c,d) : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a,b,c,d)));
            }
            var node = new WidgetNode(StableId.random(), type("flutter.material.Divider"), properties, Map.of());
            var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            var codec = new CanvasModelPayloadCodec();
            String json = new String(codec.encode(request), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(request), codec.encode(request));
            assertTrue(json.contains("\"type\":\"flutter.material.Divider\""), json);
            for (String name : List.of("height", "thickness", "indent", "endIndent", "color", "radius")) assertEquals(properties.containsKey(new PropertyName(name)), json.contains("\"" + name + "\":"), json);
            if (radius > 0) {
                assertTrue(json.contains("\"radius\":{\"kind\":\"borderRadius\",\"geometry\":{\"kind\":\"" + (radius >= 3 ? "directional" : "physical") + "\""), json);
                var corners = radius >= 3 ? List.of("topStart", "topEnd", "bottomEnd", "bottomStart") : List.of("topLeft", "topRight", "bottomRight", "bottomLeft");
                for (int index = 0; index < 4; index++) {
                    String x = radius % 2 == 1 ? "0" : (index * 2 + 1) + ".5";
                    String y = radius % 2 == 1 ? "0" : (index * 2 + 2) + ".5";
                    assertTrue(json.contains("\"" + corners.get(index) + "\":{\"x\":" + x + ",\"y\":" + y + "}"), json);
                }
            }
            assertFalse(json.contains("createBorderSide"), json);
            assertFalse(json.contains("DividerThemeData"), json);
            assertFalse(json.contains("\"resolution\":"), json);
        }
        assertEquals(1215, count);
    }

    @Test
    void dividerPayloadRejectsWrongKindsNegativeOrNonfiniteDimensionsForeignTokensAndInventedFields() {
        for (String name : List.of("height", "thickness", "indent", "endIndent", "color", "radius")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true), new PropertyValue.StringValue("invalid"), new PropertyValue.DartExpressionValue("Divider.createBorderSide(context)"))) {
                var node = new WidgetNode(StableId.random(), type("flutter.material.Divider"), Map.of(new PropertyName(name), value), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        for (String name : List.of("height", "thickness", "indent", "endIndent", "width", "space", "key", "borderRadius", "semanticLabel")) {
            for (String value : List.of("-1", "1e999")) {
                var node = new WidgetNode(StableId.random(), type("flutter.material.Divider"), Map.of(new PropertyName(name), new PropertyValue.DoubleValue(new BigDecimal(value))), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
            }
        }
        var foreign = new WidgetNode(StableId.random(), type("flutter.material.Divider"), Map.of(new PropertyName("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), foreign))));
        var slot = new WidgetNode(StableId.random(), type("flutter.material.Divider"), Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), slot))));
    }

    @Test
    void imageIconPayloadDistinguishesNullMissingAndReservedUnresolvedWithoutRequiringAssets() throws Exception {
        var none = new WidgetNode(StableId.random(), type("flutter.widgets.ImageIcon"),
                Map.of(new PropertyName("image"), new PropertyValue.NullValue()), Map.of());
        var document = new DesignerDocument(DOCUMENT_ID, source(), none);
        String json = new String(new CanvasModelPayloadCodec().encode(request(document)), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"image\":{\"kind\":\"null\"}"), json);
        assertFalse(json.contains("\"resolution\":"), json);
        assertFalse(json.contains("MaterialIcons"), json);
        assertFalse(json.contains("unresolved-image"), json);
        var unresolved = new WidgetNode(none.id(), none.type(), Map.of(new PropertyName("image"), PropertyValue.ImageProviderValue.unresolved()), Map.of());
        var issue = new dev.flutter.netbeans.designer.canvas.CanvasImageResolutionIssue(
                CanvasImageAssetId.application(PropertyValue.ImageProviderValue.unresolved().assetName()),
                dev.flutter.netbeans.designer.canvas.CanvasImageResolutionIssue.Code.UNDECLARED,
                "Select a declared image or None for ImageIcon.image.");
        var unavailable = new CanvasImageResourceBundle(List.of(), List.of(), List.of(issue));
        String retained = new String(new CanvasModelPayloadCodec().encode(request(PROFILE, new DesignerDocument(DOCUMENT_ID, source(), unresolved), unavailable)), StandardCharsets.UTF_8);
        assertTrue(retained.contains("__netbeans_flutter_designer__/unresolved-image.png"), retained);
        assertTrue(retained.contains("\"kind\":\"imageProvider\""), retained);
        assertTrue(retained.contains("Select a declared image or None"), retained);
        // Clearing the provider must also clear its revision-scoped resource issue.
        assertThrows(CanvasModelPayloadException.class, () -> new CanvasModelPayloadCodec().encode(request(PROFILE, document, unavailable)));
        assertThrows(CanvasModelPayloadException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), unresolved))));
        var missing = new WidgetNode(none.id(), none.type(), Map.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), missing))));
    }

    @Test
    void imageIconPayloadResolvesAllAssetPackageExactResizeBranchesAndPreservesNullableAndThemeFields() throws Exception {
        int count = 0;
        for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) for (boolean packaged : List.of(false, true)) {
            var resizes = new java.util.ArrayList<Optional<PropertyValue.ImageProviderValue.ResizeImageConfig>>();
            resizes.add(Optional.empty());
            for (int dimensions = 1; dimensions <= 3; dimensions++) for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) for (boolean upscale : List.of(false, true)) {
                resizes.add(Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig((dimensions & 1) != 0 ? Optional.of(16) : Optional.empty(), (dimensions & 2) != 0 ? Optional.of(24) : Optional.empty(), policy, upscale)));
            }
            for (var resize : resizes) {
                count++;
                var provider = new PropertyValue.ImageProviderValue(kind, "assets/icon.png", packaged ? Optional.of("reviewed_icons") : Optional.empty(),
                        kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new BigDecimal("2.5")) : Optional.empty(), resize);
                var icon = new WidgetNode(StableId.random(), type("flutter.widgets.ImageIcon"), Map.of(
                        new PropertyName("image"), provider,
                        new PropertyName("size"), new PropertyValue.DoubleValue(new BigDecimal("32.5")),
                        new PropertyName("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")),
                        new PropertyName("semanticLabel"), new PropertyValue.StringValue("Image icon label")), Map.of());
                var resource = CanvasImageResource.create(CanvasImageFormat.PNG, 40, 30, new byte[]{4, 3, 2, 1});
                var assetId = new CanvasImageAssetId(provider.packageName(), provider.assetName());
                var bundle = new CanvasImageResourceBundle(List.of(new CanvasImageAsset(assetId, resource.resourceId(), List.of(new CanvasImageVariant(BigDecimal.ONE, resource.resourceId())))), List.of(resource));
                var request = request(PROFILE, new DesignerDocument(DOCUMENT_ID, source(), icon), bundle);
                var codec = new CanvasModelPayloadCodec();
                String json = new String(codec.encode(request), StandardCharsets.UTF_8);
                assertArrayEquals(codec.encode(request), codec.encode(request));
                assertTrue(json.contains("\"type\":\"flutter.widgets.ImageIcon\""), json);
                assertTrue(json.contains("\"image\":{\"kind\":\"imageProvider\",\"value\":{\"kind\":\"" + kind.wireName() + "\""), json);
                assertTrue(json.contains("\"resolution\":{\"kind\":\"resolved\""), json);
                assertEquals(packaged, json.contains("reviewed_icons"), json);
                assertEquals(resize.isPresent(), json.contains("\"allowUpscaling\":"), json);
                resize.ifPresent(value -> {
                    assertTrue(json.contains("\"policy\":\"" + value.policy().wireName() + "\""), json);
                    assertTrue(json.contains("\"allowUpscaling\":" + value.allowUpscaling()), json);
                });
                assertTrue(json.contains("material.colorScheme.primary"), json);
                assertTrue(json.contains("Image icon label"), json);
                assertFalse(json.contains("MaterialIcons"), json);
            }
        }
        assertEquals(52, count);
    }

    @Test
    void imageIconPayloadRejectsMalformedProviderKindsOptionalNullAndNewerSdkOnlyProperties() {
        for (PropertyValue value : List.of(new PropertyValue.StringValue("assets/icon.png"), new PropertyValue.BooleanValue(true),
                new PropertyValue.DartExpressionValue("NetworkImage('https://example.com/image.png')"))) {
            var icon = new WidgetNode(StableId.random(), type("flutter.widgets.ImageIcon"), Map.of(new PropertyName("image"), value), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), icon))));
        }
        for (String name : List.of("size", "color", "semanticLabel", "useOriginalColors", "key", "fit")) {
            var icon = new WidgetNode(StableId.random(), type("flutter.widgets.ImageIcon"), Map.of(new PropertyName("image"), new PropertyValue.NullValue(), new PropertyName(name), new PropertyValue.NullValue()), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), icon))));
        }
    }

    @Test
    void projectsDirectImageProviderWithOneCanonicalOuterKindAndNestedValue()
            throws Exception {
        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.asset("assets/photo.png");
        WidgetNode image = new WidgetNode(
                id("b41a10da-1350-4689-9b4b-eaa6b41ac0cc"),
                type("flutter.widgets.Image"),
                Map.of(new PropertyName("image"), provider),
                Map.of());
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID, source(), image);
        CanvasImageResource resource = CanvasImageResource.create(
                CanvasImageFormat.PNG, 40, 30, new byte[]{4, 3, 2, 1});
        CanvasImageAssetId assetId = CanvasImageAssetId.application(
                "assets/photo.png");
        CanvasImageResourceBundle bundle = new CanvasImageResourceBundle(
                List.of(new CanvasImageAsset(
                        assetId,
                        resource.resourceId(),
                        List.of(new CanvasImageVariant(
                                BigDecimal.ONE, resource.resourceId())))),
                List.of(resource));

        String json = new String(
                new CanvasModelPayloadCodec().encode(request(
                        PROFILE, document, bundle)),
                StandardCharsets.UTF_8);

        assertTrue(json.contains(
                "\"image\":{\"kind\":\"imageProvider\",\"value\":{"
                + "\"kind\":\"asset\",\"assetName\":\"assets/photo.png\""), json);
        assertTrue(json.contains("\"resolution\":{\"kind\":\"resolved\""), json);
        assertFalse(json.contains(
                "\"kind\":\"imageProvider\",\"kind\":\"asset\""), json);
    }

    @Test
    void listViewExtentPayloadPreservesOmissionNullFixedExtentAndAnonymousReferenceForms() throws Exception {
        for (PropertyValue value : java.util.Arrays.asList(null, new PropertyValue.NullValue(),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "privateExtent", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_list/extents.dart"), "privateOwner",
                        Optional.of("nullableGetter"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_list/extents.dart"), "privateOwner",
                        Optional.of("nullableFactory"), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            for (boolean fixed : List.of(false, true)) {
                if (fixed && value instanceof PropertyValue.DartObjectReferenceValue) continue;
                var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                if (value != null) properties.put(new PropertyName("itemExtentBuilder"), value);
                if (fixed) properties.put(new PropertyName("itemExtent"), new PropertyValue.IntegerValue(BigInteger.valueOf(48)));
                var list = new WidgetNode(DOCUMENT_ID, type("flutter.widgets.ListView"), properties,
                        Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), list))), StandardCharsets.UTF_8);
                assertEquals(value != null, json.contains("\"itemExtentBuilder\":"), json);
                assertEquals(value instanceof PropertyValue.NullValue, json.contains("\"itemExtentBuilder\":{\"kind\":\"null\"}"), json);
                assertEquals(value instanceof PropertyValue.DartObjectReferenceValue,
                        json.contains("\"itemExtentBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
                assertEquals(fixed, json.contains("\"itemExtent\":{\"kind\":\"integer\",\"value\":48}"), json);
                for (String forbidden : List.of("privateExtent", "privateOwner", "nullableGetter", "nullableFactory",
                        "private_list", "rootSymbol", "zeroArgumentInvocation", "ItemExtentBuilder")) {
                    assertFalse(json.contains(forbidden), json);
                }
            }
        }
    }

    @Test
    void listViewExtentPayloadRejectsFixedReferenceConflictAndUnreviewedKinds() {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "maybeExtents", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var invalid = new java.util.ArrayList<Map<PropertyName, PropertyValue>>();
        for (PropertyValue value : List.of(new PropertyValue.CallbackValue("extents"), new PropertyValue.StringValue("extents"),
                new PropertyValue.DartExpressionValue("(i, dimensions) => 40.0"))) {
            invalid.add(Map.of(new PropertyName("itemExtentBuilder"), value));
        }
        for (int extent : List.of(0, 48)) invalid.add(Map.of(new PropertyName("itemExtentBuilder"), reference,
                new PropertyName("itemExtent"), new PropertyValue.IntegerValue(BigInteger.valueOf(extent))));
        for (var properties : invalid) {
            var list = new WidgetNode(DOCUMENT_ID, type("flutter.widgets.ListView"), properties, Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), list))), properties.toString());
        }
    }

    @Test
    void textFieldBuilderPayloadPreservesOmissionNullAndAnonymousNullableReferenceForms() throws Exception {
        for (var reference : List.of(
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "privateBuilder", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_fields/builders.dart"), "privateOwner",
                        Optional.of("nullableGetter"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_fields/builders.dart"), "privateOwner",
                        Optional.of("nullableFactory"), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            for (PropertyValue counter : java.util.Arrays.asList(null, new PropertyValue.NullValue(), reference)) {
                for (PropertyValue menu : java.util.Arrays.asList(null, new PropertyValue.NullValue(), reference)) {
                    var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                    if (counter != null) properties.put(new PropertyName("buildCounter"), counter);
                    if (menu != null) properties.put(new PropertyName("contextMenuBuilder"), menu);
                    var field = new WidgetNode(DOCUMENT_ID, type("flutter.material.TextField"), properties, Map.of());
                    String json = new String(new CanvasModelPayloadCodec().encode(request(
                            new DesignerDocument(DOCUMENT_ID, source(), field))), StandardCharsets.UTF_8);
                    for (String name : List.of("buildCounter", "contextMenuBuilder")) {
                        PropertyValue value = properties.get(new PropertyName(name));
                        assertEquals(value != null, json.contains("\"" + name + "\":"), json);
                        assertEquals(value instanceof PropertyValue.NullValue, json.contains("\"" + name + "\":{\"kind\":\"null\"}"), json);
                        assertEquals(value instanceof PropertyValue.DartObjectReferenceValue,
                                json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
                    }
                    for (String forbidden : List.of("privateBuilder", "privateOwner", "nullableGetter", "nullableFactory",
                            "private_fields", "rootSymbol", "zeroArgumentInvocation", "InputCounterWidgetBuilder", "EditableTextContextMenuBuilder")) {
                        assertFalse(json.contains(forbidden), json);
                    }
                }
            }
        }
    }

    @Test
    void textFieldBuilderPayloadRejectsUnreviewedCallbackAndExecutableSourceKinds() {
        for (String name : List.of("buildCounter", "contextMenuBuilder")) {
            for (PropertyValue value : List.of(new PropertyValue.CallbackValue("buildField"),
                    new PropertyValue.StringValue("builder"), new PropertyValue.BooleanValue(false),
                    new PropertyValue.DartExpressionValue("(_, _) => const SizedBox()"))) {
                var field = new WidgetNode(DOCUMENT_ID, type("flutter.material.TextField"),
                        Map.of(new PropertyName(name), value), Map.of());
                assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), field))), name + ": " + value);
            }
        }
    }

    @Test
    void projectsTextFieldScalarCompoundsAndCallbackPresenceWithoutHandlerCode()
            throws Exception {
        WidgetNode field = new WidgetNode(
                id("9fac7a8b-a2e0-463b-b39a-4b046d6b5f16"),
                type("flutter.material.TextField"),
                Map.ofEntries(
                        Map.entry(new PropertyName("keyboardType"),
                                new PropertyValue.StringValue("numberSignedDecimal")),
                        Map.entry(new PropertyName("obscuringCharacter"),
                                new PropertyValue.StringValue(Character.toString(0x2022))),
                        Map.entry(new PropertyName("maxLength"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(-1))),
                        Map.entry(new PropertyName("cursorRadiusX"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                        Map.entry(new PropertyName("cursorRadiusY"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(3))),
                        Map.entry(new PropertyName("scrollPaddingLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.entry(new PropertyName("scrollPaddingTop"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                        Map.entry(new PropertyName("scrollPaddingRight"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(3))),
                        Map.entry(new PropertyName("scrollPaddingBottom"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(4))),
                        Map.entry(new PropertyName("selectionHeightStyle"),
                                new PropertyValue.EnumValue("BoxHeightStyle", "max")),
                        Map.entry(new PropertyName("dragStartBehavior"),
                                new PropertyValue.EnumValue("DragStartBehavior", "down")),
                        Map.entry(new PropertyName("mouseCursor"),
                                new PropertyValue.StringValue("text")),
                        Map.entry(new PropertyName("onChanged"),
                                new PropertyValue.CallbackValue("handleChanged"))),
                Map.of());

        String json = new String(
                new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), field))),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"type\":\"flutter.material.TextField\""), json);
        assertTrue(json.contains("\"keyboardType\":{\"kind\":\"string\","
                + "\"value\":\"numberSignedDecimal\"}"), json);
        assertTrue(json.contains("\"maxLength\":{\"kind\":\"integer\",\"value\":-1}"),
                json);
        assertTrue(json.contains("\"selectionHeightStyle\":{\"kind\":\"enum\","
                + "\"type\":\"BoxHeightStyle\",\"value\":\"max\"}"), json);
        assertTrue(json.contains("\"onChanged\":{\"kind\":\"callbackPresence\"}"), json);
        assertFalse(json.contains("handleChanged"), json);
    }

    private static void assertCanvasAdmissionRejects(
            PropertyValue value,
            String propertyName) {
        WidgetNode text = new WidgetNode(
                id("a3e7471d-f073-4930-b85a-7ba7f04158aa"),
                type("flutter.widgets.Text"),
                Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue("Styled"),
                        new PropertyName(propertyName),
                        value),
                Map.of());
        DesignerDocument invalid = new DesignerDocument(DOCUMENT_ID, source(), text);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class, () -> request(invalid));
        assertTrue(failure.getMessage().contains("designer.property.constraint"),
                failure::getMessage);
    }

    @Test
    void cardAllVariantsAndApplicableShapeLeavesRetainTypedValuesWithoutSynthesizedDefaults() throws Exception {
        int count = 0;
        for (String variant : List.of("elevated", "filled", "outlined")) {
            var kinds = new ArrayList<String>(); kinds.add(null); kinds.addAll(dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema.shapeKinds());
            for (String kind : kinds) {
                count++;
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
                if (kind != null) {
                    properties.put(new PropertyName("shapeKind"), new PropertyValue.StringValue(kind));
                    for (String name : dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema.builtInShapePropertyNames()) {
                        if (!dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema.isShapeDetailProperty(name)
                                || !dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind)) continue;
                        PropertyValue value = switch (name) {
                            case "shapeRadius" -> {
                                var r = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.TWO);
                                yield new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(r,r,r,r));
                            }
                            case "shapeSideColor" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant"));
                            case "shapeSideStyle" -> new PropertyValue.EnumValue("BorderStyle", "solid");
                            case "shapePoints" -> new PropertyValue.DoubleValue(new BigDecimal("4097.5"));
                            default -> new PropertyValue.DoubleValue(new BigDecimal("0.25"));
                        };
                        properties.put(new PropertyName(name), value);
                    }
                }
                var node = new WidgetNode(StableId.random(), type("flutter.material.Card"), properties, Map.of());
                var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
                var codec = new CanvasModelPayloadCodec();
                String json = new String(codec.encode(request), StandardCharsets.UTF_8);
                assertArrayEquals(codec.encode(request), codec.encode(request));
                assertTrue(json.contains("\"type\":\"flutter.material.Card\""), json);
                for (String name : dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema.definitions().keySet())
                    assertEquals(properties.containsKey(new PropertyName(name)), json.contains("\"" + name + "\":"), name + json);
                if (kind != null && List.of("star", "polygon").contains(kind)) assertTrue(json.contains("4097.5"), json);
                assertFalse(json.contains("CardThemeData"), json);
                assertFalse(json.contains("\"resolution\":"), json);
            }
        }
        assertEquals(33, count);
    }

    @Test
    void cardTypedShapeReferenceAndOptionalChildStayExplicitWithoutCanvasEvaluation() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/shapes.dart"), "makeShape", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
        var child = text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Card child");
        var node = new WidgetNode(StableId.random(), type("flutter.material.Card"), Map.of(new PropertyName("variant"), new PropertyValue.StringValue("outlined"), new PropertyName("shape"), reference), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"shape\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
        assertFalse(json.contains("makeShape"), json); assertFalse(json.contains("package:app/shapes.dart"), json); assertTrue(json.contains("Card child"), json);
        assertFalse(json.contains("shapeKind"), json); assertFalse(json.contains("\"resolution\":"), json);
    }

    @Test
    void cardPayloadRejectsMissingModeShapeConflictsInactiveFieldsAndRoundingOverflow() {
        var bad = new ArrayList<Map<PropertyName, PropertyValue>>();
        bad.add(Map.of()); bad.add(Map.of(new PropertyName("variant"), new PropertyValue.NullValue()));
        bad.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("elevated"), new PropertyName("shapePoints"), new PropertyValue.DoubleValue(BigDecimal.TEN)));
        bad.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("filled"), new PropertyName("shapeKind"), new PropertyValue.StringValue("polygon"), new PropertyName("shapeValleyRounding"), new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        bad.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("outlined"), new PropertyName("shapeKind"), new PropertyValue.StringValue("star"), new PropertyName("shapePointRounding"), new PropertyValue.DoubleValue(BigDecimal.ONE), new PropertyName("shapeValleyRounding"), new PropertyValue.DoubleValue(BigDecimal.ONE)));
        for (var properties : bad) {
            var node = new WidgetNode(StableId.random(), type("flutter.material.Card"), properties, Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void badgeBothConstructorsAllStyleValuesAndSlotsPreserveExactTypedPayloadAndNoDefaults() throws Exception {
        var covered = new HashSet<PropertyName>();
        for (boolean paints : List.of(false, true)) for (boolean count : List.of(false, true)) {
            var properties = dev.flutter.netbeans.designer.catalog.BadgeTestValues.full(paints, count);
            covered.addAll(properties.keySet());
            var child = text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Badge child");
            var label = text("fac77c2c-1233-4dff-946f-a3023df9b6ce", "Badge label");
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(child));
            if (!count) slots.put(new SlotName("label"), WidgetSlot.SingleSlot.of(label));
            var node = new WidgetNode(StableId.random(), type("flutter.material.Badge"), properties, slots);
            var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            var codec = new CanvasModelPayloadCodec();
            String json = new String(codec.encode(request), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(request), codec.encode(request));
            assertTrue(json.contains("\"type\":\"flutter.material.Badge\""), json);
            for (String name : dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema.definitions().keySet())
                assertEquals(properties.containsKey(new PropertyName(name)), json.contains("\"" + name + "\":"), name + json);
            assertTrue(json.contains("Badge child"), json); assertEquals(!count, json.contains("Badge label"), json);
            assertTrue(json.contains("-2.5"), json); assertFalse(json.contains("BadgeThemeData"), json);
            assertFalse(json.contains("\"variant\":"), json);
        }
        assertEquals(41, covered.size());
    }

    @Test
    void badgePayloadPreservesEmptyDefaultsExplicitFalseAndPortableCountsWithoutNumericLabelSynthesis() throws Exception {
        for (boolean count : List.of(false, true)) {
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("isLabelVisible"), new PropertyValue.BooleanValue(false));
            if (count) properties.put(new PropertyName("count"), new PropertyValue.IntegerValue(new BigInteger("9007199254740991")));
            var node = new WidgetNode(StableId.random(), type("flutter.material.Badge"), properties, Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertEquals(count, json.contains("9007199254740991"), json);
            for (String name : List.of("maxCount", "smallSize", "largeSize", "textStyleFontSize", "backgroundColor", "variant"))
                assertFalse(json.contains("\"" + name + "\":"), json);
            assertFalse(json.contains("999+"), json);
        }
    }

    @Test
    void badgePayloadRejectsOrphanMaximumLabelConflictAndPaintColorConflicts() {
        var names = new PropertyName[]{new PropertyName("count"), new PropertyName("maxCount")};
        List<Map<PropertyName, PropertyValue>> bad = List.of(
                Map.of(names[1], dev.flutter.netbeans.designer.catalog.BadgeTestValues.i(1)),
                Map.of(names[0], dev.flutter.netbeans.designer.catalog.BadgeTestValues.i(-1)),
                Map.of(new PropertyName("textStyleColor"), dev.flutter.netbeans.designer.catalog.BadgeTestValues.theme(), new PropertyName("textStyleForeground"), dev.flutter.netbeans.designer.catalog.BadgeTestValues.paint()));
        for (var properties : bad) {
            var node = new WidgetNode(StableId.random(), type("flutter.material.Badge"), properties, Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        var node = new WidgetNode(StableId.random(), type("flutter.material.Badge"), Map.of(names[0], dev.flutter.netbeans.designer.catalog.BadgeTestValues.i(0)), Map.of(new SlotName("label"), WidgetSlot.SingleSlot.of(text("fac77c2c-1233-4dff-946f-a3023df9b6ce", "Keep this label"))));
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
    }

    @Test
    void stateBindingMetadataIsExcludedAndLiteralPreviewWireBytesRemainIdentical() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                BuiltInWidgetCatalog.getDefault().find(type("flutter.material.Checkbox")).orElseThrow(),
                id("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
        var binding = new dev.flutter.netbeans.designer.model.StateBinding(
                "_secretSelectedValue", "_secretSelectionHandler",
                dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL);
        var consumer = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_secretSelectedValue",
                dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL, Optional.empty(),
                dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.NOT);
        var bound = new WidgetNode(node.id(), node.type(), node.properties(), node.slots(), node.extensions(), Optional.of(binding),
                Map.of(new PropertyName("autofocus"), consumer));
        var before = source();
        var source = new DartSourceDescriptor(before.dartFile(), before.className(), WidgetClassKind.STATEFUL,
                before.generatorVersion(), before.managedRegions());
        var codec = new CanvasModelPayloadCodec();
        byte[] literalBytes = codec.encode(request(new DesignerDocument(DOCUMENT_ID, source, node)));
        byte[] boundBytes = codec.encode(request(new DesignerDocument(DOCUMENT_ID, source, bound)));
        assertArrayEquals(literalBytes, boundBytes);
        String json = new String(boundBytes, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"protocolVersion\":19"));
        assertTrue(json.contains("\"value\":{\"kind\":\"boolean\",\"value\":false}"), json);
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("stateBinding"));
        assertFalse(json.contains("propertyBindings"));
    }

    @Test
    void gestureDetectorBooleanStateConsumersNeverChangeLiteralCanvasBytes() throws Exception {
        var properties = Map.<PropertyName, PropertyValue>of(
                new PropertyName("excludeFromSemantics"), new PropertyValue.BooleanValue(false),
                new PropertyName("trackpadScrollCausesScale"), new PropertyValue.BooleanValue(true));
        var node = new WidgetNode(id("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                type("flutter.widgets.GestureDetector"), properties, Map.of());
        var before = source();
        var source = new DartSourceDescriptor(before.dartFile(), before.className(), WidgetClassKind.STATEFUL,
                before.generatorVersion(), before.managedRegions());
        var codec = new CanvasModelPayloadCodec();
        byte[] literalBytes = codec.encode(request(new DesignerDocument(DOCUMENT_ID, source, node)));
        for (var transform : List.of(dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT,
                dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.NOT)) {
            var binding = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_secretGestureState",
                    dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL, Optional.empty(), transform);
            var bound = new WidgetNode(node.id(), node.type(), node.properties(), node.slots(),
                    node.extensions(), Optional.empty(), Map.of(
                            new PropertyName("excludeFromSemantics"), binding,
                            new PropertyName("trackpadScrollCausesScale"), binding));
            byte[] boundBytes = codec.encode(request(new DesignerDocument(DOCUMENT_ID, source, bound)));
            assertArrayEquals(literalBytes, boundBytes);
            String json = new String(boundBytes, StandardCharsets.UTF_8);
            assertFalse(json.contains("secretGestureState"), json);
            assertFalse(json.contains("stateBinding"), json);
            assertFalse(json.contains("propertyBindings"), json);
            assertFalse(json.contains("transform"), json);
        }
    }

    @Test
    void gestureDetectorProjectsAll58CallbacksWithoutLeakingCodeIdentity() throws Exception {
        var callbacks = dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema.callbackDefinitions();
        assertEquals(58, callbacks.size());
        for (String name : callbacks.keySet()) {
            for (PropertyValue value : List.of(new PropertyValue.CallbackValue("privateGestureHandler"),
                    new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app/secret.dart"),
                            "privateGestureCallback", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                    new PropertyValue.NullValue())) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.GestureDetector"),
                        Map.of(new PropertyName(name), value),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                String expectedKind = value instanceof PropertyValue.CallbackValue ? "callbackPresence"
                        : value instanceof PropertyValue.DartObjectReferenceValue ? "dartObjectReferencePresence" : "null";
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"" + expectedKind + "\"}"), json);
                assertFalse(json.contains("privateGesture"), json);
                assertFalse(json.contains("private_app"), json);
                assertFalse(json.contains("secret.dart"), json);
            }
        }
    }

    @Test
    void listenerProjectsAllNineCallbacksWithoutApplicationCodeAndKeepsBehaviorExact() throws Exception {
        var callbacks = dev.flutter.netbeans.designer.catalog.ListenerWidgetPropertySchema.callbackDefinitions();
        assertEquals(9, callbacks.size());
        for (String name : callbacks.keySet()) {
            for (PropertyValue value : List.of(new PropertyValue.CallbackValue("privatePointerHandler"),
                    new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app/pointer.dart"),
                            "privatePointerCallback", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                    new PropertyValue.NullValue())) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.Listener"),
                        Map.of(new PropertyName(name), value), Map.of());
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                String expectedKind = value instanceof PropertyValue.CallbackValue ? "callbackPresence"
                        : value instanceof PropertyValue.DartObjectReferenceValue ? "dartObjectReferencePresence" : "null";
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"" + expectedKind + "\"}"), json);
                assertFalse(json.contains("privatePointer"), json);
                assertFalse(json.contains("private_app"), json);
                assertFalse(json.contains("pointer.dart"), json);
                assertFalse(json.contains("\"behavior\":"), json);
            }
        }
        for (String behavior : List.of("deferToChild", "opaque", "translucent")) {
            var value = new PropertyValue.EnumValue("HitTestBehavior", behavior);
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.Listener"),
                    Map.of(new PropertyName("behavior"), value),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"behavior\":{\"kind\":\"enum\",\"type\":\"HitTestBehavior\",\"value\":\"" + behavior + "\"}"), json);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
        }
        var invalid = new WidgetNode(StableId.random(), type("flutter.widgets.Listener"),
                Map.of(new PropertyName("behavior"), new PropertyValue.NullValue()), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), invalid))));
    }

    @Test
    void gestureDetectorProjectsAll64DeviceSubsetsCanonicallyAndKeepsNullDistinct() throws Exception {
        var kinds = PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind.values();
        for (int bits = 0; bits < 64; bits++) {
            var devices = new java.util.ArrayList<PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind>();
            for (int index = 5; index >= 0; index--) {
                if ((bits & (1 << index)) != 0) devices.add(kinds[index]);
            }
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.GestureDetector"),
                    Map.of(new PropertyName("supportedDevices"), new PropertyValue.PointerDeviceKindSetValue(devices)),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            String values = devices.stream().sorted().map(device -> "\"" + device.wireName() + "\"")
                    .collect(java.util.stream.Collectors.joining(","));
            assertTrue(json.contains("\"supportedDevices\":{\"kind\":\"pointerDeviceKindSet\",\"values\":[" + values + "]}"), json);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
        }
        for (boolean explicitNull : List.of(false, true)) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.GestureDetector"),
                    explicitNull ? Map.of(new PropertyName("supportedDevices"), new PropertyValue.NullValue()) : Map.of(),
                    Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertEquals(explicitNull, json.contains("\"supportedDevices\":{\"kind\":\"null\"}"), json);
        }
    }

    @Test
    void scaffoldBottomSheetScrimBuilderProjectsOnlyPresenceAndPreservesOmission() throws Exception {
        var refs = new java.util.ArrayList<PropertyValue>();
        refs.add(null);
        for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of("package:private_app/scrim.dart"))) {
            for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("privateScrimMember"))) {
                for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
                    refs.add(new PropertyValue.DartObjectReferenceValue(library, "PrivateScrimBuilder", member, access,
                            access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE ? Optional.empty() : Optional.of(false)));
                }
            }
        }
        for (var reference : refs) {
            var child = text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Scaffold child");
            var node = new WidgetNode(StableId.random(), type("flutter.material.Scaffold"),
                    reference == null ? Map.of() : Map.of(new PropertyName("bottomSheetScrimBuilder"), reference),
                    Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(child)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertEquals(reference != null, json.contains("\"bottomSheetScrimBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains(child.id().toString()), json);
            assertFalse(json.contains("PrivateScrimBuilder"), json);
            assertFalse(json.contains("privateScrimMember"), json);
            assertFalse(json.contains("private_app"), json);
            assertFalse(json.contains("scrim.dart"), json);
            assertTrue(json.contains("\"protocolVersion\":19"), json);
        }
    }

    @Test
    void scaffoldBottomSheetScrimBuilderRejectsNullAndUnreviewedCallbackKindsBeforeProjection() {
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(),
                new PropertyValue.CallbackValue("privateScrimHandler"),
                new PropertyValue.StringValue("privateScrimHandler"),
                new PropertyValue.BooleanValue(false))) {
            var node = new WidgetNode(StableId.random(), type("flutter.material.Scaffold"),
                    Map.of(new PropertyName("bottomSheetScrimBuilder"), invalid),
                    Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Scaffold child"))));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void notificationListenerProjectsAll14TypesAndNullableCallbacksWithoutApplicationIdentity() throws Exception {
        var callbackValues = new java.util.ArrayList<PropertyValue>();
        callbackValues.add(null);
        callbackValues.add(new PropertyValue.NullValue());
        callbackValues.add(new PropertyValue.CallbackValue("privateNotificationHandler"));
        for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
            callbackValues.add(new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app/notifications.dart"),
                    "PrivateNotificationOwner", Optional.of("privateNotificationHandler"), access,
                    access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE ? Optional.empty() : Optional.of(false)));
        }
        var types = dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema.typePresets();
        assertEquals(14, types.size());
        for (String selectedType : types) {
            for (var callback : callbackValues) {
                var fields = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
                fields.put(new PropertyName("notificationType"), new PropertyValue.StringValue(selectedType));
                if (callback != null) fields.put(new PropertyName("onNotification"), callback);
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.NotificationListener"), fields,
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Notification child"))));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"notificationType\":{\"kind\":\"string\",\"value\":\"" + selectedType + "\"}"), json);
                if (callback == null) {
                    assertFalse(json.contains("\"onNotification\""), json);
                } else {
                    String kind = callback instanceof PropertyValue.CallbackValue ? "callbackPresence"
                            : callback instanceof PropertyValue.DartObjectReferenceValue ? "dartObjectReferencePresence" : "null";
                    assertTrue(json.contains("\"onNotification\":{\"kind\":\"" + kind + "\"}"), json);
                }
                assertFalse(json.contains("privateNotification"), json);
                assertFalse(json.contains("PrivateNotificationOwner"), json);
                assertFalse(json.contains("private_app"), json);
                assertFalse(json.contains("notifications.dart"), json);
                assertTrue(json.contains("\"protocolVersion\":19"), json);
            }
        }
    }

    @Test
    void notificationListenerCustomSubtypeBecomesAnonymousPresenceAndPreservesChild() throws Exception {
        for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of("package:private_app/notifications.dart"))) {
            var child = text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Notification child");
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.NotificationListener"),
                    Map.of(new PropertyName("notificationType"), new PropertyValue.DartObjectReferenceValue(library,
                            "PrivateNotificationSubtype", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"notificationType\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains(child.id().toString()), json);
            assertFalse(json.contains("PrivateNotificationSubtype"), json);
            assertFalse(json.contains("private_app"), json);
            assertFalse(json.contains("notifications.dart"), json);
        }
    }

    @Test
    void focusProjectsAllCallbacksAndNullableNodesAsPresenceWithoutApplicationIdentity() throws Exception {
        for (String name : List.of("onFocusChange", "onKeyEvent", "onKey", "focusNode", "parentNode")) {
            var values = new java.util.ArrayList<PropertyValue>();
            values.add(new PropertyValue.NullValue());
            if (name.startsWith("on")) values.add(new PropertyValue.CallbackValue("privateFocusHandler"));
            for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
                values.add(new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app/focus.dart"),
                        "PrivateFocusOwner", Optional.of("privateFocusMember"), access,
                        access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE ? Optional.empty() : Optional.of(false)));
            }
            for (var value : values) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.Focus"),
                        Map.of(new PropertyName(name), value),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Focus child"))));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                String expected = value instanceof PropertyValue.CallbackValue ? "callbackPresence"
                        : value instanceof PropertyValue.DartObjectReferenceValue ? "dartObjectReferencePresence" : "null";
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"" + expected + "\"}"), json);
                assertFalse(json.contains("privateFocus"), json);
                assertFalse(json.contains("PrivateFocusOwner"), json);
                assertFalse(json.contains("private_app"), json);
                assertFalse(json.contains("focus.dart"), json);
            }
        }
    }

    @Test
    void focusExternalProjectionPreservesInactiveValuesButNeverTheirCodeIdentity() throws Exception {
        var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"));
        properties.put(new PropertyName("focusNode"), new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                "privateFocusNode", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        properties.put(new PropertyName("onKey"), new PropertyValue.CallbackValue("privateFocusKey"));
        properties.put(new PropertyName("onKeyEvent"), new PropertyValue.CallbackValue("privateFocusEvent"));
        properties.put(new PropertyName("debugLabel"), new PropertyValue.StringValue("Stored inactive label"));
        for (String name : List.of("canRequestFocus", "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable")) {
            properties.put(new PropertyName(name), new PropertyValue.BooleanValue(false));
        }
        var node = new WidgetNode(StableId.random(), type("flutter.widgets.Focus"), properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Focus child"))));
        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertFalse(json.contains("privateFocus"), json);
        assertTrue(json.contains("\"onKey\":{\"kind\":\"callbackPresence\"}"), json);
        assertTrue(json.contains("\"onKeyEvent\":{\"kind\":\"callbackPresence\"}"), json);
        assertTrue(json.contains("Stored inactive label"), json);
        assertTrue(json.contains("withExternalFocusNode"), json);
        assertTrue(json.contains("\"protocolVersion\":19"), json);
    }

    @Test
    void focusStateConsumersKeepLiteralCanvasBytesAndHideStateNames() throws Exception {
        var previous = source();
        var stateful = new DartSourceDescriptor(previous.dartFile(), previous.className(), WidgetClassKind.STATEFUL,
                previous.generatorVersion(), previous.managedRegions());
        var codec = new CanvasModelPayloadCodec();
        for (String name : List.of("autofocus", "includeSemantics", "canRequestFocus", "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable")) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.Focus"),
                    Map.of(new PropertyName(name), new PropertyValue.BooleanValue(false)),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Focus child"))));
            byte[] literal = codec.encode(request(new DesignerDocument(DOCUMENT_ID, stateful, node)));
            for (var transform : List.of(dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT,
                    dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.NOT)) {
                var binding = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_privateFocusState",
                        dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL, Optional.empty(), transform);
                var bound = new WidgetNode(node.id(), node.type(), node.properties(), node.slots(), node.extensions(),
                        Optional.empty(), Map.of(new PropertyName(name), binding));
                byte[] projected = codec.encode(request(new DesignerDocument(DOCUMENT_ID, stateful, bound)));
                assertArrayEquals(literal, projected);
                assertFalse(new String(projected, StandardCharsets.UTF_8).contains("privateFocusState"));
            }
        }
    }

    @Test
    void mouseRegionProjectsAllThreeCallbacksWithoutApplicationCodeIdentity() throws Exception {
        var callbacks = dev.flutter.netbeans.designer.catalog.MouseRegionWidgetPropertySchema.callbackDefinitions();
        assertEquals(3, callbacks.size());
        for (String name : callbacks.keySet()) {
            for (PropertyValue value : List.of(new PropertyValue.CallbackValue("privateMouseHandler"),
                    new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app/mouse.dart"),
                            "privateMouseCallback", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                    new PropertyValue.NullValue())) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.MouseRegion"),
                        Map.of(new PropertyName(name), value), Map.of());
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                String expectedKind = value instanceof PropertyValue.CallbackValue ? "callbackPresence"
                        : value instanceof PropertyValue.DartObjectReferenceValue ? "dartObjectReferencePresence" : "null";
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"" + expectedKind + "\"}"), json);
                assertFalse(json.contains("privateMouse"), json);
                assertFalse(json.contains("private_app"), json);
                assertFalse(json.contains("mouse.dart"), json);
                assertFalse(json.contains("\"cursor\":"), json);
                assertFalse(json.contains("\"opaque\":"), json);
                assertFalse(json.contains("\"hitTestBehavior\":"), json);
            }
        }
    }

    @Test
    void mouseRegionProjectsAll41CursorPresetsAndOpaqueCustomReferenceWithoutExecutingIt() throws Exception {
        var cursors = List.of(
            "none", "basic", "click", "forbidden", "wait", "progress", "contextMenu", "help", "text", "verticalText", "cell", "precise", "move", "grab", "grabbing", "noDrop", "alias", "copy", "disappearing", "allScroll", "resizeLeftRight", "resizeUpDown", "resizeUpLeftDownRight", "resizeUpRightDownLeft", "resizeUp", "resizeDown", "resizeLeft", "resizeRight", "resizeUpLeft", "resizeUpRight", "resizeDownLeft", "resizeDownRight", "resizeColumn", "resizeRow", "zoomIn", "zoomOut", "defer", "uncontrolled", "clickable", "adaptiveClickable", "textable");
        assertEquals(41, cursors.size());
        for (String cursor : cursors) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.MouseRegion"),
                    Map.of(new PropertyName("cursor"), new PropertyValue.StringValue(cursor)), Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"cursor\":{\"kind\":\"string\",\"value\":\"" + cursor + "\"}"), json);
        }
        for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
            var value = new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app/cursor.dart"),
                    "PrivateMouseCursor", Optional.of("custom"), access,
                    access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE ? Optional.empty() : Optional.of(false));
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.MouseRegion"),
                    Map.of(new PropertyName("cursor"), value), Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"cursor\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertFalse(json.contains("PrivateMouseCursor"), json);
            assertFalse(json.contains("private_app"), json);
            assertFalse(json.contains("custom"), json);
            assertFalse(json.contains("cursor.dart"), json);
        }
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(),
                new PropertyValue.StringValue("SystemMouseCursors.click"),
                new PropertyValue.StringValue("executePrivateCode()"))) {
            var node = new WidgetNode(StableId.random(), type("flutter.widgets.MouseRegion"),
                    Map.of(new PropertyName("cursor"), invalid), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void mouseRegionPreservesNullableHitTestBehaviorAndBothOpaqueLiterals() throws Exception {
        for (boolean opaque : List.of(false, true)) {
            for (PropertyValue behavior : List.of(new PropertyValue.NullValue(),
                    new PropertyValue.EnumValue("HitTestBehavior", "deferToChild"),
                    new PropertyValue.EnumValue("HitTestBehavior", "opaque"),
                    new PropertyValue.EnumValue("HitTestBehavior", "translucent"))) {
                var node = new WidgetNode(StableId.random(), type("flutter.widgets.MouseRegion"),
                        Map.of(new PropertyName("opaque"), new PropertyValue.BooleanValue(opaque),
                                new PropertyName("hitTestBehavior"), behavior),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"opaque\":{\"kind\":\"boolean\",\"value\":" + opaque + "}"), json);
                String expected = behavior instanceof PropertyValue.EnumValue enumeration
                        ? "{\"kind\":\"enum\",\"type\":\"HitTestBehavior\",\"value\":\"" + enumeration.value() + "\"}"
                        : "{\"kind\":\"null\"}";
                assertTrue(json.contains("\"hitTestBehavior\":" + expected), json);
                assertTrue(json.contains("\"protocolVersion\":19"), json);
            }
        }
    }

    @Test
    void mouseRegionOpaqueStateConsumerNeverChangesLiteralCanvasBytes() throws Exception {
        var before = source();
        var source = new DartSourceDescriptor(before.dartFile(), before.className(), WidgetClassKind.STATEFUL,
                before.generatorVersion(), before.managedRegions());
        var codec = new CanvasModelPayloadCodec();
        for (boolean opaque : List.of(false, true)) {
            var node = new WidgetNode(id("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                    type("flutter.widgets.MouseRegion"),
                    Map.of(new PropertyName("opaque"), new PropertyValue.BooleanValue(opaque)), Map.of());
            byte[] literalBytes = codec.encode(request(new DesignerDocument(DOCUMENT_ID, source, node)));
            for (var transform : List.of(dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT,
                    dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.NOT)) {
                var binding = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_secretMouseState",
                        dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL, Optional.empty(), transform);
                var bound = new WidgetNode(node.id(), node.type(), node.properties(), node.slots(),
                        node.extensions(), Optional.empty(), Map.of(new PropertyName("opaque"), binding));
                byte[] boundBytes = codec.encode(request(new DesignerDocument(DOCUMENT_ID, source, bound)));
                assertArrayEquals(literalBytes, boundBytes);
                String json = new String(boundBytes, StandardCharsets.UTF_8);
                assertFalse(json.contains("secretMouseState"), json);
                assertFalse(json.contains("stateBinding"), json);
                assertFalse(json.contains("propertyBindings"), json);
                assertFalse(json.contains("transform"), json);
            }
        }
    }

    private static CanvasRenderRequest request(DesignerDocument document) {
        return request(PROFILE, document);
    }

    private static CanvasRenderRequest request(
            CanvasRenderProfile profile, DesignerDocument document) {
        return request(profile, document, CanvasImageResourceBundle.empty());
    }

    private static CanvasRenderRequest request(
            CanvasRenderProfile profile,
            DesignerDocument document,
            CanvasImageResourceBundle imageResources) {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var snapshot = ValidatedCanvasRevisionSnapshot.captureReadOnly(
                0, document, catalog, ValidationLimits.defaults());
        var gate = new CanvasPresentationGate(
                CanvasSessionId.parse("2c8ca807-93f6-4c6b-b16b-a2c1a438d79b"),
                DOCUMENT_ID);
        return gate.present(profile, snapshot, imageResources);
    }

    private static DesignerDocument document(boolean reversePropertyOrder) {
        WidgetNode firstText = text(
                "dc7d6474-55c1-49c4-9bdb-9e1777d358b9", "Hello");
        Map<PropertyName, PropertyValue> paddingProperties = new LinkedHashMap<>();
        paddingProperties.put(new PropertyName("padding"),
                new PropertyValue.EdgeInsetsValue(
                        BigDecimal.valueOf(16), BigDecimal.valueOf(16),
                        BigDecimal.valueOf(16), BigDecimal.valueOf(16)));
        WidgetNode padding = new WidgetNode(
                id("5dd92980-94e5-407f-abce-42b195461395"),
                type("flutter.widgets.Padding"),
                paddingProperties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(firstText)));
        WidgetNode centered = new WidgetNode(
                id("76da19bb-8a55-4482-945b-f8149105b9d2"),
                type("flutter.widgets.Center"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(
                        "4508c721-215c-4059-be29-3e38fd02b411", "World"))));
        WidgetNode limited = new WidgetNode(
                id("54e955a9-27e7-44ba-af99-9c2f8704e46d"),
                type("flutter.widgets.LimitedBox"),
                Map.of(
                        new PropertyName("maxWidth"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("320.5")),
                        new PropertyName("maxHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("180.25"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(centered)));
        WidgetNode unconstrained = new WidgetNode(
                id("2627534b-1391-4a86-b676-2682afe43a82"),
                type("flutter.widgets.UnconstrainedBox"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(limited)));
        WidgetNode constrained = new WidgetNode(
                id("c8ff02c4-d10c-4b5b-94ef-f412221595d7"),
                type("flutter.widgets.ConstrainedBox"),
                Map.of(new PropertyName("constraints"),
                        new PropertyValue.BoxConstraintsValue(
                                BigDecimal.ZERO, Optional.empty(),
                                BigDecimal.ZERO, Optional.empty())),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(unconstrained)));
        WidgetNode fitted = new WidgetNode(
                id("36e98233-03c4-40de-bf0c-8480a20abe45"),
                type("flutter.widgets.FittedBox"),
                Map.of(
                        new PropertyName("fit"),
                                new PropertyValue.EnumValue("BoxFit", "cover"),
                        new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.DIRECTIONAL,
                                        new BigDecimal("0.5"),
                                        new BigDecimal("-0.25")),
                        new PropertyName("clipBehavior"),
                                new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(constrained)));
        WidgetNode fractional = new WidgetNode(
                id("c7068cbf-70b6-439d-a25e-d70eec7fc942"),
                type("flutter.widgets.FractionallySizedBox"),
                Map.of(
                        new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.PHYSICAL,
                                        new BigDecimal("-0.5"),
                                        new BigDecimal("0.75")),
                        new PropertyName("widthFactor"),
                                new PropertyValue.DoubleValue(new BigDecimal("0.625")),
                        new PropertyName("heightFactor"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(fitted)));
        WidgetNode stack = new WidgetNode(
                id("447f1126-7b29-40c6-8595-7ae6c182bc06"),
                type("flutter.widgets.Stack"),
                Map.of(
                        new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.PHYSICAL,
                                        new BigDecimal("0.125"),
                                        new BigDecimal("-0.875")),
                        new PropertyName("textDirection"),
                                new PropertyValue.EnumValue("TextDirection", "ltr"),
                        new PropertyName("fit"),
                                new PropertyValue.EnumValue("StackFit", "passthrough"),
                        new PropertyName("clipBehavior"),
                                new PropertyValue.EnumValue(
                                        "Clip", "antiAliasWithSaveLayer")),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(fractional))));
        WidgetNode align = new WidgetNode(
                id("9c39207e-e9a0-438a-8350-d71474977bd2"),
                type("flutter.widgets.Align"),
                Map.of(
                        new PropertyName("alignment"),
                                new PropertyValue.AlignmentGeometryValue(
                                        PropertyValue.AlignmentGeometryValue
                                                .HorizontalBasis.DIRECTIONAL,
                                        new BigDecimal("0.25"),
                                        new BigDecimal("-0.75")),
                        new PropertyName("widthFactor"),
                                new PropertyValue.IntegerValue(BigInteger.ZERO),
                        new PropertyName("heightFactor"),
                                new PropertyValue.DoubleValue(new BigDecimal("1.5"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(stack)));
        WidgetNode opacity = new WidgetNode(
                id("c061d779-a0a3-46fb-9b0e-7b22397b51d2"),
                type("flutter.widgets.Opacity"),
                Map.of(
                        new PropertyName("opacity"),
                                new PropertyValue.DoubleValue(new BigDecimal("0.5")),
                        new PropertyName("alwaysIncludeSemantics"),
                                new PropertyValue.BooleanValue(true)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(align)));
        WidgetNode aspectRatio = new WidgetNode(
                id("ad9476e2-bef0-4c4b-afcf-2e184cb7f622"),
                type("flutter.widgets.AspectRatio"),
                Map.of(new PropertyName("aspectRatio"),
                        new PropertyValue.DoubleValue(
                                new BigDecimal("1.7777777777777777"))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(opacity)));
        WidgetNode sizedBox = new WidgetNode(
                id("0cc7c095-7908-461b-b5b2-fe085343b6b2"),
                type("flutter.widgets.SizedBox"),
                Map.of(
                        new PropertyName("width"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(120)),
                        new PropertyName("height"),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(48.5))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(aspectRatio)));
        WidgetNode row = new WidgetNode(
                id("1efdd73a-0602-4200-aa1e-28982f5a22ca"),
                type("flutter.widgets.Row"),
                Map.of(
                        new PropertyName("mainAxisAlignment"),
                        new PropertyValue.EnumValue("MainAxisAlignment", "end"),
                        new PropertyName("mainAxisSize"),
                        new PropertyValue.EnumValue("MainAxisSize", "min"),
                        new PropertyName("crossAxisAlignment"),
                        new PropertyValue.EnumValue("CrossAxisAlignment", "baseline"),
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl"),
                        new PropertyName("verticalDirection"),
                        new PropertyValue.EnumValue("VerticalDirection", "up"),
                        new PropertyName("textBaseline"),
                        new PropertyValue.EnumValue("TextBaseline", "ideographic"),
                        new PropertyName("spacing"),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(4))),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(sizedBox))));
        WidgetNode column = new WidgetNode(
                id("a39c394b-a85e-4d0c-b2c6-87174e20b7fd"),
                type("flutter.widgets.Column"),
                Map.of(
                        new PropertyName("mainAxisAlignment"),
                        new PropertyValue.EnumValue("MainAxisAlignment", "center"),
                        new PropertyName("mainAxisSize"),
                        new PropertyValue.EnumValue("MainAxisSize", "min"),
                        new PropertyName("crossAxisAlignment"),
                        new PropertyValue.EnumValue("CrossAxisAlignment", "baseline"),
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "ltr"),
                        new PropertyName("verticalDirection"),
                        new PropertyValue.EnumValue("VerticalDirection", "down"),
                        new PropertyName("textBaseline"),
                        new PropertyValue.EnumValue("TextBaseline", "alphabetic"),
                        new PropertyName("spacing"),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(12.5))),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(padding, row, icon()))));
        Map<PropertyName, PropertyValue> scaffoldProperties = new LinkedHashMap<>();
        PropertyName background = new PropertyName("backgroundColor");
        PropertyName resize = new PropertyName("resizeToAvoidBottomInset");
        if (reversePropertyOrder) {
            scaffoldProperties.put(resize, new PropertyValue.BooleanValue(true));
            scaffoldProperties.put(background, new PropertyValue.ColorValue(0xFFF8F8F8L));
        } else {
            scaffoldProperties.put(background, new PropertyValue.ColorValue(0xFFF8F8F8L));
            scaffoldProperties.put(resize, new PropertyValue.BooleanValue(true));
        }
        WidgetNode scaffold = new WidgetNode(
                id("26d317ed-9668-4eea-8ea0-f89f4700c7aa"),
                type("flutter.material.Scaffold"),
                scaffoldProperties,
                Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(column)));
        return new DesignerDocument(DOCUMENT_ID, source(), scaffold);
    }

    private static WidgetNode text(String id, String data) {
        boolean primary = data.equals("Hello");
        return new WidgetNode(
                id(id),
                type("flutter.widgets.Text"),
                primary
                        ? Map.ofEntries(
                                Map.entry(new PropertyName("data"), new PropertyValue.StringValue(data)),
                                Map.entry(new PropertyName("textAlign"),
                                        new PropertyValue.EnumValue("TextAlign", "center")),
                                Map.entry(new PropertyName("textDirection"),
                                        new PropertyValue.EnumValue("TextDirection", "ltr")),
                                Map.entry(new PropertyName("softWrap"),
                                        new PropertyValue.BooleanValue(false)),
                                Map.entry(new PropertyName("overflow"),
                                        new PropertyValue.EnumValue("TextOverflow", "ellipsis")),
                                Map.entry(new PropertyName("maxLines"),
                                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                                Map.entry(new PropertyName("semanticsLabel"),
                                        new PropertyValue.StringValue("Primary greeting")),
                                Map.entry(new PropertyName("semanticsIdentifier"),
                                        new PropertyValue.StringValue("primary-greeting")),
                                Map.entry(new PropertyName("textWidthBasis"),
                                        new PropertyValue.EnumValue("TextWidthBasis", "longestLine")),
                                Map.entry(new PropertyName("selectionColor"),
                                        new PropertyValue.ColorValue(0xFF336699L)),
                                Map.entry(new PropertyName("styleFontSize"),
                                        new PropertyValue.DoubleValue(BigDecimal.valueOf(18))),
                                Map.entry(new PropertyName("styleFontWeight"),
                                        new PropertyValue.EnumValue("FontWeight", "w700")),
                                Map.entry(new PropertyName("styleColor"),
                                        new PropertyValue.ColorValue(0xFF112233L)),
                                Map.entry(new PropertyName("textScalerFactor"),
                                        new PropertyValue.DoubleValue(new BigDecimal("1.25"))))
                        : Map.of(
                                new PropertyName("data"), new PropertyValue.StringValue(data),
                                new PropertyName("semanticsIdentifier"),
                                new PropertyValue.StringValue("text-" + id)),
                Map.of());
    }

    private static WidgetNode icon() {
        return new WidgetNode(
                id("c247b460-bbf5-43f3-b7cf-5fba6e8944ed"),
                type("flutter.widgets.Icon"),
                Map.of(
                        new PropertyName("icon"),
                        new PropertyValue.IconDataValue(
                                Optional.of(0xE092),
                                Optional.of("MaterialIcons"),
                                Optional.empty(),
                                true,
                                List.of())),
                Map.of());
    }

    private static DartSourceDescriptor source() {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("secret-generator"),
                new ManagedRegions(
                        new ManagedRegion("A".repeat(64)),
                        new ManagedRegion("B".repeat(64))));
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }


    @Test
    void circleAvatarBothImageLayersResolveAll52ProviderBranchesWithDeduplicatedResources() throws Exception {
        int count = 0;
        for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) for (boolean packaged : List.of(false, true)) {
            var resizes = new java.util.ArrayList<Optional<PropertyValue.ImageProviderValue.ResizeImageConfig>>();
            resizes.add(Optional.empty());
            for (int dimensions = 1; dimensions <= 3; dimensions++) for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) for (boolean upscale : List.of(false, true)) {
                resizes.add(Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig((dimensions & 1) != 0 ? Optional.of(16) : Optional.empty(), (dimensions & 2) != 0 ? Optional.of(24) : Optional.empty(), policy, upscale)));
            }
            for (var resize : resizes) {
                var provider = new PropertyValue.ImageProviderValue(kind, "assets/avatar.png", packaged ? Optional.of("reviewed_avatars") : Optional.empty(),
                        kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new BigDecimal("2.5")) : Optional.empty(), resize);
                var avatar = new WidgetNode(StableId.random(), type("flutter.material.CircleAvatar"), Map.of(
                        new PropertyName("backgroundImage"), provider, new PropertyName("foregroundImage"), provider,
                        new PropertyName("backgroundColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")),
                        new PropertyName("foregroundColor"), new PropertyValue.ColorValue(0xffffffffL),
                        new PropertyName("onBackgroundImageError"), new PropertyValue.CallbackValue("privateBackgroundError"),
                        new PropertyName("onForegroundImageError"), new PropertyValue.CallbackValue("privateForegroundError"),
                        new PropertyName("maxRadius"), new PropertyValue.EnumValue("double", "infinity")), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
                var resource = CanvasImageResource.create(CanvasImageFormat.PNG, 40, 30, new byte[]{4, 3, 2, 1});
                var assetId = new CanvasImageAssetId(provider.packageName(), provider.assetName());
                var bundle = new CanvasImageResourceBundle(List.of(new CanvasImageAsset(assetId, resource.resourceId(), List.of(new CanvasImageVariant(BigDecimal.ONE, resource.resourceId())))), List.of(resource));
                var request = request(PROFILE, new DesignerDocument(DOCUMENT_ID, source(), avatar), bundle);
                var codec = new CanvasModelPayloadCodec();
                String json = new String(codec.encode(request), StandardCharsets.UTF_8);
                assertArrayEquals(codec.encode(request), codec.encode(request));
                for (String layer : List.of("backgroundImage", "foregroundImage")) {
                    assertTrue(json.contains("\"" + layer + "\":{\"kind\":\"imageProvider\",\"value\":{\"kind\":\"" + kind.wireName() + "\""), json);
                }
                assertTrue(json.contains("\"maxRadius\":{\"kind\":\"enum\",\"type\":\"double\",\"value\":\"infinity\"}"), json);
                assertTrue(json.contains("\"kind\":\"callbackPresence\""), json);
                assertFalse(json.contains("privateBackgroundError"), json);
                assertFalse(json.contains("privateForegroundError"), json);
                assertTrue(json.contains("\"resolution\":{\"kind\":\"resolved\""), json);
                assertEquals(packaged, json.contains("reviewed_avatars"), json);
                assertEquals(resize.isPresent(), json.contains("\"allowUpscaling\":"), json);
                resize.ifPresent(value -> {
                    assertTrue(json.contains("\"policy\":\"" + value.policy().wireName() + "\""), json);
                    assertTrue(json.contains("\"allowUpscaling\":" + value.allowUpscaling()), json);
                });
                assertFalse(json.contains("MaterialIcons"), json);
                assertThrows(CanvasModelPayloadException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), avatar))));
                count++;
            }
        }
        assertEquals(52, count);
    }

    @Test
    void circleAvatarEmptyAndInfinityNeedNoImagesWhileRetainedUnresolvedNeedsExactIssueClosure() throws Exception {
        var empty = new WidgetNode(StableId.random(), type("flutter.material.CircleAvatar"), Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertFalse(json.contains("imageProvider"), json);
        assertFalse(json.contains("MaterialIcons"), json);
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            var infinity = new WidgetNode(empty.id(), empty.type(), Map.of(new PropertyName(name), new PropertyValue.EnumValue("double", "infinity")), empty.slots());
            String encoded = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), infinity))), StandardCharsets.UTF_8);
            assertTrue(encoded.contains("\"" + name + "\":{\"kind\":\"enum\",\"type\":\"double\",\"value\":\"infinity\"}"), encoded);
            assertFalse(encoded.contains("imageProvider"), encoded);
        }
        var provider = PropertyValue.ImageProviderValue.unresolved();
        var unresolved = new WidgetNode(empty.id(), empty.type(), Map.of(new PropertyName("backgroundImage"), provider, new PropertyName("foregroundImage"), provider), empty.slots());
        var issue = new dev.flutter.netbeans.designer.canvas.CanvasImageResolutionIssue(
                CanvasImageAssetId.application(provider.assetName()),
                dev.flutter.netbeans.designer.canvas.CanvasImageResolutionIssue.Code.UNDECLARED,
                "CircleAvatar images are not declared; retain background and Child fallback.");
        var bundle = new CanvasImageResourceBundle(List.of(), List.of(), List.of(issue));
        String encoded = new String(new CanvasModelPayloadCodec().encode(request(PROFILE, new DesignerDocument(DOCUMENT_ID, source(), unresolved), bundle)), StandardCharsets.UTF_8);
        assertTrue(encoded.contains("CircleAvatar images are not declared"), encoded);
        assertTrue(encoded.contains("__netbeans_flutter_designer__/unresolved-image.png"), encoded);
        assertThrows(CanvasModelPayloadException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), unresolved))));
        assertThrows(CanvasModelPayloadException.class, () -> new CanvasModelPayloadCodec().encode(request(PROFILE, new DesignerDocument(DOCUMENT_ID, source(), empty), bundle)));
    }

    @Test
    void circleAvatarPayloadRejectsConflictsNullProvidersOrphansAndForeignInfinity() {
        var invalid = List.of(
                Map.of(new PropertyName("radius"), new PropertyValue.DoubleValue(BigDecimal.ONE), new PropertyName("minRadius"), new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                Map.of(new PropertyName("minRadius"), new PropertyValue.DoubleValue(BigDecimal.TEN), new PropertyName("maxRadius"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("onBackgroundImageError"), new PropertyValue.CallbackValue("onError")),
                Map.of(new PropertyName("onForegroundImageError"), new PropertyValue.CallbackValue("onError")),
                Map.of(new PropertyName("backgroundImage"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("foregroundImage"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("radius"), new PropertyValue.StringValue("Infinity")),
                Map.of(new PropertyName("maxRadius"), new PropertyValue.EnumValue("double", "nan")));
        for (var properties : invalid) {
            var avatar = new WidgetNode(StableId.random(), type("flutter.material.CircleAvatar"), new java.util.LinkedHashMap<PropertyName, PropertyValue>(properties), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), avatar))));
        }
    }



    @Test
    void linearProgressPayloadKeepsScalarAnimationAlternativesResourceFreeAndDoesNotLeakReferences() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_progress/values.dart"),
                "secretAnimation", Optional.of("secretMember"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.ColorValue(0x80123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")), reference)) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("valueColor"), value);
            properties.put(new PropertyName("controller"), reference);
            properties.put(new PropertyName("backgroundColor"), new PropertyValue.ColorValue(0xff000000L));
            properties.put(new PropertyName("color"), new PropertyValue.ColorValue(0xffffffffL));
            properties.put(new PropertyName("stopIndicatorColor"), new PropertyValue.ColorValue(0xff112233L));
            properties.put(new PropertyName("minHeight"), new PropertyValue.EnumValue("double", "infinity"));
            properties.put(new PropertyName("stopIndicatorRadius"), new PropertyValue.DoubleValue(new BigDecimal("-2")));
            properties.put(new PropertyName("trackGap"), new PropertyValue.EnumValue("double", "infinity"));
            properties.put(new PropertyName("year2023"), new PropertyValue.BooleanValue(false));
            properties.put(new PropertyName("semanticsLabel"), new PropertyValue.StringValue("Loading"));
            properties.put(new PropertyName("semanticsValue"), new PropertyValue.StringValue("Working"));
            var radius = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.TEN);
            properties.put(new PropertyName("borderRadius"), new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(radius, radius, radius, radius)));
            var node = new WidgetNode(StableId.random(), type("flutter.material.LinearProgressIndicator"), properties, Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"controller\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertEquals(value instanceof PropertyValue.NullValue, json.contains("\"valueColor\":{\"kind\":\"null\"}"), json);
            assertTrue(json.contains("\"minHeight\":{\"kind\":\"enum\",\"type\":\"double\",\"value\":\"infinity\"}"), json);
            assertTrue(json.contains("\"year2023\":{\"kind\":\"boolean\",\"value\":false}"), json);
            assertFalse(json.contains("secretAnimation"), json);
            assertFalse(json.contains("secretMember"), json);
            assertFalse(json.contains("private_progress"), json);
            assertFalse(json.contains("MaterialIcons"), json);
            assertFalse(json.contains("imageProvider"), json);
        }
    }

    @Test
    void linearProgressPayloadPreservesOutOfRangeProgressAndEmptyIndeterminateDefaults() throws Exception {
        var empty = new WidgetNode(StableId.random(), type("flutter.material.LinearProgressIndicator"), Map.of(), Map.of());
        String initial = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertFalse(initial.contains("\"valueColor\":"), initial);
        assertFalse(initial.contains("\"controller\":"), initial);
        for (String number : List.of("-10", "0", "0.5", "2", "1e308")) {
            var node = new WidgetNode(empty.id(), empty.type(), Map.of(new PropertyName("value"), new PropertyValue.DoubleValue(new BigDecimal(number))), Map.of());
            String encoded = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(encoded.contains("\"value\":{\"kind\":\"double\""), encoded);
            assertFalse(encoded.contains("\"controller\":"), encoded);
        }
    }

    @Test
    void linearProgressPayloadRejectsWrongGrammarValuesAndConflictingProgressModes() {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "animation", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var invalid = List.of(
                Map.of(new PropertyName("value"), new PropertyValue.DoubleValue(BigDecimal.ONE), new PropertyName("controller"), reference),
                Map.of(new PropertyName("minHeight"), new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                Map.of(new PropertyName("value"), new PropertyValue.EnumValue("double", "infinity")),
                Map.of(new PropertyName("trackGap"), new PropertyValue.EnumValue("double", "negativeInfinity")),
                Map.of(new PropertyName("controller"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("valueColor"), new PropertyValue.DartExpressionValue("Animation<Color?>()")));
        for (var properties : invalid) {
            var node = new WidgetNode(StableId.random(), type("flutter.material.LinearProgressIndicator"),
                    new java.util.LinkedHashMap<PropertyName, PropertyValue>(properties), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }


    @Test
    void circularProgressPayloadKeepsScalarAnimationAlternativesResourceFreeAndDoesNotLeakReferences() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_progress/values.dart"),
                "secretAnimation", Optional.of("secretMember"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        for (String variant : List.of("material", "adaptive")) for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.ColorValue(0x80123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")), reference)) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            properties.put(new PropertyName("valueColor"), value);
            properties.put(new PropertyName("controller"), reference);
            properties.put(new PropertyName("backgroundColor"), new PropertyValue.ColorValue(0xff000000L));
            if (variant.equals("material")) properties.put(new PropertyName("color"), new PropertyValue.ColorValue(0xffffffffL));
            properties.put(new PropertyName("strokeWidth"), new PropertyValue.DoubleValue(new BigDecimal("-3")));
            properties.put(new PropertyName("strokeCap"), new PropertyValue.EnumValue("StrokeCap", "round"));
            properties.put(new PropertyName("padding"), new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE));
            properties.put(new PropertyName("strokeAlign"), new PropertyValue.DoubleValue(new BigDecimal("-2")));
            properties.put(new PropertyName("trackGap"), new PropertyValue.EnumValue("double", "infinity"));
            properties.put(new PropertyName("year2023"), new PropertyValue.BooleanValue(false));
            properties.put(new PropertyName("semanticsLabel"), new PropertyValue.StringValue("Loading"));
            properties.put(new PropertyName("semanticsValue"), new PropertyValue.StringValue("Working"));
            properties.put(new PropertyName("constraints"), new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.empty(), BigDecimal.ONE, Optional.of(BigDecimal.TEN)));
            var node = new WidgetNode(StableId.random(), type("flutter.material.CircularProgressIndicator"), properties, Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"controller\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertEquals(value instanceof PropertyValue.NullValue, json.contains("\"valueColor\":{\"kind\":\"null\"}"), json);
            assertTrue(json.contains("\"trackGap\":{\"kind\":\"enum\",\"type\":\"double\",\"value\":\"infinity\"}"), json);
            assertTrue(json.contains("\"year2023\":{\"kind\":\"boolean\",\"value\":false}"), json);
            assertFalse(json.contains("secretAnimation"), json);
            assertFalse(json.contains("secretMember"), json);
            assertFalse(json.contains("private_progress"), json);
            assertFalse(json.contains("MaterialIcons"), json);
            assertFalse(json.contains("imageProvider"), json);
        }
    }

    @Test
    void circularProgressPayloadPreservesOutOfRangeProgressAndEmptyIndeterminateDefaults() throws Exception {
        var empty = new WidgetNode(StableId.random(), type("flutter.material.CircularProgressIndicator"), Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material")), Map.of());
        String initial = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertFalse(initial.contains("\"valueColor\":"), initial);
        assertFalse(initial.contains("\"controller\":"), initial);
        for (String number : List.of("-10", "0", "0.5", "2", "1e308")) {
            var node = new WidgetNode(empty.id(), empty.type(), Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material"), new PropertyName("value"), new PropertyValue.DoubleValue(new BigDecimal(number))), Map.of());
            String encoded = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(encoded.contains("\"value\":{\"kind\":\"double\""), encoded);
            assertFalse(encoded.contains("\"controller\":"), encoded);
        }
    }

    @Test
    void circularProgressPayloadRejectsWrongGrammarValuesAndConflictingProgressModes() {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "animation", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var invalid = List.of(
                Map.of(new PropertyName("value"), new PropertyValue.DoubleValue(BigDecimal.ONE), new PropertyName("controller"), reference),
                Map.of(new PropertyName("strokeWidth"), new PropertyValue.EnumValue("double", "infinity")),
                Map.of(new PropertyName("strokeAlign"), new PropertyValue.EnumValue("double", "infinity")),
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"), new PropertyName("color"), new PropertyValue.ColorValue(0xff000000L)),
                Map.of(new PropertyName("value"), new PropertyValue.EnumValue("double", "infinity")),
                Map.of(new PropertyName("trackGap"), new PropertyValue.EnumValue("double", "negativeInfinity")),
                Map.of(new PropertyName("controller"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("valueColor"), new PropertyValue.DartExpressionValue("Animation<Color?>()")));
        for (var properties : invalid) {
            var merged = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            merged.put(new PropertyName("variant"), new PropertyValue.StringValue("material"));
            merged.putAll(properties);
            var node = new WidgetNode(StableId.random(), type("flutter.material.CircularProgressIndicator"),
                    merged, Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void refreshProgressPayloadPreservesAllTwelveFieldsAndNeverLeaksAnimationReferences() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_refresh/values.dart"),
                "privateAnimation", Optional.of("privateMember"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        for (PropertyValue color : List.of(new PropertyValue.NullValue(), new PropertyValue.ColorValue(0x80123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")), reference)) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("value"), new PropertyValue.DoubleValue(new BigDecimal("1.5")));
            properties.put(new PropertyName("valueColor"), color);
            properties.put(new PropertyName("backgroundColor"), new PropertyValue.ColorValue(0xff123456L));
            properties.put(new PropertyName("color"), new PropertyValue.ColorValue(0x8000ff00L));
            properties.put(new PropertyName("strokeWidth"), new PropertyValue.NullValue());
            properties.put(new PropertyName("strokeAlign"), new PropertyValue.DoubleValue(new BigDecimal("-4")));
            properties.put(new PropertyName("strokeCap"), new PropertyValue.EnumValue("StrokeCap", "square"));
            properties.put(new PropertyName("elevation"), new PropertyValue.DoubleValue(BigDecimal.ZERO));
            properties.put(new PropertyName("indicatorMargin"), new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE));
            properties.put(new PropertyName("indicatorPadding"), new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE));
            properties.put(new PropertyName("semanticsLabel"), new PropertyValue.StringValue("Refreshing"));
            properties.put(new PropertyName("semanticsValue"), new PropertyValue.StringValue("45%"));
            assertEquals(12, properties.size());
            var node = new WidgetNode(StableId.random(), type("flutter.material.RefreshProgressIndicator"), properties, Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"strokeWidth\":{\"kind\":\"null\"}"), json);
            assertEquals(color instanceof PropertyValue.NullValue, json.contains("\"valueColor\":{\"kind\":\"null\"}"), json);
            assertEquals(color instanceof PropertyValue.DartObjectReferenceValue, json.contains("\"valueColor\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            for (String forbidden : List.of("privateAnimation", "privateMember", "private_refresh", "MaterialIcons", "imageProvider")) assertFalse(json.contains(forbidden), json);
        }
    }

    @Test
    void refreshProgressPayloadDistinguishesOmittedExplicitNullZeroAndDefaultWidth() throws Exception {
        var encodings = new java.util.HashSet<String>();
        for (PropertyValue width : java.util.Arrays.asList(null, new PropertyValue.NullValue(),
                new PropertyValue.DoubleValue(BigDecimal.ZERO), new PropertyValue.DoubleValue(new BigDecimal("2.5")))) {
            var properties = width == null ? Map.<PropertyName, PropertyValue>of() : Map.of(new PropertyName("strokeWidth"), width);
            var node = new WidgetNode(DOCUMENT_ID, type("flutter.material.RefreshProgressIndicator"), properties, Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(encodings.add(json));
            assertEquals(width != null, json.contains("\"strokeWidth\":"), json);
            assertFalse(json.contains("\"valueColor\":"), json);
            assertFalse(json.contains("\"value\":{\"kind\":"), json);
        }
    }

    @Test
    void refreshProgressPayloadRejectsInheritedArgumentsAndUnsupportedNullsWithoutBroaderTypes() {
        var invalid = new java.util.ArrayList<Map<PropertyName, PropertyValue>>();
        for (String name : List.of("controller", "variant", "constraints", "padding", "trackGap", "year2023")) {
            invalid.add(Map.of(new PropertyName(name), new PropertyValue.DoubleValue(BigDecimal.ONE)));
        }
        for (String name : List.of("value", "strokeAlign", "elevation", "indicatorMargin", "indicatorPadding", "strokeCap")) {
            invalid.add(Map.of(new PropertyName(name), new PropertyValue.NullValue()));
        }
        invalid.add(Map.of(new PropertyName("elevation"), new PropertyValue.DoubleValue(BigDecimal.ONE.negate())));
        invalid.add(Map.of(new PropertyName("strokeWidth"), new PropertyValue.EnumValue("double", "infinity")));
        invalid.add(Map.of(new PropertyName("valueColor"), new PropertyValue.DartExpressionValue("Animation<Color?>()")));
        for (var properties : invalid) {
            var node = new WidgetNode(StableId.random(), type("flutter.material.RefreshProgressIndicator"), properties, Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void appBarPredicatePayloadPreservesPresetsAndStripsAllProjectReferenceIdentity() throws Exception {
        for (PropertyValue value : java.util.Arrays.asList(null,
                new PropertyValue.StringValue("default"), new PropertyValue.StringValue("depthZero"),
                new PropertyValue.StringValue("all"),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "privatePredicate", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app_bar/predicates.dart"),
                        "privateOwner", Optional.of("privateGetter"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_app_bar/predicates.dart"),
                        "privateOwner", Optional.of("privateFactory"),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            var properties = value == null ? Map.<PropertyName, PropertyValue>of()
                    : Map.of(new PropertyName("notificationPredicate"), value);
            var node = new WidgetNode(DOCUMENT_ID, type("flutter.material.AppBar"), properties, Map.of());
            String json = new String(new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertEquals(value != null, json.contains("\"notificationPredicate\":"), json);
            assertEquals(value instanceof PropertyValue.DartObjectReferenceValue,
                    json.contains("\"notificationPredicate\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            if (value instanceof PropertyValue.StringValue preset) {
                assertTrue(json.contains("\"notificationPredicate\":{\"kind\":\"string\",\"value\":\""
                        + preset.value() + "\"}"), json);
            }
            for (String forbidden : List.of("privatePredicate", "privateOwner", "privateGetter", "privateFactory",
                    "private_app_bar", "rootSymbol", "member", "zeroArgumentInvocation", "ScrollNotificationPredicate")) {
                assertFalse(json.contains(forbidden), json);
            }
        }
    }

    @Test
    void appBarPredicatePayloadRejectsNullCallbacksRawSourceAndUnreviewedPresets() {
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("acceptScroll"),
                new PropertyValue.DartExpressionValue("(_) => true"), new PropertyValue.BooleanValue(true),
                new PropertyValue.StringValue("projectPredicate"))) {
            var node = new WidgetNode(DOCUMENT_ID, type("flutter.material.AppBar"),
                    Map.of(new PropertyName("notificationPredicate"), value), Map.of());
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))), value.toString());
        }
    }

    @Test
    void refreshIndicatorPayloadRetainsAllThreeVariantsRequiredChildAndPrivateReferencePresence() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:private_refresh/callbacks.dart"),
                "privateRefresh", Optional.of("privateMember"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        for (String variant : List.of("material", "adaptive", "noSpinner")) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            properties.put(new PropertyName("onRefresh"), reference);
            properties.put(new PropertyName("notificationPredicate"), reference);
            properties.put(new PropertyName("semanticsLabel"), new PropertyValue.StringValue("Refresh records"));
            properties.put(new PropertyName("semanticsValue"), new PropertyValue.StringValue("45%"));
            properties.put(new PropertyName("elevation"), new PropertyValue.DoubleValue(BigDecimal.ZERO));
            properties.put(new PropertyName("triggerMode"), new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", "anywhere"));
            if (variant.equals("noSpinner")) properties.put(new PropertyName("onStatusChange"), reference);
            else {
                properties.put(new PropertyName("displacement"), new PropertyValue.DoubleValue(BigDecimal.TEN));
                properties.put(new PropertyName("edgeOffset"), new PropertyValue.DoubleValue(BigDecimal.ONE.negate()));
                properties.put(new PropertyName("strokeWidth"), new PropertyValue.DoubleValue(new BigDecimal("-2.5")));
                properties.put(new PropertyName("color"), new PropertyValue.ColorValue(0xff123456L));
                properties.put(new PropertyName("backgroundColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
            }
            var node = refreshIndicatorPayloadNode(properties);
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"onRefresh\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"notificationPredicate\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertEquals(variant.equals("noSpinner"), json.contains("\"onStatusChange\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("Refresh child"), json);
            for (String forbidden : List.of("privateRefresh", "privateMember", "private_refresh", "imageProvider", "MaterialIcons")) assertFalse(json.contains(forbidden), json);
        }
    }

    @Test
    void refreshIndicatorPayloadKeepsPresetsSeparateFromCustomPredicateAndOmission() throws Exception {
        for (String preset : List.of("unset", "default", "depthZero", "all")) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue("material"));
            if (!preset.equals("unset")) properties.put(new PropertyName("notificationPredicate"), new PropertyValue.StringValue(preset));
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), refreshIndicatorPayloadNode(properties)))), StandardCharsets.UTF_8);
            assertEquals(!preset.equals("unset"), json.contains("\"notificationPredicate\":"), json);
            assertFalse(json.contains("\"onRefresh\":"), json);
            assertFalse(json.contains("dartObjectReferencePresence"), json);
        }
    }

    @Test
    void refreshIndicatorPayloadRejectsBranchConflictsNullAndMissingChild() {
        var cases = new java.util.ArrayList<Map<PropertyName, PropertyValue>>();
        cases.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("noSpinner"), new PropertyName("color"), new PropertyValue.ColorValue(0xff123456L)));
        cases.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material"), new PropertyName("strokeWidth"), new PropertyValue.NullValue()));
        cases.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"), new PropertyName("controller"), new PropertyValue.StringValue("controller")));
        cases.add(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material"), new PropertyName("onRefresh"), new PropertyValue.CallbackValue("refresh")));
        cases.add(Map.of());
        for (var properties : cases) {
            var document = new DesignerDocument(DOCUMENT_ID, source(), refreshIndicatorPayloadNode(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(document)));
        }
        var empty = new WidgetNode(StableId.random(), type("flutter.material.RefreshIndicator"),
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material")), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))));
    }

    private static WidgetNode refreshIndicatorPayloadNode(Map<PropertyName, PropertyValue> properties) {
        var child = new WidgetNode(StableId.random(), type("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Refresh child")), Map.of());
        return new WidgetNode(StableId.random(), type("flutter.material.RefreshIndicator"), properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(child))));
    }

    @Test
    void textButtonPayloadCarriesDenseLocalStylesBothSlotsAndOnlyPrivateReferencePresence() throws Exception {
        for (boolean icon : List.of(false, true)) for (boolean paints : List.of(false, true)) {
            var properties = dev.flutter.netbeans.designer.catalog.TextButtonTestValues.full(icon, paints, false);
            var node = dev.flutter.netbeans.designer.catalog.TextButtonTestValues.node(properties);
            if (icon) {
                var slots = new java.util.LinkedHashMap<>(node.slots());
                slots.put(new SlotName("icon"), new WidgetSlot.SingleSlot(Optional.of(
                        dev.flutter.netbeans.designer.catalog.TextButtonTestValues.text("Preserved icon"))));
                node = new WidgetNode(node.id(), node.type(), node.properties(), slots);
            }
            assertEquals(491, node.properties().size());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"onPressed\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"statesController\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"styleBackgroundBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"styleErrorBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleDraggedBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleSelectedBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleScrolledUnderBackgroundColor\":"), json);
            assertEquals(icon, json.contains("Preserved icon"), json);
            for (String forbidden : List.of("package:buttons", "buttonValues", "styles.dart", "MaterialIcons", "imageProvider")) assertFalse(json.contains(forbidden), json);
        }
    }

    @Test
    void textButtonPayloadPreservesExplicitNullAndWholeStyleSentinelWithoutExecutingReferences() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.TextButtonTestValues.node(Map.of(
                new PropertyName("clipBehavior"), new PropertyValue.NullValue(),
                new PropertyName("isSemanticButton"), new PropertyValue.NullValue(),
                new PropertyName("style"), dev.flutter.netbeans.designer.catalog.TextButtonTestValues.reference("wholeStyle")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"clipBehavior\":{\"kind\":\"null\"}"), json);
        assertTrue(json.contains("\"isSemanticButton\":{\"kind\":\"null\"}"), json);
        assertTrue(json.contains("\"style\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
        assertFalse(json.contains("wholeStyle"), json);
        assertFalse(json.contains("styleBackgroundColor"), json);
    }

    @Test
    void textButtonPayloadRejectsConflictingConstructorsStylesAndMissingRequiredChild() {
        var cases = List.of(
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("icon"), new PropertyName("isSemanticButton"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("style"), dev.flutter.netbeans.designer.catalog.TextButtonTestValues.reference("style"), new PropertyName("styleIconColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(new PropertyName("enabled"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onPressed"), new PropertyValue.CallbackValue("legacyCallback")));
        for (Map<PropertyName, ? extends PropertyValue> properties : cases) {
            var node = dev.flutter.netbeans.designer.catalog.TextButtonTestValues.node(new java.util.LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                dev.flutter.netbeans.designer.catalog.TextButtonTestValues.definition(), StableId.random());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), prototype))));
    }

    @Test
    void outlinedButtonPayloadCarriesDenseLocalStylesBothSlotsAndOnlyPrivateReferencePresence() throws Exception {
        for (boolean icon : List.of(false, true)) for (boolean paints : List.of(false, true)) {
            var properties = dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.full(icon, paints, false);
            var node = dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.node(properties);
            if (icon) {
                var slots = new java.util.LinkedHashMap<>(node.slots());
                slots.put(new SlotName("icon"), new WidgetSlot.SingleSlot(Optional.of(
                        dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.text("Preserved icon"))));
                node = new WidgetNode(node.id(), node.type(), node.properties(), slots);
            }
            assertEquals(icon ? 491 : 490, node.properties().size());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"onPressed\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"statesController\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"styleBackgroundBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"styleErrorBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleDraggedBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleSelectedBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleScrolledUnderBackgroundColor\":"), json);
            assertEquals(icon, json.contains("Preserved icon"), json);
            for (String forbidden : List.of("package:buttons", "buttonValues", "styles.dart", "MaterialIcons", "imageProvider")) assertFalse(json.contains(forbidden), json);
        }
    }

    @Test
    void outlinedButtonPayloadPreservesExplicitNullAndWholeStyleSentinelWithoutExecutingReferences() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.node(Map.of(
                new PropertyName("clipBehavior"), new PropertyValue.NullValue(),
                new PropertyName("style"), dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.reference("wholeStyle")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"clipBehavior\":{\"kind\":\"null\"}"), json);
        assertTrue(json.contains("\"style\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
        assertFalse(json.contains("wholeStyle"), json);
        assertFalse(json.contains("styleBackgroundColor"), json);
    }

    @Test
    void outlinedButtonPayloadRejectsConflictingConstructorsStylesAndMissingRequiredChild() {
        var cases = List.of(
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("icon"), new PropertyName("isSemanticButton"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("style"), dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.reference("style"), new PropertyName("styleIconColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(new PropertyName("enabled"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onPressed"), new PropertyValue.CallbackValue("legacyCallback")));
        for (Map<PropertyName, ? extends PropertyValue> properties : cases) {
            var node = dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.node(new java.util.LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                dev.flutter.netbeans.designer.catalog.OutlinedButtonTestValues.definition(), StableId.random());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), prototype))));
    }

    @Test
    void floatingActionButtonPayloadPreservesAllFourConstructorsAndPrivateObjectReferencePresence() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema.variants()) for (boolean paints : List.of(false, true)) {
            var values = dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.full(variant, "linear", paints);
            for (String name : List.of("onPressed", "heroTag", "mouseCursor", "focusNode")) {
                values.put(new PropertyName(name), dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.reference(name));
            }
            var node = dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.node(values);
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            for (String name : List.of("onPressed", "heroTag", "mouseCursor", "focusNode")) {
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            }
            assertTrue(json.contains("\"shapeKind\":"));
            assertEquals(variant.equals("extended"), json.contains("\"extendedTextStyleFontFeatures\":"));
            for (String forbidden : List.of("package:app", "fabValues", "fab_values.dart", "MaterialIcons", "imageProvider")) assertFalse(json.contains(forbidden), json);
        }
    }

    @Test
    void floatingActionButtonPayloadRetainsExactHeroNullAndLiteralKindsPlusCanonicalInfinity() throws Exception {
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("quote'\\\n$tag"),
                new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-42)), new PropertyValue.DoubleValue(new java.math.BigDecimal("2.5")),
                new PropertyValue.BooleanValue(false))) {
            var node = dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.node(Map.of(new PropertyName("heroTag"), value,
                    new PropertyName("elevation"), new PropertyValue.EnumValue("double", "infinity")));
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"heroTag\":{\"kind\":\"" + value.kind().wireName() + "\""), json);
            assertTrue(json.contains("infinity"));
            assertFalse(json.contains("imageProvider"));
            assertFalse(json.contains("MaterialIcons"));
        }
    }

    @Test
    void floatingActionButtonPayloadRejectsWrongConstructorsMissingLabelAndConflictingShapes() {
        for (var properties : List.of(
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("small"), new PropertyName("mini"), new PropertyValue.BooleanValue(false)),
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("large"), new PropertyName("extendedIconLabelSpacing"), new PropertyValue.DoubleValue(java.math.BigDecimal.ONE)),
                Map.of(new PropertyName("shape"), dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.reference("shape"), new PropertyName("shapeKind"), new PropertyValue.StringValue("circle")))) {
            var node = dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.node(new java.util.LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.definition(), StableId.random(),
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("extended")));
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), prototype))));
    }

    @Test
    void filledButtonPayloadCarriesDenseLocalStylesBothSlotsAndOnlyPrivateReferencePresence() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.FilledButtonWidgetPropertySchema.variants()) for (boolean paints : List.of(false, true)) {
            boolean icon = variant.equals("icon") || variant.equals("tonalIcon");
            var properties = dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.full(icon, paints, false);
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            var node = dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.node(properties);
            if (icon) {
                var slots = new java.util.LinkedHashMap<>(node.slots());
                slots.put(new SlotName("icon"), new WidgetSlot.SingleSlot(Optional.of(
                        dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.text("Preserved icon"))));
                node = new WidgetNode(node.id(), node.type(), node.properties(), slots);
            }
            assertEquals(icon ? 491 : 490, node.properties().size());
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"onPressed\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"statesController\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"styleBackgroundBuilder\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
            assertTrue(json.contains("\"styleErrorBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleDraggedBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleSelectedBackgroundColor\":"), json);
            assertTrue(json.contains("\"styleScrolledUnderBackgroundColor\":"), json);
            assertEquals(icon, json.contains("Preserved icon"), json);
            for (String forbidden : List.of("package:buttons", "buttonValues", "styles.dart", "MaterialIcons", "imageProvider")) assertFalse(json.contains(forbidden), json);
        }
    }

    @Test
    void filledButtonPayloadPreservesExplicitNullAndWholeStyleSentinelWithoutExecutingReferences() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.node(Map.of(
                new PropertyName("clipBehavior"), new PropertyValue.NullValue(),
                new PropertyName("style"), dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.reference("wholeStyle")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"clipBehavior\":{\"kind\":\"null\"}"), json);
        assertTrue(json.contains("\"style\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
        assertFalse(json.contains("wholeStyle"), json);
        assertFalse(json.contains("styleBackgroundColor"), json);
    }

    @Test
    void filledButtonPayloadRejectsConflictingConstructorsStylesAndMissingRequiredChild() {
        var cases = List.of(
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("icon"), new PropertyName("isSemanticButton"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("style"), dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.reference("style"), new PropertyName("styleIconColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(new PropertyName("enabled"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onPressed"), new PropertyValue.CallbackValue("legacyCallback")));
        for (Map<PropertyName, ? extends PropertyValue> properties : cases) {
            var node = dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.node(new java.util.LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                dev.flutter.netbeans.designer.catalog.FilledButtonTestValues.definition(), StableId.random());
        var iconPrototype = new WidgetNode(prototype.id(), prototype.type(), Map.of(
                new PropertyName("enabled"), new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("tonalIcon")), prototype.slots());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), iconPrototype))));
    }

    @Test
    void radioDenseAndWholeGenericReferencesPreserveOnlyPresenceWithoutProjectCodeOrResources() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema.variants()) {
            var fields = dev.flutter.netbeans.designer.catalog.RadioTestValues.full(variant);
            var node = dev.flutter.netbeans.designer.catalog.RadioTestValues.node(fields);
            var codec = new CanvasModelPayloadCodec();
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            String json = new String(codec.encode(candidate), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(candidate), codec.encode(candidate));
            for (var name : fields.keySet()) assertTrue(json.contains("\"" + name.value() + "\":"), name.toString());
            for (String name : List.of("onChanged", "groupRegistry", "focusNode", "mouseCursor")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            for (String absent : List.of("buttonValues", "package:buttons", "libraryUri", "resources")) assertFalse(json.contains(absent), absent);
            assertTrue(json.contains("\"protocolVersion\":19"));
        }
        var fields = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("valueType", "value", "groupValue", "fillColor", "overlayColor", "backgroundColor", "innerRadius", "side", "visualDensity")) fields.put(new PropertyName(name),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "ProjectValue", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        var root = dev.flutter.netbeans.designer.catalog.RadioTestValues.node(fields);
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
        for (var name : fields.keySet()) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertFalse(json.contains("ProjectValue"));
    }

    @Test
    void radioGroupPayloadRetainsRequiredChildAndOnlyPresenceOfGenericProjectReferences() throws Exception {
        var fields = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("valueType", "groupValue", "onChanged")) fields.put(new PropertyName(name),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_types/values.dart"),
                        "ProjectChoice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        fields.put(new PropertyName("nullableValueType"), new PropertyValue.BooleanValue(true));
        var root = radioGroupPayloadNode(fields);
        var codec = new CanvasModelPayloadCodec();
        var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
        String json = new String(codec.encode(candidate), StandardCharsets.UTF_8);
        assertArrayEquals(codec.encode(candidate), codec.encode(candidate));
        for (String name : List.of("valueType", "groupValue", "onChanged")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertTrue(json.contains("RadioGroup child retained"));
        assertTrue(json.contains("\"protocolVersion\":19"));
        for (String hidden : List.of("ProjectChoice", "radio_types", "libraryUri", "resources")) assertFalse(json.contains(hidden), hidden);
    }

    @Test
    void radioGroupPayloadDistinguishesOmittedNullAndAllLiteralIdentityKindsWithoutStoredDefaults() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        String omitted = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), radioGroupPayloadNode(Map.of())))), StandardCharsets.UTF_8);
        assertFalse(omitted.contains("\"groupValue\":"));
        assertFalse(omitted.contains("\"nullableValueType\":"));
        assertTrue(omitted.contains("\"onChanged\":{\"kind\":\"string\",\"value\":\"noop\"}"));
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("line1\r\nline2"),
                new PropertyValue.BooleanValue(true), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(9007199254740991L)),
                new PropertyValue.DoubleValue(new BigDecimal("1.5")), new PropertyValue.EnumValue("double", "infinity"),
                new PropertyValue.EnumValue("double", "negativeInfinity"), new PropertyValue.EnumValue("double", "nan"))) {
            var root = radioGroupPayloadNode(Map.of(new PropertyName("valueType"), new PropertyValue.StringValue("Object"), new PropertyName("groupValue"), value));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"groupValue\":{\"kind\":\"" + value.kind().wireName() + "\""));
        }
    }

    @Test
    void radioGroupInvalidRequiredValuesSlotsAndRawCodeFailBeforePayloadAdmission() {
        var root = radioGroupPayloadNode(Map.of());
        var codec = new CanvasModelPayloadCodec();
        for (String name : List.of("valueType", "onChanged")) {
            var values = new LinkedHashMap<>(root.properties());
            values.remove(new PropertyName(name));
            var invalid = new WidgetNode(root.id(), root.type(), values, root.slots());
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.DartExpressionValue("runProjectCode()"))) {
            var invalid = radioGroupPayloadNode(Map.of(new PropertyName("onChanged"), value));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), invalid))));
        }
        var missing = new WidgetNode(root.id(), root.type(), root.properties(), Map.of());
        var empty = new WidgetNode(root.id(), root.type(), root.properties(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        for (var invalid : List.of(missing, empty)) assertThrows(IllegalArgumentException.class,
                () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), invalid))));
    }

    private static WidgetNode radioGroupPayloadNode(Map<PropertyName, PropertyValue> fields) {
        var type = dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE;
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(), StableId.random());
        var values = new LinkedHashMap<>(prototype.properties());
        values.putAll(fields);
        return new WidgetNode(prototype.id(), type, values, Map.of(new SlotName("child"),
                new WidgetSlot.SingleSlot(Optional.of(text("2785bdda-2834-4d34-9cc3-ed6b530a8b10", "RadioGroup child retained")))));
    }

    @Test
    void radioSpecialNumbersLiteralIdentityAndNoopNullOmittedRemainDistinctWireValues() throws Exception {
        for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            var node = dev.flutter.netbeans.designer.catalog.RadioTestValues.node(Map.of(new PropertyName("valueType"), new PropertyValue.StringValue("double"),
                    new PropertyName("value"), new PropertyValue.EnumValue("double", member), new PropertyName("groupValue"), new PropertyValue.EnumValue("double", member),
                    new PropertyName("enabled"), new PropertyValue.NullValue(), new PropertyName("onChanged"), new PropertyValue.NullValue()));
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"type\":\"double\",\"value\":\"" + member + "\""));
            assertTrue(json.contains("\"onChanged\":{\"kind\":\"null\"}"));
            assertTrue(json.contains("\"enabled\":{\"kind\":\"null\"}"));
        }
        var created = dev.flutter.netbeans.designer.catalog.RadioTestValues.node(Map.of());
        String createdJson = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), created))), StandardCharsets.UTF_8);
        assertTrue(createdJson.contains("\"onChanged\":{\"kind\":\"string\",\"value\":\"noop\"}"));
        var omitted = dev.flutter.netbeans.designer.catalog.RadioTestValues.without(created, "onChanged");
        String omittedJson = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), omitted))), StandardCharsets.UTF_8);
        assertFalse(omittedJson.contains("\"onChanged\":"));
        assertFalse(omittedJson.contains("\"enabled\":"));
    }

    @Test
    void radioMalformedTypeValueWholeLocalAndRawCallbackBranchesFailBeforeEncoding() {
        for (var fields : List.of(Map.of(new PropertyName("value"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("valueType"), new PropertyValue.StringValue("int")),
                Map.of(new PropertyName("useCupertinoCheckmarkStyle"), new PropertyValue.BooleanValue(false)),
                Map.of(new PropertyName("sidePressedWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("fillColor"), dev.flutter.netbeans.designer.catalog.RadioTestValues.reference("whole"), new PropertyName("fillColorDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onChanged"), new PropertyValue.CallbackValue("legacy")))) {
            var node = dev.flutter.netbeans.designer.catalog.RadioTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void rangeSliderDenseLocalAndWholeFamiliesPreserveAll37FieldsWithoutProjectCodeOrResources() throws Exception {
        var fields = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.full();
        var node = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.node(fields);
        var codec = new CanvasModelPayloadCodec();
        var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), node));
        String json = new String(codec.encode(candidate), StandardCharsets.UTF_8);
        assertArrayEquals(codec.encode(candidate), codec.encode(candidate));
        for (var name : fields.keySet()) assertTrue(json.contains("\"" + name.value() + "\":"), name.toString());
        for (String name : List.of("onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback")) {
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), name);
        }
        for (String absent : List.of("buttonValues", "package:buttons", "libraryUri", "imageProvider", "resources")) assertFalse(json.contains(absent), absent);
        assertTrue(json.contains("\"protocolVersion\":19"));
        for (String family : List.of("labels", "overlayColor", "mouseCursor", "mouseCursorPressed")) {
            var whole = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.node(Map.of(new PropertyName(family),
                    dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.reference("projectValue")));
            String referenceJson = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), whole))), StandardCharsets.UTF_8);
            assertTrue(referenceJson.contains("\"" + family + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            assertFalse(referenceJson.contains("projectValue"));
        }
    }

    @Test
    void rangeSliderSignedInfinitiesExplicitNullAndEmptyLabelsRemainDifferentWireValues() throws Exception {
        for (boolean negative : List.of(false, true)) {
            var infinity = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.infinity(negative);
            var fields = new LinkedHashMap<PropertyName, PropertyValue>();
            for (String name : dev.flutter.netbeans.designer.catalog.RangeSliderWidgetPropertySchema.rangeProperties()) fields.put(new PropertyName(name), infinity);
            for (String name : List.of("labels", "divisions", "year2023", "mouseCursorPressed", "overlayColorPressed")) fields.put(new PropertyName(name), new PropertyValue.NullValue());
            var node = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.node(fields);
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"type\":\"double\",\"value\":\"" + (negative ? "negativeInfinity" : "infinity") + "\""));
            for (String name : List.of("labels", "divisions", "year2023", "mouseCursorPressed", "overlayColorPressed")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
        }
        var empty = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.node(Map.of(new PropertyName("labelsEnd"), new PropertyValue.StringValue("")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"labelsEnd\":{\"kind\":\"string\",\"value\":\"\"}"));
        assertFalse(json.contains("\"labels\":"));
        assertFalse(json.contains("\"labelsStart\":"));
    }

    @Test
    void rangeSliderMalformedRangesWholeLocalConflictsAndRawCallbacksFailBeforeEncoding() {
        for (var fields : List.of(Map.of(new PropertyName("valuesStart"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("min"), new PropertyValue.DoubleValue(BigDecimal.TEN)),
                Map.of(new PropertyName("valuesStart"), new PropertyValue.DoubleValue(BigDecimal.TEN)),
                Map.of(new PropertyName("labels"), new PropertyValue.NullValue(), new PropertyName("labelsStart"), new PropertyValue.StringValue("")),
                Map.of(new PropertyName("mouseCursor"), dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.reference("cursor"), new PropertyName("mouseCursorDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onChanged"), new PropertyValue.CallbackValue("legacy")))) {
            var node = dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void sliderBothConstructorsPreserveAllLocalFieldsAndOnlyReferencePresenceWithoutResources() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.SliderWidgetPropertySchema.variants()) {
            var fields = dev.flutter.netbeans.designer.catalog.SliderTestValues.full(variant);
            var node = dev.flutter.netbeans.designer.catalog.SliderTestValues.node(fields);
            var codec = new CanvasModelPayloadCodec();
            var request = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            String json = new String(codec.encode(request), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(request), codec.encode(request));
            for (var name : fields.keySet()) assertTrue(json.contains("\"" + name.value() + "\":"), name.toString());
            for (String name : List.of("onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "focusNode", "mouseCursor")) {
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), name);
            }
            for (String absent : List.of("buttonValues", "package:buttons", "libraryUri", "imageProvider", "resources")) assertFalse(json.contains(absent), absent);
            assertTrue(json.contains("\"protocolVersion\":19"));
            assertEquals(variant.equals("standard"), json.contains("\"padding\":"));
        }
    }

    @Test
    void sliderSignedInfinitiesExplicitNullsAndWholeOverlayRemainExactWireValues() throws Exception {
        for (boolean negative : List.of(false, true)) {
            var infinity = dev.flutter.netbeans.designer.catalog.SliderTestValues.infinity(negative);
            var node = dev.flutter.netbeans.designer.catalog.SliderTestValues.node(Map.of(
                    new PropertyName("min"), infinity, new PropertyName("max"), infinity, new PropertyName("value"), infinity,
                    new PropertyName("secondaryTrackValue"), new PropertyValue.NullValue(),
                    new PropertyName("divisions"), new PropertyValue.NullValue(), new PropertyName("year2023"), new PropertyValue.NullValue(),
                    new PropertyName("overlayColor"), dev.flutter.netbeans.designer.catalog.SliderTestValues.reference("overlay")));
            String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"type\":\"double\",\"value\":\"" + (negative ? "negativeInfinity" : "infinity") + "\""));
            for (String name : List.of("secondaryTrackValue", "divisions", "year2023")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
            assertTrue(json.contains("\"overlayColor\":{\"kind\":\"dartObjectReferencePresence\"}"));
        }
    }

    @Test
    void sliderMalformedRangesConstructorConflictsAndRawCallbacksFailBeforeEncoding() {
        for (var fields : List.of(Map.of(new PropertyName("value"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("min"), new PropertyValue.DoubleValue(BigDecimal.TEN)),
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"),
                        new PropertyName("padding"), dev.flutter.netbeans.designer.catalog.SliderTestValues.value("padding")),
                Map.of(new PropertyName("overlayColor"), dev.flutter.netbeans.designer.catalog.SliderTestValues.reference("overlay"),
                        new PropertyName("overlayColorDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onChanged"), new PropertyValue.CallbackValue("legacy")))) {
            var node = dev.flutter.netbeans.designer.catalog.SliderTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void switchDenseFamiliesPreserveImagesIconsAndOnlyClosedReferencePresence() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.SwitchWidgetPropertySchema.variants()) {
            var properties = dev.flutter.netbeans.designer.catalog.SwitchTestValues.full(variant);
            var node = dev.flutter.netbeans.designer.catalog.SwitchTestValues.node(properties);
            var resource = CanvasImageResource.create(CanvasImageFormat.PNG, 40, 30, new byte[]{4, 3, 2, 1});
            var assetId = CanvasImageAssetId.application("assets/switch.png");
            var bundle = new CanvasImageResourceBundle(List.of(new CanvasImageAsset(assetId, resource.resourceId(),
                    List.of(new CanvasImageVariant(BigDecimal.ONE, resource.resourceId())))), List.of(resource));
            var document = new DesignerDocument(DOCUMENT_ID, source(), node);
            assertThrows(CanvasModelPayloadException.class, () -> new CanvasModelPayloadCodec().encode(request(document)));
            String json = new String(new CanvasModelPayloadCodec().encode(request(PROFILE, document, bundle)), StandardCharsets.UTF_8);
            for (String name : List.of("onChanged", "onFocusChange", "focusNode", "mouseCursor", "onActiveThumbImageError", "onInactiveThumbImageError")) {
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), name);
            }
            for (String forbidden : List.of("buttonValues", "package:buttons", "libraryUri")) assertFalse(json.contains(forbidden));
            assertTrue(json.contains("\"thumbIconDefaultData\""));
            assertTrue(json.contains("\"activeThumbImage\""));
            assertTrue(json.contains("\"inactiveThumbImage\""));
            assertTrue(json.contains("\"protocolVersion\":19"));
        }
    }

    @Test
    void switchExplicitNullStatesAndIconNullRemainDifferentWireModels() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.SwitchTestValues.node(Map.of(
                new PropertyName("variant"), new PropertyValue.StringValue("adaptive"),
                new PropertyName("applyCupertinoTheme"), new PropertyValue.NullValue(),
                new PropertyName("thumbColorDisabled"), new PropertyValue.NullValue(),
                new PropertyName("trackOutlineWidthSelected"), new PropertyValue.NullValue(),
                new PropertyName("thumbIconDefaultMode"), new PropertyValue.StringValue("icon"),
                new PropertyName("thumbIconSelectedMode"), new PropertyValue.StringValue("inherit")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        for (String name : List.of("applyCupertinoTheme", "thumbColorDisabled", "trackOutlineWidthSelected")) {
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
        }
        assertTrue(json.contains("\"thumbIconDefaultMode\":{\"kind\":\"string\",\"value\":\"icon\"}"));
        assertTrue(json.contains("\"thumbIconSelectedMode\":{\"kind\":\"string\",\"value\":\"inherit\"}"));
        for (String absent : List.of("imageProvider", "MaterialIcons", "resources", "thumbIconDefaultData")) assertFalse(json.contains(absent), absent);
    }

    @Test
    void switchMalformedRelationsAndRawCallbacksFailBeforePayloadEncoding() {
        for (var properties : List.of(
                Map.of(new PropertyName("value"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("applyCupertinoTheme"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onChanged"), new PropertyValue.CallbackValue("legacy")),
                Map.of(new PropertyName("onActiveThumbImageError"), dev.flutter.netbeans.designer.catalog.SwitchTestValues.reference("error")),
                Map.of(new PropertyName("thumbColor"), dev.flutter.netbeans.designer.catalog.SwitchTestValues.reference("color"),
                        new PropertyName("thumbColorDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("thumbIconSelectedMode"), new PropertyValue.StringValue("inherit"),
                        new PropertyName("thumbIconSelectedSize"), new PropertyValue.DoubleValue(BigDecimal.ONE)))) {
            var node = dev.flutter.netbeans.designer.catalog.SwitchTestValues.node(new LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void checkboxFullFamiliesKeepEveryLocalPropertyAndOnlyClosedReferencePresence() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.CheckboxWidgetPropertySchema.variants()) {
            for (String shape : dev.flutter.netbeans.designer.catalog.CheckboxWidgetPropertySchema.shapeKinds()) {
                var properties = dev.flutter.netbeans.designer.catalog.CheckboxTestValues.full(variant, shape);
                var node = dev.flutter.netbeans.designer.catalog.CheckboxTestValues.node(properties);
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                for (String name : List.of("onChanged", "focusNode", "mouseCursor")) {
                    assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
                }
                for (String forbidden : List.of("buttonValues", "package:buttons", "imageProvider", "MaterialIcons", "libraryUri")) {
                    assertFalse(json.contains(forbidden), forbidden);
                }
                assertTrue(json.contains("\"protocolVersion\":19"));
            }
        }
    }

    @Test
    void checkboxNullValueStateColorsInfinityAndWholeReferencesRemainDistinctWireValues() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.CheckboxTestValues.node(Map.of(
                new PropertyName("value"), new PropertyValue.NullValue(), new PropertyName("tristate"), new PropertyValue.BooleanValue(true),
                new PropertyName("fillColorDisabled"), new PropertyValue.NullValue(),
                new PropertyName("splashRadius"), new PropertyValue.EnumValue("double", "infinity"),
                new PropertyName("side"), dev.flutter.netbeans.designer.catalog.CheckboxTestValues.reference("side"),
                new PropertyName("shape"), dev.flutter.netbeans.designer.catalog.CheckboxTestValues.reference("shape"),
                new PropertyName("overlayColor"), dev.flutter.netbeans.designer.catalog.CheckboxTestValues.reference("overlay")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        for (String name : List.of("value", "fillColorDisabled")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
        for (String name : List.of("side", "shape", "overlayColor")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertTrue(json.contains("\"splashRadius\":{\"kind\":\"enum\",\"type\":\"double\",\"value\":\"infinity\"}"));
        assertFalse(json.contains("buttonValues"));
    }

    @Test
    void checkboxInvalidRelationsAndForgedCallbackFormsFailBeforePayloadEncoding() {
        for (var properties : List.of(
                Map.of(new PropertyName("value"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("sideDisabledMode"), new PropertyValue.StringValue("border")),
                Map.of(new PropertyName("fillColor"), dev.flutter.netbeans.designer.catalog.CheckboxTestValues.reference("fill"),
                        new PropertyName("fillColorDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onChanged"), new PropertyValue.CallbackValue("legacy")),
                Map.of(new PropertyName("sideWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE.negate())))) {
            var node = dev.flutter.netbeans.designer.catalog.CheckboxTestValues.node(new LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void iconButtonDenseAllFourVariantsPreserveBothIconsAndOnlyTypedReferencePresence() throws Exception {
        for (String variant : dev.flutter.netbeans.designer.catalog.IconButtonWidgetPropertySchema.variants()) {
            for (boolean paints : List.of(false, true)) {
                var properties = dev.flutter.netbeans.designer.catalog.IconButtonTestValues.full(variant, paints, false);
                var base = dev.flutter.netbeans.designer.catalog.IconButtonTestValues.node(properties);
                var slots = new LinkedHashMap<>(base.slots());
                slots.put(new SlotName("selectedIcon"), WidgetSlot.SingleSlot.of(
                        dev.flutter.netbeans.designer.catalog.IconButtonTestValues.text("Preserved selected icon")));
                var node = new WidgetNode(base.id(), base.type(), base.properties(), slots);
                assertEquals(505, node.properties().size());
                String json = new String(new CanvasModelPayloadCodec().encode(request(
                        new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
                for (String name : List.of("onPressed", "onHover", "onLongPress", "focusNode",
                        "mouseCursor", "statesController", "styleBackgroundBuilder", "styleForegroundBuilder")) {
                    assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"), json);
                }
                assertTrue(json.contains("Preserved selected icon"));
                assertTrue(json.contains("\"icon\":{\"kind\":\"single\""));
                assertTrue(json.contains("\"selectedIcon\":{\"kind\":\"single\""));
                for (String forbidden : List.of("package:buttons", "buttonValues", "styles.dart", "libraryUri",
                        "MaterialIcons", "imageProvider")) assertFalse(json.contains(forbidden), forbidden);
            }
        }
    }

    @Test
    void iconButtonPayloadPreservesExplicitNullSelectionInfinityAndWholeStyleWithoutProtocolChanges() throws Exception {
        var node = dev.flutter.netbeans.designer.catalog.IconButtonTestValues.node(Map.of(
                new PropertyName("isSelected"), new PropertyValue.NullValue(),
                new PropertyName("iconSize"), new PropertyValue.EnumValue("double", "infinity"),
                new PropertyName("style"), dev.flutter.netbeans.designer.catalog.IconButtonTestValues.reference("wholeStyle")));
        String json = new String(new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), node))), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"protocolVersion\":19"));
        assertTrue(json.contains("\"isSelected\":{\"kind\":\"null\"}"));
        assertTrue(json.contains("\"iconSize\":{\"kind\":\"enum\",\"type\":\"double\",\"value\":\"infinity\"}"));
        assertTrue(json.contains("\"style\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertFalse(json.contains("wholeStyle"));
        assertFalse(json.contains("styleBackgroundColor"));
    }

    @Test
    void iconButtonPayloadRejectsMissingRequiredIconAndInvalidLocalOrReferenceDomainsBeforeEncoding() {
        var invalid = List.of(
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("icon")),
                Map.of(new PropertyName("splashRadius"), new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                Map.of(new PropertyName("style"), dev.flutter.netbeans.designer.catalog.IconButtonTestValues.reference("style"),
                        new PropertyName("styleIconColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(new PropertyName("onHover"), new PropertyValue.CallbackValue("legacy")));
        for (var properties : invalid) {
            var node = dev.flutter.netbeans.designer.catalog.IconButtonTestValues.node(new LinkedHashMap<>(properties));
            assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                    new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                dev.flutter.netbeans.designer.catalog.IconButtonTestValues.definition(), StableId.random());
        assertThrows(IllegalArgumentException.class, () -> new CanvasModelPayloadCodec().encode(request(
                new DesignerDocument(DOCUMENT_ID, source(), prototype))));
    }

    @Test
    void listTileAllCompoundFieldsAndFourSlotsRemainExactWhileProjectValuesArePresenceOnly() throws Exception {
        for (String shape : dev.flutter.netbeans.designer.catalog.ListTileWidgetPropertySchema.shapeKinds()) for (boolean paints : List.of(false, true)) {
            var root = dev.flutter.netbeans.designer.catalog.ListTileTestValues.fullNode(shape, paints);
            var codec = new CanvasModelPayloadCodec();
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
            String json = new String(codec.encode(candidate), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(candidate), codec.encode(candidate));
            for (var name : root.properties().keySet()) assertTrue(json.contains("\"" + name.value() + "\":"), name.toString());
            for (String name : List.of("leading", "title", "subtitle", "trailing")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"single\""));
            for (String name : List.of("onTap", "onLongPress", "onFocusChange", "focusNode", "statesController")) assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            for (String hidden : List.of("package:buttons", "buttonValues", "libraryUri", "resources")) assertFalse(json.contains(hidden), hidden);
            assertTrue(json.contains("\"protocolVersion\":19"));
        }
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("shape", "visualDensity", "titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle", "contentPadding", "iconColor", "textColor", "selectedColor", "mouseCursor")) {
            properties.put(new PropertyName(name), dev.flutter.netbeans.designer.catalog.ListTileTestValues.reference(name));
        }
        String json = new String(new CanvasModelPayloadCodec().encode(request(new DesignerDocument(DOCUMENT_ID, source(), dev.flutter.netbeans.designer.catalog.ListTileTestValues.node(properties)))), StandardCharsets.UTF_8);
        for (var name : properties.keySet()) assertTrue(json.contains("\"" + name.value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertFalse(json.contains("buttonValues"));
    }

    @Test
    void listTileEmptyCreationNullableCallbacksAndSignedConstantsHaveNoInventedFields() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var empty = dev.flutter.netbeans.designer.catalog.ListTileTestValues.node(Map.of());
        String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertFalse(json.contains("\"onTap\":"));
        assertFalse(json.contains("\"title\":"));
        for (String name : dev.flutter.netbeans.designer.catalog.ListTileWidgetPropertySchema.geometryProperties()) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.EnumValue("double", "infinity"), new PropertyValue.EnumValue("double", "negativeInfinity"), new PropertyValue.EnumValue("double", "nan"))) {
                var root = dev.flutter.netbeans.designer.catalog.ListTileTestValues.node(Map.of(new PropertyName(name), value, new PropertyName("onTap"), new PropertyValue.NullValue(), new PropertyName("enabled"), new PropertyValue.BooleanValue(false)));
                String encoded = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
                assertTrue(encoded.contains("\"" + name + "\":{\"kind\":\"" + value.kind().wireName() + "\""));
                assertTrue(encoded.contains("\"onTap\":{\"kind\":\"null\"}"));
            }
        }
    }

    @Test
    void listTileInvalidThreeLineStateDefaultsCompoundConflictsAndRawCodeRejectBeforePayloadAdmission() {
        var codec = new CanvasModelPayloadCodec();
        var invalid = List.of(Map.of(new PropertyName("isThreeLine"), new PropertyValue.BooleanValue(true)),
                Map.of(new PropertyName("iconColorDisabled"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(new PropertyName("mouseCursorDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onTap"), new PropertyValue.DartExpressionValue("unsafe()")),
                Map.of(new PropertyName("contentPadding"), new PropertyValue.DartExpressionValue("EdgeInsets.zero")),
                Map.of(new PropertyName("shape"), dev.flutter.netbeans.designer.catalog.ListTileTestValues.reference("shape"), new PropertyName("shapeKind"), new PropertyValue.StringValue("circle")));
        for (var fields : invalid) {
            var root = dev.flutter.netbeans.designer.catalog.ListTileTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void switchListTileAllFieldsThreeSlotsBothConstructorsAndShapesRemainExactAndAnonymous() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        var resource = CanvasImageResource.create(CanvasImageFormat.PNG, 40, 30, new byte[]{4, 3, 2, 1});
        var assetId = CanvasImageAssetId.application("assets/switch.png");
        var bundle = new CanvasImageResourceBundle(List.of(new CanvasImageAsset(assetId, resource.resourceId(),
                List.of(new CanvasImageVariant(BigDecimal.ONE, resource.resourceId())))), List.of(resource));
        for (String variant : List.of("standard", "adaptive")) {
            for (String shape : dev.flutter.netbeans.designer.catalog.SwitchListTileWidgetPropertySchema.shapeKinds()) {
                var root = dev.flutter.netbeans.designer.catalog.SwitchListTileTestValues.fullNode(variant, shape);
                var document = new DesignerDocument(DOCUMENT_ID, source(), root);
                assertThrows(CanvasModelPayloadException.class, () -> codec.encode(request(document)));
                var candidate = request(PROFILE, document, bundle);
                byte[] encoded = codec.encode(candidate);
                assertArrayEquals(encoded, codec.encode(candidate));
                String json = new String(encoded, StandardCharsets.UTF_8);
                for (var name : root.properties().keySet()) {
                    seen.add(name);
                    assertTrue(json.contains("\"" + name.value() + "\":"), name.toString());
                }
                for (String slot : List.of("title", "subtitle", "secondary")) assertTrue(json.contains("\"" + slot + "\":{\"kind\":\"single\""));
                assertTrue(json.contains("\"applyCupertinoTheme\":"), "Inactive Standard intent remains in model payload");
                assertFalse(json.contains("buttonValues"));
                assertFalse(json.contains("libraryUri"));
                assertTrue(json.contains("\"protocolVersion\":19"));
                assertTrue(json.contains(resource.resourceId()));
            }
        }
        var refs = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("shape", "visualDensity", "mouseCursor", "thumbColor", "trackColor", "trackOutlineColor", "overlayColor", "thumbIcon")) {
            refs.put(new PropertyName(name), dev.flutter.netbeans.designer.catalog.ListTileTestValues.reference(name));
        }
        var root = dev.flutter.netbeans.designer.catalog.SwitchListTileTestValues.node(refs);
        String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
        for (var name : root.properties().keySet()) seen.add(name);
        for (var name : refs.keySet()) assertTrue(json.contains("\"" + name.value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertEquals(dev.flutter.netbeans.designer.catalog.SwitchListTileTestValues.definition().properties().stream()
                .map(dev.flutter.netbeans.designer.catalog.PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), seen);
        assertFalse(json.contains("buttonValues"));
    }

    @Test
    void switchListTileNullableCallbacksAndImageGuardsRejectOnlyInvalidCombinations() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var nulls = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError")) nulls.put(new PropertyName(name), new PropertyValue.NullValue());
        String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(),
                dev.flutter.netbeans.designer.catalog.SwitchListTileTestValues.node(nulls)))), StandardCharsets.UTF_8);
        for (var name : nulls.keySet()) assertTrue(json.contains("\"" + name.value() + "\":{\"kind\":\"null\"}"));
        for (var fields : List.of(
                Map.of(new PropertyName("value"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(false)),
                Map.of(new PropertyName("isThreeLine"), new PropertyValue.BooleanValue(true)),
                Map.of(new PropertyName("onActiveThumbImageError"), new PropertyValue.StringValue("noop")),
                Map.of(new PropertyName("onInactiveThumbImageError"), dev.flutter.netbeans.designer.catalog.ListTileTestValues.reference("error")),
                Map.of(new PropertyName("mouseCursorHovered"), new PropertyValue.StringValue("click")),
                Map.of(new PropertyName("onChanged"), new PropertyValue.DartExpressionValue("runProject()")))) {
            var root = dev.flutter.netbeans.designer.catalog.SwitchListTileTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void radioListTileEveryLeafBothConstructorsThreeSlotsAndShapesHaveAnonymousExactPayloads() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (String variant : List.of("standard", "adaptive")) for (String shape : dev.flutter.netbeans.designer.catalog.RadioListTileWidgetPropertySchema.shapeKinds()) {
            var root = dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.fullNode(variant, shape);
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
            String json = new String(codec.encode(candidate), StandardCharsets.UTF_8);
            assertArrayEquals(codec.encode(candidate), codec.encode(candidate));
            for (var name : root.properties().keySet()) { seen.add(name); assertTrue(json.contains("\"" + name.value() + "\":")); }
            for (String slot : List.of("title", "subtitle", "secondary")) assertTrue(json.contains("\"" + slot + "\":{\"kind\":\"single\""));
            assertTrue(json.contains("\"useCupertinoCheckmarkStyle\":"));
            assertFalse(json.contains("buttonValues")); assertFalse(json.contains("libraryUri"));
            assertTrue(json.contains("\"protocolVersion\":19"));
        }
        var fields = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius", "radioSide", "visualDensity", "shape", "mouseCursor")) {
            fields.put(new PropertyName(name), dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.reference(name));
        }
        var root = dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.node(fields);
        String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
        for (var name : root.properties().keySet()) seen.add(name);
        for (var name : fields.keySet()) assertTrue(json.contains("\"" + name.value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        assertEquals(dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.definition().properties().stream()
                .map(dev.flutter.netbeans.designer.catalog.PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), seen);
    }

    @Test
    void radioListTileOptionalCallbackResetNullAndScalarGenericGuardsRemainExact() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        for (var callback : List.<PropertyValue>of(new PropertyValue.NullValue(), new PropertyValue.StringValue("noop"),
                dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.reference("callback"))) {
            var root = dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.node(Map.of(new PropertyName("onChanged"), callback));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"onChanged\":{\"kind\":\"" + (callback.kind() == dev.flutter.netbeans.designer.model.PropertyValueKind.DART_OBJECT_REFERENCE ? "dartObjectReferencePresence" : callback.kind().wireName()) + "\""));
        }
        var reset = dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.without(dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.node(Map.of()), "onChanged");
        String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), reset))), StandardCharsets.UTF_8);
        assertFalse(json.contains("\"onChanged\":"));
        for (var fields : List.of(Map.of(new PropertyName("value"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("valueType"), new PropertyValue.StringValue("int")),
                Map.of(new PropertyName("isThreeLine"), new PropertyValue.BooleanValue(true)),
                Map.of(new PropertyName("groupRegistry"), dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.reference("registry")),
                Map.of(new PropertyName("radioSidePressedWidth"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("mouseCursorHovered"), new PropertyValue.StringValue("click")))) {
            var root = dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void expansionTileAll76LeavesBothShapeFamiliesAndFiveSlotsHaveExactAnonymousPayloads() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (String shape : dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.shapeKinds()) {
            for (String collapsed : dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.shapeKinds()) {
                var root = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.fullNode(shape, collapsed);
                var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
                byte[] encoded = codec.encode(candidate);
                assertArrayEquals(encoded, codec.encode(candidate));
                String json = new String(encoded, StandardCharsets.UTF_8);
                for (var name : root.properties().keySet()) {
                    seen.add(name);
                    assertTrue(json.contains("\"" + name.value() + "\":"), name.toString());
                }
                for (String slot : List.of("title", "leading", "subtitle", "trailing")) {
                    assertTrue(json.contains("\"" + slot + "\":{\"kind\":\"single\""));
                }
                assertTrue(json.contains("\"children\":{\"kind\":\"list\""));
                assertTrue(json.contains("First")); assertTrue(json.contains("Second"));
                assertTrue(json.contains("\"protocolVersion\":19"));
                assertFalse(json.contains("buttonValues")); assertFalse(json.contains("libraryUri"));
            }
        }
        for (var property : dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.definition().properties()) {
            if (!property.acceptedKinds().contains(dev.flutter.netbeans.designer.model.PropertyValueKind.DART_OBJECT_REFERENCE)) continue;
            var name = property.name();
            var root = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.node(Map.of(name,
                    dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.reference("privateProjectValue")));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            seen.add(name);
            assertTrue(json.contains("\"" + name.value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            for (String forbidden : List.of("privateProjectValue", "buttonValues", "libraryUri", "package:buttons")) assertFalse(json.contains(forbidden));
        }
        assertEquals(76, seen.size());
        assertEquals(dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.definition().properties().stream()
                .map(dev.flutter.netbeans.designer.catalog.PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), seen);
    }

    @Test
    void expansionTileNullableArgumentsSignedMicrosecondsAndAllCurvesRemainDistinctFromOmission() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var empty = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.node(Map.of());
        String omitted = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertFalse(omitted.contains("\"initiallyExpanded\":")); assertFalse(omitted.contains("\"onExpansionChanged\":"));
        for (var property : dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.definition().properties()) {
            if (!property.acceptedKinds().contains(dev.flutter.netbeans.designer.model.PropertyValueKind.NULL)) continue;
            // Shape detail nulls still require their explicitly selected local family.
            if (dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.isShapeDetailProperty(property.name().value())) continue;
            var root = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.node(Map.of(property.name(), new PropertyValue.NullValue()));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"null\"}"));
        }
        for (String name : dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.animationDurationProperties()) {
            var root = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.node(Map.of(new PropertyName(name), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-123456))));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"integer\",\"value\":-123456}"));
        }
        for (String name : dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.animationCurveProperties()) {
            for (String curve : dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.curvePresets()) {
                var root = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.node(Map.of(new PropertyName(name), new PropertyValue.StringValue(curve)));
                String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"string\",\"value\":\"" + curve + "\"}"));
            }
        }
    }

    @Test
    void expansionTileRequiredTitleAndUnsafeTypedDomainOrCompoundConflictsRejectBeforeEncoding() {
        var codec = new CanvasModelPayloadCodec();
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.definition(), StableId.random());
        assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), prototype))));
        for (var fields : List.of(
                Map.of(new PropertyName("initiallyExpanded"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("expandedCrossAxisAlignment"), new PropertyValue.EnumValue("CrossAxisAlignment", "baseline")),
                Map.of(new PropertyName("onExpansionChanged"), new PropertyValue.CallbackValue("legacy")),
                Map.of(new PropertyName("controller"), new PropertyValue.DartExpressionValue("executeProject()")),
                Map.of(new PropertyName("shape"), new PropertyValue.NullValue(), new PropertyName("shapeKind"), new PropertyValue.StringValue("circle")),
                Map.of(new PropertyName("visualDensity"), new PropertyValue.NullValue(), new PropertyName("visualDensityHorizontal"), new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                Map.of(new PropertyName("expansionAnimationStyle"), new PropertyValue.NullValue(), new PropertyName("expansionAnimationStyleCurve"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("expansionAnimationStyleDurationUs"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("expansionAnimationStyleCurve"), new PropertyValue.StringValue("projectCurve")))) {
            var root = dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void tooltipAll53LeavesPlainAndRichContentAndOptionalChildHaveExactAnonymousPayloads() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (boolean rich : List.of(false, true)) for (boolean paints : List.of(false, true)) {
            var base = dev.flutter.netbeans.designer.catalog.TooltipTestValues.node(dev.flutter.netbeans.designer.catalog.TooltipTestValues.full(rich, paints));
            var root = new WidgetNode(base.id(), base.type(), base.properties(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(
                    dev.flutter.netbeans.designer.catalog.ListTileTestValues.text("Actual anchor"))));
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
            byte[] encoded = codec.encode(candidate);
            assertArrayEquals(encoded, codec.encode(candidate));
            String json = new String(encoded, StandardCharsets.UTF_8);
            for (var name : root.properties().keySet()) { seen.add(name); assertTrue(json.contains("\"" + name.value() + "\":")); }
            assertTrue(json.contains("Actual anchor")); assertTrue(json.contains("\"protocolVersion\":19"));
            for (String forbidden : List.of("libraryUri", "buttonValues", "package:buttons")) assertFalse(json.contains(forbidden));
        }
        for (var property : dev.flutter.netbeans.designer.catalog.TooltipTestValues.definition().properties()) {
            PropertyValue value = property.acceptedKinds().contains(dev.flutter.netbeans.designer.model.PropertyValueKind.DART_OBJECT_REFERENCE)
                    ? dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("privateProjectValue")
                    : property.name().value().equals("height") ? dev.flutter.netbeans.designer.catalog.TooltipTestValues.d("24") : null;
            if (value == null) continue;
            var fields = new LinkedHashMap<PropertyName, PropertyValue>();
            if (!property.name().value().equals("richMessage")) fields.put(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"));
            fields.put(property.name(), value);
            var root = dev.flutter.netbeans.designer.catalog.TooltipTestValues.node(fields);
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            seen.add(property.name());
            assertFalse(json.contains("privateProjectValue")); assertFalse(json.contains("libraryUri"));
            if (value instanceof PropertyValue.DartObjectReferenceValue) assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        }
        assertEquals(53, seen.size());
        assertEquals(dev.flutter.netbeans.designer.catalog.TooltipTestValues.definition().properties().stream().map(dev.flutter.netbeans.designer.catalog.PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), seen);
    }

    @Test
    void tooltipNullsSignedDurationsAndMessageSwitchRemainDistinctWithoutFabricatedChild() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        for (var property : dev.flutter.netbeans.designer.catalog.TooltipTestValues.definition().properties()) {
            if (!property.acceptedKinds().contains(dev.flutter.netbeans.designer.model.PropertyValueKind.NULL)) continue;
            var fields = new LinkedHashMap<PropertyName, PropertyValue>();
            fields.put(new PropertyName(property.name().value().equals("message") ? "richMessage" : "message"),
                    property.name().value().equals("message") ? dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("rich") : new PropertyValue.StringValue(""));
            fields.put(property.name(), new PropertyValue.NullValue());
            var root = dev.flutter.netbeans.designer.catalog.TooltipTestValues.node(fields);
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"null\"}"));
            assertTrue(json.contains("\"child\":{\"kind\":\"single\",\"child\":null}"));
        }
        for (String name : dev.flutter.netbeans.designer.catalog.TooltipWidgetPropertySchema.durationProperties()) {
            var root = dev.flutter.netbeans.designer.catalog.TooltipTestValues.with(Map.of(new PropertyName(name), dev.flutter.netbeans.designer.catalog.TooltipTestValues.i(-123456)));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"integer\",\"value\":-123456}"));
        }
    }

    @Test
    void tooltipInvalidContentCompoundAndUnsafeReferenceFormsRejectBeforePayloadAdmission() {
        var codec = new CanvasModelPayloadCodec();
        for (var fields : List.of(
                Map.<PropertyName, PropertyValue>of(),
                Map.of(new PropertyName("message"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue(""), new PropertyName("richMessage"), dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("rich")),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"), new PropertyName("enableTapToDismiss"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"), new PropertyName("height"), dev.flutter.netbeans.designer.catalog.TooltipTestValues.i(24), new PropertyName("constraints"), dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("constraints")),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"), new PropertyName("textStyle"), new PropertyValue.NullValue(), new PropertyName("textStyleInherit"), new PropertyValue.BooleanValue(true)),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"), new PropertyName("waitDurationUs"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"), new PropertyName("positionDelegate"), new PropertyValue.CallbackValue("position")),
                Map.of(new PropertyName("message"), new PropertyValue.StringValue("Tooltip"), new PropertyName("onTriggered"), new PropertyValue.DartExpressionValue("executeProject()")))) {
            var root = dev.flutter.netbeans.designer.catalog.TooltipTestValues.node(new LinkedHashMap<>(fields));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void tooltipVisibilityBothBooleanValuesPreserveRequiredChildIdentityAndExactWire() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var child = dev.flutter.netbeans.designer.catalog.ListTileTestValues.text("Visible anchor semantics");
        var nodeId = id("793560e4-67ae-4e99-907b-000000000801");
        for (boolean visible : List.of(true, false)) {
            var node = new WidgetNode(nodeId, type("flutter.material.TooltipVisibility"),
                    Map.of(new PropertyName("visible"), new PropertyValue.BooleanValue(visible)),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), node));
            byte[] bytes = codec.encode(candidate);
            assertArrayEquals(bytes, codec.encode(candidate));
            String json = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"visible\":{\"kind\":\"boolean\",\"value\":" + visible + "}"));
            assertTrue(json.contains(nodeId.toString()));
            assertTrue(json.contains(child.id().toString()));
            assertTrue(json.contains("Visible anchor semantics"));
            assertTrue(json.contains("\"protocolVersion\":19"));
            assertEquals(Map.of(new PropertyName("visible"), new PropertyValue.BooleanValue(visible)), node.properties());
            assertEquals(child, ((WidgetSlot.SingleSlot) node.slots().get(new SlotName("child"))).child().orElseThrow());
        }
    }

    @Test
    void tooltipVisibilityRejectsAbsentNullOrNonBooleanVisibleAndAbsentOrEmptyRequiredChild() {
        var codec = new CanvasModelPayloadCodec();
        var child = dev.flutter.netbeans.designer.catalog.ListTileTestValues.text("Actual child");
        var nodeId = id("793560e4-67ae-4e99-907b-000000000802");
        for (var properties : List.of(
                Map.<PropertyName, PropertyValue>of(),
                Map.of(new PropertyName("visible"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("visible"), new PropertyValue.StringValue("false")),
                Map.of(new PropertyName("visible"), new PropertyValue.IntegerValue(BigInteger.ONE)),
                Map.of(new PropertyName("visible"), new PropertyValue.CallbackValue("callback")),
                Map.of(new PropertyName("visible"), dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("projectVisible")))) {
            var node = new WidgetNode(nodeId, type("flutter.material.TooltipVisibility"), new LinkedHashMap<>(properties),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
        for (var slots : List.of(Map.<SlotName, WidgetSlot>of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))) {
            var node = new WidgetNode(nodeId, type("flutter.material.TooltipVisibility"),
                    Map.of(new PropertyName("visible"), new PropertyValue.BooleanValue(true)), new LinkedHashMap<>(slots));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), node))));
        }
    }

    @Test
    void tooltipThemeAll47FieldsHaveExactAnonymousPayloadAndRequiredChildIdentity() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (boolean paints : List.of(false, true)) {
            var root = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.node(
                    dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.full(paints));
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
            byte[] bytes = codec.encode(candidate);
            assertArrayEquals(bytes, codec.encode(candidate));
            String json = new String(bytes, StandardCharsets.UTF_8);
            for (var name : root.properties().keySet()) {
                seen.add(name); assertTrue(json.contains("\"" + name.value() + "\":"));
            }
            assertTrue(json.contains(root.id().toString()));
            assertTrue(json.contains("Theme anchor"));
            assertTrue(json.contains("\"protocolVersion\":19"));
        }
        for (var property : dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.definition().properties()) {
            PropertyValue value = property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)
                    ? dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("privateThemeFactory")
                    : property.name().value().equals("height") ? dev.flutter.netbeans.designer.catalog.TooltipTestValues.d("27") : null;
            if (value == null) continue;
            var root = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.node(Map.of(property.name(), value));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            seen.add(property.name());
            assertFalse(json.contains("privateThemeFactory")); assertFalse(json.contains("libraryUri"));
            if (value instanceof PropertyValue.DartObjectReferenceValue)
                assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
        }
        assertEquals(47, seen.size());
    }

    @Test
    void tooltipThemeEmptyAndNullableDataFieldsRemainDistinctAndCompoundConflictsFailClosed() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var empty = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.node(Map.of());
        String emptyJson = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), empty))), StandardCharsets.UTF_8);
        assertFalse(emptyJson.contains("\"data\":{\"kind\":\"dartObjectReferencePresence\"}"));
        for (String name : dev.flutter.netbeans.designer.catalog.TooltipThemeWidgetPropertySchema.localProperties()) {
            var definition = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.definition().properties().stream()
                    .filter(property -> property.name().value().equals(name)).findFirst().orElseThrow();
            if (definition.acceptedKinds().contains(PropertyValueKind.NULL)) {
                var root = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.node(Map.of(new PropertyName(name), new PropertyValue.NullValue()));
                String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
            }
            var conflict = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.node(Map.of(
                    new PropertyName("data"), dev.flutter.netbeans.designer.catalog.TooltipTestValues.reference("themeData"),
                    new PropertyName(name), dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.value(name)));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), conflict))));
        }
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("themeData"),
                new PropertyValue.CallbackValue("themeData"), new PropertyValue.DartExpressionValue("createTheme()"))) {
            var root = dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.node(Map.of(new PropertyName("data"), value));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
        for (var slots : List.of(Map.<SlotName, WidgetSlot>of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))) {
            var root = new WidgetNode(empty.id(), empty.type(), Map.of(), new LinkedHashMap<>(slots));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void menuItemButtonAll520FieldsAndAllThreeActualChildrenHaveExactAnonymousPayload() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (boolean paints : List.of(false, true)) for (boolean circles : List.of(false, true)) {
            var base = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(
                    dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.full(paints, circles));
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            for (String name : List.of("child", "leadingIcon", "trailingIcon")) slots.put(new SlotName(name),
                    WidgetSlot.SingleSlot.of(dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.text("Actual " + name)));
            var root = new WidgetNode(base.id(), base.type(), base.properties(), slots);
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
            byte[] bytes = codec.encode(candidate);
            assertArrayEquals(bytes, codec.encode(candidate));
            String json = new String(bytes, StandardCharsets.UTF_8);
            seen.addAll(root.properties().keySet());
            for (var name : root.properties().keySet()) assertTrue(json.contains("\"" + name.value() + "\":"));
            for (String name : List.of("child", "leadingIcon", "trailingIcon")) assertTrue(json.contains("Actual " + name));
            assertTrue(json.contains(root.id().toString()));
            assertFalse(json.contains("package:buttons/styles.dart")); assertFalse(json.contains("buttonValues"));
            assertFalse(json.contains("libraryUri")); assertTrue(json.contains("\"protocolVersion\":19"));
        }
        var definition = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.definition();
        for (var property : definition.properties()) {
            if (seen.contains(property.name())) continue;
            String name = property.name().value();
            PropertyValue value;
            if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) {
                value = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.reference("privateMenuFactory");
            } else if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) {
                value = new PropertyValue.BooleanValue(true);
            } else if (name.equals("semanticsLabel") || name.equals("shortcutCharacter")) {
                value = new PropertyValue.StringValue("A");
            } else {
                var constraint = property.constraints().stream().filter(c -> c instanceof dev.flutter.netbeans.designer.catalog.PropertyValueConstraint.EnumValues)
                        .map(c -> (dev.flutter.netbeans.designer.catalog.PropertyValueConstraint.EnumValues) c).findFirst().orElseThrow();
                value = new PropertyValue.EnumValue(constraint.dartType().name(), constraint.values().getLast());
            }
            var values = new LinkedHashMap<PropertyName, PropertyValue>(); values.put(property.name(), value);
            if (name.startsWith("shortcut") && !name.equals("shortcut") && !name.equals("shortcutCharacter") && !name.equals("shortcutTrigger"))
                values.put(new PropertyName("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", "keyA"));
            var root = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(values);
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + name + "\":")); assertFalse(json.contains("privateMenuFactory"));
            if (value instanceof PropertyValue.DartObjectReferenceValue)
                assertTrue(json.contains("\"" + name + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            seen.add(property.name());
        }
        assertEquals(520, seen.size());
    }

    @Test
    void menuItemButton432KeysUnicodeCharactersExplicitNullsAndOptionalSlotsRemainExact() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        for (String key : dev.flutter.netbeans.designer.catalog.MenuShortcutKeyCatalog.names()) {
            var root = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(Map.of(
                    new PropertyName("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", key)));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"type\":\"LogicalKeyboardKey\",\"value\":\"" + key + "\""));
        }
        for (String character : List.of("", "?", "Ж", "ab", "🙂")) {
            var root = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(Map.of(new PropertyName("shortcutCharacter"), new PropertyValue.StringValue(character)));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            boolean found = false;
            try (var parser = new com.fasterxml.jackson.core.JsonFactory().createParser(json)) {
                while (parser.nextToken() != null) {
                    if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.FIELD_NAME && "shortcutCharacter".equals(parser.currentName())) {
                        assertEquals(com.fasterxml.jackson.core.JsonToken.START_OBJECT, parser.nextToken());
                        assertEquals("kind", parser.nextFieldName()); assertEquals("string", parser.nextTextValue());
                        assertEquals("value", parser.nextFieldName()); assertEquals(character, parser.nextTextValue());
                        found = true; break;
                    }
                }
            }
            assertTrue(found);
        }
        for (String name : List.of("onHover", "onFocusChange", "focusNode", "statesController", "style", "shortcut", "semanticsLabel")) {
            var root = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(Map.of(new PropertyName(name), new PropertyValue.NullValue()));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
        }
        for (int mask = 0; mask < 8; mask++) for (boolean explicitEmpty : List.of(false, true)) {
            var base = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(Map.of());
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            var names = List.of("child", "leadingIcon", "trailingIcon");
            for (int bit = 0; bit < 3; bit++) {
                if ((mask & (1 << bit)) != 0)
                    slots.put(new SlotName(names.get(bit)), WidgetSlot.SingleSlot.of(dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.text(names.get(bit))));
                else if (explicitEmpty) slots.put(new SlotName(names.get(bit)), WidgetSlot.SingleSlot.empty());
            }
            var root = new WidgetNode(base.id(), base.type(), base.properties(), slots);
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            for (int bit = 0; bit < 3; bit++) {
                assertEquals(explicitEmpty || (mask & (1 << bit)) != 0,
                        json.contains("\"" + names.get(bit) + "\":{\"kind\":\"single\""), "slot presence: " + names.get(bit));
                assertEquals(explicitEmpty && (mask & (1 << bit)) == 0,
                        json.contains("\"" + names.get(bit) + "\":{\"kind\":\"single\",\"child\":null}"), "empty slot: " + names.get(bit));
            }
        }
    }

    @Test
    void menuItemButtonInvalidShortcutCompoundsKindsAndRequiredActivationFailClosed() {
        var codec = new CanvasModelPayloadCodec();
        var key = new PropertyValue.EnumValue("LogicalKeyboardKey", "keyA");
        for (var values : List.of(
                Map.of(new PropertyName("shortcutTrigger"), key, new PropertyName("shortcutCharacter"), new PropertyValue.StringValue("A")),
                Map.of(new PropertyName("shortcutShift"), new PropertyValue.BooleanValue(false)),
                Map.of(new PropertyName("shortcutCharacter"), new PropertyValue.StringValue("A"), new PropertyName("shortcutShift"), new PropertyValue.BooleanValue(false)),
                Map.of(new PropertyName("shortcutCharacter"), new PropertyValue.StringValue("A"), new PropertyName("shortcutNumLock"), new PropertyValue.EnumValue("LockState", "ignored")),
                Map.of(new PropertyName("shortcut"), new PropertyValue.NullValue(), new PropertyName("shortcutTrigger"), key),
                Map.of(new PropertyName("style"), new PropertyValue.NullValue(), new PropertyName("styleEnableFeedback"), new PropertyValue.BooleanValue(false)),
                Map.of(new PropertyName("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", "control")),
                Map.of(new PropertyName("enabled"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onPressed"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onPressed"), new PropertyValue.CallbackValue("pressed")),
                Map.of(new PropertyName("shortcut"), new PropertyValue.DartExpressionValue("createShortcut()")))) {
            var root = dev.flutter.netbeans.designer.catalog.MenuItemButtonTestValues.node(new LinkedHashMap<>(values));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    @Test
    void menuAnchorAll219PropertiesBothSlotsAndAnonymousProjectReferencesRoundTripExactly() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (boolean circles : List.of(false, true)) {
            var props = dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.full(circles);
            var base = dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.node(props);
            var root = new WidgetNode(base.id(), base.type(), base.properties(), Map.of(
                    new SlotName("child"), WidgetSlot.SingleSlot.of(dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.text("Anchor child")),
                    new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of(dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.text("Actual menu child")))));
            var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
            byte[] encoded = codec.encode(candidate);
            assertArrayEquals(encoded, codec.encode(candidate));
            String json = new String(encoded, StandardCharsets.UTF_8);
            for (var name : props.keySet()) assertTrue(json.contains("\"" + name.value() + "\":"));
            assertTrue(json.contains("Anchor child")); assertTrue(json.contains("Actual menu child"));
            seen.addAll(props.keySet());
        }
        for (var property : dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.definition().properties()) {
            if (seen.contains(property.name())) continue;
            PropertyValue value = property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)
                    ? dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.reference("privateMenuAnchorFactory")
                    : property.acceptedKinds().contains(PropertyValueKind.BOOLEAN) ? new PropertyValue.BooleanValue(true)
                    : new PropertyValue.EnumValue("Clip", "hardEdge");
            var root = dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.node(Map.of(property.name(), value));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + property.name().value() + "\":"));
            assertFalse(json.contains("privateMenuAnchorFactory")); assertFalse(json.contains("libraryUri"));
            if (value instanceof PropertyValue.DartObjectReferenceValue)
                assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            seen.add(property.name());
        }
        assertEquals(219, seen.size());
    }

    @Test
    void menuAnchorNullsRequiredEmptyListAndOptionalChildStayDistinct() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        for (String name : List.of("controller", "childFocusNode", "style", "alignmentOffset", "reservedPadding", "layerLink", "onOpen", "onClose", "onAnimationStatusChanged", "builder")) {
            var root = dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.node(Map.of(new PropertyName(name), new PropertyValue.NullValue()));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + name + "\":{\"kind\":\"null\"}"));
            assertTrue(json.contains("\"menuChildren\":{\"kind\":\"list\",\"children\":[]}"));
            assertFalse(json.contains("\"child\":{\"kind\":\"single\""));
        }
    }

    @Test
    void menuAnchorInvalidWholeLocalStyleRawCallbacksAndRequiredSlotsFailClosed() {
        var codec = new CanvasModelPayloadCodec();
        for (var values : List.of(
                Map.of(new PropertyName("style"), new PropertyValue.NullValue(), new PropertyName("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("animated"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("onOpen"), new PropertyValue.CallbackValue("openMenu")),
                Map.of(new PropertyName("builder"), new PropertyValue.DartExpressionValue("openMenu()")),
                Map.of(new PropertyName("styleAlignmentX"), new PropertyValue.DoubleValue(BigDecimal.ZERO)))) {
            var root = dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.node(new LinkedHashMap<>(values));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
        var base = dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.node(Map.of());
        var missing = new WidgetNode(base.id(), base.type(), base.properties(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), missing))));
    }

    @Test
    void submenuButtonAll721FieldsDenseDualStylesFourSlotsAndAnonymousReferencesRemainExact() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        var seen = new java.util.HashSet<PropertyName>();
        for (boolean circles : List.of(false, true)) {
            for (boolean paints : List.of(false, true)) {
                var props = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.full(circles);
                if (paints) {
                    for (String prefix : dev.flutter.netbeans.designer.catalog.SubmenuButtonWidgetPropertySchema.statePrefixes()) {
                        props.remove(new PropertyName(prefix + "TextBackgroundColor"));
                        props.put(new PropertyName(prefix + "TextBackground"), dev.flutter.netbeans.designer.catalog.TextButtonTestValues.value(prefix + "TextBackground"));
                    }
                }
                assertTrue(props.size() > 512, "legal combined style node exceeds the obsolete property cap");
                var base = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.node(props);
                var root = new WidgetNode(base.id(), base.type(), props, Map.of(
                        new SlotName("child"), WidgetSlot.SingleSlot.of(dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.text("Actual submenu label")),
                        new SlotName("leadingIcon"), WidgetSlot.SingleSlot.of(dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.text("Actual leading")),
                        new SlotName("trailingIcon"), WidgetSlot.SingleSlot.of(dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.text("Actual trailing")),
                        new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of(dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.text("Actual menu child")))));
                var candidate = request(new DesignerDocument(DOCUMENT_ID, source(), root));
                byte[] encoded = codec.encode(candidate);
                assertArrayEquals(encoded, codec.encode(candidate));
                String json = new String(encoded, StandardCharsets.UTF_8);
                for (var name : props.keySet()) assertTrue(json.contains("\"" + name.value() + "\":"));
                for (String text : List.of("Actual submenu label", "Actual leading", "Actual trailing", "Actual menu child")) assertTrue(json.contains(text));
                assertFalse(json.contains("libraryUri"));
                seen.addAll(props.keySet());
            }
        }
        assertEquals(701, seen.size());
        for (var property : dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.definition().properties()) {
            if (seen.contains(property.name())) continue;
            PropertyValue value = property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)
                    ? dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.reference("privateSubmenuFactory")
                    : property.acceptedKinds().contains(PropertyValueKind.BOOLEAN) ? new PropertyValue.BooleanValue(true)
                    : new PropertyValue.EnumValue("Clip", "hardEdge");
            var root = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.node(Map.of(property.name(), value));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + property.name().value() + "\":"));
            assertFalse(json.contains("privateSubmenuFactory")); assertFalse(json.contains("libraryUri"));
            if (value instanceof PropertyValue.DartObjectReferenceValue)
                assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"dartObjectReferencePresence\"}"));
            seen.add(property.name());
        }
        assertEquals(721, seen.size());
    }

    @Test
    void submenuButtonNullableArgumentsAndRequiredEmptySlotsRemainDistinct() throws Exception {
        var codec = new CanvasModelPayloadCodec();
        for (var property : dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.definition().properties()) {
            if (!property.acceptedKinds().contains(PropertyValueKind.NULL)) continue;
            var root = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.node(Map.of(property.name(), new PropertyValue.NullValue()));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"" + property.name().value() + "\":{\"kind\":\"null\"}"));
            assertTrue(json.contains("\"child\":{\"kind\":\"single\",\"child\":null}"));
            assertTrue(json.contains("\"menuChildren\":{\"kind\":\"list\",\"children\":[]}"));
            assertFalse(json.contains("\"leadingIcon\"")); assertFalse(json.contains("\"trailingIcon\""));
        }
        for (long delay : List.of(-9_007_199_254_740_991L, -1L, 0L, 9_007_199_254_740_991L)) {
            var root = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.node(Map.of(new PropertyName("hoverOpenDelayUs"), new PropertyValue.IntegerValue(BigInteger.valueOf(delay))));
            String json = new String(codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"hoverOpenDelayUs\":{\"kind\":\"integer\",\"value\":" + delay + "}"));
        }
    }

    @Test
    void submenuButtonInvalidStyleFamiliesRawEventsIconConflictsAndRequiredSlotsFailClosed() {
        var codec = new CanvasModelPayloadCodec();
        for (var props : List.of(
                Map.of(new PropertyName("style"), new PropertyValue.NullValue(), new PropertyName("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("menuStyle"), new PropertyValue.NullValue(), new PropertyName("menuStyleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of(new PropertyName("submenuIcon"), new PropertyValue.NullValue(), new PropertyName("submenuIconDefault"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("hoverOpenDelayUs"), new PropertyValue.NullValue()),
                Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true)),
                Map.of(new PropertyName("onOpen"), new PropertyValue.CallbackValue("openSubmenu")),
                Map.of(new PropertyName("menuStyleAlignmentX"), new PropertyValue.DoubleValue(BigDecimal.ZERO)))) {
            var root = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.node(new LinkedHashMap<>(props));
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
        var base = dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.node(Map.of());
        for (String missing : List.of("child", "menuChildren")) {
            var slots = new LinkedHashMap<>(base.slots()); slots.remove(new SlotName(missing));
            var root = new WidgetNode(base.id(), base.type(), base.properties(), slots);
            assertThrows(IllegalArgumentException.class, () -> codec.encode(request(new DesignerDocument(DOCUMENT_ID, source(), root))));
        }
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }
}
