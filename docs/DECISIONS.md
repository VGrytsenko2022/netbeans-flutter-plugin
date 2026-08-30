# Architecture Decisions

Status note: ADR-024 and ADR-027 supersede the earlier provisional statements that
`PUBLIC_MUTATION_UI_ENABLED` remains `false`. Their persistence and lifecycle
contracts remain accepted. ADR-025 records the historical Text-only and later
six-source insertion milestones; ADR-030 records the subsequent seven-widget
`SizedBox` milestone, ADR-031 records the eight-widget `Icon` milestone,
ADR-032 records the nine-widget `AppBar` milestone, and ADR-033 records the
ten-widget `ElevatedButton` milestone and its 140 candidate Palette/DnD cells,
122 accepted and 18 rejected. ADR-034 extends exact named-slot management with
atomic replacement and clear-all commands. ADR-035 governs the current writable
surface: 514 typed rows across the same ten widgets, including 17 closed scalar
`Scaffold` fields. ADR-036 authorizes the Windows-only capability-gated inline
editor for one selected existing `Text.data`; its deterministic product slice
is accepted while physical CJK IME acceptance remains open. ADR-028 authorizes
same-tree movement of an existing non-root widget, and ADR-029 authorizes the
first exact named-slot management slice.
None authorizes cross-form movement, arbitrary native Canvas mutation,
unreviewed slots or additional Palette/DnD types.

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

Accepted for 0.1.3. The main NetBeans module exports exactly `dev.flutter.netbeans.designer.catalog` and `dev.flutter.netbeans.designer.model`, because the catalog metadata constructors expose model identifier and value types in their public signatures. Contributor NBMs use a normal specification dependency on `dev.flutter.netbeans.netbeans.plugin`, register `WidgetCatalogContributor` through the default Lookup, and reuse the host module's single packaged `flutter-designer.jar`; an extension must never bundle another copy. Codec, validation and NetBeans-edge packages remain private. API 1 was frozen for 0.1.3-compatible patch builds. The direction-aware edge-insets model deliberately established `API_VERSION == 2`, because adding a permitted subtype to the exported sealed `PropertyValue` surface is source-incompatible; API-1 contributors are rejected explicitly rather than loaded under a changed contract. Typed `IconDataValue` and `PropertyValueKind.ICON_DATA` establish the next incompatible boundary at `API_VERSION == 3`; API-1 and API-2 contributors fail closed before their definitions are loaded. This is not yet a permanent 1.0 compatibility promise. Further incompatible evolution should move the SPI to a dedicated module/new package boundary rather than silently breaking extensions behind an existing API version.

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
presentation and Flutter-side hit-test epochs; they do not identify an image
payload. The identities fence delayed acknowledgements and intents across
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
framing. ADR-024, ADR-027, ADR-030, ADR-031, ADR-032 and ADR-033 make 497 catalog-backed
non-`Scaffold` Properties fields writable, including the 59-leaf Text
projection, two `SizedBox` dimensions, 13 typed Icon constructor properties and
120 grouped AppBar leaves plus 286 ElevatedButton leaves.
ADR-025 historically made only built-in `Text` publicly draggable and later
admitted six sources; ADR-030 records the seven-source stage and ADR-031 records
the eight-source stage. ADR-032 supersedes those surface counts with the
nine-source, 117-candidate capability matrix (101 accepted and 16 rejected),
and ADR-033 supersedes it with ten sources and 140 candidates (122 accepted and
18 rejected). Same-tree
existing-widget movement is separately enabled by ADR-028. A separate post-handshake runtime control codec publishes one exact
validated revision, admits its layout acknowledgement, synchronizes stable-ID
selection and capability-gates the narrow palette-drop intent. The canonical
protocol-v9 model payload accepts only exact reviewed Canvas-capable built-ins:
`Scaffold`, `AppBar`, `Column`, `Row`, `Text`, `Icon`, `Padding`, `Center` and
`SizedBox`, plus `ElevatedButton`; the
isolated runner independently enforces the same schema and receives neither
project code nor file authority. `CATALOG_JSON` remains reserved for a future
versioned catalog contract.

The Windows edge has a real heavyweight AWT HWND host, exact
PID/parent/class/style validation for runner and `FLUTTERVIEW` children, a
bounded SDK-keyed build cache and an isolated child-runner lifecycle per open
`.fd` Design MultiView. Cache reuse requires a bounded SHA-256 manifest for the
complete launch runtime, and deterministic tests fence
close/build/launch/attach/exit races plus two simultaneous sessions. The native
Canvas now renders the validated ten-widget model for Mobile, Tablet, Desktop
and Web responsive preview profiles and synchronizes selection with the
Explorer/Nodes tree and standard Properties window. The Palette exposes exactly
those ten Create-capable definitions, and the DnD-capable set uses the reviewed
140-cell candidate matrix across twelve any-widget and two trait-bound
destination slots; 122 cells are accepted and 18 rejected. No image or
pixel-transfer frame kind exists.
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

The internal host foundation is implemented but is not product-routed.
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
That smoke does not authorize provider/product selection, build/cache/session
routing, Web `CanvasEngineIdentity`, production Retry/crash recovery, or the
assembled NetBeans model/layout/selection, DPI and isolation matrix. The backend
therefore remains unavailable in the product until those independent gates pass.
The host foundation now supplies an asynchronous pre-peer-loss barrier, a
poisoned state with exact-handle teardown retry, matching-PID
`BrowserProcessExited` proof and retry-safe partial UDF deletion backed by an
external per-session ownership marker. Failed native startup also returns any
retained exact handle to Java; a null failed-create handle is release proof,
while an unconfirmed/malformed result quarantines its callback and UDF.
The assembled MultiView now talks to one backend-neutral Canvas-session
contract and chooses a route through a pure selector. Production fixes that
selector to the existing native route for all targets, including the bounded
Web responsive preview. The exact-Web selector branch is test-only, reports a
concrete unavailable state when no admitted backend exists and must never use a
native-engine fallback. This establishes an ownership/routing seam without
claiming Web product readiness.
Windows cleanup holds stable FileId handles that deny delete sharing for the
parent/root/marker, denies marker writes, and deletes the verified root and
marker by handle with the marker last.
Product routing remains disabled until
the NetBeans owner awaits that barrier and the assembled acceptance matrix
passes.
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
against the selected stable widget ID and the immutable revision token captured
when the Node tree was built. A shared one-shot fence prevents a second editor
callback from reusing that token. Stale, closed, conflicted, unsupported or
concurrently changing pairs fail closed with the operation, target and reason.
The candidate must pass catalog/relationship validation, deterministic Dart
generation and Dart analysis before the existing `PairSaveCoordinator` adopts
it; the combined Source/model Undo/Redo and Save lifecycle remain authoritative.

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
an optional blur mask. Shaders, color/image filters and raw Dart are excluded.
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

Accepted for the current ten-widget Properties surface. `Scaffold` exposes
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
non-`Scaffold` definitions, these 17 fields make the current exact total 514
writable rows across ten widgets. This slice changes no Palette publication,
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
