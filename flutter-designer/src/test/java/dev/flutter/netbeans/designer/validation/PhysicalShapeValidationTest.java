package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.*;

class PhysicalShapeValidationTest {
    @Test
    void allShapesClipsColorsAndChildModesAreAccepted() {
        for (var shape : PropertyValue.ShapeBorderClipperValue.Shape.values()) {
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                for (boolean themed : List.of(false, true)) {
                    for (boolean child : List.of(false, true)) {
                        var result = validate(fullProperties(shape, clip, themed), child);
                        assertTrue(result.valid(), result.issues().toString());
                    }
                }
            }
        }
    }

    @Test
    void requiredClipperAndColorAreNeverImplicitlyDefaultedWhenReadingDocuments() {
        for (String required : List.of("clipper", "color")) {
            var values = new LinkedHashMap<>(defaults());
            values.remove(name(required));
            var result = validate(values, false);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(i -> i.path().endsWith("/" + required)));
        }
    }

    @Test
    void invalidEnumsElevationColorsAndArbitraryClipperCodeFailClosed() {
        rejects("clipper", new PropertyValue.DartExpressionValue("ShapeBorderClipper(shape: CircleBorder())"));
        rejects("clipper", new PropertyValue.NullValue());
        rejects("elevation", new PropertyValue.DoubleValue(new BigDecimal("-0.001")));
        rejects("elevation", new PropertyValue.DoubleValue(new BigDecimal("1e10000")));
        rejects("clipBehavior", new PropertyValue.EnumValue("Clip", "custom"));
        for (String color : List.of("color", "shadowColor")) {
            rejects(color, new PropertyValue.StringValue("Colors.blue"));
            rejects(color, new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge")));
        }
    }

    private static void rejects(String key, PropertyValue value) {
        var values = new LinkedHashMap<>(defaults()); values.put(name(key), value);
        var result = validate(values, false);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(i -> i.path().equals("/root/properties/" + key)), result.issues().toString());
    }
    private static ValidationResult validate(Map<PropertyName, PropertyValue> values, boolean child) {
        return new WidgetTreeValidator().validate(document(physicalShape(values, child)), BuiltInWidgetCatalog.getDefault());
    }
}
