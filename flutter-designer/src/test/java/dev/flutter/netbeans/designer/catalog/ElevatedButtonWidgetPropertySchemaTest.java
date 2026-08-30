package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElevatedButtonWidgetPropertySchemaTest {
    private static final List<String> DIRECT_NAMES = List.of(
            "enabled",
            "onPressed",
            "onLongPress",
            "onHover",
            "onFocusChange",
            "autofocus",
            "clipBehavior");

    private static final List<StateExpectation> STATES = List.of(
            new StateExpectation(
                    "style", ElevatedButtonWidgetPropertySchema.Group.ENABLED_STYLE),
            new StateExpectation(
                    "styleDisabled", ElevatedButtonWidgetPropertySchema.Group.DISABLED_STYLE),
            new StateExpectation(
                    "stylePressed", ElevatedButtonWidgetPropertySchema.Group.PRESSED_STYLE),
            new StateExpectation(
                    "styleHovered", ElevatedButtonWidgetPropertySchema.Group.HOVERED_STYLE),
            new StateExpectation(
                    "styleFocused", ElevatedButtonWidgetPropertySchema.Group.FOCUSED_STYLE));

    private static final List<String> NON_TEXT_SUFFIXES = List.of(
            "BackgroundColor",
            "ForegroundColor",
            "OverlayColor",
            "ShadowColor",
            "SurfaceTintColor",
            "Elevation",
            "Padding",
            "MinimumWidth",
            "MinimumHeight",
            "FixedWidth",
            "FixedHeight",
            "MaximumWidth",
            "MaximumHeight",
            "IconColor",
            "IconSize",
            "SideColor",
            "SideWidth",
            "SideStyle",
            "SideStrokeAlign",
            "ShapeKind",
            "ShapeRadiusTopLeft",
            "ShapeRadiusTopRight",
            "ShapeRadiusBottomRight",
            "ShapeRadiusBottomLeft",
            "ShapeCircleEccentricity",
            "MouseCursor");

    private static final List<String> TEXT_SUFFIXES = List.of(
            "TextTheme",
            "TextInherit",
            "TextBackgroundColor",
            "TextFontSize",
            "TextFontWeight",
            "TextFontStyle",
            "TextLetterSpacing",
            "TextWordSpacing",
            "TextTextBaseline",
            "TextHeight",
            "TextLeadingDistribution",
            "TextLocaleLanguageCode",
            "TextLocaleScriptCode",
            "TextLocaleCountryCode",
            "TextBackground",
            "TextShadows",
            "TextFontFeatures",
            "TextFontVariations",
            "TextDecorationUnderline",
            "TextDecorationOverline",
            "TextDecorationLineThrough",
            "TextDecorationColor",
            "TextDecorationStyle",
            "TextDecorationThickness",
            "TextFontFamily",
            "TextFontFamilyFallback",
            "TextPackage",
            "TextOverflow");

    private static final List<String> COMMON_NAMES = List.of(
            "styleVisualDensityHorizontal",
            "styleVisualDensityVertical",
            "styleTapTargetSize",
            "styleAnimationDurationMs",
            "styleEnableFeedback",
            "styleAlignmentKind",
            "styleAlignmentX",
            "styleAlignmentY",
            "styleSplashFactory");

    @Test
    void exposesExactlySevenDirectFiveByFiftyFourStateAndNineCommonLeaves() {
        assertEquals(7, ElevatedButtonWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(5, ElevatedButtonWidgetPropertySchema.STATE_COUNT);
        assertEquals(26,
                ElevatedButtonWidgetPropertySchema.STATE_NON_TEXT_PROPERTY_COUNT);
        assertEquals(28,
                ElevatedButtonWidgetPropertySchema.STATE_TEXT_PROPERTY_COUNT);
        assertEquals(54, ElevatedButtonWidgetPropertySchema.STATE_PROPERTY_COUNT);
        assertEquals(9,
                ElevatedButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT);
        assertEquals(286,
                ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT);
        assertEquals(1, ElevatedButtonWidgetPropertySchema.SLOT_COUNT);

        LinkedHashSet<String> expected = new LinkedHashSet<>(DIRECT_NAMES);
        for (StateExpectation state : STATES) {
            for (String suffix : NON_TEXT_SUFFIXES) {
                assertTrue(expected.add(state.prefix() + suffix));
            }
            for (String suffix : TEXT_SUFFIXES) {
                assertTrue(expected.add(state.prefix() + suffix));
            }
        }
        assertTrue(expected.addAll(COMMON_NAMES));

        Map<String, ElevatedButtonWidgetPropertySchema.Definition> definitions =
                ElevatedButtonWidgetPropertySchema.definitions();
        assertEquals(286, expected.size());
        assertEquals(expected, definitions.keySet());
        assertEquals(new ArrayList<>(expected),
                new ArrayList<>(definitions.keySet()));
        assertEquals(definitions,
                ElevatedButtonWidgetPropertySchema.definitions());

        WidgetDefinition elevatedButton = BuiltInWidgetCatalog.getDefault()
                .find(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE)
                .orElseThrow();
        assertEquals(expected,
                elevatedButton.properties().stream()
                        .map(property -> property.name().value())
                        .collect(java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new)));
        assertEquals(ElevatedButtonWidgetPropertySchema.SLOT_COUNT,
                elevatedButton.slots().size());
    }

    @Test
    void assignsClosedStateTargetOrderAndEncodingMetadata() {
        Map<String, ElevatedButtonWidgetPropertySchema.Definition> definitions =
                ElevatedButtonWidgetPropertySchema.definitions();

        assertDefinition(definitions.get("enabled"),
                ElevatedButtonWidgetPropertySchema.Group.BEHAVIOR,
                ElevatedButtonWidgetPropertySchema.Target.ACTIVATION,
                ElevatedButtonWidgetPropertySchema.Encoding.SCALAR, 0);
        assertDefinition(definitions.get("onPressed"),
                ElevatedButtonWidgetPropertySchema.Group.EVENTS,
                ElevatedButtonWidgetPropertySchema.Target.ACTIVATION,
                ElevatedButtonWidgetPropertySchema.Encoding.SCALAR, 1);
        assertDefinition(definitions.get("onLongPress"),
                ElevatedButtonWidgetPropertySchema.Group.EVENTS,
                ElevatedButtonWidgetPropertySchema.Target.ACTIVATION,
                ElevatedButtonWidgetPropertySchema.Encoding.SCALAR, 2);
        for (int index = 3; index < DIRECT_NAMES.size(); index++) {
            String name = DIRECT_NAMES.get(index);
            ElevatedButtonWidgetPropertySchema.Group group = index < 5
                    ? ElevatedButtonWidgetPropertySchema.Group.EVENTS
                    : ElevatedButtonWidgetPropertySchema.Group.BEHAVIOR;
            assertDefinition(definitions.get(name), group,
                    ElevatedButtonWidgetPropertySchema.Target.DIRECT,
                    ElevatedButtonWidgetPropertySchema.Encoding.SCALAR, index);
        }

        for (StateExpectation state : STATES) {
            for (int index = 0; index < NON_TEXT_SUFFIXES.size(); index++) {
                assertDefinition(
                        definitions.get(state.prefix() + NON_TEXT_SUFFIXES.get(index)),
                        state.group(),
                        ElevatedButtonWidgetPropertySchema.Target.STYLE_STATE,
                        ElevatedButtonWidgetPropertySchema.Encoding.SCALAR,
                        index);
            }
            for (int index = 0; index < TEXT_SUFFIXES.size(); index++) {
                String suffix = TEXT_SUFFIXES.get(index);
                ElevatedButtonWidgetPropertySchema.Target target = textTarget(suffix);
                ElevatedButtonWidgetPropertySchema.Encoding encoding =
                        textEncoding(suffix);
                assertDefinition(definitions.get(state.prefix() + suffix),
                        state.group(), target, encoding,
                        NON_TEXT_SUFFIXES.size() + index);
            }
        }

        for (int index = 0; index < COMMON_NAMES.size(); index++) {
            assertDefinition(definitions.get(COMMON_NAMES.get(index)),
                    ElevatedButtonWidgetPropertySchema.Group.COMMON_STYLE,
                    ElevatedButtonWidgetPropertySchema.Target.STYLE_COMMON,
                    ElevatedButtonWidgetPropertySchema.Encoding.SCALAR,
                    index);
        }

        assertEquals(Map.of(
                ElevatedButtonWidgetPropertySchema.Target.DIRECT, 4L,
                ElevatedButtonWidgetPropertySchema.Target.ACTIVATION, 3L,
                ElevatedButtonWidgetPropertySchema.Target.STYLE_STATE, 130L,
                ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT, 110L,
                ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT_LOCALE, 15L,
                ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT_DECORATION, 15L,
                ElevatedButtonWidgetPropertySchema.Target.STYLE_COMMON, 9L),
                definitions.values().stream().collect(java.util.stream.Collectors.groupingBy(
                        ElevatedButtonWidgetPropertySchema.Definition::target,
                        java.util.stream.Collectors.counting())));
        assertEquals(5, definitions.values().stream()
                .filter(value -> value.encoding()
                == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST)
                .count());
        assertEquals(15, definitions.values().stream()
                .filter(value -> value.encoding()
                == ElevatedButtonWidgetPropertySchema.Encoding.DECORATION_FLAG)
                .count());
        assertTrue(definitions.values().stream().allMatch(value ->
                !value.displayName().isBlank()
                && !value.description().isBlank()
                && !value.dartName().isBlank()));
    }

    @Test
    void publishesAssemblyPathsWithoutExecutableExpressions() {
        assertEquals("minimumSize.width",
                ElevatedButtonWidgetPropertySchema.find("styleMinimumWidth")
                        .orElseThrow().dartName());
        assertEquals("side.strokeAlign",
                ElevatedButtonWidgetPropertySchema.find("styleHoveredSideStrokeAlign")
                        .orElseThrow().dartName());
        assertEquals("shape.radius.bottomLeft",
                ElevatedButtonWidgetPropertySchema.find(
                        "styleDisabledShapeRadiusBottomLeft")
                        .orElseThrow().dartName());
        assertEquals("textStyle.theme",
                ElevatedButtonWidgetPropertySchema.find("styleFocusedTextTheme")
                        .orElseThrow().dartName());
        assertEquals("textStyle.locale.languageCode",
                ElevatedButtonWidgetPropertySchema.find(
                        "stylePressedTextLocaleLanguageCode")
                        .orElseThrow().dartName());
        assertEquals("textStyle.decoration.underline",
                ElevatedButtonWidgetPropertySchema.find(
                        "styleDisabledTextDecorationUnderline")
                        .orElseThrow().dartName());
        assertEquals("animationDuration.milliseconds",
                ElevatedButtonWidgetPropertySchema.find("styleAnimationDurationMs")
                        .orElseThrow().dartName());
        assertTrue(ElevatedButtonWidgetPropertySchema.definitions().values().stream()
                .allMatch(value -> value.dartName().matches(
                        "[A-Za-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)*")));

        assertFalse(ElevatedButtonWidgetPropertySchema.isCompound(
                new PropertyName("onHover")));
        assertTrue(ElevatedButtonWidgetPropertySchema.isCompound(
                new PropertyName("enabled")));
        assertTrue(ElevatedButtonWidgetPropertySchema.isCompound(
                new PropertyName("styleBackgroundColor")));
        assertTrue(ElevatedButtonWidgetPropertySchema.isCompound(
                new PropertyName("styleSplashFactory")));
        assertTrue(ElevatedButtonWidgetPropertySchema.find(
                new PropertyName("notAButtonProperty")).isEmpty());

        assertEquals(Set.of(
                "elevatedButtonEvents",
                "elevatedButtonBehavior",
                "elevatedButtonEnabledStyle",
                "elevatedButtonDisabledStyle",
                "elevatedButtonPressedStyle",
                "elevatedButtonHoveredStyle",
                "elevatedButtonFocusedStyle",
                "elevatedButtonCommonStyle"),
                java.util.Arrays.stream(ElevatedButtonWidgetPropertySchema.Group.values())
                        .map(ElevatedButtonWidgetPropertySchema.Group::setName)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertEquals(List.of(
                "Events",
                "Behavior",
                "Style — enabled/default",
                "Style — disabled",
                "Style — pressed",
                "Style — hovered",
                "Style — focused",
                "Style — layout & feedback"),
                java.util.Arrays.stream(ElevatedButtonWidgetPropertySchema.Group.values())
                        .map(ElevatedButtonWidgetPropertySchema.Group::displayName)
                        .toList());
    }

    private static ElevatedButtonWidgetPropertySchema.Target textTarget(
            String suffix) {
        if (suffix.startsWith("TextLocale")) {
            return ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT_LOCALE;
        }
        if (suffix.equals("TextDecorationUnderline")
                || suffix.equals("TextDecorationOverline")
                || suffix.equals("TextDecorationLineThrough")) {
            return ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT_DECORATION;
        }
        return ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT;
    }

    private static ElevatedButtonWidgetPropertySchema.Encoding textEncoding(
            String suffix) {
        if (suffix.equals("TextFontFamilyFallback")) {
            return ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST;
        }
        if (textTarget(suffix)
                == ElevatedButtonWidgetPropertySchema.Target.STYLE_TEXT_DECORATION) {
            return ElevatedButtonWidgetPropertySchema.Encoding.DECORATION_FLAG;
        }
        return ElevatedButtonWidgetPropertySchema.Encoding.SCALAR;
    }

    private static void assertDefinition(
            ElevatedButtonWidgetPropertySchema.Definition definition,
            ElevatedButtonWidgetPropertySchema.Group group,
            ElevatedButtonWidgetPropertySchema.Target target,
            ElevatedButtonWidgetPropertySchema.Encoding encoding,
            int order) {
        assertEquals(group, definition.group());
        assertEquals(target, definition.target());
        assertEquals(encoding, definition.encoding());
        assertEquals(order, definition.dartOrder());
    }

    private record StateExpectation(
            String prefix,
            ElevatedButtonWidgetPropertySchema.Group group) {
    }
}
