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

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class SliverLayoutBuilderRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId NESTED_ID=StableId.random();
    private static final SlotName SLIVERS=new SlotName("slivers");

    @Test void fullPairSaveTypedBuildersHistoryAndResponsiveNativeLayout() throws Exception {
        initialize();
        var baseline=open(root(group(StableId.random(),List.of(adapter("Sibling",60)))),"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                SliverLayoutWidgetBuilder get _responsive => (context, constraints) => SliverToBoxAdapter(
                  child: SizedBox(
                    width: constraints.axis == Axis.horizontal ? constraints.crossAxisExtent / 2 : null,
                    height: constraints.axis == Axis.vertical ? constraints.crossAxisExtent / 2 : null,
                    child: const Text('Responsive sliver')));
                SliverLayoutWidgetBuilder _factory() => _responsive;
                Widget _direct(BuildContext context, Object constraints) => const SliverToBoxAdapter(child: SizedBox(width: 80, height: 80));
                WidgetBuilder get _wrongSignature => (context) => const SizedBox.shrink();
                SliverLayoutWidgetBuilder? get _nullable => null;
                dynamic _dynamicResult(BuildContext context, Object constraints) => const SliverToBoxAdapter();
                dynamic get _dynamicReference => _responsive;
                SliverLayoutWidgetBuilder get _wrongRenderType => (context, constraints) => const SizedBox.shrink();
                """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
            .find(SliverLayoutBuilderWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),1),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);saveCase("empty",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("builder"))).changed());
        assertFalse(current.apply(new SetProperty(NESTED_ID,new PropertyName("builder"),new PropertyValue.NullValue())).changed());
        for(String name:List.of("_responsive","_factory","_direct")) {
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("builder"),ref(name,name.equals("_factory"))));
            assertAnalysis(before,current,"probe.dart",true);saveCase(name.substring(1),current);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        }
        for(String bad:List.of("_wrongSignature","_nullable","_dynamicResult","_dynamicReference")) {
            var before=reopen(current,source(current));
            var wrong=apply(before,new SetProperty(NESTED_ID,new PropertyName("builder"),ref(bad,false)));
            assertAnalysis(before,wrong,"probe.dart",false);
        }
        // SDK signature returns Widget: a box-returning callback is statically legal but fails native layout.
        var wrongRender=apply(reopen(current,source(current)),new SetProperty(NESTED_ID,new PropertyName("builder"),ref("_wrongRenderType",false)));
        assertAnalysis(current,wrongRender,"probe.dart",true);saveCase("wrong_render",wrongRender);
        var moved=apply(reopen(current,source(current)),new MoveWidget(NESTED_ID,place(current.current().document().root().id(),0)));
        assertAnalysis(current,moved,"probe.dart",true);
        assertArrayEquals(current.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());
        saveCase("moved",moved);
        var reset=apply(reopen(moved,source(moved)),new SetProperty(NESTED_ID,new PropertyName("builder"),new PropertyValue.StringValue("empty")));
        assertAnalysis(moved,reset,"probe.dart",true);saveCase("reset",reset);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("layout_test.dart"),"""
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/responsive.dart' as responsive;
                import '../lib/factory.dart' as factory_case;
                import '../lib/direct.dart' as direct;
                import '../lib/reset.dart' as reset;
                import '../lib/moved.dart' as moved;
                import '../lib/wrong_render.dart' as wrong_render;
                void main() {
                  final cases=<String,Widget>{
                    'empty': const empty.Sample(), 'reset': const reset.Sample(),
                    'responsive':const responsive.Sample(), 'factory':const factory_case.Sample(),
                    'direct':const direct.Sample(), 'moved':const moved.Sample()};
                  for(final entry in cases.entries) {
                    for(final horizontal in [false,true]) {
                      for(final reverse in [false,true]) {
                        for(final rtl in [false,true]) {
                          testWidgets('${entry.key} horizontal=$horizontal reverse=$reverse rtl=$rtl',(tester) async {
                            await tester.pumpWidget(MaterialApp(home:entry.value));
                            final native=tester.widget<SliverLayoutBuilder>(find.byType(SliverLayoutBuilder,skipOffstage:false));
                            await tester.pumpWidget(const SizedBox.shrink());
                            final controller=ScrollController();addTearDown(controller.dispose);
                            for(final cross in [160.0,240.0]) {
                              await tester.pumpWidget(MaterialApp(home:Directionality(
                                textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                                child:Center(child:SizedBox(width:horizontal?300:cross,height:horizontal?cross:300,
                                  child:CustomScrollView(controller:controller,primary:false,reverse:reverse,
                                    scrollDirection:horizontal?Axis.horizontal:Axis.vertical,
                                    slivers:[native,SliverToBoxAdapter(child:SizedBox(width:1000,height:1000))]))))));
                              final render=tester.renderObject<RenderSliver>(find.byType(SliverLayoutBuilder,skipOffstage:false));
                              expect(render.geometry!.scrollExtent,entry.key=='empty'||entry.key=='reset'?0:entry.key=='direct'||entry.key=='moved'?80:cross/2);
                              expect(render.constraints.crossAxisExtent,cross);
                              controller.jumpTo(20);await tester.pump();
                              expect(render.constraints.scrollOffset,20);
                              expect(tester.takeException(),isNull);
                            }
                          });
                        }
                      }
                    }
                  }
                  testWidgets('SDK requires the builder result to be a sliver, not merely Widget',(tester) async {
                    await tester.pumpWidget(const MaterialApp(home:wrong_render.Sample()));
                    expect(tester.takeException(),isNotNull);
                    await tester.pumpWidget(const SizedBox.shrink());
                    // A bad callback can raise follow-on framework diagnostics; consume only in this negative case.
                    while(tester.takeException()!=null) {}
                  });
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory){
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
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
                name: sliver_layout_builder_contract
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
