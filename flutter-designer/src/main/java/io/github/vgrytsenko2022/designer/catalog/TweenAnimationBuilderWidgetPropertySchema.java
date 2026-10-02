package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Flutter 3.44.8 generic TweenAnimationBuilder and fixed box/sliver placements. */
public final class TweenAnimationBuilderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.TweenAnimationBuilder");
    public static final WidgetTypeId SLIVER_TYPE = new WidgetTypeId("flutter.widgets.TweenAnimationBuilder.sliver");
    public static final String SOURCE_TYPE = "Tween<Object>";
    public static final String CALLBACK_TYPE = "ValueWidgetBuilder<Object>";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || SLIVER_TYPE.equals(type); }
    public static String description(WidgetTypeId type) {
        return "Implicitly animates an owned Tween<T>; Builder must be Widget Function(BuildContext, T, Widget?). "
                + (SLIVER_TYPE.equals(type) ? "Sliver Child/result required. " : "Box Child/result required. ")
                + "Both placements generate the unnamed TweenAnimationBuilder<T> constructor. "
                + "Flutter mutates the supplied Tween. Transfer a fresh, exclusive instance (prefer a factory); never share or mutate it after transfer. "
                + "Tween.end must be non-null even for nullable T. A null begin starts at end without an initial animation. "
                + "Target changes animate from the current value; new begin values are ignored after mounting. "
                + "Change Value type, nullability, Tween and Builder together in the atomic type editor. "
                + "The Default tween preset creates a fresh 0-to-1 numeric tween or a constant empty/false built-in value. Project types require a typed Tween reference/factory. "
                + "Duration is required; Curve defaults to linear. On end is an optional native Event, including completion of an initial animation. "
                + "Canvas never executes project tweens, curves, durations, builders or callbacks; it displays Child with an isolated preview tween. "
                + "Use project typedefs for closed complex or SDK value types. Key uses shared identity.";
    }
    public static List<PropertyDefinition> properties() {
        var values = ValueListenableBuilderWidgetPropertySchema.properties();
        var animation = SliverAnimatedOpacityWidgetPropertySchema.properties();
        return List.of(
                new PropertyDefinition(new PropertyName("tween"), DartParameter.named(0, true),
                    List.of(new PropertyValueConstraint.StringPattern("default", "fresh default tween"),
                            new PropertyValueConstraint.DartObjectReferenceValues(SOURCE_TYPE)),
                    Optional.of(new PropertyValue.StringValue("default"))),
                new PropertyDefinition(new PropertyName("durationUs"), DartParameter.named(1, true),
                    animation.get(2).constraints(), animation.get(2).creationDefault()),
                new PropertyDefinition(new PropertyName("curve"), DartParameter.named(2, false),
                    animation.get(1).constraints(), Optional.empty()),
                new PropertyDefinition(new PropertyName("builder"), DartParameter.named(3, true),
                    values.get(1).constraints(), values.get(1).creationDefault()),
                new PropertyDefinition(new PropertyName("onEnd"), DartParameter.named(4, false),
                    animation.get(3).constraints(), Optional.empty()),
                new PropertyDefinition(new PropertyName("valueType"), DartParameter.named(6, true),
                    values.get(2).constraints(), values.get(2).creationDefault()),
                new PropertyDefinition(new PropertyName("nullableValueType"), DartParameter.named(7, false),
                    values.get(3).constraints(), Optional.empty()));
    }
    public static Optional<String> valueTypeError(WidgetNode node) {
        var value = node.properties().get(new PropertyName("valueType"));
        if (value instanceof PropertyValue.DartObjectReferenceValue type) {
            if (!RadioWidgetPropertySchema.isTypeReference(type)
                    || Set.of("dynamic", "void", "Never", "Null").contains(type.rootSymbol()))
                return Optional.of("Value type requires a concrete simple class, enum or closed typedef, without members or invocation.");
            if (node.properties().get(new PropertyName("tween")) instanceof PropertyValue.StringValue)
                return Optional.of("A project type requires a typed Tween reference/factory with a non-null end, even for nullable T. Change Value type and Tween together.");
        }
        return Optional.empty();
    }
    /** Class for a fresh default instance; subclasses fix int/double interpolation semantics. */
    public static String presetClass(WidgetNode node) {
        var value = node.properties().get(new PropertyName("valueType"));
        if (!(value instanceof PropertyValue.StringValue type))
            throw new IllegalArgumentException("Project types require a typed Tween with a non-null end.");
        return switch (type.value()) {
            case "int" -> "IntTween";
            case "double", "num" -> "Tween";
            case "String", "bool", "Object" -> "ConstantTween";
            default -> throw new IllegalArgumentException("Unreviewed value type");
        };
    }
    private TweenAnimationBuilderWidgetPropertySchema() {}
}
