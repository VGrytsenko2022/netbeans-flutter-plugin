package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogSchemaContractTest {
    @Test
    void valueKindsRetainTheExactSchemaVersionOneDiscriminators() {
        assertEquals(List.of(
                "string", "boolean", "integer", "double", "enum", "color",
                "edgeInsets", "asset", "callback", "dartExpression"),
                List.of(PropertyValueKind.values()).stream().map(PropertyValueKind::wireName).toList());
    }

    @Test
    void slotCardinalitiesRetainTheExactSchemaVersionOneDiscriminators() {
        assertEquals(List.of("single", "list"),
                List.of(SlotCardinality.values()).stream().map(SlotCardinality::wireName).toList());
    }
}
