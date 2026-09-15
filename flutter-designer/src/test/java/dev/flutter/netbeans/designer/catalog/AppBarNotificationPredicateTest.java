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

class AppBarNotificationPredicateTest {
    private static final WidgetTypeId TYPE = AppBarWidgetPropertySchema.APP_BAR_TYPE;
    private static final PropertyName PROPERTY = new PropertyName("notificationPredicate");
    private static final String PREDICATE = "ScrollNotificationPredicate";
    private static final String LIBRARY = "package:app/predicates.dart";

    @Test void addsOnePredicateWithoutAddingPropertiesEventsOrChangingTheExistingPresetConstraint() {
        var definition = definition();
        assertEquals(120, definition.properties().size());
        assertEquals(28, AppBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(5, definition.slots().size());
        var property = definition.property(PROPERTY).orElseThrow();
        assertEquals(DartParameter.named(9, false), property.parameter());
        assertEquals(Set.of(PropertyValueKind.STRING, PropertyValueKind.DART_OBJECT_REFERENCE), property.acceptedKinds());
        assertTrue(property.creationDefault().isEmpty());
        assertInstanceOf(PropertyValueConstraint.StringPattern.class, property.constraints().getFirst());
        assertEquals(new PropertyValueConstraint.DartObjectReferenceValues(PREDICATE), property.constraints().getLast());
        assertEquals(AppBarWidgetPropertySchema.Group.BEHAVIOR, AppBarWidgetPropertySchema.find(PROPERTY).orElseThrow().group());
        assertEquals(List.of("default", "depthZero", "all"), AppBarWidgetPropertySchema.notificationPredicatePresets());
        var descriptor = WidgetEventCatalog.find(TYPE, PROPERTY).orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.PREDICATE, descriptor.kind());
        assertEquals(PREDICATE, descriptor.callbackType());
        assertEquals("bool Function(ScrollNotification)", descriptor.signature().dartFunctionType());
        assertEquals("bool _accept(ScrollNotification notification)", descriptor.signature().declaration("_accept"));
        assertFalse(descriptor.required());
        assertFalse(descriptor.sdkRequired());
        assertFalse(descriptor.nullableCallback());
        assertFalse(descriptor.allowsExplicitNull());
        assertFalse(descriptor.defaultEvent());
        assertTrue(descriptor.unsetBehavior().contains("depth-zero"));
        assertTrue(WidgetEventCatalog.defaultEventFor(definition).isEmpty());
        assertEquals(1, WidgetEventCatalog.eventsFor(definition).size());
        assertTrue(WidgetEventCatalog.eventsFor(definition).stream().noneMatch(value -> value.kind() == WidgetEventDescriptor.Kind.EVENT));
    }

    @Test void omissionAndAllThreePresetsRetainTheirExactGeneratedExpressionsAndCanonicalBytes() throws Exception {
        var omitted = generated(Map.of());
        assertTrue(omitted.build().payload().contains("return AppBar();"));
        assertEquals("import 'package:flutter/material.dart';\n", omitted.imports().payload());
        assertFalse(omitted.build().payload().contains("notificationPredicate:"));
        var expected = Map.of("default", "defaultScrollNotificationPredicate",
                "depthZero", "(notification) => notification.depth == 0", "all", "(_) => true");
        var codec = new FdDocumentCodec();
        for (String preset : AppBarWidgetPropertySchema.notificationPredicatePresets()) {
            var value = new PropertyValue.StringValue(preset);
            var document = document(node(Map.of(PROPERTY, value)));
            var bytes = codec.encode(document);
            var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
            assertEquals(value, restored.root().properties().get(PROPERTY));
            assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
            var output = generated(restored.root().properties());
            assertTrue(output.build().payload().contains("notificationPredicate: " + expected.get(preset)), output.build().payload());
            assertEquals(omitted.imports().payload(), output.imports().payload());
            assertTrue(output.symbolOccurrences().stream().noneMatch(s -> s.staticTypeRequirement().isPresent()));
            assertEquals(preset.equals("default"), output.symbolOccurrences().stream()
                    .anyMatch(s -> s.symbolName().equals("defaultScrollNotificationPredicate")));
            assertFalse(output.build().payload().contains("const AppBar("));
        }
    }

