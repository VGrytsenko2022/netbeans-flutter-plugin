package dev.flutter.netbeans.designer.canvas;

import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.CanvasPreferences;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Central, deterministic viewport and adaptive-platform preview profiles. */
public final class CanvasPreviewProfileResolver {
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
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(targetPlatform, "targetPlatform");
        Objects.requireNonNull(preferences, "preferences");
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        requireCompatible(mode, targetPlatform);
        double[] size = defaultSize(mode);
        double dpr = 1.0d;
        DesignerThemeMode themeMode = DesignerThemeMode.LIGHT;
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
            themeMode = value.themeMode().orElse(themeMode);
            locale = value.locale().orElse(locale);
            textScale = value.textScaleFactor().map(Number::doubleValue)
                    .orElse(textScale);
        }
        CanvasThemeBrightness brightness = themeMode == DesignerThemeMode.DARK
                ? CanvasThemeBrightness.DARK
                : CanvasThemeBrightness.LIGHT;
        return new CanvasRenderProfile(
                mode,
                targetPlatform,
                new CanvasViewport(size[0], size[1]),
                new CanvasDevicePixelRatio(dpr),
                new CanvasResolvedTheme(
                        "material.default." + brightness.name().toLowerCase(Locale.ROOT),
                        brightness),
                new CanvasLocale(locale),
                new CanvasTextScaleFactor(textScale),
                engineIdentity);
    }

    private static double[] defaultSize(CanvasPreviewMode mode) {
        return switch (mode) {
            case MOBILE -> new double[]{390.0d, 844.0d};
            case TABLET -> new double[]{800.0d, 1_280.0d};
            case DESKTOP -> new double[]{1_280.0d, 800.0d};
            case WEB -> new double[]{1_440.0d, 900.0d};
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
