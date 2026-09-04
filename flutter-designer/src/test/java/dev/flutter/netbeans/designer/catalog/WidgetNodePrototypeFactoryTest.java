package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void createsBaselineWithVisibleReviewedDefaultsAndEmptyChildSlot() {
        WidgetDefinition definition = definition("flutter.widgets.Baseline");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Baseline"), prototype.type());
        assertEquals(List.of(
                        new PropertyName("baseline"),
                        new PropertyName("baselineType")),
                prototype.properties().keySet().stream().toList());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.valueOf(24)),
                prototype.properties().get(new PropertyName("baseline")));
        assertEquals(new PropertyValue.EnumValue("TextBaseline", "alphabetic"),
                prototype.properties().get(new PropertyName("baselineType")));
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void baselineCreationAcceptsSignedDoubleAndExactTextBaselineOverrideOnly() {
        WidgetDefinition definition = definition("flutter.widgets.Baseline");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(
                        new PropertyName("baseline"),
                                new PropertyValue.DoubleValue(new BigDecimal("-12.5")),
                        new PropertyName("baselineType"),
                                new PropertyValue.EnumValue(
                                        "TextBaseline", "ideographic")));

        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("-12.5")),
                prototype.properties().get(new PropertyName("baseline")));
        assertEquals(new PropertyValue.EnumValue("TextBaseline", "ideographic"),
                prototype.properties().get(new PropertyName("baselineType")));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("baseline"),
                                new PropertyValue.IntegerValue(java.math.BigInteger.ONE))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("baselineType"),
                                new PropertyValue.EnumValue(
                                        "TextBaseline", "central"))));
    }

    @Test
    void createsIntrinsicHeightAsEmptyInsertableStructuralWrapper() {
        WidgetDefinition definition = definition(
                "flutter.widgets.IntrinsicHeight");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.IntrinsicHeight"),
                prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertEquals(0, definition.slot(new SlotName("child"))
                .orElseThrow().minChildren());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsIntrinsicWidthWithNullableStepsAndEmptyOptionalChild() {
        WidgetDefinition definition = definition(
                "flutter.widgets.IntrinsicWidth");

        WidgetNode empty = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.IntrinsicWidth"),
                empty.type());
        assertTrue(empty.properties().isEmpty(),
                "Nullable Flutter step arguments must remain omitted by default");
        assertEquals(List.of(new SlotName("child")),
                empty.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                empty.slots().get(new SlotName("child"))).child().isEmpty());

        WidgetNode configured = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(
                        new PropertyName("stepWidth"),
                                new PropertyValue.DoubleValue(BigDecimal.ZERO),
                        new PropertyName("stepHeight"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("12.5"))));
        assertEquals(List.of(
                        new PropertyName("stepWidth"),
                        new PropertyName("stepHeight")),
                configured.properties().keySet().stream().toList());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ZERO),
                configured.properties().get(new PropertyName("stepWidth")));
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("12.5")),
                configured.properties().get(new PropertyName("stepHeight")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("stepWidth"),
                                new PropertyValue.DoubleValue(
                                        new BigDecimal("-0.01")))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsOffstageWithOmittedFrameworkDefaultAndDistinctExplicitBooleans() {
        WidgetDefinition definition = definition("flutter.widgets.Offstage");

        WidgetNode omitted = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Offstage"), omitted.type());
        assertTrue(omitted.properties().isEmpty(),
                "omission must preserve Flutter's offstage=true default");
        assertEquals(List.of(new SlotName("child")),
                omitted.slots().keySet().stream().toList());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                omitted.slots().get(new SlotName("child"))).child().isEmpty());

        WidgetNode explicitTrue = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(true)));
        WidgetNode explicitFalse = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(false)));
        assertEquals(new PropertyValue.BooleanValue(true),
                explicitTrue.properties().get(new PropertyName("offstage")));
        assertEquals(new PropertyValue.BooleanValue(false),
                explicitFalse.properties().get(new PropertyName("offstage")));
        assertFalse(explicitTrue.equals(explicitFalse));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("offstage"),
                                new PropertyValue.StringValue("true"))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsSizedOverflowBoxWithTypedRequiredSizeAndOptionalSurface() {
        WidgetDefinition definition = definition(
                "flutter.widgets.SizedOverflowBox");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.SizedOverflowBox"),
                prototype.type());
        assertEquals(Map.of(
                        new PropertyName("size"),
                        new PropertyValue.SizeValue(
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100))),
                prototype.properties());
        assertFalse(prototype.properties().containsKey(
                new PropertyName("alignment")));
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());

        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE, BigDecimal.ONE.negate());
        WidgetNode explicit = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(
                        new PropertyName("size"),
                        new PropertyValue.SizeValue(
                                new BigDecimal("240.5"), BigDecimal.ZERO),
                        new PropertyName("alignment"), directional));
        assertEquals(directional, explicit.properties().get(
                new PropertyName("alignment")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("size"),
                                new PropertyValue.StringValue("100x100"))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("size"),
                                new PropertyValue.SizeValue(
                                        BigDecimal.ONE.negate(), BigDecimal.ONE))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsTransformWithIdentityMatrixAndOmittedFrameworkDefaults() {
        WidgetDefinition definition = definition("flutter.widgets.Transform");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.Transform"), prototype.type());
        assertEquals(Map.of(
                        new PropertyName("transform"), identityMatrix()),
                prototype.properties());
        for (String omitted : List.of(
                "origin", "alignment", "transformHitTests", "filterQuality")) {
            assertFalse(prototype.properties().containsKey(new PropertyName(omitted)),
                    omitted);
        }
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());

        PropertyValue.OffsetValue origin = new PropertyValue.OffsetValue(
                new BigDecimal("-12.5"), new BigDecimal("8.25"));
        PropertyValue.AlignmentGeometryValue alignment =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                        BigDecimal.ONE, BigDecimal.ONE.negate());
        WidgetNode explicit = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(
                        new PropertyName("transform"), identityMatrix(),
                        new PropertyName("origin"), origin,
                        new PropertyName("alignment"), alignment,
                        new PropertyName("transformHitTests"),
                        new PropertyValue.BooleanValue(false),
                        new PropertyName("filterQuality"),
                        new PropertyValue.EnumValue("FilterQuality", "high")));
        assertEquals(origin, explicit.properties().get(new PropertyName("origin")));
        assertEquals(alignment, explicit.properties().get(new PropertyName("alignment")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("transform"),
                                new PropertyValue.StringValue("Matrix4.identity()"))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(
                                new PropertyName("transform"), identityMatrix(),
                                new PropertyName("origin"),
                                new PropertyValue.OffsetValue(
                                        new BigDecimal("1E+10000"), BigDecimal.ZERO))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(
                                new PropertyName("transform"), identityMatrix(),
                                new PropertyName("filterQuality"),
                                new PropertyValue.EnumValue(
                                        "FilterQuality", "ultra"))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsRotatedBoxWithOneQuarterTurnAndSignedPortableOverrides() {
        WidgetDefinition definition = definition("flutter.widgets.RotatedBox");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.RotatedBox"), prototype.type());
        assertEquals(Map.of(
                        new PropertyName("quarterTurns"),
                        new PropertyValue.IntegerValue(BigInteger.ONE)),
                prototype.properties());
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());

        WidgetNode negative = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(new PropertyName("quarterTurns"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(-3))));
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(-3)),
                negative.properties().get(new PropertyName("quarterTurns")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("quarterTurns"),
                                new PropertyValue.IntegerValue(
                                        DartNumericLiterals.MAX_PORTABLE_INTEGER
                                                .add(BigInteger.ONE)))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("quarterTurns"),
                                new PropertyValue.StringValue("1"))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsListBodyWithFlutterOwnedDefaultsAndTypedOverrides() {
        WidgetDefinition definition = definition("flutter.widgets.ListBody");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.ListBody"), prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertTrue(assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children"))).children().isEmpty());

        WidgetNode configured = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(
                        new PropertyName("mainAxis"),
                        new PropertyValue.EnumValue("Axis", "horizontal"),
                        new PropertyName("reverse"),
                        new PropertyValue.BooleanValue(true)));
        assertEquals(new PropertyValue.EnumValue("Axis", "horizontal"),
                configured.properties().get(new PropertyName("mainAxis")));
        assertEquals(new PropertyValue.BooleanValue(true),
                configured.properties().get(new PropertyName("reverse")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("mainAxis"),
                                new PropertyValue.EnumValue("Axis", "diagonal"))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("reverse"),
                                new PropertyValue.StringValue("true"))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsOverflowBarWithFlutterOwnedDefaultsAndAllTypedOverrides() {
        WidgetDefinition definition = definition("flutter.widgets.OverflowBar");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.OverflowBar"), prototype.type());
        assertTrue(prototype.properties().isEmpty());
        assertTrue(assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children"))).children().isEmpty());

        WidgetNode configured = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.of(
                        new PropertyName("spacing"),
                        new PropertyValue.DoubleValue(new BigDecimal("-4.5")),
                        new PropertyName("alignment"),
                        new PropertyValue.EnumValue("MainAxisAlignment", "spaceEvenly"),
                        new PropertyName("overflowSpacing"),
                        new PropertyValue.DoubleValue(new BigDecimal("6.25")),
                        new PropertyName("overflowAlignment"),
                        new PropertyValue.EnumValue("OverflowBarAlignment", "end"),
                        new PropertyName("overflowDirection"),
                        new PropertyValue.EnumValue("VerticalDirection", "up"),
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl")));
        assertEquals(6, configured.properties().size());
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("-4.5")),
                configured.properties().get(new PropertyName("spacing")));
        assertEquals(new PropertyValue.EnumValue("OverflowBarAlignment", "end"),
                configured.properties().get(new PropertyName("overflowAlignment")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("overflowAlignment"),
                                new PropertyValue.EnumValue(
                                        "OverflowBarAlignment", "stretch"))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("spacing"),
                                new PropertyValue.IntegerValue(BigInteger.ONE))));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void createsGridViewCountWithRequiredDefaultAndAllTypedOverrides() {
        WidgetDefinition definition = definition("flutter.widgets.GridView");

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(new WidgetTypeId("flutter.widgets.GridView"), prototype.type());
        assertEquals(Map.of(
                        new PropertyName("crossAxisCount"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                prototype.properties());
        assertTrue(assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children"))).children().isEmpty());

        WidgetNode configured = WidgetNodePrototypeFactory.create(
                definition,
                ID,
                Map.ofEntries(
                        Map.entry(new PropertyName("scrollDirection"),
                                new PropertyValue.EnumValue("Axis", "horizontal")),
                        Map.entry(new PropertyName("reverse"),
                                new PropertyValue.BooleanValue(true)),
                        Map.entry(new PropertyName("primary"),
                                new PropertyValue.BooleanValue(false)),
                        Map.entry(new PropertyName("physics"),
                                new PropertyValue.StringValue("bouncing")),
                        Map.entry(new PropertyName("shrinkWrap"),
                                new PropertyValue.BooleanValue(true)),
                        Map.entry(new PropertyName("padding"),
                                new PropertyValue.EdgeInsetsValue(
                                        BigDecimal.ONE, BigDecimal.ONE,
                                        BigDecimal.ONE, BigDecimal.ONE)),
                        Map.entry(new PropertyName("crossAxisCount"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(3))),
                        Map.entry(new PropertyName("mainAxisSpacing"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(8))),
                        Map.entry(new PropertyName("crossAxisSpacing"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(6))),
                        Map.entry(new PropertyName("childAspectRatio"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(1.5))),
                        Map.entry(new PropertyName("mainAxisExtent"),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(72))),
                        Map.entry(new PropertyName("addAutomaticKeepAlives"),
                                new PropertyValue.BooleanValue(false)),
                        Map.entry(new PropertyName("addRepaintBoundaries"),
                                new PropertyValue.BooleanValue(false)),
                        Map.entry(new PropertyName("addSemanticIndexes"),
                                new PropertyValue.BooleanValue(false)),
                        Map.entry(new PropertyName("scrollCacheExtent"),
                                new PropertyValue.IntegerValue(BigInteger.valueOf(200))),
                        Map.entry(new PropertyName("semanticChildCount"),
                                new PropertyValue.IntegerValue(BigInteger.ZERO)),
                        Map.entry(new PropertyName("dragStartBehavior"),
                                new PropertyValue.EnumValue(
                                        "DragStartBehavior", "down")),
                        Map.entry(new PropertyName("keyboardDismissBehavior"),
                                new PropertyValue.EnumValue(
                                        "ScrollViewKeyboardDismissBehavior", "onDrag")),
                        Map.entry(new PropertyName("restorationId"),
                                new PropertyValue.StringValue("main-grid")),
                        Map.entry(new PropertyName("clipBehavior"),
                                new PropertyValue.EnumValue("Clip", "antiAlias")),
                        Map.entry(new PropertyName("hitTestBehavior"),
                                new PropertyValue.EnumValue(
                                        "HitTestBehavior", "translucent"))));
        assertEquals(21, configured.properties().size());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(3)),
                configured.properties().get(new PropertyName("crossAxisCount")));
        assertEquals(new PropertyValue.StringValue("bouncing"),
                configured.properties().get(new PropertyName("physics")));

        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("crossAxisCount"),
                                new PropertyValue.IntegerValue(BigInteger.ZERO))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("mainAxisSpacing"),
                                new PropertyValue.IntegerValue(BigInteger.ONE))));
        assertThrows(IllegalArgumentException.class,
                () -> WidgetNodePrototypeFactory.create(
                        definition,
                        ID,
                        Map.of(new PropertyName("physics"),
                                new PropertyValue.StringValue("custom"))));
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

    private static PropertyValue.Matrix4Value identityMatrix() {
        return new PropertyValue.Matrix4Value(List.of(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
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
