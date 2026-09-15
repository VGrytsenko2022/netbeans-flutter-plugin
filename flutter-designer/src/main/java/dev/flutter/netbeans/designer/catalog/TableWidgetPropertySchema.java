package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Table, TableRow descriptor and TableCell APIs from Flutter 3.44.8. */
public final class TableWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.Table");
    public static final WidgetTypeId ROW = new WidgetTypeId("flutter.widgets.TableRow");
    public static final WidgetTypeId CELL = new WidgetTypeId("flutter.widgets.TableCell");
    public static final String ROW_TRAIT = "flutter.widgets.TableRow";
    public static final List<String> ALIGNMENTS = List.of("top","middle","bottom","baseline","fill","intrinsicHeight");
    public static final List<String> SIDES = List.of("all","inside","outside","top","right","bottom","left","horizontalInside","verticalInside");
    public static final String DESCRIPTION = "Native Flutter Table with structural TableRow children. All rows must have the same positive cell count. "
            + "Use the table grid editor for atomic row/column operations. TableCell controls individual vertical alignment. "
            + "Intrinsic widths and baseline alignment can be expensive. Flex/fraction widths require bounded horizontal constraints. "
            + "Canvas previews local values only; project-owned width, border and decoration sources are not executed.";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || ROW.equals(type) || CELL.equals(type); }
    public record Field(String name, String label, String group, String help) {}
    public static List<Field> fields(WidgetTypeId type) {
        var f = new ArrayList<Field>();
        f.add(new Field("key","Key","Identity","Optional literal String ValueKey or typed source. Row keys are LocalKey; cell keys must be unique across all rows."));
        if (TYPE.equals(type)) {
            f.add(new Field("columnWidths","Column widths","Columns","Indexed widths: fixed, flex, fraction, intrinsic, min or max; unspecified columns use Default column width."));
            f.add(new Field("defaultColumnWidth","Default column width","Columns","Native default FlexColumnWidth(1). Supports all six width families and strict TableColumnWidth sources."));
            f.add(new Field("textDirection","Text direction","Layout","Null/unset inherits Directionality; logical column zero starts on the leading side."));
            f.add(new Field("defaultVerticalAlignment","Default vertical alignment","Layout","Top by default; baseline requires Text baseline. An all-fill row has zero height."));
            f.add(new Field("textBaseline","Text baseline","Layout","Required when any effective cell alignment uses baseline."));
            f.add(new Field("border","Border","Border","Local all, symmetric or custom TableBorder, or a strict source; null/unset means no border."));
            f.add(new Field("borderRadius","Border radius","Border","Physical circular/elliptical corner radii; applies to native outer table border painting, not clipping."));
            for (String side : SIDES) for (String leaf : List.of("Color","Width","Style","StrokeAlign")) {
                if (side.equals("all") && leaf.equals("StrokeAlign")) continue; // TableBorder.all has no strokeAlign.
                f.add(new Field("border"+upper(side)+leaf,upper(side)+" "+leaf.toLowerCase(Locale.ROOT),"Border "+side,
                        "Active only for the matching border mode. Width is finite and non-negative; explicit zero is a hairline. "
                        + "An untouched side uses native BorderSide.none except all (black solid width one)."));
            }
        } else if (ROW.equals(type)) {
            f.add(new Field("decoration","Decoration","Row","Optional local BoxDecoration or strict Decoration source; row background paints behind all cells."));
        } else f.add(new Field("verticalAlignment","Vertical alignment","Cell","Null/unset inherits Table.defaultVerticalAlignment; all six native values supported."));
        return List.copyOf(f);
    }
    public static List<PropertyDefinition> properties(WidgetTypeId type) {
        var result = new ArrayList<PropertyDefinition>();
        int index = 0;
        for (var field : fields(type)) {
            String n = field.name();
            List<PropertyValueConstraint> c;
            if (n.equals("key")) c = List.of(new PropertyValueConstraint.StringLength(0,4096),
                    new PropertyValueConstraint.DartObjectReferenceValues(ROW.equals(type) ? "LocalKey?" : "Key?"), nil());
            else if (n.equals("columnWidths") || n.equals("defaultColumnWidth")) {
                c = new ArrayList<>(List.of(new PropertyValueConstraint.StringLength(0,16384),
                        new PropertyValueConstraint.DartObjectReferenceValues(n.equals("columnWidths") ? "Map<int, TableColumnWidth>?" : "TableColumnWidth")));
                if (n.equals("columnWidths")) c.add(nil());
            } else if (n.equals("border")) c = List.of(new PropertyValueConstraint.StringPattern("(?:all|symmetric|custom)", "Local TableBorder mode: all, symmetric or custom"),
                    new PropertyValueConstraint.DartObjectReferenceValues("TableBorder?"),nil());
            else if (n.equals("borderRadius")) c = List.of(new PropertyValueConstraint.BorderRadiusValues(false));
            else if (n.equals("decoration")) c = List.of(new PropertyValueConstraint.BoxDecorationValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),
                    new PropertyValueConstraint.DartObjectReferenceValues("Decoration?"),nil());
            else if (n.equals("textDirection")) c = List.of(en("TextDirection","ltr","rtl"),nil());
            else if (n.equals("textBaseline")) c = List.of(en("TextBaseline","alphabetic","ideographic"),nil());
            else if (n.endsWith("VerticalAlignment") || n.equals("verticalAlignment")) {
                c = new ArrayList<>(List.of(en("TableCellVerticalAlignment",ALIGNMENTS.toArray(String[]::new))));
                if (CELL.equals(type)) c.add(nil());
            } else if (n.endsWith("Color")) c = List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                    new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()));
            else if (n.endsWith("Style")) c = List.of(en("BorderStyle","none","solid"));
            else c = List.of(new PropertyValueConstraint.DoubleRange(n.endsWith("Width") ? BigDecimal.ZERO : null,true,null,true),
                        new PropertyValueConstraint.IntegerRange(n.endsWith("Width") ? BigInteger.ZERO : DartNumericLiterals.MAX_PORTABLE_INTEGER.negate(),
                                DartNumericLiterals.MAX_PORTABLE_INTEGER));
            result.add(new PropertyDefinition(new PropertyName(n),DartParameter.named(index++,false),c,Optional.empty()));
        }
        return List.copyOf(result);
    }
    public static boolean synthetic(String name) { return name.startsWith("border") && !name.equals("border"); }
    public static List<String> activeSides(String mode) {
        return switch (mode) { case "all" -> List.of("all"); case "symmetric" -> List.of("inside","outside");
            case "custom" -> SIDES.subList(3,SIDES.size()); default -> List.of(); };
    }
    public static String upper(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private static PropertyValueConstraint nil() { return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL); }
    private static PropertyValueConstraint en(String type,String... values) {
        return new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart",type),List.of(values));
    }
    private TableWidgetPropertySchema() {}
}
