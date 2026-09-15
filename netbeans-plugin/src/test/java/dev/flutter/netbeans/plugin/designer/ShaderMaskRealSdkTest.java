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
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class ShaderMaskRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void gradientsSourceProofsHandlersAndSavedNativeRuntime()throws Exception {
  initialize();
  Files.writeString(lib.resolve("references.dart"),"""
    import 'package:flutter/widgets.dart';
    ShaderCallback get shader => Values.shader;
    ShaderCallback shaderFactory() => shader;
    class Values {
      static ShaderCallback get shader => (Rect bounds) => const LinearGradient(colors:[Color(0xffff0000),Color(0xff0000ff)]).createShader(bounds);
      static ShaderCallback shaderFactory() => shader;
    }
    """);
  var body=adapter("Sized body",48);
  var baseline=open(group(GROUP_ID,List.of(body,adapter("Sibling",20))),"probe.dart","""
    // User member is preserved.
    int _userValue() => 73;
    static ShaderCallback get shader => (Rect bounds) => const LinearGradient(colors:[Color(0xffff0000),Color(0xff0000ff)]).createShader(bounds);
    static ShaderCallback shaderFactory() => shader;
    ShaderCallback get _shader => shader;
    ShaderCallback _shaderFactory() => shader;
    Shader _direct(Rect bounds) => shader(bounds);
    Shader _broader(Object bounds) => shader(bounds as Rect);
    Shader? _nullableResult(Rect bounds) => null;
    Shader _wrongParameter(int bounds) => shader(Rect.zero);
    Shader _extra(Rect bounds,int extra) => shader(bounds);
    Shader _noParameter() => shader(Rect.zero);
    Shader get _value => shader(Rect.zero);
    ShaderCallback? get _nullableCallback => shader;
    dynamic _dynamicResult(Rect bounds) => shader(bounds);
    Object _wrongResult(Rect bounds) => shader(bounds);
    dynamic get _dynamic => shader;
    """);
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(ShaderMaskWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
  assertAnalysis(baseline,base,"probe.dart",true);
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());
  assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  record("creation",base,"white","modulate",true);
  var rootWrap=apply(baseline,new WrapWidget(GROUP_ID,prototype,new SlotName("child"),0));
  assertAnalysis(baseline,rootWrap,"probe.dart",true);record("root",rootWrap,"white","modulate",true);
  base=reopen(base,source(base));
  for(String family:List.of("linear","radial","sweep")) for(boolean directional:List.of(false,true))
  for(var tile:PropertyValue.BoxDecorationValue.TileMode.values())for(boolean theme:List.of(false,true)){
    var value=gradient(family,directional,tile,theme);
    var local=apply(base,new SetProperty(NESTED_ID,new PropertyName("shaderCallback"),value));
    assertAnalysis(base,local,"probe.dart",true);
    record(family+"_"+directional+"_"+tile.name().toLowerCase()+"_"+theme,local,
      family+"|"+directional+"|"+tile.wireName()+"|"+theme,"modulate",true);
  }
  var themed=apply(base,new SetProperty(NESTED_ID,new PropertyName("shaderCallback"),
    gradient("linear",true,PropertyValue.BoxDecorationValue.TileMode.CLAMP,true)));
  assertAnalysis(base,themed,"probe.dart",true);themed=reopen(themed,source(themed));
  var directionId=StableId.random();
  var direction=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Directionality")).orElseThrow(),
    directionId,Map.of(new PropertyName("textDirection"),new PropertyValue.EnumValue("TextDirection","rtl")));
  var nested=apply(themed,new WrapWidget(NESTED_ID,direction,new SlotName("child"),0));
  assertAnalysis(themed,nested,"probe.dart",true);nested=reopen(nested,source(nested));
  var theme=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.Theme")).orElseThrow(),
    StableId.random(),Map.of(new PropertyName("data"),new PropertyValue.StringValue("dark")));
  var nestedTheme=apply(nested,new WrapWidget(directionId,theme,new SlotName("child"),0));
  assertAnalysis(nested,nestedTheme,"probe.dart",true);record("nearest_context",nestedTheme,"nested","modulate",true);
  for(var blend:PropertyValue.PaintValue.BlendMode.values()){
    var local=apply(base,new SetProperty(NESTED_ID,new PropertyName("blendMode"),new PropertyValue.EnumValue("BlendMode",blend.wireName())));
    assertAnalysis(base,local,"probe.dart",true);record("blend_"+blend.wireName().toLowerCase(),local,"white",blend.wireName(),true);
  }
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
    var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:shader_mask_contract/references.dart"):Optional.empty(),
      member?(imported?"Values":"Storage"):(imported?"shader":"_shader")+(factory?"Factory":""),
      member?Optional.of("shader"+(factory?"Factory":"")):Optional.empty(),
      factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
      factory?Optional.of(false):Optional.empty());
    var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("shaderCallback"),ref));
    assertAnalysis(base,changed,"probe.dart",true);record("source_"+imported+"_"+member+"_"+factory,changed,"source","modulate",true);
  }
  for(String name:List.of("_direct","_broader")){
    var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("shaderCallback"),ref(name,false)));
    assertAnalysis(base,changed,"probe.dart",true);record(name.substring(1),changed,"source","modulate",true);
  }
  var created=apply(base,new CreateEventHandler(NESTED_ID,new PropertyName("shaderCallback"),"_createdShader"));
  assertAnalysis(base,created,"probe.dart",true);assertFalse(source(created).contains("UnimplementedError"));record("created",created,"white","modulate",true);
  created=reopen(created,source(created));
  var renamed=apply(created,new RenameEventHandler(NESTED_ID,new PropertyName("shaderCallback"),"_renamedShader"));
  assertAnalysis(created,renamed,"probe.dart",true);assertFalse(source(renamed).contains("_createdShader"));record("renamed",renamed,"white","modulate",true);
  renamed=reopen(renamed,source(renamed));
  var disconnected=apply(renamed,new SetProperty(NESTED_ID,new PropertyName("shaderCallback"),ShaderMaskWidgetPropertySchema.neutral()));
  assertAnalysis(renamed,disconnected,"probe.dart",true);assertTrue(source(disconnected).contains("Shader _renamedShader(Rect bounds)"));
  record("disconnected",disconnected,"white","modulate",true);
  var moved=apply(base,new MoveWidget(NESTED_ID,new WidgetPlacement(GROUP_ID,CHILDREN,1)));
  assertAnalysis(base,moved,"probe.dart",true);record("moved",moved,"white","modulate",true);
  var empty=apply(base,new RemoveWidget(body.id()));assertAnalysis(base,empty,"probe.dart",true);record("empty",empty,"white","modulate",false);
  var replaced=apply(base,new ReplaceSlotChild(NESTED_ID,new SlotName("child"),body.id(),new ReplaceSlotChild.NewSubtree(adapter("Sized body",48))));
  assertAnalysis(base,replaced,"probe.dart",true);record("replaced",replaced,"white","modulate",true);
  assertFalse(base.apply(new ResetProperty(NESTED_ID,new PropertyName("shaderCallback"))).changed());
  for(String name:List.of("_nullableResult","_wrongParameter","_extra","_noParameter","_value","_nullableCallback","_dynamicResult","_wrongResult","_dynamic")){
    var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("shaderCallback"),ref(name,false)));
    assertAnalysis(base,changed,"probe.dart",false);
  }
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("shader_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private void record(String name,DesignerCommandSession session,String gradient,String blend,boolean child)throws Exception {
  saveCase(name,session);cases.put(name,gradient+";"+blend+";"+child);
 }
 private String runtime(){
  runtimeCaseCount=cases.size()*2;
  var imports=new StringBuilder("""
    import 'dart:ui' as ui;
    import 'dart:typed_data';
    import 'package:flutter/material.dart';
    import 'package:flutter_test/flutter_test.dart';
    """);
  var tests=new StringBuilder();
  cases.forEach((name,data)->{
    var info=data.split(";");
    imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
    tests.append("for(final rtl in [false,true]){testWidgets('saved ").append(name).append(" rtl=$rtl',(tester)async{\n")
      .append("await tester.pumpWidget(host(const case_").append(name).append(".Sample(),rtl,Colors.purple));")
      .append("var n=tester.widget<ShaderMask>(find.byType(ShaderMask));expect(n.blendMode,BlendMode.").append(info[1]).append(");")
      .append("expect(n.child==null,").append(!Boolean.parseBoolean(info[2])).append(");")
      .append("for(final bounds in [const Rect.fromLTWH(0,0,48,32),const Rect.fromLTWH(4,8,96,64)]){")
      .append("expect(await tester.runAsync(()=>raster(n.shaderCallback(bounds),bounds)),await tester.runAsync(()=>raster(expected('").append(info[0]).append("',rtl,Colors.purple).createShader(bounds,textDirection:rtl?TextDirection.rtl:TextDirection.ltr),bounds)));}")
      .append("final element=tester.element(find.byType(ShaderMask));await tester.pumpWidget(host(const case_").append(name).append(".Sample(),!rtl,Colors.orange));")
      .append("expect(identical(element,tester.element(find.byType(ShaderMask))),true);n=tester.widget<ShaderMask>(find.byType(ShaderMask));")
      .append("const bounds=Rect.fromLTWH(0,0,64,48);expect(await tester.runAsync(()=>raster(n.shaderCallback(bounds),bounds)),await tester.runAsync(()=>raster(expected('").append(info[0]).append("',!rtl,Colors.orange).createShader(bounds,textDirection:!rtl?TextDirection.rtl:TextDirection.ltr),bounds)));")
      .append("expect(tester.takeException(),isNull);});}\n");
  });
  return imports+"""
    Widget host(Widget child,bool rtl,Color color)=>MaterialApp(home:Theme(data:ThemeData(colorScheme:ColorScheme.light(primary:color)),child:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:240,height:160,child:child)))));
    Gradient expected(String spec,bool rtl,Color color){
      if(spec=='nested')return LinearGradient(begin:Alignment.centerRight,end:Alignment.centerLeft,
        colors:[ThemeData.dark().colorScheme.primary,const Color(0x8000ff00),const Color(0x000000ff)],
        stops:const[0,.5,1],transform:const GradientRotation(-.25));
      if(spec=='white')return const LinearGradient(colors:[Colors.white,Colors.white]);
      if(spec=='source')return const LinearGradient(colors:[Color(0xffff0000),Color(0xff0000ff)]);
      final p=spec.split('|'),directional=p[1]=='true';
      AlignmentGeometry align(double x)=>directional?AlignmentDirectional(x,0):Alignment(x,0);
      final colors=[p[3]=='true'?color:const Color(0xffff0000),const Color(0x8000ff00),const Color(0x000000ff)];
      final tile=TileMode.values.byName(p[2]);const rotation=GradientRotation(-.25);const stops=[0.0,.5,1.0];
      return switch(p[0]){
        'linear'=>LinearGradient(begin:align(-1),end:align(1),colors:colors,stops:stops,tileMode:tile,transform:rotation),
        'radial'=>RadialGradient(center:align(0),radius:.75,focal:align(-1),focalRadius:.05,colors:colors,stops:stops,tileMode:tile,transform:rotation),
        _=>SweepGradient(center:align(0),startAngle:-.5,endAngle:5.5,colors:colors,stops:stops,tileMode:tile,transform:rotation)
      };
    }
    Future<Uint8List> raster(ui.Shader shader,Rect bounds)async{
      final recorder=ui.PictureRecorder();final canvas=Canvas(recorder);canvas.drawRect(bounds,Paint()..shader=shader);
      final picture=recorder.endRecording();final image=await picture.toImage(104,80);
      final data=(await image.toByteData(format:ui.ImageByteFormat.rawRgba))!.buffer.asUint8List();
      final copy=Uint8List.fromList(data);image.dispose();picture.dispose();shader.dispose();return copy;
    }
    void main(){
    """+tests+"}\n";
 }
    public static PropertyValue.GradientValue gradient(String family, boolean directional, PropertyValue.BoxDecorationValue.TileMode tile, boolean theme) {
        var basis = directional ? PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL : PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL;
        var begin = new PropertyValue.AlignmentGeometryValue(basis, BigDecimal.valueOf(-1), BigDecimal.ZERO);
        var end = new PropertyValue.AlignmentGeometryValue(basis, BigDecimal.ONE, BigDecimal.ZERO);
        var center = new PropertyValue.AlignmentGeometryValue(basis, BigDecimal.ZERO, BigDecimal.ZERO);
        var stops = List.of(
                new PropertyValue.BoxDecorationValue.GradientStop(StableId.random(), theme ? new ColorSource.Theme(new ThemeToken("material.colorScheme.primary")) : new ColorSource.Literal(0xffff0000L), BigDecimal.ZERO),
                new PropertyValue.BoxDecorationValue.GradientStop(StableId.random(), new ColorSource.Literal(0x8000ff00L), new BigDecimal(".5")),
                new PropertyValue.BoxDecorationValue.GradientStop(StableId.random(), new ColorSource.Literal(0x000000ffL), BigDecimal.ONE));
        var rotation = Optional.of(new BigDecimal("-.25"));
        return new PropertyValue.GradientValue(switch (family) {
            case "linear" -> new PropertyValue.BoxDecorationValue.LinearGradient(begin, end, stops, tile, rotation);
            case "radial" -> new PropertyValue.BoxDecorationValue.RadialGradient(center, new BigDecimal(".75"), Optional.of(begin), new BigDecimal(".05"), stops, tile, rotation);
            case "sweep" -> new PropertyValue.BoxDecorationValue.SweepGradient(center, new BigDecimal("-.5"), new BigDecimal("5.5"), stops, tile, rotation);
            default -> throw new IllegalArgumentException(family);
        });
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
                name: shader_mask_contract
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
            System.out.println("ShaderMask pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
