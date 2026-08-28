package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasHostClose;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerClosed;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerFailure;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCapability;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCloseReason;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCodec;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireDecodeResult;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireEncodeException;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireHandshakeLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireMessage;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireNegotiation;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireSessionAdmission;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireSessionGate;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessDirection;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrame;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameCodec;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameKind;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameReader;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFrameWriter;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFramingError;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFramingException;
import dev.flutter.netbeans.designer.canvas.transport.CanvasProcessFramingPolicy;
import dev.flutter.netbeans.designer.model.StableId;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Owns the bounded binary protocol on one isolated Flutter Canvas process'
 * stdin and stdout.
 *
 * <p>Standard output is protocol-only after the directly launched debug
 * engine's one bounded Dart VM service announcement. Runner diagnostics belong
 * on stderr and are deliberately not consumed by this channel. Listener
 * callbacks are delivered through the caller-supplied executor so the
 * NetBeans integration can marshal them onto the event-dispatch thread.</p>
 */
public final class CanvasRunnerProcessChannel implements AutoCloseable {
    private static final String HOST_VERSION = "netbeans-flutter-plugin";
    private static final int MAX_PENDING_OUTBOUND_OPERATIONS = 64;
    private static final Set<CanvasWireCapability> REQUESTED_CAPABILITIES =
            Set.copyOf(EnumSet.allOf(CanvasWireCapability.class));
    private static final Set<CanvasWireCapability> REQUIRED_CAPABILITIES = Set.of(
            CanvasWireCapability.READ_ONLY_RENDER,
            CanvasWireCapability.READ_ONLY_LAYOUT,
            CanvasWireCapability.READ_ONLY_SELECTION);

    private final CanvasSessionId sessionId;
    private final InputStream stdout;
    private final OutputStream stdin;
    private final Executor listenerExecutor;
    private final Listener listener;
    private final CanvasWireLimits wireLimits = CanvasWireLimits.defaults();
    private final CanvasWireHandshakeLimits offeredLimits =
            CanvasWireHandshakeLimits.defaults();
    private final CanvasWireCodec wireCodec = new CanvasWireCodec(wireLimits);
    private final CanvasRunnerControlCodec runtimeCodec =
            new CanvasRunnerControlCodec();
    private final CanvasProcessFrameReader reader;
    private final CanvasProcessFrameWriter writer;
    private final CanvasWireSessionGate sessionGate;
    private final Object stateLock = new Object();
    private final ThreadPoolExecutor outboundExecutor;

    private State state = State.NEW;
    private CanvasProcessFramingPolicy readPolicy;
    private CanvasProcessFramingPolicy writePolicy;
    private Set<CanvasWireCapability> acceptedCapabilities = Set.of();
    private CanvasRevisionKey latestExpectedRevision;
    private Thread readerThread;
    private boolean failureDelivered;
    private boolean transportCloseQueued;

    /**
     * Creates a channel over one already-started process. The process itself is
     * never terminated by this object.
     */
    public CanvasRunnerProcessChannel(
            Process process,
            CanvasSessionId sessionId,
            Executor listenerExecutor,
            Listener listener) {
        this(
                Objects.requireNonNull(process, "process").getInputStream(),
                process.getOutputStream(),
                sessionId,
                listenerExecutor,
                listener);
    }

    CanvasRunnerProcessChannel(
            InputStream stdout,
            OutputStream stdin,
            CanvasSessionId sessionId,
            Executor listenerExecutor,
            Listener listener) {
        this.stdout = Objects.requireNonNull(stdout, "stdout");
        this.stdin = Objects.requireNonNull(stdin, "stdin");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.listenerExecutor = Objects.requireNonNull(
                listenerExecutor, "listenerExecutor");
        this.listener = Objects.requireNonNull(listener, "listener");
        CanvasProcessFrameCodec frameCodec = new CanvasProcessFrameCodec();
        reader = frameCodec.reader(new CanvasRunnerProtocolInputStream(stdout));
        writer = frameCodec.writer(stdin);
        sessionGate = new CanvasWireSessionGate(
                sessionId,
                HOST_VERSION,
                REQUESTED_CAPABILITIES,
                offeredLimits);
        outboundExecutor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_PENDING_OUTBOUND_OPERATIONS),
                task -> {
                    Thread thread = new Thread(
                            task,
                            "flutter-canvas-outbound-" + sessionId);
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
        readPolicy = CanvasProcessFramingPolicy.handshake(
                wireLimits,
                offeredLimits,
                CanvasProcessDirection.RUNNER_TO_HOST);
        writePolicy = CanvasProcessFramingPolicy.handshake(
                wireLimits,
                offeredLimits,
                CanvasProcessDirection.HOST_TO_RUNNER);
    }

