package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalyzer;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real SDK coverage of all Listener typedefs, generated bodies and raw pointer delivery. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class ListenerRealSdkTest {
    @TempDir Path project;
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.Listener");

    @Test
    void everyCallbackCompilesDispatchesAndRequiresExactStaticTypeEvidence() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: listener_contract
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
        Path lib = Files.createDirectories(project.resolve("lib"));
        run(List.of(flutter.toString(), "pub", "get", "--offline"), "pub-get.log");
        var definition = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        var events = WidgetEventCatalog.eventsFor(definition);
        assertEquals(9, events.size());
        var root = wrapper();
        var baseline = StateBindingRealSdkTest.openRoot(root, "sample.dart", "  final calls = <String, int>{};");
        var session = baseline;
        for (var event : events) {
            session = apply(session, new CreateEventHandler(root.id(), event.propertyName(), "_" + event.propertyName()));
        }
        // The persisted local shorthand and typed references must both pass the real analyzer.
        assertAnalysis(baseline, session, sdk, dart, "sample.dart", 9, true);
        for (var event : events) {
            session = apply(session, new SetProperty(root.id(), event.propertyName(), reference("_" + event.propertyName())));
        }
        assertAnalysis(baseline, session, sdk, dart, "sample.dart", 9, true);
        String source = new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        for (var event : events) {
            String name = event.propertyName().value();
            source = source.replace("// TODO: Handle " + name + ".",
                    "calls.update('" + name + "', (value) => value + 1, ifAbsent: () => 1);");
        }
        Files.writeString(lib.resolve("sample.dart"), source);
        var reopened = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8),
                BuiltInWidgetCatalog.getDefault());
        assertTrue(reopened.ready(), reopened.diagnostics().toString());

        StringBuilder tests = new StringBuilder("""
                import 'package:flutter/material.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:listener_contract/sample.dart' as sample;
                void main() {
                """);
        for (var event : events) {
            String name = event.propertyName().value();
            String argument = event.signature().parameters().getFirst().type();
            if (argument.equals("PointerSignalEvent")) argument = "PointerScrollEvent";
            tests.append("  testWidgets('").append(name).append(" generated callback', (tester) async {\n")
                    .append("    await tester.pumpWidget(const MaterialApp(home: sample.Sample()));\n")
                    .append("    final state = tester.state<sample.Storage>(find.byType(sample.Sample));\n")
                    .append("    final listener = tester.widget<Listener>(find.descendant(of: find.byType(sample.Sample), matching: find.byType(Listener)).first);\n")
                    .append("    listener.").append(name).append("!(const ").append(argument).append("());\n")
                    .append("    expect(state.calls, {'").append(name).append("': 1});\n")
                    .append("    expect(tester.takeException(), isNull);\n  });\n");
        }
        tests.append("""
                  testWidgets('all nine raw render events reach generated handlers', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: sample.Sample()));
                    final state = tester.state<sample.Storage>(find.byType(sample.Sample));
                    final target = find.descendant(of: find.byType(sample.Sample), matching: find.byType(Listener)).first;
                    final render = tester.renderObject<RenderPointerListener>(target);
                    final entry = BoxHitTestEntry(render, Offset.zero);
                    for (final event in <PointerEvent>[
                      const PointerDownEvent(), const PointerMoveEvent(), const PointerUpEvent(),
                      const PointerHoverEvent(), const PointerCancelEvent(), const PointerPanZoomStartEvent(),
                      const PointerPanZoomUpdateEvent(), const PointerPanZoomEndEvent(), const PointerScrollEvent(),
                    ]) {
                      render.handleEvent(event, entry);
                    }
                    expect(state.calls.keys.toSet(), {
                      'onPointerDown', 'onPointerMove', 'onPointerUp', 'onPointerHover', 'onPointerCancel',
                      'onPointerPanZoomStart', 'onPointerPanZoomUpdate', 'onPointerPanZoomEnd', 'onPointerSignal',
                    });
                    expect(state.calls.values, everyElement(1));
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('opaque empty Listener participates in actual hit testing', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: sample.Sample()));
                    final state = tester.state<sample.Storage>(find.byType(sample.Sample));
                    final target = find.descendant(of: find.byType(sample.Sample), matching: find.byType(Listener)).first;
                    expect(tester.widget<Listener>(target).behavior, HitTestBehavior.opaque);
                    await tester.tap(target);
                    expect(state.calls['onPointerDown'], 1);
                    expect(state.calls['onPointerUp'], 1);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("listener_test.dart"), tests);

        for (String member : List.of("dynamic _bad = (int value) {};", "void _bad(int value) {}")) {
            var badRoot = wrapper();
            var badBaseline = StateBindingRealSdkTest.openRoot(badRoot, "bad.dart",
                    "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + member);
            var bad = apply(badBaseline, new SetProperty(badRoot.id(), new PropertyName("onPointerHover"),
                    new PropertyValue.CallbackValue("_bad")));
            assertAnalysis(badBaseline, bad, sdk, dart, "bad.dart", 1, false);
        }
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void assertAnalysis(DesignerCommandSession baseline, DesignerCommandSession candidate,
            Path sdk, Path dart, String file, int callbackCount, boolean pass) throws Exception {
        Path source = project.resolve("lib").resolve(file);
        Files.write(source, baseline.current().dartCandidateBytes());
        var pair = candidate.current().preparedPair().orElseThrow();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(baseline.current().fdSnapshot()),
                new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
        var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, source, DartCandidateWarningPolicy.ALLOW, sdk);
        assertEquals(callbackCount, ticket.request().symbolProbes().stream()
                .filter(probe -> probe.expectedSymbolName().startsWith("_")).count(), ticket.request().toString());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
            assertEquals(pass, accepted.ready(), () -> analysis + "\n" + accepted.diagnostics());
            if (!pass) assertTrue(analysis.symbolEvidence().stream().anyMatch(e -> e.probe().expectedSymbolName().equals("_bad")
                    && e.staticTypeEvidence().isPresent() && !e.staticTypeEvidence().orElseThrow().accepted()), analysis.toString());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
    }

    private static WidgetNode wrapper() {
        var base = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(), StableId.random());
        return new WidgetNode(base.id(), TYPE, Map.of(new PropertyName("behavior"),
                new PropertyValue.EnumValue("HitTestBehavior", "opaque")), base.slots());
    }
    private static PropertyValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertTrue(result.changed(), result.diagnostics().toString());
        return result.session();
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
