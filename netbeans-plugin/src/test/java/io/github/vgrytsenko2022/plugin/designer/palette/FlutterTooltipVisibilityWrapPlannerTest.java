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

class FlutterTooltipVisibilityWrapPlannerTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE;
    private static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");
    private static final StableId ROOT = StableId.random(), TEXT = StableId.random(), WRAPPER = StableId.random();
    private final FlutterDesignerPaletteDropPlanner planner = new FlutterDesignerPaletteDropPlanner();

    @Test void emptyListAndSingleSlotNeverAllocateAnInvalidRequiredChildPrototype() {
        var allocations = new AtomicInteger();
        for (var root : List.of(new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of()),
                new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Center"), Map.of(), Map.of()))) {
            var result = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                    planner.plan(document(root), CATALOG, TYPE, ROOT,
                            root.type().value().endsWith("Column") ? CHILDREN : CHILD, 0, () -> { allocations.incrementAndGet(); return WRAPPER; }));
            assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.WRAP_TARGET_REQUIRED, result.code());
        }
        assertEquals(0, allocations.get());
        var prototype = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), WRAPPER);
        assertEquals(Map.of(new PropertyName("visible"), new PropertyValue.BooleanValue(true)), prototype.properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
    }

    @Test void paletteWrapsRootOrChildAtomicallyAndExactUndoRedoPreservesIdentityAndText() throws Exception {
        var original = document(new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(text())))));
        for (var selected : List.of(ROOT, TEXT)) {
            var plan = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planner.planWrapTarget(original, CATALOG, TYPE, selected, () -> WRAPPER));
            var command = plan.command(); assertEquals(selected, command.widgetId()); assertEquals(CHILD, command.wrapperSlot());
            var before = open(original); var applied = before.apply(command);
            assertEquals(DesignerCommandStatus.APPLIED, applied.status(), applied.diagnostics().toString());
            var after = applied.session();
            var wrapper = selected.equals(ROOT) ? after.current().document().root()
                    : ((WidgetSlot.ListSlot) after.current().document().root().slots().get(CHILDREN)).children().getFirst();
            assertEquals(WRAPPER, wrapper.id()); assertEquals(TYPE, wrapper.type());
            assertEquals(selected.equals(ROOT) ? original.root() : text(), ((WidgetSlot.SingleSlot) wrapper.slots().get(CHILD)).child().orElseThrow());
            assertTrue(new String(after.current().dartCandidateBytes(), StandardCharsets.UTF_8).contains("TooltipVisibility("));
            var undone = after.undo().session(); assertArrayEquals(before.current().fdBytes(), undone.current().fdBytes());
            assertArrayEquals(before.current().dartCandidateBytes(), undone.current().dartCandidateBytes());
            var redone = undone.redo().session(); assertArrayEquals(after.current().fdBytes(), redone.current().fdBytes());
            assertArrayEquals(after.current().dartCandidateBytes(), redone.current().dartCandidateBytes());
        }
    }

    @Test void staleTargetsDuplicateIdentitiesAndParentDataCannotBeWrapped() {
        var root = document(text());
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.planWrapTarget(root, CATALOG, TYPE, StableId.random(), () -> WRAPPER));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.planWrapTarget(root, CATALOG, TYPE, TEXT, () -> TEXT));
        var expanded = new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Expanded"), Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(new WidgetNode(StableId.random(), text().type(), text().properties(), Map.of()))));
        var parent = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(expanded))));
        assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.planWrapTarget(document(parent), CATALOG, TYPE, TEXT, () -> WRAPPER));
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
