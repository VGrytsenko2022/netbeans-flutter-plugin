package io.github.vgrytsenko2022.designer.canvas.runtime;

/**
 * Creates one transport-neutral backend session for one Canvas attempt.
 *
 * <p>The method must return promptly with an asynchronously starting session;
 * readiness belongs to {@link CanvasBackendSession#ready()}. A factory that
 * loses a concurrent controller close may still finish opening, but the
 * controller will fence and immediately close that detached session.</p>
 */
@FunctionalInterface
public interface CanvasBackendFactory {
    CanvasBackendSession open(CanvasBackendOpenRequest request);
}
