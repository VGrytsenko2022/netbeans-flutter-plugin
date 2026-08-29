package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasPreviewProfileResolver;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasThemeColorValue;
import dev.flutter.netbeans.designer.canvas.CanvasThemeTextStyleOverride;
import dev.flutter.netbeans.designer.canvas.CanvasThemeBrightness;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeCodec;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDefinition;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDigests;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadResult;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadStatus;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeStore;
import dev.flutter.netbeans.project.theme.FlutterThemeBrightness;
import dev.flutter.netbeans.project.theme.FlutterThemeColorValue;
import dev.flutter.netbeans.project.theme.FlutterThemeTextStyleOverride;
import dev.flutter.netbeans.project.theme.FlutterThemeMode;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.UIManager;

/**
 * Bounded, fail-closed bridge from one project theme contract to a Canvas
 * presentation. Project Dart is verified by the core store but is never loaded
 * or executed in the isolated Canvas runner.
 */
final class FlutterDesignerProjectThemeResolver {
    private final ThemeLoader loader;
    private final FlutterProjectThemeCodec codec;
    private final Supplier<CanvasThemeBrightness> systemBrightness;

    FlutterDesignerProjectThemeResolver() {
        this(
                new FlutterProjectThemeStore()::load,
                new FlutterProjectThemeCodec(),
                FlutterDesignerProjectThemeResolver::uiBrightness);
    }

