# Flutter Designer Architecture

Status: **accepted foundation for 0.1.3**. Details explicitly marked as open
remain design work and are not implementation commitments.

## Target product shape

The Flutter Designer follows the NetBeans Matisse interaction model while
respecting Flutter's semantic widget layout:

- an editor with `Design` and `Source` views;
- the standard NetBeans Palette for adding widgets;
- a widget tree published through NetBeans Explorer/Nodes;
- the standard Properties window for editing typed properties;
- a central embedded native `FlutterView`, with synchronized selection,
  semantic drop targets, zoom, device bounds and layout guides;
- standard NetBeans Save, Undo/Redo, Copy/Paste and Delete actions.

Flutter layout is not an absolute-position form. A drop operation selects a
named constructor slot such as `body`, `child`, `children`, `appBar` or
`floatingActionButton`. Row and Column drops select an insertion index. Stack
may additionally expose `Positioned` semantics. The persisted model never
stores incidental canvas coordinates as Flutter layout.

## Paired files and ownership

A designer form is a mirrored project pair:

```text
lib/screens/home_page.dart            user source plus designer-managed regions
.fd_templates/screens/home_page.fd    canonical visual model, JSON
```

The `.fd` file is the source of truth for the visual subtree. The `.dart` file
is the source of truth for all code outside designer-managed regions. A Dart
file without a matching `.fd` file is an ordinary Dart file and is never
claimed by the designer. It is owned by the normal Dart `DataObject`, opens a
single `text/x-dart` editor without a Design view, and keeps the plugin's Dart
lexer, highlighting, indentation and typing support. With a configured Dart
SDK, the same editor receives analysis-server diagnostics, completion,
navigation, refactoring, Quick Fixes and formatting.

NetBeans presents the pair as one logical designer object. The current 0.1.3
Design view is read-only; in the target writable slice it will edit the `.fd`
model, while Source continues to edit the paired Dart file. Rename, Copy, Move
and Delete must operate on the pair and must not silently leave one half
behind. Opening the JSON representation directly is an explicit advanced
action, not the normal `Source` view.

The following are the first on-disk pairing invariants. They are necessary
evidence, not sufficient write authority; ADR-016 through ADR-019 ticket,
applied-live, durable-lease, staged-replacement and shared-capacity evidence
plus the remaining chronological Undo contract are additionally required:

1. The Dart file is below the Flutter project's real `lib` directory, and the
   `.fd` file is below that project's real `.fd_templates` directory.
2. Their paths relative to those two roots match after replacing the `.dart`
   extension with `.fd`, including case where the filesystem exposes
   case-sensitive names.
3. Schema-v1 `.fd` `source.dartFile` names the exact Dart basename and contains
   no path separator; the relative directory is carried by the mirrored
   physical layout rather than duplicated in the JSON model.
4. The declared Dart class and managed-region markers occur exactly once.
5. Every managed-region hash matches the current Dart payload.

If the Dart file is missing, the designer may offer an explicit regeneration
from `.fd`. It does not regenerate automatically during project scanning.

## Project-wide theme ownership

Flutter theme definitions are project resources, not form state. A project
created by the plugin owns this pair:

```text
.fd_templates/project.fdtheme    canonical versioned theme catalog, JSON
lib/theme/app_theme.dart         deterministic generated runtime API
```

The schema-v1 descriptor starts with one `light` and one `dark` definition,
uses `system` as its default application mode, names the active light and dark
definitions, and records the SHA-256 of the generated Dart artifact. Custom
definitions have a stable lower-snake-case id, display name, light/dark
brightness and an opaque exact `0xFFRRGGBB` Material seed color. At most 64 definitions
are accepted. IDs are unique; the active references must exist and match their
required brightness. The complete JSON contract is
[`project-theme-v1.schema.json`](flutter-designer/project-theme-v1.schema.json).
Schema v2 adds a required global boolean `enabled`. Schema v3 adds a required
`enabled` state to every catalog definition. Schema v4 adds typed
per-role overrides for all 46 supported nondeprecated Material `ColorScheme`
roles and all 15 Material 3 `TextTheme` roles. Each text role admits 13 optional
typed `TextStyle` fields: foreground/background color, font size, weight, style,
letter/word spacing, height, family, composable decoration, decoration color,
style and thickness. Current schema v5 adds a closed table of exactly 36 typed
component colors for Scaffold, AppBar, Icon and the five ElevatedButton state
levels. Schema v1-v3 remains readable and materializes empty role tables;
schema v4 remains readable and materializes empty components. Only an explicit
save writes canonical v5. The frozen v2/v3/v4 and current v5 contracts are
[`project-theme-v2.schema.json`](flutter-designer/project-theme-v2.schema.json),
[`project-theme-v3.schema.json`](flutter-designer/project-theme-v3.schema.json)
[`project-theme-v4.schema.json`](flutter-designer/project-theme-v4.schema.json)
and
[`project-theme-v5.schema.json`](flutter-designer/project-theme-v5.schema.json).

`AppTheme` exposes an immutable map of enabled definitions, lookup by id, the
selected light and dark `ThemeData`, and the configured `ThemeMode`. Each enabled
definition starts with `ColorScheme.fromSeed`; schema-v4 role overrides are then
applied in deterministic order through `ColorScheme.copyWith`,
`ThemeData.from` and `TextTheme.copyWith`, followed by schema-v5 component
themes. A newly created application wires
`MaterialApp.theme`, `darkTheme` and `themeMode` to this API.
Disabled definitions stay in `project.fdtheme` but are omitted from this map.
When project themes are disabled, the enabled-definition map and `resolve` API
remain available but these three generated accessors are nullable and return `null`, making
MaterialApp use Flutter defaults without rewriting developer-owned `main.dart`.
The generated file carries a do-not-edit header. Its exact hash is a write
guard: the editor never silently replaces user or tool changes made directly
to `app_theme.dart`.

No theme definition is copied into an individual `.fd` document. Its existing
optional `canvas.themeMode` is only a local preview override (`system`, `light`
or `dark`); when absent, Canvas follows the project default. The Canvas reads
the validated project descriptor and the verified generated-artifact hash,
then sends the resolved id, seed, brightness, complete ColorScheme/TextTheme/
component override tables and revision digest through model protocol v14 to the isolated
Flutter runner. It never executes project Dart. The runner applies the same
construction order as generated Dart before form-local widget overrides. Projects
without a descriptor keep the legacy Material preview; a present but invalid
descriptor, missing generated file or hash mismatch fails closed with the
concrete cause.

The NetBeans theme editor is a singleton `Themes` TopComponent in the
`commonpalette` mode beside Palette. It owns catalog CRUD, the portable enabled
switch, active light/dark references, default mode and a seed-color chooser.
Its compact `General`, `Colors`, `Typography` and `Components` tabs expose the
complete v5 role/component catalog, with typed validation and an explicit
inherit/reset path for every
optional override. The built-in light and dark definitions cannot be removed,
while custom definitions can be added, duplicated, edited and removed subject
to reference and brightness validation. One save stages and verifies the
canonical descriptor and generated Dart bytes together; a conflicting
generated file is preserved rather than overwritten. Designer widget properties
bind to stable semantic Material `ColorScheme` and `TextTheme` roles. Those
references resolve through whichever light, dark or custom definition is active
and never name a concrete theme id.

Component themes, shape/extension contracts and complex theme-level Paint,
shadow, OpenType, font-variation, shader and filter graphs remain later typed
work. Before theme-level foreground or background `Paint` is admitted, the
contract must define its precedence against a form-local shorthand color,
because Flutter `TextStyle.copyWith` otherwise prefers an inherited Paint. No
standard ThemeData role is invented for Flex, Padding, Center, Locale,
TextScaler, TextHeightBehavior or StrutStyle.

### NetBeans 30 file and editor integration

The RELEASE300 integration keeps the Matisse property that one real Dart
`DataEditorSupport`, Dart EditorKit and LSP document owns the complete designer
editing session. The paired `.dart` is the technical primary of that
`MultiDataObject`; `.fd` remains the canonical visual model. Unlike a classic
same-folder Matisse pair, the model lives under a different mirrored root.
Registering it as a secondary entry makes NetBeans omit it from the physical
Files folder because the primary lives under `lib`. Therefore `.fd` has a
separate visible, non-editing model DataObject whose Open action delegates to
the one Dart-owned designer session. No second source or model editor buffer is
created.

The pair-aware Dart loader is registered for `text/x-dart` and returns a
primary only when both exact mirrored entries exist. The
`text/x-flutter-designer` registration supplies the visible model facade. A
Dart file outside `lib` or an orphan Dart file remains on the ordinary language
path; an orphan `.fd` remains visible but reports its missing/unsafe paired
source when opened. An unmodified cached ordinary Dart object is safely
revalidated when its mirrored `.fd` model appears. The supported
creation path is `File > New File > Flutter Designer > Flutter Designer Form`.
Its target is restricted to `lib` or a descendant, and it creates both files in
one filesystem atomic action before the first `DataObject.find`. Conversion of
an already modified or open Dart document requires an explicit later workflow
and must never force invalidation.

The designer DataObject opens a dedicated MultiView with `Design` first and
`Source` second. Source is NetBeans' standard `MultiViewEditorElement` backed
by that same Dart editor support. Its guarded reader replaces only valid marker
text with equal-length spaces while retaining indentation and line separators;
the guarded writer restores the exact canonical marker spelling. A marker is
structural only when the Dart lexer identifies its leading `//` as a real line
comment in the default lexical state; identical text inside ordinary, raw or
triple-quoted strings and block comments remains user content. Malformed,
nested, unmatched or duplicate structural markers result in no guard rather
than a partially protected document. Before writing, the provider verifies
that the complete live guard set still contains the expected unique simple
sections at valid non-overlapping positions. If that topology changed, the
writer restores the last known-good marker-bearing source to disk and rejects
the save, so masked placeholders can never become a successful persistence
fallback. Runtime gates verify the real MultiView and shared Dart document,
that user edits inside a valid guard are rejected, that edits outside it save
without losing either marker, and that adding a mirrored model does not discard
an already unsaved ordinary Dart buffer.

Dart-only Save As is intentionally not exposed because it would leave the
paired `.fd` behind. Generic DataObject Copy/Move remains disabled; pair
Copy/Paste and pair Cut/Move are exposed only through the custom paired nodes
described below.

Rename is exposed from both physical nodes, and both entry points route to one
pair operation; a one-file Rename does not exist. It accepts a canonical
lower-snake-case basename for a complete, clean, writable current-version pair,
closes the clean shared Designer/Source editor, and acquires the coordinator's
exclusive path-operation lease. Under deterministic locks it stages both files
at private names, canonically rewrites and verifies the `.fd` model with only
`source.dartFile` changed, then publishes and verifies both target names. The
Dart bytes, `source.className`, document id, canvas preferences, widget tree,
extensions and managed-region hashes are preserved. A Dart class rename is a
separate analyzer-backed refactoring; file-pair Rename never guesses it from the
new basename.

Delete is likewise exposed from both physical nodes, but both entry points
route to one pair operation; a one-file Delete does not exist. The operation
reserves the clean coordinator, closes an open clean Designer/Source editor,
locks both files in deterministic order, renames both canonical paths to
reversible private tombstones, and only then removes them. A failed Rename or
any Delete failure before both names are staged performs a reverse, exact-byte
verified rollback within the same NetBeans atomic event boundary. Incomplete,
unsafe, hard-linked, read-only, conflicted or unsaved pairs are disabled. These
are in-process rollback guarantees, not durable crash-recovery journals. An explicit
Source save is routed through the one stable
`SaveCookie` owned by `PairSaveCoordinator`. With no staged pair it delegates
to the normal editor serialization lifecycle through an exact paired-baseline
transaction; while a preparation lease is active, Source-only Save is blocked.

Copy is exposed from both physical nodes through one custom NetBeans
`NodeTransfer` paste provider. It does not publish `LoaderTransfer` or the
operating-system file-list flavor, so neither the IDE's generic loader path nor
an external application can receive only one pair member. The underlying
DataObjects continue to reject generic Copy/Move; pair Cut/Move uses the
separate private provider described below.
Paste is accepted only by the initiating member's current physical parent,
which keeps the duplicate inside the same mirrored relative folder. The target
basename is allocated jointly across `lib` and `.fd_templates`; an existing
one-sided `_copy` candidate forces both outputs to `_copy_2`, and so on.

The Copy transaction reserves the clean Dart-owned coordinator without closing
an open clean shared editor, snapshots and repeatedly verifies both exact source
members, and writes the Dart snapshot byte-for-byte. The pure pair-copy planner
decodes the exact current non-migrated `.fd`, requires its exact source filename,
assigns a fresh target `documentId`, changes only `source.dartFile`, canonically
encodes the result and proves exact round-trip parity. Class/generator metadata,
managed hashes, Canvas preferences, widget tree and stable widget IDs,
extensions and every other document semantic remain unchanged. Staged outputs
are published in one owned filesystem atomic action; before commit, rollback
removes only identity- and byte-verified artifacts owned by that transaction and
re-verifies both source snapshots. Each staging write, publish rename and
rollback delete acquires the artifact `FileLock`, then revalidates its exact
parent/name/FileObject identity under that same lock; publish and delete also
revalidate the exact planned bytes. If ownership or bytes changed, recovery
reports the conflict and deliberately leaves the foreign artifact untouched.
As with Rename and Delete, this is an
in-process rollback guarantee, not a durable crash-recovery journal.

Cross-directory Copy is deliberately rejected because changing the mirrored
relative folder also changes the resolution base for relative Dart directive
URIs; the Copy contract has no accepted rebasing rule.

Cut is exposed from either physical node through a separate private
`NodeTransfer.CLIPBOARD_CUT` paste provider. It publishes no one-file loader or
operating-system file-list flavor, and a successful Paste returns the empty
transferable so the Cut is one-shot. Paste accepts only a direct, already
existing writable folder under the corresponding pair root in the same Flutter
project; the destination's mirrored counterpart folder must already exist and
be writable. The basename and both exact source byte sequences remain
unchanged. A collision on either side, unsafe/link/escape/hard-link identity,
read-only path, incomplete or non-current pair, unsaved editor, or changed
snapshot disables or rejects the operation.

Directory Move is admitted only after a bounded project Dart dependency proof.
The pure scanner inventories strict snapshots and recognizes `import`, `export`,
`part` and URI `part of`; the NetBeans adapter requires a canonical
`pubspec.yaml` package name and matching `.dart_tool/package_config.json`
self-package root, rejects nested packages and aliases of the project `lib`, and
checks modified project Dart editors before and after inventory. The proof
blocks outgoing relative directives from the moved source, incoming references
to its old path, and references that could acquire or change binding at the
destination. Unsupported URI syntax plus case-folded/NFC-equivalent path
identities are rejected conservatively. At the mutation boundary the same
proof runs under exact NetBeans 30 MasterFS data-file locks and the real
child-cache write mutexes for every proof, source/target and physical ancestor
folder. The final proof and commit share one EDT admission, which excludes
in-process MasterFS save/create/delete/rename races. An unexpected MasterFS
implementation fails closed; a writer outside NetBeans remains outside this
in-process admission.

The Move transaction acquires the exclusive clean pair path-operation lease and
closes an open clean Designer/Source editor. It publishes `.fd` before Dart so
the pair-aware loader never has to promote a transient ordinary Dart owner, and
then verifies both exact targets, retaining their locks through retirement of both original
FileObjects as reversible private `.nbmove` tombstones. The old path-bound
DataObjects, controller, coordinator and Undo/Redo owner retire; fresh target
DataObjects are then resolved. Before logical commit, rollback restores and
verifies both exact sources first and removes only still-owned exact targets. If
safe source recreation cannot be proved, the verified targets remain available
for recovery instead of risking both copies. Post-commit tombstone cleanup is
best effort; a late provider/verifier fault records recovery conflict while the
already committed one-shot Cut remains consumed. This is an in-process
guarantee, not a durable crash journal.

## Guarded Dart regions

The first accepted source-ownership strategy is Matisse-like guarded regions
inside the primary Dart file. A generated file is structured like this:

```dart
// <netbeans-flutter-designer region="imports">
import 'package:flutter/material.dart';
// </netbeans-flutter-designer>

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  // <netbeans-flutter-designer region="build">
  @override
  Widget build(BuildContext context) {
    return const Scaffold();
  }
  // </netbeans-flutter-designer>

  // User-owned fields, methods and callbacks are written outside the markers.
}
```

Marker spelling is part of the format contract. Markers are ASCII line
comments, cannot nest, and each region id occurs once. Version 1 reserves the
`imports` and `build` ids. A later schema migration may introduce additional
region ids; an older generator must reject them rather than remove them.

For every region, `.fd` stores the SHA-256 of its generated payload. The hash
is computed over UTF-8 bytes after converting CRLF and CR to LF, excluding the
marker lines, and ensuring exactly one trailing LF. Hexadecimal hashes are
stored uppercase. This normalization makes the integrity check independent of
the platform line-ending convention.

Designer-paired Dart source is normative strict UTF-8 for this check; one
leading UTF-8 BOM is accepted and retained in the exact baseline. Other
encodings may still be opened by the ordinary Source editor, but Design reports
the source shape as unsupported and never rewrites it. Hashing rejects invalid
Unicode scalar sequences and performs no Unicode normalization.

The UI should use NetBeans guarded-section support when practical, but the
hash is the security and integrity boundary. Read-only editor decoration alone
is insufficient because a file can be changed by another editor, Git, a build
tool or an external process.

### Source conflicts

The designer enters `SOURCE_CONFLICT` and performs no automatic write when:

- a marker is missing, duplicated, reordered or nested;
- a managed payload hash differs from the `.fd` hash;
- the source file or declared class no longer matches the pair;
- the installed generator cannot understand the document schema.

The conflict UI names the exact file and region. Initial resolution actions
are: open a diff, keep the Dart source and detach the designer, or explicitly
restore the generated region from `.fd`. Importing arbitrary edited Dart back
into the model is not part of the 0.1.3 foundation. No conflict dialog may use
a default action that overwrites Dart code.

Changes outside managed regions are always preserved byte-for-byte by a
designer save. The designer does not run `dart format` on the whole file,
because that would rewrite user-owned code. Generated payloads are formatted
deterministically before insertion; the user's explicit Source-format action
may still format the complete Dart document.

## `.fd` document contract

`.fd` is UTF-8 JSON and current documents conform to
[`flutter-designer/fd-v9.schema.json`](flutter-designer/fd-v9.schema.json).
The stable format name is `netbeans-flutter-designer`, and the integer
`schemaVersion` makes migrations explicit. Versions 1 through 8 remain readable
through in-memory migrations; opening alone does not rewrite the file, while
the next admitted Designer edit emits canonical version 9. A migrated v1-v5
BoxDecoration has no image. Version 7 additionally permits JSON `null` as
positive infinity for every BoxConstraints bound, including either minimum;
older finite minima and nullable maxima migrate losslessly. Version 8 adds a
closed atomic `Size` value with finite non-negative width and height. Version 9
adds a closed atomic `Offset` value with signed finite `dx` and `dy`. Frozen
v1-v8 schema resources are immutable; the v9 encoder writes the new value graph
strictly and never down-saves a newer document.

The checked-in version-1 [`home_page.fd`](flutter-designer/examples/home_page.fd)
and [`home_page.dart`](flutter-designer/examples/home_page.dart) pair remains the
executable migration contract example. The canonical all-features version-2
fixture remains a migration input covering theme tokens and the structured
property kinds introduced in v2. Codecs, hash
checks and generators use these as golden fixtures rather than maintaining an
undocumented example.

The document contains:

- a stable document UUID;
- the paired Dart filename, class kind/name and managed-region hashes;
- optional design-time canvas preferences;
- one root widget node;
- an extension namespace for data not owned by the core schema.

Every widget has a stable UUID, a catalog type id, typed properties and named
slots. A generic unlabelled `children` array is insufficient for Flutter:
`Scaffold.body`, `AppBar.title`, `Padding.child` and `Column.children` have
different constructor semantics. Each slot therefore declares whether it is
single-valued or list-valued.

