package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
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

    void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme);

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
