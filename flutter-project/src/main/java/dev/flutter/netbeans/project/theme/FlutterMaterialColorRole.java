package dev.flutter.netbeans.project.theme;

import java.util.Arrays;

/** Reviewed, non-deprecated {@code ColorScheme} roles supported by theme schema v4. */
public enum FlutterMaterialColorRole {
    PRIMARY("primary"),
    ON_PRIMARY("onPrimary"),
    PRIMARY_CONTAINER("primaryContainer"),
    ON_PRIMARY_CONTAINER("onPrimaryContainer"),
    PRIMARY_FIXED("primaryFixed"),
    PRIMARY_FIXED_DIM("primaryFixedDim"),
    ON_PRIMARY_FIXED("onPrimaryFixed"),
    ON_PRIMARY_FIXED_VARIANT("onPrimaryFixedVariant"),
    SECONDARY("secondary"),
    ON_SECONDARY("onSecondary"),
    SECONDARY_CONTAINER("secondaryContainer"),
    ON_SECONDARY_CONTAINER("onSecondaryContainer"),
    SECONDARY_FIXED("secondaryFixed"),
    SECONDARY_FIXED_DIM("secondaryFixedDim"),
    ON_SECONDARY_FIXED("onSecondaryFixed"),
    ON_SECONDARY_FIXED_VARIANT("onSecondaryFixedVariant"),
    TERTIARY("tertiary"),
    ON_TERTIARY("onTertiary"),
    TERTIARY_CONTAINER("tertiaryContainer"),
    ON_TERTIARY_CONTAINER("onTertiaryContainer"),
    TERTIARY_FIXED("tertiaryFixed"),
    TERTIARY_FIXED_DIM("tertiaryFixedDim"),
    ON_TERTIARY_FIXED("onTertiaryFixed"),
    ON_TERTIARY_FIXED_VARIANT("onTertiaryFixedVariant"),
    ERROR("error"),
    ON_ERROR("onError"),
    ERROR_CONTAINER("errorContainer"),
    ON_ERROR_CONTAINER("onErrorContainer"),
    SURFACE("surface"),
    ON_SURFACE("onSurface"),
    SURFACE_DIM("surfaceDim"),
    SURFACE_BRIGHT("surfaceBright"),
    SURFACE_CONTAINER_LOWEST("surfaceContainerLowest"),
    SURFACE_CONTAINER_LOW("surfaceContainerLow"),
    SURFACE_CONTAINER("surfaceContainer"),
    SURFACE_CONTAINER_HIGH("surfaceContainerHigh"),
    SURFACE_CONTAINER_HIGHEST("surfaceContainerHighest"),
    ON_SURFACE_VARIANT("onSurfaceVariant"),
    OUTLINE("outline"),
    OUTLINE_VARIANT("outlineVariant"),
    SHADOW("shadow"),
    SCRIM("scrim"),
    INVERSE_SURFACE("inverseSurface"),
    ON_INVERSE_SURFACE("onInverseSurface"),
    INVERSE_PRIMARY("inversePrimary"),
    SURFACE_TINT("surfaceTint");

    private final String wireName;

    FlutterMaterialColorRole(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public String themeTokenWireId() {
        return "material.colorScheme." + wireName;
    }

    public static FlutterMaterialColorRole fromWireName(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Material ColorScheme role: " + value));
    }
}
