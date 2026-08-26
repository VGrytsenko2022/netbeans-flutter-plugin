package dev.flutter.netbeans.designer.canvas.runtime;

import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import java.util.concurrent.CompletionStage;

/**
 * One isolated backend attempt, without any process, JSON or UI assumptions.
 *
 * <p>Logical {@link #close()} and physical {@link #termination()} are separate:
 * a process-backed implementation may need bounded escalation before the child
 * actually exits. Implementations must serialize or safely tolerate concurrent
 * method calls. Every method must return promptly; long-running work belongs to
 * its returned stage. {@code close()} must be idempotent and non-blocking, must
 * stop accepting new render work, and must initiate bounded termination escalation.
 * Every ready/render/termination stage must eventually complete after close;
 * the concrete process adapter is responsible for proving those timeouts.</p>
 */
public interface CanvasBackendSession extends AutoCloseable {
    CompletionStage<CanvasBackendReady> ready();

    CompletionStage<CanvasPresentationReady> render(CanvasRenderRequest request);

    CompletionStage<Void> termination();

    @Override
    void close();
}
