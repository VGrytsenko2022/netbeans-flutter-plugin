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

class ElevatedButtonLayerBuildersTest {
    private static final WidgetTypeId TYPE = ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE;
    private static final List<String> BUILDERS = List.of("styleBackgroundBuilder", "styleForegroundBuilder");
    private static final String LIBRARY = "package:app/layers.dart";

    @Test void appendsTwoCommonStyleBuildersWithoutChangingFiveStateBucketsOrFourEvents() {
        var definition = definition(TYPE);
        assertEquals(288, definition.properties().size());
        assertEquals(11, ElevatedButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT);
        assertEquals(BUILDERS, ElevatedButtonWidgetPropertySchema.layerBuilderProperties());
        for (int i = 0; i < BUILDERS.size(); i++) {
            String name = BUILDERS.get(i);
            var property = definition.property(p(name)).orElseThrow();
            assertEquals(DartParameter.named(286 + i, false), property.parameter());
            assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE), property.acceptedKinds());
            assertEquals(List.of(new PropertyValueConstraint.DartObjectReferenceValues("ButtonLayerBuilder")), property.constraints());
            assertTrue(property.creationDefault().isEmpty());
            var metadata = ElevatedButtonWidgetPropertySchema.find(name).orElseThrow();
            assertEquals(ElevatedButtonWidgetPropertySchema.Group.COMMON_STYLE, metadata.group());
            assertEquals(9 + i, metadata.dartOrder());
            assertEquals(ElevatedButtonWidgetPropertySchema.Target.STYLE_COMMON, metadata.target());
            var event = WidgetEventCatalog.find(TYPE, p(name)).orElseThrow();
            assertEquals(WidgetEventDescriptor.Kind.BUILDER, event.kind());
            assertEquals("ButtonLayerBuilder", event.callbackType());
            assertEquals("Widget Function(BuildContext, Set<WidgetState>, Widget?)", event.signature().dartFunctionType());
            assertFalse(event.required());
            assertFalse(event.sdkRequired());
            assertTrue(event.nullableCallback());
            assertFalse(event.allowsExplicitNull());
            assertFalse(event.defaultEvent());
        }
        for (int i = 0; i < 286; i++) assertEquals(i, definition.properties().get(i).parameter().order());
        assertEquals(List.of("onPressed", "onLongPress", "onHover", "onFocusChange"), WidgetEventCatalog.eventsFor(definition).stream()
                .filter(e -> e.kind() == WidgetEventDescriptor.Kind.EVENT).map(e -> e.propertyName().value()).toList());
        var child = definition.slots().getFirst();
        assertEquals("child", child.name().value());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertEquals(DartParameter.named(288, true), child.parameter());
    }

    @Test void fullStyleFamiliesKeepExactPropertyAndLayerOrdersWithoutDuplicateCommonRows() {
        var sizes = Map.of("TextButton", 511, "OutlinedButton", 510, "FilledButton", 510, "IconButton", 524);
        var backgroundOrders = Map.of("TextButton", 508, "OutlinedButton", 507, "FilledButton", 507, "IconButton", 524);
        for (var entry : sizes.entrySet()) {
            var definition = definition(new WidgetTypeId("flutter.material." + entry.getKey()));
            assertEquals(entry.getValue().intValue(), definition.properties().size(), entry.getKey());
            assertEquals(definition.properties().size(), definition.properties().stream().map(PropertyDefinition::name).distinct().count());
            for (int i = 0; i < BUILDERS.size(); i++) {
                var property = definition.property(p(BUILDERS.get(i))).orElseThrow();
                assertEquals(backgroundOrders.get(entry.getKey()) + i, property.parameter().order(), entry.getKey());
                assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE), property.acceptedKinds());
            }
            assertEquals(backgroundOrders.get(entry.getKey()) - 1, definition.property(p("styleIconAlignment")).orElseThrow().parameter().order());
        }
        assertEquals(12, TextButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT);
        assertEquals(10, TextButtonWidgetPropertySchema.find("styleBackgroundBuilder").orElseThrow().dartOrder());
        assertEquals(11, TextButtonWidgetPropertySchema.find("styleForegroundBuilder").orElseThrow().dartOrder());
        assertEquals("styleIconAlignment", TextButtonWidgetPropertySchema.definitions().keySet().stream().skip(507).findFirst().orElseThrow());
    }

    @Test void eachReferenceGetterMemberFactoryAndConstFlagSurvivesCodecAndCarriesOneExactExpressionProof() throws Exception {
        var codec = new FdDocumentCodec();
        for (String name : BUILDERS) for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of(LIBRARY))) {
            for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("layer"))) {
                for (boolean factory : List.of(false, true)) for (boolean constant : factory ? List.of(false, true) : List.of(false)) {
                    var reference = new PropertyValue.DartObjectReferenceValue(library, member.isPresent() ? "Layers" : "layer", member,
                            factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                            factory ? Optional.of(constant) : Optional.empty());
                    var document = document(Map.of(p(name), reference));
                    var bytes = codec.encode(document);
                    var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
                    assertEquals(document, restored);
                    assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
                    var output = generated(restored.root().properties());
                    var proofs = output.symbolOccurrences().stream().filter(o -> o.staticTypeRequirement().isPresent()).toList();
                    assertEquals(1, proofs.size());
                    var occurrence = proofs.getFirst();
                    var proof = occurrence.staticTypeRequirement().orElseThrow();
                    assertEquals("ButtonLayerBuilder", proof.expectedDartType());
                    assertTrue(proof.sourceTypeOverride().isEmpty());
                    assertTrue(proof.sourceTypeBound().isEmpty());
                    assertEquals(library.orElse(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI), occurrence.libraryUri());
                    assertEquals("/root/properties/" + name + "/" + (member.isPresent() ? "member" : "rootSymbol"), occurrence.modelPath());
                    String prefix = library.map(uri -> output.importPlan().directives().stream().filter(d -> d.uri().equals(uri))
                            .findFirst().orElseThrow().prefix().map(p -> p + ".").orElse("")).orElse("");
                    String expression = output.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset());
                    assertEquals((constant ? "const " : "") + prefix + reference.rootSymbol() + member.map(m -> "." + m).orElse("") + (factory ? "()" : ""), expression);
                    assertTrue(output.build().payload().contains((name.contains("Background") ? "backgroundBuilder: " : "foregroundBuilder: ") + expression));
                    assertFalse(output.build().payload().contains("const ElevatedButton("));
                    assertTrue(output.build().payload().contains("child: null"));
                }
            }
        }
        assertEquals(16, DesignerDocument.SCHEMA_VERSION);
        assertEquals(15, WidgetCatalog.API_VERSION);
    }

    @Test void bothLayersRemainOutsideStateResolversAndPreserveActivationSparseStyleAndEmptyChild() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("styleBackgroundBuilder"), reference("background"));
        values.put(p("styleForegroundBuilder"), reference("foreground"));
        values.put(p("stylePressedElevation"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(6)));
        values.put(p("styleEnableFeedback"), new PropertyValue.BooleanValue(false));
        values.put(p("onPressed"), new PropertyValue.CallbackValue("pressed"));
        var output = generated(values);
        String dart = output.build().payload();
        assertTrue(dart.contains("backgroundBuilder: background"), dart);
        assertTrue(dart.contains("foregroundBuilder: foreground"), dart);
        assertTrue(dart.contains("WidgetState.pressed"), dart);
        assertTrue(dart.contains("enableFeedback: false"), dart);
        assertTrue(dart.contains("onPressed: pressed"), dart);
        assertTrue(dart.contains("child: null"), dart);
        assertEquals(2, output.symbolOccurrences().stream().filter(o -> o.staticTypeRequirement().isPresent()).count());
        values.put(p("enabled"), new PropertyValue.BooleanValue(false));
        dart = generated(values).build().payload();
        assertTrue(dart.contains("onPressed: null"), dart);
        assertTrue(dart.contains("backgroundBuilder: background"), dart);
        assertTrue(dart.contains("foregroundBuilder: foreground"), dart);
    }

    @Test void omittedClipKeepsSdkAutomaticLocalAndThemeBuilderResolutionWhileEveryExplicitClipWins() {
        assertFalse(generated(Map.of()).build().payload().contains("style:"));
        for (String name : BUILDERS) {
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(p(name), reference("layer"));
            assertFalse(generated(properties).build().payload().contains("clipBehavior:"));
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                properties.put(p("clipBehavior"), new PropertyValue.EnumValue("Clip", clip));
                String dart = generated(properties).build().payload();
                assertTrue(dart.contains("clipBehavior: Clip." + clip), dart);
            }
        }
        assertTrue(ElevatedButtonWidgetPropertySchema.find("clipBehavior").orElseThrow().description().contains("effective local/theme"));
    }

    @Test void resetOnlyNullPolicyDoesNotAdmitRawCallbacksOrBroadenOtherStyleApis() {
        for (String name : BUILDERS) for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("layer"),
                new PropertyValue.StringValue("layer"), new PropertyValue.BooleanValue(true), new PropertyValue.DartExpressionValue("(context, states, child) => child!"))) {
            var document = document(Map.of(p(name), invalid));
            assertFalse(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid(), invalid.toString());
            assertFalse(new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).successful(), invalid.toString());
        }
        for (String name : List.of("style", "variant", "focusNode", "statesController", "styleIconAlignment")) {
            assertTrue(definition(TYPE).property(p(name)).isEmpty(), name);
        }
    }

    @Test void pinnedSdkLayerSignatureNullableFieldsAndEffectiveClipMatchTheAdmittedContract() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        String style = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/button_style.dart"));
        String button = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/button_style_button.dart"));
        assertTrue(style.replaceAll("\\s+", " ").contains("typedef ButtonLayerBuilder = Widget Function(BuildContext context, Set<WidgetState> states, Widget? child);"));
        assertTrue(style.contains("final ButtonLayerBuilder? backgroundBuilder;"));
        assertTrue(style.contains("final ButtonLayerBuilder? foregroundBuilder;"));
        assertTrue(button.contains("widget.clipBehavior ??"));
        assertTrue(button.contains("(resolvedBackgroundBuilder ?? resolvedForegroundBuilder) != null"));
        assertTrue(button.contains("? Clip.antiAlias"));
    }

    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetDefinition definition(WidgetTypeId type) { return BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(); }
    private static DesignerDocument document(Map<PropertyName, PropertyValue> properties) {
        var region = new ManagedRegion("0".repeat(64));
        var node = new WidgetNode(StableId.random(), TYPE, properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), node);
    }
    private static GeneratedDartRegions generated(Map<PropertyName, PropertyValue> properties) {
        var result = new DartRegionGenerator().generate(document(properties), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow();
    }
}
