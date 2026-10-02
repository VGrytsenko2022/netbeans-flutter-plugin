package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Flutter 3.44.8 DataTable and all descriptor constructors, including byIndex and empty. */
public final class DataTableWidgetPropertySchema {
    public static final WidgetTypeId TYPE=new WidgetTypeId("flutter.material.DataTable");
    public static final WidgetTypeId COLUMN=new WidgetTypeId("flutter.material.DataColumn");
    public static final WidgetTypeId ROW=new WidgetTypeId("flutter.material.DataRow");
    public static final WidgetTypeId ROW_INDEX=new WidgetTypeId("flutter.material.DataRow.byIndex");
    public static final WidgetTypeId CELL=new WidgetTypeId("flutter.material.DataCell");
    public static final WidgetTypeId EMPTY=new WidgetTypeId("flutter.material.DataCell.empty");
    public static final List<WidgetTypeId> TYPES=List.of(TYPE,COLUMN,ROW,ROW_INDEX,CELL,EMPTY);
    public static final String COLUMN_TRAIT=COLUMN.value(), ROW_TRAIT=ROW.value(), CELL_TRAIT=CELL.value();
    public static final String DESCRIPTION="Native Material data table with typed column, row and cell descriptors. "
            +"Use Rows and columns for atomic rectangular edits. Sorting and selection are controlled: callbacks do not mutate saved rows or flags. "
            +"Project callbacks, colors, cursors, widths and decorations are never executed by the isolated Canvas.";
    public record Field(String name,String label,String group,String help) {}
    public static boolean supports(WidgetTypeId type) { return TYPES.contains(type)||PaginatedDataTableWidgetPropertySchema.TYPE.equals(type); }
    public static boolean table(WidgetTypeId type) { return TYPE.equals(type)||PaginatedDataTableWidgetPropertySchema.TYPE.equals(type); }
    public static boolean row(WidgetTypeId type) { return ROW.equals(type)||ROW_INDEX.equals(type); }
    public static boolean cell(WidgetTypeId type) { return CELL.equals(type)||EMPTY.equals(type); }
    public static boolean descriptor(WidgetTypeId type) { return supports(type)&&!table(type); }
    public static List<String> styleFamilies(WidgetTypeId type) { return TYPE.equals(type)?List.of("dataTextStyle","headingTextStyle"):List.of(); }
    public static List<String> stateFamilies(WidgetTypeId type) {
        return PaginatedDataTableWidgetPropertySchema.TYPE.equals(type)?List.of("headingRowColor"):TYPE.equals(type)?List.of("dataRowColor","headingRowColor"):COLUMN.equals(type)?List.of("mouseCursor"):row(type)?List.of("color","mouseCursor"):List.of();
    }
    public static Map<String,String> callbacks(WidgetTypeId type) {
        var result=new LinkedHashMap<String,String>();
        if(table(type)) result.put("onSelectAll","ValueSetter<bool?>");
        if(PaginatedDataTableWidgetPropertySchema.TYPE.equals(type)) { result.put("onPageChanged","ValueChanged<int>");result.put("onRowsPerPageChanged","ValueChanged<int?>"); }
        if(COLUMN.equals(type)) result.put("onSort","DataColumnSortCallback");
        if(row(type)) { result.put("onSelectChanged","ValueChanged<bool?>");result.put("onLongPress","GestureLongPressCallback");result.put("onHover","ValueChanged<bool>"); }
        if(CELL.equals(type)) {
            result.put("onTap","GestureTapCallback");result.put("onLongPress","GestureLongPressCallback");result.put("onTapDown","GestureTapDownCallback");
            result.put("onDoubleTap","GestureTapCallback");result.put("onTapCancel","GestureTapCancelCallback");
        }
        return Collections.unmodifiableMap(result);
    }
    public static List<PropertyDefinition> properties(WidgetTypeId type) {
        if(PaginatedDataTableWidgetPropertySchema.TYPE.equals(type))return PaginatedDataTableWidgetPropertySchema.properties();
        var p=new ArrayList<PropertyDefinition>();
        if(EMPTY.equals(type)) return List.of();
        if(TYPE.equals(type)||ROW.equals(type)) add(p,"key",List.of(new PropertyValueConstraint.StringLength(0,4096),
                ref(ROW.equals(type)?"LocalKey?":"Key?"),nil()));
        if(ROW_INDEX.equals(type)) add(p,"index",List.of(new PropertyValueConstraint.IntegerRange(DartNumericLiterals.MAX_PORTABLE_INTEGER.negate(),DartNumericLiterals.MAX_PORTABLE_INTEGER),nil()));
        if(TYPE.equals(type)) {
            add(p,"sortColumnIndex",List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,BigInteger.valueOf(9999)),nil()));
            for(String n:List.of("sortAscending","showCheckboxColumn","showBottomBorder")) add(p,n,bool());
            add(p,"decoration",TableWidgetPropertySchema.properties(TableWidgetPropertySchema.ROW).stream().filter(v->v.name().value().equals("decoration")).findFirst().orElseThrow().constraints());
            for(String n:List.of("dataRowHeight","dataRowMinHeight","dataRowMaxHeight","headingRowHeight","horizontalMargin","columnSpacing","dividerThickness","checkboxHorizontalMargin"))
                add(p,n,List.of(new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO,true,null,true),new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,DartNumericLiterals.MAX_PORTABLE_INTEGER),nil()));
            add(p,"clipBehavior",List.of(en("Clip","none","hardEdge","antiAlias","antiAliasWithSaveLayer")));
            for(var source:TableWidgetPropertySchema.properties(TableWidgetPropertySchema.TYPE)) if(source.name().value().startsWith("border")) add(p,source.name().value(),source.constraints());
            for(String family:styleFamilies(type)) {
                add(p,family,List.of(new PropertyValueConstraint.StringPattern("local","Local TextStyle fields"),ref("TextStyle?"),nil()));
                for(var leaf:BuiltInWidgetCatalog.text().properties()) if(AnimatedDefaultTextStyleWidgetPropertySchema.styleLeaf(leaf.name()))
                    add(p,family+leaf.name().value().substring("style".length()),leaf.constraints());
            }
        }
        if(COLUMN.equals(type)) {
            add(p,"columnWidth",List.of(new PropertyValueConstraint.StringLength(0,16384),ref("TableColumnWidth?"),nil()));
            add(p,"tooltip",List.of(new PropertyValueConstraint.StringLength(0,16384),nil()));
            add(p,"numeric",bool());
            add(p,"headingRowAlignment",List.of(en("MainAxisAlignment","start","end","center","spaceBetween","spaceAround","spaceEvenly"),nil()));
        }
        if(row(type)) add(p,"selected",bool());
        if(CELL.equals(type)) for(String n:List.of("placeholder","showEditIcon")) add(p,n,bool());
        for(String family:stateFamilies(type)) {
            add(p,family,List.of(new PropertyValueConstraint.StringPattern("local","Local WidgetStateProperty map"),
                    ref(family.equals("mouseCursor")?"WidgetStateProperty<MouseCursor?>?":"WidgetStateProperty<Color?>?"),nil()));
            for(String state:CheckboxWidgetPropertySchema.statePrefixes()) {
                List<PropertyValueConstraint> c=family.equals("mouseCursor")
                        ?List.of(new PropertyValueConstraint.StringPattern("(?:"+String.join("|",DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets())+")","SystemMouseCursors preset"),nil())
                        :List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                            new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),nil());
                add(p,family+state,c);
            }
        }
        callbacks(type).forEach((name,callback)->add(p,name,List.of(new PropertyValueConstraint.StringPattern("noop","Explicit controlled no-op callback"),ref(callback),nil())));
        return List.copyOf(p);
    }
    public static List<Field> fields(WidgetTypeId type) {
        return properties(type).stream().map(p->new Field(p.name().value(),label(p.name().value()),group(type,p.name().value()),
                PaginatedDataTableWidgetPropertySchema.TYPE.equals(type)?PaginatedDataTableWidgetPropertySchema.help(p.name().value()):help(type,p.name().value()))).toList();
    }
    public static Optional<String> styleFamily(WidgetTypeId type,String name) {
        return styleFamilies(type).stream().filter(f->!f.equals(name)&&name.startsWith(f)).findFirst();
    }
    public static Optional<String> stateFamily(WidgetTypeId type,String name) {
        return stateFamilies(type).stream().filter(f->!f.equals(name)&&CheckboxWidgetPropertySchema.statePrefixes().stream().anyMatch(s->name.equals(f+s))).findFirst();
    }
    public static Optional<TextWidgetPropertySchema.Definition> styleBinding(WidgetTypeId type,PropertyName name) {
        return styleFamily(type,name.value()).flatMap(f->TextWidgetPropertySchema.find(new PropertyName("style"+name.value().substring(f.length()))));
    }
    public static String group(WidgetTypeId type,String n) {
        if(callbacks(type).containsKey(n)) return "Callbacks";
        if(n.startsWith("border")) return "Border";
        if(styleFamily(type,n).isPresent()||styleFamilies(type).contains(n)) return n.startsWith("heading")?"Heading text style":"Data text style";
        if(stateFamily(type,n).isPresent()||stateFamilies(type).contains(n)) return n.startsWith("mouseCursor")?"Mouse cursor states":n.startsWith("heading")?"Heading color states":"Row color states";
        if(n.equals("key")||n.equals("index")) return "Identity";
        return "Data table";
    }
    public static String help(WidgetTypeId type,String n) {
        if(n.equals("dataRowHeight")) return "Deprecated SDK shorthand. Mutually exclusive with minimum/maximum row height; prefer the two current fields.";
        if(n.equals("sortColumnIndex")) return "Nullable zero-based column index. Column edits remap a local index; detach a State binding before editing columns. This does not reorder data rows.";
        if(n.equals("sortAscending")) return "Controls the sort indicator; onSort owns actual sorting and state updates.";
        if(n.equals("selected")) return "Controlled row selection flag. onSelectChanged must update application state; preview no-ops do not change this value.";
        if(n.equals("columnWidth")) return "All six native width strategies or strict TableColumnWidth? source. Null/unset preserves native intrinsic sizing.";
        if(n.equals("showEditIcon")) return "Displays a pencil, not an editor. Implement editing in onTap.";
        if(callbacks(type).containsKey(n)) return "Native "+callbacks(type).get(n)+" event; use Events to create, select, rename or navigate to a handler. Null/unset disables this callback.";
        if(styleFamily(type,n).isPresent()) return styleBinding(type,new PropertyName(n)).map(TextWidgetPropertySchema.Definition::description).orElse("");
        if(stateFamily(type,n).isPresent()) return "First matching WidgetState wins, including explicit null; Default is the fallback. Editing a local entry selects local mode.";
        return "Native "+label(n)+". Omission preserves the SDK default. Source-backed values are analyzer-checked and not executed in Canvas.";
    }
    private static String label(String n) {
        String words=n.replaceAll("([a-z])([A-Z])","$1 $2");
        return Character.toUpperCase(words.charAt(0))+words.substring(1);
    }
    private static List<PropertyValueConstraint> bool() { return List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)); }
    private static PropertyValueConstraint ref(String type) { return new PropertyValueConstraint.DartObjectReferenceValues(type); }
    private static PropertyValueConstraint nil() { return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL); }
    private static PropertyValueConstraint en(String type,String...v) { return new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart",type),List.of(v)); }
    private static void add(List<PropertyDefinition> p,String name,List<PropertyValueConstraint> c) { p.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(p.size(),false),c,Optional.empty())); }
    private DataTableWidgetPropertySchema() {}
}
