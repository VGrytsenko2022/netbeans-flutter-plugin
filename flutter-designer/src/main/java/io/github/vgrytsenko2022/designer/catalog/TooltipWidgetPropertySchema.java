package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 Tooltip constructor, with shared typed TextStyle leaves. */
public final class TooltipWidgetPropertySchema {
    public static final WidgetTypeId TOOLTIP_TYPE = new WidgetTypeId("flutter.material.Tooltip");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 53;
    public static final int DIRECT_PROPERTY_COUNT = 22;
    public static final int SLOT_COUNT = 1;
    public enum Group {
        CONTENT("Content"), LAYOUT("Layout"), POSITION("Position"), BEHAVIOR("Behavior"),
        APPEARANCE("Appearance"), TEXT_STYLE("TextStyle"), TEXT_STYLE_PAINT("TextStylePaint"),
        TEXT_STYLE_TYPOGRAPHY("TextStyleTypography"), ACCESSIBILITY("Accessibility");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "tooltip" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter Tooltip " + displayName() + "."; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition { Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description); Objects.requireNonNull(dartName); if (dartOrder < 0) throw new IllegalArgumentException("Negative property order"); }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private TooltipWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> durationProperties() { return List.of("waitDurationUs", "showDurationUs", "exitDurationUs"); }
    public static List<String> booleanProperties() { return List.of("preferBelow", "excludeFromSemantics", "enableTapToDismiss", "enableFeedback", "ignorePointer"); }
    public static List<String> textStyleProperties() { return BadgeWidgetPropertySchema.definitions().keySet().stream().filter(name -> BadgeWidgetPropertySchema.isTextStyleProperty(new PropertyName(name))).toList(); }
    public static Optional<TextWidgetPropertySchema.Definition> textStyleBinding(PropertyName name) { return BadgeWidgetPropertySchema.textStyleBinding(name); }
    public static boolean isTextStyleProperty(PropertyName name) { return textStyleBinding(name).isPresent(); }
    public static boolean isNonNull(WidgetNode node, String name) { PropertyValue value = node.properties().get(new PropertyName(name)); return value != null && !(value instanceof PropertyValue.NullValue); }
    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        for (String name : List.of("message", "richMessage", "height", "constraints", "padding", "margin", "verticalOffset", "preferBelow", "excludeFromSemantics", "decoration", "textStyle", "textAlign", "waitDurationUs", "showDurationUs", "exitDurationUs", "enableTapToDismiss", "triggerMode", "enableFeedback", "onTriggered", "mouseCursor", "ignorePointer", "positionDelegate")) {
            Group group = switch (name) {
                case "message", "richMessage" -> Group.CONTENT;
                case "height", "constraints", "padding", "margin" -> Group.LAYOUT;
                case "verticalOffset", "preferBelow", "positionDelegate" -> Group.POSITION;
                case "excludeFromSemantics" -> Group.ACCESSIBILITY;
                case "decoration" -> Group.APPEARANCE;
                case "textStyle", "textAlign" -> Group.TEXT_STYLE;
                default -> Group.BEHAVIOR;
            };
            String help = switch (name) {
                case "message" -> "Plain message, including an empty string, or explicit null. Exactly one of Message and Rich message must be non-null. Creation uses Tooltip. Switching content is atomic; resetting the final non-null content is rejected.";
                case "richMessage" -> "Strict InlineSpan reference/getter/member/zero-argument factory or explicit null, exclusive with non-null Message. TextSpan, WidgetSpan and custom InlineSpan trees live in user-owned Dart; no local span-tree editor or raw Dart expression. Canvas does not execute project spans and labels its surrogate as unavailable content.";
                case "height" -> "Deprecated nullable signed numeric minimum height, including Infinity/negative Infinity/NaN. Only one of non-null Height and Constraints is allowed. Generated Dart preserves the value; unsafe mounted geometry is diagnosed in Canvas.";
                case "constraints" -> "Structured BoxConstraints, strict BoxConstraints reference/factory or null. Non-null Constraints is exclusive with non-null Height. Omission/null preserves TooltipTheme/platform defaults.";
                case "padding", "margin" -> "Signed physical/directional insets, strict EdgeInsetsGeometry reference/factory or null. Omission/null preserves TooltipTheme defaults. Unsafe Container geometry is diagnosed without rewriting source.";
                case "verticalOffset" -> "Nullable signed numeric gap, including Infinity/negative Infinity/NaN, used by the default positioning delegate. A custom Position delegate receives the resolved value through TooltipPositionContext.";
                case "preferBelow" -> "Nullable preferred side. Omission/null inherits TooltipTheme then true; default positioning flips when there is insufficient space.";
                case "decoration" -> "Structured BoxDecoration with all shared reviewed border/gradient/shadow/asset fields, strict Decoration reference/factory or null. ShapeDecoration and custom decorations are supported through typed user-owned Dart. Omission/null retains TooltipTheme/Material defaults.";
                case "textStyle" -> "Whole strict TextStyle reference/factory or null, exclusive with the 31 local Text style fields. Omission/null retains TooltipTheme and platform defaults.";
                case "textAlign" -> "Nullable TextAlign; omission/null inherits TooltipTheme then start.";
                case "waitDurationUs" -> "Exact signed portable integer microseconds, strict Duration reference/factory or null. Hover delay; omission/null inherits TooltipTheme then zero. Negative Duration is preserved, not silently clamped in source.";
                case "showDurationUs" -> "Exact signed portable integer microseconds, strict Duration reference/factory or null. Touch-release visible duration, not a hover timeout. Omission/null inherits TooltipTheme then 1500ms.";
                case "exitDurationUs" -> "Exact signed portable integer microseconds, strict Duration reference/factory or null. Mouse-exit dismissal delay; omission/null inherits TooltipTheme then 100ms.";
                case "enableTapToDismiss" -> "Non-null Boolean, default true. Allows pointer-down dismissal of an already visible tooltip; it does not select the trigger mode.";
                case "triggerMode" -> "Nullable TooltipTriggerMode manual/tap/longPress. Omission/null inherits TooltipTheme then longPress. Manual mode still allows mouse hover; programmatic visibility remains project-owned.";
                case "enableFeedback" -> "Nullable platform feedback flag. Omission/null inherits TooltipTheme then true; actual feedback follows tap/long-press triggering.";
                case "onTriggered" -> "Optional no-op, strict TooltipTriggeredCallback reference/factory or explicit null. Exact signature void(). In pinned Flutter 3.44.8 it is invoked by accepted tap/long-press triggers, not mouse hover or ensureTooltipVisible(). Canvas never executes project callbacks.";
                case "mouseCursor" -> "All 41 reviewed MouseCursor/SystemMouseCursors/WidgetStateMouseCursor presets, strict MouseCursor reference/factory or null. Omission/null uses MouseCursor.defer.";
                case "ignorePointer" -> "Nullable tooltip-overlay hit-testing flag. Omission/null ignores pointers for Message, but not Rich message, preserving interactive WidgetSpan descendants. It does not disable the anchor child.";
                case "positionDelegate" -> "Optional strict TooltipPositionDelegate reference/factory or null: Offset Function(TooltipPositionContext context). A non-event positioning delegate, not a Widget builder. Omission/null uses SDK positioning; isolated Canvas does not execute project code.";
                default -> "Nullable semantic exclusion flag; omission/null inherits TooltipTheme then false. Rich message uses its plain-text semantic representation unless excluded.";
            };
            String dartName = name.endsWith("DurationUs") ? name.substring(0, name.length() - 2) : name;
            add(values, name, group, help, dartName);
        }
        for (String name : textStyleProperties()) {
            var binding = textStyleBinding(new PropertyName(name)).orElseThrow();
            Group group = switch (binding.group()) { case STYLE_PAINT -> Group.TEXT_STYLE_PAINT; case STYLE_TYPOGRAPHY -> Group.TEXT_STYLE_TYPOGRAPHY; default -> Group.TEXT_STYLE; };
            add(values, name, group, binding.description() + " Shared Tooltip local TextStyle; exclusive with whole Text style, including explicit null. All local leaves omitted preserve TooltipTheme defaults.", binding.dartName());
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("Tooltip property count: " + values.size());
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help, String dartName) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        if (name.endsWith("DurationUs")) label = label.substring(0, label.length() - 3) + " (microseconds)";
        values.put(name, new Definition(group, Character.toUpperCase(label.charAt(0)) + label.substring(1), help, dartName, SLOT_COUNT + values.size()));
    }
}
