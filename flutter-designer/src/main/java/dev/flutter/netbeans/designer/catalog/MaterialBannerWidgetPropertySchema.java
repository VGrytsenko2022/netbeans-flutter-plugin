package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** All 18 MaterialBanner arguments in pinned Flutter 3.44.8. */
public final class MaterialBannerWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.MaterialBanner");
    public static final String DESCRIPTION = "Persistent non-modal Material banner with required Content, nonempty Actions and optional Leading. "
            + "Static when animation is omitted/null; the caller owns ScaffoldMessenger presentation, queuing and closed results. "
            + "Actions do not automatically dismiss the banner. Canvas never executes project sources or handlers.";
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        var shared = AlertDialogWidgetPropertySchema.properties(false).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        for (String name : List.of("key", "contentTextStyle", "elevation", "backgroundColor", "surfaceTintColor",
                "shadowColor", "dividerColor", "padding", "margin", "leadingPadding", "forceActionsBelow",
                "overflowAlignment", "animation", "onVisible", "minActionBarHeight")) {
            List<PropertyValueConstraint> constraints = switch (name) {
                case "dividerColor" -> shared.get("backgroundColor");
                case "padding", "margin", "leadingPadding" -> shared.get("contentPadding");
                case "forceActionsBelow" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
                case "overflowAlignment" -> List.of(new PropertyValueConstraint.EnumValues(
                        new DartSymbolReference("package:flutter/widgets.dart", "OverflowBarAlignment"), List.of("start", "end", "center")));
                case "animation" -> List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, BigInteger.ONE),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, BigDecimal.ONE, true),
                        new PropertyValueConstraint.DartObjectReferenceValues("Animation<double>?"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "onVisible" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback?"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "minActionBarHeight" -> List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, null, true),
                        new PropertyValueConstraint.DartObjectReferenceValues("double"));
                default -> Objects.requireNonNull(shared.get(name), name);
            };
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(), false), constraints, Optional.empty()));
        }
        BuiltInWidgetCatalog.appendTextStyleProperties(result, "contentTextStyle", result.size());
        if (result.size() != 46) throw new IllegalStateException("MaterialBanner property inventory: " + result.size());
        return List.copyOf(result);
    }
    private static StableId derived(StableId owner, String role) {
        return new StableId(UUID.nameUUIDFromBytes(("flutter-designer:MaterialBanner:" + owner + ":" + role).getBytes(StandardCharsets.UTF_8)));
    }
    private static WidgetNode text(StableId owner, String role, String value) {
        return new WidgetNode(derived(owner, role), TextWidgetPropertySchema.TEXT_TYPE,
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    public static Map<SlotName, WidgetSlot> starterSlots(StableId owner) {
        var button = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.TextButton")).orElseThrow(), derived(owner, "action"));
        var action = new WidgetNode(button.id(), button.type(), button.properties(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(owner, "actionLabel", "Action"))));
        return Map.of(new SlotName("content"), WidgetSlot.SingleSlot.of(text(owner, "content", "Message")),
                new SlotName("actions"), new WidgetSlot.ListSlot(List.of(action)));
    }
    public static String group(String name) {
        if (name.startsWith("contentTextStyle")) return "Content text style";
        if (name.equals("onVisible")) return "Events";
        if (name.equals("animation")) return "Behavior";
        if (Set.of("padding", "margin", "leadingPadding", "forceActionsBelow", "overflowAlignment", "minActionBarHeight").contains(name)) return "Layout";
        return DialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        if (AlertDialogWidgetPropertySchema.styleFamily(new PropertyName(name)).isPresent())
            return AlertDialogWidgetPropertySchema.styleBinding(new PropertyName(name)).orElseThrow().description();
        return switch (name) {
            case "contentTextStyle" -> "Whole verified TextStyle? or all 31 local TextStyle leaves, mutually exclusive. Null/omission follows MaterialBannerTheme then bodyMedium; content text scaling is natively capped at 1.5.";
            case "padding", "leadingPadding", "margin" -> "Non-negative physical/directional insets, verified EdgeInsetsGeometry? or null. Native defaults depend on action layout/elevation; directional values follow LTR/RTL.";
            case "forceActionsBelow" -> "False/unset places a single Action beside Content. True or multiple Actions puts them below, with native OverflowBar wrapping.";
            case "overflowAlignment" -> "Start, end or center alignment when Actions overflow into a column; omission defaults to end.";
            case "minActionBarHeight" -> "Non-negative finite number or verified non-null double. Omission preserves 52 logical pixels. Sources must satisfy the runtime bounds.";
            case "animation" -> "Omitted/null is a static banner. Local 0..1 generates AlwaysStoppedAnimation<double>; a verified Animation<double>? owns transitions. ScaffoldMessenger supplies its own animation. Canvas keeps supplied animations completed and never runs project code.";
            case "onVisible" -> "Optional VoidCallback? called once when animation first completes. A static banner or already-stopped animation does not trigger it. Actions own their separate Events and must explicitly request dismissal.";
            case "elevation" -> "Non-negative finite number, verified double? or null. Theme fallback then 0; zero shows a divider and pushes Scaffold body when messenger-presented.";
            case "key" -> "String ValueKey, verified Key?, explicit null or omission; independent of Designer stable ID.";
            default -> "Literal/theme Color, verified Color? or null. Omission preserves MaterialBannerTheme and Material 2/3 defaults; surface tint is supported, but Flutter recommends tone-based surface colors instead.";
        };
    }
    private MaterialBannerWidgetPropertySchema() { }
}
