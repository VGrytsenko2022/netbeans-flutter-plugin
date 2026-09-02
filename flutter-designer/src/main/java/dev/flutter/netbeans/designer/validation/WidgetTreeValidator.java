package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ContainerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ParameterStyle;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetPlacementRules;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Performs bounded structural and catalog validation of an immutable widget tree. */
public final class WidgetTreeValidator {
    public static final String DUPLICATE_WIDGET_ID = "designer.widget.id.duplicate";
    public static final String UNKNOWN_WIDGET_TYPE = "designer.widget.type.unknown";
    public static final String MISSING_PROPERTY = "designer.property.missing";
    public static final String UNKNOWN_PROPERTY = "designer.property.unknown";
    public static final String PROPERTY_KIND = "designer.property.kind";
    public static final String PROPERTY_CONSTRAINT = "designer.property.constraint";
    public static final String PROPERTY_DEPENDENCY = "designer.property.dependency";
    public static final String PROPERTY_CONFLICT = "designer.property.conflict";
    public static final String PROPERTY_OVERRIDE = "designer.property.override";
    public static final String PROPERTY_UNIQUENESS = "designer.property.uniqueness";
    public static final String MISSING_SLOT = "designer.slot.missing";
    public static final String UNKNOWN_SLOT = "designer.slot.unknown";
    public static final String SLOT_KIND = "designer.slot.kind";
    public static final String SLOT_CARDINALITY = "designer.slot.cardinality";
    public static final String SLOT_NULL = "designer.slot.null";
    public static final String SLOT_ACCEPTANCE = "designer.slot.acceptance";
    public static final String WIDGET_PLACEMENT = "designer.widget.placement";
    public static final String POSITIONAL_GAP = "designer.parameter.positional.gap";
    public static final String DEPTH_LIMIT = "designer.tree.depth.limit";
    public static final String NODE_LIMIT = "designer.tree.nodes.limit";
    public static final String PROPERTY_LIMIT = "designer.widget.properties.limit";
    public static final String SLOT_LIMIT = "designer.widget.slots.limit";
    public static final String ISSUES_TRUNCATED = "designer.validation.issues.truncated";

    private static final String ROOT_PATH = "/root";
    private static final Comparator<PropertyName> PROPERTY_NAME_ORDER =
            Comparator.comparing(PropertyName::value);
    private static final Comparator<SlotName> SLOT_NAME_ORDER =
            Comparator.comparing(SlotName::value);

    private final ValidationLimits limits;

    public WidgetTreeValidator() {
        this(ValidationLimits.defaults());
    }

    public WidgetTreeValidator(ValidationLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ValidationResult validate(DesignerDocument document, WidgetCatalog catalog) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(catalog, "catalog");

        IssueCollector issues = new IssueCollector(limits.maxIssues());
        Map<StableId, String> firstIdPaths = new HashMap<>();
        Map<String, String> firstSemanticsIdentifierPaths = new HashMap<>();
        Deque<NodeFrame> pending = new ArrayDeque<>();
        TraversalFrontier frontier = new TraversalFrontier(limits.maxNodes());
        pending.push(new NodeFrame(document.root(), ROOT_PATH, 1, null, null));

        while (!pending.isEmpty() && !issues.truncated()) {
            NodeFrame frame = pending.pop();
            WidgetNode node = frame.node();

            if (frame.depth() > limits.maxDepth()) {
                issues.add(issue(
                        DEPTH_LIMIT,
                        frame.path(),
                        node.id(),
                        "Widget '" + node.type().value() + "' exceeds the maximum tree depth of "
                        + limits.maxDepth() + ". Its descendants were not inspected."));
                continue;
            }

            String firstPath = firstIdPaths.putIfAbsent(node.id(), frame.path());
            if (firstPath != null) {
                issues.add(issue(
                        DUPLICATE_WIDGET_ID,
                        frame.path(),
                        node.id(),
                        "Widget id '" + node.id() + "' at '" + frame.path()
                        + "' duplicates the widget first declared at '" + firstPath + "'."));
            }
            if (issues.truncated()) {
                break;
            }

            Optional<WidgetDefinition> definition = catalog.find(node.type());
            if (definition.isEmpty()) {
                issues.add(issue(
                        UNKNOWN_WIDGET_TYPE,
                        frame.path(),
                        node.id(),
                        "Widget '" + node.id() + "' at '" + frame.path()
                        + "' uses unregistered catalog type '" + node.type().value() + "'."));
            } else {
                WidgetPlacementRules.Decision placement = frame.depth() == 1
                        ? WidgetPlacementRules.evaluateRoot(definition.orElseThrow())
                        : frame.parentDefinition() != null && frame.parentSlot() != null
                                ? WidgetPlacementRules.evaluate(
                                        frame.parentDefinition(),
                                        frame.parentSlot(),
                                        definition.orElseThrow())
                                : null;
                if (placement != null && !placement.accepted()) {
                    String issueCode = placement.rejectionKind().orElseThrow()
                                    == WidgetPlacementRules.RejectionKind.SLOT_ACCEPTANCE
                            ? SLOT_ACCEPTANCE
                            : WIDGET_PLACEMENT;
                    issues.add(issue(
                            issueCode,
                            frame.path(),
                            node.id(),
                            placement.reason() + " Model path: '" + frame.path() + "'."));
                }
            }
            if (issues.truncated()) {
                break;
            }

            validateProperties(
                    node,
                    definition.orElse(null),
                    frame.path(),
                    firstSemanticsIdentifierPaths,
                    issues);
            List<NodeFrame> children = validateSlotsAndCollectChildren(
                    node,
                    definition.orElse(null),
                    frame.path(),
                    frame.depth(),
                    issues,
                    frontier);
            if (definition.isPresent() && !issues.truncated() && !frontier.limitReached()) {
                validateOptionalPositionalPrefix(
                        node,
                        definition.orElseThrow(),
                        frame.path(),
                        issues);
            }
            if (frontier.limitReached()) {
                pending.clear();
                break;
            }
            for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(children.get(index));
            }
        }

