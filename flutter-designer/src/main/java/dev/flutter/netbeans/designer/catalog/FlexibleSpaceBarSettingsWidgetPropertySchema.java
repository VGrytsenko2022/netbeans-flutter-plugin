package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Native FlexibleSpaceBarSettings constructor in Flutter 3.44.8. */
public final class FlexibleSpaceBarSettingsWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.FlexibleSpaceBarSettings");
    public static final String DESCRIPTION = "Inherited app-bar geometry and state; does not size its Child. "
            + "Wrap an existing box child containing FlexibleSpaceBar. All six native values are editable. "
            + "Required extents satisfy 0 <= Min extent <= Current extent <= Max extent. "
            + "Designer creation values (not SDK defaults) are opacity 1 and extents 56 / 200 / 200. "
            + "The required Child cannot be cleared. Raw sliver children are outside the current Designer box-wrapper contract.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("toolbarOpacity", "Toolbar opacity", "Required finite nonnegative number. The Settings constructor accepts values above 1, but FlexibleSpaceBar title color requires 0..1; Canvas labels that unavailable title preview."),
            new Field("minExtent", "Min extent", "Required nonnegative minimum, at most Current extent and Max extent. Lower this first when reducing the whole range."),
            new Field("maxExtent", "Max extent", "Required nonnegative maximum, at least Current extent and Min extent. Raise this first when increasing the whole range."),
            new Field("currentExtent", "Current extent", "Required nonnegative current value within Min extent..Max extent. Settings do not constrain Child size."),
            new Field("isScrolledUnder", "Is scrolled under", "Optional nullable state inherited by AppBar for WidgetState.scrolledUnder resolution. Unset/null means not determined."),
            new Field("hasLeading", "Has leading", "Optional nullable leading state inherited by FlexibleSpaceBar for default title padding. Unset/null retains native behavior."));
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        for (String name : List.of("toolbarOpacity", "minExtent", "maxExtent", "currentExtent")) {
            double seed = name.equals("toolbarOpacity") ? 1 : name.equals("minExtent") ? 56 : 200;
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(), true),
                    List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                            new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, null, true)),
                    Optional.of(new PropertyValue.DoubleValue(BigDecimal.valueOf(seed)))));
        }
        for (String name : List.of("isScrolledUnder", "hasLeading"))
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(), false),
                    List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()));
        return List.copyOf(result);
    }
    private FlexibleSpaceBarSettingsWidgetPropertySchema() {}
}

