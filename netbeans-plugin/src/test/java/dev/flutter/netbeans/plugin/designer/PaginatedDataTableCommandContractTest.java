package dev.flutter.netbeans.plugin.designer;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PaginatedDataTableCommandContractTest {
    @Test void ordinaryColumnRemovalRemapsTheSortIndexButNotSourceRows() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var table=WidgetNodePrototypeFactory.create(catalog.find(PaginatedDataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=StateBindingRealSdkTest.openRoot(table,"table.dart","");
        var sorted=baseline.apply(new SetProperty(table.id(),new PropertyName("sortColumnIndex"),
                new PropertyValue.IntegerValue(java.math.BigInteger.ONE)));
        assertTrue(sorted.changed(),sorted.diagnostics().toString());
        var first=DataTableGrid.children(table,DataTableGrid.COLUMNS).getFirst();
        var removed=sorted.session().apply(new RemoveWidget(first.id()));
        assertTrue(removed.changed(),removed.diagnostics().toString());
        var after=removed.session().current().document().root();
        assertEquals(new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),after.properties().get(new PropertyName("sortColumnIndex")));
        assertFalse(after.slots().containsKey(DataTableGrid.ROWS));
        assertFalse(removed.session().apply(new RemoveWidget(DataTableGrid.children(after,DataTableGrid.COLUMNS).getFirst().id())).changed());
        assertArrayEquals(sorted.session().current().fdBytes(),removed.session().undo().session().current().fdBytes());
    }
    @Test void insertionScaffoldsStableSourceUndoRedoAndReopenPreserveIt() throws Exception {
        var root=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Column"),Map.of(),Map.of(new SlotName("children"),new WidgetSlot.ListSlot(List.of())));
        var baseline=StateBindingRealSdkTest.openRoot(root,"table.dart","");
        var catalog=BuiltInWidgetCatalog.getDefault();
        var table=WidgetNodePrototypeFactory.create(catalog.find(PaginatedDataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var result=baseline.apply(new AddWidget(new WidgetPlacement(root.id(),new SlotName("children"),0),table));
        assertTrue(result.changed(),result.diagnostics().toString());var current=result.session();
        var source=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(source.contains("class _FlutterDesignerDataTableSource extends DataTableSource"));
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
        assertTrue(reopened.ready(),reopened.diagnostics().toString());current=reopened.session().orElseThrow();
        var second=WidgetNodePrototypeFactory.create(catalog.find(table.type()).orElseThrow(),StableId.random());
        result=current.apply(new AddWidget(new WidgetPlacement(root.id(),new SlotName("children"),1),second));
        assertTrue(result.changed(),result.diagnostics().toString());
        source=new String(result.session().current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertEquals(1,source.split("class _FlutterDesignerDataTableSource",-1).length-1);
        var grid=new EditDataTableGrid(table,TableGrid.Operation.ADD_COLUMN,2,0,StableId.random());
        result=current.apply(grid);assertTrue(result.changed(),result.diagnostics().toString());
        assertFalse(result.session().apply(grid).changed());
    }
}
