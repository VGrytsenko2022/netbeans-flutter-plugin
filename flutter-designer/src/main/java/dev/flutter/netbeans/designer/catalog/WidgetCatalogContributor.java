package dev.flutter.netbeans.designer.catalog;

import java.util.Collection;

/** Future extension SPI; discovery and class-loader policy belong to the IDE edge. */
public interface WidgetCatalogContributor {
    String contributorId();

    int apiVersion();

    Collection<WidgetDefinition> definitions();
}
