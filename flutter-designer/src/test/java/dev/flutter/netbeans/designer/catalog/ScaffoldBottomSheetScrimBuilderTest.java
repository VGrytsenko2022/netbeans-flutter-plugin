package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ScaffoldBottomSheetScrimBuilderTest {
    private static final WidgetTypeId TYPE = ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE;
    private static final PropertyName PROPERTY = new PropertyName("bottomSheetScrimBuilder");
    private static final String SIGNATURE = "Widget? Function(BuildContext, Animation<double>)";

    @Test void optionalNonNullableBuilderExtendsOnlyTheScaffoldPropertySurfaceAndNotDrawerEvents() {
        var definition = definition();
        assertEquals(18, definition.properties().size());
        assertEquals(18, ScaffoldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(List.of("appBar", "body", "floatingActionButton"), definition.slots().stream().map(s -> s.name().value()).toList());
        var property = definition.property(PROPERTY).orElseThrow();
        assertEquals(DartParameter.named(20, false), property.parameter());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE), property.acceptedKinds());
        assertEquals(List.of(new PropertyValueConstraint.DartObjectReferenceValues(SIGNATURE)), property.constraints());
        assertTrue(property.creationDefault().isEmpty());
        var metadata = ScaffoldWidgetPropertySchema.find(PROPERTY).orElseThrow();
        assertEquals(ScaffoldWidgetPropertySchema.Group.APPEARANCE, metadata.group());
        assertFalse(ScaffoldWidgetPropertySchema.isStaticPreset(PROPERTY));
        assertTrue(metadata.description().contains("cannot be null"));
        assertEquals(List.of("onDrawerChanged", "onEndDrawerChanged"), WidgetEventCatalog.eventsFor(definition).stream()
                .filter(e -> e.kind() == WidgetEventDescriptor.Kind.EVENT).map(e -> e.propertyName().value()).toList());
        var descriptor = WidgetEventCatalog.find(TYPE, PROPERTY).orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, descriptor.kind());
        assertEquals(SIGNATURE, descriptor.callbackType());
        assertEquals(SIGNATURE, descriptor.signature().dartFunctionType());
        assertEquals("Widget? _scrim(BuildContext context, Animation<double> animation)", descriptor.signature().declaration("_scrim"));
        assertFalse(descriptor.required());
        assertFalse(descriptor.sdkRequired());
        assertFalse(descriptor.nullableCallback());
        assertFalse(descriptor.allowsExplicitNull());
        assertFalse(descriptor.defaultEvent());
        assertTrue(descriptor.unsetBehavior().contains("returning null"));
    }

    @Test void omittedArgumentKeepsConstSdkDefaultAndExplicitNullRawCodeOrShorthandAreRejected() {
        var omitted = generated(Map.of());
        assertTrue(omitted.build().payload().contains("return const Scaffold("));
        assertFalse(omitted.build().payload().contains("bottomSheetScrimBuilder:"));
        assertTrue(omitted.symbolOccurrences().stream().noneMatch(s -> s.staticTypeRequirement().isPresent()));
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_scrim"),
                new PropertyValue.StringValue("_scrim"), new PropertyValue.DartExpressionValue("(context, animation) => null"))) {
            assertFalse(valid(node(Map.of(PROPERTY, invalid))), invalid.toString());
        }
        assertFalse(valid(node(Map.of(new PropertyName("bottomSheet"), reference(Optional.empty(), "sheet", Optional.empty(), false, false)))));
    }

    @Test void allReferenceGetterMemberAndFactoryShapesRoundTripWithExactFunctionProofAndConservativeConstness() throws Exception {
        var codec = new FdDocumentCodec();
        for (var value : List.of(reference(Optional.empty(), "_scrim", Optional.empty(), false, false),
                reference(Optional.of("package:app/builders.dart"), "scrim", Optional.empty(), false, false),
                reference(Optional.of("package:app/builders.dart"), "Builders", Optional.of("scrim"), false, false),
                reference(Optional.empty(), "_scrimFactory", Optional.empty(), true, false),
                reference(Optional.of("package:app/builders.dart"), "ConstScrimBuilder", Optional.empty(), true, true))) {
            var root = node(Map.of(PROPERTY, value));
            assertTrue(valid(root));
            var document = document(root);
            var encoded = codec.encode(document);
            var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)).document();
            assertEquals(document, restored);
            assertArrayEquals(encoded.copyBytes(), codec.encode(restored).copyBytes());
            var generated = generated(root.properties());
            var occurrences = generated.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
            assertEquals(1, occurrences.size());
            var occurrence = occurrences.getFirst();
            var proof = occurrence.staticTypeRequirement().orElseThrow();
            assertEquals(SIGNATURE, proof.expectedDartType());
            assertTrue(proof.sourceTypeOverride().isEmpty());
            assertTrue(proof.sourceTypeBound().isEmpty());
            assertEquals(value.libraryUri().orElse(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI), occurrence.libraryUri());
            assertEquals("/root/properties/bottomSheetScrimBuilder/" + (value.member().isPresent() ? "member" : "rootSymbol"), occurrence.modelPath());
            String expression = generated.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset());
            assertTrue(expression.contains(value.rootSymbol()), expression);
            assertEquals(value.access() == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, expression.endsWith("()"));
            assertEquals(value.constant().orElse(false), generated.build().payload().contains("const Scaffold("));
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test void exactAnonymousSignatureAllowlistDoesNotAdmitOtherFunctionsNullableCallbacksOrGenericOverrides() {
        assertDoesNotThrow(() -> new PropertyValueConstraint.DartObjectReferenceValues(SIGNATURE));
        assertDoesNotThrow(() -> new GeneratedDartStaticTypeRequirement(1, 2, SIGNATURE));
        assertDoesNotThrow(() -> new GeneratedDartStaticTypeRequirement(1, 2, SIGNATURE, Optional.empty(), Optional.empty()));
        for (String invalid : List.of("Widget Function(BuildContext, Animation<double>)", "Widget? Function(BuildContext, Animation<double>)?",
                "Widget? Function(BuildContext, Animation<num>)", "Widget? Function(BuildContext, dynamic)",
                "Widget? Function(BuildContext)", "Widget? Function(BuildContext, Animation<double>, int)",
                "Widget? Function({BuildContext context, Animation<double> animation})", "void Function()",
                "Widget? Function(BuildContext, Animation<double>); ignored()")) {
            assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.DartObjectReferenceValues(invalid), invalid);
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(1, 2, invalid), invalid);
            var existing = WidgetEventCatalog.find(TYPE, PROPERTY).orElseThrow();
            assertThrows(IllegalArgumentException.class, () -> new WidgetEventDescriptor(PROPERTY, invalid,
                    existing.kind(), existing.signature(), false, false, false, false, false, Optional.empty(), "Omit", List.of(), List.of()), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(1, 2, SIGNATURE, Optional.of("Custom")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(1, 2, SIGNATURE, Optional.of("Custom"), Optional.of("Notification")));
        var shifted = new GeneratedDartStaticTypeRequirement(1, 2, SIGNATURE).shifted(10);
        assertEquals(11, shifted.expressionOffset());
        assertEquals(SIGNATURE, shifted.expectedDartType());
    }

    @Test void pinnedSdkDefaultAndFieldProveCallbackNonnullDespiteContradictoryProse() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        String source = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/scaffold.dart"));
        assertTrue(source.contains("this.bottomSheetScrimBuilder = _defaultBottomSheetScrimBuilder,"));
        assertTrue(source.contains("final " + SIGNATURE + " bottomSheetScrimBuilder;"));
        assertFalse(source.contains("final " + SIGNATURE + "? bottomSheetScrimBuilder;"));
        assertTrue(source.contains("If the builder returns null, then no scrim is shown."));
    }

    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String root, Optional<String> member, boolean factory, boolean constant) {
        return new PropertyValue.DartObjectReferenceValue(library, root, member, factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(constant) : Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var base = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        return new WidgetNode(base.id(), TYPE, properties, base.slots());
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static GeneratedDartRegions generated(Map<PropertyName, PropertyValue> properties) {
        var result = new DartRegionGenerator().generate(document(node(properties)), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow();
    }
}
