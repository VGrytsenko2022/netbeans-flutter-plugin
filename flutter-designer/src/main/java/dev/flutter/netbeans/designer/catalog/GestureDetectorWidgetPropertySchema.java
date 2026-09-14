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

/** Complete non-key GestureDetector constructor contract reviewed against Flutter 3.44.8. */
public final class GestureDetectorWidgetPropertySchema {
    public static final WidgetTypeId GESTURE_DETECTOR_TYPE =
            new WidgetTypeId("flutter.widgets.GestureDetector");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 64;
    public static final int CALLBACK_PROPERTY_COUNT = 58;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("gestureBehavior", "Behavior", "Hit testing, accessibility and accepted pointer devices."),
        TRACKPAD("gestureTrackpad", "Trackpad", "Trackpad scroll-to-scale behavior."),
        TAP("gestureTap", "Tap", "Primary, secondary and tertiary tap events."),
        DOUBLE_TAP("gestureDoubleTap", "Double tap", "Double-tap recognition events."),
        LONG_PRESS("gestureLongPress", "Long press", "Primary, secondary and tertiary long-press events."),
        DRAG("gestureDrag", "Drag and pan", "Directional drag and unrestricted pan events."),
        SCALE("gestureScale", "Scale", "Scale recognition events."),
        FORCE_PRESS("gestureForcePress", "Force press", "Pressure-sensitive pointer events.");

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

    private GestureDetectorWidgetPropertySchema() { }

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

    /** Mirrors the constructor assertion, not the stricter prose summary in its API documentation. */
    public static Optional<String> gestureConflict(WidgetNode node) {
        boolean pan = recognizer(node, "onPan");
        boolean scale = recognizer(node, "onScale");
        if (pan && scale) {
            return Optional.of("GestureDetector pan and scale start/update/end callbacks cannot be set together; use scale.");
        }
        if ((pan || scale) && recognizer(node, "onVerticalDrag") && recognizer(node, "onHorizontalDrag")) {
            return Optional.of("GestureDetector cannot combine vertical drag, horizontal drag and "
                    + (pan ? "pan" : "scale") + " start/update/end callbacks.");
        }
        return Optional.empty();
    }

