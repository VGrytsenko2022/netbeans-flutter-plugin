package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TableContractTest {
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static WidgetNode table() { return WidgetNodePrototypeFactory.create(C.find(TableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random()); }
    static WidgetNode with(WidgetNode n,String key,PropertyValue value) {
        var p=new LinkedHashMap<>(n.properties()); p.put(new PropertyName(key),value);
        return new WidgetNode(n.id(),n.type(),p,n.slots());
    }
    static DesignerDocument doc(WidgetNode n) { return RotationTransitionContractTest.document(n); }
    static GeneratedDartRegions generated(WidgetNode n) {
        var result=new DartRegionGenerator().generate(doc(n),C);
        assertTrue(result.successful(),result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    @Test void nativeTableRowsAndCellPlacement() {
        var table=table(); assertTrue(new WidgetTreeValidator().validate(doc(table),C).valid());
        assertEquals(2,TableGrid.children(table).size());
        assertEquals(2,TableGrid.children(TableGrid.children(table).getFirst()).size());
        assertTrue(generated(table).build().payload().contains("TableRow("));
        assertFalse(C.find(TableWidgetPropertySchema.TYPE).orElseThrow().constConstructor());
        for (var special:List.of(TableWidgetPropertySchema.ROW,TableWidgetPropertySchema.CELL)) {
            var child=C.find(special).orElseThrow();
            assertFalse(WidgetPlacementRules.evaluateRoot(child).accepted());
            for (var owner:C.definitions()) for (var slot:owner.slots()) {
                boolean expected=slot.name().value().equals("children") && owner.typeId().equals(
                        special.equals(TableWidgetPropertySchema.ROW) ? TableWidgetPropertySchema.TYPE : TableWidgetPropertySchema.ROW);
                assertEquals(expected,WidgetPlacementRules.accepts(owner,slot,child),owner.typeId()+"."+slot.name());
            }
        }
    }
    @Test void widthLanguageAllFamiliesAndAdversarialLimits() {
        for (String source:List.of("fixed(0)","fixed(1e300)","fixed(1e-300)","fixed(1e-999)","flex(2)","fraction(0.25)","intrinsic()","intrinsic(1)",
                "min(fixed(40),max(fraction(0.5),intrinsic(2)))")) {
            var parsed=TableColumnWidths.parse(source);
            assertEquals(parsed,TableColumnWidths.parse(parsed.encode()));
            var n=with(table(),"defaultColumnWidth",new PropertyValue.StringValue(source));
            String dart=generated(n).build().payload();
            assertTrue(dart.contains("ColumnWidth("),dart);
            assertFalse(dart.contains("'"+source+"'"));
        }
        for (String bad:List.of("","evil()","flex(0)","intrinsic(0)","fixed(-1)","fixed(NaN)","fixed(1e999)",
                "fixed(1e-999999999)","fixed(.001e-999)","fixed(1);print('x')","min(fixed(1))","max(fixed(1),fixed(2),fixed(3))","fixed(1) trailing",
                "max(".repeat(10)+"fixed(1)"+",fixed(1))".repeat(10)))
            assertThrows(IllegalArgumentException.class,()->TableColumnWidths.parse(bad),bad);
        assertEquals("0=flex(1);2=fixed(40)",TableColumnWidths.encodeMap(TableColumnWidths.parseMap("2=fixed(40); 0=flex(1)")));
        for(String bad:List.of("0=flex(1);0=fixed(2)","10000=fixed(1)","-1=fixed(1)","1=fixed(2);"))
            assertThrows(IllegalArgumentException.class,()->TableColumnWidths.parseMap(bad),bad);
    }
    @Test void generatedWidthMapBordersKeyAndRoundTrip() throws Exception {
        var base=with(with(table(),"columnWidths",new PropertyValue.StringValue("0=fixed(80);1=intrinsic(1)")),
                "key",new PropertyValue.StringValue("grid"));
        for(String mode:List.of("all","symmetric","custom")) {
            var n=with(base,"border",new PropertyValue.StringValue(mode));
            var result=generated(n); String dart=result.build().payload();
            assertTrue(dart.contains("ValueKey('grid')"),dart);
            assertTrue(dart.contains("TableBorder("),dart);
            assertTrue(dart.contains("0: const FixedColumnWidth(80.0)"),dart);
            var ids=result.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).toList();
            assertEquals(ids.size(),new HashSet<>(ids).size(),"Proof IDs must be unique");
            var codec=new FdDocumentCodec();
            var document=doc(n);
            assertEquals(document,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(document))).document());
        }
    }
    @Test void atomicGridPreservesIdsAndRemapsWidths() {
        var n=with(table(),"columnWidths",new PropertyValue.StringValue("0=fixed(20);1=flex(2)"));
        var original=TableGrid.children(n);
        n=TableGrid.edit(n,TableGrid.Operation.ADD_COLUMN,1,0,StableId.random());
        assertEquals(3,TableGrid.children(TableGrid.children(n).getFirst()).size());
        assertEquals("0=fixed(20);2=flex(2)",((PropertyValue.StringValue)n.properties().get(new PropertyName("columnWidths"))).value());
        for(int r=0;r<2;r++) {
            assertEquals(original.get(r).id(),TableGrid.children(n).get(r).id());
            assertEquals(TableGrid.children(original.get(r)).get(1),TableGrid.children(TableGrid.children(n).get(r)).get(2));
        }
        n=TableGrid.edit(n,TableGrid.Operation.MOVE_COLUMN,2,0,StableId.random());
        assertEquals("0=flex(2);1=fixed(20)",((PropertyValue.StringValue)n.properties().get(new PropertyName("columnWidths"))).value());
        n=TableGrid.edit(n,TableGrid.Operation.REMOVE_COLUMN,1,0,StableId.random());
        assertTrue(TableGrid.relationshipError(n).isEmpty());
        n=TableGrid.edit(n,TableGrid.Operation.ADD_ROW,1,0,StableId.random());
        assertEquals(3,TableGrid.children(n).size());
        assertEquals(2,TableGrid.children(TableGrid.children(n).get(1)).size());
        n=TableGrid.edit(n,TableGrid.Operation.MOVE_ROW,2,0,StableId.random());
        assertEquals(original.get(1).id(),TableGrid.children(n).getFirst().id());
    }
    @Test void rejectsRaggedRowsEmptyRowsBaselineAndInactiveBorderLeaves() {
        var n=table(); var rows=new ArrayList<>(TableGrid.children(n));
        rows.set(0,TableGrid.withChildren(rows.getFirst(),List.of()));
        assertTrue(TableGrid.relationshipError(TableGrid.withChildren(n,rows)).isPresent());
        assertTrue(TableGrid.relationshipError(with(n,"defaultVerticalAlignment",new PropertyValue.EnumValue("TableCellVerticalAlignment","baseline"))).isPresent());
        assertTrue(TableGrid.relationshipError(with(n,"borderAllWidth",new PropertyValue.DoubleValue(java.math.BigDecimal.ONE))).isPresent());
        var empty=TableGrid.withChildren(n,List.of()); assertTrue(TableGrid.relationshipError(empty).isEmpty());
        assertEquals(1,TableGrid.children(TableGrid.edit(empty,TableGrid.Operation.ADD_ROW,0,0,StableId.random())).size());
    }
}
