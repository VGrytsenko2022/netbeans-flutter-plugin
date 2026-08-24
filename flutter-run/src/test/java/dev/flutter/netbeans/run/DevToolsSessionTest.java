package dev.flutter.netbeans.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class DevToolsSessionTest {
    private static final URI VM_SERVICE = URI.create("http://127.0.0.1:4321/token=/");

    @Test
    void parsesMachineEndpointAndBuildsConnectedBrowserUri() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        DevToolsSession session = new DevToolsSession(process, VM_SERVICE);

        process.emitStdout("{\"event\":\"server.dtdStarted\",\"params\":{\"uri\":\"ws://127.0.0.1:6000/token\"}}");
        process.emitStdout("{\"event\":\"server.started\",\"method\":\"server.started\","
                + "\"params\":{\"host\":\"127.0.0.1\",\"port\":61392,\"pid\":42}}");

        URI browser = session.browserUri().get(2, TimeUnit.SECONDS);
        assertEquals("http", browser.getScheme());
        assertEquals("127.0.0.1", browser.getHost());
        assertEquals(61392, browser.getPort());
        assertTrue(browser.getRawQuery().startsWith("uri="));
        assertEquals(VM_SERVICE.toString(), URLDecoder.decode(
                browser.getRawQuery().substring("uri=".length()), StandardCharsets.UTF_8));

        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void acceptsLegacyMethodOnlyServerStartedMessage() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        DevToolsSession session = new DevToolsSession(process, VM_SERVICE);

        process.emitStdout("{\"method\":\"server.started\","
                + "\"params\":{\"host\":\"127.0.0.1\",\"port\":61392,\"pid\":42}}");

        URI browser = session.browserUri().get(2, TimeUnit.SECONDS);
        assertEquals("http", browser.getScheme());
        assertEquals("127.0.0.1", browser.getHost());
        assertEquals(61392, browser.getPort());
        process.finish(0);
        assertEquals(0, session.exitCode().get(2, TimeUnit.SECONDS));
    }

    @Test
    void forwardsHumanOutputAndStderrButConsumesKnownMachineEvents() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        DevToolsSession session = new DevToolsSession(process, VM_SERVICE);
        List<String> output = new CopyOnWriteArrayList<>();

        process.emitStdout("DevTools diagnostic");
        process.emitStderr("DevTools warning");
        process.emitStdout("{\"event\":\"server.dtdStarted\",\"params\":{}}");
        Thread.sleep(25);
        session.addOutputListener(output::add);

        await(() -> output.size() == 2);
        assertTrue(output.contains("DevTools diagnostic"));
        assertTrue(output.contains("DevTools warning"));
        process.finish(0);
        session.exitCode().get(2, TimeUnit.SECONDS);
    }

    @Test
    void reportsInvalidEndpointAndExitBeforeReady() throws Exception {
        TestFlutterProcess invalidProcess = new TestFlutterProcess();
        DevToolsSession invalid = new DevToolsSession(invalidProcess, VM_SERVICE);
        invalidProcess.emitStdout("{\"event\":\"server.started\",\"params\":{\"host\":\"\",\"port\":0}}");

        ExecutionException invalidEndpoint = assertThrows(
                ExecutionException.class,
                () -> invalid.browserUri().get(2, TimeUnit.SECONDS));
        assertTrue(invalidEndpoint.getCause().getMessage().contains("invalid server endpoint"));
        invalidProcess.finish(2);
        invalid.exitCode().get(2, TimeUnit.SECONDS);

        TestFlutterProcess earlyExitProcess = new TestFlutterProcess();
        DevToolsSession earlyExit = new DevToolsSession(earlyExitProcess, VM_SERVICE);
        earlyExitProcess.finish(7);

        ExecutionException earlyExitFailure = assertThrows(
                ExecutionException.class,
                () -> earlyExit.browserUri().get(2, TimeUnit.SECONDS));
        assertTrue(earlyExitFailure.getCause().getMessage().contains("exited with code 7"));
    }

    @Test
    void stopIsIdempotentAndTerminatesTheProcess() throws Exception {
        TestFlutterProcess process = new TestFlutterProcess();
        DevToolsSession session = new DevToolsSession(process, VM_SERVICE);

        session.stop();
        session.stop();

        assertTrue(session.stopRequested());
        assertEquals(143, session.exitCode().get(2, TimeUnit.SECONDS));
        assertFalse(session.isAlive());
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "Condition was not met before timeout");
    }
}
