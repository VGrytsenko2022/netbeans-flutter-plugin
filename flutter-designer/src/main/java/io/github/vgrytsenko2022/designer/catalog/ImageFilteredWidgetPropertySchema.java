package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Flutter 3.44.8 ImageFiltered constructor and six ImageFilter factories. */
public final class ImageFilteredWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ImageFiltered");
    public static final List<String> FILTERS = List.of("blur", "dilate", "erode", "matrix", "compose", "shader");
    public static final String DESCRIPTION = "Filters Child pixels without changing layout, input or semantics. "
            + "Six native ImageFilter factories, optional Child and Enabled. Inactive drafts survive mode changes. "
            + "Project sources are never executed in Canvas. Shader filters require Impeller and a caller-owned FragmentShader.";
    public record Field(String name, String label, String group, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("imageFilter", "Image filter", "Filter", "Required blur/dilate/erode/matrix/compose/shader preset or non-null ImageFilter source (including ColorFilter). Creation uses zero-sigma blur. Configure Shader before selecting shader."),
            new Field("enabled", "Enabled", "Filter", "Apply the filter. Omission is true. False bypasses the layer, but the Dart filter expression is still evaluated."),
            new Field("sigmaX", "Sigma X", "Blur", "Horizontal Gaussian sigma, finite signed value (normally nonnegative); omission 0. Used only by blur."),
            new Field("sigmaY", "Sigma Y", "Blur", "Vertical Gaussian sigma, finite signed value (normally nonnegative); omission 0. Used only by blur."),
            new Field("tileMode", "Tile mode", "Blur", "All four TileMode values or null. Omission/null preserves native context-dependent edge sampling."),
            new Field("bounds", "Bounds source", "Blur bounds", "Nullable Rect getter/factory or explicit null, overriding local bounds. Omission uses local bounds if any coordinate is set, otherwise unbounded blur. Source bounds preview as null."),
            new Field("boundsLeft", "Left", "Blur bounds", "Finite local rectangle origin. Omission 0. Used only by blur when Bounds source is absent."),
            new Field("boundsTop", "Top", "Blur bounds", "Finite local rectangle origin. Omission 0. Used only by blur when Bounds source is absent."),
            new Field("boundsWidth", "Width", "Blur bounds", "Finite signed local bounds width; omission 0. Bounds use current canvas coordinates, not widget constraints."),
            new Field("boundsHeight", "Height", "Blur bounds", "Finite signed local bounds height; omission 0. Explicit source/null overrides all local bounds."),
            new Field("radiusX", "Radius X", "Morphology", "Horizontal finite signed radius for dilate/erode. Omission 0."),
            new Field("radiusY", "Radius Y", "Morphology", "Vertical finite signed radius for dilate/erode. Omission 0."),
            new Field("matrix4", "Matrix 4 x 4", "Matrix", "Full 16-entry column-major local matrix or non-null Float64List source with exactly 16 finite entries. Omission/source preview is identity. Project code owns list validity."),
            new Field("filterQuality", "Filter quality", "Matrix", "All four FilterQuality values. Omission medium. Only matrix uses this property."),
            new Field("inner", "Inner filter", "Composition", "Non-null ImageFilter source, including ColorFilter, applied first. Omission uses zero-sigma blur. Supports nested source-owned compositions; Canvas substitutes identity."),
            new Field("outer", "Outer filter", "Composition", "Non-null ImageFilter source applied after Inner: outer(inner(input)). Omission uses zero-sigma blur. Canvas substitutes identity."),
            new Field("shader", "Shader", "Shader", "Non-null FragmentShader source, required when shader is selected. Impeller only; first uniform must be vec2 and at least one sampler2D is required. Caller owns compilation, uniforms and disposal. Canvas substitutes identity."));
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        for (int i = 0; i < FIELDS.size(); i++) {
            String name = FIELDS.get(i).name();
            List<PropertyValueConstraint> constraints = switch (name) {
                case "imageFilter" -> List.of(
                        new PropertyValueConstraint.StringPattern("(?:blur|dilate|erode|matrix|compose|shader)", "one of the six ImageFilter factories"),
                        new PropertyValueConstraint.DartObjectReferenceValues("ImageFilter"));
                case "enabled" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
                case "bounds" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("Rect?"), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "inner", "outer" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ImageFilter"));
                case "shader" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FragmentShader"));
                case "matrix4" -> List.of(new PropertyValueConstraint.Matrix4Values(), new PropertyValueConstraint.DartObjectReferenceValues("Float64List"));
                case "tileMode" -> List.of(enums("TileMode", "clamp", "repeated", "mirror", "decal"), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "filterQuality" -> List.of(enums("FilterQuality", "none", "low", "medium", "high"));
                default -> List.of(new PropertyValueConstraint.IntegerRange(
                        new BigInteger("-9007199254740991"),
                        new BigInteger("9007199254740991")),
                        new PropertyValueConstraint.DoubleRange(null, true, null, true));
            };
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(i, i == 0), constraints,
                    i == 0 ? Optional.of(new PropertyValue.StringValue("blur")) : Optional.empty()));
        }
        return List.copyOf(result);
    }
    private static PropertyValueConstraint.EnumValues enums(String name, String... values) {
        return new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", name), List.of(values));
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        if (new PropertyValue.StringValue("shader").equals(node.properties().get(new PropertyName("imageFilter")))
                && !(node.properties().get(new PropertyName("shader")) instanceof PropertyValue.DartObjectReferenceValue)) {
            return Optional.of("ImageFiltered shader requires a non-null FragmentShader source. Configure Shader first; this factory requires Impeller in the running application.");
        }
        return Optional.empty();
    }
    public static PropertyValue.Matrix4Value identity() {
        return new PropertyValue.Matrix4Value(java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> i % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO).toList());
    }
    private ImageFilteredWidgetPropertySchema() {}
}
