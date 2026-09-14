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
import java.math.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class ImageFilteredRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void sixFactoriesStrictSourcesDraftsHistoryAndNativePixels()throws Exception{
  initialize();
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),
    "import 'dart:ui' as ui;\nimport 'package:flutter/material.dart';\nimport 'package:flutter/rendering.dart';\nimport 'package:flutter_test/flutter_test.dart';\n"+nativeRuntime()+"\nvoid main()=>nativeTests();\n");
  runtimeCaseCount=3;run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");Files.delete(project.resolve("test/native_test.dart"));
  Files.writeString(lib.resolve("references.dart"),"""
import 'dart:ui' as ui;
import 'dart:typed_data';
import 'package:flutter/material.dart';
ui.ImageFilter get imageFilter => const ColorFilter.mode(Color(0xFF225588), BlendMode.src);
ui.ImageFilter imageFilterFactory() => imageFilter;
ui.Rect? get bounds => const ui.Rect.fromLTWH(1,2,20,30);
ui.Rect? boundsFactory() => bounds;
Float64List get matrix4 => Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]);
Float64List matrix4Factory() => matrix4;
ui.ImageFilter get inner => const ColorFilter.mode(Color(0xFFFF0000), BlendMode.src);
ui.ImageFilter innerFactory() => inner;
ui.ImageFilter get outer => const ColorFilter.mode(Color(0xFF00FF00), BlendMode.src);
ui.ImageFilter outerFactory() => outer;
ui.FragmentShader get shader => throw StateError('Shader is source-owned');
ui.FragmentShader shaderFactory() => shader;
class Values {
static ui.ImageFilter get imageFilter => const ColorFilter.mode(Color(0xFF225588), BlendMode.src);
static ui.ImageFilter imageFilterFactory() => imageFilter;
static ui.Rect? get bounds => const ui.Rect.fromLTWH(1,2,20,30);
static ui.Rect? boundsFactory() => bounds;
static Float64List get matrix4 => Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]);
static Float64List matrix4Factory() => matrix4;
static ui.ImageFilter get inner => const ColorFilter.mode(Color(0xFFFF0000), BlendMode.src);
static ui.ImageFilter innerFactory() => inner;
static ui.ImageFilter get outer => const ColorFilter.mode(Color(0xFF00FF00), BlendMode.src);
static ui.ImageFilter outerFactory() => outer;
static ui.FragmentShader get shader => throw StateError('Shader is source-owned');
static ui.FragmentShader shaderFactory() => shader;
}
""");
  var frameId=StableId.random();var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
  var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""