    private static boolean recognizer(WidgetNode node, String prefix) {
        for (String suffix : List.of("Start", "Update", "End")) {
            PropertyValue value = node.properties().get(new PropertyName(prefix + suffix));
            if (value != null && !(value instanceof PropertyValue.NullValue)) return true;
        }
        return false;
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        callback(values, "onTapDown", Group.TAP, "GestureTapDownCallback", "TapDownDetails");
        callback(values, "onTapUp", Group.TAP, "GestureTapUpCallback", "TapUpDetails");
        callback(values, "onTap", Group.TAP, "GestureTapCallback", null);
        callback(values, "onTapMove", Group.TAP, "GestureTapMoveCallback", "TapMoveDetails");
        callback(values, "onTapCancel", Group.TAP, "GestureTapCancelCallback", null);
        callback(values, "onSecondaryTap", Group.TAP, "GestureTapCallback", null);
        callback(values, "onSecondaryTapDown", Group.TAP, "GestureTapDownCallback", "TapDownDetails");
        callback(values, "onSecondaryTapUp", Group.TAP, "GestureTapUpCallback", "TapUpDetails");
        callback(values, "onSecondaryTapCancel", Group.TAP, "GestureTapCancelCallback", null);
        callback(values, "onTertiaryTapDown", Group.TAP, "GestureTapDownCallback", "TapDownDetails");
        callback(values, "onTertiaryTapUp", Group.TAP, "GestureTapUpCallback", "TapUpDetails");
        callback(values, "onTertiaryTapCancel", Group.TAP, "GestureTapCancelCallback", null);
        callback(values, "onDoubleTapDown", Group.DOUBLE_TAP, "GestureTapDownCallback", "TapDownDetails");
        callback(values, "onDoubleTap", Group.DOUBLE_TAP, "GestureTapCallback", null);
        callback(values, "onDoubleTapCancel", Group.DOUBLE_TAP, "GestureTapCancelCallback", null);
        callback(values, "onLongPressDown", Group.LONG_PRESS, "GestureLongPressDownCallback", "LongPressDownDetails");
        callback(values, "onLongPressCancel", Group.LONG_PRESS, "GestureLongPressCancelCallback", null);
        callback(values, "onLongPress", Group.LONG_PRESS, "GestureLongPressCallback", null);
        callback(values, "onLongPressStart", Group.LONG_PRESS, "GestureLongPressStartCallback", "LongPressStartDetails");
        callback(values, "onLongPressMoveUpdate", Group.LONG_PRESS, "GestureLongPressMoveUpdateCallback", "LongPressMoveUpdateDetails");
        callback(values, "onLongPressUp", Group.LONG_PRESS, "GestureLongPressUpCallback", null);
        callback(values, "onLongPressEnd", Group.LONG_PRESS, "GestureLongPressEndCallback", "LongPressEndDetails");
        callback(values, "onSecondaryLongPressDown", Group.LONG_PRESS, "GestureLongPressDownCallback", "LongPressDownDetails");
        callback(values, "onSecondaryLongPressCancel", Group.LONG_PRESS, "GestureLongPressCancelCallback", null);
        callback(values, "onSecondaryLongPress", Group.LONG_PRESS, "GestureLongPressCallback", null);
        callback(values, "onSecondaryLongPressStart", Group.LONG_PRESS, "GestureLongPressStartCallback", "LongPressStartDetails");
        callback(values, "onSecondaryLongPressMoveUpdate", Group.LONG_PRESS, "GestureLongPressMoveUpdateCallback", "LongPressMoveUpdateDetails");
        callback(values, "onSecondaryLongPressUp", Group.LONG_PRESS, "GestureLongPressUpCallback", null);
        callback(values, "onSecondaryLongPressEnd", Group.LONG_PRESS, "GestureLongPressEndCallback", "LongPressEndDetails");
        callback(values, "onTertiaryLongPressDown", Group.LONG_PRESS, "GestureLongPressDownCallback", "LongPressDownDetails");
        callback(values, "onTertiaryLongPressCancel", Group.LONG_PRESS, "GestureLongPressCancelCallback", null);
        callback(values, "onTertiaryLongPress", Group.LONG_PRESS, "GestureLongPressCallback", null);
        callback(values, "onTertiaryLongPressStart", Group.LONG_PRESS, "GestureLongPressStartCallback", "LongPressStartDetails");
        callback(values, "onTertiaryLongPressMoveUpdate", Group.LONG_PRESS, "GestureLongPressMoveUpdateCallback", "LongPressMoveUpdateDetails");
        callback(values, "onTertiaryLongPressUp", Group.LONG_PRESS, "GestureLongPressUpCallback", null);
        callback(values, "onTertiaryLongPressEnd", Group.LONG_PRESS, "GestureLongPressEndCallback", "LongPressEndDetails");
        callback(values, "onVerticalDragDown", Group.DRAG, "GestureDragDownCallback", "DragDownDetails");
        callback(values, "onVerticalDragStart", Group.DRAG, "GestureDragStartCallback", "DragStartDetails");
        callback(values, "onVerticalDragUpdate", Group.DRAG, "GestureDragUpdateCallback", "DragUpdateDetails");
        callback(values, "onVerticalDragEnd", Group.DRAG, "GestureDragEndCallback", "DragEndDetails");
        callback(values, "onVerticalDragCancel", Group.DRAG, "GestureDragCancelCallback", null);
        callback(values, "onHorizontalDragDown", Group.DRAG, "GestureDragDownCallback", "DragDownDetails");
        callback(values, "onHorizontalDragStart", Group.DRAG, "GestureDragStartCallback", "DragStartDetails");
        callback(values, "onHorizontalDragUpdate", Group.DRAG, "GestureDragUpdateCallback", "DragUpdateDetails");
        callback(values, "onHorizontalDragEnd", Group.DRAG, "GestureDragEndCallback", "DragEndDetails");
        callback(values, "onHorizontalDragCancel", Group.DRAG, "GestureDragCancelCallback", null);
        callback(values, "onForcePressStart", Group.FORCE_PRESS, "GestureForcePressStartCallback", "ForcePressDetails");
        callback(values, "onForcePressPeak", Group.FORCE_PRESS, "GestureForcePressPeakCallback", "ForcePressDetails");
        callback(values, "onForcePressUpdate", Group.FORCE_PRESS, "GestureForcePressUpdateCallback", "ForcePressDetails");
        callback(values, "onForcePressEnd", Group.FORCE_PRESS, "GestureForcePressEndCallback", "ForcePressDetails");
        callback(values, "onPanDown", Group.DRAG, "GestureDragDownCallback", "DragDownDetails");
        callback(values, "onPanStart", Group.DRAG, "GestureDragStartCallback", "DragStartDetails");
        callback(values, "onPanUpdate", Group.DRAG, "GestureDragUpdateCallback", "DragUpdateDetails");
        callback(values, "onPanEnd", Group.DRAG, "GestureDragEndCallback", "DragEndDetails");
        callback(values, "onPanCancel", Group.DRAG, "GestureDragCancelCallback", null);
        callback(values, "onScaleStart", Group.SCALE, "GestureScaleStartCallback", "ScaleStartDetails");
        callback(values, "onScaleUpdate", Group.SCALE, "GestureScaleUpdateCallback", "ScaleUpdateDetails");
        callback(values, "onScaleEnd", Group.SCALE, "GestureScaleEndCallback", "ScaleEndDetails");
        property(values, "behavior", Group.BEHAVIOR, "Hit test behavior",
                "Omitted or null uses deferToChild with a child and translucent without a child.");
        property(values, "excludeFromSemantics", Group.BEHAVIOR, "Exclude from semantics",
                "Omission preserves false; true excludes these gesture actions from the semantics tree.");
        property(values, "dragStartBehavior", Group.BEHAVIOR, "Drag start behavior",
                "Omission preserves DragStartBehavior.start; select down to report the initial pointer position.");
        property(values, "trackpadScrollCausesScale", Group.TRACKPAD, "Trackpad scroll causes scale",
                "Omission preserves false; true converts trackpad scrolling into scaling.");
        property(values, "trackpadScrollToScaleFactor", Group.TRACKPAD, "Trackpad scroll to scale factor",
                "Finite signed Offset controlling scroll-to-scale conversion. Omission preserves Flutter's default Offset(0, -0.005).");
        property(values, "supportedDevices", Group.BEHAVIOR, "Supported devices",
                "Omitted or null accepts all pointer device kinds. An explicit empty set accepts none.");
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT
                || values.values().stream().filter(value -> value.callbackType().isPresent()).count() != CALLBACK_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("GestureDetector property inventory must contain 58 callbacks and 6 configuration properties");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void callback(Map<String, Definition> values, String name, Group group,
            String callbackType, String parameterType) {
        String display = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
        add(values, new Definition(group, display, callbackType
                + " callback. Omitted or null does not install this callback; handlers run only in the application, never in Canvas.",
                name, values.size() + 1, Optional.of(callbackType), Optional.ofNullable(parameterType)));
    }

    private static void property(Map<String, Definition> values, String name, Group group,
            String displayName, String description) {
        add(values, new Definition(group, displayName, description, name, values.size() + 1,
                Optional.empty(), Optional.empty()));
    }

    private static void add(Map<String, Definition> values, Definition definition) {
        if (values.putIfAbsent(definition.dartName(), definition) != null) {
            throw new IllegalArgumentException("Duplicate GestureDetector property " + definition.dartName());
        }
    }
}

