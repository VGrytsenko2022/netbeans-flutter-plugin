package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Flutter 3.44.8 ValueListenableBuilder with paired explicit T and fixed child protocols. */
public final class ValueListenableBuilderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ValueListenableBuilder");
    public static final WidgetTypeId SLIVER_TYPE = new WidgetTypeId("flutter.widgets.ValueListenableBuilder.sliver");
    public static final String SOURCE_TYPE = "ValueListenable<Object>";
    public static final String CALLBACK_TYPE = "ValueWidgetBuilder<Object>";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || SLIVER_TYPE.equals(type); }
    public static String description(WidgetTypeId type) {
        return "Rebuilds from the current ValueListenable<T>. Value listenable and Builder must share the selected T, including nullability. "
                + "Builder is Widget Function(BuildContext, T, Widget?); Child is optional and passed unchanged. "
                + (SLIVER_TYPE.equals(type) ? "Sliver placement requires a sliver Child/result. " : "Box placement requires a box Child/result. ")
                + "Both placements generate the unnamed ValueListenableBuilder<T> constructor. "
                + "The source owner disposes its notifier/controller; Flutter only subscribes, replaces and removes listeners. "
                + "Canvas never evaluates project sources or callbacks; it displays Child with an inert source. "
                + "Value type edits can atomically update T, nullability, source and Builder. "
                + "The Constant default preset uses a built-in zero/empty value or nullable null; a non-nullable project type requires a source reference. "
                + "Use a project typedef for a closed complex generic type. Key uses shared identity; Builder is not an Event.";
    }
    public static List<PropertyDefinition> properties() {
        return List.of(presetReference("valueListenable", 0, "constant", SOURCE_TYPE),
                presetReference("builder", 1, "child", CALLBACK_TYPE),
                new PropertyDefinition(new PropertyName("valueType"), DartParameter.named(3, true),
                    List.of(new PropertyValueConstraint.StringPattern("(?:String|int|double|num|bool|Object)", "value type"),
                            new PropertyValueConstraint.DartObjectReferenceValues("Type")),
                    Optional.of(new PropertyValue.StringValue("double"))),
                new PropertyDefinition(new PropertyName("nullableValueType"), DartParameter.named(4, false),
                    List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()));
    }
    private static PropertyDefinition presetReference(String name, int order, String preset, String type) {
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, true),
                List.of(new PropertyValueConstraint.StringPattern(preset, preset),
                        new PropertyValueConstraint.DartObjectReferenceValues(type)),
                Optional.of(new PropertyValue.StringValue(preset)));
    }
    public static Optional<String> valueTypeError(WidgetNode node) {
        var type = node.properties().get(new PropertyName("valueType"));
        if (type instanceof PropertyValue.DartObjectReferenceValue reference) {
            if (!RadioWidgetPropertySchema.isTypeReference(reference)
                    || Set.of("dynamic", "void", "Never", "Null").contains(reference.rootSymbol()))
                return Optional.of("Value type requires a concrete simple class, enum or closed typedef reference, without members or invocation.");
            if (!RadioWidgetPropertySchema.nullableValueType(node)
                    && node.properties().get(new PropertyName("valueListenable")) instanceof PropertyValue.StringValue)
                return Optional.of("A non-nullable project type requires a typed ValueListenable source. Change Type and Value listenable together in the Value type dialog.");
        }
        return Optional.empty();
    }
    public static String constantLiteral(WidgetNode node) {
        if (RadioWidgetPropertySchema.nullableValueType(node)) return "null";
        var type = node.properties().get(new PropertyName("valueType"));
        if (!(type instanceof PropertyValue.StringValue builtin))
            throw new IllegalArgumentException("A non-nullable project type has no fabricated default value.");
        return switch (builtin.value()) {
            case "String" -> "''";
            case "bool" -> "false";
            case "double" -> "0.0";
            case "int", "num", "Object" -> "0";
            default -> throw new IllegalArgumentException("Unreviewed value type");
        };
    }
    private ValueListenableBuilderWidgetPropertySchema() {}
}
