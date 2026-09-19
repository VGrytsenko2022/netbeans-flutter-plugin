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
class PinnedHeaderSliverRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId HEADER_ID = StableId.random();
    private static final SlotName SLIVERS = new SlotName("slivers");
    private static final SlotName CHILD = new SlotName("child");

    @Test void optionalChildHistoryAndNativePinnedLayoutPassRealSdk() throws Exception {
        initialize();
        for (boolean empty : List.of(true, false)) {
            var before = adapter("Before", 40);
            var baseline = open(root(before), "probe.dart",
                    "// User member is preserved.\nint _userValue() => 73;");
            var child = box(48);
            var header = new WidgetNode(HEADER_ID, PinnedHeaderSliverWidgetSchema.TYPE, Map.of(),
                    Map.of(CHILD, empty ? WidgetSlot.SingleSlot.empty() : WidgetSlot.SingleSlot.of(child)));
            var current = apply(baseline, new AddWidget(place(baseline.current().document().root().id(), 1), header));
            assertAnalysis(baseline, current, "probe.dart", true);
            saveCase(empty ? "empty" : "filled", current);
            assertArrayEquals(baseline.current().dartCandidateBytes(), current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(), current.undo().session().redo().session().current().dartCandidateBytes());
            var saved = reopen(current, source(current));
            var replacement = box(96);
            var changed = empty
                    ? apply(saved, new AddWidget(new WidgetPlacement(HEADER_ID, CHILD, 0), replacement))
                    : apply(saved, new ReplaceSlotChild(HEADER_ID, CHILD, child.id(), new ReplaceSlotChild.NewSubtree(replacement)));
            assertAnalysis(saved, changed, "probe.dart", true);
            assertArrayEquals(saved.current().dartCandidateBytes(), changed.undo().session().current().dartCandidateBytes());
            var edited = apply(changed, new SetProperty(replacement.id(), new PropertyName("height"), number(101)));
            assertAnalysis(saved, edited, "probe.dart", true);
            saveCase(empty ? "editedEmpty" : "editedFilled", edited);
            var removed = apply(edited, new RemoveWidget(replacement.id()));
            assertAnalysis(saved, removed, "probe.dart", true);
            var movable = ((WidgetSlot.SingleSlot)before.slots().get(CHILD)).child().orElseThrow();
            var moved = apply(removed, new MoveWidget(movable.id(), new WidgetPlacement(HEADER_ID, CHILD, 0)));
            assertAnalysis(saved, moved, "probe.dart", true);
            saveCase(empty ? "movedEmpty" : "movedFilled", moved);
            assertArrayEquals(removed.current().dartCandidateBytes(), moved.undo().session().current().dartCandidateBytes());
            assertArrayEquals(moved.current().dartCandidateBytes(), moved.undo().session().redo().session().current().dartCandidateBytes());
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("header_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/filled.dart' as filled;
                import '../lib/editedEmpty.dart' as editedEmpty;
                import '../lib/editedFilled.dart' as editedFilled;
                import '../lib/movedEmpty.dart' as movedEmpty;
                import '../lib/movedFilled.dart' as movedFilled;
                void main() {
                  final cases=<Widget>[const empty.Sample(),const filled.Sample(),const editedEmpty.Sample(),
                    const editedFilled.Sample(),const movedEmpty.Sample(),const movedFilled.Sample()];
                  final heights=[0.0,48.0,101.0,101.0,40.0,40.0], widths=[0.0,48.0,96.0,96.0,40.0,40.0];
                  for(var i=0;i<cases.length;i++)for(final horizontal in [false,true])for(final reverse in [false,true])for(final rtl in [false,true]) {
                    testWidgets('case=$i h=$horizontal reverse=$reverse rtl=$rtl',(tester) async {
                      await tester.pumpWidget(MaterialApp(home:cases[i]));
                      final native=tester.widget<PinnedHeaderSliver>(find.byType(PinnedHeaderSliver,skipOffstage:false));
                      expect(native.child==null,i==0);
                      await tester.pumpWidget(const SizedBox.shrink());
                      final controller=ScrollController();
                      try {
                        await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                          child:Center(child:SizedBox(width:300,height:200,child:CustomScrollView(controller:controller,primary:false,
                            reverse:reverse,scrollDirection:horizontal?Axis.horizontal:Axis.vertical,
                            slivers:[native,const SliverToBoxAdapter(child:SizedBox(width:1000,height:1000))]))))));
                        final finder=find.byType(PinnedHeaderSliver,skipOffstage:false);
                        final render=tester.renderObject<RenderSliver>(finder);
                        final extent=horizontal?widths[i]:heights[i];
                        expect(render.geometry!.scrollExtent,extent);
                        controller.jumpTo(300);await tester.pump();
                        expect(render.geometry!.paintExtent,extent);
                        expect(render.geometry!.maxScrollObstructionExtent,extent);
                        expect(render.geometry!.layoutExtent,0);
                        controller.jumpTo(0);await tester.pump();
                        expect(render.geometry!.layoutExtent,extent);
                        expect(tester.takeException(),isNull);
                      } finally { await tester.pumpWidget(const SizedBox.shrink());controller.dispose(); }
                    });
                  }
                  testWidgets('native semantics preserve pinned content and dynamic child size',(tester) async {
                    final handle=tester.ensureSemantics();
                    final controller=ScrollController();
                    try {
                      Future<void> show(double extent) => tester.pumpWidget(MaterialApp(home:CustomScrollView(controller:controller,
                        slivers:[PinnedHeaderSliver(child:SizedBox(height:extent,child:const Text('Pinned title'))),
                          const SliverToBoxAdapter(child:SizedBox(height:1000))])));
                      await show(48);controller.jumpTo(200);await tester.pump();
                      expect(find.bySemanticsLabel('Pinned title'),findsOneWidget);
                      final render=tester.renderObject<RenderSliver>(find.byType(PinnedHeaderSliver));
                      await show(96);await tester.pump();
                      expect(identical(render,tester.renderObject(find.byType(PinnedHeaderSliver))),isTrue);
                      expect(render.geometry!.paintExtent,96);
                      expect(render.geometry!.maxScrollObstructionExtent,96);
                      expect(find.bySemanticsLabel('Pinned title'),findsOneWidget);
                      expect(tester.takeException(),isNull);
                    } finally { handle.dispose();await tester.pumpWidget(const SizedBox.shrink());controller.dispose(); }
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }
    private static PropertyValue.DoubleValue number(int value) {
        return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value));
    }
    private static WidgetNode box(int extent) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), number(extent), new PropertyName("height"), number(extent)), Map.of());
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
                name: pinned_header_sliver_contract
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
            if (!pass) assertTrue(result.status() == DartCandidateAnalysisStatus.REJECTED || result.symbolEvidence().stream().anyMatch(evidence -> evidence.staticTypeEvidence()
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

