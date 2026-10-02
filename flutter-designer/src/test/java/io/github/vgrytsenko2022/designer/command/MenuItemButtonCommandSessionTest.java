package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.MenuItemButtonTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuItemButtonCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void optionalSlotsWrapReplaceRemoveAndHistoryStayAtomic() throws Exception {
        var child = text("Original"); var initial = open(child, WidgetClassKind.STATELESS);
        for (String name : List.of("child", "leadingIcon", "trailingIcon")) {
            var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
            var wrapped = apply(initial, new WrapWidget(child.id(), prototype, new SlotName(name), 0));
            assertEquals(child, ((WidgetSlot.SingleSlot) wrapped.current().document().root().slots().get(new SlotName(name))).child().orElseThrow());
            var removed = apply(wrapped, new RemoveWidget(child.id()));
            assertTrue(source(removed).contains("MenuItemButton(")); assertFalse(source(removed).contains("Original"));
            assertExact(initial, wrapped.undo().session()); assertExact(wrapped, removed.undo().session()); assertExact(removed, reopen(removed.markSaved()));
        }
    }
    @Test void rawShortcutConflictsRejectWhileExplicitCompoundPatchesPreserveHistoryAndUserSource() throws Exception {
        var root = node(Map.of(p("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", "keyA"), p("shortcutControl"), new PropertyValue.BooleanValue(true), p("semanticsLabel"), s("Preserved label")));
        var initial = open(root, WidgetClassKind.STATELESS);
        assertRejected(initial, new SetProperty(root.id(), p("shortcutCharacter"), s("Ж")));
        assertRejected(initial, new ResetProperty(root.id(), p("shortcutTrigger")));
        assertRejected(initial, new SetProperty(root.id(), p("shortcut"), new PropertyValue.NullValue()));
        var character = apply(initial, new PatchProperties(root.id(), List.of(new PatchProperties.ResetPatch(p("shortcutTrigger")), new PatchProperties.SetPatch(p("shortcutCharacter"), s("Ж")))));
        assertTrue(source(character).contains("CharacterActivator('Ж', control: true)"));
        var whole = apply(character, new PatchProperties(root.id(), List.of(new PatchProperties.ResetPatch(p("shortcutCharacter")), new PatchProperties.ResetPatch(p("shortcutControl")), new PatchProperties.SetPatch(p("shortcut"), new PropertyValue.NullValue()))));
        assertTrue(source(whole).contains("shortcut: null")); assertTrue(source(whole).contains("Preserved label"));
        var omitted = apply(whole, new ResetProperty(root.id(), p("shortcut"))); assertFalse(source(omitted).contains("shortcut:"));
        assertExact(initial, character.undo().session()); assertExact(character, whole.undo().session()); assertExact(whole, omitted.undo().session()); assertExact(omitted, reopen(omitted.markSaved()));
        assertTrue(source(omitted).contains("// Preserved user source"));
    }
    @Test void threeEventsSupportCreateRenameDisconnectAndDisabledRetentionWithoutInventedLongPress() throws Exception {
        var root = node(Map.of()); var initial = open(root, WidgetClassKind.STATEFUL);
        assertRejected(initial, new CreateEventHandler(root.id(), p("onLongPress"), "_longPress"));
        assertRejected(initial, new CreateEventHandler(root.id(), p("shortcut"), "_shortcut"));
        for (String name : List.of("onPressed", "onHover", "onFocusChange")) {
            var created = apply(initial, new CreateEventHandler(root.id(), p(name), "_" + name));
            assertTrue(source(created).contains("void _" + name + "("));
            assertTrue(source(created).contains("// TODO: Handle " + name + "."), source(created));
            assertExact(created, reopen(created.markSaved())); assertExact(initial, created.undo().session());
            var renamed = apply(created, new RenameEventHandler(root.id(), p(name), "_renamed"));
            assertTrue(source(renamed).contains("void _renamed("));
            var disconnected = apply(renamed, new ResetProperty(root.id(), p(name)));
            assertTrue(source(disconnected).contains("void _renamed("));
            assertFalse(source(disconnected).contains(name + ": _renamed"));
            assertExact(created, renamed.undo().session()); assertExact(renamed, disconnected.undo().session());
            assertExact(disconnected, reopen(disconnected.markSaved()));
            if (name.equals("onPressed")) {
                var disabled = apply(created, new SetProperty(root.id(), p("enabled"), new PropertyValue.BooleanValue(false)));
                assertTrue(source(disabled).contains("onPressed: null")); assertTrue(source(disabled).contains("void _onPressed("));
                var restored = apply(disabled, new SetProperty(root.id(), p("enabled"), new PropertyValue.BooleanValue(true)));
                assertTrue(source(restored).contains("onPressed: _onPressed"));
                assertEquals(created.current().document().root().properties().get(p(name)), disabled.current().document().root().properties().get(p(name)));
            }
        }
    }
    @Test void fourStateConsumersUseVerifiedOwnerFieldsAndActivationConditionalWithoutProducer() throws Exception {
        var root = node(Map.of(p("enabled"), new PropertyValue.BooleanValue(false)));
        var initial = open(root, WidgetClassKind.STATEFUL);
        assertRejected(initial, new CreateStateBinding(root.id(), "_active", "_changed"));
        for (String name : List.of("enabled", "autofocus", "requestFocusOnHover", "closeOnActivate")) for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
            var binding = new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), transform);
            var bound = apply(initial, new BindPropertyToState(root.id(), p(name), binding));
            assertFalse(source(bound).contains("enabled:"));
            assertTrue(source(bound).contains(name.equals("enabled") ? " ? () {} : null)" : name + ": " + (transform == StatePropertyBinding.Transform.NOT ? "!" : "") + "_flag"));
            assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound.markSaved()));
            var detached = apply(bound, new RemovePropertyStateBinding(root.id(), p(name))); assertExact(initial, detached);
        }
        assertRejected(initial, new BindPropertyToState(root.id(), p("shortcutControl"), new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session(); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertNotEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); assertSame(session, result.session()); assertExact(session, result.session()); }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) { assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes()); assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes()); assertEquals(expected.current().document(), actual.current().document()); }
    private static DesignerCommandSession reopen(DesignerCommandSession session) { var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG); assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow(); }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind) throws Exception {
        var id = StableId.random(); var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); var generated = result.generated().orElseThrow();
        var doc = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n" : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload() + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload() + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(doc), source, CATALOG); assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) { return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
}
