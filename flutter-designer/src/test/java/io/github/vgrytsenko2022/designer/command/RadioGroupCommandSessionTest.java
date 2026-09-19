package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.RadioTestValues.*;

class RadioGroupCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void wrappingMovingAndRemovingRetainChildIdentityNeighborsAndExactHistory() throws Exception {
        var initial = openDefault();
        var wrapped = wrap(initial);
        assertEquals(Map.of(p("valueType"), s("String"), p("onChanged"), s("noop")), find(wrapped, BUTTON).properties());
        assertEquals(FIRST, child(wrapped).id());
        assertEquals("First", ((PropertyValue.StringValue) find(wrapped, FIRST).properties().get(p("data"))).value());
        assertExactPair(initial, wrapped.undo().session());
        assertExactPair(wrapped, wrapped.undo().session().redo().session());
        var moved = applied(wrapped, new MoveWidget(BUTTON, new WidgetPlacement(ROOT, CHILDREN, 1)));
        assertExactPair(wrapped, moved.undo().session());
        assertExactPair(moved, reopen(moved));
        var removed = applied(moved, new RemoveWidget(BUTTON));
        assertTrue(find(removed.current().document().root(), FIRST).isEmpty());
        assertEquals(SECOND, find(removed, SECOND).id());
        assertExactPair(moved, removed.undo().session());
    }

    @Test
    void threeFieldTypeEditIsAtomicAndNullableSelectionNeverCoercesAChild() throws Exception {
        var initial = wrap(openDefault());
        var string = applied(initial, new SetProperty(BUTTON, p("groupValue"), s("option")));
        assertRejectedUnchanged(string, new SetProperty(BUTTON, p("valueType"), s("int")), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var integer = applied(string, patch(Map.of(p("valueType"), s("int"), p("nullableValueType"), b(false), p("groupValue"), i(7))));
        assertTrue(source(integer).contains("RadioGroup<int>"));
        assertEquals(FIRST, child(integer).id());
        assertExactPair(string, integer.undo().session());
        var nullable = applied(integer, patch(Map.of(p("nullableValueType"), b(true), p("groupValue"), nil())));
        assertTrue(source(nullable).contains("RadioGroup<int?>"));
        assertExactPair(integer, nullable.undo().session());
        assertExactPair(nullable, reopen(nullable));
        var noSelection = applied(nullable, new ResetProperty(BUTTON, p("groupValue")));
        assertFalse(source(noSelection).contains("groupValue:"));
        assertExactPair(nullable, noSelection.undo().session());
        assertExactPair(noSelection, reopen(noSelection));
    }

    @Test
    void specialIdentitiesAndProjectTypeValueCallbackReferencesRoundTripAllEndpoints() throws Exception {
        var initial = wrap(openDefault());
        for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            var changed = applied(initial, patch(Map.of(p("valueType"), s("double"), p("groupValue"), new PropertyValue.EnumValue("double", member))));
            assertExactPair(initial, changed.undo().session());
            assertExactPair(changed, reopen(changed));
        }
        var type = new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_types/types.dart"), "Choice",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var project = applied(initial, patch(Map.of(p("valueType"), type, p("groupValue"), reference("selectedChoice"),
                p("onChanged"), reference("changed"), p("nullableValueType"), b(true))));
        assertTrue(source(project).contains("Choice?"));
        assertEquals(FIRST, child(project).id());
        assertExactPair(initial, project.undo().session());
        assertExactPair(project, reopen(project));
        var anotherCallback = applied(project, new SetProperty(BUTTON, p("onChanged"), reference("otherChanged")));
        assertFalse(Arrays.equals(project.current().dartCandidateBytes(), anotherCallback.current().dartCandidateBytes()));
        assertExactPair(project, anotherCallback.undo().session());
        assertExactPair(anotherCallback, reopen(anotherCallback));
    }

    @Test
    void requiredChildRemovalMoveAndEmptyPrototypeRejectButAtomicReplacementPreservesUndo() throws Exception {
        var initial = openDefault();
        var prototype = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), BUTTON);
        var invalid = initial.apply(new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 0), prototype));
        assertFalse(invalid.changed());
        assertSame(initial, invalid.session());
        var wrapped = wrap(initial);
        assertRejectedUnchanged(wrapped, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertRejectedUnchanged(wrapped, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 0)), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var replaced = applied(wrapped, new ReplaceSlotChild(BUTTON, CHILD, FIRST, new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, child(replaced).id());
        assertExactPair(wrapped, replaced.undo().session());
        assertExactPair(replaced, reopen(replaced));
    }

    @Test
    void invalidRequiredCallbacksTypesAndUnknownFieldsCannotDamageRedoOrSourceBytes() throws Exception {
        var initial = wrap(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("groupValue"), s("option")));
        var undone = changed.undo().session();
        for (String name : List.of("valueType", "onChanged")) {
            assertRejectedUnchanged(undone, new ResetProperty(BUTTON, p(name)), DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        }
        for (PropertyValue value : List.of(nil(), new PropertyValue.CallbackValue("legacy"), s("notNoop"), new PropertyValue.DartExpressionValue("unsafe()"))) {
            assertRejectedUnchanged(undone, new SetProperty(BUTTON, p("onChanged"), value), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertRejectedUnchanged(undone, new SetProperty(BUTTON, p("groupValue"), d("1e400")), DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        var type = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.of("first"),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        assertRejectedUnchanged(undone, new SetProperty(BUTTON, p("valueType"), type), DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(changed, undone.redo().session());
    }

    private static DesignerCommandSession wrap(DesignerCommandSession initial) {
        var definition = CATALOG.find(TYPE).orElseThrow();
        var slot = WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition).orElseThrow();
        return applied(initial, new WrapWidget(FIRST, WidgetNodePrototypeFactory.create(definition, BUTTON), slot.name(), 0));
    }
    private static WidgetNode child(DesignerCommandSession session) {
        return ((WidgetSlot.SingleSlot) find(session, BUTTON).slots().get(CHILD)).child().orElseThrow();
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
