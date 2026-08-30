package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.SlotAcceptance;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.AddWidget;
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
import java.math.BigDecimal;
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
    private static final WidgetTypeId TEXT = type("flutter.widgets.Text");
    private static final WidgetTypeId COLUMN = type("flutter.widgets.Column");
    private static final WidgetTypeId ROW = type("flutter.widgets.Row");
    private static final WidgetTypeId PADDING = type("flutter.widgets.Padding");
    private static final WidgetTypeId CENTER = type("flutter.widgets.Center");
    private static final WidgetTypeId SIZED_BOX = type("flutter.widgets.SizedBox");
    private static final WidgetTypeId ICON = type("flutter.widgets.Icon");
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
    private static final PropertyName ENABLED = new PropertyName("enabled");
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
    void plansAllOneHundredTwentyAnyWidgetCompatibilityCellsWithExactPrototypes() {
        List<CoreSourceCase> sources = coreSources();
        List<MatrixTargetCase> targets = List.of(
                target("Scaffold.body", SCAFFOLD, BODY),
                target("Scaffold.floatingActionButton", SCAFFOLD,
                        FLOATING_ACTION_BUTTON),
                target("Column.children", COLUMN, CHILDREN),
                target("Row.children", ROW, CHILDREN),
                target("Padding.child", PADDING, CHILD),
                target("Center.child", CENTER, CHILD),
                target("SizedBox.child", SIZED_BOX, CHILD),
                target("ElevatedButton.child", ELEVATED_BUTTON, CHILD),
                target("AppBar.leading", APP_BAR, LEADING),
                target("AppBar.title", APP_BAR, TITLE),
                target("AppBar.actions", APP_BAR, ACTIONS),
                target("AppBar.flexibleSpace", APP_BAR, FLEXIBLE_SPACE));

        assertEquals(10, sources.size());
        assertEquals(12, targets.size());
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
    void admitsOnlyAppBarAcrossBothPreferredSizeTraitSlotsForExact140CellMatrix() {
        AtomicInteger allocations = new AtomicInteger();
        List<MatrixTargetCase> traitTargets = List.of(
                target("Scaffold.appBar", SCAFFOLD, APP_BAR_SLOT),
                target("AppBar.bottom", APP_BAR, BOTTOM));

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
                "all 18 rejected trait cells must fail before stable-id allocation");
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
    void enforcesTerminalAppendAcrossBothListSlotsForEveryCoreSource() {
        AtomicInteger rejectedAllocations = new AtomicInteger();
        List<SingleTargetCase> listTargets = List.of(
                new SingleTargetCase("Column.children", COLUMN, CHILDREN),
                new SingleTargetCase("Row.children", ROW, CHILDREN),
                new SingleTargetCase("AppBar.actions", APP_BAR, ACTIONS));

        assertAll(listTargets.stream().flatMap(target -> coreSources().stream()
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
        assertEquals(0, rejectedAllocations.get(),
                "non-terminal list insertions must fail before stable-id allocation");
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

    private static WidgetDefinition definition(WidgetTypeId type) {
        return BUILT_INS.find(type).orElseThrow();
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
