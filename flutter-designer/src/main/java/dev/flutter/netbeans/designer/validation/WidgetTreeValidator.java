package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.ParameterStyle;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
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
    public static final String PROPERTY_UNIQUENESS = "designer.property.uniqueness";
    public static final String MISSING_SLOT = "designer.slot.missing";
    public static final String UNKNOWN_SLOT = "designer.slot.unknown";
    public static final String SLOT_KIND = "designer.slot.kind";
    public static final String SLOT_CARDINALITY = "designer.slot.cardinality";
    public static final String SLOT_NULL = "designer.slot.null";
    public static final String SLOT_ACCEPTANCE = "designer.slot.acceptance";
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
        pending.push(new NodeFrame(document.root(), ROOT_PATH, 1, null));

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
            } else if (frame.parentSlot() != null
                    && !frame.parentSlot().acceptance().accepts(definition.orElseThrow())) {
                issues.add(issue(
                        SLOT_ACCEPTANCE,
                        frame.path(),
                        node.id(),
                        "Slot '" + frame.parentSlot().name().value() + "' does not accept widget type '"
                        + node.type().value() + "' at '" + frame.path() + "'."));
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

        if (!type.equals("flutter.widgets.Text")) {
            return;
        }
        PropertyValue value = node.properties().get(new PropertyName("semanticsIdentifier"));
        if (!(value instanceof PropertyValue.StringValue identifier)) {
            return;
        }
        String propertyPath = propertiesPath + "/semanticsIdentifier";
        String firstPath = firstSemanticsIdentifierPaths.putIfAbsent(identifier.value(), propertyPath);
        if (firstPath != null) {
            issues.add(issue(
                    PROPERTY_UNIQUENESS,
                    propertyPath,
                    node.id(),
                    "Text semanticsIdentifier '" + identifier.value() + "' at '" + propertyPath
                    + "' duplicates the identifier first declared at '" + firstPath + "'."));
        }
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
            collectChildren(value, null, slotPath, depth, children, issues, frontier);
            if (issues.truncated() || frontier.limitReached()) {
                return children;
            }
        }
        return children;
    }

    private static void inspectSlot(
            WidgetNode owner,
            WidgetSlot value,
            SlotDefinition definition,
            String slotsPath,
            int depth,
            List<NodeFrame> children,
            IssueCollector issues,
            TraversalFrontier frontier) {
        String slotPath = slotsPath + "/" + pointer(definition.name().value());
        if (value.cardinality() != definition.cardinality()) {
            issues.add(issue(
                    SLOT_KIND,
                    slotPath,
                    owner.id(),
                    "Slot '" + definition.name().value() + "' on widget '"
                    + owner.type().value() + "' has kind '" + value.cardinality().wireName()
                    + "'; expected '" + definition.cardinality().wireName() + "'."));
        }
        if (issues.truncated()) {
            return;
        }

        int childCount = childCount(value);
        if (value instanceof WidgetSlot.SingleSlot single
                && single.child().isEmpty()
                && definition.minChildren() > 0) {
            issues.add(issue(
                    SLOT_NULL,
                    slotPath + "/child",
                    owner.id(),
                    "Slot '" + definition.name().value() + "' on widget '"
                    + owner.type().value() + "' cannot contain an explicit null child."));
        } else if (childCount < definition.minChildren()
                || childCount > definition.maxChildren()) {
            issues.add(issue(
                    SLOT_CARDINALITY,
                    slotPath,
                    owner.id(),
                    "Slot '" + definition.name().value() + "' on widget '"
                    + owner.type().value() + "' contains " + childCount
                    + " children; expected between " + definition.minChildren()
                    + " and " + definition.maxChildren() + "."));
        }
        if (issues.truncated()) {
            return;
        }
        collectChildren(
                value,
                definition,
                slotPath,
                depth,
                children,
                issues,
                frontier);
    }

    private static void collectChildren(
            WidgetSlot value,
            SlotDefinition definition,
            String slotPath,
            int depth,
            List<NodeFrame> children,
            IssueCollector issues,
            TraversalFrontier frontier) {
        if (value instanceof WidgetSlot.SingleSlot single) {
            if (single.child().isPresent()) {
                WidgetNode child = single.child().orElseThrow();
                frontier.discover(
                        new NodeFrame(child, slotPath + "/child", depth + 1, definition),
                        children,
                        issues);
            }
        } else if (value instanceof WidgetSlot.ListSlot list) {
            for (int index = 0; index < list.children().size(); index++) {
                if (!frontier.discover(new NodeFrame(
                        list.children().get(index),
                        slotPath + "/children/" + index,
                        depth + 1,
                        definition), children, issues)) {
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

    private static String pointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
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