    @Test void everyLocalImportedRootMemberGetterFactoryShapeRetainsOneExactTypedProofAndAlias() throws Exception {
        var codec = new FdDocumentCodec();
        for (Optional<String> library : List.of(Optional.<String>empty(), Optional.of(LIBRARY))) {
            for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("accept"))) {
                for (boolean factory : List.of(false, true)) {
                    for (boolean constant : factory ? List.of(false, true) : List.of(false)) {
                        var reference = reference(library, member.isPresent() ? "Predicates" : "accept", member, factory, constant);
                        var document = document(node(Map.of(PROPERTY, reference)));
                        var bytes = codec.encode(document);
                        var restored = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
                        assertEquals(document, restored);
                        assertArrayEquals(bytes.copyBytes(), codec.encode(restored).copyBytes());
                        var output = generated(restored.root().properties());
                        var occurrences = output.symbolOccurrences().stream().filter(s -> s.staticTypeRequirement().isPresent()).toList();
                        assertEquals(1, occurrences.size(), output.symbolOccurrences().toString());
                        var occurrence = occurrences.getFirst();
                        var proof = occurrence.staticTypeRequirement().orElseThrow();
                        assertEquals(PREDICATE, proof.expectedDartType());
                        assertTrue(proof.sourceTypeOverride().isEmpty());
                        assertTrue(proof.sourceTypeBound().isEmpty());
                        assertEquals(library.orElse(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI), occurrence.libraryUri());
                        assertEquals("/root/properties/notificationPredicate/" + (member.isPresent() ? "member" : "rootSymbol"), occurrence.modelPath());
                        String prefix = library.map(uri -> output.importPlan().directives().stream().filter(d -> d.uri().equals(uri))
                                .findFirst().orElseThrow().prefix().map(p -> p + ".").orElse("")).orElse("");
                        String expression = output.build().payload().substring(proof.expressionOffset(), proof.expressionEndOffset());
                        String expected = prefix + reference.rootSymbol() + member.map(m -> "." + m).orElse("") + (factory ? "()" : "");
                        assertEquals((constant ? "const " : "") + expected, expression);
                        assertTrue(output.build().payload().contains("notificationPredicate: " + expression));
                        assertFalse(output.build().payload().contains("const AppBar("));
                        assertEquals(1, output.build().payload().split("notificationPredicate:", -1).length - 1);
                    }
                }
            }
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test void rejectsNullLegacyShorthandUnknownPresetsAndSourceInjectionBeforeGeneration() {
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_accept"),
                new PropertyValue.BooleanValue(true), new PropertyValue.StringValue("_accept"), new PropertyValue.StringValue("depth1"),
                new PropertyValue.StringValue("default; injected()"), new PropertyValue.DartExpressionValue("(_) => true"))) {
            var document = document(node(Map.of(PROPERTY, invalid)));
            assertFalse(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid(), invalid.toString());
            assertFalse(new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).successful(), invalid.toString());
        }
        for (String invalid : List.of("accept()", "x; injected", "value.member", "(notification) => true")) {
            assertThrows(IllegalArgumentException.class, () -> reference(Optional.empty(), invalid, Optional.empty(), false, false));
        }
    }

    @Test void pinnedSdkConfirmsOptionalNonNullPredicateAndDepthZeroDefault() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        assumeTrue(sdk != null && !sdk.isBlank(), "Set flutter.events.sdk for pinned SDK verification");
        String appBar = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/app_bar.dart"));
        String notification = Files.readString(Path.of(sdk, "packages/flutter/lib/src/widgets/scroll_notification.dart"));
        assertTrue(appBar.contains("this.notificationPredicate = defaultScrollNotificationPredicate,"));
        assertTrue(appBar.contains("final ScrollNotificationPredicate notificationPredicate;"));
        assertFalse(appBar.contains("final ScrollNotificationPredicate? notificationPredicate;"));
        assertTrue(notification.contains("typedef ScrollNotificationPredicate = bool Function(ScrollNotification notification);"));
        assertTrue(notification.contains("bool defaultScrollNotificationPredicate(ScrollNotification notification) {"));
        assertTrue(notification.contains("return notification.depth == 0;"));
    }

    private static PropertyValue.DartObjectReferenceValue reference(Optional<String> library, String root, Optional<String> member, boolean factory, boolean constant) {
        return new PropertyValue.DartObjectReferenceValue(library, root, member, factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(constant) : Optional.empty());
    }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        return new WidgetNode(StableId.random(), TYPE, properties, Map.of());
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
    private static GeneratedDartRegions generated(Map<PropertyName, PropertyValue> properties) {
        var result = new DartRegionGenerator().generate(document(node(properties)), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow();
    }
}
