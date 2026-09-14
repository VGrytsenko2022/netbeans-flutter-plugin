package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Complete FlexibleSpaceBar.new constructor from the pinned Flutter 3.44.8 SDK. */
public final class FlexibleSpaceBarWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.FlexibleSpaceBar");
    public static final List<String> STRETCH_PRESETS = List.of("none", "zoomBackground", "blurBackground",
            "fadeTitle", "zoomBackground,blurBackground", "zoomBackground,fadeTitle",
            "blurBackground,fadeTitle", "zoomBackground,blurBackground,fadeTitle");
    public static final String STRETCH_PATTERN = "(?:" + String.join("|", STRETCH_PRESETS) + ")";
    public static final String DESCRIPTION = "Native expanding, collapsing and stretching app-bar content. "
            + "All five scalar arguments and optional Title/Background slots are supported. "
            + "Place inside SliverAppBar.flexibleSpace, or AppBar.flexibleSpace under Scaffold; "
            + "Flutter requires inherited FlexibleSpaceBarSettings. Outside that context Canvas shows a labeled limitation. "
            + "No project references are executed by Canvas; stored values and generated Dart remain unchanged.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("centerTitle", "Center title", "Omitted or null uses the target platform default. Explicit true/false uses a checkbox."),
            new Field("titlePadding", "Title padding", "Optional nonnegative physical/directional padding, null, or a typed EdgeInsetsGeometry? reference/factory. Unset retains native leading-aware and direction-aware padding."),
            new Field("collapseMode", "Collapse mode", "All native modes: parallax (default), pin and none."),
            new Field("stretchModes", "Stretch modes", "All eight combinations of zoomBackground, blurBackground and fadeTitle; none emits an empty List<StretchMode>. Or use a strictly verified List<StretchMode> reference/factory. Effects require SliverAppBar.stretch and overscroll physics."),
            new Field("expandedTitleScale", "Expanded title scale", "Finite scale of at least 1, including exactly 1. Unset uses 1.5; the SDK assertion is inclusive."));
    public static List<PropertyDefinition> properties() {
        return List.of(
                property("centerTitle", 0, List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                property("titlePadding", 1, List.of(new PropertyValueConstraint.EdgeInsetsValues(true),
                        new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry?"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                property("collapseMode", 2, List.of(new PropertyValueConstraint.EnumValues(
                        new DartSymbolReference("package:flutter/material.dart", "CollapseMode"), List.of("parallax", "pin", "none")))),
                property("stretchModes", 3, List.of(new PropertyValueConstraint.StringPattern(STRETCH_PATTERN, "reviewed StretchMode combination"),
                        new PropertyValueConstraint.DartObjectReferenceValues("List<StretchMode>"))),
                property("expandedTitleScale", 4, List.of(
                        new PropertyValueConstraint.IntegerRange(BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                        new PropertyValueConstraint.DoubleRange(BigDecimal.ONE, true, null, true))));
    }
    private static PropertyDefinition property(String name, int order, List<PropertyValueConstraint> constraints) {
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, false), constraints, Optional.empty());
    }
    private FlexibleSpaceBarWidgetPropertySchema() {}
}

