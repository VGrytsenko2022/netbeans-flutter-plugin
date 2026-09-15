package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ListViewItemExtentBuilderTest {
    private static final WidgetTypeId TYPE = ListViewWidgetPropertySchema.LIST_VIEW_TYPE;
    private static final PropertyName PROPERTY = new PropertyName("itemExtentBuilder");
    private static final PropertyName FIXED = new PropertyName("itemExtent");
    private static final String EXPECTED = "ItemExtentBuilder?";
    private static final String LIBRARY = "package:app/extents.dart";

    @Test void appendsOneNullableBuilderWithRenderingSignatureAndPreservesAllPriorOrders() {
        var definition = definition();
        assertEquals(18, definition.properties().size());
        assertEquals(18, ListViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, definition.slots().size());
        assertEquals(11, definition.slot(new SlotName("children")).orElseThrow().parameter().order());
        assertEquals("hitTestBehavior", definition.properties().get(16).name().value());
        var property = definition.property(PROPERTY).orElseThrow();
        assertEquals(DartParameter.named(18, false), property.parameter());
        assertTrue(property.creationDefault().isEmpty());
        assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.DART_OBJECT_REFERENCE), property.acceptedKinds());
        assertEquals(new PropertyValueConstraint.DartObjectReferenceValues(EXPECTED), property.constraints().getFirst());
        assertEquals(ListViewWidgetPropertySchema.Group.LAYOUT, ListViewWidgetPropertySchema.find(PROPERTY).orElseThrow().group());
        assertEquals(ListViewWidgetPropertySchema.Target.DIRECT, ListViewWidgetPropertySchema.find(PROPERTY).orElseThrow().target());
        assertFalse(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.property(new PropertyName("prototypeItem")).isEmpty());
        var callable = WidgetEventCatalog.find(TYPE, PROPERTY).orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER, callable.kind());
        assertEquals(EXPECTED, callable.callbackType());
        assertEquals("double? Function(int, SliverLayoutDimensions)", callable.signature().dartFunctionType());
        assertEquals("double? _extent(int index, SliverLayoutDimensions dimensions)", callable.signature().declaration("_extent"));
        assertEquals(List.of("package:flutter/rendering.dart"), callable.signature().importUris());
        assertTrue(callable.signature().parameters().stream().allMatch(p -> !p.named() && p.required()));
        assertTrue(callable.nullableCallback());
        assertTrue(callable.allowsExplicitNull());
        assertFalse(callable.required());
        assertFalse(callable.sdkRequired());
        assertFalse(callable.defaultEvent());
        assertTrue(callable.createStub("_extent").contains("throw UnimplementedError"));
        assertTrue(callable.unsetBehavior().contains("every actual child"));
        assertTrue(callable.unsetBehavior().contains("not to request default sizing"));
        assertEquals(List.of(callable), WidgetEventCatalog.eventsFor(definition));
        assertTrue(WidgetEventCatalog.defaultEventFor(definition).isEmpty());
    }

    @Test void omissionAndExplicitNullRoundTripWithoutChangingStaticChildrenOrConstness() throws Exception {
        var codec = new FdDocumentCodec();
        for (var children : List.of(List.<WidgetNode>of(), children())) {
            var omitted = generated(Map.of(), children);
            assertFalse(omitted.build().payload().contains("itemExtentBuilder:"));
            assertFalse(omitted.build().payload().contains("const ListView("));
            var document = document(Map.of(PROPERTY, new PropertyValue.NullValue()), children);
            var bytes = codec.encode(document);
            var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
            assertEquals(document, restored);
            assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
            assertEquals(children, ((WidgetSlot.ListSlot) restored.root().slots().get(new SlotName("children"))).children());
            var output = generated(restored.root().properties(), children);
            assertTrue(output.build().payload().contains("itemExtentBuilder: null"), output.build().payload());
            assertFalse(output.build().payload().contains("const ListView("));
            assertTrue(output.symbolOccurrences().stream().noneMatch(s -> s.staticTypeRequirement().isPresent()));
        }
    }

    @Test void allLocalImportedGetterMemberAndFactoryShapesRetainNullableProofRangesAndChildren() throws Exception {
        var codec = new FdDocumentCodec();
        var children = children();
        for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of(LIBRARY))) {
            for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("extent"))) {
                for (boolean factory : List.of(false, true)) {
                    for (boolean constant : factory ? List.of(false, true) : List.of(false)) {
                        var reference = reference(library, member.isPresent() ? "Extents" : "extent", member, factory, constant);
                        var document = document(Map.of(PROPERTY, reference), children);
                        var bytes = codec.encode(document);
                        var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
                        assertEquals(document, restored);
                        assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
                        var output = generated(restored.root().properties(), children);
                        var occurrences = output.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
                        assertEquals(1, occurrences.size(), output.symbolOccurrences().toString());
                        var occurrence = occurrences.getFirst();
                        var proof = occurrence.staticTypeRequirement().orElseThrow();
                        assertEquals(EXPECTED, proof.expectedDartType());
                        assertTrue(proof.sourceTypeOverride().isEmpty());
                        assertTrue(proof.sourceTypeBound().isEmpty());
                        assertEquals(library.orElse(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI), occurrence.libraryUri());
                        assertEquals("/root/properties/itemExtentBuilder/" + (member.isPresent() ? "member" : "rootSymbol"), occurrence.modelPath());
                        String prefix = library.map(uri -> output.importPlan().directives().stream().filter(d -> d.uri().equals(uri)).findFirst().orElseThrow()
                                .prefix().map(p -> p + ".").orElse("")).orElse("");
                        String expression = output.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset());
                        assertEquals((constant ? "const " : "") + prefix + reference.rootSymbol() + member.map(m -> "." + m).orElse("") + (factory ? "()" : ""), expression);
                        assertTrue(output.build().payload().contains("itemExtentBuilder: " + expression));
                        assertFalse(output.build().payload().contains("const ListView("));
                    }
                }
            }
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test void fixedExtentConflictsWithEveryReferenceButNotExplicitNullAndNeverClearsValues() {
        var children = children();
        for (PropertyValue fixed : List.of(new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DoubleValue(new BigDecimal("24.5")))) {
            assertTrue(new WidgetTreeValidator().validate(document(Map.of(FIXED, fixed), children), BuiltInWidgetCatalog.getDefault()).valid());
            var nullOutput = generated(Map.of(FIXED, fixed, PROPERTY, new PropertyValue.NullValue()), children);
            assertTrue(nullOutput.build().payload().contains("itemExtentBuilder: null"));
            assertTrue(nullOutput.build().payload().contains("itemExtent:"));
            for (boolean factory : List.of(false, true)) {
                var reference = reference(Optional.empty(), "nullableExtent", Optional.empty(), factory, false);
                var document = document(Map.of(FIXED, fixed, PROPERTY, reference), children);
                var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault());
                assertFalse(result.valid());
                var issue = result.errors().stream().filter(i -> i.code().equals(WidgetTreeValidator.PROPERTY_CONFLICT)).findFirst().orElseThrow();
                assertEquals("/root/properties/itemExtentBuilder", issue.path());
                assertEquals(Optional.of(document.root().id()), issue.widgetId());
                assertTrue(issue.message().contains("Reset itemExtent"));
                assertTrue(issue.message().contains("explicit null"));
                assertTrue(issue.message().contains("No value was cleared"));
                assertEquals(Map.of(FIXED, fixed, PROPERTY, reference), document.root().properties());
                assertFalse(new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).successful());
            }
        }
    }

    @Test void rejectsLegacyCallbacksRawCodeAndUnreviewedNullableTypeSpellings() {
        for (PropertyValue invalid : List.of(new PropertyValue.CallbackValue("_extent"), new PropertyValue.StringValue("_extent"),
                new PropertyValue.BooleanValue(true), new PropertyValue.DartExpressionValue("(index, dimensions) => 40.0"))) {
            var document = document(Map.of(PROPERTY, invalid), children());
            assertFalse(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid(), invalid.toString());
            assertFalse(new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).successful(), invalid.toString());
        }
        for (String type : List.of("ItemExtentBuilder??", "ItemExtentBuilder ?", "ItemExtentBuilder<dynamic>?", "ItemExtentBuilder?; bad()", "OtherExtentBuilder?")) {
            assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.DartObjectReferenceValues(type), type);
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, type), type);
        }
        var requirement = new GeneratedDartStaticTypeRequirement(4, 2, EXPECTED);
        assertEquals(new GeneratedDartStaticTypeRequirement(9, 2, EXPECTED), requirement.shifted(5));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, EXPECTED, Optional.of("Fake")));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, EXPECTED, Optional.empty(), Optional.of("Notification")));
        assertDoesNotThrow(() -> new PropertyValueConstraint.DartObjectReferenceValues("InputCounterWidgetBuilder?"));
        assertDoesNotThrow(() -> new PropertyValueConstraint.DartObjectReferenceValues("EditableTextContextMenuBuilder?"));
    }

    @Test void preservesViewportGuardCompoundPhysicsCacheAndOrderedChildGeneration() {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(PROPERTY, reference(Optional.of(LIBRARY), "extent", Optional.empty(), false, false));
        values.put(new PropertyName("physics"), new PropertyValue.StringValue("clamping"));
        values.put(new PropertyName("scrollCacheExtent"), new PropertyValue.IntegerValue(BigInteger.valueOf(150)));
        values.put(new PropertyName("semanticChildCount"), new PropertyValue.IntegerValue(BigInteger.valueOf(2)));
        var output = generated(values, children());
        assertTrue(output.build().payload().contains("ClampingScrollPhysics"));
        assertTrue(output.build().payload().contains("ScrollCacheExtent.pixels(150)"), output.build().payload());
        assertTrue(output.build().payload().contains("semanticChildCount: 2"));
        assertTrue(output.build().payload().contains("LayoutBuilder"), output.build().payload());
        assertTrue(output.build().payload().indexOf("height: 24") < output.build().payload().indexOf("height: 48"));
        assertEquals(List.of(EXPECTED), output.symbolOccurrences().stream().flatMap(s -> s.staticTypeRequirement().stream()).map(GeneratedDartStaticTypeRequirement::expectedDartType).toList());
    }

    @Test void pinnedSdkConfirmsNullableRenderingTypedefStaticConstructorAndExclusiveSizingModes() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        String scroll = Files.readString(Path.of(sdk, "packages/flutter/lib/src/widgets/scroll_view.dart"));
        String rendering = Files.readString(Path.of(sdk, "packages/flutter/lib/src/rendering/sliver.dart"));
        int start = scroll.indexOf("  ListView({");
        int end = scroll.indexOf("  ListView.builder({", start);
        assertTrue(start >= 0 && end > start);
        String constructor = scroll.substring(start, end);
        assertTrue(constructor.contains("this.itemExtentBuilder,"));
        assertTrue(constructor.contains("List<Widget> children = const <Widget>[],"));
        assertTrue(constructor.contains("You can only pass one of itemExtent, prototypeItem and itemExtentBuilder."));
        assertTrue(scroll.contains("final ItemExtentBuilder? itemExtentBuilder;"));
        assertTrue(rendering.contains("typedef ItemExtentBuilder = double? Function(int index, SliverLayoutDimensions dimensions);"));
        assertTrue(rendering.contains("Should return null if asked to build an item extent with a greater index than"));
        assertTrue(Files.readString(Path.of(sdk, "packages/flutter/lib/rendering.dart")).contains("export 'src/rendering/sliver.dart';"));
    }

    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String root, Optional<String> member, boolean factory, boolean constant) {
        return new PropertyValue.DartObjectReferenceValue(library, root, member, factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(constant) : Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static List<WidgetNode> children() {
        return List.of(24, 48).stream().map(height -> new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.<PropertyName, PropertyValue>of(new PropertyName("height"), new PropertyValue.IntegerValue(BigInteger.valueOf(height))),
                Map.<SlotName, WidgetSlot>of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))).toList();
    }
    private static DesignerDocument document(Map<PropertyName, PropertyValue> properties, List<WidgetNode> children) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)),
                new WidgetNode(StableId.random(), TYPE, properties, Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children))));
    }
    private static GeneratedDartRegions generated(Map<PropertyName, PropertyValue> properties, List<WidgetNode> children) {
        var result = new DartRegionGenerator().generate(document(properties, children), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow();
    }
}
