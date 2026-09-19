package io.github.vgrytsenko2022.designer.model;

import io.github.vgrytsenko2022.designer.model.json.JsonValue;
import java.util.Map;

/** Immutable namespaced extension data not owned by the core schema. */
public record Extensions(Map<ExtensionKey, JsonValue> values) {
    private static final Extensions EMPTY = new Extensions(Map.of());

    public Extensions {
        values = ModelConstraints.immutableLinkedMap(values, "extension values");
    }

    public static Extensions empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
