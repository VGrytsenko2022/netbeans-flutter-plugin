package dev.flutter.netbeans.fixture.catalog;

import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalogContributor;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openide.util.lookup.ServiceProvider;

/** External-module fixture for the version 2 Flutter Designer catalog SPI. */
@ServiceProvider(service = WidgetCatalogContributor.class)
public final class FixtureWidgetCatalogContributor implements WidgetCatalogContributor {
    public static final String CONTRIBUTOR_ID = "dev.flutter.netbeans.fixture";
    public static final String WIDGET_TYPE = CONTRIBUTOR_ID + ".FixtureCard";

    private static final String DART_LIBRARY = "package:designer_catalog_fixture/widgets.dart";
    private static final WidgetDefinition FIXTURE_WIDGET = new WidgetDefinition(
            new WidgetTypeId(WIDGET_TYPE),
            "FixtureCard",
            Optional.empty(),
            true,
            DART_LIBRARY,
            List.of(DART_LIBRARY),
            Set.of(),
            new PaletteMetadata("fixture", 900, 10, "Fixture Card"),
            List.of(),
            List.of());

    @Override
    public String contributorId() {
        return CONTRIBUTOR_ID;
    }

    @Override
    public int apiVersion() {
        return WidgetCatalog.API_VERSION;
    }

    @Override
    public Collection<WidgetDefinition> definitions() {
        return List.of(FIXTURE_WIDGET);
    }
}
