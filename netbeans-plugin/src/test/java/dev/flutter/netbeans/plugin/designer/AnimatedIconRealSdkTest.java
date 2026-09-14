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
class AnimatedIconRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void fullConstructorSourceEvidencePersistenceAndNativeAnimation()throws Exception{
  initialize();
  Files.writeString(lib.resolve("references.dart"),"""
    import 'package:flutter/material.dart';
    AnimatedIconData get icon=>AnimatedIcons.play_pause;
    AnimatedIconData iconFactory()=>icon;
    Animation<double> get progress=>const AlwaysStoppedAnimation<double>(.75);
    Animation<double> progressFactory()=>progress;
    Color? get color=>const Color(0x88223344);
    Color? colorFactory()=>color;
    double? get size=>36;
    double? sizeFactory()=>size;
    class Values {
      static AnimatedIconData get icon=>AnimatedIcons.play_pause;
      static AnimatedIconData iconFactory()=>icon;
      static Animation<double> get progress=>const AlwaysStoppedAnimation<double>(.75);
      static Animation<double> progressFactory()=>progress;
      static Color? get color=>const Color(0x88223344);
      static Color? colorFactory()=>color;
      static double? get size=>36;
      static double? sizeFactory()=>size;
    }
    """);
  var frameId=StableId.random();
  var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
  var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""
    // User member is preserved.
    int _userValue() => 73;
    static AnimatedIconData get icon=>AnimatedIcons.play_pause;
    static AnimatedIconData iconFactory()=>icon;
    static Animation<double> get progress=>const AlwaysStoppedAnimation<double>(.75);
    static Animation<double> progressFactory()=>progress;
    static Color? get color=>const Color(0x88223344);
    static Color? colorFactory()=>color;
    static double? get size=>36;
    static double? sizeFactory()=>size;
    AnimatedIconData get _icon=>icon;
    AnimatedIconData _iconFactory()=>icon;
    Animation<double> get _progress=>progress;
    Animation<double> _progressFactory()=>progress;
    Color? get _color=>color;
    Color? _colorFactory()=>color;
    double? get _size=>size;
    double? _sizeFactory()=>size;
    Color? get _nullColor=>null;
    double? get _nullSize=>null;
    String get _wrong=>'wrong';
    IconData get _plainIcon=>Icons.star;
    AnimatedIconData? get _nullableIcon=>icon;
    double get _plainProgress=>.5;
    Animation<int> get _integerAnimation=>const AlwaysStoppedAnimation<int>(1);
    Animation<double>? get _nullableAnimation=>progress;
    Animation<dynamic> get _dynamicAnimation=>const AlwaysStoppedAnimation<dynamic>(.5);
    dynamic get _dynamic=>null;
    """);
  // User-owned members need their own import even when the last Material widget is removed.
  baseline=reopen(baseline,"import 'package:flutter/material.dart';\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(AnimatedIconWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));
  assertAnalysis(baseline,base,"probe.dart",true);
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());
  assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  recordCase("creation",base,"menu_close","0","null","null","null","null");
  base=reopen(base,source(base));
  for(String icon:AnimatedIconWidgetPropertySchema.ICONS){
   var changed=apply(base,new PatchProperties(NESTED_ID,List.of(
    new PatchProperties.SetPatch(new PropertyName("icon"),new PropertyValue.StringValue(icon)),
    new PatchProperties.SetPatch(new PropertyName("progress"),number(".5")),
    new PatchProperties.SetPatch(new PropertyName("semanticLabel"),new PropertyValue.StringValue("Animated action")))));
   assertAnalysis(base,changed,"probe.dart",true);recordCase("preset_"+icon,changed,icon,".5","null","null","'Animated action'","null");
  }
  for(String field:List.of("progress","size"))for(String value:List.of("-12","0",".5","1","48")){
   if(field.equals("progress")&&value.equals("0"))continue;
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),number(value)));
   assertAnalysis(base,changed,"probe.dart",true);recordCase(field+"_"+value.replace("-","minus").replace(".","point"),changed,"menu_close",field.equals("progress")?value:"0","null",field.equals("size")?value:"null","null","null");
  }
  for(String field:List.of("color","size","semanticLabel","textDirection")){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),new PropertyValue.NullValue()));
   assertAnalysis(base,changed,"probe.dart",true);recordCase("explicit_null_"+field.toLowerCase(),changed,"menu_close","0","null","null","null","null");
   changed=reopen(changed,source(changed));
   var reset=apply(changed,new ResetProperty(NESTED_ID,new PropertyName(field)));assertAnalysis(changed,reset,"probe.dart",true);
  }
  for(String direction:List.of("ltr","rtl")){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("textDirection"),new PropertyValue.EnumValue("TextDirection",direction)));
   assertAnalysis(base,changed,"probe.dart",true);recordCase(direction,changed,"menu_close","0","null","null","null","TextDirection."+direction);
  }
  for(String field:List.of("icon","progress","color","size"))
   for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
    var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(field),reference(field,imported,member,factory)));
    assertAnalysis(base,changed,"probe.dart",true);
    recordCase(field+"_"+imported+"_"+member+"_"+factory,changed,field.equals("icon")?"play_pause":"menu_close",field.equals("progress")?".75":"0",field.equals("color")?"Color(0x88223344)":"null",field.equals("size")?"36":"null","null","null");
   }
  for(var entry:Map.of("color","_nullColor","size","_nullSize").entrySet()){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(entry.getKey()),ref(entry.getValue())));
   assertAnalysis(base,changed,"probe.dart",true);recordCase("source_null_"+entry.getKey(),changed,"menu_close","0","null","null","null","null");
  }
  var theme=apply(base,new SetProperty(NESTED_ID,new PropertyName("color"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))));
  assertAnalysis(base,theme,"probe.dart",true);recordCase("theme",theme,"menu_close","0","Theme.of(tester.element(finder)).colorScheme.primary","null","null","null");
  var clear=apply(base,new PatchProperties(NESTED_ID,List.of(new PatchProperties.SetPatch(new PropertyName("color"),new PropertyValue.ColorValue(0)),new PatchProperties.SetPatch(new PropertyName("semanticLabel"),new PropertyValue.StringValue("")))));
  assertAnalysis(base,clear,"probe.dart",true);recordCase("clear",clear,"menu_close","0","Color(0)","null","''","null");
  for(String field:List.of("icon","progress"))assertFalse(base.apply(new ResetProperty(NESTED_ID,new PropertyName(field))).changed());
  var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);
  assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  for(var field:Map.of("icon",List.of("_wrong","_plainIcon","_nullableIcon","_dynamic"),"progress",List.of("_wrong","_plainProgress","_integerAnimation","_nullableAnimation","_dynamicAnimation","_dynamic"),"color",List.of("_wrong","_dynamic"),"size",List.of("_wrong","_dynamic")).entrySet())
   for(String bad:field.getValue())assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(field.getKey()),ref(bad))),"probe.dart",false);
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("animated_icon_test.dart"),runtime());
  run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private static PropertyValue.DoubleValue number(String value){return new PropertyValue.DoubleValue(new BigDecimal(value));}
 private void recordCase(String name,DesignerCommandSession session,String icon,String progress,String color,String size,String label,String direction)throws Exception{
  saveCase(name,session);cases.put(name,String.join("|",icon,progress,color,size,label,direction));
 }
 private static PropertyValue.DartObjectReferenceValue ref(String name){
  return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
 }
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:animated_icon_contract/references.dart"):Optional.empty(),
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
    import 'package:flutter_test/flutter_test.dart';
    """);
  var tests=new StringBuilder();
  cases.forEach((name,values)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   var p=values.split("\\|");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{\n")
    .append("await tester.pumpWidget(host(const case_").append(name).append(".Sample(),rtl:rtl));await tester.pump();")
    .append("final finder=find.descendant(of:find.byType(case_").append(name).append(".Sample),matching:find.byType(AnimatedIcon));")
    .append("final n=tester.widget<AnimatedIcon>(finder);expect(identical(n.icon,AnimatedIcons.").append(p[0]).append("),true);")
    .append("expect(n.progress.value,").append(p[1]).append(");expect(n.color,").append(p[2]).append(");expect(n.size,").append(p[3]).append(");")
    .append("expect(n.semanticLabel,").append(p[4]).append(");expect(n.textDirection,").append(p[5]).append(");expect(find.text('Sibling'),findsOneWidget);expect(tester.takeException(),isNull);});}\n");
  });
  return imports+"""
    const iconKey=ValueKey('tested-icon');
    Finder native()=>find.byKey(iconKey);
    Widget host(Widget child,{bool rtl=false})=>MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:Center(child:child)));
    CustomPaint paint(WidgetTester tester)=>tester.widget<CustomPaint>(find.descendant(of:native(),matching:find.byType(CustomPaint)).first);
    Future<List<int>> pixels(WidgetTester tester)async{
      final painter=paint(tester).painter!;
      return (await tester.runAsync(()async{
        final recorder=ui.PictureRecorder();painter.paint(Canvas(recorder),const Size(48,48));
        final picture=recorder.endRecording(),image=await picture.toImage(48,48);
        try{return (await image.toByteData(format:ui.ImageByteFormat.rawRgba))!.buffer.asUint8List().toList();}
        finally{image.dispose();picture.dispose();}
      }))!;
    }
    class ProgressSignal extends Animation<double> with ChangeNotifier {
      ProgressSignal(this.current);
      double current;
      @override double get value=>current;
      void update(double next){current=next;notifyListeners();}
      @override AnimationStatus get status=>AnimationStatus.forward;
      @override void addStatusListener(AnimationStatusListener listener){}
      @override void removeStatusListener(AnimationStatusListener listener){}
    }
    class UnsupportedIcon extends AnimatedIconData {
      const UnsupportedIcon();
      @override bool get matchTextDirection=>false;
    }
    void main(){
    """+tests+"""
      testWidgets('progress signal repaints in place swaps and detaches without owning controllers',(tester)async{
        final first=ProgressSignal(0),second=ProgressSignal(1);
        Widget view(Animation<double> progress)=>host(AnimatedIcon(key:iconKey,icon:AnimatedIcons.menu_close,progress:progress,size:48));
        await tester.pumpWidget(view(first));expect(first.hasListeners,true);
        final element=tester.element(native()),initial=await pixels(tester);
        first.update(.5);await tester.pump();expect(tester.element(native()),same(element));expect(await pixels(tester),isNot(equals(initial)));
        first.update(1);await tester.pump();final end=await pixels(tester);
        first.update(0);await tester.pump();expect(await pixels(tester),equals(initial));
        await tester.pumpWidget(view(second));expect(first.hasListeners,false);expect(second.hasListeners,true);expect(await pixels(tester),equals(end));
        await tester.pumpWidget(const SizedBox());expect(first.hasListeners,false);expect(second.hasListeners,false);
        first.update(.1);second.update(.9);first.dispose();second.dispose();expect(tester.takeException(),isNull);
      });
      testWidgets('real controller forward reverse and ownership',(tester)async{
        final controller=AnimationController(vsync:tester,duration:const Duration(milliseconds:100));
        try{
          await tester.pumpWidget(host(AnimatedIcon(key:iconKey,icon:AnimatedIcons.play_pause,progress:controller,size:48)));
          final start=await pixels(tester);controller.forward();await tester.pump();await tester.pump(const Duration(milliseconds:50));
          expect(controller.value,closeTo(.5,.01));expect(await pixels(tester),isNot(equals(start)));
          await tester.pump(const Duration(milliseconds:50));expect(controller.value,1);
          controller.reverse();await tester.pump();await tester.pump(const Duration(milliseconds:100));expect(controller.value,0);expect(await pixels(tester),equals(start));
          await tester.pumpWidget(const SizedBox());controller.value=.25;expect(controller.value,.25);
        }finally{controller.dispose();}
      });
      testWidgets('paint clamps endpoints without changing animation values',(tester)async{
        final signal=ProgressSignal(0);await tester.pumpWidget(host(AnimatedIcon(key:iconKey,icon:AnimatedIcons.menu_close,progress:signal,size:48)));
        final start=await pixels(tester);signal.update(-12);await tester.pump();expect(signal.value,-12);expect(await pixels(tester),equals(start));
        signal.update(1);await tester.pump();final end=await pixels(tester);
        signal.update(12);await tester.pump();expect(signal.value,12);expect(await pixels(tester),equals(end));
        await tester.pumpWidget(const SizedBox());signal.dispose();
      });
      testWidgets('IconTheme size color and opacity including explicit colors',(tester)async{
        Widget themed({Color? color,double? size})=>host(IconTheme(data:const IconThemeData(size:36,color:Color(0xFF336699),opacity:.5),child:AnimatedIcon(key:iconKey,icon:AnimatedIcons.menu_close,progress:const AlwaysStoppedAnimation<double>(0),color:color,size:size)));
        await tester.pumpWidget(themed());expect(paint(tester).size,const Size(36,36));final inherited=await pixels(tester);expect(inherited.whereIndexed((i,v)=>i%4==3).reduce((a,b)=>a>b?a:b),closeTo(128,1));
        await tester.pumpWidget(themed(color:const Color(0xFF336699),size:48));expect(paint(tester).size,const Size(48,48));final explicit=await pixels(tester);expect(explicit.whereIndexed((i,v)=>i%4==3).reduce((a,b)=>a>b?a:b),closeTo(128,1));
        await tester.pumpWidget(themed(color:Colors.transparent));expect((await pixels(tester)).every((v)=>v==0),true);
      });
      testWidgets('semantic label is not an interactive button',(tester)async{
        final handle=tester.ensureSemantics();try{
          await tester.pumpWidget(host(const AnimatedIcon(key:iconKey,icon:AnimatedIcons.menu_close,progress:AlwaysStoppedAnimation<double>(0),semanticLabel:'Animated action')));
          final sem=tester.getSemantics(find.bySemanticsLabel('Animated action')).getSemanticsData();
          expect(sem.hasFlag(ui.SemanticsFlag.isButton),false);expect(sem.hasAction(ui.SemanticsAction.tap),false);
          await tester.tap(native());await tester.pump();expect(tester.takeException(),isNull);
        }finally{handle.dispose();}
      });
      testWidgets('public custom data subclasses cannot bypass pinned SDK private data cast',(tester)async{
        await tester.pumpWidget(host(const AnimatedIcon(key:iconKey,icon:UnsupportedIcon(),progress:AlwaysStoppedAnimation<double>(0))));
        expect(tester.takeException(),isA<TypeError>());
        await tester.pumpWidget(const SizedBox());
      });
    }
    extension IndexedValues<T> on Iterable<T> {
      Iterable<T> whereIndexed(bool Function(int,T) predicate)sync*{var i=0;for(final value in this){if(predicate(i++,value))yield value;}}
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
                name: animated_icon_contract
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
            System.out.println("AnimatedIcon pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
