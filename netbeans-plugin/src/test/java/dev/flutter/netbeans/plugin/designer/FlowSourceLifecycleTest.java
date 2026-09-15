package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlowSourceLifecycleTest {
    private static final SlotName CHILDREN=new SlotName("children");
    private static DesignerCommandSession open() throws Exception {
        var root=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of())));
        return StateBindingRealSdkTest.openRoot(root,"layout.dart","// User code remains intact.\nint userValue = 17;");
    }
    private static WidgetNode layout() {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(FlowWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
    }
    private static AddWidget add(DesignerCommandSession session,WidgetNode widget) {
        return new AddWidget(new WidgetPlacement(session.current().document().root().id(),CHILDREN,0),widget);
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(),StandardCharsets.UTF_8);
    }
    @Test void bothVariantsShareEditedDelegateAndRetainChildrenAcrossReopen() throws Exception {
        var initial=open();var ordinary=layout();
        var first=initial.apply(add(initial,ordinary));assertTrue(first.changed());
        String edited=source(first.session()).replace("const Size(256, 192)","const Size(320, 240)");
        var opened=DesignerCommandSession.open(first.session().current().fdSnapshot(),edited.getBytes(StandardCharsets.UTF_8),BuiltInWidgetCatalog.getDefault());
        assertTrue(opened.ready());var session=opened.session().orElseThrow();
        var unwrapped=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(FlowWidgetPropertySchema.UNWRAPPED_TYPE).orElseThrow(),StableId.random());
        var second=session.apply(add(session,unwrapped));assertTrue(second.changed());
        session=second.session();
        assertEquals(1,source(session).split("class _FlutterDesignerFlowDelegate",-1).length-1);
        assertTrue(source(session).contains("const Size(320, 240)"));
        var child1=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),Map.of(),Map.of());
        var child2=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),Map.of(),Map.of());
        var a=session.apply(new AddWidget(new WidgetPlacement(ordinary.id(),CHILDREN,0),child1));
        assertTrue(a.changed());session=a.session();
        var b=session.apply(new AddWidget(new WidgetPlacement(ordinary.id(),CHILDREN,1),child2));
        assertTrue(b.changed());session=b.session();
        var moved=session.apply(new MoveWidget(child1.id(),new WidgetPlacement(ordinary.id(),CHILDREN,1)));
        assertTrue(moved.changed(),moved.diagnostics().toString());
        assertArrayEquals(session.current().dartCandidateBytes(),moved.session().undo().session().current().dartCandidateBytes());
        var reopened=DesignerCommandSession.open(moved.session().current().fdSnapshot(),moved.session().current().dartCandidateBytes(),BuiltInWidgetCatalog.getDefault());
        assertTrue(reopened.ready());
        assertEquals(moved.session().current().fdSnapshot(),reopened.session().orElseThrow().current().fdSnapshot());
        assertTrue(source(reopened.session().orElseThrow()).contains("const Size(320, 240)"));
    }
    @Test void removalReinsertionAndHistoryRetainExactlyOneUserOwnedClass() throws Exception {
        var initial=open();var widget=layout();
        var result=initial.apply(add(initial,widget));assertTrue(result.changed(),result.diagnostics().toString());
        var inserted=result.session();
        assertEquals(1,source(inserted).split("class _FlutterDesignerFlowDelegate",-1).length-1);
        var removed=inserted.apply(new RemoveWidget(widget.id()));
        assertTrue(removed.changed(),removed.diagnostics().toString());
        assertTrue(source(removed.session()).contains("class _FlutterDesignerFlowDelegate"));
        var readded=removed.session().apply(add(removed.session(),layout()));
        assertTrue(readded.changed(),readded.diagnostics().toString());
        assertEquals(1,source(readded.session()).split("class _FlutterDesignerFlowDelegate",-1).length-1);
        assertTrue(source(readded.session()).contains("int userValue = 17;"));
        assertArrayEquals(removed.session().current().dartCandidateBytes(),readded.session().undo().session().current().dartCandidateBytes());
        assertArrayEquals(initial.current().dartCandidateBytes(),inserted.undo().session().current().dartCandidateBytes());
        assertArrayEquals(inserted.current().dartCandidateBytes(),inserted.undo().session().redo().session().current().dartCandidateBytes());
    }
    @Test void customReferencesDoNotScaffoldAndAConflictingNameDoesNotMutateEitherSnapshot() throws Exception {
        var initial=open();var prototype=layout();
        var custom=new WidgetNode(prototype.id(),prototype.type(),Map.of(new PropertyName("delegate"),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectLayout",Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty())),Map.of());
        var result=initial.apply(add(initial,custom));assertTrue(result.changed());
        assertFalse(source(result.session()).contains("class _FlutterDesignerFlowDelegate"));
        String collision=source(initial)+"\nfinal _FlutterDesignerFlowDelegate = Object();\n";
        var opened=DesignerCommandSession.open(initial.current().fdSnapshot(),collision.getBytes(StandardCharsets.UTF_8),BuiltInWidgetCatalog.getDefault());
        assertTrue(opened.ready());
        var baseline=opened.session().orElseThrow();
        var rejected=baseline.apply(add(baseline,prototype));
        assertFalse(rejected.changed());
        assertTrue(rejected.diagnostics().stream().anyMatch(d->d.code()==DesignerCommandDiagnosticCode.FLOW_DELEGATE_REJECTED));
        assertArrayEquals(baseline.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        assertEquals(baseline.current().fdSnapshot(),rejected.session().current().fdSnapshot());
    }
}
