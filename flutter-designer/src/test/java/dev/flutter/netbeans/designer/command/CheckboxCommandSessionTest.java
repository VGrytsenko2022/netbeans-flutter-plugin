package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.CheckboxTestValues.*;

class CheckboxCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = CheckboxWidgetPropertySchema.CHECKBOX_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void creationAddMoveRemoveAndBothConstructorsHaveExactNativeCoreHistory() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(3, find(added, BUTTON).properties().size());
        assertExactPair(initial, added.undo().session());
        assertExactPair(added, added.undo().session().redo().session());
        var adaptive = applied(added, new SetProperty(BUTTON, p("variant"), s("adaptive")));
        assertTrue(source(adaptive).contains("Checkbox.adaptive("));
        var moved = applied(adaptive, new MoveWidget(BUTTON, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertExactPair(adaptive, moved.undo().session());
        assertExactPair(moved, reopen(moved));
        var removed = applied(moved, new RemoveWidget(BUTTON));
        assertExactPair(moved, removed.undo().session());
        assertEquals(FIRST, find(removed, FIRST).id());
        assertEquals(SECOND, find(removed, SECOND).id());
    }

    @Test
    void everyShapeDenseBothVariantsPatchOneStepThenSaveReopenResetAndUndo() throws Exception {
        for (String variant : CheckboxWidgetPropertySchema.variants()) {
            for (String shape : CheckboxWidgetPropertySchema.shapeKinds()) {
                var initial = add(openDefault());
                var values = full(variant, shape);
                var configured = applied(initial, patch(values));
                assertEquals(initial.cursor() + 1, configured.cursor());
                assertExactPair(initial, configured.undo().session());
                assertExactPair(configured, configured.undo().session().redo().session());
                var reopened = reopen(configured);
                assertExactPair(configured, reopened);
                var reset = applied(reopened, new PatchProperties(BUTTON, values.keySet().stream()
                        .filter(n -> !Set.of("value", "variant", "enabled").contains(n.value()))
                        .map(n -> (PatchProperties.Patch) new PatchProperties.ResetPatch(n)).toList()));
                assertEquals(3, find(reset, BUTTON).properties().size());
                assertExactPair(reopened, reset.undo().session());
                assertExactPair(reset, reopen(reset));
            }
        }
    }

    @Test
    void nullAndTristateTransitionsAreAtomicAndRequiredValuesCannotReset() throws Exception {
        var initial = add(openDefault());
        assertRejectedUnchanged(initial, new SetProperty(BUTTON, p("value"), nil()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var mixed = applied(initial, patch(Map.of(p("value"), nil(), p("tristate"), b(true))));
        assertTrue(source(mixed).contains("value: null"));
        assertExactPair(initial, mixed.undo().session());
        assertExactPair(mixed, reopen(mixed));
        assertRejectedUnchanged(mixed, new ResetProperty(BUTTON, p("tristate")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var concrete = applied(mixed, new PatchProperties(BUTTON, List.of(
                new PatchProperties.ResetPatch(p("tristate")), new PatchProperties.SetPatch(p("value"), b(false)))));
        assertExactPair(initial, concrete);
        assertExactPair(mixed, concrete.undo().session());
        for (String name : List.of("value", "enabled", "variant")) {
            assertRejectedUnchanged(mixed, new ResetProperty(BUTTON, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        }
    }

    @Test
    void referenceFamilyAndPlainStatefulSideSwitchesPreserveExactResetUndoState() throws Exception {
        var initial = add(openDefault());
        var stateful = applied(initial, patch(Map.of(p("sideStateful"), b(true), p("sideWidth"), d("2"),
                p("sideSelectedMode"), s("inherit"), p("fillColorDefault"), color(), p("fillColorDisabled"), nil())));
        assertRejectedUnchanged(stateful, new SetProperty(BUTTON, p("sideSelectedColor"), color()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var plain = applied(stateful, new PatchProperties(BUTTON, List.of(
                new PatchProperties.ResetPatch(p("sideSelectedMode")), new PatchProperties.SetPatch(p("sideStateful"), b(false)))));
        assertTrue(source(plain).contains("side: const BorderSide("));
        assertExactPair(stateful, plain.undo().session());
        var patches = new ArrayList<PatchProperties.Patch>();
        for (String name : CheckboxWidgetPropertySchema.sideLocalProperties()) {
            if (find(stateful, BUTTON).properties().containsKey(p(name))) patches.add(new PatchProperties.ResetPatch(p(name)));
        }
        patches.add(new PatchProperties.SetPatch(p("side"), reference("statefulSide")));
        var referenced = applied(stateful, new PatchProperties(BUTTON, patches));
        assertTrue(source(referenced).contains("buttonValues.statefulSide"));
        assertExactPair(stateful, referenced.undo().session());
        assertExactPair(referenced, reopen(referenced));
    }

    @Test
    void disabledCallbackMetadataEditsRetainSameDartUndoRedoAndLatestReferenceAfterEnable() throws Exception {
        var disabled = applied(add(openDefault()), new SetProperty(BUTTON, p("enabled"), b(false)));
        var first = applied(disabled, new SetProperty(BUTTON, p("onChanged"), reference("first")));
        var second = applied(first, new SetProperty(BUTTON, p("onChanged"), reference("second")));
        assertArrayEquals(first.current().dartCandidateBytes(), second.current().dartCandidateBytes());
        assertFalse(Arrays.equals(first.current().fdBytes(), second.current().fdBytes()));
        assertExactPair(first, second.undo().session());
        assertExactPair(second, second.undo().session().redo().session());
        var enabled = applied(reopen(second), new SetProperty(BUTTON, p("enabled"), b(true)));
        assertTrue(source(enabled).contains("buttonValues.second"));
        assertFalse(source(enabled).contains("buttonValues.first"));
        assertExactPair(enabled, reopen(enabled));
    }

    @Test
    void invalidPropertiesAndShapeRelationsPreserveExactCursorAndRedo() throws Exception {
        var initial = add(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("splashRadius"), d("-2")));
        var undone = changed.undo().session();
        for (var command : List.of(new SetProperty(BUTTON, p("splashRadius"), d("1e400")),
                new SetProperty(BUTTON, p("variant"), s("filled")),
                new SetProperty(BUTTON, p("sideWidth"), d("-1")),
                new SetProperty(BUTTON, p("onChanged"), new PropertyValue.CallbackValue("legacy")))) {
            assertRejectedUnchanged(undone, command, DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertRejectedUnchanged(undone, new SetProperty(BUTTON, p("shapeRadius"), value("shapeRadius")),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
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
