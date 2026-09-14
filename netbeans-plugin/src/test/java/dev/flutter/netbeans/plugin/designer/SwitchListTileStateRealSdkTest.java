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

/** Generated application State, native tile interaction, focus, semantics and adaptive controls. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class SwitchListTileStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;

    @Test
    void sharedBooleanStateEventsRenameAndRealTileInteractionSurviveSaveAndReopen() throws Exception {
        initialize();
        for (String variant : List.of("standard", "adaptive")) {
            var first = tile(variant, "First", false);
            var second = tile(variant, "Second", false);
            var disabled = tile(variant, "Disabled", true);
            var mirror = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Visibility"), Map.of(),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("enabled mirror"))));
            var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, second, disabled, mirror))));
            String file = variant + ".dart";
            var initial = open(root, file, "  int focusCalls = 0; // User-owned Привіт.\n");
            var session = apply(initial, new CreateStateBinding(first.id(), "_enabled", "_firstChanged"));
            session = apply(session, new CreateStateBinding(second.id(), "_enabled", "_secondChanged",
                    StateBinding.Action.CHANGE, Optional.empty(), "", Optional.of(field("_enabled"))));
            session = apply(session, new BindPropertyToState(first.id(), p("selected"), field("_enabled")));
            session = apply(session, new BindPropertyToState(mirror.id(), p("visible"), field("_enabled")));
            session = apply(session, new CreateEventHandler(first.id(), p("onFocusChange"), "_focusChanged"));
            session = reopen(session, source(session).replace("// TODO: Handle onFocusChange.",
                    "focusCalls += 1; // Keep the user focus handler."));
            var proofBaseline = session;
            session = apply(session, new RenameStateField(first.id(), "_enabled", "_checked"));
            String output = source(session);
            assertTrue(output.contains("bool _checked = false;"), output);
            assertTrue(output.contains("void _firstChanged(bool value)"), output);
            assertTrue(output.contains("void _secondChanged(bool value)"), output);
            assertFalse(output.contains("_enabled")); assertTrue(output.contains("// User-owned Привіт."));
            assertAnalysis(proofBaseline, session, file, true);
            Files.write(lib.resolve(file), session.current().dartCandidateBytes());
            var restored = reopen(session, Files.readString(lib.resolve(file)));
            var detached = apply(restored, new RemoveStateBinding(second.id()));
            assertTrue(source(detached).contains("void _secondChanged(bool value)"));
            assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
            var renamed = apply(restored, new SetProperty(first.id(), p("controlAffinity"),
                    new PropertyValue.EnumValue("ListTileControlAffinity", "leading")));
            assertTrue(source(renamed).contains("Keep the user focus handler."));
            assertEquals(initial.current().document().root().slots().keySet(), restored.current().document().root().slots().keySet());
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("tile_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/semantics.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:switch_list_tile_state_contract/standard.dart' as standard;
                import 'package:switch_list_tile_state_contract/adaptive.dart' as adaptive;
                Future<void> mount(WidgetTester tester, bool useAdaptive, TargetPlatform platform) async {
                  await tester.pumpWidget(MaterialApp(theme: ThemeData(platform: platform),
                    home: Scaffold(body: useAdaptive ? const adaptive.Sample() : const standard.Sample())));
                  await tester.pumpAndSettle();
                }
                List<SwitchListTile> tiles(WidgetTester tester) => tester.widgetList<SwitchListTile>(find.byType(SwitchListTile)).toList();
                void main() {
                  for (final useAdaptive in [false, true]) {
                    for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
                      testWidgets('generated shared state toggles from whole tile and thumb: $useAdaptive $platform', (tester) async {
                        await mount(tester, useAdaptive, platform);
                        expect(tiles(tester).map((t) => t.value), [false, false, false]);
                        expect(tiles(tester).first.selected, isFalse);
                        expect(find.text('enabled mirror'), findsNothing);
                        await tester.tap(find.text('First')); await tester.pumpAndSettle();
                        expect(tiles(tester).map((t) => t.value), [true, true, false]);
                        expect(tiles(tester).first.selected, isTrue);
                        expect(find.text('enabled mirror'), findsOneWidget);
                        await tester.tap(find.byType(Switch).at(1)); await tester.pumpAndSettle();
                        expect(tiles(tester).map((t) => t.value), [false, false, false]);
                        await tester.tap(find.text('Disabled')); await tester.pumpAndSettle();
                        expect(tiles(tester).last.onChanged, isNull);
                        expect(tiles(tester).map((t) => t.value), [false, false, false]);
                        // Flutter 3.44.8 uses a unified Switch painter, not a CupertinoSwitch widget.
                        final controls = tester.widgetList<Switch>(find.byType(Switch)).toList();
                        expect(controls, hasLength(3));
                        expect(controls.every((control) => control.applyCupertinoTheme == (useAdaptive ? null : false)), isTrue);
                        expect(Theme.of(tester.element(find.byType(Switch).first)).platform, platform);
                        expect(tester.takeException(), isNull);
                      });
                    }
                    testWidgets('merged semantics and focus event remain native: $useAdaptive', (tester) async {
                      final semantics = tester.ensureSemantics();
                      await mount(tester, useAdaptive, TargetPlatform.android);
                      final first = tester.getSemantics(find.byType(Switch).first).getSemanticsData();
                      expect(first.label, contains('First'));
                      expect(first.hasFlag(SemanticsFlag.hasToggledState), isTrue);
                      expect(first.hasFlag(SemanticsFlag.isToggled), isFalse);
                      expect(first.hasFlag(SemanticsFlag.isButton), isFalse);
                      expect(first.hasAction(SemanticsAction.tap), isTrue);
                      final tileFocus = tester.widget<ListTile>(find.byType(ListTile).first);
                      tileFocus.onFocusChange!(true); await tester.pump();
                      final dynamic state = useAdaptive
                        ? tester.state<adaptive.Storage>(find.byType(adaptive.Sample))
                        : tester.state<standard.Storage>(find.byType(standard.Sample));
                      expect(state.focusCalls, 1);
                      await tester.tap(find.text('First')); await tester.pumpAndSettle();
                      expect(tester.getSemantics(find.byType(Switch).first).getSemanticsData().hasFlag(SemanticsFlag.isToggled), isTrue);
                      expect(tester.widgetList<ExcludeFocus>(find.byType(ExcludeFocus)).isNotEmpty, isTrue);
                      semantics.dispose(); expect(tester.takeException(), isNull);
                    });
                  }
                }
                """);
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
    }

    private static WidgetNode tile(String variant, String label, boolean disabled) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.SwitchListTile"),
                Map.of(p("value"), new PropertyValue.BooleanValue(false), p("variant"), new PropertyValue.StringValue(variant),
                        p("onChanged"), disabled ? new PropertyValue.NullValue() : new PropertyValue.StringValue("noop")),
                Map.of(new SlotName("title"), WidgetSlot.SingleSlot.of(text(label)),
                        new SlotName("subtitle"), WidgetSlot.SingleSlot.of(text(label + " subtitle")),
                        new SlotName("secondary"), WidgetSlot.SingleSlot.of(text(label + " secondary"))));
    }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static StatePropertyBinding field(String name) {
        return new StatePropertyBinding(name, StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT, Optional.empty());
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: switch_list_tile_state_contract
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:switch_list_tile_state_contract/builders.dart"), name, member,
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
