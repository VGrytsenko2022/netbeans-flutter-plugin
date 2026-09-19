package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.SwitchTestValues.*;

class SwitchCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = SwitchWidgetPropertySchema.SWITCH_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void creationAddMoveRemoveAndBothConstructorsKeepExactHistory() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(3, find(added, BUTTON).properties().size());
        assertExactPair(initial, added.undo().session());
        assertExactPair(added, added.undo().session().redo().session());
        var adaptive = applied(added, new SetProperty(BUTTON, p("variant"), s("adaptive")));
        assertTrue(source(adaptive).contains("Switch.adaptive("));
        var moved = applied(adaptive, new MoveWidget(BUTTON, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertExactPair(adaptive, moved.undo().session());
        assertExactPair(moved, reopen(moved));
        var removed = applied(moved, new RemoveWidget(BUTTON));
        assertExactPair(moved, removed.undo().session());
        assertEquals(FIRST, find(removed, FIRST).id());
        assertEquals(SECOND, find(removed, SECOND).id());
    }

    @Test
    void denseBothVariantsPatchSaveReopenResetAndUndoKeepAllIconDetails() throws Exception {
        for (String variant : SwitchWidgetPropertySchema.variants()) {
            var initial = add(openDefault());
            var values = full(variant);
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

    @Test
    void adaptiveAndImageDependenciesRejectPartialEditsButAtomicClearsPreserveUndo() throws Exception {
        var initial = add(openDefault());
        assertRejectedUnchanged(initial, new SetProperty(BUTTON, p("applyCupertinoTheme"), nil()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var adaptive = applied(initial, patch(Map.of(p("variant"), s("adaptive"), p("applyCupertinoTheme"), nil())));
        assertRejectedUnchanged(adaptive, new SetProperty(BUTTON, p("variant"), s("standard")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var standard = applied(adaptive, new PatchProperties(BUTTON, List.of(
                new PatchProperties.ResetPatch(p("applyCupertinoTheme")), new PatchProperties.SetPatch(p("variant"), s("standard")))));
        assertExactPair(initial, standard);
        assertExactPair(adaptive, standard.undo().session());
        for (String state : List.of("Active", "Inactive")) {
            String callback = "on" + state + "ThumbImageError";
            String image = Character.toLowerCase(state.charAt(0)) + state.substring(1) + "ThumbImage";
            assertRejectedUnchanged(initial, new SetProperty(BUTTON, p(callback), reference(callback)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            var configured = applied(initial, patch(Map.of(p(image), PropertyValue.ImageProviderValue.unresolved(), p(callback), reference(callback))));
            assertRejectedUnchanged(configured, new ResetProperty(BUTTON, p(image)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            var cleared = applied(configured, new PatchProperties(BUTTON, List.of(
                    new PatchProperties.ResetPatch(p(image)), new PatchProperties.ResetPatch(p(callback)))));
            assertExactPair(initial, cleared);
            assertExactPair(configured, cleared.undo().session());
            assertExactPair(configured, reopen(configured));
        }
        for (String name : List.of("value", "enabled", "variant")) {
            assertRejectedUnchanged(initial, new ResetProperty(BUTTON, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        }
    }

    @Test
    void iconNullInheritAndWholeStateFamiliesTransitionAtomicallyWithExactReplay() throws Exception {
        var initial = add(openDefault());
        var icon = applied(initial, patch(Map.of(p("thumbIconDefaultMode"), s("icon"), p("thumbIconDefaultSize"), d("24"))));
        assertRejectedUnchanged(icon, new SetProperty(BUTTON, p("thumbIconDefaultMode"), s("inherit")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var inherited = applied(icon, new PatchProperties(BUTTON, List.of(
                new PatchProperties.ResetPatch(p("thumbIconDefaultSize")), new PatchProperties.SetPatch(p("thumbIconDefaultMode"), s("inherit")))));
        assertExactPair(icon, inherited.undo().session());
        var reference = applied(inherited, new PatchProperties(BUTTON, List.of(
                new PatchProperties.ResetPatch(p("thumbIconDefaultMode")), new PatchProperties.SetPatch(p("thumbIcon"), reference("icons")))));
        assertExactPair(inherited, reference.undo().session());
        assertExactPair(reference, reopen(reference));
        assertRejectedUnchanged(reference, new SetProperty(BUTTON, p("thumbIconSelectedMode"), s("icon")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var nullColor = applied(initial, patch(Map.of(p("thumbColorDefault"), color(), p("thumbColorSelected"), nil())));
        assertRejectedUnchanged(nullColor, new SetProperty(BUTTON, p("thumbColor"), reference("colors")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(nullColor, reopen(nullColor));
    }

    @Test
    void disabledCallbackMetadataHistoryEmitsOnlyTheLatestReferenceAfterEnable() throws Exception {
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
    void invalidPropertiesPreserveExactCursorAndRedo() throws Exception {
        var initial = add(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("splashRadius"), d("-2")));
        var undone = changed.undo().session();
        for (var command : List.of(new SetProperty(BUTTON, p("splashRadius"), d("1e400")),
                new SetProperty(BUTTON, p("variant"), s("filled")),
                new SetProperty(BUTTON, p("thumbIconDefaultSize"), d("-1")),
                new SetProperty(BUTTON, p("value"), nil()),
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
