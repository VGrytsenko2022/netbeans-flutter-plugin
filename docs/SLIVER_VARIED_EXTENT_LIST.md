# SliverVariedExtentList

Reviewed against Flutter 3.44.8 installed SDK and official constructor documentation.

## Constructor coverage

| Palette entry | Native constructor | Property rows | Slots |
| --- | --- | ---: | --- |
| SliverVariedExtentList.list | SliverVariedExtentList.list | 4 | required ordered children, empty allowed |
| SliverVariedExtentList.builder | SliverVariedExtentList.builder | 7 | none |
| SliverVariedExtentList.new | SliverVariedExtentList | 2 | none |

All thirteen non-key arguments are editable. The shared stable-identity key remains
generator-owned. Only the unnamed constructor is const-capable; generation also
requires const evidence for its arguments. Preset closures are not const.

Every constructor requires ItemExtentBuilder: double? Function(int index,
SliverLayoutDimensions dimensions). It supplies a finite non-negative main-axis
extent for each actual item; zero is valid. A null result means an out-of-range
index, not natural sizing or silently dropping an existing child. The callback
itself cannot be unset/null. Typed function tear-offs, getters, static members
and zero-argument factories use the shared analyzer proof gate; there is no raw
Dart text escape hatch. Nullable, async, dynamic or wrong-signature callbacks are
rejected even when ordinary analyzer diagnostics are suppressed.

Import `package:flutter/rendering.dart` when declaring the ItemExtentBuilder typedef
in project code; Flutter 3.44.8 does not re-export that typedef from widgets.dart.
For example, a user-owned callback for three items can be:

```dart
import 'package:flutter/rendering.dart';

double? itemExtent(int index, SliverLayoutDimensions dimensions) {
  const extents = <double>[48, 72, 96];
  return index >= 0 && index < extents.length ? extents[index] : null;
}
```

The explicit string preset "48" generates a 48 logical-pixel extent callback,
bounded by the visual children count or an explicit builder itemCount where known.
This is a Designer starting value, not an SDK default. With project-owned dynamic
children and an unknown count, bind an extent function that follows the project's
actual item range. Required callbacks cannot be reset into an invalid missing state.

The builder has required NullableIndexedWidgetBuilder, optional ChildIndexGetter?,
optional int? itemCount and the three optional boolean delegate flags.
Omission/null count is unbounded, while the item builder can stop by returning null.
The list exposes all three flags. Omission preserves true; explicit false uses
the centered checkbox editor. Neither constructor has semanticIndexOffset.
The unnamed constructor accepts the full typed SliverChildDelegate, including
project list/builder delegates and custom subclasses; their configuration stays
in project-owned Dart.

## Canvas, properties and placement

The preset renders native SliverVariedExtentList for all three constructors.
Visual list children are preserved. Native axes, reverse, RTL, scroll geometry,
empty-slot hit/drop handles and post-removal insertion indices are reused and tested.
Only sliver-accepting destinations admit the widget; visual children accept box
widgets, not slivers or direct Flex parent-data children.

Isolated Canvas does not execute project code. For a project extent callback,
SliverList provides explicitly labeled natural-size approximation, never a
fabricated callback result or hidden 48 px assumption. Project item builders and
delegates remain empty in preview with a visible limitation message; generated
Dart retains all exact references. There is no persisted placeholder child.
The five callable rows are construction/index callbacks in Properties, not
interaction Events.

Property rows and sets retain identity across updates. All fields remain editable
after save/reopen; required callbacks retain their preset/reference. FD roundtrip,
typed generation, candidate evidence, undo and redo preserve user members.

## Current inventory

141 admitted definitions; 137 typed and four structural; 6,492 writable fields
(6,474 outside Scaffold); 545 boolean rows; 111 const-capable definitions.
128 insertion destinations (124 AnyWidget + four sliver-trait slots), 18,048
source/destination candidates: 14,316 accepted and 3,732 rejected.
Native Events remain 150 across 36 types. Total callables: 196 across 49 types
(150 Events, 32 Builders, nine Delegates, three Predicates, two Formatters).
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Sources

- [Native unnamed constructor](https://api.flutter.dev/flutter/widgets/SliverVariedExtentList/SliverVariedExtentList.html)
- [Builder constructor](https://api.flutter.dev/flutter/widgets/SliverVariedExtentList/SliverVariedExtentList.builder.html)
- [List constructor](https://api.flutter.dev/flutter/widgets/SliverVariedExtentList/SliverVariedExtentList.list.html)
- [ItemExtentBuilder contract](https://api.flutter.dev/flutter/rendering/ItemExtentBuilder.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart,
  packages/flutter/lib/src/rendering/sliver.dart and sliver_fixed_extent_list.dart.

## Verification

The full Flutter runner suite passed all 3,146 tests, including 81 dedicated
SliverVariedExtentList tests. Dart analysis of lib and test reports no issues.
The full core reactor passed: 2,264 tests, 20 conditional skips, no failures/errors,
with successful Maven exit. Test totals come from fresh Surefire XML reports.
The broad targeted NetBeans/core/runner contract set also passed: 1120
tests, 8 conditional skips, no failures/errors and successful Maven exit.
It includes Properties editors, palette/drop/wrap planning, accessibility,
icon registry, Java/Dart capability parity and strict runner/Web artifact checks.
The unrelated full NetBeans reactor was not repeated because of its previously
recorded Windows AWT shutdown issue.
NBM install and development cluster creation succeeded. Verification compared
the current schema class, all twelve light/dark 16/32 SVG variants, forty runner
sources and thirty-five Web artifact entries. All three module/Designer/runner
JARs in the dev cluster exactly match the NBM. Integrity checks were not relaxed.

Web main.dart.js: 3,676,990 bytes; SHA-256
`a3839d52863028ef434665751f1335aa01cdddba0d36520de572bbe941bd4f5a`.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,695,061 bytes; SHA-256
`38da8397bbb2ae944ba41a131481ad367d7f351429d520e6b98ca6d2ee863f25`.

All 27 generated Dart runtime cases passed on Flutter 3.44.8, including zero and
fractional extents, variable per-index sizes, actual SliverLayoutDimensions,
vertical/horizontal axes and reverse. The fixture uses the same package import
identity as generated project references when observing callback state.
The separately enabled combined analyzer/save/reopen/history/runtime JUnit test
passed: one test, zero skips/failures/errors, 151.195 seconds, successful Maven
exit. It validates all three constructors, typed functions/getters/factories,
nullable key-index lookup, project delegates, rejected unsafe callbacks,
user-member preservation, required-preset restoration, exact FD identities and
Undo/Redo, then executes all 27 generated Flutter runtime cases.
Manual NetBeans desktop smoke testing and IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
