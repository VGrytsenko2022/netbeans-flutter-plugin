package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete non-key Listener constructor contract reviewed against Flutter 3.44.8. */
public final class ListenerWidgetPropertySchema {
    public static final WidgetTypeId LISTENER_TYPE = new WidgetTypeId("flutter.widgets.Listener");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 10;
    public static final int CALLBACK_PROPERTY_COUNT = 9;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("listenerBehavior", "Behavior", "Raw pointer hit testing."),
        POINTER("listenerPointer", "Pointer", "Raw pointer contact, movement, hover and signal events."),
        TRACKPAD("listenerTrackpad", "Trackpad", "Raw pointer pan/zoom events, before gesture recognition.");

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

    private ListenerWidgetPropertySchema() { }

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
        callback(values, "onPointerDown", Group.POINTER, "PointerDownEventListener", "PointerDownEvent");
        callback(values, "onPointerMove", Group.POINTER, "PointerMoveEventListener", "PointerMoveEvent");
        callback(values, "onPointerUp", Group.POINTER, "PointerUpEventListener", "PointerUpEvent");
        callback(values, "onPointerHover", Group.POINTER, "PointerHoverEventListener", "PointerHoverEvent");
        callback(values, "onPointerCancel", Group.POINTER, "PointerCancelEventListener", "PointerCancelEvent");
        callback(values, "onPointerPanZoomStart", Group.TRACKPAD, "PointerPanZoomStartEventListener", "PointerPanZoomStartEvent");
        callback(values, "onPointerPanZoomUpdate", Group.TRACKPAD, "PointerPanZoomUpdateEventListener", "PointerPanZoomUpdateEvent");
        callback(values, "onPointerPanZoomEnd", Group.TRACKPAD, "PointerPanZoomEndEventListener", "PointerPanZoomEndEvent");
        callback(values, "onPointerSignal", Group.POINTER, "PointerSignalEventListener", "PointerSignalEvent");
        add(values, new Definition(Group.BEHAVIOR, "Hit test behavior",
                "Omission preserves HitTestBehavior.deferToChild, with or without a child. Explicit null is not supported.",
                "behavior", values.size() + 1, Optional.empty(), Optional.empty()));
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT
                || values.values().stream().filter(value -> value.callbackType().isPresent()).count() != CALLBACK_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Listener property inventory must contain 9 callbacks and 1 behavior property");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void callback(Map<String, Definition> values, String name, Group group,
            String callbackType, String parameterType) {
        String display = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
        add(values, new Definition(group, display, callbackType
                + " callback. Omitted or null installs no handler; user handlers run in the application, never in Canvas.",
                name, values.size() + 1, Optional.of(callbackType), Optional.of(parameterType)));
    }

    private static void add(Map<String, Definition> values, Definition definition) {
        if (values.putIfAbsent(definition.dartName(), definition) != null) {
            throw new IllegalArgumentException("Duplicate Listener property " + definition.dartName());
        }
    }
}
