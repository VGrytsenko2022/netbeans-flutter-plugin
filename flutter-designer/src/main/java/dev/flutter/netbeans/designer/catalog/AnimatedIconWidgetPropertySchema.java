package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Flutter 3.44.8 AnimatedIcon constructor apart from shared Key. */
public final class AnimatedIconWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.AnimatedIcon");
    public static final List<String> ICONS = List.of("add_event","arrow_menu","close_menu","ellipsis_search","event_add","home_menu","list_view","menu_arrow","menu_close","menu_home","pause_play","play_pause","search_ellipsis","view_list");
    public static final String DESCRIPTION = "Draws one of the 14 Material animated vector icons at Animation<double> progress. "
            + "Progress is clamped by Flutter to 0..1 without rewriting stored values. Icon and Progress are required. "
            + "Color and Size inherit IconTheme; it is not a button and has no native events. Canvas never executes project code.";
    public record Field(String name,String label,String group,String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("icon","Animated icon","Icon","Required reviewed AnimatedIcons constant or non-null AnimatedIconData reference/getter/factory. Previews show 0%, 50% and 100%. Flutter's opaque data must originate from AnimatedIcons; arbitrary AnimatedIconData subclasses fail the SDK's private-data cast. Source preview uses menu_close."),
        new Field("progress","Progress animation","Icon","Required finite signed value or non-null Animation<double> reference/getter/factory. Local uses AlwaysStoppedAnimation<double>; creation/source preview uses 0. Flutter clamps paint progress to 0..1 but the stored value is unchanged. Live controllers remain application-owned."),
        new Field("color","Color","Appearance","Optional literal/theme Color, null or typed Color? source. Unset/null inherits IconTheme color; IconTheme opacity applies even to explicit colors. Source preview inherits IconTheme."),
        new Field("size","Size","Appearance","Optional finite signed logical pixels, null or typed double? source. Unset/null inherits IconTheme size. Zero/negative values follow native CustomPaint layout and paint scaling; Canvas keeps zero-size selection targets. Source preview inherits IconTheme size."),
        new Field("semanticLabel","Semantic label","Accessibility","Optional string, including empty, null or unset. Announced by assistive technology, not painted. This widget is not automatically a button."),
        new Field("textDirection","Text direction","Accessibility","Unset/null inherits ambient Directionality. Explicit LTR/RTL affects only icons with matchTextDirection. The pinned SDK's native transform is retained.")
    );
    public static List<PropertyDefinition> properties(){
        var rotation=RotationTransitionWidgetPropertySchema.properties().getFirst();
        var color=ModalBarrierWidgetPropertySchema.properties().getFirst();
        var size=new ArrayList<PropertyValueConstraint>(List.of(
                new PropertyValueConstraint.IntegerRange(new BigInteger("-9007199254740991"),new BigInteger("9007199254740991")),
                new PropertyValueConstraint.DoubleRange(null,true,null,true),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                new PropertyValueConstraint.DartObjectReferenceValues("double?")));
        return List.of(
            new PropertyDefinition(new PropertyName("icon"),DartParameter.named(0,true),
                List.of(new PropertyValueConstraint.StringPattern("(?:" + String.join("|", ICONS) + ")", "reviewed AnimatedIcons constant"),new PropertyValueConstraint.DartObjectReferenceValues("AnimatedIconData")),
                Optional.of(new PropertyValue.StringValue("menu_close"))),
            new PropertyDefinition(new PropertyName("progress"),DartParameter.named(1,true),rotation.constraints(),Optional.of(new PropertyValue.DoubleValue(BigDecimal.ZERO))),
            new PropertyDefinition(new PropertyName("color"),DartParameter.named(2,false),color.constraints(),Optional.empty()),
            new PropertyDefinition(new PropertyName("size"),DartParameter.named(3,false),size,Optional.empty()),
            new PropertyDefinition(new PropertyName("semanticLabel"),DartParameter.named(4,false),List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)),Optional.empty()),
            new PropertyDefinition(new PropertyName("textDirection"),DartParameter.named(5,false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart","TextDirection"),List.of("ltr","rtl")),new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)),Optional.empty())
        );
    }
    private AnimatedIconWidgetPropertySchema(){}
}
