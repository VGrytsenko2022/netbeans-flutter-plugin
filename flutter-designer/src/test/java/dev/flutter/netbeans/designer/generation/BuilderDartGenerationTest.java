package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuilderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
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
