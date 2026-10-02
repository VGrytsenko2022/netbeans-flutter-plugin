package io.github.vgrytsenko2022.designer.canvas.runtime;

/**
 * Observes immutable lifecycle transitions in order and outside the controller
 * monitor.
 *
 * <p>Delivery is serialized on whichever caller or backend-callback thread
 * owns the current drain. It has no thread-affinity or EDT guarantee. A
 * NetBeans/Swing adapter must marshal its own presentation update to the EDT.</p>
 */
@FunctionalInterface
public interface CanvasControllerListener {
    void stateChanged(CanvasControllerState previous, CanvasControllerState current);
}
