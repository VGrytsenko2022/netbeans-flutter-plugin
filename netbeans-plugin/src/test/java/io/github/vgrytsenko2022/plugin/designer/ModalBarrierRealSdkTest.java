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
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class ModalBarrierRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void fullConstructorNullableEvidenceEventLifecycleAndNativeBarrier()throws Exception{
  initialize();
  Files.writeString(lib.resolve("references.dart"),"""
    import 'package:flutter/material.dart';
    import 'package:flutter/foundation.dart';
    Color? get color=>const Color(0x88223344);
    Color? colorFactory()=>color;
    VoidCallback? get onDismiss=>() {};
    VoidCallback? onDismissFactory()=>onDismiss;
    ValueNotifier<EdgeInsets>? get clipDetailsNotifier=>Values.notifier;
    ValueNotifier<EdgeInsets>? clipDetailsNotifierFactory()=>clipDetailsNotifier;
    class Values {
      static final notifier=ValueNotifier<EdgeInsets>(const EdgeInsets.fromLTRB(1,2,3,4));
      static Color? get color=>const Color(0x88223344);
      static Color? colorFactory()=>color;
      static VoidCallback? get onDismiss=>() {};
      static VoidCallback? onDismissFactory()=>onDismiss;
      static ValueNotifier<EdgeInsets>? get clipDetailsNotifier=>notifier;
      static ValueNotifier<EdgeInsets>? clipDetailsNotifierFactory()=>notifier;
    }
    """);
  var frameId=StableId.random();
  var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.SizedBox"),
      Map.of(new PropertyName("width"),new PropertyValue.DoubleValue(BigDecimal.valueOf(240)),new PropertyName("height"),new PropertyValue.DoubleValue(BigDecimal.valueOf(160))),
      Map.of(new SlotName("child"),new WidgetSlot.SingleSlot(Optional.empty())));
  var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""
    // User member is preserved.
    int _userValue() => 73;
    static final notifier=ValueNotifier<EdgeInsets>(const EdgeInsets.fromLTRB(1,2,3,4));
    static Color? get color=>const Color(0x88223344);
    static Color? colorFactory()=>color;
    static VoidCallback? get onDismiss=>() {};
    static VoidCallback? onDismissFactory()=>onDismiss;
    static ValueNotifier<EdgeInsets>? get clipDetailsNotifier=>notifier;
    static ValueNotifier<EdgeInsets>? clipDetailsNotifierFactory()=>notifier;
    Color? get _color=>color;
    Color? _colorFactory()=>color;
    VoidCallback? get _onDismiss=>onDismiss;
    VoidCallback? _onDismissFactory()=>onDismiss;
    ValueNotifier<EdgeInsets>? get _clipDetailsNotifier=>notifier;
    ValueNotifier<EdgeInsets>? _clipDetailsNotifierFactory()=>notifier;
    Color? get _nullColor=>null;
    VoidCallback? get _nullCallback=>null;
    ValueNotifier<EdgeInsets>? get _nullNotifier=>null;
    void _direct() {}
    void _wrongArity(int value) {}
    String get _wrongColor=>'red';
    EdgeInsets get _insets=>EdgeInsets.zero;
    ValueListenable<EdgeInsets> get _listenable=>notifier;
    ValueNotifier<EdgeInsetsGeometry> get _geometry=>ValueNotifier<EdgeInsetsGeometry>(EdgeInsetsDirectional.zero);
    ValueNotifier<EdgeInsets?> get _nullableInsets=>ValueNotifier<EdgeInsets?>(null);
    ValueNotifier<dynamic> get _dynamicInsets=>ValueNotifier<dynamic>(EdgeInsets.zero);
    dynamic get _dynamic=>notifier;
    """);
  baseline=reopen(baseline,"import 'package:flutter/foundation.dart';\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(ModalBarrierWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));
  assertAnalysis(baseline,base,"probe.dart",true);
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());
  assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  recordCase("default",base,"null","true","true",false,"null","null",false);
  base=reopen(base,source(base));
  var local=apply(base,new PatchProperties(NESTED_ID,List.of(
    new PatchProperties.SetPatch(new PropertyName("color"),new PropertyValue.ColorValue(0x88223344L)),
    new PatchProperties.SetPatch(new PropertyName("dismissible"),new PropertyValue.BooleanValue(false)),
    new PatchProperties.SetPatch(new PropertyName("semanticsLabel"),new PropertyValue.StringValue("Close dialog")),
    new PatchProperties.SetPatch(new PropertyName("semanticsOnTapHint"),new PropertyValue.StringValue("dismiss dialog")))));
  assertAnalysis(base,local,"probe.dart",true);recordCase("local",local,"Color(0x88223344)","false","true",false,"'Close dialog'","'dismiss dialog'",false);
  local=reopen(local,source(local));
  for(String sem:List.of("null","true","false")){
   var changed=apply(local,new SetProperty(NESTED_ID,new PropertyName("barrierSemanticsDismissible"),sem.equals("null")?new PropertyValue.NullValue():new PropertyValue.BooleanValue(Boolean.parseBoolean(sem))));
   assertAnalysis(local,changed,"probe.dart",true);recordCase("sem_"+sem,changed,"Color(0x88223344)","false",sem,false,"'Close dialog'","'dismiss dialog'",false);
  }
  for(String field:List.of("color","onDismiss","semanticsLabel","clipDetailsNotifier","semanticsOnTapHint")){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),new PropertyValue.NullValue()));
   assertAnalysis(base,changed,"probe.dart",true);recordCase("explicit_null_"+field.toLowerCase(),changed,"null","true","true",false,"null","null",false);
   changed=reopen(changed,source(changed));
   var reset=apply(changed,new ResetProperty(NESTED_ID,new PropertyName(field)));assertAnalysis(changed,reset,"probe.dart",true);assertTrue(source(reset).contains("const ModalBarrier()"));
  }
  for(String field:List.of("color","onDismiss","clipDetailsNotifier")){
   for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
    var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),reference(field,imported,member,factory)));
    assertAnalysis(base,changed,"probe.dart",true);
    recordCase(field.toLowerCase()+"_"+imported+"_"+member+"_"+factory,changed,field.equals("color")?"Color(0x88223344)":"null","true","true",field.equals("onDismiss"),"null","null",field.equals("clipDetailsNotifier"));
   }
  }
  for(var entry:Map.of("color","_nullColor","onDismiss","_nullCallback","clipDetailsNotifier","_nullNotifier").entrySet()){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(entry.getKey()),ref(entry.getValue(),false)));
   assertAnalysis(base,changed,"probe.dart",true);recordCase("source_null_"+entry.getKey().toLowerCase(),changed,"null","true","true",false,"null","null",false);
  }
  var direct=apply(base,new SetProperty(NESTED_ID,new PropertyName("onDismiss"),ref("_direct",false)));
  assertAnalysis(base,direct,"probe.dart",true);recordCase("direct",direct,"null","true","true",true,"null","null",false);
  var created=apply(base,new CreateEventHandler(NESTED_ID,new PropertyName("onDismiss"),"_createdDismiss"));
  assertAnalysis(base,created,"probe.dart",true);recordCase("created",created,"null","true","true",true,"null","null",false);
  created=reopen(created,source(created));
  var renamed=apply(created,new RenameEventHandler(NESTED_ID,new PropertyName("onDismiss"),"_renamedDismiss"));
  assertAnalysis(created,renamed,"probe.dart",true);assertFalse(source(renamed).contains("_createdDismiss"));recordCase("renamed",renamed,"null","true","true",true,"null","null",false);
  renamed=reopen(renamed,source(renamed));
  var reset=apply(renamed,new ResetProperty(NESTED_ID,new PropertyName("onDismiss")));
  assertAnalysis(renamed,reset,"probe.dart",true);assertTrue(source(reset).contains("void _renamedDismiss()"));recordCase("disconnected",reset,"null","true","true",false,"null","null",false);
  var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  for(var field:Map.of("color",List.of("_wrongColor","_dynamic"),"onDismiss",List.of("_wrongArity","_wrongColor","_dynamic"),
       "clipDetailsNotifier",List.of("_insets","_listenable","_geometry","_nullableInsets","_dynamicInsets","_dynamic")).entrySet()){
   for(String bad:field.getValue())assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(field.getKey()),ref(bad,false))),"probe.dart",false);
  }
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("barrier_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private void recordCase(String name,DesignerCommandSession session,String color,String dismiss,String sem,boolean callback,String label,String hint,boolean notifier)throws Exception{
  saveCase(name,session);cases.put(name,String.join("|",color,dismiss,sem,String.valueOf(callback),label,hint,String.valueOf(notifier)));
 }
 private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:modal_barrier_contract/references.dart"):Optional.empty(),
    member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
    member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }
 private String runtime(){
  runtimeCaseCount=cases.size()*2+6;
  var imports=new StringBuilder("""
    import 'dart:ui' as ui;
    import 'package:flutter/material.dart';
    import 'package:flutter/foundation.dart';
    import 'package:flutter/rendering.dart';
    import 'package:flutter/services.dart';
    import 'package:flutter_test/flutter_test.dart';
    """);
  var tests=new StringBuilder();
  cases.forEach((name,values)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   var p=values.split("\\|");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{\n")
    .append("await tester.pumpWidget(host(const case_").append(name).append(".Sample(),rtl:rtl));await tester.pump();")
    .append("final finder=find.descendant(of:find.byType(case_").append(name).append(".Sample),matching:find.byType(ModalBarrier));")
    .append("final n=tester.widget<ModalBarrier>(finder);expect(n.color,").append(p[0]).append(");")
    .append("expect(n.dismissible,").append(p[1]).append(");expect(n.barrierSemanticsDismissible,").append(p[2]).append(");")
    .append("expect(n.onDismiss!=null,").append(p[3]).append(");expect(n.semanticsLabel,").append(p[4]).append(");expect(n.semanticsOnTapHint,").append(p[5]).append(");")
    .append("expect(n.clipDetailsNotifier!=null,").append(p[6]).append(");expect(tester.getSize(finder),const Size(240,160));expect(find.text('Sibling'),findsOneWidget);expect(tester.takeException(),isNull);});}\n");
  });
  return imports+"""
    const barrierKey=ValueKey('tested-barrier');
    Finder native()=>find.byKey(barrierKey);
    Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:SizedBox(width:240,height:240,child:Align(alignment:Alignment.topLeft,child:child)))));
    Widget box(Widget child)=>SizedBox(width:240,height:160,child:child);
    Future<int> pixel(WidgetTester tester,Offset p)async{
      final boundary=tester.renderObject<RenderRepaintBoundary>(find.byKey(const ValueKey('pixels')));
      return (await tester.runAsync(()async{final image=await boundary.toImage(pixelRatio:1);final bytes=(await image.toByteData(format:ui.ImageByteFormat.rawRgba))!;final i=(p.dy.toInt()*image.width+p.dx.toInt())*4;
        final result=(bytes.getUint8(i+3)<<24)|(bytes.getUint8(i)<<16)|(bytes.getUint8(i+1)<<8)|bytes.getUint8(i+2);image.dispose();return result;}))!;
    }
    void main(){
    """+tests+"""
      testWidgets('null dismissal uses Navigator.maybePop and root stays intact',(tester)async{
        final nav=GlobalKey<NavigatorState>();
        await tester.pumpWidget(MaterialApp(navigatorKey:nav,home:const Text('Root')));
        nav.currentState!.push(MaterialPageRoute<void>(builder:(_)=>const Center(child:SizedBox(width:240,height:160,child:ModalBarrier(key:barrierKey)))));
        await tester.pumpAndSettle();expect(nav.currentState!.canPop(),true);
        await tester.tapAt(tester.getCenter(native()));await tester.pumpAndSettle();expect(nav.currentState!.canPop(),false);expect(find.text('Root'),findsOneWidget);
        await tester.pumpWidget(host(box(const ModalBarrier(key:barrierKey))));await tester.tapAt(tester.getCenter(native()));await tester.pumpAndSettle();expect(native(),findsOneWidget);expect(tester.takeException(),isNull);
      });
      testWidgets('opaque barrier blocks behind targets while callback owns dismissal and false alerts',(tester)async{
        int behind=0,dismissed=0;final sounds=<MethodCall>[];
        tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(SystemChannels.platform,(call)async{if(call.method=='SystemSound.play')sounds.add(call);return null;});
        addTearDown(()=>tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(SystemChannels.platform,null));
        for(final dismissible in [true,false]){
          await tester.pumpWidget(host(box(Stack(fit:StackFit.expand,children:[
            GestureDetector(onTap:()=>behind++,child:const ColoredBox(color:Colors.blue)),
            ModalBarrier(key:barrierKey,dismissible:dismissible,color:null,onDismiss:()=>dismissed++),
          ]))));
          final before=dismissed;await tester.tapAt(tester.getCenter(native()));await tester.pump();
          expect(dismissed,before+(dismissible?1:0));expect(behind,0);expect(native(),findsOneWidget);
        }
        expect(sounds.length,1);expect(sounds.single.arguments,'SystemSoundType.alert');expect(tester.takeException(),isNull);
      });
      testWidgets('all platforms preserve native accessibility inclusion defaults null false and labels',(tester)async{
        final handle=tester.ensureSemantics();try {
        for(final platform in TargetPlatform.values){
          debugDefaultTargetPlatformOverride=platform;
          for(final dismissible in [true,false]){for(final override in <bool?>[null,true,false]){for(final label in <String?>[null,'','Close modal']){
            await tester.pumpWidget(host(box(ModalBarrier(key:barrierKey,dismissible:dismissible,barrierSemanticsDismissible:override,semanticsLabel:label,semanticsOnTapHint:'dismiss modal',onDismiss:(){}))));await tester.pump();
            final supported=[TargetPlatform.android,TargetPlatform.iOS,TargetPlatform.macOS].contains(platform);
            final excluded=!dismissible||!supported||override==false;
            final exclude=tester.widget<ExcludeSemantics>(find.descendant(of:native(),matching:find.byType(ExcludeSemantics)).first);expect(exclude.excluding,excluded);
            final sem=tester.widget<Semantics>(find.descendant(of:native(),matching:find.byType(Semantics)).first);
            expect(sem.properties.label,dismissible&&supported?label:null);expect(sem.properties.onDismiss!=null,dismissible&&supported&&label!=null);
            expect(tester.takeException(),isNull);
          }}}
        }
        } finally { debugDefaultTargetPlatformOverride=null; handle.dispose(); }
      });
      testWidgets('accessible dismiss and tap use the user callback',(tester)async{
        debugDefaultTargetPlatformOverride=TargetPlatform.android;
        final handle=tester.ensureSemantics();try {int called=0;
        await tester.pumpWidget(host(box(ModalBarrier(key:barrierKey,semanticsLabel:'Close modal',onDismiss:()=>called++))));await tester.pump();
        final semantics=tester.getSemantics(find.bySemanticsLabel('Close modal'));
        tester.binding.pipelineOwner.semanticsOwner!.performAction(semantics.id,ui.SemanticsAction.dismiss);await tester.pump();expect(called,1);
        tester.binding.pipelineOwner.semanticsOwner!.performAction(semantics.id,ui.SemanticsAction.tap);await tester.pump();expect(called,2);
        expect(native(),findsOneWidget);
        } finally { debugDefaultTargetPlatformOverride=null; handle.dispose(); }
      });
      testWidgets('notifier clips semantic bounds updates swaps and detaches without ownership transfer',(tester)async{
        debugDefaultTargetPlatformOverride=TargetPlatform.android;
        final handle=tester.ensureSemantics();try {
        final first=ValueNotifier<EdgeInsets>(const EdgeInsets.fromLTRB(1,2,3,4)),second=ValueNotifier<EdgeInsets>(EdgeInsets.zero);
        Widget view(ValueNotifier<EdgeInsets>? notifier,{bool enabled=true})=>host(box(ModalBarrier(key:barrierKey,semanticsLabel:'Close modal',barrierSemanticsDismissible:enabled,clipDetailsNotifier:notifier,onDismiss:(){})));
        Finder clipper()=>find.descendant(of:native(),matching:find.byWidgetPredicate((w)=>w.runtimeType.toString()=='_SemanticsClipper'));
        await tester.pumpWidget(view(first));await tester.pump();expect(first.hasListeners,true);
        final render=tester.renderObject<RenderBox>(clipper());expect(render.semanticBounds,const Rect.fromLTRB(1,2,237,156));
        first.value=const EdgeInsets.fromLTRB(4,5,6,7);await tester.pump();expect(render.semanticBounds,const Rect.fromLTRB(4,5,234,153));expect(tester.getSize(native()),const Size(240,160));
        await tester.pumpWidget(view(second));await tester.pump();expect(first.hasListeners,false);expect(second.hasListeners,true);
        expect(identical(render,tester.renderObject(clipper())),true);expect(render.semanticBounds,const Rect.fromLTRB(0,0,240,160));
        await tester.pumpWidget(view(second,enabled:false));await tester.pump();expect(second.hasListeners,false);expect(clipper(),findsNothing);
        await tester.pumpWidget(view(first));await tester.pump();expect(first.hasListeners,true);
        await tester.pumpWidget(const SizedBox());expect(first.hasListeners,false);expect(second.hasListeners,false);
        first.value=EdgeInsets.zero;second.value=EdgeInsets.zero;first.dispose();second.dispose();expect(tester.takeException(),isNull);
        } finally { debugDefaultTargetPlatformOverride=null; handle.dispose(); }
      });
      testWidgets('semantics clipping leaves pixels and physical hit extent intact',(tester)async{
        debugDefaultTargetPlatformOverride=TargetPlatform.android;
        final handle=tester.ensureSemantics();try {final clip=ValueNotifier<EdgeInsets>(const EdgeInsets.all(30));int taps=0;
        await tester.pumpWidget(host(RepaintBoundary(key:const ValueKey('pixels'),child:box(ModalBarrier(key:barrierKey,color:const Color(0xff22aa44),semanticsLabel:'Close modal',clipDetailsNotifier:clip,onDismiss:()=>taps++)))));
        await tester.pump();expect(await pixel(tester,const Offset(5,5)),const Color(0xff22aa44).toARGB32());
        await tester.tapAt(tester.getTopLeft(native())+const Offset(5,5));await tester.pump();expect(taps,1);
        await tester.pumpWidget(const SizedBox());expect(clip.hasListeners,false);clip.dispose();expect(tester.takeException(),isNull);
        } finally { debugDefaultTargetPlatformOverride=null; handle.dispose(); }
      });
    }
    """;
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
                name: modal_barrier_contract
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
            System.out.println("ModalBarrier pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
