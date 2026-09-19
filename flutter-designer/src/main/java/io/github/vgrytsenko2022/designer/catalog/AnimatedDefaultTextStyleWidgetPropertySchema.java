package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigInteger;
import java.util.*;

/** Complete Flutter 3.44.8 constructor and shared typed local TextStyle projection. */
public final class AnimatedDefaultTextStyleWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedDefaultTextStyle");
    public static final String DESCRIPTION = "Animates the required default TextStyle of its required box Child. "
            + "Create by wrapping an existing child. Local style exposes every TextStyle constructor field, or use a typed TextStyle reference. "
            + "Paragraph fields update immediately. Duration is required (creation 300 ms). Canvas never executes project references.";
    public static boolean styleLeaf(PropertyName name) {
        return TextWidgetPropertySchema.find(name).map(d -> switch (d.target()) {
            case TEXT_STYLE_THEME, TEXT_STYLE, TEXT_STYLE_LOCALE, TEXT_STYLE_DECORATION -> true;
            default -> false;
        }).orElse(false);
    }
    public static boolean heightLeaf(PropertyName name) {
        return TextWidgetPropertySchema.find(name).map(d -> d.target() == TextWidgetPropertySchema.Target.TEXT_HEIGHT_BEHAVIOR).orElse(false);
    }
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        result.add(new PropertyDefinition(new PropertyName("style"), DartParameter.named(0,true),
                List.of(new PropertyValueConstraint.StringPattern("local","Local TextStyle fields"),
                        new PropertyValueConstraint.DartObjectReferenceValues("TextStyle")),
                Optional.of(new PropertyValue.StringValue("local"))));
        var text = BuiltInWidgetCatalog.text().properties();
        for (String name : List.of("textAlign","softWrap","overflow","maxLines","textWidthBasis")) {
            var source = text.stream().filter(p -> p.name().value().equals(name)).findFirst().orElseThrow();
            var constraints = new ArrayList<>(source.constraints());
            if (name.equals("textAlign")) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            if (name.equals("maxLines")) {
                constraints = new ArrayList<>(List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE, new BigInteger("9007199254740991")),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                        new PropertyValueConstraint.DartObjectReferenceValues("int?")));
            }
            result.add(new PropertyDefinition(source.name(), DartParameter.named(result.size(),false), constraints, Optional.empty()));
        }
        result.add(new PropertyDefinition(new PropertyName("textHeightBehavior"), DartParameter.named(result.size(),false),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("TextHeightBehavior?"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()));
        for (var p : text) if (styleLeaf(p.name()) || heightLeaf(p.name()))
            result.add(new PropertyDefinition(p.name(), DartParameter.named(result.size(),false), p.constraints(), Optional.empty()));
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        for (var p : List.of(animation.get(1), animation.get(2), animation.get(3)))
            result.add(new PropertyDefinition(p.name(), DartParameter.named(result.size(),p.parameter().required()), p.constraints(), p.creationDefault()));
        return List.copyOf(result);
    }
    public static String help(PropertyName name) {
        return switch(name.value()) {
            case "style" -> "Required local TextStyle or exact TextStyle reference/getter/factory. Selecting a reference clears local style fields atomically; editing a local field selects local. No merge with the outer DefaultTextStyle is invented.";
            case "textHeightBehavior" -> "Omitted, explicit null or typed TextHeightBehavior? reference/getter/factory. Mutually exclusive with the three local height behavior fields; paragraph changes are immediate.";
            case "maxLines" -> "Omitted, explicit null, positive portable integer or int? reference. Updates immediately; null permits all lines.";
            case "durationUs" -> "Required nonnegative portable integer microseconds or typed Duration reference. Creation/custom-reference preview: 300000. Zero completes immediately.";
            case "curve" -> "All 43 pinned Curves presets or typed Curve reference; default linear. TextStyle uses native interpolation, including discrete changes.";
            case "onEnd" -> "Optional native VoidCallback reference/factory or null. Configure in Events. Runs on completed style transitions, not initial mount; inert in Canvas.";
            default -> TextWidgetPropertySchema.find(name).map(TextWidgetPropertySchema.Definition::description).orElse("Native paragraph property.");
        };
    }
    private AnimatedDefaultTextStyleWidgetPropertySchema() {}
}

