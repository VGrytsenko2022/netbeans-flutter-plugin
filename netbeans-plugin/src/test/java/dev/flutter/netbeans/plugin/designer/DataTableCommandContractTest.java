package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataTableCommandContractTest {
    @Test void historyStaleFenceNewColumnsAndByIndexRowsRemainRectangular() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var table=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=StateBindingRealSdkTest.openRoot(table,"table.dart","");
        var command=new EditDataTableGrid(table,TableGrid.Operation.ADD_COLUMN,1,0,StableId.random());
        var result=baseline.apply(command);assertTrue(result.changed(),result.diagnostics().toString());
        var current=result.session();
        assertArrayEquals(baseline.current().fdBytes(),current.undo().session().current().fdBytes());
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().fdBytes(),current.undo().session().redo().session().current().fdBytes());
        assertFalse(current.apply(command).changed());
        assertTrue(current.apply(command).diagnostics().stream().anyMatch(d->d.code()==DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT));
        var column=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.COLUMN).orElseThrow(),StableId.random());
        result=current.apply(new AddWidget(new WidgetPlacement(table.id(),DataTableGrid.COLUMNS,1),column));
        assertTrue(result.changed(),result.diagnostics().toString());current=result.session();
        for(int r=0;r<2;r++) {
            var row=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.ROW_INDEX).orElseThrow(),StableId.random());
            result=current.apply(new AddWidget(new WidgetPlacement(table.id(),DataTableGrid.ROWS,2+r),row));
            assertTrue(result.changed(),result.diagnostics().toString());current=result.session();
        }
        var root=current.current().document().root();assertEquals(4,DataTableGrid.children(root,DataTableGrid.COLUMNS).size());
        var rows=DataTableGrid.children(root,DataTableGrid.ROWS);assertEquals(4,rows.size());
        for(var row:rows)assertEquals(4,DataTableGrid.children(row,DataTableGrid.CELLS).size());
        assertNotEquals(rows.get(2).properties().get(new PropertyName("index")),rows.get(3).properties().get(new PropertyName("index")));
        var firstCell=DataTableGrid.children(rows.getFirst(),DataTableGrid.CELLS).getFirst();
        var empty=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.EMPTY).orElseThrow(),StableId.random());
        var beforeColumnMove=current;
        var movedColumn=DataTableGrid.children(current.current().document().root(),DataTableGrid.COLUMNS).getFirst();
        result=current.apply(new MoveWidget(movedColumn.id(),new WidgetPlacement(table.id(),DataTableGrid.COLUMNS,3)));
        assertTrue(result.changed(),result.diagnostics().toString());current=result.session();
        assertEquals(movedColumn,DataTableGrid.children(current.current().document().root(),DataTableGrid.COLUMNS).get(3));
        assertEquals(firstCell,DataTableGrid.children(DataTableGrid.children(current.current().document().root(),DataTableGrid.ROWS).getFirst(),DataTableGrid.CELLS).get(3));
        assertArrayEquals(beforeColumnMove.current().fdBytes(),current.undo().session().current().fdBytes());
        current=current.undo().session();
        var beforeCell=current;
        result=current.apply(new AddWidget(new WidgetPlacement(rows.getFirst().id(),DataTableGrid.CELLS,1),empty));
        assertTrue(result.changed(),result.diagnostics().toString());current=result.session();
        var insertedRows=DataTableGrid.children(current.current().document().root(),DataTableGrid.ROWS);
        assertEquals(5,DataTableGrid.children(current.current().document().root(),DataTableGrid.COLUMNS).size());
        for(var insertedRow:insertedRows)assertEquals(5,DataTableGrid.children(insertedRow,DataTableGrid.CELLS).size());
        assertEquals(firstCell,DataTableGrid.children(insertedRows.getFirst(),DataTableGrid.CELLS).getFirst());
        assertEquals(empty,DataTableGrid.children(insertedRows.getFirst(),DataTableGrid.CELLS).get(1));
        assertArrayEquals(beforeCell.current().fdBytes(),current.undo().session().current().fdBytes());
        var invalid=current.apply(new RemoveWidget(DataTableGrid.children(rows.getFirst(),DataTableGrid.CELLS).getFirst().id()));
        assertFalse(invalid.changed());assertArrayEquals(current.current().fdBytes(),invalid.session().current().fdBytes());
        var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
        assertTrue(reopened.ready(),reopened.diagnostics().toString());
        assertTrue(reopened.session().orElseThrow().apply(new SetProperty(rows.getFirst().id(),new PropertyName("selected"),new PropertyValue.BooleanValue(true))).changed());
    }
}
