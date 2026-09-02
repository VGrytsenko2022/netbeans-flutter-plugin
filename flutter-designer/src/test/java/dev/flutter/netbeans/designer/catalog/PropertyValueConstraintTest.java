package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
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
    void stringPatternRequiresAFullMatchAndRejectsInvalidCatalogMetadata() {
        PropertyValueConstraint.StringPattern languageCode =
                new PropertyValueConstraint.StringPattern(
                        "(?:[a-z]{2,3}|[a-z]{5,8})",
                        "Flutter locale language code");

        assertTrue(languageCode.accepts(new PropertyValue.StringValue("uk")));
        assertTrue(languageCode.accepts(new PropertyValue.StringValue("fil")));
        assertTrue(languageCode.accepts(new PropertyValue.StringValue("language")));
        assertFalse(languageCode.accepts(new PropertyValue.StringValue("EN")));
        assertFalse(languageCode.accepts(new PropertyValue.StringValue("e")));
        assertFalse(languageCode.accepts(new PropertyValue.StringValue("uk-UA")));
        assertFalse(languageCode.accepts(new PropertyValue.BooleanValue(true)));
        assertEquals("Flutter locale language code", languageCode.description());

        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.StringPattern("[", "invalid regex"));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.StringPattern("[a-z]+", " "));
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
        assertTrue(nonNegative.accepts(new PropertyValue.EdgeInsetsDirectionalValue(
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TEN)));
        assertFalse(nonNegative.accepts(new PropertyValue.EdgeInsetsDirectionalValue(
                BigDecimal.ZERO, BigDecimal.ONE.negate(), BigDecimal.TWO, BigDecimal.TEN)));
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
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.THEME_TOKEN));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.PAINT));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.SHADOW_LIST));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.FONT_VARIATION_LIST));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.ICON_DATA));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.ALIGNMENT_GEOMETRY));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.BOX_CONSTRAINTS));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.MATRIX4));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.IMAGE_PROVIDER));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.BOX_DECORATION));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.CALLBACK));
    }

    @Test
    void structuredConstraintsEnforceDartNumbersAndReviewedNestedThemeColors() {
        BigDecimal huge = BigDecimal.ONE.scaleByPowerOfTen(400);
        PropertyValueConstraint.AlignmentGeometryValues alignments =
                new PropertyValueConstraint.AlignmentGeometryValues();
        assertTrue(alignments.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.ZERO, BigDecimal.ONE)));
        assertFalse(alignments.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                huge, BigDecimal.ONE)));

        PropertyValueConstraint.BoxConstraintsValues constraints =
                new PropertyValueConstraint.BoxConstraintsValues();
        assertTrue(constraints.accepts(new PropertyValue.BoxConstraintsValue(
                BigDecimal.ZERO, Optional.empty(), BigDecimal.ZERO, Optional.empty())));
        assertTrue(constraints.accepts(new PropertyValue.BoxConstraintsValue(
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                PropertyValue.BoxConstraintBound.finite(BigDecimal.ZERO),
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE)));
        assertFalse(constraints.accepts(new PropertyValue.BoxConstraintsValue(
                BigDecimal.ZERO, Optional.of(huge), BigDecimal.ZERO, Optional.empty())));
        assertFalse(constraints.accepts(new PropertyValue.BoxConstraintsValue(
                PropertyValue.BoxConstraintBound.finite(huge),
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                PropertyValue.BoxConstraintBound.finite(BigDecimal.ZERO),
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE)));
        assertEquals("normalized BoxConstraints with finite or positive-infinite bounds",
                constraints.description());

        PropertyValueConstraint.Matrix4Values matrices =
                new PropertyValueConstraint.Matrix4Values();
        assertTrue(matrices.accepts(new PropertyValue.Matrix4Value(
                java.util.Collections.nCopies(16, BigDecimal.ZERO))));
        assertFalse(matrices.accepts(new PropertyValue.Matrix4Value(
                java.util.Collections.nCopies(16, huge))));

        PropertyValueConstraint.BoxDecorationValues decorations =
                new PropertyValueConstraint.BoxDecorationValues(
                        List.of("material.colorScheme.primary"));
        assertTrue(decorations.accepts(new PropertyValue.BoxDecorationValue(
                Optional.of(new ColorSource.Theme(
                        new ThemeToken("material.colorScheme.primary"))),
                Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                Optional.empty(), PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE)));
        assertFalse(decorations.accepts(new PropertyValue.BoxDecorationValue(
                Optional.of(new ColorSource.Theme(
                        new ThemeToken("material.colorScheme.secondary"))),
                Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                Optional.empty(), PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE)));

        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.asset("assets/logo.png");
        PropertyValue.DecorationImageValue allowedImage =
                new PropertyValue.DecorationImageValue(
                        provider, Optional.empty(),
                        Optional.of(new PropertyValue.DecorationImageValue.Mode(
                                new ColorSource.Theme(new ThemeToken(
                                        "material.colorScheme.primary")),
                                PropertyValue.PaintValue.BlendMode.SRC_IN)),
                        Optional.empty(),
                        new PropertyValue.AlignmentGeometryValue(
                                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                                BigDecimal.ZERO, BigDecimal.ZERO),
                        Optional.empty(),
                        PropertyValue.DecorationImageValue.ImageRepeat.NO_REPEAT,
                        false, BigDecimal.ONE, BigDecimal.ONE,
                        PropertyValue.PaintValue.FilterQuality.MEDIUM,
                        false, false);
        assertTrue(decorations.accepts(new PropertyValue.BoxDecorationValue(
                Optional.empty(), Optional.of(allowedImage), Optional.empty(),
                Optional.empty(), List.of(), Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE)));

        PropertyValue.DecorationImageValue disallowedImage =
                new PropertyValue.DecorationImageValue(
                        provider, Optional.empty(),
                        Optional.of(new PropertyValue.DecorationImageValue.Mode(
                                new ColorSource.Theme(new ThemeToken(
                                        "material.colorScheme.secondary")),
                                PropertyValue.PaintValue.BlendMode.SRC_IN)),
                        Optional.empty(), allowedImage.alignment(), Optional.empty(),
                        PropertyValue.DecorationImageValue.ImageRepeat.NO_REPEAT,
                        false, BigDecimal.ONE, BigDecimal.ONE,
                        PropertyValue.PaintValue.FilterQuality.MEDIUM,
                        false, false);
        assertFalse(decorations.accepts(new PropertyValue.BoxDecorationValue(
                Optional.empty(), Optional.of(disallowedImage), Optional.empty(),
                Optional.empty(), List.of(), Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE)));

        PropertyValue.ImageProviderValue hugeScale =
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/logo.png", huge);
        assertFalse(new PropertyValueConstraint.ImageProviderValues()
                .accepts(hugeScale));
        PropertyValue.DecorationImageValue hugeSaturation =
                new PropertyValue.DecorationImageValue(
                        provider, Optional.empty(),
                        Optional.of(new PropertyValue.DecorationImageValue.Saturation(huge)),
                        Optional.empty(), allowedImage.alignment(), Optional.empty(),
                        PropertyValue.DecorationImageValue.ImageRepeat.NO_REPEAT,
                        false, BigDecimal.ONE, BigDecimal.ONE,
                        PropertyValue.PaintValue.FilterQuality.MEDIUM,
                        false, false);
        assertFalse(decorations.accepts(new PropertyValue.BoxDecorationValue(
                Optional.empty(), Optional.of(hugeSaturation), Optional.empty(),
                Optional.empty(), List.of(), Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE)));
    }

    @Test
    void iconDataConstraintAcceptsOnlyTheTypedValidatedValue() {
        PropertyValueConstraint.IconDataValues icons =
                new PropertyValueConstraint.IconDataValues();
        assertTrue(icons.accepts(PropertyValue.IconDataValue.none()));
        assertTrue(icons.accepts(new PropertyValue.IconDataValue(
                Optional.of(0xE5F9), Optional.of("MaterialIcons"),
                Optional.empty(), false, List.of())));
        assertFalse(icons.accepts(new PropertyValue.DartExpressionValue("Icons.star")));
        assertEquals("typed nullable IconData metadata", icons.description());
    }

    @Test
    void materialIconConstraintAcceptsOnlyNoneOrExactBundledRegistryGlyphs() {
        PropertyValueConstraint.MaterialIconValues icons =
                new PropertyValueConstraint.MaterialIconValues();
        MaterialIconRegistry.MaterialIcon star = MaterialIconRegistry.bundled()
                .find("star").orElseThrow();
        MaterialIconRegistry.MaterialIcon arrowBack = MaterialIconRegistry.bundled()
                .find("arrow_back").orElseThrow();

        assertTrue(icons.accepts(PropertyValue.IconDataValue.none()));
        assertTrue(icons.accepts(material(star)));
        assertTrue(icons.accepts(material(arrowBack)));
        assertFalse(icons.accepts(new PropertyValue.IconDataValue(
                Optional.of(star.codePoint()), Optional.of("CustomIcons"),
                Optional.empty(), false, List.of())));
        assertFalse(icons.accepts(new PropertyValue.IconDataValue(
                Optional.of(star.codePoint()), Optional.of("MaterialIcons"),
                Optional.of("package"), false, List.of())));
        assertFalse(icons.accepts(new PropertyValue.IconDataValue(
                Optional.of(star.codePoint()), Optional.of("MaterialIcons"),
                Optional.empty(), false, List.of("Fallback"))));
        assertFalse(icons.accepts(new PropertyValue.IconDataValue(
                Optional.of(arrowBack.codePoint()), Optional.of("MaterialIcons"),
                Optional.empty(), !arrowBack.matchTextDirection(), List.of())));
        assertFalse(icons.accepts(new PropertyValue.IconDataValue(
                Optional.of(1), Optional.of("MaterialIcons"),
                Optional.empty(), false, List.of())));
        assertTrue(icons.description().contains("Flutter 3.44.8"));
        assertTrue(icons.description().contains("uses-material-design"));
    }

    private static PropertyValue.IconDataValue material(
            MaterialIconRegistry.MaterialIcon icon) {
        return new PropertyValue.IconDataValue(
                Optional.of(icon.codePoint()),
                Optional.of(icon.fontFamily()),
                Optional.empty(),
                icon.matchTextDirection(),
                List.of());
    }

    @Test
    void structuredNumericConstraintsRejectNonRepresentableDartDoubles() {
        List<String> reviewed = List.of("material.colorScheme.primary");
        PropertyValueConstraint.PaintValues paints =
                new PropertyValueConstraint.PaintValues(reviewed);
        PropertyValueConstraint.ShadowListValues shadows =
                new PropertyValueConstraint.ShadowListValues(reviewed);
        PropertyValueConstraint.FontVariationListValues variations =
                new PropertyValueConstraint.FontVariationListValues();

        List<BigDecimal> rejected = List.of(
                new BigDecimal("1E+400"),
                new BigDecimal("1E-400"),
                new BigDecimal("1.234567890123456789"));
        for (BigDecimal value : rejected) {
            assertFalse(paints.accepts(paint(value, BigDecimal.valueOf(4), BigDecimal.ONE)),
                    () -> "strokeWidth accepted " + value);
            assertFalse(paints.accepts(paint(BigDecimal.ZERO, value, BigDecimal.ONE)),
                    () -> "strokeMiterLimit accepted " + value);
            if (value.signum() > 0) {
                assertFalse(paints.accepts(paint(
                        BigDecimal.ZERO, BigDecimal.valueOf(4), value)),
                        () -> "maskFilter sigma accepted " + value);
            }

            assertFalse(shadows.accepts(shadows(value, BigDecimal.ZERO, BigDecimal.ZERO)),
                    () -> "Shadow offsetX accepted " + value);
            assertFalse(shadows.accepts(shadows(BigDecimal.ZERO, value, BigDecimal.ZERO)),
                    () -> "Shadow offsetY accepted " + value);
            if (value.signum() >= 0) {
                assertFalse(shadows.accepts(shadows(
                        BigDecimal.ZERO, BigDecimal.ZERO, value)),
                        () -> "Shadow blurRadius accepted " + value);
            }

            if (value.compareTo(BigDecimal.valueOf(-32768)) >= 0
                    && value.compareTo(BigDecimal.valueOf(32768)) < 0) {
                assertFalse(variations.accepts(variations(value)),
                        () -> "FontVariation value accepted " + value);
            }
        }
    }

    @Test
    void structuredThemeConstraintsRejectUnreviewedDirectAndNestedRoles() {
        List<String> reviewed = List.of("material.colorScheme.primary");
        ThemeToken reviewedToken = new ThemeToken("material.colorScheme.primary");
        ThemeToken unknownToken = new ThemeToken("material.colorScheme.futureRole");

        PropertyValueConstraint.ThemeTokenValues tokens =
                new PropertyValueConstraint.ThemeTokenValues(reviewed);
        assertTrue(tokens.accepts(new PropertyValue.ThemeTokenValue(reviewedToken)));
        assertFalse(tokens.accepts(new PropertyValue.ThemeTokenValue(unknownToken)));

        PropertyValueConstraint.PaintValues paints =
                new PropertyValueConstraint.PaintValues(reviewed);
        assertTrue(paints.accepts(PropertyValue.PaintValue.defaults(
                new ColorSource.Literal(0xFF112233L))));
        assertTrue(paints.accepts(PropertyValue.PaintValue.defaults(
                new ColorSource.Theme(reviewedToken))));
        assertFalse(paints.accepts(PropertyValue.PaintValue.defaults(
                new ColorSource.Theme(unknownToken))));

        PropertyValueConstraint.ShadowListValues shadows =
                new PropertyValueConstraint.ShadowListValues(reviewed);
        assertTrue(shadows.accepts(shadows(new ColorSource.Theme(reviewedToken))));
        assertFalse(shadows.accepts(shadows(new ColorSource.Theme(unknownToken))));
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

    private static PropertyValue.ShadowListValue shadows(ColorSource color) {
        return new PropertyValue.ShadowListValue(List.of(
                new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("8b41dc76-ef62-4b91-84a5-79e00f2fd774"),
                        color,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO)));
    }

    private static PropertyValue.PaintValue paint(
            BigDecimal strokeWidth,
            BigDecimal strokeMiterLimit,
            BigDecimal sigma) {
        return new PropertyValue.PaintValue(
                new ColorSource.Literal(0xFF112233L),
                PropertyValue.PaintValue.BlendMode.SRC_OVER,
                PropertyValue.PaintValue.Style.FILL,
                strokeWidth,
                PropertyValue.PaintValue.StrokeCap.BUTT,
                PropertyValue.PaintValue.StrokeJoin.MITER,
                strokeMiterLimit,
                true,
                PropertyValue.PaintValue.FilterQuality.NONE,
                false,
                Optional.of(new PropertyValue.PaintValue.BlurMask(
                        PropertyValue.PaintValue.BlurStyle.NORMAL, sigma)));
    }

    private static PropertyValue.ShadowListValue shadows(
            BigDecimal offsetX,
            BigDecimal offsetY,
            BigDecimal blurRadius) {
        return new PropertyValue.ShadowListValue(List.of(
                new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("8b41dc76-ef62-4b91-84a5-79e00f2fd774"),
                        new ColorSource.Literal(0xFF112233L),
                        offsetX,
                        offsetY,
                        blurRadius)));
    }

    private static PropertyValue.FontVariationListValue variations(BigDecimal value) {
        return new PropertyValue.FontVariationListValue(List.of(
                new PropertyValue.FontVariationListValue.FontVariation(
                        StableId.parse("21b26e93-af43-411e-902d-76a76097ba87"),
                        "GRAD",
                        value)));
    }
}
