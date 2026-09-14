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
class DecoratedBoxTransitionRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void completeLocalSourceAndNativeLifecycle()throws Exception{
  initialize();
  Files.writeString(lib.resolve("references.dart"),"""
    import 'package:flutter/widgets.dart';
    Animation<Decoration> get decoration=>Values.current;
    Animation<Decoration> decorationFactory()=>Values.current;
    class Values {
      static Animation<Decoration> current=const AlwaysStoppedAnimation<Decoration>(BoxDecoration(color:Color(0xFF123456)));
      static Animation<Decoration> decorationFactory()=>current;
    }
    """);
  var body=adapter("Sized body",100);
  var baseline=open(group(GROUP_ID,List.of(body,adapter("Sibling",20))),"probe.dart","""
    // User member is preserved.
    int _userValue() => 73;
    void onImageError(Object error,StackTrace? stack) {}
    static Animation<Decoration> current=const AlwaysStoppedAnimation<Decoration>(BoxDecoration(color:Color(0xFF123456)));
    static Animation<Decoration> decorationFactory()=>current;
    Animation<Decoration> get _decoration=>current;
    Animation<Decoration> _decorationFactory()=>current;
    Animation<BoxDecoration> get _box=>const AlwaysStoppedAnimation<BoxDecoration>(BoxDecoration(color:Color(0xFF123456)));
    Animation<ShapeDecoration> get _shape=>const AlwaysStoppedAnimation<ShapeDecoration>(ShapeDecoration(color:Color(0xFF123456),shape:CircleBorder()));
    Animation<Decoration>? get _nullableOuter=>current;
    Animation<Decoration?> get _nullableValue=>const AlwaysStoppedAnimation<Decoration?>(null);
    Decoration get _plain=>const BoxDecoration();
    Animation<Color> get _wrong=>const AlwaysStoppedAnimation<Color>(Color(0xFF123456));
    Animation<dynamic> get _dynamicAnimation=>const AlwaysStoppedAnimation<dynamic>('bad');
    ValueNotifier<Decoration> get _notAnimation=>ValueNotifier<Decoration>(const BoxDecoration());
    dynamic get _dynamic=>current;
    """);
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(DecoratedBoxTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var wrapped=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
  assertAnalysis(baseline,wrapped,"probe.dart",true);
  assertArrayEquals(baseline.current().dartCandidateBytes(),wrapped.undo().session().current().dartCandidateBytes());
  assertArrayEquals(wrapped.current().dartCandidateBytes(),wrapped.undo().session().redo().session().current().dartCandidateBytes());
  recordCase("creation",wrapped,"const BoxDecoration()",false);
  wrapped=reopen(wrapped,source(wrapped));
  for(boolean foreground:List.of(false,true))for(boolean theme:List.of(false,true)){
   ColorSource color=theme?new ColorSource.Theme(new ThemeToken("material.colorScheme.primary")):new ColorSource.Literal(0xff445566L);
   var decoration=new PropertyValue.BoxDecorationValue(Optional.of(color),Optional.empty(),Optional.empty(),List.of(),Optional.empty(),Optional.empty(),PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
   var local=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
     new PatchProperties.SetPatch(new PropertyName("decoration"),decoration),
     new PatchProperties.SetPatch(new PropertyName("position"),new PropertyValue.EnumValue("DecorationPosition",foreground?"foreground":"background")))));
   assertAnalysis(wrapped,local,"probe.dart",true);recordCase("local_"+foreground+"_"+theme,local,theme?"BoxDecoration(color:Theme.of(tester.element(native())).colorScheme.primary)":"const BoxDecoration(color:Color(0xFF445566))",foreground);
  }
  Files.createDirectories(project.resolve("assets/images"));
  var testImage=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
  javax.imageio.ImageIO.write(testImage,"png",project.resolve("assets/images/logo.png").toFile());
        PropertyValue.ImageProviderValue provider =
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET,
                        "assets/images/logo.png",
                        Optional.empty(),
                        Optional.of(new BigDecimal("2")),
                        Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(
                                Optional.of(512), Optional.of(256),
                                PropertyValue.ImageProviderValue.ResizePolicy.FIT,
                                true)));
        PropertyValue.DecorationImageValue image =
                new PropertyValue.DecorationImageValue(
                        provider,
                        Optional.of(new PropertyValue.CallbackValue("onImageError")),
                        Optional.of(new PropertyValue.DecorationImageValue.Mode(
                                new ColorSource.Theme(new ThemeToken(
                                        "material.colorScheme.primary")),
                                PropertyValue.PaintValue.BlendMode.SRC_IN)),
                        Optional.of(PropertyValue.DecorationImageValue.BoxFit.CONTAIN),
                        new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,new BigDecimal("0.25"),new BigDecimal("-0.5")),
                        Optional.of(new PropertyValue.DecorationImageValue.Rect(
                                BigDecimal.ONE, BigDecimal.TWO,
                                new BigDecimal("20"), new BigDecimal("30"))),
                        PropertyValue.DecorationImageValue.ImageRepeat.REPEAT_X,
                        true,
                        new BigDecimal("1.5"),
                        new BigDecimal("0.75"),
                        PropertyValue.PaintValue.FilterQuality.HIGH,
                        true,
                        true);
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(new ColorSource.Literal(0xFF010203L)),
                        Optional.of(image),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);

  var imageCase=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("decoration"),decoration));
  assertAnalysis(wrapped,imageCase,"probe.dart",true);saveCase("image",imageCase);
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("decoration"),reference(imported,member,factory)));
   assertAnalysis(wrapped,changed,"probe.dart",true);recordCase("source_"+imported+"_"+member+"_"+factory,changed,"const BoxDecoration(color:Color(0xFF123456))",false);
  }
  for(String valid:List.of("_box","_shape")){
   var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("decoration"),ref(valid,false)));
   assertAnalysis(wrapped,changed,"probe.dart",true);recordCase(valid.substring(1),changed,valid.equals("_box")?"const BoxDecoration(color:Color(0xFF123456))":"const ShapeDecoration(color:Color(0xFF123456),shape:CircleBorder())",false);
  }
  var live=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("decoration"),reference(true,true,false)));
  assertAnalysis(wrapped,live,"probe.dart",true);recordCase("live",live,"const BoxDecoration(color:Color(0xFF123456))",false);
  live=reopen(live,source(live));
  var reset=apply(live,new SetProperty(NESTED_ID,new PropertyName("decoration"),DecoratedBoxTransitionWidgetPropertySchema.emptyDecoration()));
  assertAnalysis(live,reset,"probe.dart",true);recordCase("local_again",reset,"const BoxDecoration()",false);
  var moved=apply(wrapped,new MoveWidget(NESTED_ID,new WidgetPlacement(GROUP_ID,CHILDREN,1)));
  assertAnalysis(wrapped,moved,"probe.dart",true);recordCase("moved",moved,"const BoxDecoration()",false);
  var replaced=apply(wrapped,new ReplaceSlotChild(NESTED_ID,new SlotName("child"),body.id(),new ReplaceSlotChild.NewSubtree(adapter("Sized body",100))));
  assertAnalysis(wrapped,replaced,"probe.dart",true);recordCase("replaced",replaced,"const BoxDecoration()",false);
  assertFalse(wrapped.apply(new RemoveWidget(body.id())).changed());
  assertFalse(wrapped.apply(new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,CHILDREN,2))).changed());
  assertFalse(wrapped.apply(new ResetProperty(NESTED_ID,new PropertyName("decoration"))).changed());
  for(String bad:List.of("_plain","_nullableOuter","_nullableValue","_wrong","_dynamicAnimation","_notAnimation","_dynamic"))
    assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("decoration"),ref(bad,false))),"probe.dart",false);
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("decoration_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private static PropertyValue.DartObjectReferenceValue reference(boolean imported,boolean member,boolean factory){
   return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:decorated_box_transition_contract/references.dart"):Optional.empty(),
     member?(imported?"Values":"Storage"):(imported?"":"_")+"decoration"+(factory?"Factory":""),
     member?Optional.of(factory?"decorationFactory":"current"):Optional.empty(),
     factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
     factory?Optional.of(false):Optional.empty());
 }
 private void recordCase(String name,DesignerCommandSession session,String expected,boolean foreground)throws Exception{
   saveCase(name,session);cases.put(name,expected+"|"+foreground);
 }
 private String runtime(){
  runtimeCaseCount=cases.size()*2+8;
  var imports=new StringBuilder("""
    import '../lib/image.dart' as case_image;
    import 'dart:ui' as ui;
    import 'package:flutter/material.dart';
    import 'package:flutter/rendering.dart';
    import 'package:flutter_test/flutter_test.dart';
    import 'package:decorated_box_transition_contract/references.dart' as refs;
    """);
  var tests=new StringBuilder();
  cases.forEach((name,values)->{
    imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
    int split=values.lastIndexOf('|');var expected=values.substring(0,split);boolean foreground=Boolean.parseBoolean(values.substring(split+1));
    tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{\n")
      .append("await tester.pumpWidget(host(const case_").append(name).append(".Sample(),rtl:rtl));await tester.pumpAndSettle();")
      .append("expect(tester.widget<DecoratedBoxTransition>(native()).decoration.value,").append(expected).append(");")
      .append("expect(tester.widget<DecoratedBoxTransition>(native()).position,DecorationPosition.").append(foreground?"foreground":"background").append(");")
      .append("expect(tester.getSize(native()),const Size(100,100));expect(find.text('Sized body'),findsOneWidget);expect(tester.takeException(),isNull);});}\n");
  });
  return imports+"""
    Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:240,height:160,child:child))));
    Finder native()=>find.byType(DecoratedBoxTransition);
    class ProbeAnimation extends Animation<Decoration>{
      ProbeAnimation(this._value);Decoration _value;final listeners=<VoidCallback>{};
      @override Decoration get value=>_value;
      void update(Decoration value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
      @override AnimationStatus get status=>AnimationStatus.forward;
      @override void addListener(VoidCallback f)=>listeners.add(f);
      @override void removeListener(VoidCallback f)=>listeners.remove(f);
      @override void addStatusListener(AnimationStatusListener f){}
      @override void removeStatusListener(AnimationStatusListener f){}
    }
    class CustomDecoration extends Decoration{
      const CustomDecoration(this.color);final Color color;
      @override BoxPainter createBoxPainter([VoidCallback? onChanged])=>CustomPainter(color,onChanged);
    }
    class CustomPainter extends BoxPainter{
      CustomPainter(this.color,super.onChanged);final Color color;
      @override void paint(Canvas canvas,Offset offset,ImageConfiguration configuration){
        canvas.drawRect(offset&configuration.size!,Paint()..color=color);
      }
    }
    Future<Color> pixel(WidgetTester tester,Offset offset)async=> (await tester.runAsync(()async{
      final boundary=tester.renderObject<RenderRepaintBoundary>(find.byKey(const ValueKey('pixels')));
      final image=await boundary.toImage();final bytes=(await image.toByteData(format:ui.ImageByteFormat.rawRgba))!;
      final index=(offset.dy.toInt()*image.width+offset.dx.toInt())*4;
      final color=Color.fromARGB(bytes.getUint8(index+3),bytes.getUint8(index),bytes.getUint8(index+1),bytes.getUint8(index+2));
      image.dispose();return color;
    }))!;
    void main(){
    """+tests+"""
    testWidgets('generated complete image nested symbols, theme and source callback',(tester)async{
      await tester.pumpWidget(host(const case_image.Sample()));await tester.pumpAndSettle();
      final decoration=tester.widget<DecoratedBoxTransition>(native()).decoration.value as BoxDecoration;
      final image=decoration.image!;expect(image.image,isA<ResizeImage>());expect(image.onError,isNotNull);
      expect(image.colorFilter,ColorFilter.mode(Theme.of(tester.element(native())).colorScheme.primary,BlendMode.srcIn));
      expect(image.fit,BoxFit.contain);expect(image.alignment,const AlignmentDirectional(.25,-.5));
      expect(image.centerSlice,const Rect.fromLTRB(1,2,20,30));expect(image.repeat,ImageRepeat.repeatX);
      expect(image.matchTextDirection,true);expect(image.scale,1.5);expect(image.opacity,.75);
      expect(image.filterQuality,FilterQuality.high);expect(image.invertColors,true);expect(image.isAntiAlias,true);
      expect(tester.takeException(),isNull);
    });
    testWidgets('live animation replacement detaches listener and keeps native state and child',(tester)async{
      final first=ProbeAnimation(const BoxDecoration(color:Colors.red)),second=ProbeAnimation(const CustomDecoration(Colors.green));
      refs.Values.current=first;await tester.pumpWidget(host(case_live.Sample()));await tester.pumpAndSettle();
      final state=tester.state(native()),child=tester.element(find.text('Sized body'));expect(first.listeners,hasLength(1));
      first.update(const ShapeDecoration(shape:CircleBorder(),color:Colors.blue));await tester.pump();
      expect(tester.widget<DecoratedBoxTransition>(native()).decoration.value,isA<ShapeDecoration>());
      refs.Values.current=second;await tester.pumpWidget(host(case_live.Sample()));
      expect(first.listeners,isEmpty);expect(second.listeners,hasLength(1));
      first.update(const BoxDecoration());await tester.pump();expect(tester.widget<DecoratedBoxTransition>(native()).decoration.value,isA<CustomDecoration>());
      expect(identical(state,tester.state(native())),true);expect(identical(child,tester.element(find.text('Sized body'))),true);
      await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);
    });
    for(final rtl in [false,true]){
      testWidgets('native DecorationTween frames and reverse rtl=$rtl',(tester)async{
        final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
        final tween=DecorationTween(begin:const BoxDecoration(color:Colors.red),end:BoxDecoration(color:Colors.blue,borderRadius:BorderRadius.circular(20)));
        refs.Values.current=tween.animate(controller);
        await tester.pumpWidget(host(case_live.Sample(),rtl:rtl));await tester.pumpAndSettle();
        final child=tester.element(find.text('Sized body'));controller.forward();await tester.pump();
        for(var i=1;i<=3;i++){await tester.pump(const Duration(milliseconds:20));expect(controller.value,closeTo(i*.2,1e-8));expect(tester.widget<DecoratedBoxTransition>(native()).decoration.value,tween.transform(controller.value));}
        controller.reverse();await tester.pumpAndSettle();expect(controller.value,0);
        controller.forward();await tester.pumpAndSettle();expect(controller.value,1);
        expect(identical(child,tester.element(find.text('Sized body'))),true);
        await tester.pumpWidget(const SizedBox());controller.dispose();expect(tester.takeException(),isNull);
      });
    }
    for(final position in DecorationPosition.values){
      testWidgets('real pixels background/foreground $position and custom painter',(tester)async{
        for(final custom in [false,true]){
          final animation=AlwaysStoppedAnimation<Decoration>(custom?const CustomDecoration(Colors.red):const BoxDecoration(color:Colors.red));
          await tester.pumpWidget(host(Center(child:RepaintBoundary(key:const ValueKey('pixels'),child:
            DecoratedBoxTransition(decoration:animation,position:position,child:const SizedBox(width:80,height:80,child:ColoredBox(color:Colors.blue)))))));
          await tester.pump();expect((await pixel(tester,const Offset(40,40))).toARGB32(),(position==DecorationPosition.foreground?Colors.red:Colors.blue).toARGB32());
          expect(tester.takeException(),isNull);
        }
      });
      testWidgets('native border adds no padding, circle does not clip and shape hit test $position',(tester)async{
        await tester.pumpWidget(host(Center(child:RepaintBoundary(key:const ValueKey('pixels'),child:DecoratedBoxTransition(
          decoration:AlwaysStoppedAnimation<Decoration>(BoxDecoration(shape:BoxShape.circle,color:Colors.red,border:Border.all(width:10))),
          position:position,child:const SizedBox(width:80,height:80,child:ColoredBox(color:Colors.blue,key:ValueKey('body'))))))));
        final outer=tester.renderObject<RenderBox>(native()),body=tester.renderObject<RenderBox>(find.byKey(const ValueKey('body')));
        expect(outer.size,body.size);expect(outer.localToGlobal(Offset.zero),body.localToGlobal(Offset.zero));
        expect((await pixel(tester,const Offset(1,1))).toARGB32(),Colors.blue.toARGB32(),reason:'Decoration must not clip child');
        await tester.pumpWidget(host(Center(child:DecoratedBoxTransition(decoration:const AlwaysStoppedAnimation<Decoration>(BoxDecoration(shape:BoxShape.circle)),
          position:position,child:const SizedBox(width:80,height:80)))));
        final render=tester.renderObject<RenderDecoratedBox>(find.descendant(of:native(),matching:find.byType(DecoratedBox)));
        expect(render.hitTest(BoxHitTestResult(),position:const Offset(1,1)),false);
        expect(render.hitTest(BoxHitTestResult(),position:const Offset(40,40)),true);expect(tester.takeException(),isNull);
      });
    }
    }
    """;
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
                name: decorated_box_transition_contract
                publish_to: none
                environment:
                  sdk: '>=3.11.0 <4.0.0'
                dependencies:
                  flutter:
                    sdk: flutter
                flutter:
                  assets:
                    - assets/images/
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
            System.out.println("DecoratedBoxTransition pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
