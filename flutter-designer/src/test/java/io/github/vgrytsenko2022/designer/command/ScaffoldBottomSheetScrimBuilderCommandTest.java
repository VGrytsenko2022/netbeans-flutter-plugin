package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScaffoldBottomSheetScrimBuilderCommandTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final PropertyName PROPERTY = new PropertyName("bottomSheetScrimBuilder");

    @Test void propertyReferencesGettersAndFactoriesPreserveUserBodiesAndOneStepHistoryAcrossSaveReopen() throws Exception {
        for (var kind : WidgetClassKind.values()) {
            var initial = open(kind);
            var root = initial.current().document().root();
            for (var value : List.of(reference("_scrim", false), reference("_scrimGetter", false), reference("_scrimFactory", true))) {
                var bound = apply(initial, new SetProperty(root.id(), PROPERTY, value));
                assertEquals(initial.cursor() + 1, bound.cursor());
                assertEquals(value, bound.current().document().root().properties().get(PROPERTY));
                assertTrue(source(bound).contains("bottomSheetScrimBuilder: " + value.rootSymbol() + (value.access() == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION ? "()" : "")));
                assertTrue(source(bound).contains("// User-owned builder body"));
                assertTrue(source(bound).contains("return null;"));
                assertExact(initial, bound.undo().session());
                assertExact(bound, bound.undo().session().redo().session());
                assertExact(bound, reopen(bound.markSaved()));
                var reset = apply(reopen(bound.markSaved()), new ResetProperty(root.id(), PROPERTY));
                assertFalse(source(reset).contains("bottomSheetScrimBuilder:"));
                assertTrue(source(reset).contains("return const Scaffold("));
                assertTrue(source(reset).contains("// User-owned builder body"));
                assertExact(initial, reset);
                assertExact(bound, reset.undo().session());
            }
        }
    }

    @Test void explicitNullAndOtherKindsFailWithoutReplacingSdkOmissionOrLosingRedo() throws Exception {
        var initial = open(WidgetClassKind.STATELESS);
        var root = initial.current().document().root();
        var bound = apply(initial, new SetProperty(root.id(), PROPERTY, reference("_scrim", false)));
        var redo = bound.undo().session();
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.CallbackValue("_scrim"),
                new PropertyValue.StringValue("_scrim"), new PropertyValue.DartExpressionValue("(context, animation) => null"))) {
            var result = redo.apply(new SetProperty(root.id(), PROPERTY, invalid));
            assertNotEquals(DesignerCommandStatus.APPLIED, result.status());
            assertSame(redo, result.session());
            assertTrue(result.session().canRedo());
            assertExact(bound, result.session().redo().session());
        }
        var rejectedBound = bound.apply(new SetProperty(root.id(), PROPERTY, new PropertyValue.NullValue()));
        assertNotEquals(DesignerCommandStatus.APPLIED, rejectedBound.status());
        assertSame(bound, rejectedBound.session());
    }

    @Test void builderCannotMasqueradeAsAnEventOrStateProducerButBothExistingDrawerEventsRemainUsable() throws Exception {
        var initial = open(WidgetClassKind.STATEFUL);
        var id = initial.current().document().root().id();
        var bound = apply(initial, new SetProperty(id, PROPERTY, reference("_scrim", false)));
        for (DesignerCommand command : List.of(new CreateEventHandler(id, PROPERTY, "_newScrim"),
                new RenameEventHandler(id, PROPERTY, "_renamed"),
                new BindPropertyToState(id, PROPERTY, new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT)))) {
            var rejected = bound.apply(command);
            assertNotEquals(DesignerCommandStatus.APPLIED, rejected.status());
            assertSame(bound, rejected.session());
        }
        for (String drawer : List.of("onDrawerChanged", "onEndDrawerChanged")) {
            var event = apply(bound, new CreateEventHandler(id, new PropertyName(drawer), "_drawer"));
            assertTrue(source(event).contains(drawer + ": _drawer"));
            assertTrue(source(event).contains("void _drawer(bool isOpened)"));
            assertEquals(bound.current().document().root().properties().get(PROPERTY), event.current().document().root().properties().get(PROPERTY));
            assertExact(bound, event.undo().session());
        }
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
        var root = WidgetNodePrototypeFactory.create(CATALOG.find(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE).orElseThrow(), StableId.random());
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
                + "  // </netbeans-flutter-designer>\n  bool _flag = false;\n"
                + "  Widget? _scrim(BuildContext context, Animation<double> animation) {\n    // User-owned builder body\n    return null;\n  }\n"
                + "  Widget? Function(BuildContext, Animation<double>) get _scrimGetter => _scrim;\n"
                + "  Widget? Function(BuildContext, Animation<double>) _scrimFactory() => _scrim;\n}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
