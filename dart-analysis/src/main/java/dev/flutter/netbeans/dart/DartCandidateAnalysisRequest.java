package dev.flutter.netbeans.dart;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Exact bounded candidate submitted to an isolated analyzer overlay. */
public record DartCandidateAnalysisRequest(
        Path projectRoot,
        Path dartFile,
        String content,
        long version,
        String sha256,
        DartCandidateWarningPolicy warningPolicy,
        List<DartSymbolProbe> symbolProbes,
        DartCandidateCapacityBudget candidateCapacityBudget) {

    public DartCandidateAnalysisRequest(
            Path projectRoot,
            Path dartFile,
            String content,
            long version,
            String sha256,
            DartCandidateWarningPolicy warningPolicy,
            List<DartSymbolProbe> symbolProbes) {
        this(
                projectRoot,
                dartFile,
                content,
                version,
                sha256,
                warningPolicy,
                symbolProbes,
                DartCandidateCapacityBudget.DEFAULT);
    }

    public DartCandidateAnalysisRequest {
        projectRoot = normalize(projectRoot, "projectRoot");
        dartFile = normalize(dartFile, "dartFile");
        if (!dartFile.startsWith(projectRoot)) {
            throw new IllegalArgumentException("dartFile must be inside projectRoot");
        }
        Objects.requireNonNull(content, "content");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        byte[] utf8 = DartCandidateHashes.strictUtf8(content);
        String actualSha = DartCandidateHashes.sha256(utf8);
        if (sha256 == null || !actualSha.equals(sha256)) {
            throw new IllegalArgumentException(
                    "sha256 does not identify the exact candidate content");
        }
        warningPolicy = Objects.requireNonNull(warningPolicy, "warningPolicy");
        Objects.requireNonNull(candidateCapacityBudget,
                "candidateCapacityBudget");
        symbolProbes = List.copyOf(Objects.requireNonNull(symbolProbes, "symbolProbes"));
        HashSet<String> ids = new HashSet<>();
        DartStaticTypeProbe sharedStaticTypeContext = null;
        for (DartSymbolProbe probe : symbolProbes) {
            Objects.requireNonNull(probe, "symbolProbes contains null");
            if (!ids.add(probe.id())) {
                throw new IllegalArgumentException("duplicate symbol probe id: " + probe.id());
            }
            long end = (long) probe.offset() + probe.length();
            if (end > content.length()) {
                throw new IllegalArgumentException("symbol probe is outside candidate: " + probe.id());
            }
            String selected = content.substring(probe.offset(), (int) end);
            if (!selected.equals(probe.expectedSymbolName())) {
                throw new IllegalArgumentException(
                        "symbol probe text does not match candidate: " + probe.id());
            }
            if (probe.staticTypeProbe().isPresent()) {
                DartStaticTypeProbe staticType = probe.staticTypeProbe().orElseThrow();
                if (staticType.expressionEndOffset() > content.length()) {
                    throw new IllegalArgumentException(
                            "static-type expression is outside candidate: " + probe.id());
                }
                String expression = content.substring(
                        staticType.expressionOffset(),
                        staticType.expressionEndOffset());
                if (expression.indexOf('\r') >= 0 || expression.indexOf('\n') >= 0) {
                    throw new IllegalArgumentException(
                            "static-type expression must be a single line: " + probe.id());
                }
                if (staticType.importInsertionOffset() > content.length()
                        || staticType.statementInsertionOffset() > content.length()) {
                    throw new IllegalArgumentException(
                            "static-type insertion point is outside candidate: " + probe.id());
                }
                if (sharedStaticTypeContext == null) {
                    sharedStaticTypeContext = staticType;
                } else if (sharedStaticTypeContext.importInsertionOffset()
                        != staticType.importInsertionOffset()
                        || sharedStaticTypeContext.statementInsertionOffset()
                        != staticType.statementInsertionOffset()
                        || !sharedStaticTypeContext.expectedTypeLibraryUri().equals(
                                staticType.expectedTypeLibraryUri())) {
                    throw new IllegalArgumentException(
                            "static-type probes must share one proof scope and type library");
                }
            }
        }
        if (sharedStaticTypeContext != null && version == Long.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "typed candidate version must leave room for one proof overlay");
        }
    }

    public DartCandidateSnapshot snapshot() {
        byte[] utf8 = DartCandidateHashes.strictUtf8(content);
        return new DartCandidateSnapshot(
                projectRoot, dartFile, version, sha256, utf8.length);
    }

    private static Path normalize(Path value, String name) {
        Objects.requireNonNull(value, name);
        if (!value.isAbsolute()) {
            throw new IllegalArgumentException(name + " must be absolute");
        }
        return value.normalize();
    }
}
