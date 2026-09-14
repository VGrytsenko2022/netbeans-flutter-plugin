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

/** Generated ExpansionTile events, controller ownership and reviewed State consumers. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class ExpansionTileStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;


    @Test
    void generatedEventsControllersAndStateConsumersSurviveReopenAndNativeExpansion() throws Exception {
        initialize();
        for (String mode : List.of("internal", "external", "preexpanded")) {
            var first = tile("First", false, false, false, !mode.equals("internal"));
            var retained = tile("Retained", false, true, false, false);
            var expanded = tile("Expanded", true, false, false, false);
            var disabled = tile("Disabled", false, false, true, false);
            var gatePrototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                    .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
            var gateValues = new LinkedHashMap<>(gatePrototype.properties());
            gateValues.put(p("value"), new PropertyValue.BooleanValue(true));
            var gate = new WidgetNode(gatePrototype.id(), gatePrototype.type(), gateValues, gatePrototype.slots());
            var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(gate, first, retained, expanded, disabled))));
            String members = """
                      int expansionEvents = 0; // User-owned Привіт.
                      bool? lastExpanded;
                      final ExpansibleController _controller = ExpansibleController()%s;
                      ExpansibleController get expansionController => _controller;
                      @override
                      void dispose() { _controller.dispose(); super.dispose(); }
                    """.formatted(mode.equals("preexpanded") ? "..expand()" : "");
            String file = mode + ".dart";
            var initial = open(root, file, members);
            var session = apply(initial, new CreateStateBinding(gate.id(), "_allowed", "_allowedChanged"));
            session = apply(session, new BindPropertyToState(first.id(), p("enabled"), field("_allowed")));
            session = apply(session, new CreateEventHandler(first.id(), p("onExpansionChanged"), "_expansionChanged"));
            String beforeBody = source(session);
            assertTrue(beforeBody.contains("// TODO: Handle onExpansionChanged."), beforeBody);
            session = reopen(session, beforeBody.replace("// TODO: Handle onExpansionChanged.",
                    "setState(() { expansionEvents += 1; lastExpanded = isExpanded; }); // Keep my expansion handler."));
            var baseline = session;
            session = apply(session, new RenameStateField(gate.id(), "_allowed", "_canExpand"));
            session = apply(session, new RenameEventHandler(first.id(), p("onExpansionChanged"), "_onExpanded"));
            var prohibited = session.apply(new BindPropertyToState(first.id(), p("initiallyExpanded"), field("_canExpand")));
            assertFalse(prohibited.changed(), "An initialization seed must not become a controlled State argument");
            assertArrayEquals(session.current().dartCandidateBytes(), prohibited.session().current().dartCandidateBytes());
            assertTrue(source(session).contains("void _onExpanded(bool isExpanded)"), source(session));
            assertTrue(source(session).contains("enabled: _canExpand"), source(session));
            assertTrue(source(session).contains("Keep my expansion handler."));
            assertAnalysis(baseline, session, file, true);
            Files.write(lib.resolve(file), session.current().dartCandidateBytes());
            var restored = reopen(session, Files.readString(lib.resolve(file)));
            var detached = apply(restored, new RemovePropertyStateBinding(first.id(), p("enabled")));
            assertTrue(source(detached).contains("Keep my expansion handler."));
            assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
            var reset = apply(restored, new ResetProperty(first.id(), p("onExpansionChanged")));
            assertTrue(source(reset).contains("void _onExpanded(bool isExpanded)"));
            assertFalse(source(reset).contains("onExpansionChanged: _onExpanded"));
            assertArrayEquals(restored.current().dartCandidateBytes(), reset.undo().session().current().dartCandidateBytes());
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("expansion_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/foundation.dart';
                import 'package:flutter/services.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:expansion_tile_state_contract/internal.dart' as internal;
                import 'package:expansion_tile_state_contract/external.dart' as externalApp;
                import 'package:expansion_tile_state_contract/preexpanded.dart' as preexpanded;
                Future<dynamic> mount(WidgetTester tester, String mode, TargetPlatform platform) async {
                  expect(defaultTargetPlatform, platform);
                  final Widget sample = switch (mode) {
                    'internal' => const internal.Sample(),
                    'external' => const externalApp.Sample(),
                    _ => const preexpanded.Sample(),
                  };
                  await tester.pumpWidget(MaterialApp(home: Scaffold(body: sample)));
                  await tester.pumpAndSettle();
                  return switch (mode) {
                    'internal' => tester.state(find.byType(internal.Sample)),
                    'external' => tester.state(find.byType(externalApp.Sample)),
                    _ => tester.state(find.byType(preexpanded.Sample)),
                  };
                }
                Finder tile(String title) => find.ancestor(of: find.text(title), matching: find.byType(ExpansionTile)).first;
                Finder fields(String title) => find.descendant(of: tile(title), matching: find.byType(TextField, skipOffstage: false), skipOffstage: false);
                Finder editable(String title) => find.descendant(of: tile(title), matching: find.byType(EditableText, skipOffstage: false), skipOffstage: false);
                void main() {
                  for (final mode in ['internal', 'external', 'preexpanded']) {
                    for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
                      testWidgets('generated events and real expansion $mode $platform', (tester) async {
                        final dynamic state = await mount(tester, mode, platform);
                        final seeded = mode == 'preexpanded';
                        expect(fields('First'), seeded ? findsOneWidget : findsNothing);
                        expect(fields('Expanded'), findsOneWidget);
                        expect(state.expansionEvents, 0);
                        await tester.tap(find.text('First')); await tester.pumpAndSettle();
                        expect(state.expansionEvents, 1);
                        expect(state.lastExpanded, !seeded);
                        expect(fields('First'), seeded ? findsNothing : findsOneWidget);
                        // Event setState rebuilds the same generated initiallyExpanded:false widget.
                        // The SDK keeps its live controller rather than reapplying that seed.
                        await tester.tap(find.text('First')); await tester.pumpAndSettle();
                        expect(state.expansionEvents, 2);
                        expect(state.lastExpanded, seeded);
                        await tester.tap(find.byType(Checkbox)); await tester.pumpAndSettle();
                        expect(tester.widget<ExpansionTile>(tile('First')).enabled, isFalse);
                        await tester.tap(find.text('First')); await tester.pumpAndSettle();
                        expect(state.expansionEvents, 2);
                        await tester.tap(find.text('Disabled')); await tester.pumpAndSettle();
                        expect(fields('Disabled'), findsNothing);
                        if (mode != 'internal') {
                          state.expansionController.toggle(); await tester.pumpAndSettle();
                          expect(state.expansionEvents, 3); // Controller is not disabled by enabled:false.
                          expect(fields('First'), seeded ? findsNothing : findsOneWidget);
                        }
                        // Dispose first, including the real iOS accessibility announcement timer.
                        await tester.pumpWidget(const SizedBox()); await tester.pump(const Duration(seconds: 2));
                        expect(tester.takeException(), isNull);
                      }, variant: TargetPlatformVariant.only(platform));
                    }
                  }
                  testWidgets('maintainState preserves retained editors but normal collapse disposes', (tester) async {
                    await mount(tester, 'internal', TargetPlatform.android);
                    await tester.tap(find.text('Retained')); await tester.pumpAndSettle();
                    final retainedState = tester.state(fields('Retained'));
                    await tester.enterText(editable('Retained'), 'keep my text');
                    await tester.tap(find.text('Retained')); await tester.pumpAndSettle();
                    expect(fields('Retained'), findsOneWidget);
                    expect(tester.state(fields('Retained')), same(retainedState));
                    await tester.tap(find.text('Retained')); await tester.pumpAndSettle();
                    expect(tester.widget<EditableText>(editable('Retained')).controller.text, 'keep my text');
                    final removedState = tester.state(fields('Expanded'));
                    await tester.enterText(editable('Expanded'), 'discard with child');
                    await tester.tap(find.text('Expanded')); await tester.pumpAndSettle();
                    expect(fields('Expanded'), findsNothing);
                    expect(removedState.mounted, isFalse);
                    await tester.tap(find.text('Expanded')); await tester.pumpAndSettle();
                    expect(tester.state(fields('Expanded')), isNot(same(removedState)));
                    expect(tester.widget<EditableText>(editable('Expanded')).controller.text, isEmpty);
                    expect(tester.takeException(), isNull);
                  }, variant: TargetPlatformVariant.only(TargetPlatform.android));
                  testWidgets('keyboard activation stays native and emits one event', (tester) async {
                    final dynamic state = await mount(tester, 'internal', TargetPlatform.android);
                    final header = find.descendant(of: tile('First'), matching: find.byType(ListTile)).first;
                    // Use the actual header's focus scope, not a callback invocation.
                    final ink = find.descendant(of: header, matching: find.byType(InkWell)).first;
                    final region = find.descendant(of: ink, matching: find.byType(MouseRegion)).first;
                    Focus.of(tester.element(region)).requestFocus();
                    await tester.pump();
                    await tester.sendKeyEvent(LogicalKeyboardKey.enter); await tester.pumpAndSettle();
                    expect(state.expansionEvents, 1);
                    expect(fields('First'), findsOneWidget);
                    expect(tester.takeException(), isNull);
                  }, variant: TargetPlatformVariant.only(TargetPlatform.android));
                }
                """);
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
    }

    private static WidgetNode tile(String label, boolean expanded, boolean retained, boolean disabled, boolean controlled) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("initiallyExpanded"), new PropertyValue.BooleanValue(expanded));
        values.put(p("maintainState"), new PropertyValue.BooleanValue(retained));
        values.put(p("enabled"), new PropertyValue.BooleanValue(!disabled));
        if (controlled) values.put(p("controller"), new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                "_controller", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        var field = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.TextField"), Map.of(), Map.of());
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.ExpansionTile"), values,
                Map.of(new SlotName("title"), WidgetSlot.SingleSlot.of(text(label)),
                        new SlotName("children"), new WidgetSlot.ListSlot(List.of(field))));
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
                name: expansion_tile_state_contract
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
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:expansion_tile_state_contract/builders.dart"), name, member,
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
