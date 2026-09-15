package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Rectangular, immutable edits. Descriptor identities and source-owned row state survive moves. */
public final class DataTableGrid {
    public static final SlotName COLUMNS=new SlotName("columns"), ROWS=new SlotName("rows"), CELLS=new SlotName("cells");
    public static List<WidgetNode> children(WidgetNode node,SlotName slot) {
        return node.slots().get(slot) instanceof WidgetSlot.ListSlot list?list.children():List.of();
    }
    public static WidgetNode withChildren(WidgetNode node,SlotName slot,List<WidgetNode> children) {
        var slots=new LinkedHashMap<>(node.slots());slots.put(slot,new WidgetSlot.ListSlot(children));
        return new WidgetNode(node.id(),node.type(),node.properties(),slots,node.extensions(),node.stateBinding(),node.propertyBindings());
    }
    private static WidgetNode text(StableId owner,String value) {
        return new WidgetNode(TableGrid.derived(owner,"text"),new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),new PropertyValue.StringValue(value)),Map.of());
    }
    public static WidgetNode starterColumn(StableId id) {
        return new WidgetNode(id,DataTableWidgetPropertySchema.COLUMN,Map.of(),
                Map.of(new SlotName("label"),WidgetSlot.SingleSlot.of(text(id,"Column"))));
    }
    public static WidgetNode starterCell(StableId id) {
        return new WidgetNode(id,DataTableWidgetPropertySchema.CELL,Map.of(),
                Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(text(id,"Cell"))));
    }
    public static WidgetNode starterRow(StableId id,WidgetTypeId type,int columns) {
        if(!DataTableWidgetPropertySchema.row(type)||columns<1||columns>1000)
            throw new IllegalArgumentException("Data row creation requires 1..1000 cells and a DataRow constructor");
        var cells=new ArrayList<WidgetNode>();
        for(int i=0;i<columns;i++)cells.add(starterCell(TableGrid.derived(id,"cell:"+i)));
        // An unset byIndex.index is valid, but siblings require distinct keys; insertion supplies an unused index.
        return new WidgetNode(id,type,Map.of(),Map.of(CELLS,new WidgetSlot.ListSlot(cells)));
    }
    public static Map<SlotName,WidgetSlot> starterSlots(WidgetTypeId type,StableId id) {
        if(DataTableWidgetPropertySchema.TYPE.equals(type)) return Map.of(
                COLUMNS,new WidgetSlot.ListSlot(List.of(starterColumn(TableGrid.derived(id,"column:0")),starterColumn(TableGrid.derived(id,"column:1")))),
                ROWS,new WidgetSlot.ListSlot(List.of(starterRow(TableGrid.derived(id,"row:0"),DataTableWidgetPropertySchema.ROW,2),
                        starterRow(TableGrid.derived(id,"row:1"),DataTableWidgetPropertySchema.ROW,2))));
        if(DataTableWidgetPropertySchema.row(type)) return starterRow(id,type,2).slots();
        if(DataTableWidgetPropertySchema.COLUMN.equals(type)) return starterColumn(id).slots();
        if(DataTableWidgetPropertySchema.CELL.equals(type)) return starterCell(id).slots();
        return Map.of();
    }
    public static WidgetNode adaptFreshRow(WidgetNode row,WidgetNode table) {
        if(!row.equals(starterRow(row.id(),row.type(),2)))return row;
        var result=starterRow(row.id(),row.type(),children(table,COLUMNS).size());
        if(DataTableWidgetPropertySchema.ROW_INDEX.equals(row.type())) {
            var used=new HashSet<BigInteger>();
            for(var sibling:children(table,ROWS))if(DataTableWidgetPropertySchema.ROW_INDEX.equals(sibling.type())
                    && sibling.properties().get(new PropertyName("index")) instanceof PropertyValue.IntegerValue i)used.add(i.value());
            BigInteger index=BigInteger.ZERO;while(used.contains(index))index=index.add(BigInteger.ONE);
            result=withProperties(result,Map.of(new PropertyName("index"),new PropertyValue.IntegerValue(index)));
        }
        return result;
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        if(!DataTableWidgetPropertySchema.supports(node.type()))return Optional.empty();
        try {
            if(DataTableWidgetPropertySchema.COLUMN.equals(node.type())&&node.properties().get(new PropertyName("columnWidth")) instanceof PropertyValue.StringValue s)
                TableColumnWidths.parse(s.value());
            for(String family:DataTableWidgetPropertySchema.styleFamilies(node.type()))requireLocalLeaves(node,family,true);
            for(String family:DataTableWidgetPropertySchema.stateFamilies(node.type()))requireLocalLeaves(node,family,false);
            if(!DataTableWidgetPropertySchema.TYPE.equals(node.type()))return Optional.empty();
            var columns=children(node,COLUMNS);
            if(columns.isEmpty())return Optional.of("DataTable requires at least one column.");
            if(node.properties().get(new PropertyName("sortColumnIndex")) instanceof PropertyValue.IntegerValue i
                    &&(i.value().signum()<0||i.value().compareTo(BigInteger.valueOf(columns.size()))>=0))
                return Optional.of("Sort column index must identify an existing DataColumn.");
            boolean old=number(node,"dataRowHeight")!=null,min=number(node,"dataRowMinHeight")!=null,max=number(node,"dataRowMaxHeight")!=null;
            if(old&&(min||max))return Optional.of("Data row height cannot be combined with minimum/maximum row height.");
            if(min&&max&&number(node,"dataRowMinHeight").compareTo(number(node,"dataRowMaxHeight"))>0)
                return Optional.of("Minimum data row height exceeds maximum data row height.");
            var border=node.properties().get(new PropertyName("border"));
            String mode=border instanceof PropertyValue.StringValue s?s.value():"";
            for(var name:node.properties().keySet())if(TableWidgetPropertySchema.synthetic(name.value())
                    &&(mode.isEmpty()||!name.value().equals("borderRadius")&&TableWidgetPropertySchema.activeSides(mode).stream()
                            .noneMatch(side->name.value().startsWith("border"+TableWidgetPropertySchema.upper(side)))))
                return Optional.of(name+" requires its matching local border mode.");
            var keys=new HashSet<Object>();
            for(var row:children(node,ROWS)) {
                if(children(row,CELLS).size()!=columns.size())return Optional.of("Every DataRow must have exactly "+columns.size()+" cells. Use Rows and columns for coordinated edits.");
                Object key=null;
                if(DataTableWidgetPropertySchema.ROW_INDEX.equals(row.type())) {
                    var value=row.properties().get(new PropertyName("index"));
                    key=value instanceof PropertyValue.IntegerValue i?List.of("int?",i.value()):List.of("int?","null");
                } else if(row.properties().get(new PropertyName("key")) instanceof PropertyValue.StringValue s)key=List.of("String",s.value());
                if(key!=null&&!keys.add(key))return Optional.of("Duplicate known DataRow key at "+row.id()+". Assign distinct keys/indices.");
            }
            return Optional.empty();
        }catch(IllegalArgumentException ex){return Optional.of("Invalid "+node.type()+" "+node.id()+": "+ex.getMessage());}
    }
    private static void requireLocalLeaves(WidgetNode node,String family,boolean style) {
        boolean local=node.properties().get(new PropertyName(family)) instanceof PropertyValue.StringValue s&&s.value().equals("local");
        for(var name:node.properties().keySet())if((style?DataTableWidgetPropertySchema.styleFamily(node.type(),name.value()):
                DataTableWidgetPropertySchema.stateFamily(node.type(),name.value())).filter(family::equals).isPresent()&&!local)
            throw new IllegalArgumentException(name+" requires local "+family+" mode");
    }
    private static BigDecimal number(WidgetNode node,String name) {
        var p=node.properties().get(new PropertyName(name));
        if(p instanceof PropertyValue.IntegerValue i)return new BigDecimal(i.value());
        if(p instanceof PropertyValue.DoubleValue d)return d.value();
        return null;
    }
    private static WidgetNode withProperties(WidgetNode node,Map<PropertyName,PropertyValue> properties) {
        return new WidgetNode(node.id(),node.type(),properties,node.slots(),node.extensions(),node.stateBinding(),node.propertyBindings());
    }
    public static WidgetNode edit(WidgetNode table,TableGrid.Operation operation,int index,int destination,StableId seed) {
        if(!DataTableWidgetPropertySchema.TYPE.equals(table.type()))throw new IllegalArgumentException("Rows and columns requires DataTable");
        relationshipError(table).ifPresent(s->{throw new IllegalArgumentException(s);});
        var rows=new ArrayList<>(children(table,ROWS));var columns=new ArrayList<>(children(table,COLUMNS));
        boolean row=operation.name().endsWith("ROW"),add=operation.name().startsWith("ADD_"),move=operation.name().startsWith("MOVE_");
        if (!row && table.propertyBindings().containsKey(new PropertyName("sortColumnIndex")))
            throw new IllegalArgumentException("Column edits cannot remap a State-owned sortColumnIndex. Detach that binding, edit the columns, then update the State index before rebinding.");
        int count=row?rows.size():columns.size();
        if(index<0||index>count||!add&&index==count)throw new IllegalArgumentException("Data table index is out of range");
        if(add&&count>=1000)throw new IllegalArgumentException("Data table editor limit is 1000 rows/columns");
        if(move&&(destination<0||destination>=count))throw new IllegalArgumentException("Data table destination is out of range");
        if(operation==TableGrid.Operation.REMOVE_COLUMN&&columns.size()==1)throw new IllegalArgumentException("Cannot remove the last DataColumn");
        if(row) {
            if(add)rows.add(index,starterRow(TableGrid.derived(seed,"row"),DataTableWidgetPropertySchema.ROW,columns.size()));
            else if(move)move(rows,index,destination);else rows.remove(index);
        } else {
            if(add)columns.add(index,starterColumn(TableGrid.derived(seed,"column")));
            else if(move)move(columns,index,destination);else columns.remove(index);
            for(int r=0;r<rows.size();r++) {
                var cells=new ArrayList<>(children(rows.get(r),CELLS));
                if(add)cells.add(index,starterCell(TableGrid.derived(seed,"cell:"+rows.get(r).id())));
                else if(move)move(cells,index,destination);else cells.remove(index);
                rows.set(r,withChildren(rows.get(r),CELLS,cells));
            }
        }
        var properties=new LinkedHashMap<>(table.properties());
        if(!row&&properties.get(new PropertyName("sortColumnIndex")) instanceof PropertyValue.IntegerValue n) {
            int old=n.value().intValueExact();
            if(operation==TableGrid.Operation.REMOVE_COLUMN&&old==index)properties.remove(new PropertyName("sortColumnIndex"));
            else {
                int mapped=add?(old>=index?old+1:old):!move?(old>index?old-1:old):
                        old==index?destination:index<destination&&old>index&&old<=destination?old-1:
                        index>destination&&old>=destination&&old<index?old+1:old;
                properties.put(new PropertyName("sortColumnIndex"),new PropertyValue.IntegerValue(BigInteger.valueOf(mapped)));
            }
        }
        return withChildren(withChildren(withProperties(table,properties),COLUMNS,columns),ROWS,rows);
    }
    private static <T> void move(List<T> values,int index,int destination){values.add(destination,values.remove(index));}
    private DataTableGrid(){}
}
