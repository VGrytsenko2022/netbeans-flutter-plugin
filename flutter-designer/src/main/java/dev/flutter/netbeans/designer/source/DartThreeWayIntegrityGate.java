package dev.flutter.netbeans.designer.source;

import dev.flutter.netbeans.designer.generation.DartGenerationDiagnostic;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegion;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure fail-closed comparison of on-disk payloads, persisted hashes and current
 * deterministic generation. It performs no I/O and never authorizes a write.
 */
public final class DartThreeWayIntegrityGate {

    private static final String SOURCE_PATH = "/source/dartFile";
    private static final String REGIONS_PATH = "/source/managedRegions";
    private static final List<String> REQUIRED_IDS = List.of(
            DartSourceIntegrityScanner.IMPORTS_REGION,
            DartSourceIntegrityScanner.BUILD_REGION);

    private final DartSourceIntegrityScanner scanner;

    public DartThreeWayIntegrityGate() {
        this(new DartSourceIntegrityScanner());
    }

    public DartThreeWayIntegrityGate(DartSourceIntegrityScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public DartThreeWayIntegrityResult evaluate(
            DartSourceIntegrityResult actual,
            DartSourceDescriptor declared,
            DartGenerationResult generation) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(declared, "declared");
        Objects.requireNonNull(generation, "generation");

        List<DartThreeWayIntegrityDiagnostic> diagnostics = new ArrayList<>();
        Optional<GeneratedDartRegions> generated = generation.generated();
        if (generated.isEmpty()) {
            diagnostics.add(generationUnavailable(generation));
        }

        Optional<SourceEvidence> evidence = validateSourceEvidence(actual, diagnostics);
        if (evidence.isEmpty() || generated.isEmpty()) {
            return result(actual, generation, List.of(), diagnostics);
        }

        GeneratedDartRegions generatedRegions = generated.orElseThrow();
        if (!validateGeneratedEvidence(generatedRegions, diagnostics)) {
            return result(actual, generation, List.of(), diagnostics);
        }

        List<DartThreeWayRegionComparison> comparisons = comparisons(
                evidence.orElseThrow(), declared, generatedRegions, diagnostics);
        validateGeneratedCandidate(
                evidence.orElseThrow(), declared, generatedRegions, diagnostics);
        return result(actual, generation, comparisons, diagnostics);
    }

    private Optional<SourceEvidence> validateSourceEvidence(
            DartSourceIntegrityResult actual,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        Optional<OriginalDartBytes> original = actual.original();
        if (original.isEmpty()) {
            return Optional.empty();
        }

        byte[] bytes = original.orElseThrow().copyBytes();
        List<DartManagedRegionSnapshot> regions = actual.regions();
        if (regions.size() != REQUIRED_IDS.size()) {
            inconsistentSourceEvidence(diagnostics,
                    "Expected exactly the imports and build source snapshots, but found "
                    + regions.size() + ".");
            return Optional.empty();
        }

        Map<String, DartManagedRegionSnapshot> byId = new LinkedHashMap<>();
        int previousEnd = -1;
        for (int index = 0; index < REQUIRED_IDS.size(); index++) {
            String expectedId = REQUIRED_IDS.get(index);
            DartManagedRegionSnapshot region = regions.get(index);
            if (!region.id().equals(expectedId)) {
                inconsistentSourceEvidence(diagnostics,
                        "Expected source region '" + expectedId + "' at position "
                        + index + ", but found '" + region.id() + "'.");
                return Optional.empty();
            }
            if (region.payloadStartByte() < previousEnd
                    || region.payloadEndByte() > bytes.length) {
                inconsistentSourceEvidence(diagnostics,
                        "Source region '" + expectedId
                        + "' has out-of-order or out-of-bounds byte offsets.");
                return Optional.empty();
            }

            String payload;
            try {
                payload = decodeStrictUtf8(
                        bytes, region.payloadStartByte(), region.payloadEndByte());
            } catch (CharacterCodingException malformed) {
                inconsistentSourceEvidence(diagnostics,
                        "Source region '" + expectedId
                        + "' byte offsets do not select strict UTF-8 payload text.");
                return Optional.empty();
            }
            String recomputed = DartManagedRegionHashing.normalizedSha256(payload);
            if (!recomputed.equals(region.normalizedSha256())) {
                inconsistentSourceEvidence(diagnostics,
                        "Source region '" + expectedId + "' records SHA-256 "
                        + region.normalizedSha256() + ", but its exact baseline bytes hash to "
                        + recomputed + ".");
                return Optional.empty();
            }
            byId.put(expectedId, region);
            previousEnd = region.payloadEndByte();
        }
        return Optional.of(new SourceEvidence(bytes, Map.copyOf(byId)));
    }