    /** Starts the one-shot handshake and reader loop. */
    public void start() {
        final CanvasProcessFrame helloFrame;
        synchronized (stateLock) {
            if (state != State.NEW) {
                throw new IllegalStateException(
                        "Canvas runner process channel has already been started.");
            }
            state = State.HANDSHAKING;
            try {
                CanvasWireMessage hostHello = sessionGate.start();
                helloFrame = new CanvasProcessFrame(
                        CanvasProcessFrameKind.CONTROL_JSON,
                        wireCodec.encode(hostHello));
            } catch (CanvasWireEncodeException | RuntimeException failure) {
                state = State.FAILED;
                failureDelivered = true;
                dispatch(() -> listener.failed(
                        "Canvas host handshake could not be encoded."));
                queueTransportClose();
                return;
            }
        }
        enqueueOutbound(() -> writeHostHello(helloFrame));
    }

    /** Returns whether the exact completed handshake admitted one capability. */
    public boolean supports(CanvasWireCapability capability) {
        Objects.requireNonNull(capability, "capability");
        synchronized (stateLock) {
            return state == State.READY && acceptedCapabilities.contains(capability);
        }
    }

    /**
     * Advances the latest-publication fence before asynchronous model encoding
     * completes. Queued older publications are discarded by the single writer
     * immediately before any bytes are written.
     */
    public void expectPresentation(CanvasRevisionKey revisionKey) {
        Objects.requireNonNull(revisionKey, "revisionKey");
        requireExactSession(revisionKey.sessionId(), "render request");
        synchronized (stateLock) {
            if (state != State.READY) {
                throw new IllegalStateException(
                        "Canvas runner process channel is not ready.");
            }
            latestExpectedRevision = revisionKey;
        }
    }

