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
import java.math.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class ColorFilteredRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void fiveFiltersStrictSourcesInactiveDraftsHistoryAndNativePixels()throws Exception{
  initialize();
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),nativeRuntime()+"\nvoid main()=>nativeTests();\n");
  runtimeCaseCount=3;run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");Files.delete(project.resolve("test/native_test.dart"));
  Files.writeString(lib.resolve("references.dart"),"""
import 'package:flutter/material.dart';
ColorFilter get colorFilter=>const ColorFilter.mode(Color(0xFF22AA66),BlendMode.src);
ColorFilter colorFilterFactory()=>colorFilter;
Color get color=>const Color(0xFF112233);
Color colorFactory()=>color;
class Values {
 static ColorFilter get colorFilter=>const ColorFilter.mode(Color(0xFF22AA66),BlendMode.src);
 static ColorFilter colorFilterFactory()=>colorFilter;
 static Color get color=>const Color(0xFF112233);
 static Color colorFactory()=>color;
}
""");
  var frameId=StableId.random();var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
  var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""

// User member is preserved.
int _userValue() => 73;
static ColorFilter get colorFilter=>const ColorFilter.mode(Color(0xFF22AA66),BlendMode.src);
static ColorFilter colorFilterFactory()=>colorFilter;
static Color get color=>const Color(0xFF112233);
static Color colorFactory()=>color;
ColorFilter get _colorFilter=>colorFilter;
ColorFilter _colorFilterFactory()=>colorFilter;
Color get _color=>color;
Color _colorFactory()=>color;
ColorFilter? get _nullableFilter=>null;
Color? get _nullableColor=>null;
Object get _wrong=>Object();
dynamic get _dynamic=>null;

""");
  baseline=reopen(baseline,"import 'package:flutter/material.dart';\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(ColorFilteredWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));assertAnalysis(baseline,base,"probe.dart",true);record("creation",base,"matrix");
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  base=reopen(base,source(base));
  var child=adapter("Filtered child",32);var withChild=apply(base,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),child));assertAnalysis(base,withChild,"probe.dart",true);
  base=reopen(withChild,source(withChild));
  for(String preset:ColorFilteredWidgetPropertySchema.FILTERS){
   var changed=base;
   if(!preset.equals("matrix")){changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("colorFilter"),new PropertyValue.StringValue(preset)));assertAnalysis(base,changed,"probe.dart",true);}
   record("local_"+preset.toLowerCase(),changed,preset);
  }
  for(String field:List.of("colorFilter","color"))for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var patches=new ArrayList<PatchProperties.Patch>();if(field.equals("color"))patches.add(new PatchProperties.SetPatch(new PropertyName("colorFilter"),new PropertyValue.StringValue("mode")));
   patches.add(new PatchProperties.SetPatch(new PropertyName(field),reference(field,imported,member,factory)));
   var changed=apply(base,new PatchProperties(NESTED_ID,patches));assertAnalysis(base,changed,"probe.dart",true);record("source_"+field.toLowerCase()+"_"+imported+"_"+member+"_"+factory,changed,field.equals("colorFilter")?"filterSource":"colorSource");
  }
  for(String field:List.of("colorFilter","color"))for(String bad:List.of("_wrong","_dynamic",field.equals("color")?"_nullableColor":"_nullableFilter")){
   var invalid=new ArrayList<PatchProperties.Patch>();if(field.equals("color"))invalid.add(new PatchProperties.SetPatch(new PropertyName("colorFilter"),new PropertyValue.StringValue("mode")));invalid.add(new PatchProperties.SetPatch(new PropertyName(field),ref(bad)));
   var changed=apply(base,new PatchProperties(NESTED_ID,invalid));
   assertAnalysis(base,changed,"probe.dart",false);
  }
  var inactive=apply(base,new SetProperty(NESTED_ID,new PropertyName("color"),ref("_dynamic")));assertAnalysis(base,inactive,"probe.dart",true);record("inactive",inactive,"matrix");
  inactive=reopen(inactive,source(inactive));
  var activated=apply(inactive,new SetProperty(NESTED_ID,new PropertyName("colorFilter"),new PropertyValue.StringValue("mode")));assertAnalysis(inactive,activated,"probe.dart",false);
  var override=apply(inactive,new SetProperty(NESTED_ID,new PropertyName("colorFilter"),ref("_colorFilter")));assertAnalysis(inactive,override,"probe.dart",true);record("override",override,"filterSource");
  var matrix=apply(base,new PatchProperties(NESTED_ID,List.of(new PatchProperties.SetPatch(new PropertyName("m00"),new PropertyValue.DoubleValue(BigDecimal.ONE.negate())),new PatchProperties.SetPatch(new PropertyName("m04"),new PropertyValue.DoubleValue(BigDecimal.valueOf(255))))));assertAnalysis(base,matrix,"probe.dart",true);record("matrix_changed",matrix,"matrixChanged");
  matrix=reopen(matrix,source(matrix));var reset=apply(matrix,new ResetProperty(NESTED_ID,new PropertyName("m00")));assertAnalysis(matrix,reset,"probe.dart",true);assertArrayEquals(matrix.current().dartCandidateBytes(),reset.undo().session().current().dartCandidateBytes());
  assertFalse(base.apply(new ResetProperty(NESTED_ID,new PropertyName("colorFilter"))).changed());
  var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  Files.writeString(project.resolve("test/color_filtered_test.dart"),runtime());run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private void record(String name,DesignerCommandSession session,String mode)throws Exception{saveCase(name,session);cases.put(name,mode);}
 private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:color_filtered_contract/references.dart"):Optional.empty(),
   member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
   member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }
 private String nativeRuntime(){return """
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

