package dev.flutter.netbeans.plugin.settings;

import dev.flutter.netbeans.api.DartSdk;
import dev.flutter.netbeans.api.FlutterSdk;
import java.util.Optional;

public record FlutterToolchainStatus(
        Optional<FlutterSdk> flutterSdk,
        Optional<DartSdk> dartSdk,
        String flutterMessage,
        String dartMessage,
        boolean validForSave) {

    public boolean isReady() {
        return flutterSdk.isPresent() && dartSdk.isPresent();
    }
}
