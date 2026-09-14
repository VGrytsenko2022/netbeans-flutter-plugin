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
class AnimatedThemeRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");




    @Test void factoriesWholeThemeDataReferencesOptionalDurationEventsSaveReopenUndoAndRuntime() throws Exception {
        initialize();
        var child=adapter("Themed child",80);
        var baseline=open(group(GROUP_ID,List.of(child)),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            ThemeData get _theme => ThemeData.dark().copyWith(
              scaffoldBackgroundColor:const Color(0xff112233),
              iconTheme:const IconThemeData(color:Color(0xff33aa55),size:31),
              textSelectionTheme:const TextSelectionThemeData(cursorColor:Color(0xff5500aa)));
            ThemeData _themeFactory() => _theme;
            Duration _duration() => const Duration(milliseconds:25);
            Curve get _curve => Curves.easeIn;
            void _end(){}
            String get _bad => 'wrong';
            ThemeData? get _nullableTheme => null;
            dynamic get _dynamicTheme => ThemeData.light();
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedThemeWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var wrapped=apply(baseline,new WrapWidget(child.id(),prototype,new SlotName("child"),0));
        assertAnalysis(baseline,wrapped,"probe.dart",true);wrapped=reopen(wrapped,source(wrapped));
        for(String preset:AnimatedThemeWidgetPropertySchema.PRESETS){
            var candidate=preset.equals("light")?wrapped:apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("data"),new PropertyValue.StringValue(preset)));
            if(candidate!=wrapped)assertAnalysis(wrapped,candidate,"probe.dart",true);
            saveCase(preset.toLowerCase(Locale.ROOT),candidate);
        }
        var typed=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("data"),ref("_theme",false)),
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)),
            new PatchProperties.SetPatch(new PropertyName("curve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_end",false)))));
        assertAnalysis(wrapped,typed,"probe.dart",true);saveCase("typed",typed);typed=reopen(typed,source(typed));
        var factory=apply(typed,new SetProperty(NESTED_ID,new PropertyName("data"),ref("_themeFactory",true)));
        assertAnalysis(typed,factory,"probe.dart",true);saveCase("factory",factory);
        var reset=apply(typed,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("durationUs")),new PatchProperties.ResetPatch(new PropertyName("curve")),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),new PropertyValue.NullValue()))));
        assertAnalysis(typed,reset,"probe.dart",true);saveCase("reset",reset);
        assertArrayEquals(typed.current().dartCandidateBytes(),reset.undo().session().current().dartCandidateBytes());
        var event=dev.flutter.netbeans.designer.events.WidgetEventCatalog.find(prototype.type(),new PropertyName("onEnd")).orElseThrow();
        var scaffoldBase=reopen(wrapped,source(wrapped).replace("int _userValue() => 73;","int _userValue() => 73;\n"+event.createStub("_generatedEnd")));
        var scaffold=apply(scaffoldBase,new SetProperty(NESTED_ID,new PropertyName("onEnd"),ref("_generatedEnd",false)));
        assertAnalysis(scaffoldBase,scaffold,"probe.dart",true);saveCase("scaffold",scaffold);
        for(String bad:List.of("_bad","_nullableTheme","_dynamicTheme"))
            assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName("data"),ref(bad,false))),"probe.dart",false);
        for(String field:List.of("curve","durationUs","onEnd"))
            assertAnalysis(typed,apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref("_bad",false))),"probe.dart",false);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("theme_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/light.dart' as light;
            import '../lib/dark.dart' as dark;
            import '../lib/fallback.dart' as fallback;
            import '../lib/lightm2.dart' as lightm2;
            import '../lib/darkm2.dart' as darkm2;
            import '../lib/fallbackm2.dart' as fallbackm2;
            import '../lib/typed.dart' as typed;
            import '../lib/factory.dart' as factoryCase;
            import '../lib/reset.dart' as reset;
            import '../lib/scaffold.dart' as scaffold;
            void main(){
              final cases=<String,Widget>{'light':const light.Sample(),'dark':const dark.Sample(),'fallback':const fallback.Sample(),
                'lightm2':const lightm2.Sample(),'darkm2':const darkm2.Sample(),'fallbackm2':const fallbackm2.Sample(),
                'typed':const typed.Sample(),'factory':const factoryCase.Sample(),'reset':const reset.Sample(),'scaffold':const scaffold.Sample()};
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final finder=find.ancestor(of:find.text('Themed child'),matching:find.byType(AnimatedTheme)).first;
                  final n=tester.widget<AnimatedTheme>(finder);
                  final theme=Theme.of(tester.element(find.text('Themed child')));
                  expect(n.child,isA<SizedBox>());
                  if(['typed','factory','reset'].contains(e.key)){
                    expect(theme.scaffoldBackgroundColor,const Color(0xff112233));expect(theme.iconTheme.size,31);
                    expect(theme.textSelectionTheme.cursorColor,const Color(0xff5500aa));
                    expect(n.duration,Duration(milliseconds:e.key=='reset'?200:25));
                    expect(n.curve,e.key=='reset'?Curves.linear:Curves.easeIn);
                    expect(n.onEnd==null,e.key=='reset');
                  }else{
                    expect(n.duration,const Duration(milliseconds:200));expect(n.curve,Curves.linear);
                    expect(n.data.brightness,e.key.startsWith('dark')?Brightness.dark:Brightness.light);
                    expect(n.data.useMaterial3,!e.key.endsWith('m2'));
                    expect(n.onEnd==null,e.key!='scaffold');
                  }
                  expect(tester.takeException(),isNull);
                });
              }
              testWidgets('native completion fires on transition not initial mount',(tester)async{
                var calls=0;
                Widget page(ThemeData data)=>MaterialApp(home:AnimatedTheme(
                  key:const ValueKey('target'),data:data,onEnd:()=>calls++,child:const SizedBox(width:20,height:20)));
                await tester.pumpWidget(page(ThemeData.light()));await tester.pumpAndSettle();expect(calls,0);
                await tester.pumpWidget(page(ThemeData.dark()));await tester.pumpAndSettle();expect(calls,1);
                await tester.pumpWidget(page(ThemeData.dark()));await tester.pumpAndSettle();expect(calls,1);
                await tester.pumpWidget(page(ThemeData.light()));await tester.pumpAndSettle();expect(calls,2);
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
                name: animated_theme_contract
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
        for(var probe:ticket.request().symbolProbes())probe.staticTypeProbe().filter(t->t.expectedDartType().equals("ThemeData")).ifPresent(t->assertEquals("package:flutter/material.dart",t.expectedTypeLibraryUri()));
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
