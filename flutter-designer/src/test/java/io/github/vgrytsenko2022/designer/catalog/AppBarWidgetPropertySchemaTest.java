package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppBarWidgetPropertySchemaTest {

    @Test
    void coversEveryFlattenedCatalogPropertyWithClosedMetadata() {
        WidgetDefinition appBar = BuiltInWidgetCatalog.getDefault()
                .find(AppBarWidgetPropertySchema.APP_BAR_TYPE).orElseThrow();

        assertEquals(28, AppBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(5, AppBarWidgetPropertySchema.SLOT_COUNT);
        assertEquals(120, AppBarWidgetPropertySchema.definitions().size());
        assertEquals(
                appBar.properties().stream().map(property -> property.name().value())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                AppBarWidgetPropertySchema.definitions().keySet());
        assertTrue(AppBarWidgetPropertySchema.definitions().values().stream()
                .allMatch(value -> !value.displayName().isBlank()
                        && !value.description().isBlank()
                        && !value.dartName().isBlank()));
    }

    @Test
    void exposesTheExactCompoundPrefixesWithoutExecutableValues() {
        assertFalse(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("backgroundColor")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("notificationPredicate")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("shapeKind")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("iconThemeWeight")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("actionsIconThemeColor")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("toolbarTextStyleThemeTextStyle")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("titleTextStyleLocaleLanguageCode")));
        assertTrue(AppBarWidgetPropertySchema.isCompound(
                new PropertyName("systemOverlayStyleStatusBarBrightness")));

        assertEquals(Set.of(
                AppBarWidgetPropertySchema.Target.DIRECT,
                AppBarWidgetPropertySchema.Target.NOTIFICATION_PREDICATE,
                AppBarWidgetPropertySchema.Target.SHAPE_KIND,
                AppBarWidgetPropertySchema.Target.SHAPE,
                AppBarWidgetPropertySchema.Target.ICON_THEME,
                AppBarWidgetPropertySchema.Target.ACTIONS_ICON_THEME,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE_THEME,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE_LOCALE,
                AppBarWidgetPropertySchema.Target.TOOLBAR_TEXT_STYLE_DECORATION,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE_THEME,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE_LOCALE,
                AppBarWidgetPropertySchema.Target.TITLE_TEXT_STYLE_DECORATION,
                AppBarWidgetPropertySchema.Target.SYSTEM_UI_OVERLAY_STYLE),
                AppBarWidgetPropertySchema.definitions().values().stream()
                        .map(AppBarWidgetPropertySchema.Definition::target)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}
