package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomPaintContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    static final WidgetTypeId TYPE = CustomPaintWidgetPropertySchema.TYPE;
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
    @Test void exactConstructorOptionalChildAndNoInventedEvents() {
        var d = C.find(TYPE).orElseThrow();
        assertTrue(d.constConstructor());
        assertEquals(List.of("painter", "foregroundPainter", "size", "isComplex", "willChange"),
                d.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(DartParameter.named(5, false), d.slots().getFirst().parameter());
        assertEquals(0, d.slots().getFirst().minChildren());
        assertTrue(node().properties().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        var plain = C.find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow();
        for (var owner : C.definitions()) for (var slot : owner.slots())
            assertEquals(WidgetPlacementRules.accepts(owner, slot, plain), WidgetPlacementRules.accepts(owner, slot, d));
        for (var child : C.definitions())
            assertEquals(WidgetPlacementRules.accepts(plain, plain.slots().getFirst(), child),
                    WidgetPlacementRules.accepts(d, d.slots().getFirst(), child));
    }
    @Test void allSourceFormsHaveExactProofAndRoundTrip() throws Exception {
        var codec = new FdDocumentCodec();
        for (String field : List.of("painter", "foregroundPainter", "size"))
        for (boolean imported : List.of(false,true)) for (boolean member : List.of(false,true)) for (boolean factory : List.of(false,true)) {
            var ref = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:sample/values.dart") : Optional.empty(),
                    member ? "Values" : "sourceValue", member ? Optional.of("sourceValue") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            var n = with(node(), field, ref);
            var out = generated(n);
            assertTrue(out.symbolOccurrences().stream().flatMap(o -> o.staticTypeRequirement().stream())
                    .anyMatch(t -> t.expectedDartType().equals(field.equals("size") ? "Size" : "CustomPainter?")));
            var document = doc(n); assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
            assertFalse(out.build().payload().contains("Builder("));
        }
    }
    @Test void localSizeAndNullablePaintersKeepNativeArguments() throws Exception {
        var n = with(with(with(node(), "painter", new PropertyValue.NullValue()), "foregroundPainter",
                new PropertyValue.NullValue()), "size", new PropertyValue.SizeValue(new BigDecimal("48.5"), new BigDecimal("32.25")));
        var source = generated(n).build().payload();
        assertTrue(source.contains("Size(48.5, 32.25)"), source);
        assertTrue(source.contains("painter: null"), source);
        assertTrue(source.contains("foregroundPainter: null"), source);
        assertFalse(source.contains("isComplex:"), source);
        assertFalse(source.contains("willChange:"), source);
        var codec = new FdDocumentCodec();
        var document = doc(n); assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
    }
    @Test void rejectsInvalidSizeKindsAndKnownNullCacheHints() {
        for (String field : List.of("isComplex", "willChange")) for (boolean explicitNull : List.of(false,true)) {
            var n = node();
            if (explicitNull) n = with(with(n, "painter", new PropertyValue.NullValue()), "foregroundPainter", new PropertyValue.NullValue());
            n = with(n, field, new PropertyValue.BooleanValue(true));
            assertFalse(new WidgetTreeValidator().validate(doc(n),C).valid());
            assertTrue(CustomPaintWidgetPropertySchema.relationshipError(n).isPresent());
        }
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.SizeValue(BigDecimal.ONE.negate(),BigDecimal.ZERO));
        for (PropertyValue bad : List.of(
                new PropertyValue.SizeValue(new BigDecimal("1e309"),BigDecimal.ZERO),new PropertyValue.NullValue(),new PropertyValue.StringValue("Size(1,2)")))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),"size",bad)),C).valid());
        for (String field : List.of("painter","foregroundPainter"))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),field,new PropertyValue.CallbackValue("_paint"))),C).valid());
    }
}
