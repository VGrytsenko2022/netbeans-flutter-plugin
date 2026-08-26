package dev.flutter.netbeans.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.flutter.netbeans.dart.DartAnalyzerProtocolSession.ProtocolFailure;
import dev.flutter.netbeans.dart.DartAnalyzerProtocolSession.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Validates exact Dart candidates in isolated native analyzer overlays.
 *
 * <p>Every operation owns a separate {@code --protocol=analyzer} process. No
 * candidate content is written to disk, and a passing result is immutable
 * evidence rather than permission to save a file.</p>
 */
public final class DartCandidateAnalyzer {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ProtocolVersion MINIMUM_PROTOCOL = new ProtocolVersion(1, 40, 0);
    private static final ScheduledExecutorService WATCHDOG =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    private final Path dartExecutable;
    private final Consumer<String> stderrConsumer;
    private final DartCandidateAnalysisLimits limits;
    private final DartAnalyzerProtocolSession.ProcessFactory processFactory;

    public DartCandidateAnalyzer(
            Path dartExecutable,
            Consumer<String> stderrConsumer) {
        this(dartExecutable, stderrConsumer, DartCandidateAnalysisLimits.DEFAULT);
    }

    public DartCandidateAnalyzer(
            Path dartExecutable,
            Consumer<String> stderrConsumer,
            DartCandidateAnalysisLimits limits) {
        this(
                dartExecutable,
                stderrConsumer,
                limits,
                DartCandidateAnalyzer::startProcess);
    }

    DartCandidateAnalyzer(
            Path dartExecutable,
            Consumer<String> stderrConsumer,
            DartCandidateAnalysisLimits limits,
            DartAnalyzerProtocolSession.ProcessFactory processFactory) {
        Objects.requireNonNull(dartExecutable, "dartExecutable");
        if (!dartExecutable.isAbsolute()) {
            throw new IllegalArgumentException("dartExecutable must be absolute");
        }
        this.dartExecutable = dartExecutable.normalize();
        this.stderrConsumer = Objects.requireNonNull(stderrConsumer, "stderrConsumer");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.processFactory = Objects.requireNonNull(processFactory, "processFactory");
    }

    /** Starts asynchronous validation of one exact candidate snapshot. */
    public DartCandidateAnalysisOperation analyze(DartCandidateAnalysisRequest request) {
        Objects.requireNonNull(request, "request");
        Operation operation = new Operation(request);
        operation.start();
        return operation;
    }

