# RotationTransition

Pinned SDK: Flutter 3.44.8, checked against
packages/flutter/lib/src/widgets/transitions.dart and animation/animations.dart.
Official [constructor](https://api.flutter.dev/flutter/widgets/RotationTransition/RotationTransition.html)
and [API](https://api.flutter.dev/flutter/widgets/RotationTransition-class.html).

## Complete constructor projection

All three scalar arguments and the optional box Child are supported.
Key follows the shared Designer identity policy.

- Turns animation is required. A finite signed local number creates
  `const AlwaysStoppedAnimation<double>(value)`; edits are immediate. Creation
  starts at 0, a Designer prototype value rather than a Flutter default.
  The source alternative is strict non-null Animation<double>, with all eight
  current-library/imported, root/member, getter/zero-argument factory forms.
  Integers use the shared portable range +/-9007199254740991.
- Alignment supports physical local values and strict Alignment source
  references/getters/factories. Omission uses center. Finite coordinates outside
  [-1,1] are allowed; null, AlignmentDirectional and broader AlignmentGeometry
  are not substitutes.
- Filter quality supports omission, explicit null, none, low, medium and high.
- Child is an optional single box slot with normal placement/parent-data
  constraints. Add, replace, remove, move and wrapping are transactional.

Turns are revolutions: 1 = 360 degrees clockwise, .25 = 90 degrees clockwise,
negative values rotate counterclockwise, also in RTL. Zero is identity, not an
invisible child. Values are not normalized and no shortest-path rewrite occurs.
Rotation affects paint, semantics and pointer coordinates, not layout. Physical
Alignment determines the pivot; ancestor hit bounds still apply. Direct
RenderTransform.hitTest intentionally bypasses its own untransformed bounds;
the real pointer dispatch through an ordinary ancestor still respects that
ancestor's bounds. Both paths are tested. No clipping is implicitly inserted.

The inherited animation, listenable and onTransform fields are not additional
constructor inputs. RotationTransition supplies its own Z-rotation matrix.
There is no Duration, Curve, On end, controller or transformHitTests parameter.

## Source ownership and Canvas boundaries

Controller/Tween/CurvedAnimation construction and listener lifetime remain in
user Dart. Plain double, nullable outer/value animation types, Animation<int>,
Animation<dynamic>, dynamic and ValueNotifier<double> are rejected by strict
source evidence. User members survive generation, save/reopen and undo/redo.

Flutter MatrixTransition applies FilterQuality only while isAnimating is true.
Forward/reverse enable filtering; dismissed/completed disable it.
AlwaysStoppedAnimation has a constant value but forward status, so local
constants and isolated Canvas previews still apply the configured filter.

Canvas never evaluates project code: animation references preview at 0 turns,
Alignment references at center, with diagnostics naming affected properties.
Generated Dart retains the actual live references. Native State/Child identity
are retained across local edits, source-preview changes and resizing.

Extreme finite turns can overflow native turns * pi * 2, and extreme pivots can
produce non-finite geometry. The existing matrix-frame guard is also used here:
unsafe paint, semantics and pointer input are withheld for that Canvas frame;
stored values, source generation and native child State are preserved and recover
when values become usable. This is not a repair or normalization of project Dart.

Structured numeric/alignment editors validate local drafts and typed source
selection before commit. Cancel does not publish a draft. Required Turns cannot
be reset/unset; optional properties preserve omission versus explicit null.
Refresh retains property/group identity and reopened rows remain editable.
Four reviewed SVG variants cover 16/32 sizes and light/dark presentation.

## Inventory

200 admitted definitions, 7,255 writable rows (7,237 outside Scaffold).
192 definitions expose typed properties; 166 are const-capable.
Material 51; Layout 60; Scrolling 52; Basic 26; Accessibility 6; Interaction 5.
Boolean-only rows remain 667, nullable booleans 51.
Optional insertion: 36,000 candidates over 180 destinations, 24,085 accepted
and 11,915 rejected. There are 160 any-widget and 20 trait destinations.
Required-wrapper counts remain 39 (29 root-compatible).
Events remain 172 rows / 58 types, callables 234 / 83 types, builders 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

225 focused Canvas cases passed (including the final full-suite run), including positive/negative/fractional turns,
zero identity, physical pivots, RTL, tight/empty children, all filter values,
source isolation, State/child preservation, resizing, selection/drop and
extreme finite matrix quarantine/recovery.
Flutter analyze reported no issues. All four SVG variants were rendered and
visually reviewed. Full core Maven tests passed. The existing RotatedBox schema
test now stops at the next widget-record boundary rather than a hard-coded
neighbor; its exact expected RotatedBox contract is unchanged.

The pinned SDK test passed **83 generated/native runtime cases**: 66 saved/reopened
source/RTL cases, one live source replacement/alignment/listener case, four real
controller frame cases, four status/filter matrices, seven pointer cases and
one ancestor-bound case. All 16 animation/alignment reference shapes passed
source evidence; incompatible animation/alignment types were rejected.
The integer-generation test compares exact numeric value, allowing the shared
formatter to choose equivalent scientific notation for large numbers.

The full Canvas suite passed: **8,706 cases**. It includes the new source/local
mode-switch identity test and fractional-turn selection cases.
Fresh Java reports: **3,885 tests in 572 suites**, zero failures/errors,
102 skipped opt-in cases (3,783 executed). The RotationTransition pinned-SDK
test executed, without skips. Broad regression passed after correcting the
remaining Layout item-count assertion from 59 to 60; the exact list/order
assertions still apply. Source and actual Web artifact contract tests passed.

Web release build passed. The source manifest covers 40 files and the Web
manifest covers 35 files. The rebuilt main.dart.js is 3,869,263 bytes,
SHA-256 `7dc8684c445ea329c5f8ae005172850fa00fdbeebb1d20e0514f441e45c42cb6`.
Final Maven install/package and nbm:cluster both passed. Archive verification
matched the compiled schema/editor/generator classes, all four SVG variants and
all 40 packaged runner sources against current sources/manifests; all 35 Web
artifact files match their manifest. All four checked development-cluster JARs
match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,989,182 bytes, built 2026-09-14T12:55:51.9761039Z, SHA-256
`f5f325a14defc4191b27959e9ddd5578de93e24f39d82e6049d97548072669c7`.
Whitespace diff check passed.
No manual IDE interaction or restart is performed.
