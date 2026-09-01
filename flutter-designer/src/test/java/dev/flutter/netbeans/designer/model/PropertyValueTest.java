package dev.flutter.netbeans.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PropertyValueTest {
    @Test
    void exposesEverySchemaV1ValueKind() {
        List<PropertyValue> values = List.of(
                new PropertyValue.StringValue(""),
                new PropertyValue.BooleanValue(true),
                new PropertyValue.IntegerValue(BigInteger.TEN),
                new PropertyValue.DoubleValue(new BigDecimal("1.25")),
                new PropertyValue.EnumValue("MainAxisAlignment", "center"),
                new PropertyValue.ColorValue(0xFFAABBCCL),
                new PropertyValue.EdgeInsetsValue(
                        BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TEN, BigDecimal.ZERO),
                new PropertyValue.AssetValue("assets/logo.png"),
                new PropertyValue.CallbackValue("onContinue"),
                new PropertyValue.DartExpressionValue("const SizedBox.shrink()"));

        assertEquals(List.of(
                PropertyValueKind.STRING,
                PropertyValueKind.BOOLEAN,
                PropertyValueKind.INTEGER,
                PropertyValueKind.DOUBLE,
                PropertyValueKind.ENUM,
                PropertyValueKind.COLOR,
                PropertyValueKind.EDGE_INSETS,
                PropertyValueKind.ASSET,
                PropertyValueKind.CALLBACK,
                PropertyValueKind.DART_EXPRESSION), values.stream().map(PropertyValue::kind).toList());
        assertEquals("edgeInsets", PropertyValueKind.EDGE_INSETS.wireName());
    }

    @Test
    void iconDataAcceptsUnicodeScalarsAndEnforcesNullableMetadataInvariants() {
        PropertyValue.IconDataValue star = new PropertyValue.IconDataValue(
                Optional.of(0xE5F9), Optional.of("MaterialIcons"),
                Optional.empty(), false, List.of("Fallback A", "Fallback B"));
        assertEquals(PropertyValueKind.ICON_DATA, star.kind());
        assertEquals(0xE5F9, star.codePoint().orElseThrow());
        assertEquals(PropertyValue.IconDataValue.none(),
                new PropertyValue.IconDataValue(
                        Optional.empty(), Optional.empty(), Optional.empty(),
                        false, List.of()));
        assertThrows(UnsupportedOperationException.class,
                () -> star.fontFamilyFallback().add("Other"));

        for (int invalid : List.of(-1, 0xD800, 0xDFFF, 0x110000)) {
            assertThrows(IllegalArgumentException.class, () -> icon(invalid, "Family"));
        }
        assertEquals(0x10FFFF, icon(0x10FFFF, "Family").codePoint().orElseThrow());
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.IconDataValue(
                        Optional.empty(), Optional.of("Family"), Optional.empty(),
                        false, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.IconDataValue(
                        Optional.of(1), Optional.empty(), Optional.of("package"),
                        false, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.IconDataValue(
                        Optional.of(1), Optional.of("Family"), Optional.empty(),
                        false, List.of("A", "A")));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.IconDataValue(
                        Optional.of(1), Optional.of("Family"), Optional.empty(),
                        false, java.util.Collections.nCopies(33, "A")));
    }

    @Test
    void iconDataMetadataRejectsOnlyTheExactSourceUnsafeCharacters() {
        List<Integer> rejected = new java.util.ArrayList<>(List.of(
                0, 0x1F, 0x7F, 0x061C, 0x200E, 0x200F,
                0x2028, 0x2029, 0x202A, 0x202E,
                0x2066, 0x2069, 0xFEFF));
        for (int codePoint : rejected) {
            String unsafe = "A" + new String(Character.toChars(codePoint)) + "B";
            assertThrows(IllegalArgumentException.class,
                    () -> icon(1, unsafe), "U+%04X".formatted(codePoint));
        }
        assertEquals("A\u200BB", icon(1, "A\u200BB").fontFamily().orElseThrow(),
                "unlisted Unicode format characters remain supported");
        for (int surroundingWhitespace : List.of(
                0x20, 0x1680,
                0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006,
                0x2008, 0x2009, 0x200A, 0x205F, 0x3000)) {
            String whitespace = new String(Character.toChars(surroundingWhitespace));
            assertThrows(IllegalArgumentException.class,
                    () -> icon(1, whitespace + "padded"),
                    "leading U+%04X".formatted(surroundingWhitespace));
            assertThrows(IllegalArgumentException.class,
                    () -> icon(1, "padded" + whitespace),
                    "trailing U+%04X".formatted(surroundingWhitespace));
        }
        assertEquals("A B", icon(1, "A B").fontFamily().orElseThrow());
        for (String edgeAccepted : List.of("\u00A0A\u00A0", "\u2007A\u2007", "\u202FA\u202F")) {
            assertEquals(edgeAccepted,
                    icon(1, edgeAccepted).fontFamily().orElseThrow());
        }
        assertThrows(IllegalArgumentException.class, () -> icon(1, "x".repeat(257)));
    }

    private static PropertyValue.IconDataValue icon(int codePoint, String family) {
        return new PropertyValue.IconDataValue(
                Optional.of(codePoint), Optional.of(family), Optional.empty(),
                false, List.of());
    }

    @Test
    void keepsArbitraryIntegersAndNormalizesDecimalSemanticEquality() {
        BigInteger huge = BigInteger.ONE.shiftLeft(4096);
        assertEquals(huge, new PropertyValue.IntegerValue(huge).value());
        assertEquals(
                new PropertyValue.DoubleValue(new BigDecimal("1.0")),
                new PropertyValue.DoubleValue(new BigDecimal("1.00000")));
        assertEquals(
                new PropertyValue.DoubleValue(new BigDecimal("-0.000")),
                new PropertyValue.DoubleValue(BigDecimal.ZERO));

        PropertyValue.EdgeInsetsValue first = new PropertyValue.EdgeInsetsValue(
                new BigDecimal("1.0"), new BigDecimal("2.00"), new BigDecimal("0.0"), new BigDecimal("-1.00"));
        PropertyValue.EdgeInsetsValue second = new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO, BigDecimal.ONE.negate());
        assertEquals(first, second);

        PropertyValue.EdgeInsetsDirectionalValue directional =
                new PropertyValue.EdgeInsetsDirectionalValue(
                        new BigDecimal("1.0"), new BigDecimal("2.00"),
                        new BigDecimal("3.000"), new BigDecimal("4.0"));
        assertEquals(new PropertyValue.EdgeInsetsDirectionalValue(
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.valueOf(3),
                BigDecimal.valueOf(4)), directional);
        assertEquals(PropertyValueKind.EDGE_INSETS, directional.kind());
    }

    @Test
    void enumUsesQualifiedIdentifierSegments() {
        assertEquals("Material.MainAxisAlignment",
                new PropertyValue.EnumValue("Material.MainAxisAlignment", "center").type());
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.EnumValue("Material..Alignment", "center"));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.EnumValue("Material.Alignment.", "center"));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.EnumValue("Material.Alignment", "bad-value"));
    }

    @Test
    void colorUsesUnsignedArgbRangeAndCanonicalWireText() {
        assertEquals("0x00000000", new PropertyValue.ColorValue(0).wireArgb());
        assertEquals("0xFFFFFFFF", new PropertyValue.ColorValue(0xFFFF_FFFFL).wireArgb());
        assertEquals(0x80ABCDEFL, PropertyValue.ColorValue.fromWireArgb("0x80ABCDEF").argb());
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ColorValue(-1));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ColorValue(0x1_0000_0000L));
        assertThrows(IllegalArgumentException.class,
                () -> PropertyValue.ColorValue.fromWireArgb("0xffFFFFFF"));
    }

    @Test
    void respectsCodePointLengthAndLexicalBoundaries() {
        String assetAtLimit = "😀".repeat(4096);
        String expressionAtLimit = "x".repeat(65_536);
        assertEquals(assetAtLimit, new PropertyValue.AssetValue(assetAtLimit).path());
        assertEquals(expressionAtLimit, new PropertyValue.DartExpressionValue(expressionAtLimit).code());
        assertEquals("_onTap2", new PropertyValue.CallbackValue("_onTap2").handler());

        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.AssetValue(""));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.AssetValue("😀".repeat(4097)));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.DartExpressionValue(""));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.DartExpressionValue("x".repeat(65_537)));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.CallbackValue("_"));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.CallbackValue("on-tap"));
    }

    @Test
    void rejectsNullPayloadsButKeepsEmptyStringAsARealValue() {
        assertEquals("", new PropertyValue.StringValue("").value());
        assertThrows(NullPointerException.class, () -> new PropertyValue.StringValue(null));
        assertThrows(NullPointerException.class, () -> new PropertyValue.IntegerValue(null));
        assertThrows(NullPointerException.class, () -> new PropertyValue.DoubleValue(null));
        assertThrows(NullPointerException.class,
                () -> new PropertyValue.EdgeInsetsValue(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertThrows(NullPointerException.class,
                () -> new PropertyValue.EdgeInsetsDirectionalValue(
                        null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void validatesThemeTokensAndRestrictsNestedColorSourcesToColorSchemeRoles() {
        ThemeToken color = new ThemeToken("material.colorScheme.primary");
        ThemeToken text = new ThemeToken("material.textTheme.bodyMedium");

        assertTrue(color.isColorSchemeToken());
        assertTrue(text.isTextThemeToken());
        assertEquals("bodyMedium", text.role());
        assertEquals(PropertyValueKind.THEME_TOKEN,
                new PropertyValue.ThemeTokenValue(text).kind());
        assertEquals("0xFFAABBCC", new ColorSource.Literal(0xFFAABBCCL).wireArgb());
        assertEquals(color, new ColorSource.Theme(color).token());

        assertThrows(IllegalArgumentException.class, () -> new ThemeToken("primary"));
        assertThrows(IllegalArgumentException.class,
                () -> new ThemeToken("material.colorScheme.Primary"));
        assertThrows(IllegalArgumentException.class, () -> new ColorSource.Theme(text));
    }

    @Test
    void modelsOnlyTheReviewedSafePaintSurface() {
        PropertyValue.PaintValue defaults = PropertyValue.PaintValue.defaults(
                new ColorSource.Theme(new ThemeToken("material.colorScheme.onSurface")));
        assertEquals(PropertyValue.PaintValue.BlendMode.SRC_OVER, defaults.blendMode());
        assertEquals(PropertyValue.PaintValue.Style.FILL, defaults.style());
        assertEquals(BigDecimal.valueOf(4), defaults.strokeMiterLimit());
        assertTrue(defaults.maskFilter().isEmpty());

        PropertyValue.PaintValue blurred = new PropertyValue.PaintValue(
                new ColorSource.Literal(0xFF000000L),
                PropertyValue.PaintValue.BlendMode.MULTIPLY,
                PropertyValue.PaintValue.Style.STROKE,
                new BigDecimal("2.5"),
                PropertyValue.PaintValue.StrokeCap.ROUND,
                PropertyValue.PaintValue.StrokeJoin.BEVEL,
                BigDecimal.TEN,
                false,
                PropertyValue.PaintValue.FilterQuality.HIGH,
                true,
                Optional.of(new PropertyValue.PaintValue.BlurMask(
                        PropertyValue.PaintValue.BlurStyle.OUTER,
                        new BigDecimal("3.5"))));
        assertEquals(PropertyValueKind.PAINT, blurred.kind());
        assertEquals("multiply", blurred.blendMode().wireName());

        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.PaintValue.BlurMask(
                PropertyValue.PaintValue.BlurStyle.NORMAL, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.PaintValue(
                new ColorSource.Literal(0),
                PropertyValue.PaintValue.BlendMode.SRC_OVER,
                PropertyValue.PaintValue.Style.FILL,
                BigDecimal.ONE.negate(),
                PropertyValue.PaintValue.StrokeCap.BUTT,
                PropertyValue.PaintValue.StrokeJoin.MITER,
                BigDecimal.ONE,
                true,
                PropertyValue.PaintValue.FilterQuality.NONE,
                false,
                Optional.empty()));
    }

    @Test
    void validatesOrderedShadowAndOpenTypeLists() {
        StableId firstId = StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        StableId secondId = StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        ColorSource black = new ColorSource.Literal(0xFF000000L);
        PropertyValue.ShadowListValue.Shadow shadow =
                new PropertyValue.ShadowListValue.Shadow(
                        firstId, black, BigDecimal.ONE.negate(), BigDecimal.TWO, BigDecimal.ZERO);
        PropertyValue.ShadowListValue shadows = new PropertyValue.ShadowListValue(List.of(shadow));
        assertEquals(firstId, shadows.items().getFirst().id());
        assertThrows(UnsupportedOperationException.class, () -> shadows.items().add(shadow));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.ShadowListValue(List.of(shadow, shadow)));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.ShadowListValue.Shadow(
                        secondId, black, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate()));

        PropertyValue.FontFeatureListValue.FontFeature liga =
                new PropertyValue.FontFeatureListValue.FontFeature(firstId, "liga", 1);
        assertEquals(List.of(liga), new PropertyValue.FontFeatureListValue(List.of(liga)).items());
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontFeatureListValue.FontFeature(secondId, "lig", 1));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontFeatureListValue.FontFeature(secondId, "liga", -1));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontFeatureListValue(List.of(
                        liga,
                        new PropertyValue.FontFeatureListValue.FontFeature(
                                secondId, "liga", 0))));

        PropertyValue.FontVariationListValue.FontVariation weight =
                new PropertyValue.FontVariationListValue.FontVariation(
                        firstId, "wght", new BigDecimal("700"));
        assertEquals(List.of(weight), new PropertyValue.FontVariationListValue(List.of(weight)).items());
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontVariationListValue.FontVariation(
                        secondId, "wght", new BigDecimal("1000.1")));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontVariationListValue.FontVariation(
                        secondId, "slnt", new BigDecimal("90")));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontVariationListValue.FontVariation(
                        secondId, "ABÇD", BigDecimal.ZERO));
    }

    @Test
    void modelsAlignmentConstraintsAndColumnMajorMatrixWithoutStringEscapes() {
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("-1.0"), BigDecimal.ONE);
        assertEquals(PropertyValueKind.ALIGNMENT_GEOMETRY, directional.kind());
        assertEquals(BigDecimal.ONE.negate(), directional.horizontal());

        PropertyValue.BoxConstraintsValue constraints =
                new PropertyValue.BoxConstraintsValue(
                        BigDecimal.TEN, Optional.empty(), BigDecimal.ZERO,
                        Optional.of(new BigDecimal("200.0")));
        assertEquals(PropertyValueKind.BOX_CONSTRAINTS, constraints.kind());
        assertTrue(constraints.maxWidth().isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.BoxConstraintsValue(
                        BigDecimal.TEN, Optional.of(BigDecimal.ONE),
                        BigDecimal.ZERO, Optional.empty()));

        List<BigDecimal> storage = java.util.stream.IntStream.range(0, 16)
                .mapToObj(BigDecimal::valueOf).toList();
        PropertyValue.Matrix4Value matrix = new PropertyValue.Matrix4Value(storage);
        assertEquals(PropertyValueKind.MATRIX4, matrix.kind());
        assertEquals(16, matrix.storage().size());
        for (int index = 0; index < 16; index++) {
            assertEquals(0, matrix.storage().get(index).compareTo(storage.get(index)));
        }
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.Matrix4Value(storage.subList(0, 15)));
    }

    @Test
    void modelsReviewedBoxDecorationUnionsAndRejectsFlutterPaintHazards() {
        ColorSource primary = new ColorSource.Theme(
                new ThemeToken("material.colorScheme.primary"));
        PropertyValue.BoxDecorationValue.BorderSide side =
                new PropertyValue.BoxDecorationValue.BorderSide(
                        primary, BigDecimal.TWO,
                        PropertyValue.BoxDecorationValue.BorderStyle.SOLID,
                        BigDecimal.ONE.negate());
        PropertyValue.BoxDecorationValue.PhysicalBorder border =
                new PropertyValue.BoxDecorationValue.PhysicalBorder(side, side, side, side);
        PropertyValue.BoxDecorationValue.DirectionalBorderRadius radius =
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.TWO),
                        new PropertyValue.BoxDecorationValue.Radius(BigDecimal.TWO, BigDecimal.ONE),
                        new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ZERO, BigDecimal.ZERO),
                        new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ZERO, BigDecimal.ZERO));
        PropertyValue.BoxDecorationValue.GradientStop first =
                new PropertyValue.BoxDecorationValue.GradientStop(
                        StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                        primary, BigDecimal.ZERO);
        PropertyValue.BoxDecorationValue.GradientStop second =
                new PropertyValue.BoxDecorationValue.GradientStop(
                        StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                        new ColorSource.Literal(0xFFFFFFFFL), BigDecimal.ONE);
        PropertyValue.AlignmentGeometryValue center = new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.ZERO, BigDecimal.ZERO);
        PropertyValue.BoxDecorationValue.LinearGradient gradient =
                new PropertyValue.BoxDecorationValue.LinearGradient(
                        center,
                        new PropertyValue.AlignmentGeometryValue(
                                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                                BigDecimal.ONE, BigDecimal.ONE),
                        List.of(first, second),
                        PropertyValue.BoxDecorationValue.TileMode.CLAMP,
                        Optional.of(new BigDecimal("0.25")));
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(primary), Optional.of(border), Optional.of(radius), List.of(),
                        Optional.of(gradient),
                        Optional.of(PropertyValue.PaintValue.BlendMode.SRC_OVER),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        assertEquals(PropertyValueKind.BOX_DECORATION, decoration.kind());
        assertEquals(List.of(first, second), gradient.stops());

        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.BoxDecorationValue(
                        Optional.empty(), Optional.empty(), Optional.of(radius), List.of(),
                        Optional.empty(), Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.CIRCLE));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.BoxDecorationValue.LinearGradient(
                        center, center, List.of(second, first),
                        PropertyValue.BoxDecorationValue.TileMode.CLAMP, Optional.empty()));
        PropertyValue.BoxDecorationValue.RadialGradient concentricGradient =
                new PropertyValue.BoxDecorationValue.RadialGradient(
                        center, BigDecimal.ONE, Optional.of(center), BigDecimal.ONE,
                        List.of(first, second), PropertyValue.BoxDecorationValue.TileMode.CLAMP,
                        Optional.empty());
        assertEquals(center, concentricGradient.focal().orElseThrow());

        PropertyValue.BoxDecorationValue.BorderSide otherColor =
                new PropertyValue.BoxDecorationValue.BorderSide(
                        new ColorSource.Literal(0xFF000000L), BigDecimal.ONE,
                        PropertyValue.BoxDecorationValue.BorderStyle.SOLID, BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.BoxDecorationValue(
                        Optional.empty(), Optional.of(
                                new PropertyValue.BoxDecorationValue.PhysicalBorder(
                                        side, otherColor, side, otherColor)),
                        Optional.of(radius), List.of(), Optional.empty(), Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE));
    }
}
