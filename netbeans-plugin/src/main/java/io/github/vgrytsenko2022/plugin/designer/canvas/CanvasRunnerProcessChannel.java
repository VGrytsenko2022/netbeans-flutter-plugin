package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResource;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasRenderRequest;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportMetrics;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportPresentation;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasHostClose;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasRunnerClosed;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasRunnerFailure;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasRunnerHello;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireCapability;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireCloseReason;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireCodec;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireDecodeResult;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireEncodeException;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireHandshakeLimits;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireLimits;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireMessage;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireNegotiation;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireProtocol;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireSessionAdmission;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireSessionGate;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessDirection;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFrame;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFrameCodec;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFrameKind;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFrameReader;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFrameWriter;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFramingError;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFramingException;
import io.github.vgrytsenko2022.designer.canvas.transport.CanvasProcessFramingPolicy;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            CanvasWireCapability.READ_ONLY_SELECTION,
            CanvasWireCapability.SURFACE_PRESENTATION_V1);

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
    private final LinkedBlockingDeque<Runnable> outboundQueue;
    private final ThreadPoolExecutor outboundExecutor;
    private final CompletableFuture<Boolean> closeWriteCompletion =
            new CompletableFuture<>();
    private final CompletableFuture<Boolean> authenticatedCloseCompletion =
            new CompletableFuture<>();

    private State state = State.NEW;
    private CanvasProcessFramingPolicy readPolicy;
    private CanvasProcessFramingPolicy writePolicy;
    private Set<CanvasWireCapability> acceptedCapabilities = Set.of();
    private CanvasRevisionKey latestExpectedRevision;
    private Thread readerThread;
    private boolean failureDelivered;
    private boolean transportCloseQueued;
    private boolean authenticatedCloseReceived;

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

    /**
     * Creates the same bounded protocol channel over an authenticated,
     * transport-owned byte stream pair.
     *
     * <p>This is the narrow entry point used by the exact Flutter Web Canvas:
     * WebView2 owns browser lifetime and the authenticated host bridge owns
     * the streams, while this class continues to own only NBFC framing and the
     * Canvas wire session. Closing the returned channel closes the supplied
     * streams but never attempts to terminate their external transport.</p>
     */
    public static CanvasRunnerProcessChannel overStreams(
            InputStream stdout,
            OutputStream stdin,
            CanvasSessionId sessionId,
            Executor listenerExecutor,
            Listener listener) {
        return new CanvasRunnerProcessChannel(
                stdout, stdin, sessionId, listenerExecutor, listener);
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
        outboundQueue = new LinkedBlockingDeque<>(
                MAX_PENDING_OUTBOUND_OPERATIONS);
        outboundExecutor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                outboundQueue,
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
        synchronized (stateLock) {
            if (state != State.NEW) {
                throw new IllegalStateException(
                        "Canvas runner process channel has already been started.");
            }
            state = State.HANDSHAKING;
            try {
                CanvasWireMessage hostHello = sessionGate.start();
                CanvasProcessFrame helloFrame = new CanvasProcessFrame(
                        CanvasProcessFrameKind.CONTROL_JSON,
                        wireCodec.encode(hostHello));
                // Queue the mandatory first frame before HANDSHAKING becomes
                // observable outside this lock. A concurrent close can now
                // only enqueue host.close behind host.hello.
                if (!enqueueOutbound(() -> writeHostHello(helloFrame))) {
                    state = State.FAILED;
                    failureDelivered = true;
                    dispatch(() -> listener.failed(
                            "Canvas host handshake could not be queued."));
                    queueTransportClose();
                }
            } catch (CanvasWireEncodeException | RuntimeException failure) {
                state = State.FAILED;
                failureDelivered = true;
                dispatch(() -> listener.failed(
                        "Canvas host handshake could not be encoded."));
                queueTransportClose();
                return;
            }
        }
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
                    || !request.revisionKey().equals(latestExpectedRevision)
                    || (!request.imageResources().resources().isEmpty()
                    && !acceptedCapabilities.contains(
                            CanvasWireCapability.ASSET_IMAGE_BYTES_V1))) {
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

    /** Sends the host-owned focus/interaction epoch for the exact layout. */
    public boolean interactionFence(
            CanvasLayoutKey layoutKey,
            long interactionFenceSequence) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        requireExactSession(layoutKey.sessionId(), "interaction fence");
        requireInteractionFenceSequence(interactionFenceSequence);
        synchronized (stateLock) {
            if (state != State.READY
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return false;
            }
            // Keep admission and priority insertion atomic against close.
            // A close that wins first makes this return false; a close that
            // follows clears this queued fence before appending host.close.
            return enqueuePriorityInteractionFence(new InteractionFenceWrite(
                    layoutKey, interactionFenceSequence));
        }
    }

    /**
     * Projects one host-authoritative Palette source for trait-aware native
     * hover. The opaque token remains the only value carried by native OLE;
     * the Java host still consumes it and replans the final mutation.
     */
    public boolean authorizePaletteDragSource(
            CanvasLayoutKey layoutKey,
            String token,
            WidgetTypeId widgetType,
            Set<String> traits) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(widgetType, "widgetType");
        Objects.requireNonNull(traits, "traits");
        requireExactSession(layoutKey.sessionId(), "Palette drag source");
        Set<String> immutableTraits = Set.copyOf(traits);
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1)
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1)
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return false;
            }
        }
        return enqueueOutbound(() -> writePaletteDragSource(
                layoutKey, token, widgetType, immutableTraits));
    }

    /**
     * Projects one already-planned widget move target into the exact current
     * Canvas layout. This optional command never grants mutation authority.
     */
    public boolean previewWidgetMove(
            CanvasLayoutKey layoutKey,
            long previewSequence,
            StableId sourceWidgetId,
            StableId parentWidgetId,
            SlotName slotName,
            int insertionIndex) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        Objects.requireNonNull(sourceWidgetId, "sourceWidgetId");
        Objects.requireNonNull(parentWidgetId, "parentWidgetId");
        Objects.requireNonNull(slotName, "slotName");
        requireExactSession(layoutKey.sessionId(), "widget move preview");
        requirePreviewSequence(previewSequence);
        requireInsertionIndex(insertionIndex);
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return false;
            }
        }
        return enqueueOutbound(() -> writeWidgetMovePreview(
                layoutKey,
                previewSequence,
                sourceWidgetId,
                parentWidgetId,
                slotName,
                insertionIndex));
    }

    /** Clears the host-projected widget move target for the exact layout. */
    public boolean clearWidgetMovePreview(
            CanvasLayoutKey layoutKey,
            long previewSequence) {
        Objects.requireNonNull(layoutKey, "layoutKey");
        requireExactSession(layoutKey.sessionId(), "widget move preview clear");
        requirePreviewSequence(previewSequence);
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return false;
            }
        }
        return enqueueOutbound(() -> writeWidgetMovePreviewClear(
                layoutKey, previewSequence));
    }

    /**
     * Sends one viewport presentation request for the exact current revision.
     * The request is optional and is admitted only when its capability was
     * negotiated by the isolated runner.
     */
    public boolean viewport(
            CanvasRevisionKey revisionKey,
            long commandSequence,
            CanvasViewportPresentation presentation) {
        Objects.requireNonNull(revisionKey, "revisionKey");
        Objects.requireNonNull(presentation, "presentation");
        requireExactSession(revisionKey.sessionId(), "viewport request");
        requireCommandSequence(commandSequence);
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.VIEWPORT_PRESENTATION_V1)
                    || !revisionKey.equals(latestExpectedRevision)) {
                return false;
            }
        }
        return enqueueOutbound(() -> writeViewport(
                revisionKey, commandSequence, presentation));
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
        requestClose(CanvasWireCloseReason.FORM_CLOSED);
    }

    /**
     * Requests shutdown of this exact runner session for the first lifecycle
     * reason observed by the channel.
     *
     * <p>Repeated requests are idempotent and cannot rewrite the reason of an
     * already queued {@code host.close} frame. This matters when AWT peer loss
     * precedes the later NetBeans form-close callback.</p>
     */
    public void requestClose(CanvasWireCloseReason reason) {
        Objects.requireNonNull(reason, "reason");
        CanvasHostClose close = null;
        CanvasProcessFramingPolicy closingPolicy = null;
        boolean closeDuringHandshake = false;
        synchronized (stateLock) {
            if (state == State.CLOSED || state == State.CLOSING
                    || state == State.FAILED) {
                if (state == State.FAILED
                        || (state == State.CLOSED
                                && !authenticatedCloseReceived)) {
                    authenticatedCloseCompletion.complete(false);
                }
                return;
            }
            if (state == State.HANDSHAKING || state == State.READY) {
                closeDuringHandshake = state == State.HANDSHAKING;
                try {
                    close = sessionGate.beginClose(reason);
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
        // During startup, host.hello may still be queued or in flight. It is
        // the mandatory first frame and must remain ahead of host.close. READY
        // channels have no pending handshake, so stale render/intent writes can
        // be discarded before their close request.
        if (!closeDuringHandshake) {
            outboundExecutor.getQueue().clear();
        }
        if (!enqueueOutbound(() -> writeClose(outboundClose, outboundPolicy))) {
            closeWriteCompletion.complete(false);
            authenticatedCloseCompletion.complete(false);
        }
    }

    /**
     * Waits only for the already-requested {@code host.close} frame to leave
     * the host-side writer. It does not wait for {@code runner.closed} or for
     * process exit.
     *
     * <p>This is a diagnostic write fence, not proof that the runner processed
     * the request. Native-peer teardown is guarded by
     * {@link #awaitAuthenticatedClose(Duration)}. The wait remains bounded so
     * an unresponsive pipe cannot wedge the AWT event-dispatch thread.</p>
     */
    public boolean awaitCloseRequestWritten(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Close-write timeout cannot be negative");
        }
        try {
            return closeWriteCompletion.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException | ExecutionException ignored) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Waits for a {@code runner.closed} message that passed the session gate.
     * Completion happens on the protocol reader thread before the listener is
     * marshalled to the EDT, allowing a bounded close callback to preserve the
     * native parent peer until authentication succeeds.
     */
    public boolean awaitAuthenticatedClose(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Close-ack timeout cannot be negative");
        }
        try {
            return authenticatedCloseCompletion.get(
                    timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException | ExecutionException ignored) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Abandons the protocol transport without asking the runner to shut down.
     *
     * <p>The owning session uses this only after the process has exited, after
     * a terminal protocol failure, or when its bounded graceful-close deadline
     * has expired. Normal form close must use {@link #close()} so stdout remains
     * available for the authenticated {@code runner.closed} acknowledgement.</p>
     */
    public void abort() {
        synchronized (stateLock) {
            if (state == State.CLOSED && transportCloseQueued) {
                return;
            }
            state = State.CLOSED;
            acceptedCapabilities = Set.of();
            latestExpectedRevision = null;
        }
        authenticatedCloseCompletion.complete(false);
        queueTransportClose();
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
            if (state != State.HANDSHAKING && state != State.CLOSING) {
                return;
            }
            if (readerThread == null) {
                readerThread = new Thread(
                        this::readLoop,
                        "flutter-canvas-protocol-" + sessionId);
                readerThread.setDaemon(true);
                readerThread.start();
            }
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
            if (request.imageResources().totalEncodedBytes()
                    > activePolicy.maxPayloadBytes(
                            CanvasProcessFrameKind.IMAGE_BYTES)) {
                throw new IllegalArgumentException(
                        "Canvas image publication exceeds the negotiated aggregate bound");
            }
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
            for (CanvasImageResource image
                    : request.imageResources().resources()) {
                CanvasProcessFrame frame = new CanvasProcessFrame(
                        CanvasProcessFrameKind.IMAGE_BYTES,
                        image.copyEncodedBytes());
                writer.write(activePolicy, frame, frame.descriptor());
            }
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

    private void writeInteractionFence(
            CanvasLayoutKey layoutKey,
            long interactionFenceSequence) {
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
            byte[] control = runtimeCodec.encodeInteractionFence(
                    layoutKey, interactionFenceSequence);
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas interaction fence could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas interaction fence could not be written to runner stdin.");
        }
    }

    private void writePaletteDragSource(
            CanvasLayoutKey layoutKey,
            String token,
            WidgetTypeId widgetType,
            Set<String> traits) {
        final CanvasProcessFramingPolicy activePolicy;
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1)
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1)
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return;
            }
            activePolicy = writePolicy;
        }
        try {
            byte[] control = runtimeCodec.encodePaletteDragSource(
                    layoutKey, token, widgetType, traits);
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas Palette drag source could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas Palette drag source could not be written to runner stdin.");
        }
    }

    private void writeWidgetMovePreview(
            CanvasLayoutKey layoutKey,
            long previewSequence,
            StableId sourceWidgetId,
            StableId parentWidgetId,
            SlotName slotName,
            int insertionIndex) {
        final CanvasProcessFramingPolicy activePolicy;
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return;
            }
            activePolicy = writePolicy;
        }
        try {
            byte[] control = runtimeCodec.encodeWidgetMovePreview(
                    layoutKey,
                    previewSequence,
                    sourceWidgetId,
                    parentWidgetId,
                    slotName,
                    insertionIndex);
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas widget move preview could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas widget move preview could not be written to runner stdin.");
        }
    }

    private void writeWidgetMovePreviewClear(
            CanvasLayoutKey layoutKey,
            long previewSequence) {
        final CanvasProcessFramingPolicy activePolicy;
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)
                    || latestExpectedRevision == null
                    || !layoutKey.frameKey().revisionKey().equals(
                            latestExpectedRevision)) {
                return;
            }
            activePolicy = writePolicy;
        }
        try {
            byte[] control = runtimeCodec.encodeWidgetMovePreviewClear(
                    layoutKey, previewSequence);
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas widget move preview clear could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas widget move preview clear could not be written to runner stdin.");
        }
    }

    private void writeViewport(
            CanvasRevisionKey revisionKey,
            long commandSequence,
            CanvasViewportPresentation presentation) {
        final CanvasProcessFramingPolicy activePolicy;
        synchronized (stateLock) {
            if (state != State.READY
                    || !acceptedCapabilities.contains(
                            CanvasWireCapability.VIEWPORT_PRESENTATION_V1)
                    || !revisionKey.equals(latestExpectedRevision)) {
                return;
            }
            activePolicy = writePolicy;
        }
        try {
            byte[] control = runtimeCodec.encodeViewport(
                    revisionKey, commandSequence, presentation);
            synchronized (stateLock) {
                if (state != State.READY
                        || !acceptedCapabilities.contains(
                                CanvasWireCapability.VIEWPORT_PRESENTATION_V1)
                        || !revisionKey.equals(latestExpectedRevision)) {
                    return;
                }
            }
            writer.write(
                    activePolicy,
                    new CanvasProcessFrame(
                            CanvasProcessFrameKind.CONTROL_JSON, control));
        } catch (CanvasRunnerControlException | RuntimeException failure) {
            fail("Canvas viewport request could not be encoded.");
        } catch (IOException failure) {
            fail("Canvas viewport request could not be written to runner stdin.");
        }
    }

    private void writeClose(
            CanvasHostClose close,
            CanvasProcessFramingPolicy closingPolicy) {
        boolean written = false;
        if (close != null && closingPolicy != null) {
            try {
                writer.write(
                        closingPolicy,
                        new CanvasProcessFrame(
                                CanvasProcessFrameKind.CONTROL_JSON,
                                wireCodec.encode(close)));
                written = true;
            } catch (Exception ignored) {
                // Explicit close remains best-effort and is never a user failure.
            }
        }
        closeWriteCompletion.complete(written);
        if (written) {
            completeAuthenticatedCloseIfReady();
        } else {
            authenticatedCloseCompletion.complete(false);
            queueTransportClose();
        }
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
                    authenticatedCloseCompletion.complete(false);
                    return;
                }
                fail(framingFailureReason(failure));
                return;
            } catch (IOException failure) {
                if (terminalOrClosing()) {
                    authenticatedCloseCompletion.complete(false);
                    return;
                }
                fail("Canvas runner stdout could not be read ("
                        + failure.getClass().getSimpleName() + ").");
                return;
            } catch (RuntimeException failure) {
                if (terminalOrClosing()) {
                    authenticatedCloseCompletion.complete(false);
                    return;
                }
                fail("Canvas runner stdout decoder failed internally ("
                        + failure.getClass().getSimpleName() + ").");
                return;
            }
            if (next.isEmpty()) {
                if (terminalOrClosing()) {
                    authenticatedCloseCompletion.complete(false);
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
            String reason = "Canvas runner reported "
                    + failure.code().name().toLowerCase(java.util.Locale.ROOT)
                    + ": " + failure.message();
            if (failure.fatal()) {
                fail(reason);
                return false;
            }
            // A non-fatal failure consumes its authenticated sequence but does
            // not end the wire session. In particular, one queued request may
            // be rejected while CLOSING before runner.closed is emitted.
            dispatch(() -> listener.warning(reason));
            return true;
        }
        if (message instanceof CanvasRunnerClosed) {
            synchronized (stateLock) {
                state = State.CLOSED;
                acceptedCapabilities = Set.of();
                latestExpectedRevision = null;
                authenticatedCloseReceived = true;
            }
            completeAuthenticatedCloseIfReady();
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
                    + "layout, selection and surface-presentation capabilities.");
            return false;
        }
        boolean closingDuringHandshake;
        synchronized (stateLock) {
            if (state != State.HANDSHAKING && state != State.CLOSING) {
                return false;
            }
            closingDuringHandshake = state == State.CLOSING;
            CanvasProcessFramingPolicy negotiatedRead =
                    CanvasProcessFramingPolicy.negotiated(
                    wireLimits,
                    negotiation,
                    CanvasProcessDirection.RUNNER_TO_HOST);
            CanvasProcessFramingPolicy negotiatedWrite =
                    CanvasProcessFramingPolicy.negotiated(
                    wireLimits,
                    negotiation,
                    CanvasProcessDirection.HOST_TO_RUNNER);
            readPolicy = closingDuringHandshake
                    ? negotiatedRead.closing()
                    : negotiatedRead;
            writePolicy = closingDuringHandshake
                    ? negotiatedWrite.closing()
                    : negotiatedWrite;
            if (!closingDuringHandshake) {
                acceptedCapabilities = Set.copyOf(
                        negotiation.acceptedCapabilities());
                state = State.READY;
            }
        }
        if (!closingDuringHandshake) {
            dispatch(() -> listener.ready(hello.engineIdentity()));
        }
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
            final boolean surfacePresentationNegotiated;
            synchronized (stateLock) {
                surfacePresentationNegotiated = acceptedCapabilities.contains(
                        CanvasWireCapability.SURFACE_PRESENTATION_V1);
            }
            if (!surfacePresentationNegotiated) {
                fail("Canvas runner presented a surface without negotiating "
                        + "its capability.");
                return false;
            }
            if (!exactSession(presented.layoutKey().sessionId())) {
                fail("Canvas runner presented a stale or foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.presented(presented));
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
        if (event instanceof CanvasRunnerRuntimeEvent.Interaction interaction) {
            if (!exactSession(interaction.intentKey().intentId().sessionId())
                    || !exactSession(
                            interaction.intentKey().layoutKey().sessionId())) {
                fail("Canvas runner sent a pointer interaction for a stale or "
                        + "foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.interaction(interaction));
            return true;
        }
        if (event instanceof CanvasRunnerRuntimeEvent.InteractionFenceApplied applied) {
            if (!exactSession(applied.layoutKey().sessionId())) {
                fail("Canvas runner acknowledged an interaction fence for a "
                        + "stale or foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.interactionFenceApplied(applied));
            return true;
        }
        if (event instanceof CanvasRunnerRuntimeEvent.ViewportMetrics viewport) {
            CanvasViewportMetrics metrics = viewport.metrics();
            final boolean viewportNegotiated;
            final boolean exactRevision;
            synchronized (stateLock) {
                viewportNegotiated = acceptedCapabilities.contains(
                        CanvasWireCapability.VIEWPORT_PRESENTATION_V1);
                exactRevision = metrics.revisionKey().equals(
                        latestExpectedRevision);
            }
            if (!viewportNegotiated) {
                fail("Canvas runner sent viewport metrics without negotiating "
                        + "its capability.");
                return false;
            }
            if (!exactSession(metrics.revisionKey().sessionId())) {
                fail("Canvas runner sent viewport metrics for a foreign Canvas session.");
                return false;
            }
            if (!exactRevision) {
                // A superseded asynchronous acknowledgement has no authority.
                return true;
            }
            dispatch(() -> listener.viewportMetrics(metrics));
            return true;
        }
        if (event instanceof CanvasRunnerRuntimeEvent.DeleteSelection deletion) {
            final boolean deleteNegotiated;
            synchronized (stateLock) {
                deleteNegotiated = acceptedCapabilities.contains(
                        CanvasWireCapability.DELETE_SELECTED_WIDGET_V1);
            }
            if (!deleteNegotiated) {
                fail("Canvas runner sent a widget deletion intent without "
                        + "negotiating its capability.");
                return false;
            }
            if (!exactSession(deletion.intentKey().intentId().sessionId())
                    || !exactSession(
                            deletion.intentKey().layoutKey().sessionId())) {
                fail("Canvas runner sent a widget deletion intent for a stale "
                        + "or foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.deleteSelection(deletion));
            return true;
        }
        if (event instanceof CanvasRunnerRuntimeEvent.TextEditCommit commit) {
            final boolean inlineTextEditNegotiated;
            synchronized (stateLock) {
                inlineTextEditNegotiated = acceptedCapabilities.contains(
                        CanvasWireCapability.INLINE_TEXT_EDIT_V1);
            }
            if (!inlineTextEditNegotiated) {
                fail("Canvas runner sent an inline text edit without "
                        + "negotiating its capability.");
                return false;
            }
            if (!exactSession(commit.intentKey().intentId().sessionId())
                    || !exactSession(
                            commit.intentKey().layoutKey().sessionId())) {
                fail("Canvas runner sent an inline text edit for a stale or "
                        + "foreign Canvas session.");
                return false;
            }
            dispatch(() -> listener.textEditCommit(commit));
            return true;
        }
        CanvasRunnerRuntimeEvent.PaletteDrop drop =
                (CanvasRunnerRuntimeEvent.PaletteDrop) event;
        final boolean paletteDropNegotiated;
        synchronized (stateLock) {
            paletteDropNegotiated = acceptedCapabilities.contains(
                    CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1);
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

    private static void requireCommandSequence(long commandSequence) {
        if (commandSequence < 1
                || commandSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Canvas viewport commandSequence must be between 1 and "
                    + CanvasWireProtocol.MAX_SEQUENCE + '.');
        }
    }

    private static void requirePreviewSequence(long previewSequence) {
        if (previewSequence < 1
                || previewSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Canvas widget move previewSequence must be between 1 and "
                    + CanvasWireProtocol.MAX_SEQUENCE + '.');
        }
    }

    private static void requireInteractionFenceSequence(long sequence) {
        if (sequence < 0 || sequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Canvas interaction fence sequence must be between 0 and "
                    + CanvasWireProtocol.MAX_SEQUENCE + '.');
        }
    }

    private static void requireInsertionIndex(int insertionIndex) {
        if (insertionIndex < 0
                || insertionIndex > WidgetSlot.MAX_LIST_CHILDREN) {
            throw new IllegalArgumentException(
                    "Canvas widget move insertionIndex must be between 0 and "
                    + WidgetSlot.MAX_LIST_CHILDREN + '.');
        }
    }

    private boolean exactSession(CanvasSessionId candidate) {
        return sessionId.equals(candidate);
    }

    /**
     * Releases the successful close barrier only after both independent facts
     * are true: the exact host.close frame left the writer and the exact-session
     * runner.closed reply passed the lifecycle gate. A guessed early reply can
     * therefore never authorize native-peer teardown or clear the pending close
     * frame from the serial outbound queue.
     */
    private void completeAuthenticatedCloseIfReady() {
        boolean ready;
        synchronized (stateLock) {
            ready = authenticatedCloseReceived
                    && Boolean.TRUE.equals(closeWriteCompletion.getNow(false));
        }
        if (ready && authenticatedCloseCompletion.complete(true)) {
            dispatch(listener::closed);
            queueTransportClose();
        }
    }

    private void fail(String reason) {
        boolean notify;
        synchronized (stateLock) {
            if (state == State.CLOSED || state == State.CLOSING
                    || state == State.FAILED) {
                authenticatedCloseCompletion.complete(false);
                return;
            }
            state = State.FAILED;
            latestExpectedRevision = null;
            notify = !failureDelivered;
            failureDelivered = true;
        }
        authenticatedCloseCompletion.complete(false);
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

    /**
     * Places the newest input fence ahead of controls that have not started
     * writing yet. The serial writer remains the sole frame writer, so this
     * never interleaves with or overtakes an already-started frame.
     */
    private boolean enqueuePriorityInteractionFence(
            InteractionFenceWrite task) {
        if (outboundExecutor.isShutdown()) {
            return false;
        }
        outboundQueue.removeIf(InteractionFenceWrite.class::isInstance);
        return outboundQueue.offerFirst(task);
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

        void presented(CanvasRunnerRuntimeEvent.Presented presented);

        void selection(CanvasIntentKey intentKey, StableId widgetId);

        /** Optional authenticated physical interaction on the native surface. */
        default void interaction(CanvasRunnerRuntimeEvent.Interaction interaction) {
        }

        /** Exact-layout acknowledgement that the runner applied an input fence. */
        default void interactionFenceApplied(
                CanvasRunnerRuntimeEvent.InteractionFenceApplied applied) {
        }

        /** Optional until the owning Designer session wires the mutation slice. */
        default void paletteDrop(CanvasRunnerRuntimeEvent.PaletteDrop drop) {
        }

        /** Optional until the owning Designer session wires the mutation slice. */
        default void deleteSelection(
                CanvasRunnerRuntimeEvent.DeleteSelection deletion) {
        }

        /** Optional final composition-free edit of one {@code Text.data}. */
        default void textEditCommit(
                CanvasRunnerRuntimeEvent.TextEditCommit commit) {
        }

        /** Optional runner-confirmed viewport presentation state. */
        default void viewportMetrics(CanvasViewportMetrics metrics) {
        }

        /** Authenticated acknowledgement of a host-initiated graceful close. */
        default void closed() {
        }

        /** Authenticated non-fatal runner diagnostic; the channel remains live. */
        default void warning(String reason) {
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

    private final class InteractionFenceWrite implements Runnable {
        private final CanvasLayoutKey layoutKey;
        private final long interactionFenceSequence;

        private InteractionFenceWrite(
                CanvasLayoutKey layoutKey,
                long interactionFenceSequence) {
            this.layoutKey = layoutKey;
            this.interactionFenceSequence = interactionFenceSequence;
        }

        @Override
        public void run() {
            writeInteractionFence(layoutKey, interactionFenceSequence);
        }
    }
}
