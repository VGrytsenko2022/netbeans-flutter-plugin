package io.github.vgrytsenko2022.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModelValueObjectsTest {
    @Test
    void acceptsCanonicalStableIdsForSchemaVersionsOneThroughFive() {
        for (int version = 1; version <= 5; version++) {
            String text = "2f04ce87-876a-" + version + "f35-8a7c-2fba3e135c7e";
            assertEquals(text, StableId.parse(text).toString());
        }
        assertEquals(4, StableId.random().value().version());
    }

    @Test
    void rejectsNonCanonicalOrUnsupportedStableIds() {
        assertThrows(IllegalArgumentException.class,
                () -> StableId.parse("2F04CE87-876A-4F35-8A7C-2FBA3E135C7E"));
        assertThrows(IllegalArgumentException.class,
                () -> StableId.parse("2f04ce87-876a-0f35-8a7c-2fba3e135c7e"));
        assertThrows(IllegalArgumentException.class,
                () -> StableId.parse("2f04ce87-876a-6f35-8a7c-2fba3e135c7e"));
        assertThrows(IllegalArgumentException.class,
                () -> new StableId(UUID.fromString("2f04ce87-876a-4f35-0a7c-2fba3e135c7e")));
        assertThrows(NullPointerException.class, () -> StableId.parse(null));
    }

    @Test
    void enforcesSourceAndManagedRegionLexicalContractWithoutNormalizingText() {
        ManagedRegion region = new ManagedRegion("0123456789ABCDEF".repeat(4));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "_home_page2.dart",
                "_HomePage2",
                WidgetClassKind.STATEFUL,
                Optional.of(" "),
                new ManagedRegions(region, region));

        assertEquals("_home_page2.dart", source.dartFile());
        assertEquals(" ", source.generatorVersion().orElseThrow());
        assertEquals("stateless", WidgetClassKind.STATELESS.wireName());
        assertThrows(IllegalArgumentException.class, () -> new ManagedRegion("a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new ManagedRegion("A".repeat(63)));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSourceDescriptor("Home.dart", "Home", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSourceDescriptor("home.dart", "home", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSourceDescriptor("home.dart", "Function", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)));
        assertThrows(IllegalArgumentException.class,
                () -> new DartSourceDescriptor("home.dart", "Home", WidgetClassKind.STATELESS,
                        Optional.of("x".repeat(65)), new ManagedRegions(region, region)));
    }

    @Test
    void validatesCanvasBoundsAndNormalizesDecimalEquality() {
        CanvasPreferences first = canvas(new BigDecimal("390.00"), new BigDecimal("1.0"));
        CanvasPreferences second = canvas(new BigDecimal("390"), new BigDecimal("1.00"));

        assertEquals(first, second);
        assertEquals(new BigDecimal("3.9E+2"), first.logicalWidth().orElseThrow());
        assertEquals("portrait", CanvasOrientation.PORTRAIT.wireName());
        assertEquals("dark", DesignerThemeMode.DARK.wireName());

        assertThrows(IllegalArgumentException.class,
                () -> canvas(BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> canvas(new BigDecimal("10000.01"), BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> canvas(BigDecimal.ONE, new BigDecimal("5.01")));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasPreferences(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.of("e")));
    }

    @Test
    void countsCanvasStringLimitsInUnicodeCodePoints() {
        String eightyAstralCharacters = "😀".repeat(80);
        CanvasPreferences preferences = new CanvasPreferences(
                Optional.of(eightyAstralCharacters), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("en-US"));

        assertEquals(eightyAstralCharacters, preferences.preset().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasPreferences(Optional.of("😀".repeat(81)), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void validatesStronglyTypedNamesAndIntendedWidgetTypeLength() {
        assertEquals("A", new WidgetTypeId("A").value());
        assertEquals(255, new WidgetTypeId("A" + "a".repeat(254)).value().length());
        assertEquals("onPressed", new PropertyName("onPressed").toString());
        assertEquals("children", new SlotName("children").toString());

        assertThrows(IllegalArgumentException.class,
                () -> new WidgetTypeId("A" + "a".repeat(255)));
        assertThrows(IllegalArgumentException.class, () -> new WidgetTypeId("flutter widget"));
        assertThrows(IllegalArgumentException.class, () -> new PropertyName("bad-name"));
        assertThrows(IllegalArgumentException.class, () -> new SlotName("9child"));
    }

    private static CanvasPreferences canvas(BigDecimal width, BigDecimal textScale) {
        return new CanvasPreferences(
                Optional.of("phone"),
                Optional.of(width),
                Optional.of(new BigDecimal("10000")),
                Optional.of(new BigDecimal("10")),
                Optional.of(CanvasOrientation.PORTRAIT),
                Optional.of(DesignerThemeMode.LIGHT),
                Optional.of(textScale),
                Optional.of("en-US"));
    }
}
