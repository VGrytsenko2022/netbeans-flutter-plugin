package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.IconButtonTestValues.*;

class IconButtonCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE;
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BUTTON = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName ICON = new SlotName("icon"), SELECTED = new SlotName("selectedIcon"),
            CHILDREN = new SlotName("children");

    @Test
    void requiredIconWrapIsAtomicAndAllFourConstructorsRetainBothChildren() throws Exception {
        var initial = openDefault();
        var wrapped = wrap(initial);
        assertExactPair(initial, wrapped.undo().session());
        assertExactPair(wrapped, wrapped.undo().session().redo().session());
        assertEquals(FIRST, ((WidgetSlot.SingleSlot) find(wrapped, BUTTON).slots().get(ICON)).child().orElseThrow().id());
        assertRejectedUnchanged(wrapped, new RemoveWidget(FIRST), DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        var selected = applied(wrapped, new MoveWidget(SECOND, new WidgetPlacement(BUTTON, SELECTED, 0)));
        for (String variant : IconButtonWidgetPropertySchema.variants()) {
            var changed = variant.equals("standard") ? selected : applied(selected, new SetProperty(BUTTON, p("variant"), s(variant)));
            assertEquals(FIRST, ((WidgetSlot.SingleSlot) find(changed, BUTTON).slots().get(ICON)).child().orElseThrow().id());
            assertEquals(SECOND, ((WidgetSlot.SingleSlot) find(changed, BUTTON).slots().get(SELECTED)).child().orElseThrow().id());
            assertExactPair(changed, reopen(changed));
            var removed = applied(changed, new RemoveWidget(SECOND));
            assertExactPair(changed, removed.undo().session());
        }
    }

    @Test
    void dense505StylesAllConstructorsHaveOneStepHistoryAndExactSaveResetReopen() throws Exception {
        for (String variant : IconButtonWidgetPropertySchema.variants()) for (boolean paints : List.of(false, true)) {
            var initial = wrap(openDefault());
            var values = full(variant, paints, false);
            assertEquals(505, values.size());
            var configured = applied(initial, new PatchProperties(BUTTON, values.entrySet().stream()
                    .map(e -> (PatchProperties.Patch) new PatchProperties.SetPatch(e.getKey(), e.getValue())).toList()));
            assertEquals(initial.cursor() + 1, configured.cursor());
            assertExactPair(initial, configured.undo().session());
            assertExactPair(configured, configured.undo().session().redo().session());
            var reopened = reopen(configured);
            assertExactPair(configured, reopened);
            for (String name : List.of("enabled", "variant")) {
                var result = reopened.apply(new ResetProperty(BUTTON, p(name)));
                assertFalse(result.changed());
                assertSame(reopened, result.session());
            }
            var reset = applied(reopened, new PatchProperties(BUTTON, values.keySet().stream()
                    .filter(n -> !Set.of("variant", "enabled").contains(n.value()))
                    .map(n -> (PatchProperties.Patch) new PatchProperties.ResetPatch(n)).toList()));
            assertEquals(2, find(reset, BUTTON).properties().size());
            assertExactPair(reset, reopen(reset));
            assertExactPair(reopened, reset.undo().session());
        }
    }

    @Test
    void inactiveCallbacksKeepMetadataHistoryAndEnableRestoresExactLatestReferences() throws Exception {
        var disabled = applied(wrap(openDefault()), new SetProperty(BUTTON, p("enabled"), new PropertyValue.BooleanValue(false)));
        var first = applied(disabled, new SetProperty(BUTTON, p("onLongPress"), reference("first")));
        var second = applied(first, new SetProperty(BUTTON, p("onLongPress"), reference("second")));
        assertArrayEquals(first.current().dartCandidateBytes(), second.current().dartCandidateBytes());
        assertFalse(Arrays.equals(first.current().fdBytes(), second.current().fdBytes()));
        assertExactPair(first, second.undo().session());
        assertExactPair(second, second.undo().session().redo().session());
        var saved = reopen(second);
        var enabled = applied(saved, new SetProperty(BUTTON, p("enabled"), new PropertyValue.BooleanValue(true)));
        assertTrue(source(enabled).contains("buttonValues.second"));
        assertTrue(source(enabled).contains("onPressed: () {}"));
        assertFalse(source(enabled).contains("buttonValues.first"));
        assertExactPair(saved, enabled.undo().session());
        assertExactPair(enabled, reopen(enabled));
    }

    @Test
    void selectionNullFalseTrueAndUnsetRetainDistinctRealModelsAndExactSource() throws Exception {
        var initial = wrap(openDefault());
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(false),
                new PropertyValue.BooleanValue(true))) {
            var changed = applied(initial, new SetProperty(BUTTON, p("isSelected"), value));
            assertEquals(value, find(reopen(changed), BUTTON).properties().get(p("isSelected")));
            var reset = applied(changed, new ResetProperty(BUTTON, p("isSelected")));
            assertExactPair(initial, reset);
            assertExactPair(changed, reset.undo().session());
        }
    }

    @Test
    void invalidCommandsKeepExactCandidateCursorAndRedoAndRequiredIconNeverVanishes() throws Exception {
        var initial = wrap(openDefault());
        var changed = applied(initial, new SetProperty(BUTTON, p("iconSize"), infinity()));
        var undone = changed.undo().session();
        for (var command : List.of(new SetProperty(BUTTON, p("splashRadius"), d("0")),
                new SetProperty(BUTTON, p("iconSize"), d("1e400")),
                new SetProperty(BUTTON, p("variant"), s("icon")),
                new SetProperty(BUTTON, p("onPressed"), new PropertyValue.CallbackValue("legacy")))) {
            assertRejectedUnchanged(undone, command, DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertRejectedUnchanged(undone, new SetProperty(BUTTON, p("styleShapeCircleEccentricity"), d("0.5")),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(undone, new MoveWidget(FIRST, new WidgetPlacement(ROOT, CHILDREN, 0)),
                DesignerCommandDiagnosticCode.SLOT_REQUIRED);
        assertExactPair(changed, undone.redo().session());
        var replacement = applied(initial, new ReplaceSlotChild(BUTTON, ICON, FIRST,
                new ReplaceSlotChild.ExistingWidget(SECOND)));
        assertEquals(SECOND, ((WidgetSlot.SingleSlot) find(replacement, BUTTON).slots().get(ICON)).child().orElseThrow().id());
        assertExactPair(initial, replacement.undo().session());
        assertExactPair(replacement, reopen(replacement));
        var spacer = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(), StableId.random());
        assertRejectedUnchanged(initial, new AddWidget(new WidgetPlacement(BUTTON, SELECTED, 0), spacer),
                DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
    }

    private static DesignerCommandSession wrap(DesignerCommandSession initial) {
        var definition = CATALOG.find(TYPE).orElseThrow();
        var slot = WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition).orElseThrow();
        return applied(initial, new WrapWidget(FIRST, WidgetNodePrototypeFactory.create(definition, BUTTON), slot.name(), 0));
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
