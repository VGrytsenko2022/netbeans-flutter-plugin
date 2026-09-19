package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.*;
/** Complete Flutter 3.44.8 AlignTransition constructor apart from shared Key. */
public final class AlignTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE=new WidgetTypeId("flutter.widgets.AlignTransition");
    public static final String DESCRIPTION="Animates the required Child's layout alignment using Animation<AlignmentGeometry>. "
        +"Local physical or directional alignment creates a stopped animation; project sources own timing and lifecycle. "
        +"Width/height factors change immediately, not through this animation. Canvas previews project alignment at center and factors at null.";
    public record Field(String name,String label,String description){}
    public static final List<Field> FIELDS=List.of(
        new Field("alignment","Alignment animation","Required physical Alignment, RTL-aware AlignmentDirectional or non-null Animation<AlignmentGeometry> reference/getter/factory. "
            +"Local signed finite coordinates, including values outside [-1,1], create a stopped animation. Project Animation<Alignment> and Animation<AlignmentDirectional> are also accepted after strict analyzer verification. Canvas previews references at center."),
        new Field("widthFactor","Width factor","Optional nonnegative finite multiplier, explicit null or typed double? reference/getter/factory. "
            +"Null/omission fills bounded width and shrink-wraps unbounded width. Zero is valid; values above 1 are allowed. Changes apply immediately. Canvas previews project sources as null; source code owns runtime numeric validity."),
        new Field("heightFactor","Height factor","Optional nonnegative finite multiplier, explicit null or typed double? reference/getter/factory. "
            +"Null/omission fills bounded height and shrink-wraps unbounded height. Zero is valid; values above 1 are allowed. Changes apply immediately. Parent constraints still apply; this widget does not clip its Child."));
    public static List<PropertyDefinition> properties(){
        var factor=SizeTransitionWidgetPropertySchema.properties().getLast();
        return List.of(
            new PropertyDefinition(new PropertyName("alignment"),DartParameter.named(0,true),
                List.of(new PropertyValueConstraint.AlignmentGeometryValues(),new PropertyValueConstraint.DartObjectReferenceValues("Animation<AlignmentGeometry>")),
                Optional.of(new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,BigDecimal.ZERO,BigDecimal.ZERO))),
            new PropertyDefinition(new PropertyName("widthFactor"),DartParameter.named(2,false),factor.constraints(),Optional.empty()),
            new PropertyDefinition(new PropertyName("heightFactor"),DartParameter.named(3,false),factor.constraints(),Optional.empty()));
    }
    private AlignTransitionWidgetPropertySchema(){}
}
