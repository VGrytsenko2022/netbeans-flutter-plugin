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

class FocusCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = FocusWidgetPropertySchema.FOCUS_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void rootAndNestedWrapPreserveSubtreeStableIdsAndExactHistoryAcrossReopen() throws Exception {
        for (boolean nested : List.of(false, true)) {
            var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                    Map.of(new PropertyName("data"), new PropertyValue.StringValue("User child")), Map.of());
            var root = nested ? new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text)))) : text;
            var initial = open(root, WidgetClassKind.STATELESS);
            var wrapper = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
            var wrapped = apply(initial, new WrapWidget(text.id(), wrapper, CHILD, 0));
            assertEquals(initial.cursor() + 1, wrapped.cursor());
            var result = EventCommandSupport.widget(wrapped.current().document().root(), wrapper.id());
            assertEquals(text, ((WidgetSlot.SingleSlot) result.slots().get(CHILD)).child().orElseThrow());
            assertTrue(source(wrapped).contains("Focus("));
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
                var created = apply(initial, new CreateEventHandler(widget.id(), event.propertyName(), "_focusHandler"));
                assertEquals(initial.cursor() + 1, created.cursor());
                assertTrue(source(created).contains(event.signature().declaration("_focusHandler")), event.toString());
                assertTrue(source(created).contains(event.propertyName().value() + ": _focusHandler"), event.toString());
                for (String uri : event.signature().importUris()) assertTrue(source(created).contains(uri), event.toString());
                assertTrue(source(created).contains("// Preserved user source"));
                if (kind == WidgetClassKind.STATEFUL) {
                    assertTrue(source(created).indexOf(event.signature().declaration("_focusHandler")) > source(created).indexOf("class FormLogic"));
                }
                assertExact(initial, created.undo().session());
                assertExact(created, created.undo().session().redo().session());
                assertExact(created, reopen(created.markSaved()));
                var disconnected = apply(created, new ResetProperty(widget.id(), event.propertyName()));
                assertFalse(source(disconnected).contains(event.propertyName().value() + ": _focusHandler"));
                assertTrue(source(disconnected).contains(event.signature().declaration("_focusHandler")), "Disconnect must preserve user handler");
                assertExact(created, disconnected.undo().session());
            }
        }
    }

    @Test
    void nullAutofocusIsRejectedWithoutLosingRedoWhileNullCallbackRemainsDistinctFromOmission() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATELESS);
        var behavior = new PropertyName("autofocus");
        var autofocus = apply(initial, new SetProperty(widget.id(), behavior, new PropertyValue.BooleanValue(true)));
        var withRedo = autofocus.undo().session();
        var rejected = withRedo.apply(new SetProperty(widget.id(), behavior, new PropertyValue.NullValue()));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
        assertSame(withRedo, rejected.session());
        assertTrue(rejected.session().canRedo());
        assertExact(autofocus, rejected.session().redo().session());
        var event = new PropertyName("onFocusChange");
        var nil = apply(autofocus, new SetProperty(widget.id(), event, new PropertyValue.NullValue()));
        assertTrue(source(nil).contains("onFocusChange: null"));
        var omitted = apply(nil, new ResetProperty(widget.id(), event));
        assertFalse(source(omitted).contains("onFocusChange:"));
        assertExact(nil, omitted.undo().session());
        assertExact(autofocus, omitted.undo().session().undo().session());
        assertExact(omitted, reopen(omitted.markSaved()));
    }

    @Test
    void autofocusStateConsumersPreserveLiteralPreviewAndExactPersistenceForAllThreeTransforms() throws Exception {
        var base = prototype();
        var widget = new WidgetNode(base.id(), TYPE, Map.of(new PropertyName("autofocus"), new PropertyValue.BooleanValue(false)), base.slots());
        var initial = open(widget, WidgetClassKind.STATEFUL);
        for (var binding : List.of(
                new StatePropertyBinding("_autofocus", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_autofocus", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS,
                        Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE))))) {
            var bound = apply(initial, new BindPropertyToState(widget.id(), new PropertyName("autofocus"), binding));
            assertEquals(initial.cursor() + 1, bound.cursor());
            var node = bound.current().document().root();
            assertEquals(new PropertyValue.BooleanValue(false), node.properties().get(new PropertyName("autofocus")));
            assertEquals(binding, node.propertyBindings().get(new PropertyName("autofocus")));
            assertTrue(node.stateBinding().isEmpty(), "Focus has no automatic State producer");
            assertExact(initial, bound.undo().session());
            assertExact(bound, bound.undo().session().redo().session());
            assertExact(bound, reopen(bound.markSaved()));
            var removed = apply(bound, new RemovePropertyStateBinding(widget.id(), new PropertyName("autofocus")));
            assertTrue(source(removed).contains("autofocus: false"));
            assertTrue(source(removed).contains("bool _autofocus = true"));
            assertExact(initial, removed);
        }
    }

    @Test
    void constructorSwitchPreservesInactiveHandlersAllowsRenameAndDisconnectButRejectsNewBindings() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATEFUL);
        var key = new PropertyName("onKeyEvent");
        var created = apply(initial, new CreateEventHandler(widget.id(), key, "_key"));
        assertTrue(source(created).contains("throw UnimplementedError('Implement _key')"));
        var withLabel = apply(created, new SetProperty(widget.id(), new PropertyName("debugLabel"), new PropertyValue.StringValue("Saved focus label")));
        var switched = apply(withLabel, new PatchProperties(widget.id(), List.of(
                new PatchProperties.SetPatch(new PropertyName("focusNode"), reference("_node")),
                new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode")))));
        assertTrue(source(switched).contains("Focus.withExternalFocusNode("));
        assertFalse(source(switched).contains("onKeyEvent: _key"));
        assertFalse(source(switched).contains("debugLabel:"));
        assertEquals(withLabel.current().document().root().properties().get(key), switched.current().document().root().properties().get(key));
        assertTrue(source(switched).contains("KeyEventResult _key(FocusNode node, KeyEvent event)"));
        assertExact(switched, reopen(switched.markSaved()));
        for (var command : List.<DesignerCommand>of(new CreateEventHandler(widget.id(), key, "_newKey"),
                new SetProperty(widget.id(), key, new PropertyValue.CallbackValue("_newKey")),
                new SetProperty(widget.id(), key, reference("_newKey")),
                new ResetProperty(widget.id(), new PropertyName("focusNode")),
                new SetProperty(widget.id(), new PropertyName("focusNode"), new PropertyValue.NullValue()))) {
            var rejected = switched.apply(command);
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status(), command.toString());
            assertSame(switched, rejected.session());
        }
        var renamed = apply(switched, new RenameEventHandler(widget.id(), key, "_renamed"));
        assertTrue(source(renamed).contains("KeyEventResult _renamed(FocusNode node, KeyEvent event)"));
        assertFalse(source(renamed).contains("onKeyEvent:"));
        var restored = apply(renamed, new SetProperty(widget.id(), new PropertyName("variant"), new PropertyValue.StringValue("standard")));
        assertTrue(source(restored).contains("onKeyEvent: _renamed"));
        assertTrue(source(restored).contains("debugLabel: 'Saved focus label'"));
        assertExact(renamed, restored.undo().session());
        var disconnected = apply(renamed, new ResetProperty(widget.id(), key));
        assertFalse(disconnected.current().document().root().properties().containsKey(key));
        assertTrue(source(disconnected).contains("KeyEventResult _renamed"));
        assertExact(renamed, disconnected.undo().session());
    }

    @Test
    void sixStateConsumersIncludeNullableDirectAndInactiveBindingsSurviveConstructorRoundTrip() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATEFUL);
        for (String name : FocusWidgetPropertySchema.booleanProperties()) {
            for (var binding : List.of(
                    new StatePropertyBinding("_autofocus", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                    new StatePropertyBinding("_autofocus", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                    new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS,
                            Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE))))) {
                var bound = apply(initial, new BindPropertyToState(widget.id(), new PropertyName(name), binding));
                assertEquals(binding, bound.current().document().root().propertyBindings().get(new PropertyName(name)));
                assertTrue(bound.current().document().root().stateBinding().isEmpty());
                assertExact(bound, reopen(bound.markSaved()));
            }
        }
        var nullable = new StatePropertyBinding("_optional", StateBinding.Type.NULLABLE_BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        var property = new PropertyName("canRequestFocus");
        var bound = apply(initial, new BindPropertyToState(widget.id(), property, nullable));
        assertTrue(source(bound).contains("canRequestFocus: _optional"));
        var external = apply(bound, new PatchProperties(widget.id(), List.of(
                new PatchProperties.SetPatch(new PropertyName("focusNode"), reference("_node")),
                new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode")))));
        assertFalse(source(external).contains("canRequestFocus:"));
        assertEquals(nullable, external.current().document().root().propertyBindings().get(property));
        assertExact(external, reopen(external.markSaved()));
        var attempt = external.apply(new BindPropertyToState(widget.id(), new PropertyName("skipTraversal"),
                new StatePropertyBinding("_autofocus", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
        assertNotEquals(DesignerCommandStatus.APPLIED, attempt.status());
        assertSame(external, attempt.session());
        var restored = apply(external, new SetProperty(widget.id(), new PropertyName("variant"), new PropertyValue.StringValue("standard")));
        assertTrue(source(restored).contains("canRequestFocus: _optional"));
        assertExact(external, restored.undo().session());
        assertNotEquals(DesignerCommandStatus.APPLIED, initial.apply(new BindPropertyToState(widget.id(), new PropertyName("autofocus"), nullable)).status());
    }

    @Test
    void inactiveStateFieldIsNotRequiredUntilSwitchingBackToStandard() throws Exception {
        var base = prototype();
        var binding = new StatePropertyBinding("_removed", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        var widget = new WidgetNode(base.id(), TYPE, Map.of(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"),
                new PropertyName("focusNode"), reference("_node")), base.slots(), Extensions.empty(), Optional.empty(),
                Map.of(new PropertyName("skipTraversal"), binding));
        var initial = open(widget, WidgetClassKind.STATEFUL);
        assertFalse(source(initial).contains("skipTraversal:"));
        assertExact(initial, reopen(initial.markSaved()));
        var rejected = initial.apply(new SetProperty(widget.id(), new PropertyName("variant"), new PropertyValue.StringValue("standard")));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
        assertSame(initial, rejected.session());
        assertEquals(binding, initial.current().document().root().propertyBindings().get(new PropertyName("skipTraversal")));
    }

    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
        var text = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        return new WidgetNode(base.id(), TYPE, base.properties(), Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(text))));
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
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _autofocus = true;\n  int? _choice = null;\n  bool? _optional = null;\n  final FocusNode _node = FocusNode();\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
