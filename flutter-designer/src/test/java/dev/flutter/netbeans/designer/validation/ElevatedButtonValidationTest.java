package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElevatedButtonValidationTest {
    private static final String HASH = "0".repeat(64);

    @Test
    void rejectsMinimumAboveMaximumButAllowsFlutterFixedSizeClamping() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleMinimumWidth", integer(100)),
                value("styleMaximumWidth", integer(80)),
                value("styleFixedWidth", integer(70)),
                value("stylePressedMinimumHeight", integer(20)),
                value("stylePressedMaximumHeight", integer(40)),
                value("stylePressedFixedHeight", integer(50))));

        assertEquals(Set.of(
                        "/root/properties/styleMaximumWidth"),
                result.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_CONFLICT))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void validatesEffectiveDimensionsAcrossEveryActiveStateCombination() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleFocusedMinimumWidth", integer(100)),
                value("styleHoveredMaximumWidth", integer(80)),
                value("stylePressedMinimumHeight", integer(60)),
                value("styleFocusedMaximumHeight", integer(50))));

        assertEquals(Set.of(
                        "/root/properties/styleHoveredMaximumWidth",
                        "/root/properties/styleFocusedMaximumHeight"),
                result.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_CONFLICT))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void requiresACompleteDeterministicAlignmentTriple() {
        ValidationResult partial = validate(Map.of(
                property("styleAlignmentX"), decimal("0.5")));
        assertEquals(1, partial.errors().stream()
                .filter(issue -> issue.code().equals(
                        WidgetTreeValidator.PROPERTY_DEPENDENCY))
                .count());
        assertTrue(partial.errors().stream().anyMatch(issue ->
                issue.path().equals("/root/properties/styleAlignmentKind")));

        ValidationResult complete = validate(Map.ofEntries(
                value("styleAlignmentKind",
                        new PropertyValue.StringValue("physical")),
                value("styleAlignmentX", decimal("0.5")),
                value("styleAlignmentY", decimal("-0.5"))));
        assertTrue(complete.valid(), () -> "Issues: " + complete.issues());
    }

    @Test
    void validatesTextStyleAndShapeRelationshipsPerStatePrefix() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(property("styleTextBackgroundColor"),
                new PropertyValue.ColorValue(0xFF112233L));
        properties.put(property("styleTextBackground"),
                PropertyValue.PaintValue.defaults(
                        new ColorSource.Literal(0xFF445566L)));
        properties.put(property("styleTextPackage"),
                new PropertyValue.StringValue("example_fonts"));
        properties.put(property("styleHoveredShapeRadiusTopLeft"), decimal("4"));
        properties.put(property("styleFocusedShapeKind"),
                new PropertyValue.StringValue("stadium"));
        properties.put(property("styleFocusedShapeRadiusTopRight"), decimal("4"));
        properties.put(property("styleDisabledShapeKind"),
                new PropertyValue.StringValue("roundedRectangle"));
        properties.put(property("styleDisabledShapeCircleEccentricity"),
                decimal("0.5"));

        ValidationResult result = validate(properties);
        assertEquals(Set.of(
                        "/root/properties/styleTextBackground",
                        "/root/properties/styleTextPackage",
                        "/root/properties/styleHoveredShapeRadiusTopLeft",
                        "/root/properties/styleFocusedShapeRadiusTopRight",
                        "/root/properties/styleDisabledShapeCircleEccentricity"),
                result.errors().stream()
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void acceptsFontPackageWithFallbackAndReviewedSuperellipseRadii() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleTextPackage",
                        new PropertyValue.StringValue("example_fonts")),
                value("styleTextFontFamilyFallback",
                        new PropertyValue.StringValue("Roboto\nNoto Sans")),
                value("styleShapeKind",
                        new PropertyValue.StringValue("roundedSuperellipse")),
                value("styleShapeRadiusTopLeft", decimal("8")),
                value("styleShapeRadiusBottomRight", decimal("8"))));

        assertTrue(result.valid(), () -> "Issues: " + result.issues());
    }

    @Test
    void stateShapeLocaleAndPackageLeavesMayInheritEnabledBaseLeaves() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleShapeKind",
                        new PropertyValue.StringValue("roundedRectangle")),
                value("stylePressedShapeRadiusTopLeft", decimal("8")),
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("Roboto")),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("example_fonts")),
                value("styleTextLocaleLanguageCode",
                        new PropertyValue.StringValue("uk")),
                value("styleHoveredTextLocaleScriptCode",
                        new PropertyValue.StringValue("Latn"))));

        assertTrue(result.valid(), () -> "Issues: " + result.issues());
    }

    @Test
    void disabledShapeRemainsIsolatedWhileLocaleUsesSafeUndFallback() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleShapeKind",
                        new PropertyValue.StringValue("roundedRectangle")),
                value("styleDisabledShapeRadiusTopLeft", decimal("8")),
                value("styleTextLocaleLanguageCode",
                        new PropertyValue.StringValue("uk")),
                value("styleDisabledTextLocaleCountryCode",
                        new PropertyValue.StringValue("UA"))));

        assertEquals(Set.of(
                        "/root/properties/styleDisabledShapeRadiusTopLeft"),
                result.errors().stream()
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void enforcesOneExplicitTextInheritValueAcrossReachableStates() {
        ValidationResult safe = validate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(false)),
                value("styleDisabledTextInherit",
                        new PropertyValue.BooleanValue(false)),
                value("stylePressedTextInherit",
                        new PropertyValue.BooleanValue(false)),
                value("stylePressedTextTheme",
                        new PropertyValue.ThemeTokenValue(
                                new dev.flutter.netbeans.designer.model.ThemeToken(
                                        "material.textTheme.labelLarge")))));
        assertTrue(safe.valid(), () -> "Issues: " + safe.issues());

        ValidationResult unsafe = validate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(false)),
                value("styleDisabledTextInherit",
                        new PropertyValue.BooleanValue(true)),
                value("styleHoveredTextTheme",
                        new PropertyValue.ThemeTokenValue(
                                new dev.flutter.netbeans.designer.model.ThemeToken(
                                        "material.textTheme.labelLarge")))));
        assertEquals(Set.of(
                        "/root/properties/styleDisabledTextInherit",
                        "/root/properties/styleHoveredTextInherit"),
                unsafe.errors().stream()
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void rejectsSparseMaximumBelowPinnedMaterial3FrameworkMinimum() {
        ValidationResult enabled = validate(Map.ofEntries(
                value("styleMaximumWidth", integer(20)),
                value("styleHoveredMaximumHeight", integer(39))));
        assertEquals(Set.of(
                        "/root/properties/styleMaximumWidth",
                        "/root/properties/styleHoveredMaximumHeight"),
                enabled.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_CONFLICT))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));

        ValidationResult disabled = validate(Map.of(
                property("styleDisabledMaximumWidth"), integer(20)));
        assertEquals(Set.of("/root/properties/styleDisabledMaximumWidth"),
                disabled.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_CONFLICT))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void allowsFixedWidthOnlyWithFiniteMaximumHeightBecauseFlutterClampsAxis() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleFixedWidth", integer(120)),
                value("styleMaximumHeight", integer(60))));

        assertTrue(result.valid(), () -> "Issues: " + result.issues());
    }

    @Test
    void validatesShapeLeavesAgainstEveryEffectiveActiveStateKind() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleShapeKind",
                        new PropertyValue.StringValue("roundedRectangle")),
                value("styleShapeRadiusTopLeft", decimal("4")),
                value("styleFocusedShapeKind",
                        new PropertyValue.StringValue("circle")),
                value("styleHoveredShapeCircleEccentricity", decimal("0.5")),
                value("stylePressedShapeRadiusBottomRight", decimal("6"))));

        assertEquals(Set.of(
                        "/root/properties/styleShapeRadiusTopLeft",
                        "/root/properties/styleHoveredShapeCircleEccentricity",
                        "/root/properties/stylePressedShapeRadiusBottomRight"),
                result.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_CONFLICT))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void localeSubtagsCanUseInheritedLocaleOrUndWithoutLanguageLeaf() {
        ValidationResult result = validate(Map.ofEntries(
                value("styleTextLocaleScriptCode",
                        new PropertyValue.StringValue("Latn")),
                value("styleDisabledTextLocaleCountryCode",
                        new PropertyValue.StringValue("UA"))));

        assertTrue(result.valid(), () -> "Issues: " + result.issues());
    }

    @Test
    void packageUsesEnabledRawFamilyOnlyWhenStateStillInherits() {
        ValidationResult inherited = validate(Map.ofEntries(
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));
        assertTrue(inherited.valid(), () -> "Issues: " + inherited.issues());

        ValidationResult isolated = validate(Map.ofEntries(
                value("styleTextInherit", new PropertyValue.BooleanValue(false)),
                value("styleDisabledTextInherit",
                        new PropertyValue.BooleanValue(false)),
                value("styleTextFontFamily",
                        new PropertyValue.StringValue("BaseFamily")),
                value("stylePressedTextInherit",
                        new PropertyValue.BooleanValue(false)),
                value("stylePressedTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));
        assertEquals(Set.of("/root/properties/stylePressedTextPackage"),
                isolated.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_DEPENDENCY))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));

        ValidationResult disabled = validate(Map.ofEntries(
                value("styleTextFontFamilyFallback",
                        new PropertyValue.StringValue("Roboto")),
                value("styleDisabledTextPackage",
                        new PropertyValue.StringValue("button_fonts"))));
        assertEquals(Set.of("/root/properties/styleDisabledTextPackage"),
                disabled.errors().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_DEPENDENCY))
                        .map(ValidationIssue::path)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    private static ValidationResult validate(
            Map<PropertyName, PropertyValue> properties) {
        WidgetNode button = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.material.ElevatedButton"),
                properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        return new WidgetTreeValidator().validate(
                document(button), BuiltInWidgetCatalog.getDefault());
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

    private static PropertyValue.IntegerValue integer(long value) {
        return new PropertyValue.IntegerValue(BigInteger.valueOf(value));
    }

    private static PropertyValue.DoubleValue decimal(String value) {
        return new PropertyValue.DoubleValue(new BigDecimal(value));
    }
}
