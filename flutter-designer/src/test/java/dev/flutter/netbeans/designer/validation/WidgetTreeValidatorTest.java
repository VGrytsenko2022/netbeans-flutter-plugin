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
                "designer.widget.placement",
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
                        WidgetTreeValidator.WIDGET_PLACEMENT,
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
    void acceptsExpandedOnlyAsDirectRowOrColumnChildIncludingZeroAndPortableMaxFlex() {
        WidgetNode rowExpanded = expanded(
                "rowExpanded",
                Map.of(name("flex"), new PropertyValue.IntegerValue(BigInteger.ZERO)),
                text("rowText"));
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(rowExpanded))));
        ValidationResult rowResult = validator().validate(
                document(row), BuiltInWidgetCatalog.getDefault());
        assertTrue(rowResult.valid(), () -> "Issues were: " + rowResult.issues());

        WidgetNode columnExpanded = expanded(
                "columnExpanded",
                Map.of(name("flex"), new PropertyValue.IntegerValue(
                        new BigInteger("9007199254740991"))),
                text("columnText"));
        WidgetNode column = node("column", "flutter.widgets.Column", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(columnExpanded))));
        ValidationResult columnResult = validator().validate(
                document(column), BuiltInWidgetCatalog.getDefault());
        assertTrue(columnResult.valid(), () -> "Issues were: " + columnResult.issues());
    }

    @Test
    void rejectsExpandedAsRootOrUnderAnyNonFlexSlotWithConcretePlacementIssue() {
        WidgetNode rootExpanded = expanded("expanded", Map.of(), text("rootText"));
        ValidationIssue rootIssue = onlyIssue(
                validator().validate(
                        document(rootExpanded), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root", rootIssue.path());
        assertTrue(rootIssue.message().contains("cannot be the Designer root"));

        WidgetNode stackExpanded = expanded("stackExpanded", Map.of(), text("stackText"));
        WidgetNode stack = node("stack", "flutter.widgets.Stack", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(stackExpanded))));
        ValidationIssue stackIssue = onlyIssue(
                validator().validate(document(stack), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root/slots/children/children/0", stackIssue.path());
        assertTrue(stackIssue.message().contains("flutter.widgets.Stack.children"));
        assertTrue(stackIssue.message().contains("flutter.widgets.Row.children"));
    }

    @Test
    void rejectsEmptyExpandedChildAndNegativeFlexButAllowsDetachedPrototypeShape() {
        WidgetNode emptyExpanded = node(
                "emptyExpanded",
                "flutter.widgets.Expanded",
                Map.of(name("flex"), new PropertyValue.IntegerValue(BigInteger.ONE.negate())),
                Map.of(slotName("child"), WidgetSlot.SingleSlot.empty()));
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(emptyExpanded))));

        ValidationResult result = validator().validate(
                document(row), BuiltInWidgetCatalog.getDefault());

        assertEquals(List.of(
                        WidgetTreeValidator.PROPERTY_CONSTRAINT,
                        WidgetTreeValidator.SLOT_NULL),
                codes(result));
    }

    @Test
    void acceptsFlexibleOnlyAsDirectRowOrColumnChildWithEveryReviewedValue() {
        WidgetNode rowFlexible = flexible(
                "rowFlexible",
                Map.of(
                        name("flex"), new PropertyValue.IntegerValue(BigInteger.ZERO),
                        name("fit"), new PropertyValue.EnumValue("FlexFit", "loose")),
                text("rowFlexibleText"));
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(rowFlexible))));
        ValidationResult rowResult = validator().validate(
                document(row), BuiltInWidgetCatalog.getDefault());
        assertTrue(rowResult.valid(), () -> "Issues were: " + rowResult.issues());

        WidgetNode columnFlexible = flexible(
                "columnFlexible",
                Map.of(
                        name("flex"), new PropertyValue.IntegerValue(
                                new BigInteger("9007199254740991")),
                        name("fit"), new PropertyValue.EnumValue("FlexFit", "tight")),
                text("columnFlexibleText"));
        WidgetNode column = node("column", "flutter.widgets.Column", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(columnFlexible))));
        ValidationResult columnResult = validator().validate(
                document(column), BuiltInWidgetCatalog.getDefault());
        assertTrue(columnResult.valid(), () -> "Issues were: " + columnResult.issues());
    }

    @Test
    void rejectsFlexibleAtRootInNonFlexParentAndInsideExpanded() {
        WidgetNode rootFlexible = flexible(
                "rootFlexible", Map.of(), text("rootFlexibleText"));
        ValidationIssue rootIssue = onlyIssue(
                validator().validate(
                        document(rootFlexible), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root", rootIssue.path());
        assertTrue(rootIssue.message().contains("flutter.widgets.Flexible"));

        WidgetNode stackFlexible = flexible(
                "stackFlexible", Map.of(), text("stackFlexibleText"));
        WidgetNode stack = node("stack", "flutter.widgets.Stack", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(stackFlexible))));
        ValidationIssue stackIssue = onlyIssue(
                validator().validate(document(stack), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertTrue(stackIssue.message().contains("flutter.widgets.Stack.children"));

        WidgetNode nestedFlexible = flexible(
                "nestedFlexible", Map.of(), text("nestedFlexibleText"));
        WidgetNode expanded = expanded(
                "expanded", Map.of(), nestedFlexible);
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(expanded))));
        ValidationIssue nestedIssue = onlyIssue(
                validator().validate(document(row), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertTrue(nestedIssue.message().contains("flutter.widgets.Expanded.child"));
    }

    @Test
    void rejectsIncompleteFlexibleAndInvalidFlexOrFitValues() {
        WidgetNode incomplete = node(
                "incompleteFlexible",
                "flutter.widgets.Flexible",
                Map.of(
                        name("flex"), new PropertyValue.IntegerValue(
                                BigInteger.ONE.negate()),
                        name("fit"), new PropertyValue.EnumValue("FlexFit", "expand")),
                Map.of(slotName("child"), WidgetSlot.SingleSlot.empty()));
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(incomplete))));

        ValidationResult result = validator().validate(
                document(row), BuiltInWidgetCatalog.getDefault());

        assertEquals(List.of(
                        WidgetTreeValidator.PROPERTY_CONSTRAINT,
                        WidgetTreeValidator.PROPERTY_CONSTRAINT,
                        WidgetTreeValidator.SLOT_NULL),
                codes(result));
    }

    @Test
    void acceptsSpacerOnlyAsDirectRowOrColumnChildWithPositivePortableFlex() {
        WidgetNode omitted = spacer("omittedSpacer", Map.of());
        WidgetNode maximum = spacer("maximumSpacer", Map.of(
                name("flex"), new PropertyValue.IntegerValue(
                        new BigInteger("9007199254740991"))));
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(omitted))));
        WidgetNode column = node("column", "flutter.widgets.Column", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(maximum))));

        assertTrue(validator().validate(document(row), BuiltInWidgetCatalog.getDefault())
                .valid());
        assertTrue(validator().validate(document(column), BuiltInWidgetCatalog.getDefault())
                .valid());
    }

    @Test
    void rejectsSpacerAtRootOutsideFlexOrWithNonPositiveFlex() {
        ValidationIssue rootIssue = onlyIssue(
                validator().validate(document(spacer("rootSpacer", Map.of())),
                        BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertTrue(rootIssue.message().contains("flutter.widgets.Spacer"));

        WidgetNode stack = node("stack", "flutter.widgets.Stack", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(
                        spacer("stackSpacer", Map.of())))));
        ValidationIssue stackIssue = onlyIssue(
                validator().validate(document(stack), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertTrue(stackIssue.message().contains("flutter.widgets.Stack.children"));

        WidgetNode zero = spacer("zeroSpacer", Map.of(
                name("flex"), new PropertyValue.IntegerValue(BigInteger.ZERO)));
        WidgetNode row = node("row", "flutter.widgets.Row", Map.of(), Map.of(
                slotName("children"), new WidgetSlot.ListSlot(List.of(zero))));
        ValidationIssue flexIssue = onlyIssue(
                validator().validate(document(row), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONSTRAINT);
        assertEquals("/root/slots/children/children/0/properties/flex",
                flexIssue.path());
    }

    @Test
    void opacityRequiresAnExplicitDoubleInsideTheInclusiveUnitInterval() {
        WidgetNode omitted = node(
                "omitted", "flutter.widgets.Opacity", Map.of(), Map.of());
        ValidationIssue missing = onlyIssue(
                validator().validate(
                        document(omitted), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.MISSING_PROPERTY);
        assertEquals("/root/properties/opacity", missing.path());

        for (BigDecimal rejected : List.of(
                new BigDecimal("-0.0001"), new BigDecimal("1.0001"))) {
            WidgetNode invalid = node(
                    "opacity-" + rejected,
                    "flutter.widgets.Opacity",
                    Map.of(name("opacity"), new PropertyValue.DoubleValue(rejected)),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/opacity", issue.path());
            assertTrue(issue.message().contains("double range"));
        }

        for (PropertyValue rejected : List.of(
                new PropertyValue.IntegerValue(BigInteger.ZERO),
                new PropertyValue.StringValue("C:/outside/widget.dart"),
                new PropertyValue.AssetValue("../../outside.png"),
                new PropertyValue.DartExpressionValue(
                        "FileImage(File('../../outside.png'))"))) {
            WidgetNode invalid = node(
                    "opacity-kind-" + rejected.kind().wireName(),
                    "flutter.widgets.Opacity",
                    Map.of(name("opacity"), rejected),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/opacity", issue.path());
        }

        WidgetNode child = node(
                "child", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Inside")),
                Map.of());
        for (BigDecimal accepted : List.of(
                BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE)) {
            WidgetNode valid = node(
                    "valid-" + accepted,
                    "flutter.widgets.Opacity",
                    Map.of(
                            name("opacity"), new PropertyValue.DoubleValue(accepted),
                            name("alwaysIncludeSemantics"),
                                    new PropertyValue.BooleanValue(true)),
                    Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> "Issues were: " + result.issues());
        }
    }

    @Test
    void alignAcceptsOptionalPhysicalOrDirectionalAlignmentAndNonNegativeFactors() {
        WidgetNode omitted = node(
                "align-omitted", "flutter.widgets.Align", Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> "Issues were: " + omittedResult.issues());

        WidgetNode child = node(
                "align-child", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Inside")),
                Map.of());
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            WidgetNode valid = node(
                    "align-valid-" + basis.name(),
                    "flutter.widgets.Align",
                    Map.of(
                            name("alignment"),
                                    new PropertyValue.AlignmentGeometryValue(
                                            basis,
                                            new BigDecimal("-0.25"),
                                            new BigDecimal("0.75")),
                            name("widthFactor"),
                                    new PropertyValue.IntegerValue(BigInteger.ZERO),
                            name("heightFactor"),
                                    new PropertyValue.DoubleValue(new BigDecimal("1.5"))),
                    Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> "Issues were: " + result.issues());
        }

        Map<String, PropertyValue> rejectedFactors = Map.of(
                "widthFactor", new PropertyValue.IntegerValue(BigInteger.ONE.negate()),
                "heightFactor", new PropertyValue.DoubleValue(new BigDecimal("-0.001")));
        for (Map.Entry<String, PropertyValue> entry : rejectedFactors.entrySet()) {
            WidgetNode invalid = node(
                    "align-negative-" + entry.getKey(),
                    "flutter.widgets.Align",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        for (Map.Entry<String, PropertyValue> entry : Map.<String, PropertyValue>of(
                "alignment", new PropertyValue.StringValue("Alignment.center"),
                "widthFactor", new PropertyValue.BooleanValue(true),
                "heightFactor", new PropertyValue.StringValue("1.5")).entrySet()) {
            WidgetNode invalid = node(
                    "align-kind-" + entry.getKey(),
                    "flutter.widgets.Align",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }
    }

    @Test
    void fractionallySizedBoxAcceptsAlignmentAndNonNegativeOptionalFactors() {
        WidgetNode omitted = node(
                "fractional-omitted", "flutter.widgets.FractionallySizedBox",
                Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> "Issues were: " + omittedResult.issues());

        WidgetNode child = node(
                "fractional-child", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Inside")),
                Map.of());
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            WidgetNode valid = node(
                    "fractional-valid-" + basis.name(),
                    "flutter.widgets.FractionallySizedBox",
                    Map.of(
                            name("alignment"),
                                    new PropertyValue.AlignmentGeometryValue(
                                            basis,
                                            new BigDecimal("-0.25"),
                                            new BigDecimal("0.75")),
                            name("widthFactor"),
                                    new PropertyValue.IntegerValue(BigInteger.ZERO),
                            name("heightFactor"),
                                    new PropertyValue.DoubleValue(new BigDecimal("1.5"))),
                    Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> "Issues were: " + result.issues());
        }

        Map<String, PropertyValue> rejectedFactors = Map.of(
                "widthFactor", new PropertyValue.IntegerValue(BigInteger.ONE.negate()),
                "heightFactor", new PropertyValue.DoubleValue(new BigDecimal("-0.001")));
        for (Map.Entry<String, PropertyValue> entry : rejectedFactors.entrySet()) {
            WidgetNode invalid = node(
                    "fractional-negative-" + entry.getKey(),
                    "flutter.widgets.FractionallySizedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        for (Map.Entry<String, PropertyValue> entry : Map.<String, PropertyValue>of(
                "alignment", new PropertyValue.StringValue("Alignment.center"),
                "widthFactor", new PropertyValue.BooleanValue(true),
                "heightFactor", new PropertyValue.StringValue("1.5")).entrySet()) {
            WidgetNode invalid = node(
                    "fractional-kind-" + entry.getKey(),
                    "flutter.widgets.FractionallySizedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }
    }

    @Test
    void fittedBoxAcceptsExactFitAlignmentClipAndOptionalChildContract() {
        WidgetNode omitted = node(
                "fitted-omitted", "flutter.widgets.FittedBox", Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(),
                () -> "Issues were: " + omittedResult.issues());

        WidgetNode child = node(
                "fitted-child", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Inside")),
                Map.of());
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            WidgetNode valid = node(
                    "fitted-alignment-" + basis.name(),
                    "flutter.widgets.FittedBox",
                    Map.of(name("alignment"),
                            new PropertyValue.AlignmentGeometryValue(
                                    basis,
                                    new BigDecimal("1.5"),
                                    new BigDecimal("-0.5"))),
                    Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> "Issues were: " + result.issues());
        }

        Map<String, List<String>> accepted = Map.of(
                "fit", List.of(
                        "fill", "contain", "cover", "fitWidth", "fitHeight",
                        "none", "scaleDown"),
                "clipBehavior", List.of(
                        "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer"));
        Map<String, String> enumTypes = Map.of(
                "fit", "BoxFit",
                "clipBehavior", "Clip");
        for (Map.Entry<String, List<String>> entry : accepted.entrySet()) {
            for (String value : entry.getValue()) {
                WidgetNode valid = node(
                        "fitted-enum-" + entry.getKey() + '-' + value,
                        "flutter.widgets.FittedBox",
                        Map.of(name(entry.getKey()), new PropertyValue.EnumValue(
                                enumTypes.get(entry.getKey()), value)),
                        Map.of());
                ValidationResult result = validator().validate(
                        document(valid), BuiltInWidgetCatalog.getDefault());
                assertTrue(result.valid(), () -> "Issues were: " + result.issues());
            }
        }

        Map<String, PropertyValue> rejectedEnums = Map.of(
                "fit", new PropertyValue.EnumValue("BoxFit", "expand"),
                "clipBehavior", new PropertyValue.EnumValue("Clip", "visible"));
        for (Map.Entry<String, PropertyValue> entry : rejectedEnums.entrySet()) {
            WidgetNode invalid = node(
                    "fitted-enum-invalid-" + entry.getKey(),
                    "flutter.widgets.FittedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        WidgetNode wrongEnumType = node(
                "fitted-enum-type", "flutter.widgets.FittedBox",
                Map.of(name("fit"),
                        new PropertyValue.EnumValue("StackFit", "contain")),
                Map.of());
        assertEquals("/root/properties/fit", onlyIssue(
                validator().validate(
                        document(wrongEnumType), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONSTRAINT).path());

        for (Map.Entry<String, PropertyValue> entry : Map.<String, PropertyValue>of(
                "fit", new PropertyValue.StringValue("contain"),
                "alignment", new PropertyValue.StringValue("Alignment.center"),
                "clipBehavior", new PropertyValue.IntegerValue(BigInteger.ZERO))
                .entrySet()) {
            WidgetNode invalid = node(
                    "fitted-kind-" + entry.getKey(),
                    "flutter.widgets.FittedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        WidgetNode wrongSlot = node(
                "fitted-list-child", "flutter.widgets.FittedBox", Map.of(),
                Map.of(slotName("child"), new WidgetSlot.ListSlot(List.of(child))));
        assertEquals("/root/slots/child", onlyIssue(
                validator().validate(
                        document(wrongSlot), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.SLOT_KIND).path());

        WidgetNode unknownProperty = node(
                "fitted-unknown", "flutter.widgets.FittedBox",
                Map.of(name("filterQuality"),
                        new PropertyValue.EnumValue("FilterQuality", "high")),
                Map.of());
        assertEquals("/root/properties/filterQuality", onlyIssue(
                validator().validate(
                        document(unknownProperty), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.UNKNOWN_PROPERTY).path());
    }

    @Test
    void constrainedBoxRequiresCompleteConstraintsAndAcceptsEveryAxisState() {
        record Axis(
                PropertyValue.BoxConstraintBound minimum,
                PropertyValue.BoxConstraintBound maximum) {
        }
        PropertyValue.BoxConstraintBound infinity =
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE;
        java.util.function.Function<Integer, PropertyValue.BoxConstraintBound> finite =
                value -> PropertyValue.BoxConstraintBound.finite(
                        BigDecimal.valueOf(value));
        List<Axis> states = List.of(
                new Axis(finite.apply(0), infinity),
                new Axis(finite.apply(0), finite.apply(100)),
                new Axis(finite.apply(10), infinity),
                new Axis(finite.apply(10), finite.apply(100)),
                new Axis(finite.apply(48), finite.apply(48)),
                new Axis(infinity, infinity));

        for (int index = 0; index < states.size(); index++) {
            Axis width = states.get(index);
            WidgetNode validWidth = node(
                    "constrained-width-" + index,
                    "flutter.widgets.ConstrainedBox",
                    Map.of(name("constraints"),
                            new PropertyValue.BoxConstraintsValue(
                                    width.minimum(), width.maximum(),
                                    finite.apply(0), infinity)),
                    Map.of());
            ValidationResult widthResult = validator().validate(
                    document(validWidth), BuiltInWidgetCatalog.getDefault());
            assertTrue(widthResult.valid(), () -> widthResult.issues().toString());

            Axis height = states.get(index);
            WidgetNode validHeight = node(
                    "constrained-height-" + index,
                    "flutter.widgets.ConstrainedBox",
                    Map.of(name("constraints"),
                            new PropertyValue.BoxConstraintsValue(
                                    finite.apply(0), infinity,
                                    height.minimum(), height.maximum())),
                    Map.of());
            ValidationResult heightResult = validator().validate(
                    document(validHeight), BuiltInWidgetCatalog.getDefault());
            assertTrue(heightResult.valid(), () -> heightResult.issues().toString());
        }

        WidgetNode child = text("constrained-child");
        WidgetNode withChild = node(
                "constrained-with-child", "flutter.widgets.ConstrainedBox",
                Map.of(name("constraints"),
                        new PropertyValue.BoxConstraintsValue(
                                infinity, infinity, infinity, infinity)),
                Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
        ValidationResult childResult = validator().validate(
                document(withChild), BuiltInWidgetCatalog.getDefault());
        assertTrue(childResult.valid(), () -> childResult.issues().toString());

        WidgetNode missing = node(
                "constrained-missing", "flutter.widgets.ConstrainedBox",
                Map.of(), Map.of());
        assertEquals("/root/properties/constraints", onlyIssue(
                validator().validate(
                        document(missing), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.MISSING_PROPERTY).path());

        WidgetNode wrongKind = node(
                "constrained-kind", "flutter.widgets.ConstrainedBox",
                Map.of(name("constraints"),
                        new PropertyValue.StringValue("BoxConstraints()")),
                Map.of());
        assertEquals("/root/properties/constraints", onlyIssue(
                validator().validate(
                        document(wrongKind), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_KIND).path());

        PropertyValue.BoxConstraintsValue neutral =
                new PropertyValue.BoxConstraintsValue(
                        BigDecimal.ZERO, Optional.empty(),
                        BigDecimal.ZERO, Optional.empty());
        WidgetNode unknownProperty = node(
                "constrained-unknown", "flutter.widgets.ConstrainedBox",
                Map.of(
                        name("constraints"), neutral,
                        name("alignment"), new PropertyValue.StringValue("center")),
                Map.of());
        assertEquals("/root/properties/alignment", onlyIssue(
                validator().validate(
                        document(unknownProperty), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.UNKNOWN_PROPERTY).path());

        WidgetNode wrongSlot = node(
                "constrained-list-child", "flutter.widgets.ConstrainedBox",
                Map.of(name("constraints"), neutral),
                Map.of(slotName("child"),
                        new WidgetSlot.ListSlot(List.of(child))));
        assertEquals("/root/slots/child", onlyIssue(
                validator().validate(
                        document(wrongSlot), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.SLOT_KIND).path());

        WidgetNode expandedChild = expanded(
                "constrained-expanded", Map.of(), text("expanded-text"));
        WidgetNode invalidPlacement = node(
                "constrained-expanded-parent", "flutter.widgets.ConstrainedBox",
                Map.of(name("constraints"), neutral),
                Map.of(slotName("child"),
                        WidgetSlot.SingleSlot.of(expandedChild)));
        ValidationIssue placement = onlyIssue(
                validator().validate(
                        document(invalidPlacement), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root/slots/child/child", placement.path());
        assertTrue(placement.message().contains(
                "flutter.widgets.ConstrainedBox.child"));
    }

    @Test
    void unconstrainedBoxAcceptsExactOptionalSurfaceAndSingleChildContract() {
        WidgetNode omitted = node(
                "unconstrained-omitted", "flutter.widgets.UnconstrainedBox",
                Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> omittedResult.issues().toString());

        WidgetNode child = text("unconstrained-child");
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            WidgetNode valid = node(
                    "unconstrained-alignment-" + basis.name(),
                    "flutter.widgets.UnconstrainedBox",
                    Map.of(name("alignment"),
                            new PropertyValue.AlignmentGeometryValue(
                                    basis,
                                    new BigDecimal("1.5"),
                                    new BigDecimal("-0.5"))),
                    Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> result.issues().toString());
        }

        Map<String, List<String>> accepted = Map.of(
                "textDirection", List.of("rtl", "ltr"),
                "constrainedAxis", List.of("horizontal", "vertical"),
                "clipBehavior", List.of(
                        "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer"));
        Map<String, String> enumTypes = Map.of(
                "textDirection", "TextDirection",
                "constrainedAxis", "Axis",
                "clipBehavior", "Clip");
        for (Map.Entry<String, List<String>> entry : accepted.entrySet()) {
            for (String value : entry.getValue()) {
                WidgetNode valid = node(
                        "unconstrained-enum-" + entry.getKey() + '-' + value,
                        "flutter.widgets.UnconstrainedBox",
                        Map.of(name(entry.getKey()), new PropertyValue.EnumValue(
                                enumTypes.get(entry.getKey()), value)),
                        Map.of());
                ValidationResult result = validator().validate(
                        document(valid), BuiltInWidgetCatalog.getDefault());
                assertTrue(result.valid(), () -> result.issues().toString());
            }
        }

        Map<String, PropertyValue> rejectedEnums = Map.of(
                "textDirection", new PropertyValue.EnumValue(
                        "TextDirection", "up"),
                "constrainedAxis", new PropertyValue.EnumValue(
                        "Axis", "diagonal"),
                "clipBehavior", new PropertyValue.EnumValue("Clip", "visible"));
        for (Map.Entry<String, PropertyValue> entry : rejectedEnums.entrySet()) {
            WidgetNode invalid = node(
                    "unconstrained-enum-invalid-" + entry.getKey(),
                    "flutter.widgets.UnconstrainedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        WidgetNode wrongEnumType = node(
                "unconstrained-enum-type", "flutter.widgets.UnconstrainedBox",
                Map.of(name("constrainedAxis"),
                        new PropertyValue.EnumValue("TextDirection", "horizontal")),
                Map.of());
        assertEquals("/root/properties/constrainedAxis", onlyIssue(
                validator().validate(
                        document(wrongEnumType), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONSTRAINT).path());

        for (Map.Entry<String, PropertyValue> entry : Map.<String, PropertyValue>of(
                "textDirection", new PropertyValue.StringValue("ltr"),
                "alignment", new PropertyValue.StringValue("Alignment.center"),
                "constrainedAxis", new PropertyValue.StringValue("horizontal"),
                "clipBehavior", new PropertyValue.IntegerValue(BigInteger.ZERO))
                .entrySet()) {
            WidgetNode invalid = node(
                    "unconstrained-kind-" + entry.getKey(),
                    "flutter.widgets.UnconstrainedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        WidgetNode unknownProperty = node(
                "unconstrained-unknown", "flutter.widgets.UnconstrainedBox",
                Map.of(name("fit"), new PropertyValue.EnumValue("BoxFit", "contain")),
                Map.of());
        assertEquals("/root/properties/fit", onlyIssue(
                validator().validate(
                        document(unknownProperty), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.UNKNOWN_PROPERTY).path());

        WidgetNode wrongSlot = node(
                "unconstrained-list-child", "flutter.widgets.UnconstrainedBox",
                Map.of(), Map.of(slotName("child"),
                        new WidgetSlot.ListSlot(List.of(child))));
        assertEquals("/root/slots/child", onlyIssue(
                validator().validate(
                        document(wrongSlot), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.SLOT_KIND).path());

        WidgetNode expandedChild = expanded(
                "unconstrained-expanded", Map.of(), text("expanded-text"));
        WidgetNode invalidPlacement = node(
                "unconstrained-expanded-parent", "flutter.widgets.UnconstrainedBox",
                Map.of(), Map.of(slotName("child"),
                        WidgetSlot.SingleSlot.of(expandedChild)));
        ValidationIssue placement = onlyIssue(
                validator().validate(
                        document(invalidPlacement), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root/slots/child/child", placement.path());
        assertTrue(placement.message().contains(
                "flutter.widgets.UnconstrainedBox.child"));
    }

    @Test
    void limitedBoxAcceptsOnlyFiniteNonNegativeDoubleBoundsAndOptionalSingleChild() {
        WidgetNode omitted = node(
                "limited-omitted", "flutter.widgets.LimitedBox",
                Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> omittedResult.issues().toString());

        WidgetNode child = text("limited-child");
        WidgetNode valid = node(
                "limited-valid", "flutter.widgets.LimitedBox",
                Map.of(
                        name("maxWidth"),
                                new PropertyValue.DoubleValue(BigDecimal.ZERO),
                        name("maxHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("720.5"))),
                Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
        ValidationResult validResult = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(validResult.valid(), () -> validResult.issues().toString());

        for (Map.Entry<String, PropertyValue> entry : Map.<String, PropertyValue>of(
                "maxWidth", new PropertyValue.DoubleValue(
                        new BigDecimal("-0.5")),
                "maxHeight", new PropertyValue.DoubleValue(
                        new BigDecimal("1E+309"))).entrySet()) {
            WidgetNode invalid = node(
                    "limited-constraint-" + entry.getKey(),
                    "flutter.widgets.LimitedBox",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        for (String property : List.of("maxWidth", "maxHeight")) {
            WidgetNode invalid = node(
                    "limited-kind-" + property,
                    "flutter.widgets.LimitedBox",
                    Map.of(name(property),
                            new PropertyValue.IntegerValue(BigInteger.ZERO)),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/" + property, issue.path());
        }

        WidgetNode unknownProperty = node(
                "limited-unknown", "flutter.widgets.LimitedBox",
                Map.of(name("minWidth"),
                        new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                Map.of());
        assertEquals("/root/properties/minWidth", onlyIssue(
                validator().validate(
                        document(unknownProperty), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.UNKNOWN_PROPERTY).path());

        WidgetNode wrongSlot = node(
                "limited-list-child", "flutter.widgets.LimitedBox",
                Map.of(), Map.of(slotName("child"),
                        new WidgetSlot.ListSlot(List.of(child))));
        assertEquals("/root/slots/child", onlyIssue(
                validator().validate(
                        document(wrongSlot), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.SLOT_KIND).path());

        WidgetNode expandedChild = expanded(
                "limited-expanded", Map.of(), text("expanded-text"));
        WidgetNode invalidPlacement = node(
                "limited-expanded-parent", "flutter.widgets.LimitedBox",
                Map.of(), Map.of(slotName("child"),
                        WidgetSlot.SingleSlot.of(expandedChild)));
        ValidationIssue placement = onlyIssue(
                validator().validate(
                        document(invalidPlacement), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root/slots/child/child", placement.path());
        assertTrue(placement.message().contains(
                "flutter.widgets.LimitedBox.child"));
    }

    @Test
    void overflowBoxValidatesExactTypedSurfaceAxisRelationshipsAndOptionalChild() {
        WidgetNode omitted = node(
                "overflow-omitted", "flutter.widgets.OverflowBox",
                Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> omittedResult.issues().toString());

        WidgetNode child = text("overflow-child");
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            WidgetNode valid = node(
                    "overflow-valid-" + basis.name(),
                    "flutter.widgets.OverflowBox",
                    Map.of(
                            name("alignment"),
                                    new PropertyValue.AlignmentGeometryValue(
                                            basis,
                                            new BigDecimal("-1.25"),
                                            new BigDecimal("0.75")),
                            name("minWidth"),
                                    new PropertyValue.DoubleValue(BigDecimal.ZERO),
                            name("maxWidth"),
                                    new PropertyValue.DoubleValue(BigDecimal.ZERO),
                            name("minHeight"),
                                    new PropertyValue.DoubleValue(
                                            new BigDecimal("24.5")),
                            name("maxHeight"),
                                    new PropertyValue.DoubleValue(
                                            new BigDecimal("720.5")),
                            name("fit"),
                                    new PropertyValue.EnumValue(
                                            "OverflowBoxFit", "deferToChild")),
                    Map.of(slotName("child"), WidgetSlot.SingleSlot.of(child)));
            ValidationResult validResult = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(validResult.valid(), () -> validResult.issues().toString());
        }

        for (String fit : List.of("max", "deferToChild")) {
            WidgetNode valid = node(
                    "overflow-fit-" + fit,
                    "flutter.widgets.OverflowBox",
                    Map.of(name("fit"),
                            new PropertyValue.EnumValue("OverflowBoxFit", fit)),
                    Map.of());
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> result.issues().toString());
        }

        for (String property : List.of(
                "minWidth", "maxWidth", "minHeight", "maxHeight")) {
            WidgetNode wrongKind = node(
                    "overflow-kind-" + property,
                    "flutter.widgets.OverflowBox",
                    Map.of(name(property),
                            new PropertyValue.IntegerValue(BigInteger.ZERO)),
                    Map.of());
            assertEquals("/root/properties/" + property, onlyIssue(
                    validator().validate(
                            document(wrongKind), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND).path());

            for (PropertyValue.DoubleValue value : List.of(
                    new PropertyValue.DoubleValue(new BigDecimal("-0.5")),
                    new PropertyValue.DoubleValue(new BigDecimal("1E+309")))) {
                WidgetNode outOfRange = node(
                        "overflow-range-" + property,
                        "flutter.widgets.OverflowBox",
                        Map.of(name(property), value), Map.of());
                assertEquals("/root/properties/" + property, onlyIssue(
                        validator().validate(
                                document(outOfRange),
                                BuiltInWidgetCatalog.getDefault()),
                        WidgetTreeValidator.PROPERTY_CONSTRAINT).path());
            }
        }

        for (Map<String, PropertyValue> invalidFit : List.of(
                Map.<String, PropertyValue>of(
                        "fit", new PropertyValue.EnumValue("StackFit", "expand")),
                Map.<String, PropertyValue>of(
                        "fit", new PropertyValue.EnumValue(
                        "OverflowBoxFit", "invalid")))) {
            WidgetNode invalid = node(
                    "overflow-fit-invalid", "flutter.widgets.OverflowBox",
                    invalidFit.entrySet().stream().collect(
                            java.util.stream.Collectors.toMap(
                                    entry -> name(entry.getKey()), Map.Entry::getValue)),
                    Map.of());
            assertEquals("/root/properties/fit", onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT).path());
        }

        WidgetNode wrongAlignmentKind = node(
                "overflow-alignment-kind", "flutter.widgets.OverflowBox",
                Map.of(name("alignment"),
                        new PropertyValue.StringValue("center")), Map.of());
        assertEquals("/root/properties/alignment", onlyIssue(
                validator().validate(
                        document(wrongAlignmentKind), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_KIND).path());

        for (Map<PropertyName, PropertyValue> invalidAxis : List.of(
                Map.<PropertyName, PropertyValue>of(
                        name("minWidth"),
                                new PropertyValue.DoubleValue(new BigDecimal("80")),
                        name("maxWidth"),
                                new PropertyValue.DoubleValue(new BigDecimal("40"))),
                Map.<PropertyName, PropertyValue>of(
                        name("minHeight"),
                                new PropertyValue.DoubleValue(new BigDecimal("48")),
                        name("maxHeight"),
                                new PropertyValue.DoubleValue(new BigDecimal("24"))))) {
            WidgetNode invalid = node(
                    "overflow-axis-invalid", "flutter.widgets.OverflowBox",
                    invalidAxis, Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            String maximum = invalidAxis.containsKey(name("maxWidth"))
                    ? "maxWidth" : "maxHeight";
            assertEquals("/root/properties/" + maximum, issue.path());
            assertTrue(issue.message().contains("cannot be greater"));
        }

        WidgetNode unknownProperty = node(
                "overflow-unknown", "flutter.widgets.OverflowBox",
                Map.of(name("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "none")),
                Map.of());
        assertEquals("/root/properties/clipBehavior", onlyIssue(
                validator().validate(
                        document(unknownProperty), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.UNKNOWN_PROPERTY).path());

        WidgetNode wrongSlot = node(
                "overflow-list-child", "flutter.widgets.OverflowBox",
                Map.of(), Map.of(slotName("child"),
                        new WidgetSlot.ListSlot(List.of(child))));
        assertEquals("/root/slots/child", onlyIssue(
                validator().validate(
                        document(wrongSlot), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.SLOT_KIND).path());

        WidgetNode expandedChild = expanded(
                "overflow-expanded", Map.of(), text("expanded-text"));
        WidgetNode invalidPlacement = node(
                "overflow-expanded-parent", "flutter.widgets.OverflowBox",
                Map.of(), Map.of(slotName("child"),
                        WidgetSlot.SingleSlot.of(expandedChild)));
        ValidationIssue placement = onlyIssue(
                validator().validate(
                        document(invalidPlacement), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.WIDGET_PLACEMENT);
        assertEquals("/root/slots/child/child", placement.path());
        assertTrue(placement.message().contains(
                "flutter.widgets.OverflowBox.child"));
    }

    @Test
    void stackAcceptsReviewedAlignmentEnumsAndOptionalAnyWidgetChildren() {
        WidgetNode omitted = node(
                "stack-omitted", "flutter.widgets.Stack", Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> "Issues were: " + omittedResult.issues());

        WidgetNode child = node(
                "stack-child", "flutter.widgets.Text",
                Map.of(name("data"), new PropertyValue.StringValue("Layer")),
                Map.of());
        for (PropertyValue.AlignmentGeometryValue.HorizontalBasis basis
                : PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()) {
            WidgetNode valid = node(
                    "stack-alignment-" + basis.name(),
                    "flutter.widgets.Stack",
                    Map.of(name("alignment"),
                            new PropertyValue.AlignmentGeometryValue(
                                    basis,
                                    new BigDecimal("-0.25"),
                                    new BigDecimal("0.75"))),
                    Map.of(slotName("children"),
                            new WidgetSlot.ListSlot(List.of(child))));
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> "Issues were: " + result.issues());
        }

        Map<String, String> enumTypes = Map.of(
                "textDirection", "TextDirection",
                "fit", "StackFit",
                "clipBehavior", "Clip");
        Map<String, List<String>> accepted = Map.of(
                "textDirection", List.of("rtl", "ltr"),
                "fit", List.of("loose", "expand", "passthrough"),
                "clipBehavior", List.of(
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        for (Map.Entry<String, List<String>> entry : accepted.entrySet()) {
            for (String value : entry.getValue()) {
                WidgetNode valid = node(
                        "stack-enum-" + entry.getKey() + '-' + value,
                        "flutter.widgets.Stack",
                        Map.of(name(entry.getKey()), new PropertyValue.EnumValue(
                                enumTypes.get(entry.getKey()), value)),
                        Map.of(slotName("children"),
                                new WidgetSlot.ListSlot(List.of(child))));
                ValidationResult result = validator().validate(
                        document(valid), BuiltInWidgetCatalog.getDefault());
                assertTrue(result.valid(), () -> "Issues were: " + result.issues());
            }
        }

        Map<String, PropertyValue> rejectedEnums = Map.of(
                "textDirection", new PropertyValue.EnumValue("TextDirection", "up"),
                "fit", new PropertyValue.EnumValue("StackFit", "cover"),
                "clipBehavior", new PropertyValue.EnumValue("Clip", "visible"));
        for (Map.Entry<String, PropertyValue> entry : rejectedEnums.entrySet()) {
            WidgetNode invalid = node(
                    "stack-enum-invalid-" + entry.getKey(),
                    "flutter.widgets.Stack",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }

        for (Map.Entry<String, PropertyValue> entry : Map.<String, PropertyValue>of(
                "alignment", new PropertyValue.StringValue("topStart"),
                "textDirection", new PropertyValue.BooleanValue(true),
                "fit", new PropertyValue.StringValue("loose"),
                "clipBehavior", new PropertyValue.IntegerValue(BigInteger.ZERO))
                .entrySet()) {
            WidgetNode invalid = node(
                    "stack-kind-" + entry.getKey(),
                    "flutter.widgets.Stack",
                    Map.of(name(entry.getKey()), entry.getValue()),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(
                            document(invalid), BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_KIND);
            assertEquals("/root/properties/" + entry.getKey(), issue.path());
        }
    }

    @Test
    void textFieldAcceptsOmittedDefaultsAndACompleteReviewedMultilineSurface() {
        WidgetNode omitted = node(
                "textfield-omitted", "flutter.material.TextField",
                Map.of(), Map.of());
        ValidationResult omittedResult = validator().validate(
                document(omitted), BuiltInWidgetCatalog.getDefault());
        assertTrue(omittedResult.valid(), () -> "Issues were: " + omittedResult.issues());

        WidgetNode configured = node(
                "textfield-configured", "flutter.material.TextField",
                Map.ofEntries(
                        Map.entry(name("keyboardType"),
                                new PropertyValue.StringValue("multiline")),
                        Map.entry(name("textInputAction"),
                                new PropertyValue.EnumValue("TextInputAction", "newline")),
                        Map.entry(name("maxLines"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(4))),
                        Map.entry(name("minLines"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                        Map.entry(name("maxLength"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(-1))),
                        Map.entry(name("cursorWidth"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                        Map.entry(name("cursorRadiusX"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                        Map.entry(name("cursorRadiusY"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(3))),
                        Map.entry(name("scrollPaddingLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.entry(name("scrollPaddingTop"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                        Map.entry(name("scrollPaddingRight"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(3))),
                        Map.entry(name("scrollPaddingBottom"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(4))),
                        Map.entry(name("selectionHeightStyle"),
                                new PropertyValue.EnumValue(
                                        "BoxHeightStyle", "includeLineSpacingMiddle")),
                        Map.entry(name("dragStartBehavior"),
                                new PropertyValue.EnumValue("DragStartBehavior", "down")),
                        Map.entry(name("mouseCursor"),
                                new PropertyValue.StringValue("text"))),
                Map.of());
        ValidationResult configuredResult = validator().validate(
                document(configured), BuiltInWidgetCatalog.getDefault());
        assertTrue(configuredResult.valid(),
                () -> "Issues were: " + configuredResult.issues());
    }

    @Test
    void textFieldRequiresCompleteRadiusAndScrollPaddingCompounds() {
        WidgetNode radius = node(
                "textfield-radius", "flutter.material.TextField",
                Map.of(name("cursorRadiusX"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of());
        ValidationIssue radiusIssue = onlyIssue(
                validator().validate(document(radius), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);
        assertEquals("/root/properties/cursorRadiusY", radiusIssue.path());
        assertEquals(
                "TextField cursorRadius requires cursorRadiusX and cursorRadiusY together.",
                radiusIssue.message());

        WidgetNode padding = node(
                "textfield-padding", "flutter.material.TextField",
                Map.of(
                        name("scrollPaddingLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE),
                        name("scrollPaddingBottom"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of());
        ValidationIssue paddingIssue = onlyIssue(
                validator().validate(document(padding), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);
        assertEquals("/root/properties/scrollPaddingTop", paddingIssue.path());
    }

    @Test
    void textFieldMirrorsAllPinnedLineAndLengthAssertions() {
        WidgetNode expandsWithLines = node(
                "textfield-expands-lines", "flutter.material.TextField",
                Map.of(
                        name("expands"), new PropertyValue.BooleanValue(true),
                        name("maxLines"),
                                new PropertyValue.IntegerValue(BigInteger.ONE),
                        name("minLines"),
                                new PropertyValue.IntegerValue(BigInteger.ONE)),
                Map.of());
        ValidationResult expandsResult = validator().validate(
                document(expandsWithLines), BuiltInWidgetCatalog.getDefault());
        assertEquals(List.of(
                        WidgetTreeValidator.PROPERTY_CONFLICT,
                        WidgetTreeValidator.PROPERTY_CONFLICT),
                codes(expandsResult));
        assertEquals(List.of(
                        "/root/properties/maxLines",
                        "/root/properties/minLines"),
                expandsResult.issues().stream().map(ValidationIssue::path).toList());

        WidgetNode minAboveDefault = node(
                "textfield-min-default", "flutter.material.TextField",
                Map.of(name("minLines"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of());
        assertEquals("/root/properties/minLines", onlyIssue(
                validator().validate(document(minAboveDefault),
                        BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONSTRAINT).path());

        WidgetNode obscureMultiline = node(
                "textfield-obscure", "flutter.material.TextField",
                Map.of(
                        name("obscureText"), new PropertyValue.BooleanValue(true),
                        name("maxLines"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of());
        assertEquals("/root/properties/obscureText", onlyIssue(
                validator().validate(document(obscureMultiline),
                        BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT).path());

        WidgetNode zeroLength = node(
                "textfield-length", "flutter.material.TextField",
                Map.of(name("maxLength"),
                        new PropertyValue.IntegerValue(BigInteger.ZERO)),
                Map.of());
        assertEquals("/root/properties/maxLength", onlyIssue(
                validator().validate(document(zeroLength),
                        BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONSTRAINT).path());

        WidgetNode newlineText = node(
                "textfield-newline", "flutter.material.TextField",
                Map.of(
                        name("keyboardType"), new PropertyValue.StringValue("text"),
                        name("textInputAction"),
                                new PropertyValue.EnumValue("TextInputAction", "newline"),
                        name("maxLines"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of());
        ValidationIssue newlineIssue = onlyIssue(
                validator().validate(document(newlineText),
                        BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT);
        assertEquals("/root/properties/keyboardType", newlineIssue.path());
        assertEquals(
                "Use keyboardType TextInputType.multiline when using "
                + "TextInputAction.newline on a multiline TextField.",
                newlineIssue.message());
    }

    @Test
    void textFieldObscurerAcceptsBmpScalarsAndRejectsEmojiAndLoneSurrogates() {
        for (String value : List.of(
                "A",
                "\u2022",
                Character.toString(0x0000),
                Character.toString(0xD7FF),
                Character.toString(0xE000),
                Character.toString(0xFFFF))) {
            WidgetNode valid = node(
                    "textfield-obscurer-valid-" + Integer.toHexString(value.charAt(0)),
                    "flutter.material.TextField",
                    Map.of(name("obscuringCharacter"),
                            new PropertyValue.StringValue(value)),
                    Map.of());
            ValidationResult result = validator().validate(
                    document(valid), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(),
                    () -> Integer.toHexString(value.charAt(0)) + ": "
                    + result.issues());
        }

        for (String value : List.of(
                new String(Character.toChars(0x1F600)),
                Character.toString(0xD800),
                Character.toString(0xDFFF))) {
            WidgetNode invalid = node(
                    "textfield-obscurer-" + value.length(),
                    "flutter.material.TextField",
                    Map.of(name("obscuringCharacter"),
                            new PropertyValue.StringValue(value)),
                    Map.of());
            ValidationIssue issue = onlyIssue(
                    validator().validate(document(invalid),
                            BuiltInWidgetCatalog.getDefault()),
                    WidgetTreeValidator.PROPERTY_CONSTRAINT);
            assertEquals("/root/properties/obscuringCharacter", issue.path());
        }
    }

    @Test
    void imageRequiresAProviderAndAcceptsACompletePositiveCenterSlice() {
        WidgetNode missingProvider = node(
                "image-missing", "flutter.widgets.Image", Map.of(), Map.of());
        ValidationIssue missing = onlyIssue(
                validator().validate(
                        document(missingProvider), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.MISSING_PROPERTY);
        assertEquals("/root/properties/image", missing.path());

        WidgetNode valid = node(
                "image-valid",
                "flutter.widgets.Image",
                Map.ofEntries(
                        Map.entry(name("image"), imageProvider()),
                        Map.entry(name("opacity"),
                                new PropertyValue.DoubleValue(new BigDecimal("0.75"))),
                        Map.entry(name("centerSliceLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.entry(name("centerSliceTop"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                        Map.entry(name("centerSliceRight"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(20))),
                        Map.entry(name("centerSliceBottom"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(30)))),
                Map.of());
        ValidationResult validResult = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(validResult.valid(), () -> "Issues were: " + validResult.issues());
    }

    @Test
    void imageCenterSliceIsAllOrNoneWithStrictGeometryAndCompatibleFit() {
        WidgetNode partial = node(
                "image-partial",
                "flutter.widgets.Image",
                Map.of(
                        name("image"), imageProvider(),
                        name("centerSliceLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                Map.of());
        ValidationIssue dependency = onlyIssue(
                validator().validate(
                        document(partial), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);
        assertEquals("/root/properties/centerSliceTop", dependency.path());

        WidgetNode invalid = node(
                "image-invalid-slice",
                "flutter.widgets.Image",
                Map.ofEntries(
                        Map.entry(name("image"), imageProvider()),
                        Map.entry(name("centerSliceLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.TEN)),
                        Map.entry(name("centerSliceTop"),
                                new PropertyValue.DoubleValue(BigDecimal.TEN)),
                        Map.entry(name("centerSliceRight"),
                                new PropertyValue.DoubleValue(BigDecimal.TEN)),
                        Map.entry(name("centerSliceBottom"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.entry(name("fit"),
                                new PropertyValue.EnumValue("BoxFit", "cover"))),
                Map.of());
        ValidationResult result = validator().validate(
                document(invalid), BuiltInWidgetCatalog.getDefault());
        assertEquals(List.of(
                        WidgetTreeValidator.PROPERTY_CONSTRAINT,
                        WidgetTreeValidator.PROPERTY_CONSTRAINT,
                        WidgetTreeValidator.PROPERTY_CONFLICT),
                codes(result));
        assertEquals(List.of(
                        "/root/properties/centerSliceRight",
                        "/root/properties/centerSliceBottom",
                        "/root/properties/fit"),
                result.issues().stream().map(ValidationIssue::path).toList());

        WidgetNode noneFit = node(
                "image-none-fit",
                "flutter.widgets.Image",
                Map.ofEntries(
                        Map.entry(name("image"), imageProvider()),
                        Map.entry(name("centerSliceLeft"),
                                new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                        Map.entry(name("centerSliceTop"),
                                new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                        Map.entry(name("centerSliceRight"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.entry(name("centerSliceBottom"),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.entry(name("fit"),
                                new PropertyValue.EnumValue("BoxFit", "none"))),
                Map.of());
        ValidationIssue conflict = onlyIssue(
                validator().validate(
                        document(noneFit), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT);
        assertEquals("/root/properties/fit", conflict.path());
    }

    @Test
    void containerEnforcesBackgroundAndClipRelationshipsAfterNestedValidation() {
        PropertyValue.BoxDecorationValue decoration = boxDecoration(
                new ColorSource.Literal(0xFF102030L));
        WidgetNode conflicting = node(
                "container-conflict", "flutter.widgets.Container",
                Map.of(
                        name("color"), new PropertyValue.ColorValue(0xFF000000L),
                        name("decoration"), decoration),
                Map.of());
        ValidationIssue conflict = onlyIssue(
                validator().validate(
                        document(conflicting), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT);
        assertEquals("/root/properties/decoration", conflict.path());

        WidgetNode missingDecoration = node(
                "container-clip", "flutter.widgets.Container",
                Map.of(name("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of());
        ValidationIssue dependency = onlyIssue(
                validator().validate(
                        document(missingDecoration), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_DEPENDENCY);
        assertEquals("/root/properties/clipBehavior", dependency.path());

        WidgetNode foregroundOnly = node(
                "container-foreground", "flutter.widgets.Container",
                Map.of(
                        name("foregroundDecoration"), decoration,
                        name("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAlias")),
                Map.of());
        assertEquals(WidgetTreeValidator.PROPERTY_DEPENDENCY,
                onlyIssue(
                        validator().validate(
                                document(foregroundOnly),
                                BuiltInWidgetCatalog.getDefault()),
                        WidgetTreeValidator.PROPERTY_DEPENDENCY).code());

        WidgetNode valid = node(
                "container-valid", "flutter.widgets.Container",
                Map.of(
                        name("decoration"), decoration,
                        name("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAlias")),
                Map.of());
        ValidationResult accepted = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(accepted.valid(), () -> "Issues were: " + accepted.issues());

        WidgetNode nestedUnreviewedTheme = node(
                "container-theme", "flutter.widgets.Container",
                Map.of(name("decoration"), boxDecoration(
                        new ColorSource.Theme(new dev.flutter.netbeans.designer.model.ThemeToken(
                                "material.colorScheme.notReviewed")))),
                Map.of());
        ValidationIssue nested = onlyIssue(
                validator().validate(
                        document(nestedUnreviewedTheme),
                        BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONSTRAINT);
        assertEquals("/root/properties/decoration", nested.path());
    }

    private static PropertyValue.BoxDecorationValue boxDecoration(
            ColorSource color) {
        return new PropertyValue.BoxDecorationValue(
                Optional.of(color), Optional.empty(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
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

    @Test
    void listViewSemanticChildCountCannotExceedItsStaticChildren() {
        WidgetNode first = text("first-list-child");
        WidgetNode invalid = node(
                "list-view-invalid",
                "flutter.widgets.ListView",
                Map.of(name("semanticChildCount"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of(slotName("children"),
                        new WidgetSlot.ListSlot(List.of(first))));

        ValidationIssue issue = onlyIssue(
                validator().validate(document(invalid), BuiltInWidgetCatalog.getDefault()),
                WidgetTreeValidator.PROPERTY_CONFLICT);

        assertEquals("/root/properties/semanticChildCount", issue.path());
        assertTrue(issue.message().contains("exceeds the current children count 1"));

        WidgetNode valid = node(
                "list-view-valid",
                "flutter.widgets.ListView",
                Map.of(name("semanticChildCount"),
                        new PropertyValue.IntegerValue(BigInteger.ONE)),
                Map.of(slotName("children"),
                        new WidgetSlot.ListSlot(List.of(text("valid-list-child")))));
        ValidationResult result = validator().validate(
                document(valid), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.valid(), () -> result.issues().toString());
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

    private static WidgetNode text(String idSeed) {
        return node(idSeed, "flutter.widgets.Text", Map.of(
                name("data"), new PropertyValue.StringValue(idSeed)), Map.of());
    }

    private static WidgetNode expanded(
            String idSeed,
            Map<PropertyName, PropertyValue> properties,
            WidgetNode child) {
        return node(idSeed, "flutter.widgets.Expanded", properties, Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(child)));
    }

    private static WidgetNode flexible(
            String idSeed,
            Map<PropertyName, PropertyValue> properties,
            WidgetNode child) {
        return node(idSeed, "flutter.widgets.Flexible", properties, Map.of(
                slotName("child"), WidgetSlot.SingleSlot.of(child)));
    }

    private static WidgetNode spacer(
            String idSeed,
            Map<PropertyName, PropertyValue> properties) {
        return node(idSeed, "flutter.widgets.Spacer", properties, Map.of());
    }

    private static PropertyValue.ImageProviderValue imageProvider() {
        return PropertyValue.ImageProviderValue.asset("assets/image.png");
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
