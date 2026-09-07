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
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircleAvatarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.LinearProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CircularProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshProgressIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.RefreshIndicatorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.OutlinedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FilledButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.CheckboxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SwitchWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ContainerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SafeAreaWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PlaceholderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCapability;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.PatchProperties;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
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
    public static final String GENERAL_TAB_NAME = "General";
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
    private volatile Presentation presentation;

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
            if (!Objects.equals(previous, next)) {
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
        } else if (TextWidgetPropertySchema.TEXT_TYPE.equals(widget.type())) {
            addTextPropertySets(sheet, hasSlotTab);
        } else if (IconWidgetPropertySchema.ICON_TYPE.equals(widget.type())) {
            addIconPropertySets(sheet, hasSlotTab);
        } else if (AppBarWidgetPropertySchema.APP_BAR_TYPE.equals(widget.type())) {
            addAppBarPropertySets(sheet, hasSlotTab);
        } else if (ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE.equals(
                widget.type())) {
            addElevatedButtonPropertySets(sheet, hasSlotTab);
        } else if (TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE.equals(widget.type())) {
            addTextButtonPropertySets(sheet, hasSlotTab);
        } else if (OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(widget.type())) {
            addOutlinedButtonPropertySets(sheet, hasSlotTab);
        } else if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(widget.type())) {
            addIconButtonPropertySets(sheet, hasSlotTab);
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
        } else if (SingleChildScrollViewWidgetPropertySchema
                .SINGLE_CHILD_SCROLL_VIEW_TYPE.equals(widget.type())) {
            addSingleChildScrollViewPropertySets(sheet, hasSlotTab);
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
        if (hasSlotTab) {
            Sheet.Set slots = createSlotsPropertySet();
            assignTab(slots, SLOTS_TAB_NAME);
            sheet.put(slots);
        }
        return sheet;
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
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    schema.encoding()
                            == AppBarWidgetPropertySchema.Encoding.NEWLINE_STRING_LIST,
                    appBarStringPresets(property.name())));
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
                java.util.List.of("default", "depthZero", "all");
            case "shapeKind" -> java.util.List.of(
                    "roundedRectangle",
                    "stadium",
                    "circle",
                    "beveledRectangle",
                    "continuousRectangle");
            default -> java.util.List.of();
        };
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
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    schema.encoding()
                            == ElevatedButtonWidgetPropertySchema.Encoding
                                    .NEWLINE_STRING_LIST,
                    elevatedButtonStringPresets(property.name())));
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
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    textFieldStringPresets(schema)));
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
            groups.get(schema.group()).put(projectProperty(
                    property,
                    Optional.empty(),
                    schema.displayName(),
                    schema.description(),
                    false,
                    presets));
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

    private PropertySupport.ReadWrite<FlutterPropertyCellValue> writableProperty(
            FlutterTypedPropertyEditors.Binding binding,
            PropertyValue explicitValue,
            String displayName,
            String schemaDescription) {
        PropertyDefinition property = binding.definition();
        PropertyName propertyName = property.name();
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
            public FlutterPropertyCellValue getValue() {
                Presentation current = presentation;
                return propertyCellValue(
                        current.widget().properties().get(propertyName));
            }

            @Override
            public void setValue(FlutterPropertyCellValue value)
                    throws IllegalAccessException {
                FlutterPropertyCellValue accepted = binding.validate(value);
                Presentation current = presentation;
                FlutterPropertyCellValue currentValue = propertyCellValue(
                        current.widget().properties().get(propertyName));
                if (currentValue.equals(accepted)) {
                    return;
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
                return binding.createEditor();
            }

            @Override
            public boolean supportsDefaultValue() {
                return binding.optional();
            }

            @Override
            public boolean isDefaultValue() {
                return !getValue().isExplicit();
            }

            @Override
            public void restoreDefaultValue()
                    throws IllegalAccessException, InvocationTargetException {
                Presentation current = presentation;
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
        if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(currentWidget.type())) {
            return iconButtonPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (SwitchWidgetPropertySchema.SWITCH_TYPE.equals(currentWidget.type())) {
            return switchPropertyCommand(currentWidget, propertyName, accepted);
        }
        if (CheckboxWidgetPropertySchema.CHECKBOX_TYPE.equals(currentWidget.type())) {
            return checkboxPropertyCommand(currentWidget, propertyName, accepted);
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
        if (CardWidgetPropertySchema.CARD_TYPE.equals(currentWidget.type())) {
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

    private DesignerCommand switchPropertyCommand(WidgetNode widget, PropertyName name, FlutterPropertyCellValue accepted) {
        String edited = name.value();
        boolean setting = accepted.explicitValue().isPresent();
        var explicit = accepted.explicitValue().orElse(null);
        var resets = new java.util.LinkedHashSet<PropertyName>();
        var sets = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        if (setting && edited.equals("applyCupertinoTheme")) sets.put(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"));
        if (edited.equals("variant") && explicit instanceof PropertyValue.StringValue variant && variant.value().equals("standard"))
            resets.add(new PropertyName("applyCupertinoTheme"));
        for (String prefix : java.util.List.of("Active", "Inactive")) {
            String image = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1) + "ThumbImage";
            String callback = "on" + prefix + "ThumbImageError";
            if (setting && edited.equals(callback) && !widget.properties().containsKey(new PropertyName(image)))
                throw new IllegalArgumentException("Cannot set " + callback + " on Switch '" + widget.id()
                        + "': set " + image + " first; an image-error callback requires its matching provider.");
            if (!setting && edited.equals(image)) resets.add(new PropertyName(callback));
        }
        var families = new java.util.LinkedHashMap<String, java.util.List<String>>();
        for (String family : SwitchWidgetPropertySchema.colorFamilies()) families.put(family, SwitchWidgetPropertySchema.colorStateProperties(family));
        families.put("trackOutlineWidth", SwitchWidgetPropertySchema.outlineWidthStateProperties());
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
        return isModernButton(node) || IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(node.type());
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
        if (IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.equals(node.type())) return IconButtonWidgetPropertySchema.statePrefixes();
        if (FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE.equals(node.type())) return FilledButtonWidgetPropertySchema.statePrefixes();
        return OutlinedButtonWidgetPropertySchema.OUTLINED_BUTTON_TYPE.equals(node.type())
                ? OutlinedButtonWidgetPropertySchema.statePrefixes() : TextButtonWidgetPropertySchema.statePrefixes();
    }

    private static java.util.List<String> modernButtonLocalStyleProperties(WidgetNode node) {
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
