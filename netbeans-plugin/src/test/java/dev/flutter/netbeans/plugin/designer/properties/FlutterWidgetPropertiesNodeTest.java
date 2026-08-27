package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterWidgetPropertiesNodeTest {

    @Test
    void exposesStandardLookupIdentityAndPaletteDisplayName() {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse("86bb276e-44b6-4c55-9fdb-aea8ec62ab90");
        WidgetNode widget = WidgetNode.empty(id, definition.typeId());

        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition);

        assertEquals("Text", node.getDisplayName());
        assertEquals(id.toString(), node.getName());
        assertEquals("Text — " + id, node.getShortDescription());
        assertSame(id, node.getLookup().lookup(StableId.class));
        assertSame(widget, node.getLookup().lookup(WidgetNode.class));
        assertSame(definition, node.getLookup().lookup(WidgetDefinition.class));
    }

    @Test
    void exposesIdentityThenCatalogPropertiesInCatalogOrderAsReadOnlyStrings() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse("c613fb29-9fde-4a4c-a94e-76bd02626153");
        LinkedHashMap<PropertyName, PropertyValue> explicit = new LinkedHashMap<>();
        explicit.put(new PropertyName("data"), new PropertyValue.StringValue("Hello\nFlutter"));
        explicit.put(new PropertyName("softWrap"), new PropertyValue.BooleanValue(false));
        WidgetNode widget = new WidgetNode(
                id, definition.typeId(), explicit, Map.of(), Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition);

        Node.PropertySet[] sets = node.getPropertySets();

        assertEquals(2, sets.length);
        assertEquals(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME, sets[0].getName());
        assertEquals(FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME, sets[1].getName());
        assertEquals(
                java.util.List.of("stableId", "type"),
                names(sets[0].getProperties()));
        assertEquals(
                java.util.List.of("data", "textAlign", "softWrap", "maxLines", "overflow"),
                names(sets[1].getProperties()));
        assertEquals(id.toString(), sets[0].getProperties()[0].getValue());
        assertEquals("flutter.widgets.Text", sets[0].getProperties()[1].getValue());
        assertEquals("\"Hello\\nFlutter\"", sets[1].getProperties()[0].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[1].getValue());
        assertEquals("false", sets[1].getProperties()[2].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[3].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[4].getValue());
        for (Node.PropertySet set : sets) {
            for (Node.Property<?> property : set.getProperties()) {
                assertFalse(property.canWrite(), property.getName());
            }
        }
    }

    @Test
    void doesNotPresentCreationDefaultAsCurrentOrEffectiveValue() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Padding");
        WidgetNode widget = WidgetNode.empty(
                StableId.parse("23b6a0ce-819f-443e-a2ae-7ba184ea84de"),
                definition.typeId());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition);

        Node.Property<?> padding = node.getPropertySets()[1].getProperties()[0];

        assertEquals("padding", padding.getName());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, padding.getValue());
        assertFalse(padding.canWrite());
    }

    @Test
    void rejectsCatalogDefinitionForAnotherWidgetType() {
        WidgetNode widget = WidgetNode.empty(
                StableId.parse("2c2a450c-4660-4a57-9d86-ea312f8cba88"),
                new WidgetTypeId("flutter.widgets.Text"));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new FlutterWidgetPropertiesNode(
                        Children.LEAF,
                        widget,
                        definition("flutter.widgets.Center")));

        assertEquals(
                "Widget type flutter.widgets.Text does not match catalog definition flutter.widgets.Center",
                failure.getMessage());
    }

    @Test
    void sixCoreDesignTreeNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
            throws ReflectiveOperationException {
        List<String> typeIds = List.of(
                "flutter.material.Scaffold",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.Text");
        Set<String> iconPaths = new HashSet<>();

        for (String typeId : typeIds) {
            WidgetDefinition definition = definition(typeId);
            WidgetNode widget = WidgetNode.empty(
                    StableId.parse("5cf3483b-d627-41b4-bb1d-4a321aa36da4"),
                    definition.typeId());
            FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                    Children.LEAF, widget, definition);
            String expectedIcon = FlutterWidgetIconRegistry
                    .findIconPath(definition.typeId())
                    .orElseThrow();

            assertEquals(expectedIcon, declaredIconPath(node), typeId);
            iconPaths.add(declaredIconPath(node));
        }

        assertEquals(6, iconPaths.size(),
                "Design tree nodes must not share a generic widget icon");
    }

    private static WidgetDefinition definition(String typeId) {
        return BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(typeId))
                .orElseThrow();
    }

    private static java.util.List<String> names(Node.Property<?>[] properties) {
        return java.util.Arrays.stream(properties).map(Node.Property::getName).toList();
    }

    private static String declaredIconPath(Node node) throws ReflectiveOperationException {
        Field base = AbstractNode.class.getDeclaredField("iconBase");
        Field extension = AbstractNode.class.getDeclaredField("iconExtension");
        base.setAccessible(true);
        extension.setAccessible(true);
        return base.get(node) + (String) extension.get(node);
    }
}
