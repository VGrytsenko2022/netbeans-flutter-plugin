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
import dev.flutter.netbeans.designer.canvas.CanvasViewport;
import dev.flutter.netbeans.designer.canvas.ValidatedCanvasRevisionSnapshot;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
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
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanvasModelPayloadCodecTest {
    private static final StableId DOCUMENT_ID = id(
            "4efb0eb1-b0f9-4809-bd3e-73f2486dd7bd");
    private static final CanvasRenderProfile PROFILE = new CanvasRenderProfile(
            CanvasPreviewMode.MOBILE,
            CanvasTargetPlatform.ANDROID,
            new CanvasViewport(390, 844),
            new CanvasDevicePixelRatio(1),
            new CanvasResolvedTheme("material.default.light", CanvasThemeBrightness.LIGHT),
            new CanvasLocale("en-US"),
            new CanvasTextScaleFactor(1),
            new CanvasEngineIdentity("3.44.8", "framework", "engine", "3.12.0"));

    @Test
    void exposesTheExactReviewedCoreV1CapabilitySetInPaletteOrder() {
        assertEquals(List.of(
                "flutter.material.Scaffold",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.Text"),
                BuiltInWidgetCatalog.getDefault().paletteDefinitions().stream()
                        .filter(CanvasModelPayloadCodec::supports)
                        .map(definition -> definition.typeId().value())
                        .toList());
    }

    @Test
    void projectsTheExactSixWidgetProfileWithoutSourceOrExecutableCode()
            throws Exception {
        CanvasRenderRequest request = request(document(false));

        String json = new String(
                new CanvasModelPayloadCodec().encode(request),
                StandardCharsets.UTF_8);

        assertTrue(json.contains("\"format\":\"netbeans-flutter-canvas-model\""));
        assertTrue(json.contains("\"previewMode\":\"mobile\""));
        assertTrue(json.contains("\"targetPlatform\":\"android\""));
        assertTrue(json.contains("\"type\":\"flutter.material.Scaffold\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Column\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Row\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Padding\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Center\""));
        assertTrue(json.contains("\"type\":\"flutter.widgets.Text\""));
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
    void rejectsAValidCatalogWidgetOutsideCoreV1BeforeEncoding() throws Exception {
        WidgetNode appBar = new WidgetNode(
                id("d5659c06-8da0-45d1-91c8-75eef08d9442"),
                type("flutter.material.AppBar"),
                Map.of(),
                Map.of());
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID, source(), appBar);
        CanvasRenderRequest request = request(document);

        CanvasModelPayloadException failure = assertThrows(
                CanvasModelPayloadException.class,
                () -> new CanvasModelPayloadCodec().encode(request));

        assertTrue(failure.getMessage().contains("flutter.material.AppBar"));
    }

    private static CanvasRenderRequest request(DesignerDocument document) {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var snapshot = ValidatedCanvasRevisionSnapshot.captureReadOnly(
                0, document, catalog, ValidationLimits.defaults());
        var gate = new CanvasPresentationGate(
                CanvasSessionId.parse("2c8ca807-93f6-4c6b-b16b-a2c1a438d79b"),
                DOCUMENT_ID);
        return gate.present(PROFILE, snapshot);
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
                        new WidgetSlot.ListSlot(List.of(centered))));
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
                        new WidgetSlot.ListSlot(List.of(padding, row))));
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
                        ? Map.of(
                                new PropertyName("data"), new PropertyValue.StringValue(data),
                                new PropertyName("textAlign"),
                                new PropertyValue.EnumValue("TextAlign", "center"),
                                new PropertyName("textDirection"),
                                new PropertyValue.EnumValue("TextDirection", "ltr"),
                                new PropertyName("softWrap"),
                                new PropertyValue.BooleanValue(false),
                                new PropertyName("overflow"),
                                new PropertyValue.EnumValue("TextOverflow", "ellipsis"),
                                new PropertyName("maxLines"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                                new PropertyName("semanticsLabel"),
                                new PropertyValue.StringValue("Primary greeting"),
                                new PropertyName("semanticsIdentifier"),
                                new PropertyValue.StringValue("primary-greeting"),
                                new PropertyName("textWidthBasis"),
                                new PropertyValue.EnumValue("TextWidthBasis", "longestLine"),
                                new PropertyName("selectionColor"),
                                new PropertyValue.ColorValue(0xFF336699L))
                        : Map.of(
                                new PropertyName("data"), new PropertyValue.StringValue(data),
                                new PropertyName("semanticsIdentifier"),
                                new PropertyValue.StringValue("text-" + id)),
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
