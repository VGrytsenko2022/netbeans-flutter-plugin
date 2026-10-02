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

class MouseRegionCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE;
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
            assertTrue(source(wrapped).contains("MouseRegion("));
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
    void allThreeCreateTypedUserHandlersInStatelessOrStatefulOwnersAsOneUndoableCommand() throws Exception {
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
    void nullOpaqueIsRejectedWithoutLosingRedoWhileNullCallbackRemainsDistinctFromOmission() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATELESS);
        var behavior = new PropertyName("opaque");
        var opaque = apply(initial, new SetProperty(widget.id(), behavior, new PropertyValue.BooleanValue(true)));
        var withRedo = opaque.undo().session();
        var rejected = withRedo.apply(new SetProperty(widget.id(), behavior, new PropertyValue.NullValue()));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
        assertSame(withRedo, rejected.session());
        assertTrue(rejected.session().canRedo());
        assertExact(opaque, rejected.session().redo().session());
        var event = new PropertyName("onHover");
        var nil = apply(opaque, new SetProperty(widget.id(), event, new PropertyValue.NullValue()));
        assertTrue(source(nil).contains("onHover: null"));
        var omitted = apply(nil, new ResetProperty(widget.id(), event));
        assertFalse(source(omitted).contains("onHover:"));
        assertExact(nil, omitted.undo().session());
        assertExact(opaque, omitted.undo().session().undo().session());
        assertExact(omitted, reopen(omitted.markSaved()));
    }

    @Test
    void opaqueStateConsumersPreserveLiteralPreviewAndExactPersistenceForAllThreeTransforms() throws Exception {
        var base = prototype();
        var widget = new WidgetNode(base.id(), TYPE, Map.of(new PropertyName("opaque"), new PropertyValue.BooleanValue(false)), base.slots());
        var initial = open(widget, WidgetClassKind.STATEFUL);
        for (var binding : List.of(
                new StatePropertyBinding("_opaque", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_opaque", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS,
                        Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE))))) {
            var bound = apply(initial, new BindPropertyToState(widget.id(), new PropertyName("opaque"), binding));
            assertEquals(initial.cursor() + 1, bound.cursor());
            var node = bound.current().document().root();
            assertEquals(new PropertyValue.BooleanValue(false), node.properties().get(new PropertyName("opaque")));
            assertEquals(binding, node.propertyBindings().get(new PropertyName("opaque")));
            assertTrue(node.stateBinding().isEmpty(), "MouseRegion has no automatic State producer");
            assertExact(initial, bound.undo().session());
            assertExact(bound, bound.undo().session().redo().session());
            assertExact(bound, reopen(bound.markSaved()));
            var removed = apply(bound, new RemovePropertyStateBinding(widget.id(), new PropertyName("opaque")));
            assertTrue(source(removed).contains("opaque: false"));
            assertTrue(source(removed).contains("bool _opaque = true"));
            assertExact(initial, removed);
        }
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
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _opaque = true;\n  int? _choice = null;\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
