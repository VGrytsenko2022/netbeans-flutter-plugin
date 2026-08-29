package dev.flutter.netbeans.designer.canvas;

import dev.flutter.netbeans.designer.catalog.MaterialThemeTokenCatalog;
import java.util.Objects;

/** Safe cross-module color value for one Canvas TextTheme override. */
public sealed interface CanvasThemeColorValue
        permits CanvasThemeColorValue.Literal, CanvasThemeColorValue.ColorRole {

    record Literal(int argb) implements CanvasThemeColorValue {
        public String argbLiteral() {
            return "0x%08X".formatted(argb);
        }
    }

    record ColorRole(String role) implements CanvasThemeColorValue {
        public ColorRole {
            Objects.requireNonNull(role, "role");
            if (!MaterialThemeTokenCatalog.colorRoles().containsValue(role)) {
                throw new IllegalArgumentException("Unknown Canvas ColorScheme role: " + role);
            }
        }
    }
}
