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
class AnimatedPhysicalModelRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void allNativeFieldsTypedProofSaveReopenUndoAndRuntime() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",20))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            Duration _duration() => const Duration(milliseconds:25);
            BorderRadius? get _radius => BorderRadius.only(topLeft:Radius.elliptical(5,8),bottomRight:Radius.circular(3));
            double get _elevation => 12.5;
            Color get _color => const Color(0xff112233);
            Color get _shadow => const Color(0x88224466);
            Curve get _curve => Curves.easeIn;
            void _end(){}
            String get _bad => 'wrong';
            BorderRadiusDirectional get _badRadius => BorderRadiusDirectional.circular(5);
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedPhysicalModelWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var wrapped=apply(baseline,new WrapWidget(GROUP_ID,prototype,new SlotName("child"),0));
        assertAnalysis(baseline,wrapped,"probe.dart",true);saveCase("defaults",wrapped);wrapped=reopen(wrapped,source(wrapped));
        var typed=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)),
            new PatchProperties.SetPatch(new PropertyName("borderRadius"),ref("_radius",false)),
            new PatchProperties.SetPatch(new PropertyName("elevation"),ref("_elevation",false)),
            new PatchProperties.SetPatch(new PropertyName("color"),ref("_color",false)),
            new PatchProperties.SetPatch(new PropertyName("shadowColor"),ref("_shadow",false)),
            new PatchProperties.SetPatch(new PropertyName("curve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_end",false)))));
        assertAnalysis(wrapped,typed,"probe.dart",true);saveCase("typed",typed);typed=reopen(typed,source(typed));
        var nullable=apply(typed,new SetProperty(NESTED_ID,new PropertyName("borderRadius"),new PropertyValue.NullValue()));
        assertAnalysis(typed,nullable,"probe.dart",true);saveCase("nullable",nullable);
        assertArrayEquals(typed.current().dartCandidateBytes(),nullable.undo().session().current().dartCandidateBytes());
        var theme=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("color"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.surface"))),
            new PatchProperties.SetPatch(new PropertyName("shadowColor"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.shadow"))))));
        assertAnalysis(wrapped,theme,"probe.dart",true);saveCase("themed",theme);
        var r=new PropertyValue.BoxDecorationValue.Radius(java.math.BigDecimal.valueOf(5),java.math.BigDecimal.valueOf(8));
        var literal=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("shape"),new PropertyValue.EnumValue("BoxShape","circle")),
            new PatchProperties.SetPatch(new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip","antiAlias")),
            new PatchProperties.SetPatch(new PropertyName("elevation"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(10))),
            new PatchProperties.SetPatch(new PropertyName("borderRadius"),new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(r,r,r,r))),
            new PatchProperties.SetPatch(new PropertyName("animateColor"),new PropertyValue.BooleanValue(false)),
            new PatchProperties.SetPatch(new PropertyName("animateShadowColor"),new PropertyValue.BooleanValue(false)))));
        assertAnalysis(wrapped,literal,"probe.dart",true);saveCase("literal",literal);
        for(String field:List.of("borderRadius","elevation","color","shadowColor")){
            var bad=apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref(field.equals("borderRadius")?"_badRadius":"_bad",false)));
            assertAnalysis(typed,bad,"probe.dart",false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("physical_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/defaults.dart' as defaults;
            import '../lib/typed.dart' as typed;
            import '../lib/nullable.dart' as nullable;
            import '../lib/themed.dart' as themed;
            import '../lib/literal.dart' as literal;
            void main(){
              final cases=<String,Widget>{'defaults':const defaults.Sample(),'typed':const typed.Sample(),
                'nullable':const nullable.Sample(),'themed':const themed.Sample(),'literal':const literal.Sample()};
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final n=tester.widget<AnimatedPhysicalModel>(find.ancestor(of:find.text('Sibling'),matching:find.byType(AnimatedPhysicalModel)).first);
                  expect(n.child,isNotNull);
                  if(e.key=='defaults'){
                    expect(n.color,const Color(0xff2196f3));expect(n.shadowColor,const Color(0xff000000));
                    expect(n.duration,const Duration(milliseconds:300));expect(n.elevation,0);expect(n.onEnd,isNull);
                    expect(n.shape,BoxShape.rectangle);expect(n.clipBehavior,Clip.none);expect(n.borderRadius,isNull);
                    expect(n.animateColor,true);expect(n.animateShadowColor,true);
                  }else if(e.key=='typed'||e.key=='nullable'){
                    expect(n.elevation,12.5);expect(n.curve,Curves.easeIn);expect(n.onEnd,isNotNull);
                    expect(n.color,const Color(0xff112233));expect(n.shadowColor,const Color(0x88224466));
                    expect(n.duration,const Duration(milliseconds:25));
                    expect(n.borderRadius?.topLeft,e.key=='typed'?const Radius.elliptical(5,8):null);
                  }else if(e.key=='literal'){
                    expect(n.elevation,10);expect(n.shape,BoxShape.circle);expect(n.clipBehavior,Clip.antiAlias);
                    expect(n.borderRadius,BorderRadius.all(const Radius.elliptical(5,8)));expect(n.animateColor,false);expect(n.animateShadowColor,false);
                  }else{
                    final theme=Theme.of(tester.element(find.text('Sibling')));
                    expect(n.color,theme.colorScheme.surface);expect(n.shadowColor,theme.colorScheme.shadow);
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
                name: animated_physical_model_contract
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

