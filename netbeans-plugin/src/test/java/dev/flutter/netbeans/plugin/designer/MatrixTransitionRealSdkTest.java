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
class MatrixTransitionRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void completeLocalSourceCallbackAndNativeLifecycle()throws Exception{
  initialize();
  Files.writeString(lib.resolve("references.dart"),"""
    import 'package:flutter/widgets.dart';
    Animation<double> get animation=>Values.current;
    Animation<double> animationFactory()=>Values.current;
    TransformCallback get onTransform=>Values.transform;
    TransformCallback onTransformFactory()=>Values.transform;
    Alignment get alignment=>const Alignment(1,-1);
    Alignment alignmentFactory()=>const Alignment(1,-1);
    class Values {
      static Animation<double> current=const AlwaysStoppedAnimation<double>(.25);
      static TransformCallback transform=(double value)=>Matrix4.translationValues(value*20,-value*12,0);
      static Animation<double> get animation=>current;
      static Animation<double> animationFactory()=>current;
      static TransformCallback get onTransform=>transform;
      static TransformCallback onTransformFactory()=>transform;
      static Alignment get alignment=>const Alignment(1,-1);
      static Alignment alignmentFactory()=>const Alignment(1,-1);
    }
    """);
  var body=adapter("Sized body",48);
  var baseline=open(group(GROUP_ID,List.of(body,adapter("Sibling",20))),"probe.dart","""
    // User member is preserved.
    int _userValue() => 73;
    static Animation<double> current=const AlwaysStoppedAnimation<double>(.25);
    static TransformCallback transform=(double value)=>Matrix4.translationValues(value*20,-value*12,0);
    static Animation<double> get animation=>current;
    static Animation<double> animationFactory()=>current;
    static TransformCallback get onTransform=>transform;
    static TransformCallback onTransformFactory()=>transform;
    static Alignment get alignment=>const Alignment(1,-1);
    static Alignment alignmentFactory()=>const Alignment(1,-1);
    Animation<double> get _animation=>current;
    Animation<double> _animationFactory()=>current;
    TransformCallback get _onTransform=>transform;
    TransformCallback _onTransformFactory()=>transform;
    Alignment get _alignment=>const Alignment(1,-1);
    Alignment _alignmentFactory()=>const Alignment(1,-1);
    Animation<double>? get _nullableAnimation=>current;
    Animation<double?> get _nullableValue=>const AlwaysStoppedAnimation<double?>(null);
    Animation<int> get _integerAnimation=>const AlwaysStoppedAnimation<int>(1);
    Animation<dynamic> get _dynamicAnimation=>const AlwaysStoppedAnimation<dynamic>(1);
    double get _plain=>.25;
    ValueNotifier<double> get _notAnimation=>ValueNotifier<double>(.25);
    dynamic get _dynamic=>current;
    Matrix4 _direct(double value)=>Matrix4.translationValues(value*20,-value*12,0);
    Matrix4 _broader(num value)=>Matrix4.translationValues(value.toDouble()*20,-value.toDouble()*12,0);
    Matrix4? _nullableMatrix(double value)=>null;
    Matrix4 _wrongParameter(int value)=>Matrix4.identity();
    Matrix4 _extra(double value,double other)=>Matrix4.identity();
    Matrix4 _noParameter()=>Matrix4.identity();
    Matrix4 get _matrix=>Matrix4.identity();
    TransformCallback? get _nullableCallback=>transform;
    dynamic _dynamicResult(double value)=>Matrix4.identity();
    Object _wrongResult(double value)=>Matrix4.identity();
    AlignmentGeometry get _geometry=>AlignmentDirectional.center;
    Alignment? get _nullableAlignment=>null;
    """);
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(MatrixTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var wrapped=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
  assertAnalysis(baseline,wrapped,"probe.dart",true);
  assertArrayEquals(baseline.current().dartCandidateBytes(),wrapped.undo().session().current().dartCandidateBytes());
  assertArrayEquals(wrapped.current().dartCandidateBytes(),wrapped.undo().session().redo().session().current().dartCandidateBytes());
  recordCase("creation",wrapped,"0","Matrix4.identity()","Alignment.center","null",true);
  var rootWrap=apply(baseline,new WrapWidget(GROUP_ID,prototype,new SlotName("child"),0));
  assertAnalysis(baseline,rootWrap,"probe.dart",true);recordCase("root",rootWrap,"0","Matrix4.identity()","Alignment.center","null",true);
  wrapped=reopen(wrapped,source(wrapped));
  var half=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("animation"),new PropertyValue.DoubleValue(new BigDecimal(".5"))));
  assertAnalysis(wrapped,half,"probe.dart",true);wrapped=reopen(half,source(half));
  for(String kind:List.of("identity","translation","scale","perspective","singular","zero"))for(String quality:List.of("null","low")){
    var local=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
      new PatchProperties.SetPatch(new PropertyName("onTransform"),localMatrix(kind)),
      new PatchProperties.SetPatch(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new BigDecimal("-2"),new BigDecimal("3"))),
      new PatchProperties.SetPatch(new PropertyName("filterQuality"),quality.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("FilterQuality",quality)))));
    assertAnalysis(wrapped,local,"probe.dart",true);
    recordCase("local_"+kind+"_"+quality,local,".5",matrixExpression(kind),"Alignment(-2,3)",quality.equals("null")?"null":"FilterQuality.low",true);
  }
  for(String field:List.of("animation","onTransform","alignment")){
    for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
      var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName(field),reference(field,imported,member,factory)));
      assertAnalysis(wrapped,changed,"probe.dart",true);
      recordCase(field+"_"+imported+"_"+member+"_"+factory,changed,field.equals("animation")?".25":".5",field.equals("onTransform")?"Matrix4.translationValues(10,-6,0)":"Matrix4.identity()",field.equals("alignment")?"Alignment(1,-1)":"Alignment.center","null",true);
    }
  }
  for(String method:List.of("_direct","_broader")){
    var local=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("onTransform"),ref(method,false)));
    assertAnalysis(wrapped,local,"probe.dart",true);recordCase(method.substring(1),local,".5","Matrix4.translationValues(10,-6,0)","Alignment.center","null",true);
  }
  var created=apply(wrapped,new CreateEventHandler(NESTED_ID,new PropertyName("onTransform"),"_createdTransform"));
  assertAnalysis(wrapped,created,"probe.dart",true);assertTrue(source(created).contains("return Matrix4.identity();"));assertFalse(source(created).contains("UnimplementedError"));
  recordCase("handler_created",created,".5","Matrix4.identity()","Alignment.center","null",true);
  created=reopen(created,source(created));
  var renamed=apply(created,new RenameEventHandler(NESTED_ID,new PropertyName("onTransform"),"_renamedTransform"));
  assertAnalysis(created,renamed,"probe.dart",true);assertFalse(source(renamed).contains("_createdTransform"));
  recordCase("handler_renamed",renamed,".5","Matrix4.identity()","Alignment.center","null",true);
  renamed=reopen(renamed,source(renamed));
  var disconnected=apply(renamed,new SetProperty(NESTED_ID,new PropertyName("onTransform"),MatrixTransitionWidgetPropertySchema.identity()));
  assertAnalysis(renamed,disconnected,"probe.dart",true);assertTrue(source(disconnected).contains("Matrix4 _renamedTransform(double animationValue)"));
  recordCase("handler_disconnected",disconnected,".5","Matrix4.identity()","Alignment.center","null",true);
  var live=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
      new PatchProperties.SetPatch(new PropertyName("animation"),reference("animation",true,true,false)),
      new PatchProperties.SetPatch(new PropertyName("onTransform"),reference("onTransform",true,true,false)))));
  assertAnalysis(wrapped,live,"probe.dart",true);recordCase("live",live,".25","Matrix4.translationValues(5,-3,0)","Alignment.center","null",true);
  live=reopen(live,source(live));
  var reset=apply(live,new PatchProperties(NESTED_ID,List.of(
      new PatchProperties.SetPatch(new PropertyName("animation"),new PropertyValue.DoubleValue(BigDecimal.ZERO)),
      new PatchProperties.SetPatch(new PropertyName("onTransform"),MatrixTransitionWidgetPropertySchema.identity()))));
  assertAnalysis(live,reset,"probe.dart",true);recordCase("local_again",reset,"0","Matrix4.identity()","Alignment.center","null",true);
  var moved=apply(wrapped,new MoveWidget(NESTED_ID,new WidgetPlacement(GROUP_ID,CHILDREN,1)));
  assertAnalysis(wrapped,moved,"probe.dart",true);recordCase("moved",moved,".5","Matrix4.identity()","Alignment.center","null",true);
  var empty=apply(wrapped,new RemoveWidget(body.id()));
  assertAnalysis(wrapped,empty,"probe.dart",true);recordCase("empty",empty,".5","Matrix4.identity()","Alignment.center","null",false);
  var replacement=apply(wrapped,new ReplaceSlotChild(NESTED_ID,new SlotName("child"),body.id(),new ReplaceSlotChild.NewSubtree(adapter("Sized body",48))));
  assertAnalysis(wrapped,replacement,"probe.dart",true);recordCase("replaced",replacement,".5","Matrix4.identity()","Alignment.center","null",true);
  assertFalse(wrapped.apply(new ResetProperty(NESTED_ID,new PropertyName("animation"))).changed());
  assertFalse(wrapped.apply(new ResetProperty(NESTED_ID,new PropertyName("onTransform"))).changed());
  for(String method:List.of("_nullableAnimation","_nullableValue","_integerAnimation","_dynamicAnimation","_plain","_notAnimation","_dynamic"))
    assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("animation"),ref(method,false))),"probe.dart",false);
  for(String method:List.of("_nullableMatrix","_wrongParameter","_extra","_noParameter","_matrix","_nullableCallback","_dynamicResult","_wrongResult","_dynamic"))
    assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("onTransform"),ref(method,false))),"probe.dart",false);
  for(String method:List.of("_geometry","_nullableAlignment","_dynamic"))
    assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("alignment"),ref(method,false))),"probe.dart",false);
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("matrix_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
   return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:matrix_transition_contract/references.dart"):Optional.empty(),
     member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
     member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
     factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
     factory?Optional.of(false):Optional.empty());
 }
 private static String matrixExpression(String kind){
   return "Matrix4.fromList(<double>["+localMatrix(kind).storage().stream().map(BigDecimal::toPlainString).collect(java.util.stream.Collectors.joining(","))+"])";
 }
 private void recordCase(String name,DesignerCommandSession session,String animation,String matrix,String alignment,String quality,boolean child)throws Exception{
   saveCase(name,session);cases.put(name,animation+"|"+matrix+"|"+alignment+"|"+quality+"|"+child);
 }
    private static PropertyValue.Matrix4Value localMatrix(String name){
        var values=new ArrayList<>(MatrixTransitionWidgetPropertySchema.identity().storage());
        switch(name){
            case "translation" -> {values.set(12,new BigDecimal("20"));values.set(13,new BigDecimal("-12"));}
            case "scale" -> {values.set(0,new BigDecimal("-2"));values.set(5,new BigDecimal(".5"));}
            case "perspective" -> {values.set(3,new BigDecimal(".001"));values.set(7,new BigDecimal("-.002"));values.set(11,new BigDecimal(".003"));}
            case "singular" -> values.set(0,BigDecimal.ZERO);
            case "zero" -> Collections.fill(values,BigDecimal.ZERO);
            case "overflow" -> {values.set(0,new BigDecimal("1e308"));values.set(12,new BigDecimal("-1e308"));}
        }
        return new PropertyValue.Matrix4Value(values);
    }
 private String runtime(){
  runtimeCaseCount=cases.size()*2+8;
  var imports=new StringBuilder("""
    import 'dart:ui' as ui;
    import 'package:flutter/material.dart';
    import 'package:flutter/rendering.dart';
    import 'package:flutter_test/flutter_test.dart';
    import 'package:matrix_transition_contract/references.dart' as refs;
    """);
  var tests=new StringBuilder();
  cases.forEach((name,values)->{
    imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
    var p=values.split("\\|");
    tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{\n")
      .append("await tester.pumpWidget(host(const case_").append(name).append(".Sample(),rtl:rtl));await tester.pump();")
      .append("final n=tester.widget<MatrixTransition>(native());expect(n.animation.value,").append(p[0]).append(");")
      .append("expect(n.onTransform(n.animation.value).storage,").append(p[1]).append(".storage);")
      .append("expect(n.alignment,").append(p[2]).append(");expect(n.filterQuality,").append(p[3]).append(");")
      .append("expect(n.child==null,").append(!Boolean.parseBoolean(p[4])).append(");expect(find.text('Sized body'),").append(Boolean.parseBoolean(p[4])?"findsOneWidget":"findsNothing").append(");expect(tester.takeException(),isNull);});}\n");
  });
  return imports+"""
    Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:240,height:160,child:child))));
    Finder native()=>find.byType(MatrixTransition);
    Finder transform()=>find.descendant(of:native(),matching:find.byType(Transform)).first;
    class ProbeAnimation extends Animation<double>{
      ProbeAnimation(this._value);double _value;final listeners=<VoidCallback>{},statusListeners=<AnimationStatusListener>{};
      AnimationStatus _status=AnimationStatus.forward;
      @override double get value=>_value;
      void update(double value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
      void setStatus(AnimationStatus value){_status=value;for(final f in List<AnimationStatusListener>.of(statusListeners)){f(value);}}
      @override AnimationStatus get status=>_status;
      @override void addListener(VoidCallback f)=>listeners.add(f);
      @override void removeListener(VoidCallback f)=>listeners.remove(f);
      @override void addStatusListener(AnimationStatusListener f)=>statusListeners.add(f);
      @override void removeStatusListener(AnimationStatusListener f)=>statusListeners.remove(f);
    }
    Future<int> pixel(WidgetTester tester,Offset offset)async=> (await tester.runAsync(()async{
      final boundary=tester.renderObject<RenderRepaintBoundary>(find.byKey(const ValueKey('pixels')));
      final image=await boundary.toImage(pixelRatio:1);
      final bytes=(await image.toByteData(format:ui.ImageByteFormat.rawRgba))!;
      final i=(offset.dy.toInt()*image.width+offset.dx.toInt())*4;
      final color=Color.fromARGB(bytes.getUint8(i+3),bytes.getUint8(i),bytes.getUint8(i+1),bytes.getUint8(i+2)).toARGB32();
      image.dispose();return color;
    }))!;
    void main(){
    """+tests+"""
      testWidgets('saved live animation and callback replacements retain child and detach old listeners',(tester)async{
        final first=ProbeAnimation(0),second=ProbeAnimation(.5),values=<double>[];
        refs.Values.current=first;
        refs.Values.transform=(double value){values.add(value);return Matrix4.translationValues(value*20,-value*12,0);};
        await tester.pumpWidget(host(const case_live.Sample()));
        final state=tester.state(native()),body=tester.element(find.text('Sized body'));
        expect(first.listeners,isNotEmpty);
        for(final value in [-2.5,0.0,.25,1.5]){
          first.update(value);await tester.pump();
          expect(values.last,value);expect(tester.widget<Transform>(transform()).transform.storage,Matrix4.translationValues(value*20,-value*12,0).storage);
        }
        refs.Values.current=second;
        refs.Values.transform=(double value)=>Matrix4.diagonal3Values(-value,2*value,1);
        tester.element(find.byType(case_live.Sample)).markNeedsBuild();await tester.pump();
        expect(first.listeners,isEmpty);expect(second.listeners,isNotEmpty);
        expect(tester.widget<Transform>(transform()).transform.storage,Matrix4.diagonal3Values(-.5,1,1).storage);
        expect(identical(state,tester.state(native())),true);expect(identical(body,tester.element(find.text('Sized body'))),true);
        first.update(10);await tester.pump();expect(tester.widget<MatrixTransition>(native()).animation,second);
        await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);
        refs.Values.current=const AlwaysStoppedAnimation<double>(.25);
        refs.Values.transform=(double value)=>Matrix4.translationValues(value*20,-value*12,0);
        expect(tester.takeException(),isNull);
      });
      for(final quality in FilterQuality.values){
        testWidgets('native filter follows value-frame status and ignores status-only notification $quality',(tester)async{
          final animation=ProbeAnimation(.2);
          await tester.pumpWidget(host(MatrixTransition(animation:animation,onTransform:(_)=>Matrix4.identity(),filterQuality:quality,child:const SizedBox(width:48,height:48))));
          expect(tester.widget<Transform>(transform()).filterQuality,quality);
          animation.setStatus(AnimationStatus.completed);await tester.pump();
          expect(tester.widget<Transform>(transform()).filterQuality,quality);
          for(final status in AnimationStatus.values){
            animation.setStatus(status);animation.update(.3);await tester.pump();
            expect(tester.widget<Transform>(transform()).filterQuality,status==AnimationStatus.forward||status==AnimationStatus.reverse?quality:null);
          }
          expect(animation.statusListeners,isEmpty);
          await tester.pumpWidget(const SizedBox());expect(animation.listeners,isEmpty);expect(tester.takeException(),isNull);
        });
      }
      testWidgets('arbitrary source callback drives forward reverse physical 3D geometry in RTL and LTR',(tester)async{
        for(final rtl in [false,true]){
          final animation=ProbeAnimation(0);
          Matrix4 compute(double value)=>Matrix4.identity()..setEntry(3,2,.001)..rotateY(value);
          await tester.pumpWidget(host(MatrixTransition(animation:animation,onTransform:compute,alignment:const Alignment(-2,.5),child:const SizedBox(key:ValueKey('body'),width:48,height:48)),rtl:rtl));
          final state=tester.state(native()),body=tester.element(find.byKey(const ValueKey('body')));
          for(final value in [-2.5,0.0,.25,1.25,.25,0.0,-2.5]){
            animation.update(value);await tester.pump();
            expect(tester.getSize(native()),const Size(240,160));
            expect(tester.widget<Transform>(transform()).transform.storage,compute(value).storage);
            final parent=tester.renderObject<RenderBox>(native()),child=tester.renderObject<RenderBox>(find.byKey(const ValueKey('body')));
            final pivot=const Alignment(-2,.5).alongSize(parent.size),effective=Matrix4.identity()..translateByDouble(pivot.dx,pivot.dy,0,1)..multiply(compute(value))..translateByDouble(-pivot.dx,-pivot.dy,0,1);
            for(final point in [Offset.zero,const Offset(10,20)]){
              final expected=MatrixUtils.transformPoint(effective,point),actual=child.localToGlobal(point,ancestor:parent);
              expect(actual.dx,closeTo(expected.dx,1e-8));expect(actual.dy,closeTo(expected.dy,1e-8));
            }
            expect(identical(state,tester.state(native())),true);expect(identical(body,tester.element(find.byKey(const ValueKey('body')))),true);
            expect(tester.takeException(),isNull);
          }
          await tester.pumpWidget(const SizedBox());expect(animation.listeners,isEmpty);
        }
      });
      testWidgets('matrix painting is not clipped and pointer input follows translated child',(tester)async{
        int taps=0;
        await tester.pumpWidget(MaterialApp(home:Center(child:RepaintBoundary(key:const ValueKey('pixels'),child:SizedBox(width:400,height:200,child:ColoredBox(color:Colors.white,child:Stack(children:[
          Positioned(left:10,top:20,width:100,height:100,child:MatrixTransition(animation:const AlwaysStoppedAnimation<double>(30),onTransform:(value)=>Matrix4.translationValues(value,0,0),child:GestureDetector(onTap:()=>taps++,child:const ColoredBox(color:Color(0xff22aa44)))))
        ])))))));
        await tester.pump();expect(await pixel(tester,const Offset(130,60)),const Color(0xff22aa44).toARGB32());
        final origin=tester.getTopLeft(find.byKey(const ValueKey('pixels')));
        await tester.tapAt(origin+const Offset(20,60));expect(taps,0);
        await tester.tapAt(origin+const Offset(100,60));expect(taps,1);
        expect(tester.takeException(),isNull);
      });
      testWidgets('optional child toggles without removing the animation shell',(tester)async{
        final animation=ProbeAnimation(.5);
        Widget view(bool child)=>host(Center(child:MatrixTransition(animation:animation,onTransform:(_)=>Matrix4.identity(),child:child?const SizedBox(width:48,height:48):null)));
        await tester.pumpWidget(view(false));final state=tester.state(native());
        expect(tester.getSize(native()),Size.zero);expect(animation.listeners,isNotEmpty);
        await tester.pumpWidget(view(true));expect(tester.getSize(native()),const Size(48,48));expect(identical(state,tester.state(native())),true);
        await tester.pumpWidget(view(false));expect(tester.getSize(native()),Size.zero);expect(identical(state,tester.state(native())),true);
        await tester.pumpWidget(const SizedBox());expect(animation.listeners,isEmpty);expect(tester.takeException(),isNull);
      });
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
                name: matrix_transition_contract
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
            System.out.println("MatrixTransition pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
