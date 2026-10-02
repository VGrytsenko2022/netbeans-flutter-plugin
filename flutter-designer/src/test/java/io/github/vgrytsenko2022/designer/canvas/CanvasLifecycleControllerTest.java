package io.github.vgrytsenko2022.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasBackendFactory;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasBackendOpenRequest;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasBackendReady;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasBackendSession;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasControllerState;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasFailureStage;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasLifecycleController;
import io.github.vgrytsenko2022.designer.canvas.runtime.CanvasPresentationReady;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CanvasLifecycleControllerTest {
    private static final StableId DOCUMENT_ID = StableId.parse(
            "83ed3c05-88e7-4220-8377-29fa1f21a99e");
    private static final StableId ROOT_ID = StableId.parse(
            "5b814fc1-ecc1-4255-898d-3111f10673a4");
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8",
            "framework-revision",
            "engine-revision",
            "3.12.0");

    @Test
    void startsExactlyOneBackendAndAcceptsOnlyTheExactReadyHandshake() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = controller(backends);

        assertInstanceOf(CanvasControllerState.New.class, controller.state());
        controller.open();
        controller.open();

        assertEquals(1, backends.sessions.size());
        FakeBackendSession backend = backends.latest();
        CanvasControllerState.Starting starting = assertInstanceOf(
                CanvasControllerState.Starting.class, controller.state());
        assertFalse(starting.restarting());
        backend.succeedReady(ENGINE);

        CanvasControllerState.Idle idle = assertInstanceOf(
                CanvasControllerState.Idle.class, controller.state());
        assertEquals(backend.request.sessionId(), idle.backend().sessionId());
        assertEquals(0, starting.attemptSequence());
    }

    @Test
    void coalescesEveryRenderBeforeReadyToTheLatestRequest() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = controller(backends);
        controller.open();

        CanvasRenderRequest latest = null;
        for (int revision = 0; revision < 500; revision++) {
            latest = controller.present(profile(ENGINE), snapshot(revision));
        }
        assertTrue(backends.latest().renderRequests.isEmpty());

        backends.latest().succeedReady(ENGINE);

        assertEquals(1, backends.latest().renderRequests.size());
        assertSame(latest, backends.latest().renderRequests.getFirst());
        assertEquals(499, latest.revisionKey().presentationSequence());
        assertEquals(499, latest.revisionKey().logicalRevisionId());
        assertEquals(latest.revisionKey(), assertInstanceOf(
                CanvasControllerState.Rendering.class,
                controller.state()).revisionKey());
    }

    @Test
    void keepsOneRenderInFlightAndOnlyOneLatestFollowUp() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = controller(backends);
        List<CanvasControllerState> publications = new ArrayList<>();
        controller.addListener((previous, current) -> publications.add(current));
        controller.open();
        FakeBackendSession backend = backends.latest();
        backend.succeedReady(ENGINE);

        CanvasRenderRequest first = controller.present(profile(ENGINE), snapshot(0));
        CanvasRenderRequest latest = null;
        for (int revision = 1; revision <= 500; revision++) {
            latest = controller.present(profile(ENGINE), snapshot(revision));
        }
        assertEquals(1, backend.renderRequests.size());

        backend.succeedRender(0, ready(first));

        assertEquals(2, backend.renderRequests.size());
        assertSame(latest, backend.renderRequests.get(1));
        assertFalse(publications.stream().anyMatch(
                CanvasControllerState.Presented.class::isInstance),
                "the superseded first completion must never be published");

        backend.succeedRender(1, ready(latest));
        CanvasControllerState.Presented presented = assertInstanceOf(
                CanvasControllerState.Presented.class, controller.state());
        assertEquals(latest.revisionKey(),
                presented.frameKey().revisionKey());
    }

    @Test
    void requiresAnExactContiguousAtomicFrameAndInitialLayout() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = readyController(backends);
        CanvasRenderRequest request = controller.present(profile(ENGINE), snapshot(0));
        CanvasFrameKey skippedFrame = new CanvasFrameKey(request.revisionKey(), 1);
        CanvasLayoutKey skippedLayout = new CanvasLayoutKey(skippedFrame, 0);

        backends.latest().succeedRender(
                0,
                new CanvasPresentationReady(skippedFrame, skippedLayout));

        CanvasControllerState.Failed failed = assertInstanceOf(
                CanvasControllerState.Failed.class, controller.state());
        assertEquals(CanvasFailureStage.PROTOCOL, failed.stage());
        assertTrue(failed.reason().contains("OUT_OF_ORDER_FRAME"));

        CanvasFrameKey exactFrame = new CanvasFrameKey(request.revisionKey(), 0);
        CanvasFrameKey otherFrame = new CanvasFrameKey(request.revisionKey(), 1);
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasPresentationReady(
                        exactFrame,
                        new CanvasLayoutKey(otherFrame, 0)));
    }

    @Test
    void restartUsesAFreshSessionAndIgnoresLateOldSessionResults() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = readyController(backends);
        FakeBackendSession firstBackend = backends.latest();
        CanvasRenderRequest first = controller.present(profile(ENGINE), snapshot(7));

        assertTrue(controller.restart());
        assertTrue(controller.restart(),
                "a repeated restart during replacement startup is idempotent");
        assertEquals(2, backends.sessions.size());
        FakeBackendSession secondBackend = backends.latest();
        assertNotEquals(firstBackend.request.sessionId(),
                secondBackend.request.sessionId());
        assertEquals(1, assertInstanceOf(
                CanvasControllerState.Starting.class,
                controller.state()).attemptSequence());
        assertEquals(1, firstBackend.closeCount);

        firstBackend.succeedRender(0, ready(first));
        assertInstanceOf(CanvasControllerState.Starting.class, controller.state());

        secondBackend.succeedReady(ENGINE);
        assertEquals(1, secondBackend.renderRequests.size());
        CanvasRenderRequest restarted = secondBackend.renderRequests.getFirst();
        assertEquals(secondBackend.request.sessionId(),
                restarted.revisionKey().sessionId());
        assertEquals(0, restarted.revisionKey().presentationSequence());
        assertEquals(first.revisionKey().logicalRevisionId(),
                restarted.revisionKey().logicalRevisionId());

        secondBackend.succeedRender(0, ready(restarted));
        assertEquals(secondBackend.request.sessionId(), assertInstanceOf(
                CanvasControllerState.Presented.class,
                controller.state()).frameKey().revisionKey().sessionId());
    }

    @Test
    void retainsTheLatestDesiredRevisionAcrossFailureAndRestart() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = controller(backends);
        controller.open();
        CanvasRenderRequest failedSessionRequest = controller.present(
                profile(ENGINE), snapshot(12));
        FakeBackendSession first = backends.latest();
        first.ready.completeExceptionally(new IllegalStateException(
                "synthetic startup failure"));

        CanvasControllerState.Failed failed = assertInstanceOf(
                CanvasControllerState.Failed.class, controller.state());
        assertEquals(CanvasFailureStage.STARTUP, failed.stage());
        assertEquals("synthetic startup failure", failed.reason());
        assertEquals(1, first.closeCount);

        assertTrue(controller.restart());
        FakeBackendSession replacement = backends.latest();
        replacement.succeedReady(ENGINE);
        CanvasRenderRequest retried = replacement.renderRequests.getFirst();

        assertNotEquals(failedSessionRequest.revisionKey().sessionId(),
                retried.revisionKey().sessionId());
        assertEquals(12, retried.revisionKey().logicalRevisionId());
        assertEquals(0, retried.revisionKey().presentationSequence());
    }

    @Test
    void rejectsHandshakeAndRequestedEngineMismatchBeforeRendering() {
        FakeBackendFactory wrongProtocolBackends = new FakeBackendFactory();
        CanvasLifecycleController wrongProtocol = controller(wrongProtocolBackends);
        wrongProtocol.open();
        FakeBackendSession first = wrongProtocolBackends.latest();
        first.ready.complete(new CanvasBackendReady(
                first.request.sessionId(),
                first.request.protocolVersion() + 1,
                ENGINE));
        assertEquals(CanvasFailureStage.HANDSHAKE, assertInstanceOf(
                CanvasControllerState.Failed.class,
                wrongProtocol.state()).stage());

        FakeBackendFactory wrongEngineBackends = new FakeBackendFactory();
        CanvasLifecycleController wrongEngine = controller(wrongEngineBackends);
        wrongEngine.open();
        wrongEngine.present(profile(ENGINE), snapshot(0));
        CanvasEngineIdentity otherEngine = new CanvasEngineIdentity(
                "3.45.0", "other-framework", "other-engine", "3.13.0");
        wrongEngineBackends.latest().succeedReady(otherEngine);

        CanvasControllerState.Failed failed = assertInstanceOf(
                CanvasControllerState.Failed.class, wrongEngine.state());
        assertEquals(CanvasFailureStage.HANDSHAKE, failed.stage());
        assertTrue(wrongEngineBackends.latest().renderRequests.isEmpty());
    }

    @Test
    void unexpectedTerminationFailsButOldExpectedTerminationCannotWinRestart() {
        FakeBackendFactory terminatedBackends = new FakeBackendFactory();
        CanvasLifecycleController terminated = readyController(terminatedBackends);
        terminatedBackends.latest().termination.complete(null);

        CanvasControllerState.Failed failed = assertInstanceOf(
                CanvasControllerState.Failed.class, terminated.state());
        assertEquals(CanvasFailureStage.TERMINATION, failed.stage());

        FakeBackendFactory restartBackends = new FakeBackendFactory();
        CanvasLifecycleController restarting = readyController(restartBackends);
        FakeBackendSession old = restartBackends.latest();
        assertTrue(restarting.restart());
        FakeBackendSession replacement = restartBackends.latest();
        old.termination.complete(null);

        CanvasControllerState.Starting state = assertInstanceOf(
                CanvasControllerState.Starting.class, restarting.state());
        assertEquals(replacement.request, state.request());
    }

    @Test
    void closeDuringStartupOrRenderIsTerminalAndSuppressesLateWork() {
        FakeBackendFactory startupBackends = new FakeBackendFactory();
        CanvasLifecycleController duringStartup = controller(startupBackends);
        duringStartup.open();
        FakeBackendSession startingBackend = startupBackends.latest();
        duringStartup.close();
        duringStartup.close();
        startingBackend.succeedReady(ENGINE);

        assertInstanceOf(CanvasControllerState.Closed.class,
                duringStartup.state());
        assertEquals(1, startingBackend.closeCount);
        assertFalse(duringStartup.restart());
        assertThrows(IllegalStateException.class, duringStartup::open);

        FakeBackendFactory renderBackends = new FakeBackendFactory();
        CanvasLifecycleController duringRender = readyController(renderBackends);
        CanvasRenderRequest request = duringRender.present(
                profile(ENGINE), snapshot(0));
        FakeBackendSession renderingBackend = renderBackends.latest();
        duringRender.close();
        renderingBackend.succeedRender(0, ready(request));

        assertInstanceOf(CanvasControllerState.Closed.class,
                duringRender.state());
        assertEquals(1, renderingBackend.closeCount);
    }

    @Test
    void rejectsResponseForAnotherRequestAndListenerFailureDoesNotWedge() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = controller(backends);
        controller.addListener((previous, current) -> {
            throw new IllegalStateException("synthetic listener failure");
        });
        controller.open();
        backends.latest().succeedReady(ENGINE);
        CanvasRenderRequest request = controller.present(profile(ENGINE), snapshot(0));
        CanvasRevisionKey foreignRevision = new CanvasRevisionKey(
                request.revisionKey().sessionId(),
                request.revisionKey().presentationSequence() + 1,
                request.revisionKey().documentId(),
                request.revisionKey().logicalRevisionId());
        CanvasFrameKey foreignFrame = new CanvasFrameKey(foreignRevision, 0);

        backends.latest().succeedRender(0, new CanvasPresentationReady(
                foreignFrame,
                new CanvasLayoutKey(foreignFrame, 0)));

        CanvasControllerState.Failed failed = assertInstanceOf(
                CanvasControllerState.Failed.class, controller.state());
        assertEquals(CanvasFailureStage.PROTOCOL, failed.stage());
        assertTrue(failed.reason().contains("different render request"));
    }

    @Test
    void listenerCloseBeforeBackendOpenOrRenderPreventsLateBackendCalls() {
        FakeBackendFactory startupBackends = new FakeBackendFactory();
        CanvasLifecycleController startup = controller(startupBackends);
        startup.addListener((previous, current) -> {
            if (current instanceof CanvasControllerState.Starting) {
                startup.close();
            }
        });

        startup.open();

        assertInstanceOf(CanvasControllerState.Closed.class, startup.state());
        assertTrue(startupBackends.sessions.isEmpty(),
                "a close published from Starting must win before factory.open");

        FakeBackendFactory renderBackends = new FakeBackendFactory();
        CanvasLifecycleController rendering = readyController(renderBackends);
        FakeBackendSession backend = renderBackends.latest();
        rendering.addListener((previous, current) -> {
            if (current instanceof CanvasControllerState.Rendering) {
                rendering.close();
            }
        });

        rendering.present(profile(ENGINE), snapshot(0));

        assertInstanceOf(CanvasControllerState.Closed.class, rendering.state());
        assertTrue(backend.renderRequests.isEmpty(),
                "a close published from Rendering must win before backend.render");
        assertEquals(1, backend.closeCount);
    }

    @Test
    void listenerRestartFromRenderingFencesTheOldBackendCall() {
        FakeBackendFactory backends = new FakeBackendFactory();
        CanvasLifecycleController controller = readyController(backends);
        FakeBackendSession first = backends.latest();
        AtomicBoolean restarted = new AtomicBoolean();
        controller.addListener((previous, current) -> {
            if (current instanceof CanvasControllerState.Rendering
                    && restarted.compareAndSet(false, true)) {
                controller.restart();
            }
        });

        controller.present(profile(ENGINE), snapshot(19));

        assertTrue(first.renderRequests.isEmpty());
        assertEquals(1, first.closeCount);
        assertEquals(2, backends.sessions.size());
        FakeBackendSession replacement = backends.latest();
        replacement.succeedReady(ENGINE);
        assertEquals(1, replacement.renderRequests.size());
        assertEquals(19, replacement.renderRequests.getFirst()
                .revisionKey().logicalRevisionId());
    }

    @Test
    void rejectingCallbackExecutorFallsBackWithoutWedgingLifecycle() {
        FakeBackendFactory backends = new FakeBackendFactory();
        Executor rejecting = command -> {
            throw new RejectedExecutionException("synthetic rejection");
        };
        CanvasLifecycleController controller = new CanvasLifecycleController(
                backends, rejecting, DOCUMENT_ID);
        controller.open();
        FakeBackendSession backend = backends.latest();

        backend.succeedReady(ENGINE);
        assertInstanceOf(CanvasControllerState.Idle.class, controller.state());
        CanvasRenderRequest request = controller.present(
                profile(ENGINE), snapshot(0));
        backend.succeedRender(0, ready(request));
        assertInstanceOf(CanvasControllerState.Presented.class, controller.state());

        backend.termination.complete(null);
        assertEquals(CanvasFailureStage.TERMINATION, assertInstanceOf(
                CanvasControllerState.Failed.class,
                controller.state()).stage());
    }

    @Test
    void failureReasonsAreAlwaysBoundedSingleLineAndDirectionSafe() {
        FakeBackendFactory anonymousBackends = new FakeBackendFactory();
        CanvasLifecycleController anonymous = controller(anonymousBackends);
        anonymous.open();
        anonymousBackends.latest().ready.completeExceptionally(new Throwable() {
            @Override
            public String getMessage() {
                throw new IllegalStateException("hostile getMessage");
            }
        });
        String fallback = assertInstanceOf(
                CanvasControllerState.Failed.class,
                anonymous.state()).reason();
        assertFalse(fallback.isBlank());

        FakeBackendFactory hostileBackends = new FakeBackendFactory();
        CanvasLifecycleController hostile = controller(hostileBackends);
        hostile.open();
        hostileBackends.latest().ready.completeExceptionally(
                new IllegalStateException(
                        "Runner\n\u202E" + "x".repeat(2_000)));

        String normalized = assertInstanceOf(
                CanvasControllerState.Failed.class,
                hostile.state()).reason();
        assertTrue(normalized.codePointCount(0, normalized.length()) <= 512);
        assertFalse(normalized.contains("\n"));
        assertFalse(normalized.contains("\u202E"));
        assertTrue(normalized.endsWith("\u2026"));

        FakeBackendFactory whitespaceBackends = new FakeBackendFactory();
        CanvasLifecycleController whitespace = controller(whitespaceBackends);
        whitespace.open();
        whitespaceBackends.latest().ready.completeExceptionally(
                new IllegalStateException(" ".repeat(100_000)));

        String whitespaceFallback = assertInstanceOf(
                CanvasControllerState.Failed.class,
                whitespace.state()).reason();
        assertEquals(
                "Canvas backend failed without a usable diagnostic",
                whitespaceFallback);
    }

    private static CanvasLifecycleController controller(
            FakeBackendFactory backends) {
        return new CanvasLifecycleController(backends, Runnable::run, DOCUMENT_ID);
    }

    private static CanvasLifecycleController readyController(
            FakeBackendFactory backends) {
        CanvasLifecycleController controller = controller(backends);
        controller.open();
        backends.latest().succeedReady(ENGINE);
        assertInstanceOf(CanvasControllerState.Idle.class, controller.state());
        return controller;
    }

    private static CanvasPresentationReady ready(CanvasRenderRequest request) {
        CanvasFrameKey frame = new CanvasFrameKey(request.revisionKey(), 0);
        return new CanvasPresentationReady(frame, new CanvasLayoutKey(frame, 0));
    }

    private static CanvasRenderProfile profile(CanvasEngineIdentity engine) {
        return new CanvasRenderProfile(
                CanvasPreviewMode.MOBILE,
                CanvasTargetPlatform.ANDROID,
                new CanvasViewport(390.0d, 844.0d),
                new CanvasDevicePixelRatio(3.0d),
                new CanvasResolvedTheme(
                        "material_light_default_v1",
                        0xFF6750A4,
                        CanvasThemeBrightness.LIGHT,
                        "A".repeat(64)),
                new CanvasLocale("uk-UA"),
                new CanvasTextScaleFactor(1.0d),
                engine);
    }

    private static ValidatedCanvasRevisionSnapshot snapshot(long logicalRevisionId) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("0.1.3-SNAPSHOT"),
                new ManagedRegions(region, region));
        WidgetNode root = WidgetNode.empty(
                ROOT_ID,
                new WidgetTypeId("flutter.material.Scaffold"));
        DesignerDocument document = new DesignerDocument(DOCUMENT_ID, source, root);
        return ValidatedCanvasRevisionSnapshot.validate(
                logicalRevisionId,
                document,
                CATALOG,
                ValidationLimits.defaults());
    }

    private static final class FakeBackendFactory implements CanvasBackendFactory {
        private final List<FakeBackendSession> sessions = new ArrayList<>();

        @Override
        public CanvasBackendSession open(CanvasBackendOpenRequest request) {
            FakeBackendSession session = new FakeBackendSession(request);
            sessions.add(session);
            return session;
        }

        FakeBackendSession latest() {
            return sessions.getLast();
        }
    }

    private static final class FakeBackendSession implements CanvasBackendSession {
        private final CanvasBackendOpenRequest request;
        private final CompletableFuture<CanvasBackendReady> ready =
                new CompletableFuture<>();
        private final CompletableFuture<Void> termination =
                new CompletableFuture<>();
        private final List<CanvasRenderRequest> renderRequests = new ArrayList<>();
        private final List<CompletableFuture<CanvasPresentationReady>> renderResults =
                new ArrayList<>();
        private int closeCount;

        FakeBackendSession(CanvasBackendOpenRequest request) {
            this.request = request;
        }

        @Override
        public CompletionStage<CanvasBackendReady> ready() {
            return ready;
        }

        @Override
        public CompletionStage<CanvasPresentationReady> render(
                CanvasRenderRequest renderRequest) {
            renderRequests.add(renderRequest);
            CompletableFuture<CanvasPresentationReady> result =
                    new CompletableFuture<>();
            renderResults.add(result);
            return result;
        }

        @Override
        public CompletionStage<Void> termination() {
            return termination;
        }

        @Override
        public void close() {
            closeCount++;
        }

        void succeedReady(CanvasEngineIdentity engine) {
            ready.complete(new CanvasBackendReady(
                    request.sessionId(),
                    request.protocolVersion(),
                    engine));
        }

        void succeedRender(int index, CanvasPresentationReady presentation) {
            renderResults.get(index).complete(presentation);
        }
    }
}
