# Architecture Decisions

Status note: ADR-024 and ADR-027 supersede the earlier provisional statements that
`PUBLIC_MUTATION_UI_ENABLED` remains `false`. Their persistence and lifecycle
contracts remain accepted. ADR-025 records the historical Text-only and later
six-source insertion milestones; ADR-030 records the subsequent seven-widget
`SizedBox` milestone, ADR-031 records the eight-widget `Icon` milestone,
ADR-032 records the nine-widget `AppBar` milestone, and ADR-033 records the
ten-widget `ElevatedButton` milestone and its 140 candidate Palette/DnD cells,
122 accepted and 18 rejected. ADR-034 extends exact named-slot management with
atomic replacement and clear-all commands. ADR-035 governs the writable
`Scaffold` slice. ADR-037 records the eleven-widget `AspectRatio` milestone.
ADR-038 records the twelve-widget `Container` milestone, and ADR-039 completes
its shared typed-asset branch. ADR-040 records the thirteen-widget `Opacity`
milestone; ADR-041 through ADR-045 add `Align`, `FractionallySizedBox`, `Stack`,
`Expanded` and `Image`, ADR-046 adds `TextField`, ADR-047 closes the agreed core
with `ListView`, ADR-048 begins the post-core surface with `Wrap`, ADR-049 adds
`FittedBox`, ADR-050 adds `ConstrainedBox`, ADR-051 adds `UnconstrainedBox`,
ADR-052 adds `LimitedBox`, ADR-053 adds `OverflowBox`, ADR-054 adds `Flexible`,
ADR-055 adds `Spacer`, ADR-056 adds `Baseline`, ADR-057 adds `IntrinsicHeight`,
ADR-058 adds `IntrinsicWidth`, ADR-059 adds `Offstage`, and ADR-060 establishes
the current `SizedOverflowBox` surface: 669 typed rows across thirty-three
widgets, twenty-eight const-constructor definitions and 1,056 Palette/DnD
candidates, including 908 accepted and 148 rejected cells. The 652-field
non-`Scaffold` total still
sits beside the 17 closed
scalar `Scaffold` fields. ADR-036
authorizes the Windows-only capability-gated inline
editor for one selected existing `Text.data`; its deterministic product slice
is accepted while physical CJK IME acceptance remains open. ADR-028 authorizes
same-tree movement of an existing non-root widget, and ADR-029 authorizes the
first exact named-slot management slice.
None authorizes cross-form movement, arbitrary native Canvas mutation,
unreviewed slots or Palette/DnD types outside the ADR-060 catalog.

## ADR-001 — IDE support before Designer

Accepted. A Matisse-like designer is built only after normal Flutter development works well in NetBeans.

## ADR-002 — Flutter/Dart SDK remains external

Accepted. The plugin discovers and validates a user-installed SDK instead of bundling one.

## ADR-003 — Process/tooling abstractions are NetBeans-independent

Accepted. SDK, analysis and run modules contain no NetBeans UI dependencies. This improves testability and keeps later IDE integrations thin.

## ADR-004 — Designer is a separate module

Accepted. The designer may consume stable services but may not become a prerequisite for standard Dart/Flutter editing and execution.

## ADR-005 — Dart semantic services use the NetBeans LSP client

Accepted. The plugin launches the SDK's Dart Language Server with standard LSP streams and registers a MIME-scoped NetBeans `LanguageServerProvider`. NetBeans owns ordinary protocol framing and editor adapters; the plugin owns SDK resolution, process isolation, project working directory, language-id mapping, and project-close termination. Narrow RELEASE300 compatibility transforms advertise the already-implemented `workspace.applyEdit` capability in the first outgoing `initialize` frame and adapt correlated, resolvable Dart completion items. The completion bridge stores the exact original `data` and `textEdit` JSON in a private Base64 envelope, hides the top-level edit, and restores both before Dart receives `completionItem/resolve`; Dart then contributes `additionalTextEdits` instead of recreating the main edit. The incoming compatibility stream also consumes only id-less custom notifications whose method is exactly `$/analyzerStatus`, because the generic NetBeans 30 client has no handler for them; messages with an id, malformed messages, diagnostics, and unrelated standard or custom traffic remain on the normal path. Unmatched frames remain byte-for-byte unchanged. Non-resolvable items retain their `textEdit`, although a frame containing a transformed sibling is reserialized. Dart's `resolved.command` path for some part-file and multi-file imports remains unsupported by NetBeans 30's standard `CompletionProviderImpl`; diagnostic Quick Fixes provide the supported fallback.

## ADR-006 — DevTools launches externally before embedding

Accepted. The plugin starts the DevTools version supplied by the configured Dart SDK, binds it to loopback on an automatically assigned port, connects it to the active Flutter session's VM Service, and opens it with the browser configured in NetBeans. The process remains project- and session-scoped with native Output, Progress, cancellation, and lifecycle cleanup. Reopening the existing server is preferred to launching duplicates. Embedding DevTools or implementing a native Flutter Inspector/widget tree remains a separate future milestone and is not a prerequisite for the external launcher.

## ADR-007 — Flutter projects own their auxiliary metadata storage

Accepted. Every loaded `FlutterProject` provides `AuxiliaryConfiguration` and `AuxiliaryProperties` instead of relying on NetBeans' generic per-fragment fallback attributes. Private fragments and properties are consolidated into one `FileObject` attribute named `dev.flutter.netbeans.projectMetadata` on the project root. Its name is deliberately slash-free, so NetBeans 30 `Ordering` does not mistake it for a relative folder-ordering rule, and it disappears with the project directory. Writes use NetBeans' transient-attribute convention so attribute-aware project copies do not inherit private IDE state. A `MoveOrRenameOperationImplementation` flushes private preferences before capturing the primary container and quarantine attributes into secure, bounded, explicitly typed, append-only handoff phases carried inside the project. `PREPARED` records the source URI, transaction UUID, and complete snapshot; `TARGET_READY` adds the verified display-name intent before rename; and `COMMITTED` records a fully restored and verified target. The handoff uses local real-path containment and bounded streaming XML parsing without DTDs or external entities. Source deletion precedes cleanup, while predecessor phases are removed before the newest phase, so every crash boundary retains the latest complete transaction. Project-open recovery runs under the write mutex before other project services. Verified target-ready work resumes idempotently, and committed recovery removes phase files without replaying the snapshot. An ambiguous target-side `PREPARED` phase or malformed/inconsistent handoff remains preserved and blocks Flutter services plus unsafe actions; Rename is the explicit resolution path when the target-name intent is missing. Rename records only a private NetBeans display name and does not mutate the Flutter package identifier. The separate shared store at `<project>/.netbeans/flutter-metadata.xml` is created only by an explicit shared write. After successful handoff recovery, the lifecycle hook copies legacy private fallback attributes into the consolidated private container and removes each legacy attribute only after successful persistence. Malformed legacy XML is retained in a private quarantine entry when it fits the bounded container; oversized XML and unexpected non-XML values are moved unchanged to verified slash-free transient quarantine attributes. This preserves recoverable open-file, bookmark, Flutter target-selection, and other project state without retaining a path-keyed private store in the NetBeans user directory.

## ADR-008 — `.fd` owns the visual model and guards generated Dart regions

Accepted. A Matisse-like Flutter form is a mirrored project pair: `lib/<relative>/<name>.dart` owns all code outside explicitly marked designer regions, while `.fd_templates/<relative>/<name>.fd` contains the versioned JSON canonical visual model. Schema v1 deliberately keeps `source.dartFile` as the exact Dart basename rather than a project-relative path; the mirrored roots and relative directory identify the physical pair. In the target writable workflow, `Design` edits the `.fd` model and `Source` edits the paired Dart file; at this ADR's acceptance the 0.1.3 Design surface remained read-only, and ADR-024 later authorizes its bounded typed Properties slice. The generator initially owns unique `imports` and `build` regions and stores a normalized SHA-256 for each region in `.fd`. A missing/duplicate marker, unsupported schema, filename/class mismatch, or changed managed payload enters an explicit source-conflict state and blocks automatic writes. The designer preserves user-owned bytes outside the markers, does not format the whole Dart file during a designer save, and never defaults a conflict dialog to overwrite source. The `Flutter Designer Form` New File wizard accepts only `lib` or its descendants and creates both mirrored entries before either is resolved as a DataObject. Details, schema and remaining integration decisions are recorded in [Flutter Designer Architecture](FLUTTER_DESIGNER_ARCHITECTURE.md).

## ADR-009 — Dart owns the editor session while `.fd` stays physically visible

Accepted. A complete mirrored pair has exactly one live designer editor owner: a Dart-primary `MultiDataObject` for `lib/.../*.dart`. Its custom `DataEditorSupport` preserves the Dart MIME type, EditorKit, LSP document, Save lifecycle and guarded sections while hosting `Design` and `Source` in a dedicated `text/x-flutter-designer` MultiView. The corresponding `.fd_templates/.../*.fd` is deliberately not registered as a secondary entry because NetBeans suppresses secondary files whose primary lives in another folder, making `.fd_templates` appear empty in the physical Files view. Instead, `.fd` has a separate visible, non-editing model DataObject whose Open action delegates to the single Dart-owned designer session. This is an IDE implementation detail: `.fd` remains the canonical visual model defined by ADR-008 and never receives a second live editor buffer. The pair-aware loader claims only complete Dart entries below `lib`; ordinary Dart editing is unchanged. An unmodified cached ordinary Dart DataObject may be safely re-recognized when the visible model appears, while a modified one is retained and opening the model reports the concrete conflict. Valid guard markers are recognized only as Dart line-comment tokens in the default lexical state, masked with equal-length spaces during editor load, and restored on save so Dart offsets remain stable. Ambiguous markers create no guards. If the live guard set no longer matches the loaded set, persistence restores the last known-good marker-bearing source and rejects the save rather than writing masked placeholders; normalized hashes remain the source-integrity boundary. Dart-only Save As and generic DataObject Copy/Move remain unavailable. Both the Dart editor owner and visible `.fd` model expose shared pair-aware Copy, Rename and Delete node operations, with no one-file variants; ADR-022 defines the deliberately narrow Copy/Paste contract. Rename accepts a canonical lower-snake-case basename, changes both mirrored paths and only `source.dartFile`, and deliberately preserves the exact Dart bytes and `source.className`; class refactoring remains a separate analyzer-backed operation. Rename and Delete close an open clean editor first, stage both paths under deterministic locks, verify their outcome and exact-byte roll back a pre-commit failure. Unsafe, read-only, conflicted or unsaved pairs fail closed. These are in-process rollback guarantees, not durable crash-recovery journals.

ADR-023 extends those paired nodes with private pair Cut/Paste while preserving
the same generic DataObject Move prohibition and fresh target-owner lifecycle.

## ADR-010 — The designer domain is typed, immutable, and catalog-driven

Accepted. `flutter-designer` represents schema version 1 with an immutable, NetBeans- and codec-independent domain model. Properties retain their exact wire kinds and arbitrary integers/decimals without primitive narrowing; named slots distinguish omission, explicit single-slot `null`, and populated children. The immutable catalog owns explicit Dart constructor/enum library bindings, accepted values, cross-target numeric-emission constraints, slot cardinality/traits, and creation-time property defaults for the reviewed first ten widgets. A creation default is persisted only by an explicit create-widget command and is never injected while loading; omission continues to request the Flutter constructor default. Built-in `flutter.*` ids are reserved, contributor composition is deterministic and never last-wins, and semantic validation is iterative, resource-bounded, and reports stable issue codes with exact model paths. JSON decoding, source-pair validation, generation, and NetBeans presentation remain separate stages around this domain boundary.

## ADR-011 — `.fd` decoding is bounded and future versions fail closed

Accepted. `flutter-designer` uses a locally constrained streaming JSON codec instead of a JSON tree or reflective data binding. It snapshots raw bytes before parsing, accepts only strict UTF-8 JSON, rejects duplicate names and trailing content, and applies independent limits to parsing, model mapping and canonical encoding. Exact format and mathematical-integer version dispatch happens only after a complete envelope pass. Version 1 produces the current typed model; a fully valid newer version exposes only its byte-identical read-only snapshot, while invalid input exposes bounded stable diagnostics. `$schema` remains non-resolved metadata. Canonical version 1 output has fixed core-field order, lexically sorted dynamic keys, bounded exact number spelling, UTF-8 without BOM, two-space indentation, LF line endings and one final LF. Codec currentness never grants writability. ADR-013 through ADR-019 add integrity, ordered analyzer/live evidence, transactional persistence, claimed staged replacement and one shared candidate-capacity policy, while the Design surface remains read-only until the chronological Source/model Undo boundary exists.

## ADR-012 — The 0.1.3 catalog contributor API is public but provisional

Accepted for 0.1.3. The main NetBeans module exports exactly
`dev.flutter.netbeans.designer.catalog` and
`dev.flutter.netbeans.designer.model`, because catalog metadata constructors
expose model identifier and value types in their public signatures. Contributor
NBMs use a normal specification dependency on
`dev.flutter.netbeans.netbeans.plugin`, register `WidgetCatalogContributor`
through the default Lookup, and reuse the host module's single packaged
`flutter-designer.jar`; an extension must never bundle another copy. Codec,
validation and NetBeans-edge packages remain private.

API 1 was frozen for 0.1.3-compatible patch builds. Direction-aware edge insets
established `API_VERSION == 2`, because adding a permitted subtype to the
exported sealed `PropertyValue` surface is source-incompatible; API-1
contributors are rejected explicitly rather than loaded under a changed
contract. Typed `IconDataValue` and `PropertyValueKind.ICON_DATA` established
`API_VERSION == 3`. ADR-038's structured `AlignmentGeometry`,
`BoxConstraints`, `Matrix4` and initial `BoxDecoration` kinds established
`API_VERSION == 4`. ADR-039 added exported `ImageProviderValue` and established
`API_VERSION == 5`. ADR-050 added finite-or-positive-infinite box-constraint
bounds and established `API_VERSION == 6`; ADR-060's atomic Size value
establishes the current `API_VERSION == 7`, and API-1 through API-6
contributors fail closed before their definitions are loaded. This is not yet a permanent 1.0
compatibility promise. Further incompatible evolution should move the SPI to a
dedicated module/new package boundary rather than silently breaking extensions
behind an existing API version.

## ADR-013 — On-disk declared integrity is read-only evidence, not a write gate

Accepted for the 0.1.3 foundation. After a current `.fd` model passes exact filename pairing, the shared clone-safe controller reads the paired Dart file through an independent 2 MiB bound and verifies a strict UTF-8 snapshot off the EDT. The smaller bound accounts for UTF-8 decoding, byte-offset indexing and normalized-hash allocation amplification inside the IDE heap. The NetBeans-independent scanner retains the exact Dart bytes and managed payload byte ranges, recognizes only canonical non-nested `imports` then `build` markers, checks their normalized uppercase SHA-256 values, and binds the current stateless contract to one unqualified top-level root class with top-level imports and a direct class-member build region. Complete `{}`, `()` and `[]` lexical scopes must balance; generic bounds cannot impersonate a class superclass, interpolation nesting is bounded, and a local class shadowing the required Flutter widget base is unsupported. Results are classified as `ON_DISK_DECLARED_MATCH`, `CONFLICT`, `UNSUPPORTED` or `UNAVAILABLE`; unsupported source shape takes aggregate precedence while any simultaneously known hash/topology conflicts remain visible as additional diagnostics. Stateful and import-prefixed bindings are explicitly unsupported rather than inferred. Both paired files trigger the existing coalesced reload lifecycle, while opening Design performs no write and never opens the Dart `StyledDocument`. `ON_DISK_DECLARED_MATCH` proves only `actual == declared`. ADR-014 through ADR-020 now supply the generated match, ordered analyzer/live proof, durable leases, save-time exact-baseline recheck, claimed replacement, shared capacity and chronological Source/model history downstream. None of those layers alone enables Designer mutation. The shared adversarial parity corpus and NetBeans 30 runtime/release matrix now pass; the visual-surface gates remain.

## ADR-014 — Deterministic generation and on-disk three-way match remain read-only

Accepted for 0.1.3. The NetBeans-independent `fd-dart-regions-v1` generator repeats semantic validation and atomically emits bounded canonical `imports` and `build` payloads only for the supported stateless model. Opaque Dart expressions fail closed and publish a typed model-path diagnostic rather than unchecked generated source. A separate pure gate revalidates exact source-region evidence, independently recomputes actual and generated normalized hashes, compares `actual == declared == generated` for both regions, reverse-splices the payloads into the exact bounded Dart baseline and submits the reconstructed candidate to the structural scanner again. The result is classified as `ON_DISK_THREE_WAY_MATCH`, `CONFLICT`, `UNSUPPORTED` or `UNAVAILABLE`, retains the source and generator evidence, performs no I/O and exposes no candidate or write capability. The three-way evaluation itself neither dirties the pair nor publishes a second Designer `SaveCookie`; ADR-016 separately installs the sole coordinator-owned `SaveCookie` for a dirty Source document or an internally staged pair Save. Even `ON_DISK_THREE_WAY_MATCH` is only on-disk evidence; ADR-017's ordered analysis/live proof and durable command lease, ADR-018's staged replacement and ADR-019's shared capacity still do not enable Designer mutation without the chronological Undo contract.

## ADR-015 — Prospective source, live document and analyzer proofs remain independent until pair Save

Accepted for the 0.1.3 foundation. A pure `DartSourceTransitionPlanner` now models the expected old-to-new managed-source transition separately from the read-only three-way gate. It accepts only a proven old three-way match, an exact marker-bearing live source snapshot, the matching old descriptor and internally consistent prospective generation; it preserves every byte outside the two payloads, updates both descriptor hashes, bounds candidate size before allocation and re-scans the complete result. Identical generated hashes publish `NO_CHANGES` without a candidate. The first prospective writable format is strict UTF-8, LF-only and BOM-free; other source formats remain readable but cannot enter this transition path.

The NetBeans edge separately reconstructs marker-bearing bytes from the equal-length marker placeholders in one exact `StyledDocument`. The immutable live snapshot binds object identity, `DocumentUtilities` version, exact UTF-8 hash, `SimpleSection` identities and UTF-16 character ranges. Its internal EDT-only atomic apply primitive replaces `build` before `imports` and verifies the exact planned candidate. On apply or finalizer failure it invokes `AtomicLockDocument.atomicUndo()` inside the same outer atomic section, then verifies byte-exact source plus both guard identities/ranges; this clears the pending compound edit and leaves the guarded provider's last-persisted fallback untouched. `dart-analysis` separately validates one path/version/SHA candidate in a no-disk-write native analyzer overlay with bounded diagnostics, timeout/cancellation and navigation probes. A probe can prove a unique real target under an expected real root and optional analyzer kind; native `analysis.getNavigation` does not independently expose the import/export URI graph. ADR-017 fixes their order: analyzer evidence is complete before the first live mutation, and the applied live revision is bound separately afterward.

Individually none of these objects authorizes persistence. The pair-save edge described by ADR-016, the ordering/command lease in ADR-017 and the claimed replacement in ADR-018 are the only boundaries allowed to combine them. ADR-019 supplies the shared capacity budget; Designer editing remains disabled until one chronological Source/model Undo transition is complete.

## ADR-016 — Pair persistence is exact and transactional, while Designer mutation stays gated

Accepted for the 0.1.3 foundation. A pure `DesignerPairPreparationPlanner` decodes the exact baseline `.fd`, proves the old source descriptor and stable `documentId`, canonicalizes the prospective model, and requires deterministic decode/encode parity before publishing an immutable `PreparedDesignerPair`. A package-private evidence gate binds that same loaded `Current` identity and both exact baselines to one unique analyzer-overlay request for the exact prospective bytes, hash, real project/Dart paths and complete ordered probe manifest. The manifest includes the scanner-owned unqualified top-level `StatelessWidget` superclass occurrence plus every generator-owned Flutter occurrence. Passing analyzer evidence is consumed before live mutation; an exact post-CAS `StyledDocument` identity/version/SHA is bound separately. Unique navigation targets must remain below the configured real Flutter SDK root. The SDK root deliberately includes both `packages/flutter/lib` and re-exported `dart:ui` declarations under `bin/cache/pkg/sky_engine`; no parent directory is trusted. A prepared pair, ticket or analyzer result alone is never write authority.

