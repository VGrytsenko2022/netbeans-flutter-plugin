package dev.flutter.netbeans.designer.transition;

import dev.flutter.netbeans.designer.generation.DartGenerationDiagnostic;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegion;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartManagedRegionSnapshot;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.source.OriginalDartBytes;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Builds a bounded prospective transition from a proven old baseline. */
public final class DartSourceTransitionPlanner {
    private static final String SOURCE_PATH = "/source/dartFile";
    private final DartSourceIntegrityScanner scanner;

    public DartSourceTransitionPlanner() {
        this(new DartSourceIntegrityScanner());
    }

    public DartSourceTransitionPlanner(DartSourceIntegrityScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public DartSourceTransitionResult plan(
            DartThreeWayIntegrityResult baseline,
            byte[] liveSourceBytes,
            DartSourceDescriptor baselineDescriptor,
            DartGenerationResult prospectiveGeneration) {
        return planInternal(baseline, liveSourceBytes, baselineDescriptor,
                prospectiveGeneration, Optional.empty());
    }

    /** Includes only the bounded, retained user-member contribution in the candidate. */
    public DartSourceTransitionResult plan(
            DartThreeWayIntegrityResult baseline,
            byte[] liveSourceBytes,
            DartSourceDescriptor baselineDescriptor,
            DartGenerationResult prospectiveGeneration,
            DartUserSourceProjection userSourceProjection) {
        return planInternal(baseline, liveSourceBytes, baselineDescriptor,
                prospectiveGeneration, Optional.of(Objects.requireNonNull(
                        userSourceProjection, "userSourceProjection")));
    }

    private DartSourceTransitionResult planInternal(
            DartThreeWayIntegrityResult baseline,
            byte[] liveSourceBytes,
            DartSourceDescriptor baselineDescriptor,
            DartGenerationResult prospectiveGeneration,
            Optional<DartUserSourceProjection> projection) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(liveSourceBytes, "liveSourceBytes");
        Objects.requireNonNull(baselineDescriptor, "baselineDescriptor");
        Objects.requireNonNull(prospectiveGeneration, "prospectiveGeneration");

        DartSourceIntegrityResult live = scanner.scan(
                liveSourceBytes, baselineDescriptor);
        if (!baseline.onDiskThreeWayMatch()) {
            return failure(
                    statusForBaseline(baseline),
                    baseline,
                    live,
                    prospectiveGeneration,
                    DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode
                                    .BASELINE_NOT_THREE_WAY_MATCH,
                            SOURCE_PATH,
                            "The loaded Dart/.fd baseline is not an on-disk "
                            + "actual/declared/generated match."));
        }
        if (!baselineMatchesDescriptor(baseline, baselineDescriptor)) {
            return failure(
                    DartSourceTransitionStatus.CONFLICT,
                    baseline,
                    live,
                    prospectiveGeneration,
                    DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode
                                    .BASELINE_DESCRIPTOR_MISMATCH,
                            "/source",
                            "The transition descriptor does not retain the exact "
                            + "managed hashes proven by the loaded baseline."));
        }
        Optional<DartSourceTransitionDiagnostic> writableFormat =
                writableFormatDiagnostic(liveSourceBytes);
        if (writableFormat.isPresent()) {
            return failure(
                    DartSourceTransitionStatus.UNSUPPORTED,
                    baseline,
                    live,
                    prospectiveGeneration,
                    writableFormat.orElseThrow());
        }
        if (!live.onDiskDeclaredMatch()) {
            return failure(
                    statusForLive(live),
                    baseline,
                    live,
                    prospectiveGeneration,
                    liveDiagnostic(live));
        }
        if (prospectiveGeneration.generated().isEmpty()) {
            return failure(
                    generationUnavailableStatus(prospectiveGeneration),
                    baseline,
                    live,
                    prospectiveGeneration,
                    generationUnavailable(prospectiveGeneration));
        }

        GeneratedDartRegions generated = prospectiveGeneration.generated().orElseThrow();
        if (!consistent(generated.imports()) || !consistent(generated.build())) {
            return failure(
                    DartSourceTransitionStatus.CONFLICT,
                    baseline,
                    live,
                    prospectiveGeneration,
                    DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode
                                    .GENERATION_EVIDENCE_INCONSISTENT,
                            "/root",
                            "Generated Dart text, UTF-8 bytes, size and normalized "
                            + "SHA-256 do not agree."));
        }
        final DartUserSourceProjection exactProjection;
        final byte[] userSourceBytes;
        try {
            exactProjection = projection.isPresent()
                    ? projection.orElseThrow().rebaseOnto(liveSourceBytes, baselineDescriptor)
                    : DartUserSourceProjection.identity(liveSourceBytes, baselineDescriptor);
            userSourceBytes = exactProjection.applyTo(liveSourceBytes, baselineDescriptor);
        } catch (IllegalArgumentException invalidProjection) {
            return failure(DartSourceTransitionStatus.CONFLICT, baseline, live,
                    prospectiveGeneration, DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode.USER_SOURCE_PROJECTION_CONFLICT,
                            SOURCE_PATH, invalidProjection.getMessage()));
        }
        if (!hashesChanged(baselineDescriptor, generated)
                && Arrays.equals(liveSourceBytes, userSourceBytes)) {
            return new DartSourceTransitionResult(
                    DartSourceTransitionStatus.NO_CHANGES,
                    baseline,
                    live,
                    prospectiveGeneration,
                    Optional.empty(),
                    List.of());
        }

        DartSourceDescriptor prospectiveDescriptor = descriptorWithGeneratedHashes(
                baselineDescriptor, generated);
        DartSourceIntegrityResult projectedLive = scanner.scan(userSourceBytes, baselineDescriptor);
        if (!projectedLive.onDiskDeclaredMatch()) {
            return failure(statusForCandidate(projectedLive), baseline, live,
                    prospectiveGeneration, candidateDiagnostic(projectedLive));
        }
        OptionalLong candidateSize = candidateSize(projectedLive, generated);
        if (candidateSize.isEmpty()) {
            return failure(
                    DartSourceTransitionStatus.CONFLICT,
                    baseline,
                    live,
                    prospectiveGeneration,
                    DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode
                                    .GENERATED_CANDIDATE_INVALID,
                            SOURCE_PATH,
                            "The exact live managed-region evidence cannot be "
                            + "reconstructed atomically."));
        }
        if (candidateSize.orElseThrow() > scanner.limits().maxSourceBytes()) {
            return failure(
                    DartSourceTransitionStatus.UNAVAILABLE,
                    baseline,
                    live,
                    prospectiveGeneration,
                    DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode
                                    .GENERATED_CANDIDATE_TOO_LARGE,
                            SOURCE_PATH,
                            "Generated Dart candidate size "
                            + candidateSize.orElseThrow() + " bytes exceeds the "
                            + scanner.limits().maxSourceBytes()
                            + " byte writable-source limit."));
        }
        Optional<byte[]> candidate = candidate(projectedLive, generated);
        if (candidate.isEmpty()) {
            return failure(
                    DartSourceTransitionStatus.CONFLICT,
                    baseline,
                    live,
                    prospectiveGeneration,
                    DartSourceTransitionDiagnostic.source(
                            DartSourceTransitionDiagnosticCode
                                    .GENERATED_CANDIDATE_INVALID,
                            SOURCE_PATH,
                            "The exact live managed-region evidence cannot be "
                            + "reconstructed atomically."));
        }
        byte[] candidateBytes = candidate.orElseThrow();
        DartSourceIntegrityResult candidateIntegrity = scanner.scan(
                candidateBytes, prospectiveDescriptor);
        if (!candidateIntegrity.onDiskDeclaredMatch()) {
            return failure(
                    statusForCandidate(candidateIntegrity),
                    baseline,
                    live,
                    prospectiveGeneration,
                    candidateDiagnostic(candidateIntegrity));
        }
        OriginalDartBytes candidateSnapshot = candidateIntegrity.original().orElseThrow();
        DartSourceTransitionPlan plan = new DartSourceTransitionPlan(
                baseline,
                live,
                prospectiveGeneration,
                prospectiveDescriptor,
                candidateSnapshot,
                candidateIntegrity,
                exactProjection,
                userSourceBytes,
                baselineDescriptor);
        return new DartSourceTransitionResult(
                DartSourceTransitionStatus.READY,
                baseline,
                live,
                prospectiveGeneration,
                Optional.of(plan),
                List.of());
    }

    private static boolean baselineMatchesDescriptor(
            DartThreeWayIntegrityResult baseline,
            DartSourceDescriptor descriptor) {
        return baseline.comparison(DartSourceIntegrityScanner.IMPORTS_REGION)
                        .filter(comparison -> comparison.declaredNormalizedSha256().equals(
                            descriptor.managedRegions().imports().sha256()))
                        .isPresent()
                && baseline.comparison(DartSourceIntegrityScanner.BUILD_REGION)
                        .filter(comparison -> comparison.declaredNormalizedSha256().equals(
                            descriptor.managedRegions().build().sha256()))
                        .isPresent();
    }

    private static Optional<DartSourceTransitionDiagnostic> writableFormatDiagnostic(
            byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return Optional.of(DartSourceTransitionDiagnostic.source(
                    DartSourceTransitionDiagnosticCode.WRITABLE_SOURCE_BOM_UNSUPPORTED,
                    SOURCE_PATH,
                    "Writable Flutter Designer Dart currently requires UTF-8 "
                    + "without a byte-order mark."));
        }
        for (byte value : bytes) {
            if (value == '\r') {
                return Optional.of(DartSourceTransitionDiagnostic.source(
                        DartSourceTransitionDiagnosticCode
                                .WRITABLE_SOURCE_EOL_UNSUPPORTED,
                        SOURCE_PATH,
                        "Writable Flutter Designer Dart currently requires LF-only "
                        + "line endings."));
            }
        }
        return Optional.empty();
    }

    private static boolean hashesChanged(
            DartSourceDescriptor baseline,
            GeneratedDartRegions generated) {
        return !baseline.managedRegions().imports().sha256()
                        .equals(generated.imports().normalizedSha256())
                || !baseline.managedRegions().build().sha256()
                        .equals(generated.build().normalizedSha256());
    }

    private static DartSourceDescriptor descriptorWithGeneratedHashes(
            DartSourceDescriptor baseline,
            GeneratedDartRegions generated) {
        return new DartSourceDescriptor(
                baseline.dartFile(),
                baseline.className(),
                baseline.widgetKind(),
                baseline.generatorVersion(),
                new ManagedRegions(
                        new ManagedRegion(generated.imports().normalizedSha256()),
                        new ManagedRegion(generated.build().normalizedSha256())));
    }

    private static Optional<byte[]> candidate(
            DartSourceIntegrityResult live,
            GeneratedDartRegions generated) {
        if (live.original().isEmpty() || live.regions().size() != 2) {
            return Optional.empty();
        }
        DartManagedRegionSnapshot imports = live.regions().get(0);
        DartManagedRegionSnapshot build = live.regions().get(1);
        if (!imports.id().equals(DartSourceIntegrityScanner.IMPORTS_REGION)
                || !build.id().equals(DartSourceIntegrityScanner.BUILD_REGION)
                || imports.payloadEndByte() > build.payloadStartByte()) {
            return Optional.empty();
        }
        byte[] original = live.original().orElseThrow().copyBytes();
        byte[] replaced = replace(
                original,
                build.payloadStartByte(),
                build.payloadEndByte(),
                generated.build().utf8Bytes());
        replaced = replace(
                replaced,
                imports.payloadStartByte(),
                imports.payloadEndByte(),
                generated.imports().utf8Bytes());
        return Optional.of(replaced);
    }

    private static OptionalLong candidateSize(
            DartSourceIntegrityResult live,
            GeneratedDartRegions generated) {
        if (live.original().isEmpty() || live.regions().size() != 2) {
            return OptionalLong.empty();
        }
        DartManagedRegionSnapshot imports = live.regions().get(0);
        DartManagedRegionSnapshot build = live.regions().get(1);
        if (!imports.id().equals(DartSourceIntegrityScanner.IMPORTS_REGION)
                || !build.id().equals(DartSourceIntegrityScanner.BUILD_REGION)
                || imports.payloadEndByte() > build.payloadStartByte()) {
            return OptionalLong.empty();
        }
        long length = (long) live.original().orElseThrow().size()
                - imports.payloadLengthBytes()
                - build.payloadLengthBytes()
                + generated.imports().utf8Size()
                + generated.build().utf8Size();
        return length < 0 || length > Integer.MAX_VALUE
                ? OptionalLong.empty()
                : OptionalLong.of(length);
    }

    private static byte[] replace(
            byte[] source,
            int start,
            int end,
            byte[] replacement) {
        int resultLength = Math.addExact(
                Math.subtractExact(source.length, end - start), replacement.length);
        byte[] result = new byte[resultLength];
        System.arraycopy(source, 0, result, 0, start);
        System.arraycopy(replacement, 0, result, start, replacement.length);
        System.arraycopy(source, end, result, start + replacement.length,
                source.length - end);
        return result;
    }

    private static boolean consistent(GeneratedDartRegion region) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(region.payload()));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return Arrays.equals(bytes, region.utf8Bytes())
                    && bytes.length == region.utf8Size()
                    && DartManagedRegionHashing.normalizedSha256(region.payload())
                            .equals(region.normalizedSha256());
        } catch (CharacterCodingException | IllegalArgumentException malformed) {
            return false;
        }
    }

    private static DartSourceTransitionResult failure(
            DartSourceTransitionStatus status,
            DartThreeWayIntegrityResult baseline,
            DartSourceIntegrityResult live,
            DartGenerationResult generation,
            DartSourceTransitionDiagnostic diagnostic) {
        return new DartSourceTransitionResult(
                status,
                baseline,
                live,
                generation,
                Optional.empty(),
                List.of(diagnostic));
    }

    private static DartSourceTransitionStatus statusForBaseline(
            DartThreeWayIntegrityResult baseline) {
        return switch (baseline.status()) {
            case UNSUPPORTED -> DartSourceTransitionStatus.UNSUPPORTED;
            case UNAVAILABLE -> DartSourceTransitionStatus.UNAVAILABLE;
            case CONFLICT, ON_DISK_THREE_WAY_MATCH ->
                DartSourceTransitionStatus.CONFLICT;
        };
    }

    private static DartSourceTransitionStatus statusForLive(
            DartSourceIntegrityResult live) {
        return switch (live.status()) {
            case UNSUPPORTED -> DartSourceTransitionStatus.UNSUPPORTED;
            case UNAVAILABLE -> DartSourceTransitionStatus.UNAVAILABLE;
            case CONFLICT, ON_DISK_DECLARED_MATCH -> DartSourceTransitionStatus.CONFLICT;
        };
    }

    private static DartSourceTransitionStatus statusForCandidate(
            DartSourceIntegrityResult candidate) {
        return switch (candidate.status()) {
            case UNSUPPORTED -> DartSourceTransitionStatus.UNSUPPORTED;
            case UNAVAILABLE -> DartSourceTransitionStatus.UNAVAILABLE;
            case CONFLICT, ON_DISK_DECLARED_MATCH -> DartSourceTransitionStatus.CONFLICT;
        };
    }

    private static DartSourceTransitionDiagnostic liveDiagnostic(
            DartSourceIntegrityResult live) {
        DartSourceIntegrityDiagnostic primary = live.primaryDiagnostic().orElse(null);
        DartSourceTransitionDiagnosticCode code = switch (live.status()) {
            case UNSUPPORTED -> DartSourceTransitionDiagnosticCode.LIVE_SOURCE_UNSUPPORTED;
            case UNAVAILABLE -> DartSourceTransitionDiagnosticCode.LIVE_SOURCE_UNAVAILABLE;
            case CONFLICT, ON_DISK_DECLARED_MATCH ->
                DartSourceTransitionDiagnosticCode.LIVE_SOURCE_CONFLICT;
        };
        String detail = primary == null
                ? "The live Dart source does not match the loaded managed-region baseline."
                : "Live source failed " + primary.code() + " at " + primary.path()
                + ": " + primary.message();
        return DartSourceTransitionDiagnostic.source(code, SOURCE_PATH, detail);
    }

    private static DartSourceTransitionDiagnostic candidateDiagnostic(
            DartSourceIntegrityResult candidate) {
        DartSourceIntegrityDiagnostic primary = candidate.primaryDiagnostic().orElse(null);
        String detail = primary == null
                ? "The reconstructed prospective Dart candidate failed source integrity."
                : "Generated candidate failed " + primary.code() + " at "
                + primary.path() + ": " + primary.message();
        return DartSourceTransitionDiagnostic.source(
                DartSourceTransitionDiagnosticCode.GENERATED_CANDIDATE_INVALID,
                SOURCE_PATH,
                detail);
    }

    private static DartSourceTransitionStatus generationUnavailableStatus(
            DartGenerationResult generation) {
        boolean unsupported = generation.diagnostics().stream().anyMatch(diagnostic ->
            switch (diagnostic.code()) {
                case UNSUPPORTED_WIDGET_KIND, DART_EXPRESSION_UNSUPPORTED -> true;
                case MODEL_INVALID,
                        INVALID_UNICODE,
                        IMPORT_LIMIT,
                        OUTPUT_SIZE_LIMIT,
                        SYMBOL_PROBE_LIMIT,
                        INTERNAL_CATALOG_INCONSISTENCY -> false;
            });
        return unsupported
                ? DartSourceTransitionStatus.UNSUPPORTED
                : DartSourceTransitionStatus.UNAVAILABLE;
    }

    private static DartSourceTransitionDiagnostic generationUnavailable(
            DartGenerationResult generation) {
        DartGenerationDiagnostic primary = generation.diagnostics().stream()
                .findFirst().orElse(null);
        String detail = primary == null
                ? "Prospective generation did not publish both managed regions."
                : "Prospective generation failed " + primary.code() + " at "
                + primary.path() + ": " + primary.message();
        return DartSourceTransitionDiagnostic.source(
                DartSourceTransitionDiagnosticCode.GENERATION_UNAVAILABLE,
                "/root",
                detail);
    }
}
