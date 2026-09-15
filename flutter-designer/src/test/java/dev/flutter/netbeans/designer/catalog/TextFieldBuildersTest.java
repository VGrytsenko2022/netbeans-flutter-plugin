package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TextFieldBuildersTest {
    private static final WidgetTypeId TYPE = TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE;
    private static final List<PropertyName> BUILDERS = List.of(new PropertyName("buildCounter"), new PropertyName("contextMenuBuilder"));
    private static final List<String> TYPES = List.of("InputCounterWidgetBuilder?", "EditableTextContextMenuBuilder?");
    private static final String LIBRARY = "package:app/builders.dart";

    @Test void appendsTwoNullableReferenceBuilderRowsWithoutChangingEarlierLeavesOrNativeEvents() {
        var definition = definition();
        assertEquals(56, definition.properties().size());
        assertEquals("canRequestFocus", definition.properties().get(53).name().value());
        assertEquals(List.of("buildCounter", "contextMenuBuilder"), definition.properties().subList(54, 56).stream().map(p -> p.name().value()).toList());
        assertEquals(TextFieldWidgetPropertySchema.Group.BUILDERS, TextFieldWidgetPropertySchema.Group.values()[6]);
        for (int index = 0; index < BUILDERS.size(); index++) {
            var name = BUILDERS.get(index);
            var property = definition.property(name).orElseThrow();
            assertEquals(DartParameter.named(54 + index, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.DART_OBJECT_REFERENCE), property.acceptedKinds());
            assertEquals(new PropertyValueConstraint.DartObjectReferenceValues(TYPES.get(index)), property.constraints().getFirst());
            var metadata = TextFieldWidgetPropertySchema.find(name).orElseThrow();
            assertEquals(TextFieldWidgetPropertySchema.Group.BUILDERS, metadata.group());
            assertEquals(TextFieldWidgetPropertySchema.Target.DIRECT, metadata.target());
            var callable = WidgetEventCatalog.find(TYPE, name).orElseThrow();
            assertEquals(WidgetEventDescriptor.Kind.BUILDER, callable.kind());
            assertEquals(TYPES.get(index), callable.callbackType());
            assertTrue(callable.nullableCallback());
            assertTrue(callable.allowsExplicitNull());
            assertFalse(callable.required());
            assertFalse(callable.sdkRequired());
            assertFalse(callable.defaultEvent());
        }
        assertEquals(7, WidgetEventCatalog.eventsFor(definition).stream().filter(p -> p.kind() == WidgetEventDescriptor.Kind.EVENT).count());
        assertEquals(new PropertyName("onChanged"), WidgetEventCatalog.defaultEventFor(definition).orElseThrow().propertyName());
    }

    @Test void exactSignaturesPreserveRequiredNamedNullableMaxLengthAndImports() {
        var counter = WidgetEventCatalog.find(TYPE, BUILDERS.getFirst()).orElseThrow();
        assertEquals("Widget? Function(BuildContext, {required int currentLength, required int? maxLength, required bool isFocused})", counter.signature().dartFunctionType());
        assertEquals("Widget? _counter(BuildContext context, {required int currentLength, required int? maxLength, required bool isFocused})", counter.signature().declaration("_counter"));
        assertEquals(List.of(false, true, true, true), counter.signature().parameters().stream().map(WidgetEventDescriptor.Parameter::named).toList());
        assertTrue(counter.signature().parameters().stream().allMatch(WidgetEventDescriptor.Parameter::required));
        assertEquals(List.of("package:flutter/material.dart"), counter.signature().importUris());
        assertTrue(counter.createStub("_counter").contains("throw UnimplementedError"));
        assertTrue(counter.unsetBehavior().contains("returning null hides"));
        var menu = WidgetEventCatalog.find(TYPE, BUILDERS.getLast()).orElseThrow();
        assertEquals("Widget Function(BuildContext, EditableTextState)", menu.signature().dartFunctionType());
        assertEquals("Widget _menu(BuildContext context, EditableTextState editableTextState)", menu.signature().declaration("_menu"));
        assertEquals(List.of("package:flutter/widgets.dart"), menu.signature().importUris());
        assertTrue(menu.unsetBehavior().contains("disables"));
    }

    @Test void omissionAndExplicitNullRemainDistinctCanonicalValuesAndGeneratedArguments() throws Exception {
        var codec = new FdDocumentCodec();
        var omitted = generated(Map.of());
        assertTrue(omitted.build().payload().contains("child: const TextField(),"), omitted.build().payload());
        assertTrue(omitted.build().payload().contains("width: constraints.hasBoundedWidth ? null : 240"), omitted.build().payload());
        assertFalse(omitted.build().payload().contains("buildCounter:"));
        assertFalse(omitted.build().payload().contains("contextMenuBuilder:"));
        for (PropertyName name : BUILDERS) {
            var document = document(Map.of(name, new PropertyValue.NullValue()));
            var bytes = codec.encode(document);
            var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
            assertInstanceOf(PropertyValue.NullValue.class, restored.root().properties().get(name));
            assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
            assertNotEquals(codec.encode(document(Map.of())), bytes);
            var output = generated(restored.root().properties());
            assertTrue(output.build().payload().contains(name.value() + ": null"), output.build().payload());
            assertTrue(output.build().payload().contains("const TextField("));
            assertTrue(output.symbolOccurrences().stream().noneMatch(s -> s.staticTypeRequirement().isPresent()));
        }
    }

    @Test void allReferenceGetterMemberFactoryShapesRetainExactNullableProofsAndAliasRanges() throws Exception {
        var codec = new FdDocumentCodec();
        for (int index = 0; index < BUILDERS.size(); index++) {
            var name = BUILDERS.get(index);
            for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of(LIBRARY))) {
                for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("builder"))) {
                    for (boolean factory : List.of(false, true)) {
                        for (boolean constant : factory ? List.of(false, true) : List.of(false)) {
                            var reference = reference(library, member.isPresent() ? "Builders" : "builder", member, factory, constant);
                            var document = document(Map.of(name, reference));
                            var bytes = codec.encode(document);
                            var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
                            assertEquals(document, restored);
                            assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
                            var output = generated(restored.root().properties());
                            var occurrences = output.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
                            assertEquals(1, occurrences.size(), output.symbolOccurrences().toString());
                            var occurrence = occurrences.getFirst();
                            var proof = occurrence.staticTypeRequirement().orElseThrow();
                            assertEquals(TYPES.get(index), proof.expectedDartType());
                            assertTrue(proof.sourceTypeOverride().isEmpty());
                            assertTrue(proof.sourceTypeBound().isEmpty());
                            assertEquals(library.orElse(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI), occurrence.libraryUri());
                            assertEquals("/root/properties/" + name.value() + "/" + (member.isPresent() ? "member" : "rootSymbol"), occurrence.modelPath());
                            String prefix = library.map(uri -> output.importPlan().directives().stream().filter(d -> d.uri().equals(uri)).findFirst().orElseThrow()
                                    .prefix().map(p -> p + ".").orElse("")).orElse("");
                            String expression = output.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset());
                            assertEquals((constant ? "const " : "") + prefix + reference.rootSymbol() + member.map(m -> "." + m).orElse("") + (factory ? "()" : ""), expression);
                            assertTrue(output.build().payload().contains(name.value() + ": " + expression));
                            assertEquals(constant, output.build().payload().contains("const TextField("));
                        }
                    }
                }
            }
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test void rejectsRawCodeLegacyCallbackShorthandAndUnreviewedNullableTypeGrammar() {
        for (PropertyName name : BUILDERS) {
            for (PropertyValue invalid : List.of(new PropertyValue.CallbackValue("_builder"), new PropertyValue.StringValue("_builder"),
                    new PropertyValue.BooleanValue(true), new PropertyValue.DartExpressionValue("(context) => null"))) {
                var document = document(Map.of(name, invalid));
                assertFalse(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid(), invalid.toString());
                assertFalse(new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).successful(), invalid.toString());
            }
        }
        for (String type : List.of("ButtonLayerBuilder?", "ScrollNotificationPredicate?", "ArbitraryBuilder?", "InputCounterWidgetBuilder??",
                "InputCounterWidgetBuilder?; bad()", "EditableTextContextMenuBuilder ?")) {
            assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.DartObjectReferenceValues(type), type);
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, type), type);
        }
        for (String type : TYPES) {
            var requirement = new GeneratedDartStaticTypeRequirement(4, 2, type);
            assertEquals(new GeneratedDartStaticTypeRequirement(9, 2, type), requirement.shifted(5));
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, type, Optional.of("Fake")));
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, type, Optional.empty(), Optional.of("Notification")));
        }
        assertDoesNotThrow(() -> new PropertyValueConstraint.DartObjectReferenceValues("FocusNode?"));
        assertDoesNotThrow(() -> new GeneratedDartStaticTypeRequirement(0, 1, "FocusNode?"));
    }

    @Test void bothBuildersHaveIndependentProofsAlongsideExistingTextFieldCompoundArguments() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(BUILDERS.getFirst(), reference(Optional.of(LIBRARY), "counter", Optional.empty(), false, false));
        values.put(BUILDERS.getLast(), reference(Optional.of(LIBRARY), "menu", Optional.empty(), true, false));
        values.put(new PropertyName("keyboardType"), new PropertyValue.StringValue("emailAddress"));
        values.put(new PropertyName("maxLength"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(12)));
        values.put(new PropertyName("onChanged"), new PropertyValue.CallbackValue("_changed"));
        var output = generated(values);
        assertEquals(TYPES, output.symbolOccurrences().stream().flatMap(s -> s.staticTypeRequirement().stream()).map(GeneratedDartStaticTypeRequirement::expectedDartType).toList());
        assertTrue(output.build().payload().contains("TextInputType.emailAddress"));
        assertTrue(output.build().payload().contains("maxLength: 12"));
        assertTrue(output.build().payload().contains("onChanged: _changed"));
        assertEquals(1, output.importPlan().directives().stream().filter(d -> d.uri().equals(LIBRARY)).count());
    }

    @Test void pinnedSdkConfirmsNullableCallbacksAndExactRequiredNamedCounterSignature() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        String textField = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/text_field.dart"));
        String editable = Files.readString(Path.of(sdk, "packages/flutter/lib/src/widgets/editable_text.dart"));
        assertTrue(textField.contains("final InputCounterWidgetBuilder? buildCounter;"));
        assertTrue(textField.contains("final EditableTextContextMenuBuilder? contextMenuBuilder;"));
        assertTrue(textField.contains("this.buildCounter,"));
        assertTrue(textField.contains("this.contextMenuBuilder = _defaultContextMenuBuilder,"));
        assertTrue(textField.replaceAll("(?m)^\\s*///[^\\r\\n]*", "").replaceAll("\\s+", " ")
                .contains("Widget? Function( BuildContext context, { required int currentLength, required int? maxLength, required bool isFocused, }"));
        assertTrue(editable.replaceAll("\\s+", " ").contains("Widget Function(BuildContext context, EditableTextState editableTextState);"));
    }

    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String root, Optional<String> member, boolean factory, boolean constant) {
        return new PropertyValue.DartObjectReferenceValue(library, root, member, factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(constant) : Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static DesignerDocument document(Map<PropertyName, PropertyValue> properties) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)),
                new WidgetNode(StableId.random(), TYPE, properties, Map.of()));
    }
    private static GeneratedDartRegions generated(Map<PropertyName, PropertyValue> properties) {
        var result = new DartRegionGenerator().generate(document(properties), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow();
    }
}