    private static boolean validateGeneratedEvidence(
            GeneratedDartRegions generated,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        return validateGeneratedRegion(generated.imports(), diagnostics)
                && validateGeneratedRegion(generated.build(), diagnostics);
    }

    private static boolean validateGeneratedRegion(
            GeneratedDartRegion region,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        String recomputedHash;
        byte[] encoded;
        try {
            recomputedHash = DartManagedRegionHashing.normalizedSha256(region.payload());
            encoded = encodeStrictUtf8(region.payload());
        } catch (IllegalArgumentException malformed) {
            inconsistentGeneratedEvidence(
                    diagnostics,
                    region.id().wireName(),
                    "Generated region contains invalid Unicode payload text.");
            return false;
        }
        if (!Arrays.equals(encoded, region.utf8Bytes())
                || encoded.length != region.utf8Size()
                || !recomputedHash.equals(region.normalizedSha256())) {
            inconsistentGeneratedEvidence(
                    diagnostics,
                    region.id().wireName(),
                    "Generated region text, UTF-8 bytes, size and normalized SHA-256 disagree.");
            return false;
        }
        return true;
    }

    private static List<DartThreeWayRegionComparison> comparisons(
            SourceEvidence evidence,
            DartSourceDescriptor declared,
            GeneratedDartRegions generated,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        List<DartThreeWayRegionComparison> comparisons = new ArrayList<>(2);
        compare(
                DartSourceIntegrityScanner.IMPORTS_REGION,
                evidence.regions().get(DartSourceIntegrityScanner.IMPORTS_REGION),
                declared.managedRegions().imports().sha256(),
                generated.imports(),
                comparisons,
                diagnostics);
        compare(
                DartSourceIntegrityScanner.BUILD_REGION,
                evidence.regions().get(DartSourceIntegrityScanner.BUILD_REGION),
                declared.managedRegions().build().sha256(),
                generated.build(),
                comparisons,
                diagnostics);
        return List.copyOf(comparisons);
    }

    private static void compare(
            String id,
            DartManagedRegionSnapshot actual,
            String declaredSha256,
            GeneratedDartRegion generated,
            List<DartThreeWayRegionComparison> comparisons,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        String generatedSha256 = generated.normalizedSha256();
        comparisons.add(new DartThreeWayRegionComparison(
                id,
                Optional.of(actual),
                declaredSha256,
                Optional.of(generatedSha256)));
        if (!declaredSha256.equals(generatedSha256)) {
            diagnostics.add(DartThreeWayIntegrityDiagnostic.region(
                    DartThreeWayIntegrityDiagnosticCode.GENERATED_REGION_HASH_MISMATCH,
                    regionPath(id) + "/sha256",
                    id,
                    "Generated region '" + id + "' has SHA-256 "
                    + generatedSha256 + ", but the .fd model records "
                    + declaredSha256 + "."));
        }
    }

