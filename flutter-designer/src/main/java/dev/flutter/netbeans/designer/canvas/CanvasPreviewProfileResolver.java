package dev.flutter.netbeans.designer.canvas;

import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.CanvasPreferences;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Central, deterministic viewport and adaptive-platform preview profiles. */
public final class CanvasPreviewProfileResolver {
    private static final int LEGACY_SEED_ARGB = 0xFF6750A4;

    private CanvasPreviewProfileResolver() {
    }

    /** Infers the initial toolbar mode without claiming a concrete device runtime. */
    public static CanvasPreviewMode initialMode(Optional<CanvasPreferences> preferences) {
        Objects.requireNonNull(preferences, "preferences");
        if (preferences.isEmpty()) {
            return CanvasPreviewMode.MOBILE;
        }
        CanvasPreferences value = preferences.orElseThrow();
        String preset = value.preset().orElse("").toLowerCase(Locale.ROOT);
        if (preset.contains("tablet") || preset.contains("ipad")) {
            return CanvasPreviewMode.TABLET;
        }
        if (preset.contains("desktop") || preset.contains("window")) {
            return CanvasPreviewMode.DESKTOP;
        }
        if (preset.contains("web") || preset.contains("browser")) {
            return CanvasPreviewMode.WEB;
        }
        if (preset.contains("phone") || preset.contains("mobile")) {
            return CanvasPreviewMode.MOBILE;
        }
        double width = value.logicalWidth().map(Number::doubleValue).orElse(390.0d);
        return width <= 600.0d
                ? CanvasPreviewMode.MOBILE
                : width <= 1_000.0d
                        ? CanvasPreviewMode.TABLET
                        : CanvasPreviewMode.DESKTOP;
    }

    /**
     * Resolves one exact profile. The target platform controls Flutter adaptive
     * widget semantics and remains independent from the responsive viewport.
     * The concrete engine that executes this profile is identified separately
     * by {@code engineIdentity} and by the native host implementation.
     */
    public static CanvasRenderProfile resolve(
            CanvasPreviewMode mode,
            CanvasTargetPlatform targetPlatform,
            Optional<CanvasPreferences> preferences,
            CanvasEngineIdentity engineIdentity) {
        DesignerThemeMode requestedMode = preferences
                .flatMap(CanvasPreferences::themeMode)
                .orElse(DesignerThemeMode.LIGHT);
        CanvasThemeBrightness brightness = requestedMode == DesignerThemeMode.DARK
                ? CanvasThemeBrightness.DARK
                : CanvasThemeBrightness.LIGHT;
        return resolve(
                mode,
                targetPlatform,
                preferences,
                engineIdentity,
                legacyTheme(brightness));
    }

    /**
     * Resolves viewport preferences around one already verified project theme.
     * The caller owns project-theme inheritance and the per-document
     * {@code canvas.themeMode} brightness override; this layer must not invent
     * or duplicate project theme definitions.
     */
    public static CanvasRenderProfile resolve(
            CanvasPreviewMode mode,
            CanvasTargetPlatform targetPlatform,
            Optional<CanvasPreferences> preferences,
            CanvasEngineIdentity engineIdentity,
            CanvasResolvedTheme resolvedTheme) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(targetPlatform, "targetPlatform");
        Objects.requireNonNull(preferences, "preferences");
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        Objects.requireNonNull(resolvedTheme, "resolvedTheme");
        requireCompatible(mode, targetPlatform);
        CanvasViewport defaultViewport = defaultViewport(mode);
        double[] size = new double[]{
            defaultViewport.logicalWidth(),
            defaultViewport.logicalHeight()
        };
        double dpr = 1.0d;
        String locale = "en-US";
        double textScale = 1.0d;
        if (preferences.isPresent()) {
            CanvasPreferences value = preferences.orElseThrow();
            boolean ownsSavedViewport = mode == initialMode(preferences);
            if (ownsSavedViewport) {
                size[0] = value.logicalWidth().map(Number::doubleValue).orElse(size[0]);
                size[1] = value.logicalHeight().map(Number::doubleValue).orElse(size[1]);
                dpr = value.devicePixelRatio().map(Number::doubleValue).orElse(dpr);
                if (value.orientation().orElse(null) == CanvasOrientation.LANDSCAPE
                        && size[0] < size[1]
                        || value.orientation().orElse(null) == CanvasOrientation.PORTRAIT
                        && size[0] > size[1]) {
                    double swap = size[0];
                    size[0] = size[1];
                    size[1] = swap;
                }
            }
            locale = value.locale().orElse(locale);
            textScale = value.textScaleFactor().map(Number::doubleValue)
                    .orElse(textScale);
        }
        return new CanvasRenderProfile(
                mode,
                targetPlatform,
                new CanvasViewport(size[0], size[1]),
                new CanvasDevicePixelRatio(dpr),
                resolvedTheme,
                new CanvasLocale(locale),
                new CanvasTextScaleFactor(textScale),
                engineIdentity);
    }

    /** Exact compatibility theme for projects created outside this plugin. */
    public static CanvasResolvedTheme legacyTheme(CanvasThemeBrightness brightness) {
        Objects.requireNonNull(brightness, "brightness");
        String variant = brightness.name().toLowerCase(Locale.ROOT);
        String definitionId = "material.default." + variant;
        String identityInput = "netbeans-flutter-canvas-legacy-v1\n"
                + definitionId + '\n'
                + String.format(Locale.ROOT, "0x%08X", LEGACY_SEED_ARGB);
        return new CanvasResolvedTheme(
                definitionId,
                LEGACY_SEED_ARGB,
                brightness,
                sha256(identityInput));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(
                            value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    /** Returns the canonical default logical-pixel viewport for a preview mode. */
    public static CanvasViewport defaultViewport(CanvasPreviewMode mode) {
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case MOBILE -> new CanvasViewport(390.0d, 844.0d);
            case TABLET -> new CanvasViewport(800.0d, 1_280.0d);
            case DESKTOP -> new CanvasViewport(1_280.0d, 800.0d);
            case WEB -> new CanvasViewport(1_440.0d, 900.0d);
        };
    }

    private static void requireCompatible(
            CanvasPreviewMode mode,
            CanvasTargetPlatform targetPlatform) {
        boolean compatible = switch (mode) {
            case MOBILE, TABLET -> targetPlatform == CanvasTargetPlatform.ANDROID
                    || targetPlatform == CanvasTargetPlatform.IOS;
            case DESKTOP -> targetPlatform == CanvasTargetPlatform.WINDOWS
                    || targetPlatform == CanvasTargetPlatform.MACOS
                    || targetPlatform == CanvasTargetPlatform.LINUX;
            case WEB -> targetPlatform == CanvasTargetPlatform.WEB;
        };
        if (!compatible) {
            throw new IllegalArgumentException(
                    "Canvas preview mode " + mode
                    + " is incompatible with target platform " + targetPlatform);
        }
    }
}
