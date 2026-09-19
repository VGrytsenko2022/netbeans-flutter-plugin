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
class RawImageRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 private static final List<String> FIELDS=List.of("image","width","height","scale","color","opacity","alignment","centerSlice");
 @Test void allSourceShapesStrictUiImageTypeOwnershipPersistenceAndNativePainting()throws Exception{
  initialize();
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),nativeRuntime()+"\nvoid main()=>nativeTests();\n");
  runtimeCaseCount=3;run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");Files.delete(project.resolve("test/native_test.dart"));
  Files.writeString(lib.resolve("references.dart"),"""
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
ui.Image? get image=>null;
ui.Image? imageFactory()=>image;
double? get width=>48;
double? widthFactory()=>width;
double? get height=>32;
double? heightFactory()=>height;
double get scale=>2;
double scaleFactory()=>scale;
Color? get color=>const Color(0xFF112233);
Color? colorFactory()=>color;
Animation<double>? get opacity=>const AlwaysStoppedAnimation<double>(0.25);
Animation<double>? opacityFactory()=>opacity;
AlignmentGeometry get alignment=>AlignmentDirectional.topEnd;
AlignmentGeometry alignmentFactory()=>alignment;
Rect? get centerSlice=>const Rect.fromLTRB(2,2,14,10);
Rect? centerSliceFactory()=>centerSlice;
class Values{
static ui.Image? get image=>null;
static ui.Image? imageFactory()=>image;
static double? get width=>48;
static double? widthFactory()=>width;
static double? get height=>32;
static double? heightFactory()=>height;
static double get scale=>2;
static double scaleFactory()=>scale;
static Color? get color=>const Color(0xFF112233);
static Color? colorFactory()=>color;
static Animation<double>? get opacity=>const AlwaysStoppedAnimation<double>(0.25);
static Animation<double>? opacityFactory()=>opacity;
static AlignmentGeometry get alignment=>AlignmentDirectional.topEnd;
static AlignmentGeometry alignmentFactory()=>alignment;
static Rect? get centerSlice=>const Rect.fromLTRB(2,2,14,10);
static Rect? centerSliceFactory()=>centerSlice;
}
""");
  var frameId=StableId.random();var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
  var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""
static ui.Image? get image=>null;
static ui.Image? imageFactory()=>image;
static double? get width=>48;
static double? widthFactory()=>width;
static double? get height=>32;
static double? heightFactory()=>height;
static double get scale=>2;
static double scaleFactory()=>scale;
static Color? get color=>const Color(0xFF112233);
static Color? colorFactory()=>color;
static Animation<double>? get opacity=>const AlwaysStoppedAnimation<double>(0.25);
static Animation<double>? opacityFactory()=>opacity;
static AlignmentGeometry get alignment=>AlignmentDirectional.topEnd;
static AlignmentGeometry alignmentFactory()=>alignment;
static Rect? get centerSlice=>const Rect.fromLTRB(2,2,14,10);
static Rect? centerSliceFactory()=>centerSlice;
ui.Image? get _image=>image;
ui.Image? _imageFactory()=>image;
double? get _width=>width;
double? _widthFactory()=>width;
double? get _height=>height;
double? _heightFactory()=>height;
double get _scale=>scale;
double _scaleFactory()=>scale;
Color? get _color=>color;
Color? _colorFactory()=>color;
Animation<double>? get _opacity=>opacity;
Animation<double>? _opacityFactory()=>opacity;
AlignmentGeometry get _alignment=>alignment;
AlignmentGeometry _alignmentFactory()=>alignment;
Rect? get _centerSlice=>centerSlice;
Rect? _centerSliceFactory()=>centerSlice;
ui.Image? get _nullImage=>null;
Rect? get _nullRect=>null;
Animation<double>? get _nullAnimation=>null;
Animation<dynamic>? get _rawAnimation=>const AlwaysStoppedAnimation<dynamic>(0.5);
Image get _widgetImage=>Image.asset('nope');
ImageProvider<Object> get _provider=>const AssetImage('nope');
String get _wrong=>'wrong';
dynamic get _dynamic=>null;
// User member is preserved.
int _userValue() => 73;

