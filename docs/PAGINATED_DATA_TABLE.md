# PaginatedDataTable and DataTableSource

Pinned API: Flutter 3.44.8. The palette adds one non-const Material definition,
with all 29 native constructor arguments (35 writable property rows after
expanding heading-state colors, plus three visual slots).

## Using it

Drop PaginatedDataTable into a normal widget slot. It starts with two editable
DataColumn headers. Header is optional; Actions requires Header. An empty Actions
slot omits that Dart argument because Flutter rejects even actions: [] without
a header. DataColumn retains widths, numeric behavior, tooltip, alignment,
state mouse cursors and the onSort event from the DataTable slice.

First insertion also creates an editable _FlutterDesignerDataTableSource class
outside the managed Dart regions. Source points to its static instance, not a
constructor called in build. The initial source is intentionally empty.
Populate it with replaceRows(List<DataRow>); each row must contain one cell per
column. replaceRows copies the list and calls notifyListeners. All four abstract
members are present: getRow, rowCount, isRowCountApproximate, selectedRowCount.

The starter instance is shared by tables using it in the same Dart library and
has application lifetime. For independent tables or shorter-lived data, change
Source to your own stable instance and dispose it in its owner's lifecycle.
PaginatedDataTable installs/removes listeners but does not dispose your source.
Do not dispose the shared starter when only one of several tables unmounts.

Source accepts strict current-class or imported references, static members,
getters and zero-argument factories returning DataTableSource. A getter/factory
must return a long-lived instance; type checking cannot prove object lifetime.
Object, dynamic and nullable sources are rejected before pair-save. Source
scaffolding is part of the analyzed insertion and Undo/Redo transaction. Existing
source edits are retained; conflicting declarations are rejected, not overwritten.

## Columns versus source rows

Properties → Table grid → Columns adds, removes and reorders header descriptors.
Palette insertion and same-table moves use the same fenced command. Known local
sortColumnIndex values follow their column; removing that column clears its index.
A State-bound sort index must be detached before column edits, then updated and
rebound. Source rows are not rewritten. Update their cell count/order yourself
when columns change. DataRow/DataCell are not visual children of this widget:
they belong to the application DataTableSource, unlike a non-paginated DataTable.

## Properties and events

All native fields include key (including typed PageStorageKey), sorting,
deprecated dataRowHeight and current min/max heights, heading height, margins,
spacing, checkbox display/margin, first/last buttons, initial row index,
page size/options, drag behavior, arrow color, horizontal ScrollController,
nullable primary, all heading WidgetState colors, divider thickness and empty
rows. Source-backed colors/controllers are preserved without execution in Canvas.

Available rows per page uses a closed comma-separated list, e.g. 10, 20, 50, 100.
Values must be distinct positive portable integers. Empty is allowed only when
the page-size callback is disabled. With that callback enabled, the list must
contain the current page size. A positive State-bound page size must maintain
this relationship at runtime. Height bounds and controller/primary conflicts
are checked. Non-null booleans use centered checkboxes; unset/null remain distinct.

Events exposes onSelectAll(bool?), onPageChanged(int firstRowIndex), and
onRowsPerPageChanged(int?). DataColumn.onSort(int columnIndex, bool ascending)
is also available. Create, select, navigate and rename use existing typed Events.

State property consumers include rowsPerPage, sortColumnIndex, sortAscending,
showCheckboxColumn, showFirstLastButtons and showEmptyRows. The first row index
is initial-only, not a controlled State field; use PaginatedDataTableState.pageTo
for later navigation. A PageStorageKey can restore the previous index.
Sorting and selection remain application logic: reorder/update the source and
call notifyListeners, and update controlled flags/page size with setState.

## Isolated Canvas

Canvas renders the native PaginatedDataTable/Card/header/columns/footer with
zero source rows and an explicit project-source preview notice. It never executes
project data sources, getters, handlers, controllers, network access or databases.
No fake records are persisted. Loading, approximate counts, application selection
and data ordering must be exercised with Run/Debug.

The empty preview source is stable and listeners are released when the table
unmounts. Native column geometry is used even if a custom header contains another
table. Large page sizes above 1,000 show a disclosed preview limit rather than
allocating an unbounded number of blank rows; generated Dart retains the saved
value. RTL follows the pinned native Flutter implementation.

## Verification

After the full Java regression and focused corrective reruns, the current
Surefire reports contain 6,229 cases: 5,961 passed, 268 explicit SDK/environment
skips, zero failures/errors. The old folder-rename lock suite passed all 38 tests
on its separate rerun without changing the lock implementation or assertions.
The PaginatedDataTable SDK gate separately passed 37 generated/native cases,
covering strict source types, source lifetime/listeners, lazy rows, approximate
counts/loading, page navigation, page-size changes, sorting, State rebuilds,
typed handlers, column commands, and pair-save/reopen.

The complete final Canvas suite passed all 11,601 tests. Flutter analyze reported
no issues, and release Web compilation passed. Package metadata checks passed
all nine integration tests. Byte/hash checks verified the current implementation
classes, all four new SVG variants (plus the previous 24 DataTable variants),
41 packaged runner sources and 35 Web artifact files. All four development-cluster
JARs match the NBM.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
(9,307,965 bytes), SHA-256:
`5ed0f0b80d6388f2d4203a02899ce841247c83d10d5e602ca70137b242dbcbe8`.

FD schema 17, Catalog API 16, Canvas model 20 and transport protocol 1 are
unchanged. The catalog now has 233 definitions, 7,739 writable property rows
(7,721 outside Scaffold), 190 events across 66 types and 178 State consumer
fields across 60 types.

No installed IDE/userdir was modified or restarted. Interactive NetBeans desktop
acceptance is separate and is not claimed here.

Official references:
[PaginatedDataTable constructor](https://api.flutter.dev/flutter/material/PaginatedDataTable/PaginatedDataTable.html),
[DataTableSource](https://api.flutter.dev/flutter/material/DataTableSource-class.html).
