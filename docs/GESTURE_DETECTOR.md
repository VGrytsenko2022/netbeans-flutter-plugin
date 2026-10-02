# GestureDetector interaction slice

Baseline: Flutter 3.44.8, reviewed on 2026-09-08 against the pinned SDK's
`widgets/gesture_detector.dart`, gesture typedefs and
[official constructor](https://api.flutter.dev/flutter/widgets/GestureDetector/GestureDetector.html).

## Scope and usage

The **Interaction** palette contains **GestureDetector**. It exposes all 64
non-key constructor properties: 58 events plus 6 behavior/device/trackpad
properties. The optional `child` is an ordinary structural slot. `key` remains
Designer-owned, consistent with the rest of the palette. This constructor is
not const; adding an empty detector does not fabricate a runtime callback.

Select an existing widget in the tree or Canvas and use the Designer toolbar's
**Wrap with GestureDetector** to keep that widget as the detector's child.
The action requires one current writable selection with a valid wrapped placement. The command keeps
all descendant IDs and edits the existing parent slot atomically. Flutter
parent-data and slot constraints still apply. Ordinary palette Add/Drop creates
a new detector; it does not silently wrap the current selection.

Under **Events**, create a handler, reuse a compatible form method, or enter a
typed project reference. The existing source editor, navigation, handler rename,
disconnect, save and Undo/Redo workflows apply. Handler bodies remain user-owned
Dart; they are not serialized into `.fd` or executed in Designer Canvas.

## Complete constructor contract

| Surface | Supported values / behavior |
| --- | --- |
| Tap | All primary, secondary and tertiary tap callbacks, including tap move and cancellation |
| Double tap | Down, recognized, cancel |
| Long press | All 7 callbacks for each of primary, secondary and tertiary buttons |
| Drag | Down/start/update/end/cancel for both vertical and horizontal drag |
| Pan | Down/start/update/end/cancel |
| Scale | Start/update/end |
| Force press | Start/peak/update/end; delivery depends on device pressure support |
| `behavior` | Omitted, explicit null, deferToChild, opaque, translucent |
| `excludeFromSemantics` | Omitted or boolean; default false |
| `dragStartBehavior` | Omitted, down, start; default start |
| `trackpadScrollCausesScale` | Omitted or boolean; default false |
| `trackpadScrollToScaleFactor` | Finite signed Offset; omission preserves SDK default `(0, -0.005)` |
| `supportedDevices` | Omitted, explicit null, or a closed set of touch, mouse, stylus, invertedStylus, trackpad, unknown |

Every callback supports omission and explicit null. Binding a handler activates
the corresponding Flutter recognizer, subject to gesture-arena competition.
Wrapping a button does not guarantee that both the button and wrapper win a tap.
`supportedDevices` has a checkbox-set editor with separate Not set, Null and Set
modes. An empty Set accepts no devices; it is not the default/all-device mode.
Generated Dart uses a typed constant set, including the empty set.
Both boolean configuration properties also reuse the existing State consumer
editor (direct or inverted bool field, or an equality comparison with a compatible
State field). The wrapper does not invent a State field, updating handler or
controller lifecycle.

Validation mirrors the actual pinned constructor assertions: pan and scale
start/update/end callbacks cannot coexist; vertical plus horizontal drag cannot
coexist with pan or scale start/update/end callbacks. Vertical and horizontal
drag alone are valid. Down/cancel-only callbacks do not activate these constructor
assertions. Validation rejects an invalid command before inserting a handler or
changing the live pair; it never removes callbacks silently.

## Persistence, analysis and preview

- FD schema **16** adds only the typed `pointerDeviceKindSet` value. Frozen
  schemas 1–15 remain unchanged; existing version-15 State/property bindings
  migrate without losing metadata or original bytes.
- Catalog API **15**, Canvas model protocol **19**; transport protocol stays 1.
- Every generated PointerDeviceKind type occurrence is included in the analyzer
  evidence manifest and must resolve inside the trusted pinned SDK.
- Typed callback proofs use the fixed Flutter gestures library for the reviewed
  gesture typedefs, including typedefs not re-exported by widgets/material.
  This affects only proof overlays, not user imports or source bytes. Persisted
  local callback shorthand receives the same static-type proof as a typed reference.
- Canvas constructs the real GestureDetector with isolated local callbacks for
  Designer selection. It does not evaluate application handlers, imported
  project functions or State bodies. Cancelled gestures cannot replace a child's
  Designer selection. Empty wrappers remain discoverable as drop destinations.
- Native source and internal offline Web manifests must be regenerated and
  hash-checked together with the matching runner sources. The internal Web
  artifact does not enable another product Canvas provider.

## Verification

Automated coverage includes exact pinned constructor/typedef inventory, all 64
device subsets, omission/null/empty persistence, callback conflicts, wrapper
admission, stable IDs, save/reopen/Undo/Redo, property editors and handler actions.
`GestureDetectorRealSdkTest` additionally checks generated callback invocation,
real mouse/touch filtering and all 58 typed callback proofs with Dart analyzer.

Verified on 2026-09-08:

- Full `flutter-designer` suite: **1,899 tests, 221 suites**, no failures, errors
  or skips. `DartCandidateAnalyzerTest`: **22 passing tests**.
- Selected NetBeans integration/editor/State suites: **1,048 distinct test cases**
  passed across the broad run and focused rerun. The broad run initially exposed
  stale palette/artifact assertions and two test-only SDK navigation fixtures;
  after correction, the final `verify` run passed **388 tests** and **9 packaged
  NBM integration tests**, with no failures, errors or skips. This is not a claim
  that every unrelated NetBeans test was rerun.
- `GestureDetectorRealSdkTest` passed against Flutter **3.44.8**: **59 Flutter
  tests**, all **58 callback type proofs**, pointer-device SDK navigation, and
  rejection of incompatible/dynamic handlers even when Dart diagnostics are
  suppressed. Existing form/State/State-expansion/ListTile SDK suites also passed.
- Full Canvas suite: **1,680 passing tests**; `flutter analyze` clean. Source
  manifest **40/40** and internal offline Web manifest **35/35** matched exact
  file sizes and SHA-256 hashes. Web bootstrap contains one local build record.
- `mvn -q -pl netbeans-plugin -am -DskipTests install`, root `nbm:cluster`, and
  module `nbm:cluster` completed successfully. The built plugin JAR's **1,631
  payload entries** match the NBM and both development clusters. All three
  embedded dependency JARs match by SHA-256; schema 16 and the GestureDetector
  catalog/model classes are present.

Package: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
(8,146,266 bytes). SHA-256:
`6039B0B8AFA62E00702B735D76BEB61C7615AB17D67A2039D07DF725A24605EF`.

Interactive installed-IDE acceptance is separate and has not been performed:
add an empty detector, drop a child, wrap an existing child, create/edit an onTap
handler, change device filters, save/reopen, and Undo/Redo. The running IDE was
not restarted or modified during this implementation.
