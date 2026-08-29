package dev.flutter.netbeans.plugin.designer.catalog;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogBuildResult;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnosticCode;
import dev.flutter.netbeans.designer.catalog.WidgetCatalogComposition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalogContributor;
import java.awt.EventQueue;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 * NetBeans edge for discovering and composing the Flutter Designer widget catalog.
 *
 * <p>Each request produces an immutable catalog snapshot on a background worker.
 * Contributor failures are represented by {@link CatalogDiagnostic diagnostics};
 * they do not make the returned stage fail and never remove the built-in catalog.
 */
public final class NetBeansWidgetCatalogProvider {
    private static final String LOOKUP_SUBJECT = "<lookup>";
    private static final String COMPOSITION_SUBJECT = "<composition>";
    private static final RequestProcessor WORKER = new RequestProcessor(
            NetBeansWidgetCatalogProvider.class.getName(), 1, true);

    private final Supplier<? extends Collection<? extends WidgetCatalogContributor>> discovery;
    private final Executor executor;
    private final Object snapshotIdentityMonitor = new Object();
    private CatalogBuildResult retainedSnapshot;

    /** Uses the live default NetBeans Lookup for every requested snapshot. */
    public NetBeansWidgetCatalogProvider() {
        this(
                () -> Lookup.getDefault().lookupAll(WidgetCatalogContributor.class),
                task -> WORKER.post(task));
    }

    NetBeansWidgetCatalogProvider(
            Supplier<? extends Collection<? extends WidgetCatalogContributor>> discovery,
            Executor executor) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Discovers extensions and composes a stable snapshot without running contributor
     * code on the caller's thread.
     */
    public CompletionStage<CatalogBuildResult> snapshotAsync() {
        return CompletableFuture.supplyAsync(this::snapshotOffEdt, executor);
    }

    /**
     * Composes a snapshot on a caller-owned background worker.
     *
     * <p>This variant lets an existing NetBeans background lifecycle avoid dispatching
     * to, or waiting for, a second worker. It rejects Event Dispatch Thread calls before
     * Lookup or contributor code can run.
     *
     * @return immutable built-in plus extension catalog and composition diagnostics
     * @throws IllegalStateException if called on the Event Dispatch Thread
     */
    public CatalogBuildResult snapshotOffEdt() {
        if (EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Widget catalog discovery must not run on the Event Dispatch Thread");
        }
        Collection<? extends WidgetCatalogContributor> contributors;
        try {
            contributors = discovery.get();
        } catch (RuntimeException | LinkageError | ServiceConfigurationError failure) {
            return retainEquivalentSnapshot(failedSnapshot(
                    LOOKUP_SUBJECT,
                    "NetBeans Lookup contributor discovery failed: "
                    + failure.getClass().getSimpleName()));
        }
        if (contributors == null) {
            return retainEquivalentSnapshot(failedSnapshot(
                    LOOKUP_SUBJECT,
                    "NetBeans Lookup contributor discovery returned null"));
        }

        try {
            return retainEquivalentSnapshot(WidgetCatalogComposition.compose(
                    BuiltInWidgetCatalog.getDefault(), contributors));
        } catch (RuntimeException | LinkageError | ServiceConfigurationError failure) {
            // The core composer already isolates well-formed SPI calls. This final edge
            // guard covers malformed/lazy Lookup collections and binary-incompatible
            // providers before an immutable result can be produced.
            return retainEquivalentSnapshot(failedSnapshot(
                    COMPOSITION_SUBJECT,
                    "Widget catalog composition failed: "
                    + failure.getClass().getSimpleName()));
        }
    }

    /**
     * Retains exact catalog identity across reloads when discovery produced the
     * same immutable definitions and diagnostics.
     *
     * <p>Designer command sessions deliberately bind catalog snapshots by
     * identity. File reloads must therefore not invalidate active Undo/Redo
     * history merely because deterministic contributor discovery rebuilt an
     * equivalent value. A real definition or diagnostic change still receives
     * a fresh identity and invalidates stale mutation authority.</p>
     */
    private CatalogBuildResult retainEquivalentSnapshot(
            CatalogBuildResult candidate) {
        synchronized (snapshotIdentityMonitor) {
            CatalogBuildResult retained = retainedSnapshot;
            if (retained != null
                    && retained.catalog().definitions().equals(
                            candidate.catalog().definitions())
                    && retained.diagnostics().equals(candidate.diagnostics())) {
                return retained;
            }
            retainedSnapshot = candidate;
            return candidate;
        }
    }

    private static CatalogBuildResult failedSnapshot(String subject, String message) {
        return new CatalogBuildResult(
                BuiltInWidgetCatalog.getDefault(),
                List.of(new CatalogDiagnostic(
                        CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                        subject,
                        List.of(),
                        message)));
    }
}
