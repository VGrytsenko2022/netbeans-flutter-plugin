package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextFieldBuildersCommandTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final PropertyName COUNTER = new PropertyName("buildCounter");
    private static final PropertyName MENU = new PropertyName("contextMenuBuilder");
    private static final String USER_BODY = "  Widget? _counter(BuildContext context, {required int currentLength, required int? maxLength, required bool isFocused}) {\n"
            + "    // User-owned counter body and null-result policy\n    return null;\n  }\n"
            + "  Widget _menu(BuildContext context, EditableTextState editableTextState) => const SizedBox();\n"
            + "  InputCounterWidgetBuilder? get _counterGetter => _counter;\n"
            + "  EditableTextContextMenuBuilder? _menuFactory() => _menu;\n";

    @Test void bothBuildersSaveReopenUndoAndResetIndependentlyWhilePreservingNullAndUserSource() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            var initial = open(kind);
            var id = initial.current().document().root().id();
            var disabledMenu = apply(initial, new SetProperty(id, MENU, new PropertyValue.NullValue()));
            assertTrue(source(disabledMenu).contains("contextMenuBuilder: null"));
            var counter = apply(disabledMenu, new SetProperty(id, COUNTER, reference("_counter", false)));
            var both = apply(counter, new SetProperty(id, MENU, reference("_menu", false)));
            assertTrue(source(both).contains("buildCounter: _counter"));
            assertTrue(source(both).contains("contextMenuBuilder: _menu"));
            assertTrue(source(both).contains("maxLength: 12"));
            assertTrue(source(both).contains("autofocus: true"));
            assertTrue(source(both).contains(USER_BODY));
            assertExact(counter, both.undo().session());
            assertExact(both, both.undo().session().redo().session());
            var reopened = reopen(both.markSaved());
            assertExact(both, reopened);
            var nullCounter = apply(reopened, new SetProperty(id, COUNTER, new PropertyValue.NullValue()));
            assertTrue(source(nullCounter).contains("buildCounter: null"));
            assertTrue(source(nullCounter).contains("contextMenuBuilder: _menu"));
            assertExact(nullCounter, reopen(nullCounter.markSaved()));
            var resetCounter = apply(nullCounter, new ResetProperty(id, COUNTER));
            assertFalse(source(resetCounter).contains("buildCounter:"));
            assertTrue(source(resetCounter).contains("contextMenuBuilder: _menu"));
            assertTrue(source(resetCounter).contains(USER_BODY));
            assertExact(nullCounter, resetCounter.undo().session());
            var resetAll = apply(resetCounter, new ResetProperty(id, MENU));
            assertExact(initial, resetAll);
            assertExact(resetCounter, resetAll.undo().session());
            assertExact(initial, apply(disabledMenu, new ResetProperty(id, MENU)));
        }
    }

    @Test void nullableGettersFactoriesAndImportedMembersRetainIdentityAndInvalidValuesPreserveRedo() throws Exception {
        var initial = open(WidgetClassKind.STATELESS);
        var id = initial.current().document().root().id();
        for (var entry : Map.of(COUNTER, List.of(reference("_counterGetter", false), reference("_counter", false)),
                MENU, List.of(reference("_menuFactory", true),
                        new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/builders.dart"), "Builders", Optional.of("menu"),
                                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()))).entrySet()) {
            for (var value : entry.getValue()) {
                var bound = apply(initial, new SetProperty(id, entry.getKey(), value));
                assertEquals(value, bound.current().document().root().properties().get(entry.getKey()));
                assertTrue(source(bound).contains(USER_BODY));
                assertExact(bound, reopen(bound.markSaved()));
                var undone = bound.undo().session();
                for (PropertyValue invalid : List.of(new PropertyValue.CallbackValue("_counter"), new PropertyValue.BooleanValue(false),
                        new PropertyValue.StringValue("_counter"), new PropertyValue.DartExpressionValue("(context) => null"))) {
                    var rejected = undone.apply(new SetProperty(id, entry.getKey(), invalid));
                    assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
                    assertSame(undone, rejected.session());
                    assertTrue(rejected.session().canRedo());
                    assertExact(bound, rejected.session().redo().session());
                }
                assertExact(initial, apply(bound, new ResetProperty(id, entry.getKey())));
            }
        }
    }

    @Test void buildersNeverBecomeEventsOrStateBindingsWhileExistingTextChangeHandlerStillWorks() throws Exception {
        var initial = open(WidgetClassKind.STATEFUL);
        var id = initial.current().document().root().id();
        var bound = apply(initial, new SetProperty(id, COUNTER, reference("_counter", false)));
        bound = apply(bound, new SetProperty(id, MENU, reference("_menuFactory", true)));
        for (PropertyName property : List.of(COUNTER, MENU)) {
            for (DesignerCommand command : List.of(new CreateEventHandler(id, property, "_newBuilder"),
                    new RenameEventHandler(id, property, "_renamed"),
                    new BindPropertyToState(id, property, new StatePropertyBinding("_flag", StateBinding.Type.BOOL,
                            Optional.empty(), StatePropertyBinding.Transform.DIRECT)))) {
                var rejected = bound.apply(command);
                assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
                assertSame(bound, rejected.session());
            }
        }
        var event = apply(bound, new CreateEventHandler(id, new PropertyName("onChanged"), "_createdChanged"));
        assertTrue(source(event).contains("void _createdChanged(String value)"));
        assertTrue(source(event).contains("onChanged: _createdChanged"));
        assertTrue(source(event).contains("buildCounter: _counter"));
        assertTrue(source(event).contains("contextMenuBuilder: _menuFactory()"));
        assertTrue(source(event).contains(USER_BODY));
        assertExact(bound, event.undo().session());
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
        var root = new WidgetNode(StableId.random(), TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE,
                Map.of(new PropertyName("maxLength"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(12)),
                        new PropertyName("autofocus"), new PropertyValue.BooleanValue(true)), Map.of());
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