One `PairSaveCoordinator` and one stable `SaveCookie` own ordinary Source saves, preparation and staged pair saves. A preparation lease is acquired before analysis and blocks Source-only Save throughout the ticket lifetime. Rejected, cancelled, replayed or stale analysis releases the lease without applying candidate C. Passing evidence enters one EDT task that compares predecessor B, applies and verifies C, and publishes the separately bound live evidence without an event-loop interleave; any later failure restores B before releasing the lease. The guarded writer advances its last-persisted marker-bearing fallback only after the real output succeeds; virtual serialization and aborted saves retain the previous fallback. The editor environment does not retain a Dart lock for the whole edit session, so pair Save can acquire both local files in stable canonical-path order. Under both locks, `PairFileTransaction` re-reads and byte-compares the Dart and `.fd` baselines, writes Dart then `.fd` in one owned NetBeans atomic event group, re-reads both results, and on failure restores `.fd` then Dart and verifies the rollback. Only events carrying that exact atomic-action identity are suppressed; any other paired-file event during preparation, staging or Save becomes an explicit conflict. A durable pair commit is never exposed as retryable against the old baseline. If guarded-editor finalization then fails, the provider adopts the committed revision only after one EDT/document-atomic proof that the live document is clean and serializes byte-exactly to the committed Dart. A dirty or mismatching live revision is preserved unchanged in sticky `RECOVERY_CONFLICT` with the stable `SaveCookie`; recovery never reloads it implicitly.

This installs the fail-closed persistence edge, not the visual editing feature. At acceptance, scanner/generator probes and pure command history existed, while the chronological command/source Undo/Redo boundary did not and the temporary foundation discarded Source Undo history after a durable paired commit. ADR-018 later added staged C1→C2 replacement, ADR-019 unified candidate capacity, and ADR-020 replaced that temporary barrier with native chronological replay plus Pair/Source Save re-anchoring. The runtime/release matrix now validates those internal contracts, but the Design surface remains read-only until its separate visual-surface contracts are accepted.

## ADR-017 — Analyze-before-apply and durable command re-anchoring precede writable UI

Accepted for the 0.1.3 foundation. `PairCandidateAnalysisTicket` is an atomic one-shot capability with a process-wide monotonic non-repeating overlay version. It retains the exact loaded/prepared identities and request; cancellation, replay, transfer to another preparation, result snapshot substitution, incomplete probes and untrusted paths all fail closed. The coordinator derives the real Dart path from its bound `FileObject`, the trust root from the configured Flutter SDK, and re-reads both disk baselines across a fixed external-event epoch before reserving the lease. Analyzer rejection changes neither the live document revision nor modified/Undo state. Only a passing analyzed token may enter the coordinator's single EDT predecessor compare-and-set and applied-live binding. A failure after observed apply restores the exact predecessor and releases the lease under one document-atomic barrier, but remains dirty with the stable `SaveCookie` until ordinary Source Save; recovery never reloads over a queued edit.

Source and Design expose one stable DataObject-owned combined Undo/Redo identity while the native NetBeans editor manager remains the underlying source history. Internal apply, restore, exact verification and post-commit Undo barriers defer and coalesce outward presentation events until the document lock is released. Publication ownership is claimed before a callback and a reentrant Designer document transaction is rejected before it can acquire the document lock; an edit queued after the completed barrier is retained and remains undoable. Presentation-listener failure, including `Error`, is logged without escaping or retroactively invalidating completed document work. Undo/Redo pins the exact active delegate until the action returns: a concurrent bind fails closed, while close is deferred until the action or internal document deferral completes.

`DesignerCommandSessionOrchestrator` pins one exact dirty cursor with a precomputed
post-commit anchor before I/O. While its durable lease is active, commands,
Undo/Redo and another lease are blocked. An `FD_ONLY` revision must keep
byte-exact Dart, change canonical `.fd`, retain the exact loaded immutable
catalog identity, and start from complete writable validation/source/three-way
facts; its internal synchronous transaction locks and verifies both files but
writes exactly one `.fd` participant. The live Dart document identity, version,
bytes, modified flag and Source Undo presentation must remain unchanged. A
verified commit adopts the precomputed anchor; a pre-write or verified-rollback
failure aborts the lease for retry; stale, partial or otherwise uncertain
outcomes invalidate and close the unsafe command session. No `SaveCookie`
exposes this internal path directly; ADR-028's bounded public tree Move is its
first mutation caller when generated Dart bytes compare exactly equal.

The command-side half of a chained C1→C2 transition is now explicit. Applying a pure command produces an identity-bound pending lease while the orchestrator retains the exact C1 session, revision and redo branch. The lease binds that predecessor to the exact prospective C2 session, revision, edit and immutable catalog identity, and keeps commands, Undo/Redo and durable Save blocked for its complete lifetime. A rejected command publishes no lease. The caller may adopt C2 only after a separate boundary has replaced and verified the exact staged/live pair, abort while C1 remains authoritative, or invalidate and close an uncertain session. Adoption offers a monitor-only identity swap whose one-shot listener/binding effects are published only after the caller releases document/coordinator locks. The lease performs no analyzer, editor, coordinator or filesystem operation and is not write authority.

This decision does not enable the Designer UI. ADR-018 supplies coordinator-owned analyzed replacement and exact rollback/fresh evidence rebind, ADR-019 supplies the unified command/generator/analyzer capacity budget, and ADR-020 later supplies the chronological Source/model cursor, Save re-anchoring and targeted recovery. The runtime/release matrix now passes, but the visual-surface gate remains; `PUBLIC_MUTATION_UI_ENABLED` stays false.

## ADR-018 — Staged C1→C2 replacement is one claimed document/pair/command transaction

Accepted for the 0.1.3 internal foundation. `PairSaveCoordinator` accepts only an exact staged `PAIRED` C1 evidence identity and the exact adjacent pending command candidate C2. Under the coordinator monitor it claims that command capability before deriving the pure live transition, so ordinary adopt, abort, invalidate and close cannot race the replacement. C1 and C2 must retain the same durable decoded `.fd`, Dart three-way and immutable catalog identities, while the prospective prepared pair and source transition must be the exact command-owned candidates. The reservation is registered before disk/live preflight and fixes one external-event epoch; analysis still completes before the first document mutation.

Passing analysis enters one EDT/document-atomic edge. The guarded document apply verifies the exact C2 bytes and its finalizer binds applied-live evidence while the coordinator and claimed command cursor jointly move to C2. Outward command-binding effects and pair/`SaveCookie` effects share one deferred queue after document/coordinator locks are released; command binding closes or changes before pair listeners run, so a NetBeans callback cannot observe half of the joint outcome. Analyzer rejection and an untouched close abort the claim and retain identity-exact C1. If apply mutates and fails, the exact regions are restored and a new C1 live-evidence identity is bound before the command returns to its predecessor. If a user edit, external event or fatal callback prevents proof of exact C1 or C2, user content is retained, staged authority is cleared, the command session is invalidated and the pair enters a sticky conflict.

This decision remains internal and does not enable Designer mutation. ADR-019 supplies the unified command/generator/analyzer capacity budget; ADR-020 later replaces the temporary post-commit Source-history barrier with one chronological Source/model cursor and exact Save re-anchoring. The runtime/release matrix now passes; `PUBLIC_MUTATION_UI_ENABLED` remains false while the visual-surface gate is open.

## ADR-019 — Candidate capacity is one identity from command generation through analyzer admission

Accepted for the 0.1.3 internal foundation. `DartCandidateCapacityBudget` lives in `flutter-core-api` so `flutter-designer`, `dart-analysis` and the NetBeans edge consume one object without introducing a designer/analysis dependency cycle. The default `fd-dart-candidate-capacity-v1` identity bounds the complete prospective Dart candidate to 2 MiB and the analyzer request to 256 symbol probes, reserving exactly one probe for the scanner-owned top-level `StatelessWidget` superclass. A detached object with equal field values is a different policy and fails closed before an analyzer process starts.

`DartGenerationLimits` retains that exact policy. Generation publishes it with `GeneratedDartRegions`, counts every generated symbol occurrence conservatively, and fails atomically before regions exist when the generated allowance is exceeded. `DesignerCommandLimits` requires its source scanner bound not to exceed the same candidate-byte bound. A valid semantic command that crosses the probe boundary returns `LIMIT_EXCEEDED` with `SYMBOL_PROBE_CAPACITY_LIMIT`, keeps the exact input session and redo branch, and publishes neither an edit nor a candidate revision. Every successful `DesignerCommandRevision` exposes the generation-owned policy identity and rejects a Dart candidate larger than it.

The NetBeans probe planner reads the policy from the exact prepared transition and no longer owns a separate `MAX_PROBES`. `PairSaveEvidenceGate` rechecks candidate bytes, transfers the same identity into `DartCandidateAnalysisRequest`, and `PairCandidateAnalysisTicket` rejects identity substitution. `DartCandidateAnalysisLimits` embeds the policy rather than duplicating byte/probe scalars; `DartCandidateAnalyzer` rejects a foreign policy before process creation and retains byte/probe checks as defensive admission. Boundary tests prove 255 generated plus one scanner probe succeeds, 256 generated plus one scanner probe produces no regions, exact command capacity succeeds, over-capacity command publication preserves the session/redo identity, exact analyzer limits reach the process, and byte/probe/foreign-policy violations do not.

This contract is capacity evidence, not persistence or mutation authority. It closes the late-staging unsaveable-command gap but does not enable the Designer UI. ADR-020 later supplies the chronological Source/model cursor and exact Pair/Source Save re-anchoring. The runtime/release matrix now passes, but the visual-surface gate remains, so `PUBLIC_MUTATION_UI_ENABLED` stays false.

## ADR-020 — Source and Designer share one native chronological Undo/Redo timeline

Accepted for implementation in 0.1.3, with writable Designer UI still gated. The paired Dart `CloneableEditorSupport` keeps NetBeans 30's default package-private `UndoRedoManager`; the plugin does not replace or wrap that manager because NetBeans owns its document locking, commit groups, on-save actions and savepoint bookkeeping. `Source` and `Design` expose that one stable native history. A Designer command session may bind lifecycle and replay authority, but it is never selected as a competing Undo delegate.

An ordinary user Dart edit remains an ordinary native Source entry. One successfully published visual command contributes exactly one non-mergeable semantic entry at its chronological position. The generated managed-region remove/insert operations are backend details of that entry and must never appear as separately selectable Source edits. The plugin registers NetBeans 30's public MIME-scoped `UndoableEditWrapper` SPI for Dart and leaves the `BaseDocument` listener array unchanged. An EDT-only, editor-owned capture token is attached by identity to the exact document before the outer atomic mutation; the wrapper recognizes only that token and replaces the closed outer `AtomicCompoundEdit` with one semantic entry immediately before the unchanged native manager receives it. A one-shot native-manager change acknowledgement performs the already-prepared no-throw joint model/pair commit, after which outward callbacks leave the lock barrier. A failed mutation invokes `AtomicLockDocument.atomicUndo()` before outer unlock, verifies the exact predecessor and aborts the token, so the native manager receives no phantom forward-plus-rollback event. `.fd`-only commands use a semantic entry with no Dart delegate and must leave the Source document cursor unchanged.

Every model move is a prepared identity-bound transition of kind `APPLY`, `UNDO` or `REDO`. Preparation derives the exact adjacent target session without moving the authoritative cursor. For a paired transition, analyzer proof and exact live predecessor checks finish before mutation; after candidate verification, a no-throw monitor-only joint commit is armed. The native semantic edit is admitted before that commit makes the candidate authoritative. Undo and Redo perform the inverse order through the same entry: preflight the exact current pair/session, replay the captured Dart edit under NetBeans' document lock, bind a fresh live identity to the retained analyzer evidence, and only then adopt the exact target command cursor and staged authority. Outward command, pair, `SaveCookie` and Undo-presentation callbacks are queued until every document, native-history and coordinator lock has been released.

Chronology, not view ownership, selects the next operation. Thus `S1 → M1 → S2` undoes as `S2`, `M1`, `S1` and redoes as `S1`, `M1`, `S2`; a new Source or Designer edit after Undo truncates the single shared redo suffix. Stable logical revision ids and command-edge data identify timeline entries because durable `markSaved()` re-anchors immutable revision objects. A Source edit above a model entry remains user-owned bytes; the next visual command must derive from and preserve the exact current live Source baseline rather than silently regenerating from an older durable object identity.

Pair Save adopts the exact command session's durable-save lease and marks the current native position as the saved pair without clearing history. Undo away from that saved tuple makes the pair dirty; Redo back to the same tuple makes it clean again. A newer Source entry above a durably committed model entry remains dirty and is never discarded. A failure proved before entering `CloneableEditorSupport.saveDocument()` may remain retryable. Once that call begins, however, NetBeans 30 moves its private `UndoRedoManager` savepoint before requesting the output stream and exposes no public rollback for that marker. Therefore every non-committed outcome after entry, including a verified zero-write or fully restored filesystem result, clears staged authority and invalidates the command session in a sticky conflict while preserving the native history exactly as CES left it. Partial writes, unknown native-edit admission, missing manager acknowledgement, failed exact replay or unrecoverable callback ordering use the same fail-closed rule instead of calling `discardAllEdits()` or guessing which side won.

If that first admitted model edit migrated a raw schema-v1 `.fd` baseline to a
canonical schema-v2 command revision, Pair Save re-anchors retained semantic
endpoints from the proven canonical v2 revision, not from the raw v1 bytes.
Exact historical Dart envelopes remain unchanged, but Undo cannot resurrect an
old model encoding or create a physical variant that is incompatible with the
new durable anchor.

The saved-history slice now proves both `B→C1→Save` and the complete `B→C1→C2→Save` chain. Two semantic Undo operations reach `C1` then former durable `B`; two Redo operations return through `C1` to the exact clean `C2` savepoint while durable `C2` bytes remain unchanged. Re-anchored analyzed endpoints use analyzer-free `REANCHORED_ANALYZED` proof, former durable endpoints use `FORMER_DURABLE`, and neither fabricates fresh `PairSaveEvidence`. An unmanaged `S2` entry can remain chronologically above a saved model edge, including an edit racing after durable output. Successful Pair Save verifies live content without mutating native history, adopts a synchronously derived saved `Current`, and publishes controller, command and Pair effects in EDT order. A stale controller generation or other post-commit split-authority risk invalidates the command session and enters conflict instead of partially adopting it.

Ordinary Source Save over that retained semantic graph uses a separate identity-bound `SourceAnchorLease`, acquired before entering CES. The lease pins the exact saved `BASELINE`, prospective Source bytes and stable logical revision, and blocks commands, Undo/Redo, another durable lease and another Source anchor until explicit adoption, abort or fail-closed invalidation. Preflight captures the exact virtual CES serialization without advancing its private savepoint. Re-anchoring is allowed only when both managed payloads are byte-for-byte identical to the saved semantic revision; equal normalized hashes are deliberately insufficient. After an exact committed output, `S2` becomes the durable command `BASELINE`, while every retained historical candidate is re-derived against `S2` using its exact retained live-source template. Stable logical revision ids remain stable, but all proof and immutable revision identities are rebuilt from the new durable anchor.

The saved semantic endpoint is then represented as a native-history overlay: its command revision and durable baseline are `S2`, while its native timeline bytes below the Source edit remain `C1`. The resulting order is exact: clean `S2` → native Undo to dirty `C1` → semantic Undo to staged `B` → semantic Redo to dirty `C1` → native Redo to clean `S2`. A later unmanaged `S3` observed after durable output remains user-owned and dirty above that graph when its managed payloads are still exact. A later Designer `C2` and Pair Save preserve both axes: one stable semantic revision may have separate `S2` and `S0` physical endpoints, so the exact order is `(C2,S2)→(C1,S2)→(C1,S0)→(B,S0)` and back. Each endpoint owns an exact pair proof; re-derivation first projects the new durable managed payloads into the historical unmanaged envelope by scanner-owned byte offsets, then derives the historical semantic candidate. A following Source Save to `S3` performs that endpoint-specific projection again and preserves `C2/S3→C2/S2→C1/S2→C1/S0→B/S0` and its reverse. Endpoint identities, not revision ids alone, select these variants. All unique physical Dart-plus-`.fd` candidates count incrementally against the command-history byte budget so over-budget history fails before CES or the pair transaction and before constructing the next projection. Successful adoption is one identity-checked controller/command/Pair operation: the controller adoption ticket, `SourceAnchorLease`, disk baseline, edge graph and cursor are all verified before controller effects, command effects and Pair effects publish in that order. `UNCHANGED` Source Save is a successful no-op for semantic/model identities: only the exact clean live cursor/savepoint is refreshed, while close or authority races fail closed. Once CES has been entered, every failed result invalidates semantic authority, clears the semantic edge/cursor and enters sticky conflict without discarding or rewriting native Source history. A committed output followed by stale controller generation, owner close or any other split-authority risk retains provable durable bytes but applies the same fail-closed semantic invalidation instead of partial adoption. Trimming the last native semantic edit retains its command owner/cursor long enough for the next Source Save to re-anchor it. If that zero-edge owner closes first, the coordinator retires it only after exact baseline/controller checks and proceeds with an ordinary Source Save while preserving native Undo history. A close racing between owner selection and `SourceAnchorLease` acquisition is rechecked at the failed pin edge; after the same exact retirement proof the coordinator re-enters the ordinary path and performs one CES Save.

Endpoint-specific command admission from saved physical history is now accepted as the final pre-Canvas chronology slice. The coordinator captures an opaque `StagedCommandSource` token which fixes the exact logical owner/revision, physical cursor and `SavedHistoryProof`, live-document evidence, external-event epoch and coordinator epoch without granting replacement authority. Reservation rechecks those identities after pure derivation; a native move, proof rebind or epoch change makes the token stale. The token also retains NetBeans' monotonic live-document version, so an edit followed by an exact byte revert cannot cross the reservation boundary as an ABA-equivalent revision or publish `PREPARING_REPLACEMENT`. At `(C1,S0)`, the token supplies the exact endpoint-specific `PreparedDesignerPair`: its live template contains the durable C2 managed payloads projected into the S0 unmanaged envelope, and its prospective bytes are exact C1/S0. `DesignerCommandSession.applyFromPhysicalEndpoint` derives C3/S0 with the session's immutable catalog, limits and shared capacity identity. The pending command lease retains that exact physical predecessor pair while the logical C1 cursor and stable revision id remain authoritative until the existing joint analyzer/document/pair/command adoption.

Staged replacement and its recovery path operate on the generalized `StagedPairProof` contract rather than requiring fresh predecessor analyzer evidence. An immutable `SavedHistoryProof` can therefore be replaced, rejected, cancelled or recovered without being converted into fabricated `PairSaveEvidence`; only the new command candidate receives a new analyzer ticket and result. Canonical-pair substitution is rejected without document mutation and the same captured physical source may retry while its identities remain current. A failed semantic apply uses the MIME capture's atomic rollback, rebinds a fresh exact predecessor proof and preserves the older native semantic graph instead of calling `discardAllEdits()`. The new physical candidate is counted with all retained endpoint variants against the aggregate Dart-plus-`.fd` history budget before analyzer admission or CES mutation. Successful adoption keeps durable C2/S2 unchanged, truncates the obsolete S2/C2 redo suffix and produces the exact branch `B/S0→C1/S0→C3/S0`; Undo/Redo restores the endpoint-specific proofs. The adopted paired revision retains its S0 live template, so a following ordinary command continues as C4/S0 instead of silently reverting to S2.

