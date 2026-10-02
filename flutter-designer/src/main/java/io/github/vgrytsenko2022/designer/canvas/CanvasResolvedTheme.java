package io.github.vgrytsenko2022.designer.canvas;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Exact resolved project-theme variant used by the Flutter Canvas runtime.
 *
 * <p>The definition id is the stable user-facing identity from the project
 * theme document. {@code digestIdentity} binds the complete canonical project
 * theme revision. Both are required because a stable definition may change
 * while retaining its id. The seed, brightness, ColorScheme, TextTheme and
 * component-color overrides are the complete protocol-v9 Canvas theme input;
 * the isolated runner never reads or executes project Dart code.</p>
 */
public record CanvasResolvedTheme(
        String definitionId,
        int seedArgb,
        CanvasThemeBrightness brightness,
        String digestIdentity,
        Map<String, Integer> colorSchemeOverrides,
        Map<String, CanvasThemeTextStyleOverride> textThemeOverrides,
        Map<CanvasThemeComponentColorRole, CanvasThemeColorValue> componentColors) {
    private static final int MAX_DEFINITION_ID_CODE_POINTS = 64;
    private static final Pattern DEFINITION_ID = Pattern.compile(
            "(?:[a-z][a-z0-9_]*|material\\.default\\.(?:light|dark))");
    private static final Pattern DIGEST = Pattern.compile("[0-9A-F]{64}");

    public CanvasResolvedTheme {
        Objects.requireNonNull(definitionId, "definitionId");
        Objects.requireNonNull(brightness, "brightness");
        Objects.requireNonNull(digestIdentity, "digestIdentity");
        Objects.requireNonNull(colorSchemeOverrides, "colorSchemeOverrides");
        Objects.requireNonNull(textThemeOverrides, "textThemeOverrides");
        Objects.requireNonNull(componentColors, "componentColors");
        if (!definitionId.equals(definitionId.strip())) {
            throw new IllegalArgumentException(
                    "definitionId must not contain leading or trailing whitespace");
        }
        int length = definitionId.codePointCount(0, definitionId.length());
        if (length == 0 || length > MAX_DEFINITION_ID_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "definitionId must contain between 1 and "
                    + MAX_DEFINITION_ID_CODE_POINTS + " Unicode code points");
        }
        if (!DEFINITION_ID.matcher(definitionId).matches()) {
            throw new IllegalArgumentException(
                    "definitionId must be a canonical project theme id or a "
                    + "built-in legacy theme id");
        }
        if ((seedArgb >>> 24) != 0xFF) {
            throw new IllegalArgumentException(
                    "seedArgb must be opaque and use alpha FF");
        }
        if (!DIGEST.matcher(digestIdentity).matches()) {
            throw new IllegalArgumentException(
                    "digestIdentity must be an uppercase SHA-256 value");
        }
        colorSchemeOverrides = Map.copyOf(colorSchemeOverrides);
        textThemeOverrides = Map.copyOf(textThemeOverrides);
        componentColors = Map.copyOf(componentColors);
        for (Map.Entry<String, Integer> entry : colorSchemeOverrides.entrySet()) {
            if (!io.github.vgrytsenko2022.designer.catalog.MaterialThemeTokenCatalog
                    .colorRoles().containsValue(entry.getKey())) {
                throw new IllegalArgumentException(
                        "Unknown Canvas ColorScheme override role: " + entry.getKey());
            }
            Objects.requireNonNull(entry.getValue(), "ColorScheme override ARGB");
        }
        for (Map.Entry<String, CanvasThemeTextStyleOverride> entry
                : textThemeOverrides.entrySet()) {
            if (!io.github.vgrytsenko2022.designer.catalog.MaterialThemeTokenCatalog
                    .textStyleRoles().containsValue(entry.getKey())) {
                throw new IllegalArgumentException(
                        "Unknown Canvas TextTheme override role: " + entry.getKey());
            }
            Objects.requireNonNull(entry.getValue(), "TextTheme override");
        }
        componentColors.forEach((role, value) -> {
            Objects.requireNonNull(role, "component color role");
            Objects.requireNonNull(value, "component color value");
        });
    }

    /** Source-compatible seed-only Canvas theme. */
    public CanvasResolvedTheme(
            String definitionId,
            int seedArgb,
            CanvasThemeBrightness brightness,
            String digestIdentity) {
        this(definitionId, seedArgb, brightness, digestIdentity, Map.of(), Map.of());
    }

    /** Source-compatible protocol-v8 Canvas theme without component colors. */
    public CanvasResolvedTheme(
            String definitionId,
            int seedArgb,
            CanvasThemeBrightness brightness,
            String digestIdentity,
            Map<String, Integer> colorSchemeOverrides,
            Map<String, CanvasThemeTextStyleOverride> textThemeOverrides) {
        this(definitionId, seedArgb, brightness, digestIdentity,
                colorSchemeOverrides, textThemeOverrides, Map.of());
    }

    /** Canonical unsigned ARGB literal used by the cross-language payload. */
    public String seedArgbLiteral() {
        return String.format(Locale.ROOT, "0x%08X", Integer.toUnsignedLong(seedArgb));
    }
}
