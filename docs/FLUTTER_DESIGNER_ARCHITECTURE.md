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
- a central canvas with synchronized selection, semantic drop targets, zoom,
  device bounds and layout guides;
- standard NetBeans Save, Undo/Redo, Copy/Paste and Delete actions.

Flutter layout is not an absolute-position form. A drop operation selects a
named constructor slot such as `body`, `child`, `children`, `appBar` or
`floatingActionButton`. Row and Column drops select an insertion index. Stack
may additionally expose `Positioned` semantics. The persisted model never
stores incidental canvas coordinates as Flutter layout.

## Paired files and ownership

A designer form is a same-directory, same-basename pair:

```text
home_page.fd       canonical visual model, JSON
home_page.dart     user source plus designer-managed regions
```

The `.fd` file is the source of truth for the visual subtree. The `.dart` file
is the source of truth for all code outside designer-managed regions. A Dart
file without a matching `.fd` file is an ordinary Dart file and is never
claimed by the designer.

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

1. Both files are local project files in the same directory.
2. Their basenames match exactly, including case where the filesystem exposes
   case-sensitive names.
3. The `.fd` `source.dartFile` value names that exact sibling and contains no
   path separator.
4. The declared Dart class and managed-region markers occur exactly once.
5. Every managed-region hash matches the current Dart payload.

If the Dart file is missing, the designer may offer an explicit regeneration
from `.fd`. It does not regenerate automatically during project scanning.

### NetBeans 30 file and editor integration

The RELEASE300 integration uses the same technical arrangement as Matisse:
the paired `.dart` is the primary `MultiDataObject` entry and `.fd` is its
secondary entry. This does not change semantic ownership: `.fd` remains the
canonical visual model. The arrangement lets the Source view use one real Dart
`DataEditorSupport`, Dart EditorKit and LSP document instead of a copied editor
surface.

The pair-aware loader is registered for both `text/x-dart` and
`text/x-flutter-designer`, but returns a primary file only when both exact
same-basename siblings exist. Opening either member therefore resolves to the
same data object. An orphan Dart file remains an ordinary Dart file, and an
orphan `.fd` remains unclaimed by the designer. An unmodified cached ordinary
Dart object is safely revalidated when its `.fd` sibling appears. New-form
creation must still create both files in one filesystem atomic action before
the first `DataObject.find`; conversion of an already modified or open Dart
document requires an explicit later workflow and must never force
invalidation.

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
without losing either marker, and that adding a sidecar does not discard an
already unsaved ordinary Dart buffer.

Dart-only Save As is intentionally not exposed because it would leave the
paired `.fd` behind. Rename, Copy and Move are also disabled in this foundation
slice: NetBeans may select a collision suffix, which must be reflected in
`source.dartFile` transactionally. Pair-aware implementations are required
before those actions are enabled. Delete remains available and deletes both
registered entries. An explicit Source save is routed through the one stable
`SaveCookie` owned by `PairSaveCoordinator`. With no staged pair it delegates
to the normal editor serialization lifecycle through an exact paired-baseline
transaction; while a preparation lease is active, Source-only Save is blocked.

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

`.fd` is UTF-8 JSON and conforms to
[`flutter-designer/fd-v1.schema.json`](flutter-designer/fd-v1.schema.json).
The stable format name is `netbeans-flutter-designer`, and version 1 uses an
integer `schemaVersion` so migrations are explicit.

The checked-in [`home_page.fd`](flutter-designer/examples/home_page.fd) and
[`home_page.dart`](flutter-designer/examples/home_page.dart) pair is the first
executable contract example. Future codecs, hash checks and generators use it
as a golden fixture rather than maintaining a separate undocumented example.

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

Version 1 represents an asset value as one deterministically escaped Dart
string literal containing its path; it does not implicitly wrap the path in
`AssetImage`, `Image.asset` or another Flutter object. An opaque Dart
expression remains an exact, loadable model value and is never parsed,
evaluated or prefix-rewritten. The current `fd-dart-regions-v1` generator fails
closed when such a value is present: copying arbitrary expression text into a
managed region is postponed until the analyzer can validate the same bounded
document revision.

Unknown top-level fields are rejected. Vendor or experimental data belongs in
the namespaced `extensions` object. A document with a newer unsupported schema
opens read-only and is never down-saved. Migrations run in memory, retain the
original bytes until an explicit Save, and have golden before/after tests.

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

Canonical version 1 output is UTF-8 without BOM, uses two-space indentation and
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
- future undoable commands for add, remove, move, wrap and property changes;
- conflict detection inputs and normalized region hashing.

`netbeans-plugin` owns only the NetBeans edge:

