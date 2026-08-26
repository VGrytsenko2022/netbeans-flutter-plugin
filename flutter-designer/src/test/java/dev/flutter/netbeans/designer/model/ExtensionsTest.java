package dev.flutter.netbeans.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.model.json.JsonValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtensionsTest {
    @Test
    void preservesArbitraryJsonIncludingExplicitNull() {
        JsonValue value = new JsonValue.ObjectValue(Map.of(
                "enabled", new JsonValue.BooleanValue(true),
                "payload", new JsonValue.ArrayValue(List.of(
                        JsonValue.NullValue.INSTANCE,
                        new JsonValue.StringValue("data"),
                        new JsonValue.NumberValue(new BigDecimal("12.50"))))));
        Extensions extensions = new Extensions(Map.of(
                new ExtensionKey("example.dev:metadata"), value));

        assertEquals(value, extensions.values().get(new ExtensionKey("example.dev:metadata")));
    }

    @Test
    void extensionAndJsonObjectsKeepInsertionOrderAndSnapshotInputs() {
        LinkedHashMap<String, JsonValue> json = new LinkedHashMap<>();
        json.put("z", JsonValue.NullValue.INSTANCE);
        json.put("a", new JsonValue.BooleanValue(false));
        JsonValue.ObjectValue object = new JsonValue.ObjectValue(json);

        LinkedHashMap<ExtensionKey, JsonValue> source = new LinkedHashMap<>();
        ExtensionKey second = new ExtensionKey("vendor:second");
        ExtensionKey first = new ExtensionKey("vendor:first");
        source.put(second, object);
        source.put(first, new JsonValue.StringValue("first"));
        Extensions extensions = new Extensions(source);
        json.clear();
        source.clear();

        assertEquals(List.of("z", "a"), new ArrayList<>(object.values().keySet()));
        assertEquals(List.of(second, first), new ArrayList<>(extensions.values().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> object.values().clear());
        assertThrows(UnsupportedOperationException.class, () -> extensions.values().clear());
    }

    @Test
    void jsonArraysAreImmutableSnapshotsAndRejectJavaNull() {
        ArrayList<JsonValue> source = new ArrayList<>();
        source.add(JsonValue.NullValue.INSTANCE);
        JsonValue.ArrayValue array = new JsonValue.ArrayValue(source);
        source.clear();

        assertEquals(List.of(JsonValue.NullValue.INSTANCE), array.values());
        assertThrows(UnsupportedOperationException.class, () -> array.values().clear());
        ArrayList<JsonValue> invalid = new ArrayList<>();
        invalid.add(null);
        assertThrows(NullPointerException.class, () -> new JsonValue.ArrayValue(invalid));
    }

    @Test
    void jsonNumbersUseSemanticDecimalEquality() {
        assertEquals(
                new JsonValue.NumberValue(new BigDecimal("100.00")),
                new JsonValue.NumberValue(new BigDecimal("1E+2")));
        assertEquals(
                new JsonValue.NumberValue(new BigDecimal("-0.0")),
                new JsonValue.NumberValue(BigDecimal.ZERO));
    }

    @Test
    void extensionKeysMustBeNamespacedAndContainersRejectNulls() {
        assertEquals("vendor.example:item-1", new ExtensionKey("vendor.example:item-1").value());
        assertThrows(IllegalArgumentException.class, () -> new ExtensionKey("notNamespaced"));
        assertThrows(IllegalArgumentException.class, () -> new ExtensionKey("Vendor:item"));

        Map<ExtensionKey, JsonValue> nullValue = new LinkedHashMap<>();
        nullValue.put(new ExtensionKey("vendor:item"), null);
        assertThrows(NullPointerException.class, () -> new Extensions(nullValue));

        Map<String, JsonValue> nullJsonKey = new LinkedHashMap<>();
        nullJsonKey.put(null, JsonValue.NullValue.INSTANCE);
        assertThrows(NullPointerException.class, () -> new JsonValue.ObjectValue(nullJsonKey));
    }
}
