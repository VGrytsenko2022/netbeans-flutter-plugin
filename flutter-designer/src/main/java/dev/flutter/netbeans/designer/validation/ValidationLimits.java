package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.WidgetDefinition;

/** Resource limits applied to one in-memory designer validation pass. */
public record ValidationLimits(
        int maxDepth,
        int maxNodes,
        int maxPropertiesPerWidget,
        int maxSlotsPerWidget,
        int maxIssues) {

    public static final int DEFAULT_MAX_DEPTH = 256;
    public static final int DEFAULT_MAX_NODES = 10_000;
    public static final int DEFAULT_MAX_PROPERTIES_PER_WIDGET = 512;
    public static final int DEFAULT_MAX_SLOTS_PER_WIDGET = WidgetDefinition.MAX_SLOTS;
    public static final int DEFAULT_MAX_ISSUES = 1_000;

    public ValidationLimits {
        requirePositive(maxDepth, "maxDepth");
        requirePositive(maxNodes, "maxNodes");
        requirePositive(maxPropertiesPerWidget, "maxPropertiesPerWidget");
        requirePositive(maxSlotsPerWidget, "maxSlotsPerWidget");
        if (maxIssues < 2) {
            throw new IllegalArgumentException("maxIssues must be at least 2.");
        }
    }

    public static ValidationLimits defaults() {
        return new ValidationLimits(
                DEFAULT_MAX_DEPTH,
                DEFAULT_MAX_NODES,
                DEFAULT_MAX_PROPERTIES_PER_WIDGET,
                DEFAULT_MAX_SLOTS_PER_WIDGET,
                DEFAULT_MAX_ISSUES);
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
