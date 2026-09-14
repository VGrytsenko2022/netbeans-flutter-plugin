package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasImageResourceBundle;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * The single ownership envelope for one backend-created Designer Canvas session.
 *
 * <p>The component and exact heavyweight focus surface are captured once while
 * adopting the session. MultiView code retains only this envelope, preventing
 * the host component and session lifecycle from acquiring independent owners.
 */
final class FlutterDesignerCanvasOwner implements FlutterDesignerCanvasSession {
    private final Backend backend;
    private final FlutterDesignerCanvasSession session;
    private final JComponent component;
    private final Component focusSurface;
    private final Object retirementLock = new Object();
    private RetirementState retirementState = RetirementState.ACTIVE;
    private CompletableFuture<Void> retirementFuture;

    private FlutterDesignerCanvasOwner(
            Backend backend,
            FlutterDesignerCanvasSession session,
            JComponent component,
            Component focusSurface) {
        this.backend = backend;
        this.session = session;
        this.component = component;
        this.focusSurface = focusSurface;
    }

    /**
     * Transfers the exact session into one owner or closes it if component
     * validation fails before ownership can be published.
     */
    static FlutterDesignerCanvasOwner adopt(
            Backend backend,
            FlutterDesignerCanvasSession session) {
        Backend acceptedBackend = Objects.requireNonNull(backend, "backend");
        FlutterDesignerCanvasSession acceptedSession = Objects.requireNonNull(
                session, "session");
        try {
            JComponent acceptedComponent = Objects.requireNonNull(
                    acceptedSession.component(),
                    "Canvas session returned no component");
            return new FlutterDesignerCanvasOwner(
                    acceptedBackend,
                    acceptedSession,
                    acceptedComponent,
                    findUniqueCanvasSurface(acceptedComponent));
        } catch (RuntimeException | LinkageError failure) {
            initiateRejectedSessionCleanup(acceptedSession, failure);
            throw failure;
        }
    }

    Backend backend() {
        return backend;
    }

    Component focusSurface() {
        return focusSurface;
    }

    boolean closed() {
        synchronized (retirementLock) {
            return retirementState == RetirementState.RETIRED;
        }
    }

    RetirementState retirementState() {
        synchronized (retirementLock) {
            return retirementState;
        }
    }

    @Override
    public JComponent component() {
        return component;
    }

    @Override
    public boolean isSurfaceFocused() {
        return session.isSurfaceFocused();
    }

    @Override
    public boolean releaseSurfaceFocus() {
        return session.releaseSurfaceFocus();
    }

    @Override
    public void show() {
        session.show();
    }

    @Override
    public void hide() {
        session.hide();
    }

    @Override
    public void requestFocus() {
        session.requestFocus();
    }

    @Override
    public void clearFocusRequest() {
        session.clearFocusRequest();
    }

    @Override
    public boolean restart() {
        return session.restart();
    }

    @Override
    public boolean canRestart() {
        return session.canRestart();
    }

