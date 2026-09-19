package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.OriginalFdBytes;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultTextHeightBehaviorCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior");
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId WRAPPER = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final PropertyName FIRST_ASCENT = new PropertyName("textHeightApplyFirstAscent");
    private static final PropertyName LAST_DESCENT = new PropertyName("textHeightApplyLastDescent");
    private static final PropertyName LEADING = new PropertyName("textHeightLeadingDistribution");

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
        var changed = applied(reopened, new SetProperty(WRAPPER, FIRST_ASCENT, new PropertyValue.BooleanValue(false)));
        changed = applied(changed, new SetProperty(FIRST, new PropertyName("data"), new PropertyValue.StringValue("After reopen")));
        assertTrue(source(changed).contains("applyHeightToFirstAscent: false"));
        assertTrue(source(changed).contains("Text('After reopen')"));
        var finalOpen = reopen(changed);
        assertExactPair(changed, finalOpen);
        assertEquals(FIRST, child(finalOpen).id());
        assertEquals(new PropertyValue.BooleanValue(false), find(finalOpen, WRAPPER).properties().get(FIRST_ASCENT));
    }

    @Test
    void checkboxStatesResetAndOneStepHistoryKeepRequiredChildAndRejectMalformedValues() throws Exception {
        var current = wrap(openDefault());
        var originalChild = child(current);
        for (boolean value : List.of(false, true)) {
            var before = current;
            current = applied(current, new SetProperty(WRAPPER, FIRST_ASCENT, new PropertyValue.BooleanValue(value)));
            assertEquals(before.cursor() + 1, current.cursor());
            assertTrue(source(current).contains("applyHeightToFirstAscent: " + value));
            assertEquals(originalChild, child(current));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        var explicit = current;
        current = applied(current, new ResetProperty(WRAPPER, FIRST_ASCENT));
        assertTrue(find(current, WRAPPER).properties().isEmpty());
        assertFalse(source(current).contains("applyHeightToFirstAscent:"));
        assertEquals(originalChild, child(current));
        assertExactPair(explicit, current.undo().session());
        assertExactPair(current, current.undo().session().redo().session());
        var withRedo = current.undo().session();
        assertTrue(withRedo.canRedo());
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.DoubleValue(BigDecimal.ZERO), new PropertyValue.DartExpressionValue("false"))) {
            assertRejectedUnchanged(withRedo, new SetProperty(WRAPPER, FIRST_ASCENT, invalid),
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
        var wrapped = applied(wrap(openDefault()), new SetProperty(WRAPPER, FIRST_ASCENT, new PropertyValue.BooleanValue(false)));
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
        current = applied(current, new SetProperty(WRAPPER, FIRST_ASCENT, new PropertyValue.BooleanValue(false)));
        states.add(current);
        current = applied(current, new SetProperty(WRAPPER, FIRST_ASCENT, new PropertyValue.BooleanValue(true)));
        states.add(current);
        current = applied(current, new ResetProperty(WRAPPER, FIRST_ASCENT));
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
    void rootWrappingAndMixedHeightFocusNestingKeepIndependentDefaultsAndStableIds() throws Exception {
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
        mixed = applied(mixed, new SetProperty(WRAPPER, FIRST_ASCENT, new PropertyValue.BooleanValue(false)));
        assertEquals(Map.of(FIRST_ASCENT, new PropertyValue.BooleanValue(false)), find(mixed, WRAPPER).properties());
        assertEquals(Map.of(), find(mixed, focusId).properties());
        assertTrue(source(mixed).contains("DefaultTextHeightBehavior("));
        assertTrue(source(mixed).contains("ExcludeFocus("));
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) find(mixed, focusId).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(mixed, reopen(mixed));
    }

    @Test
    void cannotInsertHeightBehaviorWrapperBetweenFlexParentAndItsParentDataChild() throws Exception {
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
    void allThreeLeavesSupportAtomicSetResetSaveReopenAndExactHistoryWhileRequiredCompositePersists() throws Exception {
        var initial = wrap(openDefault());
        assertTrue(source(initial).contains("textHeightBehavior: const TextHeightBehavior()"));
        var patch = applied(initial, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.SetPatch(FIRST_ASCENT, new PropertyValue.BooleanValue(false)),
                new PatchProperties.SetPatch(LAST_DESCENT, new PropertyValue.BooleanValue(false)),
                new PatchProperties.SetPatch(LEADING, new PropertyValue.EnumValue("TextLeadingDistribution", "even")))));
        assertEquals(initial.cursor() + 1, patch.cursor());
        assertTrue(source(patch).contains("applyHeightToFirstAscent: false"));
        assertTrue(source(patch).contains("applyHeightToLastDescent: false"));
        assertTrue(source(patch).contains("leadingDistribution: TextLeadingDistribution.even"));
        assertExactPair(initial, patch.undo().session());
        assertExactPair(patch, patch.undo().session().redo().session());
        var reopened = reopen(patch);
        assertExactPair(patch, reopened);
        var changed = applied(reopened, new SetProperty(WRAPPER, LAST_DESCENT, new PropertyValue.BooleanValue(true)));
        changed = applied(changed, new SetProperty(WRAPPER, LEADING, new PropertyValue.EnumValue("TextLeadingDistribution", "proportional")));
        changed = applied(changed, new SetProperty(FIRST, new PropertyName("data"), new PropertyValue.StringValue("Height child after reopen")));
        assertTrue(source(changed).contains("applyHeightToLastDescent: true"));
        assertTrue(source(changed).contains("leadingDistribution: TextLeadingDistribution.proportional"));
        assertExactPair(changed, reopen(changed));
        var reset = applied(changed, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.ResetPatch(FIRST_ASCENT), new PatchProperties.ResetPatch(LAST_DESCENT),
                new PatchProperties.ResetPatch(LEADING))));
        assertTrue(find(reset, WRAPPER).properties().isEmpty());
        assertTrue(source(reset).contains("textHeightBehavior: const TextHeightBehavior()"));
        assertFalse(source(reset).contains("textHeightBehavior: null"));
        assertEquals(FIRST, child(reset).id());
        assertTrue(source(reset).contains("Height child after reopen"));
        assertExactPair(changed, reset.undo().session());
        assertExactPair(reset, reset.undo().session().redo().session());
        assertExactPair(reset, reopen(reset));
    }

    @Test
    void enumFailuresRejectWholePatchWithoutDestroyingRedoOrIndependentChildTextBehavior() throws Exception {
        var initial = wrap(openDefault());
        var changed = applied(initial, new SetProperty(WRAPPER, LEADING, new PropertyValue.EnumValue("TextLeadingDistribution", "even")));
        var withRedo = changed.undo().session();
        for (PropertyValue invalid : List.of(new PropertyValue.EnumValue("TextLeadingDistribution", "invalid"),
                new PropertyValue.EnumValue("OtherLeading", "even"), new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true))) {
            assertRejectedUnchanged(withRedo, new PatchProperties(WRAPPER, List.of(
                    new PatchProperties.SetPatch(FIRST_ASCENT, new PropertyValue.BooleanValue(false)),
                    new PatchProperties.SetPatch(LEADING, invalid))), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        var childOverride = applied(initial, new SetProperty(FIRST, LAST_DESCENT, new PropertyValue.BooleanValue(false)));
        assertEquals(2, source(childOverride).split("textHeightBehavior:", -1).length - 1);
        var childReset = applied(childOverride, new ResetProperty(FIRST, LAST_DESCENT));
        assertEquals(1, source(childReset).split("textHeightBehavior:", -1).length - 1);
        assertTrue(source(childReset).contains("textHeightBehavior: const TextHeightBehavior()"));
        assertExactPair(childOverride, childReset.undo().session());
        assertExactPair(childReset, reopen(childReset));
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
