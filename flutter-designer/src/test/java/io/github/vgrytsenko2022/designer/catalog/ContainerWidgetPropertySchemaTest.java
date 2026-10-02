package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerWidgetPropertySchemaTest {

    @Test
    void exposesExactlyThirteenConstructorPropertiesInFlutterOrder() {
        assertEquals(13, ContainerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, ContainerWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of(
                "alignment", "padding", "color", "isAntiAlias", "decoration",
                "foregroundDecoration", "width", "height", "constraints", "margin",
                "transform", "transformAlignment", "clipBehavior"),
                List.copyOf(ContainerWidgetPropertySchema.definitions().keySet()));
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13),
                ContainerWidgetPropertySchema.definitions().values().stream()
                        .map(ContainerWidgetPropertySchema.Definition::dartOrder)
                        .toList());
        assertTrue(ContainerWidgetPropertySchema.definitions().values().stream()
                .allMatch(value -> !value.displayName().isBlank()
                        && !value.description().isBlank()
                        && !value.dartName().isBlank()));
    }

    @Test
    void findsOnlyReviewedContainerProperties() {
        assertTrue(ContainerWidgetPropertySchema.find(
                new PropertyName("decoration")).isPresent());
        assertTrue(ContainerWidgetPropertySchema.find(
                new PropertyName("transformAlignment")).isPresent());
        assertFalse(ContainerWidgetPropertySchema.find(
                new PropertyName("child")).isPresent());
        assertFalse(ContainerWidgetPropertySchema.find(
                new PropertyName("key")).isPresent());
    }
}
