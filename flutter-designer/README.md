# flutter-designer

NetBeans-independent domain layer for the Matisse-like Flutter Designer.

The NetBeans integration binds each Designer form through mirrored project
paths:

```text
lib/<relative>/<name>.dart
.fd_templates/<relative>/<name>.fd
```

Schema v1 keeps `source.dartFile` as the exact Dart basename; the NetBeans
adapter, not this domain module, enforces the two roots and matching relative
path. Its `Flutter Designer Form` New File wizard accepts only `lib` or its
subfolders and creates both entries as one complete pair.

This module owns the immutable `.fd` document model, the built-in widget
metadata catalog, semantic validation, the bounded deterministic JSON codec,
deterministic stateless Dart-region generation, and read-only Dart
marker/class/hash plus three-way candidate verification. It also owns the pure
prospective managed-source transition planner and canonical prepared-pair
planner used before any live editor or filesystem operation, plus the bounded
pair-rename planner that canonically changes only `source.dartFile` while
preserving `source.className` and every other document field. The bounded
pair-copy planner assigns a caller-supplied fresh `documentId`, retargets only
`source.dartFile`, and proves canonical exact round-trip parity while preserving
all other semantics. The pure bounded Dart directive scanner and pair-Move
dependency planner reject path-sensitive bindings before the NetBeans adapter
may move exact pair bytes between mirrored directories. It now also owns
the bounded pure Add/Remove/Move/Wrap/Set/Reset command session, immutable
revision candidates, exact inverse history, saved cursor, branch semantics and
paired versus `.fd`-only persistence classification, plus Canvas identities,
responsive render profiles and the bounded canonical twelve-widget Canvas
model projection. It deliberately has no dependency on NetBeans APIs
or Swing.

The NetBeans adapter lives in `netbeans-plugin`. On Windows it now exposes the
Design/status surface, Explorer widget tree, exact viewport/adaptive-
target preview toolbar and a real embedded native `FlutterView`, together with the
transactional pair-save edge. Stable widget IDs synchronize selection between
the tree and Flutter surface. The standard Properties window now exposes a
bounded typed read/write slice for all twelve canonical widgets: `Scaffold`,
`ElevatedButton`, `AppBar`, `Column`, `Row`, `Padding`, `Center`, `SizedBox`,
`AspectRatio`, `Container`, `Text` and `Icon`. The exact catalog currently contains 528 writable property
rows. The historical first mutating Palette vertical slice admitted
only a terminal `Text` append. It is superseded by the current catalog-driven
192-cell candidate matrix: twelve exact capability-reviewed sources target fourteen
any-widget and two `PreferredSizeWidget` destinations, with exactly 170
accepted and 22 rejected cells, subject to
empty-single or terminal-list admission. Existing-widget
reparenting and list reordering use the same catalog compatibility planner and
transactional command path; catalog-incompatible and non-reviewed operations remain
disabled.

The Properties `Slots` tab also uses catalog-authorized atomic commands. An
occupied single slot is replaced as one fenced edit, rather than as a visible
remove-then-add sequence. Optional slots can be cleared, and list slots expose
`Clear All`; each operation validates the exact source revision and expected
ordered children and is one Undo/Redo unit.

ADR-021 explicitly forbids implementing
the Canvas as Swing-painted widgets or a transferred PNG/JPEG/raw-pixel surface.
A planned platform SPI will generalize the Windows host to Linux and macOS; the
current provider keeps the Flutter engine/view in an isolated child runner.

This module owns only the NetBeans-independent host-issued presentation
identities and stale/replay admission rules, the explicit Mobile, Tablet,
Desktop and Web preview-mode identity, native-surface request validation, pure
backend contracts and per-MultiView lifecycle controller, plus the bounded
version 1 lifecycle control codec/session gate, process framing and canonical
reviewed Canvas model payload. Current model payload version 10 carries the exact resolved
project-theme identity, seed, brightness and typed ColorScheme/TextTheme
override tables; generated Dart and Flutter Canvas apply those tables in the
same order before form-local overrides and adds Container's structured
`AlignmentGeometry`, `BoxConstraints`, `Matrix4` and image-free `BoxDecoration`
unions. Version 9 introduced the resolved project-theme tables. Version 8 retains the closed typed
`IconData` and AppBar projections, adds ElevatedButton's sparse state contract
and carries callback presence without callback identifiers.
The payload admits exactly `Scaffold`, `AppBar`, `Column`, `Row`, `Text`,
`Icon`, `Padding`, `Center`, `SizedBox`, `AspectRatio`, `Container` and
`ElevatedButton` from the reviewed
built-in catalog and excludes project paths, Dart source, callback identifiers,
extensions and persistence
authority. The frame-kind whitelist remains control JSON, model JSON and catalog
JSON; catalog JSON is reserved for a future versioned contract. There is no
image or pixel-transfer channel. This module does not own Swing, native window
handles, a platform host implementation, concrete processes, SDK discovery or
persistence.

