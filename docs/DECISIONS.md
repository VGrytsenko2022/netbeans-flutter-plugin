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
ADR-058 adds `IntrinsicWidth`, ADR-059 adds `Offstage`, ADR-060 adds
`SizedOverflowBox`, and ADR-061 adds `Transform`. ADR-062 supersedes only
ADR-045's requirement that Image creation be blocked until a real declared
asset exists. ADR-063 records the completed `RotatedBox` milestone, ADR-064
records `ListBody`, ADR-065 adds `OverflowBar`, and ADR-066 establishes the
historical `GridView.count` surface. ADR-067 establishes the historical
`SingleChildScrollView` surface, ADR-068 records the historical `ColoredBox`
surface, ADR-069 records the historical `SafeArea` surface, ADR-070 records the
historical `Placeholder` surface, ADR-071 records the historical
`Directionality` surface, ADR-072 records the historical `DecoratedBox`
surface, ADR-073 establishes the historical `ExcludeSemantics` surface,
ADR-074 establishes the historical `IndexedStack` surface, ADR-075 establishes
the historical `ClipRect` surface, ADR-076 establishes the historical
`ClipOval` surface, ADR-077 establishes `ClipRRect`, ADR-078 adds `ClipPath`,
ADR-079 adds `ClipRSuperellipse`, ADR-080 adds `PhysicalModel`, ADR-081 adds
`PhysicalShape`, ADR-082 adds `RepaintBoundary`, ADR-083 adds `IgnorePointer`,
ADR-084 adds `AbsorbPointer`, ADR-085 adds `BlockSemantics`, ADR-086 adds
`MergeSemantics`, ADR-087 adds `IndexedSemantics`, ADR-088 adds `ExcludeFocus`,
ADR-089 adds `ExcludeFocusTraversal`, ADR-090 adds `Visibility`, ADR-091 adds
`TickerMode`, ADR-092 adds `DefaultTextHeightBehavior`, ADR-093 adds
`DefaultSelectionStyle`, ADR-094 adds `IconTheme`, ADR-095 adds `ImageIcon`, ADR-096 adds `Divider`, ADR-097 adds `VerticalDivider`, ADR-098 adds `Card`, ADR-099 adds `Badge`, ADR-100 adds `CircleAvatar`, ADR-101 adds `LinearProgressIndicator`, ADR-102 adds `CircularProgressIndicator`, ADR-103 adds `RefreshProgressIndicator`, ADR-104 adds `RefreshIndicator`, and ADR-105 establishes the historical `TextButton`
surface: 1454 typed rows across seventy-seven widgets, seventy-one const-constructor
definitions and 4,774 Palette/DnD candidates, including 4,448 accepted and 326
rejected cells. Its 1437 non-`Scaffold` rows sat beside the 17 closed scalar
`Scaffold` fields. ADR-106 adds OutlinedButton, ADR-107 adds FilledButton,
ADR-108 adds FloatingActionButton, ADR-109 adds IconButton, ADR-110 adds Checkbox
and ADR-111 adds Switch: the current surface is 83 widgets, 3383 rows and 5644 placement candidates. ADR-036
authorizes the Windows-only capability-gated inline
editor for one selected existing `Text.data`; its deterministic product slice
is accepted while physical CJK IME acceptance remains open. ADR-028 authorizes
same-tree movement of an existing non-root widget, and ADR-029 authorizes the
first exact named-slot management slice.
None authorizes cross-form movement, arbitrary native Canvas mutation,
unreviewed slots or Palette/DnD types outside the ADR-109 catalog.

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
established `API_VERSION == 7`; and ADR-061's atomic signed Offset value
established `API_VERSION == 8`. ADR-069 adds the exported
`EdgeInsetsValues.directionalAllowed` component needed to distinguish
SafeArea's concrete physical `EdgeInsets` parameter from broader
`EdgeInsetsGeometry` parameters and established `API_VERSION == 9`.
`NullValue` established API 10, top-level typed border-radius geometry
established API 11, and ADR-077's typed Dart-object reference establishes API 12.
ADR-080's `BorderRadiusValues.directionalAllowed` physical-radius distinction
establishes the current `API_VERSION == 13`. API-1 through API-12 contributors fail closed
before their definitions are loaded. This is not yet a permanent 1.0
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
ADR-038, ADR-039 and ADR-040 through ADR-105 make 1437 catalog-backed
non-`Scaffold` Properties
fields writable, including the 59-leaf Text projection, two `SizedBox`
dimensions, 13 typed Icon constructor properties,
120 grouped AppBar leaves, 286 ElevatedButton leaves and the required
`AspectRatio.aspectRatio` value, all 13 `Container` constructor properties,
the two Opacity properties, three Align properties, three FractionallySizedBox
properties, four Stack properties, five IndexedStack properties, one Expanded property, 22 Image properties,
54 TextField properties, 17 ListView properties, nine Wrap properties, three
FittedBox properties, the required ConstrainedBox constraints property, four
UnconstrainedBox properties, two LimitedBox properties, six OverflowBox
properties, Flexible's two properties, Spacer's one property, Baseline's two
required properties plus optional child slot, IntrinsicHeight's property-free
optional child slot, IntrinsicWidth's two optional step properties plus optional
child slot, Offstage's optional boolean plus optional child slot, and
SizedOverflowBox's required structured size, optional alignment and optional
child slot, Transform's required matrix, optional origin/alignment/hit-test/
filter values and optional child slot, RotatedBox's required signed portable
`quarterTurns` plus optional child slot, and ListBody's optional axis/reversal
rows plus ordered children slot, OverflowBar's six layout rows plus ordered
children slot, GridView.count's 21 grouped rows plus ordered children slot,
SingleChildScrollView's 10 grouped rows plus optional child slot, and
ColoredBox's required theme-aware color, optional anti-aliasing and optional
child slot, SafeArea's six optional properties plus required child slot, and
Placeholder's four optional appearance/fallback properties plus optional child,
and Directionality's required `textDirection` plus its required child assembled
by the generic atomic wrapper workflow, DecoratedBox's two properties,
ExcludeSemantics' optional excluding flag, ClipRect's and ClipOval's optional
clip behavior, and ClipRRect's typed radius geometry, typed project Dart-object
reference and optional clip behavior, ClipPath's custom-clipper/shape helper and
clip behavior, ClipRSuperellipse's radius/clipper/clip behavior, PhysicalModel's
six properties, PhysicalShape's five properties, RepaintBoundary's property-free
child slot and IgnorePointer's and AbsorbPointer's two optional boolean fields each,
including their deprecated semantics overrides, plus BlockSemantics' optional
blocking boolean and child slot. MergeSemantics adds structural child-slot editing
without a scalar row. IndexedSemantics adds one required signed index and an
optional child slot. ExcludeFocus adds one optional excluding boolean and a
required child wrapper slot. ExcludeFocusTraversal adds one optional excluding
boolean and its required child wrapper slot. Visibility adds seven optional
booleans, required child and optional replacement slots. TickerMode adds required
enabled and optional forceFrames booleans with a required child wrapper slot.
DefaultTextHeightBehavior adds three optional leaves forming the required
TextHeightBehavior value and a required child wrapper slot. DefaultSelectionStyle
adds three optional SDK fields, a required Designer-only merge Boolean and required child.
IconTheme adds nine optional IconThemeData leaves, a required Designer-only merge
Boolean and required child, always generating the required data composite.
ImageIcon adds required nullable positional image and three optional named
size/color/semanticLabel rows, without a child slot.
Divider adds six optional geometry/appearance rows without child slots.
VerticalDivider adds six optional width/appearance rows without child slots.
Card adds 31 typed rows, all three constructor variants, ten built-in shapes,
an analyzed ShapeBorder reference alternative and an optional child slot.
Badge adds 41 typed rows, both constructors and optional Label/Child slots,
including the full TextStyle projection and state-aware count/label exclusivity.
CircleAvatar adds all nine optional scalar fields and one optional Child slot,
including both image layers, error callbacks and fixed/bounded/infinite radii.
LinearProgressIndicator adds all 13 optional scalar fields, including full stopped
color and project animation/controller branches, without slots or stored defaults.
CircularProgressIndicator adds all 14 optional Material fields and one required
variant, covering both constructors with typed animation, constraints and padding.
RefreshProgressIndicator adds all 12 optional fields, including explicit nullable
strokeWidth inheritance and separate physical/directional margin and padding.
RefreshIndicator adds all 12 SDK scalar fields across material/adaptive/noSpinner,
one required variant, typed refresh/status/predicate functions and a required child.
Scaffold separately contributes 17 rows, giving 1454 overall.
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
ADR-060 established the thirty-three-source, 1,056-candidate matrix (908
accepted and 148 rejected), ADR-061 established the thirty-four-source,
1,122-candidate matrix (969 accepted and 153 rejected), ADR-063 established the
thirty-five-source, 1,190-candidate matrix (1,032 accepted and 158 rejected), and
ADR-064 established the historical thirty-six-source, 1,260-candidate matrix
(1,097 accepted and 163 rejected), and ADR-065 established the historical
thirty-seven-source, 1,332-candidate matrix (1,164 accepted and 168 rejected).
ADR-066 established the historical thirty-eight-source, 1,406-candidate matrix
(1,233 accepted and 173 rejected), ADR-067 established the historical
thirty-nine-source, 1,482-candidate matrix (1,304 accepted and 178 rejected),
ADR-068 established the historical forty-source, 1,560-candidate matrix (1,377
accepted and 183 rejected), ADR-069 established the historical forty-one-source,
1,599-candidate matrix (1,414 accepted and 185 rejected), ADR-070 established
the historical forty-two-source, 1,680-candidate matrix (1,490 accepted and
190 rejected), ADR-071 established the historical forty-three-source,
1,720-candidate matrix (1,528 accepted and 192 rejected), ADR-072 established
the historical forty-four-source, 1,804-candidate matrix (1,607 accepted and
197 rejected), ADR-073 established the historical forty-five-source,
1,890-candidate matrix (1,688 accepted and 202 rejected), ADR-074 established
the historical forty-six-source, 1,978-candidate matrix (1,771 accepted and 207
rejected), ADR-075 established the historical forty-seven-source,
2,068-candidate matrix (1,856 accepted and 212 rejected), ADR-076 established
the historical forty-eight-source, 2,160-candidate matrix (1,943 accepted and
217 rejected), ADR-077 establishes the historical forty-nine-source,
2,254-candidate matrix (2,032 accepted and 222 rejected), ADR-078 adds the
fifty-source, 2,350-candidate matrix (2,123 accepted and 227 rejected), and
ADR-079 establishes the historical fifty-one-source, 2,448-candidate matrix
(2,216 accepted and 232 rejected). ADR-080 establishes the historical fifty-two-source,
2,548-candidate matrix (2,311 accepted and 237 rejected); ADR-081 establishes the
fifty-three-source matrix with 2,650 candidates (2,408 accepted / 242 rejected),
and ADR-082 establishes fifty-four sources with 2,754 candidates (2,507 accepted /
247 rejected). ADR-083 establishes the historical fifty-five-source, 2,860-candidate
matrix (2,608 accepted / 252 rejected). ADR-084 establishes the historical fifty-six-source,
2,968-candidate matrix (2,711 accepted / 257 rejected). ADR-085 establishes the historical
fifty-seven-source, 3,078-candidate matrix (2,816 accepted / 262 rejected). ADR-086
establishes the historical fifty-eight-source, 3,190-candidate matrix (2,923 accepted /
267 rejected). ADR-087 establishes the historical fifty-nine-source, 3,304-candidate
matrix (3,032 accepted / 272 rejected). ADR-088 establishes the historical sixty-source,
3,360-candidate matrix (3,086 accepted / 274 rejected). ADR-089 establishes the historical
sixty-one-source, 3,416-candidate matrix (3,140 accepted / 276 rejected). ADR-090
establishes the historical sixty-two-source, 3,534-candidate matrix
(3,253 accepted / 281 rejected). ADR-091 establishes the historical sixty-three-source,
3,591-candidate matrix (3,308 accepted / 283 rejected). ADR-092 establishes the historical
sixty-four-source, 3,648-candidate matrix (3,363 accepted / 285 rejected).
ADR-093 establishes the historical sixty-five-source, 3,705-candidate matrix
(3,418 accepted / 287 rejected).
ADR-094 establishes the historical sixty-six-source, 3,762-candidate matrix
(3,473 accepted / 289 rejected).
ADR-095 establishes the historical sixty-eight-source, 3,819-candidate matrix
(3,528 accepted / 291 rejected).
ADR-096 establishes the historical sixty-eight-source, 3,876-candidate matrix
(3,583 accepted / 293 rejected).
ADR-097 establishes the historical sixty-nine-source, 3,933-candidate matrix
(3,638 accepted / 295 rejected).
ADR-098 establishes the historical seventy-source, 4,060-candidate matrix
(3,760 accepted / 300 rejected).
ADR-099 establishes the historical seventy-one-source, 4,260-candidate matrix
(3,952 accepted / 308 rejected).
ADR-100 establishes the historical seventy-two-source, 4,392-candidate matrix
(4,079 accepted / 313 rejected).
ADR-101 establishes the historical seventy-three-source, 4,453-candidate matrix
(4,138 accepted / 315 rejected).
ADR-102 establishes the historical seventy-four-source, 4,514-candidate matrix
(4,197 accepted / 317 rejected).
ADR-103 establishes the historical seventy-five-source, 4,575-candidate matrix
(4,256 accepted / 319 rejected).
ADR-104 establishes the historical seventy-six-source, 4,636-candidate matrix
(4,315 accepted / 321 rejected).
ADR-105 establishes the historical seventy-seven-source, 4,774-candidate matrix
(4,448 accepted / 326 rejected).
ADR-106 records the historical 78-source, 4,914-candidate matrix (4,583 accepted /
331 rejected), and ADR-107 records 79 sources and 5,135 candidates (4,796 accepted /
339 rejected). ADR-108 records the historical 80-source, 5,360-candidate matrix
(5,013 accepted / 347 rejected). ADR-109 records the historical 81-source,
5,508-candidate matrix (5,156 accepted / 352 rejected). ADR-110 records the
historical 82-source, 5,576-candidate matrix (5,222 accepted / 354 rejected).
ADR-111 establishes the current 83-source, 5,644-candidate matrix
(5,288 accepted / 356 rejected).
Same-tree existing-widget movement is separately
enabled by ADR-028.
A separate post-handshake runtime control codec publishes one exact
validated revision, admits its layout acknowledgement, synchronizes stable-ID
selection and capability-gates the narrow palette-drop intent. The canonical
protocol-v18 model payload accepts only exact reviewed Canvas-capable built-ins:
`Scaffold`, `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`, `Text`,
`Icon`, `Image`, `Padding`, `Center`, `Align`, `FractionallySizedBox`, `SizedBox`,
`AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
`IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`, `ListView`, `GridView.count`, `SingleChildScrollView`, `Wrap`, `FittedBox`,
`ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge`, `CircleAvatar`, `LinearProgressIndicator`, `CircularProgressIndicator`, `RefreshProgressIndicator`, `RefreshIndicator`, `TextButton`, `OutlinedButton`, `FilledButton`, `FloatingActionButton`, `IconButton`, `Checkbox`, `Switch`, `Container` and `Opacity`; the
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
Canvas now renders the validated eighty-three-widget model for Mobile, Tablet,
Desktop and Web responsive preview profiles and synchronizes selection with the
Explorer/Nodes tree and standard Properties window. The Palette exposes exactly
those eighty-three Create-capable definitions, and the DnD-capable set uses the
reviewed 5,644-cell candidate matrix across sixty-six insertable any-widget and two
trait-bound destination slots; 5,288 cells are accepted and 356 rejected.
Expanded and Flexible each enter only direct Row/Column wrapper targets, while
Spacer inserts only into direct Row/Column children. Expanded and Flexible's
required child slots are replacement-only rather than insertable. SafeArea,
Directionality, ExcludeFocus, ExcludeFocusTraversal, Visibility, TickerMode, DefaultTextHeightBehavior, DefaultSelectionStyle, IconTheme, RefreshIndicator, TextButton, OutlinedButton and IconButton use the same generic atomic required-slot wrapper
mode, with tree root/non-root and Canvas non-root-only targets; none can wrap Expanded,
Flexible or Spacer.
Canvas model
protocol v17's
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

Accepted historically; its creation-admission requirement is superseded by
ADR-062. The const leaf `flutter.widgets.Image` exposes the required
asset-only `image` provider and 21 optional reviewed callback, accessibility,
size, color/opacity, blend, fit/alignment/repeat, center-slice, direction,
playback, antialias and quality leaves. Network/file/memory/custom providers and
arbitrary callback expressions remain excluded. Palette, tree, Canvas and slot
Replace New Widget resolve the deterministic first sorted declared asset before
stable-ID allocation. Palette drag-source authorization now preflights the
current inventory before publishing the Image source to Canvas. Empty or
unavailable inventory blocks the drag and exposes its exact reason. Refreshing
or verifying state directs the user to wait and retry; a resolver failure directs
the user to resolve the reported failure and retry. Only a genuinely empty
usable inventory directs the user to add a PNG/JPEG/GIF/WebP file and declare it
in the `assets:` list of the existing `flutter:` block in `pubspec.yaml`. This
early check is a UX admission only: the mutation planner resolves the latest
inventory again at commit, so
removal or invalidation during an in-flight drag remains fail-closed. Every
failed check produces no command, document mutation or stable ID.

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
destination to ADR-052. At that milestone the surface was 659 writable rows across 26
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
any-widget destination to ADR-056. At that milestone the surface had 664 writable
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
any-widget destination to ADR-059. That milestone contained 669 writable
rows, with 652 outside Scaffold, across 33 widgets and 28 const-constructor
definitions. Thirty-three sources across 30 any-widget plus two trait-bound
destinations form 1,056 candidates. The new destination accepts 29 old
unrestricted sources and rejects Expanded, Flexible and Spacer; the new source
enters all 30 any-widget destinations and neither trait-bound destination. This
adds 59 accepted and five rejected cells, producing 908 accepted and 148
rejected overall. The Layout Palette contains 25 items.

The practical Material/Base Designer backlog was then 33/92 complete, with 59
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. The first-class Size value advances `.fd` schema to v8 and the
contributor Catalog API to 7; Canvas model advances to v13. NBFC framing plus
Canvas control/wire remain version 1.

## ADR-061 — Transform adds atomic Offset and paint-time geometry

Accepted. The canonical built-in is the const
[`flutter.widgets.Transform(...)`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
default constructor from `package:flutter/widgets.dart`, published in the
**Layout** Palette category at order 200 immediately after SizedOverflowBox,
matching Flutter's canonical Layout catalog. It supports typed Properties and
exact-slot presentation, Create, native and exact-Web Canvas,
Palette/tree/Canvas DnD, same-tree movement, deterministic Dart generation,
Save/reopen and further editing, and chronological Undo/Redo. Reviewed
light/dark SVG icons at 16 and 32 pixels provide its Palette identity.

The complete reviewed non-`key` Flutter 3.44.8 `Transform.new` surface contains
required named `Matrix4 transform`, optional named `Offset origin`, optional
named `AlignmentGeometry alignment`, optional named boolean
`transformHitTests` with framework default `true`, optional named
`FilterQuality filterQuality`, and one optional single any-widget `child`, in
constructor order. The model adds a first-class atomic `offset` value whose
`dx` and `dy` are signed finite Dart-representable doubles. A detached prototype
stores `Matrix4.identity()` so the required value is immediately editable;
origin, alignment, hit-test behavior and filter quality remain omitted. Wrong
kinds, missing/extra Offset fields, non-finite coordinates, invalid enum values
and raw Dart fail closed. Physical and directional alignment values remain
fully supported.

Generated Dart and both Canvas projections construct the real Flutter
Transform. Its RenderObject lays out the child without applying the matrix and
uses the child's ordinary size; the transform is applied during paint. Origin
and alignment are additive pivot inputs, optional filter quality is preserved,
and child hit testing follows the inverse paint transform when
`transformHitTests` is true; false retains the child's untransformed hit-test
coordinates. Designer selection and drop geometry follow the same effective
transform without changing Flutter layout and expose a bounded 36x36 target
only when the real result is zero-sized. Non-finite projected bounds,
projective-horizon crossings and non-invertible point projection omit the
synthetic Canvas hit region rather than inventing one; finite rotated/skewed
drop targeting verifies exact local containment instead of the transformed
axis-aligned bounding-box corners. Singular and otherwise valid finite matrices
remain legal Flutter values rather than being replaced by a Designer approximation.

`package:flutter/widgets.dart` re-exports `Matrix4`, but analyzer navigation
correctly resolves that class to its declaring
`package:vector_math/vector_math_64.dart` library outside the Flutter SDK root.
Generated occurrence metadata therefore retains the real declaring library and
pair-save does not misclassify Matrix4 as a Flutter-owned probe. The Flutter-SDK
trust boundary remains unchanged; no Pub Cache directory becomes trusted.

This slice intentionally models `Transform.new`. The `.rotate`, `.translate`,
`.scale` and `.flip` convenience constructors remain outside the admitted
constructor identity; their resulting effects remain expressible through the
editable Matrix4.

Transform adds five writable rows, one ordinary source and one insertable
any-widget destination to ADR-060. At that milestone the surface contained 674 writable
rows, with 657 outside Scaffold, across 34 widgets and 29 const-constructor
definitions. Thirty-four sources across 31 any-widget plus two trait-bound
destinations form 1,122 candidates. The new destination accepts 30 old
unrestricted sources and rejects Expanded, Flexible and Spacer; the new source
enters all 31 any-widget destinations and neither trait-bound destination. This
adds 61 accepted and five rejected cells, producing 969 accepted and 153
rejected overall. The Layout Palette contains 26 items.

The practical Material/Base Designer backlog is now 34/92 complete, with 58
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. The first-class Offset value advances `.fd` schema to v9 and
the contributor Catalog API to 8; Canvas model advances to v14. NBFC framing
plus Canvas control/wire remain version 1.

## ADR-062 — Image creation uses an editable unresolved provider when assets are unavailable

Accepted; this supersedes only the creation-admission rule in ADR-045. A valid
Palette/tree/Canvas or **Replace New Widget** destination must not disappear
merely because the current project has no declared image asset, or because its
asset inventory is refreshing, verifying or temporarily unavailable. Image
creation still initializes `image` from the deterministic first sorted declared
asset when one is usable. Otherwise it allocates the normal stable ID and stores
an explicit reserved unresolved `ImageProviderValue`.

The unresolved value reuses the existing `ASSET` wire shape and a reserved safe
logical identity, so `.fd` schema v9, Catalog API 8, Canvas model v14 and NBFC
framing/control version 1 remain unchanged. It cannot carry a package, exact
scale or `ResizeImage` options. The identity is never resolved from the project,
shown as an asset choice or emitted as an `AssetImage` path. Properties formats
it as `<choose asset>` and keeps the custom provider editor available after
Save/reopen; a real declared asset must be selected before that editor can
publish a replacement value.

Canvas projects the state directly to its bounded unavailable-image placeholder
without a filesystem lookup or leaking the reserved identity. Deterministic Dart
preserves the complete `Image(...)` argument surface but supplies a valid
embedded 8x8 PNG through `MemoryImage(base64Decode(...), scale: 0.125)` and adds
`dart:convert` only while an unresolved provider exists. Therefore generated
source remains analyzable and runnable without a nonexistent asset. The
constructor becomes non-const only for that temporary provider; choosing a real
asset restores the ordinary const `AssetImage`/`ExactAssetImage` path. Drop
admission continues to fail closed for stale authority, incompatible or
ambiguous destinations and other catalog violations, but not for image-inventory
availability.

## ADR-063 — RotatedBox follows Transform with layout-time quarter turns

Accepted. No widget after Transform had a pre-assigned repository order. The
next practical complete slice is the const
[`flutter.widgets.RotatedBox(...)`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 210 immediately after Transform. Flutter also lists
RotatedBox beside Transform in its Painting/effects catalog, but RotatedBox
applies its integral rotation before layout and therefore belongs with the
Designer's structural layout tools. Delegate-based CustomSingleChildLayout and
CustomMultiChildLayout remain unadmitted rather than introducing arbitrary Dart
expressions.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface contains
required named integer `quarterTurns` and one optional single any-widget
`child`, in constructor order. A detached prototype stores `quarterTurns: 1`
so creation has an immediately visible clockwise quarter turn. The value may be
negative and is restricted to Dart's exact shared native/Web interval
`-9007199254740991..9007199254740991`; missing, wrong-kind and out-of-range
values fail closed. No constructor branch is omitted.

Generated Dart and both Canvas projections construct the real Flutter
RotatedBox. Odd turns flip incoming constraints and exchange the child's layout
axes; even turns retain them. Flutter paints and hit-tests the corresponding
modulo-four rotation while the Designer preserves the exact signed integer for
Properties, persistence and Undo/Redo. Selection and drop instrumentation uses
the actual laid-out render box, with a bounded 36x36 target only when the real
result is zero-sized. Property and exact-slot editing, Palette/tree/Canvas DnD,
same-tree movement, Save/reopen and further signed editing share this contract.

RotatedBox adds one writable row, one ordinary source and one insertable
any-widget destination to ADR-061. The resulting surface contains 675 writable
rows, with 658 outside Scaffold, across 35 widgets and 30 const-constructor
definitions. Thirty-five sources across 32 any-widget plus two trait-bound
destinations form 1,190 candidates. The new destination accepts 31 old
unrestricted sources and rejects Expanded, Flexible and Spacer; the new source
enters all 32 any-widget destinations and neither trait-bound destination. This
adds 63 accepted and five rejected cells, producing 1,032 accepted and 158
rejected overall. The Layout Palette contains 27 items.

The practical Material/Base Designer backlog is now 35/92 complete, with 57
remaining. This is a project planning target, not a normative complete list of
Flutter widgets. Existing integer, slot and payload encodings cover the full
constructor, so `.fd` remains v9, Catalog API remains 8 and Canvas model remains
v14; NBFC framing plus Canvas control/wire remain version 1.

## ADR-064 — ListBody adds a constraint-safe linear layout slice

Accepted. After RotatedBox, the next complete practical slice is the const
[`flutter.widgets.ListBody(...)`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html)
constructor from `package:flutter/widgets.dart`, published in the **Layout**
Palette category at order 220. It provides a lightweight sequential multi-child
layout without introducing delegate expressions. More complex delegate-driven
multi-child layouts remain unadmitted until they receive separate closed
contracts.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface is
`const ListBody({super.key, this.mainAxis = Axis.vertical, this.reverse = false,
super.children})`. The Designer exposes optional `mainAxis` with the exact
`Axis.horizontal`/`Axis.vertical` values, optional `reverse`, and one ordered
any-widget `children` list. A detached prototype omits both properties so
Flutter's vertical/non-reversed defaults remain distinct from explicit values;
the list starts empty. There is no unsupported constructor branch.

Generated application Dart constructs a bare real `ListBody`; the Designer does
not emit a scroll wrapper or rewrite the surrounding application layout.
`RenderListBody` requires unbounded constraints on its main axis and bounded
constraints on its cross axis, while the Canvas itself is a bounded preview.
The native and exact-Web Canvas therefore use an axis-matched design-time
viewport guard around the real widget to satisfy that rendering precondition.
The guard is presentation-only: it does not enter `.fd`, Properties, Undo/Redo,
Save/reopen or generated Dart. Application authors remain responsible for
placing the generated bare widget in a valid Flutter constraint context.

ListBody uses the same ordered list-slot contract as the other linear
containers. An empty node exposes insertion index zero across its bounded
design-time rectangle; a populated node exposes its terminal append edge.
Vertical reversal and horizontal reversal combined with ambient
`Directionality` determine that visual edge. Expanded, Flexible and Spacer are
rejected as direct ListBody children because their parent-data contract requires
a direct Row or Column parent. Property and exact-slot editing,
Palette/tree/Canvas DnD, same-tree movement/reordering, deterministic generation,
Save/reopen and further editing and Undo/Redo share the one catalog contract.

ListBody adds two writable rows, one ordinary source and one insertable
any-widget destination to ADR-063. The resulting surface contains 677 writable
rows, with 660 outside Scaffold, across 36 widgets and 31 const-constructor
definitions. Thirty-six sources across 33 any-widget plus two trait-bound
destinations form 1,260 candidates. The new destination accepts 32 old sources
and rejects Expanded, Flexible and Spacer; the new source enters all 33
any-widget destinations and neither trait-bound destination; its self-cell is
accepted. This adds 65 accepted and five rejected cells, producing 1,097
accepted and 163 rejected overall. The Layout Palette contains 28 items.

The practical Material/Base Designer backlog is now 36/92 complete, with 56
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing enum, boolean and list-slot encodings cover the constructor,
so `.fd` remains v9, Catalog API remains 8 and Canvas model remains v14; NBFC
framing plus Canvas control/wire remain version 1.

## ADR-065 — OverflowBar adds adaptive row-to-column layout

Accepted. The official Flutter Layout catalog places
[`OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar-class.html)
after ListView; ListView is already admitted, making OverflowBar the first
unimplemented bounded non-delegate entry after ListBody. It is published in the
**Layout** Palette category at order 230. Delegate-driven
CustomSingleChildLayout, CustomMultiChildLayout and Flow remain unadmitted rather
than introducing arbitrary Dart expressions or partial contracts.

The complete reviewed non-`key` Flutter 3.44.8 constructor surface is
`const OverflowBar({super.key, this.spacing = 0.0, this.alignment,
this.overflowSpacing = 0.0, this.overflowAlignment =
OverflowBarAlignment.start, this.overflowDirection = VerticalDirection.down,
this.textDirection, super.children})`. The Designer exposes optional finite
signed `spacing`, all six `MainAxisAlignment` values through nullable
`alignment`, finite signed `overflowSpacing`, all three
`OverflowBarAlignment` values, both `VerticalDirection` values, nullable
`TextDirection`, and one ordered any-widget `children` list. A detached
prototype omits all six properties and starts empty, retaining Flutter's exact
constructor defaults. There is no unsupported constructor branch.

Generated application Dart constructs a bare real `OverflowBar`. Its normal
mode lays children out horizontally until their widths plus `spacing` exceed the
available width, then switches to a full-width vertical overflow column. The
native and exact-Web Canvas construct that same widget. For a nonempty node, a
presentation-only `LayoutBuilder` supplies a finite 240-pixel width only when
the incoming width is unbounded and `alignment` is non-null; null alignment
retains Flutter's natural-width layout. A 36x36 minimum retains selection and
insertion for an empty node. Neither guard enters `.fd`, Properties, Undo/Redo,
Save/reopen or generated Dart.

Canvas DnD derives the active mode from the rendered child widths, configured
spacing and actual OverflowBar width, matching Flutter's layout decision. A
fitting row resolves source-order insertion edges from explicit or ambient
LTR/RTL direction; an overflow column resolves them from
`overflowDirection.down/up`, while text direction continues to affect only the
column's cross-axis alignment. Expanded, Flexible and Spacer are rejected as
direct children because they require a direct Row or Column parent. Exact
property and list-slot editing, Palette/tree/Canvas DnD, same-tree
movement/reordering, deterministic generation, Save/reopen and further editing
and Undo/Redo share the catalog contract.

OverflowBar adds six writable rows, one ordinary source and one insertable
any-widget destination to ADR-064. The resulting surface contains 683 writable
rows, with 666 outside Scaffold, across 37 widgets and 32 const-constructor
definitions. Thirty-seven sources across 34 any-widget plus two trait-bound
destinations form 1,332 candidates. The new destination accepts 33 old sources
and rejects Expanded, Flexible and Spacer; the new source enters all 33 old
any-widget destinations and neither trait-bound destination; its self-cell is
accepted. This adds 67 accepted and five rejected cells, producing 1,164
accepted and 168 rejected overall. The Layout Palette contains 29 items.

The practical Material/Base Designer backlog is now 37/92 complete, with 55
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing enum, numeric and list-slot encodings cover the constructor,
so `.fd` remains v9, Catalog API remains 8 and Canvas model remains v14; NBFC
framing plus Canvas control/wire remain version 1.

## ADR-066 — GridView.count adds the first static scrolling grid

Accepted. `GridView.count` is published in the **Scrolling** Palette category at
order 20 immediately after ListView. Its catalog identity is
`flutter.widgets.GridView`; `WidgetDefinition` separately records Dart class
`GridView` and named constructor `count`, so generated source emits the exact
constructor without encoding the constructor name into the widget type ID.

The complete reviewed Flutter 3.44.8 static-child surface contains 21 non-`key`
properties: `scrollDirection`, `reverse`, `primary`, one closed `physics`
preset, `shrinkWrap`, non-negative `padding`, required positive
`crossAxisCount`, non-negative `mainAxisSpacing` and `crossAxisSpacing`, positive
`childAspectRatio`, optional non-negative `mainAxisExtent`,
`addAutomaticKeepAlives`, `addRepaintBoundaries`, `addSemanticIndexes`, numeric
pixel `scrollCacheExtent`, `semanticChildCount`, `dragStartBehavior`,
`keyboardDismissBehavior`, bounded `restorationId`, `clipBehavior` and
`hitTestBehavior`, plus one ordered any-widget `children` slot. Palette creation
stores `crossAxisCount: 2` and an empty list; all 20 optional rows remain absent.
When `mainAxisExtent` is present Flutter uses it for each tile's main-axis
extent instead of deriving that extent from `childAspectRatio`.

Controller-owned state, arbitrary `ScrollPhysics` graphs, `scrollBehavior`,
delegate/builder inputs, `GridView.builder`, `GridView.custom`,
`GridView.extent`, `key` and the deprecated raw `cacheExtent` argument remain
outside this slice. The reviewed physics string is synthesized only into one
of six static constructors; `scrollCacheExtent` continues to use the existing
typed pixel cache-extent mapping. This keeps `.fd` schema v9, Catalog API 8 and
Canvas model v14 unchanged.

NetBeans projects the rows into Scrolling, Grid layout, Caching and children,
Semantics, and Restoration groups with a closed physics chooser. The children
slot preserves exact source, paint and semantic order. Palette/tree/Canvas DnD,
same-tree movement and slot editing use the existing list-slot contract;
Expanded, Flexible and Spacer remain invalid direct grid children because their
parent-data contract requires a direct Row or Column parent. Native and
exact-Web Canvas construct the real `GridView.count`; application Dart receives
the same named constructor and the same finite 240-wide/120-high guard when
unbounded constraints require it, never a Designer-only widget. Four dedicated
reviewed 16/32 px light/dark SVGs identify the grid in Palette and Explorer.

GridView.count adds 21 writable rows, one ordinary source and one insertable
any-widget destination to ADR-065. The resulting surface contains 704 writable
rows, with 687 outside Scaffold, across 38 widgets and 32 const-constructor
definitions. Thirty-eight sources across 35 any-widget plus two trait-bound
destinations form 1,406 candidates. The new destination accepts 34 old sources
and rejects Expanded, Flexible and Spacer; the new source enters all 34 old
any-widget destinations and neither trait-bound destination; its self-cell is
accepted. This adds 69 accepted and five rejected cells, producing 1,233
accepted and 173 rejected overall. Layout remains at 29 Palette items and
Scrolling contains two.

The practical Material/Base Designer backlog is now 38/92 complete, with 54
remaining. This planning target is not a normative complete list of Flutter
widgets. NBFC framing plus Canvas control/wire remain version 1.

## ADR-067 — SingleChildScrollView adds the single-box scrolling container

Accepted. `SingleChildScrollView` is published in the **Scrolling** Palette
category at order 30 immediately after `GridView.count`. Its catalog identity
and Dart class are both `flutter.widgets.SingleChildScrollView`; the reviewed
default constructor is const.

The complete Flutter 3.44.8 surface after excluding `key` and controller-owned
state contains 10 optional properties in constructor order around one optional
single `child` slot: `scrollDirection`, `reverse`, non-negative `padding`,
nullable `primary`, one closed `physics` preset, `child`,
`dragStartBehavior`, `clipBehavior`, `hitTestBehavior`, `restorationId` and
`keyboardDismissBehavior`. The reviewed enums and six physics presets reuse the
same closed domains as ListView and GridView.count. A detached prototype stores
no properties and an empty child slot, preserving Flutter's vertical, forward,
unpadded, inferred primary/physics, start-drag, hard-edge clip, opaque hit-test,
null restoration and inherited keyboard-dismiss defaults. Arbitrary
`ScrollPhysics` graphs, `ScrollController`, `key` and raw Dart remain outside
the slice.

Generated Dart and native/exact-Web Canvas construct the real
`SingleChildScrollView`. Unlike ListView and GridView, Flutter deliberately
shrink-wraps this widget in both axes and its render object has no bounded-axis
viewport assertion. The Designer therefore does not apply the generated
LayoutBuilder/SizedBox viewport guard. Canvas retains only a
non-layout-affecting 36x36 selection/drop target when the real empty or
zero-size widget would otherwise have no usable hit area; that target is never
persisted or emitted into application Dart.

NetBeans projects the rows into Scrolling, Layout, Semantics and Restoration
groups with the shared closed physics chooser. The optional child uses the
ordinary atomic single-slot contract for Palette/tree/Canvas insertion,
replacement, clear, same-tree movement, Save/reopen and Undo/Redo. Expanded,
Flexible and Spacer remain invalid direct children because their parent-data
contract requires a direct Row or Column parent. Four dedicated reviewed 16/32
px light/dark SVGs identify the widget in Palette and Explorer.

