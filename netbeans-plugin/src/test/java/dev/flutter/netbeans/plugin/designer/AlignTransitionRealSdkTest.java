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
class AlignTransitionRealSdkTest {
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
    Animation<AlignmentGeometry> get alignment=>Values.current;
    Animation<AlignmentGeometry> alignmentFactory()=>Values.current;
    double? get widthFactor=>2;
    double? widthFactorFactory()=>2;
    double? get heightFactor=>.5;
    double? heightFactorFactory()=>.5;
    class Values {
      static Animation<AlignmentGeometry> current=const AlwaysStoppedAnimation<AlignmentGeometry>(AlignmentDirectional(1,-1));
      static Animation<AlignmentGeometry> alignmentFactory()=>current;
      static double? get widthFactor=>2;
      static double? widthFactorFactory()=>2;
      static double? get heightFactor=>.5;
      static double? heightFactorFactory()=>.5;
    }
    """);
  var body=adapter("Sized body",48);
  var baseline=open(group(GROUP_ID,List.of(body,adapter("Sibling",20))),"probe.dart","""
    // User member is preserved.
    int _userValue() => 73;
    static Animation<AlignmentGeometry> current=const AlwaysStoppedAnimation<AlignmentGeometry>(AlignmentDirectional(1,-1));
    static Animation<AlignmentGeometry> alignmentFactory()=>current;
    Animation<AlignmentGeometry> get _alignment=>current;
    Animation<AlignmentGeometry> _alignmentFactory()=>current;
    static double? get widthFactor=>2;
    static double? widthFactorFactory()=>2;
    static double? get heightFactor=>.5;
    static double? heightFactorFactory()=>.5;
    double? get _widthFactor=>2;
    double? _widthFactorFactory()=>2;
    double? get _heightFactor=>.5;
    double? _heightFactorFactory()=>.5;
    Animation<Alignment> get _physical=>const AlwaysStoppedAnimation<Alignment>(Alignment(1,-1));
    Animation<AlignmentDirectional> get _directional=>const AlwaysStoppedAnimation<AlignmentDirectional>(AlignmentDirectional(1,-1));
    Animation<AlignmentGeometry>? get _nullableOuter=>current;
    Animation<AlignmentGeometry?> get _nullableValue=>const AlwaysStoppedAnimation<AlignmentGeometry?>(null);
    AlignmentGeometry get _plain=>Alignment.center;
    Animation<Offset> get _wrong=>const AlwaysStoppedAnimation<Offset>(Offset.zero);
    Animation<dynamic> get _dynamicAnimation=>const AlwaysStoppedAnimation<dynamic>('bad');
    ValueNotifier<AlignmentGeometry> get _notAnimation=>ValueNotifier<AlignmentGeometry>(Alignment.center);
    dynamic get _dynamic=>current;
    double get _factor=>2;
    int? get _wrongFactor=>2;
    String get _text=>'2';
    """);
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AlignTransitionWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var wrapped=apply(baseline,new WrapWidget(body.id(),prototype,new SlotName("child"),0));
  assertAnalysis(baseline,wrapped,"probe.dart",true);
  assertArrayEquals(baseline.current().dartCandidateBytes(),wrapped.undo().session().current().dartCandidateBytes());
  assertArrayEquals(wrapped.current().dartCandidateBytes(),wrapped.undo().session().redo().session().current().dartCandidateBytes());
  recordCase("creation",wrapped,"Alignment.center","null","null");
  wrapped=reopen(wrapped,source(wrapped));
  for(boolean directional:List.of(false,true))for(String xy:List.of("-2.5","0","1")){
    var alignment=new PropertyValue.AlignmentGeometryValue(directional?PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL:PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new BigDecimal(xy),new BigDecimal(xy));
    var local=apply(wrapped,new PatchProperties(NESTED_ID,List.of(
      new PatchProperties.SetPatch(new PropertyName("alignment"),alignment),
      new PatchProperties.SetPatch(new PropertyName("widthFactor"),new PropertyValue.DoubleValue(BigDecimal.TWO)),
      new PatchProperties.SetPatch(new PropertyName("heightFactor"),new PropertyValue.DoubleValue(new BigDecimal(".5"))))));
    assertAnalysis(wrapped,local,"probe.dart",true);
    recordCase("local_"+directional+"_"+xy.replace("-","neg").replace(".","_"),local,(directional?"AlignmentDirectional":"Alignment")+"("+xy+","+xy+")","2",".5");
  }
  for(String field:List.of("alignment","widthFactor","heightFactor")){
    for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
      var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName(field),reference(field,imported,member,factory)));
      assertAnalysis(wrapped,changed,"probe.dart",true);
      recordCase(field+"_"+imported+"_"+member+"_"+factory,changed,field.equals("alignment")?"AlignmentDirectional(1,-1)":"Alignment.center",field.equals("widthFactor")?"2":"null",field.equals("heightFactor")?".5":"null");
    }
  }
  for(String valid:List.of("_physical","_directional")){
    var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("alignment"),ref(valid,false)));
    assertAnalysis(wrapped,changed,"probe.dart",true);
    recordCase(valid.substring(1),changed,valid.equals("_physical")?"Alignment(1,-1)":"AlignmentDirectional(1,-1)","null","null");
  }
  for(String field:List.of("widthFactor","heightFactor")){
    var factor=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName(field),ref("_factor",false)));
    assertAnalysis(wrapped,factor,"probe.dart",true);saveCase(field+"_nonnull",factor);
    for(var value:List.<PropertyValue>of(new PropertyValue.DoubleValue(BigDecimal.ZERO),new PropertyValue.NullValue())){
      var changed=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName(field),value));
      assertAnalysis(wrapped,changed,"probe.dart",true);
      String expected=value instanceof PropertyValue.NullValue?"null":"0";
      recordCase(field+"_"+expected,changed,"Alignment.center",field.equals("widthFactor")?expected:"null",field.equals("heightFactor")?expected:"null");
    }
    for(String bad:List.of("_wrongFactor","_text","_dynamic"))
      assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName(field),ref(bad,false))),"probe.dart",false);
  }
  var live=apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("alignment"),reference("alignment",true,true,false)));
  assertAnalysis(wrapped,live,"probe.dart",true);recordCase("live",live,"AlignmentDirectional(1,-1)","null","null");
  live=reopen(live,source(live));
  var reset=apply(live,new SetProperty(NESTED_ID,new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,BigDecimal.ZERO,BigDecimal.ZERO)));
  assertAnalysis(live,reset,"probe.dart",true);recordCase("local_again",reset,"Alignment.center","null","null");
  var moved=apply(wrapped,new MoveWidget(NESTED_ID,new WidgetPlacement(GROUP_ID,CHILDREN,1)));
  assertAnalysis(wrapped,moved,"probe.dart",true);recordCase("moved",moved,"Alignment.center","null","null");
  var replaced=apply(wrapped,new ReplaceSlotChild(NESTED_ID,new SlotName("child"),body.id(),new ReplaceSlotChild.NewSubtree(adapter("Sized body",48))));
  assertAnalysis(wrapped,replaced,"probe.dart",true);recordCase("replaced",replaced,"Alignment.center","null","null");
  assertFalse(wrapped.apply(new RemoveWidget(body.id())).changed());
  assertFalse(wrapped.apply(new MoveWidget(body.id(),new WidgetPlacement(GROUP_ID,CHILDREN,2))).changed());
  assertFalse(wrapped.apply(new ResetProperty(NESTED_ID,new PropertyName("alignment"))).changed());
  for(String bad:List.of("_plain","_nullableOuter","_nullableValue","_wrong","_dynamicAnimation","_notAnimation","_dynamic"))
    assertAnalysis(wrapped,apply(wrapped,new SetProperty(NESTED_ID,new PropertyName("alignment"),ref(bad,false))),"probe.dart",false);
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("alignment_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
   return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:align_transition_contract/references.dart"):Optional.empty(),
     member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
     member?Optional.of(factory?field+"Factory":field.equals("alignment")?"current":field):Optional.empty(),
     factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
     factory?Optional.of(false):Optional.empty());
 }
 private void recordCase(String name,DesignerCommandSession session,String expected,String width,String height)throws Exception{
   saveCase(name,session);cases.put(name,expected+"|"+width+"|"+height);
 }
 private String runtime(){
  runtimeCaseCount=cases.size()*2+8;
  var imports=new StringBuilder("""
    import 'dart:ui' as ui;
    import 'package:flutter/material.dart';
    import 'package:flutter/rendering.dart';
    import 'package:flutter_test/flutter_test.dart';
    import 'package:align_transition_contract/references.dart' as refs;
    """);
  var tests=new StringBuilder();
  cases.forEach((name,values)->{
    imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
    var parts=values.split("\\|");var expected=parts[0];var width=parts[1];var height=parts[2];
    tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{\n")
      .append("await tester.pumpWidget(host(const case_").append(name).append(".Sample(),rtl:rtl));await tester.pump();")
      .append("final n=tester.widget<AlignTransition>(native());expect(n.alignment.value,").append(expected).append(");")
      .append("expect(n.widthFactor,").append(width).append(");expect(n.heightFactor,").append(height).append(");")
      .append("expect(find.text('Sized body'),findsOneWidget);expect(tester.takeException(),isNull);});}\n");
  });
  return imports+"""
    Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:240,height:160,child:child))));
    Finder native()=>find.byType(AlignTransition);
    class ProbeAnimation<T> extends Animation<T>{
      ProbeAnimation(this._value);T _value;final listeners=<VoidCallback>{};
      @override T get value=>_value;
      void update(T value){_value=value;for(final f in List<VoidCallback>.of(listeners)){f();}}
      @override AnimationStatus get status=>AnimationStatus.forward;
      @override void addListener(VoidCallback f)=>listeners.add(f);
      @override void removeListener(VoidCallback f)=>listeners.remove(f);
      @override void addStatusListener(AnimationStatusListener f){}
      @override void removeStatusListener(AnimationStatusListener f){}
    }
    class NonNullGeometry extends Animatable<AlignmentGeometry>{
      NonNullGeometry(this.tween);final AlignmentGeometryTween tween;
      @override AlignmentGeometry transform(double t)=>tween.transform(t)!;
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
      testWidgets('saved project animation updates swaps and detaches listeners without replacing child',(tester)async{
        final first=ProbeAnimation<AlignmentGeometry>(Alignment.center),second=ProbeAnimation<AlignmentGeometry>(AlignmentDirectional(1,-1));
        refs.Values.current=first;
        await tester.pumpWidget(host(const case_live.Sample()));
        final state=tester.state(native()),body=tester.element(find.text('Sized body'));
        expect(first.listeners,isNotEmpty);
        first.update(Alignment(-2,1));await tester.pump();
        expect(tester.widget<Align>(find.descendant(of:native(),matching:find.byType(Align)).first).alignment,Alignment(-2,1));
        refs.Values.current=second;
        tester.element(find.byType(case_live.Sample)).markNeedsBuild();await tester.pump();
        expect(first.listeners,isEmpty);expect(second.listeners,isNotEmpty);
        expect(identical(state,tester.state(native())),true);expect(identical(body,tester.element(find.text('Sized body'))),true);
        first.update(Alignment(1,1));await tester.pump();
        expect(tester.widget<AlignTransition>(native()).alignment.value,AlignmentDirectional(1,-1));
        await tester.pumpWidget(const SizedBox());expect(second.listeners,isEmpty);
        refs.Values.current=const AlwaysStoppedAnimation<AlignmentGeometry>(AlignmentDirectional(1,-1));
        expect(tester.takeException(),isNull);
      });
      for(final rtl in [false,true])for(final reverse in [false,true]){
        testWidgets('mixed physical directional geometry tween rtl=$rtl reverse=$reverse',(tester)async{
          final source=ProbeAnimation<double>(reverse?1:0);
          final tween=AlignmentGeometryTween(begin:Alignment(-1,-1),end:AlignmentDirectional(1,1));
          final animation=NonNullGeometry(tween).animate(source);
          await tester.pumpWidget(host(AlignTransition(alignment:animation,child:const SizedBox(key:ValueKey('body'),width:48,height:48)),rtl:rtl));
          final state=tester.state(native()),body=tester.element(find.byKey(const ValueKey('body')));
          for(final t in (reverse?[1.0,.75,.5,.25,0.0]:[0.0,.25,.5,.75,1.0])){
            source.update(t);await tester.pump();
            final box=tester.renderObject<RenderBox>(native()),child=tester.renderObject<RenderBox>(find.byKey(const ValueKey('body')));
            final align=tween.transform(t)!.resolve(rtl?TextDirection.rtl:TextDirection.ltr);
            expect(child.localToGlobal(Offset.zero,ancestor:box),Offset(192*(1+align.x)/2,112*(1+align.y)/2));
            expect(identical(state,tester.state(native())),true);expect(identical(body,tester.element(find.byKey(const ValueKey('body')))),true);
          }
          await tester.pumpWidget(const SizedBox());expect(source.listeners,isEmpty);expect(tester.takeException(),isNull);
        });
      }
      testWidgets('factor values and null reset are immediate not interpolated',(tester)async{
        Widget view(double? w,double? h)=>host(Center(child:AlignTransition(alignment:const AlwaysStoppedAnimation<AlignmentGeometry>(Alignment.center),widthFactor:w,heightFactor:h,child:const SizedBox(key:ValueKey('body'),width:48,height:48))));
        await tester.pumpWidget(view(2,.5));
        final state=tester.state(native()),body=tester.element(find.byKey(const ValueKey('body')));
        expect(tester.getSize(native()),const Size(96,24));
        await tester.pumpWidget(view(0,0));expect(tester.getSize(native()),Size.zero);
        await tester.pumpWidget(view(null,null));expect(tester.getSize(native()),const Size(240,160));
        expect(identical(state,tester.state(native())),true);expect(identical(body,tester.element(find.byKey(const ValueKey('body')))),true);
        expect(tester.takeException(),isNull);
      });
      testWidgets('unbounded axes use native child factor sizing',(tester)async{
        for(final w in [null,0.0,.5,2.0])for(final h in [null,0.0,.5,2.0]){
          await tester.pumpWidget(host(UnconstrainedBox(child:AlignTransition(alignment:const AlwaysStoppedAnimation<AlignmentGeometry>(Alignment.center),widthFactor:w,heightFactor:h,child:const SizedBox(width:48,height:48)))));
          expect(tester.getSize(native()),Size(48*(w??1),48*(h??1)));expect(tester.takeException(),isNull);
        }
      });
      testWidgets('real child receives hits while overflowing paint is not clipped',(tester)async{
        int taps=0;
        await tester.pumpWidget(MaterialApp(home:Center(child:RepaintBoundary(key:const ValueKey('pixels'),child:SizedBox(width:400,height:200,child:ColoredBox(color:Colors.white,child:Stack(children:[
          Positioned(left:10,top:20,width:100,height:100,child:AlignTransition(alignment:const AlwaysStoppedAnimation<AlignmentGeometry>(Alignment(2,0)),child:GestureDetector(onTap:()=>taps++,child:const SizedBox(key:ValueKey('body'),width:48,height:48,child:ColoredBox(color:Color(0xff22aa44))))))
        ])))))));
        await tester.pump();
        expect(await pixel(tester,const Offset(130,60)),const Color(0xff22aa44).toARGB32());
        final origin=tester.getTopLeft(find.byKey(const ValueKey('pixels')));
        await tester.tapAt(origin+const Offset(100,60));expect(taps,1);
        await tester.tapAt(origin+const Offset(130,60));expect(taps,1);
        expect(tester.takeException(),isNull);
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
                name: align_transition_contract
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
            System.out.println("AlignTransition pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
