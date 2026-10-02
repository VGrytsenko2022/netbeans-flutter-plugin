package io.github.vgrytsenko2022.designer.validation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.PhysicalModelTestSupport.*;

class PhysicalModelValidationTest {
    @Test
    void acceptsAllShapesClipModesLiteralAndThemeColorsAndOptionalChild() {
        assertTrue(validate(physicalModel(defaults(), false)).valid());
        for (String shape : List.of("rectangle", "circle")) {
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                for (boolean theme : List.of(false, true)) {
                    for (boolean child : List.of(false, true)) {
                        var result = validate(physicalModel(fullProperties(shape, clip, theme), child));
                        assertTrue(result.valid(), result.issues().toString());
                    }
                }
            }
        }
    }

    @Test
    void requiresColorAndRejectsArbitraryOrWrongThemeColorValues() {
        var missing = validate(physicalModel(Map.of(), false));
        assertFalse(missing.valid());
        assertTrue(missing.errors().stream().anyMatch(issue -> issue.path().endsWith("/color")));
        for (String property : List.of("color", "shadowColor")) {
            rejects(property, new PropertyValue.StringValue("Colors.blue"), WidgetTreeValidator.PROPERTY_KIND);
            rejects(property, new PropertyValue.DartExpressionValue("Theme.of(context).primaryColor"), WidgetTreeValidator.PROPERTY_KIND);
            rejects(property, new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge")),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
        }
    }

    @Test
    void rejectsDirectionalRadiusEvenWhenCircleWouldIgnoreItAndRejectsUnrepresentableComponents() {
        var values = new LinkedHashMap<>(fullProperties("circle", "none", false));
        values.put(name("borderRadius"), radius(true));
        var result = validate(physicalModel(values, false));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, result.errors().getFirst().code());
        assertEquals("/root/properties/borderRadius", result.errors().getFirst().path());
        rejects("borderRadius", new PropertyValue.StringValue("BorderRadius.circular(4)"), WidgetTreeValidator.PROPERTY_KIND);
        var huge = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("1e10000"), BigDecimal.ONE);
        rejects("borderRadius", new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(huge, huge, huge, huge)),
                WidgetTreeValidator.PROPERTY_CONSTRAINT);
    }

    @Test
    void acceptsFiniteNonnegativeElevationAndRejectsNegativeHugeWrongKindAndUnknownEnums() {
        for (PropertyValue value : List.of(new PropertyValue.IntegerValue(BigInteger.ZERO),
                new PropertyValue.IntegerValue(BigInteger.valueOf(100)),
                new PropertyValue.DoubleValue(new BigDecimal("0.125")))) {
            var values = new LinkedHashMap<>(defaults());
            values.put(name("elevation"), value);
            assertTrue(validate(physicalModel(values, false)).valid());
        }
        rejects("elevation", new PropertyValue.IntegerValue(BigInteger.valueOf(-1)), WidgetTreeValidator.PROPERTY_CONSTRAINT);
        rejects("elevation", new PropertyValue.DoubleValue(new BigDecimal("-0.01")), WidgetTreeValidator.PROPERTY_CONSTRAINT);
        rejects("elevation", new PropertyValue.DoubleValue(new BigDecimal("1e10000")), WidgetTreeValidator.PROPERTY_CONSTRAINT);
        rejects("elevation", new PropertyValue.StringValue("Infinity"), WidgetTreeValidator.PROPERTY_KIND);
        rejects("shape", new PropertyValue.EnumValue("BoxShape", "oval"), WidgetTreeValidator.PROPERTY_CONSTRAINT);
        rejects("shape", new PropertyValue.EnumValue("Clip", "none"), WidgetTreeValidator.PROPERTY_CONSTRAINT);
        rejects("clipBehavior", new PropertyValue.EnumValue("Clip", "custom"), WidgetTreeValidator.PROPERTY_CONSTRAINT);
    }

    @Test
    void rejectsNonSingleChildWithoutLosingItsPreciseDiagnosticPath() {
        var node = physicalModel(defaults(), false);
        var invalid = new WidgetNode(node.id(), node.type(), node.properties(),
                Map.of(new SlotName("child"), new WidgetSlot.ListSlot(List.of(text(), text()))));
        var result = validate(invalid);
        assertFalse(result.valid());
        assertEquals(List.of(WidgetTreeValidator.SLOT_KIND, WidgetTreeValidator.SLOT_CARDINALITY),
                result.errors().stream().map(ValidationIssue::code).toList());
        assertTrue(result.errors().stream().allMatch(issue -> issue.path().equals("/root/slots/child")));
    }

    private static void rejects(String property, PropertyValue value, String code) {
        var values = new LinkedHashMap<>(defaults());
        values.put(name(property), value);
        var result = validate(physicalModel(values, false));
        assertEquals(1, result.errors().size(), result.issues().toString());
        assertEquals(code, result.errors().getFirst().code());
        assertEquals("/root/properties/" + property, result.errors().getFirst().path());
    }

    private static ValidationResult validate(WidgetNode root) {
        return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault());
    }
}
