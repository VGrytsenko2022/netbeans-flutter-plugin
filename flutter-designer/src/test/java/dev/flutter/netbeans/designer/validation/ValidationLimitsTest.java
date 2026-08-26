package dev.flutter.netbeans.designer.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import org.junit.jupiter.api.Test;

class ValidationLimitsTest {

    @Test
    void exposesTheAcceptedDefaultLimits() {
        ValidationLimits limits = ValidationLimits.defaults();

        assertAll(
                () -> assertEquals(256, limits.maxDepth()),
                () -> assertEquals(10_000, limits.maxNodes()),
                () -> assertEquals(WidgetDefinition.MAX_PROPERTIES,
                        limits.maxPropertiesPerWidget()),
                () -> assertEquals(WidgetDefinition.MAX_SLOTS,
                        limits.maxSlotsPerWidget()),
                () -> assertEquals(1_000, limits.maxIssues()));
    }

    @Test
    void rejectsNonPositiveResourceLimitsAndAnUnusableIssueLimit() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ValidationLimits(0, 1, 1, 1, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ValidationLimits(1, 0, 1, 1, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ValidationLimits(1, 1, 0, 1, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ValidationLimits(1, 1, 1, 0, 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ValidationLimits(1, 1, 1, 1, 1)));
    }
}
