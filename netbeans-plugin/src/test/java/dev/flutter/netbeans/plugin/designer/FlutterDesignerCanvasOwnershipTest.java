package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
import java.awt.Canvas;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.util.Lookup;

class FlutterDesignerCanvasOwnershipTest {
    @Test
    void ownerCapturesOneStableComponentAndClosesItsSessionExactlyOnce()
            throws Exception {
        TestSession session = new TestSession();
        Canvas exactSurface = new Canvas();
        session.component.add(exactSurface);

        FlutterDesignerCanvasOwner owner = onEdt(() ->
                FlutterDesignerCanvasOwner.adopt(Backend.NATIVE, session));

        assertEquals(Backend.NATIVE, owner.backend());
        assertSame(session.component, owner.component());
        assertSame(exactSurface, owner.focusSurface());
        onEdt(() -> {
            owner.close();
            owner.close();
            return null;
        });
        assertTrue(owner.closed());
        assertEquals(1, session.closeCalls);
    }

    @Test
    void rejectedSessionComponentIsClosedBeforeOwnershipEscapes() {
        TestSession session = new TestSession();
        session.componentResult = null;

        assertThrows(NullPointerException.class, () ->
                FlutterDesignerCanvasOwner.adopt(Backend.NATIVE, session));

        assertEquals(1, session.closeCalls);
    }

