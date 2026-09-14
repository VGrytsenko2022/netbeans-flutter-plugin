package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Flutter 3.44.8 SliverOpacity constructor, apart from shared identity/key. */
public final class SliverOpacityWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverOpacity");
    public static final String DESCRIPTION = "Changes sliver transparency without changing its layout or scroll extent. "
            + "Zero hides painting but does not disable pointer hit testing. The optional sliver accepts sliver widgets only. "
            + "An empty slot generates a zero-extent SliverToBoxAdapter because Flutter 3.44.8 cannot lay out a null proxy sliver child.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("opacity", "Opacity", "Required finite number from 0 (invisible) to 1 (opaque). "
                    + "The Designer starts at 1; Flutter requires an explicit value. Fractional opacity uses an intermediate buffer. "
                    + "Zero retains layout, scrolling and pointer hit testing; it is not an interaction-disable switch."),
            new Field("alwaysIncludeSemantics", "Always include semantics", "Expose child semantics even at zero opacity. "
                    + "Omission uses Flutter's false default; explicit true/false is edited with a checkbox."));
    public static PropertyDefinition opacity() {
        return new PropertyDefinition(new PropertyName("opacity"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, BigInteger.ONE),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, BigDecimal.ONE, true)),
                Optional.of(new PropertyValue.DoubleValue(BigDecimal.ONE)));
    }
    public static List<PropertyDefinition> properties() {
        return List.of(opacity(), new PropertyDefinition(new PropertyName("alwaysIncludeSemantics"),
                DartParameter.named(1, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()));
    }
    private SliverOpacityWidgetPropertySchema() {}
}
