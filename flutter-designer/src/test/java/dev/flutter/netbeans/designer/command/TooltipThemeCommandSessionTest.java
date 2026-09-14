package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class TooltipThemeCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final SlotName CHILD = new SlotName("child");

    @Test void requiredChildWrappingRootWrappingRemovalAndHistoryAreAtomic() throws Exception {
        var anchor = text(); var initial = open(anchor, WidgetClassKind.STATELESS);
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        var wrapped = apply(initial, new WrapWidget(anchor.id(), prototype, CHILD, 0));
        assertEquals(anchor, ((WidgetSlot.SingleSlot) wrapped.current().document().root().slots().get(CHILD)).child().orElseThrow());
        assertTrue(source(wrapped).contains("data: const TooltipThemeData()"));
        assertRejected(wrapped, new RemoveWidget(anchor.id()));
        var nested = apply(wrapped, new WrapWidget(prototype.id(), WidgetNodePrototypeFactory.create(definition(), StableId.random()), CHILD, 0));
        assertEquals(2, source(nested).split("TooltipThemeData\\(\\)", -1).length - 1);
        assertExact(wrapped, nested.undo().session()); assertExact(nested, reopen(nested.markSaved()));
        var replacement = text();
        var replaced = apply(wrapped, new ReplaceSlotChild(prototype.id(), CHILD, anchor.id(), new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(replacement, ((WidgetSlot.SingleSlot) replaced.current().document().root().slots().get(CHILD)).child().orElseThrow());
        assertExact(initial, wrapped.undo().session()); assertExact(wrapped, replaced.undo().session());
    }

    @Test void switchingWholeDataNeverSilentlyClearsLocalsAndExplicitPatchPreservesUndoAndSource() throws Exception {
        var root = node(Map.of(p("waitDurationUs"), TooltipTestValues.i(55), p("textStyleFontSize"), TooltipTestValues.d("14"), p("enableFeedback"), TooltipTestValues.nil()));
        var initial = open(root, WidgetClassKind.STATELESS);
        assertRejected(initial, new SetProperty(root.id(), p("data"), value("data")));
        var patches = new ArrayList<PatchProperties.Patch>();
        for (PropertyName name : root.properties().keySet()) patches.add(new PatchProperties.ResetPatch(name));
        patches.add(new PatchProperties.SetPatch(p("data"), value("data")));
        var whole = apply(initial, new PatchProperties(root.id(), patches));
        assertEquals(Map.of(p("data"), value("data")), whole.current().document().root().properties());
        assertTrue(source(whole).contains("data: themeData")); assertFalse(source(whole).contains("TooltipThemeData("));
        for (String name : TooltipThemeWidgetPropertySchema.localProperties()) {
            assertRejected(whole, new SetProperty(root.id(), p(name), value(name)));
            assertRejected(whole, new SetProperty(root.id(), p(name), TooltipTestValues.nil()));
        }
        assertRejected(whole, new SetProperty(root.id(), p("data"), TooltipTestValues.nil()));
        var local = apply(whole, new ResetProperty(root.id(), p("data")));
        assertTrue(local.current().document().root().properties().isEmpty());
        assertTrue(source(local).contains("data: const TooltipThemeData()"));
        assertExact(initial, whole.undo().session()); assertExact(whole, local.undo().session());
        assertExact(whole, reopen(whole.markSaved())); assertExact(local, reopen(local.markSaved()));
        assertTrue(source(local).contains("// Preserved user source"));
    }

    @Test void localDurationStyleSizingResetAndRejectedRedoPreserveEveryUnrelatedField() throws Exception {
        var root = node(Map.of(p("height"), TooltipTestValues.i(20), p("textStyleFontSize"), TooltipTestValues.d("14")));
        var initial = open(root, WidgetClassKind.STATELESS);
        var duration = apply(initial, new SetProperty(root.id(), p("exitDurationUs"), TooltipTestValues.i(-1)));
        assertTrue(source(duration).contains("exitDuration: const Duration(microseconds: -1)"));
        assertRejected(duration, new SetProperty(root.id(), p("constraints"), value("constraints")));
        assertRejected(duration, new SetProperty(root.id(), p("textStyle"), TooltipTestValues.nil()));
        var sizing = apply(duration, new PatchProperties(root.id(), List.of(new PatchProperties.SetPatch(p("height"), TooltipTestValues.nil()), new PatchProperties.SetPatch(p("constraints"), value("constraints")))));
        assertTrue(source(sizing).contains("height: null")); assertTrue(source(sizing).contains("constraints: const BoxConstraints("));
        var explicitNull = apply(sizing, new SetProperty(root.id(), p("exitDurationUs"), TooltipTestValues.nil()));
        assertTrue(source(explicitNull).contains("exitDuration: null"));
        var reset = apply(explicitNull, new ResetProperty(root.id(), p("exitDurationUs")));
        assertFalse(source(reset).contains("exitDuration:"));
        var withRedo = reset.undo().session(); assertTrue(withRedo.canRedo());
        assertRejected(withRedo, new SetProperty(root.id(), p("exitDurationUs"), TooltipTestValues.d("1.5")));
        assertRejected(withRedo, new SetProperty(root.id(), p("data"), value("data")));
        assertTrue(withRedo.canRedo()); assertExact(reset, withRedo.redo().session());
        assertExact(duration, sizing.undo().session()); assertExact(reset, reopen(reset.markSaved()));
    }

    @Test void threeNullableStateConsumersStayInsideDataAndWholeConflictsRemainAtomic() throws Exception {
        var root = node(Map.of()); var initial = open(root, WidgetClassKind.STATEFUL);
        assertRejected(initial, new CreateStateBinding(root.id(), "_visible", "_changed"));
        assertRejected(initial, new CreateEventHandler(root.id(), p("onTriggered"), "_triggered"));
        for (String name : TooltipThemeWidgetPropertySchema.booleanProperties()) for (var binding : List.of(
                new StatePropertyBinding("_nullableFlag", StateBinding.Type.NULLABLE_BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(TooltipTestValues.i(1))))) {
            var bound = apply(initial, new BindPropertyToState(root.id(), p(name), binding));
            assertEquals(binding, bound.current().document().root().propertyBindings().get(p(name)));
            assertTrue(source(bound).contains("data: TooltipThemeData("));
            assertEquals(1, source(bound).split(name + ":", -1).length - 1);
            assertRejected(bound, new SetProperty(root.id(), p("data"), value("data")));
            assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound.markSaved()));
            var detached = apply(bound, new RemovePropertyStateBinding(root.id(), p(name)));
            assertTrue(detached.current().document().root().propertyBindings().isEmpty());
            assertTrue(source(detached).contains("data: const TooltipThemeData()"));
            assertExact(bound, detached.undo().session());
        }
        var whole = apply(initial, new SetProperty(root.id(), p("data"), value("data")));
        assertRejected(whole, new BindPropertyToState(root.id(), p("preferBelow"), new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
    }

    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session(); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertNotEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); assertSame(session, result.session()); assertExact(session, result.session()); }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) { assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes()); assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes()); assertEquals(expected.current().document(), actual.current().document()); }
    private static DesignerCommandSession reopen(DesignerCommandSession session) { var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG); assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow(); }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind) throws Exception {
        var id = StableId.random(); var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG); assertTrue(result.successful(), result.diagnostics().toString()); var generated = result.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n" : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload() + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload() + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n  bool? _nullableFlag = null;\n  int? _choice = null;\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG); assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) { return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
}
