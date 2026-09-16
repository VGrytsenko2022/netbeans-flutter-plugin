package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCapabilityCatalog;
import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ColoredBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipOvalWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipPathWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRSuperellipseWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PhysicalModelWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PhysicalShapeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DecoratedBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DirectionalityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IgnorePointerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MouseRegionWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.AbsorbPointerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BlockSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeFocusWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeFocusTraversalWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.VisibilityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TickerModeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextHeightBehaviorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultSelectionStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconThemeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ImageIconWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DividerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.VerticalDividerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CardWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircleAvatarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.LinearProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircularProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuItemButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuAnchorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SubmenuButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MenuBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NavigationBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NavigationRailWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.NavigationDrawerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DrawerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BottomAppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BottomNavigationBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MaterialWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ScrollbarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.OutlinedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FilledButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SliderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RangeSliderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioListTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipVisibilityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TooltipThemeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RadioGroupWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ContainerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SafeAreaWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.AnimatedDefaultTextStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextStyleWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DefaultTextStyleTransitionWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewExtentWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PlaceholderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PageViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListWheelScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CustomScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SliverChildrenWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PreferredSizeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BuilderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCapability;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.PatchProperties;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Standard NetBeans node projection for one selected Flutter Designer widget.
 * The three-argument form is fully read-only; the mutation-aware form exposes
 * only the capability-reviewed property slice.
 */
public final class FlutterWidgetPropertiesNode extends AbstractNode {
    public static final String IDENTITY_SET_NAME = "identity";
    public static final String PROPERTIES_SET_NAME = Sheet.PROPERTIES;
    public static final String SLOTS_SET_NAME = "slots";
    public static final String EVENTS_SET_NAME = "events";
    public static final String GENERAL_TAB_NAME = "Properties";
    public static final String EVENTS_TAB_NAME = "Events";
    public static final String SLOTS_TAB_NAME = "Slots";
    static final String TAB_NAME_ATTRIBUTE = "tabName";
    public static final String STABLE_ID_PROPERTY_NAME = "stableId";
    public static final String TYPE_PROPERTY_NAME = "type";
    public static final String NOT_SET = FlutterPropertyCellValue.NOT_SET_TEXT;
    private static final WidgetTypeId ASPECT_RATIO_TYPE =
            new WidgetTypeId("flutter.widgets.AspectRatio");
    private static final PropertyName ASPECT_RATIO_PROPERTY =
            new PropertyName("aspectRatio");
    private static final WidgetTypeId BASELINE_TYPE =
            new WidgetTypeId("flutter.widgets.Baseline");
    private static final WidgetTypeId INTRINSIC_HEIGHT_TYPE =
            new WidgetTypeId("flutter.widgets.IntrinsicHeight");
    private static final WidgetTypeId INTRINSIC_WIDTH_TYPE =
            new WidgetTypeId("flutter.widgets.IntrinsicWidth");
    private static final WidgetTypeId OFFSTAGE_TYPE =
            new WidgetTypeId("flutter.widgets.Offstage");
    private static final WidgetTypeId SIZED_OVERFLOW_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.SizedOverflowBox");
    private static final WidgetTypeId TRANSFORM_TYPE =
            new WidgetTypeId("flutter.widgets.Transform");
    private static final WidgetTypeId ROTATED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.RotatedBox");
    private static final WidgetTypeId LIST_BODY_TYPE =
            new WidgetTypeId("flutter.widgets.ListBody");
    private static final WidgetTypeId OVERFLOW_BAR_TYPE =
            new WidgetTypeId("flutter.widgets.OverflowBar");
    private static final PropertyName OFFSTAGE_PROPERTY =
            new PropertyName("offstage");
    private static final PropertyName BASELINE_PROPERTY =
            new PropertyName("baseline");
    private static final PropertyName BASELINE_TYPE_PROPERTY =
            new PropertyName("baselineType");
    private static final WidgetTypeId OPACITY_TYPE =
            new WidgetTypeId("flutter.widgets.Opacity");
    private static final PropertyName OPACITY_PROPERTY =
            new PropertyName("opacity");
    private static final PropertyName ALWAYS_INCLUDE_SEMANTICS_PROPERTY =
            new PropertyName("alwaysIncludeSemantics");
    private static final WidgetTypeId ALIGN_TYPE =
            new WidgetTypeId("flutter.widgets.Align");
    private static final WidgetTypeId FRACTIONALLY_SIZED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.FractionallySizedBox");
    private static final WidgetTypeId FITTED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.FittedBox");
    private static final WidgetTypeId CONSTRAINED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.ConstrainedBox");
    private static final WidgetTypeId UNCONSTRAINED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.UnconstrainedBox");
    private static final WidgetTypeId LIMITED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.LimitedBox");
    private static final WidgetTypeId OVERFLOW_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.OverflowBox");
    private static final WidgetTypeId STACK_TYPE =
            new WidgetTypeId("flutter.widgets.Stack");
    private static final WidgetTypeId WRAP_TYPE =
            new WidgetTypeId("flutter.widgets.Wrap");
    private static final WidgetTypeId EXPANDED_TYPE =
            new WidgetTypeId("flutter.widgets.Expanded");
    private static final WidgetTypeId FLEXIBLE_TYPE =
            new WidgetTypeId("flutter.widgets.Flexible");
    private static final WidgetTypeId SPACER_TYPE =
            new WidgetTypeId("flutter.widgets.Spacer");
    private static final WidgetTypeId IMAGE_TYPE =
            new WidgetTypeId("flutter.widgets.Image");
    private static final PropertyName ALIGNMENT_PROPERTY =
            new PropertyName("alignment");
    private static final PropertyName WIDTH_FACTOR_PROPERTY =
            new PropertyName("widthFactor");
    private static final PropertyName HEIGHT_FACTOR_PROPERTY =
            new PropertyName("heightFactor");
    private static final PropertyName CONSTRAINTS_PROPERTY =
            new PropertyName("constraints");
    private static final SlotName CHILD_SLOT = new SlotName("child");
    private static final SlotName CHILDREN_SLOT = new SlotName("children");
    private static final PropertyName TEXT_DIRECTION_PROPERTY =
            new PropertyName("textDirection");
    private static final PropertyName STACK_FIT_PROPERTY =
            new PropertyName("fit");
    private static final PropertyName STACK_CLIP_BEHAVIOR_PROPERTY =
            new PropertyName("clipBehavior");
    private static final PropertyName FLEX_PROPERTY =
            new PropertyName("flex");
    private static final PropertyName FLEXIBLE_FIT_PROPERTY =
            new PropertyName("fit");
    private static final PropertyName CONTAINER_COLOR = new PropertyName("color");
    private static final PropertyName CONTAINER_DECORATION = new PropertyName("decoration");
    private static final PropertyName CONTAINER_CLIP = new PropertyName("clipBehavior");
    private static final PropertyName CLIP_PATH_CLIPPER = new PropertyName("clipper");
    private static final PropertyName CLIP_PATH_SHAPE = new PropertyName("shape");
    private static final java.util.List<PropertyName> TEXT_FIELD_CURSOR_RADIUS =
            java.util.List.of(
                    new PropertyName("cursorRadiusX"),
                    new PropertyName("cursorRadiusY"));
    private static final java.util.List<PropertyName> TEXT_FIELD_SCROLL_PADDING =
            java.util.List.of(
                    new PropertyName("scrollPaddingLeft"),
                    new PropertyName("scrollPaddingTop"),
                    new PropertyName("scrollPaddingRight"),
                    new PropertyName("scrollPaddingBottom"));

    private final InstanceContent lookupContent;
    private final boolean propertyMutationProjection;
    private final boolean slotMutationProjection;
    private final WidgetNode widget;
    private final WidgetDefinition definition;
    private final java.util.List<WidgetEventDescriptor> events;
    private volatile Presentation presentation;
    private volatile FlutterWidgetEventsContext eventsContext;

    private record Presentation(
            WidgetNode widget,
            WidgetDefinition definition,
            PropertyMutationHandler mutationHandler,
            FlutterWidgetSlotEditorContext slotEditorContext,
            SlotMutationHandler slotMutationHandler,
            java.util.Map<SlotName, AtomicBoolean> slotMutationGates,
            FlutterImageAssetChoices imageAssetChoices) {

        private Presentation {
            Objects.requireNonNull(widget, "widget");
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(slotMutationGates, "slotMutationGates");
            Objects.requireNonNull(imageAssetChoices, "imageAssetChoices");
            if (slotMutationHandler != null && slotEditorContext == null) {
                throw new IllegalArgumentException(
                        "A slot mutation handler requires a slot editor context.");
            }
        }
    }

    private static final class MutableLookup {
        private final InstanceContent content = new InstanceContent();
        private final AbstractLookup lookup = new AbstractLookup(content);
    }

    /** Dispatches one exact command from the immutable selected-widget snapshot. */
    @FunctionalInterface
    public interface PropertyMutationHandler {
        void submit(DesignerCommand command);
    }

    /** Dispatches one exact named-slot intent from the selected revision. */
    @FunctionalInterface
    public interface SlotMutationHandler {
        void submit(FlutterWidgetSlotMutation mutation);
    }

