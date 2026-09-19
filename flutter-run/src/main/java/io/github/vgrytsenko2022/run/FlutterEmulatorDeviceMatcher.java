package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.FlutterDevice;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Safely associates a configured emulator with a device discovered after launch. */
public final class FlutterEmulatorDeviceMatcher {
    private FlutterEmulatorDeviceMatcher() {
    }

    public static Optional<FlutterDevice> match(
            FlutterEmulator emulator,
            Set<String> deviceIdsBeforeLaunch,
            List<FlutterDevice> devices) {
        Objects.requireNonNull(emulator, "emulator");
        Objects.requireNonNull(deviceIdsBeforeLaunch, "deviceIdsBeforeLaunch");
        Objects.requireNonNull(devices, "devices");

        List<FlutterDevice> mobileEmulators = devices.stream()
                .filter(Objects::nonNull)
                .filter(FlutterDevice::emulator)
                .filter(device -> FlutterTargetKind.from(device) == FlutterTargetKind.MOBILE)
                .toList();
        List<FlutterDevice> exact = mobileEmulators.stream()
                .filter(device -> sameIdentity(emulator, device))
                .toList();
        if (exact.size() == 1) {
            return Optional.of(exact.getFirst());
        }
        if (!exact.isEmpty()) {
            return Optional.empty();
        }

        List<FlutterDevice> fresh = mobileEmulators.stream()
                .filter(device -> !deviceIdsBeforeLaunch.contains(device.id()))
                .toList();
        return fresh.size() == 1 ? Optional.of(fresh.getFirst()) : Optional.empty();
    }

    private static boolean sameIdentity(FlutterEmulator emulator, FlutterDevice device) {
        String emulatorId = normalize(emulator.id());
        String emulatorName = normalize(emulator.name());
        String deviceId = normalize(device.id());
        String deviceName = normalize(device.name());
        return matches(emulatorId, deviceId)
                || matches(emulatorId, deviceName)
                || matches(emulatorName, deviceId)
                || matches(emulatorName, deviceName);
    }

    private static boolean matches(String first, String second) {
        return !first.isEmpty() && first.equals(second);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(value.length());
        value.toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(normalized::appendCodePoint);
        return normalized.toString();
    }
}
