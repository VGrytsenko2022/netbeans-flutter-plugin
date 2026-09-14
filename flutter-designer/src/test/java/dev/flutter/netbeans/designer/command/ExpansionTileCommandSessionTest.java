package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.ExpansionTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpansionTileCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test void requiredTitleRootAndNestedWrapSaveHistoryAndLaterEditingStayAtomic() throws Exception {
        for (boolean nested : List.of(false, true)) {
            var title = ListTileTestValues.text("User title");
            var row = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(), Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(title))));
            var initial = open(nested ? row : title, WidgetClassKind.STATELESS);
            var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
            if (nested) assertRejected(initial, new AddWidget(new WidgetPlacement(row.id(), new SlotName("children"), 1), prototype));
            var wrapped = apply(initial, new WrapWidget(title.id(), prototype, new SlotName("title"), 0));
            assertEquals(initial.cursor() + 1, wrapped.cursor());
            var tile = EventCommandSupport.widget(wrapped.current().document().root(), prototype.id());
            assertEquals(title, ((WidgetSlot.SingleSlot) tile.slots().get(new SlotName("title"))).child().orElseThrow());
            assertExact(initial, wrapped.undo().session()); assertExact(wrapped, wrapped.undo().session().redo().session());
            assertRejected(wrapped, new RemoveWidget(title.id()));
            assertRejected(wrapped, new MoveWidget(title.id(), new WidgetPlacement(tile.id(), new SlotName("children"), 0)));
            var child = ListTileTestValues.text("Expanded child");
            var added = apply(wrapped, new AddWidget(new WidgetPlacement(tile.id(), new SlotName("children"), 0), child));
            assertExact(wrapped, added.undo().session()); assertExact(added, reopen(added.markSaved()));
            var edited = apply(reopen(added.markSaved()), new SetProperty(title.id(), p("data"), s("Editable after reopen")));
            assertTrue(source(edited).contains("Editable after reopen")); assertExact(added, edited.undo().session());
            var removed = apply(edited, new RemoveWidget(child.id())); assertExact(edited, removed.undo().session());
        }
    }

    @Test void bothShapesAnimationAndOptionalSlotsRoundTripWithoutLosingIndependentValues() throws Exception {
        var tile = fullNode("roundedRectangle", "star"); var initial = open(tile, WidgetClassKind.STATELESS);
        var next = apply(initial, new SetProperty(tile.id(), p("expansionAnimationStyleDurationUs"), i(-1)));
        assertTrue(source(next).contains("Duration(microseconds: -1)")); assertExact(initial, next.undo().session()); assertExact(next, reopen(next));
        assertRejected(next, new SetProperty(tile.id(), p("expansionAnimationStyle"), s("noAnimation")));
        var patches = new ArrayList<PatchProperties.Patch>();
        for (String name : ExpansionTileWidgetPropertySchema.animationStyleLocalProperties()) patches.add(new PatchProperties.ResetPatch(p(name)));
        patches.add(new PatchProperties.SetPatch(p("expansionAnimationStyle"), s("noAnimation")));
        var whole = apply(next, new PatchProperties(tile.id(), patches));
        assertTrue(source(whole).contains("AnimationStyle.noAnimation")); assertEquals(tile.slots(), whole.current().document().root().slots());
        assertEquals(tile.properties().get(p("collapsedShapeKind")), whole.current().document().root().properties().get(p("collapsedShapeKind")));
        assertExact(next, whole.undo().session()); assertExact(whole, reopen(whole.markSaved()));
        var omitted = apply(whole, new ResetProperty(tile.id(), p("expansionAnimationStyle")));
        assertFalse(source(omitted).contains("expansionAnimationStyle:"));
        var nil = apply(omitted, new SetProperty(tile.id(), p("expansionAnimationStyle"), nil()));
        assertTrue(source(nil).contains("expansionAnimationStyle: null")); assertExact(omitted, nil.undo().session());
        var reset = apply(nil, new PatchProperties(tile.id(), nil.current().document().root().properties().keySet().stream().map(name -> (PatchProperties.Patch) new PatchProperties.ResetPatch(name)).toList()));
        assertTrue(reset.current().document().root().properties().isEmpty()); assertEquals(tile.slots(), reset.current().document().root().slots()); assertExact(nil, reset.undo().session());
    }

    @Test void expansionEventCreateBindRenameAndDisconnectPreserveUserBodiesWhenDisabled() throws Exception {
        var event = WidgetEventCatalog.eventsFor(definition()).getFirst();
        for (var kind : WidgetClassKind.values()) {
            var tile = node(Map.of(p("enabled"), b(false))); var initial = open(tile, kind);
            var created = apply(initial, new CreateEventHandler(tile.id(), event.propertyName(), "_expanded"));
            assertTrue(source(created).contains("void _expanded(bool isExpanded)")); assertTrue(source(created).contains("onExpansionChanged: _expanded"));
            assertTrue(source(created).contains("// Preserved user source")); assertExact(initial, created.undo().session()); assertExact(created, reopen(created.markSaved()));
            var renamed = apply(created, new RenameEventHandler(tile.id(), event.propertyName(), "_renamed"));
            assertTrue(source(renamed).contains("void _renamed(bool isExpanded)")); assertExact(created, renamed.undo().session());
            var unset = apply(renamed, new ResetProperty(tile.id(), event.propertyName()));
            assertFalse(source(unset).contains("onExpansionChanged:")); assertTrue(source(unset).contains("void _renamed(bool isExpanded)"));
            var nil = apply(unset, new SetProperty(tile.id(), event.propertyName(), nil()));
            assertTrue(source(nil).contains("onExpansionChanged: null")); assertExact(unset, nil.undo().session()); assertExact(nil, reopen(nil));
            var rebound = apply(nil, new SetProperty(tile.id(), event.propertyName(), new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_renamed", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            assertTrue(source(rebound).contains("onExpansionChanged: _renamed")); assertTrue(source(rebound).contains("enabled: false")); assertExact(rebound, reopen(rebound));
        }
    }

    @Test void sixStateConsumersSupportAllBooleanTransformsButInitialSeedCannotBecomeControlled() throws Exception {
        var tile = node(Map.of(p("initiallyExpanded"), b(true))); var initial = open(tile, WidgetClassKind.STATEFUL);
        var direct = new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        assertRejected(initial, new BindPropertyToState(tile.id(), p("initiallyExpanded"), direct));
        assertRejected(initial, new CreateStateBinding(tile.id(), "_expanded", "_expandedChanged"));
        for (String name : List.of("showTrailingIcon", "maintainState", "dense", "enableFeedback", "enabled", "internalAddSemanticForOnTap")) {
            for (var binding : List.of(direct, new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                    new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(i(1))))) {
                var bound = apply(initial, new BindPropertyToState(tile.id(), p(name), binding));
                assertEquals(binding, bound.current().document().root().propertyBindings().get(p(name)));
                assertTrue(source(bound).contains(name + ":")); assertTrue(source(bound).contains("initiallyExpanded: true"));
                assertEquals(tile.properties(), bound.current().document().root().properties()); assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound.markSaved()));
            }
        }
    }

    @Test void rejectedAnimationAndBaselineEditsPreserveRedoAndAllDurableBytes() throws Exception {
        var tile = node(Map.of()); var initial = open(tile, WidgetClassKind.STATELESS);
        var edited = apply(initial, new SetProperty(tile.id(), p("expansionAnimationStyleCurve"), s("easeInBack")));
        var redo = edited.undo().session();
        for (var command : List.of(new SetProperty(tile.id(), p("expandedCrossAxisAlignment"), new PropertyValue.EnumValue("CrossAxisAlignment", "baseline")),
                new SetProperty(tile.id(), p("initiallyExpanded"), nil()), new SetProperty(tile.id(), p("expansionAnimationStyleDurationUs"), d("1.5")),
                new SetProperty(tile.id(), p("expansionAnimationStyleCurve"), s("arbitrary()")))) assertRejected(redo, command);
        assertTrue(redo.canRedo()); assertExact(edited, redo.redo().session()); assertExact(edited, reopen(edited));
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
