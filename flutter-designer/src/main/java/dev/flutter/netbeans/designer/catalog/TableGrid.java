package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable rectangular table operations; unchanged row/cell identities are retained. */
public final class TableGrid {
    public enum Operation { ADD_ROW, REMOVE_ROW, MOVE_ROW, ADD_COLUMN, REMOVE_COLUMN, MOVE_COLUMN }
    public static final SlotName CHILDREN = new SlotName("children");
    public static List<WidgetNode> children(WidgetNode node) {
        return node.slots().get(CHILDREN) instanceof WidgetSlot.ListSlot list ? list.children() : List.of();
    }
    public static WidgetNode withChildren(WidgetNode node, List<WidgetNode> children) {
        var slots = new LinkedHashMap<>(node.slots()); slots.put(CHILDREN,new WidgetSlot.ListSlot(children));
        return new WidgetNode(node.id(),node.type(),node.properties(),slots,node.extensions(),node.stateBinding(),node.propertyBindings());
    }
    public static WidgetNode starterRow(StableId id, int columns) {
        if (columns < 1 || columns > 1000) throw new IllegalArgumentException("A table row needs 1..1000 cells");
        var cells = new ArrayList<WidgetNode>();
        for (int i=0;i<columns;i++) cells.add(starterCell(derived(id,"cell:"+i)));
        return new WidgetNode(id,TableWidgetPropertySchema.ROW,Map.of(),Map.of(CHILDREN,new WidgetSlot.ListSlot(cells)));
    }
    public static WidgetNode starterCell(StableId id) {
        var size = new PropertyValue.IntegerValue(BigInteger.valueOf(48));
        return new WidgetNode(id,new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"),size,new PropertyName("height"),size),
                Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
    }
    public static StableId derived(StableId id,String suffix) {
        return new StableId(UUID.nameUUIDFromBytes(("flutter-designer:Table:"+id+":"+suffix).getBytes(StandardCharsets.UTF_8)));
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        if (!TableWidgetPropertySchema.TYPE.equals(node.type())) return Optional.empty();
        try {
            for (String field : List.of("columnWidths","defaultColumnWidth")) {
                if (node.properties().get(new PropertyName(field)) instanceof PropertyValue.StringValue value) {
                    if (field.equals("columnWidths")) TableColumnWidths.parseMap(value.value());
                    else TableColumnWidths.parse(value.value());
                }
            }
            PropertyValue border = node.properties().get(new PropertyName("border"));
            String mode = border instanceof PropertyValue.StringValue v ? v.value() : "";
            for (var name : node.properties().keySet()) if (TableWidgetPropertySchema.synthetic(name.value())) {
                boolean active = !mode.isEmpty() && (name.value().equals("borderRadius")
                        || TableWidgetPropertySchema.activeSides(mode).stream()
                           .anyMatch(side -> name.value().startsWith("border"+TableWidgetPropertySchema.upper(side))));
                if (!active) return Optional.of("Table " + node.id() + ": " + name + " requires its matching local Border mode.");
            }
            var rows = children(node);
            int width = rows.isEmpty() ? 0 : children(rows.getFirst()).size();
            var rowKeys = new HashSet<PropertyValue>(); var cellKeys = new HashSet<PropertyValue>();
            boolean hasBaseline = node.properties().get(new PropertyName("textBaseline")) instanceof PropertyValue.EnumValue;
            String inherited = node.properties().get(new PropertyName("defaultVerticalAlignment")) instanceof PropertyValue.EnumValue v ? v.value() : "top";
            if (!hasBaseline && inherited.equals("baseline")) return Optional.of("Table baseline alignment requires Text baseline.");
            for (var row : rows) {
                if (width == 0 || children(row).size() != width)
                    return Optional.of("Table " + node.id() + ": every row must contain the same positive cell count. Use the table grid editor to add/remove columns.");
                var key = row.properties().get(new PropertyName("key"));
                if (key instanceof PropertyValue.StringValue && !rowKeys.add(key)) return Optional.of("Duplicate TableRow key on row " + row.id());
                for (var cell : children(row)) {
                    key = cell.properties().get(new PropertyName("key"));
                    if (key instanceof PropertyValue.StringValue && !cellKeys.add(key)) return Optional.of("Duplicate cell key across Table rows on " + cell.id());
                    String alignment = cell.properties().get(new PropertyName("verticalAlignment")) instanceof PropertyValue.EnumValue v ? v.value() : inherited;
                    if (TableWidgetPropertySchema.CELL.equals(cell.type()) && !hasBaseline && alignment.equals("baseline"))
                        return Optional.of("TableCell " + cell.id() + ": baseline alignment requires Text baseline on its Table.");
                }
            }
            return Optional.empty();
        } catch (IllegalArgumentException ex) { return Optional.of("Invalid Table " + node.id() + ": " + ex.getMessage()); }
    }
    public static WidgetNode edit(WidgetNode table,Operation operation,int index,int destination,StableId seed) {
        if (!TableWidgetPropertySchema.TYPE.equals(table.type())) throw new IllegalArgumentException("Grid edits require a Table target");
        relationshipError(table).ifPresent(s -> { throw new IllegalArgumentException(s); });
        var rows = new ArrayList<>(children(table));
        int columns = rows.isEmpty() ? 0 : children(rows.getFirst()).size();
        boolean rowOperation = operation.name().endsWith("ROW");
        int count = rowOperation ? rows.size() : columns;
        boolean add = operation == Operation.ADD_ROW || operation == Operation.ADD_COLUMN;
        if (index < 0 || index > count || !add && index == count) throw new IllegalArgumentException("Table grid index is out of range");
        if (add && count >= 1000) throw new IllegalArgumentException("Table grid editor limit is 1000 rows/columns");
        if (!rowOperation && rows.isEmpty()) throw new IllegalArgumentException("Add a row before editing columns");
        if (operation == Operation.REMOVE_COLUMN && columns == 1) throw new IllegalArgumentException("Cannot remove the last column; remove rows or clear the Table instead");
        var properties = new LinkedHashMap<>(table.properties());
        if (!rowOperation) {
            var widths = properties.get(new PropertyName("columnWidths"));
            if (widths instanceof PropertyValue.DartObjectReferenceValue)
                throw new IllegalArgumentException("Column widths are owned by source; use local widths before changing columns, then update the source mapping explicitly");
            if (widths instanceof PropertyValue.StringValue s) {
                var remapped = new TreeMap<Integer,TableColumnWidths.Width>();
                for (var entry : TableColumnWidths.parseMap(s.value()).entrySet()) {
                    int old = entry.getKey();
                    if (operation == Operation.REMOVE_COLUMN && old == index) continue;
                    int mapped = switch (operation) {
                        case ADD_COLUMN -> old >= index ? old+1 : old;
                        case REMOVE_COLUMN -> old > index ? old-1 : old;
                        case MOVE_COLUMN -> old == index ? destination
                            : index < destination && old > index && old <= destination ? old-1
                            : index > destination && old >= destination && old < index ? old+1 : old;
                        default -> old;
                    };
                    if (mapped > TableColumnWidths.MAX_INDEX) throw new IllegalArgumentException("Shifted column width index exceeds 9999");
                    remapped.put(mapped,entry.getValue());
                }
                properties.put(new PropertyName("columnWidths"),new PropertyValue.StringValue(TableColumnWidths.encodeMap(remapped)));
            }
        }
        if (operation == Operation.ADD_ROW) rows.add(index,starterRow(derived(seed,"row"),columns == 0 ? 2 : columns));
        else if (operation == Operation.REMOVE_ROW) rows.remove(index);
        else if (operation == Operation.MOVE_ROW) move(rows,index,destination);
        else for (int r=0;r<rows.size();r++) {
            var row=rows.get(r); var cells=new ArrayList<>(children(row));
            if (operation == Operation.ADD_COLUMN) cells.add(index,starterCell(derived(seed,"row:"+row.id())));
            else if (operation == Operation.REMOVE_COLUMN) cells.remove(index);
            else move(cells,index,destination);
            rows.set(r,withChildren(row,cells));
        }
        return withChildren(new WidgetNode(table.id(),table.type(),properties,table.slots(),table.extensions(),table.stateBinding(),table.propertyBindings()),rows);
    }
    private static <T> void move(List<T> values,int index,int destination) {
        if (destination < 0 || destination >= values.size()) throw new IllegalArgumentException("Post-removal destination is out of range");
        T value=values.remove(index); values.add(destination,value);
    }
    private TableGrid() {}
}
