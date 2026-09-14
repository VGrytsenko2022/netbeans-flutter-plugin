package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FdV14StateBindingCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void everyClosedTypeAndRetainedCallbackRoundTripWithoutEmbeddingSourceBodies() throws Exception {
        List<Optional<PropertyValue>> previousValues = List.of(Optional.empty(),
                Optional.of(new PropertyValue.NullValue()),
                Optional.of(new PropertyValue.StringValue("noop")),
                Optional.of(new PropertyValue.CallbackValue("_previousHandler")),
                Optional.of(reference("callbacks", Optional.of("handleChanged"))));
        for (StateBinding.Type type : StateBinding.Type.values()) {
            for (Optional<PropertyValue> previous : previousValues) {
                StateBinding binding = new StateBinding("_selectedValue", "_onSelectedValueChanged", type,
                        type == StateBinding.Type.NULLABLE_REFERENCE
                                ? Optional.of(reference("Choice", Optional.empty())) : Optional.empty(), previous);
                DesignerDocument document = document(Optional.of(binding));
                OriginalFdBytes bytes = codec.encode(document);
                String json = new String(bytes.copyBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("\"schemaVersion\": 16"), json);
                assertTrue(json.contains("\"stateBinding\""), json);
                assertTrue(json.contains("\"value\": true"), "Literal preview must remain stored");
                assertFalse(json.contains("setState"));
                assertFalse(json.contains("initializer"));
                var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
                assertFalse(decoded.migrated());
                assertEquals(document, decoded.document());
                assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
            }
        }
    }

    @Test
    void preservesFrozenV13OriginalAndMigratesOnlyTheSchemaReference() throws Exception {
        String current = json(Optional.empty());
        byte[] old = current.replace("\"schemaVersion\": 16", "\"schemaVersion\": 13")
                .replace("fd-v16.schema.json", "fd-v13.schema.json").getBytes(StandardCharsets.UTF_8);
        var migrated = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(old));
        assertTrue(migrated.migrated());
        assertEquals(13, migrated.sourceSchemaVersion());
        assertArrayEquals(old, migrated.original().copyBytes());
        assertEquals(document(Optional.empty()), migrated.document());
        assertTrue(migrated.document().root().stateBinding().isEmpty());
        assertArrayEquals(current.getBytes(StandardCharsets.UTF_8), codec.encode(migrated.document()).copyBytes());
    }

    @Test
    void rejectsBindingInAllOlderVersionsAndPreservesFutureBytesOpaque() throws Exception {
        String current = json(Optional.of(new StateBinding("_value", "_changed", StateBinding.Type.BOOL)));
        for (int version = 1; version < 14; version++) {
            String old = current.replace("\"schemaVersion\": 16", "\"schemaVersion\": " + version);
            assertInstanceOf(FdDecodeResult.Invalid.class, codec.decode(old.getBytes(StandardCharsets.UTF_8)));
        }
        byte[] future = current.replace("\"schemaVersion\": 16", "\"schemaVersion\": 17")
                .getBytes(StandardCharsets.UTF_8);
        var result = assertInstanceOf(FdDecodeResult.UnsupportedNewer.class, codec.decode(future));
        assertArrayEquals(future, result.original().copyBytes());
    }

    @Test
    void rejectsUnknownMissingDuplicateRawAndIllTypedMetadata() throws Exception {
        String current = json(Optional.of(new StateBinding("_value", "_changed", StateBinding.Type.BOOL)));
        for (String invalid : List.of(
                current.replace("\"type\": \"bool\"", "\"type\": \"dartExpression\""),
                current.replace("\"fieldName\": \"_value\",", ""),
                current.replace("\"handlerName\": \"_changed\",", ""),
                current.replace("\"fieldName\": \"_value\"", "\"fieldName\": \"_value\", \"fieldName\": \"_other\""),
                current.replace("\"type\": \"bool\"", "\"type\": \"bool\", \"body\": \"danger()\""),
                current.replace("\"type\": \"bool\"", "\"type\": \"bool\", \"previousOnChanged\": {\"kind\":\"dartExpression\",\"code\":\"danger()\"}"),
                current.replace("\"type\": \"bool\"", "\"type\": \"nullableReference\""),
                current.replace("\"fieldName\": \"_value\"", "\"fieldName\": \"owner.value\""),
                current.replace("\"handlerName\": \"_changed\"", "\"handlerName\": \"_value\""))) {
            assertInstanceOf(FdDecodeResult.Invalid.class, codec.decode(invalid.getBytes(StandardCharsets.UTF_8)), invalid);
        }
    }

    @Test
    void modelRejectsPublicNamesExpressionsRecursiveBodiesAndReferenceFactories() {
        for (String name : List.of("value", "_", "__value", "_value()", "_value;", "_" + "a".repeat(128))) {
            assertThrows(IllegalArgumentException.class, () -> new StateBinding(name, "_changed", StateBinding.Type.BOOL));
        }
        assertThrows(IllegalArgumentException.class, () -> new StateBinding("_value", "_changed", StateBinding.Type.BOOL,
                Optional.of(reference("Choice", Optional.empty())), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new StateBinding("_value", "_changed", StateBinding.Type.NULLABLE_REFERENCE,
                Optional.of(reference("Choice", Optional.of("nested"))), Optional.empty()));
        var factory = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
        assertThrows(IllegalArgumentException.class, () -> new StateBinding("_value", "_changed", StateBinding.Type.NULLABLE_REFERENCE,
                Optional.of(factory), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new StateBinding("_value", "_changed", StateBinding.Type.BOOL,
                Optional.empty(), Optional.of(new PropertyValue.StringValue("danger()"))));
    }

    @Test
    void oldNodeConstructorsRemainUnboundAndDefensivelyImmutable() {
        WidgetNode root = document(Optional.empty()).root();
        assertTrue(new WidgetNode(root.id(), root.type(), root.properties(), root.slots()).stateBinding().isEmpty());
        assertTrue(new WidgetNode(root.id(), root.type(), root.properties(), root.slots(), root.extensions()).stateBinding().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> root.properties().put(new PropertyName("value"), new PropertyValue.BooleanValue(false)));
    }

    private String json(Optional<StateBinding> binding) throws Exception {
        return new String(codec.encode(document(binding)).copyBytes(), StandardCharsets.UTF_8);
    }

    private static PropertyValue.DartObjectReferenceValue reference(String symbol, Optional<String> member) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:example/choices.dart"), symbol,
                member, PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }

    private static DesignerDocument document(Optional<StateBinding> binding) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(Optional.of("../fd-v16.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor("page.dart", "Page", WidgetClassKind.STATEFUL, Optional.empty(),
                        new ManagedRegions(region, region)), Optional.empty(),
                new WidgetNode(StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                        new WidgetTypeId("flutter.material.Checkbox"),
                        Map.of(new PropertyName("value"), new PropertyValue.BooleanValue(true)),
                        Map.of(), Extensions.empty(), binding), Extensions.empty());
    }
}