// User member is preserved.
int _userValue() => 73;
static ui.ImageFilter get imageFilter => const ColorFilter.mode(Color(0xFF225588), BlendMode.src);
static ui.ImageFilter imageFilterFactory() => imageFilter;
static ui.Rect? get bounds => const ui.Rect.fromLTWH(1,2,20,30);
static ui.Rect? boundsFactory() => bounds;
static Float64List get matrix4 => Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]);
static Float64List matrix4Factory() => matrix4;
static ui.ImageFilter get inner => const ColorFilter.mode(Color(0xFFFF0000), BlendMode.src);
static ui.ImageFilter innerFactory() => inner;
static ui.ImageFilter get outer => const ColorFilter.mode(Color(0xFF00FF00), BlendMode.src);
static ui.ImageFilter outerFactory() => outer;
static ui.FragmentShader get shader => throw StateError('Shader is source-owned');
static ui.FragmentShader shaderFactory() => shader;
ui.ImageFilter get _imageFilter => imageFilter;
ui.ImageFilter _imageFilterFactory() => imageFilter;
ui.Rect? get _bounds => bounds;
ui.Rect? _boundsFactory() => bounds;
Float64List get _matrix4 => matrix4;
Float64List _matrix4Factory() => matrix4;
ui.ImageFilter get _inner => inner;
ui.ImageFilter _innerFactory() => inner;
ui.ImageFilter get _outer => outer;
ui.ImageFilter _outerFactory() => outer;
ui.FragmentShader get _shader => shader;
ui.FragmentShader _shaderFactory() => shader;
Object get _wrong=>Object();
dynamic get _dynamic=>null;
ui.ImageFilter? get _nullableFilter=>null;
ui.FragmentShader? get _nullableShader=>null;
Float64List? get _nullableMatrix=>null;
Uint8List get _wrongMatrix=>Uint8List(16);
""");
  baseline=reopen(baseline,"import 'dart:ui' as ui;\nimport 'dart:typed_data';\nimport 'package:flutter/material.dart';\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));assertAnalysis(baseline,base,"probe.dart",true);record("creation",base,"blur");
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  base=reopen(base,source(base));
  var withChild=apply(base,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),adapter("Filtered child",32)));assertAnalysis(base,withChild,"probe.dart",true);base=reopen(withChild,source(withChild));
  for(String preset:ImageFilteredWidgetPropertySchema.FILTERS){
   if(preset.equals("shader"))continue;
   var changed=base;if(!preset.equals("blur")){changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("imageFilter"),new PropertyValue.StringValue(preset)));assertAnalysis(base,changed,"probe.dart",true);}
   record("local_"+preset,changed,preset);
  }
  for(String field:List.of("imageFilter","bounds","matrix4","inner","outer","shader"))for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var patches=new ArrayList<PatchProperties.Patch>();if(!field.equals("imageFilter"))patches.add(new PatchProperties.SetPatch(new PropertyName("imageFilter"),new PropertyValue.StringValue(mode(field))));
   patches.add(new PatchProperties.SetPatch(new PropertyName(field),reference(field,imported,member,factory)));
   var changed=apply(base,new PatchProperties(NESTED_ID,patches));assertAnalysis(base,changed,"probe.dart",true);
   if(!field.equals("shader"))record("source_"+field.toLowerCase()+"_"+imported+"_"+member+"_"+factory,changed,field+"Source");
  }
  for(String field:List.of("imageFilter","bounds","matrix4","inner","outer","shader"))for(String bad:List.of("_wrong","_dynamic")){
   var patches=new ArrayList<PatchProperties.Patch>();if(!field.equals("imageFilter"))patches.add(new PatchProperties.SetPatch(new PropertyName("imageFilter"),new PropertyValue.StringValue(mode(field))));
   patches.add(new PatchProperties.SetPatch(new PropertyName(field),ref(bad)));
   var changed=apply(base,new PatchProperties(NESTED_ID,patches));assertAnalysis(base,changed,"probe.dart",false);
  }
  for(var invalid:Map.of("imageFilter","_nullableFilter","shader","_nullableShader","matrix4","_nullableMatrix").entrySet()){
   var patches=new ArrayList<PatchProperties.Patch>();if(!invalid.getKey().equals("imageFilter"))patches.add(new PatchProperties.SetPatch(new PropertyName("imageFilter"),new PropertyValue.StringValue(mode(invalid.getKey()))));
   patches.add(new PatchProperties.SetPatch(new PropertyName(invalid.getKey()),ref(invalid.getValue())));assertAnalysis(base,apply(base,new PatchProperties(NESTED_ID,patches)),"probe.dart",false);
  }
  var bounds=apply(base,new PatchProperties(NESTED_ID,List.of(new PatchProperties.SetPatch(new PropertyName("boundsLeft"),new PropertyValue.IntegerValue(BigInteger.ONE.negate())),new PatchProperties.SetPatch(new PropertyName("boundsWidth"),new PropertyValue.IntegerValue(BigInteger.TEN)))));
  assertAnalysis(base,bounds,"probe.dart",true);record("local_bounds",bounds,"localBounds");bounds=reopen(bounds,source(bounds));
  var matrix=apply(base,new SetProperty(NESTED_ID,new PropertyName("imageFilter"),new PropertyValue.StringValue("matrix")));assertAnalysis(base,matrix,"probe.dart",true);matrix=reopen(matrix,source(matrix));
  var inactive=apply(matrix,new SetProperty(NESTED_ID,new PropertyName("bounds"),ref("_dynamic")));assertAnalysis(matrix,inactive,"probe.dart",true);record("inactive",inactive,"matrix");
  inactive=reopen(inactive,source(inactive));assertAnalysis(inactive,apply(inactive,new SetProperty(NESTED_ID,new PropertyName("imageFilter"),new PropertyValue.StringValue("blur"))),"probe.dart",false);
  var override=apply(inactive,new SetProperty(NESTED_ID,new PropertyName("imageFilter"),ref("_imageFilter")));assertAnalysis(inactive,override,"probe.dart",true);record("override",override,"imageFilterSource");
  var disabled=apply(bounds,new SetProperty(NESTED_ID,new PropertyName("enabled"),new PropertyValue.BooleanValue(false)));assertAnalysis(bounds,disabled,"probe.dart",true);record("disabled",disabled,"disabled");
  disabled=reopen(disabled,source(disabled));var reset=apply(disabled,new ResetProperty(NESTED_ID,new PropertyName("enabled")));assertAnalysis(disabled,reset,"probe.dart",true);assertArrayEquals(disabled.current().dartCandidateBytes(),reset.undo().session().current().dartCandidateBytes());
  assertFalse(base.apply(new SetProperty(NESTED_ID,new PropertyName("imageFilter"),new PropertyValue.StringValue("shader"))).changed());
  assertFalse(base.apply(new ResetProperty(NESTED_ID,new PropertyName("imageFilter"))).changed());
  var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  Files.writeString(project.resolve("test/image_filtered_test.dart"),runtime());run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 @Test void shaderBackendCapabilityHasNativeErrorContract()throws Exception{
  initialize();
  Files.writeString(project.resolve("pubspec.yaml"),Files.readString(project.resolve("pubspec.yaml"))+"\nflutter:\n  shaders:\n    - shaders/filter.frag\n");
  Files.writeString(Files.createDirectories(project.resolve("shaders")).resolve("filter.frag"),"""
