package dev.flutter.netbeans.plugin.device;

import dev.flutter.netbeans.run.AndroidAvdCreateRequest;
import java.util.Optional;

/** Opens the AVD creation workflow after the backend has loaded available SDK choices. */
@FunctionalInterface
interface AndroidAvdCreationProvider {
    Optional<AndroidAvdCreateRequest> request(
            AndroidDeviceManagerBackend.CreationOptions options) throws Exception;
}
