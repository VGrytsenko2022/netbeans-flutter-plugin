package dev.flutter.netbeans.designer.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DartSymbolReference;
import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotAcceptance;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WidgetTreeValidatorTest {

    @Test
    void exposesStableLiteralIssueCodes() {
        assertEquals(List.of(
                "designer.widget.id.duplicate",
                "designer.widget.type.unknown",
                "designer.property.missing",
                "designer.property.unknown",
                "designer.property.kind",
                "designer.property.constraint",
                "designer.property.dependency",
                "designer.property.conflict",
                "designer.property.override",
                "designer.property.uniqueness",
                "designer.slot.missing",
                "designer.slot.unknown",
                "designer.slot.kind",
                "designer.slot.cardinality",
                "designer.slot.null",
                "designer.slot.acceptance",
                "designer.parameter.positional.gap",
                "designer.tree.depth.limit",
                "designer.tree.nodes.limit",
                "designer.widget.properties.limit",
                "designer.widget.slots.limit",
                "designer.validation.issues.truncated"),
                List.of(
                        WidgetTreeValidator.DUPLICATE_WIDGET_ID,
                        WidgetTreeValidator.UNKNOWN_WIDGET_TYPE,
                        WidgetTreeValidator.MISSING_PROPERTY,
                        WidgetTreeValidator.UNKNOWN_PROPERTY,
                        WidgetTreeValidator.PROPERTY_KIND,
                        WidgetTreeValidator.PROPERTY_CONSTRAINT,
                        WidgetTreeValidator.PROPERTY_DEPENDENCY,
                        WidgetTreeValidator.PROPERTY_CONFLICT,
                        WidgetTreeValidator.PROPERTY_OVERRIDE,
                        WidgetTreeValidator.PROPERTY_UNIQUENESS,
                        WidgetTreeValidator.MISSING_SLOT,
                        WidgetTreeValidator.UNKNOWN_SLOT,
                        WidgetTreeValidator.SLOT_KIND,
                        WidgetTreeValidator.SLOT_CARDINALITY,
                        WidgetTreeValidator.SLOT_NULL,
                        WidgetTreeValidator.SLOT_ACCEPTANCE,
                        WidgetTreeValidator.POSITIONAL_GAP,
                        WidgetTreeValidator.DEPTH_LIMIT,
                        WidgetTreeValidator.NODE_LIMIT,
                        WidgetTreeValidator.PROPERTY_LIMIT,
                        WidgetTreeValidator.SLOT_LIMIT,
                        WidgetTreeValidator.ISSUES_TRUNCATED));
    }

    @Test
    void acceptsTheGoldenExampleShapeAgainstTheRealBuiltInCatalog() {
        WidgetNode title = node("title", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Hello from NetBeans")), Map.of());
        WidgetNode appBar = node("appBar", "flutter.material.AppBar", Map.of(), Map.of(
                slotName("title"), WidgetSlot.SingleSlot.of(title)));
        WidgetNode buttonText = node("buttonText", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Continue")), Map.of());
        WidgetNode button = node("button", "flutter.material.ElevatedButton", Map.of(
                name("onPressed"), new PropertyValue.CallbackValue("onContinue")), Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(buttonText)));
        WidgetNode center = node("center", "flutter.widgets.Center", Map.of(), Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(button)));
        WidgetNode root = node("root", "flutter.material.Scaffold", Map.of(), Map.of(
                slotName("appBar"), WidgetSlot.SingleSlot.of(appBar),
                slotName("body"), WidgetSlot.SingleSlot.of(center)));

        ValidationResult result = validator().validate(
                document(root), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.valid(), () -> "Issues were: " + result.issues());
    }

    @Test
    void flexBaselineRequiresAnExactTextBaselineProperty() {
        Map<PropertyName, PropertyValue> properties = Map.of(
                name("crossAxisAlignment"),
                new PropertyValue.EnumValue("CrossAxisAlignment", "baseline"));
        WidgetNode invalid = node(
                "column", "flutter.widgets.Column", properties, Map.of());

        ValidationIssue issue = onlyIssue(
                validator().validate(document(invalid), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);

        assertEquals("/root/properties/textBaseline", issue.path());
        assertTrue(issue.message().contains("CrossAxisAlignment.baseline"));

        Map<PropertyName, PropertyValue> validProperties = new LinkedHashMap<>(properties);
        validProperties.put(name("textBaseline"),
                new PropertyValue.EnumValue("TextBaseline", "alphabetic"));
        ValidationResult valid = validator().validate(
                document(node("column", "flutter.widgets.Column", validProperties, Map.of())),
                BuiltInWidgetCatalog.getDefault());
        assertTrue(valid.valid(), () -> "Issues were: " + valid.issues());
    }

    @Test
    void appBarShapeMembersRequireCompatibleClosedShapeKind() {
        WidgetNode missingKind = node(
                "appBar", "flutter.material.AppBar",
                Map.of(name("shapeSideWidth"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of());
        ValidationIssue dependency = onlyIssue(
                validator().validate(
                        document(missingKind), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);
        assertEquals("/root/properties/shapeSideWidth", dependency.path());

        WidgetNode incompatibleRadius = node(
                "circle", "flutter.material.AppBar",
                Map.of(
                        name("shapeKind"), new PropertyValue.StringValue("circle"),
                        name("shapeRadiusTopLeft"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of());
        ValidationIssue conflict = onlyIssue(
                validator().validate(
                        document(incompatibleRadius), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT);
        assertEquals("/root/properties/shapeRadiusTopLeft", conflict.path());

        WidgetNode valid = node(
                "rounded", "flutter.material.AppBar",
                Map.of(
                        name("shapeKind"),
                        new PropertyValue.StringValue("roundedRectangle"),
                        name("shapeRadiusTopLeft"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of());
        ValidationResult validResult = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(validResult.valid(), () -> "Issues were: " + validResult.issues());
    }

    @Test
    void appBarTextStyleRelationshipsRemainIndependentAndTyped() {
        WidgetNode conflict = node(
                "appBar", "flutter.material.AppBar",
                Map.of(
                        name("toolbarTextStyleColor"),
                        new PropertyValue.ColorValue(0xFF000000L),
                        name("toolbarTextStyleForeground"),
                        PropertyValue.PaintValue.defaults(
                                new ColorSource.Literal(0xFFFFFFFFL))),
                Map.of());
        ValidationIssue conflictIssue = onlyIssue(
                validator().validate(
                        document(conflict), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT);
        assertEquals("/root/properties/toolbarTextStyleForeground", conflictIssue.path());

        WidgetNode missingFamily = node(
                "package", "flutter.material.AppBar",
                Map.of(name("titleTextStylePackage"),
                        new PropertyValue.StringValue("brand_fonts")),
                Map.of());
        ValidationIssue dependency = onlyIssue(
                validator().validate(
                        document(missingFamily), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);
        assertEquals("/root/properties/titleTextStylePackage", dependency.path());
    }

    @Test
    void appBarBottomRequiresPreferredSizeWidgetTrait() {
        WidgetNode text = node(
                "text", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Not preferred")),
                Map.of());
        WidgetNode appBar = node(
                "appBar", "flutter.material.AppBar", Map.of(),
                Map.of(slotName("bottom"), WidgetSlot.SingleSlot.of(text)));

        ValidationIssue issue = onlyIssue(
                validator().validate(document(appBar), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.SLOT_ACCEPTANCE);
        assertEquals("/root/slots/bottom/child", issue.path());
    }

    @Test
    void textSemanticsIdentifiersAreUniqueAcrossTheWholeTree() {
        WidgetNode first = node("first", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("First"),
                name("semanticsIdentifier"), new PropertyValue.StringValue("shared-title")), Map.of());
        WidgetNode second = node("second", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Second"),
                name("semanticsIdentifier"), new PropertyValue.StringValue("shared-title")), Map.of());
        WidgetNode root = node("column", "flutter.widgets.Column", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(first, second))));

        ValidationIssue issue = onlyIssue(
                validator().validate(document(root), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_UNIQUENESS);

        assertEquals(
                "/root/slots/children/children/1/properties/semanticsIdentifier",
                issue.path());
        assertTrue(issue.message().contains(
                "/root/slots/children/children/0/properties/semanticsIdentifier"));
    }

    @Test
    void textLocaleSubtagsUseFlutterCasingAndShapeRules() {
        WidgetNode invalid = node("text", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Hello"),
                name("localeLanguageCode"), new PropertyValue.StringValue("EN"),
                name("localeScriptCode"), new PropertyValue.StringValue("cyrl"),
                name("localeCountryCode"), new PropertyValue.StringValue("ua")), Map.of());

        ValidationResult result = validator().validate(
                document(invalid), BuiltInWidgetCatalog.getDefault());

        assertEquals(3, result.issues().stream()
                .filter(issue -> issue.code().equals(WidgetTreeValidator.PROPERTY_CONSTRAINT))
                .count());

        WidgetNode valid = node("text", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Hello"),
                name("localeLanguageCode"), new PropertyValue.StringValue("uk"),
                name("localeScriptCode"), new PropertyValue.StringValue("Cyrl"),
                name("localeCountryCode"), new PropertyValue.StringValue("UA")), Map.of());
        ValidationResult accepted = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(accepted.valid(), () -> "Issues were: " + accepted.issues());
    }

    @Test
    void textFontPackagesRequireAConfiguredFamilyOrFallbackList() {
        WidgetNode invalid = node("text", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Hello"),
                name("stylePackage"), new PropertyValue.StringValue("brand_fonts"),
                name("strutPackage"), new PropertyValue.StringValue("brand_fonts")), Map.of());

        ValidationResult result = validator().validate(
                document(invalid), BuiltInWidgetCatalog.getDefault());

        assertEquals(List.of(
                "/root/properties/strutPackage",
                "/root/properties/stylePackage"),
                result.issues().stream()
                        .filter(issue -> issue.code().equals(
                                WidgetTreeValidator.PROPERTY_DEPENDENCY))
                        .map(ValidationIssue::path)
                        .sorted()
                        .toList());

        WidgetNode valid = node("text", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Hello"),
                name("stylePackage"), new PropertyValue.StringValue("brand_fonts"),
                name("styleFontFamily"), new PropertyValue.StringValue("Brand Sans"),
                name("strutPackage"), new PropertyValue.StringValue("brand_fonts"),
                name("strutFontFamilyFallback"), new PropertyValue.StringValue("Noto Sans")),
                Map.of());
        ValidationResult accepted = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(accepted.valid(), () -> "Issues were: " + accepted.issues());
    }

    @Test
    void textStyleColorAndPaintPairsAreMutuallyExclusive() {
        WidgetNode invalid = node("text", "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue("Hello"),
                name("styleColor"), new PropertyValue.ColorValue(0xFF112233L),
                name("styleForeground"), PropertyValue.PaintValue.defaults(
                        new ColorSource.Literal(0xFF445566L)),
                name("styleBackgroundColor"), new PropertyValue.ColorValue(0xFF778899L),
                name("styleBackground"), PropertyValue.PaintValue.defaults(
                        new ColorSource.Literal(0xFFAABBCCL))), Map.of());

        ValidationResult result = validator().validate(
                document(invalid), BuiltInWidgetCatalog.getDefault());

        assertEquals(List.of(
                "/root/properties/styleBackground",
                "/root/properties/styleForeground"),
                result.issues().stream()
                        .filter(issue -> issue.code().equals(WidgetTreeValidator.PROPERTY_CONFLICT))
                        .map(ValidationIssue::path)
                        .sorted()
                        .toList());
        assertTrue(result.issues().stream()
                .filter(issue -> issue.code().equals(WidgetTreeValidator.PROPERTY_CONFLICT))
                .allMatch(issue -> issue.message().contains("mutually exclusive")));
    }

    @Test
    void iconWeightAxisWarnsThatItOverridesFontWeightWithoutBlockingGeneration() {
        WidgetNode icon = node("icon", "flutter.widgets.Icon", Map.of(
                name("icon"), new PropertyValue.IconDataValue(
                        Optional.of(0xE5F9), Optional.of("MaterialIcons"),
                        Optional.empty(), false, List.of()),
                name("weight"), new PropertyValue.DoubleValue(BigDecimal.valueOf(600)),
                name("fontWeight"), new PropertyValue.EnumValue("FontWeight", "w700")),
                Map.of());

        ValidationResult result = validator().validate(
                document(icon), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.valid(), () -> "Warnings must not block generation: " + result.issues());
        assertEquals(1, result.warnings().size());
        ValidationIssue warning = result.warnings().getFirst();
        assertEquals(WidgetTreeValidator.PROPERTY_OVERRIDE, warning.code());
        assertEquals(ValidationSeverity.WARNING, warning.severity());
        assertEquals("/root/properties/fontWeight", warning.path());
        assertTrue(warning.message().contains("overrides 'fontWeight'"));
    }

    @Test
    void acceptsACompleteCatalogValidTree() {
        WidgetDefinition text = definition("test.Text", List.of(
                property("data", 0, true,
                        new PropertyValueConstraint.StringLength(1, 20))), List.of(), Set.of());
        WidgetDefinition root = definition("test.Root", List.of(), List.of(
                slot("child", 0, true, SlotCardinality.SINGLE, 1, 1,
                        new SlotAcceptance.ExactTypes(List.of(text.typeId())))), Set.of());
        WidgetNode child = node("child", "test.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Hello")), Map.of());
        WidgetNode tree = node("root", "test.Root", Map.of(), Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(child)));

        ValidationResult result = validator().validate(document(tree), catalog(root, text));

        assertTrue(result.valid());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void doesNotApplyCreationDefaultsWhileValidatingLoadedDocuments() {
        WidgetNode textWithoutRequiredData = node(
                "text", "flutter.widgets.Text", Map.of(), Map.of());

        ValidationResult result = validator().validate(
                document(textWithoutRequiredData), BuiltInWidgetCatalog.getDefault());

        ValidationIssue issue = onlyIssue(result, WidgetTreeValidator.MISSING_PROPERTY);
        assertEquals("/root/properties/data", issue.path());
        assertTrue(textWithoutRequiredData.properties().isEmpty());
    }

    @Test
    void aspectRatioRequiresAnExplicitFinitePositiveDoubleInLoadedDocuments() {
        WidgetNode omitted = node(
                "omitted", "flutter.widgets.AspectRatio", Map.of(), Map.of());
        ValidationIssue missing = onlyIssue(
                validator().validate(
                        document(omitted), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.MISSING_PROPERTY);
        assertEquals("/root/properties/aspectRatio", missing.path());

        for (BigDecimal rejected : List.of(BigDecimal.ZERO, BigDecimal.ONE.negate())) {
            WidgetNode invalid = node(
                    "ratio-" + rejected,
                    "flutter.widgets.AspectRatio",
                    Map.of(name("aspectRatio"),
                            new PropertyValue.DoubleValue(rejected)),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/aspectRatio", issue.path());
            assertTrue(issue.message().contains("double range"));
        }

        WidgetNode child = node(
                "child", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Inside")),
                Map.of());
        WidgetNode valid = node(
                "valid", "flutter.widgets.AspectRatio",
                Map.of(name("aspectRatio"),
                        new PropertyValue.DoubleValue(new BigDecimal("1.5"))),
                Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
        ValidationResult result = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.valid(), () -> "Issues were: " + result.issues());
    }

    @Test
    void distinguishesOmittedRequiredSlotFromPresentExplicitNull() {
        Map<PropertyName, PropertyValue> properties = Map.of(
                name("enabled"), new PropertyValue.BooleanValue(false));
        WidgetNode omitted = node(
                "omitted", "flutter.material.ElevatedButton", properties, Map.of());
        WidgetNode explicitNull = node(
                "explicit", "flutter.material.ElevatedButton", properties,
                Map.of(slotName("child"), WidgetSlot.SingleSlot.empty()));

        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        ValidationResult explicitNullResult = validator().validate(
                document(explicitNull), BuiltInWidgetCatalog.getDefault());

        assertEquals(List.of(WidgetTreeValidator.MISSING_SLOT), codes(omittedResult));
        assertEquals("/root/slots/child", omittedResult.issues().getFirst().path());
        assertTrue(explicitNullResult.valid(), () -> "Issues were: " + explicitNullResult.issues());
    }

    @Test
    void rejectsPresentOptionalPositionalArgumentAfterAnOmittedOne() {
        PropertyDefinition first = new PropertyDefinition(
                name("label"),
                DartParameter.positional(0, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)),
                Optional.empty());
        SlotDefinition second = new SlotDefinition(
                slotName("child"),
                DartParameter.positional(1, false),
                SlotCardinality.SINGLE,
                0,
                1,
                new SlotAcceptance.AnyWidget());
        WidgetDefinition root = definition(
                "test.Root", List.of(first), List.of(second), Set.of());
        WidgetNode node = node(
                "root", "test.Root", Map.of(),
                Map.of(slotName("child"), WidgetSlot.SingleSlot.empty()));

        ValidationResult result = validator().validate(document(node), catalog(root));

        ValidationIssue issue = onlyIssue(result, WidgetTreeValidator.POSITIONAL_GAP);
        assertEquals("/root/slots/child", issue.path());
        assertTrue(issue.message().contains("'label' at order 0"));
        assertTrue(issue.message().contains("'child' at order 1"));
    }

    @Test
    void reportsDuplicateIdsWithTheFirstAndCurrentPreorderPaths() {
        WidgetDefinition leaf = definition("test.Leaf", List.of(), List.of(), Set.of());
        WidgetDefinition root = definition("test.Root", List.of(), List.of(
                slot("children", 0, false, SlotCardinality.LIST, 0, 10,
                        new SlotAcceptance.AnyWidget())), Set.of());
        StableId duplicate = id("duplicate");
        WidgetNode first = node(duplicate, "test.Leaf", Map.of(), Map.of());
        WidgetNode second = node(duplicate, "test.Leaf", Map.of(), Map.of());
        WidgetNode tree = node("root", "test.Root", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(first, second))));

        ValidationResult result = validator().validate(document(tree), catalog(root, leaf));

        ValidationIssue issue = onlyIssue(result, WidgetTreeValidator.DUPLICATE_WIDGET_ID);
        assertEquals("/root/slots/children/children/1", issue.path());
        assertEquals(Optional.of(duplicate), issue.widgetId());
        assertTrue(issue.message().contains("/root/slots/children/children/0"));
        assertTrue(issue.message().contains("/root/slots/children/children/1"));
    }

    @Test
    void traversesUnknownWidgetSlotsAndStillFindsDescendantProblems() {
        WidgetDefinition known = definition("test.Known", List.of(), List.of(), Set.of());
        StableId duplicate = id("same");
        WidgetNode child = node(duplicate, "test.Known", Map.of(), Map.of());
        WidgetNode tree = node(duplicate, "test.Unknown", Map.of(), Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(child)));

        ValidationResult result = validator().validate(document(tree), catalog(known));

        assertEquals(List.of(
                WidgetTreeValidator.UNKNOWN_WIDGET_TYPE,
                WidgetTreeValidator.DUPLICATE_WIDGET_ID), codes(result));
        assertEquals("/root/slots/child/child", result.issues().get(1).path());
    }

    @Test
    void reportsMissingUnknownAndWrongKindPropertiesDeterministically() {
        WidgetDefinition root = definition("test.Root", List.of(
                property("mandatory", 0, true,
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)),
                property("count", 1, false,
                        new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, BigInteger.TEN))),
                List.of(), Set.of());
        Map<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("zUnknown"), new PropertyValue.BooleanValue(true));
        properties.put(name("aUnknown"), new PropertyValue.BooleanValue(false));
        properties.put(name("count"), new PropertyValue.StringValue("one"));

        ValidationResult result = validator().validate(
                document(node("root", "test.Root", properties, Map.of())), catalog(root));

        assertEquals(List.of(
                WidgetTreeValidator.MISSING_PROPERTY,
                WidgetTreeValidator.PROPERTY_KIND,
                WidgetTreeValidator.UNKNOWN_PROPERTY,
                WidgetTreeValidator.UNKNOWN_PROPERTY), codes(result));
        assertEquals("/root/properties/aUnknown", result.issues().get(2).path());
        assertEquals("/root/properties/zUnknown", result.issues().get(3).path());
    }

    @Test
    void appliesEveryCatalogPropertyConstraintWithoutCoercion() {
        WidgetDefinition root = definition("test.Root", List.of(
                property("text", 0, false,
                        new PropertyValueConstraint.StringLength(2, 3)),
                property("integer", 1, false,
                        new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, BigInteger.TEN)),
                property("decimal", 2, false,
                        new PropertyValueConstraint.DoubleRange(
                                BigDecimal.ZERO, false, BigDecimal.ONE, true)),
                property("mode", 3, false,
                        new PropertyValueConstraint.EnumValues(
                                new DartSymbolReference("package:flutter/widgets.dart", "Axis"),
                                List.of("horizontal"))),
                property("padding", 4, false,
                        new PropertyValueConstraint.EdgeInsetsValues(true))), List.of(), Set.of());
        Map<PropertyName, PropertyValue> properties = Map.of(
                name("text"), new PropertyValue.StringValue("x"),
                name("integer"), new PropertyValue.IntegerValue(BigInteger.valueOf(11)),
                name("decimal"), new PropertyValue.DoubleValue(BigDecimal.ZERO),
                name("mode"), new PropertyValue.EnumValue("OtherAxis", "vertical"),
                name("padding"), new PropertyValue.EdgeInsetsValue(
                        BigDecimal.ONE.negate(), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO));

        ValidationResult result = validator().validate(
                document(node("root", "test.Root", properties, Map.of())), catalog(root));

        assertEquals(5, result.issues().size());
        assertTrue(result.issues().stream()
                .allMatch(issue -> issue.code().equals(WidgetTreeValidator.PROPERTY_CONSTRAINT)));
        assertTrue(result.issues().stream().anyMatch(issue -> issue.message().contains("string length")));
        assertTrue(result.issues().stream().anyMatch(issue -> issue.message().contains("Axis[horizontal]")));
        assertTrue(result.issues().stream().anyMatch(issue -> issue.message().contains("non-negative")));
    }

    @Test
    void reportsMissingAndUnknownSlotsInCatalogOrderThenNameOrder() {
        WidgetDefinition root = definition("test.Root", List.of(), List.of(
                slot("mandatory", 0, true, SlotCardinality.SINGLE, 0, 1,
                        new SlotAcceptance.AnyWidget())), Set.of());
        Map<SlotName, WidgetSlot> slots = new LinkedHashMap<>();
        slots.put(slotName("zUnknown"), WidgetSlot.SingleSlot.empty());
        slots.put(slotName("aUnknown"), new WidgetSlot.ListSlot(List.of()));

        ValidationResult result = validator().validate(
                document(node("root", "test.Root", Map.of(), slots)), catalog(root));

        assertEquals(List.of(
                WidgetTreeValidator.MISSING_SLOT,
                WidgetTreeValidator.UNKNOWN_SLOT,
                WidgetTreeValidator.UNKNOWN_SLOT), codes(result));
        assertEquals("/root/slots/aUnknown", result.issues().get(1).path());
        assertEquals("/root/slots/zUnknown", result.issues().get(2).path());
    }

    @Test
    void distinguishesSlotKindCardinalityAndExplicitNullFailures() {
        WidgetDefinition root = definition("test.Root", List.of(), List.of(
                slot("kind", 0, false, SlotCardinality.SINGLE, 0, 1,
                        new SlotAcceptance.AnyWidget()),
                slot("requiredChild", 1, false, SlotCardinality.SINGLE, 1, 1,
                        new SlotAcceptance.AnyWidget()),
                slot("list", 2, false, SlotCardinality.LIST, 2, 3,
                        new SlotAcceptance.AnyWidget())), Set.of());
        WidgetNode leaf = node("leaf", "test.Leaf", Map.of(), Map.of());
        Map<SlotName, WidgetSlot> slots = Map.of(
                slotName("kind"), new WidgetSlot.ListSlot(List.of()),
                slotName("requiredChild"), WidgetSlot.SingleSlot.empty(),
                slotName("list"), new WidgetSlot.ListSlot(List.of(leaf)));

        ValidationResult result = validator().validate(
                document(node("root", "test.Root", Map.of(), slots)), catalog(root));

        assertTrue(codes(result).contains(WidgetTreeValidator.SLOT_KIND));
        assertTrue(codes(result).contains(WidgetTreeValidator.SLOT_NULL));
        assertTrue(codes(result).contains(WidgetTreeValidator.SLOT_CARDINALITY));
        assertTrue(codes(result).contains(WidgetTreeValidator.UNKNOWN_WIDGET_TYPE));
    }

    @Test
    void validatesChildAcceptanceAtTheChildPreorderPosition() {
        WidgetDefinition accepted = definition(
                "test.Accepted", List.of(), List.of(), Set.of("preferred-size"));
        WidgetDefinition rejected = definition("test.Rejected", List.of(), List.of(), Set.of());
        WidgetDefinition root = definition("test.Root", List.of(), List.of(
                slot("child", 0, false, SlotCardinality.SINGLE, 0, 1,
                        new SlotAcceptance.HasTrait("preferred-size"))), Set.of());
        WidgetNode child = node("child", "test.Rejected", Map.of(), Map.of());
        WidgetNode tree = node("root", "test.Root", Map.of(), Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(child)));

        ValidationResult result = validator().validate(
                document(tree), catalog(root, accepted, rejected));

        ValidationIssue issue = onlyIssue(result, WidgetTreeValidator.SLOT_ACCEPTANCE);
        assertEquals("/root/slots/child/child", issue.path());
        assertEquals(Optional.of(child.id()), issue.widgetId());
    }

    @Test
    void enforcesDepthLimitWithoutRecursion() {
        WidgetDefinition branch = definition("test.Branch", List.of(), List.of(
                slot("children", 0, false, SlotCardinality.LIST, 0, 10,
                        new SlotAcceptance.AnyWidget())), Set.of());
        WidgetNode depthThree = node("deep", "test.Branch", Map.of(), Map.of());
        WidgetNode depthTwo = node("middle", "test.Branch", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(depthThree))));
        WidgetNode root = node("root", "test.Branch", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(depthTwo))));

        ValidationLimits limits = new ValidationLimits(2, 10, 256, 128, 100);
        ValidationResult result = new WidgetTreeValidator(limits)
                .validate(document(root), catalog(branch));

        assertEquals(List.of(WidgetTreeValidator.DEPTH_LIMIT), codes(result));
        assertEquals("/root/slots/children/children/0/slots/children/children/0",
                result.issues().get(0).path());
    }

    @Test
    void boundsThePendingFrontierAcrossMultipleLargeListSlots() {
        WidgetDefinition leaf = definition("test.Leaf", List.of(), List.of(), Set.of());
        WidgetDefinition rootDefinition = definition("test.Root", List.of(), List.of(
                slot("first", 0, false, SlotCardinality.LIST, 0, 10_000,
                        new SlotAcceptance.AnyWidget()),
                slot("second", 1, false, SlotCardinality.LIST, 0, 10_000,
                        new SlotAcceptance.AnyWidget()),
                slot("third", 2, false, SlotCardinality.LIST, 0, 10_000,
                        new SlotAcceptance.AnyWidget())), Set.of());
        Map<SlotName, WidgetSlot> slots = new LinkedHashMap<>();
        slots.put(slotName("third"), new WidgetSlot.ListSlot(leaves("third", 2_000)));
        slots.put(slotName("second"), new WidgetSlot.ListSlot(leaves("second", 2_000)));
        slots.put(slotName("first"), new WidgetSlot.ListSlot(leaves("first", 2_000)));
        WidgetNode root = node("root", "test.Root", Map.of(), slots);
        ValidationLimits limits = new ValidationLimits(256, 25, 256, 128, 100);
        WidgetTreeValidator validator = new WidgetTreeValidator(limits);

        ValidationResult firstRun = validator.validate(
                document(root), catalog(rootDefinition, leaf));
        ValidationResult secondRun = validator.validate(
                document(root), catalog(rootDefinition, leaf));

        ValidationIssue issue = onlyIssue(firstRun, WidgetTreeValidator.NODE_LIMIT);
        assertEquals("/root/slots/first/children/24", issue.path());
        assertEquals(firstRun.issues(), secondRun.issues());
    }

    @Test
    void reportsPerWidgetCollectionLimits() {
        WidgetDefinition root = definition("test.Root", List.of(), List.of(), Set.of());
        Map<PropertyName, PropertyValue> properties = Map.of(
                name("a"), new PropertyValue.StringValue("a"),
                name("b"), new PropertyValue.StringValue("b"));
        Map<SlotName, WidgetSlot> slots = Map.of(
                slotName("a"), WidgetSlot.SingleSlot.empty(),
                slotName("b"), WidgetSlot.SingleSlot.empty());
        ValidationLimits limits = new ValidationLimits(256, 10_000, 1, 1, 100);

        ValidationResult result = new WidgetTreeValidator(limits).validate(
                document(node("root", "test.Root", properties, slots)), catalog(root));

        assertEquals(1, result.issues().stream()
                .filter(issue -> issue.code().equals(WidgetTreeValidator.PROPERTY_LIMIT)).count());
        assertEquals(1, result.issues().stream()
                .filter(issue -> issue.code().equals(WidgetTreeValidator.SLOT_LIMIT)).count());
        assertEquals(1, result.issues().stream()
                .filter(issue -> issue.code().equals(WidgetTreeValidator.UNKNOWN_PROPERTY)).count());
        assertEquals(1, result.issues().stream()
                .filter(issue -> issue.code().equals(WidgetTreeValidator.UNKNOWN_SLOT)).count());
    }

    @Test
    void capsIssuesWithOneTerminalTruncationDiagnostic() {
        WidgetDefinition root = definition("test.Root", List.of(), List.of(), Set.of());
        Map<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        for (int index = 0; index < 10; index++) {
            properties.put(name("unknown" + index), new PropertyValue.BooleanValue(true));
        }
        ValidationLimits limits = new ValidationLimits(256, 10_000, 256, 128, 3);

        ValidationResult result = new WidgetTreeValidator(limits).validate(
                document(node("root", "test.Root", properties, Map.of())), catalog(root));

        assertEquals(3, result.issues().size());
        assertEquals(WidgetTreeValidator.ISSUES_TRUNCATED, result.issues().get(2).code());
        assertFalse(result.valid());
    }

    @Test
    void visitsChildrenInDeterministicSlotAndListOrder() {
        WidgetNode a0 = node("a0", "unknown.A0", Map.of(), Map.of());
        WidgetNode a1 = node("a1", "unknown.A1", Map.of(), Map.of());
        WidgetNode b = node("b", "unknown.B", Map.of(), Map.of());
        Map<SlotName, WidgetSlot> slots = new LinkedHashMap<>();
        slots.put(slotName("b"), WidgetSlot.SingleSlot.of(b));
        slots.put(slotName("a"), new WidgetSlot.ListSlot(List.of(a0, a1)));
        WidgetNode root = node("root", "unknown.Root", Map.of(), slots);

        ValidationResult result = validator().validate(document(root), WidgetCatalog.strict(List.of()));

        assertEquals(List.of(
                "/root",
                "/root/slots/a/children/0",
                "/root/slots/a/children/1",
                "/root/slots/b/child"),
                result.issues().stream().map(ValidationIssue::path).toList());
    }

    private static WidgetTreeValidator validator() {
        return new WidgetTreeValidator();
    }

    private static ValidationIssue onlyIssue(ValidationResult result, String code) {
        List<ValidationIssue> matching = result.issues().stream()
                .filter(issue -> issue.code().equals(code))
                .toList();
        assertEquals(1, matching.size(), () -> "Issues were: " + result.issues());
        return matching.getFirst();
    }

    private static List<String> codes(ValidationResult result) {
        return result.issues().stream().map(ValidationIssue::code).toList();
    }

    private static DesignerDocument document(WidgetNode root) {
        String emptyHash = "0".repeat(64);
        return new DesignerDocument(
                id("document"),
                new DartSourceDescriptor(
                        "page.dart",
                        "Page",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(
                                new ManagedRegion(emptyHash),
                                new ManagedRegion(emptyHash))),
                root);
    }

    private static WidgetCatalog catalog(WidgetDefinition... definitions) {
        return WidgetCatalog.strict(List.of(definitions));
    }

    private static WidgetDefinition definition(
            String type,
            List<PropertyDefinition> properties,
            List<SlotDefinition> slots,
            Set<String> traits) {
        return new WidgetDefinition(
                new WidgetTypeId(type),
                "TestWidget",
                Optional.empty(),
                false,
                "package:flutter/widgets.dart",
                List.of("package:flutter/widgets.dart"),
                traits,
                new PaletteMetadata("test", 0, Math.abs(type.hashCode()), type),
                properties,
                slots);
    }

    private static PropertyDefinition property(
            String name,
            int order,
            boolean required,
            PropertyValueConstraint constraint) {
        return new PropertyDefinition(
                new PropertyName(name),
                DartParameter.named(order, required),
                List.of(constraint),
                Optional.empty());
    }

    private static SlotDefinition slot(
            String name,
            int order,
            boolean required,
            SlotCardinality cardinality,
            int minimum,
            int maximum,
            SlotAcceptance acceptance) {
        return new SlotDefinition(
                new SlotName(name),
                DartParameter.named(order, required),
                cardinality,
                minimum,
                maximum,
                acceptance);
    }

    private static WidgetNode node(
            String idSeed,
            String type,
            Map<PropertyName, PropertyValue> properties,
            Map<SlotName, WidgetSlot> slots) {
        return node(id(idSeed), type, properties, slots);
    }

    private static WidgetNode node(
            StableId id,
            String type,
            Map<PropertyName, PropertyValue> properties,
            Map<SlotName, WidgetSlot> slots) {
        return new WidgetNode(id, new WidgetTypeId(type), properties, slots);
    }

    private static List<WidgetNode> leaves(String prefix, int count) {
        List<WidgetNode> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(node(prefix + index, "test.Leaf", Map.of(), Map.of()));
        }
        return List.copyOf(result);
    }

    private static StableId id(String seed) {
        return new StableId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }

    private static SlotName slotName(String value) {
        return new SlotName(value);
    }
}