""");
  baseline=reopen(baseline,"import 'dart:ui' as ui;\nimport 'package:flutter/material.dart';\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(RawImageWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));
  assertAnalysis(baseline,base,"probe.dart",true);record("creation",base,"empty");
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());
  assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  base=reopen(base,source(base));
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var patches=new ArrayList<PatchProperties.Patch>();
   for(String name:FIELDS)patches.add(new PatchProperties.SetPatch(new PropertyName(name),reference(name,imported,member,factory)));
   var changed=apply(base,new PatchProperties(NESTED_ID,patches));assertAnalysis(base,changed,"probe.dart",true);record("sources_"+imported+"_"+member+"_"+factory,changed,"source");
  }
  var local=apply(base,new PatchProperties(NESTED_ID,List.of(
   new PatchProperties.SetPatch(new PropertyName("opacity"),new PropertyValue.DoubleValue(new BigDecimal("0.35"))),
   new PatchProperties.SetPatch(new PropertyName("scale"),new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
   new PatchProperties.SetPatch(new PropertyName("debugImageLabel"),new PropertyValue.StringValue("diagnostic")))));
  assertAnalysis(base,local,"probe.dart",true);record("local",local,"local");
  var nullable=new ArrayList<PatchProperties.Patch>();for(var p:RawImageWidgetPropertySchema.properties())if(p.acceptedKinds().contains(PropertyValueKind.NULL))nullable.add(new PatchProperties.SetPatch(p.name(),new PropertyValue.NullValue()));
  var nulls=apply(base,new PatchProperties(NESTED_ID,nullable));assertAnalysis(base,nulls,"probe.dart",true);record("nulls",nulls,"empty");
  for(var e:Map.of("image","_nullImage","opacity","_nullAnimation","centerSlice","_nullRect").entrySet()){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(e.getKey()),ref(e.getValue())));assertAnalysis(base,changed,"probe.dart",true);record("nullable_"+e.getKey().toLowerCase(),changed,"empty");
  }
  for(String field:FIELDS)for(String bad:List.of("_wrong","_dynamic"))assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(field),ref(bad))),"probe.dart",false);
  for(String bad:List.of("_widgetImage","_provider"))assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName("image"),ref(bad))),"probe.dart",false);
  assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName("opacity"),ref("_rawAnimation"))),"probe.dart",false);
  var beforeReset=reopen(local,source(local));var reset=apply(beforeReset,new ResetProperty(NESTED_ID,new PropertyName("opacity")));assertAnalysis(beforeReset,reset,"probe.dart",true);saveCase("reset",reset);
  assertArrayEquals(beforeReset.current().dartCandidateBytes(),reset.undo().session().current().dartCandidateBytes());
  var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  Files.writeString(project.resolve("test/raw_image_test.dart"),runtime());run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private void record(String name,DesignerCommandSession session,String mode)throws Exception{saveCase(name,session);cases.put(name,mode);}
 private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:raw_image_contract/references.dart"):Optional.empty(),
   member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
   member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }
 private String nativeRuntime(){return """
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

class TrackingAnimation extends AnimationController {
 TrackingAnimation({required super.vsync, super.value});
 final listeners=<VoidCallback>{};
 @override void addListener(VoidCallback listener){listeners.add(listener);super.addListener(listener);}
 @override void removeListener(VoidCallback listener){listeners.remove(listener);super.removeListener(listener);}
 bool get hasListeners=>listeners.isNotEmpty;
}
Future<ui.Image> makeImage(WidgetTester tester,Color color) async =>
 (await tester.runAsync(()async {
  final recorder=ui.PictureRecorder();Canvas(recorder).drawRect(const Rect.fromLTWH(0,0,16,12),Paint()..color=color);
  final picture=recorder.endRecording();try{return await picture.toImage(16,12);}finally{picture.dispose();}
 }))!;