#include <flutter/runtime_effect.glsl>
uniform vec2 u_size;
uniform sampler2D u_texture;
out vec4 frag_color;
void main() { frag_color = texture(u_texture, FlutterFragCoord().xy / u_size); }
""");
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("shader_test.dart"),"""
import 'dart:ui' as ui;
import 'package:flutter_test/flutter_test.dart';
void main(){
 testWidgets('native shader backend contract',(tester)async{
  final program=await tester.runAsync(()=>ui.FragmentProgram.fromAsset('shaders/filter.frag'));
  final shader=program!.fragmentShader();
  try{
   if(ui.ImageFilter.isShaderFilterSupported){
    expect(()=>ui.ImageFilter.shader(shader),returnsNormally);
   }else{
    expect(()=>ui.ImageFilter.shader(shader),throwsUnsupportedError);
   }
  }finally{shader.dispose();}
 });
}
""");
  runtimeCaseCount=1;run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private static String mode(String field){return switch(field){case "matrix4"->"matrix";case "inner","outer"->"compose";case "shader"->"shader";default->"blur";};}
 private void record(String name,DesignerCommandSession session,String mode)throws Exception{saveCase(name,session);cases.put(name,mode);}
 private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:image_filtered_contract/references.dart"):Optional.empty(),
   member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
   member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }
 private static String nativeRuntime(){return """
Future<List<int>> pixel(
  WidgetTester tester,
  ui.ImageFilter filter,
  int x,
  int y, {
  bool enabled = true,
}) async {
  final key = GlobalKey();
  await tester.pumpWidget(
    Directionality(
      textDirection: TextDirection.ltr,
      child: Center(
        child: RepaintBoundary(
          key: key,
          child: SizedBox(
            width: 64,
            height: 64,
            child: Center(
              child: ImageFiltered(
                imageFilter: filter,
                enabled: enabled,
                child: const ColoredBox(
                  color: Color(0xFF0000FF),
                  child: SizedBox(width: 16, height: 16),
                ),
              ),
            ),
          ),
        ),
      ),
    ),
  );
  final boundary =
      key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
  final bytes = await tester.runAsync(() async {
    final image = await boundary.toImage();
    try {
      return await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    } finally {
      image.dispose();
    }
  });
  final i = (y * 64 + x) * 4;
  return [for (int n = 0; n < 4; n++) bytes!.getUint8(i + n)];
}

