package dev.flutter.netbeans.run;

import dev.flutter.netbeans.api.FlutterDevice;
import java.util.Locale;
import java.util.Objects;

/** Broad target kind used by the device selector. */
public enum FlutterTargetKind {
    DESKTOP("Desktop"),
    MOBILE("Mobile"),
    WEB("Web"),
    UNKNOWN("Unknown");

    private final String label;

    FlutterTargetKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static FlutterTargetKind from(FlutterDevice device) {
        Objects.requireNonNull(device, "device");
        String platform = normalize(device.platform());
        String id = normalize(device.id());

        if (platform.startsWith("web-") || platform.equals("web")
                || id.equals("chrome") || id.equals("edge") || id.equals("web-server")) {
            return WEB;
        }
        if (platform.startsWith("android") || platform.startsWith("ios")) {
            return MOBILE;
        }
        if (platform.startsWith("windows") || platform.startsWith("linux")
                || platform.startsWith("darwin") || platform.startsWith("macos")) {
            return DESKTOP;
        }
        return UNKNOWN;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