    private DartCandidateAnalysisResult execute(
            DartCandidateAnalysisRequest request,
            Operation operation) {
        DartCandidateSnapshot snapshot = request.snapshot();
        if (request.candidateCapacityBudget()
                != limits.candidateCapacityBudget()) {
            return unavailable(snapshot, Optional.empty(),
                    DartCandidateAnalysisIssueCode.CAPACITY_POLICY_MISMATCH,
                    "Dart candidate capacity profile "
                    + request.candidateCapacityBudget().profileId()
                    + " is not the exact analyzer capacity policy identity.",
                    request.symbolProbes().size());
        }
        if (snapshot.utf8Size() > limits.maxCandidateBytes()) {
            return unavailable(snapshot, Optional.empty(),
                    DartCandidateAnalysisIssueCode.CANDIDATE_TOO_LARGE,
                    "Dart candidate size " + snapshot.utf8Size() + " bytes exceeds the "
                    + limits.maxCandidateBytes() + " byte analysis limit.",
                    request.symbolProbes().size());
        }
        if (request.symbolProbes().size() > limits.maxSymbolProbes()) {
            return unavailable(snapshot, Optional.empty(),
                    DartCandidateAnalysisIssueCode.PROBE_LIMIT,
                    "Dart candidate requested " + request.symbolProbes().size()
                    + " symbol probes, exceeding the " + limits.maxSymbolProbes()
                    + " probe limit.",
                    request.symbolProbes().size());
        }

        try {
            validatePaths(request);
        } catch (IOException | SecurityException ex) {
            return unavailable(snapshot, Optional.empty(),
                    DartCandidateAnalysisIssueCode.PATH_UNAVAILABLE,
                    "Candidate path or project root is unavailable: " + reason(ex),
                    request.symbolProbes().size());
        }

        Optional<String> protocolVersion = Optional.empty();
        List<DartCandidateDiagnostic> diagnostics = List.of();
        List<DartSymbolEvidence> evidence = List.of();
        DartAnalyzerProtocolSession session = null;
        boolean prioritySet = false;
        boolean overlayAdded = false;
        DartCandidateAnalysisResult result;
        try {
            session = new DartAnalyzerProtocolSession(
                    dartExecutable,
                    request.projectRoot(),
                    stderrConsumer,
                    limits,
                    processFactory);
            operation.attach(session);
            if (operation.cancelled()) {
                return stale(snapshot, DartCandidateAnalysisIssueCode.CANCELLED,
                        "Candidate analysis was cancelled before startup completed.",
                        request.symbolProbes().size());
            }

            String connectedVersion = session.awaitConnected();
            protocolVersion = Optional.of(connectedVersion);
            ProtocolVersion parsed = ProtocolVersion.parse(connectedVersion);
            if (parsed.compareTo(MINIMUM_PROTOCOL) < 0) {
                throw new AnalysisFailure(
                        DartCandidateAnalysisStatus.UNAVAILABLE,
                        DartCandidateAnalysisIssueCode.PROTOCOL_UNSUPPORTED,
                        "Dart analyzer protocol " + connectedVersion
                        + " is older than required 1.40.0.");
            }

            ObjectNode roots = JSON.createObjectNode();
            roots.set("included", array(request.projectRoot().toString()));
            roots.set("excluded", JSON.createArrayNode());
            requireSuccess(session.request(
                    "analysis.setAnalysisRoots",
                    roots),
                    "Set analyzer roots");
            requireSuccess(session.request(
                    "analysis.setPriorityFiles",
                    JSON.createObjectNode()
                            .set("files", array(request.dartFile().toString()))),
                    "Set analyzer priority file");
            prioritySet = true;

            ObjectNode overlay = JSON.createObjectNode()
                    .put("type", "add")
                    .put("content", request.content())
                    .put("version", request.version());
            ObjectNode files = JSON.createObjectNode();
            files.set(request.dartFile().toString(), overlay);
            requireSuccess(session.request(
                    "analysis.updateContent",
                    JSON.createObjectNode().set("files", files)),
                    "Install candidate overlay");
            overlayAdded = true;

            Response errorsResponse = session.request(
                    "analysis.getErrors",
                    JSON.createObjectNode().put("file", request.dartFile().toString()));
            requireSuccess(errorsResponse, "Analyze candidate errors");
            diagnostics = parseDiagnostics(errorsResponse.result(), request);
            if (diagnostics.stream().anyMatch(DartCandidateDiagnostic::blocking)) {
                result = new DartCandidateAnalysisResult(
                        DartCandidateAnalysisStatus.REJECTED,
                        snapshot,
                        protocolVersion,
                        diagnostics,
                        request.symbolProbes().size(),
                        List.of(),
                        Optional.empty());
            } else {
                evidence = analyzeSymbols(session, request);
                boolean rejected = evidence.stream().anyMatch(value -> !value.accepted());
                result = new DartCandidateAnalysisResult(
                        rejected
                                ? DartCandidateAnalysisStatus.REJECTED
                                : DartCandidateAnalysisStatus.PASSED,
                        snapshot,
                        protocolVersion,
                        diagnostics,
                        request.symbolProbes().size(),
                        evidence,
                        Optional.empty());
            }
        } catch (AnalysisFailure ex) {
            result = failed(snapshot, protocolVersion, diagnostics, evidence,
                    request.symbolProbes().size(), ex);
        } catch (ProtocolFailure ex) {
            if (operation.cancelled()) {
                result = stale(snapshot, DartCandidateAnalysisIssueCode.CANCELLED,
                        "Candidate analysis was cancelled.",
                        request.symbolProbes().size());
            } else {
                result = unavailable(snapshot, protocolVersion, ex.code(), ex.getMessage(),
                        request.symbolProbes().size());
            }
        } catch (IOException | RuntimeException | LinkageError ex) {
            result = unavailable(snapshot, protocolVersion,
                    DartCandidateAnalysisIssueCode.INTERNAL_FAILURE,
                    "Candidate analyzer failed unexpectedly: " + reason(ex),
                    request.symbolProbes().size());
        } finally {
            if (session != null) {
                AnalysisFailure cleanupFailure = cleanup(
                        session, request, overlayAdded, prioritySet, operation.cancelled());
                if (cleanupFailure != null && !operation.terminal()) {
                    // The worker's result is replaced below only when it has
                    // not already been superseded by cancellation/timeout.
                    result = failed(snapshot, protocolVersion, diagnostics, evidence,
                            request.symbolProbes().size(), cleanupFailure);
                }
                session.close();
                operation.detach(session);
            }
        }
        return result;
    }

