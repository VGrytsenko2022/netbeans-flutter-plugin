package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Optional no-write candidate-overlay smoke test against an explicit real SDK. */
class DartCandidateAnalyzerRealSdkTest {
    @TempDir
    Path workspace;

    @Test
    void validatesErrorsAndNavigationWithoutChangingDisk() throws Exception {
        Path executable = configuredDartExecutable();
        Path sdkLib = executable.getParent().getParent().resolve("lib").normalize();
        assumeTrue(Files.isDirectory(sdkLib), "Dart SDK lib directory does not exist");
        Path lib = Files.createDirectories(workspace.resolve("lib"));
        Path file = lib.resolve("main.dart");
        String disk = "void main() {}\n";
        Files.writeString(file, disk, StandardCharsets.UTF_8);
        List<String> stderr = new ArrayList<>();
        DartCandidateAnalyzer analyzer = new DartCandidateAnalyzer(
                executable,
                line -> {
                    synchronized (stderr) {
                        stderr.add(line);
                    }
                });

        String broken = "void main() {\n  final value = ;\n}\n";
        DartCandidateAnalysisResult rejected = await(analyzer.analyze(request(
                file, broken, 1, List.of())));

        assertEquals(DartCandidateAnalysisStatus.REJECTED, rejected.status(),
                () -> rejected + " stderr=" + stderr);
        assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == DartCandidateDiagnosticSeverity.ERROR
                && diagnostic.blocking()));
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));

        String valid = "void main() {\n  print('overlay');\n}\n";
        int printOffset = valid.indexOf("print");
        DartSymbolProbe print = new DartSymbolProbe(
                "dart.print",
                printOffset,
                5,
                "print",
                "dart:core",
                sdkLib,
                Optional.of("FUNCTION"));
        DartCandidateAnalysisResult passed = await(analyzer.analyze(request(
                file, valid, 2, List.of(print))));

        assertEquals(DartCandidateAnalysisStatus.PASSED, passed.status(),
                () -> passed + " stderr=" + stderr);
        assertFalse(passed.symbolEvidence().isEmpty());
        assertTrue(passed.symbolEvidence().getFirst().accepted());
        assertEquals(disk, Files.readString(file, StandardCharsets.UTF_8));
    }

    private DartCandidateAnalysisRequest request(
            Path file,
            String content,
            long version,
            List<DartSymbolProbe> probes) {
        return new DartCandidateAnalysisRequest(
                workspace.toAbsolutePath().normalize(),
                file.toAbsolutePath().normalize(),
                content,
                version,
                DartCandidateHashes.sha256(DartCandidateHashes.strictUtf8(content)),
                DartCandidateWarningPolicy.ALLOW,
                probes);
    }

    private static DartCandidateAnalysisResult await(
            DartCandidateAnalysisOperation operation) throws Exception {
        return operation.result().toCompletableFuture().get(60, TimeUnit.SECONDS);
    }

    private static Path configuredDartExecutable() {
        String configured = System.getProperty("dart.executable", "").trim();
        assumeTrue(!configured.isEmpty(), "set -Ddart.executable=<path-to-dart>");
        Path executable = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isRegularFile(executable),
                "Dart executable does not exist: " + executable);
        return executable;
    }
}
