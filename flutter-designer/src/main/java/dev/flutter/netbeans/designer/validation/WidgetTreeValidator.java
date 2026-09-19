package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.AnimatedDefaultTextStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextStyleWidgetPropertySchema;

import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircleAvatarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.LinearProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircularProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ContainerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuItemButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuAnchorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SubmenuButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.OutlinedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FilledButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipThemeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SliderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RangeSliderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewExtentWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
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
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import dev.flutter.netbeans.designer.state.WidgetStatePropertyBindingCatalog;
import dev.flutter.netbeans.designer.model.StateBinding;
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
    public static final String STATE_BINDING = "designer.widget.stateBinding";

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
        Map<String, StateMemberUse> firstStateMemberPaths = new HashMap<>();
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

            validateStateBinding(document, node, definition.orElse(null), frame.path(),
                    firstStateMemberPaths, issues);
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

    private static void validateStateBinding(
            DesignerDocument document, WidgetNode node, WidgetDefinition definition,
            String nodePath, Map<String, StateMemberUse> firstMemberPaths, IssueCollector issues) {
        if (node.stateBinding().isEmpty() && node.propertyBindings().isEmpty()) return;
        String path = nodePath + "/stateBinding";
        if (document.source().widgetKind() != WidgetClassKind.STATEFUL) {
            issues.add(issue(STATE_BINDING, path, node.id(),
                    "A State binding requires a verified StatefulWidget source owner."));
        }
        WidgetStateBindingCatalog.validationError(node).ifPresent(message ->
                issues.add(issue(STATE_BINDING, path, node.id(), message)));
        WidgetStatePropertyBindingCatalog.validationError(node).ifPresent(message ->
                issues.add(issue(STATE_BINDING, nodePath + "/propertyBindings", node.id(), message)));
        for (var entry : node.propertyBindings().entrySet()) {
            // Retained Focus-only arguments do not use a source member in the external-node constructor.
            // Keep their closed binding validation above, and recheck member consistency when reactivated.
            if (!dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema.propertyAvailable(node, entry.getKey())
                    || !SwitchListTileWidgetPropertySchema.propertyAvailable(node, entry.getKey())
                    || !RadioListTileWidgetPropertySchema.propertyAvailable(node, entry.getKey())) continue;
            var consumer = entry.getValue();
            validateStateMember(consumer.fieldName(), new StateMemberUse(nodePath + "/propertyBindings/"
                    + pointer(entry.getKey().value()), consumer.type(), consumer.referenceType(), false),
                    node.id(), firstMemberPaths, issues);
        }
        if (node.stateBinding().isEmpty()) return;
        var binding = node.stateBinding().orElseThrow();
        validateStateMember(binding.fieldName(), new StateMemberUse(path, binding.type(), binding.referenceType(), false),
                node.id(), firstMemberPaths, issues);
        validateStateMember(binding.handlerName(), new StateMemberUse(path, binding.type(), binding.referenceType(), true),
                node.id(), firstMemberPaths, issues);
        if (definition != null && binding.previousOnChanged().isPresent()) {
            PropertyValue previous = binding.previousOnChanged().orElseThrow();
            PropertyName event = WidgetStateBindingCatalog.find(node).map(WidgetStateBindingCatalog.Descriptor::eventProperty)
                    .orElse(new PropertyName("onChanged"));
            boolean accepted = definition.property(event)
                    .stream().flatMap(property -> property.constraints().stream())
                    .anyMatch(constraint -> constraint.kind() == previous.kind() && constraint.accepts(previous));
            if (!accepted) {
                issues.add(issue(STATE_BINDING, path + "/previousOnChanged", node.id(),
                        "The retained previous event value is not accepted by this widget's callback contract."));
            }
        }
    }

    private record StateMemberUse(String path, StateBinding.Type type,
            Optional<PropertyValue.DartObjectReferenceValue> referenceType, boolean handler) {}

    private static void validateStateMember(String name, StateMemberUse current, StableId node,
            Map<String, StateMemberUse> firstUses, IssueCollector issues) {
        StateMemberUse first = firstUses.putIfAbsent(name, current);
        if (first != null && (first.handler() || current.handler() || first.type() != current.type()
                || !first.referenceType().equals(current.referenceType()))) {
            issues.add(issue(STATE_BINDING, current.path(), node,
                    "State member '" + name + "' conflicts with its retained type or handler use at '" + first.path() + "'."));
        }
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
        for(String family:dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.styleFamilies(node.type())) {
            validateFontPackageDependency(node,propertiesPath,issues,family+"Package",family+"FontFamily",family+"FontFamilyFallback","DataTable "+family);
            validateMutuallyExclusiveProperties(node,propertiesPath,issues,family+"Color",family+"Foreground","DataTable "+family);
            validateMutuallyExclusiveProperties(node,propertiesPath,issues,family+"BackgroundColor",family+"Background","DataTable "+family);
        }
        dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.relationshipError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
        dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.relationshipError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
        dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.relationshipError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
        dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.relationshipError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
        dev.flutter.netbeans.designer.catalog.DataTableGrid.relationshipError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
        dev.flutter.netbeans.designer.catalog.TableGrid.relationshipError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
        dev.flutter.netbeans.designer.catalog.LayoutIdWidgetPropertySchema.duplicateIdError(node)
                .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath.substring(0, propertiesPath.length() - "/properties".length()) + "/slots/children", node.id(), message)));
        if (node.type().equals(dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.TYPE)) {
            dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.relationshipError(node)
                    .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/isComplex", node.id(), message)));
            return;
        }
        if (dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.isFilter(node.type())) {
            dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.relationshipError(node)
                    .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/filter", node.id(), message)));
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.TYPE.value())) {
            dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.relationshipError(node)
                    .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/shader", node.id(), message)));
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE.value())) {
            dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema.notificationTypeError(node)
                    .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/notificationType", node.id(), message)));
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema.FOCUS_TYPE.value())) {
            if (dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema.usesExternalNode(node)
                    && !(node.properties().get(new PropertyName("focusNode")) instanceof PropertyValue.DartObjectReferenceValue)) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/focusNode", node.id(),
                        "Focus.withExternalFocusNode requires a non-null project FocusNode reference; the project owns its lifecycle."));
            }
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE.value())) {
            dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema.gestureConflict(node)
                    .ifPresent(message -> issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
            return;
        }
        if (type.equals("flutter.widgets.ClipPath")) {
            if (node.properties().containsKey(new PropertyName("clipper"))
                    && node.properties().containsKey(new PropertyName("shape"))) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/shape", node.id(),
                        "ClipPath properties 'clipper' and 'shape' select different Flutter APIs "
                        + "and cannot be set together. Unset 'shape' to use ClipPath with "
                        + "CustomClipper<Path>, or unset 'clipper' to use ClipPath.shape "
                        + "with ShapeBorder."));
            }
            return;
        }
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

        dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.conflict(node).ifPresent(message ->
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/findItemIndexCallback", node.id(), message)));

        if (type.equals(ListViewWidgetPropertySchema.LIST_VIEW_TYPE.value())) {
            validateStaticScrollViewSemanticChildCount(
                    node, propertiesPath, "ListView", issues);
            if (node.properties().containsKey(new PropertyName("itemExtent"))
                    && node.properties().get(new PropertyName("itemExtentBuilder")) instanceof PropertyValue.DartObjectReferenceValue) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/itemExtentBuilder", node.id(),
                        "ListView cannot combine itemExtent with an itemExtentBuilder reference, even if the callback may evaluate to null. "
                        + "Reset itemExtent to use the builder, or reset itemExtentBuilder/set it to explicit null to use the fixed extent. No value was cleared."));
            }
            return;
        }

        if (type.equals("flutter.widgets.SliverVisibility")) {
            for (var dependency : List.of(
                    List.of("maintainAnimation", "maintainState"),
                    List.of("maintainSize", "maintainAnimation"),
                    List.of("maintainSemantics", "maintainSize"),
                    List.of("maintainInteractivity", "maintainSize"))) {
                if (Boolean.TRUE.equals(booleanValue(node, dependency.getFirst()))
                        && !Boolean.TRUE.equals(booleanValue(node, dependency.getLast()))) {
                    issues.add(issue(PROPERTY_DEPENDENCY,
                            propertiesPath + '/' + dependency.getFirst(), node.id(),
                            "SliverVisibility " + dependency.getFirst() + "=true requires "
                            + dependency.getLast() + "=true, even when visible=true. Omitted maintenance flags default to false."));
                }
            }
            return;
        }

        if (type.equals("flutter.widgets.Visibility")) {
            for (var dependency : List.of(
                    List.of("maintainAnimation", "maintainState"),
                    List.of("maintainSize", "maintainAnimation"),
                    List.of("maintainSemantics", "maintainSize"),
                    List.of("maintainInteractivity", "maintainSize"),
                    List.of("maintainFocusability", "maintainState"))) {
                if (Boolean.TRUE.equals(booleanValue(node, dependency.getFirst()))
                        && !Boolean.TRUE.equals(booleanValue(node, dependency.getLast()))) {
                    issues.add(issue(PROPERTY_DEPENDENCY,
                            propertiesPath + '/' + dependency.getFirst(), node.id(),
                            "Visibility " + dependency.getFirst() + "=true requires "
                            + dependency.getLast() + "=true, even when visible=true. "
                            + "Omitted maintenance flags default to false."));
                }
            }
            return;
        }

        if (type.equals(GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value())) {
            validateStaticScrollViewSemanticChildCount(
                    node, propertiesPath, "GridView.count", issues);
            return;
        }

        if (type.equals(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE.value())) {
            validateStaticScrollViewSemanticChildCount(
                    node, propertiesPath, "GridView.extent", issues);
            return;
        }

        if (type.equals(IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.value())) {
            validateIndexedStackIndex(node, propertiesPath, issues);
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

        if (dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.TYPE.equals(node.type())) {
            dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.conflict(node).ifPresent(message ->
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
            return;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(node.type())) {
            dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.conflict(node).ifPresent(message ->
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath, node.id(), message)));
            return;
        }
        if (type.equals("flutter.widgets.AnimatedContainer")) {
            PropertyValue color = node.properties().get(new PropertyName("color"));
            PropertyValue decoration = node.properties().get(new PropertyName("decoration"));
            boolean hasColor = color != null && !(color instanceof PropertyValue.NullValue);
            boolean hasDecoration = decoration != null && !(decoration instanceof PropertyValue.NullValue);
            if (hasColor && hasDecoration) issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/decoration", node.id(),
                    "AnimatedContainer background: non-null color and decoration are mutually exclusive."));
            PropertyValue clip = node.properties().get(new PropertyName("clipBehavior"));
            if (clip instanceof PropertyValue.EnumValue value && !value.value().equals("none") && !hasColor && !hasDecoration)
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/clipBehavior", node.id(),
                        "AnimatedContainer clipping requires non-null background color or decoration."));
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.TYPE.value())) {
            if (dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.conflictingAlignment(node))
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/alignment", node.id(),
                    "SizeTransition Alignment and deprecated Axis alignment cannot both be non-null. Reset one or use explicit null; source references are treated as potentially non-null."));
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

        if (node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)) {
            if (node.properties().containsKey(new PropertyName("style"))) {
                for (String name : MenuAnchorWidgetPropertySchema.localStyleProperties()) {
                    if (node.properties().containsKey(new PropertyName(name))) issues.add(issue(PROPERTY_CONFLICT,
                            propertiesPath + '/' + name, node.id(),
                            "MenuAnchor whole MenuStyle (including null) is exclusive with every local style leaf. Reset one branch first."));
                }
            }
            validateElevatedButtonEffectiveDimensions(node, propertiesPath, issues);
            validateElevatedButtonEffectiveShapes(node, propertiesPath, issues);
            validateElevatedButtonAlignment(node, propertiesPath, issues);
            return;
        }
        if (node.type().equals(MenuBarWidgetPropertySchema.MENU_BAR_TYPE)) {
            if (node.properties().containsKey(new PropertyName("style"))) {
                for (String name : MenuBarWidgetPropertySchema.localStyleProperties()) {
                    if (node.properties().containsKey(new PropertyName(name))) issues.add(issue(PROPERTY_CONFLICT,
                            propertiesPath + '/' + name, node.id(),
                            "MenuBar whole MenuStyle (including null) is exclusive with every local style leaf. Reset one branch first."));
                }
            }
            validateElevatedButtonEffectiveDimensions(node, propertiesPath, issues);
            validateElevatedButtonEffectiveShapes(node, propertiesPath, issues);
            validateElevatedButtonAlignment(node, propertiesPath, issues);
            return;
        }
        if (type.equals(ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE.value())
                || OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)) {
            if (node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE)) {
                validateSubmenuButtonBranches(node, propertiesPath, issues);
            }
            if (OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)) {
                validateTextButtonBranches(node, propertiesPath, issues);
            }
            for (String prefix : buttonStatePrefixes(node)) {
                validateElevatedButtonState(node, propertiesPath, issues, prefix);
            }
            validateElevatedButtonEffectiveDimensions(node, propertiesPath, issues);
            validateElevatedButtonEffectiveShapes(node, propertiesPath, issues);
            validateElevatedButtonTextInherit(node, propertiesPath, issues);
            validateElevatedButtonAlignment(node, propertiesPath, issues);
            return;
        }

        if (type.equals(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE.value())) {
            RangeSliderWidgetPropertySchema.rangeError(node).ifPresent(message ->
                    issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/valuesStart", node.id(), message)));
            for (String family : List.of("labels", "overlayColor", "mouseCursor")) {
                List<String> locals = switch (family) {
                    case "labels" -> RangeSliderWidgetPropertySchema.labelProperties();
                    case "overlayColor" -> RangeSliderWidgetPropertySchema.overlayColorStateProperties();
                    default -> RangeSliderWidgetPropertySchema.mouseCursorStateProperties();
                };
                if (node.properties().containsKey(new PropertyName(family))) {
                    for (String name : locals) {
                        if (node.properties().containsKey(new PropertyName(name))) {
                            issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/" + name, node.id(),
                                    "RangeSlider " + family + " whole value and local entries are mutually exclusive."));
                        }
                    }
                }
            }
            return;
        }
        if (type.equals(SliderWidgetPropertySchema.SLIDER_TYPE.value())) {
            SliderWidgetPropertySchema.rangeError(node).ifPresent(message ->
                    issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/value", node.id(), message)));
            if (new PropertyValue.StringValue("adaptive").equals(node.properties().get(new PropertyName("variant")))
                    && node.properties().containsKey(new PropertyName("padding"))) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/padding", node.id(),
                        "Slider.adaptive has no Padding argument. Select Standard or reset Padding."));
            }
            if (node.properties().containsKey(new PropertyName("overlayColor"))) {
                for (String name : SliderWidgetPropertySchema.overlayColorStateProperties()) {
                    if (node.properties().containsKey(new PropertyName(name))) {
                        issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/" + name, node.id(),
                                "Slider Overlay color reference and local state entries are mutually exclusive."));
                    }
                }
            }
            return;
        }
        if (type.equals(SwitchWidgetPropertySchema.SWITCH_TYPE.value())) {
            validateSwitch(node, propertiesPath, issues);
            return;
        }
        if (type.equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE.value())) {
            validateSwitch(node, propertiesPath, issues);
            validateCardShape(node, propertiesPath, issues, "shape", "SwitchListTile shape");
            if (SwitchListTileWidgetPropertySchema.requiresSubtitle(node)
                    && (!(node.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty())) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/isThreeLine", node.id(),
                        "SwitchListTile explicit Three line true requires a nonempty Subtitle slot. No child is fabricated."));
            }
            validateListTileWholeLocal(node, propertiesPath, issues, "visualDensity", List.of("visualDensityHorizontal", "visualDensityVertical"));
            List<String> cursors = SwitchListTileWidgetPropertySchema.mouseCursorStateProperties();
            validateListTileWholeLocal(node, propertiesPath, issues, "mouseCursor", cursors);
            if (cursors.stream().anyMatch(name -> node.properties().containsKey(new PropertyName(name)))
                    && !node.properties().containsKey(new PropertyName("mouseCursorDefault"))) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/mouseCursorDefault", node.id(),
                        "SwitchListTile local cursor map requires an explicit non-null Default; no cursor is invented."));
            }
            return;
        }
        if (type.equals(RadioWidgetPropertySchema.RADIO_TYPE.value())) {
            validateRadio(node, propertiesPath, issues);
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE.value())) {
            for (String name : dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE)
                validateMutuallyExclusiveProperties(node, propertiesPath, issues, "animationStyle", name, "SliverFloatingHeader animation style");
            return;
        }
        if (type.equals(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE.value())) {
            for (String family : ExpansionTileWidgetPropertySchema.shapeFamilies()) validateCardShape(node, propertiesPath, issues, family, "ExpansionTile " + family);
            for (String name : List.of("visualDensityHorizontal", "visualDensityVertical")) validateMutuallyExclusiveProperties(node, propertiesPath, issues, "visualDensity", name, "ExpansionTile density");
            for (String name : ExpansionTileWidgetPropertySchema.animationStyleLocalProperties()) validateMutuallyExclusiveProperties(node, propertiesPath, issues, "expansionAnimationStyle", name, "ExpansionTile animation style");
            return;
        }
        if (type.equals(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE.value())) {
            if (node.properties().containsKey(new PropertyName("data"))) {
                for (String name : TooltipThemeWidgetPropertySchema.localProperties()) {
                    PropertyName key = new PropertyName(name);
                    if (node.properties().containsKey(key) || node.propertyBindings().containsKey(key)) {
                        String conflictPath = node.propertyBindings().containsKey(key)
                                ? propertiesPath.substring(0, propertiesPath.length() - "/properties".length()) + "/propertyBindings/" + name
                                : propertiesPath + "/" + name;
                        issues.add(issue(PROPERTY_CONFLICT, conflictPath, node.id(),
                                "TooltipTheme whole Data and local '" + name + "' are mutually exclusive, including explicit null and State bindings. Reset Data to use local fields, or explicitly remove all local fields before setting Data; no values are silently cleared."));
                    }
                }
            }
            if (TooltipWidgetPropertySchema.isNonNull(node, "height") && TooltipWidgetPropertySchema.isNonNull(node, "constraints")) issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/constraints", node.id(),
                    "TooltipThemeData Height and Constraints cannot both be non-null. Reset one field or set it to explicit null."));
            for (String name : TooltipThemeWidgetPropertySchema.textStyleProperties()) validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyle", name, "TooltipThemeData textStyle");
            validateFontPackageDependency(node, propertiesPath, issues, "textStylePackage", "textStyleFontFamily", "textStyleFontFamilyFallback", "TooltipThemeData textStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyleColor", "textStyleForeground", "TooltipThemeData textStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyleBackgroundColor", "textStyleBackground", "TooltipThemeData textStyle");
            return;
        }
        if (type.equals(TooltipWidgetPropertySchema.TOOLTIP_TYPE.value())) {
            boolean message = TooltipWidgetPropertySchema.isNonNull(node, "message");
            boolean rich = TooltipWidgetPropertySchema.isNonNull(node, "richMessage");
            if (message == rich) issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/richMessage", node.id(),
                    "Tooltip requires exactly one non-null Message or Rich message. Change both content fields atomically; clearing the last content or supplying both is not allowed."));
            if (TooltipWidgetPropertySchema.isNonNull(node, "height") && TooltipWidgetPropertySchema.isNonNull(node, "constraints")) issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/constraints", node.id(),
                    "Tooltip Height and Constraints cannot both be non-null. Reset one field or set it to explicit null."));
            for (String name : TooltipWidgetPropertySchema.textStyleProperties()) validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyle", name, "Tooltip textStyle");
            validateFontPackageDependency(node, propertiesPath, issues, "textStylePackage", "textStyleFontFamily", "textStyleFontFamilyFallback", "Tooltip textStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyleColor", "textStyleForeground", "Tooltip textStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyleBackgroundColor", "textStyleBackground", "Tooltip textStyle");
            return;
        }
        if (type.equals(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.value())) {
            RadioListTileWidgetPropertySchema.valueTypeError(node).ifPresent(message ->
                    issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/valueType", node.id(), message)));
            validateCardShape(node, propertiesPath, issues, "shape", "RadioListTile shape");
            if (RadioListTileWidgetPropertySchema.requiresSubtitle(node)
                    && (!(node.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty())) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/isThreeLine", node.id(), "RadioListTile explicit Three line true requires a nonempty Subtitle slot."));
            }
            for (String family : RadioListTileWidgetPropertySchema.colorFamilies()) validateListTileWholeLocal(node, propertiesPath, issues, family, RadioListTileWidgetPropertySchema.colorStateProperties(family));
            validateListTileWholeLocal(node, propertiesPath, issues, "radioInnerRadius", RadioListTileWidgetPropertySchema.innerRadiusStateProperties());
            validateListTileWholeLocal(node, propertiesPath, issues, "radioSide", RadioListTileWidgetPropertySchema.sideLocalProperties());
            validateListTileWholeLocal(node, propertiesPath, issues, "visualDensity", List.of("visualDensityHorizontal", "visualDensityVertical"));
            List<String> cursors = RadioListTileWidgetPropertySchema.mouseCursorStateProperties();
            validateListTileWholeLocal(node, propertiesPath, issues, "mouseCursor", cursors);
            if (cursors.stream().anyMatch(name -> node.properties().containsKey(new PropertyName(name)))
                    && !node.properties().containsKey(new PropertyName("mouseCursorDefault"))) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/mouseCursorDefault", node.id(), "RadioListTile local cursor map requires an explicit non-null Default."));
            }
            validateStatefulRadioOrCheckboxSide(node, propertiesPath, issues, "RadioListTile", "radioSide");
            return;
        }
        if (dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(node.type())) {
            dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.valueTypeError(node).ifPresent(message ->
                    issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/valueType", node.id(), message)));
            return;
        }
        if (dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(node.type())) {
            dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.valueTypeError(node).ifPresent(message ->
                    issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/valueType", node.id(), message)));
            return;
        }
        if (type.equals(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.value())) {
            RadioGroupWidgetPropertySchema.valueTypeError(node).ifPresent(message ->
                    issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/valueType", node.id(), message)));
            return;
        }
        if (type.equals(ListTileWidgetPropertySchema.LIST_TILE_TYPE.value())) {
            if (ListTileWidgetPropertySchema.requiresSubtitle(node)
                    && (!(node.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty())) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/isThreeLine", node.id(),
                        "ListTile explicit Three line true requires a nonempty Subtitle slot. Add Subtitle first, or reset Three line; no child is fabricated."));
            }
            validateCardShape(node, propertiesPath, issues);
            for (String family : ListTileWidgetPropertySchema.styleFamilies()) {
                validateListTileWholeLocal(node, propertiesPath, issues, family, ListTileWidgetPropertySchema.localTextStyleProperties(family));
                validateFontPackageDependency(node, propertiesPath, issues, family + "Package", family + "FontFamily", family + "FontFamilyFallback", "ListTile " + family);
                validateMutuallyExclusiveProperties(node, propertiesPath, issues, family + "Color", family + "Foreground", "ListTile " + family);
                validateMutuallyExclusiveProperties(node, propertiesPath, issues, family + "BackgroundColor", family + "Background", "ListTile " + family);
            }
            validateListTileWholeLocal(node, propertiesPath, issues, "visualDensity", List.of("visualDensityHorizontal", "visualDensityVertical"));
            for (String family : List.of("iconColor", "textColor", "mouseCursor")) {
                List<String> local = family.equals("mouseCursor") ? ListTileWidgetPropertySchema.mouseCursorStateProperties()
                        : ListTileWidgetPropertySchema.colorStateProperties(family);
                validateListTileWholeLocal(node, propertiesPath, issues, family, local);
                if (local.stream().anyMatch(name -> node.properties().containsKey(new PropertyName(name)))
                        && !node.properties().containsKey(new PropertyName(family + "Default"))) {
                    issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/" + family + "Default", node.id(),
                            "ListTile local " + family + " map requires an explicit non-null Default. Set Default before another state; no color or cursor fallback is invented."));
                }
            }
            return;
        }
        if (type.equals(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE.value())) {
            validateCheckbox(node, propertiesPath, issues, "CheckboxListTile");
            for (String family : CheckboxListTileWidgetPropertySchema.shapeFamilies()) {
                validateCardShape(node, propertiesPath, issues, family, "CheckboxListTile " + family);
            }
            if (CheckboxListTileWidgetPropertySchema.requiresSubtitle(node)
                    && (!(node.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty())) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/isThreeLine", node.id(),
                        "CheckboxListTile explicit Three line true requires a nonempty Subtitle slot. No child is fabricated."));
            }
            validateListTileWholeLocal(node, propertiesPath, issues, "visualDensity", List.of("visualDensityHorizontal", "visualDensityVertical"));
            List<String> cursors = CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties();
            validateListTileWholeLocal(node, propertiesPath, issues, "mouseCursor", cursors);
            if (cursors.stream().anyMatch(name -> node.properties().containsKey(new PropertyName(name)))
                    && !node.properties().containsKey(new PropertyName("mouseCursorDefault"))) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/mouseCursorDefault", node.id(),
                        "CheckboxListTile local cursor map requires an explicit non-null Default; no cursor is invented."));
            }
            return;
        }
        if (type.equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE.value())) {
            validateCheckbox(node, propertiesPath, issues);
            validateCardShape(node, propertiesPath, issues);
            return;
        }
        if (type.equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE.value())) {
            String variant = FloatingActionButtonWidgetPropertySchema.variant(node);
            for (PropertyName property : node.properties().keySet()) {
                if (!FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(property.value(), variant)) {
                    issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/" + property.value(), node.id(),
                            "FloatingActionButton " + variant + " has no " + property.value() + " argument; select its applicable constructor or reset this field."));
                }
            }
            if (FloatingActionButtonWidgetPropertySchema.requiresChild(node)
                    && (!(node.slots().get(new SlotName("child")) instanceof WidgetSlot.SingleSlot child) || child.child().isEmpty())) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/variant", node.id(),
                        "FloatingActionButton.extended requires a non-null Label in Child, even when Extended state is false. Add or replace Child first."));
            }
            if (!FloatingActionButtonWidgetPropertySchema.isExtendedConstructor(node)
                    && node.slots().get(new SlotName("icon")) instanceof WidgetSlot.SingleSlot icon && icon.child().isPresent()) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/variant", node.id(),
                        "FloatingActionButton Icon is available only in Extended. Move or clear the existing Icon before changing constructors."));
            }
            validateCardShape(node, propertiesPath, issues);
            validateFontPackageDependency(node, propertiesPath, issues, "extendedTextStylePackage", "extendedTextStyleFontFamily", "extendedTextStyleFontFamilyFallback", "FloatingActionButton extendedTextStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "extendedTextStyleColor", "extendedTextStyleForeground", "FloatingActionButton extendedTextStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "extendedTextStyleBackgroundColor", "extendedTextStyleBackground", "FloatingActionButton extendedTextStyle");
            return;
        }
        if ((dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.supports(node.type()) || dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.TYPE.equals(node.type()))) {
            validateCardShape(node, propertiesPath, issues);
            for (String family : dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.styleFamilies()) {
                validateListTileWholeLocal(node, propertiesPath, issues, family, dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.localStyleProperties(family));
                validateFontPackageDependency(node, propertiesPath, issues, family+"Package", family+"FontFamily", family+"FontFamilyFallback", node.type().value()+" "+family);
                validateMutuallyExclusiveProperties(node, propertiesPath, issues, family+"Color", family+"Foreground", node.type().value()+" "+family);
                validateMutuallyExclusiveProperties(node, propertiesPath, issues, family+"BackgroundColor", family+"Background", node.type().value()+" "+family);
            }
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.TYPE.value())) {
            String family = "contentTextStyle";
            validateListTileWholeLocal(node, propertiesPath, issues, family, dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.localStyleProperties(family));
            validateFontPackageDependency(node, propertiesPath, issues, family+"Package", family+"FontFamily", family+"FontFamilyFallback", "MaterialBanner contentTextStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, family+"Color", family+"Foreground", "MaterialBanner contentTextStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, family+"BackgroundColor", family+"Background", "MaterialBanner contentTextStyle");
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.ChipWidgetPropertySchema.TYPE.value())) {
            for(var family:dev.flutter.netbeans.designer.catalog.ChipWidgetPropertySchema.families().entrySet())
                validateListTileWholeLocal(node,propertiesPath,issues,family.getKey(),family.getValue());
            validateFontPackageDependency(node,propertiesPath,issues,"labelStylePackage","labelStyleFontFamily","labelStyleFontFamilyFallback","Chip labelStyle");
            validateMutuallyExclusiveProperties(node,propertiesPath,issues,"labelStyleColor","labelStyleForeground","Chip labelStyle");
            validateMutuallyExclusiveProperties(node,propertiesPath,issues,"labelStyleBackgroundColor","labelStyleBackground","Chip labelStyle");
            validateCardShape(node,propertiesPath,issues,"shape","Chip");
            validateStatefulRadioOrCheckboxSide(node,propertiesPath,issues,"Chip");
            if(dev.flutter.netbeans.designer.catalog.ChipWidgetPropertySchema.stateLeaves("mouseCursor").stream().anyMatch(n->node.properties().containsKey(new PropertyName(n)))
                    && !node.properties().containsKey(new PropertyName("mouseCursorDefault")))
                issues.add(issue(PROPERTY_DEPENDENCY,propertiesPath+"/mouseCursorDefault",node.id(),"Chip local cursor states require Default."));
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.TYPE.value())) {
            validateCardShape(node, propertiesPath, issues, "shape", "SnackBar shape");
            if (TooltipWidgetPropertySchema.isNonNull(node, "width") && TooltipWidgetPropertySchema.isNonNull(node, "margin"))
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/margin", node.id(), "SnackBar Width and Margin are mutually exclusive."));
            if ((TooltipWidgetPropertySchema.isNonNull(node, "width") || TooltipWidgetPropertySchema.isNonNull(node, "margin"))
                    && !new PropertyValue.EnumValue("SnackBarBehavior", "floating").equals(node.properties().get(new PropertyName("behavior"))))
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/behavior", node.id(),
                        "SnackBar Width/Margin require explicit floating Behavior; theme values cannot be proven by Designer."));
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.TYPE.value())) {
            validateCardShape(node, propertiesPath, issues, "shape", "BottomSheet shape");
            boolean controller = node.properties().get(new PropertyName("animationController"))
                    instanceof PropertyValue.DartObjectReferenceValue;
            if (!controller && (!new PropertyValue.BooleanValue(false).equals(node.properties().get(new PropertyName("enableDrag")))
                    || !new PropertyValue.BooleanValue(false).equals(node.properties().get(new PropertyName("showDragHandle"))))) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/animationController", node.id(),
                        "BottomSheet: supply AnimationController before enabling dragging or inheriting/showing a drag handle. Without a controller set Enable drag and Show drag handle explicitly false."));
            }
            if (node.properties().get(new PropertyName("builder")) instanceof PropertyValue.DartObjectReferenceValue
                    && node.slots().get(new SlotName("child")) instanceof WidgetSlot.SingleSlot child && child.child().isPresent()) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/builder", node.id(),
                        "BottomSheet project Builder owns its subtree. Clear Child before selecting source, or retain the Child preset."));
            }
            return;
        }
        if (type.equals(dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.TYPE.value())) {
            validateCardShape(node, propertiesPath, issues, "shape", "Dialog shape");
            return;
        }
        if (type.equals(CardWidgetPropertySchema.CARD_TYPE.value())) {
            validateCardShape(node, propertiesPath, issues);
            return;
        }
        if (type.equals(CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE.value())) {
            validateCircleAvatar(node, propertiesPath, issues);
            return;
        }
        if (type.equals(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.value())) {
            if (RefreshIndicatorWidgetPropertySchema.isNoSpinner(node)) {
                for (String name : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties()) {
                    if (node.properties().containsKey(new PropertyName(name))) {
                        issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/" + name, node.id(),
                                "RefreshIndicator.noSpinner has no " + name + " argument. Use Material or Adaptive, or reset this field."));
                    }
                }
            } else if (node.properties().containsKey(new PropertyName("onStatusChange"))) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/onStatusChange", node.id(),
                        "RefreshIndicator On status change requires the No spinner constructor."));
            }
            return;
        }
        if (type.equals(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE.value())) {
            validateMutuallyExclusiveProperties(node, propertiesPath, issues,
                    "value", "controller", "CircularProgressIndicator progress mode");
            if (new PropertyValue.StringValue("adaptive").equals(node.properties().get(new PropertyName("variant")))
                    && node.properties().containsKey(new PropertyName("color"))) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/color", node.id(),
                        "CircularProgressIndicator.adaptive has no Color argument. Use Material or reset Color."));
            }
            return;
        }
        if (type.equals(LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE.value())) {
            validateMutuallyExclusiveProperties(node, propertiesPath, issues,
                    "value", "controller", "LinearProgressIndicator progress mode");
            return;
        }
        if (type.equals(BadgeWidgetPropertySchema.BADGE_TYPE.value())) {
            if (!BadgeWidgetPropertySchema.isCountMode(node) && node.properties().containsKey(new PropertyName("maxCount"))) {
                issues.add(issue(PROPERTY_DEPENDENCY, propertiesPath + "/maxCount", node.id(), "Badge Max count requires Count. Set Count first."));
            }
            if (BadgeWidgetPropertySchema.isCountMode(node)
                    && node.slots().get(new SlotName("label")) instanceof WidgetSlot.SingleSlot label && label.child().isPresent()) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/count", node.id(), "Badge.count owns the numeric label. Clear or move the existing Label before setting Count."));
            }
            validateFontPackageDependency(node, propertiesPath, issues, "textStylePackage", "textStyleFontFamily", "textStyleFontFamilyFallback", "Badge textStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyleColor", "textStyleForeground", "Badge textStyle");
            validateMutuallyExclusiveProperties(node, propertiesPath, issues, "textStyleBackgroundColor", "textStyleBackground", "Badge textStyle");
            return;
        }
        if (dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE.equals(node.type())) {
            BigDecimal min = numericValue(node, "minExtent"), max = numericValue(node, "maxExtent"), current = numericValue(node, "currentExtent");
            if (min != null && max != null && min.compareTo(max) > 0)
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/minExtent", node.id(), "FlexibleSpaceBarSettings Min extent must not exceed Max extent."));
            if (min != null && current != null && current.compareTo(min) < 0)
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/currentExtent", node.id(), "FlexibleSpaceBarSettings Current extent must be at least Min extent."));
            if (max != null && current != null && current.compareTo(max) > 0)
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/currentExtent", node.id(), "FlexibleSpaceBarSettings Current extent must not exceed Max extent."));
        }
        if (dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(node.type())) {
            if (Boolean.TRUE.equals(booleanValue(node, "snap")) && !Boolean.TRUE.equals(booleanValue(node, "floating"))) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/snap", node.id(), "SliverAppBar snap requires floating."));
            }
            BigDecimal toolbar = numericValue(node, "toolbarHeight");
            if (toolbar == null) toolbar = BigDecimal.valueOf(dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.toolbarDefault(node.type()));
            BigDecimal collapsed = numericValue(node, "collapsedHeight");
            if (collapsed != null && collapsed.compareTo(toolbar) < 0) {
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/collapsedHeight", node.id(), "SliverAppBar collapsedHeight must be at least toolbarHeight (" + toolbar + ")."));
            }
            for (String whole : dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.WHOLE_TYPES.keySet()) {
                if (node.properties().containsKey(new PropertyName(whole))) {
                    for (String local : dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.localFamily(whole)) {
                        if (node.properties().containsKey(new PropertyName(local))) issues.add(issue(PROPERTY_CONFLICT,
                                propertiesPath + "/" + local, node.id(), "SliverAppBar whole " + whole + " and local fields are mutually exclusive."));
                    }
                }
            }
        }
        if (type.equals(AppBarWidgetPropertySchema.APP_BAR_TYPE.value())
                || dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(node.type())) {
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

        if (DefaultTextStyleWidgetPropertySchema.sharesTextProjection(node.type())) {
            boolean reference = !(node.properties().get(new PropertyName("style")) instanceof PropertyValue.StringValue);
            if (reference && node.properties().keySet().stream().anyMatch(AnimatedDefaultTextStyleWidgetPropertySchema::styleLeaf))
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/style", node.id(), "Whole TextStyle reference/null/omission and local style fields are mutually exclusive."));
            if (node.properties().containsKey(new PropertyName("textHeightBehavior")) &&
                    node.properties().keySet().stream().anyMatch(AnimatedDefaultTextStyleWidgetPropertySchema::heightLeaf))
                issues.add(issue(PROPERTY_CONFLICT, propertiesPath + "/textHeightBehavior", node.id(), "Whole TextHeightBehavior/null and local height fields are mutually exclusive."));
        }
        if (!type.equals("flutter.widgets.Text") && !DefaultTextStyleWidgetPropertySchema.sharesTextProjection(node.type())) {
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

    private static void validateStaticScrollViewSemanticChildCount(
            WidgetNode node,
            String propertiesPath,
            String owner,
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
                    owner + " semanticChildCount " + semanticCount
                    + " exceeds the current children count " + childCount + "."));
        }
    }

    private static void validateIndexedStackIndex(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues) {
        PropertyValue configured = node.properties().get(new PropertyName("index"));
        if (configured instanceof PropertyValue.NullValue) {
            return;
        }
        BigInteger index;
        if (configured == null) {
            index = BigInteger.ZERO;
        } else if (configured instanceof PropertyValue.IntegerValue integer) {
            index = integer.value();
        } else {
            // Generic kind validation reports malformed values.
            return;
        }
        if (index.signum() < 0) {
            // Generic integer-range validation reports negative values.
            return;
        }
        WidgetSlot children = node.slots().get(new SlotName("children"));
        int childCount;
        if (children == null) {
            childCount = 0;
        } else if (children instanceof WidgetSlot.ListSlot list) {
            childCount = list.children().size();
        } else {
            // Generic slot-kind validation reports malformed slots.
            return;
        }
        boolean valid = childCount == 0
                ? BigInteger.ZERO.equals(index)
                : index.compareTo(BigInteger.valueOf(childCount)) < 0;
        if (!valid) {
            issues.add(issue(
                    PROPERTY_CONSTRAINT,
                    propertiesPath + "/index",
                    node.id(),
                    "IndexedStack index " + index + " is outside the valid range for "
                    + childCount + " children; use 0 or null when empty, otherwise "
                    + "use null or an index from 0 through " + (childCount - 1) + "."));
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

    private static void validateListTileWholeLocal(WidgetNode node, String path, IssueCollector issues, String whole, List<String> local) {
        if (!node.properties().containsKey(new PropertyName(whole))) return;
        for (String name : local) {
            if (node.properties().containsKey(new PropertyName(name))) {
                issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                        "ListTile " + whole + " whole value and its local fields are mutually exclusive. Reset one representation first."));
            }
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

    private static void validateSubmenuButtonBranches(WidgetNode node, String path, IssueCollector issues) {
        for (String family : List.of("menuStyle", "submenuIcon")) {
            if (!node.properties().containsKey(new PropertyName(family))) continue;
            List<String> locals = family.equals("menuStyle") ? SubmenuButtonWidgetPropertySchema.menuStyleProperties()
                    : SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties();
            for (String name : locals) if (node.properties().containsKey(new PropertyName(name))) {
                issues.add(issue(PROPERTY_CONFLICT, path + '/' + name, node.id(),
                        "SubmenuButton whole " + family + " (including null) is exclusive with its own local fields. Reset one branch first; the other style family is independent."));
            }
        }
        WidgetNode projection = SubmenuButtonWidgetPropertySchema.menuStyleProjection(node);
        IssueCollector projected = new IssueCollector(issues.maximum);
        validateElevatedButtonEffectiveDimensions(projection, path, projected);
        validateElevatedButtonEffectiveShapes(projection, path, projected);
        validateElevatedButtonAlignment(projection, path, projected);
        for (ValidationIssue issue : projected.snapshot()) issues.add(new ValidationIssue(issue.code(), issue.severity(),
                issue.path().replace(path + "/style", path + "/menuStyle"), issue.widgetId(),
                issue.message().replace("MenuAnchor", "SubmenuButton menuStyle").replace("style", "menuStyle")));
    }

    private static void validateTextButtonBranches(WidgetNode node, String path, IssueCollector issues) {
        if (node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE)) {
            List<String> locals = MenuItemButtonWidgetPropertySchema.shortcutLocalProperties().stream()
                    .filter(name -> node.properties().containsKey(new PropertyName(name))).toList();
            if (!locals.isEmpty()) {
                if (node.properties().containsKey(new PropertyName("shortcut"))) {
                    issues.add(issue(PROPERTY_CONFLICT, path + "/shortcut", node.id(),
                            "MenuItemButton whole shortcut (including null) is exclusive with all local activator fields. Reset one branch first."));
                }
                boolean trigger = node.properties().containsKey(new PropertyName("shortcutTrigger"));
                boolean character = node.properties().containsKey(new PropertyName("shortcutCharacter"));
                if (trigger == character) issues.add(issue(PROPERTY_DEPENDENCY, path + "/shortcutTrigger", node.id(),
                        "Local shortcut requires exactly one Trigger or Character anchor. Add one anchor or reset all local shortcut fields."));
                if (character) for (String name : List.of("shortcutShift", "shortcutNumLock")) {
                    if (node.properties().containsKey(new PropertyName(name))) issues.add(issue(PROPERTY_CONFLICT, path + '/' + name, node.id(),
                            "CharacterActivator does not accept " + name + "; reset it or use SingleActivator."));
                }
            }
        }
        boolean icon = OutlinedButtonWidgetPropertySchema.isIconVariant(node);
        if (FilledButtonWidgetPropertySchema.requiresChild(node)
                && (!(node.slots().get(new SlotName("child")) instanceof WidgetSlot.SingleSlot child)
                || child.child().isEmpty())) {
            issues.add(issue(PROPERTY_CONFLICT, path + "/variant", node.id(),
                    "FilledButton Icon and Tonal icon require a non-null Child label. Add Child before selecting an icon constructor."));
        }
        if (!node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE)
                && !icon && node.slots().get(new SlotName("icon")) instanceof WidgetSlot.SingleSlot slot
                && slot.child().isPresent()) {
            issues.add(issue(PROPERTY_CONFLICT, path + "/variant", node.id(),
                    buttonName(node) + " Standard has no icon argument. Clear or move the existing Icon before switching to Standard."));
        }
        String incompatible = icon ? "isSemanticButton" : "iconAlignment";
        if (node.properties().containsKey(new PropertyName(incompatible))) {
            issues.add(issue(PROPERTY_CONFLICT, path + "/" + incompatible, node.id(),
                    buttonName(node) + " " + (icon ? "Icon" : "Standard") + " constructor does not accept "
                    + incompatible + ". Reset it or switch constructor."));
        }
        if (node.properties().containsKey(new PropertyName("style"))) {
            for (String name : TextButtonWidgetPropertySchema.localStyleProperties()) {
                if (node.properties().containsKey(new PropertyName(name))) {
                    issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                            buttonName(node) + " project ButtonStyle is exclusive with every local style field. Reset the project style or local fields first."));
                }
            }
        }
    }

    private static List<String> buttonStatePrefixes(WidgetNode node) {
        return OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                || node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)
                ? TextButtonWidgetPropertySchema.statePrefixes()
                : List.of("style", "styleDisabled", "stylePressed", "styleHovered", "styleFocused");
    }

    private static List<String> enabledButtonStatePriority(WidgetNode node) {
        return OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                || node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)
                ? List.of("Error", "Dragged", "Pressed", "Selected", "ScrolledUnder", "Hovered", "Focused")
                : List.of("Pressed", "Hovered", "Focused");
    }

    private static String buttonName(WidgetNode node) {
        return node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE) ? "MenuAnchor"
                : node.type().equals(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE) ? "SubmenuButton"
                : node.type().equals(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE) ? "MenuItemButton"
                : node.type().equals(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE) ? "IconButton"
                : node.type().equals(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE) ? "FilledButton"
                : node.type().equals(OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE) ? "OutlinedButton"
                : node.type().equals(TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE) ? "TextButton" : "ElevatedButton";
    }

    private static void validateElevatedButtonState(
            WidgetNode node,
            String propertiesPath,
            IssueCollector issues,
            String prefix) {
        String owner = buttonName(node) + " " + prefix + " state";
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
            List<String> states = enabledButtonStatePriority(node).reversed();
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
                buttonName(node) + " effective " + stateDescription + ' '
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
            return value != null ? value : elevatedFrameworkMinimum(node, suffix);
        }
        for (String state : enabledButtonStatePriority(node)) {
            if (activeStates.contains(state)) {
                EffectiveNumber value = elevatedNumber(
                        node, "style" + state + suffix);
                if (value != null) {
                    return value;
                }
            }
        }
        EffectiveNumber value = elevatedNumber(node, "style" + suffix);
        return value != null ? value : elevatedFrameworkMinimum(node, suffix);
    }

    private static EffectiveNumber elevatedFrameworkMinimum(WidgetNode node, String suffix) {
        // Full-style button families preserve context-dependent theme sizing; only two explicit
        // local bounds may conflict. Generation resolves and normalizes inherited bounds.
        if (OutlinedButtonWidgetPropertySchema.usesFullStyleProjection(node)
                || node.type().equals(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)) return null;
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
        List<String> prefixes = buttonStatePrefixes(node);
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
                    buttonName(node) + " stateful TextStyle inherit/theme overrides require "
                    + "an explicit enabled/default styleTextInherit value."));
        }
        if (disabled == null) {
            issues.add(issue(
                    PROPERTY_DEPENDENCY,
                    propertiesPath + "/styleDisabledTextInherit",
                    node.id(),
                    buttonName(node) + " stateful TextStyle inherit/theme overrides require "
                    + "an explicit disabled TextInherit value so animated state "
                    + "transitions remain safe."));
        } else if (base != null && !disabled.equals(base)) {
            issues.add(issue(
                    PROPERTY_CONFLICT,
                    propertiesPath + "/styleDisabledTextInherit",
                    node.id(),
                    buttonName(node) + " reachable TextStyle values must use one inherit "
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
                        buttonName(node) + " property '" + prefix
                        + "TextTheme' requires an explicit same-state TextInherit "
                        + "value for transition-safe TextStyle resolution."));
            } else if (!prefix.equals("style")
                    && !prefix.equals("styleDisabled")
                    && value != null && base != null && !value.equals(base)) {
                issues.add(issue(
                        PROPERTY_CONFLICT,
                        propertiesPath + '/' + prefix + "TextInherit",
                        node.id(),
                        buttonName(node) + " reachable TextStyle values must use the "
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
        List<String> states = enabledButtonStatePriority(node).reversed();
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
                        buttonName(node) + " effective " + stateDescription
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
                        buttonName(node) + " effective " + stateDescription
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
                    buttonName(node) + " effective " + stateDescription
                    + " circle eccentricity requires a locally configured "
                    + "effective shape kind.");
        } else if (!(kind.value() instanceof PropertyValue.StringValue name)
                || !name.value().equals("circle")) {
            addEffectiveShapeIssue(
                    node, propertiesPath, issues, reported,
                    PROPERTY_CONFLICT, eccentricity.propertyName(),
                    buttonName(node) + " effective " + stateDescription
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
        for (String state : enabledButtonStatePriority(node)) {
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
                    buttonName(node) + " ButtonStyle alignment requires "
                    + "styleAlignmentKind, styleAlignmentX, and styleAlignmentY together."));
        }
    }

    private static void validateSwitch(WidgetNode node, String path, IssueCollector issues) {
        boolean tile = node.type().equals(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE);
        if (!tile && node.properties().containsKey(new PropertyName("applyCupertinoTheme"))
                && !new PropertyValue.StringValue("adaptive").equals(node.properties().get(new PropertyName("variant")))) {
            issues.add(issue(PROPERTY_CONFLICT, path + "/applyCupertinoTheme", node.id(),
                    "Switch standard has no Apply Cupertino theme argument; choose Adaptive or reset this field."));
        }
        for (String state : List.of("Active", "Inactive")) {
            String image = Character.toLowerCase(state.charAt(0)) + state.substring(1) + "ThumbImage";
            String callback = "on" + state + "ThumbImageError";
            if (node.properties().containsKey(new PropertyName(callback))
                    && !(tile && node.properties().get(new PropertyName(callback)) instanceof PropertyValue.NullValue)
                    && !node.properties().containsKey(new PropertyName(image))) {
                issues.add(issue(PROPERTY_DEPENDENCY, path + "/" + callback, node.id(),
                        (tile ? "SwitchListTile " : "Switch ") + callback + " requires " + image + ". Set the image first."));
            }
        }
        List<String> families = new ArrayList<>(SwitchWidgetPropertySchema.colorFamilies());
        if (!tile) families.add("trackOutlineWidth");
        families.add("thumbIcon");
        for (String family : families) {
            if (!node.properties().containsKey(new PropertyName(family))) {
                continue;
            }
            List<String> locals = family.equals("thumbIcon") ? SwitchWidgetPropertySchema.thumbIconLocalProperties()
                    : family.equals("trackOutlineWidth") ? SwitchWidgetPropertySchema.outlineWidthStateProperties()
                    : SwitchWidgetPropertySchema.colorStateProperties(family);
            for (String name : locals) {
                if (node.properties().containsKey(new PropertyName(name))) {
                    issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                            "Switch " + family + " reference and local fields are mutually exclusive."));
                }
            }
        }
        for (String state : SwitchWidgetPropertySchema.thumbIconStates()) {
            List<String> bucket = SwitchWidgetPropertySchema.thumbIconBucketProperties(state);
            if (new PropertyValue.StringValue("inherit").equals(node.properties().get(new PropertyName(bucket.getFirst())))) {
                for (String name : bucket.subList(1, bucket.size())) {
                    if (node.properties().containsKey(new PropertyName(name))) {
                        issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                                "Switch Inherit thumb icon mode returns null and cannot contain Icon details."));
                    }
                }
            }
        }
    }

    private static void validateRadio(WidgetNode node, String path, IssueCollector issues) {
        RadioWidgetPropertySchema.valueTypeError(node).ifPresent(message ->
                issues.add(issue(PROPERTY_DEPENDENCY, path + "/valueType", node.id(), message)));
        if (!new PropertyValue.StringValue("adaptive").equals(node.properties().get(new PropertyName("variant")))
                && node.properties().containsKey(new PropertyName("useCupertinoCheckmarkStyle"))) {
            issues.add(issue(PROPERTY_CONFLICT, path + "/useCupertinoCheckmarkStyle", node.id(),
                    "Radio Use Cupertino Checkmark Style is available only on the Adaptive constructor."));
        }
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius", "side", "visualDensity")) {
            if (!node.properties().containsKey(new PropertyName(family))) continue;
            List<String> locals = switch (family) {
                case "side" -> RadioWidgetPropertySchema.sideLocalProperties();
                case "innerRadius" -> RadioWidgetPropertySchema.innerRadiusStateProperties();
                case "visualDensity" -> List.of("visualDensityHorizontal", "visualDensityVertical");
                default -> RadioWidgetPropertySchema.colorStateProperties(family);
            };
            for (String name : locals) if (node.properties().containsKey(new PropertyName(name))) {
                issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                        "Radio " + family + " whole reference and local fields are mutually exclusive."));
            }
        }
        validateStatefulRadioOrCheckboxSide(node, path, issues, "Radio");
    }

    private static void validateCheckbox(WidgetNode node, String path, IssueCollector issues) {
        validateCheckbox(node, path, issues, "Checkbox");
    }

    private static void validateCheckbox(WidgetNode node, String path, IssueCollector issues, String label) {
        if (node.properties().get(new PropertyName("value")) instanceof PropertyValue.NullValue
                && !new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("tristate")))) {
            issues.add(issue(PROPERTY_DEPENDENCY, path + "/value", node.id(),
                    label + " null Value requires Tristate true. Set a concrete false/true value before disabling Tristate."));
        }
        for (String family : List.of("fillColor", "overlayColor", "side")) {
            if (!node.properties().containsKey(new PropertyName(family))) {
                continue;
            }
            List<String> locals = family.equals("side") ? CheckboxWidgetPropertySchema.sideLocalProperties()
                    : CheckboxWidgetPropertySchema.colorStateProperties(family);
            for (String name : locals) {
                if (node.properties().containsKey(new PropertyName(name))) {
                    issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                            label + " " + family + " reference and local fields are mutually exclusive."));
                }
            }
        }
        validateStatefulRadioOrCheckboxSide(node, path, issues, label);
    }

    private static void validateStatefulRadioOrCheckboxSide(WidgetNode node, String path, IssueCollector issues, String family) {
        validateStatefulRadioOrCheckboxSide(node, path, issues, family, "side");
    }

    private static void validateStatefulRadioOrCheckboxSide(WidgetNode node, String path, IssueCollector issues, String family, String prefix) {
        boolean stateful = new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName(prefix + "Stateful")));
        for (String state : CheckboxWidgetPropertySchema.sideStates()) {
            List<String> bucket = CheckboxWidgetPropertySchema.sideBucketProperties(state).stream().map(name -> prefix + name.substring(4)).toList();
            if (!stateful && bucket.stream().anyMatch(name -> node.properties().containsKey(new PropertyName(name)))) {
                issues.add(issue(PROPERTY_DEPENDENCY, path + "/" + prefix + state + "Mode", node.id(),
                        family + " state-specific side fields require Side Stateful true."));
            }
            if (new PropertyValue.StringValue("inherit").equals(node.properties().get(new PropertyName(bucket.getFirst())))) {
                for (String detail : bucket.subList(1, bucket.size())) {
                    if (node.properties().containsKey(new PropertyName(detail))) {
                        issues.add(issue(PROPERTY_CONFLICT, path + "/" + detail, node.id(),
                                family + " Inherit side mode returns null and cannot contain border details."));
                    }
                }
            }
        }
    }

    private static void validateCardShape(WidgetNode node, String path, IssueCollector issues) {
        String family = node.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)
                ? "FloatingActionButton" : node.type().equals(CheckboxWidgetPropertySchema.CHECKBOX_TYPE) ? "Checkbox" : "Card";
        validateCardShape(node, path, issues, "shape", family);
    }

    private static void validateCardShape(WidgetNode node, String path, IssueCollector issues, String prefix, String family) {
        PropertyValue kindValue = node.properties().get(new PropertyName(prefix + "Kind"));
        String kind = kindValue instanceof PropertyValue.StringValue value ? value.value() : null;
        boolean reference = node.properties().containsKey(new PropertyName(prefix));
        for (String sourceName : CardWidgetPropertySchema.builtInShapePropertyNames()) {
            String name = prefix + sourceName.substring(5);
            if (!node.properties().containsKey(new PropertyName(name))) continue;
            if (reference) issues.add(issue(PROPERTY_CONFLICT, path + "/" + name, node.id(),
                    family + " ShapeBorder reference and built-in shape fields are mutually exclusive."));
            if (CardWidgetPropertySchema.isShapeDetailProperty(sourceName)
                    && (kind == null || !CardWidgetPropertySchema.shapePropertyAppliesToKind(sourceName, kind))) {
                issues.add(issue(PROPERTY_DEPENDENCY, path + "/" + name, node.id(),
                        family + " " + name + " requires a compatible explicit " + prefix + "Kind."));
            }
        }
        if ("star".equals(kind)) {
            BigDecimal point = numericValue(node, prefix + "PointRounding");
            BigDecimal valley = numericValue(node, prefix + "ValleyRounding");
            if ((point == null ? BigDecimal.ZERO : point).add(valley == null ? BigDecimal.ZERO : valley)
                    .compareTo(BigDecimal.ONE) > 0) {
                issues.add(issue(PROPERTY_CONSTRAINT, path + "/" + prefix + "ValleyRounding", node.id(),
                        family + " StarBorder pointRounding plus valleyRounding must not exceed one."));
            }
        }
    }

    private static void validateCircleAvatar(WidgetNode node, String path, IssueCollector issues) {
        for (String layer : List.of("Background", "Foreground")) {
            String image = Character.toLowerCase(layer.charAt(0)) + layer.substring(1) + "Image";
            String callback = "on" + layer + "ImageError";
            if (node.properties().containsKey(new PropertyName(callback))
                    && !node.properties().containsKey(new PropertyName(image))) {
                issues.add(issue(PROPERTY_DEPENDENCY, path + "/" + callback, node.id(),
                        "CircleAvatar " + callback + " requires " + image + ". Set the image first."));
            }
        }
        validateMutuallyExclusiveProperties(node, path, issues,
                "radius", "minRadius", "CircleAvatar radii");
        validateMutuallyExclusiveProperties(node, path, issues,
                "radius", "maxRadius", "CircleAvatar radii");
        double minimum = circleAvatarDiameter(node.properties().get(new PropertyName("minRadius")), 0);
        double maximum = circleAvatarDiameter(node.properties().get(new PropertyName("maxRadius")),
                Double.POSITIVE_INFINITY);
        if (minimum > maximum) {
            issues.add(issue(PROPERTY_CONFLICT, path + "/minRadius", node.id(),
                    "CircleAvatar resolved minimum diameter must not exceed maximum diameter."));
        }
    }

    private static double circleAvatarDiameter(PropertyValue value, double omittedDiameter) {
        if (value instanceof PropertyValue.IntegerValue integer) {
            return 2.0 * integer.value().doubleValue();
        }
        if (value instanceof PropertyValue.DoubleValue decimal) {
            return 2.0 * decimal.value().doubleValue();
        }
        return CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY.equals(value)
                ? Double.POSITIVE_INFINITY : omittedDiameter;
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
