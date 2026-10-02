# PositionedTransition

Pinned SDK: Flutter 3.44.8, checked against
packages/flutter/lib/src/widgets/transitions.dart.
Official [constructor](https://api.flutter.dev/flutter/widgets/PositionedTransition/PositionedTransition.html)
and [API](https://api.flutter.dev/flutter/widgets/PositionedTransition-class.html).

## Complete constructor projection

Both public inputs, required rect and required Child, are supported.
Key follows the shared Designer identity policy. Five property rows represent
the one composite rect argument; these are not five Flutter constructor inputs.

- Rectangle animation: local physical RelativeRect insets or strict non-null
  Animation<RelativeRect> reference/getter/zero-argument factory.
- Rect left/top/right/bottom: finite signed logical-pixel insets from the four
  physical edges of the parent Stack. Integers use the shared portable range
  +/-9007199254740991. These are not LTWH coordinates or fractional offsets.
- Required Child: one non-null box child. The palette wraps an existing direct
  Stack child transactionally; it does not insert an incomplete empty wrapper.

Local values generate
`const AlwaysStoppedAnimation<RelativeRect>(const RelativeRect.fromLTRB(l,t,r,b))`.
The zero-inset creation prototype fills the Stack; it is a Designer choice,
not a Flutter default. Edits are immediate rather than implicit transitions.
Editing any local edge while a source is selected atomically switches rect to
local and keeps the other three stored edges. Choosing a source preserves all
inactive local edges. Every required row rejects unset/reset/null.

The eight source forms cover current/imported libraries, root/member symbols,
getters and zero-argument factories. The exact static type is checked:
RelativeRect alone, Animation<Rect>, outer/value-nullable animation types,
dynamic, Animation<dynamic>, unrelated generics and ValueNotifier are not
substitutes. Controllers, RelativeRectTween, curves and animation/listener
lifetime remain in user Dart. The widget has no Duration, Curve, On end,
Text direction, clipping or hit-test-control constructor input and no Events.

## Placement and native behavior

Designer placement requires direct Stack.children. Root, Column/Row, IndexedStack
and arbitrary wrapper-child placement are rejected. IndexedStack inserts native
render wrappers, so it is not interchangeable here. This is deliberately
conservative: arbitrary user-defined Stateless/Stateful wrapper paths that
Flutter might permit are not inferred as parent-data-transparent model paths.

Required child replacement is supported. Removing/moving that child without a
replacement is rejected; the whole PositionedTransition can move/reorder between
legal Stack children. Nested Stack-parent-data or Flex-parent-data children and
slivers cannot be wrapped inside this required box child.

Native Positioned.fromRelativeRect determines layout. The four insets are
physical even in RTL. Opposing insets determine size; negative derived dimensions
clamp to zero. Negative insets can extend outside Stack bounds. Stack owns
clipping; normal ancestor bounds still govern pointer dispatch even with Clip.none.
A positioned-only Stack needs finite bounds. Canvas provides an explicit
unbounded-Stack diagnostic rather than inventing a document size; legal bounded
or non-positioned-sibling layouts recover normally.

Finite scalar admission is not a guarantee that arbitrary extreme combinations
or source-produced values yield finite native geometry. Runtime values and
controller lifetime remain source-owned.

## Editors, Canvas and packaging

Structured preset/reference and numeric editors validate unpublished drafts.
Cancel retains the previous value. Stable property/group identity, save/reopen
and byte-preserved user members are covered by regression tests.

Canvas never executes project references. Animation sources preview with
AlwaysStoppedAnimation<RelativeRect>(RelativeRect.fill), and the tooltip names
this substitution. Generated Dart preserves the live reference. The native
animation widget is outside render instrumentation so StackParentData reaches
the correct parent. Native State and child identity survive local/source
preview changes and resizing.

The existing positioned geometry observer updates coincident zero-size selection
handles and DnD coordinates. Palette wrapping uses the real Canvas resolver.
Four reviewed SVG variants cover 16/32 sizes in light/dark presentation.

## Inventory

202 admitted definitions; 7,265 writable rows (7,247 outside Scaffold).
194 definitions have typed properties; 168 are const-capable.
Material 51; Layout 62; Scrolling 52; Basic 26; Accessibility 6; Interaction 5.
Booleans remain 667 plus 51 nullable-boolean rows.
Optional insertion: 181 destinations (161 any-widget, 20 trait), 36,562 candidates,
24,393 accepted and 12,169 rejected.
Required wrappers: 40, of which 29 are root-compatible.
Events remain 172 rows / 58 types, callables 234 / 83 types and builders 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

272 focused Canvas tests passed, including 264 physical geometry combinations,
source isolation, native State/child preservation, zero-size selection,
actual palette-wrap DnD resolution, intrinsic sizing and unbounded diagnostics.

Full core tests passed. The new generation matrix covers 144 local rectangles
through FD round-trip and exact stopped-animation Dart generation; all eight
source-reference shapes retain strict Animation<RelativeRect> evidence.
Required-child and all-catalog placement matrices are covered. Existing box-slot
tests now include the new Stack-only type in the parent-data restrictions.

Properties tests passed, covering required rows, source/local modes, atomic
edge edits, inactive value preservation, stable rows/groups, reopen and invalid
draft rejection. Four SVG variants were rendered and visually reviewed.

The pinned SDK test passed **49 generated/native runtime cases**: 38
saved/reopened source/RTL/resize cases, one live source replacement/listener
case, two real RelativeRectTween/controller/reversal cases and eight
physical pointer/Stack-clipping cases. All eight source-reference forms passed
actual analyzer evidence; incompatible animation types were rejected.

Inactive local edits and structurally identical child replacements may change
only FD. The SDK test verifies unchanged Dart bytes and changed FD snapshots
instead of asking for a nonexistent Dart-change analysis ticket. Move indices
follow the command API's post-removal convention. Required-child removal/move
and reset remain rejected.

The full Canvas suite passed **9,626 cases**; Flutter analyze reported no issues.
The existing schema-neighbor test now ends at the next widget-record boundary,
preserving its exact old expected contract. The SVG families use explicit
16/32 viewBoxes and matching scale transforms, with a repeat visual review.

Fresh Java reports from this slice cover **3,899 tests** across 578 reports,
with **0 failures, 0 errors and 102 skipped tests**. This is the accumulated
full-core, focused/broad plugin, real-SDK and artifact-contract verification;
it is not a claim that every plugin integration test ran. The dedicated
PositionedTransition SDK test ran rather than being skipped.

The Web release build passed. `main.dart.js` is 3,872,901 bytes, SHA-256
`13aae7b32b340febd0167de6c046ace7107eb89f1d8161368785c6e1fb83794d`.
The 40-file runner source manifest and 35-file Web manifest match the current
sources/build, and their artifact contract tests passed.

`mvn -q -pl netbeans-plugin -am -DskipTests install` and
`mvn -q -pl netbeans-plugin nbm:cluster` both passed.
The NBM archive was reopened and checked against current compiled schema,
catalog/generator/placement and property/evidence classes, all four SVG variants,
40 runner source files and the 35-file Web artifact manifest. All four checked
development-cluster JARs match the NBM byte-for-byte.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
under the canonical `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`
checkout. Size: **9,003,121 bytes**; modified UTC: **2026-09-14T13:59:17.6481159Z**;
SHA-256: `a5f454736dab0b2f7f9e0d9e6dc9c6f32be09ef5f1e7330712855c23e72b7c2c`.

Whitespace validation with `git -c core.whitespace=cr-at-eol diff --check`
passed. The running IDE was not restarted and an interactive desktop
smoke test is not claimed.
