package io.github.vgrytsenko2022.designer.generation;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverPaddingDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    @Test void exactNamedSliverAndPaddingGeometrySurviveFdRoundTrip() throws Exception {
        for (boolean directional : List.of(false, true)) for (boolean empty : List.of(false, true)) {
            PropertyValue insets = directional
                    ? new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TEN, BigDecimal.ZERO)
                    : new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TEN, BigDecimal.ZERO);
            var padded = new WidgetNode(StableId.random(), SliverPaddingWidgetPropertySchema.TYPE,
                    Map.of(name("padding"), insets), Map.of(new SlotName("sliver"),
                            empty ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(sliverToBoxAdapter(text("Nested")))));
            var doc = document(customScrollView(Map.of(), List.of(padded)));
            var codec = new io.github.vgrytsenko2022.designer.codec.FdDocumentCodec();
            var decoded = assertInstanceOf(io.github.vgrytsenko2022.designer.codec.FdDecodeResult.Current.class, codec.decode(codec.encode(doc)));
            assertEquals(doc, decoded.document());
            var code = generate(decoded.document().root()).build().payload();
            assertTrue(code.contains("const SliverPadding("), code);
            assertTrue(code.contains(directional ? "EdgeInsetsDirectional.fromSTEB(" : "EdgeInsets.fromLTRB("), code);
            assertEquals(!empty, code.contains("sliver: const SliverToBoxAdapter("), code);
            assertFalse(code.contains("child: const SliverToBoxAdapter("), code);
        }
    }
    @Test void typedProjectPaddingIsNotQuotedFlattenedOrReplacedByPreviewGeometry() {
        var value = new PropertyValue.DartObjectReferenceValue(Optional.of("package:sample/insets.dart"),
                "Insets", Optional.of("create"), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
        var padded = new WidgetNode(StableId.random(), SliverPaddingWidgetPropertySchema.TYPE,
                Map.of(name("padding"), value), Map.of(new SlotName("sliver"), WidgetSlot.SingleSlot.empty()));
        var code = generate(customScrollView(Map.of(), List.of(padded))).build().payload();
        assertTrue(code.contains("Insets.create()"), code);
        assertFalse(code.contains("const SliverPadding("), code);
        assertFalse(code.contains("EdgeInsets.zero"), code);
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


