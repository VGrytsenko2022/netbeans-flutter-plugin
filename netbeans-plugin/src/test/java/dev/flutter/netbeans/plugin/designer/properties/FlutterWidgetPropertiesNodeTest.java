package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.command.DesignerCommand;
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
        assertEquals(List.of("General", "General", "Slots"),
                Arrays.stream(sets)
                        .map(set -> set.getValue(
                                FlutterWidgetPropertiesNode.TAB_NAME_ATTRIBUTE))
                        .toList(),
                "the native PropertySheet must render exactly General and Slots tabs");
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
        Map<String, Map<PropertyName, PropertyValue>> requiredValues = Map.of(
                "flutter.widgets.Padding", Map.of(
                        new PropertyName("padding"),
                        new PropertyValue.EdgeInsetsValue(
                                BigDecimal.valueOf(16), BigDecimal.valueOf(16),
                                BigDecimal.valueOf(16), BigDecimal.valueOf(16))),
                "flutter.widgets.Text", Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue("Hello Flutter")));
        List<String> types = List.of(
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.Text");

        int writableCount = 0;
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
            }
        }

        assertEquals(78, writableCount,
                "the reviewed non-Scaffold surface includes all 59 Text leaves");
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
    void scaffoldRemainsEntirelyReadOnlyEvenWhenAHandlerIsProvided() {
        WidgetDefinition definition = definition("flutter.material.Scaffold");
        WidgetNode widget = WidgetNode.empty(
                StableId.parse("fe1e6841-39d2-4729-87ea-8e40cfdce57f"),
                definition.typeId());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        for (Node.Property<?> property : node.getPropertySets()[1].getProperties()) {
            assertFalse(property.canWrite(), property.getName());
            assertEquals(String.class, property.getValueType());
        }
    }

    @Test
    void propertiesOutsideTheReviewedCapabilitySurfaceRemainReadOnly() {
        WidgetDefinition definition = definition("flutter.widgets.Icon");
        WidgetNode widget = new WidgetNode(
                StableId.parse("b20f626c-e6bd-45d8-aef1-26382ba3ffb6"),
                definition.typeId(),
                Map.of(new PropertyName("icon"),
                        new PropertyValue.DartExpressionValue("Icons.star")),
                Map.of(),
                Extensions.empty());
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF, widget, definition, ignored -> { });

        for (Node.Property<?> property : node.getPropertySets()[1].getProperties()) {
            assertFalse(property.canWrite(), property.getName());
            assertEquals(String.class, property.getValueType());
        }
    }

    @Test
    void booleanAndEnumEditorsExposeExplicitUnsetTagsAndTypedValues()
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
        assertEquals(List.of(FlutterPropertyCellValue.NOT_SET_TEXT, "true", "false"),
                List.of(bool.getTags()));
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
    void sevenCanvasWidgetNodesDeclareTheirMatchingUniqueRegistryIconsWithoutRendering()
            throws ReflectiveOperationException {
        List<String> typeIds = List.of(
                "flutter.material.Scaffold",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
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

        assertEquals(7, iconPaths.size(),
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
