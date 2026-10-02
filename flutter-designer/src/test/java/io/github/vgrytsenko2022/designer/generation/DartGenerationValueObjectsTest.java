package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.api.DartCandidateCapacityBudget;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

class DartGenerationValueObjectsTest {

    @Test
    void expandedDefaultKeepsIdentityBytesReservationAndExplicitSmallerBudgets() {
        var defaults = DartCandidateCapacityBudget.DEFAULT;
        assertEquals("fd-dart-candidate-capacity-v1", defaults.profileId());
        assertEquals(2 * 1024 * 1024, defaults.maxCandidateUtf8Bytes());
        assertEquals(2048, defaults.maxSymbolProbes());
        assertEquals(1, defaults.reservedSourceSymbolProbes());
        assertEquals(2047, defaults.maxGeneratedSymbolOccurrences());
        var strict = new DartCandidateCapacityBudget("explicit-legacy-budget", 4096, 256, 1);
        assertEquals(255, strict.maxGeneratedSymbolOccurrences());
        assertSame(defaults, DartGenerationLimits.defaults().candidateCapacityBudget());
    }

    @Test
    void limitsMustBePositiveAndStayBelowSourceScannerMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> new DartGenerationLimits(0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new DartGenerationLimits(1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new DartGenerationLimits(1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new DartGenerationLimits(
                        DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES, 1, 1));
        assertEquals(
                DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES - 1,
                DartGenerationLimits.MAX_SUPPORTED_PAYLOAD_UTF8_BYTES);
        assertSame(DartCandidateCapacityBudget.DEFAULT,
                DartGenerationLimits.defaults().candidateCapacityBudget());
        assertThrows(IllegalArgumentException.class,
                () -> new DartCandidateCapacityBudget("", 1, 2, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new DartCandidateCapacityBudget("test", 0, 2, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new DartCandidateCapacityBudget("test", 1, 1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new DartGenerationLimits(
                        1,
                        1,
                        1,
                        new DartCandidateCapacityBudget("test", 2, 3, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new DartGenerationLimits(
                        1,
                        1,
                        1,
                        new DartCandidateCapacityBudget(
                                "missing-root-constructor", 2, 3, 1)));
    }

    @Test
    void importPlanIsDefensiveOrderedAndPrefixUnique() {
        ArrayList<DartImportDirective> input = new ArrayList<>(List.of(
                new DartImportDirective("package:a/a.dart", Optional.empty()),
                new DartImportDirective("package:b/b.dart", Optional.of("_b"))));
        DartImportPlan plan = new DartImportPlan(input);
        input.clear();

        assertEquals(2, plan.directives().size());
        assertThrows(UnsupportedOperationException.class,
                () -> plan.directives().add(
                        new DartImportDirective("package:c/c.dart", Optional.empty())));
        assertThrows(IllegalArgumentException.class, () -> new DartImportPlan(List.of(
                new DartImportDirective("package:b/b.dart", Optional.empty()),
                new DartImportDirective("package:a/a.dart", Optional.empty()))));
        assertThrows(IllegalArgumentException.class, () -> new DartImportPlan(List.of(
                new DartImportDirective("package:a/a.dart", Optional.of("_same")),
                new DartImportDirective("package:b/b.dart", Optional.of("_same")))));
    }
}
