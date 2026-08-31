package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private boolean closed;

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
            try {
                acceptedSession.close();
            } catch (RuntimeException | LinkageError cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
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
        return closed;
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
            CanvasResolvedTheme resolvedTheme) {
        session.present(
                document, catalog, previewMode, targetPlatform, resolvedTheme);
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
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        session.close();
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
}
