package io.github.vgrytsenko2022.designer.generation;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverFillRemainingDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    @Test void allBooleanStatesAndOptionalChildGenerateExactlyAndRoundTrip() throws Exception {
        for (Boolean scroll : Arrays.asList(null, false, true)) for (Boolean overscroll : Arrays.asList(null, false, true))
            for (boolean empty : List.of(false, true)) {
                var props = new LinkedHashMap<PropertyName, PropertyValue>();
                if (scroll != null) props.put(name("hasScrollBody"), new PropertyValue.BooleanValue(scroll));
                if (overscroll != null) props.put(name("fillOverscroll"), new PropertyValue.BooleanValue(overscroll));
                var node = new WidgetNode(StableId.random(), SliverFillRemainingWidgetPropertySchema.TYPE, props,
                        Map.of(new SlotName("child"), empty ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(text("Body"))));
                var doc = document(customScrollView(Map.of(), List.of(node)));
                var codec = new io.github.vgrytsenko2022.designer.codec.FdDocumentCodec();
                var decoded = assertInstanceOf(io.github.vgrytsenko2022.designer.codec.FdDecodeResult.Current.class, codec.decode(codec.encode(doc)));
                assertEquals(doc, decoded.document());
                var code = generate(decoded.document().root()).build().payload();
                assertTrue(code.contains("const SliverFillRemaining("), code);
                assertEquals(scroll != null, code.contains("hasScrollBody:"), code);
                assertEquals(overscroll != null, code.contains("fillOverscroll:"), code);
                if (scroll != null) assertTrue(code.contains("hasScrollBody: " + scroll), code);
                if (overscroll != null) assertTrue(code.contains("fillOverscroll: " + overscroll), code);
                assertEquals(!empty, code.contains("child: const Text("), code);
            }
    }
    private static GeneratedDartRegions generate(WidgetNode root) {
        DartGenerationResult result = new DartRegionGenerator().generate(
                document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static WidgetNode customScrollView(
            Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> slivers) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.CustomScrollView"),
                properties,
                Map.of(new SlotName("slivers"), new WidgetSlot.ListSlot(slivers)),
                Extensions.empty());
    }

    private static WidgetNode sliverToBoxAdapter(WidgetNode child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                Map.of(),
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(child))),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
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

    private static PropertyName name(String value) {
        return new PropertyName(value);
    }
}




