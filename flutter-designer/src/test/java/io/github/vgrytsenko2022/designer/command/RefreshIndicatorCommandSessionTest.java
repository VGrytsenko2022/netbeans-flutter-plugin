package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefreshIndicatorCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId WRAPPER = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");

    @Test
    void requiredChildWrapperAndAllCompatibleFieldsRetainExactHistoryAndSaveReopen() throws Exception {
        var initial = openDefault();
        var current = wrap(initial);
        assertEquals(FIRST, child(current).id());
        assertTrue(source(current).contains("onRefresh: () async {}"));
        assertEquals(Map.of(p("variant"), s("material")), find(current, WRAPPER).properties());
        assertExactPair(initial, current.undo().session());
        for (var entry : values().entrySet()) {
            var before = current;
            current = applied(current, new SetProperty(WRAPPER, entry.getKey(), entry.getValue()));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        assertEquals(12, find(current, WRAPPER).properties().size());
        current = reopen(current);
        for (var name : new ArrayList<>(find(current, WRAPPER).properties().keySet())) {
            if (name.value().equals("variant")) continue;
            var before = current;
            current = applied(current, new ResetProperty(WRAPPER, name));
            assertExactPair(before, current.undo().session());
        }
        assertEquals(Map.of(p("variant"), s("material")), find(current, WRAPPER).properties());
        assertTrue(source(current).contains("onRefresh: () async {}"));
        assertEquals(FIRST, child(current).id());
        assertExactPair(current, reopen(current));
    }

    @Test
    void allNineConstructorTransitionsPreserveSharedFieldsChildAndOneHistoryEdge() throws Exception {
        for (String from : List.of("material", "adaptive", "noSpinner")) for (String to : List.of("material", "adaptive", "noSpinner")) {
            var current = wrap(openDefault());
            current = applied(current, new SetProperty(WRAPPER, p("semanticsLabel"), s("Shared")));
            if (!from.equals("material")) current = applied(current, new SetProperty(WRAPPER, p("variant"), s(from)));
            if (from.equals("noSpinner")) current = applied(current, new SetProperty(WRAPPER, p("onStatusChange"), reference()));
            else current = applied(current, new SetProperty(WRAPPER, p("color"), new PropertyValue.ColorValue(0x80123456L)));
            var patches = new ArrayList<PatchProperties.Patch>();
            patches.add(new PatchProperties.SetPatch(p("variant"), s(to)));
            if (to.equals("noSpinner")) for (String name : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties())
                patches.add(new PatchProperties.ResetPatch(p(name)));
            else patches.add(new PatchProperties.ResetPatch(p("onStatusChange")));
            var result = current.apply(new PatchProperties(WRAPPER, patches));
            assertFalse(result.diagnostics().stream().anyMatch(d -> d.code() == DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID));
            var transitioned = result.session();
            assertEquals(s(to), find(transitioned, WRAPPER).properties().get(p("variant")));
            assertEquals(s("Shared"), find(transitioned, WRAPPER).properties().get(p("semanticsLabel")));
            assertEquals(FIRST, child(transitioned).id());
            if (result.changed()) {
                assertEquals(current.cursor() + 1, transitioned.cursor());
                assertExactPair(current, transitioned.undo().session());
                assertExactPair(transitioned, transitioned.undo().session().redo().session());
            }
            assertExactPair(transitioned, reopen(transitioned));
        }
    }

    @Test
    void branchConflictsAndRequiredModeResetRejectWholePatchAndRetainRedo() throws Exception {
        var base = wrap(openDefault());
        var colored = applied(base, new SetProperty(WRAPPER, p("color"), new PropertyValue.ColorValue(0xff123456L)));
        assertRejectedUnchanged(colored, new SetProperty(WRAPPER, p("variant"), s("noSpinner")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(base, new SetProperty(WRAPPER, p("onStatusChange"), reference()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var withRedo = colored.undo().session();
        assertTrue(withRedo.canRedo());
        assertRejectedUnchanged(withRedo, new ResetProperty(WRAPPER, p("variant")), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        assertRejectedUnchanged(withRedo, new PatchProperties(WRAPPER, List.of(
                new PatchProperties.SetPatch(p("edgeOffset"), d("-2")),
                new PatchProperties.SetPatch(p("elevation"), d("-1")))), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertExactPair(colored, withRedo.redo().session());
    }

    @Test
    void requiredChildCannotBeDeletedMovedAwayOrReplacedWithDetachedRequiredWrapper() throws Exception {
        var wrapped = wrap(openDefault());
        assertRejectedUnchanged(wrapped, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertRejectedUnchanged(wrapped, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 1)), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var empty = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
        assertRejectedUnchanged(wrapped, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 2), empty), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(wrapped, new ReplaceSlotChild(WRAPPER, CHILD, FIRST,
                new ReplaceSlotChild.NewSubtree(empty)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var replacement = text(StableId.random(), "Replacement");
        var replaced = applied(wrapped, new ReplaceSlotChild(WRAPPER, CHILD, FIRST, new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(replacement, child(replaced));
        assertExactPair(wrapped, replaced.undo().session());
        var moved = applied(replaced, new ReplaceSlotChild(WRAPPER, CHILD, replacement.id(), new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, child(moved).id());
        assertExactPair(replaced, moved.undo().session());
        assertExactPair(moved, reopen(moved));
    }

    @Test
    void movingNestingAndRemovingTheWholeWrapperPreservesStableChildIdentity() throws Exception {
        var wrapped = wrap(openDefault());
        var moved = applied(wrapped, new MoveWidget(WRAPPER, new WidgetPlacement(ROOT, CHILDREN, 1)));
        assertEquals(FIRST, child(moved).id());
        var nested = applied(moved, new WrapWidget(FIRST,
                WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random()), CHILD, 0));
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) child(nested).slots().get(CHILD)).child().orElseThrow().id());
        assertExactPair(moved, nested.undo().session());
        var removed = applied(nested, new RemoveWidget(WRAPPER));
        assertExactPair(nested, removed.undo().session());
        assertExactPair(nested, reopen(nested));
    }

    @Test
    void callbackPredicateModesRoundTripWithoutExecutingReferencesOrLosingResetBehavior() throws Exception {
        for (String name : List.of("onRefresh", "notificationPredicate", "onStatusChange")) {
            var base = wrap(openDefault());
            if (name.equals("onStatusChange")) base = applied(base, new SetProperty(WRAPPER, p("variant"), s("noSpinner")));
            var configured = applied(base, new SetProperty(WRAPPER, p(name), reference()));
            assertExactPair(base, configured.undo().session());
            assertExactPair(configured, configured.undo().session().redo().session());
            var restored = reopen(configured);
            assertEquals(reference(), find(restored, WRAPPER).properties().get(p(name)));
            var reset = applied(restored, new ResetProperty(WRAPPER, p(name)));
            assertFalse(find(reset, WRAPPER).properties().containsKey(p(name)));
            assertExactPair(base, reset);
        }
        var current = wrap(openDefault());
        for (String preset : RefreshIndicatorWidgetPropertySchema.notificationPredicatePresets()) {
            current = applied(current, new SetProperty(WRAPPER, p("notificationPredicate"), s(preset)));
            current = reopen(current);
            assertEquals(s(preset), find(current, WRAPPER).properties().get(p("notificationPredicate")));
        }
    }

    @Test
    void nullRawAndUnsupportedFieldsRejectBeforePairBytesOrCursorChange() throws Exception {
        var base = wrap(openDefault());
        for (String name : RefreshIndicatorWidgetPropertySchema.definitions().keySet())
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.DartExpressionValue("null")))
                assertRejectedUnchanged(base, new SetProperty(WRAPPER, p(name), value), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        for (String name : List.of("controller", "valueColor", "year2023", "indicatorPadding")) {
            var result = base.apply(new SetProperty(WRAPPER, p(name), d("1")));
            assertFalse(result.changed());
            assertSame(base, result.session());
            assertExactPair(base, result.session());
        }
    }

    private static LinkedHashMap<PropertyName, PropertyValue> values() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("displacement"), d("20"));
        values.put(p("edgeOffset"), d("-2"));
        values.put(p("onRefresh"), reference());
        values.put(p("color"), new PropertyValue.ColorValue(0xff123456L));
        values.put(p("backgroundColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
        values.put(p("notificationPredicate"), s("all"));
        values.put(p("semanticsLabel"), s("Reload"));
        values.put(p("semanticsValue"), s("45%"));
        values.put(p("strokeWidth"), d("-2.5"));
        values.put(p("triggerMode"), new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", "anywhere"));
        values.put(p("elevation"), d("0"));
        return values;
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:refresh/handlers.dart"), "refreshCallbacks", Optional.of("refresh"),
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
