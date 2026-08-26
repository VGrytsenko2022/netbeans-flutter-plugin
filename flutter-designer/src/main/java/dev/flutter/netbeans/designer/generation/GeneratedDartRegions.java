package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Complete atomic output of one deterministic Dart generation pass. */
public record GeneratedDartRegions(
        GeneratedDartRegion imports,
        GeneratedDartRegion build,
        DartImportPlan importPlan,
        String profileId,
        List<GeneratedDartSymbolOccurrence> symbolOccurrences,
        DartCandidateCapacityBudget candidateCapacityBudget) {

    public GeneratedDartRegions(
            GeneratedDartRegion imports,
            GeneratedDartRegion build,
            DartImportPlan importPlan,
            String profileId) {
        this(imports, build, importPlan, profileId, List.of(),
                DartCandidateCapacityBudget.DEFAULT);
    }

    public GeneratedDartRegions(
            GeneratedDartRegion imports,
            GeneratedDartRegion build,
            DartImportPlan importPlan,
            String profileId,
            List<GeneratedDartSymbolOccurrence> symbolOccurrences) {
        this(imports, build, importPlan, profileId, symbolOccurrences,
                DartCandidateCapacityBudget.DEFAULT);
    }

    public GeneratedDartRegions {
        Objects.requireNonNull(imports, "imports");
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(importPlan, "importPlan");
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        if (imports.id() != DartManagedRegionId.IMPORTS
                || build.id() != DartManagedRegionId.BUILD) {
            throw new IllegalArgumentException("Generated regions use incorrect ids");
        }
        symbolOccurrences = List.copyOf(Objects.requireNonNull(
                symbolOccurrences, "symbolOccurrences"));
        Objects.requireNonNull(candidateCapacityBudget,
                "candidateCapacityBudget");
        if (symbolOccurrences.size()
                > candidateCapacityBudget.maxGeneratedSymbolOccurrences()) {
            throw new IllegalArgumentException(
                    "Generated Dart symbol occurrences exceed shared capacity profile "
                    + candidateCapacityBudget.profileId());
        }
        Set<String> ids = new HashSet<>();
        GeneratedDartSymbolOccurrence previous = null;
        Comparator<GeneratedDartSymbolOccurrence> order = Comparator
                .comparing(GeneratedDartSymbolOccurrence::region)
                .thenComparingInt(GeneratedDartSymbolOccurrence::offset)
                .thenComparing(GeneratedDartSymbolOccurrence::id);
        for (GeneratedDartSymbolOccurrence occurrence : symbolOccurrences) {
            Objects.requireNonNull(occurrence, "symbolOccurrences contains null");
            if (!ids.add(occurrence.id())) {
                throw new IllegalArgumentException(
                        "Duplicate generated Dart symbol occurrence id: " + occurrence.id());
            }
            if (previous != null && order.compare(previous, occurrence) >= 0) {
                throw new IllegalArgumentException(
                        "Generated Dart symbol occurrences must be strictly source ordered");
            }
            String payload = switch (occurrence.region()) {
                case IMPORTS -> imports.payload();
                case BUILD -> build.payload();
            };
            if (occurrence.endOffset() > payload.length()
                    || !payload.substring(occurrence.offset(), occurrence.endOffset())
                            .equals(occurrence.symbolName())) {
                throw new IllegalArgumentException(
                        "Generated Dart symbol occurrence does not identify its payload text: "
                        + occurrence.id());
            }
            previous = occurrence;
        }
    }

    public int totalUtf8Size() {
        return Math.addExact(imports.utf8Size(), build.utf8Size());
    }
}
