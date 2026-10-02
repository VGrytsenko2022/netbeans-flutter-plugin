package io.github.vgrytsenko2022.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.*;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.NotificationListenerWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSession;
import io.github.vgrytsenko2022.designer.command.DesignerCommandStatus;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FlutterNotificationListenerWrapPlannerTest {
    private static final io.github.vgrytsenko2022.designer.catalog.WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId NOTIFICATION_LISTENER = NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final StableId ROOT = StableId.random(), TEXT = StableId.random(), WRAPPER = StableId.random();

    @Test
    void explicitActionWrapsRootOrSelectedChildAsOneUndoableCommandWithoutChangingItsIdentityOrContents() throws Exception {
        var original = document(column(text()));
        for (StableId selected : List.of(ROOT, TEXT)) {
            var plan = FlutterNotificationListenerWrapPlanner.plan(original, CATALOG, selected, () -> WRAPPER);
            assertEquals("", plan.reason());
            var command = plan.command().orElseThrow();
            assertEquals(selected, command.widgetId()); assertEquals(WRAPPER, command.wrapper().id());
            assertEquals(NOTIFICATION_LISTENER, command.wrapper().type()); assertEquals(Map.of(new PropertyName("notificationType"), new PropertyValue.StringValue("Notification")), command.wrapper().properties());
            assertTrue(((WidgetSlot.SingleSlot) command.wrapper().slots().get(CHILD)).child().isEmpty());
            var session = open(original);
            var result = session.apply(command);
            assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
            var changed = result.session();
            assertEquals(1, changed.cursor() - session.cursor());
            WidgetNode wrapper = selected.equals(ROOT) ? changed.current().document().root()
                    : ((WidgetSlot.ListSlot) changed.current().document().root().slots().get(CHILDREN)).children().getFirst();
            WidgetNode retained = ((WidgetSlot.SingleSlot) wrapper.slots().get(CHILD)).child().orElseThrow();
            assertEquals(selected.equals(ROOT) ? original.root() : text(), retained);
            assertTrue(new String(changed.current().dartCandidateBytes(), StandardCharsets.UTF_8).contains("NotificationListener<Notification>("));
            assertArrayEquals(session.current().fdBytes(), changed.undo().session().current().fdBytes());
            assertArrayEquals(session.current().dartCandidateBytes(), changed.undo().session().current().dartCandidateBytes());
            assertArrayEquals(changed.current().dartCandidateBytes(), changed.undo().session().redo().session().current().dartCandidateBytes());
        }
    }

    @Test
    void explicitWrappingRejectsParentDataSlotsMissingTargetsAndConflictingIdentities() {
        WidgetNode expanded = new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Expanded"), Map.of(),
                Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(new WidgetNode(StableId.random(), text().type(), text().properties(), Map.of())))));
        assertTrue(FlutterNotificationListenerWrapPlanner.plan(document(column(expanded)), CATALOG, TEXT, () -> WRAPPER).command().isEmpty());
        WidgetNode appbar = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.material.AppBar")).orElseThrow(), TEXT);
        WidgetNode scaffold = new WidgetNode(ROOT, new WidgetTypeId("flutter.material.Scaffold"), Map.of(),
                Map.of(new SlotName("appBar"), new WidgetSlot.SingleSlot(Optional.of(appbar))));
        assertTrue(FlutterNotificationListenerWrapPlanner.plan(document(scaffold), CATALOG, TEXT, () -> WRAPPER).command().isEmpty());
        var baseline = document(column(text()));
        assertTrue(FlutterNotificationListenerWrapPlanner.plan(baseline, CATALOG, StableId.random(), () -> WRAPPER).reason().contains("no longer present"));
        assertTrue(FlutterNotificationListenerWrapPlanner.plan(baseline, CATALOG, TEXT, () -> TEXT).reason().contains("already used"));
        assertTrue(FlutterNotificationListenerWrapPlanner.plan(baseline, CATALOG, TEXT, () -> null).command().isEmpty());
    }

    @Test
    void paletteRequiresExistingChildAndWrapsSingleListOrRootTargetsWithoutFabricatingNodes() {
        var planner = new FlutterDesignerPaletteDropPlanner();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        for (String type : List.of("flutter.widgets.Column", "flutter.widgets.Center")) {
            boolean list = type.endsWith("Column"); SlotName slot = list ? CHILDREN : CHILD;
            var empty = new WidgetNode(ROOT, new WidgetTypeId(type), Map.of(), Map.of());
            var rejected = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(empty), CATALOG, NOTIFICATION_LISTENER, ROOT, slot, 0, () -> { calls.incrementAndGet(); return WRAPPER; }));
            assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED, rejected.code());
            assertTrue(rejected.reason().contains("add a widget first")); assertEquals(0, calls.get());
            var occupied = new WidgetNode(ROOT, empty.type(), Map.of(),
                    Map.of(slot, list ? new WidgetSlot.ListSlot(List.of(text())) : WidgetSlot.SingleSlot.of(text())));
            var result = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.plan(document(occupied), CATALOG, NOTIFICATION_LISTENER, ROOT, slot, 0, () -> WRAPPER));
            assertEquals(TEXT, result.command().widgetId());
            assertEquals(Map.of(new PropertyName("notificationType"), new PropertyValue.StringValue("Notification")), result.command().wrapper().properties());
            assertEquals(ROOT, assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.planWrapTarget(document(occupied), CATALOG, NOTIFICATION_LISTENER, ROOT, () -> WRAPPER)).command().widgetId());
        }
    }

    @Test
    void notificationListenerWrapIsReviewedAgainstEveryOptionalCatalogDestinationWithAnExistingChild() {
        var planner = new FlutterDesignerPaletteDropPlanner(); int checked = 0;
        var notificationListener = CATALOG.find(NOTIFICATION_LISTENER).orElseThrow();
        for (var parent : CATALOG.definitions()) for (var slot : parent.slots()) {
            if (slot.minChildren() != 0) continue;
            var prototype = destinationPrototype(parent, slot.name());
            var slots = new java.util.LinkedHashMap<>(prototype.slots());
            var target = new WidgetNode(StableId.random(), text().type(), text().properties(), Map.of());
            slots.put(slot.name(), slot.cardinality() == io.github.vgrytsenko2022.designer.model.SlotCardinality.SINGLE
                    ? WidgetSlot.SingleSlot.of(target) : new WidgetSlot.ListSlot(List.of(target)));
            var occupied = new WidgetNode(prototype.id(), prototype.type(), prototype.properties(), slots,
                    prototype.extensions(), prototype.stateBinding(), prototype.propertyBindings());
            var plan = planner.plan(document(occupied), CATALOG, NOTIFICATION_LISTENER, ROOT, slot.name(), 0, () -> WRAPPER);
            var placement = io.github.vgrytsenko2022.designer.catalog.WidgetPlacementRules.evaluate(parent, slot, notificationListener);
            assertEquals(placement.accepted(), plan instanceof FlutterDesignerPaletteDropPlanner.Wrapped,
                    parent.typeId().value() + "." + slot.name().value() + ": " + plan);
            checked++;
        }
        assertEquals(223, checked);
    }

    private static WidgetNode destinationPrototype(io.github.vgrytsenko2022.designer.catalog.WidgetDefinition definition, SlotName destination) {
        WidgetNode created = WidgetNodePrototypeFactory.create(definition, ROOT);
        String type = definition.typeId().value();
        var properties = new java.util.LinkedHashMap<>(created.properties());
        var slots = new java.util.LinkedHashMap<>(created.slots());
        if (destination.value().equals("icon") && List.of("flutter.material.TextButton", "flutter.material.OutlinedButton",
                "flutter.material.FilledButton", "flutter.material.FloatingActionButton").contains(type)) {
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue(
                    type.equals("flutter.material.FloatingActionButton") ? "extended" : "icon"));
            slots.put(CHILD, WidgetSlot.SingleSlot.of(text()));
        }
        if (type.equals("flutter.material.IconButton") && destination.value().equals("selectedIcon")) {
            slots.put(new SlotName("icon"), WidgetSlot.SingleSlot.of(text()));
        }
        if (type.equals("flutter.widgets.Visibility") && destination.value().equals("replacement")) {
            slots.put(CHILD, WidgetSlot.SingleSlot.of(text()));
        }
        if (type.equals("flutter.material.ExpansionTile")) {
            slots.put(new SlotName("title"), WidgetSlot.SingleSlot.of(text()));
        }
        return new WidgetNode(created.id(), created.type(), properties, slots, created.extensions(), created.stateBinding(), created.propertyBindings());
    }

    private static WidgetNode text() { return new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Text"),
            Map.of(new PropertyName("data"), new PropertyValue.StringValue("Keep this text")), Map.of()); }
    private static WidgetNode column(WidgetNode child) { return new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"),
            Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(child)))); }
    private static DesignerDocument document(WidgetNode root) { return new DesignerDocument(StableId.random(),
            descriptor("0".repeat(64), "0".repeat(64)), root); }
    private static DartSourceDescriptor descriptor(String imports, String build) { return new DartSourceDescriptor("form.dart", "Form", WidgetClassKind.STATELESS,
            Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
    private static DesignerCommandSession open(DesignerDocument draft) throws Exception {
        var generated = new DartRegionGenerator().generate(draft, CATALOG).generated().orElseThrow();
        var doc = new DesignerDocument(draft.documentId(), descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), draft.root());
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\nclass Form extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var opening = DesignerCommandSession.open(new FdDocumentCodec().encode(doc), dart, CATALOG);
        assertTrue(opening.ready(), opening.diagnostics().toString()); return opening.session().orElseThrow();
    }
}
