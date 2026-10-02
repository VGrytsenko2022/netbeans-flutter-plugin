package io.github.vgrytsenko2022.plugin.device;

import io.github.vgrytsenko2022.run.AndroidAvdCreateRequest;
import java.util.Optional;

/** Opens the AVD creation workflow after the backend has loaded available SDK choices. */
@FunctionalInterface
interface AndroidAvdCreationProvider {
    Optional<AndroidAvdCreateRequest> request(
            AndroidDeviceManagerBackend.CreationOptions options) throws Exception;
}
