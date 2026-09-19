package io.github.vgrytsenko2022.designer.codec;

import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdV10NullValueCodecTest {
    private static final PropertyName INDEX = new PropertyName("index");
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void canonicalCurrentSchemaEncodesExplicitNullAsAnExactTaggedObject() throws Exception {
        OriginalFdBytes encoded = codec.encode(document(Map.of(
                INDEX, new PropertyValue.NullValue())));
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 17"), json);
        assertTrue(json.contains("\"$schema\": \"../fd-v17.schema.json\""), json);
        String exactNull = "\"index\": {\n        \"kind\": \"null\"\n      }";
        assertTrue(json.contains(exactNull), json);
        assertFalse(json.substring(json.indexOf("\"index\""),
                json.indexOf("\n      }", json.indexOf("\"index\"")) + 8)
                .contains("\"value\""), json);

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        assertEquals(17, decoded.sourceSchemaVersion());
        assertFalse(decoded.migrated());
        assertEquals(new PropertyValue.NullValue(),
                decoded.document().root().properties().get(INDEX));
        assertArrayEquals(encoded.copyBytes(),
                codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void rejectsTypedNullBeforeV10AndRejectsMalformedRepresentations() throws Exception {
        String current = new String(codec.encode(document(Map.of(
                INDEX, new PropertyValue.NullValue()))).copyBytes(),
                StandardCharsets.UTF_8);

        assertInvalid(current
                        .replace("\"schemaVersion\": 17", "\"schemaVersion\": 9")
                        .replace("../fd-v17.schema.json", "../fd-v9.schema.json"),
                "/root/properties/index/kind");
        assertInvalid(current.replace(
                        "\"kind\": \"null\"",
                        "\"kind\": \"null\",\n        \"value\": null"),
                "/root/properties/index/value");
        assertInvalid(current.replace(
                        "\"index\": {\n        \"kind\": \"null\"\n      }",
                        "\"index\": null"),
                "/root/properties/index");
    }

    @Test
    void migratesV9OmissionWithoutInventingAnExplicitNull() throws Exception {
        String current = new String(codec.encode(document(Map.of())).copyBytes(),
                StandardCharsets.UTF_8);
        String legacy = current
                .replace("\"schemaVersion\": 17", "\"schemaVersion\": 9")
                .replace("../fd-v17.schema.json", "../fd-v9.schema.json");

        FdDecodeResult.Current migrated = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(legacy.getBytes(StandardCharsets.UTF_8)));
        assertEquals(9, migrated.sourceSchemaVersion());
        assertTrue(migrated.migrated());
        assertEquals(Optional.of("../fd-v17.schema.json"),
                migrated.document().schemaReference());
        assertFalse(migrated.document().root().properties().containsKey(INDEX));
        assertTrue(new String(codec.encode(migrated.document()).copyBytes(),
                StandardCharsets.UTF_8).contains("\"schemaVersion\": 17"));
    }

    private void assertInvalid(String json, String pointer) throws Exception {
        FdDecodeResult.Invalid invalid = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(json.getBytes(StandardCharsets.UTF_8)), json);
        assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.pointer().equals(pointer)
                || diagnostic.pointer().startsWith(pointer)),
                () -> invalid.diagnostics().toString());
    }

    private static DesignerDocument document(
            Map<PropertyName, PropertyValue> properties) {
        WidgetNode root = new WidgetNode(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new WidgetTypeId("flutter.widgets.IndexedStack"),
                properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(java.util.List.of())),
                Extensions.empty());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(
                Optional.of("../fd-v17.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor(
                        "indexed_stack_page.dart", "IndexedStackPage",
                        WidgetClassKind.STATELESS, Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(), root, Extensions.empty());
    }
}
