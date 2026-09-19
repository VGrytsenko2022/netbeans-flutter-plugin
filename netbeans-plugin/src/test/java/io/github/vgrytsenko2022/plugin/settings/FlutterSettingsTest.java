package io.github.vgrytsenko2022.plugin.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlutterSettingsTest {
    private Preferences preferences;
    private FlutterSettings settings;

    @BeforeEach
    void setUp() {
        preferences = Preferences.userRoot().node(
                "/io/github/vgrytsenko2022/tests/" + UUID.randomUUID());
        settings = new FlutterSettings(preferences);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        preferences.removeNode();
    }

    @Test
    void savesAndLoadsToolchainConfiguration() {
        settings.save(new FlutterToolchainConfig(
                "  C:\\sdk\\flutter  ",
                false,
                "  C:\\sdk\\dart  "));
        settings.markDiscoveryVersion(7);

        FlutterToolchainConfig stored = settings.load();
        assertEquals(Path.of("C:\\sdk\\flutter").toAbsolutePath().normalize().toString(), stored.flutterHome());
        assertFalse(stored.useBundledDart());
        assertEquals(Path.of("C:\\sdk\\dart").toAbsolutePath().normalize().toString(), stored.dartHome());
        assertEquals(7, settings.discoveryVersion());
    }

    @Test
    void blankSdkLocationsRestoreAutomaticResolution() {
        settings.save(new FlutterToolchainConfig("C:\\flutter", false, "C:\\dart"));
        settings.save(new FlutterToolchainConfig(" ", true, ""));

        FlutterToolchainConfig stored = settings.load();
        assertTrue(stored.flutterHome().isBlank());
        assertTrue(stored.useBundledDart());
        assertTrue(stored.dartHome().isBlank());
    }

    @Test
    void normalizesRelativeAndQuotedSdkPathsBeforePersistence() {
        settings.save(new FlutterToolchainConfig(
                "\"..\\flutter sdk\"",
                false,
                "'..\\dart sdk'"));

        FlutterToolchainConfig stored = settings.load();
        assertEquals(Path.of("..\\flutter sdk").toAbsolutePath().normalize().toString(), stored.flutterHome());
        assertEquals(Path.of("..\\dart sdk").toAbsolutePath().normalize().toString(), stored.dartHome());
    }

    @Test
    void notifiesRegisteredListenersAfterPersistedToolchainChanges() {
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = () -> {
            assertFalse(Thread.holdsLock(settings),
                    "toolchain listeners must run outside the settings monitor");
            changes.incrementAndGet();
        };
        settings.addChangeListener(listener);

        settings.save(new FlutterToolchainConfig("C:\\flutter", true, ""));
        settings.saveDiscovered(
                new FlutterToolchainConfig("C:\\detected", true, ""), 5);
        settings.removeChangeListener(listener);
        settings.save(new FlutterToolchainConfig("C:\\other", true, ""));

        assertEquals(2, changes.get());
    }
}