The NetBeans edge now has the first real Windows read-only Canvas slice: a
heavyweight AWT host inside the Design MultiView, verified
runner/`FLUTTERVIEW` HWND chain, bounded SDK-keyed runner build cache and
per-view isolated process lifecycle. Each open `.fd` Design view owns its host
and process independently; cache reuse is accepted only after a bounded runtime
SHA-256 manifest matches all launch artifacts. Resize/peer-loss and late
build/launch/exit races are fenced and covered together with simultaneous-view
tests. The isolated runner decodes the canonical twelve-widget model, renders it
directly in Flutter for the compatible native adaptive targets, acknowledges the
exact layout identity and exchanges only revision-bound stable-ID selection.
Android/iOS/macOS/Linux appearance uses `ThemeData.platform` while the physical
host remains Windows. Web uses its exact browser-sized responsive viewport on
that native engine as a layout preview. It does not claim `kIsWeb`, DOM,
browser fonts, plugins or platform-channel behavior; those require an optional
future browser-compiled backend.
It receives no project paths, Dart source, file handles or
file/Save/Undo/Redo authority; Java remains the only command-admission and
persistence owner. The Java → Flutter hit-test → revision-bound DnD bridge is
not implemented.

### Current typed Properties slice

Property rows and editors are derived from the same immutable widget catalog
used for validation and Dart generation. One accepted edit emits exactly one
revision-bound `SetProperty` command; NetBeans' native **Restore Default** emits
`ResetProperty` for optional constructor arguments. The handler is one-shot and
the resulting candidate follows the existing analyzed pair-save/Undo lifecycle.
Required arguments, including `Text.data` and `Padding.padding`, cannot be
reset to omission.

