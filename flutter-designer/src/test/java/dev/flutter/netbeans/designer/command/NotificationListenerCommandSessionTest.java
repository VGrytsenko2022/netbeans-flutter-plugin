package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationListenerCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final PropertyName EVENT = new PropertyName("onNotification");

    @Test void rootAndNestedWrapPreserveSubtreeIdsOneStepHistoryAndEditabilityAfterReopen() throws Exception {
        for (boolean nested : List.of(false, true)) {
            var text = text();
            var root = nested ? new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(text)))) : text;
            var initial = open(root, WidgetClassKind.STATELESS);
            var wrapper = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
            var wrapped = apply(initial, new WrapWidget(text.id(), wrapper, CHILD, 0));
            assertEquals(initial.cursor() + 1, wrapped.cursor());
            var result = EventCommandSupport.widget(wrapped.current().document().root(), wrapper.id());
            assertEquals(text, ((WidgetSlot.SingleSlot) result.slots().get(CHILD)).child().orElseThrow());
            assertTrue(source(wrapped).contains("NotificationListener<Notification>("));
            assertExact(initial, wrapped.undo().session());
            assertExact(wrapped, wrapped.undo().session().redo().session());
            assertExact(wrapped, reopen(wrapped.markSaved()));
            var edited = apply(reopen(wrapped.markSaved()), new SetProperty(text.id(), new PropertyName("data"), new PropertyValue.StringValue("Editable child")));
            assertTrue(source(edited).contains("Editable child"));
            assertExact(wrapped, edited.undo().session());
        }
    }

    @Test void completedSubtreeAddsAtomicallyWhileMissingRequiredChildCannotEnterTreeOrLoseRedo() throws Exception {
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())));
        var initial = open(root, WidgetClassKind.STATELESS);
        var placement = new WidgetPlacement(root.id(), new SlotName("children"), 0);
        var added = apply(initial, new AddWidget(placement, prototype()));
        assertEquals(initial.cursor() + 1, added.cursor());
        assertExact(initial, added.undo().session());
        var redo = added.undo().session();
        var rejected = redo.apply(new AddWidget(placement, WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random())));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
        assertSame(redo, rejected.session());
        assertTrue(rejected.session().canRedo());
        assertExact(added, rejected.session().redo().session());
        assertExact(added, reopen(added.markSaved()));
    }

    @Test void sharedBoolHandlerCreationRenameDisconnectAndNullAreExactAtomicUserOwnedEdits() throws Exception {
        var event = WidgetEventCatalog.defaultEventFor(CATALOG.find(TYPE).orElseThrow()).orElseThrow();
        for (var kind : WidgetClassKind.values()) {
            var widget = prototype();
            var initial = open(widget, kind);
            var created = apply(initial, new CreateEventHandler(widget.id(), EVENT, "_notice"));
            assertTrue(source(created).contains("bool _notice(Notification notification)"));
            assertTrue(source(created).contains("throw UnimplementedError('Implement _notice')"));
            assertTrue(source(created).contains("onNotification: _notice"));
            assertTrue(source(created).contains("// Preserved user source"));
            assertExact(initial, created.undo().session());
            assertExact(created, created.undo().session().redo().session());
            assertExact(created, reopen(created.markSaved()));
            var renamed = apply(created, new RenameEventHandler(widget.id(), EVENT, "_renamed"));
            assertTrue(source(renamed).contains(event.signature().declaration("_renamed")));
            var nil = apply(renamed, new SetProperty(widget.id(), EVENT, new PropertyValue.NullValue()));
            assertTrue(source(nil).contains("onNotification: null"));
            assertTrue(source(nil).contains(event.signature().declaration("_renamed")));
            var disconnected = apply(nil, new ResetProperty(widget.id(), EVENT));
            assertFalse(source(disconnected).contains("onNotification:"));
            assertTrue(source(disconnected).contains(event.signature().declaration("_renamed")));
            assertExact(nil, disconnected.undo().session());
            assertExact(disconnected, reopen(disconnected.markSaved()));
        }
    }

    @Test void changingAnyPresetOrCustomTypeRetainsCallbackAndRefreshesOnlyItsSelectedGenericProof() throws Exception {
        var widget = prototype();
        var initial = apply(open(widget, WidgetClassKind.STATELESS), new CreateEventHandler(widget.id(), EVENT, "_notice"));
        var types = new ArrayList<PropertyValue>();
        NotificationListenerWidgetPropertySchema.typePresets().stream().filter(name -> !name.equals("Notification"))
                .forEach(name -> types.add(new PropertyValue.StringValue(name)));
        types.add(reference("CustomNotice"));
        for (var type : types) {
            var changed = apply(initial, new SetProperty(widget.id(), new PropertyName("notificationType"), type));
            assertEquals(initial.current().document().root().properties().get(EVENT), changed.current().document().root().properties().get(EVENT));
            assertTrue(source(changed).contains("bool _notice(Notification notification)"));
            assertTrue(source(changed).contains("onNotification: _notice"));
            assertExact(initial, changed.undo().session());
            assertExact(changed, changed.undo().session().redo().session());
            assertExact(changed, reopen(changed.markSaved()));
        }
    }

    @Test void genericTypeRejectsResetNullFactoryMemberAndUnknownPresetWithoutChangingHistory() throws Exception {
        var widget = prototype();
        var initial = open(widget, WidgetClassKind.STATELESS);
        var property = new PropertyName("notificationType");
        for (DesignerCommand command : List.of(new ResetProperty(widget.id(), property),
                new SetProperty(widget.id(), property, new PropertyValue.NullValue()),
                new SetProperty(widget.id(), property, new PropertyValue.StringValue("Notification?")),
                new SetProperty(widget.id(), property, new PropertyValue.DartObjectReferenceValue(Optional.empty(), "createNotice", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false))),
                new SetProperty(widget.id(), property, new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Owner", Optional.of("Nested"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())))) {
            var rejected = initial.apply(command);
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status(), command.toString());
            assertSame(initial, rejected.session());
            assertExact(initial, rejected.session());
        }
        var stateful = open(widget, WidgetClassKind.STATEFUL);
        assertNotEquals(DesignerCommandStatus.APPLIED, stateful.apply(new BindPropertyToState(widget.id(), EVENT,
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT))).status());
    }

    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode text() {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue("User child")), Map.of());
    }
    private static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
        return new WidgetNode(base.id(), TYPE, base.properties(), Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(text()))));
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind) throws Exception {
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var generation = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(generation.successful(), generation.diagnostics().toString());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass CustomNotice extends Notification { const CustomNotice(); }\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = false;\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
