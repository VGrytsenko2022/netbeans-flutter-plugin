package io.github.vgrytsenko2022.project.theme;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Deterministically generates the project-owned {@code AppTheme} Dart API. */
public final class FlutterProjectThemeDartGenerator {
    private FlutterProjectThemeDartGenerator() {
    }

    /** Returns canonical UTF-8 Dart source with exactly one final LF. */
    public static byte[] generate(FlutterProjectTheme projectTheme) {
        Objects.requireNonNull(projectTheme, "projectTheme");
        StringBuilder dart = new StringBuilder(2_048);
        dart.append("// GENERATED FILE - DO NOT EDIT.\n")
                .append("// Source: ")
                .append(FlutterProjectThemePaths.DESCRIPTOR_WIRE_PATH)
                .append(".\n\n")
                .append("import 'package:flutter/material.dart';\n\n")
                .append("/// Project-wide Flutter themes generated from ")
                .append(FlutterProjectThemePaths.DESCRIPTOR_WIRE_PATH)
                .append(".\n")
                .append("abstract final class AppTheme {\n")
                .append("  static final Map<String, ThemeData> themes =\n")
                .append("      Map<String, ThemeData>.unmodifiable(<String, ThemeData>{\n");
        int componentBuilderIndex = 0;
        for (FlutterProjectThemeDefinition theme : projectTheme.themes()) {
            if (!theme.enabled()) {
                continue;
            }
            dart.append("    '").append(theme.id()).append("': ");
            if (theme.overrides().isEmpty()) {
                appendBaseTheme(dart, theme, "    ");
                dart.append(",\n");
            } else {
                dart.append(builderName(theme, componentBuilderIndex)).append("(),\n");
                if (!theme.overrides().componentColors().isEmpty()) {
                    componentBuilderIndex++;
                }
            }
        }
        dart.append("  });\n\n");
        componentBuilderIndex = 0;
        for (FlutterProjectThemeDefinition theme : projectTheme.themes()) {
            if (theme.enabled() && !theme.overrides().isEmpty()) {
                appendThemeBuilder(dart, theme, componentBuilderIndex);
                if (!theme.overrides().componentColors().isEmpty()) {
                    componentBuilderIndex++;
                }
            }
        }
        dart
                .append("  static ThemeData resolve(String id) {\n")
                .append("    final theme = themes[id];\n")
                .append("    if (theme == null) {\n")
                .append("      throw ArgumentError.value(id, 'id', 'Unknown AppTheme id');\n")
                .append("    }\n")
                .append("    return theme;\n")
                .append("  }\n\n");
        if (projectTheme.enabled()) {
            dart.append("  static ThemeData get light => resolve('")
                    .append(projectTheme.lightThemeId()).append("');\n\n")
                    .append("  static ThemeData get dark => resolve('")
                    .append(projectTheme.darkThemeId()).append("');\n\n")
                    .append("  static ThemeMode get mode => ")
                    .append(projectTheme.defaultMode().dartExpression()).append(";\n");
        } else {
            dart.append("  static ThemeData? get light => null;\n\n")
                    .append("  static ThemeData? get dark => null;\n\n")
                    .append("  static ThemeMode? get mode => null;\n");
        }
        dart.append("}\n");
        return dart.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendBaseTheme(
            StringBuilder dart, FlutterProjectThemeDefinition theme, String indent) {
        dart.append("ThemeData.from(\n")
                .append(indent).append("  colorScheme: ColorScheme.fromSeed(\n")
                .append(indent).append("    seedColor: const Color(")
                .append(theme.seedArgbLiteral()).append("),\n")
                .append(indent).append("    brightness: ")
                .append(theme.brightness().dartExpression()).append(",\n")
                .append(indent).append("  ),\n")
                .append(indent).append(')');
    }

    private static void appendThemeBuilder(
            StringBuilder dart,
            FlutterProjectThemeDefinition theme,
            int componentBuilderIndex) {
        dart.append("  static ThemeData ")
                .append(builderName(theme, componentBuilderIndex)).append("() {\n")
                .append("    final colorScheme = ColorScheme.fromSeed(\n")
                .append("      seedColor: const Color(")
                .append(theme.seedArgbLiteral()).append("),\n")
                .append("      brightness: ")
                .append(theme.brightness().dartExpression()).append(",\n")
                .append("    )");
        if (!theme.overrides().colorScheme().isEmpty()) {
            dart.append(".copyWith(\n");
            for (FlutterMaterialColorRole role : FlutterMaterialColorRole.values()) {
                Integer argb = theme.overrides().colorScheme().get(role);
                if (argb != null) {
                    dart.append("      ").append(role.wireName())
                            .append(": const Color(")
                            .append(FlutterThemeTextStyleOverride.argbLiteral(argb))
                            .append("),\n");
                }
            }
            dart.append("    )");
        }
        dart.append(";\n")
                .append("    final base = ThemeData.from(colorScheme: colorScheme);\n");
        if (theme.overrides().textTheme().isEmpty()
                && theme.overrides().componentColors().isEmpty()) {
            dart.append("    return base;\n");
        } else {
            dart.append("    return base.copyWith(\n");
            if (!theme.overrides().textTheme().isEmpty()) {
                dart.append("      textTheme: base.textTheme.copyWith(\n");
                for (FlutterMaterialTextStyleRole role : FlutterMaterialTextStyleRole.values()) {
                    FlutterThemeTextStyleOverride style = theme.overrides().textTheme().get(role);
                    if (style != null) {
                        dart.append("        ").append(role.wireName()).append(": ")
                                .append("(base.textTheme.").append(role.wireName())
                                .append(" ?? const TextStyle()).copyWith(\n");
                        appendTextStyleFields(dart, style, "          ");
                        dart.append("        ),\n");
                    }
                }
                dart.append("      ),\n");
            }
            appendComponentThemes(dart, theme.overrides(), "      ");
            dart.append("    );\n");
        }
        dart.append("  }\n\n");
    }

    private static String builderName(
            FlutterProjectThemeDefinition theme, int componentBuilderIndex) {
        if (theme.overrides().componentColors().isEmpty()) {
            // Frozen schema-v4 byte contract. Do not normalize this legacy name.
            return "_build_" + theme.id();
        }
        // Catalog-order index is deterministic, collision-free and lowerCamel lint-safe.
        return "_buildComponentTheme" + componentBuilderIndex;
    }

    private static void appendTextStyleFields(
            StringBuilder dart, FlutterThemeTextStyleOverride style, String indent) {
        appendThemeColor(dart, indent, "color", style.color());
        appendThemeColor(dart, indent, "backgroundColor", style.backgroundColor());
        appendDouble(dart, indent, "fontSize", style.fontSize());
        style.fontWeight().ifPresent(value -> dart.append(indent)
                .append("fontWeight: ").append(value.dartExpression()).append(",\n"));
        style.fontStyle().ifPresent(value -> dart.append(indent)
                .append("fontStyle: ").append(value.dartExpression()).append(",\n"));
        appendDouble(dart, indent, "letterSpacing", style.letterSpacing());
        appendDouble(dart, indent, "wordSpacing", style.wordSpacing());
        appendDouble(dart, indent, "height", style.height());
        style.fontFamily().ifPresent(value -> dart.append(indent)
                .append("fontFamily: '").append(escapeDartString(value)).append("',\n"));
        style.decoration().ifPresent(lines -> {
            dart.append(indent).append("decoration: ");
            if (lines.isEmpty()) {
                dart.append("TextDecoration.none");
            } else if (lines.size() == 1) {
                dart.append(lines.iterator().next().dartExpression());
            } else {
                dart.append("TextDecoration.combine(const <TextDecoration>[\n");
                for (FlutterThemeTextDecorationLine line
                        : FlutterThemeTextDecorationLine.values()) {
                    if (lines.contains(line)) {
                        dart.append(indent).append("  ")
                                .append(line.dartExpression()).append(",\n");
                    }
                }
                dart.append(indent).append("])" );
            }
            dart.append(",\n");
        });
        appendThemeColor(dart, indent, "decorationColor", style.decorationColor());
        style.decorationStyle().ifPresent(value -> dart.append(indent)
                .append("decorationStyle: ").append(value.dartExpression()).append(",\n"));
        appendDouble(dart, indent, "decorationThickness", style.decorationThickness());
    }

    private static void appendThemeColor(
            StringBuilder dart, String indent, String field,
            java.util.Optional<FlutterThemeColorValue> value) {
        value.ifPresent(color -> {
            dart.append(indent).append(field).append(": ");
            switch (color) {
                case FlutterThemeColorValue.Literal literal -> dart
                        .append("const Color(").append(literal.argbLiteral()).append(')');
                case FlutterThemeColorValue.ColorRole role -> dart
                        .append("colorScheme.").append(role.role().wireName());
            }
            dart.append(",\n");
        });
    }

    private static void appendComponentThemes(
            StringBuilder dart, FlutterThemeOverrides overrides, String indent) {
        var colors = overrides.componentColors();
        appendComponentColor(dart, indent, "scaffoldBackgroundColor", colors,
                FlutterThemeComponentColorRole.SCAFFOLD_BACKGROUND);

        if (hasAny(colors,
                FlutterThemeComponentColorRole.APP_BAR_BACKGROUND,
                FlutterThemeComponentColorRole.APP_BAR_FOREGROUND,
                FlutterThemeComponentColorRole.APP_BAR_SHADOW,
                FlutterThemeComponentColorRole.APP_BAR_SURFACE_TINT)) {
            dart.append(indent).append("appBarTheme: base.appBarTheme.copyWith(\n");
            appendComponentColor(dart, indent + "  ", "backgroundColor", colors,
                    FlutterThemeComponentColorRole.APP_BAR_BACKGROUND);
            appendComponentColor(dart, indent + "  ", "foregroundColor", colors,
                    FlutterThemeComponentColorRole.APP_BAR_FOREGROUND);
            appendComponentColor(dart, indent + "  ", "shadowColor", colors,
                    FlutterThemeComponentColorRole.APP_BAR_SHADOW);
            appendComponentColor(dart, indent + "  ", "surfaceTintColor", colors,
                    FlutterThemeComponentColorRole.APP_BAR_SURFACE_TINT);
            dart.append(indent).append("),\n");
        }
        if (colors.containsKey(FlutterThemeComponentColorRole.ICON_COLOR)) {
            dart.append(indent).append("iconTheme: base.iconTheme.copyWith(\n");
            appendComponentColor(dart, indent + "  ", "color", colors,
                    FlutterThemeComponentColorRole.ICON_COLOR);
            dart.append(indent).append("),\n");
        }
        if (colors.keySet().stream().anyMatch(
                role -> role.elevatedButtonProperty() != null)) {
            dart.append(indent)
                    .append("elevatedButtonTheme: ElevatedButtonThemeData(\n")
                    .append(indent).append("  style: ButtonStyle(\n");
            for (String property : java.util.List.of(
                    "backgroundColor", "foregroundColor", "overlayColor",
                    "shadowColor", "surfaceTintColor", "iconColor")) {
                appendElevatedButtonColor(dart, indent + "    ", property, colors);
            }
            dart.append(indent).append("  ).merge(base.elevatedButtonTheme.style),\n")
                    .append(indent).append("),\n");
        }
    }

    private static void appendComponentColor(
            StringBuilder dart,
            String indent,
            String field,
            java.util.Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> colors,
            FlutterThemeComponentColorRole role) {
        FlutterThemeColorValue value = colors.get(role);
        if (value != null) {
            dart.append(indent).append(field).append(": ");
            appendThemeColorExpression(dart, value);
            dart.append(",\n");
        }
    }

    private static void appendElevatedButtonColor(
            StringBuilder dart,
            String indent,
            String property,
            java.util.Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> colors) {
        java.util.EnumMap<ElevatedButtonState, FlutterThemeColorValue> states =
                new java.util.EnumMap<>(ElevatedButtonState.class);
        for (var entry : colors.entrySet()) {
            if (property.equals(entry.getKey().elevatedButtonProperty())) {
                states.put(ElevatedButtonState.fromWireName(
                        entry.getKey().elevatedButtonState()), entry.getValue());
            }
        }
        if (states.isEmpty()) {
            return;
        }
        dart.append(indent).append(property)
                .append(": WidgetStateProperty.resolveWith<Color?>((states) {\n");
        dart.append(indent).append("  if (states.contains(WidgetState.disabled)) {\n")
                .append(indent).append("    return ");
        FlutterThemeColorValue disabled = states.get(ElevatedButtonState.DISABLED);
        if (disabled == null) {
            dart.append("null");
        } else {
            appendThemeColorExpression(dart, disabled);
        }
        dart.append(";\n").append(indent).append("  }\n");
        for (ElevatedButtonState state : java.util.List.of(
                ElevatedButtonState.PRESSED,
                ElevatedButtonState.HOVERED,
                ElevatedButtonState.FOCUSED)) {
            FlutterThemeColorValue value = states.get(state);
            if (value != null) {
                dart.append(indent).append("  if (states.contains(WidgetState.")
                        .append(state.wireName()).append(")) {\n")
                        .append(indent).append("    return ");
                appendThemeColorExpression(dart, value);
                dart.append(";\n").append(indent).append("  }\n");
            }
        }
        dart.append(indent).append("  return ");
        FlutterThemeColorValue fallback = states.get(ElevatedButtonState.DEFAULT);
        if (fallback == null) {
            dart.append("null");
        } else {
            appendThemeColorExpression(dart, fallback);
        }
        dart.append(";\n").append(indent).append("}),\n");
    }

    private static void appendThemeColorExpression(
            StringBuilder dart, FlutterThemeColorValue value) {
        switch (value) {
            case FlutterThemeColorValue.Literal literal -> dart
                    .append("const Color(").append(literal.argbLiteral()).append(')');
            case FlutterThemeColorValue.ColorRole role -> dart
                    .append("colorScheme.").append(role.role().wireName());
        }
    }

    private static boolean hasAny(
            java.util.Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> colors,
            FlutterThemeComponentColorRole... roles) {
        for (FlutterThemeComponentColorRole role : roles) {
            if (colors.containsKey(role)) {
                return true;
            }
        }
        return false;
    }

    private enum ElevatedButtonState {
        DEFAULT("default"),
        DISABLED("disabled"),
        PRESSED("pressed"),
        HOVERED("hovered"),
        FOCUSED("focused");

        private final String wireName;

        ElevatedButtonState(String wireName) {
            this.wireName = wireName;
        }

        String wireName() {
            return wireName;
        }

        static ElevatedButtonState fromWireName(String value) {
            for (ElevatedButtonState state : values()) {
                if (state.wireName.equals(value)) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Unknown ElevatedButton state: " + value);
        }
    }

    private static void appendDouble(
            StringBuilder dart, String indent, String field,
            java.util.Optional<Double> value) {
        value.ifPresent(number -> dart.append(indent).append(field).append(": ")
                .append(Double.toString(number)).append(",\n"));
    }

    private static String escapeDartString(String value) {
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("$", "\\$");
    }
}
