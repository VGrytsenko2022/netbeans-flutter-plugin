package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
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
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessPayloadDescriptor;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerBuildResult;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerCacheIdentity;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class FlutterDesignerNativeCanvasSessionProtocolTest {
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework-revision", "engine-revision", "3.10.7");
    private static final List<CanvasWireCapability> CAPABILITIES = List.of(
            CanvasWireCapability.READ_ONLY_RENDER,
            CanvasWireCapability.READ_ONLY_LAYOUT,
            CanvasWireCapability.READ_ONLY_SELECTION);
    private static final StableId DOCUMENT_A = StableId.parse(
            "83ed3c05-88e7-4220-8377-29fa1f21a99e");
    private static final StableId DOCUMENT_B = StableId.parse(
            "32e43896-5be1-46ad-9e29-b9bf976da690");
    private static final StableId ROOT = StableId.parse(
            "5b814fc1-ecc1-4255-898d-3111f10673a4");
    private static final StableId CHILD = StableId.parse(
            "9dd9e5e0-5364-4dcc-bd82-1a86345cd522");

    @Test
    void presentationSequenceRemainsMonotonicAcrossDocumentAtoBtoA()
            throws Exception {
        DesignerDocument documentA = document(DOCUMENT_A, ROOT, null);
        DesignerDocument documentB = document(DOCUMENT_B, ROOT, null);
        Harness harness = Harness.start(documentA);
        try {
            RenderPublication first = harness.nextRender();
            onEdt(() -> harness.session.present(
                    documentB,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.WINDOWS));
            RenderPublication second = harness.nextRender();
            onEdt(() -> harness.session.present(
                    documentA,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.WINDOWS));
            RenderPublication third = harness.nextRender();

            assertEquals(first.revision().sessionId(), second.revision().sessionId());
            assertEquals(first.revision().sessionId(), third.revision().sessionId());
            assertEquals(List.of(0L, 1L, 2L), List.of(
                    first.revision().presentationSequence(),
                    second.revision().presentationSequence(),
                    third.revision().presentationSequence()));
            assertEquals(List.of(DOCUMENT_A, DOCUMENT_B, DOCUMENT_A), List.of(
                    first.revision().documentId(),
                    second.revision().documentId(),
                    third.revision().documentId()));
        } finally {
            harness.close();
        }
    }

    @Test
    void republishesTheExactAdaptiveTargetWhenTheViewportModeIsUnchanged()
            throws Exception {
        DesignerDocument document = document(DOCUMENT_A, ROOT, null);
        Harness harness = Harness.start(document);
        try {
            harness.nextRender();
            onEdt(() -> harness.session.present(
                    document,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.MOBILE,
                    CanvasTargetPlatform.ANDROID));
            RenderPublication android = harness.nextRender();
            onEdt(() -> harness.session.present(
                    document,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.MOBILE,
                    CanvasTargetPlatform.IOS));
            RenderPublication ios = harness.nextRender();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode androidProfile = mapper.readTree(android.model()).path("profile");
            JsonNode iosProfile = mapper.readTree(ios.model()).path("profile");
            assertEquals("mobile", androidProfile.path("previewMode").asText());
            assertEquals("android", androidProfile.path("targetPlatform").asText());
            assertEquals("mobile", iosProfile.path("previewMode").asText());
            assertEquals("ios", iosProfile.path("targetPlatform").asText());
            assertEquals(
                    android.revision().presentationSequence() + 1,
                    ios.revision().presentationSequence());
        } finally {
            harness.close();
        }
    }

    @Test
    void staleIntentIsConsumedAndRetainedSelectionIsRestoredAfterPresented()
            throws Exception {
        DesignerDocument document = document(DOCUMENT_A, ROOT, CHILD);
        Harness harness = Harness.start(document);
        try {
            RenderPublication first = harness.nextRender();
            CanvasLayoutKey firstLayout = layout(first.revision());
            harness.process.sendPresented(firstLayout);
            HostSelection initialSelection = harness.process.readSelection();
            assertEquals(ROOT, initialSelection.widgetId());
            assertEquals(firstLayout, initialSelection.layout());

            onEdt(() -> harness.session.selectWidget(CHILD));
            HostSelection explicitSelection = harness.process.readSelection();
            assertEquals(CHILD, explicitSelection.widgetId());
            assertEquals(firstLayout, explicitSelection.layout());

            onEdt(() -> harness.session.present(
                    document,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.WINDOWS));
            RenderPublication second = harness.nextRender();
            CanvasLayoutKey secondLayout = layout(second.revision());

            harness.process.sendSelection(firstLayout, 0, ROOT);
            drainEdt();
            assertTrue(harness.runnerSelections.isEmpty());

            harness.process.sendPresented(secondLayout);
            HostSelection restoredSelection = harness.process.readSelection();
            assertEquals(CHILD, restoredSelection.widgetId());
            assertEquals(secondLayout, restoredSelection.layout());

            harness.process.sendSelection(secondLayout, 1, CHILD);
            awaitEdtCondition(
                    () -> harness.runnerSelections.equals(List.of(CHILD)),
                    "current intent was wedged behind stale intent sequence zero");
        } finally {
            harness.close();
        }
    }

    private static CanvasLayoutKey layout(CanvasRevisionKey revision) {
        return new CanvasLayoutKey(new CanvasFrameKey(revision, 0), 0);
    }

    private static DesignerDocument document(
            StableId documentId,
            StableId rootId,
            StableId childId) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("0.1.3-SNAPSHOT"),
                new ManagedRegions(region, region));
        WidgetNode root;
        if (childId == null) {
            root = WidgetNode.empty(
                    rootId, new WidgetTypeId("flutter.material.Scaffold"));
        } else {
            WidgetNode child = WidgetNode.empty(
                    childId, new WidgetTypeId("flutter.widgets.Center"));
            root = new WidgetNode(
                    rootId,
                    new WidgetTypeId("flutter.material.Scaffold"),
                    Map.of(),
                    Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(child)));
        }
        return new DesignerDocument(documentId, source, root);
    }

    private static void dispatchUi(Runnable task) {
        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            EventQueue.invokeLater(task);
        }
    }

    private static void drainEdt() throws Exception {
        onEdt(() -> { });
    }

    private static void awaitEdtCondition(
            BooleanSupplier condition,
            String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (onEdt(condition::getAsBoolean)) {
                return;
            }
            Thread.sleep(5);
        }
        assertTrue(onEdt(condition::getAsBoolean), message);
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        onEdt(() -> {
            runnable.run();
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return callable.call();
        }
        Holder<T> result = new Holder<>();
        Holder<Throwable> failure = new Holder<>();
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    result.value = callable.call();
                } catch (Throwable throwable) {
                    failure.value = throwable;
                }
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return result.value;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class Harness implements AutoCloseable {
        private final ProtocolProcess process = new ProtocolProcess(7701L);
        private final FakeHost host = new FakeHost();
        private final ManualExecutor launches = new ManualExecutor();
        private final ManualPollScheduler polls = new ManualPollScheduler();
        private final List<StableId> runnerSelections = new ArrayList<>();
        private final CanvasRunnerBuildResult runner;
        private final FlutterDesignerNativeCanvasSession session;

        private Harness() {
            Path root = Path.of("target", "native-canvas-session-protocol-test")
                    .toAbsolutePath().normalize();
            Path sdkHome = root.resolve("flutter-sdk");
            FlutterSdk sdk = new FlutterSdk(
                    sdkHome, sdkHome.resolve("bin").resolve("flutter.bat"));
            runner = new CanvasRunnerBuildResult(
                    root.resolve("runner.exe"),
                    root,
                    new CanvasRunnerCacheIdentity(
                            "a".repeat(64), "test-engine", "b".repeat(64)),
                    false,
                    "");
            FlutterDesignerNativeCanvasSession.RuntimeServices runtime =
                    new FlutterDesignerNativeCanvasSession.RuntimeServices(
                            () -> new FlutterDesignerNativeCanvasSession.SdkResolution(
                                    sdk, "test Flutter SDK"),
                            ignored -> CompletableFuture.completedFuture(runner),
                            (command, workingDirectory) -> process,
                            launches,
                            FlutterDesignerNativeCanvasSessionProtocolTest::dispatchUi,
                            polls,
                            Process::destroy);
            session = new FlutterDesignerNativeCanvasSession(
                    host, runtime, ignored -> { }, runnerSelections::add);
        }

        static Harness start(DesignerDocument document) throws Exception {
            Harness harness = onEdt(Harness::new);
            onEdt(() -> {
                harness.session.show();
                harness.session.present(
                        document,
                        BuiltInWidgetCatalog.getDefault(),
                        CanvasPreviewMode.DESKTOP,
                        CanvasTargetPlatform.WINDOWS);
                harness.launches.runNext();
                harness.polls.runActivePoll();
            });
            harness.process.handshake();
            return harness;
        }

        RenderPublication nextRender() throws Exception {
            awaitEdtCondition(
                    () -> launches.pendingCount() > 0,
                    "model encoding was not scheduled after protocol readiness");
            onEdt(launches::runNext);
            return process.readRender();
        }

        @Override
        public void close() throws Exception {
            onEdt(session::close);
        }
    }

    private static final class FakeHost
            implements FlutterDesignerNativeCanvasSession.NativeCanvasHost {
        private boolean attached;

        @Override
        public long parentWindowHandle() {
            return 0x771L;
        }

        @Override
        public boolean tryAttach(long runnerProcessId) {
            attached = true;
            return true;
        }

        @Override
        public boolean isAttached() {
            return attached;
        }

        @Override
        public boolean isNativePeerReady() {
            return true;
        }

        @Override
        public void setRunnerVisible(boolean visible) {
        }

        @Override
        public void onPeerReady(Runnable listener) {
        }

        @Override
        public void onPeerLost(Runnable listener) {
        }

        @Override
        public void onAttachmentFailed(Consumer<String> listener) {
        }

        @Override
        public void close() {
            attached = false;
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> pending = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pending.addLast(command);
        }

        int pendingCount() {
            return pending.size();
        }

        void runNext() {
            Runnable task = pending.pollFirst();
            if (task == null) {
                throw new AssertionError("No pending background task");
            }
            task.run();
        }
    }

    private static final class ManualPollScheduler
            implements FlutterDesignerNativeCanvasSession.PollScheduler {
        private Runnable active;

        @Override
        public FlutterDesignerNativeCanvasSession.Cancellable schedule(Runnable poll) {
            active = poll;
            return () -> active = null;
        }

        void runActivePoll() {
            if (active == null) {
                throw new AssertionError("No active attachment poll");
            }
            active.run();
        }
    }

    private record RenderPublication(CanvasRevisionKey revision, byte[] model) {
        private RenderPublication {
            model = model.clone();
        }
    }

    private record HostSelection(CanvasLayoutKey layout, StableId widgetId) {
    }

    private static final class ProtocolProcess extends Process {
        private final long pid;
        private final java.io.PipedInputStream hostStdout;
        private final java.io.PipedOutputStream runnerStdout;
        private final java.io.PipedInputStream runnerStdin;
        private final java.io.PipedOutputStream hostStdin;
        private final CanvasProcessFrameReader hostFrames;
        private final CanvasProcessFrameWriter runnerFrames;
        private final CompletableFuture<Process> onExit = new CompletableFuture<>();
        private final ObjectMapper json = new ObjectMapper();
        private CanvasSessionId sessionId;
        private boolean alive = true;
        private int exitCode;

        private ProtocolProcess(long pid) {
            this.pid = pid;
            try {
                hostStdout = new java.io.PipedInputStream(1024 * 1024);
                runnerStdout = new java.io.PipedOutputStream(hostStdout);
                runnerStdin = new java.io.PipedInputStream(1024 * 1024);
                hostStdin = new java.io.PipedOutputStream(runnerStdin);
            } catch (IOException failure) {
                throw new AssertionError(failure);
            }
            CanvasProcessFrameCodec codec = new CanvasProcessFrameCodec();
            hostFrames = codec.reader(runnerStdin);
            runnerFrames = codec.writer(runnerStdout);
        }

        void handshake() throws Exception {
            CanvasProcessFrame helloFrame = hostFrames.read(handshakePolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER)).orElseThrow();
            CanvasWireDecodeResult decoded = new CanvasWireCodec().decode(
                    helloFrame.copyPayload());
            CanvasHostHello hello = (CanvasHostHello)
                    ((CanvasWireDecodeResult.Decoded) decoded).message();
            sessionId = hello.sessionId();
            runnerFrames.write(
                    handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(new CanvasWireCodec().encode(new CanvasRunnerHello(
                            sessionId,
                            0,
                            0,
                            "flutter-canvas-runner/1",
                            ENGINE,
                            CAPABILITIES,
                            CanvasWireHandshakeLimits.defaults()))));
        }

        RenderPublication readRender() throws Exception {
            CanvasProcessFrame control = hostFrames.read(negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER)).orElseThrow();
            JsonNode root = json.readTree(control.copyPayload());
            assertEquals("host.render", root.path("type").asText());
            JsonNode body = root.path("body");
            CanvasRevisionKey revision = revision(body);
            JsonNode model = body.path("model");
            CanvasProcessPayloadDescriptor descriptor =
                    new CanvasProcessPayloadDescriptor(
                            CanvasProcessFrameKind.MODEL_JSON,
                            model.path("payloadBytes").intValue(),
                            HexFormat.of().parseHex(model.path("sha256").asText()));
            CanvasProcessFrame payload = hostFrames.read(
                    negotiatedPolicy(CanvasProcessDirection.HOST_TO_RUNNER),
                    descriptor).orElseThrow();
            return new RenderPublication(revision, payload.copyPayload());
        }

        HostSelection readSelection() throws Exception {
            CanvasProcessFrame frame = hostFrames.read(negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER)).orElseThrow();
            JsonNode root = json.readTree(frame.copyPayload());
            assertEquals("host.selection", root.path("type").asText());
            JsonNode body = root.path("body");
            CanvasRevisionKey revision = revision(body);
            CanvasLayoutKey layout = new CanvasLayoutKey(
                    new CanvasFrameKey(
                            revision, body.path("frameSequence").longValue()),
                    body.path("layoutSequence").longValue());
            return new HostSelection(
                    layout, StableId.parse(body.path("widgetId").asText()));
        }

        void sendPresented(CanvasLayoutKey layout) throws Exception {
            sendRuntime(runtimeEnvelope(
                    layout.frameKey().revisionKey(),
                    "runner.presented",
                    "\"frameSequence\":" + layout.frameKey().frameSequence()
                            + ",\"layoutSequence\":" + layout.layoutSequence()));
        }

        void sendSelection(
                CanvasLayoutKey layout,
                long intentSequence,
                StableId widgetId) throws Exception {
            sendRuntime(runtimeEnvelope(
                    layout.frameKey().revisionKey(),
                    "runner.selection",
                    "\"frameSequence\":" + layout.frameKey().frameSequence()
                            + ",\"layoutSequence\":" + layout.layoutSequence()
                            + ",\"intentSequence\":" + intentSequence
                            + ",\"widgetId\":\"" + widgetId + "\""));
        }

        private void sendRuntime(String value) throws Exception {
            runnerFrames.write(
                    negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(value.getBytes(StandardCharsets.UTF_8)));
        }

        private CanvasRevisionKey revision(JsonNode body) {
            return new CanvasRevisionKey(
                    sessionId,
                    body.path("presentationSequence").longValue(),
                    StableId.parse(body.path("documentId").asText()),
                    body.path("logicalRevisionId").longValue());
        }

        @Override
        public OutputStream getOutputStream() {
            return hostStdin;
        }

        @Override
        public InputStream getInputStream() {
            return hostStdout;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            try {
                return onExit.thenApply(ignored -> exitCode).get();
            } catch (ExecutionException failure) {
                throw new AssertionError(failure.getCause());
            }
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit)
                throws InterruptedException {
            try {
                onExit.get(timeout, unit);
                return true;
            } catch (ExecutionException failure) {
                throw new AssertionError(failure.getCause());
            } catch (TimeoutException ignored) {
                return false;
            }
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("process is still alive");
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            if (!alive) {
                return;
            }
            alive = false;
            exitCode = 143;
            onExit.complete(this);
        }

        @Override
        public Process destroyForcibly() {
            destroy();
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public CompletableFuture<Process> onExit() {
            return onExit;
        }
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
                        CAPABILITIES,
                        CanvasWireHandshakeLimits.defaults()),
                direction);
    }

    private static String runtimeEnvelope(
            CanvasRevisionKey revision,
            String type,
            String suffix) {
        return "{\"format\":\"netbeans-flutter-canvas-runtime\""
                + ",\"protocolVersion\":1,\"sessionId\":\""
                + revision.sessionId() + "\",\"type\":\"" + type
                + "\",\"body\":{\"presentationSequence\":"
                + revision.presentationSequence() + ",\"documentId\":\""
                + revision.documentId() + "\",\"logicalRevisionId\":"
                + revision.logicalRevisionId() + ',' + suffix + "}}";
    }
}
