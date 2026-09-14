package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalyzer;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Generated forms and exact analyzer admission, not a handwritten Focus substitute. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class FocusRealSdkTest {
    @TempDir Path project;
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.Focus");
    private static final PropertyName VARIANT = new PropertyName("variant");
    private Path sdk;
    private Path flutter;
    private Path dart;
    private Path lib;

    @Test
    void generatedEventsDeliverFocusAndKeyboardResultsWithExactNullableDefaults() throws Exception {
        initialize();
        var root = focus(Map.of(new PropertyName("autofocus"), new PropertyValue.BooleanValue(true)));
        var baseline = open(root, "sample.dart", """
                  final focusChanges = <bool>[];
                  final keys = <String>[];
                  KeyEventResult result = KeyEventResult.ignored;
                  KeyEventResult legacyResult = KeyEventResult.ignored;
                """);
        var session = baseline;
        var events = WidgetEventCatalog.eventsFor(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow());
        assertEquals(3, events.size());
        for (var event : events) session = apply(session, new CreateEventHandler(root.id(), event.propertyName(), "_" + event.propertyName()));
        assertAnalysis(baseline, session, "sample.dart", true);
        for (var event : events) session = apply(session, new SetProperty(root.id(), event.propertyName(), reference("_" + event.propertyName())));
        assertAnalysis(baseline, session, "sample.dart", true);
        String source = source(session);
        assertTrue(source.contains("KeyEventResult _onKeyEvent(FocusNode node, KeyEvent event)"), source);
        assertTrue(source.contains("KeyEventResult _onKey(FocusNode node, RawKeyEvent event)"), source);
        assertTrue(source.contains("throw UnimplementedError('Implement _onKeyEvent');"), source);
        source = source.replace("// TODO: Handle onFocusChange.", "focusChanges.add(hasFocus);")
                .replace("throw UnimplementedError('Implement _onKeyEvent');", "keys.add('modern'); return result;")
                .replace("throw UnimplementedError('Implement _onKey');", "keys.add('legacy'); return legacyResult;");
        Files.writeString(lib.resolve("sample.dart"), source);
        var reopened = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
        assertTrue(reopened.ready(), reopened.diagnostics().toString());
        var afterReopen = apply(reopened.session().orElseThrow(), new SetProperty(root.id(), new PropertyName("debugLabel"), new PropertyValue.StringValue("Edited after reopen")));
        assertTrue(source(afterReopen).contains("focusChanges.add(hasFocus);"));
        assertTrue(source(afterReopen).contains("keys.add('modern'); return result;"));

        var nil = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("focusNode", "parentNode", "onFocusChange", "onKeyEvent", "onKey", "canRequestFocus",
                "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable", "debugLabel")) {
            nil.put(new PropertyName(name), new PropertyValue.NullValue());
        }
        Files.write(lib.resolve("nulls.dart"), open(focus(nil), "nulls.dart", "").current().dartCandidateBytes());
        var flags = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("autofocus", "canRequestFocus", "skipTraversal", "descendantsAreFocusable",
                "descendantsAreTraversable", "includeSemantics")) flags.put(new PropertyName(name), new PropertyValue.BooleanValue(false));
        flags.put(new PropertyName("debugLabel"), new PropertyValue.StringValue(""));
        Files.write(lib.resolve("flags.dart"), open(focus(flags), "flags.dart", "").current().dartCandidateBytes());
        var stateRoot = focus(Map.of());
        var stateBaseline = open(stateRoot, "bound.dart", """
                  bool _flag = false;
                  bool? _optional = null;
                  void update(bool flag, bool? optional) {
                    setState(() { _flag = flag; _optional = optional; });
                  }
                """);
        var stateCandidate = stateBaseline;
        for (String name : List.of("autofocus", "includeSemantics", "canRequestFocus", "skipTraversal",
                "descendantsAreFocusable", "descendantsAreTraversable")) {
            boolean nullable = !List.of("autofocus", "includeSemantics").contains(name);
            stateCandidate = apply(stateCandidate, new BindPropertyToState(stateRoot.id(), new PropertyName(name),
                    new StatePropertyBinding(nullable ? "_optional" : "_flag", nullable ? StateBinding.Type.NULLABLE_BOOL : StateBinding.Type.BOOL,
                            Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
        }
        assertAnalysis(stateBaseline, stateCandidate, "bound.dart", true);
        Files.write(lib.resolve("bound.dart"), stateCandidate.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("focus_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/services.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:focus_contract/sample.dart' as sample;
                import 'package:focus_contract/nulls.dart' as nulls;
                import 'package:focus_contract/flags.dart' as flags;
                import 'package:focus_contract/bound.dart' as bound;

                Finder region(Type type) => find.descendant(of: find.byType(type), matching: find.byType(Focus)).first;
                void main() {
                  testWidgets('generated focus callback receives acquisition and loss', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: sample.Sample()));
                    await tester.pump();
                    final state = tester.state<sample.Storage>(find.byType(sample.Sample));
                    expect(state.focusChanges, contains(true));
                    FocusManager.instance.primaryFocus!.unfocus();
                    await tester.pump();
                    expect(state.focusChanges.last, isFalse);
                    expect(tester.takeException(), isNull);
                  });
                  for (final result in KeyEventResult.values) {
                   for (final legacyResult in KeyEventResult.values) {
                    testWidgets('generated keyboard results ${result.name}/${legacyResult.name} control propagation', (tester) async {
                      var parentCalls = 0;
                      await tester.pumpWidget(MaterialApp(home: Focus(
                        onKeyEvent: (node, event) { parentCalls++; return KeyEventResult.ignored; },
                        child: const sample.Sample())));
                      await tester.pump();
                      final state = tester.state<sample.Storage>(find.byType(sample.Sample));
                      state.result = result;
                      state.legacyResult = legacyResult;
                      final handled = await tester.sendKeyDownEvent(LogicalKeyboardKey.keyA);
                      await tester.sendKeyUpEvent(LogicalKeyboardKey.keyA);
                      expect(state.keys, contains('modern'));
                      expect(state.keys, contains('legacy'));
                      expect(handled, result == KeyEventResult.handled || legacyResult == KeyEventResult.handled);
                      if (result == KeyEventResult.ignored && legacyResult == KeyEventResult.ignored) {
                        expect(parentCalls, greaterThan(0));
                      } else {
                        expect(parentCalls, 0);
                      }
                      expect(tester.takeException(), isNull);
                    });
                   }
                  }
                  testWidgets('the unsupplied internal node is disposed with Focus', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: nulls.Sample()));
                    final child = find.descendant(of: region(nulls.Sample), matching: find.byType(SizedBox)).first;
                    final node = Focus.of(tester.element(child));
                    await tester.pumpWidget(const SizedBox.shrink());
                    expect(() => node.addListener(() {}), throwsFlutterError);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('all nullable arguments preserve exact SDK defaults', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: nulls.Sample()));
                    final widget = tester.widget<Focus>(region(nulls.Sample));
                    expect(widget.focusNode, isNull);
                    expect(widget.parentNode, isNull);
                    expect(widget.onFocusChange, isNull);
                    expect(widget.onKeyEvent, isNull);
                    expect(widget.onKey, isNull);
                    expect(widget.canRequestFocus, isTrue);
                    expect(widget.skipTraversal, isFalse);
                    expect(widget.descendantsAreFocusable, isTrue);
                    expect(widget.descendantsAreTraversable, isTrue);
                    expect(widget.includeSemantics, isTrue);
                    expect(widget.debugLabel, isNull);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('six explicit false flags and empty debug label remain distinct', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: flags.Sample()));
                    final widget = tester.widget<Focus>(region(flags.Sample));
                    expect(widget.autofocus, isFalse);
                    expect(widget.canRequestFocus, isFalse);
                    expect(widget.skipTraversal, isFalse);
                    expect(widget.descendantsAreFocusable, isFalse);
                    expect(widget.descendantsAreTraversable, isFalse);
                    expect(widget.includeSemantics, isFalse);
                    expect(widget.debugLabel, '');
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('all six generated State consumers rebuild with nullable bool values', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: bound.Sample()));
                    final state = tester.state<bound.Storage>(find.byType(bound.Sample));
                    var widget = tester.widget<Focus>(region(bound.Sample));
                    expect(widget.autofocus, isFalse);
                    expect(widget.includeSemantics, isFalse);
                    expect(widget.canRequestFocus, isTrue);
                    state.update(true, false);
                    await tester.pump();
                    widget = tester.widget<Focus>(region(bound.Sample));
                    expect(widget.autofocus, isTrue);
                    expect(widget.includeSemantics, isTrue);
                    expect(widget.canRequestFocus, isFalse);
                    expect(widget.skipTraversal, isFalse);
                    expect(widget.descendantsAreFocusable, isFalse);
                    expect(widget.descendantsAreTraversable, isFalse);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        for (String member : List.of("dynamic _bad = (Object node, Object event) {};",
                "void _bad(FocusNode node, Object event) {}", "bool _bad(FocusNode node, Object event) => false;",
                "FocusOnKeyEventCallback? get _bad => null;")) {
            var empty = focus(Map.of());
            var before = open(empty, "bad_key.dart", "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + member);
            var after = apply(before, new SetProperty(empty.id(), new PropertyName("onKeyEvent"), reference("_bad")));
            assertAnalysis(before, after, "bad_key.dart", false);
        }
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    @Test
    void externalNodesRetainOwnershipAndNullableReferencesHaveConstructorSpecificProofs() throws Exception {
        initialize();
        Files.writeString(lib.resolve("nodes.dart"), """
                import 'package:flutter/widgets.dart';
                class TrackingNode extends FocusNode {
                  TrackingNode() : super(debugLabel: 'Project node', skipTraversal: true,
                    canRequestFocus: true, descendantsAreFocusable: true, descendantsAreTraversable: false,
                    onKeyEvent: (node, event) => KeyEventResult.handled);
                  bool disposed = false;
                  @override void dispose() { disposed = true; super.dispose(); }
                }
                TrackingNode suppliedNode = TrackingNode();
                FocusNode get nodeGetter => suppliedNode;
                FocusNode nodeFactory() => suppliedNode;
                FocusNode? get nullableNode => suppliedNode;
                FocusNode? get nullableParent => null;
                """);
        var common = new LinkedHashMap<PropertyName, PropertyValue>();
        common.put(new PropertyName("focusNode"), imported("suppliedNode", false));
        common.put(new PropertyName("parentNode"), imported("nullableParent", false));
        common.put(new PropertyName("autofocus"), new PropertyValue.BooleanValue(true));
        common.put(new PropertyName("canRequestFocus"), new PropertyValue.BooleanValue(false));
        common.put(new PropertyName("skipTraversal"), new PropertyValue.BooleanValue(false));
        common.put(new PropertyName("descendantsAreFocusable"), new PropertyValue.BooleanValue(false));
        common.put(new PropertyName("descendantsAreTraversable"), new PropertyValue.BooleanValue(true));
        common.put(new PropertyName("debugLabel"), new PropertyValue.StringValue("Stored standard label"));
        var root = focus(common);
        var baseline = open(root, "external.dart", "");
        var external = apply(baseline, new SetProperty(root.id(), VARIANT, new PropertyValue.StringValue("withExternalFocusNode")));
        assertAnalysis(baseline, external, "external.dart", true);
        assertTrue(source(external).contains("Focus.withExternalFocusNode("), source(external));
        for (String inactive : List.of("canRequestFocus:", "skipTraversal:", "descendantsAreFocusable:", "descendantsAreTraversable:", "debugLabel:")) {
            assertFalse(source(external).contains(inactive), source(external));
        }
        // Reopen the saved external form so this is a new analyzed change, not
        // a return to the original standard baseline (which needs no prepared pair).
        var externalClean = reopen(external);
        var restored = apply(externalClean, new SetProperty(root.id(), VARIANT, new PropertyValue.StringValue("standard")));
        assertTrue(source(restored).contains("debugLabel: 'Stored standard label'"), source(restored));
        assertEquals(root.slots(), restored.current().document().root().slots());
        assertAnalysis(externalClean, restored, "external.dart", true);
        Files.write(lib.resolve("external.dart"), external.current().dartCandidateBytes());
        Files.write(lib.resolve("standard.dart"), restored.current().dartCandidateBytes());

        for (String name : List.of("nodeGetter", "nodeFactory", "nullableNode")) {
            String file = name.toLowerCase() + ".dart";
            var empty = focus(Map.of());
            var before = open(empty, file, "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n");
            var after = apply(before, new SetProperty(empty.id(), new PropertyName("focusNode"), imported(name, name.equals("nodeFactory"))));
            assertAnalysis(before, after, file, true);
            var standardClean = reopen(after);
            var changedVariant = apply(standardClean, new SetProperty(empty.id(), VARIANT, new PropertyValue.StringValue("withExternalFocusNode")));
            assertAnalysis(standardClean, changedVariant, file, !name.equals("nullableNode"));
        }
        for (String member : List.of("dynamic _bad = FocusNode();", "final _bad = Object();")) {
            for (String name : List.of("focusNode", "parentNode")) {
                var empty = focus(Map.of());
                var before = open(empty, "bad_node.dart", "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + member);
                var after = apply(before, new SetProperty(empty.id(), new PropertyName(name), reference("_bad")));
                assertAnalysis(before, after, "bad_node.dart", false);
            }
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("nodes_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:focus_contract/nodes.dart' as nodes;
                import 'package:focus_contract/external.dart' as externalWidget;
                import 'package:focus_contract/standard.dart' as standard;

                void main() {
                  for (final externalMode in [false, true]) {
                    testWidgets('project node ownership and constructor settings external=$externalMode', (tester) async {
                      final oldNode = nodes.suppliedNode;
                      nodes.suppliedNode = nodes.TrackingNode();
                      oldNode.dispose();
                      final node = nodes.suppliedNode;
                      await tester.pumpWidget(MaterialApp(home: externalMode ? const externalWidget.Sample() : const standard.Sample()));
                      await tester.pump();
                      expect(node.canRequestFocus, externalMode);
                      expect(node.skipTraversal, externalMode);
                      expect(node.descendantsAreFocusable, externalMode);
                      expect(node.descendantsAreTraversable, !externalMode);
                      expect(node.debugLabel, 'Project node', reason: 'SDK does not overwrite supplied-node labels during initial mount');
                      expect(node.onKeyEvent, isNotNull);
                      expect(node.disposed, isFalse);
                      await tester.pumpWidget(const SizedBox.shrink());
                      expect(node.disposed, isFalse, reason: 'Focus must never dispose a caller-owned node');
                      node.addListener(() {});
                      expect(tester.takeException(), isNull);
                    });
                  }
                  testWidgets('supplied-node debug label changes on update, not initial mount', (tester) async {
                    nodes.suppliedNode.dispose();
                    nodes.suppliedNode = nodes.TrackingNode();
                    final node = nodes.suppliedNode;
                    await tester.pumpWidget(const MaterialApp(home: standard.Sample()));
                    final generated = tester.widget<Focus>(find.descendant(of: find.byType(standard.Sample), matching: find.byType(Focus)).first);
                    // Reuse the generated Focus as the baseline of one stable SDK element.
                    await tester.pumpWidget(MaterialApp(home: generated));
                    expect(node.debugLabel, 'Project node');
                    await tester.pumpWidget(MaterialApp(home: Focus(focusNode: node,
                      debugLabel: 'Changed label', child: generated.child)));
                    expect(node.debugLabel, 'Changed label');
                    await tester.pumpWidget(const SizedBox.shrink());
                    expect(node.disposed, isFalse);
                    expect(tester.takeException(), isNull);
                  });
                  tearDownAll(() => nodes.suppliedNode.dispose());
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: focus_contract
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
        assertTrue(ticket.request().symbolProbes().stream().anyMatch(probe -> probe.staticTypeProbe().isPresent()), ticket.request().toString());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
            assertEquals(pass, accepted.ready(), () -> analysis + "\n" + accepted.diagnostics());
            if (!pass) assertTrue(analysis.symbolEvidence().stream().anyMatch(e -> e.staticTypeEvidence().isPresent()
                    && !e.staticTypeEvidence().orElseThrow().accepted()), analysis.toString());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
    }

    private static WidgetNode focus(Map<PropertyName, PropertyValue> values) {
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), new PropertyValue.DoubleValue(BigDecimal.valueOf(120)),
                        new PropertyName("height"), new PropertyValue.DoubleValue(BigDecimal.valueOf(80))), Map.of());
        return new WidgetNode(StableId.random(), TYPE, values, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static DesignerCommandSession open(WidgetNode root, String file, String members) throws Exception {
        return StateBindingRealSdkTest.openRoot(root, file, members);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static PropertyValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue imported(String name, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:focus_contract/nodes.dart"), name, Optional.empty(),
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
