package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcludeSemanticsWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448UnnamedConstConstructor() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE)
                .orElseThrow();

        assertEquals("ExcludeSemantics", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.accessibility", 400, 10, "ExcludeSemantics"),
                definition.palette());
        assertEquals(List.of("excluding"), definition.properties().stream()
                .map(property -> property.name().value()).toList());
        assertEquals(ExcludeSemanticsWidgetPropertySchema.definitions().keySet()
                        .stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition excluding = definition.property(
                new PropertyName("excluding")).orElseThrow();
        assertEquals(DartParameter.named(0, false), excluding.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), excluding.acceptedKinds());
        assertTrue(excluding.creationDefault().isEmpty(),
                "Omission must preserve Flutter's exact true default");
        PropertyValueConstraint booleanConstraint = excluding.constraints().getFirst();
        assertTrue(booleanConstraint.accepts(new PropertyValue.BooleanValue(true)));
        assertTrue(booleanConstraint.accepts(new PropertyValue.BooleanValue(false)));
        assertFalse(booleanConstraint.accepts(new PropertyValue.StringValue("true")));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(1, child.parameter().order());
        assertFalse(child.parameter().required());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(ExcludeSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                ExcludeSemanticsWidgetPropertySchema.definitions().size());
        assertEquals(List.of("excluding"),
                ExcludeSemanticsWidgetPropertySchema.definitions().keySet()
                        .stream().toList());
        ExcludeSemanticsWidgetPropertySchema.Definition metadata =
                ExcludeSemanticsWidgetPropertySchema.definitions().get("excluding");
        assertEquals(ExcludeSemanticsWidgetPropertySchema.Group.SEMANTICS,
                metadata.group());
        assertEquals("Excluding", metadata.displayName());
        assertEquals("excluding", metadata.dartName());
        assertEquals(0, metadata.dartOrder());
        assertFalse(metadata.description().isBlank());
        assertTrue(ExcludeSemanticsWidgetPropertySchema.find(
                new PropertyName("excluding")).isPresent());
        assertTrue(ExcludeSemanticsWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }
}
