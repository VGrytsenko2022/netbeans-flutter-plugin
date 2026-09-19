package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete pinned Flutter 3.44.8 RefreshProgressIndicator constructor. */
public final class RefreshProgressIndicatorWidgetPropertySchema {
    public static final WidgetTypeId REFRESH_PROGRESS_INDICATOR_TYPE =
            new WidgetTypeId("flutter.material.RefreshProgressIndicator");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 12;
    public static final int SLOT_COUNT = 0;

    public enum Group {
        PROGRESS("refreshProgressIndicatorProgress", "Progress", "Refresh progress and indeterminate animation."),
        APPEARANCE("refreshProgressIndicatorAppearance", "Appearance", "Refresh disk and arrow appearance."),
        LAYOUT("refreshProgressIndicatorLayout", "Layout", "Outer margin and inner indicator padding."),
        ACCESSIBILITY("refreshProgressIndicatorAccessibility", "Accessibility", "Refresh progress semantics.");

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

    public record Definition(Group group, String displayName, String description,
            String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative RefreshProgressIndicator property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private RefreshProgressIndicatorWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    /** Explicit null differs from omission only on this nullable numeric field. */
    public static boolean supportsExplicitNullNumber(PropertyName name) {
        return Objects.requireNonNull(name, "name").value().equals("strokeWidth");
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.PROGRESS, "Value",
                "Signed finite progress, displayed with Flutter's 0..1 clamping without rewriting the stored value. A number controls arrow growth/rotation; unset continues the SDK's indeterminate animation from its retained position. This is the visual indicator, not the RefreshIndicator scroll/pull-to-refresh controller.", 0);
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal ARGB or reviewed theme color for the circular Material disk. Unset uses ProgressIndicatorTheme.refreshBackgroundColor, then ThemeData.canvasColor; it is not the circular track color.", 1);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal/theme foreground color used when Value color is unset or yields null, then ProgressIndicatorTheme.color and ColorScheme.primary. Foreground alpha controls the arrow's opacity without fading the Material disk.", 2);
        add(values, "valueColor", Group.APPEARANCE, "Value color",
                "Stopped literal/theme color, explicit stopped null color, or a project reference with strict non-null Animation<Color?> type proof. Dynamic and nullable outer reference types are rejected. Isolated Canvas cannot execute project animations and reports their preview as unavailable.", 3);
        add(values, "strokeWidth", Group.APPEARANCE, "Stroke width",
                "Signed finite stroke width; zero retains SDK hairline behavior. Unset omits the argument and uses RefreshProgressIndicator.defaultStrokeWidth (2.5). Explicit null instead inherits ProgressIndicatorTheme.strokeWidth, then 4. This distinction survives save/reopen and Restore Default.", 4);
        add(values, "strokeAlign", Group.APPEARANCE, "Stroke alignment",
                "Signed finite relative stroke alignment: -1 inside, 0 centered, 1 outside. Values beyond -1..1 are permitted. Unset inherits ProgressIndicatorTheme.strokeAlign, then centered. Resolved nonfinite geometry is diagnosed by Canvas without clamping stored values.", 5);
        add(values, "semanticsLabel", Group.ACCESSIBILITY, "Semantics label",
                "Accessible purpose of this refresh indicator.", 6);
        add(values, "semanticsValue", Group.ACCESSIBILITY, "Semantics value",
                "Accessible value override. Determinate progress expects a number/percentage in 0..100, such as '45' or '45%'; indeterminate loadingSpinner allows free text. Canvas reports incompatible resolved semantics without rewriting the string.", 7);
        add(values, "strokeCap", Group.APPEARANCE, "Stroke cap",
                "Butt, round or square endings of the painted arc. Unset inherits the progress theme, then the SDK refresh painter's indeterminate square cap. The filled arrowhead retains its SDK shape.", 8);
        add(values, "elevation", Group.APPEARANCE, "Elevation",
                "Finite nonnegative elevation of the circular Material disk. Unset uses the SDK constructor default of 2; explicit zero removes elevation and is not omission.", 9);
        add(values, "indicatorMargin", Group.LAYOUT, "Indicator margin",
                "Nonnegative physical or directional outer margin around the 41-pixel SDK disk. Unset uses EdgeInsets.all(4). Parent constraints can still reduce the disk; geometry failures are reported without introducing a synthetic size.", 10);
        add(values, "indicatorPadding", Group.LAYOUT, "Indicator padding",
                "Nonnegative physical or directional padding inside the disk. Unset uses EdgeInsets.all(12). Asymmetric resolved padding can create a non-square arrow canvas, which the pinned SDK rejects while drawing a determinate arrow; Canvas reports that context-dependent limitation without rewriting padding.", 11);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("RefreshProgressIndicator property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String display, String description, int order) {
        if (values.put(name, new Definition(group, display, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate RefreshProgressIndicator property: " + name);
        }
    }
}
