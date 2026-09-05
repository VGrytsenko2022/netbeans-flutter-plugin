package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisibilityCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.Visibility");
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId WRAPPER = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName REPLACEMENT = new SlotName("replacement");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final PropertyName VISIBLE = new PropertyName("visible");

    @Test
    void wrapIsOneAtomicEditAndSaveReopenPreservesFurtherBooleanAndDescendantEditing() throws Exception {
        var initial = openDefault();
        var wrapped = wrap(initial);
        assertEquals(1, wrapped.cursor());
        assertEquals(Map.of(), find(wrapped, WRAPPER).properties());
        assertEquals(FIRST, child(wrapped).id());
        assertExactPair(initial, wrapped.undo().session());
        assertExactPair(wrapped, wrapped.undo().session().redo().session());
        var reopened = reopen(wrapped);
        assertFalse(reopened.dirty());
        assertExactPair(wrapped, reopened);
        var changed = applied(reopened, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(false)));
        changed = applied(changed, new SetProperty(FIRST, new PropertyName("data"), new PropertyValue.StringValue("After reopen")));
        assertTrue(source(changed).contains("visible: false"));
        assertTrue(source(changed).contains("Text('After reopen')"));
        var finalOpen = reopen(changed);
        assertExactPair(changed, finalOpen);
        assertEquals(FIRST, child(finalOpen).id());
        assertEquals(new PropertyValue.BooleanValue(false), find(finalOpen, WRAPPER).properties().get(VISIBLE));
    }

    @Test
    void checkboxStatesResetAndOneStepHistoryKeepRequiredChildAndRejectMalformedValues() throws Exception {
        var current = wrap(openDefault());
        var originalChild = child(current);
        for (boolean value : List.of(false, true)) {
            var before = current;
            current = applied(current, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(value)));
            assertEquals(before.cursor() + 1, current.cursor());
            assertTrue(source(current).contains("visible: " + value));
            assertEquals(originalChild, child(current));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        var explicit = current;
        current = applied(current, new ResetProperty(WRAPPER, VISIBLE));
        assertTrue(find(current, WRAPPER).properties().isEmpty());
        assertFalse(source(current).contains("visible:"));
        assertEquals(originalChild, child(current));
        assertExactPair(explicit, current.undo().session());
        assertExactPair(current, current.undo().session().redo().session());
        var withRedo = current.undo().session();
        assertTrue(withRedo.canRedo());
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.DoubleValue(BigDecimal.ZERO), new PropertyValue.DartExpressionValue("false"))) {
            assertRejectedUnchanged(withRedo, new SetProperty(WRAPPER, VISIBLE, invalid),
                    DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String invented : List.of("key", "canRequestFocus", "skipTraversal", "includeSemantics", "descendantsAreFocusable", "descendantsAreTraversable")) {
            assertRejectedUnchanged(withRedo, new SetProperty(WRAPPER, new PropertyName(invented), new PropertyValue.BooleanValue(true)),
                    DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN);
        }
        assertEquals(originalChild, child(withRedo));
    }

    @Test
    void requiredChildCannotBeClearedMovedAwayOrReplacedByEmptyPrototype() throws Exception {
        var wrapped = wrap(openDefault());
        assertRejectedUnchanged(wrapped, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertRejectedUnchanged(wrapped, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 1)),
                DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var detached = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
        assertRejectedUnchanged(wrapped, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), detached),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(wrapped, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(detached)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var replacement = text(StableId.random(), "Replacement");
        var replaced = applied(wrapped, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(replacement, child(replaced));
        assertEquals(WRAPPER, find(replaced, WRAPPER).id());
        assertExactPair(wrapped, replaced.undo().session());
        assertExactPair(replaced, replaced.undo().session().redo().session());
        var movedExisting = applied(replaced, new ReplaceSlotChild(WRAPPER, CHILD, replacement.id(),
                new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, child(movedExisting).id());
        assertEquals(List.of(WRAPPER), rootChildren(movedExisting).stream().map(WidgetNode::id).toList());
        assertExactPair(replaced, movedExisting.undo().session());
        assertExactPair(movedExisting, reopen(movedExisting));
    }

    @Test
    void movingAndNestingConfiguredWrapperPreservesStableSubtreeAndHistory() throws Exception {
        var wrapped = applied(wrap(openDefault()), new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(false)));
        var original = find(wrapped, WRAPPER);
        var moved = applied(wrapped, new MoveWidget(WRAPPER, new WidgetPlacement(ROOT, CHILDREN, 1)));
        assertEquals(List.of(SECOND, WRAPPER), rootChildren(moved).stream().map(WidgetNode::id).toList());
        assertEquals(original, find(moved, WRAPPER));
        assertExactPair(wrapped, moved.undo().session());
        var nestedId = StableId.random();
        var nested = applied(moved, new WrapWidget(FIRST,
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), nestedId), CHILD, 0));
        assertEquals(nestedId, child(nested).id());
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) child(nested).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(moved, nested.undo().session());
        assertExactPair(nested, nested.undo().session().redo().session());
        assertRejectedUnchanged(nested, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(text(StableId.random(), "Stale"))),
                DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT);
    }

    @Test
    void completeChronologicalHistoryRestoresEveryPairAcrossAllScalarAndStructuralEdits() throws Exception {
        var states = new java.util.ArrayList<DesignerCommandSession>();
        var current = openDefault();
        states.add(current);
        current = wrap(current);
        states.add(current);
        current = applied(current, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(false)));
        states.add(current);
        current = applied(current, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(true)));
        states.add(current);
        current = applied(current, new ResetProperty(WRAPPER, VISIBLE));
        states.add(current);
        current = applied(current, new SetProperty(FIRST, new PropertyName("data"), new PropertyValue.StringValue("Edited child")));
        states.add(current);
        var replacement = text(StableId.random(), "New retained child");
        current = applied(current, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(replacement)));
        states.add(current);
        current = applied(current, new MoveWidget(WRAPPER, new WidgetPlacement(ROOT, CHILDREN, 1)));
        states.add(current);
        assertExactPair(current, reopen(current));
        for (int index = states.size() - 2; index >= 0; index--) {
            assertTrue(current.canUndo());
            current = current.undo().session();
            assertExactPair(states.get(index), current);
            assertEquals(index, current.cursor());
        }
        assertFalse(current.canUndo());
        for (int index = 1; index < states.size(); index++) {
            assertTrue(current.canRedo());
            current = current.redo().session();
            assertExactPair(states.get(index), current);
            assertEquals(index, current.cursor());
        }
        assertFalse(current.canRedo());
    }

    @Test
    void rootWrappingAndMixedVisibilityFocusNestingKeepIndependentDefaultsAndStableIds() throws Exception {
        var initial = openDefault();
        var rootWrapped = applied(initial, new WrapWidget(ROOT,
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), WRAPPER), CHILD, 0));
        assertEquals(TYPE, rootWrapped.current().document().root().type());
        assertEquals(ROOT, child(rootWrapped).id());
        assertEquals(initial.current().document().root(), child(rootWrapped));
        assertTrue(find(rootWrapped, WRAPPER).properties().isEmpty());
        assertExactPair(initial, rootWrapped.undo().session());
        var focusType = new WidgetTypeId("flutter.widgets.ExcludeFocus");
        var focusId = StableId.random();
        var mixed = applied(rootWrapped, new WrapWidget(FIRST,
                WidgetNodePrototypeFactory.create(CATALOG.find(focusType).orElseThrow(), focusId), CHILD, 0));
        mixed = applied(mixed, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(false)));
        assertEquals(Map.of(VISIBLE, new PropertyValue.BooleanValue(false)), find(mixed, WRAPPER).properties());
        assertEquals(Map.of(), find(mixed, focusId).properties());
        assertTrue(source(mixed).contains("Visibility("));
        assertTrue(source(mixed).contains("ExcludeFocus("));
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) find(mixed, focusId).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(mixed, reopen(mixed));
    }

    @Test
    void cannotInsertVisibilityWrapperBetweenFlexParentAndItsParentDataChild() throws Exception {
        for (String parentData : List.of("Expanded", "Flexible", "Spacer")) {
            var initial = openDefault();
            var dataType = new WidgetTypeId("flutter.widgets." + parentData);
            var dataId = StableId.random();
            var dataPrototype = WidgetNodePrototypeFactory.create(CATALOG.find(dataType).orElseThrow(), dataId);
            var withData = parentData.equals("Spacer")
                    ? applied(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), dataPrototype))
                    : applied(initial, new WrapWidget(FIRST, dataPrototype, CHILD, 0));
            assertRejectedUnchanged(withData, new WrapWidget(dataId,
                    WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), WRAPPER), CHILD, 0),
                    DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
        }
    }

    @Test
    void everyBooleanCanBeEditedResetAndSavedInDependencySafeOrderWithOneStepHistory() throws Exception {
        var states = new java.util.ArrayList<DesignerCommandSession>();
        var current = wrap(openDefault());
        states.add(current);
        var maintenance = List.of("maintainState", "maintainAnimation", "maintainSize",
                "maintainSemantics", "maintainInteractivity", "maintainFocusability");
        for (String name : maintenance) {
            current = applied(current, new SetProperty(WRAPPER, new PropertyName(name), new PropertyValue.BooleanValue(true)));
            states.add(current);
            assertTrue(source(current).contains(name + ": true"));
            assertEquals(FIRST, child(current).id());
        }
        current = applied(current, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(false)));
        states.add(current);
        assertExactPair(current, reopen(current));
        for (String name : maintenance.reversed()) {
            current = applied(current, new SetProperty(WRAPPER, new PropertyName(name), new PropertyValue.BooleanValue(false)));
            states.add(current);
            current = applied(current, new ResetProperty(WRAPPER, new PropertyName(name)));
            states.add(current);
        }
        current = applied(current, new ResetProperty(WRAPPER, VISIBLE));
        states.add(current);
        assertTrue(find(current, WRAPPER).properties().isEmpty());
        for (int index = states.size() - 2; index >= 0; index--) {
            current = current.undo().session();
            assertExactPair(states.get(index), current);
        }
        for (int index = 1; index < states.size(); index++) {
            current = current.redo().session();
            assertExactPair(states.get(index), current);
        }
        assertExactPair(current, reopen(current));
    }

    @Test
    void atomicMaintenancePresetAndResetSucceedButInvalidSingleEditsPreservePairAndRedo() throws Exception {
        var current = wrap(openDefault());
        var maintenance = List.of("maintainState", "maintainAnimation", "maintainSize",
                "maintainSemantics", "maintainInteractivity", "maintainFocusability");
        var patches = maintenance.stream().<PatchProperties.Patch>map(name ->
                new PatchProperties.SetPatch(new PropertyName(name), new PropertyValue.BooleanValue(true))).toList();
        var maintained = applied(current, new PatchProperties(WRAPPER, patches));
        assertEquals(current.cursor() + 1, maintained.cursor());
        assertEquals(6, find(maintained, WRAPPER).properties().size());
        assertExactPair(current, maintained.undo().session());
        assertExactPair(maintained, maintained.undo().session().redo().session());
        var withRedo = maintained.undo().session();
        for (String name : maintenance.subList(1, 6)) {
            assertRejectedUnchanged(withRedo, new SetProperty(WRAPPER, new PropertyName(name), new PropertyValue.BooleanValue(true)),
                    DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        }
        for (String name : List.of("maintainState", "maintainAnimation", "maintainSize")) {
            assertRejectedUnchanged(maintained, new ResetProperty(WRAPPER, new PropertyName(name)),
                    DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            assertRejectedUnchanged(maintained, new SetProperty(WRAPPER, new PropertyName(name), new PropertyValue.BooleanValue(false)),
                    DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        }
        var reset = applied(maintained, new PatchProperties(WRAPPER, maintenance.stream().<PatchProperties.Patch>
                map(name -> new PatchProperties.ResetPatch(new PropertyName(name))).toList()));
        assertTrue(find(reset, WRAPPER).properties().isEmpty());
        assertEquals(maintained.cursor() + 1, reset.cursor());
        assertExactPair(maintained, reset.undo().session());
        assertExactPair(reset, reopen(reset));
    }

    @Test
    void replacementCanBeAddedEditedReplacedMovedAndClearedWithoutLosingRequiredChildOrIgnoredBranch() throws Exception {
        var wrapped = wrap(openDefault());
        assertEquals(WidgetSlot.SingleSlot.empty(), find(wrapped, WRAPPER).slots().get(REPLACEMENT));
        assertFalse(source(wrapped).contains("replacement:"));
        var fallback = text(StableId.random(), "Fallback");
        var added = applied(wrapped, new AddWidget(new WidgetPlacement(WRAPPER, REPLACEMENT, 0), fallback));
        assertTrue(source(added).contains("replacement: const Text('Fallback')"));
        var hidden = applied(added, new SetProperty(WRAPPER, VISIBLE, new PropertyValue.BooleanValue(false)));
        hidden = applied(hidden, new SetProperty(fallback.id(), new PropertyName("data"), new PropertyValue.StringValue("Edited fallback")));
        assertEquals(FIRST, child(hidden).id());
        var reopened = reopen(hidden);
        assertExactPair(hidden, reopened);
        var retained = applied(reopened, new SetProperty(WRAPPER, new PropertyName("maintainState"), new PropertyValue.BooleanValue(true)));
        assertTrue(source(retained).contains("replacement: const Text('Edited fallback')"));
        var movedExisting = applied(retained, new ReplaceSlotChild(WRAPPER, REPLACEMENT, fallback.id(),
                new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, ((WidgetSlot.SingleSlot) find(movedExisting, WRAPPER).slots().get(REPLACEMENT)).child().orElseThrow().id());
        assertEquals(List.of(WRAPPER), rootChildren(movedExisting).stream().map(WidgetNode::id).toList());
        assertRejectedUnchanged(movedExisting, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 0)),
                DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var movedAway = applied(movedExisting, new MoveWidget(SECOND, new WidgetPlacement(ROOT, CHILDREN, 1)));
        assertFalse(source(movedAway).contains("replacement:"));
        assertFalse(source(movedAway).contains("replacement: null"));
        assertEquals(FIRST, child(movedAway).id());
        assertExactPair(movedExisting, movedAway.undo().session());
        var cleared = applied(movedExisting, new RemoveWidget(SECOND));
        assertFalse(find(cleared, WRAPPER).slots().containsKey(REPLACEMENT));
        assertFalse(source(cleared).contains("replacement:"));
        assertExactPair(movedExisting, cleared.undo().session());
        assertExactPair(cleared, reopen(cleared));
    }

    private static DesignerCommandSession wrap(DesignerCommandSession current) {
        return applied(current, new WrapWidget(FIRST,
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), WRAPPER), CHILD, 0));
    }
    private static WidgetNode child(DesignerCommandSession session) {
        return ((WidgetSlot.SingleSlot) find(session, WRAPPER).slots().get(CHILD)).child().orElseThrow();
    }
    private static List<WidgetNode> rootChildren(DesignerCommandSession session) {
        return ((WidgetSlot.ListSlot) session.current().document().root().slots().get(CHILDREN)).children();
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
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
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
