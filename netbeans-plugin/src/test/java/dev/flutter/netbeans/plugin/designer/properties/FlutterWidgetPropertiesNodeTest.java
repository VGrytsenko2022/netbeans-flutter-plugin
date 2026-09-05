package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.AppBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ColoredBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipOvalWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipPathWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRSuperellipseWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PhysicalModelWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ClipRectWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DecoratedBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DirectionalityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.ElevatedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ContainerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IconWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.MaterialIconRegistry;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ListViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PlaceholderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SafeAreaWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextFieldWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.PatchProperties;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.icons.FlutterWidgetIconRegistry;
import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterWidgetPropertiesNodeTest {

    @Test
    void singleChildScrollViewProjectsExactTenPropertiesAndOptionalChildSlot()
            throws Exception {
        WidgetDefinition definition = definition(
                SingleChildScrollViewWidgetPropertySchema
                        .SINGLE_CHILD_SCROLL_VIEW_TYPE.value());
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("85858585-8585-4585-8585-858585858585"));
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + SingleChildScrollViewWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(SingleChildScrollViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                Arrays.stream(sets)
                        .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                                set.getName()))
                        .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(
                                set.getName()))
                        .mapToInt(set -> set.getProperties().length).sum());
        for (SingleChildScrollViewWidgetPropertySchema.Group group
                : SingleChildScrollViewWidgetPropertySchema.Group.values()) {
            Node.PropertySet set = propertySet(node, group.setName());
            assertEquals(group.displayName(), set.getDisplayName());
            assertEquals(group.description(), set.getShortDescription());
            assertEquals("General", set.getValue(
                    FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        }

        Node.Property<?> physics = property(node, "physics");
        assertEquals(java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(FlutterWidgetPropertiesNode.NOT_SET),
                        SingleChildScrollViewWidgetPropertySchema.PHYSICS_PRESETS.stream())
                        .toList(),
                Arrays.asList(physics.getPropertyEditor().getTags()));
        cellProperty(physics).setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("bouncing")));
        SetProperty command = assertInstanceOf(SetProperty.class, commands.getFirst());
        assertEquals(new PropertyName("physics"), command.propertyName());
        assertEquals(new PropertyValue.StringValue("bouncing"), command.value());

        assertTrue(widget.properties().isEmpty(),
                "palette creation must preserve all ten Flutter defaults by omission");
        Node.PropertySet slots = propertySet(
                node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(List.of("child"), names(slots.getProperties()));
        assertTrue(slots.getProperties()[0].getShortDescription().contains(
                "Optional child inside the scrolling viewport"));
        assertTrue(slots.getProperties()[0].getShortDescription().contains(
                "Occupancy: 0/1"));
        assertEquals("Slots", slots.getValue(
                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
    }

    @Test
    void gridViewCountProjectsAllPropertiesWithClosedPhysicsChoicesAndChildrenSlot()
            throws Exception {
        WidgetDefinition definition = definition(
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value());
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("1ad9391f-ab8d-4fe0-9a79-2ac0d746e287"));
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + GridViewCountWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(GridViewCountWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                Arrays.stream(sets)
                        .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                                set.getName()))
                        .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(
                                set.getName()))
                        .mapToInt(set -> set.getProperties().length).sum());
        for (GridViewCountWidgetPropertySchema.Group group
                : GridViewCountWidgetPropertySchema.Group.values()) {
            Node.PropertySet set = propertySet(node, group.setName());
            assertEquals(group.displayName(), set.getDisplayName());
            assertEquals(group.description(), set.getShortDescription());
            assertEquals("General", set.getValue(
                    FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        }

        Node.Property<?> physics = property(node, "physics");
        assertEquals(java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(FlutterWidgetPropertiesNode.NOT_SET),
                        GridViewCountWidgetPropertySchema.PHYSICS_PRESETS.stream()).toList(),
                Arrays.asList(physics.getPropertyEditor().getTags()));
        cellProperty(physics).setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("clamping")));
        SetProperty command = assertInstanceOf(SetProperty.class, commands.getFirst());
        assertEquals(new PropertyName("physics"), command.propertyName());
        assertEquals(new PropertyValue.StringValue("clamping"), command.value());

        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                widget.properties().get(new PropertyName("crossAxisCount")));
        assertTrue(property(node, "crossAxisCount").getShortDescription().contains(
                "Positive number of tiles in the cross axis"));
        assertTrue(property(node, "mainAxisExtent").getShortDescription().contains(
                "logical-pixel extent for each tile"));

        Node.PropertySet slots = propertySet(
                node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(List.of("children"), names(slots.getProperties()));
        assertTrue(slots.getProperties()[0].getShortDescription().contains(
                "exact source, paint, and semantic order"));
        assertTrue(slots.getProperties()[0].getShortDescription().contains(
                "columns for vertical scrolling or rows for horizontal scrolling"));
        assertEquals("Slots", slots.getValue(
                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
    }

    @Test
    void listViewProjectsAllStaticPropertiesWithClosedPhysicsChoicesAndChildrenSlot()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.ListView");
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("49b6f6b9-c7c8-4eab-9807-bdb99ae62aac"));
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + ListViewWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(17, Arrays.stream(sets)
                .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                        set.getName()))
                .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(
                        set.getName()))
                .mapToInt(set -> set.getProperties().length).sum());
        for (ListViewWidgetPropertySchema.Group group
                : ListViewWidgetPropertySchema.Group.values()) {
            Node.PropertySet set = propertySet(node, group.setName());
            assertEquals(group.displayName(), set.getDisplayName());
            assertEquals("General", set.getValue(
                    FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        }

        Node.Property<?> physics = property(node, "physics");
        assertTrue(physics.canWrite());
        assertEquals(java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(FlutterWidgetPropertiesNode.NOT_SET),
                        ListViewWidgetPropertySchema.PHYSICS_PRESETS.stream()).toList(),
                Arrays.asList(physics.getPropertyEditor().getTags()));
        cellProperty(physics).setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("bouncing")));
        SetProperty command = assertInstanceOf(SetProperty.class, commands.getFirst());
        assertEquals(new PropertyName("physics"), command.propertyName());
        assertEquals(new PropertyValue.StringValue("bouncing"), command.value());

        Node.PropertySet slots = propertySet(
                node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(List.of("children"), names(slots.getProperties()));
        assertTrue(slots.getProperties()[0].getShortDescription().contains(
                "laid out linearly along the selected scroll axis"));
        assertEquals("Slots", slots.getValue(
                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
    }

    @Test
    void containerProjectsAllThirteenPropertiesIntoGeneralGroupsAndOneChildSlot()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Container");
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("344e2731-0ddd-46d5-86b4-e190e1bbf0e2"));
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + ContainerWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME, sets[0].getName());
        for (int index = 0;
                index < ContainerWidgetPropertySchema.Group.values().length;
                index++) {
            ContainerWidgetPropertySchema.Group group =
                    ContainerWidgetPropertySchema.Group.values()[index];
            assertEquals(group.setName(), sets[index + 1].getName());
            assertEquals(group.displayName(), sets[index + 1].getDisplayName());
            assertEquals("General", sets[index + 1].getValue(
                    FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        }
        assertEquals(13, Arrays.stream(sets)
                .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(set.getName()))
                .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(set.getName()))
                .mapToInt(set -> set.getProperties().length).sum());
        assertTrue(property(node, "alignment").canWrite());
        assertTrue(property(node, "constraints").canWrite());
        assertTrue(property(node, "transform").canWrite());
        assertTrue(property(node, "decoration").canWrite());
        Node.PropertySet slots = propertySet(node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(List.of("child"), names(slots.getProperties()));
        assertTrue(slots.getProperties()[0].getShortDescription().contains(
                "inside Container padding, alignment, and constraints"));
        assertEquals("Slots", slots.getValue(
                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
    }

    @Test
    void containerPropertiesSubmitOneAtomicPatchAcrossFlutterInvariants()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Container");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("855e6d82-eb43-48d8-b5df-c880d889632c"));
        WidgetNode colored = new WidgetNode(
                prototype.id(), prototype.type(),
                Map.of(new PropertyName("color"),
                        PropertyValue.ColorValue.fromWireArgb("0xFF123456")),
                prototype.slots(), prototype.extensions());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, colored, definition, commands::add);

        PropertyValue.BoxDecorationValue decoration = emptyDecoration();
        cellProperty(property(node, "decoration")).setValue(
                FlutterPropertyCellValue.explicit(decoration));

        PatchProperties patch = assertInstanceOf(
                PatchProperties.class, commands.getFirst());
        assertEquals(colored.id(), patch.widgetId());
        assertEquals(List.of(
                new PatchProperties.ResetPatch(new PropertyName("color")),
                new PatchProperties.SetPatch(new PropertyName("decoration"), decoration)),
                patch.patches());

        commands.clear();
        FlutterWidgetPropertiesNode clipNode = new FlutterWidgetPropertiesNode(
                Children.LEAF, colored, definition, commands::add);
        cellProperty(property(clipNode, "clipBehavior")).setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.EnumValue("Clip", "antiAlias")));
        PatchProperties clipPatch = assertInstanceOf(
                PatchProperties.class, commands.getFirst());
        assertEquals(List.of("color", "decoration", "clipBehavior"),
                clipPatch.patches().stream()
                        .map(item -> item.propertyName().value()).toList());
        PropertyValue.BoxDecorationValue migrated = assertInstanceOf(
                PropertyValue.BoxDecorationValue.class,
                ((PatchProperties.SetPatch) clipPatch.patches().get(1)).value());
        assertEquals(Optional.of(new dev.flutter.netbeans.designer.model.ColorSource.Literal(
                        0xFF123456L)), migrated.color());
    }

    @Test
    void readOnlyPresentationRefreshKeepsPropertyIdentityAndUpdatesFormattedValue()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse(
                "df8819d0-7e10-44d3-98cc-96175a87d933");
        PropertyName softWrap = new PropertyName("softWrap");
        WidgetNode before = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(softWrap, new PropertyValue.BooleanValue(false)),
                Map.of(),
                Extensions.empty());
        WidgetNode after = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(softWrap, new PropertyValue.BooleanValue(true)),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, before, definition);
        Node.Property<?> stableProperty = property(node, "softWrap");

        assertFalse(stableProperty.canWrite());
        assertEquals(String.class, stableProperty.getValueType());
        assertEquals("false", stableProperty.getValue());

        node.refreshPresentation(
                after,
                definition,
                null,
                null,
                null,
                FlutterImageAssetChoices.empty());

        assertSame(stableProperty, property(node, "softWrap"));
        assertFalse(stableProperty.canWrite());
        assertEquals("true", stableProperty.getValue(),
                "a null-handler Property must read the current immutable presentation");
        assertSame(after, node.getLookup().lookup(WidgetNode.class));
    }

    @Test
    void refreshPublishesAtomicWidgetAndHandlerPairToConcurrentPropertyEdits()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Container");
        StableId id = StableId.parse(
                "ef7e61dc-ec95-49c9-b5d2-c934592c5b06");
        PropertyName color = new PropertyName("color");
        WidgetNode colored = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(color, PropertyValue.ColorValue.fromWireArgb(
                        "0xFF123456")),
                Map.of(),
                Extensions.empty());
        WidgetNode empty = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode.PropertyMutationHandler coloredHandler =
                command -> assertContainerClipCommand(command, true);
        FlutterWidgetPropertiesNode.PropertyMutationHandler emptyHandler =
                command -> assertContainerClipCommand(command, false);
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, colored, definition, coloredHandler);
        Node.Property<FlutterPropertyCellValue> clip = cellProperty(
                property(node, "clipBehavior"));
        FlutterPropertyCellValue antiAlias = FlutterPropertyCellValue.explicit(
                new PropertyValue.EnumValue("Clip", "antiAlias"));
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        int iterations = 10_000;

        Thread refresher = Thread.ofVirtual().start(() -> {
            try {
                start.await();
                for (int index = 0; index < iterations; index++) {
                    boolean useColored = (index & 1) == 0;
                    node.refreshPresentation(
                            useColored ? colored : empty,
                            definition,
                            useColored ? coloredHandler : emptyHandler,
                            null,
                            null,
                            FlutterImageAssetChoices.empty());
                    if ((index & 31) == 0) {
                        Thread.yield();
                    }
                }
            } catch (Throwable thrown) {
                failure.compareAndSet(null, thrown);
            }
        });
        Thread editor = Thread.ofVirtual().start(() -> {
            try {
                start.await();
                for (int index = 0; index < iterations; index++) {
                    clip.setValue(antiAlias);
                    if ((index & 31) == 0) {
                        Thread.yield();
                    }
                }
            } catch (Throwable thrown) {
                failure.compareAndSet(null, thrown);
            }
        });

        start.countDown();
        refresher.join(TimeUnit.SECONDS.toMillis(10));
        editor.join(TimeUnit.SECONDS.toMillis(10));

        assertFalse(refresher.isAlive(), "presentation refresh thread timed out");
        assertFalse(editor.isAlive(), "property edit thread timed out");
        assertNull(failure.get(),
                () -> "widget/handler pairing was torn: " + failure.get());
        assertSame(clip, property(node, "clipBehavior"));
    }

    @Test
    void declaredImageChoicesReachTheStructuredDecorationEditorDescriptor()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Container");
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("6bfe3d47-1646-464b-b3c9-b034540ea4e1"));
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(),
                        "assets/background.png",
                        "App: assets/background.png")),
                Optional.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                widget,
                definition,
                ignored -> { },
                null,
                null,
                choices);

        assertSame(choices, property(node, "decoration").getValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE));
    }

    @Test
    void imageProjectsExactTwentyTwoRowsWithRequiredProviderChoicesAndReviewedHelp()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Image");
        PropertyValue.ImageProviderValue provider =
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                        "assets/hero.png",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        WidgetNode widget = new WidgetNode(
                StableId.parse("668d4d7d-6b1f-4098-a41b-b5acd472ed3a"),
                definition.typeId(),
                Map.of(new PropertyName("image"), provider),
                Map.of(),
                Extensions.empty());
        FlutterImageAssetChoices choices = new FlutterImageAssetChoices(
                List.of(new FlutterImageAssetChoices.Choice(
                        Optional.empty(), "assets/hero.png", "App: assets/hero.png")),
                Optional.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                widget,
                definition,
                ignored -> { },
                null,
                null,
                choices);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                "image", "frameBuilder", "loadingBuilder", "errorBuilder",
                "semanticLabel", "excludeFromSemantics", "width", "height",
                "color", "opacity", "colorBlendMode", "fit", "alignment",
                "repeat", "centerSliceLeft", "centerSliceTop",
                "centerSliceRight", "centerSliceBottom", "matchTextDirection",
                "gaplessPlayback", "isAntiAlias", "filterQuality"),
                names(properties.getProperties()));
        assertEquals(22, properties.getProperties().length);
        assertTrue(properties.getShortDescription().contains("nine-patch"));

        Node.Property<?> image = property(node, "image");
        assertSame(choices, image.getValue(
                FlutterImageAssetChoices.FEATURE_ATTRIBUTE));
        assertTrue(image.canWrite());
        assertTrue(image.getShortDescription().startsWith(
                "Required asset-only ImageProvider"));
        assertTrue(image.getShortDescription().contains("declared by the app"));
        assertTrue(image.getShortDescription().contains("arbitrary paths"));

        assertAll(Arrays.stream(properties.getProperties())
                .map(projected -> () -> {
                    assertFalse(projected.getShortDescription().isBlank(),
                            projected.getName());
                    assertFalse(projected.getShortDescription().startsWith(
                            "Explicit model value for"), projected.getName());
                }));
        assertTrue(property(node, "opacity").getShortDescription()
                .contains("0 through 1"));
        assertTrue(property(node, "fit").getShortDescription()
                .contains("cover and none are incompatible"));
        assertTrue(property(node, "centerSliceLeft").getShortDescription()
                .contains("All four center-slice edges must be set together"));
        assertTrue(property(node, "centerSliceRight").getShortDescription()
                .contains("fit decoded image bounds"));
    }

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
    void projectsEveryScaffoldNamedSlotInCatalogOrderIncludingEmptySlots()
            throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetDefinition centerDefinition = definition("flutter.widgets.Center");
        WidgetNode center = new WidgetNode(
                StableId.parse("f2fbd4df-0f53-4892-9737-a3e4c62c0614"),
                centerDefinition.typeId(),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        WidgetNode scaffold = new WidgetNode(
                StableId.parse("e08b35a8-887b-47b4-8817-9aa958c63f9b"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(center)),
                Extensions.empty());
        DesignerDocument document = document(scaffold);
        FlutterWidgetSlotEditorContext slotContext =
                new FlutterWidgetSlotEditorContext(
                        document,
                        BuiltInWidgetCatalog.getDefault(),
                        List.of(
                                centerDefinition.typeId(),
                                definition("flutter.widgets.Text").typeId()));
        List<FlutterWidgetSlotMutation> mutations = new ArrayList<>();

        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                scaffold,
                scaffoldDefinition,
                ignored -> { },
                slotContext,
                mutations::add);
        Node.PropertySet slots = propertySet(
                node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);

        Node.PropertySet[] sets = node.getPropertySets();
        List<Object> tabs = Arrays.stream(sets)
                .map(set -> set.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE))
                .toList();
        assertEquals(Set.of("General", "Slots"), Set.copyOf(tabs),
                "the native PropertySheet must render exactly General and Slots tabs");
        assertEquals("Slots", tabs.getLast());
        assertTrue(tabs.subList(0, tabs.size() - 1).stream()
                .allMatch("General"::equals));
        assertEquals(List.of("appBar", "body", "floatingActionButton"),
                names(slots.getProperties()));
        assertEquals("Empty", slots.getProperties()[0].getValue().toString());
        assertEquals("Center", slots.getProperties()[1].getValue().toString());
        assertEquals("Empty", slots.getProperties()[2].getValue().toString());
        for (Node.Property<?> property : slots.getProperties()) {
            assertTrue(property.canWrite(), property.getName());
            assertEquals(FlutterWidgetSlotCellValue.class, property.getValueType());
            assertEquals(Boolean.FALSE, property.getValue("changeImmediate"));
            assertEquals(Boolean.FALSE, property.getValue("canEditAsText"));
        }
        assertEquals(List.of(), mutations,
                "opening/projecting slot rows must not mutate the model");
    }

    @Test
    void slotRowsRemainVisibleButReadOnlyWithoutMutationAuthority() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Column");
        WidgetNode first = textWidget(
                StableId.parse("5328fdfd-283a-4a62-8ed8-241bb00c5c7c"), "one");
        WidgetNode second = textWidget(
                StableId.parse("d723f7f7-0ea1-4822-82e3-a081aed2a032"), "two");
        WidgetNode column = new WidgetNode(
                StableId.parse("04d75683-e72c-438b-a7c1-6744748add48"),
                definition.typeId(),
                Map.of(),
                Map.of(new SlotName("children"),
                        new WidgetSlot.ListSlot(List.of(first, second))),
                Extensions.empty());

        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, column, definition);
        Node.PropertySet slots = propertySet(
                node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);

        assertEquals(List.of("General", "General", "Slots"),
                Arrays.stream(node.getPropertySets())
                        .map(set -> set.getValue(
                                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE))
                        .toList(),
                "read-only slot widgets must retain the same native tabs");
        assertEquals(1, slots.getProperties().length);
        assertEquals("2 widgets", slots.getProperties()[0].getValue());
        assertFalse(slots.getProperties()[0].canWrite());
        assertEquals(String.class, slots.getProperties()[0].getValueType());
    }

    @Test
    void leafWidgetDoesNotExposeAnEmptySlotsCategory() {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                textWidget(StableId.parse(
                        "72bf176f-e208-447f-bd9b-e73e43b58c0e"), "leaf"),
                definition);

        assertTrue(Arrays.stream(node.getPropertySets())
                .noneMatch(set -> FlutterWidgetPropertiesNode.SLOTS_SET_NAME
                        .equals(set.getName())));
        assertTrue(Arrays.stream(node.getPropertySets())
                .allMatch(set -> set.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE) == null),
                "leaf widgets must retain the ordinary untabbed PropertySheet");
    }

    @Test
    void exposesIdentityThenSevenSchemaDrivenTextGroupsAsReadOnlyStrings() throws Exception {
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

        assertEquals(8, sets.length);
        assertEquals(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME, sets[0].getName());
        assertEquals(List.of(
                FlutterWidgetPropertiesNode.IDENTITY_SET_NAME,
                TextWidgetPropertySchema.Group.CONTENT.setName(),
                TextWidgetPropertySchema.Group.ACCESSIBILITY.setName(),
                TextWidgetPropertySchema.Group.LOCALE_AND_SCALING.setName(),
                TextWidgetPropertySchema.Group.STYLE.setName(),
                TextWidgetPropertySchema.Group.STYLE_PAINT.setName(),
                TextWidgetPropertySchema.Group.STYLE_TYPOGRAPHY.setName(),
                TextWidgetPropertySchema.Group.STRUT.setName()),
                Arrays.stream(sets).map(Node.PropertySet::getName).toList());
        for (int index = 0; index < TextWidgetPropertySchema.Group.values().length; index++) {
            TextWidgetPropertySchema.Group group =
                    TextWidgetPropertySchema.Group.values()[index];
            assertEquals(group.displayName(), sets[index + 1].getDisplayName());
            assertEquals(group.description(), sets[index + 1].getShortDescription());
        }
        assertEquals(
                java.util.List.of("stableId", "type"),
                names(sets[0].getProperties()));
        assertEquals(
                java.util.List.of(
                        "data", "textAlign", "textDirection", "softWrap",
                        "overflow", "maxLines", "textWidthBasis", "selectionColor"),
                names(sets[1].getProperties()));
        assertEquals(List.of("semanticsLabel", "semanticsIdentifier"),
                names(sets[2].getProperties()));
        assertEquals(List.of(
                "localeLanguageCode", "localeScriptCode", "localeCountryCode",
                "textScalerFactor", "textHeightApplyFirstAscent",
                "textHeightApplyLastDescent", "textHeightLeadingDistribution"),
                names(sets[3].getProperties()));
        assertEquals(25, sets[4].getProperties().length);
        assertEquals(List.of(
                "styleInherit", "styleColor", "styleBackgroundColor",
                "styleFontSize", "styleFontWeight", "styleFontStyle"),
                names(Arrays.copyOfRange(sets[4].getProperties(), 0, 6)));
        assertEquals(List.of(
                "styleDecorationColor", "styleForeground", "styleBackground",
                "styleShadows"), names(sets[5].getProperties()));
        assertEquals(List.of("styleFontFeatures", "styleFontVariations"),
                names(sets[6].getProperties()));
        assertEquals(11, sets[7].getProperties().length);
        assertEquals(id.toString(), sets[0].getProperties()[0].getValue());
        assertEquals("flutter.widgets.Text", sets[0].getProperties()[1].getValue());
        assertEquals("\"Hello\\nFlutter\"", sets[1].getProperties()[0].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[1].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[2].getValue());
        assertEquals("false", sets[1].getProperties()[3].getValue());
        assertEquals("Font size", property(node, "styleFontSize").getDisplayName());
        assertEquals("Font size in logical pixels.",
                property(node, "styleFontSize").getShortDescription());
        for (Node.PropertySet set : sets) {
            for (Node.Property<?> property : set.getProperties()) {
                assertFalse(property.canWrite(), property.getName());
            }
        }
    }

    @Test
    void exposesEverySafeCatalogPropertyOnAllCapabilityReviewedWidgetsAsWritable()
            throws Exception {
        Map<String, Map<PropertyName, PropertyValue>> requiredValues =
                new LinkedHashMap<>(Map.of(
                "flutter.widgets.Padding", Map.of(
                        new PropertyName("padding"),
                        new PropertyValue.EdgeInsetsValue(
                                BigDecimal.valueOf(16), BigDecimal.valueOf(16),
                                BigDecimal.valueOf(16), BigDecimal.valueOf(16))),
                "flutter.widgets.Text", Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue("Hello Flutter")),
                "flutter.widgets.Icon", Map.of(
                        new PropertyName("icon"),
                        new PropertyValue.IconDataValue(
                                java.util.Optional.of(0xE5F9),
                                java.util.Optional.of("MaterialIcons"),
                                java.util.Optional.empty(), false, List.of())),
                "flutter.widgets.AspectRatio", Map.of(
                        new PropertyName("aspectRatio"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                "flutter.widgets.Baseline", Map.of(
                        new PropertyName("baseline"),
                        new PropertyValue.DoubleValue(new BigDecimal("24")),
                        new PropertyName("baselineType"),
                        new PropertyValue.EnumValue("TextBaseline", "alphabetic")),
                "flutter.widgets.Opacity", Map.of(
                        new PropertyName("opacity"),
                        new PropertyValue.DoubleValue(BigDecimal.ONE)),
                "flutter.widgets.ConstrainedBox", Map.of(
                        new PropertyName("constraints"),
                        new PropertyValue.BoxConstraintsValue(
                                BigDecimal.ZERO, Optional.empty(),
                                BigDecimal.ZERO, Optional.empty())),
                "flutter.widgets.SizedOverflowBox", Map.of(
                        new PropertyName("size"),
                        new PropertyValue.SizeValue(
                                BigDecimal.valueOf(100), BigDecimal.valueOf(100))),
                "flutter.widgets.Transform", Map.of(
                        new PropertyName("transform"), identityMatrix()),
                "flutter.widgets.Image", Map.of(
                        new PropertyName("image"),
                        new PropertyValue.ImageProviderValue(
                                PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                                "assets/image.png",
                                Optional.empty(), Optional.empty(), Optional.empty()))));
        requiredValues.put(
                "flutter.widgets.RotatedBox",
                Map.of(
                        new PropertyName("quarterTurns"),
                        new PropertyValue.IntegerValue(BigInteger.ZERO)));
        requiredValues.put(
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
                Map.of(
                        new PropertyName("crossAxisCount"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))));
        requiredValues.put(
                "flutter.widgets.ColoredBox",
                Map.of(
                        new PropertyName("color"),
                        new PropertyValue.ColorValue(0xFF2196F3L)));
        requiredValues.put("flutter.widgets.PhysicalShape",
                Map.of(new PropertyName("color"), new PropertyValue.ColorValue(0xFF2196F3L),
                        new PropertyName("clipper"), PropertyValue.ShapeBorderClipperValue.defaultValue()));
        requiredValues.put("flutter.widgets.PhysicalModel",
                Map.of(new PropertyName("color"), new PropertyValue.ColorValue(0xFF2196F3L)));
        requiredValues.put(
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.value(),
                Map.of(
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "ltr")));
        requiredValues.put(
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.value(),
                Map.of(new PropertyName("decoration"), emptyDecoration()));
        List<String> types = List.of(
                "flutter.material.Scaffold",
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.TextField",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Wrap",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Container",
                "flutter.widgets.Opacity",
                "flutter.widgets.Align",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.FittedBox",
                "flutter.widgets.ConstrainedBox",
                "flutter.widgets.UnconstrainedBox",
                "flutter.widgets.LimitedBox",
                "flutter.widgets.OverflowBox",
                "flutter.widgets.Stack",
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer",
                "flutter.widgets.Baseline",
                "flutter.widgets.IntrinsicHeight",
                "flutter.widgets.IntrinsicWidth",
                "flutter.widgets.Offstage",
                "flutter.widgets.SizedOverflowBox",
                "flutter.widgets.Transform",
                "flutter.widgets.RotatedBox",
                "flutter.widgets.ListBody",
                "flutter.widgets.OverflowBar",
                SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE.value(),
                "flutter.widgets.ListView",
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
                SingleChildScrollViewWidgetPropertySchema
                        .SINGLE_CHILD_SCROLL_VIEW_TYPE.value(),
                "flutter.widgets.Text",
                "flutter.widgets.Icon",
                "flutter.widgets.Image",
                "flutter.widgets.ColoredBox",
                PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE.value(),
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.value(),
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.value(),
                ClipRectWidgetPropertySchema.CLIP_RECT_TYPE.value(),
                ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE.value(),
                ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE.value(),
                ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.value(),
                ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE.value(),
                PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE.value(),
                "flutter.widgets.PhysicalShape",
                    "flutter.widgets.RepaintBoundary",
                ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE.value(),
                IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.value());

        int writableCount = 0;
        int nonScaffoldWritableCount = 0;
        for (String type : types) {
            WidgetDefinition definition = definition(type);
            WidgetNode widget = new WidgetNode(
                    StableId.parse("b2583de5-3849-4401-8008-00210e1bdb7d"),
                    definition.typeId(),
                    requiredValues.getOrDefault(type, Map.of()),
                    Map.of(),
                    Extensions.empty());
            FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                    Children.LEAF, widget, definition, ignored -> { });

            Node.Property<?>[] properties = Arrays.stream(node.getPropertySets())
                    .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME
                            .equals(set.getName()))
                    .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME
                            .equals(set.getName()))
                    .flatMap(set -> Arrays.stream(set.getProperties()))
                    .toArray(Node.Property<?>[]::new);
            assertEquals(definition.properties().size(), properties.length, type);
            for (Node.Property<?> property : properties) {
                assertTrue(property.canWrite(), type + "." + property.getName());
                assertEquals(FlutterPropertyCellValue.class,
                        property.getValueType(), property.getName());
                writableCount++;
                if (!"flutter.material.Scaffold".equals(type)) {
                    nonScaffoldWritableCount++;
                }
            }
        }

        assertEquals(759, writableCount,
                "the reviewed surface includes complete Scaffold, AppBar, "
                + "ElevatedButton, TextField, Text, Icon, AspectRatio, Container, "
                + "Opacity, Align, "
                + "FractionallySizedBox, FittedBox, ConstrainedBox, UnconstrainedBox, "
                + "LimitedBox, OverflowBox, "
                + "Wrap, Stack, "
                + "Expanded, Flexible, Spacer, Baseline, IntrinsicHeight, IntrinsicWidth, "
                + "Offstage, SizedOverflowBox, Transform, RotatedBox, ListBody, "
                + "OverflowBar, SafeArea, ListView, GridView.count, SingleChildScrollView, "
                + "Image, ColoredBox, Placeholder, Directionality, DecoratedBox, and "
                + "ExcludeSemantics, IndexedStack, ClipRect, ClipOval, ClipRRect, and "
                + "ClipPath and ClipRSuperellipse leaves");
        assertEquals(742, nonScaffoldWritableCount,
                "all non-Scaffold built-ins expose their complete writable surface");
    }

    @Test
    void writableStringDispatchesExactSetCommandAndSkipsTheCapturedValue()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse("e09cd249-07b9-426e-a78c-c68452ad09a3");
        PropertyName data = new PropertyName("data");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(data, new PropertyValue.StringValue("before")),
                Map.of(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);
        Node.Property<FlutterPropertyCellValue> property = cellProperty(
                property(node, "data"));

        property.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("before")));
        assertEquals(List.of(), commands);

        property.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("after")));
        property.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("")));

        assertEquals(List.of(
                new SetProperty(id, data, new PropertyValue.StringValue("after")),
                new SetProperty(id, data, new PropertyValue.StringValue(""))),
                commands);
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue("before")),
                property.getValue(),
                "The immutable node must keep showing the last confirmed model value");
    }

    @Test
    void expandedTextStyleFieldsKeepCatalogEditorsAndDispatchRepeatedCommands()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse("84d15627-41ae-44c5-8ae0-22b29fecc0fe");
        WidgetNode widget = textWidget(id, "Text");
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        PropertyEditor fontSize = property(node, "styleFontSize").getPropertyEditor();
        PropertyEditor fontWeight = property(node, "styleFontWeight").getPropertyEditor();
        PropertyEditor fontStyle = property(node, "styleFontStyle").getPropertyEditor();
        PropertyEditor color = property(node, "styleColor").getPropertyEditor();
        assertThrows(IllegalArgumentException.class,
                () -> fontSize.setAsText("-1"));
        fontSize.setAsText("16.5");
        fontWeight.setAsText("w600");
        fontStyle.setAsText("italic");
        color.setAsText("0xFF336699");

        Node.Property<FlutterPropertyCellValue> fontSizeProperty = cellProperty(
                property(node, "styleFontSize"));
        fontSizeProperty.setValue(cell(fontSize));
        fontSizeProperty.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.DoubleValue(new BigDecimal("18"))));
        cellProperty(property(node, "styleFontWeight")).setValue(cell(fontWeight));
        cellProperty(property(node, "styleFontStyle")).setValue(cell(fontStyle));
        cellProperty(property(node, "styleColor")).setValue(cell(color));

        assertEquals(List.of(
                new SetProperty(id, new PropertyName("styleFontSize"),
                        new PropertyValue.DoubleValue(new BigDecimal("16.5"))),
                new SetProperty(id, new PropertyName("styleFontSize"),
                        new PropertyValue.DoubleValue(new BigDecimal("18"))),
                new SetProperty(id, new PropertyName("styleFontWeight"),
                        new PropertyValue.EnumValue("FontWeight", "w600")),
                new SetProperty(id, new PropertyName("styleFontStyle"),
                        new PropertyValue.EnumValue("FontStyle", "italic")),
                new SetProperty(id, new PropertyName("styleColor"),
                        PropertyValue.ColorValue.fromWireArgb("0xFF336699"))),
                commands);
    }

    @Test
    void fontFallbackEditorShowsSummaryAndStoresNewlineDelimitedString()
            throws Exception {
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                textWidget(StableId.parse(
                        "01652231-9468-4b23-9a52-14d4fa012215"), "Text"),
                definition("flutter.widgets.Text"),
                ignored -> { });
        PropertyEditor editor = property(node, "styleFontFamilyFallback")
                .getPropertyEditor();

        editor.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue("Noto Sans\nNoto Color Emoji")));
        assertEquals("Noto Sans, Noto Color Emoji", editor.getAsText());
        editor.setAsText("Inter, Roboto, Noto Sans");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue("Inter\nRoboto\nNoto Sans")),
                editor.getValue());
    }

    @Test
    void optionalPropertySupportsNativeRestoreDefaultAsExactResetCommand()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse("83fdcc69-3bdc-48bb-aa7b-37cd78ec5428");
        PropertyName softWrap = new PropertyName("softWrap");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        new PropertyName("data"), new PropertyValue.StringValue("Text"),
                        softWrap, new PropertyValue.BooleanValue(false)),
                Map.of(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);
        Node.Property<FlutterPropertyCellValue> property = cellProperty(
                property(node, "softWrap"));

        assertTrue(property.supportsDefaultValue());
        assertFalse(property.isDefaultValue());
        property.restoreDefaultValue();
        property.setValue(FlutterPropertyCellValue.unset());

        assertEquals(List.of(
                new ResetProperty(id, softWrap),
                new ResetProperty(id, softWrap)), commands);
    }

    @Test
    void alreadyUnsetOptionalPropertyIsDefaultAndRestoreIsANoOp() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        WidgetNode widget = textWidget(
                StableId.parse("43f53e24-88f3-498a-b25b-3832535dac40"), "Text");
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);
        Node.Property<FlutterPropertyCellValue> property = cellProperty(
                property(node, "softWrap"));

        assertEquals(FlutterPropertyCellValue.unset(), property.getValue());
        assertTrue(property.supportsDefaultValue());
        assertTrue(property.isDefaultValue());
        property.restoreDefaultValue();
        property.setValue(FlutterPropertyCellValue.unset());

        assertEquals(List.of(), commands);
    }

    @Test
    void requiredPropertyCannotBeResetAndNeverAdvertisesRestoreDefault()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        WidgetNode widget = textWidget(
                StableId.parse("c1616a3c-b236-4a76-a781-ff19bcadf64f"), "Text");
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.Property<FlutterPropertyCellValue> data = cellProperty(
                property(node, "data"));

        assertFalse(data.supportsDefaultValue());
        assertFalse(data.isDefaultValue());
        assertThrows(IllegalArgumentException.class,
                () -> data.setValue(FlutterPropertyCellValue.unset()));
    }

    @Test
    void aspectRatioProjectsProfessionalRequiredPositiveDoubleAndChildSlot()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.AspectRatio");
        StableId id = StableId.parse("7ba9d19a-2a79-43b0-b594-cb5c0613c96f");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);
        Node.Property<FlutterPropertyCellValue> ratio = cellProperty(
                property(node, "aspectRatio"));

        assertAll(
                () -> assertEquals("Aspect ratio", ratio.getDisplayName()),
                () -> assertTrue(ratio.getShortDescription()
                        .contains("Finite width-to-height ratio")),
                () -> assertTrue(ratio.getShortDescription()
                        .contains("cannot be unset")),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        ratio.getValue()),
                () -> assertFalse(ratio.supportsDefaultValue()),
                () -> assertFalse(ratio.isDefaultValue()));

        ratio.restoreDefaultValue();
        assertEquals(List.of(), commands,
                "the required creation default must not expose Reset");
        assertThrows(IllegalArgumentException.class,
                () -> ratio.setValue(FlutterPropertyCellValue.unset()));

        PropertyEditor editor = ratio.getPropertyEditor();
        editor.setAsText("1.7778");
        FlutterPropertyCellValue widescreen = cell(editor);
        assertEquals(
                new PropertyValue.DoubleValue(new BigDecimal("1.7778")),
                widescreen.explicitValue().orElseThrow());
        ratio.setValue(widescreen);
        assertEquals(List.of(new SetProperty(
                        id,
                        new PropertyName("aspectRatio"),
                        new PropertyValue.DoubleValue(new BigDecimal("1.7778")))),
                commands);
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("0"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("NaN"));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("fill the box resolved from Aspect ratio")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void baselineProjectsRequiredFiniteOffsetEnumAndOptionalChild() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Baseline");
        StableId id = StableId.parse("214ecb85-3115-43a8-83b0-a46f96c7350f");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("baseline", "baselineType"),
                names(properties.getProperties()));
        assertEquals(
                "Required baseline offset and type, plus an optional child, "
                + "for the selected Baseline widget.",
                properties.getShortDescription());
        assertEquals(FlutterWidgetPropertiesNode.GENERAL_TAB_NAME,
                properties.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        Node.PropertySet slots = propertySet(
                node, FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals("Slots", slots.getDisplayName());
        assertEquals(FlutterWidgetPropertiesNode.SLOTS_TAB_NAME,
                slots.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));

        Node.Property<FlutterPropertyCellValue> baseline = cellProperty(
                property(node, "baseline"));
        assertAll(
                () -> assertEquals("Baseline", baseline.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.DoubleValue(new BigDecimal("24"))),
                        baseline.getValue()),
                () -> assertTrue(baseline.getShortDescription().contains(
                        "logical-pixel distance from the top")),
                () -> assertTrue(baseline.getShortDescription().contains(
                        "starts at 24")),
                () -> assertTrue(baseline.getShortDescription().contains(
                        "Negative values")),
                () -> assertTrue(baseline.getShortDescription().contains(
                        "cannot be unset")),
                () -> assertFalse(baseline.supportsDefaultValue()),
                () -> assertFalse(baseline.isDefaultValue()));
        PropertyEditor baselineEditor = baseline.getPropertyEditor();
        baselineEditor.setAsText("-3.5");
        FlutterPropertyCellValue raised = cell(baselineEditor);
        assertEquals(
                new PropertyValue.DoubleValue(new BigDecimal("-3.5")),
                raised.explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> baselineEditor.setAsText("NaN"));
        assertThrows(IllegalArgumentException.class,
                () -> baselineEditor.setAsText("Infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> baseline.setValue(FlutterPropertyCellValue.unset()));

        Node.Property<FlutterPropertyCellValue> baselineType = cellProperty(
                property(node, "baselineType"));
        assertAll(
                () -> assertEquals("Baseline type", baselineType.getDisplayName()),
                () -> assertEquals(List.of("alphabetic", "ideographic"),
                        List.of(baselineType.getPropertyEditor().getTags())),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.EnumValue(
                                        "TextBaseline", "alphabetic")),
                        baselineType.getValue()),
                () -> assertTrue(baselineType.getShortDescription().contains(
                        "alphabetic scripts")),
                () -> assertTrue(baselineType.getShortDescription().contains(
                        "bottom edge")),
                () -> assertTrue(baselineType.getShortDescription().contains(
                        "cannot be unset")),
                () -> assertFalse(baselineType.supportsDefaultValue()));
        PropertyEditor typeEditor = baselineType.getPropertyEditor();
        typeEditor.setAsText("ideographic");
        FlutterPropertyCellValue ideographic = cell(typeEditor);
        assertEquals(
                new PropertyValue.EnumValue("TextBaseline", "ideographic"),
                ideographic.explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> typeEditor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT));

        baseline.setValue(raised);
        baselineType.setValue(ideographic);
        baseline.restoreDefaultValue();
        baselineType.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("baseline"),
                        raised.explicitValue().orElseThrow()),
                new SetProperty(id, new PropertyName("baselineType"),
                        ideographic.explicitValue().orElseThrow())),
                commands,
                "required constructor arguments never expose Restore Default");

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "alphabetic or ideographic baseline")),
                () -> assertTrue(child.getShortDescription().contains(
                        "bottom edge")),
                () -> assertTrue(child.getShortDescription().contains(
                        "shift the child above this box")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void intrinsicHeightProjectsNoWritablePropertiesAndPerformanceAwareOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.IntrinsicHeight");
        StableId id = StableId.parse("aa9f06c2-69ae-4d34-94a3-439f0f77ba7c");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(), names(properties.getProperties()));
        assertEquals(
                "Intrinsic-height sizing and optional child for the selected "
                + "IntrinsicHeight widget. Flutter performs a speculative layout pass "
                + "that can be O(N²) in tree depth.",
                properties.getShortDescription());
        assertEquals(FlutterWidgetPropertiesNode.GENERAL_TAB_NAME,
                properties.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "sized to its intrinsic height")),
                () -> assertTrue(child.getShortDescription().contains(
                        "speculative layout pass")),
                () -> assertTrue(child.getShortDescription().contains("O(N²)")),
                () -> assertTrue(child.getShortDescription().contains(
                        "prefer ordinary constraints")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void repaintBoundaryIntentionallyHasNoScalarPropertiesAndExplainsWhereChildAndDescendantsAreEdited()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.RepaintBoundary");
        StableId id = StableId.parse("da9f06c2-69ae-4d34-94a3-439f0f77ba7c");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });
        Node.PropertySet properties = propertySet(node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(), names(properties.getProperties()));
        assertTrue(properties.getShortDescription().contains("no scalar constructor properties"));
        assertTrue(properties.getShortDescription().contains("separate display list"));
        assertTrue(properties.getShortDescription().contains("does not guarantee faster rendering"));
        assertTrue(node.getShortDescription().contains("edit Child in Slots"));
        assertTrue(node.getShortDescription().contains(id.toString()));
        assertFalse(node.getShortDescription().contains("unsupported"));
        assertEquals(FlutterWidgetPropertiesNode.GENERAL_TAB_NAME,
                properties.getValue(FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        Node.Property<?> child = property(node, "child");
        assertEquals("Empty", child.getValue());
        assertTrue(child.getShortDescription().contains("Occupancy: 0/1"));
        assertTrue(child.getShortDescription().contains("add, move, replace, or remove"));
        assertTrue(child.getShortDescription().contains("select a descendant"));
    }

    @Test
    void intrinsicWidthProjectsBothTypedStepsResetAndPerformanceAwareOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.IntrinsicWidth");
        StableId id = StableId.parse("416354f4-ceb8-4f39-8ea5-708d9db64cf3");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, id);
        PropertyValue.DoubleValue initialWidth =
                new PropertyValue.DoubleValue(new BigDecimal("24"));
        PropertyValue.DoubleValue initialHeight =
                new PropertyValue.DoubleValue(new BigDecimal("12.5"));
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        new PropertyName("stepWidth"), initialWidth,
                        new PropertyName("stepHeight"), initialHeight),
                prototype.slots(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("stepWidth", "stepHeight"),
                names(properties.getProperties()));
        assertEquals(
                "Maximum-intrinsic-width sizing, optional width and height step "
                + "snapping, parent constraints, and optional child for the selected "
                + "IntrinsicWidth widget. Flutter performs a speculative layout pass "
                + "that can be O(N²) in tree depth.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> stepWidth = cellProperty(
                property(node, "stepWidth"));
        Node.Property<FlutterPropertyCellValue> stepHeight = cellProperty(
                property(node, "stepHeight"));
        assertAll(
                () -> assertEquals("Step width", stepWidth.getDisplayName()),
                () -> assertEquals("Step height", stepHeight.getDisplayName()),
                () -> assertEquals(initialWidth,
                        stepWidth.getValue().explicitValue().orElseThrow()),
                () -> assertEquals(initialHeight,
                        stepHeight.getValue().explicitValue().orElseThrow()),
                () -> assertTrue(stepWidth.getShortDescription().contains(
                        "maximum intrinsic width")),
                () -> assertTrue(stepWidth.getShortDescription().contains(
                        "subject to the parent constraints")),
                () -> assertTrue(stepWidth.getShortDescription().contains(
                        "Omission or zero")),
                () -> assertTrue(stepHeight.getShortDescription().contains(
                        "unconstrained height")),
                () -> assertTrue(stepHeight.getShortDescription().contains(
                        "subject to the parent constraints")),
                () -> assertTrue(stepWidth.supportsDefaultValue()),
                () -> assertTrue(stepHeight.supportsDefaultValue()),
                () -> assertFalse(stepWidth.isDefaultValue()),
                () -> assertFalse(stepHeight.isDefaultValue()));

        PropertyEditor widthEditor = stepWidth.getPropertyEditor();
        widthEditor.setAsText("0");
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ZERO),
                cell(widthEditor).explicitValue().orElseThrow());
        PropertyEditor heightEditor = stepHeight.getPropertyEditor();
        heightEditor.setAsText("32.25");
        PropertyValue.DoubleValue updatedHeight =
                new PropertyValue.DoubleValue(new BigDecimal("32.25"));
        assertEquals(updatedHeight, cell(heightEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("-0.1"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("NaN"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("Infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> stepWidth.setValue(FlutterPropertyCellValue.explicit(
                        new PropertyValue.IntegerValue(BigInteger.ZERO))));
        assertTrue(commands.isEmpty(),
                "editor validation must not partially submit a command");

        stepWidth.restoreDefaultValue();
        stepHeight.setValue(FlutterPropertyCellValue.explicit(updatedHeight));
        assertEquals(List.of(
                new ResetProperty(id, new PropertyName("stepWidth")),
                new SetProperty(id, new PropertyName("stepHeight"), updatedHeight)),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "maximum intrinsic width")),
                () -> assertTrue(child.getShortDescription().contains(
                        "width and height step snapping")),
                () -> assertTrue(child.getShortDescription().contains(
                        "bounded by the parent constraints")),
                () -> assertTrue(child.getShortDescription().contains(
                        "speculative layout pass")),
                () -> assertTrue(child.getShortDescription().contains("O(N²)")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void offstageProjectsNullableCheckboxResetAndActiveHiddenChildContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Offstage");
        StableId id = StableId.parse("75fd302b-5d49-453b-a55a-d491a85ec6c4");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, id);
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(true)),
                prototype.slots(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("offstage"), names(properties.getProperties()));
        assertEquals(
                "Visibility, layout participation, focus, animation, and optional "
                + "child contract for the selected Offstage widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> offstage = cellProperty(
                property(node, "offstage"));
        assertAll(
                () -> assertEquals("Offstage", offstage.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(true)),
                        offstage.getValue()),
                () -> assertNull(offstage.getPropertyEditor().getTags(),
                        "boolean values must use the global checkbox editor, not a combo"),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "still lays out the child")),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "does not paint or hit-test")),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "takes no parent layout space")),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "can receive focus")),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "animations continue to run")),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "default true")),
                () -> assertTrue(offstage.getShortDescription().contains(
                        "hiding it long-term")),
                () -> assertTrue(offstage.supportsDefaultValue()),
                () -> assertFalse(offstage.isDefaultValue()));

        PropertyEditor editor = offstage.getPropertyEditor();
        editor.setAsText(FlutterWidgetPropertiesNode.NOT_SET);
        assertEquals(FlutterPropertyCellValue.unset(), cell(editor),
                "<not set> remains a valid nullable boolean value");
        editor.setAsText("false");
        FlutterPropertyCellValue shown = cell(editor);
        assertEquals(new PropertyValue.BooleanValue(false),
                shown.explicitValue().orElseThrow());

        offstage.setValue(shown);
        offstage.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(
                        id,
                        new PropertyName("offstage"),
                        new PropertyValue.BooleanValue(false)),
                new ResetProperty(id, new PropertyName("offstage"))),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "remains laid out and active")),
                () -> assertTrue(child.getShortDescription().contains(
                        "is not painted")),
                () -> assertTrue(child.getShortDescription().contains(
                        "cannot be hit tested")),
                () -> assertTrue(child.getShortDescription().contains(
                        "occupies no parent layout space")),
                () -> assertTrue(child.getShortDescription().contains(
                        "receive focus and run animations")),
                () -> assertTrue(child.getShortDescription().contains(
                        "hiding it long-term")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));

        WidgetNode omitted = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("0ab6f58f-29e1-4faf-b54f-321cd4e5c611"));
        FlutterWidgetPropertiesNode omittedNode = new FlutterWidgetPropertiesNode(
                Children.LEAF, omitted, definition, ignored -> { });
        Node.Property<FlutterPropertyCellValue> omittedOffstage = cellProperty(
                property(omittedNode, "offstage"));
        assertAll(
                () -> assertEquals(FlutterPropertyCellValue.unset(),
                        omittedOffstage.getValue()),
                () -> assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        omittedOffstage.getPropertyEditor().getAsText()),
                () -> assertTrue(omittedOffstage.isDefaultValue()));
    }

    @Test
    void sizedOverflowBoxProjectsRequiredSizeAlignmentAndOverflowingChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.SizedOverflowBox");
        StableId id = StableId.parse("c8c3690e-8642-4ee5-bb1b-62071dfa7f16");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("size", "alignment"),
                names(properties.getProperties()));
        assertEquals(
                "Required requested size, child alignment, parent-constraint behavior, "
                + "and optional overflowing child contract for the selected "
                + "SizedOverflowBox widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> size = cellProperty(
                property(node, "size"));
        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        PropertyValue.SizeValue initial = new PropertyValue.SizeValue(
                BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        PropertyEditor sizeEditor = size.getPropertyEditor();
        sizeEditor.setValue(size.getValue());
        assertAll(
                () -> assertEquals("Size", size.getDisplayName()),
                () -> assertEquals(initial,
                        size.getValue().explicitValue().orElseThrow()),
                () -> assertEquals("100 × 100", sizeEditor.getAsText()),
                () -> assertTrue(sizeEditor.supportsCustomEditor()),
                () -> assertFalse(size.supportsDefaultValue()),
                () -> assertFalse(size.isDefaultValue()),
                () -> assertTrue(size.getShortDescription().contains(
                        "Required finite non-negative")),
                () -> assertTrue(size.getShortDescription().contains(
                        "Parent constraints still constrain")),
                () -> assertTrue(size.getShortDescription().contains(
                        "pass through unchanged to the child")),
                () -> assertTrue(size.getShortDescription().contains("100 × 100")),
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(),
                        alignment.getValue()),
                () -> assertTrue(alignment.supportsDefaultValue()),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "ambient TextDirection")),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "defaults to center")));

        size.restoreDefaultValue();
        assertTrue(commands.isEmpty(),
                "a required Size must never expose Restore Default");
        assertThrows(IllegalArgumentException.class,
                () -> size.setValue(FlutterPropertyCellValue.unset()));

        PropertyValue.SizeValue firstEdit = new PropertyValue.SizeValue(
                new BigDecimal("120.5"), new BigDecimal("64"));
        size.setValue(FlutterPropertyCellValue.explicit(firstEdit));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE,
                        BigDecimal.ONE.negate());
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("size"), firstEdit),
                new SetProperty(id, new PropertyName("alignment"), directional)),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "original incoming parent constraints")),
                () -> assertTrue(child.getShortDescription().contains(
                        "rather than tight constraints")),
                () -> assertTrue(child.getShortDescription().contains(
                        "may paint outside")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void transformProjectsMatrixPivotAlignmentHitTestsQualityAndChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Transform");
        StableId id = StableId.parse("1bbc0435-da77-41ab-88b4-ae10f1e62712");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                "transform", "origin", "alignment", "transformHitTests", "filterQuality"),
                names(properties.getProperties()));
        assertEquals(
                "Paint-time Matrix4, pivot origin, alignment, hit-testing, filter quality, "
                + "and optional child contract for the selected Transform widget; layout "
                + "size is unchanged.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> transform = cellProperty(
                property(node, "transform"));
        Node.Property<FlutterPropertyCellValue> origin = cellProperty(
                property(node, "origin"));
        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        Node.Property<FlutterPropertyCellValue> hitTests = cellProperty(
                property(node, "transformHitTests"));
        Node.Property<FlutterPropertyCellValue> quality = cellProperty(
                property(node, "filterQuality"));
        PropertyEditor transformEditor = transform.getPropertyEditor();
        transformEditor.setValue(transform.getValue());
        PropertyEditor originEditor = origin.getPropertyEditor();
        originEditor.setValue(origin.getValue());
        assertAll(
                () -> assertEquals("Transform", transform.getDisplayName()),
                () -> assertEquals(identityMatrix(),
                        transform.getValue().explicitValue().orElseThrow()),
                () -> assertEquals("Identity matrix", transformEditor.getAsText()),
                () -> assertTrue(transformEditor.supportsCustomEditor()),
                () -> assertFalse(transform.supportsDefaultValue()),
                () -> assertFalse(transform.isDefaultValue()),
                () -> assertTrue(transform.getShortDescription().contains(
                        "Required finite 4×4 column-major Matrix4")),
                () -> assertTrue(transform.getShortDescription().contains(
                        "identity matrix")),
                () -> assertTrue(transform.getShortDescription().contains(
                        "not the child's layout size")),
                () -> assertEquals("Origin", origin.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), origin.getValue()),
                () -> assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        originEditor.getAsText()),
                () -> assertTrue(originEditor.supportsCustomEditor()),
                () -> assertTrue(origin.supportsDefaultValue()),
                () -> assertTrue(origin.getShortDescription().contains("finite signed")),
                () -> assertTrue(origin.getShortDescription().contains("negative dx and dy")),
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "ambient TextDirection")),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "Omission passes null and adds no alignment pivot")),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "Center defaults belong only to unsupported Transform convenience")),
                () -> assertEquals("Transform hit tests", hitTests.getDisplayName()),
                () -> assertTrue(hitTests.getShortDescription().contains("default true")),
                () -> assertEquals("Filter quality", quality.getDisplayName()),
                () -> assertEquals(
                        List.of(FlutterWidgetPropertiesNode.NOT_SET,
                                "none", "low", "medium", "high"),
                        List.of(quality.getPropertyEditor().getTags())),
                () -> assertTrue(quality.getShortDescription().contains(
                        "higher quality can require more rendering work")));

        transform.restoreDefaultValue();
        assertTrue(commands.isEmpty(),
                "a required Transform matrix must never expose Restore Default");
        assertThrows(IllegalArgumentException.class,
                () -> transform.setValue(FlutterPropertyCellValue.unset()));

        PropertyValue.Matrix4Value translated = translatedMatrix("12.5", "-8");
        PropertyValue.OffsetValue pivot = new PropertyValue.OffsetValue(
                new BigDecimal("-4.25"), new BigDecimal("6.5"));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE,
                        BigDecimal.ONE.negate());
        PropertyValue.BooleanValue untransformedHitTests =
                new PropertyValue.BooleanValue(false);
        PropertyValue.EnumValue highQuality =
                new PropertyValue.EnumValue("FilterQuality", "high");
        transform.setValue(FlutterPropertyCellValue.explicit(translated));
        origin.setValue(FlutterPropertyCellValue.explicit(pivot));
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        hitTests.setValue(FlutterPropertyCellValue.explicit(untransformedHitTests));
        quality.setValue(FlutterPropertyCellValue.explicit(highQuality));
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("transform"), translated),
                new SetProperty(id, new PropertyName("origin"), pivot),
                new SetProperty(id, new PropertyName("alignment"), directional),
                new SetProperty(id, new PropertyName("transformHitTests"),
                        untransformedHitTests),
                new SetProperty(id, new PropertyName("filterQuality"), highQuality)),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "laid out at its ordinary size")),
                () -> assertTrue(child.getShortDescription().contains(
                        "does not change parent layout")),
                () -> assertTrue(child.getShortDescription().contains(
                        "hit testing follows the painted child")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void rotatedBoxProjectsRequiredSignedQuarterTurnsAndOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.RotatedBox");
        StableId id = StableId.parse("de241b46-a5eb-4afe-b751-49336fcbd421");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("quarterTurns"), names(properties.getProperties()));
        assertEquals(
                "Required signed quarter-turn rotation applied before layout and optional "
                + "child contract for the selected RotatedBox widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> quarterTurns = cellProperty(
                property(node, "quarterTurns"));
        assertAll(
                () -> assertEquals("Quarter turns", quarterTurns.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.IntegerValue(BigInteger.ONE)),
                        quarterTurns.getValue()),
                () -> assertTrue(quarterTurns.getShortDescription().contains(
                        "clockwise quarter turns")),
                () -> assertTrue(quarterTurns.getShortDescription().contains(
                        "Negative values rotate counter-clockwise")),
                () -> assertTrue(quarterTurns.getShortDescription().contains(
                        "multiples of four preserve orientation")),
                () -> assertTrue(quarterTurns.getShortDescription().contains(
                        "odd values swap the child's width and height")),
                () -> assertTrue(quarterTurns.getShortDescription().contains(
                        "creation starts at 1")),
                () -> assertTrue(quarterTurns.getShortDescription().contains(
                        "cannot be unset")),
                () -> assertFalse(quarterTurns.supportsDefaultValue()),
                () -> assertFalse(quarterTurns.isDefaultValue()));

        PropertyEditor editor = quarterTurns.getPropertyEditor();
        editor.setAsText("-3");
        FlutterPropertyCellValue counterClockwise = cell(editor);
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(-3)),
                counterClockwise.explicitValue().orElseThrow());
        editor.setAsText("9007199254740991");
        assertEquals(
                new PropertyValue.IntegerValue(new BigInteger("9007199254740991")),
                cell(editor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("9007199254740992"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("-9007199254740992"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("1.5"));
        assertThrows(IllegalArgumentException.class,
                () -> quarterTurns.setValue(FlutterPropertyCellValue.unset()));

        quarterTurns.setValue(counterClockwise);
        quarterTurns.restoreDefaultValue();
        assertEquals(List.of(new SetProperty(
                        id,
                        new PropertyName("quarterTurns"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(-3)))),
                commands,
                "required quarterTurns never exposes Restore Default");

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "rotated clockwise")),
                () -> assertTrue(child.getShortDescription().contains(
                        "before layout")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Odd turns swap the child's width and height")),
                () -> assertTrue(child.getShortDescription().contains(
                        "negative turns rotate counter-clockwise")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void listBodyProjectsAxisReverseAndExactOrderedChildrenContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.ListBody");
        StableId id = StableId.parse("7adeb901-04e7-4078-85df-8ca88ae67b37");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("mainAxis", "reverse"), names(properties.getProperties()));
        assertEquals(
                "Sequential main-axis layout, reading-direction reversal, and exact ordered "
                + "children for the selected ListBody widget; the main axis must be "
                + "unbounded and the cross axis bounded.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> mainAxis = cellProperty(
                property(node, "mainAxis"));
        assertAll(
                () -> assertEquals("Main axis", mainAxis.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), mainAxis.getValue()),
                () -> assertEquals(List.of("<not set>", "horizontal", "vertical"),
                        List.of(mainAxis.getPropertyEditor().getTags())),
                () -> assertTrue(mainAxis.getShortDescription().contains(
                        "vertical default")),
                () -> assertTrue(mainAxis.getShortDescription().contains(
                        "unbounded space")),
                () -> assertTrue(mainAxis.getShortDescription().contains(
                        "bounded cross axis")),
                () -> assertTrue(mainAxis.supportsDefaultValue()),
                () -> assertTrue(mainAxis.isDefaultValue()));
        PropertyEditor axisEditor = mainAxis.getPropertyEditor();
        axisEditor.setAsText("horizontal");
        mainAxis.setValue(cell(axisEditor));

        Node.Property<FlutterPropertyCellValue> reverse = cellProperty(
                property(node, "reverse"));
        assertAll(
                () -> assertEquals("Reverse", reverse.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), reverse.getValue()),
                () -> assertNull(reverse.getPropertyEditor().getTags(),
                        "optional booleans use the global nullable checkbox editor"),
                () -> assertTrue(reverse.getPropertyEditor().isPaintable()),
                () -> assertTrue(reverse.getShortDescription().contains(
                        "false default")),
                () -> assertTrue(reverse.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(reverse.getShortDescription().contains(
                        "without changing the stored source order")));
        PropertyEditor reverseEditor = reverse.getPropertyEditor();
        reverseEditor.setAsText("true");
        reverse.setValue(cell(reverseEditor));

        assertEquals(List.of(
                new SetProperty(
                        id,
                        new PropertyName("mainAxis"),
                        new PropertyValue.EnumValue("Axis", "horizontal")),
                new SetProperty(
                        id,
                        new PropertyName("reverse"),
                        new PropertyValue.BooleanValue(true))),
                commands);

        WidgetNode explicitReverseWidget = new WidgetNode(
                widget.id(),
                widget.type(),
                Map.of(
                        new PropertyName("reverse"),
                        new PropertyValue.BooleanValue(true)),
                widget.slots(),
                widget.extensions());
        List<DesignerCommand> resetCommands = new ArrayList<>();
        FlutterWidgetPropertiesNode explicitReverseNode =
                new FlutterWidgetPropertiesNode(
                        Children.LEAF,
                        explicitReverseWidget,
                        definition,
                        resetCommands::add);
        Node.Property<FlutterPropertyCellValue> explicitReverse = cellProperty(
                property(explicitReverseNode, "reverse"));
        assertFalse(explicitReverse.isDefaultValue());
        explicitReverse.restoreDefaultValue();
        assertEquals(
                List.of(new ResetProperty(id, new PropertyName("reverse"))),
                resetCommands,
                "Restore Default is exercised after the refreshed node projects the "
                + "explicit saved value");

        Node.Property<?> children = property(node, "children");
        assertAll(
                () -> assertEquals("Children", children.getDisplayName()),
                () -> assertEquals("Empty", children.getValue()),
                () -> assertTrue(children.getShortDescription().contains(
                        "laid out sequentially along Main axis")),
                () -> assertTrue(children.getShortDescription().contains(
                        "stretched across the bounded cross axis")),
                () -> assertTrue(children.getShortDescription().contains(
                        "requires unbounded space along its main axis")),
                () -> assertTrue(children.getShortDescription().contains(
                        "stored source order")),
                () -> assertTrue(children.getShortDescription().contains(
                        "Occupancy: 0/10000")));
    }

    @Test
    void overflowBarProjectsEveryResponsivePropertyAndExactOrderedChildrenContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.OverflowBar");
        StableId id = StableId.parse("f85a0266-0e4d-4de4-b727-02b50e537617");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                        "spacing", "alignment", "overflowSpacing", "overflowAlignment",
                        "overflowDirection", "textDirection"),
                names(properties.getProperties()));
        assertEquals(
                "Responsive horizontal-row or vertical-overflow layout, spacing, alignment, "
                + "direction, and exact ordered children for the selected OverflowBar widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> spacing = cellProperty(
                property(node, "spacing"));
        assertAll(
                () -> assertEquals("Spacing", spacing.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), spacing.getValue()),
                () -> assertTrue(spacing.getShortDescription().contains("Finite signed")),
                () -> assertTrue(spacing.getShortDescription().contains("0.0 default")),
                () -> assertTrue(spacing.getShortDescription().contains(
                        "only when deciding whether the row overflows")));
        PropertyEditor spacingEditor = spacing.getPropertyEditor();
        spacingEditor.setAsText("-4.5");
        spacing.setValue(cell(spacingEditor));
        assertThrows(IllegalArgumentException.class,
                () -> spacingEditor.setAsText("NaN"));

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        assertAll(
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertEquals(List.of(
                                "<not set>", "start", "end", "center", "spaceBetween",
                                "spaceAround", "spaceEvenly"),
                        List.of(alignment.getPropertyEditor().getTags())),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "only as wide as its children")),
                () -> assertTrue(alignment.getShortDescription().contains(
                        "ignored after the layout switches")));
        PropertyEditor alignmentEditor = alignment.getPropertyEditor();
        alignmentEditor.setAsText("spaceBetween");
        alignment.setValue(cell(alignmentEditor));

        Node.Property<FlutterPropertyCellValue> overflowSpacing = cellProperty(
                property(node, "overflowSpacing"));
        assertAll(
                () -> assertEquals("Overflow spacing", overflowSpacing.getDisplayName()),
                () -> assertTrue(overflowSpacing.getShortDescription().contains(
                        "Finite signed")),
                () -> assertTrue(overflowSpacing.getShortDescription().contains(
                        "unused while the row fits")));
        PropertyEditor overflowSpacingEditor = overflowSpacing.getPropertyEditor();
        overflowSpacingEditor.setAsText("6.25");
        overflowSpacing.setValue(cell(overflowSpacingEditor));

        Node.Property<FlutterPropertyCellValue> overflowAlignment = cellProperty(
                property(node, "overflowAlignment"));
        assertAll(
                () -> assertEquals("Overflow alignment",
                        overflowAlignment.getDisplayName()),
                () -> assertEquals(List.of("<not set>", "start", "end", "center"),
                        List.of(overflowAlignment.getPropertyEditor().getTags())),
                () -> assertTrue(overflowAlignment.getShortDescription().contains(
                        "explicit or ambient Text direction")),
                () -> assertTrue(overflowAlignment.getShortDescription().contains(
                        "start default")));
        PropertyEditor overflowAlignmentEditor = overflowAlignment.getPropertyEditor();
        overflowAlignmentEditor.setAsText("end");
        overflowAlignment.setValue(cell(overflowAlignmentEditor));

        Node.Property<FlutterPropertyCellValue> overflowDirection = cellProperty(
                property(node, "overflowDirection"));
        assertAll(
                () -> assertEquals("Overflow direction",
                        overflowDirection.getDisplayName()),
                () -> assertEquals(List.of("<not set>", "up", "down"),
                        List.of(overflowDirection.getPropertyEditor().getTags())),
                () -> assertTrue(overflowDirection.getShortDescription().contains(
                        "first stored child at the top")),
                () -> assertTrue(overflowDirection.getShortDescription().contains(
                        "never changes stored source order")));
        PropertyEditor overflowDirectionEditor = overflowDirection.getPropertyEditor();
        overflowDirectionEditor.setAsText("up");
        overflowDirection.setValue(cell(overflowDirectionEditor));

        Node.Property<FlutterPropertyCellValue> textDirection = cellProperty(
                property(node, "textDirection"));
        assertAll(
                () -> assertEquals("Text direction", textDirection.getDisplayName()),
                () -> assertEquals(List.of("<not set>", "rtl", "ltr"),
                        List.of(textDirection.getPropertyEditor().getTags())),
                () -> assertTrue(textDirection.getShortDescription().contains(
                        "ambient Directionality")),
                () -> assertTrue(textDirection.getShortDescription().contains(
                        "not read from the theme")));
        PropertyEditor textDirectionEditor = textDirection.getPropertyEditor();
        textDirectionEditor.setAsText("rtl");
        textDirection.setValue(cell(textDirectionEditor));

        assertEquals(List.of(
                new SetProperty(id, new PropertyName("spacing"),
                        new PropertyValue.DoubleValue(new BigDecimal("-4.5"))),
                new SetProperty(id, new PropertyName("alignment"),
                        new PropertyValue.EnumValue(
                                "MainAxisAlignment", "spaceBetween")),
                new SetProperty(id, new PropertyName("overflowSpacing"),
                        new PropertyValue.DoubleValue(new BigDecimal("6.25"))),
                new SetProperty(id, new PropertyName("overflowAlignment"),
                        new PropertyValue.EnumValue("OverflowBarAlignment", "end")),
                new SetProperty(id, new PropertyName("overflowDirection"),
                        new PropertyValue.EnumValue("VerticalDirection", "up")),
                new SetProperty(id, new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "rtl"))),
                commands);

        Node.Property<?> children = property(node, "children");
        assertAll(
                () -> assertEquals("Children", children.getDisplayName()),
                () -> assertEquals("Empty", children.getValue()),
                () -> assertTrue(children.getShortDescription().contains(
                        "one horizontal row")),
                () -> assertTrue(children.getShortDescription().contains(
                        "vertical overflow column")),
                () -> assertTrue(children.getShortDescription().contains(
                        "without changing the stored source order")),
                () -> assertTrue(children.getShortDescription().contains(
                        "Occupancy: 0/10000")));
    }

    @Test
    void opacityProjectsRequiredUnitDoubleOptionalSemanticsAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Opacity");
        StableId id = StableId.parse("e502bc32-1ce7-48ce-80f3-c85586040e68");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        new PropertyName("opacity"),
                                new PropertyValue.DoubleValue(new BigDecimal("0.5")),
                        new PropertyName("alwaysIncludeSemantics"),
                                new PropertyValue.BooleanValue(true)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.Property<FlutterPropertyCellValue> alpha = cellProperty(
                property(node, "opacity"));
        assertAll(
                () -> assertEquals("Opacity", alpha.getDisplayName()),
                () -> assertTrue(alpha.getShortDescription()
                        .contains("0 (fully transparent)")),
                () -> assertTrue(alpha.getShortDescription()
                        .contains("1 (fully opaque)")),
                () -> assertTrue(alpha.getShortDescription()
                        .contains("intermediate rendered alpha values")),
                () -> assertTrue(alpha.getShortDescription()
                        .contains("cannot be unset")),
                () -> assertFalse(alpha.supportsDefaultValue()));
        PropertyEditor alphaEditor = alpha.getPropertyEditor();
        alphaEditor.setAsText("0");
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ZERO),
                cell(alphaEditor).explicitValue().orElseThrow());
        alphaEditor.setAsText("1");
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                cell(alphaEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> alphaEditor.setAsText("-0.001"));
        assertThrows(IllegalArgumentException.class,
                () -> alphaEditor.setAsText("1.001"));
        assertThrows(IllegalArgumentException.class,
                () -> alpha.setValue(FlutterPropertyCellValue.unset()));

        Node.Property<FlutterPropertyCellValue> semantics = cellProperty(
                property(node, "alwaysIncludeSemantics"));
        assertAll(
                () -> assertEquals("Always include semantics",
                        semantics.getDisplayName()),
                () -> assertTrue(semantics.getShortDescription()
                        .contains("Flutter defaults to false")),
                () -> assertTrue(semantics.supportsDefaultValue()),
                () -> assertFalse(semantics.isDefaultValue()));
        semantics.restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(
                        id, new PropertyName("alwaysIncludeSemantics"))),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("selected group opacity")),
                () -> assertTrue(child.getShortDescription()
                        .contains("selection outline and hit target visible")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void coloredBoxProjectsRequiredThemeAwareColorOptionalCheckboxAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.ColoredBox");
        StableId id = StableId.parse("448bd3fd-c269-4d72-8f72-761e196392cb");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        new PropertyName("color"),
                                new PropertyValue.ColorValue(0xFF2196F3L),
                        new PropertyName("isAntiAlias"),
                                new PropertyValue.BooleanValue(true)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + ColoredBoxWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(ColoredBoxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                Arrays.stream(sets)
                        .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                                set.getName()))
                        .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(
                                set.getName()))
                        .mapToInt(set -> set.getProperties().length).sum());
        Node.PropertySet properties = propertySet(
                node,
                ColoredBoxWidgetPropertySchema.Group.APPEARANCE.setName());
        assertEquals(List.of("color", "isAntiAlias"),
                names(properties.getProperties()));
        assertEquals(
                ColoredBoxWidgetPropertySchema.Group.APPEARANCE.displayName(),
                properties.getDisplayName());
        assertEquals(
                ColoredBoxWidgetPropertySchema.Group.APPEARANCE.description(),
                properties.getShortDescription());
        assertEquals("General", properties.getValue(
                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));

        Node.Property<FlutterPropertyCellValue> color = cellProperty(
                property(node, "color"));
        assertAll(
                () -> assertEquals("Color", color.getDisplayName()),
                () -> assertTrue(color.getShortDescription().contains("literal ARGB")),
                () -> assertTrue(color.getShortDescription().contains("Material theme token")),
                () -> assertTrue(color.getShortDescription().contains("0xFF2196F3")),
                () -> assertFalse(color.supportsDefaultValue()));
        PropertyEditor colorEditor = color.getPropertyEditor();
        colorEditor.setAsText("0xFF336699");
        color.setValue(cell(colorEditor));
        assertThrows(IllegalArgumentException.class,
                () -> color.setValue(FlutterPropertyCellValue.unset()));

        Node.Property<FlutterPropertyCellValue> antiAlias = cellProperty(
                property(node, "isAntiAlias"));
        assertAll(
                () -> assertEquals("Anti-alias", antiAlias.getDisplayName()),
                () -> assertTrue(antiAlias.getShortDescription().contains(
                        "enabled default")),
                () -> assertTrue(antiAlias.getShortDescription().contains(
                        "checkbox editor")),
                () -> assertTrue(antiAlias.supportsDefaultValue()));
        PropertyEditor antiAliasEditor = antiAlias.getPropertyEditor();
        antiAliasEditor.setAsText("false");
        antiAlias.setValue(cell(antiAliasEditor));
        antiAlias.restoreDefaultValue();

        assertEquals(List.of(
                new SetProperty(id, new PropertyName("color"),
                        new PropertyValue.ColorValue(0xFF336699L)),
                new SetProperty(id, new PropertyName("isAntiAlias"),
                        new PropertyValue.BooleanValue(false)),
                new ResetProperty(id, new PropertyName("isAntiAlias"))),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "required solid background color")),
                () -> assertTrue(child.getShortDescription().contains(
                        "non-persisted Designer selection and drop target")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void placeholderProjectsExactGroupsOptionalThemeColorNumbersAndChild()
            throws Exception {
        WidgetDefinition definition = definition(
                PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE.value());
        StableId id = StableId.parse("1328b7ca-523e-419b-ad48-19100e4f01e4");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + PlaceholderWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(PlaceholderWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                Arrays.stream(sets)
                        .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                                set.getName()))
                        .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(
                                set.getName()))
                        .mapToInt(set -> set.getProperties().length).sum());
        for (PlaceholderWidgetPropertySchema.Group group
                : PlaceholderWidgetPropertySchema.Group.values()) {
            Node.PropertySet set = propertySet(node, group.setName());
            assertEquals(group.displayName(), set.getDisplayName());
            assertEquals(group.description(), set.getShortDescription());
            assertEquals("General", set.getValue(
                    FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        }
        assertEquals(List.of("color", "strokeWidth"), names(propertySet(
                node, PlaceholderWidgetPropertySchema.Group.APPEARANCE.setName())
                        .getProperties()));
        assertEquals(List.of("fallbackWidth", "fallbackHeight"), names(propertySet(
                node, PlaceholderWidgetPropertySchema.Group.FALLBACK_SIZE.setName())
                        .getProperties()));

        Node.Property<FlutterPropertyCellValue> color = cellProperty(
                property(node, "color"));
        Node.Property<FlutterPropertyCellValue> strokeWidth = cellProperty(
                property(node, "strokeWidth"));
        Node.Property<FlutterPropertyCellValue> fallbackWidth = cellProperty(
                property(node, "fallbackWidth"));
        Node.Property<FlutterPropertyCellValue> fallbackHeight = cellProperty(
                property(node, "fallbackHeight"));
        for (Node.Property<FlutterPropertyCellValue> property : List.of(
                color, strokeWidth, fallbackWidth, fallbackHeight)) {
            assertEquals(FlutterPropertyCellValue.unset(), property.getValue());
            assertTrue(property.supportsDefaultValue(), property.getName());
            assertTrue(property.isDefaultValue(), property.getName());
        }
        assertAll(
                () -> assertEquals("Color", color.getDisplayName()),
                () -> assertTrue(color.getShortDescription().contains("literal ARGB")),
                () -> assertTrue(color.getShortDescription().contains(
                        "Material theme token")),
                () -> assertTrue(color.getShortDescription().contains("0xFF455A64")),
                () -> assertEquals("Stroke width", strokeWidth.getDisplayName()),
                () -> assertTrue(strokeWidth.getShortDescription().contains(
                        "Finite non-negative")),
                () -> assertTrue(strokeWidth.getShortDescription().contains("hairline")),
                () -> assertEquals("Fallback width", fallbackWidth.getDisplayName()),
                () -> assertTrue(fallbackWidth.getShortDescription().contains(
                        "incoming width is unbounded")),
                () -> assertEquals("Fallback height", fallbackHeight.getDisplayName()),
                () -> assertTrue(fallbackHeight.getShortDescription().contains(
                        "incoming height is unbounded")));

        PropertyValue.ThemeTokenValue primary = new PropertyValue.ThemeTokenValue(
                new ThemeToken("material.colorScheme.primary"));
        color.setValue(FlutterPropertyCellValue.explicit(primary));
        strokeWidth.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.DoubleValue(new BigDecimal("2.5"))));
        fallbackWidth.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.IntegerValue(BigInteger.valueOf(320))));
        fallbackHeight.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.DoubleValue(new BigDecimal("480.5"))));
        WidgetNode editedWidget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(new PropertyName("fallbackWidth"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(320))),
                widget.slots(),
                Extensions.empty());
        FlutterWidgetPropertiesNode editedNode = new FlutterWidgetPropertiesNode(
                Children.LEAF, editedWidget, definition, commands::add);
        cellProperty(property(editedNode, "fallbackWidth")).restoreDefaultValue();

        assertEquals(List.of(
                new SetProperty(id, new PropertyName("color"), primary),
                new SetProperty(id, new PropertyName("strokeWidth"),
                        new PropertyValue.DoubleValue(new BigDecimal("2.5"))),
                new SetProperty(id, new PropertyName("fallbackWidth"),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(320))),
                new SetProperty(id, new PropertyName("fallbackHeight"),
                        new PropertyValue.DoubleValue(new BigDecimal("480.5"))),
                new ResetProperty(id, new PropertyName("fallbackWidth"))),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "outline and diagonals")),
                () -> assertTrue(child.getShortDescription().contains(
                        "corresponding incoming axis is unbounded")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void safeAreaProjectsExactGroupedOptionalSurfaceAndRequiredOccupiedChild()
            throws Exception {
        WidgetDefinition definition = definition(
                SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE.value());
        StableId id = StableId.parse("bd2223bb-177d-498c-bc24-dd9d78053988");
        WidgetNode child = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Text"),
                StableId.parse("4771f24f-ed69-459e-991b-e6bb587ef695"));
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + SafeAreaWidgetPropertySchema.Group.values().length,
                sets.length);
        assertEquals(SafeAreaWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                Arrays.stream(sets)
                        .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME.equals(
                                set.getName()))
                        .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(
                                set.getName()))
                        .mapToInt(set -> set.getProperties().length).sum());
        for (SafeAreaWidgetPropertySchema.Group group
                : SafeAreaWidgetPropertySchema.Group.values()) {
            Node.PropertySet set = propertySet(node, group.setName());
            assertEquals(group.displayName(), set.getDisplayName());
            assertEquals(group.description(), set.getShortDescription());
            assertEquals("General", set.getValue(
                    FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));
        }
        assertEquals(List.of("left", "top", "right", "bottom"), names(propertySet(
                node, SafeAreaWidgetPropertySchema.Group.SIDES.setName()).getProperties()));
        assertEquals(List.of("minimum"), names(propertySet(
                node, SafeAreaWidgetPropertySchema.Group.PADDING.setName()).getProperties()));
        assertEquals(List.of("maintainBottomViewPadding"), names(propertySet(
                node, SafeAreaWidgetPropertySchema.Group.VIEW_PADDING.setName())
                        .getProperties()));

        Node.Property<FlutterPropertyCellValue> left = cellProperty(property(node, "left"));
        assertEquals(FlutterPropertyCellValue.unset(), left.getValue());
        assertEquals(null, left.getPropertyEditor().getTags());
        left.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.BooleanValue(false)));
        assertTrue(left.supportsDefaultValue());
        left.restoreDefaultValue();

        Node.Property<FlutterPropertyCellValue> minimum = cellProperty(
                property(node, "minimum"));
        assertEquals(FlutterPropertyCellValue.unset(), minimum.getValue());
        assertThrows(IllegalArgumentException.class, () -> minimum.setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.EdgeInsetsDirectionalValue(
                                BigDecimal.ONE, BigDecimal.valueOf(2),
                                BigDecimal.valueOf(3), BigDecimal.valueOf(4)))));
        PropertyValue.EdgeInsetsValue physical = new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        minimum.setValue(FlutterPropertyCellValue.explicit(physical));

        assertEquals(List.of(
                new SetProperty(id, new PropertyName("left"),
                        new PropertyValue.BooleanValue(false)),
                new SetProperty(id, new PropertyName("minimum"), physical)),
                commands);

        Node.Property<?> childProperty = property(node, "child");
        assertAll(
                () -> assertEquals("Text", childProperty.getValue()),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "Required child inset")),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "physical EdgeInsets")),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "Occupancy: 1/1")),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "cannot be added empty, removed, or cleared")));
    }

    @Test
    void directionalityProjectsRequiredDirectionAndRequiredOccupiedChild()
            throws Exception {
        WidgetDefinition definition = definition(
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.value());
        StableId id = StableId.parse("ed973f31-6386-467a-b33c-504a5ba447d0");
        WidgetNode child = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Text"),
                StableId.parse("244b71a2-750a-4f66-8916-2a22e133cc01"));
        PropertyName textDirection = new PropertyName("textDirection");
        PropertyValue.EnumValue ltr =
                new PropertyValue.EnumValue("TextDirection", "ltr");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(textDirection, ltr),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + DirectionalityWidgetPropertySchema.Group.values().length,
                sets.length);
        Node.PropertySet direction = propertySet(
                node, DirectionalityWidgetPropertySchema.Group.DIRECTION.setName());
        assertAll(
                () -> assertEquals("Direction", direction.getDisplayName()),
                () -> assertEquals("Text flow direction inherited by this subtree.",
                        direction.getShortDescription()),
                () -> assertEquals("General", direction.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals(List.of("textDirection"),
                        names(direction.getProperties())));

        Node.Property<FlutterPropertyCellValue> property = cellProperty(
                property(node, "textDirection"));
        assertAll(
                () -> assertEquals("Text direction", property.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.explicit(ltr),
                        property.getValue()),
                () -> assertEquals(List.of("rtl", "ltr"),
                        List.of(property.getPropertyEditor().getTags())),
                () -> assertFalse(property.supportsDefaultValue()),
                () -> assertTrue(property.getShortDescription().contains(
                        "Required subtree direction")));
        PropertyValue.EnumValue rtl =
                new PropertyValue.EnumValue("TextDirection", "rtl");
        property.setValue(FlutterPropertyCellValue.explicit(rtl));
        assertEquals(List.of(new SetProperty(id, textDirection, rtl)), commands);

        Node.Property<?> childProperty = property(node, "child");
        assertAll(
                () -> assertEquals("Text", childProperty.getValue()),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "inherits this wrapper's explicit")),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "left-to-right or right-to-left")),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "Occupancy: 1/1")),
                () -> assertTrue(childProperty.getShortDescription().contains(
                        "cannot be added empty, removed, or cleared")));
    }

    @Test
    void decoratedBoxProjectsRequiredTypedDecorationPositionAndOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition(
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.value());
        StableId id = StableId.parse("2f67ac57-6010-42e5-8626-666a73b44aa2");
        PropertyName decorationName = new PropertyName("decoration");
        PropertyName positionName = new PropertyName("position");
        PropertyValue.BoxDecorationValue empty = emptyDecoration();
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(decorationName, empty),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + DecoratedBoxWidgetPropertySchema.Group.values().length,
                sets.length);
        Node.PropertySet decorationSet = propertySet(
                node, DecoratedBoxWidgetPropertySchema.Group.DECORATION.setName());
        assertAll(
                () -> assertEquals("Decoration", decorationSet.getDisplayName()),
                () -> assertEquals(
                        "Reviewed BoxDecoration and whether it paints behind or in front of the child.",
                        decorationSet.getShortDescription()),
                () -> assertEquals("General", decorationSet.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals(List.of("decoration", "position"),
                        names(decorationSet.getProperties())));

        Node.Property<FlutterPropertyCellValue> decoration = cellProperty(
                property(node, "decoration"));
        Node.Property<FlutterPropertyCellValue> position = cellProperty(
                property(node, "position"));
        assertAll(
                () -> assertEquals("Decoration", decoration.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.explicit(empty),
                        decoration.getValue()),
                () -> assertFalse(decoration.supportsDefaultValue()),
                () -> assertTrue(decoration.getShortDescription().contains(
                        "Required reviewed BoxDecoration")),
                () -> assertEquals("Position", position.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), position.getValue()),
                () -> assertEquals(List.of(
                        FlutterWidgetPropertiesNode.NOT_SET,
                        "background", "foreground"),
                        List.of(position.getPropertyEditor().getTags())),
                () -> assertTrue(position.supportsDefaultValue()),
                () -> assertTrue(position.getShortDescription().contains(
                        "behind or in front")));

        PropertyValue.BoxDecorationValue colored =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(new dev.flutter.netbeans.designer.model.ColorSource.Literal(
                                0xFF42A5F5L)),
                        Optional.empty(), Optional.empty(), List.of(),
                        Optional.empty(), Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        decoration.setValue(FlutterPropertyCellValue.explicit(colored));
        position.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.EnumValue("DecorationPosition", "foreground")));
        assertEquals(List.of(
                new SetProperty(id, decorationName, colored),
                new SetProperty(id, positionName,
                        new PropertyValue.EnumValue(
                                "DecorationPosition", "foreground"))),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains("Optional")),
                () -> assertTrue(child.getShortDescription().contains("Occupancy: 0/1")));
    }

    @Test
    void excludeSemanticsProjectsNullableCheckboxGroupAndOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition(
                ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE.value());
        StableId id = StableId.parse("72a8930b-26e9-41f2-a8a5-c100c3380c3a");
        PropertyName excludingName = new PropertyName("excluding");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(excludingName, new PropertyValue.BooleanValue(true)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + ExcludeSemanticsWidgetPropertySchema.Group.values().length,
                sets.length);
        Node.PropertySet semantics = propertySet(
                node, ExcludeSemanticsWidgetPropertySchema.Group.SEMANTICS.setName());
        assertAll(
                () -> assertEquals("Semantics", semantics.getDisplayName()),
                () -> assertEquals(
                        "Accessibility semantics exclusion for this subtree.",
                        semantics.getShortDescription()),
                () -> assertEquals("General", semantics.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals(List.of("excluding"),
                        names(semantics.getProperties())));

        Node.Property<FlutterPropertyCellValue> excluding = cellProperty(
                property(node, "excluding"));
        assertAll(
                () -> assertEquals("Excluding", excluding.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(true)),
                        excluding.getValue()),
                () -> assertNull(excluding.getPropertyEditor().getTags(),
                        "explicit booleans use the global checkbox editor"),
                () -> assertTrue(excluding.supportsDefaultValue()),
                () -> assertFalse(excluding.isDefaultValue()),
                () -> assertTrue(excluding.getShortDescription().contains(
                        "removed from the semantics tree")),
                () -> assertTrue(excluding.getShortDescription().contains(
                        "Omission preserves Flutter's true default")),
                () -> assertTrue(excluding.getShortDescription().contains(
                        "standard checkbox editor")));

        PropertyEditor editor = excluding.getPropertyEditor();
        editor.setAsText(FlutterWidgetPropertiesNode.NOT_SET);
        assertEquals(FlutterPropertyCellValue.unset(), cell(editor));
        editor.setAsText("false");
        FlutterPropertyCellValue explicitFalse = cell(editor);
        assertEquals(new PropertyValue.BooleanValue(false),
                explicitFalse.explicitValue().orElseThrow());
        excluding.setValue(explicitFalse);
        excluding.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, excludingName,
                        new PropertyValue.BooleanValue(false)),
                new ResetProperty(id, excludingName)), commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "descendant semantics")),
                () -> assertTrue(child.getShortDescription().contains(
                        "without changing layout, painting, or hit testing")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void clipRectProjectsTypedClipperClipBehaviorAndOptionalChild() throws Exception {
        WidgetDefinition definition = definition(
                ClipRectWidgetPropertySchema.CLIP_RECT_TYPE.value());
        StableId id = StableId.parse("bd4d317c-b6c1-43fb-83f5-1e35d5480c2f");
        PropertyName clipperName = new PropertyName("clipper");
        PropertyName clipBehaviorName = new PropertyName("clipBehavior");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "antiAlias")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + ClipRectWidgetPropertySchema.Group.values().length,
                sets.length);
        Node.PropertySet delegate = propertySet(
                node, ClipRectWidgetPropertySchema.Group.DELEGATE.setName());
        Node.PropertySet clipping = propertySet(
                node, ClipRectWidgetPropertySchema.Group.CLIPPING.setName());
        assertAll(
                () -> assertEquals("Delegate", delegate.getDisplayName()),
                () -> assertEquals("Project-declared rectangular clip delegate.",
                        delegate.getShortDescription()),
                () -> assertEquals(List.of("clipper"),
                        names(delegate.getProperties())),
                () -> assertEquals("Clipping", clipping.getDisplayName()),
                () -> assertEquals("Rectangular paint clipping behavior.",
                        clipping.getShortDescription()),
                () -> assertEquals("General", clipping.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", delegate.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals(List.of("clipBehavior"),
                        names(clipping.getProperties())));

        Node.Property<FlutterPropertyCellValue> clipper = cellProperty(
                property(node, "clipper"));
        PropertyEditor clipperEditor = clipper.getPropertyEditor();
        clipperEditor.setValue(clipper.getValue());
        assertAll(
                () -> assertEquals("Clipper", clipper.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), clipper.getValue()),
                () -> assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        clipperEditor.getAsText()),
                () -> assertTrue(clipperEditor.supportsCustomEditor()),
                () -> assertTrue(clipper.supportsDefaultValue()),
                () -> assertTrue(clipper.isDefaultValue()),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "Dart analyzer")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "CustomClipper<Rect>")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "preview-unavailable")));

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertAll(
                () -> assertEquals("Clip behavior", clipBehavior.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.EnumValue("Clip", "antiAlias")),
                        clipBehavior.getValue()),
                () -> assertEquals(List.of(
                        FlutterWidgetPropertiesNode.NOT_SET,
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                        List.of(clipBehavior.getPropertyEditor().getTags())),
                () -> assertTrue(clipBehavior.supportsDefaultValue()),
                () -> assertFalse(clipBehavior.isDefaultValue()),
                () -> assertTrue(clipBehavior.getShortDescription().contains(
                        "outside the child-bounds rectangle")),
                () -> assertTrue(clipBehavior.getShortDescription().contains(
                        "hard-edge default")));

        clipBehavior.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.EnumValue("Clip", "none")));
        clipBehavior.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "none")),
                new ResetProperty(id, clipBehaviorName)), commands);

        PropertyValue.DartObjectReferenceValue projectClipper =
                new PropertyValue.DartObjectReferenceValue(
                        java.util.Optional.empty(),
                        "_rectClipper",
                        java.util.Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        java.util.Optional.empty());
        clipper.setValue(FlutterPropertyCellValue.explicit(projectClipper));
        assertEquals(List.of(
                new SetProperty(id, clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "none")),
                new ResetProperty(id, clipBehaviorName),
                new SetProperty(id, clipperName, projectClipper)), commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "rectangular bounds")),
                () -> assertTrue(child.getShortDescription().contains(
                        "CustomClipper<Rect>")),
                () -> assertTrue(child.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(child.getShortDescription().contains(
                        "preview-unavailable")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void clipOvalProjectsTypedClipperClipBehaviorAndOptionalChild() throws Exception {
        WidgetDefinition definition = definition(
                ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE.value());
        StableId id = StableId.parse("74e2f52f-3dd9-46fe-a0ae-7f0ebf85bb95");
        PropertyName clipperName = new PropertyName("clipper");
        PropertyName clipBehaviorName = new PropertyName("clipBehavior");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + ClipOvalWidgetPropertySchema.Group.values().length,
                sets.length);
        Node.PropertySet delegate = propertySet(
                node, ClipOvalWidgetPropertySchema.Group.DELEGATE.setName());
        Node.PropertySet clipping = propertySet(
                node, ClipOvalWidgetPropertySchema.Group.CLIPPING.setName());
        assertAll(
                () -> assertEquals("Delegate", delegate.getDisplayName()),
                () -> assertEquals("Project-declared oval clip delegate.",
                        delegate.getShortDescription()),
                () -> assertEquals(List.of("clipper"),
                        names(delegate.getProperties())),
                () -> assertEquals("Clipping", clipping.getDisplayName()),
                () -> assertEquals("Oval paint clipping behavior.",
                        clipping.getShortDescription()),
                () -> assertEquals("General", clipping.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", delegate.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals(List.of("clipBehavior"),
                        names(clipping.getProperties())));

        Node.Property<FlutterPropertyCellValue> clipper = cellProperty(
                property(node, "clipper"));
        PropertyEditor clipperEditor = clipper.getPropertyEditor();
        clipperEditor.setValue(clipper.getValue());
        assertAll(
                () -> assertEquals("Clipper", clipper.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), clipper.getValue()),
                () -> assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        clipperEditor.getAsText()),
                () -> assertTrue(clipperEditor.supportsCustomEditor()),
                () -> assertTrue(clipper.supportsDefaultValue()),
                () -> assertTrue(clipper.isDefaultValue()),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "Dart analyzer")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "CustomClipper<Rect>")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "preview-unavailable")));

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertAll(
                () -> assertEquals("Clip behavior", clipBehavior.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.EnumValue("Clip", "hardEdge")),
                        clipBehavior.getValue()),
                () -> assertEquals(List.of(
                        FlutterWidgetPropertiesNode.NOT_SET,
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                        List.of(clipBehavior.getPropertyEditor().getTags())),
                () -> assertTrue(clipBehavior.supportsDefaultValue()),
                () -> assertFalse(clipBehavior.isDefaultValue()),
                () -> assertTrue(clipBehavior.getShortDescription().contains(
                        "outside the child-bounds oval")),
                () -> assertTrue(clipBehavior.getShortDescription().contains(
                        "anti-alias default")));

        clipBehavior.setValue(FlutterPropertyCellValue.explicit(
                new PropertyValue.EnumValue("Clip", "none")));
        clipBehavior.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "none")),
                new ResetProperty(id, clipBehaviorName)), commands);

        PropertyValue.DartObjectReferenceValue projectClipper =
                new PropertyValue.DartObjectReferenceValue(
                        java.util.Optional.empty(),
                        "_ovalClipper",
                        java.util.Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        java.util.Optional.empty());
        clipper.setValue(FlutterPropertyCellValue.explicit(projectClipper));
        assertEquals(List.of(
                new SetProperty(id, clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "none")),
                new ResetProperty(id, clipBehaviorName),
                new SetProperty(id, clipperName, projectClipper)), commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "oval inscribed")),
                () -> assertTrue(child.getShortDescription().contains(
                        "CustomClipper<Rect>")),
                () -> assertTrue(child.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(child.getShortDescription().contains(
                        "preview-unavailable")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void clipRRectProjectsTypedRadiusClipperClipBehaviorAndOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition(
                ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE.value());
        StableId id = StableId.parse("f93b867a-a162-47c4-841f-bd22936629aa");
        PropertyName borderRadiusName = new PropertyName("borderRadius");
        PropertyName clipperName = new PropertyName("clipper");
        PropertyName clipBehaviorName = new PropertyName("clipBehavior");
        PropertyValue.BoxDecorationValue.Radius twelve =
                new PropertyValue.BoxDecorationValue.Radius(
                        BigDecimal.valueOf(12), BigDecimal.valueOf(12));
        PropertyValue.BorderRadiusValue initialRadius =
                new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                twelve, twelve, twelve, twelve));
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        borderRadiusName, initialRadius,
                        clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        assertEquals(2 + ClipRRectWidgetPropertySchema.Group.values().length,
                node.getPropertySets().length);
        Node.PropertySet geometry = propertySet(
                node, ClipRRectWidgetPropertySchema.Group.GEOMETRY.setName());
        Node.PropertySet delegate = propertySet(
                node, ClipRRectWidgetPropertySchema.Group.DELEGATE.setName());
        Node.PropertySet clipping = propertySet(
                node, ClipRRectWidgetPropertySchema.Group.CLIPPING.setName());
        assertAll(
                () -> assertEquals("Geometry", geometry.getDisplayName()),
                () -> assertEquals("Rounded-rectangle clipping geometry.",
                        geometry.getShortDescription()),
                () -> assertEquals(List.of("borderRadius"),
                        names(geometry.getProperties())),
                () -> assertEquals("Delegate", delegate.getDisplayName()),
                () -> assertEquals("Project-declared rounded-rectangle clip delegate.",
                        delegate.getShortDescription()),
                () -> assertEquals(List.of("clipper"),
                        names(delegate.getProperties())),
                () -> assertEquals("Clipping", clipping.getDisplayName()),
                () -> assertEquals(List.of("clipBehavior"),
                        names(clipping.getProperties())),
                () -> assertEquals("General", geometry.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", delegate.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", clipping.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)));

        Node.Property<FlutterPropertyCellValue> borderRadius = cellProperty(
                property(node, "borderRadius"));
        PropertyEditor radiusEditor = borderRadius.getPropertyEditor();
        radiusEditor.setValue(borderRadius.getValue());
        assertAll(
                () -> assertEquals("Border radius", borderRadius.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.explicit(initialRadius),
                        borderRadius.getValue()),
                () -> assertEquals("physical circular 12", radiusEditor.getAsText()),
                () -> assertTrue(radiusEditor.supportsCustomEditor()),
                () -> assertTrue(borderRadius.supportsDefaultValue()),
                () -> assertFalse(borderRadius.isDefaultValue()),
                () -> assertTrue(borderRadius.getShortDescription().contains(
                        "directional")),
                () -> assertTrue(borderRadius.getShortDescription().contains(
                        "BorderRadius.zero")),
                () -> assertTrue(borderRadius.getShortDescription().contains(
                        "ignores borderRadius")));

        PropertyValue.BoxDecorationValue.Radius one =
                new PropertyValue.BoxDecorationValue.Radius(
                        BigDecimal.ONE, BigDecimal.ONE);
        PropertyValue.BorderRadiusValue directional =
                new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                one, one, one, one));
        borderRadius.setValue(FlutterPropertyCellValue.explicit(directional));
        borderRadius.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, borderRadiusName, directional),
                new ResetProperty(id, borderRadiusName)), commands);

        Node.Property<FlutterPropertyCellValue> clipper = cellProperty(
                property(node, "clipper"));
        PropertyEditor clipperEditor = clipper.getPropertyEditor();
        clipperEditor.setValue(clipper.getValue());
        assertAll(
                () -> assertEquals("Clipper", clipper.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(),
                        clipper.getValue()),
                () -> assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        clipperEditor.getAsText()),
                () -> assertTrue(clipperEditor.supportsCustomEditor()),
                () -> assertTrue(clipper.supportsDefaultValue()),
                () -> assertTrue(clipper.isDefaultValue()),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "Dart analyzer")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "CustomClipper<RRect>")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "ignores borderRadius")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "preview-unavailable")));
        PropertyValue.DartObjectReferenceValue projectClipper =
                new PropertyValue.DartObjectReferenceValue(
                        java.util.Optional.empty(),
                        "_projectClipper",
                        java.util.Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        java.util.Optional.empty());
        clipper.setValue(FlutterPropertyCellValue.explicit(projectClipper));
        assertEquals(List.of(
                new SetProperty(id, borderRadiusName, directional),
                new ResetProperty(id, borderRadiusName),
                new SetProperty(id, clipperName, projectClipper)), commands);

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertAll(
                () -> assertEquals(List.of(
                        FlutterWidgetPropertiesNode.NOT_SET,
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                        List.of(clipBehavior.getPropertyEditor().getTags())),
                () -> assertTrue(clipBehavior.getShortDescription().contains(
                        "anti-alias default")));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "rounded rectangular bounds")),
                () -> assertTrue(child.getShortDescription().contains(
                        "CustomClipper")),
                () -> assertTrue(child.getShortDescription().contains(
                        "ignores borderRadius")),
                () -> assertTrue(child.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(child.getShortDescription().contains(
                        "preview-unavailable")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void clipRSuperellipseProjectsTypedRadiusClipperClipBehaviorAndOptionalChild()
            throws Exception {
        WidgetDefinition definition = definition(
                ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE.value());
        StableId id = StableId.parse("f93b867a-a162-47c4-841f-bd22936629aa");
        PropertyName borderRadiusName = new PropertyName("borderRadius");
        PropertyName clipperName = new PropertyName("clipper");
        PropertyName clipBehaviorName = new PropertyName("clipBehavior");
        PropertyValue.BoxDecorationValue.Radius twelve =
                new PropertyValue.BoxDecorationValue.Radius(
                        BigDecimal.valueOf(12), BigDecimal.valueOf(12));
        PropertyValue.BorderRadiusValue initialRadius =
                new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                twelve, twelve, twelve, twelve));
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        borderRadiusName, initialRadius,
                        clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        assertEquals(2 + ClipRSuperellipseWidgetPropertySchema.Group.values().length,
                node.getPropertySets().length);
        Node.PropertySet geometry = propertySet(
                node, ClipRSuperellipseWidgetPropertySchema.Group.GEOMETRY.setName());
        Node.PropertySet delegate = propertySet(
                node, ClipRSuperellipseWidgetPropertySchema.Group.DELEGATE.setName());
        Node.PropertySet clipping = propertySet(
                node, ClipRSuperellipseWidgetPropertySchema.Group.CLIPPING.setName());
        assertAll(
                () -> assertEquals("Geometry", geometry.getDisplayName()),
                () -> assertEquals("Rounded-superellipse clipping geometry.",
                        geometry.getShortDescription()),
                () -> assertEquals(List.of("borderRadius"),
                        names(geometry.getProperties())),
                () -> assertEquals("Delegate", delegate.getDisplayName()),
                () -> assertEquals("Project-declared rounded-superellipse clip delegate.",
                        delegate.getShortDescription()),
                () -> assertEquals(List.of("clipper"),
                        names(delegate.getProperties())),
                () -> assertEquals("Clipping", clipping.getDisplayName()),
                () -> assertEquals(List.of("clipBehavior"),
                        names(clipping.getProperties())),
                () -> assertEquals("General", geometry.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", delegate.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", clipping.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)));

        Node.Property<FlutterPropertyCellValue> borderRadius = cellProperty(
                property(node, "borderRadius"));
        PropertyEditor radiusEditor = borderRadius.getPropertyEditor();
        radiusEditor.setValue(borderRadius.getValue());
        assertAll(
                () -> assertEquals("Border radius", borderRadius.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.explicit(initialRadius),
                        borderRadius.getValue()),
                () -> assertEquals("physical circular 12", radiusEditor.getAsText()),
                () -> assertTrue(radiusEditor.supportsCustomEditor()),
                () -> assertTrue(borderRadius.supportsDefaultValue()),
                () -> assertFalse(borderRadius.isDefaultValue()),
                () -> assertTrue(borderRadius.getShortDescription().contains(
                        "directional")),
                () -> assertTrue(borderRadius.getShortDescription().contains(
                        "BorderRadius.zero")),
                () -> assertTrue(borderRadius.getShortDescription().contains(
                        "ignores borderRadius")));

        PropertyValue.BoxDecorationValue.Radius one =
                new PropertyValue.BoxDecorationValue.Radius(
                        BigDecimal.ONE, BigDecimal.ONE);
        PropertyValue.BorderRadiusValue directional =
                new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                one, one, one, one));
        borderRadius.setValue(FlutterPropertyCellValue.explicit(directional));
        borderRadius.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, borderRadiusName, directional),
                new ResetProperty(id, borderRadiusName)), commands);

        Node.Property<FlutterPropertyCellValue> clipper = cellProperty(
                property(node, "clipper"));
        PropertyEditor clipperEditor = clipper.getPropertyEditor();
        clipperEditor.setValue(clipper.getValue());
        assertAll(
                () -> assertEquals("Clipper", clipper.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(),
                        clipper.getValue()),
                () -> assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        clipperEditor.getAsText()),
                () -> assertTrue(clipperEditor.supportsCustomEditor()),
                () -> assertTrue(clipper.supportsDefaultValue()),
                () -> assertTrue(clipper.isDefaultValue()),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "Dart analyzer")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "CustomClipper<RSuperellipse>")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "ignores borderRadius")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "preview-unavailable")));
        PropertyValue.DartObjectReferenceValue projectClipper =
                new PropertyValue.DartObjectReferenceValue(
                        java.util.Optional.empty(),
                        "_projectClipper",
                        java.util.Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        java.util.Optional.empty());
        clipper.setValue(FlutterPropertyCellValue.explicit(projectClipper));
        assertEquals(List.of(
                new SetProperty(id, borderRadiusName, directional),
                new ResetProperty(id, borderRadiusName),
                new SetProperty(id, clipperName, projectClipper)), commands);

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertAll(
                () -> assertEquals(List.of(
                        FlutterWidgetPropertiesNode.NOT_SET,
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                        List.of(clipBehavior.getPropertyEditor().getTags())),
                () -> assertTrue(clipBehavior.getShortDescription().contains(
                        "anti-alias default")));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "rounded superellipse bounds")),
                () -> assertTrue(child.getShortDescription().contains(
                        "CustomClipper<RSuperellipse>")),
                () -> assertTrue(child.getShortDescription().contains(
                        "ignores borderRadius")),
                () -> assertTrue(child.getShortDescription().contains(
                        "isolated Canvas")),
                () -> assertTrue(child.getShortDescription().contains(
                        "preview-unavailable")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void clipPathProjectsTypedExclusiveBranchesAndRefreshesAtomically()
            throws Exception {
        WidgetDefinition definition = definition(
                ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.value());
        StableId id = StableId.parse("a1936968-aae5-4d90-8a9a-8355a735d2f1");
        PropertyName clipperName = new PropertyName("clipper");
        PropertyName shapeName = new PropertyName("shape");
        PropertyName clipBehaviorName = new PropertyName("clipBehavior");
        PropertyValue.DartObjectReferenceValue shapeReference =
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of("package:app/shapes.dart"),
                        "TicketShape",
                        Optional.of("compact"),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true));
        PropertyValue.DartObjectReferenceValue clipperReference =
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(),
                        "_ticketClipper",
                        Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty());
        Map<SlotName, WidgetSlot> slots = Map.of(
                new SlotName("child"), WidgetSlot.SingleSlot.empty());
        WidgetNode shaped = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        shapeName, shapeReference,
                        clipBehaviorName,
                        new PropertyValue.EnumValue("Clip", "hardEdge")),
                slots,
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, shaped, definition, commands::add);

        assertEquals(2 + ClipPathWidgetPropertySchema.Group.values().length,
                node.getPropertySets().length);
        Node.PropertySet delegate = propertySet(
                node, ClipPathWidgetPropertySchema.Group.DELEGATE.setName());
        Node.PropertySet shape = propertySet(
                node, ClipPathWidgetPropertySchema.Group.SHAPE.setName());
        Node.PropertySet clipping = propertySet(
                node, ClipPathWidgetPropertySchema.Group.CLIPPING.setName());
        assertAll(
                () -> assertEquals("Delegate", delegate.getDisplayName()),
                () -> assertEquals(List.of("clipper"), names(delegate.getProperties())),
                () -> assertEquals("Shape", shape.getDisplayName()),
                () -> assertEquals(List.of("shape"), names(shape.getProperties())),
                () -> assertEquals("Clipping", clipping.getDisplayName()),
                () -> assertEquals(List.of("clipBehavior"),
                        names(clipping.getProperties())),
                () -> assertEquals("General", delegate.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals("General", shape.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)));

        Node.Property<FlutterPropertyCellValue> clipper = cellProperty(
                property(node, "clipper"));
        Node.Property<FlutterPropertyCellValue> shapeProperty = cellProperty(
                property(node, "shape"));
        assertAll(
                () -> assertEquals(FlutterPropertyCellValue.unset(), clipper.getValue()),
                () -> assertEquals(FlutterPropertyCellValue.explicit(shapeReference),
                        shapeProperty.getValue()),
                () -> assertTrue(clipper.getPropertyEditor().supportsCustomEditor()),
                () -> assertTrue(shapeProperty.getPropertyEditor().supportsCustomEditor()),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "CustomClipper<Path>")),
                () -> assertTrue(clipper.getShortDescription().contains(
                        "clears the mutually exclusive Shape")),
                () -> assertTrue(shapeProperty.getShortDescription().contains(
                        "ShapeBorder")),
                () -> assertTrue(shapeProperty.getShortDescription().contains(
                        "non-const ClipPath.shape")));

        shapeProperty.restoreDefaultValue();
        clipper.setValue(FlutterPropertyCellValue.explicit(clipperReference));
        assertEquals(List.of(
                new ResetProperty(id, shapeName),
                new PatchProperties(id, List.of(
                        new PatchProperties.ResetPatch(shapeName),
                        new PatchProperties.SetPatch(clipperName, clipperReference)))),
                commands);

        WidgetNode clipped = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(clipperName, clipperReference),
                slots,
                Extensions.empty());
        List<DesignerCommand> refreshedCommands = new ArrayList<>();
        Node.Property<?> stableClipper = clipper;
        Node.Property<?> stableShape = shapeProperty;
        node.refreshPresentation(
                clipped,
                definition,
                refreshedCommands::add,
                null,
                null,
                FlutterImageAssetChoices.empty());
        assertAll(
                () -> assertSame(stableClipper, property(node, "clipper")),
                () -> assertSame(stableShape, property(node, "shape")),
                () -> assertEquals(FlutterPropertyCellValue.explicit(clipperReference),
                        clipper.getValue()),
                () -> assertEquals(FlutterPropertyCellValue.unset(),
                        shapeProperty.getValue()));

        shapeProperty.setValue(FlutterPropertyCellValue.explicit(shapeReference));
        assertEquals(List.of(new PatchProperties(id, List.of(
                new PatchProperties.ResetPatch(clipperName),
                new PatchProperties.SetPatch(shapeName, shapeReference)))),
                refreshedCommands);

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertEquals(List.of(
                FlutterWidgetPropertiesNode.NOT_SET,
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                List.of(clipBehavior.getPropertyEditor().getTags()));
        assertTrue(clipBehavior.getShortDescription().contains("anti-alias default"));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "CustomClipper<Path>")),
                () -> assertTrue(child.getShortDescription().contains("ShapeBorder")),
                () -> assertTrue(child.getShortDescription().contains(
                        "mutually exclusive")),
                () -> assertTrue(child.getShortDescription().contains(
                        "preview-unavailable")),
                () -> assertTrue(child.getShortDescription().contains(
                        "Occupancy: 0/1")));
    }

    @Test
    void indexedStackProjectsFivePropertiesHonestNullableIndexAndChildren()
            throws Exception {
        WidgetDefinition definition = definition(
                IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.value());
        StableId id = StableId.parse("2bd43403-31cc-4b8d-a74c-0bcdf95a7565");
        PropertyName indexName = new PropertyName("index");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(indexName, new PropertyValue.NullValue()),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(2 + IndexedStackWidgetPropertySchema.Group.values().length,
                sets.length);
        Node.PropertySet layout = propertySet(
                node, IndexedStackWidgetPropertySchema.Group.LAYOUT.setName());
        assertAll(
                () -> assertEquals("Layout", layout.getDisplayName()),
                () -> assertEquals(
                        "Alignment, direction, sizing, clipping, and the visible child index.",
                        layout.getShortDescription()),
                () -> assertEquals("General", layout.getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE)),
                () -> assertEquals(List.of(
                        "alignment", "textDirection", "clipBehavior", "sizing", "index"),
                        names(layout.getProperties())));

        Node.Property<FlutterPropertyCellValue> index = cellProperty(
                property(node, "index"));
        PropertyEditor editor = index.getPropertyEditor();
        editor.setValue(index.getValue());
        assertAll(
                () -> assertEquals("Index", index.getDisplayName()),
                () -> assertEquals(
                        FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()),
                        index.getValue()),
                () -> assertEquals("null", editor.getAsText()),
                () -> assertTrue(index.supportsDefaultValue()),
                () -> assertTrue(editor.supportsCustomEditor()),
                () -> assertTrue(index.getShortDescription().contains(
                        "<not set> omits the argument")),
                () -> assertTrue(index.getShortDescription().contains(
                        "explicit null")),
                () -> assertTrue(index.getShortDescription().contains(
                        "below their count")),
                () -> assertTrue(index.getShortDescription().contains(
                        "Never use -1 as a sentinel")));

        editor.setAsText(FlutterWidgetPropertiesNode.NOT_SET);
        assertEquals(FlutterPropertyCellValue.unset(), cell(editor));
        editor.setAsText("null");
        assertEquals(new PropertyValue.NullValue(),
                cell(editor).explicitValue().orElseThrow());
        editor.setAsText("2");
        FlutterPropertyCellValue explicitTwo = cell(editor);
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                explicitTwo.explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText("-1"));

        index.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        index.setValue(explicitTwo);
        index.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, indexName,
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                new ResetProperty(id, indexName)), commands);

        Node.Property<?> children = property(node, "children");
        assertAll(
                () -> assertEquals("Children", children.getDisplayName()),
                () -> assertEquals("Empty", children.getValue()),
                () -> assertTrue(children.getShortDescription().contains(
                        "all remain laid out and keep their state")),
                () -> assertTrue(children.getShortDescription().contains(
                        "painted, hit-tested, and exposed through Flutter semantics")),
                () -> assertTrue(children.getShortDescription().contains(
                        "explicit null selects none")),
                () -> assertTrue(children.getShortDescription().contains(
                        "Occupancy: 0/")));
    }

    @Test
    void alignProjectsDirectionalAlignmentOptionalFactorsAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Align");
        StableId id = StableId.parse("e6cb4627-6ef8-49bb-a5d8-452746dc604c");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("alignment", "widthFactor", "heightFactor"),
                names(properties.getProperties()));
        assertAll(
                () -> assertTrue(properties.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(properties.getShortDescription().contains("not from the theme")));

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("2.5"),
                        new BigDecimal("-3"));
        assertAll(
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertTrue(alignment.getShortDescription().contains("defaults to center")),
                () -> assertTrue(alignment.getShortDescription().contains("outside -1 through 1")),
                () -> assertTrue(alignment.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(alignment.supportsDefaultValue()));
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        assertEquals(List.of(new SetProperty(
                id, new PropertyName("alignment"), directional)), commands);

        Node.Property<FlutterPropertyCellValue> width = cellProperty(
                property(node, "widthFactor"));
        Node.Property<FlutterPropertyCellValue> height = cellProperty(
                property(node, "heightFactor"));
        assertAll(
                () -> assertTrue(width.getShortDescription().contains("Zero is valid")),
                () -> assertTrue(width.getShortDescription().contains("bounded")),
                () -> assertTrue(width.getShortDescription().contains("unbounded")),
                () -> assertTrue(height.getShortDescription().contains("Zero is valid")),
                () -> assertTrue(width.supportsDefaultValue()),
                () -> assertTrue(height.supportsDefaultValue()));
        PropertyEditor widthEditor = width.getPropertyEditor();
        widthEditor.setAsText("0");
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                cell(widthEditor).explicitValue().orElseThrow());
        widthEditor.setAsText("2.5");
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("2.5")),
                cell(widthEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("-0.1"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("NaN"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("Infinity"));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(child.getShortDescription().contains("not from the theme")),
                () -> assertTrue(child.getShortDescription().contains("Occupancy: 0/1")));
    }

    @Test
    void fractionallySizedBoxProjectsIncomingFractionsAlignmentAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.FractionallySizedBox");
        StableId id = StableId.parse("7d783649-ab39-4daa-8ba3-29a7fbc7d0a0");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("alignment", "widthFactor", "heightFactor"),
                names(properties.getProperties()));
        assertAll(
                () -> assertTrue(properties.getShortDescription()
                        .contains("Incoming-size fractions")),
                () -> assertTrue(properties.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(properties.getShortDescription()
                        .contains("not from the theme")));

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("1.5"),
                        new BigDecimal("-2"));
        assertAll(
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("defaults to center")),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("outside -1 through 1")),
                () -> assertTrue(alignment.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(alignment.supportsDefaultValue()));
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        assertEquals(List.of(new SetProperty(
                id, new PropertyName("alignment"), directional)), commands);

        Node.Property<FlutterPropertyCellValue> width = cellProperty(
                property(node, "widthFactor"));
        Node.Property<FlutterPropertyCellValue> height = cellProperty(
                property(node, "heightFactor"));
        assertAll(
                () -> assertTrue(width.getShortDescription()
                        .contains("bounded incoming maximum width")),
                () -> assertTrue(width.getShortDescription()
                        .contains("tight child width")),
                () -> assertTrue(width.getShortDescription()
                        .contains("Zero and values above one are valid")),
                () -> assertTrue(width.getShortDescription()
                        .contains("unbounded")),
                () -> assertTrue(height.getShortDescription()
                        .contains("bounded incoming maximum height")),
                () -> assertTrue(height.getShortDescription()
                        .contains("tight child height")),
                () -> assertTrue(width.supportsDefaultValue()),
                () -> assertTrue(height.supportsDefaultValue()));
        PropertyEditor widthEditor = width.getPropertyEditor();
        widthEditor.setAsText("0");
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                cell(widthEditor).explicitValue().orElseThrow());
        widthEditor.setAsText("1.25");
        PropertyValue.DoubleValue explicitWidth = new PropertyValue.DoubleValue(
                new BigDecimal("1.25"));
        assertEquals(explicitWidth, cell(widthEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("-0.1"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("NaN"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("Infinity"));

        width.setValue(FlutterPropertyCellValue.explicit(explicitWidth));
        width.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("alignment"), directional),
                new SetProperty(id, new PropertyName("widthFactor"), explicitWidth)),
                commands,
                "the immutable prototype snapshot is already at the width default");

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("tight fractions")),
                () -> assertTrue(child.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(child.getShortDescription()
                        .contains("not from the theme")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void fittedBoxProjectsExactFitAlignmentClippingAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.FittedBox");
        StableId id = StableId.parse("b587a092-9a65-420a-9d1c-e127cc752f8d");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("fit", "alignment", "clipBehavior"),
                names(properties.getProperties()));
        assertEquals(
                "Scaling discipline, positioning, clipping, and optional child "
                + "for the selected FittedBox widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> fit = cellProperty(
                property(node, "fit"));
        assertEquals("Fit", fit.getDisplayName());
        assertEquals(List.of(
                        "<not set>", "fill", "contain", "cover", "fitWidth",
                        "fitHeight", "none", "scaleDown"),
                List.of(fit.getPropertyEditor().getTags()));
        assertAll(
                () -> assertTrue(fit.getShortDescription().contains("Contain is the default")),
                () -> assertTrue(fit.getShortDescription().contains("scaleDown")),
                () -> assertTrue(fit.supportsDefaultValue()));
        PropertyEditor fitEditor = fit.getPropertyEditor();
        fitEditor.setAsText("cover");
        assertEquals(new PropertyValue.EnumValue("BoxFit", "cover"),
                cell(fitEditor).explicitValue().orElseThrow());

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("1.5"),
                        new BigDecimal("-2"));
        assertAll(
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("defaults to center")),
                () -> assertTrue(alignment.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("not from the theme")),
                () -> assertTrue(alignment.supportsDefaultValue()));
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        assertEquals(List.of(new SetProperty(
                id, new PropertyName("alignment"), directional)), commands);

        Node.Property<FlutterPropertyCellValue> clip = cellProperty(
                property(node, "clipBehavior"));
        assertEquals("Clip behavior", clip.getDisplayName());
        assertEquals(List.of(
                        "<not set>", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer"),
                List.of(clip.getPropertyEditor().getTags()));
        assertTrue(clip.getShortDescription().contains("defaults to none"));
        assertTrue(clip.getShortDescription().contains("visual overflow"));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("laid out unconstrained")),
                () -> assertTrue(child.getShortDescription()
                        .contains("without changing the child's own layout size")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void constrainedBoxProjectsRequiredConstraintsAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.ConstrainedBox");
        StableId id = StableId.parse("8362400e-cf39-4f3c-b024-277e45cfb8f6");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("constraints"), names(properties.getProperties()));
        assertEquals(
                "Required normalized width and height constraints, including independently "
                + "expanding axes, and an optional child for the selected "
                + "ConstrainedBox widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> constraints = cellProperty(
                property(node, "constraints"));
        PropertyValue.BoxConstraintsValue neutral = new PropertyValue.BoxConstraintsValue(
                BigDecimal.ZERO, Optional.empty(), BigDecimal.ZERO, Optional.empty());
        assertAll(
                () -> assertEquals("Constraints", constraints.getDisplayName()),
                () -> assertEquals(neutral,
                        constraints.getValue().explicitValue().orElseThrow()),
                () -> assertTrue(constraints.canWrite()),
                () -> assertFalse(constraints.supportsDefaultValue()),
                () -> assertFalse(constraints.isDefaultValue()),
                () -> assertTrue(constraints.getPropertyEditor().supportsCustomEditor()),
                () -> assertTrue(constraints.getShortDescription()
                        .contains("finite maximum not below its minimum")),
                () -> assertTrue(constraints.getShortDescription().contains("∞…∞")),
                () -> assertTrue(constraints.getShortDescription()
                        .contains("required constructor argument cannot be unset")));

        constraints.restoreDefaultValue();
        assertTrue(commands.isEmpty(), "required constraints cannot expose a reset command");
        assertThrows(IllegalArgumentException.class,
                () -> constraints.setValue(FlutterPropertyCellValue.unset()));
        assertTrue(commands.isEmpty(), "rejected unset must not submit a command");

        PropertyValue.BoxConstraintsValue expandingWidth =
                new PropertyValue.BoxConstraintsValue(
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        new PropertyValue.BoxConstraintBound.Finite(BigDecimal.ZERO),
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE);
        constraints.setValue(FlutterPropertyCellValue.explicit(expandingWidth));
        assertEquals(List.of(new SetProperty(
                id, new PropertyName("constraints"), expandingWidth)), commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("additional normalized BoxConstraints")),
                () -> assertTrue(child.getShortDescription()
                        .contains("bounded incoming maximum")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void unconstrainedBoxProjectsDirectionAlignmentAxisClippingAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.UnconstrainedBox");
        StableId id = StableId.parse("cf4a5c37-32df-47e2-b19c-7b92e7762cb9");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                "textDirection", "alignment", "constrainedAxis", "clipBehavior"),
                names(properties.getProperties()));
        assertEquals(
                "Constraint removal, optional retained axis, positioning, clipping, "
                + "direction, and optional child for the selected UnconstrainedBox widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> textDirection = cellProperty(
                property(node, "textDirection"));
        assertAll(
                () -> assertEquals("Text direction", textDirection.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(),
                        textDirection.getValue()),
                () -> assertEquals(List.of("<not set>", "rtl", "ltr"),
                        List.of(textDirection.getPropertyEditor().getTags())),
                () -> assertTrue(textDirection.getShortDescription()
                        .contains("ambient Directionality")),
                () -> assertTrue(textDirection.getShortDescription()
                        .contains("not read from the theme")),
                () -> assertTrue(textDirection.supportsDefaultValue()),
                () -> assertTrue(textDirection.isDefaultValue()));

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("1.25"), new BigDecimal("-0.5"));
        assertAll(
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), alignment.getValue()),
                () -> assertTrue(alignment.getPropertyEditor().supportsCustomEditor()),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("defaults to center")),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("ambient Directionality")),
                () -> assertTrue(alignment.supportsDefaultValue()));

        Node.Property<FlutterPropertyCellValue> constrainedAxis = cellProperty(
                property(node, "constrainedAxis"));
        assertAll(
                () -> assertEquals("Constrained axis", constrainedAxis.getDisplayName()),
                () -> assertEquals(List.of("<not set>", "horizontal", "vertical"),
                        List.of(constrainedAxis.getPropertyEditor().getTags())),
                () -> assertTrue(constrainedAxis.getShortDescription()
                        .contains("removes constraints from both axes")),
                () -> assertTrue(constrainedAxis.supportsDefaultValue()));

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertAll(
                () -> assertEquals("Clip behavior", clipBehavior.getDisplayName()),
                () -> assertEquals(List.of(
                                "<not set>", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer"),
                        List.of(clipBehavior.getPropertyEditor().getTags())),
                () -> assertTrue(clipBehavior.getShortDescription()
                        .contains("defaults to none")),
                () -> assertTrue(clipBehavior.supportsDefaultValue()));

        PropertyValue.EnumValue rtl =
                new PropertyValue.EnumValue("TextDirection", "rtl");
        PropertyValue.EnumValue vertical =
                new PropertyValue.EnumValue("Axis", "vertical");
        PropertyValue.EnumValue antiAlias =
                new PropertyValue.EnumValue("Clip", "antiAlias");
        textDirection.setValue(FlutterPropertyCellValue.explicit(rtl));
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        constrainedAxis.setValue(FlutterPropertyCellValue.explicit(vertical));
        clipBehavior.setValue(FlutterPropertyCellValue.explicit(antiAlias));
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("textDirection"), rtl),
                new SetProperty(id, new PropertyName("alignment"), directional),
                new SetProperty(id, new PropertyName("constrainedAxis"), vertical),
                new SetProperty(id, new PropertyName("clipBehavior"), antiAlias)),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("without incoming constraints on both axes")),
                () -> assertTrue(child.getShortDescription()
                        .contains("selected constrained axis retained")),
                () -> assertTrue(child.getShortDescription()
                        .contains("paint overflow follows clip behavior")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void limitedBoxProjectsOptionalTypedFallbackLimitsAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.LimitedBox");
        StableId id = StableId.parse("ced82319-7a4e-4dc6-ab99-55193af55f37");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, id);
        PropertyValue.DoubleValue initialWidth =
                new PropertyValue.DoubleValue(new BigDecimal("120"));
        PropertyValue.DoubleValue initialHeight =
                new PropertyValue.DoubleValue(new BigDecimal("240.5"));
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(
                        new PropertyName("maxWidth"), initialWidth,
                        new PropertyName("maxHeight"), initialHeight),
                prototype.slots(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("maxWidth", "maxHeight"), names(properties.getProperties()));
        assertEquals(
                "Fallback maximum width and height for unbounded incoming axes, and an "
                + "optional child for the selected LimitedBox widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> maxWidth = cellProperty(
                property(node, "maxWidth"));
        Node.Property<FlutterPropertyCellValue> maxHeight = cellProperty(
                property(node, "maxHeight"));
        assertAll(
                () -> assertEquals("Max Width", maxWidth.getDisplayName()),
                () -> assertEquals("Max Height", maxHeight.getDisplayName()),
                () -> assertEquals(initialWidth,
                        maxWidth.getValue().explicitValue().orElseThrow()),
                () -> assertEquals(initialHeight,
                        maxHeight.getValue().explicitValue().orElseThrow()),
                () -> assertTrue(maxWidth.getShortDescription().contains("incoming width is unbounded")),
                () -> assertTrue(maxHeight.getShortDescription().contains("incoming height is unbounded")),
                () -> assertTrue(maxWidth.getShortDescription().contains("default positive infinity")),
                () -> assertTrue(maxHeight.getShortDescription().contains("zero is valid")),
                () -> assertTrue(maxWidth.supportsDefaultValue()),
                () -> assertTrue(maxHeight.supportsDefaultValue()),
                () -> assertFalse(maxWidth.isDefaultValue()),
                () -> assertFalse(maxHeight.isDefaultValue()));

        PropertyEditor widthEditor = maxWidth.getPropertyEditor();
        widthEditor.setAsText("0");
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ZERO),
                cell(widthEditor).explicitValue().orElseThrow());
        PropertyEditor heightEditor = maxHeight.getPropertyEditor();
        heightEditor.setAsText("96.5");
        PropertyValue.DoubleValue updatedWidth =
                new PropertyValue.DoubleValue(new BigDecimal("96.5"));
        assertEquals(updatedWidth, cell(heightEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("-0.1"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("NaN"));
        assertThrows(IllegalArgumentException.class,
                () -> widthEditor.setAsText("Infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> maxWidth.setValue(FlutterPropertyCellValue.explicit(
                        new PropertyValue.IntegerValue(BigInteger.ZERO))));
        assertTrue(commands.isEmpty(), "editor validation must not partially submit a command");

        maxWidth.setValue(FlutterPropertyCellValue.explicit(updatedWidth));
        maxHeight.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("maxWidth"), updatedWidth),
                new ResetProperty(id, new PropertyName("maxHeight"))),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("corresponding incoming axis is unbounded")),
                () -> assertTrue(child.getShortDescription()
                        .contains("bounded incoming constraints pass through unchanged")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void overflowBoxProjectsConstraintOverridesAlignmentFitAndControllableChild()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.OverflowBox");
        StableId id = StableId.parse("bf0274b6-a3e4-49a8-86d2-2fcfa319462d");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                "alignment", "minWidth", "maxWidth", "minHeight", "maxHeight", "fit"),
                names(properties.getProperties()));
        assertAll(
                () -> assertTrue(properties.getShortDescription()
                        .contains("Constraint overrides")),
                () -> assertTrue(properties.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(properties.getShortDescription()
                        .contains("not from the theme")));

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        PropertyValue.AlignmentGeometryValue directional =
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        new BigDecimal("1.25"), new BigDecimal("-0.5"));
        assertAll(
                () -> assertEquals("Alignment", alignment.getDisplayName()),
                () -> assertTrue(alignment.getPropertyEditor().supportsCustomEditor()),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("defaults to center")),
                () -> assertTrue(alignment.getShortDescription().contains("LTR/RTL")),
                () -> assertTrue(alignment.getShortDescription()
                        .contains("outside -1 through 1")),
                () -> assertTrue(alignment.supportsDefaultValue()),
                () -> assertTrue(alignment.isDefaultValue()));

        List<String> numericNames = List.of(
                "minWidth", "maxWidth", "minHeight", "maxHeight");
        List<String> numericLabels = List.of(
                "Min Width", "Max Width", "Min Height", "Max Height");
        List<PropertyValue.DoubleValue> values = List.of(
                new PropertyValue.DoubleValue(BigDecimal.ZERO),
                new PropertyValue.DoubleValue(new BigDecimal("320.5")),
                new PropertyValue.DoubleValue(new BigDecimal("12.25")),
                new PropertyValue.DoubleValue(new BigDecimal("240")));
        for (int index = 0; index < numericNames.size(); index++) {
            String name = numericNames.get(index);
            Node.Property<FlutterPropertyCellValue> property = cellProperty(
                    property(node, name));
            PropertyEditor editor = property.getPropertyEditor();
            editor.setAsText(values.get(index).value().toPlainString());
            assertAll(
                    () -> assertEquals(numericLabels.get(numericNames.indexOf(name)),
                            property.getDisplayName()),
                    () -> assertEquals(values.get(numericNames.indexOf(name)),
                            cell(editor).explicitValue().orElseThrow()),
                    () -> assertTrue(property.getShortDescription().contains("inherits")),
                    () -> assertTrue(property.supportsDefaultValue()),
                    () -> assertTrue(property.isDefaultValue()),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> editor.setAsText("-0.1")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> editor.setAsText("Infinity")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> property.setValue(FlutterPropertyCellValue.explicit(
                                    new PropertyValue.IntegerValue(BigInteger.ZERO)))));
        }
        assertAll(
                () -> assertTrue(property(node, "minWidth").getShortDescription()
                        .contains("minimum width must not exceed maximum width")),
                () -> assertTrue(property(node, "maxWidth").getShortDescription()
                        .contains("overflow is allowed")),
                () -> assertTrue(property(node, "minHeight").getShortDescription()
                        .contains("minimum height must not exceed maximum height")),
                () -> assertTrue(property(node, "maxHeight").getShortDescription()
                        .contains("overflow is allowed")));

        Node.Property<FlutterPropertyCellValue> fit = cellProperty(property(node, "fit"));
        assertAll(
                () -> assertEquals("Fit", fit.getDisplayName()),
                () -> assertEquals(List.of("<not set>", "max", "deferToChild"),
                        List.of(fit.getPropertyEditor().getTags())),
                () -> assertTrue(fit.getShortDescription().contains("Flutter's default")),
                () -> assertTrue(fit.getShortDescription().contains("smallest size")),
                () -> assertTrue(fit.getShortDescription()
                        .contains("only when the child does not overflow")),
                () -> assertTrue(fit.supportsDefaultValue()),
                () -> assertTrue(fit.isDefaultValue()));

        PropertyValue.EnumValue deferToChild =
                new PropertyValue.EnumValue("OverflowBoxFit", "deferToChild");
        alignment.setValue(FlutterPropertyCellValue.explicit(directional));
        for (int index = 0; index < numericNames.size(); index++) {
            cellProperty(property(node, numericNames.get(index))).setValue(
                    FlutterPropertyCellValue.explicit(values.get(index)));
        }
        fit.setValue(FlutterPropertyCellValue.explicit(deferToChild));
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("alignment"), directional),
                new SetProperty(id, new PropertyName("minWidth"), values.get(0)),
                new SetProperty(id, new PropertyName("maxWidth"), values.get(1)),
                new SetProperty(id, new PropertyName("minHeight"), values.get(2)),
                new SetProperty(id, new PropertyName("maxHeight"), values.get(3)),
                new SetProperty(id, new PropertyName("fit"), deferToChild)),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Empty", child.getValue()),
                () -> assertTrue(child.getShortDescription()
                        .contains("minimum and maximum constraint overrides")),
                () -> assertTrue(child.getShortDescription()
                        .contains("physical or directional alignment")),
                () -> assertTrue(child.getShortDescription()
                        .contains("paint outside this box")),
                () -> assertTrue(child.getShortDescription()
                        .contains("follows the child within the parent constraints")),
                () -> assertTrue(child.getShortDescription()
                        .contains("Occupancy: 0/1")));
    }

    @Test
    void stackProjectsExactLayerPropertiesAndOrderedChildrenContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Stack");
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("f2a439a3-3b4f-4510-9504-61b5e74517aa"));
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                "alignment", "textDirection", "fit", "clipBehavior"),
                names(properties.getProperties()));
        assertEquals(
                "Layer alignment, direction, sizing, clipping, and ordered "
                + "non-Positioned children for the selected Stack widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        assertEquals("Alignment", alignment.getDisplayName());
        assertEquals(
                "Physical position or directional position for the current Designer's "
                + "ordinary, non-Positioned children. Flutter defaults to directional "
                + "top-start; coordinates outside -1 through 1 extrapolate. Directional "
                + "values use the explicit Stack text direction when set, otherwise "
                + "ambient LTR/RTL Directionality. Accepted: physical or directional "
                + "AlignmentGeometry. Restore Default removes the explicit constructor "
                + "argument.",
                alignment.getShortDescription());
        assertTrue(alignment.supportsDefaultValue());

        Node.Property<FlutterPropertyCellValue> textDirection = cellProperty(
                property(node, "textDirection"));
        assertEquals("Text direction", textDirection.getDisplayName());
        assertEquals(
                "Optional LTR or RTL override used only to resolve directional Stack "
                + "alignment. Omission preserves the ambient Directionality; this value "
                + "is not read from the theme. Accepted: TextDirection[rtl, ltr]. Restore "
                + "Default removes the explicit constructor argument.",
                textDirection.getShortDescription());
        PropertyEditor textDirectionEditor = textDirection.getPropertyEditor();
        textDirectionEditor.setAsText("rtl");
        assertEquals(
                new PropertyValue.EnumValue("TextDirection", "rtl"),
                cell(textDirectionEditor).explicitValue().orElseThrow());

        Node.Property<FlutterPropertyCellValue> fit = cellProperty(
                property(node, "fit"));
        assertEquals("Fit", fit.getDisplayName());
        assertEquals(
                "How Stack constrains non-positioned children: loose relaxes incoming "
                + "minimums, expand tightens to the biggest allowed size, and passthrough "
                + "preserves incoming constraints. Flutter defaults to loose. Accepted: "
                + "StackFit[loose, expand, passthrough]. Restore Default removes the "
                + "explicit constructor argument.",
                fit.getShortDescription());
        PropertyEditor fitEditor = fit.getPropertyEditor();
        fitEditor.setAsText("expand");
        assertEquals(
                new PropertyValue.EnumValue("StackFit", "expand"),
                cell(fitEditor).explicitValue().orElseThrow());

        Node.Property<FlutterPropertyCellValue> clipBehavior = cellProperty(
                property(node, "clipBehavior"));
        assertEquals("Clip behavior", clipBehavior.getDisplayName());
        assertEquals(
                "The selected value is passed to Flutter unchanged; Flutter defaults to "
                + "hard edge. RenderStack clips only when a direct child's geometry sets "
                + "its visual-overflow flag. Extrapolated alignment of a non-Positioned "
                + "child does not set that flag, so it is not clipped; descendant or "
                + "paint-only overflow is not clipped either. Accepted: "
                + "Clip[none, hardEdge, antiAlias, "
                + "antiAliasWithSaveLayer]. Restore Default removes the explicit "
                + "constructor argument.",
                clipBehavior.getShortDescription());
        PropertyEditor clipEditor = clipBehavior.getPropertyEditor();
        clipEditor.setAsText("antiAlias");
        assertEquals(
                new PropertyValue.EnumValue("Clip", "antiAlias"),
                cell(clipEditor).explicitValue().orElseThrow());

        Node.Property<?> children = property(node, "children");
        assertAll(
                () -> assertEquals("Children", children.getDisplayName()),
                () -> assertEquals("Empty", children.getValue()),
                () -> assertEquals(
                        "Ordered Stack children painted from first (back) to last (front). "
                        + "The current Designer slice creates ordinary, non-Positioned "
                        + "children; they use Stack alignment and fit. Directional alignment "
                        + "resolves from the explicit or ambient TextDirection "
                        + "(LTR/RTL), not from the theme. Occupancy: 0/10000; minimum: 0. "
                        + "Open the custom editor to add, move, reorder, or remove a widget.",
                        children.getShortDescription()));
    }

    @Test
    void wrapProjectsEveryRunPropertyAndOrderedChildrenContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Wrap");
        WidgetNode widget = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("0cdde885-b68c-4b21-bd47-246721c4e8b2"));
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of(
                        "direction", "alignment", "spacing", "runAlignment", "runSpacing",
                        "crossAxisAlignment", "textDirection", "verticalDirection",
                        "clipBehavior"),
                names(properties.getProperties()));
        assertEquals(
                "Run direction, spacing, alignment, clipping, and ordered children "
                + "for the selected Wrap widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> direction = cellProperty(
                property(node, "direction"));
        assertEquals("Direction", direction.getDisplayName());
        assertEquals(List.of("<not set>", "horizontal", "vertical"),
                List.of(direction.getPropertyEditor().getTags()));

        Node.Property<FlutterPropertyCellValue> alignment = cellProperty(
                property(node, "alignment"));
        assertEquals("Alignment", alignment.getDisplayName());
        assertEquals(List.of(
                        "<not set>", "start", "end", "center", "spaceBetween",
                        "spaceAround", "spaceEvenly"),
                List.of(alignment.getPropertyEditor().getTags()));

        Node.Property<FlutterPropertyCellValue> spacing = cellProperty(
                property(node, "spacing"));
        assertTrue(spacing.getShortDescription().contains(
                "negative values intentionally overlap"));
        PropertyEditor spacingEditor = spacing.getPropertyEditor();
        spacingEditor.setAsText("-4.5");
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("-4.5")),
                cell(spacingEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> spacingEditor.setAsText("NaN"));

        Node.Property<FlutterPropertyCellValue> runSpacing = cellProperty(
                property(node, "runSpacing"));
        assertTrue(runSpacing.getShortDescription().contains(
                "negative values intentionally overlap"));
        assertEquals(List.of("<not set>", "start", "end", "center"),
                List.of(property(node, "crossAxisAlignment")
                        .getPropertyEditor().getTags()));
        assertEquals(List.of(
                        "<not set>", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer"),
                List.of(property(node, "clipBehavior")
                        .getPropertyEditor().getTags()));

        Node.Property<?> children = property(node, "children");
        assertAll(
                () -> assertEquals("Children", children.getDisplayName()),
                () -> assertEquals("Empty", children.getValue()),
                () -> assertTrue(children.getShortDescription()
                        .contains("flowed into one or more runs")),
                () -> assertTrue(children.getShortDescription()
                        .contains("exact source, paint, and semantic order")),
                () -> assertTrue(children.getShortDescription()
                        .contains("Occupancy: 0/10000")));
    }

    @Test
    void expandedProjectsExactFlexAndRequiredReplacementOnlyChildContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Expanded");
        WidgetDefinition textDefinition = definition("flutter.widgets.Text");
        WidgetNode text = WidgetNodePrototypeFactory.create(
                textDefinition,
                StableId.parse("438ab773-e3bd-4a42-865d-dd4d4f6f5370"));
        WidgetNode widget = new WidgetNode(
                StableId.parse("407eb327-daf5-4720-bf24-0060ee7f63b8"),
                definition.typeId(),
                Map.of(new PropertyName("flex"),
                        new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(2))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("flex"), names(properties.getProperties()));
        assertEquals(
                "Remaining-space allocation and required child contract for the "
                + "selected direct Row or Column Expanded widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> flex = cellProperty(
                property(node, "flex"));
        assertEquals("Flex", flex.getDisplayName());
        assertTrue(flex.getShortDescription().startsWith(
                "Non-negative integer share of remaining Row or Column main-axis "
                + "space. Flutter defaults to 1; zero is valid but makes the child "
                + "inflexible. Positive flex requires bounded width in Row or bounded "
                + "height in Column."));
        PropertyEditor flexEditor = flex.getPropertyEditor();
        flexEditor.setAsText("0");
        assertEquals(
                new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
                cell(flexEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> flexEditor.setAsText("-1"));

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Text", child.getValue()),
                () -> assertEquals(
                        "Required child expanded with FlexFit.tight along the direct Row or "
                        + "Column main axis. Occupancy: 1/1; minimum: 1. Open the custom "
                        + "editor to replace the child atomically; it cannot be removed "
                        + "or cleared.",
                        child.getShortDescription()));
    }

    @Test
    void flexibleProjectsOptionalFlexAndFitEditorsAndRequiredChildContract()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Flexible");
        WidgetDefinition textDefinition = definition("flutter.widgets.Text");
        WidgetNode text = WidgetNodePrototypeFactory.create(
                textDefinition,
                StableId.parse("73d20841-67ad-474c-9714-07045ff0d2bd"));
        StableId widgetId = StableId.parse(
                "38dd6a54-2e2b-413f-af2c-a2a8262c464a");
        WidgetNode widget = new WidgetNode(
                widgetId,
                definition.typeId(),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("flex", "fit"), names(properties.getProperties()));
        assertEquals(
                "Loose or tight remaining-space allocation and required child "
                + "contract for the selected direct Row or Column Flexible widget.",
                properties.getShortDescription());

        Node.Property<FlutterPropertyCellValue> flex = cellProperty(
                property(node, "flex"));
        Node.Property<FlutterPropertyCellValue> fit = cellProperty(
                property(node, "fit"));
        assertAll(
                () -> assertEquals("Flex", flex.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), flex.getValue()),
                () -> assertTrue(flex.getShortDescription().contains(
                        "zero is valid and makes the child inflexible")),
                () -> assertTrue(flex.getShortDescription().contains(
                        "fit has no effect")),
                () -> assertEquals("Fit", fit.getDisplayName()),
                () -> assertEquals(FlutterPropertyCellValue.unset(), fit.getValue()),
                () -> assertTrue(fit.getShortDescription().contains(
                        "Loose, Flutter's default")),
                () -> assertTrue(fit.getShortDescription().contains(
                        "tight requires it to fill")));

        PropertyEditor flexEditor = flex.getPropertyEditor();
        flexEditor.setAsText("0");
        assertEquals(
                new PropertyValue.IntegerValue(BigInteger.ZERO),
                cell(flexEditor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> flexEditor.setAsText("-1"));

        PropertyEditor fitEditor = fit.getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT, "loose", "tight"),
                List.of(fitEditor.getTags()));
        fitEditor.setAsText("tight");
        assertEquals(
                new PropertyValue.EnumValue("FlexFit", "tight"),
                cell(fitEditor).explicitValue().orElseThrow());

        FlutterPropertyCellValue flexValue = FlutterPropertyCellValue.explicit(
                new PropertyValue.IntegerValue(BigInteger.valueOf(3)));
        FlutterPropertyCellValue fitValue = FlutterPropertyCellValue.explicit(
                new PropertyValue.EnumValue("FlexFit", "tight"));
        flex.setValue(flexValue);
        fit.setValue(fitValue);
        assertEquals(List.of(
                new SetProperty(widgetId, new PropertyName("flex"),
                        flexValue.explicitValue().orElseThrow()),
                new SetProperty(widgetId, new PropertyName("fit"),
                        fitValue.explicitValue().orElseThrow())),
                commands);

        Node.Property<?> child = property(node, "child");
        assertAll(
                () -> assertEquals("Child", child.getDisplayName()),
                () -> assertEquals("Text", child.getValue()),
                () -> assertTrue(child.getShortDescription().contains(
                        "Loose fit allows the child to remain smaller")),
                () -> assertTrue(child.getShortDescription().contains(
                        "tight fit requires it to fill")),
                () -> assertTrue(child.getShortDescription().contains(
                        "replace the child atomically")),
                () -> assertTrue(child.getShortDescription().contains(
                        "cannot be removed or cleared")));
    }

    @Test
    void spacerProjectsPositiveFlexEditorWithoutAChildSlot() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Spacer");
        StableId widgetId = StableId.parse(
                "f43e5a28-eacb-42c3-9cec-3a2f7d56256e");
        PropertyName flexName = new PropertyName("flex");
        WidgetNode widget = new WidgetNode(
                widgetId,
                definition.typeId(),
                Map.of(flexName, new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                Map.of(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet properties = propertySet(
                node, FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME);
        assertEquals(List.of("flex"), names(properties.getProperties()));
        assertEquals(
                "Empty positive-flex remaining-space allocation for the selected direct "
                + "Row or Column Spacer widget.",
                properties.getShortDescription());
        assertTrue(Arrays.stream(node.getPropertySets()).noneMatch(set ->
                FlutterWidgetPropertiesNode.SLOTS_SET_NAME.equals(set.getName())));

        Node.Property<FlutterPropertyCellValue> flex = cellProperty(
                property(node, "flex"));
        assertAll(
                () -> assertEquals("Flex", flex.getDisplayName()),
                () -> assertTrue(flex.getShortDescription().startsWith(
                        "Positive integer share of the remaining Row or Column main-axis "
                        + "space reserved as an empty gap. Flutter defaults to 1; zero is "
                        + "invalid. Positive flex requires bounded width in Row or bounded "
                        + "height in Column.")),
                () -> assertTrue(flex.supportsDefaultValue()),
                () -> assertFalse(flex.isDefaultValue()));

        PropertyEditor editor = flex.getPropertyEditor();
        editor.setAsText("1");
        assertEquals(
                new PropertyValue.IntegerValue(BigInteger.ONE),
                cell(editor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText("0"));
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText("-1"));

        FlutterPropertyCellValue flexValue = FlutterPropertyCellValue.explicit(
                new PropertyValue.IntegerValue(BigInteger.valueOf(3)));
        flex.setValue(flexValue);
        flex.restoreDefaultValue();
        assertEquals(List.of(
                new SetProperty(widgetId, flexName,
                        flexValue.explicitValue().orElseThrow()),
                new ResetProperty(widgetId, flexName)), commands);
    }

    @Test
    void scaffoldProjectsEnterpriseGroupsPresetsAndExactSetResetCommands()
            throws Exception {
        WidgetDefinition definition = definition("flutter.material.Scaffold");
        StableId id = StableId.parse("fe1e6841-39d2-4729-87ea-8e40cfdce57f");
        PropertyName background = new PropertyName("backgroundColor");
        WidgetNode widget = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(background, new PropertyValue.ColorValue(0xFF102030L)),
                Map.of(),
                Extensions.empty());
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        List<String> expectedSets = new ArrayList<>();
        expectedSets.add(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME);
        Arrays.stream(ScaffoldWidgetPropertySchema.Group.values())
                .map(ScaffoldWidgetPropertySchema.Group::setName)
                .forEach(expectedSets::add);
        expectedSets.add(FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(expectedSets,
                Arrays.stream(sets).map(Node.PropertySet::getName).toList());
        assertEquals(ScaffoldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                Arrays.stream(sets)
                        .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME
                                .equals(set.getName()))
                        .filter(set -> !FlutterWidgetPropertiesNode.SLOTS_SET_NAME
                                .equals(set.getName()))
                        .mapToInt(set -> set.getProperties().length)
                        .sum());

        PropertyEditor location = property(
                node, "floatingActionButtonLocation").getPropertyEditor();
        assertEquals(20, location.getTags().length,
                "unset plus every public Flutter 3.44.8 location preset");
        assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT, location.getTags()[0]);
        location.setAsText("miniCenterDocked");
        assertEquals(new PropertyValue.StringValue("miniCenterDocked"),
                cell(location).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> location.setAsText("customLocation"));

        PropertyEditor callback = property(node, "onDrawerChanged").getPropertyEditor();
        callback.setAsText("_drawerChanged");
        assertEquals(new PropertyValue.CallbackValue("_drawerChanged"),
                cell(callback).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> callback.setAsText("(open) => log(open)"));

        cellProperty(property(node, "primary")).setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.BooleanValue(false)));
        cellProperty(property(node, "backgroundColor")).restoreDefaultValue();
        assertEquals(List.of(
                        new SetProperty(
                                id, new PropertyName("primary"),
                                new PropertyValue.BooleanValue(false)),
                        new ResetProperty(id, background)),
                commands);

        for (Node.PropertySet set : Arrays.copyOfRange(sets, 1, sets.length - 1)) {
            for (Node.Property<?> property : set.getProperties()) {
                assertTrue(property.canWrite(), property.getName());
                assertEquals(FlutterPropertyCellValue.class, property.getValueType());
            }
        }
    }

    @Test
    void textFieldProjectsExactFiftyFourRowsHelpPresetsAndResetSemantics()
            throws Exception {
        WidgetDefinition definition = definition("flutter.material.TextField");
        StableId id = StableId.parse("12d90d32-a237-4d35-9694-f3f434572853");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        assertEquals(
                "Text Field — " + id + ". Runtime typed text, selection, controller "
                + "state, and focus state are not stored by Designer.",
                node.getShortDescription());
        assertTrue(widget.properties().isEmpty(),
                "TextField creation must not invent constructor defaults or runtime state");
        assertTrue(widget.slots().isEmpty(), "TextField is a leaf");
        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(List.of(
                FlutterWidgetPropertiesNode.IDENTITY_SET_NAME,
                "textFieldInput",
                "textFieldLayout",
                "textFieldBehavior",
                "textFieldCursorSelection",
                "textFieldCallbacks",
                "textFieldRestoration"),
                Arrays.stream(sets).map(Node.PropertySet::getName).toList());
        assertEquals(List.of(
                "keyboardType", "textInputAction", "textCapitalization",
                "obscuringCharacter", "obscureText", "autocorrect",
                "smartDashesType", "smartQuotesType", "enableSuggestions",
                "maxLines", "minLines", "maxLength", "maxLengthEnforcement",
                "keyboardAppearance"), names(sets[1].getProperties()));
        assertEquals(List.of(
                "textAlign", "textAlignVertical", "textDirection", "expands",
                "scrollPaddingLeft", "scrollPaddingTop", "scrollPaddingRight",
                "scrollPaddingBottom", "clipBehavior"),
                names(sets[2].getProperties()));
        assertEquals(List.of(
                "readOnly", "autofocus", "enabled", "ignorePointers",
                "dragStartBehavior", "enableInteractiveSelection",
                "selectAllOnFocus",
                "stylusHandwritingEnabled", "enableIMEPersonalizedLearning",
                "enableInlinePrediction", "canRequestFocus"),
                names(sets[3].getProperties()));
        assertEquals(List.of(
                "showCursor", "cursorWidth", "cursorHeight", "cursorRadiusX",
                "cursorRadiusY", "cursorOpacityAnimates", "cursorColor",
                "cursorErrorColor", "selectionHeightStyle",
                "selectionWidthStyle", "mouseCursor"),
                names(sets[4].getProperties()));
        assertEquals(List.of(
                "onChanged", "onEditingComplete", "onSubmitted",
                "onAppPrivateCommand", "onTap", "onTapAlwaysCalled", "onTapOutside",
                "onTapUpOutside"), names(sets[5].getProperties()));
        assertEquals(List.of("restorationId"), names(sets[6].getProperties()));

        int writableRows = 0;
        for (int index = 0;
                index < TextFieldWidgetPropertySchema.Group.values().length;
                index++) {
            TextFieldWidgetPropertySchema.Group group =
                    TextFieldWidgetPropertySchema.Group.values()[index];
            Node.PropertySet set = sets[index + 1];
            assertEquals(group.displayName(), set.getDisplayName());
            assertEquals(group.description(), set.getShortDescription());
            for (Node.Property<?> property : set.getProperties()) {
                TextFieldWidgetPropertySchema.Definition presentation =
                        TextFieldWidgetPropertySchema.find(property.getName())
                                .orElseThrow();
                assertEquals(group, presentation.group(), property.getName());
                assertEquals(presentation.displayName(), property.getDisplayName());
                assertTrue(property.getShortDescription().startsWith(
                        presentation.description() + " Accepted: "),
                        property.getName());
                String resetHelp = switch (presentation.target()) {
                    case CURSOR_RADIUS ->
                        "Restore Default removes the complete cursorRadius value.";
                    case SCROLL_PADDING ->
                        "Restore Default removes the complete scrollPadding value.";
                    default ->
                        "Restore Default removes the explicit constructor argument.";
                };
                assertTrue(property.getShortDescription().endsWith(resetHelp),
                        property.getName());
                assertTrue(property.canWrite(), property.getName());
                assertEquals(FlutterPropertyCellValue.class,
                        property.getValueType(), property.getName());
                assertTrue(property.supportsDefaultValue(), property.getName());
                assertTrue(property.isDefaultValue(), property.getName());
                assertNotNull(property.getPropertyEditor(), property.getName());
                writableRows++;
            }
        }
        assertEquals(TextFieldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                writableRows);
        assertEquals(54, writableRows);

        PropertyEditor keyboardType = property(node, "keyboardType")
                .getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "text", "multiline", "number", "numberSigned",
                "numberDecimal", "numberSignedDecimal", "phone", "datetime",
                "emailAddress", "url", "visiblePassword", "name",
                "streetAddress", "none", "webSearch", "twitter"),
                List.of(keyboardType.getTags()));
        keyboardType.setAsText("emailAddress");
        assertEquals(new PropertyValue.StringValue("emailAddress"),
                cell(keyboardType).explicitValue().orElseThrow());

        PropertyEditor vertical = property(node, "textAlignVertical")
                .getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT, "top", "center", "bottom"),
                List.of(vertical.getTags()));
        vertical.setAsText("bottom");
        assertEquals(new PropertyValue.StringValue("bottom"),
                cell(vertical).explicitValue().orElseThrow());

        PropertyEditor mouseCursor = property(node, "mouseCursor")
                .getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "none", "basic", "click", "forbidden", "wait", "progress",
                "contextMenu", "help", "text", "verticalText", "cell",
                "precise", "move", "grab", "grabbing", "noDrop", "alias",
                "copy", "disappearing", "allScroll", "resizeLeftRight",
                "resizeUpDown", "resizeUpLeftDownRight",
                "resizeUpRightDownLeft", "resizeUp", "resizeDown",
                "resizeLeft", "resizeRight", "resizeUpLeft", "resizeUpRight",
                "resizeDownLeft", "resizeDownRight", "resizeColumn",
                "resizeRow", "zoomIn", "zoomOut"),
                List.of(mouseCursor.getTags()));
        mouseCursor.setAsText("click");
        assertEquals(new PropertyValue.StringValue("click"),
                cell(mouseCursor).explicitValue().orElseThrow());

        PropertyEditor inputAction = property(node, "textInputAction")
                .getPropertyEditor();
        inputAction.setAsText("search");
        assertEquals(new PropertyValue.EnumValue("TextInputAction", "search"),
                cell(inputAction).explicitValue().orElseThrow());
        PropertyEditor callback = property(node, "onChanged").getPropertyEditor();
        callback.setAsText("_handleChanged");
        assertEquals(new PropertyValue.CallbackValue("_handleChanged"),
                cell(callback).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> callback.setAsText("(value) => print(value)"));

        PropertyEditor maxLines = property(node, "maxLines").getPropertyEditor();
        maxLines.setAsText("2");
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                cell(maxLines).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> maxLines.setAsText("0"));
        PropertyEditor cursorWidth = property(node, "cursorWidth").getPropertyEditor();
        cursorWidth.setAsText("1.5");
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("1.5")),
                cell(cursorWidth).explicitValue().orElseThrow());
        PropertyEditor readOnly = property(node, "readOnly").getPropertyEditor();
        readOnly.setAsText("true");
        assertEquals(new PropertyValue.BooleanValue(true),
                cell(readOnly).explicitValue().orElseThrow());
        PropertyEditor cursorColor = property(node, "cursorColor")
                .getPropertyEditor();
        cursorColor.setAsText("0xFF336699");
        assertEquals(PropertyValue.ColorValue.fromWireArgb("0xFF336699"),
                cell(cursorColor).explicitValue().orElseThrow());

        WidgetNode explicit = new WidgetNode(
                id,
                definition.typeId(),
                Map.of(new PropertyName("readOnly"),
                        new PropertyValue.BooleanValue(true)),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode explicitNode = new FlutterWidgetPropertiesNode(
                Children.LEAF, explicit, definition, commands::add);
        Node.Property<?> explicitReadOnly = property(explicitNode, "readOnly");
        assertFalse(explicitReadOnly.isDefaultValue());
        explicitReadOnly.restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(id, new PropertyName("readOnly"))),
                commands);
    }

    @Test
    void textFieldCompoundRowsSubmitAtomicSetAndResetPatches() throws Exception {
        WidgetDefinition definition = definition("flutter.material.TextField");
        StableId id = StableId.parse("a5b58e87-6ea2-4efd-bf85-a8825d5fa124");
        WidgetNode empty = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode emptyNode = new FlutterWidgetPropertiesNode(
                Children.LEAF, empty, definition, commands::add);
        assertTrue(property(emptyNode, "cursorRadiusX").getShortDescription()
                .contains("editing either axis seeds both axes"));
        assertTrue(property(emptyNode, "scrollPaddingTop").getShortDescription()
                .contains("editing any edge seeds all four edges"));
        PropertyValue.DoubleValue radiusSeed = new PropertyValue.DoubleValue(
                new BigDecimal("4.5"));

        cellProperty(property(emptyNode, "cursorRadiusX")).setValue(
                FlutterPropertyCellValue.explicit(radiusSeed));

        PatchProperties radiusSet = assertInstanceOf(
                PatchProperties.class, commands.getFirst());
        assertEquals(id, radiusSet.widgetId());
        assertEquals(List.of(
                new PatchProperties.SetPatch(
                        new PropertyName("cursorRadiusX"), radiusSeed),
                new PatchProperties.SetPatch(
                        new PropertyName("cursorRadiusY"), radiusSeed)),
                radiusSet.patches(),
                "the first entered radius seeds a complete value without a default");

        commands.clear();
        PropertyValue.DoubleValue paddingSeed = new PropertyValue.DoubleValue(
                BigDecimal.valueOf(12));
        cellProperty(property(emptyNode, "scrollPaddingTop")).setValue(
                FlutterPropertyCellValue.explicit(paddingSeed));

        PatchProperties paddingSet = assertInstanceOf(
                PatchProperties.class, commands.getFirst());
        assertEquals(List.of(
                new PatchProperties.SetPatch(
                        new PropertyName("scrollPaddingLeft"), paddingSeed),
                new PatchProperties.SetPatch(
                        new PropertyName("scrollPaddingTop"), paddingSeed),
                new PatchProperties.SetPatch(
                        new PropertyName("scrollPaddingRight"), paddingSeed),
                new PatchProperties.SetPatch(
                        new PropertyName("scrollPaddingBottom"), paddingSeed)),
                paddingSet.patches(),
                "the first entered inset seeds all four explicit edges");

        PropertyValue.DoubleValue radiusX = new PropertyValue.DoubleValue(
                BigDecimal.valueOf(2));
        PropertyValue.DoubleValue radiusY = new PropertyValue.DoubleValue(
                BigDecimal.valueOf(3));
        PropertyValue.DoubleValue left = new PropertyValue.DoubleValue(
                BigDecimal.ONE);
        PropertyValue.DoubleValue top = new PropertyValue.DoubleValue(
                BigDecimal.valueOf(2));
        PropertyValue.DoubleValue right = new PropertyValue.DoubleValue(
                BigDecimal.valueOf(3));
        PropertyValue.DoubleValue bottom = new PropertyValue.DoubleValue(
                BigDecimal.valueOf(4));
        WidgetNode configured = new WidgetNode(
                id,
                definition.typeId(),
                Map.ofEntries(
                        Map.entry(new PropertyName("cursorRadiusX"), radiusX),
                        Map.entry(new PropertyName("cursorRadiusY"), radiusY),
                        Map.entry(new PropertyName("scrollPaddingLeft"), left),
                        Map.entry(new PropertyName("scrollPaddingTop"), top),
                        Map.entry(new PropertyName("scrollPaddingRight"), right),
                        Map.entry(new PropertyName("scrollPaddingBottom"), bottom)),
                Map.of(),
                Extensions.empty());
        commands.clear();
        FlutterWidgetPropertiesNode configuredNode =
                new FlutterWidgetPropertiesNode(
                        Children.LEAF, configured, definition, commands::add);

        PropertyValue.DoubleValue updatedRadiusX =
                new PropertyValue.DoubleValue(BigDecimal.valueOf(9));
        cellProperty(property(configuredNode, "cursorRadiusX")).setValue(
                FlutterPropertyCellValue.explicit(updatedRadiusX));
        assertEquals(List.of(
                new PatchProperties.SetPatch(
                        new PropertyName("cursorRadiusX"), updatedRadiusX),
                new PatchProperties.SetPatch(
                        new PropertyName("cursorRadiusY"), radiusY)),
                assertInstanceOf(PatchProperties.class, commands.getFirst())
                        .patches(),
                "editing a complete pair must preserve its explicit peer");

        commands.clear();
        cellProperty(property(configuredNode, "cursorRadiusY"))
                .restoreDefaultValue();
        PatchProperties radiusReset = assertInstanceOf(
                PatchProperties.class, commands.getFirst());
        assertEquals(List.of(
                new PatchProperties.ResetPatch(
                        new PropertyName("cursorRadiusX")),
                new PatchProperties.ResetPatch(
                        new PropertyName("cursorRadiusY"))),
                radiusReset.patches());

        commands.clear();
        cellProperty(property(configuredNode, "scrollPaddingRight"))
                .restoreDefaultValue();
        PatchProperties paddingReset = assertInstanceOf(
                PatchProperties.class, commands.getFirst());
        assertEquals(List.of(
                new PatchProperties.ResetPatch(
                        new PropertyName("scrollPaddingLeft")),
                new PatchProperties.ResetPatch(
                        new PropertyName("scrollPaddingTop")),
                new PatchProperties.ResetPatch(
                        new PropertyName("scrollPaddingRight")),
                new PatchProperties.ResetPatch(
                        new PropertyName("scrollPaddingBottom"))),
                paddingReset.patches());
    }

    @Test
    void elevatedButtonProjectsAllWritableLeavesIntoEnterpriseGroupsAndChildSlot()
            throws Exception {
        WidgetDefinition definition = definition("flutter.material.ElevatedButton");
        StableId id = StableId.parse("b20f626c-e6bd-45d8-aef1-26382ba3ffb6");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        List<String> expectedSets = new ArrayList<>();
        expectedSets.add(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME);
        Arrays.stream(ElevatedButtonWidgetPropertySchema.Group.values())
                .map(ElevatedButtonWidgetPropertySchema.Group::setName)
                .forEach(expectedSets::add);
        expectedSets.add(FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(expectedSets,
                Arrays.stream(sets).map(Node.PropertySet::getName).toList());
        assertEquals(java.util.Collections.nCopies(
                        1 + ElevatedButtonWidgetPropertySchema.Group.values().length,
                        FlutterWidgetPropertiesNode.GENERAL_TAB_NAME),
                Arrays.stream(sets)
                        .limit(1 + ElevatedButtonWidgetPropertySchema.Group.values().length)
                        .map(set -> set.getValue(
                                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE))
                        .toList());
        assertEquals(FlutterWidgetPropertiesNode.SLOTS_TAB_NAME,
                sets[sets.length - 1].getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));

        int writableLeaves = 0;
        for (Node.PropertySet set : Arrays.copyOfRange(sets, 1, sets.length - 1)) {
            for (Node.Property<?> property : set.getProperties()) {
                assertTrue(property.canWrite(), property.getName());
                assertEquals(FlutterPropertyCellValue.class,
                        property.getValueType(), property.getName());
                assertTrue(property.getPropertyEditor() != null, property.getName());
                writableLeaves++;
            }
        }
        assertEquals(ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT,
                writableLeaves);
        assertEquals(286, writableLeaves);
        assertEquals(List.of("child"),
                names(sets[sets.length - 1].getProperties()));

        PropertyEditor callback = property(node, "onPressed").getPropertyEditor();
        assertTrue(callback.supportsCustomEditor());
        assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT, callback.getAsText());
        callback.setAsText("_handlePress");
        assertEquals(new PropertyValue.CallbackValue("_handlePress"),
                cell(callback).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> callback.setAsText("() => arbitraryDart()"));
        assertThrows(IllegalArgumentException.class,
                () -> callback.setAsText("class"));

        PropertyEditor families = property(
                node, "stylePressedTextFontFamilyFallback").getPropertyEditor();
        assertTrue(families.supportsCustomEditor());
        families.setAsText("Roboto\nNoto Sans");
        assertEquals(new PropertyValue.StringValue("Roboto\nNoto Sans"),
                cell(families).explicitValue().orElseThrow());
        assertTrue(property(node, "styleDisabledPadding")
                .getPropertyEditor().supportsCustomEditor());
        assertTrue(property(node, "styleFocusedBackgroundColor")
                .getPropertyEditor().supportsCustomEditor());
        PropertyEditor shape = property(
                node, "stylePressedShapeKind").getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "roundedRectangle", "roundedSuperellipse", "stadium", "circle",
                "beveledRectangle", "continuousRectangle"),
                List.of(shape.getTags()));
        assertEquals(37, property(node, "styleHoveredMouseCursor")
                .getPropertyEditor().getTags().length,
                "unset plus every SystemMouseCursors constant must be selectable");
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "inkSplash", "inkRipple", "inkSparkle", "noSplash"),
                List.of(property(node, "styleSplashFactory")
                        .getPropertyEditor().getTags()));

        cellProperty(property(node, "onPressed")).setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.CallbackValue("_handlePress")));
        assertEquals(List.of(new SetProperty(
                id,
                new PropertyName("onPressed"),
                new PropertyValue.CallbackValue("_handlePress"))), commands);
    }

    @Test
    void iconProjectsTheExactEnterpriseGroupsAndWritableConstructorSurface()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Icon");
        StableId id = StableId.parse("04150536-0251-41a0-b7ab-d1b5a8bd7543");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        assertEquals(List.of(
                FlutterWidgetPropertiesNode.IDENTITY_SET_NAME,
                IconWidgetPropertySchema.Group.DATA.setName(),
                IconWidgetPropertySchema.Group.APPEARANCE.setName(),
                IconWidgetPropertySchema.Group.VARIABLE_FONT.setName(),
                IconWidgetPropertySchema.Group.ACCESSIBILITY.setName()),
                Arrays.stream(sets).map(Node.PropertySet::getName).toList());
        assertEquals(List.of("icon"), names(sets[1].getProperties()));
        assertEquals(List.of("size", "color", "shadows", "blendMode"),
                names(sets[2].getProperties()));
        assertEquals(List.of(
                "fill", "weight", "grade", "opticalSize", "fontWeight"),
                names(sets[3].getProperties()));
        assertEquals(List.of(
                "semanticLabel", "textDirection", "applyTextScaling"),
                names(sets[4].getProperties()));

        for (Node.PropertySet set : Arrays.copyOfRange(sets, 1, sets.length)) {
            for (Node.Property<?> property : set.getProperties()) {
                assertTrue(property.canWrite(), property.getName());
                assertEquals(FlutterPropertyCellValue.class,
                        property.getValueType(), property.getName());
            }
        }
        Node.Property<?> icon = property(node, "icon");
        assertFalse(icon.supportsDefaultValue(),
                "the required positional nullable IconData is explicit, including None");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.IconDataValue(
                                java.util.Optional.of(0xE5F9),
                                java.util.Optional.of("MaterialIcons"),
                                java.util.Optional.empty(), false, List.of())),
                icon.getValue());
        PropertyEditor iconEditor = icon.getPropertyEditor();
        assertTrue(iconEditor.supportsCustomEditor());
        iconEditor.setValue(icon.getValue());
        assertEquals("Icons.star (U+E5F9)", iconEditor.getAsText());
        assertTrue(property(node, "weight").getShortDescription()
                .contains("overrides Font weight"));
        String fontWeightDescription = property(node, "fontWeight").getShortDescription();
        assertTrue(fontWeightDescription.contains("not inherited from IconTheme"));
        assertTrue(fontWeightDescription.contains("Weight axis overrides"));
        assertTrue(property(node, "shadows").getShortDescription()
                .contains("explicit empty list"));

        cellProperty(property(node, "fontWeight")).setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.EnumValue("FontWeight", "w700")));
        cellProperty(property(node, "weight")).setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(650))));
        assertEquals(List.of(
                new SetProperty(id, new PropertyName("fontWeight"),
                        new PropertyValue.EnumValue("FontWeight", "w700")),
                new SetProperty(id, new PropertyName("weight"),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(650)))),
                commands,
                "both Flutter-valid values remain exact; help documents wght precedence");

        commands.clear();
        MaterialIconRegistry.MaterialIcon arrowBack = MaterialIconRegistry.bundled()
                .find("arrow_back").orElseThrow();
        PropertyValue.IconDataValue arrowBackValue =
                new PropertyValue.IconDataValue(
                        java.util.Optional.of(arrowBack.codePoint()),
                        java.util.Optional.of(arrowBack.fontFamily()),
                        java.util.Optional.empty(),
                        arrowBack.matchTextDirection(),
                        List.of());
        cellProperty(icon).setValue(
                FlutterPropertyCellValue.explicit(arrowBackValue));
        assertEquals(List.of(new SetProperty(
                        id, new PropertyName("icon"), arrowBackValue)),
                commands,
                "selecting an RTL glyph changes only Icon.icon and must never "
                + "invent a semanticLabel");
    }

    @Test
    void appBarProjectsEveryTypedLeafIntoEnterpriseGroupsAndExactSlots()
            throws Exception {
        WidgetDefinition definition = definition("flutter.material.AppBar");
        StableId id = StableId.parse("2f9193ca-c8cb-4473-a987-296cb63aaf35");
        WidgetNode widget = WidgetNodePrototypeFactory.create(definition, id);
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);

        Node.PropertySet[] sets = node.getPropertySets();
        List<String> expectedSets = new ArrayList<>();
        expectedSets.add(FlutterWidgetPropertiesNode.IDENTITY_SET_NAME);
        Arrays.stream(AppBarWidgetPropertySchema.Group.values())
                .map(AppBarWidgetPropertySchema.Group::setName)
                .forEach(expectedSets::add);
        expectedSets.add(FlutterWidgetPropertiesNode.SLOTS_SET_NAME);
        assertEquals(expectedSets,
                Arrays.stream(sets).map(Node.PropertySet::getName).toList());
        assertEquals(java.util.Collections.nCopies(
                        1 + AppBarWidgetPropertySchema.Group.values().length,
                        FlutterWidgetPropertiesNode.GENERAL_TAB_NAME),
                Arrays.stream(sets)
                        .limit(1 + AppBarWidgetPropertySchema.Group.values().length)
                        .map(set -> set.getValue(
                                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE))
                        .toList());
        assertEquals(FlutterWidgetPropertiesNode.SLOTS_TAB_NAME,
                sets[sets.length - 1].getValue(
                        FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE));

        int writableLeaves = 0;
        for (Node.PropertySet set : Arrays.copyOfRange(sets, 1, sets.length - 1)) {
            for (Node.Property<?> property : set.getProperties()) {
                assertTrue(property.canWrite(), property.getName());
                assertEquals(FlutterPropertyCellValue.class,
                        property.getValueType(), property.getName());
                assertTrue(property.getPropertyEditor() != null, property.getName());
                writableLeaves++;
            }
        }
        assertEquals(definition.properties().size(), writableLeaves);
        assertEquals(120, writableLeaves);
        assertEquals(List.of(
                "leading", "title", "actions", "flexibleSpace", "bottom"),
                names(sets[sets.length - 1].getProperties()));

        PropertyEditor predicate = property(node, "notificationPredicate")
                .getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "default", "depthZero", "all"),
                List.of(predicate.getTags()));
        predicate.setAsText("all");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue("all")),
                predicate.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> predicate.setAsText("arbitraryCallback"));

        PropertyEditor shape = property(node, "shapeKind").getPropertyEditor();
        assertEquals(List.of(
                FlutterPropertyCellValue.NOT_SET_TEXT,
                "roundedRectangle", "stadium", "circle",
                "beveledRectangle", "continuousRectangle"),
                List.of(shape.getTags()));
        PropertyEditor families = property(
                node, "toolbarTextStyleFontFamilyFallback").getPropertyEditor();
        assertTrue(families.supportsCustomEditor());
        families.setAsText("Roboto\nNoto Sans");
        assertEquals(new PropertyValue.StringValue("Roboto\nNoto Sans"),
                cell(families).explicitValue().orElseThrow());
        assertTrue(property(node, "actionsPadding")
                .getPropertyEditor().supportsCustomEditor());
        assertTrue(property(node, "systemOverlayStyleStatusBarColor")
                .getPropertyEditor().supportsCustomEditor());

        cellProperty(property(node, "notificationPredicate")).setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue("all")));
        assertEquals(List.of(new SetProperty(
                id,
                new PropertyName("notificationPredicate"),
                new PropertyValue.StringValue("all"))), commands);
    }

    @Test
    void booleanUsesCheckboxContractWhileEnumKeepsExplicitUnsetTags()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                textWidget(StableId.parse(
                        "a08766d3-71f6-4795-b642-c4c12bca156c"), "Text"),
                definition,
                ignored -> { });
        PropertyEditor bool = property(node, "softWrap").getPropertyEditor();
        PropertyEditor enumeration = property(node, "textAlign").getPropertyEditor();

        bool.setValue(FlutterPropertyCellValue.unset());
        assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT, bool.getAsText());
        assertNull(bool.getTags(),
                "boolean values must never select the combo renderer");
        assertTrue(bool.isPaintable(),
                "boolean values use the checkbox cell renderer");
        bool.setAsText("false");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.BooleanValue(false)),
                bool.getValue());

        assertEquals(FlutterPropertyCellValue.NOT_SET_TEXT,
                first(enumeration.getTags()));
        enumeration.setAsText("justify");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.EnumValue("TextAlign", "justify")),
                enumeration.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> enumeration.setAsText("diagonal"));
    }

    @Test
    void optionalStringEditorKeepsEmptyAndWhitespaceValuesDistinctFromUnset()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                textWidget(StableId.parse(
                        "cf87121b-c77b-46cb-823e-9c652b20e2aa"), "Text"),
                definition,
                ignored -> { });
        PropertyEditor editor = property(node, "semanticsLabel")
                .getPropertyEditor();

        editor.setAsText("");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue("")),
                editor.getValue());
        editor.setAsText("   ");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue("   ")),
                editor.getValue());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue(
                                FlutterPropertyCellValue.NOT_SET_TEXT)),
                editor.getValue(),
                "the presentation token remains a valid literal String value");
    }

    @Test
    void localeStringEditorsRejectInvalidSubtagsBeforeDispatchingACommand()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Text");
        StableId id = StableId.parse("a4b21569-6e59-4922-8f17-1502d46414ee");
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, textWidget(id, "Text"), definition, commands::add);

        PropertyEditor language = property(node, "localeLanguageCode").getPropertyEditor();
        PropertyEditor script = property(node, "localeScriptCode").getPropertyEditor();
        PropertyEditor country = property(node, "localeCountryCode").getPropertyEditor();

        assertThrows(IllegalArgumentException.class, () -> language.setAsText("EN"));
        assertThrows(IllegalArgumentException.class, () -> script.setAsText("cyrl"));
        assertThrows(IllegalArgumentException.class, () -> country.setAsText("ua"));
        assertEquals(List.of(), commands);

        language.setAsText("uk");
        cellProperty(property(node, "localeLanguageCode")).setValue(cell(language));
        assertEquals(List.of(new SetProperty(
                id,
                new PropertyName("localeLanguageCode"),
                new PropertyValue.StringValue("uk"))), commands);
    }

    @Test
    void numericEditorsPreserveIntegerVsDoubleAndEnforceCatalogRanges()
            throws Exception {
        FlutterWidgetPropertiesNode text = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                textWidget(StableId.parse(
                        "09e8316d-8b45-4f8b-b9ca-41024fe0b4bd"), "Text"),
                definition("flutter.widgets.Text"),
                ignored -> { });
        PropertyEditor maxLines = property(text, "maxLines").getPropertyEditor();

        maxLines.setAsText("3");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.IntegerValue(BigInteger.valueOf(3))),
                maxLines.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> maxLines.setAsText("0"));
        assertThrows(IllegalArgumentException.class,
                () -> maxLines.setAsText("1.5"));

        WidgetDefinition centerDefinition = definition("flutter.widgets.Center");
        FlutterWidgetPropertiesNode center = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                WidgetNode.empty(
                        StableId.parse("e120674c-d13a-48f7-bdf7-6d1894d67498"),
                        centerDefinition.typeId()),
                centerDefinition,
                ignored -> { });
        PropertyEditor widthFactor = property(center, "widthFactor")
                .getPropertyEditor();
        widthFactor.setAsText("2");
        assertInstanceOf(PropertyValue.IntegerValue.class,
                cell(widthFactor).explicitValue().orElseThrow());
        widthFactor.setAsText("2.5");
        assertInstanceOf(PropertyValue.DoubleValue.class,
                cell(widthFactor).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> widthFactor.setAsText("-0.1"));

        WidgetDefinition sizedBoxDefinition = definition(
                "flutter.widgets.SizedBox");
        FlutterWidgetPropertiesNode sizedBox = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                WidgetNode.empty(
                        StableId.parse("4595dfde-58bc-48b6-b2e6-a30ac9f08ebf"),
                        sizedBoxDefinition.typeId()),
                sizedBoxDefinition,
                ignored -> { });
        PropertyEditor width = property(sizedBox, "width").getPropertyEditor();
        PropertyEditor height = property(sizedBox, "height").getPropertyEditor();
        width.setAsText("120");
        assertInstanceOf(PropertyValue.IntegerValue.class,
                cell(width).explicitValue().orElseThrow());
        height.setAsText("48.5");
        assertInstanceOf(PropertyValue.DoubleValue.class,
                cell(height).explicitValue().orElseThrow());
        height.setAsText("0");
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ZERO),
                cell(height).explicitValue().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> width.setAsText("-0.1"));
        assertThrows(IllegalArgumentException.class,
                () -> height.setAsText("NaN"));

        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        FlutterWidgetPropertiesNode column = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                WidgetNode.empty(
                        StableId.parse("b217be18-889e-4634-9796-7e03af5de86c"),
                        columnDefinition.typeId()),
                columnDefinition,
                ignored -> { });
        PropertyEditor spacing = property(column, "spacing").getPropertyEditor();
        spacing.setAsText("2");
        assertEquals(FlutterPropertyCellValue.explicit(
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(2))),
                spacing.getValue(),
                "a catalog DOUBLE must not be silently changed to INTEGER");
        assertThrows(IllegalArgumentException.class,
                () -> spacing.setAsText("-1"));
    }

    @Test
    void edgeInsetsInlineEditorParsesEveryPaddingModeAndRoundTripsExactValues()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Padding");
        PropertyValue.EdgeInsetsValue initial = new PropertyValue.EdgeInsetsValue(
                BigDecimal.valueOf(16), BigDecimal.valueOf(16),
                BigDecimal.valueOf(16), BigDecimal.valueOf(16));
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                new WidgetNode(
                        StableId.parse("4f6cfe21-c535-47c2-8d82-78ef8224852c"),
                        definition.typeId(),
                        Map.of(new PropertyName("padding"), initial),
                        Map.of(),
                        Extensions.empty()),
                definition,
                ignored -> { });
        Node.Property<FlutterPropertyCellValue> padding = cellProperty(
                property(node, "padding"));
        PropertyEditor editor = padding.getPropertyEditor();

        assertFalse(padding.supportsDefaultValue());
        assertInlineInsets(editor, "8", edge("8", "8", "8", "8"));
        assertInlineInsets(editor, "all: 9.5",
                edge("9.5", "9.5", "9.5", "9.5"));
        assertInlineInsets(editor, "symmetric: 3, 7.5",
                edge("3", "7.5", "3", "7.5"));
        assertInlineInsets(editor, "physical: 1, 2, 3, 4",
                edge("1", "2", "3", "4"));
        assertInlineInsets(editor, "1, 2, 3, 4",
                edge("1", "2", "3", "4"));

        PropertyValue.EdgeInsetsDirectionalValue directional = directionalEdge(
                "5", "6.5", "7", "8.25");
        assertInlineInsets(editor, "Directional: 5, 6.5, 7, 8.25", directional);
        assertEquals("directional: 5, 6.5, 7, 8.25", editor.getAsText());
        editor.setAsText(editor.getAsText());
        assertEquals(directional, cell(editor).explicitValue().orElseThrow(),
                "the canonical inline representation must preserve directional semantics");

        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT));
    }

    @Test
    void edgeInsetsInlineEditorRejectsMalformedAndNegativeValuesWithoutLosingDraft()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Padding");
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                new WidgetNode(
                        StableId.parse("3aaf0334-64a8-45d9-895e-5939b6679d92"),
                        definition.typeId(),
                        Map.of(new PropertyName("padding"), edge("1", "2", "3", "4")),
                        Map.of(),
                        Extensions.empty()),
                definition,
                ignored -> { });
        PropertyEditor editor = property(node, "padding").getPropertyEditor();
        FlutterPropertyCellValue before = cell(editor);

        for (String invalid : List.of(
                "",
                "all: 1, 2",
                "symmetric: 1",
                "physical: 1, 2, 3",
                "directional: 1, 2, 3",
                "all: nope",
                "banana: 1, 2, 3, 4",
                "all: -1",
                "symmetric: -1, 2",
                "physical: 1, -2, 3, 4",
                "directional: 1, 2, -3, 4")) {
            assertThrows(IllegalArgumentException.class,
                    () -> editor.setAsText(invalid), invalid);
            assertEquals(before, cell(editor),
                    "a rejected inline Padding value must not replace the last valid value: "
                    + invalid);
        }
    }

    @Test
    void colorEditorUsesExactArgbAndSupportsOptionalUnset() {
        PropertyDefinition definition = new PropertyDefinition(
                new PropertyName("selectionColor"),
                DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR)),
                java.util.Optional.empty());
        FlutterTypedPropertyEditors.Binding binding =
                FlutterTypedPropertyEditors.binding(definition).orElseThrow();
        PropertyEditor editor = binding.createEditor();

        editor.setAsText("0x80aabbcc");
        assertEquals(FlutterPropertyCellValue.explicit(
                        PropertyValue.ColorValue.fromWireArgb("0x80AABBCC")),
                editor.getValue());
        assertEquals("0x80AABBCC", editor.getAsText());
        editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT);
        assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("#AABBCC"));
    }

    @Test
    void opaqueDartExpressionsRemainFailClosedInsteadOfBecomingTextEditors() {
        PropertyDefinition definition = new PropertyDefinition(
                new PropertyName("expression"),
                DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.AnyValue(
                        PropertyValueKind.DART_EXPRESSION)),
                java.util.Optional.empty());

        assertTrue(FlutterTypedPropertyEditors.binding(definition).isEmpty());
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
    void fortyFiveCanvasWidgetNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
            throws ReflectiveOperationException {
        List<String> typeIds = List.of(
                "flutter.material.Scaffold",
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.TextField",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Wrap",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Container",
                "flutter.widgets.Opacity",
                "flutter.widgets.Align",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.FittedBox",
                "flutter.widgets.ConstrainedBox",
                "flutter.widgets.UnconstrainedBox",
                "flutter.widgets.LimitedBox",
                "flutter.widgets.OverflowBox",
                "flutter.widgets.Stack",
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer",
                "flutter.widgets.Baseline",
                "flutter.widgets.IntrinsicHeight",
                "flutter.widgets.IntrinsicWidth",
                "flutter.widgets.Offstage",
                "flutter.widgets.SizedOverflowBox",
                "flutter.widgets.Transform",
                "flutter.widgets.RotatedBox",
                "flutter.widgets.ListBody",
                "flutter.widgets.OverflowBar",
                SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE.value(),
                "flutter.widgets.ListView",
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
                SingleChildScrollViewWidgetPropertySchema
                        .SINGLE_CHILD_SCROLL_VIEW_TYPE.value(),
                "flutter.widgets.Text",
                "flutter.widgets.Icon",
                "flutter.widgets.Image",
                "flutter.widgets.ColoredBox",
                PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE.value(),
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.value(),
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.value(),
                ClipRectWidgetPropertySchema.CLIP_RECT_TYPE.value(),
                ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE.value(),
                ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE.value(),
                ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.value(),
                ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE.value(),
                ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE.value(),
                IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.value());
        Set<String> iconPaths = new HashSet<>();
        typeIds = new ArrayList<>(typeIds);
        typeIds.add(PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE.value());
        typeIds.add("flutter.widgets.PhysicalShape");
        typeIds.add("flutter.widgets.RepaintBoundary");

        for (String typeId : typeIds) {
            WidgetDefinition definition = definition(typeId);
            Map<PropertyName, PropertyValue> creationValues =
                    "flutter.widgets.Image".equals(typeId)
                            ? Map.of(
                                    new PropertyName("image"),
                                    new PropertyValue.ImageProviderValue(
                                            PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                                            "assets/image.png",
                                            Optional.empty(), Optional.empty(), Optional.empty()))
                            : Map.of();
            WidgetNode widget = WidgetNodePrototypeFactory.create(
                    definition,
                    StableId.parse("5cf3483b-d627-41b4-bb1d-4a321aa36da4"),
                    creationValues);
            FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                    Children.LEAF, widget, definition);
            String expectedIcon = FlutterWidgetIconRegistry
                    .findIconPath(definition.typeId())
                    .orElseThrow();

            assertEquals(expectedIcon, declaredIconPath(node), typeId);
            iconPaths.add(declaredIconPath(node));
        }

        assertEquals(54, iconPaths.size(),
                "Design tree nodes must not share a generic widget icon");
    }

    @Test
    void physicalModelProjectsAllPropertiesPreservesRadiusAcrossCircleRefreshAndRejectsBadEdits()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.PhysicalModel");
        StableId id = StableId.parse("f93b867a-a162-47c4-841f-bd22936629bb");
        PropertyValue.BoxDecorationValue.Radius radius =
                new PropertyValue.BoxDecorationValue.Radius(
                        BigDecimal.valueOf(12), BigDecimal.valueOf(8));
        PropertyValue.BorderRadiusValue corners = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius, radius, radius, radius));
        PropertyName colorName = new PropertyName("color");
        PropertyName radiusName = new PropertyName("borderRadius");
        PropertyValue.ColorValue color = new PropertyValue.ColorValue(0xFF2196F3L);
        WidgetNode widget = new WidgetNode(id, definition.typeId(),
                Map.of(colorName, color, radiusName, corners),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);
        assertEquals(2 + PhysicalModelWidgetPropertySchema.Group.values().length,
                node.getPropertySets().length);
        for (String name : List.of("shape", "clipBehavior", "borderRadius", "elevation",
                "color", "shadowColor")) {
            assertTrue(property(node, name).canWrite(), name);
            assertEquals(FlutterPropertyCellValue.class, property(node, name).getValueType());
            assertFalse(property(node, name).getShortDescription().contains("clipper"), name);
        }
        Node.Property<FlutterPropertyCellValue> shape = cellProperty(property(node, "shape"));
        Node.Property<FlutterPropertyCellValue> borderRadius =
                cellProperty(property(node, "borderRadius"));
        Node.Property<FlutterPropertyCellValue> surface = cellProperty(property(node, "color"));
        Node.Property<FlutterPropertyCellValue> elevation =
                cellProperty(property(node, "elevation"));
        assertEquals(List.of(FlutterWidgetPropertiesNode.NOT_SET, "rectangle", "circle"),
                List.of(shape.getPropertyEditor().getTags()));
        assertEquals(List.of(FlutterWidgetPropertiesNode.NOT_SET,
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                List.of(property(node, "clipBehavior").getPropertyEditor().getTags()));
        assertTrue(borderRadius.getShortDescription().contains("null default"));
        assertTrue(borderRadius.getShortDescription().contains("ignores but preserves"));
        assertTrue(borderRadius.getPropertyEditor().supportsCustomEditor());
        assertFalse(surface.supportsDefaultValue());
        assertThrows(IllegalArgumentException.class,
                () -> surface.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> elevation.setValue(
                FlutterPropertyCellValue.explicit(
                        new PropertyValue.DoubleValue(BigDecimal.ONE.negate()))));
        assertThrows(IllegalArgumentException.class, () -> borderRadius.setValue(
                FlutterPropertyCellValue.explicit(new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                                radius, radius, radius, radius)))));
        assertTrue(commands.isEmpty());
        PropertyValue.EnumValue circle = new PropertyValue.EnumValue("BoxShape", "circle");
        shape.setValue(FlutterPropertyCellValue.explicit(circle));
        assertEquals(List.of(new SetProperty(id, new PropertyName("shape"), circle)), commands,
                "shape change must not issue a radius-reset patch");
        Node.PropertySet[] sets = node.getPropertySets();
        WidgetNode round = new WidgetNode(id, definition.typeId(),
                Map.of(colorName, color, radiusName, corners, new PropertyName("shape"), circle),
                widget.slots());
        commands.clear();
        node.refreshPresentation(round, definition, commands::add, null, null,
                FlutterImageAssetChoices.empty());
        assertEquals(List.of(sets), List.of(node.getPropertySets()));
        assertSame(shape, property(node, "shape"));
        assertSame(borderRadius, property(node, "borderRadius"));
        assertEquals(FlutterPropertyCellValue.explicit(corners), borderRadius.getValue());
        shape.restoreDefaultValue();
        borderRadius.restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(id, new PropertyName("shape")),
                new ResetProperty(id, radiusName)), commands);
        Node.Property<?> child = property(node, "child");
        assertEquals("Empty", child.getValue());
        assertTrue(child.getShortDescription().contains("elevated physical surface"));
        assertTrue(child.getShortDescription().contains("Occupancy: 0/1"));
    }

    @Test
    void physicalShapeProjectsAllFivePropertiesAndRequiredUnionRefreshKeepsFieldIdentity()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.PhysicalShape");
        StableId id = StableId.parse("d73b867a-a162-47c4-841f-bd22936629bb");
        PropertyName clipperName = new PropertyName("clipper");
        PropertyName colorName = new PropertyName("color");
        var preset = PropertyValue.ShapeBorderClipperValue.defaultValue();
        var color = new PropertyValue.ColorValue(0xFF2196F3L);
        WidgetNode widget = new WidgetNode(id, definition.typeId(),
                Map.of(colorName, color, clipperName, preset),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        List<DesignerCommand> commands = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, commands::add);
        for (String name : List.of("clipper", "clipBehavior", "elevation", "color", "shadowColor")) {
            assertTrue(property(node, name).canWrite(), name);
            assertEquals(FlutterPropertyCellValue.class, property(node, name).getValueType());
            assertNotNull(property(node, name).getPropertyEditor(), name);
        }
        var clipper = cellProperty(property(node, "clipper"));
        var surface = cellProperty(property(node, "color"));
        assertFalse(clipper.supportsDefaultValue());
        assertFalse(surface.supportsDefaultValue());
        assertThrows(IllegalArgumentException.class, () -> clipper.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> surface.setValue(FlutterPropertyCellValue.unset()));
        assertTrue(clipper.getPropertyEditor().supportsCustomEditor());
        assertTrue(clipper.getShortDescription().contains("CustomClipper<Path>"));
        assertEquals(List.of(FlutterWidgetPropertiesNode.NOT_SET,
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                List.of(property(node, "clipBehavior").getPropertyEditor().getTags()));
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_pathClipper",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        clipper.setValue(FlutterPropertyCellValue.explicit(reference));
        assertEquals(List.of(new SetProperty(id, clipperName, reference)), commands);
        Node.PropertySet[] sets = node.getPropertySets();
        WidgetNode configured = new WidgetNode(id, definition.typeId(),
                Map.of(colorName, color, clipperName, reference), widget.slots());
        commands.clear();
        node.refreshPresentation(configured, definition, commands::add, null, null,
                FlutterImageAssetChoices.empty());
        assertEquals(List.of(sets), List.of(node.getPropertySets()));
        assertSame(clipper, property(node, "clipper"));
        assertSame(surface, property(node, "color"));
        assertEquals(FlutterPropertyCellValue.explicit(reference), clipper.getValue());
        clipper.setValue(FlutterPropertyCellValue.explicit(preset));
        assertEquals(List.of(new SetProperty(id, clipperName, preset)), commands);
        assertTrue(property(node, "child").getShortDescription().contains("Occupancy: 0/1"));
    }


    private static WidgetDefinition definition(String typeId) {
        return BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(typeId))
                .orElseThrow();
    }

    private static PropertyValue.Matrix4Value identityMatrix() {
        return translatedMatrix("0", "0");
    }

    private static PropertyValue.Matrix4Value translatedMatrix(String dx, String dy) {
        java.util.ArrayList<BigDecimal> storage = new java.util.ArrayList<>(16);
        for (int index = 0; index < 16; index++) {
            storage.add(index % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO);
        }
        storage.set(12, new BigDecimal(dx));
        storage.set(13, new BigDecimal(dy));
        return new PropertyValue.Matrix4Value(storage);
    }

    private static java.util.List<String> names(Node.Property<?>[] properties) {
        return java.util.Arrays.stream(properties).map(Node.Property::getName).toList();
    }

    private static Node.PropertySet propertySet(
            FlutterWidgetPropertiesNode node,
            String name) {
        return Arrays.stream(node.getPropertySets())
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing property set " + name));
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion checksum = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(
                StableId.parse("879efc18-f7d1-457c-90cc-779c5982d85f"),
                new DartSourceDescriptor(
                        "screen.dart",
                        "Screen",
                        WidgetClassKind.STATELESS,
                        java.util.Optional.empty(),
                        new ManagedRegions(checksum, checksum)),
                root);
    }

    private static WidgetNode textWidget(StableId id, String data) {
        return new WidgetNode(
                id,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue(data)),
                Map.of(),
                Extensions.empty());
    }

    private static void assertInlineInsets(
            PropertyEditor editor,
            String source,
            PropertyValue expected) {
        editor.setAsText(source);
        assertEquals(expected, cell(editor).explicitValue().orElseThrow(), source);
        String canonical = editor.getAsText();
        editor.setAsText(canonical);
        assertEquals(expected, cell(editor).explicitValue().orElseThrow(),
                "inline Padding must round-trip through: " + canonical);
    }

    private static PropertyValue.EdgeInsetsValue edge(
            String left, String top, String right, String bottom) {
        return new PropertyValue.EdgeInsetsValue(
                new BigDecimal(left), new BigDecimal(top),
                new BigDecimal(right), new BigDecimal(bottom));
    }

    private static PropertyValue.EdgeInsetsDirectionalValue directionalEdge(
            String start, String top, String end, String bottom) {
        return new PropertyValue.EdgeInsetsDirectionalValue(
                new BigDecimal(start), new BigDecimal(top),
                new BigDecimal(end), new BigDecimal(bottom));
    }

    private static PropertyValue.BoxDecorationValue emptyDecoration() {
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static void assertContainerClipCommand(
            DesignerCommand command,
            boolean coloredPresentation) {
        PatchProperties patch = assertInstanceOf(PatchProperties.class, command);
        List<String> propertyNames = patch.patches().stream()
                .map(item -> item.propertyName().value())
                .toList();
        assertEquals(
                coloredPresentation
                        ? List.of("color", "decoration", "clipBehavior")
                        : List.of("decoration", "clipBehavior"),
                propertyNames,
                "the handler must receive a command derived from its own Widget snapshot");
    }

    private static Node.Property<?> property(
            FlutterWidgetPropertiesNode node,
            String name) {
        return Arrays.stream(node.getPropertySets())
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing property " + name));
    }

    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterPropertyCellValue> cellProperty(
            Node.Property<?> property) {
        assertEquals(FlutterPropertyCellValue.class, property.getValueType());
        return (Node.Property<FlutterPropertyCellValue>) property;
    }

    private static FlutterPropertyCellValue cell(PropertyEditor editor) {
        return assertInstanceOf(
                FlutterPropertyCellValue.class, editor.getValue());
    }

    private static String first(String[] values) {
        if (values == null || values.length == 0) {
            throw new AssertionError("Expected non-empty editor tags");
        }
        return values[0];
    }

    private static String declaredIconPath(Node node) throws ReflectiveOperationException {
        Field base = AbstractNode.class.getDeclaredField("iconBase");
        Field extension = AbstractNode.class.getDeclaredField("iconExtension");
        base.setAccessible(true);
        extension.setAccessible(true);
        return base.get(node) + (String) extension.get(node);
    }

}
