package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverPersistentHeaderSourceLifecycleTest {
    private static final SlotName SLIVERS=new SlotName("slivers");
    private static DesignerCommandSession open() throws Exception {
        var root=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(),Map.of(SLIVERS,new WidgetSlot.ListSlot(List.of())));
        return StateBindingRealSdkTest.openRoot(root,"header.dart","// User code remains intact.\nint userValue = 17;");
    }
    private static WidgetNode header() {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(SliverPersistentHeaderWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
    }
    private static AddWidget add(DesignerCommandSession session,WidgetNode widget) {
        return new AddWidget(new WidgetPlacement(session.current().document().root().id(),SLIVERS,0),widget);
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(),StandardCharsets.UTF_8);
    }
    @Test void removalReinsertionMoveAndHistoryRetainExactlyOneUserOwnedClass() throws Exception {
        var initial=open();var widget=header();
        var result=initial.apply(add(initial,widget));assertTrue(result.changed(),result.diagnostics().toString());
        var inserted=result.session();
        assertEquals(1,source(inserted).split("class _FlutterDesignerPersistentHeaderDelegate",-1).length-1);
        var removed=inserted.apply(new RemoveWidget(widget.id()));
        assertTrue(removed.changed(),removed.diagnostics().toString());
        assertTrue(source(removed.session()).contains("class _FlutterDesignerPersistentHeaderDelegate"));
        var readded=removed.session().apply(add(removed.session(),header()));
        assertTrue(readded.changed(),readded.diagnostics().toString());
        assertEquals(1,source(readded.session()).split("class _FlutterDesignerPersistentHeaderDelegate",-1).length-1);
        assertTrue(source(readded.session()).contains("int userValue = 17;"));
        assertArrayEquals(removed.session().current().dartCandidateBytes(),readded.session().undo().session().current().dartCandidateBytes());
        assertArrayEquals(initial.current().dartCandidateBytes(),inserted.undo().session().current().dartCandidateBytes());
        assertArrayEquals(inserted.current().dartCandidateBytes(),inserted.undo().session().redo().session().current().dartCandidateBytes());
    }
    @Test void customReferencesDoNotScaffoldAndAConflictingNameDoesNotMutateEitherSnapshot() throws Exception {
        var initial=open();var prototype=header();
        var custom=new WidgetNode(prototype.id(),prototype.type(),Map.of(new PropertyName("delegate"),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectHeader",Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty())),Map.of());
        var result=initial.apply(add(initial,custom));assertTrue(result.changed());
        assertFalse(source(result.session()).contains("class _FlutterDesignerPersistentHeaderDelegate"));
        String collision=source(initial)+"\nfinal _FlutterDesignerPersistentHeaderDelegate = Object();\n";
        var opened=DesignerCommandSession.open(initial.current().fdSnapshot(),collision.getBytes(StandardCharsets.UTF_8),BuiltInWidgetCatalog.getDefault());
        assertTrue(opened.ready());
        var baseline=opened.session().orElseThrow();
        var rejected=baseline.apply(add(baseline,prototype));
        assertFalse(rejected.changed());
        assertTrue(rejected.diagnostics().stream().anyMatch(d->d.code()==DesignerCommandDiagnosticCode.PERSISTENT_HEADER_DELEGATE_REJECTED));
        assertArrayEquals(baseline.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        assertEquals(baseline.current().fdSnapshot(),rejected.session().current().fdSnapshot());
    }
}