Property values are typed. Version 1 distinguishes strings, booleans,
integers, doubles, enums, colors, edge insets, asset references, callbacks and
explicit Dart expressions. A widget catalog decides which value kinds and
slots are legal for a particular widget. Omission means "use the Flutter
constructor default"; an empty string is a real value and is not treated as
omission.

Version 2 adds closed theme-token, Paint, Shadow and OpenType values; version 3
distinguishes physical and directional edge insets. Version 4 adds a closed
nullable `IconData` value with a Unicode-scalar code point and validated font
metadata. An icon never uses the arbitrary-Dart-expression kind, so the model,
generator and Canvas all consume the same typed value.

Version 5 adds closed `AlignmentGeometry`, `BoxConstraints`, `Matrix4` and the
initial `BoxDecoration` values without an image. Version 6 completes that branch with
a reusable top-level `IMAGE_PROVIDER` kind and optional typed
`BoxDecoration.image`. These are structural model types rather than escaped
Dart fragments: codec, validator, generator, Properties and Canvas therefore
share the same discriminated records and exact invariants. The BoxDecoration
subset covers semantic-theme or literal colors, physical or directional borders
with complete `BorderSide` values, physical or directional elliptical radii,
ordered stable-ID shadows, linear/radial/sweep gradients with ordered color
stops and `GradientRotation`, blend mode, shape and `DecorationImage`.

`ImageProviderValue` is intentionally narrower than Flutter's open provider
hierarchy. It admits only `AssetImage` or `ExactAssetImage`, a normalized safe
relative POSIX asset name, optional Dart package name, and an exact positive
scale if and only if the exact provider is selected. Either provider may have
at most one `ResizeImage`; at least one width/height is present, every present
dimension is in `1..16384`, policy is `exact` or `fit`, and upscaling is an
explicit boolean. Network, file, memory and custom providers remain deferred.
The runner's private `MemoryImage` is a projection of host-resolved immutable
bytes and is not a persisted provider alternative.

`DecorationImageValue` pins all 13 Flutter 3.44.8 named arguments in SDK order:
required image, optional typed `onError`, optional color filter, optional fit,
alignment, optional center slice, repeat, match-text-direction, scale, opacity,
filter quality, invert-colors and anti-alias. Defaults are center alignment,
`noRepeat`, false, `1.0`, `1.0`, medium, false and false. The filter union is
exactly mode, a 20-value finite matrix, linear-to-sRGB gamma,
sRGB-to-linear gamma or any finite saturation decimal within normal codec/Dart
emission bounds; a mode color may be literal or one reviewed theme token.
Decoration scale is positive and opacity stays in `[0,1]`. Center
slice coordinates are non-negative with `left < right` and `top < bottom`.
Fit may be omitted, `fill`, `contain`, `fitWidth`, `fitHeight` or `scaleDown`;
`cover` and `none` fail closed. The callback stores only a validated
Dart identifier expected to bind to `void Function(Object, StackTrace?)`; raw
expressions are not admitted and Canvas receives only callback presence.

Version 1 represents an asset value as one deterministically escaped Dart
string literal containing its path. That path is relative to the Flutter
project/pubspec root, never to the physical `.fd` file, so moving a pair between
mirrored directories does not rebase it. The value does not implicitly wrap the
path in `AssetImage`, `Image.asset` or another Flutter object. An opaque Dart
expression remains an exact, loadable model value and is never parsed,
evaluated or prefix-rewritten. The current `fd-dart-regions-v1` generator fails
closed when such a value is present: copying arbitrary expression text into a
managed region is postponed until the analyzer can validate the same bounded
document revision.

Unknown top-level fields are rejected. Vendor or experimental data belongs in
the namespaced `extensions` object. Schema-v1 extension values are
location-independent opaque metadata and must not encode `.fd`-relative
semantics; pair Move preserves their exact bytes rather than interpreting or
rewriting them. A document with a newer unsupported schema opens read-only and
is never down-saved. Migrations run in memory, retain the original bytes until
an explicit Save, and have golden before/after tests.

The codec first snapshots a bounded byte sequence, decodes strict UTF-8 (one
leading UTF-8 BOM is accepted), and performs a complete strict JSON envelope
pass before dispatching on the format and mathematical-integer schema version.
Duplicate names at any depth, comments, trailing commas, concatenated values,
unpaired Unicode surrogates and malformed UTF-8 are rejected. `$schema` is
descriptive metadata only: the plugin never resolves it as a URL or file.
Version 1 is mapped to the typed model; a fully parsed newer version retains
only its byte-identical raw snapshot and cannot enter the current-version
encoder. Invalid input also retains its exact bounded snapshot for diagnostics.

Codec limits are a separate security policy and may be stricter than JSON
Schema. The default policy bounds raw bytes, parser nesting and token counts,
field and string lengths, numeric tokens and decimal scale, widget depth/count,
per-widget properties and slots, list children, extension depth/count and
writer output. Hitting one of these limits is a stable codec diagnostic, not a
partially loaded document or an unchecked parser/model exception.

Canonical current-version output is UTF-8 without BOM, uses two-space indentation and
LF line endings with exactly one final LF. Core fields and discriminator fields
have fixed order, optional fields are emitted only when present, required empty
`properties`/`slots` objects remain explicit, dynamic object keys are sorted
lexically, and array/list order remains semantic. Exact decimal values use a
bounded canonical plain-or-scientific spelling, so large exponents cannot
expand output unexpectedly.

## Domain and module boundaries

`flutter-designer` remains NetBeans-independent and owns:

- `DesignerDocument`, widget nodes, named slots and typed property values;
- the built-in widget metadata catalog and its extension SPI;
- JSON decoding/encoding and schema migrations;
- structural and catalog validation;
- deterministic Dart-region generation;
- the pure bounded Dart directive scanner and pair-Move dependency planner;
- bounded undoable commands for add, remove, move, wrap and property changes;
- conflict detection inputs and normalized region hashing;
- pure Canvas identities, resolved render profiles, replay/admission gates,
  backend contracts, per-MultiView lifecycle controller and the bounded
  canonical protocol-v14 projection for the thirty-four exact built-ins carrying
  the Canvas capability;
- the strict version 1 Canvas control codec/handshake gate and bounded process
  framing for control JSON, model JSON, negotiated image bytes and reserved
  catalog JSON channels.

`netbeans-plugin` owns only the NetBeans edge:

- `.fd` MIME resolution, loader/DataObject and paired-file operations;
- the `Design`/`Source` MultiView elements;
- NetBeans Palette, Explorer/Nodes and Properties adapters, plus the concrete
  process, Flutter SDK and native Canvas host edge selected through the planned
  Windows/Linux/macOS platform SPI;
- the single `PairSaveCoordinator`-owned `SaveCookie`, file listeners, guarded
  sections and the shared native chronological Source/Designer `UndoRedo`
  bridge;
- wizards/actions and user-facing conflict resolution;
- background execution and EDT handoff.

Core Dart editing, project recognition, SDK discovery and Run/Debug do not
depend on `flutter-designer`. Disabling designer UI must leave ordinary Flutter
development functional.

## Typed model and widget catalog

The version 1 Java domain mirrors the `.fd` wire contract without Jackson or
NetBeans annotations. UUIDs and identifier wrappers prevent accidental mixing
of document ids, widget type ids, property names and slot names. JSON integers
and numbers use `BigInteger` and `BigDecimal`, so loading never narrows a value
or introduces `NaN`/infinity. The decoder retains widget-map insertion order
for source inspection, although order is not part of map equality; the catalog
defines presentation order and the canonical writer sorts dynamic JSON keys.
Extension values use a small immutable JSON value algebra so explicit JSON
`null`, nested arrays and objects can round-trip without leaking a
codec-specific node type.

Keeping the wire value exact does not imply that every number is safe to emit
as Dart. Catalog numeric constraints additionally require integers to remain
exact across native and JavaScript targets and decimals to round-trip through a
finite, non-underflowing Dart `double`. Edge-inset components follow the same
double rule. An exact but non-emittable value therefore remains loadable and is
reported by semantic validation instead of being silently narrowed.

The catalog is immutable and records both designer and Dart-constructor
semantics: Dart class/imports, constructor `const` capability, Palette
ordering, named versus positional parameters, accepted typed values,
constraints, named slot cardinality and widget traits. Constructor-argument
requiredness is independent of slot child count. Consequently an omitted
optional slot, a present single slot containing `null`, and a populated single
slot remain three distinct states. Present optional positional arguments must
form a contiguous prefix; an explicit-null slot is present for this rule.

Constructor and enum bindings name a public, unprefixed Dart symbol together
with a public import URI that exports it. Every referenced URI must be included
in that widget definition's immutable `importUris`. Import aliases are selected
deterministically by the generator; aliases are never persisted in catalog
metadata or `.fd`. An opaque Dart expression remains exact contributor/user
source and is not symbol-resolved or prefix-rewritten.

A catalog `creationDefault` is a Palette prototype value. It is materialized
and persisted only by an explicit create-widget command. It is never injected
while decoding or validating an existing document; absence continues to mean
"use the Flutter constructor default".

Built-in type ids use the reserved `flutter.*` namespace. Catalog composition
is deterministic and never uses last-wins replacement: invalid contributors
are rejected atomically, built-ins cannot be shadowed, and colliding extension
types are omitted with diagnostics. The NetBeans edge discovers contributors
and passes them explicitly to the domain composer, avoiding a global
class-loader policy in this module.

The current version 8 contributor SPI requires a reverse-DNS contributor id,
catalog `API_VERSION == 8`, and every contributed widget type id to start with
`<contributorId>.`. A malformed definition, duplicate local type, foreign or
reserved namespace, or unsupported API version rejects that contributor as one
atomic unit.

One effective Palette category id has exactly one `categoryOrder`. A
contributor that assigns a conflicting order—against a built-in category or
another extension—is rejected atomically before widget-type conflicts are
resolved, so one visual category can never be split into multiple groups.

Import planning belongs to the generator rather than the catalog composer. It
unions the selected definitions' declared requirements, removes the redundant
Flutter `widgets.dart` import when the known version 1 `material.dart`
requirement is also present, and sorts the resulting URIs lexically. Imports
from third-party contributors remain exact and receive URI-derived stable
prefixes: the generator does not infer an arbitrary package export graph.
Prefix generation is deterministic evidence only. Every generator-owned
Flutter type occurrence now carries its exact managed-region UTF-16 offset,
symbol name, library URI and model origin. The NetBeans probe planner maps that
1:1 ordered manifest through the prospective candidate's verified region byte
offsets and the installed evidence gate rejects caller-selected, missing,
reordered, substituted or extra probes. The source scanner also retains the
exact byte and UTF-16 occurrence of the unique unqualified top-level
`StatelessWidget` base class in the same `OriginalDartBytes` identity. The
NetBeans planner makes that `package:flutter/widgets.dart` class navigation
probe mandatory beside the generator manifest. This proof is implemented but
does not by itself enable mutation.

The implemented generator is NetBeans-independent, stateless and all-or-none.
It repeats bounded semantic validation internally, supports only the reviewed
stateless schema-version-1 binding, emits canonical LF payloads with exactly
one final LF, and publishes `imports` plus `build` together with their
normalized SHA-256 values only after the complete output fits its independent
UTF-8 budget. Constructor arguments follow catalog parameter order, list slots
retain semantic order, and omitted arguments remain omitted. Explicit empty
single slots emit `null` only where the admitted constructor contract requires
it; Transform's optional empty `child` remains omitted. Opaque Dart expressions and unsupported source kinds
produce typed model-path diagnostics and no partial payload.

Semantic validation is iterative and bounded. It reports stable issue codes
and exact model paths for duplicate widget UUIDs, unknown types/properties/
slots, missing constructor arguments, incompatible property values, slot kind
or cardinality errors, rejected child traits, and tree/resource limits.
Unknown widget nodes do not stop traversal of their descendants. Schema/codec
validation remains a preceding stage and source-pair/hash validation remains a
following context stage.

The JSON codec slice makes the schema packaged under
`flutter-designer/src/main/resources` the runtime source of truth. A build test
keeps the browsable copy under `docs/flutter-designer` byte-identical. The
bounded streaming mapper, schema-contract tests and golden documents are kept
in parity with that packaged contract; production never reads the docs tree or
resolves external schema references.

The NetBeans edge connects this codec to one document controller owned by the
paired `FlutterDesignerDataObject`. Opening `Design` starts a bounded `.fd`
read and decode on a module background processor; a monotonically increasing
generation prevents a stale completion from replacing a newer file-change
result. The controller publishes `Idle`, `Loading`, current, unsupported-newer,
invalid, oversized and infrastructure-failure states to the EDT. The current
state retains the codec's exact original-byte snapshot and separately records
catalog composition diagnostics, semantic validation and `source.dartFile`
pairing problems. A weak listener on the paired `.fd` FileObject reloads an
active view after external changes.

The controller reference-counts cloned Design elements. Closing the last clone
invalidates a pending publication and makes later file events no-ops; reopening
starts exactly one fresh snapshot. Reload requests are coalesced into one
running load and at most one dirty follow-up, so a file-event storm cannot fill
the background processor with obsolete full-document decodes. Before each
actual load, the edge discovers `WidgetCatalogContributor` implementations
through the default NetBeans Lookup and composes an immutable built-in plus
extension catalog off the EDT. A broken contributor is rejected by the domain
composer, its stable diagnostic is retained in the current UI state, and the
built-in catalog remains available.

The NetBeans module exports only `dev.flutter.netbeans.designer.catalog` and
`dev.flutter.netbeans.designer.model` for contributor modules. Such a module
depends on this plugin's NetBeans module and reuses its packaged
`flutter-designer.jar`; bundling another copy would split SPI class identity.
Codec, validation and NetBeans edge packages remain private. If the contributor
API must evolve independently after version 8, it moves to a dedicated SPI NBM
or new package boundary instead of silently breaking this module's public
surface. `API_VERSION == 2` was the incompatible boundary introduced for
direction-aware edge-insets values. Typed `IconData` established the next
explicit boundary at `API_VERSION == 3`. The initial four structured Container
values established `API_VERSION == 4`; the reusable exported
`ImageProviderValue` established `API_VERSION == 5`; finite-or-positive-infinity
BoxConstraints bounds established `API_VERSION == 6`; the atomic Size value
established `API_VERSION == 7`; and the atomic signed Offset value establishes
the current boundary at `API_VERSION == 8`. API-1 through API-7
contributors are rejected rather than loaded with a changed
sealed model contract. This is not yet a permanent 1.0 compatibility promise. A runtime
fixture NBM verifies the specification dependency, default
Lookup discovery, composed widget, shared API class identity and absence of a
second packaged `flutter-designer.jar`.

This load-only integration never opens or rewrites the Dart `StyledDocument`,
and viewing Design never dirties either file. It publishes no separate
Designer `SaveCookie`; the DataObject's sole stable `SaveCookie` is owned by
`PairSaveCoordinator` and appears only for a dirty Source document or an
internally prepared/staged pair. `Current` therefore means codec-current, not
writable. The Design view evaluates both the bounded on-disk Dart marker/class/scope/hash
gate and the deterministic generator against the same immutable catalog
snapshot. The core independently rehashes exact source slices, compares
`actual == declared == generated`, reverse-splices both generated payloads into
an in-memory copy, and sends that complete candidate through the source scanner
again. It distinguishes match, generated conflict, unsupported generation and
unavailable evidence while retaining the exact `.fd` and Dart baselines. The
status surface explicitly calls a success an *on-disk three-way match* and says
that editing remains disabled; it never presents this evidence as write-ready.
Unsupported or invalid models show a concrete file, version, diagnostic code
and model path without blocking Source in a modal dialog. The status surface,
details and loading progress expose named, synchronized Swing accessibility
descriptions for the same concrete state and cause.

## Command and Undo/Redo foundation and bounded UI routes

The pure bounded command layer implements the following immutable commands.
Public UI exposes only the separately authorized typed Properties, Palette
insertion, selected-widget Delete and same-tree existing-widget Move routes;
the remaining commands are still non-authorizing foundation.

UI components never mutate widget collections directly. Each edit is a
validated command with an inverse:

- `AddWidget`
- `RemoveWidget`
- `MoveWidget`
- `WrapWidget`
- `SetProperty`
- `ResetProperty`

`RenameDesignerClass` is deferred because the class declaration is user-owned
Dart outside the guarded regions. The implemented pair file Rename deliberately
updates only `source.dartFile` and leaves `source.className` and Dart source
untouched. Renaming the class requires a separate LSP and multi-file transaction
contract rather than pretending to be a managed-region edit.

One semantic user action produces one undoable edit. Selection changes, zoom
and temporary drag feedback are not document edits. The saved command cursor
defines dirty state; returning to it through Undo clears `Savable`.

The current foundation has a NetBeans-independent bounded command-session
cursor with exact before/after pair candidates, exact inverse Undo/Redo,
branch truncation after Undo, a saved cursor, and distinct `PAIRED`/`FD_ONLY`
persistence kinds. A stable NetBeans combined-Undo identity can select that
history when a future writable session owns it. The internal orchestrator now
derives a prospective C2 under an identity-bound pending lease while retaining
the exact C1 session, revision and redo branch. The lease binds the exact
before/after revisions, edit and catalog identity, and blocks commands,
Undo/Redo and durable Save until explicit staged adoption, abort or fail-closed
invalidation. Its adoption can swap the exact cursor under the owning
transaction and defer all listener/binding effects until document/coordinator
locks are released. The pair coordinator now claims this lease before deriving
the pure C1→C2 transition, fences every independent resolution attempt, analyzes
the exact C2 before live mutation, and jointly publishes the applied document,
fresh staged evidence and command cursor. Rejection retains exact C1. Apply
failure restores C1 and rebinds fresh evidence; an external or user edit that
makes neither C1 nor C2 provable preserves the content, clears staged authority
and invalidates the session. Command-binding effects precede pair/cookie
callbacks after all semantic locks are released. The same
orchestrator separately pins one exact dirty cursor in a durable lease and
precomputes its committed anchor. `FD_ONLY` can verify both disk baselines,
write only `.fd`, preserve the Dart document/Undo state and adopt that anchor
after a verified commit. Successful paired commits now retain the native
semantic edge. Stable logical revision ids re-anchor its immutable endpoints to
the saved graph: the saved endpoint becomes `BASELINE`, former durable
endpoints become analyzer-free `FORMER_DURABLE` proofs, and no historical
endpoint fabricates fresh analyzer authority. An ordinary Source Save above
that graph is separately pinned by an identity-bound `SourceAnchorLease` which
blocks commands, Undo/Redo and competing saves. The pure command session can
adopt a new durable Source anchor only when its exact managed payload bytes are
unchanged; normalized hash equality alone is not sufficient. It then rebuilds
every retained immutable revision and edit identity against the new durable
source while preserving stable logical revision ids and exact historical live
templates. The authorized bounded UI routes connect to these same paths; no
route receives parallel persistence or Undo authority, and this does not create
a generally writable Design surface.

Widget-subtree Copy/Paste will serialize a versioned widget fragment, allocate new stable ids and
validate the destination slot before creating an undoable command. Delete
reports why a required root or required slot cannot be removed.

## Load and save pipeline

Load runs off the Event Dispatch Thread:

1. Read bounded `.fd` and paired Dart snapshots.
2. Decode and validate the schema without executing Dart expressions.
3. Resolve widget definitions through the catalog.
4. Locate markers and verify normalized region hashes.
5. Generate both managed payloads in memory, compare all three hash facts and
   rescan the reconstructed candidate.
6. Publish an immutable read-only document snapshot to the EDT.

