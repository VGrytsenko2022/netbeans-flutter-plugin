package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 Checkbox constructors and closed state-property projections. */
public final class CheckboxWidgetPropertySchema {
    public static final WidgetTypeId CHECKBOX_TYPE = new WidgetTypeId("flutter.material.Checkbox");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 106;
    public static final int DIRECT_PROPERTY_COUNT = 22;
    public static final int SLOT_COUNT = 0;
    private static final List<String> STATES = List.of("Disabled", "Error", "Dragged", "Pressed",
            "Selected", "ScrolledUnder", "Hovered", "Focused");
    private static final List<String> SIDE_PARTS = List.of("Color", "Width", "Style", "StrokeAlign");

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"),
        ACCESSIBILITY("Accessibility"), SHAPE("Shape"), FILL_COLOR("FillColor"),
        OVERLAY_COLOR("OverlayColor"), SIDE("Side");

        private final String suffix;

        Group(String suffix) {
            this.suffix = suffix;
        }

        public String setName() {
            return "checkbox" + suffix;
        }

        public String displayName() {
            return suffix.replaceAll("([a-z])([A-Z])", "$1 $2");
        }

        public String description() {
            return "Flutter Checkbox " + displayName().toLowerCase(java.util.Locale.ROOT) + ".";
        }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative Checkbox property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private CheckboxWidgetPropertySchema() {
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

    /** Metadata suffixes, with the default bucket first. */
    public static List<String> statePrefixes() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of("Default"), STATES.stream()).toList();
    }

    /** First matching entry wins, including explicit null; no implicit disabled entry is added. */
    public static List<String> statePriority() {
        return java.util.stream.Stream.concat(STATES.stream().map(CheckboxWidgetPropertySchema::lowerFirst),
                java.util.stream.Stream.of("default")).toList();
    }

    public static List<String> sideStates() {
        return STATES;
    }

    public static List<String> colorStateProperties(String family) {
        if (!List.of("fillColor", "overlayColor").contains(family)) {
            throw new IllegalArgumentException("Not a Checkbox state-color family: " + family);
        }
        return statePrefixes().stream().map(state -> family + state).toList();
    }

    public static List<String> sideBaseProperties() {
        return SIDE_PARTS.stream().map(part -> "side" + part).toList();
    }

    public static List<String> sideBucketProperties(String state) {
        if (!STATES.contains(state)) {
            throw new IllegalArgumentException("Not a Checkbox side state: " + state);
        }
        return java.util.stream.Stream.concat(java.util.stream.Stream.of("side" + state + "Mode"),
                SIDE_PARTS.stream().map(part -> "side" + state + part)).toList();
    }

    public static List<String> sideStateProperties() {
        return STATES.stream().flatMap(state -> sideBucketProperties(state).stream()).toList();
    }

    public static List<String> sideLocalProperties() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.concat(sideBaseProperties().stream(),
                java.util.stream.Stream.of("sideStateful")), sideStateProperties().stream()).toList();
    }

    public static List<String> mouseCursorPresets() {
        return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
    }

    public static List<String> shapeKinds() {
        return CardWidgetPropertySchema.shapeKinds();
    }

    public static List<String> builtInShapePropertyNames() {
        return CardWidgetPropertySchema.builtInShapePropertyNames();
    }

    public static boolean isShapeDetailProperty(String name) {
        return CardWidgetPropertySchema.isShapeDetailProperty(name);
    }

    public static boolean shapePropertyAppliesToKind(String name, String kind) {
        return CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind);
    }

    public static String preferredShapeKindForProperty(String name) {
        return CardWidgetPropertySchema.preferredShapeKindForProperty(name);
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.BEHAVIOR, "Required checked value, created false. Explicit null means mixed and requires Tristate true; unset is invalid.", 0);
        add(values, "tristate", Group.BEHAVIOR, "Unset is false. True permits null and the SDK false-to-true-to-null cycle. The widget is controlled: it never changes the stored value itself.", 1);
        add(values, "onChanged", Group.BEHAVIOR, "Strict ValueChanged<bool?> reference. Enabled without a reference emits a benign no-op; disabled emits null and retains callback metadata. Canvas never executes project callbacks.", 2);
        add(values, "mouseCursor", Group.BEHAVIOR, "All 41 reviewed cursor constants or a strict MouseCursor reference, including WidgetStateMouseCursor subtypes.", 3);
        add(values, "activeColor", Group.APPEARANCE, "Literal or semantic selected fill fallback, after explicit fillColor resolution and before CheckboxTheme.", 4);
        add(values, "fillColor", Group.FILL_COLOR, "Strict non-null WidgetStateProperty<Color?> reference, exclusive with all local fill-color buckets. Dynamic and nullable outer types are rejected.", 5);
        add(values, "checkColor", Group.APPEARANCE, "Literal or semantic check color; unset preserves CheckboxTheme and actual M2/M3 defaults.", 6);
        add(values, "focusColor", Group.APPEARANCE, "Literal or semantic focused overlay fallback; explicit overlayColor resolution takes precedence.", 7);
        add(values, "hoverColor", Group.APPEARANCE, "Literal or semantic hovered overlay fallback; ignored on adaptive Apple branches.", 8);
        add(values, "overlayColor", Group.OVERLAY_COLOR, "Strict non-null WidgetStateProperty<Color?> reference, exclusive with all local overlay buckets. Ignored on adaptive Apple branches.", 9);
        add(values, "splashRadius", Group.APPEARANCE, "Signed finite reaction radius or positive Infinity. Zero or negative suppresses the reaction; unset preserves CheckboxTheme/defaults. Ignored on adaptive Apple branches.", 10);
        add(values, "materialTapTargetSize", Group.LAYOUT, "Padded or shrinkWrap; unset preserves CheckboxTheme/defaults. Ignored on adaptive Apple branches.", 11);
        add(values, "visualDensityHorizontal", Group.LAYOUT, "Density from -4 to 4. Setting one axis constructs VisualDensity with the other axis zero; both unset inherit. Ignored on adaptive Apple branches.", 12);
        add(values, "visualDensityVertical", Group.LAYOUT, "Density from -4 to 4. Setting one axis constructs VisualDensity with the other axis zero; both unset inherit. Ignored on adaptive Apple branches.", 13);
        add(values, "focusNode", Group.BEHAVIOR, "Strict FocusNode reference. Canvas uses local focus ownership without executing project code.", 14);
        add(values, "autofocus", Group.BEHAVIOR, "Unset uses false.", 15);
        add(values, "shape", Group.SHAPE, "Strict OutlinedBorder reference, exclusive with the ten built-in shape families. Broad ShapeBorder is not sufficient. Unset inherits CheckboxTheme and actual M2/M3 or Cupertino defaults.", 16);
        add(values, "side", Group.SIDE, "Strict BorderSide reference, including WidgetStateBorderSide subtypes; exclusive with all local side fields. Plain sides apply only when unselected; stateful sides can also apply when selected.", 17);
        add(values, "isError", Group.BEHAVIOR, "Unset uses false. Supplies the error state to Material color/side resolution; ignored on adaptive Apple branches.", 18);
        add(values, "semanticLabel", Group.ACCESSIBILITY, "Optional screen-reader label; the SDK also exposes checked and mixed state semantics.", 19);
        add(values, "variant", Group.BEHAVIOR, "Required Designer constructor selector, created Standard. Standard and Adaptive are both const-capable and expose all the same SDK parameters.", 20);
        add(values, "enabled", Group.BEHAVIOR, "Required Designer activation policy, created true. False emits onChanged:null while preserving stored callback metadata.", 21);
        int order = 22;
        for (String name : builtInShapePropertyNames()) {
            var shared = CardWidgetPropertySchema.definitions().get(name);
            values.put(name, new Definition(Group.SHAPE, shared.displayName(),
                    shared.description().replace("Card", "Checkbox").replace("ShapeBorder", "OutlinedBorder"), shared.dartName(), order++));
        }
        for (String family : List.of("fillColor", "overlayColor")) {
            for (String name : colorStateProperties(family)) {
                add(values, name, family.equals("fillColor") ? Group.FILL_COLOR : Group.OVERLAY_COLOR,
                        "Literal color, semantic role or explicit null. First matching state wins in disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default order. Null stops lower-priority buckets and defers to the SDK/theme; unset adds no entry. No disabled override is fabricated.", order++);
            }
        }
        for (String name : sideBaseProperties()) {
            add(values, name, Group.SIDE, sideHelp(name) + " Base side: any leaf creates BorderSide with its own SDK defaults. With Stateful false/unset it applies only when unselected; with Stateful true it is the map default.", order++);
        }
        add(values, "sideStateful", Group.SIDE, "True emits WidgetStateBorderSide.fromMap, including selected states. False/unset uses a plain base BorderSide. State-specific buckets require true; resetting this flag clears state buckets but retains the base side.", order++);
        for (String state : STATES) {
            for (String name : sideBucketProperties(state)) {
                add(values, name, Group.SIDE, name.endsWith("Mode")
                        ? "Border constructs a side using its own defaults; Inherit returns null and excludes this bucket's four details. Unset with no details adds no entry; any detail implies Border. Requires Stateful true."
                        : sideHelp(name) + " State-specific side, requiring Stateful true. Any detail creates this bucket's BorderSide with its own SDK defaults, not partial inheritance from the base side.", order++);
            }
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Checkbox schema count/order mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static String sideHelp(String name) {
        if (name.endsWith("Color")) {
            return "Literal or semantic BorderSide color; explicit side default is opaque black.";
        }
        if (name.endsWith("Width")) {
            return "Finite non-negative BorderSide width, default one; zero is a hairline.";
        }
        if (name.endsWith("Style")) {
            return "BorderStyle solid or none; explicit side defaults to solid.";
        }
        return "Finite stroke alignment, default -1 (inside); values beyond the conventional -1..1 interval remain supported.";
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
