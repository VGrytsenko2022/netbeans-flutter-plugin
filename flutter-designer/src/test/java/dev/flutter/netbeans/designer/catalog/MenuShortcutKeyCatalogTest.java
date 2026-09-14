package dev.flutter.netbeans.designer.catalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Exact pinned key coverage, without admitting modifier triggers or invented names. */
class MenuShortcutKeyCatalogTest {
    private static final Set<String> MODIFIERS = Set.of(
            "control", "controlLeft", "controlRight", "shift", "shiftLeft", "shiftRight",
            "alt", "altLeft", "altRight", "meta", "metaLeft", "metaRight");

    @Test void immutableCompleteInventoryKeepsPortableIdsAndRejectsUnknownNames() {
        assertEquals(432, MenuShortcutKeyCatalog.entries().size());
        assertEquals(432, Set.copyOf(MenuShortcutKeyCatalog.names()).size());
        assertEquals("3.44.8", MenuShortcutKeyCatalog.SDK_VERSION);
        assertEquals("package:flutter/services.dart", MenuShortcutKeyCatalog.LIBRARY_URI);
        for (var entry : MenuShortcutKeyCatalog.entries()) {
            assertTrue(entry.keyId() >= 0 && entry.keyId() <= 9_007_199_254_740_991L);
            assertFalse(MODIFIERS.contains(entry.name()));
            assertEquals(entry, MenuShortcutKeyCatalog.find(entry.name()).orElseThrow());
        }
        for (String value : List.of("control", "shiftLeft", "altRight", "meta", "doesNotExist", "keyA; throw 1", "0x61")) {
            assertTrue(MenuShortcutKeyCatalog.find(value).isEmpty(), value);
        }
        assertThrows(UnsupportedOperationException.class, () -> MenuShortcutKeyCatalog.entries().clear());
        assertThrows(UnsupportedOperationException.class, () -> MenuShortcutKeyCatalog.names().clear());
        assertEquals(0x61L, MenuShortcutKeyCatalog.find("keyA").orElseThrow().keyId());
    }

    @Test @EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
    void inventoryExactlyMatchesEveryPinnedPublicKeyExceptTheTwelveSdkForbiddenModifiers() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk"));
        String keys = Files.readString(sdk.resolve("packages/flutter/lib/src/services/keyboard_key.g.dart"));
        var matcher = Pattern.compile("static const LogicalKeyboardKey ([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*LogicalKeyboardKey\\((0x[0-9a-fA-F]+)\\);").matcher(keys);
        var expected = new LinkedHashMap<String, Long>();
        while (matcher.find()) assertNull(expected.put(matcher.group(1), Long.parseLong(matcher.group(2).substring(2), 16)));
        assertEquals(444, expected.size(), "Re-audit when the pinned SDK adds public keys.");
        MODIFIERS.forEach(name -> assertNotNull(expected.remove(name), name));
        assertEquals(List.copyOf(expected.keySet()), MenuShortcutKeyCatalog.names());
        MenuShortcutKeyCatalog.entries().forEach(entry -> assertEquals(expected.get(entry.name()).longValue(), entry.keyId()));
        String shortcuts = Files.readString(sdk.resolve("packages/flutter/lib/src/widgets/shortcuts.dart"));
        int start = shortcuts.indexOf("const SingleActivator(");
        int end = shortcuts.indexOf("final LogicalKeyboardKey trigger;", start);
        String constructor = shortcuts.substring(start, end);
        MODIFIERS.forEach(name -> assertTrue(constructor.contains("!identical(trigger, LogicalKeyboardKey." + name + ")"), name));
        assertEquals(12, Pattern.compile("!identical\\(trigger, LogicalKeyboardKey\\.").matcher(constructor).results().count());
    }
}
