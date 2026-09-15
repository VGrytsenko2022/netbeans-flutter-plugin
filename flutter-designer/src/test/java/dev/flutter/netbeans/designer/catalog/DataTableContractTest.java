package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DataTableContractTest {
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static WidgetNode table(){return WidgetNodePrototypeFactory.create(C.find(DataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String key,PropertyValue v){var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName(key),v);return new WidgetNode(n.id(),n.type(),p,n.slots());}
    static GeneratedDartRegions generate(WidgetNode n){
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();
    }
    @Test void nativeDescriptorsAndEveryClosedPlacement() {
        var table=table();var dart=generate(table).build().payload();
        for(String text:List.of("DataTable(","DataColumn(","DataRow(","DataCell(","columns:","rows:","cells:"))assertTrue(dart.contains(text),dart);
        assertFalse(dart.contains("child: const Text('Cell')"),dart);
        for(var type:DataTableWidgetPropertySchema.TYPES){
            var child=C.find(type).orElseThrow();assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(child));
            assertEquals(type.equals(DataTableWidgetPropertySchema.TYPE),WidgetPlacementRules.evaluateRoot(child).accepted());
            if(type.equals(DataTableWidgetPropertySchema.TYPE))continue;
            for(var parent:C.definitions())for(var slot:parent.slots()){
                boolean expected=type.equals(DataTableWidgetPropertySchema.COLUMN)?DataTableWidgetPropertySchema.table(parent.typeId())&&slot.name().equals(DataTableGrid.COLUMNS):
                        DataTableWidgetPropertySchema.row(type)?parent.typeId().equals(DataTableWidgetPropertySchema.TYPE)&&slot.name().equals(DataTableGrid.ROWS):
                        DataTableWidgetPropertySchema.row(parent.typeId())&&slot.name().equals(DataTableGrid.CELLS);
                assertEquals(expected,WidgetPlacementRules.accepts(parent,slot,child),type+" -> "+parent.typeId()+"."+slot.name());
            }
        }
    }
    @Test void allVariantFormsCallbacksAndNullableStateMaps() throws Exception {
        var n=table();var columns=new ArrayList<>(DataTableGrid.children(n,DataTableGrid.COLUMNS));
        var col=with(columns.getFirst(),"columnWidth",new PropertyValue.StringValue("max(fixed(40),intrinsic(1))"));
        col=with(col,"onSort",new PropertyValue.StringValue("noop"));
        col=with(col,"mouseCursor",new PropertyValue.StringValue("local"));
        col=with(col,"mouseCursorHovered",new PropertyValue.NullValue());columns.set(0,col);
        n=DataTableGrid.withChildren(n,DataTableGrid.COLUMNS,columns);
        var rows=new ArrayList<>(DataTableGrid.children(n,DataTableGrid.ROWS));
        var row=DataTableGrid.starterRow(rows.getFirst().id(),DataTableWidgetPropertySchema.ROW_INDEX,2);
        row=with(row,"index",new PropertyValue.IntegerValue(BigInteger.ONE));
        for(String callback:DataTableWidgetPropertySchema.callbacks(row.type()).keySet())row=with(row,callback,new PropertyValue.StringValue("noop"));
        var cells=new ArrayList<>(DataTableGrid.children(row,DataTableGrid.CELLS));
        cells.set(0,new WidgetNode(cells.getFirst().id(),DataTableWidgetPropertySchema.EMPTY,Map.of(),Map.of()));
        var cell=cells.get(1);for(String callback:DataTableWidgetPropertySchema.callbacks(cell.type()).keySet())cell=with(cell,callback,new PropertyValue.StringValue("noop"));
        cells.set(1,cell);row=DataTableGrid.withChildren(row,DataTableGrid.CELLS,cells);rows.set(0,row);
        n=DataTableGrid.withChildren(n,DataTableGrid.ROWS,rows);
        for(String f:DataTableWidgetPropertySchema.styleFamilies(n.type()))n=with(n,f,new PropertyValue.StringValue("local"));
        n=with(n,"dataTextStyleFontSize",new PropertyValue.DoubleValue(BigDecimal.valueOf(18)));
        n=with(n,"dataRowColor",new PropertyValue.StringValue("local"));
        n=with(n,"border",new PropertyValue.StringValue("all"));
        n=with(n,"onSelectAll",new PropertyValue.StringValue("noop"));
        var result=generate(n);String dart=result.build().payload();
        for(String text:List.of("DataCell.empty","DataRow.byIndex(","onSort: (_, __) {}","WidgetStateProperty<MouseCursor?>.fromMap",
                "WidgetStateProperty<Color?>.fromMap","TextStyle(","dataTextStyle:","headingTextStyle:","fontSize: 18.0","TableBorder("))assertTrue(dart.contains(text),dart);
        assertFalse(dart.contains("DataCell.empty()"),dart);
        assertFalse(dart.contains("const DataRow.byIndex"),dart);
        var ids=result.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).toList();
        assertEquals(ids.size(),new HashSet<>(ids).size());
        var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(n);
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
    }
    @Test void columnEditsDoNotSilentlyRewriteAStateOwnedSortIndex() {
        var source=table();
        var bound=new WidgetNode(source.id(),source.type(),source.properties(),source.slots(),source.extensions(),source.stateBinding(),
                Map.of(new PropertyName("sortColumnIndex"),new StatePropertyBinding("_sort",StateBinding.Type.NULLABLE_INT,
                        Optional.empty(),StatePropertyBinding.Transform.DIRECT)));
        for(var operation:List.of(TableGrid.Operation.ADD_COLUMN,TableGrid.Operation.REMOVE_COLUMN,TableGrid.Operation.MOVE_COLUMN))
            assertThrows(IllegalArgumentException.class,()->DataTableGrid.edit(bound,operation,0,1,StableId.random()));
        assertDoesNotThrow(()->DataTableGrid.edit(bound,TableGrid.Operation.MOVE_ROW,0,1,StableId.random()));
    }
    @Test void editsRemapSortAndPreserveAllSurvivingIdentities() {
        var n=with(table(),"sortColumnIndex",new PropertyValue.IntegerValue(BigInteger.ONE));
        var rows=DataTableGrid.children(n,DataTableGrid.ROWS);var columns=DataTableGrid.children(n,DataTableGrid.COLUMNS);
        n=DataTableGrid.edit(n,TableGrid.Operation.ADD_COLUMN,1,0,StableId.random());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.TWO),n.properties().get(new PropertyName("sortColumnIndex")));
        n=DataTableGrid.edit(n,TableGrid.Operation.MOVE_COLUMN,2,0,StableId.random());
        assertEquals(columns.get(1),DataTableGrid.children(n,DataTableGrid.COLUMNS).getFirst());
        for(int r=0;r<rows.size();r++)assertEquals(DataTableGrid.children(rows.get(r),DataTableGrid.CELLS).get(1),
                DataTableGrid.children(DataTableGrid.children(n,DataTableGrid.ROWS).get(r),DataTableGrid.CELLS).getFirst());
        n=DataTableGrid.edit(n,TableGrid.Operation.REMOVE_COLUMN,0,0,StableId.random());
        assertFalse(n.properties().containsKey(new PropertyName("sortColumnIndex")));
        assertTrue(DataTableGrid.relationshipError(n).isEmpty());
        n=DataTableGrid.withChildren(n,DataTableGrid.ROWS,List.of());
        n=DataTableGrid.edit(n,TableGrid.Operation.ADD_COLUMN,0,0,StableId.random());
        n=DataTableGrid.edit(n,TableGrid.Operation.ADD_ROW,0,0,StableId.random());
        assertEquals(3,DataTableGrid.children(DataTableGrid.children(n,DataTableGrid.ROWS).getFirst(),DataTableGrid.CELLS).size());
    }
    @Test void rejectsInvalidStructuresAndInactiveModes() {
        var n=table();
        assertTrue(DataTableGrid.relationshipError(DataTableGrid.withChildren(n,DataTableGrid.COLUMNS,List.of())).isPresent());
        assertTrue(DataTableGrid.relationshipError(with(n,"sortColumnIndex",new PropertyValue.IntegerValue(BigInteger.TWO))).isPresent());
        assertTrue(DataTableGrid.relationshipError(with(with(n,"dataRowHeight",new PropertyValue.IntegerValue(BigInteger.TEN)),"dataRowMinHeight",new PropertyValue.IntegerValue(BigInteger.ONE))).isPresent());
        assertTrue(DataTableGrid.relationshipError(with(with(n,"dataRowMinHeight",new PropertyValue.IntegerValue(BigInteger.TEN)),"dataRowMaxHeight",new PropertyValue.IntegerValue(BigInteger.ONE))).isPresent());
        assertTrue(DataTableGrid.relationshipError(with(n,"dataTextStyleFontSize",new PropertyValue.IntegerValue(BigInteger.TEN))).isPresent());
        var rows=List.of(DataTableGrid.starterRow(StableId.random(),DataTableWidgetPropertySchema.ROW_INDEX,2),DataTableGrid.starterRow(StableId.random(),DataTableWidgetPropertySchema.ROW_INDEX,2));
        assertTrue(DataTableGrid.relationshipError(DataTableGrid.withChildren(n,DataTableGrid.ROWS,rows)).isPresent());
        assertThrows(IllegalArgumentException.class,()->DataTableGrid.edit(n,TableGrid.Operation.MOVE_ROW,0,2,StableId.random()));
    }
}
