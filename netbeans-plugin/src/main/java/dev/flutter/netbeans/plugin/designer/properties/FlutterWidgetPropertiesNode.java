package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCapabilityCatalog;
import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ContainerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconWidgetPropertySchema;
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
import org.openide.util.lookup.Lookups;

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
    private static final WidgetTypeId STACK_TYPE =
            new WidgetTypeId("flutter.widgets.Stack");
    private static final WidgetTypeId EXPANDED_TYPE =
            new WidgetTypeId("flutter.widgets.Expanded");
    private static final WidgetTypeId IMAGE_TYPE =
            new WidgetTypeId("flutter.widgets.Image");
    private static final PropertyName ALIGNMENT_PROPERTY =
            new PropertyName("alignment");
    private static final PropertyName WIDTH_FACTOR_PROPERTY =
            new PropertyName("widthFactor");
    private static final PropertyName HEIGHT_FACTOR_PROPERTY =
            new PropertyName("heightFactor");
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
    private static final PropertyName CONTAINER_COLOR = new PropertyName("color");
    private static final PropertyName CONTAINER_DECORATION = new PropertyName("decoration");
    private static final PropertyName CONTAINER_CLIP = new PropertyName("clipBehavior");
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

    private final WidgetNode widget;
    private final WidgetDefinition definition;
    private final PropertyMutationHandler mutationHandler;
    private final FlutterWidgetSlotEditorContext slotEditorContext;
    private final SlotMutationHandler slotMutationHandler;
    private final FlutterImageAssetChoices imageAssetChoices;

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
        super(
                Objects.requireNonNull(children, "children"),
                Lookups.fixed(
                        Objects.requireNonNull(widget, "widget").id(),
                        widget,
                        Objects.requireNonNull(definition, "definition")));
        if (!widget.type().equals(definition.typeId())) {
            throw new IllegalArgumentException(
                    "Widget type " + widget.type() + " does not match catalog definition "
                    + definition.typeId());
        }
        this.widget = widget;
        this.definition = definition;
        this.mutationHandler = mutationHandler;
        this.slotEditorContext = slotEditorContext;
        this.slotMutationHandler = slotMutationHandler;
        this.imageAssetChoices = Objects.requireNonNull(
                imageAssetChoices, "imageAssetChoices");
        if (slotMutationHandler != null && slotEditorContext == null) {
            throw new IllegalArgumentException(
                    "A slot mutation handler requires a slot editor context.");
        }
        String displayName = definition.palette().displayName();
        setName(widget.id().toString());
        setDisplayName(displayName);
        if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(widget.type())) {
            setShortDescription(
                    displayName + " — " + widget.id()
                    + ". Runtime typed text, selection, controller state, and focus "
                    + "state are not stored by Designer.");
        } else {
            setShortDescription(displayName + " — " + widget.id());
        }
        FlutterWidgetIconRegistry.findIconPath(widget.type())
                .ifPresent(this::setIconBaseWithExtension);
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
        } else if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(widget.type())) {
            addTextFieldPropertySets(sheet, hasSlotTab);
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
            if (slotEditorContext != null && slotMutationHandler != null) {
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
        FlutterWidgetSlotCellValue captured =
                FlutterWidgetSlotCellValue.current(summary);
        AtomicBoolean submitted = new AtomicBoolean();
        PropertySupport.ReadWrite<FlutterWidgetSlotCellValue> result =
                new PropertySupport.ReadWrite<>(
                slot.name().value(),
                FlutterWidgetSlotCellValue.class,
                displayName(slot.name()),
                description) {
            @Override
            public FlutterWidgetSlotCellValue getValue() {
                return captured;
            }

            @Override
            public void setValue(FlutterWidgetSlotCellValue value) {
                Objects.requireNonNull(value, "value");
                value.mutation().ifPresent(mutation -> {
                    if (!widget.id().equals(mutation.ownerId())
                            || !slot.name().equals(mutation.slotName())) {
                        throw new IllegalArgumentException(
                                "Slot edit targets another widget or named slot.");
                    }
                    if (submitted.compareAndSet(false, true)) {
                        slotMutationHandler.submit(mutation);
                    }
                });
            }

            @Override
            public PropertyEditor getPropertyEditor() {
                return new FlutterWidgetSlotPropertyEditor(
                        widget, definition, slot, slotEditorContext);
            }
        };
        result.setValue("changeImmediate", Boolean.FALSE);
        result.setValue("canEditAsText", Boolean.FALSE);
        return result;
    }

    private String slotSummary(SlotDefinition slot, WidgetSlot value) {
        if (value == null) {
            return "Empty";
        }
        if (value.cardinality() != slot.cardinality()) {
            return "Invalid " + value.cardinality().wireName() + " slot";
        }
        return switch (value) {
            case WidgetSlot.SingleSlot single -> single.child()
                    .map(this::widgetDisplayName)
                    .orElse("Empty");
            case WidgetSlot.ListSlot list -> list.children().isEmpty()
                    ? "Empty"
                    : list.children().size() == 1
                            ? "1 widget"
                            : list.children().size() + " widgets";
        };
    }

    private String widgetDisplayName(WidgetNode child) {
        if (slotEditorContext != null) {
            return slotEditorContext.catalog().find(child.type())
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
        if (ASPECT_RATIO_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Optional child laid out to fill the box resolved from Aspect ratio. "
                    + "Occupancy: " + count + "/" + maximum
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
        if (EXPANDED_TYPE.equals(widget.type())
                && CHILD_SLOT.equals(slot.name())) {
            return "Required child expanded with FlexFit.tight along the direct Row or "
                    + "Column main axis. Occupancy: " + count + "/" + maximum
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
        return "Exact '" + slot.name().value() + "' " + cardinality
                + " slot. Occupancy: " + count + "/" + maximum
                + "; minimum: " + slot.minChildren()
                + ". Open the custom editor to add, move, reorder, or remove a widget.";
    }

    private Sheet.Set createGenericPropertySet() {
        boolean aspectRatio = ASPECT_RATIO_TYPE.equals(widget.type());
        boolean opacity = OPACITY_TYPE.equals(widget.type());
        boolean align = ALIGN_TYPE.equals(widget.type());
        boolean fractionallySizedBox = FRACTIONALLY_SIZED_BOX_TYPE.equals(widget.type());
        boolean stack = STACK_TYPE.equals(widget.type());
        boolean expanded = EXPANDED_TYPE.equals(widget.type());
        boolean image = IMAGE_TYPE.equals(widget.type());
        Sheet.Set properties = propertySet(
                PROPERTIES_SET_NAME,
                "Widget properties",
                aspectRatio
                        ? "Sizing contract for the selected AspectRatio widget."
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
                        : stack
                                ? "Layer alignment, direction, sizing, clipping, and ordered "
                                        + "non-Positioned children for the selected Stack widget."
                        : expanded
                                ? "Remaining-space allocation and required child contract for "
                                        + "the selected direct Row or Column Expanded widget."
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
            } else {
                properties.put(projectProperty(property, Optional.empty()));
            }
        }
        return properties;
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
        String value = explicitValue == null
                ? NOT_SET
                : PropertyValueFormatter.format(explicitValue);
        return readOnly(
                property.name().value(),
                projectedDisplayName,
                projectedDescription,
                value);
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
        if (mutationHandler == null
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
        FlutterPropertyCellValue captured = explicitValue == null
                ? FlutterPropertyCellValue.unset()
                : FlutterPropertyCellValue.explicit(explicitValue);
        binding.validate(captured);
        String description = propertyDescription(property, schemaDescription);
        PropertySupport.ReadWrite<FlutterPropertyCellValue> result =
                new PropertySupport.ReadWrite<>(
                propertyName.value(),
                FlutterPropertyCellValue.class,
                displayName,
                description) {
            @Override
            public FlutterPropertyCellValue getValue() {
                return captured;
            }

            @Override
            public void setValue(FlutterPropertyCellValue value) {
                FlutterPropertyCellValue accepted = binding.validate(value);
                if (captured.equals(accepted)) {
                    return;
                }
                DesignerCommand command = propertyMutationCommand(
                        propertyName, accepted);
                mutationHandler.submit(command);
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
                return !captured.isExplicit();
            }

            @Override
            public void restoreDefaultValue()
                    throws IllegalAccessException, InvocationTargetException {
                if (binding.optional() && captured.isExplicit()) {
                    mutationHandler.submit(propertyMutationCommand(
                            propertyName, FlutterPropertyCellValue.unset()));
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
                imageAssetChoices);
        return result;
    }

    private DesignerCommand propertyMutationCommand(
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        if (TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.equals(widget.type())) {
            java.util.List<PropertyName> compound =
                    textFieldCompoundProperties(propertyName);
            if (!compound.isEmpty()) {
                return textFieldCompoundPropertyCommand(
                        compound, propertyName, accepted);
            }
        }
        if (!ContainerWidgetPropertySchema.CONTAINER_TYPE.equals(widget.type())) {
            return ordinaryPropertyCommand(propertyName, accepted);
        }
        java.util.ArrayList<PatchProperties.Patch> patches =
                new java.util.ArrayList<>();
        boolean setting = accepted.explicitValue().isPresent();
        if (CONTAINER_DECORATION.equals(propertyName)) {
            if (setting && widget.properties().containsKey(CONTAINER_COLOR)) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_COLOR));
            } else if (!setting && hasNonNoneContainerClip()) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_CLIP));
            }
        } else if (CONTAINER_COLOR.equals(propertyName) && setting) {
            if (widget.properties().containsKey(CONTAINER_DECORATION)) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_DECORATION));
            }
            if (hasNonNoneContainerClip()) {
                patches.add(new PatchProperties.ResetPatch(CONTAINER_CLIP));
            }
        } else if (CONTAINER_CLIP.equals(propertyName)
                && setting
                && nonNoneClip(accepted.explicitValue().orElseThrow())
                && !widget.properties().containsKey(CONTAINER_DECORATION)) {
            PropertyValue backgroundColor = widget.properties().get(CONTAINER_COLOR);
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
                ? ordinaryPropertyCommand(propertyName, accepted)
                : new PatchProperties(widget.id(), patches);
    }

    private DesignerCommand textFieldCompoundPropertyCommand(
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
                        : widget.properties().getOrDefault(property, entered);
                patches.add(new PatchProperties.SetPatch(property, value));
            }
        }
        return new PatchProperties(widget.id(), patches);
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
            PropertyName propertyName,
            FlutterPropertyCellValue accepted) {
        return accepted.explicitValue()
                .<DesignerCommand>map(explicit ->
                    new SetProperty(widget.id(), propertyName, explicit))
                .orElseGet(() -> new ResetProperty(widget.id(), propertyName));
    }

    private boolean hasNonNoneContainerClip() {
        PropertyValue value = widget.properties().get(CONTAINER_CLIP);
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

    private String propertyDescription(
            PropertyDefinition property,
            String schemaDescription) {
        String accepted = property.constraints().stream()
                .map(dev.flutter.netbeans.designer.catalog.PropertyValueConstraint::description)
                .reduce((left, right) -> left + "; " + right)
                .orElse("catalog-declared values");
        String reset;
        if (property.parameter().required()) {
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
