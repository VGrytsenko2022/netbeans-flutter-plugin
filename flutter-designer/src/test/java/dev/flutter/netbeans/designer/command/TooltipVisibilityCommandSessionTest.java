package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipVisibilityCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE;
    private static final StableId ROOT = StableId.parse("3e6fab05-29da-4841-b536-0974ba8f346b");
    private static final StableId FIRST = StableId.parse("58610b49-c047-4993-86e5-3e3b954157ce");
    private static final StableId SECOND = StableId.parse("d7d3f949-6f50-4759-b457-554c97c39a3f");
    private static final StableId WRAPPER = StableId.parse("3a1ecbdb-2bcd-4d5d-89c8-478ccaa41c9a");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final PropertyName VISIBLE = new PropertyName("visible");

    @Test void wrapBooleanEditSaveReopenAndAllHistoryPreserveChildAndExactSourcePair() throws Exception {
        var states = new ArrayList<DesignerCommandSession>();
        var current = open();
        states.add(current);
        current = applied(current, new WrapWidget(FIRST, prototype(WRAPPER), CHILD, 0));
        states.add(current);
        assertEquals(Map.of(VISIBLE, bool(true)), find(current, WRAPPER).properties());
        assertEquals(FIRST, child(current).id());
        for (boolean value : List.of(false, true)) {
            current = applied(current, new SetProperty(WRAPPER, VISIBLE, bool(value)));
            states.add(current);
            assertTrue(source(current).contains("visible: " + value));
            assertEquals(FIRST, child(current).id());
        }
        current = applied(current, new SetProperty(FIRST, new PropertyName("data"), new PropertyValue.StringValue("Edited anchor")));
        states.add(current);
        assertTrue(source(current).contains("Text('Edited anchor')"));
        assertPair(current, reopen(current));
        for (int i = states.size() - 2; i >= 0; i--) {
            current = current.undo().session();
            assertPair(states.get(i), current);
        }
        assertFalse(current.canUndo());
        for (int i = 1; i < states.size(); i++) {
            current = current.redo().session();
            assertPair(states.get(i), current);
        }
        assertFalse(current.canRedo());
        var reopened = reopen(current);
        var after = applied(reopened, new SetProperty(WRAPPER, VISIBLE, bool(false)));
        assertEquals(FIRST, child(after).id());
        assertPair(after, reopen(after));
    }

    @Test void requiredVisibleAndMalformedEditsRejectWithoutConsumingRedo() throws Exception {
        var wrapped = applied(open(), new WrapWidget(FIRST, prototype(WRAPPER), CHILD, 0));
        var withRedo = applied(wrapped, new SetProperty(WRAPPER, VISIBLE, bool(false))).undo().session();
        assertTrue(withRedo.canRedo());
        rejected(withRedo, new ResetProperty(WRAPPER, VISIBLE), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DartExpressionValue("false"),
                new PropertyValue.CallbackValue("_visible"))) {
            rejected(withRedo, new SetProperty(WRAPPER, VISIBLE, invalid), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        for (String unknown : List.of("onChanged", "onTriggered", "enabled", "key", "merge")) {
            rejected(withRedo, new SetProperty(WRAPPER, new PropertyName(unknown), bool(true)),
                    DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN);
        }
        assertEquals(FIRST, child(withRedo).id());
    }

    @Test void requiredChildRemovalMoveAndIncompleteInsertionRejectAtomicallyWhileReplacementWorks() throws Exception {
        var wrapped = applied(open(), new WrapWidget(FIRST, prototype(WRAPPER), CHILD, 0));
        rejected(wrapped, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        rejected(wrapped, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 1)), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        rejected(wrapped, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), prototype(StableId.random())),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        rejected(wrapped, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(prototype(StableId.random()))), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var replacement = text(StableId.random(), "Replacement");
        var replaced = applied(wrapped, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(replacement, child(replaced));
        assertPair(wrapped, replaced.undo().session());
        assertPair(replaced, reopen(replaced));
        var moved = applied(replaced, new MoveWidget(WRAPPER, new WidgetPlacement(ROOT, CHILDREN, 1)));
        assertEquals(List.of(SECOND, WRAPPER), ((WidgetSlot.ListSlot) moved.current().document().root().slots().get(CHILDREN))
                .children().stream().map(WidgetNode::id).toList());
        assertEquals(replacement, child(moved));
        assertPair(replaced, moved.undo().session());
    }

    @Test void rootAndNestedWrappingRetainIndependentNearestScopeFlags() throws Exception {
        var initial = open();
        var outer = applied(initial, new WrapWidget(ROOT, prototype(WRAPPER), CHILD, 0));
        outer = applied(outer, new SetProperty(WRAPPER, VISIBLE, bool(false)));
        var innerId = StableId.random();
        var inner = applied(outer, new WrapWidget(FIRST, prototype(innerId), CHILD, 0));
        assertEquals(ROOT, child(inner).id());
        assertEquals(Map.of(VISIBLE, bool(false)), find(inner, WRAPPER).properties());
        assertEquals(Map.of(VISIBLE, bool(true)), find(inner, innerId).properties());
        assertEquals(2, source(inner).split("TooltipVisibility\\(", -1).length - 1);
        assertFalse(source(inner).contains("&&"));
        assertPair(outer, inner.undo().session());
        assertPair(inner, reopen(inner));
    }

    @Test void wrappingFlexParentDataChildrenRejectsInsteadOfBreakingTheirAncestry() throws Exception {
        for (String type : List.of("Expanded", "Flexible", "Spacer")) {
            var id = StableId.random();
            var data = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets." + type)).orElseThrow(), id);
            var current = type.equals("Spacer")
                    ? applied(open(), new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), data))
                    : applied(open(), new WrapWidget(FIRST, data, CHILD, 0));
            rejected(current, new WrapWidget(id, prototype(WRAPPER), CHILD, 0),
                    DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
        }
    }

    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static WidgetNode prototype(StableId id) { return WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), id); }
    private static WidgetNode text(StableId id, String value) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static WidgetNode child(DesignerCommandSession session) {
        return ((WidgetSlot.SingleSlot) find(session, WRAPPER).slots().get(CHILD)).child().orElseThrow();
    }
    private static WidgetNode find(DesignerCommandSession session, StableId id) {
        return find(session.current().document().root(), id).orElseThrow();
    }
    private static Optional<WidgetNode> find(WidgetNode node, StableId id) {
        if (node.id().equals(id)) return Optional.of(node);
        for (var slot : node.slots().values()) {
            var children = slot instanceof WidgetSlot.SingleSlot single ? single.child().stream().toList()
                    : ((WidgetSlot.ListSlot) slot).children();
            for (var child : children) {
                var result = find(child, id);
                if (result.isPresent()) return result;
            }
        }
        return Optional.empty();
    }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static void rejected(DesignerCommandSession session, DesignerCommand command, DesignerCommandDiagnosticCode code) {
        var result = session.apply(command);
        assertFalse(result.changed());
        assertSame(session, result.session());
        assertEquals(code, result.diagnostics().getFirst().code(), result.diagnostics().toString());
        assertEquals(session.cursor(), result.session().cursor());
        assertEquals(session.canRedo(), result.session().canRedo());
        assertPair(session, result.session());
    }
    private static void assertPair(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
        assertEquals(expected.current().document(), actual.current().document());
    }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static DesignerCommandSession reopen(DesignerCommandSession session) throws Exception {
        var saved = session.markSaved();
        var result = DesignerCommandSession.open(OriginalFdBytes.copyOf(saved.current().fdBytes(), FdCodecLimits.defaults()),
                saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DesignerCommandSession open() throws Exception {
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(text(FIRST, "First"), text(SECOND, "Second")))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root);
        var generated = new DartRegionGenerator().generate(provisional, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), root);
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), dart, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
