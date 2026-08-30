package dev.flutter.netbeans.designer.canvas.payload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasDevicePixelRatio;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
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
import java.util.List;
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
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.Text",
                "flutter.widgets.Icon"),
                BuiltInWidgetCatalog.getDefault().paletteDefinitions().stream()
                        .filter(CanvasModelPayloadCodec::supports)
                        .map(definition -> definition.typeId().value())
                        .toList());
    }

    @Test
    void projectsTheExactTenWidgetProfileWithoutSourceOrExecutableCode()
            throws Exception {
        CanvasRenderRequest request = request(document(false));

        String json = new String(
                new CanvasModelPayloadCodec().encode(request),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"format\":\"netbeans-flutter-canvas-model\""));
        assertTrue(json.contains("\"previewMode\":\"mobile\""));
        assertTrue(json.contains("\"targetPlatform\":\"android\""));
        assertTrue(json.contains("\"protocolVersion\":9"));
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
        assertTrue(json.contains("\"type\":\"flutter.widgets.Text\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Icon\""));
        assertTrue(json.contains("\"width\":{\"kind\":\"integer\",\"value\":120}"));
        assertTrue(json.contains("\"height\":{\"kind\":\"double\",\"value\":48.5}"));
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
    void projectsThemeBoundComplexTextValuesInProtocolV9() throws Exception {
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

        assertTrue(json.contains("\"protocolVersion\":9"), json);
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
    void projectsResolvedThemeOverridesAndComponentsInCanonicalProtocolV9Order()
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

    private static CanvasRenderRequest request(DesignerDocument document) {
        return request(PROFILE, document);
    }

    private static CanvasRenderRequest request(
            CanvasRenderProfile profile, DesignerDocument document) {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var snapshot = ValidatedCanvasRevisionSnapshot.captureReadOnly(
                0, document, catalog, ValidationLimits.defaults());
        var gate = new CanvasPresentationGate(
                CanvasSessionId.parse("2c8ca807-93f6-4c6b-b16b-a2c1a438d79b"),
                DOCUMENT_ID);
        return gate.present(profile, snapshot);
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
        WidgetNode sizedBox = new WidgetNode(
                id("0cc7c095-7908-461b-b5b2-fe085343b6b2"),
                type("flutter.widgets.SizedBox"),
                Map.of(
                        new PropertyName("width"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(120)),
                        new PropertyName("height"),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(48.5))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(centered)));
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

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }
}
