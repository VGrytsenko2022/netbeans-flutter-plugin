package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventHandlerCommandSessionTest {
    @Test void fadeInImageBothBuilderLifecyclesRemainPropertiesAndPreserveUserCode() throws Exception {
        var type=new WidgetTypeId("flutter.widgets.FadeInImage");
        for(var kind:WidgetClassKind.values()){
            var initial=open("",type,kind);
            var placeholder=new PropertyName("placeholderErrorBuilder");var target=new PropertyName("imageErrorBuilder");
            var first=applied(initial,new CreateEventHandler(FIRST,placeholder,"_placeholderFailed"));
            var both=applied(first,new CreateEventHandler(FIRST,target,"_targetFailed"));
            assertTrue(source(both).contains("Widget _placeholderFailed(BuildContext context, Object error, StackTrace? stackTrace)"));
            assertTrue(source(both).contains("return const SizedBox.shrink();"));
            var renamed=applied(both,new RenameEventHandler(FIRST,placeholder,"_newPlaceholder"));
            assertTrue(source(renamed).contains("placeholderErrorBuilder: _newPlaceholder"));
            assertTrue(source(renamed).contains("imageErrorBuilder: _targetFailed"));
            var disconnected=applied(renamed,new ResetProperty(FIRST,placeholder));
            assertTrue(source(disconnected).contains("Widget _newPlaceholder("));assertFalse(source(disconnected).contains("placeholderErrorBuilder: _newPlaceholder"));
            assertExact(renamed,disconnected.undo().session());assertExact(disconnected,reopen(disconnected.markSaved()));
            for(var descriptor:WidgetEventCatalog.eventsFor(CATALOG.find(type).orElseThrow())){
                assertEquals(dev.flutter.netbeans.designer.events.WidgetEventDescriptor.Kind.BUILDER,descriptor.kind());assertTrue(descriptor.supportsHandlerActions());
            }
        }
    }

    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId ROOT = StableId.parse("709a9423-a542-4624-adbb-54f8a5a6a56d");
    private static final StableId FIRST = StableId.parse("14274561-2683-46a6-83a3-c7f1f4ba17e7");
    private static final StableId SECOND = StableId.parse("ae0db035-b77c-4647-bd0a-455d7f24dc21");
    private static final StableId TEXT = StableId.parse("28c7b64a-ef7c-4081-97a5-7528128379ba");
    private static final PropertyName CHANGED = new PropertyName("onChanged");

    @Test void createBindIsOneRevisionAndSurvivesOrdinaryEditsUndoRedoSaveAndReopen() throws Exception {
        var initial = open("");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        assertEquals(initial.cursor() + 1, created.cursor());
        assertTrue(source(created).contains("void _valueChanged(String value)"));
        assertTrue(source(created).contains("onChanged: _valueChanged"));
        assertExact(initial, created.undo().session());
        assertExact(created, created.undo().session().redo().session());
        var edited = applied(created, new SetProperty(TEXT, new PropertyName("data"), new PropertyValue.StringValue("Edited")));
        assertTrue(source(edited).contains("void _valueChanged(String value)"));
        assertTrue(source(edited).contains("Edited"));
        assertExact(created, edited.undo().session());
        var saved = edited.markSaved();
        assertFalse(saved.dirty());
        assertExact(edited, saved);
        assertExact(created, saved.undo().session());
        assertExact(initial, saved.undo().session().undo().session());
        assertExact(saved, reopen(saved));
    }

    @Test void disconnectPreservesUserMethodEvenWhenModelReturnsToOriginalBytes() throws Exception {
        var initial = open("");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        var disconnected = applied(created, new ResetProperty(FIRST, CHANGED));
        assertArrayEquals(initial.current().fdBytes(), disconnected.current().fdBytes());
        assertEquals(DesignerRevisionPersistenceKind.PAIRED, disconnected.current().persistenceKind());
        assertTrue(source(disconnected).contains("void _valueChanged(String value)"));
        assertFalse(source(disconnected).contains("onChanged: _valueChanged"));
        var physical = disconnected.rederiveRetainedRevisionByProjectingAnchor(disconnected.current().revisionId(),
                disconnected.current().dartCandidateBytes());
        assertEquals(DesignerRevisionPersistenceKind.PAIRED, physical.persistenceKind());
        assertArrayEquals(disconnected.current().dartCandidateBytes(), physical.dartCandidateBytes());
        var saved = disconnected.markSaved();
        assertExact(disconnected, saved);
        assertExact(created, saved.undo().session());
        assertExact(initial, saved.undo().session().undo().session());
        assertExact(saved, reopen(saved));
    }

    @Test void sharedRenameChangesAllLocalBindingsAndNeverUserStrings() throws Exception {
        var initial = open("  void _existing(String value) { print('_existing stays in string'); }\n");
        var bound = applied(initial, new SetProperty(FIRST, CHANGED, binding("_existing")));
        bound = applied(bound, new SetProperty(SECOND, CHANGED, binding("_existing")));
        var renamed = applied(bound, new RenameEventHandler(FIRST, CHANGED, "renamed"));
        assertEquals(binding("renamed"), widget(renamed, FIRST).properties().get(CHANGED));
        assertEquals(binding("renamed"), widget(renamed, SECOND).properties().get(CHANGED));
        assertTrue(source(renamed).contains("void renamed(String value)"));
        assertTrue(source(renamed).contains("'_existing stays in string'"));
        assertFalse(source(renamed).contains("onChanged: _existing"));
        assertExact(bound, renamed.undo().session());
        var saved = renamed.markSaved();
        assertExact(bound, saved.undo().session());
        assertExact(saved, reopen(saved));
    }

    @Test void typedImportedSameNamedCallbackIsNotRenamed() throws Exception {
        var type = new WidgetTypeId("flutter.material.Switch");
        var event = new PropertyName("onChanged");
        var definition = CATALOG.find(type).orElseThrow();
        var descriptor = WidgetEventCatalog.find(type, event).orElseThrow();
        var initial = open("  void renamed(bool value) {}\n", type);
        var local = applied(initial, new SetProperty(FIRST, event,
                descriptor.bindingValue("renamed", definition.property(event).orElseThrow())));
        var imported = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                "renamed", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var external = applied(local, new SetProperty(SECOND, event, imported));
        var renamed = applied(external, new RenameEventHandler(FIRST, event, "_finalName"));
        assertEquals(imported, widget(renamed, SECOND).properties().get(event));
        assertEquals(descriptor.bindingValue("_finalName", definition.property(event).orElseThrow()),
                widget(renamed, FIRST).properties().get(event));
    }

    @Test void invalidEventsDuplicateMethodsAndUnsafeNamesNeverMutateOrDiscardRedo() throws Exception {
        var initial = open("  void _existing(String value) {}\n");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_newHandler"));
        var withRedo = created.undo().session();
        for (DesignerCommand invalid : List.of(
                new CreateEventHandler(FIRST, CHANGED, "_existing"),
                new CreateEventHandler(FIRST, new PropertyName("buildCounter"), "_builder"),
                new CreateEventHandler(TEXT, new PropertyName("onTap"), "_tap"),
                new RenameEventHandler(FIRST, CHANGED, "_renamed"))) {
            var result = withRedo.apply(invalid);
            assertFalse(result.changed(), result.diagnostics().toString());
            assertSame(withRedo, result.session());
            assertTrue(result.session().canRedo());
            assertExact(created, result.session().redo().session());
        }
        assertThrows(IllegalArgumentException.class, () -> new CreateEventHandler(FIRST, CHANGED, "bad();"));
    }

    @Test void pointerEventStubGetsItsReviewedSignatureImport() throws Exception {
        var created = applied(open(""), new CreateEventHandler(FIRST, new PropertyName("onTapOutside"), "_outside"));
        assertTrue(source(created).contains("PointerDownEvent event"));
        assertTrue(source(created).contains("package:flutter/gestures.dart"));
        assertExact(created, reopen(created));
    }

    @Test void sourceSaveKeepsUserBodyAndExactEarlierHistoryWhileLaterDesignerEditsPreserveIt() throws Exception {
        var initial = open("");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_valueChanged")).markSaved();
        String withBody = source(created).replace("// TODO: Handle onChanged.", "print(value); // User body");
        var sourceSaved = created.reanchorSavedSource(withBody.getBytes(StandardCharsets.UTF_8));
        assertTrue(source(sourceSaved).contains("print(value); // User body"));
        assertExact(initial, sourceSaved.undo().session());
        var edited = applied(sourceSaved, new SetProperty(TEXT, new PropertyName("data"), new PropertyValue.StringValue("Later")));
        assertTrue(source(edited).contains("print(value); // User body"));
        var savedAgain = edited.markSaved();
        assertExact(edited, savedAgain);
        assertExact(initial, savedAgain.undo().session().undo().session());
        assertExact(edited, reopen(savedAgain));
    }

    @Test void physicalEnvelopeProjectionRetainsMethodsAndDisjointUserChanges() throws Exception {
        var initial = open("");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        byte[] physical = ("// User header\n" + source(initial)).getBytes(StandardCharsets.UTF_8);
        var projected = created.rederiveRetainedRevision(created.current().revisionId(), physical);
        assertTrue(new String(projected.dartCandidateBytes(), StandardCharsets.UTF_8).startsWith("// User header\n"));
        assertTrue(new String(projected.dartCandidateBytes(), StandardCharsets.UTF_8).contains("void _valueChanged"));
        var editedResult = created.applyFromPhysicalEndpoint(new SetProperty(TEXT, new PropertyName("data"),
                new PropertyValue.StringValue("Physical")), projected.preparedPair().orElseThrow());
        assertEquals(DesignerCommandStatus.APPLIED, editedResult.status(), editedResult.diagnostics().toString());
        var edited = editedResult.session();
        assertTrue(source(edited).startsWith("// User header\n"));
        assertTrue(source(edited).contains("void _valueChanged"));
        var saved = edited.markSaved();
        assertTrue(source(saved.undo().session()).startsWith("// User header\n"));
        assertFalse(source(saved.undo().session().undo().session()).contains("void _valueChanged"));
    }

    @Test void observedBodyRestageRequiresExactAcceptanceAndSavesWithoutANewSemanticRevision() throws Exception {
        var initial = open("");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        byte[] edited = source(created).replace("// TODO: Handle onChanged.", "print(value); // user body")
                .getBytes(StandardCharsets.UTF_8);
        var restage = created.prepareSourceRestage(created.current().preparedPair().orElseThrow(), edited);
        assertThrows(IllegalArgumentException.class, () -> created.markSaved(restage.preparedPair()));
        var accepted = created.acceptSourceRestage(restage);
        assertSame(created.current(), accepted.current());
        assertEquals(created.cursor(), accepted.cursor());
        assertEquals(created.revisionCount(), accepted.revisionCount());
        assertThrows(IllegalArgumentException.class, () -> initial.acceptSourceRestage(restage));
        var saved = accepted.markSaved(restage.preparedPair());
        assertArrayEquals(edited, saved.current().dartCandidateBytes());
        assertFalse(saved.dirty());
        assertExact(initial, saved.undo().session());
        assertExact(saved, reopen(saved));
        var property = accepted.applyFromPhysicalEndpoint(new SetProperty(TEXT, new PropertyName("data"),
                new PropertyValue.StringValue("After body")), restage.preparedPair());
        assertEquals(DesignerCommandStatus.APPLIED, property.status(), property.diagnostics().toString());
        assertTrue(source(property.session()).contains("print(value); // user body"));
    }

    @Test void repeatedObservedRestagingPinsTheAcceptedPredecessorAndRetainsPriorPhysicalCapabilities() throws Exception {
        var created = applied(open(""), new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        var first = created.prepareSourceRestage(created.current().preparedPair().orElseThrow(),
                source(created).replace("// TODO: Handle onChanged.", "print(value);").getBytes(StandardCharsets.UTF_8));
        var accepted = created.acceptSourceRestage(first);
        byte[] secondBytes = new String(first.physicalRevision().dartCandidateBytes(), StandardCharsets.UTF_8)
                .replace("print(value);", "print(value.toUpperCase());").getBytes(StandardCharsets.UTF_8);
        var second = accepted.prepareSourceRestage(first.preparedPair(), secondBytes);
        var acceptedTwice = accepted.acceptSourceRestage(second);
        assertSame(created.current(), acceptedTwice.current());
        assertArrayEquals(secondBytes, acceptedTwice.markSaved(second.preparedPair()).current().dartCandidateBytes());
        assertArrayEquals(first.physicalRevision().dartCandidateBytes(),
                acceptedTwice.markSaved(first.preparedPair()).current().dartCandidateBytes());
        assertThrows(IllegalArgumentException.class, () -> created.prepareSourceRestage(first.preparedPair(), secondBytes));
        assertThrows(IllegalArgumentException.class, () -> acceptedTwice.prepareSourceRestage(second.preparedPair(),
                new String(secondBytes, StandardCharsets.UTF_8).replace("'Original'", "'Guarded change'")
                        .getBytes(StandardCharsets.UTF_8)));
    }

    @Test void acceptedSourceVariantsCountTheirPhysicalProofAgainstTheExistingHistoryBounds() throws Exception {
        var initial = open("");
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        byte[] body = source(created).replace("// TODO: Handle onChanged.", "print(value);")
                .getBytes(StandardCharsets.UTF_8);
        var prospective = created.prepareSourceRestage(created.current().preparedPair().orElseThrow(), body);
        long exactBytes = initial.current().retainedPairBytes() + created.current().retainedPairBytes()
                + prospective.physicalRevision().retainedPairBytes();
        var defaults = DesignerCommandLimits.defaults();
        var tooSmall = new DesignerCommandLimits(defaults.fdCodecLimits(), defaults.generationLimits(),
                defaults.sourceLimits(), defaults.validationLimits(), 1, exactBytes - 1);
        var limited = DesignerCommandSession.open(initial.current().fdSnapshot(), initial.current().dartCandidateBytes(),
                CATALOG, tooSmall).session().orElseThrow();
        var limitedCreated = applied(limited, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        var failure = assertThrows(IllegalArgumentException.class,
                () -> limitedCreated.prepareSourceRestage(limitedCreated.current().preparedPair().orElseThrow(), body));
        assertTrue(failure.getMessage().contains("byte limit"));
        assertExact(created, limitedCreated);

        var exact = new DesignerCommandLimits(defaults.fdCodecLimits(), defaults.generationLimits(),
                defaults.sourceLimits(), defaults.validationLimits(), 1, exactBytes);
        var bounded = DesignerCommandSession.open(initial.current().fdSnapshot(), initial.current().dartCandidateBytes(),
                CATALOG, exact).session().orElseThrow();
        var boundedCreated = applied(bounded, new CreateEventHandler(FIRST, CHANGED, "_valueChanged"));
        var first = boundedCreated.prepareSourceRestage(boundedCreated.current().preparedPair().orElseThrow(), body);
        var accepted = boundedCreated.acceptSourceRestage(first);
        byte[] again = new String(body, StandardCharsets.UTF_8).replace("print(value);", "print(value.length);")
                .getBytes(StandardCharsets.UTF_8);
        var countFailure = assertThrows(IllegalArgumentException.class,
                () -> accepted.prepareSourceRestage(first.preparedPair(), again));
        assertTrue(countFailure.getMessage().contains("entry limit"));
        assertArrayEquals(body, accepted.markSaved(first.preparedPair()).current().dartCandidateBytes());
    }

    @Test void statefulCreateRenameDisconnectAndHistoryTargetTheVerifiedStateOwner() throws Exception {
        var initial = open("  int counter = 0;\n", new WidgetTypeId("flutter.material.TextField"), WidgetClassKind.STATEFUL);
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_changed"));
        assertEquals("FormLogic", created.current().sourceIntegrity().verifiedMemberClassName().orElseThrow());
        assertTrue(source(created).indexOf("void _changed") > source(created).indexOf("class FormLogic"));
        assertTrue(source(created).contains("int counter = 0;"));
        var renamed = applied(created, new RenameEventHandler(FIRST, CHANGED, "_renamed"));
        assertTrue(source(renamed).contains("void _renamed(String value)"));
        assertEquals(binding("_renamed"), widget(renamed, FIRST).properties().get(CHANGED));
        var disconnected = applied(renamed, new ResetProperty(FIRST, CHANGED));
        assertTrue(source(disconnected).contains("void _renamed(String value)"));
        var edited = applied(disconnected, new SetProperty(TEXT, new PropertyName("data"), new PropertyValue.StringValue("Stateful")));
        var saved = edited.markSaved();
        assertEquals(WidgetClassKind.STATEFUL, saved.current().document().source().widgetKind());
        assertExact(edited, saved);
        assertExact(saved, reopen(saved));
        assertExact(renamed, saved.undo().session().undo().session());
        assertExact(initial, saved.undo().session().undo().session().undo().session().undo().session());
    }

    @Test void statefulCreatedBodyCanBeRestagedBeforeItsFirstSave() throws Exception {
        var initial = open("  int counter = 0;\n", new WidgetTypeId("flutter.material.TextField"), WidgetClassKind.STATEFUL);
        var created = applied(initial, new CreateEventHandler(FIRST, CHANGED, "_changed"));
        byte[] body = source(created).replace("// TODO: Handle onChanged.", "setState(() { counter++; });")
                .getBytes(StandardCharsets.UTF_8);
        var restage = created.prepareSourceRestage(created.current().preparedPair().orElseThrow(), body);
        var saved = created.acceptSourceRestage(restage).markSaved(restage.preparedPair());
        assertArrayEquals(body, saved.current().dartCandidateBytes());
        assertEquals("FormLogic", saved.current().sourceIntegrity().verifiedMemberClassName().orElseThrow());
        assertExact(initial, saved.undo().session());
        assertExact(saved, reopen(saved));
    }

    private static PropertyValue binding(String name) {
        var definition = CATALOG.find(new WidgetTypeId("flutter.material.TextField")).orElseThrow();
        return WidgetEventCatalog.find(definition.typeId(), CHANGED).orElseThrow()
                .bindingValue(name, definition.property(CHANGED).orElseThrow());
    }
    private static WidgetNode widget(DesignerCommandSession session, StableId id) {
        return EventCommandSupport.widget(session.current().document().root(), id);
    }
    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes());
        assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DesignerCommandSession open(String members) throws Exception {
        return open(members, new WidgetTypeId("flutter.material.TextField"));
    }
    private static DesignerCommandSession open(String members, WidgetTypeId type) throws Exception {
        return open(members, type, WidgetClassKind.STATELESS);
    }
    private static DesignerCommandSession open(String members, WidgetTypeId type, WidgetClassKind kind) throws Exception {
        var definition = CATALOG.find(type).orElseThrow();
        var root = new WidgetNode(ROOT, new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(
                        WidgetNodePrototypeFactory.create(definition, FIRST),
                        WidgetNodePrototypeFactory.create(definition, SECOND),
                        new WidgetNode(TEXT, new WidgetTypeId("flutter.widgets.Text"),
                                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Original")), Map.of())))));
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var generated = new DartRegionGenerator().generate(provisional, CATALOG).generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n"
                + "  @override\n  State<Sample> createState() => FormLogic();\n}\n"
                + "class FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n" + owner
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n" + members + "}\n").getBytes(StandardCharsets.UTF_8);
        var result = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build) {
        return descriptor(imports, build, WidgetClassKind.STATELESS);
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind,
                Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
