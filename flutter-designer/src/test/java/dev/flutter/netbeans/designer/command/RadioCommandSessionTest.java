package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.RadioTestValues.*;

class RadioCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = RadioWidgetPropertySchema.RADIO_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void creationMoveRemovePreserveStableNeighborsAndEveryHistoryEndpoint() throws Exception {
        var initial = openDefault();
        var added = add(initial);
        assertEquals(Map.of(p("value"), s("option"), p("valueType"), s("String"), p("variant"), s("standard"), p("onChanged"), s("noop")), find(added, BUTTON).properties());
        assertExactPair(initial, added.undo().session());
        var moved = applied(added, new MoveWidget(BUTTON, new WidgetPlacement(ROOT, CHILDREN, 0)));
        assertExactPair(added, moved.undo().session());
        assertExactPair(moved, reopen(moved));
        var removed = applied(moved, new RemoveWidget(BUTTON));
        assertExactPair(moved, removed.undo().session());
        assertEquals(FIRST, find(removed, FIRST).id());
        assertEquals(SECOND, find(removed, SECOND).id());
    }

    @Test
    void dense107RowsBothConstructorsAndWholeFamiliesSaveReopenExactly() throws Exception {
        var initial = add(openDefault());
        Set<PropertyName> covered = new HashSet<>();
        for (String variant : RadioWidgetPropertySchema.variants()) {
            var values = full(variant);
            covered.addAll(values.keySet());
            var dense = applied(initial, patch(values));
            assertExactPair(initial, dense.undo().session());
            assertExactPair(dense, dense.undo().session().redo().session());
            assertExactPair(dense, reopen(dense));
        }
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius", "side", "visualDensity")) {
            covered.add(p(family));
            var whole = applied(initial, new SetProperty(BUTTON, p(family), reference(family)));
            assertExactPair(initial, whole.undo().session());
            assertExactPair(whole, reopen(whole));
        }
        assertEquals(107, covered.size());
    }

    @Test
    void fourFieldTypeEditIsAtomicAndInvalidIntermediateStateKeepsExactCursor() throws Exception {
        var initial = add(openDefault());
        assertRejectedUnchanged(initial, new SetProperty(BUTTON, p("valueType"), s("bool")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var changed = applied(initial, patch(Map.of(p("valueType"), s("bool"), p("value"), b(true), p("groupValue"), nil(), p("nullableValueType"), b(false))));
        assertTrue(source(changed).contains("Radio<bool>"));
        assertExactPair(initial, changed.undo().session());
        assertExactPair(changed, reopen(changed));
        var nullable = applied(changed, patch(Map.of(p("nullableValueType"), b(true), p("value"), nil())));
        assertTrue(source(nullable).contains("Radio<bool?>"));
        assertRejectedUnchanged(nullable, new ResetProperty(BUTTON, p("nullableValueType")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(changed, nullable.undo().session());
        assertExactPair(nullable, reopen(nullable));
        for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            var numeric = applied(initial, patch(Map.of(p("valueType"), s("double"), p("value"), new PropertyValue.EnumValue("double", member))));
            assertExactPair(initial, numeric.undo().session());
            assertExactPair(numeric, reopen(numeric));
        }
    }

    @Test
    void wholeAndLocalStyleFamiliesSwitchWithoutLossAndInheritedNullIsDistinct() throws Exception {
        var initial = add(openDefault());
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius")) {
            var whole = applied(initial, new SetProperty(BUTTON, p(family), reference(family)));
            assertRejectedUnchanged(whole, new SetProperty(BUTTON, p(family + "Default"), nil()), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
            var local = applied(whole, new PatchProperties(BUTTON, List.of(new PatchProperties.ResetPatch(p(family)),
                    new PatchProperties.SetPatch(p(family + "Default"), family.equals("innerRadius") ? d("4.5") : color()),
                    new PatchProperties.SetPatch(p(family + "Pressed"), nil()))));
            assertExactPair(whole, local.undo().session());
            assertExactPair(local, reopen(local));
        }
        var border = applied(initial, patch(Map.of(p("sideStateful"), b(true), p("sidePressedMode"), s("border"), p("sidePressedWidth"), d("2"))));
        assertRejectedUnchanged(border, new SetProperty(BUTTON, p("sideStateful"), b(false)), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var inherit = applied(border, new PatchProperties(BUTTON, List.of(new PatchProperties.ResetPatch(p("sidePressedWidth")), new PatchProperties.SetPatch(p("sidePressedMode"), s("inherit")))));
        assertTrue(source(inherit).contains("WidgetState.pressed: null"));
        assertExactPair(border, inherit.undo().session());
        assertExactPair(inherit, reopen(inherit));
    }

    @Test
    void nullableSdkEnabledRetainsCallbacksAndNoopNullOmissionAreSeparateSourceHistories() throws Exception {
        var initial = add(openDefault());
        var disabled = applied(initial, patch(Map.of(p("enabled"), b(false), p("onChanged"), reference("first"))));
        var second = applied(disabled, new SetProperty(BUTTON, p("onChanged"), reference("second")));
        assertFalse(Arrays.equals(disabled.current().dartCandidateBytes(), second.current().dartCandidateBytes()));
        assertTrue(source(second).contains("second"));
        assertTrue(source(second).contains("enabled: false"));
        assertExactPair(disabled, second.undo().session());
        var explicitNull = applied(second, new SetProperty(BUTTON, p("onChanged"), nil()));
        assertTrue(source(explicitNull).contains("onChanged: null"));
        var omitted = applied(explicitNull, new ResetProperty(BUTTON, p("onChanged")));
        assertFalse(source(omitted).contains("onChanged:"));
        assertExactPair(explicitNull, omitted.undo().session());
        assertExactPair(omitted, reopen(omitted));
        var adaptive = applied(omitted, patch(Map.of(p("variant"), s("adaptive"), p("useCupertinoCheckmarkStyle"), b(false))));
        assertRejectedUnchanged(adaptive, new SetProperty(BUTTON, p("variant"), s("standard")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var standard = applied(adaptive, new PatchProperties(BUTTON, List.of(new PatchProperties.ResetPatch(p("useCupertinoCheckmarkStyle")), new PatchProperties.SetPatch(p("variant"), s("standard")))));
        assertExactPair(adaptive, standard.undo().session());
    }

    @Test
    void rejectedRequiredRawNumericAndTypeReferencesPreserveUndoRedoAndDiskCandidate() throws Exception {
        var initial = add(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("groupValue"), s("option")));
        var undone = changed.undo().session();
        for (String name : List.of("value", "valueType", "variant")) assertRejectedUnchanged(undone, new ResetProperty(BUTTON, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        for (var command : List.of(new SetProperty(BUTTON, p("value"), d("1e400")),
                new SetProperty(BUTTON, p("onChanged"), new PropertyValue.CallbackValue("legacy")),
                new SetProperty(BUTTON, p("value"), new PropertyValue.DartExpressionValue("unsafe()")))) {
            assertRejectedUnchanged(undone, command, DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        var invalidType = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.of("first"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        assertRejectedUnchanged(undone, new SetProperty(BUTTON, p("valueType"), invalidType), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
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
