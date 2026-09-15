# Flow and Flow.unwrapped

Pinned contract: Flutter 3.44.8. Both public constructors are admitted in Layout.
Flow adds a RepaintBoundary around each child; Flow.unwrapped preserves the
supplied children without adding those boundaries. The former constructor is
not const; the latter is const-capable. No constructor branch is omitted.

## Properties and children

- Delegate: required, non-null FlowDelegate. The initial value is a const,
  editable starter delegate.
- Clip behavior: none, hardEdge, antiAlias, antiAliasWithSaveLayer. Omission
  preserves Flutter's hardEdge default. Null is rejected.
- Children: optional ordered ordinary-widget list; empty is valid. Insert,
  delete, move, reorder and the shared slot editor apply.
- Key: the existing shared widget identity editor.

Flow is an ordinary box parent. Slivers, LayoutId, Positioned and flex parent
data such as Expanded/Flexible/Spacer are not valid direct children; established
placement rules reject them before saving. Do not confuse Flow with Wrap:
Flow positions children in paint, not in layout.

## Editable source and proof

First insertion atomically adds `_FlutterDesignerFlowDelegate` outside managed
Dart regions. The same class serves both constructors; inserting another Flow
does not duplicate it. It remains after removing the last Flow so user edits
are never deleted. Undo/redo preserves exact source snapshots; save/reopen keeps
the class and FD values. Conflicting declarations fail without mutation.

The starter exposes every behavioral method:

- getSize constrains a preferred 256 by 192 logical-pixel area.
- getConstraintsForChild supplies loose dimensions no greater than 48 by 48,
  bounded by the actual parent size.
- paintChildren paints each child once, with Matrix4 translations, in
  left-to-right rows with an 8 pixel gap. This physical ordering deliberately
  does not depend on ambient RTL. Custom source can choose different ordering.
- shouldRelayout and shouldRepaint return false for this immutable starter.
- The constructor forwards repaint, so edited source can attach a Listenable.

Replace the Delegate property with a typed current-file or package reference,
getter, static member, or zero-argument factory. Const unnamed/named constructors
are supported too. The analyzer proves the exact SDK FlowDelegate type using an
unshadowable rendering-library witness; nullable, dynamic, Object and unrelated
results are rejected. Arbitrary expression strings are not accepted.

Source code owns custom geometry, paint order, per-child opacity and repaint
notifications. Parent sizing cannot depend on child sizes. A paint pass may skip
children or paint them in any order, but must not paint a child twice. Update
shouldRelayout when size/constraints change and shouldRepaint when paint inputs
change. A repaint Listenable can move children without rebuilding or relaying
out the tree. Delegate methods are not synthetic Events.

## Canvas boundary

A regression discovered during this slice was corrected in the shared pointer
candidate collector: flex wrappers over non-Flex parents no longer receive a
misleading append target. Candidate acceptance now uses the central parent-data
rules already used by model validation and the host.

The isolated runner never executes project delegates. A visible tooltip
discloses the fixed starter substitution. Both constructor variants use native
Flow/RenderFlow, honor all clipping options and preserve native child identity
through reorder. Canvas selection uses painted transforms; empty and populated
containers expose an append drop target.

Custom transforms, custom opacity, animation and source-dependent layout must
be checked in the generated application. Zero-opacity ancestors can prevent
Flow from painting; native hit testing then has stale paint geometry. When
hiding such a Flow, also use IgnorePointer as described in Flutter's API.
The Designer does not silently rewrite user opacity/hit-testing choices.

## Inventory

223 definitions, 7,448 writable rows (7,430 outside Scaffold), 215 typed and
eight structural definitions, 186 const-capable definitions. Layout has 71
entries. Optional insertion matrix: 192 destinations (171 any-widget and 21
trait-bound), 42,816 candidates, 29,114 accepted and 13,702 rejected.
Boolean editors remain 681 boolean plus 53 nullable boolean rows.
Native Events remain 174 rows across 60 types; supported callables remain
240 across 88 types. No format bump: FD 17, Catalog API 16, Canvas model 20,
transport 1.

## Verification

- Java: 4,080 fresh tests in the full flutter-designer suite plus selected
  analyzer/plugin integration and packaging suites: 3,978 passed, 102 expected
  conditional skips, no failures or errors. This is not a full IDE UI run.
- Real Flutter 3.44.8 SDK: 72 generated/native cases (34 saved forms in LTR/RTL
  plus four behavioral tests). Covers both constructors, all source forms,
  const constructors, rejected nullable/dynamic/wrong types, source edits,
  save/reopen/history, exact clip behavior, paint transforms and opacity,
  hit testing, repaint without layout, delegate replacement and listener detach.
- Canvas: all 11,562 tests passed, including 29 Flow-specific tests.
  New tests cover child identity/reorder/removal, transformed selection,
  ordinary-child drop targets, finite constraints and both native constructors.
- flutter analyze: no issues. Web release build passed.
- Eight SVG icon variants were rendered for review and passed the registry
  safety, geometry, dimensions and light/dark contracts.
- Package verification checks all 40 runner sources and 35 Web artifacts,
  changed classes/icons and all four development-cluster JARs against the NBM.
- No running user IDE was restarted and no interactive desktop verification is
  claimed. Relaunch the project test IDE to load the new cluster.

Web main.dart.js: 4525968 bytes, SHA-256
`f0cc6e2f9ccde40c63f4900d7954a189aaf873d1af713a567161ef6407074d9a`.

NBM: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
9196736 bytes, SHA-256
`505ccb5f71fb2e2d1bbc51148cf4c675dedea99d9ab9eb932649c8a32b188cb1`.

## References

- [Flow API](https://api.flutter.dev/flutter/widgets/Flow-class.html)
- [FlowDelegate API](https://api.flutter.dev/flutter/rendering/FlowDelegate-class.html)
- [FlowPaintingContext API](https://api.flutter.dev/flutter/rendering/FlowPaintingContext-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/basic.dart and
  packages/flutter/lib/src/rendering/flow.dart.
