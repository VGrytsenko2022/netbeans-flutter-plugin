package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WindowsWebCanvasFocusControllerTest {
    private static final long PARENT = 10L;
    private static final long CONTROLLER_FOCUS = 20L;
    private static final long UNRELATED = 30L;
    private static final long CURRENT_PROCESS = 77L;
    private static final long BROWSER_PROCESS = 88L;

    @Test
    void observesOnlyForegroundFocusInTheExactOwnedControllerSubtree() {
        NativeState state = new NativeState();
        WindowsWebCanvasFocusController controller = controller(state);

        assertTrue(controller.isControllerFocused(PARENT));

        state.foregroundWindow = PARENT;
        assertFalse(controller.isControllerFocused(PARENT),
                "the AWT parent itself is not controller focus");

        state.foregroundWindow = UNRELATED;
        state.windows.add(UNRELATED);
        state.identities.put(UNRELATED, identity(44L, BROWSER_PROCESS));
        assertFalse(controller.isControllerFocused(PARENT),
                "an unrelated foreground HWND cannot acquire Canvas authority");

        state.foregroundWindow = CONTROLLER_FOCUS;
        state.descendantProcessAllowed = false;
        assertFalse(controller.isControllerFocused(PARENT),
                "a foreign process cannot masquerade as a WebView2 descendant");

        state.descendantProcessAllowed = true;
        state.identities.put(PARENT, identity(11L, 99L));
        assertFalse(controller.isControllerFocused(PARENT),
                "the exact AWT parent must remain JVM-owned");
    }

    @Test
    void releasesVerifiedControllerFocusToTheExactParentAndDetachesInReverse() {
        NativeState state = new NativeState();
        WindowsWebCanvasFocusController controller = controller(state);

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult.RELEASED,
                controller.releaseControllerFocus(PARENT));

        assertEquals(PARENT, state.foregroundWindow);
        assertEquals(1, state.setFocusCalls);
        assertEquals(List.of(
                "33->11:true",
                "33->22:true",
                "33->22:false",
                "33->11:false"), state.inputAttachmentEdges);
        assertFalse(controller.isControllerFocused(PARENT));
    }

    @Test
    void unrelatedOrDriftedFocusIsNeverStolen() {
        NativeState unrelated = new NativeState();
        unrelated.windows.add(UNRELATED);
        unrelated.foregroundWindow = UNRELATED;
        unrelated.identities.put(UNRELATED, identity(44L, BROWSER_PROCESS));

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult.REFUSED,
                controller(unrelated).releaseControllerFocus(PARENT));
        assertEquals(0, unrelated.setFocusCalls);
        assertTrue(unrelated.inputAttachmentEdges.isEmpty());

        NativeState drifted = new NativeState();
        drifted.invalidateFocusAfterFirstAttach = true;

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult.REFUSED,
                controller(drifted).releaseControllerFocus(PARENT));
        assertEquals(0, drifted.setFocusCalls,
                "identity is revalidated after joining input queues");
        assertEquals(List.of("33->11:true", "33->11:false"),
                drifted.inputAttachmentEdges);
    }

    @Test
    void partialAttachAndEveryDetachPathFailClosedWithoutLeakingAQueueJoin() {
        NativeState partialAttach = new NativeState();
        partialAttach.refuseFocusThreadAttach = true;

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult.REFUSED,
                controller(partialAttach).releaseControllerFocus(PARENT));
        assertEquals(0, partialAttach.setFocusCalls);
        assertEquals(List.of(
                "33->11:true",
                "33->22:true",
                "33->11:false"), partialAttach.inputAttachmentEdges);

        NativeState failedDetach = new NativeState();
        failedDetach.detachSucceeds = false;

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult
                        .INPUT_QUEUE_DETACH_FAILED,
                controller(failedDetach).releaseControllerFocus(PARENT));
        assertEquals(1, failedDetach.setFocusCalls);
        assertEquals(List.of(
                "33->11:true",
                "33->22:true",
                "33->22:false",
                "33->11:false"), failedDetach.inputAttachmentEdges,
                "both detach attempts must run even when the first fails");
    }

    @Test
    void postDetachIdentityOrForegroundDriftInvalidatesAnApparentTransfer() {
        NativeState focusDrift = new NativeState();
        focusDrift.moveFocusAfterParentDetach = true;

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult.REFUSED,
                controller(focusDrift).releaseControllerFocus(PARENT));
        assertEquals(UNRELATED, focusDrift.foregroundWindow);

        NativeState parentDrift = new NativeState();
        parentDrift.invalidateParentAfterParentDetach = true;

        assertEquals(
                WindowsWebCanvasFocusController.FocusReleaseResult.REFUSED,
                controller(parentDrift).releaseControllerFocus(PARENT));
        assertEquals(1, parentDrift.setFocusCalls);
    }

    private static WindowsWebCanvasFocusController controller(NativeState state) {
        return new WindowsWebCanvasFocusController(
                state,
                () -> CURRENT_PROCESS,
                (currentProcess, candidateProcess) ->
                        currentProcess == CURRENT_PROCESS
                        && (candidateProcess == CURRENT_PROCESS
                        || (candidateProcess == BROWSER_PROCESS
                        && state.descendantProcessAllowed)));
    }

    private static WindowsWebCanvasFocusController.WindowIdentity identity(
            long threadId, long processId) {
        return new WindowsWebCanvasFocusController.WindowIdentity(
                threadId, processId);
    }

    private static final class NativeState
            implements WindowsWebCanvasFocusController.NativeApi {
        private final Set<Long> windows = new HashSet<>(Set.of(
                PARENT, CONTROLLER_FOCUS));
        private final java.util.Map<Long, WindowsWebCanvasFocusController.WindowIdentity>
                identities = new java.util.HashMap<>();
        private final List<String> inputAttachmentEdges = new ArrayList<>();
        private long foregroundWindow = CONTROLLER_FOCUS;
        private boolean descendantProcessAllowed = true;
        private boolean refuseFocusThreadAttach;
        private boolean detachSucceeds = true;
        private boolean invalidateFocusAfterFirstAttach;
        private boolean moveFocusAfterParentDetach;
        private boolean invalidateParentAfterParentDetach;
        private int attachCalls;
        private int setFocusCalls;

        private NativeState() {
            identities.put(PARENT, identity(11L, CURRENT_PROCESS));
            identities.put(CONTROLLER_FOCUS, identity(22L, BROWSER_PROCESS));
        }

        @Override
        public boolean isWindow(long window) {
            return windows.contains(window);
        }

        @Override
        public boolean isChild(long parentWindow, long childWindow) {
            return parentWindow == PARENT && childWindow == CONTROLLER_FOCUS;
        }

        @Override
        public WindowsWebCanvasFocusController.WindowIdentity windowIdentity(
                long window) {
            return identities.getOrDefault(window, identity(0L, 0L));
        }

        @Override
        public long foregroundFocusedWindow() {
            return foregroundWindow;
        }

        @Override
        public long currentThreadId() {
            return 33L;
        }

        @Override
        public boolean attachThreadInput(
                long sourceThread, long targetThread, boolean attach) {
            inputAttachmentEdges.add(
                    sourceThread + "->" + targetThread + ":" + attach);
            if (attach) {
                attachCalls++;
                if (invalidateFocusAfterFirstAttach && attachCalls == 1) {
                    identities.put(CONTROLLER_FOCUS, identity(22L, 99L));
                }
                if (refuseFocusThreadAttach && targetThread == 22L) {
                    return false;
                }
                return true;
            }
            if (targetThread == 11L) {
                if (moveFocusAfterParentDetach) {
                    windows.add(UNRELATED);
                    identities.put(UNRELATED, identity(44L, CURRENT_PROCESS));
                    foregroundWindow = UNRELATED;
                }
                if (invalidateParentAfterParentDetach) {
                    identities.put(PARENT, identity(11L, 99L));
                }
            }
            return detachSucceeds;
        }

        @Override
        public void setFocus(long window) {
            setFocusCalls++;
            foregroundWindow = window;
        }
    }
}
