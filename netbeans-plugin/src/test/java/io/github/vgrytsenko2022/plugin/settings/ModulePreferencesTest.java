package io.github.vgrytsenko2022.plugin.settings;

import java.util.UUID;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModulePreferencesTest {
    private final Preferences root = Preferences.userRoot().node("/namespace-test/" + UUID.randomUUID());
    private final Preferences current = root.node("current");

    @AfterEach void cleanup() throws Exception { root.removeNode(); }

    @Test void importsKnownKeysOnceAndRetainsLegacyData() {
        Preferences old = root.node(ModulePreferences.LEGACY_PATH);
        old.put("flutter.sdk.home", "G:/sdk/flutter");
        old.putBoolean("dart.sdk.useBundled", false);
        old.put("selectedDeviceId", "windows");
        old.put("unknown", "leave alone");
        ModulePreferences.migrate(current, root);
        assertEquals("G:/sdk/flutter", current.get("flutter.sdk.home", null));
        assertFalse(current.getBoolean("dart.sdk.useBundled", true));
        assertEquals("windows", current.get("selectedDeviceId", null));
        assertNull(current.get("unknown", null));
        current.remove("flutter.sdk.home");
        ModulePreferences.migrate(current, root);
        assertNull(current.get("flutter.sdk.home", null), "Clearing a setting must not resurrect it");
        assertEquals("G:/sdk/flutter", old.get("flutter.sdk.home", null));
    }

    @Test void existingCurrentChoicesWinIncludingAutomaticSdkDiscovery() {
        Preferences old = root.node(ModulePreferences.LEGACY_PATH);
        old.put("flutter.sdk.home", "G:/old-sdk");
        old.put("selectedDeviceName", "Old desktop");
        current.putBoolean("dart.sdk.useBundled", true);
        current.put("selectedDeviceId", "android");
        ModulePreferences.migrate(current, root);
        assertNull(current.get("flutter.sdk.home", null));
        assertNull(current.get("selectedDeviceName", null));
        assertEquals("android", current.get("selectedDeviceId", null));
    }

    @Test void cleanInstallDoesNotCreateLegacyNodes() throws Exception {
        ModulePreferences.migrate(current, root);
        assertFalse(root.nodeExists(ModulePreferences.LEGACY_PATH));
    }
}
