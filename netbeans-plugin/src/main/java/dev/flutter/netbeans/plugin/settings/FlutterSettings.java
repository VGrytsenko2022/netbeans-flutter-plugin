package dev.flutter.netbeans.plugin.settings;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

public final class FlutterSettings {
    private static final Logger LOGGER = Logger.getLogger(FlutterSettings.class.getName());
    private static final String KEY_FLUTTER_HOME = "flutter.sdk.home";
    private static final String KEY_USE_BUNDLED_DART = "dart.sdk.useBundled";
    private static final String KEY_DART_HOME = "dart.sdk.home";
    private static final String KEY_DISCOVERY_VERSION = "sdk.discovery.version";

    private final Preferences preferences;

    public static FlutterSettings getDefault() {
        return DefaultHolder.INSTANCE;
    }

    FlutterSettings(Preferences preferences) {
        this.preferences = preferences;
    }

    public synchronized FlutterToolchainConfig load() {
        return new FlutterToolchainConfig(
                preferences.get(KEY_FLUTTER_HOME, ""),
                preferences.getBoolean(KEY_USE_BUNDLED_DART, true),
                preferences.get(KEY_DART_HOME, ""));
    }

    public synchronized void save(FlutterToolchainConfig config) {
        writeConfig(config);
        flush();
    }

    synchronized void saveDiscovered(FlutterToolchainConfig config, int discoveryVersion) {
        writeConfig(config);
        preferences.putInt(KEY_DISCOVERY_VERSION, discoveryVersion);
        flush();
    }

    synchronized boolean hasDartModeSetting() {
        return preferences.get(KEY_USE_BUNDLED_DART, null) != null;
    }

    public synchronized int discoveryVersion() {
        return preferences.getInt(KEY_DISCOVERY_VERSION, 0);
    }

    public synchronized void markDiscoveryVersion(int version) {
        preferences.putInt(KEY_DISCOVERY_VERSION, version);
        flush();
    }

    private void writeConfig(FlutterToolchainConfig config) {
        putPathOrRemove(KEY_FLUTTER_HOME, config.flutterHome());
        preferences.putBoolean(KEY_USE_BUNDLED_DART, config.useBundledDart());
        putPathOrRemove(KEY_DART_HOME, config.dartHome());
    }

    private void putPathOrRemove(String key, String value) {
        if (value == null || value.isBlank()) {
            preferences.remove(key);
        } else {
            preferences.put(key, normalizePath(value));
        }
    }

    private static String normalizePath(String value) {
        String cleaned = value.trim();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        try {
            return Path.of(cleaned).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException ex) {
            return cleaned;
        }
    }

    private void flush() {
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            LOGGER.log(Level.WARNING, "Could not persist Flutter SDK settings", ex);
        }
    }

    private static final class DefaultHolder {
        private static final FlutterSettings INSTANCE = new FlutterSettings(
                NbPreferences.forModule(FlutterSettings.class));

        private DefaultHolder() {
        }
    }
}