    /**
     * Creates a read-only widget node usable by Explorer and the standard
     * Properties window.
     *
     * @param children explorer children representing the widget slots
     * @param widget immutable widget model node
     * @param definition matching catalog definition
     */
    public FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition) {
        this(children, widget, definition, null, null, null);
    }

    /**
     * Creates a widget node with its capability-reviewed writable property
     * surface. A {@code null} handler keeps the entire sheet read-only.
     *
     * @param children explorer children representing the widget slots
     * @param widget immutable widget model node
     * @param definition matching catalog definition
     * @param mutationHandler mutation dispatcher, or {@code null} for a
     *     read-only node
     */
    public FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition,
            PropertyMutationHandler mutationHandler) {
        this(children, widget, definition, mutationHandler, null, null);
    }

    /**
     * Creates the complete read/write Properties projection for one widget.
     * Property and structural-slot mutations deliberately use separate
     * revision-bound handlers.
     */
    public FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition,
            PropertyMutationHandler mutationHandler,
            FlutterWidgetSlotEditorContext slotEditorContext,
            SlotMutationHandler slotMutationHandler) {
        this(
                children,
                widget,
                definition,
                mutationHandler,
                slotEditorContext,
                slotMutationHandler,
                FlutterImageAssetChoices.empty());
    }

    /** Creates the complete Properties projection with declared asset choices. */
    public FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition,
            PropertyMutationHandler mutationHandler,
            FlutterWidgetSlotEditorContext slotEditorContext,
            SlotMutationHandler slotMutationHandler,
            FlutterImageAssetChoices imageAssetChoices) {
        this(
                children,
                widget,
                definition,
                mutationHandler,
                slotEditorContext,
                slotMutationHandler,
                imageAssetChoices,
                new MutableLookup());
    }

    private FlutterWidgetPropertiesNode(
            Children children,
            WidgetNode widget,
            WidgetDefinition definition,
            PropertyMutationHandler mutationHandler,
            FlutterWidgetSlotEditorContext slotEditorContext,
            SlotMutationHandler slotMutationHandler,
            FlutterImageAssetChoices imageAssetChoices,
            MutableLookup mutableLookup) {
        super(
                Objects.requireNonNull(children, "children"),
                Objects.requireNonNull(mutableLookup, "mutableLookup").lookup);
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(definition, "definition");
        if (!widget.type().equals(definition.typeId())) {
            throw new IllegalArgumentException(
                    "Widget type " + widget.type() + " does not match catalog definition "
                    + definition.typeId());
        }
        this.lookupContent = mutableLookup.content;
        this.propertyMutationProjection = mutationHandler != null;
        this.slotMutationProjection = slotMutationHandler != null;
        this.widget = widget;
        this.definition = definition;
        this.events = WidgetEventCatalog.eventsFor(definition).stream()
                .filter(event -> event.kind() == WidgetEventDescriptor.Kind.EVENT).toList();
        this.presentation = new Presentation(
                widget,
                definition,
                mutationHandler,
                slotEditorContext,
                slotMutationHandler,
                newSlotMutationGates(definition),
                imageAssetChoices);
        updateLookupContent(presentation);
        String displayName = definition.palette().displayName();
        setName(widget.id().toString());
        setDisplayName(displayName);
        if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(widget.type())) {
            setShortDescription(
                    displayName + " — " + widget.id()
                    + ". Runtime typed text, selection, controller state, and focus "
                    + "state are not stored by Designer.");
        } else if ("flutter.widgets.MergeSemantics".equals(widget.type().value())) {
            setShortDescription(displayName + " — " + widget.id()
                    + ". Merge the optional child's semantics subtree into one node. No scalar constructor "
                    + "properties: edit Child in Slots or select a descendant in the widget tree to edit its properties.");
        } else if ("flutter.widgets.RepaintBoundary".equals(widget.type().value())) {
            setShortDescription(displayName + " — " + widget.id()
                    + ". Separate display list for its optional child. No scalar constructor "
                    + "properties: edit Child in Slots or select a descendant to edit its properties.");
        } else {
            setShortDescription(displayName + " — " + widget.id());
        }
        FlutterWidgetIconRegistry.findIconPath(widget.type())
                .ifPresent(this::setIconBaseWithExtension);
    }

    /**
     * Returns whether this stable Explorer node can accept a newer immutable
     * snapshot without changing its PropertySheet schema.
     */
    public boolean canRefreshPresentation(
            WidgetNode nextWidget,
            WidgetDefinition nextDefinition,
            PropertyMutationHandler nextMutationHandler,
            SlotMutationHandler nextSlotMutationHandler) {
        Objects.requireNonNull(nextWidget, "nextWidget");
        Objects.requireNonNull(nextDefinition, "nextDefinition");
        return widget.id().equals(nextWidget.id())
                && widget.type().equals(nextWidget.type())
                && definition.equals(nextDefinition)
                && (nextMutationHandler == null || propertyMutationProjection)
                && (nextSlotMutationHandler == null || slotMutationProjection);
    }

    /**
     * Refreshes values and revision-bound mutation authority while retaining
     * the Node, property-set and property identities used by PropertySheet.
     */
    public void refreshPresentation(
            WidgetNode nextWidget,
            WidgetDefinition nextDefinition,
            PropertyMutationHandler nextMutationHandler,
            FlutterWidgetSlotEditorContext nextSlotEditorContext,
            SlotMutationHandler nextSlotMutationHandler,
            FlutterImageAssetChoices nextImageAssetChoices) {
        if (!canRefreshPresentation(
                nextWidget,
                nextDefinition,
                nextMutationHandler,
                nextSlotMutationHandler)) {
            throw new IllegalArgumentException(
                    "A widget Properties node can only refresh an unchanged schema.");
        }
        Objects.requireNonNull(nextImageAssetChoices, "nextImageAssetChoices");
        if (nextSlotMutationHandler != null && nextSlotEditorContext == null) {
            throw new IllegalArgumentException(
                    "A slot mutation handler requires a slot editor context.");
        }

        Presentation previousPresentation = presentation;
        // An open event editor belongs to exactly one revision. The owner
        // installs new operation authority after refreshing the stable node.
        eventsContext = null;
        WidgetNode previousWidget = previousPresentation.widget();
        Presentation nextPresentation = new Presentation(
                nextWidget,
                nextDefinition,
                nextMutationHandler,
                nextSlotEditorContext,
                nextSlotMutationHandler,
                slotMutationGatesFor(
                        previousPresentation,
                        nextDefinition,
                        nextSlotMutationHandler),
                nextImageAssetChoices);
        boolean imageChoicesChanged = !previousPresentation.imageAssetChoices()
                .equals(nextImageAssetChoices);
        presentation = nextPresentation;
        updateLookupContent(nextPresentation);

        if (imageChoicesChanged) {
            for (Node.PropertySet set : getPropertySets()) {
                for (Node.Property<?> property : set.getProperties()) {
                    property.setValue(
                            FlutterImageAssetChoices.FEATURE_ATTRIBUTE,
                            nextImageAssetChoices);
                }
            }
        }

        for (PropertyDefinition property : definition.properties()) {
            PropertyValue previous = previousWidget.properties().get(property.name());
            PropertyValue next = nextWidget.properties().get(property.name());
            if (!Objects.equals(previous, next)
                    || inactiveFocusProperty(previousWidget, property.name()) != inactiveFocusProperty(nextWidget, property.name())
                    || inactiveSwitchTileProperty(previousWidget, property.name()) != inactiveSwitchTileProperty(nextWidget, property.name())
                    || inactiveRadioTileProperty(previousWidget, property.name()) != inactiveRadioTileProperty(nextWidget, property.name())
                    || requiredExternalFocusNode(previousWidget, property.name()) != requiredExternalFocusNode(nextWidget, property.name())
                    || isNotificationCallback(nextWidget, property.name()) && !Objects.equals(
                            previousWidget.properties().get(new PropertyName("notificationType")), nextWidget.properties().get(new PropertyName("notificationType")))
                    || !Objects.equals(previousWidget.propertyBindings().get(property.name()), nextWidget.propertyBindings().get(property.name()))
                    || !previousWidget.stateBinding().equals(nextWidget.stateBinding())
                    && (dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.isBoundPreview(previousWidget, property.name())
                    || dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.isBoundPreview(nextWidget, property.name()))) {
                firePropertyChange(property.name().value(), null, null);
            }
        }
        for (SlotDefinition slot : definition.slots()) {
            String previous = slotSummary(slot, previousWidget.slots().get(slot.name()));
            String next = slotSummary(slot, nextWidget.slots().get(slot.name()));
            if (!previous.equals(next)) {
                firePropertyChange(slot.name().value(), null, null);
            }
        }
        if (!Objects.equals(previousWidget.properties().get(new PropertyName("enabled")),
                nextWidget.properties().get(new PropertyName("enabled")))) {
            fireEventRowChanges();
        }
    }

    /** Rebinds Events actions without replacing property sets or row identities. */
    public void updateEventsContext(FlutterWidgetEventsContext nextContext) {
        if (eventsContext != nextContext) {
            eventsContext = nextContext;
            fireEventRowChanges();
        }
    }

    private void fireEventRowChanges() {
        events.forEach(event -> firePropertyChange(event.propertyName().value(), null, null));
    }

    private void updateLookupContent(Presentation current) {
        lookupContent.set(java.util.List.of(
                current.widget().id(),
                current.widget(),
                current.definition()), null);
    }

    private static java.util.Map<SlotName, AtomicBoolean> slotMutationGatesFor(
            Presentation previous,
            WidgetDefinition nextDefinition,
            SlotMutationHandler nextHandler) {
        if (previous.slotMutationHandler() == nextHandler) {
            return previous.slotMutationGates();
        }
        return newSlotMutationGates(nextDefinition);
    }

    private static java.util.Map<SlotName, AtomicBoolean> newSlotMutationGates(
            WidgetDefinition currentDefinition) {
        java.util.LinkedHashMap<SlotName, AtomicBoolean> gates =
                new java.util.LinkedHashMap<>();
        for (SlotDefinition slot : currentDefinition.slots()) {
            gates.put(slot.name(), new AtomicBoolean());
        }
        return java.util.Collections.unmodifiableMap(gates);
    }

    private static FlutterPropertyCellValue propertyCellValue(PropertyValue value) {
        return value == null
                ? FlutterPropertyCellValue.unset()
                : FlutterPropertyCellValue.explicit(value);
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = new Sheet();
        boolean hasSlotTab = !definition.slots().isEmpty();

        Sheet.Set identity = new Sheet.Set();
        identity.setName(IDENTITY_SET_NAME);
        identity.setDisplayName("Widget identity");
        identity.setShortDescription("Stable identity and catalog type of the selected widget.");
        identity.put(readOnly(
                STABLE_ID_PROPERTY_NAME,
                "Stable ID",
                "Stable identifier stored in the Flutter Designer model.",
                widget.id().toString()));
        identity.put(readOnly(
                TYPE_PROPERTY_NAME,
                "Widget type",
                "Catalog type identifier stored in the Flutter Designer model.",
                widget.type().value()));
        assignTab(identity, hasSlotTab ? GENERAL_TAB_NAME : null);
        sheet.put(identity);

        if (ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE.equals(widget.type())) {
            addScaffoldPropertySets(sheet, hasSlotTab);
        } else if (dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.TYPE.equals(widget.type())) {
            var set = propertySet("theme", "Theme", dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            var field = dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.FIELDS.getFirst();
            var property = definition.property(new PropertyName(field.name())).orElseThrow();
            set.put(projectProperty(property, Optional.empty(), field.label(), field.description(), false,
                    dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.PRESETS));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE.equals(widget.type())) {
            var set=propertySet("animatedTheme","Theme animation",dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.DESCRIPTION);
            assignTab(set,hasSlotTab?GENERAL_TAB_NAME:null);
            for(var field:dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.FIELDS){
                var property=definition.property(new PropertyName(field.name())).orElseThrow();
                set.put(projectProperty(property,Optional.empty(),field.label(),field.description(),false,
                    field.name().equals("curve")?ExpansionTileWidgetPropertySchema.curvePresets():field.name().equals("data")?dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.PRESETS:java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.TYPE.equals(widget.type())) {
            var set=propertySet("animatedPhysicalModel","Physical animation",dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.DESCRIPTION);
            assignTab(set,hasSlotTab?GENERAL_TAB_NAME:null);
            for(var field:dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.FIELDS){
                var property=definition.property(new PropertyName(field.name())).orElseThrow();
                set.put(projectProperty(property,Optional.empty(),field.label(),field.description(),false,
                    field.name().equals("curve")?ExpansionTileWidgetPropertySchema.curvePresets():java.util.List.of()));
            }
            sheet.put(set);
        } else if (DefaultTextStyleWidgetPropertySchema.sharesTextProjection(widget.type())) {
            var sets = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var property : definition.properties()) {
                var binding = TextWidgetPropertySchema.find(property.name()).orElse(null);
                boolean animated = AnimatedDefaultTextStyleWidgetPropertySchema.TYPE.equals(widget.type());
                boolean transition = DefaultTextStyleTransitionWidgetPropertySchema.TYPE.equals(widget.type());
                String key = binding == null ? (animated ? "animation" : "defaultTextStyle") : binding.group().setName();
                var set = sets.computeIfAbsent(key, ignored -> {
                    var created = propertySet(key, binding == null ? (animated ? "Style and animation" : "Default text style") : binding.group().displayName(),
                            transition ? DefaultTextStyleTransitionWidgetPropertySchema.DESCRIPTION : animated ? AnimatedDefaultTextStyleWidgetPropertySchema.DESCRIPTION : DefaultTextStyleWidgetPropertySchema.description(widget.type()));
                    assignTab(created, hasSlotTab ? GENERAL_TAB_NAME : null); return created;
                });
                String name = property.name().value();
                String label = binding == null ? switch(name) {
                    case "style" -> transition ? "Style animation" : "Style source";
                    case "durationUs" -> "Duration (microseconds)";
                    case "onEnd" -> "On end";
                    case "textHeightBehavior" -> "Text height behavior";
                    default -> "Curve";
                } : binding.displayName();
                set.put(projectProperty(property, Optional.empty(), label, transition ? DefaultTextStyleTransitionWidgetPropertySchema.help(property.name()) : animated ? AnimatedDefaultTextStyleWidgetPropertySchema.help(property.name()) : DefaultTextStyleWidgetPropertySchema.help(widget.type(), property.name()), false,
                    name.equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : name.equals("style") ? java.util.List.of("local") : java.util.List.of()));
            }
            sets.values().forEach(sheet::put);
        } else if (TextWidgetPropertySchema.TEXT_TYPE.equals(widget.type())) {
            addTextPropertySets(sheet, hasSlotTab);
        } else if (IconWidgetPropertySchema.ICON_TYPE.equals(widget.type())) {
            addIconPropertySets(sheet, hasSlotTab);
        } else if (dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE.equals(widget.type())) {
            var set = propertySet("flexibleSpaceBarSettingsProperties", "Flexible space settings", dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarSettingsWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarSettingsWidgetPropertySchema.FIELDS) {
                var property = definition.property(new PropertyName(field.name())).orElseThrow();
                set.put(projectProperty(property, Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarWidgetPropertySchema.TYPE.equals(widget.type())) {
            var set = propertySet("flexibleSpaceBarProperties", "Flexible space", dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarWidgetPropertySchema.FIELDS) {
                var property = definition.property(new PropertyName(field.name())).orElseThrow();
                set.put(projectProperty(property, Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("stretchModes") ? dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarWidgetPropertySchema.STRETCH_PRESETS : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(widget.type())) {
            addSliverAppBarPropertySets(sheet, hasSlotTab);
        } else if (AppBarWidgetPropertySchema.APP_BAR_TYPE.equals(widget.type())) {
            addAppBarPropertySets(sheet, hasSlotTab);
        } else if (ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE.equals(
                widget.type())) {
            addElevatedButtonPropertySets(sheet, hasSlotTab);
        } else if (TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE.equals(widget.type())) {
            addTextButtonPropertySets(sheet, hasSlotTab);
        } else if (MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(widget.type())) {
            addMenuItemButtonPropertySets(sheet, hasSlotTab);
        } else if (MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE.equals(widget.type())) {
            addMenuAnchorPropertySets(sheet, hasSlotTab);
        } else if (SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.equals(widget.type())) {
            addSubmenuButtonPropertySets(sheet, hasSlotTab);
        } else if (MenuBarWidgetPropertySchema.MENU_BAR_TYPE.equals(widget.type())) {
            addMenuBarPropertySets(sheet, hasSlotTab);
        } else if (NavigationBarWidgetPropertySchema.NAVIGATION_BAR_TYPE.equals(widget.type())) {
            addNavigationBarPropertySets(sheet, hasSlotTab);
        } else if (NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE.equals(widget.type())) {
            addNavigationRailPropertySets(sheet, hasSlotTab);
        } else if (NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE.equals(widget.type())) {
            addNavigationDrawerPropertySets(sheet, hasSlotTab);
        } else if (DrawerWidgetPropertySchema.DRAWER_TYPE.equals(widget.type())) {
            addDrawerPropertySets(sheet, hasSlotTab);
        } else if (BottomAppBarWidgetPropertySchema.BOTTOM_APP_BAR_TYPE.equals(widget.type())) {
            addBottomAppBarPropertySets(sheet, hasSlotTab);
        } else if (BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE.equals(widget.type())) {
            addBottomNavigationBarPropertySets(sheet, hasSlotTab);
      } else if (MaterialWidgetPropertySchema.MATERIAL_TYPE.equals(widget.type())) {
          addMaterialPropertySets(sheet, hasSlotTab);
      } else if (ScrollbarWidgetPropertySchema.SCROLLBAR_TYPE.equals(widget.type())) {
          addScrollbarPropertySets(sheet, hasSlotTab);
      } else if (OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(widget.type())) {
            addOutlinedButtonPropertySets(sheet, hasSlotTab);
        } else if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(widget.type())) {
            addIconButtonPropertySets(sheet, hasSlotTab);
        } else if (RadioWidgetPropertySchema.RADIO_TYPE.equals(widget.type())) {
            addRadioPropertySets(sheet, hasSlotTab);
        } else if (RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.equals(widget.type())) {
            addRadioListTilePropertySets(sheet, hasSlotTab);
        } else if (ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE.equals(widget.type())) {
            addExpansionTilePropertySets(sheet, hasSlotTab);
        } else if (TooltipWidgetPropertySchema.TOOLTIP_TYPE.equals(widget.type())) {
            addTooltipPropertySets(sheet, hasSlotTab);
        } else if (TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE.equals(widget.type())) {
            addTooltipVisibilityPropertySets(sheet, hasSlotTab);
        } else if (TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE.equals(widget.type())) {
            addTooltipThemePropertySets(sheet, hasSlotTab);
        } else if (CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE.equals(widget.type())) {
            addCheckboxListTilePropertySets(sheet, hasSlotTab);
        } else if (SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE.equals(widget.type())) {
            addSwitchListTilePropertySets(sheet, hasSlotTab);
        } else if (ListTileWidgetPropertySchema.LIST_TILE_TYPE.equals(widget.type())) {
            addListTilePropertySets(sheet, hasSlotTab);
        } else if (RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(widget.type())) {
            addRadioGroupPropertySets(sheet, hasSlotTab);
        } else if (RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE.equals(widget.type())) {
            addRangeSliderPropertySets(sheet, hasSlotTab);
        } else if (SliderWidgetPropertySchema.SLIDER_TYPE.equals(widget.type())) {
            addSliderPropertySets(sheet, hasSlotTab);
        } else if (SwitchWidgetPropertySchema.SWITCH_TYPE.equals(widget.type())) {
            addSwitchPropertySets(sheet, hasSlotTab);
        } else if (CheckboxWidgetPropertySchema.CHECKBOX_TYPE.equals(widget.type())) {
            addCheckboxPropertySets(sheet, hasSlotTab);
        } else if (FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(widget.type())) {
            addFilledButtonPropertySets(sheet, hasSlotTab);
        } else if (FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE.equals(widget.type())) {
            addFloatingActionButtonPropertySets(sheet, hasSlotTab);
        } else if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(widget.type())) {
            addTextFieldPropertySets(sheet, hasSlotTab);
        } else if (ListViewWidgetPropertySchema.LIST_VIEW_TYPE.equals(widget.type())) {
            addListViewPropertySets(sheet, hasSlotTab);
        } else if (GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.equals(
                widget.type())) {
            addGridViewCountPropertySets(sheet, hasSlotTab);
        } else if (GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE.equals(
                widget.type())) {
            addGridViewExtentPropertySets(sheet, hasSlotTab);
        } else if (SingleChildScrollViewWidgetPropertySchema
                .SINGLE_CHILD_SCROLL_VIEW_TYPE.equals(widget.type())) {
            addSingleChildScrollViewPropertySets(sheet, hasSlotTab);
        } else if (PageViewWidgetPropertySchema.PAGE_VIEW_TYPE.equals(widget.type())) {
            addPageViewPropertySets(sheet, hasSlotTab);
        } else if (ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE.equals(widget.type())) {
            addListWheelScrollViewPropertySets(sheet, hasSlotTab);
        } else if (dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(widget.type()).isPresent()) {
            var kind = dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(widget.type()).orElseThrow();
            Sheet.Set set = propertySet("sliverPrototypeExtentList", "Prototype-sized children", "Native prototype-measured list, builder or delegate.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.fields(kind)) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(), Optional.empty(),
                        field.displayName(), dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.description(field),
                        false, field.presets()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.find(widget.type()).isPresent()) {
            var kind = dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.find(widget.type()).orElseThrow();
            Sheet.Set set = propertySet("sliverFixedExtentList", "Fixed-extent children", "Native fixed-extent list, builder or delegate.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.fields(kind)) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(), Optional.empty(),
                        field.displayName(), dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.description(field),
                        false, field.presets()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.find(widget.type()).isPresent()) {
            var kind = dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.find(widget.type()).orElseThrow();
            Sheet.Set set = propertySet("sliverVariedExtentList", "Variable-extent children", "Native variable-extent list, builder or delegate.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.fields(kind)) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(), Optional.empty(),
                        field.displayName(), dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.description(field),
                        false, field.presets()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.TYPES.contains(widget.type())) {
            Sheet.Set set = propertySet("sliverFillViewport", "Viewport and children", "Native viewport-sized sliver children.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.fields(widget.type())) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.presets(field.name())));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.find(widget.type()).isPresent()) {
            addDynamicSliverPropertySet(sheet, hasSlotTab);
        } else if (dev.flutter.netbeans.designer.catalog.SliverPaddingWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverPadding", "Sliver padding", "Insets and optional nested sliver.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("padding")).orElseThrow(), Optional.empty(),
                    "Padding", dev.flutter.netbeans.designer.catalog.SliverPaddingWidgetPropertySchema.DESCRIPTION));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverConstrainedCrossAxisWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverConstrainedCrossAxis", "Cross-axis layout", "Maximum sliver cross-axis extent.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("maxExtent")).orElseThrow(), Optional.empty(),
                    "Max extent", dev.flutter.netbeans.designer.catalog.SliverConstrainedCrossAxisWidgetPropertySchema.DESCRIPTION));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverCrossAxisExpanded", "Cross-axis layout", "Proportional sliver cross-axis allocation.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("flex")).orElseThrow(), Optional.empty(),
                    "Flex", dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.DESCRIPTION));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.supports(widget.type())) {
            boolean maintain = dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.MAINTAIN_TYPE.equals(widget.type());
            Sheet.Set set = propertySet("sliverVisibility", "Visibility", dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.DESCRIPTION
                    + (maintain ? " The maintain constructor fixes all five maintenance flags to true, including semantics and pointer interaction." : ""));
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.fields(maintain)) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE.equals(widget.type())) {
            var set = propertySet("sliverFloatingHeader", "Floating header", dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.FIELDS) {
                var presets = field.name().equals("animationStyle") ? java.util.List.of("noAnimation")
                        : field.name().endsWith("Curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.<String>of();
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false, presets));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverPersistentHeader", "Persistent header",
                    dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("layoutBuilder", "Layout builder",
                    dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("builder")).orElseThrow(),
                    Optional.empty(), "Builder", dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.DESCRIPTION,
                    false, java.util.List.of("empty")));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("orientationBuilder", "Orientation builder",
                    dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("builder")).orElseThrow(),
                    Optional.empty(), "Builder", dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.DESCRIPTION,
                    false, java.util.List.of("empty")));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type())) {
            String description = dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.description(widget.type());
            Sheet.Set set = propertySet("valueListenableBuilder", "Value listenable builder", description);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (String name : java.util.List.of("valueType", "nullableValueType", "valueListenable", "builder")) {
                String label = switch (name) { case "valueType" -> "Value type"; case "nullableValueType" -> "Nullable value type";
                    case "valueListenable" -> "Value listenable"; default -> "Builder"; };
                set.put(projectProperty(definition.property(new PropertyName(name)).orElseThrow(),
                        Optional.empty(), label, description, false,
                        name.equals("valueListenable") ? java.util.List.of("constant") : name.equals("builder") ? java.util.List.of("child") : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type())) {
            String description = dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.description(widget.type());
            Sheet.Set set = propertySet("tweenAnimationBuilder", "Tween animation builder", description);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (String name : java.util.List.of("valueType", "nullableValueType", "tween", "builder", "durationUs", "curve", "onEnd")) {
                String label = switch (name) { case "valueType" -> "Value type"; case "nullableValueType" -> "Nullable value type";
                    case "tween" -> "Tween"; case "durationUs" -> "Duration (microseconds)"; case "curve" -> "Curve"; case "onEnd" -> "On end"; default -> "Builder"; };
                set.put(projectProperty(definition.property(new PropertyName(name)).orElseThrow(),
                        Optional.empty(), label, description, false,
                        name.equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : name.equals("tween") ? java.util.List.of("default") : name.equals("builder") ? java.util.List.of("child") : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.supports(widget.type())) {
            String description = dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.description(widget.type());
            Sheet.Set set = propertySet("animatedBuilder", "Animated builder", description);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("animation")).orElseThrow(),
                    Optional.empty(), "Animation", description, false, java.util.List.of("none")));
            set.put(projectProperty(definition.property(new PropertyName("builder")).orElseThrow(),
                    Optional.empty(), "Builder", description, false, java.util.List.of("child")));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.supports(widget.type())) {
            String description = dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.description(widget.type());
            Sheet.Set set = propertySet("listenableBuilder", "Listenable builder", description);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("listenable")).orElseThrow(),
                    Optional.empty(), "Listenable", description, false, java.util.List.of("none")));
            set.put(projectProperty(definition.property(new PropertyName("builder")).orElseThrow(),
                    Optional.empty(), "Builder", description, false, java.util.List.of("child")));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.supports(widget.type())) {
            Sheet.Set set = propertySet("deviceOrientationBuilder", "Device orientation builder",
                    dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.description(widget.type()));
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("builder")).orElseThrow(),
                    Optional.empty(), "Builder", dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.description(widget.type()),
                    false, java.util.List.of("empty")));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverLayoutBuilder", "Layout builder",
                    dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("builder")).orElseThrow(),
                    Optional.empty(), "Builder", dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.DESCRIPTION,
                    false, java.util.List.of("empty")));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedSlide", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedScale", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(widget.type())) {
            Sheet.Set set = propertySet("animatedPositioned", "Position and animation",
                    dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.description(widget.type()));
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.fields(widget.type())) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets()
                            : field.name().equals("rect") ? java.util.List.of("local") : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedSize", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedContainer", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedRotation", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedPadding", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedAlign", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedFractionallySizedBox", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedCrossFade", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().endsWith("Curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedSwitcher", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().endsWith("Curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.RotationTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("rotationTransition", "Animation", dev.flutter.netbeans.designer.catalog.RotationTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.RotationTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("positionedTransition", "Animation", dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("rect") ? java.util.List.of("local") : java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.DecoratedBoxTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("decoratedBoxTransition", "Animation", dev.flutter.netbeans.designer.catalog.DecoratedBoxTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.DecoratedBoxTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AlignTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("alignTransition", "Animation", dev.flutter.netbeans.designer.catalog.AlignTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AlignTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.isFilter(widget.type())
                || dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.GROUP.equals(widget.type())) {
            var fields = dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.fields(widget.type());
            for (String group : fields.stream().map(dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.Field::group).distinct().toList()) {
                Sheet.Set set = propertySet("backdrop" + group, group, dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : fields) if (field.group().equals(group)) {
                    var presets = field.name().equals("filter") ? dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.FILTERS
                            : field.name().equals("filterConfig") ? dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.CONFIGS : java.util.List.<String>of();
                    set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, presets));
                }
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Filter", "Blur", "Blur bounds", "Morphology", "Matrix", "Composition", "Shader")) {
                Sheet.Set set = propertySet("imageFiltered" + group, group, dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.FIELDS)
                    if (field.group().equals(group)) set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, field.name().equals("imageFilter")
                                    ? dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.FILTERS : java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Filter", "Blend", "Matrix 4 x 5", "Saturation")) {
                Sheet.Set set=propertySet("colorFiltered"+group,group,dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.DESCRIPTION);
                assignTab(set,hasSlotTab?GENERAL_TAB_NAME:null);
                for(var field:dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.FIELDS)
                    if(field.group().equals(group))set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(),field.label(),field.description(),false,field.name().equals("colorFilter")
                                    ?dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.FILTERS:java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.RawImageWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Image", "Layout", "Appearance")) {
                Sheet.Set set = propertySet("rawImage" + group, group, dev.flutter.netbeans.designer.catalog.RawImageWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : dev.flutter.netbeans.designer.catalog.RawImageWidgetPropertySchema.FIELDS)
                    if (field.group().equals(group)) set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Images", "Animation", "Appearance", "Layout", "Accessibility")) {
                Sheet.Set set = propertySet("fadeInImage" + group, group, dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.FIELDS)
                    if (field.group().equals(group)) set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, field.name().endsWith("Curve") ? dev.flutter.netbeans.designer.catalog.ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Icon", "Appearance", "Accessibility")) {
                Sheet.Set set = propertySet("animatedIcon" + group, group, dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.FIELDS)
                    if (field.group().equals(group)) set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, field.name().equals("icon") ? dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.ICONS : java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedModalBarrierWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Appearance", "Behavior", "Semantics", "Events")) {
                Sheet.Set set = propertySet("animatedModalBarrier" + group, group, dev.flutter.netbeans.designer.catalog.AnimatedModalBarrierWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : dev.flutter.netbeans.designer.catalog.AnimatedModalBarrierWidgetPropertySchema.FIELDS)
                    if (field.group().equals(group)) set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.ModalBarrierWidgetPropertySchema.TYPE.equals(widget.type())) {
            for (String group : java.util.List.of("Appearance", "Behavior", "Semantics", "Events")) {
                Sheet.Set set = propertySet("modalBarrier" + group, group, dev.flutter.netbeans.designer.catalog.ModalBarrierWidgetPropertySchema.DESCRIPTION);
                assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
                for (var field : dev.flutter.netbeans.designer.catalog.ModalBarrierWidgetPropertySchema.FIELDS)
                    if (field.group().equals(group)) set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                            Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
                sheet.put(set);
            }
        } else if (dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("datePickerDialog" + group, group, dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.help(name),
                        false, dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("calendarDatePicker" + group, group, dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.help(name),
                        false, dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("inputDatePickerFormField" + group, group, dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.help(name),
                        false, dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if ((dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.supports(widget.type()) || dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.supports(widget.type()))) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("alertDialog" + group, group, (dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.supports(widget.type()) ? (dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.OPTION_TYPE.equals(widget.type()) ? dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.OPTION_DESCRIPTION : dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.DESCRIPTION) : dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.DESCRIPTION));
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.styleBinding(field.name()),
                        dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.label(name),
                        (dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.supports(widget.type()) ? dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.help(name) : dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.help(name)),
                        false, dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("materialBanner" + group, group, dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.styleBinding(field.name()),
                        dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.help(name), false,
                        dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.supports(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("snackBar" + group, group, dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        name.equals("durationUs") ? "Duration (microseconds)" : dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.help(name), false,
                        name.equals("onPressed") ? java.util.List.of("noop") : dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                String group = name.equals("builder") ? "Content" : name.startsWith("on") ? "Events"
                        : name.equals("animationController") || name.equals("enableDrag") || name.equals("showDragHandle")
                                ? "Behavior" : dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.group(name);
                var set = groups.computeIfAbsent(group, key -> {
                    var result = propertySet("bottomSheet" + key, key, dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.help(name), false,
                        name.equals("builder") ? java.util.List.of("child") : name.equals("onClosing") ? java.util.List.of("noop")
                                : dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.supports(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("dialog" + group, group, dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.help(name),
                        false, dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("timePickerDialog" + group, group, dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.help(name),
                        false, dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.TYPE.equals(widget.type())) {
            var groups = new java.util.LinkedHashMap<String, Sheet.Set>();
            for (var field : definition.properties()) {
                String name = field.name().value();
                var set = groups.computeIfAbsent(dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.group(name), group -> {
                    var result = propertySet("dateRangePickerDialog" + group, group, dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.DESCRIPTION);
                    assignTab(result, hasSlotTab ? GENERAL_TAB_NAME : null);
                    return result;
                });
                set.put(projectProperty(field, Optional.empty(),
                        dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.label(name),
                        dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.help(name),
                        false, dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.presets(name)));
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.supports(widget.type())) {
            var groups = new java.util.LinkedHashMap<String,Sheet.Set>();
            for (var field : dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.fields(widget.type())) {
                var set=groups.computeIfAbsent(field.group(), group -> {
                    var result=propertySet("dataTable"+group,group,field.help());
                    assignTab(result,hasSlotTab ? GENERAL_TAB_NAME : null); return result;
                });
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.styleBinding(widget.type(),new PropertyName(field.name())),
                        field.label(),field.help(),false,field.name().equals("border") ? java.util.List.of("all","symmetric","custom")
                        : dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.styleFamilies(widget.type()).contains(field.name())
                            || dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.stateFamilies(widget.type()).contains(field.name()) ? java.util.List.of("local")
                        : dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.callbacks(widget.type()).containsKey(field.name()) ? java.util.List.of("noop")
                        : dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.stateFamily(widget.type(),field.name()).filter("mouseCursor"::equals).isPresent()
                            ? dev.flutter.netbeans.designer.catalog.DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets() : java.util.List.of()));
            }
            if (dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.table(widget.type())) {
                Sheet.Set grid=propertySet("dataTableGrid","Table grid","Atomic edits retain cell identities and support Undo/Redo.");
                assignTab(grid,hasSlotTab ? GENERAL_TAB_NAME : null);
                grid.put(new PropertySupport.ReadWrite<dev.flutter.netbeans.designer.command.EditDataTableGrid>(
                        "dataTableGrid",dev.flutter.netbeans.designer.command.EditDataTableGrid.class,
                        dev.flutter.netbeans.designer.catalog.PaginatedDataTableWidgetPropertySchema.TYPE.equals(widget.type()) ? "Columns" : "Rows and columns",
                        "Add, remove or reorder columns. PaginatedDataTable source rows are application-owned: update their cells to match column changes. DataTable column removal deletes every cell in that column.") {
                    @Override public boolean canWrite() { return presentation.mutationHandler()!=null; }
                    @Override public dev.flutter.netbeans.designer.command.EditDataTableGrid getValue() { return null; }
                    @Override public void setValue(dev.flutter.netbeans.designer.command.EditDataTableGrid value) {
                        if (!canWrite()) throw new IllegalStateException("Table grid is read-only");
                        if (value!=null) presentation.mutationHandler().submit(value);
                    }
                    @Override public java.beans.PropertyEditor getPropertyEditor() {
                        return new FlutterDataTableGridPropertyEditor(presentation.widget());
                    }
                });
                sheet.put(grid);
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.supports(widget.type())) {
            var groups = new java.util.LinkedHashMap<String,Sheet.Set>();
            for (var field : dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.fields(widget.type())) {
                var set=groups.computeIfAbsent(field.group(), group -> {
                    var result=propertySet("table"+group,group,field.help());
                    assignTab(result,hasSlotTab ? GENERAL_TAB_NAME : null); return result;
                });
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),Optional.empty(),
                        field.label(),field.help(),false,field.name().equals("border") ? java.util.List.of("all","symmetric","custom") : java.util.List.of()));
            }
            if (dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.TYPE.equals(widget.type())) {
                Sheet.Set grid=propertySet("tableGrid","Table grid","Atomic edits retain cell identities and support Undo/Redo.");
                assignTab(grid,hasSlotTab ? GENERAL_TAB_NAME : null);
                grid.put(new PropertySupport.ReadWrite<dev.flutter.netbeans.designer.command.EditTableGrid>(
                        "tableGrid",dev.flutter.netbeans.designer.command.EditTableGrid.class,"Rows and columns",
                        "Add, remove or reorder complete rows/columns. Column removal deletes every cell in that column.") {
                    @Override public boolean canWrite() { return presentation.mutationHandler()!=null; }
                    @Override public dev.flutter.netbeans.designer.command.EditTableGrid getValue() { return null; }
                    @Override public void setValue(dev.flutter.netbeans.designer.command.EditTableGrid value) {
                        if (!canWrite()) throw new IllegalStateException("Table grid is read-only");
                        if (value!=null) presentation.mutationHandler().submit(value);
                    }
                    @Override public java.beans.PropertyEditor getPropertyEditor() {
                        return new FlutterTableGridPropertyEditor(presentation.widget());
                    }
                });
                sheet.put(grid);
            }
            groups.values().forEach(sheet::put);
        } else if (dev.flutter.netbeans.designer.catalog.LayoutIdWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("layoutId", "Layout identity", dev.flutter.netbeans.designer.catalog.LayoutIdWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("id")).orElseThrow(), Optional.empty(),
                    "ID", dev.flutter.netbeans.designer.catalog.LayoutIdWidgetPropertySchema.DESCRIPTION, false, java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("customMultiChildLayout", "Custom layout", dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            set.put(projectProperty(definition.property(new PropertyName("delegate")).orElseThrow(), Optional.empty(),
                    "Delegate", dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.DESCRIPTION, false, java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.FlowWidgetPropertySchema.isFlow(widget.type())) {
            Sheet.Set set = propertySet("flow", "Flow layout", dev.flutter.netbeans.designer.catalog.FlowWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.FlowWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("customSingleChildLayout", "Custom layout", dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("customPaint", "Painting", dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.ShaderMaskWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("shaderMask", "Shader", dev.flutter.netbeans.designer.catalog.ShaderMaskWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.ShaderMaskWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false, java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("matrixTransition", "Animation", dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("relativePositionedTransition", "Animation", dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("rect") ? java.util.List.of("local", "null")
                            : field.name().equals("size") ? java.util.List.of("local") : java.util.List.of()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sizeTransition", "Animation", dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.ScaleTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("scaleTransition", "Animation", dev.flutter.netbeans.designer.catalog.ScaleTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.ScaleTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SlideTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("slideTransition", "Animation", dev.flutter.netbeans.designer.catalog.SlideTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SlideTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.supports(widget.type())) {
            Sheet.Set set = propertySet("fadeTransition", "Animation", dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.FIELDS)
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("animatedOpacity", "Animation", dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverAnimatedOpacity", "Animation", dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description(), false,
                        field.name().equals("curve") ? ExpansionTileWidgetPropertySchema.curvePresets() : java.util.List.of()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverSafeAreaWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverSafeArea", "Safe area", dev.flutter.netbeans.designer.catalog.SliverSafeAreaWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverSafeAreaWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverOffstageWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverOffstage", "Visibility", dev.flutter.netbeans.designer.catalog.SliverOffstageWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverOffstageWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverIgnorePointerWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverIgnorePointer", "Pointer behavior", dev.flutter.netbeans.designer.catalog.SliverIgnorePointerWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverIgnorePointerWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverOpacityWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverOpacity", "Transparency", dev.flutter.netbeans.designer.catalog.SliverOpacityWidgetPropertySchema.DESCRIPTION);
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverOpacityWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (dev.flutter.netbeans.designer.catalog.SliverFillRemainingWidgetPropertySchema.TYPE.equals(widget.type())) {
            Sheet.Set set = propertySet("sliverFillRemaining", "Remaining space", "Native viewport filling and overscroll behavior.");
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (var field : dev.flutter.netbeans.designer.catalog.SliverFillRemainingWidgetPropertySchema.FIELDS) {
                set.put(projectProperty(definition.property(new PropertyName(field.name())).orElseThrow(),
                        Optional.empty(), field.label(), field.description()));
            }
            sheet.put(set);
        } else if (SliverChildrenWidgetPropertySchema.TYPES.contains(widget.type())) {
            addSliverChildrenPropertySet(sheet, hasSlotTab);
        } else if (CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE.equals(widget.type())) {
            addCustomScrollViewPropertySets(sheet, hasSlotTab);
        } else if (PreferredSizeWidgetPropertySchema.PREFERRED_SIZE_TYPE.equals(widget.type())) {
            addPreferredSizePropertySets(sheet, hasSlotTab);
        } else if (BuilderWidgetPropertySchema.BUILDER_TYPE.equals(widget.type())) {
            addBuilderPropertySets(sheet, hasSlotTab);
        } else if (ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE.equals(
                widget.type())) {
            addColoredBoxPropertySets(sheet, hasSlotTab);
        } else if (PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE.equals(
                widget.type())) {
            addPlaceholderPropertySets(sheet, hasSlotTab);
        } else if (SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE.equals(
                widget.type())) {
            addSafeAreaPropertySets(sheet, hasSlotTab);
        } else if (DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.equals(
                widget.type())) {
            addDirectionalityPropertySets(sheet, hasSlotTab);
        } else if (DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.equals(
                widget.type())) {
            addDecoratedBoxPropertySets(sheet, hasSlotTab);
        } else if (IgnorePointerWidgetPropertySchema.IGNORE_POINTER_TYPE.equals(widget.type())) {
            addIgnorePointerPropertySets(sheet, hasSlotTab);
        } else if (GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE.equals(widget.type())) {
            addGestureDetectorPropertySets(sheet, hasSlotTab);
        } else if (ListenerWidgetPropertySchema.LISTENER_TYPE.equals(widget.type())) {
            addListenerPropertySets(sheet, hasSlotTab);
        } else if (MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE.equals(widget.type())) {
            addMouseRegionPropertySets(sheet, hasSlotTab);
        } else if (FocusWidgetPropertySchema.FOCUS_TYPE.equals(widget.type())) {
            addFocusPropertySets(sheet, hasSlotTab);
        } else if (NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE.equals(widget.type())) {
            addNotificationListenerPropertySets(sheet, hasSlotTab);
        } else if (AbsorbPointerWidgetPropertySchema.ABSORB_POINTER_TYPE.equals(widget.type())) {
            addAbsorbPointerPropertySets(sheet, hasSlotTab);
        } else if (ExcludeFocusWidgetPropertySchema.EXCLUDE_FOCUS_TYPE.equals(widget.type())) {
            addExcludeFocusPropertySets(sheet, hasSlotTab);
        } else if (ExcludeFocusTraversalWidgetPropertySchema.EXCLUDE_FOCUS_TRAVERSAL_TYPE.equals(widget.type())) {
            addExcludeFocusTraversalPropertySets(sheet, hasSlotTab);
        } else if (VisibilityWidgetPropertySchema.VISIBILITY_TYPE.equals(widget.type())) {
            addVisibilityPropertySets(sheet, hasSlotTab);
        } else if (TickerModeWidgetPropertySchema.TICKER_MODE_TYPE.equals(widget.type())) {
            addTickerModePropertySets(sheet, hasSlotTab);
        } else if (DefaultTextHeightBehaviorWidgetPropertySchema.DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE.equals(widget.type())) {
            addDefaultTextHeightBehaviorPropertySets(sheet, hasSlotTab);
        } else if (DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE.equals(widget.type())) {
            addDefaultSelectionStylePropertySets(sheet, hasSlotTab);
        } else if (IconThemeWidgetPropertySchema.ICON_THEME_TYPE.equals(widget.type())) {
            addIconThemePropertySets(sheet, hasSlotTab);
        } else if (ImageIconWidgetPropertySchema.IMAGE_ICON_TYPE.equals(widget.type())) {
            addImageIconPropertySets(sheet, hasSlotTab);
        } else if (DividerWidgetPropertySchema.DIVIDER_TYPE.equals(widget.type())) {
            addDividerPropertySets(sheet, hasSlotTab);
        } else if (VerticalDividerWidgetPropertySchema.VERTICAL_DIVIDER_TYPE.equals(widget.type())) {
            addVerticalDividerPropertySets(sheet, hasSlotTab);
        } else if (CardWidgetPropertySchema.CARD_TYPE.equals(widget.type())) {
            addCardPropertySets(sheet, hasSlotTab);
        } else if (BadgeWidgetPropertySchema.BADGE_TYPE.equals(widget.type())) {
            addBadgePropertySets(sheet, hasSlotTab);
        } else if (CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE.equals(widget.type())) {
            addCircleAvatarPropertySets(sheet, hasSlotTab);
        } else if (LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE.equals(widget.type())) {
            addLinearProgressIndicatorPropertySets(sheet, hasSlotTab);
        } else if (CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE.equals(widget.type())) {
            addCircularProgressIndicatorPropertySets(sheet, hasSlotTab);
        } else if (RefreshProgressIndicatorWidgetPropertySchema.REFRESH_PROGRESS_INDICATOR_TYPE.equals(widget.type())) {
            addRefreshProgressIndicatorPropertySets(sheet, hasSlotTab);
        } else if (RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.equals(widget.type())) {
            addRefreshIndicatorPropertySets(sheet, hasSlotTab);
        } else if (IndexedSemanticsWidgetPropertySchema.INDEXED_SEMANTICS_TYPE.equals(widget.type())) {
            addIndexedSemanticsPropertySets(sheet, hasSlotTab);
        } else if (BlockSemanticsWidgetPropertySchema.BLOCK_SEMANTICS_TYPE.equals(widget.type())) {
            addBlockSemanticsPropertySets(sheet, hasSlotTab);
        } else if (ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE.equals(
                widget.type())) {
            addExcludeSemanticsPropertySets(sheet, hasSlotTab);
        } else if (IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.equals(
                widget.type())) {
            addIndexedStackPropertySets(sheet, hasSlotTab);
        } else if (ClipRectWidgetPropertySchema.CLIP_RECT_TYPE.equals(
                widget.type())) {
            addClipRectPropertySets(sheet, hasSlotTab);
        } else if (ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE.equals(
                widget.type())) {
            addClipOvalPropertySets(sheet, hasSlotTab);
        } else if (ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE.equals(
                widget.type())) {
            addClipRRectPropertySets(sheet, hasSlotTab);
        } else if (ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.equals(
                widget.type())) {
            addClipPathPropertySets(sheet, hasSlotTab);
        } else if (PhysicalShapeWidgetPropertySchema.PHYSICAL_SHAPE_TYPE.equals(widget.type())) {
            addPhysicalShapePropertySets(sheet, hasSlotTab);
        } else if (PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE.equals(
                widget.type())) {
            addPhysicalModelPropertySets(sheet, hasSlotTab);
        } else if (ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE.equals(
                definition.typeId())) {
            addClipRSuperellipsePropertySets(sheet, hasSlotTab);
        } else if (ContainerWidgetPropertySchema.CONTAINER_TYPE.equals(widget.type())) {
            addContainerPropertySets(sheet, hasSlotTab);
        } else {
            Sheet.Set properties = createGenericPropertySet();
            assignTab(properties, hasSlotTab ? GENERAL_TAB_NAME : null);
            sheet.put(properties);
        }
        addEventsPropertySet(sheet);
        if (hasSlotTab) {
            Sheet.Set slots = createSlotsPropertySet();
            assignTab(slots, SLOTS_TAB_NAME);
            sheet.put(slots);
        }
        return sheet;
    }

    private void addEventsPropertySet(Sheet sheet) {
        if (events.isEmpty()) return;
        Sheet.Set eventSet = propertySet(EVENTS_SET_NAME, "Events",
                "Typed event callback bindings. Handler bodies remain in the Dart source; each event retains its declared signature and unset behavior.");
        for (WidgetEventDescriptor event : events) {
            for (Node.PropertySet set : sheet.toArray()) {
                Sheet.Set group = (Sheet.Set) set;
                Node.Property<?> row = group.remove(event.propertyName().value());
                if (row != null) {
                    row.setValue("eventSignature", event.signature().toString());
                    row.setValue("eventUnsetBehavior", event.unsetBehavior());
                    eventSet.put(row);
                    break;
                }
            }
        }
        for (Node.PropertySet set : sheet.toArray()) {
            Sheet.Set group = (Sheet.Set) set;
            if (group.getProperties().length == 0) sheet.remove(group.getName());
            else assignTab(group, GENERAL_TAB_NAME);
        }
        assignTab(eventSet, EVENTS_TAB_NAME);
        sheet.put(eventSet);
    }

    private static void assignTab(Sheet.Set set, String tabName) {
        if (tabName != null) {
            // This is the standard NetBeans PropertySheet grouping contract.
            // Sets with the same tabName share one native PropertySheet tab.
            set.setValue(TAB_NAME_ATTRIBUTE, tabName);
        }
    }

    private Sheet.Set createSlotsPropertySet() {
        Sheet.Set slots = propertySet(
                SLOTS_SET_NAME,
                "Slots",
                "Exact named child slots declared by the widget catalog.");
        for (SlotDefinition slot : definition.slots()) {
            WidgetSlot modelSlot = widget.slots().get(slot.name());
            String summary = slotSummary(slot, modelSlot);
            String description = slotDescription(slot, modelSlot);
            if (slotMutationProjection
                    && presentation.slotEditorContext() != null) {
                slots.put(writableSlotProperty(slot, summary, description));
            } else {
                slots.put(readOnly(
                        slot.name().value(),
                        displayName(slot.name()),
                        description,
                        summary));
            }
        }
        return slots;
    }

    private PropertySupport.ReadWrite<FlutterWidgetSlotCellValue>
            writableSlotProperty(
                    SlotDefinition slot,
                    String summary,
                    String description) {
        PropertySupport.ReadWrite<FlutterWidgetSlotCellValue> result =
                new PropertySupport.ReadWrite<>(
                slot.name().value(),
                FlutterWidgetSlotCellValue.class,
                displayName(slot.name()),
                description) {
            @Override
            public FlutterWidgetSlotCellValue getValue() {
                Presentation current = presentation;
                return FlutterWidgetSlotCellValue.current(
                        slotSummary(
                                slot,
                                current.widget().slots().get(slot.name()),
                                current.slotEditorContext()));
            }

            @Override
            public void setValue(FlutterWidgetSlotCellValue value)
                    throws IllegalAccessException {
                Objects.requireNonNull(value, "value");
                value.mutation().ifPresent(mutation -> {
                    Presentation current = presentation;
                    if (!current.widget().id().equals(mutation.ownerId())
                            || !slot.name().equals(mutation.slotName())) {
                        throw new IllegalArgumentException(
                                "Slot edit targets another widget or named slot.");
                    }
                    SlotMutationHandler currentHandler =
                            current.slotMutationHandler();
                    AtomicBoolean submitted =
                            current.slotMutationGates().get(slot.name());
                    if (currentHandler != null
                            && submitted != null
                            && submitted.compareAndSet(false, true)) {
                        currentHandler.submit(mutation);
                    }
                });
            }

            @Override
            public boolean canWrite() {
                return presentation.slotMutationHandler() != null;
            }

            @Override
            public PropertyEditor getPropertyEditor() {
                Presentation current = presentation;
                return new FlutterWidgetSlotPropertyEditor(
                        current.widget(),
                        current.definition(),
                        slot,
                        current.slotEditorContext());
            }
        };
        result.setValue("changeImmediate", Boolean.FALSE);

        result.setValue("canEditAsText", Boolean.FALSE);
        return result;
    }

    private String slotSummary(SlotDefinition slot, WidgetSlot value) {
        return slotSummary(slot, value, presentation.slotEditorContext());
    }

    private String slotSummary(
            SlotDefinition slot,
            WidgetSlot value,
            FlutterWidgetSlotEditorContext currentSlotEditorContext) {
        if (value == null) {
            return "Empty";
        }
        if (value.cardinality() != slot.cardinality()) {
            return "Invalid " + value.cardinality().wireName() + " slot";
        }
        return switch (value) {
            case WidgetSlot.SingleSlot single -> single.child()
                    .map(child -> widgetDisplayName(
                            child, currentSlotEditorContext))
                    .orElse("Empty");
            case WidgetSlot.ListSlot list -> list.children().isEmpty()
                    ? "Empty"
                    : list.children().size() == 1
                            ? "1 widget"
                            : list.children().size() + " widgets";
        };
    }

    private String widgetDisplayName(
            WidgetNode child,
            FlutterWidgetSlotEditorContext currentSlotEditorContext) {
        if (currentSlotEditorContext != null) {
            return currentSlotEditorContext.catalog().find(child.type())
                    .map(value -> value.palette().displayName())
                    .orElseGet(() -> displayType(child));
        }
        return displayType(child);
    }

    private static String displayType(WidgetNode child) {
        String type = child.type().value();
        int separator = type.lastIndexOf('.');
        return separator < 0 ? type : type.substring(separator + 1);
    }

    private String slotDescription(
            SlotDefinition slot,
            WidgetSlot value) {
        int count = value == null ? 0 : switch (value) {
            case WidgetSlot.SingleSlot single -> single.child().isPresent() ? 1 : 0;
            case WidgetSlot.ListSlot list -> list.children().size();
        };
        if (dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.CHILD_DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.PinnedHeaderSliverWidgetSchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.PinnedHeaderSliverWidgetSchema.CHILD_DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverResizingHeaderWidgetSchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverResizingHeaderWidgetSchema.slotDescription(slot.name().value());
        }
        if (dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.find(widget.type()).isPresent()
                && slot.name().value().equals("prototypeItem")) {
            return "Measurement-only box widget: the SDK lays it out but does not paint it or accept input. "
                    + "Edit it through the widget tree or Slots. Empty uses the explicit Designer SizedBox(48 x 48) preset, not null or an SDK default. "
                    + "Add, replace, move or clear the prototype independently of visible children; content and identities are preserved.";
        }
        if (dev.flutter.netbeans.designer.catalog.SliverMainAxisGroupWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverMainAxisGroupWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverCrossAxisGroupWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverCrossAxisGroupWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverConstrainedCrossAxisWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverConstrainedCrossAxisWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverIgnorePointerWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverIgnorePointerWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.supports(widget.type())) {
            return slot.name().value().equals("replacementSliver")
                    ? "Optional replacement Sliver, shown only when Visible=false and Maintain state=false. Empty or unset uses SliverToBoxAdapter(). Hidden/inactive branches remain editable in the tree. The maintain constructor never displays replacement."
                    : dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.supports(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.ACTION.equals(widget.type())
                    ? dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.ACTION_DESCRIPTION
                    : dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.TYPE.equals(widget.type()))
            return dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.description(widget.type());
        }
        if (dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.description(widget.type());
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.supports(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.description(widget.type());
        }
        if (dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.supports(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.description(widget.type());
        }
        if (dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.supports(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.description(widget.type());
        }
        if (dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.ThemeWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedThemeWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedSwitcherWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedFractionallySizedBoxWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.RotationTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.RotationTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.DecoratedBoxTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.DecoratedBoxTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AlignTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AlignTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.isFilter(widget.type())) return dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.GROUP.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.BackdropFilterWidgetPropertySchema.GROUP_DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.ImageFilteredWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.ColorFilteredWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.RawImageWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.RawImageWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.FadeInImageWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedIconWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedModalBarrierWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.AnimatedModalBarrierWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.ModalBarrierWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.ModalBarrierWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.DatePickerDialogWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.CalendarDatePickerWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.InputDatePickerFormFieldWidgetPropertySchema.DESCRIPTION;
        if ((dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.supports(widget.type()) || dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.supports(widget.type()))) return (dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.supports(widget.type()) ? (dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.OPTION_TYPE.equals(widget.type()) ? dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.OPTION_DESCRIPTION : dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.DESCRIPTION) : dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.DESCRIPTION);
        if (dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.supports(widget.type())) return dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.TimePickerDialogWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.DateRangePickerDialogWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.PaginatedDataTableWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.PaginatedDataTableWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.supports(widget.type())) return dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.supports(widget.type())) return dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.LayoutIdWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.LayoutIdWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.FlowWidgetPropertySchema.isFlow(widget.type())) return dev.flutter.netbeans.designer.catalog.FlowWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.CustomPaintWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.ShaderMaskWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.ShaderMaskWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.SizeTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.ScaleTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.ScaleTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.SlideTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return dev.flutter.netbeans.designer.catalog.SlideTransitionWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.supports(widget.type())) return dev.flutter.netbeans.designer.catalog.FadeTransitionWidgetPropertySchema.DESCRIPTION;
        if (DefaultTextStyleTransitionWidgetPropertySchema.TYPE.equals(widget.type())) return DefaultTextStyleTransitionWidgetPropertySchema.DESCRIPTION;
        if (DefaultTextStyleWidgetPropertySchema.supports(widget.type())) return DefaultTextStyleWidgetPropertySchema.description(widget.type());
        if (AnimatedDefaultTextStyleWidgetPropertySchema.TYPE.equals(widget.type())) return AnimatedDefaultTextStyleWidgetPropertySchema.DESCRIPTION;
        if (dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(widget.type()))
            return dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.description(widget.type());
        if (dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverSafeAreaWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverSafeAreaWidgetPropertySchema.DESCRIPTION;
        }
        if (dev.flutter.netbeans.designer.catalog.SliverOffstageWidgetPropertySchema.TYPE.equals(widget.type())) {
            return dev.flutter.netbeans.designer.catalog.SliverOffstageWidgetPropertySchema.DESCRIPTION;
        }
        String maximum = Integer.toString(slot.maxChildren());
        String cardinality = slot.cardinality() == SlotCardinality.SINGLE
                ? "single-widget" : "ordered widget-list";
        if (FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE.equals(widget.type())) {
            return slot.name().value().equals("icon")
                    ? "Optional Icon for the Extended constructor only. Move or clear an occupied Icon before selecting Standard, Small or Large; it is never silently removed."
                    : "Optional Child for Standard, Small and Large. Extended requires the same stable child as Label; add Child before selecting Extended. "
                            + "A required Label may be replaced atomically, but cannot be removed, cleared or moved out.";
        }
        if (FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(widget.type())) {
            return slot.name().value().equals("icon")
                    ? "Optional icon for Icon or Tonal icon. Switching between those constructors retains Icon and icon alignment. "
                            + "Move or clear a populated Icon before selecting Standard or Tonal; no widget is silently removed."
                    : "Child is optional for Standard and Tonal (an empty Child emits child: null), but required as Label for Icon and Tonal icon. "
                            + "Replace a required Label atomically; it cannot be removed, cleared or moved out. Constructor changes retain its stable identity.";
        }
        if (MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(widget.type())) {
            return "Optional " + displayName(slot.name().value()) + " for MenuItemButton. Child, Leading icon and Trailing icon can independently be empty, added, moved, replaced or cleared. "
                    + "The SDK's Child parameter is nullable despite its constructor comment. Style, shortcuts, callbacks and the other two slot identities are preserved. Occupancy: " + count + "/" + maximum + "; minimum: 0.";
        }
        if (SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.equals(widget.type())) {
            return "SubmenuButton " + displayName(slot.name().value()) + ". Child is a required nullable argument: empty emits child: null. "
                    + "Menu children is a required list that may be empty; an empty list disables the native opener. Leading icon and Trailing icon are optional. "
                    + "The native button opens its submenu itself; no On pressed or builder template is needed. Slot edits preserve the other slots and widget IDs.";
        }
        if (NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE.equals(widget.type())) {
            return switch (slot.name().value()) {
                case "destinations" -> "Required ordered destination widget list. Flutter accepts an empty list while the rail is assembled; selectedIndex remains nullable and destination order is preserved through DnD, Save/reopen and Undo/Redo.";
                case "leading", "trailing" -> "Optional " + displayName(slot.name().value()) + " widget for NavigationRail. It can be added, replaced, moved or cleared independently of destinations and the other edge slot.";
                default -> "NavigationRail widget slot. Add, move, replace or clear the child atomically.";
            };
        }
        if (NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE.equals(widget.type())) {
            return switch (slot.name().value()) {
                case "children" -> "Required ordered NavigationDrawer destination widget list. The model permits an empty list while the drawer is assembled; each child is emitted as a deterministic NavigationDrawerDestination in generated Dart and Canvas. Selection and order are preserved through DnD, Save/reopen and Undo/Redo.";
                case "header", "footer" -> "Optional " + displayName(slot.name().value()) + " widget for NavigationDrawer. It can be added, replaced, moved or cleared independently of the destination list and the other edge slot.";
                default -> "NavigationDrawer widget slot. Add, move, replace or clear the child atomically.";
            };
        }
        if (DrawerWidgetPropertySchema.DRAWER_TYPE.equals(widget.type())) {
            return "Optional Drawer child widget. It can be added, replaced, moved or cleared independently of the drawer properties. The child identity is preserved through DnD, Save/reopen and Undo/Redo.";
        }
        if (BottomAppBarWidgetPropertySchema.BOTTOM_APP_BAR_TYPE.equals(widget.type())) {
            return "Optional BottomAppBar child widget. It can be added, replaced, moved or cleared independently of the bar properties. The child identity is preserved through DnD, Save/reopen and Undo/Redo.";
        }
        if (BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE.equals(widget.type())) {
            return "Required ordered BottomNavigationBar item widget list. The model permits an empty list while the bar is assembled; generated Dart emits deterministic BottomNavigationBarItem entries and Canvas requires at least two items. Order and child identities are preserved through DnD, Save/reopen and Undo/Redo.";
        }
        if (MaterialWidgetPropertySchema.MATERIAL_TYPE.equals(widget.type())) {
            return "Optional Material child widget. Shape and border-radius references remain application-owned; edit child membership in the Slots tab. Save/reopen and Undo/Redo preserve the exact value and child identity.";
        }
        if (isModernButton(widget)) {
            return slot.name().value().equals("icon")
                    ? "Optional icon for " + modernButtonName(widget) + ".icon only. Select the Icon constructor before adding or moving an icon here. "
                            + "Move or clear a populated Icon before selecting Standard"
                            + (TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE.equals(widget.type()) ? " or setting Semantic button" : "")
                            + ". No child is silently removed."
                    : "Required child, emitted as Child for Standard or Label for Icon. Its stable identity and content survive all constructor changes. "
                            + "Replace it atomically; it cannot be removed or cleared. Button foreground style is inherited by text and icon descendants.";
        }
        if (RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.equals(widget.type()) && CHILD_SLOT.equals(slot.name())) {
            return "Required child retained by all three refresh constructors. Usually contains a vertical ScrollView; "
                    + "short contents need AlwaysScrollableScrollPhysics on that descendant. Any widget is accepted, "
                    + "but this wrapper does not invent scrolling or execute project callbacks in isolated Canvas. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional avatar content, commonly initials or an icon. Child inherits the avatar foreground "
                    + "text/icon color and titleMedium with text scaling disabled. Foreground image paints over Child; "
                    + "the circular image decoration does not clip arbitrary child content. Designer selection and "
                    + "editing remain available. Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Add, move, replace or clear this child atomically.";
        }
        if (BadgeWidgetPropertySchema.BADGE_TYPE.equals(widget.type())) {
            return slot.name().value().equals("label")
                    ? "Optional label for Badge(). An empty Label shows a small dot. Badge.count owns its generated "
                            + "numeric label: clear Count before adding, moving or replacing Label. To set Count, "
                            + "first clear or move the existing Label; Designer never removes it automatically."
                    : "Optional widget below the Badge overlay. With no Child, the badge is standalone; "
                            + "alignment and offset cannot position it relative to a child. Child is retained "
                            + "when switching Count and remains visible when Label visible is false.";
        }
        if (CardWidgetPropertySchema.CARD_TYPE.equals(widget.type()) && CHILD_SLOT.equals(slot.name())) {
            return "Optional child inside the Card's Material surface. Shape alone does not clip the child; "
                    + "Clip behavior controls clipping, Border on foreground controls border painting, and "
                    + "Semantic container groups or exposes child semantics. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child widget.";
        }
        if (IconThemeWidgetPropertySchema.ICON_THEME_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child receiving inherited IconThemeData. Merge true inherits unset fields from the outer IconTheme; "
                    + "false installs direct data, with IconTheme.of supplying SDK fallbacks. Local Icon fields override theme fields "
                    + "where supported; inherited theme opacity still multiplies explicit local Icon color alpha. "
                    + "Designer selection and editing remain available. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child receiving inherited cursor color, selection color and mouse cursor defaults. "
                    + "Merge true inherits unset fields from the ancestor; false uses a direct style and clears those fields. "
                    + "Local selection settings can override these defaults. This does not make a child selectable. "
                    + "Designer selection and editing remain available. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (DefaultTextHeightBehaviorWidgetPropertySchema.DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child receiving the nearest inherited TextHeightBehavior default. "
                    + "Local or nearer text-height behavior can override it. Flags affect text with a height multiplier; "
                    + "this wrapper does not invent a font size or height. Designer selection and editing remain available. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child receiving the nearest complete TooltipThemeData. Local Tooltip properties override theme fields. "
                    + "This theme does not merge with an outer TooltipTheme; all local leaves unset emits const TooltipThemeData(). "
                    + "Designer IDs and stored child properties remain unchanged. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child receiving the nearest TooltipVisibility scope. False suppresses descendant Tooltip display, "
                    + "not the child or its interaction. Tooltip annotation and overlay behavior follow the installed Flutter SDK; child accessibility remains available. A nearer true scope overrides an outer false scope. "
                    + "Designer selection and editing remain available. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (TickerModeWidgetPropertySchema.TICKER_MODE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child whose widget-aware tickers inherit Enabled and Force frames. "
                    + "Muted tickers do not invoke callbacks, but time continues to elapse; this does not pause the timeline. "
                    + "Layout, painting, pointer hits, focus and Designer selection/editing remain available. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (VisibilityWidgetPropertySchema.VISIBILITY_TYPE.equals(widget.type())) {
            if (CHILD_SLOT.equals(slot.name())) {
                return "Required child controlled by Visible and the six Maintain flags. "
                        + "Changing Maintain flags can discard descendant state; Designer selection and editing remain available. "
                        + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                        + ". Replace the child atomically; it cannot be removed or cleared.";
            }
            if ("replacement".equals(slot.name().value())) {
                return "Optional replacement shown when Visible and Maintain state are false. "
                        + "Ignored while Maintain state is true, but retained for later editing. "
                        + "Clearing omits the argument and preserves Flutter's SizedBox.shrink default. "
                        + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                        + ". Add, move, replace or clear this widget atomically.";
            }
        }
        if (ASPECT_RATIO_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out to fill the box resolved from Aspect ratio. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ExcludeFocusWidgetPropertySchema.EXCLUDE_FOCUS_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child whose descendants are excluded from focus while Excluding is true. "
                    + "Changing to true unfocuses descendants; false permits focus but does not automatically "
                    + "restore it or rewrite their local canRequestFocus configuration. Layout, paint and pointer hits remain "
                    + "available. Designer child and descendant editing remain available. Occupancy: "
                    + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (ExcludeFocusTraversalWidgetPropertySchema.EXCLUDE_FOCUS_TRAVERSAL_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child whose descendants are skipped by keyboard focus traversal while Excluding is true. "
                    + "Direct requestFocus remains allowed and existing focus is retained. Local skipTraversal "
                    + "configuration is not rewritten, while the effective getter reflects ancestor exclusion. "
                    + "Layout, paint, pointer hits and Designer child and descendant editing remain available. Occupancy: "
                    + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Replace the child atomically; it cannot be removed or cleared.";
        }
        if (SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child inset from the selected left, top, right, and bottom "
                    + "system intrusions. Minimum supplies a physical EdgeInsets lower bound, "
                    + "and Maintain bottom view padding can preserve MediaQuery viewPadding "
                    + "while the on-screen keyboard is visible. Occupancy: " + count + "/"
                    + maximum + "; minimum: " + slot.minChildren()
                    + ". This required child cannot be added empty, removed, or cleared; "
                    + "open the custom editor to replace it with a new or existing widget.";
        }
        if (DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child that inherits this wrapper's explicit left-to-right or "
                    + "right-to-left Text direction. Descendant directional alignment, "
                    + "padding, and text flow resolve from this value. Occupancy: " + count
                    + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". This required child cannot be added empty, removed, or cleared; "
                    + "open the custom editor to replace it with a new or existing widget.";
        }
        if (BASELINE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child positioned so its selected alphabetic or ideographic "
                    + "baseline sits at the required logical-pixel offset from the top. "
                    + "When the child does not report that baseline, Flutter uses its "
                    + "bottom edge; a smaller offset can shift the child above this box. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (INTRINSIC_HEIGHT_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child sized to its intrinsic height before final layout. "
                    + "Flutter performs a speculative layout pass, which can be O(N²) "
                    + "in tree depth; prefer ordinary constraints when they can express "
                    + "the layout. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (INTRINSIC_WIDTH_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child sized to its maximum intrinsic width before final "
                    + "layout, with optional width and height step snapping still bounded "
                    + "by the parent constraints. Flutter performs a speculative layout "
                    + "pass, which can be O(N²) in tree depth; prefer ordinary constraints "
                    + "when they can express the layout. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (OFFSTAGE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child that remains laid out and active while offstage, but "
                    + "is not painted, cannot be hit tested, and occupies no parent layout "
                    + "space. It can still receive focus and run animations; remove it from "
                    + "the tree instead when hiding it long-term. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (SIZED_OVERFLOW_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out under the original incoming parent constraints, "
                    + "rather than tight constraints from the requested Size. Alignment "
                    + "positions a differently sized child, which may paint outside this box. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (TRANSFORM_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out at its ordinary size before the required "
                    + "Matrix4 is applied during painting. The transform does not change "
                    + "parent layout or this widget's allocated size; Transform hit tests "
                    + "controls whether hit testing follows the painted child. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ROTATED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child rotated clockwise by the required number of "
                    + "quarter turns before layout. Odd turns swap the child's width "
                    + "and height; negative turns rotate counter-clockwise. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (PreferredSizeWidgetPropertySchema.PREFERRED_SIZE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child advertised with the selected finite preferred Size to "
                    + "PreferredSizeWidget parents such as AppBar and Scaffold. The size "
                    + "does not constrain the child's own layout. Occupancy: " + count + "/"
                    + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, replace, or remove the child widget.";
        }
        if (OPACITY_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child painted with the selected group opacity. "
                    + "The Designer keeps its selection outline and hit target visible "
                    + "even when Opacity is zero. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ALIGN_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child positioned within Align using the selected physical "
                    + "alignment or directional alignment resolved from TextDirection "
                    + "(LTR/RTL), not from the theme. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (FRACTIONALLY_SIZED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child given tight fractions of the bounded incoming width "
                    + "or height, then positioned using physical alignment or directional "
                    + "alignment resolved from TextDirection (LTR/RTL), not from the theme. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (FITTED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out unconstrained, then scaled and positioned by "
                    + "FittedBox without changing the child's own layout size. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (CONSTRAINED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out under the additional normalized BoxConstraints. "
                    + "Expanding width or height requires a bounded incoming maximum on that "
                    + "axis. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (UNCONSTRAINED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out without incoming constraints on both axes, or "
                    + "with exactly the selected constrained axis retained. The child is "
                    + "positioned by physical or directional alignment, and paint overflow "
                    + "follows clip behavior. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (LIMITED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child whose maximum width and height are limited only when "
                    + "the corresponding incoming axis is unbounded; bounded incoming "
                    + "constraints pass through unchanged. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (OVERFLOW_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out with the selected minimum and maximum constraint "
                    + "overrides, then positioned by physical or directional alignment. "
                    + "The child may paint outside this box; fit controls whether the box uses "
                    + "its largest allowed size or follows the child within the parent "
                    + "constraints. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (STACK_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered Stack children painted from first (back) to last (front). "
                    + "The current Designer slice creates ordinary, non-Positioned children; "
                    + "they use Stack alignment and fit. Directional alignment resolves from "
                    + "the explicit or ambient TextDirection "
                    + "(LTR/RTL), not from the theme. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (WRAP_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children flowed into one or more runs along the selected axis. "
                    + "The order in this slot is the exact source, paint, and semantic order; "
                    + "direction settings only change visual placement. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (ListViewWidgetPropertySchema.LIST_VIEW_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children laid out linearly along the selected scroll axis. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children that all remain laid out and keep their state while only "
                    + "the selected Index is painted, hit-tested, and exposed through Flutter "
                    + "semantics. Omitted Index selects 0, explicit null selects none, and an "
                    + "explicit integer must address an existing child (0 is also valid while "
                    + "empty). Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children assigned to grid tiles in exact source, paint, and "
                    + "semantic order. Cross-axis count fixes the number of columns for "
                    + "vertical scrolling or rows for horizontal scrolling. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children assigned to grid tiles in exact source, paint, and "
                    + "semantic order. Maximum cross-axis extent determines how many tiles "
                    + "fit in the selected axis. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (SingleChildScrollViewWidgetPropertySchema
                .SINGLE_CHILD_SCROLL_VIEW_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child inside the scrolling viewport. The child may exceed "
                    + "the viewport only along the selected scroll axis; padding surrounds "
                    + "the child inside the scrollable extent. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child painted over the required solid background color. "
                    + "An empty or zero-size ColoredBox retains only a non-persisted Designer "
                    + "selection and drop target; generated Flutter layout remains unchanged. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child painted beneath Placeholder's outline and diagonals. "
                    + "Fallback width and height apply only when the corresponding incoming "
                    + "axis is unbounded. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child composited with the required BoxDecoration. "
                    + "Position paints that decoration behind or in front of the child. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ClipRectWidgetPropertySchema.CLIP_RECT_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child clipped to this widget's rectangular bounds. "
                    + "Clip behavior controls edge quality. The Dart analyzer validates "
                    + "a configured Dart symbol as CustomClipper<Rect>. The isolated "
                    + "Canvas cannot execute project Dart and displays an explicit "
                    + "preview-unavailable state. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child clipped to the oval inscribed in this widget's bounds. "
                    + "Clip behavior controls edge quality. The Dart analyzer validates "
                    + "a configured Dart symbol as CustomClipper<Rect>. The isolated "
                    + "Canvas cannot execute project Dart and displays an explicit "
                    + "preview-unavailable state. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child clipped to this widget's rounded rectangular bounds. "
                    + "Physical or direction-aware corner radii and clip behavior control "
                    + "the exact edge. The Dart analyzer validates a configured Dart "
                    + "symbol as CustomClipper<RRect>; Flutter then ignores borderRadius. "
                    + "The isolated Canvas cannot execute project Dart and displays an "
                    + "explicit preview-unavailable state. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child painted on an elevated physical surface. Shape, color, "
                    + "shadow color, elevation and clip behavior control the surface. "
                    + "Circle shape ignores but preserves physical borderRadius. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (PhysicalShapeWidgetPropertySchema.PHYSICAL_SHAPE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child painted on an elevated path-shaped surface. Built-in ShapeBorderClipper "
                    + "geometry renders in Canvas; project CustomClipper<Path> code is not executed. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child widget.";
        }
        if (ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child clipped to this widget's rounded superellipse bounds. "
                    + "Physical or direction-aware corner radii and clip behavior control "
                    + "the exact edge. The Dart analyzer validates a configured Dart "
                    + "symbol as CustomClipper<RSuperellipse>; Flutter then ignores borderRadius. "
                    + "The isolated Canvas cannot execute project Dart and displays an "
                    + "explicit preview-unavailable state. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child clipped either by a project CustomClipper<Path> "
                    + "or by a ShapeBorder through the non-const ClipPath.shape helper. "
                    + "Clipper and Shape are mutually exclusive; setting either branch "
                    + "atomically clears the other. Clip behavior controls edge quality. "
                    + "The isolated Canvas cannot execute project Dart and displays an "
                    + "explicit preview-unavailable state. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE.equals(
                widget.type()) && CHILD_SLOT.equals(slot.name())) {
            return "Optional child whose descendant semantics can be removed from the "
                    + "accessibility tree without changing layout, painting, or hit testing. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (LIST_BODY_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children laid out sequentially along Main axis and stretched "
                    + "across the bounded cross axis. ListBody requires unbounded space "
                    + "along its main axis; Reverse changes visual placement without "
                    + "changing the stored source order. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (PageViewWidgetPropertySchema.PAGE_VIEW_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered pages displayed by the PageView viewport. Each page fills the "
                    + "viewport and may be selected by swiping or accessibility navigation. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a page.";
        }
        if (OVERFLOW_BAR_TYPE.equals(widget.type())
                && CHILDREN_SLOT.equals(slot.name())) {
            return "Ordered children laid out in one horizontal row while their total width "
                    + "including Spacing fits. When it does not fit, Flutter lays out the same "
                    + "children in a vertical overflow column. Text direction controls horizontal "
                    + "placement; Overflow direction controls vertical placement without changing "
                    + "the stored source order. Occupancy: "
                    + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, reorder, or remove a widget.";
        }
        if (EXPANDED_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child expanded with FlexFit.tight along the direct Row or "
                    + "Column main axis. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to replace the child atomically; "
                    + "it cannot be removed or cleared.";
        }
        if (FLEXIBLE_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child allocated a share of the direct Row or Column "
                    + "main axis using flex. Loose fit allows the child to remain smaller "
                    + "than its allocation; tight fit requires it to fill that allocation. "
                    + "Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to replace the child atomically; "
                    + "it cannot be removed or cleared.";
        }
        if (ContainerWidgetPropertySchema.CONTAINER_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out inside Container padding, alignment, and "
                    + "constraints. Occupancy: " + count + "/" + maximum
                    + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove "
                    + "the child widget.";
        }
        if (IndexedSemanticsWidgetPropertySchema.INDEXED_SEMANTICS_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child annotated with the required semantic index. The index describes its "
                    + "position for accessibility; it does not sort children, create a label, or change "
                    + "layout, painting or pointer hit testing. Designer child and descendant editing remain "
                    + "available. Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child.";
        }
        if (BlockSemanticsWidgetPropertySchema.BLOCK_SEMANTICS_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child keeps its own semantics and normal layout, painting, and hit testing. "
                    + "Blocking hides earlier-painted semantic nodes in the same semantics container; "
                    + "own child and later nodes remain. Unlike ExcludeSemantics it does not exclude this child, "
                    + "and unlike AbsorbPointer it does not block pointer hits. Designer editing remains available. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child.";
        }
        if (AbsorbPointerWidgetPropertySchema.ABSORB_POINTER_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out and painted normally. Absorbing stops pointer hits at this widget, "
                    + "blocking the child and targets behind it; unlike IgnorePointer, events do not pass through. "
                    + "Designer selection and editing remain available. "
                    + "Deprecated Ignoring semantics controls accessible actions or subtree exclusion. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child.";
        }
        if (IgnorePointerWidgetPropertySchema.IGNORE_POINTER_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out and painted normally. Ignoring pointer hits lets targets behind "
                    + "the subtree receive events; Designer selection and editing remain available. "
                    + "Deprecated Ignoring semantics controls accessible actions or subtree exclusion. "
                    + "Occupancy: " + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child.";
        }
        if ("flutter.widgets.MergeSemantics".equals(widget.type().value())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child whose semantics subtree is merged into one node. Labels are joined with "
                    + "newlines; the first handler in tree order wins for a shared action. Conflicting states "
                    + "follow Flutter semantics rules. Layout and pointer hit testing remain unchanged; "
                    + "isolated preview does not execute project callbacks. Occupancy: "
                    + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child; "
                    + "select a descendant in the widget tree to edit its own properties.";
        }
        if ("flutter.widgets.RepaintBoundary".equals(widget.type().value())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child painted in a separate display list. Layout, hit testing and semantics "
                    + "are unchanged; no scalar properties are omitted from the editor. Occupancy: "
                    + count + "/" + maximum + "; minimum: " + slot.minChildren()
                    + ". Open the custom editor to add, move, replace, or remove the child; "
                    + "select a descendant to edit its own properties.";
        }
        return "Exact '" + slot.name().value() + "' " + cardinality
                + " slot. Occupancy: " + count + "/" + maximum
                + "; minimum: " + slot.minChildren()
                + ". Open the custom editor to add, move, reorder, or remove a widget.";
    }

    private Sheet.Set createGenericPropertySet() {
        boolean mergeSemantics = "flutter.widgets.MergeSemantics".equals(widget.type().value());
        boolean repaintBoundary = "flutter.widgets.RepaintBoundary".equals(widget.type().value());
        boolean aspectRatio = ASPECT_RATIO_TYPE.equals(widget.type());
        boolean baseline = BASELINE_TYPE.equals(widget.type());
        boolean intrinsicHeight = INTRINSIC_HEIGHT_TYPE.equals(widget.type());
        boolean intrinsicWidth = INTRINSIC_WIDTH_TYPE.equals(widget.type());
        boolean offstage = OFFSTAGE_TYPE.equals(widget.type());
        boolean sizedOverflowBox = SIZED_OVERFLOW_BOX_TYPE.equals(widget.type());
        boolean transform = TRANSFORM_TYPE.equals(widget.type());
        boolean rotatedBox = ROTATED_BOX_TYPE.equals(widget.type());
        boolean listBody = LIST_BODY_TYPE.equals(widget.type());
        boolean overflowBar = OVERFLOW_BAR_TYPE.equals(widget.type());
        boolean opacity = OPACITY_TYPE.equals(widget.type());
        boolean align = ALIGN_TYPE.equals(widget.type());
        boolean fractionallySizedBox = FRACTIONALLY_SIZED_BOX_TYPE.equals(widget.type());
        boolean fittedBox = FITTED_BOX_TYPE.equals(widget.type());
        boolean constrainedBox = CONSTRAINED_BOX_TYPE.equals(widget.type());
        boolean unconstrainedBox = UNCONSTRAINED_BOX_TYPE.equals(widget.type());
        boolean limitedBox = LIMITED_BOX_TYPE.equals(widget.type());
        boolean overflowBox = OVERFLOW_BOX_TYPE.equals(widget.type());
        boolean stack = STACK_TYPE.equals(widget.type());
        boolean wrap = WRAP_TYPE.equals(widget.type());
        boolean expanded = EXPANDED_TYPE.equals(widget.type());
        boolean flexible = FLEXIBLE_TYPE.equals(widget.type());
        boolean spacer = SPACER_TYPE.equals(widget.type());
        boolean image = IMAGE_TYPE.equals(widget.type());
        Sheet.Set properties = propertySet(
                PROPERTIES_SET_NAME,
                "Widget properties",
                mergeSemantics
                        ? "MergeSemantics combines its optional child's semantics subtree into one node. "
                                + "There are no scalar constructor properties; edit Child in Slots or select "
                                + "a descendant in the widget tree to edit its properties. The Designer owns widget identity. "
                                + "Labels are joined with newlines; the first handler in tree order wins for "
                                + "a shared action, and conflicting states follow Flutter semantics rules. "
                                + "Layout and pointer hit testing remain unchanged; isolated preview does "
                                + "not execute project callbacks."
                        : repaintBoundary
                        ? "RepaintBoundary isolates its optional child's painting in a separate display list. "
                                + "There are no scalar constructor properties; edit Child in Slots or select "
                                + "a descendant to edit its properties. The Designer owns widget identity. "
                                + "Use repaint boundaries where repaint behavior differs; an extra layer "
                                + "does not guarantee faster rendering."
                        : aspectRatio
                        ? "Sizing contract for the selected AspectRatio widget."
                        : baseline
                                ? "Required baseline offset and type, plus an optional child, "
                                        + "for the selected Baseline widget."
                        : intrinsicHeight
                                ? "Intrinsic-height sizing and optional child for the selected "
                                        + "IntrinsicHeight widget. Flutter performs a speculative "
                                        + "layout pass that can be O(N²) in tree depth."
                        : intrinsicWidth
                                ? "Maximum-intrinsic-width sizing, optional width and height "
                                        + "step snapping, parent constraints, and optional child "
                                        + "for the selected IntrinsicWidth widget. Flutter performs "
                                        + "a speculative layout pass that can be O(N²) in tree depth."
                        : offstage
                                ? "Visibility, layout participation, focus, animation, and optional "
                                        + "child contract for the selected Offstage widget."
                        : sizedOverflowBox
                                ? "Required requested size, child alignment, parent-constraint "
                                        + "behavior, and optional overflowing child contract for "
                                        + "the selected SizedOverflowBox widget."
                        : transform
                                ? "Paint-time Matrix4, pivot origin, alignment, hit-testing, "
                                        + "filter quality, and optional child contract for the "
                                        + "selected Transform widget; layout size is unchanged."
                        : rotatedBox
                                ? "Required signed quarter-turn rotation applied before layout "
                                        + "and optional child contract for the selected "
                                        + "RotatedBox widget."
                        : listBody
                                ? "Sequential main-axis layout, reading-direction reversal, "
                                        + "and exact ordered children for the selected ListBody "
                                        + "widget; the main axis must be unbounded and the cross "
                                        + "axis bounded."
                        : overflowBar
                                ? "Responsive horizontal-row or vertical-overflow layout, "
                                        + "spacing, alignment, direction, and exact ordered "
                                        + "children for the selected OverflowBar widget."
                        : opacity
                                ? "Transparency and semantics contract for the selected "
                                        + "Opacity widget."
                        : align
                                ? "Positioning and optional shrink-wrap factors for the selected "
                                        + "Align widget; directional alignment resolves from "
                                        + "TextDirection (LTR/RTL), not from the theme."
                        : fractionallySizedBox
                                ? "Incoming-size fractions, positioning, and optional child for "
                                        + "the selected FractionallySizedBox widget; directional "
                                        + "alignment resolves from TextDirection (LTR/RTL), not "
                                        + "from the theme."
                        : fittedBox
                                ? "Scaling discipline, positioning, clipping, and optional child "
                                        + "for the selected FittedBox widget."
                         : constrainedBox
                                 ? "Required normalized width and height constraints, including "
                                         + "independently expanding axes, and an optional child "
                                         + "for the selected ConstrainedBox widget."
                        : unconstrainedBox
                                ? "Constraint removal, optional retained axis, positioning, "
                                        + "clipping, direction, and optional child for the "
                                        + "selected UnconstrainedBox widget."
                        : limitedBox
                                ? "Fallback maximum width and height for unbounded incoming axes, "
                                        + "and an optional child for the selected LimitedBox widget."
                        : overflowBox
                                ? "Constraint overrides, positioning, fit, and optional child for "
                                        + "the selected OverflowBox widget; directional alignment "
                                        + "resolves from TextDirection (LTR/RTL), not from the theme."
                        : wrap
                                ? "Run direction, spacing, alignment, clipping, and ordered "
                                        + "children for the selected Wrap widget."
                        : stack
                                ? "Layer alignment, direction, sizing, clipping, and ordered "
                                        + "non-Positioned children for the selected Stack widget."
                        : expanded
                                ? "Remaining-space allocation and required child contract for "
                                        + "the selected direct Row or Column Expanded widget."
                        : flexible
                                ? "Loose or tight remaining-space allocation and required child "
                                        + "contract for the selected direct Row or Column "
                                        + "Flexible widget."
                        : spacer
                                ? "Empty positive-flex remaining-space allocation for the "
                                        + "selected direct Row or Column Spacer widget."
                        : image
                                ? "Declared asset provider, layout, paint, nine-patch, callback, "
                                        + "and semantics settings for the selected Image widget."
                        : "Explicit property values stored on the selected widget; "
                                + "catalog creation defaults are not applied.");
        for (PropertyDefinition property : definition.properties()) {
            if (image) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        displayName(property.name()),
                        imagePropertyDescription(property.name())));
            } else if (aspectRatio && ASPECT_RATIO_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Aspect ratio",
                        "Finite width-to-height ratio used to size this widget; "
                                + "for example, use 1.7778 for a 16:9 surface."));
            } else if (baseline && BASELINE_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Baseline",
                        "Required finite logical-pixel distance from the top of this box "
                                + "at which Flutter positions the selected child baseline. "
                                + "Designer palette creation starts at 24. "
                                + "Negative values shift the child upward and can make it "
                                + "overflow above the box."));
            } else if (baseline && BASELINE_TYPE_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Baseline type",
                        "Required child baseline to query: alphabetic for alphabetic scripts "
                                + "or ideographic for ideographic scripts. When the child does "
                                + "not report the requested baseline, Flutter uses its bottom "
                                + "edge."));
            } else if (intrinsicWidth) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        intrinsicWidthPropertyDisplayName(property.name()),
                        intrinsicWidthPropertyDescription(property.name())));
            } else if (offstage && OFFSTAGE_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Offstage",
                        "When true, Flutter still lays out the child but does not paint or "
                                + "hit-test it, and this widget takes no parent layout space. "
                                + "The child remains active, can receive focus, and its animations "
                                + "continue to run. Omission preserves Flutter's default true; "
                                + "remove the child from the tree when hiding it long-term."));
            } else if (sizedOverflowBox) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        sizedOverflowBoxPropertyDisplayName(property.name()),
                        sizedOverflowBoxPropertyDescription(property.name())));
            } else if (transform) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        transformPropertyDisplayName(property.name()),
                        transformPropertyDescription(property.name())));
            } else if (rotatedBox && "quarterTurns".equals(
                    property.name().value())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Quarter turns",
                        "Required signed integer number of clockwise quarter turns applied "
                                + "before layout. Negative values rotate counter-clockwise, "
                                + "multiples of four preserve orientation, and odd values "
                                + "swap the child's width and height. Designer palette "
                                + "creation starts at 1."));
            } else if (listBody) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        listBodyPropertyDisplayName(property.name()),
                        listBodyPropertyDescription(property.name())));
            } else if (overflowBar) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        overflowBarPropertyDisplayName(property.name()),
                        overflowBarPropertyDescription(property.name())));
            } else if (opacity && OPACITY_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Opacity",
                        "Finite alpha multiplier from 0 (fully transparent) through "
                                + "1 (fully opaque); intermediate rendered alpha values "
                                + "use an offscreen buffer in Flutter."));
            } else if (opacity
                    && ALWAYS_INCLUDE_SEMANTICS_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Always include semantics",
                        "When true, exposes child semantics even when Opacity would "
                                + "otherwise hide them; Flutter defaults to false."));
            } else if (align && ALIGNMENT_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Alignment",
                        "Physical position or directional position resolved from TextDirection "
                                + "(LTR/RTL), not from the theme. Flutter "
                                + "defaults to center; coordinates outside -1 through 1 "
                                + "extrapolate beyond the available box."));
            } else if (align && WIDTH_FACTOR_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Width factor",
                        "Optional finite non-negative multiplier applied to the child's width. "
                                + "Zero is valid; when omitted, Align expands on a bounded "
                                + "horizontal axis and shrink-wraps on an unbounded axis."));
            } else if (align && HEIGHT_FACTOR_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Height factor",
                        "Optional finite non-negative multiplier applied to the child's height. "
                                + "Zero is valid; when omitted, Align expands on a bounded "
                                + "vertical axis and shrink-wraps on an unbounded axis."));
            } else if (fractionallySizedBox
                    && ALIGNMENT_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Alignment",
                        "Physical position or directional position resolved from TextDirection "
                                + "(LTR/RTL), not from the theme. Flutter defaults to center; "
                                + "coordinates outside -1 through 1 extrapolate and can place "
                                + "the child beyond the available box."));
            } else if (fractionallySizedBox
                    && WIDTH_FACTOR_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Width factor",
                        "Optional finite non-negative fraction of the bounded incoming maximum "
                                + "width, imposed as a tight child width. Zero and values above "
                                + "one are valid; omission passes horizontal constraints through. "
                                + "Do not set it when the incoming maximum width is unbounded."));
            } else if (fractionallySizedBox
                    && HEIGHT_FACTOR_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Height factor",
                        "Optional finite non-negative fraction of the bounded incoming maximum "
                                + "height, imposed as a tight child height. Zero and values above "
                                + "one are valid; omission passes vertical constraints through. "
                                + "Do not set it when the incoming maximum height is unbounded."));
            } else if (fittedBox) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        fittedBoxPropertyDisplayName(property.name()),
                        fittedBoxPropertyDescription(property.name())));
            } else if (constrainedBox && CONSTRAINTS_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Constraints",
                        "Required normalized BoxConstraints imposed on the optional child. "
                                + "Each axis supports a finite non-negative minimum, a finite "
                                + "maximum not below its minimum, an unbounded maximum (∞), "
                                + "or an expanding ∞…∞ range. Expanding width or height "
                                 + "requires a bounded incoming maximum on that axis."));
            } else if (unconstrainedBox) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        unconstrainedBoxPropertyDisplayName(property.name()),
                        unconstrainedBoxPropertyDescription(property.name())));
            } else if (limitedBox) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        displayName(property.name()),
                        limitedBoxPropertyDescription(property.name())));
            } else if (overflowBox) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        overflowBoxPropertyDisplayName(property.name()),
                        overflowBoxPropertyDescription(property.name())));
            } else if (wrap) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        displayName(property.name()),
                        wrapPropertyDescription(property.name())));
            } else if (stack && ALIGNMENT_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Alignment",
                        "Physical position or directional position for the current Designer's "
                                + "ordinary, non-Positioned children. Flutter defaults to "
                                + "directional top-start; coordinates outside -1 through 1 "
                                + "extrapolate. Directional values use the explicit Stack text "
                                + "direction when set, otherwise ambient LTR/RTL Directionality."));
            } else if (stack && TEXT_DIRECTION_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Text direction",
                        "Optional LTR or RTL override used only to resolve directional Stack "
                                + "alignment. Omission preserves the ambient Directionality; "
                                + "this value is not read from the theme."));
            } else if (stack && STACK_FIT_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Fit",
                        "How Stack constrains non-positioned children: loose relaxes incoming "
                                + "minimums, expand tightens to the biggest allowed size, and "
                                + "passthrough preserves incoming constraints. Flutter defaults "
                                + "to loose."));
            } else if (stack
                    && STACK_CLIP_BEHAVIOR_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Clip behavior",
                        "The selected value is passed to Flutter unchanged; Flutter defaults "
                                + "to hard edge. RenderStack clips only when a direct child's "
                                + "geometry sets its visual-overflow flag. Extrapolated alignment "
                                + "of a non-Positioned child does not set that flag, so it is not "
                                + "clipped; descendant or paint-only overflow is not clipped "
                                + "either."));
            } else if (expanded && FLEX_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Flex",
                        "Non-negative integer share of remaining Row or Column main-axis "
                                + "space. Flutter defaults to 1; zero is valid but makes the "
                                + "child inflexible. Positive flex requires bounded width in "
                                + "Row or bounded height in Column."));
            } else if (flexible && FLEX_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Flex",
                        "Non-negative integer share of remaining Row or Column main-axis "
                                + "space. Flutter defaults to 1; zero is valid and makes the "
                                + "child inflexible, so fit has no effect. Positive flex "
                                + "requires bounded width in Row or bounded height in Column."));
            } else if (flexible
                    && FLEXIBLE_FIT_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Fit",
                        "How a child with positive flex uses its allocated main-axis space. "
                                + "Loose, Flutter's default, lets the child be smaller than "
                                + "the allocation; tight requires it to fill the allocation. "
                                + "The value is ignored when flex is zero."));
            } else if (spacer && FLEX_PROPERTY.equals(property.name())) {
                properties.put(projectProperty(
                        property,
                        Optional.empty(),
                        "Flex",
                        "Positive integer share of the remaining Row or Column main-axis "
                                + "space reserved as an empty gap. Flutter defaults to 1; "
                                + "zero is invalid. Positive flex requires bounded width in "
                                + "Row or bounded height in Column."));
            } else {
                properties.put(projectProperty(property, Optional.empty()));
            }
        }
        return properties;
    }

    private static String listBodyPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "mainAxis" -> "Main axis";
            case "reverse" -> "Reverse";
            default -> displayName(propertyName);
        };
    }

    private static String listBodyPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "mainAxis" ->
                "Axis along which children are placed sequentially. Omission preserves "
                + "Flutter's vertical default. RenderListBody requires unbounded space "
                + "along this axis and a bounded cross axis.";
            case "reverse" ->
                "Whether children are positioned opposite the reading direction. Omission "
                + "preserves Flutter's false default. Horizontal placement combines this "
                + "value with ambient LTR/RTL Directionality; vertical placement changes "
                + "between down and up without changing the stored source order.";
            default -> "Explicit ListBody value for " + propertyName.value() + ".";
        };
    }

    private static String overflowBarPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "overflowSpacing" -> "Overflow spacing";
            case "overflowAlignment" -> "Overflow alignment";
            case "overflowDirection" -> "Overflow direction";
            case "textDirection" -> "Text direction";
            default -> displayName(propertyName);
        };
    }

    private static String overflowBarPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "spacing" ->
                "Finite signed logical-pixel gap added between children while they fit in "
                + "one horizontal row. Omission preserves Flutter's 0.0 default. For the "
                + "spaceAround, spaceBetween, and spaceEvenly alignments, Flutter uses this "
                + "value only when deciding whether the row overflows.";
            case "alignment" ->
                "Optional MainAxisAlignment for the horizontal row when all children fit. "
                + "Omission makes the row only as wide as its children and Spacing; an "
                + "explicit value expands it to the available width. This value is ignored "
                + "after the layout switches to the vertical overflow column.";
            case "overflowSpacing" ->
                "Finite signed logical-pixel gap added vertically between children after "
                + "their horizontal row exceeds the available width. Omission preserves "
                + "Flutter's 0.0 default; this value is unused while the row fits.";
            case "overflowAlignment" ->
                "Horizontal alignment of each child inside the vertical overflow column: "
                + "start, end, or center. Start and end resolve against the explicit or "
                + "ambient Text direction. Omission preserves Flutter's start default; this "
                + "value is unused while the horizontal row fits.";
            case "overflowDirection" ->
                "Visual order of children in the vertical overflow column. Down places the "
                + "first stored child at the top; up places it at the bottom. Omission "
                + "preserves Flutter's down default and never changes stored source order.";
            case "textDirection" ->
                "Optional LTR or RTL override for horizontal child order and for resolving "
                + "start/end Overflow alignment. Omission preserves ambient Directionality; "
                + "this value is not read from the theme.";
            default -> "Explicit OverflowBar value for " + propertyName.value() + ".";
        };
    }

    private static String unconstrainedBoxPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "textDirection" ->
                "Optional LTR or RTL override used to resolve directional alignment. "
                + "Omission preserves ambient Directionality; this value is not read "
                + "from the theme.";
            case "alignment" ->
                "Physical or directional position of an overflowing child inside the "
                + "incoming box. Flutter defaults to center; directional values use the "
                + "explicit text direction when set, otherwise ambient Directionality.";
            case "constrainedAxis" ->
                "Optional axis whose incoming constraints remain in force. Horizontal "
                + "retains width constraints, vertical retains height constraints, and "
                + "omission removes constraints from both axes.";
            case "clipBehavior" ->
                "How paint outside the UnconstrainedBox bounds is clipped. Flutter "
                + "defaults to none; hard edge, anti-alias, and anti-alias with save "
                + "layer are emitted unchanged.";
            default -> "Explicit UnconstrainedBox value for " + propertyName.value() + ".";
        };
    }

    private static String unconstrainedBoxPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "textDirection" -> "Text direction";
            case "constrainedAxis" -> "Constrained axis";
            case "clipBehavior" -> "Clip behavior";
            default -> displayName(propertyName);
        };
    }

    private static String limitedBoxPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "maxWidth" ->
                "Optional finite non-negative fallback maximum width applied only when the "
                + "incoming width is unbounded. Omission preserves Flutter's default positive "
                + "infinity; zero is valid.";
            case "maxHeight" ->
                "Optional finite non-negative fallback maximum height applied only when the "
                + "incoming height is unbounded. Omission preserves Flutter's default positive "
                + "infinity; zero is valid.";
            default -> "Explicit LimitedBox value for " + propertyName.value() + ".";
        };
    }

    private static String intrinsicWidthPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "stepWidth" -> "Step width";
            case "stepHeight" -> "Step height";
            default -> displayName(propertyName);
        };
    }

    private static String intrinsicWidthPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "stepWidth" ->
                "Optional finite non-negative width step. A positive value snaps the "
                + "child width upward to a multiple of this step after measuring its "
                + "maximum intrinsic width, subject to the parent constraints. Omission "
                + "or zero uses the maximum intrinsic width directly; zero is valid.";
            case "stepHeight" ->
                "Optional finite non-negative height step. A positive value snaps the "
                + "child height upward to a multiple of this step, subject to the parent "
                + "constraints. Omission or zero gives the child unconstrained height; "
                + "zero is valid.";
            default -> "Explicit IntrinsicWidth value for " + propertyName.value() + ".";
        };
    }

    private static String sizedOverflowBoxPropertyDisplayName(
            PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "size" -> "Size";
            case "alignment" -> "Alignment";
            default -> displayName(propertyName);
        };
    }

    private static String sizedOverflowBoxPropertyDescription(
            PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "size" ->
                "Required finite non-negative logical-pixel width and height requested for "
                + "this box. Parent constraints still constrain the resulting box, while "
                + "the original incoming constraints pass through unchanged to the child. "
                + "Palette creation starts at 100 × 100.";
            case "alignment" ->
                "Physical position or directional position resolved from ambient "
                + "TextDirection (LTR/RTL), not from the theme, used when the child and box "
                + "sizes differ. Flutter defaults to center; coordinates outside -1 through "
                + "1 can position the child beyond this box.";
            default -> "Explicit SizedOverflowBox value for "
                    + propertyName.value() + ".";
        };
    }

    private static String transformPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "transform" -> "Transform";
            case "origin" -> "Origin";
            case "alignment" -> "Alignment";
            case "transformHitTests" -> "Transform hit tests";
            case "filterQuality" -> "Filter quality";
            default -> displayName(propertyName);
        };
    }

    private static String transformPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "transform" ->
                "Required finite 4×4 column-major Matrix4 applied while painting the "
                + "optional child. Palette creation starts with the identity matrix. The "
                + "matrix changes paint coordinates, not the child's layout size or the "
                + "space allocated by the parent.";
            case "origin" ->
                "Optional finite signed logical-pixel Offset that shifts the transform "
                + "origin relative to the widget's upper-left corner, or relative to the "
                + "resolved alignment pivot when Alignment is also set. Omission applies "
                + "no additional origin offset; negative dx and dy values are valid.";
            case "alignment" ->
                "Optional physical or directional point within this widget around which the "
                + "Matrix4 is applied. Omission passes null and adds no alignment pivot, so "
                + "the raw matrix uses the upper-left coordinate origin plus any explicit "
                + "Origin offset. Center defaults belong only to unsupported Transform "
                + "convenience constructors. Directional values resolve from ambient "
                + "TextDirection (LTR/RTL), not from the theme.";
            case "transformHitTests" ->
                "When true, Flutter transforms hit-test coordinates so pointer interaction "
                + "follows the painted child. False keeps hit testing in the untransformed "
                + "coordinate space; omission preserves Flutter's default true.";
            case "filterQuality" ->
                "Optional sampling quality used while compositing the transformed child: "
                + "none, low, medium, or high. Omission preserves Flutter's null default; "
                + "higher quality can require more rendering work.";
            default -> "Explicit Transform value for " + propertyName.value() + ".";
        };
    }

    private static String overflowBoxPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "alignment" ->
                "Physical or directional position of the child within the OverflowBox. Flutter "
                + "defaults to center; directional values resolve from ambient TextDirection "
                + "(LTR/RTL), not from the theme. Coordinates outside -1 through 1 can place "
                + "the child beyond the box.";
            case "minWidth" ->
                "Optional finite non-negative minimum width override for the child. Omission "
                + "inherits the incoming minimum width. When both width overrides are set, "
                + "minimum width must not exceed maximum width; zero is valid.";
            case "maxWidth" ->
                "Optional finite non-negative maximum width override for the child. Omission "
                + "inherits the incoming maximum width. When both width overrides are set, "
                + "maximum width must not be below minimum width; overflow is allowed.";
            case "minHeight" ->
                "Optional finite non-negative minimum height override for the child. Omission "
                + "inherits the incoming minimum height. When both height overrides are set, "
                + "minimum height must not exceed maximum height; zero is valid.";
            case "maxHeight" ->
                "Optional finite non-negative maximum height override for the child. Omission "
                + "inherits the incoming maximum height. When both height overrides are set, "
                + "maximum height must not be below minimum height; overflow is allowed.";
            case "fit" ->
                "How the OverflowBox chooses its own size. Max, Flutter's default, uses the "
                + "largest size allowed by the incoming constraints; deferToChild matches the "
                + "child within the parent constraints, or uses the parent's smallest size "
                + "when there is no child. This matters only when the child does not overflow.";
            default -> "Explicit OverflowBox value for " + propertyName.value() + ".";
        };
    }

    private static String overflowBoxPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "minWidth" -> "Min Width";
            case "maxWidth" -> "Max Width";
            case "minHeight" -> "Min Height";
            case "maxHeight" -> "Max Height";
            default -> displayName(propertyName);
        };
    }

    private static String wrapPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "direction" ->
                "Main axis used to place children into runs. Horizontal is Flutter's "
                + "default; vertical creates top-to-bottom runs and then adds columns.";
            case "alignment" ->
                "How children are distributed within each run along the main axis. "
                + "Flutter defaults to start; the spacing value remains the minimum gap.";
            case "spacing" ->
                "Finite logical-pixel spacing added between adjacent children inside one "
                + "run. Flutter defaults to 0; negative values intentionally overlap them.";
            case "runAlignment" ->
                "How complete runs are distributed along the cross axis. Flutter defaults "
                + "to start; the run-spacing value remains the minimum gap.";
            case "runSpacing" ->
                "Finite logical-pixel spacing added between adjacent runs. Flutter defaults "
                + "to 0; negative values intentionally overlap the runs.";
            case "crossAxisAlignment" ->
                "How children within each run align to that run's cross axis. Flutter "
                + "supports start, end, and center and defaults to start.";
            case "textDirection" ->
                "Optional LTR or RTL override used for horizontal child order and for "
                + "directional start/end resolution. Omission preserves ambient Directionality.";
            case "verticalDirection" ->
                "Vertical placement order and vertical start/end resolution. Flutter "
                + "defaults to down.";
            case "clipBehavior" ->
                "How Wrap clips child paint that overflows its own bounds. Flutter defaults "
                + "to none; the selected enum value is emitted unchanged.";
            default -> "Explicit model value for " + propertyName.value() + ".";
        };
    }

    private static String fittedBoxPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "fit" ->
                "How Flutter scales the unconstrained child into the FittedBox bounds. "
                + "Contain is the default; fill may distort, cover may crop, fitWidth and "
                + "fitHeight preserve one complete axis, none keeps the source scale, and "
                + "scaleDown only shrinks an oversized child.";
            case "alignment" ->
                "Physical or directional position of the fitted child inside the available "
                + "box. Flutter defaults to center; directional values resolve from the "
                + "ambient TextDirection (LTR/RTL), not from the theme.";
            case "clipBehavior" ->
                "How visual overflow is clipped after fitting. Flutter defaults to none; "
                + "cover, fitWidth, fitHeight, and none can require an explicit clipping mode.";
            default -> "Explicit model value for " + propertyName.value() + ".";
        };
    }

    private static String fittedBoxPropertyDisplayName(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "clipBehavior" -> "Clip behavior";
            default -> displayName(propertyName);
        };
    }

    private static String imagePropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "image" ->
                "Required asset-only ImageProvider. Choose a concrete asset declared by "
                + "the app or a resolved package, then configure DPR-aware AssetImage or "
                + "ExactAssetImage and an optional bounded ResizeImage decode request; "
                + "arbitrary paths and network providers are unavailable.";
            case "frameBuilder" ->
                "Optional validated ImageFrameBuilder function identifier used to wrap "
                + "each available image frame; omission paints the first frame directly.";
            case "loadingBuilder" ->
                "Optional validated ImageLoadingBuilder function identifier invoked while "
                + "incremental image bytes load; it can rebuild on each rendering frame.";
            case "errorBuilder" ->
                "Optional validated ImageErrorWidgetBuilder function identifier invoked "
                + "for image loading failures; omission reports the failure to FlutterError.";
            case "semanticLabel" ->
                "Optional semantic description announced by accessibility services unless "
                + "Exclude from semantics is true.";
            case "excludeFromSemantics" ->
                "Whether to omit this image from the semantics tree. Flutter defaults to "
                + "false; when true, Semantic label is ignored.";
            case "width" ->
                "Optional finite non-negative logical-pixel width. Specify width and height "
                + "together, or use tight parent constraints, to avoid layout changes while loading.";
            case "height" ->
                "Optional finite non-negative logical-pixel height. Specify width and height "
                + "together, or use tight parent constraints, to avoid layout changes while loading.";
            case "color" ->
                "Optional literal or reviewed Material theme color blended with every image "
                + "pixel using Color blend mode.";
            case "opacity" ->
                "Optional finite multiplier from 0 through 1, emitted as a constant "
                + "AlwaysStoppedAnimation<double> for Image.opacity.";
            case "colorBlendMode" ->
                "Optional pinned Flutter BlendMode used to combine Color with the image. "
                + "When omitted, Flutter uses srcIn for a supplied color.";
            case "fit" ->
                "Optional pinned Flutter BoxFit describing how the image is inscribed in its "
                + "layout bounds. cover and none are incompatible with an enabled center slice.";
            case "alignment" ->
                "Physical position or directional position within the image bounds. Flutter "
                + "defaults to center; directional values require ambient LTR/RTL Directionality.";
            case "repeat" ->
                "Pinned Flutter ImageRepeat used for uncovered layout bounds. Flutter defaults "
                + "to noRepeat.";
            case "centerSliceLeft" ->
                "Optional non-negative left edge of Rect.fromLTRB for nine-patch painting. All "
                + "four center-slice edges must be set together, with left strictly below right.";
            case "centerSliceTop" ->
                "Optional non-negative top edge of Rect.fromLTRB for nine-patch painting. All "
                + "four center-slice edges must be set together, with top strictly below bottom.";
            case "centerSliceRight" ->
                "Optional non-negative right edge of Rect.fromLTRB for nine-patch painting. All "
                + "four edges are atomic; right must exceed left and fit decoded image bounds.";
            case "centerSliceBottom" ->
                "Optional non-negative bottom edge of Rect.fromLTRB for nine-patch painting. All "
                + "four edges are atomic; bottom must exceed top and fit decoded image bounds.";
            case "matchTextDirection" ->
                "Whether RTL Directionality mirrors the image horizontally. Flutter defaults "
                + "to false; do not mirror imagery containing text or directional shadows.";
            case "gaplessPlayback" ->
                "Whether a previous image remains visible while the provider changes. Flutter "
                + "defaults to false to avoid pairing stale imagery with new content.";
            case "isAntiAlias" ->
                "Whether Flutter anti-aliases image edges, useful for rotated imagery. Flutter "
                + "defaults to false.";
            case "filterQuality" ->
                "Pinned Flutter FilterQuality used while sampling misaligned or scaled pixels. "
                + "Flutter defaults to medium.";
            default -> throw new IllegalStateException(
                    "Built-in Image property is missing reviewed help: "
                    + propertyName.value());
        };
    }

    private void addTextPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<TextWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(TextWidgetPropertySchema.Group.class);
        for (TextWidgetPropertySchema.Group group
                : TextWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }

        Sheet.Set unmatched = null;
        for (PropertyDefinition property : definition.properties()) {
            Optional<TextWidgetPropertySchema.Definition> schema =
                    TextWidgetPropertySchema.find(property.name());
            if (schema.isPresent()) {
                groups.get(schema.orElseThrow().group())
                        .put(projectProperty(property, schema));
                continue;
            }
            // Preserve contributed Text properties even when they are outside
            // the reviewed built-in scalar projection. Built-in Text never
            // reaches this fallback because its catalog/schema parity is tested.
            if (unmatched == null) {
                unmatched = propertySet(
                        PROPERTIES_SET_NAME,
                        "Other properties",
                        "Catalog properties outside the built-in Text scalar projection.");
                assignTab(unmatched, hasSlotTab ? GENERAL_TAB_NAME : null);
                sheet.put(unmatched);
            }
            unmatched.put(projectProperty(property, Optional.empty()));
        }
    }

    private void addIconPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<IconWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(IconWidgetPropertySchema.Group.class);
        for (IconWidgetPropertySchema.Group group
                : IconWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            IconWidgetPropertySchema.Definition schema =
                    IconWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in Icon property is missing its presentation schema: "
                                    + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description()));
        }
    }

    private void addSliverAppBarPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<AppBarWidgetPropertySchema.Group, Sheet.Set>(AppBarWidgetPropertySchema.Group.class);
        for (var group : AppBarWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            String name = property.name().value();
            var schema = AppBarWidgetPropertySchema.find(property.name()).orElse(null);
            var group = schema == null ? AppBarWidgetPropertySchema.Group.BEHAVIOR : schema.group();
            if (schema == null) {
                group = switch (name) {
                    case "shape" -> AppBarWidgetPropertySchema.Group.SHAPE;
                    case "iconTheme" -> AppBarWidgetPropertySchema.Group.ICON_THEME;
                    case "actionsIconTheme" -> AppBarWidgetPropertySchema.Group.ACTIONS_ICON_THEME;
                    case "toolbarTextStyle" -> AppBarWidgetPropertySchema.Group.TOOLBAR_TEXT_STYLE;
                    case "titleTextStyle" -> AppBarWidgetPropertySchema.Group.TITLE_TEXT_STYLE;
                    case "systemOverlayStyle" -> AppBarWidgetPropertySchema.Group.SYSTEM_UI;
                    case "collapsedHeight", "expandedHeight" -> AppBarWidgetPropertySchema.Group.LAYOUT;
                    default -> group;
                };
            }
            String label = schema == null ? name.replaceAll("([a-z])([A-Z])", "$1 $2") : schema.displayName();
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
            String propertyHelp = name.equals("toolbarHeight")
                    ? "Toolbar height in logical pixels. Unset uses the native constructor default: "
                            + dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.toolbarDefault(widget.type())
                            + ". "
                    : schema == null ? "" : schema.description() + " ";
            String help = propertyHelp + dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.DESCRIPTION;
            groups.get(group).put(projectProperty(property, Optional.empty(), label, help,
                    schema != null && schema.encoding() == AppBarWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    name.equals("onStretchTrigger") ? java.util.List.of("noop") : appBarStringPresets(property.name())));
        }
    }

    private void addAppBarPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<AppBarWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(AppBarWidgetPropertySchema.Group.class);
        for (AppBarWidgetPropertySchema.Group group
                : AppBarWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            AppBarWidgetPropertySchema.Definition schema =
                    AppBarWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in AppBar property is missing its presentation schema: "
                                    + property.name().value()));
            Node.Property<?> row = projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    schema.encoding()
                            == AppBarWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    appBarStringPresets(property.name()));
            if (property.name().value().equals("notificationPredicate")) {
                row.setValue(FlutterDartObjectReferenceEditorComponent.APP_BAR_PREDICATE_ATTRIBUTE, Boolean.TRUE);
            }
            groups.get(schema.group()).put(row);
        }
    }

    private void addScaffoldPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ScaffoldWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ScaffoldWidgetPropertySchema.Group.class);
        for (ScaffoldWidgetPropertySchema.Group group
                : ScaffoldWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ScaffoldWidgetPropertySchema.Definition schema =
                    ScaffoldWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in Scaffold property is missing its presentation schema: "
                                    + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    schema.presets()));
        }
    }

    private static java.util.List<String> appBarStringPresets(
            PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "notificationPredicate" ->
                AppBarWidgetPropertySchema.notificationPredicatePresets();
            case "shapeKind" -> java.util.List.of(
                    "roundedRectangle",
                    "stadium",
                    "circle",
                    "beveledRectangle",
                    "continuousRectangle");
            default -> java.util.List.of();
        };
    }

    private void addSubmenuButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<SubmenuButtonWidgetPropertySchema.Group, Sheet.Set>(SubmenuButtonWidgetPropertySchema.Group.class);
        for (var group : SubmenuButtonWidgetPropertySchema.Group.values()) {
            String label = group.displayName().startsWith("Style ") ? "Button style " + group.displayName().substring("Style ".length()) : group.displayName();
            var set = propertySet(group.setName(), label, group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = SubmenuButtonWidgetPropertySchema.find(property.name()).orElseThrow(); String name = property.name().value();
            String hint = name.equals("style") || SubmenuButtonWidgetPropertySchema.localStyleProperties().contains(name)
                    ? " Button style affects the opener. Whole Button style and its 498 local leaves switch atomically within this group only; Menu style, submenu icons and all children remain unchanged. Reset restores MenuButtonTheme/SDK fallback; Undo restores the exact previous branch."
                    : name.equals("menuStyle") || SubmenuButtonWidgetPropertySchema.menuStyleProperties().contains(name)
                            ? " Menu style affects the popup panel. Whole Menu style and its 203 local leaves switch atomically within this group only; Button style, submenu icons and all children remain unchanged. Native panels and cursors resolve an empty state set; non-default buckets remain stored/generated without invented interactive effects. A local density axis creates VisualDensity with an omitted peer equal to zero."
                            : name.equals("submenuIcon") || SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties().contains(name)
                                    ? " Whole Submenu icon and four local buckets switch atomically without changing either style. Configured buckets resolve Disabled, Hovered, Focused, then Default; omission skips to the next active configured bucket. Explicit null is terminal and uses MenuTheme/native arrow fallback. Material icon None creates Icon(null), an empty glyph, not null fallback. Custom widgets, colors and sizes can use a strict Widget reference."
                                    : " The native SubmenuButton opens itself. Empty Menu children disables it; Child may be empty and is emitted as null. No Enabled, On pressed or builder-template field is invented.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + hint
                    + " Project callbacks, controllers, style/layer builders and widget factories are never executed in isolated Canvas. Source bodies, slot order and child IDs are preserved.",
                    schema.encoding() == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    elevatedButtonStringPresets(new PropertyName(SubmenuButtonWidgetPropertySchema.menuStyleProperties().contains(name)
                            ? SubmenuButtonWidgetPropertySchema.menuStyleSourceName(name) : name))));
        }
    }

    private void addMenuAnchorPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<MenuAnchorWidgetPropertySchema.Group, Sheet.Set>(MenuAnchorWidgetPropertySchema.Group.class);
        for (var group : MenuAnchorWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = MenuAnchorWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String help = name.equals("style")
                    ? " Setting whole Menu style, including null, atomically clears all 203 local style leaves. Reset retains theme/SDK fallback; Undo restores the complete prior branch."
                    : MenuAnchorWidgetPropertySchema.localStyleProperties().contains(name)
                            ? " Setting a local style leaf atomically clears whole Menu style. Non-default state buckets are stored and generated but native panels and cursors resolve only the empty state set. Density changes horizontal padding only."
                            : name.equals("builder")
                                    ? " Create Menu Builder is an explicit source action, not a native Event. It creates a TextButton toggle using Child as label/content or Text('Menu') when empty. Interactive children are not rewired. The method remains user-editable; there is no automatic Save or navigation."
                                    : " Child is optional; Menu children is a required list that may be empty. Use its slot editor to add, move, remove or reorder menu items. An ordinary Child does not open the menu automatically; configure Builder or a user-owned controller action.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + help
                    + " Canvas Preview menu is isolated and transient: project builders, controllers and callbacks are never executed; source values and child IDs remain unchanged.",
                    false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addMenuBarPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<MenuBarWidgetPropertySchema.Group, Sheet.Set>(MenuBarWidgetPropertySchema.Group.class);
        for (var group : MenuBarWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = MenuBarWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = name.equals("style")
                    ? " Setting whole MenuBar style, including null, atomically clears all 203 local MenuStyle leaves. Reset restores MenuTheme/SDK fallback; Undo restores the complete prior branch."
                    : MenuBarWidgetPropertySchema.localStyleProperties().contains(name)
                            ? " Setting a local MenuBar style leaf atomically clears whole MenuBar style. Non-default state buckets are stored and generated but native menu-bar resolution remains SDK-owned. Density and alignment leaves are applied as one local compound."
                            : " MenuBar children is a required list that may be empty. Add, move, remove or reorder menu entries in the Slots tab. Controller references remain application-owned and are not executed in isolated Canvas.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint + " Project controllers and style references are analyzed but never executed in isolated Canvas; child IDs and source remain unchanged.",
                    false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addNavigationBarPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<NavigationBarWidgetPropertySchema.Group, Sheet.Set>(
                NavigationBarWidgetPropertySchema.Group.class);
        for (var group : NavigationBarWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = NavigationBarWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "selectedIndex" -> " The value must stay within the destination list; the designer preserves the exact value and reports an invalid index instead of clamping it. Destinations are edited in the Slots tab and may be empty while the node is being assembled.";
                case "onDestinationSelected" -> " This callback is represented by a typed reference, explicit null or a no-op. Project callbacks are analyzed but never executed in isolated Canvas.";
                case "animationDurationUs" -> " Stored as exact signed microseconds; a Duration reference is also accepted. Canvas uses the value only for preview timing.";
                case "overlayColor", "labelTextStyle", "indicatorShape" -> " Project references are retained in source and model; isolated Canvas keeps the SDK/theme fallback for these application-owned objects.";
                default -> " NavigationBar destinations remain application-owned widgets; add and reorder them in the Slots tab. Save/reopen and Undo/Redo preserve the complete list.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint,
                    false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addNavigationRailPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<NavigationRailWidgetPropertySchema.Group, Sheet.Set>(
                NavigationRailWidgetPropertySchema.Group.class);
        for (var group : NavigationRailWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = NavigationRailWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "selectedIndex" -> " The nullable index must stay within the destination list when set; invalid values are preserved and diagnosed rather than clamped. Destinations, leading and trailing widgets are edited in the Slots tab.";
                case "onDestinationSelected" -> " This callback is represented by a typed reference, explicit null or a no-op. Project callbacks are analyzed but never executed in isolated Canvas.";
                case "extended" -> " Flutter asserts that an extended rail uses no labels or NavigationRailLabelType.none. Conflicting values are retained and diagnosed without silent mutation.";
                case "minExtendedWidth" -> " When minWidth is also set, Flutter requires minExtendedWidth to be at least minWidth; invalid pairs are diagnosed without clamping.";
                case "unselectedLabelTextStyle", "selectedLabelTextStyle", "unselectedIconTheme", "selectedIconTheme", "indicatorShape" -> " Project references are retained in source and model; isolated Canvas keeps the SDK/theme fallback for these application-owned objects.";
                default -> " NavigationRail destinations remain application-owned widgets; add and reorder them in the Slots tab. Save/reopen and Undo/Redo preserve the complete list.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint,
                    false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addNavigationDrawerPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<NavigationDrawerWidgetPropertySchema.Group, Sheet.Set>(
                NavigationDrawerWidgetPropertySchema.Group.class);
        for (var group : NavigationDrawerWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = NavigationDrawerWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "selectedIndex" -> " The nullable index is validated against the destination list when set; invalid values are preserved and diagnosed rather than clamped. Header, footer and children are edited in the Slots tab.";
                case "onDestinationSelected" -> " This callback is represented by a typed reference, explicit null or a no-op. Project callbacks are analyzed but never executed in isolated Canvas.";
                case "indicatorShape" -> " Project references are retained in source and model; isolated Canvas keeps the SDK/theme fallback for this application-owned object.";
                default -> " NavigationDrawer children remain application-owned widgets; add and reorder them in the Slots tab. Save/reopen and Undo/Redo preserve the complete list.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint, false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addDrawerPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<DrawerWidgetPropertySchema.Group, Sheet.Set>(
                DrawerWidgetPropertySchema.Group.class);
        for (var group : DrawerWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = DrawerWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "shape" -> " Project ShapeBorder references are retained in source and model; isolated Canvas keeps the Drawer theme/SDK fallback and never executes application-owned objects.";
                case "semanticLabel" -> " Omission lets MaterialLocalizations provide the platform drawer label; an explicit string is emitted exactly.";
                case "clipBehavior" -> " Omission preserves the SDK/theme shape-dependent clipping policy.";
                default -> " Drawer child remains application-owned; edit it in the Slots tab. Save/reopen and Undo/Redo preserve the exact value and child identity.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint,
                    false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addBottomAppBarPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<BottomAppBarWidgetPropertySchema.Group, Sheet.Set>(
                BottomAppBarWidgetPropertySchema.Group.class);
        for (var group : BottomAppBarWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = BottomAppBarWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "shape" -> " Project NotchedShape references are retained in source and model; isolated Canvas keeps the SDK/theme rectangular fallback and never executes application-owned objects.";
                case "clipBehavior" -> " Omission preserves the SDK default Clip.none.";
                case "notchMargin" -> " This margin affects a notch only when a FloatingActionButton and shape are present; Canvas does not execute project NotchedShape references.";
                default -> " BottomAppBar child remains application-owned; edit it in the Slots tab. Save/reopen and Undo/Redo preserve the exact value and child identity.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint, false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addBottomNavigationBarPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<BottomNavigationBarWidgetPropertySchema.Group, Sheet.Set>(
                BottomNavigationBarWidgetPropertySchema.Group.class);
        for (var group : BottomNavigationBarWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = BottomNavigationBarWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "currentIndex" -> " The index is retained exactly; Canvas previews item 0 when it is outside the current list and the SDK will reject that value in the generated app.";
                case "onTap" -> " Canvas uses a no-op callback when a handler is configured; application callback code is never executed in the isolated preview.";
                case "selectedIconTheme", "unselectedIconTheme", "selectedLabelStyle", "unselectedLabelStyle", "mouseCursor" -> " Application-owned references are retained in source and model; Canvas keeps the SDK/theme fallback and never executes them.";
                default -> " Items remain application-owned widgets; edit order and membership in the Slots tab. Save/reopen and Undo/Redo preserve each child identity.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint, false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addMaterialPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<MaterialWidgetPropertySchema.Group, Sheet.Set>(
                MaterialWidgetPropertySchema.Group.class);
        for (var group : MaterialWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = MaterialWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "materialType" -> " The SDK argument is named type; the collision-free model key materialType preserves the global widget type contract.";
                case "shape", "borderRadius" -> " Shape references are retained in source and model. Canvas omits application-owned ShapeBorder and conflicting geometry rather than executing project code.";
                case "textStyle" -> " Project TextStyle references remain application-owned; isolated Canvas keeps the SDK/theme text style fallback.";
                case "animationDurationUs" -> " The exact signed microsecond value is retained; Canvas converts literals to Duration without changing the source model.";
                default -> " Material child remains application-owned; edit it in the Slots tab. Save/reopen and Undo/Redo preserve the exact value and child identity.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint, false, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addScrollbarPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<ScrollbarWidgetPropertySchema.Group, Sheet.Set>(
                ScrollbarWidgetPropertySchema.Group.class);
        for (var group : ScrollbarWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = ScrollbarWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            String hint = switch (name) {
                case "controller", "radius", "notificationPredicate" ->
                        " Project-owned references are retained in source and analyzed, but are not executed in the isolated Canvas preview.";
                case "scrollbarOrientation" ->
                        " Left/right apply to vertical scrolls and top/bottom to horizontal scrolls; an incompatible orientation is diagnosed by Flutter.";
                case "thumbVisibility", "trackVisibility", "interactive" ->
                        " Nullable booleans use the centered checkbox when explicit; <not set> preserves ScrollbarThemeData/platform defaults.";
                default -> " <not set> preserves the Flutter platform/theme default. The required Child is edited in Slots; no child is fabricated.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + hint));
        }
    }

    private void addMenuItemButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<MenuItemButtonWidgetPropertySchema.Group, Sheet.Set>(MenuItemButtonWidgetPropertySchema.Group.class);
        for (var group : MenuItemButtonWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = MenuItemButtonWidgetPropertySchema.find(property.name()).orElseThrow(); String name = property.name().value();
            String hint = MenuItemButtonWidgetPropertySchema.localStyleProperties().contains(name)
                    ? " Setting a local style leaf atomically clears whole Button style. All 498 leaves use nine state buckets; disabled is isolated. Reset preserves MenuButtonTheme/framework fallback."
                    : name.equals("style") ? " Setting whole Button style clears all 498 local style leaves atomically. Reset does not resurrect discarded leaves; Undo restores the complete prior style."
                    : name.startsWith("shortcut") ? " Shortcuts display a hint only; application code must register handling separately. Choose a trigger or character before modifiers; no key is invented. Setting whole Shortcut, including explicit null, clears all eight local fields. Choosing a local anchor clears whole Shortcut and the opposite anchor; Character also clears Shift and Num lock. Resetting an anchor resets the entire local shortcut, while resetting a modifier resets only that field. Compatible modifiers survive branch changes; Undo restores exact prior values."
                    : " Child, Leading icon and Trailing icon are independently optional. There is no TextButton variant, direct icon alignment or long-press Event. Enabled controls generated activation without erasing the retained handler.";
            if (name.equals("styleBackgroundBuilder") || name.equals("styleForegroundBuilder")) hint += " ButtonLayerBuilder returns a non-null Widget from BuildContext, Set<WidgetState> and nullable Widget child. MenuItemButton defaults clipBehavior to Clip.none; a custom layer does not silently change this explicit SDK default. Canvas cannot reproduce custom layer geometry.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + hint
                    + " Project callbacks, controllers, shortcut objects and builders are analyzed but never executed in isolated Canvas. Child IDs and user Dart bodies remain unchanged.",
                    schema.encoding() == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST, elevatedButtonStringPresets(property.name())));
        }
    }

    private void addTextButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<TextButtonWidgetPropertySchema.Group, Sheet.Set> groups = new EnumMap<>(TextButtonWidgetPropertySchema.Group.class);
        for (var group : TextButtonWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = TextButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            boolean variant = property.name().value().equals("variant");
            String hint = TextButtonWidgetPropertySchema.localStyleProperties().contains(property.name().value())
                    ? " Setting a local leaf atomically clears Button style. State priority is disabled, error, dragged, pressed, selected, scrolledUnder, hovered, focused, default. Disabled remains isolated from enabled buckets. Unset preserves TextButtonTheme/framework fallback."
                    : property.name().value().equals("style")
                            ? " Setting this reference atomically clears all local style leaves; resetting it does not restore discarded leaves. Undo restores the exact prior style."
                            : " Constructor changes retain Child. A populated Icon must be moved or cleared before selecting Standard; no widget is silently deleted.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + hint,
                    schema.encoding() == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    variant ? TextButtonWidgetPropertySchema.variants() : elevatedButtonStringPresets(property.name())));
        }
    }

    private void addOutlinedButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<OutlinedButtonWidgetPropertySchema.Group, Sheet.Set> groups = new EnumMap<>(OutlinedButtonWidgetPropertySchema.Group.class);
        for (var group : OutlinedButtonWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = OutlinedButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            String hint = OutlinedButtonWidgetPropertySchema.localStyleProperties().contains(property.name().value())
                    ? " Setting a local leaf atomically clears Button style. State priority is disabled, error, dragged, pressed, selected, scrolledUnder, hovered, focused, default. Disabled remains isolated from enabled buckets. Unset preserves OutlinedButtonTheme/framework fallback."
                    : property.name().value().equals("style")
                            ? " Setting this reference atomically clears all local style leaves; resetting it does not restore discarded leaves. Undo restores the exact prior style."
                            : " Constructor changes retain Child. A populated Icon must be moved or cleared before selecting Standard; no widget is silently deleted.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + hint,
                    schema.encoding() == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    property.name().value().equals("variant") ? OutlinedButtonWidgetPropertySchema.variants() : elevatedButtonStringPresets(property.name())));
        }
    }

    private void addFloatingActionButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<FloatingActionButtonWidgetPropertySchema.Group, Sheet.Set>(FloatingActionButtonWidgetPropertySchema.Group.class);
        for (var group : FloatingActionButtonWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = FloatingActionButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            java.util.List<String> presets = switch (property.name().value()) {
                case "variant" -> FloatingActionButtonWidgetPropertySchema.variants();
                case "shapeKind" -> FloatingActionButtonWidgetPropertySchema.shapeKinds();
                case "mouseCursor" -> FloatingActionButtonWidgetPropertySchema.mouseCursorPresets();
                default -> java.util.List.of();
            };
            groups.get(schema.group()).put(projectProperty(property,
                    FloatingActionButtonWidgetPropertySchema.textStyleBinding(property.name()),
                    schema.displayName(), schema.description()
                            + " Selecting Standard resets Extended-only fields; Small/Large also reset Mini and Extended state; Extended resets Mini. One Undo restores those scalar values. No Child or Icon is silently removed. "
                            + "Extended requires a Child as Label. Optional fields may be reset; Constructor and Enabled cannot be unset. "
                            + "Built-in/custom shapes and text Paint/color alternatives switch atomically. Project references are analyzed but never executed in isolated Canvas.",
                    false, presets));
        }
    }

    private void addCheckboxListTilePropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<CheckboxListTileWidgetPropertySchema.Group, Sheet.Set>(CheckboxListTileWidgetPropertySchema.Group.class);
        for (var group : CheckboxListTileWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = CheckboxListTileWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            var presets = switch (name) {
                case "variant" -> CheckboxListTileWidgetPropertySchema.variants();
                case "shapeKind", "checkboxShapeKind" -> CheckboxListTileWidgetPropertySchema.shapeKinds();
                case "onChanged", "onFocusChange" -> java.util.List.of("noop");
                default -> name.equals("mouseCursor") || CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)
                        ? CheckboxListTileWidgetPropertySchema.mouseCursorPresets()
                        : name.endsWith("Mode") ? java.util.List.of("border", "inherit") : java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Value, On changed and Constructor cannot be unset. Enabled is independently nullable; changing it retains the callback. "
                    + "Mixed Value enables Tristate; disabling/resetting Tristate while mixed sets Value false. Three line requires an existing Subtitle. "
                    + "Whole values and local families switch atomically; one Undo restores affected fields. No child or cursor Default is invented.", false, presets));
        }
    }

    private void addSwitchListTilePropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<SwitchListTileWidgetPropertySchema.Group, Sheet.Set>(SwitchListTileWidgetPropertySchema.Group.class);
        for (var group : SwitchListTileWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = SwitchListTileWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            var presets = switch (name) {
                case "variant" -> SwitchListTileWidgetPropertySchema.variants();
                case "shapeKind" -> SwitchListTileWidgetPropertySchema.shapeKinds();
                case "onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError" -> java.util.List.of("noop");
                default -> name.equals("mouseCursor") || SwitchListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)
                        ? SwitchListTileWidgetPropertySchema.mouseCursorPresets()
                        : SwitchListTileWidgetPropertySchema.thumbIconStates().stream().anyMatch(state -> SwitchListTileWidgetPropertySchema.thumbIconBucketProperties(state).getFirst().equals(name))
                                ? java.util.List.of("icon", "inherit") : java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Value, On changed and Constructor cannot be unset. Explicit null On changed disables the tile; no separate Enabled or Tristate exists. "
                    + "Standard retains Apply Cupertino theme without emitting it; Adaptive uses the stored value. Three line requires an existing Subtitle. "
                    + "Whole values and local families switch atomically; one Undo restores affected fields. No child or cursor Default is invented. "
                    + "Resetting a thumb image clears only its matching error callback. Project code is analyzed but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addListTilePropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<ListTileWidgetPropertySchema.Group, Sheet.Set>(ListTileWidgetPropertySchema.Group.class);
        for (var group : ListTileWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = ListTileWidgetPropertySchema.find(property.name()).orElseThrow();
            String name = property.name().value();
            var presets = java.util.List.of("onTap", "onLongPress", "onFocusChange").contains(name) ? java.util.List.of("noop")
                    : name.equals("shapeKind") ? ListTileWidgetPropertySchema.shapeKinds()
                    : name.equals("mouseCursor") || ListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)
                            ? ListTileWidgetPropertySchema.mouseCursorPresets()
                            : java.util.List.<String>of();
            groups.get(schema.group()).put(projectProperty(property, ListTileWidgetPropertySchema.textStyleBinding(property.name()),
                    schema.displayName(), schema.description() + " All fields are optional. Whole values and their local projections switch atomically; one Undo restores affected fields. No child or state Default is invented.", false, presets));
        }
    }

    private void addRangeSliderPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<RangeSliderWidgetPropertySchema.Group, Sheet.Set>(RangeSliderWidgetPropertySchema.Group.class);
        for (var group : RangeSliderWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = RangeSliderWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = RangeSliderWidgetPropertySchema.mouseCursorStateProperties().contains(property.name().value())
                    ? RangeSliderWidgetPropertySchema.mouseCursorPresets() : java.util.List.<String>of();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Optional fields can be reset. Start value, End value and Enabled cannot be unset. Range edits reject inconsistent peers without clamping. "
                    + "Whole labels, overlay and cursor references switch atomically with their respective local fields; one Undo restores affected values. Project references are verified but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addSliderPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<SliderWidgetPropertySchema.Group, Sheet.Set>(SliderWidgetPropertySchema.Group.class);
        for (var group : SliderWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = SliderWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = switch (property.name().value()) {
                case "variant" -> SliderWidgetPropertySchema.variants();
                case "mouseCursor" -> SliderWidgetPropertySchema.mouseCursorPresets();
                default -> java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Optional fields can be reset. Value, Constructor and Enabled cannot be unset. Setting Padding selects Standard; selecting Adaptive resets only Padding. "
                    + "Whole overlay references and local states switch atomically; one Undo restores affected fields. Range edits reject inconsistent peers without clamping. Adaptive Apple ignores unsupported rendering parameters but retains their stored values.", false, presets));
        }
    }

    private void addSwitchPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<SwitchWidgetPropertySchema.Group, Sheet.Set>(SwitchWidgetPropertySchema.Group.class);
        for (var group : SwitchWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = SwitchWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = switch (property.name().value()) {
                case "variant" -> SwitchWidgetPropertySchema.variants();
                case "mouseCursor" -> SwitchWidgetPropertySchema.mouseCursorPresets();
                default -> SwitchWidgetPropertySchema.thumbIconStates().stream().anyMatch(state -> SwitchWidgetPropertySchema.thumbIconBucketProperties(state).getFirst().equals(property.name().value()))
                        ? java.util.List.of("icon", "inherit") : java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Optional fields can be reset. Value, Constructor and Enabled cannot be unset. Setting Apply Cupertino theme selects Adaptive; Standard resets that branch-only field. "
                    + "Whole references and local families switch atomically; one Undo restores all affected fields. Resetting an image also clears only its matching error callback. Project references are analyzed but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addRadioGroupPropertySets(Sheet sheet, boolean hasSlotTab) {
        var group = RadioGroupWidgetPropertySchema.Group.BEHAVIOR;
        var set = propertySet(group.setName(), group.displayName(), group.description());
        assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); sheet.put(set);
        for (var property : definition.properties()) {
            var schema = RadioGroupWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = property.name().value().equals("onChanged") ? java.util.List.of("noop") : java.util.List.<String>of();
            set.put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " The Value Type dialog edits type, nullability and Group Value in one transaction; one Undo restores all three. "
                    + "The callback and descendant Radio types/values are preserved. Value Type and On Changed cannot be unset. "
                    + "Project references are analyzed but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addRadioPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<RadioWidgetPropertySchema.Group, Sheet.Set>(RadioWidgetPropertySchema.Group.class);
        for (var group : RadioWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = RadioWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = switch (property.name().value()) {
                case "variant" -> RadioWidgetPropertySchema.variants();
                case "onChanged" -> java.util.List.of("noop");
                case "mouseCursor" -> RadioWidgetPropertySchema.mouseCursorPresets();
                default -> property.name().value().endsWith("Mode") ? java.util.List.of("border", "inherit") : java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Optional fields can be reset; Value, Value type and Constructor cannot be unset. "
                    + "The Value type dialog can explicitly edit type, nullability, Value and Group value in one transaction. Other peers are preserved. "
                    + "Whole references and local families switch atomically; one Undo restores affected fields. "
                    + "Selecting Standard resets only the Adaptive-only checkmark flag. Project references are analyzed but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addRadioListTilePropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<RadioListTileWidgetPropertySchema.Group, Sheet.Set>(RadioListTileWidgetPropertySchema.Group.class);
        for (var group : RadioListTileWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = RadioListTileWidgetPropertySchema.find(property.name()).orElseThrow(); String name = property.name().value();
            var presets = switch (name) {
                case "variant" -> RadioListTileWidgetPropertySchema.variants();
                case "shapeKind" -> RadioListTileWidgetPropertySchema.shapeKinds();
                case "onChanged", "onFocusChange" -> java.util.List.of("noop");
                default -> name.equals("mouseCursor") || RadioListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)
                        ? RadioListTileWidgetPropertySchema.mouseCursorPresets()
                        : RadioWidgetPropertySchema.sideStates().stream().anyMatch(state -> RadioListTileWidgetPropertySchema.sideBucketProperties(state).getFirst().equals(name))
                                ? java.util.List.of("border", "inherit") : java.util.List.<String>of();
            };
            var row = projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " The Value type dialog edits Type, nullability, Value, legacy Group value and On changed atomically; one Undo restores all five fields. "
                    + "Modern RadioGroup is the preferred group owner. No groupRegistry property is invented. Legacy On changed omission, null and No-op remain distinct; Reset omits it. "
                    + "Standard retains the inactive Cupertino checkmark flag; Adaptive uses its stored value. Three line requires an existing Subtitle. "
                    + "Whole references and local families switch atomically; project code is analyzed but never executed in isolated Canvas. All three child slots remain unchanged.", false, presets);
            if (name.equals("onChanged")) row.setValue(FlutterDartObjectReferenceEditorComponent.RADIO_TILE_CALLBACK_ATTRIBUTE, Boolean.TRUE);
            groups.get(schema.group()).put(row);
        }
    }

    private void addTooltipThemePropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<TooltipThemeWidgetPropertySchema.Group, Sheet.Set>(TooltipThemeWidgetPropertySchema.Group.class);
        for (var group : TooltipThemeWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = TooltipThemeWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, TooltipThemeWidgetPropertySchema.textStyleBinding(property.name()),
                    schema.displayName(), schema.description()
                            + " Choose whole Data or local leaves. A whole Data reference conflicts with every local value, including explicit null and State bindings; reset the conflicting values explicitly first. Nothing is silently cleared. "
                            + "Within local data, Height/Constraints and whole/local Text style switch atomically. Omission and explicit null remain distinct; concrete booleans use centered checkboxes. "
                            + "With no whole Data and all local leaves unset, const TooltipThemeData() is emitted; this is a complete nearest theme, not an outer-theme merge. Child and user source remain unchanged. "
                            + "Project data, styles, decorations and duration references are analyzed but never executed in isolated Canvas. All three durations are constructed directly, without the pinned SDK copyWith exitDuration omission."));
        }
    }

    private void addTooltipPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<TooltipWidgetPropertySchema.Group, Sheet.Set>(TooltipWidgetPropertySchema.Group.class);
        for (var group : TooltipWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = TooltipWidgetPropertySchema.find(property.name()).orElseThrow(); String name = property.name().value();
            var presets = name.equals("mouseCursor") ? DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets()
                    : name.equals("onTriggered") ? java.util.List.of("noop") : java.util.List.<String>of();
            groups.get(schema.group()).put(projectProperty(property, TooltipWidgetPropertySchema.textStyleBinding(property.name()),
                    schema.displayName(), schema.description()
                            + " Setting non-null Message or Rich message replaces its non-null peer atomically; clearing the final content is rejected. "
                            + "Height/Constraints and whole/local Text style switch atomically. Explicit null and omission stay separate; Reset never invents replacement content. "
                            + "Child identities and user-owned Dart bodies remain unchanged. Use Wrap with Tooltip to keep an existing control as the anchor; ordinary Add allows an empty Child. "
                            + "Project spans, decorations, styles, durations, cursor functions, delegates and event callbacks are analyzed but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addExpansionTilePropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<ExpansionTileWidgetPropertySchema.Group, Sheet.Set>(ExpansionTileWidgetPropertySchema.Group.class);
        for (var group : ExpansionTileWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = ExpansionTileWidgetPropertySchema.find(property.name()).orElseThrow(); String name = property.name().value();
            var presets = switch (name) {
                case "shapeKind", "collapsedShapeKind" -> ExpansionTileWidgetPropertySchema.shapeKinds();
                case "expansionAnimationStyle" -> ExpansionTileWidgetPropertySchema.animationStylePresets();
                case "expansionAnimationStyleCurve", "expansionAnimationStyleReverseCurve" -> ExpansionTileWidgetPropertySchema.curvePresets();
                case "onExpansionChanged" -> java.util.List.of("noop");
                default -> java.util.List.<String>of();
            };
            var row = projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Palette creation wraps the selected widget as required Title; expanded content belongs in Children. Leading, Subtitle and Trailing are optional. "
                    + "Initially expanded is only a seed, not a controlled current value; changes after mounting do not command expansion. No two-way expansion State binding is invented. "
                    + "Whole/local expanded shape, collapsed shape, density and animation families switch atomically without altering other families or child slots. "
                    + "The pinned SDK stores but ignores AnimationStyle reverseDuration. Project controllers, curves, styles and callbacks are never executed in Canvas; source lifecycle remains user-owned.", false, presets);
            groups.get(schema.group()).put(row);
        }
    }

    private void addCheckboxPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<CheckboxWidgetPropertySchema.Group, Sheet.Set>(CheckboxWidgetPropertySchema.Group.class);
        for (var group : CheckboxWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = CheckboxWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = switch (property.name().value()) {
                case "variant" -> CheckboxWidgetPropertySchema.variants();
                case "shapeKind" -> CheckboxWidgetPropertySchema.shapeKinds();
                case "mouseCursor" -> CheckboxWidgetPropertySchema.mouseCursorPresets();
                default -> property.name().value().endsWith("Mode") ? java.util.List.of("border", "inherit") : java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Optional fields can be reset. Value, Constructor and Enabled cannot be unset. Null Value enables Tristate; disabling/resetting Tristate while mixed sets Value false. "
                    + "Whole references and local families switch atomically; one Undo restores all affected fields. Both constructors preserve explicit values. Project references are analyzed but never executed in isolated Canvas.", false, presets));
        }
    }

    private void addIconButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        var groups = new EnumMap<IconButtonWidgetPropertySchema.Group, Sheet.Set>(IconButtonWidgetPropertySchema.Group.class);
        for (var group : IconButtonWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = IconButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            String hint = IconButtonWidgetPropertySchema.localStyleProperties().contains(property.name().value())
                    ? " Setting a local leaf atomically clears Button style. State priority is disabled, error, dragged, pressed, selected, scrolledUnder, hovered, focused, default. Disabled remains isolated from enabled buckets. Unset preserves IconButtonTheme/framework fallback."
                    : property.name().value().equals("style")
                            ? " Setting this reference atomically clears all 498 local style leaves; resetting it does not restore discarded leaves. Undo restores the exact prior style."
                            : " All four constructors retain Icon, Selected icon and explicit properties. Selected icon remains stored when selection is unset or false. Restore Default omits an optional argument.";
            var presets = switch (property.name().value()) {
                case "variant" -> IconButtonWidgetPropertySchema.variants();
                case "mouseCursor" -> IconButtonWidgetPropertySchema.mouseCursorPresets();
                default -> elevatedButtonStringPresets(property.name());
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + hint,
                    schema.encoding() == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST, presets));
        }
    }

    private void addFilledButtonPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<FilledButtonWidgetPropertySchema.Group, Sheet.Set> groups = new EnumMap<>(FilledButtonWidgetPropertySchema.Group.class);
        for (var group : FilledButtonWidgetPropertySchema.Group.values()) {
            var set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = FilledButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            String hint = FilledButtonWidgetPropertySchema.localStyleProperties().contains(property.name().value())
                    ? " Setting a local leaf atomically clears Button style. State priority is disabled, error, dragged, pressed, selected, scrolledUnder, hovered, focused, default. Disabled remains isolated from enabled buckets. Unset preserves FilledButtonTheme/framework fallback."
                    : property.name().value().equals("style")
                            ? " Setting this reference atomically clears all local style leaves; resetting it does not restore discarded leaves. Undo restores the exact prior style."
                            : " Constructor changes retain Child. Standard and Tonal allow an empty Child; Icon and Tonal icon require a Label. A populated Icon must be moved or cleared before selecting Standard or Tonal; no widget is silently deleted.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description() + hint,
                    schema.encoding() == ElevatedButtonWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    property.name().value().equals("variant") ? FilledButtonWidgetPropertySchema.variants() : elevatedButtonStringPresets(property.name())));
        }
    }

    private void addElevatedButtonPropertySets(
            Sheet sheet,
            boolean hasSlotTab) {
        EnumMap<ElevatedButtonWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ElevatedButtonWidgetPropertySchema.Group.class);
        for (ElevatedButtonWidgetPropertySchema.Group group
                : ElevatedButtonWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ElevatedButtonWidgetPropertySchema.Definition schema =
                    ElevatedButtonWidgetPropertySchema.find(property.name())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Built-in ElevatedButton property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            Node.Property<?> row = projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    schema.encoding()
                            == ElevatedButtonWidgetPropertySchema.Encoding
                                    .NEWLINE_STRING_LIST,
                    elevatedButtonStringPresets(property.name()));
            if (ElevatedButtonWidgetPropertySchema.layerBuilderProperties().contains(property.name().value())) {
                row.setValue(FlutterDartObjectReferenceEditorComponent.ELEVATED_BUTTON_LAYER_ATTRIBUTE, Boolean.TRUE);
            }
            groups.get(schema.group()).put(row);
        }
    }

    private void addContainerPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ContainerWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ContainerWidgetPropertySchema.Group.class);
        for (ContainerWidgetPropertySchema.Group group
                : ContainerWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ContainerWidgetPropertySchema.Definition schema =
                    ContainerWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in Container property is missing its "
                                    + "presentation schema: " + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property, Optional.empty(), schema.displayName(), schema.description()));
        }
    }

    private void addTextFieldPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<TextFieldWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(TextFieldWidgetPropertySchema.Group.class);
        for (TextFieldWidgetPropertySchema.Group group
                : TextFieldWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            TextFieldWidgetPropertySchema.Definition schema =
                    TextFieldWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in TextField property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            Node.Property<?> row = projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    textFieldStringPresets(schema));
            if (property.name().value().equals("buildCounter") || property.name().value().equals("contextMenuBuilder")) {
                row.setValue(FlutterDartObjectReferenceEditorComponent.TEXT_FIELD_BUILDER_ATTRIBUTE, Boolean.TRUE);
            }
            groups.get(schema.group()).put(row);
        }
    }

    private void addListViewPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ListViewWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ListViewWidgetPropertySchema.Group.class);
        for (ListViewWidgetPropertySchema.Group group
                : ListViewWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ListViewWidgetPropertySchema.Definition schema =
                    ListViewWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ListView property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            java.util.List<String> presets =
                    schema.target() == ListViewWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? ListViewWidgetPropertySchema.PHYSICS_PRESETS
                            : java.util.List.of();
            Node.Property<?> row = projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    presets);
            if (property.name().value().equals("itemExtentBuilder")) {
                row.setValue(FlutterDartObjectReferenceEditorComponent.LIST_VIEW_EXTENT_BUILDER_ATTRIBUTE, Boolean.TRUE);
            }
            groups.get(schema.group()).put(row);
        }
    }

    private void addGridViewCountPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<GridViewCountWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(GridViewCountWidgetPropertySchema.Group.class);
        for (GridViewCountWidgetPropertySchema.Group group
                : GridViewCountWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            GridViewCountWidgetPropertySchema.Definition schema =
                    GridViewCountWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in GridView.count property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            java.util.List<String> presets = schema.target()
                    == GridViewCountWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? GridViewCountWidgetPropertySchema.PHYSICS_PRESETS
                            : java.util.List.of();
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    presets));
        }
    }

    private void addGridViewExtentPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<GridViewExtentWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(GridViewExtentWidgetPropertySchema.Group.class);
        for (GridViewExtentWidgetPropertySchema.Group group
                : GridViewExtentWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            GridViewExtentWidgetPropertySchema.Definition schema =
                    GridViewExtentWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in GridView.extent property is missing its "
                                    + "presentation schema: " + property.name().value()));
            java.util.List<String> presets = schema.target()
                    == GridViewExtentWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? GridViewExtentWidgetPropertySchema.PHYSICS_PRESETS
                            : java.util.List.of();
            groups.get(schema.group()).put(projectProperty(
                    property, Optional.empty(), schema.displayName(), schema.description(),
                    false, presets));
        }
    }

    private void addSingleChildScrollViewPropertySets(
            Sheet sheet,
            boolean hasSlotTab) {
        EnumMap<SingleChildScrollViewWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(SingleChildScrollViewWidgetPropertySchema.Group.class);
        for (SingleChildScrollViewWidgetPropertySchema.Group group
                : SingleChildScrollViewWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            SingleChildScrollViewWidgetPropertySchema.Definition schema =
                    SingleChildScrollViewWidgetPropertySchema.find(property.name())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Built-in SingleChildScrollView property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            java.util.List<String> presets = schema.target()
                    == SingleChildScrollViewWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? SingleChildScrollViewWidgetPropertySchema.PHYSICS_PRESETS
                            : java.util.List.of();
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    presets));
        }
    }

    private void addPageViewPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<PageViewWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(PageViewWidgetPropertySchema.Group.class);
        for (PageViewWidgetPropertySchema.Group group : PageViewWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            PageViewWidgetPropertySchema.Definition schema =
                    PageViewWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException("Built-in PageView property is missing its presentation schema: "
                                    + property.name().value()));
            java.util.List<String> presets = schema.target()
                    == PageViewWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? PageViewWidgetPropertySchema.PHYSICS_PRESETS : java.util.List.of();
            groups.get(schema.group()).put(projectProperty(
                    property, Optional.empty(), schema.displayName(), schema.description(), false, presets));
        }
    }

    private void addListWheelScrollViewPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ListWheelScrollViewWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ListWheelScrollViewWidgetPropertySchema.Group.class);
        for (ListWheelScrollViewWidgetPropertySchema.Group group
                : ListWheelScrollViewWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ListWheelScrollViewWidgetPropertySchema.Definition schema =
                    ListWheelScrollViewWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException("Built-in ListWheelScrollView property is missing its presentation schema: "
                                    + property.name().value()));
            java.util.List<String> presets = schema.target()
                    == ListWheelScrollViewWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? ListWheelScrollViewWidgetPropertySchema.PHYSICS_PRESETS : java.util.List.of();
            groups.get(schema.group()).put(projectProperty(
                    property, Optional.empty(), schema.displayName(), schema.description(), false, presets));
        }
    }

    private void addDynamicSliverPropertySet(Sheet sheet, boolean hasSlotTab) {
        var kind = dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.find(widget.type()).orElseThrow();
        Sheet.Set set = propertySet("sliverBuilder", "Sliver builder and delegates",
                "Typed project callbacks and delegates. Project code is preserved and never executed by isolated Canvas.");
        assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
        for (var field : dev.flutter.netbeans.designer.catalog.SliverDynamicWidgetPropertySchema.fields(kind)) {
            var property = definition.property(new PropertyName(field.name())).orElseThrow();
            set.put(projectProperty(property, Optional.empty(), field.displayName(), field.description(), false, field.presets()));
        }
        sheet.put(set);
    }

    private void addSliverChildrenPropertySet(Sheet sheet, boolean hasSlotTab) {
        Sheet.Set set = propertySet("sliverChildren", "Sliver children",
                "Static sliver list or grid constructor properties.");
        assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
        for (PropertyDefinition property : definition.properties()) {
            var schema = SliverChildrenWidgetPropertySchema.find(widget.type(), property.name()).orElseThrow();
            set.put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description(), false, java.util.List.of()));
        }
        sheet.put(set);
    }

    private void addCustomScrollViewPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<CustomScrollViewWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(CustomScrollViewWidgetPropertySchema.Group.class);
        for (CustomScrollViewWidgetPropertySchema.Group group
                : CustomScrollViewWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            CustomScrollViewWidgetPropertySchema.Definition schema =
                    CustomScrollViewWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException("Built-in CustomScrollView property is missing its presentation schema: "
                                    + property.name().value()));
            java.util.List<String> presets = schema.target()
                    == CustomScrollViewWidgetPropertySchema.Target.PHYSICS_PRESET
                            ? CustomScrollViewWidgetPropertySchema.PHYSICS_PRESETS : java.util.List.of();
            groups.get(schema.group()).put(projectProperty(
                    property, Optional.empty(), schema.displayName(), schema.description(), false, presets));
        }
    }

    private void addPreferredSizePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<PreferredSizeWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(PreferredSizeWidgetPropertySchema.Group.class);
        for (PreferredSizeWidgetPropertySchema.Group group
                : PreferredSizeWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            PreferredSizeWidgetPropertySchema.Definition schema =
                    PreferredSizeWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in PreferredSize property is missing its presentation schema: "
                                            + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description()
                            + " The value must be finite and non-negative. Palette creation "
                            + "starts at 100 × 56 logical pixels; Restore Default returns to that "
                            + "creation size."));
        }
    }

    private void addColoredBoxPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ColoredBoxWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ColoredBoxWidgetPropertySchema.Group.class);
        for (ColoredBoxWidgetPropertySchema.Group group
                : ColoredBoxWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ColoredBoxWidgetPropertySchema.Definition schema =
                    ColoredBoxWidgetPropertySchema.find(property.name())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Built-in ColoredBox property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            String description = switch (property.name().value()) {
                case "color" ->
                    "Required solid background color painted behind the optional child. "
                            + "Choose a literal ARGB color or a reviewed Material theme token; "
                            + "palette creation starts with opaque blue 0xFF2196F3.";
                case "isAntiAlias" ->
                    "Whether Flutter anti-aliases the painted color edge. Omission preserves "
                            + "ColoredBox's enabled default; an explicit value uses the "
                            + "standard checkbox editor.";
                default -> schema.description();
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    description));
        }
    }

    private void addSafeAreaPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<SafeAreaWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(SafeAreaWidgetPropertySchema.Group.class);
        for (SafeAreaWidgetPropertySchema.Group group
                : SafeAreaWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            SafeAreaWidgetPropertySchema.Definition schema =
                    SafeAreaWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in SafeArea property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description()));
        }
    }

    private void addDirectionalityPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<DirectionalityWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(DirectionalityWidgetPropertySchema.Group.class);
        for (DirectionalityWidgetPropertySchema.Group group
                : DirectionalityWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            DirectionalityWidgetPropertySchema.Definition schema =
                    DirectionalityWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in Directionality property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description()));
        }
    }

    private void addDecoratedBoxPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<DecoratedBoxWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(DecoratedBoxWidgetPropertySchema.Group.class);
        for (DecoratedBoxWidgetPropertySchema.Group group
                : DecoratedBoxWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            DecoratedBoxWidgetPropertySchema.Definition schema =
                    DecoratedBoxWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in DecoratedBox property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description()));
        }
    }

    private void addExcludeFocusPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ExcludeFocusWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ExcludeFocusWidgetPropertySchema.Group.class);
        for (ExcludeFocusWidgetPropertySchema.Group group : ExcludeFocusWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = ExcludeFocusWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " <not set> preserves Flutter's true default. True unfocuses descendants; "
                            + "false permits focus but does not automatically restore focus. "
                            + "Descendant local canRequestFocus configuration is not rewritten; effective "
                            + "focusability remains constrained by ancestors. Layout, paint and pointer "
                            + "hits remain available; Designer selection and editing are not disabled. "
                            + "Explicit true/false uses the centered checkbox; Restore Default returns to <not set>."));
        }
    }

    private void addExcludeFocusTraversalPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ExcludeFocusTraversalWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ExcludeFocusTraversalWidgetPropertySchema.Group.class);
        for (ExcludeFocusTraversalWidgetPropertySchema.Group group : ExcludeFocusTraversalWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = ExcludeFocusTraversalWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " <not set> preserves Flutter's true default. Tab traversal skips descendants, "
                            + "but direct requestFocus remains allowed and existing focus is retained. "
                            + "Local skipTraversal configuration is not rewritten; the effective getter "
                            + "reflects ancestor exclusion. Layout, paint and pointer hits remain available; "
                            + "Designer selection and editing are not disabled. "
                            + "Explicit true/false uses the centered checkbox; Restore Default returns to <not set>."));
        }
    }

    private void addRefreshIndicatorPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<RefreshIndicatorWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(RefreshIndicatorWidgetPropertySchema.Group.class);
        for (var group : RefreshIndicatorWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        var ordered = definition.properties().stream().sorted(java.util.Comparator.comparingInt(
                (PropertyDefinition property) -> property.name().value().equals("variant") ? -1
                        : RefreshIndicatorWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder())).toList();
        for (var property : ordered) {
            var schema = RefreshIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            var presets = switch (property.name().value()) {
                case "variant" -> java.util.List.of("material", "adaptive", "noSpinner");
                case "notificationPredicate" -> RefreshIndicatorWidgetPropertySchema.notificationPredicatePresets();
                default -> java.util.List.<String>of();
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + " All property rows remain stable across constructor changes. "
                            + "Setting On status change selects No spinner and clears its five unsupported visual fields; "
                            + "setting a spinner-only field in No spinner selects Material and clears On status change. "
                            + "Child and shared settings are retained in the same undoable edit.",
                    false, presets));
        }
    }

    private void addRefreshProgressIndicatorPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<RefreshProgressIndicatorWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(RefreshProgressIndicatorWidgetPropertySchema.Group.class);
        for (var group : RefreshProgressIndicatorWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = RefreshProgressIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + " Restore Default omits this optional field; no defaults are stored. Stroke width has three distinct modes: omitted uses the constructor default 2.5; inherited (explicit null) uses the theme or SDK fallback 4; a signed finite number is retained exactly. "
                    + "Value color accepts stopped literal/theme colors, explicit stopped null, or typed project Animation<Color?>. Project animations generate Dart but cannot execute in isolated Canvas. "
                    + "This is the progress visual, not the RefreshIndicator pull-to-refresh wrapper."));
        }
    }

    private void addCircularProgressIndicatorPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<CircularProgressIndicatorWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(CircularProgressIndicatorWidgetPropertySchema.Group.class);
        for (var group : CircularProgressIndicatorWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        var orderedProperties = definition.properties().stream().sorted(java.util.Comparator.comparingInt(
                (PropertyDefinition property) -> property.name().value().equals("variant") ? -1
                        : CircularProgressIndicatorWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder())).toList();
        for (var property : orderedProperties) {
            var schema = CircularProgressIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            boolean variant = property.name().value().equals("variant");
            String hint = variant
                    ? " Required Designer constructor selector: material uses CircularProgressIndicator(), adaptive uses CircularProgressIndicator.adaptive(). Cannot be unset or reset. Switching to adaptive atomically removes Color, which that constructor does not accept."
                    : " Restore Default omits this optional field; SDK defaults are not stored. Setting Color in adaptive mode atomically selects material. Setting Value or Controller atomically clears the other. "
                            + "Value color supports stopped literal/theme colors, explicit stopped null and a typed project Animation<Color?>. Project animation/controller references are emitted to Dart but cannot execute in isolated Canvas. "
                            + "Explicit Year 2023 values use a centered checkbox; unset remains distinct.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + hint, false, variant ? java.util.List.of("material", "adaptive") : java.util.List.of()));
        }
    }

    private void addLinearProgressIndicatorPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<LinearProgressIndicatorWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(LinearProgressIndicatorWidgetPropertySchema.Group.class);
        for (var group : LinearProgressIndicatorWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = LinearProgressIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                    + (property.name().value().equals("semanticsValue")
                            ? " For determinate progress with semantics enabled, Flutter expects a number or percentage in 0..100, such as '45' or '45%'; arbitrary text is valid for indeterminate progress. " : "")
                    + " Restore Default omits this optional field; no SDK defaults are stored. "
                    + "Setting Value or Controller atomically clears the other in one undoable edit. "
                    + "Value color supports stopped literal/theme colors, explicit stopped null and a typed project Animation<Color?>. "
                    + "Project animation/controller references are emitted to Dart but cannot execute in isolated Canvas. "
                    + "Explicit Year 2023 values use a centered checkbox; unset remains distinct."));
        }
    }

    private void addCircleAvatarPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<CircleAvatarWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(CircleAvatarWidgetPropertySchema.Group.class);
        for (var group : CircleAvatarWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (var property : definition.properties()) {
            var schema = CircleAvatarWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " Restore Default omits this optional field; no defaults are stored on creation. "
                            + "Radius and Min/Max radius switch atomically in one undoable edit. "
                            + "Enter Infinity for explicit double.infinity; invalid radius bounds are rejected, not clamped. "
                            + "Image-error callbacks require their matching image provider; resetting that provider "
                            + "also resets its callback in one undoable edit."));
        }
    }

    private void addBadgePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<BadgeWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(BadgeWidgetPropertySchema.Group.class);
        for (BadgeWidgetPropertySchema.Group group : BadgeWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = BadgeWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property,
                    BadgeWidgetPropertySchema.textStyleBinding(property.name()),
                    schema.displayName(), schema.description()
                            + " Restore Default returns this optional field to <not set>; no SDK defaults "
                            + "are stored on creation. Resetting Count also resets Max count in one undoable edit. "
                            + "Explicit boolean values use a centered checkbox. Foreground/background Paint "
                            + "and the corresponding style color are mutually exclusive and switch atomically."));
        }
    }

    private void addCardPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<CardWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(CardWidgetPropertySchema.Group.class);
        for (CardWidgetPropertySchema.Group group : CardWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = CardWidgetPropertySchema.find(property.name()).orElseThrow();
            java.util.List<String> presets = switch (property.name().value()) {
                case "variant" -> java.util.List.of("elevated", "filled", "outlined");
                case "shapeKind" -> CardWidgetPropertySchema.shapeKinds();
                default -> java.util.List.of();
            };
            String hint = property.name().value().equals("variant")
                    ? " Required Designer constructor selector: elevated uses Card(), filled uses Card.filled(), "
                            + "outlined uses Card.outlined(); cannot be unset or reset. Material 2 treats all three alike."
                    : " Restore Default omits this optional field; no SDK defaults are stored."
                            + " Custom Shape and built-in shape details are mutually exclusive. Changing shape kind "
                            + "atomically removes incompatible details while preserving compatible border-side values. "
                            + "Editing a shape detail selects a compatible kind when necessary; resetting Shape kind "
                            + "clears its built-in details. Star point and valley rounding must sum to at most one. "
                            + "Custom Dart shapes cannot execute in the isolated Canvas; very complex stars above "
                            + "4096 points retain their data with an explicit preview-unavailable state.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + hint, false, presets));
        }
    }

    private void addVerticalDividerPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<VerticalDividerWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(VerticalDividerWidgetPropertySchema.Group.class);
        for (VerticalDividerWidgetPropertySchema.Group group : VerticalDividerWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = VerticalDividerWidgetPropertySchema.find(property.name()).orElseThrow();
            String paintHint = switch (property.name().value()) {
                case "thickness", "radius" -> " Use positive thickness for a rounded line: with nonzero radius "
                        + "and a zero-thickness hairline, Flutter debug painting asserts and release painting ignores radius. Unset thickness can resolve to "
                        + "0 in Material 2 or 1 in Material 3 unless DividerTheme overrides it. "
                        + "Designer preserves the entered values without silently adjusting thickness or radius.";
                default -> "";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + paintHint
                            + " Restore Default returns this optional field to <not set> and omits its argument. "
                            + "No constructor defaults are stored when VerticalDivider is created. "
                            + "Use a bounded parent height, such as a Row in a SizedBox with height or IntrinsicHeight, "
                            + "to make the vertical line visible; Designer does not synthesize a height."));
        }
    }

    private void addDividerPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<DividerWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(DividerWidgetPropertySchema.Group.class);
        for (DividerWidgetPropertySchema.Group group : DividerWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = DividerWidgetPropertySchema.find(property.name()).orElseThrow();
            String paintHint = switch (property.name().value()) {
                case "thickness", "radius" -> " Use positive thickness for a rounded line: with nonzero radius "
                        + "and a zero-thickness hairline, Flutter debug painting asserts and release painting ignores radius. Unset thickness can resolve to "
                        + "0 in Material 2 or 1 in Material 3 unless DividerTheme overrides it. "
                        + "Designer preserves the entered values without silently adjusting thickness or radius.";
                default -> "";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + paintHint
                            + " Restore Default returns this optional field to <not set> and omits its argument. "
                            + "No constructor defaults are stored when Divider is created."));
        }
    }

    private void addImageIconPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ImageIconWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ImageIconWidgetPropertySchema.Group.class);
        for (ImageIconWidgetPropertySchema.Group group : ImageIconWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = ImageIconWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + (property.name().value().equals("image")
                                    ? " Required positional argument: None is explicit Dart null, not <not set>. "
                                            + "Choose a declared app/package AssetImage or ExactAssetImage, with optional "
                                            + "ResizeImage dimensions, exact/fit policy, and upscaling. Imported unresolved "
                                            + "providers remain editable placeholders; None creates no asset dependency. "
                                            + "The argument cannot be unset or reset. Network, File, and custom providers are not supported."
                                    : " Restore Default returns this optional field to <not set>. "
                                            + "Size and color inherit IconTheme; its opacity still multiplies explicit color alpha. "
                                            + "ImageIcon tints the source alpha mask, rather than preserving original colors.")));
        }
    }

    private void addIconThemePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<IconThemeWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(IconThemeWidgetPropertySchema.Group.class);
        for (IconThemeWidgetPropertySchema.Group group : IconThemeWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = IconThemeWidgetPropertySchema.find(property.name()).orElseThrow();
            String reset = "merge".equals(property.name().value())
                    ? " Merge is a required Designer-only choice, created as false; it cannot be unset or reset. "
                    : " Restore Default returns this optional IconThemeData field to <not set>. ";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " Merge true emits IconTheme.merge and inherits unset fields from the outer theme. "
                            + "Merge false emits direct IconTheme data; IconTheme.of supplies SDK fallbacks for unset fields. "
                            + "Local Icon fields take precedence where supported; inherited theme opacity still multiplies "
                            + "explicit local Icon color alpha. Color supports literal and semantic theme previews. "
                            + "Explicit empty Shadows clears the inherited list; entries retain stable IDs. "
                            + "Explicit booleans use centered checkboxes." + reset
                            + "Designer child and descendant editing remain available."));
        }
    }

    private void addDefaultSelectionStylePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<DefaultSelectionStyleWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(DefaultSelectionStyleWidgetPropertySchema.Group.class);
        for (DefaultSelectionStyleWidgetPropertySchema.Group group
                : DefaultSelectionStyleWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = DefaultSelectionStyleWidgetPropertySchema.find(property.name()).orElseThrow();
            String reset = "merge".equals(property.name().value())
                    ? " Merge is a required Designer-only choice, created as false; it cannot be unset or reset. "
                            + "Both values use the centered checkbox. "
                    : " Restore Default returns this SDK field to <not set>. ";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " Merge true emits DefaultSelectionStyle.merge and inherits each unset field from the ancestor. "
                            + "Merge false emits the direct constructor: unset fields are null, not inherited. "
                            + "Colors support literal and semantic theme values with previews; Mouse cursor is a closed list "
                            + "of 36 SystemMouseCursors, MouseCursor.defer/uncontrolled and three WidgetStateMouseCursor presets. "
                            + reset
                            + "Local selection settings can override these defaults; this wrapper does not make a child selectable. "
                            + "Designer child and descendant editing remain available.", false,
                    "mouseCursor".equals(property.name().value())
                            ? DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets() : java.util.List.of()));
        }
    }

    private void addDefaultTextHeightBehaviorPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<DefaultTextHeightBehaviorWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(DefaultTextHeightBehaviorWidgetPropertySchema.Group.class);
        for (DefaultTextHeightBehaviorWidgetPropertySchema.Group group
                : DefaultTextHeightBehaviorWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = DefaultTextHeightBehaviorWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " <not set> preserves true for both Apply flags and proportional for Leading distribution. "
                            + "Even with all leaves unset, the required TextHeightBehavior object is emitted with its defaults; "
                            + "the wrapper does not pass through an outer behavior. Local or nearer behavior can override it. "
                            + "These flags affect text with a height multiplier without inventing a font size or height. "
                            + "Explicit booleans use centered checkboxes; Restore Default returns this leaf to <not set>. "
                            + "Designer child and descendant editing remain available."));
        }
    }

    private void addTooltipVisibilityPropertySets(Sheet sheet, boolean hasSlotTab) {
        for (var group : TooltipVisibilityWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            for (PropertyDefinition property : definition.properties()) {
                var schema = TooltipVisibilityWidgetPropertySchema.find(property.name()).orElseThrow();
                if (schema.group() == group) {
                    set.put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()
                            + " Required boolean: palette creation supplies true, not an SDK default. False is valid; Visible cannot be unset or reset. "
                            + "The nearest scope wins; a nearer true overrides an outer false (not AND). This suppresses Tooltip display, "
                            + "not the child. Overlay dismissal and Tooltip annotations follow the installed Flutter SDK; child accessibility remains available. "
                            + "Explicit values use the centered checkbox. State can bind this value; isolated Canvas uses the retained literal preview without executing project code."));
                }
            }
            sheet.put(set);
        }
    }

    private void addTickerModePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<TickerModeWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(TickerModeWidgetPropertySchema.Group.class);
        for (TickerModeWidgetPropertySchema.Group group : TickerModeWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = TickerModeWidgetPropertySchema.find(property.name()).orElseThrow();
            String help = "enabled".equals(property.name().value())
                    ? " Required boolean: palette creation supplies true; this is not an SDK default. "
                            + "False is valid. Enabled cannot be unset or reset. Effective enabled is combined with ancestors using AND. "
                    : " <not set> preserves Flutter's false default. True requests frames when normal frame scheduling is suspended; "
                            + "it does not unmute disabled tickers and may increase battery usage. Effective force frames is combined with ancestors using OR, "
                            + "independently of Enabled. Restore Default returns to <not set>. ";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + help
                            + "Explicit true/false uses the centered checkbox. Muting suppresses callbacks, not elapsed time; "
                            + "Designer selection and editing remain available."));
        }
    }

    private void addVisibilityPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<VisibilityWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(VisibilityWidgetPropertySchema.Group.class);
        for (VisibilityWidgetPropertySchema.Group group : VisibilityWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = VisibilityWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " <not set> preserves Flutter's constructor default; explicit true/false uses the centered checkbox. "
                            + "Restore Default returns to <not set>. Required Maintain prerequisites are enabled atomically; "
                            + "disabling or resetting a prerequisite unsets enabled dependent flags in the same Undo step. "
                            + "Changing Maintain flags can discard descendant state. All six Maintain flags true "
                            + "represent Visibility.maintain. Replacement is ignored while Maintain state is true; "
                            + "Designer selection and editing remain available."));
        }
    }

    private void addIndexedSemanticsPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<IndexedSemanticsWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(IndexedSemanticsWidgetPropertySchema.Group.class);
        for (IndexedSemanticsWidgetPropertySchema.Group group : IndexedSemanticsWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = IndexedSemanticsWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " Required signed integer; creation starts at 0 and this value cannot be unset. "
                            + "The portable exact range is -9007199254740991 through 9007199254740991. "
                            + "Negative values are accepted by the pinned Flutter constructor. "
                            + "This annotates semantic position; it does not sort children or create labels. "
                            + "Layout, painting and pointer hit testing remain unchanged."));
        }
    }

    private void addBlockSemanticsPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<BlockSemanticsWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(BlockSemanticsWidgetPropertySchema.Group.class);
        for (BlockSemanticsWidgetPropertySchema.Group group : BlockSemanticsWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = BlockSemanticsWidgetPropertySchema.find(property.name()).orElseThrow();
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description()
                            + " <not set> omits the argument and preserves Flutter's true default. "
                            + "True hides earlier-painted semantic nodes in the same semantics container; "
                            + "own child and later nodes remain. False disables this semantics blocking. "
                            + "Unlike ExcludeSemantics it does not exclude its own child, and unlike AbsorbPointer "
                            + "it does not block pointer hits. Layout and painting are unchanged. "
                            + "Explicit true/false uses the centered checkbox; Restore Default returns to <not set>."));
        }
    }

    private void addAbsorbPointerPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<AbsorbPointerWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(AbsorbPointerWidgetPropertySchema.Group.class);
        for (AbsorbPointerWidgetPropertySchema.Group group : AbsorbPointerWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = AbsorbPointerWidgetPropertySchema.find(property.name()).orElseThrow();
            String hint = property.name().value().equals("absorbing")
                    ? " <not set> omits the argument and preserves Flutter's true default. "
                            + "True stops pointer hit testing at this widget and blocks its child and targets behind it; "
                            + "false restores normal hit testing. Unlike IgnorePointer, events do not pass through. "
                            + "Layout and painting are unchanged; Designer selection and editing remain available."
                    : " Deprecated Flutter argument, retained for compatibility. <not set> means default null: "
                            + "while Absorbing is true, labels and other semantics remain but user actions are blocked. "
                            + "Explicit false preserves semantics including actions even when Absorbing is true. "
                            + "Explicit true removes the whole semantics subtree regardless of Absorbing. "
                            + "Prefer leaving this unset; use ExcludeSemantics for intentional subtree exclusion.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + hint
                            + " Explicit true/false uses the centered checkbox; Restore Default returns to <not set>."));
        }
    }

    private void addGestureDetectorPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<GestureDetectorWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(GestureDetectorWidgetPropertySchema.Group.class);
        for (GestureDetectorWidgetPropertySchema.Group group : GestureDetectorWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = GestureDetectorWidgetPropertySchema.find(property.name()).orElseThrow();
            String help = schema.description()
                    + " <not set> omits this constructor argument. Callback bodies remain in the Dart source; "
                    + "the Events editor supports Create, Bind, Go to Handler, Rename and Disconnect. "
                    + "Designer selection remains available; user gesture callbacks are not executed by the design-time Canvas.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), help));
        }
    }

    private void addListenerPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ListenerWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ListenerWidgetPropertySchema.Group.class);
        for (ListenerWidgetPropertySchema.Group group : ListenerWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set); sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = ListenerWidgetPropertySchema.find(property.name()).orElseThrow();
            String help = schema.description() + (schema.callbackType().isPresent()
                    ? " <not set> omits the callback; explicit null is retained separately. Use Events to Create, Bind, Go to Handler, Rename or Disconnect. "
                            + "Raw pointer events do not recognize gestures. User handlers remain in Dart source and never execute in Designer Canvas."
                    : " <not set> uses Flutter's deferToChild default. HitTestBehavior controls runtime hit testing; Designer selection remains available.");
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), help));
        }
    }

    private void addMouseRegionPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<MouseRegionWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(MouseRegionWidgetPropertySchema.Group.class);
        for (MouseRegionWidgetPropertySchema.Group group : MouseRegionWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set); sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = MouseRegionWidgetPropertySchema.find(property.name()).orElseThrow();
            String help = schema.description() + (schema.callbackType().isPresent()
                    ? " <not set> omits the callback; explicit null remains distinct. Use Events to Create, Bind, Go to Handler, Rename or Disconnect. User handlers stay in Dart source and never execute in Designer Canvas."
                    : property.name().value().equals("cursor")
                            ? " Choose a reviewed cursor preset or an analyzer-verified MouseCursor reference. Omission uses MouseCursor.defer; null is not accepted. Project cursor code is never executed in Designer Canvas."
                            : " <not set> preserves the Flutter constructor default. Runtime mouse hit testing is separate from Designer selection.");
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), help, false,
                    property.name().value().equals("cursor") ? DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets() : java.util.List.of()));
        }
    }

    private void addFocusPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<FocusWidgetPropertySchema.Group, Sheet.Set> groups = new EnumMap<>(FocusWidgetPropertySchema.Group.class);
        for (var group : FocusWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); groups.put(group, set); sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = FocusWidgetPropertySchema.find(property.name()).orElseThrow();
            String help = schema.description() + switch (property.name().value()) {
                case "variant" -> " Standard allows Flutter to allocate and own an internal FocusNode. Set a non-null typed Focus node before selecting withExternalFocusNode. Constructor switches retain inactive values; no user configuration or child is silently deleted.";
                case "focusNode", "parentNode" -> " Project FocusNode references are hosted, not owned, by this widget. Their application owner must manage lifetime and dispose; Designer never creates or disposes a project node. External mode requires a non-null Focus node. Standard permits omission or null.";
                case "onKeyEvent", "onKey" -> " Return KeyEventResult explicitly in Source; generated stubs throw UnimplementedError until implemented. These stored bindings are inactive in withExternalFocusNode, which reads keyboard callbacks from its FocusNode. Canvas never executes application key handlers.";
                default -> " <not set> preserves the selected constructor's Flutter default. In withExternalFocusNode, retained node attributes are read from the project's FocusNode instead of these stored overrides. Designer Canvas never runs user handlers or takes application keyboard focus.";
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(), schema.displayName(), help, false,
                    property.name().value().equals("variant") ? java.util.List.of("standard", "withExternalFocusNode") : java.util.List.of()));
        }
    }

    private void addNotificationListenerPropertySets(Sheet sheet, boolean hasSlotTab) {
        var group = NotificationListenerWidgetPropertySchema.Group.BEHAVIOR;
        Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
        assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null); sheet.put(set);
        for (PropertyDefinition property : definition.properties()) {
            var schema = NotificationListenerWidgetPropertySchema.find(property.name()).orElseThrow();
            set.put(projectProperty(property, Optional.empty(), schema.displayName(), schema.description()));
        }
    }

    private void addBuilderPropertySets(Sheet sheet, boolean hasSlotTab) {
        var group = BuilderWidgetPropertySchema.Group.BUILDER;
        Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
        assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
        sheet.put(set);
        for (PropertyDefinition property : definition.properties()) {
            var schema = BuilderWidgetPropertySchema.find(property.name()).orElseThrow();
            set.put(projectProperty(property, Optional.empty(), schema.displayName(),
                    schema.description() + " The required callback is stored as a typed Dart identifier; "
                            + "Restore Default returns to the Designer no-op preview. "
                            + "The isolated Canvas never executes project callback bodies."));
        }
    }

    private void addIgnorePointerPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<IgnorePointerWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(IgnorePointerWidgetPropertySchema.Group.class);
        for (IgnorePointerWidgetPropertySchema.Group group : IgnorePointerWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            var schema = IgnorePointerWidgetPropertySchema.find(property.name()).orElseThrow();
            String hint = property.name().value().equals("ignoring")
                    ? " <not set> omits the argument and preserves Flutter's true default. "
                            + "True removes this subtree from pointer hit testing; false restores normal hit testing. "
                            + "Layout and painting are unchanged; Designer editing is not disabled."
                    : " Deprecated Flutter argument, retained for compatibility. <not set> means default null: "
                            + "while Ignoring is true, labels and other semantics remain but user actions are blocked. "
                            + "Explicit false preserves semantics including actions even when Ignoring is true. "
                            + "Explicit true removes the whole semantics subtree regardless of Ignoring. "
                            + "Prefer leaving this unset; use ExcludeSemantics for intentional subtree exclusion.";
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + hint
                            + " Explicit true/false uses the centered checkbox; Restore Default returns to <not set>."));
        }
    }

    private void addExcludeSemanticsPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ExcludeSemanticsWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ExcludeSemanticsWidgetPropertySchema.Group.class);
        for (ExcludeSemanticsWidgetPropertySchema.Group group
                : ExcludeSemanticsWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ExcludeSemanticsWidgetPropertySchema.Definition schema =
                    ExcludeSemanticsWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ExcludeSemantics property is missing its "
                                    + "presentation schema: " + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description()
                            + " Omission preserves Flutter's true default; explicit true "
                            + "or false uses the standard checkbox editor."));
        }
    }

    private void addIndexedStackPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<IndexedStackWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(IndexedStackWidgetPropertySchema.Group.class);
        for (IndexedStackWidgetPropertySchema.Group group
                : IndexedStackWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            IndexedStackWidgetPropertySchema.Definition schema =
                    IndexedStackWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in IndexedStack property is missing its "
                                    + "presentation schema: " + property.name().value()));
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    indexedStackPropertyDescription(property.name())));
        }
    }

    private static String indexedStackPropertyDescription(PropertyName propertyName) {
        return switch (propertyName.value()) {
            case "alignment" ->
                "Physical or directional position shared by all children. Flutter defaults "
                + "to directional top-start; directional values resolve from the explicit "
                + "Text direction when set, otherwise ambient LTR/RTL Directionality.";
            case "textDirection" ->
                "Optional LTR or RTL override used only to resolve directional Alignment. "
                + "Omission preserves ambient Directionality; this is not a theme value.";
            case "clipBehavior" ->
                "How direct child geometry that overflows IndexedStack bounds is clipped. "
                + "Omission preserves Flutter's hard-edge default; descendant or paint-only "
                + "overflow does not by itself trigger RenderStack clipping.";
            case "sizing" ->
                "How non-positioned children contribute constraints and size: loose relaxes "
                + "incoming minimums, expand tightens to the largest allowed size, and "
                + "passthrough preserves incoming constraints. Flutter defaults to loose.";
            case "index" ->
                "Three distinct states: <not set> omits the argument and uses Flutter's "
                + "default index 0; explicit null paints, hit-tests, and exposes semantics "
                + "for no child; a non-negative integer selects that zero-based child. "
                + "With children present the integer must be below their count; an empty "
                + "IndexedStack accepts only effective index 0 or explicit null. Never use "
                + "-1 as a sentinel.";
            default -> "Explicit IndexedStack value for " + propertyName.value() + ".";
        };
    }

    private void addClipRectPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ClipRectWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ClipRectWidgetPropertySchema.Group.class);
        for (ClipRectWidgetPropertySchema.Group group
                : ClipRectWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ClipRectWidgetPropertySchema.Definition schema =
                    ClipRectWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ClipRect property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "clipper" -> " The Dart analyzer validates that the selected "
                        + "Dart symbol is assignable to CustomClipper<Rect>. "
                        + "Current-library and package-config-declared references, "
                        + "including zero-argument constructor or factory invocations, "
                        + "are supported. The isolated Canvas cannot execute project Dart "
                        + "and displays an explicit preview-unavailable state.";
                case "clipBehavior" -> " Omission preserves Flutter's hard-edge default.";
                default -> throw new IllegalStateException(
                        "Unexpected ClipRect property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description() + boundary));
        }
    }

    private void addClipOvalPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ClipOvalWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ClipOvalWidgetPropertySchema.Group.class);
        for (ClipOvalWidgetPropertySchema.Group group
                : ClipOvalWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ClipOvalWidgetPropertySchema.Definition schema =
                    ClipOvalWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ClipOval property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "clipper" -> " The Dart analyzer validates that the selected "
                        + "Dart symbol is assignable to CustomClipper<Rect>. "
                        + "Current-library and package-config-declared references, "
                        + "including zero-argument constructor or factory invocations, "
                        + "are supported. The isolated Canvas cannot execute project Dart "
                        + "and displays an explicit preview-unavailable state.";
                case "clipBehavior" -> " Omission preserves Flutter's anti-alias default.";
                default -> throw new IllegalStateException(
                        "Unexpected ClipOval property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description() + boundary));
        }
    }

    private void addClipRRectPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ClipRRectWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ClipRRectWidgetPropertySchema.Group.class);
        for (ClipRRectWidgetPropertySchema.Group group
                : ClipRRectWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ClipRRectWidgetPropertySchema.Definition schema =
                    ClipRRectWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ClipRRect property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "borderRadius" -> " Omission preserves Flutter's "
                        + "BorderRadius.zero default. Physical and directional "
                        + "elliptical corners are supported. Flutter ignores "
                        + "borderRadius while clipper is configured.";
                case "clipper" -> " The Dart analyzer validates that the selected "
                        + "Dart symbol is assignable to CustomClipper<RRect>. "
                        + "Current-library and package-config-declared references, "
                        + "including zero-argument constructor or factory invocations, "
                        + "are supported. When configured, Flutter ignores borderRadius. "
                        + "The isolated Canvas cannot execute project Dart and displays "
                        + "an explicit preview-unavailable state.";
                case "clipBehavior" -> " Omission preserves Flutter's "
                        + "anti-alias default.";
                default -> throw new IllegalStateException(
                        "Unexpected ClipRRect property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description() + boundary));
        }
    }

    private void addPhysicalShapePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<PhysicalShapeWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(PhysicalShapeWidgetPropertySchema.Group.class);
        for (PhysicalShapeWidgetPropertySchema.Group group
                : PhysicalShapeWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            PhysicalShapeWidgetPropertySchema.Definition schema =
                    PhysicalShapeWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in PhysicalShape property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "clipper" -> " Required CustomClipper<Path>: select a built-in ShapeBorderClipper "
                        + "or an analyzer-verified project reference. Cannot be unset. Directional "
                        + "radii need explicit text direction for rounded shapes. Project clipper code "
                        + "is not executed in Canvas; custom clip, fill and shadow preview is unavailable.";
                case "clipBehavior" -> " Omission preserves Clip.none.";
                case "elevation" -> " Must be finite and non-negative. Omission preserves 0.";
                case "color" -> " Required surface color; literal ARGB and reviewed "
                        + "Material ColorScheme roles are supported. Cannot be unset.";
                case "shadowColor" -> " Literal ARGB and reviewed Material ColorScheme "
                        + "roles are supported. Omission preserves Flutter's black default.";
                default -> throw new IllegalStateException(
                        "Unexpected PhysicalShape property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + boundary));
        }
    }

    private void addPhysicalModelPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<PhysicalModelWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(PhysicalModelWidgetPropertySchema.Group.class);
        for (PhysicalModelWidgetPropertySchema.Group group
                : PhysicalModelWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            PhysicalModelWidgetPropertySchema.Definition schema =
                    PhysicalModelWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in PhysicalModel property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "shape" -> " Omission preserves BoxShape.rectangle. Circle shape "
                        + "ignores but preserves borderRadius; changing shape does not remove it.";
                case "borderRadius" -> " Physical elliptical corners only; "
                        + "BorderRadiusDirectional is not accepted. Omission preserves "
                        + "Flutter's null default. Circle shape ignores but preserves this value.";
                case "clipBehavior" -> " Omission preserves Clip.none.";
                case "elevation" -> " Must be finite and non-negative. Omission preserves 0.";
                case "color" -> " Required surface color; literal ARGB and reviewed "
                        + "Material ColorScheme roles are supported. Cannot be unset.";
                case "shadowColor" -> " Literal ARGB and reviewed Material ColorScheme "
                        + "roles are supported. Omission preserves Flutter's black default.";
                default -> throw new IllegalStateException(
                        "Unexpected PhysicalModel property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(property, Optional.empty(),
                    schema.displayName(), schema.description() + boundary));
        }
    }

    private void addClipRSuperellipsePropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ClipRSuperellipseWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ClipRSuperellipseWidgetPropertySchema.Group.class);
        for (ClipRSuperellipseWidgetPropertySchema.Group group
                : ClipRSuperellipseWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ClipRSuperellipseWidgetPropertySchema.Definition schema =
                    ClipRSuperellipseWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ClipRSuperellipse property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "borderRadius" -> " Omission preserves Flutter's "
                        + "BorderRadius.zero default. Physical and directional "
                        + "elliptical corners are supported. Flutter ignores "
                        + "borderRadius while clipper is configured.";
                case "clipper" -> " The Dart analyzer validates that the selected "
                        + "Dart symbol is assignable to CustomClipper<RSuperellipse>. "
                        + "Current-library and package-config-declared references, "
                        + "including zero-argument constructor or factory invocations, "
                        + "are supported. When configured, Flutter ignores borderRadius. "
                        + "The isolated Canvas cannot execute project Dart and displays "
                        + "an explicit preview-unavailable state.";
                case "clipBehavior" -> " Omission preserves Flutter's "
                        + "anti-alias default.";
                default -> throw new IllegalStateException(
                        "Unexpected ClipRSuperellipse property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description() + boundary));
        }
    }

    private void addClipPathPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<ClipPathWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(ClipPathWidgetPropertySchema.Group.class);
        for (ClipPathWidgetPropertySchema.Group group
                : ClipPathWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            ClipPathWidgetPropertySchema.Definition schema =
                    ClipPathWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in ClipPath property is missing its "
                                    + "presentation schema: " + property.name().value()));
            String boundary = switch (property.name().value()) {
                case "clipper" -> " The Dart analyzer validates that the selected "
                        + "symbol is assignable to CustomClipper<Path>. Setting Clipper "
                        + "first clears the mutually exclusive Shape property and selects "
                        + "the unnamed ClipPath constructor. Current-library and declared "
                        + "package references, including zero-argument invocations, are "
                        + "supported. The isolated Canvas cannot execute project Dart and "
                        + "displays an explicit preview-unavailable state.";
                case "shape" -> " The Dart analyzer validates that the selected symbol "
                        + "is assignable to ShapeBorder. Setting Shape first clears the "
                        + "mutually exclusive Clipper property and selects the non-const "
                        + "ClipPath.shape helper. Current-library and declared package "
                        + "references, including zero-argument invocations, are supported. "
                        + "The isolated Canvas cannot execute project Dart and displays an "
                        + "explicit preview-unavailable state.";
                case "clipBehavior" -> " Omission preserves Flutter's anti-alias default.";
                default -> throw new IllegalStateException(
                        "Unexpected ClipPath property: " + property.name().value());
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description() + boundary));
        }
    }

    private void addPlaceholderPropertySets(Sheet sheet, boolean hasSlotTab) {
        EnumMap<PlaceholderWidgetPropertySchema.Group, Sheet.Set> groups =
                new EnumMap<>(PlaceholderWidgetPropertySchema.Group.class);
        for (PlaceholderWidgetPropertySchema.Group group
                : PlaceholderWidgetPropertySchema.Group.values()) {
            Sheet.Set set = propertySet(
                    group.setName(), group.displayName(), group.description());
            assignTab(set, hasSlotTab ? GENERAL_TAB_NAME : null);
            groups.put(group, set);
            sheet.put(set);
        }
        for (PropertyDefinition property : definition.properties()) {
            PlaceholderWidgetPropertySchema.Definition schema =
                    PlaceholderWidgetPropertySchema.find(property.name()).orElseThrow(
                            () -> new IllegalStateException(
                                    "Built-in Placeholder property is missing its "
                                    + "presentation schema: "
                                    + property.name().value()));
            String description = switch (property.name().value()) {
                case "color" ->
                    "Color of the Placeholder outline and diagonals. Choose a literal ARGB "
                            + "color or a reviewed Material theme token; omission preserves "
                            + "Flutter's Color(0xFF455A64) default.";
                case "strokeWidth" ->
                    "Finite non-negative logical-pixel width of the outline and diagonals. "
                            + "Zero requests a hairline; omission preserves Flutter's 2.0 "
                            + "default.";
                case "fallbackWidth" ->
                    "Finite non-negative width used only when the incoming width is "
                            + "unbounded; omission preserves Flutter's 400.0 default.";
                case "fallbackHeight" ->
                    "Finite non-negative height used only when the incoming height is "
                            + "unbounded; omission preserves Flutter's 400.0 default.";
                default -> schema.description();
            };
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    description));
        }
    }

    private static java.util.List<String> elevatedButtonStringPresets(
            PropertyName propertyName) {
        String name = propertyName.value();
        if (name.endsWith("ShapeKind")) {
            return java.util.List.of(
                    "roundedRectangle", "roundedSuperellipse", "stadium",
                    "circle", "beveledRectangle", "continuousRectangle");
        }
        if (name.endsWith("MouseCursor")) {
            return systemMouseCursorPresets();
        }
        return switch (name) {
            case "styleAlignmentKind" ->
                java.util.List.of("physical", "directional");
            case "styleSplashFactory" -> java.util.List.of(
                    "inkSplash", "inkRipple", "inkSparkle", "noSplash");
            default -> java.util.List.of();
        };
    }

    private static java.util.List<String> textFieldStringPresets(
            TextFieldWidgetPropertySchema.Definition schema) {
        return switch (schema.target()) {
            case KEYBOARD_TYPE_PRESET -> java.util.List.of(
                    "text", "multiline", "number", "numberSigned",
                    "numberDecimal", "numberSignedDecimal", "phone", "datetime",
                    "emailAddress", "url", "visiblePassword", "name",
                    "streetAddress", "none", "webSearch", "twitter");
            case TEXT_ALIGN_VERTICAL_PRESET ->
                java.util.List.of("top", "center", "bottom");
            case MOUSE_CURSOR_PRESET -> systemMouseCursorPresets();
            default -> java.util.List.of();
        };
    }

    private static java.util.List<String> systemMouseCursorPresets() {
        return java.util.List.of(
                "none", "basic", "click", "forbidden", "wait", "progress",
                "contextMenu", "help", "text", "verticalText", "cell",
                "precise", "move", "grab", "grabbing", "noDrop", "alias",
                "copy", "disappearing", "allScroll", "resizeLeftRight",
                "resizeUpDown", "resizeUpLeftDownRight",
                "resizeUpRightDownLeft", "resizeUp", "resizeDown",
                "resizeLeft", "resizeRight", "resizeUpLeft", "resizeUpRight",
                "resizeDownLeft", "resizeDownRight", "resizeColumn",
                "resizeRow", "zoomIn", "zoomOut");
    }

    private Node.Property<?> projectProperty(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema) {
        String projectedDisplayName = textSchema
                .map(TextWidgetPropertySchema.Definition::displayName)
                .orElseGet(() -> displayName(property.name()));
        String projectedDescription = textSchema
                .map(TextWidgetPropertySchema.Definition::description)
                .orElseGet(() -> "Explicit model value for "
                + property.name().value() + ".");
        return projectProperty(
                property,
                textSchema,
                projectedDisplayName,
                projectedDescription);
    }

    private Node.Property<?> projectProperty(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema,
            String projectedDisplayName,
            String projectedDescription) {
        return projectProperty(
                property,
                textSchema,
                projectedDisplayName,
                projectedDescription,
                false,
                java.util.List.of());
    }

    private Node.Property<?> projectProperty(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema,
            String projectedDisplayName,
            String projectedDescription,
            boolean newlineStringList,
            java.util.List<String> stringPresets) {
        PropertyValue explicitValue = widget.properties().get(property.name());
        var binding = writableBinding(
                property, textSchema, newlineStringList, stringPresets);
        if (binding.isPresent()) {
            return writableProperty(
                    binding.orElseThrow(), explicitValue,
                    projectedDisplayName, projectedDescription);
        }
        return readOnlyProperty(
                property.name(),
                projectedDisplayName,
                projectedDescription);
    }

    private java.util.Optional<FlutterTypedPropertyEditors.Binding> writableBinding(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema) {
        return writableBinding(
                property, textSchema, false, java.util.List.of());
    }

    private java.util.Optional<FlutterTypedPropertyEditors.Binding> writableBinding(
            PropertyDefinition property,
            Optional<TextWidgetPropertySchema.Definition> textSchema,
            boolean newlineStringList,
            java.util.List<String> stringPresets) {
        if (!propertyMutationProjection
                || !BuiltInWidgetCapabilityCatalog.supports(
                        definition, WidgetCapability.PROPERTIES)) {
            return java.util.Optional.empty();
        }
        return FlutterTypedPropertyEditors.binding(
                property, textSchema, newlineStringList, stringPresets);
    }

    private static boolean inactiveFocusProperty(WidgetNode current, PropertyName property) {
        return FocusWidgetPropertySchema.FOCUS_TYPE.equals(current.type())
                && !FocusWidgetPropertySchema.propertyAvailable(current, property);
    }

    private static boolean inactiveSwitchTileProperty(WidgetNode current, PropertyName property) {
        return SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE.equals(current.type())
                && !SwitchListTileWidgetPropertySchema.propertyAvailable(current, property);
    }

    private static boolean inactiveRadioTileProperty(WidgetNode current, PropertyName property) {
        return RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.equals(current.type())
                && !RadioListTileWidgetPropertySchema.propertyAvailable(current, property);
    }

    private static boolean isNotificationCallback(WidgetNode current, PropertyName property) {
        return NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE.equals(current.type()) && property.value().equals("onNotification");
    }

    static String notificationTypeLabel(WidgetNode current) {
        PropertyValue value = current.properties().get(new PropertyName("notificationType"));
        if (value instanceof PropertyValue.StringValue preset) return preset.value();
        if (value instanceof PropertyValue.DartObjectReferenceValue reference) return reference.libraryUri().map(uri -> uri + "::").orElse("") + reference.rootSymbol();
        return "Notification";
    }

    private static boolean requiredExternalFocusNode(WidgetNode current, PropertyName property) {
        return FocusWidgetPropertySchema.FOCUS_TYPE.equals(current.type()) && property.value().equals("focusNode")
                && FocusWidgetPropertySchema.usesExternalNode(current);
    }

    private static FlutterTypedPropertyEditors.Binding focusEditorBinding(WidgetNode current, FlutterTypedPropertyEditors.Binding binding) {
        if (!requiredExternalFocusNode(current, binding.definition().name())) return binding;
        var property = binding.definition();
        var required = new PropertyDefinition(property.name(), dev.flutter.netbeans.designer.catalog.DartParameter.named(property.parameter().order(), true),
                java.util.List.of(new dev.flutter.netbeans.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues("FocusNode")), Optional.empty());
        return FlutterTypedPropertyEditors.binding(required).orElseThrow();
    }

    private PropertySupport.ReadWrite<FlutterPropertyCellValue> writableProperty(
            FlutterTypedPropertyEditors.Binding binding,
            PropertyValue explicitValue,
            String displayName,
            String schemaDescription) {
        PropertyDefinition property = binding.definition();
        PropertyName propertyName = property.name();
        Optional<WidgetEventDescriptor> eventDescriptor = WidgetEventCatalog.eventsFor(definition).stream()
                .filter(WidgetEventDescriptor::supportsHandlerActions)
                .filter(event -> event.propertyName().equals(propertyName)).findFirst();
        FlutterPropertyCellValue initial = propertyCellValue(explicitValue);
        binding.validate(initial);
        String description = propertyDescription(property, schemaDescription);
        PropertySupport.ReadWrite<FlutterPropertyCellValue> result =
                new PropertySupport.ReadWrite<>(
                propertyName.value(),
                FlutterPropertyCellValue.class,
                displayName,
                description) {
            @Override
            public String getDisplayName() {
                WidgetNode current = presentation.widget();
                if (inactiveFocusProperty(current, propertyName)) return displayName + " (inactive; retained)";
                if (inactiveSwitchTileProperty(current, propertyName)) return displayName + " (inactive; retained)";
                if (inactiveRadioTileProperty(current, propertyName)) return displayName + " (inactive; retained)";
                if (isNotificationCallback(current, propertyName)) return displayName + " (" + notificationTypeLabel(current) + ")";
                if (requiredExternalFocusNode(current, propertyName)) return displayName + " (required)";
                var dependency = current.propertyBindings().get(propertyName);
                if (dependency != null) return displayName + " (preview; " + dependency.fieldName() + ")";
                return dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.isBoundPreview(current, propertyName)
                        ? displayName + " (preview; " + current.stateBinding().orElseThrow().fieldName() + ")" : displayName;
            }

            @Override
            public String getShortDescription() {
                WidgetNode current = presentation.widget();
                if (inactiveFocusProperty(current, propertyName)) return "Inactive in Focus.withExternalFocusNode. This value is retained but not generated; the external FocusNode supplies this attribute. Select Standard to use the stored value again. " + description;
                if (inactiveSwitchTileProperty(current, propertyName)) return "Inactive in SwitchListTile Standard. This value or State binding is retained but not generated. Select Adaptive to use the stored value again. Editing it does not switch constructors. " + description;
                if (inactiveRadioTileProperty(current, propertyName)) return "Inactive in RadioListTile Standard. This value or State binding is retained but not generated. Select Adaptive to use the stored value again. Editing it does not switch constructors. " + description;
                if (isNotificationCallback(current, propertyName)) return "Runtime callback must accept " + notificationTypeLabel(current)
                        + " and return bool. True stops propagation; false, omission or null continues. A created universal Notification handler must be implemented in Source. " + description;
                if (requiredExternalFocusNode(current, propertyName)) return "Required non-null FocusNode reference for Focus.withExternalFocusNode. Return to Standard before resetting this property. Its application owner controls disposal. " + description;
                var dependency = current.propertyBindings().get(propertyName);
                if (dependency != null) return "Canvas preview only. Runtime property reads State field " + dependency.fieldName()
                        + " using " + dependency.transform() + "; editing this preview does not change the field in Source. " + description;
                return dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.isBoundPreview(current, propertyName)
                        ? "Canvas preview only. Runtime value is bound to State field "
                                + current.stateBinding().orElseThrow().fieldName()
                                + "; editing this preview does not change the field initializer in Source. " + description
                        : description;
            }

            @Override
            public FlutterPropertyCellValue getValue() {
                Presentation current = presentation;
                return propertyCellValue(
                        current.widget().properties().get(propertyName));
            }

            @Override
            public void setValue(FlutterPropertyCellValue value)
                    throws IllegalAccessException {
                if (value.radioTypeEdit().isPresent() && (!FlutterPropertyCellValue.RadioTypeEdit.supports(presentation.widget().type()) || !propertyName.value().equals("valueType")
                        || !value.radioTypeEdit().orElseThrow().widgetType().equals(presentation.widget().type()))) {
                    throw new IllegalArgumentException("Dependent type edits are accepted only on the matching widget valueType.");
                }
                FlutterPropertyCellValue accepted = focusEditorBinding(presentation.widget(), binding).validate(value);
                Presentation current = presentation;
                FlutterPropertyCellValue currentValue = propertyCellValue(
                        current.widget().properties().get(propertyName));
                if (currentValue.equals(accepted)) {
                    return;
                }
                if (eventDescriptor.isPresent() && eventsContext != null) {
                    throw new IllegalAccessException("Use the Events editor actions to change this handler.");
                }
                PropertyMutationHandler currentHandler =
                        current.mutationHandler();
                if (currentHandler == null) {
                    throw new IllegalAccessException(
                            "Flutter property mutation admission is not ready.");
                }
                DesignerCommand command = propertyMutationCommand(
                        current.widget(), propertyName, accepted);
                currentHandler.submit(command);
            }

            @Override
            public boolean canWrite() {
                return presentation.mutationHandler() != null;
            }

            @Override
            public PropertyEditor getPropertyEditor() {
                var currentBinding = focusEditorBinding(presentation.widget(), binding);
                FlutterWidgetEventsContext currentEventsContext = eventsContext;
                if (eventDescriptor.isPresent() && currentEventsContext != null) {
                    return new FlutterWidgetEventPropertyEditor(presentation.widget(),
                            eventDescriptor.orElseThrow(), currentEventsContext,
                            () -> eventsContext, () -> presentation.widget(), currentBinding.createEditor());
                }
                if (currentEventsContext != null) {
                    if (MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE.equals(presentation.widget().type())
                            && propertyName.value().equals("builder")) {
                        return new FlutterMenuAnchorBuilderPropertyEditor(presentation.widget(), currentEventsContext,
                                () -> eventsContext, () -> presentation.widget(), currentBinding.createEditor());
                    }
                    var stateProperty = dev.flutter.netbeans.designer.state.WidgetStatePropertyBindingCatalog.find(presentation.widget(), propertyName);
                    if (stateProperty.isPresent()) return new FlutterWidgetStatePropertyEditor(presentation.widget(),
                            stateProperty.orElseThrow(), currentEventsContext, () -> eventsContext,
                            () -> presentation.widget(), currentBinding.createEditor());
                }
                return currentBinding.createEditor();
            }

            @Override
            public boolean supportsDefaultValue() {
                return binding.optional() && !requiredExternalFocusNode(presentation.widget(), propertyName);
            }

            @Override
            public boolean isDefaultValue() {
                return !getValue().isExplicit();
            }

            @Override
            public void restoreDefaultValue()
                    throws IllegalAccessException, InvocationTargetException {
                Presentation current = presentation;
                if (requiredExternalFocusNode(current.widget(), propertyName)) throw new IllegalAccessException(
                        "Focus.withExternalFocusNode requires a non-null Focus node; select Standard before resetting it.");
                FlutterPropertyCellValue currentValue = propertyCellValue(
                        current.widget().properties().get(propertyName));
                if (binding.optional() && currentValue.isExplicit()) {
                    PropertyMutationHandler currentHandler =
                            current.mutationHandler();
                    if (currentHandler == null) {
                        throw new IllegalAccessException(
                                "Flutter property mutation admission is not ready.");
                    }
                    currentHandler.submit(propertyMutationCommand(
                            current.widget(),
                            propertyName,
                            FlutterPropertyCellValue.unset()));
                }
            }
        };
        // Custom editors keep a local draft. NetBeans applies the accepted
        // value only when the cell/custom dialog is committed, so a chooser,
        // spinner, or text area's intermediate events can never consume the
        // designer's one-shot mutation lease.
        result.setValue("changeImmediate", Boolean.FALSE);
        if (FlutterPropertyCellValue.RadioTypeEdit.supports(widget.type()) && propertyName.value().equals("valueType")) {
            result.setValue(FlutterRadioTypeEditorComponent.CONTEXT_ATTRIBUTE, (java.util.function.Supplier<WidgetNode>) () -> presentation.widget());
        }
        result.setValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE,
                presentation.imageAssetChoices());
        return result;
    }

    private static java.util.List<String> visibilityPrerequisites(String name) {
        return switch (name) {
            case "maintainAnimation" -> java.util.List.of("maintainState");
            case "maintainSize" -> java.util.List.of("maintainState", "maintainAnimation");
            case "maintainSemantics", "maintainInteractivity" ->
                java.util.List.of("maintainState", "maintainAnimation", "maintainSize");
            case "maintainFocusability" -> java.util.List.of("maintainState");
            default -> java.util.List.of();
        };
    }

    private DesignerCommand visibilityPropertyCommand(
            WidgetNode currentWidget,
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        java.util.ArrayList<PatchProperties.Patch> patches = new java.util.ArrayList<>();
        PropertyValue.BooleanValue enabled = new PropertyValue.BooleanValue(true);
        if (accepted.explicitValue().filter(enabled::equals).isPresent()) {
            for (String prerequisite : visibilityPrerequisites(propertyName.value())) {
                PropertyName name = new PropertyName(prerequisite);
                if (!enabled.equals(currentWidget.properties().get(name))) {
                    patches.add(new PatchProperties.SetPatch(name, enabled));
                }
            }
        } else {
            for (String dependent : java.util.List.of("maintainState", "maintainAnimation",
                    "maintainSize", "maintainSemantics", "maintainInteractivity", "maintainFocusability")) {
                PropertyName name = new PropertyName(dependent);
                if (visibilityPrerequisites(dependent).contains(propertyName.value())
                        && enabled.equals(currentWidget.properties().get(name))) {
                    patches.add(new PatchProperties.ResetPatch(name));
                }
            }
        }
        patches.add(accepted.explicitValue()
                .<PatchProperties.Patch>map(value -> new PatchProperties.SetPatch(propertyName, value))
                .orElseGet(() -> new PatchProperties.ResetPatch(propertyName)));
        return patches.size() == 1
                ? ordinaryPropertyCommand(currentWidget, propertyName, accepted)
                : new PatchProperties(currentWidget.id(), patches);
    }

    private DesignerCommand propertyMutationCommand(
            WidgetNode currentWidget,
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        if(dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.supports(currentWidget.type())) {
            var families=new java.util.ArrayList<>(dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.styleFamilies(currentWidget.type()));
            families.addAll(dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.stateFamilies(currentWidget.type()));
            var family=dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.styleFamily(currentWidget.type(),propertyName.value())
                    .or(()->dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.stateFamily(currentWidget.type(),propertyName.value()));
            var patches=new java.util.ArrayList<PatchProperties.Patch>();
            if(family.isPresent()&&accepted.explicitValue().isPresent())
                patches.add(new PatchProperties.SetPatch(new PropertyName(family.orElseThrow()),new PropertyValue.StringValue("local")));
            if(families.contains(propertyName.value())&&!(accepted.explicitValue().orElse(null) instanceof PropertyValue.StringValue))
                for(var name:currentWidget.properties().keySet())if(!name.equals(propertyName)&&name.value().startsWith(propertyName.value()))
                    patches.add(new PatchProperties.ResetPatch(name));
            if(dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.table(currentWidget.type())
                    &&accepted.explicitValue().orElse(null)!=null&&!(accepted.explicitValue().orElseThrow() instanceof PropertyValue.NullValue)) {
                var opposite=propertyName.value().equals("dataRowHeight")?java.util.List.of("dataRowMinHeight","dataRowMaxHeight")
                        :java.util.List.of("dataRowMinHeight","dataRowMaxHeight").contains(propertyName.value())?java.util.List.of("dataRowHeight"):java.util.List.<String>of();
                for(String name:opposite)if(currentWidget.properties().containsKey(new PropertyName(name)))patches.add(new PatchProperties.ResetPatch(new PropertyName(name)));
            }
            if(!patches.isEmpty()) {
                patches.add(accepted.explicitValue().<PatchProperties.Patch>map(v->new PatchProperties.SetPatch(propertyName,v)).orElseGet(()->new PatchProperties.ResetPatch(propertyName)));
                return new PatchProperties(currentWidget.id(),patches);
            }
        }
        if ((dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.TYPE.equals(currentWidget.type())
                ||dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.TYPE.equals(currentWidget.type()))
                && propertyName.value().equals("border")) {
            var patches=new java.util.ArrayList<PatchProperties.Patch>();
            String mode=accepted.explicitValue().orElse(null) instanceof PropertyValue.StringValue v ? v.value() : "";
            for(var name:currentWidget.properties().keySet()) {
                if (!dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.synthetic(name.value())) continue;
                boolean keep=!mode.isEmpty() && (name.value().equals("borderRadius")
                        || dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.activeSides(mode).stream()
                           .anyMatch(s->name.value().startsWith("border"+dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.upper(s))));
                if(!keep) patches.add(new PatchProperties.ResetPatch(name));
            }
            patches.add(accepted.explicitValue().isPresent()
                    ? new PatchProperties.SetPatch(propertyName,accepted.explicitValue().orElseThrow())
                    : new PatchProperties.ResetPatch(propertyName));
            return new PatchProperties(currentWidget.id(),patches);
        }
        if (currentWidget.stateBinding().isPresent() || !currentWidget.propertyBindings().isEmpty()) {
            if (currentWidget.stateBinding().isPresent() && (SliderWidgetPropertySchema.SLIDER_TYPE.equals(currentWidget.type())
                    || RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE.equals(currentWidget.type()))
                    && java.util.List.of("min", "max").contains(propertyName.value())
                    && !accepted.explicitValue().equals(Optional.ofNullable(currentWidget.properties().get(propertyName)))) {
                throw new IllegalArgumentException("Cannot edit " + propertyName.value() + ": Remove the State binding before changing Minimum or Maximum; "
                        + "Canvas preview values do not prove the retained runtime field is inside the new bounds.");
            }
            var prospective = new java.util.LinkedHashMap<>(currentWidget.properties());
            if (accepted.radioTypeEdit().isPresent()) {
                accepted.radioTypeEdit().orElseThrow().requested().forEach((field, value) -> {
                    if (value.isPresent()) prospective.put(new PropertyName(field), value.orElseThrow());
                    else prospective.remove(new PropertyName(field));
                });
            } else if (accepted.explicitValue().isPresent()) {
                prospective.put(propertyName, accepted.explicitValue().orElseThrow());
            } else prospective.remove(propertyName);
            var candidate = new WidgetNode(currentWidget.id(), currentWidget.type(), prospective,
                    currentWidget.slots(), currentWidget.extensions(), currentWidget.stateBinding(), currentWidget.propertyBindings());
            dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.validationError(candidate).ifPresent(reason -> {
                throw new IllegalArgumentException("Cannot edit " + propertyName.value() + ": " + reason);
            });
            dev.flutter.netbeans.designer.state.WidgetStatePropertyBindingCatalog.validationError(candidate).ifPresent(reason -> {
                throw new IllegalArgumentException("Cannot edit " + propertyName.value() + ": " + reason);
            });
        }
        if ((LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE.equals(currentWidget.type())
                || CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE.equals(currentWidget.type()))
                && accepted.explicitValue().isPresent()
                && java.util.List.of("value", "controller").contains(propertyName.value())) {
            PropertyName opposite = new PropertyName(propertyName.value().equals("value") ? "controller" : "value");
            if (currentWidget.properties().containsKey(opposite)) {
                return new PatchProperties(currentWidget.id(), java.util.List.of(
                        new PatchProperties.ResetPatch(opposite),
                        new PatchProperties.SetPatch(propertyName, accepted.explicitValue().orElseThrow())));
            }
        }
        if (CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE.equals(currentWidget.type())
                && accepted.explicitValue().isPresent()) {
            PropertyName variant = new PropertyName("variant");
            PropertyName color = new PropertyName("color");
            var adaptive = new PropertyValue.StringValue("adaptive");
            if (propertyName.equals(variant) && accepted.explicitValue().filter(adaptive::equals).isPresent()
                    && currentWidget.properties().containsKey(color)) {
                return new PatchProperties(currentWidget.id(), java.util.List.of(
                        new PatchProperties.ResetPatch(color), new PatchProperties.SetPatch(variant, adaptive)));
            }
            if (propertyName.equals(color) && adaptive.equals(currentWidget.properties().get(variant))) {
                return new PatchProperties(currentWidget.id(), java.util.List.of(
                        new PatchProperties.SetPatch(variant, new PropertyValue.StringValue("material")),
                        new PatchProperties.SetPatch(color, accepted.explicitValue().orElseThrow())));
            }
        }
        if (CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE.equals(currentWidget.type())) {
            return checkboxListTilePropertyCommand(currentWidget, propertyName, accepted);
        }
        if (SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE.equals(currentWidget.type())) {
            return switchListTilePropertyCommand(currentWidget, propertyName, accepted);
        }
        if (dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.isType(currentWidget.type())) {
            return sliverAppBarPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE.equals(currentWidget.type())) {
            return floatingHeaderPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE.equals(currentWidget.type())) {
            return expansionTilePropertyCommand(currentWidget, propertyName, accepted);
        }
        if (TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE.equals(currentWidget.type())) {
            return tooltipThemePropertyCommand(currentWidget, propertyName, accepted);
        }
        if (TooltipWidgetPropertySchema.TOOLTIP_TYPE.equals(currentWidget.type())) {
            return tooltipPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (ListTileWidgetPropertySchema.LIST_TILE_TYPE.equals(currentWidget.type())) {
            return listTilePropertyCommand(currentWidget, propertyName, accepted);
        }
        if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(currentWidget.type())) {
            return iconButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (FlutterPropertyCellValue.RadioTypeEdit.supports(currentWidget.type())) {
            return radioPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE.equals(currentWidget.type())) {
            return rangeSliderPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (SliderWidgetPropertySchema.SLIDER_TYPE.equals(currentWidget.type())) {
            return sliderPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (SwitchWidgetPropertySchema.SWITCH_TYPE.equals(currentWidget.type())) {
            return switchPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (CheckboxWidgetPropertySchema.CHECKBOX_TYPE.equals(currentWidget.type())) {
            return checkboxPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(currentWidget.type())) {
            return menuItemButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE.equals(currentWidget.type())) {
            return iconButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (MenuBarWidgetPropertySchema.MENU_BAR_TYPE.equals(currentWidget.type())) {
            return iconButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.equals(currentWidget.type())) {
            return submenuButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (isModernButton(currentWidget)) {
            return modernButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.equals(currentWidget.type())) {
            return refreshIndicatorPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE.equals(currentWidget.type())) {
            return circleAvatarPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (BadgeWidgetPropertySchema.BADGE_TYPE.equals(currentWidget.type())) {
            return badgePropertyCommand(currentWidget, propertyName, accepted);
        }
        if ((dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.supports(currentWidget.type()) || dev.flutter.netbeans.designer.catalog.SimpleDialogWidgetPropertySchema.TYPE.equals(currentWidget.type())
                || dev.flutter.netbeans.designer.catalog.MaterialBannerWidgetPropertySchema.TYPE.equals(currentWidget.type()))) {
            return alertDialogPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.TYPE.equals(currentWidget.type())
                && java.util.Set.of("width","margin","behavior").contains(propertyName.value())) {
            var patches = new java.util.ArrayList<PatchProperties.Patch>();
            var value = accepted.explicitValue().orElse(null);
            if (!propertyName.value().equals("behavior") && value != null && !(value instanceof PropertyValue.NullValue)) {
                var other = new PropertyName(propertyName.value().equals("width") ? "margin" : "width");
                if (currentWidget.properties().containsKey(other)) patches.add(new PatchProperties.ResetPatch(other));
                patches.add(new PatchProperties.SetPatch(new PropertyName("behavior"),new PropertyValue.EnumValue("SnackBarBehavior","floating")));
            } else if (propertyName.value().equals("behavior") && !new PropertyValue.EnumValue("SnackBarBehavior","floating").equals(value)) {
                for (String name : java.util.List.of("width","margin")) {
                    var other = new PropertyName(name);
                    if (currentWidget.properties().containsKey(other)) patches.add(new PatchProperties.ResetPatch(other));
                }
            }
            patches.add(accepted.explicitValue().<PatchProperties.Patch>map(v -> new PatchProperties.SetPatch(propertyName,v))
                    .orElseGet(() -> new PatchProperties.ResetPatch(propertyName)));
            return new PatchProperties(currentWidget.id(),patches);
        }
        if (CardWidgetPropertySchema.CARD_TYPE.equals(currentWidget.type())
                || dev.flutter.netbeans.designer.catalog.SnackBarWidgetPropertySchema.TYPE.equals(currentWidget.type())
                || dev.flutter.netbeans.designer.catalog.BottomSheetWidgetPropertySchema.TYPE.equals(currentWidget.type())
                || dev.flutter.netbeans.designer.catalog.DialogWidgetPropertySchema.TYPE.equals(currentWidget.type())) {
            return cardPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE.equals(currentWidget.type())) {
            return floatingActionButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (VisibilityWidgetPropertySchema.VISIBILITY_TYPE.equals(currentWidget.type())) {
            return visibilityPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(
                currentWidget.type())) {
            java.util.List<PropertyName> compound =
                    textFieldCompoundProperties(propertyName);
            if (!compound.isEmpty()) {
                return textFieldCompoundPropertyCommand(
                        currentWidget, compound, propertyName, accepted);
            }
        }
        if (ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.equals(
                currentWidget.type())) {
            return clipPathPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (DefaultTextStyleWidgetPropertySchema.sharesTextProjection(currentWidget.type()))
            return animatedTextStylePropertyCommand(currentWidget, propertyName, accepted);
        if (dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.TYPE.equals(currentWidget.type()))
            return relativePositionedTransitionPropertyCommand(currentWidget, propertyName, accepted);
        if (dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.supports(currentWidget.type())
                || dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.TYPE.equals(currentWidget.type()))
            return animatedPositionedPropertyCommand(currentWidget, propertyName, accepted);
        if (dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.TYPE.equals(currentWidget.type())) {
            return animatedContainerPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (!ContainerWidgetPropertySchema.CONTAINER_TYPE.equals(
                currentWidget.type())) {
            return ordinaryPropertyCommand(currentWidget, propertyName, accepted);
        }
        java.util.ArrayList<PatchProperties.Patch> patches =
                new java.util.ArrayList<>();
        boolean setting = accepted.explicitValue().isPresent();
        if (CONTAINER_DECORATION.equals(propertyName)) {
            if (setting && currentWidget.properties().containsKey(CONTAINER_COLOR)) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_COLOR));
            } else if (!setting && hasNonNoneContainerClip(currentWidget)) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_CLIP));
            }
        } else if (CONTAINER_COLOR.equals(propertyName) && setting) {
            if (currentWidget.properties().containsKey(CONTAINER_DECORATION)) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_DECORATION));
            }
            if (hasNonNoneContainerClip(currentWidget)) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_CLIP));
            }
        } else if (CONTAINER_CLIP.equals(propertyName)
                && setting
                && nonNoneClip(accepted.explicitValue().orElseThrow())
                && !currentWidget.properties().containsKey(CONTAINER_DECORATION)) {
            PropertyValue backgroundColor =
                    currentWidget.properties().get(CONTAINER_COLOR);
            if (backgroundColor != null) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_COLOR));
            }
            patches.add(new PatchProperties.SetPatch(
                    CONTAINER_DECORATION,
                    emptyContainerDecoration(colorSource(backgroundColor))));
        }
        patches.add(accepted.explicitValue()
                .<PatchProperties.Patch>map(value ->
                    new PatchProperties.SetPatch(propertyName, value))
                .orElseGet(() -> new PatchProperties.ResetPatch(propertyName)));
        return patches.size() == 1
                ? ordinaryPropertyCommand(currentWidget, propertyName, accepted)
                : new PatchProperties(currentWidget.id(), patches);
    }

    private DesignerCommand animatedTextStylePropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        var resets = new java.util.LinkedHashSet<PropertyName>();
        boolean setting = accepted.explicitValue().isPresent();
        if (name.value().equals("style") && (!setting || !(accepted.explicitValue().orElseThrow() instanceof PropertyValue.StringValue)))
            widget.properties().keySet().stream().filter(AnimatedDefaultTextStyleWidgetPropertySchema::styleLeaf).forEach(resets::add);
        if (setting && AnimatedDefaultTextStyleWidgetPropertySchema.styleLeaf(name)) {
            if (!(widget.properties().get(new PropertyName("style")) instanceof PropertyValue.StringValue))
                patches.add(new PatchProperties.SetPatch(new PropertyName("style"), new PropertyValue.StringValue("local")));
            for (var pair : java.util.List.of(java.util.List.of("styleColor", "styleForeground"), java.util.List.of("styleBackgroundColor", "styleBackground")))
                if (pair.contains(name.value())) resets.add(new PropertyName(pair.get(0).equals(name.value()) ? pair.get(1) : pair.get(0)));
        }
        if (setting && name.value().equals("textHeightBehavior"))
            widget.properties().keySet().stream().filter(AnimatedDefaultTextStyleWidgetPropertySchema::heightLeaf).forEach(resets::add);
        if (setting && AnimatedDefaultTextStyleWidgetPropertySchema.heightLeaf(name)) resets.add(new PropertyName("textHeightBehavior"));
        resets.stream().filter(widget.properties()::containsKey).forEach(p -> patches.add(new PatchProperties.ResetPatch(p)));
        patches.add(accepted.explicitValue().<PatchProperties.Patch>map(v -> new PatchProperties.SetPatch(name,v)).orElseGet(() -> new PatchProperties.ResetPatch(name)));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget,name,accepted) : new PatchProperties(widget.id(),patches);
    }

    private DesignerCommand relativePositionedTransitionPropertyCommand(
            WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String mode = dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.RECT_FIELDS.contains(name.value()) ? "rect"
            : dev.flutter.netbeans.designer.catalog.RelativePositionedTransitionWidgetPropertySchema.SIZE_FIELDS.contains(name.value()) ? "size" : null;
        if (mode != null && !new PropertyValue.StringValue("local").equals(widget.properties().get(new PropertyName(mode)))) {
            return new PatchProperties(widget.id(), java.util.List.of(
                new PatchProperties.SetPatch(new PropertyName(mode), new PropertyValue.StringValue("local")),
                new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow())));
        }
        return ordinaryPropertyCommand(widget, name, accepted);
    }

    private DesignerCommand animatedPositionedPropertyCommand(
            WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        var props = new java.util.HashMap<>(widget.properties());
        if (accepted.explicitValue().isPresent()) props.put(name, accepted.explicitValue().orElseThrow());
        else props.remove(name);
        if (dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.RECT_TYPE.equals(widget.type())
                || dev.flutter.netbeans.designer.catalog.PositionedTransitionWidgetPropertySchema.TYPE.equals(widget.type())) {
            if (!name.value().equals("rect") && name.value().startsWith("rect")
                    && props.get(new PropertyName("rect")) instanceof PropertyValue.DartObjectReferenceValue)
                patches.add(new PatchProperties.SetPatch(new PropertyName("rect"), new PropertyValue.StringValue("local")));
        } else {
            boolean directional = dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.directional(widget.type());
            for (var axis : java.util.List.of(java.util.List.of(directional ? "start" : "left", directional ? "end" : "right", "width"),
                    java.util.List.of("top", "bottom", "height"))) {
                if (axis.contains(name.value()) && axis.stream().allMatch(field ->
                        dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.nonNull(props.get(new PropertyName(field)))))
                    patches.add(new PatchProperties.ResetPatch(new PropertyName(name.value().equals(axis.get(2)) ? axis.get(1) : axis.get(2))));
            }
        }
        patches.add(accepted.explicitValue().<PatchProperties.Patch>map(v -> new PatchProperties.SetPatch(name,v))
                .orElseGet(() -> new PatchProperties.ResetPatch(name)));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget,name,accepted) : new PatchProperties(widget.id(),patches);
    }

    private DesignerCommand animatedContainerPropertyCommand(
            WidgetNode currentWidget, PropertyName name, FlutterPropertyCellValue accepted) {
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        var properties = new java.util.HashMap<>(currentWidget.properties());
        if (accepted.explicitValue().isPresent()) properties.put(name, accepted.explicitValue().orElseThrow());
        else properties.remove(name);
        PropertyValue value = properties.get(name);
        boolean nonNull = value != null && !(value instanceof PropertyValue.NullValue);
        if (nonNull && (name.equals(CONTAINER_COLOR) || name.equals(CONTAINER_DECORATION))) {
            PropertyName other = name.equals(CONTAINER_COLOR) ? CONTAINER_DECORATION : CONTAINER_COLOR;
            PropertyValue previous = properties.get(other);
            if (previous != null && !(previous instanceof PropertyValue.NullValue)) {
                patches.add(new PatchProperties.ResetPatch(other)); properties.remove(other);
            }
        }
        boolean background = java.util.stream.Stream.of(CONTAINER_COLOR, CONTAINER_DECORATION)
                .map(properties::get).anyMatch(v -> v != null && !(v instanceof PropertyValue.NullValue));
        if (!background && properties.get(CONTAINER_CLIP) instanceof PropertyValue.EnumValue clip && !clip.value().equals("none")) {
            if (name.equals(CONTAINER_CLIP)) {
                patches.add(new PatchProperties.SetPatch(CONTAINER_DECORATION, emptyContainerDecoration(Optional.empty())));
            } else {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_CLIP));
            }
        }
        patches.add(accepted.explicitValue().<PatchProperties.Patch>map(v -> new PatchProperties.SetPatch(name, v))
                .orElseGet(() -> new PatchProperties.ResetPatch(name)));
        return patches.size() == 1 ? ordinaryPropertyCommand(currentWidget, name, accepted) : new PatchProperties(currentWidget.id(), patches);
    }

    private DesignerCommand circleAvatarPropertyCommand(
            WidgetNode currentWidget, PropertyName propertyName, FlutterPropertyCellValue accepted) {
        String name = propertyName.value();
        boolean setting = accepted.explicitValue().isPresent();
        String requiredProvider = switch (name) {
            case "onBackgroundImageError" -> "backgroundImage";
            case "onForegroundImageError" -> "foregroundImage";
            default -> null;
        };
        if (setting && requiredProvider != null
                && !currentWidget.properties().containsKey(new PropertyName(requiredProvider))) {
            throw new IllegalArgumentException("Cannot set " + name + " on CircleAvatar '" + currentWidget.id()
                    + "': set " + requiredProvider + " first; an image-error callback requires its matching provider.");
        }
        java.util.List<String> resetNames = setting ? switch (name) {
            case "radius" -> java.util.List.of("minRadius", "maxRadius");
            case "minRadius", "maxRadius" -> java.util.List.of("radius");
            default -> java.util.List.of();
        } : switch (name) {
            case "backgroundImage" -> java.util.List.of("onBackgroundImageError");
            case "foregroundImage" -> java.util.List.of("onForegroundImageError");
            default -> java.util.List.of();
        };
        java.util.ArrayList<PatchProperties.Patch> patches = new java.util.ArrayList<>();
        resetNames.stream().map(PropertyName::new).filter(currentWidget.properties()::containsKey)
                .forEach(reset -> patches.add(new PatchProperties.ResetPatch(reset)));
        patches.add(accepted.explicitValue().<PatchProperties.Patch>map(value ->
                new PatchProperties.SetPatch(propertyName, value))
                .orElseGet(() -> new PatchProperties.ResetPatch(propertyName)));
        return patches.size() == 1 ? ordinaryPropertyCommand(currentWidget, propertyName, accepted)
                : new PatchProperties(currentWidget.id(), patches);
    }

    private DesignerCommand floatingActionButtonPropertyCommand(
            WidgetNode currentWidget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value();
        if (edited.equals("shape") || edited.equals("shapeKind") || CardWidgetPropertySchema.isShapeDetailProperty(edited))
            return cardPropertyCommand(currentWidget, name, accepted);
        if (accepted.explicitValue().isEmpty()) return ordinaryPropertyCommand(currentWidget, name, accepted);
        var value = accepted.explicitValue().orElseThrow();
        String target = FloatingActionButtonWidgetPropertySchema.variant(currentWidget);
        if (edited.equals("variant")) target = ((PropertyValue.StringValue) value).value();
        else if (edited.equals("mini") || edited.equals("isExtended")
                && !FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(edited, target)) target = "standard";
        else if (FloatingActionButtonWidgetPropertySchema.extendedOnlyProperties().contains(edited)) target = "extended";
        if (target.equals("extended") && (!(currentWidget.slots().get(CHILD_SLOT) instanceof WidgetSlot.SingleSlot child) || child.child().isEmpty()))
            throw new IllegalArgumentException("Cannot select Extended on FloatingActionButton '" + currentWidget.id()
                    + "': Child is empty. Add a Child first; Extended requires a Label.");
        if (!target.equals("extended") && currentWidget.slots().get(new SlotName("icon")) instanceof WidgetSlot.SingleSlot icon && icon.child().isPresent())
            throw new IllegalArgumentException("Cannot select " + target + " on FloatingActionButton '" + currentWidget.id()
                    + "': Icon contains widget '" + icon.child().orElseThrow().id() + "'. Move or clear Icon first; no widget will be deleted.");
        var resets = new java.util.LinkedHashSet<PropertyName>();
        for (var key : currentWidget.properties().keySet())
            if (!FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(key.value(), target)) resets.add(key);
        String opposite = switch (edited) {
            case "extendedTextStyleForeground" -> "extendedTextStyleColor";
            case "extendedTextStyleColor" -> "extendedTextStyleForeground";
            case "extendedTextStyleBackground" -> "extendedTextStyleBackgroundColor";
            case "extendedTextStyleBackgroundColor" -> "extendedTextStyleBackground";
            default -> null;
        };
        if (opposite != null) resets.add(new PropertyName(opposite));
        resets.remove(name); var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(currentWidget.properties()::containsKey).forEach(key -> patches.add(new PatchProperties.ResetPatch(key)));
        if (!edited.equals("variant") && !target.equals(FloatingActionButtonWidgetPropertySchema.variant(currentWidget)))
            patches.add(new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue(target)));
        patches.add(new PatchProperties.SetPatch(name, value));
        return patches.size() == 1 ? ordinaryPropertyCommand(currentWidget, name, accepted) : new PatchProperties(currentWidget.id(), patches);
    }

    private DesignerCommand badgePropertyCommand(
            WidgetNode currentWidget, PropertyName propertyName, FlutterPropertyCellValue accepted) {
        String edited = propertyName.value();
        boolean setting = accepted.explicitValue().isPresent();
        if (setting && edited.equals("count")) {
            WidgetSlot label = currentWidget.slots().get(new SlotName("label"));
            if (label instanceof WidgetSlot.SingleSlot single && single.child().isPresent()) {
                throw new IllegalArgumentException("Cannot set Count on Badge '" + currentWidget.id()
                        + "': Label is occupied. Clear or move Label first; it will not be deleted automatically.");
            }
        }
        if (setting && edited.equals("maxCount") && !BadgeWidgetPropertySchema.isCountMode(currentWidget)) {
            throw new IllegalArgumentException("Cannot set Max count on Badge '" + currentWidget.id()
                    + "': set Count first.");
        }
        String reset = !setting && edited.equals("count") ? "maxCount" : setting ? switch (edited) {
            case "textStyleForeground" -> "textStyleColor";
            case "textStyleColor" -> "textStyleForeground";
            case "textStyleBackground" -> "textStyleBackgroundColor";
            case "textStyleBackgroundColor" -> "textStyleBackground";
            default -> null;
        } : null;
        if (reset == null || !currentWidget.properties().containsKey(new PropertyName(reset))) {
            return ordinaryPropertyCommand(currentWidget, propertyName, accepted);
        }
        return new PatchProperties(currentWidget.id(), java.util.List.of(
                new PatchProperties.ResetPatch(new PropertyName(reset)),
                accepted.explicitValue().<PatchProperties.Patch>map(value ->
                        new PatchProperties.SetPatch(propertyName, value))
                        .orElseGet(() -> new PatchProperties.ResetPatch(propertyName))));
    }

    private DesignerCommand rangeSliderPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var explicit = accepted.explicitValue().orElse(null);
        if (RangeSliderWidgetPropertySchema.rangeProperties().contains(edited)) {
            var prospective = new java.util.LinkedHashMap<>(widget.properties());
            if (setting) prospective.put(name, explicit); else prospective.remove(name);
            var candidate = new WidgetNode(widget.id(), widget.type(), prospective, widget.slots(), widget.extensions(), widget.stateBinding(), widget.propertyBindings());
            RangeSliderWidgetPropertySchema.rangeError(candidate).ifPresent(reason -> {
                throw new IllegalArgumentException("Cannot edit " + edited + " on RangeSlider '" + widget.id() + "': " + reason);
            });
        }
        var resets = new java.util.LinkedHashSet<PropertyName>();
        var families = new java.util.LinkedHashMap<String, java.util.List<String>>();
        families.put("labels", java.util.List.of("labelsStart", "labelsEnd"));
        families.put("overlayColor", RangeSliderWidgetPropertySchema.overlayColorStateProperties());
        families.put("mouseCursor", RangeSliderWidgetPropertySchema.mouseCursorStateProperties());
        families.forEach((family, leaves) -> {
            if (setting && edited.equals(family)) leaves.forEach(field -> resets.add(new PropertyName(field)));
            else if (setting && leaves.contains(edited)) resets.add(new PropertyName(family));
        });
        resets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, explicit) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand sliderPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var explicit = accepted.explicitValue().orElse(null);
        if (SliderWidgetPropertySchema.rangeProperties().contains(edited)) {
            var prospective = new java.util.LinkedHashMap<>(widget.properties());
            if (setting) prospective.put(name, explicit); else prospective.remove(name);
            var candidate = new WidgetNode(widget.id(), widget.type(), prospective, widget.slots(), widget.extensions(), widget.stateBinding(), widget.propertyBindings());
            SliderWidgetPropertySchema.rangeError(candidate).ifPresent(reason -> {
                throw new IllegalArgumentException("Cannot edit " + edited + " on Slider '" + widget.id() + "': " + reason);
            });
        }
        var resets = new java.util.LinkedHashSet<PropertyName>(); var sets = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        if (setting && edited.equals("padding")) sets.put(new PropertyName("variant"), new PropertyValue.StringValue("standard"));
        if (edited.equals("variant") && explicit instanceof PropertyValue.StringValue variant && variant.value().equals("adaptive")) resets.add(new PropertyName("padding"));
        if (setting && edited.equals("overlayColor")) SliderWidgetPropertySchema.overlayColorStateProperties().forEach(field -> resets.add(new PropertyName(field)));
        else if (setting && SliderWidgetPropertySchema.overlayColorStateProperties().contains(edited)) resets.add(new PropertyName("overlayColor"));
        resets.remove(name); sets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        sets.forEach((field, value) -> { if (!value.equals(widget.properties().get(field))) patches.add(new PatchProperties.SetPatch(field, value)); });
        patches.add(setting ? new PatchProperties.SetPatch(name, explicit) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand switchPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        return switchPropertyCommand(widget, name, accepted, false);
    }

    private DesignerCommand switchListTilePropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value();
        if (edited.equals("shape") || SwitchListTileWidgetPropertySchema.builtInShapePropertyNames().contains(edited))
            return cardPropertyCommand(widget, name, accepted);
        if (edited.equals("isThreeLine") && accepted.explicitValue().filter(new PropertyValue.BooleanValue(true)::equals).isPresent()
                && (!(widget.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty()))
            throw new IllegalArgumentException("Cannot enable SwitchListTile Three line: add a widget to Subtitle first. No subtitle is created automatically.");
        if (edited.startsWith("visualDensity") || edited.equals("mouseCursor") || SwitchListTileWidgetPropertySchema.mouseCursorStateProperties().contains(edited))
            return checkboxListTileDensityCursorCommand(widget, name, accepted, "SwitchListTile");
        return switchPropertyCommand(widget, name, accepted, true);
    }

    private DesignerCommand switchPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted, boolean tile) {
        String edited = name.value();
        boolean setting = accepted.explicitValue().isPresent();
        var explicit = accepted.explicitValue().orElse(null);
        var resets = new java.util.LinkedHashSet<PropertyName>();
        var sets = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        if (!tile && setting && edited.equals("applyCupertinoTheme")) sets.put(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"));
        if (!tile && edited.equals("variant") && explicit instanceof PropertyValue.StringValue variant && variant.value().equals("standard"))
            resets.add(new PropertyName("applyCupertinoTheme"));
        for (String prefix : java.util.List.of("Active", "Inactive")) {
            String image = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1) + "ThumbImage";
            String callback = "on" + prefix + "ThumbImageError";
            if (setting && edited.equals(callback) && !(tile && explicit instanceof PropertyValue.NullValue) && !widget.properties().containsKey(new PropertyName(image)))
                throw new IllegalArgumentException("Cannot set " + callback + " on " + (tile ? "SwitchListTile" : "Switch") + " '" + widget.id()
                        + "': set " + image + " first; an image-error callback requires its matching provider.");
            if (!setting && edited.equals(image)) resets.add(new PropertyName(callback));
        }
        var families = new java.util.LinkedHashMap<String, java.util.List<String>>();
        for (String family : SwitchWidgetPropertySchema.colorFamilies()) families.put(family, SwitchWidgetPropertySchema.colorStateProperties(family));
        if (!tile) families.put("trackOutlineWidth", SwitchWidgetPropertySchema.outlineWidthStateProperties());
        families.put("thumbIcon", SwitchWidgetPropertySchema.thumbIconLocalProperties());
        families.forEach((family, leaves) -> {
            if (setting && edited.equals(family)) leaves.forEach(field -> resets.add(new PropertyName(field)));
            else if (setting && leaves.contains(edited)) resets.add(new PropertyName(family));
        });
        for (String state : SwitchWidgetPropertySchema.thumbIconStates()) {
            var bucket = SwitchWidgetPropertySchema.thumbIconBucketProperties(state);
            String mode = bucket.getFirst();
            if (edited.equals(mode) && (!setting || explicit.equals(new PropertyValue.StringValue("inherit"))))
                bucket.stream().skip(1).forEach(field -> resets.add(new PropertyName(field)));
            else if (setting && bucket.contains(edited) && !edited.equals(mode))
                sets.put(new PropertyName(mode), new PropertyValue.StringValue("icon"));
        }
        resets.remove(name); sets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        sets.forEach((field, value) -> { if (!value.equals(widget.properties().get(field))) patches.add(new PatchProperties.SetPatch(field, value)); });
        patches.add(setting ? new PatchProperties.SetPatch(name, explicit) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private static Optional<String> radioTypeError(WidgetNode widget) {
        if (dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type()))
            return dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.valueTypeError(widget);
        if (dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type()))
            return dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.valueTypeError(widget);
        return RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(widget.type())
                ? RadioGroupWidgetPropertySchema.valueTypeError(widget) : RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.equals(widget.type())
                        ? RadioListTileWidgetPropertySchema.valueTypeError(widget) : RadioWidgetPropertySchema.valueTypeError(widget);
    }

    private DesignerCommand radioPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        boolean tile = RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.equals(widget.type());
        String radioFamily = dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type()) ? "TweenAnimationBuilder"
                : dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type()) ? "ValueListenableBuilder"
                : RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(widget.type()) ? "RadioGroup" : tile ? "RadioListTile" : "Radio";
        String edited = name.value();
        boolean setting = accepted.explicitValue().isPresent();
        var resets = new java.util.LinkedHashSet<PropertyName>();
        var sets = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        var explicit = accepted.explicitValue().orElse(null);
        if (accepted.radioTypeEdit().isPresent()) {
            var draft = accepted.radioTypeEdit().orElseThrow();
            if (!edited.equals("valueType") || !widget.type().equals(draft.widgetType()) || !widget.id().equals(draft.widgetId())
                    || !FlutterPropertyCellValue.RadioTypeEdit.snapshot(widget).equals(draft.baseline())) {
                throw new IllegalArgumentException("Cannot apply " + radioFamily + " type edit: the selected widget or dependent values changed after the dialog opened. Reopen the editor.");
            }
            var requested = new java.util.LinkedHashMap<>(widget.properties());
            var patches = new java.util.ArrayList<PatchProperties.Patch>();
            draft.requested().forEach((field, value) -> {
                var key = new PropertyName(field);
                FlutterTypedPropertyEditors.binding(definition.properties().stream().filter(item -> item.name().equals(key)).findFirst().orElseThrow(), Optional.empty(), false,
                        tile && field.equals("onChanged") ? java.util.List.of("noop")
                                : dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type()) && field.equals("tween") ? java.util.List.of("default")
                                : dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type()) && field.equals("builder") ? java.util.List.of("child")
                                : dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type()) && field.equals("valueListenable") ? java.util.List.of("constant")
                                : dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type()) && field.equals("builder") ? java.util.List.of("child") : java.util.List.of())
                        .orElseThrow().validate(new FlutterPropertyCellValue(value));
                if (!value.equals(draft.baseline().get(field))) {
                    if (value.isPresent()) { requested.put(key, value.orElseThrow()); patches.add(new PatchProperties.SetPatch(key, value.orElseThrow())); }
                    else { requested.remove(key); patches.add(new PatchProperties.ResetPatch(key)); }
                }
            });
            radioTypeError(new WidgetNode(widget.id(), widget.type(), requested, widget.slots(), widget.extensions(), widget.stateBinding(), widget.propertyBindings())).ifPresent(reason -> {
                throw new IllegalArgumentException("Cannot edit " + radioFamily + " type: " + reason);
            });
            if (patches.isEmpty()) return ordinaryPropertyCommand(widget, name, FlutterPropertyCellValue.explicit(explicit));
            return new PatchProperties(widget.id(), patches);
        }
        if (FlutterPropertyCellValue.RadioTypeEdit.fields(widget.type()).contains(edited)) {
            var prospective = new java.util.LinkedHashMap<>(widget.properties());
            if (setting) prospective.put(name, explicit); else prospective.remove(name);
            radioTypeError(new WidgetNode(widget.id(), widget.type(), prospective, widget.slots(), widget.extensions(), widget.stateBinding(), widget.propertyBindings())).ifPresent(reason -> {
                throw new IllegalArgumentException("Cannot edit " + radioFamily + " " + edited + ": " + reason + " Use the Value type dialog to change dependent values together.");
            });
        }
        if (RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.equals(widget.type())
                || dev.flutter.netbeans.designer.catalog.TweenAnimationBuilderWidgetPropertySchema.supports(widget.type())
                || dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.supports(widget.type())) return ordinaryPropertyCommand(widget, name, accepted);
        if (tile && (edited.equals("shape") || RadioListTileWidgetPropertySchema.builtInShapePropertyNames().contains(edited))) return cardPropertyCommand(widget, name, accepted);
        if (tile && edited.equals("isThreeLine") && new PropertyValue.BooleanValue(true).equals(explicit)
                && (!(widget.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty()))
            throw new IllegalArgumentException("Cannot enable RadioListTile Three line: add a widget to Subtitle first. No subtitle is created automatically.");
        if (tile && (edited.startsWith("visualDensity") || edited.equals("mouseCursor") || RadioListTileWidgetPropertySchema.mouseCursorStateProperties().contains(edited)))
            return checkboxListTileDensityCursorCommand(widget, name, accepted, "RadioListTile");
        if (!tile && setting && edited.equals("useCupertinoCheckmarkStyle")) sets.put(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"));
        if (!tile && edited.equals("variant") && new PropertyValue.StringValue("standard").equals(explicit)) resets.add(new PropertyName("useCupertinoCheckmarkStyle"));
        var density = java.util.List.of("visualDensityHorizontal", "visualDensityVertical");
        if (setting && edited.equals("visualDensity")) density.forEach(field -> resets.add(new PropertyName(field)));
        else if (setting && density.contains(edited)) resets.add(new PropertyName("visualDensity"));
        String radius = tile ? "radioInnerRadius" : "innerRadius";
        var radiusStates = tile ? RadioListTileWidgetPropertySchema.innerRadiusStateProperties() : RadioWidgetPropertySchema.innerRadiusStateProperties();
        if (setting && edited.equals(radius)) radiusStates.forEach(field -> resets.add(new PropertyName(field)));
        else if (setting && radiusStates.contains(edited)) resets.add(new PropertyName(radius));
        for (String family : tile ? RadioListTileWidgetPropertySchema.colorFamilies() : java.util.List.of("fillColor", "overlayColor", "backgroundColor")) {
            var local = tile ? RadioListTileWidgetPropertySchema.colorStateProperties(family) : RadioWidgetPropertySchema.colorStateProperties(family);
            if (setting && edited.equals(family)) local.forEach(field -> resets.add(new PropertyName(field)));
            else if (setting && local.contains(edited)) resets.add(new PropertyName(family));
        }
        String side = tile ? "radioSide" : "side";
        var sideLocals = tile ? RadioListTileWidgetPropertySchema.sideLocalProperties() : RadioWidgetPropertySchema.sideLocalProperties();
        if (setting && edited.equals(side)) {
            sideLocals.forEach(field -> resets.add(new PropertyName(field)));
        } else if (sideLocals.contains(edited)) {
            if (setting) resets.add(new PropertyName(side));
            if (edited.equals(side + "Stateful") && (!setting || explicit.equals(new PropertyValue.BooleanValue(false)))) {
                (tile ? RadioListTileWidgetPropertySchema.sideStateProperties() : RadioWidgetPropertySchema.sideStateProperties()).forEach(field -> resets.add(new PropertyName(field)));
            }
            for (String state : RadioWidgetPropertySchema.sideStates()) {
                var bucket = tile ? RadioListTileWidgetPropertySchema.sideBucketProperties(state) : RadioWidgetPropertySchema.sideBucketProperties(state);
                if (!setting || !bucket.contains(edited)) continue;
                sets.put(new PropertyName(side + "Stateful"), new PropertyValue.BooleanValue(true));
                String mode = bucket.getFirst();
                if (edited.equals(mode) && explicit.equals(new PropertyValue.StringValue("inherit"))) {
                    bucket.stream().skip(1).forEach(field -> resets.add(new PropertyName(field)));
                } else if (!edited.equals(mode)) {
                    sets.put(new PropertyName(mode), new PropertyValue.StringValue("border"));
                }
            }
        }
        resets.remove(name); sets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        sets.forEach((field, value) -> { if (!value.equals(widget.properties().get(field))) patches.add(new PatchProperties.SetPatch(field, value)); });
        patches.add(setting ? new PatchProperties.SetPatch(name, explicit) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand checkboxPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value();
        if (edited.equals("shape") || CheckboxWidgetPropertySchema.builtInShapePropertyNames().contains(edited)) {
            return cardPropertyCommand(widget, name, accepted);
        }
        boolean setting = accepted.explicitValue().isPresent();
        var resets = new java.util.LinkedHashSet<PropertyName>();
        var sets = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        var explicit = accepted.explicitValue().orElse(null);
        if (edited.equals("value") && explicit instanceof PropertyValue.NullValue) {
            sets.put(new PropertyName("tristate"), new PropertyValue.BooleanValue(true));
        }
        if (edited.equals("tristate") && (!setting || explicit.equals(new PropertyValue.BooleanValue(false)))
                && widget.properties().get(new PropertyName("value")) instanceof PropertyValue.NullValue) {
            sets.put(new PropertyName("value"), new PropertyValue.BooleanValue(false));
        }
        for (String family : java.util.List.of("fillColor", "overlayColor")) {
            var local = CheckboxWidgetPropertySchema.colorStateProperties(family);
            if (setting && edited.equals(family)) local.forEach(field -> resets.add(new PropertyName(field)));
            else if (setting && local.contains(edited)) resets.add(new PropertyName(family));
        }
        if (setting && edited.equals("side")) {
            CheckboxWidgetPropertySchema.sideLocalProperties().forEach(field -> resets.add(new PropertyName(field)));
        } else if (CheckboxWidgetPropertySchema.sideLocalProperties().contains(edited)) {
            if (setting) resets.add(new PropertyName("side"));
            if (edited.equals("sideStateful") && (!setting || explicit.equals(new PropertyValue.BooleanValue(false)))) {
                CheckboxWidgetPropertySchema.sideStateProperties().forEach(field -> resets.add(new PropertyName(field)));
            }
            for (String state : CheckboxWidgetPropertySchema.sideStates()) {
                var bucket = CheckboxWidgetPropertySchema.sideBucketProperties(state);
                if (!setting || !bucket.contains(edited)) continue;
                sets.put(new PropertyName("sideStateful"), new PropertyValue.BooleanValue(true));
                String mode = bucket.getFirst();
                if (edited.equals(mode) && explicit.equals(new PropertyValue.StringValue("inherit"))) {
                    bucket.stream().skip(1).forEach(field -> resets.add(new PropertyName(field)));
                } else if (!edited.equals(mode)) {
                    sets.put(new PropertyName(mode), new PropertyValue.StringValue("border"));
                }
            }
        }
        resets.remove(name); sets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        sets.forEach((field, value) -> { if (!value.equals(widget.properties().get(field))) patches.add(new PatchProperties.SetPatch(field, value)); });
        patches.add(setting ? new PatchProperties.SetPatch(name, explicit) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand checkboxListTilePropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var shapeFamily = CheckboxListTileWidgetPropertySchema.shapeFamilies().contains(edited) ? Optional.of(edited) : CheckboxListTileWidgetPropertySchema.shapeFamily(name);
        if (shapeFamily.isPresent()) return checkboxListTileShapeCommand(widget, name, accepted, shapeFamily.orElseThrow());
        if (edited.equals("isThreeLine") && accepted.explicitValue().filter(new PropertyValue.BooleanValue(true)::equals).isPresent()
                && (!(widget.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty()))
            throw new IllegalArgumentException("Cannot enable CheckboxListTile Three line: add a widget to Subtitle first. No subtitle is created automatically.");
        if (!edited.equals("visualDensity") && !edited.startsWith("visualDensity")
                && !edited.equals("mouseCursor") && !CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties().contains(edited))
            return checkboxPropertyCommand(widget, name, accepted);
        return checkboxListTileDensityCursorCommand(widget, name, accepted, "CheckboxListTile");
    }

    private DesignerCommand checkboxListTileDensityCursorCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted, String widgetName) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var resets = new java.util.LinkedHashSet<PropertyName>();
        if (setting && edited.equals("visualDensity")) {
            resets.add(new PropertyName("visualDensityHorizontal")); resets.add(new PropertyName("visualDensityVertical"));
        } else if (setting && (edited.equals("visualDensityHorizontal") || edited.equals("visualDensityVertical")))
            resets.add(new PropertyName("visualDensity"));
        var local = CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties();
        if (setting && edited.equals("mouseCursor")) local.forEach(field -> resets.add(new PropertyName(field)));
        if (local.contains(edited)) {
            if (setting) {
                if (!edited.equals("mouseCursorDefault") && !widget.properties().containsKey(new PropertyName("mouseCursorDefault")))
                    throw new IllegalArgumentException("Cannot set " + widgetName + " " + edited + ": set mouseCursorDefault first. No cursor is invented.");
                resets.add(new PropertyName("mouseCursor"));
            } else if (edited.equals("mouseCursorDefault") && local.stream().filter(field -> !field.equals(edited)).anyMatch(field -> widget.properties().containsKey(new PropertyName(field))))
                throw new IllegalArgumentException("Cannot reset " + widgetName + " mouseCursorDefault: reset its remaining local state entries first, or set the whole mouseCursor value.");
        }
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand checkboxListTileShapeCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted, String family) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var local = CheckboxListTileWidgetPropertySchema.shapeLocalProperties(family); String kindField = family + "Kind";
        var resets = new java.util.LinkedHashSet<PropertyName>(); var patches = new java.util.ArrayList<PatchProperties.Patch>();
        if (edited.equals(family) && setting) local.forEach(field -> resets.add(new PropertyName(field)));
        else if (local.contains(edited)) {
            if (setting) {
                resets.add(new PropertyName(family));
                String kind = edited.equals(kindField) ? ((PropertyValue.StringValue) accepted.explicitValue().orElseThrow()).value()
                        : widget.properties().get(new PropertyName(kindField)) instanceof PropertyValue.StringValue current
                                && CheckboxListTileWidgetPropertySchema.shapePropertyAppliesToKind(edited, current.value())
                                        ? current.value() : CheckboxListTileWidgetPropertySchema.preferredShapeKindForProperty(edited);
                local.stream().filter(field -> !field.equals(kindField) && !CheckboxListTileWidgetPropertySchema.shapePropertyAppliesToKind(field, kind))
                        .forEach(field -> resets.add(new PropertyName(field)));
                if (!edited.equals(kindField) && !new PropertyValue.StringValue(kind).equals(widget.properties().get(new PropertyName(kindField))))
                    patches.add(new PatchProperties.SetPatch(new PropertyName(kindField), new PropertyValue.StringValue(kind)));
            } else if (edited.equals(kindField)) local.forEach(field -> resets.add(new PropertyName(field)));
        }
        resets.remove(name); resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand tooltipThemePropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        if (accepted.explicitValue().isPresent()) {
            boolean whole = name.value().equals("data");
            boolean localValues = widget.properties().keySet().stream().anyMatch(field -> !field.value().equals("data"));
            if (whole && (localValues || !widget.propertyBindings().isEmpty())) {
                throw new IllegalArgumentException("Cannot set TooltipTheme Data while local values or State bindings remain. Reset each local value, including explicit null, and remove local State bindings first; no values are silently cleared.");
            }
            if (!whole && widget.properties().containsKey(new PropertyName("data"))) {
                throw new IllegalArgumentException("Cannot set TooltipTheme local " + name.value() + " while whole Data is configured. Reset Data first; the stored reference is not silently cleared.");
            }
        }
        // Local TooltipThemeData leaves use the same reviewed inner compounds.
        // Whole data has no matching inner-family name and remains an ordinary edit.
        return tooltipPropertyCommand(widget, name, accepted);
    }

    private DesignerCommand tooltipPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        boolean nonNull = setting && !(accepted.explicitValue().orElseThrow() instanceof PropertyValue.NullValue);
        var resets = new java.util.LinkedHashSet<PropertyName>();
        if (edited.equals("message") || edited.equals("richMessage")) {
            String peer = edited.equals("message") ? "richMessage" : "message";
            if (!nonNull && !TooltipWidgetPropertySchema.isNonNull(widget, peer)) throw new IllegalArgumentException(
                    "Cannot clear Tooltip " + edited + ": exactly one non-null Message or Rich message is required. Set the other content first; switching is atomic and no fallback text is invented.");
            if (nonNull && TooltipWidgetPropertySchema.isNonNull(widget, peer)) resets.add(new PropertyName(peer));
        }
        if (nonNull && (edited.equals("height") || edited.equals("constraints"))) {
            String peer = edited.equals("height") ? "constraints" : "height";
            if (TooltipWidgetPropertySchema.isNonNull(widget, peer)) resets.add(new PropertyName(peer));
        }
        if (setting && edited.equals("textStyle")) TooltipWidgetPropertySchema.textStyleProperties().forEach(field -> resets.add(new PropertyName(field)));
        else if (setting && TooltipWidgetPropertySchema.isTextStyleProperty(name)) resets.add(new PropertyName("textStyle"));
        String opposite = setting ? switch (edited) {
            case "textStyleForeground" -> "textStyleColor"; case "textStyleColor" -> "textStyleForeground";
            case "textStyleBackground" -> "textStyleBackgroundColor"; case "textStyleBackgroundColor" -> "textStyleBackground";
            default -> null;
        } : null;
        if (opposite != null) resets.add(new PropertyName(opposite)); resets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand sliverAppBarPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var resets = new java.util.LinkedHashSet<PropertyName>();
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        for (String whole : dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.WHOLE_TYPES.keySet()) {
            var local = dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.localFamily(whole);
            if (setting && edited.equals(whole)) local.forEach(n -> resets.add(new PropertyName(n)));
            else if (setting && local.contains(edited)) resets.add(new PropertyName(whole));
        }
        if (setting && edited.startsWith("shape") && !edited.equals("shape")) {
            String kind = edited.equals("shapeKind") ? ((PropertyValue.StringValue) accepted.explicitValue().orElseThrow()).value()
                    : edited.equals("shapeCircleEccentricity") ? "circle"
                    : widget.properties().get(new PropertyName("shapeKind")) instanceof PropertyValue.StringValue current ? current.value() : "roundedRectangle";
            if (edited.startsWith("shapeRadius") && !java.util.Set.of("roundedRectangle", "beveledRectangle", "continuousRectangle").contains(kind)) kind = "roundedRectangle";
            if (!edited.equals("shapeKind")) patches.add(new PatchProperties.SetPatch(new PropertyName("shapeKind"), new PropertyValue.StringValue(kind)));
            for (String field : dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.localFamily("shape")) {
                if (field.startsWith("shapeRadius") && !java.util.Set.of("roundedRectangle", "beveledRectangle", "continuousRectangle").contains(kind)
                        || field.equals("shapeCircleEccentricity") && !kind.equals("circle")) resets.add(new PropertyName(field));
            }
        } else if (!setting && edited.equals("shapeKind")) {
            dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.localFamily("shape").forEach(n -> resets.add(new PropertyName(n)));
        }
        if (edited.equals("snap") && accepted.explicitValue().orElse(null) instanceof PropertyValue.BooleanValue b && b.value()) {
            patches.add(new PatchProperties.SetPatch(new PropertyName("floating"), new PropertyValue.BooleanValue(true)));
        }
        if (edited.equals("floating") && !(accepted.explicitValue().orElse(null) instanceof PropertyValue.BooleanValue b && b.value())) {
            if (widget.properties().get(new PropertyName("snap")) instanceof PropertyValue.BooleanValue b && b.value())
                patches.add(new PatchProperties.SetPatch(new PropertyName("snap"), new PropertyValue.BooleanValue(false)));
        }
        resets.remove(name);
        resets.stream().filter(widget.properties()::containsKey).forEach(n -> patches.add(new PatchProperties.ResetPatch(n)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand floatingHeaderPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var local = dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE;
        String whole = "animationStyle";
        var resets = new java.util.LinkedHashSet<PropertyName>();
        if (setting && edited.equals(whole)) local.forEach(field -> resets.add(new PropertyName(field)));
        else if (setting && local.contains(edited)) resets.add(new PropertyName(whole));
        resets.remove(name); var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand expansionTilePropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var family = ExpansionTileWidgetPropertySchema.shapeFamilies().contains(edited) ? Optional.of(edited) : ExpansionTileWidgetPropertySchema.shapeFamily(name);
        if (family.isPresent()) return expansionTileShapeCommand(widget, name, accepted, family.orElseThrow());
        var local = edited.startsWith("visualDensity") ? java.util.List.of("visualDensityHorizontal", "visualDensityVertical")
                : ExpansionTileWidgetPropertySchema.animationStyleLocalProperties();
        String whole = edited.startsWith("visualDensity") ? "visualDensity" : "expansionAnimationStyle";
        var resets = new java.util.LinkedHashSet<PropertyName>();
        if (setting && edited.equals(whole)) local.forEach(field -> resets.add(new PropertyName(field)));
        else if (setting && local.contains(edited)) resets.add(new PropertyName(whole));
        resets.remove(name); var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand expansionTileShapeCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted, String family) {
        String edited = name.value(); boolean setting = accepted.explicitValue().isPresent();
        var local = ExpansionTileWidgetPropertySchema.shapeLocalProperties(family); String kindField = family + "Kind";
        var resets = new java.util.LinkedHashSet<PropertyName>(); var patches = new java.util.ArrayList<PatchProperties.Patch>();
        if (edited.equals(family) && setting) local.forEach(field -> resets.add(new PropertyName(field)));
        else if (local.contains(edited)) {
            if (setting) {
                resets.add(new PropertyName(family));
                String kind = edited.equals(kindField) ? ((PropertyValue.StringValue) accepted.explicitValue().orElseThrow()).value()
                        : widget.properties().get(new PropertyName(kindField)) instanceof PropertyValue.StringValue current
                                && ExpansionTileWidgetPropertySchema.shapePropertyAppliesToKind(edited, current.value())
                                        ? current.value() : ExpansionTileWidgetPropertySchema.preferredShapeKindForProperty(edited);
                local.stream().filter(field -> !field.equals(kindField) && !ExpansionTileWidgetPropertySchema.shapePropertyAppliesToKind(field, kind))
                        .forEach(field -> resets.add(new PropertyName(field)));
                if (!edited.equals(kindField) && !new PropertyValue.StringValue(kind).equals(widget.properties().get(new PropertyName(kindField))))
                    patches.add(new PatchProperties.SetPatch(new PropertyName(kindField), new PropertyValue.StringValue(kind)));
            } else if (edited.equals(kindField)) local.forEach(field -> resets.add(new PropertyName(field)));
        }
        resets.remove(name); resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand alertDialogPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited=name.value();
        if (edited.equals("shape") || CardWidgetPropertySchema.builtInShapePropertyNames().contains(edited))
            return cardPropertyCommand(widget,name,accepted);
        boolean setting=accepted.explicitValue().isPresent();
        var resets=new java.util.LinkedHashSet<PropertyName>();
        for (String family : dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.styleFamilies()) {
            var local=dev.flutter.netbeans.designer.catalog.AlertDialogWidgetPropertySchema.localStyleProperties(family);
            if (setting && edited.equals(family)) local.forEach(n -> resets.add(new PropertyName(n)));
            if (setting && local.contains(edited)) {
                resets.add(new PropertyName(family));
                String opposite=switch(edited.substring(family.length())) {
                    case "Foreground" -> "Color"; case "Color" -> "Foreground";
                    case "Background" -> "BackgroundColor"; case "BackgroundColor" -> "Background"; default -> null;
                };
                if (opposite!=null) resets.add(new PropertyName(family+opposite));
            }
        }
        resets.remove(name);
        var patches=new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(n -> patches.add(new PatchProperties.ResetPatch(n)));
        patches.add(setting ? new PatchProperties.SetPatch(name,accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return new PatchProperties(widget.id(),patches);
    }

    private DesignerCommand listTilePropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value();
        boolean setting = accepted.explicitValue().isPresent();
        if (edited.equals("shape") || edited.equals("shapeKind") || ListTileWidgetPropertySchema.isShapeDetailProperty(edited))
            return cardPropertyCommand(widget, name, accepted);
        if (edited.equals("isThreeLine") && accepted.explicitValue().filter(new PropertyValue.BooleanValue(true)::equals).isPresent()
                && (!(widget.slots().get(new SlotName("subtitle")) instanceof WidgetSlot.SingleSlot subtitle) || subtitle.child().isEmpty()))
            throw new IllegalArgumentException("Cannot enable ListTile Three line: add a widget to Subtitle first. No subtitle is created automatically.");
        var resets = new java.util.LinkedHashSet<PropertyName>();
        for (String family : ListTileWidgetPropertySchema.styleFamilies()) {
            var local = ListTileWidgetPropertySchema.localTextStyleProperties(family);
            if (setting && edited.equals(family)) local.forEach(field -> resets.add(new PropertyName(field)));
            if (setting && local.contains(edited)) {
                resets.add(new PropertyName(family));
                String suffix = edited.substring(family.length());
                String opposite = switch (suffix) { case "Foreground" -> "Color"; case "Color" -> "Foreground";
                    case "Background" -> "BackgroundColor"; case "BackgroundColor" -> "Background"; default -> null; };
                if (opposite != null) resets.add(new PropertyName(family + opposite));
            }
        }
        if (setting && edited.equals("visualDensity")) {
            resets.add(new PropertyName("visualDensityHorizontal")); resets.add(new PropertyName("visualDensityVertical"));
        } else if (setting && (edited.equals("visualDensityHorizontal") || edited.equals("visualDensityVertical")))
            resets.add(new PropertyName("visualDensity"));
        for (String family : java.util.List.of("iconColor", "textColor", "mouseCursor")) {
            var local = family.equals("mouseCursor") ? ListTileWidgetPropertySchema.mouseCursorStateProperties()
                    : ListTileWidgetPropertySchema.colorStateProperties(family);
            String defaultName = family + "Default";
            if (setting && edited.equals(family)) local.forEach(field -> resets.add(new PropertyName(field)));
            if (local.contains(edited)) {
                if (setting) {
                    if (!edited.equals(defaultName) && !widget.properties().containsKey(new PropertyName(defaultName)))
                        throw new IllegalArgumentException("Cannot set ListTile " + edited + ": set " + defaultName + " first. A local state map requires an explicit Default; no color or cursor is invented.");
                    resets.add(new PropertyName(family));
                } else if (edited.equals(defaultName) && local.stream().filter(field -> !field.equals(defaultName)).anyMatch(field -> widget.properties().containsKey(new PropertyName(field))))
                    throw new IllegalArgumentException("Cannot reset ListTile " + defaultName + ": reset its remaining local state entries first, or set the whole " + family + " value.");
            }
        }
        resets.remove(name);
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand cardPropertyCommand(
            WidgetNode currentWidget,
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        String edited = propertyName.value();
        boolean setting = accepted.explicitValue().isPresent();
        java.util.LinkedHashSet<PropertyName> resets = new java.util.LinkedHashSet<>();
        java.util.ArrayList<PatchProperties.Patch> patches = new java.util.ArrayList<>();
        if (edited.equals("shape") && setting) {
            CardWidgetPropertySchema.builtInShapePropertyNames().forEach(name ->
                    resets.add(new PropertyName(name)));
        } else if (edited.equals("shapeKind") || CardWidgetPropertySchema.isShapeDetailProperty(edited)) {
            PropertyName kindName = new PropertyName("shapeKind");
            if (setting) {
                resets.add(new PropertyName("shape"));
                String kind = edited.equals("shapeKind")
                        ? ((PropertyValue.StringValue) accepted.explicitValue().orElseThrow()).value()
                        : currentWidget.properties().get(kindName) instanceof PropertyValue.StringValue current
                                && CardWidgetPropertySchema.shapePropertyAppliesToKind(edited, current.value())
                                        ? current.value() : CardWidgetPropertySchema.preferredShapeKindForProperty(edited);
                for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) {
                    if (!name.equals("shapeKind") && !CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind)) {
                        resets.add(new PropertyName(name));
                    }
                }
                if (!edited.equals("shapeKind")
                        && !new PropertyValue.StringValue(kind).equals(currentWidget.properties().get(kindName))) {
                    patches.add(new PatchProperties.SetPatch(kindName, new PropertyValue.StringValue(kind)));
                }
            } else if (edited.equals("shapeKind")) {
                CardWidgetPropertySchema.builtInShapePropertyNames().forEach(name ->
                        resets.add(new PropertyName(name)));
            }
        }
        resets.remove(propertyName);
        resets.stream().filter(currentWidget.properties()::containsKey)
                .forEach(name -> patches.add(new PatchProperties.ResetPatch(name)));
        patches.add(accepted.explicitValue().<PatchProperties.Patch>map(value ->
                new PatchProperties.SetPatch(propertyName, value))
                .orElseGet(() -> new PatchProperties.ResetPatch(propertyName)));
        return patches.size() == 1 ? ordinaryPropertyCommand(currentWidget, propertyName, accepted)
                : new PatchProperties(currentWidget.id(), patches);
    }

    private DesignerCommand clipPathPropertyCommand(
            WidgetNode currentWidget,
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        boolean setting = accepted.explicitValue().isPresent();
        PropertyName opposite = CLIP_PATH_CLIPPER.equals(propertyName)
                ? CLIP_PATH_SHAPE
                : CLIP_PATH_SHAPE.equals(propertyName)
                        ? CLIP_PATH_CLIPPER : null;
        if (!setting || opposite == null
                || !currentWidget.properties().containsKey(opposite)) {
            return ordinaryPropertyCommand(currentWidget, propertyName, accepted);
        }
        return new PatchProperties(currentWidget.id(), java.util.List.of(
                new PatchProperties.ResetPatch(opposite),
                new PatchProperties.SetPatch(
                        propertyName, accepted.explicitValue().orElseThrow())));
    }

    private DesignerCommand textFieldCompoundPropertyCommand(
            WidgetNode currentWidget,
            java.util.List<PropertyName> compound,
            PropertyName editedProperty,
            FlutterPropertyCellValue accepted) {
        java.util.ArrayList<PatchProperties.Patch> patches =
                new java.util.ArrayList<>(compound.size());
        Optional<PropertyValue> explicit = accepted.explicitValue();
        if (explicit.isEmpty()) {
            compound.forEach(property ->
                patches.add(new PatchProperties.ResetPatch(property)));
        } else {
            PropertyValue entered = explicit.orElseThrow();
            for (PropertyName property : compound) {
                // A first direct-cell edit has no peer values to preserve. Seed
                // the complete Flutter value from the user's entered scalar;
                // never fabricate a numeric zero or constructor default.
                PropertyValue value = property.equals(editedProperty)
                        ? entered
                        : currentWidget.properties().getOrDefault(property, entered);
                patches.add(new PatchProperties.SetPatch(property, value));
            }
        }
        return new PatchProperties(currentWidget.id(), patches);
    }

    private static java.util.List<PropertyName> textFieldCompoundProperties(
            PropertyName propertyName) {
        return TextFieldWidgetPropertySchema.find(propertyName)
                .map(TextFieldWidgetPropertySchema.Definition::target)
                .map(target -> switch (target) {
                    case CURSOR_RADIUS -> TEXT_FIELD_CURSOR_RADIUS;
                    case SCROLL_PADDING -> TEXT_FIELD_SCROLL_PADDING;
                    default -> java.util.List.<PropertyName>of();
                })
                .orElseGet(java.util.List::of);
    }

    private DesignerCommand ordinaryPropertyCommand(
            WidgetNode currentWidget,
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        return accepted.explicitValue()
                .<DesignerCommand>map(explicit ->
                    new SetProperty(currentWidget.id(), propertyName, explicit))
                .orElseGet(() -> new ResetProperty(
                        currentWidget.id(), propertyName));
    }

    private static boolean hasNonNoneContainerClip(WidgetNode currentWidget) {
        PropertyValue value = currentWidget.properties().get(CONTAINER_CLIP);
        return value != null && nonNoneClip(value);
    }

    private static boolean nonNoneClip(PropertyValue value) {
        return value instanceof PropertyValue.EnumValue clip
                && "Clip".equals(clip.type())
                && !"none".equals(clip.value());
    }

    private static Optional<ColorSource> colorSource(PropertyValue value) {
        return switch (value) {
            case null -> Optional.empty();
            case PropertyValue.ColorValue literal ->
                Optional.of(new ColorSource.Literal(literal.argb()));
            case PropertyValue.ThemeTokenValue theme ->
                Optional.of(new ColorSource.Theme(theme.token()));
            default -> throw new IllegalStateException(
                    "Container color uses an unsupported value kind: " + value.kind());
        };
    }

    private static PropertyValue.BoxDecorationValue emptyContainerDecoration(
            Optional<ColorSource> color) {
        return new PropertyValue.BoxDecorationValue(
                color,
                Optional.empty(),
                Optional.empty(),
                java.util.List.of(),
                Optional.empty(),
                Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    static String displayName(PropertyName propertyName) {
        return displayName(propertyName.value());
    }

    static String displayName(SlotName slotName) {
        return displayName(slotName.value());
    }

    private static String displayName(String value) {
        StringBuilder result = new StringBuilder(value.length() + 4);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (index > 0 && Character.isUpperCase(current)) {
                result.append(' ');
            }
            result.append(index == 0
                    ? Character.toUpperCase(current) : current);
        }
        return result.toString();
    }

    private static boolean isModernButton(WidgetNode node) {
        return TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE.equals(node.type())
                || OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(node.type())
                || FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(node.type());
    }

    private static boolean hasFullButtonStyle(WidgetNode node) {
        return isModernButton(node) || IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(node.type())
                || MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(node.type())
                || SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.equals(node.type())
                || MenuBarWidgetPropertySchema.MENU_BAR_TYPE.equals(node.type());
    }

    private DesignerCommand submenuButtonPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value();
        if (edited.equals("menuStyle") || SubmenuButtonWidgetPropertySchema.menuStyleProperties().contains(edited)) {
            // Reuse the reviewed MenuStyle dependency planner in its own namespace;
            // only its returned property patches are published on the real node.
            var projectedValues = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            widget.properties().forEach((key, value) -> {
                if (key.value().equals("menuStyle")) projectedValues.put(new PropertyName("style"), value);
                else if (SubmenuButtonWidgetPropertySchema.menuStyleProperties().contains(key.value()))
                    projectedValues.put(new PropertyName(SubmenuButtonWidgetPropertySchema.menuStyleSourceName(key.value())), value);
            });
            var projection = new WidgetNode(widget.id(), MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE,
                    projectedValues, widget.slots(), widget.extensions(), widget.stateBinding(), widget.propertyBindings());
            var projectedName = new PropertyName(edited.equals("menuStyle") ? "style" : SubmenuButtonWidgetPropertySchema.menuStyleSourceName(edited));
            var command = iconButtonPropertyCommand(projection, projectedName, accepted);
            if (command instanceof SetProperty set) return new SetProperty(widget.id(), submenuMenuStyleName(set.propertyName()), set.value());
            if (command instanceof ResetProperty reset) return new ResetProperty(widget.id(), submenuMenuStyleName(reset.propertyName()));
            if (command instanceof PatchProperties patch) return new PatchProperties(widget.id(), patch.patches().stream().<PatchProperties.Patch>map(value ->
                    value instanceof PatchProperties.SetPatch set ? new PatchProperties.SetPatch(submenuMenuStyleName(set.propertyName()), set.value())
                            : new PatchProperties.ResetPatch(submenuMenuStyleName(value.propertyName()))).toList());
            throw new IllegalStateException("Menu style planner returned an unsupported command.");
        }
        var iconFields = SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties();
        if (accepted.explicitValue().isPresent() && (edited.equals("submenuIcon") || iconFields.contains(edited))) {
            var patches = new java.util.ArrayList<PatchProperties.Patch>();
            var resets = edited.equals("submenuIcon") ? iconFields : java.util.List.of("submenuIcon");
            resets.stream().map(PropertyName::new).filter(widget.properties()::containsKey).forEach(key -> patches.add(new PatchProperties.ResetPatch(key)));
            patches.add(new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()));
            return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
        }
        return iconButtonPropertyCommand(widget, name, accepted);
    }

    private static PropertyName submenuMenuStyleName(PropertyName name) {
        if (!name.value().startsWith("style")) throw new IllegalArgumentException("Only MenuStyle properties may be mapped into SubmenuButton.");
        return new PropertyName("menuStyle" + name.value().substring("style".length()));
    }

    private DesignerCommand menuItemButtonPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value(); var local = MenuItemButtonWidgetPropertySchema.shortcutLocalProperties();
        if (!edited.equals("shortcut") && !local.contains(edited)) return iconButtonPropertyCommand(widget, name, accepted);
        boolean setting = accepted.explicitValue().isPresent(); boolean anchor = edited.equals("shortcutTrigger") || edited.equals("shortcutCharacter");
        var resets = new java.util.LinkedHashSet<PropertyName>();
        if (edited.equals("shortcut") && setting || anchor && !setting) local.forEach(field -> resets.add(new PropertyName(field)));
        else if (anchor && setting) {
            resets.add(new PropertyName("shortcut")); resets.add(new PropertyName(edited.equals("shortcutTrigger") ? "shortcutCharacter" : "shortcutTrigger"));
            if (edited.equals("shortcutCharacter")) { resets.add(new PropertyName("shortcutShift")); resets.add(new PropertyName("shortcutNumLock")); }
        } else if (setting) {
            boolean single = widget.properties().containsKey(new PropertyName("shortcutTrigger"));
            boolean character = widget.properties().containsKey(new PropertyName("shortcutCharacter"));
            if (!single && !character) throw new IllegalArgumentException("Cannot set MenuItemButton " + edited + ": choose Shortcut trigger or Shortcut character first. No key is invented and the whole shortcut is not silently discarded.");
            if (character && (edited.equals("shortcutShift") || edited.equals("shortcutNumLock"))) throw new IllegalArgumentException("CharacterActivator has no Shift or Num lock field. Select a Shortcut trigger first; existing character and modifiers remain unchanged.");
        }
        resets.remove(name); var patches = new java.util.ArrayList<PatchProperties.Patch>();
        resets.stream().filter(widget.properties()::containsKey).forEach(field -> patches.add(new PatchProperties.ResetPatch(field)));
        patches.add(setting ? new PatchProperties.SetPatch(name, accepted.explicitValue().orElseThrow()) : new PatchProperties.ResetPatch(name));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand iconButtonPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        if (accepted.explicitValue().isEmpty()) {
            var resets = new java.util.LinkedHashSet<PropertyName>();
            if (name.value().endsWith("TextInherit") && widget.properties().containsKey(name)) {
                for (String prefix : modernButtonStatePrefixes(widget))
                    for (String suffix : java.util.List.of("TextInherit", "TextTheme")) {
                        var key = new PropertyName(prefix + suffix);
                        if (widget.properties().containsKey(key)) resets.add(key);
                    }
            } else if (java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY").contains(name.value())) {
                for (String key : java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY"))
                    if (widget.properties().containsKey(new PropertyName(key))) resets.add(new PropertyName(key));
            }
            if (resets.size() > 1) return new PatchProperties(widget.id(), resets.stream()
                    .<PatchProperties.Patch>map(PatchProperties.ResetPatch::new).toList());
            return ordinaryPropertyCommand(widget, name, accepted);
        }
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        var style = new PropertyName("style");
        if (name.equals(style)) {
            for (String local : modernButtonLocalStyleProperties(widget)) {
                var key = new PropertyName(local);
                if (widget.properties().containsKey(key)) patches.add(new PatchProperties.ResetPatch(key));
            }
        } else if (modernButtonLocalStyleProperties(widget).contains(name.value()) && widget.properties().containsKey(style)) {
            patches.add(new PatchProperties.ResetPatch(style));
        }
        var value = accepted.explicitValue().orElseThrow();
        modernButtonStyleDependencies(widget, name, value, patches);
        patches.add(new PatchProperties.SetPatch(name, value));
        return patches.size() == 1 ? ordinaryPropertyCommand(widget, name, accepted) : new PatchProperties(widget.id(), patches);
    }

    private static String modernButtonName(WidgetNode node) {
        return FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(node.type()) ? "FilledButton"
                : OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(node.type()) ? "OutlinedButton" : "TextButton";
    }

    private static java.util.List<String> modernButtonStatePrefixes(WidgetNode node) {
        if (SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.equals(node.type())) return SubmenuButtonWidgetPropertySchema.statePrefixes();
        if (MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE.equals(node.type())) return MenuAnchorWidgetPropertySchema.statePrefixes();
        if (MenuBarWidgetPropertySchema.MENU_BAR_TYPE.equals(node.type())) return MenuBarWidgetPropertySchema.statePrefixes();
        if (MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(node.type())) return MenuItemButtonWidgetPropertySchema.statePrefixes();
        if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(node.type())) return IconButtonWidgetPropertySchema.statePrefixes();
        if (FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(node.type())) return FilledButtonWidgetPropertySchema.statePrefixes();
        return OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(node.type())
                ? OutlinedButtonWidgetPropertySchema.statePrefixes() : TextButtonWidgetPropertySchema.statePrefixes();
    }

    private static java.util.List<String> modernButtonLocalStyleProperties(WidgetNode node) {
        if (SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.equals(node.type())) return SubmenuButtonWidgetPropertySchema.localStyleProperties();
        if (MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE.equals(node.type())) return MenuAnchorWidgetPropertySchema.localStyleProperties();
        if (MenuBarWidgetPropertySchema.MENU_BAR_TYPE.equals(node.type())) return MenuBarWidgetPropertySchema.localStyleProperties();
        if (MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(node.type())) return MenuItemButtonWidgetPropertySchema.localStyleProperties();
        if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(node.type())) return IconButtonWidgetPropertySchema.localStyleProperties();
        if (FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(node.type())) return FilledButtonWidgetPropertySchema.localStyleProperties();
        return OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(node.type())
                ? OutlinedButtonWidgetPropertySchema.localStyleProperties() : TextButtonWidgetPropertySchema.localStyleProperties();
    }

    private static boolean modernButtonIsIcon(WidgetNode node) {
        if (FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(node.type())) return FilledButtonWidgetPropertySchema.isIcon(node);
        return OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(node.type())
                ? OutlinedButtonWidgetPropertySchema.isIcon(node) : TextButtonWidgetPropertySchema.isIcon(node);
    }

    private DesignerCommand modernButtonPropertyCommand(WidgetNode currentWidget, PropertyName name, FlutterPropertyCellValue accepted) {
        if (accepted.explicitValue().isEmpty()) {
            var resets = new java.util.LinkedHashSet<PropertyName>();
            if (name.value().endsWith("TextInherit") && currentWidget.properties().containsKey(name)) {
                for (String prefix : modernButtonStatePrefixes(currentWidget))
                    for (String suffix : java.util.List.of("TextInherit", "TextTheme")) {
                        var key = new PropertyName(prefix + suffix);
                        if (currentWidget.properties().containsKey(key)) resets.add(key);
                    }
            } else if (java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY").contains(name.value())) {
                for (String key : java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY"))
                    if (currentWidget.properties().containsKey(new PropertyName(key))) resets.add(new PropertyName(key));
            }
            if (resets.size() > 1) return new PatchProperties(currentWidget.id(), resets.stream()
                    .<PatchProperties.Patch>map(PatchProperties.ResetPatch::new).toList());
            return ordinaryPropertyCommand(currentWidget, name, accepted);
        }
        var value = accepted.explicitValue().orElseThrow();
        var variant = new PropertyName("variant"); var semantics = new PropertyName("isSemanticButton");
        var alignment = new PropertyName("iconAlignment"); var style = new PropertyName("style");
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        if (name.equals(style)) {
            for (String local : modernButtonLocalStyleProperties(currentWidget)) {
                var key = new PropertyName(local);
                if (currentWidget.properties().containsKey(key)) patches.add(new PatchProperties.ResetPatch(key));
            }
        } else if (modernButtonLocalStyleProperties(currentWidget).contains(name.value()) && currentWidget.properties().containsKey(style)) {
            patches.add(new PatchProperties.ResetPatch(style));
        }
        modernButtonStyleDependencies(currentWidget, name, value, patches);
        boolean filled = FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(currentWidget.type());
        boolean selectingIcon = name.equals(variant) && (new PropertyValue.StringValue("icon").equals(value)
                || filled && new PropertyValue.StringValue("tonalIcon").equals(value));
        boolean selectingStandard = name.equals(variant) && (new PropertyValue.StringValue("standard").equals(value)
                || filled && new PropertyValue.StringValue("tonal").equals(value));
        if (selectingIcon || name.equals(alignment)) {
            if (filled && (!(currentWidget.slots().get(CHILD_SLOT) instanceof WidgetSlot.SingleSlot child) || child.child().isEmpty()))
                throw new IllegalArgumentException("Cannot select an icon constructor on FilledButton '" + currentWidget.id()
                        + "': Child is empty. Add a Child first; Icon and Tonal icon require a Label.");
            if (currentWidget.properties().containsKey(semantics)) patches.add(new PatchProperties.ResetPatch(semantics));
            if (name.equals(alignment) && !modernButtonIsIcon(currentWidget))
                patches.add(new PatchProperties.SetPatch(variant, new PropertyValue.StringValue(
                        filled && FilledButtonWidgetPropertySchema.isTonal(currentWidget) ? "tonalIcon" : "icon")));
        } else if (selectingStandard || name.equals(semantics)) {
            var icon = currentWidget.slots().get(new SlotName("icon"));
            if (icon instanceof WidgetSlot.SingleSlot single && single.child().isPresent())
                throw new IllegalArgumentException("Cannot set " + (name.equals(semantics) ? "Semantic button" : "Constructor " + ((PropertyValue.StringValue) value).value())
                        + " on " + modernButtonName(currentWidget) + " '" + currentWidget.id() + "': Icon contains widget '" + single.child().orElseThrow().id()
                        + "'. Move or clear Icon first; the existing icon will not be deleted.");
            if (currentWidget.properties().containsKey(alignment)) patches.add(new PatchProperties.ResetPatch(alignment));
            if (name.equals(semantics) && modernButtonIsIcon(currentWidget))
                patches.add(new PatchProperties.SetPatch(variant, new PropertyValue.StringValue("standard")));
        }
        patches.add(new PatchProperties.SetPatch(name, value));
        return patches.size() == 1 ? ordinaryPropertyCommand(currentWidget, name, accepted)
                : new PatchProperties(currentWidget.id(), patches);
    }

    private static void modernButtonStyleDependencies(WidgetNode widget, PropertyName name,
            PropertyValue value, java.util.List<PatchProperties.Patch> patches) {
        String edited = name.value();
        for (String prefix : modernButtonStatePrefixes(widget)) {
            if (edited.equals(prefix + "TextBackground") || edited.equals(prefix + "TextBackgroundColor")) {
                var opposite = new PropertyName(prefix + (edited.endsWith("Color") ? "TextBackground" : "TextBackgroundColor"));
                if (widget.properties().containsKey(opposite)) patches.add(new PatchProperties.ResetPatch(opposite));
            }
            if (edited.equals(prefix + "TextInherit") || edited.equals(prefix + "TextTheme")) {
                var inherit = edited.endsWith("TextInherit") ? value : widget.properties().getOrDefault(
                        new PropertyName("styleTextInherit"), widget.properties().getOrDefault(
                        new PropertyName("styleDisabledTextInherit"), new PropertyValue.BooleanValue(true)));
                for (String peer : modernButtonStatePrefixes(widget)) {
                    var key = new PropertyName(peer + "TextInherit");
                    if (!key.equals(name) && (peer.equals("style") || peer.equals("styleDisabled") || peer.equals(prefix)
                            || widget.properties().containsKey(key) || widget.properties().containsKey(new PropertyName(peer + "TextTheme")))
                            && !inherit.equals(widget.properties().get(key)))
                        patches.add(new PatchProperties.SetPatch(key, inherit));
                }
            }
            if (edited.startsWith(prefix + "Shape") && (edited.contains("Radius") || edited.endsWith("Eccentricity"))) {
                var kind = new PropertyName(prefix + "ShapeKind");
                if (!widget.properties().containsKey(kind) && (prefix.equals("styleDisabled")
                        || !widget.properties().containsKey(new PropertyName("styleShapeKind"))))
                    patches.add(new PatchProperties.SetPatch(kind, new PropertyValue.StringValue(
                            edited.endsWith("Eccentricity") ? "circle" : "roundedRectangle")));
            }
        }
        if (java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY").contains(edited)) {
            for (String key : java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY")) {
                var property = new PropertyName(key);
                if (!property.equals(name) && !widget.properties().containsKey(property))
                    patches.add(new PatchProperties.SetPatch(property, key.endsWith("Kind")
                            ? new PropertyValue.StringValue("physical") : new PropertyValue.DoubleValue(java.math.BigDecimal.ZERO)));
            }
        }
    }

    private DesignerCommand refreshIndicatorPropertyCommand(WidgetNode currentWidget,
            PropertyName name, FlutterPropertyCellValue accepted) {
        if (accepted.explicitValue().isEmpty()) return ordinaryPropertyCommand(currentWidget, name, accepted);
        var value = accepted.explicitValue().orElseThrow();
        var variant = new PropertyName("variant");
        var status = new PropertyName("onStatusChange");
        var patches = new java.util.ArrayList<PatchProperties.Patch>();
        boolean selectingNoSpinner = name.equals(variant) && new PropertyValue.StringValue("noSpinner").equals(value);
        boolean settingStatus = name.equals(status);
        boolean settingSpinner = RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties().contains(name.value());
        if (selectingNoSpinner || settingStatus) {
            for (String spinner : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties()) {
                var key = new PropertyName(spinner);
                if (currentWidget.properties().containsKey(key)) patches.add(new PatchProperties.ResetPatch(key));
            }
            if (settingStatus && !RefreshIndicatorWidgetPropertySchema.isNoSpinner(currentWidget))
                patches.add(new PatchProperties.SetPatch(variant, new PropertyValue.StringValue("noSpinner")));
        } else if (name.equals(variant) || settingSpinner && RefreshIndicatorWidgetPropertySchema.isNoSpinner(currentWidget)) {
            if (currentWidget.properties().containsKey(status)) patches.add(new PatchProperties.ResetPatch(status));
            if (settingSpinner) patches.add(new PatchProperties.SetPatch(variant, new PropertyValue.StringValue("material")));
        }
        patches.add(new PatchProperties.SetPatch(name, value));
        return patches.size() == 1 ? ordinaryPropertyCommand(currentWidget, name, accepted)
                : new PatchProperties(currentWidget.id(), patches);
    }

    private String propertyDescription(
            PropertyDefinition property,
            String schemaDescription) {
        String accepted = property.constraints().stream()
                .map(dev.flutter.netbeans.designer.catalog.PropertyValueConstraint::description)
                .reduce((left, right) -> left + "; " + right)
                .orElse("catalog-declared values");
        String reset;
        if ((DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE.equals(widget.type())
                || IconThemeWidgetPropertySchema.ICON_THEME_TYPE.equals(widget.type()))
                && "merge".equals(property.name().value())) {
            reset = " This required Designer-only selector cannot be unset or reset; "
                    + "no merge argument is emitted.";
        } else if ((CardWidgetPropertySchema.CARD_TYPE.equals(widget.type())
                || CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE.equals(widget.type())
                || RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.equals(widget.type())
                || hasFullButtonStyle(widget))
                && "variant".equals(property.name().value())) {
            reset = " This required Designer constructor selector cannot be unset or reset; "
                    + "no variant argument is emitted.";
        } else if (RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.equals(widget.type())
                && "onRefresh".equals(property.name().value())) {
            reset = " Restore Default removes the project reference and generates onRefresh: () async {}; the required callback is not null or omitted.";
        } else if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(widget.type()) && "onPressed".equals(property.name().value())) {
            reset = " Restore Default removes the project callback. Enabled generates a no-op onPressed when unset; disabled emits null and suppresses long press. Project callbacks are retained without execution in isolated Canvas.";
        } else if (MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.equals(widget.type()) && "onPressed".equals(property.name().value())) {
            reset = " Restore Default removes only the project handler. Enabled emits a no-op when unset; disabled emits null without discarding the retained handler. No long-press callback or global shortcut registration is generated.";
        } else if (isModernButton(widget) && "onPressed".equals(property.name().value())) {
            reset = " Restore Default removes the project callback, not the required Dart argument. Enabled with no activation callbacks generates a no-op; disabled or long-press-only emits onPressed: null.";
        } else if (hasFullButtonStyle(widget) && property.name().value().endsWith("TextInherit")) {
            reset = " Editing Text inherit keeps all configured state inherit flags consistent. Restore Default removes every Text inherit and Text theme selection together, preserving other style fields.";
        } else if (hasFullButtonStyle(widget)
                && java.util.List.of("styleAlignmentKind", "styleAlignmentX", "styleAlignmentY").contains(property.name().value())) {
            reset = " Editing seeds missing alignment components (Physical, X 0, Y 0). Restore Default removes the complete style alignment.";
        } else if (hasFullButtonStyle(widget) && "enabled".equals(property.name().value())) {
            reset = " Required Designer activation selector; cannot be unset or reset. No enabled argument is emitted.";
        } else if (property.parameter().required()) {
            reset = " This required constructor argument cannot be unset.";
        } else if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(widget.type())) {
            reset = TextFieldWidgetPropertySchema.find(property.name())
                    .map(TextFieldWidgetPropertySchema.Definition::target)
                    .map(target -> switch (target) {
                        case CURSOR_RADIUS ->
                            " When unset, editing either axis seeds both axes from "
                            + "the entered value; later edits preserve the other axis. "
                            + "Restore Default removes the complete cursorRadius value.";
                        case SCROLL_PADDING ->
                            " When unset, editing any edge seeds all four edges from "
                            + "the entered value; later edits preserve the other edges. "
                            + "Restore Default removes the complete scrollPadding value.";
                        default ->
                            " Restore Default removes the explicit constructor argument.";
                    })
                    .orElse(" Restore Default removes the explicit constructor argument.");
        } else {
            reset = " Restore Default removes the explicit constructor argument.";
        }
        return schemaDescription + " Accepted: " + accepted + "." + reset;
    }

    private static Sheet.Set propertySet(
            String name,
            String displayName,
            String description) {
        Sheet.Set set = new Sheet.Set();
        set.setName(name);
        set.setDisplayName(displayName);
        set.setShortDescription(description);
        return set;
    }

    private PropertySupport.ReadOnly<String> readOnlyProperty(
            PropertyName propertyName,
            String displayName,
            String description) {
        return new PropertySupport.ReadOnly<>(
                propertyName.value(), String.class, displayName, description) {
            @Override
            public String getValue() {
                Presentation current = presentation;
                PropertyValue value =
                        current.widget().properties().get(propertyName);
                return value == null
                        ? NOT_SET
                        : PropertyValueFormatter.format(value);
            }
        };
    }

    private static PropertySupport.ReadOnly<String> readOnly(
            String name,
            String displayName,
            String description,
            String value) {
        return new PropertySupport.ReadOnly<>(name, String.class, displayName, description) {
            @Override
            public String getValue() {
                return value;
            }
        };
    }
}
