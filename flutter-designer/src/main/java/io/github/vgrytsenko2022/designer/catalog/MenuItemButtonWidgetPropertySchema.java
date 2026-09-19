package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Complete pinned MenuItemButton constructor, shortcuts and shared sparse ButtonStyle. */
public final class MenuItemButtonWidgetPropertySchema {
    public static final WidgetTypeId MENU_ITEM_BUTTON_TYPE = new WidgetTypeId("flutter.material.MenuItemButton");
    public static final int DIRECT_PROPERTY_COUNT = 14;
    public static final int LOCAL_STYLE_PROPERTY_COUNT = 498;
    public static final int FLATTENED_PROPERTY_COUNT = 520;
    public static final int SLOT_COUNT = 3;
    public enum Group {
        EVENTS, BEHAVIOR, SHORTCUTS, ACCESSIBILITY, ENABLED_STYLE, DISABLED_STYLE, ERROR_STYLE,
        DRAGGED_STYLE, PRESSED_STYLE, SELECTED_STYLE, SCROLLED_UNDER_STYLE, HOVERED_STYLE,
        FOCUSED_STYLE, COMMON_STYLE, STYLE_REFERENCE;
        public String setName() { return "menuItemButton" + name(); }
        public String displayName() {
            if (this == SHORTCUTS) return "Shortcut hint";
            if (this == ACCESSIBILITY) return "Accessibility";
            return TextButtonWidgetPropertySchema.Group.valueOf(name()).displayName();
        }
        public String description() {
            if (this == SHORTCUTS) return "Display-only SingleActivator or CharacterActivator. Does not register shortcuts.";
            if (this == ACCESSIBILITY) return "Native menu item semantics.";
            return TextButtonWidgetPropertySchema.Group.valueOf(name()).description().replace("TextButton", "MenuItemButton");
        }
    }
    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder, Encoding encoding) { }
    private static final List<String> SHORTCUT = List.of("shortcutTrigger", "shortcutCharacter", "shortcutControl",
            "shortcutShift", "shortcutAlt", "shortcutMeta", "shortcutNumLock", "shortcutIncludeRepeats");
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private MenuItemButtonWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> statePrefixes() { return TextButtonWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return TextButtonWidgetPropertySchema.statePriority(); }
    public static List<String> localStyleProperties() { return TextButtonWidgetPropertySchema.localStyleProperties(); }
    public static List<String> shortcutLocalProperties() { return SHORTCUT; }
    public static List<String> localShortcutProperties() { return SHORTCUT; }
    public static List<String> logicalKeyboardKeys() { return MenuShortcutKeyCatalog.names(); }
    public static ElevatedButtonWidgetPropertySchema.Definition sharedStyleDefinition(String name) {
        return TextButtonWidgetPropertySchema.sharedStyleDefinition(name);
    }
    public static boolean isCompound(PropertyName name) {
        return name.value().equals("enabled") || name.value().equals("onPressed")
                || SHORTCUT.contains(name.value()) || localStyleProperties().contains(name.value());
    }
    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "enabled", Group.BEHAVIOR, "Enabled", "Required activation selector. Disabled emits null onPressed while retaining its reference; enabled without a reference emits a no-op. State binding controls activation, not a nonexistent SDK enabled argument.");
        add(values, "onPressed", Group.EVENTS, "On pressed", "Strict VoidCallback reference. Native activation applies pending focus changes and invokes it after the frame; closeOnActivate controls menu closure. Project callback bodies remain user-owned.");
        add(values, "onHover", Group.EVENTS, "On hover", "Nullable ValueChanged<bool> callback. Native hover notifications may occur even while activation is disabled; Canvas never executes project callbacks.");
        add(values, "requestFocusOnHover", Group.BEHAVIOR, "Request focus on hover", "Defaults to true. Native MenuItemButton requests focus while hovered; false leaves focus management to the application.");
        add(values, "onFocusChange", Group.EVENTS, "On focus change", "Nullable ValueChanged<bool> reference. The native inner button forwards it only while enabled.");
        add(values, "focusNode", Group.BEHAVIOR, "Focus node", "Null or omission lets Flutter own its internal FocusNode. A project FocusNode reference/factory keeps lifecycle ownership in the project; do not create unmanaged nodes on every build.");
        add(values, "autofocus", Group.BEHAVIOR, "Autofocus", "Defaults to false. The native button requests initial focus only while enabled.");
        add(values, "shortcut", Group.SHORTCUTS, "Shortcut reference", "Null or a strict MenuSerializableShortcut reference/factory, exclusive with all local shortcut fields. This displays a hint and does not register a shortcut.");
        add(values, "semanticsLabel", Group.ACCESSIBILITY, "Semantics label", "Optional nullable string overriding semantics for the entire item, including its icon and label children.");
        add(values, "style", Group.STYLE_REFERENCE, "ButtonStyle", "Nullable strict ButtonStyle reference/factory, exclusive with all 498 local style leaves. Omission or null inherits MenuButtonTheme and MenuItemButton defaults, not TextButton defaults.");
        add(values, "statesController", Group.BEHAVIOR, "States controller", "Nullable strict WidgetStatesController reference. Lifecycle remains project-owned; omission uses Flutter ownership.");
        add(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior", "Non-null Clip, default none. Unlike some other button constructors, MenuItemButton keeps Clip.none even when layer builders are present unless explicitly changed.");
        add(values, "closeOnActivate", Group.BEHAVIOR, "Close on activate", "Defaults to true. Native activation closes the root menu before its post-frame callback.");
        add(values, "overflowAxis", Group.BEHAVIOR, "Overflow axis", "Defaults to horizontal. A MenuAnchor or MenuBar ancestor may supply its own orientation instead.");
        for (String name : SHORTCUT) add(values, name, Group.SHORTCUTS,
                name.substring(8), switch (name) {
                    case "shortcutTrigger" -> "SingleActivator trigger: one of the 432 reviewed non-modifier LogicalKeyboardKey constants. Exclusive with Character; custom keys use a whole shortcut reference.";
                    case "shortcutCharacter" -> "CharacterActivator accepts and preserves any string, including Unicode or empty strings; matching is case-sensitive. However, pinned MenuItemButton hint serialization requires exactly one UTF-16 code unit: empty, multi-unit or supplementary characters can fail when mounted. Canvas explicitly omits an unrenderable hint, not the button or stored/source value. No implicit shortcut registration.";
                    case "shortcutShift", "shortcutNumLock" -> "SingleActivator-only field. Not accepted with CharacterActivator, even when its stored value equals the SDK default.";
                    default -> "Optional local activator argument; requires exactly one Trigger or Character anchor. Compatible modifiers are retained when changing branch.";
                });
        for (String name : localStyleProperties()) {
            var shared = TextButtonWidgetPropertySchema.find(name).orElseThrow();
            values.put(name, new Definition(Group.valueOf(shared.group().name()), shared.displayName(),
                    shared.description().replace("TextButtonTheme", "MenuButtonTheme").replace("TextButton", "MenuItemButton"),
                    shared.target(), shared.dartName(), shared.dartOrder(), shared.encoding()));
        }
        if (values.size() != FLATTENED_PROPERTY_COUNT) throw new ExceptionInInitializerError("MenuItemButton schema count " + values.size());
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String label, String description) {
        values.put(name, new Definition(group, label, description,
                name.equals("enabled") || name.equals("onPressed") ? Target.ACTIVATION : Target.DIRECT,
                name, values.size(), Encoding.SCALAR));
    }
}