| Widget | Writable properties | Flutter API |
| --- | --- | --- |
| `Column` | `mainAxisAlignment`, `mainAxisSize`, `crossAxisAlignment`, `textDirection`, `verticalDirection`, `textBaseline`, `spacing` | [Column](https://api.flutter.dev/flutter/widgets/Column/Column.html) |
| `Row` | `mainAxisAlignment`, `mainAxisSize`, `crossAxisAlignment`, `textDirection`, `verticalDirection`, `textBaseline`, `spacing` | [Row](https://api.flutter.dev/flutter/widgets/Row/Row.html) |
| `Padding` | `padding` | [Padding](https://api.flutter.dev/flutter/widgets/Padding/Padding.html) |
| `Center` | `widthFactor`, `heightFactor` | [Center](https://api.flutter.dev/flutter/widgets/Center/Center.html) |
| `SizedBox` | `width`, `height` | [SizedBox](https://api.flutter.dev/flutter/widgets/SizedBox/SizedBox.html) |
| `Text` | 59 typed leaves in the seven sets below | [Text](https://api.flutter.dev/flutter/widgets/Text/Text.html), [TextStyle](https://api.flutter.dev/flutter/painting/TextStyle/TextStyle.html), [StrutStyle](https://api.flutter.dev/flutter/painting/StrutStyle/StrutStyle.html) |
| `Icon` | `icon`, `size`, `fill`, `weight`, `grade`, `opticalSize`, `color`, `shadows`, `semanticLabel`, `textDirection`, `applyTextScaling`, `blendMode`, `fontWeight` | [Icon](https://api.flutter.dev/flutter/widgets/Icon/Icon.html), [IconData](https://api.flutter.dev/flutter/widgets/IconData-class.html) |
| `AppBar` | 120 typed leaves in the nine sets below; `leading`, `title`, `actions`, `flexibleSpace`, `bottom` slots | [AppBar](https://api.flutter.dev/flutter/material/AppBar/AppBar.html), [AppBarTheme](https://api.flutter.dev/flutter/material/AppBarTheme-class.html) |
| `ElevatedButton` | 286 typed leaves in the state-aware sets below; optional-single `child` slot | [ElevatedButton](https://api.flutter.dev/flutter/material/ElevatedButton/ElevatedButton.html), [ButtonStyle](https://api.flutter.dev/flutter/material/ButtonStyle-class.html) |

| `ElevatedButton` Properties set | Count | Projection |
| --- | ---: | --- |
| Direct behavior and callbacks | 7 | `enabled`, strict callback IDs for press/long-press/hover/focus, `autofocus`, `clipBehavior` |
| Enabled/default style | 54 | 26 ButtonStyle leaves plus 28 effective TextStyle leaves |
| Disabled style | 54 | Sparse disabled-state overrides; omission inherits theme/default |
| Pressed style | 54 | Sparse pressed-state overrides |
| Hovered style | 54 | Sparse hovered-state overrides |
| Focused style | 54 | Sparse focused-state overrides |
| Common layout and feedback | 9 | Visual density, tap target, animation duration, feedback, alignment and splash factory |

ElevatedButton generation uses a direct sparse `ButtonStyle`, not lossy
`styleFrom` reconstruction. Scalar state resolution is deterministic; compound
leaves layer default, focused, hovered and pressed fragments independently,
while disabled remains isolated from enabled fragments. Omitted leaves fall
through to the non-null `ElevatedButtonTheme` compound as one atomic value,
then framework defaults. A locally omitted `fixedSize` axis uses Flutter's
infinity sentinel only when neither inherited source supplies a size, and a
finite effective maximum clamps that sentinel. Minimum and maximum constraints
resolve as a paired state projection whenever a local bound applies, with each
maximum axis widened to the effective minimum. The effective minimum is the
authoritative bound in that pair; otherwise both properties return `null` and
defer normally.
Text family, fallback and
package leaves layer together; each package applies to the current layered
`TextStyle`, including TextTheme, and fallback-only packages replace one prior
prefix without constructing a synthetic `.../null` family. Local Text
theme/inherit fields require one explicit
transition-safe inherit mode across reachable states. The
closed contract includes all `SystemMouseCursors`, six shape presets
(`roundedRectangle`, `roundedSuperellipse`, `stadium`, `circle`,
`beveledRectangle`, `continuousRectangle`) and four splash presets
(`InkSplash`, `InkRipple`, `InkSparkle`, `NoSplash`). `TextStyle.color` is not a
leaf because effective label color belongs to `ButtonStyle.foregroundColor`.
`ButtonStyle.iconAlignment` is not a leaf either: pinned Flutter 3.44.8 consumes
it only inside the icon/label child created by `ElevatedButton.icon`; this slice
models the ordinary arbitrary-child `ElevatedButton` constructor.
Runtime-only `key`, `focusNode`, `statesController` and layer builders are also
excluded. Callback properties admit strict Dart identifiers only. That slice
originally landed on payload v8; the current aggregate protocol is v10 and still
sends `callbackPresence` instead of an identifier, so the runner installs inert
typed closures. The optional-single required-named-nullable child slot emits
`child: null` while empty.

| `AppBar` Properties set | Count | Projection |
| --- | ---: | --- |
| Behavior | 9 | Automatic controls, notification preset, semantics, clipping and color animation |
| Layout | 7 | Centering, spacing, opacities, toolbar/leading dimensions and actions padding |
| Colors and elevation | 6 | Theme-aware colors plus resting and scrolled-under elevation |
| Shape | 10 | Closed serializable `ShapeBorder`, `BorderSide` and corner/eccentricity subset |
| Leading icon theme | 9 | Optional `IconThemeData` fields |
| Actions icon theme | 9 | Optional `IconThemeData` fields |
| Toolbar text style | 31 | Optional theme base plus complete reviewed local `TextStyle` leaves |
| Title text style | 31 | Optional theme base plus complete reviewed local `TextStyle` leaves |
| System UI overlay | 8 | Typed status/navigation bar `SystemUiOverlayStyle` fields |

| `Text` Properties set | Count | Typed leaf names |
| --- | ---: | --- |
| Text | 8 | `data`, `textAlign`, `textDirection`, `softWrap`, `overflow`, `maxLines`, `textWidthBasis`, `selectionColor` |
| Accessibility | 2 | `semanticsLabel`, `semanticsIdentifier` |
| Locale and scaling | 7 | `localeLanguageCode`, `localeScriptCode`, `localeCountryCode`, `textScalerFactor`, `textHeightApplyFirstAscent`, `textHeightApplyLastDescent`, `textHeightLeadingDistribution` |
| Text style | 25 | `styleThemeTextStyle`, `styleInherit`, `styleColor`, `styleBackgroundColor`, `styleFontSize`, `styleFontWeight`, `styleFontStyle`, `styleLetterSpacing`, `styleWordSpacing`, `styleTextBaseline`, `styleHeight`, `styleLeadingDistribution`, `styleLocaleLanguageCode`, `styleLocaleScriptCode`, `styleLocaleCountryCode`, `styleDecorationUnderline`, `styleDecorationOverline`, `styleDecorationLineThrough`, `styleDecorationStyle`, `styleDecorationThickness`, `styleDebugLabel`, `styleFontFamily`, `styleFontFamilyFallback`, `stylePackage`, `styleOverflow` |
| Paint and effects | 4 | `styleForeground`, `styleBackground`, `styleShadows`, `styleDecorationColor` |
| Advanced typography | 2 | `styleFontFeatures`, `styleFontVariations` |
| Strut style | 11 | `strutFontFamily`, `strutFontFamilyFallback`, `strutFontSize`, `strutHeight`, `strutLeadingDistribution`, `strutLeading`, `strutFontWeight`, `strutFontStyle`, `strutForceHeight`, `strutDebugLabel`, `strutPackage` |

The tables above plus `AspectRatio.aspectRatio` account for 498 catalog-backed
property rows across the ten pre-Container non-`Scaffold` widgets. `Container`
adds 13 reviewed rows, bringing the non-`Scaffold` total to 511; `Scaffold` adds
17 reviewed scalar rows, so the current exact total is 528 writable rows across
twelve canonical widgets. `AspectRatio` requires one finite positive double,
uses a creation value of `1.0`, owns one optional `child` slot and has no theme
dependency.

`Container` exposes its complete reviewed non-widget constructor surface; its
optional single `child` remains a slot at constructor position 12 and is not
counted as a property row.

| `Container` property | Flutter argument | Reviewed contract |
| --- | --- | --- |
| `alignment` | `AlignmentGeometry?` | Physical or directional coordinates; omission uses Flutter layout behavior. |
| `padding` | `EdgeInsetsGeometry?` | Physical/directional, finite and non-negative. |
| `color` | `Color?` | Literal ARGB or reviewed `ColorScheme` role; mutually exclusive with `decoration`. |
| `isAntiAlias` | `bool?` | Omission preserves Flutter's `true` constructor default. |
| `decoration` | `Decoration?` | Reviewed image-free `BoxDecoration` behind the child. |
| `foregroundDecoration` | `Decoration?` | The same reviewed `BoxDecoration` domain in front of the child. |
| `width` | `double?` | Finite and non-negative. |
| `height` | `double?` | Finite and non-negative. |
| `constraints` | `BoxConstraints?` | Non-negative minima and finite-or-infinite maxima, with each maximum at least its minimum. |
| `margin` | `EdgeInsetsGeometry?` | Physical/directional, finite and non-negative, matching Flutter's constructor assertion. |
| `transform` | `Matrix4?` | Exactly 16 finite column-major entries. |
| `transformAlignment` | `AlignmentGeometry?` | Physical or directional transform origin. |
| `clipBehavior` | `Clip` | Closed enum; non-`none` requires `decoration`. |

The structured decoration covers literal/theme color, physical or directional
borders with exact side fields, physical or directional elliptical radii,
ordered stable-ID shadows, linear/radial/sweep gradients with ordered stable-ID
color stops, tile mode and optional rotation, background blend mode and shape.
Its model rejects Flutter paint hazards: blend without a fill, radius on a
circle, unsafe non-uniform border combinations, unordered/duplicate gradient
stops, invalid sweep angles, a negative focal radius or a nonzero focal radius
without a focal alignment. The paint-rect-dependent degenerate conical case is
not guessed from abstract alignments. `DecorationImage` is deferred until one
shared typed asset model can serve every image-bearing widget; no string path, URL or raw Dart
escape hatch is accepted.

Properties provides transactional alignment presets/coordinates,
finite-or-unbounded constraint fields, a visual column-major 4×4 matrix with
identity/translate/scale/rotate helpers, and a tabbed decoration editor for fill,
border, radius, shadows and gradients. Optional editors have an explicit default
state, active table edits are included before OK, and invalid drafts cannot
publish. Top-level and nested colors share the reviewed Material theme-role
allowlist, so project light/dark/custom themes flow through generated Dart and
Canvas while explicit literals remain local overrides. Dependent transitions
use one atomic `PatchProperties` command and one Undo/Redo unit.
The native Canvas builds the real Container, keeps its IDE-owned selection/layout
frame outside the paint transform, draws distinct padding and margin guides and
retains an IDE-only selectable/drop target for an empty zero-size Container.
Editors cover single-line strings, newline-delimited font fallback lists,
accessible optional boolean checkboxes, exact constrained integer/double
controls, reviewed enums, physical/directional non-negative edge insets,
ARGB/theme-aware color editors, structured Paint/Container editors and ordered
Shadow/OpenType/decoration tables.
Cross-property validation also requires `textBaseline` when
flex alignment is `baseline`, keeps `Text.semanticsIdentifier` unique in one
designer tree, rejects mutually exclusive color/Paint pairs and rejects a font
`package` without a family or fallback. `styleFontSize` is finite and
non-negative because Flutter passes it through `TextScaler.scale`.

The `.fd` model stores those 59 values as independently editable typed leaves;
each optional leaf can also be reset to omission.
Deterministic Dart generation groups them into optional `TextStyle`,
`StrutStyle`, `Locale.fromSubtags`, `TextScaler.linear` and
`TextHeightBehavior` constructor values; the isolated native Canvas performs
the same grouping into real Flutter objects. An absent group remains an omitted
constructor argument, so Properties, generated Dart and Canvas have the same
semantics.

Schema v2 adds closed structured values for Material theme tokens, the safe
serializable `Paint` subset, ordered `Shadow` values, OpenType features and
variable-font axes. Semantic `ColorScheme` and `TextTheme` roles follow the
active light, dark or custom project theme without coupling a form to a theme
definition id; literal ARGB values remain available. Missing list properties
mean inheritance/omission, while explicitly empty lists clear an inherited
list. Schema v3 distinguishes physical from directional edge insets. Schema v4
adds one closed typed nullable `IconData` value containing only a validated
Unicode scalar and font metadata. Current schema v5 adds the four Container
structured kinds above. Arbitrary Dart expressions are not used. Schema-v1
through schema-v4 documents migrate in memory and are written as canonical v5
on their next admitted Designer edit.

`Icon` is a leaf with no slots. Its 13 constructor properties preserve Flutter
theme behavior: omitted theme-backed fields inherit from `IconTheme`, while
explicit values override them identically in generated Dart and native Canvas.
`blendMode` and `fontWeight` are direct local arguments and do not inherit from
`IconTheme`; an explicit variable-font `weight` axis overrides `fontWeight` at
render time.
The built-in `icon` editor admits only **None** or one of 8,825 bundled Material
Icons locked to Flutter 3.44.8. Schema v4 keeps `IconData` generic and typed for
future reviewed definitions, but the current built-in exposes no custom metadata
entry. Material font glyphs require
`flutter.uses-material-design: true` in the application `pubspec.yaml`.

`Scaffold` exposes only its 17 reviewed scalar constructor fields. Runtime
controllers/builders, `key`, the `BoxDecoration` graph and widget-valued fields
which do not yet have an admitted catalog slot remain intentionally excluded.

The deprecated `Text.textScaleFactor` argument and `key` are not exposed;
`textScalerFactor` targets the current `textScaler` API. Arbitrary shaders,
color filters, image filters and raw Dart escape expressions remain excluded.
Broader Palette expansion, the optional runtime-faithful browser Canvas
backend and Linux/macOS native-surface providers remain pending.

Important current document semantics:

- an omitted property or slot means "omit the Dart constructor argument";
- an empty string is a real value, not omission;
- an explicitly present empty single slot means `null` and remains distinct
  from an omitted slot;
- catalog creation defaults are applied only when creating a new widget and
  are never injected while loading a document;
- optional positional constructor arguments must be present as one contiguous
  prefix; an explicitly present `null` slot still counts as present;
- exact JSON numbers remain in the model, while catalog validation rejects
  values that cannot be emitted portably as Dart integer/double literals;
- constructor and enum symbols carry an explicit exporting library URI;
  generator-selected import aliases are not persisted;
- asset paths are relative to the Flutter project/pubspec root, never to the
  `.fd` file, and are therefore not rebased by pair Move;
- supported-version `extensions` are location-independent opaque metadata and must not
  encode `.fd`-relative semantics;
- Dart expressions are opaque source values and are never evaluated here.

The source-integrity scanner accepts one independently bounded (2 MiB by
default) strict UTF-8 on-disk Dart
snapshot, retains its exact bytes, locates the exact `imports` and `build`
payload byte ranges, normalizes only line separators/trailing LF for hashing,
and reports stable `ON_DISK_DECLARED_MATCH`, `CONFLICT`, `UNSUPPORTED` or
`UNAVAILABLE` status. The current contract binds only an unqualified,
top-level stateless designer class and direct build scope; `{}`, `()` and `[]`
must balance, generic bounds cannot impersonate the class superclass, and
string-interpolation nesting is capped before scanning can exhaust the Java
stack. Stateful, import-prefixed and locally shadowed Flutter base types stay
explicitly unsupported. A declared on-disk match is read-only evidence. The
core can additionally compare it with bounded deterministic generator output
and rescan a fully reconstructed in-memory candidate. The NetBeans edge now
binds scanner/generator probes to analyzer evidence before live apply, then
binds the exact applied editor revision and both exact baselines for Save. That
persistence evidence, internal staged-replacement transaction and shared
candidate-capacity identity remain non-authorizing. The chronological native
Undo boundary and durable Pair/Source Save re-anchoring now exist, but public
Designer mutation remains gated by visual-surface work.

Successful generation additionally publishes a source-ordered occurrence
manifest for every emitted external Dart type. Each entry identifies exact
managed-region UTF-16 text, library URI and model origin; it does not contain a
filesystem trust root or authorize analyzer/navigation work. The NetBeans
adapter maps every `package:flutter` occurrence through the exact prospective
candidate region bytes and rejects any analyzer probe list that differs from
that complete manifest. The core scanner now also publishes the exact
outside-guard `StatelessWidget` superclass byte/UTF-16 occurrence for the same
retained source identity; the NetBeans edge requires its dedicated
`package:flutter/widgets.dart` class probe. The command, generator, probe
planner and analyzer now retain one exact shared candidate-capacity identity.
Third-party contributor symbol roots are still required before contributed
widgets can participate in writable commands; the built-in catalog's
pre-writable-UI runtime/release matrix now passes.

`DartSourceTransitionPlanner` handles the expected old-to-new drift after a
validated visual-model change. It requires the old on-disk three-way proof,
cross-checks its descriptor, validates the exact live marker-bearing bytes
against that descriptor, verifies successful internally consistent generation,
updates both prospective managed hashes, reverse-splices `build` then
`imports`, and scans the complete result again. It preserves all bytes outside
the payload ranges and emits either an immutable candidate or a stable
fail-closed diagnostic. Equal generated hashes produce `NO_CHANGES` and no
candidate, preventing a view/open operation from silently canonicalizing the
source. This first prospective writable contract accepts only strict UTF-8,
LF-only source without a BOM. A transition plan is still not write permission:
it contains neither a live NetBeans document revision, analyzer evidence,
prospective canonical `.fd` bytes nor lock-time baselines.

`DesignerPairPreparationPlanner` accepts only a ready source transition and
the exact original `.fd` snapshot. It re-decodes that baseline, proves the old
descriptor and stable `documentId`, canonicalizes the prospective document,
and requires deterministic decode/encode parity. Its immutable
`PreparedDesignerPair` carries defensive copies of the exact old/new Dart and
`.fd` bytes, but deliberately contains no live editor identity, analyzer proof,
filesystem lock or save authorization. The NetBeans adapter is solely
responsible for binding those remaining facts and committing the pair.

`DesignerPairCopyPlanner` accepts the exact bounded original `.fd`, its exact
current Dart basename, a canonical target Dart basename and a distinct stable
target document id. It rejects migrated/future/invalid models and substituted
source references. Its public immutable plan validates both exact non-migrated
snapshots and guarantees that the target differs only in `documentId` and
`source.dartFile`; class/generator metadata, managed hashes, Canvas preferences,
widget tree and stable ids, extensions and every other semantic remain equal.
Canonical encode plus exact decode/encode round-trip is mandatory. Mirrored
folder resolution, exact Dart-byte copying, collision allocation, coordinator
leasing, staged publication and rollback belong to the NetBeans adapter rather
than this pure module.

`DesignerPairMoveDependencyPlanner` accepts a canonical package name, distinct
old and target paths below `lib` with the same filename, and an exact bounded
project Dart-source inventory. Its scanner recognizes `import`, `export`,
`part` and URI `part of`. It rejects outgoing relative directives from the
moved source, incoming references to the old location, references that could
bind to the destination, unsupported URI forms, occupied targets, and
case-folded/NFC-equivalent path aliases. A successful immutable plan is only a
pure dependency proof; it contains no project owner, editor state, filesystem
lock or write authority. The NetBeans adapter must bind the inventory to exact
`pubspec.yaml` and `.dart_tool/package_config.json` ownership, reject nested
packages, project-`lib` aliases, links and unsaved editors, and repeat the proof
at the mutation boundary.

At that adapter edge, a private one-shot `NodeTransfer.CLIPBOARD_CUT` paste keeps
the pair in the same Flutter project, requires both already existing mirrored
destination folders and preserves the basename plus exact Dart/`.fd` bytes.
Generic DataObject Move stays disabled. The adapter closes the clean source
editor, repeats the proof and commits on the EDT under exact NetBeans 30
MasterFS proof-file locks and child-cache mutexes for all relevant/ancestor
folders, publishes `.fd` before Dart, and holds exact target locks through
reversible `.nbmove` source retirement. Rollback restores sources before
removing owned targets and retains verified recovery targets if source
recreation cannot be proved. The adapter fails closed if the exact NetBeans 30
MasterFS admission mechanism is unavailable. This guarantee is in-process only,
without a durable crash journal; an external non-NetBeans writer remains a
residual race.

At the NetBeans edge, one `PairSaveCoordinator` owns the stable pair-aware
`SaveCookie` and acquires a mandatory lease before analysis. A unique one-shot
ticket analyzes exact candidate C while the live editor remains at predecessor
B. Its Dart path comes only from the coordinator-bound file and its trust root
only from the configured Flutter SDK. Only passing ticket evidence can enter one EDT predecessor compare-and-set,
candidate apply and separate applied-live binding; rejection/cancellation does
not change the document revision or Undo state. Pair Save then commits through
the exact two-file transaction. A failure after an observed apply restores B
under one document-atomic lease-release barrier and keeps it dirty with the
stable `SaveCookie`; it never reloads over a queued user edit.
The Source and Design MultiView elements share one stable combined Undo/Redo
identity backed by the native NetBeans editor manager. Internal apply, restore,
verification and Undo-barrier changes are coalesced at that bridge and exposed
only after the document lock is released; a callback cannot re-enter a new
Designer document transaction or invalidate completed work. Each Undo/Redo
action pins one delegate; binding is rejected and binding close waits while an
action or internal document barrier owns that history.

The command adapter first derives a prospective C2 under an identity-bound
pending lease while retaining the exact C1 cursor and redo branch. The lease
binds the exact before/after revisions, edit and catalog identity, blocks a
second command, Undo/Redo and durable Save, and can only be adopted after a
separate exact staged replacement; abort retains C1 and invalidation closes an
uncertain session. Adoption separates the monitor-only exact cursor swap from
one-shot callback publication so the owner can release document/coordinator
locks first. It performs no analyzer, editor or persistence operation.
The adapter additionally pins an exact dirty cursor in a durable lease. For
`FD_ONLY`, the coordinator locks and verifies both file baselines, writes only
`.fd`, requires the exact loaded widget-catalog identity and complete writable
validation/source/three-way facts, proves the Dart document
identity/version/bytes stayed untouched, then re-anchors the session. Failure
before a durable change remains retryable; stale or uncertain outcomes freeze
the session. The coordinator now claims that pending lease, proves the exact
staged C1/durable-B identities, analyzes C2 before applying it, and jointly
publishes the applied document, fresh staged evidence and C2 command cursor.
Rejection keeps exact C1; apply failure restores and rebinds fresh C1 evidence;
an unprovable external/user-edit race preserves content and invalidates the
session. The exact shared candidate-capacity policy now travels by identity
from command generation through the generated manifest, revision, analyzer
ticket/request and analyzer limits, so an accepted command cannot first exceed
the byte/probe budget at staging.

The command core also supports durable Source re-anchoring of saved semantic
history. It scans the exact candidate `S2`, requires both managed payloads to
remain byte-for-byte equal, and rebuilds every retained historical revision and
edit proof against `S2` while preserving stable logical revision ids and exact
candidate bytes. Equal normalized hashes are not enough. At the NetBeans edge,
an identity-bound `SourceAnchorLease` pins that operation and blocks commands,
Undo/Redo and competing saves. The saved semantic endpoint keeps native `C1`
bytes underneath the Source entry even though its command/durable baseline is
`S2`, preserving the exact `S2→C1→B→C1→S2` chronology. Controller, command and
Pair authority adopt jointly; any failed outcome after CES entry or committed
split-authority risk invalidates semantic history into sticky conflict without
discarding native Source Undo or reloading user content.

A later Pair Save keeps semantic and unmanaged Source coordinates separate:
`(C2,S2)→(C1,S2)→(C1,S0)→(B,S0)` and full Redo. Endpoint-specific proofs use
scanner-owned byte offsets to project the new durable managed payloads into each
historical unmanaged envelope before deriving its semantic candidate. Unique
physical variants are bounded together. A Source `UNCHANGED` result refreshes
only the clean CES cursor and preserves exact command/Current/edge identities;
trimming the last semantic edge still leaves its owner available for the next
exact Source re-anchor. A subsequent Source Save projects its new durable anchor
through every retained physical variant; byte accounting fails incrementally
before persistence, and a closed zero-edge owner is retired only under exact
saved-baseline evidence without discarding native Undo history.

Command admission now consumes those physical variants directly. The
coordinator captures an opaque staged command-source token for the exact
logical owner, `SavedHistoryProof`, physical cursor/live identity and epochs.
At `C1/S0`, its `PreparedDesignerPair` provides the durable managed payloads in
the exact S0 envelope; the pending command therefore derives `C3/S0` and pins
that pair identity without moving logical C1 before joint adoption. Replacement
and recovery use the common staged-proof contract, so a saved-history
predecessor never fabricates analyzer evidence. Aggregate physical byte
accounting runs before analyzer or document mutation. Adoption preserves
durable C2/S2, replaces the obsolete redo suffix with the exact
`B/S0→C1/S0→C3/S0` branch, and carries S0 into the following ordinary command.

These command and pair-save paths originally served only the bounded typed
Properties UI; at that historical stage Palette insertion/DnD was still
disconnected. That stage is superseded by the twelve-source insertion matrix
described above. Pre-persistence loss of exact
staged authority now clears only semantic Designer state while retaining live
Source content and native Undo/Redo. The assembled NetBeans 30 runtime, strict
NBM verifier and isolated install lifecycle now pass. The accepted ADR-021
Windows native read-only `FlutterView`, twelve-widget projection, responsive
profiles, stable-ID tree selection, exact twelve-item context Palette and
selected-node typed Properties are implemented. Properties expose exactly
528 catalog-backed writable fields across `Scaffold`, `ElevatedButton`,
`AppBar`, `Column`, `Row`, `Padding`, `Center`, `SizedBox`, `AspectRatio`,
`Container`, `Text` and `Icon`,
including the Scaffold, ElevatedButton, AppBar, Text and Icon projections
above.
Palette DnD is enabled for the twelve exact capability-reviewed source definitions and
sixteen catalog-authorized slots, for 192 candidate cells: 170 admitted and 22
rejected. The
optional runtime-faithful browser Canvas backend, cross-platform providers and
the broader unreviewed widget contracts remain outstanding. The native-engine
Web responsive layout preview is already available.

The `.fd` document codec accepts strict UTF-8 JSON (with an optional input BOM), rejects
duplicates and trailing content, and keeps the exact bounded input snapshot.
Current version 5 data maps directly to the domain model; versions 1 through 4
migrate in memory without an open-time write, and a completely parsed newer version
remains raw/read-only and cannot be down-saved. Canonical output is
UTF-8 without BOM, two-space/LF formatted, has one final LF, fixed core-field
order and lexically sorted dynamic keys. `$schema` is never fetched.

Catalog contributors use a reverse-DNS id, target catalog API version 4, and
own only widget type ids below `<contributorId>.`. Composition is atomic per
contributor: invalid metadata never partially enters the effective catalog.
Every effective Palette category id also has one stable category order;
conflicting contributors are rejected before type resolution. API-1 through
API-3 contributors are rejected explicitly because the exported sealed value
model changed incompatibly for directional edge insets, typed `IconData` and
the four Container structured kinds.

The accepted contract and module boundaries are documented in
`docs/FLUTTER_DESIGNER_ARCHITECTURE.md`.
