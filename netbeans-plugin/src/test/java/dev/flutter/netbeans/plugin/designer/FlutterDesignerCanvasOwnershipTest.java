package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Optional;
import java.util.concurrent.Callable;
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
        private int closeCalls;
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
