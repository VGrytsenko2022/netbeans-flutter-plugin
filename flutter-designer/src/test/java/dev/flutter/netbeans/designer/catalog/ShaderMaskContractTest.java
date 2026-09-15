package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShaderMaskContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    static final WidgetTypeId TYPE = ShaderMaskWidgetPropertySchema.TYPE;
    static final PropertyName CALLBACK = new PropertyName("shaderCallback");
    public static WidgetNode node() { return WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(), StableId.random()); }
    static DesignerDocument doc(WidgetNode n) { return RotationTransitionContractTest.document(n); }
    static WidgetNode with(WidgetNode n, String name, PropertyValue value) {
        var values = new LinkedHashMap<>(n.properties()); values.put(new PropertyName(name), value);
        return new WidgetNode(n.id(), n.type(), values, n.slots());
    }
    static GeneratedDartRegions generated(WidgetNode n) {
        var result = new DartRegionGenerator().generate(doc(n), C);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    public static PropertyValue.GradientValue gradient(String family, boolean directional, PropertyValue.BoxDecorationValue.TileMode tile, boolean theme) {
        var basis = directional ? PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL : PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL;
        var begin = new PropertyValue.AlignmentGeometryValue(basis, BigDecimal.valueOf(-1), BigDecimal.ZERO);
        var end = new PropertyValue.AlignmentGeometryValue(basis, BigDecimal.ONE, BigDecimal.ZERO);
        var center = new PropertyValue.AlignmentGeometryValue(basis, BigDecimal.ZERO, BigDecimal.ZERO);
        var stops = List.of(
                new PropertyValue.BoxDecorationValue.GradientStop(StableId.random(), theme ? new ColorSource.Theme(new ThemeToken("material.colorScheme.primary")) : new ColorSource.Literal(0xffff0000L), BigDecimal.ZERO),
                new PropertyValue.BoxDecorationValue.GradientStop(StableId.random(), new ColorSource.Literal(0x8000ff00L), new BigDecimal(".5")),
                new PropertyValue.BoxDecorationValue.GradientStop(StableId.random(), new ColorSource.Literal(0x000000ffL), BigDecimal.ONE));
        var rotation = Optional.of(new BigDecimal("-.25"));
        return new PropertyValue.GradientValue(switch (family) {
            case "linear" -> new PropertyValue.BoxDecorationValue.LinearGradient(begin, end, stops, tile, rotation);
            case "radial" -> new PropertyValue.BoxDecorationValue.RadialGradient(center, new BigDecimal(".75"), Optional.of(begin), new BigDecimal(".05"), stops, tile, rotation);
            case "sweep" -> new PropertyValue.BoxDecorationValue.SweepGradient(center, new BigDecimal("-.5"), new BigDecimal("5.5"), stops, tile, rotation);
            default -> throw new IllegalArgumentException(family);
        });
    }
    @Test void exactConstructorAndComputationDelegate() {
        var d = C.find(TYPE).orElseThrow();
        assertTrue(d.constConstructor()); assertEquals(List.of("shaderCallback", "blendMode"), d.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(DartParameter.named(2, false), d.slots().getFirst().parameter()); assertEquals(0, d.slots().getFirst().minChildren());
        assertEquals(Set.of(CALLBACK), node().properties().keySet());
        var e = WidgetEventCatalog.eventsFor(d).getFirst();
        assertEquals(WidgetEventDescriptor.Kind.DELEGATE, e.kind()); assertTrue(e.supportsHandlerActions());
        assertTrue(e.required()); assertTrue(e.sdkRequired()); assertFalse(e.nullableCallback()); assertFalse(e.allowsExplicitNull()); assertFalse(e.defaultEvent());
        assertEquals("Shader Function(Rect)", e.signature().dartFunctionType());
        assertTrue(e.createStub("_mask").contains(".createShader(bounds)")); assertFalse(e.createStub("_mask").contains("UnimplementedError"));
        assertEquals(Optional.of(ShaderMaskWidgetPropertySchema.neutral()), e.creationDefault());
        var plain = C.find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow();
        for (var owner : C.definitions()) for (var slot : owner.slots())
            assertEquals(WidgetPlacementRules.accepts(owner, slot, plain), WidgetPlacementRules.accepts(owner, slot, d));
        for (var child : C.definitions()) assertEquals(WidgetPlacementRules.accepts(plain, plain.slots().getFirst(), child), WidgetPlacementRules.accepts(d, d.slots().getFirst(), child));
    }
    @Test void allLocalGradientsBlendModesAndCodecKeepNativeGeometry() throws Exception {
        var codec = new FdDocumentCodec();
        for (String family : List.of("linear", "radial", "sweep")) for (boolean direction : List.of(false,true))
        for (var tile : PropertyValue.BoxDecorationValue.TileMode.values()) for (boolean theme : List.of(false,true)) {
            var value = gradient(family, direction, tile, theme);
            var n = with(node(), "shaderCallback", value);
            var document = doc(n);
            var round = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document)));
            assertEquals(document, round.document()); assertFalse(round.migrated());
            var source = generated(n).build().payload();
            assertTrue(source.contains("Builder("), source);
            assertTrue(source.contains("Directionality.maybeOf(context)"), source);
            assertTrue(source.contains(".createShader(bounds, textDirection:"), source);
            assertTrue(source.contains("GradientRotation(-0.25)"), source);
            assertEquals(direction, source.contains("AlignmentDirectional("), source);
            assertEquals(theme, source.contains("Theme.of(context)"), source);
            assertFalse(source.contains("const ShaderMask("), source);
            assertFalse(source.contains("blendMode:"), source);
        }
        for (var blend : PropertyValue.PaintValue.BlendMode.values())
            assertTrue(generated(with(node(), "blendMode", new PropertyValue.EnumValue("BlendMode", blend.wireName()))).build().payload().contains("BlendMode." + blend.wireName()));
    }
    @Test void allEightSourceFormsUseExactProofAndNoLocalWrapper() {
        for (boolean imported : List.of(false,true)) for (boolean member : List.of(false,true)) for (boolean factory : List.of(false,true)) {
            var ref = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:sample/values.dart") : Optional.empty(),
                    member ? "Values" : "shader", member ? Optional.of("shader") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            var out = generated(with(node(), "shaderCallback", ref));
            assertFalse(out.build().payload().contains("Builder("));
            assertTrue(out.symbolOccurrences().stream().flatMap(o -> o.staticTypeRequirement().stream()).anyMatch(t -> t.expectedDartType().equals("ShaderCallback")));
        }
    }
    @Test void rejectsMissingCallbackWrongKindsAndNonFiniteGradientGeometry() {
        for (var bad : List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.StringValue("raw code"),new PropertyValue.CallbackValue("_untyped")))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),"shaderCallback",bad)),C).valid());
        var n = node(); assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
        var g = (PropertyValue.BoxDecorationValue.LinearGradient) ShaderMaskWidgetPropertySchema.neutral().gradient();
        var huge = new PropertyValue.GradientValue(new PropertyValue.BoxDecorationValue.LinearGradient(
            new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new BigDecimal("1e309"),BigDecimal.ZERO),
            g.end(),g.stops(),g.tileMode(),Optional.empty()));
        assertFalse(new WidgetTreeValidator().validate(doc(with(n,"shaderCallback",huge)),C).valid());
    }
    @Test void version16MigratesWithoutChangingGeneratedDartAndCannotSmuggleGradient() throws Exception {
        var codec = new FdDocumentCodec();
        var baseline = doc(RotationTransitionContractTest.adapter());
        var encoded = new String(codec.encode(baseline).copyBytes(), StandardCharsets.UTF_8);
        var legacy = encoded.replace("\"schemaVersion\": 17", "\"schemaVersion\": 16");
        assertNotEquals(encoded,legacy);
        var decoded = assertInstanceOf(FdDecodeResult.Current.class,codec.decode(legacy.getBytes(StandardCharsets.UTF_8)));
        assertTrue(decoded.migrated()); assertEquals(16,decoded.sourceSchemaVersion());
        assertEquals(new DartRegionGenerator().generate(baseline,C).generated().orElseThrow().build().payload(),
                new DartRegionGenerator().generate(decoded.document(),C).generated().orElseThrow().build().payload());
        var gradient = new String(codec.encode(doc(node())).copyBytes(),StandardCharsets.UTF_8).replace("\"schemaVersion\": 17", "\"schemaVersion\": 16");
        assertFalse(codec.decode(gradient.getBytes(StandardCharsets.UTF_8)) instanceof FdDecodeResult.Current);
    }
}
