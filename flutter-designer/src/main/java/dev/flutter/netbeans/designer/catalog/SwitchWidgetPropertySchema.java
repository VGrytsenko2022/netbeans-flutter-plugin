package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 Switch constructors and closed state-property projections. */
public final class SwitchWidgetPropertySchema {
    public static final WidgetTypeId SWITCH_TYPE = new WidgetTypeId("flutter.material.Switch");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 201;
    public static final int DIRECT_PROPERTY_COUNT = 30;
    public static final int SLOT_COUNT = 0;
    private static final List<String> COLOR_FAMILIES = List.of(
            "thumbColor", "trackColor", "trackOutlineColor", "overlayColor");
    private static final List<String> ICON_PARTS = List.of("Data", "Size", "Color", "Shadows",
            "BlendMode", "Fill", "Weight", "Grade", "OpticalSize", "FontWeight",
            "SemanticLabel", "TextDirection", "ApplyTextScaling");

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"), IMAGES("Images"),
        THUMB_COLOR("ThumbColor"), TRACK_COLOR("TrackColor"), TRACK_OUTLINE_COLOR("TrackOutlineColor"),
        OVERLAY_COLOR("OverlayColor"), TRACK_OUTLINE_WIDTH("TrackOutlineWidth"), THUMB_ICON("ThumbIcon");

        private final String suffix;

        Group(String suffix) {
            this.suffix = suffix;
        }

        public String setName() {
            return "switch" + suffix;
        }

        public String displayName() {
            return suffix.replaceAll("([a-z])([A-Z])", "$1 $2");
        }

