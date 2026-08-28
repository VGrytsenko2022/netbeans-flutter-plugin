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
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
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
                java.util.List.of(
                        "data", "textAlign", "textDirection", "softWrap",
                        "overflow", "maxLines", "semanticsLabel",
                        "semanticsIdentifier", "textWidthBasis", "selectionColor"),
                names(sets[1].getProperties()));
        assertEquals(id.toString(), sets[0].getProperties()[0].getValue());
        assertEquals("flutter.widgets.Text", sets[0].getProperties()[1].getValue());
        assertEquals("\"Hello\\nFlutter\"", sets[1].getProperties()[0].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[1].getValue());
        assertEquals(FlutterWidgetPropertiesNode.NOT_SET, sets[1].getProperties()[2].getValue());
        assertEquals("false", sets[1].getProperties()[3].getValue());
        for (int index = 4; index < sets[1].getProperties().length; index++) {
            assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                    sets[1].getProperties()[index].getValue());
        }
        for (Node.PropertySet set : sets) {
            for (Node.Property<?> property : set.getProperties()) {
                assertFalse(property.canWrite(), property.getName());
            }
        }
    }

    @Test
    void exposesEverySafeCatalogPropertyOnTheFiveNonScaffoldWidgetsAsWritable()
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

            Node.Property<?>[] properties = node.getPropertySets()[1].getProperties();
            assertEquals(definition.properties().size(), properties.length, type);
            for (Node.Property<?> property : properties) {
                assertTrue(property.canWrite(), type + "." + property.getName());
                assertEquals(FlutterPropertyCellValue.class,
                        property.getValueType(), property.getName());
                writableCount++;
            }
        }

        assertEquals(27, writableCount,
                "the reviewed non-Scaffold CORE_V1 surface is an exact closed 27-field contract");
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
    void safePropertiesOnAWidgetOutsideTheReviewedFiveTypeSliceRemainReadOnly() {
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
    void edgeInsetsEditorAcceptsAllOrFromLTRBAndRejectsNegativeInsets()
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
        editor.setAsText("8");
        assertEquals(new PropertyValue.EdgeInsetsValue(
                        BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                        BigDecimal.valueOf(8), BigDecimal.valueOf(8)),
                cell(editor).explicitValue().orElseThrow());
        editor.setAsText("1, 2, 3, 4");
        assertEquals("1, 2, 3, 4", editor.getAsText());
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText("1, -2, 3, 4"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.setAsText(FlutterPropertyCellValue.NOT_SET_TEXT));
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

    private static WidgetNode textWidget(StableId id, String data) {
        return new WidgetNode(
                id,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue(data)),
                Map.of(),
                Extensions.empty());
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
