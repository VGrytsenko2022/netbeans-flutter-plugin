package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 material, adaptive and noSpinner constructors. */
public final class RefreshIndicatorWidgetPropertySchema {
    public static final WidgetTypeId REFRESH_INDICATOR_TYPE =
            new WidgetTypeId("flutter.material.RefreshIndicator");
    /** Twelve SDK scalar fields across the constructors and one Designer selector. */
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 13;
    public static final int SLOT_COUNT = 1;
    private static final List<String> SPINNER_ONLY = List.of(
            "displacement", "edgeOffset", "color", "backgroundColor", "strokeWidth");

    public enum Group {
        BEHAVIOR("refreshIndicatorBehavior", "Behavior", "Constructor, trigger and scroll notification handling."),
        APPEARANCE("refreshIndicatorAppearance", "Appearance", "Refresh spinner colors, stroke and elevation."),
        LAYOUT("refreshIndicatorLayout", "Layout", "Overlay offset and settling displacement."),
        CALLBACKS("refreshIndicatorCallbacks", "Callbacks", "Strictly typed project callback references."),
        ACCESSIBILITY("refreshIndicatorAccessibility", "Accessibility", "Refresh indicator accessibility text.");

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
            if (dartOrder < 0) throw new IllegalArgumentException("Negative RefreshIndicator property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private RefreshIndicatorWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static List<String> notificationPredicatePresets() {
        return List.of("default", "depthZero", "all");
    }

    public static List<String> spinnerOnlyProperties() { return SPINNER_ONLY; }

    public static boolean isNoSpinner(WidgetNode node) {
        return node.type().equals(REFRESH_INDICATOR_TYPE)
                && new PropertyValue.StringValue("noSpinner").equals(node.properties().get(new PropertyName("variant")));
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "displacement", Group.LAYOUT, "Displacement",
                "Nonnegative finite padding from the top or bottom edge offset to the settled spinner. Unset uses 40. Material and Adaptive only; setting this in No spinner switches to Material.", 0);
        add(values, "edgeOffset", Group.LAYOUT, "Edge offset",
                "Signed finite top/bottom overlay offset; negative values are retained. Unset uses zero. Material and Adaptive only.", 1);
        add(values, "onRefresh", Group.CALLBACKS, "On refresh",
                "Project reference with strict non-null RefreshCallback (Future<void> Function()) type proof. Unset generates the required async no-op callback, which completes immediately; it does not load data. Isolated Canvas never executes project callbacks.", 2);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal ARGB or theme role for the spinner; unset uses the current theme. Material and Adaptive only. Adaptive Apple platforms use the Cupertino spinner color.", 3);
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal ARGB or theme role for the Material disk; unset uses ThemeData.canvasColor. Not accepted by No spinner; ignored by the Adaptive Apple spinner.", 4);
        add(values, "notificationPredicate", Group.BEHAVIOR, "Notification predicate",
                "Default or Depth zero accepts notifications at depth zero; All accepts every depth. A project reference requires strict non-null ScrollNotificationPredicate type proof. Unset preserves the SDK default. Canvas cannot execute a custom predicate.", 5);
        add(values, "semanticsLabel", Group.ACCESSIBILITY, "Semantics label",
                "Material refresh purpose; unset uses the localized refresh label. Empty text suppresses the label. Adaptive Apple and No spinner have no Material spinner semantics.", 6);
        add(values, "semanticsValue", Group.ACCESSIBILITY, "Semantics value",
                "Material accessible progress value. During determinate dragging the SDK requires a number or percentage in 0..100, such as '45%'; free text can assert at runtime. Adaptive Apple and No spinner do not display Material spinner semantics.", 7);
        add(values, "strokeWidth", Group.APPEARANCE, "Stroke width",
                "Signed finite Material arc stroke width, with zero retaining hairline behavior. Unset uses 2.5, not the stale 2.0 SDK field comment. Explicit null is invalid. Not accepted by No spinner and ignored by the Adaptive Apple spinner.", 8);
        add(values, "triggerMode", Group.BEHAVIOR, "Trigger mode",
                "On edge (default) or Anywhere when a vertical descendant is dragged. Short contents need AlwaysScrollableScrollPhysics on the descendant; this wrapper does not invent scrolling physics.", 9);
        add(values, "elevation", Group.APPEARANCE, "Elevation",
                "Nonnegative finite disk elevation; unset uses 2. Accepted by every constructor, but the Adaptive Apple and No spinner branches do not paint the Material disk.", 10);
        add(values, "onStatusChange", Group.CALLBACKS, "On status change",
                "No spinner only: strict non-null ValueChanged<RefreshIndicatorStatus?> project callback. Receives drag, armed, snap, refresh, done, canceled and null on return to idle. Setting it switches to No spinner and resets spinner-only fields. Canvas never executes project callbacks.", 12);
        add(values, "variant", Group.BEHAVIOR, "Constructor",
                "Required Material, Adaptive or No spinner selector. No spinner clears its five unavailable visual arguments; leaving No spinner clears On status change. Child and shared settings are retained. Never emitted as a Dart argument; cannot be reset.", 13);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("RefreshIndicator property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int order) {
        if (values.put(name, new Definition(group, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate RefreshIndicator property: " + name);
        }
    }
}
