package dev.flutter.netbeans.designer.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConstrainedBoxDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsRequiredFiniteAndUnboundedBoundsBeforeTheOptionalChild() {
        PropertyValue.BoxConstraintsValue constraints =
                new PropertyValue.BoxConstraintsValue(
                        new BigDecimal("10"), Optional.of(new BigDecimal("100")),
                        new BigDecimal("20"), Optional.empty());
        String build = generate(node(constraints, Optional.of(text("Inside"))));

        assertTrue(build.contains("return ConstrainedBox("), build);
        assertFalse(build.contains("return const ConstrainedBox("), build);
        assertTrue(build.contains("constraints: const BoxConstraints("), build);
        assertTrue(build.contains("minWidth: 10.0"), build);
        assertTrue(build.contains("maxWidth: 100.0"), build);
        assertTrue(build.contains("minHeight: 20.0"), build);
        assertFalse(build.contains("maxHeight:"), build);
        assertTrue(build.contains("child: const Text('Inside')"), build);
        assertTrue(build.indexOf("constraints:") < build.indexOf("child:"), build);
    }

    @Test
    void emitsDoubleInfinityForEachExpandingMinimumWithoutRedundantMaximum() {
        PropertyValue.BoxConstraintsValue constraints =
                new PropertyValue.BoxConstraintsValue(
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE);
        String build = generate(node(constraints, Optional.empty()));

        assertTrue(build.contains("minWidth: double.infinity"), build);
        assertTrue(build.contains("minHeight: double.infinity"), build);
        assertFalse(build.contains("maxWidth:"), build);
        assertFalse(build.contains("maxHeight:"), build);
        assertTrue(build.contains("child: null"), build);
    }

    @Test
    void refusesToGenerateWhenTheRequiredConstraintsArgumentIsMissing() {
        WidgetNode invalid = new WidgetNode(
                StableId.random(), new WidgetTypeId("flutter.widgets.ConstrainedBox"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());

        DartGenerationResult result = new DartRegionGenerator().generate(
                document(invalid), BuiltInWidgetCatalog.getDefault());

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.message().contains("constraints")),
                () -> result.diagnostics().toString());
    }

    private static WidgetNode node(
            PropertyValue.BoxConstraintsValue constraints,
            Optional<WidgetNode> child) {
        return new WidgetNode(
                StableId.random(), new WidgetTypeId("flutter.widgets.ConstrainedBox"),
                Map.of(new PropertyName("constraints"), constraints),
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(child)),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)),
                Map.of(), Extensions.empty());
    }

    private static String generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow().build().payload();
    }

    private static DesignerDocument document(WidgetNode root) {
        return new DesignerDocument(
                Optional.empty(), StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(
                                new ManagedRegion(ZERO_HASH),
                                new ManagedRegion(ZERO_HASH))),
                Optional.empty(), root, Extensions.empty());
    }
}
