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

class ExcludeFocusCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ExcludeFocus");
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId WRAPPER = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final PropertyName EXCLUDING = new PropertyName("excluding");

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
        var changed = applied(reopened, new SetProperty(WRAPPER, EXCLUDING, new PropertyValue.BooleanValue(false)));
        changed = applied(changed, new SetProperty(FIRST, new PropertyName("data"), new PropertyValue.StringValue("After reopen")));
        assertTrue(source(changed).contains("excluding: false"));
        assertTrue(source(changed).contains("Text('After reopen')"));
        var finalOpen = reopen(changed);
        assertExactPair(changed, finalOpen);
        assertEquals(FIRST, child(finalOpen).id());
        assertEquals(new PropertyValue.BooleanValue(false), find(finalOpen, WRAPPER).properties().get(EXCLUDING));
    }

    @Test
    void checkboxStatesResetAndOneStepHistoryKeepRequiredChildAndRejectMalformedValues() throws Exception {
        var current = wrap(openDefault());
        var originalChild = child(current);
        for (boolean value : List.of(false, true)) {
            var before = current;
            current = applied(current, new SetProperty(WRAPPER, EXCLUDING, new PropertyValue.BooleanValue(value)));
            assertEquals(before.cursor() + 1, current.cursor());
            assertTrue(source(current).contains("excluding: " + value));
            assertEquals(originalChild, child(current));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        var explicit = current;
        current = applied(current, new ResetProperty(WRAPPER, EXCLUDING));
        assertTrue(find(current, WRAPPER).properties().isEmpty());
        assertFalse(source(current).contains("excluding:"));
        assertEquals(originalChild, child(current));
        assertExactPair(explicit, current.undo().session());
        assertExactPair(current, current.undo().session().redo().session());
        var withRedo = current.undo().session();
        assertTrue(withRedo.canRedo());
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.DoubleValue(BigDecimal.ZERO), new PropertyValue.DartExpressionValue("false"))) {
            assertRejectedUnchanged(withRedo, new SetProperty(WRAPPER, EXCLUDING, invalid),
                    DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String invented : List.of("key", "canRequestFocus", "skipTraversal", "includeSemantics", "descendantsAreFocusable")) {
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
        var wrapped = applied(wrap(openDefault()), new SetProperty(WRAPPER, EXCLUDING, new PropertyValue.BooleanValue(false)));
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