        public String description() {
            return "Flutter Switch " + displayName().toLowerCase(java.util.Locale.ROOT) + ".";
        }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative Switch property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private SwitchWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name).value());
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name)));
    }

    public static List<String> variants() {
        return List.of("standard", "adaptive");
    }

    public static List<String> statePrefixes() {
        return CheckboxWidgetPropertySchema.statePrefixes();
    }

    /** Presence-aware first-match priority, including explicit null, with no implicit disabled entry. */
    public static List<String> statePriority() {
        return CheckboxWidgetPropertySchema.statePriority();
    }

    public static List<String> colorFamilies() {
        return COLOR_FAMILIES;
    }

    public static List<String> colorStateProperties(String family) {
        if (!COLOR_FAMILIES.contains(family)) {
            throw new IllegalArgumentException("Not a Switch state-color family: " + family);
        }
        return statePrefixes().stream().map(state -> family + state).toList();
    }

    public static List<String> outlineWidthStateProperties() {
        return statePrefixes().stream().map(state -> "trackOutlineWidth" + state).toList();
    }

    public static List<String> thumbIconStates() {
        return statePrefixes();
    }

    /** Capitalized state suffix; first entry is Mode followed by the thirteen Icon fields. */
    public static List<String> thumbIconBucketProperties(String state) {
        if (!statePrefixes().contains(state)) {
            throw new IllegalArgumentException("Not a Switch thumb-icon state: " + state);
        }
        return java.util.stream.Stream.concat(java.util.stream.Stream.of("thumbIcon" + state + "Mode"),
                ICON_PARTS.stream().map(part -> "thumbIcon" + state + part)).toList();
    }

    public static List<String> thumbIconLocalProperties() {
        return thumbIconStates().stream().flatMap(state -> thumbIconBucketProperties(state).stream()).toList();
    }

    /** Original Icon property name, excluding the synthetic Mode selector. */
    public static Optional<String> iconSourceName(PropertyName name) {
        return iconSourceName(Objects.requireNonNull(name).value());
    }

    public static Optional<String> iconSourceName(String name) {
        for (String state : thumbIconStates()) {
            String prefix = "thumbIcon" + state;
            if (name.startsWith(prefix)) {
                String part = name.substring(prefix.length());
                if (ICON_PARTS.contains(part)) {
                    return Optional.of(part.equals("Data") ? "icon" : lowerFirst(part));
                }
            }
        }
        return Optional.empty();
    }

    public static List<String> mouseCursorPresets() {
        return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.BEHAVIOR, "Required controlled switch value, created false. The widget never changes the stored value itself.", 0);
        add(values, "onChanged", Group.BEHAVIOR, "Strict ValueChanged<bool> reference. Enabled without a reference emits a benign no-op; disabled emits null while preserving metadata. Canvas never executes project callbacks.", 1);
        add(values, "activeColor", Group.APPEARANCE, "Deprecated SDK fallback retained exactly: Standard and adaptive non-Apple use it for active thumb; adaptive Apple uses it for active track. Prefer the explicit active colors.", 2);
        add(values, "activeThumbColor", Group.APPEARANCE, "Literal or semantic active thumb fallback; explicit state-color resolution takes precedence.", 3);
        add(values, "activeTrackColor", Group.APPEARANCE, "Literal or semantic active track fallback; explicit state-color resolution takes precedence.", 4);
        add(values, "inactiveThumbColor", Group.APPEARANCE, "Literal or semantic inactive thumb fallback; unset preserves the actual SDK/theme behavior.", 5);
        add(values, "inactiveTrackColor", Group.APPEARANCE, "Literal or semantic inactive track fallback; unset preserves the actual SDK/theme behavior.", 6);
        add(values, "activeThumbImage", Group.IMAGES, "Typed asset/package/exact/resize ImageProvider. Unset uses no local image; resetting also clears its error callback.", 7);
        add(values, "onActiveThumbImageError", Group.IMAGES, "Strict non-null ImageErrorListener reference or zero-argument factory returning that handler, with signature void Function(Object, StackTrace?). Requires Active thumb image; Canvas never executes it.", 8);
        add(values, "inactiveThumbImage", Group.IMAGES, "Typed asset/package/exact/resize ImageProvider. Unset uses no local image; resetting also clears its error callback.", 9);
        add(values, "onInactiveThumbImageError", Group.IMAGES, "Strict non-null ImageErrorListener reference or zero-argument factory returning that handler, with signature void Function(Object, StackTrace?). Requires Inactive thumb image; Canvas never executes it.", 10);
        add(values, "thumbColor", Group.THUMB_COLOR, stateReferenceHelp("Color", "thumb color"), 11);
        add(values, "trackColor", Group.TRACK_COLOR, stateReferenceHelp("Color", "track color"), 12);
        add(values, "trackOutlineColor", Group.TRACK_OUTLINE_COLOR, stateReferenceHelp("Color", "track outline color"), 13);
        add(values, "trackOutlineWidth", Group.TRACK_OUTLINE_WIDTH, stateReferenceHelp("double", "track outline width"), 14);
        add(values, "thumbIcon", Group.THUMB_ICON, stateReferenceHelp("Icon", "thumb icon"), 15);
        add(values, "materialTapTargetSize", Group.LAYOUT, "Padded or shrinkWrap; unset preserves SwitchTheme and actual platform/configuration defaults.", 16);
        add(values, "dragStartBehavior", Group.BEHAVIOR, "DragStartBehavior start or down; unset uses start.", 17);
        add(values, "mouseCursor", Group.BEHAVIOR, "All 41 reviewed cursor constants or a strict MouseCursor reference, including WidgetStateMouseCursor subtypes.", 18);
        add(values, "focusColor", Group.APPEARANCE, "Focused overlay fallback after explicit overlay resolution.", 19);
        add(values, "hoverColor", Group.APPEARANCE, "Hovered overlay fallback after explicit overlay resolution.", 20);
        add(values, "overlayColor", Group.OVERLAY_COLOR, stateReferenceHelp("Color", "overlay color"), 21);
        add(values, "splashRadius", Group.APPEARANCE, "Signed finite reaction radius or positive Infinity. Zero/negative suppresses the reaction; unset preserves SDK/theme defaults.", 22);
        add(values, "focusNode", Group.BEHAVIOR, "Strict FocusNode reference; Canvas keeps local focus ownership without executing project code.", 23);
        add(values, "onFocusChange", Group.BEHAVIOR, "Strict ValueChanged<bool> reference; Canvas never executes project code.", 24);
        add(values, "autofocus", Group.BEHAVIOR, "Unset uses false.", 25);
        add(values, "padding", Group.LAYOUT, "Non-negative physical or directional EdgeInsetsGeometry; unset preserves SDK/theme defaults.", 26);
        add(values, "applyCupertinoTheme", Group.BEHAVIOR, "Adaptive constructor only: true/false or explicit null (the SDK's theme-driven mode). Unset also uses null. Standard does not accept this argument.", 27);
        add(values, "variant", Group.BEHAVIOR, "Required constructor selector, created Standard. Standard and Adaptive are both const-capable; adaptive Apple changes configuration, not the public state-property contract.", 28);
        add(values, "enabled", Group.BEHAVIOR, "Required activation policy, created true. False emits onChanged:null while retaining the callback metadata.", 29);
        int order = DIRECT_PROPERTY_COUNT;
        for (String family : COLOR_FAMILIES) {
            Group group = values.get(family).group();
            for (String name : colorStateProperties(family)) {
                add(values, name, group, "Literal color, semantic role or explicit null. " + stateHelp(), order++);
            }
        }
        for (String name : outlineWidthStateProperties()) {
            add(values, name, Group.TRACK_OUTLINE_WIDTH, "Signed finite outline width, positive Infinity or explicit null. Unequal resolved finite/Infinity endpoints are an upstream SDK interpolation limitation; Canvas diagnoses that context without changing source/model. " + stateHelp(), order++);
        }
        for (String state : thumbIconStates()) {
            for (String name : thumbIconBucketProperties(state)) {
                String help = iconSourceName(name).isEmpty()
                        ? "Icon constructs a full Icon, including Icon(null) when Data is absent; Inherit returns null and excludes all thirteen details. Unset with no details adds no state entry. " + stateHelp()
                        : IconWidgetPropertySchema.find(new PropertyName(iconSourceName(name).orElseThrow())).orElseThrow().description()
                                + " Stored and emitted as part of this state's Icon. Switch paints the glyph directly: semanticLabel, textDirection, blendMode, fontWeight, applyTextScaling and IconData direction matching are not consumed by the SDK painter.";
                add(values, name, Group.THUMB_ICON, help, order++);
            }
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Switch schema count/order mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static String stateReferenceHelp(String type, String family) {
        return "Strict non-null WidgetStateProperty<" + type + "?> reference, exclusive with all local "
                + family + " buckets. Dynamic and nullable outer types are rejected.";
    }

    private static String stateHelp() {
        return "First matching state wins in disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default order. Explicit null stops lower-priority entries and defers to SDK/theme; unset adds no entry. No disabled override is fabricated.";
    }

    private static void add(Map<String, Definition> values, String name, Group group, String help, int order) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, name.equals("variant") ? "Constructor"
                : Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, order));
    }

    private static String lowerFirst(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
