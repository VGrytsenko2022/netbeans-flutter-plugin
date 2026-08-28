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
import java.util.ArrayList;
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
    private static final WidgetTypeId TEXT = type("flutter.widgets.Text");
    private static final WidgetTypeId COLUMN = type("flutter.widgets.Column");
    private static final WidgetTypeId ROW = type("flutter.widgets.Row");
    private static final WidgetTypeId CENTER = type("flutter.widgets.Center");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName CHILD = new SlotName("child");
    private static final PropertyName DATA = new PropertyName("data");
    private static final StableId DOCUMENT_ID = id("14f6c16f-893b-44d0-b809-edbd51bbcdaa");
    private static final StableId ROOT_ID = id("0209809f-351a-4ce7-8c07-1ec625b1e109");
    private static final StableId FIRST_ID = id("710c4ad9-c3cf-434e-af1e-5217ac38aa92");
    private static final StableId NEW_ID = id("805b5a85-397c-4d5f-ae6f-2171456d7a5a");

    private final FlutterDesignerPaletteDropPlanner planner =
            new FlutterDesignerPaletteDropPlanner();

    @Test
    void plansTextAsATerminalAppendForBothSupportedParents() {
        List<AcceptedCase> cases = List.of(
                new AcceptedCase("empty Column", document(parent(COLUMN, List.of())), 0),
                new AcceptedCase(
                        "non-empty Row",
                        document(parent(ROW, List.of(text(FIRST_ID, "existing")))),
                        1),
                new AcceptedCase(
                        "absent optional model slot",
                        document(WidgetNode.empty(ROOT_ID, COLUMN)),
                        0));

        assertAll(cases.stream().map(testCase -> (Executable) () -> {
            AtomicInteger allocations = new AtomicInteger();
            FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                    testCase.document(),
                    BUILT_INS,
                    TEXT,
                    ROOT_ID,
                    CHILDREN,
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
            assertEquals(CHILDREN, command.destination().slotName(), testCase.name());
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
        WidgetCatalog singleChildren = catalog(text, replaceSlots(
                column,
                new SlotDefinition(
                        CHILDREN,
                        builtInChildren.parameter(),
                        SlotCardinality.SINGLE,
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
                        "wrong source",
                        emptyColumn,
                        BUILT_INS,
                        CENTER,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.UNSUPPORTED_WIDGET_TYPE),
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
                        "wrong target",
                        document(WidgetNodePrototypeFactory.create(definition(CENTER), ROOT_ID)),
                        BUILT_INS,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.UNSUPPORTED_PARENT),
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
                        "wrong slot",
                        emptyColumn,
                        BUILT_INS,
                        TEXT,
                        ROOT_ID,
                        CHILD,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.UNSUPPORTED_SLOT),
                rejection(
                        "slot definition missing",
                        emptyColumn,
                        catalog(text, replaceSlots(column)),
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_DEFINITION_MISSING),
                rejection(
                        "catalog slot is not a list",
                        emptyColumn,
                        singleChildren,
                        TEXT,
                        ROOT_ID,
                        CHILDREN,
                        0,
                        FlutterDesignerPaletteDropPlanner.RejectionCode.CATALOG_SLOT_CARDINALITY_MISMATCH),
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

        FlutterDesignerPaletteDropPlanner.Result result = planner.plan(
                document(parent(COLUMN, children)),
                BUILT_INS,
                TEXT,
                ROOT_ID,
                CHILDREN,
                WidgetSlot.MAX_LIST_CHILDREN,
                () -> NEW_ID);

        FlutterDesignerPaletteDropPlanner.Rejected rejected = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Rejected.class, result);
        assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_FULL,
                rejected.code());
        assertTrue(rejected.reason().contains(
                Integer.toString(WidgetSlot.MAX_LIST_CHILDREN)));
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
        return new WidgetNode(
                ROOT_ID,
                type,
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(children)));
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

    private record AcceptedCase(String name, DesignerDocument document, int index) {
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
