package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableCommandContractTest {
    @Test void oversizedRowCreationIsRejectedWithoutThrowingOrMutating() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var table=WidgetNodePrototypeFactory.create(catalog.find(TableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var cells=new java.util.ArrayList<WidgetNode>();
        for(int i=0;i<1001;i++) cells.add(TableGrid.starterCell(StableId.random()));
        var row=TableGrid.withChildren(TableGrid.children(table).getFirst(),cells);
        table=TableGrid.withChildren(table,java.util.List.of(row));
        var baseline=StateBindingRealSdkTest.openRoot(table,"table.dart","");
        var inserted=WidgetNodePrototypeFactory.create(catalog.find(TableWidgetPropertySchema.ROW).orElseThrow(),StableId.random());
        var result=baseline.apply(new AddWidget(new WidgetPlacement(table.id(),TableGrid.CHILDREN,1),inserted));
        assertFalse(result.changed());
        assertArrayEquals(baseline.current().fdBytes(),result.session().current().fdBytes());
        assertTrue(result.diagnostics().stream().anyMatch(d->d.message().contains("1000 columns")));
    }

    @Test void gridOperationsAreUndoableFencedAndNewPaletteRowsAdaptToAllColumns() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var table=WidgetNodePrototypeFactory.create(catalog.find(TableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=StateBindingRealSdkTest.openRoot(table,"table.dart","");
        var command=new EditTableGrid(table,TableGrid.Operation.ADD_COLUMN,1,0,StableId.random());
        var result=baseline.apply(command);
        assertTrue(result.changed(),result.diagnostics().toString());
        var current=result.session();
        assertEquals(3,TableGrid.children(TableGrid.children(current.current().document().root()).getFirst()).size());
        assertArrayEquals(baseline.current().fdBytes(),current.undo().session().current().fdBytes());
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().fdBytes(),current.undo().session().redo().session().current().fdBytes());
        var stale=current.apply(command);
        assertFalse(stale.changed());
        assertTrue(stale.diagnostics().stream().anyMatch(d->d.code()==DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT));
        assertArrayEquals(current.current().fdBytes(),stale.session().current().fdBytes());
        var row=WidgetNodePrototypeFactory.create(catalog.find(TableWidgetPropertySchema.ROW).orElseThrow(),StableId.random());
        var added=current.apply(new AddWidget(new WidgetPlacement(table.id(),TableGrid.CHILDREN,2),row));
        assertTrue(added.changed(),added.diagnostics().toString());
        var rows=TableGrid.children(added.session().current().document().root());
        assertEquals(3,rows.size());
        assertEquals(row.id(),rows.getLast().id());
        for(var actual:rows) assertEquals(3,TableGrid.children(actual).size());
        var invalid=added.session().apply(new RemoveWidget(TableGrid.children(rows.getFirst()).getFirst().id()));
        assertFalse(invalid.changed(),"A one-cell removal must not create a ragged table");
        assertArrayEquals(added.session().current().fdBytes(),invalid.session().current().fdBytes());
    }
}
