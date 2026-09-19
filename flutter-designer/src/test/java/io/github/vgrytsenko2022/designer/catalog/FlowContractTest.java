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

public class FlowContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    static final WidgetTypeId TYPE = FlowWidgetPropertySchema.TYPE;
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
        assertFalse(d.constConstructor());
        assertTrue(C.find(FlowWidgetPropertySchema.UNWRAPPED_TYPE).orElseThrow().constConstructor());
        assertEquals(List.of("delegate", "clipBehavior"),
                d.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(DartParameter.named(1, false), d.slots().getFirst().parameter());
        assertEquals(0, d.slots().getFirst().minChildren());
        assertEquals(FlowWidgetPropertySchema.INITIAL_DELEGATE,node().properties().get(new PropertyName("delegate")));
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
        for (var type : List.of(TYPE, FlowWidgetPropertySchema.UNWRAPPED_TYPE))
        for (String field : List.of("delegate"))
        for (boolean imported : List.of(false,true)) for (boolean member : List.of(false,true)) for (boolean factory : List.of(false,true)) {
            var ref = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:sample/values.dart") : Optional.empty(),
                    member ? "Values" : "sourceValue", member ? Optional.of("sourceValue") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            var n = with(WidgetNodePrototypeFactory.create(C.find(type).orElseThrow(),StableId.random()), field, ref);
            var out = generated(n);
            assertTrue(out.symbolOccurrences().stream().flatMap(o -> o.staticTypeRequirement().stream())
                    .anyMatch(t -> t.expectedDartType().equals("FlowDelegate")));
            var document = doc(n); assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
            assertFalse(out.build().payload().contains("Builder("));
        }
    }
    @Test void clipVariantsAndConstructorNamesArePreserved() {
        for (var type : List.of(TYPE, FlowWidgetPropertySchema.UNWRAPPED_TYPE)) {
            var base = WidgetNodePrototypeFactory.create(C.find(type).orElseThrow(),StableId.random());
            String ctor = type.equals(TYPE) ? "Flow(" : "Flow.unwrapped(";
            assertTrue(generated(base).build().payload().contains(ctor));
            assertFalse(generated(base).build().payload().contains("clipBehavior:"));
            for (String clip : List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer")) {
                var n = with(base,"clipBehavior",new PropertyValue.EnumValue("Clip",clip));
                assertTrue(generated(n).build().payload().contains("clipBehavior: Clip."+clip));
            }
            assertFalse(new WidgetTreeValidator().validate(doc(with(base,"clipBehavior",new PropertyValue.NullValue())),C).valid());
        }
    }
    @Test void requiredDelegateRejectsOmissionNullLiteralAndCallback() {
        assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(StableId.random(),TYPE,Map.of(),Map.of())),C).valid());
        for (PropertyValue bad : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("delegate"),
                new PropertyValue.CallbackValue("_layout"), new PropertyValue.BooleanValue(false)))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),"delegate",bad)),C).valid());
    }
}
