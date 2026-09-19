package io.github.vgrytsenko2022.plugin.designer.canvas.spi;

import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * Lifecycle boundary for one native Flutter Canvas surface.
 *
 * <p>Implementations own native attachment, visibility, focus, liveness and
 * cleanup. Platform handles remain opaque outside the provider edge. Every
 * method is called on the AWT event-dispatch thread. This contract reports
 * native-surface liveness; the owning session remains the single authority for
 * isolated runner-process crash notification through {@link Process#onExit()}.
 * </p>
 */
public interface NativeCanvasHost extends AutoCloseable {
    JComponent component();

    NativeCanvasParentHandle parentHandle();

    boolean attachRunner(long runnerProcessId);

    void detachRunner();

    boolean isRunnerAttached();

    boolean isRunnerSurfaceLive();

    boolean isNativePeerReady();

    Optional<NativeCanvasSurfaceMetrics> surfaceMetrics();

    void setRunnerVisible(boolean visible);

    /**
     * Makes a best-effort focus transfer to the verified runner surface.
     * A {@code false} result is not an attachment-liveness failure.
     */
    boolean requestRunnerFocus();

    /**
     * Makes a best-effort transfer from the verified runner surface back to
     * this provider's native parent peer. Providers that cannot perform an
     * exact native transfer keep the conservative {@code false} default.
     * A {@code false} result is not necessarily an attachment-liveness
     * failure.
     */
    default boolean releaseRunnerFocus() {
        return false;
    }

    /**
     * Reports whether keyboard focus currently belongs to this host's exact
     * verified runner surface. Providers that cannot inspect native focus keep
     * the conservative {@code false} default.
     */
    default boolean isRunnerFocused() {
        return false;
    }

    void onPeerReady(Runnable listener);

    /**
     * Registers the bounded pre-teardown hook invoked while the native parent
     * peer is still valid. Providers that can lose a parent peer must invoke
     * this listener before detaching the child or destroying the parent.
     */
    default void onPeerWillBeLost(Runnable listener) {
    }

    void onPeerLost(Runnable listener);

    void onAttachmentFailed(Consumer<String> listener);

    void onSurfaceMetricsChanged(Consumer<NativeCanvasSurfaceMetrics> listener);

    /** Idempotently detaches and releases resources owned by this host. */
    @Override
    void close();
}
