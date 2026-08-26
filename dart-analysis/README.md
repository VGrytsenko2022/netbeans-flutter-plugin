# dart-analysis

Owns the lifecycle-safe raw LSP transport for Dart analysis tooling. It starts the configured executable as `dart language-server --protocol=lsp`, exposes clean stdin/stdout streams, drains stderr separately, and provides bounded idempotent termination and restart.

The NetBeans module hands this connection to the standard NetBeans 30 LSP client. Protocol framing, document synchronization, diagnostics, and language-feature adapters therefore remain outside UI classes and are not reimplemented in this module.

The module also provides a deliberately separate candidate-validation API for
the Flutter Designer save gate. `DartCandidateAnalyzer` starts an isolated
`dart language-server --protocol=analyzer` process, installs one exact
versioned in-memory overlay, requests bounded diagnostics and optional
navigation evidence, then removes the overlay and terminates the process. The
request is bound to an absolute project/file path, caller revision, strict
UTF-8 size and SHA-256; cancellation, timeout, process failure, malformed or
oversized protocol data and `CONTENT_MODIFIED` all fail closed. Candidate text
is never written to disk.

Candidate byte and symbol-probe admission uses the same identity-bearing
`DartCandidateCapacityBudget` that qualified Designer generation. The exact
object is retained by the analyzer request and analyzer limits; a detached,
value-equal policy fails before a process starts. The default profile permits
2 MiB and 256 total probes, including the one scanner-owned superclass probe.

A passing navigation probe proves that the exact symbol occurrence resolved to
one real target below the caller's expected real root and, when requested, to
the expected analyzer target kind. Native `analysis.getNavigation` does not
report the declaring import/export URI, so `expectedLibraryUri` is retained as
identity context but its export graph is not independently proven by this
slice. The immutable result is analyzer evidence only and never authorizes a
file write by itself.

The optional `DartAnalysisServerRealSdkTest` creates a temporary Dart package and validates diagnostics, completion imports returned as `additionalTextEdits` through a NetBeans-30-shaped resolve, a missing-import Quick Fix and its workspace edit, Organize Imports, definition, references, rename, and formatting against a real SDK. `DartCandidateAnalyzerRealSdkTest` additionally proves overlay rejection/navigation without changing the source file. Run them with `mvn -pl dart-analysis -am -Ddart.executable=<absolute-path-to-dart> test`; without that property the SDK-dependent tests are skipped. The NetBeans RELEASE300 capability and completion compatibility wrappers are intentionally kept at the `netbeans-plugin` integration edge rather than in this transport module. Dart's `resolved.command` path for some part-file and multi-file imports is not executed by NetBeans 30's standard completion adapter and is therefore outside this supported completion-import path; the diagnostic Quick Fix remains available.
