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
class SliverPersistentHeaderRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId NESTED_ID=StableId.random();
    private static final SlotName SLIVERS=new SlotName("slivers");

    @Test void fullPairSaveDelegateScaffoldingHistoryTypesAndNativeModes() throws Exception {
        initialize();
        var baseline=open(root(group(StableId.random(),List.of(adapter("Sibling",60)))),"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                SliverPersistentHeaderDelegate get _header => const ProjectHeader();
                SliverPersistentHeaderDelegate _factory() => _header;
                SliverPersistentHeaderDelegate? get _nullable => null;
                dynamic get _dynamic => _header;
                Object get _wrongType => Object();
                """);
        baseline=reopen(baseline,"import 'package:flutter/rendering.dart';\n"+source(baseline)+"""
                class ProjectHeader extends SliverPersistentHeaderDelegate {
                  const ProjectHeader();
                  @override
                  double get minExtent => 40;
                  @override
                  double get maxExtent => 100;
                  @override
                  Widget build(BuildContext context, double shrinkOffset, bool overlapsContent) =>
                      SizedBox.expand(child: Text('Header $shrinkOffset $overlapsContent'));
                  @override
                  bool shouldRebuild(covariant ProjectHeader oldDelegate) => false;
                  @override
                  OverScrollHeaderStretchConfiguration get stretchConfiguration =>
                      OverScrollHeaderStretchConfiguration(stretchTriggerOffset: 60, onStretchTrigger: () async {});
                }
                """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(SliverPersistentHeaderWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),1),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);saveCase("starter",current);
        assertTrue(source(current).contains("class _FlutterDesignerPersistentHeaderDelegate"));
        assertFalse(source(baseline).contains("class _FlutterDesignerPersistentHeaderDelegate"));
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        var duplicate=apply(current,new AddWidget(place(current.current().document().root().id(),2),
                WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(prototype.type()).orElseThrow(),StableId.random())));
        assertEquals(1,source(duplicate).split("class _FlutterDesignerPersistentHeaderDelegate",-1).length-1);
        assertAnalysis(baseline,duplicate,"probe.dart",true);
        assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("delegate"))).changed());
        assertFalse(current.apply(new SetProperty(NESTED_ID,new PropertyName("delegate"),new PropertyValue.NullValue())).changed());
        for(String name:List.of("_header","_factory")) {
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),ref(name,name.equals("_factory"))));
            assertAnalysis(before,current,"probe.dart",true);saveCase(name.substring(1),current);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        }
        for(String bad:List.of("_nullable","_dynamic","_wrongType")) {
            var before=reopen(current,source(current));
            var wrong=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),ref(bad,false)));
            assertAnalysis(before,wrong,"probe.dart",false);
        }
        for(boolean pinned:List.of(false,true))for(boolean floating:List.of(false,true)) {
            var before=reopen(current,source(current));
            var mode=apply(before,new SetProperty(NESTED_ID,new PropertyName("pinned"),new PropertyValue.BooleanValue(pinned)));
            mode=apply(mode,new SetProperty(NESTED_ID,new PropertyName("floating"),new PropertyValue.BooleanValue(floating)));
            assertAnalysis(before,mode,"probe.dart",true);
            saveCase("mode_"+(pinned?1:0)+(floating?1:0),mode);
        }
        var before=reopen(current,source(current));
        var restored=apply(before,new SetProperty(NESTED_ID,new PropertyName("delegate"),SliverPersistentHeaderWidgetPropertySchema.INITIAL_DELEGATE));
        assertEquals(1,source(restored).split("class _FlutterDesignerPersistentHeaderDelegate",-1).length-1);
        assertAnalysis(before,restored,"probe.dart",true);
        // Reopening an edited starter class never regenerates it.
        var userEdited=reopen(restored,source(restored).replace("minExtent => 56","minExtent => 60"));
        var edited=apply(userEdited,new SetProperty(NESTED_ID,new PropertyName("pinned"),new PropertyValue.BooleanValue(true)));
        assertTrue(source(edited).contains("minExtent => 60"));assertAnalysis(userEdited,edited,"probe.dart",true);

        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("header_test.dart"),"""
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/starter.dart' as starter;
                import '../lib/header.dart' as header;
                import '../lib/factory.dart' as factory_case;
                import '../lib/mode_00.dart' as m00;
                import '../lib/mode_01.dart' as m01;
                import '../lib/mode_10.dart' as m10;
                import '../lib/mode_11.dart' as m11;
                void main() {
                  final cases=<String,Widget>{'starter':const starter.Sample(),'header':const header.Sample(),
                    'factory':const factory_case.Sample(),'00':const m00.Sample(),'01':const m01.Sample(),
                    '10':const m10.Sample(),'11':const m11.Sample()};
                  for(final entry in cases.entries) {
                    for(final horizontal in [false,true])for(final reverse in [false,true])for(final rtl in [false,true]) {
                      testWidgets('${entry.key} horizontal=$horizontal reverse=$reverse rtl=$rtl',(tester) async {
                        await tester.pumpWidget(MaterialApp(home:entry.value));
                        final native=tester.widget<SliverPersistentHeader>(find.byType(SliverPersistentHeader,skipOffstage:false));
                        expect(native.delegate.maxExtent,entry.key=='starter'?112:100);
                        if(entry.key.length==2) {
                          expect(native.pinned,entry.key[0]=='1');expect(native.floating,entry.key[1]=='1');
                        }
                        await tester.pumpWidget(const SizedBox.shrink());
                        final controller=ScrollController();addTearDown(controller.dispose);
                        await tester.pumpWidget(MaterialApp(home:Directionality(
                          textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                          child:Center(child:SizedBox(width:horizontal?300:220,height:horizontal?220:300,
                            child:CustomScrollView(controller:controller,primary:false,reverse:reverse,
                              scrollDirection:horizontal?Axis.horizontal:Axis.vertical,
                              slivers:[native,const SliverToBoxAdapter(child:SizedBox(width:1000,height:1000))]))))));
                        final render=tester.renderObject<RenderSliver>(find.byType(SliverPersistentHeader,skipOffstage:false));
                        expect(render.geometry!.scrollExtent,entry.key=='starter'?112:100);
                        controller.jumpTo(150);await tester.pump();
                        expect(render.geometry!.paintExtent,native.pinned?native.delegate.minExtent:0);
                        final gesture=await tester.startGesture(tester.getCenter(find.byType(CustomScrollView)));
                        final delta=switch(controller.position.axisDirection) {
                          AxisDirection.down => const Offset(0,40), AxisDirection.up => const Offset(0,-40),
                          AxisDirection.right => const Offset(40,0), AxisDirection.left => const Offset(-40,0)};
                        await gesture.moveBy(delta);await tester.pump();
                        if(native.floating) expect(render.geometry!.paintExtent,greaterThan(0));
                        await gesture.cancel();await tester.pumpAndSettle();
                        expect(tester.takeException(),isNull);
                      });
                    }
                  }
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
                name: sliver_persistent_header_contract
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
