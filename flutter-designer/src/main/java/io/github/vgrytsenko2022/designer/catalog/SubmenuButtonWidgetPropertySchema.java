package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.util.*;

/** Complete pinned Flutter 3.44.8 SubmenuButton, with independently owned style families. */
public final class SubmenuButtonWidgetPropertySchema {
    public static final WidgetTypeId SUBMENU_BUTTON_TYPE = new WidgetTypeId("flutter.material.SubmenuButton");
    public static final int DIRECT_PROPERTY_COUNT = 16;
    public static final int LOCAL_STYLE_PROPERTY_COUNT = 498;
    public static final int LOCAL_MENU_STYLE_PROPERTY_COUNT = 203;
    public static final int FLATTENED_PROPERTY_COUNT = 721;
    public static final int SLOT_COUNT = 4;
    public enum Group {
        EVENTS, BEHAVIOR, POSITION, SUBMENU_ICON,
        ENABLED_STYLE, DISABLED_STYLE, ERROR_STYLE, DRAGGED_STYLE, PRESSED_STYLE, SELECTED_STYLE,
        SCROLLED_UNDER_STYLE, HOVERED_STYLE, FOCUSED_STYLE, COMMON_STYLE, STYLE_REFERENCE,
        MENU_DEFAULT_STYLE, MENU_DISABLED_STYLE, MENU_ERROR_STYLE, MENU_DRAGGED_STYLE, MENU_PRESSED_STYLE,
        MENU_SELECTED_STYLE, MENU_SCROLLED_UNDER_STYLE, MENU_HOVERED_STYLE, MENU_FOCUSED_STYLE,
        MENU_COMMON_STYLE, MENU_STYLE_REFERENCE;
        public String setName() { return "submenuButton" + name(); }
        public String displayName() {
            if (name().startsWith("MENU_")) return switch (this) {
                case MENU_STYLE_REFERENCE -> "Menu style — project reference";
                case MENU_DEFAULT_STYLE -> "Menu style — default";
                case MENU_COMMON_STYLE -> "Menu style — density & alignment";
                default -> "Menu " + TextButtonWidgetPropertySchema.Group.valueOf(name().substring(5)).displayName();
            };
            return switch (this) {
                case POSITION -> "Menu position"; case SUBMENU_ICON -> "Submenu indicator";
                default -> TextButtonWidgetPropertySchema.Group.valueOf(name()).displayName();
            };
        }
        public String description() {
            return name().startsWith("MENU_")
                    ? "Native submenu MenuStyle, independent of the button ButtonStyle. The panel resolves empty states; the padding alignment prepass alone uses statesController values."
                    : this == SUBMENU_ICON ? "Four reviewed indicator buckets or a whole custom WidgetStateProperty<Widget?>. No invented icon constructor fields."
                    : "SubmenuButton " + displayName() + ". Empty Menu children disables native activation; no synthetic Enabled or On pressed argument exists.";
        }
    }
    public record Definition(Group group, String displayName, String description, Target target,
            String dartName, int dartOrder, Encoding encoding) {
        public Definition { Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description); Objects.requireNonNull(target); Objects.requireNonNull(dartName); Objects.requireNonNull(encoding); if (dartOrder < 0) throw new IllegalArgumentException("Negative SubmenuButton order"); }
    }
    private static final List<String> ICONS = List.of("submenuIconDefault", "submenuIconDisabled", "submenuIconHovered", "submenuIconFocused");
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private SubmenuButtonWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> statePrefixes() { return TextButtonWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return TextButtonWidgetPropertySchema.statePriority(); }
    public static List<String> localStyleProperties() { return TextButtonWidgetPropertySchema.localStyleProperties(); }
    public static ElevatedButtonWidgetPropertySchema.Definition sharedStyleDefinition(String name) { return TextButtonWidgetPropertySchema.sharedStyleDefinition(name); }
    public static List<String> menuStyleProperties() { return MenuAnchorWidgetPropertySchema.localStyleProperties().stream().map(SubmenuButtonWidgetPropertySchema::menuStylePropertyName).toList(); }
    public static List<String> localMenuStyleProperties() { return menuStyleProperties(); }
    public static List<String> menuStatePrefixes() { return statePrefixes().stream().map(SubmenuButtonWidgetPropertySchema::menuStylePropertyName).toList(); }
    public static String menuStyleSourceName(String name) {
        if (!name.startsWith("menuStyle")) throw new IllegalArgumentException("Not a SubmenuButton menuStyle property: " + name);
        return "style" + name.substring("menuStyle".length());
    }
    public static String menuStylePropertyName(String source) {
        if (!source.startsWith("style")) throw new IllegalArgumentException("Not a source MenuStyle property: " + source);
        return "menuStyle" + source.substring("style".length());
    }
    public static List<String> submenuIconLocalProperties() { return ICONS; }
    public static boolean isCompound(PropertyName name) { return localStyleProperties().contains(name.value()) || menuStyleProperties().contains(name.value()) || ICONS.contains(name.value()) || name.value().equals("hoverOpenDelayUs"); }
    public static List<String> booleanProperties() { return List.of("useRootOverlay", "animated"); }
    /** Internal semantic projection only; the real node identity and source paths stay SubmenuButton-owned. */
    public static WidgetNode menuStyleProjection(WidgetNode node) {
        Map<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        for (String name : menuStyleProperties()) {
            PropertyValue value = node.properties().get(new PropertyName(name));
            if (value != null) properties.put(new PropertyName(menuStyleSourceName(name)), value);
        }
        return new WidgetNode(node.id(), MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE, properties, Map.of());
    }
    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> result = new LinkedHashMap<>();
        for (String name : List.of("onHover", "onFocusChange", "onOpen", "onClose", "controller", "style", "menuStyle", "alignmentOffset", "clipBehavior", "focusNode", "statesController", "submenuIcon", "useRootOverlay", "hoverOpenDelayUs", "animated", "onAnimationStatusChanged")) {
            Group group = name.startsWith("on") ? Group.EVENTS : name.equals("style") ? Group.STYLE_REFERENCE
                    : name.equals("menuStyle") ? Group.MENU_STYLE_REFERENCE : name.equals("submenuIcon") ? Group.SUBMENU_ICON
                    : Set.of("alignmentOffset", "useRootOverlay").contains(name) ? Group.POSITION : Group.BEHAVIOR;
            String help = switch (name) {
                case "onHover" -> "Nullable ValueChanged<bool> on real pointer movement/exit, not merely pointer enter. Empty Menu children disables this native hover handler.";
                case "onFocusChange" -> "Nullable ValueChanged<bool>, forwarded to the internal button only while Menu children is nonempty.";
                case "onOpen" -> "Nullable VoidCallback when opening starts. Native focus and menu state remain SDK-owned.";
                case "onClose" -> "Nullable VoidCallback after the submenu hides; disposal does not invent a close callback.";
                case "onAnimationStatusChanged" -> "Nullable ValueChanged<AnimationStatus>; animated mode reports direction and terminal states, non-animated mode completed/dismissed.";
                case "style" -> "Whole ButtonStyle reference/null, exclusive only with the 498 button style leaves. Native MenuButtonTheme/SubmenuButton defaults and whole WidgetStateProperty merge semantics remain authoritative.";
                case "menuStyle" -> "Whole MenuStyle reference/null, exclusive only with the 203 menu style leaves. Native padding prepass force-unwraps the chosen padding resolver; custom styles returning null there are not guaranteed mountable.";
                case "submenuIcon" -> "Whole WidgetStateProperty<Widget?> reference/null, exclusive with the four local indicator buckets. Native resolution uses disabled, hovered and focused; null results fall through to MenuTheme.submenuIcon and the RTL-aware native arrow.";
                case "statesController" -> "Nullable WidgetStatesController reference. Pinned SDK reads it only for the submenu-padding alignment prepass; it is NOT forwarded to TextButton or used to synthesize indicator states. Project ownership remains external.";
                case "controller" -> "Nullable MenuController reference/factory; omission/null uses an internal controller. Keep application identity stable and one-anchor attachment. MenuController has no dispose method.";
                case "focusNode" -> "Nullable project FocusNode; allocation/disposal stays user-owned. Omission/null uses the native internal focus node and keyboard/hover opening behavior.";
                case "alignmentOffset" -> "Nullable signed Offset or strict reference. Native menu-padding alignment adjustment is added after this offset; null/omission does not disable that correction.";
                case "clipBehavior" -> "Non-null Clip, default hardEdge, applied to the popup MenuAnchor. This is not the internal button's clipping argument; its layer builders still use native TextButton clip behavior.";
                case "hoverOpenDelayUs" -> "Signed integer microseconds or strict non-null Duration reference, default zero. Negative Duration is accepted and Timer treats it as zero delay. A positive delay under a native horizontal MenuBar asserts; Designer does not infer arbitrary external ancestry.";
                case "useRootOverlay" -> "Default false; generated source preserves the native root-versus-nearest overlay choice. Isolated Canvas placement may be approximated explicitly.";
                default -> "Default false; native popup animation controls direction/status and reversal. No controlled open-state binding is invented.";
            };
            result.put(name, new Definition(group, name.replaceAll("([a-z])([A-Z])", "$1 $2"), help, Target.DIRECT,
                    name.equals("hoverOpenDelayUs") ? "hoverOpenDelay" : name, result.size(), Encoding.SCALAR));
        }
        for (String name : localStyleProperties()) {
            var shared = TextButtonWidgetPropertySchema.find(name).orElseThrow();
            result.put(name, new Definition(Group.valueOf(shared.group().name()), shared.displayName(),
                    shared.description() + " SubmenuButton button-style family only; changing it never clears MenuStyle or indicator buckets.", shared.target(), shared.dartName(), shared.dartOrder(), shared.encoding()));
        }
        for (String source : MenuAnchorWidgetPropertySchema.localStyleProperties()) {
            var shared = MenuAnchorWidgetPropertySchema.find(source).orElseThrow();
            Group group = shared.group() == MenuAnchorWidgetPropertySchema.Group.ENABLED_STYLE ? Group.MENU_DEFAULT_STYLE
                    : Group.valueOf("MENU_" + shared.group().name());
            String help = shared.description() + " This is the independent submenu MenuStyle family; changing it never clears ButtonStyle. Local padding resolvers retry MenuTheme/native padding on a null local result so the SDK alignment prepass has valid padding.";
            result.put(menuStylePropertyName(source), new Definition(group, "Menu " + shared.displayName(), help,
                    shared.target(), shared.dartName(), shared.dartOrder(), shared.encoding()));
        }
        for (String name : ICONS) result.put(name, new Definition(Group.SUBMENU_ICON, name.substring("submenuIcon".length()) + " indicator",
                "Material IconData, a strict Widget reference/factory, or explicit null. Priority disabled, hovered, focused, default; omitted buckets skip to the next configured active bucket/default. Explicit null is terminal and uses native theme/arrow fallback. IconData None produces Icon(null), distinct from null fallback.",
                Target.DIRECT, "submenuIcon", result.size(), Encoding.SCALAR));
        if (result.size() != FLATTENED_PROPERTY_COUNT) throw new ExceptionInInitializerError("SubmenuButton schema count " + result.size());
        return Collections.unmodifiableMap(result);
    }
}
