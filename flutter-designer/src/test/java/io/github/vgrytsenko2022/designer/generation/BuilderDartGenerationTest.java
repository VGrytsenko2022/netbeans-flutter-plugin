package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.catalog.BuilderWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderDartGenerationTest {

    @Test
    void emitsSafeNoopBuilderForPaletteCreationWithoutConstMisclassification() {
        WidgetNode builder = node(new PropertyValue.CallbackValue("noop"));
        var result = new DartRegionGenerator().generate(document(builder),
                BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("Builder(builder: (_) => const SizedBox.shrink())"), build);
        assertFalse(build.contains("const Builder("), build);
        assertEquals(0, result.generated().orElseThrow().symbolOccurrences().stream()
                .filter(occurrence -> occurrence.modelPath().contains("builder"))
                .count());
    }

    @Test
    void emitsProjectCallbackIdentifierWithoutExecutingItsBody() {
        var callback = new PropertyValue.CallbackValue("buildPreview");
        var result = new DartRegionGenerator().generate(
                document(node(callback)), BuiltInWidgetCatalog.getDefault());

        assertTrue(result.successful(), result.diagnostics().toString());
        var generated = result.generated().orElseThrow();
        assertTrue(generated.build().payload().contains("Builder(builder: buildPreview)"),
                generated.build().payload());
        var occurrence = generated.symbolOccurrences().stream()
                .filter(value -> value.modelPath().equals("/root/properties/builder/rootSymbol"))
                .findFirst().orElseThrow();
        assertEquals(BuilderWidgetPropertySchema.CALLBACK_TYPE,
                occurrence.staticTypeRequirement().orElseThrow().expectedDartType());
        assertEquals(DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI, occurrence.libraryUri());
    }

    private static WidgetNode node(PropertyValue callback) {
        return new WidgetNode(StableId.random(), BuilderWidgetPropertySchema.BUILDER_TYPE,
                Map.of(new PropertyName("builder"), callback), Map.of(), Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        var regions = new ManagedRegions(new ManagedRegion("0".repeat(64)),
                new ManagedRegion("0".repeat(64)));
        return new DesignerDocument(StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), regions), root);
    }
}
