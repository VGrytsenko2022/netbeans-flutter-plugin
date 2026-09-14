package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Flutter 3.44.8 TextButton and TextButton.icon, with all eight WidgetState
 * buckets, a sparse local ButtonStyle and a strictly typed whole-style branch.
 * Arbitrary state combinations and shapes remain available through ButtonStyle
 * references; the local projection is deliberately a closed, ordered preset.
 */
public final class TextButtonWidgetPropertySchema {
    public static final WidgetTypeId TEXT_BUTTON_TYPE =
            new WidgetTypeId("flutter.material.TextButton");
    public static final int DIRECT_PROPERTY_COUNT = 12;
    public static final int STATE_COUNT = 9;
    public static final int STATE_PROPERTY_COUNT =
            ElevatedButtonWidgetPropertySchema.STATE_PROPERTY_COUNT;
    public static final int COMMON_STYLE_PROPERTY_COUNT = 12;
    public static final int LOCAL_STYLE_PROPERTY_COUNT =
            STATE_COUNT * STATE_PROPERTY_COUNT + COMMON_STYLE_PROPERTY_COUNT;
    public static final int FLATTENED_PROPERTY_COUNT =
            DIRECT_PROPERTY_COUNT + LOCAL_STYLE_PROPERTY_COUNT + 1;
    public static final int SLOT_COUNT = 2;

