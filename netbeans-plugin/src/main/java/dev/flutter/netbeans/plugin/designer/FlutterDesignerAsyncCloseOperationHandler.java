package dev.flutter.netbeans.plugin.designer;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Action;
import org.netbeans.core.spi.multiview.CloseOperationHandler;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Non-blocking close policy for a MultiView that owns asynchronous native
 * peers. The first close request is always vetoed while Canvas owners retire;
 * a marker action schedules a fresh TopComponent close only after the peer-safe
 * barrier has completed.
 *
 * <p>This handler is intentionally not installed by the production selector
 * yet. NetBeans split/clear-split can reparent a MultiView without consulting
 * {@code canCloseElement()}, and closing a non-last clone bypasses the
 * last-clone handler. Exact Web remains default-off until every physical-peer
 * path has the same proven veto.</p>
 */
final class FlutterDesignerAsyncCloseOperationHandler
        implements CloseOperationHandler {
    static final String PENDING_ID =
            "flutter-designer.canvas-release.pending";
    static final String READY_SAVE_ID =
            "flutter-designer.canvas-release.ready-save";
    static final String READY_DISCARD_ID =
            "flutter-designer.canvas-release.ready-discard";

    private final DecisionPrompt decisionPrompt;

    FlutterDesignerAsyncCloseOperationHandler() {
        this(FlutterDesignerAsyncCloseOperationHandler::showDecision);
    }

    FlutterDesignerAsyncCloseOperationHandler(DecisionPrompt decisionPrompt) {
        this.decisionPrompt = Objects.requireNonNull(
                decisionPrompt, "decisionPrompt");
    }

    static CloseOperationState canvasState(
            GatePhase phase,
            Action proceedAction,
            Action discardAction) {
        Objects.requireNonNull(phase, "phase");
        return MultiViewFactory.createUnsafeCloseState(
                switch (phase) {
                    case PENDING -> PENDING_ID;
                    case READY_SAVE -> READY_SAVE_ID;
                    case READY_DISCARD -> READY_DISCARD_ID;
                },
                Objects.requireNonNull(proceedAction, "proceedAction"),
                Objects.requireNonNull(discardAction, "discardAction"));
    }

    @Override
    public boolean resolveCloseOperation(CloseOperationState[] elements) {
        List<CloseOperationState> canvas = new ArrayList<>();
        LinkedHashMap<String, CloseOperationState> ordinary =
                new LinkedHashMap<>();
        if (elements != null) {
            for (CloseOperationState state : elements) {
                if (state == null || state.canClose()) {
                    continue;
                }
                if (isCanvasState(state)) {
                    canvas.add(state);
                } else {
                    ordinary.put(state.getCloseWarningID(), state);
                }
            }
        }
        if (canvas.isEmpty()) {
            return resolveOrdinary(ordinary);
        }

        boolean allReady = canvas.stream().allMatch(
                FlutterDesignerAsyncCloseOperationHandler::isReadyState);
        boolean discardAuthorized = canvas.stream().anyMatch(state ->
                READY_DISCARD_ID.equals(state.getCloseWarningID()));
        if (allReady && ordinary.isEmpty()) {
            return true;
        }

        // A previously authorized Canvas discard never authorizes later edits
        // reported by another MultiView element. Prompt for every current
        // ordinary unsafe state and let the ready Canvas action schedule one
        // fresh close after those actions have run.
        Decision decision = ordinary.isEmpty()
                ? (discardAuthorized ? Decision.DISCARD : Decision.SAVE)
                : decisionPrompt.choose(snapshot(ordinary));
        if (decision == Decision.CANCEL) {
            return false;
        }
        invoke(ordinary.values(), decision);
        invoke(canvas, decision);
        // Canvas actions either begin retirement or schedule a fresh close for
        // a previously ready gate. Never allow this close stack to remove the
        // peer synchronously.
        return false;
    }

    private boolean resolveOrdinary(
            LinkedHashMap<String, CloseOperationState> ordinary) {
        if (ordinary.isEmpty()) {
            return true;
        }
        Decision decision = decisionPrompt.choose(snapshot(ordinary));
        if (decision == Decision.CANCEL) {
            return false;
        }
        invoke(ordinary.values(), decision);
        // A custom handler cannot use NetBeans' private DefaultCloseHandler
        // recheck marker. Fail closed instead of assuming that Save/Discard
        // cleared every state; this dormant handler is installed only together
        // with a Canvas retry path that can request a fresh close.
        return false;
    }

    private static Map<String, CloseOperationState> snapshot(
            LinkedHashMap<String, CloseOperationState> states) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(states));
    }

    private static void invoke(
            Iterable<CloseOperationState> states,
            Decision decision) {
        for (CloseOperationState state : states) {
            Action action = decision == Decision.DISCARD
                    ? state.getDiscardAction()
                    : state.getProceedAction();
            if (action != null) {
                action.actionPerformed(new ActionEvent(
                        state,
                        ActionEvent.ACTION_PERFORMED,
                        decision == Decision.DISCARD ? "discard" : "proceed"));
            }
        }
    }

    private static boolean isCanvasState(CloseOperationState state) {
        String id = state.getCloseWarningID();
        return PENDING_ID.equals(id)
                || READY_SAVE_ID.equals(id)
                || READY_DISCARD_ID.equals(id);
    }

    private static boolean isReadyState(CloseOperationState state) {
        return READY_SAVE_ID.equals(state.getCloseWarningID())
                || READY_DISCARD_ID.equals(state.getCloseWarningID());
    }

    private static Decision showDecision(
            Map<String, CloseOperationState> states) {
        Object message;
        if (states.size() == 1) {
            message = description(states.values().iterator().next());
        } else {
            StringBuilder combined = new StringBuilder();
            for (CloseOperationState state : states.values()) {
                if (!combined.isEmpty()) {
                    combined.append(' ');
                }
                combined.append(description(state));
            }
            message = combined.toString();
        }
        NotifyDescriptor descriptor = new NotifyDescriptor.Confirmation(
                message,
                NotifyDescriptor.YES_NO_CANCEL_OPTION);
        Object save = "Save";
        Object discard = "Discard";
        Object cancel = NotifyDescriptor.CANCEL_OPTION;
        descriptor.setOptions(new Object[] {save, discard, cancel});
        Object selected = DialogDisplayer.getDefault().notify(descriptor);
        if (selected == save) {
            return Decision.SAVE;
        }
        if (selected == discard) {
            return Decision.DISCARD;
        }
        return Decision.CANCEL;
    }

    private static Object description(CloseOperationState state) {
        Action action = state.getProceedAction();
        Object description = action == null
                ? null : action.getValue(Action.LONG_DESCRIPTION);
        if (description == null && action != null) {
            description = action.getValue(Action.SHORT_DESCRIPTION);
        }
        if (description == null && action != null) {
            description = action.getValue(Action.NAME);
        }
        return description == null
                ? state.getCloseWarningID()
                : description;
    }

    enum GatePhase {
        PENDING,
        READY_SAVE,
        READY_DISCARD
    }

    enum Decision {
        SAVE,
        DISCARD,
        CANCEL
    }

    @FunctionalInterface
    interface DecisionPrompt {
        Decision choose(Map<String, CloseOperationState> states);
    }
}