    @Test
    void concurrentRetirementCallsShareOneInFlightAttempt() throws Exception {
        TestSession session = new TestSession();
        CompletableFuture<Void> pending = new CompletableFuture<>();
        session.retirementAttempts.add(pending);
        FlutterDesignerCanvasOwner owner =
                FlutterDesignerCanvasOwner.adopt(Backend.EXACT_WEB, session);
        int callers = 8;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        List<Future<CompletionStage<Void>>> calls = new ArrayList<>();
        try {
            for (int index = 0; index < callers; index++) {
                calls.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return owner.preparePeerRemovalAsync();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            CompletionStage<Void> shared = calls.getFirst().get(5, TimeUnit.SECONDS);
            for (Future<CompletionStage<Void>> call : calls) {
                assertSame(shared, call.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, session.preparePeerRemovalCalls);
            assertEquals(
                    FlutterDesignerCanvasOwner.RetirementState.RETIRING,
                    owner.retirementState());
            assertFalse(owner.closed());

            pending.complete(null);
            shared.toCompletableFuture().join();
            assertEquals(
                    FlutterDesignerCanvasOwner.RetirementState.RETIRED,
                    owner.retirementState());
            assertTrue(owner.closed());
            assertSame(shared, owner.preparePeerRemovalAsync());
            assertEquals(1, session.preparePeerRemovalCalls);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failedRetirementIsPoisonedButRetryableUntilSuccess() {
        TestSession session = new TestSession();
        CompletableFuture<Void> failed = new CompletableFuture<>();
        CompletableFuture<Void> recovered = new CompletableFuture<>();
        session.retirementAttempts.add(failed);
        session.retirementAttempts.add(recovered);
        FlutterDesignerCanvasOwner owner =
                FlutterDesignerCanvasOwner.adopt(Backend.EXACT_WEB, session);

        CompletionStage<Void> first = owner.preparePeerRemovalAsync();
        IOException cleanupFailure = new IOException("simulated WebView2 cleanup failure");
        failed.completeExceptionally(cleanupFailure);
        CompletionException reported = assertThrows(
                CompletionException.class,
                () -> first.toCompletableFuture().join());
        assertSame(cleanupFailure, reported.getCause());
        assertEquals(
                FlutterDesignerCanvasOwner.RetirementState.POISONED,
                owner.retirementState());
        assertFalse(owner.closed());

        CompletionStage<Void> retry = owner.preparePeerRemovalAsync();
        assertNotSame(first, retry);
        assertEquals(2, session.preparePeerRemovalCalls);
        assertEquals(
                FlutterDesignerCanvasOwner.RetirementState.RETIRING,
                owner.retirementState());
        assertFalse(owner.closed());

        recovered.complete(null);
        retry.toCompletableFuture().join();
        assertEquals(
                FlutterDesignerCanvasOwner.RetirementState.RETIRED,
                owner.retirementState());
        assertTrue(owner.closed());
    }

    @Test
    void closeInitiatesAsyncRetirementWithoutClaimingEarlySuccess() {
        TestSession session = new TestSession();
        CompletableFuture<Void> pending = new CompletableFuture<>();
        session.retirementAttempts.add(pending);
        FlutterDesignerCanvasOwner owner =
                FlutterDesignerCanvasOwner.adopt(Backend.EXACT_WEB, session);

        owner.close();

        assertEquals(1, session.preparePeerRemovalCalls);
        assertEquals(
                FlutterDesignerCanvasOwner.RetirementState.RETIRING,
                owner.retirementState());
        assertFalse(owner.closed());
        pending.complete(null);
        assertTrue(owner.closed());
    }

    @Test
    void rejectedAdoptionPreservesSynchronousCleanupFailureAsSuppressed() {
        TestSession session = new TestSession();
        session.componentResult = null;
        IllegalStateException cleanupFailure =
                new IllegalStateException("simulated cleanup failure");
        session.closeAction = () -> {
            throw cleanupFailure;
        };

        NullPointerException adoptionFailure = assertThrows(
                NullPointerException.class,
                () -> FlutterDesignerCanvasOwner.adopt(Backend.NATIVE, session));

        assertEquals(1, session.closeCalls);
        assertEquals(1, adoptionFailure.getSuppressed().length);
        assertSame(cleanupFailure, adoptionFailure.getSuppressed()[0]);
    }

    @Test
    void rejectedAdoptionPreservesLaterAsyncCleanupFailureAsSuppressed() {
        TestSession session = new TestSession();
        session.componentResult = null;
        CompletableFuture<Void> cleanup = new CompletableFuture<>();
        session.retirementAttempts.add(cleanup);

        NullPointerException adoptionFailure = assertThrows(
                NullPointerException.class,
                () -> FlutterDesignerCanvasOwner.adopt(Backend.EXACT_WEB, session));
        assertEquals(0, adoptionFailure.getSuppressed().length);

        IOException cleanupFailure = new IOException("later cleanup failure");
        cleanup.completeExceptionally(cleanupFailure);
        assertEquals(1, adoptionFailure.getSuppressed().length);
        assertSame(cleanupFailure, adoptionFailure.getSuppressed()[0]);
        assertEquals(1, session.preparePeerRemovalCalls);
    }

    @Test
    void nativeFactoryClosesTheRawHostWhenSessionCreationFails() {
        TestHost host = new TestHost();
        TestProvider provider = new TestProvider(host);
        FlutterDesignerNativeCanvasSessionFactory factory =
                FlutterDesignerNativeCanvasSessionFactory.forTests(
                        () -> provider,
                        (ignoredProvider, ignoredHost, ignoredCallbacks) -> {
                            throw new IOException("simulated session failure");
                        });

        FlutterDesignerCanvasSessionFactory.CreationException failure =
                assertThrows(
                        FlutterDesignerCanvasSessionFactory.CreationException.class,
                        () -> factory.create(Backend.NATIVE, callbacks()));

        assertEquals("test native surface", failure.target());
        assertTrue(failure.getMessage().contains("simulated session failure"));
        assertEquals(1, provider.createHostCalls);
        assertEquals(1, host.closeCalls);
    }

    @Test
    void nativeFactoryTransfersTheHostToExactlyOneReturnedOwner() throws Exception {
        TestHost host = new TestHost();
        TestProvider provider = new TestProvider(host);
        TestSession session = new TestSession();
        session.closeAction = host::close;
        FlutterDesignerNativeCanvasSessionFactory factory =
                FlutterDesignerNativeCanvasSessionFactory.forTests(
                        () -> provider,
                        (ignoredProvider, acceptedHost, ignoredCallbacks) -> {
                            assertSame(host, acceptedHost);
                            return session;
                        });

        FlutterDesignerCanvasOwner owner = factory.create(
                Backend.NATIVE, callbacks());
        assertEquals(0, host.closeCalls);

        owner.close();
        owner.close();

        assertEquals(1, session.closeCalls);
        assertEquals(1, host.closeCalls);
    }

    @Test
    void routedFactoryCreatesTheExactRequestedBackendOnly() throws Exception {
        AtomicInteger nativeCreates = new AtomicInteger();
        AtomicInteger webCreates = new AtomicInteger();
        TestSession nativeSession = new TestSession();
        TestSession webSession = new TestSession();
        FlutterDesignerCanvasSessionFactory factory =
                FlutterDesignerRoutedCanvasSessionFactory.forTests(
                        (backend, ignoredCallbacks) -> {
                            nativeCreates.incrementAndGet();
                            return FlutterDesignerCanvasOwner.adopt(
                                    backend, nativeSession);
                        },
                        ignoredCallbacks -> {
                            webCreates.incrementAndGet();
                            return webSession;
                        });

        FlutterDesignerCanvasOwner web = factory.create(
                Backend.EXACT_WEB, callbacks());
        assertEquals(Backend.EXACT_WEB, web.backend());
        assertEquals(0, nativeCreates.get());
        assertEquals(1, webCreates.get());

        FlutterDesignerCanvasOwner nativeOwner = factory.create(
                Backend.NATIVE, callbacks());
        assertEquals(Backend.NATIVE, nativeOwner.backend());
        assertEquals(1, nativeCreates.get());
        assertEquals(1, webCreates.get());
    }

    @Test
    void routedFactoryNamesExactWebCreationFailures() {
        FlutterDesignerCanvasSessionFactory factory =
                FlutterDesignerRoutedCanvasSessionFactory.forTests(
                        (backend, ignoredCallbacks) -> {
                            throw new AssertionError("native route must not run");
                        },
                        ignoredCallbacks -> {
                            throw new IOException("simulated WebView2 load failure");
                        });

        FlutterDesignerCanvasSessionFactory.CreationException failure =
                assertThrows(
                        FlutterDesignerCanvasSessionFactory.CreationException.class,
                        () -> factory.create(Backend.EXACT_WEB, callbacks()));

        assertEquals("exact Flutter Web Canvas", failure.target());
        assertTrue(failure.getMessage().contains("simulated WebView2 load failure"));
    }

    @Test
    void multiViewPublishesOnlyOneEagerOwnerAndClosesItOnFailureOrTeardown()
            throws Exception {
        AtomicInteger successfulCreates = new AtomicInteger();
        TestSession successfulSession = new TestSession();
        FlutterDesignerCanvasSessionFactory successfulFactory =
                (backend, ignoredCallbacks) -> {
                    assertEquals(Backend.NATIVE, backend);
                    successfulCreates.incrementAndGet();
                    return FlutterDesignerCanvasOwner.adopt(
                            backend, successfulSession);
                };

        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        FlutterDesignerCanvasBackendSelector.production(),
                        successfulFactory));
        try {
            assertEquals(1, successfulCreates.get());
            assertTrue(SwingUtilities.isDescendingFrom(
                    successfulSession.component,
                    design.getVisualRepresentation()));
            onEdt(() -> {
                design.componentShowing();
                design.componentHidden();
                design.componentShowing();
                return null;
            });
            assertEquals(1, successfulCreates.get());
        } finally {
            onEdt(() -> {
                design.componentClosed();
                design.componentClosed();
                return null;
            });
        }
        assertEquals(1, successfulSession.closeCalls);

        TestSession rejectedSession = new TestSession();
        rejectedSession.failViewportListener = true;
        AtomicInteger rejectedCreates = new AtomicInteger();
        FlutterDesignerMultiViewDesign rejected = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        FlutterDesignerCanvasBackendSelector.production(),
                        (backend, ignoredCallbacks) -> {
                            rejectedCreates.incrementAndGet();
                            return FlutterDesignerCanvasOwner.adopt(
                                    backend, rejectedSession);
                        }));
        try {
            assertEquals(1, rejectedCreates.get());
            assertEquals(1, rejectedSession.closeCalls);
            assertNull(rejectedSession.component.getParent());
        } finally {
            onEdt(() -> {
                rejected.componentClosed();
                return null;
            });
        }
        assertEquals(1, rejectedSession.closeCalls);
    }

