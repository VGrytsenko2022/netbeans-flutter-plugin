package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasImageResourceBundle;
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
import java.awt.event.ActionEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.netbeans.core.spi.multiview.CloseOperationState;
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

    @Test
    void hiddenLateOwnerActivationDoesNotReinstallGlobalFocusListeners()
            throws Exception {
        TestSession nativeSession = new TestSession();
        TestSession exactSession = new TestSession();
        AtomicReference<FlutterDesignerMultiViewDesign> designReference =
                new AtomicReference<>();
        FlutterDesignerCanvasSessionFactory factory = (backend, ignoredCallbacks) -> {
            if (backend == Backend.EXACT_WEB) {
                FlutterDesignerMultiViewDesign design = designReference.get();
                assertTrue(EventQueue.isDispatchThread());
                design.componentHidden();
                return FlutterDesignerCanvasOwner.adopt(backend, exactSession);
            }
            return FlutterDesignerCanvasOwner.adopt(backend, nativeSession);
        };

        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        factory));
        designReference.set(design);
        try {
            onEdt(() -> {
                design.componentOpened();
                assertFalse(design.canvasFocusListenersInstalledForTests(),
                        "an opened but hidden Design view must not own global listeners");
                design.componentShowing();
                assertTrue(design.canvasFocusListenersInstalledForTests());
                design.requestCanvasBackendForTests(Backend.EXACT_WEB);
                return null;
            });

            assertSame(Backend.EXACT_WEB, design.canvasOwnerForTests().backend());
            assertFalse(design.canvasFocusListenersInstalledForTests(),
                    "late activation must preserve the hidden-view listener fence");
            assertNull(nativeSession.component.getParent());
            assertTrue(SwingUtilities.isDescendingFrom(
                    exactSession.component,
                    design.getVisualRepresentation()));
        } finally {
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(1, nativeSession.closeCalls);
        assertEquals(1, exactSession.closeCalls);
    }

    @Test
    void multiViewConfigurationFailureRetainsOwnerUntilCleanupAndCreationRetries()
            throws Exception {
        TestSession rejectedSession = new TestSession();
        rejectedSession.failViewportListener = true;
        CompletableFuture<Void> failedCleanup = new CompletableFuture<>();
        CompletableFuture<Void> recoveredCleanup = new CompletableFuture<>();
        rejectedSession.retirementAttempts.add(failedCleanup);
        rejectedSession.retirementAttempts.add(recoveredCleanup);
        TestSession replacementSession = new TestSession();
        AtomicInteger factoryCalls = new AtomicInteger();
        AtomicReference<FlutterDesignerCanvasOwner> rejectedOwner =
                new AtomicReference<>();
        FlutterDesignerCanvasSessionFactory factory = (backend, ignoredCallbacks) -> {
            int attempt = factoryCalls.incrementAndGet();
            if (attempt == 1) {
                FlutterDesignerCanvasOwner owner =
                        FlutterDesignerCanvasOwner.adopt(backend, rejectedSession);
                rejectedOwner.set(owner);
                return owner;
            }
            assertEquals(2, attempt,
                    "configuration failure must not enter an automatic create loop");
            return FlutterDesignerCanvasOwner.adopt(backend, replacementSession);
        };

        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        FlutterDesignerCanvasBackendSelector.production(),
                        factory));
        try {
            onEdt(() -> {
                design.componentOpened();
                return null;
            });
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(1, factoryCalls.get());
            assertEquals(1, rejectedSession.preparePeerRemovalCalls);
            assertNull(design.canvasOwnerForTests());

            IOException cleanupFailure = new IOException(
                    "simulated rejected-owner cleanup failure");
            onEdt(() -> {
                failedCleanup.completeExceptionally(cleanupFailure);
                return null;
            });

            FlutterDesignerCanvasOwner retained = rejectedOwner.get();
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.POISONED,
                    design.canvasOwnerPhaseForTests());
            assertNull(design.canvasOwnerForTests());
            assertSame(
                    FlutterDesignerCanvasOwner.RetirementState.POISONED,
                    retained.retirementState());
            assertEquals(1, factoryCalls.get());
            assertEquals(1, rejectedSession.preparePeerRemovalCalls);
            assertNull(rejectedSession.component.getParent());
            assertNull(replacementSession.component.getParent());

            onEdt(() -> {
                design.retryCanvasOwnerTransitionForTests();
                return null;
            });
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(2, rejectedSession.preparePeerRemovalCalls);
            assertEquals(1, factoryCalls.get());

            onEdt(() -> {
                recoveredCleanup.complete(null);
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.EMPTY,
                    design.canvasOwnerPhaseForTests());
            assertTrue(retained.closed());
            assertNull(design.canvasOwnerForTests());
            assertEquals(1, factoryCalls.get(),
                    "cleanup recovery must not automatically recreate the owner");
            assertNull(replacementSession.component.getParent());

            onEdt(() -> {
                design.retryCanvasOwnerTransitionForTests();
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            assertEquals(2, factoryCalls.get());
            assertSame(
                    replacementSession.component,
                    design.canvasOwnerForTests().component());
            assertTrue(SwingUtilities.isDescendingFrom(
                    replacementSession.component,
                    design.getVisualRepresentation()));
        } finally {
            failedCleanup.completeExceptionally(
                    new IOException("test cleanup fallback"));
            recoveredCleanup.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(1, replacementSession.closeCalls);
    }

    @Test
    void multiViewKeepsNativeComponentAttachedUntilRetirementThenInstallsExactWeb()
            throws Exception {
        TestSession nativeSession = new TestSession();
        CompletableFuture<Void> nativeRetirement = new CompletableFuture<>();
        nativeSession.retirementAttempts.add(nativeRetirement);
        TestSession webSession = new TestSession();
        AtomicInteger nativeCreates = new AtomicInteger();
        AtomicInteger webCreates = new AtomicInteger();
        FlutterDesignerCanvasSessionFactory factory = (backend, ignoredCallbacks) -> {
            TestSession selected = switch (backend) {
                case NATIVE -> {
                    nativeCreates.incrementAndGet();
                    yield nativeSession;
                }
                case EXACT_WEB -> {
                    webCreates.incrementAndGet();
                    yield webSession;
                }
            };
            return FlutterDesignerCanvasOwner.adopt(backend, selected);
        };

        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        factory));
        try {
            FlutterDesignerCanvasOwner nativeOwner = design.canvasOwnerForTests();
            assertEquals(Backend.NATIVE, nativeOwner.backend());
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            assertEquals(1, nativeCreates.get());
            assertEquals(0, webCreates.get());
            assertTrue(SwingUtilities.isDescendingFrom(
                    nativeSession.component,
                    design.getVisualRepresentation()));

            onEdt(() -> {
                design.requestCanvasBackendForTests(Backend.EXACT_WEB);
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertNull(design.canvasOwnerForTests());
            assertEquals(1, nativeSession.preparePeerRemovalCalls);
            assertEquals(0, webCreates.get());
            assertTrue(SwingUtilities.isDescendingFrom(
                    nativeSession.component,
                    design.getVisualRepresentation()));
            assertNull(webSession.component.getParent());

            onEdt(() -> {
                nativeRetirement.complete(null);
                return null;
            });

            FlutterDesignerCanvasOwner webOwner = design.canvasOwnerForTests();
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            assertEquals(Backend.EXACT_WEB, webOwner.backend());
            assertEquals(1, webCreates.get());
            assertTrue(nativeOwner.closed());
            assertNull(nativeSession.component.getParent());
            assertTrue(SwingUtilities.isDescendingFrom(
                    webSession.component,
                    design.getVisualRepresentation()));
        } finally {
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(1, webSession.closeCalls);
        assertNull(webSession.component.getParent());
    }

    @Test
    void multiViewFailedRetirementKeepsOldComponentAndRetryCompletesReplacement()
            throws Exception {
        TestSession nativeSession = new TestSession();
        CompletableFuture<Void> failedRetirement = new CompletableFuture<>();
        CompletableFuture<Void> recoveredRetirement = new CompletableFuture<>();
        nativeSession.retirementAttempts.add(failedRetirement);
        nativeSession.retirementAttempts.add(recoveredRetirement);
        TestSession webSession = new TestSession();
        AtomicInteger webCreates = new AtomicInteger();
        FlutterDesignerCanvasSessionFactory factory = (backend, ignoredCallbacks) -> {
            if (backend == Backend.EXACT_WEB) {
                webCreates.incrementAndGet();
                return FlutterDesignerCanvasOwner.adopt(backend, webSession);
            }
            return FlutterDesignerCanvasOwner.adopt(backend, nativeSession);
        };

        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        factory));
        try {
            FlutterDesignerCanvasOwner nativeOwner = design.canvasOwnerForTests();
            onEdt(() -> {
                design.requestCanvasBackendForTests(Backend.EXACT_WEB);
                return null;
            });

            IOException cleanupFailure = new IOException(
                    "simulated native peer release failure");
            onEdt(() -> {
                failedRetirement.completeExceptionally(cleanupFailure);
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.POISONED,
                    design.canvasOwnerPhaseForTests());
            assertEquals(
                    FlutterDesignerCanvasOwner.RetirementState.POISONED,
                    nativeOwner.retirementState());
            assertNull(design.canvasOwnerForTests());
            assertEquals(0, webCreates.get());
            assertTrue(SwingUtilities.isDescendingFrom(
                    nativeSession.component,
                    design.getVisualRepresentation()));
            assertNull(webSession.component.getParent());

            onEdt(() -> {
                design.retryCanvasOwnerTransitionForTests();
                return null;
            });
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(2, nativeSession.preparePeerRemovalCalls);
            assertTrue(SwingUtilities.isDescendingFrom(
                    nativeSession.component,
                    design.getVisualRepresentation()));

            onEdt(() -> {
                recoveredRetirement.complete(null);
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            assertEquals(Backend.EXACT_WEB, design.canvasOwnerForTests().backend());
            assertEquals(1, webCreates.get());
            assertTrue(nativeOwner.closed());
            assertNull(nativeSession.component.getParent());
            assertTrue(SwingUtilities.isDescendingFrom(
                    webSession.component,
                    design.getVisualRepresentation()));
        } finally {
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(1, webSession.closeCalls);
        assertNull(webSession.component.getParent());
    }

    @Test
    void multiViewCloseDuringBackendRetirementKeepsOldComponentUntilPeerSafe()
            throws Exception {
        TestSession nativeSession = new TestSession();
        CompletableFuture<Void> nativeRetirement = new CompletableFuture<>();
        nativeSession.retirementAttempts.add(nativeRetirement);
        TestSession webSession = new TestSession();
        AtomicInteger webCreates = new AtomicInteger();
        FlutterDesignerCanvasSessionFactory factory = (backend, ignoredCallbacks) -> {
            if (backend == Backend.EXACT_WEB) {
                webCreates.incrementAndGet();
                return FlutterDesignerCanvasOwner.adopt(backend, webSession);
            }
            return FlutterDesignerCanvasOwner.adopt(backend, nativeSession);
        };

        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        factory));
        try {
            onEdt(() -> {
                design.componentOpened();
                design.requestCanvasBackendForTests(Backend.EXACT_WEB);
                return null;
            });
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(0, webCreates.get());
            assertTrue(SwingUtilities.isDescendingFrom(
                    nativeSession.component,
                    design.getVisualRepresentation()));

            onEdt(() -> {
                design.componentClosed();
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(0, webCreates.get());
            assertTrue(SwingUtilities.isDescendingFrom(
                    nativeSession.component,
                    design.getVisualRepresentation()));
            assertNull(webSession.component.getParent());

            onEdt(() -> {
                nativeRetirement.complete(null);
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.CLOSED,
                    design.canvasOwnerPhaseForTests());
            assertEquals(0, webCreates.get());
            assertNull(nativeSession.component.getParent());
            assertNull(webSession.component.getParent());
        } finally {
            nativeRetirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
    }

    @Test
    void tokenizedEditorShellCloseRetriesTheExactSuccessfulAttempt()
            throws Exception {
        TestSession session = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        session.retirementAttempts.add(retirement);
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) ->
                                FlutterDesignerCanvasOwner.adopt(
                                        backend, session)));
        List<Long> retryAttempts = new ArrayList<>();
        List<Long> failureAttempts = new ArrayList<>();
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (attemptId, ignoredFailure) ->
                                failureAttempts.add(attemptId));
                design.componentOpened();
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.OPEN,
                        design.editorShellCloseBarrierState(41));
                design.beginEditorShellClose(41);
                return null;
            });

            assertEquals(
                    FlutterDesignerEditorPerspective.CloseBarrierState.PENDING,
                    onEdt(() -> design.editorShellCloseBarrierState(41)));
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(1, session.preparePeerRemovalCalls);

            onEdt(() -> {
                retirement.complete(null);
                return null;
            });
            onEdt(() -> null);

            assertEquals(
                    FlutterDesignerEditorPerspective.CloseBarrierState.READY,
                    onEdt(() -> design.editorShellCloseBarrierState(41)));
            assertEquals(List.of(41L), retryAttempts,
                    "the successful retirement must retry its owning attempt exactly once");
            assertTrue(failureAttempts.isEmpty());
        } finally {
            retirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
    }

    @Test
    void readyReplacementAttemptIsRescheduledAfterAStaleQueuedRetry()
            throws Exception {
        TestSession session = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        session.retirementAttempts.add(retirement);
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) ->
                                FlutterDesignerCanvasOwner.adopt(
                                        backend, session)));
        List<Long> retryAttempts = new ArrayList<>();
        List<Long> failureAttempts = new ArrayList<>();
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (attemptId, ignoredFailure) ->
                                failureAttempts.add(attemptId));
                design.componentOpened();
                design.beginEditorShellClose(41);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.PENDING,
                        design.editorShellCloseBarrierState(41));

                retirement.complete(null);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.READY,
                        design.editorShellCloseBarrierState(41),
                        "attempt 41 must enqueue its retry before it is abandoned");

                design.abandonEditorShellClose(41);
                design.beginEditorShellClose(42);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.READY,
                        design.editorShellCloseBarrierState(42),
                        "attempt 42 becomes ready while retry 41 is still queued");
                assertTrue(retryAttempts.isEmpty(),
                        "neither queued retry may execute inside the setup EDT event");
                return null;
            });

            // A stale retry may enqueue the replacement retry behind the first
            // flush event, so drain two EDT turns deterministically.
            onEdt(() -> null);
            onEdt(() -> null);

            assertEquals(List.of(42L), retryAttempts,
                    "the stale retry for 41 must hand off its wakeup to ready attempt 42");
            assertTrue(failureAttempts.isEmpty());
        } finally {
            retirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
    }

    @Test
    void abandonedPendingEditorShellCloseRebuildsAFreshOwnerWithoutRetrying()
            throws Exception {
        TestSession retiredSession = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        retiredSession.retirementAttempts.add(retirement);
        TestSession recoveredSession = new TestSession();
        AtomicInteger creates = new AtomicInteger();
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) -> {
                            int attempt = creates.incrementAndGet();
                            assertTrue(attempt <= 2,
                                    "pending abandonment must rebuild exactly once");
                            return FlutterDesignerCanvasOwner.adopt(
                                    backend,
                                    attempt == 1
                                            ? retiredSession : recoveredSession);
                        }));
        FlutterDesignerCanvasOwner retiredOwner = design.canvasOwnerForTests();
        List<Long> retryAttempts = new ArrayList<>();
        List<Long> failureAttempts = new ArrayList<>();
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (attemptId, ignoredFailure) ->
                                failureAttempts.add(attemptId));
                design.componentOpened();
                design.beginEditorShellClose(41);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.PENDING,
                        design.editorShellCloseBarrierState(41));
                design.abandonEditorShellClose(41);
                return null;
            });

            onEdt(() -> {
                retirement.complete(null);
                return null;
            });
            onEdt(() -> null);

            assertEquals(
                    FlutterDesignerEditorPerspective.CloseBarrierState.OPEN,
                    onEdt(() -> design.editorShellCloseBarrierState(41)));
            assertTrue(retryAttempts.isEmpty(),
                    "completion must not resurrect an abandoned close attempt");
            assertTrue(failureAttempts.isEmpty());
            assertEquals(2, creates.get(),
                    "peer-safe completion must install one fresh generation");
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            FlutterDesignerCanvasOwner recoveredOwner =
                    design.canvasOwnerForTests();
            assertNotSame(retiredOwner, recoveredOwner);
            assertEquals(recoveredSession.component, recoveredOwner.component());
            assertTrue(retiredOwner.closed());
            assertNull(retiredSession.component.getParent());
            assertTrue(SwingUtilities.isDescendingFrom(
                    recoveredSession.component,
                    design.getVisualRepresentation()));
        } finally {
            retirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(0, retiredSession.closeCalls,
                "the asynchronous retirement completion owns old-session cleanup");
        assertEquals(1, recoveredSession.closeCalls);
    }

    @Test
    void abandonedReadyCloseRebuildsCanvasInAFreshCoordinatorGeneration()
            throws Exception {
        TestSession retiredSession = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        retiredSession.retirementAttempts.add(retirement);
        TestSession recoveredSession = new TestSession();
        AtomicInteger creates = new AtomicInteger();
        List<FlutterDesignerCanvasSessionFactory.Callbacks> callbackSets =
                new ArrayList<>();
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, callbacks) -> {
                            callbackSets.add(callbacks);
                            TestSession session = creates.incrementAndGet() == 1
                                    ? retiredSession : recoveredSession;
                            return FlutterDesignerCanvasOwner.adopt(
                                    backend, session);
                        }));
        FlutterDesignerCanvasOwner retiredOwner = design.canvasOwnerForTests();
        List<Long> retryAttempts = new ArrayList<>();
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (ignoredAttempt, ignoredFailure) -> { });
                design.componentOpened();
                design.beginEditorShellClose(41);
                assertEquals(
                        FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                        design.canvasOwnerPhaseForTests());
                design.requestCanvasBackendForTests(Backend.EXACT_WEB);
                retirement.complete(null);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.READY,
                        design.editorShellCloseBarrierState(41));
                design.abandonEditorShellClose(41);
                return null;
            });

            // Drain the stale retry and then the recovery event. The retired
            // coordinator is terminal; recovery must create a distinct owner
            // generation instead of weakening closeAsync().
            onEdt(() -> null);
            onEdt(() -> null);

            assertTrue(retryAttempts.isEmpty(),
                    "an abandoned attempt must never receive its queued retry");
            assertEquals(2, creates.get());
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            assertEquals(recoveredSession.component,
                    design.canvasOwnerForTests().component());
            assertEquals(Backend.EXACT_WEB,
                    design.canvasOwnerForTests().backend(),
                    "recovery must honor the latest requested preview backend");
            assertTrue(retiredOwner.closed());
            assertNull(retiredSession.component.getParent());
            assertTrue(SwingUtilities.isDescendingFrom(
                    recoveredSession.component,
                    design.getVisualRepresentation()));

            FlutterDesignerNativeCanvasStatus currentStatus =
                    new FlutterDesignerNativeCanvasStatus(
                            FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                            "Recovered generation",
                            "current callback");
            FlutterDesignerNativeCanvasStatus staleStatus =
                    new FlutterDesignerNativeCanvasStatus(
                            FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                            "Retired generation",
                            "stale callback");
            onEdt(() -> {
                callbackSets.get(1).statusListener().accept(currentStatus);
                callbackSets.get(0).statusListener().accept(staleStatus);
                return null;
            });
            assertSame(currentStatus, design.lastCanvasStatusForTests(),
                    "retired coordinator callbacks must not cross the new "
                    + "generation fence");
        } finally {
            retirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(0, retiredSession.closeCalls,
                "the supplied asynchronous retirement completion owns the "
                + "test session cleanup");
        assertEquals(1, recoveredSession.closeCalls);
    }

    @Test
    void failedRecoveryCreationStaysEmptyUntilExplicitRetryCreatesFreshOwner()
            throws Exception {
        TestSession retiredSession = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        retiredSession.retirementAttempts.add(retirement);
        TestSession recoveredSession = new TestSession();
        AtomicInteger creates = new AtomicInteger();
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) -> {
                            int attempt = creates.incrementAndGet();
                            if (attempt == 1) {
                                return FlutterDesignerCanvasOwner.adopt(
                                        backend, retiredSession);
                            }
                            if (attempt == 2) {
                                throw new FlutterDesignerCanvasSessionFactory
                                        .CreationException(
                                                "recovered Canvas",
                                                "simulated recovery creation failure");
                            }
                            assertEquals(3, attempt,
                                    "failed recovery must wait for explicit Retry");
                            return FlutterDesignerCanvasOwner.adopt(
                                    backend, recoveredSession);
                        }));
        List<Long> retryAttempts = new ArrayList<>();
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (ignoredAttempt, ignoredFailure) -> { });
                design.componentOpened();
                design.beginEditorShellClose(41);
                retirement.complete(null);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.READY,
                        design.editorShellCloseBarrierState(41));
                design.abandonEditorShellClose(41);
                return null;
            });
            onEdt(() -> null);
            onEdt(() -> null);

            assertTrue(retryAttempts.isEmpty());
            assertEquals(2, creates.get());
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.EMPTY,
                    design.canvasOwnerPhaseForTests());
            assertNull(design.canvasOwnerForTests());
            FlutterDesignerNativeCanvasStatus failed =
                    design.lastCanvasStatusForTests();
            assertEquals(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    failed.stage());
            assertTrue(failed.detail().contains(
                    "simulated recovery creation failure"));
            assertNull(recoveredSession.component.getParent());

            onEdt(() -> {
                design.retryCanvasOwnerTransitionForTests();
                return null;
            });

            assertEquals(3, creates.get());
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.ACTIVE,
                    design.canvasOwnerPhaseForTests());
            assertEquals(recoveredSession.component,
                    design.canvasOwnerForTests().component());
            assertTrue(SwingUtilities.isDescendingFrom(
                    recoveredSession.component,
                    design.getVisualRepresentation()));
        } finally {
            retirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
        assertEquals(0, retiredSession.closeCalls);
        assertEquals(1, recoveredSession.closeCalls);
    }

    @Test
    void admittedTerminalCloseDoesNotRebuildCanvasAfterComponentClosed()
            throws Exception {
        TestSession session = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        session.retirementAttempts.add(retirement);
        AtomicInteger creates = new AtomicInteger();
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) -> {
                            assertEquals(1, creates.incrementAndGet(),
                                    "an admitted terminal close must not rebuild");
                            return FlutterDesignerCanvasOwner.adopt(
                                    backend, session);
                        }));
        AtomicBoolean componentClosed = new AtomicBoolean();
        List<Long> retryAttempts = new ArrayList<>();
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (ignoredAttempt, ignoredFailure) -> { });
                design.componentOpened();
                design.beginEditorShellClose(41);
                retirement.complete(null);
                assertEquals(
                        FlutterDesignerEditorPerspective.CloseBarrierState.READY,
                        design.editorShellCloseBarrierState(41));
                design.componentClosed();
                componentClosed.set(true);
                return null;
            });
            onEdt(() -> null);
            onEdt(() -> null);

            assertTrue(retryAttempts.isEmpty(),
                    "a physically closed component must suppress its queued retry");
            assertEquals(1, creates.get());
            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.CLOSED,
                    design.canvasOwnerPhaseForTests());
            assertNull(design.canvasOwnerForTests());
            assertNull(session.component.getParent());
        } finally {
            retirement.complete(null);
            if (!componentClosed.get()) {
                onEdt(() -> {
                    design.componentClosed();
                    return null;
                });
            }
        }
        assertEquals(0, session.closeCalls,
                "the supplied retirement completion already made the owner safe");
    }

    @Test
    void failedEditorShellCloseReportsItsAttemptWithoutGrantingAStalePermit()
            throws Exception {
        TestSession session = new TestSession();
        CompletableFuture<Void> failedRetirement = new CompletableFuture<>();
        CompletableFuture<Void> recoveredRetirement = new CompletableFuture<>();
        session.retirementAttempts.add(failedRetirement);
        session.retirementAttempts.add(recoveredRetirement);
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) ->
                                FlutterDesignerCanvasOwner.adopt(
                                        backend, session)));
        List<Long> retryAttempts = new ArrayList<>();
        List<Long> failureAttempts = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        IOException retirementFailure = new IOException(
                "simulated editor-shell retirement failure");
        try {
            onEdt(() -> {
                design.setEditorShellCloseCallbacks(
                        retryAttempts::add,
                        (attemptId, failure) -> {
                            failureAttempts.add(attemptId);
                            failures.add(failure);
                        });
                design.componentOpened();
                design.beginEditorShellClose(41);
                return null;
            });

            onEdt(() -> {
                failedRetirement.completeExceptionally(retirementFailure);
                return null;
            });
            onEdt(() -> null);

            assertTrue(retryAttempts.isEmpty(),
                    "a failed retirement must not schedule a close retry");
            assertEquals(List.of(41L), failureAttempts);
            assertEquals(1, failures.size());
            assertSame(retirementFailure, failures.get(0));
            assertEquals(
                    FlutterDesignerEditorPerspective.CloseBarrierState.OPEN,
                    onEdt(() -> design.editorShellCloseBarrierState(41)),
                    "the failed attempt must not retain a ready permit");

            onEdt(() -> {
                design.beginEditorShellClose(42);
                return null;
            });
            assertEquals(
                    FlutterDesignerEditorPerspective.CloseBarrierState.STALE,
                    onEdt(() -> design.editorShellCloseBarrierState(41)),
                    "the old token must not observe the replacement attempt's barrier");
            assertEquals(
                    FlutterDesignerEditorPerspective.CloseBarrierState.PENDING,
                    onEdt(() -> design.editorShellCloseBarrierState(42)));
            assertEquals(2, session.preparePeerRemovalCalls);

            onEdt(() -> {
                design.abandonEditorShellClose(42);
                recoveredRetirement.complete(null);
                return null;
            });
            onEdt(() -> null);
            assertTrue(retryAttempts.isEmpty());
            assertEquals(List.of(41L), failureAttempts);
        } finally {
            recoveredRetirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
    }

    @Test
    void multiViewCloseGateBecomesReadyOnlyAfterComponentIsPeerSafe()
            throws Exception {
        assertCloseGateTransition(
                false,
                FlutterDesignerAsyncCloseOperationHandler.READY_SAVE_ID);
        assertCloseGateTransition(
                true,
                FlutterDesignerAsyncCloseOperationHandler.READY_DISCARD_ID);
    }

    private static void assertCloseGateTransition(
            boolean discard,
            String expectedReadyId) throws Exception {
        TestSession session = new TestSession();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        session.retirementAttempts.add(retirement);
        FlutterDesignerMultiViewDesign design = onEdt(() ->
                new FlutterDesignerMultiViewDesign(
                        Lookup.EMPTY,
                        () -> true,
                        () -> true,
                        new FlutterDesignerCanvasBackendSelector(true),
                        (backend, ignoredCallbacks) ->
                                FlutterDesignerCanvasOwner.adopt(
                                        backend, session)));
        AtomicInteger closeRetries = new AtomicInteger();
        try {
            onEdt(() -> {
                design.setEditorShellCloseRetryRequest(
                        closeRetries::incrementAndGet);
                design.componentOpened();
                return null;
            });
            CloseOperationState pending = onEdt(design::canCloseElement);
            assertFalse(pending.canClose());
            assertEquals(
                    FlutterDesignerAsyncCloseOperationHandler.PENDING_ID,
                    pending.getCloseWarningID());

            onEdt(() -> {
                (discard
                        ? pending.getDiscardAction()
                        : pending.getProceedAction()).actionPerformed(
                                new ActionEvent(
                                        design,
                                        ActionEvent.ACTION_PERFORMED,
                                        discard ? "discard" : "proceed"));
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.RETIRING,
                    design.canvasOwnerPhaseForTests());
            assertEquals(1, session.preparePeerRemovalCalls);
            assertTrue(SwingUtilities.isDescendingFrom(
                    session.component,
                    design.getVisualRepresentation()));
            assertEquals(
                    FlutterDesignerAsyncCloseOperationHandler.PENDING_ID,
                    onEdt(design::canCloseElement).getCloseWarningID());

            onEdt(() -> {
                retirement.complete(null);
                return null;
            });

            assertEquals(
                    FlutterDesignerCanvasOwnerCoordinator.Phase.CLOSED,
                    design.canvasOwnerPhaseForTests());
            assertNull(session.component.getParent());
            CloseOperationState ready = onEdt(design::canCloseElement);
            assertFalse(ready.canClose());
            assertEquals(expectedReadyId, ready.getCloseWarningID());
            onEdt(() -> null);
            assertEquals(1, closeRetries.get(),
                    "the dedicated shell must receive one fresh close request");
        } finally {
            retirement.complete(null);
            onEdt(() -> {
                design.componentClosed();
                return null;
            });
        }
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
                CanvasResolvedTheme resolvedTheme,
                CanvasImageResourceBundle imageResources) {
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