When staged authority becomes unprovable before persistence, recovery now clears the exact semantic edge graph and invalidates its durable command lease without touching the native manager. Newer user-owned Source content, its Undo/Redo cursor, dirty state and stable `SaveCookie` remain available in sticky conflict; exact candidate bytes cannot revive the revoked authority. The branch/runtime matrix, strict NBM verification and isolated NetBeans 30 install lifecycle now pass. The Palette/tree/properties/Canvas mutation surface remains a separate staged slice governed by ADR-021; `PUBLIC_MUTATION_UI_ENABLED` therefore remains `false`.

## ADR-021 — The Canvas is an embedded native FlutterView, not a Swing raster proxy

Accepted for staged implementation in 0.1.3. The broad public Designer mutation
surface remains gated, while ADR-024 and ADR-025 later enable their two exact
reviewed slices. The active Design MultiView supplies the standard
context-sensitive NetBeans Palette. The visual widget tree is published through
Explorer/Nodes, and the selected revision-bound widget Node drives the standard
Properties window. These surfaces and the MultiView chrome remain NetBeans
Swing UI.

The central Canvas is a real native `FlutterView` embedded in that chrome. Its
Flutter engine paints the widget tree, scale-stable dashed widget outlines, a
solid selected-widget outline, direction-resolved `Padding` distance guides,
drop zones and layout guides directly into the native surface and performs the
authoritative widget hit test. These overlays are paint-only and cannot change
layout or hit geometry. A Swing component, the NetBeans Visual Library, a PNG/JPEG/raw-RGBA
transfer, screenshots or periodic image copies must not stand in for the
Canvas. Swing may own the surrounding chrome and a heavyweight native host peer;
it must not paint a second approximation of Flutter layout. Runtime widget and
overlay state remain disposable projections. The exact validated immutable
`.fd` revision and catalog remain the canonical visual model.

Native hosting is hidden behind a platform SPI. The SPI owns attach, detach,
resize, device-pixel-ratio changes, visibility, focus, liveness and final
surface destruction without exposing platform handles to the domain core. The
first implementation target is Windows: a Flutter desktop runner creates a
native child surface and the Windows provider embeds that surface into the
NetBeans Canvas host. Linux and macOS providers must implement the same
lifecycle and fencing contract before their platforms are supported. The first
Windows vertical spike now implements that actual child surface inside the
Design MultiView, not merely a source scaffold; it is not yet the completed
cross-platform SPI or broad public Designer surface.

The preferred deployment keeps the Flutter engine and view in an isolated
runner process when the platform can safely embed and supervise its child
surface. Process isolation is crash containment, not by itself an operating
system sandbox. The runner receives bounded canonical allowlisted model values
and may receive catalog values only under a future reviewed versioned contract,
not project file paths, Dart source, file handles or arbitrary project code, and
has no protocol capability to read, write, Save, Undo/Redo or persist anything.
NetBeans alone owns `.fd`, Dart, `SaveCookie`, the command session and all file
authority. A platform on which an isolated child surface is infeasible requires
an explicit reviewed native-provider decision; it must not silently fall back
to transferred pixels.

Every request and runtime response is bound to an exact open-Canvas session,
the validated document/model revision and a host-issued monotonically
increasing presentation sequence. Frame and layout sequences identify native
presentation and Flutter-side hit-test epochs. Image-resource identity is the
lowercase raw SHA-256 of immutable compressed bytes, and its bundle is bound to
the exact model revision rather than inferred from frame/layout sequence. The identities fence delayed acknowledgements and intents across
reload, Undo/Redo, runner restart, close and reopen. Every backend spawn or
restart receives a fresh session identity, so an ABA return to the same logical
revision cannot revive detached work.

Drag and drop deliberately crosses the Java/native boundary as an intent
protocol. For the first implemented slice, ADR-025 replaces a descriptive
palette/model payload with one bounded opaque token carried unchanged through
Flutter-authoritative hover, prepare and commit or cancel. OLE `MOVE` is only
the immutable-Palette transport result; the semantic intent is `ADD`. Java
rejects stale, replayed, malformed or no-longer-valid targets and validates the
intent against the current catalog and domain model. Only that trusted Java
admission may invoke a Designer command; Flutter never mutates the model or
files directly. The same host-authoritative rule applies to selection and
property intents.

