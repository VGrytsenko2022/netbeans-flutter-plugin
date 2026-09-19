package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Complete Focus and Focus.withExternalFocusNode constructor contract from Flutter 3.44.8. */
public final class FocusWidgetPropertySchema {
    public static final WidgetTypeId FOCUS_TYPE = new WidgetTypeId("flutter.widgets.Focus");
    public static final int SDK_PROPERTY_COUNT = 12;
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 13;
    public static final int CALLBACK_PROPERTY_COUNT = 3;
    public static final int SLOT_COUNT = 1;
    public static final String STANDARD = "standard";
    public static final String WITH_EXTERNAL_FOCUS_NODE = "withExternalFocusNode";
    private static final Set<String> STANDARD_ONLY = Set.of("onKeyEvent", "onKey", "canRequestFocus",
            "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable", "debugLabel");
    private static final Set<String> BOOLEANS = Set.of("autofocus", "canRequestFocus", "skipTraversal",
            "descendantsAreFocusable", "descendantsAreTraversable", "includeSemantics");

    public enum Group {
        BEHAVIOR("focusBehavior", "Behavior", "Constructor selection, focus and traversal behavior."),
        NODES("focusNodes", "Focus nodes", "Optional project-owned node references; ownership and disposal remain with the user."),
        KEYBOARD("focusKeyboard", "Keyboard", "Focus and key dispatch callbacks, not text input."),
        DEBUG("focusDebug", "Debug", "Diagnostic labeling.");
        private final String id, displayName, description;
        Group(String id, String displayName, String description) {
            this.id = id; this.displayName = displayName; this.description = description;
        }
        public String id() { return id; }
        public String setName() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }
    public record Definition(Group group, String displayName, String description, String dartName,
            int dartOrder, Optional<String> callbackType, String returnType, List<String> parameters) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description);
            Objects.requireNonNull(dartName); Objects.requireNonNull(callbackType); Objects.requireNonNull(returnType);
            parameters = List.copyOf(parameters);
            if (dartOrder < 1) throw new IllegalArgumentException("Property order follows required child");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private FocusWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return Optional.ofNullable(DEFINITIONS.get(name.value())); }
    public static Map<String, Definition> callbackDefinitions() {
        var result = new LinkedHashMap<String, Definition>();
        DEFINITIONS.forEach((name, definition) -> { if (definition.callbackType().isPresent()) result.put(name, definition); });
        return Collections.unmodifiableMap(result);
    }
    public static Set<String> standardOnlyProperties() { return STANDARD_ONLY; }
    public static Set<String> booleanProperties() { return BOOLEANS; }
    public static String variant(WidgetNode node) {
        return node.properties().get(new PropertyName("variant")) instanceof PropertyValue.StringValue value ? value.value() : STANDARD;
    }
    public static boolean usesExternalNode(WidgetNode node) { return WITH_EXTERNAL_FOCUS_NODE.equals(variant(node)); }
    public static boolean propertyAvailableInVariant(String name, String variant) {
        return !WITH_EXTERNAL_FOCUS_NODE.equals(variant) || !STANDARD_ONLY.contains(name);
    }
    public static boolean propertyAvailable(WidgetNode node, PropertyName name) {
        return !node.type().equals(FOCUS_TYPE) || propertyAvailableInVariant(name.value(), variant(node));
    }
    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        property(values, "focusNode", Group.NODES, "Focus node",
                "Standard: omitted/null lets Focus own an internal node. External: a non-null project FocusNode is required. The project owns disposal; a factory must return a stable appropriately owned node, not allocate a new node on every build.");
        property(values, "parentNode", Group.NODES, "Parent node", "Optional nullable FocusNode reference. Omitted/null uses the enclosing focus tree. The project owns the node lifecycle.");
        property(values, "autofocus", Group.BEHAVIOR, "Autofocus", "Omission preserves false; both constructors support this argument.");
        callback(values, "onFocusChange", "ValueChanged<bool>", "void", "bool:hasFocus");
        callback(values, "onKeyEvent", "FocusOnKeyEventCallback", "KeyEventResult", "FocusNode:node", "KeyEvent:event");
        callback(values, "onKey", "FocusOnKeyCallback", "KeyEventResult", "FocusNode:node", "RawKeyEvent:event");
        property(values, "canRequestFocus", Group.BEHAVIOR, "Can request focus", "Standard only. Omitted/null defers to focusNode.canRequestFocus or true. External uses the node's own value.");
        property(values, "skipTraversal", Group.BEHAVIOR, "Skip traversal", "Standard only. Omitted/null defers to focusNode.skipTraversal or false. This differs from preventing focus requests.");
        property(values, "descendantsAreFocusable", Group.BEHAVIOR, "Descendants are focusable", "Standard only. Omitted/null defers to the node or true; false excludes descendants from focus.");
        property(values, "descendantsAreTraversable", Group.BEHAVIOR, "Descendants are traversable", "Standard only. Omitted/null defers to the node or true; false excludes descendants from traversal.");
        property(values, "includeSemantics", Group.BEHAVIOR, "Include semantics", "Omission preserves true; both constructors support this argument.");
        property(values, "debugLabel", Group.DEBUG, "Debug label", "Standard only. Omitted/null defers to focusNode.debugLabel. External uses the node's own label.");
        property(values, "variant", Group.BEHAVIOR, "Constructor", "Standard or With external focus node. Switching preserves inactive standard-only values and bindings without emitting them; switching back restores their use.");
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("Focus requires all 12 SDK properties plus its selector");
        return Collections.unmodifiableMap(values);
    }
    private static void property(Map<String, Definition> values, String name, Group group, String display, String description) {
        values.put(name, new Definition(group, display, description, name, values.size() + 1, Optional.empty(), "void", List.of()));
    }
    private static void callback(Map<String, Definition> values, String name, String type, String result, String... parameters) {
        String description = type + " callback; omission/null preserves SDK fallback. User handlers never execute in Canvas."
                + (name.equals("onFocusChange") ? " Available in both constructors." : " Standard only; External uses the node's callback.")
                + (name.equals("onKey") ? " Deprecated by Flutter; prefer onKeyEvent." : "");
        values.put(name, new Definition(Group.KEYBOARD, name, description, name, values.size() + 1,
                Optional.of(type), result, List.of(parameters)));
    }
}
