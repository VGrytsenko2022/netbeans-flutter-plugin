package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListenerCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = ListenerWidgetPropertySchema.LISTENER_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void rootAndNestedWrapPreserveSubtreeStableIdsAndExactHistoryAcrossReopen() throws Exception {
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
            assertTrue(source(wrapped).contains("Listener("));
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
    void allNineCreateTypedUserHandlersInStatelessOrStatefulOwnersAsOneUndoableCommand() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            for (var event : WidgetEventCatalog.eventsFor(CATALOG.find(TYPE).orElseThrow())) {
                var widget = prototype();
                var initial = open(widget, kind);
                var created = apply(initial, new CreateEventHandler(widget.id(), event.propertyName(), "_pointer"));
                assertEquals(initial.cursor() + 1, created.cursor());
                assertTrue(source(created).contains(event.signature().declaration("_pointer")), event.toString());
                assertTrue(source(created).contains(event.propertyName().value() + ": _pointer"), event.toString());
                assertTrue(source(created).contains("package:flutter/gestures.dart"), event.toString());
                assertTrue(source(created).contains("// Preserved user source"));
                if (kind == WidgetClassKind.STATEFUL) {
                    assertTrue(source(created).indexOf("void _pointer") > source(created).indexOf("class FormLogic"));
                }
                assertExact(initial, created.undo().session());
                assertExact(created, created.undo().session().redo().session());
                assertExact(created, reopen(created.markSaved()));
                var disconnected = apply(created, new ResetProperty(widget.id(), event.propertyName()));
                assertFalse(source(disconnected).contains(event.propertyName().value() + ": _pointer"));
                assertTrue(source(disconnected).contains(event.signature().declaration("_pointer")), "Disconnect must preserve user handler");
                assertExact(created, disconnected.undo().session());
            }
        }
    }

    @Test
    void nullBehaviorIsRejectedWithoutLosingRedoWhileNullCallbackRemainsDistinctFromOmission() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATELESS);
        var behavior = new PropertyName("behavior");
        var opaque = apply(initial, new SetProperty(widget.id(), behavior, new PropertyValue.EnumValue("HitTestBehavior", "opaque")));
        var withRedo = opaque.undo().session();
        var rejected = withRedo.apply(new SetProperty(widget.id(), behavior, new PropertyValue.NullValue()));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
        assertSame(withRedo, rejected.session());
        assertTrue(rejected.session().canRedo());
        assertExact(opaque, rejected.session().redo().session());
        var event = new PropertyName("onPointerSignal");
        var nil = apply(opaque, new SetProperty(widget.id(), event, new PropertyValue.NullValue()));
        assertTrue(source(nil).contains("onPointerSignal: null"));
        var omitted = apply(nil, new ResetProperty(widget.id(), event));
        assertFalse(source(omitted).contains("onPointerSignal:"));
        assertExact(nil, omitted.undo().session());
        assertExact(opaque, omitted.undo().session().undo().session());
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
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
