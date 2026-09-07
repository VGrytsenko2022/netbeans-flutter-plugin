package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.RangeSliderTestValues.*;

class RangeSliderCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void creationMoveRemovePreserveExactHistoryAndStableNeighbors() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(Map.of(p("valuesStart"), i(0), p("valuesEnd"), i(1), p("enabled"), b(true)), find(added, BUTTON).properties());
        assertExactPair(initial, added.undo().session());
        var moved = applied(added, new MoveWidget(BUTTON, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertExactPair(added, moved.undo().session());
        assertExactPair(moved, reopen(moved));
        var removed = applied(moved, new RemoveWidget(BUTTON));
        assertExactPair(moved, removed.undo().session());
        assertEquals(FIRST, find(removed, FIRST).id());
        assertEquals(SECOND, find(removed, SECOND).id());
    }

    @Test
    void denseAll37FieldsWholeFamiliesResetAndSaveReopenHaveExactHistory() throws Exception {
        var initial = add(openDefault());
        var dense = applied(initial, patch(full()));
        assertExactPair(dense, reopen(dense));
        assertExactPair(initial, dense.undo().session());
        assertExactPair(dense, dense.undo().session().redo().session());
        Set<PropertyName> covered = new HashSet<>(full().keySet());
        var replacements = new ArrayList<PatchProperties.Patch>();
        for (String name : RangeSliderWidgetPropertySchema.definitions().keySet()) {
            if (!Set.of("valuesStart", "valuesEnd", "enabled").contains(name)) replacements.add(new PatchProperties.ResetPatch(p(name)));
        }
        replacements.add(new PatchProperties.SetPatch(p("valuesStart"), i(0)));
        replacements.add(new PatchProperties.SetPatch(p("valuesEnd"), i(1)));
        var reset = applied(dense, new PatchProperties(BUTTON, replacements));
        assertEquals(3, find(reset, BUTTON).properties().size());
        assertExactPair(dense, reset.undo().session());
        assertExactPair(reset, reopen(reset));
        for (String family : List.of("labels", "overlayColor", "mouseCursor")) {
            var whole = applied(reset, new SetProperty(BUTTON, p(family), reference(family)));
            covered.add(p(family));
            assertExactPair(whole, reopen(whole));
            assertExactPair(reset, whole.undo().session());
            assertExactPair(whole, whole.undo().session().redo().session());
        }
        assertEquals(37, covered.size());
    }

    @Test
    void rangeEndpointsAndBoundsRejectInvalidIntermediateStatesWithoutClamping() throws Exception {
        var initial = add(openDefault());
        for (String name : List.of("valuesStart", "min")) {
            assertRejectedUnchanged(initial, new SetProperty(BUTTON, p(name), i(2)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        }
        assertRejectedUnchanged(initial, new SetProperty(BUTTON, p("valuesEnd"), i(-1)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var range = applied(initial, patch(Map.of(p("min"), i(20), p("max"), i(30), p("valuesStart"), i(23), p("valuesEnd"), i(28))));
        assertRejectedUnchanged(range, new ResetProperty(BUTTON, p("max")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(initial, range.undo().session());
        assertExactPair(range, reopen(range));
        for (boolean negative : List.of(false, true)) {
            var fields = new LinkedHashMap<PropertyName, PropertyValue>();
            RangeSliderWidgetPropertySchema.rangeProperties().forEach(name -> fields.put(p(name), infinity(negative)));
            var infinite = applied(range, patch(fields));
            assertExactPair(range, infinite.undo().session());
            assertExactPair(infinite, reopen(infinite));
        }
    }

    @Test
    void nullableLabelsAndEachLocalStateFamilySwitchAtomicallyWithoutDataLoss() throws Exception {
        var initial = add(openDefault());
        var labels = applied(initial, new SetProperty(BUTTON, p("labelsEnd"), s("End\r\n")));
        assertTrue(source(labels).contains("RangeLabels('', "));
        assertRejectedUnchanged(labels, new SetProperty(BUTTON, p("labels"), nil()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var absent = applied(labels, new PatchProperties(BUTTON, List.of(new PatchProperties.ResetPatch(p("labelsEnd")), new PatchProperties.SetPatch(p("labels"), nil()))));
        assertTrue(source(absent).contains("labels: null"));
        assertExactPair(labels, absent.undo().session());
        assertExactPair(absent, reopen(absent));
        for (String family : List.of("overlayColor", "mouseCursor")) {
            var whole = applied(initial, new SetProperty(BUTTON, p(family), reference(family)));
            assertRejectedUnchanged(whole, new SetProperty(BUTTON, p(family + "Default"), nil()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            var local = applied(whole, new PatchProperties(BUTTON, List.of(new PatchProperties.ResetPatch(p(family)),
                    new PatchProperties.SetPatch(p(family + "Default"), family.equals("overlayColor") ? color() : s("click")),
                    new PatchProperties.SetPatch(p(family + "Pressed"), nil()))));
            assertExactPair(whole, local.undo().session());
            assertExactPair(local, reopen(local));
        }
    }

    @Test
    void inactiveChangedRetainsSameDartHistoryAndEnabledUsesNewestReference() throws Exception {
        var initial = applied(add(openDefault()), new SetProperty(BUTTON, p("enabled"), b(false)));
        var first = applied(initial, new SetProperty(BUTTON, p("onChanged"), reference("firstCallback")));
        var second = applied(first, new SetProperty(BUTTON, p("onChanged"), reference("secondCallback")));
        assertArrayEquals(initial.current().dartCandidateBytes(), second.current().dartCandidateBytes());
        assertFalse(Arrays.equals(first.current().fdBytes(), second.current().fdBytes()));
        assertExactPair(first, second.undo().session());
        assertExactPair(second, second.undo().session().redo().session());
        assertExactPair(second, reopen(second));
        var enabled = applied(second, new SetProperty(BUTTON, p("enabled"), b(true)));
        assertTrue(source(enabled).contains("secondCallback"));
        assertFalse(source(enabled).contains("firstCallback"));
        assertExactPair(second, enabled.undo().session());
        for (String callback : List.of("onChangeStart", "onChangeEnd", "semanticFormatterCallback")) {
            var changed = applied(second, new SetProperty(BUTTON, p(callback), reference("activeReference")));
            assertTrue(source(changed).contains("activeReference"));
            assertExactPair(second, changed.undo().session());
        }
    }

    @Test
    void failedEditsPreserveCursorAndRedoIncludingRequiredAndUnsupportedFields() throws Exception {
        var initial = add(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("valuesStart"), d("0.25")));
        var undone = changed.undo().session();
        for (String name : List.of("valuesStart", "valuesEnd", "enabled")) {
            assertRejectedUnchanged(undone, new ResetProperty(BUTTON, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        }
        for (var command : List.of(new SetProperty(BUTTON, p("valuesStart"), d("1e400")),
                new SetProperty(BUTTON, p("divisions"), i(0)), new SetProperty(BUTTON, p("valuesStart"), nil()),
                new SetProperty(BUTTON, p("onChanged"), new PropertyValue.CallbackValue("legacy")))) {
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
