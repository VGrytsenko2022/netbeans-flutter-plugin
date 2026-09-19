package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static io.github.vgrytsenko2022.designer.catalog.MenuBarTestValues.definition;
import static io.github.vgrytsenko2022.designer.catalog.MenuBarTestValues.document;
import static io.github.vgrytsenko2022.designer.catalog.MenuBarTestValues.node;
import static io.github.vgrytsenko2022.designer.catalog.MenuBarTestValues.p;
import static io.github.vgrytsenko2022.designer.catalog.MenuBarTestValues.reference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuBarContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void completeConstConstructorAndRequiredChildrenListKeepAllMenuStyleLeaves() {
        WidgetDefinition definition = definition();
        assertEquals(206, definition.properties().size());
        assertEquals(203, MenuBarWidgetPropertySchema.localStyleProperties().size());
        assertEquals(new ArrayList<>(MenuBarWidgetPropertySchema.definitions().keySet()),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(new PaletteMetadata("flutter.material", 100, 360, "MenuBar"), definition.palette());
        assertTrue(definition.constConstructor());
        for (int index = 0; index < 206; index++) {
            assertEquals(DartParameter.named(index, false), definition.properties().get(index).parameter());
        }
        assertEquals(List.of("children"), definition.slots().stream().map(value -> value.name().value()).toList());
        assertEquals(DartParameter.named(206, true), definition.slots().get(0).parameter());
        assertEquals(SlotCardinality.LIST, definition.slots().get(0).cardinality());
        assertEquals(0, definition.slots().get(0).minChildren());
        assertEquals(10_000, definition.slots().get(0).maxChildren());

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, io.github.vgrytsenko2022.designer.model.StableId.random());
        assertTrue(new WidgetTreeValidator().validate(document(prototype), CATALOG).valid());
        String source = generated(prototype).build().payload();
        assertTrue(source.contains("const MenuBar("), source);
        assertTrue(source.contains("children: []"), source);
        assertFalse(source.contains("onPressed:"), source);
        assertFalse(definition.property(p("enabled")).isPresent());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void wholeStyleAndLocalLeavesAreAtomicAndReferencesStayUnexecuted() {
        assertFalse(valid(node(Map.of(p("style"), reference("menuStyle"),
                p("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)))));
        WidgetNode local = node(Map.of(p("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE)));
        assertTrue(valid(local));
        String source = generated(local).build().payload();
        assertTrue(source.contains("MenuStyle("), source);
        assertTrue(source.contains("children: []"), source);
        WidgetNode whole = node(Map.of(p("style"), reference("menuStyle"), p("controller"), reference("menuController")));
        String referenced = generated(whole).build().payload();
        assertTrue(referenced.contains("style: menuStyle"), referenced);
        assertTrue(referenced.contains("controller: menuController"), referenced);
        assertFalse(referenced.contains("onOpen:"), referenced);
    }

    private static boolean valid(WidgetNode root) {
        return new WidgetTreeValidator().validate(document(root), CATALOG).valid();
    }

    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics());
        return result.generated().orElseThrow();
    }
}
