package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/** Complete Flutter 3.44.8 constructor: key, nonnegative maxExtent and required sliver. */
public final class SliverConstrainedCrossAxisWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverConstrainedCrossAxis");
    public static final PropertyValue.EnumValue INFINITY = new PropertyValue.EnumValue("double", "infinity");
    public static final String DESCRIPTION = "Limits the sliver's cross-axis extent to the smaller of Max extent and the available space. "
            + "Zero and positive Infinity are supported. The Designer starts at 120 logical pixels (not an SDK default). "
            + "Wrap an existing sliver; the required child cannot be cleared or moved out. "
            + "In SliverCrossAxisGroup this lane has zero flex; other lanes need remaining space.";
    public static PropertyDefinition maxExtent() {
        return new PropertyDefinition(new PropertyName("maxExtent"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, null, true),
                        new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity"))),
                Optional.of(new PropertyValue.DoubleValue(BigDecimal.valueOf(120))));
    }
    public static List<PropertyDefinition> properties() { return List.of(maxExtent()); }
    private SliverConstrainedCrossAxisWidgetPropertySchema() {}
}

