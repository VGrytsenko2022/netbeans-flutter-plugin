package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe pure host-side lifecycle and negotiation gate.
 *
 * <p>The gate issues host sequence zero for {@code host.hello} and sequence one
 * for {@code host.close}. Runner sequences must start at zero and advance by
 * exactly one. A foreign, reordered or capability/limit-escalating runner
 * message fails the session without adopting the untrusted sequence.</p>
 */
public final class CanvasWireSessionGate {
    private final CanvasSessionId sessionId;
    private final String hostVersion;
    private final List<CanvasWireCapability> requestedCapabilities;
    private final EnumSet<CanvasWireCapability> requestedCapabilitySet;
    private final CanvasWireHandshakeLimits offeredLimits;

    private CanvasWireSessionState state = CanvasWireSessionState.NEW;
    private long expectedRunnerSequence;
    private long closeSequence = -1;
    private boolean runnerHelloAccepted;
    private CanvasWireNegotiation negotiation;

    public CanvasWireSessionGate(
            CanvasSessionId sessionId,
            String hostVersion,
            Collection<CanvasWireCapability> requestedCapabilities,
            CanvasWireHandshakeLimits offeredLimits) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.hostVersion = CanvasWireValues.printableText(
                hostVersion, "hostVersion", CanvasWireValues.MAX_VERSION_CODE_POINTS);
        this.requestedCapabilities = CanvasWireValues.capabilities(
                requestedCapabilities, "requestedCapabilities");
        this.requestedCapabilitySet = this.requestedCapabilities.isEmpty()
                ? EnumSet.noneOf(CanvasWireCapability.class)
                : EnumSet.copyOf(this.requestedCapabilities);
        this.offeredLimits = Objects.requireNonNull(offeredLimits, "offeredLimits");
    }

    /** Issues the only legal first host message. */
    public synchronized CanvasHostHello start() {
        requireState(CanvasWireSessionState.NEW, "start");
        CanvasHostHello hello = new CanvasHostHello(
                sessionId,
                0,
                hostVersion,
                requestedCapabilities,
                offeredLimits);
        state = CanvasWireSessionState.HELLO_SENT;
        return hello;
    }

    /** Issues the close request during startup or after successful negotiation. */
    public synchronized CanvasHostClose beginClose(CanvasWireCloseReason reason) {
        if (state != CanvasWireSessionState.HELLO_SENT
                && state != CanvasWireSessionState.READY) {
            throw new IllegalStateException(
                    "Cannot beginClose Canvas wire session while state is " + state);
        }
        closeSequence = 1;
        CanvasHostClose close = new CanvasHostClose(
                sessionId, closeSequence, Objects.requireNonNull(reason, "reason"));
        state = CanvasWireSessionState.CLOSING;
        return close;
    }

    /** Admits one decoded runner message and advances only on an exact match. */
    public synchronized CanvasWireSessionAdmission admit(CanvasWireMessage message) {
        Objects.requireNonNull(message, "message");
        if (state == CanvasWireSessionState.CLOSED) {
            return CanvasWireSessionAdmission.CLOSED;
        }
        if (state == CanvasWireSessionState.FAILED) {
            return CanvasWireSessionAdmission.FAILED;
        }
        if (message instanceof CanvasHostHello || message instanceof CanvasHostClose) {
            return fail(CanvasWireSessionAdmission.WRONG_DIRECTION);
        }
        if (!sessionId.equals(message.sessionId())) {
            return fail(CanvasWireSessionAdmission.STALE_SESSION);
        }
        if (message.sequence() != expectedRunnerSequence) {
            return fail(CanvasWireSessionAdmission.NONCONTIGUOUS_SEQUENCE);
        }

        if (message instanceof CanvasRunnerHello hello) {
            return admitHello(hello);
        }
        if (message instanceof CanvasRunnerClosed closed) {
            return admitClosed(closed);
        }
        if (message instanceof CanvasRunnerFailure failure) {
            return admitFailure(failure);
        }
        return fail(CanvasWireSessionAdmission.WRONG_DIRECTION);
    }

    public synchronized CanvasWireSessionState state() {
        return state;
    }

    public CanvasSessionId sessionId() {
        return sessionId;
    }

    /**
     * Returns the exact accepted handshake, when one was admitted.
     *
     * <p>After this becomes present, the concrete transport must tighten its
     * framing and codec policy to the returned effective limits.</p>
     */
    public synchronized Optional<CanvasWireNegotiation> negotiation() {
        return Optional.ofNullable(negotiation);
    }

    private CanvasWireSessionAdmission admitHello(CanvasRunnerHello hello) {
        boolean closingDuringHandshake = state == CanvasWireSessionState.CLOSING
                && !runnerHelloAccepted;
        if (state != CanvasWireSessionState.HELLO_SENT
                && !closingDuringHandshake) {
            return fail(CanvasWireSessionAdmission.WRONG_STATE);
        }
        if (hello.repliedTo() != 0) {
            return fail(CanvasWireSessionAdmission.WRONG_REPLY);
        }
        if (!requestedCapabilitySet.containsAll(hello.acceptedCapabilities())) {
            return fail(CanvasWireSessionAdmission.CAPABILITY_ESCALATION);
        }
        if (!within(hello.effectiveLimits(), offeredLimits)) {
            return fail(CanvasWireSessionAdmission.LIMIT_ESCALATION);
        }
        expectedRunnerSequence++;
        runnerHelloAccepted = true;
        negotiation = new CanvasWireNegotiation(
                hello.runnerVersion(),
                hello.engineIdentity(),
                hello.acceptedCapabilities(),
                hello.effectiveLimits());
        if (!closingDuringHandshake) {
            state = CanvasWireSessionState.READY;
        }
        return CanvasWireSessionAdmission.ACCEPTED;
    }

    private CanvasWireSessionAdmission admitClosed(CanvasRunnerClosed closed) {
        if (state != CanvasWireSessionState.CLOSING) {
            return fail(CanvasWireSessionAdmission.WRONG_STATE);
        }
        if (closed.repliedTo() != closeSequence) {
            return fail(CanvasWireSessionAdmission.WRONG_REPLY);
        }
        expectedRunnerSequence++;
        state = CanvasWireSessionState.CLOSED;
        return CanvasWireSessionAdmission.ACCEPTED;
    }

    private CanvasWireSessionAdmission admitFailure(CanvasRunnerFailure failure) {
        boolean startupFailure = state == CanvasWireSessionState.HELLO_SENT
                || (state == CanvasWireSessionState.CLOSING
                        && !runnerHelloAccepted);
        if (startupFailure) {
            if (!failure.fatal()) {
                return fail(CanvasWireSessionAdmission.NON_FATAL_STARTUP_FAILURE);
            }
            if (failure.replyTo().isPresent()
                    && failure.replyTo().orElseThrow() != 0) {
                return fail(CanvasWireSessionAdmission.WRONG_REPLY);
            }
            expectedRunnerSequence++;
            state = CanvasWireSessionState.FAILED;
            return CanvasWireSessionAdmission.ACCEPTED;
        }
        boolean readyFailure = state == CanvasWireSessionState.READY
                || (state == CanvasWireSessionState.CLOSING
                        && runnerHelloAccepted);
        if (readyFailure) {
            // This first lifecycle slice has no outstanding post-hello host
            // request to correlate. Future render request gates validate their
            // own reply ids before delegating an event-style failure here.
            if (failure.replyTo().isPresent()) {
                return fail(CanvasWireSessionAdmission.WRONG_REPLY);
            }
            expectedRunnerSequence++;
            if (failure.fatal()) {
                state = CanvasWireSessionState.FAILED;
            }
            return CanvasWireSessionAdmission.ACCEPTED;
        }
        return fail(CanvasWireSessionAdmission.WRONG_STATE);
    }

    private CanvasWireSessionAdmission fail(CanvasWireSessionAdmission admission) {
        state = CanvasWireSessionState.FAILED;
        return admission;
    }

    private void requireState(CanvasWireSessionState expected, String operation) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Cannot " + operation + " Canvas wire session while state is " + state);
        }
    }

    private static boolean within(
            CanvasWireHandshakeLimits effective,
            CanvasWireHandshakeLimits offered) {
        return effective.maxControlMessageBytes() <= offered.maxControlMessageBytes()
                && effective.maxModelBytes() <= offered.maxModelBytes()
                && effective.maxCatalogBytes() <= offered.maxCatalogBytes()
                && effective.maxLayoutBytes() <= offered.maxLayoutBytes()
                && effective.maxEncodedImageBytes() <= offered.maxEncodedImageBytes()
                && effective.maxPhysicalDimension() <= offered.maxPhysicalDimension()
                && effective.maxPhysicalPixels() <= offered.maxPhysicalPixels();
    }
}
