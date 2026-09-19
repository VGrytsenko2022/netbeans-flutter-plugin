package io.github.vgrytsenko2022.run;

/** A hardware profile advertised by {@code avdmanager list device}. */
public record AndroidDeviceDefinition(
        String id,
        String displayName,
        String manufacturer,
        String tag) {

    public AndroidDeviceDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Device definition id is required");
        }
        id = id.strip();
        displayName = displayName == null || displayName.isBlank() ? id : displayName.strip();
        manufacturer = manufacturer == null ? "" : manufacturer.strip();
        tag = tag == null ? "" : tag.strip();
    }
}
