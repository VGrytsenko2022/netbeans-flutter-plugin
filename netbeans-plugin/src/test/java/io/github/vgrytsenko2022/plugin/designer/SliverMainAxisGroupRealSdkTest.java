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
class SliverMainAxisGroupRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");

    @Test void structuralEditsSurviveCandidateGateReopenAndNativeAxisGeometry() throws Exception {
        initialize();
        var empty = open(root(group(GROUP_ID, List.of())), "probe.dart", "// User member is preserved.\nint _userValue() => 73;");
        // Prepare an actual candidate, retaining the empty required slot.
        var first = adapter("First", 40);
        var populated = apply(empty, new AddWidget(place(GROUP_ID, 0), first));
        var firstId = first.id();
        var second = adapter("Second", 60);
        populated = apply(populated, new AddWidget(place(GROUP_ID, 1), group(NESTED_ID, List.of(second))));
        var third = adapter("Third", 80);
        populated = apply(populated, new AddWidget(place(GROUP_ID, 2), third));
        assertAnalysis(empty, populated, "probe.dart", true);
        saveCase("populated", populated);
        populated = reopen(populated, source(populated));

        var reordered = apply(populated, new MoveWidget(third.id(), place(GROUP_ID, 0)));
        assertArrayEquals(populated.current().dartCandidateBytes(), reordered.undo().session().current().dartCandidateBytes());
        assertArrayEquals(reordered.current().dartCandidateBytes(), reordered.undo().session().redo().session().current().dartCandidateBytes());
        assertAnalysis(populated, reordered, "probe.dart", true); saveCase("reordered", reordered);
        reordered = reopen(reordered, source(reordered));

        var moved = apply(reordered, new MoveWidget(firstId, place(NESTED_ID, 1)));
        assertAnalysis(reordered, moved, "probe.dart", true); saveCase("moved", moved);
        moved = reopen(moved, source(moved));
        assertFalse(moved.apply(new MoveWidget(GROUP_ID, place(NESTED_ID, 0))).changed(), "Reject ancestor cycles.");
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Rejected")), Map.of());
        assertFalse(moved.apply(new AddWidget(place(GROUP_ID, 0), box)).changed(), "A box requires SliverToBoxAdapter.");

        var removed = apply(moved, new RemoveWidget(third.id()));
        assertAnalysis(moved, removed, "probe.dart", true); saveCase("removed", removed);
        removed = reopen(removed, source(removed));
        var cleared = apply(removed, new RemoveWidget(NESTED_ID));
        assertAnalysis(removed, cleared, "probe.dart", true); saveCase("cleared", cleared);
        assertTrue(source(cleared).contains("slivers:"));
        saveCase("empty", empty);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("group_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/populated.dart' as populated;
                import '../lib/reordered.dart' as reordered;
                import '../lib/moved.dart' as moved;
                import '../lib/removed.dart' as removed;
                import '../lib/cleared.dart' as cleared;
                import '../lib/empty.dart' as empty;
                void main() {
                  final cases = <String, (Widget, List<String>, double, int)>{
                    'populated': (const populated.Sample(), ['First', 'Second', 'Third'], 180, 2),
                    'reordered': (const reordered.Sample(), ['Third', 'First', 'Second'], 180, 2),
                    'moved': (const moved.Sample(), ['Third', 'Second', 'First'], 180, 2),
                    'removed': (const removed.Sample(), ['Second', 'First'], 100, 2),
                    'cleared': (const cleared.Sample(), [], 0, 1),
                    'empty': (const empty.Sample(), [], 0, 1),
                  };
                  for (final entry in cases.entries) {
                    for (final horizontal in [false, true]) {
                      for (final reverse in [false, true]) {
                        for (final rtl in [false, true]) {
                          testWidgets('${entry.key} h=$horizontal reverse=$reverse rtl=$rtl', (tester) async {
                            await tester.pumpWidget(MaterialApp(home: entry.value.$1));
                            await tester.pumpAndSettle();
                            final group = tester.widget<SliverMainAxisGroup>(find.byType(SliverMainAxisGroup, skipOffstage: false).first);
                            final texts = tester.widgetList<Text>(find.byType(Text, skipOffstage: false))
                                .where((text) => entry.value.$2.contains(text.data)).map((text) => text.data).toList();
                            expect(texts, entry.value.$2);
                            // Reuse the exact generated native group under every inherited viewport configuration.
                            await tester.pumpWidget(MaterialApp(home: Directionality(
                              textDirection: rtl ? TextDirection.rtl : TextDirection.ltr,
                              child: Center(child: SizedBox(width: 300, height: 160, child: CustomScrollView(
                                primary: false, scrollDirection: horizontal ? Axis.horizontal : Axis.vertical,
                                reverse: reverse, slivers: [group]))))));
                            await tester.pumpAndSettle();
                            final groups = find.byType(SliverMainAxisGroup, skipOffstage: false);
                            expect(groups, findsNWidgets(entry.value.$4));
                            final render = tester.renderObject<RenderSliverMainAxisGroup>(groups.first);
                            expect(render.geometry!.scrollExtent, closeTo(entry.value.$3, .01));
                            expect(render.constraints.axis, horizontal ? Axis.horizontal : Axis.vertical);
                            final positions = tester.state<ScrollableState>(find.byType(Scrollable)).position;
                            expect(positions.maxScrollExtent, closeTo(
                              (entry.value.$3 - (horizontal ? 300 : 160)).clamp(0, double.infinity), .01));
                            positions.jumpTo(positions.maxScrollExtent); await tester.pump();
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
        return new WidgetNode(id, SliverMainAxisGroupWidgetPropertySchema.TYPE, Map.of(), Map.of(SLIVERS, new WidgetSlot.ListSlot(children)));
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
                name: sliver_main_axis_group_contract
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
