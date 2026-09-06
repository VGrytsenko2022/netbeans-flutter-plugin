package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Full Flutter 3.44.8 Badge/Badge.count contract with the shared complete TextStyle projection. */
public final class BadgeWidgetPropertySchema {
    public static final WidgetTypeId BADGE_TYPE = new WidgetTypeId("flutter.material.Badge");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 41;
    public static final int SLOT_COUNT = 2;
    public enum Group {
        APPEARANCE("badgeAppearance", "Appearance", "Badge colors and small/large geometry."),
        POSITION("badgePosition", "Position", "Label padding, alignment and offset."),
        BEHAVIOR("badgeBehavior", "Behavior", "Label visibility and optional numeric-count constructor."),
        TEXT_STYLE("badgeTextStyle", "Text style", "Complete optional TextStyle and semantic base."),
        TEXT_STYLE_PAINT("badgeTextStylePaint", "Text paint and effects", "Foreground/background paint, shadows and decoration."),
        TEXT_STYLE_TYPOGRAPHY("badgeTextStyleTypography", "Advanced typography", "OpenType features and variable-font axes.");

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

    public record Definition(
            Group group,
            String displayName,
            String description,
            String dartName,
            int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative Badge property order");
            }
        }
    }

    private static final List<String> STYLE_SUFFIXES = List.of(
            "ThemeTextStyle", "Inherit", "Color", "BackgroundColor", "FontSize",
            "FontWeight", "FontStyle", "LetterSpacing", "WordSpacing", "TextBaseline",
            "Height", "LeadingDistribution", "LocaleLanguageCode", "LocaleScriptCode",
            "LocaleCountryCode", "DecorationUnderline", "DecorationOverline",
            "DecorationLineThrough", "Foreground", "Background", "Shadows",
            "FontFeatures", "FontVariations", "DecorationColor", "DecorationStyle",
            "DecorationThickness", "DebugLabel", "FontFamily", "FontFamilyFallback",
            "Package", "Overflow");
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private BadgeWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name).value()));
    }

    public static boolean isTextStyleProperty(PropertyName name) {
        return textStyleBinding(name).isPresent();
    }

    public static Optional<TextWidgetPropertySchema.Definition> textStyleBinding(PropertyName name) {
        String value = Objects.requireNonNull(name).value();
        if (!value.startsWith("textStyle") || !STYLE_SUFFIXES.contains(value.substring(9))) {
            return Optional.empty();
        }
        return TextWidgetPropertySchema.find(new PropertyName("style" + value.substring(9)));
    }

    public static boolean isCountMode(WidgetNode node) {
        return BADGE_TYPE.equals(node.type())
                && node.properties().containsKey(new PropertyName("count"));
    }

    public static Optional<String> slotUnavailableReason(WidgetNode node, SlotName slot) {
        if (!isCountMode(node) || !slot.value().equals("label")) {
            return Optional.empty();
        }
        return Optional.of("Badge.count owns its generated numeric label. Clear Count "
                + "before inserting, moving or replacing the Label slot.");
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal or reviewed ColorScheme color; unset uses BadgeTheme then ColorScheme.error (also in Material 2).", 0);
        add(values, "textColor", Group.APPEARANCE, "Text color",
                "Unset uses BadgeTheme then ColorScheme.onError. The SDK applies this color through TextStyle.copyWith; an explicit foreground Paint still takes precedence.", 1);
        add(values, "smallSize", Group.APPEARANCE, "Small size",
                "Non-negative small-dot diameter, used with no label. Unset inherits BadgeTheme then six logical pixels.", 2);
        add(values, "largeSize", Group.APPEARANCE, "Large size",
                "Finite signed minimum large-badge size, used with a label or Count. The SDK takes the maximum of this value and the label intrinsic height; negative values remain valid. Unset inherits BadgeTheme then sixteen logical pixels.", 3);
        add(values, "padding", Group.POSITION, "Padding",
                "Non-negative physical or directional label padding; unset inherits BadgeTheme then horizontal four. Retained but unused for a small dot.", 4);
        add(values, "alignment", Group.POSITION, "Alignment",
                "Physical or directional finite alignment relative to Child. Unset inherits BadgeTheme then AlignmentDirectional.topEnd. Also positions a small dot; has no effect without Child.", 5);
        add(values, "offset", Group.POSITION, "Offset",
                "Finite signed label offset; unset inherits BadgeTheme then (4,-4) LTR or (-4,-4) RTL. The pinned SDK additionally applies (0,8). Small dots ignore offset; no Child means no relative placement.", 6);
        add(values, "isLabelVisible", Group.BEHAVIOR, "Label visible",
                "Unset uses true. False returns only Child (or an empty box), retaining label, count and style in the model.", 7);
        add(values, "count", Group.BEHAVIOR, "Count",
                "Optional non-negative integer. Presence selects non-const Badge.count and its generated Text label; Label must first be empty. Reset Count also resets Max count and returns to Badge.", 10);
        add(values, "maxCount", Group.BEHAVIOR, "Max count",
                "Optional positive integer, allowed only after Count is set. Unset uses 999; counts above the maximum display maximum followed by '+'.", 11);
        int order = 12;
        for (String suffix : STYLE_SUFFIXES) {
            var binding = TextWidgetPropertySchema.find(
                    new PropertyName("style" + suffix)).orElseThrow();
            Group group = switch (binding.group()) {
                case STYLE_PAINT -> Group.TEXT_STYLE_PAINT;
                case STYLE_TYPOGRAPHY -> Group.TEXT_STYLE_TYPOGRAPHY;
                default -> Group.TEXT_STYLE;
            };
            String help = binding.description()
                    + " Badge uses this optional TextStyle only with a label or Count; "
                    + "all leaves unset preserve BadgeTheme/TextTheme.labelSmall. "
                    + "Badge textColor overrides style color unless foreground Paint is present.";
            values.put("textStyle" + suffix, new Definition(
                    group, binding.displayName(), help, binding.dartName(), order++));
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Badge property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String display,
            String help,
            int order) {
        values.put(name, new Definition(group, display, help, name, order));
    }
}
