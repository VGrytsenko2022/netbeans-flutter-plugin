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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipPathDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsDeterministicConstConstructorWithFrameworkDefaultsAndEmptyChild() {
        GeneratedDartRegions first = generate(clipPath(Map.of(), Optional.empty()));
        GeneratedDartRegions second = generate(clipPath(Map.of(), Optional.empty()));

        assertEquals(first.imports(), second.imports());
        assertEquals(first.build(), second.build());
        assertEquals(first.importPlan(), second.importPlan());
        assertEquals("import 'package:flutter/widgets.dart';\n",
                first.imports().payload());
        String build = first.build().payload();
        assertTrue(build.contains("return const ClipPath("), build);
        assertFalse(build.contains("clipBehavior:"), build);
        assertFalse(build.contains("clipper:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void emitsEveryClosedClipBehaviorBeforeTheOptionalConstChild() {
        for (String value : new String[] {
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"}) {
            GeneratedDartRegions generated = generate(clipPath(
                    Map.of(new PropertyName("clipBehavior"),
                            new PropertyValue.EnumValue("Clip", value)),
                    Optional.of(text("Visible"))));

            String build = generated.build().payload();
            String behavior = "clipBehavior: Clip." + value + ',';
            assertTrue(build.contains("return const ClipPath("), build);
            assertTrue(build.contains(behavior), build);
            assertTrue(build.contains("child: const Text('Visible')"), build);
            assertTrue(build.indexOf(behavior)
                    < build.indexOf("child: const Text('Visible')"), build);
            assertFalse(build.contains("clipper:"), build);
        }
    }

    @Test
    void emitsImportedConstClipperInvocationWithAliasedImportAndProvenance() {
        String library = "package:sample/clippers/path_clipper.dart";
        PropertyValue.DartObjectReferenceValue clipper =
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "PathClipper", Optional.of("create"),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true));

        GeneratedDartRegions generated = generate(clipPath(
                Map.of(new PropertyName("clipper"), clipper),
                Optional.of(text("Visible"))));
        DartImportDirective directive = generated.importPlan().directives().stream()
                .filter(value -> value.uri().equals(library))
                .findFirst().orElseThrow();
        String prefix = directive.prefix().orElseThrow();

        assertEquals(List.of("package:flutter/widgets.dart", library),
                generated.importPlan().directives().stream()
                        .map(DartImportDirective::uri).toList());
        assertTrue(generated.imports().payload().contains(
                "import '" + library + "' as " + prefix + ";"),
                generated.imports().payload());
        String build = generated.build().payload();
        assertTrue(build.contains("return const ClipPath("), build);
        assertTrue(build.contains(
                "clipper: const " + prefix + ".PathClipper.create()"), build);
        assertTrue(build.indexOf("clipper:") < build.indexOf("child:"), build);
        List<GeneratedDartSymbolOccurrence> occurrences = generated
                .symbolOccurrences().stream()
                .filter(value -> value.modelPath()
                        .startsWith("/root/properties/clipper/"))
                .toList();
        assertEquals(List.of("PathClipper", "create"), occurrences.stream()
                .map(GeneratedDartSymbolOccurrence::symbolName).toList());
        assertTrue(occurrences.stream()
                .allMatch(value -> value.libraryUri().equals(library)));
        GeneratedDartStaticTypeRequirement requirement = occurrences.stream()
                .flatMap(value -> value.staticTypeRequirement().stream())
                .findFirst().orElseThrow();
        assertEquals("CustomClipper<Path>", requirement.expectedDartType());
        assertEquals("const " + prefix + ".PathClipper.create()",
                build.substring(
                        requirement.expressionOffset(),
                        requirement.expressionEndOffset()));
        assertEquals(1, occurrences.stream()
                .filter(value -> value.staticTypeRequirement().isPresent())
                .count());
    }

    @Test
    void removesOnlyTheOuterConstWhenTheChildIsNotConstCapable() {
        WidgetNode container = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Container"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        String build = generate(clipPath(Map.of(), Optional.of(container)))
                .build().payload();

        assertTrue(build.contains("return ClipPath("), build);
        assertFalse(build.contains("return const ClipPath("), build);
        assertTrue(build.contains("child: Container("), build);
    }

    private static WidgetNode clipPath(
            Map<PropertyName, PropertyValue> properties,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ClipPath"),
                properties,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    @Test
    void shapeHelperNeverUsesConstAndRetainsTypedProofAndMethodProvenance() {
        for (boolean constant : List.of(false, true)) {
            String library = "package:sample/shapes.dart";
            var shape = new PropertyValue.DartObjectReferenceValue(
                    Optional.of(library), "SampleShape", Optional.of("create"),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                    Optional.of(constant));
            for (String mode : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                GeneratedDartRegions generated = generate(clipPath(Map.of(
                        new PropertyName("shape"), shape,
                        new PropertyName("clipBehavior"), new PropertyValue.EnumValue("Clip", mode)),
                        Optional.of(text("Visible"))));
                String build = generated.build().payload();
                String prefix = prefix(generated, library);
                String expression = (constant ? "const " : "") + prefix + ".SampleShape.create()";
                assertTrue(build.contains("return ClipPath.shape("), build);
                assertFalse(build.contains("const ClipPath"), build);
                assertFalse(build.contains("clipper:"), build);
                assertTrue(build.contains("shape: " + expression), build);
                assertTrue(build.contains("clipBehavior: Clip." + mode), build);
                assertTrue(build.indexOf("shape:") < build.indexOf("clipBehavior:"), build);
                assertTrue(build.indexOf("clipBehavior:") < build.indexOf("child:"), build);
                assertStaticType(generated, "/root/properties/shape/", "ShapeBorder", expression);
                List<GeneratedDartSymbolOccurrence> factory = generated.symbolOccurrences().stream()
                        .filter(value -> value.id().endsWith(":shapeFactory")).toList();
                assertEquals(1, factory.size());
                assertEquals("shape", factory.getFirst().symbolName());
                assertEquals("package:flutter/widgets.dart", factory.getFirst().libraryUri());
                assertAllOffsets(generated);
            }
        }
    }

    @Test
    void shapeHelperPropagatesNonConstToAncestorsWhilePreservingConstantDescendants() {
        var shape = new PropertyValue.DartObjectReferenceValue(
                Optional.of("package:flutter/painting.dart"), "CircleBorder", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                Optional.of(true));
        WidgetNode shaped = clipPath(Map.of(new PropertyName("shape"), shape),
                Optional.of(text("Child")));
        String build = generate(clipPath(Map.of(), Optional.of(shaped))).build().payload();
        assertTrue(build.contains("return ClipPath("), build);
        assertTrue(build.contains("child: ClipPath.shape("), build);
        assertFalse(build.contains("const ClipPath"), build);
        assertTrue(build.contains("child: const Text('Child')"), build);
    }

    @Test
    void currentReferencesAndNonConstInvocationsCarryExactClipperAndShapeTypeRequirements() {
        for (String property : List.of("clipper", "shape")) {
            for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
                var reference = new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "_registry", Optional.of("selected"), access,
                        access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                                ? Optional.empty() : Optional.of(false));
                var generated = generate(clipPath(Map.of(new PropertyName(property), reference),
                        Optional.empty()));
                String expression = "_registry.selected"
                        + (access == PropertyValue.DartObjectReferenceValue.Access.REFERENCE ? "" : "()");
                assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
                String build = generated.build().payload();
                assertFalse(build.contains("const ClipPath"), build);
                assertTrue(build.contains(property + ": " + expression), build);
                assertStaticType(generated, "/root/properties/" + property + "/",
                        property.equals("clipper") ? "CustomClipper<Path>" : "ShapeBorder", expression);
                assertAllOffsets(generated);
            }
        }
    }

    @Test
    void deduplicatesImportsAndDisambiguatesSameNamedProjectSymbolsAcrossBothBranches() {
        String first = "package:sample/first.dart";
        String second = "package:sample/second.dart";
        WidgetNode nested = clipPath(Map.of(new PropertyName("shape"), importedReference(first)),
                Optional.of(clipPath(Map.of(new PropertyName("clipper"), importedReference(second)),
                        Optional.empty())));
        GeneratedDartRegions generated = generate(clipPath(
                Map.of(new PropertyName("clipper"), importedReference(first)), Optional.of(nested)));
        String firstPrefix = prefix(generated, first);
        String secondPrefix = prefix(generated, second);
        assertFalse(firstPrefix.equals(secondPrefix));
        assertEquals(List.of("package:flutter/widgets.dart", first, second), generated.importPlan()
                .directives().stream().map(DartImportDirective::uri).toList());
        String build = generated.build().payload();
        assertTrue(build.contains("clipper: " + firstPrefix + ".Objects.selected"), build);
        assertTrue(build.contains("shape: " + firstPrefix + ".Objects.selected"), build);
        assertTrue(build.contains("clipper: " + secondPrefix + ".Objects.selected"), build);
        assertAllOffsets(generated);
    }

    @Test
    void supportsInlineShapeFactoryWhenChildSlotIsOmitted() {
        WidgetNode node = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.ClipPath"),
                Map.of(new PropertyName("shape"), new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "shape", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())),
                Map.of(), Extensions.empty());
        GeneratedDartRegions generated = generate(node);
        assertTrue(generated.build().payload().contains("return ClipPath.shape(shape: shape);"),
                generated.build().payload());
        assertAllOffsets(generated);
        assertStaticType(generated, "/root/properties/shape/", "ShapeBorder", "shape");
    }

    @Test
    void conflictingBranchesFailBeforeAnyDartIsGenerated() {
        WidgetNode node = clipPath(Map.of(
                new PropertyName("clipper"), importedReference("package:sample/clipper.dart"),
                new PropertyName("shape"), importedReference("package:sample/shape.dart")), Optional.empty());
        DartGenerationResult result = new DartRegionGenerator().generate(document(node),
                BuiltInWidgetCatalog.getDefault());
        assertFalse(result.successful());
        assertTrue(result.generated().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.toString().contains("/root/properties/shape")), result.diagnostics().toString());
    }

    @Test
    void helperProvenanceUsesTheEffectiveMaterialUmbrellaLibrary() {
        WidgetNode shaped = clipPath(Map.of(new PropertyName("shape"), new PropertyValue.DartObjectReferenceValue(
                Optional.empty(), "shape", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())), Optional.empty());
        WidgetNode scaffold = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Scaffold"),
                Map.of(), Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(shaped)));
        GeneratedDartRegions generated = generate(scaffold);
        assertEquals("import 'package:flutter/material.dart';\n", generated.imports().payload());
        var factory = generated.symbolOccurrences().stream()
                .filter(occurrence -> occurrence.id().endsWith(":shapeFactory")).findFirst().orElseThrow();
        assertEquals("package:flutter/material.dart", factory.libraryUri());
        assertAllOffsets(generated);
    }

    private static PropertyValue.DartObjectReferenceValue importedReference(String uri) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of(uri), "Objects", Optional.of("selected"),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }

    private static String prefix(GeneratedDartRegions generated, String uri) {
        return generated.importPlan().directives().stream().filter(directive -> directive.uri().equals(uri))
                .findFirst().orElseThrow().prefix().orElseThrow();
    }

    private static void assertStaticType(GeneratedDartRegions generated,
            String path, String type, String expression) {
        var requirements = generated.symbolOccurrences().stream()
                .filter(occurrence -> occurrence.modelPath().startsWith(path))
                .flatMap(occurrence -> occurrence.staticTypeRequirement().stream()).toList();
        assertEquals(1, requirements.size());
        var requirement = requirements.getFirst();
        assertEquals(type, requirement.expectedDartType());
        assertEquals(expression, generated.build().payload().substring(
                requirement.expressionOffset(), requirement.expressionEndOffset()));
    }

    private static void assertAllOffsets(GeneratedDartRegions generated) {
        for (var occurrence : generated.symbolOccurrences()) {
            String payload = occurrence.region() == DartManagedRegionId.BUILD
                    ? generated.build().payload() : generated.imports().payload();
            assertEquals(occurrence.symbolName(), payload.substring(occurrence.offset(), occurrence.endOffset()),
                    occurrence.toString());
        }
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
