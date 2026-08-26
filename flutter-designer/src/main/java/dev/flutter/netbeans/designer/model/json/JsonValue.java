package dev.flutter.netbeans.designer.model.json;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deeply immutable JSON value used to preserve namespaced extension data
 * without exposing a particular JSON library in the designer model API.
 */
public sealed interface JsonValue permits
        JsonValue.NullValue,
        JsonValue.BooleanValue,
        JsonValue.NumberValue,
        JsonValue.StringValue,
        JsonValue.ArrayValue,
        JsonValue.ObjectValue {

    enum NullValue implements JsonValue {
        INSTANCE
    }

    record BooleanValue(boolean value) implements JsonValue {
    }

    record NumberValue(BigDecimal value) implements JsonValue {
        public NumberValue {
            Objects.requireNonNull(value, "value");
            value = value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        }
    }

    record StringValue(String value) implements JsonValue {
        public StringValue {
            Objects.requireNonNull(value, "value");
        }
    }

    record ArrayValue(List<JsonValue> values) implements JsonValue {
        public ArrayValue {
            Objects.requireNonNull(values, "values");
            values = List.copyOf(values);
        }
    }

    record ObjectValue(Map<String, JsonValue> values) implements JsonValue {
        public ObjectValue {
            Objects.requireNonNull(values, "values");
            LinkedHashMap<String, JsonValue> copy = new LinkedHashMap<>(values.size());
            values.forEach((key, value) -> copy.put(
                    Objects.requireNonNull(key, "JSON object key"),
                    Objects.requireNonNull(value, "JSON object value")));
            values = Collections.unmodifiableMap(copy);
        }
    }
}
