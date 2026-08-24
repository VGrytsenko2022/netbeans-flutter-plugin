package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.DartSdk;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevToolsLauncherTest {
    @TempDir
    Path projectRoot;

    @Test
    void startsMachineServerOnAnAutomaticPortWithHttpServiceUri() throws Exception {
        AtomicReference<Path> workingDirectory = new AtomicReference<>();
        AtomicReference<List<String>> arguments = new AtomicReference<>();
        TestFlutterProcess process = new TestFlutterProcess();
        DartSdk sdk = new DartSdk(projectRoot.resolve("dart-sdk"), projectRoot.resolve("dart"));
        DevToolsLauncher launcher = new DevToolsLauncher(sdk, (directory, commandArguments) -> {
            workingDirectory.set(directory);
            arguments.set(commandArguments);
            return process;
        });

        DevToolsSession session = launcher.start(
                projectRoot,
                URI.create("ws://127.0.0.1:4321/token=/ws"));

        assertEquals(projectRoot.toAbsolutePath().normalize(), workingDirectory.get());
        assertEquals(List.of(
                "devtools",
                "--machine",
                "--no-launch-browser",
                "--host=127.0.0.1",
                "--port=0",
                "http://127.0.0.1:4321/token=/"), arguments.get());
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void convertsSecureWebSocketAndPreservesQuery() {
        URI converted = DevToolsLauncher.serviceProtocolUri(
                URI.create("wss://localhost:1234/auth/ws?foo=bar"));

        assertEquals(URI.create("https://localhost:1234/auth/?foo=bar"), converted);
    }

    @Test
    void preservesExistingHttpServiceUri() {
        URI source = URI.create("http://127.0.0.1:4321/token=/");

        assertEquals(source, DevToolsLauncher.serviceProtocolUri(source));
    }

    @Test
    void rejectsRelativeAndHostlessServiceUris() {
        assertThrows(IllegalArgumentException.class,
                () -> DevToolsLauncher.serviceProtocolUri(URI.create("service/token")));
        assertThrows(IllegalArgumentException.class,
                () -> DevToolsLauncher.serviceProtocolUri(URI.create("http:///service/token")));
    }

    @Test
    void rejectsMissingProjectAndUnsupportedServiceSchemeBeforeStarting() {
        AtomicReference<Boolean> started = new AtomicReference<>(false);
        DartSdk sdk = new DartSdk(projectRoot.resolve("dart-sdk"), projectRoot.resolve("dart"));
        DevToolsLauncher launcher = new DevToolsLauncher(sdk, (directory, arguments) -> {
            started.set(true);
            return new TestFlutterProcess();
        });

        assertThrows(IOException.class, () -> launcher.start(
                projectRoot.resolve("missing"),
                URI.create("ws://127.0.0.1:4321/token/ws")));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> launcher.start(projectRoot, URI.create("ftp://127.0.0.1/service")));

        assertTrue(failure.getMessage().contains("ftp"));
        assertEquals(false, started.get());
    }
}
