package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.SliderTestValues.*;

class SliderCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = SliderWidgetPropertySchema.SLIDER_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void creationMoveRemoveAndBothConstructorsPreserveExactHistory() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(3, find(added, BUTTON).properties().size());
        assertExactPair(initial, added.undo().session());
        var adaptive = applied(added, new SetProperty(BUTTON, p("variant"), s("adaptive")));
        assertTrue(source(adaptive).contains("Slider.adaptive("));
        var moved = applied(adaptive, new MoveWidget(BUTTON, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertExactPair(adaptive, moved.undo().session());
        assertExactPair(moved, reopen(moved));
        var removed = applied(moved, new RemoveWidget(BUTTON));
        assertExactPair(moved, removed.undo().session());
        assertEquals(FIRST, find(removed, FIRST).id());
        assertEquals(SECOND, find(removed, SECOND).id());
    }

    @Test
    void denseBothConstructorsSaveReopenAndAtomicResetVisitAll33Fields() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        for (String variant : SliderWidgetPropertySchema.variants()) {
            var initial = add(openDefault());
            var fields = full(variant);
            covered.addAll(fields.keySet());
            var dense = applied(initial, patch(fields));
            assertExactPair(dense, reopen(dense));
            assertExactPair(initial, dense.undo().session());
            assertExactPair(dense, dense.undo().session().redo().session());
            var resets = new ArrayList<PatchProperties.Patch>();
            for (String name : SliderWidgetPropertySchema.definitions().keySet()) {
                if (!Set.of("value", "variant", "enabled").contains(name)) resets.add(new PatchProperties.ResetPatch(p(name)));
            }
            resets.add(new PatchProperties.SetPatch(p("value"), i(0)));
            var reset = applied(dense, new PatchProperties(BUTTON, resets));
            assertEquals(3, find(reset, BUTTON).properties().size());
            assertExactPair(dense, reset.undo().session());
            assertExactPair(reset, reopen(reset));
            var whole = applied(reset, new SetProperty(BUTTON, p("overlayColor"), reference("wholeOverlay")));
            covered.add(p("overlayColor"));
            assertExactPair(whole, reopen(whole));
        }
        assertEquals(33, covered.size());
    }

    @Test
    void prospectiveRangeAndConstructorConflictsRejectWithoutClampOrHistoryLoss() throws Exception {
        var initial = add(openDefault());
        assertRejectedUnchanged(initial, new SetProperty(BUTTON, p("min"), d("20")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(initial, new SetProperty(BUTTON, p("value"), d("20")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var range = applied(initial, patch(Map.of(p("min"), d("20"), p("max"), d("30"), p("value"), d("25"), p("secondaryTrackValue"), d("24"))));
        assertRejectedUnchanged(range, new ResetProperty(BUTTON, p("max")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(range, new SetProperty(BUTTON, p("secondaryTrackValue"), d("31")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(range, reopen(range));
        var padded = applied(initial, new SetProperty(BUTTON, p("padding"), value("padding")));
        assertRejectedUnchanged(padded, new SetProperty(BUTTON, p("variant"), s("adaptive")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var adaptive = applied(padded, new PatchProperties(BUTTON, List.of(
                new PatchProperties.ResetPatch(p("padding")), new PatchProperties.SetPatch(p("variant"), s("adaptive")))));
        assertExactPair(padded, adaptive.undo().session());
        assertExactPair(adaptive, reopen(adaptive));
        for (String name : List.of("value", "enabled", "variant")) {
            assertRejectedUnchanged(initial, new ResetProperty(BUTTON, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        }
    }

    @Test
    void signedInfinityNullAndOverlaySwitchesAreAtomicAndUndoable() throws Exception {
        var initial = add(openDefault());
        for (boolean negative : List.of(false, true)) {
            var infinity = infinity(negative);
            var range = applied(initial, patch(Map.of(p("min"), infinity, p("max"), infinity, p("value"), infinity,
                    p("secondaryTrackValue"), infinity, p("divisions"), nil(), p("year2023"), nil())));
            assertExactPair(initial, range.undo().session());
            assertExactPair(range, reopen(range));
        }
        var local = applied(initial, patch(Map.of(p("overlayColorDefault"), color(), p("overlayColorDragged"), nil())));
        assertRejectedUnchanged(local, new SetProperty(BUTTON, p("overlayColor"), reference("whole")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var patches = new ArrayList<PatchProperties.Patch>();
        for (String name : SliderWidgetPropertySchema.overlayColorStateProperties()) patches.add(new PatchProperties.ResetPatch(p(name)));
        patches.add(new PatchProperties.SetPatch(p("overlayColor"), reference("whole")));
        var whole = applied(local, new PatchProperties(BUTTON, patches));
        assertExactPair(local, whole.undo().session());
        assertExactPair(whole, reopen(whole));
    }

    @Test
    void disabledChangedMetadataKeepsSameDartWhileStartAndEndRemainSourceVisible() throws Exception {
        var disabled = applied(add(openDefault()), new SetProperty(BUTTON, p("enabled"), b(false)));
        var first = applied(disabled, new SetProperty(BUTTON, p("onChanged"), reference("first")));
        var second = applied(first, new SetProperty(BUTTON, p("onChanged"), reference("second")));
        assertArrayEquals(first.current().dartCandidateBytes(), second.current().dartCandidateBytes());
        assertFalse(Arrays.equals(first.current().fdBytes(), second.current().fdBytes()));
        assertExactPair(first, second.undo().session());
        assertExactPair(second, second.undo().session().redo().session());
        var start = applied(second, new SetProperty(BUTTON, p("onChangeStart"), reference("start")));
        assertFalse(Arrays.equals(second.current().dartCandidateBytes(), start.current().dartCandidateBytes()));
        assertTrue(source(start).contains("buttonValues.start"));
        var end = applied(start, new SetProperty(BUTTON, p("onChangeEnd"), reference("end")));
        assertTrue(source(end).contains("buttonValues.end"));
        var enabled = applied(reopen(end), new SetProperty(BUTTON, p("enabled"), b(true)));
        assertTrue(source(enabled).contains("buttonValues.second"));
        assertFalse(source(enabled).contains("buttonValues.first"));
        assertExactPair(enabled, reopen(enabled));
    }

    @Test
    void invalidDomainsKeepExactCursorRedoAndSource() throws Exception {
        var initial = add(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("value"), d("0.5")));
        var undone = changed.undo().session();
        for (var command : List.of(new SetProperty(BUTTON, p("value"), d("1e400")),
                new SetProperty(BUTTON, p("variant"), s("filled")), new SetProperty(BUTTON, p("divisions"), i(0)),
                new SetProperty(BUTTON, p("value"), nil()), new SetProperty(BUTTON, p("onChanged"), new PropertyValue.CallbackValue("legacy")))) {
            assertRejectedUnchanged(undone, command, DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertExactPair(changed, undone.redo().session());
    }

    private static DesignerCommandSession add(DesignerCommandSession initial) {
        return applied(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 1),
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), BUTTON)));
    }
    private static PatchProperties patch(Map<PropertyName, PropertyValue> values) {
        return new PatchProperties(BUTTON, values.entrySet().stream()
                .map(e -> (PatchProperties.Patch) new PatchProperties.SetPatch(e.getKey(), e.getValue())).toList());
    }
    private static WidgetNode find(DesignerCommandSession session, StableId id) {
        return find(session.current().document().root(), id).orElseThrow();
    }
    private static Optional<WidgetNode> find(WidgetNode node, StableId id) {
        if (node.id().equals(id)) return Optional.of(node);
        for (WidgetSlot slot : node.slots().values()) {
            var children = slot instanceof WidgetSlot.SingleSlot single ? single.child().stream().toList()
                    : ((WidgetSlot.ListSlot) slot).children();
            for (var child : children) {
                var result = find(child, id);
                if (result.isPresent()) return result;
            }
        }
        return Optional.empty();
    }
    private static WidgetNode text(StableId id, String value) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static void assertRejectedUnchanged(DesignerCommandSession session, DesignerCommand command,
            DesignerCommandDiagnosticCode expected) {
        var result = session.apply(command);
        assertFalse(result.changed());
        assertSame(session, result.session());
        assertEquals(expected, result.diagnostics().getFirst().code());
        assertEquals(session.cursor(), result.session().cursor());
        assertEquals(session.canUndo(), result.session().canUndo());
        assertEquals(session.canRedo(), result.session().canRedo());
        assertExactPair(session, result.session());
    }
    private static void assertExactPair(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
        assertEquals(expected.current().document(), actual.current().document());
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession current) throws Exception {
        var saved = current.markSaved();
        var result = DesignerCommandSession.open(OriginalFdBytes.copyOf(saved.current().fdBytes(), FdCodecLimits.defaults()),
                saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DesignerCommandSession openDefault() throws Exception {
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(text(FIRST, "First"), text(SECOND, "Second")))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root);
        var generated = new DartRegionGenerator().generate(provisional, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(),
                generated.build().normalizedSha256()), root);
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), dart, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
