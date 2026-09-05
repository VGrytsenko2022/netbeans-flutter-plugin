package dev.flutter.netbeans.designer.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FdV7BoxConstraintsValueCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsEveryFiniteUnboundedTightAndExpandingStateOnBothAxes()
            throws Exception {
        List<Axis> states = List.of(
                axis(0, null),
                axis(0, 100),
                axis(10, null),
                axis(10, 100),
                axis(48, 48),
                Axis.expanding());

        for (Axis width : states) {
            assertStable(value(width, axis(0, null)), "width " + width);
        }
        for (Axis height : states) {
            assertStable(value(axis(0, null), height), "height " + height);
        }
    }

    @Test
    void canonicalCurrentSchemaUsesNullForPositiveInfinityOnAllFourBounds()
            throws Exception {
        PropertyValue.BoxConstraintsValue expanding = value(
                Axis.expanding(), Axis.expanding());
        OriginalFdBytes encoded = codec.encode(document(expanding));
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 13"), json);
        assertTrue(json.contains("\"$schema\": \"../fd-v13.schema.json\""), json);
        assertTrue(json.contains("\"minWidth\": null"), json);
        assertTrue(json.contains("\"maxWidth\": null"), json);
        assertTrue(json.contains("\"minHeight\": null"), json);
        assertTrue(json.contains("\"maxHeight\": null"), json);
        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        PropertyValue.BoxConstraintsValue actual = constraints(decoded.document());
        assertTrue(actual.expandingWidth());
        assertTrue(actual.expandingHeight());
    }

    @Test
    void migratesFiniteV6BoundsLosslesslyAndReencodesCanonicalCurrentSchema()
            throws Exception {
        PropertyValue.BoxConstraintsValue original = value(
                axis(10, null), axis(20, 200));
        String current = new String(
                codec.encode(document(original)).copyBytes(), StandardCharsets.UTF_8);
        String v6 = current
                .replace("\"schemaVersion\": 13", "\"schemaVersion\": 6")
                .replace("../fd-v13.schema.json", "../fd-v6.schema.json");

        FdDecodeResult.Current migrated = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(v6.getBytes(StandardCharsets.UTF_8)));
        assertEquals(6, migrated.sourceSchemaVersion());
        assertTrue(migrated.migrated());
        assertEquals(original, constraints(migrated.document()));
        assertEquals("../fd-v13.schema.json",
                migrated.document().schemaReference().orElseThrow());

        OriginalFdBytes canonical = codec.encode(migrated.document());
        String canonicalJson = new String(
                canonical.copyBytes(), StandardCharsets.UTF_8);
        assertTrue(canonicalJson.contains("\"schemaVersion\": 13"), canonicalJson);
        assertTrue(canonicalJson.contains("\"minWidth\": 10"), canonicalJson);
        assertTrue(canonicalJson.contains("\"maxWidth\": null"), canonicalJson);
        assertArrayEquals(canonical.copyBytes(), codec.encode(assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(canonical)).document()).copyBytes());
    }

    @Test
    void rejectsExpandingMinimumWithFiniteMaximumAndLegacyNullMinimum()
            throws Exception {
        String current = new String(codec.encode(document(value(
                axis(10, 100), axis(0, null)))).copyBytes(),
                StandardCharsets.UTF_8);
        String incoherentV7 = current.replace(
                "\"minWidth\": 10", "\"minWidth\": null");
        assertInvalid(incoherentV7, "infinite minimum with finite maximum");

        String mislabeledV6 = current
                .replace("\"schemaVersion\": 13", "\"schemaVersion\": 6")
                .replace("../fd-v13.schema.json", "../fd-v6.schema.json")
                .replace("\"minWidth\": 10", "\"minWidth\": null");
        assertInvalid(mislabeledV6, "schema v6 finite minimum gate");

        String inverted = current.replace(
                "\"maxWidth\": 100", "\"maxWidth\": 5");
        assertInvalid(inverted, "finite maximum below minimum");
    }

    private void assertStable(
            PropertyValue.BoxConstraintsValue value,
            String description) throws Exception {
        OriginalFdBytes first = codec.encode(document(value));
        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(first), description);
        assertEquals(13, decoded.sourceSchemaVersion(), description);
        assertEquals(value, constraints(decoded.document()), description);
        assertArrayEquals(first.copyBytes(),
                codec.encode(decoded.document()).copyBytes(), description);
    }

    private void assertInvalid(String json, String description) throws Exception {
        FdDecodeResult.Invalid invalid = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(json.getBytes(StandardCharsets.UTF_8)), description);
        assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.pointer().contains("constraints")),
                () -> description + ": " + invalid.diagnostics());
    }

    private static PropertyValue.BoxConstraintsValue constraints(
            DesignerDocument document) {
        return assertInstanceOf(PropertyValue.BoxConstraintsValue.class,
                document.root().properties().get(new PropertyName("constraints")));
    }

    private static PropertyValue.BoxConstraintsValue value(
            Axis width,
            Axis height) {
        return new PropertyValue.BoxConstraintsValue(
                width.minimum(), width.maximum(),
                height.minimum(), height.maximum());
    }

    private static Axis axis(int minimum, Integer maximum) {
        return new Axis(
                PropertyValue.BoxConstraintBound.finite(
                        BigDecimal.valueOf(minimum)),
                maximum == null
                        ? PropertyValue.BoxConstraintBound.Infinity.INSTANCE
                        : PropertyValue.BoxConstraintBound.finite(
                                BigDecimal.valueOf(maximum)));
    }

    private static DesignerDocument document(
            PropertyValue.BoxConstraintsValue constraints) {
        WidgetNode root = new WidgetNode(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new WidgetTypeId("flutter.widgets.ConstrainedBox"),
                Map.of(new PropertyName("constraints"), constraints),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(
                Optional.of("../fd-v13.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor(
                        "constraints_page.dart", "ConstraintsPage",
                        WidgetClassKind.STATELESS, Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(), root, Extensions.empty());
    }

    private record Axis(
            PropertyValue.BoxConstraintBound minimum,
            PropertyValue.BoxConstraintBound maximum) {
        static Axis expanding() {
            return new Axis(
                    PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                    PropertyValue.BoxConstraintBound.Infinity.INSTANCE);
        }
    }
}
