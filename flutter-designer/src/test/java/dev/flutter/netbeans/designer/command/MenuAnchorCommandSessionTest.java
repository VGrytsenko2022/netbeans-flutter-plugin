package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.MenuAnchorTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuAnchorCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void builderCreationIsOnePairedEditWithExactSignatureOpenerAndRetainedChildrenCallbacks() throws Exception {
        for (var kind : WidgetClassKind.values()) for (boolean explicitNull : List.of(false, true)) {
            var original = node(explicitNull ? Map.of(p("builder"), new PropertyValue.NullValue(), p("onOpen"), reference("_opened")) : Map.of(p("onOpen"), reference("_opened")));
            var root = new WidgetNode(original.id(), original.type(), original.properties(), Map.of(
                    new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of(text("Item"))),
                    new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text("Anchor")))));
            var initial = open(root, kind, "  void _opened() {}\n");
            var created = apply(initial, new CreateMenuAnchorBuilder(root.id(), "_menuBuilder"));
            String source = source(created);
            assertEquals(initial.cursor() + 1, created.cursor());
            assertTrue(source.contains("Widget _menuBuilder(BuildContext context, MenuController controller, Widget? child)"));
            assertTrue(source.contains("if (controller.isOpen)")); assertTrue(source.contains("controller.close();")); assertTrue(source.contains("controller.open();"));
            assertTrue(source.contains("child: child ?? const Text('Menu')")); assertTrue(source.contains("builder: _menuBuilder"));
            assertTrue(source.contains("onOpen: _opened")); assertTrue(source.contains("Anchor")); assertTrue(source.contains("Item"));
            assertEquals(root.slots(), created.current().document().root().slots());
            assertTrue(source.indexOf("Widget _menuBuilder") > source.lastIndexOf("// </netbeans-flutter-designer>"));
            assertEquals(reference("_menuBuilder"), created.current().document().root().properties().get(p("builder")));
            assertExact(initial, created.undo().session()); assertExact(created, created.undo().session().redo().session());
            assertExact(created, reopen(created.markSaved()));
        }
    }
    @Test void builderRejectsWrongTargetExistingReferencesIdentifierAndMemberCollisionsWithoutDroppingRedo() throws Exception {
        var root = node(Map.of()); var initial = open(root, WidgetClassKind.STATELESS, "  void _existing() {}\n  int _field = 0;\n");
        var created = apply(initial, new CreateMenuAnchorBuilder(root.id(), "_menuBuilder"));
        assertRejected(created, new CreateMenuAnchorBuilder(root.id(), "_other"));
        assertRejected(initial, new CreateMenuAnchorBuilder(root.id(), "_existing"));
        assertRejected(initial, new CreateMenuAnchorBuilder(root.id(), "_field"));
        assertRejected(initial, new CreateMenuAnchorBuilder(StableId.random(), "_unknown"));
        assertRejected(initial, new CreateEventHandler(root.id(), p("builder"), "_notEvent"));
        var withRedo = created.undo().session();
        assertRejected(withRedo, new CreateMenuAnchorBuilder(root.id(), "_existing")); assertTrue(withRedo.canRedo());
        assertExact(created, withRedo.redo().session());
        var wrong = text("Not a menu"); assertRejected(open(wrong, WidgetClassKind.STATELESS, ""), new CreateMenuAnchorBuilder(wrong.id(), "_wrong"));
        for (String name : List.of("bad();", "a.b", "class", "() => menu")) assertThrows(IllegalArgumentException.class, () -> new CreateMenuAnchorBuilder(root.id(), name));
    }
    @Test void builderSourceRestagingUserBodyDisconnectAndSubsequentPropertiesKeepExactHistory() throws Exception {
        var root = node(Map.of()); var initial = open(root, WidgetClassKind.STATEFUL, "");
        var created = apply(initial, new CreateMenuAnchorBuilder(root.id(), "_menuBuilder"));
        byte[] edited = source(created).replace("Text('Menu')", "Text('User menu')").getBytes(StandardCharsets.UTF_8);
        var restage = created.prepareSourceRestage(created.current().preparedPair().orElseThrow(), edited);
        var accepted = created.acceptSourceRestage(restage).markSaved(restage.preparedPair());
        assertTrue(source(accepted).contains("User menu")); assertExact(initial, accepted.undo().session());
        var changed = apply(accepted, new SetProperty(root.id(), p("animated"), new PropertyValue.BooleanValue(true)));
        assertTrue(source(changed).contains("User menu")); assertTrue(source(changed).contains("animated: true"));
        var disconnected = apply(changed, new ResetProperty(root.id(), p("builder")));
        assertFalse(source(disconnected).contains("builder: _menuBuilder")); assertTrue(source(disconnected).contains("Widget _menuBuilder("));
        assertRejected(disconnected, new CreateMenuAnchorBuilder(root.id(), "_menuBuilder"));
        assertExact(changed, disconnected.undo().session()); assertExact(disconnected, reopen(disconnected.markSaved()));
    }
    @Test void menuListAndAnchorWrapClearMoveAndWholeStyleHistoryRemainAtomic() throws Exception {
        var child = text("Original"); var initial = open(child, WidgetClassKind.STATELESS, "");
        for (String slot : List.of("menuChildren", "child")) {
            var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
            var wrapped = apply(initial, new WrapWidget(child.id(), prototype, new SlotName(slot), 0));
            assertTrue(source(wrapped).contains("Original")); assertTrue(source(wrapped).contains("menuChildren:"));
            var removed = apply(wrapped, new RemoveWidget(child.id())); assertFalse(source(removed).contains("Original"));
            assertTrue(source(removed).contains("menuChildren:")); assertExact(initial, wrapped.undo().session()); assertExact(wrapped, removed.undo().session());
        }
        var root = node(Map.of(p("styleElevation"), new PropertyValue.IntegerValue(BigInteger.ONE)));
        var local = open(root, WidgetClassKind.STATELESS, "");
        assertRejected(local, new SetProperty(root.id(), p("style"), new PropertyValue.NullValue()));
        var whole = apply(local, new PatchProperties(root.id(), List.of(new PatchProperties.ResetPatch(p("styleElevation")), new PatchProperties.SetPatch(p("style"), new PropertyValue.NullValue()))));
        assertTrue(source(whole).contains("style: null")); assertExact(local, whole.undo().session()); assertExact(whole, reopen(whole.markSaved()));
    }
    @Test void threeNativeEventsAndFourStateConsumersWorkWithoutFakeBuilderEventOrDeprecatedState() throws Exception {
        var root = node(Map.of()); var initial = open(root, WidgetClassKind.STATEFUL, "");
        for (String name : List.of("onOpen", "onClose", "onAnimationStatusChanged")) {
            var created = apply(initial, new CreateEventHandler(root.id(), p(name), "_" + name));
            assertTrue(source(created).contains("void _" + name + "("));
            if (name.equals("onAnimationStatusChanged")) assertTrue(source(created).contains("AnimationStatus status"));
            var renamed = apply(created, new RenameEventHandler(root.id(), p(name), "_renamed"));
            var disconnected = apply(renamed, new ResetProperty(root.id(), p(name)));
            assertTrue(source(disconnected).contains("void _renamed(")); assertExact(created, renamed.undo().session());
            assertExact(renamed, disconnected.undo().session()); assertExact(disconnected, reopen(disconnected.markSaved()));
        }
        assertRejected(initial, new CreateStateBinding(root.id(), "_open", "_openChanged"));
        for (String name : MenuAnchorWidgetPropertySchema.booleanProperties()) for (var binding : List.of(
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE))))) {
            var bound = apply(initial, new BindPropertyToState(root.id(), p(name), binding));
            assertTrue(source(bound).contains(name + ": ")); assertExact(initial, bound.undo().session());
            assertExact(initial, apply(bound, new RemovePropertyStateBinding(root.id(), p(name))));
        }
        assertRejected(initial, new BindPropertyToState(root.id(), p("anchorTapClosesMenu"), new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session(); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertNotEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); assertSame(session, result.session()); assertExact(session, result.session()); }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) { assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes()); assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes()); assertEquals(expected.current().document(), actual.current().document()); }
    private static DesignerCommandSession reopen(DesignerCommandSession session) { var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG); assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow(); }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind, String members) throws Exception {
        var id = StableId.random(); var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); var generated = result.generated().orElseThrow();
        var doc = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n" : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload() + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload() + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n  int? _choice = 1;\n" + members + "}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(doc), source, CATALOG); assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) { return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
}
