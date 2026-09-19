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
class AnimatedFractionallySizedBoxRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void nullableFactorsAlignmentTypedProofSaveReopenUndoAndRuntime() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",20))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            Duration _duration() => const Duration(milliseconds:25);
            AlignmentGeometry get _alignment => const AlignmentDirectional(-.5,.5);
            double? get _width => .75;
            double? _height() => null;
            Curve get _curve => Curves.easeIn;
            void _end(){}
            String get _bad => 'wrong';
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var wrapped=apply(baseline,new WrapWidget(GROUP_ID,prototype,new SlotName("child"),0));
        assertAnalysis(baseline,wrapped,"probe.dart",true);saveCase("defaults",wrapped);wrapped=reopen(wrapped,source(wrapped));
        var typed=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)),
            new PatchProperties.SetPatch(new PropertyName("alignment"),ref("_alignment",false)),
            new PatchProperties.SetPatch(new PropertyName("widthFactor"),ref("_width",false)),
            new PatchProperties.SetPatch(new PropertyName("heightFactor"),ref("_height",true)),
            new PatchProperties.SetPatch(new PropertyName("curve"),ref("_curve",false)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_end",false)))));
        assertAnalysis(wrapped,typed,"probe.dart",true);saveCase("typed",typed);typed=reopen(typed,source(typed));
        var nullable=apply(typed,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("widthFactor"),new PropertyValue.NullValue()),
            new PatchProperties.SetPatch(new PropertyName("heightFactor"),new PropertyValue.NullValue()),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),new PropertyValue.NullValue()))));
        assertAnalysis(typed,nullable,"probe.dart",true);saveCase("nullable",nullable);
        assertArrayEquals(typed.current().dartCandidateBytes(),nullable.undo().session().current().dartCandidateBytes());
        nullable=reopen(nullable,source(nullable));
        var reset=apply(nullable,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("alignment")),
            new PatchProperties.ResetPatch(new PropertyName("widthFactor")),
            new PatchProperties.ResetPatch(new PropertyName("heightFactor")))));
        assertAnalysis(nullable,reset,"probe.dart",true);saveCase("reset",reset);
        var literal=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,java.math.BigDecimal.ONE.negate(),java.math.BigDecimal.ONE)),
            new PatchProperties.SetPatch(new PropertyName("widthFactor"),new PropertyValue.DoubleValue(new java.math.BigDecimal("1.25"))),
            new PatchProperties.SetPatch(new PropertyName("heightFactor"),new PropertyValue.DoubleValue(new java.math.BigDecimal(".5"))),
            new PatchProperties.SetPatch(new PropertyName("durationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO)))));
        assertAnalysis(wrapped,literal,"probe.dart",true);saveCase("literal",literal);
        var empty=apply(wrapped,new RemoveWidget(GROUP_ID));
        assertAnalysis(wrapped,empty,"probe.dart",true);saveCase("empty",empty);
        assertArrayEquals(wrapped.current().dartCandidateBytes(),empty.undo().session().current().dartCandidateBytes());
        assertFalse(wrapped.apply(new ResetProperty(NESTED_ID,new PropertyName("durationUs"))).changed());
        for(String field:List.of("alignment","widthFactor","heightFactor","durationUs","curve","onEnd")){
            var bad=apply(typed,new SetProperty(NESTED_ID,new PropertyName(field),ref("_bad",false)));
            assertAnalysis(typed,bad,"probe.dart",false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("fractional_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/defaults.dart' as defaults;
            import '../lib/typed.dart' as typed;
            import '../lib/nullable.dart' as nullable;
            import '../lib/reset.dart' as reset;
            import '../lib/literal.dart' as literal;
            import '../lib/empty.dart' as empty;
            void main(){
              final cases=<String,Widget>{'defaults':const defaults.Sample(),'typed':const typed.Sample(),
                'nullable':const nullable.Sample(),'reset':const reset.Sample(),'literal':const literal.Sample(),'empty':const empty.Sample()};
              for(final e in cases.entries)for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final n=tester.widget<AnimatedFractionallySizedBox>(find.byType(AnimatedFractionallySizedBox));
                  expect(n.child==null,e.key=='empty');
                  if(e.key=='defaults'||e.key=='empty'){
                    expect(n.alignment,Alignment.center);expect(n.widthFactor,isNull);expect(n.heightFactor,isNull);
                    expect(n.duration,const Duration(milliseconds:300));expect(n.curve,Curves.linear);expect(n.onEnd,isNull);
                  }else if(e.key=='typed'||e.key=='nullable'||e.key=='reset'){
                    expect(n.alignment,e.key=='reset'?Alignment.center:const AlignmentDirectional(-.5,.5));
                    expect(n.widthFactor,e.key=='typed' ? .75 : null);expect(n.heightFactor,isNull);
                    expect(n.curve,Curves.easeIn);expect(n.duration,const Duration(milliseconds:25));
                    expect(n.onEnd!=null,e.key=='typed');if(n.onEnd!=null)n.onEnd!();
                  }else{
                    expect(n.alignment,AlignmentDirectional.bottomStart);expect(n.widthFactor,1.25);expect(n.heightFactor,.5);
                    expect(n.duration,Duration.zero);
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
                name: animated_fractionally_sized_box_contract
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
