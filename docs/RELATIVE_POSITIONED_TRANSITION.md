# RelativePositionedTransition

Pinned Flutter SDK: 3.44.8, transitions.dart (constructor and build implementation).
Official [constructor](https://api.flutter.dev/flutter/widgets/RelativePositionedTransition/RelativePositionedTransition.html)
and [API](https://api.flutter.dev/flutter/widgets/RelativePositionedTransition-class.html).

## Constructor and ownership

All required public inputs are supported: rect (Animation<Rect?>), size (Size)
and Child. Key follows shared Designer identity policy. Eight writable rows
represent the two composite values, not eight Flutter constructor arguments.

- Rectangle animation: local physical LTWH rectangle, null-value stopped
  animation, or analyzer-verified Animation<Rect?> reference/getter/factory.
  The animation itself is non-null; a nullable Rect value is a separate case.
- Rect left/top/width/height: signed finite logical pixels, including zero and
  negative extents. Creation is Rect.fromLTWH(0,0,48,48).
- Reference size: local signed Size or typed non-null Size reference/getter/factory.
  Local width and height default to 48. Zero and negative reference dimensions
  are native-admissible; they are not silently normalized.
- Child: one required box child. Palette creation wraps an existing direct
  Stack child transactionally, never creating an incomplete wrapper.

Local rect emits const AlwaysStoppedAnimation<Rect?>(const Rect.fromLTWH(...));
the null-value preset emits const AlwaysStoppedAnimation<Rect?>(null).
Local size emits const Size(...). Sources retain their exact Dart expressions.
Controllers, RectTween, curves and animation lifetime remain in user Dart.
There is no Duration, Curve, onEnd, textDirection, clipping or hit-test-control
constructor argument and no native Event added.

## Actual layout, not scaling

Flutter computes RelativeRect.fromSize(rect.value ?? Rect.zero, size) and passes
its physical insets to Positioned. Left/top are rectangle coordinates; right and
bottom are reference size minus rectangle right/bottom. RTL does not reverse them.

If actual Stack size differs from reference Size, the child's derived size
changes by that difference; no proportional scaling or LayoutBuilder is
implicitly inserted. Even a null animation value can yield a nonzero child
when actual and reference sizes differ. Negative derived layout sizes clamp to
zero. Stack controls clipping; ordinary ancestor bounds still restrict pointer
dispatch with Clip.none.

The Designer conservatively requires direct Stack.children. Root, IndexedStack,
Column/Row and arbitrary wrapper paths are rejected. IndexedStack inserts native
render wrappers and is not an equivalent parent. Replacing the required child is
supported; removing or moving it out without replacement is rejected. Moving
the whole wrapper within/between legal Stacks is supported.

All-positioned Stacks still require bounded native constraints. Canvas keeps the
existing explicit diagnostic and recovery behavior for unbounded layouts.

## Editors, validation and Canvas

Rectangle component edits atomically choose local rect, whether the previous
mode was project source or null-value. Reference dimension edits choose local
Size only. Neither operation changes the other source mode. Inactive numeric
drafts survive source switching, save/reopen and Undo/Redo.

Scalars must be finite; integers use the portable +/-9007199254740991 range.
Local LTWH edges, derived rectangle sizes and locally computable reference
offsets must remain finite. Invalid/missing fields yield diagnostics, not crashes.
Inactive drafts do not participate in composite calculations. This does not
guarantee finite geometry for every arbitrary extreme combination with actual
parent constraints or source-produced values; source runtime values remain
user-owned.

Canvas never executes project references. Source rect previews as
Rect.fromLTWH(0,0,48,48), source Size as Size(48,48), with individual explanatory
tooltips. The null-value preset uses actual native null/Rect.zero semantics.
Native RelativePositionedTransition sits outside render instrumentation so the
StackParentData reaches the correct parent. Geometry tracking, collapsed
selection targets, DnD, native State and child identity are preserved.

Four SVG variants (16/32, light/dark) were rendered and visually reviewed.

## Inventory and verification

203 definitions; 7,273 writable rows (7,255 outside Scaffold); 195 typed and
169 const-capable definitions. Categories: Material 51, Layout 63, Scrolling 52,
Basic 26, Accessibility 6, Interaction 5. Booleans remain 667 plus 51 nullable.
Optional insertion: 181 destinations, 36,743 candidates, 24,394 accepted and
12,349 rejected. Required wrappers: 41 (29 root-compatible).
Events remain 172 / 58 types, callables 234 / 83 types and builders 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

584 focused Canvas cases passed, including 576 native geometry combinations
across rectangles/null, reference sizes, RTL, StackFit and Clip. Core and UI tests
cover complete domains, generation, source/local independence, finite overflow,
required-child placement, stable property rows, reopening and draft rejection.

The pinned SDK test passed **71 generated/native runtime cases**: 60 saved/reopened
cases across both text directions and parent resizing, one live animation/Size
replacement and listener-detachment case, two real nullable-endpoint RectTween
controller/reversal cases and eight pointer/Stack-clipping cases. All eight
reference forms for each of rect and size passed actual candidate analysis.
Animation<Rect> covariance and Animation<Rect?> with null values are accepted;
outer-nullable animations, nullable Size, wrong types and dynamic values are
rejected by real analyzer evidence. Canvas does not execute these references.

The full Canvas suite passed **10,210 cases**. Four reviewed SVG variants and
their registry contracts are included. Core tests passed; a defensive validator
fix ensures missing/corrupt numeric fields yield diagnostics instead of an
unboxing exception. Existing historical palette matrix counts are preserved.

Fresh reports from this slice cover **3,908 Java tests** across 581 reports:
**0 failures, 0 errors, 102 skipped**. This combines full core, focused/broad
plugin, real-SDK and artifact-contract checks; it is not a claim that every
plugin integration test ran. The dedicated SDK test ran without being skipped.
The final 584-case focused Canvas rerun passed after formatting changes.
Flutter analyze reports no issues.

The Web release build passed. main.dart.js: **3,874,570 bytes**,
SHA-256 `37cb86761c0a4ce83429c249c104667e8e81b4c3689349e9b04057ed2e3f6b75`.
The 40-file source bundle and 35-file Web artifact manifests were refreshed
from actual files; artifact contract tests passed.

Both `mvn -q -pl netbeans-plugin -am -DskipTests install` and
`mvn -q -pl netbeans-plugin nbm:cluster` passed. Reopening the NBM verified
current schema, generator/catalog/placement, property/evidence classes, four
SVG variants, 40 bundled runner sources and the current 35-file Web manifest.
All four checked development-cluster JARs match the NBM byte-for-byte.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
in the canonical `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`
checkout. **9,011,510 bytes**, modified UTC **2026-09-14T14:25:08.9539274Z**,
SHA-256 `c9fb0f29a5b2fd7441de1e2ac48fdcb85bdfe2cc7fcc3e55b8ddac7c03161f9e`.

Whitespace validation (`git -c core.whitespace=cr-at-eol diff --check`) passed.
The IDE was not restarted and an interactive desktop smoke test is not claimed.
Next planned widget: DecoratedBoxTransition.
