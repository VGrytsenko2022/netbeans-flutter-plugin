package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.canvas.CanvasImageResourceBundle;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasPreviewMode;
import io.github.vgrytsenko2022.designer.canvas.CanvasResolvedTheme;
import io.github.vgrytsenko2022.designer.canvas.CanvasTargetPlatform;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportMetrics;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportPresentation;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireProtocol;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WidgetPlacement;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.CanvasOrientation;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * Backend-neutral lifecycle and interaction contract for one Designer Canvas.
 *
 * <p>The contract deliberately exposes no process, native attachment, or
 * platform-host type. A backend remains the sole owner of those resources and
 * presents only the surface and protocol operations used by the MultiView.
 */
interface FlutterDesignerCanvasSession extends AutoCloseable {
    JComponent component();

    boolean isSurfaceFocused();

    boolean releaseSurfaceFocus();

    void show();

    void hide();

    void requestFocus();

    void clearFocusRequest();

    boolean restart();

    boolean canRestart();

    default void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme) {
        present(
                document,
                catalog,
                previewMode,
                targetPlatform,
                resolvedTheme,
                CanvasImageResourceBundle.empty());
    }

    void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources);

    /**
     * Presents the document with an optional transient device orientation.
     * The override affects only the resolved Canvas viewport and is never
     * written back to the {@code .fd} document.
     */
    default void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources,
            Optional<CanvasOrientation> orientationOverride) {
        Objects.requireNonNull(orientationOverride, "orientationOverride");
        present(document, catalog, previewMode, targetPlatform, resolvedTheme,
                imageResources);
    }

    void withdraw();

    void selectWidget(StableId widgetId);

    void setViewportMetricsListener(Consumer<CanvasViewportMetrics> listener);

    void setInteractionListener(Runnable listener);

    void setTextEditCommitListener(
            Consumer<CanvasRunnerRuntimeEvent.TextEditCommit> listener);

    void setInteractionBarrierListener(
            Consumer<InteractionBarrierState> listener);

    InteractionBarrierState interactionBarrierState();

    void setViewportPresentation(CanvasViewportPresentation presentation);

    boolean paletteCatalogInsertDropAvailable();

    boolean authorizePaletteDragSource(String token, WidgetTypeId widgetType);

    void showWidgetMovePreview(
            StableId sourceWidgetId,
            WidgetPlacement destination);

    void clearWidgetMovePreview();

    /**
     * Starts backend-owned teardown and completes only when the heavyweight
     * surface may be removed from its AWT hierarchy.
     *
     * <p>The default is safe for existing sessions whose {@link #close()}
     * performs the complete peer-release step synchronously. Backends with an
     * asynchronous native barrier must override this method and complete the
     * returned stage only after that barrier succeeds.</p>
     */
    default CompletionStage<Void> preparePeerRemovalAsync() {
        try {
            close();
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException | LinkageError failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    @Override
    void close();

    enum InteractionBarrierPhase {
        INACTIVE,
        SYNCHRONIZING,
        SYNCHRONIZED,
        TIMED_OUT
    }

    record InteractionBarrierState(
            InteractionBarrierPhase phase,
            long fenceSequence,
            Optional<CanvasLayoutKey> layoutKey) {
        public InteractionBarrierState {
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(layoutKey, "layoutKey");
            if (fenceSequence < 0
                    || fenceSequence > CanvasWireProtocol.MAX_SEQUENCE) {
                throw new IllegalArgumentException(
                        "interaction fence sequence is outside the wire range");
            }
            if (phase == InteractionBarrierPhase.SYNCHRONIZED
                    && layoutKey.isEmpty()) {
                throw new IllegalArgumentException(
                        "a synchronized interaction barrier requires a layout");
            }
        }

        public boolean inputEnabled() {
            return phase == InteractionBarrierPhase.SYNCHRONIZED;
        }
    }

    /** One opaque drag token resolved once to its authoritative Palette type. */
    record AdmittedPaletteDrop(
            WidgetTypeId widgetType,
            CanvasRunnerRuntimeEvent.PaletteDrop drop) {
        public AdmittedPaletteDrop {
            Objects.requireNonNull(widgetType, "widgetType");
            Objects.requireNonNull(drop, "drop");
        }
    }
}
