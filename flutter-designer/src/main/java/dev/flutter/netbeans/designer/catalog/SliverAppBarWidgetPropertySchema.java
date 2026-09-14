package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Flutter 3.44.8: all three native SliverAppBar constructors, sharing AppBar style leaves. */
public final class SliverAppBarWidgetPropertySchema {
    public static final List<String> TYPES = List.of("flutter.material.SliverAppBar",
            "flutter.material.SliverAppBar.medium", "flutter.material.SliverAppBar.large");
    public static final Set<String> EXCLUDED = Set.of("notificationPredicate", "toolbarOpacity", "bottomOpacity", "animateColor");
    public static final Map<String, String> WHOLE_TYPES = Map.of(
            "shape", "ShapeBorder?", "iconTheme", "IconThemeData?", "actionsIconTheme", "IconThemeData?",
            "toolbarTextStyle", "TextStyle?", "titleTextStyle", "TextStyle?", "systemOverlayStyle", "SystemUiOverlayStyle?");
    public static final Set<String> NULLABLE_DIRECT = Set.of("backgroundColor", "centerTitle", "elevation",
            "scrolledUnderElevation", "shadowColor", "surfaceTintColor", "foregroundColor",
            "titleSpacing", "leadingWidth", "clipBehavior", "actionsPadding");
    public static final List<String> EXTRA = List.of("forceElevated", "collapsedHeight", "expandedHeight", "floating",
            "pinned", "snap", "stretch", "stretchTriggerOffset", "onStretchTrigger");
    public static final String DESCRIPTION = "Native scrolling Material app bar. Standard, medium and large constructors preserve their SDK defaults. "
            + "Medium/large default to pinned with a 64px toolbar; standard defaults to unpinned with a 56px toolbar. "
            + "Snap requires Floating; the editor updates that pair atomically. Collapsed height must be at least Toolbar height. "
            + "Leading, Title, Actions, Flexible space and PreferredSizeWidget Bottom are independent slots. "
            + "Whole project styles and callbacks are retained and generated but never executed in the isolated Canvas.";
    public static boolean isType(WidgetTypeId type) { return TYPES.contains(type.value()); }
    public static double toolbarDefault(WidgetTypeId type) { return type.value().equals(TYPES.getFirst()) ? 56 : 64; }
    public static List<String> localFamily(String whole) {
        return BuiltInWidgetCatalog.appBarProperties().stream().map(p -> p.name().value())
                .filter(n -> n.startsWith(whole) && !n.equals(whole)).toList();
    }
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        for (var property : BuiltInWidgetCatalog.appBarProperties()) {
            String name = property.name().value();
            if (EXCLUDED.contains(name)) continue;
            var constraints = new ArrayList<>(property.constraints());
            if (name.equals("actionsPadding")) constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"));
            if (NULLABLE_DIRECT.contains(name)) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            result.add(new PropertyDefinition(property.name(), DartParameter.named(result.size(), false), constraints, Optional.empty()));
        }
        for (String name : new TreeSet<>(WHOLE_TYPES.keySet())) result.add(property(name, result.size(),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues(WHOLE_TYPES.get(name))), true));
        for (String name : EXTRA) {
            List<PropertyValueConstraint> constraints = switch (name) {
                case "collapsedHeight", "expandedHeight" -> numbers(false);
                case "stretchTriggerOffset" -> numbers(true);
                case "onStretchTrigger" -> List.of(
                    new PropertyValueConstraint.StringPattern("noop", "asynchronous no-op stretch callback"),
                    new PropertyValueConstraint.DartObjectReferenceValues("AsyncCallback"));
                default -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
            };
            result.add(property(name, result.size(), constraints,
                    Set.of("collapsedHeight", "expandedHeight", "onStretchTrigger").contains(name)));
        }
        return List.copyOf(result);
    }
    private static PropertyDefinition property(String name, int order, List<PropertyValueConstraint> values, boolean nullable) {
        var constraints = new ArrayList<>(values);
        if (nullable) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, false), constraints, Optional.empty());
    }
    private static List<PropertyValueConstraint> numbers(boolean positive) {
        return List.of(new PropertyValueConstraint.IntegerRange(positive ? java.math.BigInteger.ONE : java.math.BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                new PropertyValueConstraint.DoubleRange(java.math.BigDecimal.ZERO, !positive, null, true));
    }
    private SliverAppBarWidgetPropertySchema() {}
}
