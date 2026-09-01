package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlutterDesignerPaletteTreeDropAdapterTest {
    private static final WidgetCatalog CATALOG =
            BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId SCAFFOLD =
            type("flutter.material.Scaffold");
    private static final WidgetTypeId COLUMN =
            type("flutter.widgets.Column");
    private static final WidgetTypeId CENTER =
            type("flutter.widgets.Center");
    private static final WidgetTypeId SIZED_BOX =
            type("flutter.widgets.SizedBox");
    private static final WidgetTypeId ASPECT_RATIO =
            type("flutter.widgets.AspectRatio");
    private static final WidgetTypeId OPACITY = type("flutter.widgets.Opacity");
    private static final WidgetTypeId ALIGN = type("flutter.widgets.Align");
    private static final WidgetTypeId TEXT = type("flutter.widgets.Text");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName BODY = new SlotName("body");
    private static final PropertyName DATA = new PropertyName("data");
    private static final PropertyName ASPECT_RATIO_VALUE =
            new PropertyName("aspectRatio");
    private static final PropertyName OPACITY_VALUE = new PropertyName("opacity");
    private static final StableId DOCUMENT_ID =
            id("f56a6bbb-fe08-4977-9597-a8273aa143eb");
    private static final StableId ROOT_ID =
            id("5a975d36-09fd-4012-8c33-083cb9de4bf5");
    private static final StableId FIRST_ID =
            id("939d0528-5cea-4550-a620-ff943214a7a2");
    private static final StableId NEW_ID =
            id("cce2050f-8846-4378-843e-58371d52d1c5");

    @Test
    void previewIsRepeatableNonConsumingAndCommitReplansTerminalIndex() {
        Fixture fixture = fixture(TEXT);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument empty = document(column(List.of()));
        AtomicInteger allocations = new AtomicInteger();

        var first = fixture.adapter().preview(
                transferable,
                DnDConstants.ACTION_MOVE,
                empty,
                CATALOG,
                ROOT_ID);
        var second = fixture.adapter().preview(
                transferable,
                DnDConstants.ACTION_MOVE,
                empty,
                CATALOG,
                ROOT_ID);

        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                first);
        assertEquals(prepared, second);
        assertAll(
                () -> assertEquals(TEXT, prepared.widgetType()),
                () -> assertEquals(ROOT_ID, prepared.parentId()),
                () -> assertEquals(CHILDREN, prepared.slotName()),
                () -> assertEquals(0, prepared.insertionIndex()),
                () -> assertEquals(0, allocations.get()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isPresent()));

        DesignerDocument changed = document(column(List.of(
                text(FIRST_ID, "existing"))));
        var commit = fixture.adapter().commit(
                prepared,
                transferable,
                DnDConstants.ACTION_MOVE,
                changed,
                CATALOG,
                () -> {
                    allocations.incrementAndGet();
                    return NEW_ID;
                });

        AddWidget command = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Committed.class,
                commit).command();
        assertAll(
                () -> assertEquals(ROOT_ID,
                        command.destination().parentId()),
                () -> assertEquals(CHILDREN,
                        command.destination().slotName()),
                () -> assertEquals(1, command.destination().index()),
                () -> assertEquals(NEW_ID, command.widget().id()),
                () -> assertEquals(TEXT, command.widget().type()),
                () -> assertEquals(1, allocations.get()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty()));

        var repeatedCommit = fixture.adapter().commit(
                prepared,
                transferable,
                DnDConstants.ACTION_MOVE,
                changed,
                CATALOG,
                () -> {
                    allocations.incrementAndGet();
                    return StableId.random();
                });
        var rejection = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                repeatedCommit);
        assertEquals(
                FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                        .TOKEN_UNAVAILABLE,
                rejection.code());
        assertEquals(1, allocations.get());
    }

    @Test
    void sizedBoxTokenPreviewsAndCommitsTheExactCatalogPrototype() {
        Fixture fixture = fixture(SIZED_BOX);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument document = document(column(List.of()));

        var preview = fixture.adapter().preview(
                transferable,
                DnDConstants.ACTION_MOVE,
                document,
                CATALOG,
                ROOT_ID);

        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                preview);
        assertAll(
                () -> assertEquals(SIZED_BOX, prepared.widgetType()),
                () -> assertEquals(ROOT_ID, prepared.parentId()),
                () -> assertEquals(CHILDREN, prepared.slotName()),
                () -> assertEquals(0, prepared.insertionIndex()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isPresent(),
                        "preview must not consume the palette authority"));

        var committed = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Committed.class,
                fixture.adapter().commit(
                        prepared,
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        () -> NEW_ID));
        AddWidget command = committed.command();
        assertAll(
                () -> assertEquals(ROOT_ID,
                        command.destination().parentId()),
                () -> assertEquals(CHILDREN,
                        command.destination().slotName()),
                () -> assertEquals(0, command.destination().index()),
                () -> assertEquals(NEW_ID, command.widget().id()),
                () -> assertEquals(SIZED_BOX, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit must consume the palette authority exactly once"));
    }

    @Test
    void aspectRatioTokenPreviewsAndCommitsRequiredDefaultAndEmptyChild() {
        Fixture fixture = fixture(ASPECT_RATIO);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument document = document(column(List.of()));

        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                fixture.adapter().preview(
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));
        assertAll(
                () -> assertEquals(ASPECT_RATIO, prepared.widgetType()),
                () -> assertEquals(ROOT_ID, prepared.parentId()),
                () -> assertEquals(CHILDREN, prepared.slotName()),
                () -> assertEquals(0, prepared.insertionIndex()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isPresent()));

        AddWidget command = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Committed.class,
                fixture.adapter().commit(
                        prepared,
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        () -> NEW_ID)).command();
        assertAll(
                () -> assertEquals(NEW_ID, command.widget().id()),
                () -> assertEquals(ASPECT_RATIO, command.widget().type()),
                () -> assertEquals(
                        Map.of(ASPECT_RATIO_VALUE,
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the AspectRatio palette authority once"));
    }

    @Test
    void opacityTokenPreviewsAndCommitsOpaqueDefaultAndEmptyChild() {
        Fixture fixture = fixture(OPACITY);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument document = document(column(List.of()));

        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                fixture.adapter().preview(
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));
        assertAll(
                () -> assertEquals(OPACITY, prepared.widgetType()),
                () -> assertEquals(ROOT_ID, prepared.parentId()),
                () -> assertEquals(CHILDREN, prepared.slotName()),
                () -> assertEquals(0, prepared.insertionIndex()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isPresent()));

        AddWidget command = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Committed.class,
                fixture.adapter().commit(
                        prepared,
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        () -> NEW_ID)).command();
        assertAll(
                () -> assertEquals(NEW_ID, command.widget().id()),
                () -> assertEquals(OPACITY, command.widget().type()),
                () -> assertEquals(
                        Map.of(OPACITY_VALUE,
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the Opacity palette authority once"));
    }

    @Test
    void alignTokenPreviewsAndCommitsOptionalPropertiesAndEmptyChild() {
        Fixture fixture = fixture(ALIGN);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument document = document(column(List.of()));

        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                fixture.adapter().preview(
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));
        assertAll(
                () -> assertEquals(ALIGN, prepared.widgetType()),
                () -> assertEquals(ROOT_ID, prepared.parentId()),
                () -> assertEquals(CHILDREN, prepared.slotName()),
                () -> assertEquals(0, prepared.insertionIndex()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isPresent()));

        AddWidget command = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Committed.class,
                fixture.adapter().commit(
                        prepared,
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        () -> NEW_ID)).command();
        assertAll(
                () -> assertEquals(NEW_ID, command.widget().id()),
                () -> assertEquals(ALIGN, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the Align palette authority once"));
    }

    @Test
    void resolvesOnlyOneCurrentlyAvailableCompatibleCatalogSlot() {
        List<AcceptedCase> accepted = List.of(
                new AcceptedCase(
                        "empty Column",
                        document(column(List.of())),
                        CHILDREN),
                new AcceptedCase(
                        "empty Center",
                        document(prototype(CENTER)),
                        CHILD));

        assertAll(accepted.stream().map(testCase -> () -> {
            Fixture fixture = fixture(TEXT);
            var result = fixture.adapter().preview(
                    new StringSelection(fixture.token()),
                    DnDConstants.ACTION_MOVE,
                    testCase.document(),
                    CATALOG,
                    ROOT_ID);
            var prepared = assertInstanceOf(
                    FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                    result,
                    testCase.name());
            assertEquals(testCase.slotName(), prepared.slotName(),
                    testCase.name());
            assertEquals(0, prepared.insertionIndex(), testCase.name());
        }));

        List<RejectedCase> rejected = List.of(
                new RejectedCase(
                        "leaf Text",
                        document(text(ROOT_ID, "leaf")),
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .NO_COMPATIBLE_DESTINATION),
                new RejectedCase(
                        "occupied Center",
                        document(center(text(FIRST_ID, "occupied"))),
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .NO_COMPATIBLE_DESTINATION),
                new RejectedCase(
                        "ambiguous Scaffold",
                        document(prototype(SCAFFOLD)),
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .AMBIGUOUS_DESTINATION),
                new RejectedCase(
                        "ambiguous Scaffold with occupied body",
                        document(withSlot(
                                prototype(SCAFFOLD),
                                BODY,
                                WidgetSlot.SingleSlot.of(
                                        text(FIRST_ID, "occupied body")))),
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .AMBIGUOUS_DESTINATION));

        assertAll(rejected.stream().map(testCase -> () -> {
            Fixture fixture = fixture(TEXT);
            var result = fixture.adapter().preview(
                    new StringSelection(fixture.token()),
                    DnDConstants.ACTION_MOVE,
                    testCase.document(),
                    CATALOG,
                    ROOT_ID);
            var failure = assertInstanceOf(
                    FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                    result,
                    testCase.name());
            assertEquals(testCase.code(), failure.code(), testCase.name());
            assertFalse(failure.reason().isBlank(), testCase.name());
        }));
    }

    @Test
    void unsupportedAndHostilePreviewsFailWithoutConsumingAuthority() {
        Fixture fixture = fixture(TEXT);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument document = document(column(List.of()));

        var copied = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                fixture.adapter().preview(
                        transferable,
                        DnDConstants.ACTION_COPY,
                        document,
                        CATALOG,
                        ROOT_ID));
        var arbitrary = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                fixture.adapter().preview(
                        new StringSelection("flutter.widgets.Text"),
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));

        assertAll(
                () -> assertEquals(
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .UNSUPPORTED_ACTION,
                        copied.code()),
                () -> assertEquals(
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .TOKEN_UNAVAILABLE,
                        arbitrary.code()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isPresent()));

        assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                fixture.adapter().preview(
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));
    }

    @Test
    void commitRequiresTheExactPreparedTransferAndBurnsChangedAuthority() {
        Fixture fixture = fixture(TEXT);
        StringSelection original = new StringSelection(fixture.token());
        DesignerDocument document = document(column(List.of()));
        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                fixture.adapter().preview(
                        original,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));
        String replacement = fixture.registry().issue(CENTER).orElseThrow();
        AtomicInteger allocations = new AtomicInteger();

        var result = fixture.adapter().commit(
                prepared,
                new StringSelection(replacement),
                DnDConstants.ACTION_MOVE,
                document,
                CATALOG,
                () -> {
                    allocations.incrementAndGet();
                    return NEW_ID;
                });

        var rejected = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                result);
        assertAll(
                () -> assertEquals(
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .TRANSFER_CHANGED,
                        rejected.code()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(original).isEmpty()),
                () -> assertEquals(0, allocations.get()));
    }

    private static Fixture fixture(WidgetTypeId type) {
        FlutterDesignerPaletteDragRegistry registry =
                new FlutterDesignerPaletteDragRegistry();
        FlutterDesignerPaletteDragLifecycle lifecycle =
                new FlutterDesignerPaletteDragLifecycle(registry);
        String token = registry.issueReplacingOutstanding(type).orElseThrow();
        return new Fixture(
                registry,
                lifecycle,
                new FlutterDesignerPaletteTreeDropAdapter(lifecycle),
                token);
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

    private static WidgetNode column(List<WidgetNode> children) {
        return withSlot(
                prototype(COLUMN),
                CHILDREN,
                new WidgetSlot.ListSlot(children));
    }

    private static WidgetNode center(WidgetNode child) {
        return withSlot(
                prototype(CENTER),
                CHILD,
                WidgetSlot.SingleSlot.of(child));
    }

    private static WidgetNode withSlot(
            WidgetNode parent,
            SlotName slotName,
            WidgetSlot slot) {
        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(parent.slots());
        slots.put(slotName, slot);
        return new WidgetNode(
                parent.id(),
                parent.type(),
                parent.properties(),
                slots);
    }

    private static WidgetNode prototype(WidgetTypeId type) {
        WidgetDefinition definition = CATALOG.find(type).orElseThrow();
        return WidgetNodePrototypeFactory.create(definition, ROOT_ID);
    }

    private static WidgetNode text(StableId id, String data) {
        return new WidgetNode(
                id,
                TEXT,
                Map.of(DATA, new PropertyValue.StringValue(data)),
                Map.of());
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }

    private record Fixture(
            FlutterDesignerPaletteDragRegistry registry,
            FlutterDesignerPaletteDragLifecycle lifecycle,
            FlutterDesignerPaletteTreeDropAdapter adapter,
            String token) {
    }

    private record AcceptedCase(
            String name,
            DesignerDocument document,
            SlotName slotName) {
    }

    private record RejectedCase(
            String name,
            DesignerDocument document,
            FlutterDesignerPaletteTreeDropAdapter.RejectionCode code) {
    }
}
