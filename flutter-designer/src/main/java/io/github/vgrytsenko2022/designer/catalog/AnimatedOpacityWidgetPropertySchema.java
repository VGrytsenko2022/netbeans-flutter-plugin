package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 AnimatedOpacity constructor. */
public final class AnimatedOpacityWidgetPropertySchema {
    public static final WidgetTypeId TYPE=new WidgetTypeId("flutter.widgets.AnimatedOpacity");
    public static final String DESCRIPTION="Animates child opacity without changing layout or disabling pointer hits. "
            +"Opacity and Duration are required; the Designer starts at 1 and 300000 microseconds. "
            +"On end fires after each completed transition, not on initial mount. Canvas never executes project code. "
            +"Child is optional.";
    public record Field(String name,String label,String description) {}
    public static final List<Field> FIELDS=List.of(
        new Field("opacity","Opacity","Required finite target from 0 to 1. Zero does not disable hit testing. Fractional opacity uses an intermediate buffer."),
        new Field("curve","Curve","All 43 pinned Curves presets or a strict Curve reference/factory. Omission uses linear. Canvas uses linear for project-owned curves without executing them."),
        new Field("durationUs","Duration (microseconds)","Required nonnegative portable integer microseconds or strict Duration reference/factory. Zero completes immediately. Canvas uses 300000 microseconds for project-owned duration values."),
        new Field("onEnd","On end","Optional VoidCallback reference/factory or null. Invoked after each completed transition, not initial mount; configure or navigate the handler in Events. Canvas does not execute the handler."),
        new Field("alwaysIncludeSemantics","Always include semantics","Expose child semantics at zero opacity. Omission uses false; explicit values use the shared checkbox."));
    public static String curvePattern(){return "(?:"+String.join("|",ExpansionTileWidgetPropertySchema.curvePresets())+")";}
    public static List<PropertyDefinition> properties(){
        return List.of(SliverOpacityWidgetPropertySchema.opacity(),
            new PropertyDefinition(new PropertyName("curve"),DartParameter.named(1,false),
                List.of(new PropertyValueConstraint.StringPattern(curvePattern(),"reviewed Curves preset"),
                    new PropertyValueConstraint.DartObjectReferenceValues("Curve")),Optional.empty()),
            new PropertyDefinition(new PropertyName("durationUs"),DartParameter.named(2,true),
                List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,new BigInteger("9007199254740991")),
                    new PropertyValueConstraint.DartObjectReferenceValues("Duration")),
                Optional.of(new PropertyValue.IntegerValue(BigInteger.valueOf(300000)))),
            new PropertyDefinition(new PropertyName("onEnd"),DartParameter.named(3,false),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback"),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)),Optional.empty()),
            new PropertyDefinition(new PropertyName("alwaysIncludeSemantics"),DartParameter.named(4,false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)),Optional.empty()));
    }
    private AnimatedOpacityWidgetPropertySchema(){}
}


