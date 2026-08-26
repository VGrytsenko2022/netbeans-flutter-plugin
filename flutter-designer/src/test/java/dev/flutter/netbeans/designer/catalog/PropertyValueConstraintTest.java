package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyValueConstraintTest {
    @Test
    void validatesCreationDefaultAgainstTheCompleteRange() {
        assertThrows(IllegalArgumentException.class, () -> new PropertyDefinition(
                new PropertyName("maxLines"),
                DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE, null)),
                Optional.of(new PropertyValue.IntegerValue(BigInteger.ZERO))));
    }

    @Test
    void validatesCreationDefaultAgainstExactEnumTypeAndMember() {
        DartSymbolReference type = new DartSymbolReference(
                "package:flutter/widgets.dart", "MainAxisSize");
        PropertyValueConstraint.EnumValues values = new PropertyValueConstraint.EnumValues(
                type, List.of("min", "max"));
        assertEquals(type, values.dartType());
        assertTrue(values.accepts(new PropertyValue.EnumValue("MainAxisSize", "min")));
        assertFalse(values.accepts(new PropertyValue.EnumValue("OtherAxisSize", "min")));

        assertThrows(IllegalArgumentException.class, () -> new PropertyDefinition(
                new PropertyName("mainAxisSize"),
                DartParameter.named(0, false),
                List.of(values),
                Optional.of(new PropertyValue.EnumValue("OtherAxisSize", "min"))));
    }

    @Test
    void stringLengthCountsUnicodeCodePoints() {
        PropertyValueConstraint.StringLength oneCharacter = new PropertyValueConstraint.StringLength(1, 1);
        assertTrue(oneCharacter.accepts(new PropertyValue.StringValue("\uD83D\uDE80")));
        assertFalse(oneCharacter.accepts(new PropertyValue.StringValue("ab")));
    }

    @Test
    void doubleRangeHonorsExclusiveBoundary() {
        PropertyValueConstraint.DoubleRange positive = new PropertyValueConstraint.DoubleRange(
                BigDecimal.ZERO, false, null, true);
        assertFalse(positive.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertTrue(positive.accepts(new PropertyValue.DoubleValue(new BigDecimal("0.01"))));
        assertFalse(positive.accepts(new PropertyValue.IntegerValue(BigInteger.ONE)));
    }

    @Test
    void numericRangesRejectNonPortableOrValueChangingDartLiterals() {
        PropertyValueConstraint.IntegerRange integers =
                new PropertyValueConstraint.IntegerRange(null, null);
        BigInteger portableMaximum = DartNumericLiterals.MAX_PORTABLE_INTEGER;
        assertTrue(integers.accepts(new PropertyValue.IntegerValue(portableMaximum)));
        assertTrue(integers.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MIN_PORTABLE_INTEGER)));
        assertFalse(integers.accepts(new PropertyValue.IntegerValue(portableMaximum.add(BigInteger.ONE))));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.IntegerRange(
                BigInteger.ZERO, portableMaximum.add(BigInteger.ONE)));

        PropertyValueConstraint.DoubleRange doubles =
                new PropertyValueConstraint.DoubleRange(null, true, null, true);
        assertTrue(doubles.accepts(new PropertyValue.DoubleValue(new BigDecimal("0.1"))));
        assertTrue(doubles.accepts(new PropertyValue.DoubleValue(
                new BigDecimal("1.7976931348623157E+308"))));
        assertFalse(doubles.accepts(new PropertyValue.DoubleValue(new BigDecimal("1E+1000000"))));
        assertFalse(doubles.accepts(new PropertyValue.DoubleValue(new BigDecimal("1E-1000000"))));
        assertFalse(doubles.accepts(new PropertyValue.DoubleValue(
                new BigDecimal("1.234567890123456789"))));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.DoubleRange(
                BigDecimal.ZERO, true, new BigDecimal("1E+1000000"), true));
    }

    @Test
    void nonNegativeEdgeInsetsRejectAnyNegativeSide() {
        PropertyValueConstraint.EdgeInsetsValues nonNegative =
                new PropertyValueConstraint.EdgeInsetsValues(true);
        assertTrue(nonNegative.accepts(insets("0", "1", "2", "3")));
        assertFalse(nonNegative.accepts(insets("0", "-1", "2", "3")));
        assertFalse(nonNegative.accepts(insets("0", "1E+1000000", "2", "3")));
        assertFalse(nonNegative.accepts(insets("0", "1E-1000000", "2", "3")));
        assertFalse(nonNegative.accepts(insets("0", "1.234567890123456789", "2", "3")));
    }

    @Test
    void allowsOptionalPositionalParametersInExtensionMetadata() {
        DartParameter parameter = DartParameter.positional(0, false);
        assertFalse(parameter.required());
    }

    @Test
    void acceptedKindsHaveStableEnumOrder() {
        PropertyDefinition property = new PropertyDefinition(
                new PropertyName("value"),
                DartParameter.named(0, false),
                List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION),
                        new PropertyValueConstraint.CallbackReference()),
                Optional.empty());
        assertTrue(property.acceptedKinds().containsAll(
                List.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_EXPRESSION)));
        assertThrows(UnsupportedOperationException.class,
                () -> property.acceptedKinds().remove(PropertyValueKind.CALLBACK));
    }

    @Test
    void structuredAndNumericKindsCannotBypassTypedPoliciesThroughAnyValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.INTEGER));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.DOUBLE));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.ENUM));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.EDGE_INSETS));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.CALLBACK));
    }

    @Test
    void dartSymbolReferenceRequiresAValidLibraryAndPublicUnqualifiedName() {
        assertThrows(IllegalArgumentException.class,
                () -> new DartSymbolReference("package:flutter/widgets.dart", "type"));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSymbolReference("package:flutter/widgets.dart", "Example.Type"));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSymbolReference("package:flutter/widgets.dart", "_PrivateEnum"));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSymbolReference("package:flutter/widgets.dart';boom", "Example"));

        DartSymbolReference type = new DartSymbolReference(
                "package:flutter/widgets.dart", "Example");
        assertEquals(type, new DartSymbolReference("package:flutter/widgets.dart", "Example"));
        assertEquals(type.hashCode(),
                new DartSymbolReference("package:flutter/widgets.dart", "Example").hashCode());
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.EnumValues(type, List.of("class")));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.EnumValues(type, List.of("_privateValue")));
    }

    @Test
    void callbackReferenceRejectsReservedHandlersAndCreationDefaults() {
        PropertyValueConstraint.CallbackReference callback =
                new PropertyValueConstraint.CallbackReference();
        assertTrue(callback.accepts(new PropertyValue.CallbackValue("_onPressed")));
        assertTrue(callback.accepts(new PropertyValue.CallbackValue("_class")));
        assertFalse(callback.accepts(new PropertyValue.CallbackValue("class")));
        assertFalse(callback.accepts(new PropertyValue.CallbackValue("type")));

        assertThrows(IllegalArgumentException.class, () -> new PropertyDefinition(
                new PropertyName("onPressed"),
                DartParameter.named(0, true),
                List.of(callback),
                Optional.of(new PropertyValue.CallbackValue("class"))));
    }

    private static PropertyValue.EdgeInsetsValue insets(
            String left, String top, String right, String bottom) {
        return new PropertyValue.EdgeInsetsValue(
                new BigDecimal(left),
                new BigDecimal(top),
                new BigDecimal(right),
                new BigDecimal(bottom));
    }
}