The implemented load-only controller completes all six steps for a matching
stateless pair. It accepts only an unqualified, top-level root class extending
`StatelessWidget`, a top-level `imports` region before that class, and a
`build` region wrapping a direct member of that exact class. The independent
2 MiB Dart-source bound accounts for the scanner's decoding and offset-index
allocations. Structural `{}`, `()` and `[]` scopes must balance, managed
regions cannot sit inside expression delimiters, generic bounds cannot stand
in for a class superclass, and string interpolation has a hard lexical nesting
limit. Stateful binding, import-prefixed widget bases and locally shadowed
Flutter base types remain explicitly unsupported until their scope/import and
symbol-resolution contracts are frozen. The immutable result is intentionally named
`ON_DISK_DECLARED_MATCH`: it proves `actual == declared`, not that the current
visual model would generate those bytes. This scanner result alone never
authorizes Designer mutation or a staged pair Save; the typed Properties
controller must additionally hold the exact current revision and enter the
one-shot command/pair-save admission path. The implemented three-way layer
proves the bounded on-disk
`actual hash == .fd declared hash == current deterministic generator hash`
relationship and validates a reconstructed candidate, but this still is not a
write authorization. ADR-016 performs the live/analyzer binding and lock-time
baseline rechecks only when invoked internally through `PairSaveCoordinator`;
the scanner and three-way result alone never authorize a write.

### Prospective transition and live evidence

The next preparation layer is implemented without changing that read-only UI
boundary:

- `DartSourceTransitionPlanner` starts from the exact old three-way proof and
  old descriptor, validates the current marker-bearing live bytes, accepts
  only successful internally consistent prospective generation, installs both
  new managed hashes and re-scans the complete reverse-spliced source. It
  preserves every byte outside the two payloads. Same hashes yield
  `NO_CHANGES` with no candidate; stale, substituted, malformed or oversized
  evidence fails closed.
- `LiveDartDocumentSnapshot` reconstructs the marker-bearing UTF-8 source from
  the equal-length marker placeholders without invoking the guarded save path
  or advancing its persistence fallback. It retains the exact
  `StyledDocument` identity, `DocumentUtilities` version, content SHA-256,
  `SimpleSection` identities and UTF-16 character ranges. The internal
  EDT-only bridge applies `build` before `imports`, verifies the complete
  expected candidate, and restores `imports` then `build` if either mutation
  or verification fails. The apply primitive may be invoked only after exact
  ticket analysis passes and while `PairSaveCoordinator` holds the mandatory
  preparation lease acquired before analysis.
- `DartCandidateAnalyzer` runs a separate
  `dart language-server --protocol=analyzer` process for one exact
  path/version/SHA candidate. It installs a versioned overlay, obtains bounded
  diagnostics and optional navigation probes, then removes the overlay and
  terminates. Candidate bytes never reach the filesystem. Errors always
  reject; warning policy is explicit; timeout, cancellation,
  `CONTENT_MODIFIED`, process failure and malformed/oversized protocol data
  all fail closed. Navigation proves a unique real target below an expected
  real root and optionally its analyzer kind. The native response does not
  independently expose the requested library's import/export graph.

The prospective writable path currently accepts only strict UTF-8, LF-only,
BOM-free Dart source. The read-only scanner may continue to diagnose other
line-ending/BOM forms, but they are not silently normalized by a designer
operation.

These artifacts remain independent, and the live apply primitive still has no
Designer UI caller. The production persistence edge now adds five explicit
layers without weakening that boundary:

- `DesignerPairPreparationPlanner` proves the exact baseline `.fd`, stable
  document identity and prospective source descriptor, then canonicalizes and
  round-trips the prospective `.fd` into one immutable `PreparedDesignerPair`.
- `PairSaveEvidenceGate` issues one globally unique, one-shot overlay ticket
  which binds the prepared pair and loaded `Current` identity to exact
  candidate bytes/hash, real project/Dart paths and the complete ordered
  scanner/generator probe manifest. It consumes `PASSED` analyzer evidence
  before mutation and binds the applied live identity/version/SHA separately.
  The coordinator supplies the Dart real path only from its bound `FileObject`
  and the navigation trust root only from the configured Flutter SDK; callers
  cannot substitute either authority.
- one `PairSaveCoordinator` publishes the only `SaveCookie` for normal Source
  saves, preparation and staged pair saves. A mandatory lease is acquired
  before analysis and blocks Source-only Save throughout it. Rejected or
  cancelled evidence does not mutate the document. Passing evidence enters
  one EDT predecessor compare-and-set, exact apply and staging publication;
  later failure restores the exact pre-apply snapshot and releases the lease
  under the same document-atomic barrier. If apply/rollback was observed, the
  restored predecessor remains dirty with the stable `SaveCookie` until an
  ordinary Source Save; it is never reloaded destructively.
  Virtual serialization cannot advance the guarded persistence fallback; that
  fallback is committed only after the actual output stream succeeds.
- `DesignerCommandSessionOrchestrator` pins the exact dirty command cursor
  through durable outcome classification. Its `FD_ONLY` path uses the same
  dual-lock transaction with old/new Dart byte equality and requires exactly
  one `.fd` write before re-anchoring the session. It also binds the exact
  immutable widget-catalog identity retained by loaded `Current` and requires
  complete writable validation, source and three-way facts.
- `PairFileTransaction` takes both local locks in stable canonical-path order,
  repeats exact byte-baseline checks under the locks, and owns one stable
  NetBeans atomic-action identity for precise file-event correlation.

After exact evidence has been staged, pair Save:

1. Rechecks the loaded model identity and live document identity, version,
   guards and candidate SHA immediately before serialization.
2. Runs the standard NetBeans editor serialization lifecycle, but accepts the
   output only if its Dart bytes exactly equal the analyzed candidate.
3. Under both locks, re-reads and compares the exact Dart and `.fd` baselines.
4. Writes Dart first and canonical `.fd` second inside the owned filesystem
   atomic event group, then re-reads and byte-verifies both results.
5. On any partial-write or post-write verification failure, restores `.fd`
   then Dart, re-reads both baselines, and retains the save owner with a typed
   failure or recovery-conflict state.
6. Treats a verified transaction commit as the durable boundary and performs
   one identity-checked command/model/pair re-anchor without clearing native
   history or scheduling a replacement load. The exact `Current` is derived
   from the committed `.fd`; controller, command and Pair effects publish on
   the EDT in that order after all locks are released. A newer unmanaged Source
   entry may remain above the saved semantic cursor only when managed content
   is exact and CES reports it dirty; Undoing that entry lazily rebinds the
   saved cursor's newer document version. A saved `B→C1→C2` graph retains both
   native semantic edges: Undo visits re-anchored analyzed `C1` and former
   durable `B`, Redo returns through `C1` to the clean `C2` savepoint, and none
   of those moves rewrites the durable `C2` pair. Events from only the exact owned
   action are suppressed; another paired-file event, stale controller ticket,
   false clean marker or unverified live edit invalidates command authority and
   enters a sticky conflict without reloading user content.

### Source Save over saved semantic history

An ordinary Source Save does not flatten or discard an already saved semantic
graph. Before CES is entered, the coordinator captures the exact virtual
serialization and opens one identity-bound `SourceAnchorLease` for the saved
command `BASELINE` and prospective Source bytes. The lease retains defensive
copies of the prior/candidate Dart and `.fd` bytes, binds the stable logical
revision, blocks command, Undo/Redo and other save ownership, and supports only
explicit adoption, safe pre-CES abort or fail-closed invalidation.

The preflight scanner must prove that the `imports` and `build` payload bytes in
prospective `S2` are exactly equal to those of the saved semantic revision.
Descriptor or normalized-hash equality cannot substitute for that proof. The
coordinator also fixes the exact live document identity/evidence, controller
`Current` and adoption ticket, disk baseline, source-state/external-event
epochs, semantic edge graph and saved cursor before accepting CES output. The
output stream is bounded and byte-compared with the preflight `S2` candidate.

After the filesystem commit, `S2` becomes the durable command `BASELINE`.
Historical `B`/`C1` candidates keep their exact Dart and `.fd` bytes, but their
transition proofs and immutable revision identities are rebuilt against `S2`
using the retained historical live-source template. Stable logical revision
ids do not change. The semantic saved endpoint becomes an overlay whose
command/durable side is `S2` while the native bytes directly below the Source
edit are still `C1`. Native chronology is therefore:

```text
clean S2
  -- native Undo --> dirty C1
  -- semantic Undo --> staged B
  -- semantic Redo --> dirty C1
  -- native Redo --> clean S2
```

A newer unmanaged `S3` observed after durable output remains above this graph,
dirty and user-owned, if its managed payloads are still exact. Successful
completion is one identity-checked controller/command/Pair adoption. The
controller ticket and Source lease are resolved under the coordinator barrier;
only after the controller `Current`, command session, durable baseline, edge
graph and cursor agree do controller, command and Pair effects publish in that
order.

A subsequent Designer command and Pair Save keep semantic and Source-envelope
coordinates independent. One semantic revision can therefore occur as distinct
physical endpoints. For `C2` saved over an `S2` envelope, the complete path is
`(C2,S2) → (C1,S2) → (C1,S0) → (B,S0)` and the exact reverse. Pair re-anchoring
maps endpoint identity rather than revision id alone. For each historical
envelope it projects the new durable managed payload bytes into the old
unmanaged envelope using scanner-owned byte offsets, then derives the exact
historical semantic candidate and endpoint-specific `PreparedDesignerPair`.
A later Source Save to `S3` repeats that endpoint-specific projection and keeps
`C2/S3 → C2/S2 → C1/S2 → C1/S0 → B/S0` and the exact reverse. All unique
physical endpoint candidates share one Dart-plus-`.fd` command-history byte
budget. Accounting is incremental: an over-budget graph fails before CES, the
pair transaction and construction of the next projected endpoint.

The pre-Canvas command-admission boundary also supports a noncanonical physical
variant such as `(C1,S0)`. Before command derivation, the coordinator captures
an opaque `StagedCommandSource` token which binds the exact logical authority,
physical history cursor, endpoint-specific staged proof, live document identity
and both event epochs. Capture is non-authorizing; replacement rechecks every
identity and rejects the token after any native cursor or epoch movement. The
token's exact `PreparedDesignerPair` contains durable C2 managed payloads
projected into the `S0` unmanaged envelope. `applyFromPhysicalEndpoint` uses
that live template to derive `C3/S0`, and the pending command lease retains the
exact physical predecessor pair while the logical C1 revision id and cursor
remain unchanged until joint adoption.

Replacement is no longer limited to a freshly analyzed predecessor. Its common
staged-proof contract accepts both `AnalyzedStagedPairProof` and the analyzer-free
`SavedHistoryProof` reconstructed from immutable history. The latter never
claims fresh analyzer authority: only the new C3 candidate receives a new
ticket/result. Rejection, cancellation and pre-apply recovery restore the same
saved proof identity; stale token, canonical-pair substitution or uncertain
post-apply recovery fails closed. Before analyzer admission, the coordinator
counts the new candidate together with every retained physical Dart-plus-`.fd`
endpoint against the shared aggregate byte budget.

Successful C3 adoption keeps durable `C2/S2` untouched, truncates the obsolete
native `S2/C2` redo suffix and installs the precise branch
`B/S0 → C1/S0 → C3/S0`, with exact Undo/Redo back through its endpoint proofs.
The adopted paired revision retains the `S0` live envelope, so the following
ordinary command derives `C4/S0` rather than falling back to canonical `S2`.

If the transaction returns `UNCHANGED` after an edit was exactly reverted, the
command session, loaded `Current`, semantic revisions and edge identities remain
unchanged. Only the exact clean CES savepoint/live cursor is refreshed. Native
positions may have byte-identical content but different dirty state; replay uses
CES savepoint state rather than byte equality to preserve that chronology. If
NetBeans trims the last semantic edit, its exact command owner/cursor remains
available for the next Source re-anchor even though the edge map is empty. If
that zero-edge owner closes first, the coordinator retires it only while the
saved baseline, controller identity and disk bytes remain exact, then performs
an ordinary Source Save without clearing native Undo history. A close between
owner selection and Source-lease pinning is revalidated at the failed pin edge;
the exact retirement then re-enters the ordinary path for one CES Save.

Entering `CloneableEditorSupport.saveDocument()` is the fail-closed boundary.
Any failed result after that point invalidates the command owner, clears
the semantic graph/cursor and enters sticky conflict while preserving the
native CES history exactly; it never calls `discardAllEdits()`. If output was
committed but controller generation, owner lifetime or another identity check
prevents joint adoption, provable durable bytes are retained, semantic
authority is removed, and the same recovery-conflict rule prevents a partial
controller/command/Pair outcome.

Both MultiView elements publish one stable DataObject-owned Undo/Redo identity.
The ordinary NetBeans editor manager is its only public delegate for the whole
DataObject lifetime, preserving native savepoints, grouping and document
locking. A Designer command session acquires only an exclusive lifetime token;
it never substitutes a second history and is not registered as another history
listener. Binding close waits until an in-flight native action or internal
document deferral completes.

One successful visual command is represented at the Dart MIME boundary by a
non-merging semantic `UndoableEdit` which delegates the one closed outer
`BaseDocument` atomic edit and retains stable before/after model revision ids.
The public NetBeans 30 `UndoableEditWrapper` SPI performs this replacement only
for an EDT-local token attached by identity to the exact document; ordinary
Source edits pass through unchanged. Failed apply/finalization calls
`AtomicLockDocument.atomicUndo()` inside the same outer atomic section and
verifies the exact predecessor, producing no native forward/rollback entry.
Semantic pair/model callbacks and the one coalesced presentation edge drain
only after the document/native/coordinator barriers are released. Every queued
callback is attempted independently, and listener failure cannot retain
publication ownership or change an already committed semantic result.