    private void validateGeneratedCandidate(
            SourceEvidence evidence,
            DartSourceDescriptor declared,
            GeneratedDartRegions generated,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        DartManagedRegionSnapshot imports = evidence.regions().get(
                DartSourceIntegrityScanner.IMPORTS_REGION);
        DartManagedRegionSnapshot build = evidence.regions().get(
                DartSourceIntegrityScanner.BUILD_REGION);
        long candidateSize = (long) evidence.bytes().length
                - imports.payloadLengthBytes()
                - build.payloadLengthBytes()
                + generated.imports().utf8Size()
                + generated.build().utf8Size();
        if (candidateSize > scanner.limits().maxSourceBytes()) {
            diagnostics.add(DartThreeWayIntegrityDiagnostic.source(
                    DartThreeWayIntegrityDiagnosticCode.GENERATED_CANDIDATE_TOO_LARGE,
                    SOURCE_PATH,
                    "Generated Dart candidate size " + candidateSize
                    + " bytes exceeds the " + scanner.limits().maxSourceBytes()
                    + " byte source-integrity limit."));
            return;
        }

        byte[] candidate = replace(
                evidence.bytes(),
                build.payloadStartByte(),
                build.payloadEndByte(),
                generated.build().utf8Bytes());
        candidate = replace(
                candidate,
                imports.payloadStartByte(),
                imports.payloadEndByte(),
                generated.imports().utf8Bytes());

        DartSourceDescriptor generatedDescriptor = new DartSourceDescriptor(
                declared.dartFile(),
                declared.className(),
                declared.widgetKind(),
                declared.generatorVersion(),
                new ManagedRegions(
                        new ManagedRegion(generated.imports().normalizedSha256()),
                        new ManagedRegion(generated.build().normalizedSha256())));
        DartSourceIntegrityResult candidateIntegrity = scanner.scan(
                candidate, generatedDescriptor);
        if (candidateIntegrity.onDiskDeclaredMatch()) {
            return;
        }

        DartSourceIntegrityDiagnostic primary = candidateIntegrity.primaryDiagnostic()
                .orElse(null);
        boolean unsupported = candidateIntegrity.status()
                == DartSourceIntegrityStatus.UNSUPPORTED;
        String explanation = primary == null
                ? "The reconstructed generated Dart candidate did not pass source integrity."
                : "Generated candidate failed " + primary.code() + " at "
                + primary.path() + ": " + primary.message();
        diagnostics.add(DartThreeWayIntegrityDiagnostic.source(
                unsupported
                        ? DartThreeWayIntegrityDiagnosticCode.GENERATED_CANDIDATE_UNSUPPORTED
                        : DartThreeWayIntegrityDiagnosticCode.GENERATED_CANDIDATE_INVALID,
                SOURCE_PATH,
                explanation));
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
        System.arraycopy(
                source,
                end,
                result,
                start + replacement.length,
                source.length - end);
        return result;
    }

    private static String decodeStrictUtf8(byte[] source, int start, int end)
            throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(source, start, end - start))
                .toString();
    }

    private static byte[] encodeStrictUtf8(String source) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(source));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("source must be strict Unicode", malformed);
        }
    }

    private static DartThreeWayIntegrityResult result(
            DartSourceIntegrityResult actual,
            DartGenerationResult generation,
            List<DartThreeWayRegionComparison> comparisons,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        return new DartThreeWayIntegrityResult(
                actual, generation, comparisons, List.copyOf(diagnostics));
    }

    private static DartThreeWayIntegrityDiagnostic generationUnavailable(
            DartGenerationResult generation) {
        String explanation = generation.diagnostics().stream().findFirst()
                .map(DartThreeWayIntegrityGate::generationDiagnostic)
                .orElse("Generation did not publish both managed Dart regions.");
        return DartThreeWayIntegrityDiagnostic.source(
                DartThreeWayIntegrityDiagnosticCode.GENERATION_UNAVAILABLE,
                "/root",
                explanation);
    }

    private static String generationDiagnostic(DartGenerationDiagnostic diagnostic) {
        return "Generation did not publish managed Dart regions: "
                + diagnostic.code() + " at " + diagnostic.path()
                + ": " + diagnostic.message();
    }

    private static void inconsistentSourceEvidence(
            List<DartThreeWayIntegrityDiagnostic> diagnostics,
            String message) {
        diagnostics.add(DartThreeWayIntegrityDiagnostic.source(
                DartThreeWayIntegrityDiagnosticCode.SOURCE_EVIDENCE_INCONSISTENT,
                SOURCE_PATH,
                message));
    }

    private static void inconsistentGeneratedEvidence(
            List<DartThreeWayIntegrityDiagnostic> diagnostics,
            String id,
            String message) {
        diagnostics.add(DartThreeWayIntegrityDiagnostic.region(
                DartThreeWayIntegrityDiagnosticCode.GENERATED_EVIDENCE_INCONSISTENT,
                regionPath(id),
                id,
                message));
    }

    private static String regionPath(String id) {
        return REGIONS_PATH + "/" + id;
    }

    private record SourceEvidence(
            byte[] bytes,
            Map<String, DartManagedRegionSnapshot> regions) {
        SourceEvidence {
            bytes = bytes.clone();
            regions = Map.copyOf(regions);
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
