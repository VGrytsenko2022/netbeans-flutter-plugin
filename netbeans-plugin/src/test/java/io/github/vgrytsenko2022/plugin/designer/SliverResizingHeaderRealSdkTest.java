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
class SliverResizingHeaderRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId NESTED_ID=StableId.random();
    private static final SlotName SLIVERS=new SlotName("slivers");

    @Test void nullableSlotsFullHistoryAndNativeSizingPassRealSdk() throws Exception {
        initialize();
        for(int mask=0;mask<8;mask++) {
            var baseline=open(root(adapter("Before",40)),"probe.dart",
                    "// User member is preserved.\nint _userValue() => 73;");
            var prototype=header(mask);
            var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),1),prototype));
            assertAnalysis(baseline,current,"probe.dart",true);saveCase("case"+mask,current);
            assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
            if(mask==7) {
                var saved=reopen(current,source(current));
                for(var name:SliverResizingHeaderWidgetSchema.SLOTS) {
                    var slot=new SlotName(name);
                    var child=((WidgetSlot.SingleSlot)prototype.slots().get(slot)).child().orElseThrow();
                    var replacement=box(96);
                    var changed=apply(saved,new ReplaceSlotChild(NESTED_ID,slot,child.id(),new ReplaceSlotChild.NewSubtree(replacement)));
                    assertArrayEquals(saved.current().dartCandidateBytes(),changed.undo().session().current().dartCandidateBytes());
                    var removed=apply(changed,new RemoveWidget(replacement.id()));
                    var owner=((WidgetSlot.ListSlot)removed.current().document().root().slots().get(SLIVERS))
                            .children().stream().filter(w->w.id().equals(NESTED_ID)).findFirst().orElseThrow();
                    assertTrue(!owner.slots().containsKey(slot)||((WidgetSlot.SingleSlot)owner.slots().get(slot)).child().isEmpty());
                    var moved=apply(removed,new MoveWidget(((WidgetSlot.SingleSlot)prototype.slots().get(
                            new SlotName(name.equals("child")?"minExtentPrototype":"child"))).child().orElseThrow().id(),
                            new WidgetPlacement(NESTED_ID,slot,0)));
                    assertAnalysis(saved,moved,"probe.dart",true);
                    assertArrayEquals(removed.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());
                    assertArrayEquals(moved.current().dartCandidateBytes(),moved.undo().session().redo().session().current().dartCandidateBytes());
                    var edited=apply(saved,new SetProperty(child.id(),new PropertyName("height"),number(101)));
                    assertAnalysis(saved,edited,"probe.dart",true);
                    assertTrue(source(edited).contains("101"));
                }
            }
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("header_test.dart"),"""
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/case0.dart' as c0;
                import '../lib/case1.dart' as c1;
                import '../lib/case2.dart' as c2;
                import '../lib/case3.dart' as c3;
                import '../lib/case4.dart' as c4;
                import '../lib/case5.dart' as c5;
                import '../lib/case6.dart' as c6;
                import '../lib/case7.dart' as c7;
                void main() {
                  final cases=<Widget>[const c0.Sample(),const c1.Sample(),const c2.Sample(),const c3.Sample(),
                    const c4.Sample(),const c5.Sample(),const c6.Sample(),const c7.Sample()];
                  for(var mask=0;mask<8;mask++) {
                    for(final horizontal in [false,true])for(final reverse in [false,true])for(final rtl in [false,true]) {
                      testWidgets('mask=$mask horizontal=$horizontal reverse=$reverse rtl=$rtl',(tester) async {
                        await tester.pumpWidget(MaterialApp(home:cases[mask]));
                        final native=tester.widget<SliverResizingHeader>(find.byType(SliverResizingHeader,skipOffstage:false));
                        expect(native.minExtentPrototype!=null,mask&1!=0);expect(native.maxExtentPrototype!=null,mask&2!=0);
                        expect(native.child!=null,mask&4!=0);
                        await tester.pumpWidget(const SizedBox.shrink());
                        final controller=ScrollController();addTearDown(controller.dispose);
                        await tester.pumpWidget(MaterialApp(home:Directionality(
                          textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                          child:Center(child:SizedBox(width:horizontal?300:220,height:horizontal?220:300,
                            child:CustomScrollView(controller:controller,primary:false,reverse:reverse,
                              scrollDirection:horizontal?Axis.horizontal:Axis.vertical,
                              slivers:[native,const SliverToBoxAdapter(child:SizedBox(width:1000,height:1000))]))))));
                        final render=tester.renderObject<RenderSliver>(find.byType(SliverResizingHeader,skipOffstage:false));
                        final max=mask&2!=0?120.0:mask&4!=0?80.0:0.0;
                        final min=mask&1!=0?24.0:0.0;
                        expect(render.geometry!.scrollExtent,max);
                        expect(render.geometry!.maxScrollObstructionExtent,min);
                        controller.jumpTo(200);await tester.pump();
                        expect(render.geometry!.paintExtent,min);expect(render.geometry!.layoutExtent,0);
                        controller.jumpTo(0);await tester.pump();
                        expect(render.geometry!.scrollExtent,max);
                        expect(tester.takeException(),isNull);
                      });
                    }
                  }
                  testWidgets('native measurement prototypes are excluded from focus',(tester) async {
                    final minFocus=FocusNode(),maxFocus=FocusNode();
                    addTearDown(minFocus.dispose);addTearDown(maxFocus.dispose);
                    await tester.pumpWidget(MaterialApp(home:CustomScrollView(slivers:[
                      SliverResizingHeader(
                        minExtentPrototype:SizedBox(height:24,child:Focus(focusNode:minFocus,autofocus:true,child:const Text('min'))),
                        maxExtentPrototype:SizedBox(height:120,child:Focus(focusNode:maxFocus,autofocus:true,child:const Text('max'))),
                        child:const SizedBox(height:80)),
                    ])));
                    await tester.pump();
                    expect(minFocus.canRequestFocus,isFalse);expect(maxFocus.canRequestFocus,isFalse);
                    expect(minFocus.hasFocus,isFalse);expect(maxFocus.hasFocus,isFalse);
                    expect(tester.takeException(),isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    private static PropertyValue.DoubleValue number(int value) {
        return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value));
    }
    private static WidgetNode box(int extent) {
        return new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"),number(extent),new PropertyName("height"),number(extent)),Map.of());
    }
    private static WidgetNode header(int mask) {
        var slots=new LinkedHashMap<SlotName,WidgetSlot>();
        var extents=List.of(24,120,80);
        for(int i=0;i<3;i++) slots.put(new SlotName(SliverResizingHeaderWidgetSchema.SLOTS.get(i)),
                (mask&(1<<i))==0?WidgetSlot.SingleSlot.empty():WidgetSlot.SingleSlot.of(box(extents.get(i))));
        return new WidgetNode(NESTED_ID,SliverResizingHeaderWidgetSchema.TYPE,Map.of(),slots);
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
                name: sliver_resizing_header_contract
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