SingleChildScrollView adds 10 writable rows, one ordinary source and one
insertable any-widget destination to ADR-066. The resulting surface contains
714 writable rows, with 697 outside Scaffold, across 39 widgets and 33
const-constructor definitions. Thirty-nine sources across 36 any-widget plus
two trait-bound destinations form 1,482 candidates. The new destination accepts
35 old sources and rejects Expanded, Flexible and Spacer; the new source enters
all 35 old any-widget destinations and neither trait-bound destination; its
self-cell is accepted. This adds 71 accepted and five rejected cells, producing
1,304 accepted and 178 rejected overall. Layout remains at 29 Palette items and
Scrolling contains three.

The practical Material/Base Designer backlog is now 39/92 complete, with 53
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing value and slot encodings keep `.fd` schema v9, Catalog API 8
and Canvas model v14 unchanged; NBFC framing plus Canvas control/wire remain
version 1.

## ADR-068 — ColoredBox adds a theme-aware painted single-child container

Accepted. [`ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
is published in the **Basic** Palette category at order 40 after `Image`. Its
catalog identity and Dart class are both `flutter.widgets.ColoredBox`; the
reviewed default constructor is const.

The complete Flutter 3.44.8 surface after excluding the common `key` argument
contains required `color`, optional `isAntiAlias` whose omitted Flutter default
is `true`, and one optional single `child` slot. Detached creation stores the
required literal `Color(0xFF2196F3)`, leaves `isAntiAlias` omitted and begins
with an empty child. The color domain is deliberately closed to an exact ARGB
literal or a reviewed Material `ColorScheme` theme token; arbitrary Dart color
expressions are not admitted. Literal output keeps the constructor const. A
theme token emits `Theme.of(context).colorScheme...`, so generation removes
const from that `ColoredBox` without weakening const output for literal values.

NetBeans projects the two arguments into one Appearance property set. The
required color cannot be restored to omission. Explicit anti-alias values use
the shared accessible checkbox editor, while **Restore Default** returns it to
the separate `<not set>` state. The optional child uses the ordinary atomic
single-slot contract for Palette/tree/Canvas insertion, replacement, clear,
same-tree movement, Save/reopen and further editing, and Undo/Redo. Generated
Dart and native/exact-Web Canvas construct the real `ColoredBox`. An empty box
has no intrinsic size, so Canvas retains a non-layout-affecting 36x36 selection
and drop target when its real bounds collapse to zero; that Designer overlay is
never stored in `.fd` or emitted into application Dart. Four dedicated reviewed
16/32 px light/dark SVGs identify the widget in Palette and Explorer.

ColoredBox adds two writable rows, one ordinary source and one insertable
any-widget destination to ADR-067. The resulting surface contains 716 writable
rows, with 699 outside Scaffold, across 40 widgets and 34 const-constructor
definitions. Forty sources across 37 any-widget plus two trait-bound
destinations form 1,560 candidates. The new destination accepts 36 old sources
and rejects Expanded, Flexible and Spacer; the new source enters all 36 old
any-widget destinations and neither trait-bound destination; its self-cell is
accepted. This adds 73 accepted and five rejected cells, producing 1,377
accepted and 183 rejected overall. Layout remains at 29 Palette items,
Scrolling at three and Basic contains four.

The practical Material/Base Designer backlog is now 40/92 complete, with 52
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing value and slot encodings keep `.fd` schema v9, Catalog API 8
and Canvas model v14 unchanged; NBFC framing plus Canvas control/wire remain
version 1.

## ADR-069 — SafeArea is a generic atomic required-child wrapper

Accepted. [`SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html)
is published in the **Layout** Palette category at order 240 immediately after
`OverflowBar`. Its catalog identity is `flutter.widgets.SafeArea`, its Dart
class is `SafeArea`, and the reviewed default constructor is const.

The complete Flutter 3.44.8 surface after excluding only the common `key`
argument contains optional `left`, `top`, `right`, `bottom`, `minimum` and
`maintainBottomViewPadding`, followed by one required `child`. The detached
wrapper payload omits all six properties, preserving Flutter's exact `true`,
`EdgeInsets.zero` and `false` defaults, but is never admitted as a standalone
node. `minimum` accepts signed finite physical `EdgeInsets`.
`EdgeInsetsDirectional` is deliberately rejected because SafeArea declares the
concrete physical type rather than `EdgeInsetsGeometry`. Non-finite components
and arbitrary Dart expressions are separately rejected by the Designer's closed
stored-value policy.

A required child cannot be represented by a valid empty inserted widget.
Creation mode is therefore derived generically from the catalog shape: exactly
one required single any-widget `child`, with no unresolved required property,
uses one atomic `WrapWidget` around an existing subtree. This is not a SafeArea
type allow-list and never publishes an incomplete node. The widget-tree route
can wrap either the document root or a non-root row. The current Canvas target
wire resolves only non-root children and intentionally publishes no synthetic
root target; root wrapping remains available through the tree. SafeArea cannot
wrap Expanded, Flexible or Spacer because their ParentData must remain a direct
child of Row or Column. Generated Dart and native/exact-Web Canvas construct the
real widget. Save/reopen, further editing and Undo/Redo use the existing paired
mutation pipeline, and four reviewed 16/32 px light/dark SVGs identify the
widget.

SafeArea adds six writable rows and one source, but its required occupied child
slot is not an insertable destination. The resulting surface contains 722
writable rows, with 705 outside Scaffold, across 41 widgets and 35
const-constructor definitions. Forty-one sources across 37 any-widget and two
trait-bound destinations form 1,599 candidates. The new source enters all 37
any-widget destinations and neither trait destination, adding 37 accepted and
two rejected cells to ADR-068: 1,414 accepted and 185 rejected overall. Layout
contains 30 Palette items, Scrolling three, Basic four and Material four.

The practical Material/Base Designer backlog is now 41/92 complete, with 51
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing persisted and transport shapes keep `.fd` schema v9 and
Canvas model v14 unchanged; adding the exported `directionalAllowed` component
to `PropertyValueConstraint.EdgeInsetsValues` advances the contributor Catalog
API to 9. NBFC framing plus Canvas control/wire remain version 1.

## ADR-070 — Placeholder is a complete optional-child Basic slice

