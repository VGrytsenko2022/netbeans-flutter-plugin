package io.github.vgrytsenko2022.run;

import java.util.OptionalInt;

/** A system image advertised by {@code sdkmanager --list}. */
public record AndroidSystemImage(
        String packageId,
        String apiLevel,
        String tag,
        String abi,
        String description,
        String version,
        boolean installed) {

    public AndroidSystemImage {
        packageId = required(packageId, "System image package id");
        apiLevel = valueOr(apiLevel, "unknown");
        tag = valueOr(tag, "unknown");
        abi = valueOr(abi, "unknown");
        description = valueOr(description, packageId);
        version = version == null ? "" : version.strip();
    }

    public OptionalInt numericApiLevel() {
        try {
            return OptionalInt.of(Integer.parseInt(apiLevel));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.strip();
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
