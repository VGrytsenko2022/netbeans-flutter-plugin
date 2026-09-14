package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.*;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Generated modern RadioGroup and legacy RadioListTile State with actual native interaction. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class RadioListTileStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;

    @Test
    void legacyAndModernGenericStateEventsRenameSaveReopenAndNativeSelectionRemainExact() throws Exception {
        initialize();
        for (boolean modern : List.of(false, true)) for (String variant : List.of("standard", "adaptive")) {
            var first = tile(variant, "First", modern, false);
            var second = tile(variant, "Second", modern, false);
            var disabled = tile(variant, "Disabled", modern, true);
            var mirror = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Visibility"), Map.of(),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("selected mirror"))));
            var children = new ArrayList<>(List.of(first, second, disabled, mirror));
            if (modern) children.add(new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Radio"),
                    Map.of(p("valueType"), s("String"), p("variant"), s(variant), p("value"), s("Third")), Map.of()));
            var column = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
            var root = modern ? new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.RadioGroup"),
                    Map.of(p("valueType"), s("String"), p("onChanged"), s("noop")),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(column))) : column;
            var owner = modern ? root : first;
            String file = (modern ? "modern_" : "legacy_") + variant + ".dart";
            var initial = open(root, file, "  int focusCalls = 0;\n  int legacyCalls = 0; // User-owned Привіт.\n");
            var session = apply(initial, new CreateStateBinding(owner.id(), "_selection", "_selectionChanged"));
            if (!modern) session = apply(session, new CreateStateBinding(second.id(), "_selection", "_secondChanged",
                    StateBinding.Action.CHANGE, Optional.empty(), "", Optional.of(sharedSelection())));
            session = apply(session, new BindPropertyToState(first.id(), p("selected"), firstSelected()));
            session = apply(session, new BindPropertyToState(mirror.id(), p("visible"), firstSelected()));
            session = apply(session, new CreateEventHandler(first.id(), p("onFocusChange"), "_focusChanged"));
            if (modern) session = apply(session, new CreateEventHandler(first.id(), p("onChanged"), "_legacyTileChanged"));
            session = reopen(session, source(session).replace("// TODO: Handle onFocusChange.",
                    "focusCalls += 1; // Preserve focus handler.")
                    .replace("// TODO: Handle onChanged.", "legacyCalls += 1; // Also preserve the optional legacy observer."));
            var proofBaseline = session;
            session = apply(session, new RenameStateField(owner.id(), "_selection", "_selectedValue"));
            String output = source(session);
            assertTrue(output.contains("String? _selectedValue"), output);
            assertTrue(output.contains("void _selectionChanged(String? value)"), output);
            assertTrue(output.contains("groupValue: _selectedValue"), output);
            assertFalse(output.contains("_selection ="), output);
            assertTrue(output.contains("// User-owned Привіт."), output);
            assertAnalysis(proofBaseline, session, file, true);
            Files.write(lib.resolve(file), session.current().dartCandidateBytes());
            var restored = reopen(session, Files.readString(lib.resolve(file)));
            var detached = apply(restored, new RemoveStateBinding(owner.id()));
            assertTrue(source(detached).contains("void _selectionChanged(String? value)"), source(detached));
            assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
            var edited = apply(restored, new SetProperty(first.id(), p("controlAffinity"),
                    new PropertyValue.EnumValue("ListTileControlAffinity", "trailing")));
            assertTrue(source(edited).contains("Preserve focus handler."));
            assertArrayEquals(restored.current().dartCandidateBytes(), edited.undo().session().current().dartCandidateBytes());
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("tile_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/services.dart';
                import 'package:flutter/semantics.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:radio_list_tile_state_contract/legacy_standard.dart' as ls;
                import 'package:radio_list_tile_state_contract/legacy_adaptive.dart' as la;
                import 'package:radio_list_tile_state_contract/modern_standard.dart' as ms;
                import 'package:radio_list_tile_state_contract/modern_adaptive.dart' as ma;
                Widget form(bool modern, bool adaptive) => modern
                    ? (adaptive ? const ma.Sample() : const ms.Sample())
                    : (adaptive ? const la.Sample() : const ls.Sample());
                Finder formFinder(bool modern, bool adaptive) => find.byType(modern
                    ? (adaptive ? ma.Sample : ms.Sample) : (adaptive ? la.Sample : ls.Sample));
                Future<void> mount(WidgetTester tester, bool modern, bool adaptive, TargetPlatform platform) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(platform: platform),
                    home: Scaffold(body: form(modern, adaptive))));
                  await tester.pumpAndSettle();
                }
                List<RadioListTile<String>> tiles(WidgetTester tester) =>
                    tester.widgetList<RadioListTile<String>>(find.byType(RadioListTile<String>)).toList();
                String? selection(WidgetTester tester, bool modern) => modern
                    ? tester.widget<RadioGroup<String>>(find.byType(RadioGroup<String>)).groupValue
                    : tiles(tester).first.groupValue;
                void main() {
                  for (final modern in [false, true]) for (final adaptive in [false, true]) {
                    for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
                      testWidgets('generated radio selection $modern $adaptive $platform', (tester) async {
                        await mount(tester, modern, adaptive, platform);
                        expect(selection(tester, modern), isNull);
                        expect(tiles(tester).first.selected, isFalse);
                        expect(find.text('selected mirror'), findsNothing);
                        await tester.tap(find.text('First')); await tester.pumpAndSettle();
                        expect(selection(tester, modern), 'First');
                        expect(tiles(tester).first.selected, isTrue);
                        expect(find.text('selected mirror'), findsOneWidget);
                        final dynamic state = tester.state(formFinder(modern, adaptive));
                        if (modern) expect(state.legacyCalls, 1);
                        await tester.tap(find.text('First')); await tester.pumpAndSettle();
                        expect(selection(tester, modern), isNull, reason: 'Toggleable must request null');
                        expect(find.text('selected mirror'), findsNothing);
                        await tester.tap(find.text('Second')); await tester.pumpAndSettle();
                        expect(selection(tester, modern), 'Second');
                        if (!modern) expect(tiles(tester).map((tile) => tile.groupValue).take(2), ['Second', 'Second']);
                        await tester.tap(find.text('Disabled')); await tester.pumpAndSettle();
                        expect(selection(tester, modern), 'Second');
                        expect(tiles(tester).last.enabled, isFalse);
                        expect(tester.takeException(), isNull);
                      });
                    }
                    testWidgets('native checked semantics and outer focus callback $modern $adaptive', (tester) async {
                      final semantics = tester.ensureSemantics();
                      await mount(tester, modern, adaptive, TargetPlatform.android);
                      final firstRadio = find.byType(Radio<String>).first;
                      final before = tester.getSemantics(firstRadio).getSemanticsData();
                      expect(before.label, contains('First'));
                      expect(before.hasFlag(SemanticsFlag.hasCheckedState), isTrue);
                      expect(before.hasFlag(SemanticsFlag.isChecked), isFalse);
                      expect(before.hasAction(SemanticsAction.tap), isTrue);
                      final dynamic state = tester.state(formFinder(modern, adaptive));
                      final int focusBefore = state.focusCalls;
                      tester.widget<ListTile>(find.byType(ListTile).first).onFocusChange!(true);
                      await tester.pump();
                      expect(state.focusCalls, focusBefore + 1);
                      await tester.tap(find.text('First')); await tester.pumpAndSettle();
                      expect(tester.getSemantics(firstRadio).getSemanticsData().hasFlag(SemanticsFlag.isChecked), isTrue);
                      semantics.dispose(); expect(tester.takeException(), isNull);
                    });
                  }
                  for (final adaptive in [false, true]) {
                    testWidgets('modern group has one tile client and mixed Radio keyboard traversal $adaptive', (tester) async {
                      await mount(tester, true, adaptive, TargetPlatform.android);
                      await tester.tap(find.text('First')); await tester.pumpAndSettle();
                      tester.widget<ListTile>(find.byType(ListTile).first).focusNode!.requestFocus();
                      await tester.pumpAndSettle();
                      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown); await tester.pumpAndSettle();
                      expect(selection(tester, true), 'Second');
                      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown); await tester.pumpAndSettle();
                      expect(selection(tester, true), 'Third', reason: 'Skip disabled tile and hidden internal Radio clients');
                      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown); await tester.pumpAndSettle();
                      expect(selection(tester, true), 'First');
                      expect(tester.takeException(), isNull);
                    });
                  }
                }
                """);
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
    }

    private static WidgetNode tile(String variant, String value, boolean modern, boolean disabled) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("variant"), s(variant)); values.put(p("valueType"), s("String")); values.put(p("value"), s(value));
        values.put(p("toggleable"), new PropertyValue.BooleanValue(true));
        if (!modern) values.put(p("onChanged"), s("noop"));
        if (disabled) { values.put(p("enabled"), new PropertyValue.BooleanValue(false)); values.put(p("onChanged"), new PropertyValue.NullValue()); }
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.RadioListTile"), values,
                Map.of(new SlotName("title"), WidgetSlot.SingleSlot.of(text(value)),
                        new SlotName("subtitle"), WidgetSlot.SingleSlot.of(text(value + " subtitle")),
                        new SlotName("secondary"), WidgetSlot.SingleSlot.of(text(value + " secondary"))));
    }
    private static StatePropertyBinding sharedSelection() {
        return new StatePropertyBinding("_selection", StateBinding.Type.NULLABLE_STRING, Optional.empty(),
                StatePropertyBinding.Transform.DIRECT, Optional.empty());
    }
    private static StatePropertyBinding firstSelected() {
        return new StatePropertyBinding("_selection", StateBinding.Type.NULLABLE_STRING, Optional.empty(),
                StatePropertyBinding.Transform.EQUALS, Optional.of(s("First")));
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), s(value)), Map.of());
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue s(String value) { return new PropertyValue.StringValue(value); }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: radio_list_tile_state_contract
                publish_to: none
                environment:
                  sdk: '>=3.11.0 <4.0.0'
                dependencies:
                  flutter:
                    sdk: flutter
                dev_dependencies:
                  flutter_test:
                    sdk: flutter
                """);
        lib = Files.createDirectories(project.resolve("lib"));
        run(List.of(flutter.toString(), "pub", "get", "--offline"), "pub-get.log");
    }
    private void assertAnalysis(DesignerCommandSession baseline, DesignerCommandSession candidate, String file, boolean pass) throws Exception {
        Path source = lib.resolve(file);
        Files.write(source, baseline.current().dartCandidateBytes());
        var pair = candidate.current().preparedPair().orElseThrow();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(baseline.current().fdSnapshot()),
                new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
        var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, source, DartCandidateWarningPolicy.ALLOW, sdk);
        assertTrue(ticket.request().symbolProbes().stream().anyMatch(probe -> probe.staticTypeProbe()
                .isPresent()), ticket.request().toString());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var result = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, result);
            assertEquals(pass, accepted.ready(), () -> result + "\n" + accepted.diagnostics());
            if (!pass) assertTrue(result.symbolEvidence().stream().anyMatch(evidence -> evidence.staticTypeEvidence()
                    .filter(proof -> !proof.accepted()).isPresent()), result.toString());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
    }
    private static DesignerCommandSession open(WidgetNode root, String file, String members) throws Exception {
        return StateBindingRealSdkTest.openRoot(root, file, members);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session, String source) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static PropertyValue imported(String name) { return imported(name, Optional.empty(), false); }
    private static PropertyValue imported(String name, Optional<String> member, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:radio_list_tile_state_contract/builders.dart"), name, member,
                factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                        : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertTrue(result.changed(), result.diagnostics().toString());
        return result.session();
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private void run(List<String> command, String logName) throws Exception {
        Path log = project.resolve(logName);
        Process process = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(180, TimeUnit.SECONDS);
        if (!finished) { process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS); }
        String output = Files.readString(log);
        assertTrue(finished, command + " timed out\n" + output);
        assertEquals(0, process.exitValue(), command + "\n" + output);
    }
}