Accepted. [`Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
is published in the **Basic** Palette category at order 50 immediately after
`ColoredBox`. Its catalog identity is `flutter.widgets.Placeholder`, its Dart
class is `Placeholder`, and the reviewed Flutter 3.44.8 default constructor is
const.

The complete public surface after excluding only the common `key` argument is
optional `color`, `strokeWidth`, `fallbackWidth` and `fallbackHeight`, followed
by one optional single any-widget `child`. A detached node stores none of the
four properties and starts with an empty child, preserving Flutter's exact
`Color(0xFF455A64)`, `2.0`, `400.0` and `400.0` defaults through omission.
Explicit numeric values use the existing integer-or-double encoding and must
be finite and non-negative. Color admits either an exact ARGB literal or one
reviewed Material `ColorScheme` token. Omitted and literal colors retain const
generation; a theme token emits `Theme.of(context).colorScheme...` and removes
const from the surrounding widget.

Generated Dart and native/exact-Web Canvas construct the real Flutter
`Placeholder`, including framework-owned fallback sizing, diagonal outline,
stroke and child layout. An empty node remains a valid ordinary Palette
prototype, and its optional child is an insertable destination available to
Palette, tree, Canvas and Slots operations. Any Designer-only selection, hit
and drop affordance remains outside the widget and is never persisted. The
slice includes grouped typed Properties, Restore Default, deterministic
generation, Save/reopen and further editing, chronological Undo/Redo,
same-tree movement, accessibility and four reviewed 16/32 px light/dark SVGs.

Placeholder adds four writable rows, one ordinary source and one insertable
any-widget destination. The resulting surface contains 726 writable rows, with
709 outside Scaffold, across 42 widgets and 36 const-constructor definitions.
The new destination admits the 38 old non-ParentData sources and rejects
Expanded, Flexible and Spacer; the new source enters all 37 old any-widget
destinations and neither trait destination; and the new source-to-destination
cell is admitted. This adds 76 accepted and five rejected cells to ADR-069.
Forty-two sources across 38 any-widget and two trait-bound destinations
therefore form 1,680 candidates: 1,490 accepted and 190 rejected. Layout
contains 30 Palette items, Scrolling three, Basic five and Material four.

At the ADR-070 milestone the practical Material/Base Designer backlog was 42/92
complete, with 50 remaining. This planning target is not a normative complete list of Flutter
widgets. Existing value, slot and transport shapes keep `.fd` schema v9,
Catalog API 9, Canvas model v14 and NBFC framing plus Canvas control/wire v1
unchanged.

## ADR-071 — Directionality is a complete atomic required-child Basic wrapper

Accepted. [`Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
is published in the **Basic** Palette category at order 60 immediately after
`Placeholder`. Its catalog identity is `flutter.widgets.Directionality`,
its Dart class is `Directionality`, and its sole Flutter 3.44.8 constructor
is const.

The complete public surface after excluding only the common `key` argument
contains required `TextDirection textDirection` and one required any-widget
`child`. Flutter provides no default direction. A detached Designer wrapper
therefore persists the explicit reviewed creation value
`TextDirection.ltr`; the only other admitted value is
`TextDirection.rtl`. This is a Designer creation choice, not a claimed
framework default. No nullable, string or arbitrary Dart-expression surrogate
is admitted.

The required child prevents ordinary empty insertion. The existing
catalog-derived wrapper mode recognizes that every required property has a
creation value and fills `child` by one atomic `WrapWidget` command around
an existing subtree. Tree DnD can wrap the root or a non-root row; the current
Canvas target wire exposes exact non-root children. Expanded, Flexible and
Spacer cannot be wrapped because their ParentData must remain directly under
Row or Column. Generated Dart and native/exact-Web Canvas construct the real
inherited `Directionality`, so directional alignment, padding and text below
it resolve from the selected value. Because the widget creates no render
object, Designer selection and drop affordances remain outside it and are
never persisted or emitted.

The slice includes typed Properties, exact required-slot presentation,
Palette/tree/Canvas wrapping, same-tree movement, deterministic const
generation, Save/reopen and further editing, chronological Undo/Redo,
accessibility and four reviewed 16/32 px light/dark SVGs.
`Directionality` adds one writable row and one source, while its occupied
required child is not an insertable destination. The resulting surface has
727 writable rows, 710 outside Scaffold, across 43 widgets and 37 reviewed
const-constructor definitions. The new source enters all 38 any-widget
destinations and neither trait destination, adding 38 accepted and two
rejected cells to ADR-070. Forty-three sources across 38 any-widget and two
trait-bound destinations therefore form 1,720 candidates: 1,528 accepted and
192 rejected. Layout contains 30 Palette items, Scrolling three, Basic six and
Material four.

The practical Material/Base Designer backlog is now 43/92 complete, with 49
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing value, slot and transport shapes keep `.fd` schema v9,
Catalog API 9, Canvas model v14 and NBFC framing plus Canvas control/wire v1
unchanged.

## ADR-072 — DecoratedBox reuses the complete typed BoxDecoration contract

Accepted. [`DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
is published in the **Basic** Palette category at order 70 immediately after
`Directionality`. Its catalog identity is `flutter.widgets.DecoratedBox`, its
Dart class is `DecoratedBox`, and its canonical Flutter 3.44.8 constructor is
const.

After excluding only the common `key` argument, the constructor contains
required `Decoration decoration`, optional `DecorationPosition position` with
the exact `background` default, and one optional any-widget `child`. The
Designer admits the existing closed `BoxDecoration` algebra as the complete
reviewed value branch for the abstract `Decoration` parameter: literal and
reviewed theme colors, asset-backed images, physical or directional borders
and radii, ordered shadows, linear/radial/sweep gradients, background blend
mode and rectangle/circle shape. Custom `Decoration` subclasses and arbitrary
Dart expressions are not serialized or executed. New nodes persist an exact
empty rectangular `BoxDecoration()` because Flutter requires the argument;
omitting `position` continues to mean `DecorationPosition.background`.

The optional child is an ordinary insertable single slot. Generated Dart and
native/exact-Web Canvas construct the real `DecoratedBox`, including foreground
painting when selected; an empty render object retains only an IDE-owned
selection/drop target outside the painted widget. The slice includes typed
Properties and the existing transactional BoxDecoration editor,
Palette/tree/Canvas creation, exact-slot management, same-tree movement,
deterministic const generation, Save/reopen and further editing,
chronological Undo/Redo, accessibility and four reviewed 16/32 px light/dark
SVGs.

`DecoratedBox` added two writable rows, one source and one optional any-widget
destination. The 43 previous sources add 40 accepted and three ParentData
rejections at the new child slot; the new source adds 39 accepted any-widget
and two rejected trait-bound cells. At that milestone the surface had 729 writable
rows, 712 outside Scaffold, across 44 widgets and 38 reviewed const-constructor
definitions. Forty-four sources across 39 any-widget and two trait-bound
destinations formed 1,804 candidates: 1,607 accepted and 197 rejected. Layout
contained 30 Palette items, Scrolling three, Basic seven and Material four.

The practical Material/Base Designer backlog was then 44/92 complete, with 48
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing value, slot and transport shapes kept `.fd` schema v9,
Catalog API 9, Canvas model v14 and NBFC framing plus Canvas control/wire v1
unchanged.

## ADR-073 — ExcludeSemantics opens the Accessibility Palette category

Accepted. [`ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
is the first item in the **Accessibility** Palette category at category order
400 and item order 10. Its catalog identity is
`flutter.widgets.ExcludeSemantics`, its Dart class is `ExcludeSemantics`, and
its canonical Flutter 3.44.8 constructor is const.

After excluding only the common `key` argument, the complete reviewed
constructor contains optional `bool excluding` with the exact Flutter default
`true` and one optional any-widget `child`. A new Palette node stores neither
the property nor a child, so generated Dart preserves both constructor
defaults. The standard optional-boolean Properties editor exposes `<not set>`,
explicit `true` and explicit `false`, supports Restore Default, and remains
editable after Save/reopen and Undo/Redo. The Slots sheet manages the optional
child with the same exact-slot contract as other single-child wrappers.

Generated Dart and native/exact-Web Canvas construct the real
`ExcludeSemantics`. Omitted or explicit `true` removes the application child
subtree from Flutter's semantics tree, while explicit `false` preserves it;
layout, paint and hit testing continue to proxy the child unchanged. The
`ExcludeSemantics` node's own Designer selection, hit/drop and accessibility
wrapper remains outside the application widget and stays operable in both
modes. Descendant Canvas semantics labels follow the real subtree exclusion;
the separate NetBeans widget tree remains accessible. An empty node
retains only the transient IDE-owned zero-size selection/drop target. The full
slice includes Palette/tree/Canvas creation, optional-child management,
same-tree movement, deterministic const generation, Save/reopen and further
editing, chronological Undo/Redo, accessibility and four reviewed 16/32 px
light/dark SVGs.

`ExcludeSemantics` adds one writable row, one source and one optional
any-widget destination. The 44 previous sources add 41 accepted cells and
three ParentData rejections at the new child slot; the new source contributes
40 accepted any-widget cells, including its self-cell, and two rejected
trait-bound cells. The resulting surface has 730 writable rows, 713 outside
Scaffold, across 45 widgets and 39 reviewed const-constructor definitions.
Forty-five sources across 40 any-widget and two trait-bound destinations form
1,890 candidates: 1,688 accepted and 202 rejected. Layout contains 30 Palette
items, Scrolling three, Basic seven, Material four and Accessibility one.

The practical Designer backlog is now 45/92 complete, with 47
remaining. This planning target is not a normative complete list of Flutter
widgets. Existing value, slot and transport shapes keep `.fd` schema v9,
Catalog API 9, Canvas model v14 and NBFC framing plus Canvas control/wire v1
unchanged.

## ADR-074 — IndexedStack adds exact nullable selection to Layout

Accepted. [`IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
is published in the **Layout** Palette category at item order 115 beside
`Stack`. Its catalog identity is `flutter.widgets.IndexedStack`, its Dart class
is `IndexedStack`, and its canonical Flutter 3.44.8 constructor is const.

After excluding only the common `key` argument, the complete reviewed
constructor contains optional `AlignmentGeometry alignment`, nullable
`TextDirection textDirection`, `Clip clipBehavior`, `StackFit sizing`, nullable
`int index`, and one ordered any-widget `children` list. A new Palette node
stores neither properties nor children, preserving `AlignmentDirectional.topStart`,
ambient text direction, `Clip.hardEdge`, `StackFit.loose`, index `0` and the
empty list through constructor omission.

The nullable index requires three exact Designer states. Property omission is
`<not set>` and keeps Flutter's index-zero default; an explicit non-negative
portable integer selects that existing child; explicit typed `NullValue`
generates `index: null` and selects none. Validation rejects a concrete index
outside the current ordered list, except that effective index zero remains
valid for an empty list exactly as Flutter permits. Removing or reordering
children is therefore checked against the same relation before mutation is
accepted. The new null value is a closed tagged value, not a sentinel integer
or arbitrary Dart expression.

Generated Dart and native/exact-Web Canvas construct the real `IndexedStack`.
Its layout remains as large as its largest child; only the selected child is
painted, hit-tested and visited for application semantics, while every child
remains ordered and editable through the Designer model and NetBeans widget
tree. An empty instance keeps only a transient IDE-owned selection/drop target.
The full slice includes typed Properties, ordered Slots,
Palette/tree/Canvas creation and movement, deterministic const generation,
Save/reopen and further editing, chronological Undo/Redo, accessibility and
four reviewed 16/32 px light/dark SVGs.

`IndexedStack` adds five writable rows, one source and one insertable
any-widget destination. The 45 previous sources add 42 accepted cells and
three ParentData rejections at the new `children` slot; the new source adds 41
accepted any-widget cells and two rejected trait-bound cells. The resulting
surface has 735 writable rows, 718 outside Scaffold, across 46 widgets and 40
reviewed const-constructor definitions. Forty-six sources across 41 any-widget
and two trait-bound destinations form 1,978 candidates: 1,771 accepted and 207
rejected. Layout contains 31 Palette items, Scrolling three, Basic seven,
Material four and Accessibility one.

At that milestone the practical Designer backlog was 46/92 complete, with 46
remaining; the next missing item in the fixed priority order was `ClipRect`.
Exact typed null
advances `.fd` schema to v10, contributor Catalog API to 10 and Canvas model to
v15. NBFC framing plus Canvas control/wire remain v1.

## ADR-075 — ClipRect is a complete typed paint-clipping slice

Accepted. The fixed practical inventory now admits canonical
`flutter.widgets.ClipRect` in Basic at item order 80 after `DecoratedBox`. The
definition maps to the const `ClipRect` constructor from
`package:flutter/widgets.dart`, now exposes optional typed `clipper`, optional
closed `clipBehavior` and one optional single any-widget `child` slot, and
excludes only the common `key`. The original Palette node
stores no property and no child, preserving Flutter's `Clip.hardEdge` default by
constructor omission. The property editor admits exactly `Clip.none`,
`Clip.hardEdge`, `Clip.antiAlias` and `Clip.antiAliasWithSaveLayer`.

The SDK exposes `CustomClipper<Rect>` as an abstract user extension point and no
public concrete delegate suitable for a bounded literal catalog value. The
initial v10 slice therefore omitted the row instead of accepting opaque Dart.
ADR-077's schema-v12 typed declaration/reference contract now supersedes that
limitation for `ClipRect.clipper`.

Generated Dart and native/exact-Web Canvas construct the real `ClipRect` with
the exact effective clip behavior and optional child. Paint, hit testing and
application semantics follow Flutter's widget; Designer selection outlines,
empty-node targets and drag affordances remain transient overlays outside the
clip. The complete slice covers typed Properties, Slots,
Palette/tree/Canvas creation and movement, deterministic const generation,
Save/reopen and further editing, chronological Undo/Redo, accessibility and
four reviewed 16/32 px light/dark SVGs.

`ClipRect` adds one writable row, one source and one insertable any-widget
destination. At the new destination the 46 previous sources add 43 accepted
cells and three ParentData rejections; the new source adds 42 accepted
any-widget cells and two rejected trait-bound cells. At this ADR-075 milestone the resulting surface had
736 writable rows, 719 outside Scaffold, across 47 widgets and 41 reviewed
const-constructor definitions. Forty-seven sources across 42 any-widget and two
trait-bound destinations form 2,068 candidates: 1,856 accepted and 212
rejected. Layout contains 31 Palette items, Scrolling three, Basic eight,
Material four and Accessibility one.

At the ADR-075 milestone the practical Designer backlog was 47/92 complete,
with 45 remaining; the next missing item in the fixed priority order was
`ClipOval`. Existing value and
transport shapes keep `.fd` schema v10, contributor Catalog API 10, Canvas model
v15, and NBFC framing plus Canvas control/wire v1 unchanged.

## ADR-076 — ClipOval is a complete typed oval-clipping slice

Accepted. The fixed practical inventory now admits canonical
`flutter.widgets.ClipOval` in Basic at item order 90 after `ClipRect`. The
definition maps to the const `ClipOval` constructor from
`package:flutter/widgets.dart`, now exposes optional typed `clipper`, optional
closed `clipBehavior` and one optional single any-widget `child` slot, and
excludes only the common `key`. The original Palette node
stores no property and no child, preserving Flutter's `Clip.antiAlias` default
by constructor omission. The property editor admits exactly `Clip.none`,
`Clip.hardEdge`, `Clip.antiAlias` and `Clip.antiAliasWithSaveLayer`.

The SDK exposes `CustomClipper<Rect>` as an abstract user extension point and no
public concrete delegate suitable for a bounded literal catalog value. The
initial v10 slice therefore omitted the row instead of accepting opaque Dart.
ADR-077's schema-v12 typed declaration/reference contract now supersedes that
limitation for `ClipOval.clipper`.

Generated Dart and native/exact-Web Canvas construct the real `ClipOval` with
the exact effective clip behavior and optional child. Without a custom clipper,
Flutter inscribes the oval in the child's layout bounds. Paint, hit testing and
application semantics follow Flutter's widget; Designer selection outlines,
empty-node targets and drag affordances remain transient overlays outside the
clip. The complete slice covers typed Properties, Slots,
Palette/tree/Canvas creation and movement, deterministic const generation,
Save/reopen and further editing, chronological Undo/Redo, accessibility and
four reviewed 16/32 px light/dark SVGs.

`ClipOval` adds one writable row, one source and one insertable any-widget
destination. At the new destination the 47 previous sources add 44 accepted
cells and three ParentData rejections; the new source adds 43 accepted
any-widget cells and two rejected trait-bound cells. At this ADR-076 milestone the resulting surface had
737 writable rows, 720 outside Scaffold, across 48 widgets and 42 reviewed
const-constructor definitions. Forty-eight sources across 43 any-widget and two
trait-bound destinations form 2,160 candidates: 1,943 accepted and 217
rejected. Layout contains 31 Palette items, Scrolling three, Basic nine,
Material four and Accessibility one.

At the ADR-076 milestone the practical Designer backlog was 48/92 complete,
with 44 remaining; the next missing item in the fixed priority order was
`ClipRRect`. Existing value and transport shapes kept `.fd` schema v10,
contributor Catalog API 10, Canvas model v15, and NBFC framing plus Canvas
control/wire v1 unchanged.

## ADR-077 — ClipRRect is a complete typed rounded-rectangle clipping slice

Accepted. The fixed practical inventory now admits canonical
`flutter.widgets.ClipRRect` in Basic at item order 100 after `ClipOval`. The
definition maps to the const `ClipRRect` constructor from
`package:flutter/widgets.dart`, exposes optional typed `borderRadius`, optional
typed `clipper`, optional closed `clipBehavior` and one optional single
any-widget `child` slot, and excludes only the common `key`. A
new Palette node stores no property and no child, preserving Flutter's
`BorderRadius.zero` and `Clip.antiAlias` defaults by constructor omission. The
clip editor admits exactly `Clip.none`, `Clip.hardEdge`, `Clip.antiAlias` and
`Clip.antiAliasWithSaveLayer`.

The new top-level border-radius value retains either physical `BorderRadius` or
directional `BorderRadiusDirectional` geometry. Each of the four logical or
physical corners stores an elliptical X/Y `Radius`; every component must be
finite and non-negative. This is a closed value graph, not an arbitrary Dart
expression. The SDK exposes `CustomClipper<RRect>` as an abstract user extension
point and no public concrete delegate suitable for the bounded catalog. The
Designer therefore admits that constructor branch through a closed
`DartObjectReferenceValue`, not by trying to serialize an implementation.
The same value closes the previously deferred `ClipRect.clipper` and
`ClipOval.clipper` branches with expected type `CustomClipper<Rect>`.
The value identifies either the current paired Dart library or one canonical
package-config-declared `package:` library, one root identifier, an optional member, and either an
existing-value reference or a zero-argument constructor, factory or function invocation. A
`const` flag exists only for the invocation form. Identifiers and library URIs
are syntactically constrained, generated imports are deterministic, and normal
Dart candidate analysis must prove the emitted value assignable to
`CustomClipper<RRect>` before Save can commit. Configured arguments remain fully
available through a user-owned field/getter or zero-argument factory/function; raw
argument text never enters `.fd`.

This semantic evidence is deliberately point-in-time. The post-apply bind
re-derives the exact probe manifest, package-config mapping and real target-root
containment, but it does not hash the complete transitive Dart dependency graph.
An external dependency changed concurrently after analysis therefore does not
become a false freshness claim; retained Undo/Redo evidence remains identity
bound to the analyzed candidate and a later new Designer mutation is analyzed
again.

Generated Dart constructs the real `ClipRRect` with the exact effective clipper,
radius geometry, clip behavior and optional child; it omits the enclosing const
when a non-const reference requires that. Flutter ignores `borderRadius` when a
non-null clipper is supplied. The isolated native/exact-Web Canvas receives only
`dartObjectReferencePresence`, never the library or symbol, because it does not
compile or execute project Dart. In that branch it preserves the child and
shows an explicit accessible preview-unavailable overlay instead of presenting
a false standard radius clip. Without a custom clipper, both Canvas routes
construct the real widget and Flutter owns paint, hit testing and application
semantics. Designer selection outlines, empty-node targets and drag affordances
remain transient overlays outside the clip. The complete slice covers both
typed Properties editors, Slots, Palette/tree/Canvas creation and movement,
deterministic generation, analyzer validation, Save/reopen and further editing,
chronological Undo/Redo, accessibility and four reviewed 16/32 px light/dark
SVGs.

`ClipRRect` adds three writable rows, one source and one insertable any-widget
destination. At the new destination the 48 previous sources add 45 accepted
cells and three ParentData rejections; the new source adds 44 accepted
any-widget cells and two rejected trait-bound cells across all destinations.
The resulting surface has 742 writable rows, 725 outside Scaffold, across 49
widgets and 43 reviewed const-constructor definitions. Forty-nine sources
across 44 any-widget and two trait-bound destinations form 2,254 candidates:
2,032 accepted and 222 rejected. Layout contains 31 Palette items, Scrolling
three, Basic ten, Material four and Accessibility one.

The practical Designer backlog is now 49/92 complete, with 43 remaining. The
new typed Dart-object reference advances `.fd` schema to v12,
contributor Catalog API to 12 and Canvas model to v17. NBFC framing plus Canvas
control/wire remain v1.

## ADR-078 — ClipPath with both constructor and ShapeBorder helper branches

Status: accepted, 2026-09-05.

Add one `flutter.widgets.ClipPath` definition in Basic at order 110. Flutter
3.44.8 provides the const default constructor (`clipper`, `clipBehavior`, nullable
`child`, plus framework `key`) and the static `ClipPath.shape` helper requiring
`ShapeBorder`. Designer stable identity continues to replace user-editable `key`.
The three writable rows are optional `clipper`, optional `shape`, and optional
`clipBehavior`; `child` is an optional single any-widget slot. Setting `shape`
selects the helper. Setting both geometry branches in a model is an explicit
property conflict, never a silently ignored clipper. The Properties editor explains
branch switching and atomically resets the opposite property when accepting a new
geometry value; one Undo restores the exact previous branch and value.

Both geometry properties reuse schema-v12 closed Dart-object references:
`CustomClipper<Path>` and `ShapeBorder` respectively. Current-library and declared
package references, optional members, const/non-const zero-argument constructors,
factories and functions are supported. Parameterized construction belongs in a
project-owned getter or factory. Normal analysis and the strict-casts proof overlay
must prove the exact non-null type, including with downward generic inference.
Raw expression parsing/evaluation is not added. Shape helper invocation is always
non-const and propagates that fact through generated parents. Its class and static
method carry separate SDK symbol occurrences, while geometry carries its own
symbol/provenance and static-type requirements.

The isolated native/exact-Web Canvas receives only reference presence, never project
library paths or symbols. Default `ClipPath` uses the real Flutter widget; custom
clipper/shape branches preserve the child with an accessible preview-unavailable
warning, including zero-sized selection targets. This is an explicit preview
limitation, not a claim that the project geometry was rendered or executed.

Catalog totals become 50 widgets, 44 const-capable definitions, 745 property rows
(728 outside Scaffold), 45 any-widget and two trait destinations. The full matrix
is 2,350 cells: 2,123 accepted and 227 rejected. Categories are Layout 31,
Scrolling 3, Basic 11, Material 4, Accessibility 1. The historical practical target
is 50/92 (42 remaining); no complete ordered 92-widget inventory exists in the
tracked plan or history. ClipPath is an API-based continuation of clipping, not a
recovered plan order. `.fd` 12, Catalog API 12 and Canvas model 17 stay unchanged.

Validation for this milestone: clean Maven install and cluster assembly passed;
Surefire recorded 3,184 cases (six optional skips) and Failsafe 13 (one optional
skip), with zero failures/errors. The Flutter suite passed 446 tests and analysis
was clean. The real Flutter 3.44.8 analyzer exercised Path/ShapeBorder assignments,
generic inference, imports and invalid-type/const rejection. The packaged Web
build/cache and exact generated artifact checks passed all 41 cases. Release
verification passed; the full physical desktop gate was not run.

## ADR-079 — ClipRSuperellipse with exact typed custom delegates

Status: accepted, 2026-09-05.

Add `flutter.widgets.ClipRSuperellipse` in Basic at order 120 after ClipPath.
The pinned Flutter 3.44.8 constructor exposes optional `borderRadius`, `clipper`,
`clipBehavior` and nullable `child`; framework `key` remains Designer identity.
All non-key arguments are supported. Three typed property rows reuse physical
and directional finite non-negative elliptical radii, the closed
`CustomClipper<RSuperellipse>` reference and all four `Clip` values. The optional
single child is an insertable any-widget slot. Four distinct reviewed SVG assets
cover small/large and light/dark presentation.

Current-library and declared-package references, values/members and const or
non-const zero-argument constructors/factories/functions are admitted. Configured
arguments remain in a project-owned getter/factory, not raw property source.
Normal analysis and strict assignment proof require the exact non-null generic
type, including downward-inferred generic calls. Dynamic, nullable and
`CustomClipper<RRect>`/`CustomClipper<Rect>` values do not bypass the proof even
when project analysis ignores assignment diagnostics. Symbol/type evidence remains
bound to the candidate through pair-save verification.

Canvas constructs the actual `ClipRSuperellipse`/`RenderClipRSuperellipse`, not
ClipRRect. It honors directionality, independent corner axes, SDK radius clamping
and all clip modes. A custom clipper overrides but never deletes `borderRadius`.
The isolated runner receives reference presence only and does not execute project
code: it preserves the child with an accessible preview-unavailable warning.
Selection and empty child drop targets stay outside the paint clip. This is an
explicit preview limitation, not a missing persistence or generation branch.

The slice includes typed property and child editors, Palette/tree/Canvas insertion,
movement, generation/const propagation, validation, save/reopen/further-edit,
Undo/Redo and rollback on failed analysis. Existing value shapes retain `.fd` 12,
Catalog API 12, Canvas model 17 and NBFC framing/control/wire 1.

Totals: 51 widgets, 45 const-capable definitions, 748 property rows (731 outside
Scaffold), 46 any-widget plus two trait-bound destinations. The 2,448-cell DnD
matrix admits 2,216 and rejects 232. Categories are Layout 31, Scrolling 3,
Basic 12, Material 4, Accessibility 1. The historical practical target is 51/92,
with 41 remaining; the complete ordered target is not preserved in tracked plans.
Physical global acceptance remains deferred until the palette target is complete.

Validation for this milestone: clean Maven install and cluster assembly passed.
Surefire records 3,225 cases (six optional skips) and Failsafe 13 (one optional
skip), with zero failures/errors. The full Flutter suite passed 460 tests and
Flutter analysis was clean. Real Flutter 3.44.8 analysis verifies superellipse
references, imported and inferred calls, and rejection of wrong/nullable/dynamic
types. All 41 packaged Web build/cache and artifact cases passed; a fresh configured
pair reopen, typed property edit and subsequent save passed in the 70-case
controller integration suite. The release verifier confirms a fresh NBM with
correct metadata; installed-userdir and full physical desktop acceptance were not
run. NBM SHA-256: `e990665361392ee4e1df6b82a84af9b4c08e52d3d7845b8216b3075cf61f5bba`.

## ADR-080 — PhysicalModel with concrete radius geometry and real shadow rendering

Status: accepted, 2026-09-05.

Add `flutter.widgets.PhysicalModel` in Basic at order 130 after ClipRSuperellipse.
All six non-key constructor arguments from pinned Flutter 3.44.8 are supported:
shape, clipBehavior, borderRadius, elevation, required color and shadowColor, plus
an optional any-widget child. Framework key remains managed identity. The Palette
prototype supplies literal color `0xFF2196F3`, consistent with ColoredBox. Shape,
clipping, elevation and shadow omission preserve rectangle, none, zero and black
defaults. Both colors support literal ARGB and reviewed theme colors, with correct
const propagation through parents. Elevation is finite and non-negative.

PhysicalModel takes concrete `BorderRadius?`, not BorderRadiusGeometry. Extend the
reusable exported constraint with `BorderRadiusValues.directionalAllowed`, keeping
the zero-argument constructor equivalent to `true` for existing callers. This
widget uses `false`; model validation, property controls and the independent Canvas
decoder reject directional values even with circle shape. The exact physical-only
fingerprint differs from the existing broad geometry fingerprint, which stays
unchanged for prior clipping widgets. The exported record contract raises Catalog
API from 12 to 13, rejecting older contributors before loading their definitions.
No value-algebra change is needed: `.fd` stays 12, Canvas model 17,
NBFC framing/control/wire 1.

Physical radius omission stays omitted/null; the SDK treats it like zero corners.
Circle ignores but retains a stored radius, so switching back to rectangle recovers
the exact geometry. Non-square circle bounds form an oval in the pinned renderer.
Canvas instantiates actual PhysicalModel/RenderPhysicalModel and delegates fill,
shadow, clipping, radius clamping and translucent occluder behavior to Flutter.
An absent child is not replaced by a preview helper; selection/drop targets stay
external to real layout and paint. There is no project-code preview branch here.

The slice includes grouped Properties, physical-radius and color editors, child
management, Palette/tree/Canvas placement, same-tree movement, generation, validation,
save/reopen/further-edit, Undo/Redo, rejected-change rollback, accessibility and
four reviewed SVG variants. Current totals: 52 widgets, 46 const definitions,
754 rows (737 outside Scaffold), 47 any-widget plus two trait-bound destinations.
The 52×49 matrix has 2,548 cells: 2,311 accepted and 237 rejected. Categories are
Layout 31, Scrolling 3, Basic 13, Material 4, Accessibility 1. The historical practical
target is 52/92 with 40 remaining, not a recovered ordered full Flutter inventory. The global
physical desktop gate remains deferred until that target is complete.

Validation for this milestone: clean Maven install and cluster assembly passed.
Surefire recorded 3,255 cases (six optional skips), Failsafe 13 (one optional skip),
with zero failures/errors. Flutter analysis was clean and all 478 Flutter tests
passed, including real rectangle/oval paint, shadows, translucent occluders, theme
colors and childless targets on Windows/Web profiles. The pinned Flutter 3.44.8
analyzer accepted all constructor branches and rejected missing required color,
negative const elevation, directional radii (including circle) and invalid theme
constness. The configured save/reopen/further-edit and rejection rollback passed
in the 71-case mutation controller suite. All 41 packaged Web build/cache and exact
artifact cases passed. Release verification confirmed the fresh NBM and metadata;
installed-userdir and global physical desktop acceptance were not run.
NBM SHA-256: `bb1c78b11964427c7c108ebbcff995fa97407c2a163454678fef252ff149bffb`.

## ADR-081 — PhysicalShape with structured ShapeBorderClipper and typed project paths

Status: accepted, 2026-09-05.

Add `flutter.widgets.PhysicalShape` in Basic at order 140. Implement all five
non-key constructor arguments from pinned Flutter 3.44.8: required clipper, clip
behavior, finite non-negative elevation, required color and optional shadowColor,
plus optional any-widget child. Framework key remains Designer-managed identity.
Colors accept literal ARGB and reviewed theme tokens with correct const propagation.
Omitted clipping/elevation/shadow preserve SDK none/zero/black defaults.

The required CustomClipper<Path> cannot be instantiated by the existing zero-argument
project-reference value without user code. Introduce a typed ShapeBorderClipperValue
as a closed union branch, rather than raw generated Dart or a synthetic enum. Its
six reviewed SDK ShapeBorder implementations are rounded rectangle, beveled rectangle,
continuous rectangle, rounded superellipse, circle and stadium. The value always
retains physical/directional elliptical radius geometry and nullable explicit
textDirection. Cornered shapes with directional radii require LTR or RTL because
ShapeBorderClipper does not derive ambient Directionality. Circle/stadium ignore but
retain radius/direction, supporting lossless shape switching. The creation prototype
uses zero physical rounded corners and literal fill color 0xFF2196F3.

The same property accepts exact non-null CustomClipper<Path> references from the
current library or a declared package, including members and const/non-const
zero-argument constructor/factory/function calls. Configured arguments live in a
project getter/factory, not executable text in .fd. The analyzer verifies actual
assignment compatibility and symbol provenance, rejecting nullable, dynamic and
wrong generic types even under suppressed assignment diagnostics.

The compound editor composes unpublished preset/radius/reference drafts and flushes
an active radius-table cell only at final validation. Cancel is non-mutating; mode
changes, invalid drafts and subsequent edits retain the normal transaction boundary.
Generation emits real ShapeBorderClipper/ShapeBorder SDK constructors with accurate
helper/radius/direction probes. Canvas instantiates real PhysicalShape geometry,
shadows and translucent/theme paint for presets. Custom code is never executed in
the isolated runner: transmit reference presence only, retain child and expose an
accessible preview-unavailable warning without invented fill/shadow/clipping. Empty
targets carry the full reason outside real widget layout.

The new exported sealed value/constraint requires Catalog API 14. Canonical .fd
schema 13 adds exactly kind, shape, borderRadius and nullable textDirection fields;
frozen schemas 1-12 remain immutable and migrate only on an admitted edit. Canvas
model 18 mirrors the closed structured graph; NBFC framing/control/wire remain 1.
Properties/Slots, Palette/tree/Canvas DnD, movement, generation, validation,
Save/reopen/further-edit, Undo/Redo, rejected-change rollback and four SVG variants
form one reviewed vertical slice.

Totals: 53 widgets, 47 reviewed const definitions, 759 writable rows (742 outside
Scaffold), 48 any-widget plus two trait destinations. The 53×50 matrix contains
2,650 candidates: 2,408 accepted and 242 rejected. Categories: Layout 31, Scrolling 3,
Basic 14, Material 4, Accessibility 1. The historical practical target is 53/92,
with 39 remaining; this does not claim recovery of the missing ordered inventory.
Full physical desktop acceptance remains deferred until the palette target is done.

Validation for this milestone: the clean-build pipeline and the assembled-NetBeans
runtime rerun completed successfully after refreshing current-schema fixtures.
Surefire records 3,289 cases (six optional skips), Failsafe 13 (one optional skip),
with zero failures/errors. Flutter analysis is clean and all 500 Flutter tests
passed. Real Flutter 3.44.8 analysis accepts all six presets across clip modes and
physical/directional geometry, imported/current/generic Path references, and
rejects missing required values, wrong/nullable/dynamic clippers and invalid const
arguments. All 72 mutation-controller integration cases pass, including both saved
clipper branches, repeated reopening/further edits, Undo/Redo and rejected-change
rollback followed by a successful edit. Both 32px SVGs have validated 32×32 geometry.
All 41 actual packaged Web artifact/build/cache cases pass. Cluster assembly and
release metadata/freshness verification pass; installed-userdir and full physical
desktop acceptance were not run. NBM size: 7,257,085 bytes.
NBM SHA-256: `712c0ad7c3bd0d626fdba25bc6c74f721ae22f36ec281102a53ff224752285b1`.

## ADR-082 — RepaintBoundary as a complete structural repaint-isolation slice

Status: accepted, 2026-09-05.

Add `flutter.widgets.RepaintBoundary` in Basic at order 150 after PhysicalShape.
Pinned Flutter 3.44.8 has a const default constructor with optional child and key.
It exposes no scalar constructor properties; key remains Designer-managed identity.
The exact empty-property/optional-any-widget-child projection uses STATIC_STRUCTURAL
capabilities, like IntrinsicHeight, and does not claim scalar PROPERTIES capability.
The selected Node explicitly describes this SDK contract and exposes its editable
Child slot; descendant properties remain available by selecting the descendant.

RepaintBoundary.wrap and wrapAll only derive wrapper keys from child keys or indices;
they add no distinct paint behavior. They are not separate saved constructor modes
or childIndex properties. Runtime RenderRepaintBoundary methods such as toImage and
debug counters are likewise not constructor state. The default constructor covers
the complete visual widget contract within the existing stable-identity boundary.

Native/Web Canvas instantiates actual RepaintBoundary/RenderRepaintBoundary and
retains an independent OffsetLayer. Tests prove descendant paint dirtiness stops
at the boundary and ancestor repaint can reuse an unchanged inner layer. Layout,
constraints, intrinsic sizing, semantics and hit testing remain unchanged; no fake
child is inserted for an empty boundary. External Designer targets retain selection
and child-drop affordances. This is paint isolation, not a universal performance
improvement or a promise/control of engine raster caching.

The slice includes Palette/tree/Canvas insertion, movement, occupied-child replace,
clear/cancel, deterministic const/non-const generation, model validation, codec
round-trip, Save/reopen/further child and descendant edits, Undo/Redo, rejected-change
rollback and four size/theme-specific SVGs. No new value shape or constraint is
introduced: schema/API/model remain 13/14/18 and NBFC framing/control/wire remain 1.

Totals: 54 widgets, 48 reviewed const definitions, 759 writable rows (742 outside
Scaffold), 49 any-widget plus two trait destinations. The 54×51 matrix contains
2,754 cells: 2,507 accepted and 247 rejected. Categories: Layout 31, Scrolling 3,
Basic 15, Material 4, Accessibility 1. The historical practical target is 54/92,
with 38 remaining; the complete ordered original inventory is not recovered.
Full physical desktop acceptance remains deferred until the palette target is done.

Validation (2026-09-05): Flutter analyze reports no issues and all 517 Flutter
tests pass, including 17 RepaintBoundary render/layer/interaction tests. The pinned
Flutter 3.44.8 real-SDK analyzer gate verifies the complete constructor, key-only
helpers, symbol provenance and invalid candidates without changing project files.
The focused NetBeans pass covers 445 tests, and the full mutation integration
class passes all 73 cases. Clean Maven install passes across all 11 reactor modules;
the final recorded Surefire inventory is 3,308 tests with zero failures/errors and
six declared optional skips. Failsafe records 13 tests with zero failures/errors
and one optional native-desktop skip. All 41 actual packaged Web artifact/build
checks pass without skips. The Web bundle is rebuilt from the final Flutter
sources and both source/artifact manifests are refreshed. nbm:cluster and the
release verifier pass. The NBM is 7,258,788 bytes with SHA-256
66700A14512B8965E5C98DE051F8643BF74C26D2A2AFC25B53C76FF31F531B54.
Installed-userdir and physical desktop acceptance were not performed; optional
skips and the deferred global acceptance are not claimed as verified.

## ADR-083 — IgnorePointer retains the full pointer and deprecated semantics contract

Status: accepted, 2026-09-05.

After RepaintBoundary, admit `flutter.widgets.IgnorePointer` in Basic at order 160.
The official constructor and pinned Flutter 3.44.8 widgets/basic.dart agree: const,
optional bool ignoring (default true), deprecated bool? ignoringSemantics (default
null), optional child and managed key. Both non-key scalar parameters are supported;
deprecation is not a reason to leave a constructor branch unimplemented. Runtime
RenderObject methods, AbsorbPointer and SliverIgnorePointer are not constructor
parameters or aliases for this widget.

The catalog uses two optional BOOLEAN-only properties at orders 0 and 1, with child
at order 2 and exact STATIC_EDITABLE capability. No creation default is persisted.
Omission of ignoring preserves true; omission of ignoringSemantics preserves null,
which is behaviorally identical to explicit literal null. Explicit false and true
remain separate stored values. NullValue and arbitrary expressions are not added
as alternative encodings. Both fields use the shared centered checkbox editor and
Restore Default, preserving the independent `<not set>` state and edit identity.
The semantics field is clearly labeled deprecated with an explanation of all modes.

Canvas constructs actual IgnorePointer/RenderIgnorePointer: layout and painting
remain unchanged while ignored pointer hits can pass through to content behind the
subtree instead of being absorbed. Pinned RenderIgnorePointer blocks semantic user
actions when ignoring && (ignoringSemantics ?? true). Null keeps semantic labels and
other properties while following ignoring for action blocking; explicit false keeps
actions; explicit true drops the entire semantic subtree regardless of ignoring.
Designer selection/drop affordances remain separate from runtime pointer filtering.
The ignored node's generic body selection recognizer and opaque mouse region must
not capture a tap intended for a lower Stack child. A compact 36px external handle
preserves direct Designer selection, searching adjacent placements outside the
body, including at viewport corners. If none fits, its explicit compact hit target
may overlap the body; this is a Designer control, not runtime widget behavior.
Tree selection, geometric DnD and selected-descendant F2/keyboard editing remain
available regardless of the pointer setting.

The complete slice includes typed validation, deterministic const/non-const
generation, analyzer symbol evidence, codec/payload projection, Properties/Slots,
Palette/tree/Canvas insertion and movement, child replace/clear/cancel,
Save/reopen/further property and child edits, Undo/Redo, rejected-change rollback,
accessibility hints and four reviewed light/dark 16/32px SVGs. Existing encodings
cover the contract, so `.fd`/Catalog API/Canvas model stay 13/14/18 and NBFC framing,
control and wire stay 1.

At that milestone: 55 widgets, 49 const definitions, 761 writable rows (744 outside
Scaffold); 53 definitions have scalar properties and IntrinsicHeight/RepaintBoundary
remain structural. Fifty any-widget plus two trait destinations produce 55x52 =
2,860 compatibility cells, 2,608 accepted and 252 rejected. Categories: Layout 31,
Scrolling 3, Basic 16, Material 4, Accessibility 1. The historical practical target
is 55/92 with 37 remaining, not a recovered ordered full Flutter inventory.
Full physical desktop acceptance remains deferred until the palette target is done.

Validation (2026-09-05): final Flutter format/analyze checks pass; all 539 Flutter
tests pass, including 22 dedicated IgnorePointer cases. They verify all default/
boolean states, SDK semantics, live retained rendering, real Stack hit pass-through,
small bodies at all four viewport corners, the viewport-filling compact-handle
exception, empty/tight layout, geometric DnD and tree-selected Text F2/keyboard
commit on Windows/Web model profiles. The complete core suite passes 1,101 tests
and the focused NetBeans suite passes 509. The pinned Flutter 3.44.8 analyzer test
accepts every constructor branch (including deprecated/null semantics), validates
symbol provenance and rejects wrong/null ignoring, invalid types, invented
absorbing and invalid const descendants without changing the original project file.

Clean Maven install with real SDK and actual Web artifact/build options succeeds
across all 11 modules. Surefire records 3,329 tests, zero failures/errors and six
declared optional skips; Failsafe records 13 tests, zero failures/errors and one
optional native-desktop skip. The complete mutation integration class passes all
74 tests, and both actual Web suites pass all 41 cases without skips. Final sources
produce the refreshed source manifest and 35-file offline Web manifest; main.dart.js
is 2,861,398 bytes, SHA-256
c1999500feeac0e60073542845ad6976320fce472ef80f98ffdb98a658478247.
nbm:cluster and the release metadata/freshness verifier pass. NBM size: 7,266,662
bytes; SHA-256 DABC6EE4297808FDC480B8F55CA75AD4EF775D918EFD26D983E475CFADD21944.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-084 — AbsorbPointer preserves actual absorption and every semantics branch

Status: accepted, 2026-09-05.

After IgnorePointer, admit `flutter.widgets.AbsorbPointer` in Basic at order 170.
The official constructor and pinned Flutter 3.44.8 widgets/basic.dart define const,
optional absorbing (bool, default true), deprecated ignoringSemantics (bool?,
default null), optional child and managed key. Both scalar fields are implemented;
deprecation does not remove the compatibility branch from the Designer.

The exact STATIC_EDITABLE catalog uses optional BOOLEAN properties at constructor
orders 0 and 1, with an optional any-widget child at order 2. No forced creation
defaults are stored. Omission of absorbing preserves true; omission of semantics
preserves null and is behaviorally equivalent to an explicit literal null. Both
explicit boolean values are preserved separately from unset. Shared centered
checkboxes and Restore Default retain the existing editor identity/focus contract;
the deprecated field is visibly labeled with complete mode explanations. NullValue
and arbitrary Dart expressions are not admitted as alternate boolean encodings.

Canvas creates actual AbsorbPointer/RenderAbsorbPointer. In pinned proxy_box.dart,
absorbing returns size.contains(position) from hitTest without visiting children,
terminating Stack traversal even if the hit path itself is empty. Thus neither the
child nor the lower sibling receives a hit; this is not IgnorePointer pass-through.
With absorbing false, normal child hit testing resumes. Layout, paint and intrinsic
sizing are not changed. Semantics actions are blocked when absorbing &&
(ignoringSemantics ?? true): null retains labels and other properties, false retains
actions even while absorbing, true excludes the entire semantic subtree regardless
of absorbing. The deprecated call uses a narrow SDK warning suppression.

The outer Designer selection remains active on the AbsorbPointer body; the special
IgnorePointer nonempty-handle/transparency policy is not applied to it. Empty nodes
retain external selection/drop affordances without fake child content. Geometric
DnD, tree selection, property/slot editing and selected Text F2/keyboard commit are
separate from application pointer delivery. Preserve the previous IgnorePointer
pass-through and corner/viewport-handle regressions unchanged.

The complete slice covers typed validation, exact capability/projection,
deterministic const/non-const generation and symbol evidence, codec/payload
round-trip, Properties/Slots, Palette/tree/Canvas insertion and movement,
child replace/clear/cancel, Save/reopen/further property and child editing,
Undo/Redo, rejected-change rollback, accessibility hints and four unique SVGs.
Existing values cover this contract: schema/API/model remain 13/14/18 and NBFC
framing/control/wire remain 1.

Current totals: 56 widgets, 50 const definitions, 763 writable rows (746 outside
Scaffold); 54 scalar definitions plus the two structural definitions IntrinsicHeight
and RepaintBoundary. Fifty-one any-widget plus two trait destinations produce
56x53 = 2,968 cells, 2,711 accepted and 257 rejected. Categories: Layout 31,
Scrolling 3, Basic 17, Material 4, Accessibility 1. The historical practical target
is 56/92 with 36 remaining, not a recovered ordered full Flutter inventory.
Full physical desktop acceptance remains deferred until the palette target is done.

Validation (2026-09-05): Flutter format/analyze checks pass and all 559 Flutter
tests pass, including 20 dedicated AbsorbPointer cases. Coverage includes all nine
unset/boolean combinations, actual Stack hit absorption versus IgnorePointer
pass-through, SDK semantics, live retained rendering, empty/tight child layout,
selection, geometric DnD and selected Text F2/keyboard commit. The complete core
suite passes 1,112 tests; the focused NetBeans suite passes 517 and the complete
mutation integration class passes 75. All nine real-SDK constructor analyzer tests
pass, including the new complete AbsorbPointer constructor/provenance test and
wrong-type, invented-property, invalid-null and invalid-const rejection cases.

The clean Maven install builds and installs the first ten reactor modules with
zero widget-test failures. The final runtime module initially exposes an existing
intermittent pair-move test fixture race: it captures an ordinary Dart DataObject
before resolving the .fd model promotes that owner. An unchanged runtime rerun
passes. The test now resolves the model before capturing the current Dart owner,
asserts stable owner identity and reports actual type/validity on failure; separate
Dart-first loading and late-pair promotion tests remain intact. No production file
ownership code changes. The final runtime install rerun also passes all six cases
with one declared optional native-desktop skip.

Final Surefire reports record 3,350 tests, zero failures/errors and six declared
optional skips; Failsafe records 13 tests, zero failures/errors and one optional
skip. Both actual Web artifact/build suites pass all 41 cases without skips.
The release Web build and refreshed source/35-file offline Web manifests agree;
main.dart.js is 2,861,930 bytes, SHA-256
5a3b9caece0b43a0a96a13792e1b07b947aee926b09878123af3383a1c745e9c.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,273,108
bytes; SHA-256 F49459537E29D3AC3519137C254D217E7A3212796FF2AB9214930E2D7CE783BC.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-085 — BlockSemantics preserves paint-order and semantic-container scope

Status: accepted, 2026-09-05.

After AbsorbPointer, admit `flutter.widgets.BlockSemantics` in Accessibility at
order 20 after ExcludeSemantics. This is a reviewed API-based successor; the
repository does not contain the complete ordered historical 92-widget inventory.
The official constructor and pinned Flutter 3.44.8 basic.dart define const,
optional blocking (bool, default true), optional child and managed key. There are
no omitted constructor branches or invented pointer/descendant-exclusion fields.

The exact STATIC_EDITABLE catalog has optional BOOLEAN blocking at constructor
order 0 and optional any-widget child at order 1. No creation default is stored.
Omission preserves true; explicit false and true remain independent states across
the centered checkbox editor, Restore Default, deterministic generation, codec,
payload and history. Unknown properties, null/raw-expression encodings and
capability/schema drift are rejected before mutation.

Canvas constructs actual BlockSemantics/RenderBlockSemantics. In pinned
proxy_box.dart its only behavioral override sets
isBlockingSemanticsOfPreviouslyPaintedNodes on the semantics configuration. Earlier-
painted semantics below the same semantic boundary disappear while its own child,
later-painted nodes and content outside that boundary remain. Blocking false
restores the earlier content. Layout, paint, intrinsic sizing and ordinary pointer
hit testing still proxy the child. This is neither AbsorbPointer hit absorption
nor ExcludeSemantics descendant exclusion. Designer selection/editing and empty
external targets stay available without fabricated child content.

The complete slice includes typed validation, exact capability/projection,
const/non-const generation and analyzer symbol proof, codec/payload round-trip,
Properties/Slots, all Palette/tree/Canvas insertion and movement paths, child
replace/clear/cancel, Save/reopen/further property and descendant edits, Undo/Redo,
rejected-change rollback, accessibility hints and four distinct light/dark SVGs.
Existing encodings cover the contract: schema/API/model stay 13/14/18 and NBFC
framing/control/wire stay 1.

Current totals: 57 widgets, 51 const definitions, 764 writable rows (747 outside
Scaffold); 55 scalar definitions plus structural IntrinsicHeight/RepaintBoundary.
Fifty-two any-widget plus two trait destinations form 57x54 = 3,078 cells,
2,816 accepted and 262 rejected. Categories: Layout 31, Scrolling 3, Basic 17,
Material 4, Accessibility 2. Historical practical target: 57/92, 35 remaining.
Full physical desktop acceptance remains deferred until the palette target is done.

Validation (2026-09-05): Flutter format/analyze checks pass and all 578 Flutter
tests pass, including 19 dedicated BlockSemantics cases. Full Canvas semantics
tests cover unset/false/true, all three paint-order positions, child absent/present,
common versus real button semantic boundaries and earlier ancestor siblings on
Windows/Web model profiles. Own/later content and Designer identity remain;
outside-boundary content is retained. Live updates preserve render identity,
layout/intrinsics, paint and ordinary child hits. Empty/tight targets, child
replacement/removal, geometric DnD/moves and tree-selected earlier Text F2/keyboard
editing also pass without changes to shared Designer semantics instrumentation.

The complete core suite passes 1,123 tests and the focused NetBeans suite passes
525. The full mutation integration class passes all 76 cases. The pinned Flutter
3.44.8 constructor analyzer class passes all ten tests, including complete
BlockSemantics optional/boolean/const/non-const branches, exact symbol provenance
and rejected wrong/null kinds, invented arguments and invalid const children;
the analyzer leaves the original project file unchanged.

Clean Maven install succeeds across all 11 modules. Surefire records 3,371 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. Both actual Web
artifact/build suites pass all 41 cases without skips. Release Web build succeeds;
all 40 source-manifest and 35 offline Web-manifest entries match final files.
main.dart.js is 2,862,308 bytes, SHA-256
efeb9a9cd5fe35360c301449a1d611341cac2779ce673590b0844aad4b3f083c.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,279,469
bytes; SHA-256 D03DC01119F921D2A7920EB3200783BA5D44A84F71FBEF816989DDC27FBAD73D.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-086 — MergeSemantics completes structural child editing and semantic merging

Status: accepted, 2026-09-05.

After BlockSemantics, admit `flutter.widgets.MergeSemantics` in Accessibility at
order 30. This is a reviewed API-based successor, not a recovered ordered entry
from the historical 92-widget target. The official constructor and pinned Flutter
3.44.8 basic.dart define const, optional child and key. Key remains Designer-managed
identity. There are no scalar constructor fields to postpone or invent.

Use exact STATIC_STRUCTURAL capability like IntrinsicHeight/RepaintBoundary: Canvas,
Create and DnD with identity/child-slot editing, not scalar Properties mutation.
The optional any-widget child has constructor order 0. Unknown properties and
schema/capability drift, including required/list children, fail closed. Empty slot,
omitted slot and populated child preserve their deterministic encoding/generation.
Const propagation follows descendants; project/theme-dependent children remove
const only where required. Analyzer evidence proves the actual SDK constructor.

Canvas constructs actual MergeSemantics/RenderMergeSemantics. The pinned render
object sets isSemanticBoundary and isMergingSemanticsOfDescendants. Descendant
labels, flags and actions merge into one semantic node. Labels concatenate with
newlines; when descendants share an action, its first tree-order handler wins.
Conflicting flags can produce a misleading combined description; no Designer-only
validation or reconciliation policy is invented. Layout, paint, intrinsic sizing
and ordinary hits still proxy the child. Project callbacks remain isolated/no-op
in Canvas and are emitted normally in generated Dart.

Designer selection gestures must not override merged application semantic actions.
Suppress only synthetic Designer gesture semantics and identity/selected annotations
inside the merged subtree, retaining actual widget semantics and accessible preview
diagnostics. Keep outer MergeSemantics Designer identity, individual NetBeans tree
navigation, mouse selection/double-click, keys/outlines, geometric DnD and F2 editing.
Nested merging carries this scope; children moved outside regain normal Designer
semantics. No blanket ExcludeSemantics or fake application node is introduced.

The full slice includes exact validation/projection, const/non-const generation and
provenance, codec/payload round-trip, identity/Slots, all Palette/tree/Canvas insertion
and movement routes, child add/replace/clear/cancel, Save/reopen/further child and
descendant edits, Undo/Redo, rejected-change rollback, accessibility hints and four
distinct light/dark 16/32px SVGs. Existing encodings suffice: schema/API/model stay
13/14/18 and NBFC framing/control/wire stay 1.

Current totals: 58 widgets, 52 const definitions, 764 writable rows (747 outside
Scaffold); 55 scalar definitions plus structural IntrinsicHeight/RepaintBoundary/
MergeSemantics. Fifty-three any-widget plus two trait destinations form 58x55 =
3,190 cells, 2,923 accepted and 267 rejected. Categories: Layout 31, Scrolling 3,
Basic 17, Material 4, Accessibility 3. Historical practical target: 58/92 with 34
remaining. Full physical desktop acceptance remains deferred until the palette
target is done.

Validation (2026-09-05): final Flutter format/analyze checks pass and all 602 Flutter
tests pass, including 24 dedicated MergeSemantics cases. Full Canvas verifies one
exported merged descendant node, separate outside and outer Designer nodes,
combined labels/states, nested groups and normal pointer/layout/paint/intrinsic
behavior. Plain Text does not acquire a synthetic tap; real button semantic taps
do not select a Designer ancestor, and TextField contributes its genuine state.
Image-unavailable diagnostics remain accessible. Actual reparenting of the same
descendant outside and back restores/removes only synthetic Designer contributions.
Childless/tight nodes, replacement/clear, DnD/moves and F2 descendant editing pass
on Windows/Web model profiles. A complementary real-SDK callback-counter test
proves tree-order handler selection and conflicting checked-state behavior without
executing project callbacks. The complete core suite passes 1,135 tests and the
focused NetBeans suite passes 533. The dedicated pinned Flutter 3.44.8 analyzer
test accepts all structural/const/non-const constructor branches, verifies symbol
provenance and rejects invented fields, wrong child types and invalid const without
changing the original project file.

Clean Maven install succeeds across all 11 modules. Surefire records 3,393 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. The complete real-SDK
analyzer class passes 11 cases and mutation-controller integration passes 77.
Both actual Web artifact/build suites pass all 41 cases without skips. Release
Web build succeeds; all 40 source-manifest and 35 offline Web-manifest entries
match final files. main.dart.js is 2,864,282 bytes, SHA-256
70c080b1e8def70ddd5ee0aa991364e62e875b3ef5a37f4933f1adea27b42edd.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,281,998
bytes; SHA-256 ABFA580A8DB3B95B994CA6795771DD8A45A0DFB760809A18AE6B8984100B5664.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-087 — IndexedSemantics completes required index editing and scroll semantics

Status: accepted, 2026-09-05.

After MergeSemantics, admit `flutter.widgets.IndexedSemantics` in Accessibility at
order 40. This is a reviewed API-based successor, not a recovered ordered entry
from the historical 92-widget target. The official constructor and pinned Flutter
3.44.8 basic.dart define const, required int index, optional child and key.
Key remains Designer-managed identity; no public constructor branch is postponed.

Use exact STATIC_EDITABLE capability. Required INTEGER index is constructor order
0, with Designer prototype 0 and shared portable integer range
-9007199254740991..9007199254740991. Zero is not a Flutter constructor default.
Required omission/null and noninteger encodings fail closed; unset cannot erase
the argument. The signed range matches existing native/Web model portability,
not an invented SDK bound. The pinned constructor/render object does not assert
nonnegativity. Child is optional SINGLE any-widget at order 1. Unknown fields,
altered defaults/orders/constraints/requiredness and slot drift cannot borrow the
canonical projection or capabilities.

Canvas constructs actual IndexedSemantics/RenderIndexedSemantics. The render object
sets indexInParent, with normal layout, paint, intrinsic sizing and ordinary hits.
The index annotates the first child semantic node according to actual SDK topology;
multiple semantic children can have an indexed container rather than an indexed
leaf. Do not invent boundaries or force an index onto every descendant. Changes
update semantic metadata, not sibling order or indexes. Manual ListView indexing
requires explicit addSemanticIndexes and semanticChildCount settings; Designer
does not silently rewrite parent properties or renumber children after moves.

Extend the existing MergeSemantics descendant scope to IndexedSemantics and name
the internal flag suppressDesignerSemantics. Suppress only synthetic Designer
identity/selected annotations and GestureDetector semantics in those descendants,
retaining actual widget semantics and accessible preview diagnostics. IndexedSemantics
does not create the isolating SDK boundary that MergeSemantics creates, so the
IndexedSemantics wrapper's own synthetic annotations/actions must also be suppressed.
Do not insert a container or explicitChildNodes boundary to compensate. Outer
MergeSemantics Canvas identity remains unchanged; accessible IndexedSemantics
identity/editing stays in the NetBeans tree. Mouse selection/double-click,
keys/outlines, geometric DnD and F2 editing remain available. Nested wrappers carry
the scope; children moved outside regain normal Designer contributions. No blanket
ExcludeSemantics, fake application node or forced leaf index is introduced.

Outside-scope ancestors such as Center retain their existing Designer labels and
selection actions. The SDK may combine those ancestor contributions with a
non-boundary child; this change does not claim an entirely uninstrumented Canvas
semantic tree. Tests distinguish wrapper/descendant-owned suppression from retained
ancestor behavior, and calibrate indexed container versus leaf topology against
the pinned SDK rather than forcing semantic boundaries to satisfy a test.

The complete slice includes typed Properties/Slots, all Palette/tree/Canvas
insertion and movement routes, index edits and child add/replace/clear/cancel,
const/non-const generation/provenance, codec/payload round-trip, Save/reopen/further
index and descendant edits, Undo/Redo, rejected-change rollback, accessibility
hints and four distinct light/dark 16/32px SVGs. Existing encodings suffice:
schema/API/model remain 13/14/18 and NBFC framing/control/wire remain 1.

Current totals: 59 widgets, 53 const definitions, 765 writable rows (748 outside
Scaffold); 56 scalar definitions plus structural IntrinsicHeight/RepaintBoundary/
MergeSemantics. Fifty-four any-widget plus two trait destinations form 59x56 =
3,304 cells, 3,032 accepted and 272 rejected. Categories: Layout 31, Scrolling 3,
Basic 17, Material 4, Accessibility 4. Historical practical target: 59/92 with 33
remaining. Full physical desktop acceptance remains deferred until the palette
target is done.

Validation (2026-09-05): Flutter analyze is clean and all 628 Flutter tests pass,
including 26 dedicated IndexedSemantics cases. Coverage includes signed portable
endpoints and closed decoding, live RenderIndexedSemantics reuse/index updates,
raw-SDK-calibrated anonymous-parent versus leaf topology, nested/merge/exclude/block
compositions, actual ListView scrollIndex changing 0 to 1 across an unindexed
separator with explicit count, real button semantic actions, and accessible Image
preview diagnostics. Tests preserve outside-scope ancestor annotations while
rejecting wrapper/descendant-owned synthetic contributions; actual reparenting
out and back restores those contributions without leaking the index. Ordinary
layout, paint, intrinsic sizing, pointer selection, childless/tight targets,
child replacement/clear, DnD/moves and descendant F2 editing pass on Windows/Web
model profiles. The full core suite passes 1,146 cases and the focused NetBeans
suite passes 541. The dedicated real Flutter 3.44.8 analyzer test accepts every
constructor branch, signed bounds, const/non-const and scroll/merge compositions,
verifies SDK symbol provenance, rejects missing/null/wrong-type/invented fields
and invalid const, and leaves the original project file unchanged.

Clean Maven install succeeds across all 11 modules. Surefire records 3,414 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. The complete real-SDK
analyzer class passes 12 cases and mutation-controller integration passes 78.
Both actual Web artifact/build suites pass all 41 cases without skips. Release
Web build succeeds; all 40 source-manifest and 35 offline Web-manifest entries
match final files. main.dart.js is 2,864,730 bytes, SHA-256
be035178d3d04962798ce9953f6d93b07d728d7d13a7fe47bef14247e3e31b23.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,288,245
bytes; SHA-256 706247B7669FD50394D31FE2ED3C87B8345587CF2FE1C1E54EB408801B8282D1.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-088 — ExcludeFocus completes descendant focus gating and required-child wrapping

Status: accepted, 2026-09-05.

After IndexedSemantics, admit `flutter.widgets.ExcludeFocus` in Accessibility at
order 50. This is a reviewed API-based successor, not a recovered ordered entry
from the historical 92-widget target. The official constructor and pinned Flutter
3.44.8 focus_scope.dart define const, optional bool excluding (default true),
required Widget child and key. Key stays Designer-managed; no constructor field
is deferred or replaced by an internal Focus parameter.

Use exact STATIC_EDITABLE capability. Optional BOOLEAN excluding is order 0 with
no persisted creation default: omission preserves SDK true, explicit false and true
remain separate states. The shared centered checkbox and Restore Default retain
this distinction. Child is required SINGLE any-widget at order 1, min/max 1.
Unknown fields, null/wrong boolean kinds, altered defaults/orders/constraints and
required-child slot drift fail closed. Existing encodings suffice: schema/API/model
remain 13/14/18 and NBFC framing/control/wire remain 1.

Reuse the existing generic atomic wrapper workflow from SafeArea/Directionality.
Palette/tree wrapping takes an existing root or non-root subtree; Canvas exposes
existing non-root targets and no synthetic root target. Do not persist an empty
prototype or manufacture a child. The occupied required child supports atomic
replacement, not clearing/removal or an insertable destination in the palette
matrix. Expanded/Flexible/Spacer remain constrained to direct Row/Column ParentData
placement and cannot be wrapped. Parent slot/trait compatibility, revision-bound
intent admission, cancellation and rejected-change rollback remain authoritative.

Canvas constructs actual ExcludeFocus. Its SDK Focus sets canRequestFocus false,
skipTraversal true, includeSemantics false and descendantsAreFocusable to !excluding.
It gates descendant focus and traversal, unfocuses descendants when exclusion
turns on, and does not automatically refocus them when exclusion turns off.
Descendants' own configured focus flags are not rewritten; their effective
FocusNode.canRequestFocus includes ancestor exclusions and can therefore be false.
Normal layout, paint, intrinsic sizing, pointer hits and semantic labels remain.
The widget does not act as ExcludeSemantics or IgnorePointer, and project callbacks
remain isolated according to the existing Canvas contract.

Designer inline Text editing is a service operation, not application focus. Keep
the actual ExcludeFocus in effect while attaching only the temporary inline editor
focus branch to the outer Designer FocusNode. This service-only Focus parent has
canRequestFocus false, skipTraversal true and includeSemantics false. Do not change
excluding, enable application focus, add a model value or introduce a semantic
boundary merely to let F2 edit the selected Text. Pointer/tree selection and
property/slot editing retain their existing host-authoritative routes.

Canvas move previews also reject moving a direct required child away from its
own slot, using the existing canonical required-wrapper classifier rather than
a new widget list. This covers SafeArea/Directionality/ExcludeFocus/Expanded/
Flexible and only subtracts misleading accepted previews. Same-slot no-op geometry,
moving an intact wrapper and moving ordinary optional/list children retain their
existing behavior; the host still owns final revision/placement/mutation validation.

The full slice covers typed validation/projection, generation/provenance,
const/non-const composition, codec/payload round-trip, centered checkbox/reset,
required-child wrapping/replacement/cancel, moves, Save/reopen/further widget and
descendant edits, Undo/Redo, rejected-change rollback, accessibility hints and
four distinct light/dark 16/32px SVGs.

At that milestone: 60 widgets, 54 const definitions, 766 writable rows (749 outside
Scaffold); 57 scalar definitions plus structural IntrinsicHeight/RepaintBoundary/
MergeSemantics. The 54 any-widget plus two trait destinations remain 56 insertable
slots: 60x56 = 3,360 cells, 3,086 accepted and 274 rejected. Categories: Layout 31,
Scrolling 3, Basic 17, Material 4, Accessibility 5. Historical practical target:
60/92 with 32 remaining. Full physical desktop acceptance remains deferred until
the palette target is done.

Validation: Flutter analysis reports no issues, all 28 focused ExcludeFocus tests
pass, and the complete Canvas suite passes 656 tests. They cover SDK focus and
traversal exclusion, unfocus/no automatic refocus, nested exclusions, configured
versus effective focus flags, unaffected layout/paint/intrinsics/pointer/labels,
zero-size selection handles and Windows/Web model profiles. F2 editing, Escape,
reopening and existing inline-editor regression tests pass while an actual sibling
application control remains excluded. Required-child move-preview regressions pass
for all five reviewed wrappers, retaining same-slot, intact-wrapper and ordinary
optional/list-child behavior. Core tests pass 1,157 cases; focused NetBeans tests
pass 552. The dedicated real Flutter 3.44.8 analyzer test accepts all constructor
branches, nested/scroll/semantics and const/non-const compositions, verifies SDK
symbol provenance, rejects missing/null/wrong-type/invented fields and invalid
const, and leaves the original project file unchanged.

Clean Maven install succeeds across all 11 modules. Surefire records 3,438 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. The complete real-SDK
analyzer class passes 13 cases and mutation-controller integration passes 79.
Both actual Web artifact/build suites pass all 41 cases without skips. Release
Web build succeeds; all 40 source-manifest and 35 offline Web-manifest entries
match final files. main.dart.js is 2,865,554 bytes, SHA-256
ecb7acd386cccbe5ed385a370436c8fb8f1202bc00ed6b722f9880b79eda5744.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,295,440
bytes; SHA-256 E47E89C912D69635641AB66F01FFDF4082304B444E39A786FBCE395B3D6CAF67.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-089 — ExcludeFocusTraversal completes traversal-only exclusion

Status: accepted, 2026-09-05.

After ExcludeFocus, admit `flutter.widgets.ExcludeFocusTraversal` in Accessibility
at order 60. This is a reviewed API-based successor, not a recovered ordered entry
from the historical 92-widget target. The official constructor and pinned Flutter
3.44.8 focus_traversal.dart:2439–2472 define const, optional bool excluding (default
true), required Widget child and key. Key stays Designer-managed; every public
constructor field is covered without substituting an internal Focus parameter.

Exact STATIC_EDITABLE capability exposes optional BOOLEAN excluding at order 0
with no persisted creation default. Centered checkbox false/true and unset/reset
stay distinct; omission preserves SDK true. Child is required SINGLE any-widget
at order 1, min/max 1. Unknown fields, null/wrong boolean kinds, changed defaults,
orders, constraints, requiredness and single/list slot drift fail closed. Existing
encodings suffice: schema/API/model remain 13/14/18, NBFC framing/control/wire 1.

Reuse generic atomic required-child wrapping from SafeArea, Directionality and
ExcludeFocus. Palette/tree can wrap existing root/non-root subtrees; Canvas uses
existing non-root targets and has no synthetic root target. Do not manufacture a
child or admit an empty prototype. Required child replacement is atomic, cannot
clear/remove the slot, and adds no insertable destination. Direct Row/Column
Expanded/Flexible/Spacer ParentData restrictions remain. Existing revision/slot
fences, cancellation, stale admission and rejected-change rollback retain authority.

Canvas constructs actual ExcludeFocusTraversal. Its SDK Focus sets canRequestFocus
false, skipTraversal true, includeSemantics false and descendantsAreTraversable to
!excluding. Unlike ExcludeFocus, traversal exclusion allows explicit focus requests
and retains already focused descendants. Other focus restrictions still apply;
an inner excluding:false cannot override an excluded ancestor. Local configured
skipTraversal flags are not rewritten by this wrapper, but FocusNode.skipTraversal
at focus_manager.dart:489–499 includes ancestor descendantsAreTraversable flags,
so its effective getter can become true. Do not confuse local settings with that
effective getter or imply that traversal exclusion grants focus through ExcludeFocus.
Turning traversal back on does not itself request or move focus. Framework-owned
TextField internal Focus synchronization is not a Designer-controlled reset of
its external FocusNode state; isolated explicit-node and actual-TextField tests
cover their distinct behavior rather than imposing an invented SDK invariant.

Normal layout, paint, intrinsics, pointer hits and semantic labels remain. The
temporary F2 Text editor retains the existing Designer-only focus parent; real
application traversal restrictions are not toggled to permit editing. No model
value, semantic boundary or new host mutation authority is introduced. The existing
generic required-wrapper classifier handles wrapping and source-child move-preview
rejection; intact wrappers and ordinary optional/list children remain movable.

The full slice covers typed schema/projection, generation/provenance, const and
non-const compositions, codec/payload, centered checkbox/reset, wrapping/replacement/
cancel, all placement/move routes, Save/reopen/further widget and descendant edits,
Undo/Redo, rejected-change rollback, accessibility hints and four distinct light/dark
16/32px SVGs. At that milestone: 61 widgets, 55 const definitions, 767 writable rows
(750 outside Scaffold); 58 scalar plus structural IntrinsicHeight/RepaintBoundary/
MergeSemantics. The 54 any-widget plus two trait destinations remain 56 insertable
slots: 61x56 = 3,416 cells, 3,140 accepted and 276 rejected. Categories: Layout 31,
Scrolling 3, Basic 17, Material 4, Accessibility 6. Historical practical target:
61/92 with 31 remaining. Full physical desktop acceptance remains deferred until
the palette target is done.

Validation: all 29 focused ExcludeFocusTraversal Flutter tests and the complete
685-test Canvas suite pass; analyze and format checks are clean. Actual Tab and
Shift+Tab, direct focus requests, retained focus across toggles, configured versus
effective flags, nested exclusions, ExcludeFocus composition, unchanged layout/
paint/intrinsics/labels, pointer selection, zero-size handles and F2 commit/Escape
pass for Windows/Web model profiles. Existing required-wrapper move rejection
also covers the new type. All 1,171 core and 642 focused NetBeans tests pass,
including 80 mutation lifecycle cases and the shared 113-Boolean editor contract.
The real Flutter 3.44.8 analyzer accepts all constructor branches, mixed focus/
semantics/scroll compositions and const/non-const values, verifies SDK symbol
provenance, rejects missing/null/wrong-type/invented fields and invalid const,
and leaves the original project file unchanged.

Clean Maven install succeeds across all 11 modules. Surefire records 3,465 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. The complete real-SDK
analyzer class passes 14 cases and mutation-controller integration passes 80.
Both actual Web artifact/build suites pass all 41 cases without skips. The
NetBeans Surefire fork needed its post-System.exit(0) shutdown timeout; the reactor
and recorded test reports still completed successfully, with no test failures.
Release Web build succeeds; all 40 source-manifest and 35 offline Web-manifest
entries match final files. main.dart.js is 2,866,072 bytes, SHA-256
6badfed353d79332d83d7ba92f716a12220cb3be48124192b44cd4ab8ee45ff2.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,301,205
bytes; SHA-256 90C9E830C06EA42A6AAF9A17ADA8484B110CC8AE0CFA40BE0E3430F9B3866219.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-090 — Visibility completes maintained state and alternate-child editing

Accepted: 2026-09-05.

Add flutter.widgets.Visibility as one STATIC_EDITABLE Basic palette definition at
order 180. The reviewed official API and pinned Flutter 3.44.8 visibility.dart agree:
the bare constructor is const, child is required and non-null, replacement is
optional and non-null with SizedBox.shrink() as its default, visible defaults to
true and the six maintain flags default to false. The managed key is not a writable
constructor property. All seven Boolean arguments are exposed without materialized
creation defaults: visible, maintainState, maintainAnimation, maintainSize,
maintainSemantics, maintainInteractivity and maintainFocusability. Child has Dart
order 0, replacement order 1, and the scalar arguments orders 2 through 8.

The SDK Visibility.maintain constructor is fully represented by its documented exact
equivalent: the bare constructor with all six maintain flags true and replacement
omitted. No duplicate palette type or redundant source-spelling selector is needed.
The generated const/non-const decision includes both retained child subtrees, even
when replacement is currently ignored. A missing or empty replacement slot must
omit that constructor argument, never generate replacement: null. This explicit
non-null default case extends the generator's optional-single-slot omission policy.

Validate all five SDK implications for every visible state: maintainAnimation
requires maintainState; maintainSize requires maintainAnimation; maintainSemantics
and maintainInteractivity require maintainSize; maintainFocusability requires
maintainState. Decode and command validation reject malformed imported states.
The seven shared checkbox editors retain explicit true, explicit false and unset.
Enabling a dependent flag supplies missing/false transitive prerequisites in one
atomic PatchProperties command. Disabling or resetting a prerequisite resets only
currently-true transitive dependents, preserving unrelated and explicit-false rows.
The edited row's true/false/unset state is retained; visible has no dependencies.
One user edit remains one Undo step and refreshes the affected properties only.

The child slot is required SINGLE any-widget with min=max=1. Replacement is optional
SINGLE any-widget with min=0 and max=1; it supports selection, add, replace, clear,
cancel and stale-owner protection. Generalize the shared wrapper-shape classifier
to accept a required single child plus other optional zero-minimum slots. Multiple
required slots and required scalar arguments lacking creation defaults still fail
closed. Visibility wraps existing valid targets atomically, without a fabricated
child. Removing or moving its required child away is forbidden; moving the intact
wrapper or removing/moving its optional replacement follows normal placement rules.
Canvas and host use matching generic classification rather than separate whitelists.

Render the actual SDK Visibility widget. Its configured flags control retained child
state, tickers, layout/intrinsics, painting, hit testing, semantics and focus.
Synthetic Designer geometry follows the active branch: child when visible;
replacement only when hidden without maintainState. Hidden/inactive descendants
cannot resurrect handles, drop targets or F2 editors. Hiding an active F2 editor
cancels its draft; both branches remain accessible through the tree and property
editors. Retained runtime state follows Flutter, not a persisted Designer value.

The pinned SDK has a separate confirmed semantics invalidation defect: its
_RenderVisibility.visible setter (visibility.dart:596-602) marks paint only, while
the maintainSemantics setter marks semantics too. With maintainSize and
maintainInteractivity enabled and maintainSemantics false, toggling visible can
leave an unchanged child's semantic label cached; rebuilding the semantic child
can instead expose the parentDataDirty assertion on reveal. A minimal uninstrumented
SDK composition reproduces this and the raw-SDK characterization test records the
known defect. Canvas keeps actual Visibility under a small state holder and calls
the public markNeedsSemanticsUpdate on its existing SDK render object before a
visibility transition from maintained-size mode. Corrected Canvas tests require
hide/reveal, retained child, changing flags, nested Visibility and semantics
composition to behave correctly. No SDK file, layout/paint/hit/focus implementation
or semantic boundary is replaced. This workaround is Canvas-only: generated
application Dart still calls the upstream SDK directly and is not globally patched.

The completed slice includes schema/capability projection, generation/provenance,
codec/payload, both slot editors, centered checkbox/unset/dependency handling,
palette/tree/Canvas placement, wrapping and reparenting, save/reopen and further
editing of both subtrees, Undo/Redo, failed-change rollback, accessibility hints and
four distinct light/dark 16/32px SVGs. No format/protocol bump is necessary:
FD schema 13, API model 14, Canvas model 18 and NBFC1 remain unchanged.

At that milestone: 62 widgets, 56 const definitions, 774 writable rows (757 outside
Scaffold); 59 scalar plus structural IntrinsicHeight/RepaintBoundary/MergeSemantics.
The 55 any-widget plus two trait destinations give 57 insertable slots:
62x57 = 3,534 cells, 3,253 accepted and 281 rejected. Categories: Layout 31,
Scrolling 3, Basic 18, Material 4, Accessibility 6. Historical practical target:
62/92 with 30 remaining. The earlier ordered 92-widget inventory has not been
recovered; this addition follows reviewed official API coverage, not an invented
historical ordering. Full physical desktop acceptance remains deferred until the
palette target is complete.

Focused validation: all 38 new Visibility Flutter cases and the complete 723-test
Canvas suite pass, with analyze and format checks clean. Core tests pass all 1,191
cases, including exhaustive validation of 2,187 Boolean true/false/unset combinations.
The focused NetBeans suite passes 659 cases, including the shared 120-Boolean editor
contract. A dedicated real Flutter 3.44.8 analyzer case accepts the full constructor
and maintain-equivalent branches, validates SDK symbol provenance, rejects missing,
null, wrong-type, dependency-invalid and invented arguments, and preserves the
original project file. Final clean-build and release results are recorded below.

Clean Maven install succeeds across all 11 modules. Surefire records 3,503 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. The complete real-SDK
analyzer class passes 15 cases and mutation-controller integration passes 82,
including the final required-child move-target regression. Both actual Web
artifact/build suites pass all 41 cases without skips. Release Web build succeeds;
all 40 source-manifest and 35 offline Web-manifest entries match final files.
main.dart.js is 2,869,298 bytes, SHA-256
88e4b056dccc69927b410dd1476f1d8aada8340ac1f097cd9ca1d72632f27ed4.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,310,919
bytes; SHA-256 EC00CA39F96FC70DE06D39ABF1CADF25BD075F1BCBBF1D498BF1C596488B689A.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-091 — TickerMode completes required enabled and forceFrames control

Accepted: 2026-09-05.

Add flutter.widgets.TickerMode as one STATIC_EDITABLE Basic palette definition at
order 190. The official API and pinned Flutter 3.44.8 ticker_provider.dart define
the complete const constructor: required bool enabled, required Widget child and
optional bool forceFrames=false, plus managed key. Project enabled as required
BOOLEAN at Dart order 0 with explicit Designer creation value true; this is not a
Flutter default. Project required SINGLE any-widget child at order 1 with min=max=1,
and optional BOOLEAN forceFrames at order 2 without a persisted default.

Both flags are editable with the shared centered checkbox. Enabled allows true and
false but cannot be unset or reset because its constructor argument is required.
Force frames retains unset/false/true, with unset omitting the Dart argument and
preserving false. False enabled combined with true forceFrames is valid; there is
no cross-property dependency. Validate missing/wrong-type enabled, invalid optional
types and missing/null/empty required child at the model boundary. Imported malformed
values must not gain authority through Canvas or command replay.

The existing generic atomic wrapper path already accepts required scalar arguments
when the catalog provides creation values. Reuse it without changing wrapper rules
or adding fake children. Tree drops can wrap a root or non-root target; the current
Canvas wire offers non-root targets only. Required child replacement is atomic,
while clearing/removing/moving it away is forbidden. Moving the intact wrapper is
allowed by the normal catalog matrix. Expanded/Flexible/Spacer remain invalid child
targets because their ParentData needs direct Row/Column ownership.

Use actual SDK TickerMode, including both widget-aware ticker-provider mixins.
Effective enabled is ancestorEnabled AND localEnabled, and effective forceFrames is
ancestorForceFrames OR localForceFrames. Enabled true cannot undo ancestor disabling;
forceFrames false cannot undo ancestor forcing. The widgets mute callbacks, not
elapsed time: an animation can advance on resuming rather than restarting from its
last painted value. Disabling does not dispose the child, change its layout/paint,
hide its semantics, reject ordinary pointer hits or prevent focus. Canvas must not
reuse Visibility's hidden-branch filter for merely muted tickers. Both fields update
the real SDK notifiers, including inherited changes and reparenting.

TickerMode.merge is a static Widget-returning helper, not another constructor. It
adds a Builder and substitutes ambient values for null requests; after TickerMode's
AND/OR combination, null enabled has the same effective behavior as true and null
forceFrames as false. The palette can represent every effective flag combination
with the bare constructor; it does not preserve that helper's Builder source shape
or introduce a redundant catalog type. SDK static value/notifier accessors work
normally through the actual rendered widget.

The full slice covers typed schema/capability projection, generation/provenance and
const propagation, codec/payload, both checkbox contracts, Slots replacement,
palette/tree/Canvas placement and movement, save/reopen/further editing, Undo/Redo,
rollback, accessibility and four light/dark 16/32px SVGs. No format/protocol bump:
FD schema 13, API model 14, Canvas model 18 and NBFC1 remain unchanged.

At that milestone: 63 widgets, 57 const definitions, 776 writable rows (759 outside
Scaffold); 60 scalar plus structural IntrinsicHeight/RepaintBoundary/MergeSemantics.
The 55 any-widget plus two trait destinations remain 57 insertable slots:
63x57 = 3,591 cells, 3,308 accepted and 283 rejected. Categories: Layout 31,
Scrolling 3, Basic 19, Material 4, Accessibility 6. Historical practical target:
63/92 with 29 remaining. This is an API-reviewed successor; the historical ordered
92-widget inventory has not been recovered. Full physical desktop acceptance stays
deferred until the palette target is complete.

Focused validation: all 29 new TickerMode Flutter cases and the complete 752-test
Canvas suite pass; analyze and formatting are clean. Tests exercise all 16 nested
flag combinations, single/multiple ticker providers, mute/unmute with elapsed-time
catch-up, AnimationController completion after unmuting, stable value notifiers,
inherited changes, GlobalKey reparenting, merge-null effective equivalence,
unmanaged ticker limitations and Visibility composition. Real paint/layout,
intrinsics, hit testing, labels, retained focus, Canvas wrapping/moves and F2
commit/cancel/reopen remain correct on Windows/Web model profiles.

Core tests pass all 1,208 cases, including 17 new TickerMode contract/command/payload
cases. The focused NetBeans suite passes 671 cases, including 12 new tests, 83
mutation lifecycle cases and the shared 122-Boolean editor contract. Required
Enabled rejects unset/reset and toggles both ways; Force frames preserves optional
states without changing Enabled. Persistence tests cover save/reopen/further widget
and descendant editing, child replacement, Undo/Redo and rejected-change rollback.

Clean Maven install succeeds across all 11 modules. Surefire records 3,533 tests,
zero failures/errors and six declared optional skips; Failsafe records 13 tests,
zero failures/errors and one optional native-desktop skip. The complete real-SDK
analyzer class passes 16 cases, including all TickerMode constructor combinations,
static merge and canonical effective behavior, nested/theme/focus/semantics/scroll
compositions, invalid required/optional values, const rejection, exact SDK symbol
provenance and original-file preservation. Mutation-controller integration passes
83 cases. Both actual Web artifact/build suites pass all 41 cases without skips.
Release Web build succeeds; all 40 source-manifest and 35 offline Web-manifest
entries match final files. main.dart.js is 2,869,722 bytes, SHA-256
17f429b26c097190ef17f15bf0d8fbf46e171b227abb22ee18bc590562d92835.
nbm:cluster and release metadata/freshness verification pass. NBM size: 7,317,721
bytes; SHA-256 BF68C564A21ABA133396CF1E1773944752ADFD416BB0B95B53A7810059520DED.
Installed-userdir and full physical desktop acceptance were not performed; optional
skips are recorded and not presented as passed physical tests.

## ADR-092 — DefaultTextHeightBehavior completes the required inherited composite

Accepted: 2026-09-05.

Add flutter.widgets.DefaultTextHeightBehavior as one STATIC_EDITABLE Basic palette
definition at order 200. The reviewed official API and pinned Flutter 3.44.8 text.dart
define a const constructor with required non-null TextHeightBehavior and required
non-null child, plus managed key. The TextHeightBehavior value itself exposes all
three supported arguments: applyHeightToFirstAscent=true, applyHeightToLastDescent=true
and leadingDistribution=TextLeadingDistribution.proportional, with even as the other
enum member. There are no further public value constructor branches to defer.

Reuse the existing Text flattened names textHeightApplyFirstAscent,
textHeightApplyLastDescent and textHeightLeadingDistribution, their Boolean/closed
enum representations and scalar property editors. Catalog leaf orders are 0/1/2,
with the required SINGLE any-widget child at order 3 and min=max=1. All scalar
leaves are optional and have no creation defaults. Generation maps them to the
actual inner member names and always emits the required textHeightBehavior composite
before child. All-unset therefore produces const TextHeightBehavior(), not an
omitted argument or null. This differs deliberately from Text's optional local
composite, which must still disappear when its last explicit leaf is reset.

The two booleans render and edit as centered checkboxes, retaining unset/false/true.
The enum retains unset/even/proportional. Reset removes only the selected leaf;
all-unset still creates the value's true/true/proportional defaults and replaces
an outer inherited value as a whole. It does not merge missing leaves with the
outer wrapper. Validate all 27 omission/value combinations, wrong kinds, foreign
enum domains/members and malformed required child; do not impose new TextStyle
height constraints. Leading distribution is applied before the first-ascent and
last-descent flags, and the wrapper itself does not supply a font size or height.

Reuse the generic atomic required-child wrapper flow, with tree root/non-root and
Canvas non-root targets. Required child replacement is atomic; clearing/removal/
moving it away independently is forbidden. Intact wrappers use normal placement
and history rules. Expanded/Flexible/Spacer cannot become its child because their
ParentData requires a direct Row/Column ancestor. No empty child is fabricated.

Render actual SDK DefaultTextHeightBehavior with an always-present value. The
existing nullable Text helper remains unchanged; only this required wrapper supplies
its default when no leaf is explicit. Descendant Text prefers an explicit local
behavior, then a non-null DefaultTextStyle behavior, then the nearest inherited
DefaultTextHeightBehavior. EditableText's explicit/inherited behavior, nested
all-unset reset, inherited notifications, InheritedTheme wrap/capture and actual
TextPainter line metrics remain SDK behavior. No substitute layout or geometry
filter is introduced. Selection, pointer hits, labels, retained focus and F2 remain
available as text geometry changes.

The complete slice covers schema/capability, compound generation/provenance and
const propagation, codec/payload, Properties/Slots, palette/tree/Canvas placement,
required-child protection, save/reopen/further editing, Undo/Redo, rejected-change
rollback, accessibility and four distinct light/dark 16/32px SVGs. Existing value
shapes suffice: FD schema 13, API model 14, Canvas model 18 and NBFC1 stay unchanged.

At that milestone: 64 widgets, 58 const definitions, 779 writable rows (762 outside
Scaffold); 61 scalar plus structural IntrinsicHeight/RepaintBoundary/MergeSemantics.
The 55 any-widget plus two trait destinations remain 57 insertable slots:
64x57 = 3,648 cells, 3,363 accepted and 285 rejected. Categories: Layout 31,
Scrolling 3, Basic 20, Material 4, Accessibility 6. Historical practical target:
64/92 with 28 remaining. This is an API-reviewed successor; the historical ordered
92-widget inventory has not been recovered. Full physical desktop acceptance stays
deferred until the palette target is complete.

Focused validation: all 25 new DefaultTextHeightBehavior Flutter cases and the
complete 777-test Canvas suite pass; analyze and formatting are clean. Coverage
includes all omission combinations, eight rendered configurations against TextPainter
height/baselines, local/default-style/inherited precedence, EditableText and guarded
TextField inheritance, inherited-theme capture/wrap and change notification, retained
focus/semantics/pointer interaction, F2 and required-child wrapping/move protection
on Windows/Web model profiles. Core tests pass all 1,226 cases, including 18 new
contract/command/payload tests and the Text optional-composite non-regression.

The focused NetBeans suite passes 682 cases, including 11 new tests, 84 mutation
lifecycle cases and the shared 124-Boolean editor contract. Both checkboxes retain
unset/false/true without replacing the property sheet, and enum/reset preserves
the exact domain. The lifecycle suite covers each leaf, reset of all leaves while
retaining the required composite, nested required-child replacement, save/reopen,
further descendant edits, Undo/Redo and rejected-change rollback.

Final validation in the canonical G: checkout: `mvn clean install` with the pinned
Flutter/Dart SDK and real Web artifact passes all 11 reactor modules. Surefire
records 3,563 tests (zero failures/errors, six allowed optional skips); Failsafe
records 13 tests (zero failures/errors, one allowed physical Windows Canvas skip).
The real Dart candidate analyzer suite passes 17 tests, including the new complete
DefaultTextHeightBehavior default/eight-value/inheritance/invalid-input fixture
and three accepted SDK symbol probes. `mvn nbm:cluster` and `tools/verify-release.ps1`
pass. No installed-userdir or physical desktop acceptance is claimed.

The rebuilt Web entry is 2,870,332 bytes with SHA-256
`1cc72b5072a91dc4b379ec155f00bbba89be431bea8cc600616abf821fc3578c`;
both source/Web manifests and the artifact contract match. The verified
`netbeans-plugin-0.1.3-SNAPSHOT.nbm` is 7,325,029 bytes with SHA-256
`B6DC1A0D887E49E464FE84054DABAC8171C682472A1DCD0F75C9BEEC837FA381`.

## ADR-093 — DefaultSelectionStyle includes direct and merging inheritance

Accepted, 2026-09-05. The canonical checkout remains
G:\MyProjects\java\project\netbeans-flutter-plugin-starter; this slice follows
25a1c5e on branch 0.1.3. Review the pinned Flutter 3.44.8 framework implementation
and the official DefaultSelectionStyle constructor, merge and fallback API pages.

Add flutter.widgets.DefaultSelectionStyle as one STATIC_EDITABLE Basic/order 210
palette definition. The complete insertable constructor has optional cursorColor,
selectionColor and mouseCursor at orders 0/1/2, required single any-widget child
at order 3, and the existing managed key. Both colors use literal ARGB or reviewed
Material ColorScheme roles. The three SDK fields have no explicit creation defaults.

Also expose required Boolean merge at order 4 as a Designer-only construction mode,
with explicit false creation value. False selects the normal const-capable
constructor; true selects the actual static
DefaultSelectionStyle.merge helper. This is not a named const constructor and must
disable const propagation to ancestors. The mode never appears as a Dart argument.
The factory has separate candidate-bound symbol evidence. Direct mode replaces
all three inherited fields, including nulls. Merge inherits each unset field
independently and overrides only explicit values. To clear an inherited field to
null, choose direct mode; merge's omitted/null argument intentionally inherits.

The cursor is a closed StringValue admitting all 36 SystemMouseCursors presets,
MouseCursor.defer/uncontrolled and WidgetStateMouseCursor.clickable,
adaptiveClickable/textable: 41 predefined values in total. Emit each with its real
SDK owner and member provenance; do not turn the preset name into a quoted Dart
string. WidgetStateMouseCursor can be used as an ordinary MouseCursor and resolves
the empty state in createSession. adaptiveClickable uses kIsWeb, not the simulated
Canvas device profile. Deprecated aliases need no duplicate value; arbitrary
cursor subclasses and resolver callbacks are outside this closed preset contract.
The existing TextField 36-preset domain stays unchanged.

The separate const DefaultSelectionStyle.fallback constructor is deliberately not
insertable: it holds an invalid _NullWidget child and the SDK throws when it is
mounted. It supplies a fallback value from of(), not an application subtree. This
is an SDK restriction, not a deferred supported palette branch.

Render the actual constructor/helper and use real SDK consumer precedence and
inherited-theme notifications/capture/wrap. Explicit Text selectionColor and
TextField cursorColor override the corresponding inherited defaults, subject to
the SDK's error/theme fallback rules. TextField's mouse cursor is a separate local
setting; DefaultSelectionStyle.mouseCursor applies to selectable Text. The wrapper
does not itself make Text selectable or override Designer's guarded input policy.
ThemeData/TextSelectionTheme wrappers retain their actual framework behavior.
Selection color can remain null in an unfocused EditableText; do not fabricate
selection or focus merely to display a style in the Canvas preview.

The four editable rows use stable shared Properties infrastructure: two color
editors/previews, a closed cursor chooser and centered required Merge checkbox.
The three nullable SDK fields retain unset/reset. Merge is an explicit true/false
construction choice and cannot be removed/reset; rejected reset leaves the model,
focus and history untouched. Reset of an SDK field removes only that selected
field; it does not replace the property sheet or discard the required child.
Reuse generic atomic tree root/non-root and Canvas non-root wrapping, required
child replacement and protection against clearing/removing/moving it away.
Intact wrappers use ordinary placement rules; direct Row/Column ParentData keeps
Expanded/Flexible/Spacer outside the eligible wrapped-child set. Complete the
codec/payload, Save/reopen/further editing, Undo/Redo, rollback, selection/F2 and
four light/dark 16/32px SVG paths without a new property kind or format version.

At this milestone: 65 widgets, 59 const-capable definitions, 783 writable rows
(766 outside Scaffold); 62 scalar plus structural IntrinsicHeight/RepaintBoundary/
MergeSemantics. Eight generic wrappers do not add an ordinary insertion destination.
55 any-widget plus two trait destinations remain 57 slots: 65x57 = 3,705 cells,
3,418 accepted and 287 rejected. Categories: Layout 31, Scrolling 3, Basic 21,
Material 4, Accessibility 6. Historical practical target: 65/92, 27 remaining.
This is an API-reviewed successor; the historical ordered 92-widget inventory
has not been recovered. FD schema 13, contributor API 14, Canvas model 18 and
NBFC1 stay unchanged. Full physical desktop acceptance remains deferred until
the palette target is complete.

Focused real-SDK validation passes the new candidate test: both generation modes
with all 41 cursor presets, literal/theme colors, nested reset/merge, selection
and TextField composition. Eleven accepted class/factory/member probes confirm
the widgets.dart re-exports resolve inside the pinned SDK. Thirteen malformed
forms are rejected, including missing/wrong child, wrong color/cursor types,
invented cursor, leaked merge arguments and illegal const static-helper calls.
Each candidate leaves the original disk file and analysis options unchanged.

The exhaustive color matrix uncovered a shared generator defect: repeated use of
one theme token in different properties of a single widget reused the same symbol
occurrence ID. Qualify the existing theme-token occurrence with its exact model
path, retaining deterministic identity and unchanged emitted Dart text. This
allows cursorColor=primary and selectionColor=primary on one DefaultSelectionStyle
and fixes the same pre-existing Text case. A dedicated Text regression protects
the shared path; existing strict validation and package trust are not weakened.

The complete core Maven suite passes 1,245 tests with no failures/errors/skips.
Nineteen new tests comprise eight contract tests, nine command/history tests and
two payload tests, covering 756 generation/codec combinations and 168 payload
combinations across both required construction modes and exhaustive typed values.
Both owners and members of all 41 cursor constants, static merge
evidence, repeated theme roles, const propagation and exact counts are verified.
The final required-mode run passes all 1,245 tests again: missing merge is invalid,
reset is rejected in both false/true modes, and failed reset or atomic patches
leave the exact pair, revision and retained Undo/Redo path unchanged.

The complete Flutter suite passes 805 tests (777 baseline plus 28 new), with clean
analyze/format and no SDK edits. Coverage includes all mode/color/cursor decoding
combinations, actual direct-null versus merge inheritance, semantic theme colors,
TextField local/error/theme precedence, selection highlight and mouse hover on
selectable Text, all three WidgetState cursor sessions, dynamic inherited updates,
InheritedTheme capture/wrap, F2/focus/semantics, required-child wrapping/moves and
the explicit invalid-child fallback assertion. Windows/Web model profiles use the
real framework semantics; adaptiveClickable retains compile-time kIsWeb behavior.
The rebuilt Web entry is 2,871,643 bytes with SHA-256
`13b7b74d4b463d673a9a803dabf8d902c289b19afe95a99eb28dd4feec5cafee`.
All 75 source/Web manifest entries were rehashed; both manifests and the packaged
artifact test match the final runner and Web release build.

During development, a nullable prototype for the Designer-only mode exposed a
separate retained-history limitation: a same-Dart/different-model endpoint can
become FD_ONLY after durable re-anchoring, while native retained pair history has
no complete FD_ONLY endpoint/replay/save representation. Do not claim that this
shared limitation is fixed, weaken the PAIRED/BASELINE proof contracts, synthesize
Dart edits, or discard semantic history. The admitted widget instead models its
construction choice explicitly as required false/true, like other required
Designer creation fields. This does not restrict any nullable Flutter argument
or either supported construction branch. The experimental coordinator change was
removed; broader retained FD_ONLY history support requires its own complete work.

The final focused NetBeans suite passes 693 tests with no failures/errors/skips,
including all 41 cursor choices, typed color previews, required Merge checkbox,
stable cell identity, required-reset rejection, optional SDK resets, wrapping/
replacement/movement protection and the save/reopen/further-edit lifecycle.
The shared Boolean editor contract now covers 125 fields. The lifecycle preserves
the exact model, Dart/.fd bytes and Undo/Redo path when required reset is rejected;
successful mode/color/cursor edits, child replacement and rejected analyzer changes
retain the existing transaction behavior. PairSaveCoordinator is unchanged.

Final clean release validation in the canonical G: checkout passes all 11 Maven
reactor modules with the pinned Flutter/Dart SDK and real Web artifact. Surefire:
3,594 tests, zero failures/errors, six allowed optional skips. Failsafe: 13 tests,
zero failures/errors, one allowed physical Windows Canvas skip. The complete real
Dart candidate analyzer suite passes 18 tests, and all 85 mutation lifecycle tests
pass. `mvn nbm:cluster` and `tools/verify-release.ps1` both pass. The verified
`netbeans-plugin-0.1.3-SNAPSHOT.nbm` is 7,334,831 bytes with SHA-256
`CCD2B55488F715B0B5477CEA70BA61481E742D30D65124C57211735353E7097B`.
No installed-userdir verification or full physical desktop acceptance is claimed.

## ADR-094 — IconTheme and complete static IconThemeData inheritance slice

Admit `flutter.widgets.IconTheme` in Basic at item order 220, after
DefaultSelectionStyle, against pinned Flutter 3.44.8. Cover the ordinary widget
constructor and static `.merge` helper, required `data` and required single child.
Publish all nine optional IconThemeData leaves: size, fill, weight, grade,
opticalSize, color, opacity, shadows and applyTextScaling at orders 0–8. Child is
order 9; Designer-only required Boolean merge is order 10 with creation value
false. It selects ordinary construction or `.merge` and is never a Dart argument.
There is no third metadata-only mode. Every SDK leaf still supports unset/reset.

Always emit required IconThemeData, including `const IconThemeData()` with all
leaves unset. Ordinary literal construction remains const-capable; semantic theme
references and static merge disable const propagation as appropriate. Preserve
exact data-constructor, static-helper and nested shadow/theme symbol provenance.
Repeated use of one theme role in different model paths retains distinct
occurrence IDs through the existing shared generator fix from ADR-093.

Reuse existing typed editors and values. Size is finite non-negative integer or
double; fill is [0,1]; weight/opticalSize are (0,32768); grade is [-32768,32768).
The font-axis limits also protect actual Icon rendering, beyond the narrower
IconThemeData constructor assertions. Opacity accepts any finite double, preserving
the raw input through model/codec/history; the SDK clamps the effective getter to
[0,1]. Colors and ordered shadows retain literal or reviewed semantic ColorScheme
sources; explicit empty shadows are distinct from unset. Both Boolean rows use
the shared centered checkbox; only optional applyTextScaling allows reset.

Use the actual SDK theme, not host-computed inheritance. Direct data replaces the
nearest outer data; consumer IconTheme.of resolves it and fills missing fields
from fallback. Static merge combines the nearest raw theme field by field before
consumer resolution. Local Icon arguments retain their precedence, while theme
opacity multiplies even explicit Icon color alpha. It does not fade the entire
subtree or shadow list. Null local Icon arguments continue allowing inheritance;
false scaling and empty shadows explicitly override their inherited values.

IconThemeData.fallback is fully representable by explicit size 24, fill 0,
weight 400, grade 0, opticalSize 48, black color, opacity 1 and applyTextScaling
false, with shadows unset. copyWith/merge/lerp are value operations, not additional
widget constructors; their static base-data results fit the same leaves. This
closed editor does not serialize arbitrary IconThemeData subclasses, custom
resolve methods or Cupertino dynamic-color expressions. Existing ambient themes
continue resolving through the framework rather than being flattened by the host.

Reuse generic atomic root/non-root tree and non-root Canvas wrapping, required
child replacement and clear/remove/move protection. Preserve stable Properties,
typed previews, selection/F2, save/reopen/further editing, Undo/Redo and rollback.
No FD_ONLY retained-history expansion or new property/format shape is introduced.
FD schema 13, contributor API 14, Canvas model 18 and NBFC1 remain unchanged.

At this milestone: 66 widgets, 60 const-capable definitions, 793 writable rows
(776 outside Scaffold), 63 scalar plus three structural definitions. Nine generic
wrappers leave 55 any-widget and two trait destinations: 66x57 = 3,762 cells,
3,473 accepted and 289 rejected. Categories: Layout 31, Scrolling 3, Basic 22,
Material 4, Accessibility 6. Historical practical target: 66/92, 26 remaining.
The ordered historical 92-widget inventory remains unrecovered; this is an
API-reviewed successor, not a claim of recovered ordering. Full physical desktop
acceptance remains deferred until the palette is complete.

The complete Flutter suite passes 836 tests (805 baseline plus 31 new), with clean
analyze/format. The new suite covers 1,024 mode/leaf-omission decoding combinations,
all nine actual SDK fields in direct/merge mode, fallback equivalence, nearest raw
inheritance, partial/reset/false/empty-shadow overrides, local Icon precedence,
raw/effective opacity and independent shadow alpha, scaling, inherited updates and
theme capture/wrap. Required-child Windows/Web DnD, semantics, selection, F2 and
state/focus remain covered. Existing Icon and AppBar behavior is unchanged.

The rebuilt Web entry is 2,872,753 bytes with SHA-256
`2fb2c97cab89c28ac0ba0900a3e2fed850d11fbbce100bc66f49307948e7e8e1`.
All 75 source/Web manifest entries were checked and rehashed; both manifests and
the packaged artifact contract test match the final runner and Web release build.

The complete core Maven suite passes 1,265 tests with no failures/errors/skips.
Twenty new tests comprise nine contract, nine command/history and two payload
tests. Both generation/codec and payload matrices cover 1,536 optional-presence/
Boolean combinations. Coverage includes required empty data, all nine leaves,
fallback-equivalent explicit values, numeric bounds and retained raw opacity,
distinct same-token data/shadow/Text provenance, const/static-helper behavior,
local Icon fields, both-mode save/reopen/reset, whole-patch rollback, required-child
protection and direct-parent ParentData rules. No shared persistence contract was
weakened or changed for this slice.

The focused NetBeans suite passes 705 tests, including 12 new tests, with no
failures/errors/skips. The shared Boolean contract covers 127 fields. Coverage
includes all ten property routes, required two-state Merge, stable cell identity,
typed color previews, shadow transactions/stable IDs, optional reset, required
reset rejection, wrapping/replacement/movement protection and all four SVG paths.
The lifecycle sets every field, saves/reopens, edits every SDK leaf again, retains
raw opacity and explicit empty shadows, resets all optional fields and continues
child editing/replacement with Undo/Redo and rollback. PairSaveCoordinator remains
unchanged; property/slot/palette help explicitly preserves the theme-opacity caveat.

The focused pinned-SDK candidate analyzer test passes: 40 direct/merge data variants
plus complete, empty/null, fallback-equivalent, nested, local-override and themed
compositions. Eight accepted class/factory/constructor probes resolve through
widgets.dart inside the pinned SDK. Eighteen malformed candidates are rejected,
including missing/wrong data or child, invalid field types/assertions, invented
data fields, leaked Designer merge flags and illegal const static-helper calls.
Every candidate leaves the original Dart file and analysis options unchanged.

Final clean release validation passes all 11 Maven reactor modules in the
canonical G: checkout, with the pinned SDK and real Web artifact. Surefire:
3,627 tests, zero failures/errors, six allowed optional skips, 335 reports.
Failsafe: 13 tests, zero failures/errors, one allowed physical Canvas skip.
All 19 real Dart candidate analyzer tests and 86 mutation lifecycle tests pass.
`mvn nbm:cluster` and `tools/verify-release.ps1` both pass. The verified
`netbeans-plugin-0.1.3-SNAPSHOT.nbm` is 7,343,711 bytes with SHA-256
`FE490F16F5564C488C75560F6A351B187E7B52C38A9BA1CFFD2CDAA25201B53C`.
Installed-userdir verification and full physical desktop acceptance are not claimed.

## ADR-095 — ImageIcon with nullable typed image providers

Admit `flutter.widgets.ImageIcon` in Basic at order 230 after IconTheme. The pinned
Flutter 3.44.8 constructor has four modeled fields: required nullable positional
image at ordinal 0, optional named size/color/semanticLabel at ordinals 0/1/2 and
no child slots. key remains framework-owned like other catalog widgets. The live
class documentation has begun showing useOriginalColors, but it is absent from
the pinned constructor/source and is deliberately not accepted. No SDK upgrade
is part of this change.

Reuse existing NullValue and ImageProviderValue. Required image has an explicit
NullValue creation default; missing/reset-to-omission is invalid, while None is a
real empty SDK icon. Optional size is finite non-negative integer/double; color
accepts literal ARGB or reviewed ColorScheme roles; semanticLabel is a string.
The existing typed provider domain covers AssetImage, ExactAssetImage, package
asset identity and optional ResizeImage dimensions, exact/fit policy and upscaling.
It remains asset-backed: arbitrary NetworkImage, FileImage, MemoryImage expressions
and custom ImageProvider subclasses are not admitted. The existing reserved
unresolved provider remains safely interpretable, but is not confused with null.

Add a UI-only nullable provider editor with explicit None and reuse the provider
controls. Null/provider transitions, invalid edits and dialog cancellation retain
the shared transaction/focus/history semantics. Creation chooses the first sorted
declared image asset, otherwise null, through the shared creation-values owner for
Canvas/tree drops and new slot replacements. Keep Image's existing unresolved
creation path unchanged. Required image never resets to absent; optional fields
retain unset/reset. Property previews remain those actually supported by the shared
editor infrastructure; bitmap image rendering is provided by Canvas.

The generic asset projector visits every typed ImageProviderValue and skips null;
do not broaden Image-only centerSlice checks. Include ImageIcon providers in Canvas
relationship/resource closure and runtime image-use validation with centerSlice=null
and scale=1, preserving exact resolved-scale and platform resize validation. Bind
validated content-addressed resources using the existing provider builder. Resolved
and unavailable non-null providers retain the current safe placeholder/diagnostic
behavior; actual ImageIcon(null) neither loads bytes nor reports an asset failure.

Render the actual SDK ImageIcon, not a hand-written Image replacement. It inherits
IconTheme size/color/opacity, local size/color override their defaults, and inherited
opacity still multiplies local color alpha. Contrary to old color-field prose,
IconTheme.of supplies black fallback in the pinned implementation. Font axes,
shadows and applyTextScaling are not consumed by ImageIcon. Its internal Image uses
BoxFit.scaleDown and excludes duplicate semantics; the outer semantic label remains
authoritative. Null reserves the same inherited/local icon size. Designer zero-size
hit targets remain available without inventing visible Flutter layout or pixels.
ImageIcon has no errorBuilder argument: prevalidated unavailable resources use the
existing diagnostics, while truly late SDK stream failures remain framework errors.

Preserve ordinary leaf insertion/movement, selection, accessibility, property edits,
asset dependencies, save/reopen/further editing, Undo/Redo and rollback. ImageIcon
does not add a generic wrapper or insertion destination. It does not enable F2 text
editing or add its own opacity/scaling/font/shadow properties. Existing Image, Icon,
IconTheme and DecorationImage behavior remains intact. No new persisted value kind,
FD_ONLY history capability or format version is introduced: FD schema 13, contributor
API 14, Canvas model 18 and NBFC1 remain unchanged.

At that milestone: 67 widgets, 61 const-capable definitions, 797 writable rows
(780 outside Scaffold), 64 scalar plus three structural definitions. Nine generic
wrappers leave 55 any-widget and two trait destinations: 67x57 = 3,819 cells,
3,528 accepted and 291 rejected. Categories: Layout 31, Scrolling 3, Basic 23,
Material 4, Accessibility 6. Historical practical target: 67/92, 25 remaining.
The ordered historical 92-widget inventory remains unrecovered. This API-reviewed
successor does not imply a recovered fixed order or unreviewed provider capability.
Full physical desktop acceptance remains deferred until the palette is complete.

The complete core Maven suite passes 1,281 tests, zero failures/errors/skips.
Sixteen new tests comprise seven contract, six command/history and three payload
tests. The 432 generation/codec combinations and 52 resolved-resource payload
variants cover nullable/asset/exact/package/resize providers, positional const and
symbol provenance, numeric/color constraints, save/reopen/further edits, reset
rejection, movement and transactional rollback. Null consumes no resource; the
reserved unresolved provider requires its exact unavailable issue, and changing
to null rejects stale image issues. Exact catalog/capability/count/placement tests
pass without widening Image's non-nullable image contract or persistence authority.

The full Flutter suite passes 865 tests (836 baseline plus 29 new), with clean
analyze/format. Twenty-three decoder/view/SDK and six runtime tests cover all four
fields, actual decoded RawImage and its removal on explicit null, SDK tint/size/
opacity/semantics behavior, zero-size hit selection, asset/exact/package/resize
bindings, shared resources, null/unavailable operation without image-byte capability,
exact-scale mismatch, missing/extra descriptors and corrupt decode. Runtime
native/Web profiles verify their distinct zero-axis resize derivation rules.
The 3,819-cell placement matrix passes; no SDK code or platform provider is changed.

The focused NetBeans suite passes 738 tests, zero failures/errors/skips, including
14 new tests and the existing Image creation/projector regressions. It covers all
four properties, declared/package/exact/resize controls, stored missing and imported
unresolved providers, None transitions, invalid draft and one-lease dialog commit/
cancel behavior, save/reopen/further editing, six-step Undo/Redo and analyzer/range/
required-reset rollback. Property cell identity is preserved. Null projects zero
resources; Asset/Exact providers share validated content-addressed resources. All
creation, slot, tree, Canvas and move paths, four SVGs and exact palette counts pass.
The shared Boolean property contract remains 127; None is an editor control, not
a new Boolean catalog property. No persistence or dependency scanner is changed.

The rebuilt Web entry is 2,874,266 bytes with SHA-256
`d8b16a8ec05be33e675bc99ef3d8ae13665823a268a2cba71996ea33ea1e7eaa`.
All 75 source/Web manifest entries were checked and rehashed; the manifests and
packaged artifact contract test match the final runner and Web release build.

The focused pinned-SDK candidate analyzer test passes 18 provider/field variants
plus complete, explicit null, inherited/merged theme, local-override and themed
compositions. Eleven class/factory/enum-member probes resolve through widgets.dart
inside the pinned SDK. Sixteen malformed forms are rejected, including absent or
named image, wrong property/provider types, invalid ResizeImage construction and
invented child/scaling/shadow/fit/merge/useOriginalColors arguments. The project has
no uses-material-design flag; original Dart/pubspec bytes and analysis options
remain unchanged after all candidates.

The final canonical G: clean reactor install passes all 11 modules (9:26):
Surefire records 3,658 tests across 338 reports, zero failures/errors and six
documented optional skips; Failsafe records 13 tests across seven reports, zero
failures/errors and one optional skip. The complete candidate analyzer class has
20 passing real-SDK tests, and the mutation-controller lifecycle class has 87
passing tests. `nbm:cluster` and `tools/verify-release.ps1` both pass. The final
NBM is 7,354,255 bytes with SHA-256
`1DA5A946ADD820CE95F03C9E11EE3AB0913D58BB07E0FB8108C8813DD12DD76C`.
Package metadata, license, source freshness and the embedded module were verified.
No installed userdir or physical desktop acceptance was claimed.

## ADR-096 — Divider completes the horizontal Material separator

Admit flutter.material.Divider into Material at category order 100/item 50 after
TextField. Follow the pinned Flutter 3.44.8 default constructor and official API:
https://api.flutter.dev/flutter/material/Divider/Divider.html. Its six optional
named arguments are height, thickness, indent, endIndent, color and radius, in that
order. Key stays Designer identity rather than a writable constructor field.
The leaf has no slots or traits, is const-capable and is created with no explicit
fields. At that milestone VerticalDivider was a separate, not-yet-admitted widget; static
createBorderSide is a utility, not an additional palette constructor.

The first four properties accept the existing finite non-negative INTEGER/DOUBLE
domain. They are independent: do not infer thickness<=height or bound indents by
an unavailable parent width. Color accepts literal ARGB or reviewed ColorScheme
roles. Radius reuses the closed physical/directional BorderRadiusGeometry model,
including eight independent elliptical corner coordinates; arbitrary custom/mixed
runtime geometry expressions are not added. Every field can be reset to omission.
The radius editor uses neutral omit-argument wording so omission does not pretend to
store BorderRadius.zero. No new Boolean catalog property or persistence authority.

Render actual SDK Divider, with local value -> DividerTheme -> Material-default
precedence. Both versions default to space 16 and zero indents. Material 2 uses
thickness 0 and dividerColor, while Material 3 uses thickness 1 and outlineVariant.
Leading/trailing indents and directional radii resolve through actual Directionality.
Do not add Image resources, synthetic children, a semantic label, or F2 text editing.
Zero-height Designer hit targets must not change actual Flutter layout or paint.

The pinned SDK's bottom-only Border exposes a paint-stage limit: nonzero radius
plus effective hairline thickness 0 asserts in debug Border.paint; release falls
through to paintBorder, ignoring radius. An omitted thickness can resolve to zero
under Material 2 or DividerTheme, while Material 3 defaults to one. Preserve source
acceptance and real rendering; do not silently force 1 or reject theme-dependent
omission at model decode. Property help recommends positive thickness for rounded
lines; SDK/Canvas tests characterize the limitation. Existing framework diagnostic
forwarding remains unchanged, with no synthetic release-only geometry validation.

Cover six-field editors, ordinary Palette/tree/Canvas/slot insertion and movement,
accessibility, generation/const and exact symbol provenance, codecs/payload,
save/reopen/further editing, Undo/Redo, optional reset and rejection rollback.
Reuse existing values/generator and pair persistence; FD 13, contributor API 14,
Canvas model 18 and NBFC1 remain unchanged. CJK IME and platform-provider work are
not part of this slice; full physical acceptance remains deferred.

At that milestone: 68 widgets, 62 const-capable definitions, 803 writable rows
(786 outside Scaffold), 65 scalar plus three structural definitions. Nine generic
wrappers and 55 any-widget plus two trait destinations remain unchanged.
68x57 = 3,876 candidates: 3,583 accepted and 293 rejected. Categories are Layout 31,
Scrolling 3, Basic 23, Material 5, Accessibility 6. Historical practical target 68/92,
24 remaining; the ordered historical 92-widget inventory is still unrecovered.

The core Maven suite passes 1,295 tests with zero failures/errors/skips. Fourteen
new tests comprise six constructor/contract, six command/history and two payload
tests. Generation/codec and payload matrices each exercise 1,215 combinations,
covering all optional fields, semantic/literal color and physical/directional
elliptical radii. Exact capabilities/counts, per-field reset, save/reopen/further
edits, movement, Undo/Redo and transactional rollback pass without a generator or
format change. The SDK paint limitation is not turned into a model dependency.

The complete Flutter suite passes 892 tests (865 baseline plus 27 new Divider
tests), with clean analyze/format. It includes 729 decoder combinations, actual
M2/M3 defaults, local and DividerTheme overrides, dynamic semantic colors, RTL
indents/radii, zero-height hit targets, semantics, leaf drop and F2 rejection.
Raw SDK and both Canvas profiles explicitly observe the expected rounded-hairline
paint errors; successful construction is not confused with successful painting.

The focused real-SDK candidate analyzer passes 18 numeric/radius variants plus
default, complete, explicit-null, directional, inherited/local/theme and M2/M3
compositions. Eleven class/constructor probes are accepted in the pinned SDK;
16 malformed forms are rejected. The original Dart/pubspec bytes and absence of
analysis-options overrides are preserved. The legal rounded-hairline constructor
is accepted by static analysis, with its paint behavior tested separately.

The final Web release build succeeds. Its entry is 2,876,138 bytes with SHA-256
`fb1ef3a2ad1f8e87e427d3594e446e7a15921e3232af1868cf6a3ac8645cffb6`.
All 75 source/Web manifest entries were rehashed and checked; source manifests and
the packaged Web contract test now match the final frozen runner and release build.

The focused NetBeans rerun passes all 747 tests, zero failures/errors/skips,
including nine new tests and all existing Image/ImageIcon regressions. The first
run observed an intermediate copied Canvas capability resource; rerunning after
the final source/manifests were frozen proves exact Java/Canvas parity. All six
fields, neutral radius omission wording, stable cell identity, save/reopen/further
editing/reset, eight-step Undo/Redo and negative/raw-radius/analyzer rollback pass.
The complete mutation-controller lifecycle class has 88 tests. Palette, tree,
slots, moves, accessibility and four SVGs are verified. No persistence authority
or Boolean property contract is changed.

Final canonical G: clean install passes all 11 reactor modules in 8:40. Surefire
records 3,682 tests across 340 reports, zero failures/errors and six documented
optional skips. Failsafe records 13 tests across seven reports, zero failures/
errors and one optional skip. All 21 real-SDK analyzer tests and all 88 mutation-
controller lifecycle tests pass. `nbm:cluster` and `tools/verify-release.ps1` pass;
package metadata, license, source freshness and the embedded module are verified.
The final NBM is 7,361,357 bytes with SHA-256
`B626CB4C2314176BAE0E83C9F8D0F7591C4AF545AC980F52A7F13739FBA05469`.
No installed userdir verification or full physical desktop acceptance is claimed.

## ADR-097 — VerticalDivider completes the vertical Material separator

Admit flutter.material.VerticalDivider into Material at category order 100/item 60,
after Divider. The pinned Flutter 3.44.8 constructor and official API agree:
https://api.flutter.dev/flutter/material/VerticalDivider/VerticalDivider.html.
Its six optional named fields at ordinals 0..5 are width, thickness, indent,
endIndent, color and radius. Key remains Designer identity. The const-capable leaf
has no slots, traits or creation defaults; default source is const VerticalDivider().

Reuse finite non-negative integer/double geometry, literal/semantic ColorScheme
colors and existing physical/directional elliptical BorderRadiusGeometry. All eight
corner axes are editable within the closed radius domain. Every field can be reset
to omission; the radius editor retains neutral omit-argument wording. Custom/mixed
runtime geometry expressions are not added. Do not impose thickness<=width, bound
insets by unknown parent height, or create a Designer-only construction switch.

Unlike Divider, width is the horizontal extent and indent/endIndent are the top/
bottom gaps; RTL never reverses those gaps. Directional corner radii still resolve
through ambient Directionality. Actual SDK VerticalDivider uses a left-only Border,
local -> DividerTheme -> M2/M3 precedence, default space 16/zero insets, M2 hairline/
dividerColor and M3 thickness 1/outlineVariant. Parent layout supplies the height:
bounded Row, IntrinsicHeight Row with other children and bounded-height horizontal
ListView compositions are supported without a fabricated height argument or wrapper.

The SDK's rounded-hairline limit remains unchanged: effective thickness 0 plus
nonzero radius asserts during debug Border.paint and ignores radius in release.
Omitted thickness may resolve to a hairline under Material 2 or DividerTheme, so
neither source validation nor payload decoding invents a cross-field prohibition.
Property help recommends positive thickness for rounded lines and explains parent
height. Existing framework error forwarding is preserved; no release-only guard.

Cover ordinary palette/tree/Canvas/slot insertion, moves, zero-width selection,
accessibility and F2 rejection. All six fields participate in exact generation/
const/provenance, codec/payload, stable Properties, save/reopen/further editing,
Undo/Redo, optional reset and rejected-edit rollback. No asset resources, writable
semantic label, Boolean property or persistence authority is introduced. FD 13,
contributor API 14, Canvas model 18 and NBFC1 remain unchanged. Platform/IME work
and full physical desktop acceptance stay outside this palette slice.

At that milestone: 69 widgets, 63 const-capable definitions, 809 writable rows
(792 outside Scaffold), 66 scalar plus three structural definitions. Nine generic
wrappers and 55 any-widget plus two trait destinations remain unchanged.
69x57 = 3,933 candidates: 3,638 accepted and 295 rejected. Categories: Layout 31,
Scrolling 3, Basic 23, Material 6, Accessibility 6. Historical practical target
69/92, 23 remaining; the ordered historical 92-widget inventory is still unrecovered.

The complete core suite passes 1,310 tests, zero failures/errors/skips. Fifteen new
tests comprise seven contract, six command/history and two payload tests. The
generation/codec and payload matrices each cover 1,215 optional-field combinations,
including semantic/literal color and physical/directional elliptical radii. A
separate axis contract distinguishes vertical width/top/bottom from horizontal
height/leading/trailing. All-field reset, save/reopen/further editing, exact symbol
provenance, movement, Undo/Redo and rejection rollback pass without changing the
generator, value model, protocol or persistence authority.

The full Flutter suite passes 925 tests (892 baseline plus 33 new), with clean
analyze/format. It covers 729 decoder combinations, M2/M3/DividerTheme/local values,
RTL top/bottom invariance and directional radius reversal, real bounded Row heights
90/37/0, horizontally scrolling ListView, raw-SDK and Canvas unbounded Column
behavior, zero-width selection/drop/existing moves, semantics and F2 rejection.
With unbounded vertical space, the SDK line can have zero height; margins alone
can add extent. Tests characterize this rather than inventing a parent height.
Rounded-hairline paint errors are explicitly observed, not treated as good renders.

The focused NetBeans suite passes all 756 tests on the first run, zero failures/
errors/skips, including nine new tests. All six fields, inherited/reset radius,
stable property identity, save/reopen/further editing, Undo/Redo and rejected-edit
rollback are covered. Ordinary palette/tree/slot/move paths, exact Java/Canvas
capability parity, accessibility and four SVGs pass. No global editor changes or
new Boolean catalog property are introduced.

The rebuilt Web entry is 2,877,480 bytes with SHA-256
`ccaa55664b9f14eaf8b6d78d2fa8a8f7f22b878089da3fe78d9b6ccb257d0a64`.
All 75 source/Web manifest entries were rehashed and checked. Both manifests and
the packaged Web artifact contract test match the final frozen runner and release.

The focused real Flutter 3.44.8/Dart 3.12.2 SDK analyzer test passes (one test,
24.90 seconds). It covers all six properties, 18 numeric/radius variants, explicit
null/default and M2/M3/DividerTheme/local precedence, bounded Row, IntrinsicHeight
Row and horizontal ListView compositions. Eleven exact symbol probes are accepted
and 16 invalid API/type variants are rejected; project source/pubspec bytes stay
unchanged. Accepted rounded-hairline source is explicitly separate from paint
success, which is characterized by the Flutter tests above.

The final clean `mvn clean install` passes all 11 reactor modules in 9:01 with
the pinned Dart/Flutter SDK and real Web artifact gates enabled. Surefire records
3,707 tests across 342 reports (six documented optional skips); Failsafe records
13 tests across seven reports (one documented optional skip), with zero failures
or errors. This includes all 22 real-SDK analyzer cases and 89 mutation-controller
lifecycle tests. The 925-test Flutter suite, analyze and formatting checks passed
on the same frozen runner sources before the Web build. An independent read-only
review found no cross-layer or packaging discrepancies.

`mvn nbm:cluster` and `tools/verify-release.ps1` both pass. The final NBM is
7,368,080 bytes with SHA-256
`7F32BE8A33C8124131EDD007D3273E1766F5656593E6AE9707FBAB1DBBD32F4D`.
Metadata, licensing, complete package resources, source freshness and all report
coverage pass verification. No installed-userdir verification, interactive IDE
launch, physical desktop acceptance, CJK IME work or Linux/macOS provider work
was performed as part of this palette slice.

## ADR-098 — Card admits all variants and fully editable standard outlined shapes

Implement `flutter.material.Card` as Material/order 70 with 31 typed rows and one
optional any-widget child, using pinned Flutter 3.44.8 `material/card.dart` and
painting shape constructors. This is the next API-reviewed palette slice, not a
claim that the missing ordered historical 92-item inventory has been recovered.

The eight direct optional fields are color, shadowColor, surfaceTintColor,
elevation, borderOnForeground, margin, clipBehavior and semanticContainer. Reuse
literal/reviewed ColorScheme colors, finite non-negative elevation, non-negative
physical/directional insets, all four Clip values and centered optional Booleans.
Negative margin is a Padding/layout restriction, not a Card constructor assertion.
Creation stores only required Designer `variant=elevated`; this is never a Dart
argument. Elevated emits Card, filled Card.filled and outlined Card.outlined.
All three are const-capable; runtime references/semantic colors propagate non-const
as usual. The child can be created, replaced, moved or cleared independently.

The 22 shape rows comprise typed `shape` reference plus 21 built-in fields:
shapeKind, shapeRadius, four side leaves (Color/Width/Style/StrokeAlign),
shapeCircleEccentricity, six star leaves (Points/InnerRadiusRatio/PointRounding/
ValleyRounding/Rotation/Squash), and eight linear leaves (Start/End/Top/Bottom,
each Size/Alignment). Ten kinds map exactly to RoundedRectangleBorder,
BeveledRectangleBorder, ContinuousRectangleBorder, RoundedSuperellipseBorder,
CircleBorder, OvalBorder, StadiumBorder, LinearBorder, StarBorder and
StarBorder.polygon. Four rectangle kinds accept all eight physical/directional
elliptical radius axes. Circle/oval eccentricity is in [0,1], with omitted SDK
defaults 0/1 respectively. Any explicit side leaf builds BorderSide; absence of
all side fields preserves the shape constructor's BorderSide.none default.

Star points/polygon sides are finite doubles >=2, including fractional values;
rotation is finite clockwise degrees. Ratios, rounding and squash are in [0,1];
the star point/valley rounding sum must not exceed one. Polygon does not admit
inner radius or valley rounding. Linear edge size is [0,1]; alignment is any
finite number, with the conventional documented -1..1 interval explained rather
than imposed as a fabricated SDK assertion. Either edge leaf creates one edge,
using SDK defaults for its missing sibling; both omitted means no edge.

The model rejects mixed shape-reference/built-in branches, shape details without
a kind, and inapplicable detail fields. UI edits use one atomic PatchProperties:
references clear the built-in branch, built-ins clear the reference, kind changes
retain compatible values and remove incompatible ones, and a detail-first edit
selects an appropriate kind. Resetting kind removes its entire branch. Invalid
rounding is rejected, not clamped. Preserve unrelated fields, property identity,
exact Undo/Redo, revision fencing and all existing persistence authority.

CardTheme overrides every themeable local omission. M2 variants share cardColor,
Theme.shadowColor, elevation 1 and radius 4. M3 elevated uses surfaceContainerLow
and elevation 1; filled uses surfaceContainerHighest and 0; outlined uses surface
and 0 with outlineVariant side width 1. M3 radius is 12, shadow is ColorScheme.shadow
and default tint transparent. Explicit custom shape replaces the complete theme
shape; it does not silently inherit an outlined border side. Margin defaults to
four, clip to none, borderOnForeground and semanticContainer to true.

The isolated Canvas renders actual SDK Card variants and all ten built-in shape
branches, including directional geometry, border painting, clipping and semantics.
Other BoxBorder/InputBorder/compound/custom ShapeBorders are expressible through
the established typed project/package reference or zero-argument factory branch.
Their code cannot execute in this isolated runner: show explicit unavailability,
preserving child/tree, selection and property editing. Before path allocation,
star/polygon counts above 4096 use the same explicit unavailable presentation,
naming the requested count and budget. This is an operational Canvas limit only;
model and generated Dart retain every finite SDK-accepted count without a clamp.

Current totals: 70 widgets, 64 const-capable definitions, 840 writable rows
(823 outside Scaffold), 67 scalar plus three structural definitions. There are
129 Boolean catalog fields, nine generic required-child wrappers, 56 any-widget
and two trait-bound insertable destinations. 70x58 = 4,060 candidates: 3,760
accepted and 300 rejected. Categories: Layout 31, Scrolling 3, Basic 23,
Material 7, Accessibility 6. Historical target 70/92, 22 remaining. FD 13,
contributor API 14, Canvas model 18 and NBFC1 remain unchanged. Full physical
desktop acceptance and platform/IME work remain outside this palette slice.

The focused real-SDK test passes in 43.36 seconds after correcting its proof-scope
fixture. It checks all three constructors across ten complete shape forms (30
combinations), optional nulls, themes, all direct fields, large source-valid point
counts and 27 accepted symbol probes including current-library/factory/imported
typed references. Twenty-seven invalid API/type/const-assert variants are rejected;
project source/pubspec bytes remain unchanged.

Card pointer tests exposed a shared unavailable-preview overlay issue: the outer
Tooltip mouse region intercepted events before the underlying child. Move Tooltip
around the complete preview Stack and make the overlay entirely pointer-transparent.
Regression coverage includes existing ClipPath/PhysicalShape references as well as
Card custom/complexity states. Preserve the raw SDK nuance that RenderPhysicalShape
hit testing uses its shape even with Clip.none; clipping controls paint rather than
expanding the hit region. No synthetic hit-test behavior is introduced.

The full core suite passes 1,333 tests (1,310 baseline plus 23), zero failures,
errors or skips. Thirteen Card contract tests cover all three variants, all ten
shapes, finite/domain/cross-field validation, exact generation/codec/provenance,
capability drift and aliased Card/StarBorder factory offsets. Seven command tests
cover atomic shape transitions, optional child/history, save/reopen/further edits
and rejection rollback; three payload tests cover complete admission and strict
reference-presence projection, malformed branches and uncapped large counts.

Flutter passes all 972 tests (925 baseline plus 47 Card cases), including the
focused 47-test run, with clean analyze/format and all processes closed. Coverage
includes 31-field decoding/relations, three variants, ten shapes, raw-SDK/theme
precedence, geometry, clipping versus hit testing, border order, semantics, optional
child insertion/movement and exact 70x58 matrix. Ten pointer/F2/CtrlEnter regressions
cover existing ClipPath/PhysicalShape and Card unavailable states in both profiles.
Seven earlier warning-center selection expectations now correctly select the
underlying child rather than the tooltip-intercepting wrapper; existing hover,
accessibility, host-selected wrapper outlines and zero-size affordances still pass.

The Web release rebuild completes in 15.4 seconds with the pinned SDK/engine
defines and reviewed offline/no-icon-tree-shaking flags. All 75 source/Web manifest
entries were rehashed; three runner source entries and main.dart.js changed.
The final Web entry is 2,901,950 bytes with SHA-256
`ecba5da54fce8d8bf54c080135480126e11c1403607302d7e5ab0f41b8c15664`.
Both manifests and the packaged Web artifact contract test match these bytes.

The focused NetBeans suite passes all 770 tests (756 baseline plus 14), zero
failures/errors/skips, in 41.587 seconds after correcting two new-test fixture
expectations. It includes all 31 stable property rows, 100 shape-kind transitions,
all 21 built-in shape fields versus custom-reference transitions, neutral reference/
radius dialogs, required variant and 129 centered optional Boolean contracts.
Optional-child create/replace/clear/move, 4,060 placements, tree/a11y/four SVGs and
the live 31-step save/reopen/further-edit/history/rollback sequence all pass.
Core, UI and Flutter source/test work is now frozen for the final clean build.

Final clean `mvn clean install` with pinned Dart/Flutter and Web artifact inputs
passes in 09:57 minutes (2026-09-06 12:00:12 +03:00). Surefire records 3,745 tests
across 345 reports and Failsafe 13 tests across seven reports: zero failures or
errors, with six plus one documented optional environment-dependent skips. All
23 real-SDK candidate-analysis cases execute, none skipped (391.214 seconds).
The 90-case live mutation-controller suite also passes. Independent final
read-only cross-layer audit finds no actionable schema/UI/generation/Canvas drift.

`mvn nbm:cluster` succeeds in 1.584 seconds and `tools/verify-release.ps1` passes
test freshness, package metadata, license and artifact checks. The resulting
`netbeans-plugin/target/netbeans-plugin-0.1.3-SNAPSHOT.nbm` is 7,387,350 bytes;
SHA-256 `68BDDBA6588915EE404091249B72F303C994F71D16C50FAD5160688A27F41051`.
No installed userdir or interactive desktop acceptance is claimed; no unrelated
platform work, user Flutter project changes or IDE launch was performed.

## ADR-099 — Badge covers both constructors and the complete editable label style

Implement `flutter.material.Badge` at Material/order 80 in the canonical G: checkout.
Pinned Flutter 3.44.8 supplies const Badge and non-const Badge.count; one catalog
definition covers both without a synthetic mode selector or stored SDK defaults.
An empty prototype emits the equivalent of `const Badge()`; its empty optional
Label/Child slots may be emitted as null. Optional Count presence selects the
numeric constructor and disables const propagation through that node and parents.
Count is a nonnegative portable Dart integer; optional Max count is positive and
requires Count. The numeric label displays Count up to Max count, otherwise the
maximum plus a trailing plus sign; omitted Max count uses 999. Zero Count is an
explicit value, not omission. Candidate analysis cannot prove count assertions:
Badge.count is non-const and executes its assertions at runtime.

There are 41 typed scalar rows: backgroundColor, textColor, smallSize, largeSize,
padding, alignment, offset, isLabelVisible, count, maxCount and all 31 existing
TextStyle leaves under the textStyle prefix. Reuse literal/semantic colors, finite
numeric geometry, nonnegative physical/directional padding, full physical or
directional alignment, signed Offset and optional centered Boolean editors.
Small size is nonnegative because the dot uses Container width/height. Large size
accepts finite signed values: the SDK's intrinsic stadium uses max(minSize, child
intrinsic height) without a negative-value assertion. Do not invent a nonnegative
Large size restriction; Canvas must retain its actual label-positioning effect.
The complete style includes theme base, inherit, color/backgroundColor, font size/
weight/style/spacing/baseline/height/leading, locale subtags, foreground/background
Paint, shadows, features, variations, all decoration leaves, debug label, families,
package and overflow. TextStyle color-versus-Paint alternatives use existing strict
validation and atomic UI normalization. No raw-expression escape hatch is added.

Label and Child are separate optional single ANY_WIDGET slots, not required-child
wrappers. In ordinary mode an empty Label is a small dot, and any widget Label is
a large badge. Count mode owns its generated Text label and requires the stored
Label slot to be empty. Switching with a nonempty Label is rejected with a clear
move/clear-first instruction; user subtrees are never deleted implicitly. Max count
cannot be set before Count. Reset Count atomically resets Max count, preserving
all shared values and Child with one Undo. Slot editors, tree/palette planners,
existing-widget moves and Canvas insertion enforce the same current-node rule;
the generic catalog matrix remains the ordinary prototype's structural baseline.

Generate only Badge's named SDK parameters, composing the 31 leaves as textStyle;
omit an empty Label argument for Badge.count. Preserve exact Badge.count member,
TextStyle/locale/decoration/theme/paint symbol provenance and normal const behavior.
Keep document/cell identity, optimistic revision fencing and pair-save/history
authority unchanged. Save, reopen, further edits, reset, Undo/Redo and rejection
rollback must retain both slots and both constructor branches losslessly.

Canvas renders actual SDK Badge/Badge.count. BadgeTheme precedes built-in M3 badge
defaults even under Theme.useMaterial3 false: error/onError colors, labelSmall,
sizes 6/16, horizontal padding four and AlignmentDirectional.topEnd. A local
textStyle replaces the theme/default style; local textColor overrides style.color,
but an existing foreground Paint still wins according to TextStyle.copyWith.
The SDK adds Offset(0,8) to labelled offsets; small dots ignore Offset but still
use resolved Alignment, notwithstanding the API prose's label-only description.
With isLabelVisible false, Child remains mounted while Label/dot is hidden; the
stored Label remains editable in the tree but has no mounted Canvas geometry or
inline-text target. Badge itself is not an inline Text target. Both empty slots
need usable insertion affordances for tiny and hidden badges, without advertising
a Label destination in count mode. No arbitrary user Dart is executed by this slice.

Current totals: 71 widgets, 65 const-capable definitions, 881 writable rows
(864 outside Scaffold), 68 scalar plus three structural definitions, 134 Boolean
fields and nine required-child wrappers. There are 58 ANY_WIDGET and two trait
destinations: 71x60 = 4,260 cells, 3,952 accepted and 308 rejected. Categories are
Layout 31, Scrolling 3, Basic 23, Material 8 and Accessibility 6. Historical target
71/92 leaves 21; the full ordered 92-item inventory is not claimed recovered.
FD 13, contributor API 14, Canvas model 18 and NBFC1 remain unchanged. Physical
desktop acceptance and unrelated platform work remain outside this palette slice.

The focused real-SDK Badge test passes in 34.84 seconds: both constructors,
complete TextStyle, themes, nullable omissions, directional geometry and portable
integer boundaries resolve 24 accepted symbol probes. Twenty-three invalid API/
type/const-constructor variants are rejected. A separate accepted candidate proves
that runtime count assertions and negative size/padding layout behavior cannot be
inferred from analyzer success. Source and pubspec bytes remain unchanged. Two
initial probe-fixture failures resolved string labels instead of class symbols;
the test labels were corrected without changing production analysis behavior.

Dynamic-size tests expose a pinned SDK bug: Badge's private
_IntrinsicHorizontalStadium creates a render object with minSize but does not
implement updateRenderObject, leaving an already mounted large badge at its old
minimum after local/theme changes. A raw-SDK regression records this independently.
The isolated preview keys only the actual SDK Badge by its active effective
largeSize (local, then BadgeTheme, then 16). Existing stable model GlobalKeys
reparent Child/Label subtrees, preserving their Element/State/FocusNode identity,
runtime input and an active F2 draft. There is no SDK installation patch, synthetic
paint/geometry or model-value change. Normal and count branches are covered in
Windows/Web profiles, including local/theme transitions and finite negative sizes.

Core verification passes all 1,357 tests, zero failures/errors/skips: 1,333
baseline plus 24 new cases (14 Badge contract, seven command/history and three
payload tests). Coverage includes all 41 routes, both constructors, full style
alternatives, exact provenance, count/label dependency rejection, optional slots,
save/reopen/further edits, Undo/Redo and rollback. Initial local test compilation/
string-expectation fixtures were corrected before the successful full run.

Flutter verification passes 47 focused Badge tests and all 1,019 suite tests
(972 baseline plus 47), with clean analyze, format and scoped diff checks. Real-SDK
comparisons cover dots/counts/custom labels, theme and Paint precedence, geometry,
signed largeSize, hidden-label interaction, all 4,260 prototype placements,
conditional slot eligibility, movement/wrapping and count/label transitions.
Live local/theme size edits preserve TextField Element/State/FocusNode and runtime
text in both profiles; active F2 label draft/focus survives and commits afterward.
Source and tests are frozen and all Flutter CLI sessions are closed.

The pinned Web release rebuild succeeds in 17.0 seconds with the existing offline
and no-icon-tree-shaking flags. All 75 source/Web manifest entries were rehashed:
three runner source entries and main.dart.js changed. The final Web main is
2,908,801 bytes, SHA-256
`eab7c7a5f4470f7551cb3c5f46d436e1f68639f0e77fdf728509b0e0b509dec0`.
Both manifests and the packaged Web artifact contract test match these bytes.
Independent read-only documentation and cross-layer admission/state audits found
no actionable regressions; prototype-null and UI-label wording was clarified.

Live UI tests also characterize an existing shared staging limitation: adding a
second explicit false decoration flag keeps generated TextDecoration.none unchanged.
The model changes, but a dirty paired revision has no C1-to-C2 Dart transition;
the existing NO_CHANGES guard refuses replacement. Do not weaken pair authority,
implicitly save an unsaved pair, delete explicit flags or insert artificial Dart
changes to bypass it. Improve only the NO_CHANGES diagnostic to ask for Save/Undo
and retry. Other transition-status diagnostics and all persistence fences remain
unchanged. The dedicated live regression preserves exact C1 bytes/proof/history/
cells on refusal; the controller reissues its one-shot presentation token and
rejects the old token. Save and retry the same false value through the existing
FD_ONLY path without analysis or Dart rewriting. Cover all three flags, durable
reopen, true/false/unset and retained history separately from the all-field
Dart-changing sequence. After Undo/Redo, resetting a flag can also reach a
Designer-only variant of the durable baseline that the existing physical-history
endpoint refuses. The test records that refusal without data/history loss, saves
the current state, then successfully retries the identical reset. Neither guard
is relaxed. These shared limitations remain explicit in user-facing docs.

The focused NetBeans UI suite passes 787 tests (770 baseline plus 17), with zero
failures/errors/skips, in 44.032 seconds (2026-09-06 12:43:49 +03:00). It covers all
41 stable property cells, full TextStyle editors, 134 centered optional Boolean
fields, two slot editors, count/label conflicts and atomic Count/Max count reset,
4,260 placements, tree/move/a11y and four SVGs. The live all-field lifecycle passes
save/reopen/further edits, Undo/Redo and rejection rollback. The separate three-flag
regression verifies both save-first limitations and exact retry after Save. All
Java/UI, Flutter and artifact inputs are frozen for the final clean build.

Final clean `mvn clean install` with pinned Dart/Flutter and Web inputs passes in
10:42 minutes (2026-09-06 12:55:34 +03:00). Surefire records 3,787 tests across
348 reports; Failsafe records 13 across seven reports. There are zero failures or
errors, with six plus one documented optional environment-dependent skips. All
24 real-SDK candidate-analysis cases execute without skips (421.676 seconds),
and the 92-case live mutation-controller suite passes (37.75 seconds). The final
read-only audit confirms the coordinator diagnostic, regression and documented
limitations agree; the coordinator changes only four diagnostic lines. All 58
changed files retain their pre-build hashes before this evidence append.

`mvn nbm:cluster` succeeds in 1.562 seconds. `tools/verify-release.ps1` passes test
freshness, package metadata, license and artifact checks. The resulting
`netbeans-plugin/target/netbeans-plugin-0.1.3-SNAPSHOT.nbm` is 7,401,569 bytes;
SHA-256 `EE0C4E764A4724C27B45C20BFBA4C8B31D60CC7F53A7F13D3A3EC49F80F61AC3`.
No installed userdir or interactive desktop acceptance is claimed; no unrelated
platform work, user Flutter project changes or IDE launch was performed.

## ADR-100 — CircleAvatar completes both image layers and radius alternatives

Admit `flutter.material.CircleAvatar` at Material/order 90 in the canonical G:
checkout. The official constructor and pinned Flutter 3.44.8 implementation agree:
one const constructor, nine optional scalar arguments and one optional Child slot.
No constructor branch or scalar creation default is invented. New nodes retain
the equivalent of `const CircleAvatar()`, whose all-unset radii resolve to 20.
This is an API-reviewed successor to Badge, not a recovered historical 92-item order.

Expose backgroundColor, backgroundImage, foregroundImage, onBackgroundImageError,
onForegroundImageError, foregroundColor, radius, minRadius and maxRadius in exact
SDK parameter order after Child. Reuse literal/semantic colors, the closed declared
AssetImage/ExactAssetImage/ResizeImage algebra and strict Dart callback identifiers.
Each image-error callback requires its own provider. Resetting a provider clears
that callback in the same atomic UI patch; setting an orphan callback is rejected.
Both image references participate in the existing resource closure and deduplication.
No provider is required for creation. Network/file/custom provider families remain
outside the shared asset-backed editor; arbitrary callback code is not executed by
the isolated Canvas and its identifier is not transported there.

Fixed radius and min/max bounds are mutually exclusive. UI changes clear only the
conflicting radius fields, preserving other values, Child and a one-step Undo.
Every radius admits a nonnegative finite integer/double or the exact existing
`EnumValue("double", "infinity")`, bound to `dart:core`. This closed static constant
requires no new model/wire kind. Explicit infinity must not be silently mapped to
unset: maxRadius infinity alone yields minimum zero and unbounded maximum, unlike
the all-unset fixed default. Validate effective doubled double-precision bounds,
not arbitrary decimal ordering or a half-MAX_VALUE cap: two finite radii may both
overflow to infinite diameters. Infinite fixed/minimum constraints need a bounded
parent during layout. Invalid finite/NaN/negative bounds are not proven safe by
candidate analysis, because CircleAvatar's constructor leaves those constraints to
its AnimatedContainer build. Strict model and Canvas validators enforce the rule.

Render actual SDK CircleAvatar. Preserve Material 3 primaryContainer/onPrimaryContainer
and titleMedium inheritance, Material 2 contrast/primary-text-theme behavior,
child IconTheme and MediaQuery.withNoTextScaling. Circle decorations crop image
layers but do not ClipOval arbitrary Child. Foreground image paints above Child;
background image is behind it. An unavailable foreground must not become a
successfully painted checker image that hides valid fallback content. Omit an
unavailable layer with a property-specific diagnostic; safely report corrupt image
decode errors without invoking user code. Finite size/color changes keep normal
SDK animation. Finite/infinite constraint changes key only the SDK shell because
BoxConstraints interpolation cannot mix finite and infinite bounds; stable keyed
child state/focus is preserved. Empty and zero-size external selection/Child drop
affordances must remain usable after animated size changes.

The catalog now contains 72 widgets, 66 reviewed const definitions and 890 writable
rows (873 outside Scaffold): 69 scalar plus three structural definitions, 134 Boolean
fields and nine required-child wrappers. There are 59 any-widget and two trait-bound
destinations: 72x61 = 4,392 placements, 4,079 accepted and 313 rejected. Categories:
Layout 31, Scrolling 3, Basic 23, Material 9, Accessibility 6. Historical target 72/92
leaves 20; the missing ordered inventory is not claimed recovered. FD13, contributor
API14, Canvas model18 and NBFC1 remain unchanged. Physical acceptance stays deferred.

The real-SDK scope regression also proves that introducing only a prefixed
`dart:core` import removes the implicit unprefixed core scope, breaking otherwise
unchanged user String/Object references. Generation remains source-independent:
lower only this exact reviewed positive-infinity value to the fixed constant
expression `(1.0 / 0.0)`, which contains no shadowable user identifiers and adds no
core import. A const-constructor assertion proves its equality to the SDK's positive
infinity. The enum's closed identity is retained in model/UI/wire; this is not a
user-supplied expression escape hatch. No unrelated enum emission, user import
show/hide scope, source declarations or trust boundaries are changed.

The focused real-SDK overlay verifies all nine fields, both image layers and
callbacks, asset/exact/resize providers, null omissions, M2/M3, theme colors,
finite/infinite radii and finite overflow. Twelve exact symbol probes pass, including
`double` and `infinity` under the actual Flutter sky_engine core library. The initial
test expected standalone dart-sdk/lib; Flutter navigation actually resolves
bin/cache/pkg/sky_engine/lib/core, so the fixture was corrected without changing
production trust boundaries. Fifteen invalid constructor/type/callback variants are
rejected. A separate accepted candidate records that negative/NaN/inverted layout
constraints are not analyzer errors. Source/pubspec bytes remain unchanged.

Core verification passes all 1,377 tests with no failures, errors or skips,
including 20 new tests: ten contract, seven command/history and three payload
cases. The contract covers 125 radius combinations, 212 single-layer provider/
callback combinations, 52 dual-layer resource variants and five unchanged user
core-import forms through Infinity set/reset. Normal enum/import behavior is
unchanged. Initial test compilation required making the new validation helper
static. Final review narrowed the new core-import omission to CircleAvatar alone;
a contributor `double.maxFinite` regression proves unchanged generation/imports
both alone and beside CircleAvatar. The final core install passed in 12.537 seconds.

Flutter verification passes all 1,037 tests, including 18 new tests (16 focused
CircleAvatar and two protocol/runtime cases); analyze and formatting are clean.
Coverage includes M2/M3 light/dark, exact SDK pixel comparisons, unclipped Child,
both image layers/fallback/decoder errors, no text scaling, zero-size DnD after
animation and stable TextField/inline-edit state through finite/infinite changes.
An old Card test fixture's block delimiter was narrowed to avoid including the
new unrelated CircleAvatar builder; the successful full suite contains no skips.

The offline pinned Web release rebuild passed in 14.9 seconds. Source and Web
manifests were rehashed; main.dart.js is 2,914,108 bytes with SHA-256
`d3982fbe880f5cb9e99f464a0ea1183fece57241256190f726c81755e876315a`.
Independent read-only cross-layer and documentation audits found no functional
regressions; stale current-total headings were corrected without rewriting
historical milestones or changing resource budgets.

The focused NetBeans UI gate passes 701 tests with no failures/errors/skips,
including all nine live edits, save/reopen/further editing, exact Undo/Redo and
rejected-edit rollback, optional image dialogs, Infinity inline editing, typed
colors/callbacks, child slots/moves, accessible metadata, four SVGs and all 4,392
prototype placements. Shared Image/ImageIcon required-provider behavior remains
covered. Three host/runner parity tests and 23 Web artifact contract tests pass
against the freshly packaged runner and current Web artifact. The successful
scoped run finished at 14:43:03 +03:00; all 47 non-documentation changed inputs
were then frozen before the final full reactor install.

Final reactor install succeeds across all 11 modules in 11:45 minutes, finishing
at 2026-09-06 14:58:22 +03:00. Surefire records 3,822 tests, zero failures/errors
and six declared optional skips; Failsafe records 15 tests, zero failures/errors
and one optional native-desktop skip. All 358 XML reports are fresh from this
full run. The real-SDK analysis module passes 47 tests, including all 25 candidate
analyzer cases; the mutation-controller lifecycle suite passes all 93 cases.
The NetBeans test fork repeats the previously recorded 30-second shutdown
timeout after System.exit(0): its dump shows the Windows AWT ToolkitShutdown
hook in native WToolkit.shutdown. This is recorded as a test-process shutdown
warning, not a passed physical-desktop check or a test failure; Maven exits zero.

The staged whitespace gate then removed one redundant blank line at the new
schema file's EOF, with no executable-code change. All 1,377 core tests were
rerun successfully, followed by the four CircleAvatar property contract tests
and nine package metadata integration tests during a fresh plugin install.
The rest of the full-reactor evidence above remains from the complete run,
not from this scoped repackage.

Development nbm:cluster and release metadata/freshness verification both pass
again for the final package at 15:01:54 +03:00. The package is
netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
7,419,473 bytes, SHA-256
`E5716EC9D4DAF7E6A384E858CBCFDDD47AA02CC2887F9F739FFC71DA2F5BDEA1`.
Packaged and development-cluster module JAR hashes match; all four CircleAvatar
SVGs are present. SHA-256 comparison confirms that all 47 changed non-documentation
inputs remained identical throughout the final reactor run. Plugin basename,
developer contacts, donation link and the corrected description spacing remain
unchanged. No installed userdir was changed and no user IDE was launched; global
interactive physical acceptance remains deferred until palette completion.

## ADR-101 — Complete LinearProgressIndicator constructor and animation references

Accepted. Admit `flutter.material.LinearProgressIndicator` at Material/order 100
against pinned Flutter 3.44.8. Its single const constructor exposes 13 optional
scalar fields: value, backgroundColor, color, valueColor, minHeight, semanticsLabel,
semanticsValue, borderRadius, stopIndicatorColor, stopIndicatorRadius, trackGap,
year2023 and controller. There are no child slots, traits or stored creation
defaults. Omitted Value selects the SDK's indeterminate animation, not a synthetic
half-filled preview. Value and Controller are mutually exclusive; setting either
clears only the conflicting scalar in one undoable patch. Strict validation never
silently normalizes a loaded model.

Represent valueColor as a closed union using existing value kinds: literal Color
or reviewed theme color lowered to AlwaysStoppedAnimation<Color>, explicit Null
lowered to AlwaysStoppedAnimation<Color?>(null), or an existing project object
reference assignable to Animation<Color?>. A stopped null color is not a null
animation: it retains a non-null animation whose value falls through to the normal
color/theme defaults. Controller uses a project AnimationController reference.
Both object branches support current-library and declared-package values/members
or zero-argument calls with the existing explicit invocation-constness rules.
Ordinary literal/theme construction retains exact generated symbol provenance and
const/non-const propagation; no raw expression is admitted.

Extend the closed expected-type grammar only to a nullable single generic argument.
Animation<Color?> is admitted; a nullable outer Animation<Color?>?, nested/multiple
arguments, qualified source strings, comments and expressions are not. The generator,
probe boundary and analyzer witness agree. Collision-free proof import aliases
qualify both Animation and Color while retaining only the inner question mark.
Strict-casts proof with its negative control continues to reject dynamic, nullable
outer and wrong types even if the original source suppresses assignment diagnostics.

Value retains every representable signed finite number and is not clamped in the
model, editor or generated source; Flutter clamps its effective display to 0..1.
Min height accepts strictly positive finite numbers or positive Infinity. Stop
indicator radius and track gap accept signed finite numbers or positive Infinity.
The exact enum pair double/infinity is lowered to fixed `(1.0 / 0.0)` without a new
core import or a user-code scope change. Only the reviewed CircleAvatar and
LinearProgressIndicator contracts receive the metadata-import exception. Negative
stop radii are valid because the pinned painter draws a stop only above zero;
positive radii are capped to half the actual height. Negative gaps retain the SDK's
actual signed indeterminate geometry. Positive infinite gap hides the track while
leaving the active indicator. Arbitrary NaN or negative-infinity wire values are not
admitted by the existing closed numeric model.

Reuse literal/theme colors, physical/directional elliptical BorderRadiusGeometry,
centered optional Boolean editing, strings and the existing typed object-reference
editor. ValueColor has a transactional union editor with independent default,
stopped-color, stopped-null and project-animation modes. Cancel does not publish a
draft; Restore Default remains distinct from explicit null/false/zero. Stable
Properties, save/reopen/further edits, exact history and failed-edit rollback are
part of the same slice, together with Palette/tree/Canvas DnD and four SVG icons.

Render the actual SDK LinearProgressIndicator with M2/M3/ProgressIndicatorTheme
precedence, year2023/new-appearance behavior, RTL corners/painting, determinate and
indeterminate animation, TickerMode and accessibility roles. The isolated runner
does not receive project object identifiers or execute project animation/controller
code; those branches report preview unavailable while preserving the stored value
and generated source. Width must be bounded, and infinite minimum height needs a
bounded height. Do not invent a preview width or replace an unavailable custom
animation with a falsely exact constant.

Record pinned SDK context-dependent limitations rather than concealing them with
new defaults. With M2 and year2023=false, explicitly configured stop/gap settings
can apply despite the SDK documentation's M2 exclusion. A positive stop radius
without any effective stop color can reach the painter's null assertion; a theme
may supply that color, so this is a resolved preview diagnostic, not a fabricated
scalar dependency. Determinate semantics use progressBar and validate the override
as a progress number/percentage; indeterminate loadingSpinner can retain free text.
Model STRING values remain unchanged, and invalid resolved determinate semantics
receive a diagnostic rather than rewritten user text.

Current totals: 73 widgets, 67 reviewed const definitions, 903 writable rows
(886 outside Scaffold), 70 scalar plus three structural definitions, 135 Boolean
fields and nine required-child wrappers. The 59 any-widget and two trait-bound
destinations form 73x61 = 4,453 placements: 4,138 accepted and 315 rejected.
Categories are Layout 31, Scrolling 3, Basic 23, Material 10 and Accessibility 6. The
historical target is 73/92, leaving 19; the missing ordered inventory is not claimed
recovered. FD 13, Catalog API 14, Canvas model 18 and NBFC 1 remain unchanged.
Global physical acceptance stays deferred until the palette is complete.

Focused verification initially passes 16 dart-analysis tests, including all 13
constructor fields, 16 symbol probes, six accepted current/imported object proofs,
seven rejected dynamic/nullable/wrong object types and 16 invalid constructor/type
variants against the real SDK. The successful real-SDK case took 47.68 seconds.
Analysis alone accepts non-const value/controller conflicts and context-dependent
semantics/M2 painter failures; model and Canvas guards remain necessary. Original
source, imported dependency, pubspec and analysis-options files remain untouched.
The complete core install passes 1,395 tests with zero failures/errors/skips,
including 18 new contract/history/payload cases, at 15:20:29 +03:00.

The focused NetBeans UI run passes 687 tests with zero failures/errors/skips at
15:21:36 +03:00. It covers all 13 fields through live save/reopen/continued editing,
exact Undo/Redo and rejection rollback, four valueColor modes and cancel-safe nested
editors, the prior CircleAvatar lifecycle, shared property/Boolean editors and the
full placement matrix, SVGs, accessibility, slots and moves. Packaged runner/Web
parity and the final reactor gate are checked separately after the Canvas freeze.

The full Flutter suite passes 1,063 tests, including 26 new focused preview cases;
Flutter analyze is clean. The offline Web release build succeeds. The final
main.dart.js is 2,926,669 bytes, SHA-256
`0c41b523ca2a06f5eef9d2e9a0a94c8487b54a7a2d5b8fb1cf839f6a3a0664ae`.
The runner source and Web manifests, together with the Java artifact-contract
pins, describe these exact bytes. All 49 changed non-documentation inputs are
frozen before the full reactor reaches runner packaging and NetBeans tests.

The final 11-module Maven install succeeds at 15:37:28 +03:00 on 2026-09-06
in 12 minutes 9 seconds. All 26 real-SDK candidate-analyzer tests pass in
509.969 seconds. Fresh reports contain 3,856 Surefire tests (six allowed optional
skips) and 15 Failsafe tests (one allowed optional skip), with zero failures or
errors. All 362 XML reports were written during this full run; scoped reports
from earlier iterations are not being counted as fresh full-reactor evidence.
The NetBeans test fork repeats the known 30-second shutdown timeout after
System.exit(0); its dump again identifies ToolkitShutdown in native
WToolkit.shutdown. This is recorded as a test-process shutdown warning, not a
passed physical-desktop test. Maven exits zero.

Development nbm:cluster and release metadata/freshness verification both pass
at 15:37:58 +03:00. The final package is
netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
7,439,276 bytes, SHA-256
`F22891938155E6542D22670E08DB17496A1A5F9CED6F33FC3208648AA8D111B1`.
Packaged and development-cluster module JARs have identical SHA-256
`91F3CBD0C71F060550F8E3D13A185CF8DC081E78283F665D6F6E85085C8E0A73`;
all four LinearProgressIndicator SVGs are present. Final SHA-256 comparison
confirms that all 49 frozen non-documentation inputs remained unchanged.
Plugin basename, developer contacts, donation link and corrected description
spacing remain unchanged. No installed userdir was modified and no user IDE
was launched; global interactive physical acceptance remains deferred.

## ADR-102 — Complete material and adaptive CircularProgressIndicator slice

Accepted. Admit one `flutter.material.CircularProgressIndicator` definition at
Material/order 110 against Flutter 3.44.8. Its 15 Properties comprise all 14 optional
Material constructor fields plus required Designer-only variant material/adaptive.
Creation stores only variant=material; there are no child slots, traits or fabricated
progress/size defaults. Both constructors are const-capable. The selector chooses
the constructor symbol and is never emitted as a named Dart argument. Adaptive
exposes 13 SDK fields because it has no color parameter, even when color would be
null. Strict loaded-model validation rejects adaptive/color and value/controller
conflicts. Properties switches to adaptive by removing color, or to material when
setting color in adaptive mode; value/controller transitions remove only the other
field. Each is one atomic undoable patch, preserving unrelated values and node IDs.

Reuse the existing typed algebra: literal/theme colors, stopped Color or stopped
null via AlwaysStoppedAnimation, project Animation<Color?> and AnimationController
references, StrokeCap, normalized finite/infinite BoxConstraints, and nonnegative
physical/directional EdgeInsetsGeometry. All explicit values remain separate from
omission. The closed nullable-inner generic proof, strict-casts negative control,
collision-free import aliases and accepted symbol provenance already introduced
by ADR-101 cover both constructors. No raw expressions, project code execution or
new wire kinds are admitted. Project values/members and zero-argument calls retain
the existing invocation-constness contract.

Value, strokeWidth and strokeAlign retain signed finite numbers exactly. The SDK
clamps effective progress to 0..1, admits negative/zero stroke widths and explicitly
allows alignment outside -1..1. TrackGap also accepts positive Infinity through
the exact double/infinity enum and fixed `(1.0 / 0.0)` lowering. Nonpositive gaps,
zero/tiny paint sizes and infinite gaps are not arbitrarily rejected. Infinity
stroke width/alignment are not admitted: the actual SDK can produce NaN rectangle
coordinates. Very large finite values remain valid model/source values, while
nonfinite resolved geometry, padding sums or unbounded infinite minimum constraints
produce an isolated-Canvas diagnostic without rewriting those values or inventing
a size. Restore Default and the centered optional year2023 checkbox remain shared.

Render the actual SDK constructor with ProgressIndicatorTheme, M2/M3 and appearance
precedence, caps, directional padding, constraints, determinate/indeterminate modes,
TickerMode and Material progress semantics. Resolve adaptive from Theme.platform,
not a replacement painter or assumed host platform. On iOS/macOS the pinned SDK
creates CupertinoActivityIndicator, forwarding only backgroundColor/value/key;
backgroundColor becomes the tick color, finite Value is clamped before partial
reveal, and omission gives the actual Cupertino animation. It ignores Material
valueColor/stroke/constraints/padding/semantics/year2023/controller fields there,
including more fields than the public constructor comment lists. Retain those
fields in the model and generated adaptive call; do not execute project animation
or controller references and do not falsely block the Apple path for ignored
values. Material paths still explicitly report project-reference preview as
unavailable. This widget-level platform behavior does not implement native OS
Canvas providers or advance the deferred global physical acceptance work.

Stable grouped Properties, all-field editing, cancel-safe nested animation editors,
variant switches, save/reopen/continued editing, reset, exact Undo/Redo, rejected-edit
rollback, Palette/tree/Canvas DnD, existing-widget moves, accessibility and four SVGs
belong to the same slice. Current totals are 74 widgets, 68 const definitions,
918 writable rows (901 outside Scaffold), 71 scalar plus three structural
definitions and 136 Boolean fields. Nine required-child wrappers and 59 any-widget
plus two trait-bound destinations produce 74x61 = 4,514 placements: 4,197 accepted
and 317 rejected. Categories are Layout 31, Scrolling 3, Basic 23, Material 11 and
Accessibility 6. Historical target 74/92 leaves 18; no missing ordered inventory is
claimed recovered. FD 13, Catalog API 14, Canvas model 18 and NBFC 1 stay unchanged.

The focused real-SDK candidate test passes in 72.07 seconds at 15:58:31 +03:00 on
2026-09-06. It covers both constructors' complete fields, defaults/all-null values,
theme/platform branches, three stroke caps, both padding coordinate systems,
constraints and numeric boundaries. All 27 symbol probes are accepted, including
the adaptive constructor member and 12 current/imported animation/controller
static-type proofs. Fourteen dynamic/nullable-outer/wrong-type references are
rejected despite source diagnostic suppression; 19 invalid constructor/field types
are rejected, including adaptive color=null. Analysis alone accepts non-const
value/controller assertions and contextual geometry/semantics failures, confirming
the need for strict model and resolved Canvas guards. Original source, imported
dependency, pubspec and analysis-options files remain untouched.

The complete core install passes 1,417 tests with zero failures/errors/skips at
16:06:18 +03:00 in 9.877 seconds: 1,395 baseline plus 13 constructor-contract,
six command/history and three payload tests. It covers both const variants,
48 typed-reference combinations, all constructor fields and strict conflicts,
required variant reset rejection, signed numeric/Infinity domains, structured
constraints/padding/caps, import aliases and exact constructor/type provenance.
The shared StrokeCap metadata explicitly retains its widgets.dart import root;
positive Infinity does not introduce a new dart:core import. Payload tests prove
project identifiers and source expressions do not enter the isolated runner.
All current catalog counts and the full 4,514-placement matrix pass executable
assertions. Core production/tests are frozen for the final reactor.

The focused NetBeans UI suite passes 700 tests with zero failures/errors/skips at
16:08:15 +03:00 in 23.536 seconds. Coverage includes all 15 rows through live
save/reopen/further editing, both atomic dependency pairs, exact Undo/Redo and failed
analysis rollback, required Constructor lifecycle, all padding modes, expanding
constraints, cancel-safe nested drafts and shared LinearProgressIndicator/CircleAvatar
regressions. The constructor is displayed first in Progress without changing the
model's Dart argument ordering. Production/test sources are frozen except the final
Web artifact pins. A read-only cross-layer review found no functional mismatch;
two stale current non-flex source counts in documentation were corrected to 71.

The complete Flutter suite passes 1,086 tests, including 23 new focused Circular
cases, and analyze is clean. Raw SDK experiments separately verify signed widths,
unbounded finite alignment, positive infinite gaps and zero/tiny arcs before those
domains are admitted. The permanent suite covers both constructors, theme/platform
branches, ignored project references on Apple, real painting, RTL padding,
animation/TickerMode, semantics, constraints/overflow diagnostics and Palette DnD.

The offline Web release succeeds in 15.4 seconds. Its final main.dart.js is
2,940,725 bytes, SHA-256
`71bc79878ee9855664038b017e3f2618e345e7d37ad64101fd30d17be281e719`.
The runner source and Web manifests and Java artifact-contract pins identify these
exact final bytes. All 42 changed non-documentation inputs are frozen before the
full reactor reaches runner packaging and NetBeans compilation/tests.

All 27 real-SDK candidate-analyzer tests pass in 570.814 seconds during the final
reactor (report written at 16:18:51 +03:00). Independent filesystem verification
matches all 40 source-bundle and 35 Web-artifact entries to their declared sizes
and SHA-256 hashes with zero mismatches.

The final 11-module Maven install succeeds at 16:22:15 +03:00 on 2026-09-06
in 12 minutes 57 seconds. Fresh reports contain 3,892 Surefire tests (six allowed
optional skips) and 15 Failsafe tests (one allowed optional skip), with zero
failures/errors: 3,900 executed tests pass. All 365 XML reports were written
during this full run. The NetBeans fork repeats the known 30-second exit timeout
after System.exit(0), with ToolkitShutdown in native WToolkit.shutdown. This is a
test-process shutdown warning, not a passed physical-desktop acceptance check;
Maven exits zero.

Development nbm:cluster and release metadata/freshness verification pass at
16:22:35 +03:00. The final package is
netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
7,450,591 bytes, SHA-256
`3BF10C6DC464BFE1120620F94ABE5AC6E79503AFE3706F9BE41587D4E9CB36E7`.
Packaged and development-cluster module JARs share SHA-256
`AFB571F70D1C6EEF84B7DE97B5E9CDA3584F675BB8BAF3394498AE746F5927CE`,
and all four CircularProgressIndicator SVGs are present. All 42 frozen changed
non-documentation inputs remain byte-identical after the final build. Plugin
basename, developer contacts, donation link and corrected description spacing
remain unchanged. No installed userdir was modified or user IDE launched;
global interactive physical acceptance remains deferred until palette completion.

## ADR-103 — Complete RefreshProgressIndicator and nullable numeric inheritance

Accepted. Admit `flutter.material.RefreshProgressIndicator` at Material/order 120,
with all 12 optional fields of the pinned Flutter 3.44.8 constructor: value,
backgroundColor, color, valueColor, strokeWidth, strokeAlign, semanticsLabel,
semanticsValue, strokeCap, elevation, indicatorMargin and indicatorPadding.
There are no slots, traits or stored creation defaults; the constructor is const.
Inherited CircularProgressIndicator properties do not imply accepted constructor
arguments: controller, variant, constraints, padding, trackGap and year2023 remain
unavailable for this exact type. This visual leaf is not the separate gesture and
scroll wrapper RefreshIndicator.

### Contract and UI

Value and stroke geometry retain signed finite numbers, including zero and values
outside the displayed progress/alignment ranges. Elevation and both physical or
directional insets are finite and nonnegative. StrokeCap retains the reviewed
widgets.dart import root. ValueColor reuses stopped literal/theme color, stopped
null color and the strict non-null outer Animation<Color?> project-reference
union; no new arbitrary expression or executable project-code branch is introduced.

Only strokeWidth adds the exact INTEGER|DOUBLE|NULL numeric union. Omission uses
RefreshProgressIndicator.defaultStrokeWidth (2.5); explicit null inherits the
progress theme's strokeWidth or SDK fallback 4. The new narrowly selected numeric
editor presents omission, inherited null and explicit number as distinct modes,
with inactive text `Inherited (null)` and an exact inline null round-trip. Mode
drafts remain local until OK; the numeric mode rejects null, omission, expressions
and nonfinite values. Other numeric, color-animation and Boolean bindings retain
their existing behavior. Stable Properties, Restore Default, save/reopen/further
editing and Undo/Redo preserve omitted/null/zero states and exact typed values.

### Canvas and safe projection

The runner builds the actual SDK RefreshProgressIndicator, retaining its refresh
arrow/arc, value-to-indeterminate state, theme and default precedence, Material
disk/elevation, separate margin/padding and foreground-opacity/background split.
It uses the constructor's distinct omitted width, not a generic null fallback.
It does not execute project Animation<Color?> code: that preview limit is explicit
while the reference remains saved and generated Dart retains its analyzer proof.

Resolved guards apply to geometry, not an arbitrary scalar range. Nonfinite inset
sums or arc/arrow coordinates and a visible arrow's non-square inner paint area
receive concrete diagnostics. Transparent foregrounds preserve the SDK's paint
skip rather than diagnosing geometry the SDK does not draw. The model is not
rewritten, resized or clamped to hide a context-dependent SDK assertion. Both
determinate semantics and SDK clamping retain the shared reviewed behavior.

### Catalog and verification

The current catalog contains 75 widgets, 69 reviewed const definitions,
930 writable rows (913 outside Scaffold), 72 scalar plus three structural
definitions and 136 Boolean fields. Nine required-child wrappers and 59 any-widget
plus two trait-bound destinations produce 75x61 = 4,575 placements: 4,256 accepted
and 319 rejected. Categories are Layout 31, Scrolling 3, Basic 23, Material 12 and
Accessibility 6. The historical 92-widget target leaves 17; no missing fixed-order
inventory is claimed. Formats remain `.fd` 13, Catalog API 14, Canvas model 18 and
NBFC/control/wire 1. Plugin basename and contact/donation/description formatting
remain unchanged; installed IDE/userdir and user Flutter projects remain untouched.

The new real Flutter 3.44.8 analyzer test passes at 16:33:57 +03:00 on 2026-09-06
(47.92 seconds for the test, 50.036 seconds for its module install). All 15 symbol
probes are accepted, including defaultStrokeWidth and three current/imported
Animation<Color?> proofs. Four dynamic/nullable-outer/wrong-type references fail
strict proof despite diagnostic suppression; all 22 invalid constructor/type
cases are rejected, including inherited-but-unaccepted named arguments. Analysis
alone accepts downstream runtime geometry, semantics and Material assertions,
confirming that model and resolved Canvas checks are still necessary. Original
source, imported dependency, pubspec and analysis-options files remain untouched.

The complete core install passes 1,437 tests with zero failures/errors/skips at
16:35:41 +03:00 in 11.463 seconds: 1,417 baseline plus 11 constructor/domain/
provenance, six history/save/reset/rollback and three payload tests. All current
counts and the 4,575-cell matrix pass executable assertions. Core sources are
frozen for the final reactor.

The focused NetBeans UI test run passes 713 tests with zero failures/errors/skips
at 16:38:43 +03:00 in 24.902 seconds. All 12 live fields, omitted versus explicit
strokeWidth:null source, save/reopen/further edits, exact Undo/Redo and rejected
reset rollback are covered. Shared numeric/color-animation editors, all physical/
directional inset modes, accessibility, four SVGs and the complete DnD matrix pass.
The first focused run exposed a test fixture assumption: returning to the exact
durable state clears staged-save evidence. The fixture now verifies live Dart and
durable FD in that clean state; production behavior was already correct.

A separate read-only cross-layer review confirms schema/UI/codec/generation/Canvas
field and null-presence parity. It found one resolved-opacity edge case: the SDK
quantizes opacity to a byte before deciding whether to paint. Canvas therefore
uses that exact quantized-alpha test, avoiding false geometry diagnostics for a
nonzero theme alpha that rounds to zero. A regression covers that boundary.

The final Flutter suite passes all 1,109 tests, including 23 new Refresh cases;
analyze is clean. SDK characterization and permanent tests cover width omission/
null/theme precedence, actual arc/arrow painting and state transitions, inherited
theme controllers, foreground alpha, M2/M3 Material surface defaults, directional
insets, semantics, TickerMode, profiles, DnD and resolved geometry diagnostics.
The final offline Web release succeeds in 15.7 seconds. Its main.dart.js is
2,949,365 bytes, SHA-256
`67899301e63575e6901ec317152fd55cf50fbbc844c115612b318a02f320152d`.
The source manifest SHA-256 is
`ed15ca0578eb952e00a2b8d2eae375e0de23c46fd99c1680f9abe39a658fe9f2`;
the Web manifest SHA-256 is
`38f1e96817d460d29ae98b8e5ef2b6623ba38ce0f0adf7e94a17ae56b13cffb8`.
The two Java Web artifact pins now match the exact final release bytes. All changed
non-documentation inputs are frozen before the full reactor reaches runner
packaging and NetBeans compilation/tests.

All 28 real-SDK candidate-analyzer tests pass during the full reactor in
626.685 seconds (report written at 16:50:19 +03:00). Independent filesystem
verification matches all 40 source-bundle and 35 Web-artifact entries to their
declared sizes and SHA-256 hashes with zero mismatches. The frozen input snapshot
covers 41 changed non-documentation files.

The final 11-module Maven install succeeds at 16:53:45 +03:00 on 2026-09-06
in 13 minutes 55 seconds. Fresh reports contain 3,926 Surefire tests (six allowed
optional skips) and 15 Failsafe tests (one allowed optional skip), with zero
failures/errors: 3,934 executed tests pass. All 368 XML reports were written
during this full run. The NetBeans fork repeats the known 30-second exit timeout
after System.exit(0); its fresh dump shows ToolkitShutdown in native
WToolkit.shutdown. This remains a test-process shutdown warning, not a passed
physical-desktop acceptance check; Maven exits zero.

Development nbm:cluster and release metadata/freshness verification pass at
16:53:59 +03:00. The final package is
netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
7,465,639 bytes, SHA-256
`3671EB372CCFAB5FA94424E577DDD9D715F1BF015C92108AAB0FB1577704F083`.
Packaged and development-cluster module JARs share SHA-256
`8F6FA9F02469CCB4A61B0DFA03D7C74F3449091C2B3668AD7A735D4F74EC8E0D`,
and all four RefreshProgressIndicator SVGs are present. All 41 frozen changed
non-documentation inputs remain byte-identical after the final build. No installed
userdir was modified or user IDE launched; global interactive physical acceptance
remains deferred until palette completion.

## ADR-104 — Complete RefreshIndicator wrapper and typed refresh functions

Accepted. Admit `flutter.material.RefreshIndicator` at Material/order 130 with all
three pinned Flutter 3.44.8 constructors: material, adaptive and noSpinner. The
13 scalar rows cover all 12 SDK fields across those branches plus a required
Designer variant. Child is a required any-widget slot at constructor position 11;
the generic wrapper command preserves an existing child, while empty insertion or
clearing the required child fails closed. Creation stores only the material variant.
The definition is const-capable; the generated default callback makes the untouched
prototype non-const without storing synthetic handler code in the model.

### Constructor, function and numeric contracts

Material/adaptive admit displacement, edgeOffset, onRefresh, color, backgroundColor,
notificationPredicate, semanticsLabel, semanticsValue, strokeWidth, triggerMode and
elevation. NoSpinner excludes displacement, edgeOffset, color, backgroundColor and
strokeWidth, and uniquely admits onStatusChange. Model validation rejects conflicting
loaded states. Properties switch status/variant/spinner fields atomically with one
undo step, retaining the child and shared fields; required Constructor cannot reset.

OnRefresh is an optional typed project RefreshCallback reference. When absent,
generation supplies the required `onRefresh: () async {}` and the editor explicitly
describes that no-op; it does not pretend to load application data or omit the SDK's
required argument. OnStatusChange uses ValueChanged<RefreshIndicatorStatus?> with a
non-null outer function and nullable status parameter. NotificationPredicate admits
default/depthZero/all presets or a typed ScrollNotificationPredicate. Named typedefs
and the existing nullable-inner generic grammar suffice without a format change.
Current/imported function tear-offs and zero-argument callback factories use strict
assignability proofs. The generated candidate uses one common material.dart proof
import if any RefreshCallback or ValueChanged<RefreshIndicatorStatus?> requirement
is present; otherwise it retains widgets.dart. Material reexports Widgets, so mixed
Animation<Color?> and CustomClipper<RRect> proofs remain valid even when they precede
the Material-only callbacks. Each symbol's original navigation URI and trusted root
remain unchanged. Stale or mixed proof-library evidence is rejected by the exact
generated manifest rather than relaxing analyzer trust.

A narrowly matched STRING|DART_OBJECT_REFERENCE editor retains reviewed presets
and the existing closed project-reference editor, with local drafts until OK and
Cancel leaving the original value untouched. It does not admit arbitrary Dart
expressions. Displacement and elevation are nonnegative finite, because downstream
Padding and constructor assertions require those domains. EdgeOffset and strokeWidth
retain signed finite values, including zero. StrokeWidth is not nullable here and
defaults to 2.5, despite the SDK field comment's stale 2.0. No implicit child physics,
application callback, preview progress or fabricated height is stored.

### Canvas policy

The runner builds the actual SDK wrapper and retains the visible, editable child.
An unset handler and reviewed predicate use the same generated no-op for the SDK's
refresh cycle. Explicit project onRefresh or a custom notification predicate
disables preview refresh activation with a property-specific diagnostic, not a
silently substituted default filter or fake successful completion. The required
callback cannot report that unexecuted project work finished. Project onStatusChange
is reported as unexecuted while the SDK cycle remains enabled. Designer exposes no
programmatic show() capability and does not execute project or dependency code.

Adaptive follows Theme.platform; Apple targets use the genuine Cupertino spinner
and ignore the same Material-only appearance/semantics as the SDK. NoSpinner retains
notification/status/async lifecycle without painting a spinner. Resolved geometry
checks apply only to the active path and actual laid-out wrapper, without replacing
its child, rewriting stored values or blocking ignored branch-specific properties.
Short or empty lists need explicit overscroll-capable child physics. Save/reopen,
continued editing, reset, Undo/Redo, rollback, wrapping and slot replacement are
covered across all three constructors.

An intrinsic-transparent render observer mounts the real SDK child eagerly and
delegates intrinsic/dry-layout queries without building a second tree, inventing
dimensions or changing state. Its private BuildScope isolates ticker/scroll updates
from siblings and flushes dirty descendants even when constraints are unchanged.
Actual measured size is checked before paint, with a bounded second layout when an
unsafe active SDK cycle must be disposed; the globally keyed scrollable child keeps
its State. Regressions cover tight and loose parent resizing, child-only width edits,
visible bounded and hidden scroll branches under intrinsic parents, and retained
Theme font changes against raw SDK sizes in the same frame. The active overlay's
maximum 1.5 height factor is checked for finite overflow on Material and Apple paths;
noSpinner bypasses visual checks. No stored finite value is silently clamped.

### Counts and verification

At that milestone the catalog contained 76 widgets, 70 reviewed const definitions,
943 writable rows (926 outside Scaffold), 73 scalar plus three structural
definitions and 136 Boolean fields. There are ten generic wrappers; the required
RefreshIndicator child does not add an insertable destination. The existing
59 any-widget plus two trait-bound destinations give 76x61 = 4,636 placements,
4,315 accepted and 321 rejected. Categories are Layout 31, Scrolling 3, Basic 23,
Material 13 and Accessibility 6. The historical 92-widget target leaves 16; no
missing fixed-order inventory is claimed. Formats remain `.fd` 13, Catalog API 14,
Canvas model 18 and NBFC/control/wire 1. Package naming, contacts, donation link,
description spacing and installed userdir remain unchanged.

The final focused real Flutter 3.44.8 analyzer test passes at 17:27:55 +03:00 on
2026-09-06 in 86.58 seconds (module install 1 minute 28 seconds). All 40 symbol
probes pass, including both named constructors and 23 strict typed proofs: 21
current/imported/factory function cases across all applicable branches, plus mixed
Widgets animation and clipper references before the Material callbacks. Eleven dynamic, nullable-outer or
wrong-function-signature references fail strict proof despite diagnostic suppression.
Twenty-nine invalid constructor/type/required-argument cases are rejected. Source
analysis alone accepts downstream negative displacement, non-const elevation,
nonfinite geometry and active semantics assertions, so model and resolved Canvas
guards remain necessary. Original source, dependency, pubspec and analysis-options
files remain unchanged. Earlier SDK tests retain their original widgets.dart proof
helper through an overload; the new Material function types use material.dart.

The final core install passes all 1,456 tests at 17:21:45 +03:00, including 19 new
contract/history/payload cases. The final focused UI suite passes 761 tests with no
failures, errors or skips at 17:32:00 +03:00 in 31.331 seconds. This includes both
proof-library regressions, stable 943-row editors, all constructor transitions,
physical pair save/reopen, Undo/Redo and rollback, palette/tree/move/slot integration,
four SVGs and the full 4,636-candidate matrix. The 12 creation-mode wrappers include
Expanded/Flexible; the ordinary generic required-child subset contains ten.

The first complete reactor invocation on 2026-09-06 finished at 17:47:24 +03:00
after 14 minutes 18 seconds. All 29 real-SDK candidate tests passed in 707.504
seconds, and core passed 1,456 tests. Its 2,292 plugin cases reported no assertion
failures but two packaging errors: CanvasRunnerSourceBundleTest and the configured
WebCanvasBuildServiceTest correctly rejected canvas_model.dart against the previous
source manifest while the final Canvas review was still in progress. That invocation
is not a successful release gate; the complete downstream reactor must be repeated
from flutter-canvas-runner against the final frozen sources, manifests and Web pins.

The final Flutter suite passes 1,155 tests, including 46 new RefreshIndicator cases;
the exact final 46 cases were rerun after test-only formatting, and flutter analyze
is clean. Independent review found no remaining actionable observer issue. The
offline release Web build succeeds: main.dart.js is 2,965,966 bytes, SHA-256
ac9a947cb1a34d355c58b49be344ae4df8c49e85097a7df38527bd2e88e15897.
Both manifest verification passes independently match all 40 source-bundle files
and 35 Web artifacts by exact length and SHA-256. Source manifest SHA-256 is
11d3be0de2ba45ebcc5158d108975da7df39e23863b5123d8d4ba72c3e60c226;
Web manifest SHA-256 is
b32a2ad5d9fa1f263469d46f3b03186b3e53991fcd03812c7c99061a6b29837d.
Only the two changed runner source entries and main.dart.js Web entry changed.
All 47 non-document source/test/manifest/SVG inputs were frozen and SHA-256 captured
before the complete downstream reactor restart; Java's artifact contract pins the
same final main.dart.js length and hash.

The complete five-module downstream reactor from flutter-canvas-runner now passes
at 17:57:32 +03:00 in 3 minutes 24 seconds, including configured real Web rebuilding,
all plugin tests, package metadata and the assembled NetBeans runtime integration
gate. Combined with the unchanged successful upstream modules, all 371 report files
are fresh after this turn's 17:33:04 start: Surefire records 3,962 tests with six
allowed optional skips; Failsafe records 15 with one allowed optional skip. All
3,970 executed Maven cases pass with zero failures/errors. The same pre-existing
Surefire fork-shutdown warning recurred after System.exit(0); its fresh dump again
shows ToolkitShutdown inside WToolkit.shutdown, not a test failure or a claim of
completed physical desktop acceptance.

`mvn nbm:cluster` passes at 17:57:57, and `tools/verify-release.ps1` reports PASSED.
The final NBM is 7,489,909 bytes, SHA-256
E6355B4448AC764FD1DAD0AD05C2D4FEBC1FCE992BCAFEF79CBAF292D710E5D2.
Its embedded module JAR and both packaging/development cluster copies are identical:
3,365,477 bytes, SHA-256
C3B014F180BE3A76ABC8AF6AC4D0AF8FFE5EA0D55FAE155BDEBB1F0BB20BE539,
each containing all four RefreshIndicator SVGs. All 47 frozen non-document inputs
retain their captured bytes and SHA-256 after the gate. Work and artifacts remain
in the canonical G: checkout; no user IDE launch, installed userdir change, user
Flutter application edit, push, or deferred platform/physical acceptance was done.

## ADR-105 — TextButton standard/icon and complete state-style slice

Status: Accepted, 2026-09-06.

TextButton adds Material/order 140 with standard and icon constructors. Its 511
scalar rows comprise 12 direct controls, nine 54-leaf state style buckets, 12 common
style fields and a strict whole ButtonStyle reference. Child is required and stable;
the optional Icon slot is admitted only by the icon constructor. Standard is
const-capable when all emitted arguments are const; the SDK icon factory is not.
Required Enabled/Constructor controls select valid activation and construction.
Disabled activation references remain stored without generated occurrences or
evidence obligations. Long-press-only emits null onPressed; an enabled button with
neither activation reference gets an explicitly documented no-op.

All eight WidgetStates plus default are editable, with disabled/error/dragged/
pressed/selected/scrolledUnder/hovered/focused/default priority. Arbitrary combined
state constraints and custom styles remain supported through strict project
ButtonStyle references, not raw Dart text. Whole style and all 498 local leaves
are mutually exclusive, with atomic Properties transitions. ButtonLayerBuilder,
VoidCallback, ValueChanged<bool>, FocusNode and WidgetStatesController references
retain exact current/imported/factory type proofs. Standard semantic role and
constructor-dependent Clip behavior distinguish omission, explicit null and
concrete values. Nullable Boolean values keep centered checkbox rendering.

Switching to icon clears standard-only semantics; setting iconAlignment selects
icon. Returning to standard rejects an occupied Icon with a clear move/remove-first
diagnostic instead of deleting it. The actual SDK Canvas preserves local button
interaction and child state across constructor/icon/diagnostic changes, with unique
per-button retained keys. It never executes project code: callbacks/controllers/
focus/builders are explicitly isolated, and a project-defined whole style is an
explicitly approximate SDK-default preview. ElevatedButton's existing 286-row
contract remains unchanged; shared style assembly uses the correct button's theme
and constructor defaults.

Dense legal 491-row/464-row style families round-trip; not all 511 fields may
coexist because of constructor and style exclusivity. The default per-widget codec
limit rises to 512 and the shared candidate budget to 2048 symbol probes, preserving
every occurrence, explicit smaller caller limits and the existing 2 MiB/45-second
bounds. Structured-list proof IDs now include property paths so independent state
buckets do not collide. Every typed proof uses a collision-free dart:core alias for
its dynamic control; bool callback arguments use the same qualified core scope.
Original implicit/explicit imports, including adjacent and multiline URI literals,
remain unchanged. Formats remain .fd 13, Catalog API 14 and Canvas model 18.

The current catalog contains 77 widgets, 71 const-capable definitions and 1454
writable rows (1437 outside Scaffold): 74 scalar plus three structural definitions,
175 Boolean-only fields plus one nullable Boolean union, fourteen Material items,
eleven generic required-child wrappers and 62 insertable destinations (60 ANY plus
two trait-bound). The exact 77-by-62 placement matrix has 4774 cells, 4448 accepted
and 326 rejected. The historical practical target is 77/92, with 15 remaining;
the repository does not retain an authoritative fixed-order 92-item inventory.

Same-source paired replacements retain an explicit `NO_CHANGES` transition witness
and enter native semantic history without manufacturing a Dart edit or advancing
its document version. Historical revisions that differ only in metadata are
explicit `FD_ONLY` endpoints relative to the current durable anchor. Clean-source
metadata Save writes only `.fd`; combined metadata/Source Save preserves historical
native envelopes and redo chronology. Exact cursor, catalog, document, source/event
epoch, durable-anchor and capacity fences remain enforced. Rejected analysis and
verified no-write/rollback failures preserve retryable authority. Subsequent
metadata edits retain native history, while source-changing edits require fresh
analyzer evidence. A stale asynchronous Properties bind retries against retained
history instead of replacing an authoritative in-memory revision from disk.
A focused 78-test gate verifies these guarantees, including atomic publication,
clone-safe save leases and eight live lifecycle cases.

Pre-release focused evidence: all 1484 core tests pass. The main real-SDK constructor
test proves 62 exact occurrences, including 38 strict typed references, and rejects
16 invalid typed references plus 24 invalid constructor/type cases. A separate
scope test passes implicit, explicit, raw, escaped, hidden and conditional core
imports and rejects a shadowed bool callback without altering user import scope.
The final analyzer-focused install passes all 20 cases, including non-bool
ButtonStyle/VoidCallback/FocusNode proofs under restricted core imports and
dynamic rejection. The complete frozen dart-analysis suite subsequently passes
all 62 tests with zero failures, errors or skips, including all 32 candidate
real-SDK tests. Its 39 source/configuration input hashes remain unchanged and the
tested target and installed analyzer JARs are byte-identical. The final frozen Flutter suite passes
1201 tests (44 dedicated TextButton cases), with clean analysis. Both source and
Web manifests independently verify all 40 and 35 entries. The production generator
and probe-planner integration also passes dense 491-property standard and
464-property icon candidates against the real SDK, retaining every occurrence
and strict typed proof under the unchanged per-candidate 45-second timeout.
Final verification used frozen-input Maven passes: the complete analyzer suite,
the remaining reactor suites, and the final plugin/runtime package pass. All 1552
non-Markdown source/configuration hashes match the final verified snapshot. The
shared same-source contract also updates Badge's second explicit-false decoration
regression: all three flag combinations retain exact source bytes/version, fresh
analysis, staged metadata, native history, Save and reopen. Its separate
source-changing physical-endpoint rejection remains covered and unchanged.

The full plugin run passed all 107 live mutation scenarios. One unchanged folder
rename guard test intermittently exceeded its five-second post-release wait; its
in-admission exclusion assertion passed. The entire 38-case guard class passed
unchanged in isolation and again in the successful package install. No production
guard or timeout was changed. Final current reports cover 4032 Surefire cases
(six allowed optional skips) and 15 Failsafe cases (one allowed optional skip):
4040 executed, zero failures/errors, across 376 reports. All reports are newer than
the frozen verification window; `tools/verify-release.ps1` passes without bypassing
freshness checks. Maven install and `nbm:cluster` complete successfully.

The final `netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm` is 7,539,166 bytes, SHA-256
`825530997363b0cdc5a3ed5262402f9e03789c0f34b7d0191c9de3b5be416f5c`.
Its module JAR, NBM staging, module/root development clusters and assembled test
runtime are byte-identical: SHA-256
`8df85b32425c87921641cd8ddf193b36e49daf8d55613ecde7d33e2933c6c6d1`.
The ordinary Maven JAR has identical code/resources, including all four TextButton
SVGs; only the expected NBM Class-Path manifest transformation differs.
No installed userdir, user IDE, user Flutter application, push or deferred physical
desktop acceptance was involved.

## ADR-106 — OutlinedButton standard/icon and shared complete style slice

Status: Accepted, 2026-09-06.

OutlinedButton is the next API-reviewed palette addition at Material/order 150.
The standard and icon constructors use 510 typed fields: 11 direct controls,
nine 54-leaf state/default style buckets, 12 common fields and one strict project
ButtonStyle reference. The required stable Child becomes the label in icon mode;
Icon is optional and available only in that mode. Standard is const-capable;
the icon constructor is non-const. Unlike TextButton, neither constructor exposes
isSemanticButton, and both leave omitted clipBehavior null. Explicit null/enum
remain distinct model values and preserve the SDK layer-builder clipping rules.

The implementation narrowly extends the existing full-style assembly rather than
forking it. Generated inherited values resolve against OutlinedButtonTheme and
the actual constructor's defaults. The outline's default side overrides the
shape's own side; shape and outline are independently configurable. Icon alignment
resolves constructor, component theme, local style, then start, respecting RTL.
All nine buckets, custom whole project styles, layer builders, strict callbacks,
focus/controllers and activation semantics reuse the exact existing proof paths.
Inactive activation references retain metadata without emitting calls, imports or
symbol occurrences. Whole/local style and constructor transitions are atomic,
never delete an occupied icon, and preserve the required child's identity.

Real SDK Canvas renders both constructors with correct M2/M3 defaults and themes,
retains child state and local interactions across changes, and diagnoses rather
than executes project callbacks/builders/controllers. A project-defined whole style
is an explicitly approximate SDK-default preview. The property surface, icon-slot
admission, palette/tree/Canvas DnD, four SVGs, Save/reopen/further editing and native
history are part of the same slice. Dense legal families include 490-property
standard, 491-property rounded-icon and 464-property circle-icon configurations.
TextButton/ElevatedButton contracts, .fd 13, Catalog API 14, Canvas model 18 and
existing 512-property/2048-probe/2-MiB/45-second limits are unchanged.

Verified catalog totals are 78 widgets, 72 const definitions, 1964 writable rows
(1947 outside Scaffold), 75 scalar plus three structural definitions, 214
Boolean-only fields plus one nullable Boolean union, and fifteen Material items.
Twelve generic required-child wrappers plus Expanded/Flexible make fourteen
creation wrappers. There are 63 insertable destinations: 61 ANY and two trait-bound.
The 4914-cell matrix has exactly 4583 accepted and 331 rejected placements.
The historical 92-widget planning target leaves 14; no full ordered inventory is
claimed. Deferred platform work and global physical desktop acceptance stay out
of this slice.

Verification on 2026-09-06: the final remaining-reactor `mvn install` completed
successfully at 21:10:35 +03:00 in 7 minutes 27 seconds, with no test filters or
test-class exclusions. It excluded only the unchanged `dart-analysis` module:
all 39 analyzer/core-API input hashes and both target/installed analyzer JARs were
rechecked byte-identical to its successful 62-test gate at 19:24:03 +03:00
(JAR SHA-256 `20edfe6618f316d2ae607ede2d960c1661d219e38145609a605f864784bd94d3`).
Every other Maven report is fresh for this final build. The combined 379 reports
record 4080 Surefire and 15 Failsafe tests, zero failures/errors, and seven allowed
optional SDK/physical/platform skips: 4088 executed tests, including the retained
62 analyzer tests. The current 1510-test core suite, all three Java/Dart parity
checks, the 510-field live Save/reopen/history scenarios, and both real-SDK dense
TextButton/OutlinedButton methods (four generated candidates) passed.

The final Flutter suite passed all 1251 tests, including 48 dedicated
OutlinedButton cases and two additional runtime cases; `flutter analyze` was
clean. All 1562 non-document source/configuration inputs were frozen for the
full gate. The only subsequent input changes removed trailing blank lines from
four new SVGs, with their content otherwise verified identical. A fresh packaging
gate at 21:16:02 +03:00 passed all 46 icon-registry tests and reran all 15 package/
runtime integration tests (14 passed, one deferred physical test skipped).
Independent checks verified all 40 source-bundle and 35 Web manifest
entries; `main.dart.js` is 2,982,192 bytes with SHA-256
`9c5b623d1a31325b858db762c7828336e42f08bd45b8e6e6f4bb59307fd8b725`.

Both root and module-local `nbm:cluster` commands passed, followed by
`tools/verify-release.ps1` with freshness checks enabled. The NBM is 7,554,479
bytes with SHA-256
`19ca5f3154406e7d8ce6116860ea9879c8a273a064162699b8519fe485704d29`.
Its embedded module, packaging-stage module, both development clusters and
assembled runtime module are byte-identical (SHA-256
`5fcca3b3710ab0d9e849c29f60a941ea1f0a0d11d5fdc88ae7c80164d199cfa7`);
the ordinary Maven JAR differs only in the expected transformed manifest, and
all four OutlinedButton SVGs are present. No user IDE/userdir was launched or
modified, and no global physical desktop acceptance is claimed.

## ADR-107 — FilledButton four-constructor slice and conditional nullable child

Accepted 2026-09-06.

FilledButton is the next API-reviewed palette addition at Material/order 160.
Its standard, icon, tonal and tonalIcon constructors share 510 typed fields:
11 direct controls, nine 54-leaf state/default style buckets, 12 common style
fields and one strict project ButtonStyle reference. All local state fields and
both layer builders remain editable; whole styles use typed references rather
than arbitrary expressions. Whole/local-style transitions remain atomic, with
strict proof for callbacks/focus/controllers/builders and metadata-only history
for inactive activation references.

The pinned SDK permits a nullable child in standard and tonal, but requires a
non-null label in icon and tonalIcon. Support that distinction rather than impose
the older buttons' required-child wrapper restriction: Child is a single min-zero
slot and empty standard/tonal nodes emit the required child:null argument. The
icon modes retain the same Child identity as their required Label. The optional
Icon is available only in icon modes. Validation rejects missing labels, and
Properties rejects selecting an icon constructor before a Child exists. Clear,
remove, move-out and replacement-source guards apply consistently to the tree
and every slot editor. Icon-to-tonalIcon changes preserve both subtrees and
constructor alignment; returning to a non-icon variant rejects an occupied Icon
and clears only inapplicable constructor alignment. Existing TextButton and
OutlinedButton child/wrapper policies are unchanged.

FilledButton uses ordinary empty-allowed palette creation, not the required-child
wrapper path. Standard and tonal are const-capable; both icon constructors are
non-const. All four constructors default clipBehavior to Clip.none; explicit null
is distinct and retains the SDK's layer-builder automatic clipping. None exposes
isSemanticButton. Local generated styles and actual Canvas resolve against
FilledButtonTheme and the selected filled/tonal defaultStyleOf, never the outline
or text-button defaults. Icon alignment follows constructor, component theme,
local style and start, with RTL and the actual icon-presence-dependent padding.
All eight WidgetStates plus default retain their established priority and isolated
disabled fallback.

Canvas renders the real four SDK constructors and nullable non-icon child,
preserves local button/child interaction across modes, and never executes project
callbacks/builders or adopts project focus/controllers. Whole project styles are
explicitly an approximate SDK-default preview. The complete slice includes stable
510-field Properties, SVGs, palette/tree/Canvas DnD, Save/reopen/further editing,
rollback and native history. Dense legal standard/tonal families contain 490
properties, rounded icon families 491 and circle/paint icon families 464; mutually
exclusive style branches are not claimed to coexist. Formats .fd 13, Catalog API
14 and Canvas model 18, 512 properties, 2048 symbol probes, 2 MiB and the 45-second
candidate-analysis budget remain unchanged.

Verified core totals are 79 widgets, 73 const definitions, 2474 writable rows
(2457 outside Scaffold), 76 scalar plus three structural definitions, and 253
Boolean-only fields plus one nullable Boolean union. The 65 insertable slots
comprise 63 ANY and two trait-bound destinations. The 5135-cell matrix has exactly
4796 accepted and 339 rejected placements. Twelve generic required-child wrappers
and Expanded/Flexible still make fourteen creation wrappers. Palette categories
are Material 16, Layout 31, Scrolling 3, Basic 23 and Accessibility 6. The historical
92-widget target leaves 13; no complete fixed ordered inventory is claimed.
Deferred platform work and global physical desktop acceptance are out of scope.

Verification on 2026-09-06: 1539 core tests passed, together with all 1315
Flutter tests (56 dedicated Filled cases plus eight new runtime cases) and clean
Flutter analysis. The final full remaining-reactor run started at 21:37:52 +03:00
and ended at 21:45:55 with one error only: the unchanged
FlutterDesignerMoveDependencyGuardTest.exclusiveProofPreventsARelevantFolderFromBeingRenamed
timed out at line 483 waiting for the rename after the admission lock had already
been released. Its in-admission exclusion assertion passed. This repeats the
previous baseline timing/liveness issue, not a changed Filled path; its exact
runtime blocker was not proven. The entire 38-test class then passed unchanged
at 21:46:42 and passed again in the successful packaging/runtime install ending
at 21:47:52. No timeout, assertion, production lock or test was weakened, and
the intermittent baseline issue is not claimed fixed.

The final 382 XML reports record 4134 Surefire plus 15 Failsafe tests, zero
failures/errors and seven allowed optional SDK/physical/filesystem skips: 4142
executed tests. This includes the retained 62-test dart-analysis gate from
19:24:03: all 39 analyzer/core-API input hashes and both analyzer JARs were
reverified unchanged (SHA-256
`20edfe6618f316d2ae607ede2d960c1661d219e38145609a605f864784bd94d3`).
Every other report is fresh for the final full run or its unchanged retry/package
gate. All 114 live mutation/persistence scenarios, three Java/Dart parity tests
and four real-SDK test methods (ten generated TextButton/OutlinedButton/FilledButton
candidates, including both empty Filled children) passed. All 1572 non-document
inputs remained byte-identical from the full-gate snapshot through packaging.

Independent verification checked all 40 source-bundle and 35 Web manifest entries.
The final main.dart.js is 2,990,346 bytes, SHA-256
`500e7cda6d11a6c6013c22ba21356781d283c2f36abf4ab9c890f3b3980d7e09`.
Both root and module-local nbm:cluster commands and tools/verify-release.ps1 passed
with freshness checks enabled. The NBM is 7,568,502 bytes, SHA-256
`60aff78af5b04d27cbee2d0688d347cf5128525fa4a66fc91a9ca8ec0559f0c8`.
Its embedded module, packaging-stage module, both development clusters and
assembled runtime module are byte-identical, SHA-256
`1ef9c56d25a7e4858bc927e5b0f29a45a52758b876151b2cce17cd6192aaab6b`.
The ordinary Maven JAR differs only in the expected transformed manifest, and
all four FilledButton SVGs are present. No user IDE/userdir was launched or
modified; global physical desktop acceptance was not run.

## ADR-108 — FloatingActionButton four constructors and typed Hero tags

Date: 2026-09-06

Admit `flutter.material.FloatingActionButton` at Material/order 170 from the
pinned Flutter 3.44.8 SDK and its [official constructor contract](https://api.flutter.dev/flutter/material/FloatingActionButton-class.html).
All four const constructors are covered: standard, small, large and extended.
Normal/small/large have nullable Child. Extended maps Child to required Label,
admits optional Icon, and retains Label even when Extended state is false.
A new standard FAB can be inserted into an empty destination; it is not a
required-child creation wrapper. All tree moves, slot mutations, source
detachment and replacement operations enforce the conditional required Label.

The 78 typed rows project all 25 distinct non-widget SDK argument names:
27 direct/selector rows, with Shape replaced by its complete 22-row projection
and extendedTextStyle replaced by its 31-row projection. Constructor-only
arguments are admitted only in their corresponding variants: mini in standard,
isExtended in standard/extended, and extended spacing/padding/TextStyle only
in extended. Constructor changes remove incompatible scalar arguments in one
undoable patch. Empty-Label and occupied-Icon transitions are rejected with the
operation, widget and actionable reason; no child is silently deleted.
All five elevations accept nonnegative finite values and positive Infinity,
matching the SDK constructor. Signed finite or positive-infinite icon/label
spacing is retained; Canvas diagnoses invalid spacing only when actually mounted.
Insets retain the existing nonnegative physical/directional typed contract.
The real SDK can tween mixed finite/infinite elevation states through NaN.
Canvas reports that configuration as preview-unavailable without clamping source
values. Uniformly infinite state sets render with a scoped SDK-component key
change across finite/infinite history, preserving the editable model child State,
text and selection; all four constructor transitions have executable coverage.

The ten built-in ShapeBorder families include all existing Card shape details;
a strict ShapeBorder reference remains a mutually exclusive alternative.
The full TextStyle projection covers colors/paints, font metadata and fallback,
locale, leading, decoration, shadows, font features and variations. Alternative
paint/color and built-in/custom shape edits remain atomic. Booleans retain
centered checkboxes, and optional values keep unset/default semantics. Stable
Properties identity, native focus behavior, Save/reopen and subsequent editing,
Undo/Redo, four SVGs and capability-gated palette admission remain required.

Hero tag is one closed union: unset (the SDK's default tag), explicit null
(no Hero), string/integer/double/Boolean literal, or analyzed non-null Object
reference/factory. It has a dedicated cancel-safe typed editor, not a raw Dart
expression field. Integer literals use the existing portable exact range
[-9007199254740991, 9007199254740991]; double literals are signed finite values.
Other application identities remain available through typed Object references.
Object qualification uses the witness-owned
dart:core alias; all other expected types retain their existing library aliases.
The expected-type witness still excludes dynamic and nullable values and
preserves the user's implicit, explicit, hidden and prefixed core scope.
Activation, FocusNode, MouseCursor and ShapeBorder references use the same
strict static evidence. Disabled onPressed metadata remains stored but does not
generate calls, imports or symbol evidence until enabled again.

Canvas uses the actual SDK FAB constructors and component theme in both M2/M3,
including default sizes, RTL padding, collapsed extended mode, empty children
and child state. It never invokes application callbacks, adopts application
FocusNodes or evaluates custom tag equality. Default and literal tags remain
exact; known duplicate tags produce a diagnostic, not invented unique IDs.
Only an unresolved custom Hero wrapper is disabled while the real button and
child remain present. Unresolved custom shapes/cursors retain explicit isolated
preview diagnostics and SDK fallback, without altering generated application
source. Project-specific Hero transitions are not claimed to be executable in
the isolated preview.

The current target is 80/92, leaving 12 relative to the historical practical
target; the full ordered inventory is not preserved. The catalog has 80 widgets,
74 const-capable definitions, 2552 writable rows (2535 outside Scaffold),
77 scalar plus three structural definitions, and 262 Boolean-only fields.
The existing nullable-Boolean union remains, alongside the heterogeneous
Object-tag union. There are 67 insertable destinations (65 ANY plus two traits),
giving 5360 placements: 5013 accepted and 347 rejected. Twelve generic wrappers
plus Expanded/Flexible remain fourteen creation wrappers. Categories are
Material 17, Layout 31, Scrolling 3, Basic 23 and Accessibility 6.
Formats .fd 13 / Catalog API 14 / Canvas model 18, NBFC 1, 512 stored properties,
2048 symbol probes, 2 MiB and the 45-second candidate-analysis budget do not change.
Deferred platform work and global physical desktop acceptance are out of scope.

Verification on 2026-09-06: 1560 core tests pass. The full analyzer install
passes 63 tests (zero failures/errors/skips), finished at 22:13:42 +03:00.
Its 39 source/config input hashes remain unchanged through final packaging;
target and installed analyzer JARs are byte-identical, SHA-256
`6dce65a3ec1b43733c67290db090b65a390d55fe347d80c788ee91a3c7ad8c5f`.
This same-turn complete analyzer gate is retained for the final remaining-reactor
install, rather than rerunning its unchanged 14-minute protocol suite.

The final full remaining-reactor install starts at 22:31:55 +03:00 and finishes
with BUILD SUCCESS at 22:42:04 (10:07 Maven elapsed). Every non-analyzer report is
fresh for that run; no test filters are applied to the remaining modules.
All 386 reports contain 4196 registered cases: 4189 executed, zero failures,
zero errors and seven existing optional SDK/physical/platform skips.
Surefire has 4181 cases across 379 reports (six skips); Failsafe has 15 cases
across seven reports (one skip). The 117 mutation-controller integration cases,
three Java/Dart parity tests, and unchanged 38-case move dependency guard pass
inside this full run, without a retry. All 1585 non-Markdown source/config
inputs remain byte-identical from final source freeze through packaging.

The FAB real-SDK gate exercises eight generated/analyzed candidates: all four
dense constructors, a default/literal/null/empty-child matrix, concrete typed
Object/Shape/Cursor/Focus/callback references, and two deliberately rejected
dynamic/nullable Object cases. The other four typed witnesses remain accepted
in both negative controls. It uses the production generator, source-transition
and pair preparation, symbol probe planner and 45-second analyzer budget.
Source, pubspec and package_config bytes remain unchanged and every candidate's
FD data reopens exactly. The existing ten Text/Outlined/Filled generated SDK
candidates also pass. Targeted UI/editor regression passes 128 cases; the final
full run includes those methods and the rest of the plugin tests. An independent
review found JTextField would alter multiline string tags; STRING now uses
JTextArea, with exact LF/CRLF/CR/TAB unchanged-open/commit and cancel tests.

Flutter passes all 1367 tests twice, with the final exact source bytes and clean
static analysis. Raw SDK controls cover M2/M3, geometry/pixels, theme/RTL, empty
and collapsed variants, hero equality/numeric boundaries and elevation history.
All 2646 pre-existing reviewed records remain unchanged after CRLF normalization;
the new widget adds 81 records. The offline release Web build and all 40 source
plus 35 Web manifest entries are independently checked. Source manifest SHA-256:
`af591ec984c9eb7996e1e25b1b5d184a80b6af7cdf22648a6f3995d640483d6b`;
Web manifest SHA-256:
`f67bd202feabb6790c8baba71a772b5ee383dc2a08fd5602b39d77c0a593a886`.
main.dart.js is 3,017,112 bytes, SHA-256
`4680fc3299d6b6f3ccaa4ea54b558b0bc4023c11f91cc8849d164c95d01bfc66`.

Both root and module nbm:cluster targets and tools/verify-release.ps1 pass with
freshness checks enabled. NBM size: 7,595,874 bytes; SHA-256
`df8febb5f1fe8e36e6f0c4e55a69f995f2a121312d486c1a3a09052a7867022c`.
Its embedded module, packaging-stage module, both development clusters and
assembled runtime module are byte-identical (3,411,513 bytes), SHA-256
`a58bd1902331031ae7ac3813369eadfef8c366ee68afb2d7087a6a3e4c8e4f55`.
The ordinary Maven JAR differs only in the expected transformed manifest, and
all four FloatingActionButton SVGs are present. No user IDE/userdir was launched
or changed, and no global physical desktop acceptance was run.

## ADR-109 — IconButton required Icon and full state style

### Contract and scope

Admit `flutter.material.IconButton` at Material/order 180 in the canonical G:
checkout against pinned Flutter 3.44.8 and its
[official API](https://api.flutter.dev/flutter/material/IconButton-class.html).
All four const constructors are included: standard, filled, filledTonal and
outlined. The required `icon` and optional `selectedIcon` are real named Widget
slots, not scalar glyph selectors or a fictitious `child` argument. Palette
creation wraps one existing any-widget subtree in `icon` atomically, preserving
its stable IDs and properties. Optional Selected icon remains valid for every
constructor and every selection value. Constructor/selection edits cannot delete
either subtree. The required Icon can be replaced but not removed or moved out.

The full 524-row schema includes all direct SDK parameters, two required Designer
selectors (`variant`/`enabled`), two flattened direct density axes, a strict whole
ButtonStyle reference and all 498 existing local state/style leaves. The eight
WidgetState buckets plus default preserve the reviewed priority, disabled
isolation, complete TextStyle/ShapeBorder projections, paint/color alternatives,
lists, common layout and layer builders. Whole style and local leaves switch
atomically. `isSelected` uses the existing Boolean/null union: unset and explicit
null both mean a non-toggle button, while false/true are unselected/selected
toggles. Both null representations persist without discarding Selected icon.

Callbacks, FocusNode, WidgetStatesController, MouseCursor, ButtonStyle and layer
builders retain strict static witnesses for references or supported zero-argument
invocations. Dynamic, nullable outer reference types and wrong signatures remain
rejected. Enabled with no onPressed supplies an explicitly documented benign
onPressed, so long press can work; disabled emits `onPressed: null` and
`onLongPress: null`, omitting the stored project references from calls/imports/proofs
while preserving them in FD. This matches the
SDK rule that onLongPress is ignored without onPressed in both Material versions.

### Real SDK style and preview semantics

IconButton is not a public ButtonStyleButton subclass. The generator and Canvas
therefore do not use private SDK classes, element inspection or fabricated
TextButton defaults. Only the compound defaults required to complete sparse
local fields are projected with public API: 40-by-40 minimum, unbounded maximum,
null fixed/text style, StadiumBorder, standard density and the outlined state
side. Actual IconButton retains variant colors, overlays, IconTheme merging,
compact behavior and remaining defaults. Missing counterpart axes of local
size/density composites deliberately retain the corresponding direct constraint/
density before the IconButtonTheme/default fallback. This projection policy is
not a change to SDK property-level style precedence or a per-state scalar merge.

The isolated runner constructs the real four SDK variants. Material 2 ignores
style, selection, selectedIcon and statesController as the SDK does; those model
fields remain intact. Project Dart is never executed in preview. Unsupported
project-object/style/builder effects receive explicit diagnostics; constructor,
visible subtree and supported SDK-local state are preserved where possible.
Geometry guards are based on the mounted SDK branch, not blanket rejection of
signed/infinite iconSize or splashRadius values that another branch ignores.

### Limits and aggregate surface

Catalog property-descriptor capacity increases to 1024 because 524 alternative
rows cannot fit in the previous metadata limit. Persisted node values, validation
and atomic property patches are explicitly decoupled and remain capped at 512.
The maximal compatible rounded/color fixture uses 505 values; the circle/paint
fixture uses 478. All 2048 symbol occurrences, 2 MiB candidate bytes and the
45-second analyzer deadline remain enforced. FD13, Catalog API14, Canvas model18
and NBFC1 are unchanged. The reviewed pre-IconButton records remain byte-for-byte
equivalent after normalizing line endings.

Current totals are 81 widgets, 75 const-capable definitions, 3076 writable rows
(3059 outside Scaffold), 78 scalar plus three structural definitions, 302
Boolean-only rows, two nullable-Boolean unions and the separate Object-tag union.
There are 68 insertable destinations (66 ANY plus two trait-bound), yielding
5508 placements: 5156 accepted and 352 rejected. Thirteen generic required-slot
wrappers plus Expanded/Flexible give fifteen creation wrappers. Categories are
Material18, Layout31, Scrolling3, Basic23 and Accessibility6. Relative to the
historical practical 92-widget target, 81 are complete and 11 remain; no recovered
fixed-order inventory is claimed.

Stable Properties cells, all constructor/selection combinations, strict editors,
native history, save/reopen/continued edits, required-slot guards, SVGs and the
candidate/SDK/Canvas tests belong to this slice. No user IDE/userdir or Flutter
application is modified; deferred platform and global physical acceptance work
are not part of this palette change.

### Pinned SDK, Canvas and focused verification

All 1582 core tests pass on the final source snapshot. The focused NetBeans
selection comprises 767 tests, including all 524 stable Properties rows, 92
editor-component cases, all 5508 placement cells and the three live IconButton
lifecycles. Its first run found one incorrect new-test assumption: removing an
optional Selected icon may remove the slot key rather than leave SingleSlot.empty.
The assertion now accepts either canonical empty representation while requiring
the selected widget to be absent and the required Icon intact. All three live
cases pass on rerun (50.57 seconds); production was unchanged by that correction.

The four real-SDK test methods validate twelve same-file candidates: ten accepted
and two intentional dynamic/nullable ButtonStyle-reference rejections. These
include all four dense constructors, whole styles and factories, repeated scoped
state-list items, direct/local composite fallback and disabled foreign-package
handlers. Every symbol occurrence remains unique and proved; FD reopening is
exact and the original Dart/pubspec/package-config bytes remain unchanged.
No candidate-budget or analyzer-timeout relaxation is used.

The complete Flutter suite passes 1421 tests with clean analyze output, including
46 dedicated IconButton cases, all 256 style-state combinations and 96 raw-SDK
pixel comparisons across Material 2/3, four variants, selection, enabled state and
direction. Both supported Canvas routes preserve child editing state during
constructor/history changes. Mounted-size/ink/constraint diagnostics retain the
active Selected icon in M3 and the ordinary Icon in M2; the geometry fallback
is labeled as button geometry rather than mislabeling every failure as icon size.

All 2727 previous W/P/S/C records are unchanged in order after normalizing line
endings; IconButton contributes 528 new records. The final offline Web build and
all 40 source/35 Web manifest entries were independently rehashed and size checked.
Source manifest SHA-256:
`16fede70effeaa1f48282c0b7c3a4eaa71bd1e10213e20839dce78ac5019b484`.
Web manifest SHA-256:
`2e240defbac62ed8e73687648c26cea7cff8eb3aac9275f127698472241987de`.
main.dart.js is 3,028,796 bytes, SHA-256
`656f034a6848a284e1773da923ae81e76b647da2eb178c97236c16fa9772417c`.

### Final integration corrections and evidence

The first complete remaining-reactor pass exposed one exact Java/Canvas metadata
disagreement: IconButton.padding had nonnegative numeric bounds on both sides,
but Canvas omitted its explicit nonnegative-insets constraint flag. The Canvas
definition and its one reviewed record are corrected together, with a regression
for physical and directional padding. All other 3254 reviewed records are
unchanged. The complete Flutter suite, affected Java Canvas suites, all three
parity checks and packaged runtime integration are repeated for this correction.

That pass also repeated the unchanged Windows folder-rename timing failure in
FlutterDesignerMoveDependencyGuardTest at line 483, after the exclusive admission
lock had been released; the in-admission exclusion assertion passed. Its exact
runtime blocker is not established and no guard or timeout was relaxed. All 38
tests in the unchanged class passed in a fresh Maven fork (1.874 seconds). This
is recorded as a retry, not a one-shot clean full-reactor result or a fixed bug.

The final affected-module install succeeds at 2026-09-07 00:11:25 +03:00.
It rebuilds the corrected runner and plugin, reruns 575 Canvas/parity/Palette/
IconButton Properties tests (573 executed, two existing optional skips), and
runs all 15 Failsafe cases (14 executed, one existing physical-Canvas skip).
No Failsafe filter or test-skip override is used. The Palette description also
uses the actual `Selected` Properties label.

Final reports contain 4242 registered Java cases across 390 reports: 4235
executed, zero failures/errors and seven allowed optional SDK/physical/filesystem
skips. Surefire records 4227 cases across 383 reports and Failsafe records 15
across seven. All reports except the retained analyzer gate are fresh relative
to the full remaining-reactor start at 2026-09-06 23:47:48 +03:00. The 63 analyzer
tests are retained with all 39 analyzer/core-API inputs rehashed unchanged;
target and installed analyzer JARs both have SHA-256
`6dce65a3ec1b43733c67290db090b65a390d55fe347d80c788ee91a3c7ad8c5f`.
The new IconButton real-SDK suite runs freshly in the complete reactor; it is not
part of that retained evidence. All 1596 non-Markdown repository inputs match
the final corrected snapshot after packaging.

Root and module `nbm:cluster` complete successfully. `tools/verify-release.ps1`
passes with freshness checks enabled. The final
`netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm` is 7,623,663 bytes, SHA-256
`9c34187fda0ef73c0bd43d24cc7035ac962361930ea6deb26624b882e9980eab`.
The NBM's embedded module matches all four package/development/runtime copies
(3,414,732 bytes, SHA-256
`c00524423df5c9ffa0ce31f760d5cd1f2c010b471f3fd359b0bdc1869d6eea19`).
Only META-INF/MANIFEST.MF differs from the ordinary Maven module JAR; all four
IconButton SVG assets are present. All 40 source and 35 Web manifest entries
are reverified against the final artifacts. No interactive IDE acceptance or
installed-userdir verification is claimed.

## ADR-110 — Checkbox controlled nullable value and state properties

### Scope and public SDK contract

Admit `flutter.material.Checkbox` at Material/order 190 as a slotless leaf with
both public const constructors, Standard and Adaptive. The pinned Flutter 3.44.8
implementation and the official [Checkbox API](https://api.flutter.dev/flutter/material/Checkbox-class.html),
[standard constructor](https://api.flutter.dev/flutter/material/Checkbox/Checkbox.html)
and [adaptive constructor](https://api.flutter.dev/flutter/material/Checkbox/Checkbox.adaptive.html)
have the same 19 non-key constructor parameters. Stable Designer identity remains
the Key policy, not a free-form source argument. There is no invented child/label.

Expose 106 typed rows: 22 direct/policy rows (19 SDK parameters with density split
into two axes plus Constructor/Enabled), 21 local outlined-shape leaves, 18 nullable
fill/overlay state-color rows and 45 local plain/stateful-side rows. Ten shared
shape families remain fully editable; whole shape requires OutlinedBorder, not
merely ShapeBorder. Prototype values are Standard, Enabled true and Value false.

### Controlled value, null and atomic editing

Value, Constructor and Enabled are required. Value admits false, true and explicit
null; null is mixed and requires Tristate true. Core validation rejects invalid
direct commands. Setting mixed in Properties also sets Tristate true in one Patch;
disabling/resetting Tristate while mixed sets Value false in the same Patch. One
Undo restores both values. Required nullable Value cannot be omitted. Concrete
Boolean values remain centered checkboxes, including after save/reopen and edits.

The generated and preview widgets remain controlled. Enabled without onChanged
gets a benign `(_) {}`; the application must still update its value in its own
callback. Disabled emits onChanged:null without evaluating, importing or proving
the retained inactive callback reference. FD-only callback edits retain paired
source history. Canvas interaction never silently rewrites the model Value.

### Complete local state alternatives and strict references

Whole fillColor/overlayColor use strict non-null WidgetStateProperty<Color?>
references or supported zero-argument invocations. Each excludes its own nine
local nullable-color rows. State priority is disabled, error, dragged, pressed,
selected, scrolledUnder, hovered, focused, default. An omitted entry continues;
explicit null terminates local resolution and lets the real SDK proceed through
its own fallback chain. No synthetic disabled override or button styleFrom rule
is reused. All 256 combinations remain deterministic; arbitrary project resolvers
are supported through strictly analyzed whole references, never opaque source text.

Whole side is a strict BorderSide reference, including WidgetStateBorderSide
subtypes, exclusive with all local side fields. Base Color/Width/Style/StrokeAlign
construct a plain BorderSide using its SDK defaults. SideStateful false/unset
preserves the SDK's unselected-only plain-side behavior. SideStateful true uses
the public WidgetStateBorderSide.fromMap and also resolves selected states.
Each of eight state buckets has Mode border/inherit plus four side leaves.
Inherit returns explicit null and clears bucket details; a detail selects Border
and enables Stateful atomically. Missing details use that BorderSide constructor's
defaults rather than partially inheriting a different state, the base or theme.
Resetting/disabling Stateful clears state buckets but preserves the base side.
Whole/local color, side and shape switches are atomic and independently undoable.

ValueChanged<bool?>, WidgetStateProperty<Color?>, OutlinedBorder, BorderSide,
MouseCursor and FocusNode use the existing closed generic/static witness pipeline.
Dynamic, nullable outer reference types, wrong generics and wrong callback
signatures remain rejected. No analyzer grammar, timeout, candidate budget or
raw-expression escape hatch is added. Both literal public state-map constructors
remain const-capable when all members are const.

### Actual SDK Canvas behavior

The isolated runner constructs real Checkbox/Checkbox.adaptive widgets and never
executes project callbacks, factories or objects; unavailable project effects are
diagnosed. SDK theme/default precedence, plain-versus-stateful sides and selection
semantics are retained. Adaptive's Cupertino branch ignores fillColor, hoverColor,
overlayColor, splashRadius, materialTapTargetSize, visualDensity and isError while
keeping every corresponding model field. Shape and side still apply. Theme.platform
chooses adaptation; Cupertino tap sizing also depends on the SDK's separate host
defaultTargetPlatform. Tests vary both rather than fabricating a platform rule.

An explicit density axis uses zero for its omitted peer. Signed finite and
positive-infinite splashRadius values are retained: actual Material SDK tests
exercise hover, focus, held press, painting, release and callback without clamping.
Negative/zero values suppress reaction painting through the SDK's own guard.
Existing shape resource budgets remain enforced without changing source values.

Pinned Flutter 3.44.8's Material and Cupertino Checkbox painters omit TextDirection
when painting an OutlinedBorder. Raw SDK controls and pinned source inspection
identify this limitation for
LinearBorder and directional/mixed corner geometry in RoundedRectangleBorder,
BeveledRectangleBorder, ContinuousRectangleBorder and RoundedSuperellipseBorder.
Canvas diagnoses this limitation at Checkbox.shape and uses an explicit SDK-default
RoundedRectangleBorder (radius 1 for M2, 2 for M3, 4 for Cupertino). This is a
documented preview approximation, not claimed pixel parity for those unsafe shapes.
It retains the real Checkbox State, value, semantics, model fields and exact Dart
generation. Safe physical-corner shapes remain supported, and an explicit safe
shape overrides an unsafe CheckboxTheme shape. The guard checks effective theme
fallbacks without executing project code; adaptive Apple ignores CheckboxTheme.
No custom wrapper or source rewrite attempts to repair the upstream SDK painter.

### Aggregate surface and verification boundary

Current totals are 82 widgets, 76 const definitions, 3182 writable rows (3165
outside Scaffold), 79 scalar plus three structural definitions, 307 Boolean-only
rows, three nullable-Boolean unions and the separate Object-tag union. The same
68 insertable destinations (66 ANY plus two trait-bound) form 5576 placements:
5222 accepted and 354 rejected. Categories are Material19, Layout31, Scrolling3,
Basic23 and Accessibility6. Thirteen generic/fifteen total creation wrappers are
unchanged. The historical practical 92-widget target leaves ten; no recovered
fixed-order inventory is claimed. FD13, Catalog API14, Canvas model18, NBFC1 and
the 512-value/2048-probe/2-MiB/45-second budgets remain unchanged.

The slice includes typed stable cells, all-field edits, dependent atomic changes,
Save/reopen/continued edits, native Undo/Redo and rollback, real-SDK generated
candidates, actual-SDK Canvas parity and all four SVG assets. Deferred physical
desktop acceptance and platform-provider work remain outside this palette change.
No user IDE/userdir or Flutter application is modified.

### Checkbox verification evidence (2026-09-07)

The focused core install passed 1602 tests; the final UI/palette/editor selection
passed 836 tests across 22 classes. Three live paired-mutation tests cover all
106 stable property rows, constructors, dependent edits, save/reopen/continued
editing, native Undo/Redo and rejection rollback. Four real-SDK test methods
cover 25 same-file candidate analyses: 16 accepted and nine deliberately rejected.
Incompatible arguments require a blocking argument-type diagnostic at the exact
reference span; Dart-assignable dynamic/nullable outer references must instead
fail the stricter typed witness. The combined seven live/SDK methods passed.
Initial focus failures exposed two UI fixture/dispatch omissions and one SDK-test
expectation that incorrectly required symbol evidence after compilation already
failed; the corrected focused selections were rerun in full before the final gate.

Flutter analysis is clean; all 1457 Flutter tests passed, including 34 dedicated
Checkbox tests and two runtime leaf-admission tests. Coverage includes 192 default
SDK pixel combinations, 24 plain/stateful-side pixel combinations, 24 themed/null
color/density combinations, all 256 color and 256 side state subsets, semantics,
controlled callbacks, retained State/value, TickerMode, signed/infinite splash and
the diagnosed upstream shape limitation. The 5576-cell runtime placement matrix
passes. All 3255 prior contract records are unchanged; the 107 new records match
the compiled Java catalog exactly.

The source manifest verifies all 40 entries (SHA-256
`7b40047f88de02cdf5986c090740ffb626dbc0c2cc287441372f28574637d29f`),
and the offline release Web manifest verifies all 35 entries (SHA-256
`ef12f6efa1333f04ed6049e8ea033cb32b72269f774177533151337ed457b010`).
The Web entry point is 3064148 bytes, SHA-256
`28e3f45662e9f946ff66b1c1947376abe6bd410adc1618f6a3280c3b9e246486`.
Both Java artifact pins are updated. Before the final Java reactor, all 1608
non-Markdown source/build inputs were snapshotted for post-build comparison.

The unchanged analyzer gate is retained rather than counted as freshly rerun:
63 tests in eight reports. All 39 relevant source/build hashes and both target
and installed analyzer JARs were rechecked; their identical JAR SHA-256 remains
`6DCE65A3EC1B43733C67290DB090B65A390D55FE347D80C788EE91A3C7AD8C5F`.
Fresh Checkbox candidates exercise the new types through that same analyzer.

The unfiltered remaining reactor (`mvn --no-transfer-progress install -pl
"!:dart-analysis"` with the pinned Dart/Flutter/pub-cache/Web SDK flags) completed
successfully in 13:00 at 10:47:26 +03:00. Final XML evidence has 4287 recorded
tests across 394 reports, zero failures/errors and seven allowed optional skips:
4217 fresh passes plus the 63 unchanged retained analyzer passes. All other
386 reports postdate the final gate start. This includes all 123 paired-mutation
integration tests, the Java/Flutter parity gate, real Web artifact gate and
assembled NetBeans runtime tests. The unchanged 38-test move-dependency guard
passed on its first full run; no test retry or timeout adjustment was needed.
Surefire reported forced fork termination after its 30-second post-System.exit(0)
exit wait; all test reports and the complete build succeeded. This is recorded as
an observed shutdown warning, not a claimed production fix or diagnosed cause.

Root and module `nbm:cluster` both succeeded, and `tools/verify-release.ps1` passed
with freshness checks enabled. The final NBM is 7646684 bytes, SHA-256
`929B6A787C3DAB6A68D936F6AA99DCCF91FE60D54F1B9A138F7FA4AA5A913B18`.
Its embedded module matches all four package/development/runtime module copies:
3424897 bytes, SHA-256
`22146EF7D49F840480B15CF70A1B6736B130B3AF642D1D64EC66ECDFC9529632`.
Only META-INF/MANIFEST.MF differs from the ordinary Maven module JAR; all four
Checkbox SVG assets are present. The embedded Canvas runner exactly matches its
built JAR (333460 bytes, SHA-256
`BAC0370FF26B7A03E26333A812A0F06DF6EA19C617DE0FA0689257A13EB4C0F9`).
All 1608 frozen non-Markdown inputs are unchanged after packaging; all 40 source
and 35 Web manifest entries were independently reverified. No interactive IDE
acceptance or installed-userdir verification is claimed.

## ADR-111 — Switch state icons, images and adaptive styling

Date: 2026-09-07

### Complete public constructor surface

Admit flutter.material.Switch as a Material/order-200 scalar leaf with Standard
and Adaptive constructor selection. Both public constructors are const-capable;
the standard constructor has 27 non-key SDK parameters and adaptive has 28.
Key remains the shared stable-identity policy. The 201-row schema comprises
30 direct/policy fields (including required Constructor, Enabled and Value),
36 local nullable state colors, nine local nullable outline widths and 126 local
state-Icon fields. No child slot, builder or invented preview child is introduced.

Value is required and non-nullable, initially false. Enabled is initially true;
without a project onChanged reference it emits a benign callback. Disabled emits
onChanged:null while retaining its reference metadata without importing, evaluating
or proving the inactive callback. Preview interaction does not change stored Value.
onChanged/onFocusChange are strict ValueChanged<bool> references or zero-argument
factories. Both image-error callbacks use strict ImageErrorListener, including
factory support, with the existing closed type-witness protocol. Dynamic, nullable
outer values and incorrect generics/signatures remain rejected.

Apply Cupertino theme admits omission, true, false and explicit null, but is an
adaptive-only constructor argument. Setting it selects Adaptive in the same patch;
switching to Standard resets only this incompatible argument. Deprecated activeColor
remains editable and emitted alongside newer activeThumbColor/activeTrackColor;
the actual SDK, not Designer, supplies precedence and branch-dependent fallback.

### Presence-aware state maps and full Icon values

Thumb color, track color, track-outline color, overlay color, track-outline width
and thumb icon each provide a strict whole WidgetStateProperty reference and
mutually exclusive local alternatives. Whole color maps use Color?, width uses
double?, and icon uses Icon?; nullable outer references are not accepted. Four
color families reuse typed literal/theme/null cells. Nine width cells use signed
finite integer/double, positive infinity and explicit null. Local numeric maps rely on the actual Switch
argument's downward inference rather than introducing a shadowable bare double
type or an extra dart:core import. Public fromMap constructors remain const when
all entries are const.

Each local map resolves the first present matching state in this exact priority:
disabled, error, dragged, pressed, selected, scrolledUnder, hovered, focused, then
default. Omission continues lookup; explicit null terminates local lookup and
delegates to SDK fallback. No implicit disabled:null entry changes the default.
Arbitrary state combinations remain available through typed whole references.

Each of nine thumb-icon buckets has Mode plus all thirteen non-key Icon fields:
Data, Size, Color, Shadows, Blend mode, Fill, Weight, Grade, Optical size, Font
weight, Semantic label, Text direction and Apply text scaling. Mode inherit
returns null and clears local details. Editing a detail selects Mode icon;
resetting Mode clears its bucket. Icon mode with omitted or explicit-None Data
emits Icon(null), which is not a null resolver: the SDK can still change thumb
radius when an Icon object exists. Optional glyph editors distinguish omitted
Data from explicit None; the existing required Icon.icon editor remains unchanged.
Whole/local switches and dependent bucket edits are single undoable patches.

Flutter's Switch painter reads Icon fields directly rather than mounting an Icon
widget. It uses glyph, size, color, variable-font axes and shadows, but ignores
Icon blendMode, fontWeight, semanticLabel, textDirection, applyTextScaling and
IconData.matchTextDirection. These fields are nevertheless preserved and emitted
exactly; Canvas does not fabricate effects the SDK lacks. Existing Icon numeric,
glyph, theme, shadow and resource constraints are reused.

### Images, actual SDK rendering and lifecycle

Both thumb-image fields use existing declared project/package asset identities,
AssetImage/ExactAssetImage, exact scale and ResizeImage policies. No raw file path,
network expression or executable provider is added. Image-error handlers require
their matching image. Setting a handler without a provider is rejected; resetting
one provider atomically clears only its matching handler. Both images enter the
existing revision-scoped, bounded image-resource admission/projection pipeline.
Canvas reports missing resources and unavailable project effects without executing
project callbacks, factories or resolvers.

In pinned Flutter 3.44.8 both constructors use the same internal Material switch
implementation; adaptive Apple selects a Cupertino configuration, not a separate
CupertinoSwitch widget. State icons, images and direct parameters remain forwarded.
Actual Theme.platform, Material 2/3, SwitchTheme, custom SwitchTheme adaptations,
Cupertino theme overrides, focus, gestures, padding and tap-target behavior remain
SDK-owned. Signed/infinite splash values retain the established SDK-faithful domain.
Equal infinite resolved outline endpoints are legal and rendered exactly. The SDK
unconditionally interpolates unequal finite/infinite endpoints and asserts, even at
a settled value. Canvas diagnoses only this unsafe pair and previews its outline
with the SDK default width of two; the stored values and generated Dart remain exact.
Theme-derived endpoints follow the same contextual check, not a blanket numeric clamp.
Finite padding sides can overflow their resolved sum to infinity. Bounded parents
retain exact SDK layout; only an affected unbounded axis receives a diagnosed
padding-preview-unavailable presentation with the existing selectable Designer
handle. The exact padding values remain stored and emitted; no global cap is added.

The pinned SDK sets its retained switch State's isCupertino flag true when entering
Apple adaptation, but never resets it on returning to Material. Canvas handles only
that effective configuration boundary by remounting the public Switch shell while
retaining its Canvas-owned FocusNode, outer stable-ID wrapper, selection and stored
value. Within a configuration, ordinary edits retain SDK State. The boundary resets
internal animations; there is no child subtree to lose. No private SDK state is
mutated and no workaround key is emitted into project Dart. Raw retained/fresh SDK
controls and Canvas transition/focus tests cover this specific upstream lifecycle
bug rather than claiming the source-generated app itself has been repaired.

### Current aggregate and verification scope

The catalog now contains 83 widgets, 77 const-capable definitions, 3383 writable
rows (3366 outside Scaffold), 80 scalar plus three structural definitions, 319
Boolean-only fields, four nullable-Boolean unions and the separate Object-tag
union. The unchanged 68 destinations (66 ANY and two trait-bound) produce 5644
placement candidates: 5288 accepted and 356 rejected. Categories are Material20,
Layout31, Scrolling3, Basic23 and Accessibility6. Thirteen generic/fifteen total
creation wrappers remain unchanged. The historical practical 92-widget target
leaves nine; this does not claim recovery of a missing fixed-order inventory.
FD13, Catalog API14, Canvas model18, NBFC1, metadata1024 and the existing
512-value/2048-probe/2-MiB/45-second bounds stay unchanged. The existing closed
type-witness grammar already admits double?; its analyzer overlay now qualifies
that nested primitive through the isolated dart:core alias, just as bool? already
does. Previously, a valid WidgetStateProperty<double?> reference was incorrectly
qualified through the Flutter alias and rejected. Dynamic, nullable outer types,
wrong generics and user core-import restrictions remain fail-closed; no core-API
or grammar widening is introduced. No user IDE/userdir modification or user Flutter
application edit belongs to this slice. Deferred global physical acceptance remains
deferred.

### Focused evidence and reproducible Canvas artifacts

The complete core install passed 1624 tests with no failures, errors or skips;
22 new cases cover constructor/schema/generation, command history and payload
contracts. The 24-class Properties/palette/image gate passed 871 tests. Its initial
failure was an SVG test expectation missing the normalizer's default stroke width;
the expectation was corrected and the identical full scope passed. All 201 cells,
both constructor configurations, nine optional glyph editors, image dependencies,
nullable/infinite widths and the full placement matrix are covered.

The frozen Flutter suite passed 1493 tests; a separate repeat passed all 34 dedicated
Switch tests. Flutter analysis is clean. The first complete run found an old
Scaffold test's next-block delimiter now including the intervening Switch contract;
that test boundary was corrected before the successful full repeat. Raw SDK pixel
controls cover signed/equal-infinite widths, unsupported interpolation, adaptation
and retained-state behavior. Canvas tests cover exact supported rendering, all Icon
fields, state priority/null fallback, focus/ordinary State retention, controlled
tap/drag selection, semantics, images, intrinsic parents and contextual padding.
All 3362 previously reviewed capability records remain byte-identical after line
ending normalization; the 202 Switch records exactly match the compiled Java proof.

Offline Web release succeeded in 15.1 seconds. main.dart.js is 3103961 bytes,
SHA-256 `fc11d2e089e36186bf496a776f6f0f528cc348a3f1a9cd961170d99b5932eb8e`.
All 40 source and 35 Web manifest entries were independently checked for exact
scoped paths, bytes and SHA-256. Source manifest SHA-256 is
`2dbedf98ca980e7905bfad2d2d90e5c72dfe548e97c0036ee9f70ad3c47392b7`;
Web manifest SHA-256 is
`f16ca04e77d240fd503fc59f02bada6bc173a7989e50c37110700a41b91bb57e`.

The first Switch candidate gate exposed the nested-double witness defect described
above; the three live history tests already passed, while two SDK methods correctly
prevented acceptance of the incomplete implementation. After the narrow fix, the
complete analyzer install ran fresh from 08:27:56 to 08:42:21 UTC: 65 tests across
eight reports, zero failures/errors/skips, including 33 real-SDK candidate methods.
The new regression tests cover twelve scripted import-scope combinations, ten
accepted numeric SDK proofs across five scopes and four adversarial branches.
Both built and installed analyzer JARs have SHA-256
`466C88A04BCE84A2C31B52E177AC995BADA0055E6E67C37D07027FD792DA02F7`.
These are fresh results from this slice, not the previous retained 63-test baseline.

With the rebuilt analyzer and Canvas JARs installed, the repeated plugin-focused
gate passed all 35 methods: six Switch SDK methods, three live mutation/history
methods, three Java/Dart parity methods and 23 Web artifact methods. The six SDK
methods accepted 23 valid candidates and rejected 15 intentional invalid candidates
(38 total); the complete gate took 2 minutes 10 seconds. Both constructors,
dense local fields, whole references/factories, images, explicit null, Icon(null),
signed/mixed/infinite widths, disabled callback isolation and unchanged project
files are covered. The live gate traverses all 201 cells, atomic dependencies,
Save/reopen/further edits, native Undo/Redo and exact-byte rollback.

### Final reactor and package verification

The unfiltered remaining ten-module reactor (excluding only the separately and
freshly verified analyzer) completed successfully at 09:01:21 UTC after 14 minutes
47 seconds. Together the two fresh gates produced 398 reports: 4338 recorded
tests, 4331 executed, seven permitted optional skips, zero failures and errors.
All reports meet their gate's UTC freshness cutoff; no old baseline reports are
retained. The plugin has 2474 Surefire tests and nine package integration tests;
the runtime gate has six cases, one optional physical acceptance case skipped.
All 38 dependency-guard tests passed on the first full run without retry or timeout
adjustment. All 126 live mutation/history tests passed, including the new Switch
cases. Surefire reported a 30-second post-System.exit(0) fork termination warning;
the complete reports and reactor succeeded. This records an observed shutdown
warning, not a diagnosed cause or a claimed production repair.

Root and module nbm:cluster both succeeded. tools/verify-release.ps1 passed with
freshness checks enabled. The final NBM is 7678537 bytes, SHA-256
`5DC5469126DBCE8803D5FB8A6743674D0119F3F8556E122DCC453F3EA4504827`.
Its embedded module matches all four package/development/runtime module copies:
3430245 bytes, SHA-256
`044074CA62D60302F2D1ADF5237F359DA054069D1ADC19B7EB098C84B7806343`.
Only META-INF/MANIFEST.MF differs from the ordinary Maven module JAR; all four
Switch SVG assets are present. The embedded Canvas runner matches its built JAR:
340748 bytes, SHA-256
`3BD36BFD87C49544E442FFB6A301BE7ADE0380D204A0C674D22327D28373E12E`.
The embedded analyzer is the newly verified 87439-byte JAR with the exact
`466C88A04BCE84A2C31B52E177AC995BADA0055E6E67C37D07027FD792DA02F7`
hash, not the previous build. All 1619 frozen non-Markdown inputs (including the
40 analyzer/core/root-POM inputs) are unchanged after packaging; all 40 source
and 35 Web manifest entries were reverified. No interactive IDE acceptance or
installed-userdir verification is claimed.

## ADR-112 — Slider ranges, interaction and adaptive rendering

Status: Accepted. Scope: the next complete palette slice after Switch, reviewed
against Flutter 3.44.8 and the public
[standard constructor](https://api.flutter.dev/flutter/material/Slider/Slider.html),
[adaptive constructor](https://api.flutter.dev/flutter/material/Slider/Slider.adaptive.html)
and [Slider API](https://api.flutter.dev/flutter/material/Slider-class.html).

### Complete constructor and property contract

Admit flutter.material.Slider as Material/order 210, a scalar leaf with two
const-capable constructors. The 33 rows are all 22 non-key standard arguments,
required constructor selector and Enabled policy, plus nine local overlay colors.
Adaptive has 21 arguments: padding is absent, not merely ignored at runtime.
Creation stores exactly variant=standard, enabled=true and value=0. Min/Max and
other SDK defaults remain unstored. There are no child slots or synthetic Key row.

Value, Min, Max and nullable Secondary track value accept signed finite numbers
and the narrowly closed dart:core double.infinity/negativeInfinity members.
Fixed arithmetic lowering preserves infinities without requiring a visible
unshadowed double type or admitting arbitrary expressions. Prospective validation
compares effective SDK doubles: min<=max, both present track values within range,
and divisions either null or positive. Invalid peer edits/resets submit nothing
and never clamp or rewrite another field. Divisions use the existing shared
native/Web exact integer upper bound 9007199254740991; there is no artificial
global tick-count restriction. Secondary values below Value are legal and let the
SDK suppress the secondary track. Nullable fields preserve explicit null versus
unset even when the SDK effect is the same; an empty label is also retained.

All four SliderInteraction and six ShowValueIndicator constants are admitted,
including deprecated always. The deprecated nullable year2023 parameter remains
editable and generated. Colors reuse literal/semantic theme values, padding
reuses non-negative physical/directional EdgeInsetsGeometry, and Mouse cursor
has all 41 reviewed presets or a strict project reference.

The three activation callbacks use strict ValueChanged<double>; the semantic
formatter uses strict SemanticFormatterCallback (String Function(double)).
FocusNode, MouseCursor and WidgetStateProperty<Color?> retain non-null public
reference/zero-argument-factory proof. Disabled onChanged alone becomes inactive:
metadata is retained but its import, evaluation and symbol/type proof disappear
from generated Dart, where onChanged:null is emitted. Enabled without a project
callback emits a benign no-op. Start/End and semantic formatter remain emitted
and proved even when disabled, matching supplied SDK arguments rather than
inventing broader callback suppression. Existing analyzer witness grammar and
nested dart:core double qualification are reused unchanged. The plugin proof
planner now includes the Material-only SemanticFormatterCallback typedef in its
existing candidate-wide Material umbrella selection; every witness shares that
library regardless of occurrence order. Navigation roots remain bound to each
original symbol, and candidates without the formatter keep their Widgets proof
umbrella. Real SDK tests assert this selection and strict acceptance/rejection.

Whole overlay references and nine local nullable states are exclusive.
Local presence-aware priority is disabled, error, dragged, pressed, selected,
scrolledUnder, hovered, focused, default. Omission continues searching; explicit
null stops lower local entries and lets the actual SDK resolve its fallback.
No implicit disabled:null is added. The runtime currently requests only its
actual disabled/hovered/focused/dragged state combinations; the complete reviewed
local protocol is retained, and whole references can express arbitrary sets.

### Stable editing and isolated actual-SDK preview

All 33 cells use the catalog-backed stable Properties bridge. Scalar edits do not
recreate the property set or replace its cells. Explicit Booleans remain centered
checkboxes, including nullable year2023's explicit values. Nullable division and
secondary-track panels have field-specific null/default text rather than the
old IndexedStack-index and inherited-radius wording. Negative Infinity is enabled
only by the exact corresponding catalog enum constraint, not globally.

Setting Padding atomically selects Standard; choosing Adaptive atomically resets
Padding. Whole/local overlay replacement resets only conflicting local state or
whole-reference data. Both operations are one native Undo entry. Other fields
ignored by adaptive Apple rendering remain stored through constructor switches.

Canvas instantiates the actual Material Slider or the SDK's adaptive Apple
SizedBox(width:infinity, child:CupertinoSlider(...)) branch. Material resolves
SliderTheme, Material 2/3, year2023 and platform defaults normally. Cupertino
receives only Value, Min/Max, Divisions, the three activation callbacks,
Active color and Thumb color. The SDK ignores all other shared arguments there,
including focus/autofocus, cursor, overlay, formatter, label, secondary track,
interaction, indicator and year2023. There is no Switch-style Theme Adaptation
lookup for Slider.

Canvas never executes project callbacks, factories or formatters, nor writes
gesture values into the model. A Slider-only pointer listener handles selection
even for equal-value taps that the SDK does not report via onChanged. A persistent
isolated local FocusNode avoids the raw Material supplied-node-to-null lifecycle
fault without remounting ordinary Slider State. A second raw public-shape control
found stale thumb position after Material(.2) -> Cupertino(.8) -> Material(.8):
the outer SDK position controller retains .2. Canvas rekeys only the public SDK
shell at an effective Material/Cupertino branch boundary, retaining its outer
stable ID, model selection and isolated FocusNode identity. Same-branch ordinary
value/theme edits and standard/adaptive switches on Material retain SDK State.
Apple still ignores focus; no Apple focus behavior is fabricated. An explicit
lifecycle diagnostic documents this Canvas-only workaround, and no key or SDK
repair is emitted into project Dart. Actual Apple-ignored references
produce no false unavailable warning. Active references that cannot be executed
in isolation receive a concrete diagnostic and documented preview behavior.

Constructor legality is not universal mount/paint safety. Raw pinned-SDK probes
confirm equal finite and equal signed-infinite bounds render disabled Material
sliders; Cupertino normalization divides by zero for any equal range. Unequal
infinite ranges and extreme finite subtraction can produce NaN normalized values
or non-finite inverse gesture values. The adaptive infinite-width wrapper also
cannot lay out in an unconstrained Row. Those contextual SDK limits preserve the
exact model/generated source and receive explicit preview-unavailable reasons.
A huge discrete count with zero-width or sufficiently tiny positive themed ticks
can force an unbounded SDK paint loop. For counts above 10,000, Canvas checks the
SDK density predicate against a conservative width bound derived from public
constraints and preferred part sizes; combinations that cannot guarantee density
suppression receive a diagnostic instead of changing divisions or silently
replacing the theme. Ordinary large counts with density-suppressed SDK ticks and
Cupertino rendering remain admitted. Finite-span overflow on Cupertino is also
retained when normalization is finite: its stable inverse interpolation differs
from Material's non-finite inverse conversion.

### Aggregate and scope boundaries

The catalog contains 84 widgets, 78 const-capable definitions, 3416 writable rows
(3399 outside Scaffold), 81 scalar plus three structural definitions, 321
Boolean-only rows, five nullable-Boolean unions and the separate Object-tag union.
The unchanged 68 destinations (66 ANY/two trait-bound) produce 5712 candidates:
5354 accepted and 358 rejected. Categories are Material21, Layout31, Scrolling3,
Basic23 and Accessibility6. Thirteen generic/fifteen total creation wrappers stay
unchanged. Historical target 84/92 leaves eight; this is not a recovered fixed
ordered inventory. FD13, Catalog API14, Canvas model18, NBFC1, metadata1024,
512-value/patch, 2048-probe, 2-MiB and 45-second bounds stay unchanged.
User IDE/userdir and Flutter application files are outside this slice.
Previously deferred global physical acceptance remains deferred.

### Focused verification and frozen artifacts

The full core install passed 1643 tests, including 19 new Slider cases, with no
failures/errors/skips. The Properties/palette/editor gate produced all 25 fresh
reports: 887 tests, zero failures/errors/skips. Its terminal exit status was lost
during agent context compaction, so the reports, not an unavailable Maven status,
are the evidence for that focused gate; the final reactor reruns the whole scope.

The five real-SDK methods exercised 52 candidates: 37 valid candidates accepted,
15 intentional invalid candidates rejected. Together with three live pair-history
methods, the focused Maven gate passed all eight methods in 1 minute 49 seconds.
It covers every field, dense constructors, strict references/factories, wrong,
dynamic and nullable-outer witnesses, callback isolation, nullable/empty values,
all enum members, portable division limits and constructor-legal extreme ranges.
Candidate overlays leave source, pubspec and package config unchanged.
The live tests preserve exact Dart/FD bytes through insertion, each of the 33
cells, invalid changes, native history, Save/reopen/further edits, atomic padding
transitions, whole/local overlay replacement and inactive callback endpoints.
The formatter's Material proof umbrella is asserted across every typed probe.

After the final lifecycle repair, the complete Flutter suite passed 1525 visible
tests (45 additional hidden loader entries are not tests) in 26.393 seconds,
including 30 dedicated Slider tests. Flutter analysis is clean. Coverage includes
32 exact raw SDK pixel comparisons, all 256 overlay-state combinations, both
constructor branches, M2/M3, year2023, all interaction/indicator modes, controlled
selection, ordinary retained State, focus ownership, contextual numeric/layout/
tick limits and the independently reproduced boundary-position regression.
All 3564 previously reviewed capability records remain unchanged; the 34 new
Slider records exactly match an independently compiled Java snapshot.

Offline Web release succeeded in 15.6 seconds. main.dart.js is 3157840 bytes,
SHA-256 d81431f39c2ea48fa2c29c9d145d67775fcfbbb0f34515e0bb8c6e9a5590ac6b.
All 40 source and 35 Web manifest entries were independently checked for exact
scoped paths, sizes and hashes. Source manifest SHA-256:
d11baf980db0ced49f06e53ba51119ca90b2dac0d434f2193fc79cef4a829469.
Web manifest SHA-256:
0e7ec234c3fbf4ddbe7a700e0d3bf1a2f8979b70cf5a1533af81c6f8365b9d75.
After installing that Canvas JAR, all 26 Java/Dart parity and Web artifact methods
passed with Maven exit zero in 12.229 seconds. All 1631 non-Markdown source inputs
are frozen for the final reactor/package checks.

The analyzer itself is unchanged from Switch's fresh 65-test gate on this same
SDK at 08:27:56–08:42:21 UTC. Its 40 analyzer/core-API/root-POM input hashes match
that baseline exactly; this is retained evidence, not a claimed new analyzer run.
The plugin proof planner change is covered by the fresh SDK candidate tests above.

### Final reactor, retry and package verification

The unfiltered remaining ten-module reactor started at 09:44:54 UTC and ran for
15 minutes 48 seconds. Its only error was the unchanged
FlutterDesignerMoveDependencyGuardTest.exclusiveProofPreventsARelevantFolderFromBeingRenamed
at line 483: the post-admission rename Future did not complete within five seconds.
The exclusion assertion had already passed. No worker stack identified the cause,
and no Slider relation or justified lock change was found; this is an observed
timeout, not a diagnosed production bug or a claimed repair.

Without changing source or timeouts, the entire 38-test dependency-guard class was
rerun in a new Maven process together with all three downstream packaging/runtime
modules. All 38 tests passed in 2.200 seconds. The nine package metadata tests and
six runtime cases (one permitted optional skip) also passed; the three-module
install exited zero in 53.401 seconds at 10:02:01 UTC. All 129 live mutation/history
tests and the Slider SDK gate passed in the original unfiltered run. The final
evidence therefore combines the unfiltered run, the exact unchanged-class retry,
downstream gates and the explicitly retained analyzer baseline; it is not a claim
that the initial full reactor was clean on its first attempt.

The final 402 XML reports record 4381 tests: 4374 executed, seven permitted optional
skips, zero failures/errors. Of those executed tests, 4309 are fresh in this slice
and 65 are the hash-matched analyzer baseline. The plugin has 2498 Surefire tests
and nine package integration tests. Every non-analyzer report meets the 09:44:54
UTC freshness cutoff; the eight retained analyzer reports meet their documented
08:27:56 UTC baseline cutoff. Freshness checks were not disabled.

Root and module nbm:cluster succeeded. tools/verify-release.ps1 passed, including
source/report/package freshness and metadata. NBM:
7696625 bytes, SHA-256
0C0E1E81FF2C271030091418657542F179E27E8B50E391B3BE8A42243C606079.
The embedded module matches all four package/development/runtime module copies:
3435879 bytes, SHA-256
99C00CF92FA3DB8B069A7F45DD3CBE0C1FEB46838E1499107A8417ED251B7842.
Only META-INF/MANIFEST.MF differs from the ordinary Maven module JAR, and all four
Slider SVG assets are present. The embedded Canvas matches its built JAR:
344569 bytes, SHA-256
5957B74EFF926220AEF816789D5C432F8278F6B6CF9BD19EBA1898F86DBF8238.
The embedded analyzer is the unchanged 87439-byte baseline JAR, SHA-256
466C88A04BCE84A2C31B52E177AC995BADA0055E6E67C37D07027FD792DA02F7.
All 1631 frozen non-Markdown inputs, including the 40 analyzer/core/root-POM
inputs, remain unchanged after packaging. All 75 source/Web manifest entries were
reverified. No interactive IDE or installed-userdir acceptance is claimed.
