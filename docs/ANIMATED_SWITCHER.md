# AnimatedSwitcher

Pinned API: Flutter 3.44.8 `widgets/animated_switcher.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/AnimatedSwitcher/AnimatedSwitcher.html),
[class and identity semantics](https://api.flutter.dev/flutter/widgets/AnimatedSwitcher-class.html).
The SDK files were inspected without changes; the local SDK checkout is clean.

## Model and Properties

Layout order 470, after AnimatedCrossFade. Six constructor properties:
durationUs, reverseDurationUs, switchInCurve, switchOutCurve, transitionBuilder,
layoutBuilder. Optional single Child supports insertion, removal, replacement,
movement and wrapping. Creation uses duration 300000 microseconds and no child;
300 ms is a Designer preset, not a Flutter default for required duration.

Duration accepts a nonnegative portable integer in microseconds or a strict
Duration reference/factory. Reverse duration additionally permits null and
Duration? references. Both curves support all 43 pinned Curves presets and
strict Curve references/factories; omission uses linear. Required duration
cannot be reset; optional values can be restored to omission. Null is not
accepted for either builder or curve. There is no onEnd, alignment, or
clipBehavior constructor argument. No extra property is invented.

Stable ID remains Designer identity, not an application Flutter Key. This slice
does not add a global Dart Key editor or force generated children into keyed
wrappers. Consequently, same-type unkeyed children update without a transition;
changing only Stable ID or a Text value does not force an application animation.

## Builders and source ownership

- transitionBuilder: Widget Function(Widget child, Animation<double> animation).
- layoutBuilder: Widget Function(Widget? currentChild, List<Widget> previousChildren).

Both are optional non-null construction callbacks, not interaction events.
Shared callable editors support binding and generating user-owned methods,
renaming/unbinding, and typed zero-argument factories. The initial method bodies
delegate to the native default builders, preserve all arguments, and remain
editable outside managed regions. Exact Flutter/core type witnesses consume the
original callback result. Wrong argument lists, void/dynamic returns, and wrong
scalar bindings are rejected before pair-save. Duration symbol occurrences use
trusted dart:core evidence.

FD encode/decode, source generation, pair-save evidence, source preservation,
reopen and Undo use the existing command lifecycle; no new format version.

## Native animation and Canvas

The first child is fully visible. A changed native child type starts a new
entry; outgoing entries reverse. Duration, reverseDuration and curves are
captured when each entry is created, not retroactively applied to old entries.
An interrupted entrance reverses using its existing curve. The centered Stack
uses the largest retained child's extent, with no AnimatedSize interpolation.
An empty Stack takes bounded maximum constraints; it is not always zero sized.

Canvas instruments each retained episode with private GlobalKeys, including
nested widgets, menu controllers and Tooltip overlays. Geometry aliases point
only to the active model branch and are separate from key allocation. This
prevents duplicate keys on A-to-B-to-A, Undo, and moving a child out while its
old episode fades. Outgoing branches are inert for Designer pointer/focus/
semantics interaction. Current children remain available for selection/drop
and inline text editing. Application Flutter keys are not inferred from FD IDs.

Project code is never executed in Canvas. Presence-only references are labeled:
unresolved duration uses 300 ms, reverse duration null, curves linear, and
builders use the default fade/centered Stack preview. Stored values and
generated Dart are not rewritten.

### Pinned Flutter 3.44.8 default-transition key limitation

The pinned implementation uses `KeyedSubtree.wrap(builder(...), entryNumber)`.
That constructor prefers the returned transition's key over entryNumber.
The default builder returns a FadeTransition keyed by the child's key.
As a result, repeated keys can make the SDK filter an outgoing entry out of
the layout; for unkeyed generated children, both default transition keys are
ValueKey(null). A focused native-SDK regression reproduces this. This differs
from the documented independent-entry A-to-B-to-A behavior.

Canvas uses an unkeyed outer KeyedSubtree around the native fade to keep its
preview entries independent. Its tooltip explicitly states this difference.
Generated Dart and the installed SDK retain their original behavior. The
editable transition builder can opt into the same workaround in application
code without an SDK patch:

```dart
Widget _transition(Widget child, Animation<double> animation) {
  return KeyedSubtree(
    child: AnimatedSwitcher.defaultTransitionBuilder(child, animation),
  );
}
```

Bind this method to Transition builder when independent outgoing entries are
needed with this pinned SDK. It does not force transitions for a same-type,
same-key child; that is normal Flutter identity behavior.

## Inventory

190 admitted definitions; 182 typed-property definitions; 7119 writable rows
(7101 outside Scaffold); 157 const-capable definitions; 55 Layout entries;
646 BOOLEAN-only rows. Events remain 171 rows across 57 types. All supported
callables: 233 across 82 types, including 48 builders.

Ordinary insertion: 33250 candidates, 22036 accepted, 11214 rejected;
175 destinations, of which 156 accept arbitrary widget types.
Required-child wrappers remain 34, including 24 root-compatible wrappers.
Formats: FD 16 / Catalog API 15 / Canvas model 19 / transport 1.

## Verification

- Dedicated Canvas suite: 58 passing tests, including the pinned SDK limitation
  witness, repeated IDs, same-type replacement, move/Undo, nested switchers,
  MenuBar/Tooltip isolation, all 43 curves, LTR/RTL, empty child and null/reset.
- Full Canvas runner: 8038 passing tests; Flutter analyzer: no issues.
- Full core plus selected NetBeans/analyzer/artifact regressions: 3800 cases
  in 517 fresh Surefire reports, 73 conditional skips, zero failures/errors
  (3727 executed). This is not every unfiltered repository Java test.
- Real SDK: six generated forms mounted in LTR/RTL (12 runtime cases), both
  native-default generated scaffolds, direct typed references, zero-argument
  factories, empty child and null/reset, exact analysis evidence, source
  preservation, reopen and Undo. Wrong scalar/callback types, argument lists,
  void and dynamic callback results were rejected. The SDK test executed
  successfully with flutter.events.sdk set, not merely skipped.
- Web release build succeeded; all four SVG variants were rendered and reviewed.
  Packaged source and local Web manifests were refreshed from actual bytes.
- Working checkout: G:. No SDK or user Flutter-project edits, no IDE restart.
  Interactive NetBeans desktop smoke testing was not performed.

Final Maven install and nbm:cluster succeeded. Verification opened the NBM and
checked the current schema/editor/evidence/source-lifecycle classes, all four
SVG variants, all 40 packaged runner sources, all 35 local Web release files,
and equality of all four development-cluster JARs with their NBM counterparts.

- NBM: 8947134 bytes; SHA-256
  `67bc0cdb4ef7a970e3ad5dd8fc0776bcb41b9f6a51b34bdead966d35e3b0e1c4`.
  Built at 2026-09-14T07:29:22.8518577Z.
- main.dart.js: 3828768 bytes; SHA-256
  `922e1d9890a3ff344984a4d3b98b006e980f2cf926bca5d62b83e58b3271e0c1`.
  The Web build is a verified local artifact; it is not claimed as bundled JS.
- Installable file:
  `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.
