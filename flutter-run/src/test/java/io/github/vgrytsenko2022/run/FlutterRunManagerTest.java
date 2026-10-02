package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterRunManagerTest {
    @TempDir
    Path projectRoot;

    @Test
    void runStartsMachineDebugBuildWithoutPausing() throws Exception {
        AtomicReference<Path> workingDirectory = new AtomicReference<>();
        AtomicReference<List<String>> arguments = new AtomicReference<>();
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunManager manager = new FlutterRunManager((directory, commandArguments) -> {
            workingDirectory.set(directory);
            arguments.set(commandArguments);
            return process;
        });

        FlutterRunSession session = manager.run(projectRoot, " windows ");

        assertEquals(projectRoot.toAbsolutePath().normalize(), workingDirectory.get());
        assertEquals(List.of(
                "run", "--machine", "--debug", "--device-id", "windows"), arguments.get());
        assertFalse(arguments.get().contains("--start-paused"));
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void debugStartsPausedMachineSession() throws Exception {
        AtomicReference<List<String>> arguments = new AtomicReference<>();
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunManager manager = new FlutterRunManager((directory, commandArguments) -> {
            arguments.set(commandArguments);
            return process;
        });

        FlutterRunSession session = manager.debug(projectRoot, "emulator-5554");

        assertEquals(List.of(
                "run", "--machine", "--debug", "--device-id", "emulator-5554",
                "--start-paused"), arguments.get());
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void refusesMissingProjectOrDeviceBeforeStartingProcess() {
        List<List<String>> invocations = new ArrayList<>();
        FlutterRunManager manager = new FlutterRunManager((directory, arguments) -> {
            invocations.add(arguments);
            return new TestFlutterProcess();
        });

        assertThrows(IOException.class,
                () -> manager.run(projectRoot.resolve("missing"), "windows"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.run(projectRoot, "  "));
        assertTrue(invocations.isEmpty());
    }
}
