# Scaffold bottom-sheet scrim builder — G1

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`material/scaffold.dart` constructor and field declaration, and the official
[bottomSheetScrimBuilder API](https://api.flutter.dev/flutter/material/Scaffold/bottomSheetScrimBuilder.html).
The pinned declaration is authoritative for this release.

## Property and callback contract

`Scaffold.bottomSheetScrimBuilder` is a **BUILDER**, edited under
**Properties → Appearance → Bottom sheet scrim builder**. It is not an Event;
Scaffold retains only `onDrawerChanged` and `onEndDrawerChanged` in Events.

The exact SDK field type is:

```dart
Widget? Function(BuildContext, Animation<double>)
```

The constructor argument is optional, but the callback itself is **non-null**.
Omitting the property preserves Flutter's actual private default scrim builder.
Explicit callback null is invalid. A valid callback may return null to suppress
the scrim, or return a Widget to supply it. These states are not interchangeable.
Adjacent SDK prose suggests a null callback fallback, but the pinned constructor
default and non-null field declaration determine the implemented contract.

The callback receives the Scaffold build context and an `Animation<double>`.
Flutter maps bottom-sheet coverage from 70% to 100% of Scaffold height to animation
values from 0 to 1. Application code owns the returned widget and any intended
animation behavior; Designer does not invent a project scrim policy.

## Editing, persistence and generation

The ordinary typed project-reference editor supports local or canonical package
references, getters, static/instance members and zero-argument factories that
produce a compatible callback. The existing explicit reference/invocation and
constness model is retained. Arbitrary Dart expressions, raw function literals,
legacy callback shorthand and explicit null are not admitted for this property.

Applying or resetting the property uses the existing analyzed model/source
transaction. Reset removes the constructor argument; it does not substitute an
invented no-op builder. Save/reopen and Undo/Redo retain reference identity and
factory semantics, and regeneration preserves user-owned function/getter/factory
bodies. Rejected edits leave the current pair and history unchanged. Event
Create/Rename actions and State bindings are not offered for this builder.

Generation emits the exact reference or invocation and retains its symbol and
static-type requirements. Omitted configuration leaves the existing Scaffold
const eligibility unchanged. Explicit references/factories follow the shared
const-safety rules rather than forcing a const invocation. There is no fabricated
SDK typedef for this anonymous function signature.

## Exact analyzer proof

Only this reviewed anonymous function signature is added to the closed type
allowlist; arbitrary function-type text is still rejected. Analyzer evidence
independently qualifies `Widget`, `BuildContext` and `Animation` through an
accepted Flutter SDK library, and `double` through dart:core. Normal candidate
diagnostics alone are insufficient evidence, including when source contains
diagnostic suppressions.

The strict function-assignment proof is accompanied by a separate call-result
witness requiring `Widget?`. This rejects dynamic-return functions that ordinary
function assignability could otherwise accept. Dynamic or nullable callback
references, nullable callback factories, wrong results, async results and
incompatible parameter types are rejected. Valid non-null Widget results, null
results and compatible broader parameter types remain supported.

These witnesses are analyzer-only; they never execute application code or write
probe text into the user's physical source. Existing exact-manifest, source
identity and trusted-root checks remain intact. Generic source-type overrides
and bounds are not admitted for this signature.

## Isolated Canvas behavior

The Canvas payload carries only anonymous reference presence; it does not expose
the project symbol or import identity. Canvas never resolves or invokes the
application builder, getter or factory. With a configured project builder it
retains the real SDK default and presents an explicit approximation notice.
With the property omitted it uses the actual SDK default without claiming that
the application supplied a builder.

Consequently, a project's custom scrim appearance or null-return behavior must
be checked in the application or a Flutter widget test, not inferred from the
isolated preview. The default animated scrim and its real bottom-sheet coverage
mapping are exercised by Canvas tests.

## Scope and inventory

Scaffold has **18 properties** and the unchanged `appBar`, `body` and
`floatingActionButton` slots. This slice does not add the remaining widget-valued
Scaffold slots, `persistentFooterDecoration`, a general BoxDecoration expansion,
raw code customization or additional Events/State bindings.

Current inventory is **94 widgets / 3,990 properties / 140 callables**, including
**125 native Events, 12 builders, 1 predicate and 2 formatters**. The 86
const-capable definitions and State consumer coverage of 129 properties across
47 widget types are unchanged. G1 is closed; **six callable gaps across four
widgets** remain. Next is **G2 — AppBar.notificationPredicate**, preserving its
existing presets while adding project-handler bindings.

FD schema **16**, Catalog API **15**, Canvas model protocol **19** and transport
protocol **1** are unchanged. No frozen schema is rewritten.

## Verification

Measured automated results on 2026-09-08:

- Full `flutter-designer` suite: **1,982 tests / 235 suites**, zero failures,
  errors or skips.
- Core contract/codec/generation/command-history tests: **8/8**.
- Property editor tests: **5/5**.
- Real SDK test: **1 Java scenario**, including **3 generated Flutter widget
  tests**. It accepts all eight reviewed reference/getter/member/factory shapes
  and rejects nine invalid signature, dynamic and nullable-callback cases.
- Mutation integration regression: **1/1**; evidence-gate regression: **1/1**.
- Full Flutter Canvas suite: **1,789/1,789**; `flutter analyze` is clean and the
  offline Web release build passes.

- Selected broad NetBeans regression: **950 tests / 44 suites**; analyzer/probe
  regression: **32 tests / 2 suites**; packaged integration checks: **9 tests**.
  All completed with zero failures, errors or skips. The broad selection includes
  all five interaction-wrapper SDK regressions and State expansion; it is not a
  claim that every historical plugin test was rerun.
- `mvn verify`, reactor `install` and both root/module `nbm:cluster` builds pass.
  All **1,668** non-manifest module payload entries agree between the built JAR,
  NBM and both launch clusters. The complete `flutter-designer`, `dart-analysis`
  and `flutter-canvas-runner` dependency JAR hashes agree as well. Source/Web
  bundle checks cover all **40 source / 35 Web entries**.

Artifact:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`

- Size: **8,218,787 bytes**.
- SHA-256: `ae09c386d2fe7b934a51ead994068f959b1f8555624f0d0a1536461bbf38cec0`.
- Web `main.dart.js`: **3,346,313 bytes**;
  SHA-256: `ee8ea718f3c6007f040d01ca36059fc5bd88e0fe43ff6a7ee25daf943a90952b`.

The installed IDE was not restarted and the user's Flutter application was not
modified. Automated results do not claim a fresh manual NetBeans desktop smoke
test.