    private AnalysisFailure cleanup(
            DartAnalyzerProtocolSession session,
            DartCandidateAnalysisRequest request,
            boolean overlayAdded,
            boolean prioritySet,
            boolean cancelled) {
        if (cancelled || !session.isAlive()) {
            return null;
        }
        AnalysisFailure firstFailure = null;
        if (overlayAdded) {
            ObjectNode files = JSON.createObjectNode();
            files.set(request.dartFile().toString(),
                    JSON.createObjectNode().put("type", "remove"));
            firstFailure = cleanupRequest(session, "analysis.updateContent",
                    JSON.createObjectNode().set("files", files),
                    "Remove candidate overlay", firstFailure);
        }
        if (prioritySet) {
            firstFailure = cleanupRequest(session, "analysis.setPriorityFiles",
                    JSON.createObjectNode().set("files", JSON.createArrayNode()),
                    "Clear analyzer priority file", firstFailure);
        }
        return cleanupRequest(session, "server.shutdown", null,
                "Shut down candidate analyzer", firstFailure);
    }

    private static AnalysisFailure cleanupRequest(
            DartAnalyzerProtocolSession session,
            String method,
            JsonNode params,
            String operation,
            AnalysisFailure firstFailure) {
        if (!session.isAlive()) {
            return firstFailure;
        }
        try {
            requireSuccess(session.request(method, params), operation);
        } catch (ProtocolFailure ex) {
            if (firstFailure == null) {
                return new AnalysisFailure(
                        DartCandidateAnalysisStatus.UNAVAILABLE,
                        ex.code(),
                        "Candidate analyzer cleanup failed during " + operation + ": "
                        + ex.getMessage());
            }
        } catch (AnalysisFailure ex) {
            if (firstFailure == null) {
                return ex;
            }
        }
        return firstFailure;
    }