const identity=ColorFilter.matrix(<double>[1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0]);
Future<List<int>> pixel(WidgetTester tester,ColorFilter filter,Color color)async{
 final key=GlobalKey();
 await tester.pumpWidget(Directionality(textDirection:TextDirection.ltr,child:Center(child:RepaintBoundary(key:key,
  child:ColorFiltered(colorFilter:filter,child:ColoredBox(color:color,child:const SizedBox(width:16,height:16)))))));
 await tester.pump();
 final boundary=key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
 final rgba=(await tester.runAsync(()async{
  final image=await boundary.toImage(pixelRatio:1);try{return (await image.toByteData(format:ui.ImageByteFormat.rawRgba))!.buffer.asUint8List().sublist((8*16+8)*4,(8*16+8)*4+4);}finally{image.dispose();}
 }))!;
 return rgba;
}
void nativeTests(){
 testWidgets('native pixel output for identity mode matrix gamma saturation',(tester)async{
  expect(await pixel(tester,identity,const Color(0xFF0000FF)),[0,0,255,255]);
  expect(await pixel(tester,const ColorFilter.mode(Color(0xFFFF0000),BlendMode.src),const Color(0xFF0000FF)),[255,0,0,255]);
  expect(await pixel(tester,const ColorFilter.matrix([0,0,1,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0]),const Color(0xFF0000FF)),[255,0,0,255]);
  final gray=await pixel(tester,ColorFilter.saturation(0),const Color(0xFF0000FF));
  expect(gray[0],closeTo(18,1));expect(gray[0],gray[1]);expect(gray[1],gray[2]);expect(gray[3],255);
  final linear=await pixel(tester,const ColorFilter.srgbToLinearGamma(),const Color(0xFF808080));
  final srgb=await pixel(tester,const ColorFilter.linearToSrgbGamma(),const Color(0xFF808080));
  expect(linear[0],closeTo(55,1));expect(srgb[0],closeTo(188,1));
  expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());
 });
 testWidgets('layout hit testing semantics and layer reuse stay native',(tester)async{
  int taps=0;final semantics=tester.ensureSemantics();
  Widget view(ColorFilter filter)=>Directionality(textDirection:TextDirection.rtl,child:Center(child:ColorFiltered(key:const ValueKey('filter'),colorFilter:filter,
    child:Semantics(label:'Filtered child',button:true,child:GestureDetector(onTap:(){taps++;},child:const ColoredBox(color:Colors.blue,child:SizedBox(width:64,height:32)))))));
  await tester.pumpWidget(view(identity));final render=tester.renderObject<RenderObject>(find.byType(ColorFiltered));
  expect(tester.getSize(find.byType(ColorFiltered)),const Size(64,32));expect(find.bySemanticsLabel('Filtered child'),findsOneWidget);
  await tester.tap(find.byType(ColorFiltered));expect(taps,1);
  await tester.pumpWidget(view(const ColorFilter.mode(Colors.transparent,BlendMode.clear)));
  expect(tester.renderObject(find.byType(ColorFiltered)),same(render));expect(tester.getSize(find.byType(ColorFiltered)),const Size(64,32));
  await tester.tap(find.byType(ColorFiltered));expect(taps,2);expect(find.bySemanticsLabel('Filtered child'),findsOneWidget);
  expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());semantics.dispose();
 });
 testWidgets('empty child remains legal and zero size',(tester)async{
  await tester.pumpWidget(const Directionality(textDirection:TextDirection.ltr,child:Center(child:ColorFiltered(colorFilter:identity))));
  expect(tester.getSize(find.byType(ColorFiltered)),Size.zero);expect(tester.takeException(),isNull);
  await tester.pumpWidget(const SizedBox());
 });
}

""";}
 private String runtime(){
  runtimeCaseCount=cases.size()*2+3;var imports=new StringBuilder();var tests=new StringBuilder();
  cases.forEach((name,mode)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{")
    .append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:const case_").append(name).append(".Sample())));await tester.pump();")
    .append("final n=tester.widget<ColorFiltered>(find.byType(ColorFiltered));expect(find.text('Sibling'),findsOneWidget);");
   String expected=switch(mode){case "filterSource"->"const ColorFilter.mode(Color(0xFF22AA66),BlendMode.src)";case "colorSource"->"const ColorFilter.mode(Color(0xFF112233),BlendMode.srcOver)";case "mode"->"const ColorFilter.mode(Colors.transparent,BlendMode.srcOver)";case "saturation"->"ColorFilter.saturation(1)";case "linearToSrgbGamma"->"const ColorFilter.linearToSrgbGamma()";case "srgbToLinearGamma"->"const ColorFilter.srgbToLinearGamma()";case "matrixChanged"->"const ColorFilter.matrix([-1,0,0,0,255,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0])";default->"identity";};
   tests.append("expect(n.colorFilter,").append(expected).append(");");
   if(!name.equals("creation"))tests.append("expect(find.text('Filtered child'),findsOneWidget);");
   tests.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());});}\n");
  });
  return imports+nativeRuntime()+"\nvoid main(){nativeTests();"+tests+"}\n";
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
                name: color_filtered_contract
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
        // Inactive local drafts and structurally identical replacements can be FD-only.
        // No analyzer ticket is necessary when the exact generated Dart is unchanged.
        if (candidate.current().preparedPair().isEmpty()) {
            assertTrue(pass);
            assertArrayEquals(baseline.current().dartCandidateBytes(), candidate.current().dartCandidateBytes());
            assertNotEquals(baseline.current().fdSnapshot(), candidate.current().fdSnapshot());
            return;
        }
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
        if (logName.equals("flutter-test.log")) {
            assertTrue(output.contains("+"+runtimeCaseCount+": All tests passed!"), output);
            System.out.println("ColorFiltered pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
