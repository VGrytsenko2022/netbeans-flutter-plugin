# SliverList and SliverGrid: builders and delegates

Pinned SDK: Flutter 3.44.8, widgets/sliver.dart.
Official [SliverList constructors](https://api.flutter.dev/flutter/widgets/SliverList-class.html)
and [SliverGrid constructors](https://api.flutter.dev/flutter/widgets/SliverGrid-class.html).

## Complete additional constructor surface

| Palette entry | Properties | Children |
| --- | --- | --- |
| SliverList.builder | itemBuilder, findChildIndexCallback, itemCount, addAutomaticKeepAlives, addRepaintBoundaries, addSemanticIndexes, semanticIndexOffset | supplied by builder |
| SliverList.separated | itemBuilder, separatorBuilder, findChildIndexCallback, findItemIndexCallback, itemCount, addAutomaticKeepAlives, addRepaintBoundaries, addSemanticIndexes | supplied by builder |
| SliverList.new | delegate | supplied by SliverChildDelegate |
| SliverGrid.builder | gridDelegate, itemBuilder, findChildIndexCallback, itemCount, addAutomaticKeepAlives, addRepaintBoundaries, addSemanticIndexes, semanticIndexOffset | supplied by builder |
| SliverGrid.list | gridDelegate, addAutomaticKeepAlives, addRepaintBoundaries, addSemanticIndexes, semanticIndexOffset | required ordered list, may be empty |
| SliverGrid.new | delegate, gridDelegate | supplied by SliverChildDelegate |

Together with the [static slice](SLIVER_CHILDREN.md), these cover all constructors
and their parameters in the pinned SDK, except the shared framework key (Designer
uses stable model identity). They are six distinct model/Palette entries, not
silent conversions to static lists. There are 31 new property rows, 12 booleans
and eight non-event callable descriptors (four builders and four index delegates).

## Editing and generation

Required builder/object properties accept closed presets or typed project
references: a function/object, member/getter, or zero-argument factory.
User-owned implementations and delegate configuration remain in project Dart;
this slice does not add arbitrary inline Dart or a delegate-object graph editor.
Custom SliverChildDelegate and SliverGridDelegate subclasses are supported by
static type proof, not by enumerating a limited list of implementations.

Presets are explicit application behavior, not Flutter defaults:
- itemBuilder: empty returns null immediately;
- separatorBuilder: shrink returns const SizedBox.shrink();
- delegate: empty uses SliverChildListDelegate(const []);
- gridDelegate: fixedCount uses two columns; maxExtent uses 200 logical pixels.

Optional omission preserves SDK defaults. itemCount and index callbacks also
allow explicit null; booleans retain the common checkbox/unset editor. Required
fields cannot be reset to unset/null. All values survive FD decode/reopen and
stable Properties refresh, without replacing rows or groups.

itemBuilder is proved as NullableIndexedWidgetBuilder. Flutter declares a nullable
separator result but asserts that each separator is a Widget; Designer therefore
requires the stronger IndexedWidgetBuilder contract. ChildIndexGetter? accepts
nullable callbacks and int? results. Original call expressions are separately
checked under strict casts, rejecting dynamic results even if function assignment
alone would accept them. Proof overlays never execute or persist user code.

SliverList.separated cannot combine findChildIndexCallback and
findItemIndexCallback. The deprecated former counts children including
separators; the latter counts items and Flutter translates indices. Set one to
explicit null or reset it before selecting the other. Designer never silently
clears a conflicting value. Counts and semantic offsets are nonnegative; the
separated count is additionally bounded so 2 * itemCount - 1 stays portable.

## Canvas and placement

All six definitions require a sliver slot (currently CustomScrollView.slivers).
They cannot be roots or children of ordinary box widgets. Only SliverGrid.list
exposes editable static children, accepting boxes and rejecting direct slivers.

Canvas uses real native SDK constructors and the explicit presets. It never
executes project builders/delegates. A visible preview-limit notice accompanies
project references: dynamic items are not previewed, and custom grid geometry
uses a labeled two-column approximation. Static SliverGrid.list children remain
in the preview. Generated application Dart retains the exact references and
does not contain this notice or the approximation. Run the application to
verify dynamic content, custom layout, keys and lifecycle behavior.

## Verification

- SliverDynamicContractTest: exact fields, defaults, traits, callbacks and conflicts.
- SliverDynamicDartGenerationTest: all presets, project factories, explicit null,
  exact constructors and FD round-trip.
- SliverDynamicPropertyContractTest: editable fields, required-value guards,
  checkbox painting, stable row/group identity and reopened editing.
- SliverDynamicRealSdkTest: actual candidate save gate, strict callback/delegate
  typing, getters/factories, adversarial dynamic/wrong signatures, and six
  generated application widget tests.
- DartCandidateAnalyzerTest: isolated, alias-safe call-result proof structure.
- Canvas tests: 48 axis/reverse/reference combinations plus closed model validation.
- Full catalog/projection, Palette/DnD, icon and packaged-source hash contracts.

Current catalog: 128 definitions, 6,440 property rows, 518 boolean rows,
124 typed Properties definitions plus four structural definitions, and
105 const-capable definitions. Callables: 186 across 43 types; native Events
remain 150 across 36 types. FD 16 / Catalog API 15 / Canvas 19 / transport 1 unchanged.

