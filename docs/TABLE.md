# Table, TableRow and TableCell

Pinned API: Flutter 3.44.8. All three constructor surfaces are admitted together
in Layout. TableRow is a row descriptor, not a Flutter Widget; Designer exposes
it in the tree without ever trying to render it as a standalone widget.

## Editing the grid

A new Table contains two explicit rows with two 48 × 48 SizedBox cells each.
Drop content into a cell's Child slot, replace that child in Slots, or edit the
cell's dimensions. The seed is saved in FD and generated Dart; it is not an
invisible Canvas-only placeholder. TableRow insertion adapts a fresh, unchanged
row prototype to the destination table's current width.

Table → Properties → Table grid → Rows and columns performs one atomic operation:

- Add, remove or reorder a complete row.
- Add, remove or reorder a complete column across every row.
- Preserve unaffected row/cell stable IDs and their properties/subtrees.
- Remap local indexed column-width overrides with the column operation.
- Reject a stale editor snapshot before changing anything.
- Use normal command analysis, pair-save, Undo and Redo.

Indices are zero-based; move destinations refer to the post-removal list.
Removing a column deletes its complete contents in all rows. Removing the last
column is rejected: remove rows or clear Table.children instead. An empty Table
is legal; a Table containing an empty row or unequal row lengths is not.
Grid dialogs limit additions to 1,000 rows/columns; shared document/node limits
still apply. Existing generic slot commands cannot bypass rectangular validation.

A project-owned columnWidths map cannot be safely remapped by Designer.
Switch to local widths before editing columns, or maintain that map in Source.
This prevents a visual operation from silently changing an unrelated source map.

## Complete constructor properties

Table exposes key, columnWidths, defaultColumnWidth, textDirection, border,
defaultVerticalAlignment, textBaseline and children. It is not const.

TableRow exposes key, decoration and children. Local decoration uses the shared
complete BoxDecoration editor, including gradients, borders, shadows and declared
asset images. Other Decoration implementations use strictly typed source references.

TableCell exposes key, nullable verticalAlignment and its required child.
All six alignments are supported: top, middle, bottom, baseline, fill and
intrinsicHeight. Null/unset cell alignment inherits the table setting.
Baseline requires Table.textBaseline (alphabetic or ideographic); a row whose
cells all use fill has native zero height. Designer supports TableCell directly
inside TableRow.children. Flutter can also allow intervening non-render widgets;
Designer does not admit that indirect parent-data branch because its selection
instrumentation introduces render objects.

Keys are separate from stable Designer IDs. Local strings generate ValueKey;
typed sources accept Key? for Table/TableCell and LocalKey? for TableRow.
Duplicate known row keys and duplicate known direct-cell keys across all rows
are rejected. Equality and lifetime of project-owned keys remain source-owned.

## Column widths

The structured editor supports all six native TableColumnWidth families:

- FixedColumnWidth: finite non-negative logical pixels.
- FlexColumnWidth: finite positive flex factor.
- FractionColumnWidth: finite non-negative fraction of available width.
- IntrinsicColumnWidth: optional finite positive flex factor.
- MinColumnWidth and MaxColumnWidth: recursively combine two widths.

Indexed overrides are sorted, unique and bounded to 0..9999. Unspecified columns
use defaultColumnWidth, whose omitted value is native FlexColumnWidth(1).
The internal closed value grammar has depth 8, 256 nodes and 16,384-character
limits. Numeric drafts are bounded to 64 significant digits and scale ±999;
canonical scientific notation avoids unbounded text expansion. It is not Dart
code and is never evaluated. Local values serialize through
the existing StringValue wire type with matching Java/Canvas semantic validation.

Strict current-file/package references, members and zero-argument factories can
supply arbitrary TableColumnWidth subclasses and Map<int, TableColumnWidth>?.
The analyzer verifies assignability to SDK types before pair-save.
Intrinsic layout and baseline calculations can be expensive. Flex/fraction
widths require suitable bounded horizontal constraints; Designer does not silently
clamp the parent or replace the chosen native sizing algorithm.

## Borders

