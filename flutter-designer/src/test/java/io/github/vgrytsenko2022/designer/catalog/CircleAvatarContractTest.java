package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircleAvatarContractTest {
    private static final WidgetTypeId TYPE = CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE;
    private static final PropertyValue.EnumValue INFINITY = CircleAvatarWidgetPropertySchema.POSITIVE_INFINITY;
    private static final List<String> NAMES = List.of("backgroundColor", "backgroundImage", "foregroundImage",
            "onBackgroundImageError", "onForegroundImageError", "foregroundColor", "radius", "minRadius", "maxRadius");

    @Test
    void exactPinnedConstructorHasNineOptionalFieldsOptionalChildAndNoStoredDefaults() {
        var definition = definition();
        assertEquals("CircleAvatar", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.traits().isEmpty());
        assertEquals(List.of("dart:core", "package:flutter/material.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.material", 100, 90, "CircleAvatar"), definition.palette());
        assertEquals(9, CircleAvatarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, CircleAvatarWidgetPropertySchema.SLOT_COUNT);
        assertEquals(NAMES, definition.properties().stream().map(v -> v.name().value()).toList());
        assertEquals(new LinkedHashSet<>(NAMES), CircleAvatarWidgetPropertySchema.definitions().keySet());
        for (int index = 0; index < NAMES.size(); index++) {
            var property = definition.property(p(NAMES.get(index))).orElseThrow();
            assertEquals(DartParameter.named(index + 1, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            var hint = CircleAvatarWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(NAMES.get(index), hint.dartName());
            assertEquals(index + 1, hint.dartOrder());
            assertFalse(hint.description().isBlank());
        }
        var slot = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(0, false), slot.parameter());
        assertEquals(0, slot.minChildren());
        assertEquals(1, slot.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, slot.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertTrue(valid(prototype));
        assertTrue(generated(prototype).build().payload().contains("return const CircleAvatar("));
        assertFalse(generated(prototype).imports().payload().contains("dart:core"));
        assertFalse(generated(prototype).build().payload().contains("Image("));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(definition));
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void radiusUnionAdmitsOnlyPortableNonnegativeNumbersAndCanonicalPositiveInfinity() {
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            var property = definition().property(p(name)).orElseThrow();
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE, PropertyValueKind.ENUM), property.acceptedKinds());
            for (var value : List.of(i(0), i(42), d("0.125"), d("1e308"), INFINITY)) {
                assertTrue(accepts(name, value), name + ": " + value);
                assertTrue(valid(node(Map.of(p(name), value))));
            }
            for (var value : List.of(i(-1), d("-0.01"), d("1e999"), d("1e-999"),
                    new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE)),
                    new PropertyValue.EnumValue("double", "nan"), new PropertyValue.EnumValue("double", "negativeInfinity"),
                    new PropertyValue.EnumValue("other.double", "infinity"), new PropertyValue.StringValue("Infinity"),
                    new PropertyValue.DartExpressionValue("double.infinity"), new PropertyValue.NullValue())) {
                assertFalse(accepts(name, value), name + ": " + value);
                assertFalse(valid(node(Map.of(p(name), value))));
            }
            assertTrue(CircleAvatarWidgetPropertySchema.isRadiusProperty(p(name)));
        }
        assertFalse(CircleAvatarWidgetPropertySchema.isRadiusProperty(p("backgroundColor")));
    }

    @Test
    void all125RadiusPresenceAndInfinityCombinationsMatchActualSdkDiameterNormalization() {
        var values = Arrays.asList(null, i(0), d("20.5"), d("1e308"), INFINITY);
        int cases = 0;
        for (var radius : values) for (var minimum : values) for (var maximum : values) {
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            if (radius != null) properties.put(p("radius"), radius);
            if (minimum != null) properties.put(p("minRadius"), minimum);
            if (maximum != null) properties.put(p("maxRadius"), maximum);
            boolean expected = (radius == null || minimum == null && maximum == null)
                    && diameter(minimum, 0) <= diameter(maximum, Double.POSITIVE_INFINITY);
            assertEquals(expected, valid(node(properties)), properties.toString());
            cases++;
        }
        assertEquals(125, cases);
        assertTrue(valid(node(Map.of(p("minRadius"), d("1.1e308"), p("maxRadius"), d("1e308")))));
        assertTrue(valid(node(Map.of(p("minRadius"), INFINITY, p("maxRadius"), d("1e308")))));
        assertFalse(valid(node(Map.of(p("minRadius"), d("20.5"), p("maxRadius"), i(20)))));
    }

    @Test
    void infinityGenerationUsesFixedConstArithmeticWithoutCoreScopeOrFabricatedEvidence() throws Exception {
        var codec = new FdDocumentCodec();
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            var root = node(Map.of(p(name), INFINITY));
            var generated = generated(root);
            String source = generated.build().payload();
            assertTrue(source.contains("return const CircleAvatar("), source);
            assertTrue(source.contains(name + ": (1.0 / 0.0)"), source);
            assertFalse(generated.imports().payload().contains("dart:core"));
            assertTrue(generated.symbolOccurrences().stream().noneMatch(v -> v.libraryUri().equals("dart:core")));
            for (var symbol : generated.symbolOccurrences()) {
                assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
            }
            var document = document(root);
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
            assertTrue(new String(bytes.copyBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("infinity"));
        }
    }

    @Test
    void all53ProviderBranchesOnBothLayersWithAndWithoutCallbacksRoundTripAndRespectConst() throws Exception {
        var codec = new FdDocumentCodec();
        int count = 0;
        for (var provider : providers()) for (String layer : List.of("Background", "Foreground")) for (boolean callback : List.of(false, true)) {
            String imageName = Character.toLowerCase(layer.charAt(0)) + layer.substring(1) + "Image";
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(p(imageName), provider);
            if (callback) properties.put(p("on" + layer + "ImageError"), new PropertyValue.CallbackValue("handle" + layer + "Error"));
            var root = node(properties);
            assertTrue(valid(root));
            var generated = generated(root);
            var source = generated.build().payload();
            assertEquals(!provider.isUnresolved() && !callback, source.contains("return const CircleAvatar("), source);
            String owner = provider.isUnresolved() ? "MemoryImage" : provider.providerKind() == PropertyValue.ImageProviderValue.ProviderKind.ASSET ? "AssetImage" : "ExactAssetImage";
            var occurrence = generated.symbolOccurrences().stream().filter(v -> v.symbolName().equals(owner)).findFirst().orElseThrow();
            assertEquals("/root/properties/" + imageName, occurrence.modelPath());
            assertEquals(Optional.of(root.id()), occurrence.widgetId());
            assertEquals(provider.resize().isPresent(), source.contains("ResizeImage("));
            assertEquals(provider.packageName().isPresent(), source.contains("package: 'reviewed_icons'"));
            assertEquals(provider.isUnresolved(), generated.imports().payload().contains("dart:convert"));
            assertFalse(generated.imports().payload().contains("dart:core"));
            for (var symbol : generated.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
            var document = document(root);
            assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
            count++;
        }
        assertEquals(212, count);
    }

    @Test
    void callbacksRequireMatchingImageAndProvidersRemainClosedNonNullableAssetValues() {
        for (String layer : List.of("Background", "Foreground")) {
            String imageName = Character.toLowerCase(layer.charAt(0)) + layer.substring(1) + "Image";
            String otherName = layer.equals("Background") ? "foregroundImage" : "backgroundImage";
            var callback = new PropertyValue.CallbackValue("onImageError");
            assertFalse(valid(node(Map.of(p("on" + layer + "ImageError"), callback))));
            assertFalse(valid(node(Map.of(p("on" + layer + "ImageError"), callback, p(otherName), PropertyValue.ImageProviderValue.asset("assets/photo.png")))));
            assertTrue(valid(node(Map.of(p("on" + layer + "ImageError"), callback, p(imageName), PropertyValue.ImageProviderValue.asset("assets/photo.png")))));
            assertEquals(Set.of(PropertyValueKind.IMAGE_PROVIDER), definition().property(p(imageName)).orElseThrow().acceptedKinds());
            for (var value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("assets/photo.png"),
                    new PropertyValue.DartExpressionValue("NetworkImage('https://example.com/photo.png')"),
                    new PropertyValue.DartExpressionValue("MemoryImage(bytes)"))) assertFalse(valid(node(Map.of(p(imageName), value))));
            for (var value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("onImageError"),
                    new PropertyValue.DartExpressionValue("(error, stack) {}"))) assertFalse(accepts("on" + layer + "ImageError", value));
        }
        for (String name : List.of("textStyle", "clipBehavior", "image", "fit", "key", "semanticLabel")) {
            assertFalse(valid(node(Map.of(p(name), new PropertyValue.BooleanValue(true)))));
            assertTrue(CircleAvatarWidgetPropertySchema.find(p(name)).isEmpty());
        }
    }

    @Test
    void allNineFieldsInTwoLegalRadiusModesKeepThemePathsAndChildConstPropagation() {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        var token = new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        properties.put(p("backgroundColor"), token);
        properties.put(p("foregroundColor"), token);
        properties.put(p("backgroundImage"), PropertyValue.ImageProviderValue.asset("assets/photo.png"));
        properties.put(p("foregroundImage"), PropertyValue.ImageProviderValue.exactAsset("assets/front.png", new BigDecimal("2")));
        properties.put(p("onBackgroundImageError"), new PropertyValue.CallbackValue("backgroundError"));
        properties.put(p("onForegroundImageError"), new PropertyValue.CallbackValue("foregroundError"));
        properties.put(p("minRadius"), i(12));
        properties.put(p("maxRadius"), INFINITY);
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue("AB")), Map.of());
        var root = node(properties, Optional.of(child));
        var generated = generated(root);
        var source = generated.build().payload();
        assertFalse(source.contains("return const CircleAvatar("));
        assertTrue(source.contains("child: const Text('AB')"), source);
        assertTrue(source.indexOf("child:") < source.indexOf("backgroundColor:"));
        for (String name : List.of("backgroundColor", "foregroundColor")) {
            assertTrue(generated.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals("Theme") && v.modelPath().equals("/root/properties/" + name)));
        }
        properties.remove(p("minRadius"));
        properties.remove(p("maxRadius"));
        properties.put(p("radius"), d("24.5"));
        assertTrue(generated(node(properties, Optional.of(child))).build().payload().contains("radius: 24.5"));
        var literal = node(Map.of(p("backgroundColor"), new PropertyValue.ColorValue(0xff123456L), p("foregroundColor"), new PropertyValue.ColorValue(0xffffffffL)), Optional.of(child));
        assertTrue(generated(literal).build().payload().contains("return const CircleAvatar("));
    }

    @Test
    void capabilityProjectionRejectsInfinityBindingDefaultRequiredAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES), BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(9, projection.propertyContracts().size());
        assertEquals(1, projection.slotContracts().size());
        for (String name : List.of("radius", "minRadius", "maxRadius")) {
            var property = definition.property(p(name)).orElseThrow();
            var constraint = property.constraints().stream().filter(PropertyValueConstraint.EnumValues.class::isInstance).map(PropertyValueConstraint.EnumValues.class::cast).findFirst().orElseThrow();
            assertEquals(new DartSymbolReference("dart:core", "double"), constraint.dartType());
            var withoutEnum = property.constraints().stream().filter(v -> !(v instanceof PropertyValueConstraint.EnumValues)).toList();
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), property.parameter(), withoutEnum, Optional.empty()),
                    new PropertyDefinition(property.name(), DartParameter.named(property.parameter().order(), true), property.constraints(), Optional.empty()),
                    new PropertyDefinition(property.name(), property.parameter(), property.constraints(), Optional.of(INFINITY)))) {
                var properties = new ArrayList<>(definition.properties());
                properties.set(NAMES.indexOf(name), changed);
                assertNoCapability(properties, definition.slots());
            }
            assertFalse(projection.propertyContracts().get(p(name)).required());
            assertTrue(projection.propertyContracts().get(p(name)).creationDefaultFingerprint().isEmpty());
        }
        assertNoCapability(definition.properties(), List.of());
        assertNoCapability(definition.properties().subList(0, 8), definition.slots());
    }

    @Test
    void childIsOptionalButStillRejectsIncompatibleParentDataAndUnexpectedSlots() {
        assertTrue(valid(node(Map.of())));
        var expanded = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Spacer")).orElseThrow(), StableId.random());
        assertFalse(valid(node(Map.of(), Optional.of(expanded))));
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())))));
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(new SlotName("child"), new WidgetSlot.ListSlot(List.of())))));
    }

    @Test
    void circleAvatarImportExceptionDoesNotSuppressContributedCoreEnumImports() {
        var contributedType = new WidgetTypeId("contributed.widgets.CoreConstantWidget");
        var contributed = new WidgetDefinition(contributedType, "CoreConstantWidget", Optional.empty(), true,
                "package:contributed/widgets.dart", List.of("package:contributed/widgets.dart", "dart:core"),
                Set.of(), new PaletteMetadata("contributed", 1000, 10, "Core constant"),
                List.of(new PropertyDefinition(p("value"), DartParameter.named(0, false),
                        List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"),
                                List.of("maxFinite"))), Optional.empty())), List.of());
        var definitions = new ArrayList<>(BuiltInWidgetCatalog.getDefault().definitions());
        definitions.add(contributed);
        var catalog = WidgetCatalog.strict(definitions);
        var contributedNode = new WidgetNode(StableId.random(), contributedType,
                Map.of(p("value"), new PropertyValue.EnumValue("double", "maxFinite")), Map.of());
        var mixed = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        contributedNode, node(Map.of(p("radius"), INFINITY))))));
        for (var root : List.of(contributedNode, mixed)) {
            var result = new DartRegionGenerator().generate(document(root), catalog);
            assertTrue(result.successful(), result.diagnostics().toString());
            var generated = result.generated().orElseThrow();
            assertTrue(generated.imports().payload().contains("import 'dart:core' as _nbfd_"));
            assertTrue(generated.build().payload().contains(".double.maxFinite"));
            var evidence = generated.symbolOccurrences().stream()
                    .filter(value -> value.libraryUri().equals("dart:core")).toList();
            assertEquals(1, evidence.size());
            assertEquals("double", evidence.getFirst().symbolName());
            assertEquals(Optional.of(contributedNode.id()), evidence.getFirst().widgetId());
            assertEquals("double", generated.build().payload().substring(
                    evidence.getFirst().offset(), evidence.getFirst().endOffset()));
        }
        assertFalse(generated(node(Map.of(p("radius"), INFINITY))).imports().payload().contains("dart:core"));
    }

    private static List<PropertyValue.ImageProviderValue> providers() {
        var values = new ArrayList<PropertyValue.ImageProviderValue>();
        values.add(PropertyValue.ImageProviderValue.unresolved());
        for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) for (boolean packaged : List.of(false, true)) {
            var resizes = new ArrayList<Optional<PropertyValue.ImageProviderValue.ResizeImageConfig>>();
            resizes.add(Optional.empty());
            for (int dimensions = 1; dimensions <= 3; dimensions++) for (var policy : PropertyValue.ImageProviderValue.ResizePolicy.values()) for (boolean upscale : List.of(false, true)) {
                resizes.add(Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig((dimensions & 1) != 0 ? Optional.of(1) : Optional.empty(), (dimensions & 2) != 0 ? Optional.of(16384) : Optional.empty(), policy, upscale)));
            }
            for (var resize : resizes) values.add(new PropertyValue.ImageProviderValue(kind, "assets/icon.png", packaged ? Optional.of("reviewed_icons") : Optional.empty(), kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new BigDecimal("2.5")) : Optional.empty(), resize));
        }
        assertEquals(53, values.size());
        return values;
    }

    private static double diameter(PropertyValue value, double omitted) {
        if (value == null) return omitted;
        if (value instanceof PropertyValue.IntegerValue integer) return 2.0 * integer.value().doubleValue();
        if (value instanceof PropertyValue.DoubleValue decimal) return 2.0 * decimal.value().doubleValue();
        return Double.POSITIVE_INFINITY;
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) { return node(values, Optional.empty()); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values, Optional<WidgetNode> child) { return new WidgetNode(StableId.random(), TYPE, values, Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child))); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) { var original = definition(); var changed = new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(), original.importUris(), original.traits(), original.palette(), properties, slots); assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty()); assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty()); }
}
