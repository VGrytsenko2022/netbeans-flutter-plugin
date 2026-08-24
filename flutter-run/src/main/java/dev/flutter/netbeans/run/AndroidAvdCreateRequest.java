package dev.flutter.netbeans.run;

import java.util.Optional;
import java.util.regex.Pattern;

/** Validated input for creating an Android Virtual Device. */
public record AndroidAvdCreateRequest(
        String name,
        String systemImagePackage,
        Optional<String> deviceDefinitionId,
        Optional<String> sdCardSize,
        boolean overwriteExisting) {

    private static final Pattern SAFE_AVD_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern SYSTEM_IMAGE = Pattern.compile(
            "system-images;[^;\\s]+;[^;\\s]+;[^;\\s]+");
    private static final Pattern SD_CARD_SIZE = Pattern.compile("[1-9][0-9]*[KM]", Pattern.CASE_INSENSITIVE);

    public AndroidAvdCreateRequest {
        if (name == null || !SAFE_AVD_NAME.matcher(name.strip()).matches()) {
            throw new IllegalArgumentException(
                    "AVD name must start with a letter or digit and contain only letters, digits, '.', '_' or '-'");
        }
        name = name.strip();
        if (systemImagePackage == null
                || !SYSTEM_IMAGE.matcher(systemImagePackage.strip()).matches()) {
            throw new IllegalArgumentException(
                    "System image package must have the form system-images;android-<api>;<tag>;<abi>");
        }
        systemImagePackage = systemImagePackage.strip();
        deviceDefinitionId = cleanOptional(deviceDefinitionId, "Device definition id");
        sdCardSize = cleanOptional(sdCardSize, "SD card size");
        if (sdCardSize.isPresent() && !SD_CARD_SIZE.matcher(sdCardSize.get()).matches()) {
            throw new IllegalArgumentException("SD card size must be a positive value in K or M, for example 512M");
        }
    }

    public AndroidAvdCreateRequest(String name, String systemImagePackage) {
        this(name, systemImagePackage, Optional.empty(), Optional.empty(), false);
    }

    private static Optional<String> cleanOptional(Optional<String> value, String label) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String cleaned = value.get().strip();
        if (cleaned.isEmpty() || cleaned.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " must not be empty or contain control characters");
        }
        return Optional.of(cleaned);
    }
}
