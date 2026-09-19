package io.github.vgrytsenko2022.plugin.device;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.run.AndroidSystemImage;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CreateAndroidAvdPanelTest {
    private static final AndroidSystemImage IMAGE = new AndroidSystemImage(
            "system-images;android-35;google_apis;x86_64",
            "35",
            "google_apis",
            "x86_64",
            "Google APIs Intel x86_64 Atom System Image",
            "1",
            true);

    @Test
    void acceptsNewNameAndOptionalHardwareValues() {
        assertNull(CreateAndroidAvdPanel.validationError(
                "Pixel_8_API_35",
                IMAGE,
                Optional.of("pixel_8"),
                "512M",
                Set.of("existing_avd")));
    }

    @Test
    void rejectsDuplicateNameCaseInsensitively() {
        String error = CreateAndroidAvdPanel.validationError(
                "Pixel_8_API_35",
                IMAGE,
                Optional.empty(),
                "",
                Set.of("pixel_8_api_35"));

        assertTrue(error.contains("already exists"));
        assertTrue(error.contains("Pixel_8_API_35"));
    }

    @Test
    void rejectsUnsafeNameAndInvalidSdCardSize() {
        String unsafeName = CreateAndroidAvdPanel.validationError(
                "Pixel 8",
                IMAGE,
                Optional.empty(),
                "",
                Set.of());
        String invalidSdCard = CreateAndroidAvdPanel.validationError(
                "Pixel_8",
                IMAGE,
                Optional.empty(),
                "2G",
                Set.of());

        assertTrue(unsafeName.contains("AVD name"));
        assertTrue(invalidSdCard.contains("SD card size"));
    }

    @Test
    void rejectsMissingInstalledSystemImage() {
        String error = CreateAndroidAvdPanel.validationError(
                "Pixel_8",
                null,
                Optional.empty(),
                "",
                Set.of());

        assertTrue(error.contains("no installed system image"));
    }
}
