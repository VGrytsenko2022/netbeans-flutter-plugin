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
                "flutter.widgets.ListBody",
                "flutter.widgets.OverflowBar",
                "flutter.widgets.SafeArea",
                "flutter.widgets.ListView",
                "flutter.widgets.GridView",
                "flutter.widgets.SingleChildScrollView",
                "flutter.widgets.Text",
                "flutter.widgets.Icon",
                "flutter.widgets.Image",
                "flutter.widgets.ColoredBox",
                "flutter.widgets.Placeholder",
                "flutter.widgets.Directionality",
                "flutter.widgets.DecoratedBox",
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
                "flutter.widgets.ExcludeSemantics",
                "flutter.widgets.BlockSemantics",
                "flutter.widgets.MergeSemantics",
                "flutter.widgets.IndexedSemantics",
                "flutter.widgets.ExcludeFocus",
                "flutter.widgets.ExcludeFocusTraversal"),
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(omittedJson.contains("\"protocolVersion\":18"), omittedJson);
        assertTrue(omittedJson.contains(
                "\"type\":\"flutter.widgets.Placeholder\""), omittedJson);
        assertTrue(omittedJson.contains(
                "\"properties\":{},\"slots\":{\"child\":{"
                + "\"kind\":\"single\",\"child\":null}"), omittedJson);
        assertFalse(omittedJson.contains("\"color\":"), omittedJson);
        assertFalse(omittedJson.contains("\"strokeWidth\":"), omittedJson);
        assertFalse(omittedJson.contains("\"fallbackWidth\":"), omittedJson);
        assertFalse(omittedJson.contains("\"fallbackHeight\":"), omittedJson);

        assertTrue(explicitJson.contains("\"protocolVersion\":18"), explicitJson);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(physicalJson.contains("\"protocolVersion\":18"), physicalJson);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(physicalJson.contains("\"protocolVersion\":18"), physicalJson);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(omittedJson.contains("\"protocolVersion\":18"), omittedJson);
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
        assertTrue(json.contains("\"protocolVersion\":18"));
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
                    assertTrue(json.contains("\"protocolVersion\":18"), json);
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
                    assertTrue(json.contains("\"protocolVersion\":18"), json);
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
                assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
                assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
                assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
                assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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
            assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

        assertTrue(json.contains("\"protocolVersion\":18"), json);
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

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }
}
