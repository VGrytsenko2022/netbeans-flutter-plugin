package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageIconContractTest {
    private static final WidgetTypeId TYPE = ImageIconWidgetPropertySchema.IMAGE_ICON_TYPE;
    private static final PropertyName IMAGE = p("image");

    @Test
    void exactPinnedConstructorAndPresentationKeepRequiredNullablePositionalArgumentWithoutFormatBump() {
        var definition = definition();
        assertEquals("ImageIcon", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 230, "ImageIcon"), definition.palette());
        assertEquals(4, ImageIconWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(0, ImageIconWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of("image", "size", "color", "semanticLabel"), definition.properties().stream().map(v -> v.name().value()).toList());
        assertTrue(definition.slots().isEmpty());
        var names = List.of("image", "size", "color", "semanticLabel");
        for (int index = 0; index < names.size(); index++) {
            var property = definition.property(p(names.get(index))).orElseThrow();
            assertEquals(index == 0 ? DartParameter.positional(0) : DartParameter.named(index - 1, false), property.parameter());
            assertEquals(index == 0 ? Optional.of(new PropertyValue.NullValue()) : Optional.empty(), property.creationDefault());
            var hint = ImageIconWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(names.get(index), hint.dartName());
            assertEquals(Math.max(0, index - 1), hint.dartOrder());
            assertFalse(hint.description().isBlank());
        }
        assertEquals("imageIconImage", ImageIconWidgetPropertySchema.find(IMAGE).orElseThrow().group().setName());
        assertEquals("imageIconAppearance", ImageIconWidgetPropertySchema.find(p("size")).orElseThrow().group().setName());
        assertEquals("imageIconAccessibility", ImageIconWidgetPropertySchema.find(p("semanticLabel")).orElseThrow().group().setName());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(IMAGE, new PropertyValue.NullValue()), prototype.properties());
        assertTrue(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(16, DesignerDocument.SCHEMA_VERSION);
        assertEquals(15, WidgetCatalog.API_VERSION);
        assertEquals(19, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void sizeAndThemeColorReuseIconBoundsWhileImageRemainsNonNullable() {
        var icon = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow();
        for (String name : List.of("size", "color", "semanticLabel")) {
            assertEquals(icon.property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints());
        }
        for (var value : List.of(new PropertyValue.IntegerValue(BigInteger.ZERO), d("0.125"), d("1e100"))) assertTrue(accepts("size", value));
        for (var value : List.of(d("-0.1"), d("1e999"), new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE)))) assertFalse(accepts("size", value));
        assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.IMAGE_PROVIDER), definition().property(IMAGE).orElseThrow().acceptedKinds());
        assertFalse(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Image")).orElseThrow().property(IMAGE).orElseThrow().acceptedKinds().contains(PropertyValueKind.NULL));
        assertTrue(accepts("image", new PropertyValue.NullValue()));
        assertFalse(accepts("color", new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))));
    }

    @Test
    void capabilityProjectionLocksNullableRequiredDefaultAndRejectsContractDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES), BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(4, projection.propertyContracts().size());
        assertTrue(projection.slotContracts().isEmpty());
        var image = definition.property(IMAGE).orElseThrow();
        var contract = projection.propertyContracts().get(IMAGE);
        assertTrue(contract.required());
        assertEquals(Optional.of("null"), contract.creationDefaultFingerprint());
        assertEquals(image.acceptedKinds(), contract.acceptedKinds());
        for (var changed : List.of(
                new PropertyDefinition(IMAGE, DartParameter.named(3, true), image.constraints(), image.creationDefault()),
                new PropertyDefinition(IMAGE, DartParameter.named(3, false), image.constraints(), image.creationDefault()),
                new PropertyDefinition(IMAGE, image.parameter(), image.constraints(), Optional.empty()),
                new PropertyDefinition(IMAGE, image.parameter(), image.constraints(), Optional.of(PropertyValue.ImageProviderValue.unresolved())),
                new PropertyDefinition(IMAGE, image.parameter(), List.of(new PropertyValueConstraint.ImageProviderValues()), Optional.empty()),
                new PropertyDefinition(IMAGE, image.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
            var properties = new ArrayList<>(definition.properties());
            properties.set(0, changed);
            assertNoCapability(properties, List.of());
        }
        assertNoCapability(definition.properties().subList(0, 3), List.of());
        assertNoCapability(definition.properties(), List.of(new SlotDefinition(new SlotName("child"), DartParameter.named(3, false), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget())));
    }

    @Test
    void all54ProviderBranchesTimesEightOptionalFieldCombinationsRoundTripAndRetainPositionalConstAndSourceEvidence() throws Exception {
        var codec = new FdDocumentCodec();
        int count = 0;
        for (PropertyValue provider : providers()) for (int mask = 0; mask < 8; mask++) {
            count++;
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(IMAGE, provider);
            if ((mask & 1) != 0) properties.put(p("size"), d("32.5"));
            if ((mask & 2) != 0) properties.put(p("color"), new PropertyValue.ColorValue(0x80123456L));
            if ((mask & 4) != 0) properties.put(p("semanticLabel"), new PropertyValue.StringValue("A '\\' icon"));
            var root = node(properties);
            assertTrue(valid(root));
            var generated = generated(root);
            var source = generated.build().payload();
            boolean unresolved = provider instanceof PropertyValue.ImageProviderValue image && image.isUnresolved();
            assertEquals(!unresolved, source.contains("return const ImageIcon("), source);
            assertFalse(source.contains("image:"), source);
            assertEquals((mask & 1) != 0, source.contains("size: 32.5"), source);
            assertEquals((mask & 2) != 0, source.contains("color: const Color(0x80123456)"), source);
            assertEquals((mask & 4) != 0, source.contains("semanticLabel:"), source);
            if (provider instanceof PropertyValue.NullValue) assertTrue(source.matches("(?s).*ImageIcon\\(\\s*null[,)].*"), source);
            if (provider instanceof PropertyValue.ImageProviderValue image) {
                String owner = image.isUnresolved() ? "MemoryImage" : image.providerKind() == PropertyValue.ImageProviderValue.ProviderKind.ASSET ? "AssetImage" : "ExactAssetImage";
                var occurrence = generated.symbolOccurrences().stream().filter(v -> v.symbolName().equals(owner)).findFirst().orElseThrow();
                assertEquals("/root/properties/image", occurrence.modelPath());
                assertEquals(Optional.of(root.id()), occurrence.widgetId());
                assertEquals(image.resize().isPresent(), source.contains("ResizeImage("), source);
                image.resize().ifPresent(resize -> {
                    assertTrue(source.contains("ResizeImagePolicy." + resize.policy().wireName()), source);
                    assertTrue(source.contains("allowUpscaling: " + resize.allowUpscaling()), source);
                    assertEquals(resize.width().isPresent(), source.contains("width:"), source);
                    assertEquals(resize.height().isPresent(), source.contains("height:"), source);
                });
                assertEquals(image.packageName().isPresent(), source.contains("package: 'reviewed_icons'"), source);
                assertEquals(unresolved, generated.imports().payload().contains("dart:convert"));
            }
            for (var symbol : generated.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
            assertEquals(generated, generated(root));
            var document = document(root);
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(decoded.migrated());
            assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
        assertEquals(432, count);
    }

    @Test
    void explicitNullUsesSdkEmptyIconAndThemeTokenDisablesOnlyNecessaryConstAncestors() {
        var empty = node(Map.of(IMAGE, new PropertyValue.NullValue()));
        var source = generated(empty).build().payload();
        assertTrue(source.contains("return const ImageIcon(null)"), source);
        assertFalse(source.contains("AssetImage"));
        assertFalse(source.contains("MemoryImage"));
        var themed = node(Map.of(IMAGE, new PropertyValue.NullValue(), p("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))));
        var generated = generated(themed);
        assertTrue(generated.build().payload().contains("return ImageIcon("));
        assertTrue(generated.build().payload().contains("Theme.of(context).colorScheme.primary"));
        assertTrue(generated.imports().payload().contains("package:flutter/material.dart"));
        var wrapper = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Center"), Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(themed)));
        assertFalse(generated(wrapper).build().payload().contains("return const Center("));
        var literal = node(Map.of(IMAGE, new PropertyValue.NullValue(), p("color"), new PropertyValue.ColorValue(0)));
        assertTrue(generated(literal).build().payload().contains("return const ImageIcon("));
    }

    @Test
    void requiredImageAndClosedSdkShapeRejectOmissionRawProvidersUnknownFieldsAndSlots() {
        assertFalse(valid(node(Map.of())));
        for (PropertyValue value : List.of(new PropertyValue.StringValue("assets/icon.png"), new PropertyValue.BooleanValue(false),
                new PropertyValue.DartExpressionValue("NetworkImage('https://example.com/icon.png')"), new PropertyValue.DartExpressionValue("FileImage(file)"),
                new PropertyValue.DartExpressionValue("MemoryImage(bytes)"), new PropertyValue.IconDataValue(Optional.empty(), Optional.empty(), Optional.empty(), false, List.of()))) {
            var root = node(Map.of(IMAGE, value));
            assertFalse(valid(root));
            assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
        }
        for (String name : List.of("useOriginalColors", "key", "fit", "blendMode", "opacity", "applyTextScaling", "fill", "shadows", "textDirection", "imageProvider")) {
            assertFalse(valid(node(Map.of(IMAGE, new PropertyValue.NullValue(), p(name), new PropertyValue.BooleanValue(true)))));
            assertTrue(ImageIconWidgetPropertySchema.find(p(name)).isEmpty());
        }
        for (String name : List.of("size", "color", "semanticLabel")) assertFalse(valid(node(Map.of(IMAGE, new PropertyValue.NullValue(), p(name), new PropertyValue.NullValue()))));
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(IMAGE, new PropertyValue.NullValue()), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))));
    }

    @Test
    void providerSafetyRejectsForeignKindsUnsafeNamesUnboundedResizeAndForgedUnresolvedValues() {
        for (String name : List.of("../secret.png", "/absolute.png", "C:\\secret.png", "https://example.com/icon.png")) {
            assertThrows(IllegalArgumentException.class, () -> PropertyValue.ImageProviderValue.asset(name));
        }
        for (String kind : List.of("network", "file", "memory", "custom")) assertThrows(IllegalArgumentException.class, () -> PropertyValue.ImageProviderValue.ProviderKind.fromWireName(kind));
        assertFalse(accepts("image", PropertyValue.ImageProviderValue.exactAsset("assets/icon.png", new BigDecimal("1e999"))));
        assertThrows(IllegalArgumentException.class, () -> PropertyValue.ImageProviderValue.exactAsset("assets/icon.png", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ImageProviderValue.ResizeImageConfig(Optional.empty(), Optional.empty(), PropertyValue.ImageProviderValue.ResizePolicy.EXACT, false));
        for (int size : List.of(-1, 0, 16385)) assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ImageProviderValue.ResizeImageConfig(Optional.of(size), Optional.empty(), PropertyValue.ImageProviderValue.ResizePolicy.FIT, false));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ImageProviderValue(PropertyValue.ImageProviderValue.ProviderKind.ASSET, PropertyValue.ImageProviderValue.unresolved().assetName(), Optional.of("forged"), Optional.empty(), Optional.empty()));
    }

    private static List<PropertyValue> providers() {
        var values = new ArrayList<PropertyValue>();
        values.add(new PropertyValue.NullValue());
        values.add(PropertyValue.ImageProviderValue.unresolved());
        for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) for (boolean packaged : List.of(false, true)) {
            var resizes = new ArrayList<Optional<PropertyValue.ImageProviderValue.ResizeImageConfig>>();
            resizes.add(Optional.empty());
            for (int dimensions = 1; dimensions <= 3; dimensions++) for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) for (boolean upscale : List.of(false, true)) {
                resizes.add(Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig((dimensions & 1) != 0 ? Optional.of(1) : Optional.empty(), (dimensions & 2) != 0 ? Optional.of(16384) : Optional.empty(), policy, upscale)));
            }
            for (var resize : resizes) values.add(new PropertyValue.ImageProviderValue(kind, "assets/icon.png", packaged ? Optional.of("reviewed_icons") : Optional.empty(), kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new BigDecimal("2.5")) : Optional.empty(), resize));
        }
        assertEquals(54, values.size());
        return values;
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) { return new WidgetNode(StableId.random(), TYPE, values, Map.of()); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) { var original = definition(); var changed = new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(), original.importUris(), original.traits(), original.palette(), properties, slots); assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty()); assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty()); }
}
