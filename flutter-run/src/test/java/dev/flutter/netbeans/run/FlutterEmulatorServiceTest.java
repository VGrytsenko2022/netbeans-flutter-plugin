package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class FlutterEmulatorServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesFlutterPlainTextEmulatorTable() throws Exception {
        String output = """
                2 available emulators:

                Id               • Name             • Manufacturer • Platform

                Galaxy_S24_Ultra • Galaxy S24 Ultra • User         • android
                ios_simulator    • iPhone 16 Pro    • Apple        • ios

                To run an emulator, run 'flutter emulators --launch <emulator id>'.
                """;
        FlutterEmulatorService service = new FlutterEmulatorService(
                (workingDirectory, timeout, arguments) -> {
                    assertEquals(Duration.ofSeconds(30), timeout);
                    assertEquals(List.of("emulators"), List.of(arguments));
                    return new ProcessResult(0, output, "");
                });

        var emulators = service.list(temporaryDirectory);

        assertEquals(2, emulators.size());
        assertEquals(new FlutterEmulator(
                "Galaxy_S24_Ultra", "Galaxy S24 Ultra", "User", "android"), emulators.get(0));
        assertEquals("ios_simulator", emulators.get(1).id());
        assertEquals("ios", emulators.get(1).platform());
    }

    @Test
    void returnsEmptyListWhenFlutterReportsNoEmulators() throws Exception {
        FlutterEmulatorService service = new FlutterEmulatorService(
                (workingDirectory, timeout, arguments) ->
                        new ProcessResult(0, "No emulators available.\n", ""));

        assertTrue(service.list(temporaryDirectory).isEmpty());
    }

    @Test
    void launchesExactEmulatorId() throws Exception {
        AtomicReference<List<String>> invokedArguments = new AtomicReference<>();
        FlutterEmulatorService service = new FlutterEmulatorService(
                (workingDirectory, timeout, arguments) -> {
                    assertEquals(temporaryDirectory.toAbsolutePath().normalize(), workingDirectory);
                    assertEquals(Duration.ofMinutes(2), timeout);
                    invokedArguments.set(List.of(arguments));
                    return new ProcessResult(0, "The Android emulator exited successfully", "");
                });

        service.launch(temporaryDirectory, " Galaxy_S24_Ultra ");

        assertEquals(List.of("emulators", "--launch", "Galaxy_S24_Ultra"),
                invokedArguments.get());
    }

    @Test
    void reportsLaunchFailureWithEmulatorAndCause() {
        FlutterEmulatorService service = new FlutterEmulatorService(
                (workingDirectory, timeout, arguments) ->
                        new ProcessResult(1, "", "Emulator did not start"));

        IOException failure = assertThrows(IOException.class,
                () -> service.launch(temporaryDirectory, "Galaxy_S24_Ultra"));

        assertTrue(failure.getMessage().contains("Galaxy_S24_Ultra"));
        assertTrue(failure.getMessage().contains("Emulator did not start"));
    }

    @Test
    void rejectsFlutterNoMatchMessageEvenWhenCommandExitsWithZero() {
        FlutterEmulatorService service = new FlutterEmulatorService(
                (workingDirectory, timeout, arguments) ->
                        new ProcessResult(0, "No emulator found that matches 'missing_avd'.", ""));

        IOException failure = assertThrows(IOException.class,
                () -> service.launch(temporaryDirectory, "missing_avd"));

        assertTrue(failure.getMessage().contains("missing_avd"));
        assertTrue(failure.getMessage().contains("exit code 0"));
        assertTrue(failure.getMessage().contains("No emulator found"));
    }
}
