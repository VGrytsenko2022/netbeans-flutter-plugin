package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.FloatingActionButtonTestValues.*;

class FloatingActionButtonCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId FAB = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child"), ICON = new SlotName("icon"), CHILDREN = new SlotName("children");

    @Test
    void ordinaryCreationAllFourVariantsAndConditionalSlotRemovalPreserveIdentity() throws Exception {
        var initial = openDefault();
        var empty = add(initial);
        assertTrue(source(empty).contains("child: null"));
        assertExactPair(initial, empty.undo().session());
        for (String variant : List.of("small", "large")) {
            var changed = applied(empty, new SetProperty(FAB, p("variant"), s(variant)));
            assertExactPair(changed, reopen(changed));
        }
        assertRejectedUnchanged(empty, new SetProperty(FAB, p("variant"), s("extended")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var populated = applied(empty, new MoveWidget(FIRST, new WidgetPlacement(FAB, CHILD, 0)));
        var extended = applied(populated, new SetProperty(FAB, p("variant"), s("extended")));
        var withIcon = applied(extended, new MoveWidget(SECOND, new WidgetPlacement(FAB, ICON, 0)));
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) find(withIcon, FAB).slots().get(CHILD)).child().orElseThrow().id());
        assertEquals(SECOND, ((WidgetSlot.SingleSlot) find(withIcon, FAB).slots().get(ICON)).child().orElseThrow().id());
        for (String variant : List.of("standard", "small", "large")) assertRejectedUnchanged(withIcon, new SetProperty(FAB, p("variant"), s(variant)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(withIcon, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var collapsed = applied(withIcon, new SetProperty(FAB, p("isExtended"), new PropertyValue.BooleanValue(false)));
        assertRejectedUnchanged(collapsed, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(collapsed, reopen(collapsed));
        var returned = applied(collapsed, new MoveWidget(SECOND, new WidgetPlacement(ROOT, CHILDREN, 0)));
        returned = applied(returned, new PatchProperties(FAB, List.of(new PatchProperties.SetPatch(p("variant"), s("small")), new PatchProperties.ResetPatch(p("isExtended")))));
        var cleared = applied(returned, new RemoveWidget(FIRST));
        assertTrue(source(cleared).contains("child: null"));
        assertExactPair(returned, cleared.undo().session());
        assertExactPair(cleared, reopen(cleared));
    }

    @Test
    void denseAllConstructorFamiliesHaveExactOneStepUndoRedoSaveResetAndRequiredSelectors() throws Exception {
        for (String variant : FloatingActionButtonWidgetPropertySchema.variants()) for (boolean paints : List.of(false, true)) {
            var initial = add(openDefault());
            var populated = applied(initial, new MoveWidget(FIRST, new WidgetPlacement(FAB, CHILD, 0)));
            var values = full(variant, "roundedRectangle", paints);
            var configured = applied(populated, new PatchProperties(FAB, values.entrySet().stream()
                    .map(e -> (PatchProperties.Patch) new PatchProperties.SetPatch(e.getKey(), e.getValue())).toList()));
            assertEquals(populated.cursor() + 1, configured.cursor());
            assertExactPair(populated, configured.undo().session());
            assertExactPair(configured, configured.undo().session().redo().session());
            var reopened = reopen(configured);
            assertExactPair(configured, reopened);
            for (String name : List.of("enabled", "variant")) {
                var result = reopened.apply(new ResetProperty(FAB, p(name)));
                assertFalse(result.changed());
                assertSame(reopened, result.session());
            }
            var reset = applied(reopened, new PatchProperties(FAB, values.keySet().stream()
                    .filter(n -> !Set.of("variant", "enabled").contains(n.value()))
                    .map(n -> (PatchProperties.Patch) new PatchProperties.ResetPatch(n)).toList()));
            assertEquals(2, find(reset, FAB).properties().size());
            assertExactPair(reset, reopen(reset));
            assertExactPair(reopened, reset.undo().session());
        }
    }

    @Test
    void inactiveCallbackMetadataHasRealHistoryAndReenableRestoresExactTypedSource() throws Exception {
        var initial = add(openDefault());
        var disabled = applied(initial, new SetProperty(FAB, p("enabled"), new PropertyValue.BooleanValue(false)));
        var first = applied(disabled, new SetProperty(FAB, p("onPressed"), reference("first")));
        var second = applied(first, new SetProperty(FAB, p("onPressed"), reference("second")));
        assertArrayEquals(first.current().dartCandidateBytes(), second.current().dartCandidateBytes());
        assertFalse(Arrays.equals(first.current().fdBytes(), second.current().fdBytes()));
        assertExactPair(first, second.undo().session());
        assertExactPair(second, second.undo().session().redo().session());
        var saved = reopen(second);
        var enabled = applied(saved, new SetProperty(FAB, p("enabled"), new PropertyValue.BooleanValue(true)));
        assertTrue(source(enabled).contains("fabValues.second"));
        assertFalse(source(enabled).contains("fabValues.first"));
        assertExactPair(saved, enabled.undo().session());
        assertExactPair(enabled, reopen(enabled));
    }

    @Test
    void heroNullOmissionAndEveryLiteralKindPersistAndResetWithoutFakeDefaults() throws Exception {
        var initial = add(openDefault());
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), s("quote'\\n$tag"), i(-2), d("2.5"), new PropertyValue.BooleanValue(false), reference("hero"))) {
            var configured = applied(initial, new SetProperty(FAB, p("heroTag"), value));
            assertTrue(source(configured).contains("heroTag:"));
            assertEquals(value, find(reopen(configured), FAB).properties().get(p("heroTag")));
            var reset = applied(configured, new ResetProperty(FAB, p("heroTag")));
            assertExactPair(initial, reset);
            assertExactPair(configured, reset.undo().session());
        }
    }

    @Test
    void invalidValueShapeAndConstructorTransitionsPreserveExactRedoAndCandidate() throws Exception {
        var initial = add(openDefault());
        var changed = applied(initial, new SetProperty(FAB, p("elevation"), infinity()));
        var undone = changed.undo().session();
        assertTrue(undone.canRedo());
        for (var command : List.of(new SetProperty(FAB, p("elevation"), d("-1")),
                new SetProperty(FAB, p("heroTag"), d("1e400")),
                new SetProperty(FAB, p("clipBehavior"), new PropertyValue.NullValue()))) {
            assertRejectedUnchanged(undone, command, DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertRejectedUnchanged(undone, new SetProperty(FAB, p("shapeRadius"), value("shapeRadius")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(undone, new SetProperty(FAB, p("extendedPadding"), value("extendedPadding")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(changed, undone.redo().session());
    }

    @Test
    void slotReplacementRejectsFlexParentDataAndStandardIconBeforeCommitting() throws Exception {
        var initial = add(openDefault());
        var spacer = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(), StableId.random());
        assertRejectedUnchanged(initial, new AddWidget(new WidgetPlacement(FAB, CHILD, 0), spacer), DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
        assertRejectedUnchanged(initial, new MoveWidget(FIRST, new WidgetPlacement(FAB, ICON, 0)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var withChild = applied(initial, new MoveWidget(FIRST, new WidgetPlacement(FAB, CHILD, 0)));
        var replacement = applied(withChild, new ReplaceSlotChild(FAB, CHILD, FIRST, new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, ((WidgetSlot.SingleSlot) find(replacement, FAB).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(withChild, replacement.undo().session());
        assertExactPair(replacement, reopen(replacement));
    }

    private static DesignerCommandSession add(DesignerCommandSession initial) {
        return applied(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), FAB)));
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
