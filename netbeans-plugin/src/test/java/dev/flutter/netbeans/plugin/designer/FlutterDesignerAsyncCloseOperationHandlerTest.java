package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.flutter.netbeans.plugin.designer.FlutterDesignerAsyncCloseOperationHandler.Decision;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerAsyncCloseOperationHandler.GatePhase;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.junit.jupiter.api.Test;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewFactory;

class FlutterDesignerAsyncCloseOperationHandlerTest {

    @Test
    void pendingCanvasGateVetoesCloseAndStartsRetirement() {
        ActionProbe canvas = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler = handlerThatMustNotPrompt();

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            canvasState(GatePhase.PENDING, canvas)
        });

        assertFalse(resolved);
        assertEquals(1, canvas.proceedCalls());
        assertEquals(0, canvas.discardCalls());
    }

    @Test
    void readySaveAllowsCloseWhenItIsTheOnlyUnsafeState() {
        ActionProbe canvas = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler = handlerThatMustNotPrompt();

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            canvasState(GatePhase.READY_SAVE, canvas)
        });

        assertTrue(resolved);
        assertEquals(0, canvas.proceedCalls());
        assertEquals(0, canvas.discardCalls());
    }

    @Test
    void readySaveDoesNotBypassAnOrdinaryUnsafeState() {
        ActionProbe canvas = new ActionProbe();
        ActionProbe ordinary = new ActionProbe();
        AtomicInteger promptCalls = new AtomicInteger();
        FlutterDesignerAsyncCloseOperationHandler handler =
                new FlutterDesignerAsyncCloseOperationHandler(states -> {
                    promptCalls.incrementAndGet();
                    assertEquals(1, states.size());
                    assertTrue(states.containsKey("ordinary-dirty"));
                    return Decision.SAVE;
                });

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            canvasState(GatePhase.READY_SAVE, canvas),
            ordinaryState("ordinary-dirty", ordinary)
        });

        assertFalse(resolved);
        assertEquals(1, promptCalls.get());
        assertEquals(1, ordinary.proceedCalls());
        assertEquals(0, ordinary.discardCalls());
        assertEquals(1, canvas.proceedCalls());
        assertEquals(0, canvas.discardCalls());
    }

    @Test
    void readyDiscardDoesNotAuthorizeAConcurrentOrdinaryUnsafeState() {
        ActionProbe canvas = new ActionProbe();
        ActionProbe ordinary = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler =
                handlerReturning(Decision.SAVE);

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            canvasState(GatePhase.READY_DISCARD, canvas),
            ordinaryState("ordinary-dirty", ordinary)
        });

        assertFalse(resolved);
        assertEquals(1, canvas.proceedCalls());
        assertEquals(0, canvas.discardCalls());
        assertEquals(1, ordinary.proceedCalls());
        assertEquals(0, ordinary.discardCalls());
    }

    @Test
    void ordinarySaveFailsClosedUntilAReplacementCloseRechecksState() {
        ActionProbe ordinary = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler = handlerReturning(Decision.SAVE);

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            ordinaryState("ordinary-dirty", ordinary)
        });

        assertFalse(resolved);
        assertEquals(1, ordinary.proceedCalls());
        assertEquals(0, ordinary.discardCalls());
    }

    @Test
    void ordinaryDiscardFailsClosedUntilAReplacementCloseRechecksState() {
        ActionProbe ordinary = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler = handlerReturning(Decision.DISCARD);

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            ordinaryState("ordinary-dirty", ordinary)
        });

        assertFalse(resolved);
        assertEquals(0, ordinary.proceedCalls());
        assertEquals(1, ordinary.discardCalls());
    }

    @Test
    void cancelDecisionLeavesOrdinaryActionsUntouchedAndVetoesClose() {
        ActionProbe ordinary = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler = handlerReturning(Decision.CANCEL);

        boolean resolved = handler.resolveCloseOperation(new CloseOperationState[] {
            ordinaryState("ordinary-dirty", ordinary)
        });

        assertFalse(resolved);
        assertEquals(0, ordinary.proceedCalls());
        assertEquals(0, ordinary.discardCalls());
    }

    @Test
    void multipleCanvasGatesAllRunAndVetoUntilEveryGateIsReady() {
        ActionProbe pending = new ActionProbe();
        ActionProbe ready = new ActionProbe();
        FlutterDesignerAsyncCloseOperationHandler handler = handlerThatMustNotPrompt();

        boolean pendingResolved = handler.resolveCloseOperation(new CloseOperationState[] {
            canvasState(GatePhase.PENDING, pending),
            canvasState(GatePhase.READY_SAVE, ready)
        });

        assertFalse(pendingResolved);
        assertEquals(1, pending.proceedCalls());
        assertEquals(1, ready.proceedCalls());
        assertEquals(0, pending.discardCalls());
        assertEquals(0, ready.discardCalls());

        ActionProbe firstReady = new ActionProbe();
        ActionProbe secondReady = new ActionProbe();
        boolean readyResolved = handler.resolveCloseOperation(new CloseOperationState[] {
            canvasState(GatePhase.READY_SAVE, firstReady),
            canvasState(GatePhase.READY_SAVE, secondReady)
        });

        assertTrue(readyResolved);
        assertEquals(0, firstReady.proceedCalls());
        assertEquals(0, secondReady.proceedCalls());
    }

    private static FlutterDesignerAsyncCloseOperationHandler handlerReturning(
            Decision decision) {
        return new FlutterDesignerAsyncCloseOperationHandler(
                ignoredStates -> decision);
    }

    private static FlutterDesignerAsyncCloseOperationHandler handlerThatMustNotPrompt() {
        return new FlutterDesignerAsyncCloseOperationHandler(
                ignoredStates -> {
                    fail("the close policy must not prompt for this state set");
                    return Decision.CANCEL;
                });
    }

    private static CloseOperationState canvasState(
            GatePhase phase,
            ActionProbe probe) {
        return FlutterDesignerAsyncCloseOperationHandler.canvasState(
                phase, probe.proceedAction(), probe.discardAction());
    }

    private static CloseOperationState ordinaryState(
            String warningId,
            ActionProbe probe) {
        return MultiViewFactory.createUnsafeCloseState(
                warningId, probe.proceedAction(), probe.discardAction());
    }

    private static final class ActionProbe {
        private final AtomicInteger proceedCalls = new AtomicInteger();
        private final AtomicInteger discardCalls = new AtomicInteger();

        Action proceedAction() {
            return action(proceedCalls);
        }

        Action discardAction() {
            return action(discardCalls);
        }

        int proceedCalls() {
            return proceedCalls.get();
        }

        int discardCalls() {
            return discardCalls.get();
        }

        private static Action action(AtomicInteger calls) {
            return new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    calls.incrementAndGet();
                }
            };
        }
    }
}
