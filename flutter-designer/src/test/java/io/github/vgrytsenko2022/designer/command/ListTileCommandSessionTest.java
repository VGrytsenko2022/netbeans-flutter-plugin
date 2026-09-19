package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.ListTileTestValues.*;

class ListTileCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId TILE = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void emptyCreationSlotMovesAndRemovalNeverFabricateTextOrCallbacksAndRetainEveryEndpoint() throws Exception {
        var initial = openDefault();
        var added = addEmpty(initial);
        assertEquals(Map.of(), find(added, TILE).properties());
        assertFalse(source(added).contains("onTap:"));
        assertExactPair(initial, added.undo().session());
        var title = applied(added, new MoveWidget(FIRST, new WidgetPlacement(TILE, new SlotName("title"), 0)));
        assertEquals(FIRST, child(title, "title").id());
        assertEquals("First", ((PropertyValue.StringValue) find(title, FIRST).properties().get(p("data"))).value());
        assertExactPair(added, title.undo().session());
        var subtitle = applied(title, new MoveWidget(SECOND, new WidgetPlacement(TILE, new SlotName("subtitle"), 0)));
        assertEquals(SECOND, child(subtitle, "subtitle").id());
        assertExactPair(subtitle, reopen(subtitle));
        var moved = applied(subtitle, new MoveWidget(FIRST, new WidgetPlacement(TILE, new SlotName("leading"), 0)));
        assertEquals(FIRST, child(moved, "leading").id());
        assertExactPair(subtitle, moved.undo().session());
        var removed = applied(moved, new RemoveWidget(TILE));
        assertExactPair(moved, removed.undo().session());
        assertExactPair(removed, reopen(removed));
    }

    @Test
    void explicitThreeLineRequiresSubtitleButFalseAndNullRestoreRemovalWithExactUndo() throws Exception {
        var empty = addEmpty(openDefault());
        rejected(empty, new SetProperty(TILE, p("isThreeLine"), b(true)));
        var subtitle = applied(empty, new MoveWidget(SECOND, new WidgetPlacement(TILE, new SlotName("subtitle"), 0)));
        var three = applied(subtitle, new SetProperty(TILE, p("isThreeLine"), b(true)));
        rejected(three, new RemoveWidget(SECOND));
        rejected(three, new MoveWidget(SECOND, new WidgetPlacement(ROOT, CHILDREN, 0)));
        for (PropertyValue value : List.of(b(false), nil())) {
            var relaxed = applied(three, new SetProperty(TILE, p("isThreeLine"), value));
            var removed = applied(relaxed, new RemoveWidget(SECOND));
            assertExactPair(relaxed, removed.undo().session());
            assertExactPair(removed, reopen(removed));
            assertExactPair(three, relaxed.undo().session());
        }
    }

    @Test
    void stateMapWholeLocalSwitchAndDefaultRejectionPreserveRedoAndDoNotErasePeerValues() throws Exception {
        var initial = addEmpty(openDefault());
        for (String family : List.of("iconColor", "textColor", "mouseCursor")) {
            PropertyValue entry = family.equals("mouseCursor") ? s("click") : CheckboxTestValues.theme();
            rejected(initial, new SetProperty(TILE, p(family + "Disabled"), entry));
            var local = applied(initial, patch(Map.of(p(family + "Default"), entry, p(family + "Disabled"), entry), List.of()));
            rejected(local, new ResetProperty(TILE, p(family + "Default")));
            var whole = applied(local, patch(Map.of(p(family), reference(family)), List.of(family + "Default", family + "Disabled")));
            assertFalse(find(whole, TILE).properties().containsKey(p(family + "Default")));
            assertExactPair(local, whole.undo().session());
            assertExactPair(whole, reopen(whole));
            var undone = whole.undo().session();
            rejected(undone, new SetProperty(TILE, p(family), reference(family)));
            assertExactPair(whole, undone.redo().session());
        }
    }

    @Test
    void allLocalTextStylesShapesAndNonfiniteGeometryRoundTripExactSavedHistory() throws Exception {
        var initial = openDefault();
        for (String shape : ListTileWidgetPropertySchema.shapeKinds()) {
            var candidate = fullNode(shape, false);
            var inserted = new WidgetNode(TILE, candidate.type(), candidate.properties(), candidate.slots());
            var dense = applied(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 0), inserted));
            assertExactPair(initial, dense.undo().session());
            assertExactPair(dense, reopen(dense));
            for (String family : ListTileWidgetPropertySchema.styleFamilies()) {
                var whole = applied(dense, patch(Map.of(p(family), reference(family)), ListTileWidgetPropertySchema.localTextStyleProperties(family)));
                assertExactPair(dense, whole.undo().session());
                assertExactPair(whole, reopen(whole));
            }
        }
        var empty = addEmpty(initial);
        for (String name : ListTileWidgetPropertySchema.geometryProperties()) for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            var changed = applied(empty, new SetProperty(TILE, p(name), new PropertyValue.EnumValue("double", member)));
            assertExactPair(empty, changed.undo().session());
            assertExactPair(changed, reopen(changed));
        }
    }

    @Test
    void disabledStoredCallbackEditsStillChangeDartAndInvalidWritesPreserveNativeHistory() throws Exception {
        var empty = addEmpty(openDefault());
        var disabled = applied(empty, patch(Map.of(p("enabled"), b(false), p("onTap"), reference("firstTap")), List.of()));
        var second = applied(disabled, new SetProperty(TILE, p("onTap"), reference("secondTap")));
        assertFalse(Arrays.equals(disabled.current().dartCandidateBytes(), second.current().dartCandidateBytes()));
        assertExactPair(disabled, second.undo().session());
        assertExactPair(second, reopen(second));
        var undone = second.undo().session();
        rejected(undone, new SetProperty(TILE, p("onTap"), new PropertyValue.DartExpressionValue("unsafe()")));
        rejected(undone, new SetProperty(TILE, p("minTileHeight"), d("1e400")));
        assertExactPair(second, undone.redo().session());
        var explicitNull = applied(second, new SetProperty(TILE, p("onTap"), nil()));
        assertTrue(source(explicitNull).contains("onTap: null"));
        var omitted = applied(explicitNull, new ResetProperty(TILE, p("onTap")));
        assertFalse(source(omitted).contains("onTap:"));
        assertExactPair(explicitNull, omitted.undo().session());
        assertExactPair(omitted, reopen(omitted));
    }

    private static DesignerCommandSession addEmpty(DesignerCommandSession initial) {
        return applied(initial, new AddWidget(new WidgetPlacement(ROOT, CHILDREN, 0), WidgetNodePrototypeFactory.create(definition(), TILE)));
    }
    private static WidgetNode child(DesignerCommandSession session, String name) { return ((WidgetSlot.SingleSlot) find(session, TILE).slots().get(new SlotName(name))).child().orElseThrow(); }
    private static PatchProperties patch(Map<PropertyName, PropertyValue> values, List<String> reset) {
        var edits = new ArrayList<PatchProperties.Patch>();
        values.forEach((name, value) -> edits.add(new PatchProperties.SetPatch(name, value)));
        reset.forEach(name -> edits.add(new PatchProperties.ResetPatch(p(name))));
        return new PatchProperties(TILE, edits);
    }
    private static WidgetNode find(DesignerCommandSession session, StableId id) { return find(session.current().document().root(), id).orElseThrow(); }
    private static Optional<WidgetNode> find(WidgetNode node, StableId id) {
        if (node.id().equals(id)) return Optional.of(node);
        for (var slot : node.slots().values()) for (var child : slot instanceof WidgetSlot.SingleSlot single ? single.child().stream().toList() : ((WidgetSlot.ListSlot) slot).children()) {
            var found = find(child, id); if (found.isPresent()) return found;
        }
        return Optional.empty();
    }
    private static WidgetNode text(StableId id, String value) { return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), s(value)), Map.of()); }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static void rejected(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertFalse(result.changed()); assertSame(session, result.session());
        assertEquals(session.cursor(), result.session().cursor());
        assertEquals(session.canRedo(), result.session().canRedo());
        assertExactPair(session, result.session());
    }
    private static void assertExactPair(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
        assertEquals(expected.current().document(), actual.current().document());
    }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static DesignerCommandSession reopen(DesignerCommandSession current) throws Exception {
        var saved = current.markSaved();
        var result = DesignerCommandSession.open(OriginalFdBytes.copyOf(saved.current().fdBytes(), FdCodecLimits.defaults()), saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DesignerCommandSession openDefault() throws Exception {
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Row"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(text(FIRST, "First"), text(SECOND, "Second")))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root);
        var generated = new DartRegionGenerator().generate(provisional, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), root);
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatelessWidget {\n  // <netbeans-flutter-designer region=\"build\">\n"
                + generated.build().payload() + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), dart, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
