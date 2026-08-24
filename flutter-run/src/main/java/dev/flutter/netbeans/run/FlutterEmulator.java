package dev.flutter.netbeans.run;

/** An emulator advertised by {@code flutter emulators}. */
public record FlutterEmulator(String id, String name, String manufacturer, String platform) {
    public FlutterEmulator {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Emulator id is required");
        }
        id = id.strip();
        name = name == null || name.isBlank() ? id : name.strip();
        manufacturer = manufacturer == null ? "" : manufacturer.strip();
        platform = platform == null || platform.isBlank() ? "unknown" : platform.strip();
    }
}
