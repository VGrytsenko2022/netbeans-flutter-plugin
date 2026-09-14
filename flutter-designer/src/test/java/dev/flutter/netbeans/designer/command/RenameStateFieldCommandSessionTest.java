package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenameStateFieldCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("13e3556d-8c4e-4bdb-ac52-aa58991688d1");
    private static final StableId CONTROL = StableId.parse("653d47ec-7a40-4266-a7b2-de7d868b64f3");
    private static final StableId SECOND = StableId.parse("503d47ec-7a40-4266-a7b2-de7d868b64f3");
    private static final StableId TEXT = StableId.parse("8c70da6a-0921-4b7a-8aec-48961a080197");

    @Test
    void sharedActionsAndConsumersRenameFromEitherWidgetAsOneExactUndoableRevision() throws Exception {
        var initial = open("flutter.material.Switch", "  // _flag remains a comment.\n  String label() => '_flag stays text';\n");
        var created = applied(initial, new CreateStateBinding(CONTROL, "_flag", "_changed"));
        var second = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), SECOND);
        created = applied(created, new AddWidget(new WidgetPlacement(ROOT, new SlotName("children"), 1), second));
        var direct = field("_flag", StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT);
        created = applied(created, new CreateStateBinding(SECOND, "_flag", "_secondChanged", StateBinding.Action.CHANGE,
                Optional.empty(), "", Optional.of(direct)));
        created = applied(created, new BindPropertyToState(TEXT, new PropertyName("data"),
                field("_flag", StateBinding.Type.BOOL, StatePropertyBinding.Transform.TO_STRING)));
        var before = created;
        var renamed = applied(before, new RenameStateField(TEXT, "_flag", "_enabled"));
        assertEquals(before.cursor() + 1, renamed.cursor());
        assertEquals("_enabled", node(renamed, CONTROL).stateBinding().orElseThrow().fieldName());
        assertEquals("_enabled", node(renamed, SECOND).stateBinding().orElseThrow().fieldName());
        assertEquals("_enabled", node(renamed, TEXT).propertyBindings().get(new PropertyName("data")).fieldName());
        assertEquals("_changed", node(renamed, CONTROL).stateBinding().orElseThrow().handlerName());
        assertEquals("_secondChanged", node(renamed, SECOND).stateBinding().orElseThrow().handlerName());
        for (StableId id : List.of(CONTROL, SECOND, TEXT)) assertEquals(node(before, id).properties(), node(renamed, id).properties());
        assertTrue(source(renamed).contains("bool _enabled = false;"), source(renamed));
        assertTrue(source(renamed).contains("_enabled = value;"));
        assertFalse(source(renamed).contains("bool _flag ="));
        assertTrue(source(renamed).contains("// _flag remains a comment."));
        assertTrue(source(renamed).contains("'_flag stays text'"));
        assertExact(before, renamed.undo().session());
        assertExact(renamed, renamed.undo().session().redo().session());
        var saved = renamed.markSaved();
        assertExact(saved, reopen(saved));
        assertExact(before, saved.undo().session());
        var edited = applied(saved, new SetProperty(TEXT, new PropertyName("data"), new PropertyValue.StringValue("Preview only")));
        assertEquals("_enabled", node(edited, TEXT).propertyBindings().get(new PropertyName("data")).fieldName());
        assertFalse(new String(edited.current().fdBytes(), StandardCharsets.UTF_8).contains("setState"));
    }

    @Test
    void textControllerLifecycleAndExplicitListenerReferenceRenameButUpdateHandlerDoesNot() throws Exception {
        var initial = open("flutter.material.TextField", "");
        var created = applied(initial, new CreateStateBinding(CONTROL, "_text", "_textChanged", StateBinding.Action.CHANGE,
                Optional.empty(), "Initial user text", Optional.empty()));
        created = applied(created, new BindPropertyToState(TEXT, new PropertyName("data"),
                field("_text", StateBinding.Type.TEXT_CONTROLLER, StatePropertyBinding.Transform.TEXT)));
        var listener = new PropertyValue.CallbackValue("_textStateListener");
        created = applied(created, new SetProperty(CONTROL, new PropertyName("onEditingComplete"), listener));
        var renamed = applied(created, new RenameStateField(CONTROL, "_text", "_message"));
        String dart = source(renamed);
        assertTrue(dart.contains("TextEditingController _message"), dart);
        assertTrue(dart.contains("_message.addListener(_messageStateListener)"), dart);
        assertTrue(dart.contains("_message.removeListener(_messageStateListener)"), dart);
        assertTrue(dart.contains("_message.dispose()"), dart);
        assertTrue(dart.contains("void _messageStateListener()"), dart);
        assertTrue(dart.contains("void _textChanged(String value)"), dart);
        assertFalse(dart.contains("_textStateListener"), dart);
        assertEquals(new PropertyValue.CallbackValue("_messageStateListener"), node(renamed, CONTROL).properties().get(new PropertyName("onEditingComplete")));
        assertEquals("_textChanged", node(renamed, CONTROL).stateBinding().orElseThrow().handlerName());
        assertExact(created, renamed.undo().session());
        assertExact(renamed, reopen(renamed.markSaved()));
    }

    @Test
    void consumerOnlyFieldCanRenameAndPreserveSourceBodiesWithoutCreatingAnAction() throws Exception {
        var initial = open("flutter.material.Switch", "  String _caption = 'Original';\n  String readCaption() => _caption;\n");
        var bound = applied(initial, new BindPropertyToState(TEXT, new PropertyName("data"),
                field("_caption", StateBinding.Type.STRING, StatePropertyBinding.Transform.DIRECT)));
        var renamed = applied(bound, new RenameStateField(TEXT, "_caption", "_title"));
        assertTrue(node(renamed, TEXT).stateBinding().isEmpty());
        assertTrue(source(renamed).contains("String _title = 'Original';"));
        assertTrue(source(renamed).contains("String readCaption() => _title;"));
        assertExact(bound, renamed.undo().session());
        assertExact(renamed, reopen(renamed.markSaved()));
    }

    @Test
    void sameNameIsNoopAndUnrelatedOrMissingWidgetHasNoRenameAuthority() throws Exception {
        var initial = open("flutter.material.Switch", "");
        var created = applied(initial, new CreateStateBinding(CONTROL, "_flag", "_changed"));
        var unchanged = created.apply(new RenameStateField(CONTROL, "_flag", "_flag"));
        assertEquals(DesignerCommandStatus.NO_CHANGE, unchanged.status());
        assertSame(created, unchanged.session());
        for (RenameStateField command : List.of(new RenameStateField(TEXT, "_flag", "_other"),
                new RenameStateField(CONTROL, "_missing", "_other"),
                new RenameStateField(SECOND, "_flag", "_other"))) {
            var rejected = created.apply(command);
            assertFalse(rejected.changed());
            assertSame(created, rejected.session());
        }
        for (String name : List.of("flag", "_", "__flag", "this._flag", "_flag()", "_" + "x".repeat(128))) {
            assertThrows(IllegalArgumentException.class, () -> new RenameStateField(CONTROL, name, "_valid"));
            assertThrows(IllegalArgumentException.class, () -> new RenameStateField(CONTROL, "_valid", name));
        }
    }

    @Test
    void sourceAndMetadataCollisionsRejectWithoutMergingOrAdvancingHistory() throws Exception {
        var created = applied(open("flutter.material.Switch", "  bool _existing = true;\n"),
                new CreateStateBinding(CONTROL, "_flag", "_changed"));
        created = applied(created, new BindPropertyToState(TEXT, new PropertyName("softWrap"),
                field("_existing", StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT)));
        for (String name : List.of("_existing", "_changed")) {
            var result = created.apply(new RenameStateField(CONTROL, "_flag", name));
            assertFalse(result.changed(), name);
            assertSame(created, result.session());
            assertExact(created, result.session());
        }
        var plainCollision = applied(open("flutter.material.Switch", "  bool _existing = true;\n"),
                new CreateStateBinding(CONTROL, "_flag", "_changed"));
        var result = plainCollision.apply(new RenameStateField(CONTROL, "_flag", "_existing"));
        assertFalse(result.changed());
        assertSame(plainCollision, result.session());
    }

    @Test
    void exactCurrentLibraryReferencesRenameWithoutTouchingForeignReferencesOrPlainStrings() {
        var names = Map.of("_field", "_renamed", "_fieldStateListener", "_renamedStateListener");
        var member = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_field", Optional.of("text"),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var renamed = (PropertyValue.DartObjectReferenceValue) StateCommandSupport.renameReference(member, names);
        assertEquals("_renamed", renamed.rootSymbol());
        assertEquals(member.member(), renamed.member());
        var foreign = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/other.dart"), "Field", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        assertSame(foreign, StateCommandSupport.renameReference(foreign, Map.of("Field", "Renamed")));
        var literal = new PropertyValue.StringValue("_field");
        assertSame(literal, StateCommandSupport.renameReference(literal, names));
        assertEquals(new PropertyValue.CallbackValue("_renamedStateListener"),
                StateCommandSupport.renameReference(new PropertyValue.CallbackValue("_fieldStateListener"), names));
    }

    private static StatePropertyBinding field(String name, StateBinding.Type type, StatePropertyBinding.Transform transform) {
        return new StatePropertyBinding(name, type, Optional.empty(), transform);
    }
    private static PropertyValue.DartObjectReferenceValue local(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode node(DesignerCommandSession session, StableId id) {
        return EventCommandSupport.widget(session.current().document().root(), id);
    }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
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
    private static DesignerCommandSession open(String controlType, String members) throws Exception {
        var control = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId(controlType)).orElseThrow(), CONTROL);
        var text = new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Original")), Map.of());
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(control, text))));
        var id = StableId.random();
        var generated = new DartRegionGenerator().generate(new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root), CATALOG)
                .generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), root);
        byte[] bytes = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatefulWidget {\n  const Sample({super.key});\n"
                + "  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n" + members + "}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), bytes, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATEFUL, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
