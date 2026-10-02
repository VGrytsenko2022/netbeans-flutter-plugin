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
class AnimatedContainerRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void generatedFormsExactProofReopenHistoryAndNativeArguments() throws Exception {
        initialize();
        var baseline=open(group(GROUP_ID,List.of(adapter("Sibling",20))),"probe.dart","""
            // User member is preserved.
            int _userValue() => 73;
            Duration _duration() => const Duration(milliseconds: 25);
            Curve get _curve => Curves.easeIn;
            AlignmentGeometry? get _alignment => AlignmentDirectional.centerEnd;
            EdgeInsetsGeometry? get _insets => const EdgeInsetsDirectional.all(2);
            Color? get _color => const Color(0xff123456);
            Decoration? get _decoration => const ShapeDecoration(color: Color(0xff123456), shape: StadiumBorder());
            BoxConstraints? get _constraints => const BoxConstraints(maxWidth: 100, maxHeight: 100);
            Matrix4? get _matrix => Matrix4.identity()..translateByDouble(3, 4, 0, 1);
            double? get _width => 80;
            double? get _nothing => null;
            dynamic get _dynamic => 60.0;
            String get _wrong => 'bad';
            void _handleEnd() {}
            """);
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedContainerWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var empty=apply(baseline,new AddWidget(place(GROUP_ID,1),prototype));
        assertAnalysis(baseline,empty,"probe.dart",true);saveCase("empty",empty);
        assertArrayEquals(baseline.current().dartCandidateBytes(),empty.undo().session().current().dartCandidateBytes());
        var current=apply(reopen(empty,source(empty)),new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),adapter("Body",30)));
        current=reopen(current,source(current));
        var patches=new ArrayList<PatchProperties.Patch>();
        for(var entry:Map.ofEntries(Map.entry("alignment","_alignment"),Map.entry("padding","_insets"),Map.entry("margin","_insets"),
            Map.entry("decoration","_decoration"),Map.entry("foregroundDecoration","_decoration"),Map.entry("width","_width"),Map.entry("height","_width"),
            Map.entry("constraints","_constraints"),Map.entry("transform","_matrix"),Map.entry("transformAlignment","_alignment"),
            Map.entry("curve","_curve"),Map.entry("onEnd","_handleEnd")).entrySet())
            patches.add(new PatchProperties.SetPatch(new PropertyName(entry.getKey()),ref(entry.getValue(),false)));
        patches.add(new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_duration",true)));
        patches.add(new PatchProperties.SetPatch(new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip","antiAlias")));
        var typed=apply(current,new PatchProperties(NESTED_ID,patches));
        assertAnalysis(current,typed,"probe.dart",true);saveCase("typed",typed);typed=reopen(typed,source(typed));
        for(String bad:List.of("_dynamic","_wrong")){
            var wrong=apply(typed,new SetProperty(NESTED_ID,new PropertyName("width"),ref(bad,false)));
            assertAnalysis(typed,wrong,"probe.dart",false);
        }
        var color=apply(typed,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.ResetPatch(new PropertyName("decoration")),
            new PatchProperties.SetPatch(new PropertyName("color"),ref("_color",false)),
            new PatchProperties.SetPatch(new PropertyName("height"),ref("_nothing",false)))));
        assertAnalysis(typed,color,"probe.dart",true);saveCase("color",color);color=reopen(color,source(color));
        var nulls=new ArrayList<PatchProperties.Patch>();
        for(var p:AnimatedContainerWidgetPropertySchema.properties())if(p.acceptedKinds().contains(PropertyValueKind.NULL))
            nulls.add(new PatchProperties.SetPatch(p.name(),new PropertyValue.NullValue()));
        nulls.add(new PatchProperties.ResetPatch(new PropertyName("clipBehavior")));
        var cleared=apply(color,new PatchProperties(NESTED_ID,nulls));
        assertAnalysis(color,cleared,"probe.dart",true);saveCase("cleared",cleared);
        empty=reopen(empty,source(empty));
        var infinity=apply(empty,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("width"),new PropertyValue.EnumValue("double","infinity")),
            new PatchProperties.SetPatch(new PropertyName("height"),new PropertyValue.DoubleValue(java.math.BigDecimal.TEN)))));
        assertAnalysis(empty,infinity,"probe.dart",true);saveCase("infinity",infinity);
        assertArrayEquals(color.current().dartCandidateBytes(),cleared.undo().session().current().dartCandidateBytes());
        assertFalse(current.apply(new ResetProperty(NESTED_ID,new PropertyName("durationUs"))).changed());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("container_test.dart"),"""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/empty.dart' as empty;
            import '../lib/typed.dart' as typed;
            import '../lib/color.dart' as color;
            import '../lib/cleared.dart' as cleared;
            import '../lib/infinity.dart' as infinite;
            void main(){
              final cases=<String,Widget>{'empty':const empty.Sample(),'typed':const typed.Sample(),
                'color':const color.Sample(),'cleared':const cleared.Sample(),'infinity':const infinite.Sample()};
              for(final e in cases.entries) for(final rtl in [false,true]){
                testWidgets('generated ${e.key} rtl=$rtl',(tester)async{
                  await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:e.value)));
                  await tester.pumpAndSettle();
                  final n=tester.widget<AnimatedContainer>(find.byType(AnimatedContainer));
                  if(e.key=='typed'){
                    expect(n.decoration,isA<ShapeDecoration>());expect(n.foregroundDecoration,isA<ShapeDecoration>());
                    expect(n.constraints,const BoxConstraints.tightFor(width:80,height:80));expect(n.padding,const EdgeInsetsDirectional.all(2));
                    expect(n.margin,const EdgeInsetsDirectional.all(2));expect(n.transform!.storage[12],3);
                    expect(n.alignment,AlignmentDirectional.centerEnd);expect(n.transformAlignment,AlignmentDirectional.centerEnd);
                    expect(n.clipBehavior,Clip.antiAlias);expect(n.duration,const Duration(milliseconds:25));expect(n.curve,Curves.easeIn);expect(n.onEnd,isNotNull);
                  }else if(e.key=='color'){
                    expect(n.decoration,isA<BoxDecoration>());expect((n.decoration as BoxDecoration).color,const Color(0xff123456));
                    expect(n.constraints,const BoxConstraints(minWidth:80,maxWidth:80,maxHeight:100));expect(n.clipBehavior,Clip.antiAlias);
                  }else if(e.key=='infinity'){
                    expect(n.constraints!.minWidth,double.infinity);expect(n.constraints!.minHeight,10);
                  }else{
                    expect(n.decoration,isNull);expect(n.padding,isNull);expect(n.alignment,isNull);expect(n.transform,isNull);
                    expect(n.onEnd,isNull);expect(n.duration,e.key=='cleared'?const Duration(milliseconds:25):const Duration(milliseconds:300));
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
                name: animated_container_contract
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
