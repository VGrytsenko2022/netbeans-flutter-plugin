package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
    private static final WidgetTypeId LIST_VIEW = type("flutter.widgets.ListView");
    private static final WidgetTypeId GRID_VIEW = type("flutter.widgets.GridView");
    private static final WidgetTypeId ICON = type("flutter.widgets.Icon");
    private static final WidgetTypeId IMAGE = type("flutter.widgets.Image");
    private static final SlotName APP_BAR_SLOT = new SlotName("appBar");
    private static final SlotName LEADING = new SlotName("leading");
    private static final SlotName TITLE = new SlotName("title");
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
            assertTrue(failure.reason().contains("nesting would move the inner"),
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
        assertTrue(nested.reason().contains("Cannot wrap Expanded '" + FIRST_ID));
        assertTrue(nested.reason().contains("required direct Row.children or "
                + "Column.children parent"));
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

    private static MatrixTargetCase target(
            String name,
            WidgetTypeId parentType,
            SlotName slot) {
        return new MatrixTargetCase(name, document(prototype(parentType)), slot);
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
        return WidgetNodePrototypeFactory.create(definition(type), ROOT_ID);
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
