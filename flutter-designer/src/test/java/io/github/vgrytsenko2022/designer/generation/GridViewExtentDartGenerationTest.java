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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridViewExtentDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsNamedExtentConstructorWithRequiredExtentAndOrderedChildren() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("maxCrossAxisExtent"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(180)));
        properties.put(name("crossAxisSpacing"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(6)));
        WidgetNode grid = grid(properties, List.of(text("First")));

        GeneratedDartRegions generated = generate(grid);
        String build = generated.build().payload();

        assertTrue(build.contains("child: GridView.extent("), build);
        assertFalse(build.contains("GridView.count("), build);
        assertTrue(build.contains("maxCrossAxisExtent: 180.0"), build);
        assertTrue(build.contains("crossAxisSpacing: 6.0"), build);
        assertTrue(build.contains("children: ["), build);
        assertTrue(build.contains("const Text('First')"), build);
    }

    @Test
    void preservesRequiredDefaultAndUsesBoundedViewportGuardAtRoot() {
        WidgetNode grid = grid(Map.of(name("maxCrossAxisExtent"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(200))), List.of());
        String build = generate(grid).build().payload();

        assertTrue(build.contains("return LayoutBuilder("), build);
        assertTrue(build.contains("child: GridView.extent("), build);
        assertTrue(build.contains("maxCrossAxisExtent: 200.0"), build);
        assertFalse(build.contains("GridView.count("), build);
    }

    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode grid(Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> children) {
        return new WidgetNode(StableId.random(),
                new WidgetTypeId("flutter.widgets.GridView.extent"), properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)), Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(Optional.empty(), StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))), Optional.empty(), root,
                Extensions.empty());
    }

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }
}
