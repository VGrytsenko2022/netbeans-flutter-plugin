package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlutterLocalNumberDraftPropertyTest {
    @Test void allNumericReferencePropertiesHaveValidLocalDrafts() {
        int checked = 0;
        for (var widget : BuiltInWidgetCatalog.getDefault().definitions()) {
            for (var definition : widget.properties()) {
                var binding = FlutterTypedPropertyEditors.binding(definition);
                if (binding.isEmpty() || binding.get().editorKind() != FlutterTypedPropertyEditors.EditorKind.NUMBER_REFERENCE) continue;
                var value = FlutterLocalDartReferenceEditorComponent.initialNumber(definition);
                assertTrue(value instanceof PropertyValue.IntegerValue || value instanceof PropertyValue.DoubleValue);
                assertTrue(definition.constraints().stream().anyMatch(c -> c.accepts(value)),
                        widget.typeId() + "." + definition.name());
                checked++;
            }
        }
        assertTrue(checked > 0, "The catalog must contain numeric-reference properties");
    }

    @Test void boundedDraftsRespectExclusiveDecimalsAndNegativeOnlyIntegers() {
        for (var constraint : List.of(
                new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, false, new BigDecimal("0.01"), false),
                new PropertyValueConstraint.DoubleRange(null, true, BigDecimal.ZERO, false),
                new PropertyValueConstraint.IntegerRange(BigInteger.valueOf(-10), BigInteger.valueOf(-2)))) {
            var definition = new PropertyDefinition(new PropertyName("value"), DartParameter.named(0, false),
                    List.of(constraint), Optional.empty());
            var value = FlutterLocalDartReferenceEditorComponent.initialNumber(definition);
            assertTrue(constraint.accepts(value), constraint.description());
        }
    }
}