    @Override
    public void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources) {
        session.present(
                document,
                catalog,
                previewMode,
                targetPlatform,
                resolvedTheme,
                imageResources);
    }

    @Override
    public void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources,
            java.util.Optional<CanvasOrientation> orientationOverride) {
        session.present(
                document,
                catalog,
                previewMode,
                targetPlatform,
                resolvedTheme,
                imageResources,
                orientationOverride);
    }

    @Override
    public void withdraw() {
        session.withdraw();
    }

    @Override
    public void selectWidget(StableId widgetId) {
        session.selectWidget(widgetId);
    }

    @Override
    public void setViewportMetricsListener(
            Consumer<CanvasViewportMetrics> listener) {
        session.setViewportMetricsListener(listener);
    }

    @Override
    public void setInteractionListener(Runnable listener) {
        session.setInteractionListener(listener);
    }

    @Override
    public void setTextEditCommitListener(
            Consumer<CanvasRunnerRuntimeEvent.TextEditCommit> listener) {
        session.setTextEditCommitListener(listener);
    }

    @Override
    public void setInteractionBarrierListener(
            Consumer<InteractionBarrierState> listener) {
        session.setInteractionBarrierListener(listener);
    }

    @Override
    public InteractionBarrierState interactionBarrierState() {
        return session.interactionBarrierState();
    }

    @Override
    public void setViewportPresentation(
            CanvasViewportPresentation presentation) {
        session.setViewportPresentation(presentation);
    }

    @Override
    public boolean paletteCatalogInsertDropAvailable() {
        return session.paletteCatalogInsertDropAvailable();
    }

    @Override
    public boolean authorizePaletteDragSource(
            String token,
            WidgetTypeId widgetType) {
        return session.authorizePaletteDragSource(token, widgetType);
    }

    @Override
    public void showWidgetMovePreview(
            StableId sourceWidgetId,
            WidgetPlacement destination) {
        session.showWidgetMovePreview(sourceWidgetId, destination);
    }

    @Override
    public void clearWidgetMovePreview() {
        session.clearWidgetMovePreview();
    }

    @Override
    public CompletionStage<Void> preparePeerRemovalAsync() {
        CompletableFuture<Void> published;
        synchronized (retirementLock) {
            if (retirementState == RetirementState.RETIRED) {
                return retirementFuture == null
                        ? CompletableFuture.completedFuture(null)
                        : retirementFuture;
            }
            if (retirementState == RetirementState.RETIRING) {
                return retirementFuture;
            }
            retirementState = RetirementState.RETIRING;
            published = new CompletableFuture<>();
            retirementFuture = published;
        }

        final CompletionStage<Void> delegated;
        try {
            delegated = Objects.requireNonNull(
                    session.preparePeerRemovalAsync(),
                    "Canvas session returned no peer-removal completion");
        } catch (RuntimeException | LinkageError failure) {
            finishRetirement(published, failure);
            return published;
        }
        try {
            delegated.whenComplete((ignored, failure) ->
                    finishRetirement(published, failure));
        } catch (RuntimeException | LinkageError failure) {
            finishRetirement(published, failure);
        }
        return published;
    }

    @Override
    public void close() {
        // AutoCloseable cannot expose the asynchronous completion. Initiate the
        // same retirement without claiming success; callers that remove the
        // component must await preparePeerRemovalAsync() instead.
        preparePeerRemovalAsync();
    }

    private void finishRetirement(
            CompletableFuture<Void> published,
            Throwable failure) {
        Throwable terminal = unwrapCompletionFailure(failure);
        synchronized (retirementLock) {
            if (retirementFuture != published
                    || retirementState != RetirementState.RETIRING) {
                return;
            }
            if (terminal == null) {
                retirementState = RetirementState.RETIRED;
            } else {
                retirementState = RetirementState.POISONED;
                // A failed native cleanup is explicitly retryable. The failed
                // attempt remains observable through its returned future, but
                // a subsequent call receives a fresh attempt.
                retirementFuture = null;
            }
        }
        if (terminal == null) {
            published.complete(null);
        } else {
            published.completeExceptionally(terminal);
        }
    }

    private static void initiateRejectedSessionCleanup(
            FlutterDesignerCanvasSession rejected,
            Throwable adoptionFailure) {
        try {
            CompletionStage<Void> cleanup = Objects.requireNonNull(
                    rejected.preparePeerRemovalAsync(),
                    "Rejected Canvas session returned no peer-removal completion");
            cleanup.whenComplete((ignored, cleanupFailure) -> {
                Throwable terminal = unwrapCompletionFailure(cleanupFailure);
                if (terminal != null && terminal != adoptionFailure) {
                    adoptionFailure.addSuppressed(terminal);
                }
            });
        } catch (RuntimeException | LinkageError cleanupFailure) {
            if (cleanupFailure != adoptionFailure) {
                adoptionFailure.addSuppressed(cleanupFailure);
            }
        }
    }

    private static Throwable unwrapCompletionFailure(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static Component findUniqueCanvasSurface(Component root) {
        List<Component> candidates = new ArrayList<>(1);
        collectCanvasSurfaces(root, candidates);
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private static void collectCanvasSurfaces(
            Component candidate,
            List<Component> surfaces) {
        if (candidate instanceof java.awt.Canvas) {
            surfaces.add(candidate);
        }
        if (candidate instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                collectCanvasSurfaces(child, surfaces);
            }
        }
    }

    enum RetirementState {
        ACTIVE,
        RETIRING,
        RETIRED,
        POISONED
    }
}
