package dev.flutter.netbeans.designer.generation;

import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartImportPlan;
import dev.flutter.netbeans.designer.generation.DartManagedRegionId;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegion;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.transition.DartUserSourceProjection;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionStatus;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionDiagnosticCode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DartUserSourceProjectionTest {
    private static final String IMPORTS = "import 'package:flutter/material.dart';\n";
    private static final String BUILD = "  @override\n  Widget build(BuildContext context) {\n"
            + "    return const SizedBox();\n  }\n";
    private static final DartSourceDescriptor DESCRIPTOR = descriptor(IMPORTS, BUILD);

    @Test
    void persistentHeaderScaffoldIsAtomicReversibleAndPreservesUserEdits() {
        for (String nl : List.of("\n", "\r\n")) {
            byte[] original = bytes("\ufeff" + text(source("  // user 😀\n")).replace("\n", nl));
            var projection = DartUserSourceProjection.identity(original, DESCRIPTOR).insertPersistentHeaderDelegate();
            byte[] candidate = projection.applyTo(original, DESCRIPTOR);
            assertTrue(text(candidate).contains("class _FlutterDesignerPersistentHeaderDelegate extends SliverPersistentHeaderDelegate"));
            assertTrue(text(candidate).contains("double get minExtent => 56;"));
            assertTrue(text(candidate).startsWith("\ufeff"));
            assertArrayEquals(original, projection.inverse().applyTo(candidate, DESCRIPTOR));
            assertArrayEquals(candidate, DartUserSourceProjection.identity(candidate, DESCRIPTOR)
                    .insertPersistentHeaderDelegate().applyTo(candidate, DESCRIPTOR));
            byte[] edited = bytes(text(candidate).replace("minExtent => 56", "minExtent => 64"));
            assertArrayEquals(edited, DartUserSourceProjection.identity(edited, DESCRIPTOR)
                    .insertPersistentHeaderDelegate().applyTo(edited, DESCRIPTOR));
            assertThrows(IllegalArgumentException.class, () -> projection.inverse().applyTo(edited, DESCRIPTOR));
            byte[] disjoint = bytes(text(original).replace("// user 😀", "// retained edit"));
            byte[] rebased = projection.applyTo(disjoint, DESCRIPTOR);
            assertArrayEquals(disjoint, projection.inverse().applyTo(rebased, DESCRIPTOR));
        }
        for (String collision : List.of("typedef _FlutterDesignerPersistentHeaderDelegate = Object;",
                "final _FlutterDesignerPersistentHeaderDelegate = Object();",
                "class _FlutterDesignerPersistentHeaderDelegate {}\nclass _FlutterDesignerPersistentHeaderDelegate {}")) {
            byte[] original = bytes(text(source("")) + collision);
            assertThrows(IllegalArgumentException.class, () -> DartUserSourceProjection.identity(original, DESCRIPTOR).insertPersistentHeaderDelegate());
        }
        byte[] commented = bytes(text(source("")) + "// class _FlutterDesignerPersistentHeaderDelegate {}");
        assertTrue(text(DartUserSourceProjection.identity(commented, DESCRIPTOR).insertPersistentHeaderDelegate()
                .applyTo(commented, DESCRIPTOR)).contains("extends SliverPersistentHeaderDelegate"));
    }

    @Test
    void insertsOnlyMethodPreservesExactBaselineAndCanInvert() {
        byte[] original = source("  // user 😀\n  final String title = 'keep';\n");
        byte[] snapshot = original.clone();
        var identity = DartUserSourceProjection.identity(original, DESCRIPTOR);
        var projection = identity.insertHandler("HomePage", "_tap", "void _tap() {\n  // TODO\n}");
        byte[] candidate = projection.applyTo(original, DESCRIPTOR);
        assertTrue(text(candidate).contains("void _tap()"));
        assertTrue(text(candidate).contains("  // user 😀\n  final String title = 'keep';\n"));
        assertFalse(projection.isIdentity());
        assertTrue(projection.targetMatches(candidate, DESCRIPTOR));
        assertArrayEquals(snapshot, original);
        assertArrayEquals(original, projection.inverse().applyTo(candidate, DESCRIPTOR));
        assertTrue(projection.then(projection.inverse()).isIdentity());
    }

    @Test
    void renameAndInsertComposeAndReverseExactly() {
        byte[] original = source("  void _before() {}\n  void other() { _before(); }\n");
        var renamed = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .renameHandler("HomePage", "_before", "_after");
        byte[] renamedSource = renamed.applyTo(original, DESCRIPTOR);
        var added = DartUserSourceProjection.identity(renamedSource, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {}");
        var composed = renamed.then(added);
        byte[] candidate = composed.applyTo(original, DESCRIPTOR);
        assertTrue(text(candidate).contains("void _after()"));
        assertTrue(text(candidate).contains("other() { _after(); }"));
        assertTrue(text(candidate).contains("void _tap()"));
        assertArrayEquals(original, composed.inverse().applyTo(candidate, DESCRIPTOR));
    }

    @Test
    void acceptsReprojectionOfAlreadyAppliedContribution() {
        byte[] original = source("");
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {}");
        byte[] candidate = projection.applyTo(original, DESCRIPTOR);
        assertArrayEquals(candidate, projection.applyTo(candidate, DESCRIPTOR));
        assertTrue(projection.rebaseOnto(candidate, DESCRIPTOR).isIdentity());
    }

    @Test
    void disjointSourceEditsSurviveForwardAndReverseReprojection() {
        byte[] original = source("  final String title = 'original';\n");
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {}");
        byte[] external = bytes(text(original).replace("'original'", "'user-edited 😀'"));
        byte[] projected = projection.applyTo(external, DESCRIPTOR);
        assertTrue(text(projected).contains("'user-edited 😀'"));
        assertTrue(text(projected).contains("void _tap()"));
        assertArrayEquals(external, projection.inverse().applyTo(projected, DESCRIPTOR));
        var rebased = projection.rebaseOnto(external, DESCRIPTOR);
        assertArrayEquals(projected, rebased.applyTo(external, DESCRIPTOR));
        assertArrayEquals(external, rebased.inverse().applyTo(projected, DESCRIPTOR));
    }

    @Test
    void undoNeverDeletesAUserEditedNewMethod() {
        byte[] original = source("");
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {}");
        byte[] edited = bytes(text(projection.applyTo(original, DESCRIPTOR))
                .replace("void _tap() {}", "void _tap() { print('user work'); }"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> projection.inverse().applyTo(edited, DESCRIPTOR));
        assertTrue(error.getMessage().contains("overlaps"));
        assertTrue(text(edited).contains("user work"));
    }

    @Test
    void canReanchorOldRevisionAfterSavingNewRevision() {
        byte[] original = source("  final int untouched = 7;\n");
        var old = DartUserSourceProjection.identity(original, DESCRIPTOR);
        var saved = old.insertHandler("HomePage", "_tap", "void _tap() {}");
        byte[] disk = saved.applyTo(original, DESCRIPTOR);
        var historical = saved.inverse().then(old).rebaseOnto(disk, DESCRIPTOR);
        assertArrayEquals(original, historical.applyTo(disk, DESCRIPTOR));
        assertArrayEquals(disk, historical.inverse().applyTo(original, DESCRIPTOR));
    }

    @Test
    void preservesNewManagedPayloadsDuringUserProjection() {
        byte[] original = source("");
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {}");
        String newBuild = BUILD.replace("const SizedBox()", "const Text('new')");
        byte[] current = bytes(text(original).replace(BUILD, newBuild));
        DartSourceDescriptor next = descriptor(IMPORTS, newBuild);
        byte[] projected = projection.applyTo(current, next);
        assertTrue(text(projected).contains(newBuild));
        assertTrue(text(projected).contains("void _tap()"));
        assertTrue(DartUserSourceProjection.sameUserEnvelope(
                original, DESCRIPTOR, current, next));
    }

    @Test
    void rejectsWrongOwnerGuardChangesAndInvalidComposition() {
        byte[] original = source("");
        var identity = DartUserSourceProjection.identity(original, DESCRIPTOR);
        var added = identity.insertHandler("HomePage", "_tap", "void _tap() {}");
        assertThrows(IllegalArgumentException.class,
                () -> identity.insertHandler("Other", "_tap", "void _tap() {}"));
        assertThrows(IllegalArgumentException.class, () -> added.then(identity));
        assertThrows(IllegalArgumentException.class,
                () -> added.applyTo(bytes(text(original).replace("SizedBox", "Text")), DESCRIPTOR));
        assertThrows(IllegalArgumentException.class, () -> identity.insertHandler(
                "HomePage", "_tap", "void _tap() {}\nvoid injected() {}"));
    }

    @Test
    void importsArePartOfReversibleProofAndExistingUserImportsRemain() {
        byte[] original = bytes("import 'dart:async';\n" + text(source("")));
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR).insertHandler(
                "HomePage", "_tap", "void _tap(PointerDownEvent event) {}",
                List.of("package:flutter/gestures.dart", "dart:async"));
        byte[] candidate = projection.applyTo(original, DESCRIPTOR);
        assertTrue(text(candidate).startsWith("import 'package:flutter/gestures.dart';\n"));
        assertEquals(1, text(candidate).split("import 'dart:async';", -1).length - 1);
        assertArrayEquals(original, projection.inverse().applyTo(candidate, DESCRIPTOR));
        assertThrows(IllegalArgumentException.class,
                () -> projection.insertHandler("HomePage", "_other", "void _other() {}",
                        List.of("dart:async'; evil(); //")));
    }

    @Test
    void plannerAllowsProvedSourceOnlyChangeWithoutInventingManagedHashes() {
        byte[] original = source("");
        var scanner = new DartSourceIntegrityScanner();
        var generated = generation();
        var baseline = new DartThreeWayIntegrityGate().evaluate(
                scanner.scan(original, DESCRIPTOR), DESCRIPTOR, success(generated));
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {}");
        var result = new DartSourceTransitionPlanner().plan(baseline, original, DESCRIPTOR,
                success(generated),
                projection);
        assertEquals(DartSourceTransitionStatus.READY, result.status());
        var plan = result.plan().orElseThrow();
        assertEquals(DESCRIPTOR, plan.prospectiveDescriptor());
        assertEquals(DESCRIPTOR, plan.baselineDescriptor());
        assertArrayEquals(original, plan.liveSource().original().orElseThrow().copyBytes());
        assertArrayEquals(projection.applyTo(original, DESCRIPTOR), plan.candidateBytes());
        assertArrayEquals(plan.candidateBytes(), plan.userSourceBytes());
        byte[] leaked = plan.userSourceBytes();
        leaked[0] = '!';
        assertNotEquals(leaked[0], plan.userSourceBytes()[0]);
    }

    @Test
    void plannerKeepsExactLiveProofWhileReportingOverlappingExternalEdit() {
        byte[] original = source("  void _tap() {}\n");
        var projection = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .renameHandler("HomePage", "_tap", "_pressed");
        byte[] live = bytes(text(original).replace("_tap", "_other"));
        var baseline = new DartThreeWayIntegrityGate().evaluate(
                new DartSourceIntegrityScanner().scan(original, DESCRIPTOR), DESCRIPTOR, success(generation()));
        var result = new DartSourceTransitionPlanner().plan(baseline, live, DESCRIPTOR,
                success(generation()),
                projection);
        assertEquals(DartSourceTransitionStatus.CONFLICT, result.status());
        assertEquals(DartSourceTransitionDiagnosticCode.USER_SOURCE_PROJECTION_CONFLICT,
                result.diagnostics().getFirst().code());
        assertArrayEquals(live, result.liveSource().original().orElseThrow().copyBytes());
    }

    @Test
    void observedSourceBodyHelpersAndImportsComposeWithUnsavedCreationAndInvertExactly() {
        byte[] original = source("  final String note = 'keep';\n");
        var created = DartUserSourceProjection.identity(original, DESCRIPTOR)
                .insertHandler("HomePage", "_tap", "void _tap() {\n  // TODO\n}");
        byte[] stub = created.applyTo(original, DESCRIPTOR);
        byte[] edited = bytes("import 'dart:async';\n" + text(stub)
                .replace("// TODO", "print('Привіт 😀');")
                .replace("  final String note", "  void helper() { _tap(); }\n  final String note"));
        byte[] stubBefore = stub.clone();
        byte[] editedBefore = edited.clone();

        var observed = DartUserSourceProjection.observedEdit(stub, edited, DESCRIPTOR);
        var combined = created.then(observed);
        assertArrayEquals(edited, combined.applyTo(original, DESCRIPTOR));
        assertArrayEquals(stub, observed.inverse().applyTo(edited, DESCRIPTOR));
        assertArrayEquals(original, combined.inverse().applyTo(edited, DESCRIPTOR));
        assertTrue(combined.targetMatches(edited, DESCRIPTOR));
        assertArrayEquals(stubBefore, stub);
        assertArrayEquals(editedBefore, edited);

        edited[0] = '!';
        stub[0] = '!';
        assertArrayEquals(editedBefore, combined.applyTo(original, DESCRIPTOR),
                "observed evidence cannot retain caller-owned mutable arrays");
    }

    @Test
    void observedIdentityAndNewerGeneratedPayloadProjectionRemainExact() {
        byte[] original = source("  void _tap() {}\n");
        assertTrue(DartUserSourceProjection.observedEdit(original, original, DESCRIPTOR).isIdentity());
        byte[] edited = bytes(text(original).replace("void _tap() {}", "void _tap() { print('changed'); }"));
        var observed = DartUserSourceProjection.observedEdit(original, edited, DESCRIPTOR);
        String newBuild = BUILD.replace("SizedBox()", "Text('new')");
        var nextDescriptor = descriptor(IMPORTS, newBuild);
        byte[] newer = bytes(text(original).replace(BUILD, newBuild));
        byte[] projected = observed.applyTo(newer, nextDescriptor);
        assertTrue(text(projected).contains(newBuild));
        assertTrue(text(projected).contains("print('changed')"));
        assertArrayEquals(newer, observed.inverse().applyTo(projected, nextDescriptor));
    }

    @Test
    void observedSourceRejectsChangedManagedPayloadEvenIfNormalizedHashIsUnchanged() {
        byte[] original = source("  void _tap() {}\n");
        byte[] changedPayload = bytes(text(original).replace("return const SizedBox();", "return const Text('bad');"));
        assertThrows(IllegalArgumentException.class,
                () -> DartUserSourceProjection.observedEdit(original, changedPayload, DESCRIPTOR));
        assertEquals(DartManagedRegionHashing.normalizedSha256(BUILD),
                DartManagedRegionHashing.normalizedSha256(BUILD + "\n"));
        byte[] trailingWhitespace = bytes(text(original).replace(BUILD, BUILD + "\n"));
        assertThrows(IllegalArgumentException.class,
                () -> DartUserSourceProjection.observedEdit(original, trailingWhitespace, DESCRIPTOR));
    }

    @Test
    void observedSourceRejectsChangedMarkerIndentationOrMalformedMarkers() {
        byte[] original = source("");
        byte[] openingIndent = bytes(text(original).replace(
                "  // <netbeans-flutter-designer region=\"build\">",
                "    // <netbeans-flutter-designer region=\"build\">"));
        byte[] closingIndent = bytes(text(original).replace(
                "  // </netbeans-flutter-designer>", "    // </netbeans-flutter-designer>"));
        for (byte[] changed : List.of(openingIndent, closingIndent,
                bytes(text(original).replace("region=\"build\"", "region=\"other\"")))) {
            assertThrows(IllegalArgumentException.class,
                    () -> DartUserSourceProjection.observedEdit(original, changed, DESCRIPTOR));
        }
    }

    @Test
    void observedSourceRejectsWrongOwnerClassRenameAndWritableFormatViolations() {
        byte[] original = source("");
        var wrongOwner = new DartSourceDescriptor("home_page.dart", "Other", WidgetClassKind.STATELESS,
                DESCRIPTOR.generatorVersion(), DESCRIPTOR.managedRegions());
        assertThrows(IllegalArgumentException.class,
                () -> DartUserSourceProjection.observedEdit(original, original, wrongOwner));
        byte[] bom = new byte[original.length + 3];
        bom[0] = (byte) 0xEF;
        bom[1] = (byte) 0xBB;
        bom[2] = (byte) 0xBF;
        System.arraycopy(original, 0, bom, 3, original.length);
        for (byte[] invalid : List.of(bytes(text(original).replace("HomePage", "Other")),
                bytes(text(original).replace("extends StatelessWidget", "extends StatefulWidget")),
                bytes(text(original).replace("\n", "\r\n")), bom,
                new byte[] {(byte) 0xC3, (byte) 0x28})) {
            assertThrows(IllegalArgumentException.class,
                    () -> DartUserSourceProjection.observedEdit(original, invalid, DESCRIPTOR));
            assertThrows(IllegalArgumentException.class,
                    () -> DartUserSourceProjection.observedEdit(invalid, original, DESCRIPTOR));
        }
    }

    @Test
    void observedSourceRejectsOverLimitInputBeforeScanning() {
        byte[] oversized = new byte[new DartSourceIntegrityScanner().limits().maxSourceBytes() + 1];
        var failure = assertThrows(IllegalArgumentException.class,
                () -> DartUserSourceProjection.observedEdit(source(""), oversized, DESCRIPTOR));
        assertTrue(failure.getMessage().contains("size limit"));
    }

    @Test
    void statefulHandlerProjectionOwnsTheVerifiedStateClassAndRemainsExactlyReversible() {
        var stateful = new DartSourceDescriptor(DESCRIPTOR.dartFile(), DESCRIPTOR.className(),
                WidgetClassKind.STATEFUL, DESCRIPTOR.generatorVersion(), DESCRIPTOR.managedRegions());
        byte[] original = statefulSource("  final String label = 'keep';\n");
        var identity = DartUserSourceProjection.identity(original, stateful);
        assertThrows(IllegalArgumentException.class,
                () -> identity.insertHandler("HomePage", "_tap", "void _tap() {}"));
        assertThrows(IllegalArgumentException.class,
                () -> identity.insertHandler("_HomePageState", "_tap", "void _tap() {}"));
        var inserted = identity.insertHandler("PageLogic", "_tap", "void _tap() {}");
        byte[] candidate = inserted.applyTo(original, stateful);
        assertTrue(text(candidate).indexOf("void _tap") > text(candidate).indexOf("class PageLogic"));
        assertTrue(text(candidate).contains("final String label = 'keep'"));
        var renamed = inserted.renameHandler("PageLogic", "_tap", "_pressed");
        byte[] renamedCandidate = renamed.applyTo(original, stateful);
        assertTrue(text(renamedCandidate).contains("void _pressed()"));
        assertArrayEquals(original, renamed.inverse().applyTo(renamedCandidate, stateful));
        byte[] body = bytes(text(candidate).replace("void _tap() {}", "void _tap() { setState(() {}); }"));
        var observed = DartUserSourceProjection.observedEdit(candidate, body, stateful);
        assertArrayEquals(body, inserted.then(observed).applyTo(original, stateful));
    }

    @Test
    void statefulProjectionRejectsObservedOwnerReplacementEvenWhenBothSourcesIndividuallyVerify() {
        var stateful = new DartSourceDescriptor(DESCRIPTOR.dartFile(), DESCRIPTOR.className(),
                WidgetClassKind.STATEFUL, DESCRIPTOR.generatorVersion(), DESCRIPTOR.managedRegions());
        byte[] original = statefulSource("");
        byte[] renamedOwner = bytes(text(original).replace("PageLogic", "OtherLogic"));
        assertTrue(new DartSourceIntegrityScanner().scan(renamedOwner, stateful).onDiskDeclaredMatch());
        var identity = DartUserSourceProjection.identity(original, stateful);
        assertThrows(IllegalArgumentException.class,
                () -> DartUserSourceProjection.observedEdit(original, renamedOwner, stateful));
        assertThrows(IllegalArgumentException.class, () -> identity.rebaseOnto(renamedOwner, stateful));
        assertThrows(IllegalArgumentException.class, () -> identity.applyTo(renamedOwner, stateful));
        assertThrows(IllegalArgumentException.class, () -> identity.applyTo(original, DESCRIPTOR));
    }

    @Test
    void authoredFieldRenameReplaysAcrossHistoricalBodyVariantsAndPreservesManagedBytes() {
        var stateful = new DartSourceDescriptor(DESCRIPTOR.dartFile(), DESCRIPTOR.className(),
                WidgetClassKind.STATEFUL, DESCRIPTOR.generatorVersion(), DESCRIPTOR.managedRegions());
        var field = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_flag",
                dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL, Optional.empty(),
                dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT);
        byte[] stub = statefulSource("  bool _flag = false;\n"
                + "  void _changed(bool value) { _flag = value; }\n"
                + "  bool _read() => _flag;\n");
        byte[] edited = bytes(text(stub).replace("_flag = value;", "_flag = value;\n"
                + "    // Keep _flag in comments and 'strings'.\n    print(this._flag);"));
        var observed = DartUserSourceProjection.observedEdit(stub, edited, stateful);
        var renamed = DartUserSourceProjection.identity(edited, stateful).renameStateField("PageLogic", field, "_enabled");
        byte[] renamedEdited = renamed.applyTo(edited, stateful);
        byte[] renamedStub = renamed.applyTo(stub, stateful);
        assertEquals(text(stub).replace("_flag", "_enabled"), text(renamedStub));
        assertArrayEquals(stub, renamed.inverse().applyTo(renamedStub, stateful));
        assertTrue(text(renamedEdited).contains("// Keep _flag in comments and 'strings'."));
        assertTrue(text(renamedEdited).contains("print(this._enabled)"));

        var composed = observed.then(renamed);
        assertArrayEquals(renamedEdited, composed.applyTo(stub, stateful));
        assertArrayEquals(renamedEdited, composed.applyTo(edited, stateful));
        assertArrayEquals(stub, composed.inverse().applyTo(renamedEdited, stateful));
        assertTrue(composed.retainedBytes() > renamed.retainedBytes(), "Ordered endpoint arrays count against history limits.");
        var rebased = renamed.rebaseOnto(stub, stateful);
        assertArrayEquals(renamedStub, rebased.applyTo(stub, stateful));
        assertArrayEquals(stub, rebased.inverse().applyTo(renamedStub, stateful));

        String generatedBuild = BUILD.replace("const SizedBox()", "Text(_enabled.toString())");
        var durableDescriptor = new DartSourceDescriptor(stateful.dartFile(), stateful.className(), WidgetClassKind.STATEFUL,
                stateful.generatorVersion(), descriptor(IMPORTS, generatedBuild).managedRegions());
        byte[] historicalTemplate = bytes(text(stub).replace(BUILD, generatedBuild));
        byte[] projected = renamed.applyTo(historicalTemplate, durableDescriptor);
        assertEquals(text(renamedStub).replace(BUILD, generatedBuild), text(projected));
        assertArrayEquals(historicalTemplate, renamed.inverse().applyTo(projected, durableDescriptor));
        assertArrayEquals(projected, renamed.applyTo(projected, durableDescriptor), "An already-applied typed rename with a historical body remains idempotent.");
    }

    @Test
    void fieldRenameProvenanceNeverComesFromObservedTextAndReplayRejectsChangedTypesOrNames() {
        var stateful = new DartSourceDescriptor(DESCRIPTOR.dartFile(), DESCRIPTOR.className(),
                WidgetClassKind.STATEFUL, DESCRIPTOR.generatorVersion(), DESCRIPTOR.managedRegions());
        var field = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_flag",
                dev.flutter.netbeans.designer.model.StateBinding.Type.BOOL, Optional.empty(),
                dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT);
        byte[] original = statefulSource("  bool _flag = false;\n"
                + "  void _changed(bool value) { _flag = value; print(this._flag); }\n"
                + "  bool _read() => _flag;\n");
        var rename = DartUserSourceProjection.identity(original, stateful).renameStateField("PageLogic", field, "_enabled");
        byte[] renamed = rename.applyTo(original, stateful);
        byte[] bodyVariant = bytes(text(original).replace(" print(this._flag);", ""));
        var arbitraryObservedRename = DartUserSourceProjection.observedEdit(original, renamed, stateful);
        assertThrows(IllegalArgumentException.class, () -> arbitraryObservedRename.applyTo(bodyVariant, stateful),
                "Observed bytes are not authority to replay a semantic rename.");
        for (String change : List.of(
                text(bodyVariant).replace("bool _flag = false;", "String _flag = 'changed';"),
                text(bodyVariant).replace("bool _flag = false;", "bool _flag = false;\n  bool _enabled = true;"),
                text(bodyVariant).replace("_flag = value;", "_flag = value; print(other._flag);"),
                text(bodyVariant).replace("_flag = value;", "_flag = value; print('$_flag');"))) {
            byte[] unsafe = bytes(change);
            byte[] untouched = unsafe.clone();
            assertThrows(IllegalArgumentException.class, () -> rename.applyTo(unsafe, stateful));
            assertArrayEquals(untouched, unsafe);
        }
    }

    @Test
    void orderedControllerRenameRetainsLifecycleProofAcrossUserBodyVariants() {
        var stateful = new DartSourceDescriptor(DESCRIPTOR.dartFile(), DESCRIPTOR.className(),
                WidgetClassKind.STATEFUL, DESCRIPTOR.generatorVersion(), DESCRIPTOR.managedRegions());
        byte[] original = statefulSource("");
        var definition = dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog.getDefault()
                .find(new dev.flutter.netbeans.designer.model.WidgetTypeId("flutter.material.TextField")).orElseThrow();
        var prototype = dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(definition,
                dev.flutter.netbeans.designer.model.StableId.random());
        var binding = dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.createBinding(prototype, "_text", "_changed");
        var widget = new dev.flutter.netbeans.designer.model.WidgetNode(prototype.id(), prototype.type(), prototype.properties(),
                prototype.slots(), prototype.extensions(), Optional.of(binding));
        var created = DartUserSourceProjection.identity(original, stateful).insertStateBinding("PageLogic", widget, "Initial", false);
        byte[] stub = created.applyTo(original, stateful);
        byte[] edited = bytes(text(stub).replace("// Handle user text changes", "print(this._text.text);\n    // Handle user text changes"));
        assertFalse(java.util.Arrays.equals(stub, edited));
        var observed = DartUserSourceProjection.observedEdit(stub, edited, stateful);
        var field = new dev.flutter.netbeans.designer.model.StatePropertyBinding("_text",
                dev.flutter.netbeans.designer.model.StateBinding.Type.TEXT_CONTROLLER, Optional.empty(),
                dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT);
        var renamed = DartUserSourceProjection.identity(edited, stateful).renameStateField("PageLogic", field, "_query");
        byte[] renamedStub = renamed.applyTo(stub, stateful);
        assertTrue(text(renamedStub).contains("_query.addListener(_queryStateListener)"));
        assertTrue(text(renamedStub).contains("_query.dispose()"));
        assertTrue(text(renamedStub).contains("void _changed(String value)"));
        assertArrayEquals(stub, renamed.inverse().applyTo(renamedStub, stateful));
        var composed = created.then(observed).then(renamed);
        byte[] finalSource = composed.applyTo(original, stateful);
        assertArrayEquals(original, composed.inverse().applyTo(finalSource, stateful));
        byte[] brokenLifecycle = bytes(text(stub).replace("_text.dispose();", "// user removed disposal"));
        assertThrows(IllegalArgumentException.class, () -> renamed.applyTo(brokenLifecycle, stateful));
        byte[] userEditedNewMethod = bytes(text(stub).replace("// Handle user text changes", "print(value); // User body"));
        assertThrows(IllegalArgumentException.class, () -> created.inverse().applyTo(userEditedNewMethod, stateful),
                "Creation/deletion never receives rename replay authority.");
    }

    private static byte[] statefulSource(String members) {
        return bytes("// <netbeans-flutter-designer region=\"imports\">\n" + IMPORTS
                + "// </netbeans-flutter-designer>\n\nclass HomePage extends StatefulWidget {\n"
                + "  const HomePage({super.key});\n"
                + "  @override\n  State<HomePage> createState() => PageLogic();\n}\n"
                + "class PageLogic extends State<HomePage> {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + BUILD
                + "  // </netbeans-flutter-designer>\n" + members + "}\n");
    }

    private static GeneratedDartRegions generation() {
        return new GeneratedDartRegions(
                GeneratedDartRegion.create(DartManagedRegionId.IMPORTS, IMPORTS),
                GeneratedDartRegion.create(DartManagedRegionId.BUILD, BUILD),
                new DartImportPlan(List.of()), "test");
    }

    private static DartGenerationResult success(GeneratedDartRegions generated) {
        return new DartGenerationResult(new ValidationResult(List.of()), Optional.of(generated), List.of());
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("home_page.dart", "HomePage", WidgetClassKind.STATELESS,
                Optional.of("test"), new ManagedRegions(
                        new ManagedRegion(DartManagedRegionHashing.normalizedSha256(imports)),
                        new ManagedRegion(DartManagedRegionHashing.normalizedSha256(build))));
    }

    private static byte[] source(String members) {
        return bytes("// <netbeans-flutter-designer region=\"imports\">\n" + IMPORTS
                + "// </netbeans-flutter-designer>\n\nclass HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + BUILD
                + "  // </netbeans-flutter-designer>\n" + members + "}\n");
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
