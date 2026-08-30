package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.plugin.designer.canvas.WindowsNativeCanvasPlatformProvider;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
import java.awt.EventQueue;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class FlutterDesignerCanvasSessionContractTest {
    @Test
    void nativeSessionImplementsCompleteBackendNeutralContract() {
        assertTrue(FlutterDesignerCanvasSession.class.isAssignableFrom(
                FlutterDesignerNativeCanvasSession.class));

        Set<String> methods = Arrays.stream(
                FlutterDesignerCanvasSession.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "component",
                "isSurfaceFocused",
                "releaseSurfaceFocus",
                "show",
                "hide",
                "requestFocus",
                "clearFocusRequest",
                "restart",
                "canRestart",
                "present",
                "withdraw",
                "selectWidget",
                "setViewportMetricsListener",
                "setInteractionListener",
                "setTextEditCommitListener",
                "setInteractionBarrierListener",
                "interactionBarrierState",
                "setViewportPresentation",
                "paletteCatalogInsertDropAvailable",
                "authorizePaletteDragSource",
                "showWidgetMovePreview",
                "clearWidgetMovePreview",
                "close"), methods);

        for (Method method : FlutterDesignerCanvasSession.class
                .getDeclaredMethods()) {
            assertBackendNeutral(method.getReturnType(), method.toString());
            for (Class<?> parameter : method.getParameterTypes()) {
                assertBackendNeutral(parameter, method.toString());
            }
        }
    }

    @Test
    void nativeAdapterDelegatesSurfaceAndFocusToItsOwnedHost() throws Exception {
        TestHost host = new TestHost();
        FlutterDesignerNativeCanvasSession session = onEdt(() ->
                new FlutterDesignerNativeCanvasSession(
                        new WindowsNativeCanvasPlatformProvider(),
                        host,
                        inertRuntime(),
                        ignored -> { },
                        ignored -> { },
                        ignored -> Optional.empty(),
                        ignored -> { },
                        ignored -> { }));
        try {
            onEdt(() -> {
                assertSame(host.component, session.component());
                assertTrue(session.isSurfaceFocused());
                assertTrue(session.releaseSurfaceFocus());
                return null;
            });
            assertEquals(1, host.focusInspectionCalls);
            assertEquals(1, host.focusReleaseCalls);
        } finally {
            onEdt(() -> {
                session.close();
                return null;
            });
        }
        assertEquals(1, host.closeCalls);
    }

    @Test
    void sharedInteractionTypesRetainTheirAdmissionRules() {
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(
                        new CanvasRevisionKey(
                                CanvasSessionId.parse(
                                        "80ef60ed-b108-4674-99a6-c1f3102f01ab"),
                                7,
                                StableId.parse(
                                        "a4b202a7-060a-4d07-ba52-54340b6a80ea"),
                                11),
                        3),
                5);

        var synchronizedState =
                new FlutterDesignerCanvasSession.InteractionBarrierState(
                        FlutterDesignerCanvasSession.InteractionBarrierPhase
                                .SYNCHRONIZED,
                        9,
                        Optional.of(layout));
        assertTrue(synchronizedState.inputEnabled());
        assertFalse(new FlutterDesignerCanvasSession.InteractionBarrierState(
                FlutterDesignerCanvasSession.InteractionBarrierPhase.INACTIVE,
                0,
                Optional.empty()).inputEnabled());

        assertThrows(IllegalArgumentException.class, () ->
                new FlutterDesignerCanvasSession.InteractionBarrierState(
                        FlutterDesignerCanvasSession.InteractionBarrierPhase
                                .SYNCHRONIZING,
                        -1,
                        Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                new FlutterDesignerCanvasSession.InteractionBarrierState(
                        FlutterDesignerCanvasSession.InteractionBarrierPhase
                                .SYNCHRONIZING,
                        CanvasWireProtocol.MAX_SEQUENCE + 1,
                        Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                new FlutterDesignerCanvasSession.InteractionBarrierState(
                        FlutterDesignerCanvasSession.InteractionBarrierPhase
                                .SYNCHRONIZED,
                        1,
                        Optional.empty()));
        assertThrows(NullPointerException.class, () ->
                new FlutterDesignerCanvasSession.AdmittedPaletteDrop(null, null));

        Set<String> nativeNestedTypes = Arrays.stream(
                FlutterDesignerNativeCanvasSession.class.getDeclaredClasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertFalse(nativeNestedTypes.contains("InteractionBarrierPhase"));
        assertFalse(nativeNestedTypes.contains("InteractionBarrierState"));
        assertFalse(nativeNestedTypes.contains("AdmittedPaletteDrop"));
    }

    private static void assertBackendNeutral(Class<?> type, String method) {
        String name = type.getName();
        assertFalse(Process.class.isAssignableFrom(type)
                || name.contains(".canvas.spi.NativeCanvas")
                || name.contains("NativeCanvas"),
                () -> "Backend-specific type " + name + " leaked through " + method);
    }

    private static FlutterDesignerNativeCanvasSession.RuntimeServices inertRuntime() {
        return new FlutterDesignerNativeCanvasSession.RuntimeServices(
                () -> new FlutterDesignerNativeCanvasSession.SdkResolution(
                        null, "not used by the contract test"),
                ignored -> CompletableFuture.failedFuture(
                        new AssertionError("build must not start")),
                (command, workingDirectory) -> {
                    throw new AssertionError("process must not start");
                },
                Runnable::run,
                Runnable::run,
                ignored -> () -> { },
                ignored -> CompletableFuture.completedFuture(null),
                System::nanoTime);
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

    private static final class TestHost implements NativeCanvasHost {
        private final JPanel component = new JPanel();
        private int focusInspectionCalls;
        private int focusReleaseCalls;
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
        public boolean releaseRunnerFocus() {
            focusReleaseCalls++;
            return true;
        }

        @Override
        public boolean isRunnerFocused() {
            focusInspectionCalls++;
            return true;
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
