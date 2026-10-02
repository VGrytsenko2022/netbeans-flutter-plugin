package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.DartCandidateAnalyzer;
import io.github.vgrytsenko2022.dart.DartCandidateWarningPolicy;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.model.PropertyValue.PointerDeviceKindSetValue;
import io.github.vgrytsenko2022.designer.model.PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
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

/** Compiles and invokes every generated callback; exercises actual pointer input and analyzer evidence. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class GestureDetectorRealSdkTest {
    @TempDir Path project;
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.GestureDetector");

    @Test
    void allCallbacksCompileAndInvokeAndGeneratedFiltersControlRealPointerInput() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        Path dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: gesture_detector_contract
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
        assertEquals(58, events.size());
        StringBuilder imports = new StringBuilder("""
                import 'package:flutter/material.dart';
                import 'package:flutter/gestures.dart';
                import 'package:flutter_test/flutter_test.dart';
                """);
        StringBuilder tests = new StringBuilder("void main() {\n");
        for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            String file = "event_" + i + ".dart";
            String alias = "event" + i;
            var wrapper = wrapper();
            var initial = StateBindingRealSdkTest.openRoot(wrapper, file, "  int calls = 0;");
            var session = apply(initial, new CreateEventHandler(wrapper.id(), event.propertyName(), "_handle"));
            session = apply(session, new SetProperty(wrapper.id(), event.propertyName(), reference("_handle")));
            String source = new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8)
                    .replace("// TODO: Handle " + event.propertyName() + ".", "calls++;");
            Files.writeString(lib.resolve(file), source);
            var reopened = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
            assertTrue(reopened.ready(), reopened.diagnostics().toString());
            imports.append("import 'package:gesture_detector_contract/").append(file).append("' as ").append(alias).append(";\n");
            String argument = event.signature().parameters().isEmpty() ? "" : details(event.signature().parameters().getFirst().type());
            tests.append("  testWidgets('").append(event.propertyName()).append(" exact generated handler', (tester) async {\n")
                    .append("    await tester.pumpWidget(const MaterialApp(home: ").append(alias).append(".Sample()));\n")
                    .append("    final state = tester.state<").append(alias).append(".Storage>(find.byType(").append(alias).append(".Sample));\n")
                    .append("    final detector = tester.widget<GestureDetector>(find.descendant(of: find.byType(")
                    .append(alias).append(".Sample), matching: find.byType(GestureDetector)).first);\n")
                    .append("    detector.").append(event.propertyName()).append("!(").append(argument).append(");\n")
                    .append("    expect(state.calls, 1);\n    expect(tester.takeException(), isNull);\n  });\n");
        }

        // Pointer kind evidence must resolve into the pinned SDK, never to a same-named project declaration.
        var detector = wrapper();
        var baseline = StateBindingRealSdkTest.openRoot(detector, "filtered.dart", "  int calls = 0;\n  void _tap() { calls++; }");
        var filtered = apply(baseline, new SetProperty(detector.id(), new PropertyName("onTap"), reference("_tap")));
        filtered = apply(filtered, new SetProperty(detector.id(), new PropertyName("supportedDevices"),
                new PointerDeviceKindSetValue(List.of(PointerDeviceKind.MOUSE))));
        filtered = apply(filtered, new SetProperty(detector.id(), new PropertyName("excludeFromSemantics"), new PropertyValue.BooleanValue(true)));
        filtered = apply(filtered, new SetProperty(detector.id(), new PropertyName("trackpadScrollCausesScale"), new PropertyValue.BooleanValue(true)));
        filtered = apply(filtered, new SetProperty(detector.id(), new PropertyName("trackpadScrollToScaleFactor"),
                new PropertyValue.OffsetValue(java.math.BigDecimal.ZERO, new java.math.BigDecimal("-0.01"))));
        filtered = apply(filtered, new SetProperty(detector.id(), new PropertyName("dragStartBehavior"), new PropertyValue.EnumValue("DragStartBehavior", "down")));
        Path source = lib.resolve("filtered.dart");
        Files.write(source, baseline.current().dartCandidateBytes());
        var pair = filtered.current().preparedPair().orElseThrow();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(baseline.current().fdSnapshot()),
                new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
        var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, source, DartCandidateWarningPolicy.ALLOW, sdk);
        assertEquals(2, ticket.request().symbolProbes().stream().filter(p -> p.expectedSymbolName().equals("PointerDeviceKind")).count(),
                "Model: " + filtered.current().document().root().properties() + "\nCandidate: " + ticket.request().content()
                        + "\nGenerated: " + pair.dartTransition().generation().generated().orElseThrow());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var analysis = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, analysis);
            assertTrue(accepted.ready(), () -> analysis + "\n" + accepted.diagnostics());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
        Files.write(source, filtered.current().dartCandidateBytes());
        var empty = apply(filtered, new SetProperty(detector.id(), new PropertyName("supportedDevices"), new PointerDeviceKindSetValue(List.of())));
        Files.write(lib.resolve("empty.dart"), empty.current().dartCandidateBytes());
        var all = apply(filtered, new SetProperty(detector.id(), new PropertyName("supportedDevices"), new PropertyValue.NullValue()));
        Files.write(lib.resolve("all.dart"), all.current().dartCandidateBytes());
        imports.append("import 'package:gesture_detector_contract/filtered.dart' as filtered;\n")
                .append("import 'package:gesture_detector_contract/empty.dart' as empty;\n")
                .append("import 'package:gesture_detector_contract/all.dart' as all;\n");
        tests.append("""
                  testWidgets('generated device filters and configuration affect real input', (tester) async {
                    await tester.pumpWidget(const MaterialApp(home: filtered.Sample()));
                    final state = tester.state<filtered.Storage>(find.byType(filtered.Sample));
                    final target = find.descendant(of: find.byType(filtered.Sample), matching: find.byType(GestureDetector)).first;
                    final detector = tester.widget<GestureDetector>(target);
                    expect(detector.supportedDevices, {PointerDeviceKind.mouse});
                    expect(detector.excludeFromSemantics, isTrue);
                    expect(detector.trackpadScrollCausesScale, isTrue);
                    expect(detector.trackpadScrollToScaleFactor, const Offset(0, -0.01));
                    expect(detector.dragStartBehavior, DragStartBehavior.down);
                    await tester.tap(target, kind: PointerDeviceKind.touch);
                    expect(state.calls, 0);
                    await tester.tap(target, kind: PointerDeviceKind.mouse);
                    expect(state.calls, 1);
                    await tester.pumpWidget(const MaterialApp(home: empty.Sample()));
                    final emptyState = tester.state<empty.Storage>(find.byType(empty.Sample));
                    await tester.tap(find.byType(empty.Sample), kind: PointerDeviceKind.mouse);
                    expect(emptyState.calls, 0);
                    await tester.pumpWidget(const MaterialApp(home: all.Sample()));
                    final allState = tester.state<all.Storage>(find.byType(all.Sample));
                    await tester.tap(find.byType(all.Sample), kind: PointerDeviceKind.touch);
                    await tester.tap(find.byType(all.Sample), kind: PointerDeviceKind.mouse);
                    expect(allState.calls, 2);
                    expect(tester.takeException(), isNull);
                  });
                """);
        tests.append("}\n");
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("gesture_test.dart"), imports + "\n" + tests);

        // A single real analyzer request proves all 58 typedefs, including types not re-exported by widgets.dart.
        var wrappers = events.stream().map(ignored -> wrapper()).toList();
        var column = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(wrappers)));
        var batchBaseline = StateBindingRealSdkTest.openRoot(column, "batch.dart", "");
        var batch = batchBaseline;
        for (int i = 0; i < events.size(); i++) {
            batch = apply(batch, new CreateEventHandler(wrappers.get(i).id(), events.get(i).propertyName(), "_event" + i));
            batch = apply(batch, new SetProperty(wrappers.get(i).id(), events.get(i).propertyName(), reference("_event" + i)));
        }
        Path batchSource = lib.resolve("batch.dart");
        Files.write(batchSource, batchBaseline.current().dartCandidateBytes());
        var batchPair = batch.current().preparedPair().orElseThrow();
        var batchCurrent = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(batchBaseline.current().fdSnapshot()),
                new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                Optional.of(batchPair.dartTransition().baseline().source()), Optional.of(batchPair.dartTransition().baseline()));
        var batchTicket = PairSaveEvidenceGate.prepareAnalysis(batchCurrent, batchPair, project, batchSource, DartCandidateWarningPolicy.ALLOW, sdk);
        assertEquals(58, batchTicket.request().symbolProbes().stream().filter(p -> p.expectedSymbolName().startsWith("_event")).count());
        var batchOperation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(batchTicket.request());
        try {
            var analysis = batchOperation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(batchTicket, analysis);
            assertTrue(accepted.ready(), () -> analysis + "\n" + accepted.diagnostics());
        } finally { batchOperation.cancel(); }
        assertArrayEquals(batchBaseline.current().dartCandidateBytes(), Files.readAllBytes(batchSource));
        for (String member : List.of("dynamic _bad = (int value) {};", "void _bad(int value) {}")) {
            var badRoot = wrapper();
            var badBaseline = StateBindingRealSdkTest.openRoot(badRoot, "bad.dart",
                    "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + member);
            var bad = apply(badBaseline, new SetProperty(badRoot.id(), new PropertyName("onTap"),
                    new PropertyValue.CallbackValue("_bad")));
            assertInstanceOf(PropertyValue.CallbackValue.class, bad.current().document().root().properties().get(new PropertyName("onTap")));
            Path badSource = lib.resolve("bad.dart");
            Files.write(badSource, badBaseline.current().dartCandidateBytes());
            var badPair = bad.current().preparedPair().orElseThrow();
            var badCurrent = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(badBaseline.current().fdSnapshot()),
                    new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                    Optional.of(badPair.dartTransition().baseline().source()), Optional.of(badPair.dartTransition().baseline()));
            var badTicket = PairSaveEvidenceGate.prepareAnalysis(badCurrent, badPair, project, badSource, DartCandidateWarningPolicy.ALLOW, sdk);
            var badOperation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(badTicket.request());
            try {
                var analysis = badOperation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
                assertFalse(PairSaveEvidenceGate.evaluateAnalysis(badTicket, analysis).ready(), analysis.toString());
                assertTrue(analysis.symbolEvidence().stream().anyMatch(e -> e.probe().expectedSymbolName().equals("_bad")
                        && e.staticTypeEvidence().isPresent() && !e.staticTypeEvidence().orElseThrow().accepted()), analysis.toString());
            } finally { badOperation.cancel(); }
            assertArrayEquals(badBaseline.current().dartCandidateBytes(), Files.readAllBytes(badSource));
        }
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private static String details(String type) {
        return switch (type) {
            case "TapUpDetails" -> "TapUpDetails(kind: PointerDeviceKind.touch)";
            case "TapMoveDetails" -> "TapMoveDetails(kind: PointerDeviceKind.touch, globalPosition: Offset.zero, delta: Offset.zero)";
            case "DragUpdateDetails" -> "DragUpdateDetails(globalPosition: Offset.zero)";
            case "ForcePressDetails" -> "ForcePressDetails(globalPosition: Offset.zero, pressure: 0.5)";
            default -> type + "()";
        };
    }
    private static WidgetNode wrapper() {
        var base = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(), StableId.random());
        return new WidgetNode(base.id(), TYPE, Map.of(new PropertyName("behavior"), new PropertyValue.EnumValue("HitTestBehavior", "opaque")),
                base.slots());
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
