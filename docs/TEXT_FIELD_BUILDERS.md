# TextField counter and context-menu builders — G4

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`material/text_field.dart` and `widgets/editable_text.dart` declarations.
Official API references are
[InputCounterWidgetBuilder](https://api.flutter.dev/flutter/material/InputCounterWidgetBuilder.html),
[TextField.buildCounter](https://api.flutter.dev/flutter/material/TextField/buildCounter.html)
and [TextField.contextMenuBuilder](https://api.flutter.dev/flutter/material/TextField/contextMenuBuilder.html).
The pinned SDK declaration is authoritative for this release.

## Exact property contract

Two ordinary **BUILDER** rows are appended under **Properties → Builders**:
`buildCounter` and `contextMenuBuilder`. The first 54 TextField property orders
remain unchanged; these rows use orders 54 and 55. Existing property groups
retain their order, with Builders appended after Restoration.

Both properties admit omission, explicit `NullValue` and the existing typed
`DartObjectReferenceValue`. Local or canonical package references, getters,
static/instance members and zero-argument factories are supported. Actual
nullable callback references/factories are admitted, not merely an explicit-null
editor mode. Their exact expected types are `InputCounterWidgetBuilder?` and
`EditableTextContextMenuBuilder?`; arbitrary nullable type spellings are not added
to the closed model grammar.

```dart
typedef InputCounterWidgetBuilder = Widget? Function(
  BuildContext context, {
  required int currentLength,
  required int? maxLength,
  required bool isFocused,
});

typedef EditableTextContextMenuBuilder =
    Widget Function(BuildContext context, EditableTextState editableTextState);
```

Counter `maxLength` is a **required named argument with nullable int? type**.
The counter result may be null; a non-null menu callback must return a non-null
Widget. The callback's outer nullability and its result's nullability are distinct.

Neither row accepts legacy CallbackValue shorthand, raw Dart or arbitrary
function literals. Builders stay in Properties, not the Events Create/Bind/Rename
workflow or State binding. User-owned function bodies are preserved. Reset
removes only the selected argument and does not clear its sibling, existing
TextField properties or native Events.

## Omission, null callback and null result

| Property | Omitted argument | Explicit null or reference evaluating to null | Non-null callback returning null |
|---|---|---|---|
| buildCounter | SDK default counter policy | SDK default counter policy | Hides the counter and its Semantics |
| contextMenuBuilder | SDK default context-menu builder | Disables the context menu | Invalid: the SDK requires a Widget result |

The default counter policy still depends on the normal TextField configuration,
including maxLength and decoration counter precedence; omission does not promise
an always-visible counter. Flutter supplies the actual current text length,
nullable maximum length and focus state. The menu callback receives the live
EditableTextState and can use its supported context-menu anchors and button items.
Platform/browser context-menu behavior remains governed by Flutter and the host.

Generation retains omission versus explicit `buildCounter: null` or
`contextMenuBuilder: null`, with ordinary const eligibility. A project reference
is emitted exactly, including its import alias, member and optional invocation.
No default no-op or fabricated builder is inserted into application Dart.

## Named-parameter metadata and analyzer proof

`WidgetEventDescriptor.Parameter` now models positional/named and required/optional
independently of its Dart type. The existing two-argument constructor remains a
required positional parameter. Signatures render named braces, required keywords
and parameter names in declarations and function types. Positional parameters
must precede named parameters; duplicate or invalid names are rejected. Optional
named parameters are currently admitted only with a nullable type, which permits
an implicit null default without inventing default-value source. All three
counter named parameters are required.

Every typed binding carries an exact nullable callback type requirement. SDK
qualification uses Material for InputCounterWidgetBuilder, and Widgets/Material
for EditableTextContextMenuBuilder; the proof import does not trust an ambient
project typedef with the same name. Existing source identity, exact-manifest and
trusted-root checks remain mandatory.

The analyzer independently inspects the **original expression's null-aware
invocation** using the exact argument types and named counter arguments. A strict
SDK-qualified Widget? invocation-result witness rejects dynamic-return functions
that ordinary function assignability can otherwise accept. The nullable typedef
assignment separately rejects a menu callback with a Widget? result, while a
nullable callback reference itself remains valid. Broader compatible parameter
types remain valid; missing/wrong/extra required named arguments, wrong return
types and async/dynamic bindings are rejected before paired save.

The proof is analyzer-only. It never executes a project function, getter or
factory and is never written into physical user source. There is no schema
change or arbitrary function-type grammar expansion.

## Isolated Canvas limitation

Canvas receives anonymous reference presence, not project identifiers or import
URIs. It never executes project builders, getters or factories. A custom reference,
including a nullable one, uses the isolated SDK default with an explicit
approximation notice; Canvas cannot know whether the application reference will
evaluate to null or what widget it returns.

Explicit null is distinguishable without executing code: null buildCounter uses
the SDK counter policy, while null contextMenuBuilder disables the preview menu.
Omission uses each SDK default. Real application builder behavior, counter
semantics and menu contents must be checked in the application or a Flutter
widget test, not inferred from the isolated default approximation.

## Scope and inventory

G4 closes the two callable gaps on the already admitted TextField projection,
now **56 properties**. It does not claim complete TextField SDK coverage:
decoration graphs, styles, formatter lists, controller/focus-node property
expansion and other owner-managed configuration remain outside this projection.
Existing reviewed State workflows are unchanged; no new State producer or
consumer is introduced.

Current inventory is **94 widgets / 3,994 properties / 145 callables**:
**125 native Events, 16 builders, 2 predicates and 2 formatters**, across 25
callable widget definitions. Const-capable definitions remain 86; State consumers
remain 129 properties across 47 widget types.

Exactly **one audited callable gap in one admitted widget** remains:
**G5 — ListView.itemExtentBuilder**. This bounded audit is not completion of all
Flutter APIs, dynamic list/grid constructors or the historical palette target.

FD schema **16**, Catalog API **15**, Canvas model **19** and transport **1**
remain unchanged. Frozen schemas and unrelated widget contracts are preserved.

## Verification

Measured on 2026-09-08:

- Analyzer/probe regression: **37 tests**.
- Full `flutter-designer`: **2,021 tests / 242 suites**, zero failures, errors
  or skips. Includes seven new contract/generation/codec tests, three
  command/history tests, four named-parameter tests, the schema regression and
  two Canvas payload regressions.
- G4 property editor: **6 tests**.
- Real SDK: **two Java scenarios with seven generated Flutter runtime tests**,
  all passing. Ten valid counter shapes, six valid menu shapes, combined
  bindings and a user-owned local function were checked; twelve invalid counter
  and seven invalid menu shapes were rejected, including suppressed source
  diagnostics. Physical user source is unchanged during analyzer proof.
- Mutation lifecycle and exact evidence-gate regression: **one case each**,
  both passing, including independent null/reset and retained onChanged code.
- Canvas: **19 targeted tests**, full Flutter suite **1,836/1,836**;
  `flutter analyze` clean and offline Web release successful. The **40 source /
  35 Web entries** pass exact size and SHA-256 integrity checks.

- Broad selected NetBeans regression: **999 tests / 45 suites**; analyzer/probe:
  **37 tests / 2 suites**; packaged integration checks: **9 tests**. Zero
  failures, errors or skips. This selection covers property editors, Events/
  State UI, source guards, mutation/history, exact evidence gates, palette and
  Canvas artifact contracts. The G4 real-SDK scenarios passed separately above;
  earlier widget real-SDK suites retain their recorded evidence and are not
  claimed as freshly rerun. This is not the entire historical plugin suite.
- Reactor `verify`, `install` and both root/module `nbm:cluster` builds pass.
  All **1,668 non-manifest module payload entries** agree between the built JAR,
  NBM and both launch clusters. Complete `flutter-designer`, `dart-analysis`
  and `flutter-canvas-runner` dependency JAR hashes also match.

Artifact:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`

- Size: **8,227,957 bytes**.
- SHA-256: `765e45ed374cf0f3ab0973dc7929c650a25bee7cd35c2a3a3aaa123ea83b6983`.
- Web `main.dart.js`: **3,348,546 bytes**;
  SHA-256: `88b2b42e57e9e50b62205f05a7d75ceabde2d70c5dd273a2c6654595e781871a`.

The installed IDE has not been restarted, the user's Flutter project has not
been modified and no fresh manual NetBeans desktop smoke test is claimed.
