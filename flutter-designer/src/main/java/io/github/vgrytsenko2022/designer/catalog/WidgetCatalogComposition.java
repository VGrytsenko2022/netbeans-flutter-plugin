package io.github.vgrytsenko2022.designer.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Deterministic, fail-closed composition of the built-in catalog and extensions. */
public final class WidgetCatalogComposition {
    private static final Pattern CONTRIBUTOR_ID =
            Pattern.compile("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9-]*)+$");
    private static final Comparator<CatalogDiagnostic> DIAGNOSTIC_ORDER = Comparator
            .comparing(CatalogDiagnostic::code)
            .thenComparing(CatalogDiagnostic::subject)
            .thenComparing(value -> String.join("\u0000", value.contributors()))
            .thenComparing(CatalogDiagnostic::message);

    private WidgetCatalogComposition() {
    }

    public static CatalogBuildResult compose(
            WidgetCatalog builtIn,
            Collection<? extends WidgetCatalogContributor> contributors) {
        Objects.requireNonNull(builtIn, "builtIn");
        Objects.requireNonNull(contributors, "contributors");

        ArrayList<CatalogDiagnostic> diagnostics = new ArrayList<>();
        ArrayList<ContributorSnapshot> snapshots = snapshotContributors(contributors, diagnostics);
        rejectDuplicateContributorIds(snapshots, diagnostics);

        TreeMap<String, ArrayList<OwnedDefinition>> candidatesByType = new TreeMap<>();
        for (ContributorSnapshot snapshot : snapshots) {
            if (snapshot.rejected) {
                continue;
            }
            validateAndIndex(snapshot, candidatesByType, diagnostics);
        }
        rejectInconsistentPaletteCategories(builtIn, candidatesByType, diagnostics);

        NavigableMap<String, WidgetDefinition> result = new TreeMap<>(builtIn.indexedDefinitions());
        for (Map.Entry<String, ArrayList<OwnedDefinition>> entry : candidatesByType.entrySet()) {
            String typeId = entry.getKey();
            ArrayList<OwnedDefinition> owners = entry.getValue();
            owners.sort(Comparator.comparing(value -> value.contributorId));
            if (result.containsKey(typeId)) {
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.RESERVED_WIDGET_TYPE,
                        typeId,
                        ownerIds(owners),
                        "An extension cannot replace a built-in widget type"));
            } else if (owners.size() > 1) {
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.DUPLICATE_WIDGET_TYPE,
                        typeId,
                        ownerIds(owners),
                        "No extension wins a duplicate widget type id"));
            } else {
                result.put(typeId, owners.getFirst().definition);
            }
        }

        diagnostics.sort(DIAGNOSTIC_ORDER);
        return new CatalogBuildResult(WidgetCatalog.fromIndexed(result), diagnostics);
    }

    private static void rejectInconsistentPaletteCategories(
            WidgetCatalog builtIn,
            TreeMap<String, ArrayList<OwnedDefinition>> candidatesByType,
            ArrayList<CatalogDiagnostic> diagnostics) {
        TreeMap<String, Integer> builtInOrders = new TreeMap<>();
        for (WidgetDefinition definition : builtIn.definitions()) {
            PaletteMetadata palette = definition.palette();
            builtInOrders.put(palette.categoryId(), palette.categoryOrder());
        }

        TreeMap<String, TreeMap<Integer, TreeSet<String>>> extensionOwners = new TreeMap<>();
        for (ArrayList<OwnedDefinition> owners : candidatesByType.values()) {
            for (OwnedDefinition owner : owners) {
                PaletteMetadata palette = owner.definition.palette();
                extensionOwners
                        .computeIfAbsent(palette.categoryId(), ignored -> new TreeMap<>())
                        .computeIfAbsent(palette.categoryOrder(), ignored -> new TreeSet<>())
                        .add(owner.contributorId);
            }
        }

        TreeSet<String> rejectedContributors = new TreeSet<>();
        for (Map.Entry<String, TreeMap<Integer, TreeSet<String>>> entry : extensionOwners.entrySet()) {
            String categoryId = entry.getKey();
            TreeMap<Integer, TreeSet<String>> ownersByOrder = entry.getValue();
            Integer builtInOrder = builtInOrders.get(categoryId);
            if (builtInOrder != null) {
                TreeSet<String> mismatching = new TreeSet<>();
                TreeSet<Integer> mismatchingOrders = new TreeSet<>();
                for (Map.Entry<Integer, TreeSet<String>> orderEntry : ownersByOrder.entrySet()) {
                    if (!orderEntry.getKey().equals(builtInOrder)) {
                        mismatchingOrders.add(orderEntry.getKey());
                        mismatching.addAll(orderEntry.getValue());
                    }
                }
                if (!mismatching.isEmpty()) {
                    rejectedContributors.addAll(mismatching);
                    diagnostics.add(diagnostic(
                            CatalogDiagnosticCode.INCONSISTENT_PALETTE_CATEGORY,
                            categoryId,
                            List.copyOf(mismatching),
                            "Built-in order " + builtInOrder
                            + " conflicts with extension orders " + mismatchingOrders));
                }
            } else if (ownersByOrder.size() > 1) {
                TreeSet<String> conflicting = new TreeSet<>();
                for (TreeSet<String> owners : ownersByOrder.values()) {
                    conflicting.addAll(owners);
                }
                rejectedContributors.addAll(conflicting);
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INCONSISTENT_PALETTE_CATEGORY,
                        categoryId,
                        List.copyOf(conflicting),
                        "Extension orders conflict: " + ownersByOrder.navigableKeySet()));
            }
        }

        if (rejectedContributors.isEmpty()) {
            return;
        }
        candidatesByType.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(owner -> rejectedContributors.contains(owner.contributorId));
            return entry.getValue().isEmpty();
        });
    }

    private static ArrayList<ContributorSnapshot> snapshotContributors(
            Collection<? extends WidgetCatalogContributor> contributors,
            ArrayList<CatalogDiagnostic> diagnostics) {
        ArrayList<ContributorSnapshot> result = new ArrayList<>();
        Object[] suppliedContributors;
        try {
            suppliedContributors = contributors.toArray();
        } catch (RuntimeException | LinkageError failure) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                    "<contributors>",
                    List.of(),
                    "Contributor snapshot failed: " + failure.getClass().getSimpleName()));
            return result;
        }
        if (suppliedContributors == null) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                    "<contributors>",
                    List.of(),
                    "Contributor snapshot returned null"));
            return result;
        }
        for (Object supplied : suppliedContributors) {
            if (supplied == null) {
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                        "<null-contributor>",
                        List.of(),
                        "Contributor is null"));
                continue;
            }
            if (!(supplied instanceof WidgetCatalogContributor contributor)) {
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                        supplied.getClass().getName(),
                        List.of(),
                        "Contributor collection contains a non-contributor value"));
                continue;
            }

            String id;
            int version;
            try {
                id = contributor.contributorId();
                version = contributor.apiVersion();
            } catch (RuntimeException | LinkageError failure) {
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                        contributor.getClass().getName(),
                        List.of(),
                        "Contributor metadata failed: " + failure.getClass().getSimpleName()));
                continue;
            }

            ContributorSnapshot snapshot = new ContributorSnapshot(contributor, id, version);
            result.add(snapshot);
            if (id == null || !CONTRIBUTOR_ID.matcher(id).matches()) {
                snapshot.rejected = true;
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                        String.valueOf(id),
                        id == null ? List.of() : List.of(id),
                        "Contributor id must be a reverse-DNS identifier"));
            } else if (version != WidgetCatalog.API_VERSION) {
                snapshot.rejected = true;
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.UNSUPPORTED_API_VERSION,
                        id,
                        List.of(id),
                        "Expected catalog API " + WidgetCatalog.API_VERSION + " but received " + version));
            }
        }
        result.sort(Comparator.comparing(value -> value.id == null ? "" : value.id));
        return result;
    }

    private static void rejectDuplicateContributorIds(
            List<ContributorSnapshot> snapshots,
            ArrayList<CatalogDiagnostic> diagnostics) {
        HashMap<String, ArrayList<ContributorSnapshot>> byId = new HashMap<>();
        for (ContributorSnapshot snapshot : snapshots) {
            if (snapshot.id != null) {
                byId.computeIfAbsent(snapshot.id, ignored -> new ArrayList<>()).add(snapshot);
            }
        }
        for (Map.Entry<String, ArrayList<ContributorSnapshot>> entry : byId.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (ContributorSnapshot snapshot : entry.getValue()) {
                    snapshot.rejected = true;
                }
                diagnostics.add(diagnostic(
                        CatalogDiagnosticCode.DUPLICATE_CONTRIBUTOR_ID,
                        entry.getKey(),
                        List.of(entry.getKey()),
                        "All contributors with a duplicate id were rejected"));
            }
        }
    }

    private static void validateAndIndex(
            ContributorSnapshot snapshot,
            TreeMap<String, ArrayList<OwnedDefinition>> candidatesByType,
            ArrayList<CatalogDiagnostic> diagnostics) {
        Collection<WidgetDefinition> supplied;
        try {
            supplied = snapshot.source.definitions();
        } catch (RuntimeException | LinkageError failure) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_DEFINITION,
                    snapshot.id,
                    List.of(snapshot.id),
                    "Definition loading failed: " + failure.getClass().getSimpleName()));
            return;
        }
        if (supplied == null) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_DEFINITION,
                    snapshot.id,
                    List.of(snapshot.id),
                    "Definitions collection is null"));
            return;
        }

        Object[] suppliedDefinitions;
        try {
            suppliedDefinitions = supplied.toArray();
        } catch (RuntimeException | LinkageError failure) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_DEFINITION,
                    snapshot.id,
                    List.of(snapshot.id),
                    "Definition snapshot failed: " + failure.getClass().getSimpleName()));
            return;
        }
        if (suppliedDefinitions == null) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_DEFINITION,
                    snapshot.id,
                    List.of(snapshot.id),
                    "Definition snapshot returned null"));
            return;
        }
        ArrayList<CatalogDiagnostic> localDiagnostics = new ArrayList<>();
        ArrayList<WidgetDefinition> definitions = new ArrayList<>(suppliedDefinitions.length);
        for (Object suppliedDefinition : suppliedDefinitions) {
            if (suppliedDefinition == null) {
                localDiagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INVALID_DEFINITION,
                        snapshot.id,
                        List.of(snapshot.id),
                        "Definitions collection contains null"));
                continue;
            }
            if (!(suppliedDefinition instanceof WidgetDefinition definition)) {
                localDiagnostics.add(diagnostic(
                        CatalogDiagnosticCode.INVALID_DEFINITION,
                        snapshot.id,
                        List.of(snapshot.id),
                        "Definitions collection contains " + suppliedDefinition.getClass().getName()
                        + " instead of WidgetDefinition"));
                continue;
            }
            definitions.add(definition);
        }
        if (!localDiagnostics.isEmpty()) {
            diagnostics.addAll(localDiagnostics);
            return;
        }
        try {
            definitions.sort(Comparator.comparing(value -> value.typeId().value()));
        } catch (RuntimeException | LinkageError failure) {
            diagnostics.add(diagnostic(
                    CatalogDiagnosticCode.INVALID_DEFINITION,
                    snapshot.id,
                    List.of(snapshot.id),
                    "Definition ordering failed: " + failure.getClass().getSimpleName()));
            return;
        }

        HashSet<String> localIds = new HashSet<>();
        for (WidgetDefinition definition : definitions) {
            String typeId = definition.typeId().value();
            if (!localIds.add(typeId)) {
                localDiagnostics.add(diagnostic(
                        CatalogDiagnosticCode.DUPLICATE_WIDGET_TYPE,
                        typeId,
                        List.of(snapshot.id),
                        "A contributor declared the same widget type more than once"));
            }
            if (typeId.startsWith("flutter.")) {
                localDiagnostics.add(diagnostic(
                        CatalogDiagnosticCode.RESERVED_WIDGET_TYPE,
                        typeId,
                        List.of(snapshot.id),
                        "The flutter.* widget namespace is reserved"));
            } else if (!typeId.startsWith(snapshot.id + '.')) {
                localDiagnostics.add(diagnostic(
                        CatalogDiagnosticCode.FOREIGN_WIDGET_NAMESPACE,
                        typeId,
                        List.of(snapshot.id),
                        "An extension widget type must be owned by its contributor namespace"));
            }
        }
        if (!localDiagnostics.isEmpty()) {
            diagnostics.addAll(localDiagnostics);
            return; // A contributor is accepted atomically or not at all.
        }

        for (WidgetDefinition definition : definitions) {
            candidatesByType.computeIfAbsent(definition.typeId().value(), ignored -> new ArrayList<>())
                    .add(new OwnedDefinition(snapshot.id, definition));
        }
    }

    private static CatalogDiagnostic diagnostic(
            CatalogDiagnosticCode code,
            String subject,
            List<String> contributors,
            String message) {
        return new CatalogDiagnostic(code, subject, contributors, message);
    }

    private static List<String> ownerIds(List<OwnedDefinition> owners) {
        ArrayList<String> result = new ArrayList<>();
        for (OwnedDefinition owner : owners) {
            result.add(owner.contributorId);
        }
        return List.copyOf(result);
    }

    private static final class ContributorSnapshot {
        private final WidgetCatalogContributor source;
        private final String id;
        @SuppressWarnings("unused")
        private final int version;
        private boolean rejected;

        private ContributorSnapshot(WidgetCatalogContributor source, String id, int version) {
            this.source = source;
            this.id = id;
            this.version = version;
        }
    }

    private record OwnedDefinition(String contributorId, WidgetDefinition definition) {
    }
}
