package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.codec.OriginalFdBytes;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRSuperellipseCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e71");
    private static final StableId CHILD = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e72");
    private static final PropertyName RADIUS = new PropertyName("borderRadius");
    private static final PropertyName CLIPPER = new PropertyName("clipper");
    private static final PropertyName CLIP = new PropertyName("clipBehavior");

    @Test
    void eachPropertyEditIsOneUndoStepWithExactPairRestorationAndPreservedChild() throws Exception {
        var initial = openDefault();
        var rounded = applied(initial, new SetProperty(ROOT, RADIUS, radius(false)));
        var clipped = applied(rounded, new SetProperty(ROOT, CLIPPER, clipper(Optional.of(true))));
        var painted = applied(clipped, new SetProperty(ROOT, CLIP,
                new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer")));
        assertEquals(3, painted.cursor());
        assertEquals(3, painted.current().document().root().properties().size());
        assertTrue(source(painted).contains("return const ClipRSuperellipse("));
        assertTrue(source(painted).contains("borderRadius: const BorderRadius.only("));
        assertTrue(source(painted).contains("clipper: const LocalClipper.create()"));
        assertTrue(source(painted).contains("clipBehavior: Clip.antiAliasWithSaveLayer"));
        assertChildPreserved(painted);
        assertExactPair(clipped, painted.undo().session());
        assertExactPair(painted, painted.undo().session().redo().session());
        assertExactPair(rounded, painted.undo().session().undo().session());
        assertExactPair(initial, painted.undo().session().undo().session().undo().session());
    }

    @Test
    void savedCandidatesReopenEditableAndAllowRadiusReferenceClipEditsAndAllResets() throws Exception {
        for (Optional<Boolean> constant : List.of(Optional.<Boolean>empty(),
                Optional.of(false), Optional.of(true))) {
            var edited = applied(openDefault(), new PatchProperties(ROOT, List.of(
                    new PatchProperties.SetPatch(RADIUS, radius(true)),
                    new PatchProperties.SetPatch(CLIPPER, clipper(constant)),
                    new PatchProperties.SetPatch(CLIP, new PropertyValue.EnumValue("Clip", "hardEdge")))));
            var saved = edited.markSaved();
            assertFalse(saved.dirty());
            var opened = DesignerCommandSession.open(OriginalFdBytes.copyOf(
                    saved.current().fdBytes(), FdCodecLimits.defaults()),
                    saved.current().dartCandidateBytes(), CATALOG);
            assertTrue(opened.ready(), opened.diagnostics().toString());
            var reopened = opened.session().orElseThrow();
            assertEquals(0, reopened.cursor());
            assertEquals(edited.current().document().root().properties(),
                    reopened.current().document().root().properties());
            assertChildPreserved(reopened);
            var rerounded = applied(reopened, new SetProperty(ROOT, RADIUS, radius(false)));
            assertTrue(source(rerounded).contains("borderRadius: const BorderRadius.only("));
            var resetClipper = applied(rerounded, new ResetProperty(ROOT, CLIPPER));
            assertTrue(source(resetClipper).contains("return const ClipRSuperellipse("));
            assertFalse(source(resetClipper).contains("clipper:"));
            assertEquals(radius(false), resetClipper.current().document().root().properties().get(RADIUS));
            assertExactPair(rerounded, resetClipper.undo().session());
            var resetAll = applied(resetClipper, new PatchProperties(ROOT, List.of(
                    new PatchProperties.ResetPatch(RADIUS), new PatchProperties.ResetPatch(CLIP))));
            assertEquals(Map.of(), resetAll.current().document().root().properties());
            assertTrue(source(resetAll).contains("return const ClipRSuperellipse("));
            assertExactPair(resetClipper, resetAll.undo().session());
            assertChildPreserved(resetAll);
        }
    }

    @Test
    void malformedEditsRetainCandidatePairNodeIdentityAndUndoRedoHistory() throws Exception {
        var rounded = applied(openDefault(), new SetProperty(ROOT, RADIUS, radius(true)));
        var painted = applied(rounded, new SetProperty(ROOT, CLIP,
                new PropertyValue.EnumValue("Clip", "none")));
        var current = painted.undo().session();
        assertTrue(current.canRedo());
        assertRejectedUnchanged(current, new SetProperty(ROOT, CLIPPER,
                new PropertyValue.DartExpressionValue("const UnsafeClipper()")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(current, new SetProperty(ROOT, RADIUS,
                new PropertyValue.StringValue("BorderRadius.circular(8)")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejectedUnchanged(current, new SetProperty(ROOT, CLIP,
                new PropertyValue.EnumValue("Clip", "custom")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertEquals(ROOT, current.current().document().root().id());
        assertChildPreserved(current);
    }

    private static PropertyValue.DartObjectReferenceValue clipper(Optional<Boolean> constant) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                constant.isEmpty() ? "_clippers" : "LocalClipper", Optional.of("create"),
                constant.isEmpty() ? PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                        : PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                constant);
    }

    private static PropertyValue.BorderRadiusValue radius(boolean directional) {
        var radius = new PropertyValue.BoxDecorationValue.Radius(
                new java.math.BigDecimal("12"), new java.math.BigDecimal("18"));
        return new PropertyValue.BorderRadiusValue(directional
                ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(radius, radius, radius, radius)
                : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(radius, radius, radius, radius));
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
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.ClipRSuperellipse"), Map.of(),
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
