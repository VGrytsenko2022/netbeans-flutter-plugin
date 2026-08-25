# Architecture Decisions

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
