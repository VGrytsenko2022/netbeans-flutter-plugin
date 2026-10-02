package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Optional read-only smoke test against a real Android SDK. */
class AndroidSdkAvdRealSdkTest {
    @Test
    void readsRealSdkInventoriesWithoutMutatingIt() throws Exception {
        String configured = System.getProperty("android.sdk.integration", "").strip();
        assumeTrue(!configured.isEmpty(),
                "set -Dandroid.sdk.integration=<path-to-android-sdk>");
        Path root = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isDirectory(root), "Android SDK does not exist: " + root);
        AndroidSdkInstallation installation = new AndroidSdkDiscovery()
                .detect(root).orElseThrow();
        assumeTrue(installation.complete(),
                "Android SDK is missing tools: " + installation.missingTools());

        try (AndroidSdkAvdService service = new AndroidSdkAvdService(installation)) {
            assertNotNull(service.listAvds());
            assertNotNull(service.listConnectedDevices());
            assertFalse(service.listDeviceDefinitions().isEmpty());
            assertTrue(service.listSystemImages().stream().anyMatch(AndroidSystemImage::installed));
        }
    }
}
