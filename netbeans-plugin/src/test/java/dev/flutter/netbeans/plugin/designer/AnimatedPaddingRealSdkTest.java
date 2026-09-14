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
class AnimatedPaddingRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void generatedFormsExactProofReopenHistoryAndNativeTransitions() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",60))),"probe.dart",
            "// User member is preserved.\nint _userValue() => 73;\nDuration _customDuration() => const Duration(milliseconds: 25);\nCurve get _customCurve => Curves.easeIn;\nEdgeInsetsGeometry _customPadding() => const EdgeInsets.all(2).add(const EdgeInsetsDirectional.only(start: 6, end: 10));\nvoid _handleEnd() {}");
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedPaddingWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var empty=apply(baseline,new AddWidget(place(GROUP_ID,1),prototype));
        assertAnalysis(baseline,empty,"probe.dart",true);saveCase("empty",empty);
        assertArrayEquals(baseline.current().dartCandidateBytes(),empty.undo().session().current().dartCandidateBytes());
        assertArrayEquals(empty.current().dartCandidateBytes(),empty.undo().session().redo().session().current().dartCandidateBytes());
        empty=reopen(empty,source(empty));
        var child=adapter("Padded body",40);
        var current=apply(empty,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),child));
        assertAnalysis(empty,current,"probe.dart",true);saveCase("center",current);current=reopen(current,source(current));
        for(boolean directional:List.of(false,true)){
            PropertyValue value=directional
                ?new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.ONE,java.math.BigDecimal.TWO,java.math.BigDecimal.TEN,java.math.BigDecimal.ZERO)
                :new PropertyValue.EdgeInsetsValue(java.math.BigDecimal.ONE,java.math.BigDecimal.TWO,java.math.BigDecimal.TEN,java.math.BigDecimal.ZERO);
            var changed=apply(current,new SetProperty(NESTED_ID,new PropertyName("padding"),value));
            assertAnalysis(current,changed,"probe.dart",true);saveCase(directional?"directional":"physical",changed);current=reopen(changed,source(changed));
        }
        var reset=apply(current,new SetProperty(NESTED_ID,new PropertyName("padding"),
            new PropertyValue.EdgeInsetsValue(java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO)));
        assertAnalysis(current,reset,"probe.dart",true);saveCase("zero",reset);reset=reopen(reset,source(reset));
        var typed=apply(reset,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("padding"),ref("_customPadding",true)),
            new PatchProperties.SetPatch(new PropertyName("curve"),ref("_customCurve",false)),
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_customDuration",true)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_handleEnd",false)))));
        assertAnalysis(reset,typed,"probe.dart",true);saveCase("typed",typed);
        assertArrayEquals(reset.current().dartCandidateBytes(),typed.undo().session().current().dartCandidateBytes());
        typed=reopen(typed,source(typed));
        var wrong=apply(typed,new SetProperty(NESTED_ID,new PropertyName("padding"),ref("_customDuration",true)));
        assertAnalysis(typed,wrong,"probe.dart",false);
        var cleared=apply(typed,new SetProperty(NESTED_ID,new PropertyName("onEnd"),new PropertyValue.NullValue()));
        assertAnalysis(typed,cleared,"probe.dart",true);saveCase("cleared",cleared);
        var moved=apply(reset,new MoveWidget(child.id(),place(GROUP_ID,2)));
        assertAnalysis(reset,moved,"probe.dart",true);saveCase("moved",moved);
        assertArrayEquals(reset.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());
        assertFalse(reset.apply(new ResetProperty(NESTED_ID,new PropertyName("padding"))).changed());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("padding_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/empty.dart' as empty;
            import '../lib/center.dart' as center;
            import '../lib/physical.dart' as physical;
            import '../lib/directional.dart' as directional;
            import '../lib/zero.dart' as zero;
            import '../lib/typed.dart' as typed;
            import '../lib/cleared.dart' as cleared;
            import '../lib/moved.dart' as moved;
            void main(){
              final cases=<String,(Widget,EdgeInsetsGeometry,bool)>{
                'empty':(const empty.Sample(),const EdgeInsets.all(16),true),
                'center':(const center.Sample(),const EdgeInsets.all(16),false),
                'physical':(const physical.Sample(),const EdgeInsets.fromLTRB(1,2,10,0),false),
                'directional':(const directional.Sample(),const EdgeInsetsDirectional.fromSTEB(1,2,10,0),false),
                'zero':(const zero.Sample(),EdgeInsets.zero,false),
                'moved':(const moved.Sample(),EdgeInsets.zero,true),
              };
              for(final e in cases.entries){
                for(final rtl in [false,true]){
                  testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                    await tester.pumpWidget(MaterialApp(home:Directionality(
                      textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value.$1)));
                    await tester.pumpAndSettle();
                    final native=tester.widget<AnimatedPadding>(find.byType(AnimatedPadding));
                    expect(native.padding,e.value.$2);expect(native.child==null,e.value.$3);
                    expect(native.duration,const Duration(milliseconds:300));expect(native.curve,Curves.linear);
                    expect(tester.takeException(),isNull);
                  });
                }
              }
              testWidgets('generated mixed typed geometry and callback are exact',(tester)async{
                await tester.pumpWidget(const MaterialApp(home:typed.Sample()));
                final native=tester.widget<AnimatedPadding>(find.byType(AnimatedPadding));
                expect(native.padding.resolve(TextDirection.ltr),const EdgeInsets.fromLTRB(8,2,12,2));
                expect(native.padding.resolve(TextDirection.rtl),const EdgeInsets.fromLTRB(12,2,8,2));
                expect(native.curve,Curves.easeIn);expect(native.duration,const Duration(milliseconds:25));
                expect(native.onEnd,isNotNull);native.onEnd!();
                await tester.pumpWidget(const MaterialApp(home:cleared.Sample()));
                expect(tester.widget<AnimatedPadding>(find.byType(AnimatedPadding)).onEnd,isNull);
                expect(tester.takeException(),isNull);
              });
              testWidgets('native interruption child identity completion and zero-duration RTL',(tester)async{
                var ends=0;
                Future<void> show(EdgeInsetsGeometry padding,{int ms=100,bool rtl=false})async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(
                    textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                    child:Center(child:AnimatedPadding(padding:padding,duration:Duration(milliseconds:ms),onEnd:()=>ends++,
                      child:const SizedBox(key:ValueKey('body'),width:40,height:40))))));
                  await tester.pump();
                }
                Padding inner()=>tester.widget<Padding>(find.descendant(of:find.byType(AnimatedPadding),matching:find.byType(Padding)).first);
                await show(EdgeInsets.zero);expect(ends,0);
                final child=tester.element(find.byKey(const ValueKey('body')));
                await show(const EdgeInsets.all(20));await tester.pump(const Duration(milliseconds:50));expect(ends,0);
                expect(inner().padding,const EdgeInsets.all(10));
                await show(EdgeInsets.zero);await tester.pump(const Duration(milliseconds:101));await tester.pump();expect(ends,1);
                expect(identical(child,tester.element(find.byKey(const ValueKey('body')))),isTrue);
                await show(EdgeInsets.zero);await tester.pump(const Duration(milliseconds:200));expect(ends,1);
                await show(const EdgeInsetsDirectional.only(start:30,end:10),ms:0,rtl:true);await tester.pump();expect(ends,2);
                final parent=tester.getRect(find.byType(AnimatedPadding)),body=tester.getRect(find.byKey(const ValueKey('body')));
                expect(body.left-parent.left,10);expect(parent.right-body.right,30);
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
                name: animated_padding_contract
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