The implemented surface contains the standard context-sensitive NetBeans
Palette, selected-Node Properties, the pure lifecycle/admission identities, the
exact version 1 hello/close/failure handshake and fail-stop bounded process
framing. ADR-024, ADR-027, ADR-030, ADR-031, ADR-032, ADR-033, ADR-037,
ADR-038, ADR-039 and ADR-040 through ADR-060 make 652 catalog-backed
non-`Scaffold` Properties
fields writable, including the 59-leaf Text projection, two `SizedBox`
dimensions, 13 typed Icon constructor properties,
120 grouped AppBar leaves, 286 ElevatedButton leaves and the required
`AspectRatio.aspectRatio` value, all 13 `Container` constructor properties,
the two Opacity properties, three Align properties, three FractionallySizedBox
properties, four Stack properties, one Expanded property, 22 Image properties,
54 TextField properties, 17 ListView properties, nine Wrap properties, three
FittedBox properties, the required ConstrainedBox constraints property, four
UnconstrainedBox properties, two LimitedBox properties, six OverflowBox
properties, Flexible's two properties, Spacer's one property, Baseline's two
required properties plus optional child slot, IntrinsicHeight's property-free
optional child slot, IntrinsicWidth's two optional step properties plus optional
child slot, Offstage's optional boolean plus optional child slot, and
SizedOverflowBox's required structured size, optional alignment and optional
child slot.
ADR-025 historically made only built-in `Text` publicly draggable and later
admitted six sources; ADR-030 records the seven-source stage and ADR-031 records
the eight-source stage. ADR-032 supersedes those surface counts with the
nine-source, 117-candidate capability matrix (101 accepted and 16 rejected),
and ADR-033 superseded it with ten sources and 140 candidates (122 accepted and
18 rejected), ADR-037 superseded that stage with eleven sources and 165
candidates (145 accepted and 20 rejected), and ADR-038 established twelve
sources and 192 candidates (170 accepted and 22 rejected). ADR-040 established
thirteen sources and 221 candidates (197 accepted and 24 rejected). ADR-041
established fourteen sources and 252 candidates (226 accepted and 26 rejected),
ADR-042 established 285 candidates (257 accepted and 28 rejected), ADR-043
established 320 (290 accepted and 30 rejected), ADR-044 established 340 (292
accepted and 48 rejected), and ADR-045 established 360 (310 accepted and 50
rejected). ADR-046 established the nineteen-source, 380-candidate matrix
(328 accepted and 52 rejected); ADR-047 established the twenty-source,
420-candidate core matrix (365 accepted and 55 rejected), ADR-048 established
the twenty-one-source, 462-candidate matrix (404 accepted and 58 rejected), and
ADR-049 established the twenty-two-source, 506-candidate matrix (445 accepted
and 61 rejected), ADR-050 established the twenty-three-source, 552-candidate
matrix (488 accepted and 64 rejected), ADR-051 established the twenty-four-source,
600-candidate matrix (533 accepted and 67 rejected), ADR-052 established the
twenty-five-source, 650-candidate matrix (580 accepted and 70 rejected), and
ADR-053 established the twenty-six-source, 702-candidate matrix (629 accepted
and 73 rejected), ADR-054 established the twenty-seven-source, 729-candidate
matrix (631 accepted and 98 rejected), ADR-055 established the
twenty-eight-source, 756-candidate matrix (633 accepted and 123 rejected), and
ADR-056 established the twenty-nine-source, 812-candidate matrix (684 accepted
and 128 rejected), ADR-057 established the thirty-source, 870-candidate matrix
(737 accepted and 133 rejected), ADR-058 established the thirty-one-source,
930-candidate matrix (792 accepted and 138 rejected), ADR-059 established the
thirty-two-source, 992-candidate matrix (849 accepted and 143 rejected), and
ADR-060 establishes the current thirty-three-source, 1,056-candidate matrix
(908 accepted and 148 rejected).
Same-tree existing-widget movement is separately
enabled by ADR-028.
A separate post-handshake runtime control codec publishes one exact
validated revision, admits its layout acknowledgement, synchronizes stable-ID
selection and capability-gates the narrow palette-drop intent. The canonical
protocol-v13 model payload accepts only exact reviewed Canvas-capable built-ins:
`Scaffold`, `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`, `Text`,
`Icon`, `Image`, `Padding`, `Center`, `Align`, `FractionallySizedBox`, `SizedBox`,
`AspectRatio`, `Stack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
`IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `ListView`, `Wrap`, `FittedBox`,
`ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `Container` and `Opacity`; the
isolated runner independently enforces the same schema and receives neither
project code nor file authority. `CATALOG_JSON` remains reserved for a future
versioned catalog contract. Under negotiated `asset.imageBytes.v1`, exact
revision-scoped model descriptors are followed by ordered NBFC kind 4
`IMAGE_BYTES` frames for referenced compressed assets; paths and callback
identifiers remain excluded.

The Windows edge has a real heavyweight AWT HWND host, exact
PID/parent/class/style validation for runner and `FLUTTERVIEW` children, a
bounded SDK-keyed build cache and an isolated child-runner lifecycle per open
`.fd` Design MultiView. Cache reuse requires a bounded SHA-256 manifest for the
complete launch runtime, and deterministic tests fence
close/build/launch/attach/exit races plus two simultaneous sessions. The native
Canvas now renders the validated thirty-three-widget model for Mobile, Tablet,
Desktop and Web responsive preview profiles and synchronizes selection with the
Explorer/Nodes tree and standard Properties window. The Palette exposes exactly
those thirty-three Create-capable definitions, and the DnD-capable set uses the
reviewed 1,056-cell candidate matrix across thirty insertable any-widget and two
trait-bound destination slots; 908 cells are accepted and 148 rejected.
Expanded and Flexible each enter only direct Row/Column wrapper targets, while
Spacer inserts only into direct Row/Column children. Expanded and Flexible's
required child slots are replacement-only rather than insertable. Canvas model
protocol v13's
content-addressed asset-resource frames do not change ADR-021's core boundary:
the Canvas is still rendered directly by Flutter and never transferred as a
screenshot or framebuffer-pixel stream.
Preview availability follows real generated project platform directories rather
than connected devices or a stale wizard choice. Each choice is now an exact
`responsive mode + adaptive target` pair: Android Phone/Tablet, iPhone/iPad,
Windows/macOS/Linux Desktop, or Web. A project-scoped observable snapshot updates
open Design views on canonical folder creation, deletion or rename; the exact
choice is retained first, then the same responsive mode, and otherwise the first
canonical target is selected. An empty snapshot disables Preview and does not
fall back to Mobile. Android/iOS/macOS/Linux targets reach Flutter
`ThemeData.platform` on the Windows engine. Selecting Web renders the exact Web
responsive viewport on that same engine as a design-time layout preview. The UI
states that browser-only runtime behavior is not emulated. Browser-runtime
fidelity remains a separate backend and is not implied by this native layout
preview.

The separate Windows browser backend is now fixed to a compiled Flutter Web
release bundle hosted directly in a windowed Microsoft Edge WebView2 child
controller. A narrow native Win32 adapter, not JavaFX WebView, owns WebView2's
COM STA, message pump, controller and child HWND beneath the heavyweight AWT
carrier. The adapter and architecture-matched Microsoft loader are distributed
as one manifest-pinned x64 bundle with exact sizes, SHA-256 digests, license and
notice. Flutter uses direct DOM multi-view embedding rather than an iframe.
WebView2 JSON web messages carry the existing bounded NBFC frame bytes; they do
not replace the lifecycle/model protocol or grant browser code model, file or
mutation authority.

The internal host and exact-Web session foundation are implemented but are not
product-routed.
`main_web.dart` reuses the bounded Canvas runtime through platform-specific I/O,
creates browser-managed Flutter views, and refuses to start without the WebView2
page bridge. The Java/native host detects the installed Runtime, extracts and
verifies the pinned bundle, owns the child controller lifecycle, leases one
private publication generation and passes an ABI-v3 path/size/SHA-256 manifest
to native code. Native creation rehashes and freezes every resource in memory;
the isolated `.invalid` HTTPS origin has no disk-folder fallback. It admits exact
snapshot paths under a frozen CSP, requires an exclusive owned user-data folder,
injects the exact 256-bit session nonce before page script and exposes only the
authenticated, bounded, contiguous NBFC stream. Runtime admission requires
`100.0.1185.39` or newer, sets the same native target-compatible version, and
retains COM `QueryInterface` capability checks. Foreign sessions are ignored;
authenticated malformed, oversized or non-contiguous messages terminate the
channel.

The deterministic Flutter release bundle has been reproduced from the offline
dependency cache with local CanvasKit and registered local Roboto plus its
Apache-2.0 license. A standalone physical Windows x64 smoke accepts installed-
Runtime detection, exact page/bridge authentication, an authenticated NBFC
`host.hello` → digest-verified `runner.hello` round trip, read-back-verified
bounds and visibility, the focus API, private parking-parent handoff,
parent-HWND release, matching-PID browser resource release, exact-handle destroy
retry, close-during-start and deadline-bounded teardown.
That smoke does not authorize production provider selection or session routing,
production Retry/crash recovery, or the
assembled NetBeans model/layout/selection, DPI and isolation matrix. The backend
therefore remains unavailable in the product until those independent gates pass.
The host foundation now supplies an asynchronous pre-peer-loss barrier, a
poisoned state with exact-handle teardown retry, matching-PID
`BrowserProcessExited` proof and retry-safe partial UDF deletion backed by an
external per-session ownership marker. Failed native startup also returns any
retained exact handle to Java; a null failed-create handle is release proof,
while an unconfirmed/malformed result quarantines its callback and UDF.
The exact-Web session consumes that authenticated stream through the same
bounded model/layout/selection/viewport/interaction gates, verifies the compiled
`CanvasEngineIdentity` against `runner.hello`, binds physical presentation to
the independently observed WebView2 bounds/DPR and leaves native OLE Palette
insertion unavailable. The Web runner binds to its exact `FlutterView`; a second
distinct view is terminal rather than silently borrowing `implicitView`.
The assembled MultiView owns one backend-neutral envelope for the Canvas
session, component and exact focus surface, with raw host transfer hidden behind
a routed factory. The coordinator is now connected as the assembled MultiView's
sole component-replacement authority. It implements mutually exclusive owner
creation, keeps the old heavyweight component attached throughout asynchronous
pre-peer-loss retirement, removes it only after that proof, and creates the
successor afterward. Exact owner epochs fence model, presentation, focus,
visibility and interaction callbacks; rapid route changes coalesce and poisoned
cleanup retains the owner for explicit Retry.

The asynchronous close-handler foundation exposes pending and peer-safe Canvas
close states, preserves ordinary Save/Discard/Cancel decisions, vetoes the
current close stack while coordinator retirement runs and schedules a fresh
TopComponent close only after completion. It is deliberately not installed in
production by default while exact-Web selection remains off. NetBeans
`Split Document` and `Clear Split` currently reparent or remove the heavyweight
AWT hierarchy directly without consulting `canCloseElement()`; closing a
non-last clone also bypasses `closeLast()` and its handler. The close foundation
cannot protect those paths. Production therefore still fixes the selector
to the existing native route for all targets, including the bounded Web
responsive preview. The exact-Web selector branch must never use a native-engine
fallback. This establishes the assembled ownership/session seam without claiming
Web product readiness.
The disconnected exact-Web build prerequisite strictly binds Flutter version,
framework revision, engine revision and Dart SDK version from SDK evidence into
its cache fingerprint, compiler defines, build result and `runner.hello`. It
uses one JVM/cross-process lock, stable bounded markers, full artifact
revalidation, fresh private publication and shared descendant-aware process-tree
retirement. Mutable generated roots are never served; publication-owner and
final-lease cleanup are retryable. The internal session, owner and assembled
MultiView now enforce the exact hello comparison and pre-peer-loss barrier for
coordinator-owned replacement and ordinary close preparation. This still grants
no product route until every physical peer-removal path, including Split/Clear
Split, non-last clone close and direct `componentClosed()`, is proven to await
that barrier.
The RELEASE300 source audit established that this cannot be completed by an
action wrapper or MIME `CloseOperationHandler`: `TabsComponent` performs the
split reparent synchronously, and `CloneableTopComponent.Ref` admits a non-last
clone without calling `closeLast()`. There is no exported asynchronous
pre-removal SPI spanning both operations. A plugin-owned `CloneableEditor`
shell is therefore the supported plugin-only direction. Its dormant foundation
reuses CES' sole Source pane, manually brackets the Design lifecycle, preserves
clone grouping, supplies a tokenized shell close-retry host and does not
implement internal `Splitable`. A support-owned reservation now makes clone
construction and ordinary close mutually exclusive. The completed support-wide
`CloseCookie`/shell-owned Close All batch resolves Save/Discard/Cancel exactly
once, then
binds the exact clone topology and post-decision live-document revision plus one
atomic pair identity/state/external-event/Source-state revision. It issues one
exact permit at a time, retires each clone-local Canvas sequentially and closes
that owner before dispatching the next. Failure, topology drift or revision
drift abort fail-closed, while successful authority remains held through each
`componentClosed()`. Because completion is asynchronous, the initiating
synchronous `CloseCookie` API returns `false` instead of reporting early
success. A unique per-open shell stamp separates harmless sibling lifecycle
drift from same-owner close/reopen ABA: the former may finish only the already
admitted physical cleanup, while the latter revokes the old authority without
touching the reopened shell. The irreversible post-`Ref.unregister` snapshot is
bound explicitly, and the final internal `close(false)` must match exact
pre/post topology and document/pair revisions and acknowledge actual success.
The shell remains production-disabled and `PERSISTENCE_NEVER`; exact Web stays
off. An abandoned or stale close now waits for the captured Canvas-owner
coordinator to reach a safe terminal state and then rebuilds Canvas through a
fresh coordinator generation. Factory, owner, observer and close-completion
callbacks are generation-fenced, and the latest requested backend survives the
rebuild. The shell also permanently sets
`TopComponent.PROP_CLOSING_DISABLED` and supplies its own permit-aware Close
action, which closes the RELEASE300 `Close Mode` bypass without weakening the
asynchronous permit. The same latch makes NetBeans' stock global Close All skip
the shell, so the shell contributes its own support-batch action. This does not
authorize the product route. Pair-aware node Rename, Delete and Cut-Move now
reserve an operation-owned pair lease before asynchronous shell retirement.
After the final exact `componentClosed()`, the support reservation remains held
while one identity-bound, one-shot proof dispatches the operation callback off
the EDT. The callback must claim that exact proof and lease before replaying the
existing synchronous operation once. Rename and Delete re-enter the real
`DataObject` contract, preserving ordinary NetBeans events and binding updates;
Cut-Move re-enters the existing paired move transaction. Its exact `CutSession`
blocks duplicate paste, clears only the still-current clipboard value after
commit, and becomes retryable after cancellation or failure. Cancellation,
topology or revision drift, stale/foreign/reused proof and close or operation
failure are mutation-free and release both reservations. Direct synchronous
pair-path admission fails closed while a dedicated shell exists. `New Tab
Group`, `Collapse Tab Group`, public
`Mode.dockInto()` and direct post-removal callbacks still lack a universal
plugin-side asynchronous veto. Exact-Web product binding, restart/runtime
parity and physical NetBeans acceptance also remain open. A dormant shell is
evidence for the path forward, not authorization to remove the stock MultiView.
Windows cleanup holds stable FileId handles that deny delete sharing for the
parent/root/marker, denies marker writes, and deletes the verified root and
marker by handle with the marker last.
Product routing remains disabled until the reparent/post-removal bypasses are
closed, exact Web is product-bound, Source/History/restart/runtime parity is
proven, and the assembled physical acceptance matrices pass.
Current Web input acceptance is English-only; physical CJK IME and other
language-specific input remain deferred to the final internationalization phase.
The platform-neutral SPI, completed broader Windows acceptance matrix,
Linux/macOS providers, cross-form/native-surface drag-and-drop, remaining Properties and the
broader Designer mutation surface remain foundation gates.

`MOBILE`, `TABLET`, `DESKTOP` and `WEB` remain responsive viewport intents. Their
selected `CanvasTargetPlatform` is the requested Flutter adaptive appearance,
while the native host and `CanvasEngineIdentity` still identify the physical
runtime. The Windows-first embedded Canvas is authentic only for its bound
Windows Flutter engine, resolved theme, locale, viewport, text scale and
device-pixel ratio. Android/iOS/macOS/Linux adaptive appearance does not claim
their operating-system runtime, fonts, plugins, platform channels, IME or
accessibility stack. Likewise the native Web layout preview does not claim
`kIsWeb`, browser fonts, DOM, plugins or platform channels. Those require a
completed and physically accepted browser backend; the compiled runner and
JavaScript transport foundation alone do not satisfy that gate.

Accepting this ADR did not by itself enable writable UI. ADR-024 later enables
only the reviewed typed Properties allowlist after its revision fencing,
pair-save, Undo/Redo, close and assembled-runtime gates pass. ADR-025 first
enabled one separately bounded historical `Text` insertion transaction after
its own assembled acceptance, then superseded that source/slot restriction with
the reviewed six-`CORE_V1` 36-cell insertion matrix. ADR-028 later authorizes
only same-tree existing-widget Move. Cross-form/native-surface Move and every
broader mutation surface remain outside these authorizations.

## ADR-022 — Pair Copy/Paste is a same-folder Node transaction

Accepted for the 0.1.3 Designer foundation. Copy initiated from either the
visible Dart node or its mirrored `.fd` node publishes a custom NetBeans
`NodeTransfer` paste provider. It deliberately omits `LoaderTransfer` and the
operating-system file-list flavor, because either generic flavor could escape
one physical member without the other. The underlying DataObjects therefore
continue to report generic Copy and Move as unavailable; only the paired nodes
advertise Copy. Pair Cut/Move is the separate contract in ADR-023.

This first slice permits Paste only into the initiating member's current
physical parent. The duplicate consequently stays in the same mirrored relative
folder under `lib` and `.fd_templates`. One target basename is selected jointly
across both trees (`name_copy`, `name_copy_2`, ...), so a one-sided collision
reserves that suffix for the whole pair. Cross-directory Copy is rejected until
the schema has explicit, reviewed rebasing semantics for relative URI values;
preserving those values while changing their physical resolution base would not
be a semantics-preserving copy.

The transaction takes exact bounded snapshots of both source members, verifies
the clean Dart-owned coordinator lease, copies the Dart bytes unchanged, and
canonically produces the target `.fd`. The target document receives a fresh
`documentId`; only `source.dartFile` changes to the selected Dart basename.
Class metadata, generator metadata, managed hashes, Canvas preferences, widget
tree and stable widget ids, extensions and all other document semantics are
preserved. A clean open shared editor remains open and bound to the original
pair; Copy creates independent target DataObjects and never rebinds the source
session.

Both staged target files are published inside one owned filesystem atomic
action. A failure removes only identity- and byte-verified artifacts created by
that transaction and re-verifies the exact source snapshots. Staging write,
publish and rollback delete revalidate the exact FileObject path identity under
the same `FileLock` as their mutation; publish and delete also require the
planned bytes. Recovery never deletes an artifact whose identity or content was
changed by another owner. This is a verified
in-process rollback contract, not a durable crash-recovery journal. Copy/Paste
does not transfer the live editor/DataObject lifecycle and remains restricted to
the same mirrored folder even after ADR-023 adds the distinct pair Move flow.

## ADR-023 — Pair Cut/Move is a same-project mirrored Node transaction

Accepted for the 0.1.3 Designer foundation. Cut initiated from either physical
pair node publishes only a private pair paste provider through
`NodeTransfer.CLIPBOARD_CUT`; it does not expose `LoaderTransfer` or an
operating-system file-list flavor. Successful Paste returns the empty
transferable, making the Cut one-shot. The underlying path-bound DataObjects
continue to reject generic Move, so no one-file or foreign clipboard path can
separate the Dart and `.fd` members.

The target must be a direct, already existing writable folder under `lib` or
`.fd_templates` in the same Flutter project. Its corresponding mirrored folder
must also already exist and be writable. Move preserves the basename and both
exact Dart and `.fd` byte sequences; it does not create folders, rename a class,
rewrite `source.dartFile`, rebase URI values or merge with an existing target.
One-sided collisions, ambiguous case/Unicode identities, links, escapes,
hard-linked identities, read-only paths, incomplete or non-current-version pairs,
conflicts and unsaved project Dart editors all fail closed.

Before the filesystem edge, a bounded pure planner scans exact project Dart
snapshots for `import`, `export`, `part` and URI `part of` directives. The
NetBeans adapter binds that inventory to the canonical `pubspec.yaml` package
name and matching `.dart_tool/package_config.json` self-package roots, rejects
nested packages and package aliases of the project `lib`, and scans again for
modified editors. It blocks every relative directive originating in the moved
source, every incoming reference to its old path and every reference that could
acquire or change binding at the destination. Unsupported URI forms and
case-folded/NFC-equivalent path identities are rejected rather than guessed.
At the mutation boundary the adapter acquires exact NetBeans 30 MasterFS data
locks for every proof input and the child-cache write mutexes for every proof,
source and target folder plus the physical ancestor chain to the local
filesystem root. The final inventory/modified-editor proof and the filesystem
callback then run together on the EDT. This excludes in-process MasterFS save,
create, delete and rename races; the adapter verifies the exact NetBeans 30
MasterFS runtime shape and fails closed if that admission mechanism is
unavailable. Because NetBeans exposes no public folder-admission API, that
bridge is one isolated reflective compatibility edge rather than a compile-time
dependency on a private MasterFS package. A writer outside NetBeans remains a
residual race.

Paste acquires the pair path-operation lease, closes an open clean
Designer/Source editor, snapshots both members, publishes the `.fd` target
before Dart so the loader never observes a transient ordinary Dart owner,
verifies both targets, and retains their locks while it renames the original FileObjects to
reversible private `.nbmove` tombstones. Only after both exact source identities
are retired is the logical Move committed; post-commit tombstone deletion is
best effort. A late provider/verifier failure after that commit is recorded as
a recovery conflict but cannot report the operation as uncommitted or leave the
one-shot Cut reusable. The old path-bound DataObjects/controller/coordinator
retire, and fresh DataObjects own the exact target files.

Before commit, rollback restores and verifies both original sources first and
then removes only identity- and byte-verified target artifacts owned by the
transaction. If exact source recreation cannot be proved, verified targets are
retained as recovery copies rather than risking total data loss. This is an
in-process rollback/recovery contract, not a durable crash journal.

Schema-v1 `AssetValue` paths are defined relative to the Flutter
project/pubspec root, never relative to the `.fd` file. Schema-v1 `extensions`
are location-independent opaque metadata and must not encode `.fd`-relative
semantics. Those rules make byte-preserving pair Move well-defined; they do not
relax ADR-022's block on cross-directory Copy, whose duplication contract still
requires explicit relative-URI rebasing semantics.

## ADR-024 — Typed Properties is a bounded revision-bound mutation slice

Accepted for 0.1.3. The standard NetBeans Properties window is writable only
for the reviewed constructor arguments of `Column`, `Row`, `Padding`, `Center`
and `Text`. The closed allowlist contains 76 properties; `Scaffold` stays
read-only and is a separate design task. `Text` contributes 59 typed leaves in
seven sets: Text (8), Accessibility (2), Locale and scaling (7), Text style (25),
Paint and effects (4), Advanced typography (2) and Strut style (11). Editor
choice and value admission come from the immutable
widget catalog: bounded strings and newline font-family lists, optional boolean
checkboxes, exact constrained integers/doubles, closed enums, physical edge
insets and ARGB colors. Required arguments cannot be unset; NetBeans Restore
Default emits `ResetProperty` only for an optional argument.

Those Text leaves are not lossy serialized composites. Deterministic Dart
generation and the native Canvas use the same mapping to construct optional
`TextStyle`, `StrutStyle`, `Locale.fromSubtags`, `TextScaler.linear` and
`TextHeightBehavior` values, and omit a composite when none of its leaves is
present. The deprecated `Text.textScaleFactor` argument and `key` are not
exposed. ADR-027 adds a closed, typed subset for `Paint`, shadows, font features,
font variations and semantic theme roles; raw Dart expressions are not an
alternative representation. Directional edge-inset values remain outside the
current contract.

Every accepted cell edit creates one exact `SetProperty` or `ResetProperty`
against the selected stable widget ID and the immutable revision token held by
the Node's current atomic `READY` presentation. A revision-scoped one-shot
fence prevents a second editor callback from reusing that token and is rearmed
only when the Node receives a new admitted handler. Stale, closed, conflicted,
unsupported or concurrently changing pairs fail closed with the operation,
target and reason.
The candidate must pass catalog/relationship validation, deterministic Dart
generation and Dart analysis before the existing `PairSaveCoordinator` adopts
it; the combined Source/model Undo/Redo and Save lifecycle remain authoritative.

Property-only revisions retain the selected Explorer `Node`, its property sets
and each `Node.Property` identity. Their immutable backing snapshot, current
revision handler and mutable lookup are refreshed in place, followed only by
exact named property-change events. This preserves the standard PropertySheet's
active editor, selected row, focus, scroll position and tab across `APPLYING`
and `READY`. A changed widget type, slot topology/order or catalog property
schema still requires a complete Explorer tree rebuild.

`DesignerCommandSessionOrchestrator.PUBLIC_MUTATION_UI_ENABLED` is therefore
`true` only for this explicitly admitted Properties path. It does not authorize
Palette insertion, Java/Flutter DnD, arbitrary Canvas commands, writable
`Scaffold`, contributed properties, unreviewed nested object/list graphs, the
optional runtime-faithful browser Canvas backend or Linux/macOS native-surface
providers. This does not disable the bounded native-engine Web responsive
layout preview accepted by
ADR-021.

## ADR-025 — Native DnD is one catalog-authorized CORE_V1 insertion transaction

Accepted and enabled for the six exact CORE_V1 definitions in the standard
NetBeans Palette: `Scaffold`, `Column`, `Row`, `Padding`, `Center` and `Text`.
The target is either the terminal position of an existing
`Row.children`/`Column.children` list or an empty `Scaffold.body`,
`Scaffold.floatingActionButton`, `Padding.child` or `Center.child` single slot at
index zero. At this historical ADR-025 milestone, that was the complete 36-cell
ANY_WIDGET matrix for its six sources. `Scaffold.appBar` remains unavailable because none of those sources has
the required `PreferredSizeWidget` trait. Java resolves the exact source,
parent and slot through the current widget catalog and revalidates cardinality,
acceptance, capacity and insertion index. For this Palette `ADD` route, arbitrary list indices,
before/between-child insertion, occupied single-child slots, existing-widget
move/reorder, cross-form drag, non-CORE_V1 Palette types, unreviewed slots and
Linux/macOS/Web DnD remain disabled.

Windows uses a native OLE bridge because the drop crosses from Swing-owned
Palette chrome into the embedded child-HWND Flutter surface. At drag start,
Java stores the allowlisted source type behind a bounded, short-lived,
process-local opaque token. Its wire form is printable ASCII, no longer than
160 characters and begins with `nbfdnd:v1:`. The token is one-shot and conveys
neither widget JSON nor a stable ID, file path, Dart source, catalog authority
or file-write capability. OLE transfers only that exact token and native-view
coordinates; the identical active-view value must cross hover, prepare and the
terminal commit or cancel, and only Java resolves it. Closing or restarting the
Canvas, replacing its presentation or layout, starting another drag,
consumption or expiry invalidates the outstanding token. Canceled, failed and
non-`MOVE` drags revoke it immediately. A successful native `MOVE` retains it
for a bounded three-second asynchronous grace, ending sooner on consumption,
so the committed runner event can arrive after the OLE source callback.

OLE `MOVE` is only the Windows/NetBeans transport contract: the Palette offers
`ACTION_MOVE`, and its immutable prototype is not removed. The semantic
Designer intent is always `operation = ADD`; this decision does not enable an
existing-widget Move or reorder.

Flutter owns the authoritative live-tree hit test and terminal drop-zone
visuals. Native hover starts fail-closed and advertises `MOVE` only after the
exact latest Flutter probe is approved. Fast release may proceed to final
validation only while the exact latest already-sent probe remains in flight;
hover and prepare use one FIFO `MethodChannel`, so Flutter observes the hover
first. Under the negotiated `palette.drop.catalogInsert.v1` capability, prepare
revalidates generation, token, probe, point, presentation and semantic target
and stores one candidate without emitting `runner.paletteDrop`. Windows pumps
its STA for at most 250 ms waiting for that reply. Timeout, error, reentrant
cancel or shutdown fails closed, sends exact cancel/leave while the channel is
alive and returns OLE `NONE`, so a late prepare cannot mutate Java state. Only
a timely positive prepare sends a single-use commit and returns OLE `MOVE`.
Commit revalidates the same identity and may then emit one
`runner.paletteDrop` containing the exact session, presentation, document,
logical revision, frame, layout and intent sequences, the opaque token,
`operation = ADD`, parent stable ID, the reviewed `slotName`, and its exact
insertion index. The current native geometry admits the terminal
`Row/Column.children` position or an empty `Scaffold.body`,
`Scaffold.floatingActionButton`, `Padding.child` or `Center.child` at index zero.
Flutter never resolves the token into a prototype and never mutates the
canonical model or either file.

Java consumes the token atomically before command admission; an unknown,
expired, duplicate or already consumed token fails closed and is never made
reusable after rejection. It then rechecks the exact current revision and
accepted layout, resolves the parent and slot through the current widget
catalog, verifies cardinality, acceptance, capacity and index, and creates the
exact consumed CORE_V1 prototype with a fresh host-owned stable ID. Any stale, foreign,
malformed or concurrently invalidated fact produces no command and no file or
history change.

OLE `MOVE` confirms timely Flutter prepare and commit dispatch, not Java
admission. Only a drop subsequently admitted by Java becomes one bounded
`AddWidget` command. It must pass the existing catalog/relationship validation,
deterministic Dart generation, analyzer admission, claimed paired `.fd`/Dart
replacement and `PairSaveCoordinator` adoption. The action owns one
chronological native Undo/Redo edit and follows the existing Save lifecycle; no
parallel DnD-specific persistence or Undo stack is allowed. Public enablement
passed live assembled Windows NetBeans 30 drop → Save → Undo → Redo → Save
acceptance. This acceptance statement does not include a separate saved-history
Undo → Save cycle. ADR-024's Properties authorization does not implicitly
authorize this path, and all Palette DnD outside this exact slice stays
disabled. ADR-028 separately authorizes the same-tree existing-widget route.

## ADR-026 — Project themes are shared versioned resources

Accepted for 0.1.3. Theme definitions belong to the Flutter project and are
never embedded in a `.fd` form. The canonical descriptor is
`.fd_templates/project.fdtheme`; deterministic runtime Dart is generated at
`lib/theme/app_theme.dart`. New applications receive one light and one dark
Material seed theme, `ThemeMode.system`, and exact `MaterialApp.theme`,
`darkTheme` and `themeMode` wiring while retaining the standard counter sample
and widget test.

Schema v1 admits a bounded catalog of stable ids, display names, brightness and
opaque `0xFFRRGGBB` seed colors. Schema v2 adds the required project-portable
boolean `enabled`. Schema v3 adds a required `enabled` state to each catalog
definition. Schema v4 adds closed typed override tables for the 46 supported
nondeprecated Material `ColorScheme` roles and the 15 Material 3 `TextTheme`
roles. Each text role admits optional color, background color, font size,
weight, style, letter spacing, word spacing, height, family, composable
decoration, decoration color/style/thickness fields. Theme-level text colors
may be literal ARGB values or semantic references to the same ColorScheme role
catalog. Omission means inherit the seed-derived Material value; an explicitly
empty decoration means `TextDecoration.none`.

Schema v5 adds one closed table of exactly 36 typed component colors:
Scaffold background; AppBar background, foreground, shadow and surface tint;
global Icon color; and ElevatedButton background, foreground, overlay, shadow,
surface tint and icon colors for default, disabled, pressed, hovered and
focused states. Values reuse the literal/semantic theme-color algebra. Schema
v4 migrates to empty components without changing generated Dart bytes; an
explicit save emits canonical v5. Generated Dart and Canvas preserve local
widget value > project component theme > Flutter framework precedence.

Schema v1-v3 definitions migrate in memory with empty override tables, schema
v4 migrates with an empty component table, and an explicit save emits
canonical v5. An individually disabled definition remains
in the canonical descriptor but is omitted from the generated Dart map. While
project themes are globally enabled, the selected light/dark references must
point to enabled definitions of the required brightness. Global disable
preserves those references and individual states, generated nullable accessors
return `null`, and both MaterialApp and Canvas use bounded Flutter defaults.
Re-enabling restores the selected definitions without reconstructing deleted
state. Component properties outside the admitted 36 colors, shapes, theme
extensions, Paint/shadow/OpenType/font-
variation graphs, shaders, filters and arbitrary Dart expressions remain
outside this schema and must not enter through an opaque escape hatch; they
require typed fields, generation rules, preview parity and a schema migration.

The generated Dart hash is part of the descriptor and is verified before a
read or write is trusted. Descriptor and generated Dart updates use one
staged, exact-byte-verified transaction with in-process rollback. A manually
changed generated file is a conflict and is never overwritten. Initial project
provisioning additionally transforms only the uniquely recognized fresh
Flutter `MaterialApp` template; an unsupported source shape fails before any
theme artifact is published. The managed MaterialApp references stay stable;
disabled generated accessors are nullable and return `null`, so toggling never
rewrites developer-owned `main.dart`.

The theme editor is a singleton docked TopComponent in NetBeans'
`commonpalette` mode next to Palette. Opening the canonical descriptor or the
Flutter menu action loads that project into the tab; Save and Reload make draft
ownership explicit, per-definition enablement and custom-theme CRUD are edited
there, and baseline conflicts fail closed. Its `General`, `Colors`,
`Typography` and `Components` tabs expose the complete schema-v5 catalog through typed
editors; each optional role or field has an explicit inherit/reset path, and an
invalid draft cannot silently replace the persisted theme pair.

Canvas inherits the project default unless its existing `canvas.themeMode`
selects a preview brightness. It consumes a validated theme definition and
revision digest, not project Dart code. Canvas model protocol v9 carries the
complete resolved ColorScheme, TextTheme and component-color override tables.
Generated Dart and Canvas both apply `ColorScheme.fromSeed`,
`ColorScheme.copyWith`, `ThemeData.from`, `TextTheme.copyWith`, then component
themes; form-local widget leaves are applied after that base and therefore
remain intentional local overrides. An older
project with no descriptor uses the bounded legacy preview without being
modified merely by opening it. Once any descriptor exists, invalid JSON,
unsafe paths, missing generated Dart or a hash mismatch makes the theme
unavailable and withdraws the preview rather than silently reverting to another
appearance. A valid globally disabled descriptor instead selects the
intentional compatible Material fallback and reports that project themes are
disabled.

## ADR-027 — TextStyle uses structured values and semantic theme roles

Accepted for 0.1.3. TextStyle structured values were introduced in `.fd` v2;
at this ADR milestone the `.fd` schema was v3. Schema-v1 and schema-v2 documents were decoded
through explicit migrations and written as canonical v3 only after an
admitted edit; a newer schema fails closed. The Java domain model,
JSON codec, immutable catalog, Dart generator, native Canvas payload and Flutter
runner share the same closed structured-value contract.

Theme integration is semantic rather than coupled to a concrete theme
definition. A widget may select one reviewed `material.textTheme.<role>` as its
base `TextStyle`, then layer local properties through `copyWith`. Color-bearing
fields may store either a literal ARGB color or one reviewed
`material.colorScheme.<role>`. Switching the project light/dark/custom theme
therefore changes every inherited field without rewriting any `.fd` document;
an explicit local value remains an intentional override.

The additional Text fields are `styleThemeTextStyle`, `styleForeground`,
`styleBackground`, `styleShadows`, `styleFontFeatures` and
`styleFontVariations`. Paint is a deliberately safe subset: color, blend mode,
painting style, stroke geometry, antialiasing, filter quality, invert-colors and
an optional blur mask. Shaders, color/image filters and raw Dart are excluded
from this Paint value; ADR-039 separately admits five typed
`DecorationImage.colorFilter` variants.
Shadows, OpenType features and font variations are ordered stable-ID lists. An
absent list means inherit/omit; an explicitly empty list means clear. The
catalog rejects `styleColor` with `styleForeground` and
`styleBackgroundColor` with `styleBackground`.

Properties uses compact type-aware editors and commits each custom-editor OK as
one immutable revision-bound value; Cancel is a no-op and Restore Default emits
the existing reset command. Nested theme colors use the same catalog allowlist
as top-level colors. Deterministic Dart and Canvas parity is mandatory for every
admitted value. `styleFontSize` is finite and non-negative, matching Flutter's
text-scaling contract.

Schema v3 adds a semantic distinction between physical `EdgeInsets` and
text-direction-aware `EdgeInsetsDirectional`. Existing physical v1/v2 payloads
retain their exact side values during migration. The editor offers All,
Symmetric, Physical and Directional modes, while persistence and Dart generation
canonicalize them to `fromLTRB` or `fromSTEB`. At this milestone Canvas model protocol v5 preserved
the same distinction so RTL preview cannot silently exchange physical sides.

## ADR-028 — Existing widgets move only through exact same-tree planning

Accepted for the 0.1.3 bounded Designer surface. An existing non-root widget
may be dragged only inside its originating widget tree. The transfer is a
private JVM-local value containing the owning tree identity and source stable
ID; it is not a serialized fragment, cross-form authority or native Canvas
mutation request.

The Designer disables `BeanTreeView`'s unrelated Explorer drag source, detaches
its inactive non-Swing AWT drop target, and installs one scoped Swing
`TransferHandler`. A mouse-threshold bridge explicitly starts the local
transfer. This is plumbing only: target admission still occurs through the same
`ON_OR_INSERT` planner, and listeners plus the exact prior handler, drop target
and active state are restored when the Design view closes. Single selection
makes the drag source and standard Properties projection deterministic.

The current immutable document and widget catalog are the only placement
authority. `ON` requires exactly one catalog-compatible slot before occupancy
is considered; it appends to a list after source removal or uses index zero of
an empty single slot. `INSERT` resolves the visible flattened boundary before a
child or after the last child to that anchor's semantic list slot, then converts
the boundary to a post-removal index. Root moves, cycles, missing definitions,
incompatible/full/cardinality-mismatched slots, source minimum-child violations,
invalid boundaries and exact no-ops fail closed. An accepted `MoveWidget`
reuses the immutable source subtree, preserving every descendant and stable ID.

Hover never owns mutation authority. Drop reads the latest immutable document,
catalog and revision token, repeats the entire plan and requires the resulting
command to equal the prepared command. One accepted command uses the shared
chronological Undo/Redo, generation and persistence pipeline. A PAIRED result
regenerates, analyzes and replaces the managed pair. If generated Dart bytes
compare exactly equal, `FD_ONLY` locks and verifies both baselines but commits
only canonical `.fd`; live/disk Dart, modified state and Source Undo presentation
remain unchanged.

Canvas feedback is optional. Under negotiated `widget.movePreview.v1`, Java
sends the exact source and destination plus presentation/document/revision/
frame/layout identities and a monotonic preview sequence. Flutter removes the
source from destination geometry and paints a thin amber target distinct from
selection and Palette feedback. Last-write-wins bounded retry makes a newer
target or clear supersede queued older feedback. The projection is cleared on
invalid/canceled/completed drags and every model, presentation, layout, session,
runner or lifecycle replacement. Its absence never disables the Swing move.

The flattened tree cannot select an empty named slot on a multi-slot parent:
`ON` remains ambiguous even when only one compatible slot is currently empty,
and `INSERT` requires an existing list-child anchor. `Scaffold` and future
multi-slot widgets therefore use ADR-029's explicit Slots editor. This decision
does not authorize cross-form movement, arbitrary native Canvas drag,
unreviewed slots or a second Undo/persistence path.

## ADR-029 — Exact named slots are managed transactionally through Properties

Accepted for the first bounded 0.1.3 slot-management slice. A slot-capable
selected widget uses the standard NetBeans PropertySheet's native `General` and
`Slots` tabs; leaf widgets remain untabbed. Every slot declared by the selected
widget's catalog definition appears in the `Slots` tab in declaration order,
including omitted optional slots. Identity and ordinary catalog-backed fields
remain in `General`.
The model and schema remain unchanged: `WidgetPlacement(parentId, slotName,
index)` is already the exact address, and the catalog remains the sole
cardinality, capacity and type-acceptance matrix.

Each row opens one transactional custom editor. The first bounded slice could
append a compatible reviewed Palette prototype, move/reorder an existing
non-root widget to an exact planner-admitted post-removal index, or remove one
exact direct child. For an occupied single slot that remove action was
presented as `Clear`. ADR-034 extends the same editor with explicit atomic
replacement and list `Clear All`; neither operation is an implicit add/remove
sequence, and Cancel still produces no intent.

The editor owns only a semantic draft bound to the presented immutable
document, catalog and revision. On OK, MultiView revalidates that exact
authority and repeats the appropriate Palette-add, exact-slot move, direct-
child removal, ADR-034 replacement or clear-all plan. One accepted
`AddWidget`, `MoveWidget`, `RemoveWidget`, `ReplaceSlotChild` or
`ClearSlotChildren` command then enters the existing generator, analyzer, pair
Save and chronological Undo/Redo pipeline. Stale dialogs, roots, cycles,
missing definitions, incompatible/full/cardinality-mismatched slots,
minimum-child violations, invalid indices and no-op moves fail closed. This
decision adds no `.fd` schema or Canvas protocol field, no alternate
persistence path and no second history. Flattened-tree drop remains ambiguous
for multi-slot parents; the explicit Properties row is the exact-choice route.

A byte-identical Dart result follows the existing `FD_ONLY` boundary. After
the verified `.fd` commit, pair Save atomically adopts the exact saved
`Current` and re-anchors the command-session cursor before publishing
`WAITING -> READY`; it does not reload a substitute catalog or revision
identity between consecutive slot commands.

## ADR-030 — Widget expansion is fail-closed and complete by capability

Accepted for the first post-ADR-025 expansion stage. A built-in type is not
interactive merely because its type id appears in the extensible catalog.
`Properties`, `Canvas`, `Create` and `DnD` are independent capabilities granted
only to an exact canonical built-in definition. Palette publication, property
projection and Canvas encoding query their own capability; an altered or
contributed same-id definition receives none. Java fingerprints the complete
constructor/property constraints, creation defaults and slot cardinality, while
the isolated Dart runner independently declares and enforces the same reviewed
schema. Exact parity is a build gate.

The first complete expansion is `SizedBox`. It exposes nullable non-negative
`width` and `height`, one optional `child` slot, its own reviewed light/dark
16×16 and 32×32 SVG icons, deterministic Dart generation, strict model decode,
native Canvas rendering and selection, Palette/tree/Canvas insertion,
same-tree reparenting with stable-id preservation, Slots management, Save,
reopen and chronological Undo/Redo. An empty `SizedBox()` keeps its real 0×0
Flutter layout; only a designer overlay supplies a selectable target, and
coincident zero-size siblings cycle deterministically without changing layout.

At the ADR-030 milestone the active surface contained exactly seven reviewed built-ins:
`Scaffold`, `Column`, `Row`, `Padding`, `Center`, `SizedBox` and `Text`. All seven
are Create/Canvas/DnD-capable; the six non-`Scaffold` definitions expose 78
writable fields. The insertion compatibility contract is the complete 49-cell
matrix across seven sources and the seven reviewed destinations:
`Scaffold.body`, `Scaffold.floatingActionButton`, `Column.children`,
`Row.children`, `Padding.child`, `Center.child` and `SizedBox.child`.
`Scaffold.appBar`, cross-form/native-surface movement and every definition or
schema without the exact reviewed capability remain fail-closed. ADR-025's
six-widget/36-cell wording remains the historical milestone it originally
accepted; this decision superseded it only for the then-current surface.

## ADR-031 — Icon is a complete typed vertical slice

Accepted for the next 0.1.3 Designer stage. `Icon` is admitted only as the same
complete canonical built-in across Properties, Create, Canvas, Palette DnD,
deterministic Dart generation, Save/reopen and Undo/Redo. It is a leaf with no
slots. Its required positional `icon` value is schema-v4 typed nullable
`IconData`, never an executable Dart expression, and the 12 reviewed named
constructor arguments bring the active writable surface to 91 property rows
across seven non-`Scaffold` widgets.

The built-in Icon editor admits only **None** or an exact glyph from a bundled,
reviewed registry of exactly 8,825 public Material Icons locked to Flutter
3.44.8. Material font use requires
`flutter.uses-material-design: true`; the plugin must report the requirement and
must not invent a substitute glyph. Omitted theme-backed Icon values inherit
`IconTheme`; `blendMode` and `fontWeight` remain local, while generated Dart and
the native Canvas construct the same real `IconData` and `Icon` arguments. The
Java capability fingerprint and independent
Dart protocol-v6 decoder enforce exact property types, numeric bounds, defaults
and leaf cardinality.

The closed `IconData` editor also owns the visible selector preview contract.
Its PropertySheet cell and chooser rows keep the readable `Icons.*` name and
paint the exact private Material font glyph loaded asynchronously from the
currently resolved SDK. This value preview is deliberately font-derived because
the registry represents Flutter `IconData`, not a generic NetBeans action icon;
the plugin neither registers the font globally nor substitutes a platform font.
Only bytes at the fixed normalized SDK-cache path that match the packaged Web
artifact size and SHA-256 contract are decoded. Missing, linked, changing or
mismatched bytes fail closed to a neutral vector placeholder, and a persisted
toolchain change invalidates an unavailable result without blocking the EDT.

At the ADR-031 milestone the active surface contained exactly eight built-ins: `Scaffold`,
`Column`, `Row`, `Padding`, `Center`, `SizedBox`, `Text` and `Icon`. All eight
are Create/Canvas/Palette-DnD sources; the seven non-`Scaffold` definitions are
read/write. Since Icon adds no destination slot, eight sources across the same
seven reviewed destinations form exactly 56 compatibility cells. `AppBar`,
`ElevatedButton`, writable `Scaffold` properties, unreviewed slots and every
definition without exact capability parity remain fail-closed. ADR-025's 36-cell
and ADR-030's 49-cell counts remain their historical accepted milestones.

## ADR-032 — AppBar is a complete typed PreferredSize vertical slice

Accepted for the next 0.1.3 Designer stage. `AppBar` is admitted only as one
exact canonical definition across Properties, Create, Canvas, Palette/tree
DnD, deterministic Dart generation, Save/reopen and Undo/Redo. Its 28 Flutter
constructor arguments are represented by 120 independently resettable typed
model leaves: direct behavior/layout/color fields plus closed serializable
projections for notification presets, `ShapeBorder`, both `IconThemeData`
values, both `TextStyle` values and `SystemUiOverlayStyle`. Arbitrary callbacks,
Dart expressions and open object graphs remain excluded. Omitting a compound
group omits the complete Dart argument and preserves `AppBarTheme` inheritance;
explicit leaves remain local overrides. The `.fd` schema stays at v4 because no
new value kind or persistence shape is introduced.

AppBar declares exactly five slots: optional `leading`, `title`,
`flexibleSpace` and `bottom` singles plus the optional ordered `actions` list.
`AppBar` owns the canonical `flutter.widgets.PreferredSizeWidget` trait.
`Scaffold.appBar` and `AppBar.bottom` accept only that trait; the other eleven
destinations accept any reviewed source. Nine sources across thirteen
destinations form 117 candidate cells: exactly 101 are admitted and 16 are
rejected by the shared compatibility planner.

Canvas payload protocol v7 adds the slot-acceptance fingerprint and the
negotiated `palette.drop.sourceAware.v1` control. Before native dragging starts,
Java binds the opaque one-shot token to the exact current canonical type and
traits. Flutter uses that bounded authority only to filter visual hover zones;
Java still consumes the token, re-resolves the latest immutable revision and
repeats the canonical planner before admitting any command. Failure to project
the source revokes the token before a native transferable is published. At the
ADR-032 milestone the active surface was nine Create/Canvas/DnD definitions and 211 writable
properties across the eight non-`Scaffold` definitions. `ElevatedButton`,
writable `Scaffold`, contributed same-id definitions and unreviewed slots remain
fail-closed.

## ADR-033 — ElevatedButton is a complete sparse-state vertical slice

Accepted for the next 0.1.3 Designer stage. `ElevatedButton` is admitted as one
canonical definition across Properties, Create, Canvas, Palette/tree DnD,
deterministic Dart generation, Save/reopen and Undo/Redo. Its closed writable
contract has exactly 286 leaves: seven direct behavior/callback fields, five
54-leaf groups for enabled/default, disabled, pressed, hovered and focused
state values (26 `ButtonStyle` plus 28 effective `TextStyle` leaves per group),
and nine common layout/feedback fields. `TextStyle.color` is intentionally not
represented because Flutter resolves effective label color through
`ButtonStyle.foregroundColor`. `ButtonStyle.iconAlignment` is likewise absent:
pinned Flutter 3.44.8 consumes it only in the icon/label child synthesized by
`ElevatedButton.icon`, while this slice admits the ordinary arbitrary-child
constructor. `key`, `focusNode`, `statesController`,
`backgroundBuilder`, `foregroundBuilder` and other runtime object references
remain outside the serializable model.

Callbacks are strict Dart identifiers, not arbitrary expressions. Generated
Dart may reference those identifiers, but Canvas payload protocol v8 publishes
only callback presence. The isolated runner creates inert typed closures and
never receives or executes a project callback name. A disabled button emits
null press callbacks; an enabled button with no press binding receives the
generator-owned empty `onPressed` closure required to retain enabled semantics.
The `.fd` document schema remains v4 because the existing callback value kind
already expresses the persisted binding.

Generation uses direct sparse `ButtonStyle` state properties. Scalar leaves use
deterministic disabled, pressed, hovered, focused and default precedence.
Compound leaves combine default, focused, hovered and pressed fragments per
leaf over the atomic non-null `ElevatedButtonTheme` or framework-default
compound; disabled uses only its own fragment over the inherited value. Partial
axes and TextStyle structures therefore inherit without Canvas-only defaults,
while explicit empty lists and Paint/color replacement remain authoritative. A
missing `fixedSize` axis uses Flutter's infinity sentinel only when no inherited
size exists and is clamped by any finite effective maximum on that axis. Each
state with a local minimum or maximum bound resolves both inherited compounds
and widens each effective maximum axis to its minimum; a state with no local
bound returns a null pair so normal theme/default resolution remains intact.
Each Text package fragment is applied after its current layered `TextStyle`,
including active TextTheme and lower active-state font leaves, without crossing
disabled or explicit `inherit: false` isolation. A fallback-only package
replaces one prior package prefix and never synthesizes a `.../null` family.
Local Text theme/inherit configuration requires one consistent explicit
inherit mode across reachable states so Flutter can animate safely. The closed style editors cover theme/literal colors,
typography, constraints, padding, borders, all `SystemMouseCursors`, six shape
presets (`roundedRectangle`, `roundedSuperellipse`, `stadium`, `circle`,
`beveledRectangle`, `continuousRectangle`) and four splash presets
(`InkSplash`, `InkRipple`, `InkSparkle`, `NoSplash`).

`ElevatedButton.child` is one optional-single, required-named-but-nullable slot.
The empty catalog prototype is valid and generates `child: null`; an occupied
slot accepts every reviewed source. Ten sources across twelve any-widget and
two `PreferredSizeWidget` destinations form 140 candidate cells. Exactly 122
are admitted and 18 rejected: only AppBar carries the trait required by
`Scaffold.appBar` and `AppBar.bottom`. The active surface is therefore ten
Create/Canvas/DnD definitions and 497 writable properties across the nine
non-`Scaffold` definitions. Writable `Scaffold`, contributed same-id
definitions and unreviewed slots remain fail-closed.

## ADR-034 — Occupied-single replacement and list clear-all are explicit atomic commands

Accepted for the current exact Slots editor. An occupied `SINGLE` slot exposes
three explicit operations: replace its exact expected child with a fresh
catalog-canonical widget, replace it with an existing non-root widget from the
same document, or `Clear` that one child. A `LIST` slot exposes `Clear All` for
its exact ordered child set in addition to its existing add, move, reorder and
single-child removal operations. Replacement is never inferred from an add,
and clear-all is never implemented as a loop of partial `RemoveWidget`
commands.

Every editor draft is bound to the immutable document revision and exact
owner/slot address presented to the user. Replacement carries the expected
occupied child id; clear-all carries the complete expected ordered child-id
list. Labels, confirmations and disabled states distinguish replacement with a
new canonical prototype, replacement with an existing subtree, single clear
and list clear-all. OK has a one-submit guard and Cancel submits nothing.

Before submission, the editor and MultiView adapter revalidate the exact
revision, owner, slot, cardinality and expected child fence. A new replacement
must still resolve to the exact canonical catalog definition and pass the
shared compatibility matrix. An existing replacement must additionally remain
detachable, non-root, same-document, non-cyclic and planner-admitted after
removal. Clear operations must preserve the slot's minimum cardinality. The
domain transformer independently repeats compatibility, cycle, root,
cardinality, minimum, stale-id and no-op checks.

One accepted replacement becomes one `ReplaceSlotChild` command whose payload
is either `NewSubtree` or `ExistingWidget`; one accepted list clear becomes one
`ClearSlotChildren` command. Each produces one candidate revision, one
generator/analyzer and pair-Save admission, and one chronological Undo/Redo
edit. Incompatible sources, roots, cycles, stale revisions or child ids,
missing definitions, minimum violations, cardinality changes and no-ops are
rejected without a partial model mutation, generated-Dart change, persistence
write or history entry. This decision changes no `.fd` schema, Canvas protocol,
Palette definition or DnD compatibility cell.

## ADR-035 — Scaffold has a closed writable scalar contract

Accepted for the then-current ten-widget Properties surface. `Scaffold` exposes
exactly 17 independently resettable closed scalar fields in `General`:
`floatingActionButtonLocation`, `floatingActionButtonAnimator`,
`persistentFooterAlignment`, `onDrawerChanged`, `onEndDrawerChanged`,
`backgroundColor`, `resizeToAvoidBottomInset`, `primary`,
`drawerDragStartBehavior`, `extendBody`, `drawerBarrierDismissible`,
`extendBodyBehindAppBar`, `drawerScrimColor`, `drawerEdgeDragWidth`,
`drawerEnableOpenDragGesture`, `endDrawerEnableOpenDragGesture` and
`restorationId`. The PropertySheet groups them as Floating action button,
Appearance, Layout, Drawer behavior and Restoration. Static Flutter presets,
booleans, non-negative finite dimensions, theme/literal colors, strict callback
identifiers and the non-empty restoration id are typed and validated before
they reach deterministic generation or the native Canvas.

The existing exact `appBar`, `body` and `floatingActionButton` slots are
unchanged and remain managed through `Slots`. This slice deliberately excludes
`key`; the widget-valued `persistentFooterButtons`, `drawer`, `endDrawer`,
`bottomNavigationBar` and `bottomSheet`; and the open/runtime-valued
`persistentFooterDecoration` and `bottomSheetScrimBuilder`. Those arguments
require separately reviewed persistence or named-slot contracts and are not
smuggled through scalar strings or executable expressions.

Together with ADR-033's historical 497 writable rows across the nine
non-`Scaffold` definitions, these 17 fields made that exact total 514 writable
rows across ten widgets. ADR-037 later supersedes the aggregate count. This
slice changes no Palette publication,
Create capability, DnD source/destination, compatibility matrix, slot
cardinality, `.fd` schema or Canvas protocol.

## ADR-036 — Windows inline Text editing is Flutter-owned and one-shot

Accepted for the bounded Windows product slice. The current product acceptance
scope is English input only; physical CJK IME and other language-specific input
acceptance are intentionally deferred to the final internationalization phase.
Inline editing applies only to the selected existing
`flutter.widgets.Text` and only to its `data` property. Double-click or F2
activates a real multiline Flutter `TextField` backed by Flutter's
`TextInputClient`. Ordinary Enter inserts a newline. Ctrl+Enter requests final
commit and Escape requests cancel only when Flutter reports an empty composing
range. A non-empty composing range permits neither terminal transition, and
preedit remains runner-local; Java never receives an intermediate composition
value. Cancel produces no property command.

The heavyweight AWT carrier disables Java input methods. The native host does
not decode, synthesize or relay `WM_IME`; the embedded Flutter engine and its
text-input client own OS text input directly. This avoids two competing IME
stacks and keeps Java outside platform-specific composition protocols.

The runner must negotiate `widget.inlineTextEdit.v1`. It may then publish one
strict `runner.textEditCommit` body containing the exact presentation,
document, logical revision, frame, layout and intent identities, the current
interaction-fence sequence, widget stable ID, final `text`, and required
`compositionObserved` metadata. Text must be well-formed UTF-16 and fit both
the 65,536 UTF-16-unit and 32,768 Unicode-scalar limits. The metadata reports
only that the runner observed a non-empty Flutter composing range during the
edit; Java does not validate OS provenance. It is not mutation authority and
does not expose preedit.

Java rejects the event unless the capability was negotiated, both session
identities are exact, the intent is first-delivery, the presentation is current
and visible, layout and interaction fence are synchronized, and the widget is
the exact selected existing `Text`. Missing, extra, malformed, oversize,
foreign, stale, replayed, hidden, selection-mismatched and non-Text events fail
closed. One admitted event maps to at most one existing `SetProperty(data)`
command; unchanged text is a no-op. Changed text therefore uses the same
deterministic generation, analyzer admission, paired persistence, Save and
chronological Undo/Redo pipeline as Properties; the runner receives no direct
model, file or history authority.

Deterministic Flutter tests cover activation, composition-aware keyboard
semantics and final/cancel behavior, while Java codec/channel/session and
view/mutation-bridge tests cover Unicode, bounds, exact fields, capability,
admission fencing, no-op handling and the `SetProperty(data)` boundary. Those
tests accept the product vertical slice but do not prove a physical IME. The
current assembled Windows gate host has no composition-capable CJK input
method, so physical CJK composition remains `OPEN` and deferred. This decision
does not claim Linux, macOS or runtime-faithful Web inline editing, and it does not mark
broad Canvas IME/menu/popup acceptance complete.

## ADR-037 — AspectRatio is a complete single-child vertical slice

Accepted for the next 0.1.3 Palette stage. `AspectRatio` is admitted as one
exact canonical `flutter.widgets.AspectRatio` definition across Properties,
Create, native Canvas, Palette/tree DnD, deterministic Dart generation,
Save/reopen and chronological Undo/Redo. Its complete writable scalar contract
is the required named `aspectRatio` double. The model admits only a finite value
greater than zero, matching both the Flutter widget assertion and render-object
constraint; the canonical empty prototype persists `1.0`. There is no theme or
Directionality dependency and no synthetic expression escape hatch.

`AspectRatio.child` is an optional single any-widget slot. Eleven reviewed
sources across thirteen any-widget and two `PreferredSizeWidget` destinations
form 165 candidate cells. Exactly 145 are admitted and 20 rejected: only
`AppBar` carries the trait required by `Scaffold.appBar` and `AppBar.bottom`.
The exact writable surface is now 515 fields across eleven widgets, including
498 fields across the ten non-`Scaffold` definitions and the existing 17-field
Scaffold slice.

The `.fd` schema remains v4, Canvas payload remains v9 and no wire value kind is
added. Java and the isolated Dart runner duplicate the exact required/default,
numeric-bound and slot contracts as an explicit parity gate. The Properties
editor is a required exact-decimal editor; Restore Default is unavailable.
Canvas renders the real Flutter `AspectRatio`, preserves the standard
IDE-owned selection outline for an empty child and exposes the same optional
single-child drop semantics used by the compatibility planner. Unique reviewed
SVG assets identify the widget in both Palette and tree at light/dark 16/32 px.

## ADR-038 — Container uses structured values as one complete vertical slice

Historical scope note: this ADR records the original image-free Container
milestone. ADR-039 later supplies the shared typed asset infrastructure and
supersedes only the decision to defer `DecorationImage`; the remaining
Container contract and rationale stay in force.

Accepted for the 0.1.3 Palette stage. The canonical
`flutter.widgets.Container` definition is complete across Properties, Create,
native Canvas, Palette/tree DnD, deterministic Dart generation, Save/reopen and
chronological Undo/Redo. Excluding the framework-owned `key` and the
widget-valued `child`, its closed writable constructor surface contains exactly
13 optional arguments in Flutter order: `alignment`, `padding`, `color`,
`isAntiAlias`, `decoration`, `foregroundDecoration`, `width`, `height`,
`constraints`, `margin`, `transform`, `transformAlignment` and `clipBehavior`.
`child` remains one optional single any-widget slot at its actual constructor
position. Omission always preserves Flutter's constructor default; no field uses
raw Dart or a string expression escape hatch.

Four structured value kinds represent the constructor graphs without flattening
or loss: physical/directional `AlignmentGeometry`, normalized `BoxConstraints`
with nullable infinite maxima, a 16-entry column-major `Matrix4`, and a reviewed
image-free `BoxDecoration`. The decoration model supports a literal or reviewed
Material theme color, physical or directional borders with exact `BorderSide`
fields, physical or directional elliptical corner radii, ordered stable-ID box
shadows, linear/radial/sweep gradients with ordered stable-ID color stops,
literal or reviewed theme colors, every `TileMode`, optional
`GradientRotation`, background blend mode and rectangle/circle shape. The same
model is used for background and foreground decorations.

Admission is fail closed at every boundary. Width, height, margin, padding,
radii, shadow blur and constraint bounds obey their non-negative Flutter
contracts; finite maxima cannot be less than minima. `color` and `decoration`
are mutually exclusive, while non-`none` clipping requires `decoration`.
Background blend mode requires a color or gradient, a circle cannot carry a
border radius, and non-uniform borders must satisfy Flutter's paint-safe color,
hairline, shape, radius and `strokeAlign` combinations. Gradients require 2–256
nondecreasing stops with unique IDs; sweep start is less than end, focal radius
is non-negative, and a nonzero focal radius requires a focal alignment. The
paint-rect-dependent degenerate conical case cannot be inferred safely from
abstract alignments and is therefore left to Flutter's runtime contract. All
numbers must survive finite Dart-double emission. Transitions
that must change dependent fields together use one revision-bound
`PatchProperties` command and one Undo/Redo unit, never a temporarily invalid
sequence of scalar commands.

Properties uses transactional structured editors rather than exposing nested
objects as text. Alignment provides presets plus physical/directional
coordinates; constraints provide finite/unbounded bounds and presets; Matrix4
uses a visual 4×4 grid with identity, translation, scale and Z-rotation helpers;
and BoxDecoration groups fill, border, radius, ordered shadows and gradients.
Every optional editor has an explicit inherited/default state, invalid drafts
cannot publish, and an active table cell is incorporated before OK commits.
Top-level `color` and every nested decoration color share the reviewed
`material.colorScheme.*` allowlist, so project light/dark/custom themes flow into
generated Dart and Canvas while explicit literals remain local overrides.

This structured contract raises `.fd` to schema v5 and the contributor Catalog
API to version 4; v1–v4 documents still migrate in memory and are written as v5
only after an admitted edit, while API-1 through API-3 contributors fail closed.
Canvas model protocol v10 carries the same unions and invariants to the isolated
runner. The runner builds a real Flutter `Container`, retains an IDE-owned
layout/selection frame outside the widget's paint transform, paints distinct
padding and margin guides, and supplies a selectable/drop target for an empty
zero-size container without changing its zero layout size.

Adding `Container.child` creates fourteen any-widget destinations. Twelve
reviewed sources across those destinations and the two existing
`PreferredSizeWidget` destinations form 192 candidate cells: 170 admitted and
22 rejected, because only `AppBar` satisfies the two trait-bound slots. The
exact writable catalog now contains 528 fields across twelve widgets: the prior
515 plus the 13 Container properties.

`DecorationImage` is deliberately deferred. A correct implementation needs one
shared typed asset identity, pubspec/package resolution, generated-Dart import
and asset emission, cache/error behavior and native Canvas decoding that other
image-bearing widgets can reuse. It must not enter `BoxDecoration` first through
an opaque path, URL or Dart-expression field.

## ADR-039 — Shared typed assets complete Container DecorationImage

Accepted for the 0.1.3 Palette stage. The model stores image identity, never
project bytes or a filesystem path. `ImageProviderValue` is a closed
asset-only union: `AssetImage` or `ExactAssetImage`, a normalized relative POSIX
asset name, optional Dart package, exact positive scale only for
`ExactAssetImage`, and at most one bounded `ResizeImage` (`width` and/or
`height` in `1..16384`, `exact`/`fit`, optional upscaling). Network, file,
memory and custom providers remain outside the public model; the runner's
internal `MemoryImage` is only the safe projection of host-resolved bytes.

`BoxDecoration.image` now contains a typed `DecorationImageValue` with the 13
Flutter 3.44.8 named arguments in SDK order: required `image`, optional
`onError`, optional `colorFilter`, optional `fit`, alignment, optional
`centerSlice`, repeat, `matchTextDirection`, scale, opacity, filter quality,
`invertColors` and `isAntiAlias`. Color filters are exactly mode, 20-value
matrix, linear-to-sRGB gamma, sRGB-to-linear gamma or saturation; mode colors
admit the same literal/reviewed-theme tokens as other decoration colors. A
center slice has non-negative coordinates and strict positive area. Fit may be
omitted, `fill`, `contain`, `fitWidth`, `fitHeight` or `scaleDown`; `cover` and
`none` fail closed. The callback is one validated declarative Dart
identifier for an `ImageErrorListener`-compatible two-argument handler; no raw
Dart is admitted, and Canvas receives callback presence rather than its text.

The NetBeans resolver inventories only app/package image assets declared by
`pubspec.yaml`, resolving packages through `.dart_tool/package_config.json`.
It rejects absolute, backslash and dot/traversal identities and snapshots only
PNG/JPEG/GIF/WebP after lexical/real-root, symlink, magic,
dimension and bounded-resource checks. Inventory defaults are 4,096 logical
assets, 16 MiB/file, 64 MiB total, dimension 4,096 and 8,388,608 pixels; a
presentation projects at most 256 logical assets and 256 resources, 16 MiB total,
dimension 16,384 and 67,108,864 pixels. Asset variant choice is pinned to
Flutter 3.44.8 framework revision
`058e0af2c2b57e369d905a03ac9748b0ebf543c6`: exact DPR wins; an out-of-range
DPR clamps to the nearest endpoint; between lower and upper variants, DPR below
2.0 chooses upper, otherwise values strictly above the midpoint choose upper
and midpoint ties choose lower.
Only a parsed package root whose `pubspec.yaml` name matches `package_config`
becomes an external recursive-listener boundary. Listener replacement and
cleanup run off the EDT behind a generation fence; changing the watch set forces
a fresh inventory before any bytes are published.

Canvas model protocol v11 over NBFC framing v1 negotiates
`asset.imageBytes.v1`. A resource-bearing
render follows exact `CONTROL` → `MODEL` → `IMAGE` order; its revision-scoped
model descriptor is followed by NBFC frame kind 4
`IMAGE_BYTES`; every descriptor/payload size and lowercase raw SHA-256 is
rechecked before immutable compressed bytes become visible. No filesystem path
or callback identifier crosses the Canvas boundary. Both native and internal
exact-Web runtimes construct a real `DecorationImage` from `MemoryImage(bytes,
scale: resolvedScale)` plus at most one `ResizeImage`. Authenticated
media/decode/resize/center-slice failures quarantine only the affected resource;
framing, identity, digest, ordering and exact model-resource coverage failures
remain fatal. An unavailable or quarantined asset produces a deterministic
non-interactive placeholder and status naming its logical identity, code and
reason; selection, layout and drop overlays remain outside `Container`.

Center-slice size admission mirrors the pinned host decode paths. Native exact
resize rounds a missing width, truncates a missing height and honors explicit
upscale. Web rounds either missing axis, while its
`instantiateImageCodecWithSize` delegation forces `allowUpscaling: false` and
therefore keeps the intrinsic image if either completed target axis would
upscale. The unmodified Flutter `fit` arithmetic may derive a zero axis for an
extreme aspect ratio; that is rejected for `centerSlice` rather than silently
promoted to one pixel.

This decision raises `.fd` to schema v6, the contributor Catalog API to 5 and
the Canvas model protocol to v11. Schema v1-v5 documents migrate in memory with
an absent decoration image and become canonical v6 only on an admitted edit.
The typed Image tab, inventory status and validation descriptions are exposed
to assistive tools; one accepted structured edit, including dependent
property repair, remains one chronological Undo/Redo unit. ADR-038's rejection
of opaque/unvalidated filesystem paths, URLs and Dart-expression escape hatches
remains in force.

## ADR-040 — Opacity is a complete paint-and-semantics vertical slice

Accepted for the 0.1.3 Palette stage. The canonical built-in type is
`flutter.widgets.Opacity`, backed by the const `Opacity` constructor from
`package:flutter/widgets.dart` in pinned Flutter 3.44.8. It is complete across
catalog/model validation, strict JSON round-trip and migration fixtures,
Properties, Create, native Canvas, Palette/tree DnD, same-tree movement,
deterministic Dart generation, pair Save/reopen and chronological Undo/Redo.
Its reviewed palette entry uses its own SVG asset in Layout order 80. `key`, raw
Dart and every unreviewed constructor surface remain excluded.

The API surface contains exactly two writable properties and one slot, in Dart
constructor order: required named `double opacity`, optional named
`bool alwaysIncludeSemantics`, and optional named `Widget? child`. Opacity must
be finite and inside inclusive `[0, 1]`. A new prototype stores
`opacity: 1.0`; it cannot restore that required argument to an absent state.
`alwaysIncludeSemantics` has no stored creation value, so omission preserves
Flutter's `false` default and generates no argument. `child` is one optional
single any-widget slot and is not a property row. The canonical sorted contract
fingerprint is:

```text
W|flutter.widgets.Opacity
P|alwaysIncludeSemantics|boolean|0|-|-|boolean:any
P|opacity|double|1|double:1|double:0:1:1:1|double:range:0:1:1:1
S|child|single|0|0|1|any
```

Native and internal exact-Web Canvas projections construct the real Flutter
`Opacity`, never `AnimatedOpacity`, a color rewrite or an `IgnorePointer`.
At `opacity == 0.0`, Flutter omits child paint and normally omits child
semantics, but it continues to hit-test the child. Explicit
`alwaysIncludeSemantics: true` retains child semantics at zero opacity.
Intermediate rendered alpha values use Flutter's normal offscreen compositing
buffer. Selection, hit testing and Palette/move drop feedback are Designer
controls, so their
overlays wrap the Opacity and remain visible outside its paint/semantics effect;
an empty Opacity keeps its real zero layout size while receiving an IDE-only
selectable child target. Neither Theme nor Directionality participates in this
contract.

No version number changes. `.fd` remains schema v6, the contributor Catalog API
remains 5, Canvas model protocol remains v11, and NBFC framing plus Canvas
control/wire remain version 1. The rationale is structural: required/optional
doubles, optional booleans and optional single any-widget slots already have
canonical model, JSON, migration, Dart and Canvas encodings; Opacity introduces
no value kind, union member, frame kind or payload shape. Existing documents
need no data transform. The exact built-in catalog fingerprint and the runner's
closed type/property/slot validator fence the new semantic allowlist without
claiming a new transport grammar.

Opacity adds two writable rows to ADR-038's 528, producing 530 across thirteen
widgets and 513 across the twelve non-`Scaffold` definitions. `Opacity.child`
is the fifteenth any-widget destination. Thirteen sources across fifteen
any-widget plus two trait-bound destinations produce 221 candidates:
`13 × 15 = 195` accepted any-widget cells, plus the two cells in which AppBar
satisfies `PreferredSizeWidget`, for 197 accepted; the other `12 × 2 = 24`
trait-bound cells are rejected. All Palette insertion and movement outside this
closed matrix remains fail closed.

## ADR-041 — Align is a complete directional-layout vertical slice

Accepted for the 0.1.3 Palette stage. The canonical built-in type is
`flutter.widgets.Align`, backed by the const `Align` constructor exported from
`package:flutter/widgets.dart` in pinned Flutter 3.44.8. It is complete across
catalog/model validation, strict JSON round-trip and migration preservation,
Properties, Create, native Canvas, Palette/tree DnD, same-tree movement,
deterministic Dart generation, pair Save/reopen and chronological Undo/Redo.
Its reviewed Palette entry uses its own SVG asset in Layout order 90. `key`, raw
Dart and every unreviewed constructor surface remain excluded.

The API surface contains exactly three writable properties and one slot, in
Dart constructor order: optional `AlignmentGeometry alignment`, optional
`double? widthFactor`, optional `double? heightFactor`, and optional `Widget?
child`. The model deliberately leaves all three properties absent in a new
prototype. Omitted alignment preserves Flutter's `Alignment.center` default;
omitted factors preserve null and therefore bounded-axis expansion rather than
an explicit factor of one. Present factors accept only finite non-negative
Designer integers or doubles; zero and values greater than one are valid.
Physical and directional alignment coordinates are finite but are not clamped
to `[-1, 1]`, because Flutter permits linear extrapolation. `child` is one
optional single any-widget slot and is not a property row. The canonical sorted
contract fingerprint is:

```text
W|flutter.widgets.Align
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|heightFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|widthFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
S|child|single|0|0|1|any
```

Native and internal exact-Web Canvas projections construct a real Flutter
`Align` and therefore a `RenderPositionedBox`, never `Center`, `AnimatedAlign`
or a paint-only approximation. A physical `Alignment` is independent of text
direction; `AlignmentDirectional` resolves against the current Canvas
`Directionality` and flips its horizontal placement between LTR and RTL. A
factor-driven empty Align may have a real zero layout size. Selection and
Palette/move feedback remain Designer controls outside the widget and retain a
bounded IDE-only selectable/drop target without changing generated layout.

No version number changes. `.fd` remains schema v6, the contributor Catalog API
remains 5, Canvas model protocol remains v11, and NBFC framing plus Canvas
control/wire remain version 1. AlignmentGeometry, optional finite numeric values
and optional single any-widget slots already have canonical model, JSON,
migration, Dart and Canvas encodings. Existing documents need no data transform;
the exact built-in catalog fingerprint and the runner's closed validator fence
the new semantic allowlist.

Align adds three writable rows to ADR-040's 530, producing 533 across fourteen
widgets and 516 across the thirteen non-`Scaffold` definitions. `Align.child`
is the sixteenth any-widget destination. Fourteen sources across sixteen
any-widget plus two trait-bound destinations produce 252 candidates:
`14 × 16 = 224` accepted any-widget cells, plus the two cells in which AppBar
satisfies `PreferredSizeWidget`, for 226 accepted; the other `13 × 2 = 26`
trait-bound cells are rejected. All Palette insertion and movement outside this
closed matrix remains fail closed.

## ADR-042 — Complete FractionallySizedBox as a bounded Layout slice

Accepted. `flutter.widgets.FractionallySizedBox` is the canonical const Flutter
3.44.8 constructor from `package:flutter/widgets.dart`. Its reviewed surface is
optional physical/directional `alignment`, optional finite non-negative
`widthFactor` and `heightFactor`, and one optional any-widget `child`; `key` and
raw Dart are excluded. New prototypes keep all properties omitted so Flutter's
center alignment and null factors remain distinct from explicit values. Native
and exact-Web projections build the real widget under bounded/unbounded and
LTR/RTL inputs, with an IDE-only selection/drop target for an empty zero-size
layout. Existing value/slot encodings keep every version unchanged.

This adds three rows and one source/destination to ADR-041: 536 writable rows
across 15 widgets and 285 DnD candidates, 257 accepted and 28 rejected.

## ADR-043 — Complete Stack with non-positioned ordered children

Accepted. `flutter.widgets.Stack` is a const Flutter 3.44.8 slice with optional
`alignment`, `textDirection`, `fit` and `clipBehavior` plus an ordered list of
any-widget `children`. Designer currently admits non-positioned children only;
`Positioned` is not implied. Clip values are passed to Flutter exactly.
Extrapolated non-positioned alignment does not set RenderStack's visual-overflow
flag, and descendant or paint-only overflow is not clipped by Stack. Palette,
tree, Canvas, slot editing and same-tree moves share terminal append semantics.

This adds four rows, one source and one destination: 540 writable rows across 16
widgets and 320 DnD candidates, 290 accepted and 30 rejected. Versions remain
unchanged.

## ADR-044 — Expanded is an atomic wrapper, not a terminal prototype

Accepted. `flutter.widgets.Expanded` is a const slice with optional
non-negative integer `flex` and one required any-widget `child`. An Expanded node
is valid only as a direct `Row.children` or `Column.children` child. Palette
creation therefore targets an existing direct flex child and submits one atomic
`WrapWidget` with the existing parent/slot/index, preserved child and one new
wrapper ID. It never exposes terminal Add or an empty placeholder. Existing
Expanded nodes move only between those destinations, and `Expanded.child` is
replacement-only with no add, remove or clear operation.

Expanded adds one row and one source but no optional destination: 541 writable
rows across 17 widgets and 340 DnD candidates, 292 accepted and 48 rejected.
No schema or protocol version changes.

## ADR-045 — Image creation requires a real declared asset

Accepted. The const leaf `flutter.widgets.Image` exposes the required
asset-only `image` provider and 21 optional reviewed callback, accessibility,
size, color/opacity, blend, fit/alignment/repeat, center-slice, direction,
playback, antialias and quality leaves. Network/file/memory/custom providers and
arbitrary callback expressions remain excluded. Palette, tree, Canvas and slot
Replace New Widget resolve the deterministic first sorted declared asset before
stable-ID allocation. Empty or unavailable inventory returns a concrete
fail-closed reason, no command and no ID.

The four non-negative center-slice coordinates are all-or-none, require strict
left < right and top < bottom, and cannot accompany `BoxFit.cover` or
`BoxFit.none`. Provider decode scale and effective ResizeImage bounds constrain
the projected rectangle identically in Dart and Canvas. Image adds 22 rows and
one source: 563 writable rows across 18 widgets, 546 outside Scaffold, 15 const
definitions and 360 DnD candidates, 310 accepted and 50 rejected. Existing API
5 image-provider and protocol-v11 resource encodings require no version bump.

## ADR-046 — TextField stores constructor intent, not editable state

Accepted. The canonical const `flutter.material.TextField` is a Material Palette
leaf named **Text Field**, ordered after Elevated Button. It has no creation
dialog, child slot or creation defaults. Its 54 optional named leaves are
presented as Input (14), Layout (9), Behavior (11), Cursor and selection (11),
Callbacks (8) and Restoration (1). Closed keyboard-type (16),
`TextAlignVertical` (3) and system-mouse-cursor (36) presets complement typed
enums, numbers, booleans, colors and strict callback identifiers.

Designer deliberately excludes `controller`, `focusNode`, input formatters,
decoration/style/builder and other owner-managed graphs. Runtime typed text,
selection, controller state and focus state are not persisted. Radius X/Y and
the four scroll-padding leaves are all-or-none; the Properties UI sets/resets
each compound atomically without inventing a numeric default. Validation also
enforces positive ordered line limits, the `expands`/line-limit relation,
single-line obscure text, the newline keyboard relation, one-BMP-scalar
obscuring character and `maxLength == -1 || maxLength > 0`. Dart synthesis emits
reviewed TextInputType/static/mouse-cursor presets, `TextField.noMaxLength`,
`Radius.elliptical` and `EdgeInsets.fromLTRB`.

Generated Dart wraps every TextField in an unconditional
`LayoutBuilder`/`SizedBox`; it supplies width 240 only when width is unbounded and
height 120 only for `expands: true` under unbounded height. The Canvas builds the
same real TextField behind pointer/focus exclusion and uses inert closures for
callback presence, so direct and indirect Row/Column placement remains admitted
without storing runtime input state or adding placement rules.

TextField adds 54 non-Scaffold rows and one ordinary source to ADR-045. The final
surface is 617 writable rows across 19 widgets, 600 outside Scaffold and 16
const-constructor definitions. Nineteen sources across 18 any-widget and two
trait-bound destinations form 380 candidates: TextField adds 18 accepted
any-widget cells and two rejected trait cells, producing 328 accepted and 52
rejected overall. `.fd` stays v6, Catalog API stays 5, Canvas model stays v11 and
NBFC/control framing stays version 1.

## ADR-047 — ListView completes the agreed static core Palette

Accepted. The canonical built-in is the non-const
`flutter.widgets.ListView` default constructor with a static ordered
`children` list. It is published in the **Scrolling** Palette category, supports
Properties, Create, native and exact-Web Canvas, Palette/tree/Canvas DnD,
same-tree movement, exact-slot management, deterministic Dart generation,
Save/reopen and chronological Undo/Redo, and owns one optional-list any-widget
`children` slot at constructor position 17.

The 17 optional constructor-intent rows are grouped as Scrolling (5), Layout
(4), Caching and children (4), Semantics (2), and Restoration (2):
`scrollDirection`, `reverse`, `primary`, `physics`, `shrinkWrap`, `padding`,
`itemExtent`, `addAutomaticKeepAlives`, `addRepaintBoundaries`,
`addSemanticIndexes`, `scrollCacheExtent`, `semanticChildCount`,
`dragStartBehavior`, `keyboardDismissBehavior`, `restorationId`,
`clipBehavior` and `hitTestBehavior`. Physics is restricted to six reviewed
static presets. A numeric cache extent generates
`ScrollCacheExtent.pixels(value)`. Padding and item/cache extents are
non-negative, restoration identity is bounded and non-empty, and
`semanticChildCount` is a non-negative integer no greater than the current
static child count. Controller-owned state, builders/delegates,
`itemExtentBuilder`, `prototypeItem`, deprecated `cacheExtent`, `key` and raw
Dart expressions remain excluded.

Generated Dart and Canvas build the real Flutter ListView. They preserve
vertical/horizontal, reverse and LTR/RTL layout, while Canvas resolves
insertion/terminal zones in visual order. Their shared
`LayoutBuilder`/`SizedBox` guard supplies width 240 or height 120 whenever the
viewport cross axis is unbounded; it supplies the same fallback on an unbounded
main axis only when `shrinkWrap` is false. Bounded Flutter semantics remain
framework-owned. Empty-list selection and drop targets remain Designer overlays
and do not alter the modeled child list.

ListView adds 17 non-Scaffold rows, one ordinary source and one any-widget
destination to ADR-046. At core completion the surface was 634 writable rows across 20
widgets, 617 outside Scaffold and 16 const-constructor definitions. Twenty
sources across 19 any-widget and two trait-bound destinations form 420
candidates. ListView contributes 37 newly accepted and three newly rejected
cells, producing 365 accepted and 55 rejected overall; Expanded remains valid
only as a direct Row/Column child and only AppBar satisfies the two trait-bound
destinations.

This completes the originally agreed eight-item core Palette—`Container`,
`Row`, `Column`, `Text`, `Image`, Button represented by `ElevatedButton`,
`TextField` and `ListView`—at 8/8. It does not claim that all Flutter widgets are
implemented. `.fd` remains v6, Catalog API remains 5, Canvas model remains v11
and NBFC/control framing remains version 1.

## ADR-048 — Wrap begins the practical post-core Palette backlog

Accepted. The canonical built-in is the const default
`flutter.widgets.Wrap(children: ...)` constructor from
`package:flutter/widgets.dart`. It is published in the **Layout** Palette
category between Row and Padding and supports Properties, Create, native and
exact-Web Canvas, Palette/tree/Canvas DnD, same-tree movement, exact-slot
management, deterministic Dart generation, Save/reopen and chronological
Undo/Redo.

The complete reviewed non-`key` constructor surface has nine optional named
properties in Flutter order: `direction`, `alignment`, `spacing`,
`runAlignment`, `runSpacing`, `crossAxisAlignment`, `textDirection`,
`verticalDirection` and `clipBehavior`. Axis, alignment, direction and clipping
use closed Flutter 3.44.8 enums. `spacing` and `runSpacing` accept finite signed
doubles, including negative values as the framework does. The optional ordered
any-widget `children` slot occupies constructor position 9; a new prototype
stores no explicit Flutter defaults.

Generated Dart and both Canvas projections construct the real Flutter Wrap.
Flutter owns horizontal/vertical run formation, alignment, directionality and
clipping. Empty Wraps keep their real zero-size layout while receiving a
non-layout-affecting 36-pixel Designer selection target. Both empty and
populated Wraps expose the complete rendered rectangle as the deterministic
terminal append zone: after children split into runs there is no single stable
linear terminal edge. Palette and tree insertion still append at
`children.length`.

Wrap adds nine non-Scaffold rows, one ordinary source and one any-widget
destination to ADR-047. At that milestone the surface was 643 writable rows across 21
widgets, 626 outside Scaffold and 17 const-constructor definitions. Twenty-one
sources across 20 any-widget and two trait-bound destinations form 462
candidates. Wrap contributes 39 newly accepted and three newly rejected cells,
producing 404 accepted and 58 rejected overall. Expanded remains valid only as
a direct Row/Column child and only AppBar satisfies the trait-bound
destinations. The Layout Palette now contains 13 items.

This is the first completed slice in a practical 92-widget Material/Base
Designer backlog. With 21 implemented widgets, 71 remain. The target is a
project planning backlog, not a normative complete list of Flutter widgets.
`FittedBox` was the next planned complete slice. `.fd` remains v6, Catalog API
remains 5, Canvas model remains v11 and NBFC/control framing remains version 1.

## ADR-049 — FittedBox is the second complete post-core Palette slice

Accepted. The canonical built-in is the const default
`flutter.widgets.FittedBox(...)` constructor from
`package:flutter/widgets.dart`. It is published in the **Layout** Palette
category after `FractionallySizedBox` and supports Properties, Create, native
and exact-Web Canvas, Palette/tree/Canvas DnD, same-tree movement, exact-slot
management, deterministic Dart generation, Save/reopen and chronological
Undo/Redo. The Palette supplies reviewed light/dark SVG icons at 16 and 32
pixels; no font or text-glyph fallback participates in widget identity.

The complete reviewed non-`key` constructor surface has three optional named
properties in Flutter order: `fit`, `alignment` and `clipBehavior`, followed by
one optional single any-widget `child`. `fit` admits the seven pinned Flutter
3.44.8 `BoxFit` values: `fill`, `contain`, `cover`, `fitWidth`, `fitHeight`,
`none` and `scaleDown`. `alignment` uses the existing finite physical or
directional `AlignmentGeometry` value. `clipBehavior` admits `none`, `hardEdge`,
`antiAlias` and `antiAliasWithSaveLayer`. A new prototype stores no explicit
constructor defaults, preserving `BoxFit.contain`, `Alignment.center` and
`Clip.none` through omission.

Generated Dart and both Canvas projections construct the real Flutter
FittedBox. Flutter owns the fit calculation and scale transform; directional
alignment resolves through the current LTR/RTL `Directionality`; and clipping
is applied only by the selected framework clip behavior. A childless FittedBox
keeps its real zero-size layout while receiving a non-layout-affecting 36-pixel
Designer selection/drop target. The target is outside the widget and never
changes generated Dart or runtime layout. Catalog, validator, generation,
Properties, Canvas model/view/drop, DnD/slot/move commands and their closed-
contract tests share this exact definition.

FittedBox adds three non-Scaffold rows, one ordinary source and one any-widget
destination to ADR-048. At that milestone the surface was 646 writable rows across 22
widgets, 629 outside Scaffold and 18 const-constructor definitions. Twenty-two
sources across 21 any-widget and two trait-bound destinations form 506
candidates. The new target admits every ordinary source except Expanded; the
new FittedBox source enters all 21 any-widget destinations but neither
trait-bound destination. This contributes 41 newly accepted and three newly
rejected cells, producing 445 accepted and 61 rejected overall. Expanded
remains valid only as a direct Row/Column child, and only AppBar satisfies the
trait-bound destinations. The Layout Palette then contained 14 items.

The practical Material/Base Designer backlog was then 22/92 complete, with 70
remaining. The target is a project planning backlog, not a normative complete
list of Flutter widgets. `ConstrainedBox` is the next planned complete slice.
`.fd` remains v6, Catalog API remains 5, Canvas model remains v11 and NBFC/
control framing remains version 1.

## ADR-050 — ConstrainedBox completes the finite, unbounded and expanding constraint domain

Accepted. The canonical built-in is the non-const
`flutter.widgets.ConstrainedBox(...)` constructor from
`package:flutter/widgets.dart`, published in the **Layout** Palette category
after FittedBox. It supports Properties, Create, native and exact-Web Canvas,
Palette/tree/Canvas DnD, same-tree movement, exact-slot management,
deterministic Dart generation, Save/reopen and chronological Undo/Redo. Reviewed
light/dark SVG icons at 16 and 32 pixels provide its Palette identity.

The complete reviewed non-`key` constructor surface is one required named
`BoxConstraints constraints` property followed by one optional single
any-widget `child`. A new prototype stores the neutral `0..∞` range for width
and height. Every axis admits the six normalized Flutter states: `0..∞`,
`0..max`, `min..∞`, `min..max`, tight `value..value`, and expanding `∞..∞`.
Finite bounds must be non-negative and a finite minimum cannot exceed its
maximum; positive infinity is valid as a minimum only when the matching maximum
is also infinite. Raw Dart, negative values and inconsistent infinity pairs
remain fail-closed.

Generated Dart and both Canvas projections construct the real Flutter
ConstrainedBox. Flutter therefore owns enforcement of its additional constraints
against the incoming parent constraints, including authentic failures when an
expanding axis is used under an unbounded parent. A childless zero-size node
keeps its real layout behind a non-layout-affecting bounded Designer
selection/drop target. Catalog, validator, generation, Properties, Canvas
model/view/drop, DnD/slot/move commands and their closed-contract tests share
this exact definition.

The earlier BoxConstraints representation could encode finite minima and
finite-or-unbounded maxima, but not the expanding `∞..∞` state. The exported
semantic domain now models finite or positive-infinity values on all four
bounds. Canonical `.fd` JSON uses a number for a finite bound and `null` for
positive infinity. That new persisted meaning advances `.fd` to schema v7;
schema-v1 through schema-v6 documents migrate losslessly and are emitted as v7
only after an admitted edit. The changed exported value domain advances the
contributor Catalog API to 6. Allowing nullable minima in the runner payload
advances Canvas model protocol to v12. NBFC framing and Canvas control/wire
remain version 1.

ConstrainedBox adds one non-Scaffold row, one ordinary source and one any-widget
destination to ADR-049. At that milestone the surface was 647 writable rows across 23
widgets, 630 outside Scaffold and 18 const-constructor definitions. Twenty-three
sources across 22 any-widget and two trait-bound destinations form 552
candidates. The new target admits every old source except Expanded; the new
ConstrainedBox source enters all 22 any-widget destinations but neither
trait-bound destination. This contributes 43 accepted and three rejected cells,
producing 488 accepted and 64 rejected overall. Expanded remains valid only as
a direct Row/Column child, and only AppBar satisfies the trait-bound
destinations. The Layout Palette then contained 15 items.

The practical Material/Base Designer backlog was then 23/92 complete, with 69
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. UnconstrainedBox became the next complete vertical slice.

## ADR-051 — UnconstrainedBox completes optional axis-retention layout

Accepted. The canonical built-in is the const
[`flutter.widgets.UnconstrainedBox(...)`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 107, immediately after ConstrainedBox. It supports
Properties, Create, native and exact-Web Canvas, Palette/tree/Canvas DnD,
same-tree movement, exact-slot management, deterministic Dart generation,
Save/reopen and chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and
32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface is one
optional single any-widget `child` plus optional `TextDirection textDirection`,
`AlignmentGeometry alignment`, `Axis constrainedAxis` and `Clip clipBehavior`.
The catalog orders the four writable rows as `textDirection`, `alignment`,
`constrainedAxis`, `clipBehavior`, with `child` as its slot. A new prototype
stores no properties and an empty child slot. Omission therefore preserves
`Alignment.center`, no retained constrained axis and `Clip.none`; omitted
`textDirection` uses the ambient `Directionality` whenever directional alignment
requires resolution. The closed domains admit `ltr`/`rtl`, finite physical or
directional alignment, `horizontal`/`vertical`, and all four reviewed Clip
values. Raw Dart and other object graphs remain fail-closed.

Generated Dart and both Canvas projections construct the real Flutter
UnconstrainedBox. Flutter removes the incoming constraints on both axes when
`constrainedAxis` is omitted and retains exactly the selected horizontal or
vertical axis when present. Alignment and clipping remain framework-owned. A
childless zero-size node keeps its real layout behind a bounded,
non-layout-affecting Designer selection/drop target. Catalog, validator,
generation, Properties, Canvas model/view/drop, DnD/slot/move commands and their
closed-contract tests share this exact definition.

UnconstrainedBox adds four non-Scaffold rows, one ordinary source and one
any-widget destination to ADR-050. At that milestone the surface was 651 writable rows
across 24 widgets, 634 outside Scaffold and 19 const-constructor definitions.
Twenty-four sources across 23 any-widget and two trait-bound destinations form
600 candidates. The new target admits every old source except Expanded; the new
UnconstrainedBox source enters all 23 any-widget destinations but neither
trait-bound destination. This contributes 45 accepted and three rejected cells,
producing 533 accepted and 67 rejected overall. Expanded remains valid only as
a direct Row/Column child, and only AppBar satisfies the trait-bound
destinations. The Layout Palette then contained 16 items.

The practical Material/Base Designer backlog was then 24/92 complete, with 68
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. LimitedBox became the next complete vertical slice. The
existing encodings cover this contract,
so `.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-052 — LimitedBox completes unbounded-axis maximum limits

