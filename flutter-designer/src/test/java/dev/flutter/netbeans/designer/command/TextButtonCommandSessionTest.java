package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextButtonCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId WRAPPER = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");

    @Test
    void requiredWrapperAndDenseStyleHistoryRoundTripBothConstructorsAndEveryLegalFamily() throws Exception {
        for (boolean icon : List.of(false, true)) for (boolean paint : List.of(false, true)) {
            var initial = openDefault();
            var base = wrap(initial);
            assertEquals(FIRST, child(base).id());
            assertExactPair(initial, base.undo().session());
            var values = TextButtonTestValues.full(icon, paint, false);
            var patches = values.entrySet().stream().map(entry ->
                    (PatchProperties.Patch) new PatchProperties.SetPatch(entry.getKey(), entry.getValue())).toList();
            var styled = applied(base, new PatchProperties(WRAPPER, patches));
            assertEquals(491, find(styled, WRAPPER).properties().size());
            assertEquals(base.cursor() + 1, styled.cursor());
            assertExactPair(base, styled.undo().session());
            assertExactPair(styled, styled.undo().session().redo().session());
            var saved = reopen(styled);
            assertExactPair(styled, saved);
            var resets = values.keySet().stream()
                    .filter(name -> !Set.of("enabled", "variant").contains(name.value()))
                    .map(name -> (PatchProperties.Patch) new PatchProperties.ResetPatch(name)).toList();
            var reset = applied(saved, new PatchProperties(WRAPPER, resets));
            assertEquals(2, find(reset, WRAPPER).properties().size());
            assertEquals(FIRST, child(reset).id());
            assertExactPair(saved, reset.undo().session());
            assertExactPair(reset, reopen(reset));
        }
    }

    @Test
    void wholeStyleSwitchIsAtomicAndRejectsAnyUnclearedLeafWithoutLosingRedo() throws Exception {
        var base = wrap(openDefault());
        var local = applied(base, new SetProperty(WRAPPER, p("styleErrorBackgroundColor"), new PropertyValue.ColorValue(0xff123456L)));
        assertRejectedUnchanged(local, new SetProperty(WRAPPER, p("style"), reference()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var project = applied(local, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.ResetPatch(p("styleErrorBackgroundColor")),
                new PatchProperties.SetPatch(p("style"), reference()))));
        assertExactPair(local, project.undo().session());
        assertExactPair(project, reopen(project));
        var withRedo = project.undo().session();
        assertTrue(withRedo.canRedo());
        assertRejectedUnchanged(withRedo, new SetProperty(WRAPPER, p("style"), reference()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(project, withRedo.redo().session());
        var toLocal = applied(project, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.ResetPatch(p("style")),
                new PatchProperties.SetPatch(p("styleDraggedIconColor"), new PropertyValue.ColorValue(0xff123456L)))));
        assertExactPair(project, toLocal.undo().session());
    }

    @Test
    void populatedIconPreventsStandardSwitchAndRequiredChildIsNeverDeleted() throws Exception {
        var base = wrap(openDefault());
        var icon = applied(base, new SetProperty(WRAPPER, p("variant"), s("icon")));
        var withIcon = applied(icon, new MoveWidget(SECOND, new WidgetPlacement(WRAPPER, new SlotName("icon"), 0)));
        assertTrue(source(withIcon).contains("TextButton.icon("));
        assertTrue(source(withIcon).contains("label: const Text('First')"));
        assertRejectedUnchanged(withIcon, new SetProperty(WRAPPER, p("variant"), s("standard")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(withIcon, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertRejectedUnchanged(withIcon, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 0)), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var movedBack = applied(withIcon, new MoveWidget(SECOND, new WidgetPlacement(ROOT, CHILDREN, 1)));
        var standard = applied(movedBack, new SetProperty(WRAPPER, p("variant"), s("standard")));
        assertEquals(FIRST, child(standard).id());
        assertExactPair(movedBack, standard.undo().session());
        assertRejectedUnchanged(standard, new MoveWidget(SECOND, new WidgetPlacement(WRAPPER, new SlotName("icon"), 0)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(standard, reopen(standard));
    }

    @Test
    void nullableBooleanAndClipStayDistinctAcrossSaveResetAndHistory() throws Exception {
        for (String name : List.of("clipBehavior", "isSemanticButton")) {
            var base = wrap(openDefault());
            var nulled = applied(base, new SetProperty(WRAPPER, p(name), new PropertyValue.NullValue()));
            assertTrue(source(nulled).contains(name + ": null"));
            assertExactPair(base, nulled.undo().session());
            var saved = reopen(nulled);
            assertEquals(new PropertyValue.NullValue(), find(saved, WRAPPER).properties().get(p(name)));
            var explicit = applied(saved, new SetProperty(WRAPPER, p(name), name.equals("clipBehavior")
                    ? new PropertyValue.EnumValue("Clip", "none") : new PropertyValue.BooleanValue(false)));
            assertExactPair(saved, explicit.undo().session());
            var reset = applied(explicit, new ResetProperty(WRAPPER, p(name)));
            assertFalse(source(reset).contains(name + ":"));
            assertExactPair(base, reset);
        }
    }

    @Test
    void variantScalarTransitionPrunesOnlyIncompatibleFieldsAndRollsBackMissingRequiredSelectors() throws Exception {
        var base = wrap(openDefault());
        var semantic = applied(base, new SetProperty(WRAPPER, p("isSemanticButton"), new PropertyValue.NullValue()));
        assertRejectedUnchanged(semantic, new SetProperty(WRAPPER, p("variant"), s("icon")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var icon = applied(semantic, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.ResetPatch(p("isSemanticButton")), new PatchProperties.SetPatch(p("variant"), s("icon")),
                new PatchProperties.SetPatch(p("iconAlignment"), new PropertyValue.EnumValue("IconAlignment", "end")))));
        assertExactPair(semantic, icon.undo().session());
        var standard = applied(icon, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.ResetPatch(p("iconAlignment")), new PatchProperties.SetPatch(p("variant"), s("standard")))));
        assertExactPair(base, standard);
        for (String name : List.of("variant", "enabled")) {
            assertRejectedUnchanged(icon, new ResetProperty(WRAPPER, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
            assertRejectedUnchanged(icon, new SetProperty(WRAPPER, p(name), new PropertyValue.NullValue()), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
    }

    @Test
    void disabledActivationReferencesRemainStoredAndRestoreOnEnableWithExactPairHistory() throws Exception {
        var base = wrap(openDefault());
        var longOnly = applied(base, new SetProperty(WRAPPER, p("onLongPress"), reference()));
        assertTrue(source(longOnly).contains("onPressed: null"));
        var disabled = applied(longOnly, new SetProperty(WRAPPER, p("enabled"), new PropertyValue.BooleanValue(false)));
        assertEquals(reference(), find(disabled, WRAPPER).properties().get(p("onLongPress")));
        assertFalse(source(disabled).contains("buttonValues.callback"));
        assertExactPair(longOnly, disabled.undo().session());
        var saved = reopen(disabled);
        var enabled = applied(saved, new SetProperty(WRAPPER, p("enabled"), new PropertyValue.BooleanValue(true)));
        assertExactPair(longOnly, enabled);
        assertTrue(source(enabled).contains("buttonValues.callback"));
        assertEquals(FIRST, child(enabled).id());
    }

    @Test
    void replacingRequiredChildAndMovingWholeWrapperPreservesBothSlotsAndStableIds() throws Exception {
        var base = wrap(openDefault());
        var replaced = applied(base, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(text(StableId.random(), "Replacement"))));
        assertExactPair(base, replaced.undo().session());
        var moved = applied(replaced, new MoveWidget(WRAPPER, new WidgetPlacement(ROOT, CHILDREN, 1)));
        assertExactPair(replaced, moved.undo().session());
        var nested = applied(moved, new WrapWidget(child(moved).id(),
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random()), CHILD, 0));
        assertExactPair(moved, nested.undo().session());
        assertExactPair(nested, reopen(nested));
        var removed = applied(nested, new RemoveWidget(WRAPPER));
        assertExactPair(nested, removed.undo().session());
    }

    @Test
    void metadataSourceEnvelopeSaveRetainsManagedBytesAndEveryHistoryPosition() throws Exception {
        var saved = applied(wrap(openDefault()), new SetProperty(WRAPPER, p("enabled"),
                new PropertyValue.BooleanValue(false))).markSaved();
        var metadata = applied(saved, new SetProperty(WRAPPER, p("onPressed"), reference()));
        assertEquals(DesignerRevisionPersistenceKind.FD_ONLY, metadata.current().persistenceKind());
        byte[] exact = ("// historical user envelope\n" + source(metadata)).getBytes(StandardCharsets.UTF_8);
        var anchored = metadata.markSavedWithSourceEnvelope(exact);
        assertFalse(anchored.dirty());
        assertEquals(metadata.current().revisionId(), anchored.current().revisionId());
        assertArrayEquals(metadata.current().fdBytes(), anchored.current().fdBytes());
        assertArrayEquals(exact, anchored.current().dartCandidateBytes());
        exact[0] = 'X';
        assertTrue(source(anchored).startsWith("// historical user envelope"));
        var undone = anchored.undo().session();
        assertEquals(DesignerRevisionPersistenceKind.FD_ONLY, undone.current().persistenceKind());
        assertExactPair(anchored, undone.redo().session());
        assertExactPair(anchored, reopen(anchored));
    }

    @Test
    void metadataSourceEnvelopeSaveRejectsChangedManagedPayloadMalformedAndOversizedInput() throws Exception {
        var saved = applied(wrap(openDefault()), new SetProperty(WRAPPER, p("enabled"),
                new PropertyValue.BooleanValue(false))).markSaved();
        var metadata = applied(saved, new SetProperty(WRAPPER, p("onPressed"), reference()));
        assertThrows(IllegalArgumentException.class, () -> metadata.markSavedWithSourceEnvelope(
                source(metadata).replace("TextButton(", "TextField(").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> metadata.markSavedWithSourceEnvelope(new byte[]{(byte) 0xff}));
        assertThrows(IllegalArgumentException.class, () -> metadata.markSavedWithSourceEnvelope(
                ("//" + "x".repeat(2_097_152) + "\n" + source(metadata)).getBytes(StandardCharsets.UTF_8)));
        assertEquals(DesignerRevisionPersistenceKind.FD_ONLY, metadata.current().persistenceKind());
        assertTrue(metadata.dirty());
    }

    @Test
    void metadataSourceEnvelopeSaveRejectsCleanAndPairedRevisions() throws Exception {
        var clean = openDefault();
        var paired = wrap(clean);
        assertThrows(IllegalArgumentException.class,
                () -> clean.markSavedWithSourceEnvelope(clean.current().dartCandidateBytes()));
        assertThrows(IllegalArgumentException.class,
                () -> paired.markSavedWithSourceEnvelope(paired.current().dartCandidateBytes()));
    }

    private static DesignerCommandSession wrap(DesignerCommandSession session) {
        return applied(session, new WrapWidget(FIRST, WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), WRAPPER), CHILD, 0));
    }
    private static WidgetNode child(DesignerCommandSession session) {
        return ((WidgetSlot.SingleSlot) find(session, WRAPPER).slots().get(CHILD)).child().orElseThrow();
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static PropertyValue.DartObjectReferenceValue reference() {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:buttons/styles.dart"), "buttonValues", Optional.of("callback"),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
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
