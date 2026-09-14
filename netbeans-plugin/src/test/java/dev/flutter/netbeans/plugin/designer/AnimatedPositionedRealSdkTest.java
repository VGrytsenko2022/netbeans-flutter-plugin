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
class AnimatedPositionedRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void threeConstructorsExactProofAtomicWrapReopenAndNativeRuntime() throws Exception {
        initialize();int index=0;
        for(var type:AnimatedPositionedWidgetPropertySchema.TYPES){
            var child=adapter("Child",40);
            var baseline=open(group(GROUP_ID,List.of(child,adapter("Sibling",20))),"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                Duration _duration() => const Duration(milliseconds: 25);
                double? get _left => 10;
                double? _width() => 80;
                Curve get _curve => Curves.easeIn;
                Rect get _rect => const Rect.fromLTWH(10,20,80,40);
                String get _bad => 'wrong type';
                void _end() {}
                """);
            var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(),NESTED_ID);
            var wrapped=apply(baseline,new WrapWidget(child.id(),prototype,new SlotName("child"),0));
            assertAnalysis(baseline,wrapped,"probe.dart",true);saveCase("initial"+index,wrapped);wrapped=reopen(wrapped,source(wrapped));
            var patches=new ArrayList<PatchProperties.Patch>();
            patches.add(new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)));
            patches.add(new PatchProperties.SetPatch(new PropertyName("curve"),ref("_curve",false)));
            patches.add(new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_end",false)));
            boolean rect=type.equals(AnimatedPositionedWidgetPropertySchema.RECT_TYPE);
            if(rect)patches.add(new PatchProperties.SetPatch(new PropertyName("rect"),ref("_rect",false)));
            else{
                patches.add(new PatchProperties.SetPatch(new PropertyName(AnimatedPositionedWidgetPropertySchema.directional(type)?"start":"left"),ref("_left",false)));
                patches.add(new PatchProperties.SetPatch(new PropertyName("top"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(20))));
                patches.add(new PatchProperties.SetPatch(new PropertyName("width"),ref("_width",true)));
                patches.add(new PatchProperties.SetPatch(new PropertyName("height"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(40))));
            }
            var typed=apply(wrapped,new PatchProperties(NESTED_ID,patches));
            assertAnalysis(wrapped,typed,"probe.dart",true);saveCase("typed"+index,typed);typed=reopen(typed,source(typed));
            patches.clear();
            if(rect){
                patches.add(new PatchProperties.SetPatch(new PropertyName("rect"),new PropertyValue.StringValue("local")));
                patches.add(new PatchProperties.SetPatch(new PropertyName("rectLeft"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(-10))));
                patches.add(new PatchProperties.SetPatch(new PropertyName("rectWidth"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(-20))));
            }else{
                patches.add(new PatchProperties.SetPatch(new PropertyName("width"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(-20))));
                patches.add(new PatchProperties.SetPatch(new PropertyName("height"),new PropertyValue.NullValue()));
            }
            patches.add(new PatchProperties.SetPatch(new PropertyName("durationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)));
            var local=apply(typed,new PatchProperties(NESTED_ID,patches));
            assertAnalysis(typed,local,"probe.dart",true);saveCase("local"+index,local);
            assertArrayEquals(typed.current().dartCandidateBytes(),local.undo().session().current().dartCandidateBytes());
            var bad=apply(typed,new SetProperty(NESTED_ID,new PropertyName(rect?"rect":"width"),ref("_bad",false)));
            assertAnalysis(typed,bad,"probe.dart",false);
            assertFalse(typed.apply(new RemoveWidget(child.id())).changed());
            index++;
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("positioned_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/initial0.dart' as i0; import '../lib/initial1.dart' as i1; import '../lib/initial2.dart' as i2;
            import '../lib/typed0.dart' as t0; import '../lib/typed1.dart' as t1; import '../lib/typed2.dart' as t2;
            import '../lib/local0.dart' as l0; import '../lib/local1.dart' as l1; import '../lib/local2.dart' as l2;
            void main(){
              final cases=<String,Widget>{'initial0':const i0.Sample(),'initial1':const i1.Sample(),'initial2':const i2.Sample(),
                'typed0':const t0.Sample(),'typed1':const t1.Sample(),'typed2':const t2.Sample(),
                'local0':const l0.Sample(),'local1':const l1.Sample(),'local2':const l2.Sample()};
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final finder=e.key.endsWith('2')?find.byType(AnimatedPositionedDirectional):find.byType(AnimatedPositioned);
                  final n=tester.widget<ImplicitlyAnimatedWidget>(finder);
                  final pd=tester.widget<Positioned>(find.descendant(of:finder,matching:find.byType(Positioned)).first);
                  if(e.key.startsWith('typed')){
                    expect(pd.top,20);expect(pd.width,80);expect(pd.height,40);
                    expect(e.key.endsWith('2')&&rtl?pd.right:pd.left,10);
                    expect(n.duration,const Duration(milliseconds:25));expect(n.curve,Curves.easeIn);expect(n.onEnd,isNotNull);
                  }else if(e.key.startsWith('initial')){
                    expect(n.duration,const Duration(milliseconds:300));expect(n.onEnd,isNull);
                  }else{
                    expect(pd.width,-20);expect(n.duration,Duration.zero);
                  }
                  expect(tester.takeException(),isNull);
                });
              }
              testWidgets('IndexedStack inserts a RenderObject boundary incompatible with Positioned',(tester)async{
                await tester.pumpWidget(const Directionality(textDirection:TextDirection.ltr,child:SizedBox(width:200,height:200,
                  child:IndexedStack(children:[AnimatedPositioned(left:0,top:0,duration:Duration(milliseconds:100),child:SizedBox(width:20,height:20))]))));
                expect(tester.takeException(),isFlutterError);
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
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Stack"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(children)));
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
                name: animated_positioned_contract
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


