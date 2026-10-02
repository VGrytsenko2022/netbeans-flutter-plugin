package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterDevice;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FlutterEmulatorDeviceMatcherTest {
    private static final FlutterEmulator PIXEL =
            new FlutterEmulator("Pixel_9_API_35", "Pixel 9 API 35", "Google", "android");
    private static final FlutterDevice OLD_EMULATOR =
            new FlutterDevice("emulator-5554", "Old phone", "android-x64", true);
    private static final FlutterDevice NEW_EMULATOR =
            new FlutterDevice("emulator-5556", "sdk gphone64", "android-x64", true);

    @Test
    void normalizedConfiguredNameWinsWhenSeveralEmulatorsAreRunning() {
        FlutterDevice selected = new FlutterDevice(
                "emulator-5558", "Pixel_9_API_35", "android-x64", true);

        var match = FlutterEmulatorDeviceMatcher.match(
                PIXEL,
                Set.of(OLD_EMULATOR.id()),
                List.of(OLD_EMULATOR, NEW_EMULATOR, selected));

        assertEquals(selected, match.orElseThrow());
    }

    @Test
    void uniqueNewMobileEmulatorIsSelectedWhenRuntimeNameDiffers() {
        var match = FlutterEmulatorDeviceMatcher.match(
                PIXEL,
                Set.of(OLD_EMULATOR.id()),
                List.of(OLD_EMULATOR, NEW_EMULATOR));

        assertEquals(NEW_EMULATOR, match.orElseThrow());
    }

    @Test
    void physicalAndWebDevicesAreNeverAcceptedAsLaunchedEmulator() {
        FlutterDevice physical = new FlutterDevice(
                "phone", "Pixel 9 API 35", "android-arm64", false);
        FlutterDevice chrome = new FlutterDevice(
                "chrome", "Pixel 9 API 35", "web-javascript", false);

        var match = FlutterEmulatorDeviceMatcher.match(
                PIXEL,
                Set.of(),
                List.of(physical, chrome));

        assertTrue(match.isEmpty());
    }

    @Test
    void ambiguousNewEmulatorsAreNotResolvedByListOrder() {
        FlutterDevice secondNew = new FlutterDevice(
                "emulator-5558", "Another phone", "android-x64", true);

        var match = FlutterEmulatorDeviceMatcher.match(
                PIXEL,
                Set.of(OLD_EMULATOR.id()),
                List.of(OLD_EMULATOR, NEW_EMULATOR, secondNew));

        assertTrue(match.isEmpty());
    }

    @Test
    void soleUnrelatedEmulatorThatPredatesLaunchIsNotSelected() {
        var match = FlutterEmulatorDeviceMatcher.match(
                PIXEL,
                Set.of(OLD_EMULATOR.id()),
                List.of(OLD_EMULATOR));

        assertTrue(match.isEmpty());
    }
}
