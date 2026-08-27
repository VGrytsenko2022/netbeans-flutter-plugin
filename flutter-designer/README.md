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
responsive render profiles and the bounded canonical six-widget `CORE_V1`
read-only model projection. It deliberately has no dependency on NetBeans APIs
or Swing.

The NetBeans adapter lives in `netbeans-plugin`. On Windows it now exposes the
read-only Design/status surface, Explorer widget tree, exact viewport/adaptive-
target preview toolbar and a real embedded native `FlutterView`, together with the
transactional pair-save edge. Stable widget IDs synchronize selection between
the tree and Flutter surface. A later writable slice will project commands into
the Palette and editable Properties. ADR-021 explicitly forbids implementing
the Canvas as Swing-painted widgets or a transferred PNG/JPEG/raw-pixel surface.
A planned platform SPI will generalize the Windows host to Linux and macOS; the
current provider keeps the Flutter engine/view in an isolated child runner.

This module owns only the NetBeans-independent host-issued presentation
identities and stale/replay admission rules, the explicit Mobile, Tablet,
Desktop and Web preview-mode identity, native-surface request validation, pure
backend contracts and per-MultiView lifecycle controller, plus the bounded
version 1 lifecycle control codec/session gate, process framing and canonical
`CORE_V1` model payload. That payload admits exactly `Scaffold`, `Column`,
`Row`, `Text`, `Padding` and `Center` from the reviewed built-in catalog and
excludes project paths, Dart source, callbacks, extensions and persistence
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
tests. The isolated runner decodes the canonical six-widget model, renders it
directly in Flutter for the compatible native adaptive targets, acknowledges the
exact layout identity and exchanges only revision-bound stable-ID selection.
Android/iOS/macOS/Linux appearance uses `ThemeData.platform` while the physical
host remains Windows. Web is an explicit pending browser backend, not a Windows
engine rendering relabeled as Web.
It receives no project paths, Dart source, file handles or
file/Save/Undo/Redo authority; Java remains the only command-admission and
persistence owner. The Java → Flutter hit-test → revision-bound DnD bridge is
not implemented.
`PUBLIC_MUTATION_UI_ENABLED` remains `false`.

Important version 1 semantics:

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
- schema-v1 `extensions` are location-independent opaque metadata and must not
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

These paths are not connected to writable UI. Pre-persistence loss of exact
staged authority now clears only semantic Designer state while retaining live
Source content and native Undo/Redo. The assembled NetBeans 30 runtime, strict
NBM verifier and isolated install lifecycle now pass. The accepted ADR-021
Windows native read-only `FlutterView`, six-widget projection, responsive
profiles, stable-ID tree selection, exact six-item context Palette and
selected-node read-only Properties are implemented. Editable Properties, the
runner/DnD mutation bridge, cross-platform providers and the
pair-aware writable workflow, property and callback contracts remain
outstanding;
`PUBLIC_MUTATION_UI_ENABLED` stays `false`.

The `.fd` document codec accepts strict UTF-8 JSON (with an optional input BOM), rejects
duplicates and trailing content, and keeps the exact bounded input snapshot.
Current version 1 data maps to the domain model; a completely parsed newer
version remains raw/read-only and cannot be down-saved. Canonical output is
UTF-8 without BOM, two-space/LF formatted, has one final LF, fixed core-field
order and lexically sorted dynamic keys. `$schema` is never fetched.

Catalog contributors use a reverse-DNS id, target catalog API version 1, and
own only widget type ids below `<contributorId>.`. Composition is atomic per
contributor: invalid metadata never partially enters the effective catalog.
Every effective Palette category id also has one stable category order;
conflicting contributors are rejected before type resolution.

The accepted contract and module boundaries are documented in
`docs/FLUTTER_DESIGNER_ARCHITECTURE.md`.
