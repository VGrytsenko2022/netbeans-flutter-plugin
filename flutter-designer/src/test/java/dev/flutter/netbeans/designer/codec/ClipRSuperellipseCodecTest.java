package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
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
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRSuperellipseCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsBothRadiusGeometriesEveryReferenceFormAndAllClipModesWithoutLosingChildOrConstness()
            throws Exception {
        for (boolean directional : List.of(false, true)) {
            for (Optional<String> uri : List.of(Optional.<String>empty(),
                    Optional.of("package:sample/objects.dart"))) {
                for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("create"))) {
                    for (Optional<Boolean> constant : List.of(Optional.<Boolean>empty(),
                            Optional.of(false), Optional.of(true))) {
                        var reference = new PropertyValue.DartObjectReferenceValue(uri, "Objects", member,
                                constant.isEmpty() ? PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                                        : PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                                constant);
                        for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                            for (boolean hasChild : List.of(false, true)) {
                                var original = document(Map.of(new PropertyName("clipper"), reference,
                                        new PropertyName("borderRadius"), radius(directional),
                                        new PropertyName("clipBehavior"), new PropertyValue.EnumValue("Clip", clip)),
                                        hasChild);
                                var encoded = codec.encode(original);
                                var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
                                assertEquals(12, decoded.sourceSchemaVersion());
                                assertFalse(decoded.migrated());
                                assertEquals(original, decoded.document());
                                assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
                                var validation = new WidgetTreeValidator().validate(decoded.document(),
                                        BuiltInWidgetCatalog.getDefault());
                                assertTrue(validation.valid(), validation.issues().toString());
                                var before = new DartRegionGenerator().generate(original, BuiltInWidgetCatalog.getDefault());
                                var after = new DartRegionGenerator().generate(decoded.document(), BuiltInWidgetCatalog.getDefault());
                                assertTrue(after.successful(), after.diagnostics().toString());
                                assertEquals(before.generated().orElseThrow().build(), after.generated().orElseThrow().build());
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void defaultOmissionRemainsEditableAfterReopenWithoutInventingADelegate() throws Exception {
        DesignerDocument original = document(Map.of(), false);
        OriginalFdBytes encoded = codec.encode(original);
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        assertFalse(json.contains("\"borderRadius\""), json);
        assertFalse(json.contains("\"clipper\""), json);
        DesignerDocument reopened = assertInstanceOf(FdDecodeResult.Current.class,
                codec.decode(encoded)).document();
        assertEquals(original, reopened);
        assertTrue(new WidgetTreeValidator().validate(reopened, BuiltInWidgetCatalog.getDefault()).valid());
        String build = new DartRegionGenerator().generate(reopened, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow().build().payload();
        assertTrue(build.contains("const ClipRSuperellipse("), build);
    }

    private static PropertyValue.BorderRadiusValue radius(boolean directional) {
        var first = new PropertyValue.BoxDecorationValue.Radius(
                new java.math.BigDecimal("1.5"), new java.math.BigDecimal("2.5"));
        var second = new PropertyValue.BoxDecorationValue.Radius(
                new java.math.BigDecimal("3.5"), new java.math.BigDecimal("4.5"));
        return new PropertyValue.BorderRadiusValue(directional
                ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(first, second, first, second)
                : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(first, second, first, second));
    }

    private static DesignerDocument document(Map<PropertyName, PropertyValue> properties, boolean hasChild) {
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Persisted child")), Map.of());
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.ClipRSuperellipse"), properties,
                Map.of(new SlotName("child"), hasChild ? WidgetSlot.SingleSlot.of(child) : WidgetSlot.SingleSlot.empty()));
        ManagedRegion region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(Optional.empty(), StableId.random(), new DartSourceDescriptor(
                "sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(),
                new ManagedRegions(region, region)), Optional.empty(), root, Extensions.empty());
    }
}
