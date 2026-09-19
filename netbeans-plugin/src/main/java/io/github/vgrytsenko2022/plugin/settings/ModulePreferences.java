package io.github.vgrytsenko2022.plugin.settings;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/** One-time, non-destructive import from the pre-Maven-Central module identity. */
public final class ModulePreferences {
    private static final Logger LOGGER = Logger.getLogger(ModulePreferences.class.getName());
    static final String LEGACY_PATH = "dev/flutter/netbeans/netbeans/plugin";
    private static final String MIGRATED = "packageNamespaceMigrated";
    private static final List<List<String>> GROUPS = List.of(
            List.of("flutter.sdk.home", "dart.sdk.home", "dart.sdk.useBundled", "sdk.discovery.version"),
            List.of("selectedDeviceId", "selectedDeviceName", "selectedTargetKind"));

    private ModulePreferences() { }

    public static Preferences forModule(Class<?> owner) {
        Preferences current = NbPreferences.forModule(owner);
        migrate(current, NbPreferences.root());
        return current;
    }

    static void migrate(Preferences current, Preferences root) {
        synchronized (ModulePreferences.class) {
            if (current.getBoolean(MIGRATED, false)) {
                return;
            }
            try {
                if (root.nodeExists(LEGACY_PATH)) {
                    Preferences legacy = root.node(LEGACY_PATH);
                    for (List<String> group : GROUPS) {
                        // Existing choices (including automatic SDK discovery) win as a group.
                        if (group.stream().anyMatch(key -> current.get(key, null) != null)) {
                            continue;
                        }
                        for (String key : group) {
                            String value = legacy.get(key, null);
                            if (value != null) {
                                current.put(key, value);
                            }
                        }
                    }
                }
                current.putBoolean(MIGRATED, true);
                current.flush();
            } catch (BackingStoreException | SecurityException ex) {
                LOGGER.log(Level.WARNING, "Could not import legacy Flutter module preferences into "
                        + current.absolutePath(), ex);
            }
        }
    }
}