    FlutterDesignerProjectThemeResolver(
            ThemeLoader loader,
            FlutterProjectThemeCodec codec,
            Supplier<CanvasThemeBrightness> systemBrightness) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.systemBrightness = Objects.requireNonNull(
                systemBrightness, "systemBrightness");
    }

    Resolution resolve(
            Path projectRoot,
            Optional<DesignerThemeMode> previewOverride) {
        return resolve(projectRoot, previewOverride, requireSystemBrightness());
    }

    Resolution resolve(
            Path projectRoot,
            Optional<DesignerThemeMode> previewOverride,
            CanvasThemeBrightness resolvedSystemBrightness) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(previewOverride, "previewOverride");
        Objects.requireNonNull(resolvedSystemBrightness, "resolvedSystemBrightness");
        FlutterProjectThemeLoadResult loaded = loader.load(projectRoot);
        if (loaded.status() == FlutterProjectThemeLoadStatus.MISSING) {
            CanvasThemeBrightness brightness = previewOverride.isEmpty()
                    ? CanvasThemeBrightness.LIGHT
                    : switch (previewOverride.orElseThrow()) {
                        case LIGHT -> CanvasThemeBrightness.LIGHT;
                        case DARK -> CanvasThemeBrightness.DARK;
                        case SYSTEM -> resolvedSystemBrightness;
                    };
            return Resolution.available(
                    CanvasPreviewProfileResolver.legacyTheme(brightness),
                    true,
                    "The project has no .fd_templates/project.fdtheme; Canvas uses "
                    + "the compatible built-in Material "
                    + brightness.name().toLowerCase(java.util.Locale.ROOT)
                    + " theme.");
        }
        if (!loaded.valid()) {
            return Resolution.unavailable(loaded.detail());
        }

        FlutterProjectTheme projectTheme = loaded.theme().orElseThrow();
        if (!projectTheme.enabled()) {
            CanvasThemeBrightness brightness = legacyBrightness(
                    previewOverride, resolvedSystemBrightness);
            return Resolution.available(
                    CanvasPreviewProfileResolver.legacyTheme(brightness),
                    true,
                    "Project themes are disabled; Canvas uses the compatible built-in "
                    + "Material "
                    + brightness.name().toLowerCase(java.util.Locale.ROOT)
                    + " theme.");
        }
        CanvasThemeBrightness brightness = resolveBrightness(
                previewOverride, projectTheme.defaultMode(), resolvedSystemBrightness);
        FlutterProjectThemeDefinition definition = brightness
                == CanvasThemeBrightness.DARK
                ? projectTheme.darkTheme()
                : projectTheme.lightTheme();
        if (!definition.enabled()) {
            return Resolution.unavailable(
                    "Project theme " + definition.displayName()
                    + " (" + definition.id() + ") is disabled, so Canvas cannot "
                    + "use it while project themes are enabled.");
        }
        if (!matches(definition.brightness(), brightness)) {
            return Resolution.unavailable(
                    "Project theme " + definition.id() + " has brightness "
                    + definition.brightness().wireName() + " but Canvas resolved "
                    + brightness.name().toLowerCase(java.util.Locale.ROOT) + ".");
        }

        final String digest;
        try {
            digest = FlutterProjectThemeDigests.sha256(codec.encode(projectTheme));
        } catch (IOException | RuntimeException failure) {
            return Resolution.unavailable(
                    "Could not compute the canonical project theme identity: "
                    + compact(failure.getMessage()) + ".");
        }
        return Resolution.available(
                new CanvasResolvedTheme(
                        definition.id(),
                        definition.seedArgb(),
                        brightness,
                        digest,
                        canvasColors(definition),
                        canvasTextTheme(definition)),
                false,
                "Canvas uses verified project theme " + definition.displayName()
                + " (" + definition.id() + ").");
    }

    private CanvasThemeBrightness resolveBrightness(
            Optional<DesignerThemeMode> previewOverride,
            FlutterThemeMode projectDefault,
            CanvasThemeBrightness resolvedSystemBrightness) {
        if (previewOverride.isPresent()) {
            return switch (previewOverride.orElseThrow()) {
                case LIGHT -> CanvasThemeBrightness.LIGHT;
                case DARK -> CanvasThemeBrightness.DARK;
                case SYSTEM -> resolvedSystemBrightness;
            };
        }
        return switch (projectDefault) {
            case LIGHT -> CanvasThemeBrightness.LIGHT;
            case DARK -> CanvasThemeBrightness.DARK;
            case SYSTEM -> resolvedSystemBrightness;
        };
    }

    private static CanvasThemeBrightness legacyBrightness(
            Optional<DesignerThemeMode> previewOverride,
            CanvasThemeBrightness resolvedSystemBrightness) {
        if (previewOverride.isEmpty()) {
            return CanvasThemeBrightness.LIGHT;
        }
        return switch (previewOverride.orElseThrow()) {
            case LIGHT -> CanvasThemeBrightness.LIGHT;
            case DARK -> CanvasThemeBrightness.DARK;
            case SYSTEM -> resolvedSystemBrightness;
        };
    }

    private CanvasThemeBrightness requireSystemBrightness() {
        return Objects.requireNonNull(
                systemBrightness.get(), "systemBrightness returned null");
    }

    private static boolean matches(
            FlutterThemeBrightness projectBrightness,
            CanvasThemeBrightness canvasBrightness) {
        return projectBrightness == FlutterThemeBrightness.DARK
                ? canvasBrightness == CanvasThemeBrightness.DARK
                : canvasBrightness == CanvasThemeBrightness.LIGHT;
    }

    private static Map<String, Integer> canvasColors(
            FlutterProjectThemeDefinition definition) {
        Map<String, Integer> values = new LinkedHashMap<>();
        definition.overrides().colorScheme().forEach(
                (role, argb) -> values.put(role.wireName(), argb));
        return Map.copyOf(values);
    }

    private static Map<String, CanvasThemeTextStyleOverride> canvasTextTheme(
            FlutterProjectThemeDefinition definition) {
        Map<String, CanvasThemeTextStyleOverride> values = new LinkedHashMap<>();
        definition.overrides().textTheme().forEach((role, style) -> values.put(
                role.wireName(), canvasTextStyle(style)));
        return Map.copyOf(values);
    }

    private static CanvasThemeTextStyleOverride canvasTextStyle(
            FlutterThemeTextStyleOverride style) {
        Optional<Set<String>> decoration = style.decoration().map(lines -> lines.stream()
                .map(line -> line.wireName())
                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        return new CanvasThemeTextStyleOverride(
                style.color().map(FlutterDesignerProjectThemeResolver::canvasColor),
                style.backgroundColor().map(FlutterDesignerProjectThemeResolver::canvasColor),
                style.fontSize(),
                style.fontWeight().map(value -> value.wireName()),
                style.fontStyle().map(value -> value.wireName()),
                style.letterSpacing(),
                style.wordSpacing(),
                style.height(),
                style.fontFamily(),
                decoration,
                style.decorationColor().map(FlutterDesignerProjectThemeResolver::canvasColor),
                style.decorationStyle().map(value -> value.wireName()),
                style.decorationThickness());
    }

    private static CanvasThemeColorValue canvasColor(FlutterThemeColorValue color) {
        return switch (color) {
            case FlutterThemeColorValue.Literal literal ->
                new CanvasThemeColorValue.Literal(literal.argb());
            case FlutterThemeColorValue.ColorRole role ->
                new CanvasThemeColorValue.ColorRole(role.role().wireName());
        };
    }

    static CanvasThemeBrightness uiBrightness() {
        Color background = UIManager.getColor("Panel.background");
        if (background == null) {
            return CanvasThemeBrightness.LIGHT;
        }
        double luminance = (0.2126d * background.getRed()
                + 0.7152d * background.getGreen()
                + 0.0722d * background.getBlue()) / 255.0d;
        return luminance < 0.5d
                ? CanvasThemeBrightness.DARK
                : CanvasThemeBrightness.LIGHT;
    }

    private static String compact(String message) {
        if (message == null || message.isBlank()) {
            return "no cause was reported";
        }
        String value = message.trim().replaceAll("\\s+", " ");
        return value.length() <= 400 ? value : value.substring(0, 400) + "…";
    }

    @FunctionalInterface
    interface ThemeLoader {
        FlutterProjectThemeLoadResult load(Path projectRoot);
    }

    record Resolution(
            Optional<CanvasResolvedTheme> theme,
            boolean legacyFallback,
            String detail) {
        Resolution {
            theme = Objects.requireNonNull(theme, "theme");
            detail = Objects.requireNonNull(detail, "detail");
            if (detail.isBlank()) {
                throw new IllegalArgumentException("Theme resolution detail cannot be blank");
            }
            if (legacyFallback && theme.isEmpty()) {
                throw new IllegalArgumentException(
                        "A legacy fallback resolution must contain a theme");
            }
        }

        static Resolution available(
                CanvasResolvedTheme theme,
                boolean legacyFallback,
                String detail) {
            return new Resolution(Optional.of(theme), legacyFallback, detail);
        }

        static Resolution unavailable(String detail) {
            return new Resolution(Optional.empty(), false, detail);
        }

        boolean available() {
            return theme.isPresent();
        }
    }
}
