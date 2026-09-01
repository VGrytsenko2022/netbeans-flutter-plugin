package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.ArrayList;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetCatalogCompositionTest {
    @Test
    void contributorInputOrderCannotChangeCatalogOrDiagnostics() {
        Contributor alpha = contributor("com.alpha", definition("com.alpha.Card"));
        Contributor beta = contributor("org.beta", definition("org.beta.Panel"));

        CatalogBuildResult forward = WidgetCatalogComposition.compose(empty(), List.of(alpha, beta));
        CatalogBuildResult reverse = WidgetCatalogComposition.compose(empty(), List.of(beta, alpha));

        assertEquals(typeIds(forward.catalog()), typeIds(reverse.catalog()));
        assertEquals(forward.diagnostics(), reverse.diagnostics());
        assertEquals(List.of("com.alpha.Card", "org.beta.Panel"), typeIds(forward.catalog()));
    }

    @Test
    void duplicateContributorIdRejectsEveryClaimantWithoutLastWins() {
        Contributor first = contributor("com.example", definition("com.example.First"));
        Contributor second = contributor("com.example", definition("com.example.Second"));

        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(first, second));

        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(List.of(CatalogDiagnosticCode.DUPLICATE_CONTRIBUTOR_ID),
                result.diagnostics().stream().map(CatalogDiagnostic::code).toList());
    }

    @Test
    void duplicateTypeInsideOneContributorRejectsItsWholeContribution() {
        WidgetDefinition duplicate = definition("com.example.Card");
        Contributor contributor = new Contributor(
                "com.example", WidgetCatalog.API_VERSION, List.of(duplicate, duplicate));

        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(contributor));

        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(CatalogDiagnosticCode.DUPLICATE_WIDGET_TYPE, result.diagnostics().getFirst().code());
    }

    @Test
    void extensionCannotReplaceReservedFlutterDefinition() {
        WidgetDefinition replacement = definition("flutter.widgets.Text");
        Contributor contributor = contributor("com.hostile", replacement);

        CatalogBuildResult result = WidgetCatalogComposition.compose(
                BuiltInWidgetCatalog.getDefault(), List.of(contributor));

        WidgetDefinition text = result.catalog().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        assertEquals("Text", text.dartClassName());
        assertTrue(result.diagnostics().stream()
                .anyMatch(value -> value.code() == CatalogDiagnosticCode.RESERVED_WIDGET_TYPE));
        assertEquals(13, result.catalog().definitions().size());
    }

    @Test
    void contributorCannotClaimAnotherNamespace() {
        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(),
                List.of(contributor("com.owner", definition("org.other.Widget"))));
        assertTrue(result.catalog().definitions().isEmpty());
        assertTrue(result.diagnostics().stream()
                .anyMatch(value -> value.code() == CatalogDiagnosticCode.FOREIGN_WIDGET_NAMESPACE));
    }

    @Test
    void unsupportedApiVersionIsRejected() {
        Contributor contributor = new Contributor(
                "com.example", WidgetCatalog.API_VERSION + 1, List.of(definition("com.example.Card")));
        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(contributor));
        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(CatalogDiagnosticCode.UNSUPPORTED_API_VERSION, result.diagnostics().getFirst().code());
    }

    @Test
    void previousApiVersionIsRejectedAfterIconDataExpandedTheSealedModel() {
        Contributor api2 = new Contributor(
                "com.example", 2, List.of(definition("com.example.Card")));

        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(api2));

        assertTrue(result.catalog().definitions().isEmpty());
        CatalogDiagnostic diagnostic = result.diagnostics().getFirst();
        assertEquals(CatalogDiagnosticCode.UNSUPPORTED_API_VERSION, diagnostic.code());
        assertTrue(diagnostic.message().contains("Expected catalog API 5"));
        assertTrue(diagnostic.message().contains("received 2"));
    }

    @Test
    void resultDoesNotTrackLaterContributorCollectionMutation() {
        ArrayList<WidgetDefinition> supplied = new ArrayList<>(List.of(definition("com.example.Card")));
        Contributor contributor = new Contributor("com.example", WidgetCatalog.API_VERSION, supplied);
        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(contributor));

        supplied.clear();

        assertEquals(List.of("com.example.Card"), typeIds(result.catalog()));
        assertFalse(result.hasErrors());
    }

    @Test
    void failingDefinitionCollectionIsRejectedInsteadOfEscapingComposition() {
        Collection<WidgetDefinition> failing = new AbstractCollection<>() {
            @Override
            public Iterator<WidgetDefinition> iterator() {
                throw new IllegalStateException("broken provider");
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public Object[] toArray() {
                throw new IllegalStateException("broken provider");
            }
        };
        Contributor contributor = new Contributor("com.example", WidgetCatalog.API_VERSION, failing);

        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(contributor));

        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(CatalogDiagnosticCode.INVALID_DEFINITION, result.diagnostics().getFirst().code());
        assertEquals("Definition snapshot failed: IllegalStateException",
                result.diagnostics().getFirst().message());
    }

    @Test
    void nullContributorDiagnosticIsIndependentOfInputPosition() {
        Contributor valid = contributor("com.example", definition("com.example.Card"));
        ArrayList<WidgetCatalogContributor> first = new ArrayList<>();
        first.add(null);
        first.add(valid);
        ArrayList<WidgetCatalogContributor> second = new ArrayList<>();
        second.add(valid);
        second.add(null);

        CatalogBuildResult forward = WidgetCatalogComposition.compose(empty(), first);
        CatalogBuildResult reverse = WidgetCatalogComposition.compose(empty(), second);

        assertEquals(typeIds(forward.catalog()), typeIds(reverse.catalog()));
        assertEquals(forward.diagnostics(), reverse.diagnostics());
        assertEquals("<null-contributor>", forward.diagnostics().getFirst().subject());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void rawDefinitionValueRejectsContributorBeforeOrdering() {
        Collection<WidgetDefinition> raw = (Collection) List.of(
                definition("com.example.ValidButRejected"),
                "not-a-widget-definition");
        Contributor contributor = new Contributor("com.example", WidgetCatalog.API_VERSION, raw);

        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(contributor));

        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(List.of(CatalogDiagnosticCode.INVALID_DEFINITION),
                result.diagnostics().stream().map(CatalogDiagnostic::code).toList());
        assertEquals("Definitions collection contains java.lang.String instead of WidgetDefinition",
                result.diagnostics().getFirst().message());
    }

    @Test
    void runtimeAndLinkageFailuresFromDefinitionsRejectOnlyBrokenContributors() {
        WidgetCatalogContributor runtimeBroken = failingDefinitions(
                "com.runtime", new IllegalStateException("broken provider"));
        WidgetCatalogContributor linkageBroken = failingDefinitions(
                "com.linkage", new NoClassDefFoundError("missing optional widget dependency"));
        Contributor valid = contributor("com.valid", definition("com.valid.Card"));

        CatalogBuildResult result = WidgetCatalogComposition.compose(
                empty(), List.of(runtimeBroken, valid, linkageBroken));

        assertEquals(List.of("com.valid.Card"), typeIds(result.catalog()));
        assertEquals(2, result.diagnostics().size());
        assertTrue(result.diagnostics().stream()
                .allMatch(value -> value.code() == CatalogDiagnosticCode.INVALID_DEFINITION));
        assertEquals(List.of(
                "Definition loading failed: IllegalStateException",
                "Definition loading failed: NoClassDefFoundError"),
                result.diagnostics().stream().map(CatalogDiagnostic::message).sorted().toList());
    }

    @Test
    void runtimeAndLinkageFailuresFromContributorMetadataAreContained() {
        WidgetCatalogContributor runtimeFailure = failingMetadata(new IllegalStateException("broken"));
        WidgetCatalogContributor linkageFailure = failingMetadata(new NoClassDefFoundError("missing"));

        CatalogBuildResult result = WidgetCatalogComposition.compose(
                empty(), List.of(runtimeFailure, linkageFailure));

        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(2, result.diagnostics().size());
        assertTrue(result.diagnostics().stream()
                .allMatch(value -> value.code() == CatalogDiagnosticCode.INVALID_CONTRIBUTOR));
        assertEquals(List.of(
                "Contributor metadata failed: IllegalStateException",
                "Contributor metadata failed: NoClassDefFoundError"),
                result.diagnostics().stream().map(CatalogDiagnostic::message).sorted().toList());
    }

    @Test
    void paletteKeepsEqualOrderCategoriesGroupedByCategoryId() {
        WidgetDefinition alphaLate = definition("com.example.AlphaLate", "alpha", 10, 90);
        WidgetDefinition betaEarly = definition("com.example.BetaEarly", "beta", 10, 1);
        WidgetDefinition alphaEarly = definition("com.example.AlphaEarly", "alpha", 10, 2);

        WidgetCatalog catalog = WidgetCatalog.strict(List.of(betaEarly, alphaLate, alphaEarly));

        assertEquals(List.of(
                "com.example.AlphaEarly",
                "com.example.AlphaLate",
                "com.example.BetaEarly"),
                catalog.paletteDefinitions().stream().map(value -> value.typeId().value()).toList());
    }

    @Test
    void strictCatalogRejectsOneCategoryWithMultipleOrdersDeterministically() {
        WidgetDefinition early = definition("com.example.Early", "shared", 10, 1);
        WidgetDefinition late = definition("com.example.Late", "shared", 20, 2);

        IllegalArgumentException forward = assertThrows(IllegalArgumentException.class,
                () -> WidgetCatalog.strict(List.of(early, late)));
        IllegalArgumentException reverse = assertThrows(IllegalArgumentException.class,
                () -> WidgetCatalog.strict(List.of(late, early)));

        assertEquals("Palette category shared has inconsistent orders [10, 20]", forward.getMessage());
        assertEquals(forward.getMessage(), reverse.getMessage());
    }

    @Test
    void localPaletteOrderConflictRejectsTheWholeContributor() {
        Contributor inconsistent = new Contributor(
                "com.local",
                WidgetCatalog.API_VERSION,
                List.of(
                        definition("com.local.First", "shared", 10, 1),
                        definition("com.local.Second", "shared", 20, 2),
                        definition("com.local.OtherwiseValid", "other", 30, 1)));

        CatalogBuildResult result = WidgetCatalogComposition.compose(empty(), List.of(inconsistent));

        assertTrue(result.catalog().definitions().isEmpty());
        assertEquals(List.of(new CatalogDiagnostic(
                CatalogDiagnosticCode.INCONSISTENT_PALETTE_CATEGORY,
                "shared",
                List.of("com.local"),
                "Extension orders conflict: [10, 20]")), result.diagnostics());
    }

    @Test
    void crossContributorPaletteConflictIsIndependentOfInputOrderAndRejectsBothAtomically() {
        Contributor alpha = new Contributor(
                "com.alpha",
                WidgetCatalog.API_VERSION,
                List.of(
                        definition("com.alpha.Shared", "shared", 10, 1),
                        definition("com.alpha.Safe", "alpha-safe", 30, 1)));
        Contributor beta = new Contributor(
                "com.beta",
                WidgetCatalog.API_VERSION,
                List.of(
                        definition("com.beta.Shared", "shared", 20, 1),
                        definition("com.beta.Safe", "beta-safe", 40, 1)));

        CatalogBuildResult forward = WidgetCatalogComposition.compose(empty(), List.of(alpha, beta));
        CatalogBuildResult reverse = WidgetCatalogComposition.compose(empty(), List.of(beta, alpha));

        assertTrue(forward.catalog().definitions().isEmpty());
        assertEquals(forward.catalog().definitions(), reverse.catalog().definitions());
        assertEquals(forward.diagnostics(), reverse.diagnostics());
        assertEquals(List.of(new CatalogDiagnostic(
                CatalogDiagnosticCode.INCONSISTENT_PALETTE_CATEGORY,
                "shared",
                List.of("com.alpha", "com.beta"),
                "Extension orders conflict: [10, 20]")), forward.diagnostics());
    }

    @Test
    void builtInCategoryKeepsMatchingContributorAndRejectsMismatchAtomically() {
        WidgetCatalog builtIn = WidgetCatalog.strict(List.of(
                definition("flutter.base.BuiltIn", "shared", 10, 1)));
        Contributor matching = contributor(
                "com.matching", definition("com.matching.Widget", "shared", 10, 2));
        Contributor mismatching = new Contributor(
                "com.mismatch",
                WidgetCatalog.API_VERSION,
                List.of(
                        definition("com.mismatch.Widget", "shared", 20, 3),
                        definition("com.mismatch.OtherwiseValid", "other", 30, 1)));

        CatalogBuildResult result = WidgetCatalogComposition.compose(
                builtIn, List.of(mismatching, matching));

        assertEquals(List.of("com.matching.Widget", "flutter.base.BuiltIn"), typeIds(result.catalog()));
        assertEquals(List.of(new CatalogDiagnostic(
                CatalogDiagnosticCode.INCONSISTENT_PALETTE_CATEGORY,
                "shared",
                List.of("com.mismatch"),
                "Built-in order 10 conflicts with extension orders [20]")), result.diagnostics());
    }

    private static WidgetCatalog empty() {
        return WidgetCatalog.strict(List.of());
    }

    private static Contributor contributor(String id, WidgetDefinition... definitions) {
        return new Contributor(id, WidgetCatalog.API_VERSION, List.of(definitions));
    }

    private static WidgetDefinition definition(String typeId) {
        return definition(typeId, "example", 1, 1);
    }

    private static WidgetDefinition definition(
            String typeId,
            String categoryId,
            int categoryOrder,
            int itemOrder) {
        String className = typeId.substring(typeId.lastIndexOf('.') + 1);
        return new WidgetDefinition(
                new WidgetTypeId(typeId),
                className,
                Optional.empty(),
                false,
                "package:example/widgets.dart",
                List.of("package:example/widgets.dart"),
                Set.of(),
                new PaletteMetadata(categoryId, categoryOrder, itemOrder, className),
                List.of(),
                List.of());
    }

    private static WidgetCatalogContributor failingMetadata(LinkageError failure) {
        return new WidgetCatalogContributor() {
            @Override
            public String contributorId() {
                throw failure;
            }

            @Override
            public int apiVersion() {
                return WidgetCatalog.API_VERSION;
            }

            @Override
            public Collection<WidgetDefinition> definitions() {
                return List.of();
            }
        };
    }

    private static WidgetCatalogContributor failingMetadata(RuntimeException failure) {
        return new WidgetCatalogContributor() {
            @Override
            public String contributorId() {
                throw failure;
            }

            @Override
            public int apiVersion() {
                return WidgetCatalog.API_VERSION;
            }

            @Override
            public Collection<WidgetDefinition> definitions() {
                return List.of();
            }
        };
    }

    private static WidgetCatalogContributor failingDefinitions(String id, LinkageError failure) {
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

    private static WidgetCatalogContributor failingDefinitions(String id, RuntimeException failure) {
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

    private static List<String> typeIds(WidgetCatalog catalog) {
        return catalog.definitions().stream().map(value -> value.typeId().value()).toList();
    }

    private record Contributor(
            String contributorId,
            int apiVersion,
            Collection<WidgetDefinition> definitions) implements WidgetCatalogContributor {
    }
}
