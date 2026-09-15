package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 Dialog (14 arguments) and Dialog.fullscreen (6 arguments). */
public final class DialogWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.Dialog");
    public static final WidgetTypeId FULLSCREEN_TYPE = new WidgetTypeId("flutter.material.Dialog.fullscreen");
    public static final String DESCRIPTION = "Material dialog surface with optional Child. Show it using showDialog/DialogRoute; "
            + "the route owns dismissal, barrier, focus and result. Child widgets own their Events. "
            + "Fullscreen selects the native surface constructor, not a fullscreen route. Canvas never executes project sources.";
    public static final List<String> ROLES = List.of("none", "tab", "tabBar", "tabPanel", "dialog", "alertDialog", "table", "cell", "row",
            "columnHeader", "dragHandle", "spinButton", "comboBox", "menuBar", "menu", "menuItem", "menuItemCheckbox", "menuItemRadio",
            "list", "listItem", "form", "tooltip", "loadingSpinner", "progressBar", "hotKey", "radioGroup", "status", "alert",
            "complementary", "contentInfo", "main", "navigation", "region");
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || FULLSCREEN_TYPE.equals(type); }
    public static List<PropertyDefinition> properties(boolean fullscreen) {
        var result = new ArrayList<PropertyDefinition>();
        add(result, "key", List.of(new PropertyValueConstraint.StringLength(0,4096), ref("Key?"), nil()));
        add(result, "backgroundColor", colors());
        if (!fullscreen) {
            add(result, "elevation", List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO,true,null,true), ref("double?"), nil()));
            add(result, "shadowColor", colors());
            add(result, "surfaceTintColor", colors());
        }
        add(result, "insetAnimationDurationUs", List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER), ref("Duration")));
        add(result, "insetAnimationCurve", List.of(new PropertyValueConstraint.StringPattern(
                "(?:" + String.join("|", ExpansionTileWidgetPropertySchema.curvePresets()) + ")", "Curves preset"), ref("Curve")));
        if (!fullscreen) {
            add(result, "insetPadding", List.of(new PropertyValueConstraint.EdgeInsetsValues(true,false), ref("EdgeInsets?"), nil()));
            add(result, "clipBehavior", List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart","Clip"),
                    List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer")),nil()));
            add(result, "shape", List.of(ref("ShapeBorder?"),nil()));
            add(result, "alignment", List.of(new PropertyValueConstraint.AlignmentGeometryValues(),ref("AlignmentGeometry?"),nil()));
        }
        add(result, "semanticsRole", List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:ui","SemanticsRole"),ROLES)));
        if (!fullscreen) {
            add(result, "constraints", List.of(new PropertyValueConstraint.BoxConstraintsValues(),ref("BoxConstraints?"),nil()));
            for (var p : BuiltInWidgetCatalog.cardProperties())
                if (CardWidgetPropertySchema.builtInShapePropertyNames().contains(p.name().value())) add(result,p.name().value(),p.constraints());
        }
        if (result.size() != (fullscreen ? 5 : 34)) throw new IllegalStateException("Dialog property inventory changed");
        return List.copyOf(result);
    }
    private static List<PropertyValueConstraint> colors() {
        return List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR), new PropertyValueConstraint.ThemeTokenValues(
                MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()), ref("Color?"), nil());
    }
    private static void add(List<PropertyDefinition> list,String name,List<PropertyValueConstraint> constraints) {
        list.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(list.size(),false),constraints,Optional.empty()));
    }
    private static PropertyValueConstraint ref(String type) { return new PropertyValueConstraint.DartObjectReferenceValues(type); }
    private static PropertyValueConstraint nil() { return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL); }
    public static List<String> presets(String name) { return name.equals("shapeKind") ? CardWidgetPropertySchema.shapeKinds()
            : name.equals("insetAnimationCurve") ? ExpansionTileWidgetPropertySchema.curvePresets() : List.of(); }
    public static String label(String name) { return name.equals("insetAnimationDurationUs") ? "Inset animation duration (microseconds)"
            : CardWidgetPropertySchema.find(new PropertyName(name)).map(CardWidgetPropertySchema.Definition::displayName).orElse(DatePickerDialogWidgetPropertySchema.label(name)); }
    public static String group(String name) { return name.startsWith("shape") ? "Shape" : name.startsWith("insetAnimation") ? "Animation"
            : Set.of("insetPadding","alignment","constraints").contains(name) ? "Layout" : name.equals("semanticsRole") ? "Accessibility" : "Appearance"; }
    public static String help(String name) {
        if (CardWidgetPropertySchema.builtInShapePropertyNames().contains(name))
            return CardWidgetPropertySchema.find(new PropertyName(name)).orElseThrow().description();
        return switch(name) {
            case "key" -> "String ValueKey, verified Key reference, null or omission. Dialog properties update normally; no initial-only state.";
            case "insetAnimationDurationUs" -> "Non-negative microseconds or verified Duration. Native default: 100ms for Dialog, zero for Dialog.fullscreen. Animates keyboard/inset changes.";
            case "insetAnimationCurve" -> "One of 43 Curves presets or verified Curve. Default decelerate. Project curves are not executed in Canvas.";
            case "insetPadding" -> "Non-negative physical EdgeInsets, verified EdgeInsets? or null. Directional insets are not accepted by Flutter. Null/omission inherits DialogTheme then horizontal 40 / vertical 24. Keyboard viewInsets are added.";
            case "shape" -> "Verified ShapeBorder? or null, exclusive with ten local shape families. Null/omission inherits DialogTheme and native M2/M3 defaults.";
            case "alignment" -> "Physical or directional alignment, verified AlignmentGeometry? or null. Null/omission inherits DialogTheme then center.";
            case "constraints" -> "BoxConstraints, verified BoxConstraints? or null. Null/omission inherits DialogTheme then minWidth 280; no automatic maxWidth 560.";
            case "semanticsRole" -> "Native dart:ui SemanticsRole (33 roles). Default dialog; no callback or route behavior is implied.";
            case "elevation" -> "Non-negative elevation, verified double? or null; null/omission inherits DialogTheme and native defaults.";
            case "clipBehavior" -> "Nullable Clip override; omission/null retains native DialogTheme fallback.";
            default -> "Literal/theme color, verified Color? or null. Null/omission inherits DialogTheme and native Material 2/3 defaults.";
        };
    }
    private DialogWidgetPropertySchema() { }
}
