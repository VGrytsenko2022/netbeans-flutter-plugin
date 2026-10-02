package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** All 20 SnackBar and seven SnackBarAction arguments in pinned Flutter 3.44.8. */
public final class SnackBarWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.SnackBar");
    public static final WidgetTypeId ACTION = new WidgetTypeId("flutter.material.SnackBarAction");
    public static final String DESCRIPTION = "Transient Material message with required Content and optional exact SnackBarAction. "
            + "The caller owns ScaffoldMessenger.showSnackBar, queuing, timeouts and closed results. "
            + "Designer creation supplies Text content and a stopped animation at 1 for safe direct mounting; "
            + "ScaffoldMessenger replaces that animation when presenting the bar. Canvas never executes project sources or dismisses the design.";
    public static final String ACTION_DESCRIPTION = "One-shot action with required label and VoidCallback. Flutter disables the button after activation, "
            + "then hides the current SnackBar with reason action. Creation/reset uses a no-action handler; it is not a disabled button. "
            + "Canvas suppresses activation and never executes user code.";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || ACTION.equals(type); }
    public static List<PropertyDefinition> properties(boolean action) {
        var result = new ArrayList<PropertyDefinition>();
        var shared = DialogWidgetPropertySchema.properties(false).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        var names = action ? List.of("key","textColor","disabledTextColor","backgroundColor","disabledBackgroundColor","label","onPressed")
                : List.of("key","backgroundColor","elevation","margin","padding","width","shape","hitTestBehavior","behavior",
                        "actionOverflowThreshold","showCloseIcon","closeIconColor","durationUs","persist","animation","onVisible","dismissDirection","clipBehavior");
        for (String name : names) {
            List<PropertyValueConstraint> constraints = switch (name) {
                case "textColor","disabledTextColor","disabledBackgroundColor","closeIconColor" -> shared.get("backgroundColor");
                case "margin","padding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true,true), ref("EdgeInsetsGeometry?"), nil());
                case "width" -> numbers(BigDecimal.ZERO, false, null, true, "double?");
                case "actionOverflowThreshold" -> numbers(BigDecimal.ZERO, true, BigDecimal.ONE, true, "double?");
                case "animation" -> numbers(BigDecimal.ZERO, true, BigDecimal.ONE, true, "Animation<double>?");
                case "showCloseIcon","persist" -> List.of(any(PropertyValueKind.BOOLEAN), nil());
                case "durationUs" -> List.of(new PropertyValueConstraint.IntegerRange(DartNumericLiterals.MAX_PORTABLE_INTEGER.negate(), DartNumericLiterals.MAX_PORTABLE_INTEGER),ref("Duration"));
                case "onVisible" -> List.of(ref("VoidCallback?"),nil());
                case "onPressed" -> List.of(new PropertyValueConstraint.StringPattern("noop","no action"),ref("VoidCallback"));
                case "label" -> List.of(new PropertyValueConstraint.StringLength(0,16384),ref("String"));
                case "hitTestBehavior" -> enums("package:flutter/rendering.dart","HitTestBehavior",true,"deferToChild","opaque","translucent");
                case "behavior" -> enums("package:flutter/material.dart","SnackBarBehavior",true,"fixed","floating");
                case "dismissDirection" -> enums("package:flutter/widgets.dart","DismissDirection",true,"vertical","horizontal","endToStart","startToEnd","up","down","none");
                case "clipBehavior" -> enums("package:flutter/widgets.dart","Clip",false,"none","hardEdge","antiAlias","antiAliasWithSaveLayer");
                default -> Objects.requireNonNull(shared.get(name),name);
            };
            Optional<PropertyValue> initial = switch(name) {
                case "animation" -> Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE));
                case "label" -> Optional.of(new PropertyValue.StringValue("Action"));
                case "onPressed" -> Optional.of(new PropertyValue.StringValue("noop"));
                default -> Optional.empty();
            };
            result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(result.size(),name.equals("label")||name.equals("onPressed")),constraints,initial));
        }
        if (!action) for (String name : CardWidgetPropertySchema.builtInShapePropertyNames())
            result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(result.size(),false),shared.get(name),Optional.empty()));
        return List.copyOf(result);
    }
    private static List<PropertyValueConstraint> numbers(BigDecimal min,boolean inclusive,BigDecimal max,boolean maxInclusive,String type) {
        BigInteger intMin = inclusive ? min.toBigIntegerExact() : min.toBigIntegerExact().add(BigInteger.ONE);
        return List.of(new PropertyValueConstraint.IntegerRange(intMin,max==null?DartNumericLiterals.MAX_PORTABLE_INTEGER:max.toBigIntegerExact()),
                new PropertyValueConstraint.DoubleRange(min,inclusive,max,maxInclusive),ref(type),nil());
    }
    private static List<PropertyValueConstraint> enums(String library,String type,boolean nullable,String... members) {
        var result = new ArrayList<PropertyValueConstraint>();
        result.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference(library,type),List.of(members)));
        if(nullable) result.add(nil());
        return List.copyOf(result);
    }
    private static PropertyValueConstraint ref(String type) { return new PropertyValueConstraint.DartObjectReferenceValues(type); }
    private static PropertyValueConstraint any(PropertyValueKind kind) { return new PropertyValueConstraint.AnyValue(kind); }
    private static PropertyValueConstraint nil() { return any(PropertyValueKind.NULL); }
    public static WidgetNode starterContent(StableId owner) {
        var id = new StableId(UUID.nameUUIDFromBytes(("flutter-designer:SnackBar:content:"+owner).getBytes(StandardCharsets.UTF_8)));
        return new WidgetNode(id,new WidgetTypeId("flutter.widgets.Text"),Map.of(new PropertyName("data"),new PropertyValue.StringValue("Message")),Map.of());
    }
    public static String group(String name) {
        return name.startsWith("on") ? "Events" : name.equals("label") ? "Content"
                : Set.of("margin","padding","width","behavior","actionOverflowThreshold").contains(name) ? "Layout"
                : Set.of("durationUs","persist","animation","dismissDirection","hitTestBehavior","showCloseIcon").contains(name) ? "Behavior"
                : DialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        return switch(name) {
            case "label" -> "Required label: literal (including empty) or verified non-null String reference/getter/factory.";
            case "onPressed" -> ACTION_DESCRIPTION;
            case "onVisible" -> "Optional VoidCallback? called the first time the bar becomes fully visible. Source stays user-owned; Canvas never executes it.";
            case "durationUs" -> "Signed portable integer microseconds or verified Duration; native default 4000000 (4 seconds). Negative durations are retained and timers treat them as zero. Used by ScaffoldMessenger, not direct widget mounting.";
            case "persist" -> "Null/unset defaults to whether Action exists. True prevents timeout; false permits it. Accessibility and messenger state also affect timing.";
            case "animation" -> "Local 0..1 creates AlwaysStoppedAnimation<double>; creation uses 1. Or verified Animation<double>?, null/omission. ScaffoldMessenger supplies the live animation; direct mounting with null throws in Flutter. Canvas always uses a completed animation to retain selection.";
            case "width" -> "Positive finite width, verified double? or null. Exclusive with non-null Margin; set Behavior to floating when using Width. Source must resolve a valid width at runtime.";
            case "margin" -> "Non-negative physical/directional EdgeInsetsGeometry, verified EdgeInsetsGeometry? or null. Exclusive with Width and requires floating Behavior. Pinned Flutter resolves directional margin using LTR internally.";
            case "padding" -> "Non-negative physical/directional EdgeInsetsGeometry, verified EdgeInsetsGeometry? or null. Omission follows native fixed/floating/action/close-icon spacing.";
            case "behavior" -> "Fixed or floating; null/omission follows SnackBarTheme then fixed. Set floating explicitly before Width or Margin.";
            case "actionOverflowThreshold" -> "Fraction in 0..1, verified double? or null. Action wraps below Content when its measured width exceeds this share; default 0.25.";
            case "hitTestBehavior" -> "Nullable HitTestBehavior. Native fallback is deferToChild with margin/theme inset padding, otherwise opaque.";
            case "dismissDirection" -> "All seven DismissDirection values. Null/omission follows SnackBarTheme then down. Canvas suppresses dismissal.";
            case "clipBehavior" -> "Non-null Clip; omission defaults to hardEdge.";
            case "showCloseIcon" -> "Nullable flag; omission follows SnackBarTheme then false. Native close action hides the current bar with dismiss reason; Canvas blocks it.";
            case "backgroundColor" -> "Literal/theme Color, verified Color? or null. For SnackBarAction a source may be WidgetStateColor; then Disabled background color must be null/omitted (native runtime assertion).";
            case "key" -> "String ValueKey, verified Key?, null or omission; separate from the stable Designer ID.";
            case "shape" -> "Verified ShapeBorder? or null; mutually exclusive with ten local shape families. Null/omission preserves SnackBarTheme/native defaults.";
            case "elevation" -> "Non-negative finite number, verified double? or null; null/omission preserves SnackBarTheme/native defaults.";
            default -> CardWidgetPropertySchema.builtInShapePropertyNames().contains(name) ? DialogWidgetPropertySchema.help(name)
                    : "Literal/theme Color, verified Color? or null; native SnackBarTheme/Material defaults. Key and shape use their shared typed editors.";
        };
    }
    private SnackBarWidgetPropertySchema() {}
}
