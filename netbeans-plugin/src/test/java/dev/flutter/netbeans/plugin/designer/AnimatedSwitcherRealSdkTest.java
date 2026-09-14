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
class AnimatedSwitcherRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");



    @Test void sixBindingsBothBuildersProofSaveReopenUndoAndGeneratedRuntime() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",20))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            Duration _duration() => const Duration(milliseconds:25);
            Duration? get _reverse => const Duration(milliseconds:70);
            Curve get _curve => Curves.easeIn;
            Widget _transition(Widget child, Animation<double> animation) => AnimatedSwitcher.defaultTransitionBuilder(child,animation);
            Widget _layout(Widget? currentChild, List<Widget> previousChildren) => AnimatedSwitcher.defaultLayoutBuilder(currentChild,previousChildren);
            AnimatedSwitcherTransitionBuilder _transitionFactory() => _transition;
            AnimatedSwitcherLayoutBuilder _layoutFactory() => _layout;
            dynamic _dynamicTransition(Widget child, Animation<double> animation) => child;
            void _voidTransition(Widget child, Animation<double> animation) {}
            dynamic _dynamicLayout(Widget? child, List<Widget> previous) => child;
            void _voidLayout(Widget? child, List<Widget> previous) {}
            Widget _wrongArgs(BuildContext context, Widget? child) => child!;
            String get _bad => 'wrong';
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedSwitcherWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var inserted=apply(baseline,new AddWidget(place(GROUP_ID,1),prototype));
        assertAnalysis(baseline,inserted,"probe.dart",true);saveCase("empty",inserted);inserted=reopen(inserted,source(inserted));
        var child=adapter("Switcher",48);
        var full=apply(inserted,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),child));
        assertAnalysis(inserted,full,"probe.dart",true);saveCase("defaults",full);full=reopen(full,source(full));
        var typed=apply(full,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)),
            new PatchProperties.SetPatch(new PropertyName("reverseDurationUs"),ref("_reverse",false)),
            new PatchProperties.SetPatch(new PropertyName("switchInCurve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("switchOutCurve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("transitionBuilder"),ref("_transition",false)),
            new PatchProperties.SetPatch(new PropertyName("layoutBuilder"),ref("_layout",false)))));
        assertAnalysis(full,typed,"probe.dart",true);saveCase("typed",typed);typed=reopen(typed,source(typed));
        var factory=apply(typed,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("transitionBuilder"),ref("_transitionFactory",true)),
            new PatchProperties.SetPatch(new PropertyName("layoutBuilder"),ref("_layoutFactory",true)))));
        assertAnalysis(typed,factory,"probe.dart",true);saveCase("factory",factory);
        var reset=apply(typed,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("transitionBuilder")),
            new PatchProperties.ResetPatch(new PropertyName("layoutBuilder")),
            new PatchProperties.SetPatch(new PropertyName("reverseDurationUs"),new PropertyValue.NullValue()))));
        assertAnalysis(typed,reset,"probe.dart",true);saveCase("reset",reset);
        assertArrayEquals(typed.current().dartCandidateBytes(),reset.undo().session().current().dartCandidateBytes());
        String stubs="";
        for(String field:List.of("transitionBuilder","layoutBuilder"))
            stubs+=dev.flutter.netbeans.designer.events.WidgetEventCatalog.find(prototype.type(),new PropertyName(field)).orElseThrow().createStub("_generated"+field)+"\n";
        var scaffoldBase=reopen(full,source(full).replace("int _userValue() => 73;","int _userValue() => 73;\n"+stubs));
        var scaffold=apply(scaffoldBase,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("transitionBuilder"),ref("_generatedtransitionBuilder",false)),
            new PatchProperties.SetPatch(new PropertyName("layoutBuilder"),ref("_generatedlayoutBuilder",false)))));
        assertAnalysis(scaffoldBase,scaffold,"probe.dart",true);saveCase("scaffold",scaffold);
        for(String field:List.of("transitionBuilder","layoutBuilder")){
            String suffix=field.equals("layoutBuilder")?"Layout":"Transition";
            for(String bad:List.of("_bad","_dynamic"+suffix,"_void"+suffix,"_wrongArgs"))
                assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref(bad,false))),"probe.dart",false);
        }
        for(String field:List.of("durationUs","reverseDurationUs","switchInCurve","switchOutCurve"))
            assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref("_bad",false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("switcher_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/empty.dart' as empty;
            import '../lib/defaults.dart' as defaults;
            import '../lib/typed.dart' as typed;
            import '../lib/factory.dart' as factoryCase;
            import '../lib/reset.dart' as reset;
            import '../lib/scaffold.dart' as scaffold;
            void main(){
              final cases=<String,Widget>{'empty':const empty.Sample(),'defaults':const defaults.Sample(),'typed':const typed.Sample(),
                'factory':const factoryCase.Sample(),'reset':const reset.Sample(),'scaffold':const scaffold.Sample()};
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final n=tester.widget<AnimatedSwitcher>(find.byType(AnimatedSwitcher));
                  if(e.key=='empty'){expect(n.child,isNull);}else{expect(n.child,isA<SizedBox>());expect(n.child!.key,isNull);}
                  if(['defaults','empty','scaffold'].contains(e.key)){
                    expect(n.duration,const Duration(milliseconds:300));expect(n.reverseDuration,isNull);
                    expect(n.switchInCurve,Curves.linear);expect(n.switchOutCurve,Curves.linear);
                  }else{
                    expect(n.duration,const Duration(milliseconds:25));expect(n.switchInCurve,Curves.easeIn);expect(n.switchOutCurve,Curves.easeIn);
                    expect(n.reverseDuration,e.key=='reset'?null:const Duration(milliseconds:70));
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
                name: animated_switcher_contract
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
