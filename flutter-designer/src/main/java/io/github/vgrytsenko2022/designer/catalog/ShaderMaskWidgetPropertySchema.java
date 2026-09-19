package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complete Flutter 3.44.8 ShaderMask constructor, with a local gradient delegate. */
public final class ShaderMaskWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ShaderMask");
    public static final String DESCRIPTION = "Masks Child pixels with a ShaderCallback without changing layout, hit testing or semantics. "
            + "Local linear/radial/sweep gradients resolve the current paint bounds and inherited text direction. "
            + "A typed Shader Function(Rect bounds) delegate supports any project-owned Shader, including image and fragment shaders. "
            + "Canvas never executes project callbacks and substitutes an opaque white shader; Blend mode is still applied.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("shaderCallback", "Shader callback", "Required local gradient or non-null ShaderCallback. "
                + "All three gradient families support 2..256 ordered literal/theme color stops, physical/directional alignment, "
                + "four tile modes and rotation. Radial supports focal geometry; sweep supports start/end angles. "
                + "Create/select/rename/go-to/disconnect callback actions preserve user-owned Dart bodies. "
                + "Disconnect restores opaque white. A shader object itself is not a callback; return it from the typed delegate."),
        new Field("blendMode", "Blend mode", "All 29 BlendMode values; omission modulate. "
                + "The shader is the source and painted Child is the destination. "
                + "An opaque white shader is neutral only for suitable modes (including the default modulate)."));
    public static PropertyValue.GradientValue neutral() {
        return new PropertyValue.GradientValue(new PropertyValue.BoxDecorationValue.LinearGradient(
                new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL, BigDecimal.valueOf(-1), BigDecimal.ZERO),
                new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL, BigDecimal.ONE, BigDecimal.ZERO),
                List.of(
                    new PropertyValue.BoxDecorationValue.GradientStop(StableId.parse("30ff2123-0000-4000-8000-000000000001"), new ColorSource.Literal(0xffffffffL), BigDecimal.ZERO),
                    new PropertyValue.BoxDecorationValue.GradientStop(StableId.parse("30ff2123-0000-4000-8000-000000000002"), new ColorSource.Literal(0xffffffffL), BigDecimal.ONE)),
                PropertyValue.BoxDecorationValue.TileMode.CLAMP, Optional.empty()));
    }
    public static List<PropertyDefinition> properties() {
        return List.of(
            new PropertyDefinition(new PropertyName("shaderCallback"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.GradientValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),
                    new PropertyValueConstraint.DartObjectReferenceValues("ShaderCallback")), Optional.of(neutral())),
            new PropertyDefinition(new PropertyName("blendMode"), DartParameter.named(1, false),
                ColorFilteredWidgetPropertySchema.properties().stream().filter(p -> p.name().value().equals("blendMode")).findFirst().orElseThrow().constraints(), Optional.empty()));
    }
    private ShaderMaskWidgetPropertySchema() {}
}
