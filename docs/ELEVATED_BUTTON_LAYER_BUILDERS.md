# ElevatedButton layer builders — G3

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`material/elevated_button.dart`, `button_style.dart` and `button_style_button.dart`.
Official API references are
[ButtonLayerBuilder](https://api.flutter.dev/flutter/material/ButtonLayerBuilder.html),
[backgroundBuilder](https://api.flutter.dev/flutter/material/ButtonStyle/backgroundBuilder.html)
and [foregroundBuilder](https://api.flutter.dev/flutter/material/ButtonStyle/foregroundBuilder.html).
The pinned declaration is authoritative for this release.

## Property and callback contract

Two ordinary **BUILDER** rows are added under
**Properties → Style — layout & feedback**:

- `styleBackgroundBuilder` → `ButtonStyle.backgroundBuilder`.
- `styleForegroundBuilder` → `ButtonStyle.foregroundBuilder`.

The exact SDK signature is:

```dart
typedef ButtonLayerBuilder =
    Widget Function(BuildContext context, Set<WidgetState> states, Widget? child);
```

The returned Widget is non-null; the child argument is nullable. Each optional
SDK ButtonStyle field is itself nullable, meaning omission leaves local/theme/
framework fallback intact. Designer accepts a non-null typed project binding
when the field is set. Reset removes just that field; it does not clear the
other builder or suppress an inherited theme builder. There is no separate
explicit-null mode, because Reset supplies the admitted inheritance operation.

Local or canonical package callables, getters, static/instance members and
zero-argument factories producing a compatible builder use the existing typed
reference representation. Nullable callback references/factories, CallbackValue
shorthand, arbitrary function literals and raw Dart expressions are not admitted.
These are Properties editors, not Events Create/Rename actions or State bindings.

## Flutter behavior and clipping

Flutter supplies the current BuildContext, actual `Set<WidgetState>` and child
to each builder. The five Designer style buckets do not restrict the runtime
states passed to application code. Builders can inspect other SDK states without
this slice adding extra state-bucket editors or a statesController property.
Disabled buttons still build their visual layers; disabled press/long-press
callbacks retain the existing null behavior.

Foreground and background retain the SDK's own construction and sizing order;
Designer does not replace them with a guessed Stack. Empty Designer children
remain admitted and generated as `child: null`; application builders must accept
the nullable child contract and still return a Widget.

The unnamed ElevatedButton constructor preserves an omitted clipBehavior.
Flutter then resolves **Clip.antiAlias** if either effective local/theme builder
exists, otherwise **Clip.none**. An explicitly selected Clip value always wins.
Resetting the last local builder can still leave clipping enabled when a theme
builder is inherited. Designer does not hardcode Clip.none or force antiAlias
over an explicit value.

## Persistence, generation and shared-family safety

Both fields append to the existing ElevatedButton contract. All prior direct,
common and five-state property orders are unchanged. Typed generation emits
the exact `ButtonStyle.backgroundBuilder/foregroundBuilder` expressions together
with import aliases, symbol occurrences and static-type requirements; it does
not stringify the reference or put a builder inside a WidgetStateProperty
resolver. Existing callback Enabled behavior, sparse state styles, child IDs
and source ownership are preserved.

Applying, replacing and independently resetting either builder use the existing
analyzed model/source transaction. Save/reopen and Undo/Redo retain local,
imported, member/getter and factory identities. User-owned builder bodies are
preserved byte-for-byte. Rejected null/raw/wrong-kind edits leave the current
pair and redo history unchanged. Reference invocation constness follows the
existing model; the catalog's ElevatedButton const eligibility is not changed.

TextButton's shared common-style cloning explicitly excludes these new Elevated
entries before adding its own already supported builders. This prevents duplicate
fields or shifted constructor/property orders in TextButton, OutlinedButton,
FilledButton and IconButton. Their common count remains 12; flattened counts
remain **511 / 510 / 510 / 524**, respectively.

## Exact analyzer evidence

The existing Material-SDK ButtonLayerBuilder assignment proof gains a separate
SDK-qualified invocation-result witness requiring a **non-null Widget**. The
witness supplies an SDK BuildContext, dart:core Set of SDK WidgetState and SDK
Widget? child. It rejects dynamic-return functions that ordinary function
assignability can otherwise accept, as well as nullable/wrong/async results,
incompatible context/state arguments and a non-null-only child parameter.
Compatible broader parameter types remain valid.

This narrow witness also hardens already supported ButtonLayerBuilder references
in other button families. It does not broaden the closed type grammar or accept
arbitrary proof libraries. Exact-manifest, source-identity, strict-casts controls
and trusted-root checks remain intact, even with suppressed source diagnostics.
Proof code is analyzer-only: it never invokes application builders, getters or
factories and is never written into the physical user source.

## Isolated Canvas limitation

Canvas receives anonymous builder presence, not project identifiers/imports.
It cannot reproduce or execute a project builder. A configured layer uses an
isolated pass-through substitute with an explicit approximation notice; this
preserves the child and the SDK's effective-builder clipping path, not the
application's custom layer appearance. No application source is evaluated.

Omitted builders retain the actual isolated SDK/theme fallback. Generated Dart
keeps the exact project bindings. Custom visuals and application-specific builder
logic must be checked in the application or a Flutter widget test, not inferred
from the isolated Canvas substitute.

## Scope and inventory

G3 covers only the two missing layer fields of the already admitted **unnamed
ElevatedButton constructor**. It retains **288 properties / 11 common-style
rows / five state buckets / one optional-content child slot**. It does not claim
full ElevatedButton SDK coverage: icon constructors, whole ButtonStyle references,
focusNode/statesController, additional state-bucket editors and other unrelated
API expansion are outside this slice. No native Event or State binding is added.

Current inventory is **94 widgets / 3,992 properties / 143 callables**, including
**125 native Events, 14 builders, 2 predicates and 2 formatters**. Exactly 25
widget definitions have supported callables. Const-capable definitions remain
86; State consumers remain 129 properties across 47 widget types.

**Three callable gaps across two widgets** remain: TextField.buildCounter,
TextField.contextMenuBuilder and ListView.itemExtentBuilder. Next is **G4, the
two TextField builders**, which requires exact named-parameter metadata for
InputCounterWidgetBuilder, including required nullable maxLength. Named
parameters must not be represented as positional ones.

FD schema **16**, Catalog API **15**, Canvas model **19** and transport **1** are
unchanged. Frozen schemas and unrelated widget/decoration contracts are intact.

## Verification

Measured on 2026-09-08:

- Analyzer/probe regression: **34 tests passed**, including the narrow
  Material-SDK Widget-result witness.
- Full `flutter-designer`: **2,004 tests / 239 suites**, zero failures, errors
  or skips. Includes the seven new contract/generation/codec tests, three
  command/history tests and two Canvas payload regressions.
- G3 property editor: **5/5**; mutation lifecycle and exact evidence-gate
  regression: **1/1 each**.
- Real SDK: **one Java scenario with five generated Flutter runtime tests**,
  all passing.
- Canvas: **14 targeted tests**, full Flutter suite **1,817/1,817**;
  `flutter analyze` clean and offline Web release successful. All **40 source /
  35 Web entries** independently checked for exact size and SHA-256.

Verified SDK coverage includes eight valid single-layer reference shapes, a
two-layer batch, ten invalid background bindings, two invalid foreground
bindings and a user-owned local function. Runtime coverage exercises actual
pressed/hover/focus and auxiliary SDK states, disabled layers, nullable child,
layer bounds/composition, explicit/automatic clipping and theme inheritance.

- Broad selected NetBeans regression: **1,002 tests / 48 suites**; analyzer/probe:
  **34 tests / 2 suites**; packaged integration checks: **9 tests**. Zero
  failures, errors or skips. Selection includes G3, existing interaction/editor/
  source/history/evidence-gate regressions, State expansion and the real-SDK
  TextButton/OutlinedButton/FilledButton/IconButton generation regressions.
  All eight tests in the two shared-button SDK suites ran with explicit SDK,
  Dart executable and pub-cache settings, not as skipped optional gates.
  This is not a claim that every historical plugin test was rerun; the five
  interaction-wrapper and G1/G2 real-SDK suites retain their earlier evidence.
- Reactor `verify`, `install` and both root/module `nbm:cluster` builds pass.
  All **1,668 non-manifest module payload entries** agree between the built JAR,
  NBM and both launch clusters. Complete `flutter-designer`, `dart-analysis`
  and `flutter-canvas-runner` dependency JAR hashes also match.

Artifact:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`

- Size: **8,222,517 bytes**.
- SHA-256: `49ff8cf641984d02e0c1553cf38aab37eb325c2b6fd207eaa43f35f80db5aae4`.
- Web `main.dart.js`: **3,347,190 bytes**;
  SHA-256: `38b1106ab2490d3209870db68880856f69311200fcdcc5f0181bb39d87d8d158`.

The installed IDE was not restarted and the user's Flutter project was not
modified. No fresh manual NetBeans desktop smoke test is claimed.
