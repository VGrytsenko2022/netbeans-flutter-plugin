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
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomScrollViewDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);

    @Test
    void emitsCustomScrollViewWithTypedSliverChildrenAndReviewedArguments() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(name("scrollDirection"),
                new PropertyValue.EnumValue("Axis", "horizontal"));
        properties.put(name("reverse"), new PropertyValue.BooleanValue(true));
        properties.put(name("shrinkWrap"), new PropertyValue.BooleanValue(true));
        properties.put(name("anchor"),
                new PropertyValue.DoubleValue(BigDecimal.valueOf(0.25)));
        properties.put(name("semanticChildCount"),
                new PropertyValue.IntegerValue(BigInteger.ONE));
        properties.put(name("restorationId"),
                new PropertyValue.StringValue("feed-scroll"));
        WidgetNode root = customScrollView(properties,
                List.of(sliverToBoxAdapter(text("First item"))));

        String build = generate(root).build().payload();

        assertTrue(build.contains("return LayoutBuilder("), build);
        assertTrue(build.contains("child: CustomScrollView("), build);
        assertTrue(build.contains("scrollDirection: Axis.horizontal"), build);
        assertTrue(build.contains("reverse: true"), build);
        assertTrue(build.contains("shrinkWrap: true"), build);
        assertTrue(build.contains("anchor: 0.25"), build);
        assertTrue(build.contains("slivers: ["), build);
        assertTrue(build.contains("const SliverToBoxAdapter("), build);
        assertTrue(build.contains("child: const Text('First item')"), build);
        assertTrue(build.contains("semanticChildCount: 1"), build);
        assertTrue(build.contains("restorationId: 'feed-scroll'"), build);
        assertFalse(build.contains("children: ["), build);
    }

    @Test
    void omittedPropertiesStillEmitSafeBoundedRootAndEmptySliverList() {
        String build = generate(customScrollView(Map.of(), List.of()))
                .build().payload();

        assertTrue(build.contains("return LayoutBuilder("), build);
        assertTrue(build.contains("constraints.hasBoundedWidth"), build);
        assertTrue(build.contains("constraints.hasBoundedHeight"), build);
        assertTrue(build.contains("child: CustomScrollView("), build);
        assertTrue(build.contains("slivers: []"), build);
        assertFalse(build.contains("SliverToBoxAdapter("), build);
    }

    @Test
    void emitsEveryStaticSliverConstructorPropertyWithoutConstOrBoxWrappers() throws Exception {
        WidgetNode list = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SliverList"),
                Map.of(name("addAutomaticKeepAlives"), new PropertyValue.BooleanValue(false),
                        name("addRepaintBoundaries"), new PropertyValue.BooleanValue(false),
                        name("addSemanticIndexes"), new PropertyValue.BooleanValue(false)),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text("List item")))));
        var slivers = new java.util.ArrayList<WidgetNode>();
        slivers.add(list);
        for (boolean extent : List.of(false, true)) {
            Map<PropertyName, PropertyValue> values = new LinkedHashMap<>();
            values.put(name(extent ? "maxCrossAxisExtent" : "crossAxisCount"),
                    extent ? new PropertyValue.DoubleValue(new BigDecimal("150"))
                            : new PropertyValue.IntegerValue(BigInteger.valueOf(3)));
            values.put(name("mainAxisSpacing"), new PropertyValue.DoubleValue(new BigDecimal("4")));
            values.put(name("crossAxisSpacing"), new PropertyValue.DoubleValue(new BigDecimal("6")));
            values.put(name("childAspectRatio"), new PropertyValue.DoubleValue(new BigDecimal("1.5")));
            slivers.add(new WidgetNode(StableId.random(),
                    new WidgetTypeId(extent ? "flutter.widgets.SliverGrid.extent" : "flutter.widgets.SliverGrid"),
                    values, Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text("Grid item"))))));
        }
        var original = document(customScrollView(Map.of(), slivers));
        var codec = new dev.flutter.netbeans.designer.codec.FdDocumentCodec();
        var decoded = org.junit.jupiter.api.Assertions.assertInstanceOf(
                dev.flutter.netbeans.designer.codec.FdDecodeResult.Current.class, codec.decode(codec.encode(original)));
        org.junit.jupiter.api.Assertions.assertEquals(original, decoded.document());
        String build = generate(decoded.document().root()).build().payload();
        for (String expected : List.of("SliverList.list(", "SliverGrid.count(", "SliverGrid.extent(",
                "addAutomaticKeepAlives: false", "addRepaintBoundaries: false", "addSemanticIndexes: false",
                "crossAxisCount: 3", "maxCrossAxisExtent: 150.0", "mainAxisSpacing: 4.0",
                "crossAxisSpacing: 6.0", "childAspectRatio: 1.5", "children: [")) {
            assertTrue(build.contains(expected), build);
        }
        assertTrue(build.indexOf("addSemanticIndexes: false") < build.indexOf("children: ["),
                "Flutter's named children argument stays last: " + build);
        assertFalse(build.contains("const SliverList"), build);
        assertFalse(build.contains("const SliverGrid"), build);
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
