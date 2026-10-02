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

class ClipPathCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e61");
    private static final StableId CHILD = StableId.parse("cae4c05b-1c9d-4b6f-a6a3-3cae02bd7e62");
    private static final PropertyName SHAPE = new PropertyName("shape");
    private static final PropertyName CLIPPER = new PropertyName("clipper");
    private static final PropertyValue.DartObjectReferenceValue SHAPE_VALUE =
            new PropertyValue.DartObjectReferenceValue(Optional.of("package:flutter/painting.dart"),
                    "CircleBorder", Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
    private static final PropertyValue.DartObjectReferenceValue CLIPPER_VALUE =
            new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_clipper", Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());

    @Test
    void bothAtomicBranchSwitchesAreOneUndoStepAndRestoreExactPairBytes() throws Exception {
        var initial = openDefault();
        var shaped = applied(initial, new SetProperty(ROOT, SHAPE, SHAPE_VALUE));
        assertEquals(1, shaped.cursor());
        assertTrue(source(shaped).contains("ClipPath.shape("));
        var clipped = applied(shaped, switchTo(CLIPPER, CLIPPER_VALUE, SHAPE));
        assertEquals(2, clipped.cursor());
        assertEquals(Map.of(CLIPPER, CLIPPER_VALUE), clipped.current().document().root().properties());
        assertTrue(source(clipped).contains("ClipPath("));
        assertFalse(source(clipped).contains("ClipPath.shape("));
        assertChildPreserved(clipped);
        assertExactPair(shaped, clipped.undo().session());
        assertExactPair(clipped, clipped.undo().session().redo().session());

        var reshaped = applied(clipped, switchTo(SHAPE, SHAPE_VALUE, CLIPPER));
        assertEquals(3, reshaped.cursor());
        assertEquals(Map.of(SHAPE, SHAPE_VALUE), reshaped.current().document().root().properties());
        assertTrue(source(reshaped).contains("ClipPath.shape("));
        assertChildPreserved(reshaped);
        assertExactPair(clipped, reshaped.undo().session());
        assertExactPair(reshaped, reshaped.undo().session().redo().session());
    }

    @Test
    void savedCandidateReopensEditableAndStillAllowsBranchSwitchAndReset() throws Exception {
        for (PropertyName branch : List.of(CLIPPER, SHAPE)) {
            var value = branch.equals(CLIPPER) ? CLIPPER_VALUE : SHAPE_VALUE;
            var opposite = branch.equals(CLIPPER) ? SHAPE : CLIPPER;
            var oppositeValue = branch.equals(CLIPPER) ? SHAPE_VALUE : CLIPPER_VALUE;
            var edited = applied(openDefault(), new SetProperty(ROOT, branch, value));
            var saved = edited.markSaved();
            assertFalse(saved.dirty());
            var open = DesignerCommandSession.open(OriginalFdBytes.copyOf(
                    saved.current().fdBytes(), FdCodecLimits.defaults()),
                    saved.current().dartCandidateBytes(), CATALOG);
            assertTrue(open.ready(), open.diagnostics().toString());
            var reopened = open.session().orElseThrow();
            assertEquals(0, reopened.cursor());
            assertEquals(value, reopened.current().document().root().properties().get(branch));
            assertChildPreserved(reopened);
            var switched = applied(reopened, switchTo(opposite, oppositeValue, branch));
            assertEquals(Map.of(opposite, oppositeValue), switched.current().document().root().properties());
            var reset = applied(switched, new ResetProperty(ROOT, opposite));
            assertEquals(Map.of(), reset.current().document().root().properties());
            assertTrue(source(reset).contains("const ClipPath("));
            assertFalse(source(reset).contains("ClipPath.shape("));
            assertExactPair(switched, reset.undo().session());
            assertChildPreserved(reset);
        }
    }

    @Test
    void conflictingOrMalformedPatchRetainsPriorBranchPairAndUndoRedoHistory() throws Exception {
        var shaped = applied(openDefault(), new SetProperty(ROOT, SHAPE, SHAPE_VALUE));
        var clipped = applied(shaped, switchTo(CLIPPER, CLIPPER_VALUE, SHAPE));
        var current = clipped.undo().session();
        assertTrue(current.canRedo());
        assertRejectedUnchanged(current, new SetProperty(ROOT, CLIPPER, CLIPPER_VALUE),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(current, new PatchProperties(ROOT, List.of(
                new PatchProperties.SetPatch(CLIPPER, CLIPPER_VALUE),
                new PatchProperties.SetPatch(SHAPE, SHAPE_VALUE))),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        assertRejectedUnchanged(current, switchTo(CLIPPER,
                new PropertyValue.StringValue("const CustomClipper<Path>()"), SHAPE),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
    }

    private static PatchProperties switchTo(PropertyName target, PropertyValue value, PropertyName previous) {
        return new PatchProperties(ROOT, List.of(new PatchProperties.ResetPatch(previous),
                new PatchProperties.SetPatch(target, value)));
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
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.ClipPath"), Map.of(),
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
