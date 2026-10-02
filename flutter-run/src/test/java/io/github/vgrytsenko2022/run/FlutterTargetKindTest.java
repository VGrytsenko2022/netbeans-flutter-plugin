package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vgrytsenko2022.api.FlutterDevice;
import org.junit.jupiter.api.Test;

class FlutterTargetKindTest {
    @Test
    void classifiesDesktopTargets() {
        assertKind(FlutterTargetKind.DESKTOP, "windows", "windows-x64");
        assertKind(FlutterTargetKind.DESKTOP, "linux", "linux-arm64");
        assertKind(FlutterTargetKind.DESKTOP, "macos", "darwin-arm64");
    }

    @Test
    void classifiesMobileTargets() {
        assertKind(FlutterTargetKind.MOBILE, "emulator-5554", "android-x64");
        assertKind(FlutterTargetKind.MOBILE, "iphone", "ios");
    }

    @Test
    void classifiesWebTargetsByPlatformOrKnownDeviceId() {
        assertKind(FlutterTargetKind.WEB, "web-server", "web-javascript");
        assertKind(FlutterTargetKind.WEB, "chrome", "unknown");
        assertKind(FlutterTargetKind.WEB, "edge", "unknown");
    }

    @Test
    void retainsUnknownTargetsAndProvidesLabels() {
        assertKind(FlutterTargetKind.UNKNOWN, "custom-device", "fuchsia-arm64");
        assertEquals("Desktop", FlutterTargetKind.DESKTOP.label());
        assertEquals("Mobile", FlutterTargetKind.MOBILE.label());
        assertEquals("Web", FlutterTargetKind.WEB.label());
        assertEquals("Unknown", FlutterTargetKind.UNKNOWN.label());
    }

    private static void assertKind(FlutterTargetKind expected, String id, String platform) {
        FlutterDevice device = new FlutterDevice(id, id, platform, false);
        assertEquals(expected, FlutterTargetKind.from(device));
    }
}