- `.fd` MIME resolution, loader/DataObject and paired-file operations;
- the `Design`/`Source` MultiView elements;
- Palette, Explorer/Nodes, Properties and Visual Library adapters;
- the single `PairSaveCoordinator`-owned `SaveCookie`, file listeners, guarded
  sections, the current source-Undo barrier and the future combined
  `UndoRedo` bridge;
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

The version 1 contributor SPI requires a reverse-DNS contributor id, catalog
`API_VERSION == 1`, and every contributed widget type id to start with
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
retain semantic order, omitted arguments remain omitted, and explicit empty
single slots emit `null`. Opaque Dart expressions and unsupported source kinds
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
pairing problems. A weak listener on the secondary `.fd` entry reloads an
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
API must evolve independently after version 1, it moves to a dedicated SPI NBM
or new package boundary instead of widening or silently breaking this module's
public surface. The export and `API_VERSION == 1` are provisionally frozen for
0.1.3-compatible patch builds; they are not yet a permanent 1.0 compatibility
promise. A runtime fixture NBM verifies the specification dependency, default
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

## Command and Undo/Redo foundation — UI not yet enabled

The pure bounded command layer implements the following immutable commands;
they are not current UI capabilities.

UI components never mutate widget collections directly. Each edit is a
validated command with an inverse:

- `AddWidget`
- `RemoveWidget`
- `MoveWidget`
- `WrapWidget`
- `SetProperty`
- `ResetProperty`

`RenameDesignerClass` is deferred because the class declaration is
user-owned Dart outside the guarded regions. It requires a separate LSP and
multi-file transaction contract rather than pretending to be a managed-region
edit.

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
templates. These paths remain disconnected from writable UI.

Copy/Paste serializes a versioned widget fragment, allocates new stable ids and
validates the destination slot before creating an undoable command. Delete
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
visual model would generate those bytes. Designer mutation and any
Design-initiated staged pair Save remain disabled. The implemented three-way
layer proves the bounded on-disk
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

This is deliberately not a writable Design surface yet. Scanner/generator
probes, the B→C analyze-before-apply transition, claimed staged C1→C2
replacement with exact rollback/fresh rebind across chained commands,
endpoint-specific command admission from retained physical history, durable
re-anchoring, exact `FD_ONLY` persistence, native semantic replay, saved-pair
savepoint re-anchoring, Source Save over saved semantic history, and one
identity-bound command/generator/analyzer capacity policy are implemented.
Remaining stop-ship work includes the recovery-only unprovable-authority
barrier, the full runtime/release matrix and the actual
Palette/tree/properties/Canvas mutation surface. Canvas work requires a
separate architecture discussion before its first implementation slice. Until
those contracts pass together, `PUBLIC_MUTATION_UI_ENABLED` remains `false`.

## Target NetBeans presentation

The writable designer will publish its selected widget as a NetBeans Node.
That one selection will drive the canvas highlight, widget tree and standard
Properties window. The Palette will be supplied through the active MultiView
element's Lookup, so the normal NetBeans Palette window becomes
context-sensitive.

The canvas will use the NetBeans Visual Library for selection, zoom, pan,
drag/drop feedback and semantic layout guides. Canvas widgets will be
projections of the domain model and never become the persisted model
themselves.

The first usable vertical slice contains `Scaffold`, `AppBar`, `Column`, `Row`,
`Padding`, `Center`, `Text`, `Icon`, `SizedBox` and `ElevatedButton`. It must
support create, open, edit, save, reopen, undo/redo and deterministic Dart
generation before additional widgets are added.

## Preview boundary

Pixel-accurate Flutter rendering is not part of the `.fd` ownership contract.
The initial canvas may be a semantic projection, but it must not pretend to be
pixel-accurate. A later preview runner will use the configured project Flutter
SDK, consume the same validated document/catalog, and keep process/tooling code
outside Swing event handlers. Embedding a native Flutter surface, streaming a
rendered surface, and selected-target preview remain open implementation
choices that require a separate ADR.

## Remaining decisions before writable UI implementation

1. Complete the recovery/runtime release matrix, including replacement of the
   recovery-only history discard used when staged authority is already
   unprovable before persistence. The chronological model/source cursor,
   Pair-Save re-anchoring and Source-Save durable-anchor overlay are already
   implemented as non-authorizing infrastructure.
2. Pair-aware Save As, rename/copy metadata updates, and the explicit
   conversion flow for an already modified or open Dart source.
3. The NetBeans property-editor provider SPI and localized presentation; the
   built-in domain metadata is now fixed by ADR-010.
4. Callback stub creation without modifying user-owned code on later saves.
5. The first semantic Canvas slice and its Palette/tree/properties interaction;
   agree its exact scope before implementation. Pixel-accurate preview
   transport and lifecycle remain a separate decision.

These decisions must be resolved with focused prototypes and tests; they do
not weaken the accepted `.fd` canonical-model and guarded-Dart-region rule.
