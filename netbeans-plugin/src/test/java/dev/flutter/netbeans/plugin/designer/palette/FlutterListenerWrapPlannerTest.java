package dev.flutter.netbeans.plugin.designer.palette;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerCommandStatus;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FlutterListenerWrapPlannerTest {
    private static final dev.flutter.netbeans.designer.catalog.WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId LISTENER = ListenerWidgetPropertySchema.LISTENER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final StableId ROOT = StableId.random(), TEXT = StableId.random(), WRAPPER = StableId.random();

    @Test
    void explicitActionWrapsRootOrSelectedChildAsOneUndoableCommandWithoutChangingItsIdentityOrContents() throws Exception {
        var original = document(column(text()));
        for (StableId selected : List.of(ROOT, TEXT)) {
            var plan = FlutterListenerWrapPlanner.plan(original, CATALOG, selected, () -> WRAPPER);
            assertEquals("", plan.reason());
            var command = plan.command().orElseThrow();
            assertEquals(selected, command.widgetId()); assertEquals(WRAPPER, command.wrapper().id());
            assertEquals(LISTENER, command.wrapper().type()); assertTrue(command.wrapper().properties().isEmpty());
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
            assertTrue(new String(changed.current().dartCandidateBytes(), StandardCharsets.UTF_8).contains("Listener("));
            assertArrayEquals(session.current().fdBytes(), changed.undo().session().current().fdBytes());
            assertArrayEquals(session.current().dartCandidateBytes(), changed.undo().session().current().dartCandidateBytes());
            assertArrayEquals(changed.current().dartCandidateBytes(), changed.undo().session().redo().session().current().dartCandidateBytes());
        }
    }

    @Test
    void explicitWrappingRejectsParentDataSlotsMissingTargetsAndConflictingIdentities() {
        WidgetNode expanded = new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Expanded"), Map.of(),
                Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(new WidgetNode(StableId.random(), text().type(), text().properties(), Map.of())))));
        assertTrue(FlutterListenerWrapPlanner.plan(document(column(expanded)), CATALOG, TEXT, () -> WRAPPER).command().isEmpty());
        WidgetNode appbar = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.material.AppBar")).orElseThrow(), TEXT);
        WidgetNode scaffold = new WidgetNode(ROOT, new WidgetTypeId("flutter.material.Scaffold"), Map.of(),
                Map.of(new SlotName("appBar"), new WidgetSlot.SingleSlot(Optional.of(appbar))));
        assertTrue(FlutterListenerWrapPlanner.plan(document(scaffold), CATALOG, TEXT, () -> WRAPPER).command().isEmpty());
        var baseline = document(column(text()));
        assertTrue(FlutterListenerWrapPlanner.plan(baseline, CATALOG, StableId.random(), () -> WRAPPER).reason().contains("no longer present"));
        assertTrue(FlutterListenerWrapPlanner.plan(baseline, CATALOG, TEXT, () -> TEXT).reason().contains("already used"));
        assertTrue(FlutterListenerWrapPlanner.plan(baseline, CATALOG, TEXT, () -> null).command().isEmpty());
    }

    @Test
    void paletteAddKeepsOptionalChildEmptyAndDoesNotSilentlyWrapExistingWidgets() {
        var baseline = document(new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of()));
        var planner = new FlutterDesignerPaletteDropPlanner();
        var result = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(baseline, CATALOG, LISTENER, ROOT, CHILDREN, 0, () -> WRAPPER));
        assertEquals(LISTENER, result.command().widget().type());
        assertTrue(result.command().widget().properties().isEmpty());
        assertTrue(((WidgetSlot.SingleSlot) result.command().widget().slots().get(CHILD)).child().isEmpty());
        var occupied = document(new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Center"), Map.of(),
                Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(text())))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(occupied, CATALOG, LISTENER, ROOT, CHILD, 0, () -> WRAPPER));
        assertTrue(FlutterListenerWrapPlanner.plan(occupied, CATALOG, TEXT, () -> WRAPPER).command().isPresent());
    }

    @Test
    void listenerPaletteSourceIsReviewedAgainstEveryOptionalCatalogDestination() {
        var planner = new FlutterDesignerPaletteDropPlanner(); int checked = 0;
        var gesture = CATALOG.find(LISTENER).orElseThrow();
        for (var parent : CATALOG.definitions()) for (var slot : parent.slots()) {
            if (slot.minChildren() != 0) continue;
            var prototype = destinationPrototype(parent, slot.name());
            var plan = planner.plan(document(prototype), CATALOG, LISTENER, ROOT, slot.name(), 0, () -> WRAPPER);
            var placement = dev.flutter.netbeans.designer.catalog.WidgetPlacementRules.evaluate(parent, slot, gesture);
            assertEquals(placement.accepted(), plan instanceof FlutterDesignerPaletteDropPlanner.Accepted,
                    parent.typeId().value() + "." + slot.name().value() + ": " + plan);
            checked++;
        }
        assertEquals(188, checked);
    }

    private static WidgetNode destinationPrototype(dev.flutter.netbeans.designer.catalog.WidgetDefinition definition, SlotName destination) {
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
