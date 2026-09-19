package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlutterMenuItemButtonPaletteDropPlannerTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE;
    private static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");
    private static final StableId ROOT = StableId.random(), TEXT = StableId.random(), WRAPPER = StableId.random();
    private final FlutterDesignerPaletteDropPlanner planner = new FlutterDesignerPaletteDropPlanner();

    @Test void ordinaryPaletteAddCreatesAllThreeOptionalSlotsWithoutAWrapperOrFabricatedChildren() throws Exception {
        var original = document(new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of()));
        var accepted = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, planner.plan(original, CATALOG, TYPE, ROOT, CHILDREN, 0, () -> WRAPPER));
        var prototype = accepted.command().widget(); assertEquals(TYPE, prototype.type()); assertEquals(WRAPPER, prototype.id());
        assertEquals(Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true)), prototype.properties());
        assertEquals(Set.of(CHILD, new SlotName("leadingIcon"), new SlotName("trailingIcon")), prototype.slots().keySet());
        assertTrue(prototype.slots().values().stream().allMatch(slot -> slot instanceof WidgetSlot.SingleSlot single && single.child().isEmpty()));
        var before = open(original); var applied = before.apply(accepted.command()); assertEquals(DesignerCommandStatus.APPLIED, applied.status(), applied.diagnostics().toString());
        var after = applied.session(); assertTrue(new String(after.current().dartCandidateBytes(), StandardCharsets.UTF_8).contains("MenuItemButton("));
        var undo = after.undo().session(); assertArrayEquals(before.current().fdBytes(), undo.current().fdBytes()); assertArrayEquals(before.current().dartCandidateBytes(), undo.current().dartCandidateBytes());
        var redo = undo.redo().session(); assertArrayEquals(after.current().fdBytes(), redo.current().fdBytes()); assertArrayEquals(after.current().dartCandidateBytes(), redo.current().dartCandidateBytes());
    }

    @Test void allThreeOptionalDestinationsAcceptIndependentChildrenAndOccupiedSlotsRejectSilentReplacement() {
        for (String name : List.of("child", "leadingIcon", "trailingIcon")) {
            var slot = new SlotName(name); var menu = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), ROOT);
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, planner.plan(document(menu), CATALOG, text().type(), ROOT, slot, 0, () -> TEXT));
            var slots = new LinkedHashMap<>(menu.slots()); slots.put(slot, WidgetSlot.SingleSlot.of(text()));
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.plan(document(new WidgetNode(ROOT, TYPE, menu.properties(), slots)), CATALOG, text().type(), ROOT, slot, 0, () -> WRAPPER));
        }
    }

    @Test void staleTargetsDuplicateIdsAndIncompatibleAppBarSlotsRejectWithoutAllocating() {
        var original = document(new WidgetNode(ROOT, new WidgetTypeId("flutter.material.Scaffold"), Map.of(), Map.of())); var ids = new AtomicInteger();
        for (var target : List.of(ROOT, StableId.random())) {
            assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.plan(original, CATALOG, TYPE, target, new SlotName("appBar"), 0, () -> { ids.incrementAndGet(); return WRAPPER; }));
        }
        assertEquals(0, ids.get());
        var column = document(new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of()));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.plan(column, CATALOG, TYPE, ROOT, CHILDREN, 0, () -> ROOT));
    }

    private static WidgetNode text() { return new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue("Retain my anchor")), Map.of()); }
    private static DartSourceDescriptor descriptor(String imports, String build) { return new DartSourceDescriptor("form.dart", "Form", WidgetClassKind.STATELESS,
            Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
    private static DesignerDocument document(WidgetNode root) { return new DesignerDocument(StableId.random(), descriptor("0".repeat(64), "0".repeat(64)), root); }
    private static DesignerCommandSession open(DesignerDocument draft) throws Exception {
        var generated = new DartRegionGenerator().generate(draft, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(draft.documentId(), descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), draft.root());
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\nclass Form extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document), dart, CATALOG);
        assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
}




