package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.TooltipTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class TooltipCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test void plainRichSwitchIsAtomicAndUndoReopenPreserveAllUserContent() throws Exception {
        var tile = with(Map.of(p("message"), s("Original"), p("showDurationUs"), i(555)));
        var initial = open(tile, WidgetClassKind.STATELESS);
        assertRejected(initial, new SetProperty(tile.id(), p("richMessage"), reference("_span")));
        assertRejected(initial, new ResetProperty(tile.id(), p("message")));
        var rich = apply(initial, new PatchProperties(tile.id(), List.of(
                new PatchProperties.SetPatch(p("message"), nil()),
                new PatchProperties.SetPatch(p("richMessage"), reference("_span")))));
        assertTrue(source(rich).contains("message: null")); assertTrue(source(rich).contains("richMessage: _span"));
        assertTrue(source(rich).contains("Duration(microseconds: 555)")); assertExact(initial, rich.undo().session()); assertExact(rich, reopen(rich.markSaved()));
        var plain = apply(rich, new PatchProperties(tile.id(), List.of(
                new PatchProperties.ResetPatch(p("richMessage")),
                new PatchProperties.SetPatch(p("message"), s("")))));
        assertTrue(source(plain).contains("message: ''")); assertFalse(source(plain).contains("richMessage:")); assertExact(rich, plain.undo().session());
        assertRejected(rich, new SetProperty(tile.id(), p("richMessage"), nil()));
        assertExact(plain, reopen(plain.markSaved()));
    }

    @Test void optionalChildPaletteAddAndExplicitWrapPreserveTitleIdentityAndHistory() throws Exception {
        var child = ListTileTestValues.text("Original child");
        var row = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(child))));
        var initial = open(row, WidgetClassKind.STATELESS);
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        var added = apply(initial, new AddWidget(new WidgetPlacement(row.id(), new SlotName("children"), 1), prototype));
        assertExact(initial, added.undo().session()); assertExact(added, reopen(added));
        var wrapped = apply(initial, new WrapWidget(child.id(), prototype, new SlotName("child"), 0));
        var tooltip = EventCommandSupport.widget(wrapped.current().document().root(), prototype.id());
        assertEquals(child, ((WidgetSlot.SingleSlot) tooltip.slots().get(new SlotName("child"))).child().orElseThrow());
        assertExact(initial, wrapped.undo().session()); assertExact(wrapped, reopen(wrapped.markSaved()));
        var removed = apply(wrapped, new RemoveWidget(child.id()));
        var remainingChild = EventCommandSupport.widget(removed.current().document().root(), prototype.id()).slots().get(new SlotName("child"));
        assertTrue(remainingChild == null || ((WidgetSlot.SingleSlot) remainingChild).child().isEmpty());
        assertExact(wrapped, removed.undo().session());
    }

    @Test void triggerHandlerCreateRenameDisconnectAndDelegateRejectionPreserveUserBodies() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            var tile = with(Map.of()); var initial = open(tile, kind);
            var created = apply(initial, new CreateEventHandler(tile.id(), p("onTriggered"), "_triggered"));
            assertTrue(source(created).contains("void _triggered()")); assertTrue(source(created).contains("onTriggered: _triggered"));
            assertTrue(source(created).contains("// Preserved user source")); assertExact(initial, created.undo().session()); assertExact(created, reopen(created.markSaved()));
            var renamed = apply(created, new RenameEventHandler(tile.id(), p("onTriggered"), "_renamed"));
            assertTrue(source(renamed).contains("void _renamed()")); assertExact(created, renamed.undo().session());
            var unset = apply(renamed, new ResetProperty(tile.id(), p("onTriggered")));
            assertFalse(source(unset).contains("onTriggered:")); assertTrue(source(unset).contains("void _renamed()"));
            var disabled = apply(unset, new SetProperty(tile.id(), p("onTriggered"), nil()));
            assertTrue(source(disabled).contains("onTriggered: null")); assertExact(unset, disabled.undo().session()); assertExact(disabled, reopen(disabled));
            assertRejected(initial, new CreateEventHandler(tile.id(), p("positionDelegate"), "_position"));
            var delegate = apply(initial, new SetProperty(tile.id(), p("positionDelegate"), reference("_position")));
            assertTrue(source(delegate).contains("positionDelegate: _position")); assertFalse(source(delegate).contains("Offset _position(")); assertExact(delegate, reopen(delegate));
        }
    }

    @Test void durationStyleAndConstraintEditsKeepOmissionNullAndRejectedRedoDistinct() throws Exception {
        var tile = with(Map.of(p("textStyleFontSize"), d("14"), p("height"), i(25))); var initial = open(tile, WidgetClassKind.STATELESS);
        var duration = apply(initial, new SetProperty(tile.id(), p("waitDurationUs"), i(-1)));
        assertTrue(source(duration).contains("waitDuration: const Duration(microseconds: -1)"));
        assertRejected(duration, new SetProperty(tile.id(), p("constraints"), value("constraints")));
        var sizing = apply(duration, new PatchProperties(tile.id(), List.of(new PatchProperties.SetPatch(p("height"), nil()), new PatchProperties.SetPatch(p("constraints"), value("constraints")))));
        assertTrue(source(sizing).contains("height: null")); assertTrue(source(sizing).contains("constraints: const BoxConstraints(")); assertExact(duration, sizing.undo().session());
        assertRejected(sizing, new SetProperty(tile.id(), p("textStyle"), nil()));
        var whole = apply(sizing, new PatchProperties(tile.id(), List.of(new PatchProperties.ResetPatch(p("textStyleFontSize")), new PatchProperties.SetPatch(p("textStyle"), reference("_style")))));
        assertTrue(source(whole).contains("textStyle: _style")); assertExact(sizing, whole.undo().session()); assertExact(whole, reopen(whole.markSaved()));
        var nilDuration = apply(whole, new SetProperty(tile.id(), p("waitDurationUs"), nil()));
        assertTrue(source(nilDuration).contains("waitDuration: null"));
        var reset = apply(nilDuration, new ResetProperty(tile.id(), p("waitDurationUs")));
        assertFalse(source(reset).contains("waitDuration:")); assertExact(nilDuration, reset.undo().session());
        var redo = reset.undo().session(); assertRejected(redo, new SetProperty(tile.id(), p("enableTapToDismiss"), nil()));
        assertRejected(redo, new SetProperty(tile.id(), p("waitDurationUs"), d("1.5")));
        assertTrue(redo.canRedo()); assertExact(reset, redo.redo().session());
    }

    @Test void fiveBooleanStateConsumersDoNotCreateTooltipVisibilityProducer() throws Exception {
        var tile = with(Map.of()); var initial = open(tile, WidgetClassKind.STATEFUL);
        assertRejected(initial, new CreateStateBinding(tile.id(), "_visible", "_visibleChanged"));
        for (String name : TooltipWidgetPropertySchema.booleanProperties()) for (var binding : List.of(
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(i(1))))) {
            var bound = apply(initial, new BindPropertyToState(tile.id(), p(name), binding));
            assertEquals(binding, bound.current().document().root().propertyBindings().get(p(name)));
            assertTrue(source(bound).contains(name + ":")); assertEquals(tile.properties(), bound.current().document().root().properties());
            assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound.markSaved()));
        }
    }

    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session(); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertNotEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); assertSame(session, result.session()); assertExact(session, result.session()); }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) { assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes()); assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes()); }
    private static DesignerCommandSession reopen(DesignerCommandSession session) { var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG); assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow(); }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind) throws Exception {
        var id = StableId.random(); var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG); assertTrue(result.successful(), result.diagnostics().toString()); var generated = result.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n" : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload() + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload() + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n  int? _choice = null;\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG); assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) { return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
}