This is deliberately a bounded writable Designer surface—typed Properties,
reviewed Palette insertion, selected-widget Delete and same-tree widget Move—
not a generally writable Design surface. Scanner/generator
probes, the B→C analyze-before-apply transition, claimed staged C1→C2
replacement with exact rollback/fresh rebind across chained commands,
endpoint-specific command admission from retained physical history, durable
re-anchoring, exact `FD_ONLY` persistence, native semantic replay, saved-pair
savepoint re-anchoring, Source Save over saved semantic history, and one
identity-bound command/generator/analyzer capacity policy are implemented.
Pre-persistence loss of staged authority now clears only the exact semantic
  graph and durable command lease while retaining native Source content and
  Undo/Redo in sticky conflict. The assembled Apache NetBeans IDE 31 runtime, strict NBM
  verifier and isolated install lifecycle now pass. ADR-021 freezes the Canvas
  target, and the first Windows native host slice is implemented. It builds
  a versioned isolated runner, creates the real Flutter child window inside a
  heavyweight AWT host in the Design MultiView, validates the exact HWND/PID
  hierarchy and publishes one bounded validated protocol-v14 revision restricted
  by the exact capability gate to `Scaffold`, `AppBar`, `ElevatedButton`,
  `TextField`, `Column`, `Row`, `Wrap`, `Padding`, `Center`, `Align`,
  `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `SizedBox`,
  `AspectRatio`, `Stack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
  `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `ListView`, `Container`, `Opacity`, `Text`,
  `Icon` and `Image`. The owning
  project's observable platform snapshot limits the toolbar to exact
  mode/target pairs: Android Phone/Tablet, iPhone/iPad, named Windows/macOS/Linux
  Desktop targets, and Web. Canonical platform-folder changes reconcile every
  open Design view on the EDT, retaining the exact target, then its responsive
  mode, or choosing the first available fallback; an empty snapshot disables
  Preview. Android/iOS/macOS/Linux targets are carried to Flutter
  `ThemeData.platform` on the bound Windows engine. Web uses an exact
  browser-sized responsive viewport on that engine as a bounded layout preview;
  it does not claim browser-runtime identity or `kIsWeb` behavior.
  Stable widget IDs synchronize revision-bound selection
  between the Flutter surface and Explorer/Nodes tree. Every open `.fd` Design MultiView
  owns its own host, lifecycle session and runner process; only the immutable
  SDK-keyed build cache is shared. Resize/peer-loss races, late
  build/launch/exit callbacks, visibility transitions and two simultaneous
  sessions have deterministic coverage. Cache reuse additionally requires a
  bounded SHA-256 manifest for the complete launchable Windows runtime. The
  platform-neutral SPI, full NetBeans focus/DPI/IME/DnD/crash acceptance,
  cross-form and Linux/macOS native drag-and-drop and the broader unreviewed Designer
  workflow/property/callback contracts remain stop-ship work. Catalog-driven
  read/write Properties are enabled only for the 674 reviewed fields of
  `Scaffold`, `ElevatedButton`, `AppBar`, `TextField`, `Column`, `Row`, `Padding`,
  `Center`, `Align`, `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`,
  `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `SizedBox`, `AspectRatio`, `Stack`,
  `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `ListView`, `Wrap`, `Text`, `Icon`, `Image`, `Container`
  and `Opacity`; 17 are
  Scaffold leaves, 286 are ElevatedButton leaves, 120 are grouped AppBar
  leaves, 59 are typed Text leaves, 13 are
  typed Icon constructor properties, one is `AspectRatio.aspectRatio` and 13
  form the complete reviewed Container surface described below; Opacity adds
  its required alpha and optional semantics flag, Align and FractionallySizedBox
  add three rows each, Stack adds four, Expanded one, Flexible two, Spacer one, Baseline two, Image 22,
  TextField 54 and
  ListView 17, Wrap nine, FittedBox three, ConstrainedBox one, UnconstrainedBox
  four, LimitedBox two, OverflowBox six, IntrinsicWidth two, Offstage one,
  SizedOverflowBox two and Transform five. Twenty-nine definitions use reviewed
  const constructors, and 657 rows belong to the thirty-three non-`Scaffold`
  definitions.

  ListView is the non-const static `ListView(children: ...)` contract. Its five
  Scrolling, four Layout, four Caching and children, two Semantics and two
  Restoration rows cover 17 optional constructor arguments; `children` is one
  ordered list slot at constructor position 17. The model uses closed presets
  for axis, six physics objects, drag start, keyboard dismissal, clipping and
  hit testing, stores cache extent as non-negative logical pixels and generates
  `ScrollCacheExtent.pixels`, and rejects a `semanticChildCount` greater than the
  current static child count. Controller-owned state, builders/delegates,
  `itemExtentBuilder`, `prototypeItem`, deprecated `cacheExtent`, `key` and raw
  Dart are excluded. Generated Dart and the runner build a real ListView and
  resolve vertical or horizontal, reverse and LTR/RTL child/drop geometry. A
  shared `LayoutBuilder`/`SizedBox` guard supplies a 120-high or 240-wide bound
  for every unbounded viewport cross axis and for an unbounded main axis only
  when `shrinkWrap` is false. The originally agreed core list—`Container`, `Row`, `Column`,
  `Text`, `Image`, Button through `ElevatedButton`, `TextField` and `ListView`—is
  complete 8/8. That statement closes the agreed core Palette, not the full
  Flutter widget catalog.

  Wrap is the first post-core slice. Its const default constructor exposes all
  nine non-`key` properties with closed axis/alignment/direction/clip enums and
  finite signed spacing values, plus one ordered any-widget `children` slot at
  constructor position 9. Generated Dart and Canvas construct the real Flutter
  Wrap. Empty instances retain a non-layout-affecting 36-pixel Designer target;
  both empty and populated instances use the complete rendered rectangle for
  terminal append because wrapped runs have no single stable terminal edge.

  FittedBox is the second post-core slice. Its const default constructor adds
  optional `fit`, physical/directional `alignment`, `clipBehavior` and one
  optional single any-widget `child`. Omission preserves `BoxFit.contain`,
  `Alignment.center` and `Clip.none`; the closed value domains contain all seven
  fits and all four clip behaviors. Generated Dart and Canvas construct a real
  Flutter FittedBox, so framework scaling, LTR/RTL directional resolution and
  overflow clipping remain exact. A childless zero-size instance retains only
  the non-layout-affecting 36-pixel Designer selection/drop target. Properties,
  generation, slot/DnD/move commands and Canvas share closed-contract tests;
  Palette identity uses reviewed light/dark 16- and 32-pixel SVG resources. The practical
  Material/Base Designer backlog was 22/92 complete after FittedBox.

  ConstrainedBox is the third post-core slice. Its non-const constructor requires
  one typed `BoxConstraints constraints` value and admits one optional single
  any-widget `child`. Finite and positive-infinity bounds cover loose, bounded,
  lower-bounded, tight and expanding axes; an infinite minimum requires an
  infinite maximum. New instances store `0..∞` on both axes. Generated Dart and
  Canvas construct a real Flutter ConstrainedBox. Empty zero-size nodes retain
  only the non-layout-affecting bounded Designer selection/drop target. The
  practical backlog was then 23/92 complete with 69 remaining, and the Layout
  category contained 15 items.

  [`UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
  is the fourth post-core slice, at Layout order 107 immediately after
  ConstrainedBox. Its pinned Flutter 3.44.8 const constructor exposes optional
  `textDirection`, `alignment`, `constrainedAxis` and `clipBehavior` plus one
  optional single any-widget `child`. New instances store no property defaults.
  Omission preserves centered alignment, no retained constrained axis and
  `Clip.none`; omitted `textDirection` uses ambient `Directionality` for
  directional alignment. Generated Dart and Canvas construct a real Flutter
  UnconstrainedBox, removing constraints from both axes or retaining exactly the
  selected horizontal or vertical axis. Empty zero-size nodes retain only the
  bounded Designer selection/drop target. At that milestone the practical
  backlog was 24/92 complete with 68 remaining, and Layout contained 16 items.

  [`LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
  is the fifth post-core slice, at Layout order 108 between UnconstrainedBox and
  Stack. Its pinned Flutter 3.44.8 const constructor exposes optional finite
  non-negative `maxWidth` and `maxHeight` plus one optional single any-widget
  `child`. New instances store no property defaults; omission canonically
  preserves each `double.infinity` default. Generated Dart and Canvas construct
  a real Flutter LimitedBox, applying a selected maximum only when the incoming
  maximum constraint on that axis is unbounded. Empty zero-size nodes retain
  only the bounded Designer selection/drop target. At that milestone the
  practical backlog was 25/92 complete with 67 remaining, and Layout contained
  17 items.

  [`OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
  is the sixth post-core slice, at Layout order 109 between LimitedBox and Stack.
  Its pinned Flutter 3.44.8 const constructor exposes optional alignment, four
  finite non-negative double constraint overrides, exact
  `OverflowBoxFit.max`/`deferToChild`, and one optional single any-widget child.
  New instances store no property defaults. Omitted bounds inherit the
  corresponding parent constraints; omitted alignment and fit preserve
  `Alignment.center` and `OverflowBoxFit.max`. Present minima may not exceed
  matching maxima, and explicit non-finite overrides fail closed. Generated
  Dart and Canvas construct a real Flutter OverflowBox, preserving constraint
  replacement, overflow, LTR/RTL directional alignment and both fit modes.
  Empty or zero-size nodes retain only the bounded Designer selection/drop
  target. At that milestone the practical backlog was 26/92 complete with 66
  remaining, and Layout contained 18 items.

  [`Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html)
  is the seventh post-core slice, at Layout order 130 immediately after
  Expanded. Its pinned Flutter 3.44.8 const constructor exposes optional
  portable non-negative integer `flex`, optional exact
  `FlexFit.loose`/`FlexFit.tight`, and one required single any-widget `child`.
  Omission preserves `flex: 1` and loose fit; zero flex is valid and remains
  inflexible. Palette, tree and Canvas creation atomically wrap an existing
  direct `Row.children` or `Column.children` child and never create an empty
  terminal prototype. Flexible and Expanded cannot wrap either wrapper type,
  because the inner ParentDataWidget would no longer be a direct Flex child.
  The occupied required child is replacement-only, cannot be cleared and is not
  an insertable destination. Generated Dart and both Canvas projections
  construct the real Flexible directly below Row or Column and keep Designer
  instrumentation inside its child. Positive loose flex may use less than its
  allocation; positive tight flex fills it. At that milestone the practical
  backlog was 27/92 complete with 65 remaining, and Layout contained 19 items.

  [`Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html) is the
  eighth post-core slice, at Layout order 140 immediately after Flexible. Its
  pinned Flutter 3.44.8 const constructor exposes one optional positive portable
  integer `flex` and no slots. Omission preserves `flex: 1`; zero, negative and
  over-limit values fail closed. Palette, tree and Canvas creation insert a
  childless Spacer only into direct `Row.children` or `Column.children`; unlike
  Expanded and Flexible, it never wraps an existing child. Expanded and Flexible
  cannot wrap Spacer because Spacer's internal Expanded parent-data path must
  remain directly below Row or Column. Generated Dart and both Canvas projections
  construct the real Spacer directly under the Flex. Canvas selection, hit
  testing and outlines use surface-overlay instrumentation rather than an outer
  render-object wrapper. At that milestone the practical backlog was 28/92
  complete with 64 remaining, and Layout contained 20 items.

  [`Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html)
  is the ninth post-core slice, at Layout order 150 immediately after Spacer.
  Its pinned Flutter 3.44.8 const constructor requires finite-double `baseline`
  and `TextBaseline.alphabetic`/`ideographic` `baselineType`, followed by one
  optional single any-widget `child`. Because Flutter supplies no constructor
  defaults, a detached Designer prototype stores the reviewed visible starting
  values `baseline: 24.0` and `baselineType: TextBaseline.alphabetic`. Generated
  Dart and both Canvas projections construct the real Baseline. Childless layout
  remains framework-owned at `constraints.smallest` (often zero), with a
  non-layout-affecting Designer selection and empty-child drop target. The
  practical backlog was then 29/92 complete with 63 remaining, and Layout
  contained 21 items. `.fd` remains v7, Catalog API remains 6, Canvas model remains
  v12, and NBFC framing plus Canvas control/wire remain version 1.

  [`IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
  is the tenth post-core slice, at Layout order 160 immediately after Baseline.
  Its pinned Flutter 3.44.8 const constructor has no writable properties and
  accepts one optional single any-widget `child`. Generated Dart and both
  Canvas projections construct the real IntrinsicHeight, preserving parent
  constraints and Flutter's speculative intrinsic-height pass. Palette and slot
  descriptions expose the framework performance warning: intrinsic measurement
  is relatively expensive and can be O(N²) in tree depth. An empty or collapsed
  node receives a bounded, non-layout-affecting Designer selection and
  empty-child drop target. The practical backlog was then 30/92 complete with
  62 remaining, and Layout contained 22 items. `.fd` remains v7, Catalog API
  remains 6, Canvas model remains v12, and NBFC framing plus Canvas control/wire
  remain version 1.

  [`IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
  is the eleventh post-core slice, at Layout order 170 immediately after
  IntrinsicHeight. Its pinned Flutter 3.44.8 const constructor exposes optional
  finite non-negative `stepWidth` and `stepHeight` doubles plus one optional
  single any-widget `child`. Null and explicit zero remain distinct model,
  history and generated-Dart values, while Flutter treats either as no snapping
  on the corresponding axis; positive values round the intrinsic child extent
  upward to the next multiple. Generated Dart and both Canvas projections
  construct the real IntrinsicWidth under parent constraints. Palette,
  property and slot descriptions expose the relatively expensive speculative
  layout pass and worst-case O(N²) tree-depth behavior. An empty or collapsed
  node receives a bounded, non-layout-affecting Designer selection and
  empty-child drop target. At that milestone the practical backlog was 31/92
  complete with 61 remaining, and Layout contained 23 items. `.fd` remains v7,
  Catalog API remains 6, Canvas model remains
  v12, and NBFC framing plus Canvas control/wire remain version 1.

  [`Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html)
  is the twelfth post-core slice, at Layout order 180 immediately after
  IntrinsicWidth. Its pinned Flutter 3.44.8 const constructor exposes optional
  boolean `offstage` with constructor default `true` plus one optional single
  any-widget `child`. Omission and explicit `true` remain distinct model,
  history and generated-Dart values even though both hide the child; explicit
  `false` restores normal participation. Generated Dart and both Canvas
  projections construct the real Offstage. While hidden, Flutter still lays
  the child out and keeps it active and focusable, including animations, but
  suppresses painting, hit testing and semantics and normally contributes no
  parent space. Palette, property and slot descriptions surface this resource
  cost and recommend subtree removal for long-term hiding. Selection and drop
  instrumentation remains outside the Offstage effect; a real zero-sized result
  receives a bounded 36x36, non-layout-affecting target. At that milestone the
  practical backlog was 32/92 complete with 60 remaining, and Layout contained
  24 items. `.fd` remained v7, Catalog API remained 6 and Canvas model remained
  v12; NBFC framing plus Canvas control/wire remained version 1.

  [`SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
  is the thirteenth post-core slice, at Layout order 190 immediately after
  Offstage. Its pinned Flutter 3.44.8 const constructor exposes required
  structured `Size size`, optional physical/directional `alignment` with
  framework default `Alignment.center`, and one optional single any-widget
  `child`. The atomic Size value contains finite non-negative width and height;
  detached prototypes start at `Size(100, 100)`. Generated Dart and both Canvas
  projections construct the real SizedOverflowBox. The parent constrains the
  requested outer size, while the child receives the original incoming
  constraints and may paint outside the box according to alignment; Flutter
  still bounds hit testing by the parent box. Only a true zero-size result gets
  the bounded 36x36 Designer selection/drop target. At that milestone the
  practical backlog was 33/92 complete with 59 remaining, and Layout contained
  25 items. `.fd` was v8, Catalog API was 7 and Canvas model was v13; NBFC
  framing plus Canvas control/wire remained version 1.

  [`Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
  is the fourteenth post-core slice, at Layout order 200 immediately after
  SizedOverflowBox in Flutter's canonical Layout catalog. Its pinned Flutter
  3.44.8 const `Transform.new` surface exposes required structured `Matrix4
  transform`, optional signed finite atomic `Offset origin`, optional
  physical/directional `alignment`, optional boolean `transformHitTests` with
  framework default `true`, optional `FilterQuality.none/low/medium/high`, and
  one optional single any-widget `child`. Detached prototypes store only an
  identity Matrix4. Generated Dart and both Canvas projections construct the
  real paint-time Transform: layout size is unchanged, origin and alignment
  compose, filtering stays optional and hit testing follows the matrix only
  when `transformHitTests` resolves to true. The bounded 36x36 Designer selection/drop target is used only
  for a true zero-size result. The named rotate/translate/scale/flip convenience
  constructors remain outside the slice. The practical backlog is now 34/92
  complete with 58 remaining, and Layout contains 26 items. `.fd` is v9,
  Catalog API is 8 and Canvas model is v14; NBFC framing plus Canvas control/wire
  remain version 1.

## Target NetBeans presentation and embedded FlutterView boundary

The designer publishes every validated widget as a
revision-bound NetBeans Node and synchronizes one stable-ID selection in both
directions between the Explorer/Nodes tree and Canvas. The selected Node is
available through the standard Explorer lookup and supplies a standard
property sheet. All thirty-four current definitions publish typed read/write catalog
properties. The active MultiView element also supplies a
context-sensitive standard NetBeans Palette containing exactly the thirty-four
definitions carrying the Create capability. Palette publication alone has no
mutation authority; the DnD capability separately admits only the
host-authoritative thirty-four-source, 1,122-candidate insertion matrix with
969 accepted and 153 rejected cells. Palette, Explorer/Nodes,
Properties and the MultiView chrome remain native NetBeans Swing surfaces.

### Writable Properties API contract (674 fields)

The writable matrix is intentionally closed, catalog-driven and excludes
unreviewed constructor graphs. Scaffold contributes exactly 17 independently
resettable scalar leaves grouped as Layout, Floating action button, Appearance,
Drawer behavior and State restoration. Its closed fields use reviewed public
static presets, literal/semantic colors, optional booleans, a non-negative
numeric value, strict callback identifiers and a bounded restoration ID. Its
existing `appBar`, `body` and `floatingActionButton` children remain named
slots; widget-valued constructor parameters, `persistentFooterDecoration`,
`bottomSheetScrimBuilder` and `key` remain outside this slice. ElevatedButton
adds 286 leaves described below. AppBar adds 120 independently resettable leaves grouped as 9
Behavior, 7 Layout, 6 Colors/elevation, 10 Shape, 9 leading-icon-theme, 9
actions-icon-theme, 31 toolbar-text-style, 31 title-text-style and 8 system-UI
overlay fields. Its optional `leading`, `title`, `actions`, `flexibleSpace` and
`bottom` children remain named slots, not executable property values. The seven
flex rows below apply independently to both
[`Column`](https://api.flutter.dev/flutter/widgets/Column/Column.html) and
[`Row`](https://api.flutter.dev/flutter/widgets/Row/Row.html), so they account
for 14 fields. All seven are optional named arguments. Restore Default omits
the argument: for a non-null Dart parameter this selects its constructor
default; for a nullable parameter it restores `null`/ambient resolution rather
than writing a literal default into `.fd`.

[`ElevatedButton`](https://api.flutter.dev/flutter/material/ElevatedButton/ElevatedButton.html)
has exactly 286 writable leaves. Seven are direct: `enabled`, strict callback
identifiers for press/long-press/hover/focus, `autofocus` and `clipBehavior`.
Five state groups—enabled/default, disabled, pressed, hovered and focused—each
contain 26 effective `ButtonStyle` leaves and 28 effective `TextStyle` leaves,
for 270 state leaves. Nine common leaves cover visual density, tap-target size,
animation duration, feedback, alignment and splash factory.

Generation constructs direct sparse `ButtonStyle` state properties. Scalar
leaves use deterministic disabled, pressed, hovered, focused and default
precedence. Compound `Size`, `BorderSide`, shape and `TextStyle` leaves layer
the default fragment, then active focused, hovered and pressed fragments per
leaf over the atomic non-null `ElevatedButtonTheme` or framework-default
compound. Disabled uses only its own fragment over the inherited compound, so
enabled values cannot leak into it. Partial axes and structured TextStyle
members retain inherited leaves, while explicit empty lists and Paint/color
replacement remain explicit. When `fixedSize` has no inherited compound, an
omitted axis is Flutter's infinity sentinel and is clamped by any finite
effective maximum on that axis. A state with any local minimum or maximum
constraint resolves both compounds through the same layers and widens maximum
per axis to minimum; without an applicable local layer both properties resolve
null and defer normally. Text family, fallback and package leaves layer
per active state. Each package applies after the current layered `TextStyle`,
including TextTheme and lower active-state leaves; it cannot cross disabled or
explicit `inherit: false` isolation. Fallback-only packages replace one prior
package prefix and never construct a `.../null` family. Local Text theme/inherit configuration requires
one consistent explicit inherit mode across reachable states to keep Flutter's
state animation valid. The closed
style values cover theme/literal colors, typography, padding, size constraints,
border side, all `SystemMouseCursors`, and six shape presets:
`roundedRectangle`, `roundedSuperellipse`, `stadium`, `circle`,
`beveledRectangle` and `continuousRectangle`. Splash factory is one of
`InkSplash`, `InkRipple`, `InkSparkle` or `NoSplash`. `TextStyle.color` is
intentionally excluded because `ButtonStyle.foregroundColor` owns effective
label color. `ButtonStyle.iconAlignment` is intentionally excluded because
pinned Flutter 3.44.8 reads it only inside the icon/label child synthesized by
`ElevatedButton.icon`; the admitted ordinary constructor carries one arbitrary
child unchanged. Runtime object references—`key`, `focusNode`, `statesController`,
`backgroundBuilder` and `foregroundBuilder`—are not serialized.

Callback values are strict Dart identifiers rather than arbitrary expressions.
Generated source may bind them, but Canvas model payload protocol v14 projects only
`callbackPresence`; the runner creates inert typed closures and cannot receive
or execute a project handler. Disabling the button emits null press callbacks;
an enabled button without a press binding gets a generator-owned empty
`onPressed` closure so its enabled state remains truthful. Its `child` is an
optional-single, required-named-but-nullable catalog slot. The empty prototype
is valid and emits `child: null`. No new value kind was required for that
milestone, so `.fd` remained schema v4; the initial Container slice raised the
aggregate document contract to schema v5, and its completed image branch raised
it to schema v6. ConstrainedBox's positive-infinity minima raised the contract
to schema v7; SizedOverflowBox's atomic Size value raises it to schema v8, and
Transform's atomic Offset value raises the current contract to schema v9.

| Widget(s) | Property | Dart type and argument contract | Documented semantics and bounds | Properties editor |
| --- | --- | --- | --- | --- |
| `Column`, `Row` | `mainAxisAlignment` | `MainAxisAlignment`, optional non-null | Omitted value is `start`; values are `start`, `end`, `center`, `spaceBetween`, `spaceAround`, `spaceEvenly`. | Catalog enum list plus Restore Default. |
| `Column`, `Row` | `mainAxisSize` | `MainAxisSize`, optional non-null | Omitted value is `max`; values are `min`, `max`. | Catalog enum list plus Restore Default. |
| `Column`, `Row` | `crossAxisAlignment` | `CrossAxisAlignment`, optional non-null | Omitted value is `center`; values are `start`, `end`, `center`, `stretch`, `baseline`. `baseline` requires an explicit `textBaseline`; for a vertical main axis it behaves like `start`. | Catalog enum list plus Restore Default; reject an unpaired `baseline` transition. |
| `Column`, `Row` | `textDirection` | `TextDirection?`, optional nullable | Omission resolves through ambient `Directionality`; values are `rtl`, `ltr`. A direction is required when no ambient value can disambiguate the relevant `start`/`end` or row ordering. | Nullable catalog enum list plus Restore Default. |
| `Column`, `Row` | `verticalDirection` | `VerticalDirection`, optional non-null | Omitted value is `down`; values are `up`, `down`. | Catalog enum list plus Restore Default. |
| `Column`, `Row` | `textBaseline` | `TextBaseline?`, optional nullable | No baseline is selected when omitted; values are `alphabetic`, `ideographic`. It must be present while `crossAxisAlignment` is `baseline`. | Nullable catalog enum list plus Restore Default; Reset is rejected while baseline alignment remains selected. |
| `Column`, `Row` | `spacing` | `double`, optional non-null | Omitted value is `0.0`; value must be at least zero. It applies only between children, becomes the minimum inter-child gap for `space*` alignments and can contribute to overflow. | Exact validated decimal field (`DOUBLE`, minimum `0`) plus Restore Default. |

The flex defaults and baseline/ambient-direction rules come from the
[`Column`](https://api.flutter.dev/flutter/widgets/Column/Column.html),
[`Row`](https://api.flutter.dev/flutter/widgets/Row/Row.html) and
[`Flex`](https://api.flutter.dev/flutter/widgets/Flex/Flex.html) constructors.
The rendering contract explicitly asserts non-negative
[`spacing`](https://api.flutter.dev/flutter/rendering/RenderFlex/RenderFlex.html),
and the [`CrossAxisAlignment`](https://api.flutter.dev/flutter/rendering/CrossAxisAlignment.html)
API defines the vertical-axis baseline behavior.

`Padding` contributes one required field, while `Center` and `SizedBox`
contribute two optional nullable fields each:

| Widget | Property | Dart type and argument contract | Documented semantics and bounds | Properties editor |
| --- | --- | --- | --- | --- |
| `Padding` | `padding` | `EdgeInsetsGeometry`, required named and non-null | There is no Flutter constructor default. Every resolved dimension must be non-negative. Schema v2 intentionally accepts physical `EdgeInsets` only, not directional insets. | Required structured editor with labelled left/top/right/bottom decimal fields and an atomic All sides action; no Restore Default. |
| `Center` | `widthFactor` | `double?`, optional nullable | When present, width is child width multiplied by the factor; value must be non-negative, including zero. When omitted, constrained width expands and unconstrained width follows the child. | Nullable exact numeric field (`INTEGER` or `DOUBLE`, minimum `0`) plus Restore Default. |
| `Center` | `heightFactor` | `double?`, optional nullable | Equivalent height rule; value must be non-negative, including zero. | Nullable exact numeric field (`INTEGER` or `DOUBLE`, minimum `0`) plus Restore Default. |
| `SizedBox` | `width` | `double?`, optional nullable | When present, requests that exact non-negative width subject to parent constraints; omission leaves width unconstrained by `SizedBox`. Zero is valid. | Nullable exact numeric field (`INTEGER` or `DOUBLE`, minimum `0`) plus Restore Default. |
| `SizedBox` | `height` | `double?`, optional nullable | Equivalent height rule; omission leaves height unconstrained by `SizedBox`, and zero is valid. | Nullable exact numeric field (`INTEGER` or `DOUBLE`, minimum `0`) plus Restore Default. |

The required [`Padding.padding`](https://api.flutter.dev/flutter/widgets/Padding/Padding.html)
argument is checked by
[`RenderPadding`](https://api.flutter.dev/flutter/rendering/RenderPadding/padding.html).
The value `16` used when the designer creates a new `Padding` is a catalog
creation default, not a Flutter constructor default. `Center` inherits the
[`Align.widthFactor`](https://api.flutter.dev/flutter/widgets/Align/widthFactor.html)
and [`Align.heightFactor`](https://api.flutter.dev/flutter/widgets/Align/heightFactor.html)
contract; omission retains the layout behavior documented by
[`Align`](https://api.flutter.dev/flutter/widgets/Align-class.html). `SizedBox`
uses the nullable dimensions documented by
[`SizedBox`](https://api.flutter.dev/flutter/widgets/SizedBox/SizedBox.html);
the Designer keeps omission distinct from an explicit zero.

The [`Text`](https://api.flutter.dev/flutter/widgets/Text/Text.html) projection
contributes 59 typed, independently editable leaves in seven standard
Properties sets. Every optional leaf supports Restore Default. Scalar leaves
stay independent; schema-v2 Paint/Shadow/OpenType values are closed typed
graphs edited atomically. Generation and Canvas projection assemble the same
Flutter composites.

| Properties set | Count | Typed leaves | Flutter target |
| --- | ---: | --- | --- |
| Text | 8 | `data`, `textAlign`, `textDirection`, `softWrap`, `overflow`, `maxLines`, `textWidthBasis`, `selectionColor` | Direct `Text` arguments. |
| Accessibility | 2 | `semanticsLabel`, `semanticsIdentifier` | Direct `Text` semantics arguments. |
| Locale and scaling | 7 | `localeLanguageCode`, `localeScriptCode`, `localeCountryCode`, `textScalerFactor`, `textHeightApplyFirstAscent`, `textHeightApplyLastDescent`, `textHeightLeadingDistribution` | `Locale.fromSubtags`, `TextScaler.linear` and `TextHeightBehavior`. |
| Text style | 25 | `styleThemeTextStyle`, `styleInherit`, `styleColor`, `styleBackgroundColor`, `styleFontSize`, `styleFontWeight`, `styleFontStyle`, `styleLetterSpacing`, `styleWordSpacing`, `styleTextBaseline`, `styleHeight`, `styleLeadingDistribution`, `styleLocaleLanguageCode`, `styleLocaleScriptCode`, `styleLocaleCountryCode`, `styleDecorationUnderline`, `styleDecorationOverline`, `styleDecorationLineThrough`, `styleDecorationStyle`, `styleDecorationThickness`, `styleDebugLabel`, `styleFontFamily`, `styleFontFamilyFallback`, `stylePackage`, `styleOverflow` | Optional semantic `TextTheme` base plus local `TextStyle.copyWith` overrides, nested locale and combined decoration. |
| Paint and effects | 4 | `styleForeground`, `styleBackground`, `styleShadows`, `styleDecorationColor` | Typed foreground/background `Paint`, ordered `Shadow` list and decoration color. |
| Advanced typography | 2 | `styleFontFeatures`, `styleFontVariations` | Ordered OpenType `FontFeature` tags and variable-font `FontVariation` axes. |
| Strut style | 11 | `strutFontFamily`, `strutFontFamilyFallback`, `strutFontSize`, `strutHeight`, `strutLeadingDistribution`, `strutLeading`, `strutFontWeight`, `strutFontStyle`, `strutForceHeight`, `strutDebugLabel`, `strutPackage` | One optional `StrutStyle`. |

The first ten leaves are direct constructor arguments:

| Property | Dart type and argument contract | Documented semantics and bounds | Properties editor |
| --- | --- | --- | --- |
| `data` | `String`, required positional and non-null for `Text(data)` | No constructor default. Empty text is a real explicit value. The nullable `Text.data` getter also serves `Text.rich`; it does not make this constructor argument optional. | Required `STRING` editor; no Restore Default. |
| `textAlign` | `TextAlign?`, optional nullable | Omission resolves through `DefaultTextStyle.textAlign`, then `start`; values are `left`, `right`, `center`, `justify`, `start`, `end`. | Nullable catalog enum list plus Restore Default. |
| `textDirection` | `TextDirection?`, optional nullable | Omission resolves through ambient `Directionality`; values are `rtl`, `ltr`. It determines bidirectional layout and the meaning of `start`/`end`. | Nullable catalog enum list plus Restore Default. |
| `softWrap` | `bool?`, optional nullable | Omission inherits `DefaultTextStyle.softWrap`, whose ordinary default is `true`; explicit `false` lays glyphs out as if horizontal space were unlimited. Unset is therefore not the same model value as `true`. | Explicit `true`/`false` uses an accessible checked/unchecked checkbox; omission is shown as `<not set>` and only Restore Default returns to it. |
| `overflow` | `TextOverflow?`, optional nullable | Omission resolves through effective `TextStyle.overflow`, then `DefaultTextStyle.overflow` (normally `clip`); values are `clip`, `fade`, `ellipsis`, `visible`. Its behavior depends on `softWrap`. | Nullable catalog enum list plus Restore Default. |
| `maxLines` | `int?`, optional nullable | A present value must be greater than zero; `1` prevents wrapping. Omission inherits `DefaultTextStyle.maxLines`, so Reset does not necessarily mean unlimited lines. Excess text is truncated according to `overflow`. | Nullable exact integer field, minimum `1` and schema portable-integer maximum, plus Restore Default. |
| `semanticsLabel` | `String?`, optional nullable | When present, replaces the actual text in this widget's semantics. Empty string remains an explicit value. | Optional `STRING` editor; reset only through Restore Default so empty text and `<not set>` can remain literal values. |
| `semanticsIdentifier` | `String?`, optional nullable | Identifies the semantics node and is documented as unique; the designer enforces uniqueness within one form. | Optional `STRING` editor plus Restore Default and duplicate-value validation. |
| `textWidthBasis` | `TextWidthBasis?`, optional nullable | Omission inherits `DefaultTextStyle.textWidthBasis` (normally `parent`); values are `parent`, `longestLine`. | Nullable catalog enum list plus Restore Default. |
| `selectionColor` | `Color?`, optional nullable | Used only inside a `SelectionContainer`. Omission uses ambient `DefaultSelectionStyle`, then its semi-transparent grey fallback. Flutter interprets `Color(0xAARRGGBB)` with alpha `00` fully transparent and `FF` fully opaque. | Literal ARGB chooser or reviewed semantic `ColorScheme` role, plus Restore Default. |

The exact inherited fallback chain is visible in
[`Text.build`](https://api.flutter.dev/flutter/widgets/Text/build.html).
[`Text.maxLines`](https://api.flutter.dev/flutter/widgets/Text/maxLines.html)
and [`RichText`](https://api.flutter.dev/flutter/widgets/RichText/RichText.html)
require a non-null line limit to be greater than zero. The semantics contracts
are documented by
[`semanticsLabel`](https://api.flutter.dev/flutter/widgets/Text/semanticsLabel.html)
and
[`semanticsIdentifier`](https://api.flutter.dev/flutter/widgets/Text/semanticsIdentifier.html).
[`selectionColor`](https://api.flutter.dev/flutter/widgets/Text/selectionColor.html)
defines its ambient fallback, while
[`Color`](https://api.flutter.dev/flutter/dart-ui/Color/Color.html) defines the
32-bit ARGB layout and alpha semantics. Six-digit RGB input must not be treated
as opaque because its omitted leading alpha byte is zero.

The [`Icon`](https://api.flutter.dev/flutter/widgets/Icon/Icon.html) projection
adds 13 typed constructor properties and no slots:

| Properties set | Typed properties | Contract |
| --- | --- | --- |
| Icon data | `icon` | Required positional, nullable typed `IconData`; never an executable Dart expression. The searchable bundled registry contains exactly 8,825 public Material Icons from Flutter 3.44.8. |
| Appearance | `size`, `color`, `shadows`, `blendMode` | Size is finite and non-negative. Color is literal ARGB or a semantic `ColorScheme` role. Unset size, color, and shadows inherit `IconTheme`; an explicit empty shadow list clears inherited shadows. `blendMode` is a closed local Flutter enum and unset uses the normal `srcOver` behavior rather than inheriting from `IconTheme`. |
| Variable font | `fill`, `weight`, `grade`, `opticalSize`, `fontWeight` | `fill` is in `[0,1]`; `weight` and `opticalSize` are greater than `0` and less than `32768`; `grade` is in `[-32768,32768)`. The four axes inherit from `IconTheme`. Local `fontWeight` is `w100` through `w900`, does not inherit from `IconTheme`, and is overridden at render time by an explicit `weight` axis. |
| Accessibility and direction | `semanticLabel`, `textDirection`, `applyTextScaling` | Optional bounded label, closed `rtl`/`ltr` direction, and optional boolean text-scaling behavior. Unset `applyTextScaling` inherits `IconTheme` and then falls back to `false`; the other two values do not come from `IconTheme`. |

The schema-v4 `IconData` kind can represent a nullable Unicode scalar from U+0000
through U+10FFFF excluding surrogates, plus safe bounded font family/package
metadata and at most 32 unique ordered fallback families. A package requires a
family; a null code point carries no metadata. That generic typed representation
is internal foundation for future reviewed definitions. The current built-in
Icon constraint, Properties editor and Canvas payload admit only **None** or an
exact glyph from the bundled Material registry; there is no custom metadata UI.
Each admitted value generates the same `IconData` and `Icon` arguments used by
the native Canvas. Material glyphs require
`flutter.uses-material-design: true` in the application `pubspec.yaml`.

[`AspectRatio`](https://api.flutter.dev/flutter/widgets/AspectRatio-class.html)
adds its complete scalar contract as one required `aspectRatio` double and an
optional single `child` slot. New instances use `1.0`; the editor and both
Java/Dart validation boundaries require a finite value greater than zero.
Restore Default is unavailable because Flutter has no constructor default.
AspectRatio reads neither Theme nor Directionality, so its value remains local
layout data. The native Canvas renders the real widget and retains an IDE-owned
outline/drop target when the child slot is empty.

[`Opacity`](https://api.flutter.dev/flutter/widgets/Opacity-class.html) is a
complete const Flutter 3.44.8 vertical slice under the canonical type
`flutter.widgets.Opacity` from `package:flutter/widgets.dart`. Its exact public
surface excludes `key` and contains, in Dart constructor order:

| `Opacity` member | Flutter argument | Reviewed contract and editor |
| --- | --- | --- |
| `opacity` | required named `double` | Finite inclusive `[0, 1]`; a new prototype stores `1.0`. It has no Flutter constructor default, so Restore Default is unavailable. |
| `alwaysIncludeSemantics` | optional named `bool` | Omission preserves Flutter's `false` default. Explicit `true`/`false` uses the checked/unchecked checkbox; Restore Default returns to `<not set>`. |
| `child` | optional named `Widget?` | One optional single any-widget slot, not a writable property row. |

Deterministic Dart emits the required alpha first, then the optional semantics
flag only when stored, and then an occupied child. Native and exact-Web Canvas
projections construct real Flutter `Opacity`, not `AnimatedOpacity` or a paint
approximation. At `opacity == 0.0` Flutter skips child paint and normally removes
child semantics, but it does not disable hit testing; setting
`alwaysIncludeSemantics` true retains those semantics. Intermediate rendered
alpha values use Flutter's normal offscreen buffer. The Designer therefore keeps
selection, hit and Palette/move overlays outside the Opacity effect, including a
selectable
drop target for an empty zero-size widget. Opacity reads neither Theme nor
Directionality. The existing double, boolean and optional-single-slot model,
JSON, Dart and Canvas encodings represent this contract exactly, so `.fd`
schema v6, contributor Catalog API 5, Canvas model protocol v11, NBFC framing v1
and Canvas control/wire v1 remain unchanged.

[`Align`](https://api.flutter.dev/flutter/widgets/Align-class.html) is a
complete const Flutter 3.44.8 vertical slice under the canonical type
`flutter.widgets.Align` from `package:flutter/widgets.dart`. Its exact public
surface excludes `key` and contains, in Dart constructor order:

| `Align` member | Flutter argument | Reviewed contract and editor |
| --- | --- | --- |
| `alignment` | optional named `AlignmentGeometry` | Omission preserves `Alignment.center`; the structured editor stores finite physical or directional coordinates without clamping extrapolation to `[-1, 1]`. |
| `widthFactor` | optional named `double?` | Omitted or finite non-negative integer/double; zero and values above one are valid. |
| `heightFactor` | optional named `double?` | Omitted or finite non-negative integer/double; zero and values above one are valid. |
| `child` | optional named `Widget?` | One optional single any-widget slot, not a writable property row. |

New prototypes store no properties, so generated Dart preserves Flutter's
center alignment and null-factor bounded-axis expansion rather than spelling an
explicit factor of one. Native and exact-Web Canvas projections construct real
Flutter `Align`/`RenderPositionedBox`. Physical coordinates ignore text
direction; directional coordinates resolve through the current LTR/RTL
`Directionality`. A factor-driven empty Align may have zero real layout size,
so Designer selection and Palette/move feedback keep a bounded IDE-only target
outside the widget without altering generated layout. Existing alignment,
numeric and optional-single-slot encodings keep every schema/protocol version
unchanged.

`FractionallySizedBox` is the analogous const fractional-layout slice. It adds
optional physical/directional `alignment`, optional finite non-negative
`widthFactor` and `heightFactor`, and one optional any-widget `child`. Omission
preserves Flutter's center alignment and null factors. Native and exact-Web
projections use the real widget under bounded/unbounded constraints and LTR/RTL;
an empty zero-size layout retains only an IDE-owned selection/drop target.

`Stack` is a const four-row layout slice: optional `alignment`,
`textDirection`, `fit` and `clipBehavior`, followed by an ordered any-widget
`children` list. Designer supports non-positioned children only. The exact clip
value is forwarded to Flutter; extrapolated non-positioned alignment does not
set RenderStack's visual-overflow flag, and descendant/paint-only overflow is
not clipped. Empty lists admit index zero and populated lists expose terminal
append.

`Expanded` and `Flexible` are const required-child slices. Expanded has one
optional non-negative integer `flex`; Flexible adds the same portable
non-negative `flex` plus optional `FlexFit.loose`/`tight`. Palette creation wraps
an existing direct `Row.children` or `Column.children` child with one atomic
`WrapWidget`, preserving the child and adding one wrapper ID. Neither creates
an empty prototype in a terminal slot or wraps Expanded/Flexible; each occupied
required child editor is replacement-only and cannot clear the child. Neither
wrapper may wrap Spacer.

`Spacer` is a const childless leaf with one optional positive portable integer
`flex`. Omission preserves `1`; zero, negative and over-limit values fail
closed. It is inserted only into direct `Row.children` or `Column.children` and
never wraps an existing child.

`Baseline` is a const single-child layout widget with required finite-double
`baseline`, required `TextBaseline.alphabetic`/`ideographic` `baselineType` and
one optional `child`. Flutter provides no defaults; the Designer prototype
stores `24.0` and `alphabetic` so an inserted child begins from a visible
baseline. Both arguments remain required typed rows, and the child remains an
ordinary optional single slot.

`Image` is a const leaf with exactly 22 rows: required `image`, then
`frameBuilder`, `loadingBuilder`, `errorBuilder`, `semanticLabel`,
`excludeFromSemantics`, `width`, `height`, `color`, `opacity`, `colorBlendMode`,
`fit`, `alignment`, `repeat`, `centerSliceLeft`, `centerSliceTop`,
`centerSliceRight`, `centerSliceBottom`, `matchTextDirection`, `gaplessPlayback`,
`isAntiAlias` and `filterQuality`. Its provider editor reuses the declared
asset-only choices from Container decoration. The four center-slice leaves are
all-or-none, strict left/right and top/bottom pairs and incompatible with
`BoxFit.cover`/`none`. Add/Replace resolves the first sorted declared asset before
stable-ID allocation; unavailable inventory emits no command.

`TextField` is the canonical const `flutter.material.TextField` leaf. Its 54
optional named leaves have no creation defaults and are projected into these
reviewed Properties groups:

| Group | Count | Exact leaves |
| --- | ---: | --- |
| Input | 14 | `keyboardType`, `textInputAction`, `textCapitalization`, `obscuringCharacter`, `obscureText`, `autocorrect`, `smartDashesType`, `smartQuotesType`, `enableSuggestions`, `maxLines`, `minLines`, `maxLength`, `maxLengthEnforcement`, `keyboardAppearance` |
| Layout | 9 | `textAlign`, `textAlignVertical`, `textDirection`, `expands`, `scrollPaddingLeft`, `scrollPaddingTop`, `scrollPaddingRight`, `scrollPaddingBottom`, `clipBehavior` |
| Behavior | 11 | `readOnly`, `autofocus`, `enabled`, `ignorePointers`, `dragStartBehavior`, `enableInteractiveSelection`, `selectAllOnFocus`, `stylusHandwritingEnabled`, `enableIMEPersonalizedLearning`, `enableInlinePrediction`, `canRequestFocus` |
| Cursor and selection | 11 | `showCursor`, `cursorWidth`, `cursorHeight`, `cursorRadiusX`, `cursorRadiusY`, `cursorOpacityAnimates`, `cursorColor`, `cursorErrorColor`, `selectionHeightStyle`, `selectionWidthStyle`, `mouseCursor` |
| Callbacks | 8 | `onChanged`, `onEditingComplete`, `onSubmitted`, `onAppPrivateCommand`, `onTap`, `onTapAlwaysCalled`, `onTapOutside`, `onTapUpOutside` |
| Restoration | 1 | `restorationId` |

Keyboard type has 16 reviewed presets, `TextAlignVertical` three and
`SystemMouseCursors` 36. Callback values are identifiers, never arbitrary Dart.
Controller, focus node, formatter, decoration/style/builder graphs, typed text,
selection and runtime controller/focus state are deliberately absent. Radius
X/Y and the four scroll-padding leaves are all-or-none and mutate atomically.
Line limits, `expands`, obscure mode, keyboard action/type, obscuring character
and max length retain the catalog's cross-property validation. Generation
synthesizes TextInputType options, TextAlignVertical, `TextField.noMaxLength`,
`Radius.elliptical`, `EdgeInsets.fromLTRB` and SystemMouseCursors.

Every generated TextField is guarded by an unconditional
`LayoutBuilder`/`SizedBox`: width is 240 only when incoming width is unbounded;
height is 120 only for `expands: true` under unbounded height. Canvas builds the
same real TextField behind pointer/focus exclusion with inert callback presence.
This makes direct and indirect flex placements safe without a TextField-specific
placement rule.

[`Container`](https://api.flutter.dev/flutter/widgets/Container/Container.html)
adds its complete reviewed non-widget constructor surface. Its optional single
`child` remains an any-widget slot at constructor position 12 and is not counted
as a property row.

| `Container` property | Flutter argument | Reviewed contract and editor |
| --- | --- | --- |
| `alignment` | `AlignmentGeometry?` | Transactional presets or finite physical/directional coordinates; omission preserves Flutter layout behavior. |
| `padding` | `EdgeInsetsGeometry?` | Physical/directional finite non-negative insets editor. |
| `color` | `Color?` | Literal ARGB or reviewed `ColorScheme` role; mutually exclusive with `decoration`. |
| `isAntiAlias` | `bool?` | Accessible optional checkbox; omission preserves Flutter's `true` constructor default. |
| `decoration` | `Decoration?` | Transactional reviewed `BoxDecoration`, including typed asset-only `DecorationImage`, behind the child. |
| `foregroundDecoration` | `Decoration?` | The same reviewed `BoxDecoration` domain in front of the child. |
| `width` | `double?` | Exact finite non-negative decimal. |
| `height` | `double?` | Exact finite non-negative decimal. |
| `constraints` | `BoxConstraints?` | Presets or exact non-negative minima and finite/unbounded maxima, each maximum at least its minimum. |
| `margin` | `EdgeInsetsGeometry?` | Physical/directional finite non-negative insets editor, matching Flutter's constructor assertion. |
| `transform` | `Matrix4?` | Visual 4×4 column-major editor with exactly 16 finite entries and identity/translation/scale/Z-rotation helpers. |
| `transformAlignment` | `AlignmentGeometry?` | Transactional presets or finite physical/directional transform-origin coordinates. |
| `clipBehavior` | `Clip` | Closed enum; any value other than `none` requires `decoration`. |

The structured decoration union covers a literal or semantic-theme fill,
physical or directional borders with complete `BorderSide` fields, physical or
directional elliptical radii, ordered stable-ID shadows, linear/radial/sweep
gradients with ordered stable-ID theme/literal color stops, every `TileMode`,
optional `GradientRotation`, background blend mode, rectangle/circle shape and
the complete typed `DecorationImage` contract described below.
It rejects Flutter layout and paint hazards at the model, codec, command,
generator and runner boundaries: non-negative sizes/insets/radii/shadow blur,
finite-or-infinite constraint ordering, blend without a fill, radius on a
circle, unsafe non-uniform border combinations, unordered or duplicate gradient
stops, invalid sweep angles, a negative focal radius or a nonzero focal radius
without a focal alignment. The paint-rect-dependent degenerate conical case is
not guessed from abstract alignments. Changes that must update
dependent fields use one revision-bound `PatchProperties` command and one
Undo/Redo unit, so an intermediate invalid graph never reaches persistence.

Top-level `color` and every nested decoration color, including
`DecorationImage.colorFilter.mode`, use the same reviewed
`material.colorScheme.*` allowlist. Project light, dark and custom themes
therefore resolve identically in generated Dart and Canvas; explicit literals
remain local overrides. The native runner builds the real `Container`, keeps
its IDE-owned layout/selection frame outside the paint transform, draws distinct
padding and margin guides, and supplies an IDE-only selectable/drop target for
an empty zero-size container without changing its Flutter layout size.

The NetBeans asset inventory reads the application `pubspec.yaml` plus package
asset declarations rooted through `.dart_tool/package_config.json`. The `.fd`
model persists normalized `assetName`, optional `packageName`, provider kind and
typed provider options; it never persists project bytes or filesystem paths.
Canvas derives the diagnostic/wire identity `app:<assetName>` or
`package:<packageName>:<assetName>` from those fields.
Resolution accepts PNG/JPEG/GIF/WebP, validates file magic and dimensions,
normalizes POSIX-relative names, and rejects absolute paths, backslashes,
percent/dot/traversal segments, non-file package roots, symlink/root escape and
unreadable or inconsistent files. Host hardening defaults are 4,096 logical
assets, 16 MiB per file, 64 MiB aggregate, dimension 4,096 and 8,388,608 pixels.
The immutable snapshot includes each base asset and declared `Nx` resolution
peer, metadata, compressed-byte SHA-256 and one inventory digest; file watching
invalidates and recomputes the project inventory without writing the form. Only
parsed package roots whose `pubspec.yaml` name matches `package_config` become
external recursive-listener boundaries. Listener replacement, rollback and
cleanup run off the EDT under a generation fence; a changed watch set forces a
fresh inventory before bytes are published, and all path events remain relevant
because Flutter permits arbitrary and extensionless declared asset names.

Variant selection reproduces Flutter 3.44.8 framework revision
`058e0af2c2b57e369d905a03ac9748b0ebf543c6`, source
`packages/flutter/lib/src/painting/image_resolution.dart`, with low-DPR limit
2.0. The inventory fingerprint names this contract exactly as
`flutter-3.44.8-058e0af2c2b57e369d905a03ac9748b0ebf543c6-asset-image-dpr-low-limit-2.0`.
An exact DPR match wins; DPR below/above the available range chooses the
lowest/highest variant. Between `lower < dpr < upper`, DPR below 2.0 chooses
upper; at or above 2.0 it chooses upper only when
`dpr > (lower + upper) / 2`, so an exact midpoint ties to lower. This pinned
Flutter algorithm is distinct from the IDE security/size policy above.

One presentation projects only referenced resources, with at most 256 logical
assets, 256 resources and 16 MiB encoded bytes in total. Each resource remains
within 16 MiB, dimension 16,384 and 67,108,864 pixels; `resourceId` is lowercase
raw SHA-256 of the immutable compressed bytes. Canvas model v13 over NBFC
framing v1 negotiates
`asset.imageBytes.v1`; one resource-bearing render follows the canonical
`CONTROL` → `MODEL` → `IMAGE` order: exact `host.render` control, model JSON
with sorted resource descriptors, then NBFC kind 4 `IMAGE_BYTES` frames.
Revision, resource order, descriptor/payload size and SHA are rechecked before
admission. Authenticated media/decode/resize/center-slice failures quarantine
only the affected resource, while framing, identity, digest, ordering and exact
model-resource coverage failures remain fatal. The independently declared
Java/Dart capability fingerprint includes
the provider/resize union, all 13 image arguments, five filters and dependent
fit rule, so an incomplete runner fails closed at capability/model admission
rather than narrowing the model. No filesystem path, callback identifier or
project Dart crosses the boundary. This is asset-resource transport, not a
screenshot/framebuffer proxy.

Native and internal exact-Web runtimes both turn an available resource into
`MemoryImage(bytes, scale: resolvedScale)`, optionally wrap it once in
`ResizeImage`, and construct the real `DecorationImage`. Runtime validation
predicts the pinned codec, not an idealized resize: native exact resize rounds
a missing width, truncates a missing height and honors explicit upscale; Web
rounds either missing dimension and its codec path forces no-upscale, returning
the intrinsic image if the completed target would enlarge either axis. A
Flutter `fit` result with a zero axis is preserved as invalid for
`centerSlice`, never clamped to a fictional 1 px. An unavailable or quarantined
logical asset produces a deterministic non-interactive checker placeholder and
accessible status naming the logical identity, issue code and bounded reason.
The selection/layout outline, padding/margin guides and Palette/move drop
overlays remain outside the decorated/transformed `Container` and remain usable.

FittedBox contributes three optional constructor rows. `fit` is a closed
`BoxFit` enum containing `fill`, `contain`, `cover`, `fitWidth`, `fitHeight`,
`none` and `scaleDown`; `alignment` uses the existing finite physical/
directional `AlignmentGeometry` editor; and `clipBehavior` contains `none`,
`hardEdge`, `antiAlias` and `antiAliasWithSaveLayer`. Restore Default omits the
argument and therefore restores `BoxFit.contain`, `Alignment.center` or
`Clip.none` without storing a synthetic default. Its optional `child` remains a
slot rather than a writable property row.

ConstrainedBox contributes one required `BoxConstraints constraints` row. The
shared editor represents each width and height axis as one of the normalized
finite/unbounded/expanding states: `0..∞`, `0..max`, `min..∞`, `min..max`, tight
`value..value`, or `∞..∞`. Finite values are non-negative, a minimum cannot
exceed its maximum, and an infinite minimum requires an infinite maximum. The
required row cannot be reset to omission; its creation value is `0..∞` on both
axes. The optional `child` remains a slot rather than a writable property row.

UnconstrainedBox contributes four optional constructor rows. `textDirection`
admits `ltr` or `rtl`; omission lets directional alignment resolve through the
ambient `Directionality`. `alignment` reuses the finite physical/directional
`AlignmentGeometry` editor and omits to `Alignment.center`. `constrainedAxis`
admits `horizontal` or `vertical`, with omission removing constraints on both
axes. `clipBehavior` admits `none`, `hardEdge`, `antiAlias` or
`antiAliasWithSaveLayer` and omits to `Clip.none`. No creation-time property
default is persisted. The optional `child` remains a slot rather than a writable
property row.

LimitedBox contributes two optional constructor rows. `maxWidth` and `maxHeight`
admit finite non-negative values; omission canonically represents each
`double.infinity` framework default. Restore Default therefore removes the
argument rather than storing a duplicate infinity value. The optional `child`
remains a slot rather than a writable property row.

OverflowBox contributes six optional constructor rows. `alignment` reuses the
physical/directional `AlignmentGeometry` editor. `minWidth`, `maxWidth`,
`minHeight` and `maxHeight` accept finite non-negative doubles only; omission
inherits the corresponding parent bound, and each present minimum must not
exceed its matching maximum. `fit` admits only `OverflowBoxFit.max` and
`OverflowBoxFit.deferToChild`, omitting to `max`. Explicit non-finite overrides
fail closed. The widget symbol comes from `package:flutter/widgets.dart`; the
fit enum's exact symbol and generated import come from
`package:flutter/rendering.dart`. The optional `child` remains a slot rather
than a writable property row.

Flexible contributes two optional constructor rows. `flex` admits portable
non-negative integers and omits to Flutter's value `1`; zero is valid. `fit`
admits only `FlexFit.loose` or `FlexFit.tight` and omits to `loose`; the enum is
owned by `package:flutter/rendering.dart`. Its single any-widget `child` is
required, replacement-only and not a writable property row or insertable DnD
destination. Flexible is valid only as a direct Row/Column child, and its direct
child cannot itself be Expanded or Flexible because that nested
ParentDataWidget would lose its required direct Row/Column relationship.

Spacer contributes one optional constructor row. `flex` admits positive
portable integers and omits to Flutter's value `1`. Spacer owns no slot and is
valid only as a direct Row/Column child. Its widget symbol is const
`flutter.widgets.Spacer` from `package:flutter/widgets.dart`.

Baseline contributes two required constructor rows and one optional child slot.
`baseline` admits every finite double and starts at `24.0`; `baselineType`
admits `TextBaseline.alphabetic` or `TextBaseline.ideographic` and starts at
`alphabetic`. Neither required row can be reset to omission. Its widget symbol
is const `flutter.widgets.Baseline` from `package:flutter/widgets.dart`.

Offstage contributes one optional boolean constructor row and one optional child
slot. Omission preserves Flutter's `true` default while explicit `true` and
`false` remain separately stored values. Its widget symbol is const
`flutter.widgets.Offstage` from `package:flutter/widgets.dart`.

SizedOverflowBox contributes one required atomic Size row, one optional
physical/directional alignment row and one optional child slot. Its Size editor
commits finite non-negative width and height together; prototypes start at
`Size(100, 100)`. Its widget symbol is const
`flutter.widgets.SizedOverflowBox` from `package:flutter/widgets.dart`.

Transform contributes one required Matrix4 row, optional atomic signed finite
Offset origin, physical/directional alignment, boolean transform-hit-test and
closed FilterQuality rows, plus one optional child slot. Its prototype starts
with an identity matrix. Its widget symbol is const `flutter.widgets.Transform`
from `package:flutter/widgets.dart`; named convenience constructors remain
outside the slice.

These 674 fields use catalog constraints for editor selection, value admission,
Dart generation and Canvas projection. Text strings use a bounded text editor;
font fallbacks use a dedicated multiline one-family-per-line editor; optional
booleans render and edit every explicit value as an accessible checked/unchecked
checkbox, while omission remains the separate `<not set>` state restored through
**Restore Default**; integer and double values use exact constrained numeric
controls; enum values use closed
catalog lists; theme-aware colors choose literal ARGB or a reviewed
`ColorScheme` role; and structured values use transactional Paint and ordered
Shadow/OpenType editors with Add/Remove/Up/Down controls. ElevatedButton adds
strict callback-ID fields and the same typed state-aware color, numeric, enum,
insets, typography and shape editors. Built-in Icon data uses
the searchable reviewed Material registry plus an explicit **None** choice. A
shared Swing preview paints the exact glyph beside its readable name in both
the PropertySheet cell and chooser list. The font is loaded off the EDT only
from the currently resolved Flutter SDK at its fixed normalized cache path after
no-link, stable-byte,
size and packaged-manifest SHA-256 verification; a platform font is never used
as fallback, and unavailable bytes retain a neutral vector placeholder plus
the complete textual/accessibility contract.
Container adds the transactional alignment, constraints, matrix and tabbed
decoration editors described above. Its accessible Image tab selects only the
declared app/package inventory and exposes provider/exact scale, one resize,
callback, five filters, fit/alignment/center slice, repeat/direction, scale,
opacity, quality, inversion and anti-alias controls. The inventory status and
every dependent field have concrete accessible names/descriptions. Every
optional structured editor has an explicit inherited/default state, includes
an active table cell before OK and cannot publish an invalid draft.
In particular, `aspectRatio > 0`, `maxLines > 0`, `textScalerFactor >= 0`, `styleFontSize >= 0`,
`strutFontSize > 0` and `strutLeading >= 0`; the remaining admitted Text
style/strut doubles are finite without inventing undocumented constructor
ranges. Locale subtags must be
non-empty when present, and a style/strut package requires a font family or a
non-empty fallback list.

An accepted cell edit creates exactly one `SetProperty` bound to the selected
stable widget ID and captured document revision. NetBeans' native **Restore
Default** creates `ResetProperty` only for optional arguments. A single-use
submission fence prevents duplicate editor callbacks from reusing the captured
revision; the accepted candidate then follows the existing analyzed pair-save
and shared Undo/Redo lifecycle. Required `Text.data`, `Padding.padding` and
`Icon.icon` cannot be reset to omission; optional `SizedBox.width` and `SizedBox.height`
can. A complete BoxDecoration/Image draft publishes as one structured property
edit; any required top-level repair uses one `PatchProperties`, so each accepted
image transition is one chronological Undo/Redo unit and one canonical pair-save.

Generation folds Text leaves into `Locale.fromSubtags`, `TextScaler.linear`,
`TextHeightBehavior`, `TextStyle`, its optional nested locale and combined
decoration, and `StrutStyle`. A selected semantic `TextTheme` role is the base;
explicit leaves are emitted through `copyWith`. Color roles resolve through
`Theme.of(context).colorScheme`. A composite is omitted when none of its leaves
is present. Icon generation emits only typed `IconData` metadata and the 12
reviewed named arguments. The bounded Canvas payload carries the same typed
values, and the
isolated runner constructs real `Paint`, `Shadow`, `FontFeature` and
`FontVariation` objects as well. An absent list means omit/inherit; an explicit
empty list means clear. This gives Properties, generated Dart and native Canvas
one mapping rather than a lossy presentation string.

BoxDecoration generation emits `image` immediately after `color`, preserving
Flutter constructor order. Providers become const `AssetImage` or
`ExactAssetImage` and optional const `ResizeImage`; the image graph emits all 13
arguments, five `ColorFilter` constructors, `Rect.fromLTRB` and closed enums
without raw text. Theme detection walks `ColorFilter.mode`. Const propagation
is structural: `ColorFilter.saturation` and a callback identifier keep the
containing decoration non-const; the other closed values remain const-capable
only while their nested values (for example, a literal rather than theme color)
are const.

The deprecated `Text.textScaleFactor` argument and `key` are deliberately not
shown; `textScalerFactor` is explicitly labeled as linear and generates the
current `textScaler` argument. Paint shaders and image filters, plus ColorFilter
forms outside DecorationImage's five-variant union, remain outside the reviewed
safe subset and are not represented by raw Dart or opaque string escape hatches.
Schema v3 represents Padding as either physical `EdgeInsets` or
text-direction-aware `EdgeInsetsDirectional`; All, Symmetric and Individual are
editor modes that canonicalize to four semantic sides. Flex baseline alignment
additionally requires an explicit `textBaseline`, and
`Text.semanticsIdentifier` is unique within one designer tree. Scaffold's
separate closed 17-field scalar contract is described above; its excluded
widget-valued and runtime graphs remain fail-closed.

The Explorer widget hierarchy is an outline of the complete current model, so
its branches remain expanded after initial load, mutation, root replacement and
look-and-feel refresh. Collapse attempts are vetoed, and the now-redundant
expand/collapse icons are removed from this tree's own `BasicTreeUI` instance.
Thin light/dark-aware golden parent-child connectors are painted only by this
Designer tree; no global `UIManager` tree style is changed. The tree is also a scoped Palette
drop target. It resolves the exact hovered row to a stable widget ID and uses
the same catalog compatibility matrix and `AddWidget` planner as the native
Canvas. Hover preview is non-consuming; commit re-reads the latest immutable
document and consumes the opaque Palette token exactly once. A tree row is
admitted only when its widget type has one catalog-compatible destination:
`Row`/`Column.children` append at the terminal index and empty
`Center.child`/`Padding.child`/`SizedBox.child`/`ElevatedButton.child` use index zero. Leaves, occupied single slots and
multi-slot parents such as `Scaffold` fail closed. The selected parent's
explicit `Slots` Properties tab, rather than heuristic tree drop, owns the
exact multi-slot choice.

The same tree is a JVM-local drag source for an existing non-root widget.
`BeanTreeView`'s Explorer drag source is disabled for this view and its inactive
AWT drop target is detached so Swing can install the local `TransferHandler`
target. A scoped left-button mouse adapter waits for the platform drag threshold
and explicitly exports through that handler. The adapter is removed and the
exact prior handler, drop target and active state are restored with the view
lifecycle. Its private transfer retains only the originating tree identity and source stable ID; it is
not accepted by another form or process. Single tree selection keeps the source
and the standard Properties Node deterministic. `DropMode.ON_OR_INSERT`
provides two semantic targets. `ON` first finds every catalog-compatible slot,
before considering occupancy: exactly one slot is required, a list appends after
source removal, and an empty single slot uses index zero. `INSERT` means the
flattened child boundary before child `i` or after the last child. The boundary
derives the anchored child's semantic slot, accepts only a list slot and
normalizes the destination to a post-removal index for same-list reorder.

Planning rejects the root, a target inside the source subtree, missing catalog
definitions, incompatible/full/cardinality-mismatched slots, a source slot that
would fall below its minimum, an invalid boundary and an exact no-op. One
accepted `MoveWidget` removes and reinserts the same immutable subtree, so every
descendant and stable ID is preserved. Hover is non-mutating. Drop obtains the
latest immutable document, catalog and revision token, repeats the full plan and
requires the exact command prepared during hover before submitting it to the
shared mutation, Undo/Redo and Save pipeline. When generation proves that Dart
bytes are identical, the command takes the exact `FD_ONLY` boundary: canonical
`.fd` alone is committed while disk/live Dart and Source Undo remain unchanged.

The flattened tree deliberately cannot name an empty slot on a multi-slot
parent. `ON` therefore remains ambiguous even if only one compatible slot is
currently empty; `INSERT` needs an existing list-child anchor and rejects a
single-slot anchor. `Scaffold` and future multi-slot widgets use the explicit
`Slots` Properties editor rather than an occupancy heuristic.

`Slots` is a conditional native Properties set built from the selected
widget's catalog definition, not from only its populated model children. It
therefore exposes every exact named slot in declaration order, including an
omitted optional slot. Each row reports single/list cardinality and occupancy
and opens a transactional custom editor. The editor may add a compatible
reviewed prototype, move/reorder one existing non-root widget into the named
slot, explicitly replace an occupied single child with a fresh prototype or an
existing subtree, remove one exact direct child, or atomically clear every
direct child of a list slot whose minimum cardinality is zero. An occupied
single slot labels removal `Clear`; replacement is always a separate explicit
action. The draft has no authority until the dialog commits, so Cancel changes
neither model nor files.

The dialog filters choices with the shared catalog acceptance matrix and the
same pure placement planners used by Palette/tree DnD. Its semantic intent is
bound to the presented document, catalog and revision. On OK, MultiView checks
that exact authority again, repeats the full plan against the current immutable
snapshot, and submits precisely one `AddWidget`, `MoveWidget`, `RemoveWidget`,
`ReplaceSlotChild` or `ClearSlotChildren` through the existing generation,
analyzer, pair Save and chronological Undo/Redo pipeline. Compound operations
also fence the exact expected current child or ordered direct-child IDs. Stale
dialogs, cycles, root moves, incompatible or full slots, minimum-child
violations and no-op moves fail closed with operation, target and reason. No
`.fd` schema, Canvas wire protocol, second persistence path or second Undo
history is introduced.

For this safe slice a new list child is appended, while an existing child may
be moved to every planner-admitted post-removal position. An occupied single
slot never performs an implicit replace. Explicit replacement and Clear All are
implemented as dedicated atomic domain commands, never simulated as several
partial remove/add commands, so one accepted action produces one pair-save and
one Undo/Redo step.

The Canvas itself is a real native `FlutterView` embedded inside that chrome.
Flutter paints the widget tree, a scale-stable thin dashed outline for every
widget, the solid blue selected-widget outline, direction-resolved orange
`Padding` distance guides, drop zones and layout guides directly into its native
surface and performs the authoritative widget hit test. These paint-only
affordances do not alter Flutter layout, hit boxes or DnD geometry. Swing and the
NetBeans Visual Library must not imitate Flutter widgets or
maintain a competing layout model. A Swing-painted projection, transferred
PNG/JPEG/raw-RGBA frames, screenshots or periodic image copies are not an
acceptable Canvas implementation. A heavyweight Java host peer may reserve the
native region, but Flutter remains its renderer.

The host-window size is not the Flutter responsive viewport. Every preview
profile retains its exact logical width, height, device-pixel ratio and
`MediaQuery`; resizing NetBeans changes only how that fixed viewport is
presented. Each Design MultiView owns an in-memory presentation state: `Fit`
scales down as needed without enlarging the profile, while manual zoom accepts
the reviewed 25–200% range. Overflow is clipped and scrolled inside the native
Flutter surface. Flutter owns the scrollbars, wheel/Shift+wheel scrolling,
Ctrl+wheel zoom, transformed hit testing and semantic drop coordinates; placing
a Swing `JScrollPane` around the heavyweight child HWND would create a competing
geometry authority. Presentation values use normalized integer micros, are
fenced to one exact model revision, and never enter `.fd`, generated Dart,
Undo/Redo or project preferences.

Native hosting is abstracted by a planned platform SPI. Its contract covers
creation/attachment, detachment, bounds and device-pixel-ratio changes,
visibility, focus, liveness, crash notification and final destruction. Platform
handles stay at the NetBeans/native edge and do not enter the domain model. The
first implementation target is Windows: a Flutter desktop runner creates a
native child surface and the Windows provider embeds it into the NetBeans host.
Linux and macOS providers follow the same SPI and lifecycle/fencing tests; no
platform may substitute an image-transfer surface.

The preferred Windows design places the Flutter engine and `FlutterView` in an
isolated runner process when child-surface embedding and supervision prove
feasible. NetBeans owns bounded startup, cancellation, restart, termination,
diagnostics and project/form-close cleanup. Isolation contains crashes but is
not claimed as an operating-system security sandbox. The current protocol
supplies only bounded canonical allowlisted model data and lifecycle/input
intents; catalog data requires a future reviewed versioned contract. It supplies
no project paths, Dart source, file handles or arbitrary project code.
The runner has no `.fd`, Dart, `SaveCookie`, Undo/Redo, command-session or
persistence authority. If a platform cannot support the isolated-child design,
an alternative native provider requires explicit review and the same ownership
contract; falling back to transferred pixels is forbidden.

The implemented pure lifecycle controller is owned by one Design MultiView,
not by the project or DataObject. Each start/restart creates a fresh session,
admits one presentation request at a time, coalesces a burst to one replace-only
latest request, and fences callbacks from detached attempts. Startup,
handshake, rendering, protocol, termination and closed states are explicit. The
backend contract is thread-safe and asynchronous; concrete adapters must prove
prompt calls, idempotent close, bounded termination escalation and eventual
completion of all stages. Listener delivery has no EDT affinity, so the
NetBeans adapter must marshal presentation changes itself.

The concrete Windows adapter follows that ownership boundary as well. An open
`.fd` Design view creates one heavyweight host and one isolated runner; hiding
the Design view hides its verified child surface, while closing the editor
terminates only that view's process. Generation fencing prevents late build,
launch and `Process.onExit` callbacks from tearing down a newer surface. A
failed `MoveWindow` or invalid HWND hierarchy clears only the matching
attachment and reports peer loss without letting an exception escape the EDT.
The shared build cache is reusable only when its final commit marker and bounded
runtime SHA-256 manifest agree with every allowlisted launch artifact.

The version 1 lifecycle handshake is exact and deliberately small: strict UTF-8
JSON without a BOM for `host.hello`, `runner.hello`, `host.close`,
`runner.closed` and bounded `runner.failure`. Runner control sequences start at
zero and are contiguous; wire integers do not exceed 9,007,199,254,740,991. The
implemented process codec validates a fixed header, kind-specific negotiated
size and SHA-256 before accepting a payload. Its complete frame-kind whitelist
is control JSON, model JSON, NBFC kind 4 `IMAGE_BYTES` and catalog JSON; only
control is legal before hello, catalog remains reserved, and image bytes require
`asset.imageBytes.v1` plus the immediately preceding exact revision/model
descriptor and declared resource order. Stream-bound readers and writers fail
closed after malformed or partial traffic, and model/image frames require the
exact expected kind/length/SHA-256 descriptor before allocation.

After the handshake, a strict runtime control codec carries `host.render`,
`runner.presented`, `host.selection`, `runner.selection`, the capability-gated
`host.viewport`/`runner.viewport` pair and the capability-gated
`runner.paletteDrop` event and source-aware `host.paletteDragSource` command,
the capability-gated `runner.textEditCommit` event, and the optional
capability-gated
`host.widgetMovePreview`/`host.widgetMovePreviewClear` pair, for exact session,
presentation, revision, frame and layout identities. Viewport commands use a monotonically increasing
`commandSequence`; exact acknowledgements, delayed-metric rejection and bounded
queue retry prevent an older scale/scroll report from replacing newer toolbar
intent. The `viewport.presentation.v1` capability changes only presentation:
the runner continues to build with the fixed logical `MediaQuery`. The
`palette.drop.catalogInsert.v1` capability is limited to the exact reviewed
DnD-capable definitions below. `palette.drop.sourceAware.v1` binds the opaque
token to the exact current canonical type and traits for hover filtering;
negotiating or decoding either capability does not by itself enable Palette
mutation. `host.render` describes one canonical bounded protocol-v14 model
frame. The runner decodes only the thirty-four reviewed Canvas-capable built-in
widget contracts and never loads project code. `CATALOG_JSON` remains reserved
for a future versioned catalog contract. When `asset.imageBytes.v1` is
negotiated, the model's sorted resource descriptors are followed by exact NBFC
kind 4 `IMAGE_BYTES` frames for that revision. These carry compressed referenced
asset bytes only; the Windows path still renders the validated model directly
in its native Flutter surface and never transfers Canvas screenshots or
framebuffer pixels.

`widget.movePreview.v1` is disposable view feedback, never move authority. Java
sends source/parent/slot/post-removal index with the exact presented identities
and a monotonic preview sequence. Last-write-wins bounded retry ensures a newer
target or clear supersedes queued older feedback. Flutter excludes the source
subtree while resolving post-removal list geometry and paints a thin amber
before/between/after marker or compatible container zone, visually distinct
from blue selection and Palette feedback. An explicit clear, invalid target,
drag cancel/exit/export completion/drop, model/layout/presentation/session
replacement, runner failure or close removes it. Missing capability or an
unavailable Canvas disables only this projection; the Swing-tree planner and
move remain available.

Every presentation receives a fresh host-issued open-session identity and a
monotonically increasing presentation sequence, independent of the logical
command revision id. Flutter responses echo that identity. Frame and layout
sequences identify a native paint/presentation epoch and its Flutter-side
hit-test epoch; they do not identify transferred images. Initial frame/layout
evidence is admitted atomically. These identities reject delayed work across
reload, Undo/Redo, runner restart, close and reopen, including an ABA return to
the same logical revision.

Palette insertion has two UI routes that share one Java authority, catalog
matrix, planner and mutation pipeline: the native Canvas route crosses the
child-HWND boundary and returns an intent, while the widget-tree route targets
one exact Explorer row. The reviewed insertion slice is deliberately closed to
the thirty-four exact DnD-capable Palette definitions: `Scaffold`, `AppBar`,
`ElevatedButton`, `TextField`, `Column`, `Row`, `Padding`, `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `Text`,
`Icon`, `Image`, `ListView`, `Wrap`, `Container` and `Opacity`.
Thirty-one any-widget slots expose the reusable slot contract: `Scaffold.body`,
`Scaffold.floatingActionButton`,
`Column.children`, `Row.children`, `ListView.children`, `Wrap.children`, `Padding.child`,
`Center.child`, `Align.child`, `FittedBox.child`, `ConstrainedBox.child`,
`UnconstrainedBox.child`, `LimitedBox.child`, `OverflowBox.child`,
`FractionallySizedBox.child`, `SizedBox.child`, `AspectRatio.child`,
`ElevatedButton.child`, `Container.child`, `Opacity.child`, `Baseline.child`,
`IntrinsicHeight.child`, `IntrinsicWidth.child`, `Offstage.child`,
`SizedOverflowBox.child`, `Transform.child`, `Stack.children`,
`AppBar.leading`, `AppBar.title`, `AppBar.actions` and
`AppBar.flexibleSpace`. The two trait-bound slots, `Scaffold.appBar` and
`AppBar.bottom`, admit only AppBar through its canonical
`PreferredSizeWidget` trait. Expanded and Flexible each admit only direct
`Row.children` and `Column.children` wrapping; Spacer enters those two list
slots by ordinary insertion, and the other thirty-one sources enter all
thirty-one any-widget destinations. The wrappers' occupied required child
slots are replacement-only and excluded from the insertable matrix. This yields
1,122 candidate cells: exactly 969 admitted and 153 rejected. An empty Row, Column or ListView has no ordering
ambiguity, so its complete bounded visible design-time rectangle resolves
insertion index zero; after the first child, only the terminal append zone is
exposed. Stack uses its full rendered rectangle for every z-order append. Wrap
also uses its full rectangle for terminal append because run formation has no
stable linear edge, while AppBar actions use their dedicated logical actions
zone. Empty zero-size FittedBox, ConstrainedBox, UnconstrainedBox, LimitedBox and
OverflowBox nodes, plus an empty or collapsed Baseline, IntrinsicHeight,
IntrinsicWidth or Offstage,
expose only their
bounded Designer single-child targets; the helper geometry does not affect
Flutter layout.
Expanded and Flexible Palette creation use a separate atomic wrapper mode over
an existing direct Row/Column child, never terminal Add or an empty placeholder;
neither may wrap either wrapper type or Spacer. Spacer uses ordinary insertion
but only for a direct Row/Column list destination. Image Add
and Replace New Widget resolve the first sorted declared asset before stable-ID
allocation and fail closed when inventory is unavailable. TextField remains an
ordinary direct leaf source; its generated and Canvas constraint guards make
unbounded flex placements safe without a placement exception.
The slice does not support an arbitrary list index, a definition without the
exact reviewed DnD capability or an
unreviewed constructor slot. This
restriction describes Palette `ADD`; existing-widget tree `MOVE` is the separate
bounded route below.

1. On Windows, NetBeans starts one native OLE drag for a reviewed DnD-capable
   Palette item. Java retains its exact authoritative widget type behind a
   bounded, short-lived opaque
   token. Its wire representation is printable ASCII, at most 160 characters
   and starts with `nbfdnd:v1:`. The token is process-local and one-shot; it is
   not widget JSON, a project path, Dart source or mutation authority. The same
   exact active-view token must cross hover, prepare and the terminal commit or
   cancel unchanged; only Java resolves it to the retained prototype. Before
   native transfer publication, Java also projects the exact type and traits to
   the current layout. A failed projection revokes the token and starts no drag.
2. The OLE bridge delivers only that token and native-view coordinates across
   the child-HWND boundary. OLE reports `MOVE` because the NetBeans Palette
   offers `ACTION_MOVE`; Palette items are immutable prototypes and are not
   removed. The semantic Designer operation remains `ADD`, never an
   existing-widget Move or reorder.
3. Flutter performs the authoritative hit test against its live tree and
   accepts only the reviewed empty-single or list-append drop zones from the
   exact currently presented layout. An empty Row/Column uses its bounded
   36-pixel-minimum visible Designer area; a populated list uses only its
   terminal edge. Native hover starts fail-closed, coalesces
   bounded probes, rejects stale generation/probe replies and advertises OLE
   `MOVE` only after the exact latest Flutter approval.
4. Fast release is admitted only when the exact latest probe was already sent
   and remains in flight. Hover and prepare share one FIFO `MethodChannel`, so
   Flutter establishes the token/generation/probe/target state before handling
   prepare even when the native hover-result callback has not arrived.
5. Prepare repeats exact token, generation, probe, point, presentation, layout
   and semantic-target validation and stores at most one candidate. It emits no
   `runner.paletteDrop`. The synchronous OLE `Drop` pumps its Windows STA for at
   most 250 ms while waiting for the asynchronous reply. Timeout, error,
   reentrant cancellation or shutdown fails closed, sends the matching cancel
   and leave while the channel remains alive, and returns OLE `NONE`. Exact
   cancel consumes only its matching prepared candidate, so a late prepare
   result after `NONE` cannot mutate Java state.
6. Only a timely positive prepare sends an ordered single-use commit and
   returns OLE `MOVE`. Commit consumes and revalidates the prepared identity,
   then may publish `runner.paletteDrop` with the token, parent stable ID,
   `operation = ADD`, the reviewed `slotName` and exact insertion index, plus the
   exact session/presentation/document/logical-revision/frame/layout/intent
   identities. Flutter does not mutate the model or touch a file.
7. Java atomically consumes the token and rejects unknown, expired, duplicate,
   malformed, stale or foreign responses. It rechecks the exact current
   revision and layout, resolves the current parent and slot through the widget
   catalog, and verifies cardinality, acceptance, capacity and insertion index
    before materializing the consumed capability-gated type's catalog creation defaults
    with a fresh stable ID. OLE `MOVE` is not proof of this
   Java admission and may still be followed by a fail-closed rejection.
8. One admitted `AddWidget` command follows the existing deterministic
   generation, analyzer, paired `.fd`/Dart replacement and `PairSaveCoordinator`
   adoption path. The successful user action contributes one chronological
   native Undo/Redo edit; failure before verified adoption changes neither
   file nor history.

The widget-tree route never depends on native Canvas readiness or presented
layout evidence. Its Swing target accepts only the bounded opaque Palette token
on one exact visible row, performs repeatable non-consuming preview, and on
drop re-resolves that target against the latest document. It appends to the
sole compatible list slot or fills the sole compatible empty single slot, then
submits the same `AddWidget` command described in step 8. If the catalog exposes
more than one compatible slot, the parent row remains ambiguous regardless of
current occupancy; the route never silently changes a `Scaffold.body` drop into
`Scaffold.floatingActionButton`.

Existing-widget tree DnD does not reuse the Palette token or native OLE route.
A scoped platform-threshold mouse bridge starts a private same-tree transfer
that identifies the current source stable ID, and the pure planner produces one
catalog-authorized `MoveWidget` for `ON` or `INSERT`
as specified above. Canvas receives only an optional host-driven amber
projection of that already-planned destination; it neither admits nor reports
the move. Commit re-plans against the latest snapshot and exact revision token,
then uses the same chronological Undo/Redo and pair-save authority as every
other admitted Designer command. PAIRED moves regenerate/analyze/replace the
managed Dart regions; byte-identical generated Dart instead uses the exact
one-file `FD_ONLY` commit.

Token consumption is fail-closed: a rejected or failed drop attempt cannot
reuse the token. Canceled, failed and non-`MOVE` drag completion revokes it
immediately. Successful OLE `MOVE` retains it only for a bounded three-second
asynchronous grace, ending earlier on consumption; expiry, another drag, runner
restart, Canvas close, presentation replacement or a new layout invalidates
outstanding drag authority. The historical first public contract admitted only
`Text` at a terminal `Row|Column.children` position or empty
`Center.child[0]`. It is superseded by the current thirty-four-definition,
1,122-candidate matrix with 969 accepted and 153 rejected cells described above,
whose live assembled drop → Save → Undo → Redo → Save
acceptance passed. This statement does not claim a separate saved-history Undo
→ Save cycle. Palette insertion outside the reviewed matrix and existing-widget
movement outside the same-tree `ON_OR_INSERT` contract remain disabled,
including native Linux, macOS and Web DnD backends.

Inline `Text.data` editing follows the same host-authoritative rule and adds no
second model or persistence channel. On Windows, double-click or F2 activates
only the selected existing `flutter.widgets.Text`. The runner temporarily
renders a real multiline Flutter `TextField`, whose `TextInputClient` owns OS
text input and composition. Ordinary Enter inserts a newline. Ctrl+Enter emits
a final commit and Escape cancels only when the composing range is empty;
preedit remains runner-local and never becomes an intermediate Java command.
The heavyweight AWT carrier disables input methods, and Java neither decodes
nor relays `WM_IME` into Flutter.

The runner must negotiate `widget.inlineTextEdit.v1` before it may emit one
`runner.textEditCommit`. The strict body binds presentation, document, logical
revision, frame, layout and one-shot intent identities, the current
interaction-fence sequence and selected stable widget ID to bounded
well-formed Unicode final text plus `compositionObserved` metadata. Host
admission requires the exact current visible presentation/layout/fence, the
exact selected existing `Text`, and first delivery. Missing, extra, malformed,
oversize, stale, foreign, replayed, hidden, selection-mismatched or non-Text
events produce no mutation. One admitted event maps to at most one existing
`SetProperty(data)` command; unchanged text is a no-op. Changed text therefore
reuses deterministic generation, analyzer admission, paired Save and
chronological Undo/Redo. Deterministic Flutter and Java codec/channel/session
plus view/mutation-bridge tests accept this Windows product slice.
Current acceptance is English input only. Physical CJK IME and other
language-specific acceptance remain open and are deferred to the final
internationalization phase because the current gate host has no
composition-capable input method. No Linux/macOS or runtime-faithful Web inline
editor is claimed.

Selection and other future property intents follow the same
host-authoritative rule.
Render-profile limits reject a requested native surface above 4096 physical
pixels on either axis or 8,388,608 total pixels before native allocation. This
is a resource bound for the embedded surface, not a raw-RGBA transfer budget.

Every presentation request selects one exact compatible pair from real platform
folders in the owning Flutter project: `MOBILE`/`TABLET` with Android or iOS,
`DESKTOP` with Windows, macOS or Linux, and `WEB` with Web. The mode supplies the
viewport intent; `CanvasTargetPlatform` supplies Flutter adaptive appearance;
`CanvasEngineIdentity` and the native host identify the concrete runtime. The
Windows-first embedded Canvas is authentic for its bound Windows Flutter engine,
exact resolved theme, locale, viewport, text scale and device-pixel ratio.
Android/iOS/macOS/Linux `ThemeData.platform` behavior does not claim their OS,
fonts, plugins or platform channels. Web renders its exact browser-sized
responsive viewport on the native engine as a bounded layout preview, using
Windows adaptive controls because Flutter has no `TargetPlatform.web`. It does
not claim `kIsWeb`, DOM, browser fonts, plugins or platform channels. Runtime
fidelity belongs to the separately selected browser-compiled WebView2 backend;
the existing native preview does not acquire those claims merely because the
browser runner foundation now compiles. A complex built-in or contributed
widget is rendered only after
its type and constructor metadata are present in the validated catalog; the
runner may not execute arbitrary unreviewed project code merely because Flutter
can load it.

That WebView2 backend uses a static Flutter Web release bundle, direct DOM
multi-view embedding and the same bounded Canvas runtime/model decoder. Its page
bridge transports existing NBFC bytes through JSON web messages only after an
exact host-issued 256-bit session nonce is present; fixed format, version,
direction, contiguous sequences and bounded canonical base64 chunks fence the
transport. Exact-session malformed, oversized, wrong-envelope and sequence-invalid
messages are terminal; foreign nonces are ignored. The browser entry point,
authenticated JavaScript/Dart transport, focused Dart tests, manually executable
headless-browser bridge harness and deterministic offline release-build proof
with local CanvasKit and licensed local Roboto for the English-only scope are
complete. The Web runtime profile observes resize metrics and republishes exact
layout geometry without installing or advertising the Windows OLE DnD channel.
The native Win32 WebView2 Runtime/loader and COM child-controller host, virtual
HTTPS origin, frozen resource policy, Java endpoint and standalone physical host
gate are complete. A disconnected build/cache prerequisite now binds the exact
Flutter, framework, engine and Dart SDK identity into its compiler defines,
cache fingerprint, result and `runner.hello`, then privately publishes every
validated generation. The assembled MultiView component/owner handoff and its
asynchronous close-handler foundation are implemented internally; production
routing, the split/clone/direct-close peer-removal gate, end-to-end Retry/crash
behavior and NetBeans physical acceptance remain open. Host-level explicit
focus/visibility behavior is implemented. The
internal exact-Web session now
consumes that authenticated stream, checks the compiled engine identity against
`runner.hello`, reuses the native session's model/layout/selection/viewport and
interaction fences, and matches Flutter presentation against independently
observed WebView2 physical metrics. A backend-neutral routed factory and
epoch-fenced asynchronous owner coordinator guarantee retirement-before-create
and retry poisoned cleanup. That coordinator is now the assembled MultiView's
component-replacement authority: it keeps the retiring heavyweight component
attached until the pre-peer-loss barrier completes, removes it before installing
the successor, and admits callbacks only for the exact active epoch. The close
foundation vetoes the current stack and retries TopComponent close only after
coordinator retirement. It is not installed in production by default while
exact-Web selection remains off, because NetBeans Split/Clear Split can directly
reparent or remove the AWT hierarchy without consulting `canCloseElement()`, and
closing a non-last clone bypasses `closeLast()` and its close handler.
Exact Web therefore remains unavailable as a product Canvas backend.

A RELEASE300 implementation audit additionally proves that the missing hook is
not available to a normal module: split/clear-split synchronously removes or
reparents children inside `TabsComponent`, while non-last clone admission
short-circuits inside `CloneableTopComponent.Ref`. The plugin-only replacement
path is a dedicated CES `CloneableEditor` shell. Its dormant foundation wraps
the one Source editor component in an explicit Design/Source switcher, forwards
the existing Design lifecycle, keeps the CES clone group, and exposes no
internal `Splitable` contract. It now owns a support-wide close reservation:
clone creation and ordinary close admission are mutually exclusive. One
`CloseCookie`/shell-owned Close All batch asks Save/Discard/Cancel exactly once
and then
captures the exact clone topology, live document version and atomic pair
coordinator/state/external-event/Source-state revision. It issues one exact
owner permit at a time, retires that clone-local Canvas and reaches
`componentClosed()` before dispatching the next owner. Topology or revision
drift and retirement failure abort fail-closed. The synchronous entry point
returns `false` while this sequence completes asynchronously instead of
claiming early success. Each open shell incarnation has a unique stamp. A
sibling lifecycle change may finish only the exact already-admitted physical
cleanup and then abort the batch; same-owner close/reopen ABA invalidates the
old permit and cannot remove the new incarnation. After NetBeans unregisters
the clone, the permit binds its exact post-admission topology and document/pair
revision. The final internal support close is authorized and acknowledged only
when exact pre/post snapshots match and `close(false)` actually succeeds. When
a document revision makes
an in-flight close stale, the shell abandons that permit, waits for the captured
owner coordinator to finish and rebuilds Canvas through a fresh coordinator
generation. The owner factory, active owner, observer and close completion are
generation-fenced, and the latest requested backend is retained. The shell
permanently sets `TopComponent.PROP_CLOSING_DISABLED` and exposes a shell-owned
permit-aware Close action, so RELEASE300 `Close Mode` cannot directly remove
the shell. Since that latch also makes the stock global Close All skip the
component, the shell exposes a permit-aware Close All action backed by the same
support batch. It deliberately uses `PERSISTENCE_NEVER` and is not selected by
`createPane()` in production. Pair-aware node Rename, Delete and Cut-Move now
acquire an operation-owned pair lease before starting the support close. The
support reservation remains active after the final exact
`componentClosed()`; only its identity-bound one-shot proof can dispatch the
matching operation callback off the EDT. That callback claims the same lease
and replays the existing synchronous operation once. Rename and Delete invoke
the real `DataObject` operation, preserving NetBeans events and binding updates,
while Cut-Move reuses the paired move transaction. An exact `CutSession` blocks
duplicate paste and clears only the still-current clipboard value after commit;
cancel or failure leaves it retryable. Cancel, topology/revision drift,
foreign/stale/reused proof, close failure and operation failure are
mutation-free and release the close and pair reservations. The legacy
synchronous entry point refuses a dedicated shell rather than reporting an
asynchronous close as success. Platform Source/History/action and
restart/runtime parity, exact-Web product binding, and physical NetBeans
acceptance remain mandatory before the shell or exact-Web selector may be
enabled. `New Tab
Group`, `Collapse Tab Group`, public `Mode.dockInto()` and direct post-removal
callbacks also remain unguarded because RELEASE300 exports no universal
plugin-side asynchronous pre-removal veto for those paths.

The current protocol-v14 projection intentionally contains exactly the thirty-four
Canvas-capable definitions: `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`,
`Column`, `Row`, `Text`, `Icon`, `Image`, `Padding`, `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
`IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `ListView`, `Wrap`, `Container` and `Opacity`. It proves native hosting, bounded model
publication,
exact native adaptive preview profiles and stable-ID selection synchronization.
Host-side property mutation is admitted only for the thirty-four widget types and 674
properties listed above, including the 17-leaf Scaffold, 286-leaf
ElevatedButton, 120-leaf AppBar, 59-leaf Text, 13-property Icon and 13-property
Container projections plus Opacity's two, Align's three,
FractionallySizedBox's three, Stack's four, Expanded's one, Flexible's two,
Spacer's one, Baseline's two, IntrinsicWidth's two, Offstage's one,
SizedOverflowBox's two, Transform's five, Image's 22,
TextField's 54, ListView's 17, Wrap's nine, FittedBox's three, ConstrainedBox's
required constraints property, UnconstrainedBox's four optional rows,
LimitedBox's two optional limits and OverflowBox's six optional rows; the runner still receives
no persistence authority.

This thirty-four-widget slice is complete across create, open, edit, save, reopen,
Undo/Redo, deterministic Dart generation, Palette/tree/Canvas insertion,
same-tree move and exact-slot management. Unreviewed widget definitions remain
fail-closed until each receives an equally complete independently reviewed
vertical slice.

Implementation proceeds through explicit gates:

1. [Complete] Define the pure host-issued session/presentation/frame/layout
   identities, exact validated-revision/render-profile binding and replay-safe
   interaction admission rules.
2. [Complete for the capability-gated projection] Add the read-only backend lifecycle, bounded
   versioned lifecycle handshake, runtime render/selection control and process
   framing. The canonical model schema is implemented; catalog JSON is reserved.
3. [Windows lifecycle spike complete; cross-platform contract pending] Define the
    platform-neutral native-surface SPI and prove its teardown,
    resize/DPR, focus, visibility, crash and stale-callback contract.
4. [Current thirty-four-widget slice complete] Embed the isolated Flutter runner,
   render one exact validated capability-gated revision and support the compatible
   Android/iOS/desktop adaptive profiles plus a native-engine Web layout
   viewport with the same revision-scoped typed asset resources and no
   screenshot/framebuffer transfer. The separately compiled browser runner
   foundation described below does not change this product route until every
   split/clone/direct-close peer-removal bypass is closed and the assembled
   physical gate passes.
5. [Context and bounded Properties complete] Synchronize stable-ID selection
   with the Explorer/Nodes widget tree, publish the exact thirty-four-item Palette
   and expose selected-node Properties. Enable catalog-driven Set/Reset/Patch
   for the 674 reviewed fields, including all 17 Scaffold leaves, all 286
   ElevatedButton leaves,
   all 120 reviewed AppBar leaves,
   all 59 reviewed Text leaves, both
   nullable non-negative `SizedBox` dimensions, the required positive
   `AspectRatio.aspectRatio` value, all 13 typed Icon constructor properties and
   all 13 typed Container constructor properties, Opacity's required alpha plus
   optional semantics flag, Align's and FractionallySizedBox's three rows each,
   Stack's four, Expanded's flex, Flexible's flex and fit, Spacer's flex,
   Baseline's baseline and baseline type, the property-free IntrinsicHeight
   child slot, IntrinsicWidth's step width and step height, Offstage's optional
   boolean and child slot, SizedOverflowBox's structured size, alignment and
   child slot, Transform's matrix, origin, alignment, hit-test and filter rows
   plus child slot, Image's 22,
   TextField's 54, ListView's 17 and
   Wrap's nine rows, FittedBox's three rows, ConstrainedBox's required
   constraints row, UnconstrainedBox's four optional rows, LimitedBox's two
   optional maximum rows and OverflowBox's six optional rows,
   through one-shot
   revision-bound pair-save.
6. [Complete; historical Text-only slice superseded] Implement the Windows
   native OLE Palette drag → Flutter hit-test and two-phase prepare/commit →
   exact revision/layout-bound intent → one-shot Java admission →
   pair-save/Undo command path described above. The first accepted vertical
   slice admitted only `Text` at terminal `Row|Column.children` or empty
   `Center.child[0]`; the current contract is the thirty-four-source,
   1,122-candidate compatibility matrix with 969 accepted and 153 rejected cells.
   Its live
   assembled drop → Save → Undo → Redo → Save
   acceptance passed. This completion does not claim the separate saved-history
   Undo → Save cycle.
7. [Complete for same-tree existing widgets] Implement JVM-local widget-tree
   `MOVE`, catalog-authorized `ON_OR_INSERT` planning, exact latest-revision
   re-plan, stable-subtree preservation, one shared Save/Undo command and the
   optional amber `widget.movePreview.v1` Canvas projection. Ambiguous
   flattened-tree drops continue to fail closed.
8. [Explicit named-slot management complete for current definitions] Project
   every catalog slot into Properties and support transactional exact-slot Add,
   Move/reorder, Remove/Clear, explicit occupied-single Replace and list Clear
   All through one revision-bound command and the shared Save/Undo pipeline.
   Compound operations use `ReplaceSlotChild` and `ClearSlotChildren` with exact
   current-child fences and one chronological Undo/Redo step.
9. [Windows inline Text product slice complete; physical CJK acceptance open]
   Activate a real Flutter `TextField`/`TextInputClient` only for the selected
   existing Text, keep composition runner-local, and admit one capability-
   gated exact revision/layout/fence/selection-bound final commit into at most
   one `SetProperty(data)` command; unchanged text is a no-op. Deterministic
   Flutter and Java tests pass; the
   current physical host still lacks a composition-capable input method. This
   gate does not claim Linux/macOS or runtime-faithful Web support.
10. [Authenticated Web runner/transport, host, exact build/cache, internal Web
    session, assembled owner transition, stale-close Canvas recovery and
    Close-Mode-safe asynchronous close foundation complete; product gates
    pending] Keep the browser entry point, exact-view
    multi-view root and nonce-, sequence- and size-fenced JavaScript/Dart
    transport on the existing bounded NBFC/model protocol. The support-wide
    Close All batch and operation-owned, exact-proof Rename/Delete/Cut-Move
    continuations are complete. Next resolve the `New Tab Group`, `Collapse Tab
    Group`, public `Mode.dockInto()` and direct post-removal paths that can still
    change the heavyweight AWT hierarchy without the asynchronous permit. Then
    product-bind exact Web and pass Source/History/restart/runtime plus assembled
    Windows physical readiness, interaction and teardown. The standalone host,
    internal route and close handler do not authorize the product route.
11. Prove runner crash/restart/close, native-handle cleanup, pair Save and
    Undo/Redo behavior, then implement the Linux and macOS SPI providers.
12. Admit every further built-in only as a complete capability-gated vertical
    slice after all applicable gates pass and Palette expansion resumes.

## Remaining decisions for broader writable UI

The chronological model/source cursor, Pair-Save re-anchoring, Source-Save
durable-anchor overlay, targeted pre-persistence semantic invalidation and the
NetBeans 30 runtime/release matrix are complete as non-authorizing
infrastructure.

`Opacity` is now a supported, complete widget vertical slice under ADR-040. Its
required bounded alpha, optional semantics flag and optional child share one
closed catalog contract across schema/model validation, Properties, Create,
Canvas, DnD, deterministic Dart generation, Save/reopen and Undo/Redo. It adds
no schema or protocol version because every encoded value and slot shape already
exists.

`Align` is now a supported, complete widget vertical slice under ADR-041. Its
optional physical/directional alignment, two optional non-negative factors and
optional child share one closed catalog contract across schema/model validation,
Properties, Create, Canvas, DnD, deterministic Dart generation, Save/reopen and
Undo/Redo. It adds no schema or protocol version because all encoded value and
slot shapes already exist.

`FractionallySizedBox`, `Stack`, `Expanded`, `Flexible`, `Spacer`, `Image`, `TextField`, `ListView`,
`Wrap`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox` and
`OverflowBox` are complete under ADR-042 through ADR-055. FractionallySizedBox reuses
alignment/factor and
single-slot contracts. Stack adds a closed four-property non-positioned list
layout. Expanded and Flexible use the shared direct-Flex placement rule and
atomic wrapper command for their required children; Flexible adds the closed
loose/tight fit domain, and mutual wrapper nesting fails closed. Image reuses
the API-5 typed asset provider with pre-ID
inventory admission. TextField adds 54 reviewed scalar/preset leaves, atomic
radius/padding patches and the generated/Canvas constraint guard while
deliberately storing no runtime editable state. ListView adds the reviewed
static-children scrolling contract described above. Wrap adds the complete
nine-property run-layout contract and ordered children described above.
FittedBox adds the exact fit/alignment/clip contract, optional child, real
scaling/directional/clipping projection and zero-size Designer target.
ConstrainedBox adds the required finite/unbounded/expanding BoxConstraints
domain, optional child, real framework constraint projection and bounded empty
target. The earlier slices through FittedBox do not change a schema or protocol
version; ConstrainedBox advances `.fd` to v7, Catalog API to 6 and Canvas model
to v12 while leaving NBFC/control framing v1 unchanged. UnconstrainedBox adds
the complete direction/alignment/retained-axis/clip contract, optional child,
real framework projection and bounded empty target without changing those
versions. LimitedBox adds two canonical finite non-negative maximum rows,
optional child, real unbounded-axis-only constraint projection and bounded empty
target without changing those versions. OverflowBox adds alignment, four
finite non-negative constraint overrides, both fit modes, optional child and
real overflow projection without changing those versions. Flexible adds
non-negative flex, loose/tight fit, a required replacement-only child, direct
Row/Column placement and real ParentDataWidget projection without changing
those versions. Spacer adds positive flex, no slots, insertion-only direct
Row/Column placement and real surface-overlay Canvas instrumentation without
changing those versions. Baseline adds required finite baseline and exact
baseline type, an optional child, real framework projection and a bounded empty
selection/drop target without changing those versions. IntrinsicHeight adds a
property-free optional child, real speculative intrinsic-height projection and
a bounded empty target. IntrinsicWidth adds optional finite non-negative step
width and height rows, an optional child, real stepped intrinsic-width
   projection and the same non-layout-affecting empty target without changing
   those versions. Offstage adds one optional boolean, an optional child, real
   active-but-hidden framework projection and an external bounded zero-size
   selection/drop target without changing those versions. SizedOverflowBox adds
   the required atomic finite non-negative Size, optional alignment and child,
   real parent-constrained overflow layout and a bounded zero-size Designer
   target; it advances `.fd` to v8, Catalog API to 7 and Canvas model to v13.
   Transform adds required Matrix4, optional atomic signed finite Offset,
   alignment, hit-test and filter-quality values plus an optional child, and
   real paint-time transform/hit-test geometry; it advances `.fd` to v9,
   Catalog API to 8 and Canvas model to v14.

1. Pair-aware Save As, cross-directory Copy with explicit relative-URI rebasing,
   and the explicit conversion flow for an already modified or open Dart
   source. Same-folder pair Copy/Paste is implemented under ADR-022; same-project
   mirrored-folder pair Cut/Move and its fresh target lifecycle are implemented
   under ADR-023.
2. Structured property-editor/model contracts for contributed widgets and the
   remaining unreviewed Flutter graphs/callback shapes, plus localized
   presentation. Strict callback identifiers are implemented for Scaffold and
   ElevatedButton. The catalog-driven provider for the current thirty-four-widget
   writable-Properties slice now includes Scaffold scalars, ElevatedButton sparse
   state composites, AppBar composites, Text composites, a closed Paint subset,
   shadows, font features, font variations, IconData, semantic theme roles and
   Container's structured AlignmentGeometry/BoxConstraints/Matrix4/complete
   BoxDecoration and asset-only DecorationImage graphs, direct Image provider
   editing, Align/FractionallySizedBox alignment and factor editors, Stack,
   Expanded and Flexible layout fields, and TextField's closed presets, strict callbacks and
   atomic compound rows, plus ListView's closed scrolling/layout/cache/semantics/
   restoration rows and ordered children, plus Wrap's closed run-layout fields
   and ordered children, plus FittedBox's fit/alignment/clip fields and optional
   child, plus ConstrainedBox's required constraints and optional child, plus
   UnconstrainedBox's optional direction/alignment/axis/clip rows and child, plus
   LimitedBox's optional maximum rows and child, plus OverflowBox's alignment,
   constraint-override, fit and child contract, plus Flexible's direct-Flex
   wrapper, non-negative flex and loose/tight fit contract, plus Spacer's
   positive-flex childless direct-Flex insertion contract, plus Baseline's
   required finite position, exact baseline type and optional child, plus
   IntrinsicHeight's property-free optional child, IntrinsicWidth's optional
   non-negative step width, step height and child, and Offstage's optional
   boolean plus child, SizedOverflowBox's required atomic finite non-negative
   size, optional alignment and optional child, and Transform's required matrix,
   optional atomic origin, alignment, hit-test behavior, filter quality and
   child; built-in
   domain metadata is fixed by ADR-010 and ADR-027.
3. Callback stub creation without modifying user-owned code on later saves.
4. The platform-neutral native-surface SPI, Linux/macOS isolated-runner
   feasibility, remaining Windows native lifecycle acceptance, a future
   versioned catalog contract and Java → Flutter hit-test → revision-bound DnD
   intent validation required by ADR-021. The bounded protocol-v14 capability-gated model payload,
   direct native rendering, stable-ID selection bridge and bounded typed
   Properties path are already implemented. The Windows inline Text product
   slice is also implemented under ADR-036, while physical CJK IME acceptance
   remains open. The authenticated browser runner/transport foundation, offline
   static-build proof, native WebView2 host, and origin/resource confinement are
   complete. The internal exact Web session, routed factory and asynchronous
   owner-transition coordinator are also implemented and deterministically
   tested. The coordinator now owns assembled MultiView component replacement,
   epoch fencing, poisoned-transition Retry and fresh-generation Canvas rebuild
   after an abandoned stale close. The dedicated shell permanently disables
   stock `Close Mode` and owns the permit-aware Close action. One exact-topology,
   exact-revision Close All batch now asks Save/Discard/Cancel once and retires
   clone-local Canvases sequentially. Pair-aware Rename, Delete and Cut-Move now
   retain that reservation through one exact-proof, off-EDT continuation and
   re-enter the existing synchronous operation under the matching pair lease.
   Production routing remains off because `New Tab Group`, `Collapse Tab Group`,
   public `Mode.dockInto()` and direct post-removal paths are not yet covered by
   one asynchronous permit. Exact-Web product binding,
   Source/History/restart/runtime parity and the physical gate also remain
   pending alongside the Linux/macOS native-surface providers.

These decisions must be resolved with focused prototypes and tests; they do
not weaken the accepted `.fd` canonical-model and guarded-Dart-region rule.
