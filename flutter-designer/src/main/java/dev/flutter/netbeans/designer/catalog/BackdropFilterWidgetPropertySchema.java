package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Flutter 3.44.8 BackdropFilter, grouped constructor and BackdropGroup. */
public final class BackdropFilterWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.BackdropFilter");
    public static final WidgetTypeId GROUPED = new WidgetTypeId("flutter.widgets.BackdropFilter.grouped");
    public static final WidgetTypeId GROUP = new WidgetTypeId("flutter.widgets.BackdropGroup");
    public static final List<String> CONFIGS = List.of("wrap", "blur", "compose");
    public static final String DESCRIPTION = "Filters previously painted content, then paints Child. "
            + "Clip the output explicitly (for example with ClipRect); bounded blur only limits sampling. "
            + "Filter config overrides, but preserves, Filter drafts. Only srcOver is guaranteed across backends. "
            + "Grouped filters share the nearest BackdropGroup; overlapping filters should not share a key. "
            + "Project code is never executed in Canvas; shader requires Impeller.";
    public static final String GROUP_DESCRIPTION = "Shares one backdrop input with descendant BackdropFilter.grouped widgets. "
            + "Wrap an existing Child. Omission/null creates a fresh BackdropKey; a typed source can share an application-owned key. "
            + "Do not group overlapping filters. Canvas uses a local group key, never project code.";
    public record Field(String name, String label, String group, String description) {}
    public static boolean isFilter(WidgetTypeId type) { return type.equals(TYPE) || type.equals(GROUPED); }
    public static List<Field> fields(WidgetTypeId type) {
        if (type.equals(GROUP)) return List.of(new Field("backdropKey", "Backdrop key", "Grouping", GROUP_DESCRIPTION));
        var result = new ArrayList<Field>();
        for (var f : ImageFilteredWidgetPropertySchema.FIELDS) {
            result.add(f.name().equals("imageFilter")
                    ? new Field("filter", "Filter", "Filter", "Six ImageFilter factories or a non-null ImageFilter source. Omission/null is valid only with an active Filter config. Config wrap uses this value; config blur/compose/source overrides it without deleting drafts.")
                    : new Field(f.name(), f.label(), f.group(), f.description()));
        }
        result.add(new Field("blendMode", "Blend mode", "Filter", "All 29 BlendMode values; omission srcOver. Only srcOver is supported on every backend. src can help under a saveLayer/Opacity ancestor."));
        result.add(new Field("filterConfig", "Filter config", "Config", "Optional wrap/blur/compose preset or non-null ImageFilterConfig source. A non-null config takes precedence over Filter and is emitted alone. Omission/null activates Filter."));
        result.add(new Field("configSigmaX", "Config sigma X", "Config blur", "Finite signed horizontal sigma; omission 0. Used only by config blur."));
        result.add(new Field("configSigmaY", "Config sigma Y", "Config blur", "Finite signed vertical sigma; omission 0. Used only by config blur."));
        result.add(new Field("configTileMode", "Config tile mode", "Config blur", "All four TileMode values, no null; omission clamp."));
        result.add(new Field("configBounded", "Bounded sampling", "Config blur", "Omission false. True resolves sampling bounds from the render object's current bounds during painting. This does not clip output."));
        result.add(new Field("configInner", "Inner config", "Config composition", "Non-null ImageFilterConfig source applied first. Omission uses zero-sigma config blur. Arbitrarily nested source compositions are supported."));
        result.add(new Field("configOuter", "Outer config", "Config composition", "Non-null ImageFilterConfig source applied after Inner. Omission uses zero-sigma config blur."));
        if (type.equals(TYPE)) result.add(new Field("backdropGroupKey", "Backdrop group key", "Grouping", "Nullable BackdropKey source; omission/null disables explicit sharing. Reuse the same source object for shared input. Canvas ignores project-owned explicit keys."));
        return List.copyOf(result);
    }
    public static List<PropertyDefinition> properties(WidgetTypeId type) {
        var result = new ArrayList<PropertyDefinition>();
        var originals = ImageFilteredWidgetPropertySchema.properties();
        for (var f : fields(type)) {
            var original = originals.stream().filter(p -> p.name().value().equals(f.name().equals("filter") ? "imageFilter" : f.name())).findFirst();
            var constraints = new ArrayList<PropertyValueConstraint>();
            Optional<PropertyValue> creation = Optional.empty();
            if (original.isPresent()) {
                constraints.addAll(original.orElseThrow().constraints());
                creation = original.orElseThrow().creationDefault();
                if (f.name().equals("filter")) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else switch (f.name()) {
                case "blendMode" -> constraints.addAll(ColorFilteredWidgetPropertySchema.properties().stream().filter(p -> p.name().value().equals("blendMode")).findFirst().orElseThrow().constraints());
                case "filterConfig" -> {
                    constraints.add(new PropertyValueConstraint.StringPattern("(?:wrap|blur|compose)", "one of the three ImageFilterConfig factories"));
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("ImageFilterConfig"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "configSigmaX", "configSigmaY" -> constraints.addAll(originals.stream().filter(p -> p.name().value().equals("sigmaX")).findFirst().orElseThrow().constraints());
                case "configTileMode" -> constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "TileMode"), List.of("clamp", "repeated", "mirror", "decal")));
                case "configBounded" -> constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
                case "configInner", "configOuter" -> constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("ImageFilterConfig"));
                case "backdropKey", "backdropGroupKey" -> {
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("BackdropKey?"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                default -> throw new IllegalStateException(f.name());
            }
            result.add(new PropertyDefinition(new PropertyName(f.name()), DartParameter.named(result.size(), false), constraints, creation));
        }
        return List.copyOf(result);
    }
    public static boolean usesConfig(WidgetNode node) {
        var config = node.properties().get(new PropertyName("filterConfig"));
        return config != null && config.kind() != PropertyValueKind.NULL;
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        var config = node.properties().get(new PropertyName("filterConfig"));
        boolean needsFilter = !usesConfig(node) || new PropertyValue.StringValue("wrap").equals(config);
        var filter = node.properties().get(new PropertyName("filter"));
        if (needsFilter && (filter == null || filter.kind() == PropertyValueKind.NULL))
            return Optional.of("BackdropFilter requires a non-null Filter or Filter config. Config wrap requires Filter.");
        if (needsFilter && new PropertyValue.StringValue("shader").equals(filter)
                && !(node.properties().get(new PropertyName("shader")) instanceof PropertyValue.DartObjectReferenceValue))
            return Optional.of("BackdropFilter shader requires a non-null FragmentShader source. Configure Shader first; Impeller is required.");
        return Optional.empty();
    }
    private BackdropFilterWidgetPropertySchema() {}
}
