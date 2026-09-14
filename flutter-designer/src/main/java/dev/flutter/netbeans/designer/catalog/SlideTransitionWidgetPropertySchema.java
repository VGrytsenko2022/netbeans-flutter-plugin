package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complete pinned Flutter 3.44.8 SlideTransition constructor apart from shared key. */
public final class SlideTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SlideTransition");
    public static final String DESCRIPTION = "Translates the child by fractions of its size using Animation<Offset>, without changing layout. "
            + "Local offsets create a stopped animation; project animation/getter/factory references supply live updates and own their lifetime. "
            + "Unset/null Text direction uses physical X even in RTL; explicit RTL reverses X. Canvas never executes project code and previews references at zero.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("position", "Position animation", "Required finite signed X/Y fractions of child width/height, or non-null Animation<Offset> reference/getter/factory. "
                + "Values outside [-1,1] are supported. Local values create AlwaysStoppedAnimation<Offset>; edits are immediate. Project animation preview uses zero."),
        new Field("transformHitTests", "Transform hit tests", "Whether pointer hit testing follows the translation. Unset uses true. False keeps the original hit position; ancestor hit bounds still apply. Painting and semantics remain translated."),
        new Field("textDirection", "Text direction", "Unset or null uses physical X (positive moves right), regardless of ambient Directionality. Explicit RTL reverses X; LTR preserves X. Y is unchanged."));
    public static List<PropertyDefinition> properties() {
        return List.of(
            new PropertyDefinition(new PropertyName("position"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.OffsetValues(), new PropertyValueConstraint.DartObjectReferenceValues("Animation<Offset>")),
                Optional.of(new PropertyValue.OffsetValue(BigDecimal.ZERO, BigDecimal.ZERO))),
            new PropertyDefinition(new PropertyName("transformHitTests"), DartParameter.named(1, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()),
            new PropertyDefinition(new PropertyName("textDirection"), DartParameter.named(2, false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "TextDirection"), List.of("ltr", "rtl")),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()));
    }
    private SlideTransitionWidgetPropertySchema() {}
}
