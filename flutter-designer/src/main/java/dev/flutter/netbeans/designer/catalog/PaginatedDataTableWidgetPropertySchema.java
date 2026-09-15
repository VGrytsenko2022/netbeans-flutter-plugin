package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** All Flutter 3.44.8 PaginatedDataTable arguments. Source is application-owned, not an FD row slot. */
public final class PaginatedDataTableWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.PaginatedDataTable");
    public static final String SOURCE_CLASS = "_FlutterDesignerDataTableSource";
    public static final PropertyValue.DartObjectReferenceValue INITIAL_SOURCE =
            new PropertyValue.DartObjectReferenceValue(Optional.empty(), SOURCE_CLASS, Optional.of("instance"),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    public static final String DESCRIPTION = "Native paginated Material table. Columns, header and actions are visual slots; "
            + "DataTableSource owns rows, selection, sorting, approximate counts and notifyListeners. "
            + "First insertion creates an editable empty source outside managed regions, with a stable application-lifetime instance. "
            + "It is not recreated by build. Replace Source with your own long-lived instance for independent tables; its owner disposes it. "
            + "Canvas never executes project sources, callbacks or controllers and explicitly previews zero data rows.";
    public static final Set<String> SHARED = Set.of("key", "sortColumnIndex", "sortAscending", "onSelectAll",
            "dataRowHeight", "dataRowMinHeight", "dataRowMaxHeight", "headingRowHeight", "horizontalMargin",
            "columnSpacing", "showCheckboxColumn", "checkboxHorizontalMargin", "dividerThickness");
    public static List<PropertyDefinition> properties() {
        var p = new ArrayList<PropertyDefinition>();
        for (var field : DataTableWidgetPropertySchema.properties(DataTableWidgetPropertySchema.TYPE)) {
            String name = field.name().value();
            if (!SHARED.contains(name) && !name.startsWith("headingRowColor")) continue;
            var constraints = field.constraints();
            if (Set.of("headingRowHeight", "horizontalMargin", "columnSpacing").contains(name))
                constraints = constraints.stream().filter(c -> c.kind() != PropertyValueKind.NULL).toList();
            add(p, name, constraints);
        }
        for (String name : List.of("showFirstLastButtons", "showEmptyRows")) add(p, name, List.of(any(PropertyValueKind.BOOLEAN)));
        add(p, "initialFirstRowIndex", List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER), any(PropertyValueKind.NULL)));
        add(p, "rowsPerPage", List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER)));
        add(p, "availableRowsPerPage", List.of(new PropertyValueConstraint.StringLength(0, 16384)));
        for (String name : List.of("onPageChanged", "onRowsPerPageChanged"))
            add(p, name, List.of(new PropertyValueConstraint.StringPattern("noop", "Explicit controlled no-op callback"),
                    new PropertyValueConstraint.DartObjectReferenceValues(name.equals("onPageChanged") ? "ValueChanged<int>" : "ValueChanged<int?>"), any(PropertyValueKind.NULL)));
        add(p, "dragStartBehavior", List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference("package:flutter/gestures.dart", "DragStartBehavior"), List.of("start", "down"))));
        add(p, "arrowHeadColor", List.of(any(PropertyValueKind.COLOR),
                new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()), any(PropertyValueKind.NULL)));
        add(p, "controller", List.of(new PropertyValueConstraint.DartObjectReferenceValues("ScrollController"), any(PropertyValueKind.NULL)));
        add(p, "primary", List.of(any(PropertyValueKind.BOOLEAN), any(PropertyValueKind.NULL)));
        p.add(new PropertyDefinition(new PropertyName("source"), DartParameter.named(p.size(), true),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("DataTableSource")), Optional.of(INITIAL_SOURCE)));
        return List.copyOf(p);
    }
    /** Closed decimal list, not a Dart expression. Empty is legal when the native dropdown is disabled. */
    public static List<BigInteger> pageSizes(String text) {
        if (text.isBlank()) return List.of();
        var values = new ArrayList<BigInteger>();
        for (String part : text.split(",", -1)) {
            String token = part.strip();
            if (!token.matches("[1-9][0-9]*")) throw new IllegalArgumentException("Page sizes must be comma-separated positive integers.");
            var value = new BigInteger(token);
            if (value.compareTo(DartNumericLiterals.MAX_PORTABLE_INTEGER) > 0 || values.contains(value))
                throw new IllegalArgumentException("Page sizes must be distinct portable positive integers.");
            values.add(value);
        }
        return List.copyOf(values);
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        if (!TYPE.equals(node.type())) return Optional.empty();
        try {
            var actions = node.slots().get(new SlotName("actions"));
            if (actions instanceof WidgetSlot.ListSlot a && !a.children().isEmpty()
                    && !(node.slots().get(new SlotName("header")) instanceof WidgetSlot.SingleSlot h && h.child().isPresent()))
                return Optional.of("PaginatedDataTable actions require a header.");
            if (new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("primary")))
                    && node.properties().get(new PropertyName("controller")) instanceof PropertyValue.DartObjectReferenceValue)
                return Optional.of("Primary scrolling cannot use an explicit ScrollController.");
            var sizes = node.properties().get(new PropertyName("availableRowsPerPage")) instanceof PropertyValue.StringValue s
                    ? pageSizes(s.value()) : List.of(BigInteger.TEN, BigInteger.valueOf(20), BigInteger.valueOf(50), BigInteger.valueOf(100));
            var callback = node.properties().get(new PropertyName("onRowsPerPageChanged"));
            var current = node.properties().get(new PropertyName("rowsPerPage")) instanceof PropertyValue.IntegerValue i ? i.value() : BigInteger.TEN;
            if (callback != null && !(callback instanceof PropertyValue.NullValue) && !sizes.contains(current))
                return Optional.of("Available rows per page must contain Rows per page while its change callback is enabled.");
            return Optional.empty();
        } catch (IllegalArgumentException error) { return Optional.of(error.getMessage()); }
    }
    public static String help(String name) {
        return switch (name) {
            case "source" -> DESCRIPTION;
            case "availableRowsPerPage" -> "Comma-separated distinct positive integers, for example 10, 20, 50, 100. Empty is allowed with the change callback disabled. Keep Rows per page in the list when enabling its callback.";
            case "rowsPerPage" -> "Positive controlled page size; omission uses 10. onRowsPerPageChanged must update this value in State.";
            case "initialFirstRowIndex" -> "Non-negative initial row index, default 0. Only applied on initial mount; use PaginatedDataTableState.pageTo for later navigation. PageStorageKey may restore a saved index.";
            case "controller" -> "Typed application-owned ScrollController for horizontal scrolling, not pagination. Cannot be combined with primary=true. Canvas does not execute it.";
            default -> DataTableWidgetPropertySchema.help(DataTableWidgetPropertySchema.TYPE, name);
        };
    }
    public static boolean usesInitialSource(WidgetNode node) {
        if (TYPE.equals(node.type()) && INITIAL_SOURCE.equals(node.properties().get(new PropertyName("source")))) return true;
        for (var slot : node.slots().values()) {
            if (slot instanceof WidgetSlot.SingleSlot s && s.child().filter(PaginatedDataTableWidgetPropertySchema::usesInitialSource).isPresent()) return true;
            if (slot instanceof WidgetSlot.ListSlot l && l.children().stream().anyMatch(PaginatedDataTableWidgetPropertySchema::usesInitialSource)) return true;
        }
        return false;
    }
    private static PropertyValueConstraint any(PropertyValueKind kind) { return new PropertyValueConstraint.AnyValue(kind); }
    private static void add(List<PropertyDefinition> p, String name, List<PropertyValueConstraint> constraints) {
        p.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(p.size(), false), constraints, Optional.empty()));
    }
    private PaginatedDataTableWidgetPropertySchema() {}
}
