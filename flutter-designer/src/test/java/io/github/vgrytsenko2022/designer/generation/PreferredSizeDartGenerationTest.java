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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferredSizeDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsTypedPreferredSizeAndRequiredChild() {
        WidgetNode preferred = new WidgetNode(StableId.random(),
                new WidgetTypeId("flutter.widgets.PreferredSize"),
                Map.of(new PropertyName("preferredSize"),
                        new PropertyValue.SizeValue(BigDecimal.valueOf(120), BigDecimal.valueOf(64))),
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text("Toolbar")))),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(preferred), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        String build = result.generated().orElseThrow().build().payload();
        assertTrue(build.contains("const PreferredSize("), build);
        assertTrue(build.contains("preferredSize: Size(120.0, 64.0)"), build);
        assertTrue(build.contains("child: const Text('Toolbar')"), build);
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)),
                Map.of(), Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(Optional.empty(), StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))), Optional.empty(), root,
                Extensions.empty());
    }
}