    private static FlutterDesignerCanvasSessionFactory.Callbacks callbacks() {
        return new FlutterDesignerCanvasSessionFactory.Callbacks(
                ignored -> { },
                ignored -> { },
                ignored -> Optional.empty(),
                ignored -> { },
                ignored -> { });
    }

    private static <T> T onEdt(Callable<T> task) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return task.call();
        }
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            try {
                value.set(task.call());
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        Throwable thrown = failure.get();
        if (thrown instanceof Exception exception) {
            throw exception;
        }
        if (thrown instanceof Error error) {
            throw error;
        }
        return value.get();
    }

    private static final class TestSession
            implements FlutterDesignerCanvasSession {
        private final JPanel component = new JPanel();
        private JComponent componentResult = component;
        private Runnable closeAction = () -> { };
        private final ArrayDeque<CompletionStage<Void>> retirementAttempts =
                new ArrayDeque<>();
        private int closeCalls;
        private int preparePeerRemovalCalls;
        private boolean failViewportListener;

        @Override
        public JComponent component() {
            return componentResult;
        }

        @Override
        public boolean isSurfaceFocused() {
            return false;
        }

        @Override
        public boolean releaseSurfaceFocus() {
            return false;
        }

        @Override
        public void show() {
        }

        @Override
        public void hide() {
        }

        @Override
        public void requestFocus() {
        }

        @Override
        public void clearFocusRequest() {
        }

        @Override
        public boolean restart() {
            return false;
        }

        @Override
        public boolean canRestart() {
            return false;
        }

        @Override
        public void present(
                DesignerDocument document,
                WidgetCatalog catalog,
                CanvasPreviewMode previewMode,
                CanvasTargetPlatform targetPlatform,
                CanvasResolvedTheme resolvedTheme) {
        }

        @Override
        public void withdraw() {
        }

        @Override
        public void selectWidget(StableId widgetId) {
        }

        @Override
        public void setViewportMetricsListener(
                Consumer<CanvasViewportMetrics> listener) {
            if (failViewportListener) {
                throw new IllegalStateException("simulated listener failure");
            }
        }

        @Override
        public void setInteractionListener(Runnable listener) {
        }

        @Override
        public void setTextEditCommitListener(
                Consumer<CanvasRunnerRuntimeEvent.TextEditCommit> listener) {
        }

        @Override
        public void setInteractionBarrierListener(
                Consumer<InteractionBarrierState> listener) {
        }

        @Override
        public InteractionBarrierState interactionBarrierState() {
            return new InteractionBarrierState(
                    InteractionBarrierPhase.INACTIVE,
                    0,
                    Optional.empty());
        }

        @Override
        public void setViewportPresentation(
                CanvasViewportPresentation presentation) {
        }

        @Override
        public boolean paletteCatalogInsertDropAvailable() {
            return false;
        }

        @Override
        public boolean authorizePaletteDragSource(
                String token,
                WidgetTypeId widgetType) {
            return false;
        }

        @Override
        public void showWidgetMovePreview(
                StableId sourceWidgetId,
                WidgetPlacement destination) {
        }

        @Override
        public void clearWidgetMovePreview() {
        }

        @Override
        public CompletionStage<Void> preparePeerRemovalAsync() {
            if (retirementAttempts.isEmpty()) {
                return FlutterDesignerCanvasSession.super.preparePeerRemovalAsync();
            }
            preparePeerRemovalCalls++;
            return retirementAttempts.removeFirst();
        }

        @Override
        public void close() {
            closeCalls++;
            closeAction.run();
        }
    }

    private static final class TestProvider
            implements NativeCanvasPlatformProvider {
        private final TestHost host;
        private int createHostCalls;

        private TestProvider(TestHost host) {
            this.host = host;
        }

        @Override
        public NativeCanvasPlatform platform() {
            return NativeCanvasPlatform.WINDOWS;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String availabilityReason() {
            return "available for test";
        }

        @Override
        public NativeCanvasHost createHost() {
            createHostCalls++;
            return host;
        }

        @Override
        public String targetDescription() {
            return "test native surface";
        }

        @Override
        public dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasRunnerLaunch
                createLaunch(
                        Path executable,
                        NativeCanvasParentHandle parentHandle,
                        long hostProcessId,
                        long surfaceEpoch,
                        String nonce) {
            throw new AssertionError("launch is not used by this test");
        }
    }

    private static final class TestHost implements NativeCanvasHost {
        private final JPanel component = new JPanel();
        private int closeCalls;

        @Override
        public JComponent component() {
            return component;
        }

        @Override
        public NativeCanvasParentHandle parentHandle() {
            return new NativeCanvasParentHandle(
                    NativeCanvasPlatform.WINDOWS,
                    "0x0000000000000001");
        }

        @Override
        public boolean attachRunner(long runnerProcessId) {
            return false;
        }

        @Override
        public void detachRunner() {
        }

        @Override
        public boolean isRunnerAttached() {
            return false;
        }

        @Override
        public boolean isRunnerSurfaceLive() {
            return false;
        }

        @Override
        public boolean isNativePeerReady() {
            return false;
        }

        @Override
        public Optional<NativeCanvasSurfaceMetrics> surfaceMetrics() {
            return Optional.empty();
        }

        @Override
        public void setRunnerVisible(boolean visible) {
        }

        @Override
        public boolean requestRunnerFocus() {
            return false;
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
        public void onSurfaceMetricsChanged(
                Consumer<NativeCanvasSurfaceMetrics> listener) {
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
