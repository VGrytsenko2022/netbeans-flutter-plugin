package dev.flutter.netbeans.designer.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialIconRegistryTest {

    @Test
    void loadsTheExactReviewedFlutter3448Catalog() {
        MaterialIconRegistry registry = MaterialIconRegistry.bundled();

        assertEquals(8_825, registry.entries().size());
        assertEquals(8_825, registry.metadata().iconCount());
        assertEquals("3.44.8", registry.metadata().flutterVersion());
        assertEquals("058e0af2c2", registry.metadata().flutterRevision());
        assertEquals("MaterialIcons", registry.metadata().fontFamily());
        assertEquals("flutter.uses-material-design: true",
                registry.metadata().projectRequirement());
        assertEquals("abc", registry.entries().getFirst().name());
        assertEquals("zoom_out_sharp", registry.entries().getLast().name());

        MaterialIconRegistry.MaterialIcon star = registry.find("star").orElseThrow();
        assertEquals(0xE5F9, star.codePoint());
        assertEquals("MaterialIcons", star.fontFamily());
        assertEquals("Icons.star", star.dartExpression());
        assertFalse(star.matchTextDirection());

        assertThrows(UnsupportedOperationException.class,
                () -> registry.entries().add(star));
    }

    @Test
    void preservesRtlMirroringMetadataExactly() {
        MaterialIconRegistry registry = MaterialIconRegistry.bundled();

        assertTrue(registry.find("arrow_back").orElseThrow().matchTextDirection());
        assertTrue(registry.find("arrow_forward").orElseThrow().matchTextDirection());
        assertFalse(registry.find("arrow_circle_down").orElseThrow().matchTextDirection());
        assertFalse(registry.find("star").orElseThrow().matchTextDirection());
    }

    @Test
    void searchIsRankedDeterministicNormalizedAndBounded() {
        MaterialIconRegistry registry = MaterialIconRegistry.bundled();

        List<String> starResults = names(registry.search("star", 6));
        assertEquals("star", starResults.getFirst());
        assertEquals(starResults, names(registry.search("star", 6)));
        assertEquals(6, starResults.size());

        List<String> arrowResults = names(registry.search("  arrow-back  ", 4));
        assertEquals("arrow_back", arrowResults.getFirst());
        assertEquals(4, arrowResults.size());

        assertEquals(List.of("abc", "abc_outlined"), names(registry.search("", 2)));
        assertThrows(IllegalArgumentException.class, () -> registry.search("star", 0));
        assertThrows(IllegalArgumentException.class,
                () -> registry.search("star", MaterialIconRegistry.MAX_SEARCH_RESULTS + 1));
        assertThrows(IllegalArgumentException.class,
                () -> registry.search("x".repeat(129), 10));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.search("star", 2).clear());

        MaterialIconRegistry rankedFixture = MaterialIconRegistry.readForTesting(
                stream(fixture(4, List.of(
                        "alpha\te001\t0",
                        "alpha_beta\te002\t0",
                        "beta_alpha\te003\t0",
                        "calpha\te004\t0"))),
                4);
        assertEquals(
                List.of("alpha", "alpha_beta", "beta_alpha", "calpha"),
                names(rankedFixture.search("alpha", 4)));
        assertEquals(
                List.of("alpha", "alpha_beta"),
                names(rankedFixture.search("alpha", 2)));
    }

    @Test
    void selectorLabelsAreNeutralAndNeverInventAccessibilityCopy() {
        MaterialIconRegistry registry = MaterialIconRegistry.bundled();

        assertEquals("Star", registry.find("star").orElseThrow().displayLabel());
        assertEquals("Star border outlined",
                registry.find("star_border_outlined").orElseThrow().displayLabel());
        assertEquals("Ten k",
                registry.find("ten_k").orElseThrow().displayLabel());
    }

    @Test
    void rejectsDuplicateAndCorruptRows() {
        String duplicate = fixture(2, List.of(
                "alpha\te001\t0",
                "alpha\te002\t1"));
        IllegalArgumentException duplicateFailure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialIconRegistry.readForTesting(stream(duplicate), 2));
        assertTrue(duplicateFailure.getMessage().contains("duplicate icon identifier"));

        String invalidCodePoint = fixture(1, List.of("alpha\t0xE001\t0"));
        IllegalArgumentException codePointFailure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialIconRegistry.readForTesting(stream(invalidCodePoint), 1));
        assertTrue(codePointFailure.getMessage().contains("invalid code point"));

        String wrongCount = fixture(2, List.of("alpha\te001\t0"));
        IllegalArgumentException countFailure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialIconRegistry.readForTesting(stream(wrongCount), 2));
        assertTrue(countFailure.getMessage().contains("declared 2 icons but contains 1"));
    }

    @Test
    void acceptsWindowsCrlfWithoutWeakeningBareCarriageReturnValidation() {
        String canonical = fixture(1, List.of("alpha\te001\t0"));
        MaterialIconRegistry crlfRegistry = MaterialIconRegistry.readForTesting(
                stream(canonical.replace("\n", "\r\n")), 1);
        assertEquals("alpha", crlfRegistry.entries().getFirst().name());

        String bareCarriageReturn = canonical.replaceFirst("\\n", "\r");
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialIconRegistry.readForTesting(stream(bareCarriageReturn), 1));
        assertTrue(failure.getMessage().contains("bare carriage return"));
    }

    private static List<String> names(
            List<MaterialIconRegistry.MaterialIcon> icons) {
        return icons.stream().map(MaterialIconRegistry.MaterialIcon::name).toList();
    }

    private static ByteArrayInputStream stream(String source) {
        return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
    }

    private static String fixture(int count, List<String> rows) {
        return String.join("\n", List.of(
                "# netbeans-flutter-material-icons-v1",
                "# flutter.version=3.44.8",
                "# flutter.revision=058e0af2c2",
                "# source.path=packages/flutter/lib/src/material/icons.dart",
                "# source.sha256=ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0",
                "# source.count=" + count,
                "# font.family=MaterialIcons",
                "# project.requirement=flutter.uses-material-design: true",
                "# columns=name\tcodePointHex\tmatchTextDirection"))
                + "\n" + String.join("\n", rows) + "\n";
    }
}
