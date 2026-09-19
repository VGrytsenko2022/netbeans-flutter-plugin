package io.github.vgrytsenko2022.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.PropertyDefinition;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Arrays;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class BuilderPropertyContractTest {

    private static final String TYPE = "flutter.widgets.Builder";
    private static final PropertyName BUILDER = new PropertyName("builder");

    @Test
    void requiredBuilderUsesTypedCallbackEditorAndRetainsNoopCreationValue() throws Exception {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(TYPE)).orElseThrow();
        PropertyDefinition property = definition.property(BUILDER).orElseThrow();
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("b3bb7e69-799a-4c11-bf8f-bd9f6caab8e9"),
                java.util.Map.of(BUILDER, new PropertyValue.CallbackValue("noop")));
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, new ArrayList<>()::add);

        Node.Property<?> row = Arrays.stream(node.getPropertySets())
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(candidate -> candidate.getName().equals(BUILDER.value()))
                .findFirst()
                .orElseThrow();

        assertTrue(property.parameter().required());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.CALLBACK,
                FlutterTypedPropertyEditors.binding(property).orElseThrow().editorKind());
        assertTrue(row.canWrite());
        assertTrue(!row.supportsDefaultValue(),
                "required builder callback cannot be reset to <not set>");
        assertEquals(FlutterPropertyCellValue.class, row.getValueType());
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.CallbackValue("noop")),
                row.getValue());
        assertNotNull(row.getPropertyEditor());
        assertTrue(row.getShortDescription().contains("required callback"));
        assertTrue(row.getShortDescription().contains("isolated Canvas"));
    }
}
