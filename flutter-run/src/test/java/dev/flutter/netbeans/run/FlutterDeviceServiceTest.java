package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.ProcessResult;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterDeviceServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesCurrentNestedMachineOutputAndEscapedNames() throws Exception {
        String output = """
                [
                  {
                    "name": "Pixel \\"Pro\\"",
                    "id": "emulator-5554",
                    "isSupported": true,
                    "targetPlatform": "android-x64",
                    "emulator": true,
                    "capabilities": {
                      "hotReload": true,
                      "hotRestart": true,
                      "startPaused": true
                    }
                  },
                  {
                    "name": "Chrome",
                    "id": "chrome",
                    "targetPlatform": "web-javascript",
                    "emulator": false,
                    "capabilities": {"hotReload": true}
                  }
                ]
                """;
        AtomicReference<List<String>> arguments = new AtomicReference<>();
        FlutterDeviceService service = new FlutterDeviceService(
                (workingDirectory, timeout, commandArguments) -> {
                    assertEquals(temporaryDirectory.toAbsolutePath().normalize(), workingDirectory);
                    assertEquals(Duration.ofSeconds(30), timeout);
                    arguments.set(List.of(commandArguments));
                    return new ProcessResult(0, output, "");
                });

        var devices = service.list(temporaryDirectory);

        assertEquals(List.of("devices", "--machine"), arguments.get());
        assertEquals(2, devices.size());
        assertEquals("Pixel \"Pro\"", devices.get(0).name());
        assertEquals("emulator-5554", devices.get(0).id());
        assertEquals("android-x64", devices.get(0).platform());
        assertTrue(devices.get(0).emulator());
        assertEquals("web-javascript", devices.get(1).platform());
        assertFalse(devices.get(1).emulator());
    }

    @Test
    void treatsSuccessfulEmptyArrayAsNoDevices() throws Exception {
        FlutterDeviceService service = new FlutterDeviceService(
                (workingDirectory, timeout, arguments) -> new ProcessResult(0, "[]", ""));

        assertTrue(service.list(temporaryDirectory).isEmpty());
    }

    @Test
    void filtersDevicesExplicitlyReportedAsUnsupported() throws Exception {
        String output = """
                [
                  {
                    "name": "Unsupported Linux target",
                    "id": "linux",
                    "isSupported": false,
                    "targetPlatform": "linux-x64",
                    "emulator": false,
                    "capabilities": {"hotReload": true}
                  },
                  {
                    "name": "Windows",
                    "id": "windows",
                    "isSupported": true,
                    "targetPlatform": "windows-x64",
                    "emulator": false,
                    "capabilities": {"hotReload": true}
                  },
                  {
                    "name": "Legacy device without support flag",
                    "id": "legacy",
                    "targetPlatform": "android-x64",
                    "emulator": true
                  }
                ]
                """;
        FlutterDeviceService service = new FlutterDeviceService(
                (workingDirectory, timeout, arguments) -> new ProcessResult(0, output, ""));

        var devices = service.list(temporaryDirectory);

        assertEquals(List.of("windows", "legacy"),
                devices.stream().map(device -> device.id()).toList());
    }

    @Test
    void reportsCommandFailureInsteadOfPretendingThereAreNoDevices() {
        FlutterDeviceService service = new FlutterDeviceService(
                (workingDirectory, timeout, arguments) ->
                        new ProcessResult(1, "", "Android toolchain is unavailable"));

        IOException failure = assertThrows(IOException.class,
                () -> service.list(temporaryDirectory));

        assertTrue(failure.getMessage().contains("discover Flutter devices"));
        assertTrue(failure.getMessage().contains(temporaryDirectory.toString()));
        assertTrue(failure.getMessage().contains("Android toolchain is unavailable"));
    }

    @Test
    void reportsMalformedMachineOutput() {
        FlutterDeviceService service = new FlutterDeviceService(
                (workingDirectory, timeout, arguments) -> new ProcessResult(0, "not-json", ""));

        IOException failure = assertThrows(IOException.class,
                () -> service.list(temporaryDirectory));

        assertTrue(failure.getMessage().contains("invalid response"));
        assertTrue(failure.getMessage().contains("not valid JSON"));
    }
}
