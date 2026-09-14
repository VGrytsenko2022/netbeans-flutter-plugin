package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete pinned Flutter 3.44.8 TooltipThemeData, plus its whole-object reference. */
public final class TooltipThemeWidgetPropertySchema {
    public static final WidgetTypeId TOOLTIP_THEME_TYPE = new WidgetTypeId("flutter.material.TooltipTheme");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 47;
    public static final int DIRECT_PROPERTY_COUNT = 16;
    public static final int SLOT_COUNT = 1;
    public enum Group {
        THEME("Theme"), LAYOUT("Layout"), POSITION("Position"), BEHAVIOR("Behavior"),
        APPEARANCE("Appearance"), TEXT_STYLE("TextStyle"), TEXT_STYLE_PAINT("TextStylePaint"),
        TEXT_STYLE_TYPOGRAPHY("TextStyleTypography"), ACCESSIBILITY("Accessibility");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "tooltipTheme" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter TooltipTheme " + displayName() + ". The nearest theme replaces, rather than merges with, its parent."; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName);
            Objects.requireNonNull(description); Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative property order");
        }
    }
    private static final List<String> DATA_FIELDS = List.of("height", "constraints", "padding", "margin",
            "verticalOffset", "preferBelow", "excludeFromSemantics", "decoration", "textStyle", "textAlign",
            "waitDurationUs", "showDurationUs", "exitDurationUs", "triggerMode", "enableFeedback");
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private TooltipThemeWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> dataFields() { return DATA_FIELDS; }
    public static List<String> localProperties() { return DEFINITIONS.keySet().stream().filter(name -> !name.equals("data")).toList(); }
    public static List<String> durationProperties() { return TooltipWidgetPropertySchema.durationProperties(); }
    public static List<String> booleanProperties() { return List.of("preferBelow", "excludeFromSemantics", "enableFeedback"); }
    public static List<String> textStyleProperties() { return TooltipWidgetPropertySchema.textStyleProperties(); }
    public static Optional<TextWidgetPropertySchema.Definition> textStyleBinding(PropertyName name) { return TooltipWidgetPropertySchema.textStyleBinding(name); }
    public static boolean isTextStyleProperty(PropertyName name) { return textStyleBinding(name).isPresent(); }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        values.put("data", new Definition(Group.THEME, "Data",
                "Optional strict non-null TooltipThemeData reference/getter/member/zero-argument factory. Exclusive with every local field, including explicit null or State bindings; no fields are silently cleared. Omit Data to construct a fresh TooltipThemeData from local fields, or const TooltipThemeData() if empty. The nearest TooltipTheme replaces the entire parent tooltip theme; it does not merge. Factory and object lifecycle remain user-owned.", "data", 0));
        for (String name : DATA_FIELDS) {
            var source = TooltipWidgetPropertySchema.find(name).orElseThrow();
            String help = switch (name) {
                case "height" -> "Deprecated nullable signed minimum height, including positive/negative Infinity and NaN. Non-null Height and Constraints are exclusive. Unsafe mounted geometry is diagnosed in Canvas, not rewritten in source.";
                case "constraints" -> "Structured BoxConstraints, strict non-null BoxConstraints reference/factory or explicit null. Non-null Constraints and Height are exclusive.";
                case "padding", "margin" -> "Signed physical/directional insets, strict non-null EdgeInsetsGeometry reference/factory or explicit null. Unsafe mounted geometry is diagnosed without changing source.";
                case "verticalOffset" -> "Nullable signed numeric positioning gap, including positive/negative Infinity and NaN. Descendant Tooltip's explicit value takes precedence.";
                case "preferBelow" -> "Nullable preferred side; SDK positioning can flip when space is insufficient. Descendant Tooltip's explicit value takes precedence.";
                case "excludeFromSemantics" -> "Nullable exclusion of the tooltip message annotation, not anchor child semantics. Descendant Tooltip's explicit value takes precedence.";
                case "decoration" -> "Structured BoxDecoration with all shared reviewed border/gradient/shadow/declared-asset fields, strict Decoration reference/factory or null. ShapeDecoration and custom implementations stay source-owned.";
                case "textStyle" -> "Whole strict non-null TextStyle reference/factory or explicit null, exclusive with all 31 local Text style fields, including their explicit null values.";
                case "textAlign" -> "Nullable TextAlign start/end/left/right/center/justify. Descendant Tooltip's explicit value takes precedence.";
                case "waitDurationUs" -> "Exact signed portable integer microseconds, strict non-null Duration reference/factory or null. Mouse hover delay; signed values are preserved in source.";
                case "showDurationUs" -> "Exact signed portable integer microseconds, strict non-null Duration reference/factory or null. Touch-release visible duration, not a hover timeout.";
                case "exitDurationUs" -> "Exact signed portable integer microseconds, strict non-null Duration reference/factory or null. Mouse-exit dismissal delay. Fresh constructor generation preserves this field; pinned TooltipThemeData.copyWith drops it.";
                case "triggerMode" -> "Nullable manual/tap/longPress trigger mode. Manual still allows hover; this field supplies policy, not an onTriggered callback.";
                case "enableFeedback" -> "Nullable acoustic/haptic feedback policy for accepted touch triggers. No Event or runtime visibility ownership is added.";
                default -> throw new IllegalArgumentException(name);
            };
            values.put(name, new Definition(Group.valueOf(source.group().name()), source.displayName(), help
                    + " Omission/null leaves this nearest theme field null; it does not inherit that field from an outer TooltipTheme. Tooltip applies its own fallback.", source.dartName(), values.size() + 1));
        }
        for (String name : textStyleProperties()) {
            var source = TooltipWidgetPropertySchema.find(name).orElseThrow();
            var binding = textStyleBinding(new PropertyName(name)).orElseThrow();
            values.put(name, new Definition(Group.valueOf(source.group().name()), source.displayName(),
                    binding.description() + " Local TooltipThemeData TextStyle, exclusive with whole Data and whole Text style, including explicit null. No local leaves means textStyle remains null.", source.dartName(), values.size() + 1));
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("TooltipTheme property count: " + values.size());
        return Collections.unmodifiableMap(values);
    }
}