void nativeTests() {
  testWidgets('pixels blur morphology matrix and Enabled bypass', (
    tester,
  ) async {
    expect((await pixel(tester, ui.ImageFilter.blur(), 23, 32))[3], 0);
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.blur(sigmaX: 2, sigmaY: 2),
        23,
        32,
      ))[3],
      greaterThan(0),
    );
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.blur(sigmaX: 2, sigmaY: 2),
        23,
        32,
        enabled: false,
      ))[3],
      0,
    );
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.dilate(radiusX: 2, radiusY: 2),
        23,
        32,
      ))[3],
      255,
    );
    expect(
      (await pixel(
        tester,
        ui.ImageFilter.erode(radiusX: 2, radiusY: 2),
        24,
        32,
      ))[3],
      0,
    );
    final matrix = Matrix4.translationValues(8, 0, 0);
    final filter = ui.ImageFilter.matrix(
      matrix.storage,
      filterQuality: FilterQuality.none,
    );
    matrix.storage[12] = 0;
    expect((await pixel(tester, filter, 25, 32))[3], 0);
    expect(await pixel(tester, filter, 40, 32), [0, 0, 255, 255]);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
  testWidgets(
    'compose preserves outer after inner including ColorFilter subtype',
    (tester) async {
      const red = ColorFilter.mode(Color(0xFFFF0000), BlendMode.src);
      const green = ColorFilter.mode(Color(0xFF00FF00), BlendMode.src);
      expect(
        await pixel(
          tester,
          ui.ImageFilter.compose(outer: green, inner: red),
          32,
          32,
        ),
        [0, 255, 0, 255],
      );
      expect(
        await pixel(
          tester,
          ui.ImageFilter.compose(outer: red, inner: green),
          32,
          32,
        ),
        [255, 0, 0, 255],
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'layout taps semantics and render identity survive disabling filter',
    (tester) async {
      int taps = 0;
      final semantics = tester.ensureSemantics();
      Widget view(bool enabled) => Directionality(
        textDirection: TextDirection.rtl,
        child: Center(
          child: ImageFiltered(
            imageFilter: ui.ImageFilter.blur(sigmaX: 3),
            enabled: enabled,
            child: Semantics(
              label: 'Filtered child',
              button: true,
              child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                onTap: () {
                  taps++;
                },
                child: const SizedBox(width: 64, height: 32),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(view(true));
      final render = tester.renderObject<RenderObject>(
        find.byType(ImageFiltered),
      );
      expect(tester.getSize(find.byType(ImageFiltered)), const Size(64, 32));
      expect(render.needsCompositing, isTrue);
      expect(find.bySemanticsLabel('Filtered child'), findsOneWidget);
      await tester.tap(find.byType(ImageFiltered));
      expect(taps, 1);
      await tester.pumpWidget(view(false));
      expect(tester.renderObject(find.byType(ImageFiltered)), same(render));
      expect(render.needsCompositing, isFalse);
      expect(tester.getSize(find.byType(ImageFiltered)), const Size(64, 32));
      await tester.tap(find.byType(ImageFiltered));
      expect(taps, 2);
      expect(find.bySemanticsLabel('Filtered child'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
      semantics.dispose();
    },
  );
}

""";}
 private String runtime(){
  runtimeCaseCount=cases.size()*2+3;var imports=new StringBuilder();var tests=new StringBuilder();
  cases.forEach((name,mode)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{")
    .append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:const case_").append(name).append(".Sample())));await tester.pump();")
    .append("final n=tester.widget<ImageFiltered>(find.byType(ImageFiltered));expect(find.text('Sibling'),findsOneWidget);");
   String expected=switch(mode){
    case "imageFilterSource"->"const ColorFilter.mode(Color(0xFF225588),BlendMode.src)";
    case "boundsSource"->"ui.ImageFilter.blur(sigmaX:0,sigmaY:0,bounds:const ui.Rect.fromLTWH(1,2,20,30))";
    case "matrix4Source"->"ui.ImageFilter.matrix(Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]))";
    case "innerSource"->"ui.ImageFilter.compose(inner:const ColorFilter.mode(Color(0xFFFF0000),BlendMode.src),outer:ui.ImageFilter.blur())";
    case "outerSource"->"ui.ImageFilter.compose(inner:ui.ImageFilter.blur(),outer:const ColorFilter.mode(Color(0xFF00FF00),BlendMode.src))";
    case "dilate"->"ui.ImageFilter.dilate()";case "erode"->"ui.ImageFilter.erode()";
    case "matrix"->"ui.ImageFilter.matrix(Matrix4.identity().storage)";
    case "compose"->"ui.ImageFilter.compose(inner:ui.ImageFilter.blur(),outer:ui.ImageFilter.blur())";
    case "localBounds","disabled"->"ui.ImageFilter.blur(bounds:const ui.Rect.fromLTWH(-1,0,10,0))";
    default->"ui.ImageFilter.blur()";
   };
   tests.append("expect(n.imageFilter,").append(expected).append(");expect(n.enabled,").append(!mode.equals("disabled")).append(");");
   if(!name.equals("creation"))tests.append("expect(find.text('Filtered child'),findsOneWidget);");
   tests.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());});}\n");
  });
  return "import 'dart:ui' as ui;\nimport 'dart:typed_data';\nimport 'package:flutter/material.dart';\nimport 'package:flutter/rendering.dart';\nimport 'package:flutter_test/flutter_test.dart';\n"+imports+nativeRuntime()+"\nvoid main(){nativeTests();"+tests+"}\n";
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
                name: image_filtered_contract
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
            System.out.println("ImageFiltered pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
