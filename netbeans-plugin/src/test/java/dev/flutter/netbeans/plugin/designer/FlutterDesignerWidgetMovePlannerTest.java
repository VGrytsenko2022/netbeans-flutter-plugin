package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ClipOvalWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRSuperellipseWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PhysicalModelWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.DecoratedBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DirectionalityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SlotAcceptance;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
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
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlutterDesignerWidgetMovePlannerTest {
    private static final WidgetCatalog BUILT_INS = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId SCAFFOLD = type("flutter.material.Scaffold");
    private static final WidgetTypeId APP_BAR = type("flutter.material.AppBar");
    private static final WidgetTypeId TEXT_FIELD =
            type("flutter.material.TextField");
    private static final WidgetTypeId COLUMN = type("flutter.widgets.Column");
    private static final WidgetTypeId ROW = type("flutter.widgets.Row");
    private static final WidgetTypeId CENTER = type("flutter.widgets.Center");
    private static final WidgetTypeId SIZED_BOX = type("flutter.widgets.SizedBox");
    private static final WidgetTypeId ASPECT_RATIO =
            type("flutter.widgets.AspectRatio");
    private static final WidgetTypeId OPACITY = type("flutter.widgets.Opacity");
    private static final WidgetTypeId COLORED_BOX = type("flutter.widgets.ColoredBox");
    private static final WidgetTypeId PLACEHOLDER = type("flutter.widgets.Placeholder");
    private static final WidgetTypeId ALIGN = type("flutter.widgets.Align");
    private static final WidgetTypeId FRACTIONALLY_SIZED_BOX =
            type("flutter.widgets.FractionallySizedBox");
    private static final WidgetTypeId FITTED_BOX =
            type("flutter.widgets.FittedBox");
    private static final WidgetTypeId CONSTRAINED_BOX =
            type("flutter.widgets.ConstrainedBox");
    private static final WidgetTypeId UNCONSTRAINED_BOX =
            type("flutter.widgets.UnconstrainedBox");
    private static final WidgetTypeId LIMITED_BOX =
            type("flutter.widgets.LimitedBox");
    private static final WidgetTypeId OVERFLOW_BOX =
            type("flutter.widgets.OverflowBox");
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
    private static final WidgetTypeId LIST_BODY = type("flutter.widgets.ListBody");
    private static final WidgetTypeId OVERFLOW_BAR =
            type("flutter.widgets.OverflowBar");
    private static final WidgetTypeId SAFE_AREA = type("flutter.widgets.SafeArea");
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
    private static final WidgetTypeId PHYSICAL_MODEL =
            PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE;
    private static final WidgetTypeId CLIP_RSUPERELLIPSE =
            ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE;
    private static final WidgetTypeId TEXT = type("flutter.widgets.Text");
    private static final SlotName CHILDREN = slot("children");
    private static final SlotName CHILD = slot("child");
    private static final SlotName BODY = slot("body");
    private static final SlotName LEADING = slot("leading");
    private static final SlotName ACTIONS = slot("actions");
    private static final SlotName FIRST = slot("first");
    private static final SlotName SECOND = slot("second");
    private static final SlotName SOURCE = slot("source");
    private static final SlotName DESTINATION = slot("destination");
    private static final PropertyName DATA = new PropertyName("data");
    private static final StableId DOCUMENT_ID = id(
            "14f6c16f-893b-44d0-b809-edbd51bbcdaa");
    private static final StableId ROOT_ID = id(
            "0209809f-351a-4ce7-8c07-1ec625b1e109");
    private static final StableId A_ID = id(
            "710c4ad9-c3cf-434e-af1e-5217ac38aa92");
    private static final StableId B_ID = id(
            "805b5a85-397c-4d5f-ae6f-2171456d7a5a");
    private static final StableId C_ID = id(
            "10731acc-85f1-4d4a-b1c5-211c29e6066b");
    private static final StableId D_ID = id(
            "50ac7543-24e5-414c-aa6b-41e962a6bd0d");

    private final FlutterDesignerWidgetMovePlanner planner =
            new FlutterDesignerWidgetMovePlanner();

    @Test
    void circleAvatarMovesWithExactPropertiesAndChildAndAllowsChildMoveInAndOut() {
        WidgetNode child = WidgetNodePrototypeFactory.create(BUILT_INS.find(type("flutter.widgets.Text")).orElseThrow(), D_ID);
        WidgetNode avatar = new WidgetNode(A_ID, type("flutter.material.CircleAvatar"),
                Map.of(new PropertyName("radius"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(24)),
                        new PropertyName("backgroundColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode destination = listParent(B_ID, STACK, CHILDREN, List.of());
        var wholeDocument = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(avatar, destination)));
        assertAcceptedCommandApplies(wholeDocument, BUILT_INS, avatar,
                planner.plan(wholeDocument, BUILT_INS, A_ID, new FlutterDesignerWidgetMovePlanner.On(B_ID)));
        assertAcceptedCommandApplies(wholeDocument, BUILT_INS, child,
                planner.plan(wholeDocument, BUILT_INS, D_ID, new FlutterDesignerWidgetMovePlanner.On(ROOT_ID)));
        var empty = new WidgetNode(A_ID, avatar.type(), avatar.properties(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
        var emptyDocument = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(empty, child)));
        assertAcceptedCommandApplies(emptyDocument, BUILT_INS, child,
                planner.plan(emptyDocument, BUILT_INS, D_ID, new FlutterDesignerWidgetMovePlanner.IntoSlot(A_ID, CHILD, 0)));
    }

    @Test
    void badgeCountModeRejectsLabelMovesButChildAndWholeBadgeMovesRemainAvailable() {
        WidgetNode source = WidgetNodePrototypeFactory.create(BUILT_INS.find(type("flutter.widgets.Text")).orElseThrow(), A_ID);
        SlotName label = new SlotName("label");
        WidgetNode badge = new WidgetNode(B_ID, type("flutter.material.Badge"),
                Map.of(new PropertyName("count"), new PropertyValue.IntegerValue(java.math.BigInteger.TEN),
                        new PropertyName("textColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty(), label, WidgetSlot.SingleSlot.empty()));
        var document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(source, badge)));
        var rejected = assertInstanceOf(FlutterDesignerWidgetMovePlanner.Rejected.class,
                planner.plan(document, BUILT_INS, A_ID, new FlutterDesignerWidgetMovePlanner.IntoSlot(B_ID, label, 0)));
        assertTrue(rejected.reason().contains("Cannot move")); assertTrue(rejected.reason().contains("Clear Count"));
        assertTrue(rejected.reason().contains(B_ID.toString()));
        var intoChild = planner.plan(document, BUILT_INS, A_ID, new FlutterDesignerWidgetMovePlanner.On(B_ID));
        assertEquals(new WidgetPlacement(B_ID, CHILD, 0), accepted(intoChild).command().destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, intoChild);
        var plainBadge = new WidgetNode(B_ID, badge.type(), Map.of(), badge.slots());
        var plainDocument = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(source, plainBadge)));
        assertInstanceOf(FlutterDesignerWidgetMovePlanner.Rejected.class,
                planner.plan(plainDocument, BUILT_INS, A_ID, new FlutterDesignerWidgetMovePlanner.On(B_ID)));
        assertAcceptedCommandApplies(plainDocument, BUILT_INS, source,
                planner.plan(plainDocument, BUILT_INS, A_ID, new FlutterDesignerWidgetMovePlanner.IntoSlot(B_ID, label, 0)));
        WidgetNode occupied = new WidgetNode(B_ID, badge.type(), badge.properties(), Map.of(CHILD, WidgetSlot.SingleSlot.of(source)));
        var occupiedDocument = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(occupied)));
        assertAcceptedCommandApplies(occupiedDocument, BUILT_INS, source,
                planner.plan(occupiedDocument, BUILT_INS, A_ID, new FlutterDesignerWidgetMovePlanner.On(ROOT_ID)));
        WidgetNode labelText = WidgetNodePrototypeFactory.create(BUILT_INS.find(type("flutter.widgets.Text")).orElseThrow(), D_ID);
        WidgetNode withBoth = new WidgetNode(B_ID, badge.type(), Map.of(new PropertyName("textColor"), new PropertyValue.ColorValue(0xff123456L)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(source), label, WidgetSlot.SingleSlot.of(labelText)));
        WidgetNode destination = listParent(C_ID, STACK, CHILDREN, List.of());
        var wholeDocument = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(withBoth, destination)));
        assertAcceptedCommandApplies(wholeDocument, BUILT_INS, withBoth,
                planner.plan(wholeDocument, BUILT_INS, B_ID, new FlutterDesignerWidgetMovePlanner.On(C_ID)));
    }

    @Test
    void cardMovesWithItsConfiguredShapeAndExactDescendantThenAllowsChildMoveOut() {
        WidgetNode child = WidgetNodePrototypeFactory.create(BUILT_INS.find(type("flutter.widgets.Text")).orElseThrow(), StableId.parse("fea038da-cb8e-497c-9f35-0efc8b273c64"));
        WidgetNode card = new WidgetNode(A_ID, type("flutter.material.Card"),
                Map.of(new PropertyName("variant"), new PropertyValue.StringValue("filled"),
                        new PropertyName("shapeKind"), new PropertyValue.StringValue("star"),
                        new PropertyName("shapePoints"), new PropertyValue.DoubleValue(new BigDecimal("6.5")),
                        new PropertyName("shapeSideColor"), new PropertyValue.ColorValue(0xFF123456L)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(card, stack)));
        var moved = planner.plan(document, BUILT_INS, card.id(), new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), accepted(moved).command().destination());
        assertAcceptedCommandApplies(document, BUILT_INS, card, moved);
        var movedChild = planner.plan(document, BUILT_INS, child.id(), new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        assertAcceptedCommandApplies(document, BUILT_INS, child, movedChild);
    }

    @Test
    void verticalDividerMovesAsLeafWithoutLosingAnyConfiguredProperty() {
        var ellipse = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.valueOf(2));
        WidgetNode verticalDivider = new WidgetNode(A_ID, new WidgetTypeId("flutter.material.VerticalDivider"),
                Map.of(new PropertyName("width"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(20)),
                        new PropertyName("thickness"), new PropertyValue.DoubleValue(BigDecimal.valueOf(2)),
                        new PropertyName("indent"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3)),
                        new PropertyName("endIndent"), new PropertyValue.DoubleValue(BigDecimal.valueOf(4)),
                        new PropertyName("color"), new PropertyValue.ColorValue(0x80123456L),
                        new PropertyName("radius"), new PropertyValue.BorderRadiusValue(
                                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(ellipse, ellipse, ellipse, ellipse))),
                Map.of());
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(verticalDivider, stack)));
        var result = planner.plan(document, BUILT_INS, verticalDivider.id(), new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(verticalDivider.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, verticalDivider, result);
    }

    @Test
    void dividerMovesAsLeafWithoutLosingAnyConfiguredProperty() {
        var ellipse = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.valueOf(2));
        WidgetNode divider = new WidgetNode(A_ID, new WidgetTypeId("flutter.material.Divider"),
                Map.of(new PropertyName("height"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(20)),
                        new PropertyName("thickness"), new PropertyValue.DoubleValue(BigDecimal.valueOf(2)),
                        new PropertyName("indent"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(3)),
                        new PropertyName("endIndent"), new PropertyValue.DoubleValue(BigDecimal.valueOf(4)),
                        new PropertyName("color"), new PropertyValue.ColorValue(0x80123456L),
                        new PropertyName("radius"), new PropertyValue.BorderRadiusValue(
                                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(ellipse, ellipse, ellipse, ellipse))),
                Map.of());
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(divider, stack)));
        var result = planner.plan(document, BUILT_INS, divider.id(), new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(divider.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, divider, result);
    }

    @Test
    void imageIconMovesAsLeafWithNullOrProviderAndPreservesAllProperties() {
        for (PropertyValue image : List.of(new PropertyValue.NullValue(),
                PropertyValue.ImageProviderValue.asset("assets/icon.png"))) {
            WidgetNode icon = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.ImageIcon"),
                    Map.of(new PropertyName("image"), image, new PropertyName("size"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(24)),
                            new PropertyName("semanticLabel"), new PropertyValue.StringValue("Move image")), Map.of());
            WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
            DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(icon, stack)));
            var result = planner.plan(document, BUILT_INS, icon.id(), new FlutterDesignerWidgetMovePlanner.On(stack.id()));
            MoveWidget command = accepted(result).command();
            assertEquals(icon.id(), command.widgetId());
            assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
            assertAcceptedCommandApplies(document, BUILT_INS, icon, result);
        }
    }

    @Test
    void completedIconThemeMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "themed icon child");
        WidgetNode iconTheme = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.IconTheme"),
                Map.of(new PropertyName("merge"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(iconTheme, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                iconTheme.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(iconTheme.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                iconTheme.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, iconTheme, result);
    }

    @Test
    void completedDefaultSelectionStyleMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "selection style child");
        WidgetNode defaultSelectionStyle = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.DefaultSelectionStyle"),
                Map.of(new PropertyName("merge"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(defaultSelectionStyle, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                defaultSelectionStyle.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(defaultSelectionStyle.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                defaultSelectionStyle.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, defaultSelectionStyle, result);
    }

    @Test
    void completedDefaultTextHeightBehaviorMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "focus child");
        WidgetNode defaultTextHeightBehavior = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior"),
                Map.of(new PropertyName("textHeightApplyFirstAscent"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(defaultTextHeightBehavior, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                defaultTextHeightBehavior.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(defaultTextHeightBehavior.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                defaultTextHeightBehavior.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, defaultTextHeightBehavior, result);
    }

    @Test
    void completedTickerModeMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "focus child");
        WidgetNode tickerMode = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.TickerMode"),
                Map.of(new PropertyName("enabled"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(tickerMode, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                tickerMode.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(tickerMode.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                tickerMode.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, tickerMode, result);
    }

    @Test
    void completedVisibilityMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "focus child");
        WidgetNode visibility = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.Visibility"),
                Map.of(new PropertyName("visible"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild), new SlotName("replacement"), WidgetSlot.SingleSlot.empty()));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(visibility, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                visibility.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(visibility.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                visibility.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, visibility, result);
    }

    @Test
    void onListPlansTerminalPostRemovalIndexWithoutMutatingSubtree() {
        WidgetNode nestedText = validText(D_ID, "nested");
        WidgetNode source = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(nestedText)));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, node(B_ID, TEXT), node(C_ID, TEXT))));
        DesignerDocument original = document;

        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.On(ROOT_ID)));

        assertAll(
                () -> assertEquals(A_ID, accepted.command().widgetId()),
                () -> assertEquals(ROOT_ID, accepted.command().destination().parentId()),
                () -> assertEquals(CHILDREN, accepted.command().destination().slotName()),
                () -> assertEquals(2, accepted.command().destination().index()),
                () -> assertEquals(original, document),
                () -> assertEquals(nestedText,
                        ((WidgetSlot.SingleSlot) source.slots().get(CHILD))
                                .child().orElseThrow()));
    }

    @Test
    void onStackAppendsTheCompleteSubtreeAtTheFrontPaintLayer() {
        WidgetNode nestedText = validText(D_ID, "nested");
        WidgetNode source = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(nestedText)));
        WidgetNode stack = listParent(
                B_ID,
                STACK,
                CHILDREN,
                List.of(validText(C_ID, "existing back layer")));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.On(B_ID));
        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(result);

        assertEquals(
                new MoveWidget(
                        A_ID,
                        new WidgetPlacement(B_ID, CHILDREN, 1)),
                accepted.command(),
                "Stack On-drop must append after the current front layer");
        assertAcceptedCommandApplies(
                document,
                BUILT_INS,
                source,
                result);
    }

    @Test
    void existingTextFieldLeafMovesWithinTheSameTreeWithStableIdPreserved() {
        WidgetNode source = WidgetNode.empty(A_ID, TEXT_FIELD);
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(Map.of(), source.properties());
        assertEquals(Map.of(), source.slots());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void expandedMovesOnlyBetweenDirectRowAndColumnChildrenAndKeepsItsSubtree() {
        WidgetNode nested = validText(D_ID, "expanded child");
        WidgetNode source = expanded(A_ID, nested);
        WidgetNode row = listParent(B_ID, ROW, CHILDREN, List.of());
        WidgetNode stack = listParent(C_ID, STACK, CHILDREN, List.of());
        StableId targetExpandedId = id(
                "f33a0092-c584-4839-ad77-9b157369d48e");
        StableId targetTextId = id(
                "f5941d9a-3bf4-41bb-b696-110e8a08fbaa");
        WidgetNode targetExpanded = expanded(
                targetExpandedId,
                validText(targetTextId, "target child"));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, row, stack, targetExpanded)));

        FlutterDesignerWidgetMovePlanner.Result rowMove = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(row.id()));
        MoveWidget rowCommand = accepted(rowMove).command();
        assertEquals(
                new WidgetPlacement(row.id(), CHILDREN, 0),
                rowCommand.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, rowMove);

        FlutterDesignerWidgetMovePlanner.Result columnReorder = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 4));
        assertEquals(
                new WidgetPlacement(ROOT_ID, CHILDREN, 3),
                accepted(columnReorder).command().destination());
        assertAcceptedCommandApplies(
                document, BUILT_INS, source, columnReorder);

        FlutterDesignerWidgetMovePlanner.Rejected stackFailure =
                assertInstanceOf(
                        FlutterDesignerWidgetMovePlanner.Rejected.class,
                        planner.plan(
                                document,
                                BUILT_INS,
                                source.id(),
                                new FlutterDesignerWidgetMovePlanner.On(stack.id())));
        FlutterDesignerWidgetMovePlanner.Rejected childFailure =
                assertInstanceOf(
                        FlutterDesignerWidgetMovePlanner.Rejected.class,
                        planner.plan(
                                document,
                                BUILT_INS,
                                source.id(),
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        targetExpanded.id(), CHILD, 0)));
        assertAll(
                () -> assertEquals(
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .NO_COMPATIBLE_DESTINATION,
                        stackFailure.code()),
                () -> assertTrue(stackFailure.reason().contains(
                        "Expanded '" + source.id() + "'")),
                () -> assertTrue(stackFailure.reason().contains(
                        "direct child of Row.children or Column.children")),
                () -> assertEquals(
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        childFailure.code()),
                () -> assertTrue(childFailure.reason().contains(
                        "flutter.widgets.Expanded.child")));
    }

    @Test
    void completedExcludeFocusMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "focus child");
        WidgetNode excludeFocus = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.ExcludeFocus"),
                Map.of(new PropertyName("excluding"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(excludeFocus, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                excludeFocus.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(excludeFocus.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                excludeFocus.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, excludeFocus, result);
    }

    @Test
    void completedExcludeFocusTraversalMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "focus child");
        WidgetNode excludeFocusTraversal = new WidgetNode(
                A_ID,
                new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal"),
                Map.of(new PropertyName("excluding"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(excludeFocusTraversal, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                excludeFocusTraversal.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(excludeFocusTraversal.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                excludeFocusTraversal.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, excludeFocusTraversal, result);
    }

    @Test
    void completedSafeAreaMovesWithinTheSameTreeWithRequiredChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "safe child");
        WidgetNode safeArea = new WidgetNode(
                A_ID,
                SAFE_AREA,
                Map.of(new PropertyName("left"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(safeArea, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                safeArea.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(safeArea.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0),
                command.destination());
        assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                safeArea.slots().get(CHILD)).child().orElseThrow());
        assertAcceptedCommandApplies(document, BUILT_INS, safeArea, result);
    }

    @Test
    void completedDirectionalityMovesWithRequiredChildDirectionAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "directional child");
        WidgetNode directionality = new WidgetNode(
                A_ID,
                DIRECTIONALITY,
                Map.of(
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl")),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(directionality, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                directionality.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(directionality.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(
                        new PropertyValue.EnumValue("TextDirection", "rtl"),
                        directionality.properties().get(
                                new PropertyName("textDirection"))),
                () -> assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                        directionality.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, directionality, result);
    }

    @Test
    void completedDecoratedBoxMovesWithDecorationPositionChildAndIdsPreserved() {
        WidgetNode requiredChild = validText(D_ID, "decorated child");
        WidgetNode palettePrototype = WidgetNodePrototypeFactory.create(
                definition(DECORATED_BOX), A_ID);
        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>(palettePrototype.properties());
        properties.put(
                new PropertyName("position"),
                new PropertyValue.EnumValue("DecorationPosition", "foreground"));
        WidgetNode decoratedBox = new WidgetNode(
                palettePrototype.id(),
                palettePrototype.type(),
                properties,
                Map.of(CHILD, WidgetSlot.SingleSlot.of(requiredChild)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(decoratedBox, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                decoratedBox.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(decoratedBox.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertTrue(decoratedBox.properties().containsKey(
                        new PropertyName("decoration"))),
                () -> assertEquals(
                        new PropertyValue.EnumValue(
                                "DecorationPosition", "foreground"),
                        decoratedBox.properties().get(new PropertyName("position"))),
                () -> assertEquals(requiredChild, ((WidgetSlot.SingleSlot)
                        decoratedBox.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, decoratedBox, result);
    }

    @Test
    void completedExcludeSemanticsMovesWithBooleanChildAndIdsPreserved() {
        WidgetNode child = validText(D_ID, "semantic child");
        WidgetNode widget = new WidgetNode(
                A_ID,
                EXCLUDE_SEMANTICS,
                Map.of(new PropertyName("excluding"),
                        new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(
                        new PropertyValue.BooleanValue(false),
                        widget.properties().get(new PropertyName("excluding"))),
                () -> assertEquals(child, ((WidgetSlot.SingleSlot)
                        widget.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void completedIndexedStackMovesWithFivePropertiesChildrenAndIdsPreserved() {
        WidgetNode first = validText(C_ID, "indexed first");
        WidgetNode second = validText(D_ID, "indexed second");
        WidgetNode widget = new WidgetNode(
                A_ID,
                INDEXED_STACK,
                Map.of(
                        new PropertyName("alignment"),
                        new PropertyValue.AlignmentGeometryValue(
                                PropertyValue.AlignmentGeometryValue
                                        .HorizontalBasis.DIRECTIONAL,
                                java.math.BigDecimal.ONE.negate(),
                                java.math.BigDecimal.ZERO),
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl"),
                        new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAlias"),
                        new PropertyName("sizing"),
                        new PropertyValue.EnumValue("StackFit", "expand"),
                        new PropertyName("index"),
                        new PropertyValue.NullValue()),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(first, second))));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(5, widget.properties().size()),
                () -> assertEquals(new PropertyValue.NullValue(),
                        widget.properties().get(new PropertyName("index"))),
                () -> assertEquals(List.of(first, second),
                        ((WidgetSlot.ListSlot) widget.slots().get(CHILDREN))
                                .children()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void completedClipRectMovesWithClipBehaviorChildAndIdsPreserved() {
        WidgetNode child = validText(D_ID, "clipped child");
        WidgetNode widget = new WidgetNode(
                A_ID,
                CLIP_RECT,
                Map.of(new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAlias")),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(
                        new PropertyValue.EnumValue("Clip", "antiAlias"),
                        widget.properties().get(new PropertyName("clipBehavior"))),
                () -> assertEquals(child, ((WidgetSlot.SingleSlot)
                        widget.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void completedClipOvalMovesWithClipBehaviorChildAndIdsPreserved() {
        WidgetNode child = validText(D_ID, "oval-clipped child");
        WidgetNode widget = new WidgetNode(
                A_ID,
                CLIP_OVAL,
                Map.of(new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(
                        new PropertyValue.EnumValue("Clip", "hardEdge"),
                        widget.properties().get(new PropertyName("clipBehavior"))),
                () -> assertEquals(child, ((WidgetSlot.SingleSlot)
                        widget.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void completedClipRRectMovesWithBorderRadiusClipBehaviorChildAndIdsPreserved() {
        PropertyValue.BorderRadiusValue borderRadius = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(12), BigDecimal.valueOf(8)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(10), BigDecimal.valueOf(6)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(4), BigDecimal.valueOf(2)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.ONE, BigDecimal.valueOf(3))));
        WidgetNode child = validText(D_ID, "rounded-clipped child");
        WidgetNode widget = new WidgetNode(
                A_ID,
                CLIP_RRECT,
                Map.of(
                        new PropertyName("borderRadius"), borderRadius,
                        new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer")),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(borderRadius,
                        widget.properties().get(new PropertyName("borderRadius"))),
                () -> assertEquals(
                        new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer"),
                        widget.properties().get(new PropertyName("clipBehavior"))),
                () -> assertEquals(child, ((WidgetSlot.SingleSlot)
                        widget.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void completedClipRSuperellipseMovesWithBorderRadiusClipBehaviorChildAndIdsPreserved() {
        PropertyValue.BorderRadiusValue borderRadius = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(12), BigDecimal.valueOf(8)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(10), BigDecimal.valueOf(6)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(4), BigDecimal.valueOf(2)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.ONE, BigDecimal.valueOf(3))));
        WidgetNode child = validText(D_ID, "rounded-clipped child");
        WidgetNode widget = new WidgetNode(
                A_ID,
                CLIP_RSUPERELLIPSE,
                Map.of(
                        new PropertyName("borderRadius"), borderRadius,
                        new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer")),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(borderRadius,
                        widget.properties().get(new PropertyName("borderRadius"))),
                () -> assertEquals(
                        new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer"),
                        widget.properties().get(new PropertyName("clipBehavior"))),
                () -> assertEquals(child, ((WidgetSlot.SingleSlot)
                        widget.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void completedPhysicalModelMovesWithBorderRadiusClipBehaviorChildAndIdsPreserved() {
        PropertyValue.BorderRadiusValue borderRadius = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(12), BigDecimal.valueOf(8)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(10), BigDecimal.valueOf(6)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.valueOf(4), BigDecimal.valueOf(2)),
                        new PropertyValue.BoxDecorationValue.Radius(
                                BigDecimal.ONE, BigDecimal.valueOf(3))));
        WidgetNode child = validText(D_ID, "rounded-clipped child");
        WidgetNode widget = new WidgetNode(
                A_ID,
                PHYSICAL_MODEL,
                Map.of(
                        new PropertyName("color"), new PropertyValue.ColorValue(0xFF2196F3L),
                        new PropertyName("shape"), new PropertyValue.EnumValue("BoxShape", "circle"),
                        new PropertyName("elevation"), new PropertyValue.DoubleValue(BigDecimal.TEN),
                        new PropertyName("shadowColor"), new PropertyValue.ColorValue(0xFF000000L),
                        new PropertyName("borderRadius"), borderRadius,
                        new PropertyName("clipBehavior"),
                        new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer")),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();

        assertAll(
                () -> assertEquals(widget.id(), command.widgetId()),
                () -> assertEquals(
                        new WidgetPlacement(stack.id(), CHILDREN, 0),
                        command.destination()),
                () -> assertEquals(borderRadius,
                        widget.properties().get(new PropertyName("borderRadius"))),
                () -> assertEquals(
                        new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer"),
                        widget.properties().get(new PropertyName("clipBehavior"))),
                () -> assertEquals(child, ((WidgetSlot.SingleSlot)
                        widget.slots().get(CHILD)).child().orElseThrow()));
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void onListBodyAppendsTheCompleteSubtreeAtExactConstructorOrder() {
        WidgetNode nestedText = validText(D_ID, "nested");
        WidgetNode source = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(nestedText)));
        WidgetNode listBody = new WidgetNode(
                B_ID,
                LIST_BODY,
                Map.of(
                        new PropertyName("mainAxis"),
                        new PropertyValue.EnumValue("Axis", "horizontal"),
                        new PropertyName("reverse"),
                        new PropertyValue.BooleanValue(true)),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(
                        List.of(validText(C_ID, "existing child")))));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, listBody)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.On(B_ID));
        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(result);

        assertEquals(
                new MoveWidget(
                        A_ID,
                        new WidgetPlacement(B_ID, CHILDREN, 1)),
                accepted.command(),
                "ListBody On-drop must append in exact children constructor order");
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
        assertEquals(
                new PropertyValue.EnumValue("Axis", "horizontal"),
                listBody.properties().get(new PropertyName("mainAxis")));
        assertEquals(
                new PropertyValue.BooleanValue(true),
                listBody.properties().get(new PropertyName("reverse")));
    }

    @Test
    void onOverflowBarAppendsTheCompleteSubtreeAtExactSourceOrder() {
        WidgetNode nestedText = validText(D_ID, "nested");
        WidgetNode source = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(nestedText)));
        WidgetNode overflowBar = new WidgetNode(
                B_ID,
                OVERFLOW_BAR,
                Map.of(
                        new PropertyName("alignment"),
                        new PropertyValue.EnumValue(
                                "MainAxisAlignment", "spaceBetween"),
                        new PropertyName("overflowDirection"),
                        new PropertyValue.EnumValue("VerticalDirection", "up")),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(
                        List.of(validText(C_ID, "existing child")))));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, overflowBar)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.On(B_ID));
        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(result);

        assertEquals(
                new MoveWidget(
                        A_ID,
                        new WidgetPlacement(B_ID, CHILDREN, 1)),
                accepted.command(),
                "OverflowBar On-drop must append in exact children source order");
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
        assertEquals(
                new PropertyValue.EnumValue(
                        "MainAxisAlignment", "spaceBetween"),
                overflowBar.properties().get(new PropertyName("alignment")));
        assertEquals(
                new PropertyValue.EnumValue("VerticalDirection", "up"),
                overflowBar.properties().get(
                        new PropertyName("overflowDirection")));
    }

    @Test
    void explicitOverflowBarReorderUsesPostRemovalSourceIndex() {
        WidgetNode first = validText(A_ID, "first");
        WidgetNode second = validText(B_ID, "second");
        WidgetNode third = validText(C_ID, "third");
        DesignerDocument document = document(listParent(
                ROOT_ID,
                OVERFLOW_BAR,
                CHILDREN,
                List.of(first, second, third)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                        ROOT_ID, CHILDREN, 2));

        assertEquals(
                new MoveWidget(
                        A_ID,
                        new WidgetPlacement(ROOT_ID, CHILDREN, 2)),
                accepted(result).command(),
                "source-aware reorder removes the child before resolving its "
                        + "final OverflowBar index");
        assertAcceptedCommandApplies(document, BUILT_INS, first, result);
    }

    @Test
    void flexibleMovesOnlyBetweenDirectRowAndColumnChildrenAndKeepsItsSubtree() {
        WidgetNode nested = validText(D_ID, "flexible child");
        WidgetNode source = flexible(A_ID, nested);
        WidgetNode row = listParent(B_ID, ROW, CHILDREN, List.of());
        WidgetNode stack = listParent(C_ID, STACK, CHILDREN, List.of());
        StableId targetFlexibleId = id(
                "f33a0092-c584-4839-ad77-9b157369d48e");
        StableId targetTextId = id(
                "f5941d9a-3bf4-41bb-b696-110e8a08fbaa");
        WidgetNode targetFlexible = flexible(
                targetFlexibleId,
                validText(targetTextId, "target child"));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, row, stack, targetFlexible)));

        FlutterDesignerWidgetMovePlanner.Result rowMove = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(row.id()));
        MoveWidget rowCommand = accepted(rowMove).command();
        assertEquals(
                new WidgetPlacement(row.id(), CHILDREN, 0),
                rowCommand.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, rowMove);

        FlutterDesignerWidgetMovePlanner.Rejected stackFailure =
                assertInstanceOf(
                        FlutterDesignerWidgetMovePlanner.Rejected.class,
                        planner.plan(
                                document,
                                BUILT_INS,
                                source.id(),
                                new FlutterDesignerWidgetMovePlanner.On(stack.id())));
        FlutterDesignerWidgetMovePlanner.Rejected childFailure =
                assertInstanceOf(
                        FlutterDesignerWidgetMovePlanner.Rejected.class,
                        planner.plan(
                                document,
                                BUILT_INS,
                                source.id(),
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        targetFlexible.id(), CHILD, 0)));
        assertAll(
                () -> assertEquals(
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .NO_COMPATIBLE_DESTINATION,
                        stackFailure.code()),
                () -> assertTrue(stackFailure.reason().contains(
                        "Flexible '" + source.id() + "'")),
                () -> assertTrue(stackFailure.reason().contains(
                        "direct child of Row.children or Column.children")),
                () -> assertEquals(
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET,
                        childFailure.code()),
                () -> assertTrue(childFailure.reason().contains(
                        "flutter.widgets.Flexible.child")));
    }

    @Test
    void spacerMovesOnlyBetweenDirectRowAndColumnChildren() {
        WidgetNode source = node(A_ID, SPACER);
        WidgetNode row = listParent(B_ID, ROW, CHILDREN, List.of());
        WidgetNode stack = listParent(C_ID, STACK, CHILDREN, List.of());
        WidgetNode baseline = WidgetNodePrototypeFactory.create(
                definition(BASELINE), D_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, row, stack, baseline)));

        FlutterDesignerWidgetMovePlanner.Result rowMove = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(row.id()));
        MoveWidget rowCommand = accepted(rowMove).command();
        assertEquals(
                new WidgetPlacement(row.id(), CHILDREN, 0),
                rowCommand.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, rowMove);

        FlutterDesignerWidgetMovePlanner.Rejected stackFailure = assertInstanceOf(
                FlutterDesignerWidgetMovePlanner.Rejected.class,
                planner.plan(
                        document,
                        BUILT_INS,
                        source.id(),
                        new FlutterDesignerWidgetMovePlanner.On(stack.id())));
        FlutterDesignerWidgetMovePlanner.Rejected baselineFailure = assertInstanceOf(
                FlutterDesignerWidgetMovePlanner.Rejected.class,
                planner.plan(
                        document,
                        BUILT_INS,
                        source.id(),
                        new FlutterDesignerWidgetMovePlanner.On(baseline.id())));
        assertAll(
                () -> assertEquals(
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .NO_COMPATIBLE_DESTINATION,
                        stackFailure.code()),
                () -> assertTrue(stackFailure.reason().contains(
                        "Spacer '" + source.id() + "'")),
                () -> assertTrue(stackFailure.reason().contains(
                        "direct child of Row.children or Column.children")),
                () -> assertEquals(
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .NO_COMPATIBLE_DESTINATION,
                        baselineFailure.code()),
                () -> assertTrue(baselineFailure.reason().contains(
                        "Spacer '" + source.id() + "'")),
                () -> assertTrue(baselineFailure.reason().contains(
                        "direct child of Row.children or Column.children")));
    }

    @Test
    void insertBeforeNormalizesSameListIndexAfterRemoval() {
        DesignerDocument document = threeTextColumn();

        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 2)));

        assertEquals(
                new MoveWidget(
                        A_ID,
                        new WidgetPlacement(ROOT_ID, CHILDREN, 1)),
                accepted.command());
    }

    @Test
    void terminalInsertMeansAfterLastAndUsesPostRemovalIndex() {
        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(planner.plan(
                threeTextColumn(),
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 3)));

        assertEquals(2, accepted.command().destination().index());
    }

    @Test
    void flattenedBoundaryBeforeNextSlotMapsToThatSemanticList() {
        WidgetDefinition parentDefinition = replaceSlots(
                definition(COLUMN),
                listDefinition(FIRST, 7, 0, 10),
                listDefinition(SECOND, 8, 0, 10));
        WidgetCatalog catalog = catalog(parentDefinition, definition(TEXT));
        DesignerDocument document = document(parent(
                ROOT_ID,
                COLUMN,
                slots(
                        FIRST, new WidgetSlot.ListSlot(List.of(node(A_ID, TEXT))),
                        SECOND, new WidgetSlot.ListSlot(List.of(
                                node(B_ID, TEXT), node(C_ID, TEXT))))));

        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(planner.plan(
                document,
                catalog,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 1)));

        assertAll(
                () -> assertEquals(SECOND, accepted.command().destination().slotName()),
                () -> assertEquals(0, accepted.command().destination().index()));
    }

    @Test
    void insertBesideFlattenedSingleSlotIsRejected() {
        DesignerDocument document = document(parent(
                ROOT_ID,
                APP_BAR,
                slots(
                        LEADING, WidgetSlot.SingleSlot.of(node(A_ID, TEXT)),
                        ACTIONS, new WidgetSlot.ListSlot(List.of(node(B_ID, TEXT))))));

        assertRejected(
                planner.plan(
                        document,
                        BUILT_INS,
                        B_ID,
                        new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 0)),
                FlutterDesignerWidgetMovePlanner.RejectionCode.INSERT_REQUIRES_LIST_SLOT);
    }

    @Test
    void onSingleAcceptsEmptySlotAndRejectsOccupiedSlot() {
        WidgetNode emptyTarget = node(B_ID, CENTER);
        DesignerDocument empty = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(node(A_ID, TEXT), emptyTarget)));
        WidgetNode occupiedTarget = new WidgetNode(
                B_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(node(C_ID, TEXT))));
        DesignerDocument occupied = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(node(A_ID, TEXT), occupiedTarget)));

        assertAll(
                () -> {
                    MoveWidget command = accepted(planner.plan(
                            empty,
                            BUILT_INS,
                            A_ID,
                            new FlutterDesignerWidgetMovePlanner.On(B_ID)))
                            .command();
                    assertEquals(new WidgetPlacement(B_ID, CHILD, 0),
                            command.destination());
                },
                () -> assertRejected(
                        planner.plan(
                                occupied,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.On(B_ID)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode.SLOT_FULL));
    }

    @Test
    void existingTextMovesIntoEmptySizedBoxChildWithItsStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into SizedBox");
        WidgetNode emptySizedBox = new WidgetNode(
                B_ID,
                SIZED_BOX,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptySizedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptySizedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptySizedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(
                document,
                BUILT_INS,
                source,
                result);
    }

    @Test
    void existingTextMovesIntoEmptyAspectRatioChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into AspectRatio");
        WidgetNode emptyAspectRatio = WidgetNodePrototypeFactory.create(
                definition(ASPECT_RATIO), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyAspectRatio)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyAspectRatio.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyAspectRatio.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyOpacityChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into Opacity");
        WidgetNode emptyOpacity = WidgetNodePrototypeFactory.create(
                definition(OPACITY), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyOpacity)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyOpacity.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyOpacity.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyAlignChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into Align");
        WidgetNode emptyAlign = WidgetNodePrototypeFactory.create(
                definition(ALIGN), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyAlign)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyAlign.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyAlign.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyFractionallySizedBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into FractionallySizedBox");
        WidgetNode emptyFractionallySizedBox = WidgetNodePrototypeFactory.create(
                definition(FRACTIONALLY_SIZED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyFractionallySizedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyFractionallySizedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyFractionallySizedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyFittedBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into FittedBox");
        WidgetNode emptyFittedBox = WidgetNodePrototypeFactory.create(
                definition(FITTED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyFittedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyFittedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyFittedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyConstrainedBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ConstrainedBox");
        WidgetNode emptyConstrainedBox = WidgetNodePrototypeFactory.create(
                definition(CONSTRAINED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyConstrainedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyConstrainedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyConstrainedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyUnconstrainedBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into UnconstrainedBox");
        WidgetNode emptyUnconstrainedBox = WidgetNodePrototypeFactory.create(
                definition(UNCONSTRAINED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyUnconstrainedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyUnconstrainedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyUnconstrainedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyLimitedBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into LimitedBox");
        WidgetNode emptyLimitedBox = WidgetNodePrototypeFactory.create(
                definition(LIMITED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyLimitedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyLimitedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyLimitedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyOverflowBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into OverflowBox");
        WidgetNode emptyOverflowBox = WidgetNodePrototypeFactory.create(
                definition(OVERFLOW_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyOverflowBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyOverflowBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyOverflowBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyColoredBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ColoredBox");
        WidgetNode emptyColoredBox = WidgetNodePrototypeFactory.create(
                definition(COLORED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyColoredBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyColoredBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyColoredBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyPlaceholderChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into Placeholder");
        WidgetNode emptyPlaceholder = WidgetNodePrototypeFactory.create(
                definition(PLACEHOLDER), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyPlaceholder)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyPlaceholder.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyPlaceholder.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyDecoratedBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into DecoratedBox");
        WidgetNode emptyDecoratedBox = WidgetNodePrototypeFactory.create(
                definition(DECORATED_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyDecoratedBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyDecoratedBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyDecoratedBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyExcludeSemanticsChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ExcludeSemantics");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(EXCLUDE_SEMANTICS), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyClipRectChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ClipRect");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(CLIP_RECT), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyClipOvalChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ClipOval");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(CLIP_OVAL), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyClipRRectChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ClipRRect");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(CLIP_RRECT), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyClipRSuperellipseChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into ClipRSuperellipse");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(CLIP_RSUPERELLIPSE), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyBlockSemanticsChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into BlockSemantics");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.BlockSemantics")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyAbsorbPointerChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into AbsorbPointer");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.AbsorbPointer")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyIgnorePointerChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into IgnorePointer");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.IgnorePointer")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void configuredIndexedSemanticsMovesWithoutLosingIndexChildOrStableIds() {
        WidgetNode child = validText(D_ID, "indexed semantics child remains editable");
        WidgetNode widget = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.IndexedSemantics"),
                Map.of(new PropertyName("index"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-3))),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));
        var result = planner.plan(document, BUILT_INS, widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(widget.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void configuredMergeSemanticsMovesWithoutLosingChildOrStableIds() {
        WidgetNode child = validText(D_ID, "merged semantics child remains editable");
        WidgetNode widget = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.MergeSemantics"),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));
        var result = planner.plan(document, BUILT_INS, widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(widget.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void configuredBlockSemanticsMovesWithoutLosingBooleanChildOrStableIds() {
        WidgetNode child = validText(D_ID, "own semantics and child remain editable");
        WidgetNode widget = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.BlockSemantics"),
                Map.of(new PropertyName("blocking"), new PropertyValue.BooleanValue(true)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));
        var result = planner.plan(document, BUILT_INS, widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(widget.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void configuredAbsorbPointerMovesWithoutLosingBooleansChildOrStableIds() {
        WidgetNode child = validText(D_ID, "ignored child remains editable");
        WidgetNode widget = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.AbsorbPointer"),
                Map.of(new PropertyName("absorbing"), new PropertyValue.BooleanValue(true),
                        new PropertyName("ignoringSemantics"), new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));
        var result = planner.plan(document, BUILT_INS, widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(widget.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void configuredIgnorePointerMovesWithoutLosingBooleansChildOrStableIds() {
        WidgetNode child = validText(D_ID, "ignored child remains editable");
        WidgetNode widget = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.IgnorePointer"),
                Map.of(new PropertyName("ignoring"), new PropertyValue.BooleanValue(true),
                        new PropertyName("ignoringSemantics"), new PropertyValue.BooleanValue(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
        DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));
        var result = planner.plan(document, BUILT_INS, widget.id(),
                new FlutterDesignerWidgetMovePlanner.On(stack.id()));
        MoveWidget command = accepted(result).command();
        assertEquals(widget.id(), command.widgetId());
        assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
    }

    @Test
    void existingTextMovesIntoEmptyIndexedSemanticsChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into IndexedSemantics");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.IndexedSemantics")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyMergeSemanticsChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into MergeSemantics");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.MergeSemantics")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyRepaintBoundaryChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into RepaintBoundary");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.RepaintBoundary")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyPhysicalShapeChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into PhysicalShape");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(new WidgetTypeId("flutter.widgets.PhysicalShape")), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void configuredPhysicalShapeMovesWithBothClipperBranchesPropertiesAndChildPreserved() {
        for (PropertyValue clipper : List.of(
                PropertyValue.ShapeBorderClipperValue.defaultValue(),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_pathClipper",
                        Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()))) {
            WidgetNode child = validText(D_ID, "physical child");
            WidgetNode widget = new WidgetNode(A_ID, new WidgetTypeId("flutter.widgets.PhysicalShape"),
                    Map.of(new PropertyName("clipper"), clipper,
                            new PropertyName("color"), new PropertyValue.ColorValue(0xFF123456L),
                            new PropertyName("shadowColor"), new PropertyValue.ColorValue(0xFF654321L),
                            new PropertyName("elevation"), new PropertyValue.DoubleValue(BigDecimal.TEN),
                            new PropertyName("clipBehavior"), new PropertyValue.EnumValue("Clip", "hardEdge")),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
            WidgetNode stack = listParent(B_ID, STACK, CHILDREN, List.of());
            DesignerDocument document = document(listParent(ROOT_ID, COLUMN, CHILDREN, List.of(widget, stack)));
            var result = planner.plan(document, BUILT_INS, widget.id(),
                    new FlutterDesignerWidgetMovePlanner.On(stack.id()));
            MoveWidget command = accepted(result).command();
            assertEquals(widget.id(), command.widgetId());
            assertEquals(new WidgetPlacement(stack.id(), CHILDREN, 0), command.destination());
            assertAcceptedCommandApplies(document, BUILT_INS, widget, result);
        }
    }

    @Test
    void existingTextMovesIntoEmptyPhysicalModelChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into PhysicalModel");
        WidgetNode empty = WidgetNodePrototypeFactory.create(
                definition(PHYSICAL_MODEL), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source, empty)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(empty.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(new WidgetPlacement(empty.id(), CHILD, 0), command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void indexedStackChildReorderUsesPostRemovalIndexAndPreservesSelectedIndex() {
        WidgetNode first = validText(A_ID, "first");
        WidgetNode second = validText(C_ID, "second");
        WidgetNode indexedStack = new WidgetNode(
                B_ID,
                INDEXED_STACK,
                Map.of(new PropertyName("index"),
                        new PropertyValue.IntegerValue(java.math.BigInteger.ONE)),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(first, second))));
        DesignerDocument document = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(indexedStack)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                second.id(),
                new FlutterDesignerWidgetMovePlanner.Insert(
                        indexedStack.id(), 0));
        MoveWidget command = accepted(result).command();

        assertEquals(
                new MoveWidget(
                        second.id(),
                        new WidgetPlacement(indexedStack.id(), CHILDREN, 0)),
                command);
        assertEquals(new PropertyValue.IntegerValue(java.math.BigInteger.ONE),
                indexedStack.properties().get(new PropertyName("index")));
        assertAcceptedCommandApplies(document, BUILT_INS, second, result);
    }

    @Test
    void existingTextMovesIntoEmptyBaselineChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into Baseline");
        WidgetNode emptyBaseline = WidgetNodePrototypeFactory.create(
                definition(BASELINE), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyBaseline)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyBaseline.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyBaseline.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void existingTextMovesIntoEmptyIntrinsicHeightChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into IntrinsicHeight");
        WidgetNode emptyIntrinsicHeight = WidgetNodePrototypeFactory.create(
                definition(INTRINSIC_HEIGHT), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyIntrinsicHeight)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyIntrinsicHeight.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyIntrinsicHeight.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void intrinsicHeightChildRejectsAllFlexParentDataSourcesOnMove() {
        record RejectedSource(String label, WidgetNode node) {
        }
        List<RejectedSource> sources = List.of(
                new RejectedSource(
                        "Expanded",
                        expanded(A_ID, validText(C_ID, "expanded child"))),
                new RejectedSource(
                        "Flexible",
                        flexible(A_ID, validText(C_ID, "flexible child"))),
                new RejectedSource("Spacer", node(A_ID, SPACER)));

        assertAll(sources.stream().map(source -> () -> {
            WidgetNode intrinsicHeight = WidgetNodePrototypeFactory.create(
                    definition(INTRINSIC_HEIGHT), B_ID);
            DesignerDocument document = document(listParent(
                    ROOT_ID,
                    ROW,
                    CHILDREN,
                    List.of(source.node(), intrinsicHeight)));

            FlutterDesignerWidgetMovePlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerWidgetMovePlanner.Rejected.class,
                    planner.plan(
                            document,
                            BUILT_INS,
                            source.node().id(),
                            new FlutterDesignerWidgetMovePlanner.On(
                                    intrinsicHeight.id())),
                    source.label());
            assertEquals(
                    FlutterDesignerWidgetMovePlanner.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    rejected.code(),
                    source.label());
            assertTrue(rejected.reason().contains(source.label()), source.label());
            assertTrue(rejected.reason().contains(
                    "direct child of Row.children or Column.children"),
                    source.label());
        }));
    }

    @Test
    void existingTextMovesIntoEmptyIntrinsicWidthChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into IntrinsicWidth");
        WidgetNode emptyIntrinsicWidth = WidgetNodePrototypeFactory.create(
                definition(INTRINSIC_WIDTH), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyIntrinsicWidth)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyIntrinsicWidth.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyIntrinsicWidth.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void intrinsicWidthChildRejectsAllFlexParentDataSourcesOnMove() {
        record RejectedSource(String label, WidgetNode node) {
        }
        List<RejectedSource> sources = List.of(
                new RejectedSource(
                        "Expanded",
                        expanded(A_ID, validText(C_ID, "expanded child"))),
                new RejectedSource(
                        "Flexible",
                        flexible(A_ID, validText(C_ID, "flexible child"))),
                new RejectedSource("Spacer", node(A_ID, SPACER)));

        assertAll(sources.stream().map(source -> () -> {
            WidgetNode intrinsicWidth = WidgetNodePrototypeFactory.create(
                    definition(INTRINSIC_WIDTH), B_ID);
            DesignerDocument document = document(listParent(
                    ROOT_ID,
                    ROW,
                    CHILDREN,
                    List.of(source.node(), intrinsicWidth)));

            FlutterDesignerWidgetMovePlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerWidgetMovePlanner.Rejected.class,
                    planner.plan(
                            document,
                            BUILT_INS,
                            source.node().id(),
                            new FlutterDesignerWidgetMovePlanner.On(
                                    intrinsicWidth.id())),
                    source.label());
            assertEquals(
                    FlutterDesignerWidgetMovePlanner.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    rejected.code(),
                    source.label());
            assertTrue(rejected.reason().contains(source.label()), source.label());
            assertTrue(rejected.reason().contains(
                    "direct child of Row.children or Column.children"),
                    source.label());
        }));
    }

    @Test
    void existingTextMovesIntoEmptyOffstageChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into Offstage");
        WidgetNode emptyOffstage = WidgetNodePrototypeFactory.create(
                definition(OFFSTAGE), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyOffstage)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyOffstage.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyOffstage.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void offstageChildRejectsAllFlexParentDataSourcesOnMove() {
        record RejectedSource(String label, WidgetNode node) {
        }
        List<RejectedSource> sources = List.of(
                new RejectedSource(
                        "Expanded",
                        expanded(A_ID, validText(C_ID, "expanded child"))),
                new RejectedSource(
                        "Flexible",
                        flexible(A_ID, validText(C_ID, "flexible child"))),
                new RejectedSource("Spacer", node(A_ID, SPACER)));

        assertAll(sources.stream().map(source -> () -> {
            WidgetNode offstage = WidgetNodePrototypeFactory.create(
                    definition(OFFSTAGE), B_ID);
            DesignerDocument document = document(listParent(
                    ROOT_ID,
                    ROW,
                    CHILDREN,
                    List.of(source.node(), offstage)));

            FlutterDesignerWidgetMovePlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerWidgetMovePlanner.Rejected.class,
                    planner.plan(
                            document,
                            BUILT_INS,
                            source.node().id(),
                            new FlutterDesignerWidgetMovePlanner.On(offstage.id())),
                    source.label());
            assertEquals(
                    FlutterDesignerWidgetMovePlanner.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    rejected.code(),
                    source.label());
            assertTrue(rejected.reason().contains(source.label()), source.label());
            assertTrue(rejected.reason().contains(
                    "direct child of Row.children or Column.children"),
                    source.label());
        }));
    }

    @Test
    void existingTextMovesIntoEmptySizedOverflowBoxChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into SizedOverflowBox");
        WidgetNode emptySizedOverflowBox = WidgetNodePrototypeFactory.create(
                definition(SIZED_OVERFLOW_BOX), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptySizedOverflowBox)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptySizedOverflowBox.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptySizedOverflowBox.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void sizedOverflowBoxChildRejectsAllFlexParentDataSourcesOnMove() {
        record RejectedSource(String label, WidgetNode node) {
        }
        List<RejectedSource> sources = List.of(
                new RejectedSource(
                        "Expanded",
                        expanded(A_ID, validText(C_ID, "expanded child"))),
                new RejectedSource(
                        "Flexible",
                        flexible(A_ID, validText(C_ID, "flexible child"))),
                new RejectedSource("Spacer", node(A_ID, SPACER)));

        assertAll(sources.stream().map(source -> () -> {
            WidgetNode sizedOverflowBox = WidgetNodePrototypeFactory.create(
                    definition(SIZED_OVERFLOW_BOX), B_ID);
            DesignerDocument document = document(listParent(
                    ROOT_ID,
                    ROW,
                    CHILDREN,
                    List.of(source.node(), sizedOverflowBox)));

            FlutterDesignerWidgetMovePlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerWidgetMovePlanner.Rejected.class,
                    planner.plan(
                            document,
                            BUILT_INS,
                            source.node().id(),
                            new FlutterDesignerWidgetMovePlanner.On(
                                    sizedOverflowBox.id())),
                    source.label());
            assertEquals(
                    FlutterDesignerWidgetMovePlanner.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    rejected.code(),
                    source.label());
            assertTrue(rejected.reason().contains(source.label()), source.label());
            assertTrue(rejected.reason().contains(
                    "direct child of Row.children or Column.children"),
                    source.label());
        }));
    }

    @Test
    void existingTextMovesIntoEmptyTransformChildWithStableIdPreserved() {
        WidgetNode source = validText(A_ID, "move into Transform");
        WidgetNode emptyTransform = WidgetNodePrototypeFactory.create(
                definition(TRANSFORM), B_ID);
        DesignerDocument document = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(source, emptyTransform)));

        FlutterDesignerWidgetMovePlanner.Result result = planner.plan(
                document,
                BUILT_INS,
                source.id(),
                new FlutterDesignerWidgetMovePlanner.On(emptyTransform.id()));
        MoveWidget command = accepted(result).command();

        assertEquals(source.id(), command.widgetId());
        assertEquals(
                new WidgetPlacement(emptyTransform.id(), CHILD, 0),
                command.destination());
        assertAcceptedCommandApplies(document, BUILT_INS, source, result);
    }

    @Test
    void transformChildRejectsAllFlexParentDataSourcesOnMove() {
        record RejectedSource(String label, WidgetNode node) {
        }
        List<RejectedSource> sources = List.of(
                new RejectedSource(
                        "Expanded",
                        expanded(A_ID, validText(C_ID, "expanded child"))),
                new RejectedSource(
                        "Flexible",
                        flexible(A_ID, validText(C_ID, "flexible child"))),
                new RejectedSource("Spacer", node(A_ID, SPACER)));

        assertAll(sources.stream().map(source -> () -> {
            WidgetNode transform = WidgetNodePrototypeFactory.create(
                    definition(TRANSFORM), B_ID);
            DesignerDocument document = document(listParent(
                    ROOT_ID,
                    ROW,
                    CHILDREN,
                    List.of(source.node(), transform)));

            FlutterDesignerWidgetMovePlanner.Rejected rejected = assertInstanceOf(
                    FlutterDesignerWidgetMovePlanner.Rejected.class,
                    planner.plan(
                            document,
                            BUILT_INS,
                            source.node().id(),
                            new FlutterDesignerWidgetMovePlanner.On(transform.id())),
                    source.label());
            assertEquals(
                    FlutterDesignerWidgetMovePlanner.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    rejected.code(),
                    source.label());
            assertTrue(rejected.reason().contains(source.label()), source.label());
            assertTrue(rejected.reason().contains(
                    "direct child of Row.children or Column.children"),
                    source.label());
        }));
    }

    @Test
    void rejectsRootAndMoveIntoOwnDescendant() {
        WidgetNode inner = listParent(
                C_ID, COLUMN, CHILDREN, List.of(node(D_ID, TEXT)));
        WidgetNode source = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(inner)));
        DesignerDocument nested = document(listParent(
                ROOT_ID, COLUMN, CHILDREN, List.of(source)));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                nested,
                                BUILT_INS,
                                ROOT_ID,
                                new FlutterDesignerWidgetMovePlanner.On(A_ID)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode.ROOT_MOVE_FORBIDDEN),
                () -> assertRejected(
                        planner.plan(
                                nested,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.On(C_ID)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .DESTINATION_INSIDE_SUBTREE));
    }

    @Test
    void rejectsSameListAndSameSingleNoOps() {
        DesignerDocument list = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(node(A_ID, TEXT), node(B_ID, TEXT))));
        DesignerDocument single = document(new WidgetNode(
                ROOT_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(node(A_ID, TEXT)))));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                list,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 1)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode.NO_CHANGE),
                () -> assertRejected(
                        planner.plan(
                                single,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.On(ROOT_ID)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode.NO_CHANGE));
    }

    @Test
    void onContainerRejectsAmbiguousAndIncompatibleCatalogTargets() {
        WidgetNode sourceParent = listParent(
                B_ID, COLUMN, CHILDREN, List.of(node(A_ID, TEXT)));
        DesignerDocument scaffold = document(new WidgetNode(
                ROOT_ID,
                SCAFFOLD,
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.of(sourceParent))));

        WidgetDefinition restrictiveCenter = replaceSlots(
                definition(CENTER),
                new SlotDefinition(
                        CHILD,
                        DartParameter.named(2, false),
                        SlotCardinality.SINGLE,
                        0,
                        1,
                        new SlotAcceptance.ExactTypes(List.of(COLUMN))));
        DesignerDocument center = document(new WidgetNode(
                ROOT_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(sourceParent))));
        WidgetCatalog restrictive = catalog(
                restrictiveCenter, definition(COLUMN), definition(TEXT));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                scaffold,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.On(ROOT_ID)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .AMBIGUOUS_DESTINATION),
                () -> assertRejected(
                        planner.plan(
                                center,
                                restrictive,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.On(ROOT_ID)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .NO_COMPATIBLE_DESTINATION));
    }

    @Test
    void exactNamedSlotResolvesScaffoldAmbiguityWithoutGuessing() {
        WidgetNode sourceParent = listParent(
                B_ID, COLUMN, CHILDREN, List.of(node(A_ID, TEXT)));
        DesignerDocument document = document(new WidgetNode(
                ROOT_ID,
                SCAFFOLD,
                Map.of(),
                Map.of(
                        BODY, WidgetSlot.SingleSlot.of(sourceParent),
                        slot("floatingActionButton"), WidgetSlot.SingleSlot.empty())));

        FlutterDesignerWidgetMovePlanner.Accepted accepted = accepted(planner.plan(
                document,
                BUILT_INS,
                A_ID,
                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                        ROOT_ID, slot("floatingActionButton"), 0)));

        assertEquals(
                new MoveWidget(
                        A_ID,
                        new WidgetPlacement(
                                ROOT_ID, slot("floatingActionButton"), 0)),
                accepted.command());
    }

    @Test
    void exactNamedListSlotUsesPostRemovalIndexAndRejectsNoOp() {
        DesignerDocument document = threeTextColumn();

        assertAll(
                () -> assertEquals(
                        new MoveWidget(
                                A_ID,
                                new WidgetPlacement(ROOT_ID, CHILDREN, 2)),
                        accepted(planner.plan(
                                document,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        ROOT_ID, CHILDREN, 2)))
                                .command()),
                () -> assertRejected(
                        planner.plan(
                                document,
                                BUILT_INS,
                                B_ID,
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        ROOT_ID, CHILDREN, 1)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode.NO_CHANGE));
    }

    @Test
    void exactNamedSlotReusesCatalogAcceptanceAndCardinalityChecks() {
        WidgetNode sourceParent = listParent(
                B_ID, COLUMN, CHILDREN, List.of(node(A_ID, TEXT)));
        DesignerDocument document = document(new WidgetNode(
                ROOT_ID,
                SCAFFOLD,
                Map.of(),
                Map.of(
                        slot("appBar"), WidgetSlot.SingleSlot.empty(),
                        BODY, WidgetSlot.SingleSlot.of(sourceParent))));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                document,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        ROOT_ID, slot("appBar"), 0)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET),
                () -> assertRejected(
                        planner.plan(
                                document,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        ROOT_ID, slot("missing"), 0)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .SLOT_DEFINITION_MISSING));
    }

    @Test
    void enforcesDestinationCapacityButAllowsReorderAtCapacity() {
        WidgetDefinition twoLists = replaceSlots(
                definition(COLUMN),
                listDefinition(SOURCE, 7, 0, 10),
                listDefinition(DESTINATION, 8, 0, 2));
        WidgetCatalog crossSlotCatalog = catalog(twoLists, definition(TEXT));
        DesignerDocument crossSlot = document(parent(
                ROOT_ID,
                COLUMN,
                slots(
                        SOURCE, new WidgetSlot.ListSlot(List.of(node(A_ID, TEXT))),
                        DESTINATION, new WidgetSlot.ListSlot(List.of(
                                node(B_ID, TEXT), node(C_ID, TEXT))))));

        WidgetDefinition maxThree = replaceSlots(
                definition(COLUMN),
                listDefinition(CHILDREN, 7, 0, 3));
        WidgetCatalog reorderCatalog = catalog(maxThree, definition(TEXT));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                crossSlot,
                                crossSlotCatalog,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 1)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode.SLOT_FULL),
                () -> assertEquals(
                        2,
                        accepted(planner.plan(
                                threeTextColumn(),
                                reorderCatalog,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 3)))
                                .command().destination().index()));
    }

    @Test
    void rejectsMoveThatViolatesSourceSlotMinimum() {
        WidgetDefinition definition = replaceSlots(
                definition(COLUMN),
                listDefinition(SOURCE, 7, 1, 10),
                listDefinition(DESTINATION, 8, 0, 10));
        WidgetCatalog catalog = catalog(definition, definition(TEXT));
        DesignerDocument document = document(parent(
                ROOT_ID,
                COLUMN,
                slots(
                        SOURCE, new WidgetSlot.ListSlot(List.of(node(A_ID, TEXT))),
                        DESTINATION, new WidgetSlot.ListSlot(List.of(node(B_ID, TEXT))))));

        assertRejected(
                planner.plan(
                        document,
                        catalog,
                        A_ID,
                        new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 1)),
                FlutterDesignerWidgetMovePlanner.RejectionCode.SOURCE_SLOT_REQUIRED);
    }

    @Test
    void insertRevalidatesSlotAcceptanceAndModelCardinality() {
        WidgetDefinition rejectsText = replaceSlots(
                definition(COLUMN),
                listDefinition(SOURCE, 7, 0, 10),
                new SlotDefinition(
                        DESTINATION,
                        DartParameter.named(8, false),
                        SlotCardinality.LIST,
                        0,
                        10,
                        new SlotAcceptance.ExactTypes(List.of(CENTER))));
        WidgetCatalog rejectingCatalog = catalog(
                rejectsText, definition(TEXT), definition(CENTER));
        DesignerDocument rejectedType = document(parent(
                ROOT_ID,
                COLUMN,
                slots(
                        SOURCE, new WidgetSlot.ListSlot(List.of(node(A_ID, TEXT))),
                        DESTINATION, new WidgetSlot.ListSlot(List.of(node(B_ID, TEXT))))));

        WidgetDefinition expectsLists = replaceSlots(
                definition(COLUMN),
                listDefinition(SOURCE, 7, 0, 10),
                listDefinition(DESTINATION, 8, 0, 10));
        WidgetCatalog listCatalog = catalog(expectsLists, definition(TEXT));
        DesignerDocument wrongCardinality = document(parent(
                ROOT_ID,
                COLUMN,
                slots(
                        SOURCE, new WidgetSlot.ListSlot(List.of(node(A_ID, TEXT))),
                        DESTINATION, WidgetSlot.SingleSlot.of(node(B_ID, TEXT)))));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                rejectedType,
                                rejectingCatalog,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 1)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .SLOT_REJECTS_WIDGET),
                () -> assertRejected(
                        planner.plan(
                                wrongCardinality,
                                listCatalog,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 1)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .SLOT_CARDINALITY_MISMATCH));
    }

    @Test
    void rejectsInvalidOrUnanchoredFlattenedInsertIndex() {
        DesignerDocument populated = threeTextColumn();
        DesignerDocument emptyTarget = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(
                        node(A_ID, TEXT),
                        listParent(D_ID, COLUMN, CHILDREN, List.of()))));

        assertAll(
                () -> assertRejected(
                        planner.plan(
                                populated,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, -1)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .INSERT_INDEX_OUT_OF_BOUNDS),
                () -> assertRejected(
                        planner.plan(
                                populated,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 4)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .INSERT_INDEX_OUT_OF_BOUNDS),
                () -> assertRejected(
                        planner.plan(
                                emptyTarget,
                                BUILT_INS,
                                A_ID,
                                new FlutterDesignerWidgetMovePlanner.Insert(
                                        D_ID, 0)),
                        FlutterDesignerWidgetMovePlanner.RejectionCode
                                .AMBIGUOUS_DESTINATION));
    }

    @Test
    void everyRepresentativeAcceptedPlacementAppliesAndPreservesExactSubtree() {
        WidgetNode nestedText = validText(D_ID, "nested");
        WidgetNode nestedSource = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(nestedText)));
        DesignerDocument sameList = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(
                        nestedSource,
                        validText(B_ID, "second"),
                        validText(C_ID, "third"))));
        assertAcceptedCommandApplies(
                sameList,
                BUILT_INS,
                nestedSource,
                planner.plan(
                        sameList,
                        BUILT_INS,
                        A_ID,
                        new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 3)));

        WidgetNode singleSource = validText(D_ID, "single");
        WidgetNode populatedCenter = new WidgetNode(
                A_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(singleSource)));
        WidgetNode emptyCenter = new WidgetNode(
                B_ID,
                CENTER,
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
        DesignerDocument crossParent = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(populatedCenter, emptyCenter)));
        assertAcceptedCommandApplies(
                crossParent,
                BUILT_INS,
                singleSource,
                planner.plan(
                        crossParent,
                        BUILT_INS,
                        D_ID,
                        new FlutterDesignerWidgetMovePlanner.On(B_ID)));

        WidgetDefinition maxThree = replaceSlots(
                definition(COLUMN),
                listDefinition(CHILDREN, 7, 0, 3));
        WidgetCatalog capacityCatalog = catalog(maxThree, definition(TEXT));
        DesignerDocument atCapacity = document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(
                        validText(A_ID, "same"),
                        validText(B_ID, "same"),
                        validText(C_ID, "same"))));
        assertAcceptedCommandApplies(
                atCapacity,
                capacityCatalog,
                validText(A_ID, "same"),
                planner.plan(
                        atCapacity,
                        capacityCatalog,
                        A_ID,
                        new FlutterDesignerWidgetMovePlanner.Insert(ROOT_ID, 3)));
    }

    private static void assertAcceptedCommandApplies(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetNode expectedSubtree,
            FlutterDesignerWidgetMovePlanner.Result result) {
        MoveWidget command = accepted(result).command();
        Object applied = applyWithCanonicalTransformer(document, catalog, command);
        assertEquals(
                "APPLIED",
                invoke(applied, "status").toString(),
                () -> "Every planner-accepted placement must pass the canonical "
                + "transformer: " + invoke(applied, "diagnostic"));
        @SuppressWarnings("unchecked")
        Optional<DesignerDocument> transformed =
                (Optional<DesignerDocument>) invoke(applied, "document");
        assertEquals(
                expectedSubtree,
                find(transformed.orElseThrow().root(), command.widgetId()),
                "MoveWidget must preserve the complete source subtree and stable IDs");
    }

    private static Object applyWithCanonicalTransformer(
            DesignerDocument document,
            WidgetCatalog catalog,
            DesignerCommand command) {
        try {
            Class<?> type = Class.forName(
                    "dev.flutter.netbeans.designer.command.DesignerCommandTransformer");
            var constructor = type.getDeclaredConstructor(
                    WidgetCatalog.class, ValidationLimits.class);
            constructor.setAccessible(true);
            Object transformer = constructor.newInstance(
                    catalog, ValidationLimits.defaults());
            var apply = type.getDeclaredMethod(
                    "apply", DesignerDocument.class, DesignerCommand.class);
            apply.setAccessible(true);
            return apply.invoke(transformer, document, command);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(
                    "Could not invoke the canonical command transformer", failure);
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(
                    "Could not read canonical transformer result " + methodName,
                    failure);
        }
    }

    private static WidgetNode find(WidgetNode node, StableId id) {
        WidgetNode found = findOrNull(node, id);
        if (found != null) {
            return found;
        }
        throw new AssertionError("Widget not found after accepted move: " + id);
    }

    private static WidgetNode findOrNull(WidgetNode node, StableId id) {
        if (node.id().equals(id)) {
            return node;
        }
        for (WidgetSlot slot : node.slots().values()) {
            if (slot instanceof WidgetSlot.SingleSlot single) {
                if (single.child().isPresent()) {
                    WidgetNode found = findOrNull(single.child().orElseThrow(), id);
                    if (found != null) {
                        return found;
                    }
                }
            } else {
                for (WidgetNode child : ((WidgetSlot.ListSlot) slot).children()) {
                    WidgetNode found = findOrNull(child, id);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
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

    private static FlutterDesignerWidgetMovePlanner.Accepted accepted(
            FlutterDesignerWidgetMovePlanner.Result result) {
        return assertInstanceOf(
                FlutterDesignerWidgetMovePlanner.Accepted.class, result);
    }

    private static void assertRejected(
            FlutterDesignerWidgetMovePlanner.Result result,
            FlutterDesignerWidgetMovePlanner.RejectionCode expected) {
        FlutterDesignerWidgetMovePlanner.Rejected rejected = assertInstanceOf(
                FlutterDesignerWidgetMovePlanner.Rejected.class, result);
        assertEquals(expected, rejected.code(), rejected.reason());
    }

    private static DesignerDocument threeTextColumn() {
        return document(listParent(
                ROOT_ID,
                COLUMN,
                CHILDREN,
                List.of(node(A_ID, TEXT), node(B_ID, TEXT), node(C_ID, TEXT))));
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

    private static WidgetNode node(StableId id, WidgetTypeId type) {
        return WidgetNode.empty(id, type);
    }

    private static WidgetNode validText(StableId id, String data) {
        return new WidgetNode(
                id,
                TEXT,
                Map.of(DATA, new PropertyValue.StringValue(data)),
                Map.of());
    }

    private static WidgetNode listParent(
            StableId id,
            WidgetTypeId type,
            SlotName slot,
            List<WidgetNode> children) {
        return parent(
                id,
                type,
                Map.of(slot, new WidgetSlot.ListSlot(children)));
    }

    private static WidgetNode parent(
            StableId id,
            WidgetTypeId type,
            Map<SlotName, WidgetSlot> slots) {
        return new WidgetNode(id, type, Map.of(), slots);
    }

    private static Map<SlotName, WidgetSlot> slots(
            SlotName firstName,
            WidgetSlot firstSlot,
            SlotName secondName,
            WidgetSlot secondSlot) {
        LinkedHashMap<SlotName, WidgetSlot> result = new LinkedHashMap<>();
        result.put(firstName, firstSlot);
        result.put(secondName, secondSlot);
        return result;
    }

    private static SlotDefinition listDefinition(
            SlotName name,
            int order,
            int minimum,
            int maximum) {
        return new SlotDefinition(
                name,
                DartParameter.named(order, false),
                SlotCardinality.LIST,
                minimum,
                maximum,
                new SlotAcceptance.AnyWidget());
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

    private static WidgetDefinition definition(WidgetTypeId type) {
        return BUILT_INS.find(type).orElseThrow();
    }

    private static WidgetCatalog catalog(WidgetDefinition... definitions) {
        return WidgetCatalog.strict(new ArrayList<>(List.of(definitions)));
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }

    private static SlotName slot(String value) {
        return new SlotName(value);
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }
}
