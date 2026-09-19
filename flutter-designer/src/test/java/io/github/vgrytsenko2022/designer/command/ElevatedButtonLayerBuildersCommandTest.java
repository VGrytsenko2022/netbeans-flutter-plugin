package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElevatedButtonLayerBuildersCommandTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final PropertyName BACKGROUND = new PropertyName("styleBackgroundBuilder");
    private static final PropertyName FOREGROUND = new PropertyName("styleForegroundBuilder");
    private static final String USER_BODY = "  Widget _background(BuildContext context, Set<WidgetState> states, Widget? child) {\n"
            + "    // User-owned layer body\n    return child ?? const SizedBox();\n  }\n"
            + "  Widget _foreground(BuildContext context, Set<WidgetState> states, Widget? child) => child ?? const SizedBox();\n"
            + "  ButtonLayerBuilder get _layerGetter => _background;\n"
            + "  ButtonLayerBuilder _layerFactory() => _foreground;\n";

    @Test void bothLayersSaveReopenUndoAndResetIndependentlyWithoutClearingSparseStylesOrUserSource() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            var initial = open(kind);
            var id = initial.current().document().root().id();
            var background = apply(initial, new SetProperty(id, BACKGROUND, reference("_background", false)));
            var both = apply(background, new SetProperty(id, FOREGROUND, reference("_foreground", false)));
            assertEquals(initial.cursor() + 2, both.cursor());
            assertTrue(source(both).contains("backgroundBuilder: _background"));
            assertTrue(source(both).contains("foregroundBuilder: _foreground"));
            assertTrue(source(both).contains("enableFeedback: false"));
            assertTrue(source(both).contains("WidgetState.pressed"));
            assertTrue(source(both).contains("child: null"));
            assertTrue(source(both).contains(USER_BODY));
            assertExact(background, both.undo().session());
            assertExact(both, both.undo().session().redo().session());
            var reopened = reopen(both.markSaved());
            assertExact(both, reopened);
            var resetBackground = apply(reopened, new ResetProperty(id, BACKGROUND));
            assertFalse(source(resetBackground).contains("backgroundBuilder:"));
            assertTrue(source(resetBackground).contains("foregroundBuilder: _foreground"));
            assertTrue(source(resetBackground).contains(USER_BODY));
            assertExact(both, resetBackground.undo().session());
            var resetAll = apply(resetBackground, new ResetProperty(id, FOREGROUND));
            assertExact(initial, resetAll);
            assertExact(resetBackground, resetAll.undo().session());
        }
    }

    @Test void gettersFactoriesAndImportedMembersRetainIdentityAndRejectedValuesPreserveRedo() throws Exception {
        var initial = open(WidgetClassKind.STATELESS);
        var id = initial.current().document().root().id();
        var foreground = apply(initial, new SetProperty(id, FOREGROUND, reference("_foreground", false)));
        for (var value : List.of(reference("_layerGetter", false), reference("_layerFactory", true),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/layers.dart"), "Layers", Optional.of("background"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()))) {
            var bound = apply(foreground, new SetProperty(id, BACKGROUND, value));
            assertEquals(value, bound.current().document().root().properties().get(BACKGROUND));
            assertTrue(source(bound).contains(USER_BODY));
            assertExact(bound, reopen(bound.markSaved()));
            var undone = bound.undo().session();
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_background"),
                    new PropertyValue.StringValue("_background"), new PropertyValue.DartExpressionValue("(context, states, child) => child!"))) {
                var rejected = undone.apply(new SetProperty(id, BACKGROUND, invalid));
                assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
                assertSame(undone, rejected.session());
                assertTrue(rejected.session().canRedo());
                assertExact(bound, rejected.session().redo().session());
            }
            assertExact(foreground, apply(bound, new ResetProperty(id, BACKGROUND)));
        }
    }

    @Test void layerBuildersNeverBecomeEventsOrStateBindingsAndExistingPressWorkflowRemainsIntact() throws Exception {
        var initial = open(WidgetClassKind.STATEFUL);
        var id = initial.current().document().root().id();
        var bound = apply(initial, new SetProperty(id, BACKGROUND, reference("_background", false)));
        bound = apply(bound, new SetProperty(id, FOREGROUND, reference("_foreground", false)));
        for (PropertyName property : List.of(BACKGROUND, FOREGROUND)) {
            for (DesignerCommand command : List.of(new CreateEventHandler(id, property, "_newLayer"),
                    new RenameEventHandler(id, property, "_renamed"),
                    new BindPropertyToState(id, property, new StatePropertyBinding("_flag", StateBinding.Type.BOOL,
                            Optional.empty(), StatePropertyBinding.Transform.DIRECT)))) {
                var rejected = bound.apply(command);
                assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
                assertSame(bound, rejected.session());
            }
        }
        var event = apply(bound, new CreateEventHandler(id, new PropertyName("onPressed"), "_createdPressed"));
        assertTrue(source(event).contains("void _createdPressed()"));
        assertTrue(source(event).contains("onPressed: _createdPressed"));
        assertTrue(source(event).contains("backgroundBuilder: _background"));
        assertTrue(source(event).contains("foregroundBuilder: _foreground"));
        assertTrue(source(event).contains(USER_BODY));
        assertExact(bound, event.undo().session());
        var disabled = apply(event, new SetProperty(id, new PropertyName("enabled"), new PropertyValue.BooleanValue(false)));
        assertTrue(source(disabled).contains("onPressed: null"));
        assertTrue(source(disabled).contains("backgroundBuilder: _background"));
        assertTrue(source(disabled).contains("foregroundBuilder: _foreground"));
        assertExact(event, disabled.undo().session());
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
        var root = new WidgetNode(StableId.random(), ElevatedButtonWidgetPropertySchema.ELEVATED_BUTTON_TYPE,
                Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true),
                        new PropertyName("styleEnableFeedback"), new PropertyValue.BooleanValue(false),
                        new PropertyName("stylePressedElevation"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(6))),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
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
