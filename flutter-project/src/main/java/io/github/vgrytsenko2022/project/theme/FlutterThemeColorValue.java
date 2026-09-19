package io.github.vgrytsenko2022.project.theme;

import java.util.Objects;

/** Literal or semantic ColorScheme-backed color used by a TextTheme override. */
public sealed interface FlutterThemeColorValue
        permits FlutterThemeColorValue.Literal, FlutterThemeColorValue.ColorRole {

    record Literal(int argb) implements FlutterThemeColorValue {
        public String argbLiteral() {
            return FlutterThemeTextStyleOverride.argbLiteral(argb);
        }
    }

    record ColorRole(FlutterMaterialColorRole role) implements FlutterThemeColorValue {
        public ColorRole {
            Objects.requireNonNull(role, "role");
        }
    }
}
