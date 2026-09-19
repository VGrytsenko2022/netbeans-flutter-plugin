package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Pinned Flutter 3.44.8: 28 standard / 32 adaptive arguments, including four slots. */
public final class AlertDialogWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.AlertDialog");
    public static final WidgetTypeId ADAPTIVE_TYPE = new WidgetTypeId("flutter.material.AlertDialog.adaptive");
    public static final String DESCRIPTION = "Alert dialog with Icon, Title, Content and Actions slots. "
            + "The native route owns dismissal and results; actions own their Events. Adaptive uses Cupertino on iOS/macOS "
            + "and Material elsewhere. Cupertino ignores Material appearance properties and Icon; Material ignores adaptive "
            + "scroll controllers and inset animation. Canvas never executes project sources.";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || ADAPTIVE_TYPE.equals(type); }
    public static List<String> styleFamilies() { return List.of("titleTextStyle", "contentTextStyle"); }
    public static List<String> localStyleProperties(String family) {
        if (!styleFamilies().contains(family)) throw new IllegalArgumentException("Unknown alert style: " + family);
        return ListTileWidgetPropertySchema.localTextStyleProperties("titleTextStyle").stream()
                .map(n -> family + n.substring("titleTextStyle".length())).toList();
    }
    public static Optional<String> styleFamily(PropertyName name) {
        return styleFamilies().stream().filter(f -> localStyleProperties(f).contains(name.value())).findFirst();
    }
    public static Optional<TextWidgetPropertySchema.Definition> styleBinding(PropertyName name) {
        return styleFamily(name).flatMap(f -> TextWidgetPropertySchema.find(new PropertyName("style" + name.value().substring(f.length()))));
    }
    public static List<PropertyDefinition> properties(boolean adaptive) {
        var result = new ArrayList<PropertyDefinition>();
        var dialog = DialogWidgetPropertySchema.properties(false).stream().collect(java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        var animation = DialogWidgetPropertySchema.properties(true).stream().collect(java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        for (String name : List.of("key", "iconPadding", "iconColor", "titlePadding", "titleTextStyle",
                "contentPadding", "contentTextStyle", "actionsPadding", "actionsAlignment", "actionsOverflowAlignment",
                "actionsOverflowDirection", "actionsOverflowButtonSpacing", "buttonPadding", "backgroundColor",
                "elevation", "shadowColor", "surfaceTintColor", "semanticLabel", "insetPadding", "clipBehavior",
                "shape", "alignment", "constraints", "scrollable")) {
            List<PropertyValueConstraint> c = dialog.get(name);
            if (Set.of("iconPadding", "titlePadding", "contentPadding", "actionsPadding", "buttonPadding").contains(name))
                c = List.of(new PropertyValueConstraint.EdgeInsetsValues(true, true), ref("EdgeInsetsGeometry?"), nil());
            else if (name.equals("iconColor")) c = dialog.get("backgroundColor");
            else if (styleFamilies().contains(name)) c = List.of(ref("TextStyle?"), nil());
            else if (name.equals("actionsAlignment")) c = List.of(en("MainAxisAlignment", "start", "end", "center", "spaceBetween", "spaceAround", "spaceEvenly"), nil());
            else if (name.equals("actionsOverflowAlignment")) c = List.of(en("OverflowBarAlignment", "start", "end", "center"), nil());
            else if (name.equals("actionsOverflowDirection")) c = List.of(en("VerticalDirection", "up", "down"), nil());
            else if (name.equals("actionsOverflowButtonSpacing")) c = List.of(
                    new PropertyValueConstraint.IntegerRange(DartNumericLiterals.MAX_PORTABLE_INTEGER.negate(), DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    new PropertyValueConstraint.DoubleRange(null, true, null, true), ref("double?"), nil());
            else if (name.equals("semanticLabel")) c = List.of(new PropertyValueConstraint.StringLength(0, 16384), ref("String?"), nil());
            else if (name.equals("scrollable")) c = List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
            else if (name.equals("insetPadding") && adaptive) c = List.of(new PropertyValueConstraint.EdgeInsetsValues(true, false), ref("EdgeInsets"));
            add(result, name, Objects.requireNonNull(c, name));
        }
        if (adaptive) {
            for (String name : List.of("scrollController", "actionScrollController")) add(result, name, List.of(ref("ScrollController?"), nil()));
            for (String name : List.of("insetAnimationDurationUs", "insetAnimationCurve")) add(result, name, animation.get(name));
        }
        for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) add(result, name, dialog.get(name));
        for (String family : styleFamilies()) BuiltInWidgetCatalog.appendTextStyleProperties(result, family, result.size());
        if (result.size() != (adaptive ? 111 : 107)) throw new IllegalStateException("AlertDialog property inventory changed: " + result.size());
        return List.copyOf(result);
    }
    private static void add(List<PropertyDefinition> ps, String name, List<PropertyValueConstraint> c) {
        ps.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(ps.size(), false), c, Optional.empty()));
    }
    private static PropertyValueConstraint ref(String name) { return new PropertyValueConstraint.DartObjectReferenceValues(name); }
    private static PropertyValueConstraint nil() { return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL); }
    private static PropertyValueConstraint en(String type, String... values) {
        return new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", type), List.of(values));
    }
    public static List<String> presets(String name) { return DialogWidgetPropertySchema.presets(name); }
    public static String label(String name) { return DialogWidgetPropertySchema.label(name); }
    public static String group(String name) {
        if (name.startsWith("titleTextStyle")) return "Title text style";
        if (name.startsWith("contentTextStyle")) return "Content text style";
        if (name.startsWith("actions") || name.equals("buttonPadding")) return "Actions layout";
        if (name.endsWith("Controller") || name.equals("scrollable")) return "Scrolling";
        if (name.equals("semanticLabel")) return "Accessibility";
        return DialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        if (styleFamilies().contains(name)) return "Verified nullable TextStyle or complete local TextStyle family. Null/omission uses native DialogTheme defaults. Exclusive with all local leaves.";
        if (styleFamily(new PropertyName(name)).isPresent()) return styleBinding(new PropertyName(name)).orElseThrow().description();
        return switch (name) {
            case "insetPadding" -> "Physical non-negative EdgeInsets. Standard permits null (DialogTheme fallback); adaptive's public factory requires a non-null explicit value. Omission preserves native defaults.";
            case "iconPadding", "titlePadding", "contentPadding", "actionsPadding", "buttonPadding" -> "Non-negative physical/directional insets, verified EdgeInsetsGeometry? or null. Null/omission preserves native context-dependent spacing. Ignored by adaptive Cupertino.";
            case "scrollable" -> "Default false. Material wraps title/content in a scroll view when true; actions remain visible. Lazy viewports still require bounded intrinsic dimensions. Cupertino owns its scrolling independently.";
            case "scrollController", "actionScrollController" -> "Verified ScrollController? reference/getter/factory or null. Adaptive Cupertino only; ignored by Material. The application owns and disposes supplied controllers. Canvas uses native owned scrolling.";
            case "insetAnimationDurationUs", "insetAnimationCurve" -> "Adaptive Cupertino only; ignored by Material. " + DialogWidgetPropertySchema.help(name);
            case "semanticLabel" -> "Optional literal or verified String? announcing the dialog. Null/omission preserves native localized/title behavior; ignored by adaptive Cupertino.";
            case "actionsAlignment", "actionsOverflowAlignment", "actionsOverflowDirection", "actionsOverflowButtonSpacing" -> "Native Material OverflowBar layout. Null/omission preserves SDK and theme defaults; signed finite overflow spacing is preserved. Ignored by adaptive Cupertino.";
            default -> DialogWidgetPropertySchema.help(name);
        };
    }
    private AlertDialogWidgetPropertySchema() { }
}
