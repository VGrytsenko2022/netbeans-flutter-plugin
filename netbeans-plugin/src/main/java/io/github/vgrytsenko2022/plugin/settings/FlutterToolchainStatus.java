package io.github.vgrytsenko2022.plugin.settings;

import io.github.vgrytsenko2022.api.DartSdk;
import io.github.vgrytsenko2022.api.FlutterSdk;
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
