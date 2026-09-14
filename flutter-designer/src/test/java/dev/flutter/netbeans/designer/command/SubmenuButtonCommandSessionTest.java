package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.SubmenuButtonTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class SubmenuButtonCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void allFourSlotsWrapAtomicallyAndRequiredNullableChildCanBeRemoved() throws Exception {
        for (String name : List.of("child", "leadingIcon", "trailingIcon", "menuChildren")) {
            var target = text("Original"); var initial = open(target);
            var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
            var wrapped = apply(initial, new WrapWidget(target.id(), prototype, new SlotName(name), 0));
            assertTrue(source(wrapped).contains("Original")); assertTrue(source(wrapped).contains("SubmenuButton("));
            var removed = apply(wrapped, new RemoveWidget(target.id()));
            assertFalse(source(removed).contains("Original")); assertTrue(source(removed).contains("child: null"));
            assertTrue(source(removed).contains("menuChildren:")); assertFalse(source(removed).contains("onPressed:"));
            assertExact(initial, wrapped.undo().session()); assertExact(wrapped, removed.undo().session());
            assertExact(removed, reopen(removed.markSaved()));
        }
    }
    @Test void wholeAndLocalStylesPatchOnlyTheirOwnFamilyAndPreserveSourceHistory() throws Exception {
        var root = node(Map.of(p("styleElevation"), new PropertyValue.DoubleValue(BigDecimal.ONE), p("menuStyleElevation"), new PropertyValue.DoubleValue(BigDecimal.TEN)));
        var initial = open(root);
        assertRejected(initial, new SetProperty(root.id(), p("style"), reference("_style")));
        assertRejected(initial, new SetProperty(root.id(), p("menuStyle"), new PropertyValue.NullValue()));
        var button = apply(initial, new PatchProperties(root.id(), List.of(new PatchProperties.ResetPatch(p("styleElevation")), new PatchProperties.SetPatch(p("style"), new PropertyValue.NullValue()))));
        assertTrue(source(button).contains("style: null")); assertTrue(source(button).contains("menuStyle: MenuStyle("));
        assertEquals(root.properties().get(p("menuStyleElevation")), button.current().document().root().properties().get(p("menuStyleElevation")));
        var menu = apply(button, new PatchProperties(root.id(), List.of(new PatchProperties.ResetPatch(p("menuStyleElevation")), new PatchProperties.SetPatch(p("menuStyle"), new PropertyValue.NullValue()))));
        assertTrue(source(menu).contains("style: null")); assertTrue(source(menu).contains("menuStyle: null"));
        var reset = apply(menu, new ResetProperty(root.id(), p("menuStyle"))); assertFalse(source(reset).contains("menuStyle:"));
        assertExact(initial, button.undo().session()); assertExact(button, menu.undo().session()); assertExact(menu, reset.undo().session());
        assertExact(reset, reopen(reset.markSaved())); assertTrue(source(reset).contains("// Preserved user source"));
    }
    @Test void iconNullIsStoredAndResetRestoresOmissionWithoutErasingOtherBuckets() throws Exception {
        var root = node(Map.of(p("submenuIconDefault"), PropertyValue.IconDataValue.none())); var initial = open(root);
        var nullBucket = apply(initial, new SetProperty(root.id(), p("submenuIconHovered"), new PropertyValue.NullValue()));
        assertTrue(source(nullBucket).contains("WidgetState.hovered: null")); assertTrue(source(nullBucket).contains("Icon(null)"));
        assertRejected(nullBucket, new SetProperty(root.id(), p("submenuIcon"), new PropertyValue.NullValue()));
        var reset = apply(nullBucket, new ResetProperty(root.id(), p("submenuIconHovered"))); assertExact(initial, reset);
        assertExact(initial, nullBucket.undo().session()); assertExact(nullBucket, reopen(nullBucket.markSaved()));
    }
    @Test void allFiveEventsKeepBodiesSourceOwnedAndRefuseBuilderOrInventedPressed() throws Exception {
        var root = node(Map.of()); var initial = open(root);
        for (String property : List.of("onPressed", "onLongPress", "styleBackgroundBuilder", "submenuIcon")) assertRejected(initial, new CreateEventHandler(root.id(), p(property), "_invalid"));
        for (String name : List.of("onHover", "onFocusChange", "onOpen", "onClose", "onAnimationStatusChanged")) {
            var created = apply(initial, new CreateEventHandler(root.id(), p(name), "_" + name));
            assertTrue(source(created).contains("void _" + name + "("), source(created));
            assertTrue(source(created).contains(name + ": _" + name), source(created));
            var renamed = apply(created, new RenameEventHandler(root.id(), p(name), "_renamed"));
            var detached = apply(renamed, new ResetProperty(root.id(), p(name)));
            assertTrue(source(detached).contains("void _renamed(")); assertFalse(source(detached).contains(name + ": _renamed"));
            assertExact(initial, created.undo().session()); assertExact(created, renamed.undo().session());
            assertExact(renamed, detached.undo().session()); assertExact(detached, reopen(detached.markSaved()));
        }
    }
    @Test void onlyTwoConsumersSupportDirectNotAndEqualsWithNoOpenStateProducer() throws Exception {
        var root = node(Map.of()); var initial = open(root);
        assertRejected(initial, new CreateStateBinding(root.id(), "_flag", "_changed"));
        for (String name : List.of("useRootOverlay", "animated")) {
            for (var transform : List.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT)) {
                var binding = new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), transform);
                var bound = apply(initial, new BindPropertyToState(root.id(), p(name), binding));
                assertTrue(source(bound).contains(name + ": " + (transform == StatePropertyBinding.Transform.NOT ? "!" : "") + "_flag"));
                assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound.markSaved()));
                assertExact(initial, apply(bound, new RemovePropertyStateBinding(root.id(), p(name))));
            }
            var equals = new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.BooleanValue(false)));
            var bound = apply(initial, new BindPropertyToState(root.id(), p(name), equals));
            assertTrue(source(bound).contains("_flag == false"), source(bound));
            assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound.markSaved()));
        }
        for (String name : List.of("enabled", "hoverOpenDelayUs", "submenuIconFocused")) assertRejected(initial, new BindPropertyToState(root.id(), p(name), new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session(); }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) { var result = session.apply(command); assertNotEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); assertSame(session, result.session()); assertExact(session, result.session()); }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) { assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes()); assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes()); assertEquals(expected.current().document(), actual.current().document()); }
    private static DesignerCommandSession reopen(DesignerCommandSession session) { var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG); assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow(); }
    private static DesignerCommandSession open(WidgetNode root) throws Exception {
        var id = StableId.random(); var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64)), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); var generated = result.generated().orElseThrow();
        var doc = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), root);
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload() + "// </netbeans-flutter-designer>\n\nclass Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload() + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(doc), source, CATALOG); assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) { return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATEFUL, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build))); }
}
