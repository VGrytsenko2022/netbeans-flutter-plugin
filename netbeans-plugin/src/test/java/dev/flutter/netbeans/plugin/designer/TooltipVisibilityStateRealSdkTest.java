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

/** Generated required visibility, typed State consumer and pinned native Tooltip behavior. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class TooltipVisibilityStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;

    @Test
    void generatedVisibilityStateScopePrecedenceAndNativeTooltipLifecycle() throws Exception {
        initialize();
        var gate = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
        var gateValues = new LinkedHashMap<>(gate.properties());
        gateValues.put(p("value"), new PropertyValue.BooleanValue(true));
        gate = new WidgetNode(gate.id(), gate.type(), gateValues, gate.slots());
        var inherited = scope(true, column(List.of(tooltip("manual"), tooltip("tap"), tooltip("longPress"))));
        var nested = scope(false, column(List.of(tooltip("Disabled"), scope(true, tooltip("Override")))));
        var root = column(List.of(gate, inherited, nested, tooltip("Outside")));
        String members = """
                  int triggeredEvents = 0; // User-owned Привіт.
                  void _triggered() { triggeredEvents += 1; }
                """;
        String file = "visibility.dart";
        var initial = open(root, file, members);
        assertTrue(source(initial).contains("TooltipVisibility("), source(initial));
        assertTrue(source(initial).contains("visible: true"));
        assertTrue(source(initial).contains("visible: false"));
        var session = apply(initial, new CreateStateBinding(gate.id(), "_shown", "_shownChanged"));
        session = apply(session, new BindPropertyToState(inherited.id(), p("visible"), field("_shown")));
        session = reopen(session, source(session));
        var baseline = session;
        session = apply(session, new RenameStateField(gate.id(), "_shown", "_tooltipVisibility"));
        assertTrue(source(session).contains("visible: _tooltipVisibility"), source(session));
        assertTrue(source(session).contains("User-owned Привіт."));
        assertTrue(source(session).contains("void _triggered()"));
        for (DesignerCommand invalid : List.<DesignerCommand>of(
                new CreateStateBinding(inherited.id(), "_invented", "_inventedChanged"),
                new CreateEventHandler(inherited.id(), p("onChanged"), "_inventedEvent"),
                new ResetProperty(inherited.id(), p("visible")),
                new SetProperty(inherited.id(), p("visible"), new PropertyValue.NullValue()),
                new SetProperty(inherited.id(), p("visible"), new PropertyValue.StringValue("false")))) {
            var rejected = session.apply(invalid);
            assertFalse(rejected.changed(), invalid + ": " + rejected.diagnostics());
            assertArrayEquals(session.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
            assertArrayEquals(session.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
        }
        assertAnalysis(baseline, session, file, true);
        Files.write(lib.resolve(file), session.current().dartCandidateBytes());
        var restored = reopen(session, Files.readString(lib.resolve(file)));
        assertArrayEquals(session.current().dartCandidateBytes(), restored.current().dartCandidateBytes());
        var detached = apply(restored, new RemovePropertyStateBinding(inherited.id(), p("visible")));
        assertFalse(source(detached).contains("visible: _tooltipVisibility"));
        assertTrue(source(detached).contains("visible: true"));
        assertTrue(source(detached).contains("User-owned Привіт."));
        assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, detached, file, true);
        Files.write(lib.resolve(file), restored.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("visibility_test.dart"), runtimeTests());
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
        assertTrue(Files.readString(project.resolve("flutter-test.log")).contains("All tests passed!"));
    }

    private static WidgetNode scope(boolean visible, WidgetNode child) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.TooltipVisibility"),
                Map.of(p("visible"), new PropertyValue.BooleanValue(visible)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static WidgetNode column(List<WidgetNode> children) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode tooltip(String mode) {
        String trigger = List.of("manual", "tap", "longPress").contains(mode) ? mode : "tap";
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Tooltip"),
                Map.of(p("message"), new PropertyValue.StringValue("Message " + mode),
                        p("triggerMode"), new PropertyValue.EnumValue("TooltipTriggerMode", trigger),
                        p("waitDurationUs"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(10000)),
                        p("showDurationUs"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(200000)),
                        p("onTriggered"), new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_triggered",
                                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Anchor " + mode))));
    }

    private static String runtimeTests() {
        return """
                import 'package:flutter/material.dart';
                import 'package:flutter/foundation.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:tooltip_visibility_contract/visibility.dart';

                Finder tip(String name) => find.ancestor(of: find.text('Anchor $name'), matching: find.byType(Tooltip)).first;
                Finder raw(String name) => find.descendant(of: tip(name), matching: find.byType(RawTooltip));
                Finder content(String name) => find.text('Message $name', findRichText: true);
                Future<dynamic> mount(WidgetTester tester) async {
                  await tester.pumpWidget(const MaterialApp(home: Scaffold(body: Sample())));
                  await tester.pumpAndSettle();
                  return tester.state(find.byType(Sample));
                }
                Future<void> visible(WidgetTester tester, bool value) async {
                  tester.widget<Checkbox>(find.byType(Checkbox)).onChanged!(value);
                  await tester.pumpAndSettle();
                }
                Future<void> cleanup(WidgetTester tester) async {
                  await tester.pumpWidget(const SizedBox());
                  await tester.pump(const Duration(seconds: 2));
                  expect(tester.takeException(), isNull);
                }
                Future<void> dismiss(WidgetTester tester) async {
                  Tooltip.dismissAllToolTips();
                  await tester.pump(); await tester.pump(const Duration(milliseconds: 250));
                }
                void main() {
                  for (final mode in ['manual', 'tap', 'longPress']) {
                    for (final platform in [TargetPlatform.android, TargetPlatform.iOS, TargetPlatform.windows]) {
                      testWidgets('generated visible gates every activation $mode $platform', (tester) async {
                        expect(defaultTargetPlatform, platform);
                        final dynamic state = await mount(tester);
                        final original = tester.state<TooltipState>(tip(mode));
                        await visible(tester, false);
                        expect(tester.state<TooltipState>(tip(mode)), same(original));
                        expect(raw(mode), findsNothing);
                        expect(original.ensureTooltipVisible(), isFalse);
                        final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                        await mouse.addPointer(location: const Offset(790, 590));
                        await mouse.moveTo(tester.getCenter(find.text('Anchor $mode')));
                        await tester.pump(); await tester.pump(const Duration(milliseconds: 250));
                        expect(content(mode), findsNothing);
                        await mouse.moveTo(const Offset(790, 590)); await tester.pump();
                        await tester.tap(find.text('Anchor $mode')); await tester.pump();
                        await tester.longPress(find.text('Anchor $mode')); await tester.pump();
                        expect(state.triggeredEvents, 0);
                        expect(content(mode), findsNothing);
                        await visible(tester, true);
                        expect(tester.state<TooltipState>(tip(mode)), same(original));
                        expect(raw(mode), findsOneWidget);
                        expect(original.ensureTooltipVisible(), isTrue);
                        await tester.pumpAndSettle();
                        expect(content(mode), findsOneWidget);
                        expect(state.triggeredEvents, 0, reason: 'Pinned programmatic activation has no onTriggered callback.');
                        await dismiss(tester);
                        if (mode == 'tap') {
                          await tester.tap(find.text('Anchor $mode')); await tester.pump();
                          expect(state.triggeredEvents, 1);
                        } else if (mode == 'longPress') {
                          await tester.longPress(find.text('Anchor $mode')); await tester.pump();
                          expect(state.triggeredEvents, 1);
                        }
                        await dismiss(tester);
                        await mouse.moveTo(tester.getCenter(find.text('Anchor $mode')));
                        await tester.pump(); await tester.pump(const Duration(milliseconds: 100));
                        expect(content(mode), findsOneWidget);
                        await mouse.removePointer(); await cleanup(tester);
                      }, variant: TargetPlatformVariant.only(platform));
                    }
                  }
                  testWidgets('closest nested scope overrides false and unwrapped scope defaults true', (tester) async {
                    await mount(tester);
                    await visible(tester, false);
                    expect(TooltipVisibility.of(tester.element(tip('Disabled'))), isFalse);
                    expect(TooltipVisibility.of(tester.element(tip('Override'))), isTrue);
                    expect(TooltipVisibility.of(tester.element(tip('Outside'))), isTrue);
                    expect(tester.state<TooltipState>(tip('Disabled')).ensureTooltipVisible(), isFalse);
                    for (final label in ['Override', 'Outside']) {
                      expect(tester.state<TooltipState>(tip(label)).ensureTooltipVisible(), isTrue);
                      await tester.pumpAndSettle();
                      expect(content(label), findsOneWidget);
                      await dismiss(tester);
                    }
                    await cleanup(tester);
                  });
                  testWidgets('false closes existing overlay while retaining Tooltip State and child layout', (tester) async {
                    await mount(tester);
                    final state = tester.state<TooltipState>(tip('tap'));
                    final rect = tester.getRect(find.text('Anchor tap'));
                    expect(state.ensureTooltipVisible(), isTrue);
                    await tester.pumpAndSettle();
                    expect(content('tap'), findsOneWidget);
                    await visible(tester, false);
                    expect(content('tap'), findsNothing);
                    expect(tester.state<TooltipState>(tip('tap')), same(state));
                    expect(tester.getRect(find.text('Anchor tap')), rect);
                    await visible(tester, true);
                    expect(state.ensureTooltipVisible(), isTrue);
                    await tester.pumpAndSettle();
                    expect(content('tap'), findsOneWidget);
                    await cleanup(tester);
                  });
                  testWidgets('SDK keeps child semantics but removes RawTooltip annotation when disabled', (tester) async {
                    final semantics = tester.ensureSemantics();
                    await mount(tester);
                    expect(tester.getSemantics(find.text('Anchor tap')).getSemanticsData().tooltip, 'Message tap');
                    await visible(tester, false);
                    final data = tester.getSemantics(find.text('Anchor tap')).getSemanticsData();
                    expect(data.label, contains('Anchor tap'));
                    expect(data.tooltip, isEmpty, reason: 'Pinned 3.44.8 removes RawTooltip itself, contrary to broad API prose.');
                    expect(raw('tap'), findsNothing);
                    await visible(tester, true);
                    expect(tester.getSemantics(find.text('Anchor tap')).getSemanticsData().tooltip, 'Message tap');
                    await cleanup(tester); semantics.dispose();
                  });
                  testWidgets('disabling cancels a pending hover without a stale timer', (tester) async {
                    await mount(tester);
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(790, 590));
                    await mouse.moveTo(tester.getCenter(find.text('Anchor tap')));
                    await tester.pump();
                    await visible(tester, false);
                    await tester.pump(const Duration(seconds: 2));
                    expect(content('tap'), findsNothing);
                    expect(tester.takeException(), isNull);
                    await mouse.removePointer(); await cleanup(tester);
                  });
                }
                """;
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
                name: tooltip_visibility_contract
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

