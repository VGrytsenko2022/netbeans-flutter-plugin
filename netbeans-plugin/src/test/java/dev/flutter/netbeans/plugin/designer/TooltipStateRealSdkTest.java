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

/** Generated Tooltip events, native trigger behavior and reviewed State consumers. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class TooltipStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;



    @Test
    void generatedTooltipEventsStateAndNativeTriggersSurviveSaveReopenHistory() throws Exception {
        initialize();
        for (String mode : List.of("manual", "tap", "longPress")) {
            var first = tooltip("Trigger " + mode, mode, false);
            var rich = tooltip("Rich anchor", "tap", true);
            var empty = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Tooltip"),
                    Map.of(p("message"), new PropertyValue.StringValue("")),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Empty anchor"))));
            var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                    .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
            var gateValues = new LinkedHashMap<>(prototype.properties());
            gateValues.put(p("value"), new PropertyValue.BooleanValue(true));
            var gate = new WidgetNode(prototype.id(), prototype.type(), gateValues, prototype.slots());
            var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(gate, first, rich, empty))));
            String members = """
                      int triggeredEvents = 0; // User-owned Привіт.
                      int positioned = 0;
                      TooltipPositionContext? lastPositionContext;
                      InlineSpan get _richSpan => const TextSpan(text: 'Rich message ', children: [
                        TextSpan(text: 'bold', style: TextStyle(fontWeight: FontWeight.bold)),
                        WidgetSpan(child: SizedBox(width: 24, height: 20, child: Text('W'))),
                      ]);
                      Offset _positionTooltip(TooltipPositionContext context) {
                        positioned += 1;
                        lastPositionContext = context;
                        return const Offset(12, 34);
                      }
                    """;
            String file = (mode.equals("longPress") ? "long_press" : mode) + ".dart";
            var session = apply(open(root, file, members), new CreateStateBinding(gate.id(), "_options", "_optionsChanged"));
            for (String name : CONSUMERS) session = apply(session, new BindPropertyToState(first.id(), p(name), field("_options")));
            session = apply(session, new CreateEventHandler(first.id(), p("onTriggered"), "_triggered"));
            assertTrue(source(session).contains("// TODO: Handle onTriggered."), source(session));
            session = reopen(session, source(session).replace("// TODO: Handle onTriggered.",
                    "setState(() { triggeredEvents += 1; }); // Keep my tooltip handler."));
            var baseline = session;
            session = apply(session, new RenameStateField(gate.id(), "_options", "_tooltipOptions"));
            session = apply(session, new RenameEventHandler(first.id(), p("onTriggered"), "_onTooltipTriggered"));
            assertTrue(source(session).contains("void _onTooltipTriggered()"), source(session));
            for (String name : CONSUMERS) assertTrue(source(session).contains(name + ": _tooltipOptions"), source(session));
            assertTrue(source(session).contains("Keep my tooltip handler."));
            var invalid = session.apply(new CreateStateBinding(first.id(), "_visibility", "_visibilityChanged"));
            assertFalse(invalid.changed(), "Tooltip has no controlled visible State producer");
            assertArrayEquals(session.current().dartCandidateBytes(), invalid.session().current().dartCandidateBytes());
            assertAnalysis(baseline, session, file, true);
            Files.write(lib.resolve(file), session.current().dartCandidateBytes());
            var restored = reopen(session, Files.readString(lib.resolve(file)));
            var detached = apply(restored, new RemovePropertyStateBinding(first.id(), p("preferBelow")));
            assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
            var reset = apply(restored, new ResetProperty(first.id(), p("onTriggered")));
            assertTrue(source(reset).contains("void _onTooltipTriggered()"));
            assertFalse(source(reset).contains("onTriggered: _onTooltipTriggered"));
            assertArrayEquals(restored.current().dartCandidateBytes(), reset.undo().session().current().dartCandidateBytes());
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("tooltip_test.dart"), runtimeTests());
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
    }

    private static final List<String> CONSUMERS = List.of(
            "preferBelow", "excludeFromSemantics", "enableTapToDismiss", "enableFeedback", "ignorePointer");

    private static WidgetNode tooltip(String label, String mode, boolean rich) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("triggerMode"), new PropertyValue.EnumValue("TooltipTriggerMode", mode));
        values.put(p("waitDurationUs"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(10000)));
        values.put(p("showDurationUs"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(200000)));
        values.put(p("exitDurationUs"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(30000)));
        if (rich) {
            values.put(p("richMessage"), reference("_richSpan"));
        } else {
            values.put(p("message"), new PropertyValue.StringValue("Message " + mode));
            values.put(p("verticalOffset"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(8)));
            values.put(p("positionDelegate"), reference("_positionTooltip"));
        }
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Tooltip"), values,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text(label))));
    }

    private static PropertyValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }

    private static String runtimeTests() {
        return """
                import 'package:flutter/material.dart';
                import 'package:flutter/foundation.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:tooltip_state_contract/manual.dart' as manual;
                import 'package:tooltip_state_contract/tap.dart' as tapped;
                import 'package:tooltip_state_contract/long_press.dart' as pressed;

                Finder tip(String label) => find.ancestor(of: find.text(label), matching: find.byType(Tooltip)).first;
                Finder raw(String label) => find.descendant(of: tip(label), matching: find.byType(RawTooltip)).first;
                Finder content(String value) => find.text(value, findRichText: true);
                Future<dynamic> mount(WidgetTester tester, String mode, TargetPlatform platform) async {
                  expect(defaultTargetPlatform, platform);
                  final Widget sample = switch(mode) {
                    'manual' => const manual.Sample(),
                    'tap' => const tapped.Sample(),
                    _ => const pressed.Sample(),
                  };
                  await tester.pumpWidget(MaterialApp(home: Scaffold(body: sample)));
                  await tester.pumpAndSettle();
                  return switch(mode) {
                    'manual' => tester.state(find.byType(manual.Sample)),
                    'tap' => tester.state(find.byType(tapped.Sample)),
                    _ => tester.state(find.byType(pressed.Sample)),
                  };
                }
                Future<void> dismiss(WidgetTester tester) async {
                  Tooltip.dismissAllToolTips();
                  await tester.pump();
                  await tester.pump(const Duration(milliseconds: 250));
                }
                Future<void> dispose(WidgetTester tester) async {
                  await tester.pumpWidget(const SizedBox());
                  await tester.pump(const Duration(seconds: 2));
                  expect(tester.takeException(), isNull);
                }
                void main() {
                  for (final mode in ['manual', 'tap', 'longPress']) {
                    for (final platform in [TargetPlatform.android, TargetPlatform.iOS, TargetPlatform.windows]) {
                      testWidgets('real trigger semantics and all generated State consumers $mode $platform', (tester) async {
                        final dynamic state = await mount(tester, mode, platform);
                        final anchor = 'Trigger $mode';
                        final message = 'Message $mode';
                        final tooltipState = tester.state<TooltipState>(tip(anchor));
                        expect(state.triggeredEvents, 0);
                        final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                        await mouse.addPointer(location: const Offset(790, 590));
                        await mouse.moveTo(tester.getCenter(find.text(anchor)));
                        await tester.pump(); await tester.pump(const Duration(milliseconds: 250));
                        expect(content(message), findsOneWidget);
                        expect(state.triggeredEvents, 0, reason: 'Hover is independent of mode and never calls onTriggered.');
                        expect(state.positioned, greaterThan(0));
                        expect(state.lastPositionContext.verticalOffset, 8);
                        expect(state.lastPositionContext.preferBelow, isTrue);
                        expect(state.lastPositionContext.overlaySize, const Size(800, 600));
                        expect(tester.getTopLeft(content(message)).dx, greaterThanOrEqualTo(12));
                        await mouse.moveTo(const Offset(790, 590));
                        await tester.pump(); await tester.pump(const Duration(milliseconds: 300));
                        expect(content(message), findsNothing);
                        expect(tooltipState.ensureTooltipVisible(), isTrue);
                        await tester.pumpAndSettle();
                        expect(content(message), findsOneWidget);
                        expect(tooltipState.ensureTooltipVisible(), isFalse);
                        expect(state.triggeredEvents, 0, reason: 'Pinned ensureTooltipVisible does not invoke the callback despite API prose.');
                        await dismiss(tester);
                        await tester.tap(find.text(anchor)); await tester.pump();
                        expect(state.triggeredEvents, mode == 'tap' ? 1 : 0);
                        await tester.pump(const Duration(milliseconds: 500));
                        if (mode == 'longPress') {
                          await tester.longPress(find.text(anchor)); await tester.pump();
                          expect(state.triggeredEvents, 1);
                          await tester.pump(const Duration(milliseconds: 500));
                        }
                        expect(tester.state<TooltipState>(tip(anchor)), same(tooltipState));
                        await tester.tap(find.byType(Checkbox)); await tester.pumpAndSettle();
                        final widget = tester.widget<Tooltip>(tip(anchor));
                        expect(widget.preferBelow, isFalse);
                        expect(widget.excludeFromSemantics, isFalse);
                        expect(widget.enableTapToDismiss, isFalse);
                        expect(widget.enableFeedback, isFalse);
                        expect(widget.ignorePointer, isFalse);
                        expect(tester.state<TooltipState>(tip(anchor)), same(tooltipState));
                        await mouse.removePointer(); await dispose(tester);
                      }, variant: TargetPlatformVariant.only(platform));
                    }
                  }
                  testWidgets('rich WidgetSpan renders and empty message bypasses tooltip machinery', (tester) async {
                    final dynamic state = await mount(tester, 'tap', TargetPlatform.android);
                    final rich = tester.widget<Tooltip>(tip('Rich anchor'));
                    expect(rich.message, isNull);
                    expect(rich.richMessage, isA<TextSpan>());
                    expect(tester.widget<RawTooltip>(raw('Rich anchor')).ignorePointer, isFalse);
                    expect(find.descendant(of: tip('Empty anchor'), matching: find.byType(RawTooltip)), findsNothing);
                    await tester.tap(find.text('Rich anchor')); await tester.pump();
                    await tester.pump(const Duration(milliseconds: 100));
                    expect(find.text('W'), findsOneWidget);
                    expect(content('Rich message bold\\uFFFC'), findsOneWidget);
                    expect(state.triggeredEvents, 0);
                    await dispose(tester);
                  }, variant: TargetPlatformVariant.only(TargetPlatform.android));
                  testWidgets('State-controlled tap dismissal retains native behavior', (tester) async {
                    await mount(tester, 'manual', TargetPlatform.android);
                    final selected = tester.state<TooltipState>(tip('Trigger manual'));
                    expect(selected.ensureTooltipVisible(), isTrue); await tester.pumpAndSettle();
                    await tester.tapAt(const Offset(780, 580)); await tester.pump(); await tester.pump(const Duration(milliseconds: 250));
                    expect(content('Message manual'), findsNothing);
                    await tester.tap(find.byType(Checkbox)); await tester.pumpAndSettle();
                    expect(selected.ensureTooltipVisible(), isTrue); await tester.pumpAndSettle();
                    await tester.tapAt(const Offset(780, 580)); await tester.pump(); await tester.pump(const Duration(milliseconds: 250));
                    expect(content('Message manual'), findsOneWidget);
                    await dispose(tester);
                  }, variant: TargetPlatformVariant.only(TargetPlatform.android));
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
                name: tooltip_state_contract
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