    /**
     * Enqueues one runtime control declaration and its exact model payload.
     * Pipe I/O and descriptor hashing happen only on the serial outbound
     * worker. Returns {@code false} when the session is not ready or the
     * request is already behind the latest-publication fence.
     */
    public boolean present(CanvasRenderRequest request, byte[] encodedModel) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(encodedModel, "encodedModel");
        requireExactSession(request.revisionKey().sessionId(), "render request");
        synchronized (stateLock) {
            if (state != State.READY
                    || !request.revisionKey().equals(latestExpectedRevision)) {
                return false;
            }
        }
        return enqueueOutbound(() -> writePresentation(request, encodedModel));
    }

    /** Sends a read-only selection projection for the exact current layout. */
    public boolean select(CanvasLayoutKey layoutKey, StableId widgetId) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        Objects.requireNonNull(widgetId, "widgetId");
        requireExactSession(layoutKey.sessionId(), "selection");
        synchronized (stateLock) {
            if (state != State.READY
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return false;
            }
        }
        return enqueueOutbound(() -> writeSelection(layoutKey, widgetId));
    }

    public CanvasSessionId sessionId() {
        return sessionId;
    }

    public boolean isReady() {
        synchronized (stateLock) {
            return state == State.READY;
        }
    }

    @Override
    public void close() {
        CanvasHostClose close = null;
        CanvasProcessFramingPolicy closingPolicy = null;
        synchronized (stateLock) {
            if (state == State.CLOSED || state == State.CLOSING
                    || state == State.FAILED) {
                return;
            }
            if (state == State.HANDSHAKING || state == State.READY) {
                try {
                    close = sessionGate.beginClose(
                            CanvasWireCloseReason.FORM_CLOSED);
                    closingPolicy = writePolicy.closing();
                } catch (IllegalStateException ignored) {
                    // A concurrent terminal protocol event already owns shutdown.
                }
            }
            state = State.CLOSING;
            latestExpectedRevision = null;
        }
        final CanvasHostClose outboundClose = close;
        final CanvasProcessFramingPolicy outboundPolicy = closingPolicy;
        outboundExecutor.getQueue().clear();
        if (!enqueueOutbound(() -> writeCloseAndCloseTransport(
                outboundClose, outboundPolicy))) {
            queueTransportClose();
        }
    }

    private void writeHostHello(CanvasProcessFrame helloFrame) {
        synchronized (stateLock) {
            if (state == State.FAILED || state == State.CLOSED) {
                return;
            }
        }
        try {
            writer.write(writePolicy, helloFrame);
        } catch (IOException | RuntimeException failure) {
            fail("Canvas host handshake could not be written to runner stdin.");
            return;
        }
        synchronized (stateLock) {
            if (state != State.HANDSHAKING) {
                return;
            }
            readerThread = new Thread(
                    this::readLoop,
                    "flutter-canvas-protocol-" + sessionId);
            readerThread.setDaemon(true);
            readerThread.start();
        }
    }

    private void writePresentation(
            CanvasRenderRequest request,
            byte[] encodedModel) {
        final CanvasProcessFramingPolicy activePolicy;
        synchronized (stateLock) {
            if (state != State.READY
                    || !request.revisionKey().equals(latestExpectedRevision)) {
                return;
            }
            activePolicy = writePolicy;
        }
        try {
            CanvasProcessFrame model = new CanvasProcessFrame(
                    CanvasProcessFrameKind.MODEL_JSON,
                    encodedModel);
            byte[] control = runtimeCodec.encodeRender(
                    request, model.descriptor());
            synchronized (stateLock) {
                if (state != State.READY
                        || !request.revisionKey().equals(
                                latestExpectedRevision)) {
                    return;
                }
            }
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
            writer.write(activePolicy, model, model.descriptor());
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas render request could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas render request could not be written to runner stdin.");
        }
    }

    private void writeSelection(CanvasLayoutKey layoutKey, StableId widgetId) {
        final CanvasProcessFramingPolicy activePolicy;
        synchronized (stateLock) {
            if (state != State.READY
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return;
            }
            activePolicy = writePolicy;
        }
        try {
            byte[] control = runtimeCodec.encodeSelection(layoutKey, widgetId);
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas selection request could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas selection request could not be written to runner stdin.");
        }
    }

    private void writeCloseAndCloseTransport(
            CanvasHostClose close,
            CanvasProcessFramingPolicy closingPolicy) {
        if (close != null && closingPolicy != null) {
            try {
                writer.write(
                        closingPolicy,
                        new CanvasProcessFrame(
                                CanvasProcessFrameKind.CONTROL_JSON,
                                wireCodec.encode(close)));
            } catch (Exception ignored) {
                // Explicit close remains best-effort and is never a user failure.
            }
        }
        closeTransportOnOutboundWorker();
    }

    private void readLoop() {
        while (true) {
            CanvasProcessFramingPolicy activePolicy;
            synchronized (stateLock) {
                if (state == State.CLOSED || state == State.FAILED) {
                    return;
                }
                activePolicy = readPolicy;
            }
            Optional<CanvasProcessFrame> next;
            try {
                next = reader.read(activePolicy);
            } catch (CanvasProcessFramingException failure) {
                if (terminalOrClosing()) {
                    return;
                }
                fail(framingFailureReason(failure));
                return;
            } catch (IOException failure) {
                if (terminalOrClosing()) {
                    return;
                }
                fail("Canvas runner stdout could not be read ("
                        + failure.getClass().getSimpleName() + ").");
                return;
            } catch (RuntimeException failure) {
                if (terminalOrClosing()) {
                    return;
                }
                fail("Canvas runner stdout decoder failed internally ("
                        + failure.getClass().getSimpleName() + ").");
                return;
            }
            if (next.isEmpty()) {
                if (terminalOrClosing()) {
                    return;
                }
                fail("Canvas runner stdout ended before the Canvas session closed.");
                return;
            }
            if (!handleControl(next.orElseThrow().copyPayload())) {
                return;
            }
        }
    }

    private boolean handleControl(byte[] payload) {
        CanvasWireDecodeResult lifecycle = wireCodec.decode(payload);
        if (lifecycle instanceof CanvasWireDecodeResult.Decoded decoded) {
            return handleLifecycle(decoded.message());
        }
        CanvasRunnerRuntimeEvent runtime;
        try {
            runtime = runtimeCodec.decode(payload);
        } catch (CanvasRunnerControlException failure) {
            String diagnostic = ((CanvasWireDecodeResult.Invalid) lifecycle)
                    .diagnostic().message();
            fail("Canvas runner stdout control message is invalid: " + diagnostic);
            return false;
        }
        return handleRuntime(runtime);
    }

    private boolean handleLifecycle(CanvasWireMessage message) {
        CanvasWireSessionAdmission admission = sessionGate.admit(message);
        if (!admission.accepted()) {
            fail("Canvas runner lifecycle message was rejected: "
                    + admission.name().toLowerCase(java.util.Locale.ROOT) + '.');
            return false;
        }
        if (message instanceof CanvasRunnerHello hello) {
            return acceptHello(hello);
        }
        if (message instanceof CanvasRunnerFailure failure) {
            fail("Canvas runner reported "
                    + failure.code().name().toLowerCase(java.util.Locale.ROOT)
                    + ": " + failure.message());
            return false;
        }
        if (message instanceof CanvasRunnerClosed) {
            synchronized (stateLock) {
                state = State.CLOSED;
                acceptedCapabilities = Set.of();
                latestExpectedRevision = null;
            }
            queueTransportClose();
            return false;
        }
        fail("Canvas runner sent a host-only lifecycle message.");
        return false;
    }

    private boolean acceptHello(CanvasRunnerHello hello) {
        CanvasWireNegotiation negotiation = sessionGate.negotiation()
                .orElseThrow();
        if (!Set.copyOf(negotiation.acceptedCapabilities())
                .containsAll(REQUIRED_CAPABILITIES)) {
            fail("Canvas runner did not negotiate the required read-only render, "
                    + "layout and selection capabilities.");
            return false;
        }
        synchronized (stateLock) {
            if (state != State.HANDSHAKING) {
                return false;
            }
            readPolicy = CanvasProcessFramingPolicy.negotiated(
                    wireLimits,
                    negotiation,
                    CanvasProcessDirection.RUNNER_TO_HOST);
            writePolicy = CanvasProcessFramingPolicy.negotiated(
                    wireLimits,
                    negotiation,
                    CanvasProcessDirection.HOST_TO_RUNNER);
            acceptedCapabilities = Set.copyOf(negotiation.acceptedCapabilities());
            state = State.READY;
        }
        dispatch(() -> listener.ready(hello.engineIdentity()));
        return true;
    }

    private boolean handleRuntime(CanvasRunnerRuntimeEvent event) {
        boolean ready;
        synchronized (stateLock) {
            ready = state == State.READY;
        }
        if (!ready) {
            fail("Canvas runner sent a runtime event before handshake completion.");
            return false;
        }
        if (event instanceof CanvasRunnerRuntimeEvent.Presented presented) {
            if (!exactSession(presented.layoutKey().sessionId())) {
                fail("Canvas runner presented a stale or foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.presented(presented.layoutKey()));
            return true;
        }
        if (event instanceof CanvasRunnerRuntimeEvent.Selection selection) {
            if (!exactSession(selection.intentKey().intentId().sessionId())
                    || !exactSession(selection.intentKey().layoutKey().sessionId())) {
                fail("Canvas runner selected a widget for a stale or foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.selection(
                    selection.intentKey(), selection.widgetId()));
            return true;
        }
        CanvasRunnerRuntimeEvent.PaletteDrop drop =
                (CanvasRunnerRuntimeEvent.PaletteDrop) event;
        final boolean paletteDropNegotiated;
        synchronized (stateLock) {
            paletteDropNegotiated = acceptedCapabilities.contains(
                    CanvasWireCapability.PALETTE_DROP_TEXT_APPEND_V1);
        }
        if (!paletteDropNegotiated) {
            fail("Canvas runner sent a Palette drop without negotiating its capability.");
            return false;
        }
        if (!exactSession(drop.intentKey().intentId().sessionId())
                || !exactSession(drop.intentKey().layoutKey().sessionId())) {
            fail("Canvas runner sent a Palette drop for a stale or foreign Canvas session.");
            return false;
        }
        dispatch(() -> listener.paletteDrop(drop));
        return true;
    }

    private boolean terminalOrClosing() {
        synchronized (stateLock) {
            return state == State.CLOSING
                    || state == State.CLOSED
                    || state == State.FAILED;
        }
    }

    private static String framingFailureReason(
            CanvasProcessFramingException failure) {
        String reason = "Canvas runner stdout protocol framing failed ("
                + failure.error().name().toLowerCase(java.util.Locale.ROOT)
                + "): " + failure.getMessage();
        if (failure.error() == CanvasProcessFramingError.INVALID_MAGIC) {
            return reason + " The runner wrote unexpected non-protocol bytes; "
                    + "only one bounded loopback Dart VM service announcement "
                    + "is permitted before the first NBFC frame.";
        }
        return reason;
    }

    private void requireExactSession(CanvasSessionId candidate, String operation) {
        if (!exactSession(candidate)) {
            throw new IllegalArgumentException(
                    "Canvas " + operation + " belongs to another session.");
        }
    }

    private boolean exactSession(CanvasSessionId candidate) {
        return sessionId.equals(candidate);
    }

    private void fail(String reason) {
        boolean notify;
        synchronized (stateLock) {
            if (state == State.CLOSED || state == State.CLOSING
                    || state == State.FAILED) {
                return;
            }
            state = State.FAILED;
            latestExpectedRevision = null;
            notify = !failureDelivered;
            failureDelivered = true;
        }
        if (notify) {
            dispatch(() -> listener.failed(reason));
        }
        queueTransportClose();
    }

    private boolean enqueueOutbound(Runnable task) {
        try {
            outboundExecutor.execute(task);
            return true;
        } catch (RejectedExecutionException ignored) {
            return false;
        }
    }

    private void queueTransportClose() {
        synchronized (stateLock) {
            if (transportCloseQueued) {
                return;
            }
            transportCloseQueued = true;
        }
        outboundExecutor.getQueue().clear();
        if (!enqueueOutbound(this::closeTransportOnOutboundWorker)) {
            // The only code that shuts this executor down is the already-queued
            // transport close, so rejection means that worker owns teardown.
        }
    }

    private void closeTransportOnOutboundWorker() {
        closeQuietly(stdin);
        closeQuietly(stdout);
        Thread activeReader;
        synchronized (stateLock) {
            state = State.CLOSED;
            latestExpectedRevision = null;
            transportCloseQueued = true;
            activeReader = readerThread;
        }
        if (activeReader != null && activeReader != Thread.currentThread()) {
            activeReader.interrupt();
        }
        outboundExecutor.shutdown();
    }

    private void dispatch(Runnable callback) {
        try {
            listenerExecutor.execute(() -> {
                try {
                    callback.run();
                } catch (RuntimeException ignored) {
                    // A UI callback cannot poison or escape the protocol thread.
                }
            });
        } catch (RuntimeException ignored) {
            // Executor shutdown must not escape a process-protocol thread.
        }
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Best-effort transport teardown.
        }
    }

    private static void closeQuietly(OutputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Best-effort transport teardown.
        }
    }

    /** Callbacks emitted only after bounded decoding and exact-session checks. */
    public interface Listener {
        void ready(CanvasEngineIdentity engineIdentity);

        void presented(CanvasLayoutKey layoutKey);

        void selection(CanvasIntentKey intentKey, StableId widgetId);

        /** Optional until the owning Designer session wires the mutation slice. */
        default void paletteDrop(CanvasRunnerRuntimeEvent.PaletteDrop drop) {
        }

        void failed(String reason);
    }

    private enum State {
        NEW,
        HANDSHAKING,
        READY,
        CLOSING,
        CLOSED,
        FAILED
    }
}
