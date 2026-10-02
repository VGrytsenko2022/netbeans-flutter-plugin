package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaginatedDataTableContractTest {
    static WidgetNode table() { return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(PaginatedDataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random()); }
    static WidgetNode with(WidgetNode n,String key,PropertyValue v) {var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName(key),v);return new WidgetNode(n.id(),n.type(),p,n.slots());}
    static String dart(WidgetNode n) {
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow().build().payload();
    }
    @Test void completeNativeFieldsAndColumnsOnly() throws Exception {
        var n=table();var definition=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
        var names=new HashSet<String>();definition.properties().forEach(p->names.add(p.name().value()));definition.slots().forEach(s->names.add(s.name().value()));
        assertTrue(names.containsAll(List.of("key","header","actions","columns","sortColumnIndex","sortAscending","onSelectAll","dataRowHeight","dataRowMinHeight","dataRowMaxHeight","headingRowHeight","horizontalMargin","columnSpacing","showCheckboxColumn","showFirstLastButtons","initialFirstRowIndex","onPageChanged","rowsPerPage","availableRowsPerPage","onRowsPerPageChanged","dragStartBehavior","arrowHeadColor","source","checkboxHorizontalMargin","controller","primary","headingRowColor","dividerThickness","showEmptyRows")));
        assertEquals(38,names.size());assertFalse(names.contains("rows"));
        assertEquals(2,DataTableGrid.children(n,DataTableGrid.COLUMNS).size());
        var output=dart(n);assertTrue(output.contains("source: _FlutterDesignerDataTableSource.instance"),output);
        assertFalse(output.contains("_FlutterDesignerDataTableSource()"),output);
        assertFalse(output.contains("const PaginatedDataTable"),output);
        assertFalse(output.contains("actions:"),output);
        for(String callback:List.of("onPageChanged","onRowsPerPageChanged","onSelectAll"))n=with(n,callback,new PropertyValue.StringValue("noop"));
        n=with(n,"availableRowsPerPage",new PropertyValue.StringValue("5, 10, 20"));
        output=dart(n);assertTrue(output.contains("availableRowsPerPage: const <int>[5, 10, 20]"),output);
        for(String callback:List.of("onPageChanged","onRowsPerPageChanged","onSelectAll"))assertTrue(output.contains(callback+": (_) {}"),output);
        var codec=new FdDocumentCodec();var document=RotationTransitionContractTest.document(n);
        assertEquals(document,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(document))).document());
    }
    @Test void pageListRelationsAndClosedPropertyValidation() {
        assertEquals(List.of(BigInteger.TEN,BigInteger.valueOf(20)),PaginatedDataTableWidgetPropertySchema.pageSizes("10, 20"));
        assertTrue(PaginatedDataTableWidgetPropertySchema.pageSizes("").isEmpty());
        for(String value:List.of("0","-1","1,1","1,","1.5","1e2","9007199254740992","foo()"))
            assertThrows(IllegalArgumentException.class,()->PaginatedDataTableWidgetPropertySchema.pageSizes(value));
        var n=table();
        assertTrue(DataTableGrid.relationshipError(with(n,"sortColumnIndex",new PropertyValue.IntegerValue(BigInteger.TWO))).isPresent());
        var sizes=with(n,"availableRowsPerPage",new PropertyValue.StringValue("5, 20"));
        assertTrue(DataTableGrid.relationshipError(sizes).isEmpty());
        assertTrue(DataTableGrid.relationshipError(with(sizes,"onRowsPerPageChanged",new PropertyValue.StringValue("noop"))).isPresent());
        var primary=with(with(n,"controller",PaginatedDataTableWidgetPropertySchema.INITIAL_SOURCE),"primary",new PropertyValue.BooleanValue(true));
        assertTrue(DataTableGrid.relationshipError(primary).isPresent());
        assertTrue(DataTableGrid.relationshipError(DataTableGrid.withChildren(n,new SlotName("actions"),List.of(DataTableGrid.starterCell(StableId.random())))).isPresent());
        assertThrows(IllegalArgumentException.class,()->DataTableGrid.edit(n,TableGrid.Operation.ADD_ROW,0,0,StableId.random()));
    }
    @Test void columnEditsNeverSynthesizeSourceRowsAndPreserveSortIdentity() {
        var n=with(table(),"sortColumnIndex",new PropertyValue.IntegerValue(BigInteger.ONE));
        var before=DataTableGrid.children(n,DataTableGrid.COLUMNS).get(1);
        var edited=DataTableGrid.edit(n,TableGrid.Operation.MOVE_COLUMN,1,0,StableId.random());
        assertEquals(before,DataTableGrid.children(edited,DataTableGrid.COLUMNS).getFirst());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),edited.properties().get(new PropertyName("sortColumnIndex")));
        assertFalse(edited.slots().containsKey(DataTableGrid.ROWS));
        assertEquals(n.properties().get(new PropertyName("source")),edited.properties().get(new PropertyName("source")));
    }
    @Test void sourceScaffoldIsIdempotentUserOwnedAndNameFenced() {
        byte[] initial="import 'package:flutter/material.dart';\nclass Sample {}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] scaffold=DartEventHandlerSource.insertDataTableSource(initial);
        String text=new String(scaffold,java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("static final instance = _FlutterDesignerDataTableSource();"));
        assertTrue(text.contains("notifyListeners();"));assertTrue(text.contains("bool get isRowCountApproximate => false;"));
        assertArrayEquals(scaffold,DartEventHandlerSource.insertDataTableSource(scaffold));
        var edited=text.replace("bool get isRowCountApproximate => false;","bool get isRowCountApproximate => true;").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertArrayEquals(edited,DartEventHandlerSource.insertDataTableSource(edited));
        assertThrows(IllegalArgumentException.class,()->DartEventHandlerSource.insertDataTableSource(
            "final _FlutterDesignerDataTableSource = 2;".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
