# ListView item extent builder — G5

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`widgets/scroll_view.dart`, `rendering/sliver.dart` and
`rendering/sliver_fixed_extent_list.dart` declarations.
Official API references are
[ListView.itemExtentBuilder](https://api.flutter.dev/flutter/widgets/ListView/itemExtentBuilder.html),
[ItemExtentBuilder](https://api.flutter.dev/flutter/rendering/ItemExtentBuilder.html)
and [SliverLayoutDimensions](https://api.flutter.dev/flutter/rendering/SliverLayoutDimensions-class.html).
The pinned SDK declaration and implementation are authoritative for this release.

## Property and callback contract

`itemExtentBuilder` is an ordinary **BUILDER** row under **Properties → Layout**
on the existing unnamed/static `ListView(children: ...)` constructor. It is
appended at argument order **18**, retaining all previous 17 property orders and
the children slot at order **11**. The projection now has **18 properties / one
ordered children slot**. ListView remains non-const.

```dart
typedef ItemExtentBuilder =
    double? Function(int index, SliverLayoutDimensions dimensions);

// The optional ListView constructor field is nullable:
final ItemExtentBuilder? itemExtentBuilder;
```

The two callback arguments are required positional arguments. Signature metadata
imports `package:flutter/rendering.dart`, which owns ItemExtentBuilder and
SliverLayoutDimensions; it does not invent a Widgets-exported typedef.

Omission, explicit `NullValue` and typed `DartObjectReferenceValue` are admitted.
Local or canonical package callables, getters, static/instance members and
zero-argument factories are supported, including actual nullable callbacks and
factories returning `ItemExtentBuilder?`. The closed expected type is exactly
`ItemExtentBuilder?`. No raw Dart, function literal, legacy CallbackValue, new
Event or State binding is introduced.

Reset removes only this property. Generated application Dart preserves the exact
reference/member/invocation/import alias, or explicit `itemExtentBuilder: null`;
omission stays omitted. Static child IDs, order, sparse scroll properties,
semantic count checks and existing bounded-viewport generation remain intact.
User-owned callback bodies are never rewritten by property or child edits.

## Sizing modes and null semantics

The admitted sizing modes are:

- Omitted/null callback without itemExtent: normal SDK child sizing.
- Omitted/null callback with itemExtent: the configured fixed extent.
- A non-null callback without itemExtent: callback-driven per-child extents.

Flutter's constructor permits only one non-null sizing strategy among itemExtent,
prototypeItem and itemExtentBuilder. prototypeItem is not part of this slice.
Designer conservatively rejects **any typed builder reference together with
itemExtent**, even when the reference's type is nullable: its runtime value cannot
be known without executing project code. An explicit null builder with itemExtent
is valid. The diagnostic identifies the conflict and asks the user to reset
itemExtent, reset the builder, or set the builder to explicit null. Neither field
is silently cleared; a rejected edit preserves the source/model pair and history.

**A null callback and a callback returning null are not equivalent.** The SDK
documents the callback's null result for an out-of-range item index. A configured
callback must provide a valid extent for **every actual child**. Returning null
for an existing child is not a request for default sizing and is not a supported
way to truncate the explicit child list: the pinned
`rendering/sliver_fixed_extent_list.dart` force-unwraps the callback result in
`_getChildConstraints` and `paintExtentOf` (around lines 268 and 292).

The SliverLayoutDimensions argument contains scrollOffset, precedingScrollExtent,
viewportMainAxisExtent and crossAxisExtent. Application code receives Flutter's
actual layout dimensions and remains responsible for valid runtime extents.
Static type proof cannot establish the numerical value or child-count policy of
an arbitrary user function.

## Exact analyzer evidence

Each project binding carries a nullable ItemExtentBuilder type requirement and
exact source range. The ListView owner supplies the reviewed Widgets proof
context; the analyzer resolves the actual typedef and SliverLayoutDimensions
through a fixed Rendering import. This does not widen arbitrary proof-library or
type-expression admission.

The normal nullable typedef assignment proves callback assignability. A separate
strict witness checks the **original expression's null-aware invocation result**
against dart:core double?, using a dart:core int index and an SDK-qualified
SliverLayoutDimensions argument. This rejects dynamic-return functions even where
ordinary function assignability could accept them, without rejecting a correctly
typed nullable callback. Wrong argument/result types, extra required parameters,
async results and dynamic references fail before paired save.

Existing exact-manifest, source identity, trusted-root and navigation checks remain
mandatory. A forged non-null type requirement cannot replace the original nullable
one. Proof code is analyzer-only: it never executes a project function, getter or
factory and never enters physical user source.

## Isolated Canvas limitation

Canvas receives anonymous reference presence, not project identifiers or import
URIs, and never executes application builders/getters/factories. A custom builder
reference is explicitly approximated using normal isolated SDK child sizing.
Canvas cannot determine whether a nullable reference evaluates to null, or compute
the application's variable extents. Explicit null and omission retain the normal
SDK sizing path, including a configured fixed itemExtent.

This approximation is a preview limitation, not generated application behavior:
the application receives its exact binding. Verify custom extent calculations,
layout changes and out-of-range behavior in the application or a Flutter widget
test. The isolated Canvas must not suggest that a null result supplies a default
extent for an actual child.

## Scope and audit closure

G5 closes the final **known callable gap on the admitted constructors** in the
current audit. It does not implement ListView.builder/separated/custom,
GridView.builder/custom, prototypeItem, new controller properties or delegate
graphs. It does not claim complete ListView SDK coverage, completion of the
historical palette target, or support for every Flutter event/listener API.

Current inventory is **94 widgets / 3,995 properties / 146 callables**:
**125 native Events, 17 builders, 2 predicates and 2 formatters**, across **26**
callable widget definitions. There are **zero known callable gaps within the
audited admitted-constructor scope**. The 23 native-event-bearing widget types,
86 const-capable definitions and 129 State consumers across 47 widget types are
unchanged.

FD schema **16**, Catalog API **15**, Canvas model **19** and transport **1**
remain unchanged. Frozen schemas and unrelated widget contracts are preserved.

## Verification

Measured verification on 2026-09-08:

- Analyzer/probe regression: **40 tests passed**.
- **23 targeted tests** and **1,859/1,859** full Flutter tests passed.
- `flutter analyze` clean; offline Web release successful; all **40 source /
  35 Web entries** verified for exact size and SHA-256.
- Runtime coverage includes both axes × reverse × shrinkWrap, layout dimensions
  after scroll-offset/viewport changes, and null results only beyond the
  actual children. Custom-reference preview keeps every configured child as an
  explicit approximation rather than fabricating a constant callback.
- Full `flutter-designer`: **2,035 tests / 244 suites**, zero failures, errors
  or skips. Includes seven focused schema/codec/generation/conflict/SDK-baseline
  tests and four command/history/source/child-edit tests.
- Property editor: **6 tests**, including both conflict directions and unchanged
  children, callback bindings and scroll properties.
- Exact nullable-type/trusted-navigation evidence gate and mutation lifecycle:
  **one G5 case each**, passing. Save/reopen, explicit null, Reset, Undo/Redo,
  rejected fixed/builder conflicts and user-owned Dart preservation are covered.
- Real SDK: **two Java scenarios**, including **six generated Flutter runtime
  tests**, all passing. Nine imported/member/factory shapes, a local member and
  a mixed Material/Rendering/pointer candidate pass; ten adversarial signatures
  or dynamic references fail even with source diagnostic suppression. Physical
  source remains unchanged during proof. Real generated lists preserve variable
  extents, child order, axis/reverse, scroll/resize dimensions, shrinkWrap,
  natural/null/fixed sizing and the empty-child case. Empty slivers are checked
  with offstage-inclusive finders because their geometry is not visible.
- Broad selected NetBeans regression: **1,009 tests / 47 suites**, including
  those two real-SDK scenarios; analyzer/probe: **40 tests / two suites**.
  Final source/Web contract rerun: **33 tests**; packaged integration: **9
  tests**. All final reports have zero failures, errors or skips. The Web fixture
  was refreshed after the final warning-text rebuild and its contract rerun.
  This selection covers property editors, Events/State, source guards,
  mutation/history, evidence gates, palette and Canvas packaging; it is not the
  entire historical plugin suite. Earlier widget real-SDK families are not
  claimed as freshly rerun.
- Reactor `verify`, `install` and both root/module `nbm:cluster` builds pass.
  All **1,668 non-manifest module entries** agree between the built JAR, NBM
  and both launch clusters. The complete `flutter-designer`, `dart-analysis`
  and `flutter-canvas-runner` dependency JAR hashes also match.

Artifact:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`

- Size: **8,230,604 bytes**.
- SHA-256: `b533854f9b4856b5cdd6d7965eb70efb2b181ef5f5dc14eee7b45a9574a9431c`.
- Web `main.dart.js`: **3,349,553 bytes**;
  SHA-256: `166fa94fe14a334f363533a744330a88f28f35ecfac45a95092f0d6004436d5c`.

The installed IDE has not been restarted, the user's Flutter project has not
been modified and no fresh manual NetBeans desktop smoke test is claimed.
