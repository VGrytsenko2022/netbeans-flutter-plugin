package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Closed, writable projection of the scalar {@code Scaffold} constructor
 * surface supported by the Designer model.
 *
 * <p>The three preset properties are intentionally strings rather than opaque
 * Dart expressions. Generation and the native Canvas resolve only the reviewed
 * public static constants listed here. Widget-valued constructor parameters,
 * and {@code BoxDecoration} remain outside this property schema because they
 * require slots or structured graphs. The bottom-sheet scrim builder is an
 * exact analyzer-verified project function reference, never an executed preview callback.</p>
 */
public final class ScaffoldWidgetPropertySchema {
    public static final WidgetTypeId SCAFFOLD_TYPE =
            new WidgetTypeId("flutter.material.Scaffold");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 18;
    public static final String BOTTOM_SHEET_SCRIM_BUILDER_TYPE = "Widget? Function(BuildContext, Animation<double>)";
    public static final int SLOT_COUNT = 3;

    public enum Group {
        LAYOUT("scaffoldLayout", "Layout",
                "Body extension, insets, footer alignment, and drawer edge geometry."),
        FLOATING_ACTION_BUTTON("scaffoldFab", "Floating action button",
                "Reviewed location and motion presets for the existing floating-action slot."),
        APPEARANCE("scaffoldAppearance", "Appearance",
                "Theme-aware scaffold and drawer-scrim colors."),
        DRAWER_BEHAVIOR("scaffoldDrawerBehavior", "Drawer behavior",
                "Drawer gestures, dismissal, drag start, and lifecycle callbacks."),
        RESTORATION("scaffoldRestoration", "State restoration",
                "Stable restoration identity for drawer state.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return setName;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    public enum Target {
        DIRECT,
        FLOATING_ACTION_BUTTON_LOCATION,
        FLOATING_ACTION_BUTTON_ANIMATOR,
        PERSISTENT_FOOTER_ALIGNMENT
    }

    public record Definition(
            Group group,
            String displayName,
            String description,
            Target target,
            String dartType,
            List<String> presets) {

        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            Objects.requireNonNull(target, "target");
            dartType = Objects.requireNonNull(dartType, "dartType");
            presets = List.copyOf(Objects.requireNonNull(presets, "presets"));
            if (target == Target.DIRECT) {
                if (!dartType.isEmpty() || !presets.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Direct Scaffold properties cannot declare static presets");
                }
            } else if (dartType.isBlank() || presets.isEmpty()
                    || presets.stream().anyMatch(String::isBlank)
                    || presets.stream().distinct().count() != presets.size()) {
                throw new IllegalArgumentException(
                        "Static Scaffold presets require a Dart type and unique values");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private ScaffoldWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static boolean isStaticPreset(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();

        preset(values, "floatingActionButtonLocation", Group.FLOATING_ACTION_BUTTON,
                "Location", "Public Flutter FloatingActionButtonLocation preset.",
                Target.FLOATING_ACTION_BUTTON_LOCATION, "FloatingActionButtonLocation",
                List.of(
                        "startTop", "miniStartTop", "centerTop", "miniCenterTop",
                        "endTop", "miniEndTop", "startFloat", "miniStartFloat",
                        "centerFloat", "miniCenterFloat", "endFloat", "miniEndFloat",
                        "startDocked", "miniStartDocked", "centerDocked",
                        "miniCenterDocked", "endDocked", "miniEndDocked", "endContained"));
        preset(values, "floatingActionButtonAnimator", Group.FLOATING_ACTION_BUTTON,
                "Animator", "Public Flutter FloatingActionButtonAnimator preset.",
                Target.FLOATING_ACTION_BUTTON_ANIMATOR, "FloatingActionButtonAnimator",
                List.of("scaling", "noAnimation"));
        preset(values, "persistentFooterAlignment", Group.LAYOUT,
                "Footer alignment", "Directional alignment of persistent footer buttons.",
                Target.PERSISTENT_FOOTER_ALIGNMENT, "AlignmentDirectional",
                List.of(
                        "topStart", "topCenter", "topEnd", "centerStart", "center",
                        "centerEnd", "bottomStart", "bottomCenter", "bottomEnd"));

        direct(values, "backgroundColor", Group.APPEARANCE,
                "Background color", "Literal or semantic theme scaffold background color.");
        direct(values, "drawerScrimColor", Group.APPEARANCE,
                "Drawer scrim color", "Literal or semantic theme color behind an open drawer.");
        direct(values, "resizeToAvoidBottomInset", Group.LAYOUT,
                "Avoid bottom inset", "Resize body and floating widgets around the keyboard.");
        direct(values, "primary", Group.LAYOUT,
                "Primary", "Treat this Scaffold as the top-level primary screen.");
        direct(values, "extendBody", Group.LAYOUT,
                "Extend body", "Extend the body behind bottom scaffold widgets.");
        direct(values, "extendBodyBehindAppBar", Group.LAYOUT,
                "Extend behind app bar", "Extend the body behind the app-bar slot.");
        direct(values, "drawerEdgeDragWidth", Group.LAYOUT,
                "Drawer edge drag width", "Non-negative logical width of the drawer swipe edge.");

        direct(values, "drawerDragStartBehavior", Group.DRAWER_BEHAVIOR,
                "Drag start behavior", "Flutter DragStartBehavior: down or start.");
        direct(values, "drawerBarrierDismissible", Group.DRAWER_BEHAVIOR,
                "Barrier dismissible", "Allow a barrier tap to close an open drawer.");
        direct(values, "drawerEnableOpenDragGesture", Group.DRAWER_BEHAVIOR,
                "Open drawer by drag", "Enable the start-side drawer drag gesture.");
        direct(values, "endDrawerEnableOpenDragGesture", Group.DRAWER_BEHAVIOR,
                "Open end drawer by drag", "Enable the end-side drawer drag gesture.");
        direct(values, "onDrawerChanged", Group.DRAWER_BEHAVIOR,
                "Drawer changed", "Strict Dart callback identifier receiving the open state.");
        direct(values, "onEndDrawerChanged", Group.DRAWER_BEHAVIOR,
                "End drawer changed", "Strict Dart callback identifier receiving the open state.");

        direct(values, "restorationId", Group.RESTORATION,
                "Restoration ID", "Non-empty stable ID used to restore drawer state.");
        direct(values, "bottomSheetScrimBuilder", Group.APPEARANCE,
                "Bottom sheet scrim builder", "Optional non-null " + BOTTOM_SHEET_SCRIM_BUILDER_TYPE
                        + " project reference. Omission preserves Flutter's default animated scrim. The callback itself cannot be null; returning null produces no scrim. References, members/getters and zero-argument factories require strict analyzer proof. Project callbacks never run in Canvas.");

        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Scaffold schema must expose exactly " + CONSTRUCTOR_PROPERTY_COUNT
                    + " properties; actual=" + values.size());
        }
        return Map.copyOf(values);
    }

    private static void direct(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description) {
        add(values, name, new Definition(
                group, displayName, description, Target.DIRECT, "", List.of()));
    }

    private static void preset(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            String dartType,
            List<String> presets) {
        add(values, name, new Definition(
                group, displayName, description, target, dartType, presets));
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Definition definition) {
        if (values.putIfAbsent(name, definition) != null) {
            throw new IllegalStateException("Duplicate Scaffold property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
