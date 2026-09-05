package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.*;

class PhysicalShapeCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e71");
    private static final StableId CHILD = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e72");

    @Test
    void everyPropertyEditUndoRedoSaveReopenAndFurtherEditPreserveExactPairAndChild() throws Exception {
        var current = openDefault();
        int count = 0;
        for (var entry : fullProperties(PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE,
                "antiAliasWithSaveLayer", true).entrySet()) {
            var before = current;
            current = applied(current, new SetProperty(ROOT, entry.getKey(), entry.getValue()));
            assertEquals(++count, current.cursor());
            assertChildPreserved(current);
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        var saved = current.markSaved();
        var opened = DesignerCommandSession.open(OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults()), saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(opened.ready(), opened.diagnostics().toString());
        current = opened.session().orElseThrow();
        assertFalse(current.dirty());
        assertEquals(0, current.cursor());
        assertExactPair(saved, current);
        current = applied(current, new SetProperty(ROOT, name("elevation"),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("2.5"))));
        assertChildPreserved(current);
        for (String optional : List.of("clipBehavior", "elevation", "shadowColor")) {
            var before = current;
            current = applied(current, new ResetProperty(ROOT, name(optional)));
            assertFalse(current.current().document().root().properties().containsKey(name(optional)));
            assertExactPair(before, current.undo().session());
        }
    }

    @Test
    void roundedCircleStadiumAndBackRetainDirectionalRadiusAndExplicitDirection() throws Exception {
        var current = openDefault();
        var radius = radius(true).geometry();
        for (var shape : List.of(PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE,
                PropertyValue.ShapeBorderClipperValue.Shape.CIRCLE,
                PropertyValue.ShapeBorderClipperValue.Shape.STADIUM,
                PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE)) {
            var value = new PropertyValue.ShapeBorderClipperValue(shape, radius,
                    Optional.of(PropertyValue.ShapeBorderClipperValue.TextDirection.RTL));
            var before = current;
            current = applied(current, new SetProperty(ROOT, name("clipper"), value));
            assertEquals(value, current.current().document().root().properties().get(name("clipper")));
            assertExactPair(before, current.undo().session());
            assertChildPreserved(current);
        }
        assertTrue(source(current).contains("BorderRadiusDirectional.only("));
        assertTrue(source(current).contains("textDirection: TextDirection.rtl"));
    }

    @Test
    void requiredResetAndMalformedEditsRetainHistoryCandidateBytesAndChild() throws Exception {
        var before = openDefault();
        var changed = applied(before, new SetProperty(ROOT, name("elevation"),
                new PropertyValue.IntegerValue(java.math.BigInteger.ONE)));
        var current = changed.undo().session();
        assertTrue(current.canRedo());
        for (String required : List.of("clipper", "color")) {
            assertRejectedUnchanged(current, new ResetProperty(ROOT, name(required)),
                    DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        }
        for (String required : List.of("clipper", "color")) {
            assertRejectedUnchanged(current, new SetProperty(ROOT, name(required), new PropertyValue.NullValue()),
                    DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        }
        assertRejectedUnchanged(current, new SetProperty(ROOT, name("elevation"),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("-0.1"))),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertChildPreserved(current);
    }

    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }

    private static void assertRejectedUnchanged(DesignerCommandSession session, DesignerCommand command,
            DesignerCommandDiagnosticCode expected) {
        var result = session.apply(command);
        assertFalse(result.changed());
        assertSame(session, result.session());
        assertEquals(expected, result.diagnostics().getFirst().code());
        assertEquals(session.cursor(), result.session().cursor());
        assertEquals(session.canUndo(), result.session().canUndo());
        assertEquals(session.canRedo(), result.session().canRedo());
        assertExactPair(session, result.session());
    }

    private static void assertExactPair(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
        assertEquals(expected.current().document(), actual.current().document());
    }

    private static void assertChildPreserved(DesignerCommandSession session) {
        var child = ((WidgetSlot.SingleSlot) session.current().document().root().slots()
                .get(new SlotName("child"))).child().orElseThrow();
        assertEquals(CHILD, child.id());
        assertEquals(new PropertyValue.StringValue("Preserved child"), child.properties().get(new PropertyName("data")));
    }

    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }

    private static DesignerCommandSession openDefault() throws Exception {
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.PhysicalShape"), defaults(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(new WidgetNode(CHILD,
                        new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"),
                                new PropertyValue.StringValue("Preserved child")), Map.of()))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root);
        var generated = new DartRegionGenerator().generate(provisional, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(),
                generated.build().normalizedSha256()), root);
        byte[] dart = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), dart, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
