package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Pinned Flutter 3.44.8 MenuAnchor and complete sparse MenuStyle projection. */
public final class MenuAnchorWidgetPropertySchema {
    public static final WidgetTypeId MENU_ANCHOR_TYPE = new WidgetTypeId("flutter.material.MenuAnchor");
    public static final int DIRECT_PROPERTY_COUNT = 16;
    public static final int LOCAL_STYLE_PROPERTY_COUNT = 203;
    public static final int FLATTENED_PROPERTY_COUNT = 219;
    public static final int SLOT_COUNT = 2;
    public enum Group {
        EVENTS, BEHAVIOR, POSITION, BUILDER, ENABLED_STYLE, DISABLED_STYLE, ERROR_STYLE,
        DRAGGED_STYLE, PRESSED_STYLE, SELECTED_STYLE, SCROLLED_UNDER_STYLE,
        HOVERED_STYLE, FOCUSED_STYLE, COMMON_STYLE, STYLE_REFERENCE;
        public String setName() { return "menuAnchor" + name(); }
        public String displayName() {
            return switch (this) {
                case POSITION -> "Position"; case BUILDER -> "Anchor builder";
                case ENABLED_STYLE -> "Style — default";
                case COMMON_STYLE -> "Style — density & alignment";
                default -> TextButtonWidgetPropertySchema.Group.valueOf(name()).displayName();
            };
        }
        public String description() {
            return "MenuAnchor " + displayName() + ". Pinned native menu panels and cursors resolve MenuStyle against the empty WidgetState set; non-default buckets are retained and generated but are not driven by panel hover, focus or press.";
        }
    }
    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder, Encoding encoding) { }
    private static final Set<String> STATE_FIELDS = Set.of("BackgroundColor", "ShadowColor", "SurfaceTintColor", "Elevation", "Padding",
            "MinimumWidth", "MinimumHeight", "FixedWidth", "FixedHeight", "MaximumWidth", "MaximumHeight",
            "SideColor", "SideWidth", "SideStyle", "SideStrokeAlign", "ShapeKind", "ShapeRadiusTopLeft",
            "ShapeRadiusTopRight", "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft", "ShapeCircleEccentricity", "MouseCursor");
    private static final List<String> COMMON = List.of("styleVisualDensityHorizontal", "styleVisualDensityVertical", "styleAlignmentKind", "styleAlignmentX", "styleAlignmentY");
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private MenuAnchorWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> statePrefixes() { return TextButtonWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return TextButtonWidgetPropertySchema.statePriority(); }
    public static List<String> localStyleProperties() { return DEFINITIONS.keySet().stream().filter(name -> !name.equals("style") && name.startsWith("style")).toList(); }
    public static List<String> commonStyleProperties() { return COMMON; }
    public static boolean isCompound(PropertyName name) { return localStyleProperties().contains(name.value()); }
    public static List<String> booleanProperties() { return List.of("consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated"); }
    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        for (String name : List.of("controller", "childFocusNode", "style", "alignmentOffset", "reservedPadding", "layerLink", "clipBehavior", "anchorTapClosesMenu", "consumeOutsideTap", "onOpen", "onClose", "crossAxisUnconstrained", "useRootOverlay", "animated", "onAnimationStatusChanged", "builder")) {
            Group group = name.startsWith("on") ? Group.EVENTS : name.equals("builder") ? Group.BUILDER
                    : name.equals("style") ? Group.STYLE_REFERENCE : Set.of("alignmentOffset", "reservedPadding", "layerLink").contains(name) ? Group.POSITION : Group.BEHAVIOR;
            String description = switch (name) {
                case "controller" -> "Null or omission uses Flutter's internal MenuController. A strict project reference/factory must maintain stable one-anchor attachment; MenuController has no dispose method. Designer never creates a project controller field implicitly.";
                case "childFocusNode" -> "Optional nullable FocusNode reference associated with the opening widget. Without it, keyboard traversal back to the opening widget is disabled. Project node lifecycle remains user-owned.";
                case "style" -> "Whole nullable MenuStyle reference/factory, exclusive with all 203 local leaves. Native field resolution retries local, theme and SDK default when a resolver returns null. Panel and placement have distinct theme/default paths.";
                case "alignmentOffset" -> "Optional signed Offset or strict reference; omission/null uses Offset.zero. Directional alignment mirrors horizontal offset in RTL. MenuController.open(position:) overrides this offset.";
                case "reservedPadding" -> "Optional physical/directional EdgeInsetsGeometry or strict reference. Omission/null uses EdgeInsets.all(8), measured from the safe area. Runtime-invalid geometry is not silently rewritten in source.";
                case "layerLink" -> "Nullable strict LayerLink reference. Flutter creates the linked transform target/follower; changing link presence changes SDK subtree topology. Project identity and ownership remain external.";
                case "clipBehavior" -> "Non-null Clip, default hardEdge. No synthetic button clipping policy is applied.";
                case "anchorTapClosesMenu" -> "Deprecated SDK field, default false. Flutter 3.44.8 stores and diagnoses it but does not use it in native MenuAnchor behavior. Retained/generated exactly; not offered as a State consumer.";
                case "consumeOutsideTap" -> "Default false. True consumes a tap closing the menu; false also allows that tap to participate in the gesture arena.";
                case "onOpen" -> "Nullable VoidCallback invoked when the menu begins opening, not at animation completion. Callback body remains user-owned.";
                case "onClose" -> "Nullable VoidCallback invoked after the menu finishes closing/hiding. Disposal is not a user close callback. Callback body remains user-owned.";
                case "crossAxisUnconstrained" -> "Default true. Allows the menu panel's natural cross-axis size using an UnconstrainedBox; false keeps both axes constrained.";
                case "useRootOverlay" -> "Default false. Select the root overlay rather than the nearest overlay; respects native OverlayPortal lifecycle.";
                case "animated" -> "Default false. Enables pinned SDK opening/closing animation. MenuController.isOpen remains true through animated closing; use animation-status events for direction.";
                case "onAnimationStatusChanged" -> "Nullable ValueChanged<AnimationStatus>. Non-animated mode reports completed/dismissed; animated mode also reports forward/reverse. It does not create a controlled State binding.";
                default -> "Nullable MenuAnchorChildBuilder reference: Widget Function(BuildContext, MenuController, Widget?). The supplied Child is passed unchanged; absent builder returns Child or SizedBox. Create Menu Builder explicitly inserts a user-owned opener; project builders are not executed by Canvas.";
            };
            values.put(name, new Definition(group, name.replaceAll("([a-z])([A-Z])", "$1 $2"), description, Target.DIRECT, name, values.size(), Encoding.SCALAR));
        }
        for (String prefix : statePrefixes()) for (var entry : TextButtonWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(prefix) || !STATE_FIELDS.contains(name.substring(prefix.length()))) continue;
            var shared = entry.getValue();
            values.put(name, new Definition(Group.valueOf(shared.group().name()), shared.displayName(),
                    shared.description() + " MenuStyle resolves with empty states in this pinned MenuAnchor; non-default state leaves remain source data, not interactive panel states.",
                    shared.target(), shared.dartName(), shared.dartOrder(), shared.encoding()));
        }
        for (String name : COMMON) {
            var shared = TextButtonWidgetPropertySchema.find(name).orElseThrow();
            values.put(name, new Definition(Group.COMMON_STYLE, shared.displayName(),
                    name.startsWith("styleAlignment") ? "Complete local MenuStyle alignment requires kind, X and Y together; physical and directional coordinates remain distinct. Omission preserves native root/nested placement fallback."
                            : "Local MenuStyle VisualDensity constructor axis. Setting either axis constructs a complete local density; the omitted peer uses the native constructor default 0, not an inherited theme axis. Reset both to inherit native menu density. Menu density only increases horizontal padding, not vertical padding.",
                    shared.target(), shared.dartName(), shared.dartOrder(), shared.encoding()));
        }
        if (values.size() != FLATTENED_PROPERTY_COUNT) throw new ExceptionInInitializerError("MenuAnchor schema count " + values.size());
        return Collections.unmodifiableMap(values);
    }
}
