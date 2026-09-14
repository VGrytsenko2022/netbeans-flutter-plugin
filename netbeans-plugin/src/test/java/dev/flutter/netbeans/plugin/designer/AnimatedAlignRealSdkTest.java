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
class AnimatedAlignRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void generatedFormsExactProofReopenHistoryAndNativeTransitions() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",60))),"probe.dart",
            "// User member is preserved.\nint _userValue() => 73;\nDuration _customDuration() => const Duration(milliseconds: 25);\nCurve get _customCurve => Curves.easeIn;\nAlignmentGeometry _customAlignment() => const AlignmentDirectional(-0.5, 0.5);\nvoid _handleEnd() {}");
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedAlignWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var empty=apply(baseline,new AddWidget(place(GROUP_ID,1),prototype));
        assertAnalysis(baseline,empty,"probe.dart",true);saveCase("empty",empty);
        assertArrayEquals(baseline.current().dartCandidateBytes(),empty.undo().session().current().dartCandidateBytes());
        assertArrayEquals(empty.current().dartCandidateBytes(),empty.undo().session().redo().session().current().dartCandidateBytes());
        empty=reopen(empty,source(empty));
        var child=adapter("Aligned body",40);
        var current=apply(empty,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),child));
        assertAnalysis(empty,current,"probe.dart",true);saveCase("center",current);current=reopen(current,source(current));
        for(boolean directional:List.of(false,true)){
            var value=new PropertyValue.AlignmentGeometryValue(directional?PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL:
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,java.math.BigDecimal.ONE.negate(),java.math.BigDecimal.ONE);
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("alignment"),value));
            assertAnalysis(current,changed,"probe.dart",true);saveCase(directional?"directional":"physical",changed);current=reopen(changed,source(changed));
        }
        for(var entry:Map.<String,PropertyValue>of("factors",new PropertyValue.DoubleValue(new java.math.BigDecimal("2")),
            "zero",new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),"nulls",new PropertyValue.NullValue()).entrySet()){
            var changed=apply(current,new PatchProperties(NESTED_ID,List.of(
                new PatchProperties.SetPatch(new PropertyName("widthFactor"),entry.getValue()),
                new PatchProperties.SetPatch(new PropertyName("heightFactor"),entry.getValue()))));
            assertAnalysis(current,changed,"probe.dart",true);saveCase(entry.getKey(),changed);current=reopen(changed,source(changed));
        }
        // Clear each explicit factor atomically while retaining the required alignment/duration.
        var reset=apply(current,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("widthFactor")),new PatchProperties.ResetPatch(new PropertyName("heightFactor")))));
        assertAnalysis(current,reset,"probe.dart",true);saveCase("omitted",reset);reset=reopen(reset,source(reset));
        var typed=apply(reset,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("alignment"),ref("_customAlignment",true)),
            new PatchProperties.SetPatch(new PropertyName("curve"),ref("_customCurve",false)),
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_customDuration",true)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_handleEnd",false)))));
        assertAnalysis(reset,typed,"probe.dart",true);saveCase("typed",typed);
        assertArrayEquals(reset.current().dartCandidateBytes(),typed.undo().session().current().dartCandidateBytes());
        typed=reopen(typed,source(typed));
        var wrong=apply(typed,new SetProperty(NESTED_ID,new PropertyName("alignment"),ref("_customDuration",true)));
        assertAnalysis(typed,wrong,"probe.dart",false);
        var cleared=apply(typed,new SetProperty(NESTED_ID,new PropertyName("onEnd"),new PropertyValue.NullValue()));
        assertAnalysis(typed,cleared,"probe.dart",true);saveCase("cleared",cleared);
        var moved=apply(reset,new MoveWidget(child.id(),place(GROUP_ID,2)));
        assertAnalysis(reset,moved,"probe.dart",true);saveCase("moved",moved);
        assertArrayEquals(reset.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());
        assertFalse(reset.apply(new ResetProperty(NESTED_ID,new PropertyName("alignment"))).changed());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("alignment_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/empty.dart' as empty;
            import '../lib/center.dart' as center;
            import '../lib/physical.dart' as physical;
            import '../lib/directional.dart' as directional;
            import '../lib/factors.dart' as factors;
            import '../lib/zero.dart' as zero;
            import '../lib/nulls.dart' as nulls;
            import '../lib/omitted.dart' as omitted;
            import '../lib/typed.dart' as typed;
            import '../lib/cleared.dart' as cleared;
            import '../lib/moved.dart' as moved;
            void main(){
              final cases=<String,(Widget,AlignmentGeometry,double?,bool)>{
                'empty':(const empty.Sample(),Alignment.center,null,true),
                'center':(const center.Sample(),Alignment.center,null,false),
                'physical':(const physical.Sample(),Alignment.bottomLeft,null,false),
                'directional':(const directional.Sample(),AlignmentDirectional.bottomStart,null,false),
                'factors':(const factors.Sample(),AlignmentDirectional.bottomStart,2,false),
                'zero':(const zero.Sample(),AlignmentDirectional.bottomStart,0,false),
                'nulls':(const nulls.Sample(),AlignmentDirectional.bottomStart,null,false),
                'omitted':(const omitted.Sample(),AlignmentDirectional.bottomStart,null,false),
                'moved':(const moved.Sample(),AlignmentDirectional.bottomStart,null,true),
              };
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(
                    textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value.$1)));
                  await tester.pumpAndSettle();
                  final native=tester.widget<AnimatedAlign>(find.byType(AnimatedAlign));
                  expect(native.alignment,e.value.$2);expect(native.widthFactor,e.value.$3);expect(native.heightFactor,e.value.$3);
                  expect(native.child==null,e.value.$4);expect(native.duration,const Duration(milliseconds:300));expect(native.curve,Curves.linear);
                  expect(tester.takeException(),isNull);
                });
              }
              testWidgets('generated typed bindings preserve exact runtime objects',(tester)async{
                await tester.pumpWidget(const MaterialApp(home:typed.Sample()));
                final native=tester.widget<AnimatedAlign>(find.byType(AnimatedAlign));
                expect(native.alignment,const AlignmentDirectional(-.5,.5));expect(native.curve,Curves.easeIn);
                expect(native.duration,const Duration(milliseconds:25));expect(native.onEnd,isNotNull);native.onEnd!();
                await tester.pumpWidget(const MaterialApp(home:cleared.Sample()));
                expect(tester.widget<AnimatedAlign>(find.byType(AnimatedAlign)).onEnd,isNull);
                expect(tester.takeException(),isNull);
              });
              testWidgets('native alignment interruption child identity and completion lifecycle',(tester)async{
                var ends=0;
                Future<void> show(AlignmentGeometry alignment,{int ms=100,bool rtl=false})async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(
                    textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                    child:Center(child:SizedBox(width:200,height:120,
                      child:AnimatedAlign(alignment:alignment,duration:Duration(milliseconds:ms),onEnd:()=>ends++,
                        child:const SizedBox(key:ValueKey('body'),width:40,height:40)))))));
                  await tester.pump();
                }
                await show(Alignment.topLeft);expect(ends,0);
                final child=tester.element(find.byKey(const ValueKey('body')));
                await show(Alignment.bottomRight);await tester.pump(const Duration(milliseconds:50));expect(ends,0);
                final container=tester.getRect(find.byType(AnimatedAlign));
                expect(tester.getTopLeft(find.byKey(const ValueKey('body')))-container.topLeft,const Offset(80,40));
                await show(Alignment.topLeft);await tester.pump(const Duration(milliseconds:101));await tester.pump();expect(ends,1);
                expect(identical(child,tester.element(find.byKey(const ValueKey('body')))),isTrue);
                await show(Alignment.topLeft);await tester.pump(const Duration(milliseconds:200));expect(ends,1);
                await show(AlignmentDirectional.bottomStart,ms:0,rtl:true);await tester.pump();expect(ends,2);
                expect(tester.getTopLeft(find.byKey(const ValueKey('body')))-container.topLeft,const Offset(160,80));
                expect(tester.takeException(),isNull);
              });
              testWidgets('native factor animation and pinned null tween behavior',(tester)async{
                Future<void> show(double? factor,{int generation=0})async{
                  await tester.pumpWidget(MaterialApp(home:Center(child:AnimatedAlign(
                    key:ValueKey(generation),alignment:Alignment.center,widthFactor:factor,heightFactor:factor,
                    duration:const Duration(milliseconds:100),child:const SizedBox(width:40,height:40)))));
                  await tester.pump();
                }
                Align align(WidgetTester t)=>t.widget<Align>(find.descendant(of:find.byType(AnimatedAlign),matching:find.byType(Align)).first);
                await show(2);expect(tester.getSize(find.byType(AnimatedAlign)),const Size(80,80));
                await show(4);await tester.pump(const Duration(milliseconds:50));expect(align(tester).widthFactor,3);
                await tester.pump(const Duration(milliseconds:51));expect(align(tester).widthFactor,4);
                await show(null);await tester.pumpAndSettle();
                expect(tester.widget<AnimatedAlign>(find.byType(AnimatedAlign)).widthFactor,isNull);
                expect(align(tester).widthFactor,4); // Flutter 3.44.8 retains the existing tween.
                await show(null,generation:1);expect(align(tester).widthFactor,isNull);
                expect(tester.takeException(),isNull);
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
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, CHILDREN, index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode adapter(String label, int extent) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(label)), Map.of());
        var size = new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(extent));
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), size, new PropertyName("height"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return box;
    }
    private static WidgetNode root(WidgetNode group) { return group; }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: animated_align_contract
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

