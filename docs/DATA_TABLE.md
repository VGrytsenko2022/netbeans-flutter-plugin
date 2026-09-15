# DataTable, DataColumn, DataRow and DataCell

Pinned API: Flutter 3.44.8. Material exposes six reviewed definitions together:
DataTable, DataColumn, DataRow, DataRow.byIndex, DataCell and DataCell.empty.
The latter five are descriptors, not standalone Widgets. They only enter their
native columns, rows and cells slots; they cannot be roots or ordinary children.

## Structure and editing

A new DataTable saves two columns and two rows of two Text cells. The seed is
real FD/Dart content, not a hidden preview placeholder. At least one column is
required; zero rows is legal. Each row must have exactly the column count.

DataTable → Properties → Table grid → Rows and columns supports atomic
add/remove/reorder operations. Columns include their header and every row's
corresponding cell. Surviving IDs, values, callbacks and subtrees are preserved.
Removing a column deletes its contents across all rows; the final column cannot
be removed. A stale dialog is rejected. Commands use normal analysis, pair-save,
Undo/Redo and reopen validation.

Adding a DataColumn inserts a whole column. Adding DataCell or DataCell.empty to
a row also inserts a whole column at that index: the supplied cell goes into
the selected row, with explicit starter header/cells elsewhere. Existing cells
are not overwritten. Fresh DataRow prototypes adapt to the destination width;
fresh byIndex rows receive distinct unused integer indices.

Moving a DataColumn within its table moves the whole column. Moving cells within
one row reorders only those cells. Cross-row cell moves and incomplete column
removals are rejected if they would make the grid ragged; use the grid editor
for column removal. Local sortColumnIndex follows column edits and is unset if
its column is removed. A State-bound index cannot be remapped safely: column
edits are rejected until that binding is detached. Update the State index before
rebinding; it must stay null or within the current column range. Row edits remain
available.

Indices are zero-based and move destinations address the post-removal list.
Grid/prototype additions are bounded to 1,000 rows/columns, within shared document
limits; existing larger valid lists can be reduced/reordered without truncation.
Slots retain the shared 10,000-child validation ceiling.

## Constructor coverage

DataTable includes every constructor field: key; columns and rows; sort index
and direction; onSelectAll; decoration; data/heading state colors and TextStyles;
deprecated dataRowHeight and current minimum/maximum heights; heading height;
horizontal and checkbox margins; column spacing; divider thickness; checkbox
column and bottom-border flags; border and clipBehavior. Mutually exclusive
height forms, min/max order and local border/style relations are checked.

DataColumn includes label, all six column-width strategies, tooltip, numeric,
onSort, state mouse cursor and nullable headingRowAlignment. The native header
Row supports a direct Expanded/Flexible/Spacer label; ordinary cells do not.

Both DataRow constructors include selected, onSelectChanged, onLongPress,
onHover, color, mouseCursor and cells. DataRow accepts LocalKey?; byIndex accepts
int? index instead and is not const. Known duplicate row keys are rejected,
including two byIndex rows with null indices. Equality of source-backed keys
is application-owned.

DataCell includes its required positional child, placeholder, showEditIcon and
all five gesture callbacks. DataCell.empty is the actual static const member,
not a fabricated constructor. The pencil is a visual editing affordance;
implement the editor in onTap.

Unset fields preserve SDK defaults. Null is retained only where explicitly
admitted. Non-null booleans use the shared centered checkbox editor.

## Styles, state maps and typed source

The two TextStyle families reuse all 31 shared flattened leaves, including
font features/variations, paint and typography. Borders reuse all 37 TableBorder
fields. Decorations reuse the complete shared local BoxDecoration editor.

Color and cursor maps have a Default entry and all eight WidgetState entries.
The first matching state wins, including an explicit null; an absent fallback
resolves to null. Editing a map/style leaf selects local mode atomically.
Switching its base to a source or null clears inactive local leaves.

Widths support fixed, flex, fraction, intrinsic, min and max, including nested
min/max expressions through the existing structured width editor. Fixed widths
must leave room for native header text, sort controls and margins; an explicitly
too-small width can overflow just as in Flutter. Designer does not silently
widen a user-specified column.

Other implementations use strict references to TableColumnWidth?, TextStyle?,
WidgetStateProperty<Color?>?, WidgetStateProperty<MouseCursor?>?, TableBorder?,
Decoration?, Key? and LocalKey?. Current-class, imported and static getters or
zero-argument factories are analyzer-checked. Object/dynamic mismatches fail
before pair-save; arbitrary unverified expressions are not admitted.

## Events and controlled state

Thirteen native event rows are added: one on DataTable, one on DataColumn,
three on each DataRow constructor and five on DataCell. Events supports typed
handler creation, selection, rename and navigation, including onSort(int, bool)
and nullable boolean selection callbacks.

DataTable.sortAscending and sortColumnIndex, and selected on both DataRow
constructors, are reviewed State property consumers. Callbacks remain
user-owned: update State with setState. Neither Flutter nor Designer invents
automatic sorting or reorders the saved rows after onSort. Row data and column
lists in this slice are structurally edited FD lists, not dynamic data sources.
PaginatedDataTable/DataTableSource are separate, not silently emulated here.

## Isolated Canvas boundary

Local values use native DataTable, DataColumn, DataRow and DataCell with native
layout, RTL, checkbox column, sort semantics, state styling and row/cell geometry.
Designer instrumentation does not replace native accessibility semantics.

The isolated preview never executes project factories, handlers or methods.
Source-backed widths/styles/cursors/borders/decorations use disclosed native
fallbacks; preview keys are private stable IDs. Callback presence is shown with
inert handlers, and clicks do not mutate saved selection/sort flags. State
consumers show their saved preview values, not a live application State object.
Use Run/Debug to test application logic and source-owned visuals.

## Verification (2026-09-15)

The full Java run plus corrected-suite reruns produced 6,221 current tests:
5,954 passed, 267 conditional tests skipped, zero failures/errors. The skips
include separately opt-in SDK/environment tests, not a blanket test bypass.
The focused DataTable SDK gate separately passed 202 generated/native cases,
including handler wiring, typed references, wrong Object/dynamic rejection,
all width strategies, controlled State rebuilds, LTR/RTL and saved-pair reopen.

The complete Canvas suite passed 11,593 tests; a final schema/DataTable rerun
passed 195. Flutter analyze reported no issues; release Web compilation passed.
Package metadata checks passed all nine integration tests. Byte/hash checks
verified 41 packaged runner sources, 35 Web files, all 24 new SVGs, current
implementation classes and all four development-cluster JARs against the NBM.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
(9295281 bytes), SHA-256:
`d5d4593a4982a18c2d4a92e1c155704c5fe2303ec75900f2a4ef8fd6a1b962e8`.

No installed IDE/userdir was modified or restarted. Interactive NetBeans
desktop acceptance is not claimed; automated Swing, source, Canvas and package
contracts are the evidence above.

Official references:
[DataTable](https://api.flutter.dev/flutter/material/DataTable/DataTable.html),
[DataColumn](https://api.flutter.dev/flutter/material/DataColumn/DataColumn.html),
[DataRow](https://api.flutter.dev/flutter/material/DataRow/DataRow.html),
[DataCell](https://api.flutter.dev/flutter/material/DataCell/DataCell.html).
