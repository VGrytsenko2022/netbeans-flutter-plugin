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
class AnimatedCrossFadeRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");


    @Test void bothChildrenTypedBuilderEvidenceSaveReopenUndoAndRuntime() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",20))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            Duration _duration() => const Duration(milliseconds:25);
            Duration? get _reverse => const Duration(milliseconds:70);
            AlignmentGeometry get _alignment => const AlignmentDirectional(-.5,.5);
            Curve get _curve => Curves.easeIn;
            void _end(){}
            Widget _layout(Widget topChild, Key topChildKey, Widget bottomChild, Key bottomChildKey) =>
              Stack(children:[Positioned(key:bottomChildKey,child:bottomChild),Positioned(key:topChildKey,child:topChild)]);
            AnimatedCrossFadeBuilder _layoutFactory() => _layout;
            dynamic _dynamicLayout(Widget a, Key b, Widget c, Key d) => a;
            void _voidLayout(Widget a, Key b, Widget c, Key d) {}
            Widget _wrongArgs(BuildContext context, Widget? child) => child!;
            String get _bad => 'wrong';
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedCrossFadeWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var inserted=apply(baseline,new AddWidget(place(GROUP_ID,1),prototype));
        assertAnalysis(baseline,inserted,"probe.dart",true);saveCase("defaults",inserted);inserted=reopen(inserted,source(inserted));
        // Explicit list keeps every constructor binding independently witnessed.
        var typed=apply(inserted,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)),
            new PatchProperties.SetPatch(new PropertyName("reverseDurationUs"),ref("_reverse",false)),
            new PatchProperties.SetPatch(new PropertyName("alignment"),ref("_alignment",false)),
            new PatchProperties.SetPatch(new PropertyName("firstCurve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("secondCurve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("sizeCurve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("layoutBuilder"),ref("_layout",false)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_end",false)))));
        assertAnalysis(inserted,typed,"probe.dart",true);saveCase("typed",typed);typed=reopen(typed,source(typed));
        var factory=apply(typed,new SetProperty(NESTED_ID,new PropertyName("layoutBuilder"),ref("_layoutFactory",true)));
        assertAnalysis(typed,factory,"probe.dart",true);saveCase("factory",factory);
        var switched=apply(typed,new SetProperty(NESTED_ID,new PropertyName("crossFadeState"),new PropertyValue.EnumValue("CrossFadeState","showSecond")));
        assertAnalysis(typed,switched,"probe.dart",true);saveCase("second",switched);
        assertArrayEquals(typed.current().dartCandidateBytes(),switched.undo().session().current().dartCandidateBytes());
        var reset=apply(typed,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("layoutBuilder")),
            new PatchProperties.SetPatch(new PropertyName("reverseDurationUs"),new PropertyValue.NullValue()),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),new PropertyValue.NullValue()))));
        assertAnalysis(typed,reset,"probe.dart",true);saveCase("reset",reset);
        var builder=io.github.vgrytsenko2022.designer.events.WidgetEventCatalog.find(prototype.type(),new PropertyName("layoutBuilder")).orElseThrow();
        var scaffoldBase=reopen(inserted,source(inserted).replace("int _userValue() => 73;", "int _userValue() => 73;\n"+builder.createStub("_generatedLayout")));
        var scaffold=apply(scaffoldBase,new SetProperty(NESTED_ID,new PropertyName("layoutBuilder"),ref("_generatedLayout",false)));
        assertAnalysis(scaffoldBase,scaffold,"probe.dart",true);saveCase("scaffold",scaffold);
        for(String bad:List.of("_bad","_dynamicLayout","_voidLayout","_wrongArgs")){
            var rejected=apply(typed,new SetProperty(NESTED_ID,new PropertyName("layoutBuilder"),ref(bad,false)));
            assertAnalysis(typed,rejected,"probe.dart",false);
        }
        for(String field:List.of("alignment","firstCurve","secondCurve","sizeCurve","durationUs","reverseDurationUs","onEnd"))
            assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref("_bad",false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("cross_fade_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/defaults.dart' as defaults;
            import '../lib/typed.dart' as typed;
            import '../lib/factory.dart' as factoryCase;
            import '../lib/second.dart' as second;
            import '../lib/reset.dart' as reset;
            import '../lib/scaffold.dart' as scaffold;
            void main(){
              final cases=<String,Widget>{'defaults':const defaults.Sample(),'typed':const typed.Sample(),'factory':const factoryCase.Sample(),
                'second':const second.Sample(),'reset':const reset.Sample(),'scaffold':const scaffold.Sample()};
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final n=tester.widget<AnimatedCrossFade>(find.byType(AnimatedCrossFade));
                  expect(n.firstChild,isA<SizedBox>());expect(n.secondChild,isA<SizedBox>());
                  expect(n.crossFadeState,e.key=='second'?CrossFadeState.showSecond:CrossFadeState.showFirst);
                  expect(n.excludeBottomFocus,isTrue);
                  if(e.key=='defaults'||e.key=='scaffold'){
                    expect(n.alignment,Alignment.topCenter);expect(n.duration,const Duration(milliseconds:300));
                    expect(n.firstCurve,Curves.linear);expect(n.reverseDuration,isNull);expect(n.onEnd,isNull);
                  }else{
                    expect(n.alignment,const AlignmentDirectional(-.5,.5));expect(n.duration,const Duration(milliseconds:25));
                    expect(n.firstCurve,Curves.easeIn);expect(n.secondCurve,Curves.easeIn);expect(n.sizeCurve,Curves.easeIn);
                    expect(n.reverseDuration,e.key=='reset'?null:const Duration(milliseconds:70));expect(n.onEnd==null,e.key=='reset');
                  }
                  expect(tester.takeException(),isNull);
                });
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
                name: animated_cross_fade_contract
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
