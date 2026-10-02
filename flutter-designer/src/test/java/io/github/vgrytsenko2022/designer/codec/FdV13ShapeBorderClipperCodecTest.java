package io.github.vgrytsenko2022.designer.codec;

import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.PhysicalShapeTestSupport.*;

class FdV13ShapeBorderClipperCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void allSixShapesPhysicalDirectionalAndIgnoredRadiusAndDirectionRoundTripExactly() throws Exception {
        for (var shape : PropertyValue.ShapeBorderClipperValue.Shape.values()) {
            for (boolean directional : List.of(false, true)) {
                for (var direction : PropertyValue.ShapeBorderClipperValue.TextDirection.values()) {
                    var value = new PropertyValue.ShapeBorderClipperValue(shape, radius(directional).geometry(), Optional.of(direction));
                    var properties = new LinkedHashMap<>(defaults()); properties.put(name("clipper"), value);
                    var original = document(physicalShape(properties, true));
                    var encoded = codec.encode(original);
                    String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
                    assertTrue(json.contains("\"schemaVersion\": 17"), json);
                    assertTrue(json.contains("\"kind\": \"shapeBorderClipper\""), json);
                    var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
                    assertFalse(decoded.migrated());
                    assertEquals(original, decoded.document());
                    assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
                }
            }
        }
    }

    @Test
    void rejectsUnknownMissingExtraMalformedAndPreV13PresetFieldsAtCodecBoundary() throws Exception {
        String json = new String(codec.encode(document(physicalShape(defaults(), false))).copyBytes(), StandardCharsets.UTF_8);
        for (String invalid : List.of(
                json.replace("\"shape\": \"roundedRectangle\"", "\"shape\": \"custom\""),
                json.replace("\"textDirection\": null", "\"textDirection\": \"up\""),
                json.replace("\"textDirection\": null", "\"rawCode\": \"danger()\""),
                json.replace("\"textDirection\": null", "\"textDirection\": null, \"extra\": true"),
                json.replace("\"schemaVersion\": 17", "\"schemaVersion\": 12"),
                json.replace("\"x\": 0", "\"x\": -1"))) {
            assertInstanceOf(FdDecodeResult.Invalid.class, codec.decode(invalid.getBytes(StandardCharsets.UTF_8)), invalid);
        }
        String directional = new String(codec.encode(document(physicalShape(
                new LinkedHashMap<>(java.util.Map.of(name("color"), new PropertyValue.ColorValue(0xFF000000L),
                        name("clipper"), clipper(PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE, true))),
                false))).copyBytes(), StandardCharsets.UTF_8);
        assertInstanceOf(FdDecodeResult.Invalid.class, codec.decode(directional.replace(
                "\"textDirection\": \"rtl\"", "\"textDirection\": null").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void migratesV12WithoutChangingExistingValuesOrOriginalBytesAndReencodesV13() throws Exception {
        var original = document(text());
        String current = new String(codec.encode(original).copyBytes(), StandardCharsets.UTF_8);
        byte[] old = current.replace("\"schemaVersion\": 17", "\"schemaVersion\": 12").getBytes(StandardCharsets.UTF_8);
        var migrated = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(old));
        assertTrue(migrated.migrated());
        assertEquals(12, migrated.sourceSchemaVersion());
        assertEquals(original, migrated.document());
        assertArrayEquals(old, migrated.original().copyBytes());
        assertTrue(new String(codec.encode(migrated.document()).copyBytes(), StandardCharsets.UTF_8).contains("\"schemaVersion\": 17"));
    }
}
