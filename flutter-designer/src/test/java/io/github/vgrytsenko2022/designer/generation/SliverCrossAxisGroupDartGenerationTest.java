package io.github.vgrytsenko2022.designer.generation;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverCrossAxisGroupDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    @Test void nestedAndEmptyGroupsRetainOrderSlotsIdentitiesAndConstEligibility() throws Exception {
        for(boolean empty:List.of(false,true)) {
            var inner=group(List.of(sliverToBoxAdapter(text("Second"))));
            var outer=group(empty?List.of():List.of(sliverToBoxAdapter(text("First")),inner,sliverToBoxAdapter(text("Third"))));
            var doc=document(customScrollView(Map.of(),List.of(outer)));
            var codec=new io.github.vgrytsenko2022.designer.codec.FdDocumentCodec();
            var decoded=assertInstanceOf(io.github.vgrytsenko2022.designer.codec.FdDecodeResult.Current.class,codec.decode(codec.encode(doc)));
            assertEquals(doc,decoded.document());
            var code=generate(decoded.document().root()).build().payload();
            assertTrue(code.contains("const SliverCrossAxisGroup("),code);
            assertTrue(code.contains("slivers:"),code);
            assertFalse(code.contains("children:"),code);
            if(empty) assertTrue(code.contains("slivers: []"),code);
            else {
                assertTrue(code.indexOf("'First'")<code.indexOf("'Second'"),code);
                assertTrue(code.indexOf("'Second'")<code.indexOf("'Third'"),code);
            }
        }
    }
    private static WidgetNode group(List<WidgetNode> children) {
        return new WidgetNode(StableId.random(),SliverCrossAxisGroupWidgetPropertySchema.TYPE,Map.of(),
                Map.of(new SlotName("slivers"),new WidgetSlot.ListSlot(children)));
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