Local all, symmetric and custom modes cover every TableBorder constructor.
Custom mode exposes top, right, bottom, left, horizontalInside and verticalInside;
symmetric exposes inside/outside. Each side has color (literal or Material theme),
width, style and strokeAlign. TableBorder.all has no strokeAlign parameter.
Physical elliptical border radii are supported; directional radii are rejected.

Untouched custom/symmetric sides remain BorderSide.none; all uses black, solid,
one-pixel sides. Local modes lower to the unnamed TableBorder constructor with
equivalent native values, including const eligibility. Switching modes clears
only incompatible local fields in one property patch. Null/unset means no border.
Project-owned TableBorder? is also supported. Border painting does not imply clipping.

## Canvas and source boundary

Canvas uses native Table/RenderTable and TableCell, honors local widths, row
decorations, border values, LTR/RTL and vertical alignment. Stable preview keys
preserve child identity on reorder. TableRow selection uses external row handles
derived from the actual RenderTable row geometry, not a fabricated row widget.

The isolated runner never executes project-owned factories, width implementations,
keys or decorations. A visible diagnostic names source-backed fields; Canvas uses
default flex widths, no source border/decoration and private identity keys.
Generated application code retains the original typed sources.

No Events are invented: these constructors have no event callback parameters.
Decoration image callbacks retain the shared typed decoration behavior.
FD 17, Catalog API 16, Canvas model 20 and transport 1 remain unchanged.

## Inventory and verification

226 definitions, 7,495 writable constructor/property rows (7,477 outside Scaffold),
218 typed and eight propertyless structural definitions, 188 const-capable
definitions. Layout has 74 entries. The additional Rows and columns operation is
a structural editor, not a constructor argument.

Optional insertion matrix: 194 destinations (172 any-widget and 22 trait-bound),
43,844 candidates, 29,454 accepted and 14,390 rejected. Boolean editors remain
681 boolean plus 53 nullable boolean rows. Events remain 174 across 60 types;
supported callables remain 240 across 88 types.

- Java: 4,092 fresh tests across the full flutter-designer suite and selected
  analyzer, plugin and packaging suites: 3,989 passed, 103 conditional skips,
  zero failures/errors. This is not a full interactive IDE test.
- Real Flutter 3.44.8 SDK: 114 generated/native cases (57 saved forms in LTR/RTL).
  Includes all six width families, border modes, all six alignments, typed
  current-file/imported/member/factory sources, strict rejection of wrong,
  dynamic and incompatible nullable types, save/reopen and Undo/Redo.
- Canvas: all 11,580 tests passed, including 18 table-specific tests. Native
  tests cover row geometry, row moves, append drops, direct-cell wrapping,
  identity on reorder, LTR/RTL and every TableCell alignment.
- Structured-editor tests cover cancel/OK, committed spinner text, stale
  snapshots, rectangular guards, width remapping and large-grid rejection
  without throwing or losing the ability to open the grid editor.
- flutter analyze: no issues. Offline Web release build passed.
- Twelve SVG variants were rendered for visual review and passed icon contracts.
- Package checks verify 41 source files, 35 Web artifacts, new classes/icons
  and all four development-cluster JARs against the NBM.
- No running IDE was restarted; no live desktop verification is claimed.
  Relaunch the project test IDE to load the refreshed cluster.

Web main.dart.js: 4561744 bytes, SHA-256
`38641efa30ccd61c2ec9f6d726f30f8787c340a50de19cb5e292b3742d5f69ab`.

NBM: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
9250454 bytes, SHA-256
`515c4002f1cc631c7a01f23669c2c07928ad15c9ce11737901f0cb8dfc7a2d38`.

## Official references

- [Table](https://api.flutter.dev/flutter/widgets/Table-class.html)
- [TableRow](https://api.flutter.dev/flutter/widgets/TableRow-class.html)
- [TableCell](https://api.flutter.dev/flutter/widgets/TableCell-class.html)
- [TableColumnWidth](https://api.flutter.dev/flutter/rendering/TableColumnWidth-class.html)
- [TableBorder](https://api.flutter.dev/flutter/rendering/TableBorder-class.html)
