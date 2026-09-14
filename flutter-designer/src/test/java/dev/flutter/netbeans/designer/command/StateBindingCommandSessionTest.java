package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateBindingCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("13e3556d-8c4e-4bdb-ac52-aa58991688d1");
    private static final StableId CONTROL = StableId.parse("653d47ec-7a40-4266-a7b2-de7d868b64f3");
    private static final StableId TEXT = StableId.parse("8c70da6a-0921-4b7a-8aec-48961a080197");
    private static final PropertyName CHANGED = new PropertyName("onChanged");

    @Test void allSixControlsCreateOneTypedRevisionWithLiteralPreviewsAndExactHistory() throws Exception {
        for (String type : List.of("flutter.material.Checkbox", "flutter.material.CheckboxListTile",
                "flutter.material.Switch", "flutter.material.Slider", "flutter.material.RangeSlider",
                "flutter.widgets.RadioGroup")) {
            var initial = open(type, Map.of(), "", WidgetClassKind.STATEFUL);
            Map<PropertyName, PropertyValue> previews = widget(initial).properties();
            var created = applied(initial, new CreateStateBinding(CONTROL, "_controlled", "_controlChanged"));
            assertEquals(initial.cursor() + 1, created.cursor());
            assertEquals("FormLogic", created.current().sourceIntegrity().verifiedMemberClassName().orElseThrow());
            assertEquals("_controlChanged", EventCommandSupport.localHandler(widget(created).properties().get(CHANGED)).orElseThrow());
            previews.forEach((name, value) -> { if (!name.equals(CHANGED)) assertEquals(value, widget(created).properties().get(name)); });
            assertTrue(source(created).contains("setState(() {"), type + source(created));
            assertTrue(source(created).contains("_controlled = value;"));
            if (type.endsWith("Checkbox") || type.endsWith("CheckboxListTile")) {
                assertTrue(source(created).contains("bool _controlled = false;"));
                assertTrue(source(created).contains("if (value == null) return;"));
            } else if (type.endsWith("Switch")) {
                assertTrue(source(created).contains("bool _controlled = false;"));
                assertTrue(source(created).contains("void _controlChanged(bool value)"));
            } else if (type.endsWith("RangeSlider")) {
                assertTrue(source(created).contains("RangeValues _controlled = RangeValues("));
            } else if (type.endsWith("Slider")) {
                assertTrue(source(created).contains("double _controlled = "));
            } else {
                assertTrue(source(created).contains("String? _controlled = null;"));
            }
            assertExact(initial, created.undo().session());
            assertExact(created, created.undo().session().redo().session());
            var edited = applied(created, new SetProperty(TEXT, new PropertyName("data"), new PropertyValue.StringValue("Later")));
            assertEquals(widget(created).stateBinding(), widget(edited).stateBinding());
            var saved = edited.markSaved();
            assertExact(saved, reopen(saved));
            assertExact(created, saved.undo().session());
            assertExact(initial, saved.undo().session().undo().session());
            assertFalse(new String(saved.current().fdBytes(), StandardCharsets.UTF_8).contains("setState"));
        }
    }

    @Test void nullableCheckboxAndAllRadioBuiltinTypesHaveExactFieldsAndSafeInitializers() throws Exception {
        var checkbox = applied(open("flutter.material.Checkbox", Map.of(
                new PropertyName("tristate"), new PropertyValue.BooleanValue(true),
                new PropertyName("value"), new PropertyValue.NullValue()), "", WidgetClassKind.STATEFUL),
                new CreateStateBinding(CONTROL, "_nullable", "_changed"));
        assertTrue(source(checkbox).contains("bool? _nullable = null;"));
        assertFalse(source(checkbox).contains("if (value == null)"));
        var mismatchedType = checkbox.apply(new SetProperty(CONTROL, new PropertyName("tristate"), new PropertyValue.BooleanValue(false)));
        assertFalse(mismatchedType.changed());
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            var created = applied(open("flutter.widgets.RadioGroup", Map.of(
                    new PropertyName("valueType"), new PropertyValue.StringValue(type)), "", WidgetClassKind.STATEFUL),
                    new CreateStateBinding(CONTROL, "_selected", "_changed"));
            assertTrue(source(created).contains(type + "? _selected = null;"), source(created));
            assertTrue(source(created).contains("void _changed(" + type + "? value)"));
            assertExact(created, reopen(created));
        }
        String text = "'quoted' \\ path $interpolation\nline";
        var string = applied(open("flutter.widgets.RadioGroup", Map.of(
                new PropertyName("groupValue"), new PropertyValue.StringValue(text)), "", WidgetClassKind.STATEFUL),
                new CreateStateBinding(CONTROL, "_selected", "_changed"));
        assertTrue(source(string).contains("\\$interpolation\\u000aline"));
        assertEquals(new PropertyValue.StringValue(text), widget(string).properties().get(new PropertyName("groupValue")));
    }

    @Test void projectRadioTypesAndInitialValuesKeepUserOwnedImports() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/choice.dart"),
                "Choice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var choice = new PropertyValue.DartObjectReferenceValue(reference.libraryUri(), "Choice", Optional.of("first"),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var created = applied(open("flutter.widgets.RadioGroup", Map.of(new PropertyName("valueType"), reference,
                new PropertyName("groupValue"), choice), "", WidgetClassKind.STATEFUL),
                new CreateStateBinding(CONTROL, "_selected", "_changed"));
        assertTrue(source(created).contains("Choice? _selected = Choice.first;"));
        assertRetainedImportOutsideGuards(source(created), "package:app/choice.dart");
        var removed = applied(created, new RemoveStateBinding(CONTROL));
        assertTrue(source(removed).contains("Choice? _selected = Choice.first;"));
        assertRetainedImportOutsideGuards(source(removed), "package:app/choice.dart");
    }

    private static void assertRetainedImportOutsideGuards(String source, String uri) {
        String closingMarker = "// </netbeans-flutter-designer>";
        int importsEnd = source.indexOf(closingMarker) + closingMarker.length();
        int classStart = source.indexOf("class Sample");
        assertTrue(importsEnd >= closingMarker.length() && classStart > importsEnd, source);
        String userDirectives = source.substring(importsEnd, classStart);
        String declaration = "import '" + uri + "';";
        assertEquals(1, userDirectives.split(java.util.regex.Pattern.quote(declaration), -1).length - 1,
                "Exactly one retained user import must remain outside the imports guard and before the owning class.");
        assertTrue(source.startsWith("// <netbeans-flutter-designer region=\"imports\">\n"),
                "The native imports guard remains at its original file boundary.");
    }

    @Test void radioNonfiniteInitializersAreClosedNumericForms() throws Exception {
        for (var entry : Map.of("infinity", "(1.0 / 0.0)", "negativeInfinity", "(-1.0 / 0.0)",
                "nan", "(0.0 / 0.0)").entrySet()) {
            var created = applied(open("flutter.widgets.RadioGroup", Map.of(
                    new PropertyName("valueType"), new PropertyValue.StringValue("double"),
                    new PropertyName("groupValue"), new PropertyValue.EnumValue("double", entry.getKey())),
                    "", WidgetClassKind.STATEFUL), new CreateStateBinding(CONTROL, "_selected", "_changed"));
            assertTrue(source(created).contains("double? _selected = " + entry.getValue() + ";"));
            assertExact(created, reopen(created));
        }
    }

    @Test void creationRejectsNamesAlreadyUsedByManagedReferences() throws Exception {
        var initial = open("flutter.material.Switch", Map.of(new PropertyName("focusNode"),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_controlled", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())), "", WidgetClassKind.STATEFUL);
        var rejected = initial.apply(new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        assertFalse(rejected.changed());
        assertSame(initial, rejected.session());
    }

    @Test void creatingHandlerNeverShadowsAnotherWidgetsCompatibleTopLevelCallback() throws Exception {
        var baseline = open("flutter.material.Switch", Map.of(), "", WidgetClassKind.STATEFUL);
        var initial = DesignerCommandSession.open(baseline.current().fdSnapshot(),
                (source(baseline) + "void _changed(bool value) {}\n").getBytes(StandardCharsets.UTF_8), CATALOG)
                .session().orElseThrow();
        var other = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.material.Switch")).orElseThrow(),
                StableId.random(), Map.of(CHANGED, new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                        "_changed", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        var withOther = applied(initial, new AddWidget(new WidgetPlacement(ROOT, new SlotName("children"), 2), other));
        var rejected = withOther.apply(new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        assertFalse(rejected.changed());
        assertSame(withOther, rejected.session());
        assertTrue(source(withOther).contains("void _changed(bool value) {}"));
    }

    @Test void treeMovesWrapsAndRemovalsPreserveBindingMetadataAndNeverDeleteSource() throws Exception {
        var initial = applied(open("flutter.material.Switch", Map.of(), "", WidgetClassKind.STATEFUL),
                new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        var wrapper = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Center")).orElseThrow(),
                StableId.random());
        var wrapped = applied(initial, new WrapWidget(CONTROL, wrapper, new SlotName("child"), 0));
        assertEquals(widget(initial).stateBinding(), widget(wrapped).stateBinding());
        var moved = applied(wrapped, new MoveWidget(CONTROL, new WidgetPlacement(ROOT, new SlotName("children"), 2)));
        assertEquals(widget(initial).stateBinding(), widget(moved).stateBinding());
        var removed = applied(moved, new RemoveWidget(CONTROL));
        assertTrue(source(removed).contains("bool _controlled = false;"));
        assertTrue(source(removed).contains("void _changed(bool value)"));
        assertExact(moved, removed.undo().session());
        assertExact(wrapped, removed.undo().session().undo().session());
    }

    @Test void removingRestoresOriginalCallbackButNeverDeletesOrRewritesUserMembers() throws Exception {
        var initial = open("flutter.material.Switch", Map.of(), "  void _independent(bool value) {}\n", WidgetClassKind.STATEFUL);
        var created = applied(initial, new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        var renamed = applied(created, new RenameEventHandler(CONTROL, CHANGED, "_renamed"));
        assertEquals("_renamed", widget(renamed).stateBinding().orElseThrow().handlerName());
        var removed = applied(renamed, new RemoveStateBinding(CONTROL));
        assertTrue(widget(removed).stateBinding().isEmpty());
        assertEquals(widget(initial).properties(), widget(removed).properties());
        assertTrue(source(removed).contains("void _renamed(bool value)"));
        assertTrue(source(removed).contains("bool _controlled = false;"));
        var independent = applied(renamed, new SetProperty(CONTROL, CHANGED,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_independent", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        var independentlyRemoved = applied(independent, new RemoveStateBinding(CONTROL));
        assertEquals(widget(independent).properties().get(CHANGED), widget(independentlyRemoved).properties().get(CHANGED));
        assertExact(renamed, removed.undo().session());
        assertExact(removed, reopen(removed.markSaved()));
    }

    @Test void manualBodyBeforeFirstSaveRestagesWithoutNewSemanticUndoAndFieldsRemainValidated() throws Exception {
        var initial = open("flutter.material.Switch", Map.of(), "", WidgetClassKind.STATEFUL);
        var created = applied(initial, new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        byte[] edited = source(created).replace("_controlled = value;", "_controlled = value;\n      print(value); // User body")
                .getBytes(StandardCharsets.UTF_8);
        var restaged = created.prepareSourceRestage(created.current().preparedPair().orElseThrow(), edited);
        var accepted = created.acceptSourceRestage(restaged);
        assertSame(created.current(), accepted.current());
        var saved = accepted.markSaved(restaged.preparedPair());
        assertArrayEquals(edited, saved.current().dartCandidateBytes());
        assertExact(initial, saved.undo().session());
        assertExact(saved, reopen(saved));
        for (String invalid : List.of("final bool _controlled = false;", "static bool _controlled = false;",
                "bool get _controlled => false;", "bool _elsewhere = false;", "late bool _controlled = false;")) {
            byte[] changed = source(created).replace("bool _controlled = false;", invalid).getBytes(StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class,
                    () -> created.prepareSourceRestage(created.current().preparedPair().orElseThrow(), changed), invalid);
            var opening = DesignerCommandSession.open(created.current().fdSnapshot(), changed, CATALOG);
            assertFalse(opening.ready(), invalid);
            assertThrows(IllegalArgumentException.class, () -> saved.reanchorSavedSource(
                    source(saved).replace("bool _controlled = false;", invalid).getBytes(StandardCharsets.UTF_8)), invalid);
        }
    }

    @Test void unsafeCreationPreservesOriginalPairAndRedo() throws Exception {
        for (String members : List.of("  bool _controlled = false;\n", "  void _changed(bool value) {}\n",
                "  bool test() => _controlled;\n", "  String test() => '$_controlled';\n")) {
            var initial = open("flutter.material.Switch", Map.of(), members, WidgetClassKind.STATEFUL);
            var result = initial.apply(new CreateStateBinding(CONTROL, "_controlled", "_changed"));
            assertFalse(result.changed(), members);
            assertSame(initial, result.session());
        }
        var stateless = open("flutter.material.Switch", Map.of(), "", WidgetClassKind.STATELESS);
        assertFalse(stateless.apply(new CreateStateBinding(CONTROL, "_controlled", "_changed")).changed());
        var initial = open("flutter.material.Switch", Map.of(), "", WidgetClassKind.STATEFUL);
        var independent = applied(initial, new CreateEventHandler(CONTROL, CHANGED, "_independent"));
        var conflict = independent.apply(new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        assertFalse(conflict.changed());
        assertTrue(conflict.diagnostics().toString().contains("Disconnect"));
        var created = applied(initial, new CreateStateBinding(CONTROL, "_controlled", "_changed"));
        var undo = created.undo().session();
        assertFalse(undo.apply(new CreateStateBinding(TEXT, "_controlled", "_changed")).changed());
        assertExact(created, undo.redo().session());
        assertThrows(IllegalArgumentException.class, () -> new CreateStateBinding(CONTROL, "publicField", "_changed"));
        assertThrows(IllegalArgumentException.class, () -> new CreateStateBinding(CONTROL, "_same", "_same"));
    }

    @Test void controllerLifecycleConsumerAndRenameRemainAtomicAcrossSaveAndHistory() throws Exception {
        String custom = "  @override\n  void initState() {\n    super.initState();\n    print('existing init');\n  }\n"
                + "  @override\n  void dispose() {\n    print('existing dispose');\n    super.dispose();\n  }\n";
        var initial = open("flutter.material.TextField", Map.of(), custom, WidgetClassKind.STATEFUL);
        var created = applied(initial, new CreateStateBinding(CONTROL, "_text", "_changed", StateBinding.Action.CHANGE,
                Optional.empty(), "Hello $text", Optional.empty()));
        assertTrue(source(created).contains("final TextEditingController _text"));
        assertTrue(source(created).contains("text: 'Hello \\$text'"));
        assertTrue(source(created).contains("print('existing init');"));
        assertTrue(source(created).contains("print('existing dispose');"));
        var textBinding = new StatePropertyBinding("_text", StateBinding.Type.TEXT_CONTROLLER, Optional.empty(),
                StatePropertyBinding.Transform.TEXT);
        var dependent = applied(created, new BindPropertyToState(TEXT, new PropertyName("data"), textBinding));
        assertEquals(textBinding, EventCommandSupport.widget(dependent.current().document().root(), TEXT)
                .propertyBindings().get(new PropertyName("data")));
        assertEquals(new PropertyValue.StringValue("Original"), EventCommandSupport.widget(dependent.current().document().root(), TEXT)
                .properties().get(new PropertyName("data")));
        assertTrue(source(dependent).contains("_text.text"));
        var renamed = applied(dependent, new RenameEventHandler(CONTROL, CHANGED, "_renamed"));
        assertTrue(source(renamed).contains("void _renamed(String value)"));
        assertTrue(source(renamed).contains("void _textStateListener()"));
        assertFalse(source(renamed).contains("void _renamedStateListener"));
        var removedAction = applied(renamed, new RemoveStateBinding(CONTROL));
        assertTrue(widget(removedAction).stateBinding().isEmpty());
        assertTrue(source(removedAction).contains("_text.text"));
        assertTrue(source(removedAction).contains("_text.dispose();"));
        var removedConsumer = applied(removedAction, new RemovePropertyStateBinding(TEXT, new PropertyName("data")));
        assertTrue(source(removedConsumer).contains("final TextEditingController _text"));
        var saved = removedConsumer.markSaved();
        assertExact(saved, reopen(saved));
        var undo = saved;
        for (int i = 0; i < 5; i++) undo = undo.undo().session();
        assertExact(initial, undo);
        for (int i = 0; i < 5; i++) undo = undo.redo().session();
        assertExact(saved, undo);
        assertFalse(new String(saved.current().fdBytes(), StandardCharsets.UTF_8).contains("Hello"));
    }

    @Test void reusedTypedFieldAndDependentPropertyBindingKeepSourceAndTreeMetadata() throws Exception {
        var initial = open("flutter.material.Switch", Map.of(), "  bool _shared = true;\n", WidgetClassKind.STATEFUL);
        var field = new StatePropertyBinding("_shared", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
        var created = applied(initial, new CreateStateBinding(CONTROL, "_shared", "_changed", StateBinding.Action.CHANGE,
                Optional.empty(), "", Optional.of(field)));
        assertTrue(source(created).contains("bool _shared = true;"));
        assertFalse(source(created).contains("bool _shared = false;"));
        var dependent = applied(created, new BindPropertyToState(TEXT, new PropertyName("softWrap"), field));
        var formatted = new StatePropertyBinding("_shared", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.TO_STRING);
        dependent = applied(dependent, new BindPropertyToState(TEXT, new PropertyName("data"), formatted));
        var wrapper = WidgetNodePrototypeFactory.create(CATALOG.find(new WidgetTypeId("flutter.widgets.Center")).orElseThrow(), StableId.random());
        var wrapped = applied(dependent, new WrapWidget(TEXT, wrapper, new SlotName("child"), 0));
        assertEquals(EventCommandSupport.widget(dependent.current().document().root(), TEXT).propertyBindings(),
                EventCommandSupport.widget(wrapped.current().document().root(), TEXT).propertyBindings());
        assertExact(wrapped, reopen(wrapped.markSaved()));
        assertExact(dependent, wrapped.undo().session());
        var mismatch = initial.apply(new CreateStateBinding(CONTROL, "_shared", "_changed", StateBinding.Action.CHANGE,
                Optional.empty(), "", Optional.of(new StatePropertyBinding("_shared", StateBinding.Type.DOUBLE,
                        Optional.empty(), StatePropertyBinding.Transform.DIRECT))));
        assertFalse(mismatch.changed());
        assertSame(initial, mismatch.session());
    }

    @Test void consumerBindingsRejectUnownedFieldsAndInvalidTransformsWithoutMutation() throws Exception {
        var initial = open("flutter.material.Switch", Map.of(), "  bool _flag = false;\n  final String _constant = '';\n", WidgetClassKind.STATEFUL);
        for (StatePropertyBinding invalid : List.of(
                new StatePropertyBinding("_missing", StateBinding.Type.STRING, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_constant", StateBinding.Type.STRING, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT))) {
            var rejected = initial.apply(new BindPropertyToState(TEXT, new PropertyName("data"), invalid));
            assertFalse(rejected.changed(), invalid.toString());
            assertSame(initial, rejected.session());
        }
        var binding = new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT);
        var accepted = applied(initial, new BindPropertyToState(TEXT, new PropertyName("softWrap"), binding));
        assertTrue(source(accepted).contains("!_flag"));
        var invalidSource = source(accepted).replace("bool _flag = false;", "final bool _flag = false;");
        assertFalse(DesignerCommandSession.open(accepted.current().fdSnapshot(), invalidSource.getBytes(StandardCharsets.UTF_8), CATALOG).ready());
        assertThrows(IllegalArgumentException.class, () -> accepted.prepareSourceRestage(
                accepted.current().preparedPair().orElseThrow(), invalidSource.getBytes(StandardCharsets.UTF_8)));
    }

    private static WidgetNode widget(DesignerCommandSession session) {
        return EventCommandSupport.widget(session.current().document().root(), CONTROL);
    }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var opened = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG);
        assertTrue(opened.ready(), opened.diagnostics().toString());
        return opened.session().orElseThrow();
    }
    private static DesignerCommandSession open(String type, Map<PropertyName, PropertyValue> overrides,
            String members, WidgetClassKind kind) throws Exception {
        var definition = CATALOG.find(new WidgetTypeId(type)).orElseThrow();
        var control = WidgetNodePrototypeFactory.create(definition, CONTROL, overrides);
        if (type.equals("flutter.widgets.RadioGroup")) {
            var slots = new LinkedHashMap<>(control.slots());
            slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(new WidgetNode(StableId.random(),
                    new WidgetTypeId("flutter.widgets.SizedBox"), Map.of(), Map.of())));
            control = new WidgetNode(control.id(), control.type(), control.properties(), slots);
        }
        var text = new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Original")), Map.of());
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(control, text))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var generation = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(generation.successful(), generation.diagnostics().toString());
        var generated = generation.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n"
                + "  @override\n  State<Sample> createState() => FormLogic();\n}\n"
                + "class FormLogic extends State<Sample> {\n";
        byte[] bytes = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n" + members + "}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document), bytes, CATALOG);
        assertTrue(opened.ready(), opened.diagnostics().toString());
        return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
