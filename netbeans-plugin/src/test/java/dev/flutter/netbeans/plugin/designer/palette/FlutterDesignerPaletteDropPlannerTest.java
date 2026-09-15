package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.GridViewExtentWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipOvalWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipPathWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRSuperellipseWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PhysicalModelWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DecoratedBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DirectionalityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotAcceptance;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlutterDesignerPaletteDropPlannerTest {
    private static final WidgetCatalog BUILT_INS = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId SCAFFOLD = type("flutter.material.Scaffold");
    private static final WidgetTypeId APP_BAR = type("flutter.material.AppBar");
    private static final WidgetTypeId ELEVATED_BUTTON =
            type("flutter.material.ElevatedButton");
    private static final WidgetTypeId TEXT_FIELD =
            type("flutter.material.TextField");
    private static final WidgetTypeId TEXT = type("flutter.widgets.Text");
    private static final WidgetTypeId CHECKBOX_LIST_TILE =
            type("flutter.material.CheckboxListTile");
    private static final WidgetTypeId COLUMN = type("flutter.widgets.Column");
    private static final WidgetTypeId ROW = type("flutter.widgets.Row");
    private static final WidgetTypeId WRAP = type("flutter.widgets.Wrap");
    private static final WidgetTypeId PADDING = type("flutter.widgets.Padding");
    private static final WidgetTypeId CENTER = type("flutter.widgets.Center");
    private static final WidgetTypeId SIZED_BOX = type("flutter.widgets.SizedBox");
    private static final WidgetTypeId ASPECT_RATIO =
            type("flutter.widgets.AspectRatio");
    private static final WidgetTypeId CONTAINER = type("flutter.widgets.Container");
    private static final WidgetTypeId OPACITY = type("flutter.widgets.Opacity");
    private static final WidgetTypeId ALIGN = type("flutter.widgets.Align");
    private static final WidgetTypeId FRACTIONALLY_SIZED_BOX =
            type("flutter.widgets.FractionallySizedBox");
    private static final WidgetTypeId FITTED_BOX = type("flutter.widgets.FittedBox");
    private static final WidgetTypeId CONSTRAINED_BOX =
            type("flutter.widgets.ConstrainedBox");
    private static final WidgetTypeId UNCONSTRAINED_BOX =
            type("flutter.widgets.UnconstrainedBox");
    private static final WidgetTypeId LIMITED_BOX = type("flutter.widgets.LimitedBox");
    private static final WidgetTypeId OVERFLOW_BOX = type("flutter.widgets.OverflowBox");
    private static final WidgetTypeId STACK = type("flutter.widgets.Stack");
    private static final WidgetTypeId EXPANDED = type("flutter.widgets.Expanded");
    private static final WidgetTypeId FLEXIBLE = type("flutter.widgets.Flexible");
    private static final WidgetTypeId SPACER = type("flutter.widgets.Spacer");
    private static final WidgetTypeId BASELINE = type("flutter.widgets.Baseline");
    private static final WidgetTypeId INTRINSIC_HEIGHT =
            type("flutter.widgets.IntrinsicHeight");
    private static final WidgetTypeId INTRINSIC_WIDTH =
            type("flutter.widgets.IntrinsicWidth");
    private static final WidgetTypeId OFFSTAGE = type("flutter.widgets.Offstage");
    private static final WidgetTypeId SIZED_OVERFLOW_BOX =
            type("flutter.widgets.SizedOverflowBox");
    private static final WidgetTypeId TRANSFORM = type("flutter.widgets.Transform");
    private static final WidgetTypeId ROTATED_BOX = type("flutter.widgets.RotatedBox");
    private static final WidgetTypeId LIST_BODY = type("flutter.widgets.ListBody");
    private static final WidgetTypeId OVERFLOW_BAR = type("flutter.widgets.OverflowBar");
    private static final WidgetTypeId SAFE_AREA = type("flutter.widgets.SafeArea");
    private static final WidgetTypeId LIST_VIEW = type("flutter.widgets.ListView");
    private static final WidgetTypeId GRID_VIEW = type("flutter.widgets.GridView");
    private static final WidgetTypeId SINGLE_CHILD_SCROLL_VIEW =
            type("flutter.widgets.SingleChildScrollView");
    private static final WidgetTypeId PAGE_VIEW =
            type("flutter.widgets.PageView");
    private static final WidgetTypeId ICON = type("flutter.widgets.Icon");
    private static final WidgetTypeId IMAGE = type("flutter.widgets.Image");
    private static final WidgetTypeId COLORED_BOX = type("flutter.widgets.ColoredBox");
    private static final WidgetTypeId PLACEHOLDER = type("flutter.widgets.Placeholder");
    private static final WidgetTypeId DIRECTIONALITY =
            DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE;
    private static final WidgetTypeId DECORATED_BOX =
            DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE;
    private static final WidgetTypeId EXCLUDE_SEMANTICS =
            ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE;
    private static final WidgetTypeId INDEXED_STACK =
            IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE;
    private static final WidgetTypeId CLIP_RECT =
            ClipRectWidgetPropertySchema.CLIP_RECT_TYPE;
    private static final WidgetTypeId CLIP_OVAL =
            ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE;
    private static final WidgetTypeId CLIP_RRECT =
            ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE;
    private static final WidgetTypeId CLIP_RSUPERELLIPSE =
            ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE;
    private static final WidgetTypeId CLIP_PATH =
            ClipPathWidgetPropertySchema.CLIP_PATH_TYPE;
    private static final SlotName APP_BAR_SLOT = new SlotName("appBar");
    private static final SlotName LEADING = new SlotName("leading");
    private static final SlotName TITLE = new SlotName("title");
    private static final SlotName SUBTITLE = new SlotName("subtitle");
    private static final SlotName SECONDARY = new SlotName("secondary");
    private static final SlotName ACTIONS = new SlotName("actions");
    private static final SlotName FLEXIBLE_SPACE = new SlotName("flexibleSpace");
    private static final SlotName BOTTOM = new SlotName("bottom");
    private static final SlotName BODY = new SlotName("body");
    private static final SlotName FLOATING_ACTION_BUTTON =
            new SlotName("floatingActionButton");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName CHILD = new SlotName("child");
    private static final PropertyName DATA = new PropertyName("data");
    private static final PropertyName PADDING_VALUE = new PropertyName("padding");
    private static final PropertyName ICON_VALUE = new PropertyName("icon");
    private static final PropertyName IMAGE_VALUE = new PropertyName("image");
    private static final PropertyName COLOR_VALUE = new PropertyName("color");
    private static final PropertyName DECORATION_VALUE =
            new PropertyName("decoration");
    private static final PropertyName ENABLED = new PropertyName("enabled");
    private static final PropertyName ASPECT_RATIO_VALUE =
            new PropertyName("aspectRatio");
    private static final PropertyName OPACITY_VALUE = new PropertyName("opacity");
    private static final PropertyName CONSTRAINTS = new PropertyName("constraints");
    private static final PropertyName SIZE = new PropertyName("size");
    private static final PropertyName TRANSFORM_VALUE = new PropertyName("transform");
    private static final PropertyName QUARTER_TURNS = new PropertyName("quarterTurns");
    private static final PropertyName BASELINE_VALUE = new PropertyName("baseline");
    private static final PropertyName BASELINE_TYPE_VALUE =
            new PropertyName("baselineType");
    private static final PropertyName CROSS_AXIS_COUNT =
            new PropertyName("crossAxisCount");
    private static final StableId DOCUMENT_ID = id("14f6c16f-893b-44d0-b809-edbd51bbcdaa");
    private static final StableId ROOT_ID = id("0209809f-351a-4ce7-8c07-1ec625b1e109");
    private static final StableId FIRST_ID = id("710c4ad9-c3cf-434e-af1e-5217ac38aa92");
    private static final StableId NEW_ID = id("805b5a85-397c-4d5f-ae6f-2171456d7a5a");

    private final FlutterDesignerPaletteDropPlanner planner =
            new FlutterDesignerPaletteDropPlanner();

    @Test
    void listTileCompletes6336PlacementsWithFourOptionalDestinationsAndNoCreatedContent() {
        var type = new WidgetTypeId("flutter.material.ListTile");
        var targets = preGestureDetectorDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preGestureDetectorDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(92, preGestureDetectorDefinitions().count()); assertEquals(80, targets.size()); assertEquals(16, wrappers);
        assertEquals(7360, accepted + rejected); assertEquals(6950, accepted); assertEquals(410, rejected);
        var empty = target("Column.children", COLUMN, CHILDREN);
        var planned = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(empty.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        assertTrue(planned.command().widget().properties().isEmpty());
        assertEquals(Set.of(new SlotName("leading"), new SlotName("title"), new SlotName("subtitle"), new SlotName("trailing")), planned.command().widget().slots().keySet());
        planned.command().widget().slots().values().forEach(slot -> assertTrue(((WidgetSlot.SingleSlot) slot).child().isEmpty()));
    }

    @Test
    void pageViewIsAcceptedAsAStaticPaletteSourceAndItsChildrenSlotAcceptsOrderedPages() {
        var columnTarget = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(columnTarget.document(), BUILT_INS, PAGE_VIEW, ROOT_ID,
                        CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID))
                .command().widget();
        assertEquals(PAGE_VIEW, created.type());
        assertTrue(created.slots().get(CHILDREN) instanceof WidgetSlot.ListSlot);
        assertTrue(((WidgetSlot.ListSlot) created.slots().get(CHILDREN)).children().isEmpty());

        var pageTarget = target("PageView.children", PAGE_VIEW, CHILDREN);
        var inserted = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(pageTarget.document(), BUILT_INS, TEXT, ROOT_ID,
                        CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        assertEquals(TEXT, inserted.command().widget().type());
        assertEquals(ROOT_ID, inserted.command().destination().parentId());
        assertEquals(CHILDREN, inserted.command().destination().slotName());
        assertEquals(0, inserted.command().destination().index());
    }

    @Test
    void preferredSizeWrapsAnExistingChildAndRejectsEmptySlotInsertion() {
        var type = type("flutter.widgets.PreferredSize");
        var empty = target("Column.children", COLUMN, CHILDREN);
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(empty.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0,
                        FlutterImageAssetChoices.empty(), () -> NEW_ID));

        var occupied = occupiedTarget("Column.children", COLUMN, CHILDREN);
        var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(occupied.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0,
                        FlutterImageAssetChoices.empty(), () -> NEW_ID)).command();
        assertEquals(FIRST_ID, wrapped.widgetId());
        assertEquals(type, wrapped.wrapper().type());
        assertEquals(CHILD, wrapped.wrapperSlot());
        assertEquals(NEW_ID, wrapped.wrapper().id());
        assertEquals(Map.of(new PropertyName("preferredSize"),
                        new PropertyValue.SizeValue(BigDecimal.valueOf(100), BigDecimal.valueOf(56))),
                wrapped.wrapper().properties());
        assertTrue(((WidgetSlot.SingleSlot) wrapped.wrapper().slots().get(CHILD)).child().isEmpty());
    }

    @Test
    void checkboxListTileCreatesBothConstructorsWithAllThreeOptionalSlots() {
        var target = target("Column.children", COLUMN, CHILDREN);
        var planned = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, CHECKBOX_LIST_TILE, ROOT_ID,
                        CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        var created = planned.command().widget();
        assertEquals(CHECKBOX_LIST_TILE, created.type());
        assertEquals(Map.of(
                new PropertyName("value"), new PropertyValue.BooleanValue(false),
                new PropertyName("onChanged"), new PropertyValue.StringValue("noop"),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")),
                created.properties());
        assertEquals(Map.of(
                TITLE, WidgetSlot.SingleSlot.empty(),
                SUBTITLE, WidgetSlot.SingleSlot.empty(),
                SECONDARY, WidgetSlot.SingleSlot.empty()), created.slots());

        for (var slot : List.of(TITLE, SUBTITLE, SECONDARY)) {
            var slotTarget = target("CheckboxListTile." + slot.value(),
                    CHECKBOX_LIST_TILE, slot);
            var insertion = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(slotTarget.document(), BUILT_INS, TEXT, ROOT_ID,
                            slot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
            assertEquals(TEXT, insertion.command().widget().type());
            assertEquals(slot, insertion.command().destination().slotName());
        }
    }

    @Test
    void expansionTileWrapsTheSelectedWidgetAsRequiredTitleAndAdmitsOtherFourDestinationsIndependently() {
        var type = type("flutter.material.ExpansionTile"); var empty = target("Column.children", COLUMN, CHILDREN);
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(empty.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        var selected = text(FIRST_ID, "Keep my exact Title"); var occupied = new WidgetNode(ROOT_ID, COLUMN, Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(selected))));
        var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(document(occupied), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command();
        assertEquals(FIRST_ID, wrapped.widgetId()); assertEquals(TITLE, wrapped.wrapperSlot()); assertEquals(type, wrapped.wrapper().type());
        assertEquals(NEW_ID, wrapped.wrapper().id()); assertTrue(wrapped.wrapper().properties().isEmpty());
        assertEquals(5, wrapped.wrapper().slots().size()); assertTrue(((WidgetSlot.SingleSlot) wrapped.wrapper().slots().get(TITLE)).child().isEmpty());
        assertTrue(((WidgetSlot.ListSlot) wrapped.wrapper().slots().get(CHILDREN)).children().isEmpty(), "No fake Title or expanded child is created.");
        assertEquals(FIRST_ID, assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(document(selected), BUILT_INS, type, FIRST_ID, () -> NEW_ID)).command().widgetId());
        var prototype = dev.flutter.netbeans.plugin.designer.properties.ExpansionTilePropertyContractTest.prototype();
        for (String destination : List.of("leading", "subtitle", "trailing", "children")) {
            var slot = new SlotName(destination); var slots = new java.util.LinkedHashMap<>(prototype.slots());
            slots.put(slot, destination.equals("children") ? new WidgetSlot.ListSlot(List.of()) : WidgetSlot.SingleSlot.empty());
            var parent = new WidgetNode(ROOT_ID, type, prototype.properties(), slots);
            var inserted = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, slot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
            assertEquals(slot, inserted.command().destination().slotName()); assertEquals(TEXT, inserted.command().widget().type());
            assertEquals(prototype.slots().get(TITLE), parent.slots().get(TITLE));
        }
        var parent = new WidgetNode(ROOT_ID, type, prototype.properties(), prototype.slots());
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, TITLE, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        assertEquals(prototype.slots(), parent.slots());
    }

    @Test
    void radioListTileAddsWithoutAssetsAndBothConstructorsAdmitThreeIndependentSlots() {
        var type = type("flutter.material.RadioListTile"); var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(Map.of(new PropertyName("value"), new PropertyValue.StringValue("option"), new PropertyName("valueType"), new PropertyValue.StringValue("String"), new PropertyName("onChanged"), new PropertyValue.StringValue("noop"),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        assertEquals(Map.of(TITLE, WidgetSlot.SingleSlot.empty(), SUBTITLE, WidgetSlot.SingleSlot.empty(), SECONDARY, WidgetSlot.SingleSlot.empty()), created.slots());
        for (String variant : List.of("standard", "adaptive")) for (var slot : List.of(TITLE, SUBTITLE, SECONDARY)) {
            var properties = new java.util.LinkedHashMap<>(created.properties()); properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            var parent = new WidgetNode(ROOT_ID, type, properties, created.slots());
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, slot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
            assertEquals(slot, added.command().destination().slotName()); assertEquals(TEXT, added.command().widget().type());
            var occupied = new java.util.LinkedHashMap<>(parent.slots()); occupied.put(slot, WidgetSlot.SingleSlot.of(text(FIRST_ID, "Retain me")));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.plan(document(new WidgetNode(ROOT_ID, type, properties, occupied)),
                    BUILT_INS, TEXT, ROOT_ID, slot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void switchListTileAddsWithoutAssetsAndBothConstructorsAdmitThreeIndependentSlots() {
        var type = type("flutter.material.SwitchListTile"); var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(Map.of(new PropertyName("value"), new PropertyValue.BooleanValue(false), new PropertyName("onChanged"), new PropertyValue.StringValue("noop"),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        assertEquals(Map.of(TITLE, WidgetSlot.SingleSlot.empty(), SUBTITLE, WidgetSlot.SingleSlot.empty(), SECONDARY, WidgetSlot.SingleSlot.empty()), created.slots());
        for (String variant : List.of("standard", "adaptive")) for (var slot : List.of(TITLE, SUBTITLE, SECONDARY)) {
            var properties = new java.util.LinkedHashMap<>(created.properties()); properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            var parent = new WidgetNode(ROOT_ID, type, properties, created.slots());
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, slot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
            assertEquals(slot, added.command().destination().slotName()); assertEquals(TEXT, added.command().widget().type());
            var occupied = new java.util.LinkedHashMap<>(parent.slots()); occupied.put(slot, WidgetSlot.SingleSlot.of(text(FIRST_ID, "Retain me")));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.plan(document(new WidgetNode(ROOT_ID, type, properties, occupied)),
                    BUILT_INS, TEXT, ROOT_ID, slot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }



    @Test
    void radioGroupCompletes5916CellMatrixAndRequiresAnExistingChild() {
        var type = new WidgetTypeId("flutter.widgets.RadioGroup");
        var targets = preListTileDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preListTileDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(90, preListTileDefinitions().count()); assertEquals(73, targets.size()); assertEquals(16, wrappers);
        assertEquals(6570, accepted + rejected); assertEquals(6185, accepted); assertEquals(385, rejected);
        var empty = target("Column.children", COLUMN, CHILDREN);
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(empty.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        var occupied = occupiedTarget("Column.children", COLUMN, CHILDREN);
        var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(occupied.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command();
        assertEquals(type, wrapped.wrapper().type()); assertEquals(CHILD, wrapped.wrapperSlot());
        assertEquals(Map.of(new PropertyName("valueType"), new PropertyValue.StringValue("String"),
                new PropertyName("onChanged"), new PropertyValue.StringValue("noop")), wrapped.wrapper().properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), wrapped.wrapper().slots());
    }



    @Test
    void radioCompletes5848CellMatrixWithTypedValueAndCreationCallback() {
        var type = new WidgetTypeId("flutter.material.Radio");
        var targets = preRadioGroupDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preRadioGroupDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(89, preRadioGroupDefinitions().count()); assertEquals(73, targets.size()); assertEquals(15, wrappers);
        assertEquals(6497, accepted + rejected); assertEquals(6114, accepted); assertEquals(383, rejected);
        var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type()); assertTrue(created.slots().isEmpty());
        assertEquals(Map.of(new PropertyName("value"), new PropertyValue.StringValue("option"), new PropertyName("valueType"), new PropertyValue.StringValue("String"), new PropertyName("variant"), new PropertyValue.StringValue("standard"), new PropertyName("onChanged"), new PropertyValue.StringValue("noop")), created.properties());
        {
            var sliderWidget = new WidgetNode(ROOT_ID, type, Map.of(new PropertyName("value"), new PropertyValue.StringValue("option"), new PropertyName("valueType"), new PropertyValue.StringValue("String"), new PropertyName("variant"), new PropertyValue.StringValue("standard"), new PropertyName("onChanged"), new PropertyValue.StringValue("noop")), Map.of());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(sliderWidget), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void rangeSliderCompletes5780CellMatrixWithExactControlledEndpoints() {
        var type = new WidgetTypeId("flutter.material.RangeSlider");
        var targets = preRadioDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preRadioDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(88, preRadioDefinitions().count()); assertEquals(73, targets.size()); assertEquals(15, wrappers);
        assertEquals(6424, accepted + rejected); assertEquals(6043, accepted); assertEquals(381, rejected);
        var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type()); assertTrue(created.slots().isEmpty());
        assertEquals(Map.of(new PropertyName("valuesStart"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), ENABLED, new PropertyValue.BooleanValue(true),
                new PropertyName("valuesEnd"), new PropertyValue.IntegerValue(java.math.BigInteger.ONE)), created.properties());
        {
            var sliderWidget = new WidgetNode(ROOT_ID, type, Map.of(new PropertyName("valuesStart"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("valuesEnd"), new PropertyValue.IntegerValue(java.math.BigInteger.ONE)), Map.of());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(sliderWidget), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void sliderWidgetCompletes5712CellMatrixAsLeafWithThreeRequiredDefaults() {
        var type = new WidgetTypeId("flutter.material.Slider");
        var targets = preRangeSliderDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preRangeSliderDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(87, preRangeSliderDefinitions().count()); assertEquals(73, targets.size()); assertEquals(15, wrappers);
        assertEquals(6351, accepted + rejected); assertEquals(5972, accepted); assertEquals(379, rejected);
        var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type()); assertTrue(created.slots().isEmpty());
        assertEquals(Map.of(new PropertyName("value"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), ENABLED, new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        for (String variant : List.of("standard", "adaptive")) {
            var sliderWidget = new WidgetNode(ROOT_ID, type, Map.of(new PropertyName("value"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO), ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue(variant)), Map.of());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(sliderWidget), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void switchWidgetCompletes5644CellMatrixAsLeafWithThreeRequiredDefaults() {
        var type = new WidgetTypeId("flutter.material.Switch");
        var targets = preSliderDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preSliderDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(86, preSliderDefinitions().count()); assertEquals(73, targets.size()); assertEquals(15, wrappers);
        assertEquals(6278, accepted + rejected); assertEquals(5901, accepted); assertEquals(377, rejected);
        var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type()); assertTrue(created.slots().isEmpty());
        assertEquals(Map.of(new PropertyName("value"), new PropertyValue.BooleanValue(false), ENABLED, new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        for (String variant : List.of("standard", "adaptive")) {
            var switchWidget = new WidgetNode(ROOT_ID, type, Map.of(new PropertyName("value"), new PropertyValue.BooleanValue(false), ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue(variant)), Map.of());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(switchWidget), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void checkboxCompletes5576CellMatrixAsLeafWithThreeRequiredDefaults() {
        var type = new WidgetTypeId("flutter.material.Checkbox");
        var targets = preSwitchDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preSwitchDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(85, preSwitchDefinitions().count()); assertEquals(73, targets.size()); assertEquals(15, wrappers);
        assertEquals(6205, accepted + rejected); assertEquals(5830, accepted); assertEquals(375, rejected);
        var target = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type()); assertTrue(created.slots().isEmpty());
        assertEquals(Map.of(new PropertyName("value"), new PropertyValue.BooleanValue(false), ENABLED, new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        for (String variant : List.of("standard", "adaptive")) {
            var checkbox = new WidgetNode(ROOT_ID, type, Map.of(new PropertyName("value"), new PropertyValue.BooleanValue(false), ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue(variant)), Map.of());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(checkbox), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void iconButtonCompletes5508CellMatrixUsingItsRequiredIconAndOptionalSelectedIcon() {
        var type = new WidgetTypeId("flutter.material.IconButton");
        var targets = preCheckboxDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preCheckboxDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(84, preCheckboxDefinitions().count()); assertEquals(73, targets.size()); assertEquals(15, wrappers);
        assertEquals(6132, accepted + rejected); assertEquals(5759, accepted); assertEquals(373, rejected);
        var iconSlot = new SlotName("icon");
        var selectedIconSlot = new SlotName("selectedIcon");
        for (String variant : List.of("standard", "filled", "filledTonal", "outlined")) {
            var requiredIcon = text(FIRST_ID, "Any Widget is a valid icon");
            for (PropertyValue selected : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true))) {
                var parent = new WidgetNode(ROOT_ID, type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                        new PropertyName("variant"), new PropertyValue.StringValue(variant), new PropertyName("isSelected"), selected),
                        Map.of(iconSlot, WidgetSlot.SingleSlot.of(requiredIcon), selectedIconSlot, WidgetSlot.SingleSlot.empty()));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, selectedIconSlot, 0, choices, () -> NEW_ID));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, choices, () -> NEW_ID));
            }
        }
    }

    @Test
    void floatingActionButtonCompletes5360CellMatrixWithNormalCreationAndBothConditionalSlots() {
        var type = new WidgetTypeId("flutter.material.FloatingActionButton");
        var targets = preIconButtonDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preIconButtonDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(83, preIconButtonDefinitions().count()); assertEquals(72, targets.size()); assertEquals(14, wrappers);
        assertEquals(5976, accepted + rejected); assertEquals(5608, accepted); assertEquals(368, rejected);
        var emptyList = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(emptyList.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type());
        assertEquals(Map.of(ENABLED, new PropertyValue.BooleanValue(true), new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        var iconSlot = new SlotName("icon");
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(), iconSlot, WidgetSlot.SingleSlot.empty()), created.slots());
        for (String variant : List.of("standard", "small", "large", "extended")) {
            boolean icon = variant.equals("extended");
            var slots = Map.<SlotName, WidgetSlot>of(CHILD, icon ? WidgetSlot.SingleSlot.of(text(FIRST_ID, "Label")) : WidgetSlot.SingleSlot.empty(),
                    iconSlot, WidgetSlot.SingleSlot.empty());
            var parent = new WidgetNode(ROOT_ID, type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue(variant)), slots);
            var result = planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (icon) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result);
            else {
                assertTrue(assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result).reason().toLowerCase(java.util.Locale.ROOT).contains("icon"));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
            }
        }
    }

    @Test
    void filledButtonCompletes5135CellMatrixWithNormalCreationAndBothConditionalSlots() {
        var type = new WidgetTypeId("flutter.material.FilledButton");
        var targets = preFloatingActionButtonDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preFloatingActionButtonDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(82, preFloatingActionButtonDefinitions().count()); assertEquals(70, targets.size()); assertEquals(14, wrappers);
        assertEquals(5740, accepted + rejected); assertEquals(5380, accepted); assertEquals(360, rejected);
        var emptyList = target("Column.children", COLUMN, CHILDREN);
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(emptyList.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command().widget();
        assertEquals(type, created.type());
        assertEquals(Map.of(ENABLED, new PropertyValue.BooleanValue(true), new PropertyName("variant"), new PropertyValue.StringValue("standard")), created.properties());
        var iconSlot = new SlotName("icon");
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(), iconSlot, WidgetSlot.SingleSlot.empty()), created.slots());
        for (String variant : List.of("standard", "tonal", "icon", "tonalIcon")) {
            boolean icon = variant.equals("icon") || variant.equals("tonalIcon");
            var slots = Map.<SlotName, WidgetSlot>of(CHILD, icon ? WidgetSlot.SingleSlot.of(text(FIRST_ID, "Label")) : WidgetSlot.SingleSlot.empty(),
                    iconSlot, WidgetSlot.SingleSlot.empty());
            var parent = new WidgetNode(ROOT_ID, type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue(variant)), slots);
            var result = planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (icon) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result);
            else {
                assertTrue(assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result).reason().contains("icon"));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(parent), BUILT_INS, TEXT, ROOT_ID, CHILD, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
            }
        }
    }

    @Test
    void outlinedButtonCompletes4914CellMatrixAndBothConstructorSlotAdmissions() {
        var type = new WidgetTypeId("flutter.material.OutlinedButton");
        var targets = preFilledButtonDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preFilledButtonDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(81, preFilledButtonDefinitions().count()); assertEquals(68, targets.size()); assertEquals(14, wrappers);
        assertEquals(5508, accepted + rejected); assertEquals(5156, accepted); assertEquals(352, rejected);
        var iconSlot = new SlotName("icon"); var iconParent = prototype(type);
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(iconParent), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        var standard = new WidgetNode(ROOT_ID, type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), iconParent.slots());
        var denied = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(standard), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        assertTrue(denied.reason().contains("icon"), denied::reason);
        var target = occupiedTarget("Column.children", COLUMN, CHILDREN);
        var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command();
        assertEquals(Map.of(ENABLED, new PropertyValue.BooleanValue(true), new PropertyName("variant"), new PropertyValue.StringValue("standard")), wrapped.wrapper().properties());
        assertEquals(FIRST_ID, wrapped.widgetId()); assertEquals(CHILD, wrapped.wrapperSlot());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(), iconSlot, WidgetSlot.SingleSlot.empty()), wrapped.wrapper().slots());
    }

    @Test
    void textButtonCompletes4774CellMatrixAndBothConstructorSlotAdmissions() {
        var type = new WidgetTypeId("flutter.material.TextButton");
        var targets = preOutlinedButtonDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preOutlinedButtonDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(80, preOutlinedButtonDefinitions().count()); assertEquals(67, targets.size()); assertEquals(13, wrappers);
        assertEquals(5360, accepted + rejected); assertEquals(5013, accepted); assertEquals(347, rejected);
        var iconSlot = new SlotName("icon"); var iconParent = prototype(type);
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(iconParent), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        var standard = new WidgetNode(ROOT_ID, type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), iconParent.slots());
        var denied = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(standard), BUILT_INS, TEXT, ROOT_ID, iconSlot, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        assertTrue(denied.reason().contains("icon"), denied::reason);
        var target = occupiedTarget("Column.children", COLUMN, CHILDREN);
        var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(target.document(), BUILT_INS, type, ROOT_ID, CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID)).command();
        assertEquals(Map.of(ENABLED, new PropertyValue.BooleanValue(true), new PropertyName("variant"), new PropertyValue.StringValue("standard")), wrapped.wrapper().properties());
        assertEquals(FIRST_ID, wrapped.widgetId()); assertEquals(CHILD, wrapped.wrapperSlot());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(), iconSlot, WidgetSlot.SingleSlot.empty()), wrapped.wrapper().slots());
    }

    @Test
    void refreshIndicatorCompletes4636CellMatrixAndWrapsExistingChildrenWithoutAssets() {
        var type = new WidgetTypeId("flutter.material.RefreshIndicator");
        var targets = preTextButtonDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0, wrappers = 0;
        for (var definition : preTextButtonDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            if (wrapper) wrappers++;
            for (var target : targets) {
                var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
                var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
                if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
            }
        }
        assertEquals(79, preTextButtonDefinitions().toList().size()); assertEquals(66, targets.size()); assertEquals(12, wrappers);
        assertEquals(10, preTextButtonDefinitions().filter(d ->
                dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(d) == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD)
                .filter(d -> !List.of("flutter.widgets.Expanded", "flutter.widgets.Flexible").contains(d.typeId().value())).count());
        assertEquals(5214, accepted + rejected); assertEquals(4872, accepted); assertEquals(342, rejected);
        for (var target : targets) {
            var destination = occupiedTarget(target.name(), target.document().root().type(), target.slot());
            var result = planner.plan(destination.document(), BUILT_INS, type, ROOT_ID, target.slot(), 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
            else {
                var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class, result).command();
                assertEquals(FIRST_ID, command.widgetId()); assertEquals(CHILD, command.wrapperSlot());
                assertEquals(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material")), command.wrapper().properties());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), command.wrapper().slots());
            }
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(target.document(), BUILT_INS, type, ROOT_ID, target.slot(), 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));
        }
    }

    @Test
    void refreshProgressCompletes4575CellMatrixAndCreatesWithoutDefaultsOrAssets() {
        var type = new WidgetTypeId("flutter.material.RefreshProgressIndicator");
        var targets = preRefreshIndicatorDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0;
        for (var definition : preRefreshIndicatorDefinitions().toList()) for (var target : targets) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
            var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
            if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
        }
        assertEquals(78, preRefreshIndicatorDefinitions().count()); assertEquals(66, targets.size());
        assertEquals(5148, accepted + rejected); assertEquals(4808, accepted); assertEquals(340, rejected);
        for (var target : targets) {
            var result = planner.plan(target.document(), BUILT_INS, type, ROOT_ID, target.slot(), 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
            else { var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result).command().widget(); assertTrue(created.properties().isEmpty()); assertTrue(created.slots().isEmpty()); }
        }
    }

    @Test
    void circularProgressCompletes4514CellMatrixAndCreatesOnlyRequiredSelectorWithoutAssets() {
        var type = new WidgetTypeId("flutter.material.CircularProgressIndicator");
        var targets = preRefreshProgressIndicatorDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0;
        for (var definition : preRefreshProgressIndicatorDefinitions().toList()) for (var target : targets) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
            var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
            if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
        }
        assertEquals(77, preRefreshProgressIndicatorDefinitions().count()); assertEquals(66, targets.size());
        assertEquals(5082, accepted + rejected); assertEquals(4744, accepted); assertEquals(338, rejected);
        for (var target : targets) {
            var result = planner.plan(target.document(), BUILT_INS, type, ROOT_ID, target.slot(), 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
            else { var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result).command().widget(); assertEquals(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material")), created.properties()); assertTrue(created.slots().isEmpty()); }
        }
    }

    @Test
    void linearProgressCompletes4453CellMatrixAndCreatesWithoutAssetsOrDefaults() {
        var type = new WidgetTypeId("flutter.material.LinearProgressIndicator");
        var targets = preCircularProgressIndicatorDefinitions().flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0;
        for (var definition : preCircularProgressIndicatorDefinitions().toList()) for (var target : targets) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
            var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
            if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++; else accepted++;
        }
        assertEquals(76, preCircularProgressIndicatorDefinitions().count()); assertEquals(66, targets.size());
        assertEquals(5016, accepted + rejected); assertEquals(4680, accepted); assertEquals(336, rejected);
        for (var target : targets) {
            var result = planner.plan(target.document(), BUILT_INS, type, ROOT_ID, target.slot(), 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
            else { var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result).command().widget(); assertTrue(created.properties().isEmpty()); assertTrue(created.slots().isEmpty()); }
        }
    }

    @Test
    void circleAvatarCompletes4392CellMatrixAndCreatesWithoutAssetsOrDefaults() {
        var avatar = new WidgetTypeId("flutter.material.CircleAvatar");
        var targets = preLinearProgressIndicatorDefinitions().flatMap(definition -> definition.slots().stream()
                .filter(slot -> slot.minChildren() == 0).map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0;
        for (var definition : preLinearProgressIndicatorDefinitions().toList()) for (var target : targets) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
            var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
            if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++;
            else { accepted++; assertTrue(result instanceof FlutterDesignerPaletteDropPlanner.Accepted || result instanceof FlutterDesignerPaletteDropPlanner.Wrapped); }
        }
        assertEquals(75, preLinearProgressIndicatorDefinitions().count()); assertEquals(66, targets.size());
        assertEquals(4950, accepted + rejected); assertEquals(4616, accepted); assertEquals(334, rejected);
        for (var target : targets) {
            var result = planner.plan(target.document(), BUILT_INS, avatar, ROOT_ID, target.slot(), 0, FlutterImageAssetChoices.empty(), () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
            } else {
                var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result).command().widget();
                assertTrue(created.properties().isEmpty());
                assertTrue(((WidgetSlot.SingleSlot) created.slots().get(CHILD)).child().isEmpty());
            }
        }
    }

    @Test
    void badgeCompletes4260CellMatrixAndCountModeRejectsLabelBeforeAllocatingIds() {
        var badge = new WidgetTypeId("flutter.material.Badge");
        var targets = preCircleAvatarDefinitions().flatMap(definition -> definition.slots().stream()
                .filter(slot -> slot.minChildren() == 0).map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        var choices = new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int accepted = 0, rejected = 0;
        for (var definition : preCircleAvatarDefinitions().toList()) for (var target : targets) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            var destination = wrapper ? occupiedTarget(target.name(), target.document().root().type(), target.slot()) : target;
            var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
            if (result instanceof FlutterDesignerPaletteDropPlanner.Rejected) rejected++;
            else { accepted++; assertTrue(result instanceof FlutterDesignerPaletteDropPlanner.Accepted || result instanceof FlutterDesignerPaletteDropPlanner.Wrapped); }
        }
        assertEquals(74, preCircleAvatarDefinitions().count()); assertEquals(65, targets.size());
        assertEquals(4810, accepted + rejected); assertEquals(4481, accepted); assertEquals(329, rejected);
        var seed = target("Badge.label", badge, new SlotName("label"));
        WidgetNode owner = seed.document().root();
        var countOwner = new WidgetNode(owner.id(), owner.type(), Map.of(new PropertyName("count"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)), owner.slots());
        var countDocument = new DesignerDocument(seed.document().documentId(), seed.document().source(), countOwner);
        AtomicInteger allocations = new AtomicInteger();
        for (var definition : BUILT_INS.definitions()) {
            var result = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(countDocument, BUILT_INS, definition.typeId(), ROOT_ID, new SlotName("label"), 0, choices, () -> { allocations.incrementAndGet(); return NEW_ID; }));
            assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_REJECTS_WIDGET, result.code());
            assertTrue(result.reason().contains("Cannot add")); assertTrue(result.reason().contains(ROOT_ID.toString()));
            assertTrue(result.reason().contains("Clear Count"));
        }
        assertEquals(0, allocations.get());
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(countDocument, BUILT_INS, badge, ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
    }

    @Test
    void cardCompletes4060CellMatrixIncludingOptionalChildAndExistingWrapperRoutes() {
        WidgetTypeId card = new WidgetTypeId("flutter.material.Card");
        List<MatrixTargetCase> targets = preBadgeDefinitions()
                .flatMap(definition -> definition.slots().stream().filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(), definition.typeId(), slot.name()))).toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(List.of(
                new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")), Optional.empty());
        int sourceAccepted = 0, sourceRejected = 0, destinationAccepted = 0, destinationRejected = 0;
        for (var target : targets) {
            var result = planner.plan(target.document(), BUILT_INS, card, ROOT_ID, target.slot(), 0, choices, () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result); sourceRejected++;
            } else {
                var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result);
                assertEquals(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("elevated")), added.command().widget().properties());
                assertTrue(((WidgetSlot.SingleSlot) added.command().widget().slots().get(CHILD)).child().isEmpty());
                sourceAccepted++;
            }
        }
        var empty = target("Card.child", card, CHILD);
        var occupied = occupiedTarget("Card.child", card, CHILD);
        for (var definition : preBadgeDefinitions().toList()) {
            boolean wrapper = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.creationMode(definition)
                    == dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD;
            var destination = wrapper ? occupied : empty;
            var result = planner.plan(destination.document(), BUILT_INS, definition.typeId(), ROOT_ID, CHILD, 0, choices, () -> NEW_ID);
            if (List.of("flutter.widgets.Expanded", "flutter.widgets.Flexible", "flutter.widgets.Spacer").contains(definition.typeId().value())) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result); destinationRejected++;
            } else {
                if (wrapper) {
                    var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class, result);
                    assertEquals(FIRST_ID, wrapped.command().widgetId());
                } else assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result);
                destinationAccepted++;
            }
        }
        assertEquals(73, preBadgeDefinitions().count()); assertEquals(63, targets.size());
        assertEquals(61, sourceAccepted); assertEquals(2, sourceRejected);
        assertEquals(70, destinationAccepted); assertEquals(3, destinationRejected);
        assertEquals(4410, 70 * targets.size());
        assertEquals(3768, 3638 + sourceAccepted + destinationAccepted - 1);
        assertEquals(300, 295 + sourceRejected + destinationRejected);
    }

    @Test
    void verticalDividerCompletesExact3933CellModelWithoutInventedDefaults() {
        List<MatrixTargetCase> targets = preCardDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(),
                                definition.typeId(), slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.material.VerticalDivider"), ROOT_ID,
                    target.slot(), 0, FlutterImageAssetChoices.empty(), () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        target.name());
                assertEquals(Map.of(),
                        success.command().widget().properties());
                assertEquals(Map.of(), success.command().widget().slots());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(72, Math.toIntExact(preCardDefinitions().count())),
                () -> assertEquals(62, targets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(4340, 70 * targets.size()),
                () -> assertEquals(3643, 3583 + accepted.get()),
                () -> assertEquals(295, 293 + rejected.get()));
    }

    @Test
    void dividerCompletesExact3876CellModelWithoutInventedDefaults() {
        List<MatrixTargetCase> targets = preVerticalDividerDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(),
                                definition.typeId(), slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.material.Divider"), ROOT_ID,
                    target.slot(), 0, FlutterImageAssetChoices.empty(), () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        target.name());
                assertEquals(Map.of(),
                        success.command().widget().properties());
                assertEquals(Map.of(), success.command().widget().slots());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(71, Math.toIntExact(preVerticalDividerDefinitions().count())),
                () -> assertEquals(62, targets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(4278, 69 * targets.size()),
                () -> assertEquals(3588, 3528 + accepted.get()),
                () -> assertEquals(293, 291 + rejected.get()));
    }

    @Test
    void imageIconCompletesExact3819CellModelWithExplicitNone() {
        List<MatrixTargetCase> targets = preCardDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(definition.palette().displayName() + "." + slot.name().value(),
                                definition.typeId(), slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.ImageIcon"), ROOT_ID,
                    target.slot(), 0, FlutterImageAssetChoices.empty(), () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        target.name());
                assertEquals(Map.of(IMAGE_VALUE, new PropertyValue.NullValue()),
                        success.command().widget().properties());
                assertEquals(Map.of(), success.command().widget().slots());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(62, targets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(4216, 68 * targets.size()),
                () -> assertEquals(3533, 3473 + accepted.get()),
                () -> assertEquals(291, 289 + rejected.get()));
    }

    @Test
    void iconThemeCompletesExact3762CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preImageIconDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.IconTheme"), ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "IconTheme -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "IconTheme -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(new WidgetTypeId("flutter.widgets.IconTheme"), command.wrapper().type());
                assertEquals(Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), command.wrapper().properties());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(69, Math.toIntExact(preImageIconDefinitions().count())),
                () -> assertEquals(62, optionalTargets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(4154, 67 * optionalTargets.size()),
                () -> assertEquals(3478, 3418 + accepted.get()),
                () -> assertEquals(289, 287 + rejected.get()));
    }

    @Test
    void iconThemeNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.IconTheme"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.IconTheme"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.IconTheme"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.IconTheme"), wrapped.command().wrapper().type());
        assertEquals(Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), wrapped.command().wrapper().properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void defaultSelectionStyleCompletesExact3705CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preIconThemeDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"), ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "DefaultSelectionStyle -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "DefaultSelectionStyle -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"), command.wrapper().type());
                assertEquals(Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), command.wrapper().properties());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(68, Math.toIntExact(preIconThemeDefinitions().count())),
                () -> assertEquals(62, optionalTargets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(4092, 66 * optionalTargets.size()),
                () -> assertEquals(3423, 3363 + accepted.get()),
                () -> assertEquals(287, 285 + rejected.get()));
    }

    @Test
    void defaultSelectionStyleNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"), wrapped.command().wrapper().type());
        assertEquals(Map.of(new PropertyName("merge"), new PropertyValue.BooleanValue(false)), wrapped.command().wrapper().properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void defaultTextHeightBehaviorCompletesExact3648CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preDefaultSelectionStyleDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"), ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "DefaultTextHeightBehavior -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "DefaultTextHeightBehavior -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"), command.wrapper().type());
                assertTrue(command.wrapper().properties().isEmpty());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(67, Math.toIntExact(preDefaultSelectionStyleDefinitions().count())),
                () -> assertEquals(62, optionalTargets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(4030, 65 * optionalTargets.size()),
                () -> assertEquals(3368, 3308 + accepted.get()),
                () -> assertEquals(285, 283 + rejected.get()));
    }

    @Test
    void defaultTextHeightBehaviorNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"), wrapped.command().wrapper().type());
        assertTrue(wrapped.command().wrapper().properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void tickerModeCompletesExact3591CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preDefaultTextHeightBehaviorDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.TickerMode"), ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "TickerMode -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "TickerMode -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(new WidgetTypeId("flutter.widgets.TickerMode"), command.wrapper().type());
                assertEquals(Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true)), command.wrapper().properties());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(66, Math.toIntExact(preDefaultTextHeightBehaviorDefinitions().count())),
                () -> assertEquals(62, optionalTargets.size()),
                () -> assertEquals(60, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(3968, 64 * optionalTargets.size()),
                () -> assertEquals(3313, 3253 + accepted.get()),
                () -> assertEquals(283, 281 + rejected.get()));
    }

    @Test
    void tickerModeNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.TickerMode"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.TickerMode"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.TickerMode"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.TickerMode"), wrapped.command().wrapper().type());
        assertEquals(Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true)), wrapped.command().wrapper().properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void visibilityNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.Visibility"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.Visibility"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.Visibility"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.Visibility"), wrapped.command().wrapper().type());
        assertTrue(wrapped.command().wrapper().properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(), new SlotName("replacement"), WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void visibilityCompletes3534CellMatrixAsRequiredWrapperAndOptionalReplacementDestination() {
        WidgetTypeId visibility = new WidgetTypeId("flutter.widgets.Visibility");
        SlotName replacement = new SlotName("replacement");
        List<MatrixTargetCase> targets = preTickerModeDefinitions()
                .flatMap(parent -> parent.slots().stream().filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(parent.palette().displayName() + "." + slot.name().value(),
                                parent.typeId(), slot.name())))
                .toList();
        int sourceAccepted = 0;
        int sourceRejected = 0;
        for (var target : targets) {
            var result = planner.plan(target.document(), BUILT_INS, visibility,
                    ROOT_ID, target.slot(), 0, () -> NEW_ID);
            if (target.name().equals("Scaffold.appBar") || target.name().equals("AppBar.bottom")) {
                assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_REJECTS_WIDGET,
                        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result).code());
                sourceRejected++;
            } else {
                WrapWidget command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class, result,
                        target.name()).command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(visibility, command.wrapper().type());
                assertEquals(Map.of(), command.wrapper().properties());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(),
                        replacement, WidgetSlot.SingleSlot.empty()), command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                sourceAccepted++;
            }
        }
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(List.of(
                new FlutterImageAssetChoices.Choice(Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        int targetAccepted = 0;
        int targetRejected = 0;
        for (WidgetDefinition source : preVisibilityDefinitions().toList()) {
            boolean required = source.slot(CHILD).filter(slot -> slot.minChildren() > 0).isPresent();
            MatrixTargetCase target = required
                    ? occupiedTarget("Visibility.replacement", visibility, replacement)
                    : target("Visibility.replacement", visibility, replacement);
            var result = planner.plan(target.document(), BUILT_INS, source.typeId(),
                    ROOT_ID, replacement, 0, choices, () -> NEW_ID);
            if (List.of(EXPANDED, FLEXIBLE, SPACER).contains(source.typeId())) {
                assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_REJECTS_WIDGET,
                        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result).code());
                targetRejected++;
            } else {
                if (required) {
                    var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class, result,
                            source.typeId().value()).command();
                    assertEquals(FIRST_ID, command.widgetId());
                    assertEquals(source.typeId(), command.wrapper().type());
                } else {
                    var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, result,
                            source.typeId().value()).command();
                    assertEquals(source.typeId(), command.widget().type());
                }
                targetAccepted++;
            }
        }
        assertEquals(65, Math.toIntExact(preTickerModeDefinitions().count()));
        assertEquals(62, targets.size());
        assertEquals(60, sourceAccepted);
        assertEquals(2, sourceRejected);
        assertEquals(61, targetAccepted);
        assertEquals(3, targetRejected);
        assertEquals(3906, 63 * targets.size());
        assertEquals(3261, 3140 + sourceAccepted + targetAccepted);
        assertEquals(281, 276 + sourceRejected + targetRejected);
    }

    @Test
    void plansTextIntoCatalogAcceptedListAndEmptySingleSlots() {
        List<AcceptedCase> cases = List.of(
                new AcceptedCase(
                        "empty Column",
                        document(parent(COLUMN, List.of())),
                        CHILDREN,
                        0),
                new AcceptedCase(
                        "non-empty Row",
                        document(parent(ROW, List.of(text(FIRST_ID, "existing")))),
                        CHILDREN,
                        1),
                new AcceptedCase(
                        "non-empty Wrap",
                        document(parent(WRAP, List.of(text(FIRST_ID, "existing")))),
                        CHILDREN,
                        1),
                new AcceptedCase(
                        "absent optional list slot",
                        document(WidgetNode.empty(ROOT_ID, COLUMN)),
                        CHILDREN,
                        0),
                new AcceptedCase(
                        "absent optional Center child",
                        document(WidgetNode.empty(ROOT_ID, CENTER)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Center child",
                        document(singleParent(CENTER, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional SizedBox child",
                        document(WidgetNode.empty(ROOT_ID, SIZED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty SizedBox child",
                        document(singleParent(SIZED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional AspectRatio child",
                        document(prototype(ASPECT_RATIO)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty AspectRatio child",
                        document(singleParent(ASPECT_RATIO, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional Container child",
                        document(WidgetNode.empty(ROOT_ID, CONTAINER)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Container child",
                        document(singleParent(CONTAINER, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional Opacity child",
                        document(prototype(OPACITY)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Opacity child",
                        document(singleParent(OPACITY, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional Align child",
                        document(prototype(ALIGN)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Align child",
                        document(singleParent(ALIGN, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional FractionallySizedBox child",
                        document(prototype(FRACTIONALLY_SIZED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty FractionallySizedBox child",
                        document(singleParent(FRACTIONALLY_SIZED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional FittedBox child",
                        document(prototype(FITTED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty FittedBox child",
                        document(singleParent(FITTED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional ConstrainedBox child",
                        document(prototype(CONSTRAINED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty ConstrainedBox child",
                        document(singleParent(CONSTRAINED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional UnconstrainedBox child",
                        document(prototype(UNCONSTRAINED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty UnconstrainedBox child",
                        document(singleParent(UNCONSTRAINED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional LimitedBox child",
                        document(prototype(LIMITED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty LimitedBox child",
                        document(singleParent(LIMITED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional OverflowBox child",
                        document(prototype(OVERFLOW_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty OverflowBox child",
                        document(singleParent(OVERFLOW_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional Baseline child",
                        document(prototype(BASELINE)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Baseline child",
                        document(singleParent(BASELINE, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional IntrinsicHeight child",
                        document(prototype(INTRINSIC_HEIGHT)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty IntrinsicHeight child",
                        document(singleParent(INTRINSIC_HEIGHT, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional IntrinsicWidth child",
                        document(prototype(INTRINSIC_WIDTH)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty IntrinsicWidth child",
                        document(singleParent(INTRINSIC_WIDTH, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional Offstage child",
                        document(prototype(OFFSTAGE)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Offstage child",
                        document(singleParent(OFFSTAGE, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional SizedOverflowBox child",
                        document(prototype(SIZED_OVERFLOW_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty SizedOverflowBox child",
                        document(singleParent(SIZED_OVERFLOW_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional Transform child",
                        document(prototype(TRANSFORM)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty Transform child",
                        document(singleParent(TRANSFORM, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional RotatedBox child",
                        document(prototype(ROTATED_BOX)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional ClipRect child",
                        document(prototype(CLIP_RECT)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional ClipOval child",
                        document(prototype(CLIP_OVAL)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "absent optional ClipRRect child",
                        document(prototype(CLIP_RRECT)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "explicitly empty RotatedBox child",
                        document(singleParent(ROTATED_BOX, null)),
                        CHILD,
                        0),
                new AcceptedCase(
                        "empty Stack",
                        document(prototype(STACK)),
                        CHILDREN,
                        0),
                new AcceptedCase(
                        "non-empty Stack terminal front layer",
                        document(parent(
                                STACK, List.of(text(FIRST_ID, "existing back layer")))),
                        CHILDREN,
                        1),
                new AcceptedCase(
                        "empty ListBody",
                        document(prototype(LIST_BODY)),
                        CHILDREN,
                        0),
                new AcceptedCase(
                        "non-empty ListBody terminal constructor order",
                        document(parent(
                                LIST_BODY, List.of(text(FIRST_ID, "existing child")))),
                        CHILDREN,
                        1),
                new AcceptedCase(
                        "empty OverflowBar",
                        document(prototype(OVERFLOW_BAR)),
                        CHILDREN,
                        0),
                new AcceptedCase(
                        "non-empty OverflowBar terminal source order",
                        document(parent(
                                OVERFLOW_BAR, List.of(text(FIRST_ID, "existing child")))),
                        CHILDREN,
                        1),
                new AcceptedCase(
                        "absent optional ElevatedButton child",
                        document(WidgetNodePrototypeFactory.create(
                                definition(ELEVATED_BUTTON), ROOT_ID)),
                        CHILD,
                        0));

        assertAll(cases.stream().map(testCase -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    testCase.document(),
                    BUILT_INS,
                    TEXT,
                    ROOT_ID,
                    testCase.slot(),
                    testCase.index(),
                    () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });

            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class,
                    result,
                    testCase.name());
            AddWidget command = accepted.command();
            assertEquals(ROOT_ID, command.destination().parentId(), testCase.name());
            assertEquals(testCase.slot(), command.destination().slotName(),
                    testCase.name());
            assertEquals(testCase.index(), command.destination().index(), testCase.name());
            assertEquals(NEW_ID, command.widget().id(), testCase.name());
            assertEquals(TEXT, command.widget().type(), testCase.name());
            assertEquals(new PropertyValue.StringValue("Text"),
                    command.widget().properties().get(DATA), testCase.name());
            assertTrue(command.widget().slots().isEmpty(), testCase.name());
            assertEquals(1, allocations.get(), testCase.name());
        }));
    }

    @Test
    void plansAllOneThousandOneHundredTwentyTwoAnyWidgetCompatibilityCellsWithExactPrototypes() {
        List<CoreSourceCase> sources = coreSources();
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                target("Column.children", COLUMN, CHILDREN),
                target("Row.children", ROW, CHILDREN),
                target("Wrap.children", WRAP, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("AspectRatio.child", ASPECT_RATIO, CHILD),
                target("Container.child", CONTAINER, CHILD),
                target("Opacity.child", OPACITY, CHILD),
                target("Align.child", ALIGN, CHILD),
                target("FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                target("FittedBox.child", FITTED_BOX, CHILD),
                target("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                target("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                target("LimitedBox.child", LIMITED_BOX, CHILD),
                target("OverflowBox.child", OVERFLOW_BOX, CHILD),
                target("Baseline.child", BASELINE, CHILD),
                target("IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                target("IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                target("Offstage.child", OFFSTAGE, CHILD),
                target("SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                target("Transform.child", TRANSFORM, CHILD),
                target("RotatedBox.child", ROTATED_BOX, CHILD),
                target("Stack.children", STACK, CHILDREN),
                target("ListBody.children", LIST_BODY, CHILDREN),
                target("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                target("ListView.children", LIST_VIEW, CHILDREN),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE));

        assertEquals(33, sources.size());
        assertEquals(34, targets.size());
        assertAll(sources.stream().flatMap(source -> targets.stream().map(target ->
                (Executable) () -> {
                    AtomicInteger allocations = new AtomicInteger();
                    FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                            target.document(),
                            BUILT_INS,
                            source.type(),
                            ROOT_ID,
                            target.slot(),
                            0,
                            () -> {
                                allocations.incrementAndGet();
                                return NEW_ID;
                            });

                    FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                            FlutterDesignerPaletteDropPlanner.Accepted.class,
                            result,
                            source.name() + " -> " + target.name());
                    AddWidget command = accepted.command();
                    assertEquals(ROOT_ID, command.destination().parentId());
                    assertEquals(target.slot(), command.destination().slotName());
                    assertEquals(0, command.destination().index());
                    assertExactPrototype(source, command.widget());
                    assertEquals(1, allocations.get());
                })));
    }

    @Test
    void admitsOnlyAppBarAcrossBothPreferredSizeTraitSlotsForExact1188CellMatrix() {
        AtomicInteger allocations = new AtomicInteger();
        List<MatrixTargetCase> traitTargets = List.of(
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));
        assertAll(
                () -> assertEquals(36, 34 + traitTargets.size(),
                        "thirty-four any-widget plus two trait destinations"),
                () -> assertEquals(1188, 33 * (34 + traitTargets.size()),
                        "exact compatibility-matrix candidates"),
                () -> assertEquals(1124, 33 * 34 + traitTargets.size(),
                        "1,122 any-widget cells plus two AppBar trait cells"),
                () -> assertEquals(64, traitTargets.size() * (33 - 1),
                        "all non-AppBar trait cells are rejected"));

        assertAll(traitTargets.stream().map(target -> (Executable) () -> {
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(
                            target.document(), BUILT_INS, APP_BAR, ROOT_ID,
                            target.slot(), 0, () -> NEW_ID),
                    "AppBar -> " + target.name());
            assertEquals(APP_BAR, accepted.command().widget().type());
            assertEquals(target.slot(), accepted.command().destination().slotName());
        }));

        assertAll(traitTargets.stream().flatMap(target -> coreSources().stream()
                .filter(source -> !APP_BAR.equals(source.type()))
                .map(source -> (Executable) () -> {
                    FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                            FlutterDesignerPaletteDropPlanner.Rejected.class,
                            planner.plan(
                                    target.document(), BUILT_INS, source.type(), ROOT_ID,
                                    target.slot(), 0, () -> {
                                        allocations.incrementAndGet();
                                        return NEW_ID;
                                    }),
                            source.name() + " -> " + target.name());
                    assertEquals(
                            FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_REJECTS_WIDGET,
                            rejected.code(), source.name());
                })));
        assertEquals(0, allocations.get(),
                "all 64 rejected trait cells must fail before stable-id allocation");
    }

    @Test
    void expandedWrapsOnlyExistingDirectRowOrColumnChildrenForExact1224CellModel() {
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                new MatrixTargetCase(
                        "Column.children",
                        document(parent(COLUMN,
                                List.of(text(FIRST_ID, "column child")))),
                        CHILDREN),
                new MatrixTargetCase(
                        "Row.children",
                        document(parent(ROW,
                                List.of(text(FIRST_ID, "row child")))),
                        CHILDREN),
                target("Wrap.children", WRAP, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("AspectRatio.child", ASPECT_RATIO, CHILD),
                target("Container.child", CONTAINER, CHILD),
                target("Opacity.child", OPACITY, CHILD),
                target("Align.child", ALIGN, CHILD),
                target("FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                target("FittedBox.child", FITTED_BOX, CHILD),
                target("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                target("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                target("LimitedBox.child", LIMITED_BOX, CHILD),
                target("OverflowBox.child", OVERFLOW_BOX, CHILD),
                target("Baseline.child", BASELINE, CHILD),
                target("IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                target("IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                target("Offstage.child", OFFSTAGE, CHILD),
                target("SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                target("Transform.child", TRANSFORM, CHILD),
                target("RotatedBox.child", ROTATED_BOX, CHILD),
                target("Stack.children", STACK, CHILDREN),
                target("ListBody.children", LIST_BODY, CHILDREN),
                target("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                target("ListView.children", LIST_VIEW, CHILDREN),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE),
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, EXPANDED, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Column.children")
                    || target.name().equals("Row.children")) {
                FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        target.name());
                WrapWidget command = wrapped.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(NEW_ID, command.wrapper().id());
                assertEquals(EXPANDED, command.wrapper().type());
                assertTrue(command.wrapper().properties().isEmpty(),
                        "omitted flex preserves Flutter's constructor default of 1");
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertTrue(failure.reason().contains("Expanded"));
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(36, targets.size()),
                () -> assertEquals(2, accepted.get()),
                () -> assertEquals(34, rejected.get()),
                () -> assertEquals(1224, 34 * targets.size()),
                () -> assertEquals(1126, 1124 + accepted.get()),
                () -> assertEquals(98, 64 + rejected.get()));
    }

    @Test
    void imageCompletesExact1260CellModelWithDeclaredAssetCreation() {
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                target("Column.children", COLUMN, CHILDREN),
                target("Row.children", ROW, CHILDREN),
                target("Wrap.children", WRAP, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("AspectRatio.child", ASPECT_RATIO, CHILD),
                target("Container.child", CONTAINER, CHILD),
                target("Opacity.child", OPACITY, CHILD),
                target("Align.child", ALIGN, CHILD),
                target("FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                target("FittedBox.child", FITTED_BOX, CHILD),
                target("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                target("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                target("LimitedBox.child", LIMITED_BOX, CHILD),
                target("OverflowBox.child", OVERFLOW_BOX, CHILD),
                target("Baseline.child", BASELINE, CHILD),
                target("IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                target("IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                target("Offstage.child", OFFSTAGE, CHILD),
                target("SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                target("Transform.child", TRANSFORM, CHILD),
                target("RotatedBox.child", ROTATED_BOX, CHILD),
                target("Stack.children", STACK, CHILDREN),
                target("ListBody.children", LIST_BODY, CHILDREN),
                target("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                target("ListView.children", LIST_VIEW, CHILDREN),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE),
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, IMAGE, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        target.name());
                PropertyValue.ImageProviderValue provider = assertInstanceOf(
                        PropertyValue.ImageProviderValue.class,
                        success.command().widget().properties().get(IMAGE_VALUE));
                assertEquals("assets/matrix.png", provider.assetName());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(36, targets.size()),
                () -> assertEquals(34, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(1260, 35 * targets.size()),
                () -> assertEquals(1160, 1126 + accepted.get()),
                () -> assertEquals(100, 98 + rejected.get()));
    }

    @Test
    void flexibleCompletesExact1296CellModelByWrappingOnlyDirectFlexChildren() {
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                new MatrixTargetCase(
                        "Column.children",
                        document(parent(COLUMN,
                                List.of(text(FIRST_ID, "column child")))),
                        CHILDREN),
                new MatrixTargetCase(
                        "Row.children",
                        document(parent(ROW,
                                List.of(text(FIRST_ID, "row child")))),
                        CHILDREN),
                target("Wrap.children", WRAP, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("AspectRatio.child", ASPECT_RATIO, CHILD),
                target("Container.child", CONTAINER, CHILD),
                target("Opacity.child", OPACITY, CHILD),
                target("Align.child", ALIGN, CHILD),
                target("FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                target("FittedBox.child", FITTED_BOX, CHILD),
                target("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                target("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                target("LimitedBox.child", LIMITED_BOX, CHILD),
                target("OverflowBox.child", OVERFLOW_BOX, CHILD),
                target("Baseline.child", BASELINE, CHILD),
                target("IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                target("IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                target("Offstage.child", OFFSTAGE, CHILD),
                target("SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                target("Transform.child", TRANSFORM, CHILD),
                target("RotatedBox.child", ROTATED_BOX, CHILD),
                target("Stack.children", STACK, CHILDREN),
                target("ListBody.children", LIST_BODY, CHILDREN),
                target("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                target("ListView.children", LIST_VIEW, CHILDREN),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE),
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, FLEXIBLE, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Column.children")
                    || target.name().equals("Row.children")) {
                FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        target.name());
                WrapWidget command = wrapped.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(NEW_ID, command.wrapper().id());
                assertEquals(FLEXIBLE, command.wrapper().type());
                assertTrue(command.wrapper().properties().isEmpty(),
                        "omitted flex and fit preserve Flutter defaults 1 and loose");
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertTrue(failure.reason().contains("Flexible"));
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(36, targets.size()),
                () -> assertEquals(2, accepted.get()),
                () -> assertEquals(34, rejected.get()),
                () -> assertEquals(1296, 36 * targets.size()),
                () -> assertEquals(1162, 1160 + accepted.get()),
                () -> assertEquals(134, 100 + rejected.get()));
    }

    @Test
    void spacerCompletesExact1332CellModelAsATerminalDirectFlexChild() {
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                target("Column.children", COLUMN, CHILDREN),
                target("Row.children", ROW, CHILDREN),
                target("Wrap.children", WRAP, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("AspectRatio.child", ASPECT_RATIO, CHILD),
                target("Container.child", CONTAINER, CHILD),
                target("Opacity.child", OPACITY, CHILD),
                target("Align.child", ALIGN, CHILD),
                target("FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                target("FittedBox.child", FITTED_BOX, CHILD),
                target("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                target("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                target("LimitedBox.child", LIMITED_BOX, CHILD),
                target("OverflowBox.child", OVERFLOW_BOX, CHILD),
                target("Baseline.child", BASELINE, CHILD),
                target("IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                target("IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                target("Offstage.child", OFFSTAGE, CHILD),
                target("SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                target("Transform.child", TRANSFORM, CHILD),
                target("RotatedBox.child", ROTATED_BOX, CHILD),
                target("Stack.children", STACK, CHILDREN),
                target("ListBody.children", LIST_BODY, CHILDREN),
                target("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                target("ListView.children", LIST_VIEW, CHILDREN),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE),
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, SPACER, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Column.children")
                    || target.name().equals("Row.children")) {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        target.name());
                AddWidget command = success.command();
                assertEquals(ROOT_ID, command.destination().parentId());
                assertEquals(CHILDREN, command.destination().slotName());
                assertEquals(0, command.destination().index());
                assertEquals(SPACER, command.widget().type());
                assertTrue(command.widget().properties().isEmpty(),
                        "omitted flex preserves Flutter's positive default of 1");
                assertTrue(command.widget().slots().isEmpty(),
                        "Spacer is terminal and has no child slot");
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertTrue(failure.reason().contains("flutter.widgets.Spacer"));
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(36, targets.size()),
                () -> assertEquals(2, accepted.get()),
                () -> assertEquals(34, rejected.get()),
                () -> assertEquals(1332, 37 * targets.size()),
                () -> assertEquals(1164, 1162 + accepted.get()),
                () -> assertEquals(168, 134 + rejected.get()));
    }

    @Test
    void gridViewCountCompletesExact1406CellModelWithOrderedChildren() {
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                target("Column.children", COLUMN, CHILDREN),
                target("Row.children", ROW, CHILDREN),
                target("Wrap.children", WRAP, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("AspectRatio.child", ASPECT_RATIO, CHILD),
                target("Container.child", CONTAINER, CHILD),
                target("Opacity.child", OPACITY, CHILD),
                target("Align.child", ALIGN, CHILD),
                target("FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                target("FittedBox.child", FITTED_BOX, CHILD),
                target("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                target("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                target("LimitedBox.child", LIMITED_BOX, CHILD),
                target("OverflowBox.child", OVERFLOW_BOX, CHILD),
                target("Baseline.child", BASELINE, CHILD),
                target("IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                target("IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                target("Offstage.child", OFFSTAGE, CHILD),
                target("SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                target("Transform.child", TRANSFORM, CHILD),
                target("RotatedBox.child", ROTATED_BOX, CHILD),
                target("Stack.children", STACK, CHILDREN),
                target("ListBody.children", LIST_BODY, CHILDREN),
                target("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                target("ListView.children", LIST_VIEW, CHILDREN),
                target("GridView.count.children", GRID_VIEW, CHILDREN),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE),
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger gridAccepted = new AtomicInteger();
        AtomicInteger gridRejected = new AtomicInteger();

        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, GRID_VIEW, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "GridView.count -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                gridRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "GridView.count -> " + target.name());
                WidgetNode widget = success.command().widget();
                assertEquals(GRID_VIEW, widget.type());
                assertEquals(
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                        widget.properties().get(CROSS_AXIS_COUNT));
                WidgetSlot.ListSlot children = assertInstanceOf(
                        WidgetSlot.ListSlot.class,
                        widget.slots().get(CHILDREN));
                assertTrue(children.children().isEmpty());
                assertEquals(1, allocations.get());
                gridAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> oldSources = Stream.concat(
                coreSources().stream().map(CoreSourceCase::type),
                Stream.of(EXPANDED, IMAGE, FLEXIBLE, SPACER)).toList();
        MatrixTargetCase gridTarget = target(
                "GridView.count.children", GRID_VIEW, CHILDREN);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(oldSources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    gridTarget.document(), BUILT_INS, source, ROOT_ID,
                    gridTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> GridView.count.children");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> GridView.count.children");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILDREN, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(37, targets.size()),
                () -> assertEquals(35, gridAccepted.get()),
                () -> assertEquals(2, gridRejected.get()),
                () -> assertEquals(37, oldSources.size()),
                () -> assertEquals(34, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1406,
                        1332 + targets.size() + oldSources.size()),
                () -> assertEquals(1233,
                        1164 + gridAccepted.get() + targetAccepted.get()),
                () -> assertEquals(173,
                        168 + gridRejected.get() + targetRejected.get()));
    }

    @Test
    void gridViewExtentCreatesRequiredPositiveExtentAndOrderedChildren() {
        WidgetTypeId extent = GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE;
        MatrixTargetCase columnTarget = target("Column.children", COLUMN, CHILDREN);
        FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(columnTarget.document(), BUILT_INS, extent, ROOT_ID,
                        CHILDREN, 0, FlutterImageAssetChoices.empty(), () -> NEW_ID));

        WidgetNode widget = accepted.command().widget();
        assertEquals(extent, widget.type());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.valueOf(200.0)),
                widget.properties().get(new PropertyName("maxCrossAxisExtent")));
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class, widget.slots().get(CHILDREN));
        assertTrue(children.children().isEmpty());
    }

    @Test
    void singleChildScrollViewCompletesExact1482CellModelWithOptionalChild() {
        List<MatrixTargetCase> previousTargets = preIndexedStackDefinitions()
                .filter(definition -> !SINGLE_CHILD_SCROLL_VIEW.equals(definition.typeId()))
                .filter(definition -> !COLORED_BOX.equals(definition.typeId()))
                .filter(definition -> !PLACEHOLDER.equals(definition.typeId()))
                .filter(definition -> !DECORATED_BOX.equals(definition.typeId()))
                .filter(definition -> !EXCLUDE_SEMANTICS.equals(definition.typeId()))
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(previousTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, SINGLE_CHILD_SCROLL_VIEW, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "SingleChildScrollView -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "SingleChildScrollView -> " + target.name());
                assertEquals(SINGLE_CHILD_SCROLL_VIEW, success.command().widget().type());
                assertTrue(success.command().widget().properties().isEmpty());
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> allSources = preIndexedStackDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !COLORED_BOX.equals(type))
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !PLACEHOLDER.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !DECORATED_BOX.equals(type))
                .filter(type -> !EXCLUDE_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase scrollTarget = target(
                "SingleChildScrollView.child", SINGLE_CHILD_SCROLL_VIEW, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(allSources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    scrollTarget.document(), BUILT_INS, source, ROOT_ID,
                    scrollTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> SingleChildScrollView.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> SingleChildScrollView.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(42, previousTargets.size()),
                () -> assertEquals(40, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(42, allSources.size()),
                () -> assertEquals(39, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1490,
                        1406 + previousTargets.size() + allSources.size()),
                () -> assertEquals(1310,
                        1231 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(178,
                        173 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void coloredBoxCompletesExact1560CellModelWithRequiredCreationColorAndOptionalChild() {
        List<MatrixTargetCase> previousTargets = preIndexedStackDefinitions()
                .filter(definition -> !COLORED_BOX.equals(definition.typeId()))
                .filter(definition -> !PLACEHOLDER.equals(definition.typeId()))
                .filter(definition -> !DECORATED_BOX.equals(definition.typeId()))
                .filter(definition -> !EXCLUDE_SEMANTICS.equals(definition.typeId()))
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(previousTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, COLORED_BOX, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ColoredBox -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ColoredBox -> " + target.name());
                assertEquals(COLORED_BOX, success.command().widget().type());
                assertEquals(
                        new PropertyValue.ColorValue(0xFF2196F3L),
                        success.command().widget().properties().get(COLOR_VALUE));
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> allSources = preIndexedStackDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !PLACEHOLDER.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !DECORATED_BOX.equals(type))
                .filter(type -> !EXCLUDE_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase coloredBoxTarget = target(
                "ColoredBox.child", COLORED_BOX, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(allSources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    coloredBoxTarget.document(), BUILT_INS, source, ROOT_ID,
                    coloredBoxTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ColoredBox.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ColoredBox.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(43, previousTargets.size()),
                () -> assertEquals(41, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(43, allSources.size()),
                () -> assertEquals(40, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1568,
                        1482 + previousTargets.size() + allSources.size()),
                () -> assertEquals(1385,
                        1304 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(183,
                        178 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void spacerAppendsToPopulatedFlexChildrenAndNeverWrapsTheExistingChild() {
        DesignerDocument document = document(parent(
                ROW, List.of(text(FIRST_ID, "existing"))));
        FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(
                        document,
                        BUILT_INS,
                        SPACER,
                        ROOT_ID,
                        CHILDREN,
                        1,
                        () -> NEW_ID));

        assertEquals(1, accepted.command().destination().index());
        assertEquals(SPACER, accepted.command().widget().type());
        assertTrue(accepted.command().widget().slots().isEmpty());
        assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(
                        document,
                        BUILT_INS,
                        SPACER,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        () -> NEW_ID));
    }

    @Test
    void excludeFocusCompletesExact3360CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preExcludeFocusTraversalDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.ExcludeFocus"), ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ExcludeFocus -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "ExcludeFocus -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(new WidgetTypeId("flutter.widgets.ExcludeFocus"), command.wrapper().type());
                assertTrue(command.wrapper().properties().isEmpty());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(63, Math.toIntExact(preExcludeFocusTraversalDefinitions().count())),
                () -> assertEquals(61, optionalTargets.size()),
                () -> assertEquals(59, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(3660, 60 * optionalTargets.size()),
                () -> assertEquals(3091, 3032 + accepted.get()),
                () -> assertEquals(274, 272 + rejected.get()));
    }

    @Test
    void excludeFocusTraversalCompletesExact3416CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preVisibilityDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ExcludeFocusTraversal -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "ExcludeFocusTraversal -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), command.wrapper().type());
                assertTrue(command.wrapper().properties().isEmpty());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(64, Math.toIntExact(preVisibilityDefinitions().count())),
                () -> assertEquals(61, optionalTargets.size()),
                () -> assertEquals(59, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(3721, 61 * optionalTargets.size()),
                () -> assertEquals(3145, 3086 + accepted.get()),
                () -> assertEquals(276, 274 + rejected.get()));
    }

    @Test
    void safeAreaCompletesExact1599CellModelAsWrapperOnlyAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preIndexedStackDefinitions()
                .filter(definition -> !PLACEHOLDER.equals(definition.typeId()))
                .filter(definition -> !DECORATED_BOX.equals(definition.typeId()))
                .filter(definition -> !EXCLUDE_SEMANTICS.equals(definition.typeId()))
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, SAFE_AREA, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "SafeArea -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "SafeArea -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(SAFE_AREA, command.wrapper().type());
                assertTrue(command.wrapper().properties().isEmpty());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(46, preIndexedStackWidgetCount() - 2),
                () -> assertEquals(44, optionalTargets.size()),
                () -> assertEquals(42, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(1804, 41 * optionalTargets.size()),
                () -> assertEquals(1419, 1377 + accepted.get()),
                () -> assertEquals(185, 183 + rejected.get()));
    }

    @Test
    void placeholderCompletesExact1680CellModelWithOmittedDefaultsAndOptionalChild() {
        List<MatrixTargetCase> previousTargets = preIndexedStackDefinitions()
                .filter(definition -> !PLACEHOLDER.equals(definition.typeId()))
                .filter(definition -> !DECORATED_BOX.equals(definition.typeId()))
                .filter(definition -> !EXCLUDE_SEMANTICS.equals(definition.typeId()))
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(previousTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, PLACEHOLDER, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "Placeholder -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "Placeholder -> " + target.name());
                assertEquals(PLACEHOLDER, success.command().widget().type());
                assertTrue(success.command().widget().properties().isEmpty(),
                        "all Placeholder framework defaults must remain omitted");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> ordinarySources = preIndexedStackDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !DECORATED_BOX.equals(type))
                .filter(type -> !EXCLUDE_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase placeholderTarget = target(
                "Placeholder.child", PLACEHOLDER, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(ordinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    placeholderTarget.document(), BUILT_INS, source, ROOT_ID,
                    placeholderTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> Placeholder.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> Placeholder.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedPlaceholder = occupiedTarget(
                "Placeholder.child", PLACEHOLDER, CHILD);
        FlutterDesignerPaletteDropPlanner.Wrapped safeArea = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(
                        occupiedPlaceholder.document(), BUILT_INS, SAFE_AREA, ROOT_ID,
                        CHILD, 0, choices, () -> NEW_ID));
        assertEquals(FIRST_ID, safeArea.command().widgetId());
        assertEquals(SAFE_AREA, safeArea.command().wrapper().type());

        assertAll(
                () -> assertEquals(44, previousTargets.size()),
                () -> assertEquals(42, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(44, ordinarySources.size()),
                () -> assertEquals(41, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1688,
                        1599 + previousTargets.size()
                                + preIndexedStackWidgetCount() - 3),
                () -> assertEquals(1498,
                        1414 + sourceAccepted.get() + targetAccepted.get() + 1),
                () -> assertEquals(190,
                        185 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void directionalityCompletesExact1720CellModelAsRequiredWrapperAcrossAllOptionalTargets() {
        List<MatrixTargetCase> optionalTargets = preIndexedStackDefinitions()
                .filter(definition -> !DECORATED_BOX.equals(definition.typeId()))
                .filter(definition -> !EXCLUDE_SEMANTICS.equals(definition.typeId()))
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> occupiedTarget(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        assertAll(optionalTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, DIRECTIONALITY, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "Directionality -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                rejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Wrapped success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Wrapped.class,
                        result,
                        "Directionality -> " + target.name());
                WrapWidget command = success.command();
                assertEquals(FIRST_ID, command.widgetId());
                assertEquals(DIRECTIONALITY, command.wrapper().type());
                assertEquals(
                        Map.of(
                                new PropertyName("textDirection"),
                                new PropertyValue.EnumValue("TextDirection", "ltr")),
                        command.wrapper().properties());
                assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.wrapper().slots());
                assertEquals(CHILD, command.wrapperSlot());
                assertEquals(0, command.wrapperIndex());
                assertEquals(1, allocations.get());
                accepted.incrementAndGet();
            }
        }));

        assertAll(
                () -> assertEquals(46, preIndexedStackWidgetCount() - 2),
                () -> assertEquals(45, optionalTargets.size()),
                () -> assertEquals(43, accepted.get()),
                () -> assertEquals(2, rejected.get()),
                () -> assertEquals(2070,
                        (preIndexedStackWidgetCount() - 2) * optionalTargets.size()),
                () -> assertEquals(1533, 1490 + accepted.get()),
                () -> assertEquals(192, 190 + rejected.get()));
    }

    @Test
    void decoratedBoxCompletesExact1804CellModelWithRequiredDecorationAndOptionalChild() {
        List<MatrixTargetCase> previousTargets = preIndexedStackDefinitions()
                .filter(definition -> !DECORATED_BOX.equals(definition.typeId()))
                .filter(definition -> !EXCLUDE_SEMANTICS.equals(definition.typeId()))
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(previousTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, DECORATED_BOX, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "DecoratedBox -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "DecoratedBox -> " + target.name());
                assertEquals(DECORATED_BOX, success.command().widget().type());
                assertEquals(
                        Map.of(DECORATION_VALUE, emptyBoxDecoration()),
                        success.command().widget().properties());
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> ordinarySources = preIndexedStackDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !EXCLUDE_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase decoratedBoxTarget = target(
                "DecoratedBox.child", DECORATED_BOX, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(ordinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    decoratedBoxTarget.document(), BUILT_INS, source, ROOT_ID,
                    decoratedBoxTarget.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> DecoratedBox.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> DecoratedBox.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedDecoratedBox = occupiedTarget(
                "DecoratedBox.child", DECORATED_BOX, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedDecoratedBox.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(47, preIndexedStackWidgetCount() - 1),
                () -> assertEquals(45, previousTargets.size()),
                () -> assertEquals(43, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(45, ordinarySources.size()),
                () -> assertEquals(44, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1812,
                        1720 + previousTargets.size()
                                + preIndexedStackWidgetCount() - 1),
                () -> assertEquals(1615,
                        1528 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(197,
                        192 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void excludeSemanticsCompletesExact1890CellModelWithNullableBooleanAndOptionalChild() {
        List<MatrixTargetCase> allTargets = preIndexedStackDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, EXCLUDE_SEMANTICS, ROOT_ID,
                    target.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ExcludeSemantics -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ExcludeSemantics -> " + target.name());
                assertEquals(EXCLUDE_SEMANTICS, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves Flutter's excluding=true default");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preIndexedStackDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !EXCLUDE_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase excludeSemanticsTarget = target(
                "ExcludeSemantics.child", EXCLUDE_SEMANTICS, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    excludeSemanticsTarget.document(), BUILT_INS, source, ROOT_ID,
                    excludeSemanticsTarget.slot(), 0, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ExcludeSemantics.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ExcludeSemantics.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedExcludeSemantics = occupiedTarget(
                "ExcludeSemantics.child", EXCLUDE_SEMANTICS, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedExcludeSemantics.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(48, preIndexedStackWidgetCount()),
                () -> assertEquals(47, allTargets.size()),
                () -> assertEquals(45, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(45, previousOrdinarySources.size()),
                () -> assertEquals(44, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1898,
                        1804 + allTargets.size()
                                + preIndexedStackWidgetCount() - 1),
                () -> assertEquals(1696,
                        1607 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(202,
                        197 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void indexedStackCompletesExact1978CellModelWithNullableIndexAndOrderedChildren() {
        List<MatrixTargetCase> allTargets = preClipRectDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, INDEXED_STACK, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "IndexedStack -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "IndexedStack -> " + target.name());
                assertEquals(INDEXED_STACK, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves Flutter's index=0 default");
                assertEquals(
                        Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preClipRSuperellipseDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !CLIP_PATH.equals(type))
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !INDEXED_STACK.equals(type))
                .filter(type -> !CLIP_RECT.equals(type))
                .filter(type -> !CLIP_OVAL.equals(type))
                .filter(type -> !CLIP_RRECT.equals(type))
                .toList();
        MatrixTargetCase indexedStackTarget = target(
                "IndexedStack.children", INDEXED_STACK, CHILDREN);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    indexedStackTarget.document(), BUILT_INS, source, ROOT_ID,
                    indexedStackTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> IndexedStack.children");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> IndexedStack.children");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILDREN, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedIndexedStack = occupiedTarget(
                "IndexedStack.children", INDEXED_STACK, CHILDREN);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedIndexedStack.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILDREN, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(49, preClipRectWidgetCount()),
                () -> assertEquals(48, allTargets.size()),
                () -> assertEquals(46, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(46, previousOrdinarySources.size()),
                () -> assertEquals(45, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(1986,
                        1890 + allTargets.size()
                                + preClipRectWidgetCount() - 1),
                () -> assertEquals(1779,
                        1688 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(207,
                        202 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void clipRectCompletesExact2068CellModelWithOptionalClipAndChild() {
        List<MatrixTargetCase> allTargets = preClipOvalDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, CLIP_RECT, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ClipRect -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ClipRect -> " + target.name());
                assertEquals(CLIP_RECT, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves Flutter's hardEdge default");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preClipRSuperellipseDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !CLIP_PATH.equals(type))
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !CLIP_RECT.equals(type))
                .filter(type -> !CLIP_OVAL.equals(type))
                .filter(type -> !CLIP_RRECT.equals(type))
                .toList();
        MatrixTargetCase clipRectTarget = target(
                "ClipRect.child", CLIP_RECT, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    clipRectTarget.document(), BUILT_INS, source, ROOT_ID,
                    clipRectTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ClipRect.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ClipRect.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedClipRect = occupiedTarget(
                "ClipRect.child", CLIP_RECT, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedClipRect.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(50, preClipOvalWidgetCount()),
                () -> assertEquals(49, allTargets.size()),
                () -> assertEquals(47, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(47, previousOrdinarySources.size()),
                () -> assertEquals(46, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2076,
                        1978 + allTargets.size()
                                + preClipOvalWidgetCount() - 1),
                () -> assertEquals(1863,
                        1770 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(212,
                        207 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void clipOvalCompletesExact2160CellModelWithOptionalClipAndChild() {
        List<MatrixTargetCase> allTargets = preClipRRectDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, CLIP_OVAL, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ClipOval -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ClipOval -> " + target.name());
                assertEquals(CLIP_OVAL, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves Flutter's antiAlias default");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preClipRSuperellipseDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !CLIP_PATH.equals(type))
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !CLIP_OVAL.equals(type))
                .filter(type -> !CLIP_RRECT.equals(type))
                .toList();
        MatrixTargetCase clipOvalTarget = target(
                "ClipOval.child", CLIP_OVAL, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    clipOvalTarget.document(), BUILT_INS, source, ROOT_ID,
                    clipOvalTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ClipOval.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ClipOval.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedClipOval = occupiedTarget(
                "ClipOval.child", CLIP_OVAL, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedClipOval.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(51, preClipRRectWidgetCount()),
                () -> assertEquals(50, allTargets.size()),
                () -> assertEquals(48, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(48, previousOrdinarySources.size()),
                () -> assertEquals(47, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2168,
                        2068 + allTargets.size()
                                + preClipRRectWidgetCount() - 1),
                () -> assertEquals(1951,
                        1856 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(217,
                        212 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void clipRRectCompletesExact2254CellModelWithOptionalRadiusClipAndChild() {
        List<MatrixTargetCase> allTargets = preClipPathDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, CLIP_RRECT, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ClipRRect -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ClipRRect -> " + target.name());
                assertEquals(CLIP_RRECT, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves BorderRadius.zero and Clip.antiAlias defaults");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preClipPathDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !CLIP_RRECT.equals(type))
                .toList();
        MatrixTargetCase clipRRectTarget = target(
                "ClipRRect.child", CLIP_RRECT, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    clipRRectTarget.document(), BUILT_INS, source, ROOT_ID,
                    clipRRectTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ClipRRect.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ClipRRect.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedClipRRect = occupiedTarget(
                "ClipRRect.child", CLIP_RRECT, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedClipRRect.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(52, preClipPathWidgetCount()),
                () -> assertEquals(51, allTargets.size()),
                () -> assertEquals(49, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(49, previousOrdinarySources.size()),
                () -> assertEquals(48, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2262,
                        2160 + allTargets.size()
                                + preClipPathWidgetCount() - 1),
                () -> assertEquals(2038,
                        1941 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(222,
                        217 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void clipPathCompletesExact2350CellModelWithOptionalBranchesAndChild() {
        List<MatrixTargetCase> allTargets = preClipRSuperellipseDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, CLIP_PATH, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ClipPath -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ClipPath -> " + target.name());
                assertEquals(CLIP_PATH, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves unnamed ClipPath and antiAlias defaults");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preClipRSuperellipseDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !CLIP_PATH.equals(type))
                .toList();
        MatrixTargetCase clipPathTarget = target(
                "ClipPath.child", CLIP_PATH, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    clipPathTarget.document(), BUILT_INS, source, ROOT_ID,
                    clipPathTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ClipPath.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ClipPath.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedClipPath = occupiedTarget(
                "ClipPath.child", CLIP_PATH, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedClipPath.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(53, preClipRSuperellipseWidgetCount()),
                () -> assertEquals(52, allTargets.size()),
                () -> assertEquals(50, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(50, previousOrdinarySources.size()),
                () -> assertEquals(49, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2358,
                        2254 + allTargets.size()
                                + preClipRSuperellipseWidgetCount() - 1),
                () -> assertEquals(2129,
                        2030 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(227,
                        222 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void clipRSuperellipseCompletesExact2448CellModelWithOptionalRadiusClipAndChild() {
        List<MatrixTargetCase> allTargets = prePhysicalModelDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, CLIP_RSUPERELLIPSE, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "ClipRSuperellipse -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "ClipRSuperellipse -> " + target.name());
                assertEquals(CLIP_RSUPERELLIPSE, success.command().widget().type());
                assertEquals(Map.of(), success.command().widget().properties(),
                        "omission preserves BorderRadius.zero and antiAlias defaults");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = prePhysicalModelDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !CLIP_RSUPERELLIPSE.equals(type))
                .toList();
        MatrixTargetCase clipRSuperellipseTarget = target(
                "ClipRSuperellipse.child", CLIP_RSUPERELLIPSE, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    clipRSuperellipseTarget.document(), BUILT_INS, source, ROOT_ID,
                    clipRSuperellipseTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> ClipRSuperellipse.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> ClipRSuperellipse.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedClipRSuperellipse = occupiedTarget(
                "ClipRSuperellipse.child", CLIP_RSUPERELLIPSE, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedClipRSuperellipse.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(54, prePhysicalModelWidgetCount()),
                () -> assertEquals(53, allTargets.size()),
                () -> assertEquals(51, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(51, previousOrdinarySources.size()),
                () -> assertEquals(50, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2456,
                        2350 + allTargets.size()
                                + prePhysicalModelWidgetCount() - 1),
                () -> assertEquals(2222,
                        2121 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(232,
                        227 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void indexedSemanticsCompletesExact3304CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preExcludeFocusDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, INDEXED_SEMANTICS, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "IndexedSemantics -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "IndexedSemantics -> " + target.name());
                assertEquals(INDEXED_SEMANTICS, success.command().widget().type());
                assertEquals(Map.of(new PropertyName("index"),
                                new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)),
                        success.command().widget().properties(),
                        "required index is initialized to zero");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preExcludeFocusDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !INDEXED_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase indexedSemanticsTarget = target(
                "IndexedSemantics.child", INDEXED_SEMANTICS, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    indexedSemanticsTarget.document(), BUILT_INS, source, ROOT_ID,
                    indexedSemanticsTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> IndexedSemantics.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> IndexedSemantics.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedIndexedSemantics = occupiedTarget(
                "IndexedSemantics.child", INDEXED_SEMANTICS, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedIndexedSemantics.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(62, Math.toIntExact(preExcludeFocusDefinitions().count())),
                () -> assertEquals(61, allTargets.size()),
                () -> assertEquals(59, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(59, previousOrdinarySources.size()),
                () -> assertEquals(58, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(3312,
                        3190 + allTargets.size()
                                + Math.toIntExact(preExcludeFocusDefinitions().count()) - 1),
                () -> assertEquals(3040,
                        2923 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(272,
                        267 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void mergeSemanticsCompletesExact3190CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preIndexedSemanticsDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, MERGE_SEMANTICS, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "MergeSemantics -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "MergeSemantics -> " + target.name());
                assertEquals(MERGE_SEMANTICS, success.command().widget().type());
                assertEquals(Map.of(),
                        success.command().widget().properties(),
                        "there are no scalar constructor properties");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preIndexedSemanticsDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !MERGE_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase mergeSemanticsTarget = target(
                "MergeSemantics.child", MERGE_SEMANTICS, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    mergeSemanticsTarget.document(), BUILT_INS, source, ROOT_ID,
                    mergeSemanticsTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> MergeSemantics.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> MergeSemantics.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedMergeSemantics = occupiedTarget(
                "MergeSemantics.child", MERGE_SEMANTICS, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedMergeSemantics.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(61, Math.toIntExact(preIndexedSemanticsDefinitions().count())),
                () -> assertEquals(60, allTargets.size()),
                () -> assertEquals(58, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(58, previousOrdinarySources.size()),
                () -> assertEquals(57, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(3198,
                        3078 + allTargets.size()
                                + Math.toIntExact(preIndexedSemanticsDefinitions().count()) - 1),
                () -> assertEquals(2931,
                        2816 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(267,
                        262 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void blockSemanticsCompletesExact3078CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preMergeSemanticsDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, BLOCK_SEMANTICS, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "BlockSemantics -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "BlockSemantics -> " + target.name());
                assertEquals(BLOCK_SEMANTICS, success.command().widget().type());
                assertEquals(Map.of(),
                        success.command().widget().properties(),
                        "omission preserves the SDK blocking=true default");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preMergeSemanticsDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !BLOCK_SEMANTICS.equals(type))
                .toList();
        MatrixTargetCase blockSemanticsTarget = target(
                "BlockSemantics.child", BLOCK_SEMANTICS, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    blockSemanticsTarget.document(), BUILT_INS, source, ROOT_ID,
                    blockSemanticsTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> BlockSemantics.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> BlockSemantics.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedBlockSemantics = occupiedTarget(
                "BlockSemantics.child", BLOCK_SEMANTICS, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedBlockSemantics.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(60, Math.toIntExact(preMergeSemanticsDefinitions().count())),
                () -> assertEquals(59, allTargets.size()),
                () -> assertEquals(57, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(57, previousOrdinarySources.size()),
                () -> assertEquals(56, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(3086,
                        2968 + allTargets.size()
                                + Math.toIntExact(preMergeSemanticsDefinitions().count()) - 1),
                () -> assertEquals(2824,
                        2711 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(262,
                        257 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void absorbPointerCompletesExact2968CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preBlockSemanticsDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, ABSORB_POINTER, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "AbsorbPointer -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "AbsorbPointer -> " + target.name());
                assertEquals(ABSORB_POINTER, success.command().widget().type());
                assertEquals(Map.of(),
                        success.command().widget().properties(),
                        "omission preserves absorbing=true and deprecated ignoringSemantics=null");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preBlockSemanticsDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !ABSORB_POINTER.equals(type))
                .toList();
        MatrixTargetCase absorbPointerTarget = target(
                "AbsorbPointer.child", ABSORB_POINTER, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    absorbPointerTarget.document(), BUILT_INS, source, ROOT_ID,
                    absorbPointerTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> AbsorbPointer.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> AbsorbPointer.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedAbsorbPointer = occupiedTarget(
                "AbsorbPointer.child", ABSORB_POINTER, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedAbsorbPointer.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(59, Math.toIntExact(preBlockSemanticsDefinitions().count())),
                () -> assertEquals(58, allTargets.size()),
                () -> assertEquals(56, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(56, previousOrdinarySources.size()),
                () -> assertEquals(55, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2976,
                        2860 + allTargets.size()
                                + Math.toIntExact(preBlockSemanticsDefinitions().count()) - 1),
                () -> assertEquals(2719,
                        2608 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(257,
                        252 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void ignorePointerCompletesExact2860CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preAbsorbPointerDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, IGNORE_POINTER, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "IgnorePointer -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "IgnorePointer -> " + target.name());
                assertEquals(IGNORE_POINTER, success.command().widget().type());
                assertEquals(Map.of(),
                        success.command().widget().properties(),
                        "omission preserves ignoring=true and deprecated ignoringSemantics=null");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preAbsorbPointerDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !IGNORE_POINTER.equals(type))
                .toList();
        MatrixTargetCase ignorePointerTarget = target(
                "IgnorePointer.child", IGNORE_POINTER, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    ignorePointerTarget.document(), BUILT_INS, source, ROOT_ID,
                    ignorePointerTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> IgnorePointer.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> IgnorePointer.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedIgnorePointer = occupiedTarget(
                "IgnorePointer.child", IGNORE_POINTER, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedIgnorePointer.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(58, Math.toIntExact(preAbsorbPointerDefinitions().count())),
                () -> assertEquals(57, allTargets.size()),
                () -> assertEquals(55, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(55, previousOrdinarySources.size()),
                () -> assertEquals(54, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2868,
                        2754 + allTargets.size()
                                + Math.toIntExact(preAbsorbPointerDefinitions().count()) - 1),
                () -> assertEquals(2616,
                        2507 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(252,
                        247 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void repaintBoundaryCompletesExact2754CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preIgnorePointerDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, REPAINT_BOUNDARY, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "RepaintBoundary -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "RepaintBoundary -> " + target.name());
                assertEquals(REPAINT_BOUNDARY, success.command().widget().type());
                assertEquals(Map.of(),
                        success.command().widget().properties(),
                        "no scalar constructor arguments are synthesized");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preIgnorePointerDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !REPAINT_BOUNDARY.equals(type))
                .toList();
        MatrixTargetCase repaintBoundaryTarget = target(
                "RepaintBoundary.child", REPAINT_BOUNDARY, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    repaintBoundaryTarget.document(), BUILT_INS, source, ROOT_ID,
                    repaintBoundaryTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> RepaintBoundary.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> RepaintBoundary.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedRepaintBoundary = occupiedTarget(
                "RepaintBoundary.child", REPAINT_BOUNDARY, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedRepaintBoundary.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(57, Math.toIntExact(preIgnorePointerDefinitions().count())),
                () -> assertEquals(56, allTargets.size()),
                () -> assertEquals(54, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(54, previousOrdinarySources.size()),
                () -> assertEquals(53, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2762,
                        2650 + allTargets.size()
                                + Math.toIntExact(preIgnorePointerDefinitions().count()) - 1),
                () -> assertEquals(2515,
                        2408 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(247,
                        242 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void physicalShapeCompletesExact2650CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = preRepaintBoundaryDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, PHYSICAL_SHAPE, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "PhysicalShape -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "PhysicalShape -> " + target.name());
                assertEquals(PHYSICAL_SHAPE, success.command().widget().type());
                assertEquals(Map.of(new PropertyName("color"),
                        new PropertyValue.ColorValue(0xFF2196F3L), new PropertyName("clipper"),
                        PropertyValue.ShapeBorderClipperValue.defaultValue()),
                        success.command().widget().properties(),
                        "required surface color has an explicit safe prototype");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = preRepaintBoundaryDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !PHYSICAL_SHAPE.equals(type))
                .toList();
        MatrixTargetCase physicalShapeTarget = target(
                "PhysicalShape.child", PHYSICAL_SHAPE, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    physicalShapeTarget.document(), BUILT_INS, source, ROOT_ID,
                    physicalShapeTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> PhysicalShape.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> PhysicalShape.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedPhysicalShape = occupiedTarget(
                "PhysicalShape.child", PHYSICAL_SHAPE, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedPhysicalShape.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(56, Math.toIntExact(preRepaintBoundaryDefinitions().count())),
                () -> assertEquals(55, allTargets.size()),
                () -> assertEquals(53, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(53, previousOrdinarySources.size()),
                () -> assertEquals(52, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2658,
                        2548 + allTargets.size()
                                + Math.toIntExact(preRepaintBoundaryDefinitions().count()) - 1),
                () -> assertEquals(2416,
                        2311 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(242,
                        237 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void physicalModelCompletesExact2548CellModelWithAllSurfacePropertiesAndChild() {
        List<MatrixTargetCase> allTargets = prePhysicalShapeDefinitions()
                .flatMap(definition -> definition.slots().stream()
                        .filter(slot -> slot.minChildren() == 0)
                        .map(slot -> target(
                                definition.palette().displayName() + "."
                                        + slot.name().value(),
                                definition.typeId(),
                                slot.name())))
                .toList();
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/matrix.png", "Matrix asset")),
                Optional.empty());
        AtomicInteger sourceAccepted = new AtomicInteger();
        AtomicInteger sourceRejected = new AtomicInteger();

        assertAll(allTargets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    target.document(), BUILT_INS, PHYSICAL_MODEL, ROOT_ID,
                    target.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (target.name().equals("Scaffold.appBar")
                    || target.name().equals("AppBar.bottom")) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        "PhysicalModel -> " + target.name());
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                sourceRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        "PhysicalModel -> " + target.name());
                assertEquals(PHYSICAL_MODEL, success.command().widget().type());
                assertEquals(Map.of(new PropertyName("color"),
                        new PropertyValue.ColorValue(0xFF2196F3L)),
                        success.command().widget().properties(),
                        "required surface color has an explicit safe prototype");
                assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        success.command().widget().slots());
                assertEquals(1, allocations.get());
                sourceAccepted.incrementAndGet();
            }
        }));

        List<WidgetTypeId> previousOrdinarySources = prePhysicalShapeDefinitions()
                .map(WidgetDefinition::typeId)
                .filter(type -> !SAFE_AREA.equals(type))
                .filter(type -> !DIRECTIONALITY.equals(type))
                .filter(type -> !PHYSICAL_MODEL.equals(type))
                .toList();
        MatrixTargetCase physicalModelTarget = target(
                "PhysicalModel.child", PHYSICAL_MODEL, CHILD);
        AtomicInteger targetAccepted = new AtomicInteger();
        AtomicInteger targetRejected = new AtomicInteger();

        assertAll(previousOrdinarySources.stream().map(source -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    physicalModelTarget.document(), BUILT_INS, source, ROOT_ID,
                    physicalModelTarget.slot(), 0, choices, () -> {
                        allocations.incrementAndGet();
                        return NEW_ID;
                    });
            if (source.equals(EXPANDED)
                    || source.equals(FLEXIBLE)
                    || source.equals(SPACER)) {
                FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        result,
                        source.value() + " -> PhysicalModel.child");
                assertEquals(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        failure.code());
                assertEquals(0, allocations.get());
                targetRejected.incrementAndGet();
            } else {
                FlutterDesignerPaletteDropPlanner.Accepted success = assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Accepted.class,
                        result,
                        source.value() + " -> PhysicalModel.child");
                assertEquals(ROOT_ID, success.command().destination().parentId());
                assertEquals(CHILD, success.command().destination().slotName());
                assertEquals(source, success.command().widget().type());
                assertEquals(1, allocations.get());
                targetAccepted.incrementAndGet();
            }
        }));

        MatrixTargetCase occupiedPhysicalModel = occupiedTarget(
                "PhysicalModel.child", PHYSICAL_MODEL, CHILD);
        for (WidgetTypeId wrapperType : List.of(SAFE_AREA, DIRECTIONALITY)) {
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(
                            occupiedPhysicalModel.document(), BUILT_INS, wrapperType,
                            ROOT_ID, CHILD, 0, choices, () -> NEW_ID));
            assertEquals(FIRST_ID, wrapped.command().widgetId());
            assertEquals(wrapperType, wrapped.command().wrapper().type());
            targetAccepted.incrementAndGet();
        }

        assertAll(
                () -> assertEquals(55, Math.toIntExact(prePhysicalShapeDefinitions().count())),
                () -> assertEquals(54, allTargets.size()),
                () -> assertEquals(52, sourceAccepted.get()),
                () -> assertEquals(2, sourceRejected.get()),
                () -> assertEquals(52, previousOrdinarySources.size()),
                () -> assertEquals(51, targetAccepted.get()),
                () -> assertEquals(3, targetRejected.get()),
                () -> assertEquals(2556,
                        2448 + allTargets.size()
                                + Math.toIntExact(prePhysicalShapeDefinitions().count()) - 1),
                () -> assertEquals(2319,
                        2216 + sourceAccepted.get() + targetAccepted.get()),
                () -> assertEquals(237,
                        232 + sourceRejected.get() + targetRejected.get()));
    }

    @Test
    void excludeFocusNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.ExcludeFocus"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.ExcludeFocus"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.ExcludeFocus"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.ExcludeFocus"), wrapped.command().wrapper().type());
        assertTrue(wrapped.command().wrapper().properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void excludeFocusTraversalNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), wrapped.command().wrapper().type());
        assertTrue(wrapped.command().wrapper().properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void safeAreaNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        Supplier<StableId> rejectedSupplier = () -> {
            rejectedAllocations.incrementAndGet();
            return NEW_ID;
        };
        FlutterDesignerPaletteDropPlanner.Rejected emptyList = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        SAFE_AREA, ROOT_ID, CHILDREN, 0, rejectedSupplier));
        FlutterDesignerPaletteDropPlanner.Rejected emptySingle = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(prototype(CENTER)), BUILT_INS,
                        SAFE_AREA, ROOT_ID, CHILD, 0, rejectedSupplier));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptyList.code());
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                emptySingle.code());
        assertEquals(0, rejectedAllocations.get());

        WidgetNode root = text(ROOT_ID, "root target");
        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(root), BUILT_INS, SAFE_AREA, ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(SAFE_AREA, wrapped.command().wrapper().type());
        assertTrue(wrapped.command().wrapper().properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
        assertEquals(0, wrapped.command().wrapperIndex());
    }

    @Test
    void directionalityNeverCreatesAnEmptyPrototypeAndCanWrapTheDesignerRootExactly() {
        AtomicInteger allocations = new AtomicInteger();
        FlutterDesignerPaletteDropPlanner.Rejected empty = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS,
                        DIRECTIONALITY, ROOT_ID, CHILDREN, 0, () -> {
                            allocations.incrementAndGet();
                            return NEW_ID;
                        }));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                empty.code());
        assertEquals(0, allocations.get());

        FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.planWrapTarget(
                        document(text(ROOT_ID, "root target")), BUILT_INS,
                        DIRECTIONALITY, ROOT_ID, () -> NEW_ID));
        assertEquals(ROOT_ID, wrapped.command().widgetId());
        assertEquals(NEW_ID, wrapped.command().wrapper().id());
        assertEquals(DIRECTIONALITY, wrapped.command().wrapper().type());
        assertEquals(
                Map.of(
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "ltr")),
                wrapped.command().wrapper().properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                wrapped.command().wrapper().slots());
        assertEquals(CHILD, wrapped.command().wrapperSlot());
    }

    @Test
    void excludeFocusRejectsWrappingFlexParentDataTargetsBeforeIdAllocation() {
        List<WidgetNode> targets = List.of(
                expanded(FIRST_ID, text(indexedId(20), "expanded child")),
                flexible(FIRST_ID, text(indexedId(21), "flexible child")),
                WidgetNodePrototypeFactory.create(definition(SPACER), FIRST_ID));
        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of(target))), BUILT_INS,
                            new WidgetTypeId("flutter.widgets.ExcludeFocus"), ROOT_ID, CHILDREN, 0, () -> {
                                allocations.incrementAndGet();
                                return NEW_ID;
                            }));
            assertEquals(
                    FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REJECTED,
                    failure.code());
            assertTrue(failure.reason().contains("must be a direct child"),
                    failure.reason());
            assertEquals(0, allocations.get());
        }));
    }

    @Test
    void excludeFocusTraversalRejectsWrappingFlexParentDataTargetsBeforeIdAllocation() {
        List<WidgetNode> targets = List.of(
                expanded(FIRST_ID, text(indexedId(20), "expanded child")),
                flexible(FIRST_ID, text(indexedId(21), "flexible child")),
                WidgetNodePrototypeFactory.create(definition(SPACER), FIRST_ID));
        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of(target))), BUILT_INS,
                            new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"), ROOT_ID, CHILDREN, 0, () -> {
                                allocations.incrementAndGet();
                                return NEW_ID;
                            }));
            assertEquals(
                    FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REJECTED,
                    failure.code());
            assertTrue(failure.reason().contains("must be a direct child"),
                    failure.reason());
            assertEquals(0, allocations.get());
        }));
    }

    @Test
    void safeAreaRejectsWrappingFlexParentDataTargetsBeforeIdAllocation() {
        List<WidgetNode> targets = List.of(
                expanded(FIRST_ID, text(indexedId(20), "expanded child")),
                flexible(FIRST_ID, text(indexedId(21), "flexible child")),
                WidgetNodePrototypeFactory.create(definition(SPACER), FIRST_ID));
        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of(target))), BUILT_INS,
                            SAFE_AREA, ROOT_ID, CHILDREN, 0, () -> {
                                allocations.incrementAndGet();
                                return NEW_ID;
                            }));
            assertEquals(
                    FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REJECTED,
                    failure.code());
            assertTrue(failure.reason().contains("must be a direct child"),
                    failure.reason());
            assertEquals(0, allocations.get());
        }));
    }

    @Test
    void directionalityRejectsWrappingFlexParentDataTargetsBeforeIdAllocation() {
        List<WidgetNode> targets = List.of(
                expanded(FIRST_ID, text(indexedId(24), "expanded child")),
                flexible(FIRST_ID, text(indexedId(25), "flexible child")),
                WidgetNodePrototypeFactory.create(definition(SPACER), FIRST_ID));
        assertAll(targets.stream().map(target -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of(target))), BUILT_INS,
                            DIRECTIONALITY, ROOT_ID, CHILDREN, 0, () -> {
                                allocations.incrementAndGet();
                                return NEW_ID;
                            }));
            assertEquals(
                    FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REJECTED,
                    failure.code());
            assertTrue(failure.reason().contains("must be a direct child"),
                    failure.reason());
            assertEquals(0, allocations.get());
        }));
    }

    @Test
    void expandedAndFlexibleCannotWrapAnyFlexParentDataWidget() {
        record NestedCase(String name, WidgetTypeId outer, WidgetNode inner) {
        }
        WidgetNode innerChild = text(
                id("26bd530c-a49d-4a9e-b81c-32d918966681"), "inner child");
        List<NestedCase> cases = List.of(
                new NestedCase("Expanded over Expanded", EXPANDED,
                        expanded(FIRST_ID, innerChild)),
                new NestedCase("Flexible over Expanded", FLEXIBLE,
                        expanded(FIRST_ID, innerChild)),
                new NestedCase("Expanded over Flexible", EXPANDED,
                        flexible(FIRST_ID, innerChild)),
                new NestedCase("Flexible over Flexible", FLEXIBLE,
                        flexible(FIRST_ID, innerChild)),
                new NestedCase("Expanded over Spacer", EXPANDED,
                        WidgetNodePrototypeFactory.create(definition(SPACER), FIRST_ID)),
                new NestedCase("Flexible over Spacer", FLEXIBLE,
                        WidgetNodePrototypeFactory.create(definition(SPACER), FIRST_ID)));

        assertAll(cases.stream().map(testCase -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Rejected failure = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(
                            document(parent(ROW, List.of(testCase.inner()))),
                            BUILT_INS,
                            testCase.outer(),
                            ROOT_ID,
                            CHILDREN,
                            0,
                            () -> {
                                allocations.incrementAndGet();
                                return NEW_ID;
                            }),
                    testCase.name());
            assertEquals(
                    FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REJECTED,
                    failure.code(),
                    testCase.name());
            assertTrue(failure.reason().contains("must be a direct child"),
                    failure.reason());
            assertEquals(0, allocations.get(), testCase.name());
        }));
    }

    @Test
    void expandedNeverCreatesAnEmptyOrTerminalPlaceholder() {
        AtomicInteger allocations = new AtomicInteger();
        Supplier<StableId> supplier = () -> {
            allocations.incrementAndGet();
            return NEW_ID;
        };

        FlutterDesignerPaletteDropPlanner.Rejected empty = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(
                        document(parent(ROW, List.of())),
                        BUILT_INS,
                        EXPANDED,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        supplier));
        FlutterDesignerPaletteDropPlanner.Rejected terminal = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(
                        document(parent(COLUMN,
                                List.of(text(FIRST_ID, "existing")))),
                        BUILT_INS,
                        EXPANDED,
                        ROOT_ID,
                        CHILDREN,
                        1,
                        supplier));
        FlutterDesignerPaletteDropPlanner.Rejected nested = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(
                        document(parent(ROW, List.of(expanded(
                                FIRST_ID,
                                text(id("26bd530c-a49d-4a9e-b81c-32d918966681"),
                                        "inner child"))))),
                        BUILT_INS,
                        EXPANDED,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        supplier));

        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                empty.code());
        assertTrue(empty.reason().contains("is empty; add a widget first"));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED,
                terminal.code());
        assertTrue(terminal.reason().contains("existing child index"));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REJECTED,
                nested.code());
        assertTrue(nested.reason().contains("Cannot wrap 'flutter.widgets.Expanded' ('"
                + FIRST_ID));
        assertTrue(nested.reason().contains("must be a direct child"));
        assertEquals(0, allocations.get());
    }

    @Test
    void rejectsInvalidStatesAcrossEveryAcceptedSingleSlot() {
        List<SingleTargetCase> targets = List.of(
                new SingleTargetCase("Scaffold.body", SCAFFOLD, BODY),
                new SingleTargetCase("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                new SingleTargetCase("Padding.child", PADDING, CHILD),
                new SingleTargetCase("Center.child", CENTER, CHILD),
                new SingleTargetCase("SizedBox.child", SIZED_BOX, CHILD),
                new SingleTargetCase("AspectRatio.child", ASPECT_RATIO, CHILD),
                new SingleTargetCase("Container.child", CONTAINER, CHILD),
                new SingleTargetCase("Opacity.child", OPACITY, CHILD),
                new SingleTargetCase("ColoredBox.child", COLORED_BOX, CHILD),
                new SingleTargetCase("Placeholder.child", PLACEHOLDER, CHILD),
                new SingleTargetCase("Align.child", ALIGN, CHILD),
                new SingleTargetCase(
                        "FractionallySizedBox.child", FRACTIONALLY_SIZED_BOX, CHILD),
                new SingleTargetCase("FittedBox.child", FITTED_BOX, CHILD),
                new SingleTargetCase("ConstrainedBox.child", CONSTRAINED_BOX, CHILD),
                new SingleTargetCase("UnconstrainedBox.child", UNCONSTRAINED_BOX, CHILD),
                new SingleTargetCase("LimitedBox.child", LIMITED_BOX, CHILD),
                new SingleTargetCase("OverflowBox.child", OVERFLOW_BOX, CHILD),
                new SingleTargetCase("Baseline.child", BASELINE, CHILD),
                new SingleTargetCase(
                        "IntrinsicHeight.child", INTRINSIC_HEIGHT, CHILD),
                new SingleTargetCase(
                        "IntrinsicWidth.child", INTRINSIC_WIDTH, CHILD),
                new SingleTargetCase("Offstage.child", OFFSTAGE, CHILD),
                new SingleTargetCase(
                        "SizedOverflowBox.child", SIZED_OVERFLOW_BOX, CHILD),
                new SingleTargetCase("Transform.child", TRANSFORM, CHILD),
                new SingleTargetCase("RotatedBox.child", ROTATED_BOX, CHILD),
                new SingleTargetCase(
                        "ElevatedButton.child", ELEVATED_BUTTON, CHILD));
        AtomicInteger allocations = new AtomicInteger();
        Supplier<StableId> supplier = () -> {
            allocations.incrementAndGet();
            return NEW_ID;
        };

        assertAll(targets.stream().flatMap(target -> Stream.of(
                (Executable) () -> assertRejection(
                        target.name() + " occupied",
                        document(singleParent(
                                target.parentType(), target.slot(),
                                text(FIRST_ID, "existing"))),
                        TEXT,
                        target.slot(),
                        0,
                        supplier,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_FULL),
                (Executable) () -> assertRejection(
                        target.name() + " non-zero index",
                        document(prototype(target.parentType())),
                        TEXT,
                        target.slot(),
                        1,
                        supplier,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.NON_TERMINAL_INSERTION),
                (Executable) () -> assertRejection(
                        target.name() + " wrong model cardinality",
                        document(parentWithSlot(
                                target.parentType(), target.slot(),
                                new WidgetSlot.ListSlot(List.of()))),
                        TEXT,
                        target.slot(),
                        0,
                        supplier,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.MODEL_SLOT_CARDINALITY_MISMATCH))));
        assertEquals(0, allocations.get(),
                "invalid single-slot states must fail before stable-id allocation");
    }

    @Test
    void enforcesTerminalAppendAcrossEveryListSlotForEveryPaletteSource() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        List<CoreSourceCase> sources = Stream.concat(
                coreSources().stream(), Stream.of(gridViewCountSource())).toList();
        List<SingleTargetCase> listTargets = List.of(
                new SingleTargetCase("Column.children", COLUMN, CHILDREN),
                new SingleTargetCase("Row.children", ROW, CHILDREN),
                new SingleTargetCase("Wrap.children", WRAP, CHILDREN),
                new SingleTargetCase("Stack.children", STACK, CHILDREN),
                new SingleTargetCase("ListBody.children", LIST_BODY, CHILDREN),
                new SingleTargetCase("OverflowBar.children", OVERFLOW_BAR, CHILDREN),
                new SingleTargetCase("ListView.children", LIST_VIEW, CHILDREN),
                new SingleTargetCase(
                        "GridView.count.children", GRID_VIEW, CHILDREN),
                new SingleTargetCase("AppBar.actions", APP_BAR, ACTIONS));

        assertAll(listTargets.stream().flatMap(target -> sources.stream()
                .flatMap(source -> Stream.of(
                        (Executable) () -> assertRejection(
                                source.name() + " -> " + target.name()
                                        + " at non-terminal index",
                                document(listParent(
                                        target.parentType(), target.slot(),
                                        List.of(text(FIRST_ID, "existing")))),
                                source.type(),
                                target.slot(),
                                0,
                                () -> {
                                    rejectedAllocations.incrementAndGet();
                                    return NEW_ID;
                                },
                                FlutterDesignerPaletteDropPlanner.RejectionCode.NON_TERMINAL_INSERTION),
                        (Executable) () -> {
                            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                                    FlutterDesignerPaletteDropPlanner.Accepted.class,
                                    planner.plan(
                                            document(parent(
                                                    target.parentType(),
                                                    target.slot(),
                                                    List.of(text(FIRST_ID, "existing")))),
                                            BUILT_INS,
                                            source.type(),
                                            ROOT_ID,
                                            target.slot(),
                                            1,
                                            () -> NEW_ID),
                                    source.name() + " -> " + target.name()
                                            + " terminal append");
                            assertEquals(1, accepted.command().destination().index());
                            assertExactPrototype(source, accepted.command().widget());
                        }))));
        assertAll(
                () -> assertEquals(34, sources.size()),
                () -> assertEquals(9, listTargets.size()),
                () -> assertEquals(0, rejectedAllocations.get(),
                        "non-terminal list insertions must fail before "
                        + "stable-id allocation"));
    }

    @Test
    void rejectsEveryUnsupportedOrInconsistentFirstSliceDestination() {
        WidgetDefinition column = definition(COLUMN);
        WidgetDefinition text = definition(TEXT);
        SlotDefinition builtInChildren = column.slot(CHILDREN).orElseThrow();
        WidgetCatalog maxOne = catalog(text, replaceSlots(
                column,
                new SlotDefinition(
                        CHILDREN,
                        builtInChildren.parameter(),
                        SlotCardinality.LIST,
                        0,
                        1,
                        new SlotAcceptance.AnyWidget())));
        WidgetCatalog rejectsText = catalog(text, replaceSlots(
                column,
                new SlotDefinition(
                        CHILDREN,
                        builtInChildren.parameter(),
                        SlotCardinality.LIST,
                        0,
                        WidgetSlot.MAX_LIST_CHILDREN,
                        new SlotAcceptance.ExactTypes(List.of(CENTER)))));

        DesignerDocument emptyColumn = document(parent(COLUMN, List.of()));
        DesignerDocument oneChildColumn = document(parent(
                COLUMN, List.of(text(FIRST_ID, "existing"))));
        List<RejectedCase> cases = List.of(
                rejection(
                        "source definition missing",
                        emptyColumn,
                        catalog(column),
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SOURCE_DEFINITION_MISSING),
                rejection(
                        "parent missing",
                        emptyColumn,
                        BUILT_INS,
                        TEXT,
                        FIRST_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.PARENT_NOT_FOUND),
                rejection(
                        "slot absent on Center",
                        document(WidgetNodePrototypeFactory.create(definition(CENTER), ROOT_ID)),
                        BUILT_INS,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_DEFINITION_MISSING),
                rejection(
                        "parent definition missing",
                        emptyColumn,
                        catalog(text),
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.PARENT_DEFINITION_MISSING),
                rejection(
                        "slot absent on Column",
                        emptyColumn,
                        BUILT_INS,
                        TEXT,
                        ROOT_ID,
                        CHILD,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_DEFINITION_MISSING),
                rejection(
                        "model slot disagrees with catalog",
                        document(new WidgetNode(
                                ROOT_ID,
                                COLUMN,
                                Map.of(),
                                Map.of(CHILDREN, WidgetSlot.SingleSlot.empty()))),
                        BUILT_INS,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.MODEL_SLOT_CARDINALITY_MISMATCH),
                rejection(
                        "slot rejects Text",
                        emptyColumn,
                        rejectsText,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_REJECTS_WIDGET),
                rejection(
                        "nonterminal insertion",
                        oneChildColumn,
                        BUILT_INS,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.NON_TERMINAL_INSERTION),
                rejection(
                        "catalog maximum reached",
                        oneChildColumn,
                        maxOne,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        1,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_FULL));

        cases = new ArrayList<>(cases);
        cases.add(rejection(
                "occupied Center child",
                document(singleParent(CENTER, text(FIRST_ID, "existing"))),
                BUILT_INS,
                TEXT,
                ROOT_ID,
                CHILD,
                0,
                FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_FULL));
        cases.add(rejection(
                "non-zero Center child index",
                document(WidgetNode.empty(ROOT_ID, CENTER)),
                BUILT_INS,
                TEXT,
                ROOT_ID,
                CHILD,
                1,
                FlutterDesignerPaletteDropPlanner.RejectionCode.NON_TERMINAL_INSERTION));

        AtomicInteger allocations = new AtomicInteger();
        Supplier<StableId> supplier = () -> {
            allocations.incrementAndGet();
            return NEW_ID;
        };
        assertAll(cases.stream().map(testCase -> (Executable) () -> {
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    testCase.document(),
                    testCase.catalog(),
                    testCase.source(),
                    testCase.parentId(),
                    testCase.slot(),
                    testCase.index(),
                    supplier);

            FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    result,
                    testCase.name());
            assertEquals(testCase.expected(), rejected.code(), testCase.name());
            assertTrue(!rejected.reason().isBlank(), testCase.name());
        }));
        assertEquals(0, allocations.get(),
                "structurally rejected drops must not consume a stable id");
    }

    @Test
    void enforcesTheAbsoluteWidgetSlotBound() {
        ArrayList<WidgetNode> children = new ArrayList<>(WidgetSlot.MAX_LIST_CHILDREN);
        for (int index = 0; index < WidgetSlot.MAX_LIST_CHILDREN; index++) {
            children.add(text(indexedId(index), "item"));
        }

        assertAll(List.of(COLUMN, ROW).stream().map(parentType -> (Executable) () -> {
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    document(parent(parentType, children)),
                    BUILT_INS,
                    TEXT,
                    ROOT_ID,
                    CHILDREN,
                    WidgetSlot.MAX_LIST_CHILDREN,
                    () -> NEW_ID);

            FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class, result,
                    parentType.value());
            assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_FULL,
                    rejected.code(), parentType.value());
            assertTrue(rejected.reason().contains(
                    Integer.toString(WidgetSlot.MAX_LIST_CHILDREN)),
                    parentType.value());
        }));
    }

    @Test
    void rejectsDuplicateExistingAndAllocatedStableIds() {
        WidgetNode duplicate = text(FIRST_ID, "duplicate");
        DesignerDocument invalidDocument = document(parent(
                COLUMN, List.of(duplicate, duplicate)));
        FlutterDesignerPaletteDropPlanner.Result invalid = planner.plan(
                invalidDocument,
                BUILT_INS,
                TEXT,
                ROOT_ID,
                CHILDREN,
                2,
                () -> NEW_ID);
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.DOCUMENT_ID_CONFLICT,
                assertInstanceOf(
                        FlutterDesignerPaletteDropPlanner.Rejected.class,
                        invalid).code());

        FlutterDesignerPaletteDropPlanner.Result conflict = planner.plan(
                document(parent(COLUMN, List.of(text(FIRST_ID, "existing")))),
                BUILT_INS,
                TEXT,
                ROOT_ID,
                CHILDREN,
                1,
                () -> FIRST_ID);
        FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class, conflict);
        assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.STABLE_ID_CONFLICT,
                rejected.code());
        assertTrue(rejected.reason().contains(FIRST_ID.toString()));
    }

    @Test
    void convertsInvalidInputsAndIdSupplierFailuresToTypedRejections() {
        DesignerDocument document = document(parent(COLUMN, List.of()));
        List<InvalidCase> cases = List.of(
                new InvalidCase("null document", null, BUILT_INS, TEXT, ROOT_ID, CHILDREN, 0,
                        () -> NEW_ID),
                new InvalidCase("null catalog", document, null, TEXT, ROOT_ID, CHILDREN, 0,
                        () -> NEW_ID),
                new InvalidCase("null source", document, BUILT_INS, null, ROOT_ID, CHILDREN, 0,
                        () -> NEW_ID),
                new InvalidCase("null parent", document, BUILT_INS, TEXT, null, CHILDREN, 0,
                        () -> NEW_ID),
                new InvalidCase("null slot", document, BUILT_INS, TEXT, ROOT_ID, null, 0,
                        () -> NEW_ID),
                new InvalidCase("negative index", document, BUILT_INS, TEXT, ROOT_ID, CHILDREN, -1,
                        () -> NEW_ID),
                new InvalidCase("null supplier", document, BUILT_INS, TEXT, ROOT_ID, CHILDREN, 0,
                        null));

        Stream<Executable> invalidAssertions = cases.stream().map(testCase -> () -> {
            FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(
                            testCase.document(),
                            testCase.catalog(),
                            testCase.source(),
                            testCase.parentId(),
                            testCase.slot(),
                            testCase.index(),
                            testCase.supplier()),
                    testCase.name());
            assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.INVALID_REQUEST,
                    rejected.code(), testCase.name());
            assertTrue(!rejected.reason().isBlank(), testCase.name());
        });
        assertAll(invalidAssertions);

        FlutterDesignerPaletteDropPlanner.Rejected nullId = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(
                        document, BUILT_INS, TEXT, ROOT_ID, CHILDREN, 0, () -> null));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.STABLE_ID_ALLOCATION_FAILED,
                nullId.code());

        FlutterDesignerPaletteDropPlanner.Rejected thrown = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document, BUILT_INS, TEXT, ROOT_ID, CHILDREN, 0, () -> {
                    throw new IllegalStateException("allocator unavailable");
                }));
        assertEquals(
                FlutterDesignerPaletteDropPlanner.RejectionCode.STABLE_ID_ALLOCATION_FAILED,
                thrown.code());
        assertTrue(thrown.reason().contains("allocator unavailable"));
    }

    private static RejectedCase rejection(
            String name,
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId source,
            StableId parentId,
            SlotName slot,
            int index,
            FlutterDesignerPaletteDropPlanner.RejectionCode expected) {
        return new RejectedCase(
                name, document, catalog, source, parentId, slot, index, expected);
    }

    private void assertRejection(
            String name,
            DesignerDocument document,
            WidgetTypeId source,
            SlotName slot,
            int index,
            Supplier<StableId> supplier,
            FlutterDesignerPaletteDropPlanner.RejectionCode expected) {
        FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(
                        document,
                        BUILT_INS,
                        source,
                        ROOT_ID,
                        slot,
                        index,
                        supplier),
                name);
        assertEquals(expected, rejected.code(), name);
        assertTrue(!rejected.reason().isBlank(), name);
    }

    private static void assertExactPrototype(
            CoreSourceCase expected,
            WidgetNode actual) {
        assertEquals(NEW_ID, actual.id(), expected.name());
        assertEquals(expected.type(), actual.type(), expected.name());
        assertEquals(expected.properties(), actual.properties(), expected.name());
        assertEquals(expected.slots().keySet(), actual.slots().keySet(), expected.name());
        assertAll(expected.slots().entrySet().stream().map(entry -> (Executable) () -> {
            WidgetSlot actualSlot = actual.slots().get(entry.getKey());
            assertEquals(entry.getValue(), actualSlot.cardinality(),
                    expected.name() + "." + entry.getKey().value());
            switch (actualSlot) {
                case WidgetSlot.SingleSlot single -> assertTrue(single.child().isEmpty(),
                        expected.name() + "." + entry.getKey().value());
                case WidgetSlot.ListSlot list -> assertTrue(list.children().isEmpty(),
                        expected.name() + "." + entry.getKey().value());
            }
        }));
    }

    private static List<CoreSourceCase> coreSources() {
        PropertyValue.EdgeInsetsValue sixteen = new PropertyValue.EdgeInsetsValue(
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(16));
        return List.of(
                new CoreSourceCase(
                        "AppBar",
                        APP_BAR,
                        Map.of(),
                        Map.of(
                                LEADING, SlotCardinality.SINGLE,
                                TITLE, SlotCardinality.SINGLE,
                                ACTIONS, SlotCardinality.LIST,
                                FLEXIBLE_SPACE, SlotCardinality.SINGLE,
                                BOTTOM, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Scaffold",
                        SCAFFOLD,
                        Map.of(),
                        Map.of(
                                APP_BAR_SLOT, SlotCardinality.SINGLE,
                                BODY, SlotCardinality.SINGLE,
                                FLOATING_ACTION_BUTTON, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Elevated Button",
                        ELEVATED_BUTTON,
                        Map.of(ENABLED, new PropertyValue.BooleanValue(true)),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "TextField",
                        TEXT_FIELD,
                        Map.of(),
                        Map.of()),
                new CoreSourceCase(
                        "Column",
                        COLUMN,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "Row",
                        ROW,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "Wrap",
                        WRAP,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "Padding",
                        PADDING,
                        Map.of(PADDING_VALUE, sixteen),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Center",
                        CENTER,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "SizedBox",
                        SIZED_BOX,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Aspect Ratio",
                        ASPECT_RATIO,
                        Map.of(ASPECT_RATIO_VALUE,
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Container",
                        CONTAINER,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Opacity",
                        OPACITY,
                        Map.of(OPACITY_VALUE,
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Align",
                        ALIGN,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "FractionallySizedBox",
                        FRACTIONALLY_SIZED_BOX,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "FittedBox",
                        FITTED_BOX,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "ConstrainedBox",
                        CONSTRAINED_BOX,
                        Map.of(CONSTRAINTS, new PropertyValue.BoxConstraintsValue(
                                BigDecimal.ZERO,
                                Optional.empty(),
                                BigDecimal.ZERO,
                                Optional.empty())),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "UnconstrainedBox",
                        UNCONSTRAINED_BOX,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "LimitedBox",
                        LIMITED_BOX,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "OverflowBox",
                        OVERFLOW_BOX,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Baseline",
                        BASELINE,
                        Map.of(
                                BASELINE_VALUE,
                                new PropertyValue.DoubleValue(new BigDecimal("24")),
                                BASELINE_TYPE_VALUE,
                                new PropertyValue.EnumValue(
                                        "TextBaseline", "alphabetic")),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "IntrinsicHeight",
                        INTRINSIC_HEIGHT,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "IntrinsicWidth",
                        INTRINSIC_WIDTH,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Offstage",
                        OFFSTAGE,
                        Map.of(),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "SizedOverflowBox",
                        SIZED_OVERFLOW_BOX,
                        Map.of(SIZE, new PropertyValue.SizeValue(
                                BigDecimal.valueOf(100), BigDecimal.valueOf(100))),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Transform",
                        TRANSFORM,
                        Map.of(TRANSFORM_VALUE, identityMatrix()),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "RotatedBox",
                        ROTATED_BOX,
                        Map.of(QUARTER_TURNS,
                                new PropertyValue.IntegerValue(BigInteger.ONE)),
                        Map.of(CHILD, SlotCardinality.SINGLE)),
                new CoreSourceCase(
                        "Stack",
                        STACK,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "ListBody",
                        LIST_BODY,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "OverflowBar",
                        OVERFLOW_BAR,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "ListView",
                        LIST_VIEW,
                        Map.of(),
                        Map.of(CHILDREN, SlotCardinality.LIST)),
                new CoreSourceCase(
                        "Text",
                        TEXT,
                        Map.of(DATA, new PropertyValue.StringValue("Text")),
                        Map.of()),
                new CoreSourceCase(
                        "Icon",
                        ICON,
                        Map.of(ICON_VALUE, new PropertyValue.IconDataValue(
                                Optional.of(0xE5F9),
                                Optional.of("MaterialIcons"),
                                Optional.empty(),
                                false,
                                List.of())),
                        Map.of()));
    }

    private static CoreSourceCase gridViewCountSource() {
        return new CoreSourceCase(
                "GridView.count",
                GRID_VIEW,
                Map.of(
                        CROSS_AXIS_COUNT,
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of(CHILDREN, SlotCardinality.LIST));
    }

    private static Stream<WidgetDefinition> preIndexedStackDefinitions() {
        return preClipRSuperellipseDefinitions()
                .filter(definition -> !INDEXED_STACK.equals(definition.typeId()))
                .filter(definition -> !CLIP_RECT.equals(definition.typeId()))
                .filter(definition -> !CLIP_OVAL.equals(definition.typeId()))
                .filter(definition -> !CLIP_RRECT.equals(definition.typeId()))
                .filter(definition -> !CLIP_PATH.equals(definition.typeId()));
    }

    private static int preIndexedStackWidgetCount() {
        return Math.toIntExact(preIndexedStackDefinitions().count());
    }

    private static Stream<WidgetDefinition> preClipRectDefinitions() {
        return preClipRSuperellipseDefinitions()
                .filter(definition -> !CLIP_RECT.equals(definition.typeId()))
                .filter(definition -> !CLIP_OVAL.equals(definition.typeId()))
                .filter(definition -> !CLIP_RRECT.equals(definition.typeId()))
                .filter(definition -> !CLIP_PATH.equals(definition.typeId()));
    }

    private static int preClipRectWidgetCount() {
        return Math.toIntExact(preClipRectDefinitions().count());
    }

    private static Stream<WidgetDefinition> preClipOvalDefinitions() {
        return preClipRSuperellipseDefinitions()
                .filter(definition -> !CLIP_OVAL.equals(definition.typeId()))
                .filter(definition -> !CLIP_RRECT.equals(definition.typeId()))
                .filter(definition -> !CLIP_PATH.equals(definition.typeId()));
    }

    private static int preClipOvalWidgetCount() {
        return Math.toIntExact(preClipOvalDefinitions().count());
    }

    private static Stream<WidgetDefinition> preClipRRectDefinitions() {
        return preClipRSuperellipseDefinitions()
                .filter(definition -> !CLIP_RRECT.equals(definition.typeId()))
                .filter(definition -> !CLIP_PATH.equals(definition.typeId()));
    }

    private static int preClipRRectWidgetCount() {
        return Math.toIntExact(preClipRRectDefinitions().count());
    }

    private static Stream<WidgetDefinition> preClipPathDefinitions() {
        return preClipRSuperellipseDefinitions()
                .filter(definition -> !CLIP_PATH.equals(definition.typeId()));
    }

    private static int preClipPathWidgetCount() {
        return Math.toIntExact(preClipPathDefinitions().count());
    }

    private static final WidgetTypeId PHYSICAL_MODEL =
            PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE;

    private static final WidgetTypeId PHYSICAL_SHAPE = new WidgetTypeId("flutter.widgets.PhysicalShape");

    private static final WidgetTypeId REPAINT_BOUNDARY = new WidgetTypeId("flutter.widgets.RepaintBoundary");

    private static final WidgetTypeId IGNORE_POINTER = new WidgetTypeId("flutter.widgets.IgnorePointer");
    private static final WidgetTypeId ABSORB_POINTER = new WidgetTypeId("flutter.widgets.AbsorbPointer");
    private static final WidgetTypeId BLOCK_SEMANTICS = new WidgetTypeId("flutter.widgets.BlockSemantics");

    private static final WidgetTypeId MERGE_SEMANTICS = new WidgetTypeId("flutter.widgets.MergeSemantics");

    private static final WidgetTypeId INDEXED_SEMANTICS = new WidgetTypeId("flutter.widgets.IndexedSemantics");

    private static Stream<WidgetDefinition> preBadgeDefinitions() {
        return preCircleAvatarDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.Badge"));
    }

    private static Stream<WidgetDefinition> preCircleAvatarDefinitions() {
        return preLinearProgressIndicatorDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.CircleAvatar"));
    }

    private static Stream<WidgetDefinition> preLinearProgressIndicatorDefinitions() {
        return preCircularProgressIndicatorDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.LinearProgressIndicator"));
    }

    private static Stream<WidgetDefinition> preCircularProgressIndicatorDefinitions() {
        return preRefreshProgressIndicatorDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.CircularProgressIndicator"));
    }

    private static Stream<WidgetDefinition> preRefreshProgressIndicatorDefinitions() {
        return preRefreshIndicatorDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.RefreshProgressIndicator"));
    }

    private static Stream<WidgetDefinition> preRefreshIndicatorDefinitions() {
        return preTextButtonDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.RefreshIndicator"));
    }

    private static Stream<WidgetDefinition> preTextButtonDefinitions() {
        return preOutlinedButtonDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.TextButton"));
    }

    private static Stream<WidgetDefinition> preOutlinedButtonDefinitions() {
        return preFilledButtonDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.OutlinedButton"));
    }

    private static Stream<WidgetDefinition> preFilledButtonDefinitions() {
        return preFloatingActionButtonDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.FilledButton"));
    }

    private static Stream<WidgetDefinition> preFloatingActionButtonDefinitions() {
        return preIconButtonDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.FloatingActionButton"));
    }

    private static Stream<WidgetDefinition> preListTileDefinitions() {
        return preGestureDetectorDefinitions().filter(definition ->
                !Set.of("flutter.material.ListTile", "flutter.material.CheckboxListTile")
                        .contains(definition.typeId().value()));
    }

    @Test void crossFadePaletteInsertionPersistsTwoRequiredChildrenAndConsumesOneId() {
        var type=dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.TYPE;
        var allocations=new AtomicInteger();var planner=new FlutterDesignerPaletteDropPlanner();
        var destination=target("Column.children",COLUMN,CHILDREN);
        var accepted=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
            planner.plan(destination.document(),BUILT_INS,type,ROOT_ID,CHILDREN,0,FlutterImageAssetChoices.empty(),
                ()->{allocations.incrementAndGet();return NEW_ID;}));
        assertEquals(1,allocations.get());var widget=accepted.command().widget();assertEquals(type,widget.type());
        for(String name:List.of("firstChild","secondChild")){
            var child=assertInstanceOf(WidgetSlot.SingleSlot.class,widget.slots().get(new SlotName(name))).child().orElseThrow();
            assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedCrossFadeWidgetPropertySchema.starterChild(NEW_ID,name),child);
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(widget),BUILT_INS,TEXT,
                    widget.id(),new SlotName(name),0,FlutterImageAssetChoices.empty(),()->StableId.random()));
        }
    }

    private static Stream<WidgetDefinition> preGestureDetectorDefinitions() {
        return BUILT_INS.definitions().stream().filter(definition -> !Set.of("flutter.widgets.GestureDetector", "flutter.widgets.Listener", "flutter.widgets.MouseRegion", "flutter.widgets.Focus", "flutter.widgets.NotificationListener", "flutter.material.SwitchListTile", "flutter.material.RadioListTile", "flutter.material.ExpansionTile", "flutter.material.Tooltip", "flutter.material.TooltipVisibility", "flutter.material.TooltipTheme", "flutter.material.MenuItemButton", "flutter.material.MenuAnchor", "flutter.material.SubmenuButton", "flutter.material.MenuBar", "flutter.material.NavigationDrawer", "flutter.material.Drawer", "flutter.material.BottomAppBar", "flutter.material.BottomNavigationBar", "flutter.material.Material", "flutter.material.Scrollbar", "flutter.widgets.PageView", "flutter.widgets.PreferredSize", "flutter.widgets.Builder", GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE.value(), "flutter.widgets.CustomScrollView", "flutter.widgets.SliverToBoxAdapter", "flutter.widgets.SliverList", "flutter.widgets.SliverGrid", "flutter.widgets.SliverGrid.extent", "flutter.widgets.SliverList.builder", "flutter.widgets.SliverList.separated", "flutter.widgets.SliverList.delegate", "flutter.widgets.SliverGrid.builder", "flutter.widgets.SliverGrid.list", "flutter.widgets.SliverGrid.delegate", "flutter.widgets.SliverPadding", "flutter.widgets.SliverFillRemaining", "flutter.widgets.SliverFillViewport", "flutter.widgets.SliverFillViewport.delegate", "flutter.widgets.SliverFixedExtentList", "flutter.widgets.SliverFixedExtentList.builder", "flutter.widgets.SliverFixedExtentList.delegate", "flutter.widgets.SliverPrototypeExtentList", "flutter.widgets.SliverPrototypeExtentList.builder", "flutter.widgets.SliverPrototypeExtentList.delegate", "flutter.widgets.SliverVariedExtentList", "flutter.widgets.SliverVariedExtentList.builder", "flutter.widgets.SliverVariedExtentList.delegate", "flutter.widgets.SliverMainAxisGroup", "flutter.widgets.SliverCrossAxisGroup", "flutter.widgets.SliverCrossAxisExpanded", "flutter.widgets.SliverConstrainedCrossAxis", "flutter.widgets.SliverOpacity", "flutter.widgets.SliverIgnorePointer", "flutter.widgets.SliverOffstage", "flutter.widgets.SliverVisibility", "flutter.widgets.SliverVisibility.maintain", "flutter.widgets.SliverSafeArea", "flutter.widgets.SliverAnimatedOpacity", "flutter.widgets.LayoutBuilder", "flutter.widgets.OrientationBuilder", "flutter.widgets.DeviceOrientationBuilder", "flutter.widgets.DeviceOrientationBuilder.sliver", "flutter.widgets.ListenableBuilder", "flutter.widgets.ListenableBuilder.sliver", "flutter.widgets.AnimatedBuilder", "flutter.widgets.AnimatedBuilder.sliver", "flutter.widgets.ValueListenableBuilder", "flutter.widgets.ValueListenableBuilder.sliver", "flutter.widgets.TweenAnimationBuilder", "flutter.widgets.TweenAnimationBuilder.sliver", "flutter.widgets.AnimatedOpacity", "flutter.widgets.AnimatedAlign", "flutter.widgets.AnimatedPadding", "flutter.widgets.AnimatedSlide", "flutter.widgets.AnimatedScale", "flutter.widgets.AnimatedRotation", "flutter.widgets.AnimatedContainer", "flutter.widgets.AnimatedSize", "flutter.widgets.AnimatedPositioned", "flutter.widgets.AnimatedPositioned.fromRect", "flutter.widgets.AnimatedPositionedDirectional", "flutter.widgets.AnimatedDefaultTextStyle", "flutter.widgets.DefaultTextStyle", "flutter.widgets.DefaultTextStyle.merge", "flutter.widgets.DefaultTextStyleTransition", "flutter.widgets.ScaleTransition", "flutter.widgets.RotationTransition", "flutter.widgets.SizeTransition", "flutter.widgets.PositionedTransition", "flutter.widgets.RelativePositionedTransition", "flutter.widgets.DecoratedBoxTransition", "flutter.widgets.AlignTransition", "flutter.widgets.MatrixTransition", "flutter.widgets.ModalBarrier", "flutter.widgets.AnimatedModalBarrier", "flutter.widgets.SlideTransition", "flutter.widgets.FadeTransition", "flutter.widgets.SliverFadeTransition", "flutter.widgets.AnimatedPhysicalModel", "flutter.widgets.AnimatedFractionallySizedBox", "flutter.widgets.AnimatedCrossFade", "flutter.widgets.AnimatedSwitcher", "flutter.material.AnimatedTheme", "flutter.material.Theme", "flutter.material.AnimatedIcon", "flutter.widgets.FadeInImage", "flutter.widgets.RawImage", "flutter.widgets.ColorFiltered", "flutter.widgets.ImageFiltered", "flutter.widgets.BackdropFilter", "flutter.widgets.BackdropFilter.grouped", "flutter.widgets.BackdropGroup", "flutter.widgets.ShaderMask", "flutter.widgets.CustomPaint", "flutter.widgets.CustomSingleChildLayout", "flutter.widgets.CustomMultiChildLayout", "flutter.widgets.LayoutId", "flutter.widgets.Flow", "flutter.widgets.Flow.unwrapped", "flutter.widgets.Table", "flutter.widgets.TableRow", "flutter.widgets.TableCell", "flutter.material.DataTable", "flutter.material.DataColumn", "flutter.material.DataRow", "flutter.material.DataRow.byIndex", "flutter.material.DataCell", "flutter.material.DataCell.empty", "flutter.widgets.SliverLayoutBuilder", "flutter.widgets.SliverPersistentHeader", "flutter.widgets.SliverResizingHeader", "flutter.widgets.PinnedHeaderSliver", "flutter.widgets.SliverFloatingHeader", "flutter.material.SliverAppBar", "flutter.material.SliverAppBar.medium", "flutter.material.SliverAppBar.large", "flutter.material.FlexibleSpaceBar", "flutter.material.FlexibleSpaceBarSettings").contains(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preRadioGroupDefinitions() {
        return preListTileDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.widgets.RadioGroup"));
    }

    private static Stream<WidgetDefinition> preRadioDefinitions() {
        return preRadioGroupDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.Radio"));
    }

    private static Stream<WidgetDefinition> preRangeSliderDefinitions() {
        return preRadioDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.RangeSlider"));
    }

    private static Stream<WidgetDefinition> preSliderDefinitions() {
        return preRangeSliderDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.Slider"));
    }

    private static Stream<WidgetDefinition> preSwitchDefinitions() {
        return preSliderDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.Switch"));
    }

    private static Stream<WidgetDefinition> preCheckboxDefinitions() {
        return preSwitchDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.Checkbox"));
    }

    private static Stream<WidgetDefinition> preIconButtonDefinitions() {
        return preCheckboxDefinitions().filter(definition -> !definition.typeId().value().equals("flutter.material.IconButton"));
    }

    private static Stream<WidgetDefinition> preCardDefinitions() {
        return preBadgeDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.Card"));
    }

    private static Stream<WidgetDefinition> preVerticalDividerDefinitions() {
        return preCardDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.VerticalDivider"));
    }

    private static Stream<WidgetDefinition> preDividerDefinitions() {
        return preVerticalDividerDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.material.Divider"));
    }

    private static Stream<WidgetDefinition> preImageIconDefinitions() {
        return preDividerDefinitions().filter(definition ->
                !definition.typeId().value().equals("flutter.widgets.ImageIcon"));
    }

    private static Stream<WidgetDefinition> preIconThemeDefinitions() {
        return preImageIconDefinitions().filter(definition ->
                !"flutter.widgets.IconTheme".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preDefaultSelectionStyleDefinitions() {
        return preIconThemeDefinitions().filter(definition ->
                !"flutter.widgets.DefaultSelectionStyle".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preDefaultTextHeightBehaviorDefinitions() {
        return preDefaultSelectionStyleDefinitions().filter(definition ->
                !"flutter.widgets.DefaultTextHeightBehavior".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preTickerModeDefinitions() {
        return preDefaultTextHeightBehaviorDefinitions().filter(definition ->
                !"flutter.widgets.TickerMode".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preVisibilityDefinitions() {
        return preTickerModeDefinitions().filter(definition ->
                !"flutter.widgets.Visibility".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preExcludeFocusTraversalDefinitions() {
        return preVisibilityDefinitions().filter(definition ->
                !"flutter.widgets.ExcludeFocusTraversal".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preExcludeFocusDefinitions() {
        return preExcludeFocusTraversalDefinitions().filter(definition ->
                !"flutter.widgets.ExcludeFocus".equals(definition.typeId().value()));
    }

    private static Stream<WidgetDefinition> preIndexedSemanticsDefinitions() {
        return preExcludeFocusDefinitions().filter(definition -> !INDEXED_SEMANTICS.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> preMergeSemanticsDefinitions() {
        return preIndexedSemanticsDefinitions().filter(definition -> !MERGE_SEMANTICS.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> preBlockSemanticsDefinitions() {
        return preMergeSemanticsDefinitions().filter(definition -> !BLOCK_SEMANTICS.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> preAbsorbPointerDefinitions() {
        return preBlockSemanticsDefinitions().filter(definition -> !ABSORB_POINTER.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> preIgnorePointerDefinitions() {
        return preAbsorbPointerDefinitions().filter(definition -> !IGNORE_POINTER.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> preRepaintBoundaryDefinitions() {
        return preIgnorePointerDefinitions().filter(definition -> !REPAINT_BOUNDARY.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> prePhysicalShapeDefinitions() {
        return preRepaintBoundaryDefinitions().filter(definition -> !PHYSICAL_SHAPE.equals(definition.typeId()));
    }

    private static Stream<WidgetDefinition> prePhysicalModelDefinitions() {
        return prePhysicalShapeDefinitions()
                .filter(definition -> !PHYSICAL_MODEL.equals(definition.typeId()));
    }

    private static int prePhysicalModelWidgetCount() {
        return Math.toIntExact(prePhysicalModelDefinitions().count());
    }

    private static Stream<WidgetDefinition> preClipRSuperellipseDefinitions() {
        return prePhysicalModelDefinitions()
                .filter(definition -> !CLIP_RSUPERELLIPSE.equals(definition.typeId()));
    }

    private static int preClipRSuperellipseWidgetCount() {
        return Math.toIntExact(preClipRSuperellipseDefinitions().count());
    }

    private static MatrixTargetCase target(
            String name,
            WidgetTypeId parentType,
            SlotName slot) {
        if ((parentType.value().equals("flutter.material.FilledButton")
                || parentType.value().equals("flutter.material.FloatingActionButton")) && slot.value().equals("icon")) {
            return new MatrixTargetCase(name, document(parentWithSlot(parentType, slot, WidgetSlot.SingleSlot.empty())), slot);
        }
        return new MatrixTargetCase(name, document(prototype(parentType)), slot);
    }

    private static MatrixTargetCase occupiedTarget(
            String name,
            WidgetTypeId parentType,
            SlotName slot) {
        SlotDefinition definition = definition(parentType).slot(slot).orElseThrow();
        WidgetNode target = text(FIRST_ID, "existing wrap target");
        WidgetNode parent = definition.cardinality() == SlotCardinality.SINGLE
                ? singleParent(parentType, slot, target)
                : listParent(parentType, slot, List.of(target));
        return new MatrixTargetCase(name, document(parent), slot);
    }

    @Test
    void sliverPaddingCreatesWithInsetsAndAcceptsOnlyAnUnoccupiedNestedSliverSlot() {
        var type = new WidgetTypeId("flutter.widgets.SliverPadding");
        var slivers = new SlotName("slivers"); var slot = new SlotName("sliver");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.SliverPaddingWidgetPropertySchema.padding().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("padding")));
        var withPadding = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                new WidgetTypeId("flutter.widgets.SliverList"), new WidgetTypeId("flutter.widgets.SliverGrid.builder"))) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withPadding), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(TEXT, COLUMN, EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withPadding), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void sliverIgnorePointerCreatesWithNativeDefaultsAndAcceptsOnlyAnUnoccupiedNestedSliverSlot() {
        var type = new WidgetTypeId("flutter.widgets.SliverIgnorePointer");
        var slivers = new SlotName("slivers"); var slot = new SlotName("sliver");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertTrue(created.properties().isEmpty());
        var withIgnore = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                new WidgetTypeId("flutter.widgets.SliverList"), new WidgetTypeId("flutter.widgets.SliverGrid.builder"))) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withIgnore), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(TEXT, COLUMN, EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withIgnore), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void sliverOffstageCreatesWithNativeDefaultsAndAcceptsOnlyAnUnoccupiedNestedSliverSlot() {
        var type = new WidgetTypeId("flutter.widgets.SliverOffstage");
        var slivers = new SlotName("slivers"); var slot = new SlotName("sliver");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertTrue(created.properties().isEmpty());
        var withIgnore = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                new WidgetTypeId("flutter.widgets.SliverList"), new WidgetTypeId("flutter.widgets.SliverGrid.builder"))) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withIgnore), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(TEXT, COLUMN, EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withIgnore), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void layoutBuilderInsertsWithTypedEmptyPresetAndDoesNotInventChildSlots() {
        var type = dev.flutter.netbeans.designer.catalog.LayoutBuilderWidgetPropertySchema.TYPE;
        for (String parent : List.of("Column", "Center")) {
            var slot = new SlotName(parent.equals("Column") ? "children" : "child");
            WidgetSlot empty = parent.equals("Column") ? new WidgetSlot.ListSlot(List.of()) : WidgetSlot.SingleSlot.empty();
            var destination = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent), Map.of(), Map.of(slot, empty));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(destination), BUILT_INS, type, ROOT_ID, slot, 0, () -> NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("empty"), created.properties().get(new PropertyName("builder")));
            assertTrue(created.slots().isEmpty());
            for (String invented : List.of("child", "children", "sliver")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(created), BUILT_INS, type, NEW_ID, new SlotName(invented), 0, () -> FIRST_ID));
            }
        }
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
    }

    @Test
    void orientationBuilderInsertsWithTypedEmptyPresetAndDoesNotInventChildSlots() {
        var type = dev.flutter.netbeans.designer.catalog.OrientationBuilderWidgetPropertySchema.TYPE;
        for (String parent : List.of("Column", "Center")) {
            var slot = new SlotName(parent.equals("Column") ? "children" : "child");
            WidgetSlot empty = parent.equals("Column") ? new WidgetSlot.ListSlot(List.of()) : WidgetSlot.SingleSlot.empty();
            var destination = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent), Map.of(), Map.of(slot, empty));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(destination), BUILT_INS, type, ROOT_ID, slot, 0, () -> NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("empty"), created.properties().get(new PropertyName("builder")));
            assertTrue(created.slots().isEmpty());
            for (String invented : List.of("child", "children", "sliver")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(created), BUILT_INS, type, NEW_ID, new SlotName(invented), 0, () -> FIRST_ID));
            }
        }
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
    }

    @Test
    void listenableBuilderInsertsBothProjectionsAndAcceptsOnlyMatchingChildProtocol() {
        for(boolean sliver:List.of(false,true)) {
            var type=sliver?dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.SLIVER_TYPE
                    :dev.flutter.netbeans.designer.catalog.ListenableBuilderWidgetPropertySchema.TYPE;
            var slot=new SlotName(sliver?"slivers":"children");
            var root=new WidgetNode(ROOT_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),
                    Map.of(),Map.of(slot,new WidgetSlot.ListSlot(List.of())));
            var created=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(root),BUILT_INS,type,ROOT_ID,slot,0,()->NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("none"),created.properties().get(new PropertyName("listenable")));
            assertEquals(new PropertyValue.StringValue("child"),created.properties().get(new PropertyName("builder")));
            assertEquals(Set.of(new SlotName("child")),created.slots().keySet());
            var attached=new WidgetNode(root.id(),root.type(),root.properties(),Map.of(slot,new WidgetSlot.ListSlot(List.of(created))));
            var good=new WidgetTypeId(sliver?"flutter.widgets.SliverToBoxAdapter":"flutter.widgets.Text");
            var wrong=new WidgetTypeId(sliver?"flutter.widgets.Text":"flutter.widgets.SliverToBoxAdapter");
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(attached),BUILT_INS,good,NEW_ID,new SlotName("child"),0,()->FIRST_ID));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(attached),BUILT_INS,wrong,NEW_ID,new SlotName("child"),0,()->FIRST_ID));
        }
    }

    @Test
    void animatedBuilderInsertsBothProjectionsAndAcceptsOnlyMatchingChildProtocol() {
        for(boolean sliver:List.of(false,true)) {
            var type=sliver?dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.SLIVER_TYPE
                    :dev.flutter.netbeans.designer.catalog.AnimatedBuilderWidgetPropertySchema.TYPE;
            var slot=new SlotName(sliver?"slivers":"children");
            var root=new WidgetNode(ROOT_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),
                    Map.of(),Map.of(slot,new WidgetSlot.ListSlot(List.of())));
            var created=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(root),BUILT_INS,type,ROOT_ID,slot,0,()->NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("none"),created.properties().get(new PropertyName("animation")));
            assertEquals(new PropertyValue.StringValue("child"),created.properties().get(new PropertyName("builder")));
            assertEquals(Set.of(new SlotName("child")),created.slots().keySet());
            var attached=new WidgetNode(root.id(),root.type(),root.properties(),Map.of(slot,new WidgetSlot.ListSlot(List.of(created))));
            var good=new WidgetTypeId(sliver?"flutter.widgets.SliverToBoxAdapter":"flutter.widgets.Text");
            var wrong=new WidgetTypeId(sliver?"flutter.widgets.Text":"flutter.widgets.SliverToBoxAdapter");
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(attached),BUILT_INS,good,NEW_ID,new SlotName("child"),0,()->FIRST_ID));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(attached),BUILT_INS,wrong,NEW_ID,new SlotName("child"),0,()->FIRST_ID));
        }
    }

    @Test
    void valueListenableBuilderInsertsBothProjectionsAndAcceptsOnlyMatchingChildProtocol() {
        for(boolean sliver:List.of(false,true)) {
            var type=sliver?dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE
                    :dev.flutter.netbeans.designer.catalog.ValueListenableBuilderWidgetPropertySchema.TYPE;
            var slot=new SlotName(sliver?"slivers":"children");
            var root=new WidgetNode(ROOT_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),
                    Map.of(),Map.of(slot,new WidgetSlot.ListSlot(List.of())));
            var created=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(root),BUILT_INS,type,ROOT_ID,slot,0,()->NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("constant"),created.properties().get(new PropertyName("valueListenable")));
            assertEquals(new PropertyValue.StringValue("child"),created.properties().get(new PropertyName("builder")));
            assertEquals(Set.of(new SlotName("child")),created.slots().keySet());
            var attached=new WidgetNode(root.id(),root.type(),root.properties(),Map.of(slot,new WidgetSlot.ListSlot(List.of(created))));
            var good=new WidgetTypeId(sliver?"flutter.widgets.SliverToBoxAdapter":"flutter.widgets.Text");
            var wrong=new WidgetTypeId(sliver?"flutter.widgets.Text":"flutter.widgets.SliverToBoxAdapter");
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(attached),BUILT_INS,good,NEW_ID,new SlotName("child"),0,()->FIRST_ID));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(attached),BUILT_INS,wrong,NEW_ID,new SlotName("child"),0,()->FIRST_ID));
        }
    }

    @Test
    void deviceOrientationBuilderInsertsWithTypedEmptyPresetAndDoesNotInventChildSlots() {
        var type = dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.TYPE;
        for (String parent : List.of("Column", "Center")) {
            var slot = new SlotName(parent.equals("Column") ? "children" : "child");
            WidgetSlot empty = parent.equals("Column") ? new WidgetSlot.ListSlot(List.of()) : WidgetSlot.SingleSlot.empty();
            var destination = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent), Map.of(), Map.of(slot, empty));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(destination), BUILT_INS, type, ROOT_ID, slot, 0, () -> NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("empty"), created.properties().get(new PropertyName("builder")));
            assertTrue(created.slots().isEmpty());
            for (String invented : List.of("child", "children", "sliver")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(created), BUILT_INS, type, NEW_ID, new SlotName(invented), 0, () -> FIRST_ID));
            }
        }
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
    }

    @Test
    void sliverLayoutBuilderInsertsIntoEverySliverListButCannotOwnDesignerChildren() {
        var type = dev.flutter.netbeans.designer.catalog.SliverLayoutBuilderWidgetPropertySchema.TYPE;
        var slot = new SlotName("slivers");
        for (String parent : List.of("CustomScrollView", "SliverMainAxisGroup", "SliverCrossAxisGroup")) {
            var destination = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent), Map.of(),
                    Map.of(slot, new WidgetSlot.ListSlot(List.of())));
            var docRoot = parent.equals("CustomScrollView") ? destination :
                    new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                            Map.of(slot, new WidgetSlot.ListSlot(List.of(destination))));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(docRoot), BUILT_INS, type, ROOT_ID, slot, 0, () -> NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("empty"), created.properties().get(new PropertyName("builder")));
            assertTrue(created.slots().isEmpty());
            var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                    Map.of(slot, new WidgetSlot.ListSlot(List.of(created))));
            for (String invented : List.of("child", "sliver", "children")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(viewport), BUILT_INS, type, NEW_ID, new SlotName(invented), 0, () -> FIRST_ID));
            }
        }
    }

    @Test
    void deviceOrientationBuilderSliverInsertsIntoEverySliverListButCannotOwnDesignerChildren() {
        var type = dev.flutter.netbeans.designer.catalog.DeviceOrientationBuilderWidgetPropertySchema.SLIVER_TYPE;
        var slot = new SlotName("slivers");
        for (String parent : List.of("CustomScrollView", "SliverMainAxisGroup", "SliverCrossAxisGroup")) {
            var destination = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent), Map.of(),
                    Map.of(slot, new WidgetSlot.ListSlot(List.of())));
            var docRoot = parent.equals("CustomScrollView") ? destination :
                    new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                            Map.of(slot, new WidgetSlot.ListSlot(List.of(destination))));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(docRoot), BUILT_INS, type, ROOT_ID, slot, 0, () -> NEW_ID)).command().widget();
            assertEquals(new PropertyValue.StringValue("empty"), created.properties().get(new PropertyName("builder")));
            assertTrue(created.slots().isEmpty());
            var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                    Map.of(slot, new WidgetSlot.ListSlot(List.of(created))));
            for (String invented : List.of("child", "sliver", "children")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(viewport), BUILT_INS, type, NEW_ID, new SlotName(invented), 0, () -> FIRST_ID));
            }
        }
    }

    @Test
    void flexibleSpaceBarSettingsWrapsExistingBoxAtomicallyAndNeverInventsRequiredChild() {
        var type = dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE;
        var leaf = new WidgetNode(FIRST_ID, TEXT, Map.of(new PropertyName("data"), new PropertyValue.StringValue("Title")), Map.of());
        var root = new WidgetNode(ROOT_ID, COLUMN, Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(leaf))));
        var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(document(root), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID)).command();
        assertEquals(FIRST_ID, command.widgetId());assertEquals(CHILD,command.wrapperSlot());
        assertEquals(type,command.wrapper().type());assertEquals(4,command.wrapper().properties().size());
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(root), BUILT_INS, type, ROOT_ID, CHILDREN, 1, () -> NEW_ID));
        var empty = new WidgetNode(ROOT_ID, COLUMN, Map.of(), Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of())));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(empty), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
    }

    @Test
    void flexibleSpaceBarInsertsIntoFourAppBarsWithIndependentBoxSlots() {
        var type = dev.flutter.netbeans.designer.catalog.FlexibleSpaceBarWidgetPropertySchema.TYPE;
        var owners = new ArrayList<>(List.of("flutter.material.AppBar"));
        owners.addAll(dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.TYPES);
        for (String ownerType : owners) {
            var owner = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                    BUILT_INS.find(new WidgetTypeId(ownerType)).orElseThrow(), ROOT_ID);
            var root = ownerType.endsWith(".AppBar")
                    ? new WidgetNode(StableId.random(), SCAFFOLD, Map.of(), Map.of(APP_BAR_SLOT, WidgetSlot.SingleSlot.of(owner)))
                    : new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                        Map.of(new SlotName("slivers"), new WidgetSlot.ListSlot(List.of(owner))));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(root), BUILT_INS, type, ROOT_ID, FLEXIBLE_SPACE, 0, () -> NEW_ID)).command().widget();
            assertTrue(created.properties().isEmpty());
            assertEquals(Set.of(TITLE, new SlotName("background")), created.slots().keySet());
            // A root may receive settings from an outer application; no generated wrapper is invented.
            for (String name : List.of("title", "background")) {
                var slot = new SlotName(name);
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(created), BUILT_INS, TEXT, NEW_ID, slot, 0, () -> FIRST_ID));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(created), BUILT_INS, new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), NEW_ID, slot, 0, () -> FIRST_ID));
                var occupied = new WidgetNode(NEW_ID, type, Map.of(), Map.of(slot, WidgetSlot.SingleSlot.of(
                        dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(TEXT).orElseThrow(), FIRST_ID))));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(occupied), BUILT_INS, TEXT, NEW_ID, slot, 0, () -> StableId.random()));
            }
        }
    }

    @Test
    void sliverAppBarsInsertIntoSliverListsAndEnforceAllFiveSlotContracts() {
        var slivers = new SlotName("slivers");
        for (String typeName : dev.flutter.netbeans.designer.catalog.SliverAppBarWidgetPropertySchema.TYPES) {
            var type = new WidgetTypeId(typeName);
            for (String parent : List.of("CustomScrollView", "SliverMainAxisGroup", "SliverCrossAxisGroup")) {
                var owner = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent),
                        Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
                var viewport = parent.equals("CustomScrollView") ? owner : new WidgetNode(StableId.random(),
                        new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                        Map.of(slivers, new WidgetSlot.ListSlot(List.of(owner))));
                var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID))
                        .command().widget();
                assertEquals(type, created.type());
                assertTrue(created.properties().isEmpty(), "Native constructor defaults must remain unset");
                assertEquals(Set.of(LEADING, TITLE, ACTIONS, FLEXIBLE_SPACE, BOTTOM), created.slots().keySet());
                var root = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"),
                        Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
                for (var slot : List.of(LEADING, TITLE, ACTIONS, FLEXIBLE_SPACE, BOTTOM)) {
                    var childType = slot.equals(BOTTOM) ? APP_BAR : TEXT;
                    var child = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                            planner.plan(document(root), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID))
                            .command().widget();
                    assertEquals(childType, child.type());
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                            planner.plan(document(root), BUILT_INS, type, NEW_ID, slot, 0, () -> FIRST_ID));
                }
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(root), BUILT_INS, TEXT, NEW_ID, BOTTOM, 0, () -> FIRST_ID));
            }
            var box = new WidgetNode(ROOT_ID, COLUMN, Map.of(),
                    Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(box), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
            var scaffold = new WidgetNode(ROOT_ID, SCAFFOLD, Map.of(),
                    Map.of(APP_BAR_SLOT, new WidgetSlot.SingleSlot(Optional.empty())));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(scaffold), BUILT_INS, type, ROOT_ID, APP_BAR_SLOT, 0, () -> NEW_ID));
        }
    }

    @Test
    void floatingHeaderInsertsWithExplicitRequiredSeedAndRejectsAppendingToOccupiedChild() {
        var type=dev.flutter.netbeans.designer.catalog.SliverFloatingHeaderWidgetPropertySchema.TYPE;
        var slivers=new SlotName("slivers");
        for(String parent:List.of("CustomScrollView","SliverMainAxisGroup","SliverCrossAxisGroup")) {
            var owner=new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets."+parent),Map.of(),
                    Map.of(slivers,new WidgetSlot.ListSlot(List.of())));
            var viewport=parent.equals("CustomScrollView")?owner:new WidgetNode(StableId.random(),
                    new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),Map.of(slivers,new WidgetSlot.ListSlot(List.of(owner))));
            var created=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport),BUILT_INS,type,ROOT_ID,slivers,0,()->NEW_ID)).command().widget();
            assertTrue(created.properties().isEmpty());assertEquals(1,created.slots().size());
            assertTrue(((WidgetSlot.SingleSlot)created.slots().get(new SlotName("child"))).child().isPresent());
            var root=new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                    Map.of(slivers,new WidgetSlot.ListSlot(List.of(created))));
            for(String slot:List.of("child")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(root),BUILT_INS,new WidgetTypeId("flutter.widgets.Text"),NEW_ID,new SlotName(slot),0,()->FIRST_ID));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(root),BUILT_INS,type,NEW_ID,new SlotName(slot),0,()->FIRST_ID));
            }
        }
    }

    @Test
    void pinnedHeaderCreatesOptionalChildWithExactDestinations() {
        var type=dev.flutter.netbeans.designer.catalog.PinnedHeaderSliverWidgetSchema.TYPE;
        var slivers=new SlotName("slivers");
        for(String parent:List.of("CustomScrollView","SliverMainAxisGroup","SliverCrossAxisGroup")) {
            var owner=new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets."+parent),Map.of(),
                    Map.of(slivers,new WidgetSlot.ListSlot(List.of())));
            var viewport=parent.equals("CustomScrollView")?owner:new WidgetNode(StableId.random(),
                    new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),Map.of(slivers,new WidgetSlot.ListSlot(List.of(owner))));
            var created=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport),BUILT_INS,type,ROOT_ID,slivers,0,()->NEW_ID)).command().widget();
            assertTrue(created.properties().isEmpty());assertEquals(1,created.slots().size());
            var root=new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                    Map.of(slivers,new WidgetSlot.ListSlot(List.of(created))));
            for(String slot:List.of("child")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(root),BUILT_INS,new WidgetTypeId("flutter.widgets.Text"),NEW_ID,new SlotName(slot),0,()->FIRST_ID));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(root),BUILT_INS,type,NEW_ID,new SlotName(slot),0,()->FIRST_ID));
            }
        }
    }

    @Test
    void resizingHeaderCreatesThreeOptionalBoxSlotsWithExactDestinations() {
        var type=dev.flutter.netbeans.designer.catalog.SliverResizingHeaderWidgetSchema.TYPE;
        var slivers=new SlotName("slivers");
        for(String parent:List.of("CustomScrollView","SliverMainAxisGroup","SliverCrossAxisGroup")) {
            var owner=new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets."+parent),Map.of(),
                    Map.of(slivers,new WidgetSlot.ListSlot(List.of())));
            var viewport=parent.equals("CustomScrollView")?owner:new WidgetNode(StableId.random(),
                    new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),Map.of(slivers,new WidgetSlot.ListSlot(List.of(owner))));
            var created=assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport),BUILT_INS,type,ROOT_ID,slivers,0,()->NEW_ID)).command().widget();
            assertTrue(created.properties().isEmpty());assertEquals(3,created.slots().size());
            var root=new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                    Map.of(slivers,new WidgetSlot.ListSlot(List.of(created))));
            for(String slot:dev.flutter.netbeans.designer.catalog.SliverResizingHeaderWidgetSchema.SLOTS) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                        planner.plan(document(root),BUILT_INS,new WidgetTypeId("flutter.widgets.Text"),NEW_ID,new SlotName(slot),0,()->FIRST_ID));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(root),BUILT_INS,type,NEW_ID,new SlotName(slot),0,()->FIRST_ID));
            }
        }
    }

    @Test
    void sliverPersistentHeaderInsertsIntoEverySliverListButCannotOwnDesignerChildren() {
        var type = dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.TYPE;
        var slot = new SlotName("slivers");
        for (String parent : List.of("CustomScrollView", "SliverMainAxisGroup", "SliverCrossAxisGroup")) {
            var destination = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parent), Map.of(),
                    Map.of(slot, new WidgetSlot.ListSlot(List.of())));
            var docRoot = parent.equals("CustomScrollView") ? destination :
                    new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                            Map.of(slot, new WidgetSlot.ListSlot(List.of(destination))));
            var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(docRoot), BUILT_INS, type, ROOT_ID, slot, 0, () -> NEW_ID)).command().widget();
            assertEquals(dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.INITIAL_DELEGATE, created.properties().get(new PropertyName("delegate")));
            assertTrue(created.slots().isEmpty());
            var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                    Map.of(slot, new WidgetSlot.ListSlot(List.of(created))));
            for (String invented : List.of("child", "sliver", "children")) {
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(viewport), BUILT_INS, type, NEW_ID, new SlotName(invented), 0, () -> FIRST_ID));
            }
        }
    }

    @Test
    void animatedSlideCreatesZeroOffsetAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedSlide");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedSlideWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("offset")));
        var withSlide = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withSlide), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withSlide), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void animatedScaleCreatesUnitScaleAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedScale");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedScaleWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("scale")));
        var withSlide = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withSlide), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withSlide), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void animatedRotationCreatesZeroTurnsAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedRotation");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedRotationWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("turns")));
        var withSlide = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withSlide), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withSlide), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void animatedContainerCreatesDurationAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedContainer");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedContainerWidgetPropertySchema.properties().get(13).creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("durationUs")));
        var withSlide = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withSlide), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withSlide), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void animatedPositionedVariantsWrapExistingStackChildrenOnly() {
        var child = WidgetNodePrototypeFactory.create(BUILT_INS.find(TEXT).orElseThrow(), FIRST_ID);
        for (var type : dev.flutter.netbeans.designer.catalog.AnimatedPositionedWidgetPropertySchema.TYPES) {
            for (var parentType : List.of(STACK, INDEXED_STACK, COLUMN)) {
                var root = new WidgetNode(ROOT_ID, parentType, Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(child))));
                var result = planner.plan(document(root), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID);
                if (parentType.equals(STACK)) {
                    var wrapped = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class, result).command();
                    assertEquals(type, wrapped.wrapper().type());
                    assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(300000)),
                            wrapped.wrapper().properties().get(new PropertyName("durationUs")));
                } else assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
                var empty = new WidgetNode(ROOT_ID, parentType, Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(empty), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
            }
        }
    }

    @Test
    void animatedSizeCreatesDurationAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedSize");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedSizeWidgetPropertySchema.properties().get(2).creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("durationUs")));
        var withSlide = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withSlide), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withSlide), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void animatedPaddingCreatesInsetAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedPadding");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedPaddingWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("padding")));
        var withPadding = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withPadding), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withPadding), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }
    @Test
    void animatedAlignCreatesCenteredAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedAlign");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedAlignWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("alignment")));
        var withOpacity = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withOpacity), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withOpacity), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }    @Test
    void animatedOpacityCreatesOpaqueAndAcceptsOnlyAnUnoccupiedNestedChildSlot() {
        var type = new WidgetTypeId("flutter.widgets.AnimatedOpacity");
        var children = new SlotName("children"); var slot = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(children, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, children, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.AnimatedOpacityWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("opacity")));
        var withOpacity = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, TEXT, COLUMN)) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withOpacity), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withOpacity), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(children, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }
    @Test
    void sliverAnimatedOpacityCreatesOpaqueAndAcceptsOnlyAnUnoccupiedNestedSliverSlot() {
        var type = new WidgetTypeId("flutter.widgets.SliverAnimatedOpacity");
        var slivers = new SlotName("slivers"); var slot = new SlotName("sliver");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.SliverAnimatedOpacityWidgetPropertySchema.properties().getFirst().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("opacity")));
        var withOpacity = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                new WidgetTypeId("flutter.widgets.SliverList"), new WidgetTypeId("flutter.widgets.SliverGrid.builder"))) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withOpacity), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(TEXT, COLUMN, EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withOpacity), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void sliverOpacityCreatesOpaqueAndAcceptsOnlyAnUnoccupiedNestedSliverSlot() {
        var type = new WidgetTypeId("flutter.widgets.SliverOpacity");
        var slivers = new SlotName("slivers"); var slot = new SlotName("sliver");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertEquals(dev.flutter.netbeans.designer.catalog.SliverOpacityWidgetPropertySchema.opacity().creationDefault().orElseThrow(),
                created.properties().get(new PropertyName("opacity")));
        var withOpacity = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(type, new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                new WidgetTypeId("flutter.widgets.SliverList"), new WidgetTypeId("flutter.widgets.SliverGrid.builder"))) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withOpacity), BUILT_INS, childType, NEW_ID, slot, 0, () -> FIRST_ID));
        }
        for (var rejectedType : List.of(TEXT, COLUMN, EXPANDED)) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(withOpacity), BUILT_INS, rejectedType, NEW_ID, slot, 0, () -> FIRST_ID));
        var nested = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(slot, WidgetSlot.SingleSlot.of(nested)));
        var fullViewport = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(fullViewport), BUILT_INS, type, NEW_ID, slot, 0, () -> StableId.random()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(fullViewport), BUILT_INS, type, FIRST_ID, slot, 0, () -> StableId.random()));
    }

    @Test
    void mainAxisGroupsCreateEmptyAndNestOnlyInSliverDestinations() {
        var type = new WidgetTypeId("flutter.widgets.SliverMainAxisGroup");
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertTrue(added.properties().isEmpty());
        assertEquals(List.of(), ((WidgetSlot.ListSlot) added.slots().get(slivers)).children());
        var withGroup = new WidgetNode(ROOT_ID, viewport.type(), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of(added))));
        for (var definition : BUILT_INS.definitions()) {
            var result = planner.plan(document(withGroup), BUILT_INS, definition.typeId(), NEW_ID, slivers, 0, () -> FIRST_ID);
            assertEquals(dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.isSliverWidget(definition) && dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.supportsDirectPrototypeInsertion(definition),
                    result instanceof FlutterDesignerPaletteDropPlanner.Accepted, definition.typeId().value());
        }
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
    }

    @Test
    void sliverVisibilityConstructorsWrapsExistingSliversInEveryAdmittedSliverDestination() {
        for (var type : List.of(dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.TYPE,
                dev.flutter.netbeans.designer.catalog.SliverVisibilityWidgetPropertySchema.MAINTAIN_TYPE)) {
        var slivers = new SlotName("slivers"); var sliver = new SlotName("sliver");
        var child = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                BUILT_INS.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), FIRST_ID);
        for (String parentType : List.of("CustomScrollView", "SliverCrossAxisGroup", "SliverMainAxisGroup")) {
            var parent = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parentType), Map.of(),
                    Map.of(slivers, new WidgetSlot.ListSlot(List.of(child))));
            var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command();
            assertEquals(FIRST_ID, command.widgetId()); assertEquals(sliver, command.wrapperSlot());
            assertTrue(command.wrapper().properties().isEmpty());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 1, () -> NEW_ID));
            var empty = new WidgetNode(ROOT_ID, parent.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(empty), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
        }
        var wrapper = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), ROOT_ID);
        wrapper = new WidgetNode(ROOT_ID, type, wrapper.properties(), Map.of(sliver, WidgetSlot.SingleSlot.of(child)));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(document(wrapper), BUILT_INS, type, ROOT_ID, sliver, 0, () -> NEW_ID));
        var expandedType = dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.TYPE;
        var expanded = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(expandedType).orElseThrow(), FIRST_ID);
        var parent = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.SliverCrossAxisGroup"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of(expanded))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID),
                "Wrapping an Expanded would move it away from its required group parent.");
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of(text(FIRST_ID, "box")))), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
        }
    }

    @Test
    void sliverSafeAreaWrapsExistingSliversInEveryAdmittedSliverDestination() {
        var type = dev.flutter.netbeans.designer.catalog.SliverSafeAreaWidgetPropertySchema.TYPE;
        var slivers = new SlotName("slivers"); var sliver = new SlotName("sliver");
        var child = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                BUILT_INS.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), FIRST_ID);
        for (String parentType : List.of("CustomScrollView", "SliverCrossAxisGroup", "SliverMainAxisGroup")) {
            var parent = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parentType), Map.of(),
                    Map.of(slivers, new WidgetSlot.ListSlot(List.of(child))));
            var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command();
            assertEquals(FIRST_ID, command.widgetId()); assertEquals(sliver, command.wrapperSlot());
            assertTrue(command.wrapper().properties().isEmpty());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 1, () -> NEW_ID));
            var empty = new WidgetNode(ROOT_ID, parent.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(empty), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
        }
        var wrapper = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), ROOT_ID);
        wrapper = new WidgetNode(ROOT_ID, type, wrapper.properties(), Map.of(sliver, WidgetSlot.SingleSlot.of(child)));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(document(wrapper), BUILT_INS, type, ROOT_ID, sliver, 0, () -> NEW_ID));
        var expandedType = dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.TYPE;
        var expanded = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(expandedType).orElseThrow(), FIRST_ID);
        var parent = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.SliverCrossAxisGroup"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of(expanded))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID),
                "Wrapping an Expanded would move it away from its required group parent.");
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of(text(FIRST_ID, "box")))), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
    }

    @Test
    void constrainedCrossAxisWrapsExistingSliversInEveryAdmittedSliverDestination() {
        var type = dev.flutter.netbeans.designer.catalog.SliverConstrainedCrossAxisWidgetPropertySchema.TYPE;
        var slivers = new SlotName("slivers"); var sliver = new SlotName("sliver");
        var child = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                BUILT_INS.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), FIRST_ID);
        for (String parentType : List.of("CustomScrollView", "SliverCrossAxisGroup", "SliverMainAxisGroup")) {
            var parent = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parentType), Map.of(),
                    Map.of(slivers, new WidgetSlot.ListSlot(List.of(child))));
            var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command();
            assertEquals(FIRST_ID, command.widgetId()); assertEquals(sliver, command.wrapperSlot());
            assertEquals(new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(120)),
                    command.wrapper().properties().get(new PropertyName("maxExtent")));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 1, () -> NEW_ID));
            var empty = new WidgetNode(ROOT_ID, parent.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(empty), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
        }
        var wrapper = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(type).orElseThrow(), ROOT_ID);
        wrapper = new WidgetNode(ROOT_ID, type, wrapper.properties(), Map.of(sliver, WidgetSlot.SingleSlot.of(child)));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                planner.plan(document(wrapper), BUILT_INS, type, ROOT_ID, sliver, 0, () -> NEW_ID));
        var expandedType = dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.TYPE;
        var expanded = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(expandedType).orElseThrow(), FIRST_ID);
        var parent = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.SliverCrossAxisGroup"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of(expanded))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID),
                "Wrapping an Expanded would move it away from its required group parent.");
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of(text(FIRST_ID, "box")))), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
    }

    @Test
    void crossAxisExpandedWrapsOnlyExistingDirectCrossGroupChildren() {
        var type = dev.flutter.netbeans.designer.catalog.SliverCrossAxisExpandedWidgetPropertySchema.TYPE;
        var slivers = new SlotName("slivers");
        var child = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(
                BUILT_INS.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), FIRST_ID);
        for (String parentType : List.of("SliverCrossAxisGroup", "SliverMainAxisGroup", "CustomScrollView")) {
            var group = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets." + parentType), Map.of(),
                    Map.of(slivers, new WidgetSlot.ListSlot(List.of(child))));
            var result = planner.plan(document(group), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID);
            if (parentType.equals("SliverCrossAxisGroup")) {
                var command = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class, result).command();
                assertEquals(FIRST_ID, command.widgetId()); assertEquals(new SlotName("sliver"), command.wrapperSlot());
                assertEquals(new PropertyValue.IntegerValue(java.math.BigInteger.ONE),
                        command.wrapper().properties().get(new PropertyName("flex")));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(group), BUILT_INS, type, ROOT_ID, slivers, 1, () -> NEW_ID));
                var empty = new WidgetNode(ROOT_ID, group.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(empty), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID));
            } else assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, result);
        }
    }

    @Test
    void crossAxisGroupsCreateEmptyAndNestOnlyInSliverDestinations() {
        var type = new WidgetTypeId("flutter.widgets.SliverCrossAxisGroup");
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertTrue(added.properties().isEmpty());
        assertEquals(List.of(), ((WidgetSlot.ListSlot) added.slots().get(slivers)).children());
        var withGroup = new WidgetNode(ROOT_ID, viewport.type(), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of(added))));
        for (var definition : BUILT_INS.definitions()) {
            var result = planner.plan(document(withGroup), BUILT_INS, definition.typeId(), NEW_ID, slivers, 0, () -> FIRST_ID);
            assertEquals(dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.isSliverWidget(definition) && dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.supportsDirectPrototypeInsertion(definition),
                    result instanceof FlutterDesignerPaletteDropPlanner.Accepted, definition.typeId().value());
        }
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(parent(COLUMN, List.of())), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
    }

    @Test
    void prototypeExtentVariantsCreateWithSeparateMeasurementAndVisibleChildrenSlots() {
        var slivers = new SlotName("slivers"); var measurement = new SlotName("prototypeItem");
        var viewport = new WidgetNode(ROOT_ID,new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                Map.of(slivers,new WidgetSlot.ListSlot(List.of())));
        for(var kind : dev.flutter.netbeans.designer.catalog.SliverPrototypeExtentListWidgetPropertySchema.Kind.values()) {
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport),BUILT_INS,kind.type(),ROOT_ID,slivers,0,()->NEW_ID)).command().widget();
            assertTrue(((WidgetSlot.SingleSlot)added.slots().get(measurement)).child().isEmpty());
            var withList = new WidgetNode(ROOT_ID,viewport.type(),Map.of(),Map.of(slivers,new WidgetSlot.ListSlot(List.of(added))));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withList),BUILT_INS,TEXT,NEW_ID,measurement,0,()->FIRST_ID));
            assertEquals(kind.hasChildren(), planner.plan(document(withList),BUILT_INS,TEXT,NEW_ID,CHILDREN,0,()->FIRST_ID)
                    instanceof FlutterDesignerPaletteDropPlanner.Accepted);
            for(var rejected : List.of(kind.type(),EXPANDED,new WidgetTypeId("flutter.widgets.SliverPadding")))
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(withList),BUILT_INS,rejected,NEW_ID,measurement,0,()->FIRST_ID));
            var box = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(TEXT).orElseThrow(),FIRST_ID);
            var slots = new java.util.LinkedHashMap<>(added.slots()); slots.put(measurement,WidgetSlot.SingleSlot.of(box));
            var occupied = new WidgetNode(added.id(),added.type(),added.properties(),slots);
            var full = new WidgetNode(ROOT_ID,viewport.type(),Map.of(),Map.of(slivers,new WidgetSlot.ListSlot(List.of(occupied))));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(full),BUILT_INS,TEXT,NEW_ID,measurement,0,StableId::random));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN,List.of())),BUILT_INS,kind.type(),ROOT_ID,CHILDREN,0,()->NEW_ID));
        }
    }

    @Test
    void fixedExtentVariantsPreserveRequiredExtentAndOwnOnlyTheirDeclaredChildren() {
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        for (var kind : dev.flutter.netbeans.designer.catalog.SliverFixedExtentListWidgetPropertySchema.Kind.values()) {
            var type = kind.type();
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
            var withFill = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(added))));
            assertEquals(new PropertyValue.DoubleValue(new java.math.BigDecimal("48")),
                    added.properties().get(new PropertyName("itemExtent")));
            if (!kind.hasChildren()) {
                assertTrue(added.slots().isEmpty());
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(withFill), BUILT_INS, TEXT, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            } else {
                assertEquals(1, added.properties().size());
                for (var source : List.of(TEXT, COLUMN, new WidgetTypeId("flutter.widgets.ListView")))
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                            planner.plan(document(withFill), BUILT_INS, source, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
                for (var source : List.of(type, EXPANDED, new WidgetTypeId("flutter.widgets.SliverFillRemaining")))
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                            planner.plan(document(withFill), BUILT_INS, source, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            }
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of())), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
        }
    }

    @Test
    void variedExtentVariantsPreserveRequiredExtentAndOwnOnlyTheirDeclaredChildren() {
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        for (var kind : dev.flutter.netbeans.designer.catalog.SliverVariedExtentListWidgetPropertySchema.Kind.values()) {
            var type = kind.type();
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
            var withFill = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(added))));
            assertEquals(new PropertyValue.StringValue("48"),
                    added.properties().get(new PropertyName("itemExtentBuilder")));
            if (!kind.hasChildren()) {
                assertTrue(added.slots().isEmpty());
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(withFill), BUILT_INS, TEXT, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            } else {
                assertEquals(1, added.properties().size());
                for (var source : List.of(TEXT, COLUMN, new WidgetTypeId("flutter.widgets.ListView")))
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                            planner.plan(document(withFill), BUILT_INS, source, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
                for (var source : List.of(type, EXPANDED, new WidgetTypeId("flutter.widgets.SliverFillRemaining")))
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                            planner.plan(document(withFill), BUILT_INS, source, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            }
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of())), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
        }
    }

    @Test
    void fillViewportVariantsKeepChildrenAndDelegateOwnershipSeparate() {
        var slivers = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        for (var type : dev.flutter.netbeans.designer.catalog.SliverFillViewportWidgetPropertySchema.TYPES) {
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
            var withFill = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(added))));
            if (type.value().endsWith(".delegate")) {
                assertTrue(added.slots().isEmpty());
                assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                        planner.plan(document(withFill), BUILT_INS, TEXT, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            } else {
                assertTrue(added.properties().isEmpty());
                for (var source : List.of(TEXT, COLUMN, new WidgetTypeId("flutter.widgets.ListView")))
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                            planner.plan(document(withFill), BUILT_INS, source, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
                for (var source : List.of(type, EXPANDED, new WidgetTypeId("flutter.widgets.SliverFillRemaining")))
                    assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                            planner.plan(document(withFill), BUILT_INS, source, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            }
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of())), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
        }
    }

    @Test
    void sliverFillRemainingCreatesInSliverSlotsAndAcceptsOnlyOneBoxChild() {
        var type = new WidgetTypeId("flutter.widgets.SliverFillRemaining");
        var slivers = new SlotName("slivers"); var child = new SlotName("child");
        var viewport = new WidgetNode(ROOT_ID, new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(slivers, new WidgetSlot.ListSlot(List.of())));
        var created = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, slivers, 0, () -> NEW_ID)).command().widget();
        assertTrue(created.properties().isEmpty(), "Omission preserves both native defaults.");
        var withFill = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(created))));
        for (var childType : List.of(TEXT, COLUMN, new WidgetTypeId("flutter.widgets.ListView")))
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withFill), BUILT_INS, childType, NEW_ID, child, 0, () -> FIRST_ID));
        for (var rejected : List.of(type, new WidgetTypeId("flutter.widgets.SliverPadding"), EXPANDED))
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(withFill), BUILT_INS, rejected, NEW_ID, child, 0, () -> FIRST_ID));
        var box = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(BUILT_INS.find(TEXT).orElseThrow(), FIRST_ID);
        var occupied = new WidgetNode(created.id(), created.type(), created.properties(), Map.of(child, WidgetSlot.SingleSlot.of(box)));
        var full = new WidgetNode(ROOT_ID, viewport.type(), Map.of(), Map.of(slivers, new WidgetSlot.ListSlot(List.of(occupied))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(document(full), BUILT_INS, TEXT, NEW_ID, child, 0, () -> StableId.random()));
    }

    @Test
    void staticSliversAreCreatedOnlyInSliverSlotsAndAcceptOrdinaryChildren() {
        var viewportType = type("flutter.widgets.CustomScrollView");
        var sliversSlot = new SlotName("slivers");
        var viewport = new WidgetNode(ROOT_ID, viewportType, Map.of(),
                Map.of(sliversSlot, new WidgetSlot.ListSlot(List.of())));
        for (var type : dev.flutter.netbeans.designer.catalog.SliverChildrenWidgetPropertySchema.TYPES) {
            var added = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(viewport), BUILT_INS, type, ROOT_ID, sliversSlot, 0, () -> NEW_ID));
            var sliver = added.command().widget();
            assertEquals(type, sliver.type());
            var withSliver = new WidgetNode(ROOT_ID, viewportType, Map.of(),
                    Map.of(sliversSlot, new WidgetSlot.ListSlot(List.of(sliver))));
            var child = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planner.plan(document(withSliver), BUILT_INS, TEXT, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            assertEquals(TEXT, child.command().widget().type());
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(withSliver), BUILT_INS, type, NEW_ID, CHILDREN, 0, () -> FIRST_ID));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(parent(COLUMN, List.of())), BUILT_INS, type, ROOT_ID, CHILDREN, 0, () -> NEW_ID));
        }
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion emptyHash = new ManagedRegion("0".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.empty(),
                new ManagedRegions(emptyHash, emptyHash));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static WidgetNode parent(WidgetTypeId type, List<WidgetNode> children) {
        return parent(type, CHILDREN, children);
    }

    private static WidgetNode parent(
            WidgetTypeId type,
            SlotName slot,
            List<WidgetNode> children) {
        return listParent(type, slot, children);
    }

    private static WidgetNode listParent(
            WidgetTypeId type,
            SlotName slot,
            List<WidgetNode> children) {
        WidgetNode prototype = prototype(type);
        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(prototype.slots());
        slots.put(slot, new WidgetSlot.ListSlot(children));
        return new WidgetNode(
                ROOT_ID,
                type,
                prototype.properties(),
                slots);
    }

    private static WidgetNode singleParent(WidgetTypeId type, WidgetNode child) {
        return singleParent(type, CHILD, child);
    }

    private static WidgetNode singleParent(
            WidgetTypeId type,
            SlotName slot,
            WidgetNode child) {
        return parentWithSlot(
                type,
                slot,
                child == null
                        ? WidgetSlot.SingleSlot.empty()
                        : WidgetSlot.SingleSlot.of(child));
    }

    private static WidgetNode parentWithSlot(
            WidgetTypeId type,
            SlotName slot,
            WidgetSlot value) {
        WidgetNode prototype = prototype(type);
        if (type.value().equals("flutter.material.FloatingActionButton") && slot.value().equals("icon")) {
            prototype = new WidgetNode(prototype.id(), type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue("extended")),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text(id("412a3e9a-abf4-4a26-8af4-8fb1192f9d3b"), "required label")),
                            new SlotName("icon"), WidgetSlot.SingleSlot.empty()));
        }
        if (type.value().equals("flutter.material.FilledButton") && slot.value().equals("icon")) {
            prototype = new WidgetNode(prototype.id(), type, Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue("icon")),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text(id("312a3e9a-abf4-4a26-8af4-8fb1192f9d3b"), "required label")),
                            new SlotName("icon"), WidgetSlot.SingleSlot.empty()));
        }
        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(prototype.slots());
        slots.put(slot, value);
        return new WidgetNode(
                ROOT_ID,
                type,
                prototype.properties(),
                slots);
    }

    private static WidgetNode prototype(WidgetTypeId type) {
        WidgetNode created = WidgetNodePrototypeFactory.create(definition(type), ROOT_ID);
        if ("flutter.material.IconButton".equals(type.value())) {
            return new WidgetNode(created.id(), created.type(), created.properties(),
                    Map.of(new SlotName("icon"), WidgetSlot.SingleSlot.of(text(id("512a3e9a-abf4-4a26-8af4-8fb1192f9d3b"), "required icon")),
                            new SlotName("selectedIcon"), WidgetSlot.SingleSlot.empty()));
        }
        if ("flutter.material.TextButton".equals(type.value())
                || "flutter.material.OutlinedButton".equals(type.value())) {
            return new WidgetNode(created.id(), created.type(), Map.of(ENABLED, new PropertyValue.BooleanValue(true),
                    new PropertyName("variant"), new PropertyValue.StringValue("icon")),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text(id("212a3e9a-abf4-4a26-8af4-8fb1192f9d3b"), "required label")),
                            new SlotName("icon"), WidgetSlot.SingleSlot.empty()));
        }
        if ("flutter.widgets.Visibility".equals(type.value())) {
            return new WidgetNode(created.id(), created.type(), created.properties(),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text(
                            id("112a3e9a-abf4-4a26-8af4-8fb1192f9d3b"), "required child")),
                            new SlotName("replacement"), WidgetSlot.SingleSlot.empty()));
        }
        return created;
    }

    private static WidgetNode text(StableId id, String data) {
        return new WidgetNode(
                id,
                TEXT,
                Map.of(DATA, new PropertyValue.StringValue(data)),
                Map.of());
    }

    private static WidgetNode expanded(StableId id, WidgetNode child) {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition(EXPANDED), id);
        return new WidgetNode(
                prototype.id(),
                prototype.type(),
                prototype.properties(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }

    private static WidgetNode flexible(StableId id, WidgetNode child) {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition(FLEXIBLE), id);
        return new WidgetNode(
                prototype.id(),
                prototype.type(),
                prototype.properties(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }

    private static WidgetDefinition definition(WidgetTypeId type) {
        return BUILT_INS.find(type).orElseThrow();
    }

    private static PropertyValue.Matrix4Value identityMatrix() {
        java.util.ArrayList<BigDecimal> storage = new java.util.ArrayList<>(16);
        for (int index = 0; index < 16; index++) {
            storage.add(index % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO);
        }
        return new PropertyValue.Matrix4Value(storage);
    }

    private static PropertyValue.BoxDecorationValue emptyBoxDecoration() {
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static WidgetDefinition replaceSlots(
            WidgetDefinition definition,
            SlotDefinition... slots) {
        return new WidgetDefinition(
                definition.typeId(),
                definition.dartClassName(),
                definition.namedConstructor(),
                definition.constConstructor(),
                definition.dartLibraryUri(),
                definition.importUris(),
                definition.traits(),
                definition.palette(),
                definition.properties(),
                List.of(slots));
    }

    private static WidgetCatalog catalog(WidgetDefinition... definitions) {
        return WidgetCatalog.strict(List.of(definitions));
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }

    private static StableId indexedId(int index) {
        return id("00000000-0000-4000-8000-" + String.format("%012x", index + 1L));
    }

    private record AcceptedCase(
            String name,
            DesignerDocument document,
            SlotName slot,
            int index) {
    }

    private record CoreSourceCase(
            String name,
            WidgetTypeId type,
            Map<PropertyName, PropertyValue> properties,
            Map<SlotName, SlotCardinality> slots) {
    }

    private record MatrixTargetCase(
            String name,
            DesignerDocument document,
            SlotName slot) {
    }

    private record SingleTargetCase(
            String name,
            WidgetTypeId parentType,
            SlotName slot) {
    }

    private record RejectedCase(
            String name,
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId source,
            StableId parentId,
            SlotName slot,
            int index,
            FlutterDesignerPaletteDropPlanner.RejectionCode expected) {
    }

    private record InvalidCase(
            String name,
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId source,
            StableId parentId,
            SlotName slot,
            int index,
            Supplier<StableId> supplier) {
    }
}
