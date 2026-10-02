package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.BadgeTestValues.*;

class BadgeCommandSessionTest {
    private static final WidgetCatalog CATALOG=BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE=BadgeWidgetPropertySchema.BADGE_TYPE;
    private static final StableId ROOT=StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST=StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND=StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId BADGE=StableId.parse("45c2bb0e-5bb3-4140-9d6c-7eaa26fae227");
    private static final SlotName CHILD=new SlotName("child"),LABEL=new SlotName("label"),CHILDREN=new SlotName("children");

    @Test void emptyCreationCountZeroSaveReopenResetAndConstructorSwitchRemainOneHistoryStep() throws Exception {
        var base=openDefault();var direct=add(base);assertTrue(find(direct,BADGE).properties().isEmpty());
        var count=applied(direct,new SetProperty(BADGE,p("count"),i(0)));
        assertTrue(source(count).contains("Badge.count("));assertExactPair(direct,count.undo().session());assertExactPair(count,count.undo().session().redo().session());
        var saved=reopen(count);assertFalse(saved.dirty());assertEquals(i(0),find(saved,BADGE).properties().get(p("count")));
        var changed=applied(saved,new SetProperty(BADGE,p("maxCount"),i(1)));changed=applied(changed,new SetProperty(BADGE,p("count"),i(2)));
        assertRejectedUnchanged(changed,new ResetProperty(BADGE,p("count")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var reset=applied(changed,patch(Map.of(),Set.of(p("count"),p("maxCount"))));
        assertEquals(changed.cursor()+1,reset.cursor());assertTrue(find(reset,BADGE).properties().isEmpty());assertFalse(source(reset).contains("Badge.count("));
        assertExactPair(changed,reset.undo().session());assertExactPair(reset,reset.undo().session().redo().session());assertExactPair(reset,reopen(reset));
    }
    @Test void fortyOnePropertyRoutesAndBothPaintBranchesPreserveUndoRedoAndResetAfterReopen() throws Exception {
        var covered=new HashSet<PropertyName>();
        for(boolean paints:List.of(false,true)){
            var initial=add(openDefault());var values=full(paints,true);covered.addAll(values.keySet());var states=new ArrayList<DesignerCommandSession>();states.add(initial);var current=initial;
            for(var entry:values.entrySet()){current=applied(current,new SetProperty(BADGE,entry.getKey(),entry.getValue()));states.add(current);}
            for(int i=states.size()-2;i>=0;i--){current=current.undo().session();assertExactPair(states.get(i),current);}
            for(int i=1;i<states.size();i++){current=current.redo().session();assertExactPair(states.get(i),current);}
            current=reopen(current);var reset=applied(current,patch(Map.of(),values.keySet()));assertTrue(find(reset,BADGE).properties().isEmpty());assertExactPair(current,reset.undo().session());assertExactPair(reset,reopen(reset));
        }
        assertEquals(41,covered.size());
    }
    @Test void labelIsNeverSilentlyDeletedWhenEnteringCountAndCanBeMovedToChildFirst() throws Exception {
        var base=add(openDefault());var labelled=applied(base,new MoveWidget(FIRST,new WidgetPlacement(BADGE,LABEL,0)));
        assertRejectedUnchanged(labelled,new SetProperty(BADGE,p("count"),i(0)),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertEquals(FIRST,((WidgetSlot.SingleSlot)find(labelled,BADGE).slots().get(LABEL)).child().orElseThrow().id());
        var moved=applied(labelled,new MoveWidget(FIRST,new WidgetPlacement(BADGE,CHILD,0)));
        var count=applied(moved,new SetProperty(BADGE,p("count"),i(0)));
        assertTrue(source(count).contains("First"));assertFalse(source(count).contains("label:"));assertTrue(source(count).contains("child:"));
        assertRejectedUnchanged(count,new MoveWidget(SECOND,new WidgetPlacement(BADGE,LABEL,0)),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(count,new AddWidget(new WidgetPlacement(BADGE,LABEL,0),text(StableId.random(),"forbidden label")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertExactPair(count,reopen(count));assertExactPair(moved,count.undo().session());
    }
    @Test void bothOptionalSlotsSupportReplacementRemovalMoveSaveAndHistoryWithoutLosingIds() throws Exception {
        var current=add(openDefault());var states=new ArrayList<DesignerCommandSession>();states.add(current);
        current=applied(current,new MoveWidget(FIRST,new WidgetPlacement(BADGE,LABEL,0)));states.add(current);
        current=applied(current,new MoveWidget(SECOND,new WidgetPlacement(BADGE,CHILD,0)));states.add(current);
        var replacement=text(StableId.random(),"replacement");
        current=applied(current,new ReplaceSlotChild(BADGE,LABEL,FIRST,new ReplaceSlotChild.NewSubtree(replacement)));states.add(current);
        assertRejectedUnchanged(current,new ReplaceSlotChild(BADGE,LABEL,FIRST,new ReplaceSlotChild.NewSubtree(text(StableId.random(),"stale"))),DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT);
        current=applied(current,new RemoveWidget(replacement.id()));states.add(current);
        current=applied(current,new MoveWidget(SECOND,new WidgetPlacement(ROOT,CHILDREN,1)));states.add(current);
        current=applied(current,new MoveWidget(BADGE,new WidgetPlacement(ROOT,CHILDREN,1)));states.add(current);
        assertExactPair(current,reopen(current));
        for(int i=states.size()-2;i>=0;i--){current=current.undo().session();assertExactPair(states.get(i),current);}
        for(int i=1;i<states.size();i++){current=current.redo().session();assertExactPair(states.get(i),current);}
        current=applied(current,new RemoveWidget(BADGE));assertTrue(find(current.current().document().root(),BADGE).isEmpty());
    }
    @Test void stylePaintSwapsAreAtomicAndSharedTextColorRemainsIndependent() throws Exception {
        var base=applied(add(openDefault()),new SetProperty(BADGE,p("textStyleColor"),theme()));
        assertRejectedUnchanged(base,new SetProperty(BADGE,p("textStyleForeground"),paint()),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var painted=applied(base,patch(Map.of(p("textStyleForeground"),paint(),p("textColor"),theme()),Set.of(p("textStyleColor"))));
        assertEquals(base.cursor()+1,painted.cursor());assertExactPair(base,painted.undo().session());assertTrue(source(painted).contains("foreground: (Paint()"),source(painted));
        var changed=applied(reopen(painted),new SetProperty(BADGE,p("isLabelVisible"),new PropertyValue.BooleanValue(false)));
        assertTrue(source(changed).contains("foreground: (Paint()"));assertTrue(source(changed).contains("isLabelVisible: false"));
        assertExactPair(changed,reopen(changed));
    }
    @Test void wrongValuesAndDependencyFailuresDoNotAdvanceCursorDestroyRedoOrModifyPairs() throws Exception {
        var base=applied(add(openDefault()),new SetProperty(BADGE,p("count"),i(4)));base=applied(base,new SetProperty(BADGE,p("count"),i(5))).undo().session();assertTrue(base.canRedo());
        for(String name:BadgeWidgetPropertySchema.definitions().keySet())assertRejectedUnchanged(base,new SetProperty(BADGE,p(name),new PropertyValue.NullValue()),DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(base,new SetProperty(BADGE,p("count"),i(-1)),DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(base,new SetProperty(BADGE,p("maxCount"),i(0)),DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(base,new SetProperty(BADGE,p("smallSize"),d("-1")),DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(base,new SetProperty(BADGE,p("textStylePackage"),s("fonts")),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var direct=add(openDefault());assertRejectedUnchanged(direct,new SetProperty(BADGE,p("maxCount"),i(1)),DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        var negativeLarge=applied(base,new SetProperty(BADGE,p("largeSize"),d("-10")));assertEquals(d("-10"),find(negativeLarge,BADGE).properties().get(p("largeSize")));
    }
    @Test void parentDataRestrictionsStillApplyToBothSlotsInAllCountModes() throws Exception {
        for(boolean count:List.of(false,true))for(var slot:List.of(LABEL,CHILD)){
            var current=add(openDefault());if(count)current=applied(current,new SetProperty(BADGE,p("count"),i(0)));
            var spacer=WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(),StableId.random());
            assertRejectedUnchanged(current,new AddWidget(new WidgetPlacement(BADGE,slot,0),spacer),DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED);
        }
    }
    private static DesignerCommandSession add(DesignerCommandSession session){return applied(session,new AddWidget(new WidgetPlacement(ROOT,CHILDREN,2),WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(),BADGE)));}
    private static PatchProperties patch(Map<PropertyName,PropertyValue> sets,Collection<PropertyName> resets){var patches=new ArrayList<PatchProperties.Patch>();for(var name:resets)patches.add(new PatchProperties.ResetPatch(name));for(var entry:sets.entrySet())patches.add(new PatchProperties.SetPatch(entry.getKey(),entry.getValue()));return new PatchProperties(BADGE,patches);}
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
