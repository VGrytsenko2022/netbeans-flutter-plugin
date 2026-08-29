package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.CanvasZoomMode;
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
import dev.flutter.netbeans.designer.command.WidgetPlacement;
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
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FlutterDesignerNativeCanvasSessionProtocolTest {
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework-revision", "engine-revision", "3.10.7");
    private static final List<CanvasWireCapability> CAPABILITIES = List.of(
            CanvasWireCapability.READ_ONLY_RENDER,
            CanvasWireCapability.READ_ONLY_LAYOUT,
            CanvasWireCapability.READ_ONLY_SELECTION,
            CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1,
            CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1,
            CanvasWireCapability.DELETE_SELECTED_WIDGET_V1,
            CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1);
    private static final List<CanvasWireCapability> VIEWPORT_CAPABILITIES = List.of(
            CanvasWireCapability.READ_ONLY_RENDER,
            CanvasWireCapability.READ_ONLY_LAYOUT,
            CanvasWireCapability.READ_ONLY_SELECTION,
            CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1,
            CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1,
            CanvasWireCapability.DELETE_SELECTED_WIDGET_V1,
            CanvasWireCapability.VIEWPORT_PRESENTATION_V1);
    private static final StableId DOCUMENT_A = StableId.parse(
            "83ed3c05-88e7-4220-8377-29fa1f21a99e");
    private static final StableId DOCUMENT_B = StableId.parse(
            "32e43896-5be1-46ad-9e29-b9bf976da690");
    private static final StableId ROOT = StableId.parse(
            "5b814fc1-ecc1-4255-898d-3111f10673a4");
    private static final StableId CHILD = StableId.parse(
            "9dd9e5e0-5364-4dcc-bd82-1a86345cd522");
    private static final WidgetTypeId TEXT = new WidgetTypeId(
            "flutter.widgets.Text");
    private static final WidgetTypeId PADDING = new WidgetTypeId(
            "flutter.widgets.Padding");
    private static final String DROP_TOKEN_A =
            "nbfdnd:v1:4f9719e3-fda7-46f4-8d75-57cc1d3bbc1b:"
            + "22d5a643-f6a2-4f56-bcef-b244c7c4c5a7";
    private static final String DROP_TOKEN_B =
            "nbfdnd:v1:4f9719e3-fda7-46f4-8d75-57cc1d3bbc1b:"
            + "1f985eaa-20e9-4ce5-aceb-bd1098ad45f8";
    private static final String DROP_TOKEN_C =
            "nbfdnd:v1:4f9719e3-fda7-46f4-8d75-57cc1d3bbc1b:"
            + "6dc63d5c-f5d3-439c-ad22-e0c819e5568b";

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
    void publishesWebAsTheExactNativeResponsiveLayoutProfile()
            throws Exception {
        DesignerDocument document = document(DOCUMENT_A, ROOT, null);
        Harness harness = Harness.start(document);
        try {
            harness.nextRender();
            onEdt(() -> harness.session.present(
                    document,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.WEB,
                    CanvasTargetPlatform.WEB));
            RenderPublication web = harness.nextRender();

            JsonNode profile = new ObjectMapper().readTree(web.model()).path("profile");
            assertEquals("web", profile.path("previewMode").asText());
            assertEquals("web", profile.path("targetPlatform").asText());
            assertEquals(1_440.0d, profile.path("logicalWidth").asDouble());
            assertEquals(900.0d, profile.path("logicalHeight").asDouble());
        } finally {
            harness.close();
        }
    }

    @Test
    void staleViewportMetricsCannotOverwriteTheCurrentCommandAndLocalMetricsFollowItsAck()
            throws Exception {
        Harness harness = Harness.startViewport(document(DOCUMENT_A, ROOT, null));
        try {
            RenderPublication publication = harness.nextRender();
            HostViewport initial = harness.process.readViewport();
            assertEquals(publication.revision(), initial.revision());
            assertEquals(1, initial.commandSequence());
            assertEquals(CanvasViewportPresentation.fit(), initial.presentation());

            harness.process.sendViewportMetrics(
                    initial, initial.presentation(), 750_000, false, false);
            harness.awaitViewportMetrics(1);

            CanvasViewportPresentation currentPresentation =
                    CanvasViewportPresentation.manual(
                            1_250_000, 300_000, 400_000);
            onEdt(() -> harness.session.setViewportPresentation(
                    currentPresentation));
            HostViewport current = harness.process.readViewport();
            assertEquals(2, current.commandSequence());
            assertEquals(currentPresentation, current.presentation());

            harness.ui.hold();
            harness.process.sendViewportMetrics(
                    initial, initial.presentation(), 750_000, false, false);
            harness.process.sendViewportMetrics(
                    current, current.presentation(), 1_250_000, true, true);
            harness.awaitUiTasks(2);
            onEdt(harness.ui::releaseAll);

            assertEquals(2, harness.runnerViewportMetrics.size());
            CanvasViewportMetrics accepted = harness.runnerViewportMetrics.getLast();
            assertEquals(2, accepted.commandSequence());
            assertEquals(currentPresentation, accepted.presentation());

            CanvasViewportPresentation runnerLocalPresentation =
                    CanvasViewportPresentation.manual(
                            1_250_000, 700_000, 800_000);
            harness.process.sendViewportMetrics(
                    current,
                    runnerLocalPresentation,
                    1_250_000,
                    true,
                    true);
            harness.awaitViewportMetrics(3);
            assertEquals(runnerLocalPresentation,
                    harness.runnerViewportMetrics.getLast().presentation());
        } finally {
            harness.close();
        }
    }

    @Test
    void rejectedNewViewportDoesNotLeaveTheAcknowledgedPreviousCommandPending()
            throws Exception {
        Harness harness = Harness.startViewport(document(DOCUMENT_A, ROOT, CHILD));
        try {
            RenderPublication publication = harness.nextRender();
            HostViewport acceptedA = harness.process.readViewport();
            CanvasLayoutKey layout = layout(publication.revision());
            harness.process.sendPresented(layout);
            harness.process.readSelection();

            harness.process.blockNextHostWrite();
            onEdt(() -> harness.session.selectWidget(CHILD));
            harness.process.awaitHostWriteBlocked();
            onEdt(() -> {
                for (int index = 0; index < 64; index++) {
                    harness.session.selectWidget((index & 1) == 0 ? ROOT : CHILD);
                }
                harness.session.setViewportPresentation(
                        CanvasViewportPresentation.manual(
                                1_500_000, 250_000, 350_000));
                harness.stopViewportRetryTimer();
            });

            harness.ui.hold();
            harness.process.sendViewportMetrics(
                    acceptedA,
                    acceptedA.presentation(),
                    750_000,
                    false,
                    false);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerViewportMetrics.isEmpty());

            onEdt(() -> harness.session.setViewportPresentation(
                    acceptedA.presentation()));
            CanvasViewportPresentation runnerLocalPresentation =
                    CanvasViewportPresentation.manual(
                            1_000_000, 600_000, 700_000);
            harness.process.sendViewportMetrics(
                    acceptedA,
                    runnerLocalPresentation,
                    1_000_000,
                    true,
                    true);
            harness.awaitViewportMetrics(1);
            assertEquals(runnerLocalPresentation,
                    harness.runnerViewportMetrics.getFirst().presentation());
        } finally {
            harness.process.releaseHostWrite();
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

    @Test
    void exactCurrentVisiblePaletteDropIsDeliveredOnceAndReplayIsRejected()
            throws Exception {
        Harness harness = Harness.start(
                document(DOCUMENT_A, ROOT, CHILD),
                ignored -> Optional.of(PADDING));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.process.sendPaletteDrop(
                    currentLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.awaitUiTasks(2);
            onEdt(harness.ui::releaseAll);

            assertEquals(1, harness.runnerPaletteDrops.size());
            CanvasRunnerRuntimeEvent.PaletteDrop delivered =
                    harness.runnerPaletteDrops.get(0);
            assertEquals(currentLayout, delivered.intentKey().layoutKey());
            assertEquals(0, delivered.intentKey().intentId().intentSequence());
            assertEquals(DROP_TOKEN_A, delivered.token());
            assertEquals(ROOT, delivered.parentWidgetId());
            assertEquals(new SlotName("children"), delivered.slotName());
            assertEquals(0, delivered.insertionIndex());
            assertEquals(List.of(PADDING), harness.runnerPaletteDropTypes);
        } finally {
            harness.close();
        }
    }

    @Test
    void exactCurrentPaletteSourceProjectsCanonicalTypeAndTraits()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            WidgetTypeId appBar = new WidgetTypeId("flutter.material.AppBar");

            assertTrue(onEdt(() -> harness.session.authorizePaletteDragSource(
                    DROP_TOKEN_A, appBar)));
            JsonNode body = harness.process.readHostControl(
                    "host.paletteDragSource");

            assertEquals(currentLayout, harness.process.layout(body));
            assertEquals(DROP_TOKEN_A, body.path("token").asText());
            assertEquals(appBar.value(), body.path("widgetType").asText());
            assertEquals(
                    List.of(BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT),
                    List.of(body.path("traits").get(0).asText()));
            assertEquals(1, body.path("traits").size());

            assertFalse(onEdt(() -> harness.session.authorizePaletteDragSource(
                    DROP_TOKEN_B,
                    new WidgetTypeId("flutter.material.ElevatedButton"))));
        } finally {
            harness.close();
        }
    }

    @Test
    void stalePaletteDropRevisionAndLayoutAreConsumedWithoutDelivery()
            throws Exception {
        DesignerDocument document = document(DOCUMENT_A, ROOT, CHILD);
        Harness harness = Harness.start(document);
        try {
            CanvasLayoutKey staleRevisionLayout = renderAndPresent(harness);
            onEdt(() -> harness.session.present(
                    document,
                    BuiltInWidgetCatalog.getDefault(),
                    CanvasPreviewMode.DESKTOP,
                    CanvasTargetPlatform.WINDOWS));
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            CanvasLayoutKey staleLayout = new CanvasLayoutKey(
                    currentLayout.frameKey(), currentLayout.layoutSequence() + 1);

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    staleRevisionLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.process.sendPaletteDrop(
                    staleLayout, 1, DROP_TOKEN_B, ROOT, 1);
            harness.awaitUiTasks(2);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerPaletteDrops.isEmpty());

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 2, DROP_TOKEN_C, ROOT, 2);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertEquals(
                    List.of(DROP_TOKEN_C),
                    harness.runnerPaletteDrops.stream()
                            .map(CanvasRunnerRuntimeEvent.PaletteDrop::token)
                            .toList());
        } finally {
            harness.close();
        }
    }

    @Test
    void firstSameSessionDropAttemptBurnsItsOpaquePaletteToken()
            throws Exception {
        Set<String> liveTokens = new HashSet<>(Set.of(DROP_TOKEN_A, DROP_TOKEN_B));
        Harness harness = Harness.start(
                document(DOCUMENT_A, ROOT, CHILD),
                token -> liveTokens.remove(token)
                        ? Optional.of(TEXT) : Optional.empty());
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            CanvasLayoutKey staleLayout = new CanvasLayoutKey(
                    currentLayout.frameKey(), currentLayout.layoutSequence() + 1);

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    staleLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerPaletteDrops.isEmpty());
            assertEquals(Set.of(DROP_TOKEN_B), liveTokens);

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 1, DROP_TOKEN_A, ROOT, 0);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerPaletteDrops.isEmpty());

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 2, DROP_TOKEN_B, ROOT, 0);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertEquals(
                    List.of(DROP_TOKEN_B),
                    harness.runnerPaletteDrops.stream()
                            .map(CanvasRunnerRuntimeEvent.PaletteDrop::token)
                            .toList());
        } finally {
            harness.close();
        }
    }

    @Test
    void newerExactLayoutInvalidatesDropsBoundToThePreviousLayout()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey firstLayout = renderAndPresent(harness);
            CanvasLayoutKey resizedLayout = new CanvasLayoutKey(
                    firstLayout.frameKey(), firstLayout.layoutSequence() + 1);
            harness.ui.hold();
            harness.process.sendPresented(resizedLayout);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    firstLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.process.sendPaletteDrop(
                    resizedLayout, 1, DROP_TOKEN_B, ROOT, 0);
            harness.awaitUiTasks(2);
            onEdt(harness.ui::releaseAll);

            assertEquals(
                    List.of(DROP_TOKEN_B),
                    harness.runnerPaletteDrops.stream()
                            .map(CanvasRunnerRuntimeEvent.PaletteDrop::token)
                            .toList());
        } finally {
            harness.close();
        }
    }

    @Test
    void hiddenPaletteDropIsConsumedWithoutDelivery()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            onEdt(harness.session::hide);

            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerPaletteDrops.isEmpty());

            onEdt(harness.session::show);
            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 1, DROP_TOKEN_B, ROOT, 0);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertEquals(
                    List.of(DROP_TOKEN_B),
                    harness.runnerPaletteDrops.stream()
                            .map(CanvasRunnerRuntimeEvent.PaletteDrop::token)
                            .toList());
        } finally {
            harness.close();
        }
    }

    @Test
    void queuedPaletteDropIsNotDeliveredAfterPresentationIsWithdrawn()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.awaitUiTasks(1);

            onEdt(harness.session::withdraw);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerPaletteDrops.isEmpty());
        } finally {
            harness.close();
        }
    }

    @Test
    void queuedPaletteDropIsNotDeliveredAfterSessionIsClosed()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            harness.ui.hold();
            harness.process.sendPaletteDrop(
                    currentLayout, 0, DROP_TOKEN_A, ROOT, 0);
            harness.awaitUiTasks(1);

            onEdt(harness.session::close);
            onEdt(harness.ui::releaseAll);
            assertTrue(harness.runnerPaletteDrops.isEmpty());
        } finally {
            harness.close();
        }
    }

    @Test
    void deleteIntentRequiresExactCurrentSelectionLayoutVisibilityAndOneShot()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            CanvasLayoutKey staleLayout = new CanvasLayoutKey(
                    currentLayout.frameKey(), currentLayout.layoutSequence() + 1);

            harness.ui.hold();
            harness.process.sendDeleteSelection(currentLayout, 0, CHILD);
            harness.process.sendSelection(currentLayout, 1, CHILD);
            harness.process.sendDeleteSelection(staleLayout, 2, CHILD);
            harness.process.sendDeleteSelection(currentLayout, 3, ROOT);
            harness.process.sendDeleteSelection(currentLayout, 4, CHILD);
            harness.process.sendDeleteSelection(currentLayout, 4, CHILD);
            harness.awaitUiTasks(6);
            onEdt(harness.ui::releaseAll);

            assertEquals(List.of(CHILD), harness.runnerSelections);
            assertEquals(
                    List.of(CHILD),
                    harness.runnerDeletions.stream()
                            .map(CanvasRunnerRuntimeEvent.DeleteSelection::widgetId)
                            .toList());

            onEdt(harness.session::hide);
            harness.ui.hold();
            harness.process.sendDeleteSelection(currentLayout, 5, CHILD);
            harness.awaitUiTasks(1);
            onEdt(harness.ui::releaseAll);
            assertEquals(1, harness.runnerDeletions.size());
        } finally {
            harness.close();
        }
    }

    @Test
    void widgetMovePreviewUsesExactLayoutAndClearsExplicitlyAndOnHide()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            WidgetPlacement destination = new WidgetPlacement(
                    ROOT, new SlotName("body"), 0);

            onEdt(() -> harness.session.showWidgetMovePreview(
                    CHILD, destination));
            HostWidgetMovePreview preview =
                    harness.process.readWidgetMovePreview();
            assertEquals(currentLayout, preview.layout());
            assertEquals(1, preview.previewSequence());
            assertEquals(CHILD, preview.sourceWidgetId());
            assertEquals(destination, preview.destination());

            onEdt(harness.session::clearWidgetMovePreview);
            HostWidgetMovePreviewClear explicitClear =
                    harness.process.readWidgetMovePreviewClear();
            assertEquals(currentLayout, explicitClear.layout());
            assertEquals(2, explicitClear.previewSequence());

            onEdt(() -> harness.session.showWidgetMovePreview(
                    CHILD, destination));
            assertEquals(
                    3,
                    harness.process.readWidgetMovePreview().previewSequence());
            onEdt(harness.session::hide);
            HostWidgetMovePreviewClear hideClear =
                    harness.process.readWidgetMovePreviewClear();
            assertEquals(currentLayout, hideClear.layout());
            assertEquals(4, hideClear.previewSequence());
        } finally {
            harness.close();
        }
    }

    @Test
    void widgetMovePreviewRetryIsLastWriteWinsAndClearSupersedesPendingShow()
            throws Exception {
        Harness harness = Harness.start(document(DOCUMENT_A, ROOT, CHILD));
        try {
            CanvasLayoutKey currentLayout = renderAndPresent(harness);
            WidgetPlacement placementA = new WidgetPlacement(
                    ROOT, new SlotName("body"), 0);
            WidgetPlacement placementB = new WidgetPlacement(
                    ROOT, new SlotName("floatingActionButton"), 0);
            WidgetPlacement placementC = new WidgetPlacement(
                    ROOT, new SlotName("appBar"), 0);
            onEdt(() -> harness.session.showWidgetMovePreview(
                    CHILD, placementA));
            HostWidgetMovePreview acceptedA =
                    harness.process.readWidgetMovePreview();
            assertEquals(1, acceptedA.previewSequence());
            assertEquals(placementA, acceptedA.destination());

            harness.process.blockNextHostWrite();
            onEdt(() -> harness.session.selectWidget(CHILD));
            harness.process.awaitHostWriteBlocked();
            onEdt(() -> {
                for (int index = 0; index < 64; index++) {
                    harness.session.selectWidget(
                            (index & 1) == 0 ? ROOT : CHILD);
                }
                harness.session.showWidgetMovePreview(CHILD, placementB);
                harness.session.showWidgetMovePreview(CHILD, placementC);
                harness.stopWidgetMovePreviewRetryTimer();
            });
            harness.process.releaseHostWrite();
            for (int index = 0; index < 65; index++) {
                harness.process.readSelection();
            }
            onEdt(harness::retryWidgetMovePreviewNow);
            HostWidgetMovePreview retriedLatest =
                    harness.process.readWidgetMovePreview();
            assertEquals(currentLayout, retriedLatest.layout());
            assertEquals(2, retriedLatest.previewSequence());
            assertEquals(placementC, retriedLatest.destination());

            harness.process.blockNextHostWrite();
            onEdt(() -> harness.session.selectWidget(ROOT));
            harness.process.awaitHostWriteBlocked();
            onEdt(() -> {
                for (int index = 0; index < 64; index++) {
                    harness.session.selectWidget(
                            (index & 1) == 0 ? CHILD : ROOT);
                }
                harness.session.showWidgetMovePreview(CHILD, placementB);
                harness.session.clearWidgetMovePreview();
                harness.stopWidgetMovePreviewRetryTimer();
            });
            harness.process.releaseHostWrite();
            for (int index = 0; index < 65; index++) {
                harness.process.readSelection();
            }
            onEdt(harness::retryWidgetMovePreviewNow);
            HostWidgetMovePreviewClear retriedClear =
                    harness.process.readWidgetMovePreviewClear();
            assertEquals(currentLayout, retriedClear.layout());
            assertEquals(3, retriedClear.previewSequence());
        } finally {
            harness.process.releaseHostWrite();
            harness.close();
        }
    }

    private static CanvasLayoutKey renderAndPresent(Harness harness)
            throws Exception {
        RenderPublication publication = harness.nextRender();
        CanvasLayoutKey layout = layout(publication.revision());
        harness.process.sendPresented(layout);
        HostSelection selection = harness.process.readSelection();
        assertEquals(layout, selection.layout());
        return layout;
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
        private final ControllableUiExecutor ui = new ControllableUiExecutor();
        private final List<StableId> runnerSelections = new ArrayList<>();
        private final List<CanvasRunnerRuntimeEvent.PaletteDrop> runnerPaletteDrops =
                new ArrayList<>();
        private final List<WidgetTypeId> runnerPaletteDropTypes = new ArrayList<>();
        private final List<CanvasRunnerRuntimeEvent.DeleteSelection>
                runnerDeletions = new ArrayList<>();
        private final List<CanvasViewportMetrics> runnerViewportMetrics =
                new ArrayList<>();
        private final CanvasRunnerBuildResult runner;
        private final FlutterDesignerNativeCanvasSession session;

        private Harness() {
            this(ignored -> Optional.of(TEXT));
        }

        private Harness(
                Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver) {
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
                            ui,
                            polls,
                            Process::destroy);
            session = new FlutterDesignerNativeCanvasSession(
                    host,
                    runtime,
                    ignored -> { },
                    runnerSelections::add,
                    paletteDropTokenResolver,
                    admitted -> {
                        runnerPaletteDropTypes.add(admitted.widgetType());
                        runnerPaletteDrops.add(admitted.drop());
                    },
                    runnerDeletions::add);
            session.setViewportMetricsListener(runnerViewportMetrics::add);
        }

        static Harness start(DesignerDocument document) throws Exception {
            Harness harness = onEdt((Callable<Harness>) Harness::new);
            return start(harness, document, CAPABILITIES);
        }

        static Harness startViewport(DesignerDocument document) throws Exception {
            Harness harness = onEdt((Callable<Harness>) Harness::new);
            return start(harness, document, VIEWPORT_CAPABILITIES);
        }

        static Harness start(
                DesignerDocument document,
                Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver)
                throws Exception {
            Harness harness = onEdt((Callable<Harness>)
                    () -> new Harness(paletteDropTokenResolver));
            return start(harness, document, CAPABILITIES);
        }

        private static Harness start(
                Harness harness,
                DesignerDocument document,
                List<CanvasWireCapability> capabilities)
                throws Exception {
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
            harness.process.handshake(capabilities);
            return harness;
        }

        RenderPublication nextRender() throws Exception {
            awaitEdtCondition(
                    () -> launches.pendingCount() > 0,
                    "model encoding was not scheduled after protocol readiness");
            onEdt(launches::runNext);
            return process.readRender();
        }

        void awaitUiTasks(int expectedMinimum) throws Exception {
            awaitEdtCondition(
                    () -> ui.pendingCount() >= expectedMinimum,
                    "runtime event was not queued for UI delivery");
        }

        void awaitViewportMetrics(int expectedMinimum) throws Exception {
            awaitEdtCondition(
                    () -> runnerViewportMetrics.size() >= expectedMinimum,
                    "viewport metrics were not delivered to the session listener");
        }

        void stopViewportRetryTimer() {
            try {
                var field = FlutterDesignerNativeCanvasSession.class
                        .getDeclaredField("viewportCommandRetryTimer");
                field.setAccessible(true);
                ((javax.swing.Timer) field.get(session)).stop();
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
        }

        void stopWidgetMovePreviewRetryTimer() {
            try {
                var field = FlutterDesignerNativeCanvasSession.class
                        .getDeclaredField("widgetMovePreviewRetryTimer");
                field.setAccessible(true);
                ((javax.swing.Timer) field.get(session)).stop();
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
        }

        void retryWidgetMovePreviewNow() {
            try {
                var method = FlutterDesignerNativeCanvasSession.class
                        .getDeclaredMethod("retryWidgetMovePreview");
                method.setAccessible(true);
                method.invoke(session);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
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

    private static final class ControllableUiExecutor implements Executor {
        private final Deque<Runnable> pending = new ArrayDeque<>();
        private boolean held;

        @Override
        public void execute(Runnable command) {
            synchronized (this) {
                if (held) {
                    pending.addLast(command);
                    return;
                }
            }
            dispatchUi(command);
        }

        synchronized void hold() {
            if (held || !pending.isEmpty()) {
                throw new AssertionError("UI executor is already held or has pending work");
            }
            held = true;
        }

        int pendingCount() {
            synchronized (this) {
                return pending.size();
            }
        }

        void releaseAll() {
            List<Runnable> tasks;
            synchronized (this) {
                if (!held) {
                    throw new AssertionError("UI executor is not held");
                }
                held = false;
                tasks = new ArrayList<>(pending);
                pending.clear();
            }
            tasks.forEach(Runnable::run);
        }
    }

    private record RenderPublication(CanvasRevisionKey revision, byte[] model) {
        private RenderPublication {
            model = model.clone();
        }
    }

    private record HostSelection(CanvasLayoutKey layout, StableId widgetId) {
    }

    private record HostViewport(
            CanvasRevisionKey revision,
            long commandSequence,
            CanvasViewportPresentation presentation) {
    }

    private record HostWidgetMovePreview(
            CanvasLayoutKey layout,
            long previewSequence,
            StableId sourceWidgetId,
            WidgetPlacement destination) {
    }

    private record HostWidgetMovePreviewClear(
            CanvasLayoutKey layout,
            long previewSequence) {
    }

    private static final class GateOutputStream extends OutputStream {
        private final OutputStream delegate;
        private CountDownLatch blocked = new CountDownLatch(0);
        private CountDownLatch release = new CountDownLatch(0);
        private boolean blockNextWrite;

        private GateOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        synchronized void blockNextWrite() {
            if (blockNextWrite || blocked.getCount() != 0) {
                throw new AssertionError("host write gate is already armed");
            }
            blocked = new CountDownLatch(1);
            release = new CountDownLatch(1);
            blockNextWrite = true;
        }

        boolean awaitWriteBlocked(long timeout, TimeUnit unit)
                throws InterruptedException {
            return blocked.await(timeout, unit);
        }

        void releaseWrite() {
            release.countDown();
        }

        @Override
        public void write(int value) throws IOException {
            awaitGate();
            delegate.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length)
                throws IOException {
            awaitGate();
            delegate.write(bytes, offset, length);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            releaseWrite();
            delegate.close();
        }

        private void awaitGate() throws IOException {
            CountDownLatch activeRelease;
            synchronized (this) {
                if (!blockNextWrite) {
                    return;
                }
                blockNextWrite = false;
                activeRelease = release;
                blocked.countDown();
            }
            try {
                activeRelease.await();
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IOException("host write gate interrupted", failure);
            }
        }
    }

    private static final class ProtocolProcess extends Process {
        private final long pid;
        private final java.io.PipedInputStream hostStdout;
        private final java.io.PipedOutputStream runnerStdout;
        private final java.io.PipedInputStream runnerStdin;
        private final java.io.PipedOutputStream hostStdin;
        private final GateOutputStream gatedHostStdin;
        private final CanvasProcessFrameReader hostFrames;
        private final CanvasProcessFrameWriter runnerFrames;
        private final CompletableFuture<Process> onExit = new CompletableFuture<>();
        private final ObjectMapper json = new ObjectMapper();
        private CanvasSessionId sessionId;
        private List<CanvasWireCapability> negotiatedCapabilities = CAPABILITIES;
        private boolean alive = true;
        private int exitCode;

        private ProtocolProcess(long pid) {
            this.pid = pid;
            try {
                hostStdout = new java.io.PipedInputStream(1024 * 1024);
                runnerStdout = new java.io.PipedOutputStream(hostStdout);
                runnerStdin = new java.io.PipedInputStream(1024 * 1024);
                hostStdin = new java.io.PipedOutputStream(runnerStdin);
                gatedHostStdin = new GateOutputStream(hostStdin);
            } catch (IOException failure) {
                throw new AssertionError(failure);
            }
            CanvasProcessFrameCodec codec = new CanvasProcessFrameCodec();
            hostFrames = codec.reader(runnerStdin);
            runnerFrames = codec.writer(runnerStdout);
        }

        void handshake(List<CanvasWireCapability> capabilities) throws Exception {
            CanvasProcessFrame helloFrame = hostFrames.read(handshakePolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER)).orElseThrow();
            CanvasWireDecodeResult decoded = new CanvasWireCodec().decode(
                    helloFrame.copyPayload());
            CanvasHostHello hello = (CanvasHostHello)
                    ((CanvasWireDecodeResult.Decoded) decoded).message();
            sessionId = hello.sessionId();
            negotiatedCapabilities = List.copyOf(capabilities);
            runnerFrames.write(
                    handshakePolicy(CanvasProcessDirection.RUNNER_TO_HOST),
                    control(new CanvasWireCodec().encode(new CanvasRunnerHello(
                            sessionId,
                            0,
                            0,
                            "flutter-canvas-runner/1",
                            ENGINE,
                            negotiatedCapabilities,
                            CanvasWireHandshakeLimits.defaults()))));
        }

        RenderPublication readRender() throws Exception {
            CanvasProcessFrame control = hostFrames.read(negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER,
                    negotiatedCapabilities)).orElseThrow();
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
                    negotiatedPolicy(
                            CanvasProcessDirection.HOST_TO_RUNNER,
                            negotiatedCapabilities),
                    descriptor).orElseThrow();
            return new RenderPublication(revision, payload.copyPayload());
        }

        HostSelection readSelection() throws Exception {
            CanvasProcessFrame frame = hostFrames.read(negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER,
                    negotiatedCapabilities)).orElseThrow();
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

        HostWidgetMovePreview readWidgetMovePreview() throws Exception {
            JsonNode body = readHostControl("host.widgetMovePreview");
            return new HostWidgetMovePreview(
                    layout(body),
                    body.path("previewSequence").longValue(),
                    StableId.parse(body.path("sourceWidgetId").asText()),
                    new WidgetPlacement(
                            StableId.parse(body.path("parentWidgetId").asText()),
                            new SlotName(body.path("slotName").asText()),
                            body.path("insertionIndex").intValue()));
        }

        HostWidgetMovePreviewClear readWidgetMovePreviewClear()
                throws Exception {
            JsonNode body = readHostControl("host.widgetMovePreviewClear");
            return new HostWidgetMovePreviewClear(
                    layout(body), body.path("previewSequence").longValue());
        }

        private JsonNode readHostControl(String expectedType) throws Exception {
            CanvasProcessFrame frame = hostFrames.read(negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER,
                    negotiatedCapabilities)).orElseThrow();
            JsonNode root = json.readTree(frame.copyPayload());
            assertEquals(expectedType, root.path("type").asText());
            return root.path("body");
        }

        private CanvasLayoutKey layout(JsonNode body) {
            return new CanvasLayoutKey(
                    new CanvasFrameKey(
                            revision(body),
                            body.path("frameSequence").longValue()),
                    body.path("layoutSequence").longValue());
        }

        HostViewport readViewport() throws Exception {
            CanvasProcessFrame frame = hostFrames.read(negotiatedPolicy(
                    CanvasProcessDirection.HOST_TO_RUNNER,
                    negotiatedCapabilities)).orElseThrow();
            JsonNode root = json.readTree(frame.copyPayload());
            assertEquals("host.viewport", root.path("type").asText());
            JsonNode body = root.path("body");
            String mode = body.path("mode").asText();
            CanvasViewportPresentation presentation =
                    new CanvasViewportPresentation(
                            "fit".equals(mode)
                                    ? CanvasZoomMode.FIT
                                    : CanvasZoomMode.MANUAL,
                            body.path("zoomMicros").intValue(),
                            body.path("horizontalScrollMicros").intValue(),
                            body.path("verticalScrollMicros").intValue());
            return new HostViewport(
                    revision(body),
                    body.path("commandSequence").longValue(),
                    presentation);
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

        void sendPaletteDrop(
                CanvasLayoutKey layout,
                long intentSequence,
                String token,
                StableId parentWidgetId,
                int insertionIndex) throws Exception {
            sendRuntime(runtimeEnvelope(
                    layout.frameKey().revisionKey(),
                    "runner.paletteDrop",
                    "\"frameSequence\":" + layout.frameKey().frameSequence()
                            + ",\"layoutSequence\":" + layout.layoutSequence()
                            + ",\"intentSequence\":" + intentSequence
                            + ",\"token\":\"" + token + "\""
                            + ",\"operation\":\"ADD\""
                            + ",\"parentWidgetId\":\"" + parentWidgetId + "\""
                            + ",\"slotName\":\"children\""
                            + ",\"insertionIndex\":" + insertionIndex));
        }

        void sendDeleteSelection(
                CanvasLayoutKey layout,
                long intentSequence,
                StableId widgetId) throws Exception {
            sendRuntime(runtimeEnvelope(
                    layout.frameKey().revisionKey(),
                    "runner.deleteSelection",
                    "\"frameSequence\":" + layout.frameKey().frameSequence()
                            + ",\"layoutSequence\":" + layout.layoutSequence()
                            + ",\"intentSequence\":" + intentSequence
                            + ",\"widgetId\":\"" + widgetId + "\""));
        }

        void sendViewportMetrics(
                HostViewport command,
                CanvasViewportPresentation presentation,
                int effectiveScaleMicros,
                boolean horizontalScrollable,
                boolean verticalScrollable) throws Exception {
            String mode = presentation.mode() == CanvasZoomMode.FIT
                    ? "fit"
                    : "manual";
            sendRuntime(runtimeEnvelope(
                    command.revision(),
                    "runner.viewport",
                    "\"commandSequence\":" + command.commandSequence()
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
                            + verticalScrollable));
        }

        void blockNextHostWrite() {
            gatedHostStdin.blockNextWrite();
        }

        void awaitHostWriteBlocked() throws Exception {
            assertTrue(gatedHostStdin.awaitWriteBlocked(
                    2, TimeUnit.SECONDS), "host protocol write did not block");
        }

        void releaseHostWrite() {
            gatedHostStdin.releaseWrite();
        }

        private void sendRuntime(String value) throws Exception {
            runnerFrames.write(
                    negotiatedPolicy(
                            CanvasProcessDirection.RUNNER_TO_HOST,
                            negotiatedCapabilities),
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
            return gatedHostStdin;
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
            CanvasProcessDirection direction,
            List<CanvasWireCapability> capabilities) {
        return CanvasProcessFramingPolicy.negotiated(
                CanvasWireLimits.defaults(),
                new CanvasWireNegotiation(
                        "flutter-canvas-runner/1",
                        ENGINE,
                        capabilities,
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