    private List<DartCandidateDiagnostic> parseDiagnostics(
            JsonNode result,
            DartCandidateAnalysisRequest request) throws AnalysisFailure, IOException {
        JsonNode errors = result == null ? null : result.get("errors");
        if (errors == null || !errors.isArray()) {
            throw malformed("analysis.getErrors did not return an errors array.");
        }
        if (errors.size() > limits.maxDiagnostics()) {
            throw new AnalysisFailure(
                    DartCandidateAnalysisStatus.UNAVAILABLE,
                    DartCandidateAnalysisIssueCode.DIAGNOSTIC_LIMIT,
                    "Analyzer returned " + errors.size() + " diagnostics, exceeding the "
                    + limits.maxDiagnostics() + " diagnostic limit.");
        }
        Path expectedFile = request.dartFile().toRealPath();
        List<DartCandidateDiagnostic> parsed = new ArrayList<>(errors.size());
        for (JsonNode error : errors) {
            if (!error.isObject()) {
                throw malformed("Analyzer diagnostic is not an object.");
            }
            DartCandidateDiagnosticSeverity severity;
            try {
                severity = DartCandidateDiagnosticSeverity.valueOf(
                        boundedText(error, "severity").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw malformed("Analyzer diagnostic has an unknown severity.");
            }
            String type = boundedText(error, "type");
            String message = boundedText(error, "message");
            JsonNode location = error.get("location");
            if (location == null || !location.isObject()) {
                throw malformed("Analyzer diagnostic has no location.");
            }
            Path file;
            try {
                file = Path.of(boundedText(location, "file"))
                        .toAbsolutePath().normalize();
            } catch (RuntimeException ex) {
                throw malformed("Analyzer diagnostic contains an invalid file path.");
            }
            if (!Files.isSameFile(expectedFile, file)) {
                throw malformed("Analyzer diagnostic refers to a different file.");
            }
            int offset = nonNegativeInt(location, "offset");
            int length = nonNegativeInt(location, "length");
            if ((long) offset + length > request.content().length()) {
                throw malformed("Analyzer diagnostic range is outside the candidate.");
            }
            boolean blocking = severity == DartCandidateDiagnosticSeverity.ERROR
                    || severity == DartCandidateDiagnosticSeverity.WARNING
                    && request.warningPolicy() == DartCandidateWarningPolicy.REJECT;
            parsed.add(new DartCandidateDiagnostic(
                    severity,
                    type,
                    optionalBoundedText(error, "code"),
                    message,
                    optionalBoundedText(error, "correction"),
                    optionalBoundedText(error, "url"),
                    file,
                    offset,
                    length,
                    positiveInt(location, "startLine"),
                    positiveInt(location, "startColumn"),
                    positiveInt(location, "endLine"),
                    positiveInt(location, "endColumn"),
                    blocking));
        }
        return List.copyOf(parsed);
    }

    private List<DartSymbolEvidence> analyzeSymbols(
            DartAnalyzerProtocolSession session,
            DartCandidateAnalysisRequest request)
            throws ProtocolFailure, AnalysisFailure {
        List<DartSymbolEvidence> evidence = new ArrayList<>(request.symbolProbes().size());
        for (DartSymbolProbe probe : request.symbolProbes()) {
            Response response = session.request(
                    "analysis.getNavigation",
                    JSON.createObjectNode()
                            .put("file", request.dartFile().toString())
                            .put("offset", probe.offset())
                            .put("length", probe.length()));
            requireSuccess(response, "Resolve candidate symbol " + probe.id());
            evidence.add(parseSymbolEvidence(response.result(), probe));
        }
        return List.copyOf(evidence);
    }

    private DartSymbolEvidence parseSymbolEvidence(
            JsonNode result,
            DartSymbolProbe probe) throws AnalysisFailure {
        if (result == null || !result.isObject()
                || !result.path("files").isArray()
                || !result.path("targets").isArray()
                || !result.path("regions").isArray()) {
            throw malformed("analysis.getNavigation returned an invalid result.");
        }
        JsonNode files = result.path("files");
        JsonNode targets = result.path("targets");
        JsonNode regions = result.path("regions");
        if (files.size() > limits.maxDiagnostics()
                || targets.size() > limits.maxDiagnostics()
                || regions.size() > limits.maxDiagnostics()) {
            throw new AnalysisFailure(
                    DartCandidateAnalysisStatus.UNAVAILABLE,
                    DartCandidateAnalysisIssueCode.RESPONSE_LIMIT,
                    "Analyzer navigation evidence exceeds the bounded entry limit.");
        }

        List<Integer> targetIndexes = null;
        for (JsonNode region : regions) {
            int offset = nonNegativeInt(region, "offset");
            int length = nonNegativeInt(region, "length");
            long regionEnd = (long) offset + length;
            long probeEnd = (long) probe.offset() + probe.length();
            if (offset <= probe.offset() && probeEnd <= regionEnd) {
                if (targetIndexes != null) {
                    return rejected(probe, List.of(),
                            "Multiple analyzer navigation regions cover the symbol.");
                }
                JsonNode indexes = region.get("targets");
                if (indexes == null || !indexes.isArray()) {
                    throw malformed("Analyzer navigation region has no target indexes.");
                }
                targetIndexes = new ArrayList<>(indexes.size());
                for (JsonNode index : indexes) {
                    if (!index.canConvertToInt() || index.intValue() < 0
                            || index.intValue() >= targets.size()) {
                        throw malformed("Analyzer navigation target index is invalid.");
                    }
                    targetIndexes.add(index.intValue());
                }
            }
        }
        if (targetIndexes == null || targetIndexes.isEmpty()) {
            return rejected(probe, List.of(),
                    "Analyzer did not resolve the requested symbol.");
        }

        List<DartNavigationTarget> resolved = new ArrayList<>(targetIndexes.size());
        for (int index : targetIndexes) {
            JsonNode target = targets.get(index);
            int fileIndex = nonNegativeInt(target, "fileIndex");
            if (fileIndex >= files.size() || !files.get(fileIndex).isTextual()) {
                throw malformed("Analyzer navigation target file index is invalid.");
            }
            Path file;
            try {
                file = Path.of(files.get(fileIndex).textValue())
                        .toAbsolutePath().normalize().toRealPath();
            } catch (IOException | RuntimeException ex) {
                throw new AnalysisFailure(
                        DartCandidateAnalysisStatus.UNAVAILABLE,
                        DartCandidateAnalysisIssueCode.PATH_UNAVAILABLE,
                        "Analyzer navigation target is unavailable: " + reason(ex));
            }
            resolved.add(new DartNavigationTarget(
                    boundedText(target, "kind"),
                    file,
                    nonNegativeInt(target, "offset"),
                    positiveInt(target, "length"),
                    positiveInt(target, "startLine"),
                    positiveInt(target, "startColumn")));
        }
        if (resolved.size() != 1) {
            return rejected(probe, resolved,
                    "Analyzer resolved the symbol to " + resolved.size()
                    + " targets instead of exactly one.");
        }
        DartNavigationTarget target = resolved.getFirst();
        Path expectedTargetRoot;
        try {
            expectedTargetRoot = probe.expectedTargetRoot().toRealPath();
        } catch (IOException | SecurityException ex) {
            throw new AnalysisFailure(
                    DartCandidateAnalysisStatus.UNAVAILABLE,
                    DartCandidateAnalysisIssueCode.PATH_UNAVAILABLE,
                    "Expected navigation root is unavailable: " + reason(ex));
        }
        if (!target.file().startsWith(expectedTargetRoot)) {
            return rejected(probe, resolved,
                    "Analyzer target is outside expected library root "
                    + expectedTargetRoot + ".");
        }
        if (probe.expectedTargetKind().isPresent()
                && !probe.expectedTargetKind().orElseThrow().equals(target.kind())) {
            return rejected(probe, resolved,
                    "Analyzer target kind " + target.kind() + " does not match expected "
                    + probe.expectedTargetKind().orElseThrow() + ".");
        }
        return new DartSymbolEvidence(probe, resolved, true, Optional.empty());
    }

    private static DartSymbolEvidence rejected(
            DartSymbolProbe probe,
            List<DartNavigationTarget> targets,
            String reason) {
        return new DartSymbolEvidence(probe, targets, false, Optional.of(reason));
    }

    private String boundedText(JsonNode object, String field) throws AnalysisFailure {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw malformed("Analyzer field '" + field + "' is missing or invalid.");
        }
        if (value.textValue().length() > limits.maxDiagnosticTextChars()) {
            throw new AnalysisFailure(
                    DartCandidateAnalysisStatus.UNAVAILABLE,
                    DartCandidateAnalysisIssueCode.RESPONSE_LIMIT,
                    "Analyzer field '" + field + "' exceeds the text limit.");
        }
        return value.textValue();
    }

