package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultSelectionStyleContractTest {
    private static final WidgetTypeId TYPE = DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE;
    private static final PropertyName CURSOR = new PropertyName("cursorColor");
    private static final PropertyName SELECTION = new PropertyName("selectionColor");
    private static final PropertyName MOUSE = new PropertyName("mouseCursor");
    private static final PropertyName MERGE = new PropertyName("merge");
    private static final SlotName CHILD = new SlotName("child");
    private static final List<PropertyName> NAMES = List.of(CURSOR, SELECTION, MOUSE, MERGE);
    private static final List<Boolean> MODES = List.of(false, true);
    private static final List<Optional<PropertyValue>> COLORS = List.of(Optional.empty(),
            Optional.of(new PropertyValue.ColorValue(0x80123456L)),
            Optional.of(new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))));

    @Test
    void exactThreeOptionalSdkFieldsAndRequiredDesignerModeWithCreationDefaultRequireRealChild() {
        var definition = definition();
        assertEquals("DefaultSelectionStyle", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 210, "DefaultSelectionStyle"), definition.palette());
        assertEquals(NAMES, definition.properties().stream().map(PropertyDefinition::name).toList());
        assertEquals(4, DefaultSelectionStyleWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, DefaultSelectionStyleWidgetPropertySchema.SLOT_COUNT);
        for (int index = 0; index < NAMES.size(); index++) {
            var property = definition.properties().get(index);
            int order = index == 3 ? 4 : index;
            assertEquals(DartParameter.named(order, property.name().equals(MERGE)), property.parameter());
            assertEquals(property.name().equals(MERGE) ? Optional.of(bool(false)) : Optional.empty(),
                    property.creationDefault());
            assertTrue(property.constraints().stream().noneMatch(value -> value.accepts(new PropertyValue.NullValue())));
            var hint = DefaultSelectionStyleWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), hint.dartName());
            assertEquals(order, hint.dartOrder());
            assertEquals("defaultSelectionStyle", hint.group().setName());
        }
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN), definition.property(CURSOR).orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN), definition.property(SELECTION).orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.STRING), definition.property(MOUSE).orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), definition.property(MERGE).orElseThrow().acceptedKinds());
        assertTrue(DefaultSelectionStyleWidgetPropertySchema.find(MERGE).orElseThrow().description().contains("never emitted"));
        assertTrue(DefaultSelectionStyleWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        assertEquals(new SlotDefinition(CHILD, DartParameter.named(3, true), SlotCardinality.SINGLE, 1, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(CHILD).orElseThrow());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(MERGE, bool(false)), prototype.properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(List.of("C|flutter.widgets.DefaultSelectionStyle|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
    }

    @Test
    void all41CursorPresetsUseExactClosedDomainWithoutWideningExistingTextField() {
        var presets = DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
        assertEquals(41, presets.size());
        assertEquals(41, Set.copyOf(presets).size());
        assertEquals(List.of("defer", "uncontrolled", "clickable", "adaptiveClickable", "textable"), presets.subList(36, 41));
        var cursor = assertInstanceOf(PropertyValueConstraint.StringPattern.class,
                definition().property(MOUSE).orElseThrow().constraints().getFirst());
        var oldCursor = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.TextField"))
                .orElseThrow().property(MOUSE).orElseThrow().constraints().getFirst();
        for (int index = 0; index < presets.size(); index++) {
            var value = new PropertyValue.StringValue(presets.get(index));
            assertTrue(cursor.accepts(value));
            assertEquals(index < 36, oldCursor.accepts(value));
        }
        for (String invalid : List.of("", "bogus", "Text", "SystemMouseCursors.text", "MouseCursor.defer",
                "WidgetStateMouseCursor.textable", "text\n", "text; arbitrary()")) {
            assertFalse(cursor.accepts(new PropertyValue.StringValue(invalid)), invalid);
        }
        assertThrows(UnsupportedOperationException.class, () -> presets.add("invented"));
    }

    @Test
    void exactCapabilitiesRejectNullableRequiredDefaultDomainAndStructuralDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Set.copyOf(NAMES), projection.propertyContracts().keySet());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, true, 1, 1)),
                projection.slotContracts());
        for (var property : definition.properties()) {
            var contract = projection.propertyContracts().get(property.name());
            assertEquals(property.name().equals(MERGE), contract.required());
            assertEquals(property.name().equals(MERGE) ? Optional.of("boolean:false") : Optional.empty(),
                    contract.creationDefaultFingerprint());
            assertEquals(property.acceptedKinds(), contract.acceptedKinds());
            PropertyValue defaultValue = property.name().equals(MERGE) ? bool(true)
                    : property.name().equals(MOUSE) ? new PropertyValue.StringValue("text") : new PropertyValue.ColorValue(0L);
            var nullable = new ArrayList<>(property.constraints());
            nullable.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), DartParameter.named(property.parameter().order(), !property.parameter().required()), property.constraints(), property.creationDefault()),
                    new PropertyDefinition(property.name(), property.parameter(), property.constraints(), Optional.of(defaultValue)),
                    new PropertyDefinition(property.name(), property.parameter(), nullable, property.creationDefault()),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
                var properties = new ArrayList<>(definition.properties());
                properties.set(properties.indexOf(property), changed);
                assertNoCapability(properties, definition.slots());
            }
        }
        var missingCreationDefault = new ArrayList<>(definition.properties());
        var mode = definition.property(MERGE).orElseThrow();
        missingCreationDefault.set(3, new PropertyDefinition(MERGE, mode.parameter(), mode.constraints(), Optional.empty()));
        assertNoCapability(missingCreationDefault, definition.slots());
        var changed = new ArrayList<>(definition.properties());
        changed.set(2, new PropertyDefinition(MOUSE, DartParameter.named(2, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)), Optional.empty()));
        assertNoCapability(changed, definition.slots());
        assertNoCapability(definition.properties().subList(0, 3), definition.slots());
        assertNoCapability(definition.properties(), List.of());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(3, false), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(3, true), SlotCardinality.LIST, 1, 1, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(slot));
        }
    }

    @Test
    void all756ModeColorCursorCombinationsGenerateExactConstFactorySymbolsAndRoundTrip() throws Exception {
        var cursorStates = new ArrayList<Optional<String>>();
        cursorStates.add(Optional.empty());
        DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets().forEach(value -> cursorStates.add(Optional.of(value)));
        var codec = new FdDocumentCodec();
        int combinations = 0;
        for (var mode : MODES) for (var cursorColor : COLORS) for (var selectionColor : COLORS) for (var mouse : cursorStates) {
            combinations++;
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(MERGE, bool(mode));
            cursorColor.ifPresent(value -> properties.put(CURSOR, value));
            selectionColor.ifPresent(value -> properties.put(SELECTION, value));
            mouse.ifPresent(value -> properties.put(MOUSE, new PropertyValue.StringValue(value)));
            var root = node(properties, text(Map.of()));
            var document = document(root);
            assertTrue(valid(root));
            var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
            assertTrue(result.successful(), result.diagnostics().toString());
            var generated = result.generated().orElseThrow();
            assertEquals(generated, new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow());
            String build = generated.build().payload();
            boolean merge = mode;
            boolean theme = cursorColor.filter(PropertyValue.ThemeTokenValue.class::isInstance).isPresent()
                    || selectionColor.filter(PropertyValue.ThemeTokenValue.class::isInstance).isPresent();
            assertEquals(!merge && !theme, build.contains("return const DefaultSelectionStyle("), build);
            assertEquals(merge, build.contains("DefaultSelectionStyle.merge("), build);
            assertFalse(build.contains("const DefaultSelectionStyle.merge("), build);
            assertFalse(build.contains("merge:"), build);
            assertEquals(cursorColor.isPresent(), build.contains("cursorColor:"), build);
            assertEquals(selectionColor.isPresent(), build.contains("selectionColor:"), build);
            assertEquals(mouse.isPresent(), build.contains("mouseCursor:"), build);
            assertFalse(build.contains("cursorColor: null"), build);
            assertFalse(build.contains("selectionColor: null"), build);
            assertFalse(build.contains("mouseCursor: null"), build);
            assertFalse(build.contains("key:"), build);
            assertEquals(theme, generated.imports().payload().contains("package:flutter/material.dart"));
            String library = theme ? "package:flutter/material.dart" : "package:flutter/widgets.dart";
            for (String name : List.of("DefaultSelectionStyle", "Text")) {
                var symbol = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals(name)).findFirst().orElseThrow();
                assertEquals(library, symbol.libraryUri());
                assertEquals(name, build.substring(symbol.offset(), symbol.endOffset()));
            }
            var mergeSymbol = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("merge")).findFirst();
            assertEquals(merge, mergeSymbol.isPresent());
            mergeSymbol.ifPresent(value -> {
                assertEquals("merge", build.substring(value.offset(), value.endOffset()));
                assertEquals(Optional.of(root.id()), value.widgetId());
                assertEquals(library, value.libraryUri());
            });
            mouse.ifPresent(value -> {
                String owner = List.of("defer", "uncontrolled").contains(value) ? "MouseCursor"
                        : List.of("clickable", "adaptiveClickable", "textable").contains(value) ? "WidgetStateMouseCursor" : "SystemMouseCursors";
                assertTrue(build.contains("mouseCursor: " + owner + "." + value), build);
                assertTrue(build.indexOf("mouseCursor:") < build.indexOf("child:"), build);
                var symbol = generated.symbolOccurrences().stream().filter(item -> item.symbolName().equals(owner)).findFirst().orElseThrow();
                assertEquals(owner, build.substring(symbol.offset(), symbol.endOffset()));
                assertEquals(library, symbol.libraryUri());
                assertEquals(Optional.of(root.id()), symbol.widgetId());
                var member = generated.symbolOccurrences().stream().filter(item -> item.symbolName().equals(value)).findFirst().orElseThrow();
                assertEquals(value, build.substring(member.offset(), member.endOffset()));
                assertEquals(library, member.libraryUri());
                assertEquals(Optional.of(root.id()), member.widgetId());
            });
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(decoded.migrated());
            assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
        assertEquals(756, combinations);
    }

    @Test
    void requiredDirectAndMergeModesNeverInferAmbientFieldsAndMissingModeFailsClosed() throws Exception {
        var child = text(Map.of());
        var direct = node(Map.of(), child);
        assertEquals(Map.of(MERGE, bool(false)), direct.properties());
        var missing = new WidgetNode(direct.id(), TYPE, Map.of(), direct.slots());
        assertFalse(valid(missing));
        assertFalse(new DartRegionGenerator().generate(document(missing), BuiltInWidgetCatalog.getDefault()).successful());
        var outer = node(Map.of(CURSOR, new PropertyValue.ColorValue(0xFF123456L),
                SELECTION, new PropertyValue.ColorValue(0L), MOUSE, new PropertyValue.StringValue("click")), direct);
        String source = build(outer);
        assertEquals(1, source.split("cursorColor:", -1).length - 1);
        assertEquals(1, source.split("selectionColor:", -1).length - 1);
        assertEquals(1, source.split("mouseCursor:", -1).length - 1);
        assertEquals(2, source.split("DefaultSelectionStyle\\(", -1).length - 1);
        var inherited = new WidgetNode(direct.id(), TYPE, Map.of(MERGE, bool(true)), direct.slots());
        String merged = build(node(outer.properties(), inherited));
        assertTrue(merged.contains("return DefaultSelectionStyle("), merged);
        assertTrue(merged.contains("DefaultSelectionStyle.merge("), merged);
        assertEquals(1, merged.split("cursorColor:", -1).length - 1);
        assertEquals(1, merged.split("selectionColor:", -1).length - 1);
    }

    @Test
    void mergeAndNonConstDescendantsPreventAncestorConstButKeepConstChildAndLiteralColors() {
        var direct = node(Map.of(CURSOR, new PropertyValue.ColorValue(0L), SELECTION, new PropertyValue.ColorValue(0xFFFFFFFFL)), text(Map.of()));
        assertTrue(build(direct).contains("return const DefaultSelectionStyle("));
        assertTrue(build(direct).contains("cursorColor: const Color(0x00000000)"));
        assertTrue(build(direct).contains("selectionColor: const Color(0xFFFFFFFF)"));
        var merge = node(Map.of(MERGE, bool(true)), text(Map.of()));
        var center = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Center"), Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(merge)));
        assertTrue(build(center).contains("return Center("));
        assertFalse(build(center).contains("const Center("));
        assertTrue(build(center).contains("child: const Text('Selection child')"));
        var dynamicChild = text(Map.of(new PropertyName("styleColor"), COLORS.get(2).orElseThrow()));
        assertTrue(build(node(Map.of(MERGE, bool(false)), dynamicChild)).contains("return DefaultSelectionStyle("));
        assertFalse(build(node(Map.of(), dynamicChild)).contains("const DefaultSelectionStyle("));
    }

    @Test
    void repeatedThemeRoleOnExistingTextAndNewSelectionFieldsRetainsDistinctPropertyProvenance() {
        PropertyValue theme = COLORS.get(2).orElseThrow();
        var child = text(Map.of(new PropertyName("styleColor"), theme, new PropertyName("styleDecorationColor"), theme));
        var root = node(Map.of(CURSOR, theme, SELECTION, theme), child);
        var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        var generated = result.generated().orElseThrow();
        var occurrences = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("Theme")).toList();
        assertEquals(4, occurrences.size());
        assertEquals(4, occurrences.stream().map(value -> value.id()).distinct().count());
        assertEquals(4, occurrences.stream().map(value -> value.modelPath()).distinct().count());
        assertEquals(4, generated.build().payload().split("Theme.of\\(context\\).colorScheme.primary", -1).length - 1);
        for (var occurrence : occurrences) {
            assertEquals("Theme", generated.build().payload().substring(occurrence.offset(), occurrence.endOffset()));
            assertTrue(occurrence.id().endsWith(occurrence.modelPath()));
        }
    }

    @Test
    void invalidKindsTokensCursorsRawHelpersAndMissingRequiredChildFailClosed() {
        for (var name : NAMES) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("invalid"),
                    new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DartExpressionValue("DefaultSelectionStyle.merge(child: child)"))) {
                var root = node(Map.of(name, invalid), text(Map.of()));
                assertFalse(valid(root));
                assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
            }
        }
        for (var name : List.of(CURSOR, SELECTION)) {
            assertFalse(valid(node(Map.of(name, new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))), text(Map.of()))));
            assertFalse(valid(node(Map.of(name, bool(false)), text(Map.of()))));
        }
        for (String name : List.of("key", "fallback", "inherit", "defaultColor")) {
            assertFalse(valid(node(Map.of(new PropertyName(name), bool(true)), text(Map.of()))));
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text(Map.of())))))) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(MERGE, bool(false)), slots)));
        }
        assertTrue(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.DefaultSelectionStyle.fallback")).isEmpty());
        assertTrue(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.DefaultSelectionStyle.merge")).isEmpty());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var original = definition();
        var copy = new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(),
                original.importUris(), original.traits(), original.palette(), properties, slots);
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(copy).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(copy).isEmpty());
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static String build(WidgetNode root) { return new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload(); }
    private static WidgetNode text(Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(new PropertyName("data"), new PropertyValue.StringValue("Selection child"));
        properties.putAll(values);
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), properties, Map.of());
    }
    private static WidgetNode node(Map<PropertyName, PropertyValue> properties, WidgetNode child) {
        var complete = new LinkedHashMap<PropertyName, PropertyValue>();
        complete.put(MERGE, bool(false));
        complete.putAll(properties);
        return new WidgetNode(StableId.random(), TYPE, complete, Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}
