package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetDefinitionTest {
    @Test
    void validatesPositionalParametersAcrossPropertiesAndSlotsAsOneSequence() {
        PropertyDefinition second = property("label", DartParameter.positional(1));
        SlotDefinition first = slot("child", DartParameter.positional(0));

        WidgetDefinition definition = definition(List.of(second), List.of(first));

        assertEquals("child", definition.slots().getFirst().name().value());
        assertEquals("label", definition.properties().getFirst().name().value());
    }

    @Test
    void rejectsGapInCombinedPositionalSequence() {
        assertThrows(IllegalArgumentException.class,
                () -> definition(List.of(property("label", DartParameter.positional(1))), List.of()));
    }

    @Test
    void rejectsRequiredPositionalParameterAfterOptionalOne() {
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(property("first", DartParameter.positional(0, false))),
                List.of(slot("second", DartParameter.positional(1)))));
    }

    @Test
    void rejectsDuplicateNamedParameterOrderAcrossPropertyAndSlot() {
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(property("label", DartParameter.named(0, false))),
                List.of(slot("child", DartParameter.named(0, false)))));
    }

    @Test
    void rejectsArgumentModeledAsBothPropertyAndSlot() {
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(property("child", DartParameter.named(0, false))),
                List.of(slot("child", DartParameter.named(1, false)))));
    }

    @Test
    void defensivelyCopiesAndDeterministicallyOrdersMetadata() {
        ArrayList<String> imports = new ArrayList<>(List.of("package:z/z.dart", "package:a/a.dart"));
        ArrayList<String> traits = new ArrayList<>(List.of("z.Trait", "a.Trait"));
        ArrayList<PropertyDefinition> properties = new ArrayList<>(List.of(
                property("later", DartParameter.named(1, false)),
                property("first", DartParameter.named(0, false))));

        WidgetDefinition definition = new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:a/a.dart",
                imports,
                traits,
                new PaletteMetadata("example", 1, 1, "Widget"),
                properties,
                List.of());
        imports.clear();
        traits.clear();
        properties.clear();

        assertEquals("package:a/a.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:a/a.dart", "package:z/z.dart"), definition.importUris());
        assertEquals(List.of("a.Trait", "z.Trait"), List.copyOf(definition.traits()));
        assertEquals(List.of("first", "later"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> definition.properties().add(property("other", DartParameter.named(2, false))));
        assertThrows(UnsupportedOperationException.class,
                () -> definition.importUris().add("package:other/other.dart"));
    }

    @Test
    void requiresWidgetOwnerAndEveryEnumOwnerInImmutableImports() {
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:owner/widget.dart';boom",
                List.of("package:owner/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:owner/widget.dart",
                List.of("package:dependency/enum.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));

        PropertyDefinition enumProperty = new PropertyDefinition(
                new PropertyName("axis"),
                DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.EnumValues(
                        new DartSymbolReference("package:dependency/enum.dart", "Axis"),
                        List.of("horizontal", "vertical"))),
                Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:owner/widget.dart",
                List.of("package:owner/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(enumProperty),
                List.of()));

        WidgetDefinition valid = new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:owner/widget.dart",
                List.of("package:dependency/enum.dart", "package:owner/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(enumProperty),
                List.of());
        assertEquals("package:owner/widget.dart", valid.dartLibraryUri());
        assertEquals(List.of("package:dependency/enum.dart", "package:owner/widget.dart"),
                valid.importUris());
    }

    @Test
    void widgetOwnerParticipatesInEqualityAndHashCode() {
        WidgetDefinition first = definitionWithOwner("package:a/a.dart");
        WidgetDefinition equal = definitionWithOwner("package:a/a.dart");
        WidgetDefinition otherOwner = definitionWithOwner("package:b/b.dart");

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, otherOwner);
        assertNotEquals(first.hashCode(), otherOwner.hashCode());
    }

    @Test
    void oneCharacterTraitIsValid() {
        assertTrue(new SlotAcceptance.HasTrait("A").trait().equals("A"));
    }

    @Test
    void boundsMetadataIndependentlyFromPersistedValueAndCommandBudgets() {
        assertEquals(1024, WidgetDefinition.MAX_PROPERTIES);
        assertEquals(512, dev.flutter.netbeans.designer.validation.ValidationLimits.defaults().maxPropertiesPerWidget());
        assertEquals(512, dev.flutter.netbeans.designer.codec.FdCodecLimits.defaults().maxPropertiesPerWidget());
        ArrayList<PropertyDefinition> tooManyProperties = new ArrayList<>();
        for (int index = 0; index <= WidgetDefinition.MAX_PROPERTIES; index++) {
            tooManyProperties.add(property("p" + index, DartParameter.named(index, false)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> definition(tooManyProperties, List.of()));

        ArrayList<SlotDefinition> tooManySlots = new ArrayList<>();
        for (int index = 0; index <= WidgetDefinition.MAX_SLOTS; index++) {
            tooManySlots.add(slot("s" + index, DartParameter.named(index, false)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> definition(List.of(), tooManySlots));
    }

    @Test
    void rejectsReservedDartIdentifiersAndHostileImportUris() {
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "class",
                Optional.empty(),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "type",
                Optional.empty(),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "_PrivateWidget",
                Optional.empty(),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.of("switch"),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.of("_internal"),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart", "package:x/x.dart';throw 'boom"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(property("class", DartParameter.named(0, false))),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(),
                List.of(slot("required", DartParameter.named(0, false)))));
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(property("_privateValue", DartParameter.named(0, false))),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> definition(
                List.of(),
                List.of(slot("_privateChild", DartParameter.named(0, false)))));
    }

    private static WidgetDefinition definition(
            List<PropertyDefinition> properties,
            List<SlotDefinition> slots) {
        return new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                "package:example/widget.dart",
                List.of("package:example/widget.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                properties,
                slots);
    }

    private static WidgetDefinition definitionWithOwner(String owner) {
        return new WidgetDefinition(
                new WidgetTypeId("com.example.Widget"),
                "Widget",
                Optional.empty(),
                false,
                owner,
                List.of("package:a/a.dart", "package:b/b.dart"),
                Set.of(),
                new PaletteMetadata("example", 1, 1, "Widget"),
                List.of(),
                List.of());
    }

    private static PropertyDefinition property(String name, DartParameter parameter) {
        return new PropertyDefinition(
                new PropertyName(name),
                parameter,
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)),
                Optional.empty());
    }

    private static SlotDefinition slot(String name, DartParameter parameter) {
        return new SlotDefinition(
                new SlotName(name),
                parameter,
                SlotCardinality.SINGLE,
                0,
                1,
                new SlotAcceptance.AnyWidget());
    }
}
