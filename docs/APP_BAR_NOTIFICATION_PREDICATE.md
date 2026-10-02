# AppBar notification predicate — G2

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`material/app_bar.dart` and `widgets/scroll_notification.dart`, and the official
[AppBar.notificationPredicate API](https://api.flutter.dev/flutter/material/AppBar/notificationPredicate.html)
and [ScrollNotificationPredicate typedef](https://api.flutter.dev/flutter/widgets/ScrollNotificationPredicate.html).
The pinned SDK declaration is authoritative for this release.

## Property and callback contract

The existing **Properties → Behavior → Scroll notifications** row gains project
bindings. It is a **PREDICATE**, not an Event or an automatic State producer.
AppBar still has no directly bindable native Events.

The constructor argument is optional and defaults to
`defaultScrollNotificationPredicate`; its type and return value are non-null:

```dart
typedef ScrollNotificationPredicate = bool Function(ScrollNotification notification);
```

The three existing string presets retain their exact model values and generated
expressions:

| Model value | Generated argument | Policy |
| --- | --- | --- |
| Omitted | No notificationPredicate argument | Actual SDK depth-zero default |
| `default` | `defaultScrollNotificationPredicate` | Accept depth zero |
| `depthZero` | `(notification) => notification.depth == 0` | Accept depth zero |
| `all` | `(_) => true` | Accept every notification passed to this predicate |

Typed `DartObjectReferenceValue` additionally supports local or canonical package
callables, getters, static/instance members and zero-argument factories returning
a compatible predicate. The shared explicit reference/invocation and constness
model is preserved. Explicit null, legacy CallbackValue shorthand, unknown
presets, raw Dart expressions and anonymous code snippets are rejected.

## Actual AppBar behavior

This predicate filters AppBar's scroll-under handling; its bool result does
**not** consume a notification or stop bubbling. That differs from the bool
result of `NotificationListener.onNotification`.

The pinned AppBar invokes the predicate for ScrollUpdateNotification. For a
selected update it derives the vertical scroll-under state from `extentBefore`
for `AxisDirection.down`, or `extentAfter` for `AxisDirection.up` (reverse).
Horizontal updates do not change that state. Other notification kinds do not
gain AppBar scroll-under behavior merely because the selected preset is `all`.
The resulting state participates in the existing scrolled-under elevation and
theme behavior; G2 does not add a separate elevation policy.

## Editing, persistence and generation

For example, a user-owned function can select a nested scroll view:

```dart
bool acceptNestedScroll(ScrollNotification notification) => notification.depth == 1;
```

Keep that function in user-owned Source code, outside the managed generated
regions. Open **Scroll notifications**, choose **Project reference**, select
the current library and enter `acceptNestedScroll` as the root symbol using
reference access. For another library, select its canonical package URI. The
optional member field supports static/instance members; invocation access is
for a zero-argument factory returning the predicate, not a call with a scroll
notification. Pressing OK validates the reference and the paired candidate;
Cancel publishes no changes.

Preset/reference switching updates the same existing property. Applying,
resetting, save/reopen and Undo/Redo use the analyzed paired source/model
transaction and preserve user-owned handler/getter/factory bodies. Reset removes
the constructor argument and restores the SDK default; it does not substitute
an invented always-true predicate. Rejected edits preserve the current pair and
redo history. Events Create/Rename and State binding are not offered for this
predicate.

The AppBar compound generator now carries the typed reference's complete
rendered expression, import alias and exact symbol/static-type requirements.
It does not stringify the binding or discard the proof while assembling the
AppBar constructor. All old preset expressions are unchanged. AppBar remains a
non-const constructor, independently of a configured reference's invocation
constness. No SDK typedef or new serialization kind is invented.

## Exact analyzer evidence

The existing SDK `ScrollNotificationPredicate` assignment proof is supplemented
with an independent SDK-qualified ScrollNotification argument and dart:core
bool-result witness. This closes the dynamic-return case that ordinary function
assignability can accept. Dynamic/nullable callback references and factories,
nullable/dynamic/wrong/async results and incompatible parameters are rejected.
Compatible broader parameter types remain valid.

The narrow witness applies to the existing widgets.dart/material.dart SDK proof
family, also hardening RefreshIndicator's predicate without changing its model
or presets. It does not broaden the closed Dart type grammar, accept arbitrary
proof libraries or weaken exact-manifest, source-identity and trusted-root
checks. Candidate diagnostic suppressions are not a substitute for strict type
evidence.

All proof witnesses are analyzer-only. They never invoke application predicates,
getters or factories, and do not write probe code to the physical user source.

## Isolated Canvas behavior

Omission and all three reviewed presets use their actual policies in Canvas.
A custom typed binding carries anonymous reference presence only, with project
identifier and import identity stripped from the payload. Canvas never resolves
or executes that project code; it uses the SDK depth-zero default and displays
an explicit custom-predicate approximation notice. This limitation is confined
to custom references, not the exact preset path.

The saved model and generated Dart retain the exact custom predicate. Its
application-specific filtering must be checked in the application or a Flutter
widget test, not inferred from the isolated Canvas approximation.

## Scope and inventory

AppBar retains **120 flattened property rows / 28 constructor properties / five
slots**. G2 widens one existing property's admitted value kinds; it adds no
property, slot, widget, native Event or State binding.

Current inventory is **94 widgets / 3,990 properties / 141 callables**, including
**125 native Events, 12 builders, 2 predicates and 2 formatters**. Exactly 25
widget definitions have a supported callable. The 86 const-capable definitions
and State consumer coverage of 129 properties across 47 widget types are
unchanged. **Five callable gaps across three widgets** remain; next is
**G3 — ElevatedButton.styleBackgroundBuilder/styleForegroundBuilder**.

FD schema **16**, Catalog API **15**, Canvas model protocol **19** and transport
protocol **1** are unchanged. Frozen schemas, unrelated Scaffold slots and
nested decoration graphs are not expanded by G2.

## Verification

Measured on 2026-09-08:

- Full `flutter-designer`: **1,992 tests / 237 suites**, zero failures, errors or
  skips; includes the eight new G2 core contract/history tests and two payload
  regressions.
- Analyzer/probe regression: **33 tests passed**, including the narrow
  SDK-qualified bool-result witness.
- G2 property editor: **6/6**; shared RefreshIndicator property regression:
  **5/5**.
- G2 mutation lifecycle and exact evidence-gate regression: **1/1 each**.
- Real SDK: **one Java scenario with three generated Flutter runtime tests**,
  all passing. Eight compatible binding cases and nine invalid result/parameter/
  dynamic/nullable cases are checked, plus a local user-owned function and
  unchanged physical source during analysis. Runtime tests use actual nested
  scrolling, elevation changes and ancestor notification delivery.
- Canvas: **14 targeted tests**, full Flutter suite **1,803/1,803**;
  `flutter analyze` clean and offline Web release successful. All **40 source /
  35 Web entries** independently checked for exact size and SHA-256.

- Broad selected NetBeans regression: **964 tests / 47 suites**; analyzer/probe:
  **33 tests / 2 suites**; packaged integration checks: **9 tests**. Zero
  failures, errors or skips. Selection includes the five interaction-wrapper
  SDK regressions, G1, State expansion and shared predicate editor regression;
  it is not a claim that every historical plugin test was rerun.
- Reactor `verify`, `install` and both root/module `nbm:cluster` builds pass.
  All **1,668 non-manifest module payload entries** agree between the built JAR,
  NBM and both launch clusters. Complete `flutter-designer`, `dart-analysis`
  and `flutter-canvas-runner` dependency JAR hashes also match.

Artifact:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`

- Size: **8,220,623 bytes**.
- SHA-256: `8bb9eb7183ed2573b9ff5e52c5388d96fc74b1148a54adb224a392b58f36eb41`.
- Web `main.dart.js`: **3,346,907 bytes**;
  SHA-256: `4127da195e5d66238b2bb9c92a2093d123712120552910f51597b52929aaadd2`.

The installed IDE was not restarted and the user's Flutter project was not
modified. No fresh manual NetBeans desktop smoke test is claimed.
