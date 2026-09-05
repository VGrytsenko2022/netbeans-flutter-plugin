package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;

class IndexedSemanticsCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e71");
    private static final StableId CHILD = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e72");

    @Test
    void addChildSaveReopenEditRemoveAndUndoRedoPreservePairAndIndexedWidgetIdentity() throws Exception {
        var initial = openDefault();
        var added = applied(initial, new AddWidget(new WidgetPlacement(ROOT, new SlotName("child"), 0),
                text(CHILD, "Preserved child")));
        assertEquals(1, added.cursor());
        assertChildPreserved(added);
        assertExactPair(initial, added.undo().session());
        assertExactPair(added, added.undo().session().redo().session());
        var saved = added.markSaved();
        var reopened = DesignerCommandSession.open(OriginalFdBytes.copyOf(saved.current().fdBytes(),
                FdCodecLimits.defaults()), saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopened.ready(), reopened.diagnostics().toString());
        var current = reopened.session().orElseThrow();
        assertExactPair(saved, current);
        assertFalse(current.dirty());
        assertEquals(0, current.cursor());
        var edited = applied(current, new SetProperty(CHILD, name("data"), new PropertyValue.StringValue("After reopen")));
        assertTrue(source(edited).contains("Text('After reopen')"));
        assertExactPair(current, edited.undo().session());
        var removed = applied(edited, new RemoveWidget(CHILD));
        assertEquals(ROOT, removed.current().document().root().id());
        var emptyChild = removed.current().document().root().slots().get(new SlotName("child"));
        assertTrue(emptyChild == null || ((WidgetSlot.SingleSlot) emptyChild).child().isEmpty());
        assertTrue(source(removed).contains("return const IndexedSemantics("));
        assertFalse(source(removed).contains("After reopen"));
        assertExactPair(edited, removed.undo().session());
        assertExactPair(removed, removed.undo().session().redo().session());
        var replacement = applied(removed, new AddWidget(new WidgetPlacement(ROOT, new SlotName("child"), 0),
                text(StableId.random(), "Replacement")));
        assertTrue(source(replacement).contains("Text('Replacement')"));
    }

    @Test
    void wrappingExistingChildInNestedIndexedWidgetKeepsItsIdentityAndAllowsFurtherEditing() throws Exception {
        var initial = applied(openDefault(), new AddWidget(new WidgetPlacement(ROOT, new SlotName("child"), 0),
                text(CHILD, "Preserved child")));
        var wrapperId = StableId.random();
        var definition = CATALOG.find(new WidgetTypeId("flutter.widgets.IndexedSemantics")).orElseThrow();
        var wrapper = WidgetNodePrototypeFactory.create(definition, wrapperId);
        var wrapped = applied(initial, new WrapWidget(CHILD, wrapper, new SlotName("child"), 0));
        assertTrue(source(wrapped).contains("child: const IndexedSemantics("));
        var nested = ((WidgetSlot.SingleSlot) wrapped.current().document().root().slots()
                .get(new SlotName("child"))).child().orElseThrow();
        assertEquals(wrapperId, nested.id());
        assertEquals(CHILD, ((WidgetSlot.SingleSlot) nested.slots().get(new SlotName("child"))).child().orElseThrow().id());
        assertExactPair(initial, wrapped.undo().session());
        assertExactPair(wrapped, wrapped.undo().session().redo().session());
        var edited = applied(wrapped, new SetProperty(CHILD, name("data"), new PropertyValue.StringValue("Still editable")));
        assertTrue(source(edited).contains("Text('Still editable')"));
    }

    @Test
    void unknownPropertiesAndOccupiedSlotRejectWithoutMutatingHistoryOrBytes() throws Exception {
        var initial = openDefault();
        var changed = applied(initial, new AddWidget(new WidgetPlacement(ROOT, new SlotName("child"), 0),
                text(CHILD, "Preserved child")));
        var current = changed.undo().session();
        assertTrue(current.canRedo());
        for (String invented : List.of("key", "blocking", "excluding", "label", "mergeAllDescendantsIntoThisNode")) {
            assertRejectedUnchanged(current, new SetProperty(ROOT, name(invented), new PropertyValue.BooleanValue(true)),
                    DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN);
        }
        var full = current.redo().session();
        var rejected = full.apply(new AddWidget(new WidgetPlacement(ROOT, new SlotName("child"), 0),
                text(StableId.random(), "Unexpected replacement")));
        assertFalse(rejected.changed());
        assertSame(full, rejected.session());
        assertExactPair(full, rejected.session());
        assertChildPreserved(full);
    }

    @Test
    void signedIndexEditsPersistAcrossSaveReopenAndRejectUnsetWithoutChangingHistory() throws Exception {
        var current = applied(openDefault(), new AddWidget(new WidgetPlacement(ROOT, new SlotName("child"), 0),
                text(CHILD, "Preserved child")));
        for (var index : List.of(dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MIN_PORTABLE_INTEGER,
                java.math.BigInteger.valueOf(-1), java.math.BigInteger.ZERO,
                dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MAX_PORTABLE_INTEGER)) {
            var before = current;
            current = applied(current, new SetProperty(ROOT, name("index"), new PropertyValue.IntegerValue(index)));
            assertEquals(before.cursor() + 1, current.cursor());
            assertTrue(source(current).contains("index: " + index));
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
            assertChildPreserved(current);
        }
        var saved = current.markSaved();
        var reopened = DesignerCommandSession.open(OriginalFdBytes.copyOf(saved.current().fdBytes(),
                FdCodecLimits.defaults()), saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopened.ready(), reopened.diagnostics().toString());
        current = reopened.session().orElseThrow();
        assertExactPair(saved, current);
        current = applied(current, new SetProperty(ROOT, name("index"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-4))));
        assertTrue(source(current).contains("index: -4"));
        var withRedo = current.undo().session();
        assertTrue(withRedo.canRedo());
        assertRejectedUnchanged(withRedo, new ResetProperty(ROOT, name("index")), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(true),
                new PropertyValue.StringValue("0"), new PropertyValue.DoubleValue(java.math.BigDecimal.ONE),
                new PropertyValue.DartExpressionValue("0"),
                new PropertyValue.IntegerValue(dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MIN_PORTABLE_INTEGER.subtract(java.math.BigInteger.ONE)),
                new PropertyValue.IntegerValue(dev.flutter.netbeans.designer.catalog.DartNumericLiterals.MAX_PORTABLE_INTEGER.add(java.math.BigInteger.ONE)))) {
            assertRejectedUnchanged(withRedo, new SetProperty(ROOT, name("index"), invalid), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertChildPreserved(withRedo);
    }

    private static PropertyName name(String value) { return new PropertyName(value); }

    private static WidgetNode text(StableId id, String value) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)), Map.of());
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

    private static void assertChildPreserved(DesignerCommandSession session) {
        var child = ((WidgetSlot.SingleSlot) session.current().document().root().slots()
                .get(new SlotName("child"))).child().orElseThrow();
        assertEquals(CHILD, child.id());
        assertEquals(new PropertyValue.StringValue("Preserved child"), child.properties().get(new PropertyName("data")));
    }

    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }

    private static DesignerCommandSession openDefault() throws Exception {
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.IndexedSemantics"), Map.of(name("index"), new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
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
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
