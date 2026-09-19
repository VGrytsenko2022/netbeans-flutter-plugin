package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRRectDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsDeterministicConstConstructorWithFrameworkDefaultsAndEmptyChild() {
        GeneratedDartRegions first = generate(clipRRect(Map.of(), Optional.empty()));
        GeneratedDartRegions second = generate(clipRRect(Map.of(), Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const ClipRRect("), build);
        assertFalse(build.contains("borderRadius:"), build);
        assertFalse(build.contains("clipBehavior:"), build);
        assertFalse(build.contains("clipper:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void emitsPhysicalEllipticalCornersInConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyName("borderRadius"), new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius("1", "2"), radius("3", "4"),
                        radius("5", "6"), radius("7", "8"))));
        properties.put(new PropertyName("clipBehavior"),
                new PropertyValue.EnumValue("Clip", "hardEdge"));

        String build = generate(clipRRect(properties, Optional.of(text("Visible"))))
                .build().payload();

        assertTrue(build.contains("return const ClipRRect("), build);
        assertTrue(build.contains("borderRadius: const BorderRadius.only("), build);
        assertTrue(build.contains("topLeft: const Radius.elliptical(1.0, 2.0)"), build);
        assertTrue(build.contains("topRight: const Radius.elliptical(3.0, 4.0)"), build);
        assertTrue(build.contains("bottomRight: const Radius.elliptical(5.0, 6.0)"), build);
        assertTrue(build.contains("bottomLeft: const Radius.elliptical(7.0, 8.0)"), build);
        assertTrue(build.contains("clipBehavior: Clip.hardEdge"), build);
        assertTrue(build.contains("child: const Text('Visible')"), build);
        assertTrue(build.indexOf("borderRadius:") < build.indexOf("clipBehavior:"), build);
        assertTrue(build.indexOf("clipBehavior:") < build.indexOf("child:"), build);
        assertFalse(build.contains("clipper:"), build);
    }

    @Test
    void emitsDirectionalGeometryAndEveryClosedClipBehavior() {
        PropertyValue.BorderRadiusValue directional = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        radius("1.5", "2.5"), radius("3.5", "4.5"),
                        radius("5.5", "6.5"), radius("7.5", "8.5")));
        for (String value : new String[] {
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"}) {
            String build = generate(clipRRect(
                    Map.of(
                            new PropertyName("borderRadius"), directional,
                            new PropertyName("clipBehavior"),
                            new PropertyValue.EnumValue("Clip", value)),
                    Optional.empty())).build().payload();

            assertTrue(build.contains(
                    "borderRadius: const BorderRadiusDirectional.only("), build);
            assertTrue(build.contains(
                    "topStart: const Radius.elliptical(1.5, 2.5)"), build);
            assertTrue(build.contains(
                    "topEnd: const Radius.elliptical(3.5, 4.5)"), build);
            assertTrue(build.contains(
                    "bottomEnd: const Radius.elliptical(5.5, 6.5)"), build);
            assertTrue(build.contains(
                    "bottomStart: const Radius.elliptical(7.5, 8.5)"), build);
            assertTrue(build.contains("clipBehavior: Clip." + value), build);
            assertTrue(build.indexOf("borderRadius:") < build.indexOf("clipBehavior:"), build);
            assertTrue(build.indexOf("clipBehavior:") < build.indexOf("child:"), build);
        }
    }

    @Test
    void emitsCurrentLibraryReferencesWithoutAddingAnImport() {
        PropertyValue.DartObjectReferenceValue clipper =
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "_clipperRegistry",
                        Optional.of("rounded"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty());

        GeneratedDartRegions generated = generate(clipRRect(
                Map.of(new PropertyName("clipper"), clipper), Optional.empty()));

        assertEquals("import 'package:flutter/widgets.dart';\n",
                generated.imports().payload());
        String build = generated.build().payload();
        assertTrue(build.contains("return ClipRRect("), build);
        assertFalse(build.contains("return const ClipRRect("), build);
        assertTrue(build.contains("clipper: _clipperRegistry.rounded"), build);
        assertEquals(List.of("_clipperRegistry", "rounded"),
                clipperOccurrences(generated).stream()
                        .map(GeneratedDartSymbolOccurrence::symbolName).toList());
        assertTrue(clipperOccurrences(generated).stream().allMatch(occurrence ->
                occurrence.libraryUri().equals(
                        DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI)));
        assertEquals(List.of(
                        "/root/properties/clipper/rootSymbol",
                        "/root/properties/clipper/member"),
                clipperOccurrences(generated).stream()
                        .map(GeneratedDartSymbolOccurrence::modelPath).toList());
        assertStaticTypeRequirement(
                generated, "_clipperRegistry.rounded", "CustomClipper<RRect>");
    }

    @Test
    void emitsImportedReferenceAndConstInvocationWithDeterministicAliasedImport() {
        String library = "package:sample/clippers/rrect_clipper.dart";
        PropertyValue.DartObjectReferenceValue clipper =
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "RoundedClipper",
                        Optional.of("create"),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true));

        GeneratedDartRegions first = generate(clipRRect(
                Map.of(new PropertyName("clipper"), clipper), Optional.empty()));
        GeneratedDartRegions second = generate(clipRRect(
                Map.of(new PropertyName("clipper"), clipper), Optional.empty()));
        DartImportDirective directive = first.importPlan().directives().stream()
                .filter(value -> value.uri().equals(library))
                .findFirst().orElseThrow();
        String prefix = directive.prefix().orElseThrow();

        assertTrue(prefix.matches("_nbfd_[0-9a-f]{12}"), prefix);
        assertEquals(first.imports(), second.imports());
        assertEquals(List.of("package:flutter/widgets.dart", library),
                first.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        assertTrue(first.imports().payload().contains(
                "import '" + library + "' as " + prefix + ";"),
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const ClipRRect("), build);
        assertTrue(build.contains(
                "clipper: const " + prefix + ".RoundedClipper.create()"), build);
        assertEquals(List.of("RoundedClipper", "create"),
                clipperOccurrences(first).stream()
                        .map(GeneratedDartSymbolOccurrence::symbolName).toList());
        assertTrue(clipperOccurrences(first).stream()
                .allMatch(occurrence -> occurrence.libraryUri().equals(library)));
        assertTrue(clipperOccurrences(first).stream().allMatch(occurrence ->
                build.substring(occurrence.offset(), occurrence.endOffset())
                        .equals(occurrence.symbolName())));
        assertStaticTypeRequirement(
                first,
                "const " + prefix + ".RoundedClipper.create()",
                "CustomClipper<RRect>");
    }

    @Test
    void nonConstInvocationRemovesOuterConstAndKeepsConstructorOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyName("clipBehavior"),
                new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer"));
        properties.put(new PropertyName("clipper"),
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "LocalClipper", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(false)));
        properties.put(new PropertyName("borderRadius"), uniformPhysicalRadius("6"));

        String build = generate(clipRRect(properties, Optional.of(text("Child"))))
                .build().payload();

        assertTrue(build.contains("return ClipRRect("), build);
        assertFalse(build.contains("return const ClipRRect("), build);
        assertTrue(build.contains("clipper: LocalClipper()"), build);
        assertTrue(build.indexOf("borderRadius:") < build.indexOf("clipper:"), build);
        assertTrue(build.indexOf("clipper:") < build.indexOf("clipBehavior:"), build);
        assertTrue(build.indexOf("clipBehavior:") < build.indexOf("child:"), build);
    }

    @Test
    void valueDrivenProjectImportCountsTowardTheImportLimit() {
        WidgetNode root = clipRRect(
                Map.of(new PropertyName("clipper"),
                        new PropertyValue.DartObjectReferenceValue(
                                Optional.of("package:sample/rrect_clipper.dart"),
                                "RoundedClipper", Optional.empty(),
                                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                                Optional.empty())),
                Optional.empty());
        DartGenerationResult result = new DartRegionGenerator(
                new DartGenerationLimits(100_000, 1, 1_000))
                .generate(document(root), BuiltInWidgetCatalog.getDefault());

        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        assertEquals(DartGenerationDiagnosticCode.IMPORT_LIMIT,
                result.diagnostics().getFirst().code());
    }

    @Test
    void deduplicatesImportsAndAliasesSameNamedSymbolsFromDifferentLibraries() {
        String firstLibrary = "package:sample/clippers/first.dart";
        String secondLibrary = "package:sample/clippers/second.dart";
        WidgetNode repeatedFirst = clipRRect(
                Map.of(new PropertyName("clipper"), importedReference(firstLibrary)),
                Optional.empty());
        WidgetNode second = clipRRect(
                Map.of(new PropertyName("clipper"), importedReference(secondLibrary)),
                Optional.of(repeatedFirst));
        WidgetNode root = clipRRect(
                Map.of(new PropertyName("clipper"), importedReference(firstLibrary)),
                Optional.of(second));

        GeneratedDartRegions generated = generate(root);
        assertEquals(List.of(
                        "package:flutter/widgets.dart", firstLibrary, secondLibrary),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        String firstPrefix = prefix(generated, firstLibrary);
        String secondPrefix = prefix(generated, secondLibrary);
        assertFalse(firstPrefix.equals(secondPrefix));
        String imports = generated.imports().payload();
        assertEquals(1, count(imports, "import '" + firstLibrary + "'"));
        assertEquals(1, count(imports, "import '" + secondLibrary + "'"));
        String build = generated.build().payload();
        assertEquals(2, count(build,
                "clipper: " + firstPrefix + ".RoundedClipper.instance"));
        assertEquals(1, count(build,
                "clipper: " + secondPrefix + ".RoundedClipper.instance"));
    }

    @Test
    void removesOnlyTheOuterConstWhenTheChildIsNotConstCapable() {
        WidgetNode container = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Container"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        String build = generate(clipRRect(
                Map.of(new PropertyName("borderRadius"), uniformPhysicalRadius("8")),
                Optional.of(container))).build().payload();

        assertTrue(build.contains("return ClipRRect("), build);
        assertFalse(build.contains("return const ClipRRect("), build);
        assertTrue(build.contains("borderRadius: const BorderRadius.only("), build);
        assertTrue(build.contains("child: Container("), build);
    }

    private static WidgetNode clipRRect(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ClipRRect"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static PropertyValue.BorderRadiusValue uniformPhysicalRadius(String value) {
        PropertyValue.BoxDecorationValue.Radius radius = radius(value, value);
        return new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius, radius, radius, radius));
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(
            String x, String y) {
        return new PropertyValue.BoxDecorationValue.Radius(
                new BigDecimal(x), new BigDecimal(y));
    }

    private static List<GeneratedDartSymbolOccurrence> clipperOccurrences(
            GeneratedDartRegions generated) {
        return generated.symbolOccurrences().stream()
                .filter(occurrence -> occurrence.modelPath()
                        .startsWith("/root/properties/clipper/"))
                .toList();
    }

    private static void assertStaticTypeRequirement(
            GeneratedDartRegions generated,
            String expectedExpression,
            String expectedType) {
        GeneratedDartStaticTypeRequirement requirement = clipperOccurrences(generated)
                .stream()
                .flatMap(occurrence -> occurrence.staticTypeRequirement().stream())
                .reduce((left, right) -> {
                    throw new AssertionError("multiple clipper static-type requirements");
                })
                .orElseThrow();
        assertEquals(expectedType, requirement.expectedDartType());
        assertEquals(expectedExpression, generated.build().payload().substring(
                requirement.expressionOffset(),
                requirement.expressionEndOffset()));
    }

    private static PropertyValue.DartObjectReferenceValue importedReference(
            String library) {
        return new PropertyValue.DartObjectReferenceValue(
                Optional.of(library), "RoundedClipper", Optional.of("instance"),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                Optional.empty());
    }

    private static String prefix(GeneratedDartRegions generated, String library) {
        return generated.importPlan().directives().stream()
                .filter(directive -> directive.uri().equals(library))
                .findFirst().orElseThrow().prefix().orElseThrow();
    }

    private static int count(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(
                Optional.empty(),
                StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart",
                        "Sample",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(
                                new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))),
                Optional.empty(),
                root,
                Extensions.empty());
    }
}