Widget host(Widget child)=>MaterialApp(home:Center(child:child));
void nativeTests(){
 testWidgets('decoded image ownership survives replacement and unmount',(tester)async{
  final a=await makeImage(tester,Colors.red),b=await makeImage(tester,Colors.blue);
  expect(a.debugGetOpenHandleStackTraces(),hasLength(1));
  await tester.pumpWidget(host(RawImage(image:a,scale:2)));
  expect(tester.getSize(find.byType(RawImage)),const Size(8,6));expect(a.debugGetOpenHandleStackTraces(),hasLength(2));
  final render=tester.renderObject<RenderImage>(find.byType(RawImage));expect(render.image,isNot(same(a)));expect(render.image!.isCloneOf(a),isTrue);
  await tester.pumpWidget(host(RawImage(image:b,width:32,height:24)));expect(a.debugGetOpenHandleStackTraces(),hasLength(1));expect(b.debugGetOpenHandleStackTraces(),hasLength(2));
  await tester.pumpWidget(const SizedBox());expect(b.debugGetOpenHandleStackTraces(),hasLength(1));a.dispose();b.dispose();expect(tester.takeException(),isNull);
 });
 testWidgets('live opacity listener and all painting fields use native renderer',(tester)async{
  final image=await makeImage(tester,Colors.red);final animation=TrackingAnimation(vsync:tester,value:0.75);
  await tester.pumpWidget(host(Directionality(textDirection:TextDirection.rtl,child:RawImage(image:image,
    debugImageLabel:'not semantics',width:32,height:24,scale:1,color:Colors.green,opacity:animation,
    colorBlendMode:BlendMode.multiply,fit:BoxFit.fill,alignment:AlignmentDirectional.topEnd,repeat:ImageRepeat.repeatX,
    centerSlice:const Rect.fromLTRB(2,2,14,10),matchTextDirection:true,invertColors:true,filterQuality:FilterQuality.high,isAntiAlias:true))));
  final r=tester.renderObject<RenderImage>(find.byType(RawImage));expect(r.opacity,same(animation));expect(r.color,Colors.green);
  expect(r.colorBlendMode,BlendMode.multiply);expect(r.fit,BoxFit.fill);expect(r.alignment,AlignmentDirectional.topEnd);expect(r.repeat,ImageRepeat.repeatX);
  expect(r.centerSlice,const Rect.fromLTRB(2,2,14,10));expect(r.textDirection,TextDirection.rtl);expect(r.matchTextDirection,isTrue);expect(r.invertColors,isTrue);expect(r.isAntiAlias,isTrue);expect(r.filterQuality,FilterQuality.high);
  expect(animation.hasListeners,isTrue);animation.value=0.25;await tester.pump();expect(r.opacity!.value,0.25);expect(tester.takeException(),isNull);
  await tester.pumpWidget(const SizedBox());expect(animation.hasListeners,isFalse);animation.dispose();image.dispose();
 });
 testWidgets('unset image is empty and debug label is not semantics',(tester)async{
  final semantics=tester.ensureSemantics();
  await tester.pumpWidget(host(const RawImage(debugImageLabel:'debug-only',width:30,height:20)));
  expect(tester.getSize(find.byType(RawImage)),const Size(30,20));expect(find.bySemanticsLabel('debug-only'),findsNothing);
  await tester.pumpWidget(host(const RawImage()));expect(tester.getSize(find.byType(RawImage)),Size.zero);
  expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());semantics.dispose();
 });
}

""";}
 private String runtime(){
  runtimeCaseCount=cases.size()*2+3;var imports=new StringBuilder();var tests=new StringBuilder();
  cases.forEach((name,mode)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{")
    .append("await tester.pumpWidget(host(Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:const case_").append(name).append(".Sample())));await tester.pump();")
    .append("final n=tester.widget<RawImage>(find.byType(RawImage));expect(n.image,isNull);expect(find.text('Sibling'),findsOneWidget);");
   if(mode.equals("source"))tests.append("expect(n.width,48);expect(n.height,32);expect(n.scale,2);expect(n.opacity!.value,0.25);expect(n.alignment,AlignmentDirectional.topEnd);expect(n.color,const Color(0xFF112233));expect(n.centerSlice,const Rect.fromLTRB(2,2,14,10));");
   else if(mode.equals("local"))tests.append("expect(n.opacity!.value,0.35);expect(n.scale,2);expect(n.debugImageLabel,'diagnostic');");
   else tests.append("expect(n.opacity,isNull);expect(n.scale,1);expect(n.centerSlice,isNull);");
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
                name: raw_image_contract
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
            System.out.println("RawImage pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
