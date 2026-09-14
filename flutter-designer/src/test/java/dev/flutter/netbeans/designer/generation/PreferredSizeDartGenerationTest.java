package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
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
