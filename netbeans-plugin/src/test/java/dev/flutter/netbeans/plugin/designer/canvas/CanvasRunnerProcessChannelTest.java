package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasDevicePixelRatio;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasIntentId;
import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasLocale;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasRenderProfile;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasSurfaceMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasTextScaleFactor;
import dev.flutter.netbeans.designer.canvas.CanvasThemeBrightness;
import dev.flutter.netbeans.designer.canvas.CanvasViewport;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.CanvasZoomMode;
import dev.flutter.netbeans.designer.canvas.ValidatedCanvasRevisionSnapshot;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasHostClose;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasHostHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerClosed;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerFailure;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasRunnerHello;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCapability;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCloseReason;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCodec;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireDecodeResult;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireFailureCode;
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
import dev.flutter.netbeans.designer.model.SlotName;
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
import java.util.OptionalLong;
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
            CanvasWireCapability.READ_ONLY_SELECTION,
            CanvasWireCapability.SURFACE_PRESENTATION_V1,
            CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1,
            CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1,
            CanvasWireCapability.DELETE_SELECTED_WIDGET_V1,
            CanvasWireCapability.INLINE_TEXT_EDIT_V1,
            CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1,
            CanvasWireCapability.VIEWPORT_PRESENTATION_V1);

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
        assertTrue(harness.channel.supports(
                CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1));
        assertTrue(harness.channel.supports(
                CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1));
    }

    @Test
    void projectsPaletteSourceOnlyWhenBothDropCapabilitiesAndRevisionMatch()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        harness.channel.expectPresentation(request.revisionKey());
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 3), 5);
        String token = "nbfdnd:v1:6a7bab32-9507-4f6d-b986-39f183742017:"
                + "f83e4ad8-e66f-43ae-a5b3-cb057809f17e";

        assertTrue(harness.channel.authorizePaletteDragSource(
                layout,
                token,
                new WidgetTypeId("flutter.material.AppBar"),
                java.util.Set.of("flutter.widgets.PreferredSizeWidget")));
        List<CanvasProcessFrame> frames = harness.awaitHostFrames(2);
        String control = new String(
                frames.get(1).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(control.contains("\"type\":\"host.paletteDragSource\""));
        assertTrue(control.contains("\"token\":\"" + token + "\""));
        assertTrue(control.contains("\"widgetType\":\"flutter.material.AppBar\""));
        assertTrue(control.contains(
                "\"traits\":[\"flutter.widgets.PreferredSizeWidget\"]"));

        CanvasLayoutKey stale = new CanvasLayoutKey(
                new CanvasFrameKey(renderRequest(
                        harness.sessionId, 8).revisionKey(), 3), 5);
        assertFalse(harness.channel.authorizePaletteDragSource(
                stale,
                token,
                new WidgetTypeId("flutter.material.AppBar"),
                java.util.Set.of("flutter.widgets.PreferredSizeWidget")));
    }

    @Test
    void paletteSourceProjectionRequiresTheSourceAwareCapability() throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);
        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT,
                CanvasWireCapability.READ_ONLY_SELECTION,
                CanvasWireCapability.SURFACE_PRESENTATION_V1,
                CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1));
        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        harness.channel.expectPresentation(request.revisionKey());
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 3), 5);

        assertFalse(harness.channel.authorizePaletteDragSource(
                layout,
                "nbfdnd:v1:6a7bab32-9507-4f6d-b986-39f183742017:"
                        + "f83e4ad8-e66f-43ae-a5b3-cb057809f17e",
                new WidgetTypeId("flutter.material.AppBar"),
                java.util.Set.of("flutter.widgets.PreferredSizeWidget")));
        assertEquals(1, harness.awaitHostFrames(1).size());
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
    void rejectsHandshakeWithoutRequiredSurfacePresentationCapability()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.awaitHostFrames(1);

        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT,
                CanvasWireCapability.READ_ONLY_SELECTION));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains(
                "surface-presentation"));
        assertFalse(harness.channel.isReady());
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
    void writesAndDispatchesViewportForTheExactNegotiatedRevision()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasViewportPresentation presentation =
                CanvasViewportPresentation.manual(
                        1_250_000, 400_000, 600_000);
        harness.channel.expectPresentation(request.revisionKey());

        assertTrue(harness.channel.viewport(
                request.revisionKey(), 1, presentation));

        List<CanvasProcessFrame> frames = harness.awaitHostFrames(2);
        String viewport = new String(
                frames.get(1).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(viewport.contains("\"type\":\"host.viewport\""));
        assertTrue(viewport.contains("\"commandSequence\":1"));
        assertTrue(viewport.contains("\"mode\":\"manual\""));
        assertTrue(viewport.contains("\"zoomMicros\":1250000"));
        assertTrue(viewport.contains(
                "\"horizontalScrollMicros\":400000"));
        assertTrue(viewport.contains("\"verticalScrollMicros\":600000"));

        harness.sendRuntime(viewportMetrics(
                request.revisionKey(), 1, presentation,
                1_250_000, true, false));

        assertTrue(harness.listener.viewportMetrics.await(
                2, TimeUnit.SECONDS));
        CanvasViewportMetrics metrics = harness.listener.metrics;
        assertEquals(request.revisionKey(), metrics.revisionKey());
        assertEquals(1, metrics.commandSequence());
        assertEquals(presentation, metrics.presentation());
        assertEquals(1_250_000, metrics.effectiveScaleMicros());
        assertTrue(metrics.horizontalScrollable());
        assertFalse(metrics.verticalScrollable());
        assertEquals(1, harness.listener.viewportMetricsCalls.get());
    }

    @Test
    void rejectsOutOfRangeViewportCommandBeforeQueueingIt() throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        harness.channel.expectPresentation(request.revisionKey());

        assertThrows(IllegalArgumentException.class,
                () -> harness.channel.viewport(
                        request.revisionKey(),
                        0,
                        CanvasViewportPresentation.fit()));
        assertThrows(IllegalArgumentException.class,
                () -> harness.channel.viewport(
                        request.revisionKey(),
                        9_007_199_254_740_992L,
                        CanvasViewportPresentation.fit()));
        assertEquals(1, harness.awaitHostFrames(1).size());
    }

    @Test
    void discardsSupersededViewportMetricsWithoutPoisoningTheChannel()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest stale = renderRequest(harness.sessionId, 7);
        CanvasRenderRequest current = renderRequest(harness.sessionId, 8);
        harness.channel.expectPresentation(stale.revisionKey());
        harness.channel.expectPresentation(current.revisionKey());

        harness.sendRuntime(viewportMetrics(
                stale.revisionKey(),
                1,
                CanvasViewportPresentation.fit(),
                750_000,
                false,
                false));

        assertFalse(harness.listener.viewportMetrics.await(
                100, TimeUnit.MILLISECONDS));
        assertEquals(0, harness.listener.viewportMetricsCalls.get());
        assertTrue(harness.channel.isReady());

        harness.sendRuntime(viewportMetrics(
                current.revisionKey(),
                2,
                CanvasViewportPresentation.fit(),
                750_000,
                false,
                false));
        assertTrue(harness.listener.viewportMetrics.await(
                2, TimeUnit.SECONDS));
        assertEquals(current.revisionKey(),
                harness.listener.metrics.revisionKey());
    }

    @Test
    void viewportRequiresNegotiatedCapabilityForBothDirections()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT,
                CanvasWireCapability.READ_ONLY_SELECTION,
                CanvasWireCapability.SURFACE_PRESENTATION_V1));
        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        harness.channel.expectPresentation(request.revisionKey());

        assertFalse(harness.channel.viewport(
                request.revisionKey(), 1, CanvasViewportPresentation.fit()));
        assertEquals(1, harness.awaitHostFrames(1).size());

        harness.sendRuntime(viewportMetrics(
                request.revisionKey(),
                1,
                CanvasViewportPresentation.fit(),
                750_000,
                false,
                false));
        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains(
                "without negotiating"));
        assertEquals(0, harness.listener.viewportMetricsCalls.get());
    }

    @Test
    void rejectsForeignViewportMetricsWhenCapabilityIsNegotiated()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        harness.channel.expectPresentation(request.revisionKey());
        CanvasRevisionKey foreign = renderRequest(
                CanvasSessionId.random()).revisionKey();

        harness.sendRuntime(viewportMetrics(
                foreign,
                1,
                CanvasViewportPresentation.fit(),
                750_000,
                false,
                false));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("foreign"));
        assertEquals(0, harness.listener.viewportMetricsCalls.get());
    }

    @Test
    void dispatchesPresentedFenceSelectionInteractionPaletteDropAndDeleteForExactSession()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasRevisionKey revision = request.revisionKey();
        StableId widgetId = request.snapshot().document().root().id();
        String token = "nbfdnd:v1:opaque-token";

        harness.sendRuntime(presented(revision, 3, 5));
        harness.sendRuntime(interactionFenceApplied(revision, 3, 5, 17));
        harness.sendRuntime(selection(revision, 3, 5, 9, widgetId));
        harness.sendRuntime(interaction(revision, 3, 5, 10, 17));
        harness.sendRuntime(paletteDrop(
                revision, 3, 5, 10, token, widgetId, 2));
        harness.sendRuntime(deleteSelection(revision, 3, 5, 11, widgetId));

        assertTrue(harness.listener.presented.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.interactionFenceApplied.await(
                2, TimeUnit.SECONDS));
        assertTrue(harness.listener.selection.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.interaction.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.paletteDrop.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.deleteSelection.await(2, TimeUnit.SECONDS));
        CanvasLayoutKey layout = harness.listener.layout;
        assertEquals(new CanvasLayoutKey(new CanvasFrameKey(revision, 3), 5), layout);
        assertEquals(new CanvasSurfaceMetrics(1_600, 900, 1_500_000),
                harness.listener.presentedEvent.metrics());
        assertEquals(layout, harness.listener.appliedFence.layoutKey());
        assertEquals(17, harness.listener.appliedFence.interactionFenceSequence());
        assertEquals(widgetId, harness.listener.widgetId);
        assertEquals(9, harness.listener.intent.intentId().intentSequence());
        assertEquals(new CanvasIntentKey(
                new CanvasIntentId(harness.sessionId, 10), layout),
                harness.listener.pointerInteraction.intentKey());
        assertEquals(17,
                harness.listener.pointerInteraction.interactionFenceSequence());
        assertEquals(1, harness.listener.interactionCalls.get());
        CanvasRunnerRuntimeEvent.PaletteDrop drop = harness.listener.drop;
        assertEquals(new CanvasIntentKey(
                new CanvasIntentId(harness.sessionId, 10),
                layout), drop.intentKey());
        assertEquals(token, drop.token());
        assertEquals(widgetId, drop.parentWidgetId());
        assertEquals(new SlotName("children"), drop.slotName());
        assertEquals(2, drop.insertionIndex());
        assertEquals(1, harness.listener.paletteDropCalls.get());
        assertEquals(new CanvasIntentKey(
                new CanvasIntentId(harness.sessionId, 11), layout),
                harness.listener.deletion.intentKey());
        assertEquals(widgetId, harness.listener.deletion.widgetId());
        assertEquals(1, harness.listener.deleteSelectionCalls.get());
        assertTrue(harness.listener.failureReason == null);
    }

    @Test
    void dispatchesInlineTextEditForTheExactNegotiatedSession()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasRevisionKey revision = request.revisionKey();
        StableId widgetId = request.snapshot().document().root().id();
        String text = "Привіт 你好 😀 e\u0301";

        harness.sendRuntime(textEditCommit(
                revision, 3, 5, 7, 11, widgetId, text, true));

        assertTrue(harness.listener.textEditCommit.await(2, TimeUnit.SECONDS));
        CanvasRunnerRuntimeEvent.TextEditCommit commit =
                harness.listener.inlineTextEdit;
        assertEquals(new CanvasIntentKey(
                new CanvasIntentId(harness.sessionId, 7),
                new CanvasLayoutKey(new CanvasFrameKey(revision, 3), 5)),
                commit.intentKey());
        assertEquals(11, commit.interactionFenceSequence());
        assertEquals(widgetId, commit.widgetId());
        assertEquals(text, commit.text());
        assertTrue(commit.compositionObserved());
        assertEquals(1, harness.listener.textEditCommitCalls.get());
        assertTrue(harness.listener.failureReason == null);
    }

    @Test
    void rejectsInlineTextEditWhenCapabilityWasNotNegotiated()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.sendHello(ALL_CAPABILITIES.stream()
                .filter(capability -> capability
                        != CanvasWireCapability.INLINE_TEXT_EDIT_V1)
                .toList());
        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        StableId widgetId = request.snapshot().document().root().id();

        harness.sendRuntime(textEditCommit(
                request.revisionKey(), 0, 0, 0, 0,
                widgetId, "blocked", false));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains(
                "without negotiating"));
        assertEquals(0, harness.listener.textEditCommitCalls.get());
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsInlineTextEditFromAnotherSessionWithoutDispatchingIt()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest foreign = renderRequest(CanvasSessionId.random());
        StableId widgetId = foreign.snapshot().document().root().id();

        harness.sendRuntime(textEditCommit(
                foreign.revisionKey(), 0, 0, 0, 0,
                widgetId, "foreign", false));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("stale or foreign"));
        assertEquals(0, harness.listener.textEditCommitCalls.get());
        assertFalse(harness.channel.isReady());
    }

    @Test
    void presentedCodecRequiresExactBoundedSurfaceMetrics() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        String valid = presented(request.revisionKey(), 3, 5);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        CanvasRunnerRuntimeEvent.Presented decoded = assertInstanceOf(
                CanvasRunnerRuntimeEvent.Presented.class,
                assertDoesNotThrow(() -> codec.decode(
                        valid.getBytes(StandardCharsets.UTF_8))));
        assertEquals(new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 3), 5),
                decoded.layoutKey());
        assertEquals(new CanvasSurfaceMetrics(1_600, 900, 1_500_000),
                decoded.metrics());

        List<String> malformed = List.of(
                valid.replace(",\"physicalWidth\":1600", ""),
                valid.replace(
                        "\"physicalWidth\":1600",
                        "\"unexpected\":true,\"physicalWidth\":1600"),
                valid.replace(
                        "\"physicalWidth\":1600",
                        "\"physicalWidth\":0"),
                valid.replace(
                        "\"physicalHeight\":900",
                        "\"physicalHeight\":4097"),
                valid.replace(
                        "\"physicalWidth\":1600,\"physicalHeight\":900",
                        "\"physicalWidth\":4096,\"physicalHeight\":4096"),
                valid.replace(
                        "\"devicePixelRatioMicros\":1500000",
                        "\"devicePixelRatioMicros\":0"),
                valid.replace(
                        "\"devicePixelRatioMicros\":1500000",
                        "\"devicePixelRatioMicros\":10000001"),
                valid.replace(
                        "\"devicePixelRatioMicros\":1500000",
                        "\"devicePixelRatioMicros\":1.5"));
        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void deleteSelectionCodecRequiresExactBoundedIdentityAndStableWidget() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        StableId widgetId = request.snapshot().document().root().id();
        String valid = deleteSelection(
                request.revisionKey(), 3, 5, 10, widgetId);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        CanvasRunnerRuntimeEvent.DeleteSelection decoded = assertInstanceOf(
                CanvasRunnerRuntimeEvent.DeleteSelection.class,
                assertDoesNotThrow(() -> codec.decode(
                        valid.getBytes(StandardCharsets.UTF_8))));
        assertEquals(widgetId, decoded.widgetId());
        List<String> malformed = List.of(
                valid.replace("\"widgetId\":", "\"unexpected\":true,\"widgetId\":"),
                valid.replace(",\"intentSequence\":10", ""),
                valid.replace(widgetId.toString(), "not-a-stable-id"),
                valid.replace("\"layoutSequence\":5", "\"layoutSequence\":-1"),
                valid.replace("\"intentSequence\":10", "\"intentSequence\":2.5"));
        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void interactionCodecRequiresExactBoundedIntentIdentity() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        String valid = interaction(request.revisionKey(), 3, 5, 10, 17);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        CanvasRunnerRuntimeEvent.Interaction decoded = assertInstanceOf(
                CanvasRunnerRuntimeEvent.Interaction.class,
                assertDoesNotThrow(() -> codec.decode(
                        valid.getBytes(StandardCharsets.UTF_8))));
        assertEquals(new CanvasIntentKey(
                new CanvasIntentId(request.revisionKey().sessionId(), 10),
                new CanvasLayoutKey(
                        new CanvasFrameKey(request.revisionKey(), 3), 5)),
                decoded.intentKey());
        assertEquals(17, decoded.interactionFenceSequence());
        List<String> malformed = List.of(
                valid.replace(
                        "\"interactionFenceSequence\":17}",
                        "\"interactionFenceSequence\":17,\"unexpected\":true}"),
                valid.replace(",\"intentSequence\":10", ""),
                valid.replace(",\"interactionFenceSequence\":17", ""),
                valid.replace("\"layoutSequence\":5", "\"layoutSequence\":-1"),
                valid.replace("\"intentSequence\":10", "\"intentSequence\":2.5"),
                valid.replace(
                        "\"interactionFenceSequence\":17",
                        "\"interactionFenceSequence\":-1"),
                valid.replace(
                        "\"interactionFenceSequence\":17",
                        "\"interactionFenceSequence\":2.5"));
        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void interactionFenceAppliedCodecRequiresExactBoundedLayoutIdentity() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        String valid = interactionFenceApplied(
                request.revisionKey(), 3, 5, 17);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        CanvasRunnerRuntimeEvent.InteractionFenceApplied decoded =
                assertInstanceOf(
                        CanvasRunnerRuntimeEvent.InteractionFenceApplied.class,
                        assertDoesNotThrow(() -> codec.decode(
                                valid.getBytes(StandardCharsets.UTF_8))));
        assertEquals(new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 3), 5),
                decoded.layoutKey());
        assertEquals(17, decoded.interactionFenceSequence());
        List<String> malformed = List.of(
                valid.replace(
                        "\"interactionFenceSequence\":17}",
                        "\"interactionFenceSequence\":17,\"unexpected\":true}"),
                valid.replace(",\"layoutSequence\":5", ""),
                valid.replace(",\"interactionFenceSequence\":17", ""),
                valid.replace("\"frameSequence\":3", "\"frameSequence\":-1"),
                valid.replace("\"layoutSequence\":5", "\"layoutSequence\":2.5"),
                valid.replace(
                        "\"interactionFenceSequence\":17",
                        "\"interactionFenceSequence\":-1"),
                valid.replace(
                        "\"interactionFenceSequence\":17",
                        "\"interactionFenceSequence\":2.5"));
        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void paletteDropCodecAcceptsEveryReviewedAppBarAndCoreSlot() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        StableId parent = request.snapshot().document().root().id();
        String valid = paletteDrop(
                request.revisionKey(), 3, 5, 10,
                "nbfdnd:v1:opaque-token", parent, 2);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();

        CanvasRunnerControlException operation = assertThrows(
                CanvasRunnerControlException.class,
                () -> codec.decode(valid.replace(
                        "\"operation\":\"ADD\"",
                        "\"operation\":\"MOVE\"")
                        .getBytes(StandardCharsets.UTF_8)));
        for (String slotName : List.of(
                "child", "body", "floatingActionButton", "appBar",
                "leading", "title", "actions", "flexibleSpace", "bottom")) {
            CanvasRunnerRuntimeEvent.PaletteDrop decoded = assertInstanceOf(
                    CanvasRunnerRuntimeEvent.PaletteDrop.class,
                    assertDoesNotThrow(() -> codec.decode(valid.replace(
                            "\"slotName\":\"children\"",
                            "\"slotName\":\"" + slotName + "\"")
                            .replace("\"insertionIndex\":2",
                                    "\"insertionIndex\":0")
                            .getBytes(StandardCharsets.UTF_8))), slotName);
            assertEquals(new SlotName(slotName), decoded.slotName());
            assertEquals(0, decoded.insertionIndex());
        }
        CanvasRunnerControlException unknown = assertThrows(
                CanvasRunnerControlException.class,
                () -> codec.decode(valid.replace(
                        "\"slotName\":\"children\"",
                        "\"slotName\":\"unknownSlot\"")
                        .getBytes(StandardCharsets.UTF_8)));

        assertTrue(operation.getMessage().contains("operation"));
        assertTrue(unknown.getMessage().contains("slotName"));
    }

    @Test
    void paletteDropCodecRejectsUntrustedTokens() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        StableId parent = request.snapshot().document().root().id();
        String valid = paletteDrop(
                request.revisionKey(), 3, 5, 10,
                "nbfdnd:v1:opaque-token", parent, 2);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();
        List<String> invalidTokens = List.of(
                "foreign:v1:opaque-token",
                "nbfdnd:v1:opaque\\ntoken",
                "nbfdnd:v1:" + "x".repeat(151));

        for (String invalidToken : invalidTokens) {
            String malformed = valid.replace(
                    "nbfdnd:v1:opaque-token", invalidToken);
            CanvasRunnerControlException failure = assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(malformed.getBytes(StandardCharsets.UTF_8)),
                    invalidToken);
            assertTrue(failure.getMessage().contains("token"), invalidToken);
        }
    }

    @Test
    void paletteDropCodecRequiresExactFieldsAndBoundedInsertionIndex() {
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        StableId parent = request.snapshot().document().root().id();
        String valid = paletteDrop(
                request.revisionKey(), 3, 5, 10,
                "nbfdnd:v1:opaque-token", parent, 2);
        CanvasRunnerControlCodec codec = new CanvasRunnerControlCodec();
        List<String> malformed = List.of(
                valid.replace(
                        "\"insertionIndex\":2}",
                        "\"insertionIndex\":2,\"unexpected\":true}"),
                valid.replace(",\"intentSequence\":10", ""),
                valid.replace(parent.toString(), "not-a-stable-id"),
                valid.replace("\"insertionIndex\":2", "\"insertionIndex\":-1"),
                valid.replace("\"insertionIndex\":2", "\"insertionIndex\":10001"),
                valid.replace("\"insertionIndex\":2", "\"insertionIndex\":2.5"));

        for (String payload : malformed) {
            assertThrows(
                    CanvasRunnerControlException.class,
                    () -> codec.decode(payload.getBytes(StandardCharsets.UTF_8)));
        }
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
    void rejectsAPaletteDropFromAnotherSessionWithoutDispatchingIt()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        StableId parent = request.snapshot().document().root().id();

        harness.sendRuntime(paletteDrop(
                request.revisionKey(), 0, 0, 0,
                "nbfdnd:v1:foreign", parent, 0));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("stale or foreign"));
        assertEquals(0, harness.listener.paletteDropCalls.get());
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsADeleteIntentFromAnotherSessionWithoutDispatchingIt()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());
        StableId widgetId = request.snapshot().document().root().id();

        harness.sendRuntime(deleteSelection(
                request.revisionKey(), 0, 0, 0, widgetId));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("stale or foreign"));
        assertEquals(0, harness.listener.deleteSelectionCalls.get());
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsAnInteractionFromAnotherSessionWithoutDispatchingIt()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(CanvasSessionId.random());

        harness.sendRuntime(interaction(request.revisionKey(), 0, 0, 0, 0));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("stale or foreign"));
        assertEquals(0, harness.listener.interactionCalls.get());
        assertFalse(harness.channel.isReady());
    }

    @Test
    void keepsReadOnlyCanvasReadyWhenOptionalPaletteDropIsUnavailable()
            throws Exception {
        harness = new Harness();
        harness.channel.start();

        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT,
                CanvasWireCapability.READ_ONLY_SELECTION,
                CanvasWireCapability.SURFACE_PRESENTATION_V1));

        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        assertTrue(harness.channel.isReady());
        assertFalse(harness.channel.supports(
                CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1));

        CanvasRenderRequest request = renderRequest(harness.sessionId);
        StableId parent = request.snapshot().document().root().id();
        harness.sendRuntime(paletteDrop(
                request.revisionKey(), 0, 0, 0,
                "nbfdnd:v1:unnegotiated", parent, 0));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("without negotiating"));
        assertEquals(0, harness.listener.paletteDropCalls.get());
        assertFalse(harness.channel.isReady());
    }

    @Test
    void rejectsDeleteIntentWhenOptionalCapabilityWasNotNegotiated()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT,
                CanvasWireCapability.READ_ONLY_SELECTION,
                CanvasWireCapability.SURFACE_PRESENTATION_V1));
        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));

        CanvasRenderRequest request = renderRequest(harness.sessionId);
        StableId widgetId = request.snapshot().document().root().id();
        harness.sendRuntime(deleteSelection(
                request.revisionKey(), 0, 0, 0, widgetId));

        assertTrue(harness.listener.failed.await(2, TimeUnit.SECONDS));
        assertTrue(harness.listener.failureReason.contains("without negotiating"));
        assertEquals(0, harness.listener.deleteSelectionCalls.get());
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
    void sendsHostInteractionFenceForTheExactPresentedLayout() throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 4), 6);

        harness.channel.expectPresentation(request.revisionKey());
        assertTrue(harness.channel.interactionFence(layout, 17));

        List<CanvasProcessFrame> frames = harness.awaitHostFrames(2);
        assertEquals(2, frames.size());
        String fence = new String(
                frames.get(1).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(fence.contains("\"type\":\"host.interactionFence\""));
        assertTrue(fence.contains("\"frameSequence\":4"));
        assertTrue(fence.contains("\"layoutSequence\":6"));
        assertTrue(fence.contains("\"interactionFenceSequence\":17"));
    }

    @Test
    void closeWinningAdmissionRejectsLaterFenceAndWritesNoFenceAheadOfClose()
            throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 4), 6);
        harness.channel.expectPresentation(request.revisionKey());

        harness.channel.requestClose(CanvasWireCloseReason.BACKEND_REPLACED);
        assertFalse(harness.channel.interactionFence(layout, 17));
        assertTrue(harness.channel.awaitCloseRequestWritten(
                Duration.ofSeconds(2)));

        List<CanvasProcessFrame> frames = harness.awaitHostFrames(2);
        assertEquals(2, frames.size());
        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class, lifecycle(frames.getLast()));
        assertEquals(CanvasWireCloseReason.BACKEND_REPLACED, close.reason());
        String wire = new String(
                harness.hostStdin.toByteArray(), StandardCharsets.UTF_8);
        assertFalse(wire.contains("\"type\":\"host.interactionFence\""));
    }

    @Test
    void newestInteractionFencePreemptsQueuedControlsButNotStartedFrame()
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

            CanvasRenderRequest request = renderRequest(sessionId);
            CanvasLayoutKey layout = new CanvasLayoutKey(
                    new CanvasFrameKey(request.revisionKey(), 4), 6);
            channel.expectPresentation(request.revisionKey());
            StableId started = StableId.random();
            StableId queuedFirst = StableId.random();
            StableId queuedLast = StableId.random();

            stdin.blockNextWrite();
            assertTrue(channel.select(layout, started));
            assertTrue(stdin.writeBlocked.await(2, TimeUnit.SECONDS));
            assertTrue(channel.select(layout, queuedFirst));
            assertTrue(channel.interactionFence(layout, 17));
            assertTrue(channel.select(layout, queuedLast));
            assertTrue(channel.interactionFence(layout, 18));

            stdin.releaseWrite();
            awaitBytesContaining(stdin, queuedLast.toString());
            List<CanvasProcessFrame> frames = readHostFrames(
                    stdin.toByteArray());
            assertEquals(5, frames.size());
            String first = new String(
                    frames.get(1).copyPayload(), StandardCharsets.UTF_8);
            String fence = new String(
                    frames.get(2).copyPayload(), StandardCharsets.UTF_8);
            String second = new String(
                    frames.get(3).copyPayload(), StandardCharsets.UTF_8);
            String third = new String(
                    frames.get(4).copyPayload(), StandardCharsets.UTF_8);
            assertTrue(first.contains(started.toString()));
            assertTrue(fence.contains("\"type\":\"host.interactionFence\""));
            assertTrue(fence.contains("\"interactionFenceSequence\":18"));
            assertFalse(fence.contains("\"interactionFenceSequence\":17"));
            assertTrue(second.contains(queuedFirst.toString()));
            assertTrue(third.contains(queuedLast.toString()));
            String wire = new String(
                    stdin.toByteArray(), StandardCharsets.UTF_8);
            assertEquals(1, occurrences(
                    wire, "\"type\":\"host.interactionFence\""));
        } finally {
            stdin.releaseWrite();
            channel.close();
        }
    }

    @Test
    void sendsAndClearsAnExactNegotiatedWidgetMovePreview() throws Exception {
        harness = Harness.ready();
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 4), 6);
        StableId sourceId = StableId.random();
        StableId parentId = StableId.random();

        harness.channel.expectPresentation(request.revisionKey());
        assertTrue(harness.channel.previewWidgetMove(
                layout,
                1,
                sourceId,
                parentId,
                new SlotName("children"),
                3));
        List<CanvasProcessFrame> previewFrames = harness.awaitHostFrames(2);
        assertEquals(2, previewFrames.size());
        String preview = new String(
                previewFrames.get(1).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(preview.contains("\"type\":\"host.widgetMovePreview\""));
        assertTrue(preview.contains("\"previewSequence\":1"));
        assertTrue(preview.contains("\"sourceWidgetId\":\"" + sourceId + "\""));
        assertTrue(preview.contains("\"parentWidgetId\":\"" + parentId + "\""));
        assertTrue(preview.contains("\"slotName\":\"children\""));
        assertTrue(preview.contains("\"insertionIndex\":3"));
        assertTrue(preview.contains("\"frameSequence\":4"));
        assertTrue(preview.contains("\"layoutSequence\":6"));

        assertTrue(harness.channel.clearWidgetMovePreview(layout, 2));
        List<CanvasProcessFrame> clearFrames = harness.awaitHostFrames(3);
        assertEquals(3, clearFrames.size());
        String clear = new String(
                clearFrames.get(2).copyPayload(), StandardCharsets.UTF_8);
        assertTrue(clear.contains("\"type\":\"host.widgetMovePreviewClear\""));
        assertTrue(clear.contains("\"previewSequence\":2"));
        assertTrue(clear.contains("\"frameSequence\":4"));
        assertTrue(clear.contains("\"layoutSequence\":6"));
        assertFalse(clear.contains("sourceWidgetId"));
    }

    @Test
    void widgetMovePreviewIsOptionalAndRequiresExactCurrentRevision()
            throws Exception {
        harness = new Harness();
        harness.channel.start();
        harness.sendHello(List.of(
                CanvasWireCapability.READ_ONLY_RENDER,
                CanvasWireCapability.READ_ONLY_LAYOUT,
                CanvasWireCapability.READ_ONLY_SELECTION,
                CanvasWireCapability.SURFACE_PRESENTATION_V1));
        assertTrue(harness.listener.ready.await(2, TimeUnit.SECONDS));
        CanvasRenderRequest request = renderRequest(harness.sessionId);
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(request.revisionKey(), 0), 0);
        harness.channel.expectPresentation(request.revisionKey());

        assertFalse(harness.channel.previewWidgetMove(
                layout,
                1,
                StableId.random(),
                StableId.random(),
                new SlotName("children"),
                0));
        assertFalse(harness.channel.clearWidgetMovePreview(layout, 2));
        assertEquals(1, harness.awaitHostFrames(1).size());
        assertTrue(harness.channel.isReady());
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
        assertTrue(channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));

        List<CanvasProcessFrame> frames = awaitHostFrames(stdin, 2);
        assertEquals(2, frames.size());
        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class, lifecycle(frames.get(1)));
        assertEquals(CanvasWireCloseReason.FORM_CLOSED, close.reason());
        assertFalse(listener.closed.await(100, TimeUnit.MILLISECONDS));
        assertFalse(channel.awaitAuthenticatedClose(Duration.ofMillis(20)));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(
                        new CanvasRunnerClosed(sessionId, 1, close.sequence()))));
        assertTrue(channel.awaitAuthenticatedClose(Duration.ofSeconds(2)));
        assertTrue(listener.closed.await(2, TimeUnit.SECONDS));
        assertEquals(0, process.destroyCalls.get());
    }

    @Test
    void firstCloseReasonSurvivesRepeatedLifecycleCloseRequests() throws Exception {
        harness = Harness.ready();

        harness.channel.requestClose(CanvasWireCloseReason.BACKEND_REPLACED);
        harness.channel.close();
        assertTrue(harness.channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));

        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class,
                lifecycle(harness.awaitHostFrames(2).getLast()));
        assertEquals(CanvasWireCloseReason.BACKEND_REPLACED, close.reason());
    }

    @Test
    void foreignCloseAcknowledgementCannotReleaseTheAuthenticatedBarrier()
            throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout.input, stdin, sessionId, Runnable::run, listener);
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
        assertTrue(channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));
        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class,
                lifecycle(awaitHostFrames(stdin, 2).getLast()));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(new CanvasRunnerClosed(
                        CanvasSessionId.random(), 1, close.sequence()))));

        assertTimeoutPreemptively(
                Duration.ofMillis(250),
                () -> assertFalse(channel.awaitAuthenticatedClose(
                        Duration.ofSeconds(2))));
        assertFalse(listener.failed.await(100, TimeUnit.MILLISECONDS));
        assertFalse(listener.closed.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void exactCloseAcknowledgementCannotPassBeforeCloseFrameIsWritten()
            throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        GateOutputStream stdin = new GateOutputStream();
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout.input, stdin, sessionId, Runnable::run, listener);
        CanvasProcessFrameWriter runner = new CanvasProcessFrameCodec().writer(
                stdout.output);
        channel.start();
        awaitBytesContaining(stdin, "host.hello");
        runner.write(
                handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(runnerHello(
                        sessionId, ALL_CAPABILITIES))));
        assertTrue(listener.ready.await(2, TimeUnit.SECONDS));

        stdin.blockNextWrite();
        channel.close();
        assertTrue(stdin.writeBlocked.await(2, TimeUnit.SECONDS));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(
                        new CanvasRunnerClosed(sessionId, 1, 1))));

        assertFalse(channel.awaitAuthenticatedClose(Duration.ofMillis(20)));
        channel.close();
        assertFalse(channel.awaitAuthenticatedClose(Duration.ofMillis(20)),
                "an idempotent close must not reject a valid early acknowledgement");
        assertFalse(listener.closed.await(100, TimeUnit.MILLISECONDS));
        stdin.releaseWrite();
        assertTrue(channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));
        assertTrue(channel.awaitAuthenticatedClose(Duration.ofSeconds(2)));
        assertTrue(listener.closed.await(2, TimeUnit.SECONDS));
    }

    @Test
    void nonFatalFailureDuringCloseDoesNotConsumeTheClosedAcknowledgement()
            throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout.input, stdin, sessionId, Runnable::run, listener);
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
        assertTrue(channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));
        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class,
                lifecycle(awaitHostFrames(stdin, 2).getLast()));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST).closing(),
                control(new CanvasWireCodec().encode(new CanvasRunnerFailure(
                        sessionId,
                        1,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INVALID_REQUEST,
                        false,
                        "Queued render request was rejected."))));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST).closing(),
                control(new CanvasWireCodec().encode(
                        new CanvasRunnerClosed(sessionId, 2, close.sequence()))));

        assertTrue(listener.warning.await(2, TimeUnit.SECONDS));
        assertTrue(channel.awaitAuthenticatedClose(Duration.ofSeconds(2)));
        assertTrue(listener.closed.await(2, TimeUnit.SECONDS));
        assertFalse(listener.failed.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void abortCompletesTheAuthenticatedCloseBarrierAsRejected() {
        BlockingPipe stdout = new BlockingPipe();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout.input,
                new ByteArrayOutputStream(),
                CanvasSessionId.random(),
                Runnable::run,
                new RecordingListener());

        channel.abort();

        assertTimeoutPreemptively(
                Duration.ofMillis(250),
                () -> assertFalse(channel.awaitAuthenticatedClose(
                        Duration.ofSeconds(2))));
    }

    @Test
    void eofDuringCloseCompletesTheAuthenticatedCloseBarrierAsRejected()
            throws Exception {
        harness = Harness.ready();
        harness.channel.close();
        assertTrue(harness.channel.awaitCloseRequestWritten(
                Duration.ofSeconds(2)));

        harness.closeRunnerStdout();

        assertTimeoutPreemptively(
                Duration.ofMillis(250),
                () -> assertFalse(harness.channel.awaitAuthenticatedClose(
                        Duration.ofSeconds(2))));
        assertFalse(harness.listener.closed.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void closeDuringHandshakePreservesHelloAndAdmitsAuthenticatedClosedAck()
            throws Exception {
        BlockingPipe stdout = new BlockingPipe();
        GateOutputStream stdin = new GateOutputStream();
        RecordingListener listener = new RecordingListener();
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                stdout.input, stdin, sessionId, Runnable::run, listener);
        CanvasProcessFrameWriter runner = new CanvasProcessFrameCodec().writer(
                stdout.output);

        stdin.blockNextWrite();
        channel.start();
        assertTrue(stdin.writeBlocked.await(2, TimeUnit.SECONDS));
        assertTimeoutPreemptively(Duration.ofMillis(250), channel::close);
        assertFalse(channel.awaitCloseRequestWritten(Duration.ofMillis(20)));
        stdin.releaseWrite();

        awaitBytesContaining(stdin, "host.close");
        assertTrue(channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));
        List<CanvasProcessFrame> frames = readHostFrames(stdin.toByteArray());
        assertEquals(2, frames.size());
        assertInstanceOf(CanvasHostHello.class, lifecycle(frames.get(0)));
        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class, lifecycle(frames.get(1)));

        runner.write(
                handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(runnerHello(
                        sessionId, ALL_CAPABILITIES))));
        assertFalse(listener.ready.await(100, TimeUnit.MILLISECONDS));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST).closing(),
                control(new CanvasWireCodec().encode(
                        new CanvasRunnerClosed(sessionId, 1, close.sequence()))));

        assertTrue(listener.closed.await(2, TimeUnit.SECONDS));
        assertFalse(listener.failed.await(100, TimeUnit.MILLISECONDS));
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
        assertFalse(channel.awaitCloseRequestWritten(Duration.ofMillis(20)));
        assertFalse(stdin.closed.await(100, TimeUnit.MILLISECONDS));

        stdin.releaseWrite();
        awaitBytesContaining(stdin, "host.close");
        assertTrue(channel.awaitCloseRequestWritten(Duration.ofSeconds(2)));
        List<CanvasProcessFrame> frames = readHostFrames(stdin.toByteArray());
        CanvasHostClose close = assertInstanceOf(
                CanvasHostClose.class, lifecycle(frames.getLast()));
        runner.write(
                negotiatedPolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                control(new CanvasWireCodec().encode(
                        new CanvasRunnerClosed(sessionId, 1, close.sequence()))));
        assertTrue(listener.closed.await(2, TimeUnit.SECONDS));
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
                        "material_light_default_v1",
                        0xFF6750A4,
                        CanvasThemeBrightness.LIGHT,
                        "A".repeat(64)),
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
                        + ",\"layoutSequence\":" + layout
                        + ",\"physicalWidth\":1600"
                        + ",\"physicalHeight\":900"
                        + ",\"devicePixelRatioMicros\":1500000");
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

    private static String interaction(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long intent,
            long interactionFenceSequence) {
        return runtimeEnvelope(
                revision,
                "runner.interaction",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout
                        + ",\"intentSequence\":" + intent
                        + ",\"interactionFenceSequence\":"
                        + interactionFenceSequence);
    }

    private static String interactionFenceApplied(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long interactionFenceSequence) {
        return runtimeEnvelope(
                revision,
                "runner.interactionFenceApplied",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout
                        + ",\"interactionFenceSequence\":"
                        + interactionFenceSequence);
    }

    private static String paletteDrop(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long intent,
            String token,
            StableId parentWidgetId,
            int insertionIndex) {
        return runtimeEnvelope(
                revision,
                "runner.paletteDrop",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout
                        + ",\"intentSequence\":" + intent
                        + ",\"token\":\"" + token + "\""
                        + ",\"operation\":\"ADD\""
                        + ",\"parentWidgetId\":\"" + parentWidgetId + "\""
                        + ",\"slotName\":\"children\""
                        + ",\"insertionIndex\":" + insertionIndex);
    }

    private static String deleteSelection(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long intent,
            StableId widgetId) {
        return runtimeEnvelope(
                revision,
                "runner.deleteSelection",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout
                        + ",\"intentSequence\":" + intent
                + ",\"widgetId\":\"" + widgetId + "\"");
    }

    private static String textEditCommit(
            CanvasRevisionKey revision,
            long frame,
            long layout,
            long intent,
            long interactionFenceSequence,
            StableId widgetId,
            String text,
            boolean compositionObserved) {
        return runtimeEnvelope(
                revision,
                "runner.textEditCommit",
                "\"frameSequence\":" + frame
                        + ",\"layoutSequence\":" + layout
                        + ",\"intentSequence\":" + intent
                        + ",\"interactionFenceSequence\":"
                        + interactionFenceSequence
                        + ",\"widgetId\":\"" + widgetId + "\""
                        + ",\"text\":\"" + text + "\""
                        + ",\"compositionObserved\":"
                        + compositionObserved);
    }

    private static String viewportMetrics(
            CanvasRevisionKey revision,
            long commandSequence,
            CanvasViewportPresentation presentation,
            int effectiveScaleMicros,
            boolean horizontalScrollable,
            boolean verticalScrollable) {
        String mode = presentation.mode() == CanvasZoomMode.FIT
                ? "fit"
                : "manual";
        return runtimeEnvelope(
                revision,
                "runner.viewport",
                "\"commandSequence\":" + commandSequence
                        + ",\"mode\":\"" + mode + "\""
                        + ",\"zoomMicros\":" + presentation.zoomMicros()
                        + ",\"horizontalScrollMicros\":"
                        + presentation.horizontalScrollMicros()
                        + ",\"verticalScrollMicros\":"
                        + presentation.verticalScrollMicros()
                        + ",\"effectiveScaleMicros\":"
                        + effectiveScaleMicros
                        + ",\"horizontalScrollable\":"
                        + horizontalScrollable
                        + ",\"verticalScrollable\":"
                        + verticalScrollable);
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
        private final CountDownLatch interaction = new CountDownLatch(1);
        private final CountDownLatch interactionFenceApplied =
                new CountDownLatch(1);
        private final CountDownLatch paletteDrop = new CountDownLatch(1);
        private final CountDownLatch deleteSelection = new CountDownLatch(1);
        private final CountDownLatch textEditCommit = new CountDownLatch(1);
        private final CountDownLatch viewportMetrics = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final CountDownLatch warning = new CountDownLatch(1);
        private final CountDownLatch failed = new CountDownLatch(1);
        private final AtomicInteger paletteDropCalls = new AtomicInteger();
        private final AtomicInteger interactionCalls = new AtomicInteger();
        private final AtomicInteger deleteSelectionCalls = new AtomicInteger();
        private final AtomicInteger textEditCommitCalls = new AtomicInteger();
        private final AtomicInteger viewportMetricsCalls = new AtomicInteger();
        private volatile CanvasEngineIdentity engine;
        private volatile CanvasLayoutKey layout;
        private volatile CanvasRunnerRuntimeEvent.Presented presentedEvent;
        private volatile CanvasIntentKey intent;
        private volatile CanvasRunnerRuntimeEvent.Interaction pointerInteraction;
        private volatile CanvasRunnerRuntimeEvent.InteractionFenceApplied
                appliedFence;
        private volatile StableId widgetId;
        private volatile CanvasRunnerRuntimeEvent.PaletteDrop drop;
        private volatile CanvasRunnerRuntimeEvent.DeleteSelection deletion;
        private volatile CanvasRunnerRuntimeEvent.TextEditCommit inlineTextEdit;
        private volatile CanvasViewportMetrics metrics;
        private volatile String failureReason;

        @Override
        public void ready(CanvasEngineIdentity value) {
            engine = value;
            ready.countDown();
        }

        @Override
        public void presented(CanvasRunnerRuntimeEvent.Presented value) {
            presentedEvent = value;
            layout = value.layoutKey();
            presented.countDown();
        }

        @Override
        public void selection(CanvasIntentKey value, StableId selectedWidgetId) {
            intent = value;
            widgetId = selectedWidgetId;
            selection.countDown();
        }

        @Override
        public void interaction(CanvasRunnerRuntimeEvent.Interaction value) {
            pointerInteraction = value;
            interactionCalls.incrementAndGet();
            interaction.countDown();
        }

        @Override
        public void interactionFenceApplied(
                CanvasRunnerRuntimeEvent.InteractionFenceApplied value) {
            appliedFence = value;
            interactionFenceApplied.countDown();
        }

        @Override
        public void paletteDrop(CanvasRunnerRuntimeEvent.PaletteDrop value) {
            drop = value;
            paletteDropCalls.incrementAndGet();
            paletteDrop.countDown();
        }

        @Override
        public void deleteSelection(
                CanvasRunnerRuntimeEvent.DeleteSelection value) {
            deletion = value;
            deleteSelectionCalls.incrementAndGet();
            deleteSelection.countDown();
        }

        @Override
        public void textEditCommit(
                CanvasRunnerRuntimeEvent.TextEditCommit value) {
            inlineTextEdit = value;
            textEditCommitCalls.incrementAndGet();
            textEditCommit.countDown();
        }

        @Override
        public void viewportMetrics(CanvasViewportMetrics value) {
            metrics = value;
            viewportMetricsCalls.incrementAndGet();
            viewportMetrics.countDown();
        }

        @Override
        public void closed() {
            closed.countDown();
        }

        @Override
        public void warning(String reason) {
            warning.countDown();
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
