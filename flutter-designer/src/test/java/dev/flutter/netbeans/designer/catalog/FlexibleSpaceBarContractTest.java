package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlexibleSpaceBarContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    static final WidgetDefinition D = C.find(FlexibleSpaceBarWidgetPropertySchema.TYPE).orElseThrow();
    static PropertyName p(String name) { return new PropertyName(name); }
    static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        return new WidgetNode(StableId.random(), D.typeId(), properties, Map.of());
    }
    static DesignerDocument doc(WidgetNode node) {
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("form.dart", "Form", WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion("0".repeat(64)), new ManagedRegion("0".repeat(64)))), node);
    }
    static GeneratedDartRegions generated(WidgetNode node) {
        var result = new DartRegionGenerator().generate(doc(node), C);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    @Test void exactNativeConstructorCapabilitiesAndSlotPlacement() {
        assertEquals(Set.of("centerTitle", "titlePadding", "collapseMode", "stretchModes", "expandedTitleScale"),
                D.properties().stream().map(v -> v.name().value()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of("title", "background"), D.slots().stream().map(v -> v.name().value()).toList());
        assertTrue(D.constConstructor()); assertTrue(D.namedConstructor().isEmpty()); assertTrue(D.traits().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(D).isEmpty());
        for (var capability : WidgetCapability.values()) assertTrue(BuiltInWidgetCapabilityCatalog.supports(D, capability));
        assertTrue(WidgetNodePrototypeFactory.create(D, StableId.random()).properties().isEmpty());
        assertTrue(WidgetPlacementRules.evaluateRoot(D).accepted(), "The containing application may supply settings externally");
        for (var owner : C.definitions()) for (var slot : owner.slots())
            assertEquals(slot.acceptance() instanceof SlotAcceptance.AnyWidget, WidgetPlacementRules.accepts(owner, slot, D));
        assertTrue(generated(node(Map.of())).build().payload().contains("const FlexibleSpaceBar("));
    }
    @Test void everyStretchCombinationAndCollapseModeHasTypedEvidenceAndExactRoundTrip() throws Exception {
        for (String stretch : FlexibleSpaceBarWidgetPropertySchema.STRETCH_PRESETS) for (String collapse : List.of("parallax", "pin", "none")) {
            var n = node(Map.of(p("stretchModes"), new PropertyValue.StringValue(stretch),
                    p("collapseMode"), new PropertyValue.EnumValue("CollapseMode", collapse)));
            var result = generated(n); String source = result.build().payload();
            assertTrue(source.contains("collapseMode: CollapseMode." + collapse), source);
            assertTrue(source.contains("stretchModes: const <StretchMode>["), source);
            int count = stretch.equals("none") ? 0 : stretch.split(",").length;
            assertEquals(count + 1, result.symbolOccurrences().stream().filter(v -> v.symbolName().equals("StretchMode")).count());
            assertTrue(result.symbolOccurrences().stream().filter(v -> v.symbolName().equals("StretchMode"))
                    .allMatch(v -> v.libraryUri().equals("package:flutter/material.dart")));
            if (count == 0) assertTrue(source.contains("const <StretchMode>[]"), source);
            else for (String mode : stretch.split(",")) assertTrue(source.contains("StretchMode." + mode), source);
            var document = doc(n); var codec = new FdDocumentCodec();
            assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
        }
    }
    @Test void nativeNullabilityAndInclusiveScaleBoundaryAreEnforced() {
        for (String name : List.of("centerTitle", "titlePadding"))
            assertTrue(generated(node(Map.of(p(name), new PropertyValue.NullValue()))).build().payload().contains(name + ": null"));
        for (String name : List.of("collapseMode", "stretchModes", "expandedTitleScale"))
            assertFalse(new WidgetTreeValidator().validate(doc(node(Map.of(p(name), new PropertyValue.NullValue()))), C).valid());
        for (String value : List.of("1", "1.5", "3"))
            assertTrue(generated(node(Map.of(p("expandedTitleScale"), new PropertyValue.DoubleValue(new BigDecimal(value))))).build().payload().contains("expandedTitleScale:"));
        for (String value : List.of("-1", "0", "0.999"))
            assertFalse(new WidgetTreeValidator().validate(doc(node(Map.of(p("expandedTitleScale"), new PropertyValue.DoubleValue(new BigDecimal(value))))), C).valid());
        for (String bad : List.of("", "unknown", "zoomBackground; evil()", "null"))
            assertFalse(new WidgetTreeValidator().validate(doc(node(Map.of(p("stretchModes"), new PropertyValue.StringValue(bad)))), C).valid());
        assertFalse(D.property(p("titlePadding")).orElseThrow().constraints().stream().anyMatch(c -> c.accepts(
                new PropertyValue.EdgeInsetsValue(BigDecimal.valueOf(-1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));
    }
    @Test void paddingAndListReferencesRetainImportedMemberAndFactoryIdentity() {
        for (String field : List.of("titlePadding", "stretchModes")) for (boolean imported : List.of(false, true)) for (boolean invoke : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/values.dart") : Optional.empty(),
                    "Values", Optional.of("value"), invoke ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                    : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, invoke ? Optional.of(false) : Optional.empty());
            String source = generated(node(Map.of(p(field), reference))).build().payload();
            assertTrue(source.contains("Values.value" + (invoke ? "()" : "")), source);
        }
        assertEquals("EdgeInsetsGeometry?", D.property(p("titlePadding")).orElseThrow().constraints().stream()
                .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow().expectedDartType());
    }
}
