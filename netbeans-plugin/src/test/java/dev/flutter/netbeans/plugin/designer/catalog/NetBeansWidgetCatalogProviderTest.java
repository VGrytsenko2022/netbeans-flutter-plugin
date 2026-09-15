package dev.flutter.netbeans.plugin.designer.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.CatalogBuildResult;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnosticCode;
import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalogContributor;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.EventQueue;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NetBeansWidgetCatalogProviderTest {
    private static final String EXTENSION_TYPE = "com.example.ExampleCard";

    @Test
    void defaultLookupAlwaysRetainsTheBuiltInSnapshot() throws Exception {
        CatalogBuildResult result = new NetBeansWidgetCatalogProvider()
                .snapshotAsync()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        assertTrue(typeIds(result).contains("flutter.material.Scaffold"));
        assertTrue(typeIds(result).contains("flutter.widgets.Text"));
        assertTrue(typeIds(result).contains("flutter.widgets.Align"));
        assertTrue(typeIds(result).contains("flutter.widgets.FractionallySizedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.FittedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.ConstrainedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.UnconstrainedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.LimitedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.OverflowBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.Stack"));
        assertTrue(typeIds(result).contains("flutter.widgets.Wrap"));
        assertTrue(typeIds(result).contains("flutter.widgets.Expanded"));
        assertTrue(typeIds(result).contains("flutter.widgets.Flexible"));
        assertTrue(typeIds(result).contains("flutter.widgets.Spacer"));
        assertTrue(typeIds(result).contains("flutter.widgets.Baseline"));
        assertTrue(typeIds(result).contains("flutter.widgets.IntrinsicHeight"));
        assertTrue(typeIds(result).contains("flutter.widgets.IntrinsicWidth"));
        assertTrue(typeIds(result).contains("flutter.widgets.Offstage"));
        assertTrue(typeIds(result).contains("flutter.widgets.SizedOverflowBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.Transform"));
        assertTrue(typeIds(result).contains("flutter.widgets.RotatedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.ListBody"));
        assertTrue(typeIds(result).contains("flutter.widgets.OverflowBar"));
        assertTrue(typeIds(result).contains("flutter.widgets.SafeArea"));
        assertTrue(typeIds(result).contains("flutter.widgets.ListView"));
        assertTrue(typeIds(result).contains("flutter.widgets.GridView"));
        assertTrue(typeIds(result).contains("flutter.widgets.SingleChildScrollView"));
        assertTrue(typeIds(result).contains("flutter.widgets.ColoredBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.Placeholder"));
        assertTrue(typeIds(result).contains("flutter.widgets.Directionality"));
        assertTrue(typeIds(result).contains("flutter.widgets.DecoratedBox"));
        assertTrue(typeIds(result).contains("flutter.widgets.ExcludeSemantics"));
        assertTrue(typeIds(result).contains("flutter.widgets.IndexedStack"));
        assertTrue(typeIds(result).contains("flutter.widgets.ClipRect"));
        assertTrue(typeIds(result).contains("flutter.widgets.ClipOval"));
        assertTrue(typeIds(result).contains("flutter.widgets.ClipRRect"));
        assertTrue(typeIds(result).contains("flutter.widgets.ClipPath"));
        assertTrue(typeIds(result).contains("flutter.widgets.ClipRSuperellipse"));
        assertTrue(typeIds(result).contains("flutter.widgets.PhysicalModel"));
        assertTrue(typeIds(result).contains("flutter.widgets.PhysicalShape"));
        assertTrue(typeIds(result).contains("flutter.widgets.RepaintBoundary"));
        assertTrue(typeIds(result).contains("flutter.widgets.IgnorePointer"));
        assertTrue(typeIds(result).contains("flutter.widgets.AbsorbPointer"));
        assertTrue(typeIds(result).contains("flutter.widgets.BlockSemantics"));
        assertTrue(typeIds(result).contains("flutter.widgets.MergeSemantics"));
        assertTrue(typeIds(result).contains("flutter.widgets.IndexedSemantics"));
        assertTrue(typeIds(result).contains("flutter.widgets.ExcludeFocus"));
        assertTrue(typeIds(result).contains("flutter.widgets.ExcludeFocusTraversal"));
        assertTrue(typeIds(result).contains("flutter.widgets.Visibility"));
        assertTrue(typeIds(result).contains("flutter.widgets.TickerMode"));
        assertTrue(typeIds(result).contains("flutter.widgets.DefaultTextHeightBehavior"));
        assertTrue(typeIds(result).contains("flutter.widgets.DefaultSelectionStyle"));
        assertTrue(typeIds(result).contains("flutter.widgets.IconTheme"));
        assertTrue(typeIds(result).contains("flutter.widgets.ImageIcon"));
        assertTrue(typeIds(result).contains("flutter.material.Divider"));
        assertTrue(typeIds(result).contains("flutter.material.VerticalDivider"));
        assertTrue(typeIds(result).contains("flutter.material.Card"));
        assertTrue(typeIds(result).contains("flutter.material.Badge"));
        assertTrue(typeIds(result).contains("flutter.material.CircleAvatar"));
        assertTrue(typeIds(result).contains("flutter.material.LinearProgressIndicator"));
        assertTrue(typeIds(result).contains("flutter.material.CircularProgressIndicator"));
        assertTrue(typeIds(result).contains("flutter.material.RefreshProgressIndicator"));
        assertTrue(typeIds(result).contains("flutter.material.RefreshIndicator"));
        assertTrue(typeIds(result).contains("flutter.material.TextButton"));
        assertTrue(typeIds(result).contains("flutter.material.OutlinedButton"));
        assertTrue(typeIds(result).contains("flutter.material.FilledButton"));
        assertTrue(typeIds(result).contains("flutter.material.FloatingActionButton"));
        assertTrue(typeIds(result).contains("flutter.material.IconButton"));
        assertTrue(typeIds(result).contains("flutter.material.Checkbox"));
        assertTrue(typeIds(result).contains("flutter.material.Switch"));
        assertTrue(typeIds(result).contains("flutter.material.Slider"));
        assertTrue(typeIds(result).contains("flutter.material.RangeSlider"));
        assertTrue(typeIds(result).contains("flutter.material.Radio"));
        assertTrue(typeIds(result).contains("flutter.widgets.RadioGroup"));
        assertTrue(typeIds(result).contains("flutter.material.ListTile"));
    }

    @Test
    void lookupAndContributorCompositionRunOffTheEventDispatchThread() throws Exception {
        AtomicBoolean discoveryOnEdt = new AtomicBoolean(true);
        AtomicReference<String> discoveryThread = new AtomicReference<>();
        ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "catalog-provider-test-worker");
            thread.setDaemon(true);
            return thread;
        });
        try {
            NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                    () -> {
                        discoveryOnEdt.set(EventQueue.isDispatchThread());
                        discoveryThread.set(Thread.currentThread().getName());
                        return List.of(contributor("com.example", definition(EXTENSION_TYPE)));
                    },
                    worker);
            AtomicReference<CompletionStage<CatalogBuildResult>> pending = new AtomicReference<>();

            EventQueue.invokeAndWait(() -> pending.set(provider.snapshotAsync()));
            CatalogBuildResult result = pending.get().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            assertFalse(discoveryOnEdt.get());
            assertEquals("catalog-provider-test-worker", discoveryThread.get());
            assertTrue(typeIds(result).contains(EXTENSION_TYPE));
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void synchronousSnapshotRejectsTheEventDispatchThreadBeforeDiscovery() throws Exception {
        AtomicBoolean discoveryCalled = new AtomicBoolean();
        NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                () -> {
                    discoveryCalled.set(true);
                    return List.of();
                },
                Runnable::run);
        AtomicReference<IllegalStateException> failure = new AtomicReference<>();

        EventQueue.invokeAndWait(() -> failure.set(assertThrows(
                IllegalStateException.class,
                provider::snapshotOffEdt)));

        assertEquals(
                "Widget catalog discovery must not run on the Event Dispatch Thread",
                failure.get().getMessage());
        assertFalse(discoveryCalled.get());
    }

    @Test
    void malformedContributorsAreRejectedWithoutHidingValidPeersOrBuiltIns() {
        WidgetCatalogContributor runtimeBroken = failingDefinitions(
                "com.brokenruntime", new IllegalStateException("broken"));
        WidgetCatalogContributor linkageBroken = failingDefinitions(
                "com.brokenlinkage", new NoClassDefFoundError("missing"));
        WidgetCatalogContributor valid = contributor("com.example", definition(EXTENSION_TYPE));
        NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                () -> List.of(runtimeBroken, valid, linkageBroken),
                Runnable::run);

        CatalogBuildResult result = provider.snapshotAsync().toCompletableFuture().join();

        assertTrue(typeIds(result).contains("flutter.material.Scaffold"));
        assertTrue(typeIds(result).contains(EXTENSION_TYPE));
        assertEquals(
                List.of("com.brokenlinkage", "com.brokenruntime"),
                result.diagnostics().stream().map(CatalogDiagnostic::subject).toList());
        assertTrue(result.diagnostics().stream()
                .allMatch(value -> value.code() == CatalogDiagnosticCode.INVALID_DEFINITION));
    }

    @Test
    void lookupFailureBecomesStableDiagnosticAndBuiltInFallback() {
        NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                () -> {
                    throw new IllegalStateException("environment-specific detail");
                },
                Runnable::run);

        CatalogBuildResult result = provider.snapshotAsync().toCompletableFuture().join();

        assertEquals(242, result.catalog().definitions().size());
        assertEquals(List.of(new CatalogDiagnostic(
                CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                "<lookup>",
                List.of(),
                "NetBeans Lookup contributor discovery failed: IllegalStateException")),
                result.diagnostics());
    }

    @Test
    void lazyLookupCollectionFailureIsContainedByTheEdgeGuard() {
        Collection<WidgetCatalogContributor> hostile = new java.util.AbstractCollection<>() {
            @Override
            public java.util.Iterator<WidgetCatalogContributor> iterator() {
                return List.<WidgetCatalogContributor>of().iterator();
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public Object[] toArray() {
                throw new java.util.ServiceConfigurationError("miscompiled provider");
            }
        };
        NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                () -> hostile,
                Runnable::run);

        CatalogBuildResult result = provider.snapshotAsync().toCompletableFuture().join();

        assertEquals(242, result.catalog().definitions().size());
        assertEquals(List.of(new CatalogDiagnostic(
                CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                "<composition>",
                List.of(),
                "Widget catalog composition failed: ServiceConfigurationError")),
                result.diagnostics());
    }

    @Test
    void returnedSnapshotCollectionsAreImmutable() {
        NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                List::of,
                Runnable::run);

        CatalogBuildResult result = provider.snapshotAsync().toCompletableFuture().join();

        assertThrows(UnsupportedOperationException.class,
                () -> result.diagnostics().add(new CatalogDiagnostic(
                        CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                        "unused",
                        List.of(),
                        "unused")));
        assertThrows(UnsupportedOperationException.class,
                () -> result.catalog().definitions().clear());
    }

    @Test
    void equivalentRediscoveryRetainsIdentityButDefinitionChangesReplaceIt() {
        AtomicReference<List<WidgetCatalogContributor>> contributors =
                new AtomicReference<>(List.of());
        NetBeansWidgetCatalogProvider provider = new NetBeansWidgetCatalogProvider(
                contributors::get,
                Runnable::run);

        CatalogBuildResult first = provider.snapshotOffEdt();
        CatalogBuildResult equivalent = provider.snapshotOffEdt();

        assertSame(first, equivalent,
                "equivalent reloads must retain exact command-session catalog identity");

        contributors.set(List.of(contributor(
                "com.example", definition(EXTENSION_TYPE))));
        CatalogBuildResult extended = provider.snapshotOffEdt();

        assertNotSame(first, extended);
        assertTrue(typeIds(extended).contains(EXTENSION_TYPE));
        assertSame(extended, provider.snapshotOffEdt(),
                "the new equivalent extension snapshot must become stable");
    }

    private static WidgetCatalogContributor contributor(
            String id, WidgetDefinition... definitions) {
        return new Contributor(id, WidgetCatalog.API_VERSION, List.of(definitions));
    }

    private static WidgetCatalogContributor failingDefinitions(
            String id, RuntimeException failure) {
        return new WidgetCatalogContributor() {
            @Override
            public String contributorId() {
                return id;
            }

            @Override
            public int apiVersion() {
                return WidgetCatalog.API_VERSION;
            }

            @Override
            public Collection<WidgetDefinition> definitions() {
                throw failure;
            }
        };
    }

    private static WidgetCatalogContributor failingDefinitions(
            String id, LinkageError failure) {
        return new WidgetCatalogContributor() {
            @Override
            public String contributorId() {
                return id;
            }

            @Override
            public int apiVersion() {
                return WidgetCatalog.API_VERSION;
            }

            @Override
            public Collection<WidgetDefinition> definitions() {
                throw failure;
            }
        };
    }

    private static WidgetDefinition definition(String typeId) {
        String className = typeId.substring(typeId.lastIndexOf('.') + 1);
        return new WidgetDefinition(
                new WidgetTypeId(typeId),
                className,
                Optional.empty(),
                false,
                "package:example/widgets.dart",
                List.of("package:example/widgets.dart"),
                Set.of(),
                new PaletteMetadata("example", 500, 10, className),
                List.of(),
                List.of());
    }

    private static List<String> typeIds(CatalogBuildResult result) {
        return result.catalog().definitions().stream()
                .map(value -> value.typeId().value())
                .toList();
    }

    private record Contributor(
            String contributorId,
            int apiVersion,
            Collection<WidgetDefinition> definitions) implements WidgetCatalogContributor {
    }
}
