package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
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
    private static final WidgetTypeId TEXT_FIELD =
            type("flutter.material.TextField");
    private static final WidgetTypeId COLUMN =
            type("flutter.widgets.Column");
    private static final WidgetTypeId ROW =
            type("flutter.widgets.Row");
    private static final WidgetTypeId CENTER =
            type("flutter.widgets.Center");
    private static final WidgetTypeId SIZED_BOX =
            type("flutter.widgets.SizedBox");
    private static final WidgetTypeId ASPECT_RATIO =
            type("flutter.widgets.AspectRatio");
    private static final WidgetTypeId OPACITY = type("flutter.widgets.Opacity");
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
    private static final WidgetTypeId IMAGE = type("flutter.widgets.Image");
    private static final WidgetTypeId TEXT = type("flutter.widgets.Text");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName BODY = new SlotName("body");
    private static final PropertyName DATA = new PropertyName("data");
    private static final PropertyName ASPECT_RATIO_VALUE =
            new PropertyName("aspectRatio");
    private static final PropertyName OPACITY_VALUE = new PropertyName("opacity");
    private static final PropertyName CONSTRAINTS = new PropertyName("constraints");
    private static final PropertyName BASELINE_VALUE = new PropertyName("baseline");
    private static final PropertyName BASELINE_TYPE_VALUE =
            new PropertyName("baselineType");
    private static final StableId DOCUMENT_ID =
            id("f56a6bbb-fe08-4977-9597-a8273aa143eb");
    private static final StableId ROOT_ID =
            id("5a975d36-09fd-4012-8c33-083cb9de4bf5");
    private static final StableId FIRST_ID =
            id("939d0528-5cea-4550-a620-ff943214a7a2");
    private static final StableId NEW_ID =
            id("cce2050f-8846-4378-843e-58371d52d1c5");

    @Test
    void imagePreviewAndCommitFailClosedWhenDeclaredInventoryIsUnavailable() {
        Fixture previewFixture = fixture(IMAGE);
        StringSelection previewTransfer = new StringSelection(previewFixture.token());
        FlutterImageAssetChoices unavailable = new FlutterImageAssetChoices(
                List.of(), Optional.of("the current pubspec declares no safe image asset."));

        var previewRejected = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                previewFixture.adapter().preview(
                        previewTransfer,
                        DnDConstants.ACTION_MOVE,
                        document(column(List.of())),
                        CATALOG,
                        ROOT_ID,
                        unavailable));
        assertEquals(
                FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                        .REQUIRED_CREATION_VALUE_UNAVAILABLE,
                previewRejected.code());
        assertTrue(previewRejected.reason().contains("required property 'image'"));

        Fixture commitFixture = fixture(IMAGE);
        StringSelection commitTransfer = new StringSelection(commitFixture.token());
        FlutterImageAssetChoices available = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/a.png", "App: assets/a.png")),
                Optional.empty());
        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                commitFixture.adapter().preview(
                        commitTransfer,
                        DnDConstants.ACTION_MOVE,
                        document(column(List.of())),
                        CATALOG,
                        ROOT_ID,
                        available));
        AtomicInteger allocations = new AtomicInteger();
        var commitRejected = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                commitFixture.adapter().commit(
                        prepared,
                        commitTransfer,
                        DnDConstants.ACTION_MOVE,
                        document(column(List.of())),
                        CATALOG,
                        unavailable,
                        () -> {
                            allocations.incrementAndGet();
                            return NEW_ID;
                        }));
        assertEquals(
                FlutterDesignerPaletteTreeDropAdapter.RejectionCode.PLANNER_REJECTED,
                commitRejected.code());
        assertTrue(commitRejected.reason().contains("required property 'image'"));
        assertEquals(0, allocations.get());
    }

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
    void textFieldTokenPreviewsAndCommitsEmptyLeafWithoutCreationDialogState() {
        Fixture fixture = fixture(TEXT_FIELD);
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
                () -> assertEquals(TEXT_FIELD, prepared.widgetType()),
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
                () -> assertEquals(TEXT_FIELD, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(Map.of(), command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the TextField palette authority once"));
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
    void fractionallySizedBoxTokenPreviewsAndCommitsOptionalPropertiesAndEmptyChild() {
        Fixture fixture = fixture(FRACTIONALLY_SIZED_BOX);
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
                () -> assertEquals(FRACTIONALLY_SIZED_BOX, prepared.widgetType()),
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
                () -> assertEquals(FRACTIONALLY_SIZED_BOX, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the FractionallySizedBox palette authority once"));
    }

    @Test
    void fittedBoxTokenPreviewsAndCommitsOptionalPropertiesAndEmptyChild() {
        Fixture fixture = fixture(FITTED_BOX);
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
                () -> assertEquals(FITTED_BOX, prepared.widgetType()),
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
                () -> assertEquals(FITTED_BOX, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the FittedBox palette authority once"));
    }

    @Test
    void constrainedBoxTokenPreviewsAndCommitsRequiredDefaultAndEmptyChild() {
        Fixture fixture = fixture(CONSTRAINED_BOX);
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
                () -> assertEquals(CONSTRAINED_BOX, prepared.widgetType()),
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
                () -> assertEquals(CONSTRAINED_BOX, command.widget().type()),
                () -> assertEquals(
                        Map.of(CONSTRAINTS, new PropertyValue.BoxConstraintsValue(
                                BigDecimal.ZERO,
                                Optional.empty(),
                                BigDecimal.ZERO,
                                Optional.empty())),
                        command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the ConstrainedBox palette authority once"));
    }

    @Test
    void unconstrainedBoxTokenPreviewsAndCommitsOptionalDefaultsAndEmptyChild() {
        Fixture fixture = fixture(UNCONSTRAINED_BOX);
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
                () -> assertEquals(UNCONSTRAINED_BOX, prepared.widgetType()),
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
                () -> assertEquals(UNCONSTRAINED_BOX, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the UnconstrainedBox palette authority once"));
    }

    @Test
    void limitedBoxTokenPreviewsAndCommitsOptionalDefaultsAndEmptyChild() {
        Fixture fixture = fixture(LIMITED_BOX);
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
                () -> assertEquals(LIMITED_BOX, prepared.widgetType()),
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
                () -> assertEquals(LIMITED_BOX, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the LimitedBox palette authority once"));
    }

    @Test
    void overflowBoxTokenPreviewsAndCommitsOptionalDefaultsAndEmptyChild() {
        Fixture fixture = fixture(OVERFLOW_BOX);
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
                () -> assertEquals(OVERFLOW_BOX, prepared.widgetType()),
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
                () -> assertEquals(OVERFLOW_BOX, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the OverflowBox palette authority once"));
    }

    @Test
    void baselineTokenPreviewsAndCommitsRequiredDefaultsAndEmptyChild() {
        Fixture fixture = fixture(BASELINE);
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
                () -> assertEquals(BASELINE, prepared.widgetType()),
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
                () -> assertEquals(BASELINE, command.widget().type()),
                () -> assertEquals(
                        Map.of(
                                BASELINE_VALUE,
                                new PropertyValue.DoubleValue(new BigDecimal("24")),
                                BASELINE_TYPE_VALUE,
                                new PropertyValue.EnumValue(
                                        "TextBaseline", "alphabetic")),
                        command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the Baseline palette authority once"));
    }

    @Test
    void intrinsicHeightTokenPreviewsAndCommitsEmptyPropertiesAndChild() {
        Fixture fixture = fixture(INTRINSIC_HEIGHT);
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
                () -> assertEquals(INTRINSIC_HEIGHT, prepared.widgetType()),
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
                () -> assertEquals(INTRINSIC_HEIGHT, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the IntrinsicHeight palette authority once"));
    }

    @Test
    void intrinsicWidthTokenPreviewsAndCommitsOptionalStepsAndEmptyChild() {
        Fixture fixture = fixture(INTRINSIC_WIDTH);
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
                () -> assertEquals(INTRINSIC_WIDTH, prepared.widgetType()),
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
                () -> assertEquals(INTRINSIC_WIDTH, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the IntrinsicWidth palette authority once"));
    }

    @Test
    void stackTokenPreviewsAndCommitsOptionalPropertiesAndEmptyOrderedChildren() {
        Fixture fixture = fixture(STACK);
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
                () -> assertEquals(STACK, prepared.widgetType()),
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
                () -> assertEquals(STACK, command.widget().type()),
                () -> assertEquals(Map.of(), command.widget().properties()),
                () -> assertEquals(
                        Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())),
                        command.widget().slots()),
                () -> assertTrue(fixture.lifecycle()
                        .resolve(transferable).isEmpty(),
                        "commit consumes the Stack palette authority once"));
    }

    @Test
    void textTokenAppendsAFullNodeAtTheFrontOfStackPaintOrder() {
        Fixture fixture = fixture(TEXT);
        StringSelection transferable = new StringSelection(fixture.token());
        DesignerDocument document = document(stack(List.of(
                text(FIRST_ID, "existing back layer"))));

        var prepared = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                fixture.adapter().preview(
                        transferable,
                        DnDConstants.ACTION_MOVE,
                        document,
                        CATALOG,
                        ROOT_ID));
        assertAll(
                () -> assertEquals(ROOT_ID, prepared.parentId()),
                () -> assertEquals(CHILDREN, prepared.slotName()),
                () -> assertEquals(1, prepared.insertionIndex()));

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
                () -> assertEquals(ROOT_ID, command.destination().parentId()),
                () -> assertEquals(CHILDREN, command.destination().slotName()),
                () -> assertEquals(1, command.destination().index(),
                        "terminal insertion is Stack's front paint layer"),
                () -> assertEquals(NEW_ID, command.widget().id()),
                () -> assertEquals(TEXT, command.widget().type()),
                () -> assertEquals(
                        Map.of(DATA, new PropertyValue.StringValue("Text")),
                        command.widget().properties()),
                () -> assertEquals(Map.of(), command.widget().slots()));
    }

    @Test
    void resolvesOnlyOneCurrentlyAvailableCompatibleCatalogSlot() {
        List<AcceptedCase> accepted = List.of(
                new AcceptedCase(
                        "empty Column",
                        document(column(List.of())),
                        CHILDREN),
                new AcceptedCase(
                        "empty Stack",
                        document(stack(List.of())),
                        CHILDREN),
                new AcceptedCase(
                        "empty Center",
                        document(prototype(CENTER)),
                        CHILD),
                new AcceptedCase(
                        "empty ConstrainedBox",
                        document(prototype(CONSTRAINED_BOX)),
                        CHILD),
                new AcceptedCase(
                        "empty UnconstrainedBox",
                        document(prototype(UNCONSTRAINED_BOX)),
                        CHILD),
                new AcceptedCase(
                        "empty LimitedBox",
                        document(prototype(LIMITED_BOX)),
                        CHILD),
                new AcceptedCase(
                        "empty OverflowBox",
                        document(prototype(OVERFLOW_BOX)),
                        CHILD),
                new AcceptedCase(
                        "empty Baseline",
                        document(prototype(BASELINE)),
                        CHILD),
                new AcceptedCase(
                        "empty IntrinsicHeight",
                        document(prototype(INTRINSIC_HEIGHT)),
                        CHILD),
                new AcceptedCase(
                        "empty IntrinsicWidth",
                        document(prototype(INTRINSIC_WIDTH)),
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
    void expandedTreeDropWrapsTheExactDirectFlexChildWithOneNewId() {
        for (WidgetTypeId flexType : List.of(COLUMN, ROW)) {
            Fixture fixture = fixture(EXPANDED);
            StringSelection transferable = new StringSelection(fixture.token());
            WidgetNode target = text(FIRST_ID, "target");
            DesignerDocument document = document(flexParent(flexType, List.of(target)));
            AtomicInteger allocations = new AtomicInteger();

            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                            fixture.adapter().preview(
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    document,
                                    CATALOG,
                                    FIRST_ID),
                            flexType.value());
            assertAll(
                    () -> assertEquals(EXPANDED, prepared.widgetType()),
                    () -> assertEquals(ROOT_ID, prepared.parentId()),
                    () -> assertEquals(CHILDREN, prepared.slotName()),
                    () -> assertEquals(0, prepared.insertionIndex()),
                    () -> assertEquals(FIRST_ID, prepared.treeTargetId()),
                    () -> assertEquals(Optional.of(FIRST_ID),
                            prepared.wrapTargetId()),
                    () -> assertTrue(fixture.lifecycle()
                            .resolve(transferable).isPresent()));

            FlutterDesignerPaletteTreeDropAdapter.Wrapped committed =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.Wrapped.class,
                            fixture.adapter().commit(
                                    prepared,
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    document,
                                    CATALOG,
                                    () -> {
                                        allocations.incrementAndGet();
                                        return NEW_ID;
                                    }));
            WrapWidget command = committed.command();
            assertAll(
                    () -> assertEquals(FIRST_ID, command.widgetId()),
                    () -> assertEquals(NEW_ID, command.wrapper().id()),
                    () -> assertEquals(EXPANDED, command.wrapper().type()),
                    () -> assertTrue(command.wrapper().properties().isEmpty(),
                            "omitted flex preserves Flutter's constructor default of 1"),
                    () -> assertEquals(CHILD, command.wrapperSlot()),
                    () -> assertEquals(0, command.wrapperIndex()),
                    () -> assertEquals(1, allocations.get()),
                    () -> assertTrue(fixture.lifecycle()
                            .resolve(transferable).isEmpty()));
        }
    }

    @Test
    void flexibleTreeDropWrapsTheExactDirectRowOrColumnChildWithOneNewId() {
        for (WidgetTypeId flexType : List.of(COLUMN, ROW)) {
            Fixture fixture = fixture(FLEXIBLE);
            StringSelection transferable = new StringSelection(fixture.token());
            WidgetNode target = text(FIRST_ID, "target");
            DesignerDocument document = document(flexParent(flexType, List.of(target)));
            AtomicInteger allocations = new AtomicInteger();

            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                            fixture.adapter().preview(
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    document,
                                    CATALOG,
                                    FIRST_ID),
                            flexType.value());
            assertAll(
                    () -> assertEquals(FLEXIBLE, prepared.widgetType()),
                    () -> assertEquals(ROOT_ID, prepared.parentId()),
                    () -> assertEquals(CHILDREN, prepared.slotName()),
                    () -> assertEquals(0, prepared.insertionIndex()),
                    () -> assertEquals(FIRST_ID, prepared.treeTargetId()),
                    () -> assertEquals(Optional.of(FIRST_ID),
                            prepared.wrapTargetId()));

            FlutterDesignerPaletteTreeDropAdapter.Wrapped committed =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.Wrapped.class,
                            fixture.adapter().commit(
                                    prepared,
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    document,
                                    CATALOG,
                                    () -> {
                                        allocations.incrementAndGet();
                                        return NEW_ID;
                                    }));
            WrapWidget command = committed.command();
            assertAll(
                    () -> assertEquals(FIRST_ID, command.widgetId()),
                    () -> assertEquals(NEW_ID, command.wrapper().id()),
                    () -> assertEquals(FLEXIBLE, command.wrapper().type()),
                    () -> assertTrue(command.wrapper().properties().isEmpty(),
                            "omitted flex and fit preserve Flutter defaults 1 and loose"),
                    () -> assertEquals(CHILD, command.wrapperSlot()),
                    () -> assertEquals(0, command.wrapperIndex()),
                    () -> assertEquals(1, allocations.get()),
                    () -> assertTrue(fixture.lifecycle()
                            .resolve(transferable).isEmpty()));
        }
    }

    @Test
    void spacerTreeDropAppendsATerminalPrototypeOnlyToRowOrColumn() {
        for (WidgetTypeId flexType : List.of(COLUMN, ROW)) {
            Fixture fixture = fixture(SPACER);
            StringSelection transferable = new StringSelection(fixture.token());
            DesignerDocument empty = document(flexParent(flexType, List.of()));
            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                            fixture.adapter().preview(
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    empty,
                                    CATALOG,
                                    ROOT_ID),
                            flexType.value());
            assertAll(
                    () -> assertEquals(SPACER, prepared.widgetType()),
                    () -> assertEquals(ROOT_ID, prepared.parentId()),
                    () -> assertEquals(CHILDREN, prepared.slotName()),
                    () -> assertEquals(0, prepared.insertionIndex()),
                    () -> assertEquals(Optional.empty(), prepared.wrapTargetId()));

            DesignerDocument populated = document(flexParent(
                    flexType, List.of(text(FIRST_ID, "existing"))));
            FlutterDesignerPaletteTreeDropAdapter.Committed committed =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.Committed.class,
                            fixture.adapter().commit(
                                    prepared,
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    populated,
                                    CATALOG,
                                    () -> NEW_ID));
            AddWidget command = committed.command();
            assertAll(
                    () -> assertEquals(ROOT_ID, command.destination().parentId()),
                    () -> assertEquals(CHILDREN, command.destination().slotName()),
                    () -> assertEquals(1, command.destination().index()),
                    () -> assertEquals(SPACER, command.widget().type()),
                    () -> assertTrue(command.widget().properties().isEmpty()),
                    () -> assertTrue(command.widget().slots().isEmpty()));
        }

        Fixture rejectedFixture = fixture(SPACER);
        StringSelection rejectedTransfer = new StringSelection(rejectedFixture.token());
        FlutterDesignerPaletteTreeDropAdapter.Rejected rejected = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                rejectedFixture.adapter().preview(
                        rejectedTransfer,
                        DnDConstants.ACTION_MOVE,
                        document(stack(List.of())),
                        CATALOG,
                        ROOT_ID));
        assertEquals(
                FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                        .NO_COMPATIBLE_DESTINATION,
                rejected.code());
        assertTrue(rejectedFixture.lifecycle().resolve(rejectedTransfer).isPresent(),
                "preview rejection must not consume Spacer source authority");
    }

    @Test
    void treeDropRejectsWrappingAnyFlexParentDataWidgetBeforeIdAllocation() {
        record NestedCase(String name, WidgetTypeId outer, WidgetNode inner) {
        }
        WidgetNode innerChild = text(
                id("57984d01-8905-4664-b286-82c11af6197c"), "inner child");
        List<NestedCase> cases = List.of(
                new NestedCase("Expanded over Flexible", EXPANDED,
                        flexible(FIRST_ID, innerChild)),
                new NestedCase("Flexible over Expanded", FLEXIBLE,
                        expanded(FIRST_ID, innerChild)),
                new NestedCase("Expanded over Spacer", EXPANDED,
                        WidgetNodePrototypeFactory.create(
                                CATALOG.find(SPACER).orElseThrow(), FIRST_ID)),
                new NestedCase("Flexible over Spacer", FLEXIBLE,
                        WidgetNodePrototypeFactory.create(
                                CATALOG.find(SPACER).orElseThrow(), FIRST_ID)));

        assertAll(cases.stream().map(testCase -> () -> {
            Fixture fixture = fixture(testCase.outer());
            StringSelection transferable = new StringSelection(fixture.token());
            FlutterDesignerPaletteTreeDropAdapter.Rejected failure =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                            fixture.adapter().preview(
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    document(flexParent(ROW, List.of(testCase.inner()))),
                                    CATALOG,
                                    FIRST_ID),
                            testCase.name());
            assertEquals(
                    FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    failure.code(),
                    testCase.name());
            assertTrue(failure.reason().contains("nesting would move the inner"),
                    failure.reason());
        }));
    }

    @Test
    void expandedTreeDropBurnsStaleTargetAndRejectsInvalidParentsConcretely() {
        StableId secondId = id("305af782-f541-44c5-b9a7-78b133a7e0e2");
        WidgetNode first = text(FIRST_ID, "first");
        WidgetNode second = text(secondId, "second");
        Fixture staleFixture = fixture(EXPANDED);
        StringSelection staleTransfer = new StringSelection(staleFixture.token());
        DesignerDocument original = document(column(List.of(first, second)));
        FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared =
                assertInstanceOf(
                        FlutterDesignerPaletteTreeDropAdapter.PreparedDrop.class,
                        staleFixture.adapter().preview(
                                staleTransfer,
                                DnDConstants.ACTION_MOVE,
                                original,
                                CATALOG,
                                FIRST_ID));
        AtomicInteger allocations = new AtomicInteger();
        FlutterDesignerPaletteTreeDropAdapter.Rejected stale = assertInstanceOf(
                FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                staleFixture.adapter().commit(
                        prepared,
                        staleTransfer,
                        DnDConstants.ACTION_MOVE,
                        document(column(List.of(second, first))),
                        CATALOG,
                        () -> {
                            allocations.incrementAndGet();
                            return NEW_ID;
                        }));
        assertAll(
                () -> assertEquals(
                        FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                                .TARGET_CHANGED,
                        stale.code()),
                () -> assertTrue(stale.reason().contains(
                        "changed parent, slot or child index")),
                () -> assertEquals(0, allocations.get()),
                () -> assertTrue(staleFixture.lifecycle()
                        .resolve(staleTransfer).isEmpty()));

        List<InvalidExpandedTarget> invalid = List.of(
                new InvalidExpandedTarget(
                        "root Row",
                        document(flexParent(ROW, List.of())),
                        ROOT_ID,
                        "direct child of Row.children or Column.children"),
                new InvalidExpandedTarget(
                        "Stack child",
                        document(stack(List.of(first))),
                        FIRST_ID,
                        "flutter.widgets.Stack.children"),
                new InvalidExpandedTarget(
                        "Center child",
                        document(center(first)),
                        FIRST_ID,
                        "flutter.widgets.Center.child"),
                new InvalidExpandedTarget(
                        "nested Expanded",
                        document(flexParent(ROW, List.of(expanded(
                                FIRST_ID,
                                text(id("57984d01-8905-4664-b286-82c11af6197c"),
                                        "inner child"))))),
                        FIRST_ID,
                        "Cannot wrap Expanded '" + FIRST_ID + "' with Expanded"));
        assertAll(invalid.stream().map(testCase -> () -> {
            Fixture fixture = fixture(EXPANDED);
            StringSelection transferable = new StringSelection(fixture.token());
            FlutterDesignerPaletteTreeDropAdapter.Rejected failure =
                    assertInstanceOf(
                            FlutterDesignerPaletteTreeDropAdapter.Rejected.class,
                            fixture.adapter().preview(
                                    transferable,
                                    DnDConstants.ACTION_MOVE,
                                    testCase.document(),
                                    CATALOG,
                                    testCase.targetId()),
                            testCase.name());
            assertEquals(
                    FlutterDesignerPaletteTreeDropAdapter.RejectionCode
                            .NO_COMPATIBLE_DESTINATION,
                    failure.code(),
                    testCase.name());
            assertTrue(failure.reason().contains(testCase.reasonFragment()),
                    failure.reason());
            assertTrue(fixture.lifecycle().resolve(transferable).isPresent(),
                    "preview rejection must not consume authority");
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

    private static WidgetNode stack(List<WidgetNode> children) {
        return withSlot(
                prototype(STACK),
                CHILDREN,
                new WidgetSlot.ListSlot(children));
    }

    private static WidgetNode flexParent(
            WidgetTypeId type,
            List<WidgetNode> children) {
        return withSlot(
                prototype(type),
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

    private static WidgetNode expanded(StableId id, WidgetNode child) {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(EXPANDED).orElseThrow(), id);
        return new WidgetNode(
                prototype.id(),
                prototype.type(),
                prototype.properties(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }

    private static WidgetNode flexible(StableId id, WidgetNode child) {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(FLEXIBLE).orElseThrow(), id);
        return new WidgetNode(
                prototype.id(),
                prototype.type(),
                prototype.properties(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
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

    private record InvalidExpandedTarget(
            String name,
            DesignerDocument document,
            StableId targetId,
            String reasonFragment) {
    }
}
