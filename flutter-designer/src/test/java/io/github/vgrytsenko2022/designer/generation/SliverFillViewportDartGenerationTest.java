package io.github.vgrytsenko2022.designer.generation;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverFillViewportDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    @Test void generatedDelegatesPreserveEveryFlagChildAndRoundTrip() throws Exception {
        for (var type : SliverFillViewportWidgetPropertySchema.TYPES)
        for (Boolean pad : Arrays.asList(null, false, true))
        for (Boolean implicit : Arrays.asList(null, false, true))
        for (String fraction : List.of("0.5", "1", "1.5"))
        for (boolean empty : List.of(false, true)) {
            var definition = BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
            var props = new LinkedHashMap<>(prototype.properties());
            props.put(name("viewportFraction"), new PropertyValue.DoubleValue(new java.math.BigDecimal(fraction)));
            if (pad != null) props.put(name("padEnds"), new PropertyValue.BooleanValue(pad));
            if (implicit != null) props.put(name("allowImplicitScrolling"), new PropertyValue.BooleanValue(implicit));
            boolean delegate = type.equals(SliverFillViewportWidgetPropertySchema.DELEGATE);
            if (!delegate) {
                props.put(name("addAutomaticKeepAlives"), new PropertyValue.BooleanValue(false));
                props.put(name("addRepaintBoundaries"), new PropertyValue.BooleanValue(false));
                props.put(name("addSemanticIndexes"), new PropertyValue.BooleanValue(false));
                props.put(name("semanticIndexOffset"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(4)));
            }
            var slots = delegate ? Map.<SlotName,WidgetSlot>of() : Map.<SlotName,WidgetSlot>of(new SlotName("children"),
                    new WidgetSlot.ListSlot(empty ? List.of() : List.of(text("First"), text("Second"))));
            var doc = document(customScrollView(Map.of(), List.of(new WidgetNode(prototype.id(), type, props, slots))));
            var codec = new io.github.vgrytsenko2022.designer.codec.FdDocumentCodec();
            var decoded = assertInstanceOf(io.github.vgrytsenko2022.designer.codec.FdDecodeResult.Current.class, codec.decode(codec.encode(doc)));
            assertEquals(doc, decoded.document());
            var code = generate(decoded.document().root()).build().payload();
            assertTrue(code.contains("SliverFillViewport("), code);
            assertTrue(code.contains("delegate: SliverChildListDelegate("), code);
            assertFalse(code.contains("SliverFillViewport.delegate"), code);
            assertEquals(pad != null, code.contains("padEnds:"), code);
            assertEquals(implicit != null, code.contains("allowImplicitScrolling:"), code);
            assertEquals(!delegate, code.contains("semanticIndexOffset: 4"), code);
            assertEquals(!delegate && !empty, code.contains("'First'"), code);
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






