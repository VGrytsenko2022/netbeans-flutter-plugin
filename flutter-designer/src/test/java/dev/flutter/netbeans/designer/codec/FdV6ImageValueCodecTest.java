package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdV6ImageValueCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsTopLevelProviderAndEveryDecorationImageColorFilterVariant()
            throws Exception {
        DesignerDocument document = imageDocument();
        OriginalFdBytes encoded = codec.encode(document);
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 16"), json);
        assertTrue(json.contains("\"kind\": \"imageProvider\""), json);
        assertTrue(json.contains("\"providerKind\": \"exactAsset\""), json);
        assertTrue(json.contains("\"packageName\": \"reviewed_icons\""), json);
        assertTrue(json.contains("\"exactScale\": 2"), json);
        assertTrue(json.contains("\"width\": 512"), json);
        assertTrue(json.contains("\"height\": null"), json);
        assertTrue(json.contains("\"policy\": \"fit\""), json);
        assertTrue(json.contains("\"allowUpscaling\": true"), json);
        assertTrue(json.contains("\"kind\": \"mode\""), json);
        assertTrue(json.contains("\"kind\": \"matrix\""), json);
        assertTrue(json.contains("\"kind\": \"linearToSrgbGamma\""), json);
        assertTrue(json.contains("\"kind\": \"srgbToLinearGamma\""), json);
        assertTrue(json.contains("\"kind\": \"saturation\""), json);
        assertTrue(json.contains("\"handler\": \"onImageError\""), json);
        assertTrue(json.contains("\"fit\": \"fill\""), json);
        assertTrue(json.contains("\"repeat\": \"repeatX\""), json);
        assertTrue(json.contains("\"filterQuality\": \"high\""), json);
        assertEquals(
                "1acda383c41457e933ed56a295f539e194bb0d3daf3125d51917726cfa1a5e0c",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(encoded.copyBytes())),
                "The canonical all-image current fixture is a byte-for-byte golden");
        assertEquals(
                "d623cae68786cae8e9cb0d0eaf44931560b6227c966d09a31060e53c460d87ea",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(json.replace("\"schemaVersion\": 16", "\"schemaVersion\": 13")
                                .replace("fd-v16.schema.json", "fd-v13.schema.json")
                                .getBytes(StandardCharsets.UTF_8))),
                "Only the schema URI and current schema version differ from the frozen v13 image golden");

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        assertEquals(16, decoded.sourceSchemaVersion());
        assertFalse(decoded.migrated());
        assertEquals(document, decoded.document());
        assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void roundTripsReservedUnresolvedDirectImageAcrossSaveAndReopen()
            throws Exception {
        PropertyName imageProperty = new PropertyName("image");
        WidgetNode image = new WidgetNode(
                StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                new WidgetTypeId("flutter.widgets.Image"),
                Map.of(imageProperty, PropertyValue.ImageProviderValue.unresolved()),
                Map.of(),
                Extensions.empty());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DesignerDocument document = new DesignerDocument(
                Optional.of("../fd-v16.schema.json"),
                StableId.parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                new DartSourceDescriptor(
                        "unresolved_image_page.dart",
                        "UnresolvedImagePage",
                        WidgetClassKind.STATELESS,
                        Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(),
                image,
                Extensions.empty());

        OriginalFdBytes encoded = codec.encode(document);
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        assertTrue(json.contains(
                "\"assetName\": \"__netbeans_flutter_designer__/unresolved-image.png\""),
                json);

        FdDecodeResult.Current reopened = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        PropertyValue.ImageProviderValue provider = assertInstanceOf(
                PropertyValue.ImageProviderValue.class,
                reopened.document().root().properties().get(imageProperty));
        assertTrue(provider.isUnresolved());
        assertEquals(document, reopened.document());
        assertArrayEquals(encoded.copyBytes(),
                codec.encode(reopened.document()).copyBytes());
    }

    @Test
    void migratesV5BoxDecorationWithoutInventingAnImage() throws Exception {
        DesignerDocument document = imageFreeDocument();
        String current = new String(
                codec.encode(document).copyBytes(), StandardCharsets.UTF_8);
        String v5 = current
                .replace("\"schemaVersion\": 16", "\"schemaVersion\": 5")
                .replace("../fd-v16.schema.json", "../fd-v5.schema.json")
                .replaceAll("(?m)^\\s*\"image\": null,\\R", "");

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(v5.getBytes(StandardCharsets.UTF_8)));
        assertEquals(5, decoded.sourceSchemaVersion());
        assertTrue(decoded.migrated());
        assertEquals("../fd-v16.schema.json",
                decoded.document().schemaReference().orElseThrow());
        PropertyValue.BoxDecorationValue decoration = assertInstanceOf(
                PropertyValue.BoxDecorationValue.class,
                decoded.document().root().properties().get(
                        new PropertyName("decoration")));
        assertTrue(decoration.image().isEmpty());
    }

    @Test
    void rejectsImageProviderInV5AndUnknownOrIncoherentV6ImageFields()
            throws Exception {
        PropertyValue.ImageProviderValue provider =
                assertInstanceOf(PropertyValue.ImageProviderValue.class,
                        imageDocument().root().properties().get(
                                new PropertyName("provider")));
        String providerJson = new String(
                codec.encode(document(Map.of(
                        new PropertyName("provider"), provider))).copyBytes(),
                StandardCharsets.UTF_8);
        String claimedV5 = providerJson.replace(
                "\"schemaVersion\": 16", "\"schemaVersion\": 5");
        FdDecodeResult.Invalid old = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(claimedV5.getBytes(StandardCharsets.UTF_8)));
        assertTrue(old.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.pointer().endsWith("/kind")
                && diagnostic.message().contains("schema version 6")));

        String current = new String(
                codec.encode(imageDocument()).copyBytes(), StandardCharsets.UTF_8);
        String unknown = current.replaceFirst(
                "\"assetName\": \"assets/images/logo.png\",",
                "\"assetName\": \"assets/images/logo.png\",\n"
                + "        \"networkUrl\": \"https://example.invalid/logo.png\",");
        FdDecodeResult.Invalid unknownResult = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(unknown.getBytes(StandardCharsets.UTF_8)));
        assertTrue(unknownResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == FdCodecDiagnosticCode.UNKNOWN_FIELD
                && diagnostic.pointer().endsWith("/networkUrl")));

        String leadingTilde = current.replaceFirst(
                "\"assetName\": \"assets/images/logo.png\"",
                "\"assetName\": \"~/images/logo.png\"");
        FdDecodeResult.Invalid leadingTildeResult = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(leadingTilde.getBytes(StandardCharsets.UTF_8)));
        assertTrue(leadingTildeResult.diagnostics().stream().anyMatch(
                diagnostic -> diagnostic.code()
                        == FdCodecDiagnosticCode.INVALID_VALUE));

        String percentEncoded = current.replaceFirst(
                "\"assetName\": \"assets/images/logo.png\"",
                "\"assetName\": \"assets/%2e%2e/logo.png\"");
        FdDecodeResult.Invalid percentEncodedResult = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(percentEncoded.getBytes(StandardCharsets.UTF_8)));
        assertTrue(percentEncodedResult.diagnostics().stream().anyMatch(
                diagnostic -> diagnostic.code()
                        == FdCodecDiagnosticCode.INVALID_VALUE));

        for (String whitespacePath : List.of(
                " ", "\u1680", " assets/images/logo.png",
                "assets/images/logo.png ")) {
            String whitespaceAsset = current.replaceFirst(
                    "\"assetName\": \"assets/images/logo.png\"",
                    "\"assetName\": \"" + whitespacePath + "\"");
            FdDecodeResult.Invalid whitespaceResult = assertInstanceOf(
                    FdDecodeResult.Invalid.class,
                    codec.decode(whitespaceAsset.getBytes(StandardCharsets.UTF_8)));
            assertTrue(whitespaceResult.diagnostics().stream().anyMatch(
                    diagnostic -> diagnostic.code()
                            == FdCodecDiagnosticCode.INVALID_VALUE));
        }

        String invalidFit = current.replaceFirst(
                "\"fit\": \"fill\"", "\"fit\": \"cover\"");
        FdDecodeResult.Invalid invalidFitResult = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(invalidFit.getBytes(StandardCharsets.UTF_8)));
        assertTrue(invalidFitResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == FdCodecDiagnosticCode.INVALID_VALUE));

        String validContain = current.replaceFirst(
                "\"fit\": \"fill\"", "\"fit\": \"contain\"");
        assertInstanceOf(FdDecodeResult.Current.class,
                codec.decode(validContain.getBytes(StandardCharsets.UTF_8)));

        String invalidNone = current.replaceFirst(
                "\"fit\": \"fill\"", "\"fit\": \"none\"");
        FdDecodeResult.Invalid invalidNoneResult = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(invalidNone.getBytes(StandardCharsets.UTF_8)));
        assertTrue(invalidNoneResult.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == FdCodecDiagnosticCode.INVALID_VALUE));
    }

    private static DesignerDocument imageDocument() {
        PropertyValue.ImageProviderValue provider =
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET,
                        "assets/images/logo.png",
                        Optional.of("reviewed_icons"),
                        Optional.of(BigDecimal.valueOf(2)),
                        Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(
                                Optional.of(512), Optional.empty(),
                                PropertyValue.ImageProviderValue.ResizePolicy.FIT,
                                true)));
        PropertyValue.AlignmentGeometryValue center =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                        BigDecimal.ZERO, BigDecimal.ZERO);
        PropertyValue.DecorationImageValue.Rect slice =
                new PropertyValue.DecorationImageValue.Rect(
                        BigDecimal.ONE, BigDecimal.TWO,
                        BigDecimal.valueOf(20), BigDecimal.valueOf(30));
        List<BigDecimal> matrix = java.util.stream.IntStream.range(0, 20)
                .mapToObj(BigDecimal::valueOf)
                .toList();

        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyName("provider"), provider);
        properties.put(new PropertyName("mode"), decoration(provider,
                new PropertyValue.DecorationImageValue.Mode(
                        new ColorSource.Theme(new ThemeToken(
                                "material.colorScheme.primary")),
                        PropertyValue.PaintValue.BlendMode.SRC_IN),
                center, Optional.of(slice), true));
        properties.put(new PropertyName("matrix"), decoration(provider,
                new PropertyValue.DecorationImageValue.Matrix(matrix),
                center, Optional.empty(), false));
        properties.put(new PropertyName("linearGamma"), decoration(provider,
                new PropertyValue.DecorationImageValue.LinearToSrgbGamma(),
                center, Optional.empty(), false));
        properties.put(new PropertyName("srgbGamma"), decoration(provider,
                new PropertyValue.DecorationImageValue.SrgbToLinearGamma(),
                center, Optional.empty(), false));
        properties.put(new PropertyName("saturation"), decoration(provider,
                new PropertyValue.DecorationImageValue.Saturation(
                        new BigDecimal("1.25")),
                center, Optional.empty(), false));
        return document(properties);
    }

    private static PropertyValue.BoxDecorationValue decoration(
            PropertyValue.ImageProviderValue provider,
            PropertyValue.DecorationImageValue.ColorFilter filter,
            PropertyValue.AlignmentGeometryValue alignment,
            Optional<PropertyValue.DecorationImageValue.Rect> centerSlice,
            boolean complete) {
        PropertyValue.DecorationImageValue image =
                new PropertyValue.DecorationImageValue(
                        provider,
                        complete
                                ? Optional.of(new PropertyValue.CallbackValue("onImageError"))
                                : Optional.empty(),
                        Optional.of(filter),
                        centerSlice.isPresent()
                                ? Optional.of(PropertyValue.DecorationImageValue.BoxFit.FILL)
                                : Optional.empty(),
                        alignment,
                        centerSlice,
                        complete
                                ? PropertyValue.DecorationImageValue.ImageRepeat.REPEAT_X
                                : PropertyValue.DecorationImageValue.ImageRepeat.NO_REPEAT,
                        complete,
                        complete ? new BigDecimal("1.5") : BigDecimal.ONE,
                        complete ? new BigDecimal("0.75") : BigDecimal.ONE,
                        complete
                                ? PropertyValue.PaintValue.FilterQuality.HIGH
                                : PropertyValue.PaintValue.FilterQuality.MEDIUM,
                        complete,
                        complete);
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(), Optional.of(image), Optional.empty(), Optional.empty(),
                List.of(), Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static DesignerDocument imageFreeDocument() {
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
                        Optional.empty(), Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        return document(Map.of(new PropertyName("decoration"), decoration));
    }

    private static DesignerDocument document(Map<PropertyName, PropertyValue> properties) {
        WidgetNode root = new WidgetNode(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new WidgetTypeId("flutter.widgets.Container"),
                properties,
                Map.of());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "image_page.dart", "ImagePage", WidgetClassKind.STATELESS,
                Optional.of("test"), new ManagedRegions(region, region));
        return new DesignerDocument(
                Optional.of("../fd-v16.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                source,
                Optional.empty(),
                root,
                Extensions.empty());
    }
}
