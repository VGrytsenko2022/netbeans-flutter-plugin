package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetNodePrototypeFactoryTest {
    private static final StableId ID = StableId.parse("26a92207-e622-43a3-ac53-211cf86a99e9");

    @Test
    void createsTextWithItsRequiredCreationDefault() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition("flutter.widgets.Text"), ID);

        assertEquals(ID, prototype.id());
        assertEquals(new WidgetTypeId("flutter.widgets.Text"), prototype.type());
        assertEquals(List.of(new PropertyName("data")), prototype.properties().keySet().stream().toList());
        assertEquals(new PropertyValue.StringValue("Text"),
                prototype.properties().get(new PropertyName("data")));
        assertTrue(prototype.slots().isEmpty());
        assertTrue(prototype.extensions().values().isEmpty());
    }

    @Test
    void createsColumnWithAnEmptyListSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition("flutter.widgets.Column"), ID);

        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("children")), prototype.slots().keySet().stream().toList());
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children")));
        assertTrue(children.children().isEmpty());
    }

    @Test
    void createsCenterWithAnEmptySingleSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition("flutter.widgets.Center"), ID);

        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")), prototype.slots().keySet().stream().toList());
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child")));
        assertTrue(child.child().isEmpty());
    }

    @Test
    void createsAspectRatioWithItsRequiredDefaultAndEmptyChildSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.AspectRatio"), ID);

        assertEquals(Map.of(
                        new PropertyName("aspectRatio"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                prototype.properties());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsOpacityWithOpaqueDefaultAndEmptyChildSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Opacity"), ID);

        assertEquals(Map.of(
                        new PropertyName("opacity"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                prototype.properties());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsAlignWithoutMaterializingFlutterDefaultsAndWithEmptyChildSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Align"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Align"), prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsFractionallySizedBoxWithoutMaterializingDefaultsAndWithEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.FractionallySizedBox"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.FractionallySizedBox"),
                prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsFittedBoxWithoutMaterializingDefaultsAndWithEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.FittedBox"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.FittedBox"),
                prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsConstrainedBoxWithNeutralRequiredConstraintsAndEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.ConstrainedBox"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.ConstrainedBox"),
                prototype.type());
        assertEquals(List.of(new PropertyName("constraints")),
                prototype.properties().keySet().stream().toList());
        PropertyValue.BoxConstraintsValue constraints = assertInstanceOf(
                PropertyValue.BoxConstraintsValue.class,
                prototype.properties().get(new PropertyName("constraints")));
        assertEquals(0, constraints.minWidth().finiteValue().orElseThrow()
                .compareTo(BigDecimal.ZERO));
        assertTrue(constraints.maxWidth().infinite());
        assertEquals(0, constraints.minHeight().finiteValue().orElseThrow()
                .compareTo(BigDecimal.ZERO));
        assertTrue(constraints.maxHeight().infinite());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsUnconstrainedBoxWithoutMaterializingDefaultsAndWithEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.UnconstrainedBox"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.UnconstrainedBox"),
                prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsLimitedBoxWithoutMaterializingInfinityDefaultsAndWithEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.LimitedBox"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.LimitedBox"),
                prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsOverflowBoxWithoutMaterializingFrameworkDefaultsAndWithEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.OverflowBox"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.OverflowBox"),
                prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsStackWithoutMaterializingDefaultsAndWithEmptyChildrenList() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Stack"), ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Stack"), prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("children")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children"))).children().isEmpty());
    }

    @Test
    void createsTextFieldWithoutMaterializingFrameworkDefaultsOrSlots() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.material.TextField"), ID);

        assertEquals(new WidgetTypeId("flutter.material.TextField"), prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertTrue(prototype.slots().isEmpty());
        assertTrue(prototype.extensions().values().isEmpty());
    }

    @Test
    void createsExpandedOnlyAsAnIncompleteDetachedAtomicWrapperPayload() {
        WidgetDefinition definition = definition("flutter.widgets.Expanded");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Expanded"), prototype.type());
        assertTrue(prototype.properties().isEmpty(),
                "Flutter's flex default is intentionally not materialized");
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertEquals(1, definition.slot(new SlotName("child"))
                .orElseThrow().minChildren());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(definition));
    }

    @Test
    void createsFlexibleOnlyAsAnIncompleteDetachedAtomicWrapperPayload() {
        WidgetDefinition definition = definition("flutter.widgets.Flexible");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Flexible"), prototype.type());
        assertTrue(prototype.properties().isEmpty(),
                "Flutter's flex and loose-fit defaults are intentionally not materialized");
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertEquals(1, definition.slot(new SlotName("child"))
                .orElseThrow().minChildren());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(definition));
    }

    @Test
    void createsSpacerAsAnEmptyInsertableLeafWithoutMaterializingFlexDefault() {
        WidgetDefinition definition = definition("flutter.widgets.Spacer");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Spacer"), prototype.type());
        assertTrue(prototype.properties().isEmpty(),
                "Flutter's flex default is intentionally not materialized");
        assertTrue(prototype.slots().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void imageRequiresAnExplicitReviewedProviderAndNeverFabricatesOne() {
        WidgetDefinition image = definition("flutter.widgets.Image");

        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(image, ID));
        assertTrue(missing.getMessage().contains(
                "Required creation property 'image' is missing"));
        AtomicInteger allocations = new AtomicInteger();
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(image, () -> {
                    allocations.incrementAndGet();
                    return ID;
                }));
        assertEquals(0, allocations.get(),
                "required creation values are validated before id allocation");

        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.asset("assets/photo.png");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                image,
                ID,
                Map.of(
                        new PropertyName("image"), provider,
                        new PropertyName("semanticLabel"),
                                new PropertyValue.StringValue("Photo")));

        assertEquals(Map.of(
                        new PropertyName("image"), provider,
                        new PropertyName("semanticLabel"),
                                new PropertyValue.StringValue("Photo")),
                prototype.properties());
        assertTrue(prototype.slots().isEmpty());
    }

    @Test
    void explicitCreationValuesAreGenericValidatedAndOverrideCatalogDefaults() {
        WidgetDefinition ratio = definition("flutter.widgets.AspectRatio");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                ratio,
                ID,
                Map.of(new PropertyName("aspectRatio"),
                        new PropertyValue.DoubleValue(new BigDecimal("1.5"))));
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("1.5")),
                prototype.properties().get(new PropertyName("aspectRatio")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        ratio,
                        ID,
                        Map.of(new PropertyName("unknown"),
                                new PropertyValue.BooleanValue(true))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        ratio,
                        ID,
                        Map.of(new PropertyName("aspectRatio"),
                                new PropertyValue.BooleanValue(true))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        ratio,
                        ID,
                        Map.of(new PropertyName("aspectRatio"),
                                new PropertyValue.DoubleValue(BigDecimal.ZERO))));
    }

    @Test
    void createsContainerWithoutMaterializingFlutterDefaultsAndWithEmptyChild() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Container"), ID);

        assertEquals(ContainerWidgetPropertySchema.CONTAINER_TYPE, prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
    }

    @Test
    void createsAppBarWithoutMaterializingThemeDefaultsAndWithFiveEmptySlots() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.material.AppBar"), ID);

        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(
                new SlotName("leading"),
                new SlotName("title"),
                new SlotName("actions"),
                new SlotName("flexibleSpace"),
                new SlotName("bottom")),
                prototype.slots().keySet().stream().toList());
        assertInstanceOf(WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("actions")));
        assertTrue(((WidgetSlot.ListSlot) prototype.slots()
                .get(new SlotName("actions"))).children().isEmpty());
        for (String slot : List.of("leading", "title", "flexibleSpace", "bottom")) {
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    prototype.slots().get(new SlotName(slot))).child().isEmpty());
        }
    }

    @Test
    void sameDefinitionAndIdProduceEqualDetachedImmutablePrototypes() {
        WidgetDefinition definition = definition("flutter.widgets.Column");

        WidgetNode first = WidgetNodePrototypeFactory.create(definition, ID);
        WidgetNode second = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(first, second);
        assertNotSame(first, second);
        assertNotSame(first.properties(), second.properties());
        assertNotSame(first.slots(), second.slots());
        assertNotSame(
                first.slots().get(new SlotName("children")),
                second.slots().get(new SlotName("children")));
        assertThrows(UnsupportedOperationException.class,
                () -> first.properties().put(new PropertyName("extra"), new PropertyValue.StringValue("value")));
        assertThrows(UnsupportedOperationException.class,
                () -> first.slots().clear());
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class,
                first.slots().get(new SlotName("children")));
        assertThrows(UnsupportedOperationException.class,
                () -> children.children().add(WidgetNode.empty(StableId.random(), definition.typeId())));
    }

    @Test
    void supplierIsInvokedExactlyOnceAndItsIdIsUsed() {
        AtomicInteger calls = new AtomicInteger();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Text"),
                () -> {
                    calls.incrementAndGet();
                    return ID;
                });

        assertEquals(1, calls.get());
        assertEquals(ID, prototype.id());
    }

    @Test
    void rejectsNullDefinitionBeforeInvokingSupplier() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<StableId> supplier = () -> {
            calls.incrementAndGet();
            return ID;
        };

        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(null, supplier));
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsNullIdSupplierAndNullSuppliedOrGeneratedIds() {
        WidgetDefinition definition = definition("flutter.widgets.Text");

        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(definition, (StableId) null));
        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(definition, (Supplier<StableId>) null));
        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(definition, () -> null));
    }

    private static WidgetDefinition definition(String typeId) {
        return BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(typeId))
                .orElseThrow();
    }
}
