package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.*;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Generated complete theme, three State consumers and pinned native Tooltip behavior. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class TooltipThemeStateRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;

    @Test
    void generatedThemeAllFieldsStateScopesSaveReopenAndNativeBehavior() throws Exception {
        initialize();
        var gate = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.Checkbox")).orElseThrow(), StableId.random());
        var gateValues = new LinkedHashMap<>(gate.properties());
        gateValues.put(p("value"), new PropertyValue.BooleanValue(true));
        gate = new WidgetNode(gate.id(), gate.type(), gateValues, gate.slots());
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("constraints", "padding", "margin", "decoration", "textStyle")) values.put(p(name), reference("_" + name));
        values.put(p("verticalOffset"), number(17));
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback")) values.put(p(name), new PropertyValue.BooleanValue(false));
        values.put(p("textAlign"), new PropertyValue.EnumValue("TextAlign", "end"));
        values.put(p("triggerMode"), new PropertyValue.EnumValue("TooltipTriggerMode", "tap"));
        values.put(p("waitDurationUs"), integer(120000));
        values.put(p("showDurationUs"), integer(2200000));
        values.put(p("exitDurationUs"), integer(320000));
        var themed = theme(values, column(List.of(tooltip("inherited", false), tooltip("explicit", true),
                theme(Map.of(), tooltip("empty", false)),
                theme(Map.of(p("preferBelow"), new PropertyValue.NullValue()), tooltip("null", false)))));
        var whole = theme(Map.of(p("data"), reference("_wholeTheme")), tooltip("whole", false));
        var root = column(List.of(gate, themed, whole, theme(Map.of(p("height"), number(35)), tooltip("height", false))));
        String members = """
                  int triggeredEvents = 0; // User-owned Привіт.
                  void _triggered() { triggeredEvents += 1; }
                  final _constraints = const BoxConstraints(minWidth: 24, maxWidth: 280, minHeight: 30);
                  final _padding = const EdgeInsetsDirectional.fromSTEB(4, 3, 8, 5);
                  final _margin = const EdgeInsets.all(12);
                  final Decoration _decoration = const ShapeDecoration(shape: StadiumBorder(), color: Color(0xff123456));
                  final _textStyle = const TextStyle(fontSize: 18, color: Color(0xffabcdef), fontWeight: FontWeight.w600);
                  final _wholeTheme = const TooltipThemeData(triggerMode: TooltipTriggerMode.manual, exitDuration: Duration(microseconds: 789));
                """;
        String file = "theme.dart";
        var session = open(root, file, members);
        session = apply(session, new CreateStateBinding(gate.id(), "_options", "_optionsChanged"));
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback")) {
            session = apply(session, new BindPropertyToState(themed.id(), p(name), field("_options")));
        }
        session = reopen(session, source(session));
        var baseline = session;
        session = apply(session, new RenameStateField(gate.id(), "_options", "_themeOptions"));
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback")) assertTrue(source(session).contains(name + ": _themeOptions"));
        assertFalse(source(session).contains("copyWith("));
        assertFalse(source(session).contains("TooltipThemeData.lerp("));
        for (DesignerCommand invalid : List.<DesignerCommand>of(
                new CreateStateBinding(themed.id(), "_invented", "_inventedChanged"),
                new CreateEventHandler(themed.id(), p("onChanged"), "_inventedEvent"),
                new SetProperty(themed.id(), p("data"), reference("_wholeTheme")),
                new SetProperty(whole.id(), p("preferBelow"), new PropertyValue.NullValue()),
                new BindPropertyToState(whole.id(), p("preferBelow"), field("_themeOptions")),
                new SetProperty(whole.id(), p("data"), new PropertyValue.NullValue()))) {
            var rejected = session.apply(invalid);
            assertFalse(rejected.changed(), invalid + ": " + rejected.diagnostics());
            assertArrayEquals(session.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
            assertArrayEquals(session.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
        }
        assertAnalysis(baseline, session, file, true);
        Files.write(lib.resolve(file), session.current().dartCandidateBytes());
        var restored = reopen(session, Files.readString(lib.resolve(file)));
        assertArrayEquals(session.current().dartCandidateBytes(), restored.current().dartCandidateBytes());
        var detached = apply(restored, new RemovePropertyStateBinding(themed.id(), p("preferBelow")));
        assertTrue(source(detached).contains("preferBelow: false"));
        assertArrayEquals(restored.current().dartCandidateBytes(), detached.undo().session().current().dartCandidateBytes());
        assertAnalysis(restored, detached, file, true);
        var reset = apply(restored, new ResetProperty(whole.id(), p("data")));
        assertTrue(source(reset).contains("TooltipThemeData()"));
        assertArrayEquals(restored.current().dartCandidateBytes(), reset.undo().session().current().dartCandidateBytes());
        assertTrue(source(reset).contains("User-owned Привіт."));
        Files.write(lib.resolve(file), restored.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("theme_test.dart"), runtimeTests());
        run(List.of(dart.toString(), "analyze", "--no-fatal-warnings", lib.toString()), "analyze.log");
        run(List.of(flutter.toString(), "test", "--no-pub", "--reporter", "expanded"), "flutter-test.log");
        assertTrue(Files.readString(project.resolve("flutter-test.log")).contains("All tests passed!"));
    }

    private static WidgetNode theme(Map<PropertyName, PropertyValue> values, WidgetNode child) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.TooltipTheme"), values,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static WidgetNode column(List<WidgetNode> children) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
    }
    private static PropertyValue number(int value) { return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value)); }
    private static PropertyValue integer(long value) { return new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(value)); }
    private static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static WidgetNode tooltip(String label, boolean explicit) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("message"), new PropertyValue.StringValue("Message " + label));
        values.put(p("onTriggered"), reference("_triggered"));
        if (explicit) {
            values.put(p("triggerMode"), new PropertyValue.EnumValue("TooltipTriggerMode", "longPress"));
            values.put(p("waitDurationUs"), integer(42000));
            values.put(p("showDurationUs"), integer(520000));
            values.put(p("exitDurationUs"), integer(62000));
            values.put(p("excludeFromSemantics"), new PropertyValue.BooleanValue(false));
            values.put(p("enableFeedback"), new PropertyValue.BooleanValue(false));
        }
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.material.Tooltip"), values,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Anchor " + label))));
    }

    private static String runtimeTests() {
        return """
                import 'package:flutter/material.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:tooltip_theme_contract/theme.dart';

                Finder tip(String name) => find.ancestor(of: find.text('Anchor $name'), matching: find.byType(Tooltip)).first;
                RawTooltip raw(WidgetTester tester, String name) => tester.widget<RawTooltip>(find.descendant(of: tip(name), matching: find.byType(RawTooltip)));
                TooltipThemeData data(WidgetTester tester, String name) => TooltipTheme.of(tester.element(tip(name)));
                Future<dynamic> mount(WidgetTester tester) async {
                  await tester.pumpWidget(const MaterialApp(home: Scaffold(body: Sample())));
                  await tester.pumpAndSettle();
                  return tester.state(find.byType(Sample));
                }
                Future<void> options(WidgetTester tester, bool value) async {
                  tester.widget<Checkbox>(find.byType(Checkbox)).onChanged!(value);
                  await tester.pumpAndSettle();
                }
                Future<void> cleanup(WidgetTester tester) async {
                  await tester.pumpWidget(const SizedBox());
                  await tester.pump(const Duration(seconds: 3));
                  expect(tester.takeException(), isNull);
                }
                void main() {
                  testWidgets('all 15 data fields survive fresh generation including exitDuration', (tester) async {
                    await mount(tester);
                    final theme = data(tester, 'inherited');
                    expect(theme.height, isNull);
                    expect(theme.constraints, const BoxConstraints(minWidth: 24, maxWidth: 280, minHeight: 30));
                    expect(theme.padding, const EdgeInsetsDirectional.fromSTEB(4, 3, 8, 5));
                    expect(theme.margin, const EdgeInsets.all(12));
                    expect(theme.verticalOffset, 17);
                    expect(theme.preferBelow, isTrue);
                    expect(theme.excludeFromSemantics, isTrue);
                    expect(theme.enableFeedback, isTrue);
                    expect(theme.decoration, const ShapeDecoration(shape: StadiumBorder(), color: Color(0xff123456)));
                    expect(theme.textStyle, const TextStyle(fontSize: 18, color: Color(0xffabcdef), fontWeight: FontWeight.w600));
                    expect(theme.textAlign, TextAlign.end);
                    expect(theme.waitDuration, const Duration(microseconds: 120000));
                    expect(theme.showDuration, const Duration(microseconds: 2200000));
                    expect(theme.exitDuration, const Duration(microseconds: 320000));
                    expect(theme.triggerMode, TooltipTriggerMode.tap);
                    expect(data(tester, 'height').height, 35);
                    final policy = raw(tester, 'inherited');
                    expect(policy.hoverDelay, theme.waitDuration);
                    expect(policy.touchDelay, theme.showDuration);
                    expect(policy.dismissDelay, theme.exitDuration);
                    expect(policy.triggerMode, theme.triggerMode);
                    await cleanup(tester);
                  });
                  testWidgets('empty and null nearest scopes replace rather than merge outer fields', (tester) async {
                    await mount(tester);
                    for (final name in ['empty', 'null']) {
                      expect(data(tester, name), const TooltipThemeData());
                      expect(raw(tester, name).hoverDelay, Duration.zero);
                      expect(raw(tester, name).triggerMode, TooltipTriggerMode.longPress);
                      expect(raw(tester, name).semanticsTooltip, 'Message $name');
                    }
                    expect(data(tester, 'whole').exitDuration, const Duration(microseconds: 789));
                    expect(raw(tester, 'whole').dismissDelay, const Duration(microseconds: 789));
                    expect(raw(tester, 'whole').triggerMode, TooltipTriggerMode.manual);
                    await cleanup(tester);
                  });
                  testWidgets('explicit Tooltip policy overrides all inherited timing and flags', (tester) async {
                    await mount(tester);
                    final policy = raw(tester, 'explicit');
                    expect(policy.hoverDelay, const Duration(microseconds: 42000));
                    expect(policy.touchDelay, const Duration(microseconds: 520000));
                    expect(policy.dismissDelay, const Duration(microseconds: 62000));
                    expect(policy.triggerMode, TooltipTriggerMode.longPress);
                    expect(policy.enableFeedback, isFalse);
                    expect(policy.semanticsTooltip, 'Message explicit');
                    await cleanup(tester);
                  });
                  testWidgets('three live State consumers update open overlay without losing Tooltip State', (tester) async {
                    await mount(tester);
                    final original = tester.state<TooltipState>(tip('inherited'));
                    expect(original.ensureTooltipVisible(), isTrue);
                    await tester.pumpAndSettle();
                    final anchor = tester.getRect(find.text('Anchor inherited'));
                    await options(tester, false);
                    final theme = data(tester, 'inherited');
                    expect(theme.preferBelow, isFalse);
                    expect(theme.excludeFromSemantics, isFalse);
                    expect(theme.enableFeedback, isFalse);
                    expect(theme.exitDuration, const Duration(microseconds: 320000));
                    expect(raw(tester, 'inherited').semanticsTooltip, 'Message inherited');
                    expect(tester.state<TooltipState>(tip('inherited')), same(original));
                    expect(tester.getRect(find.text('Anchor inherited')), anchor);
                    expect(find.text('Message inherited', findRichText: true), findsOneWidget);
                    await cleanup(tester);
                  });
                  testWidgets('inherited tap invokes only Tooltip callback and child semantics remain', (tester) async {
                    final semantics = tester.ensureSemantics();
                    final dynamic state = await mount(tester);
                    expect(tester.getSemantics(find.text('Anchor inherited')).getSemanticsData().label, contains('Anchor inherited'));
                    expect(raw(tester, 'inherited').semanticsTooltip, isNull);
                    await tester.tap(find.text('Anchor inherited'));
                    await tester.pump(); await tester.pump(const Duration(milliseconds: 100));
                    expect(state.triggeredEvents, 1);
                    expect(find.text('Message inherited', findRichText: true), findsOneWidget);
                    await cleanup(tester); semantics.dispose();
                  });
                  testWidgets('inherited hover wait and exit timers use exact generated durations', (tester) async {
                    await mount(tester);
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(790, 590));
                    await mouse.moveTo(tester.getCenter(find.text('Anchor inherited')));
                    await tester.pump(); await tester.pump(const Duration(milliseconds: 100));
                    expect(find.text('Message inherited', findRichText: true), findsNothing);
                    await tester.pump(const Duration(milliseconds: 200));
                    expect(find.text('Message inherited', findRichText: true), findsOneWidget);
                    await mouse.moveTo(const Offset(790, 590));
                    await tester.pump(); await tester.pump(const Duration(milliseconds: 200));
                    expect(find.text('Message inherited', findRichText: true), findsOneWidget);
                    await tester.pump(const Duration(milliseconds: 500));
                    await tester.pumpAndSettle();
                    expect(find.text('Message inherited', findRichText: true), findsNothing);
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
                name: tooltip_theme_contract
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
            assertEquals(pass, accepted.ready(), () -> result.status() + "\n" + result.diagnostics()
                    + "\n" + result.symbolEvidence().stream().filter(evidence -> !evidence.accepted()).toList()
                    + "\n" + accepted.diagnostics());
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
