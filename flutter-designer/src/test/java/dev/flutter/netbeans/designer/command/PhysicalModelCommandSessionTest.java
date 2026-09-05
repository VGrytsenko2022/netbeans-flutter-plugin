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
import static dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.*;

class PhysicalModelCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e71");
    private static final StableId CHILD = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e72");

    @Test
    void eachPropertyEditIsOneUndoStepWithExactPairRestorationAndPreservedChild() throws Exception {
        var current = openDefault();
        int count = 0;
        for (var entry : fullProperties("circle", "antiAliasWithSaveLayer", true).entrySet()) {
            var before = current;
            current = applied(current, new SetProperty(ROOT, entry.getKey(), entry.getValue()));
            assertEquals(++count, current.cursor());
            assertChildPreserved(current);
            assertExactPair(before, current.undo().session());
            assertExactPair(current, current.undo().session().redo().session());
        }
        assertEquals(6, current.current().document().root().properties().size());
        assertTrue(source(current).contains("return PhysicalModel("));
        assertTrue(source(current).contains("shape: BoxShape.circle"));
        assertTrue(source(current).contains("borderRadius: const BorderRadius.only("));
        assertTrue(source(current).contains("shadowColor: Theme.of(context).colorScheme.shadow"));
    }

    @Test
    void savedPairReopensEditableShapeSwitchRetainsRadiusAndOptionalResetsAreUndoable() throws Exception {
        for (boolean themed : List.of(false, true)) {
            var edited = openDefault();
            for (var entry : fullProperties("circle", "hardEdge", themed).entrySet()) {
                edited = applied(edited, new SetProperty(ROOT, entry.getKey(), entry.getValue()));
            }
            var saved = edited.markSaved();
            assertFalse(saved.dirty());
            var opened = DesignerCommandSession.open(OriginalFdBytes.copyOf(
                    saved.current().fdBytes(), FdCodecLimits.defaults()),
                    saved.current().dartCandidateBytes(), CATALOG);
            assertTrue(opened.ready(), opened.diagnostics().toString());
            var current = opened.session().orElseThrow();
            assertEquals(0, current.cursor());
            assertEquals(edited.current().document().root().properties(),
                    current.current().document().root().properties());
            assertChildPreserved(current);
            var circle = current;
            current = applied(current, new SetProperty(ROOT, name("shape"),
                    new PropertyValue.EnumValue("BoxShape", "rectangle")));
            assertEquals(radius(false), current.current().document().root().properties().get(name("borderRadius")));
            assertExactPair(circle, current.undo().session());
            current = applied(current, new SetProperty(ROOT, name("color"), new PropertyValue.ColorValue(0xFF2196F3L)));
            for (String property : List.of("shape", "clipBehavior", "borderRadius", "elevation", "shadowColor")) {
                var before = current;
                current = applied(current, new ResetProperty(ROOT, name(property)));
                assertFalse(current.current().document().root().properties().containsKey(name(property)));
                assertExactPair(before, current.undo().session());
                assertExactPair(current, current.undo().session().redo().session());
            }
            assertEquals(defaults(), current.current().document().root().properties());
            assertTrue(source(current).contains("return const PhysicalModel("));
            assertChildPreserved(current);
        }
    }

    @Test
    void malformedEditsAndRequiredColorResetRetainCandidateIdentityAndHistory() throws Exception {
        var before = applied(openDefault(), new SetProperty(ROOT, name("borderRadius"), radius(false)));
        var after = applied(before, new SetProperty(ROOT, name("shape"), new PropertyValue.EnumValue("BoxShape", "circle")));
        var current = after.undo().session();
        assertTrue(current.canRedo());
        assertRejectedUnchanged(current, new SetProperty(ROOT, name("borderRadius"), radius(true)),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(current, new SetProperty(ROOT, name("elevation"),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("-0.1"))),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(current, new SetProperty(ROOT, name("shape"),
                new PropertyValue.EnumValue("BoxShape", "custom")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(current, new SetProperty(ROOT, name("shadowColor"),
                new PropertyValue.DartExpressionValue("Colors.black")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(current, new ResetProperty(ROOT, name("color")),
                DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        assertEquals(ROOT, current.current().document().root().id());
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
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.PhysicalModel"), defaults(),
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
