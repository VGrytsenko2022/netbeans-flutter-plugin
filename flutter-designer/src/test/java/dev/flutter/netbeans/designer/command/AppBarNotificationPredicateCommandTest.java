package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppBarNotificationPredicateCommandTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final PropertyName PROPERTY = new PropertyName("notificationPredicate");
    private static final String USER_BODY = "  bool _accept(ScrollNotification notification) {\n"
            + "    // User-owned predicate body\n    return notification.depth > 0;\n  }\n"
            + "  ScrollNotificationPredicate get _predicateGetter => _accept;\n"
            + "  ScrollNotificationPredicate _predicateFactory() => _accept;\n";

    @Test void presetsAndTypedReferencesSurviveSaveReopenAndOneStepHistoryWithoutRewritingUserBodies() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            var initial = open(kind);
            var id = initial.current().document().root().id();
            List<PropertyValue> values = new ArrayList<>();
            AppBarWidgetPropertySchema.notificationPredicatePresets().forEach(preset -> values.add(new PropertyValue.StringValue(preset)));
            values.add(reference("_accept", false));
            values.add(reference("_predicateGetter", false));
            values.add(reference("_predicateFactory", true));
            values.add(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/predicates.dart"), "Predicates", Optional.of("accept"),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
            for (var value : values) {
                var bound = apply(initial, new SetProperty(id, PROPERTY, value));
                assertEquals(initial.cursor() + 1, bound.cursor());
                assertEquals(value, bound.current().document().root().properties().get(PROPERTY));
                assertTrue(source(bound).contains("notificationPredicate:"));
                assertTrue(source(bound).contains(USER_BODY));
                assertFalse(source(bound).contains("const AppBar("));
                assertExact(initial, bound.undo().session());
                assertExact(bound, bound.undo().session().redo().session());
                var reopened = reopen(bound.markSaved());
                assertExact(bound, reopened);
                var reset = apply(reopened, new ResetProperty(id, PROPERTY));
                assertFalse(source(reset).contains("notificationPredicate:"));
                assertTrue(source(reset).contains("return AppBar();"));
                assertTrue(source(reset).contains(USER_BODY));
                assertExact(initial, reset);
                assertExact(bound, reset.undo().session());
            }
        }
    }

    @Test void switchingPresetAndReferenceKeepsDistinctValuesAndRejectedEditsPreserveRedo() throws Exception {
        var initial = open(WidgetClassKind.STATELESS);
        var id = initial.current().document().root().id();
        var preset = apply(initial, new SetProperty(id, PROPERTY, new PropertyValue.StringValue("depthZero")));
        var bound = apply(preset, new SetProperty(id, PROPERTY, reference("_accept", false)));
        assertTrue(source(bound).contains("notificationPredicate: _accept"));
        assertFalse(source(bound).contains("notificationPredicate: (notification)"));
        assertExact(preset, bound.undo().session());
        var undone = bound.undo().session();
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_accept"),
                new PropertyValue.StringValue("_accept"), new PropertyValue.StringValue("depth1"),
                new PropertyValue.DartExpressionValue("(_) => true"))) {
            var rejected = undone.apply(new SetProperty(id, PROPERTY, invalid));
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
            assertSame(undone, rejected.session());
            assertTrue(rejected.session().canRedo());
            assertExact(bound, rejected.session().redo().session());
        }
        var restoredPreset = apply(bound, new SetProperty(id, PROPERTY, new PropertyValue.StringValue("all")));
        assertTrue(source(restoredPreset).contains("notificationPredicate: (_) => true"));
        assertTrue(source(restoredPreset).contains(USER_BODY));
        assertExact(bound, restoredPreset.undo().session());
    }

    @Test void predicateDoesNotBecomeAnEventOrStateBindingAndDoesNotBlockOtherAppBarProperties() throws Exception {
        var initial = open(WidgetClassKind.STATEFUL);
        var id = initial.current().document().root().id();
        var bound = apply(initial, new SetProperty(id, PROPERTY, reference("_accept", false)));
        for (DesignerCommand command : List.of(new CreateEventHandler(id, PROPERTY, "_newPredicate"),
                new RenameEventHandler(id, PROPERTY, "_renamed"),
                new BindPropertyToState(id, PROPERTY, new StatePropertyBinding("_flag", StateBinding.Type.BOOL,
                        Optional.empty(), StatePropertyBinding.Transform.DIRECT)))) {
            var rejected = bound.apply(command);
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
            assertSame(bound, rejected.session());
        }
        var edited = apply(bound, new SetProperty(id, new PropertyName("centerTitle"), new PropertyValue.BooleanValue(true)));
        assertTrue(source(edited).contains("centerTitle: true"));
        assertTrue(source(edited).contains("notificationPredicate: _accept"));
        assertTrue(source(edited).contains(USER_BODY));
        assertExact(bound, edited.undo().session());
    }

    private static PropertyValue.DartObjectReferenceValue reference(String name, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session();
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
    private static DesignerCommandSession open(WidgetClassKind kind) throws Exception {
        var id = StableId.random();
        var root = new WidgetNode(StableId.random(), AppBarWidgetPropertySchema.APP_BAR_TYPE, Map.of(), Map.of());
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var generation = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(generation.successful(), generation.diagnostics().toString());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => SampleState();\n}\nclass SampleState extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n  bool _flag = false;\n" + USER_BODY + "}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
