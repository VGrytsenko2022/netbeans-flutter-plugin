package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.*;

class PhysicalModelCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsEveryShapeClipColorBranchAndOptionalChildWithExactGeometryAndGeneratedSource() throws Exception {
        for (String shape : List.of("rectangle", "circle")) {
            for (String clip : List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                for (boolean theme : List.of(false, true)) {
                    for (boolean child : List.of(false, true)) {
                        var original = document(physicalModel(fullProperties(shape, clip, theme), child));
                        var encoded = codec.encode(original);
                        var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
                        assertEquals(17, decoded.sourceSchemaVersion());
                        assertFalse(decoded.migrated());
                        assertEquals(original, decoded.document());
                        assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
                        assertTrue(new WidgetTreeValidator().validate(decoded.document(), BuiltInWidgetCatalog.getDefault()).valid());
                        var before = new DartRegionGenerator().generate(original, BuiltInWidgetCatalog.getDefault());
                        var after = new DartRegionGenerator().generate(decoded.document(), BuiltInWidgetCatalog.getDefault());
                        assertTrue(after.successful(), after.diagnostics().toString());
                        assertEquals(before.generated().orElseThrow().build(), after.generated().orElseThrow().build());
                        assertEquals(radius(false), decoded.document().root().properties().get(name("borderRadius")));
                    }
                }
            }
        }
    }

    @Test
    void requiredColorOnlyReopensWithoutInventingOptionalFrameworkDefaults() throws Exception {
        var original = document(physicalModel(defaults(), false));
        var encoded = codec.encode(original);
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        for (String omitted : List.of("shape", "clipBehavior", "borderRadius", "elevation", "shadowColor")) {
            assertFalse(json.contains("\"" + omitted + "\""), json);
        }
        var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
        assertEquals(original, decoded.document());
        assertTrue(new WidgetTreeValidator().validate(decoded.document(), BuiltInWidgetCatalog.getDefault()).valid());
    }
}
