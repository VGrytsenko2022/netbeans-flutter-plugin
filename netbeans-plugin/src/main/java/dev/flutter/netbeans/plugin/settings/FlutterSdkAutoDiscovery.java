package dev.flutter.netbeans.plugin.settings;

import dev.flutter.netbeans.api.DartSdk;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.sdk.DartSdkLocator;
import dev.flutter.netbeans.sdk.FlutterSdkLocator;
import java.util.Optional;
import org.openide.modules.OnStart;
import org.openide.util.RequestProcessor;

@OnStart
public final class FlutterSdkAutoDiscovery implements Runnable {
    static final int DISCOVERY_VERSION = 1;
    private static final RequestProcessor DISCOVERY_TASKS = new RequestProcessor(
            FlutterSdkAutoDiscovery.class.getName(), 1, true);

    @Override
    public void run() {
        // @OnStart tasks delay IDE startup until they return. PATH may contain
        // slow network locations, so discovery continues on a daemon worker.
        DISCOVERY_TASKS.post(() -> initialize(
                FlutterSettings.getDefault(),
                new FlutterSdkLocator(),
                new DartSdkLocator()));
    }

    static void initialize(
            FlutterSettings settings,
            FlutterSdkLocator flutterLocator,
            DartSdkLocator dartLocator) {
        if (settings.discoveryVersion() >= DISCOVERY_VERSION) return;

        FlutterToolchainConfig current = settings.load();
        String flutterHome = current.flutterHome();
        String dartHome = current.dartHome();
        boolean useBundledDart = current.useBundledDart();
        boolean dartModeConfigured = settings.hasDartModeSetting();

        Optional<FlutterSdk> flutterSdk = Optional.empty();
        if (flutterHome.isBlank()) {
            flutterSdk = flutterLocator.locate();
            if (flutterSdk.isPresent()) flutterHome = flutterSdk.get().home().toString();
        } else {
            try {
                flutterSdk = flutterLocator.fromHome(java.nio.file.Path.of(flutterHome));
            } catch (java.nio.file.InvalidPathException ex) {
                flutterSdk = Optional.empty();
            }
        }

        if (!dartModeConfigured) {
            if (!dartHome.isBlank()) {
                useBundledDart = false;
            } else {
                Optional<DartSdk> detectedDart = dartLocator.detect(flutterSdk)
                        .map(detection -> detection.sdk());
                Optional<DartSdk> bundledDart = flutterSdk.flatMap(dartLocator::fromFlutterSdk);
                if (detectedDart.isPresent()) {
                    boolean bundled = bundledDart.isPresent()
                            && bundledDart.get().home().equals(detectedDart.get().home());
                    useBundledDart = bundled;
                    if (!bundled) dartHome = detectedDart.get().home().toString();
                }
            }
        }

        settings.saveDiscovered(
                new FlutterToolchainConfig(flutterHome, useBundledDart, dartHome),
                DISCOVERY_VERSION);
    }
}
