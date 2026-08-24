package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.api.RunState;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class FlutterRunSessionTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void followsMachineLifecycleAndPublishesVmServiceAndOutput() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", true);
        List<RunState> states = new CopyOnWriteArrayList<>();
        List<String> output = new CopyOnWriteArrayList<>();
        session.addStateListener(states::add);
        session.addOutputListener(output::add);

        process.emitStdout("Launching lib/main.dart on Windows...");
        process.emitStderr("native diagnostic");
        process.emitStdout("[{\"event\":\"app.start\",\"params\":{\"appId\":\"app-1\",\"deviceId\":\"windows\"}}]");
        process.emitStdout("[{\"event\":\"app.debugPort\",\"params\":{\"appId\":\"app-1\",\"wsUri\":\"ws://127.0.0.1:4321/token/ws\"}}]");
        process.emitStdout("[{\"event\":\"app.log\",\"params\":{\"appId\":\"app-1\",\"log\":\"hello from app\"}}]");
        process.emitStdout("[{\"event\":\"app.started\",\"params\":{\"appId\":\"app-1\"}}]");

        await(() -> session.state() == RunState.RUNNING);
        assertEquals(URI.create("ws://127.0.0.1:4321/token/ws"),
                session.vmServiceUri().get(2, TimeUnit.SECONDS));
        assertEquals(URI.create("ws://127.0.0.1:4321/token/ws"),
                session.currentVmServiceUri().orElseThrow());
        await(() -> output.contains("hello from app") && output.contains("native diagnostic"));
        assertTrue(output.contains("Launching lib/main.dart on Windows..."));
        assertEquals(List.of(RunState.STARTING, RunState.RUNNING), states);

        process.finish(0);

        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
        assertEquals(RunState.STOPPED, session.state());
        assertEquals(List.of(RunState.STARTING, RunState.RUNNING, RunState.STOPPED), states);
    }

    @Test
    void publishesVmServiceWhenDebugPortArrivesAfterAppStarted() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", true);

        process.emitStdout("[{\"event\":\"app.start\",\"params\":{\"appId\":\"app-1\"}}]");
        process.emitStdout("[{\"event\":\"app.started\",\"params\":{\"appId\":\"app-1\"}}]");

        await(() -> session.state() == RunState.RUNNING);
        assertTrue(session.currentVmServiceUri().isEmpty());

        URI expected = URI.create("ws://127.0.0.1:4321/token/ws");
        process.emitStdout("[{\"event\":\"app.debugPort\",\"params\":{\"appId\":\"app-1\","
                + "\"wsUri\":\"" + expected + "\"}}]");

        assertEquals(expected, session.vmServiceUri().get(2, TimeUnit.SECONDS));
        assertEquals(expected, session.currentVmServiceUri().orElseThrow());
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void ignoresInvalidVmServiceUriAndAcceptsLaterValidValue() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", true);
        List<String> output = new CopyOnWriteArrayList<>();
        session.addOutputListener(output::add);

        process.emitStdout("[{\"event\":\"app.debugPort\",\"params\":{\"appId\":\"app-1\","
                + "\"wsUri\":\"ws://[invalid\"}}]");

        await(() -> output.stream().anyMatch(line -> line.contains("invalid VM service URI")));
        assertTrue(session.currentVmServiceUri().isEmpty());

        URI expected = URI.create("ws://127.0.0.1:4321/token/ws");
        process.emitStdout("[{\"event\":\"app.debugPort\",\"params\":{\"appId\":\"app-1\","
                + "\"wsUri\":\"" + expected + "\"}}]");

        assertEquals(expected, session.vmServiceUri().get(2, TimeUnit.SECONDS));
        assertEquals(expected, session.currentVmServiceUri().orElseThrow());
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void encodesReloadRestartAndStopAsCorrelatedJsonRequests() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "emulator-5554", true);
        startApp(process, "app-debug");
        await(() -> session.state() == RunState.RUNNING);

        session.hotReload();
        session.hotRestart();
        await(() -> process.stdinText().lines().count() == 2);

        List<String> requests = process.stdinText().lines().toList();
        JsonNode reload = request(requests.get(0));
        JsonNode restart = request(requests.get(1));
        assertEquals("app.restart", reload.path("method").asText());
        assertEquals("app-debug", reload.path("params").path("appId").asText());
        assertFalse(reload.path("params").path("fullRestart").asBoolean());
        assertTrue(reload.path("params").path("pause").asBoolean());
        assertTrue(restart.path("params").path("fullRestart").asBoolean());
        assertTrue(restart.path("params").path("pause").asBoolean());

        respond(process, reload.path("id").asText(), "{\"code\":0,\"message\":\"Reloaded\"}");
        respond(process, restart.path("id").asText(), "{\"code\":0,\"message\":\"Restarted\"}");
        session.quit();
        await(() -> process.stdinText().lines().count() == 3);
        JsonNode stop = request(process.stdinText().lines().toList().get(2));
        assertEquals("app.stop", stop.path("method").asText());
        assertEquals("app-debug", stop.path("params").path("appId").asText());
        assertEquals(RunState.STOPPING, session.state());

        respond(process, stop.path("id").asText(), "true");
        process.emitStdout("[{\"event\":\"app.stop\",\"params\":{\"appId\":\"app-debug\"}}]");
        Thread.sleep(25);
        assertEquals(RunState.STOPPING, session.state(),
                "app.stop must not advertise STOPPED while the process is alive");
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
        assertEquals(RunState.STOPPED, session.state());
    }

    @Test
    void normalRunDoesNotPauseAfterReloadOrRestart() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", false);
        startApp(process, "app-run");
        await(() -> session.state() == RunState.RUNNING);

        session.hotReload();
        session.hotRestart();
        await(() -> process.stdinText().lines().count() == 2);

        for (String line : process.stdinText().lines().toList()) {
            assertFalse(request(line).path("params").path("pause").asBoolean());
        }
        process.finish(0);
        session.exitCode().get(2, TimeUnit.SECONDS);
    }

    @Test
    void rejectsHotCommandsBeforeAppIsRunning() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", false);

        assertTrue(session.currentVmServiceUri().isEmpty());

        IOException reloadFailure = assertThrows(IOException.class, session::hotReload);
        IOException restartFailure = assertThrows(IOException.class, session::hotRestart);

        assertTrue(reloadFailure.getMessage().contains("STARTING"));
        assertTrue(restartFailure.getMessage().contains("windows"));
        process.finish(0);
        session.exitCode().get(2, TimeUnit.SECONDS);
    }

    @Test
    void marksUnexpectedNonZeroExitAsFailedAndCompletesDebugFutureExceptionally()
            throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", true);
        List<String> output = new CopyOnWriteArrayList<>();
        session.addOutputListener(output::add);

        process.finish(2);

        assertEquals(2, session.exitCode().get(2, TimeUnit.SECONDS));
        assertEquals(RunState.FAILED, session.state());
        assertThrows(ExecutionException.class,
                () -> session.vmServiceUri().get(2, TimeUnit.SECONDS));
        assertTrue(output.stream().anyMatch(line -> line.contains("exited with code 2")));
    }

    @Test
    void drainsLargeOutputEvenWithoutListeners() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", false);
        var writer = CompletableFuture.runAsync(() -> {
            try {
                for (int index = 0; index < 20_000; index++) {
                    process.emitStdout("log line " + index + " " + "x".repeat(80));
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        writer.get(5, TimeUnit.SECONDS);
        process.finish(0);

        assertEquals(0, session.exitCode().get(5, TimeUnit.SECONDS));
    }

    @Test
    void quitDuringStartupTerminatesProcessAndIsIdempotent() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        FlutterRunSession session = new FlutterRunSession(process, "windows", false);

        session.quit();
        session.quit();

        assertEquals(143, session.exitCode().get(2, TimeUnit.SECONDS));
        assertEquals(RunState.STOPPED, session.state());
    }

    private static void startApp(TestFlutterProcess process, String appId) throws IOException {
        process.emitStdout("[{\"event\":\"app.start\",\"params\":{\"appId\":\""
                + appId + "\"}}]");
        process.emitStdout("[{\"event\":\"app.started\",\"params\":{\"appId\":\""
                + appId + "\"}}]");
    }

    private static JsonNode request(String line) throws IOException {
        JsonNode envelope = JSON.readTree(line);
        assertTrue(envelope.isArray());
        assertEquals(1, envelope.size());
        return envelope.get(0);
    }

    private static void respond(TestFlutterProcess process, String id, String result)
            throws IOException {
        process.emitStdout("[{\"id\":\"" + id + "\",\"result\":" + result + "}]");
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "Condition was not met before timeout");
    }
}