    private Optional<String> optionalBoundedText(JsonNode object, String field)
            throws AnalysisFailure {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw malformed("Analyzer field '" + field + "' is invalid.");
        }
        if (value.textValue().length() > limits.maxDiagnosticTextChars()) {
            throw new AnalysisFailure(
                    DartCandidateAnalysisStatus.UNAVAILABLE,
                    DartCandidateAnalysisIssueCode.RESPONSE_LIMIT,
                    "Analyzer field '" + field + "' exceeds the text limit.");
        }
        return Optional.of(value.textValue());
    }

    private static int nonNegativeInt(JsonNode object, String field)
            throws AnalysisFailure {
        JsonNode value = object.get(field);
        if (value == null || !value.canConvertToInt() || value.intValue() < 0) {
            throw malformed("Analyzer integer field '" + field + "' is invalid.");
        }
        return value.intValue();
    }

    private static int positiveInt(JsonNode object, String field)
            throws AnalysisFailure {
        int value = nonNegativeInt(object, field);
        if (value == 0) {
            throw malformed("Analyzer integer field '" + field + "' must be positive.");
        }
        return value;
    }

    private static ArrayNode array(String value) {
        return JSON.createArrayNode().add(value);
    }

    private static void requireSuccess(Response response, String operation)
            throws AnalysisFailure {
        if (!response.failed()) {
            return;
        }
        String code = response.errorCode();
        String detail = response.errorMessage().isBlank()
                ? "Analyzer returned " + (code.isBlank() ? "an unknown error" : code) + "."
                : response.errorMessage();
        if ("CONTENT_MODIFIED".equals(code)) {
            throw new AnalysisFailure(
                    DartCandidateAnalysisStatus.STALE,
                    DartCandidateAnalysisIssueCode.CONTENT_MODIFIED,
                    operation + " became stale: " + detail);
        }
        throw new AnalysisFailure(
                DartCandidateAnalysisStatus.UNAVAILABLE,
                DartCandidateAnalysisIssueCode.REQUEST_FAILED,
                operation + " failed"
                + (code.isBlank() ? "" : " with " + code) + ": " + detail);
    }

    private static AnalysisFailure malformed(String message) {
        return new AnalysisFailure(
                DartCandidateAnalysisStatus.UNAVAILABLE,
                DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                message);
    }

    private static void validatePaths(DartCandidateAnalysisRequest request)
            throws IOException {
        if (!Files.isDirectory(request.projectRoot())) {
            throw new IOException("project root is not a directory");
        }
        if (!Files.isRegularFile(request.dartFile())) {
            throw new IOException("Dart source is not a regular file");
        }
        Path realRoot = request.projectRoot().toRealPath();
        Path realFile = request.dartFile().toRealPath();
        if (!realFile.startsWith(realRoot)) {
            throw new IOException("Dart source resolves outside the project root");
        }
    }

    private static DartCandidateAnalysisResult failed(
            DartCandidateSnapshot snapshot,
            Optional<String> protocolVersion,
            List<DartCandidateDiagnostic> diagnostics,
            List<DartSymbolEvidence> evidence,
            int requestedSymbolProbes,
            AnalysisFailure failure) {
        return new DartCandidateAnalysisResult(
                failure.status,
                snapshot,
                protocolVersion,
                diagnostics,
                requestedSymbolProbes,
                evidence,
                Optional.of(new DartCandidateAnalysisIssue(
                        failure.code, failure.getMessage())));
    }

    private static DartCandidateAnalysisResult unavailable(
            DartCandidateSnapshot snapshot,
            Optional<String> protocolVersion,
            DartCandidateAnalysisIssueCode code,
            String message,
            int requestedSymbolProbes) {
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.UNAVAILABLE,
                snapshot,
                protocolVersion,
                List.of(),
                requestedSymbolProbes,
                List.of(),
                Optional.of(new DartCandidateAnalysisIssue(code, message)));
    }

    private static DartCandidateAnalysisResult stale(
            DartCandidateSnapshot snapshot,
            DartCandidateAnalysisIssueCode code,
            String message,
            int requestedSymbolProbes) {
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.STALE,
                snapshot,
                Optional.empty(),
                List.of(),
                requestedSymbolProbes,
                List.of(),
                Optional.of(new DartCandidateAnalysisIssue(code, message)));
    }

    private static DartCandidateAnalysisResult timeout(
            DartCandidateSnapshot snapshot,
            int requestedSymbolProbes) {
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.TIMEOUT,
                snapshot,
                Optional.empty(),
                List.of(),
                requestedSymbolProbes,
                List.of(),
                Optional.of(new DartCandidateAnalysisIssue(
                        DartCandidateAnalysisIssueCode.TIMEOUT,
                        "Candidate analysis exceeded its bounded timeout.")));
    }

    private static String reason(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.strip();
    }

    private static Process startProcess(List<String> command, Path workingDirectory)
            throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(false);
        return builder.start();
    }

    private final class Operation implements DartCandidateAnalysisOperation {
        private final DartCandidateAnalysisRequest request;
        private final CompletableFuture<DartCandidateAnalysisResult> completion =
                new CompletableFuture<>();
        private final AtomicReference<DartAnalyzerProtocolSession> session =
                new AtomicReference<>();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private volatile ScheduledFuture<?> watchdog;

        Operation(DartCandidateAnalysisRequest request) {
            this.request = request;
        }

        void start() {
            watchdog = WATCHDOG.schedule(
                    this::timeout,
                    limits.totalTimeout().toNanos(),
                    TimeUnit.NANOSECONDS);
            Thread.ofVirtual()
                    .name("dart-candidate-analysis")
                    .start(() -> finish(execute(request, this)));
        }

        @Override
        public CompletionStage<DartCandidateAnalysisResult> result() {
            return completion.minimalCompletionStage();
        }

        @Override
        public boolean cancel() {
            if (terminal.get() || !cancelled.compareAndSet(false, true)) {
                return false;
            }
            finish(stale(request.snapshot(), DartCandidateAnalysisIssueCode.CANCELLED,
                    "Candidate analysis was cancelled.", request.symbolProbes().size()));
            stopSession();
            return true;
        }

        void attach(DartAnalyzerProtocolSession value) {
            if (!session.compareAndSet(null, value)) {
                throw new IllegalStateException("analyzer session already attached");
            }
            if (terminal.get()) {
                stopSession();
            }
        }

        void detach(DartAnalyzerProtocolSession value) {
            session.compareAndSet(value, null);
        }

        boolean cancelled() {
            return cancelled.get();
        }

        boolean terminal() {
            return terminal.get();
        }

        private void timeout() {
            if (terminal.compareAndSet(false, true)) {
                completion.complete(DartCandidateAnalyzer.timeout(
                        request.snapshot(), request.symbolProbes().size()));
                stopSession();
            }
        }

        private void finish(DartCandidateAnalysisResult value) {
            if (terminal.compareAndSet(false, true)) {
                ScheduledFuture<?> currentWatchdog = watchdog;
                if (currentWatchdog != null) {
                    currentWatchdog.cancel(false);
                }
                completion.complete(value);
            }
        }

        private void stopSession() {
            DartAnalyzerProtocolSession current = session.get();
            if (current == null) {
                return;
            }
            current.cancelOutstanding();
            Thread.ofVirtual().name("dart-candidate-analysis-stop").start(current::close);
        }
    }

    private static final class AnalysisFailure extends Exception {
        private final DartCandidateAnalysisStatus status;
        private final DartCandidateAnalysisIssueCode code;

        AnalysisFailure(
                DartCandidateAnalysisStatus status,
                DartCandidateAnalysisIssueCode code,
                String message) {
            super(message);
            this.status = Objects.requireNonNull(status, "status");
            this.code = Objects.requireNonNull(code, "code");
        }
    }

    private record ProtocolVersion(int major, int minor, int patch)
            implements Comparable<ProtocolVersion> {
        static ProtocolVersion parse(String value) throws AnalysisFailure {
            String[] parts = value.split("\\.", -1);
            if (parts.length < 2 || parts.length > 3) {
                throw malformed("Analyzer protocol version is invalid: " + value);
            }
            try {
                return new ProtocolVersion(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        parts.length == 3 ? Integer.parseInt(parts[2]) : 0);
            } catch (NumberFormatException ex) {
                throw malformed("Analyzer protocol version is invalid: " + value);
            }
        }

        @Override
        public int compareTo(ProtocolVersion other) {
            int comparison = Integer.compare(major, other.major);
            if (comparison == 0) {
                comparison = Integer.compare(minor, other.minor);
            }
            return comparison == 0 ? Integer.compare(patch, other.patch) : comparison;
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "dart-candidate-analysis-watchdog");
            thread.setDaemon(true);
            return thread;
        }
    }
}
