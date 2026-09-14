package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete non-key MouseRegion constructor contract reviewed against Flutter 3.44.8. */
public final class MouseRegionWidgetPropertySchema {
    public static final WidgetTypeId MOUSE_REGION_TYPE = new WidgetTypeId("flutter.widgets.MouseRegion");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 6;
    public static final int CALLBACK_PROPERTY_COUNT = 3;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("mouseRegionBehavior", "Behavior", "Mouse tracking opacity, cursor and hit testing."),
        MOUSE("mouseRegionMouse", "Mouse", "Mouse enter, exit and hover events.");

        private final String id;
        private final String displayName;
        private final String description;
        Group(String id, String displayName, String description) {
            this.id = id; this.displayName = displayName; this.description = description;
        }
        public String id() { return id; }
        public String setName() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description, String dartName,
            int dartOrder, Optional<String> callbackType, Optional<String> parameterType) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            Objects.requireNonNull(callbackType, "callbackType");
            Objects.requireNonNull(parameterType, "parameterType");
            if (dartOrder < 1) throw new IllegalArgumentException("Property order follows optional child");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private MouseRegionWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Map<String, Definition> callbackDefinitions() {
        LinkedHashMap<String, Definition> result = new LinkedHashMap<>();
        DEFINITIONS.forEach((name, definition) -> {
            if (definition.callbackType().isPresent()) result.put(name, definition);
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        callback(values, "onEnter", Group.MOUSE, "PointerEnterEventListener", "PointerEnterEvent");
        callback(values, "onExit", Group.MOUSE, "PointerExitEventListener", "PointerExitEvent");
        callback(values, "onHover", Group.MOUSE, "PointerHoverEventListener", "PointerHoverEvent");
        add(values, new Definition(Group.BEHAVIOR, "Cursor",
                "All 36 SystemMouseCursors, MouseCursor.defer/uncontrolled and three WidgetStateMouseCursor presets, or an analyzer-verified custom MouseCursor reference/factory. Omission preserves MouseCursor.defer; explicit null is not supported. Canvas never executes custom cursor code.",
                "cursor", values.size() + 1, Optional.empty(), Optional.empty()));
        add(values, new Definition(Group.BEHAVIOR, "Opaque",
                "Omission preserves true. False lets MouseRegions behind this region also detect the mouse; this is independent of hit test behavior.",
                "opaque", values.size() + 1, Optional.empty(), Optional.empty()));
        add(values, new Definition(Group.BEHAVIOR, "Hit test behavior",
                "Omitted or null preserves Flutter's effective HitTestBehavior.opaque. This controls hit testing separately from mouse tracking opacity.",
                "hitTestBehavior", values.size() + 1, Optional.empty(), Optional.empty()));
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT
                || values.values().stream().filter(value -> value.callbackType().isPresent()).count() != CALLBACK_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("MouseRegion property inventory must contain 3 callbacks and 3 configuration properties");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void callback(Map<String, Definition> values, String name, Group group,
            String callbackType, String parameterType) {
        String display = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
        add(values, new Definition(group, display, callbackType
                + " callback. Omitted or null installs no handler; user handlers run in the application, never in Canvas."
                + (name.equals("onExit") ? " Flutter does not call onExit when the region is removed or unmounted." : ""),
                name, values.size() + 1, Optional.of(callbackType), Optional.of(parameterType)));
    }

    private static void add(Map<String, Definition> values, Definition definition) {
        if (values.putIfAbsent(definition.dartName(), definition) != null) {
            throw new IllegalArgumentException("Duplicate MouseRegion property " + definition.dartName());
        }
    }
}