    public enum Group {
        EVENTS("textButtonEvents", "Events", "Strictly typed project callback references."),
        BEHAVIOR("textButtonBehavior", "Behavior", "Constructor, activation, focus and clipping."),
        ENABLED_STYLE("textButtonEnabledStyle", "Style — enabled/default", "Sparse default style; disabled is isolated."),
        DISABLED_STYLE("textButtonDisabledStyle", "Style — disabled", "Highest-priority, isolated disabled values."),
        ERROR_STYLE("textButtonErrorStyle", "Style — error", "Sparse style while the error state is present."),
        DRAGGED_STYLE("textButtonDraggedStyle", "Style — dragged", "Sparse style while the dragged state is present."),
        PRESSED_STYLE("textButtonPressedStyle", "Style — pressed", "Sparse style while the button is pressed."),
        SELECTED_STYLE("textButtonSelectedStyle", "Style — selected", "Sparse style while the selected state is present."),
        SCROLLED_UNDER_STYLE("textButtonScrolledUnderStyle", "Style — scrolled under", "Sparse style while scrolledUnder is present."),
        HOVERED_STYLE("textButtonHoveredStyle", "Style — hovered", "Sparse style while a pointer hovers."),
        FOCUSED_STYLE("textButtonFocusedStyle", "Style — focused", "Sparse style while the button has focus."),
        COMMON_STYLE("textButtonCommonStyle", "Style — layout & feedback", "Non-state layout, feedback, icon alignment and layer builders."),
        STYLE_REFERENCE("textButtonStyleReference", "Style — project reference", "A complete ButtonStyle reference, exclusive with every local style leaf.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder, Encoding encoding) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(dartName, "dartName");
            Objects.requireNonNull(encoding, "encoding");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative TextButton property order");
            }
        }
    }

    private static final Map<String, Group> STATES = stateGroups();
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private static final List<String> LOCAL_STYLE = DEFINITIONS.entrySet().stream()
            .filter(entry -> entry.getValue().target() != Target.DIRECT
                    && entry.getValue().target() != Target.ACTIVATION)
            .map(Map.Entry::getKey).toList();

    private TextButtonWidgetPropertySchema() { }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name, "name").value());
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static List<String> variants() { return List.of("standard", "icon"); }
    public static List<String> statePrefixes() { return List.copyOf(STATES.keySet()); }
    public static List<String> localStyleProperties() { return LOCAL_STYLE; }

    /** Highest to lowest priority; disabled never inherits an enabled bucket. */
    public static List<String> statePriority() {
        return List.of("disabled", "error", "dragged", "pressed", "selected",
                "scrolledUnder", "hovered", "focused", "any");
    }

    public static boolean isCompound(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    public static boolean isIcon(WidgetNode node) {
        return node.type().equals(TEXT_BUTTON_TYPE)
                && new PropertyValue.StringValue("icon").equals(
                        node.properties().get(new PropertyName("variant")));
    }

    /** Model-aware admission: the optional icon is unavailable in standard mode. */
    public static boolean acceptsIcon(WidgetNode node) { return isIcon(node); }

    public static Optional<String> slotUnavailableReason(WidgetNode node, SlotName slot) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(slot, "slot");
        return node.type().equals(TEXT_BUTTON_TYPE) && slot.value().equals("icon") && !isIcon(node)
                ? Optional.of("TextButton icon slot requires the Icon constructor; switch Constructor to Icon first.")
                : Optional.empty();
    }

    /** Shared assembly metadata for an existing sparse state leaf. */
    public static ElevatedButtonWidgetPropertySchema.Definition sharedStyleDefinition(String name) {
        for (String prefix : statePrefixes()) {
            if (name.startsWith(prefix + "Text")) {
                return ElevatedButtonWidgetPropertySchema.find("style" + name.substring(prefix.length()))
                        .orElseThrow();
            }
        }
        throw new IllegalArgumentException("Not a TextButton text-style leaf: " + name);
    }

    private static Map<String, Group> stateGroups() {
        Map<String, Group> values = new LinkedHashMap<>();
        values.put("style", Group.ENABLED_STYLE);
        values.put("styleDisabled", Group.DISABLED_STYLE);
        values.put("styleError", Group.ERROR_STYLE);
        values.put("styleDragged", Group.DRAGGED_STYLE);
        values.put("stylePressed", Group.PRESSED_STYLE);
        values.put("styleSelected", Group.SELECTED_STYLE);
        values.put("styleScrolledUnder", Group.SCROLLED_UNDER_STYLE);
        values.put("styleHovered", Group.HOVERED_STYLE);
        values.put("styleFocused", Group.FOCUSED_STYLE);
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        direct(values, "enabled", Group.BEHAVIOR, "Enabled", Target.ACTIVATION,
                "Required activation selector. Disabled emits null activation callbacks while retaining references. Enabled with neither callback generates a no-op; long-press-only remains long-press-only. Cannot be reset.");
        for (String name : List.of("onPressed", "onLongPress", "onHover", "onFocusChange")) {
            var shared = ElevatedButtonWidgetPropertySchema.find(name).orElseThrow();
            direct(values, name, Group.EVENTS, shared.displayName(), shared.target(),
                    "Strict non-null " + (name.equals("onPressed") || name.equals("onLongPress")
                            ? "VoidCallback" : "ValueChanged<bool>")
                    + " project reference; dynamic, nullable outer types and wrong signatures are rejected. Canvas does not execute project callbacks.");
        }
        direct(values, "focusNode", Group.BEHAVIOR, "Focus node", Target.DIRECT,
                "Strict non-null FocusNode project reference. Ownership stays with the project; Canvas does not execute it.");
        direct(values, "autofocus", Group.BEHAVIOR, "Autofocus", Target.DIRECT,
                "Request initial focus. Unset preserves false.");
        direct(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior", Target.DIRECT,
                "Clip enum or explicit null. Standard omission is null; Icon omission is Clip.none. Explicit null enables SDK automatic antiAlias when layer builders are present.");
        direct(values, "statesController", Group.BEHAVIOR, "States controller", Target.DIRECT,
                "Strict non-null WidgetStatesController project reference. External ownership; Canvas does not execute the controller.");
        direct(values, "isSemanticButton", Group.BEHAVIOR, "Semantic button", Target.DIRECT,
                "Standard only. Unset uses true; explicit null suppresses the button semantics annotation. Not accepted by TextButton.icon.");
        direct(values, "iconAlignment", Group.BEHAVIOR, "Icon alignment", Target.DIRECT,
                "Icon constructor only. Constructor alignment wins over TextButtonTheme, then local style, then start. Direction resolves with RTL.");
        direct(values, "variant", Group.BEHAVIOR, "Constructor", Target.ACTIVATION,
                "Required Standard or Icon constructor. Child is always required and becomes label in Icon mode. Clear or move a populated icon before switching to Standard. Cannot be reset; never emitted as an argument.");

        Map<String, ElevatedButtonWidgetPropertySchema.Definition> source =
                ElevatedButtonWidgetPropertySchema.definitions();
        for (Map.Entry<String, Group> state : STATES.entrySet()) {
            source.forEach((name, binding) -> {
                if (binding.group() == ElevatedButtonWidgetPropertySchema.Group.ENABLED_STYLE) {
                    values.put(state.getKey() + name.substring("style".length()),
                            new Definition(state.getValue(), binding.displayName(), binding.description(),
                                    binding.target(), binding.dartName(), binding.dartOrder(), binding.encoding()));
                }
            });
        }
        source.forEach((name, binding) -> {
            if (binding.group() == ElevatedButtonWidgetPropertySchema.Group.COMMON_STYLE
                    && !ElevatedButtonWidgetPropertySchema.layerBuilderProperties().contains(name)) {
                values.put(name, new Definition(Group.COMMON_STYLE, binding.displayName(),
                        binding.description(), binding.target(), binding.dartName(),
                        binding.dartOrder(), binding.encoding()));
            }
        });
        common(values, "styleIconAlignment", "Style icon alignment", "iconAlignment", 9,
                "Local ButtonStyle icon alignment. The Icon constructor and TextButtonTheme alignment take precedence.");
        common(values, "styleBackgroundBuilder", "Background builder", "backgroundBuilder", 10,
                "Strict non-null ButtonLayerBuilder project reference for the background layer. Canvas cannot execute project builders.");
        common(values, "styleForegroundBuilder", "Foreground builder", "foregroundBuilder", 11,
                "Strict non-null ButtonLayerBuilder project reference for the foreground layer. Canvas cannot execute project builders.");
        values.put("style", new Definition(Group.STYLE_REFERENCE, "Button style",
                "Strict non-null ButtonStyle project reference, including arbitrary WidgetStatesConstraint combinations, shapes, text and layer builders. Exclusive with all 498 local style leaves. Canvas cannot execute a project style.",
                Target.DIRECT, "style", values.size(), Encoding.SCALAR));
        if (values.size() != FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("TextButton property count: " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void direct(Map<String, Definition> values, String name, Group group,
            String label, Target target, String description) {
        values.put(name, new Definition(group, label, description, target, name,
                values.size(), Encoding.SCALAR));
    }

    private static void common(Map<String, Definition> values, String name, String label,
            String dartName, int order, String description) {
        values.put(name, new Definition(Group.COMMON_STYLE, label, description,
                Target.STYLE_COMMON, dartName, order, Encoding.SCALAR));
    }
}
