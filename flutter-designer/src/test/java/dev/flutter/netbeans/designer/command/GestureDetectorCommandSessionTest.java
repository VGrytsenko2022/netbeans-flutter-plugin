package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GestureDetectorCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void wrapRootAndNestedTextRetainsStableIdsChildrenAndExactUndoRedoReopen() throws Exception {
        for (boolean nested : List.of(false, true)) {
            var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                    Map.of(new PropertyName("data"), new PropertyValue.StringValue("User child")), Map.of());
            var root = nested ? new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text)))) : text;
            var initial = open(root, WidgetClassKind.STATELESS);
            var wrapper = prototype();
            var wrapped = apply(initial, new WrapWidget(text.id(), wrapper, CHILD, 0));
            assertEquals(initial.cursor() + 1, wrapped.cursor());
            var result = EventCommandSupport.widget(wrapped.current().document().root(), wrapper.id());
            assertEquals(text, ((WidgetSlot.SingleSlot) result.slots().get(CHILD)).child().orElseThrow());
            assertTrue(source(wrapped).contains("GestureDetector("));
            assertFalse(source(wrapped).contains("const GestureDetector("));
            assertTrue(source(wrapped).contains("User child"));
            assertExact(initial, wrapped.undo().session());
            assertExact(wrapped, wrapped.undo().session().redo().session());
            assertExact(wrapped, reopen(wrapped.markSaved()));
            var edited = apply(reopen(wrapped.markSaved()), new SetProperty(text.id(),
                    new PropertyName("data"), new PropertyValue.StringValue("Editable after reopen")));
            assertTrue(source(edited).contains("Editable after reopen"));
            assertExact(wrapped, edited.undo().session());
        }
    }

    @Test
    void everyGestureEventCreatesTypedUserHandlerAndBindingAsOneUndoableCommand() throws Exception {
        for (var event : WidgetEventCatalog.eventsFor(CATALOG.find(TYPE).orElseThrow())) {
            var widget = prototype();
            var initial = open(widget, WidgetClassKind.STATEFUL);
            var created = apply(initial, new CreateEventHandler(widget.id(), event.propertyName(), "_gesture"));
            assertEquals(initial.cursor() + 1, created.cursor());
            assertTrue(source(created).contains(event.signature().declaration("_gesture")), event.toString());
            assertTrue(source(created).contains(event.propertyName().value() + ": _gesture"), event.toString());
            assertTrue(source(created).indexOf("void _gesture") > source(created).indexOf("class FormLogic"));
            assertTrue(source(created).contains("// Preserved user source"));
            assertExact(initial, created.undo().session());
            assertExact(created, created.undo().session().redo().session());
            assertExact(created, reopen(created.markSaved()));
        }
    }

    @Test
    void incompatibleRecognizerCommandDoesNotInsertAHandlerOrDiscardRedoAndAtomicPatchCanSwitchFamilies() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATEFUL);
        var pan = apply(initial, new CreateEventHandler(widget.id(), new PropertyName("onPanStart"), "_pan"));
        var changed = apply(pan, new SetProperty(widget.id(), new PropertyName("excludeFromSemantics"), new PropertyValue.BooleanValue(true)));
        var withRedo = changed.undo().session();
        var rejected = withRedo.apply(new CreateEventHandler(widget.id(), new PropertyName("onScaleUpdate"), "_scale"));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
        assertSame(withRedo, rejected.session());
        assertTrue(rejected.session().canRedo());
        assertFalse(source(rejected.session()).contains("void _scale"));
        assertExact(changed, rejected.session().redo().session());
        var transition = apply(pan, new PatchProperties(widget.id(), List.of(
                new PatchProperties.ResetPatch(new PropertyName("onPanStart")),
                new PatchProperties.SetPatch(new PropertyName("onScaleUpdate"), new PropertyValue.CallbackValue("_existingScale")))));
        assertFalse(source(transition).contains("onPanStart:"));
        assertTrue(source(transition).contains("onScaleUpdate: _existingScale"));
        assertTrue(source(transition).contains("void _pan(DragStartDetails details)"));
        assertExact(pan, transition.undo().session());
        assertExact(transition, reopen(transition.markSaved()));
    }

    @Test
    void supportedDevicesCanSwitchBetweenExplicitEmptyNullAndOmittedWithoutLosingHistory() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATELESS);
        var empty = apply(initial, new SetProperty(widget.id(), new PropertyName("supportedDevices"),
                new PropertyValue.PointerDeviceKindSetValue(List.of())));
        assertTrue(source(empty).contains("supportedDevices:"));
        assertTrue(java.util.regex.Pattern.compile("const <(?:[A-Za-z_][A-Za-z0-9_]*\\.)?PointerDeviceKind>\\{\\}")
                .matcher(source(empty)).find(), source(empty));
        var nullValue = apply(empty, new SetProperty(widget.id(), new PropertyName("supportedDevices"), new PropertyValue.NullValue()));
        assertTrue(source(nullValue).contains("supportedDevices: null"));
        var omitted = apply(nullValue, new ResetProperty(widget.id(), new PropertyName("supportedDevices")));
        assertFalse(source(omitted).contains("supportedDevices:"));
        assertExact(nullValue, omitted.undo().session());
        assertExact(empty, omitted.undo().session().undo().session());
        assertExact(initial, omitted.undo().session().undo().session().undo().session());
        assertExact(omitted, reopen(omitted.markSaved()));
    }

    private static WidgetNode prototype() {
        return WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind) throws Exception {
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var generation = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(generation.successful(), generation.diagnostics().toString());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(id,
                descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n"
                + "  @override\n  State<Sample> createState() => FormLogic();\n}\n"
                + "class FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n"
                + "  void _existingScale(dynamic details) {}\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