Accepted. The canonical built-in is the const
[`flutter.widgets.LimitedBox(...)`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 108 between UnconstrainedBox and Stack. It supports
Properties, Create, native and exact-Web Canvas, Palette/tree/Canvas DnD,
same-tree movement, exact-slot management, deterministic Dart generation,
Save/reopen and chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and
32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface has optional
`double maxWidth`, optional `double maxHeight` and one optional single any-widget
`child`, at constructor positions zero through two. Both numeric fields admit
finite non-negative values. Omission is the canonical representation of the
framework's `double.infinity` default, so explicitly persisted positive infinity
would add no distinct state and is rejected together with negative, NaN and
other non-finite numeric input. A new prototype stores no properties and an
empty child slot. Generated Dart omits unset limits and otherwise emits them in
constructor order before `child`.

Generated Dart and both Canvas projections construct the real Flutter
LimitedBox. Flutter applies a configured maximum on an axis only when the
incoming maximum constraint on that axis is unbounded; a bounded incoming axis
passes through unchanged. A childless zero-size node keeps its real layout
behind a bounded, non-layout-affecting Designer selection/drop target. Catalog,
validator, generation, Properties, Canvas model/view/drop, DnD/slot/move
commands and their closed-contract tests share this exact definition.

LimitedBox adds two non-Scaffold rows, one ordinary source and one any-widget
destination to ADR-051. At that milestone the surface was 653 writable rows across 25
widgets, 636 outside Scaffold and 20 const-constructor definitions. Twenty-five
sources across 24 any-widget and two trait-bound destinations form 650
candidates. The new target admits every old source except Expanded; the new
LimitedBox source enters all 24 any-widget destinations but neither trait-bound
destination. This contributes 47 accepted and three rejected cells, producing
580 accepted and 70 rejected overall. Expanded remains valid only as a direct
Row/Column child, and only AppBar satisfies the trait-bound destinations. The
Layout Palette then contained 17 items.

The practical Material/Base Designer backlog was then 25/92 complete, with 67
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. OverflowBox became the next complete vertical slice. The
existing encodings cover this contract,
so `.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-053 — OverflowBox completes bounded constraint override and fit

Accepted. The canonical built-in is the const
[`flutter.widgets.OverflowBox(...)`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 109 between LimitedBox and Stack. It supports
Properties, Create, native and exact-Web Canvas, Palette/tree/Canvas DnD,
same-tree movement, exact-slot management, deterministic Dart generation,
Save/reopen and chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and
32 pixels provide its Palette identity.

The reviewed non-`key` Flutter 3.44.8 constructor surface has optional
`AlignmentGeometry alignment`, `double minWidth`, `double maxWidth`,
`double minHeight`, `double maxHeight`, `OverflowBoxFit fit` and one optional
single any-widget `child`, at constructor positions zero through six. The four
constraint overrides admit only finite non-negative `DoubleValue` values; each
omitted bound inherits the corresponding constraint from the parent. A present
minimum may not exceed its matching maximum. Explicit infinity, NaN, negative
values and integer wire kinds fail closed. Omitted alignment and fit preserve
`Alignment.center` and `OverflowBoxFit.max`; the only other fit value is
`deferToChild`. The widget class is owned by `package:flutter/widgets.dart`,
while the fit enum's exact Dart symbol and generated import are owned by
`package:flutter/rendering.dart`. A new prototype stores no properties and an
empty child slot.

Generated Dart emits present arguments in constructor order. Both Canvas
projections construct the real Flutter OverflowBox. The child receives the
selected constraint replacements and may overflow the parent; physical
alignment remains physical while directional alignment resolves through LTR or
RTL. `OverflowBoxFit.max` sizes the render object to the parent maximum, while
`deferToChild` follows the child's constrained size when it does not overflow.
A childless or zero-size node keeps its real layout behind a bounded,
non-layout-affecting Designer selection/drop target. Catalog, validator,
generation, Properties, Canvas model/view/drop, DnD/slot/move commands and
their closed-contract tests share this exact definition.

OverflowBox adds six non-Scaffold rows, one ordinary source and one any-widget
destination to ADR-052. The current surface is 659 writable rows across 26
widgets, 642 outside Scaffold and 21 const-constructor definitions. Twenty-six
sources across 25 any-widget and two trait-bound destinations form 702
candidates. The new target admits every old source except Expanded; the new
OverflowBox source enters all 25 any-widget destinations but neither trait-bound
destination. This contributes 49 accepted and three rejected cells, producing
629 accepted and 73 rejected overall. Expanded remains valid only as a direct
Row/Column child, and only AppBar satisfies the trait-bound destinations. The
Layout Palette now contains 18 items.

The practical Material/Base Designer backlog is now 26/92 complete, with 66
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. No later widget has an explicit order; the next admission must
again be a complete vertical slice. Existing encodings cover this contract, so
`.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-054 — Flexible completes loose and tight Flex allocation

Accepted. The canonical built-in is the const
[`flutter.widgets.Flexible(...)`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 130 immediately after Expanded. It supports
Properties, Create, native and exact-Web Canvas, Palette/tree/Canvas DnD,
same-tree movement, exact-slot replacement, deterministic Dart generation,
Save/reopen and chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and
32 pixels provide its Palette identity.

The reviewed non-`key` Flutter 3.44.8 constructor surface has optional
non-negative portable integer `flex`, optional `FlexFit fit`, and one required
single any-widget `child`, at constructor positions zero through two. Omission
preserves `flex: 1` and `FlexFit.loose`; explicit fit admits only `loose` or
`tight`. Zero flex is valid and makes the child inflexible, so fit has no layout
effect. Raw Dart, negative integers and values above the shared exact portable
integer ceiling remain fail-closed. A detached prototype stores no properties
and an empty required child slot, but it is never inserted directly.

Palette, tree and Canvas creation instead wrap one existing direct child of
`Row.children` or `Column.children` with one atomic `WrapWidget` command. The
same direct-parent rule applies to move and validation. Expanded and Flexible
cannot wrap either wrapper type: nesting would place the inner parent-data
widget under another ParentDataWidget instead of directly under Row or Column.
The occupied required child is replacement-only and cannot be cleared.

Generated Dart and both Canvas projections construct the real Flutter Flexible.
Positive loose flex lets the child remain smaller than its allocated main-axis
space, positive tight flex requires it to fill that allocation, and zero flex
uses the child's own main-axis size. The Canvas keeps Flexible as the direct
ParentDataWidget below Row or Column and places Designer instrumentation inside
its child, preserving Flutter's parent-data path. Catalog, validator,
generation, Properties, Canvas model/view/drop, DnD/slot/move commands and their
closed-contract tests share this exact definition.

Flexible adds two non-Scaffold rows, one parent-restricted source and one
required non-insertable child slot to ADR-053. At that milestone the surface was 661
writable rows across 27 widgets, 644 outside Scaffold and 22 const-constructor
definitions. Twenty-seven sources across the unchanged 25 insertable any-widget
plus two trait-bound destinations formed 729 candidates. The 25 ordinary sources
entered all 25 any-widget destinations, AppBar additionally entered both trait
destinations, and Expanded plus Flexible each entered only Row.children and
Column.children. This produced 631 accepted and 98 rejected cells. The Layout
Palette contained 19 items.

The practical Material/Base Designer backlog was then 27/92 complete, with 65
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. Existing encodings cover this contract, so
`.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-055 — Spacer completes childless proportional Flex spacing

Accepted. The canonical built-in is the const
[`flutter.widgets.Spacer(...)`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 140 immediately after Flexible. It supports
Properties, Create, native and exact-Web Canvas, Palette/tree/Canvas DnD,
same-tree movement, deterministic Dart generation, Save/reopen and
chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and 32 pixels
provide its Palette identity.

The reviewed non-`key` Flutter 3.44.8 constructor surface has one optional
positive portable integer `flex` at constructor position zero and no child or
other slot. Omission preserves Flutter's `flex: 1`; explicit values range from
one through the shared exact portable integer ceiling. Zero, negative values,
over-limit integers and raw Dart remain fail-closed. A detached prototype stores
no properties or slots and is safe for direct insertion only after the complete
placement rule accepts its destination.

Palette, tree and Canvas creation append a Spacer prototype only to direct
`Row.children` or `Column.children`; unlike Expanded and Flexible, Spacer never
wraps an existing child. The same direct-parent rule applies to move and
validation. Expanded and Flexible cannot wrap Spacer because Spacer's own
`build` creates an Expanded parent-data path that must still reach Row or Column
without another ParentDataWidget in between.

Generated Dart constructs the real Flutter Spacer. Both Canvas projections also
keep the real Spacer directly under Row or Column. Because an outer
Semantics/gesture/paint RenderObject would invalidate Spacer's internal Expanded
path, the Canvas uses only transparent component wrappers around Spacer and
publishes Designer selection, hit target and outline through the surface overlay
geometry. Catalog, validator, generation, Properties, Canvas model/view/drop,
DnD/move commands and their closed-contract tests share this exact definition.

Spacer adds one non-Scaffold row, one parent-restricted insertion source and no
destination slot to ADR-054. At that milestone the surface was 662 writable rows across 28
widgets, 645 outside Scaffold and 23 const-constructor definitions. Twenty-eight
sources across the unchanged 25 insertable any-widget plus two trait-bound
destinations form 756 candidates. The 25 unrestricted sources enter all 25
any-widget destinations, AppBar additionally enters both trait destinations,
Expanded and Flexible each enter only Row.children and Column.children as
wrappers, and Spacer enters those two slots as a leaf. This produces 633
accepted and 123 rejected cells. The Layout Palette contains 20 items.

The practical Material/Base Designer backlog was then 28/92 complete, with 64
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. Baseline became the next complete vertical slice. Existing encodings cover this contract, so
`.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-056 — Baseline completes typed baseline positioning

Accepted. The canonical built-in is the const
[`flutter.widgets.Baseline(...)`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 150 immediately after Spacer. It supports Properties,
Create, native and exact-Web Canvas, Palette/tree/Canvas DnD, same-tree
movement, exact-slot management, deterministic Dart generation, Save/reopen
and chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and 32 pixels
provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface has required
`double baseline`, required `TextBaseline baselineType`, and one optional single
any-widget `child`, at constructor positions zero through two. The numeric value
accepts every finite `DoubleValue`, including signed values as Flutter does;
integer wire kinds, NaN, infinity and raw Dart fail closed. The enum admits only
`alphabetic` and `ideographic` from the widgets library. Flutter defines no
constructor defaults, so a detached Designer prototype stores the reviewed
visible starting values `baseline: 24.0` and
`baselineType: TextBaseline.alphabetic`, plus an empty child slot. Required
properties cannot be reset to omission but remain editable through their typed
number and enum editors.

Generated Dart and both Canvas projections construct the real Flutter Baseline.
Flutter shifts a present child against its requested baseline and falls back to
the child's bottom when that child exposes no matching baseline. A childless
Baseline keeps its framework `constraints.smallest` layout (often zero) while
receiving a bounded, non-layout-affecting Designer selection and empty-child
drop target. Catalog,
validator, generation, Properties, Canvas model/view/drop, DnD/slot/move
commands and their closed-contract tests share this exact definition.

Baseline adds two non-Scaffold property rows, one ordinary source and one
insertable any-widget destination to ADR-055. At that milestone the surface was
664 writable rows across 29 widgets, 647 outside Scaffold and 24
const-constructor definitions.
Twenty-nine sources across 26 any-widget plus two trait-bound destinations form
812 candidates. The new Baseline destination accepts 25 old unrestricted
sources and rejects Expanded, Flexible and Spacer; the new Baseline source
enters all 26 any-widget destinations and neither trait-bound destination. This
adds 51 accepted and five rejected cells, producing 684 accepted and 128
rejected overall. The Layout Palette contains 21 items.

The practical Material/Base Designer backlog was then 29/92 complete, with 63
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. IntrinsicHeight became the next complete vertical slice.
Existing encodings cover this contract, so
`.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-057 — IntrinsicHeight completes property-free intrinsic height sizing

Accepted. The canonical built-in is the const
[`flutter.widgets.IntrinsicHeight(...)`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 160 immediately after Baseline. It supports
Properties and exact-slot presentation, Create, native and exact-Web Canvas,
Palette/tree/Canvas DnD, same-tree movement, deterministic Dart generation,
Save/reopen and chronological Undo/Redo. Reviewed light/dark SVG icons at 16 and
32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface contains no
writable properties and one optional single any-widget `child` at constructor
position zero. Detached Designer prototypes therefore contain only an empty
child slot. Catalog, validator, generation, Properties, Canvas model/view/drop
and DnD/slot/move commands share this exact property-free definition.

Generated Dart and both Canvas projections construct the real Flutter
IntrinsicHeight. Flutter performs a speculative pass to obtain the child's
maximum intrinsic height, then lays the child out within the parent's
constraints. This is relatively expensive and can be O(N²) in tree depth, so
the performance warning is part of the Palette and slot descriptions. An empty
or collapsed node retains real framework layout while a bounded,
non-layout-affecting Designer target supplies selection and empty-child drop.

IntrinsicHeight adds no writable rows, one ordinary source and one insertable
any-widget destination to ADR-056. The current surface remains 664 writable
rows, with 647 outside Scaffold, across 30 widgets and 25 const-constructor
definitions. Thirty sources across 27 any-widget plus two trait-bound
destinations form 870 candidates. The new IntrinsicHeight destination accepts
26 old unrestricted sources and rejects Expanded, Flexible and Spacer; the new
IntrinsicHeight source enters all 27 any-widget destinations and neither
trait-bound destination. This adds 53 accepted and five rejected cells,
producing 737 accepted and 133 rejected overall. The Layout Palette contains 22
items.

The practical Material/Base Designer backlog was then 30/92 complete, with 62
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. IntrinsicWidth became the next complete vertical slice.
Existing encodings cover this contract, so
`.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-058 — IntrinsicWidth completes stepped intrinsic width sizing

Accepted. The canonical built-in is the const
[`flutter.widgets.IntrinsicWidth(...)`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 170 immediately after IntrinsicHeight. It supports
typed Properties and exact-slot presentation, Create, native and exact-Web
Canvas, Palette/tree/Canvas DnD, same-tree movement, deterministic Dart
generation, Save/reopen and chronological Undo/Redo. Reviewed light/dark SVG
icons at 16 and 32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface contains
optional named finite non-negative `double stepWidth`, optional named finite
non-negative `double stepHeight` and one optional single any-widget `child`, in
that constructor order. Detached Designer prototypes omit both step values and
start with an empty child slot. Null and explicit `0.0` remain distinct model,
history and generated-Dart values. Flutter internally treats zero like null for
the corresponding render-object step; a positive step rounds the child's
intrinsic extent upward to the next multiple. Negative, non-finite and
wrong-typed values fail closed.

Generated Dart and both Canvas projections construct the real Flutter
IntrinsicWidth under the parent's constraints. Flutter performs a speculative
intrinsic layout pass before final layout. This is relatively expensive and can
be O(N²) in tree depth, so the performance warning and step semantics are part
of the Palette, property and slot descriptions. An empty or collapsed node
retains real framework layout while a bounded, non-layout-affecting Designer
target supplies selection and empty-child drop.

IntrinsicWidth adds two writable rows, one ordinary source and one insertable
any-widget destination to ADR-057. At that milestone the surface contained 666 writable
rows, with 649 outside Scaffold, across 31 widgets and 26 const-constructor
definitions. Thirty-one sources across 28 any-widget plus two trait-bound
destinations form 930 candidates. The new IntrinsicWidth destination accepts
27 old unrestricted sources and rejects Expanded, Flexible and Spacer; the new
IntrinsicWidth source enters all 28 any-widget destinations and neither
trait-bound destination. This adds 55 accepted and five rejected cells,
producing 792 accepted and 138 rejected overall. The Layout Palette contains 23
items.

The practical Material/Base Designer backlog was then 31/92 complete, with 61
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. Offstage became the next complete vertical slice. Existing encodings cover this contract, so
`.fd` remains v7, Catalog API remains 6, Canvas model remains v12, and NBFC
framing plus Canvas control/wire remain version 1.

## ADR-059 — Offstage completes active-but-hidden layout

Accepted. The canonical built-in is the const
[`flutter.widgets.Offstage(...)`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 180 immediately after IntrinsicWidth. It supports
typed Properties and exact-slot presentation, Create, native and exact-Web
Canvas, Palette/tree/Canvas DnD, same-tree movement, deterministic Dart
generation, Save/reopen and further editing, and chronological Undo/Redo.
Reviewed light/dark SVG icons at 16 and 32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface contains the
optional named boolean `offstage`, whose constructor default is `true`, and one
optional single any-widget `child`, in that constructor order. Detached
Designer prototypes omit `offstage` and start with an empty child slot.
Omission and explicit `true` remain distinct model, history and generated-Dart
states even though they have the same runtime effect; explicit `false` is also
accepted. Wrong-typed values and raw Dart fail closed.

Generated Dart and both Canvas projections construct the real Flutter Offstage.
When `offstage` is omitted or true, Flutter lays the child out but suppresses
painting, hit testing and semantics; the widget reports the parent's minimum
permitted size, normally zero under loose constraints. The child remains active
and focusable and animations continue to consume resources. Palette, property
and slot descriptions expose those semantics and recommend removing a subtree
for long-term hiding when background work is undesirable. The Designer keeps
selection and drop instrumentation outside the Offstage effect and adds a
bounded 36x36 target only when the real result is zero-sized, without changing
Flutter layout.

Offstage adds one writable row, one ordinary source and one insertable
any-widget destination to ADR-058. The surface at that milestone contained 667 writable
rows, with 650 outside Scaffold, across 32 widgets and 27 const-constructor
definitions. Thirty-two sources across 29 any-widget plus two trait-bound
destinations form 992 candidates. The new Offstage destination accepts 28 old
unrestricted sources and rejects Expanded, Flexible and Spacer; the new
Offstage source enters all 29 any-widget destinations and neither trait-bound
destination. This adds 57 accepted and five rejected cells, producing 849
accepted and 143 rejected overall. The Layout Palette contains 24 items.

At that milestone the practical Material/Base Designer backlog was 32/92
complete, with 60 remaining. This is a project planning target, not a normative
complete list of Flutter widgets. Existing encodings covered this contract, so
`.fd` remained v7, Catalog API remained 6 and Canvas model remained v12; NBFC
framing plus Canvas control/wire remained version 1.

## ADR-060 — SizedOverflowBox adds atomic Size and child overflow

Accepted. The canonical built-in is the const
[`flutter.widgets.SizedOverflowBox(...)`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 190 immediately after Offstage. It supports typed
Properties and exact-slot presentation, Create, native and exact-Web Canvas,
Palette/tree/Canvas DnD, same-tree movement, deterministic Dart generation,
Save/reopen and further editing, and chronological Undo/Redo. Reviewed
light/dark SVG icons at 16 and 32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface contains
required named `Size size`, optional named `AlignmentGeometry alignment` with
framework default `Alignment.center`, and one optional single any-widget
`child`, in that constructor order. The model adds a first-class atomic `size`
value with finite non-negative width and height. A detached prototype stores
`Size(100, 100)` so the required widget is visible and immediately editable;
alignment remains omitted and the child slot starts empty. Wrong kinds,
missing/extra Size fields, negative or non-finite dimensions and raw Dart fail
closed. Physical and directional alignment values remain fully supported.

Generated Dart and both Canvas projections construct the real Flutter
SizedOverflowBox. Its RenderObject constrains only the requested outer size to
the incoming parent constraints, passes those original constraints unchanged
to its child and aligns the resulting child inside or outside the box. Painting
may overflow, while Flutter hit testing remains limited to the parent's bounds.
The Designer adds no sizing wrapper; it exposes a bounded 36x36 selection and
empty-child drop target only when the real result is zero-sized.

SizedOverflowBox adds two writable rows, one ordinary source and one insertable
any-widget destination to ADR-059. The current surface contains 669 writable
rows, with 652 outside Scaffold, across 33 widgets and 28 const-constructor
definitions. Thirty-three sources across 30 any-widget plus two trait-bound
destinations form 1,056 candidates. The new destination accepts 29 old
unrestricted sources and rejects Expanded, Flexible and Spacer; the new source
enters all 30 any-widget destinations and neither trait-bound destination. This
adds 59 accepted and five rejected cells, producing 908 accepted and 148
rejected overall. The Layout Palette contains 25 items.

The practical Material/Base Designer backlog is now 33/92 complete, with 59
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. The first-class Size value advances `.fd` schema to v8 and the
contributor Catalog API to 7; Canvas model advances to v13. NBFC framing plus
Canvas control/wire remain version 1.
