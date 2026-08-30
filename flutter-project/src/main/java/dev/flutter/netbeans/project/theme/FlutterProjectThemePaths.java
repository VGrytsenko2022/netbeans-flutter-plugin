package dev.flutter.netbeans.project.theme;

import java.nio.file.Path;

/** Canonical project-relative paths and wire-format identity for project themes. */
public final class FlutterProjectThemePaths {
    public static final String FORMAT = "netbeans-flutter-project-theme";
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int GLOBAL_ENABLED_SCHEMA_VERSION = 2;
    public static final int PER_THEME_ENABLED_SCHEMA_VERSION = 3;
    public static final int OVERRIDES_SCHEMA_VERSION = 4;
    public static final int SCHEMA_VERSION = 5;
    public static final String DESCRIPTOR_WIRE_PATH = ".fd_templates/project.fdtheme";
    public static final String GENERATED_DART_WIRE_PATH = "lib/theme/app_theme.dart";
    public static final Path DESCRIPTOR_PATH = Path.of(".fd_templates", "project.fdtheme");
    public static final Path GENERATED_DART_PATH = Path.of("lib", "theme", "app_theme.dart");

    private FlutterProjectThemePaths() {
    }
}
