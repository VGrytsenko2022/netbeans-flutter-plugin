package dev.flutter.netbeans.designer.generation;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverPrototypeExtentListDartGenerationTest {
    private static final String ZERO_HASH = "0".repeat(64);
    @Test void allPresetsGenerateExactConstructorsWithSymbolEvidenceAndRoundTrip() throws Exception {
        var slivers = new ArrayList<WidgetNode>();
        for (var kind : SliverPrototypeExtentListWidgetPropertySchema.Kind.values()) {
            var d = BuiltInWidgetCatalog.getDefault().find(kind.type()).orElseThrow();
            var n = WidgetNodePrototypeFactory.create(d, StableId.random());
            var values = new LinkedHashMap<>(n.properties());
            for (var f : SliverPrototypeExtentListWidgetPropertySchema.fields(kind)) {
                if (f.type().equals("bool")) values.put(name(f.name()), new PropertyValue.BooleanValue(false));
                if (f.type().startsWith("int")) values.put(name(f.name()), new PropertyValue.IntegerValue(BigInteger.valueOf(3)));
            }
            slivers.add(new WidgetNode(n.id(), n.type(), values, n.slots()));
        }
        var doc = document(customScrollView(Map.of(), slivers));
        var codec = new dev.flutter.netbeans.designer.codec.FdDocumentCodec();
        var decoded = assertInstanceOf(dev.flutter.netbeans.designer.codec.FdDecodeResult.Current.class, codec.decode(codec.encode(doc)));
        assertEquals(doc, decoded.document());
        var generated = generate(decoded.document().root());
        var code = generated.build().payload();
        for(var expected : List.of("SliverPrototypeExtentList.list(", "SliverPrototypeExtentList.builder(", "SliverPrototypeExtentList(",
                "prototypeItem: const SizedBox(width: 48.0, height: 48.0)", "itemBuilder: (_, index) => null", "delegate: SliverChildListDelegate(const [])",
                "itemCount: 3", "addAutomaticKeepAlives: false", "children: []"))
            assertTrue(code.contains(expected),code);
        assertFalse(code.contains("'empty'"), code);
        assertFalse(code.contains("const SliverPrototypeExtentList("), code);
    }
    @Test void projectReferencesFactoriesAndExplicitNullStayTypedAndUnquoted() {
        for (var kind : SliverPrototypeExtentListWidgetPropertySchema.Kind.values()) {
            var d = BuiltInWidgetCatalog.getDefault().find(kind.type()).orElseThrow();
            var n = WidgetNodePrototypeFactory.create(d, StableId.random());
            var values = new LinkedHashMap<>(n.properties());
            for (var f : SliverPrototypeExtentListWidgetPropertySchema.fields(kind)) {
                if (f.required() && !f.type().equals("double")) values.put(name(f.name()), new PropertyValue.DartObjectReferenceValue(
                        Optional.of("package:sample/builders.dart"), "Builders", Optional.of(f.name()),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)));
                if (f.type().endsWith("?")) values.put(name(f.name()), new PropertyValue.NullValue());
            }
            var generated = generate(customScrollView(Map.of(), List.of(new WidgetNode(n.id(), n.type(), values, n.slots()))));
            var code = generated.build().payload();
            for (var f : SliverPrototypeExtentListWidgetPropertySchema.fields(kind)) {
                if (f.required() && !f.type().equals("double")) assertTrue(code.contains("Builders." + f.name() + "()"), code);
                if (f.type().endsWith("?")) assertTrue(code.contains(f.name() + ": null"), code);
            }
        }
    }
    @Test void prototypeIsSeparateFromPaintedChildrenAndSurvivesRoundTrip() throws Exception {
        var codec = new dev.flutter.netbeans.designer.codec.FdDocumentCodec();
        for(var kind : SliverPrototypeExtentListWidgetPropertySchema.Kind.values()) {
            var definition = BuiltInWidgetCatalog.getDefault().find(kind.type()).orElseThrow();
            var prototype = WidgetNodePrototypeFactory.create(definition,StableId.random());
            var slots = new LinkedHashMap<>(prototype.slots());
            var measurement = text("Measure only");
            slots.put(new SlotName("prototypeItem"),WidgetSlot.SingleSlot.of(measurement));
            if(kind.hasChildren()) slots.put(new SlotName("children"),new WidgetSlot.ListSlot(List.of(text("Visible item"))));
            var node = new WidgetNode(prototype.id(),prototype.type(),prototype.properties(),slots);
            var document = document(customScrollView(Map.of(),List.of(node)));
            var decoded = assertInstanceOf(dev.flutter.netbeans.designer.codec.FdDecodeResult.Current.class,codec.decode(codec.encode(document)));
            assertEquals(document,decoded.document());
            var code = generate(decoded.document().root()).build().payload();
            assertTrue(code.contains("prototypeItem:"),code);
            assertTrue(code.contains("Measure only"),code);
            assertEquals(kind.hasChildren(),code.contains("Visible item"),code);
            assertFalse(code.contains("prototypeItem: null"),code);
            assertFalse(code.contains("semanticIndexOffset:"),code);
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

