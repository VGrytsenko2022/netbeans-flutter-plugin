package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.model.PropertyValue.PointerDeviceKindSetValue;
import dev.flutter.netbeans.designer.model.PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FdV16PointerDeviceKindSetCodecTest {
    private static final PropertyName DEVICES = new PropertyName("supportedDevices");
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void closedImmutableSetUsesCanonicalSdkOrderAndRejectsDuplicatesNullsAndUnknownKinds() {
        var mutable = new ArrayList<>(List.of(PointerDeviceKind.UNKNOWN, PointerDeviceKind.MOUSE,
                PointerDeviceKind.TOUCH, PointerDeviceKind.TRACKPAD));
        var value = new PointerDeviceKindSetValue(mutable);
        mutable.clear();
        assertEquals(List.of(PointerDeviceKind.TOUCH, PointerDeviceKind.MOUSE,
                PointerDeviceKind.TRACKPAD, PointerDeviceKind.UNKNOWN), value.values());
        assertThrows(UnsupportedOperationException.class, () -> value.values().clear());
        assertThrows(IllegalArgumentException.class, () -> new PointerDeviceKindSetValue(
                List.of(PointerDeviceKind.MOUSE, PointerDeviceKind.MOUSE)));
        assertThrows(NullPointerException.class, () -> new PointerDeviceKindSetValue(null));
        assertThrows(NullPointerException.class, () -> new PointerDeviceKindSetValue(java.util.Arrays.asList((PointerDeviceKind) null)));
        assertThrows(IllegalArgumentException.class, () -> PointerDeviceKind.fromWireName("keyboard"));
        assertThrows(IllegalArgumentException.class, () -> PointerDeviceKind.fromWireName("TOUCH"));
        assertEquals(PropertyValueKind.POINTER_DEVICE_KIND_SET, value.kind());
        assertEquals("pointerDeviceKindSet", value.kind().wireName());
    }

    @Test
    void everySubsetIncludingEmptyRoundTripsCanonically() throws Exception {
        PointerDeviceKind[] kinds = PointerDeviceKind.values();
        for (int mask = 0; mask < 64; mask++) {
            var values = new ArrayList<PointerDeviceKind>();
            for (int index = kinds.length - 1; index >= 0; index--) {
                if ((mask & (1 << index)) != 0) values.add(kinds[index]);
            }
            var original = document(Map.of(DEVICES, new PointerDeviceKindSetValue(values)));
            byte[] bytes = codec.encode(original).copyBytes();
            var result = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(result.migrated());
            assertEquals(17, result.sourceSchemaVersion());
            assertEquals(original, result.document());
            assertArrayEquals(bytes, codec.encode(result.document()).copyBytes());
        }
    }

    @Test
    void omissionNullAndEmptySetRemainThreeDistinctStates() throws Exception {
        var absent = document(Map.of());
        var explicitNull = document(Map.of(DEVICES, new PropertyValue.NullValue()));
        var empty = document(Map.of(DEVICES, new PointerDeviceKindSetValue(List.of())));
        for (var original : List.of(absent, explicitNull, empty)) {
            assertEquals(original, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(original))).document());
        }
        assertFalse(json(absent).contains("supportedDevices"));
        assertTrue(json(explicitNull).contains("\"kind\": \"null\""));
        assertTrue(json(empty).contains("\"values\": []"));
        assertNotEquals(absent.root().properties(), explicitNull.root().properties());
        assertNotEquals(explicitNull.root().properties(), empty.root().properties());
    }

    @Test
    void malformedSetContentsUnknownFieldsAndOldVersionsFailClosed() throws Exception {
        String valid = json(document(Map.of(DEVICES, new PointerDeviceKindSetValue(List.of(PointerDeviceKind.MOUSE)))));
        for (String values : List.of("[\"mouse\", \"mouse\"]", "[\"keyboard\"]", "[\"MOUSE\"]", "[null]", "[true]", "[{}]", "\"mouse\"", "null")) {
            assertInvalid(valid.replaceAll("(?s)\"values\"\\s*:\\s*\\[[^]]*]", "\"values\": " + values));
        }
        assertInvalid(valid.replace("\"kind\": \"pointerDeviceKindSet\"", "\"kind\": \"pointerDeviceKindSet\", \"extra\": true"));
        assertInvalid(valid.replace("\"values\"", "\"devices\""));
        for (int version = 1; version < 16; version++) {
            assertInvalid(valid.replace("\"schemaVersion\": 17", "\"schemaVersion\": " + version));
        }
    }

    @Test
    void frozenV15MigratesBoundedReferencesAndPreservesOriginalBytesAndStateMetadata() throws Exception {
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Preview")), Map.of(),
                Extensions.empty(), Optional.empty(), Map.of(new PropertyName("data"),
                new StatePropertyBinding("_query", StateBinding.Type.TEXT_CONTROLLER, Optional.empty(), StatePropertyBinding.Transform.TEXT)));
        var plain = document(Map.of());
        for (String reference : List.of("../fd-v15.schema.json", "urn:netbeans-flutter-designer:schema:fd:15", "custom.json")) {
            var value = new DesignerDocument(Optional.of(reference), plain.documentId(), plain.source(),
                    Optional.empty(), root, Extensions.empty());
            byte[] old = json(value).replace("\"schemaVersion\": 17", "\"schemaVersion\": 15").getBytes(StandardCharsets.UTF_8);
            var result = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(old));
            assertEquals(15, result.sourceSchemaVersion());
            assertTrue(result.migrated());
            assertArrayEquals(old, result.original().copyBytes());
            assertEquals(root, result.document().root());
            String expected = reference.equals("custom.json") ? reference : reference.replace("15", "17");
            assertEquals(Optional.of(expected), result.document().schemaReference());
            assertTrue(json(result.document()).contains("\"schemaVersion\": 17"));
        }
    }

    private void assertInvalid(String value) throws Exception {
        assertInstanceOf(FdDecodeResult.Invalid.class, codec.decode(value.getBytes(StandardCharsets.UTF_8)), value);
    }
    private String json(DesignerDocument document) throws Exception {
        return new String(codec.encode(document).copyBytes(), StandardCharsets.UTF_8);
    }
    private static DesignerDocument document(Map<PropertyName, PropertyValue> values) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(Optional.of("../fd-v17.schema.json"), StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATEFUL,
                        Optional.empty(), new ManagedRegions(region, region)), Optional.empty(),
                new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.GestureDetector"),
                        values, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())), Extensions.empty());
    }
}