        return new ValidationResult(issues.snapshot());
    }

    private static void validateOptionalPositionalPrefix(
            WidgetNode node,
            WidgetDefinition definition,
            String nodePath,
            IssueCollector issues) {
        List<PositionalArgument> positional = new ArrayList<>();
        for (PropertyDefinition property : definition.properties()) {
            if (property.parameter().style() == ParameterStyle.POSITIONAL) {
                positional.add(new PositionalArgument(
                        property.parameter().order(),
                        property.parameter().required(),
                        node.properties().containsKey(property.name()),
                        property.name().value(),
                        nodePath + "/properties/" + pointer(property.name().value())));
            }
        }
        for (SlotDefinition slot : definition.slots()) {
            if (slot.parameter().style() == ParameterStyle.POSITIONAL) {
                positional.add(new PositionalArgument(
                        slot.parameter().order(),
                        slot.parameter().required(),
                        node.slots().containsKey(slot.name()),
                        slot.name().value(),
                        nodePath + "/slots/" + pointer(slot.name().value())));
            }
        }
        positional.sort(Comparator.comparingInt(PositionalArgument::order));

        PositionalArgument firstOmitted = null;
        for (PositionalArgument argument : positional) {
            if (argument.required()) {
                continue;
            }
            if (!argument.present()) {
                if (firstOmitted == null) {
                    firstOmitted = argument;
                }
                continue;
            }
            if (firstOmitted != null) {
                issues.add(issue(
                        POSITIONAL_GAP,
                        argument.path(),
                        node.id(),
                        "Optional positional argument '" + argument.name() + "' at order "
                        + argument.order() + " is present after omitted optional positional argument '"
                        + firstOmitted.name() + "' at order " + firstOmitted.order()
                        + "; Dart positional arguments must form a contiguous prefix."));
                if (issues.truncated()) {
                    return;
                }
            }
        }
    }

    private void validateProperties(
            WidgetNode node,
            WidgetDefinition definition,
            String nodePath,
            Map<String, String> firstSemanticsIdentifierPaths,
            IssueCollector issues) {
        String propertiesPath = nodePath + "/properties";
        if (node.properties().size() > limits.maxPropertiesPerWidget()) {
            issues.add(issue(
                    PROPERTY_LIMIT,
                    propertiesPath,
                    node.id(),
                    "Widget '" + node.type().value() + "' declares " + node.properties().size()
                    + " properties, exceeding the limit of "
                    + limits.maxPropertiesPerWidget() + "."));
        }
        if (definition == null || issues.truncated()) {
            return;
        }

        int inspected = 0;
        for (PropertyDefinition property : definition.properties()) {
            PropertyValue value = node.properties().get(property.name());
            if (value == null) {
                if (property.parameter().required()) {
                    issues.add(issue(
                            MISSING_PROPERTY,
                            propertiesPath + "/" + pointer(property.name().value()),
                            node.id(),
                            "Widget '" + node.type().value() + "' at '" + nodePath
                            + "' is missing required property '" + property.name().value() + "'."));
                }
                if (issues.truncated()) {
                    return;
                }
                continue;
            }
            if (inspected++ < limits.maxPropertiesPerWidget()) {
                validatePropertyValue(node, property, value, propertiesPath, issues);
            }
            if (issues.truncated()) {
                return;
            }
        }

        int remainingCapacity = Math.max(0, limits.maxPropertiesPerWidget() - inspected);
        List<PropertyName> unknown = smallestMatching(
                node.properties().keySet(),
                name -> definition.property(name).isEmpty(),
                PROPERTY_NAME_ORDER,
                remainingCapacity);
        for (PropertyName name : unknown) {
            if (inspected++ >= limits.maxPropertiesPerWidget()) {
                break;
            }
            issues.add(issue(
                    UNKNOWN_PROPERTY,
                    propertiesPath + "/" + pointer(name.value()),
                    node.id(),
                    "Widget '" + node.type().value() + "' at '" + nodePath
                    + "' declares unknown property '" + name.value() + "'."));
            if (issues.truncated()) {
                return;
            }
        }
        if (!issues.truncated()) {
            validateBuiltInPropertyRelationships(
                    node,
                    propertiesPath,
                    firstSemanticsIdentifierPaths,
                    issues);
        }
    }

    private static void validateBuiltInPropertyRelationships(
            WidgetNode node,
            String propertiesPath,
            Map<String, String> firstSemanticsIdentifierPaths,
            IssueCollector issues) {
        String type = node.type().value();
        if (type.equals("flutter.widgets.Column") || type.equals("flutter.widgets.Row")) {
            PropertyValue crossAxisAlignment = node.properties().get(
                    new PropertyName("crossAxisAlignment"));
            if (crossAxisAlignment instanceof PropertyValue.EnumValue alignment
                    && alignment.type().equals("CrossAxisAlignment")
                    && alignment.value().equals("baseline")
                    && !node.properties().containsKey(new PropertyName("textBaseline"))) {
                issues.add(issue(
                        PROPERTY_DEPENDENCY,
                        propertiesPath + "/textBaseline",
                        node.id(),
                        "Property 'textBaseline' is required when 'crossAxisAlignment' is "
                        + "CrossAxisAlignment.baseline on widget '" + type + "'."));
            }
        }

        if (type.equals(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.value())) {
            validateTextField(node, propertiesPath, issues);
            return;
        }

        if (type.equals(ListViewWidgetPropertySchema.LIST_VIEW_TYPE.value())) {
            validateListViewSemanticChildCount(node, propertiesPath, issues);
            return;
        }

        if (type.equals("flutter.widgets.Image")) {
            validateImageCenterSlice(node, propertiesPath, issues);
            return;
        }

        if (type.equals("flutter.widgets.OverflowBox")) {
            validateOverflowBoxConstraints(node, propertiesPath, issues);
            return;
        }

        if (type.equals("flutter.widgets.Icon")) {
            if (node.properties().containsKey(new PropertyName("weight"))
                    && node.properties().containsKey(new PropertyName("fontWeight"))) {
                issues.add(warning(
                        PROPERTY_OVERRIDE,
                        propertiesPath + "/fontWeight",
                        node.id(),
                        "Icon property 'weight' emits the variable-font wght axis and "
                        + "overrides 'fontWeight' while both values are set."));
            }
            return;
        }

        if (type.equals(ContainerWidgetPropertySchema.CONTAINER_TYPE.value())) {
            validateMutuallyExclusiveProperties(
                    node, propertiesPath, issues,
                    "color", "decoration", "Container background");
            PropertyValue clip = node.properties().get(new PropertyName("clipBehavior"));
            if (clip instanceof PropertyValue.EnumValue enumValue
                    && enumValue.type().equals("Clip")
                    && !enumValue.value().equals("none")
                    && !node.properties().containsKey(new PropertyName("decoration"))) {
                issues.add(issue(
                        PROPERTY_DEPENDENCY,
                        propertiesPath + "/clipBehavior",
                        node.id(),
                        "Container property 'clipBehavior' may be non-none only when "
                        + "'decoration' is set."));
            }
            return;
        }

        if (type.equals(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE.value())) {
            for (String prefix : List.of(
                    "style", "styleDisabled", "stylePressed",
                    "styleHovered", "styleFocused")) {
                validateElevatedButtonState(node, propertiesPath, issues, prefix);
            }
            validateElevatedButtonEffectiveDimensions(node, propertiesPath, issues);
            validateElevatedButtonEffectiveShapes(node, propertiesPath, issues);
            validateElevatedButtonTextInherit(node, propertiesPath, issues);
            validateElevatedButtonAlignment(node, propertiesPath, issues);
            return;
        }

        if (type.equals(AppBarWidgetPropertySchema.APP_BAR_TYPE.value())) {
            validateFontPackageDependency(
                    node, propertiesPath, issues,
                    "toolbarTextStylePackage", "toolbarTextStyleFontFamily",
                    "toolbarTextStyleFontFamilyFallback", "AppBar toolbarTextStyle");
            validateFontPackageDependency(
                    node, propertiesPath, issues,
                    "titleTextStylePackage", "titleTextStyleFontFamily",
                    "titleTextStyleFontFamilyFallback", "AppBar titleTextStyle");
            validateMutuallyExclusiveProperties(
                    node, propertiesPath, issues,
                    "toolbarTextStyleColor", "toolbarTextStyleForeground",
                    "AppBar toolbarTextStyle");
            validateMutuallyExclusiveProperties(
                    node, propertiesPath, issues,
                    "toolbarTextStyleBackgroundColor", "toolbarTextStyleBackground",
                    "AppBar toolbarTextStyle");
            validateMutuallyExclusiveProperties(
                    node, propertiesPath, issues,
                    "titleTextStyleColor", "titleTextStyleForeground",
                    "AppBar titleTextStyle");
            validateMutuallyExclusiveProperties(
                    node, propertiesPath, issues,
                    "titleTextStyleBackgroundColor", "titleTextStyleBackground",
                    "AppBar titleTextStyle");
            validateAppBarShape(node, propertiesPath, issues);
            return;
        }

        if (!type.equals("flutter.widgets.Text")) {
            return;
        }
        validateFontPackageDependency(
                node, propertiesPath, issues,
                "stylePackage", "styleFontFamily", "styleFontFamilyFallback",
                "Text style");
        validateFontPackageDependency(
                node, propertiesPath, issues,
                "strutPackage", "strutFontFamily", "strutFontFamilyFallback",
                "Text strutStyle");
        validateMutuallyExclusiveProperties(
                node, propertiesPath, issues,
                "styleColor", "styleForeground", "Text style");
        validateMutuallyExclusiveProperties(
                node, propertiesPath, issues,
                "styleBackgroundColor", "styleBackground", "Text style");

        PropertyValue value = node.properties().get(new PropertyName("semanticsIdentifier"));
        if (value instanceof PropertyValue.StringValue identifier) {
            String propertyPath = propertiesPath + "/semanticsIdentifier";
            String firstPath = firstSemanticsIdentifierPaths.putIfAbsent(
                    identifier.value(), propertyPath);
            if (firstPath != null) {
                issues.add(issue(
                        PROPERTY_UNIQUENESS,
                        propertyPath,
                        node.id(),
                        "Text semanticsIdentifier '" + identifier.value() + "' at '" + propertyPath
                        + "' duplicates the identifier first declared at '" + firstPath + "'."));
            }
        }
    }

    private static void validateListViewSemanticChildCount(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        BigInteger semanticCount = integerValue(node, "semanticChildCount");
        if (semanticCount == null) {
            return;
        }
        WidgetSlot children = node.slots().get(new SlotName("children"));
        int childCount = children instanceof WidgetSlot.ListSlot list
                ? list.children().size() : 0;
        if (semanticCount.compareTo(BigInteger.valueOf(childCount)) > 0) {
            issues.add(issue(
                    PROPERTY_CONFLICT,
                    propertiesPath + "/semanticChildCount",
                    node.id(),
                    "ListView semanticChildCount " + semanticCount
                    + " exceeds the current children count " + childCount + "."));
        }
    }

    private static void validateOverflowBoxConstraints(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        validateOverflowBoxAxis(
                node, propertiesPath, issues, "minWidth", "maxWidth", "width");
        validateOverflowBoxAxis(
                node, propertiesPath, issues, "minHeight", "maxHeight", "height");
    }

    private static void validateOverflowBoxAxis(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String minimumName,
            String maximumName,
            String axis) {
        PropertyValue minimumValue = node.properties().get(new PropertyName(minimumName));
        PropertyValue maximumValue = node.properties().get(new PropertyName(maximumName));
        if (!(minimumValue instanceof PropertyValue.DoubleValue minimum)
                || !(maximumValue instanceof PropertyValue.DoubleValue maximum)
                || minimum.value().compareTo(maximum.value()) <= 0) {
            return;
        }
        issues.add(issue(
                PROPERTY_CONSTRAINT,
                propertiesPath + '/' + maximumName,
                node.id(),
                "OverflowBox " + minimumName + " cannot be greater than "
                + maximumName + " for the " + axis + " axis."));
    }

    private static void validateTextField(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        validateTextFieldComponents(
                node,
                propertiesPath,
                issues,
                List.of("cursorRadiusX", "cursorRadiusY"),
                "TextField cursorRadius requires cursorRadiusX and cursorRadiusY together.");
        validateTextFieldComponents(
                node,
                propertiesPath,
                issues,
                List.of(
                        "scrollPaddingLeft", "scrollPaddingTop",
                        "scrollPaddingRight", "scrollPaddingBottom"),
                "TextField scrollPadding requires scrollPaddingLeft, scrollPaddingTop, "
                + "scrollPaddingRight, and scrollPaddingBottom together.");

        BigInteger maxLines = integerValue(node, "maxLines");
        BigInteger minLines = integerValue(node, "minLines");
        boolean expands = Boolean.TRUE.equals(booleanValue(node, "expands"));
        if (expands) {
            if (maxLines != null) {
                issues.add(issue(
                        PROPERTY_CONFLICT,
                        propertiesPath + "/maxLines",
                        node.id(),
                        "TextField expands=true requires maxLines and minLines to be omitted."));
            }
            if (minLines != null) {
                issues.add(issue(
                        PROPERTY_CONFLICT,
                        propertiesPath + "/minLines",
                        node.id(),
                        "TextField expands=true requires maxLines and minLines to be omitted."));
            }
        } else if (minLines != null) {
            BigInteger effectiveMaxLines = maxLines == null ? BigInteger.ONE : maxLines;
            if (minLines.compareTo(effectiveMaxLines) > 0) {
                issues.add(issue(
                        PROPERTY_CONSTRAINT,
                        propertiesPath + "/minLines",
                        node.id(),
                        "TextField minLines cannot be greater than maxLines."));
            }
        }

        BigInteger maxLength = integerValue(node, "maxLength");
        if (BigInteger.ZERO.equals(maxLength)) {
            issues.add(issue(
                    PROPERTY_CONSTRAINT,
                    propertiesPath + "/maxLength",
                    node.id(),
                    "TextField maxLength must be -1 or greater than zero."));
        }

        if (Boolean.TRUE.equals(booleanValue(node, "obscureText"))
                && (expands
                || (maxLines != null && !BigInteger.ONE.equals(maxLines)))) {
            issues.add(issue(
                    PROPERTY_CONFLICT,
                    propertiesPath + "/obscureText",
                    node.id(),
                    "TextField obscureText=true requires maxLines=1 and expands=false."));
        }

        PropertyValue actionValue = node.properties().get(
                new PropertyName("textInputAction"));
        PropertyValue keyboardValue = node.properties().get(
                new PropertyName("keyboardType"));
        boolean effectiveMultiline = expands
                || (maxLines != null && !BigInteger.ONE.equals(maxLines));
        if (effectiveMultiline
                && actionValue instanceof PropertyValue.EnumValue action
                && action.type().equals("TextInputAction")
                && action.value().equals("newline")
                && keyboardValue instanceof PropertyValue.StringValue keyboard
                && keyboard.value().equals("text")) {
            issues.add(issue(
                    PROPERTY_CONFLICT,
                    propertiesPath + "/keyboardType",
                    node.id(),
                    "Use keyboardType TextInputType.multiline when using "
                    + "TextInputAction.newline on a multiline TextField."));
        }
    }

    private static void validateTextFieldComponents(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            List<String> componentNames,
            String message) {
        List<String> present = componentNames.stream()
                .filter(name -> node.properties().containsKey(new PropertyName(name)))
                .toList();
        if (present.isEmpty() || present.size() == componentNames.size()) {
            return;
        }
        String firstMissing = componentNames.stream()
                .filter(name -> !present.contains(name))
                .findFirst()
                .orElseThrow();
        issues.add(issue(
                PROPERTY_DEPENDENCY,
                propertiesPath + '/' + firstMissing,
                node.id(),
                message));
    }

    private static void validateImageCenterSlice(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        List<String> componentNames = List.of(
                "centerSliceLeft",
                "centerSliceTop",
                "centerSliceRight",
                "centerSliceBottom");
        List<String> present = componentNames.stream()
                .filter(name -> node.properties().containsKey(new PropertyName(name)))
                .toList();
        if (!present.isEmpty() && present.size() != componentNames.size()) {
            String firstMissing = componentNames.stream()
                    .filter(name -> !present.contains(name))
                    .findFirst()
                    .orElseThrow();
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + '/' + firstMissing,
                    node.id(),
                    "Image centerSlice requires centerSliceLeft, centerSliceTop, "
                    + "centerSliceRight, and centerSliceBottom together."));
            return;
        }
        if (present.isEmpty()) {
            return;
        }

        PropertyValue leftValue = node.properties().get(new PropertyName("centerSliceLeft"));
        PropertyValue topValue = node.properties().get(new PropertyName("centerSliceTop"));
        PropertyValue rightValue = node.properties().get(new PropertyName("centerSliceRight"));
        PropertyValue bottomValue = node.properties().get(new PropertyName("centerSliceBottom"));
        if (leftValue instanceof PropertyValue.DoubleValue left
                && topValue instanceof PropertyValue.DoubleValue top
                && rightValue instanceof PropertyValue.DoubleValue right
                && bottomValue instanceof PropertyValue.DoubleValue bottom) {
            if (left.value().compareTo(right.value()) >= 0) {
                issues.add(issue(
                        PROPERTY_CONSTRAINT,
                        propertiesPath + "/centerSliceRight",
                        node.id(),
                        "Image centerSlice requires centerSliceLeft to be strictly less than "
                        + "centerSliceRight."));
            }
            if (top.value().compareTo(bottom.value()) >= 0) {
                issues.add(issue(
                        PROPERTY_CONSTRAINT,
                        propertiesPath + "/centerSliceBottom",
                        node.id(),
                        "Image centerSlice requires centerSliceTop to be strictly less than "
                        + "centerSliceBottom."));
            }
        }

        PropertyValue fitValue = node.properties().get(new PropertyName("fit"));
        if (fitValue instanceof PropertyValue.EnumValue fit
                && fit.type().equals("BoxFit")
                && (fit.value().equals("cover") || fit.value().equals("none"))) {
            issues.add(issue(
                    PROPERTY_CONFLICT,
                    propertiesPath + "/fit",
                    node.id(),
                    "Image centerSlice does not allow BoxFit.cover or BoxFit.none."));
        }
    }

    private static void validateMutuallyExclusiveProperties(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String firstProperty,
            String secondProperty,
            String owner) {
        if (!node.properties().containsKey(new PropertyName(firstProperty))
                || !node.properties().containsKey(new PropertyName(secondProperty))) {
            return;
        }
        issues.add(issue(
                PROPERTY_CONFLICT,
                propertiesPath + '/' + secondProperty,
                node.id(),
                owner + " properties '" + firstProperty + "' and '" + secondProperty
                + "' are mutually exclusive in Flutter TextStyle."));
    }

    private static void validateFontPackageDependency(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String packageProperty,
            String familyProperty,
            String fallbackProperty,
            String owner) {
        if (node.properties().containsKey(new PropertyName(packageProperty))
                && !hasNonBlankString(node, familyProperty)
                && !hasNonBlankLine(node, fallbackProperty)) {
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + '/' + packageProperty,
                    node.id(),
                    owner + " property '" + packageProperty + "' requires '"
                    + familyProperty + "' or '" + fallbackProperty + "'."));
        }
    }

    private static void validateAppBarShape(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        PropertyValue kindValue = node.properties().get(new PropertyName("shapeKind"));
        String kind = kindValue instanceof PropertyValue.StringValue string
                ? string.value() : null;
        for (PropertyName name : node.properties().keySet()) {
            AppBarWidgetPropertySchema.Definition binding =
                    AppBarWidgetPropertySchema.find(name).orElse(null);
            if (binding == null
                    || binding.target() != AppBarWidgetPropertySchema.Target.SHAPE) {
                continue;
            }
            if (kind == null) {
                issues.add(issue(
                        PROPERTY_DEPENDENCY,
                        propertiesPath + '/' + name.value(),
                        node.id(),
                        "AppBar shape property '" + name.value()
                        + "' requires 'shapeKind'."));
                continue;
            }
            if (name.value().startsWith("shapeRadius")
                    && !kind.equals("roundedRectangle")
                    && !kind.equals("beveledRectangle")
                    && !kind.equals("continuousRectangle")) {
                issues.add(issue(
                        PROPERTY_CONFLICT,
                        propertiesPath + '/' + name.value(),
                        node.id(),
                        "AppBar shape property '" + name.value()
                        + "' is only valid for roundedRectangle, beveledRectangle, "
                        + "or continuousRectangle shapeKind."));
            }
            if (name.value().equals("shapeCircleEccentricity")
                    && !kind.equals("circle")) {
                issues.add(issue(
                        PROPERTY_CONFLICT,
                        propertiesPath + '/' + name.value(),
                        node.id(),
                        "AppBar shapeCircleEccentricity is only valid for circle shapeKind."));
            }
        }
    }

    private static void validateElevatedButtonState(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String prefix) {
        String owner = "ElevatedButton " + prefix + " state";
        validateElevatedButtonFontPackage(
                node, propertiesPath, issues, prefix, owner);
        validateMutuallyExclusiveProperties(
                node, propertiesPath, issues,
                prefix + "TextBackgroundColor",
                prefix + "TextBackground",
                owner + " TextStyle");
    }

    private static void validateElevatedButtonEffectiveDimensions(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        HashSet<String> reported = new HashSet<>();
        for (String dimension : List.of("Width", "Height")) {
            validateEffectiveDimension(
                    node, propertiesPath, issues, dimension,
                    Set.of(), false, reported);
            validateEffectiveDimension(
                    node, propertiesPath, issues, dimension,
                    Set.of(), true, reported);
            List<String> states = List.of("Focused", "Hovered", "Pressed");
            for (int mask = 1; mask < (1 << states.size()); mask++) {
                HashSet<String> active = new HashSet<>();
                for (int index = 0; index < states.size(); index++) {
                    if ((mask & (1 << index)) != 0) {
                        active.add(states.get(index));
                    }
                }
                validateEffectiveDimension(
                        node, propertiesPath, issues, dimension,
                        Set.copyOf(active), false, reported);
            }
        }
    }

    private static void validateEffectiveDimension(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String dimension,
            Set<String> activeStates,
            boolean disabled,
            Set<String> reported) {
        EffectiveNumber minimum = effectiveElevatedNumber(
                node, "Minimum" + dimension, activeStates, disabled);
        EffectiveNumber maximum = effectiveElevatedNumber(
                node, "Maximum" + dimension, activeStates, disabled);
        if (minimum == null || maximum == null
                || minimum.value().compareTo(maximum.value()) <= 0) {
            return;
        }
        String key = minimum.propertyName() + '|' + maximum.propertyName();
        if (!reported.add(key)) {
            return;
        }
        String stateDescription = disabled
                ? "disabled"
                : activeStates.isEmpty()
                        ? "enabled/default"
                        : activeStates.stream().sorted().collect(
                                Collectors.joining(" + "));
        issues.add(issue(
                PROPERTY_CONFLICT,
                propertiesPath + '/' + maximum.propertyName(),
                node.id(),
                "ElevatedButton effective " + stateDescription + ' '
                + dimension.toLowerCase()
                + " resolves '" + minimum.propertyName() + "' above '"
                + maximum.propertyName() + "'. Minimum size must be less than "
                + "or equal to maximum size."));
    }

    private static EffectiveNumber effectiveElevatedNumber(
            WidgetNode node,
            String suffix,
            Set<String> activeStates,
            boolean disabled) {
        if (disabled) {
            EffectiveNumber value = elevatedNumber(
                    node, "styleDisabled" + suffix);
            return value != null ? value : elevatedFrameworkMinimum(suffix);
        }
        for (String state : List.of("Pressed", "Hovered", "Focused")) {
            if (activeStates.contains(state)) {
                EffectiveNumber value = elevatedNumber(
                        node, "style" + state + suffix);
                if (value != null) {
                    return value;
                }
            }
        }
        EffectiveNumber value = elevatedNumber(node, "style" + suffix);
        return value != null ? value : elevatedFrameworkMinimum(suffix);
    }

    private static EffectiveNumber elevatedFrameworkMinimum(String suffix) {
        // Validation has no BuildContext, so the deterministic floor is the
        // pinned generated-project Material 3 default. A runtime-supplied
        // ElevatedButtonTheme may replace this value; Dart generation still
        // resolves that external theme exactly before framework defaults.
        return switch (suffix) {
            case "MinimumWidth" -> new EffectiveNumber(
                    "Flutter M3 framework minimumWidth", BigDecimal.valueOf(64));
            case "MinimumHeight" -> new EffectiveNumber(
                    "Flutter M3 framework minimumHeight", BigDecimal.valueOf(40));
            default -> null;
        };
    }

    private static EffectiveNumber elevatedNumber(
            WidgetNode node,
            String propertyName) {
        BigDecimal value = numericValue(node, propertyName);
        return value == null ? null : new EffectiveNumber(propertyName, value);
    }

    private static void validateElevatedButtonFontPackage(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String prefix,
            String owner) {
        String packageName = prefix + "TextPackage";
        if (!node.properties().containsKey(new PropertyName(packageName))) {
            return;
        }
        boolean localFamily = hasNonBlankString(
                node, prefix + "TextFontFamily")
                || hasNonBlankLine(node, prefix + "TextFontFamilyFallback");
        boolean mayUseBase = !prefix.equals("style")
                && !prefix.equals("styleDisabled")
                && !Boolean.FALSE.equals(booleanValue(
                        node, prefix + "TextInherit"));
        boolean baseFamily = mayUseBase
                && (hasNonBlankString(node, "styleTextFontFamily")
                    || hasNonBlankLine(node, "styleTextFontFamilyFallback"));
        if (!localFamily && !baseFamily) {
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + '/' + packageName,
                    node.id(),
                    owner + " property '" + packageName
                    + "' requires an effective fontFamily or fontFamilyFallback"
                    + (mayUseBase ? " from this state or enabled/default style." : ".")));
        }
    }

    private static void validateElevatedButtonTextInherit(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        List<String> prefixes = List.of(
                "style", "styleDisabled", "styleFocused",
                "styleHovered", "stylePressed");
        boolean guardedFeaturePresent = prefixes.stream().anyMatch(prefix ->
                node.properties().containsKey(new PropertyName(prefix + "TextInherit"))
                || node.properties().containsKey(new PropertyName(prefix + "TextTheme")));
        if (!guardedFeaturePresent) {
            return;
        }

        Boolean base = booleanValue(node, "styleTextInherit");
        Boolean disabled = booleanValue(node, "styleDisabledTextInherit");
        if (base == null) {
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + "/styleTextInherit",
                    node.id(),
                    "ElevatedButton stateful TextStyle inherit/theme overrides require "
                    + "an explicit enabled/default styleTextInherit value."));
        }
        if (disabled == null) {
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + "/styleDisabledTextInherit",
                    node.id(),
                    "ElevatedButton stateful TextStyle inherit/theme overrides require "
                    + "an explicit disabled TextInherit value so animated state "
                    + "transitions remain safe."));
        } else if (base != null && !disabled.equals(base)) {
            issues.add(issue(
                    PROPERTY_CONFLICT,
                    propertiesPath + "/styleDisabledTextInherit",
                    node.id(),
                    "ElevatedButton reachable TextStyle values must use one inherit "
                    + "value; disabled differs from enabled/default."));
        }

        for (String prefix : prefixes) {
            boolean themePresent = node.properties().containsKey(
                    new PropertyName(prefix + "TextTheme"));
            Boolean value = booleanValue(node, prefix + "TextInherit");
            if (themePresent && value == null) {
                issues.add(issue(
                        PROPERTY_DEPENDENCY,
                        propertiesPath + '/' + prefix + "TextInherit",
                        node.id(),
                        "ElevatedButton property '" + prefix
                        + "TextTheme' requires an explicit same-state TextInherit "
                        + "value for transition-safe TextStyle resolution."));
            } else if (!prefix.equals("style")
                    && !prefix.equals("styleDisabled")
                    && value != null && base != null && !value.equals(base)) {
                issues.add(issue(
                        PROPERTY_CONFLICT,
                        propertiesPath + '/' + prefix + "TextInherit",
                        node.id(),
                        "ElevatedButton reachable TextStyle values must use the "
                        + "enabled/default inherit value " + base + "."));
            }
        }
    }

    private static Boolean booleanValue(
            WidgetNode node,
            String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        return value instanceof PropertyValue.BooleanValue flag
                ? flag.value() : null;
    }

    private static BigInteger integerValue(
            WidgetNode node,
            String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        return value instanceof PropertyValue.IntegerValue integer
                ? integer.value() : null;
    }

    private static void validateElevatedButtonEffectiveShapes(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        HashSet<String> reported = new HashSet<>();
        validateEffectiveShape(
                node, propertiesPath, issues, Set.of(), true, reported);
        List<String> states = List.of("Focused", "Hovered", "Pressed");
        for (int mask = 0; mask < (1 << states.size()); mask++) {
            HashSet<String> active = new HashSet<>();
            for (int index = 0; index < states.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    active.add(states.get(index));
                }
            }
            validateEffectiveShape(
                    node, propertiesPath, issues, Set.copyOf(active),
                    false, reported);
        }
    }

    private static void validateEffectiveShape(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            Set<String> activeStates,
            boolean disabled,
            Set<String> reported) {
        EffectiveShapeValue kind = effectiveElevatedShapeValue(
                node, "ShapeKind", activeStates, disabled);
        String stateDescription = disabled
                ? "disabled"
                : activeStates.isEmpty()
                        ? "enabled/default"
                        : activeStates.stream().sorted().collect(
                                Collectors.joining(" + "));
        for (String suffix : List.of(
                "ShapeRadiusTopLeft", "ShapeRadiusTopRight",
                "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft")) {
            EffectiveShapeValue radius = effectiveElevatedShapeValue(
                    node, suffix, activeStates, disabled);
            if (radius == null) {
                continue;
            }
            if (kind == null) {
                addEffectiveShapeIssue(
                        node, propertiesPath, issues, reported,
                        PROPERTY_DEPENDENCY, radius.propertyName(),
                        "ElevatedButton effective " + stateDescription
                        + " corner radius requires a locally configured effective "
                        + "shape kind.");
            } else if (!(kind.value() instanceof PropertyValue.StringValue name)
                    || !Set.of(
                            "roundedRectangle", "roundedSuperellipse",
                            "beveledRectangle", "continuousRectangle")
                            .contains(name.value())) {
                addEffectiveShapeIssue(
                        node, propertiesPath, issues, reported,
                        PROPERTY_CONFLICT, radius.propertyName(),
                        "ElevatedButton effective " + stateDescription
                        + " corner radius is incompatible with '"
                        + shapeKindName(kind) + "' from '"
                        + kind.propertyName() + "'.");
            }
        }
        EffectiveShapeValue eccentricity = effectiveElevatedShapeValue(
                node, "ShapeCircleEccentricity", activeStates, disabled);
        if (eccentricity == null) {
            return;
        }
        if (kind == null) {
            addEffectiveShapeIssue(
                    node, propertiesPath, issues, reported,
                    PROPERTY_DEPENDENCY, eccentricity.propertyName(),
                    "ElevatedButton effective " + stateDescription
                    + " circle eccentricity requires a locally configured "
                    + "effective shape kind.");
        } else if (!(kind.value() instanceof PropertyValue.StringValue name)
                || !name.value().equals("circle")) {
            addEffectiveShapeIssue(
                    node, propertiesPath, issues, reported,
                    PROPERTY_CONFLICT, eccentricity.propertyName(),
                    "ElevatedButton effective " + stateDescription
                    + " circle eccentricity is incompatible with '"
                    + shapeKindName(kind) + "' from '"
                    + kind.propertyName() + "'.");
        }
    }

    private static EffectiveShapeValue effectiveElevatedShapeValue(
            WidgetNode node,
            String suffix,
            Set<String> activeStates,
            boolean disabled) {
        if (disabled) {
            return elevatedShapeValue(node, "styleDisabled" + suffix);
        }
        for (String state : List.of("Pressed", "Hovered", "Focused")) {
            if (activeStates.contains(state)) {
                EffectiveShapeValue value = elevatedShapeValue(
                        node, "style" + state + suffix);
                if (value != null) {
                    return value;
                }
            }
        }
        return elevatedShapeValue(node, "style" + suffix);
    }

    private static EffectiveShapeValue elevatedShapeValue(
            WidgetNode node,
            String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        return value == null ? null : new EffectiveShapeValue(propertyName, value);
    }

    private static String shapeKindName(EffectiveShapeValue kind) {
        return kind.value() instanceof PropertyValue.StringValue value
                ? value.value() : kind.value().kind().name();
    }

    private static void addEffectiveShapeIssue(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            Set<String> reported,
            String code,
            String propertyName,
            String message) {
        if (reported.add(code + '|' + propertyName)) {
            issues.add(issue(
                    code, propertiesPath + '/' + propertyName,
                    node.id(), message));
        }
    }

    private static void validateElevatedButtonAlignment(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        List<String> names = List.of(
                "styleAlignmentKind", "styleAlignmentX", "styleAlignmentY");
        long present = names.stream()
                .filter(name -> node.properties().containsKey(new PropertyName(name)))
                .count();
        if (present > 0 && present < names.size()) {
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + "/styleAlignmentKind",
                    node.id(),
                    "ElevatedButton ButtonStyle alignment requires "
                    + "styleAlignmentKind, styleAlignmentX, and styleAlignmentY together."));
        }
    }

    private static BigDecimal numericValue(
            WidgetNode node,
            String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        if (value instanceof PropertyValue.IntegerValue integer) {
            return new BigDecimal(integer.value());
        }
        if (value instanceof PropertyValue.DoubleValue decimal) {
            return decimal.value();
        }
        return null;
    }

    private static boolean hasNonBlankString(WidgetNode node, String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        return value instanceof PropertyValue.StringValue string
                && !string.value().isBlank();
    }

    private static boolean hasNonBlankLine(WidgetNode node, String propertyName) {
        PropertyValue value = node.properties().get(new PropertyName(propertyName));
        return value instanceof PropertyValue.StringValue string
                && string.value().lines().anyMatch(line -> !line.isBlank());
    }

    private static void validatePropertyValue(
            WidgetNode node,
            PropertyDefinition property,
            PropertyValue value,
            String propertiesPath,
            IssueCollector issues) {
        String propertyPath = propertiesPath + "/" + pointer(property.name().value());
        Optional<PropertyValueConstraint> constraint = property.constraints().stream()
                .filter(candidate -> candidate.kind() == value.kind())
                .findFirst();
        if (constraint.isEmpty()) {
            String expected = property.constraints().stream()
                    .map(candidate -> candidate.kind().wireName())
                    .collect(Collectors.joining(", "));
            issues.add(issue(
                    PROPERTY_KIND,
                    propertyPath,
                    node.id(),
                    "Property '" + property.name().value() + "' on widget '"
                    + node.type().value() + "' has kind '" + value.kind().wireName()
                    + "'; accepted kinds are: " + expected + "."));
            return;
        }

        PropertyValueConstraint accepted = constraint.orElseThrow();
        if (!accepted.accepts(value)) {
            issues.add(issue(
                    PROPERTY_CONSTRAINT,
                    propertyPath,
                    node.id(),
                    "Property '" + property.name().value() + "' on widget '"
                    + node.type().value() + "' does not satisfy catalog constraint '"
                    + accepted.description() + "'."));
        }
    }

    private List<NodeFrame> validateSlotsAndCollectChildren(
            WidgetNode node,
            WidgetDefinition definition,
            String nodePath,
            int depth,
            IssueCollector issues,
            TraversalFrontier frontier) {
        String slotsPath = nodePath + "/slots";
        if (node.slots().size() > limits.maxSlotsPerWidget()) {
            issues.add(issue(
                    SLOT_LIMIT,
                    slotsPath,
                    node.id(),
                    "Widget '" + node.type().value() + "' declares " + node.slots().size()
                    + " slots, exceeding the limit of " + limits.maxSlotsPerWidget() + "."));
        }

        List<NodeFrame> children = new ArrayList<>();
        if (issues.truncated()) {
            return children;
        }
        int inspected = 0;
        if (definition != null) {
            for (SlotDefinition slot : definition.slots()) {
                WidgetSlot value = node.slots().get(slot.name());
                if (value == null) {
                    if (slot.parameter().required()) {
                        issues.add(issue(
                                MISSING_SLOT,
                                slotsPath + "/" + pointer(slot.name().value()),
                                node.id(),
                                "Widget '" + node.type().value() + "' at '" + nodePath
                                + "' is missing required slot '" + slot.name().value() + "'."));
                    }
                    if (issues.truncated()) {
                        return children;
                    }
                    continue;
                }
                if (inspected++ < limits.maxSlotsPerWidget()) {
                    inspectSlot(
                            node,
                            definition,
                            value,
                            slot,
                            slotsPath,
                            depth,
                            children,
                            issues,
                            frontier);
                }
                if (issues.truncated() || frontier.limitReached()) {
                    return children;
                }
            }
        }

        int remainingCapacity = Math.max(0, limits.maxSlotsPerWidget() - inspected);
        List<SlotName> remaining = smallestMatching(
                node.slots().keySet(),
                name -> definition == null || definition.slot(name).isEmpty(),
                SLOT_NAME_ORDER,
                remainingCapacity);
        for (SlotName name : remaining) {
            if (inspected++ >= limits.maxSlotsPerWidget()) {
                break;
            }
            WidgetSlot value = node.slots().get(name);
            String slotPath = slotsPath + "/" + pointer(name.value());
            if (definition != null) {
                issues.add(issue(
                        UNKNOWN_SLOT,
                        slotPath,
                        node.id(),
                        "Widget '" + node.type().value() + "' at '" + nodePath
                        + "' declares unknown slot '" + name.value() + "'."));
            }
            collectChildren(
                    value,
                    definition,
                    null,
                    slotPath,
                    depth,
                    children,
                    issues,
                    frontier);
            if (issues.truncated() || frontier.limitReached()) {
                return children;
            }
        }
        return children;
    }

    private static void inspectSlot(
            WidgetNode owner,
            WidgetDefinition ownerDefinition,
            WidgetSlot value,
            SlotDefinition slotDefinition,
            String slotsPath,
            int depth,
            List<NodeFrame> children,
            IssueCollector issues,
            TraversalFrontier frontier) {
        String slotPath = slotsPath + "/" + pointer(slotDefinition.name().value());
        if (value.cardinality() != slotDefinition.cardinality()) {
            issues.add(issue(
                    SLOT_KIND,
                    slotPath,
                    owner.id(),
                    "Slot '" + slotDefinition.name().value() + "' on widget '"
                    + owner.type().value() + "' has kind '" + value.cardinality().wireName()
                    + "'; expected '" + slotDefinition.cardinality().wireName() + "'."));
        }
        if (issues.truncated()) {
            return;
        }

        int childCount = childCount(value);
        if (value instanceof WidgetSlot.SingleSlot single
                && single.child().isEmpty()
                && slotDefinition.minChildren() > 0) {
            issues.add(issue(
                    SLOT_NULL,
                    slotPath + "/child",
                    owner.id(),
                    "Slot '" + slotDefinition.name().value() + "' on widget '"
                    + owner.type().value() + "' cannot contain an explicit null child."));
        } else if (childCount < slotDefinition.minChildren()
                || childCount > slotDefinition.maxChildren()) {
            issues.add(issue(
                    SLOT_CARDINALITY,
                    slotPath,
                    owner.id(),
                    "Slot '" + slotDefinition.name().value() + "' on widget '"
                    + owner.type().value() + "' contains " + childCount
                    + " children; expected between " + slotDefinition.minChildren()
                    + " and " + slotDefinition.maxChildren() + "."));
        }
        if (issues.truncated()) {
            return;
        }
        collectChildren(
                value,
                ownerDefinition,
                slotDefinition,
                slotPath,
                depth,
                children,
                issues,
                frontier);
    }

    private static void collectChildren(
            WidgetSlot value,
            WidgetDefinition parentDefinition,
            SlotDefinition parentSlot,
            String slotPath,
            int depth,
            List<NodeFrame> children,
            IssueCollector issues,
            TraversalFrontier frontier) {
        if (value instanceof WidgetSlot.SingleSlot single) {
            if (single.child().isPresent()) {
                WidgetNode child = single.child().orElseThrow();
                frontier.discover(
                        new NodeFrame(
                                child,
                                slotPath + "/child",
                                depth + 1,
                                parentDefinition,
                                parentSlot),
                        children,
                        issues);
            }
        } else if (value instanceof WidgetSlot.ListSlot list) {
            for (int index = 0; index < list.children().size(); index++) {
                if (!frontier.discover(new NodeFrame(
                        list.children().get(index),
                        slotPath + "/children/" + index,
                        depth + 1,
                        parentDefinition,
                        parentSlot), children, issues)) {
                    return;
                }
            }
        }
    }

    private static <T> List<T> smallestMatching(
            Iterable<T> values,
            Predicate<T> included,
            Comparator<T> order,
            int limit) {
        if (limit == 0) {
            return List.of();
        }
        PriorityQueue<T> selected = new PriorityQueue<>(limit, order.reversed());
        for (T value : values) {
            if (!included.test(value)) {
                continue;
            }
            if (selected.size() < limit) {
                selected.add(value);
            } else if (order.compare(value, selected.peek()) < 0) {
                selected.remove();
                selected.add(value);
            }
        }
        ArrayList<T> result = new ArrayList<>(selected);
        result.sort(order);
        return List.copyOf(result);
    }

    private static int childCount(WidgetSlot value) {
        if (value instanceof WidgetSlot.SingleSlot single) {
            return single.child().isPresent() ? 1 : 0;
        }
        return ((WidgetSlot.ListSlot) value).children().size();
    }

    private static ValidationIssue issue(
            String code,
            String path,
            StableId widgetId,
            String message) {
        return new ValidationIssue(
                code,
                ValidationSeverity.ERROR,
                path,
                Optional.of(widgetId),
                message);
    }

    private static ValidationIssue warning(
            String code,
            String path,
            StableId widgetId,
            String message) {
        return new ValidationIssue(
                code,
                ValidationSeverity.WARNING,
                path,
                Optional.of(widgetId),
                message);
    }

    private static String pointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private record EffectiveNumber(
            String propertyName,
            BigDecimal value) {
    }

    private record EffectiveShapeValue(
            String propertyName,
            PropertyValue value) {
    }

    private record PositionalArgument(
            int order,
            boolean required,
            boolean present,
            String name,
            String path) {
    }

    private record NodeFrame(
            WidgetNode node,
            String path,
            int depth,
            WidgetDefinition parentDefinition,
            SlotDefinition parentSlot) {
    }

    private static final class TraversalFrontier {
        private final int maximumNodes;
        private int discoveredNodes = 1;
        private boolean limitReached;

        private TraversalFrontier(int maximumNodes) {
            this.maximumNodes = maximumNodes;
        }

        boolean discover(
                NodeFrame frame,
                List<NodeFrame> destination,
                IssueCollector issues) {
            if (limitReached) {
                return false;
            }
            if (discoveredNodes >= maximumNodes) {
                issues.addTerminal(issue(
                        NODE_LIMIT,
                        frame.path(),
                        frame.node().id(),
                        "The widget tree exceeds the limit of " + maximumNodes
                        + " widgets at '" + frame.path() + "'; validation stopped before this widget."));
                limitReached = true;
                return false;
            }
            discoveredNodes++;
            destination.add(frame);
            return true;
        }

        boolean limitReached() {
            return limitReached;
        }
    }

    private static final class IssueCollector {
        private final int maximum;
        private final List<ValidationIssue> issues = new ArrayList<>();
        private boolean truncated;

        private IssueCollector(int maximum) {
            this.maximum = maximum;
        }

        void add(ValidationIssue issue) {
            if (truncated) {
                return;
            }
            if (issues.size() < maximum - 1) {
                issues.add(issue);
                return;
            }
            issues.add(new ValidationIssue(
                    ISSUES_TRUNCATED,
                    ValidationSeverity.ERROR,
                    issue.path(),
                    issue.widgetId(),
                    "Validation stopped after reaching the limit of " + maximum
                    + " reported issues."));
            truncated = true;
        }

        void addTerminal(ValidationIssue issue) {
            if (!truncated && issues.size() < maximum) {
                issues.add(issue);
            }
        }

        boolean truncated() {
            return truncated;
        }

        List<ValidationIssue> snapshot() {
            return List.copyOf(issues);
        }
    }
}
