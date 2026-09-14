package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FdV15StatePropertyBindingsCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void everyClosedTransformRoundTripsCanonicallyWithoutRuntimeBodiesOrInitializers() throws Exception {
        for (StatePropertyBinding.Transform transform : StatePropertyBinding.Transform.values()) {
            StateBinding.Type type = switch (transform) {
                case TEXT -> StateBinding.Type.TEXT_CONTROLLER;
                case NOT -> StateBinding.Type.BOOL;
                case CLAMP -> StateBinding.Type.DOUBLE;
                default -> StateBinding.Type.NULLABLE_INT;
            };
            var binding = new StatePropertyBinding("_state", type, Optional.empty(), transform,
                    transform == StatePropertyBinding.Transform.EQUALS
                            ? Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE)) : Optional.empty());
            var document = document(Optional.empty(), Map.of(new PropertyName("data"), binding));
            byte[] original = codec.encode(document).copyBytes();
            String json = new String(original, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"schemaVersion\": 16"));
            assertTrue(json.contains("\"propertyBindings\""));
            assertTrue(json.contains("Preview remains independent"));
            assertFalse(json.contains("setState"));
            assertFalse(json.contains("initializer"));
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(original));
            assertEquals(document, decoded.document());
            assertArrayEquals(original, codec.encode(decoded.document()).copyBytes());
        }
    }

    @Test
    void everyStateActionPreservesItsClosedValueAndPreviousEvent() throws Exception {
        for (StateBinding.Action action : StateBinding.Action.values()) {
            var binding = new StateBinding("_state", "_changed", StateBinding.Type.BOOL, Optional.empty(),
                    Optional.of(new PropertyValue.StringValue("noop")), action,
                    action == StateBinding.Action.SELECT ? Optional.of(new PropertyValue.BooleanValue(true)) : Optional.empty());
            var document = document(Optional.of(binding), Map.of());
            var bytes = codec.encode(document);
            assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document());
        }
    }

    @Test
    void frozen14MigratesBindingsWithoutNewActionAndRetainsExactOriginalBytes() throws Exception {
        var document = document(Optional.of(new StateBinding("_state", "_changed", StateBinding.Type.BOOL)), Map.of());
        String current = new String(codec.encode(document).copyBytes(), StandardCharsets.UTF_8);
        byte[] old = current.replace("\"schemaVersion\": 16", "\"schemaVersion\": 14")
                .replace("fd-v16.schema.json", "fd-v14.schema.json").getBytes(StandardCharsets.UTF_8);
        var result = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(old));
        assertEquals(14, result.sourceSchemaVersion());
        assertTrue(result.migrated());
        assertArrayEquals(old, result.original().copyBytes());
        assertEquals(document, result.document());
        assertEquals(StateBinding.Action.CHANGE, result.document().root().stateBinding().orElseThrow().action());
    }

    @Test
    void olderVersionsRejectNewMetadataAndTypesRatherThanSilentlyDroppingThem() throws Exception {
        String consumer = json(document(Optional.empty(), Map.of(new PropertyName("data"),
                new StatePropertyBinding("_state", StateBinding.Type.STRING, Optional.empty(), StatePropertyBinding.Transform.DIRECT))));
        assertInvalid(consumer.replace("\"schemaVersion\": 16", "\"schemaVersion\": 14"));
        for (StateBinding.Type type : List.of(StateBinding.Type.STRING, StateBinding.Type.INT,
                StateBinding.Type.NUM, StateBinding.Type.TEXT_CONTROLLER)) {
            String producer = json(document(Optional.of(new StateBinding("_state", "_changed", type)), Map.of()));
            assertInvalid(producer.replace("\"schemaVersion\": 16", "\"schemaVersion\": 14"));
        }
        String toggle = json(document(Optional.of(new StateBinding("_state", "_changed", StateBinding.Type.BOOL,
                Optional.empty(), Optional.empty(), StateBinding.Action.TOGGLE, Optional.empty())), Map.of()));
        assertInvalid(toggle.replace("\"schemaVersion\": 16", "\"schemaVersion\": 14"));
    }

    @Test
    void rejectsUnknownRawDuplicateMissingAndIllTypedPropertyBindingFields() throws Exception {
        String good = json(document(Optional.empty(), Map.of(new PropertyName("data"),
                new StatePropertyBinding("_state", StateBinding.Type.STRING, Optional.empty(), StatePropertyBinding.Transform.DIRECT))));
        for (String bad : List.of(good.replace("\"transform\": \"direct\"", "\"transform\": \"eval\""),
                good.replace("\"transform\": \"direct\"", "\"transform\": \"direct\", \"code\": \"run()\""),
                good.replace("\"fieldName\": \"_state\",", ""),
                good.replace("\"fieldName\": \"_state\"", "\"fieldName\": \"owner.value\""),
                good.replace("\"fieldName\": \"_state\"", "\"fieldName\": \"_state\", \"fieldName\": \"_other\""),
                good.replace("\"transform\": \"direct\"", "\"transform\": \"equals\""),
                good.replace("\"transform\": \"direct\"", "\"transform\": \"not\""),
                good.replace("\"transform\": \"direct\"", "\"transform\": \"text\""),
                good.replace("\"transform\": \"direct\"", "\"transform\": \"clamp\""),
                good.replace("\"transform\": \"direct\"", "\"transform\": \"direct\", \"comparisonValue\": {\"kind\":\"string\",\"value\":\"bad\"}"))) {
            assertInvalid(bad);
        }
    }

    @Test
    void mapIsDefensivelyImmutableBoundedAndEncodingSorted() throws Exception {
        var mutable = new LinkedHashMap<PropertyName, StatePropertyBinding>();
        var binding = new StatePropertyBinding("_state", StateBinding.Type.STRING, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        mutable.put(new PropertyName("semanticsLabel"), binding);
        mutable.put(new PropertyName("data"), binding);
        var document = document(Optional.empty(), mutable);
        mutable.clear();
        assertEquals(2, document.root().propertyBindings().size());
        assertThrows(UnsupportedOperationException.class, () -> document.root().propertyBindings().clear());
        String json = json(document);
        String map = json.substring(json.indexOf("\"propertyBindings\""));
        assertTrue(map.indexOf("\"data\"") < map.indexOf("\"semanticsLabel\""));
        for (int index = 0; index < 513; index++) mutable.put(new PropertyName("p" + index), binding);
        assertThrows(IllegalArgumentException.class, () -> document(Optional.empty(), mutable));
    }

    private void assertInvalid(String value) throws Exception {
        assertInstanceOf(FdDecodeResult.Invalid.class, codec.decode(value.getBytes(StandardCharsets.UTF_8)), value);
    }
    private String json(DesignerDocument document) throws Exception {
        return new String(codec.encode(document).copyBytes(), StandardCharsets.UTF_8);
    }
    private static DesignerDocument document(Optional<StateBinding> action, Map<PropertyName, StatePropertyBinding> consumers) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(Optional.of("../fd-v16.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor("page.dart", "Page", WidgetClassKind.STATEFUL, Optional.empty(), new ManagedRegions(region, region)),
                Optional.empty(), new WidgetNode(StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                        new WidgetTypeId("flutter.widgets.Text"),
                        Map.of(new PropertyName("data"), new PropertyValue.StringValue("Preview remains independent")),
                        Map.of(), Extensions.empty(), action, consumers), Extensions.empty());
    }
}
