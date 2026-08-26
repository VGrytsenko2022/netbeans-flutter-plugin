package dev.flutter.netbeans.dart;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DartCandidateAnalyzerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void validatesOverlayDiagnosticsAndExactNavigationWithoutWritingDisk() throws Exception {
        Fixture fixture = fixture(Mode.PASS, limits(Duration.ofSeconds(3), 1024 * 1024));
        String disk = "void main() {}\n";
        String candidate = "void main() { print('candidate'); }\n";
        Files.writeString(fixture.dartFile, disk, StandardCharsets.UTF_8);
        Path sdkRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        int offset = candidate.indexOf("print");
        DartSymbolProbe probe = new DartSymbolProbe(
                "dart.print",
                offset,
                "print".length(),
                "print",
                "dart:core",
                sdkRoot,
                Optional.of("FUNCTION"));

        DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                fixture,
                candidate,
                99173,
                DartCandidateWarningPolicy.ALLOW,
                List.of(probe))));

        assertEquals(DartCandidateAnalysisStatus.PASSED, result.status());
        assertEquals(Optional.of("1.40.1"), result.analyzerProtocolVersion());
        assertEquals(1, result.requestedSymbolProbes());
        assertTrue(result.symbolEvidence().getFirst().accepted());
        assertEquals("FUNCTION",
                result.symbolEvidence().getFirst().targets().getFirst().kind());
        assertEquals(disk, Files.readString(fixture.dartFile, StandardCharsets.UTF_8));
        assertEquals(List.of(
                "analysis.setAnalysisRoots",
                "analysis.setPriorityFiles",
                "analysis.updateContent",
                "analysis.getErrors",
                "analysis.getNavigation",
                "analysis.updateContent",
                "analysis.setPriorityFiles",
                "server.shutdown"), fixture.process.methods());
        JsonNode overlay = fixture.process.requests().stream()
                .filter(value -> "analysis.updateContent".equals(value.path("method").asText()))
                .findFirst().orElseThrow()
                .path("params").path("files").path(fixture.dartFile.toString());
        assertEquals("add", overlay.path("type").asText());
        assertEquals(candidate, overlay.path("content").asText());
        assertEquals(99173, overlay.path("version").asLong());
        assertExpectedCommand(fixture);
    }

    @Test
    void analyzerErrorRejectsAndStillRemovesOverlayAndPriority() throws Exception {
        Fixture fixture = fixture(Mode.ERROR, limits(Duration.ofSeconds(3), 1024 * 1024));
        String candidate = "void main() { final value = ; }\n";
        Files.writeString(fixture.dartFile, "void main() {}\n", StandardCharsets.UTF_8);

        DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                fixture, candidate, 7, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, result.status());
        assertEquals(1, result.diagnostics().size());
        assertEquals(DartCandidateDiagnosticSeverity.ERROR,
                result.diagnostics().getFirst().severity());
        assertTrue(result.diagnostics().getFirst().blocking());
        assertEquals(List.of("add", "remove"), fixture.process.overlayTypes());
        assertEquals(List.of(1, 0), fixture.process.priorityCounts());
    }

    @Test
    void warningPolicyIsExplicitlyRepresented() throws Exception {
        Fixture allowed = fixture(Mode.WARNING, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(allowed.dartFile, "void main() {}\n", StandardCharsets.UTF_8);
        String candidate = "void main() { final value = 1; }\n";

        DartCandidateAnalysisResult pass = await(allowed.analyzer.analyze(request(
                allowed, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.PASSED, pass.status());
        assertFalse(pass.diagnostics().getFirst().blocking());

        Fixture rejected = fixture(Mode.WARNING, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(rejected.dartFile, "void main() {}\n", StandardCharsets.UTF_8);
        DartCandidateAnalysisResult fail = await(rejected.analyzer.analyze(request(
                rejected, candidate, 2, DartCandidateWarningPolicy.REJECT, List.of())));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, fail.status());
        assertTrue(fail.diagnostics().getFirst().blocking());
    }

    @Test
    void contentModifiedIsStaleAndNeverPasses() throws Exception {
        Fixture fixture = fixture(
                Mode.CONTENT_MODIFIED,
                limits(Duration.ofSeconds(3), 1024 * 1024));
        String candidate = "void main() {}\n";
        Files.writeString(fixture.dartFile, candidate, StandardCharsets.UTF_8);

        DartCandidateAnalysisResult result = await(fixture.analyzer.analyze(request(
                fixture, candidate, 3, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.STALE, result.status());
        assertEquals(DartCandidateAnalysisIssueCode.CONTENT_MODIFIED,
                result.issue().orElseThrow().code());
        assertFalse(result.passed());
    }

    @Test
    void unsupportedProtocolAndMalformedOrOversizedResponseFailClosed() throws Exception {
        String candidate = "void main() {}\n";

        Fixture old = fixture(Mode.OLD_PROTOCOL, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(old.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult oldResult = await(old.analyzer.analyze(request(
                old, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));
        assertEquals(DartCandidateAnalysisIssueCode.PROTOCOL_UNSUPPORTED,
                oldResult.issue().orElseThrow().code());

        Fixture malformed = fixture(Mode.MALFORMED, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(malformed.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult malformedResult = await(malformed.analyzer.analyze(request(
                malformed, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));
        assertEquals(DartCandidateAnalysisStatus.UNAVAILABLE, malformedResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                malformedResult.issue().orElseThrow().code());

        Fixture oversized = fixture(Mode.OVERSIZED, limits(Duration.ofSeconds(3), 512));
        Files.writeString(oversized.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult oversizedResult = await(oversized.analyzer.analyze(request(
                oversized, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));
        assertEquals(DartCandidateAnalysisStatus.UNAVAILABLE, oversizedResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.RESPONSE_LIMIT,
                oversizedResult.issue().orElseThrow().code());
    }

    @Test
    void timeoutAndCancellationTerminateHungProcessWithFailClosedEvidence() throws Exception {
        String candidate = "void main() {}\n";
        Fixture timedOut = fixture(Mode.HANG, limits(Duration.ofMillis(120), 1024 * 1024));
        Files.writeString(timedOut.dartFile, candidate, StandardCharsets.UTF_8);

        DartCandidateAnalysisResult timeoutResult = await(timedOut.analyzer.analyze(request(
                timedOut, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.TIMEOUT, timeoutResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.TIMEOUT,
                timeoutResult.issue().orElseThrow().code());
        assertTrue(awaitDestroyed(timedOut.process));

        Fixture cancelled = fixture(Mode.HANG, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(cancelled.dartFile, candidate, StandardCharsets.UTF_8);
        DartCandidateAnalysisOperation operation = cancelled.analyzer.analyze(request(
                cancelled, candidate, 2, DartCandidateWarningPolicy.ALLOW, List.of()));
        assertTrue(cancelled.process.getErrorsSeen.await(2, TimeUnit.SECONDS));

        assertTrue(operation.cancel());
        DartCandidateAnalysisResult cancelledResult = await(operation);

        assertEquals(DartCandidateAnalysisStatus.STALE, cancelledResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.CANCELLED,
                cancelledResult.issue().orElseThrow().code());
        assertTrue(awaitDestroyed(cancelled.process));
    }

    @Test
    void processExitAndWrongSymbolTargetNeverPass() throws Exception {
        String candidate = "void main() { print('x'); }\n";
        Fixture exited = fixture(Mode.PROCESS_EXIT, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(exited.dartFile, candidate, StandardCharsets.UTF_8);

        DartCandidateAnalysisResult exitResult = await(exited.analyzer.analyze(request(
                exited, candidate, 1, DartCandidateWarningPolicy.ALLOW, List.of())));

        assertEquals(DartCandidateAnalysisStatus.UNAVAILABLE, exitResult.status());
        assertEquals(DartCandidateAnalysisIssueCode.PROCESS_FAILED,
                exitResult.issue().orElseThrow().code());

        Fixture wrong = fixture(Mode.WRONG_TARGET, limits(Duration.ofSeconds(3), 1024 * 1024));
        Files.writeString(wrong.dartFile, candidate, StandardCharsets.UTF_8);
        Path expected = Files.createDirectories(temporaryDirectory.resolve("expected-sdk"));
        int offset = candidate.indexOf("print");
        DartSymbolProbe probe = new DartSymbolProbe(
                "print", offset, 5, "print", "dart:core", expected,
                Optional.of("FUNCTION"));
        DartCandidateAnalysisResult wrongResult = await(wrong.analyzer.analyze(request(
                wrong, candidate, 2, DartCandidateWarningPolicy.ALLOW, List.of(probe))));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, wrongResult.status());
        assertFalse(wrongResult.symbolEvidence().getFirst().accepted());
        assertTrue(wrongResult.symbolEvidence().getFirst().rejectionReason()
                .orElseThrow().contains("outside expected library root"));
    }

    @Test
    void requestRejectsMismatchedShaAndOutOfBoundsProbe() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("validation"));
        Path file = root.resolve("main.dart");
        String content = "void main() {}\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> new DartCandidateAnalysisRequest(
                root, file, content, 1, "0".repeat(64),
                DartCandidateWarningPolicy.ALLOW, List.of()));
        DartSymbolProbe invalid = new DartSymbolProbe(
                "bad", 500, 1, "x", "dart:core", root, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new DartCandidateAnalysisRequest(
                root, file, content, 1, sha(content),
                DartCandidateWarningPolicy.ALLOW, List.of(invalid)));
    }

    @Test
    void sharedCapacityAcceptsExactBoundsAndRejectsBytesOrProbesBeforeProcess()
            throws Exception {
        String exact = "void main() {}\n";
        int exactBytes = exact.getBytes(StandardCharsets.UTF_8).length;
        DartCandidateCapacityBudget budget = new DartCandidateCapacityBudget(
                "analysis-boundary-test", exactBytes, 1, 0);
        DartCandidateAnalysisLimits limits = limits(
                Duration.ofSeconds(3), 1024 * 1024, budget);

        Fixture accepted = fixture(Mode.ERROR, limits);
        Files.writeString(accepted.dartFile, exact, StandardCharsets.UTF_8);
        DartSymbolProbe exactProbe = new DartSymbolProbe(
                "p0", 0, 1, "v", "dart:core",
                accepted.root, Optional.empty());
        DartCandidateAnalysisResult exactResult = await(
                accepted.analyzer.analyze(request(
                        accepted,
                        exact,
                        1,
                        DartCandidateWarningPolicy.ALLOW,
                        List.of(exactProbe))));
        assertFalse(exactResult.issue().map(DartCandidateAnalysisIssue::code)
                .filter(code -> code == DartCandidateAnalysisIssueCode.CANDIDATE_TOO_LARGE
                || code == DartCandidateAnalysisIssueCode.PROBE_LIMIT
                || code == DartCandidateAnalysisIssueCode.CAPACITY_POLICY_MISMATCH)
                .isPresent());
        assertTrue(accepted.factory.command != null,
                "the exact capacity boundary must reach the analyzer process");
        assertEquals(1, exactResult.requestedSymbolProbes());

        Fixture tooLarge = fixture(Mode.PASS, limits);
        String oversized = exact + "é";
        Files.writeString(tooLarge.dartFile, exact, StandardCharsets.UTF_8);
        DartCandidateAnalysisResult byteResult = await(
                tooLarge.analyzer.analyze(request(
                        tooLarge,
                        oversized,
                        2,
                        DartCandidateWarningPolicy.ALLOW,
                        List.of())));
        assertEquals(DartCandidateAnalysisIssueCode.CANDIDATE_TOO_LARGE,
                byteResult.issue().orElseThrow().code());
        assertTrue(tooLarge.factory.command == null,
                "an over-capacity candidate must not start a process");

        Fixture tooManyProbes = fixture(Mode.PASS, limits);
        Files.writeString(tooManyProbes.dartFile, exact, StandardCharsets.UTF_8);
        List<DartSymbolProbe> probes = List.of(
                new DartSymbolProbe(
                        "p0", 0, 1, "v", "dart:core",
                        tooManyProbes.root, Optional.empty()),
                new DartSymbolProbe(
                        "p1", 1, 1, "o", "dart:core",
                        tooManyProbes.root, Optional.empty()));
        DartCandidateAnalysisResult probeResult = await(
                tooManyProbes.analyzer.analyze(request(
                        tooManyProbes,
                        exact,
                        3,
                        DartCandidateWarningPolicy.ALLOW,
                        probes)));
        assertEquals(DartCandidateAnalysisIssueCode.PROBE_LIMIT,
                probeResult.issue().orElseThrow().code());
        assertTrue(tooManyProbes.factory.command == null,
                "an over-capacity probe request must not start a process");
    }

    @Test
    void valueEqualButForeignCapacityIdentityFailsBeforeProcess()
            throws Exception {
        String content = "void main() {}\n";
        int bytes = content.getBytes(StandardCharsets.UTF_8).length;
        DartCandidateCapacityBudget analyzerBudget =
                new DartCandidateCapacityBudget("identity-test", bytes, 1, 0);
        DartCandidateCapacityBudget foreignBudget =
                new DartCandidateCapacityBudget("identity-test", bytes, 1, 0);
        Fixture fixture = fixture(
                Mode.PASS,
                limits(Duration.ofSeconds(3), 1024 * 1024, analyzerBudget));
        Files.writeString(fixture.dartFile, content, StandardCharsets.UTF_8);
        DartCandidateAnalysisRequest request = new DartCandidateAnalysisRequest(
                fixture.root,
                fixture.dartFile,
                content,
                7,
                sha(content),
                DartCandidateWarningPolicy.ALLOW,
                List.of(),
                foreignBudget);

        DartCandidateAnalysisResult result = await(
                fixture.analyzer.analyze(request));

        assertEquals(DartCandidateAnalysisIssueCode.CAPACITY_POLICY_MISMATCH,
                result.issue().orElseThrow().code());
        assertTrue(fixture.factory.command == null);
    }

    private Fixture fixture(Mode mode, DartCandidateAnalysisLimits limits) throws IOException {
        Path root = Files.createDirectories(temporaryDirectory.resolve(
                "project-" + mode + '-' + System.nanoTime()));
        Path file = root.resolve("lib/main.dart");
        Files.createDirectories(file.getParent());
        Path targetRoot = Files.createDirectories(temporaryDirectory.resolve("sdk/lib"));
        Path resolvedTarget = targetRoot.resolve("core/print.dart");
        Files.createDirectories(resolvedTarget.getParent());
        Files.writeString(resolvedTarget, "void print(Object? value) {}\n",
                StandardCharsets.UTF_8);
        Path wrongTarget = targetRoot.getParent().resolve("wrong/print.dart");
        Files.createDirectories(wrongTarget.getParent());
        Files.writeString(wrongTarget, "void print(Object? value) {}\n",
                StandardCharsets.UTF_8);
        ScriptedProcess process = new ScriptedProcess(mode, targetRoot);
        RecordingFactory factory = new RecordingFactory(process);
        Path executable = temporaryDirectory.resolve("dart-sdk/bin/dart.exe").toAbsolutePath();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(
                executable, ignored -> { }, limits, factory);
        return new Fixture(
                analyzer, process, factory, root, file, executable, limits);
    }

    private static DartCandidateAnalysisLimits limits(Duration timeout, int maxJsonLine) {
        return limits(
                timeout,
                maxJsonLine,
                new DartCandidateCapacityBudget(
                        "analysis-test-default", 2 * 1024 * 1024, 16, 0));
    }

    private static DartCandidateAnalysisLimits limits(
            Duration timeout,
            int maxJsonLine,
            DartCandidateCapacityBudget budget) {
        return new DartCandidateAnalysisLimits(
                budget,
                maxJsonLine,
                32,
                4_096,
                timeout,
                Duration.ofMillis(20));
    }

    private static DartCandidateAnalysisRequest request(
            Fixture fixture,
            String content,
            long version,
            DartCandidateWarningPolicy warningPolicy,
            List<DartSymbolProbe> probes) {
        return new DartCandidateAnalysisRequest(
                fixture.root,
                fixture.dartFile,
                content,
                version,
                sha(content),
                warningPolicy,
                probes,
                fixture.limits.candidateCapacityBudget());
    }

    private static String sha(String content) {
        return DartCandidateHashes.sha256(DartCandidateHashes.strictUtf8(content));
    }

    private static DartCandidateAnalysisResult await(DartCandidateAnalysisOperation operation)
            throws Exception {
        return operation.result().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static boolean awaitDestroyed(ScriptedProcess process) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline && process.isAlive()) {
            Thread.sleep(5);
        }
        return !process.isAlive();
    }

    private static void assertExpectedCommand(Fixture fixture) {
        assertEquals(List.of(
                fixture.executable.toString(),
                "language-server",
                "--protocol=analyzer",
                "--client-id=netbeans-flutter-designer",
                "--client-version=" + DartAnalysisServer.CLIENT_VERSION),
                fixture.factory.command);
        assertEquals(fixture.root, fixture.factory.workingDirectory);
    }

    private record Fixture(
            DartCandidateAnalyzer analyzer,
            ScriptedProcess process,
            RecordingFactory factory,
            Path root,
            Path dartFile,
            Path executable,
            DartCandidateAnalysisLimits limits) {
    }

    private enum Mode {
        PASS,
        ERROR,
        WARNING,
        CONTENT_MODIFIED,
        OLD_PROTOCOL,
        MALFORMED,
        OVERSIZED,
        HANG,
        PROCESS_EXIT,
        WRONG_TARGET
    }

    private static final class RecordingFactory
            implements DartAnalyzerProtocolSession.ProcessFactory {
        private final ScriptedProcess process;
        private List<String> command;
        private Path workingDirectory;

        RecordingFactory(ScriptedProcess process) {
            this.process = process;
        }

        @Override
        public Process start(List<String> command, Path workingDirectory) {
            this.command = List.copyOf(command);
            this.workingDirectory = workingDirectory;
            process.startServer();
            return process;
        }
    }

    private static final class ScriptedProcess extends Process {
        private final Mode mode;
        private final Path targetRoot;
        private final PipedOutputStream clientInput = new PipedOutputStream();
        private final PipedInputStream serverInput;
        private final PipedInputStream clientOutput = new PipedInputStream(64 * 1024);
        private final PipedOutputStream serverOutput;
        private final List<JsonNode> requests = java.util.Collections.synchronizedList(
                new ArrayList<>());
        private final AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        private final AtomicBoolean alive = new AtomicBoolean(true);
        private final AtomicInteger destroyCalls = new AtomicInteger();
        private final CountDownLatch getErrorsSeen = new CountDownLatch(1);
        private Thread serverThread;

        ScriptedProcess(Mode mode, Path targetRoot) throws IOException {
            this.mode = mode;
            this.targetRoot = targetRoot;
            serverInput = new PipedInputStream(clientInput, 64 * 1024);
            serverOutput = new PipedOutputStream(clientOutput);
        }

        void startServer() {
            serverThread = Thread.ofVirtual().start(this::runServer);
        }

        List<JsonNode> requests() {
            synchronized (requests) {
                return requests.stream()
                        .map(value -> (JsonNode) value.deepCopy())
                        .toList();
            }
        }

        List<String> methods() {
            return requests().stream().map(value -> value.path("method").asText()).toList();
        }

        List<String> overlayTypes() {
            List<String> types = new ArrayList<>();
            for (JsonNode request : requests()) {
                if (!"analysis.updateContent".equals(request.path("method").asText())) {
                    continue;
                }
                request.path("params").path("files").properties()
                        .forEach(entry -> types.add(entry.getValue().path("type").asText()));
            }
            return types;
        }

        List<Integer> priorityCounts() {
            return requests().stream()
                    .filter(value -> "analysis.setPriorityFiles".equals(
                            value.path("method").asText()))
                    .map(value -> value.path("params").path("files").size())
                    .toList();
        }

        private void runServer() {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    serverInput, StandardCharsets.UTF_8))) {
                send(JSON.createObjectNode()
                        .put("event", "server.connected")
                        .set("params", JSON.createObjectNode().put(
                                "version", mode == Mode.OLD_PROTOCOL ? "1.39.9" : "1.40.1")));
                String line;
                while (alive.get() && (line = reader.readLine()) != null) {
                    JsonNode request = JSON.readTree(line);
                    requests.add(request.deepCopy());
                    String method = request.path("method").asText();
                    if ("analysis.getErrors".equals(method)) {
                        getErrorsSeen.countDown();
                        if (mode == Mode.HANG) {
                            continue;
                        }
                        if (mode == Mode.MALFORMED) {
                            sendRaw("{not-json\n");
                            continue;
                        }
                        if (mode == Mode.OVERSIZED) {
                            sendRaw("x".repeat(2_048) + "\n");
                            continue;
                        }
                        if (mode == Mode.PROCESS_EXIT) {
                            alive.set(false);
                            serverOutput.close();
                            return;
                        }
                        if (mode == Mode.CONTENT_MODIFIED) {
                            error(request, "CONTENT_MODIFIED", "Overlay changed.");
                            continue;
                        }
                        errors(request);
                        continue;
                    }
                    if ("analysis.getNavigation".equals(method)) {
                        navigation(request);
                        continue;
                    }
                    acknowledge(request);
                    if ("server.shutdown".equals(method)) {
                        alive.set(false);
                        serverOutput.close();
                        return;
                    }
                }
            } catch (Throwable ex) {
                if (alive.get()) {
                    serverFailure.set(ex);
                }
            } finally {
                alive.set(false);
            }
        }

        private void errors(JsonNode request) throws IOException {
            ArrayNode errors = JSON.createArrayNode();
            if (mode == Mode.ERROR || mode == Mode.WARNING) {
                boolean error = mode == Mode.ERROR;
                String file = latestDartFile();
                ObjectNode location = JSON.createObjectNode()
                        .put("file", file)
                        .put("offset", error ? 30 : 20)
                        .put("length", 1)
                        .put("startLine", 1)
                        .put("startColumn", error ? 31 : 21)
                        .put("endLine", 1)
                        .put("endColumn", error ? 32 : 22);
                errors.add(JSON.createObjectNode()
                        .put("severity", error ? "ERROR" : "WARNING")
                        .put("type", error ? "SYNTACTIC_ERROR" : "STATIC_WARNING")
                        .put("message", error ? "Expected an identifier." : "Unused local variable.")
                        .put("code", error ? "missing_identifier" : "unused_local_variable")
                        .set("location", location));
            }
            ObjectNode result = JSON.createObjectNode().set("errors", errors);
            respond(request, result);
        }

        private void navigation(JsonNode request) throws IOException {
            int requestedOffset = request.path("params").path("offset").asInt();
            int requestedLength = request.path("params").path("length").asInt();
            Path target = mode == Mode.WRONG_TARGET
                    ? targetRoot.getParent().resolve("wrong/print.dart")
                    : targetRoot.resolve("core/print.dart");
            ArrayNode files = JSON.createArrayNode().add(target.toString());
            ArrayNode targets = JSON.createArrayNode().add(JSON.createObjectNode()
                    .put("kind", "FUNCTION")
                    .put("fileIndex", 0)
                    .put("offset", 100)
                    .put("length", 5)
                    .put("startLine", 10)
                    .put("startColumn", 3));
            ArrayNode regions = JSON.createArrayNode().add(JSON.createObjectNode()
                    .put("offset", requestedOffset)
                    .put("length", requestedLength)
                    .set("targets", JSON.createArrayNode().add(0)));
            ObjectNode result = JSON.createObjectNode();
            result.set("files", files);
            result.set("targets", targets);
            result.set("regions", regions);
            respond(request, result);
        }

        private String latestDartFile() {
            return requests().stream()
                    .filter(value -> "analysis.updateContent".equals(
                            value.path("method").asText()))
                    .findFirst().orElseThrow()
                    .path("params").path("files").propertyStream()
                    .findFirst().orElseThrow().getKey();
        }

        private void acknowledge(JsonNode request) throws IOException {
            send(JSON.createObjectNode().set("id", request.get("id")));
        }

        private void respond(JsonNode request, JsonNode result) throws IOException {
            ObjectNode response = JSON.createObjectNode();
            response.set("id", request.get("id"));
            response.set("result", result);
            send(response);
        }

        private void error(JsonNode request, String code, String message) throws IOException {
            ObjectNode response = JSON.createObjectNode();
            response.set("id", request.get("id"));
            response.set("error", JSON.createObjectNode()
                    .put("code", code)
                    .put("message", message));
            send(response);
        }

        private synchronized void send(JsonNode value) throws IOException {
            sendRaw(JSON.writeValueAsString(value) + "\n");
        }

        private synchronized void sendRaw(String value) throws IOException {
            serverOutput.write(value.getBytes(StandardCharsets.UTF_8));
            serverOutput.flush();
        }

        @Override
        public OutputStream getOutputStream() {
            return clientInput;
        }

        @Override
        public InputStream getInputStream() {
            return clientOutput;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() throws InterruptedException {
            if (serverThread != null) {
                serverThread.join();
            }
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (serverThread == null) {
                return !alive.get();
            }
            serverThread.join(unit.toMillis(timeout));
            return !alive.get();
        }

        @Override
        public int exitValue() {
            if (alive.get()) {
                throw new IllegalThreadStateException("still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalls.incrementAndGet();
            destroyForcibly();
        }

        @Override
        public Process destroyForcibly() {
            alive.set(false);
            try {
                clientInput.close();
                clientOutput.close();
                serverInput.close();
                serverOutput.close();
            } catch (IOException ignored) {
            }
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive.get();
        }
    }
}
