package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasDevicePixelRatio;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasLocale;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasTextScaleFactor;
import dev.flutter.netbeans.designer.canvas.CanvasThemeBrightness;
import dev.flutter.netbeans.designer.canvas.CanvasViewport;
import dev.flutter.netbeans.designer.canvas.ValidatedCanvasRevisionSnapshot;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasHostClose;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasHostHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCapability;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCodec;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireDecodeResult;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireHandshakeLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireNegotiation;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessDirection;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrame;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameCodec;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameKind;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameReader;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameWriter;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFramingPolicy;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CanvasRunnerProcessChannelTest {
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework-revision", "engine-revision", "3.10.7");
    private static final List<CanvasWireCapability> ALL_CAPABILITIES = List.of(
            CanvasWireCapability.READ_ONLY_RENDER,
            CanvasWireCapability.READ_ONLY_LAYOUT,
            CanvasWireCapability.READ_ONLY_SELECTION);

    private Harness harness;

    @AfterEach
    void closeHarness() {
        if (harness != null) {
            harness.close();
        }
    }

    @Test
    void performsVersionOneHandshakeAndEmitsReadyOnTheListenerExecutor()
            throws Exception {
        harness = new Harness();

        harness.channel.start();
        harness.awaitHostFrames(1);
        CanvasHostHello hello = assertInstanceOf(
                CanvasHostHello.class,
                lifecycle(harness.hostFrames().getFirst()));
        assertEquals(harness.sessionId, hello.sessionId());
        assertEquals(0, hello.sequence());
        assertEquals(ALL_CAPABILITIES, hello.requestedCapabilities());

        harness.sendHello(ALL_CAPABILITIES);

        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        assertEquals(ENGINE, harness.listener.engine);
        assertTrue(harness.channel.isReady());
    }

    @Test
    void processConstructorAcceptsTheDebugVmServiceAnnouncementBeforeHello()
            throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        FakeProcess process = new FakeProcess(stdout.input, stdin);
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                process, sessionId, Runnable::run, listener);
        CanvasProcessFrameWriter runner = new CanvasProcessFrameCodec().writer(
                stdout.output);
        try {
            channel.start();
            awaitHostFrames(stdin, 1);
            stdout.output.write(("The Dart VM service is listening on "
                    + "http://127.0.0.1:52834/JIMREaRrfiI=/\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            stdout.output.flush();
            runner.write(
                    handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(new CanvasWireCodec().encode(runnerHello(
                            sessionId, ALL_CAPABILITIES))));

            assertTrue(listener.ready.await(2, TimeUnit.SECONDS));
            assertTrue(channel.isReady());
            assertEquals(null, listener.failureReason);
            assertEquals(0, process.destroyCalls.get());
        } finally {
            channel.close();
        }
    }

    @Test
    void rejectsArbitraryTextBeforeTheFirstFrameWithConcreteFramingError()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);

        harness.sendRawStdout("Flutter engine banner\r\n");

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("invalid_magic"));
        assertTrue(harness.listener.failureReason.contains(
                "unexpected non-protocol bytes"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void acceptsALineFeedTerminatedLoopbackVmServiceAnnouncement()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);

        harness.sendRawStdout(
                "The Dart VM service is listening on "
                + "http://127.0.0.1:52834/JIMREaRrfiI=/\n");
        harness.sendHello(ALL_CAPABILITIES);

        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        assertTrue(harness.channel.isReady());
    }

    @Test
    void acceptsFragmentedVmServiceAnnouncementAndFragmentedHelloFrame()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);
        ByteArrayOutputStream encodedHello = new ByteArrayOutputStream();
        new CanvasProcessFrameCodec().writer(encodedHello).write(
                handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(runnerHello(
                        harness.sessionId, ALL_CAPABILITIES))));
        byte[] banner = ("The Dart VM service is listening on "
                + "http://127.0.0.1:52834/JIMREaRrfiI=/\r\n")
                .getBytes(StandardCharsets.US_ASCII);

        harness.sendFragmentedStdout(banner, encodedHello.toByteArray());

        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        assertTrue(harness.channel.isReady());
    }

    @Test
    void eofAfterPermittedVmServiceAnnouncementFailsTheHandshake()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);

        harness.sendRawStdout(
                "The Dart VM service is listening on "
                + "http://127.0.0.1:52834/JIMREaRrfiI=/\r\n");
        harness.closeRunnerStdout();

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains(
                "stdout ended before the Canvas session closed"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsAnOversizedVmServiceAnnouncementWithoutResynchronizing()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);

        harness.sendRawStdout(
                "The Dart VM service is listening on "
                + "http://127.0.0.1:52834/"
                + "a".repeat(CanvasRunnerProtocolInputStream.MAX_STARTUP_LINE_BYTES)
                + "\r\n");

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("invalid_magic"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsAWebOrForeignVmServiceAnnouncementInsteadOfHidingIt()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);

        harness.sendRawStdout(
                "The Dart VM service is listening on "
                + "https://example.com:52834/SecretAuthCode=/\r\n");

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("invalid_magic"));
        assertFalse(harness.listener.failureReason.contains("example.com"));
        assertFalse(harness.listener.failureReason.contains("SecretAuthCode"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsASecondVmServiceAnnouncementBeforeTheFirstFrame()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);
        String announcement = "The Dart VM service is listening on "
                + "http://127.0.0.1:52834/JIMREaRrfiI=/\r\n";

        harness.sendRawStdout(announcement + announcement);

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("invalid_magic"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void neverResynchronizesPastVmServiceTextAfterProtocolHasStarted()
            throws Exception {
        harness = Harness.ready();

        harness.sendRawStdout(
                "The Dart VM service is listening on "
                + "http://127.0.0.1:52834/JIMREaRrfiI=/\r\n");

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("invalid_magic"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void writesRenderControlAndDeclaredModelAsOneSerializedPair()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        byte[] model = "{\"model\":\"exact\"}".getBytes(StandardCharsets.UTF_8);

        harness.channel.expectPresentation(request.revisionKey());
        assertTrue(harness.channel.present(request, model));

        List<CanvasProcessFrame> frames = harness.awaitHostFramesWithModel(model);
        assertEquals(3, frames.size());
        String control = new String(
                frames.get(1).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(control.contains("\"type\":\"host.render\""));
        assertTrue(control.contains("\"sessionId\":\""
                + harness.sessionId + "\""));
        assertTrue(control.contains("\"presentationSequence\":7"));
        assertArrayEquals(model, frames.get(2).copyPayload());
    }

    @Test
    void dispatchesPresentedAndSelectionOnlyForTheExactSession()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasRevisionKey revision = request.revisionKey();
        StableId widgetId = request.snapshot().document().root().id();

        harness.sendRuntime(presented(revision, 3, 5));
        harness.sendRuntime(selection(revision, 3, 5, 9, widgetId));

        assertTrue(harness.listener.presented.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.selection.await(2, TimeUnit.SECONDS));
        CanvasLayoutKey layout = harness.listener.layout;
        assertEquals(new CanvasLayoutKey(new CanvasFrameKey(revision, 3), 5), layout);
        assertEquals(widgetId, harness.listener.widgetId);
        assertEquals(9, harness.listener.intent.intentId().intentSequence());
        assertTrue(harness.listener.failureReason == null);
    }

    @Test
    void rejectsAWellFormedRuntimeEventFromAnotherSession() throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());

        harness.sendRuntime(presented(request.revisionKey(), 0, 0));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("stale or foreign"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void refusesAHandshakeThatOmitsSelectionCapability() throws Exception {
        harness = new Harness();
        harness.channel.start();

        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains(
                "render, layout, and selection"));
        assertFalse(harness.channel.isReady());
    }

    @Test
    void sendsHostSelectionForTheExactPresentedLayout() throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 4), 6);
        StableId widgetId = request.snapshot().document().root().id();

        harness.channel.expectPresentation(request.revisionKey());
        assertTrue(harness.channel.select(layout, widgetId));

        List<CanvasProcessFrame> frames = harness.awaitHostFrames(2);
        assertEquals(2, frames.size());
        String selection = new String(
                frames.get(1).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(selection.contains("\"type\":\"host.selection\""));
        assertTrue(selection.contains("\"widgetId\":\"" + widgetId + "\""));
        assertTrue(selection.contains("\"frameSequence\":4"));
        assertTrue(selection.contains("\"layoutSequence\":6"));
    }

    @Test
    void rejectsHostRequestsForAnotherSessionBeforeWriting() throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest foreign = renderRequest(CanvasSessionId.random());

        assertThrows(
                IllegalArgumentException.class,
                () -> harness.channel.present(
                        foreign,
                        "{}".getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, harness.awaitHostFrames(1).size());
    }

    @Test
    void closeSendsLifecycleCloseAndNeverDestroysTheProcess() throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        FakeProcess process = new FakeProcess(stdout.input, stdin);
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                process, sessionId, Runnable::run, listener);
        CanvasProcessFrameWriter runner = new CanvasProcessFrameCodec().writer(
                stdout.output);
        channel.start();
        awaitHostFrames(stdin, 1);
        runner.write(
                handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(runnerHello(
                        sessionId, ALL_CAPABILITIES))));
        assertTrue(listener.ready.await(2, TimeUnit.SECONDS));

        channel.close();

        List<CanvasProcessFrame> frames = awaitHostFrames(stdin, 2);
        assertEquals(2, frames.size());
        assertInstanceOf(CanvasHostClose.class, lifecycle(frames.get(1)));
        assertEquals(0, process.destroyCalls.get());
    }

    @Test
    void serialOutboundWriterDropsASupersededRenderAtTheActualWriteFence()
            throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        GateOutputStream stdin = new GateOutputStream();
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout.input, stdin, sessionId, Runnable::run, listener);
        CanvasProcessFrameWriter runner = new CanvasProcessFrameCodec().writer(
                stdout.output);
        try {
            channel.start();
            awaitBytesContaining(stdin, "host.hello");
            runner.write(
                    handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(new CanvasWireCodec().encode(runnerHello(
                            sessionId, ALL_CAPABILITIES))));
            assertTrue(listener.ready.await(2, TimeUnit.SECONDS));

            CanvasRenderRequest initial = renderRequest(sessionId, 1);
            CanvasLayoutKey initialLayout = new CanvasLayoutKey(
                    new CanvasFrameKey(initial.revisionKey(), 0), 0);
            channel.expectPresentation(initial.revisionKey());
            stdin.blockNextWrite();
            assertTimeoutPreemptively(Duration.ofMillis(250), () -> assertTrue(
                    channel.select(
                            initialLayout,
                            initial.snapshot().document().root().id())));
            assertTrue(stdin.writeBlocked.await(2, TimeUnit.SECONDS));

            CanvasRenderRequest stale = renderRequest(sessionId, 7);
            channel.expectPresentation(stale.revisionKey());
            assertTrue(channel.present(
                    stale, "{\"model\":\"stale\"}".getBytes(StandardCharsets.UTF_8)));
            CanvasRenderRequest latest = renderRequest(sessionId, 8);
            channel.expectPresentation(latest.revisionKey());
            assertTrue(channel.present(
                    latest, "{\"model\":\"latest\"}".getBytes(StandardCharsets.UTF_8)));

            stdin.releaseWrite();
            awaitBytesContaining(stdin, "\"presentationSequence\":8");
            String wire = new String(stdin.toByteArray(), StandardCharsets.UTF_8);
            assertEquals(1, occurrences(wire, "\"type\":\"host.render\""));
            assertTrue(wire.contains("\"presentationSequence\":8"));
            assertFalse(wire.contains("\"presentationSequence\":7"));
        } finally {
            stdin.releaseWrite();
            channel.close();
        }
    }

    @Test
    void selectionAndCloseNeverPerformPipeIoOnTheCallingThread()
            throws Exception {
        BlockingPipe stdoutPipe = new BlockingPipe();
        TrackingInputStream stdout = new TrackingInputStream(stdoutPipe.input);
        GateOutputStream stdin = new GateOutputStream();
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout, stdin, sessionId, Runnable::run, listener);
        CanvasProcessFrameWriter runner = new CanvasProcessFrameCodec().writer(
                stdoutPipe.output);
        channel.start();
        awaitBytesContaining(stdin, "host.hello");
        runner.write(
                handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(runnerHello(
                        sessionId, ALL_CAPABILITIES))));
        assertTrue(listener.ready.await(2, TimeUnit.SECONDS));
        CanvasRenderRequest request = renderRequest(sessionId, 3);
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 0), 0);
        channel.expectPresentation(request.revisionKey());

        stdin.blockNextWrite();
        assertTimeoutPreemptively(Duration.ofMillis(250), () -> assertTrue(
                channel.select(layout, request.snapshot().document().root().id())));
        assertTrue(stdin.writeBlocked.await(2, TimeUnit.SECONDS));
        assertTimeoutPreemptively(Duration.ofMillis(250), channel::close);
        assertFalse(stdin.closed.await(100, TimeUnit.MILLISECONDS));

        stdin.releaseWrite();
        assertTrue(stdin.closed.await(2, TimeUnit.SECONDS));
        assertTrue(stdout.closed.await(2, TimeUnit.SECONDS));
        assertTrue(stdin.closeThread.get().getName().startsWith(
                "flutter-canvas-outbound-"));
        assertTrue(stdout.closeThread.get().getName().startsWith(
                "flutter-canvas-outbound-"));
    }

    private static CanvasRenderRequest renderRequest(CanvasSessionId sessionId) {
        return renderRequest(sessionId, 7);
    }

    private static CanvasRenderRequest renderRequest(
            CanvasSessionId sessionId,
            long presentationSequence) {
        StableId documentId = StableId.random();
        StableId rootId = StableId.random();
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("0.1.3-SNAPSHOT"),
                new ManagedRegions(region, region));
        DesignerDocument document = new DesignerDocument(
                documentId,
                source,
                WidgetNode.empty(
                        rootId,
                        new WidgetTypeId("flutter.material.Scaffold")));
        long logicalRevision = 11;
        ValidatedCanvasRevisionSnapshot snapshot =
                ValidatedCanvasRevisionSnapshot.captureReadOnly(
                        logicalRevision,
                        document,
                        BuiltInWidgetCatalog.getDefault(),
                        ValidationLimits.defaults());
        CanvasRenderProfile profile = new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.WINDOWS,
                new CanvasViewport(390, 844),
                new CanvasDevicePixelRatio(1),
                new CanvasResolvedTheme(
                        "material-light-default-v1",
                        CanvasThemeBrightness.LIGHT),
                new CanvasLocale("uk-UA"),
                new CanvasTextScaleFactor(1),
                ENGINE);
        return new CanvasRenderRequest(
                new CanvasRevisionKey(
                        sessionId,
                        presentationSequence,
                        documentId,
                        logicalRevision),
                profile,
                snapshot);
    }

    private static String presented(
            CanvasRevisionKey revision,
            long frame,
            long layout) {
        return runtimeEnvelope(
                revision,
                "runner.presented",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout);
    }

    private static String selection(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long intent,
            StableId widgetId) {
        return runtimeEnvelope(
                revision,
                "runner.selection",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout
                        + ",\"intentSequence\":" + intent
                        + ",\"widgetId\":\"" + widgetId + "\"");
    }

    private static String runtimeEnvelope(
            CanvasRevisionKey revision,
            String type,
            String suffix) {
        return "{\"format\":\"" + CanvasRunnerControlCodec.FORMAT
                + "\",\"protocolVersion\":1,\"sessionId\":\""
                + revision.sessionId() + "\",\"type\":\"" + type
                + "\",\"body\":{\"presentationSequence\":"
                + revision.presentationSequence() + ",\"documentId\":\""
                + revision.documentId() + "\",\"logicalRevisionId\":"
                + revision.logicalRevisionId() + ',' + suffix + "}}";
    }

    private static CanvasRunnerHello runnerHello(
            CanvasSessionId sessionId,
            List<CanvasWireCapability> capabilities) {
        return new CanvasRunnerHello(
                sessionId,
                0,
                0,
                "flutter-canvas-runner/1",
                ENGINE,
                capabilities,
                CanvasWireHandshakeLimits.defaults());
    }

    private static CanvasProcessFrame control(byte[] payload) {
        return new CanvasProcessFrame(CanvasProcessFrameKind.CONTROL_JSON, payload);
    }

    private static CanvasProcessFramingPolicy handshakePolicy(
            CanvasProcessDirection direction) {
        return CanvasProcessFramingPolicy.handshake(
                CanvasWireLimits.defaults(),
                CanvasWireHandshakeLimits.defaults(),
                direction);
    }

    private static CanvasProcessFramingPolicy negotiatedPolicy(
            CanvasProcessDirection direction) {
        return CanvasProcessFramingPolicy.negotiated(
                CanvasWireLimits.defaults(),
                new CanvasWireNegotiation(
                        "flutter-canvas-runner/1",
                        ENGINE,
                        ALL_CAPABILITIES,
                        CanvasWireHandshakeLimits.defaults()),
                direction);
    }

    private static Object lifecycle(CanvasProcessFrame frame) {
        CanvasWireDecodeResult result = new CanvasWireCodec().decode(
                frame.copyPayload());
        return ((CanvasWireDecodeResult.Decoded) result).message();
    }

    private static List<CanvasProcessFrame> readHostFrames(byte[] bytes)
            throws IOException {
        CanvasProcessFrameReader reader = new CanvasProcessFrameCodec().reader(
                new ByteArrayInputStream(bytes));
        java.util.ArrayList<CanvasProcessFrame> frames = new java.util.ArrayList<>();
        while (true) {
            Optional<CanvasProcessFrame> next = reader.read(
                    handshakePolicy(CanvasProcessDirection.HOST_TO_RUNNER));
            if (next.isEmpty()) {
                return List.copyOf(frames);
            }
            frames.add(next.orElseThrow());
        }
    }

    private static List<CanvasProcessFrame> awaitHostFrames(
            ByteArrayOutputStream output,
            int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        Exception lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                List<CanvasProcessFrame> frames = readHostFrames(
                        output.toByteArray());
                if (frames.size() >= expected) {
                    return frames;
                }
            } catch (IOException | RuntimeException failure) {
                lastFailure = failure;
            }
            Thread.sleep(5);
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AssertionError("Timed out waiting for " + expected
                + " host protocol frames");
    }

    private static void awaitBytesContaining(
            GateOutputStream output,
            String expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            String value = new String(
                    output.toByteArray(), StandardCharsets.UTF_8);
            if (value.contains(expected)) {
                return;
            }
            Thread.sleep(5);
        }
        throw new AssertionError("Timed out waiting for outbound bytes: " + expected);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static final class Harness implements AutoCloseable {
        private final CanvasSessionId sessionId = CanvasSessionId.random();
        private final BlockingPipe runnerStdout = new BlockingPipe();
        private final ByteArrayOutputStream hostStdin = new ByteArrayOutputStream();
        private final RecordingListener listener = new RecordingListener();
        private final CanvasRunnerProcessChannel channel =
                new CanvasRunnerProcessChannel(
                        runnerStdout.input,
                        hostStdin,
                        sessionId,
                        Runnable::run,
                        listener);
        private final CanvasProcessFrameWriter runnerWriter =
                new CanvasProcessFrameCodec().writer(runnerStdout.output);

        static Harness ready() throws Exception {
            Harness harness = new Harness();
            harness.channel.start();
            harness.awaitHostFrames(1);
            harness.sendHello(ALL_CAPABILITIES);
            assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
            return harness;
        }

        void sendHello(List<CanvasWireCapability> capabilities) throws Exception {
            runnerWriter.write(
                    handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(new CanvasWireCodec().encode(
                            runnerHello(sessionId, capabilities))));
        }

        void sendRuntime(String json) throws Exception {
            runnerWriter.write(
                    negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(json.getBytes(StandardCharsets.UTF_8)));
        }

        void sendRawStdout(String text) throws IOException {
            runnerStdout.output.write(text.getBytes(StandardCharsets.US_ASCII));
            runnerStdout.output.flush();
        }

        void sendFragmentedStdout(byte[]... parts) throws IOException {
            for (byte[] part : parts) {
                for (byte value : part) {
                    runnerStdout.output.write(value);
                    runnerStdout.output.flush();
                }
            }
        }

        void closeRunnerStdout() throws IOException {
            runnerStdout.output.close();
        }

        List<CanvasProcessFrame> hostFrames() throws IOException {
            return readHostFrames(hostStdin.toByteArray());
        }

        List<CanvasProcessFrame> awaitHostFrames(int expected) throws Exception {
            return CanvasRunnerProcessChannelTest.awaitHostFrames(
                    hostStdin, expected);
        }

        List<CanvasProcessFrame> hostFramesWithModel(byte[] model)
                throws IOException {
            CanvasProcessFrameReader hostReader =
                    new CanvasProcessFrameCodec().reader(
                            new ByteArrayInputStream(hostStdin.toByteArray()));
            CanvasProcessFramingPolicy handshake = handshakePolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER);
            CanvasProcessFramingPolicy negotiated = negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER);
            CanvasProcessFrame hello = hostReader.read(handshake).orElseThrow();
            CanvasProcessFrame render = hostReader.read(negotiated).orElseThrow();
            CanvasProcessFrame expected = new CanvasProcessFrame(
                    CanvasProcessFrameKind.MODEL_JSON, model);
            CanvasProcessFrame payload = hostReader.read(
                    negotiated, expected.descriptor()).orElseThrow();
            assertTrue(hostReader.read(negotiated).isEmpty());
            return List.of(hello, render, payload);
        }

        List<CanvasProcessFrame> awaitHostFramesWithModel(byte[] model)
                throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            Exception lastFailure = null;
            while (System.nanoTime() < deadline) {
                try {
                    return hostFramesWithModel(model);
                } catch (IOException | RuntimeException failure) {
                    lastFailure = failure;
                }
                Thread.sleep(5);
            }
            if (lastFailure != null) {
                throw lastFailure;
            }
            throw new AssertionError("Timed out waiting for model frame");
        }

        @Override
        public void close() {
            channel.close();
        }
    }

    private static final class RecordingListener
            implements CanvasRunnerProcessChannel.Listener {
        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch presented = new CountDownLatch(1);
        private final CountDownLatch selection = new CountDownLatch(1);
        private final CountDownLatch failed = new CountDownLatch(1);
        private volatile CanvasEngineIdentity engine;
        private volatile CanvasLayoutKey layout;
        private volatile CanvasIntentKey intent;
        private volatile StableId widgetId;
        private volatile String failureReason;

        @Override
        public void ready(CanvasEngineIdentity value) {
            engine = value;
            ready.countDown();
        }

        @Override
        public void presented(CanvasLayoutKey value) {
            layout = value;
            presented.countDown();
        }

        @Override
        public void selection(CanvasIntentKey value, StableId selectedWidgetId) {
            intent = value;
            widgetId = selectedWidgetId;
            selection.countDown();
        }

        @Override
        public void failed(String reason) {
            failureReason = reason;
            failed.countDown();
        }
    }

    private static final class BlockingPipe {
        private final java.io.PipedInputStream input;
        private final java.io.PipedOutputStream output;

        private BlockingPipe() {
            try {
                input = new java.io.PipedInputStream(64 * 1024);
                output = new java.io.PipedOutputStream(input);
            } catch (IOException failure) {
                throw new AssertionError(failure);
            }
        }
    }

    private static final class GateOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private final AtomicReference<Thread> closeThread = new AtomicReference<>();
        private final CountDownLatch writeBlocked = new CountDownLatch(1);
        private final CountDownLatch releaseWrite = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private volatile boolean blockNextWrite;

        void blockNextWrite() {
            blockNextWrite = true;
        }

        void releaseWrite() {
            releaseWrite.countDown();
        }

        byte[] toByteArray() {
            synchronized (delegate) {
                return delegate.toByteArray();
            }
        }

        @Override
        public void write(int value) throws IOException {
            awaitReleaseIfArmed();
            synchronized (delegate) {
                delegate.write(value);
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            awaitReleaseIfArmed();
            synchronized (delegate) {
                delegate.write(bytes, offset, length);
            }
        }

        @Override
        public void close() {
            closeThread.compareAndSet(null, Thread.currentThread());
            closed.countDown();
        }

        private void awaitReleaseIfArmed() throws IOException {
            if (!blockNextWrite) {
                return;
            }
            blockNextWrite = false;
            writeBlocked.countDown();
            try {
                if (!releaseWrite.await(2, TimeUnit.SECONDS)) {
                    throw new IOException("Timed out waiting to release test output");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while blocking test output", failure);
            }
        }
    }

    private static final class TrackingInputStream extends FilterInputStream {
        private final AtomicReference<Thread> closeThread = new AtomicReference<>();
        private final CountDownLatch closed = new CountDownLatch(1);

        private TrackingInputStream(InputStream delegate) {
            super(delegate);
        }

        @Override
        public void close() throws IOException {
            closeThread.compareAndSet(null, Thread.currentThread());
            try {
                super.close();
            } finally {
                closed.countDown();
            }
        }
    }

    private static final class FakeProcess extends Process {
        private final InputStream stdout;
        private final OutputStream stdin;
        private final AtomicInteger destroyCalls = new AtomicInteger();

        private FakeProcess(InputStream stdout, OutputStream stdin) {
            this.stdout = stdout;
            this.stdin = stdin;
        }

        @Override
        public OutputStream getOutputStream() {
            return stdin;
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalls.incrementAndGet();
        }
    }
}
