package dev.flutter.netbeans.designer.canvas.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CanvasWireSessionGateTest {
    private static final CanvasSessionId SESSION = CanvasSessionId.parse(
            "80ef60ed-b108-4674-99a6-c1f3102f01ab");
    private static final CanvasSessionId OTHER_SESSION = CanvasSessionId.parse(
            "0d236d13-7048-4bc6-a11f-bf03a85d1724");
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework", "engine", "3.12.0");
    private static final CanvasWireHandshakeLimits LIMITS =
            CanvasWireHandshakeLimits.defaults();

    @Test
    void followsTheExactHappyPathAndNegotiatesOnlyARequestedSubset() {
        CanvasWireSessionGate gate = gate(
                List.of(
                        CanvasWireCapability.READ_ONLY_RENDER,
                        CanvasWireCapability.READ_ONLY_LAYOUT));

        CanvasHostHello hello = gate.start();
        assertEquals(0, hello.sequence());
        assertEquals(CanvasWireSessionState.HELLO_SENT, gate.state());

        CanvasWireSessionAdmission admitted = gate.admit(new CanvasRunnerHello(
                SESSION,
                0,
                hello.sequence(),
                "0.1.3",
                ENGINE,
                List.of(CanvasWireCapability.READ_ONLY_RENDER),
                tighterLimits()));
        assertTrue(admitted.accepted());
        assertEquals(CanvasWireSessionState.READY, gate.state());
        CanvasWireNegotiation negotiation = gate.negotiation().orElseThrow();
        assertEquals(ENGINE, negotiation.engineIdentity());
        assertEquals(List.of(CanvasWireCapability.READ_ONLY_RENDER),
                negotiation.acceptedCapabilities());
        assertEquals(tighterLimits(), negotiation.effectiveLimits());

        CanvasHostClose close = gate.beginClose(CanvasWireCloseReason.FORM_CLOSED);
        assertEquals(1, close.sequence());
        assertEquals(CanvasWireSessionState.CLOSING, gate.state());
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerClosed(SESSION, 1, close.sequence())));
        assertEquals(CanvasWireSessionState.CLOSED, gate.state());
        assertEquals(CanvasWireSessionAdmission.CLOSED,
                gate.admit(new CanvasRunnerClosed(SESSION, 2, close.sequence())));
    }

    @Test
    void rejectsWrongDirectionOrderSessionSequenceAndReplyAsFatal() {
        assertFatal(
                gate(List.of(CanvasWireCapability.READ_ONLY_RENDER)),
                new CanvasRunnerHello(
                        SESSION, 0, 0, "0.1.3", ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER), LIMITS),
                CanvasWireSessionAdmission.WRONG_STATE);

        CanvasWireSessionGate wrongDirection = startedGate();
        assertEquals(CanvasWireSessionAdmission.WRONG_DIRECTION,
                wrongDirection.admit(new CanvasHostClose(
                        SESSION, 1, CanvasWireCloseReason.RESTART)));
        assertEquals(CanvasWireSessionState.FAILED, wrongDirection.state());

        CanvasWireSessionGate foreign = startedGate();
        assertEquals(CanvasWireSessionAdmission.STALE_SESSION,
                foreign.admit(new CanvasRunnerHello(
                        OTHER_SESSION, 0, 0, "0.1.3", ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER), LIMITS)));
        assertEquals(CanvasWireSessionState.FAILED, foreign.state());

        CanvasWireSessionGate jumped = startedGate();
        assertEquals(CanvasWireSessionAdmission.NONCONTIGUOUS_SEQUENCE,
                jumped.admit(new CanvasRunnerFailure(
                        SESSION,
                        CanvasWireProtocol.MAX_SEQUENCE,
                        OptionalLong.of(0),
                        CanvasWireFailureCode.INTERNAL_FAILURE,
                        true,
                        "Runner failed.")));
        assertEquals(CanvasWireSessionState.FAILED, jumped.state());

        CanvasWireSessionGate wrongReply = startedGate();
        assertEquals(CanvasWireSessionAdmission.WRONG_REPLY,
                wrongReply.admit(new CanvasRunnerHello(
                        SESSION, 0, 1, "0.1.3", ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER), LIMITS)));
        assertEquals(CanvasWireSessionState.FAILED, wrongReply.state());
    }

    @Test
    void rejectsCapabilityAndLimitEscalation() {
        CanvasWireSessionGate capabilities = gate(
                List.of(CanvasWireCapability.READ_ONLY_RENDER));
        capabilities.start();
        assertEquals(CanvasWireSessionAdmission.CAPABILITY_ESCALATION,
                capabilities.admit(new CanvasRunnerHello(
                        SESSION,
                        0,
                        0,
                        "0.1.3",
                        ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_LAYOUT),
                        LIMITS)));
        assertEquals(CanvasWireSessionState.FAILED, capabilities.state());

        CanvasWireSessionGate limits = new CanvasWireSessionGate(
                SESSION,
                "0.1.3",
                List.of(CanvasWireCapability.READ_ONLY_RENDER),
                tighterLimits());
        limits.start();
        assertEquals(CanvasWireSessionAdmission.LIMIT_ESCALATION,
                limits.admit(new CanvasRunnerHello(
                        SESSION,
                        0,
                        0,
                        "0.1.3",
                        ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER),
                        LIMITS)));
        assertEquals(CanvasWireSessionState.FAILED, limits.state());
    }

    @Test
    void acceptsOnlyFatalFailureBeforeReady() {
        CanvasWireSessionGate fatal = startedGate();
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                fatal.admit(new CanvasRunnerFailure(
                        SESSION,
                        0,
                        OptionalLong.of(0),
                        CanvasWireFailureCode.RUNNER_START_FAILED,
                        true,
                        "Runner did not start.")));
        assertEquals(CanvasWireSessionState.FAILED, fatal.state());

        CanvasWireSessionGate nonFatal = startedGate();
        assertEquals(CanvasWireSessionAdmission.NON_FATAL_STARTUP_FAILURE,
                nonFatal.admit(new CanvasRunnerFailure(
                        SESSION,
                        0,
                        OptionalLong.of(0),
                        CanvasWireFailureCode.RUNNER_START_FAILED,
                        false,
                        "Runner is not ready.")));
        assertEquals(CanvasWireSessionState.FAILED, nonFatal.state());
    }

    @Test
    void acceptsBoundedReadyFailuresAndFatalFailureEndsTheSession() {
        CanvasWireSessionGate gate = startedGate();
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerHello(
                        SESSION,
                        0,
                        0,
                        "0.1.3",
                        ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER),
                        LIMITS)));

        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerFailure(
                        SESSION,
                        1,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INVALID_REQUEST,
                        false,
                        "Render request was rejected.")));
        assertEquals(CanvasWireSessionState.READY, gate.state());
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerFailure(
                        SESSION,
                        2,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INTERNAL_FAILURE,
                        true,
                        "Runner stopped.")));
        assertEquals(CanvasWireSessionState.FAILED, gate.state());
    }

    @Test
    void closeDuringStartupUsesRunnerSequenceZeroAndCloseReply() {
        CanvasWireSessionGate gate = startedGate();
        CanvasHostClose close = gate.beginClose(CanvasWireCloseReason.FORM_CLOSED);

        assertEquals(1, close.sequence());
        assertEquals(CanvasWireSessionState.CLOSING, gate.state());
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerClosed(
                        SESSION, 0, close.sequence())));
        assertEquals(CanvasWireSessionState.CLOSED, gate.state());
    }

    @Test
    void closeDuringStartupConsumesAnAlreadyInFlightHelloBeforeClosed() {
        CanvasWireSessionGate gate = startedGate();
        CanvasHostClose close = gate.beginClose(CanvasWireCloseReason.FORM_CLOSED);

        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerHello(
                        SESSION,
                        0,
                        0,
                        "0.1.3",
                        ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER),
                        LIMITS)));
        assertEquals(CanvasWireSessionState.CLOSING, gate.state());
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerClosed(
                        SESSION, 1, close.sequence())));
        assertEquals(CanvasWireSessionState.CLOSED, gate.state());
    }

    @Test
    void closeAfterReadyConsumesQueuedNonFatalFailureBeforeClosed() {
        CanvasWireSessionGate gate = startedGate();
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerHello(
                        SESSION,
                        0,
                        0,
                        "0.1.3",
                        ENGINE,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER),
                        LIMITS)));
        CanvasHostClose close = gate.beginClose(CanvasWireCloseReason.FORM_CLOSED);

        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerFailure(
                        SESSION,
                        1,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INVALID_REQUEST,
                        false,
                        "Queued render request was rejected.")));
        assertEquals(CanvasWireSessionState.CLOSING, gate.state());
        assertEquals(CanvasWireSessionAdmission.ACCEPTED,
                gate.admit(new CanvasRunnerClosed(
                        SESSION, 2, close.sequence())));
        assertEquals(CanvasWireSessionState.CLOSED, gate.state());
    }

    @Test
    void localOperationsAreSingleUseAndStateBound() {
        CanvasWireSessionGate gate = gate(List.of());
        gate.start();
        assertThrows(IllegalStateException.class, gate::start);
        gate.beginClose(CanvasWireCloseReason.FORM_CLOSED);
        assertThrows(IllegalStateException.class,
                () -> gate.beginClose(CanvasWireCloseReason.FORM_CLOSED));
    }

    private static CanvasWireSessionGate startedGate() {
        CanvasWireSessionGate gate = gate(
                List.of(CanvasWireCapability.READ_ONLY_RENDER));
        gate.start();
        return gate;
    }

    private static CanvasWireSessionGate gate(
            List<CanvasWireCapability> capabilities) {
        return new CanvasWireSessionGate(
                SESSION, "0.1.3", capabilities, LIMITS);
    }

    private static CanvasWireHandshakeLimits tighterLimits() {
        return new CanvasWireHandshakeLimits(
                CanvasWireHandshakeLimits.MAX_CONTROL_MESSAGE_BYTES / 2,
                CanvasWireHandshakeLimits.MAX_MODEL_BYTES / 2,
                CanvasWireHandshakeLimits.MAX_CATALOG_BYTES / 2,
                CanvasWireHandshakeLimits.MAX_LAYOUT_BYTES / 2,
                CanvasWireHandshakeLimits.MAX_ENCODED_IMAGE_BYTES / 2,
                CanvasWireHandshakeLimits.MAX_PHYSICAL_DIMENSION / 2,
                CanvasWireHandshakeLimits.MAX_PHYSICAL_PIXELS / 2);
    }

    private static void assertFatal(
            CanvasWireSessionGate gate,
            CanvasWireMessage message,
            CanvasWireSessionAdmission expected) {
        assertEquals(expected, gate.admit(message));
        assertEquals(CanvasWireSessionState.FAILED, gate.state());
    }
}
