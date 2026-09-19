package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderWidgetPropertySchemaTest {

    @Test
    void catalogExposesRequiredWidgetBuilderCallbackWithoutSlots() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(BuilderWidgetPropertySchema.BUILDER_TYPE)
                .orElseThrow();

        assertEquals("Builder", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 75, "Builder"),
                definition.palette());
        assertEquals(java.util.Set.of(io.github.vgrytsenko2022.designer.model.PropertyValueKind.CALLBACK),
                definition.property(new PropertyName("builder")).orElseThrow().acceptedKinds());
        assertTrue(definition.property(new PropertyName("builder")).orElseThrow().parameter().required());
        assertEquals(new PropertyValue.CallbackValue("noop"),
                definition.property(new PropertyName("builder")).orElseThrow().creationDefault().orElseThrow());
        assertTrue(definition.slots().isEmpty());
    }

    @Test
    void prototypeKeepsRequiredNoopCallbackAndValidationRejectsMissingOrInvalidValues() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(BuilderWidgetPropertySchema.BUILDER_TYPE)
                .orElseThrow();
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(new PropertyName("builder"), new PropertyValue.CallbackValue("noop")),
                prototype.properties());
        assertTrue(new WidgetTreeValidator().validate(
                document(prototype),
                BuiltInWidgetCatalog.getDefault()).valid());

        WidgetNode missing = new WidgetNode(prototype.id(), prototype.type(), Map.of(), prototype.slots());
        assertFalse(new WidgetTreeValidator().validate(
                document(missing),
                BuiltInWidgetCatalog.getDefault()).valid());
    }

    private static DesignerDocument document(WidgetNode root) {
        var regions = new ManagedRegions(new ManagedRegion("0".repeat(64)),
                new ManagedRegion("0".repeat(64)));
        return new DesignerDocument(StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), regions), root);
    }
}
