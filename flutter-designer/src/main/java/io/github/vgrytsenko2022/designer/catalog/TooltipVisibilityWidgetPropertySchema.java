package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete pinned Flutter 3.44.8 TooltipVisibility constructor, excluding managed key. */
public final class TooltipVisibilityWidgetPropertySchema {
    public static final WidgetTypeId TOOLTIP_VISIBILITY_TYPE =
            new WidgetTypeId("flutter.material.TooltipVisibility");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        VISIBILITY("tooltipVisibilityBehavior", "Visibility",
                "Inherited visibility policy for descendant Material tooltips, without hiding their anchors or child semantics.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("dartOrder must be non-negative");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = Map.of("visible", new Definition(
            Group.VISIBILITY, "Visible",
            "Required boolean; Designer creates an explicit true value, not a Flutter constructor default. "
                    + "False disables visual tooltips inherited from this scope for tap, long press, mouse hover and programmatic visibility. "
                    + "The closest TooltipVisibility wins: a nested true scope overrides an outer false scope, not an AND of ancestors. "
                    + "The anchor stays laid out, painted and interactive, with its child semantics available. "
                    + "Toggling can change the SDK's internal child topology; arbitrary child runtime state or focus retention is not guaranteed. "
                    + "The pinned SDK removes RawTooltip while disabled; do not rely on preserving its own tooltip-message semantic annotation. "
                    + "This controls descendant Material Tooltip policy; it is not a controlled visible state or an event of one tooltip.",
            "visible", 0));

    private TooltipVisibilityWidgetPropertySchema() { }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
