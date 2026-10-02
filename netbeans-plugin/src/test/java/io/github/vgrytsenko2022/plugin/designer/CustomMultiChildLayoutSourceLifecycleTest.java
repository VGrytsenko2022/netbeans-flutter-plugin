package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomMultiChildLayoutSourceLifecycleTest {
    private static final SlotName CHILDREN=new SlotName("children");
    private static DesignerCommandSession open() throws Exception {
        var root=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of())));
        return StateBindingRealSdkTest.openRoot(root,"layout.dart","// User code remains intact.\nint userValue = 17;");
    }
    private static WidgetNode layout() {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(CustomMultiChildLayoutWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
    }
    private static AddWidget add(DesignerCommandSession session,WidgetNode widget) {
        return new AddWidget(new WidgetPlacement(session.current().document().root().id(),CHILDREN,0),widget);
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(),StandardCharsets.UTF_8);
    }
    @Test void removalReinsertionAndHistoryRetainExactlyOneUserOwnedClass() throws Exception {
        var initial=open();var widget=layout();
        var result=initial.apply(add(initial,widget));assertTrue(result.changed(),result.diagnostics().toString());
        var inserted=result.session();
        assertEquals(1,source(inserted).split("class _FlutterDesignerMultiChildLayoutDelegate",-1).length-1);
        var removed=inserted.apply(new RemoveWidget(widget.id()));
        assertTrue(removed.changed(),removed.diagnostics().toString());
        assertTrue(source(removed.session()).contains("class _FlutterDesignerMultiChildLayoutDelegate"));
        var readded=removed.session().apply(add(removed.session(),layout()));
        assertTrue(readded.changed(),readded.diagnostics().toString());
        assertEquals(1,source(readded.session()).split("class _FlutterDesignerMultiChildLayoutDelegate",-1).length-1);
        assertTrue(source(readded.session()).contains("int userValue = 17;"));
        assertArrayEquals(removed.session().current().dartCandidateBytes(),readded.session().undo().session().current().dartCandidateBytes());
        assertArrayEquals(initial.current().dartCandidateBytes(),inserted.undo().session().current().dartCandidateBytes());
        assertArrayEquals(inserted.current().dartCandidateBytes(),inserted.undo().session().redo().session().current().dartCandidateBytes());
    }
    @Test void idsReplacementReorderDuplicateRejectionAndReopenAreAtomic() throws Exception {
        var initial=open();var root=layout();var current=initial.apply(add(initial,root)).session();
        var a=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(LayoutIdWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var b=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(LayoutIdWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var first=current.apply(new AddWidget(new WidgetPlacement(root.id(),CHILDREN,0),a));assertTrue(first.changed());
        var second=first.session().apply(new AddWidget(new WidgetPlacement(root.id(),CHILDREN,1),b));assertTrue(second.changed());current=second.session();
        var duplicate=current.apply(new SetProperty(b.id(),new PropertyName("id"),a.properties().get(new PropertyName("id"))));
        assertFalse(duplicate.changed());assertArrayEquals(current.current().dartCandidateBytes(),duplicate.session().current().dartCandidateBytes());
        var seed=((WidgetSlot.SingleSlot)a.slots().get(new SlotName("child"))).child().orElseThrow();
        assertFalse(current.apply(new RemoveWidget(seed.id())).changed());
        assertFalse(current.apply(new MoveWidget(seed.id(),new WidgetPlacement(initial.current().document().root().id(),CHILDREN,1))).changed());
        var newChild=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        var replaced=current.apply(new ReplaceSlotChild(a.id(),new SlotName("child"),seed.id(),new ReplaceSlotChild.NewSubtree(newChild)));
        assertTrue(replaced.changed(),replaced.diagnostics().toString());
        assertArrayEquals(current.current().dartCandidateBytes(),replaced.session().undo().session().current().dartCandidateBytes());
        var moved=replaced.session().apply(new MoveWidget(b.id(),new WidgetPlacement(root.id(),CHILDREN,0)));
        assertTrue(moved.changed(),moved.diagnostics().toString());
        assertArrayEquals(replaced.session().current().dartCandidateBytes(),moved.session().undo().session().current().dartCandidateBytes());
        var opened=DesignerCommandSession.open(moved.session().current().fdSnapshot(),moved.session().current().dartCandidateBytes(),BuiltInWidgetCatalog.getDefault());
        assertTrue(opened.ready(),opened.diagnostics().toString());
        assertArrayEquals(moved.session().current().dartCandidateBytes(),opened.session().orElseThrow().current().dartCandidateBytes());
        assertTrue(opened.session().orElseThrow().apply(new SetProperty(a.id(),new PropertyName("id"),new PropertyValue.StringValue("renamed"))).changed());
    }
    @Test void customReferencesDoNotScaffoldAndAConflictingNameDoesNotMutateEitherSnapshot() throws Exception {
        var initial=open();var prototype=layout();
        var custom=new WidgetNode(prototype.id(),prototype.type(),Map.of(new PropertyName("delegate"),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectLayout",Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty())),Map.of());
        var result=initial.apply(add(initial,custom));assertTrue(result.changed());
        assertFalse(source(result.session()).contains("class _FlutterDesignerMultiChildLayoutDelegate"));
        String collision=source(initial)+"\nfinal _FlutterDesignerMultiChildLayoutDelegate = Object();\n";
        var opened=DesignerCommandSession.open(initial.current().fdSnapshot(),collision.getBytes(StandardCharsets.UTF_8),BuiltInWidgetCatalog.getDefault());
        assertTrue(opened.ready());
        var baseline=opened.session().orElseThrow();
        var rejected=baseline.apply(add(baseline,prototype));
        assertFalse(rejected.changed());
        assertTrue(rejected.diagnostics().stream().anyMatch(d->d.code()==DesignerCommandDiagnosticCode.MULTI_CHILD_LAYOUT_DELEGATE_REJECTED));
        assertArrayEquals(baseline.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        assertEquals(baseline.current().fdSnapshot(),rejected.session().current().fdSnapshot());
    }
}
