package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElevatedButtonDartGenerationTest {
    private static final String HASH = "0".repeat(64);

    @Test
    void repeatedStructuredListsInDifferentStyleBucketsHaveIndependentOccurrenceIds() {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String prefix : List.of("style", "styleHovered")) {
            for (String suffix : List.of("Shadows", "FontFeatures", "FontVariations")) {
                properties.put(property(prefix + "Text" + suffix),
                        dev.flutter.netbeans.designer.catalog.BadgeTestValues.value("textStyle" + suffix));
            }
        }
        String dart = generate(properties);
        for (String expected : List.of("Shadow", "FontFeature", "FontVariation", "WidgetState.hovered")) {
            assertTrue(dart.contains(expected), dart);
        }
    }

    @Test
    void emitsSafeActivationModesAndRequiredNullableChild() {
        String fresh = generate(Map.of());
        assertTrue(fresh.contains("onPressed: () {}"), fresh);
        assertTrue(fresh.contains("child: null"), fresh);

        String disabled = generate(Map.ofEntries(
                value("enabled", new PropertyValue.BooleanValue(false)),
                value("onPressed", new PropertyValue.CallbackValue("handlePress")),
                value("onLongPress", new PropertyValue.CallbackValue("handleLongPress"))));
        assertTrue(disabled.contains("onPressed: null"), disabled);
        assertTrue(disabled.contains("onLongPress: null"), disabled);
        assertFalse(disabled.contains("handlePress"), disabled);
        assertFalse(disabled.contains("handleLongPress"), disabled);

        String longPressOnly = generate(Map.of(
                property("onLongPress"),
                new PropertyValue.CallbackValue("handleLongPress")));
        assertTrue(longPressOnly.contains("onPressed: null"), longPressOnly);
        assertTrue(longPressOnly.contains("onLongPress: handleLongPress"), longPressOnly);

        String allHandlers = generate(Map.ofEntries(
                value("onPressed", new PropertyValue.CallbackValue("handlePress")),
                value("onLongPress", new PropertyValue.CallbackValue("handleLongPress")),
                value("onHover", new PropertyValue.CallbackValue("handleHover")),
                value("onFocusChange", new PropertyValue.CallbackValue("handleFocus"))));
        assertTrue(allHandlers.contains("onPressed: handlePress"), allHandlers);
        assertTrue(allHandlers.contains("onLongPress: handleLongPress"), allHandlers);
        assertTrue(allHandlers.contains("onHover: handleHover"), allHandlers);
        assertTrue(allHandlers.contains("onFocusChange: handleFocus"), allHandlers);
    }

    @Test
    void emitsDirectSparseStateMapsInFlutterResolutionOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("styleBackgroundColor"), color(0xFF010101L));
        properties.put(property("styleDisabledBackgroundColor"), color(0xFF020202L));
        properties.put(property("stylePressedBackgroundColor"), color(0xFF030303L));
        properties.put(property("styleHoveredBackgroundColor"), color(0xFF040404L));
        properties.put(property("styleFocusedBackgroundColor"), color(0xFF050505L));
        properties.put(property("styleForegroundColor"), color(0xFF111111L));

        String generated = generate(properties);
        String background = member(generated, "backgroundColor:", "foregroundColor:");
        assertOrdered(background,
                "WidgetState.disabled", "WidgetState.pressed",
                "WidgetState.hovered", "WidgetState.focused", "WidgetState.any");
        String foreground = member(generated, "foregroundColor:", "overlayColor:");
        assertOrdered(foreground, "WidgetState.disabled: null", "WidgetState.any");
        assertTrue(generated.contains("WidgetStateProperty<Color?>.fromMap"), generated);
        assertFalse(generated.contains("styleFrom("), generated);
    }

    @Test
    void emitsSizesPaddingSideShapeTextAndEveryCommonButtonStyleField() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("stylePadding"), new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        properties.put(property("styleMinimumWidth"), integer(80));
        properties.put(property("styleFixedHeight"), integer(44));
        properties.put(property("styleMaximumWidth"), integer(320));
        properties.put(property("styleMaximumHeight"), integer(64));
        properties.put(property("styleSideColor"), color(0xFF112233L));
        properties.put(property("styleSideWidth"), decimal("2.5"));
        properties.put(property("styleSideStyle"),
                new PropertyValue.EnumValue("BorderStyle", "solid"));
        properties.put(property("styleSideStrokeAlign"), decimal("0.5"));
        properties.put(property("styleShapeKind"),
                new PropertyValue.StringValue("roundedSuperellipse"));
        properties.put(property("styleShapeRadiusTopLeft"), decimal("8"));
        properties.put(property("styleShapeRadiusTopRight"), decimal("9"));
        properties.put(property("styleShapeRadiusBottomRight"), decimal("10"));
        properties.put(property("styleShapeRadiusBottomLeft"), decimal("11"));
        properties.put(property("styleMouseCursor"),
                new PropertyValue.StringValue("zoomIn"));
        properties.put(property("styleTextTheme"),
                new PropertyValue.ThemeTokenValue(
                        new ThemeToken("material.textTheme.labelLarge")));
        properties.put(property("styleTextInherit"),
                new PropertyValue.BooleanValue(true));
        properties.put(property("styleDisabledTextInherit"),
                new PropertyValue.BooleanValue(true));
        properties.put(property("styleTextFontSize"), decimal("16"));
        properties.put(property("styleTextLocaleLanguageCode"),
                new PropertyValue.StringValue("uk"));
        properties.put(property("styleTextDecorationUnderline"),
                new PropertyValue.BooleanValue(true));
        properties.put(property("styleTextFontFamilyFallback"),
                new PropertyValue.StringValue("Roboto\nNoto Sans"));
        properties.put(property("styleVisualDensityHorizontal"), decimal("-1"));
        properties.put(property("styleVisualDensityVertical"), decimal("1"));
        properties.put(property("styleTapTargetSize"),
                new PropertyValue.EnumValue("MaterialTapTargetSize", "shrinkWrap"));
        properties.put(property("styleAnimationDurationMs"), integer(180));
        properties.put(property("styleEnableFeedback"),
                new PropertyValue.BooleanValue(false));
        properties.put(property("styleAlignmentKind"),
                new PropertyValue.StringValue("directional"));
        properties.put(property("styleAlignmentX"), decimal("-1"));
        properties.put(property("styleAlignmentY"), decimal("0"));
        properties.put(property("styleSplashFactory"),
                new PropertyValue.StringValue("inkSplash"));

        String generated = generate(properties);
        for (String expected : List.of(
                "padding: WidgetStateProperty<EdgeInsetsGeometry?>.fromMap",
                "minimumSize: WidgetStateProperty.resolveWith<Size?>",
                "fixedSize: WidgetStateProperty.resolveWith<Size?>",
                "maximumSize: WidgetStateProperty.resolveWith<Size?>",
                "side: WidgetStateProperty.resolveWith<BorderSide?>",
                "shape: WidgetStateProperty.resolveWith<OutlinedBorder?>",
                "mouseCursor: WidgetStateProperty<MouseCursor?>.fromMap",
                "textStyle: WidgetStateProperty.resolveWith<TextStyle?>",
                "const EdgeInsets.fromLTRB(1.0, 2.0, 3.0, 4.0)",
                "inheritedMinimum?.height ?? 0.0",
                "inherited?.width ?? double.infinity",
                "Size(320, 64)",
                "resolved = resolved.copyWith(color: const Color(0xFF112233)",
                "RoundedSuperellipseBorder(",
                "SystemMouseCursors.zoomIn",
                "var localAny = Theme.of(context).textTheme.labelLarge",
                "resolved = resolved.merge(localAny)",
                "Locale.fromSubtags(languageCode: 'uk'",
                "TextDecoration.underline",
                "const <String>['Roboto', 'Noto Sans']",
                "VisualDensity(horizontal: -1.0, vertical: 1.0)",
                "tapTargetSize: MaterialTapTargetSize.shrinkWrap",
                "animationDuration: const Duration(milliseconds: 180)",
                "enableFeedback: false",
                "alignment: const AlignmentDirectional(-1.0, 0.0)",
                "splashFactory: InkSplash.splashFactory")) {
            assertTrue(generated.contains(expected),
                    () -> "Missing `" + expected + "` in:\n" + generated);
        }
        assertFalse(generated.contains("DartExpression"), generated);
    }

    @Test
    void layersSparseCompoundLeavesAcrossSimultaneousStatesAndInheritedStyle() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("styleMinimumWidth"), integer(80));
        properties.put(property("styleFocusedMinimumWidth"), integer(90));
        properties.put(property("styleHoveredMinimumHeight"), integer(40));
        properties.put(property("stylePressedMinimumHeight"), integer(44));
        properties.put(property("styleSideColor"), color(0xFF010203L));
        properties.put(property("styleFocusedSideStyle"),
                new PropertyValue.EnumValue("BorderStyle", "solid"));
        properties.put(property("styleHoveredSideWidth"), decimal("2"));
        properties.put(property("stylePressedSideStrokeAlign"), decimal("0.5"));
        properties.put(property("styleTextInherit"),
                new PropertyValue.BooleanValue(false));
        properties.put(property("styleDisabledTextInherit"),
                new PropertyValue.BooleanValue(false));
        properties.put(property("styleTextFontSize"), decimal("14"));
        properties.put(property("styleFocusedTextFontWeight"),
                new PropertyValue.EnumValue("FontWeight", "w600"));
        properties.put(property("styleHoveredTextLetterSpacing"), decimal("1"));
        properties.put(property("stylePressedTextWordSpacing"), decimal("2"));
        properties.put(property("styleVisualDensityHorizontal"), decimal("-1"));

        String generated = generate(properties);
        assertTrue(generated.contains(
                "ElevatedButtonTheme.of(context).style?.minimumSize?.resolve(states)"),
                generated);
        assertTrue(generated.contains(
                "states.contains(WidgetState.focused) ? 90 : 80"), generated);
        assertTrue(generated.contains(
                "states.contains(WidgetState.pressed) ? 44 : "
                + "states.contains(WidgetState.hovered) ? 40 : "
                + "inheritedMinimum?.height ?? 0.0"), generated);
        String side = member(generated, "side:", "shape:");
        assertOrdered(side,
                "resolved = resolved.copyWith(color:",
                "WidgetState.focused",
                "resolved = resolved.copyWith(style:",
                "WidgetState.hovered",
                "resolved = resolved.copyWith(width:",
                "WidgetState.pressed",
                "resolved = resolved.copyWith(strokeAlign:");
        String text = textStyleMember(generated);
        assertOrdered(text,
                "localAny = localAny.copyWith(inherit: false, fontSize: 14.0)",
                "resolved = resolved.merge(localAny)",
                "WidgetState.focused",
                "localFocused = localFocused.copyWith(",
                "fontWeight: FontWeight.w600",
                "WidgetState.hovered",
                "localHovered = localHovered.copyWith(",
                "letterSpacing: 1.0",
                "WidgetState.pressed",
                "localPressed = localPressed.copyWith(",
                "wordSpacing: 2.0");
        assertTrue(generated.contains(
                "inherited.vertical"), generated);
        assertFalse(generated.contains("Size(0.0, 44)"), generated);
    }

    @Test
    void guardsDisabledBeforeEnabledOnlyAtomicAndCompoundStates() {
        String generated = generate(Map.ofEntries(
                value("styleFocusedBackgroundColor", color(0xFF123456L)),
                value("styleFocusedMinimumWidth", integer(100))));

        String background = member(generated, "backgroundColor:", "foregroundColor:");
        assertOrdered(background,
                "WidgetState.disabled: null", "WidgetState.focused");
        String size = member(generated, "minimumSize:", "fixedSize:");
        assertOrdered(size,
                "WidgetState.disabled", "return null",
                "!states.contains(WidgetState.focused)",
                "final effectiveMinimum");
    }

    @Test
    void emitsPairedBoundsAndLetsMinimumWidenExternalThemeMaximum() {
        String generated = generate(Map.of(
                property("styleMinimumWidth"), integer(120)));

        String minimum = member(generated, "minimumSize:", "maximumSize:");
        String maximum = member(generated, "maximumSize:", "\n      ),");
        for (String resolver : List.of(minimum, maximum)) {
            assertOrdered(resolver,
                    "inheritedMinimum = ElevatedButtonTheme.of(context)"
                    + ".style?.minimumSize?.resolve(states)",
                    "inheritedMaximum = ElevatedButtonTheme.of(context)"
                    + ".style?.maximumSize?.resolve(states)",
                    "final effectiveMinimum = Size(120,",
                    "final unresolvedMaximum = Size(",
                    "unresolvedMaximum.width < effectiveMinimum.width",
                    "? effectiveMinimum.width");
        }
        assertTrue(minimum.contains("return effectiveMinimum"), minimum);
        assertTrue(maximum.contains("return effectiveMaximum"), maximum);
    }

    @Test
    void emitsPairedBoundsAndLetsExternalThemeMinimumWidenLocalMaximum() {
        String generated = generate(Map.of(
                property("styleMaximumWidth"), integer(80)));

        assertEquals(1, count(generated,
                "minimumSize: WidgetStateProperty.resolveWith<Size?>"), generated);
        assertEquals(1, count(generated,
                "maximumSize: WidgetStateProperty.resolveWith<Size?>"), generated);
        assertEquals(2, count(generated,
                "final unresolvedMaximum = Size(80,"), generated);
        assertEquals(2, count(generated,
                "unresolvedMaximum.width < effectiveMinimum.width"), generated);
    }

    @Test
    void pairsActiveBoundLayersAndKeepsDisabledIsolated() {
        String generated = generate(Map.ofEntries(
                value("styleFocusedMinimumWidth", integer(90)),
                value("styleHoveredMaximumWidth", integer(110)),
                value("stylePressedMinimumHeight", integer(50))));

        String minimum = member(generated, "minimumSize:", "maximumSize:");
        String maximum = member(generated, "maximumSize:", "\n      ),");
        for (String resolver : List.of(minimum, maximum)) {
            assertOrdered(resolver,
                    "WidgetState.disabled",
                    "return null",
                    "!states.contains(WidgetState.focused)",
                    "!states.contains(WidgetState.hovered)",
                    "!states.contains(WidgetState.pressed)",
                    "states.contains(WidgetState.focused) ? 90",
                    "states.contains(WidgetState.pressed) ? 50",
                    "states.contains(WidgetState.hovered) ? 110",
                    "final effectiveMaximum");
        }
    }

    @Test
    void stateBackgroundColorClearsInheritedPaintAndShapeRadiusUsesBaseKind() {
        PropertyValue.PaintValue basePaint = PropertyValue.PaintValue.defaults(
                new dev.flutter.netbeans.designer.model.ColorSource.Literal(
                        0xFF010203L));
        String generated = generate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(true)),
                value("styleDisabledTextInherit", new PropertyValue.BooleanValue(true)),
                value("styleTextBackground", basePaint),
                value("stylePressedTextBackgroundColor", color(0xFFABCDEFL)),
                value("styleShapeKind",
                        new PropertyValue.StringValue("roundedRectangle")),
                value("stylePressedShapeRadiusTopLeft", decimal("12"))));

        assertTrue(generated.contains(
                "background: (Paint()..color = const Color(0xFFABCDEF))"), generated);
        assertFalse(generated.contains(
                "backgroundColor: const Color(0xFFABCDEF)"), generated);
        assertTrue(generated.contains(
                "states.contains(WidgetState.pressed) ? "
                + "Radius.circular(12.0) : inheritedRadii.topLeft"), generated);
        assertTrue(generated.contains("RoundedRectangleBorder("), generated);
    }

    @Test
    void inheritFalseStateFragmentCutsLowerTextLeavesButKeepsSameStateTheme() {
        String generated = generate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(false)),
                value("styleDisabledTextInherit", new PropertyValue.BooleanValue(false)),
                value("styleTextFontWeight",
                        new PropertyValue.EnumValue("FontWeight", "w700")),
                value("styleTextLetterSpacing", decimal("1.5")),
                value("stylePressedTextTheme",
                        new PropertyValue.ThemeTokenValue(
                                new ThemeToken("material.textTheme.labelLarge"))),
                value("stylePressedTextInherit",
                        new PropertyValue.BooleanValue(false)),
                value("stylePressedTextFontSize", decimal("18")),
                value("stylePressedTextBackgroundColor", color(0xFF334455L))));

        String text = textStyleMember(generated);
        assertOrdered(text,
                "localAny = const TextStyle()",
                "localAny = localAny.copyWith(inherit: false, fontWeight: "
                + "FontWeight.w700, letterSpacing: 1.5)",
                "resolved = resolved.merge(localAny)",
                "WidgetState.pressed",
                "localPressed = Theme.of(context).textTheme.labelLarge",
                "localPressed = localPressed.copyWith(inherit: false, "
                + "background: (Paint()..color = const Color(0xFF334455)), "
                + "fontSize: 18.0)",
                "resolved = resolved.merge(localPressed)");
        assertFalse(text.contains("resolved = resolved.copyWith(inherit: false"), text);
    }

    @Test
    void localBackgroundPaintOverridesLowerColorAndFrameworkFallbackIsDirect() {
        PropertyValue.PaintValue paint = PropertyValue.PaintValue.defaults(
                new dev.flutter.netbeans.designer.model.ColorSource.Literal(
                        0xFF778899L));
        String generated = generate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(true)),
                value("styleDisabledTextInherit", new PropertyValue.BooleanValue(true)),
                value("styleTextBackgroundColor", color(0xFF010203L)),
                value("styleHoveredTextBackground", paint),
                value("styleFixedWidth", integer(120)),
                value("styleMaximumHeight", integer(60)),
                value("styleVisualDensityHorizontal", decimal("2"))));

        assertTrue(generated.contains(
                "localHovered = localHovered.copyWith(background: (Paint()"), generated);
        assertTrue(generated.contains(
                "return Size(120, inherited?.height ?? double.infinity)"), generated);
        assertTrue(generated.contains(
                "final unresolvedMaximum = Size("
                + "inheritedMaximum?.width ?? double.infinity, 60)"), generated);
        assertTrue(count(generated, ".defaultStyleOf(context)") > 0, generated);
        assertFalse(generated.contains("invalid_use_of_protected_member"), generated);
        assertFalse(generated.contains("?? (const ElevatedButton"), generated);
    }

    @Test
    void packagesEffectiveRawFamilyAndNormalizesFallbackOnlyWithoutNullFamily() {
        String family = generate(Map.ofEntries(
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));
        String familyText = textStyleMember(family);
        assertOrdered(familyText,
                "localAny = localAny.copyWith(fontFamily: 'BaseFamily')",
                "WidgetState.pressed",
                "resolved = resolved.merge(localPressed)",
                "final packagePressed = 'button_fonts'",
                "resolved = resolved.copyWith(package: packagePressed)");
        String pressed = familyText.substring(
                familyText.indexOf("WidgetState.pressed"));
        assertFalse(pressed.contains("fontFamily: 'BaseFamily'"), pressed);
        assertFalse(familyText.contains("packages/button_fonts/null"), familyText);

        String fallback = generate(Map.ofEntries(
                value("styleTextFontFamilyFallback",
                        new PropertyValue.StringValue("Roboto\nNoto Sans")),
                value("styleHoveredTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));
        String fallbackText = textStyleMember(fallback);
        assertTrue(fallbackText.contains(
                "resolved.fontFamilyFallback?.map((family)"), fallbackText);
        assertTrue(fallbackText.contains(
                "'packages/$packageHovered/$family'"), fallbackText);
        assertFalse(fallbackText.contains("packages/button_fonts/null"), fallbackText);
    }

    @Test
    void keepsReceiverPackageWhenActiveStateOverridesFamilyOrFallback() {
        String family = generate(Map.ofEntries(
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("styleTextPackage",
                        new PropertyValue.StringValue("base_fonts")),
                value("stylePressedTextFontFamily",
                        new PropertyValue.StringValue("PressedFamily"))));
        String familyText = textStyleMember(family);
        assertOrdered(familyText,
                "localAny = localAny.copyWith(fontFamily: 'BaseFamily')",
                "resolved = resolved.merge(localAny)",
                "final packageAny = 'base_fonts'",
                "resolved = resolved.copyWith(fontFamily: 'BaseFamily', "
                + "package: packageAny)",
                "WidgetState.pressed");
        String pressedFamily = familyText.substring(
                familyText.indexOf("WidgetState.pressed"));
        assertTrue(pressedFamily.contains(
                "localPressed.copyWith(fontFamily: 'PressedFamily')"),
                pressedFamily);
        assertFalse(pressedFamily.contains("package: 'base_fonts'"),
                pressedFamily);

        String fallback = generate(Map.ofEntries(
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("styleTextPackage",
                        new PropertyValue.StringValue("base_fonts")),
                value("stylePressedTextFontFamilyFallback",
                        new PropertyValue.StringValue("Pressed Fallback"))));
        String fallbackText = textStyleMember(fallback);
        String pressedFallback = fallbackText.substring(
                fallbackText.indexOf("WidgetState.pressed"));
        assertTrue(pressedFallback.contains(
                "localPressed.copyWith(fontFamilyFallback: "
                + "const <String>['Pressed Fallback'])"), pressedFallback);
        assertFalse(pressedFallback.contains("package: 'base_fonts'"),
                pressedFallback);
    }

    @Test
    void appliesPackageToTheResolvedFamilyAfterAllActiveLowerStateLayers() {
        String generated = generate(Map.ofEntries(
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("styleHoveredTextFontFamily",
                        new PropertyValue.StringValue("HoverFamily")),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));

        String text = textStyleMember(generated);
        assertOrdered(text,
                "localAny = localAny.copyWith(fontFamily: 'BaseFamily')",
                "WidgetState.hovered",
                "localHovered = localHovered.copyWith(fontFamily: 'HoverFamily')",
                "resolved = resolved.merge(localHovered)",
                "WidgetState.pressed",
                "resolved = resolved.merge(localPressed)",
                "final packagePressed = 'button_fonts'",
                "resolved = resolved.copyWith(package: packagePressed)");
        String pressed = text.substring(text.indexOf("WidgetState.pressed"));
        assertFalse(pressed.contains("fontFamily: 'BaseFamily'"), pressed);
        assertFalse(pressed.contains("fontFamily: 'HoverFamily'"), pressed);
        assertFalse(pressed.contains("localPressed.copyWith()"), pressed);
    }

    @Test
    void appliesPackageToCurrentThemeResolvedFamilyOrFallback() {
        String generated = generate(Map.ofEntries(
                value("styleTextInherit",
                        new PropertyValue.BooleanValue(true)),
                value("styleDisabledTextInherit",
                        new PropertyValue.BooleanValue(true)),
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("styleHoveredTextTheme",
                        new PropertyValue.ThemeTokenValue(
                                new ThemeToken("material.textTheme.labelLarge"))),
                value("styleHoveredTextInherit",
                        new PropertyValue.BooleanValue(true)),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));

        String text = textStyleMember(generated);
        assertOrdered(text,
                "WidgetState.hovered",
                "localHovered = Theme.of(context).textTheme.labelLarge",
                "resolved = resolved.merge(localHovered)",
                "WidgetState.pressed",
                "final resolvedFontFamilyPressed = resolved.fontFamily",
                "resolved.fontFamilyFallback?.map((family)",
                "resolved = resolved.copyWith(package: packagePressed)");
        String pressed = text.substring(text.indexOf("WidgetState.pressed"));
        assertFalse(pressed.contains("fontFamily: 'BaseFamily'"), pressed);
    }

    @Test
    void replacesOneOldFallbackPackagePrefixWithoutNullOrDoublePrefix() {
        String generated = generate(Map.ofEntries(
                value("styleTextFontFamilyFallback",
                        new PropertyValue.StringValue("FallbackFamily")),
                value("styleTextPackage",
                        new PropertyValue.StringValue("old_fonts")),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("new_fonts"))));

        String text = textStyleMember(generated);
        String pressed = text.substring(text.indexOf("WidgetState.pressed"));
        assertOrdered(pressed,
                "final packagePressed = 'new_fonts'",
                "resolved.fontFamilyFallback?.map((family)",
                "final separator = family.indexOf('/', 9)",
                "family.substring(separator + 1)",
                "'packages/$packagePressed/$family'",
                "resolved = TextStyle(",
                "fontFamilyFallback: repackagedFallbackPressed");
        assertFalse(pressed.contains("packages/new_fonts/null"), pressed);
        assertFalse(pressed.contains("packages/$packagePressed/packages/"),
                pressed);
    }

    @Test
    void normalizesThemeBackgroundAndForcesExplicitTrueInheritAfterMerge() {
        String generated = generate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(true)),
                value("styleDisabledTextInherit", new PropertyValue.BooleanValue(true)),
                value("stylePressedTextTheme",
                        new PropertyValue.ThemeTokenValue(
                                new ThemeToken("material.textTheme.labelLarge"))),
                value("stylePressedTextInherit",
                        new PropertyValue.BooleanValue(true))));

        String text = textStyleMember(generated);
        assertTrue(text.contains(
                "if (localPressed.backgroundColor != null && "
                + "localPressed.background == null)"), text);
        assertTrue(text.contains(
                "localPressed.copyWith(background: (Paint()..color = "
                + "localPressed.backgroundColor!))"), text);
        assertTrue(text.contains(
                "resolved.merge(localPressed).copyWith(inherit: true)"), text);
        assertFalse(text.contains("invalid_use_of_protected_member"), text);
    }

    private static String member(String generated, String start, String next) {
        int startIndex = generated.indexOf(start);
        assertTrue(startIndex >= 0, generated);
        int endIndex = generated.indexOf(next, startIndex + start.length());
        return endIndex < 0 ? generated.substring(startIndex)
                : generated.substring(startIndex, endIndex);
    }

    private static String textStyleMember(String generated) {
        return member(generated, "textStyle:", "\n      child:");
    }

    private static void assertOrdered(String value, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = value.indexOf(token);
            assertTrue(current > previous,
                    () -> "Expected ordered token `" + token + "` in " + value);
            previous = current;
        }
    }

    private static int count(String value, String token) {
        int result = 0;
        for (int index = value.indexOf(token); index >= 0;
                index = value.indexOf(token, index + token.length())) {
            result++;
        }
        return result;
    }

    private static String generate(Map<PropertyName, PropertyValue> properties) {
        WidgetNode button = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.material.ElevatedButton"),
                properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(button), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),
                () -> "Generation diagnostics: " + result.diagnostics());
        return result.generated().orElseThrow().build().payload();
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(
                StableId.random(),
                new DartSourceDescriptor(
                        "button.dart", "ButtonView", WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(
                                new ManagedRegion(HASH), new ManagedRegion(HASH))),
                root);
    }

    private static Map.Entry<PropertyName, PropertyValue> value(
            String name,
            PropertyValue value) {
        return Map.entry(property(name), value);
    }

    private static PropertyName property(String name) {
        return new PropertyName(name);
    }

    private static PropertyValue.ColorValue color(long argb) {
        return new PropertyValue.ColorValue(argb);
    }

    private static PropertyValue.IntegerValue integer(long value) {
        return new PropertyValue.IntegerValue(BigInteger.valueOf(value));
    }

    private static PropertyValue.DoubleValue decimal(String value) {
        return new PropertyValue.DoubleValue(new BigDecimal(value));
    }
}
