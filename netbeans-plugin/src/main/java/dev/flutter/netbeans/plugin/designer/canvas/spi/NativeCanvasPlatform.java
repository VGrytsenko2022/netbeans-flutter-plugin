package dev.flutter.netbeans.plugin.designer.canvas.spi;

import java.util.Locale;
import java.util.Objects;

/** Operating-system identity used only at the native Canvas provider edge. */
public enum NativeCanvasPlatform {
    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    public static NativeCanvasPlatform detect(String operatingSystemName) {
        String normalized = Objects.requireNonNullElse(operatingSystemName, "")
                .strip()
                .toLowerCase(Locale.ROOT);
        if (normalized.startsWith("windows")) {
            return WINDOWS;
        }
        if (normalized.startsWith("linux")) {
            return LINUX;
        }
        if (normalized.startsWith("mac") || normalized.startsWith("darwin")) {
            return MACOS;
        }
        return UNKNOWN;
    }
}
