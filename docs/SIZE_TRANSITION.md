# SizeTransition

Pinned SDK: Flutter 3.44.8, verified against
packages/flutter/lib/src/widgets/transitions.dart.
Official [constructor](https://api.flutter.dev/flutter/widgets/SizeTransition/SizeTransition.html)
and [API](https://api.flutter.dev/flutter/widgets/SizeTransition-class.html).

## Complete constructor projection

All five scalar arguments and the optional box Child are supported.
Key follows the shared Designer identity policy.

- Axis: omitted/vertical/horizontal, with native vertical default.
- Size factor: required finite signed local number or strict non-null
  Animation<double> source. Local values generate
  `const AlwaysStoppedAnimation<double>(value)`; creation starts at 1, a
  Designer prototype value, not a Flutter constructor default.
  Integers follow the shared portable range +/-9007199254740991.
  Negative values are preserved in FD/Dart; native layout clamps them to zero.
- Axis alignment: omitted, explicit null, finite signed local number or strict
  double? source. This deprecated SDK argument remains supported. Values outside
  [-1,1] are legal; horizontal alignment follows text direction.
- Alignment: omitted, explicit null, physical Alignment, directional
  AlignmentDirectional, or strict AlignmentGeometry? source.
- Fixed cross-axis size factor: omitted, explicit null, finite nonnegative local
  number or strict double? source. Zero and values greater than one are supported.
- Child: optional single box slot with the normal placement and parent-data
  constraints. Add, replace, remove, move and wrap are transactional.

All four source-capable values support the eight combinations of current/imported
library, root/member and getter/zero-argument factory. Nullability is checked
against the exact argument type; plain numbers are not Animation<double>.

Alignment and axisAlignment are mutually exclusive when non-null. Omission and
explicit null remain distinct serialized states. One strategy can be cleared
before selecting the other, or switched with an atomic property patch.
Conservative source rule: a configured nullable reference is potentially non-null;
two configured strategies are rejected even if one reference might return null
at runtime. Clear or explicitly null one strategy instead. The Designer does not
execute user Dart to determine nullness.

## Native layout behavior

This is ClipRect + Align layout, not a paint-only transform. The animated main
axis uses max(sizeFactor.value, 0); the cross axis uses fixedCrossAxisSizeFactor.
With a null cross factor, a bounded cross axis expands to its available maximum;
an unbounded cross axis shrink-wraps the child. Parent constraints remain
authoritative, and tight constraints may prevent visible shrinking.
Native ClipRect uses hard-edge clipping and constrains pointer hits.

The pinned implementation's fallback is directional, not always Alignment.center:
horizontal uses AlignmentDirectional(axisAlignment ?? 0, -1); vertical uses
AlignmentDirectional(-1, axisAlignment ?? 0). Thus main-axis alignment defaults
to center while the cross axis defaults to top/start. RTL is resolved by Flutter.
The implementation is authoritative where the constructor prose describes a
simplified center default.

There is no Duration, Curve, On end, clipBehavior, filterQuality or
transformHitTests constructor argument. This widget has no callback/builder
Events. AnimationController/Tween construction and lifetime remain in user Dart.
Non-finite source results or negative runtime cross factors violate the native
contract; source-owned runtime values are not silently repaired by the Designer.
Finite local input validation is not a promise that arbitrary extreme products
will yield finite native geometry.

## Editors and isolated Canvas

Required Size factor cannot be unset/reset. Nullable properties preserve
omitted/null/local/source states; structured editors validate drafts before
commit and Cancel does not publish them. Properties and groups retain identity
across refresh, and reopened rows remain editable.

Canvas constructs native SizeTransition with an AlwaysStoppedAnimation and
retains native State/child identity across local updates and source-preview
changes. It never executes project code: source sizeFactor previews at 1;
source alignment, axisAlignment and fixedCrossAxisSizeFactor preview at null.
Diagnostics identify these substitutions. Generated Dart keeps the real
references and follows live animations.

Zero-size and empty children remain selectable with the shared hit-candidate
logic and accept legal box-child drops. Four reviewed SVG variants provide
16/32 light/dark palette and tree icons.

## Inventory

201 admitted definitions, 7,260 writable rows (7,242 outside Scaffold).
193 definitions expose typed properties; 167 are const-capable.
Material 51; Layout 61; Scrolling 52; Basic 26; Accessibility 6; Interaction 5.
Boolean-only rows remain 667, nullable booleans 51.
Optional insertion: 36,381 candidates over 181 destinations, 24,392 accepted
and 11,989 rejected. There are 161 any-widget and 20 trait destinations.
Required-wrapper counts remain 39 (29 root-compatible).
Events remain 172 rows / 58 types, callables 234 / 83 types, builders 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

648 focused Canvas cases passed: both axes and text directions, signed/zero/
fractional factors, null/zero/fractional/expanding cross factors, physical/
directional/deprecated alignment, loose/tight constraints, empty/zero-size
selection and drop, source isolation and clipped pointer dispatch.

Full core Maven tests passed. The constructor test covers 750 combinations
through FD round-trip and generated Dart; all 32 source-reference shapes retain
their exact required static types. Numeric/null/conflict rejection and
transactional child edits are covered. The existing SingleChildScrollView schema
test now ends at the next widget record rather than a hard-coded neighbor; its
expected SingleChildScrollView contract is unchanged.

The pinned SDK test passed **127 generated/native runtime cases**: 108
saved/reopened source/RTL cases, one live source replacement/listener test,
two real-controller frame/tight-constraint cases, four empty-child bounded/
unbounded cases and 12 clipped pointer cases. Real analyzer evidence accepts all
32 supported reference shapes and rejects incompatible animation, numeric and
alignment types. The saved user members remain byte-preserved. Property editor
tests passed, including stable rows/groups, reopen, null/reset/reference modes,
invalid drafts and cancel behavior.

The full Canvas suite passed **9,354 cases**. After adding explicit loop blocks
to satisfy the analyzer, the 648 focused cases passed again. Flutter analyze
reported no issues. Broad Java regression passed with no failures or errors.
Four SVG variants were rendered and visually reviewed.

Fresh Java reports: **3,892 tests in 575 suites**, zero failures/errors,
102 skipped opt-in cases (3,790 executed). SizeTransitionRealSdkTest executed
without skips. Source-bundle and actual Web artifact contract tests passed.

Web release build passed. The source manifest covers 40 files; the Web manifest
covers 35. Rebuilt main.dart.js: 3,871,116 bytes,
SHA-256 `38473a0c5b7256f85f8c4e9a49bf6d07457a831cb5ce21a578416178e1244912`.

Final Maven install/package and nbm:cluster passed. Archive verification matches
the current compiled schema, validator, generator and editor classes, all four
SVG variants and all 40 packaged runner sources. The 35 Web artifact files match
their updated manifest. All four checked development-cluster JARs match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,993,545 bytes, built 2026-09-14T13:26:04.3428063Z, SHA-256
`3756f6993d25c4a070569d6f7c3befb75f63dc904f5d84604e2943c6ba018fce`.

Whitespace diff check passed. No manual IDE interaction or restart was performed.
Next planned slice: PositionedTransition.
