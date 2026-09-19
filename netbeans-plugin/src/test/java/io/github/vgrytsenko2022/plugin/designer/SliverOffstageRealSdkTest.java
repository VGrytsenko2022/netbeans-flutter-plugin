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

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class SliverOffstageRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");

    @Test void AllVisibilityBranchesPassAnalyzerReopenHistoryAndNativeSdk() throws Exception {
        initialize();
        var baseline = open(root(group(GROUP_ID, List.of(adapter("Sibling", 60)))),
                "probe.dart", "// User member is preserved.\nint _userValue() => 73;");
        var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(SliverOffstageWidgetPropertySchema.TYPE).orElseThrow(), NESTED_ID);
        var empty = apply(baseline, new AddWidget(place(baseline.current().document().root().id(), 1), prototype));
        assertAnalysis(baseline, empty, "probe.dart", true); saveCase("empty", empty);
        assertArrayEquals(baseline.current().dartCandidateBytes(), empty.undo().session().current().dartCandidateBytes());
        assertArrayEquals(empty.current().dartCandidateBytes(), empty.undo().session().redo().session().current().dartCandidateBytes());
        empty = reopen(empty, source(empty));
        var child = adapter("Offstage body", 40);
        var current = apply(empty, new AddWidget(new WidgetPlacement(NESTED_ID, new SlotName("sliver"), 0), child));
        assertAnalysis(empty, current, "probe.dart", true); saveCase("defaults", current); current = reopen(current, source(current));
        for (boolean offstage : List.of(false,true)) {
            var changed = apply(current, new SetProperty(NESTED_ID, new PropertyName("offstage"), new PropertyValue.BooleanValue(offstage)));
            assertAnalysis(current, changed, "probe.dart", true);
            saveCase(offstage ? "blocked" : "interactive", changed); current = reopen(changed, source(changed));
        }
        for (String name : List.of("offstage")) {
            var reset = apply(current, new ResetProperty(NESTED_ID, new PropertyName(name)));
            assertAnalysis(current, reset, "probe.dart", true); current = reopen(reset, source(reset));
        }
        assertFalse(source(current).contains("offstage:"));
        saveCase("reset", current);
        var moved = apply(current, new MoveWidget(child.id(), place(current.current().document().root().id(), 2)));
        assertAnalysis(current, moved, "probe.dart", true);
        assertArrayEquals(current.current().dartCandidateBytes(), moved.undo().session().current().dartCandidateBytes());
        saveCase("moved", moved);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("offstage_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/defaults.dart' as defaults;
                import '../lib/interactive.dart' as interactive;
                import '../lib/blocked.dart' as blocked;
                import '../lib/moved.dart' as moved;
                import '../lib/reset.dart' as reset;
                void main() {
                  final cases = <String, (Widget, bool, bool)>{
                    'empty': (const empty.Sample(), true, true),
                    'defaults': (const defaults.Sample(), true, false),
                    'interactive': (const interactive.Sample(), false, false),
                    'blocked': (const blocked.Sample(), true, false),
                    'reset': (const reset.Sample(), true, false),
                    'moved': (const moved.Sample(), true, true),
                  };
                  for (final entry in cases.entries) {
                    for (final horizontal in [false, true]) {
                      for (final reverse in [false, true]) {
                        for (final rtl in [false, true]) {
                          testWidgets('${entry.key} horizontal=$horizontal reverse=$reverse rtl=$rtl', (tester) async {
                            await tester.pumpWidget(MaterialApp(home: entry.value.$1));
                            await tester.pumpAndSettle();
                            final sliver = tester.widget<SliverOffstage>(find.byType(SliverOffstage, skipOffstage: false));
                            expect(sliver.offstage, entry.value.$2);
                            if (entry.value.$3) expect((sliver.child! as SliverToBoxAdapter).child, isNull);
                            await tester.pumpWidget(MaterialApp(home: Directionality(
                              textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
                              child: Center(child: SizedBox(width: 300, height: 160, child: CustomScrollView(
                                primary: false, scrollDirection: horizontal ? Axis.horizontal : Axis.vertical,
                                reverse: reverse, slivers: [sliver]))))));
                            await tester.pumpAndSettle();
                            final render = tester.renderObject<RenderSliverOffstage>(find.byType(SliverOffstage, skipOffstage: false));
                            expect(render.geometry!.scrollExtent, entry.value.$2 || entry.value.$3 ? 0 : 40);
                            expect(render.geometry!.paintExtent, entry.value.$2 || entry.value.$3 ? 0 : 40);
                            final children = <RenderObject>[]; render.visitChildrenForSemantics(children.add);
                            expect(children.isNotEmpty, !entry.value.$2);
                            expect(render.child!.geometry!.scrollExtent, entry.value.$3 ? 0 : 40);
                            if (entry.value.$2) expect(render.hitTest(SliverHitTestResult(),mainAxisPosition:5,crossAxisPosition:5),false);
                            expect(tester.takeException(), isNull);
                          });
                        }
                      }
                    }
                  }
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }
    private void saveCase(String name, DesignerCommandSession session) throws Exception {
        var reopened = reopen(session, source(session));
        assertEquals(session.current().fdSnapshot(), reopened.current().fdSnapshot());
        assertArrayEquals(session.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("// User member is preserved."));
        assertTrue(source(reopened).contains("int _userValue() => 73;"));
        Files.write(lib.resolve(name + ".dart"), reopened.current().dartCandidateBytes());
    }
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, SLIVERS, index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, SliverCrossAxisGroupWidgetPropertySchema.TYPE, Map.of(), Map.of(SLIVERS, new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode adapter(String label, int extent) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(label)), Map.of());
        var size = new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(extent));
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), size, new PropertyName("height"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"), Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(box)));
    }
    private static WidgetNode root(WidgetNode group) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"),
                Map.of(new PropertyName("primary"), new PropertyValue.BooleanValue(false)),
                Map.of(SLIVERS, new WidgetSlot.ListSlot(List.of(group))));
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sliver_offstage_contract
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
        assertFalse(ticket.request().symbolProbes().isEmpty());
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

