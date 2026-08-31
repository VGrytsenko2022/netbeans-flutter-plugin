package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasSurfaceMetrics;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasHostHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCodec;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireDecodeResult;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireHandshakeLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireLimits;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessDirection;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrame;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameCodec;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameKind;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameReader;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameWriter;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFramingPolicy;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class FlutterDesignerWebCanvasSessionTest {
    private static final CanvasEngineIdentity EXPECTED_ENGINE =
            new CanvasEngineIdentity(
                    "3.44.8",
                    "expected-framework",
                    "expected-engine",
                    "3.12.2");
    private static final CanvasEngineIdentity DIFFERENT_ENGINE =
            new CanvasEngineIdentity(
                    "3.44.8",
                    "different-framework",
                    "different-engine",
                    "3.12.2");
    private static final FlutterSdk SDK = sdk();

    @Test
    void constructionAndPublicOperationsRequireTheEdt() throws Exception {
        FakeRuntime runtime = new FakeRuntime();
        List<FlutterDesignerNativeCanvasStatus> statuses = new ArrayList<>();

        assertFalse(EventQueue.isDispatchThread());
        assertThrows(IllegalStateException.class,
                () -> construct(runtime, availableSdk(), statuses));

        FlutterDesignerWebCanvasSession session = onEdt(
                () -> construct(runtime, availableSdk(), statuses));
        try {
            assertThrows(IllegalStateException.class, session::component);
            assertThrows(IllegalStateException.class, session::show);
            assertSame(runtime.component, onEdt(session::component));
        } finally {
            onEdt(session::close);
        }
    }

    @Test
    void unavailableSdkFailsClosedWithoutStartingTheRuntime() throws Exception {
        try (Harness harness = Harness.create(
                new FlutterDesignerWebCanvasSession.SdkResolution(
                        null, "Configure a Flutter SDK first."))) {
            onEdt(() -> {
                harness.session.show();
                harness.session.show();
            });

            assertEquals(0, harness.runtime.startCalls);
            assertEquals(List.of(true, true), harness.runtime.visibility);
            assertEquals(1, harness.statuses.size());
            FlutterDesignerNativeCanvasStatus unavailable =
                    harness.statuses.getFirst();
            assertEquals(FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    unavailable.stage());
            assertTrue(unavailable.summary().contains("unavailable"));
            assertTrue(unavailable.detail().contains(
                    "A Flutter SDK is required"));
            assertTrue(unavailable.detail().contains(
                    "Configure a Flutter SDK first"));
        }
    }

    @Test
    void mismatchedAuthenticatedEngineIdentityFailsClosed() throws Exception {
        try (Harness harness = Harness.create(availableSdk());
                ProtocolBridge bridge = new ProtocolBridge()) {
            onEdt(harness.session::show);
            assertEquals(1, harness.runtime.startCalls);

            onEdt(() -> harness.runtime.fireBridgeReady(
                    EXPECTED_ENGINE,
                    bridge.sessionStdout(),
                    bridge.sessionStdin()));
            bridge.handshake(DIFFERENT_ENGINE);

            awaitEdtCondition(
                    () -> !harness.statuses.isEmpty()
                            && harness.statuses.getLast().stage()
                            == FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                    "engine identity mismatch did not fail the Web session");
            FlutterDesignerNativeCanvasStatus failure =
                    onEdt(harness.statuses::getLast);
            assertTrue(failure.detail().contains(
                    "Authenticate exact Flutter Web Canvas engine"));
            assertTrue(failure.detail().contains(
                    "engine identity differs"));
        }
    }

    @Test
    void visibilityAndFocusAreForwardedThroughTheRuntime() throws Exception {
        try (Harness harness = Harness.create(availableSdk())) {
            onEdt(() -> {
                assertSame(harness.runtime.component,
                        harness.session.component());
                harness.session.show();
                harness.session.requestFocus();
                assertTrue(harness.session.isSurfaceFocused());
                assertTrue(harness.session.releaseSurfaceFocus());
                assertFalse(harness.session.isSurfaceFocused());
                harness.session.hide();
                harness.session.requestFocus();
                harness.session.show();
            });

            assertEquals(1, harness.runtime.startCalls);
            assertSame(SDK, harness.runtime.startedSdk);
            assertTrue(harness.runtime.startOnEdt);
            assertEquals(List.of(true, false, true),
                    harness.runtime.visibility);
            assertEquals(2, harness.runtime.focusRequests);
            assertEquals(1, harness.runtime.focusReleases);
        }
    }

    @Test
    void successfulRetirementWaitsForTheAsynchronousRuntimeBarrier()
            throws Exception {
        try (Harness harness = Harness.create(availableSdk())) {
            CompletableFuture<Void> runtimeBarrier = new CompletableFuture<>();
            harness.runtime.retirements.addLast(runtimeBarrier);

            CompletionStage<Void> first = onEdt(
                    harness.session::preparePeerRemovalAsync);
            CompletionStage<Void> duplicate = onEdt(
                    harness.session::preparePeerRemovalAsync);

            assertSame(first, duplicate);
            assertFalse(first.toCompletableFuture().isDone());
            assertEquals(1, harness.runtime.retirementCalls);

            runtimeBarrier.complete(null);
            first.toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertTrue(first.toCompletableFuture().isDone());
        }
    }

    @Test
    void rejectedRetirementDispatchCompletesExceptionallyAndCanBeRetried()
            throws Exception {
        FakeRuntime runtime = new FakeRuntime();
        List<FlutterDesignerNativeCanvasStatus> statuses = new ArrayList<>();
        AtomicBoolean rejectNextDispatch = new AtomicBoolean(true);
        Executor uiExecutor = task -> {
            if (rejectNextDispatch.compareAndSet(true, false)) {
                throw new RejectedExecutionException(
                        "simulated EDT dispatcher shutdown");
            }
            dispatchUi(task);
        };
        FlutterDesignerWebCanvasSession session = onEdt(() -> construct(
                runtime, availableSdk(), uiExecutor, statuses));
        try {
            CompletionStage<Void> first = onEdt(
                    session::preparePeerRemovalAsync);

            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> first.toCompletableFuture().get(
                            2, TimeUnit.SECONDS));
            assertTrue(failure.getCause().getMessage().contains(
                    "Dispatch exact Flutter Web Canvas retirement on the EDT"));
            assertEquals(0, runtime.retirementCalls);

            CompletionStage<Void> retry = onEdt(
                    session::preparePeerRemovalAsync);
            assertNotSame(first, retry);
            retry.toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertEquals(1, runtime.retirementCalls);
        } finally {
            onEdt(session::close);
        }
    }

    @Test
    void failedAsynchronousRetirementCanBeRetried() throws Exception {
        try (Harness harness = Harness.create(availableSdk())) {
            CompletableFuture<Void> firstBarrier = new CompletableFuture<>();
            CompletableFuture<Void> retryBarrier = new CompletableFuture<>();
            harness.runtime.retirements.addLast(firstBarrier);
            harness.runtime.retirements.addLast(retryBarrier);

            CompletionStage<Void> first = onEdt(
                    harness.session::preparePeerRemovalAsync);
            firstBarrier.completeExceptionally(
                    new IllegalStateException("simulated WebView2 teardown failure"));
            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> first.toCompletableFuture().get(
                            2, TimeUnit.SECONDS));
            assertTrue(failure.getCause().getMessage().contains(
                    "simulated WebView2 teardown failure"));
            drainEdt();

            CompletionStage<Void> retry = onEdt(
                    harness.session::preparePeerRemovalAsync);
            assertNotSame(first, retry);
            assertEquals(2, harness.runtime.retirementCalls);
            assertFalse(retry.toCompletableFuture().isDone());

            retryBarrier.complete(null);
            retry.toCompletableFuture().get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void terminalFailureFencesLateCallbacksAndClosesRejectedBridgeStreams()
            throws Exception {
        try (Harness harness = Harness.create(availableSdk())) {
            onEdt(harness.session::show);
            onEdt(() -> harness.runtime.fireFailed(
                    "Run exact Flutter Web Canvas",
                    "simulated terminal failure"));

            assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                    onEdt(harness.statuses::getLast).stage());
            int terminalStatusCount = onEdt(() -> {
                return harness.statuses.size();
            });
            CloseTrackingInputStream lateStdout =
                    new CloseTrackingInputStream();
            CloseTrackingOutputStream lateStdin =
                    new CloseTrackingOutputStream();

            onEdt(() -> {
                harness.runtime.firePreparing("late preparation");
                harness.runtime.fireNativeStarted("late-runtime");
                harness.runtime.fireBridgeReady(
                        EXPECTED_ENGINE, lateStdout, lateStdin);
                harness.runtime.fireBridgeTerminal("late terminal");
                harness.runtime.fireFailed("Late failure", "must be ignored");
                harness.runtime.fireSurfaceMetrics(
                        new CanvasSurfaceMetrics(800, 600, 1_000_000));
            });

            assertTrue(lateStdout.closed);
            assertTrue(lateStdin.closed);
            assertEquals(terminalStatusCount, onEdt(() -> {
                return harness.statuses.size();
            }));
            assertNull(field(harness.session, "channel"));
            assertNull(field(harness.session, "hostSurfaceMetrics"));
        }
    }

    @Test
    void webSessionNeverOffersPaletteCatalogDragAndDrop() throws Exception {
        try (Harness harness = Harness.create(availableSdk())) {
            onEdt(() -> {
                assertFalse(harness.session.paletteCatalogInsertDropAvailable());
                assertFalse(harness.session.authorizePaletteDragSource(
                        "nbfdnd:v1:6a7bab32-9507-4f6d-b986-39f183742017:"
                        + "f83e4ad8-e66f-43ae-a5b3-cb057809f17e",
                        new WidgetTypeId("flutter.widgets.Text")));
            });

            assertEquals(0, harness.runtime.startCalls);
        }
    }

    @Test
    void physicalPresentationRequiresExactHostMetrics() throws Exception {
        try (Harness harness = Harness.create(availableSdk())) {
            CanvasSessionId sessionId = CanvasSessionId.random();
            CanvasLayoutKey layout = new CanvasLayoutKey(
                    new CanvasFrameKey(
                            new CanvasRevisionKey(
                                    sessionId,
                                    7,
                                    StableId.random(),
                                    11),
                            0),
                    3);
            CanvasSurfaceMetrics runnerMetrics =
                    new CanvasSurfaceMetrics(1_280, 800, 1_500_000);

            onEdt(() -> {
                setField(harness.session, "currentLayout", layout);
                setField(harness.session,
                        "latestRunnerMetrics", runnerMetrics);

                harness.runtime.fireSurfaceMetrics(
                        new CanvasSurfaceMetrics(1_279, 800, 1_500_000));
                assertNull(field(harness.session, "confirmedLayout"));

                harness.runtime.fireSurfaceMetrics(runnerMetrics);
                assertEquals(layout,
                        field(harness.session, "confirmedLayout"));

                harness.runtime.fireSurfaceMetrics(
                        new CanvasSurfaceMetrics(1_280, 800, 1_250_000));
                assertNull(field(harness.session, "confirmedLayout"));
            });
        }
    }

    private static FlutterDesignerWebCanvasSession construct(
            FakeRuntime runtime,
            FlutterDesignerWebCanvasSession.SdkResolution resolution,
            List<FlutterDesignerNativeCanvasStatus> statuses) {
        return construct(runtime, resolution,
                FlutterDesignerWebCanvasSessionTest::dispatchUi, statuses);
    }

    private static FlutterDesignerWebCanvasSession construct(
            FakeRuntime runtime,
            FlutterDesignerWebCanvasSession.SdkResolution resolution,
            Executor uiExecutor,
            List<FlutterDesignerNativeCanvasStatus> statuses) {
        return new FlutterDesignerWebCanvasSession(
                runtime,
                () -> resolution,
                command -> command.run(),
                uiExecutor,
                statuses::add,
                ignored -> { },
                ignored -> { },
                ignored -> { });
    }

    private static FlutterDesignerWebCanvasSession.SdkResolution availableSdk() {
        return new FlutterDesignerWebCanvasSession.SdkResolution(
                SDK, "test Flutter SDK");
    }

    private static FlutterSdk sdk() {
        Path home = Path.of("target", "web-canvas-session-test", "flutter-sdk")
                .toAbsolutePath().normalize();
        return new FlutterSdk(
                home, home.resolve("bin").resolve("flutter.bat"));
    }

    private static CanvasProcessFramingPolicy handshakePolicy(
            CanvasProcessDirection direction) {
        return CanvasProcessFramingPolicy.handshake(
                CanvasWireLimits.defaults(),
                CanvasWireHandshakeLimits.defaults(),
                direction);
    }

    private static void dispatchUi(Runnable task) {
        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            EventQueue.invokeLater(task);
        }
    }

    private static void awaitEdtCondition(
            BooleanSupplier condition,
            String failureMessage) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (onEdt(condition::getAsBoolean)) {
                return;
            }
            Thread.sleep(5);
        }
        assertTrue(onEdt(condition::getAsBoolean), failureMessage);
    }

    private static void drainEdt() throws Exception {
        onEdt(() -> { });
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

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class Harness implements AutoCloseable {
        private final FakeRuntime runtime;
        private final List<FlutterDesignerNativeCanvasStatus> statuses;
        private final FlutterDesignerWebCanvasSession session;

        private Harness(
                FakeRuntime runtime,
                List<FlutterDesignerNativeCanvasStatus> statuses,
                FlutterDesignerWebCanvasSession session) {
            this.runtime = runtime;
            this.statuses = statuses;
            this.session = session;
        }

        static Harness create(
                FlutterDesignerWebCanvasSession.SdkResolution resolution)
                throws Exception {
            FakeRuntime runtime = new FakeRuntime();
            List<FlutterDesignerNativeCanvasStatus> statuses =
                    new ArrayList<>();
            FlutterDesignerWebCanvasSession session = onEdt(
                    () -> construct(runtime, resolution, statuses));
            return new Harness(runtime, statuses, session);
        }

        @Override
        public void close() throws Exception {
            onEdt(session::close);
        }
    }

    private static final class FakeRuntime
            implements FlutterDesignerWebCanvasSession.Runtime {
        private final JPanel component = new JPanel();
        private final List<Boolean> visibility = new ArrayList<>();
        private final Deque<CompletionStage<Void>> retirements =
                new ArrayDeque<>();
        private Consumer<CanvasSurfaceMetrics> surfaceMetricsListener =
                ignored -> { };
        private FlutterDesignerWebCanvasSession.Runtime.Listener listener;
        private FlutterSdk startedSdk;
        private int startCalls;
        private int focusRequests;
        private int focusReleases;
        private int retirementCalls;
        private boolean running;
        private boolean focused;
        private boolean startOnEdt;

        @Override
        public JComponent component() {
            return component;
        }

        @Override
        public void start(
                FlutterSdk sdk,
                FlutterDesignerWebCanvasSession.Runtime.Listener listener) {
            startCalls++;
            startOnEdt = EventQueue.isDispatchThread();
            startedSdk = sdk;
            this.listener = listener;
            running = true;
        }

        @Override
        public void setVisible(boolean visible) {
            visibility.add(visible);
        }

        @Override
        public void requestControllerFocus() {
            focusRequests++;
            focused = true;
        }

        @Override
        public boolean isControllerFocused() {
            return focused;
        }

        @Override
        public boolean releaseControllerFocus() {
            focusReleases++;
            boolean released = focused;
            focused = false;
            return released;
        }

        @Override
        public boolean running() {
            return running;
        }

        @Override
        public void setSurfaceMetricsListener(
                Consumer<CanvasSurfaceMetrics> listener) {
            surfaceMetricsListener = listener;
        }

        @Override
        public CompletionStage<Void> preparePeerRemovalAsync() {
            retirementCalls++;
            running = false;
            return retirements.isEmpty()
                    ? CompletableFuture.completedFuture(null)
                    : retirements.removeFirst();
        }

        void fireBridgeReady(
                CanvasEngineIdentity expectedIdentity,
                InputStream stdout,
                OutputStream stdin) {
            if (listener == null) {
                throw new AssertionError("runtime was not started");
            }
            listener.bridgeReady(expectedIdentity, stdout, stdin);
        }

        void firePreparing(String detail) {
            requireListener().preparing(detail);
        }

        void fireNativeStarted(String runtimeVersion) {
            requireListener().nativeStarted(runtimeVersion);
        }

        void fireBridgeTerminal(String reason) {
            requireListener().bridgeTerminal(reason);
        }

        void fireFailed(String operation, String reason) {
            requireListener().failed(operation, reason);
        }

        void fireSurfaceMetrics(CanvasSurfaceMetrics metrics) {
            surfaceMetricsListener.accept(metrics);
        }

        private FlutterDesignerWebCanvasSession.Runtime.Listener requireListener() {
            if (listener == null) {
                throw new AssertionError("runtime was not started");
            }
            return listener;
        }
    }

    private static final class CloseTrackingInputStream extends InputStream {
        private boolean closed;

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class CloseTrackingOutputStream extends OutputStream {
        private boolean closed;

        @Override
        public void write(int value) {
            // No transport is needed; only ownership closure is observed.
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class ProtocolBridge implements AutoCloseable {
        private final PipedInputStream sessionStdout;
        private final PipedOutputStream runnerStdout;
        private final PipedInputStream runnerStdin;
        private final PipedOutputStream sessionStdin;
        private final CanvasProcessFrameReader hostFrames;
        private final CanvasProcessFrameWriter runnerFrames;

        private ProtocolBridge() throws IOException {
            sessionStdout = new PipedInputStream(1024 * 1024);
            runnerStdout = new PipedOutputStream(sessionStdout);
            runnerStdin = new PipedInputStream(1024 * 1024);
            sessionStdin = new PipedOutputStream(runnerStdin);
            CanvasProcessFrameCodec codec = new CanvasProcessFrameCodec();
            hostFrames = codec.reader(runnerStdin);
            runnerFrames = codec.writer(runnerStdout);
        }

        InputStream sessionStdout() {
            return sessionStdout;
        }

        OutputStream sessionStdin() {
            return sessionStdin;
        }

        void handshake(CanvasEngineIdentity actualIdentity) throws Exception {
            CanvasProcessFrame helloFrame = hostFrames.read(handshakePolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER)).orElseThrow();
            assertEquals(CanvasProcessFrameKind.CONTROL_JSON,
                    helloFrame.kind());
            CanvasWireDecodeResult.Decoded decoded = assertInstanceOf(
                    CanvasWireDecodeResult.Decoded.class,
                    new CanvasWireCodec().decode(helloFrame.copyPayload()));
            CanvasHostHello hello = assertInstanceOf(
                    CanvasHostHello.class, decoded.message());
            CanvasRunnerHello response = new CanvasRunnerHello(
                    hello.sessionId(),
                    0,
                    hello.sequence(),
                    "flutter-canvas-runner/1",
                    actualIdentity,
                    hello.requestedCapabilities(),
                    hello.offeredLimits());
            runnerFrames.write(
                    handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON,
                            new CanvasWireCodec().encode(response)));
        }

        @Override
        public void close() {
            closeQuietly(sessionStdin);
            closeQuietly(runnerStdin);
            closeQuietly(runnerStdout);
            closeQuietly(sessionStdout);
        }

        private static void closeQuietly(AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // The session may already have aborted both protocol streams.
            }
        }
    }
}
