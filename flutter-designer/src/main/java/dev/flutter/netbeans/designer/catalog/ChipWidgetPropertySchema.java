package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Complete pinned Flutter 3.44.8 Chip: 27 native arguments and closed local styles. */
public final class ChipWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.Chip");
    public static final List<String> DIRECT = List.of("key", "labelStyle", "labelPadding", "onDeleted",
            "deleteIconColor", "deleteButtonTooltipMessage", "side", "shape", "clipBehavior", "focusNode",
            "autofocus", "color", "backgroundColor", "padding", "visualDensity", "materialTapTargetSize",
            "elevation", "shadowColor", "surfaceTintColor", "iconTheme", "avatarBoxConstraints",
            "deleteIconBoxConstraints", "chipAnimationStyle", "mouseCursor");
    public static final List<String> ANIMATIONS = List.of("enableAnimation", "selectAnimation", "avatarDrawerAnimation", "deleteDrawerAnimation");
    public static final String DESCRIPTION = "Informational Material chip with required Label and optional Avatar/Delete icon. "
            + "Only onDeleted is a Chip event: it requests deletion; the application must remove the widget. "
            + "Delete icon is visible only with a handler. Chip has no body press or selection callback. "
            + "Canvas never executes project code, requests focus or deletes the design.";
    public static String upper(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    public static String animationPrefix(String part) { return "chipAnimationStyle" + upper(part); }
    public static List<String> animationLeaves(String prefix) { return List.of(prefix+"DurationUs", prefix+"ReverseDurationUs", prefix+"Curve", prefix+"ReverseCurve"); }
    public static List<String> textLeaves() { return AlertDialogWidgetPropertySchema.localStyleProperties("contentTextStyle").stream().map(n -> "labelStyle"+n.substring("contentTextStyle".length())).toList(); }
    public static Optional<TextWidgetPropertySchema.Definition> textBinding(PropertyName name) {
        return textLeaves().contains(name.value()) ? TextWidgetPropertySchema.find(new PropertyName("style"+name.value().substring("labelStyle".length()))) : Optional.empty();
    }
    public static List<String> iconLeaves() { return IconThemeWidgetPropertySchema.definitions().keySet().stream().filter(n -> !n.equals("merge")).map(n -> "iconTheme"+upper(n)).toList(); }
    public static List<String> stateLeaves(String family) { return CheckboxWidgetPropertySchema.statePrefixes().stream().map(s -> family+s).toList(); }
    public static Map<String,List<String>> families() {
        var result = new LinkedHashMap<String,List<String>>();
        result.put("labelStyle", textLeaves()); result.put("shape", CardWidgetPropertySchema.builtInShapePropertyNames());
        result.put("side", CheckboxWidgetPropertySchema.sideLocalProperties());
        result.put("color", stateLeaves("color")); result.put("mouseCursor", stateLeaves("mouseCursor"));
        result.put("iconTheme", iconLeaves()); result.put("visualDensity", List.of("visualDensityHorizontal","visualDensityVertical"));
        var animation = new ArrayList<String>();
        for (String part : ANIMATIONS) { String prefix=animationPrefix(part); animation.add(prefix); animation.addAll(animationLeaves(prefix)); result.put(prefix, animationLeaves(prefix)); }
        result.put("chipAnimationStyle", List.copyOf(animation));
        return Collections.unmodifiableMap(result);
    }
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        var dialog = DialogWidgetPropertySchema.properties(false).stream().collect(java.util.stream.Collectors.toMap(p->p.name().value(),p->p.constraints()));
        var check = BuiltInWidgetCatalog.checkbox();
        for (String name : DIRECT) {
            List<PropertyValueConstraint> c = switch(name) {
                case "labelStyle" -> nullableRef("TextStyle?");
                case "labelPadding","padding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true,true),ref("EdgeInsetsGeometry?"),nil());
                case "onDeleted" -> nullableRef("VoidCallback?");
                case "deleteIconColor" -> dialog.get("backgroundColor");
                case "deleteButtonTooltipMessage" -> List.of(new PropertyValueConstraint.StringLength(0,16384),ref("String?"),nil());
                case "side" -> nullableRef("BorderSide?");
                case "shape" -> nullableRef("OutlinedBorder?");
                case "focusNode" -> nullableRef("FocusNode?");
                case "color" -> nullableRef("WidgetStateProperty<Color?>?");
                case "visualDensity" -> nullableRef("VisualDensity?");
                case "iconTheme" -> nullableRef("IconThemeData?");
                case "avatarBoxConstraints","deleteIconBoxConstraints" -> dialog.get("constraints");
                case "chipAnimationStyle" -> nullableRef("ChipAnimationStyle?");
                case "autofocus","clipBehavior" -> name.equals("autofocus") ? List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN))
                        : List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart","Clip"),List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer")));
                case "mouseCursor" -> List.of(cursorPattern(),ref("MouseCursor?"),nil());
                case "materialTapTargetSize" -> withNull(check.property(new PropertyName(name)).orElseThrow().constraints());
                default -> Objects.requireNonNull(dialog.get(name),name);
            };
            add(result,name,c);
        }
        BuiltInWidgetCatalog.appendTextStyleProperties(result,"labelStyle",result.size());
        for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) add(result,name,dialog.get(name));
        for (String name : CheckboxWidgetPropertySchema.sideLocalProperties()) add(result,name,check.property(new PropertyName(name)).orElseThrow().constraints());
        for (String name : stateLeaves("color")) add(result,name,check.property(new PropertyName("fillColor"+name.substring("color".length()))).orElseThrow().constraints());
        for (String name : stateLeaves("mouseCursor")) add(result,name,List.of(cursorPattern(),ref("MouseCursor")));
        for (String name : List.of("visualDensityHorizontal","visualDensityVertical")) add(result,name,check.property(new PropertyName(name)).orElseThrow().constraints());
        for (var p : BuiltInWidgetCatalog.iconTheme().properties()) if(!p.name().value().equals("merge")) add(result,"iconTheme"+upper(p.name().value()),withNull(p.constraints()));
        for (String part : ANIMATIONS) {
            String prefix = animationPrefix(part);
            add(result,prefix,List.of(new PropertyValueConstraint.StringPattern("noAnimation","AnimationStyle preset"),ref("AnimationStyle?"),nil()));
            for(String name:animationLeaves(prefix)) add(result,name,name.endsWith("Us")
                    ? List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,DartNumericLiterals.MAX_PORTABLE_INTEGER),ref("Duration?"),nil())
                    : List.of(new PropertyValueConstraint.StringPattern("(?:"+String.join("|",ExpansionTileWidgetPropertySchema.curvePresets())+")","Curves preset"),ref("Curve?"),nil()));
        }
        if(result.size()!=170) throw new IllegalStateException("Chip property inventory: "+result.size());
        return List.copyOf(result);
    }
    private static PropertyValueConstraint cursorPattern() { return new PropertyValueConstraint.StringPattern(DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(),"MouseCursor preset"); }
    private static PropertyValueConstraint ref(String type) { return new PropertyValueConstraint.DartObjectReferenceValues(type); }
    private static PropertyValueConstraint nil() { return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL); }
    private static List<PropertyValueConstraint> nullableRef(String type) { return List.of(ref(type),nil()); }
    private static List<PropertyValueConstraint> withNull(List<PropertyValueConstraint> source) { var c=new ArrayList<>(source);c.add(nil());return List.copyOf(c); }
    private static void add(List<PropertyDefinition> ps,String name,List<PropertyValueConstraint> c) { ps.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(ps.size(),false),c,Optional.empty())); }
    public static WidgetNode starterLabel(StableId owner) {
        return new WidgetNode(new StableId(UUID.nameUUIDFromBytes(("flutter-designer:Chip:label:"+owner).getBytes(StandardCharsets.UTF_8))),
                TextWidgetPropertySchema.TEXT_TYPE,Map.of(new PropertyName("data"),new PropertyValue.StringValue("Chip")),Map.of());
    }
    public static List<String> presets(String name) {
        if(name.startsWith("mouseCursor")) return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
        if(name.startsWith("chipAnimationStyle")) return name.endsWith("Curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : name.endsWith("Us") ? List.of() : name.equals("chipAnimationStyle") ? List.of() : List.of("noAnimation");
        if(name.startsWith("side") && name.endsWith("Mode")) return List.of("border","inherit");
        return DialogWidgetPropertySchema.presets(name);
    }
    public static String group(String name) {
        if(name.equals("onDeleted")) return "Events";
        if(name.startsWith("labelStyle")) return "Label text style";
        if(name.startsWith("chipAnimationStyle")) return "Animation";
        if(name.startsWith("iconTheme")) return "Icon theme";
        if(name.startsWith("mouseCursor") || Set.of("focusNode","autofocus","deleteButtonTooltipMessage").contains(name)) return "Interaction";
        if(name.startsWith("side")) return "Border side";
        if(name.startsWith("color")) return "State colors";
        return DialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        if(textBinding(new PropertyName(name)).isPresent()) return textBinding(new PropertyName(name)).orElseThrow().description();
        if(name.startsWith("chipAnimationStyle")) return "Whole ChipAnimationStyle? or four independent local AnimationStyle groups; each supports noAnimation, typed sources/null, both durations and all 43 curves. Pinned Flutter consumes durations at state initialization, ignores these curves, and does not reconfigure controllers on ordinary updates. Recreate using a new Key to apply changed durations. Canvas suppresses autonomous animations.";
        if(name.equals("onDeleted")) return "Optional VoidCallback?; a handler reveals the delete button. Called on delete activation, but does not remove Chip. Canvas shows a harmless delete affordance for a bound handler and never executes user code.";
        if(name.equals("focusNode")) return "Strict FocusNode?; the application owns and disposes it. Canvas never borrows the source node or requests focus.";
        if(name.equals("deleteButtonTooltipMessage")) return "Literal String, strict String? or null. Null uses localized deleteButtonTooltip; empty string suppresses the native delete tooltip.";
        if(name.equals("labelStyle")) return "Whole TextStyle? or all 31 local TextStyle leaves, exclusive and atomic. Omission/null follows ChipTheme then native M2/M3 defaults.";
        if(name.startsWith("mouseCursor")) return "MouseCursor? source/preset or nine state buckets. Local states require Default; first matching Disabled/Error/Dragged/Pressed/Selected/ScrolledUnder/Hovered/Focused wins, then Default.";
        if(name.startsWith("color")) return "Nullable WidgetStateProperty<Color?> or nine literal/theme/null state buckets. First matching state wins, including explicit null; omission/null resolves ChipTheme/native defaults.";
        if(name.startsWith("side")) return "BorderSide? source/null or local four-field side plus eight state buckets. Stateful must be true for state buckets; inherit returns null. Side overrides the selected OutlinedBorder side; omission preserves native theme precedence.";
        if(name.startsWith("iconTheme")) return "Whole IconThemeData? or all nine local fields, mutually exclusive. Null/omission follows ChipTheme/native icon defaults.";
        if(name.startsWith("visualDensity")) return "Whole VisualDensity? or independent local horizontal/vertical -4..4. An omitted local peer uses constructor zero, not the ambient density axis.";
        if(name.endsWith("BoxConstraints")) return "Nonnegative normalized BoxConstraints, strict BoxConstraints? or null. Native constraints govern avatar/delete icon space; deleteIcon is hidden without onDeleted.";
        if(name.equals("shape")) return "OutlinedBorder? source/null or ten local outlined shape families. Not arbitrary ShapeBorder. Native ChipTheme and M2/M3 defaults apply when absent.";
        if(name.equals("clipBehavior")) return "Non-null Clip; default none.";
        return "Typed native Chip argument. Null/omission preserves native ChipTheme and Material 2/3 defaults; Canvas does not execute project sources.";
    }
    private ChipWidgetPropertySchema() {}
}
