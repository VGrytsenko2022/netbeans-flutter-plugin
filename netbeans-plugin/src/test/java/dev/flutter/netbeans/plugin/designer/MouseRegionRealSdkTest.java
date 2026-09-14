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

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class MouseRegionRealSdkTest {
    @TempDir Path project;
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.MouseRegion");

    @Test
    void callbacksCursorBranchesHoverAndUnmountRespectTheRealSdk() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: mouse_region_contract
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
        assertEquals(3, events.size());
        var root = wrapper(Map.of(new PropertyName("cursor"), new PropertyValue.StringValue("click"),
                new PropertyName("opaque"), new PropertyValue.BooleanValue(false),
                new PropertyName("hitTestBehavior"), new PropertyValue.EnumValue("HitTestBehavior", "opaque")));
        var baseline = StateBindingRealSdkTest.openRoot(root, "sample.dart", "  final calls = <String, int>{};");
        var session = baseline;
        for (var event : events) session = apply(session, new CreateEventHandler(root.id(), event.propertyName(), "_" + event.propertyName()));
        assertAnalysis(baseline, session, sdk, dart, "sample.dart", true);
        for (var event : events) session = apply(session, new SetProperty(root.id(), event.propertyName(), reference("_" + event.propertyName())));
        assertAnalysis(baseline, session, sdk, dart, "sample.dart", true);
        String source = new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        for (var event : events) {
            String name = event.propertyName().value();
            source = source.replace("// TODO: Handle " + name + ".",
                    "calls.update('" + name + "', (value) => value + 1, ifAbsent: () => 1);");
        }
        Files.writeString(lib.resolve("sample.dart"), source);
        var reopened = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
        assertTrue(reopened.ready(), reopened.diagnostics().toString());
        StringBuilder imports = new StringBuilder("""
                import 'package:flutter/material.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter/services.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:mouse_region_contract/sample.dart' as sample;
                """);
        StringBuilder tests = new StringBuilder("void main() {\n");
        for (var event : events) {
            String name = event.propertyName().value();
            tests.append("  testWidgets('").append(name).append(" generated callback', (tester) async {\n")
                    .append("    await tester.pumpWidget(const MaterialApp(home: sample.Sample()));\n")
                    .append("    final state = tester.state<sample.Storage>(find.byType(sample.Sample));\n")
                    .append("    final region = tester.widget<MouseRegion>(find.descendant(of: find.byType(sample.Sample), matching: find.byType(MouseRegion)).first);\n")
                    .append("    region.").append(name).append("!(const ").append(event.signature().parameters().getFirst().type()).append("());\n")
                    .append("    expect(state.calls, {'").append(name).append("': 1});\n")
                    .append("    expect(tester.takeException(), isNull);\n  });\n");
        }
        tests.append("""
                  testWidgets('actual movement delivers enter hover exit and configuration', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: Align(alignment: Alignment.topLeft,
                      child: SizedBox(width: 120, height: 100, child: sample.Sample()))));
                    final state = tester.state<sample.Storage>(find.byType(sample.Sample));
                    final region = tester.widget<MouseRegion>(find.descendant(of: find.byType(sample.Sample), matching: find.byType(MouseRegion)).first);
                    expect(region.cursor, SystemMouseCursors.click);
                    expect(region.opaque, isFalse);
                    expect(region.hitTestBehavior, HitTestBehavior.opaque);
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(200, 200));
                    await mouse.moveTo(const Offset(20, 20));
                    await tester.pump();
                    await mouse.moveTo(const Offset(25, 25));
                    await tester.pump();
                    await mouse.moveTo(const Offset(200, 200));
                    await tester.pump();
                    expect(state.calls['onEnter'], 1);
                    expect(state.calls['onHover'], greaterThanOrEqualTo(1));
                    expect(state.calls['onExit'], 1);
                    await mouse.removePointer();
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('unmounting a hovered region does not synthesize onExit', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: Align(alignment: Alignment.topLeft,
                      child: SizedBox(width: 120, height: 100, child: sample.Sample()))));
                    final state = tester.state<sample.Storage>(find.byType(sample.Sample));
                    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
                    await mouse.addPointer(location: const Offset(200, 200));
                    await mouse.moveTo(const Offset(20, 20));
                    await tester.pump();
                    expect(state.calls['onEnter'], 1);
                    await tester.pumpWidget(const SizedBox.shrink());
                    expect(state.calls['onExit'], isNull);
                    await mouse.removePointer();
                    expect(state.calls['onExit'], isNull);
                    expect(tester.takeException(), isNull);
                  });
                """);
        var presets = DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
        assertEquals(41, presets.size());
        for (int i = 0; i < presets.size(); i++) {
            String preset = presets.get(i), file = "preset_" + i + ".dart", alias = "preset" + i;
            var model = StateBindingRealSdkTest.openRoot(wrapper(Map.of(new PropertyName("cursor"), new PropertyValue.StringValue(preset))), file, "");
            Files.write(lib.resolve(file), model.current().dartCandidateBytes());
            imports.append("import 'package:mouse_region_contract/").append(file).append("' as ").append(alias).append(";\n");
            String owner = List.of("defer", "uncontrolled").contains(preset) ? "MouseCursor"
                    : List.of("clickable", "adaptiveClickable", "textable").contains(preset) ? "WidgetStateMouseCursor" : "SystemMouseCursors";
            tests.append("  testWidgets('cursor preset ").append(preset).append("', (tester) async {\n")
                    .append("    await tester.pumpWidget(const MaterialApp(home: ").append(alias).append(".Sample()));\n")
                    .append("    final region = tester.widget<MouseRegion>(find.descendant(of: find.byType(").append(alias)
                    .append(".Sample), matching: find.byType(MouseRegion)).first);\n")
                    .append("    expect(region.cursor, ").append(owner).append('.').append(preset).append(");\n")
                    .append("    expect(region.opaque, isTrue);\n    expect(region.hitTestBehavior, isNull);\n")
                    .append("    expect(tester.takeException(), isNull);\n  });\n");
        }
        Files.writeString(lib.resolve("cursors.dart"), """
                import 'package:flutter/services.dart';
                class ContractCursor extends MouseCursor {
                  const ContractCursor();
                  @override String get debugDescription => 'Contract cursor';
                  @override MouseCursorSession createSession(int device) => ContractSession(this, device);
                }
                class ContractSession extends MouseCursorSession {
                  ContractSession(super.cursor, super.device);
                  @override Future<void> activate() async {}
                  @override void dispose() {}
                }
                const customCursor = ContractCursor();
                MouseCursor buildCursor() => const ContractCursor();
                """);
        imports.append("import 'package:mouse_region_contract/cursors.dart' as cursors;\n");
        for (boolean factory : List.of(false, true)) {
            String file = factory ? "factory.dart" : "custom.dart", alias = factory ? "cursorFactory" : "custom";
            var empty = wrapper(Map.of());
            var before = StateBindingRealSdkTest.openRoot(empty, file, "");
            var value = new PropertyValue.DartObjectReferenceValue(Optional.of("package:mouse_region_contract/cursors.dart"),
                    factory ? "buildCursor" : "customCursor", Optional.empty(), factory
                    ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                    : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
            var after = apply(before, new SetProperty(empty.id(), new PropertyName("cursor"), value));
            assertAnalysis(before, after, sdk, dart, file, true);
            Files.write(lib.resolve(file), after.current().dartCandidateBytes());
            imports.append("import 'package:mouse_region_contract/").append(file).append("' as ").append(alias).append(";\n");
            tests.append("  testWidgets('custom cursor ").append(alias).append("', (tester) async {\n")
                    .append("    await tester.pumpWidget(const MaterialApp(home: ").append(alias).append(".Sample()));\n")
                    .append("    final region = tester.widget<MouseRegion>(find.descendant(of: find.byType(").append(alias)
                    .append(".Sample), matching: find.byType(MouseRegion)).first);\n")
                    .append("    expect(region.cursor, isA<cursors.ContractCursor>());\n")
                    .append("    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);\n")
                    .append("    await mouse.addPointer(location: const Offset(20, 20));\n    await tester.pump();\n")
                    .append("    await mouse.removePointer();\n    expect(tester.takeException(), isNull);\n  });\n");
        }
        tests.append("}\n");
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("mouse_region_test.dart"), imports + "\n" + tests);
        for (String member : List.of("dynamic _bad = (int value) {};", "void _bad(int value) {}")) {
            var empty = wrapper(Map.of());
            var before = StateBindingRealSdkTest.openRoot(empty, "bad.dart",
                    "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + member);
            var after = apply(before, new SetProperty(empty.id(), new PropertyName("onEnter"), new PropertyValue.CallbackValue("_bad")));
            assertAnalysis(before, after, sdk, dart, "bad.dart", false);
        }
        for (String member : List.of("dynamic _bad = Object();", "final _bad = Object();",
                "MouseCursor? get _bad => SystemMouseCursors.click;")) {
            var empty = wrapper(Map.of());
            var before = StateBindingRealSdkTest.openRoot(empty, "bad_cursor.dart",
                    "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + member);
            var after = apply(before, new SetProperty(empty.id(), new PropertyName("cursor"), reference("_bad")));
            assertAnalysis(before, after, sdk, dart, "bad_cursor.dart", false);
        }
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void assertAnalysis(DesignerCommandSession baseline, DesignerCommandSession candidate,
            Path sdk, Path dart, String file, boolean pass) throws Exception {
        Path source = project.resolve("lib").resolve(file);
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
            if (!pass) assertTrue(analysis.symbolEvidence().stream().anyMatch(e -> e.probe().expectedSymbolName().equals("_bad")
                    && e.staticTypeEvidence().isPresent() && !e.staticTypeEvidence().orElseThrow().accepted()), analysis.toString());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
    }
    private static WidgetNode wrapper(Map<PropertyName, PropertyValue> values) {
        var base = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(), StableId.random());
        return new WidgetNode(base.id(), TYPE, values, base.slots());
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
